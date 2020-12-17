// See LICENSE for license details.

package cpu.port.debug

import chisel3._

class DebugWb extends Bundle {
  val pc = Output(UInt(32.W))
  val rf_wen = Output(UInt(4.W))
  val rf_wnum = Output(UInt(5.W))
  val rf_wdata = Output(UInt(32.W))
}
