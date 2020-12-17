// See LICENSE for license details.

package cpu.port.core

import chisel3._

class SramIO extends Bundle {
  val en = Output(Bool())
  val wen = Output(UInt(4.W))
  val addr = Output(UInt(32.W))
  val wdata = Output(UInt(32.W))
  val rdata = Input(UInt(32.W))
}
