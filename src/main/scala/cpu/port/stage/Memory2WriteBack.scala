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
  val except_type = Output(UInt(32.W))
  val pc_now = Output(UInt(32.W))
  val is_in_delayslot = Output(Bool())
  val c0_epc = Input(UInt(32.W))
  val c0_status = Input(UInt(32.W))
  val c0_cause = Input(UInt(32.W))
  val c0_wen_f = Input(Bool())
  val c0_waddr_f = Input(UInt(5.W))
  val c0_wdata_f = Input(UInt(32.W))
}
