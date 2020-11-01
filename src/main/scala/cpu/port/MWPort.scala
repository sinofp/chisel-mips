// See LICENSE for license details.

package cpu.port

import chisel3._
import cpu.decode.CtrlSigDef.SZ_SEL_REG_WDATA

class MWPort extends Bundle {
  val pc = UInt(32.W)
  val reg_wen = Bool()
  val sel_reg_wdata = UInt(SZ_SEL_REG_WDATA)
  val reg_waddr = UInt(5.W)
  val mem_rdata = UInt(32.W)
  val alu_out = UInt(32.W)
}
