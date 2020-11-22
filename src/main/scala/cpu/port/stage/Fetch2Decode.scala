// See LICENSE for license details.

package cpu.port.stage

import chisel3._

class Fetch2Decode extends Bundle {
  val inst = UInt(32.W)
  val pcp4 = UInt(32.W)
}