// See LICENSE for license details.

package cpu.port.stage

import chisel3._
import cpu.decode.CtrlSigDef.{SZ_MEM_TYPE, SZ_SEL_REG_WDATA}
import cpu.writeback.CP0.SZ_EXCEPT_TYPE

class Execute2Memory extends Bundle {
  val mem_wen = Input(Bool())
  val mem_wdata = Input(UInt(32.W))
  val mem_size = Input(UInt(SZ_MEM_TYPE))
  val reg_wen = Input(Bool())
  val sel_reg_wdata = Input(UInt(SZ_SEL_REG_WDATA))
  val reg_waddr = Input(UInt(5.W))
  val alu_out = Input(UInt(32.W))
  val pcp8 = Input(UInt(32.W))
  val hi = Input(UInt(32.W))
  val lo = Input(UInt(32.W))
  val hi_wen = Input(Bool())
  val lo_wen = Input(Bool())
  val c0_wen = Input(Bool())
  val c0_waddr = Input(UInt(5.W))
  val c0_wdata = Input(UInt(32.W))
  val except_type = Input(UInt(SZ_EXCEPT_TYPE))
  val pc_now = Input(UInt(32.W))
  val is_in_delayslot = Input(Bool())
  val hi_forward = Output(UInt(32.W))
  val lo_forward = Output(UInt(32.W))
  val c0_data = Output(UInt(32.W))
}