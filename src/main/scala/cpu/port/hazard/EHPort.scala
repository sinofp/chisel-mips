// See LICENSE for license details.

package cpu.port.hazard

import chisel3._
import cpu.port.{Flush, Stall, WenWaddr}

class EHPort extends Bundle with WenWaddr with Flush with Stall {
  val branch = Input(Bool())
  val div_not_ready = Input(Bool())
}
