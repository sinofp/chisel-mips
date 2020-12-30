// See LICENSE for license details.

package cpu.port

import chisel3._

trait Flush extends Bundle {
  val flush = Output(Bool())
}

trait Stall extends Bundle {
  val stall = Output(Bool())
}

trait WEn extends Bundle {
  val wen = Output(Bool())
}

trait WAddr extends Bundle {
  val waddr = Output(UInt(5.W))
}

trait WData extends Bundle {
  val wdata = Output(UInt(32.W))
}