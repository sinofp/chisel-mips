// See LICENSE for license details.

package cpu.port.core

import chisel3._
import cpu.decode.CtrlSigDef.SZ_MEM_TYPE

class Core2DataMem extends Bundle {
  val addr = Output(UInt(32.W))
  val wen = Output(Bool())
  val wdata = Output(UInt(32.W))
  val size = Output(UInt(SZ_MEM_TYPE))
  val rdata = Input(UInt(32.W))
}
