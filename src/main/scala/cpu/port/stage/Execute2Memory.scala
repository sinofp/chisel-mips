// See LICENSE for license details.

package cpu.port.stage

import chisel3._
import cpu.decode.CtrlSigDef.{SZ_MEM_TYPE, SZ_SEL_REG_WDATA}

class Execute2Memory extends Bundle {
  val mem_wen = Bool()
  val mem_wdata = UInt(32.W)
  val mem_size = UInt(SZ_MEM_TYPE)
  val reg_wen = Bool()
  val sel_reg_wdata = UInt(SZ_SEL_REG_WDATA)
  val reg_waddr = UInt(5.W)
  val alu_out = UInt(32.W)
  val pcp8 = UInt(32.W)
  val hi = UInt(32.W)
  val lo = UInt(32.W)
  val hi_wen = Bool()
  val lo_wen = Bool()
  val c0_wen = Bool()
  val c0_waddr = UInt(5.W)
  val c0_wdata = UInt(32.W)
}