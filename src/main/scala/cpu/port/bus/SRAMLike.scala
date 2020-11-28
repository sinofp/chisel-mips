// See LICENSE for license details.

package cpu.port.bus

import chisel3._

object SRAMLike {
  val SRAM_LIKE_SIZE_1B = 0.U
  val SRAM_LIKE_SIZE_2B = 1.U
  val SRAM_LIKE_SIZE_4B = 2.U
}

class SRAMLike extends Bundle {
  val req = Output(Bool())
  val wr = Output(Bool())
  val size = Output(UInt(2.W))
  val addr = Output(UInt(32.W))
  val wdata = Output(UInt(32.W))
  val addr_ok = Input(Bool())
  val data_ok = Input(Bool())
  val rdata = Input(UInt(32.W))
}
