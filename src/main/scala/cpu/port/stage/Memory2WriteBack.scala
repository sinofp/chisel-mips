// See LICENSE for license details.

package cpu.port.stage

import chisel3._
import cpu.decode.CtrlSigDef.SZ_SEL_REG_WDATA

class Memory2WriteBack extends Bundle {
  val pcp8 = Output(UInt(32.W))
  val reg_wen = Output(Bool())
  val sel_reg_wdata = Output(UInt(SZ_SEL_REG_WDATA))
  val reg_waddr = Output(UInt(5.W))
  val mem_rdata = Output(UInt(32.W))
  val alu_out = Output(UInt(32.W))
  val hi = Output(UInt(32.W))
  val hi_wen = Output(Bool())
  val lo_wen = Output(Bool())
  val lo = Output(UInt(32.W))
  val c0_wen = Output(Bool())
  val c0_waddr = Output(UInt(5.W))
  val c0_wdata = Output(UInt(32.W))
}
