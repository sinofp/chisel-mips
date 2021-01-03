// See LICENSE for license details.

package cpu.port.stage

import chisel3._
import cpu.port.WData

class WriteBack2Execute extends Bundle {
  val hi       = Flipped(new Bundle with WData)
  val lo       = Flipped(new Bundle with WData)
  val c0_data  = Input(UInt(32.W))
  val c0_rdata = Input(UInt(32.W))
  val c0_raddr = Output(UInt(5.W))
}
