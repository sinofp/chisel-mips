// See LICENSE for license details.

package cpu.port.hazard

import chisel3._

class Fetch2Hazard extends Bundle with Stall {
  val estart = Output(Bool())
  val newpc = Output(UInt(32.W))
}