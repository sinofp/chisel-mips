// See LICENSE for license details.

package cpu.port.stage

import chisel3._
import cpu.decode.CtrlSigDef.SZ_SEL_REG_WDATA
import cpu.port.{HILO, HILOWen}

class MWPort extends Bundle with HILO with HILOWen {
  val pcp8 = UInt(32.W)
  val reg_wen = Bool()
  val sel_reg_wdata = UInt(SZ_SEL_REG_WDATA)
  val reg_waddr = UInt(5.W)
  val mem_rdata = UInt(32.W)
  val alu_out = UInt(32.W)
  val c0_wen = Bool()
}
