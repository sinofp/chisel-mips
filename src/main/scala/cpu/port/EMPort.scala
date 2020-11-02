// See LICENSE for license details.

package cpu.port

import chisel3._
import cpu.decode.CtrlSigDef.{SZ_MEM_TYPE, SZ_SEL_REG_WDATA}

class EMPort extends Bundle {
  val mem_wen = Bool()
  val mem_wdata = UInt(32.W)
  val mem_size = UInt(SZ_MEM_TYPE)
  val reg_wen = Bool()
  val sel_reg_wdata = UInt(SZ_SEL_REG_WDATA)
  val reg_waddr = UInt(5.W)
  val alu_out = UInt(32.W)
  val pcp8 = UInt(32.W)
}