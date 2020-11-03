// See LICENSE for license details.

package cpu.port

import chisel3._

trait WenWaddr extends Bundle {
  val wen = Input(Bool())
  val waddr = Input(UInt(5.W))
}

trait Wdata extends Bundle {
  val wdata = UInt(32.W)
}

trait Flush extends Bundle {
  val flush = Output(Bool())
}

trait Stall extends Bundle {
  val stall = Output(Bool())
}