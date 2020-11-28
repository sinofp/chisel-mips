// See LICENSE for license details.

package cpu.port.core

import chisel3._

class Core2InstMem extends Bundle {
  val pc = Output(UInt(32.W))
  val inst = Input(UInt(32.W))
}
