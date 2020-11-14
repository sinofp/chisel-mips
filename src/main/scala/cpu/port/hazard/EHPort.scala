// See LICENSE for license details.

package cpu.port.hazard

import chisel3._
import cpu.decode.CtrlSigDef.SZ_FORWARD_HILO
import cpu.port.{Flush, HILOWenIn, Stall, WenWaddr}

class EHPort extends Bundle with WenWaddr with Flush with Stall with HILOWenIn {
  val branch = Input(Bool())
  val div_not_ready = Input(Bool())
  val forward_hi = Output(UInt(SZ_FORWARD_HILO))
  val forward_lo = Output(UInt(SZ_FORWARD_HILO))
}
