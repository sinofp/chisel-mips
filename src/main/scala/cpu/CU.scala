// See LICENSE for license details.

package cpu

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._
import cpu.ALU._
import cpu.Instructions._

object CtrlSigDef {
  // @formatter:off
  // BR
  val SZ_BR_TYPE = 3.W

  def isBeq = (x: UInt) => x === 1.U(SZ_BR_TYPE)
  def isBne = (x: UInt) => x === 2.U(SZ_BR_TYPE)
  def isBgez = (x: UInt) => x === 3.U(SZ_BR_TYPE)
  def isBgtz = (x: UInt) => x === 4.U(SZ_BR_TYPE)
  def isBlez = (x: UInt) => x === 5.U(SZ_BR_TYPE)
  def isBltz = (x: UInt) => x === 6.U(SZ_BR_TYPE)

  // Select
  // todo SIZE
  val SZ_SEL_ALU1 = 2.W
  val SZ_SEL_ALU2 = 2.W
  val SZ_SEL_IMM = 3.W
  val SZ_SEL_REG_WADDR = 2.W
  val SZ_SEL_REG_WDATA = 2.W

  val SEL_ALU1_SA = 0.U(SZ_SEL_ALU1) // todo 没别的就改成Bool()
  val SEL_ALU1_RS = 1.U(SZ_SEL_ALU1)
  val SEL_ALU2_IMM = 0.U(SZ_SEL_ALU2)
  val SEL_ALU2_RT = 1.U(SZ_SEL_ALU2)
  val SEL_ALU2_ZERO = 2.U(SZ_SEL_ALU2)

  val SEL_IMM_LUI = 0.U(SZ_SEL_IMM)
  val SEL_IMM_S = 1.U(SZ_SEL_IMM)
  val SEL_IMM_U = 2.U(SZ_SEL_IMM)
  val SEL_IMM_J = 3.U(SZ_SEL_IMM)
  val SEL_IMM_SH = 4.U(SZ_SEL_IMM)

  val SEL_REG_WADDR_RD = 0.U(SZ_SEL_REG_WADDR)
  val SEL_REG_WADDR_RT = 1.U(SZ_SEL_REG_WADDR)
  val SEL_REG_WADDR_31 = 2.U(SZ_SEL_REG_WADDR)
  val SEL_REG_WDATA_ALU = 0.U(SZ_SEL_REG_WDATA)
  val SEL_REG_WDATA_MEM = 1.U(SZ_SEL_REG_WDATA)
  val SEL_REG_WDATA_LUI = 2.U(SZ_SEL_REG_WDATA)
  val SEL_REG_WDATA_LNK = 3.U(SZ_SEL_REG_WDATA)

  // 也许应该用BitPat?
  val X = 0.U
  val XX = 0.U(2.W)
  val XXX = 0.U(3.W)
  // @formatter:on
}

import cpu.CtrlSigDef._

class CtrlSigs extends Bundle {
  val sel_alu1 = UInt(SZ_SEL_ALU1)
  val sel_alu2 = UInt(SZ_SEL_ALU2)
  val sel_imm = UInt(SZ_SEL_IMM)
  val alu_fn = UInt(SZ_ALU_FN)
  val mul = Bool()
  val div = Bool()
  val mem_wen = Bool()
  val reg_wen = Bool() // link包含于其中
  val sel_reg_waddr = UInt(SZ_SEL_REG_WADDR) // rs rt $31
  val sel_reg_wdata = UInt(SZ_SEL_REG_WDATA)
  val br_type = UInt(SZ_BR_TYPE)
  // todo ...?
}

class CU extends MultiIOModule {
  val inst = IO(Input(UInt(32.W)))
  val ctrl = IO(Output(new CtrlSigs))

  val outW = ctrl.getWidth.W

  val out = Wire(UInt(outW))

  private def is(bp: BitPat) = inst === bp

  out := MuxCase(0.U(outW), Array(
    is(ADD) -> Cat(SEL_ALU1_RS, SEL_ALU2_RT, XXX, FN_ADD, false.B, false.B, false.B, true.B, SEL_REG_WADDR_RD, SEL_REG_WDATA_ALU, XXX),
    is(ADDI) -> Cat(SEL_ALU1_RS, SEL_ALU2_IMM, XXX, FN_ADD, false.B, false.B, false.B, true.B, SEL_REG_WADDR_RT, SEL_REG_WDATA_ALU, XXX),
  ))

  ctrl <> out.asTypeOf(new CtrlSigs)
}

object CU extends App {
  (new ChiselStage).emitVerilog(new CU)
}
