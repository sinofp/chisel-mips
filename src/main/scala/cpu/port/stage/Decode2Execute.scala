// See LICENSE for license details.

package cpu.port.stage

import chisel3._
import cpu.decode.CtrlSigDef.{SZ_BR_TYPE, SZ_MEM_TYPE, SZ_SEL_MOVE, SZ_SEL_REG_WDATA}
import cpu.execute.ALU.SZ_ALU_FN
import cpu.writeback.CP0.SZ_EXCEPT_TYPE

class Decode2Execute extends Bundle {
  val pcp8 = Output(UInt(32.W))
  val alu_fn = Output(UInt(SZ_ALU_FN))
  val alu_n = Output(Bool())
  val mul = Output(Bool())
  val div = Output(Bool())
  val mem_wen = Output(Bool())
  val mem_wdata = Output(UInt(32.W))
  val reg_wen = Output(Bool())
  val sel_reg_wdata = Output(UInt(SZ_SEL_REG_WDATA))
  val br_type = Output(UInt(SZ_BR_TYPE))
  val br_addr = Output(UInt(32.W))
  val num1 = Output(UInt(32.W))
  val num2 = Output(UInt(32.W))
  val reg_waddr = Output(UInt(5.W))
  val mem_size = Output(UInt(SZ_MEM_TYPE))
  val c0_wen = Output(Bool())
  val c0_addr = Output(UInt(5.W)) // rd
  val sel_move = Output(UInt(SZ_SEL_MOVE))
  val hi_wen = Output(Bool())
  val lo_wen = Output(Bool())
  val except_type = Output(UInt(SZ_EXCEPT_TYPE))
  val is_in_delayslot = Output(Bool())
  val wdata = Input(UInt(32.W))
}
