// See LICENSE for license details.

package cpu.port.hazard

import chisel3._

class WdataPort extends Bundle {
  val wdata = UInt(32.W)
}
