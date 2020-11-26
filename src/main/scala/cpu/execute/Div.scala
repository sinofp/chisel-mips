// See LICENSE for license details.

package cpu.execute

import chisel3._
import chisel3.util.HasBlackBoxResource
import cpu.util.{Config, DefCon}

class Div(implicit c: Config = DefCon) extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle() {
    val clk = Input(Bool())
    val start = Input(Bool())
    val sign = Input(Bool())
    val dividend = Input(UInt(32.W))
    val divider = Input(UInt(32.W))
    val ready = Output(Bool())
    val quotient = Output(UInt(32.W))
    val remainder = Output(UInt(32.W))
  })
  addResource("/Div.v")
}
