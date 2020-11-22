// See LICENSE for license details.

package cpu.port.hazard

import chisel3._
import cpu.decode.CtrlSigDef.{SZ_FORWARD_C0, SZ_FORWARD_HILO}
import cpu.port.{Flush, Stall}

class EHPort extends Bundle with Flush with Stall {
  val branch = Input(Bool())
  val div_not_ready = Input(Bool())
  val c0_raddr = Input(UInt(5.W))
  val forward_hi = Output(UInt(SZ_FORWARD_HILO))
  val forward_lo = Output(UInt(SZ_FORWARD_HILO))
  val forward_c0 = Output(UInt(SZ_FORWARD_C0))
  val hi_wen = Input(Bool())
  val lo_wen = Input(Bool())
  val wen = Input(Bool())
  val waddr = Input(UInt(5.W))
}
