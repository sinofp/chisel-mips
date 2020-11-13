// See LICENSE for license details.

package cpu.port.stage

import chisel3._
import cpu.decode.CtrlSigDef.{SZ_BR_TYPE, SZ_MEM_TYPE, SZ_SEL_REG_WDATA}
import cpu.execute.ALU.SZ_ALU_FN
import cpu.port.HILOWen

class DEPort extends Bundle with HILOWen {
  val pcp8 = UInt(32.W)
  val alu_fn = UInt(SZ_ALU_FN)
  val mul = Bool()
  val div = Bool()
  val mem_wen = Bool()
  val mem_wdata = UInt(32.W)
  val reg_wen = Bool() // link包含于其中
  val sel_reg_wdata = UInt(SZ_SEL_REG_WDATA)
  val br_type = UInt(SZ_BR_TYPE)
  val br_addr = UInt(32.W)
  val num1 = UInt(32.W)
  val num2 = UInt(32.W)
  val reg_waddr = UInt(5.W)
  val mem_size = UInt(SZ_MEM_TYPE)
}
