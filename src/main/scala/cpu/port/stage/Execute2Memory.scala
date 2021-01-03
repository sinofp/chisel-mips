// See LICENSE for license details.

package cpu.port.stage

import chisel3._
import cpu.decode.CtrlSigDef.{SZ_MEM_TYPE, SZ_SEL_REG_WDATA}
import cpu.port.{WAddr, WData, WEn}
import cpu.writeback.CP0.SZ_EXCEPT_TYPE

class Execute2Memory extends Bundle {
  val mem = Flipped(new Bundle with WEn with WData {
    val size = Output(UInt(SZ_MEM_TYPE))
  })
  val rf = Flipped(new Bundle with WEn with WAddr)
  val sel_reg_wdata = Input(UInt(SZ_SEL_REG_WDATA))
  val alu_out = Input(UInt(32.W))
  val pcp8 = Input(UInt(32.W))
  val hi = Flipped(new Bundle with WEn with WData)
  val lo = Flipped(new Bundle with WEn with WData)
  val c0 = Flipped(new Bundle with WEn with WAddr with WData)
  val except_type = Input(UInt(SZ_EXCEPT_TYPE))
  val is_in_delayslot = Input(Bool())
  val fwd_hi = new Bundle with WData
  val fwd_lo = new Bundle with WData
  val fwd_c0 = new Bundle with WData
  val data_sram_en = Input(Bool())
  val fwd_rf_load = Output(Bool())
  val fwd_rf_ldata = Output(UInt(32.W))
}