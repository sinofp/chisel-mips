// See LICENSE for license details.

package cpu.port.core

import chisel3._

class Core2WriteBack extends Bundle {
  val int = Input(UInt(6.W))
  val timer_int = Output(Bool())
  val junk_output = Output(UInt(32.W))
}
