// See LICENSE for license details.

package cpu.port.stage

import chisel3._

class Memory2Execute extends Bundle {
  val hi = UInt(32.W)
  val lo = UInt(32.W)
  val c0_data = UInt(32.W)
}
