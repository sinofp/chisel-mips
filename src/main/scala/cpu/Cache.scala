// See LICENSE for license details.

package cpu

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._
import cpu.util.Chisel3IsBad.log2I

class Cache extends MultiIOModule {
  val nWays = 4
  val nSets = 16
  val nBlocks = 16

  require(nWays % 2 == 0, "ways should be even")
  require(nSets % 2 == 0, "sets should be even")
  require(nBlocks % 2 == 0, "blocks should be even")

  val cpu = IO(new Bundle() {
    val addr = Input(UInt(32.W))
    val wen = Input(Bool())
    val wdata = Input(UInt(32.W)) // 如果是ICache，就不会有write
    val ren = Input(Bool())
    val rdata = Output(UInt(32.W)) // Valid?
    val miss = Output(Bool())
  })
  val mem = IO(new Bundle() {
    val addr = Output(UInt(32.W))
    val write = Decoupled(UInt((nBlocks * 32).W))
    val read = Flipped(Decoupled(UInt((nBlocks * 32).W)))
  })

  val byteOffsetLen = 2
  val blockOffsetLen = log2I(nBlocks)
  val setLen = log2I(nSets)
  val tagLen = 32 - setLen - blockOffsetLen - byteOffsetLen

  val V = Seq.fill(nWays)(RegInit(VecInit(Seq.fill(nSets)(false.B))))
  val D = Seq.fill(nWays)(RegInit(VecInit(Seq.fill(nSets)(false.B))))
  val U = RegInit(VecInit(Seq.fill(nSets)(false.B)))

  // 本来想用SyncReadMem，但Mux1H要作用于Seq[Data]，SRM不是Data
  val tagMem = Seq.fill(nWays)(RegInit(VecInit(Seq.fill(nSets * nBlocks)(0.U(tagLen.W)))))
  val dataMem = Seq.fill(nWays)(RegInit(VecInit(Seq.fill(nSets * nBlocks)(0.U(32.W)))))

  val addr = cpu.addr.asTypeOf(new Bundle() {
    val tag = UInt(tagLen.W)
    val set = UInt(setLen.W)
    val block = UInt(blockOffsetLen.W)
    val byteOffset = UInt(byteOffsetLen.W)
  })

  val cache_addr = addr.set ## addr.block
  val hits = tagMem.map(_ (cache_addr) === addr.tag)
  val hit = hits.reduce((a, b) => a | b)
  cpu.miss := ~hit

  val sCompare :: sALLOCATE :: sWRITEBACK :: Nil = Enum(3)
  val state = RegInit(sCompare)

  // 用于伪LRU，假设4路，分成两组，用U选其中一组，用它选这一组的哪一路要被换出去
  val (roulette, _) = Counter(false.B, nWays / 2)

  // 这行么
  cpu.rdata := DontCare
  mem := DontCare

  switch(state) {
    is(sCompare) {
      when(hit) {
        when(cpu.ren) {
          cpu.rdata := Mux1H(hits, dataMem.map(_ (cache_addr)))
          // todo update U
        }.elsewhen(cpu.wen) {
          Mux1H(hits, D)(addr.set) := true.B
          Mux1H(hits, dataMem)(cache_addr) := cpu.wdata
        }
      }.elsewhen(cpu.ren | cpu.wen) {
        // cache miss，抓一组写回去
        // todo 找不脏的一组直接分配
        when(D.head(cache_addr)) {
          mem.write.valid := true.B // = wen?
          state := sWRITEBACK
        }.otherwise {
          mem.read.ready := true.B
          state := sALLOCATE
        }
      }
    }
    is(sALLOCATE) {
      when(mem.read.fire) {
        mem.addr := cpu.addr
        dataMem.head(cache_addr) := mem.read.bits
      }
      mem.read.ready := false.B
      state := sCompare
    }
    is(sWRITEBACK) {
      when(mem.write.fire) {
        mem.addr := cpu.addr
        mem.write.bits := dataMem.head(cache_addr)
      }
      mem.write.valid := false.B
      mem.read.ready := true.B
      state := sALLOCATE
    }
  }
}

object Cache extends App {
  new ChiselStage emitVerilog new Cache
}
