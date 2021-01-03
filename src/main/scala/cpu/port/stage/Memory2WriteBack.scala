// See LICENSE for license details.

package cpu.port.stage

import chisel3._
import cpu.decode.CtrlSigDef.SZ_SEL_REG_WDATA
import cpu.port.{WAddr, WData, WEn}
import cpu.writeback.CP0.SZ_EXCEPT_TYPE
import cpu.writeback.{Cause, Status}

class Memory2WriteBack extends Bundle {
  val pcp8            = Output(UInt(32.W))
  val rf              = new Bundle with WEn with WAddr
  val sel_reg_wdata   = Output(UInt(SZ_SEL_REG_WDATA))
  val mem_rdata       = Output(UInt(32.W))
  val alu_out         = Output(UInt(32.W))
  val hi              = new Bundle with WEn with WData
  val lo              = new Bundle with WEn with WData
  val c0              = new Bundle with WEn with WAddr with WData
  val except_type     = Output(UInt(SZ_EXCEPT_TYPE))
  val is_in_delayslot = Output(Bool())
  val c0_epc          = Input(UInt(32.W))
  val c0_status       = Input(new Status)
  val c0_cause        = Input(new Cause)
  val fwd_c0          = Flipped(new Bundle with WEn with WAddr with WData)
}
