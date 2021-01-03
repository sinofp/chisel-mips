// See LICENSE for license details.

package cpu.execute

import chisel3._

class Mul extends Module {
  val io = IO(new Bundle() {
    val sign         = Input(Bool())
    val multiplicand = Input(UInt(32.W))
    val multiplier   = Input(UInt(32.W))
    val product      = Output(UInt(64.W))
  })

  import io._

  product := Mux(sign, (multiplicand.asSInt() * multiplier.asSInt()).asUInt(), multiplicand * multiplier)
}
