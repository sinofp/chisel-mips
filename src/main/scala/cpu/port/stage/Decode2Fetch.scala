// See LICENSE for license details.

package cpu.port.stage

import chisel3._

class Decode2Fetch extends Bundle {
  val inst = Input(UInt(32.W))
  val pcp4 = Input(UInt(32.W))
  val jump = Output(Bool())
  val j_addr = Output(UInt(32.W))
}
