// See LICENSE for license details.

package cpu.port

import chisel3._

class WDPort extends ForwardPort {
  val wdata = UInt(32.W)
}