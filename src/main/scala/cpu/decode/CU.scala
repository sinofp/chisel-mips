// See LICENSE for license details.

package cpu.decode

import chisel3._
import chisel3.util._
import cpu.decode.Instructions._
import cpu.execute.ALU._
import cpu.util._

// @formatter:off
object CtrlSigDef {
  // Br
  val SZ_BR_TYPE = 3.W
  val BR_TYPE_NO = 0.U(SZ_BR_TYPE)
  val BR_TYPE_EQ = 1.U(SZ_BR_TYPE)
  val BR_TYPE_NE = 2.U(SZ_BR_TYPE)
  val BR_TYPE_GE = 3.U(SZ_BR_TYPE)
  val BR_TYPE_GT = 4.U(SZ_BR_TYPE)
  val BR_TYPE_LE = 5.U(SZ_BR_TYPE)
  val BR_TYPE_LT = 6.U(SZ_BR_TYPE)

  // Mem
  val SZ_MEM_TYPE = 2.W
  val MEM_WORD = 0.U(SZ_MEM_TYPE)
  val MEM_HALF = 1.U(SZ_MEM_TYPE)
  val MEM_BYTE = 2.U(SZ_MEM_TYPE)

  // Forward
  val SZ_FORWARD = 2.W
  val FORWARD_DEF = 0.U(SZ_FORWARD)
  val FORWARD_EXE = 1.U(SZ_FORWARD)
  val FORWARD_MEM = 2.U(SZ_FORWARD)
  val FORWARD_WB = 3.U(SZ_FORWARD)

  // Select
  val SZ_SEL_ALU1 = 2.W
  val SEL_ALU1_SA = 0.U(SZ_SEL_ALU1) // todo 没别的就改成Bool()
  val SEL_ALU1_RS = 1.U(SZ_SEL_ALU1)

  val SZ_SEL_ALU2 = 2.W
  val SEL_ALU2_IMM = 0.U(SZ_SEL_ALU2)
  val SEL_ALU2_RT = 1.U(SZ_SEL_ALU2)
  val SEL_ALU2_ZERO = 2.U(SZ_SEL_ALU2)

  val SZ_SEL_IMM = 3.W
  val SEL_IMM_U = 0.U(SZ_SEL_IMM)
  val SEL_IMM_S = 1.U(SZ_SEL_IMM)
  val SEL_IMM_B = 2.U(SZ_SEL_IMM) // USB!
  val SEL_IMM_J = 3.U(SZ_SEL_IMM)
  val SEL_IMM_SH = 4.U(SZ_SEL_IMM)

  val SZ_SEL_REG_WADDR = 2.W
  val SEL_REG_WADDR_RD = 0.U(SZ_SEL_REG_WADDR)
  val SEL_REG_WADDR_RT = 1.U(SZ_SEL_REG_WADDR)
  val SEL_REG_WADDR_31 = 2.U(SZ_SEL_REG_WADDR)

  val SZ_SEL_REG_WDATA = 2.W
  val SEL_REG_WDATA_ALU = 0.U(SZ_SEL_REG_WDATA)
  val SEL_REG_WDATA_MEM = 1.U(SZ_SEL_REG_WDATA)
  val SEL_REG_WDATA_LNK = 2.U(SZ_SEL_REG_WDATA)

  val X = BitPat(0.U)
  val XX = BitPat(0.U(2.W))
  val XXX = BitPat(0.U(3.W))
}
// @formatter:on

import cpu.decode.CtrlSigDef._

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
  val mem_size = UInt(SZ_MEM_TYPE)
  val load = Bool()
  // todo ...?

  implicit def uint2BitPat(x: UInt): BitPat = BitPat(x)

  implicit def int2BitPat(x: Int): BitPat = BitPat(x.U)

  private val default: List[BitPat] = List(SEL_ALU1_RS, SEL_ALU2_RT, XXX, FN_X, 0, 0, 0, 0, XX, XX, XXX, MEM_WORD, X)

  private val table: Array[(BitPat, List[BitPat])] = Array(
    ADD -> List(SEL_ALU1_RS, SEL_ALU2_RT, XXX, FN_ADD, 0, 0, 0, 1, SEL_REG_WADDR_RD, SEL_REG_WDATA_ALU, XXX, XX, 0),
    ADDI -> List(SEL_ALU1_RS, SEL_ALU2_IMM, SEL_IMM_S, FN_ADD, 0, 0, 0, 1, SEL_REG_WADDR_RT, SEL_REG_WDATA_ALU, XXX, XX, 0),
    SUB -> List(SEL_ALU1_RS, SEL_ALU2_RT, XXX, FN_SUB, 0, 0, 0, 1, SEL_REG_WADDR_RD, SEL_REG_WDATA_ALU, XXX, XX, 0),
    BEQ -> List(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_B, FN_SLT, 0, 0, 0, 0, XX, XX, BR_TYPE_EQ, XX),
    BLTZ -> List(SEL_ALU1_RS, SEL_ALU2_ZERO, SEL_IMM_B, FN_SLT, 0, 0, 0, 0, XX, XX, BR_TYPE_LT, XX),
    LW -> List(SEL_ALU1_RS, SEL_ALU2_IMM, SEL_IMM_S, FN_ADD, 0, 0, 0, 1, SEL_REG_WADDR_RT, SEL_REG_WDATA_MEM, BR_TYPE_NO, MEM_WORD, 1),
    SW -> List(SEL_ALU1_RS, SEL_ALU2_IMM, SEL_IMM_S, FN_ADD, 0, 0, 1, 0, XX, XX, BR_TYPE_NO, MEM_WORD, 0),
  )

  def decode(inst: UInt): this.type = {
    val decoder = DecodeLogic(inst, default, table)
    val sigs = Seq(sel_alu1, sel_alu2, sel_imm, alu_fn, mul, div, mem_wen,
      reg_wen, sel_reg_waddr, sel_reg_wdata, br_type, mem_size, load)
    sigs zip decoder foreach { case (s, d) => s := d }
    this
  }
}

class CU(implicit c: Config = DefCon) extends MultiIOModule {
  val inst = IO(Input(UInt(32.W)))
  val ctrl = IO(Output(new CtrlSigs))

  ctrl := Wire(new CtrlSigs).decode(inst)
}