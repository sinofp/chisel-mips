// See LICENSE for license details.

package cpu.port.hazard

import chisel3._
import cpu.port.{Flush, Stall}

class Fetch2Hazard extends Bundle with Stall with Flush {
  val newpc = Output(UInt(32.W))
}
