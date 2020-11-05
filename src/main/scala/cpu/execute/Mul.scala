// See LICENSE for license details.

package cpu.execute

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._
import cpu.util.{Config, DefCon}

import scala.math.log10

class Mul(val xLen: Int = 32)(implicit c: Config = DefCon) extends Module {
  require(xLen % 2 == 0)
  val io = IO(new Bundle() {
    val multiplicand = Input(UInt(xLen.W))
    val multiplier = Input(UInt(xLen.W))
    val start = Input(Bool())
    val prod = Output(UInt((2 * xLen).W))
    val ready = Output(Bool())
  })

  val product = Reg(UInt((2 * xLen + 1).W))
  io.prod := product(2 * xLen, 0)
  val log2 = (x: Int) => (log10(x) / log10(2)).toInt
  val bit = RegInit(UInt(log2(xLen).W), 0.U)
  io.ready := !bit
  val lostbit = Reg(UInt(1.W))
  val multsx = io.multiplicand(xLen - 1) ## io.multiplicand

  when(io.start && io.ready) {
    bit := (xLen / 2).U
    product := 0.U((xLen + 1).W) ## io.multiplier
    lostbit := 0.U
  }.elsewhen(bit =/= 0.U) {
    val hi_old = product(2 * xLen, xLen)
    val is = (s: String) => (product(1, 0) ## lostbit) === ("b" + s).U
    // Hi-ν
    val hi_new = MuxCase(hi_old, Array( // 000, 111 -> only shift
      is("001") -> (hi_old + multsx),
      is("010") -> (hi_old + multsx),
      is("011") -> (hi_old + 2.U * io.multiplicand),
      is("100") -> (hi_old - 2.U * io.multiplicand),
      is("101") -> (hi_old - multsx),
      is("110") -> (hi_old - multsx),
    ))
    lostbit := product(1)
    product := Cat(Fill(2, hi_new(xLen)), hi_new(xLen, 0), product(xLen - 1, 2)) // 不写hi_new(16,0)，写hi_new拼接时就会加个0，为啥？
    if (c.debugMul) {
      printf(p"\tsel = ${Binary(product(1, 0) ## lostbit)}, hi_new = ${Binary(hi_new)}\n")
    }
    bit := bit - 1.U
  }

  if (c.debugMul) {
    val cnt = Counter(true.B, 100)
    printf(p"[log Mul]\n\tcycle = ${Decimal(cnt._1)}, product = ${Binary(product)}\n" +
      p"\tbooth code = ${Binary(product(1, 0) ## lostbit)}, bit = $bit, ready = ${io.ready}\n")
  }
}

object Mul extends App {
  new ChiselStage emitVerilog new Mul
}
