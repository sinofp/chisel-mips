// See LICENSE for license details.

package cpu.port

import chisel3._

class FDPort extends Bundle {
  val inst = UInt(32.W)
  val pcp4 = UInt(32.W)
}