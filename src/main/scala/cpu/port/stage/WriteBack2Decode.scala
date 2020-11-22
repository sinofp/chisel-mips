// See LICENSE for license details.

package cpu.port.stage

import chisel3._

class WriteBack2Decode extends Bundle {
  val wdata = UInt(32.W)
  val wen = Input(Bool())
  val waddr = Input(UInt(5.W))
}