// See LICENSE for license details.

package cpu.writeback

import chisel3._
import cpu.util.{Config, DefCon}

class HILO(implicit c: Config = DefCon) extends Module {
  val io = IO(new Bundle() {
    val wen = Input(Bool())
    val _hi = Input(UInt(32.W))
    val _lo = Input(UInt(32.W))
    val hi = Output(UInt(32.W))
    val lo = Output(UInt(32.W))
  })

  import io._

  hi := RegNext(hi, 0.U)
  lo := RegNext(lo, 0.U)

  when(wen) {
    hi := _hi
    lo := _lo
  }
}