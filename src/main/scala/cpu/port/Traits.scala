// See LICENSE for license details.

package cpu.port

import chisel3._

trait WenWaddr extends Bundle {
  val wen = Input(Bool())
  val waddr = Input(UInt(5.W))
}

trait C0WenWaddr extends Bundle {
  val c0_wen = Input(Bool())
  val c0_waddr = Input(UInt(5.W))
}

trait C0 extends Bundle {
  val c0_wen = Bool()
  val c0_waddr = UInt(5.W)
  val c0_wdata = UInt(32.W)
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

trait HILO extends Bundle {
  val hi = UInt(32.W)
  val lo = UInt(32.W)
}

trait C0UN extends Bundle {
  val c0_data = UInt(32.W)
}

trait HILOWen extends Bundle {
  val hi_wen = Bool()
  val lo_wen = Bool()
}

trait HILOWenIn extends Bundle {
  val hi_wen = Input(Bool())
  val lo_wen = Input(Bool())
}