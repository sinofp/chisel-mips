// See LICENSE for license details.

package cpu.port.stage

import chisel3._

class EWPort extends Bundle {
  val c0_raddr = Output(UInt(5.W))
  val c0_rdata = Input(UInt(32.W))
}
