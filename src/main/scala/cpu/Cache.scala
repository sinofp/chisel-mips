// See LICENSE for license details.

package cpu

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._
import cpu.util.Chisel3IsBad.log2I

class Cache extends MultiIOModule {
  val nWays   = 4
  val nSets   = 16
  val nBlocks = 16

  require(nWays   % 2 == 0 && nWays > 2, "ways should be even")
  require(nSets   % 2 == 0, "sets should be even")
  require(nBlocks % 2 == 0, "blocks should be even")

  val cpu = IO(new Bundle() {
    val addr  = Input(UInt(32.W))
    val wen   = Input(Bool())
    val wdata = Input(UInt(32.W))  // 如果是ICache，就不会有write
    val ren   = Input(Bool())
    val rdata = Output(UInt(32.W)) // Valid?
    val miss  = Output(Bool())
  })
  val mem = IO(new Bundle() {
    val addr  = Output(UInt(32.W))
    val write = Decoupled(UInt((nBlocks * 32).W))
    val read  = Flipped(Decoupled(UInt((nBlocks * 32).W)))
  })

  val byteOffsetLen  = 2
  val blockOffsetLen = log2I(nBlocks)
  val setLen         = log2I(nSets)
  val tagLen         = 32 - setLen - blockOffsetLen - byteOffsetLen

  val V = Seq.fill(nWays)(RegInit(VecInit(Seq.fill(nSets)(false.B))))
  val D = Seq.fill(nWays)(RegInit(VecInit(Seq.fill(nSets)(false.B))))
  val U = RegInit(VecInit(Seq.fill(nSets)(false.B)))

  // 本来想用SyncReadMem，但Mux1H要作用于Seq[Data]，SRM不是Data
  val tagMem  = Seq.fill(nWays)(RegInit(VecInit(Seq.fill(nSets * nBlocks)(0.U(tagLen.W)))))
  val dataMem = Seq.fill(nWays)(RegInit(VecInit(Seq.fill(nSets * nBlocks)(0.U(32.W)))))

  val addr = cpu.addr.asTypeOf(new Bundle() {
    val tag        = UInt(tagLen.W)
    val set        = UInt(setLen.W)
    val block      = UInt(blockOffsetLen.W)
    val byteOffset = UInt(byteOffsetLen.W)
  })

  val cache_addr = addr.set ## addr.block

  // 伪LRU：
  // 假设有4路cache
  // idx   : 0 1 | 2 3
  // hits  : f f | t f
  // group :  f     t
  // group代表本次使用的Cache是4路中的哪一群——为真时代表后半群
  // 类似的，Used记录上次同样低地址，选出的是哪一组，所以U(cache_addr) := group
  // 未命中，要换出时，通过U找哪一群上次被使用。如果是后半群（U为真），选中随机数+0的那一路，否则+halfWay
  // 随机数在这里简单实现成了计数器

  val halfWays      = nWays / 2
  val (roulette, _) = Counter(false.B, halfWays) // 有必要么？我一直“随机”第一个不行？
  val hitWay        = tagMem.map(_(cache_addr) === addr.tag)
  val hit           = hitWay.reduce(_ | _)
  val group         = hitWay.drop(halfWays).reduce(_ | _)
  val getOut        = UIntToOH(roulette + Mux(U(cache_addr), 0.U, halfWays.asUInt)).asBools
  // 对Seq[Bool]的土法Mux，因为Seq[Bool]不是Data，也没有asUInt之类的
  val selWay        = for (way <- 0 until nWays) yield Mux(hit, hitWay(way), getOut(way))
  cpu.miss := ~hit

  val sCompare :: sALLOCATE :: sWRITEBACK :: Nil = Enum(3)
  val state                                      = RegInit(sCompare)

  // 这行么
  cpu.rdata := DontCare
  mem       := DontCare

  switch(state) {
    is(sCompare) {
      when(hit) {
        U(cache_addr) := group
        when(cpu.ren) {
          cpu.rdata := Mux1H(hitWay, dataMem.map(_(cache_addr)))
        }.elsewhen(cpu.wen) {
          Mux1H(selWay, D)(addr.set)         := true.B
          Mux1H(selWay, dataMem)(cache_addr) := cpu.wdata
        }
      }.elsewhen(cpu.ren | cpu.wen) {
        // cache miss，抓一组写回去
        // todo 找不脏的一组直接分配 —— 意义大么？
        when(Mux1H(selWay, D)(cache_addr)) {
          Mux1H(selWay, D)(cache_addr) := false.B
          mem.write.valid              := true.B // = wen?
          state                        := sWRITEBACK
        }.otherwise {
          mem.read.ready := true.B
          state          := sALLOCATE
        }
      }
    }
    is(sALLOCATE) {
      U(cache_addr)  := group
      when(mem.read.fire) {
        mem.addr                           := cpu.addr
        Mux1H(selWay, dataMem)(cache_addr) := mem.read.bits
      }
      mem.read.ready := false.B
      state          := sCompare
    }
    is(sWRITEBACK) {
      U(cache_addr)   := group
      when(mem.write.fire) {
        mem.addr       := cpu.addr
        mem.write.bits := Mux1H(selWay, dataMem)(cache_addr)
      }
      mem.write.valid := false.B
      mem.read.ready  := true.B
      state           := sALLOCATE
    }
  }
}

object Cache extends App {
  new ChiselStage emitVerilog new Cache
}
