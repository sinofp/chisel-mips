// See LICENSE for license details.

package cpu.port.stage

import chisel3._

class Decode2Memory extends Bundle {
  val wdata = Input(UInt(32.W))
}
