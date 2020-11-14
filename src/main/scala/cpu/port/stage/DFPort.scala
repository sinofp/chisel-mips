// See LICENSE for license details.

package cpu.port.stage

import chisel3._

class DFPort extends Bundle {
  val jump = Output(Bool())
  val j_addr = Output(UInt(32.W))
}
