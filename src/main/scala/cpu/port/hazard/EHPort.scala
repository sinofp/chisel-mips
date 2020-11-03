// See LICENSE for license details.

package cpu.port.hazard

import chisel3._
import cpu.port.{Flush, WenWaddr}

class EHPort extends Bundle with WenWaddr with Flush {
  val branch = Input(Bool())
}
