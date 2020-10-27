// See LICENSE for license details.

package cpu

import chisel3._
import chisel3.stage.ChiselStage

class HILO extends Module {
  val io = IO(new Bundle() {
    val wen = Input(Bool())
    val _hi = Input(UInt(32.W))
    val _lo = Input(UInt(32.W))
    val hi_ = Output(UInt(32.W))
    val lo_ = Output(UInt(32.W))
  })

  import io._

  val hi = RegInit(0.U(32.W))
  val lo = RegInit(0.U(32.W))

  when(wen) {
    hi := _hi
    lo := _lo
  }
  hi_ := hi
  lo_ := lo
}

object HILO extends App {
  (new ChiselStage).emitVerilog(new HILO)
}
