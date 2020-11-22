// See LICENSE for license details.

package cpu.port.stage

import chisel3._

class Execute2Fetch extends Bundle {
  val branch = Output(Bool())
  val br_addr = Output(UInt(32.W))
}
