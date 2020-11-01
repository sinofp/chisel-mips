// See LICENSE for license details.

package cpu.port

import chisel3._

class WDPort extends Bundle {
  val wen = Bool()
  val waddr = UInt(5.W)
  val wdata = UInt(32.W)
}