// See LICENSE for license details.

package cpu.util.port

import chisel3._

class InputInst extends Bundle {
  val wen = Bool()
  val waddr = UInt(5.W)
  val wdata = UInt(32.W)
}
