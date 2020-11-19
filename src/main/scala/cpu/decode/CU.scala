// See LICENSE for license details.

package cpu.decode

import chisel3._
import chisel3.util.{BitPat => B}
import cpu.decode.Instructions._
import cpu.execute.ALU._
import cpu.port.HILOWen
import cpu.util._

// @formatter:off
object CtrlSigDef {
  // Br
  val SZ_BR_TYPE = 3.W
  val BR_TYPE_NO = 0.U(SZ_BR_TYPE)
  val BR_TYPE_X = BR_TYPE_NO
  val BR_TYPE_EQ = 1.U(SZ_BR_TYPE)
  val BR_TYPE_NE = 2.U(SZ_BR_TYPE)
  val BR_TYPE_GE = 3.U(SZ_BR_TYPE)
  val BR_TYPE_GT = 4.U(SZ_BR_TYPE)
  val BR_TYPE_LE = 5.U(SZ_BR_TYPE)
  val BR_TYPE_LT = 6.U(SZ_BR_TYPE)

  // Mem
  val SZ_MEM_TYPE = 3.W
  val MEM_W = 0.U(SZ_MEM_TYPE)
  val MEM_X = MEM_W
  val MEM_H = 1.U(SZ_MEM_TYPE)
  val MEM_B = 2.U(SZ_MEM_TYPE)
  val MEM_HU = 3.U(SZ_MEM_TYPE) // 也可以拆成单独信号
  val MEM_BU = 4.U(SZ_MEM_TYPE)

  // Forward
  val SZ_FORWARD = 2.W
  val FORWARD_NO = 0.U(SZ_FORWARD)
  val FORWARD_EXE = 1.U(SZ_FORWARD)
  val FORWARD_MEM = 2.U(SZ_FORWARD)
  val FORWARD_WB = 3.U(SZ_FORWARD)

  // Forward HILO
  val SZ_FORWARD_HILO = 2.W
  val FORWARD_HILO_NO = 0.U(SZ_FORWARD_HILO)
  val FORWARD_HILO_MEM = 1.U(SZ_FORWARD_HILO)
  val FORWARD_HILO_WB = 2.U(SZ_FORWARD_HILO)

  // Select
  val SZ_SEL_ALU1 = 1.W
  val SEL_ALU1_SA = 0.U(SZ_SEL_ALU1)
  val SEL_ALU1_RS = 1.U(SZ_SEL_ALU1)
  val SEL_ALU1_X = SEL_ALU1_RS

  val SZ_SEL_ALU2 = 2.W
  val SEL_ALU2_IMM = 0.U(SZ_SEL_ALU2)
  val SEL_ALU2_RT = 1.U(SZ_SEL_ALU2)
  val SEL_ALU2_ZERO = 2.U(SZ_SEL_ALU2)
  val SEL_ALU2_X = SEL_ALU2_RT

  val SZ_SEL_IMM = 3.W
  val SEL_IMM_U = 0.U(SZ_SEL_IMM)
  val SEL_IMM_S = 1.U(SZ_SEL_IMM)
  val SEL_IMM_B = 2.U(SZ_SEL_IMM) // USB!
  val SEL_IMM_J = 3.U(SZ_SEL_IMM)
  val SEL_IMM_SH = 4.U(SZ_SEL_IMM)
  val SEL_IMM_LUI = 5.U(SZ_SEL_IMM)
  val SEL_IMM_X = SEL_IMM_U

  val SZ_SEL_REG_WADDR = 2.W
  val SEL_REG_WADDR_RD = 0.U(SZ_SEL_REG_WADDR)
  val SEL_REG_WADDR_RT = 1.U(SZ_SEL_REG_WADDR)
  val SEL_REG_WADDR_31 = 2.U(SZ_SEL_REG_WADDR)
  val SEL_REG_WADDR_X = SEL_REG_WADDR_RD

  val SZ_SEL_REG_WDATA = 3.W
  val SEL_REG_WDATA_ALU = 0.U(SZ_SEL_REG_WDATA)
  val SEL_REG_WDATA_MEM = 1.U(SZ_SEL_REG_WDATA)
  val SEL_REG_WDATA_LNK = 2.U(SZ_SEL_REG_WDATA)
  val SEL_REG_WDATA_HI = 3.U(SZ_SEL_REG_WDATA)
  val SEL_REG_WDATA_LO = 4.U(SZ_SEL_REG_WDATA)
  val SEL_REG_WDATA_X = SEL_REG_WDATA_ALU
}
// @formatter:on

import cpu.decode.CtrlSigDef._

class CtrlSigs extends Bundle with HILOWen {
  val sel_alu1 = UInt(SZ_SEL_ALU1)
  val sel_alu2 = UInt(SZ_SEL_ALU2)
  val sel_imm = UInt(SZ_SEL_IMM)
  val alu_fn = UInt(SZ_ALU_FN)
  val alu_n = Bool()
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

  private implicit def uint2B(x: UInt): B = B(x)

  private implicit def int2B(x: Int): B = B(x.U)

  private val default: List[B] = d(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_X, FN_X, 0, 0, 0, 0, SEL_REG_WADDR_X, SEL_REG_WDATA_X, BR_TYPE_X, MEM_X, 0, 0, 0)

  private val table: Array[(B, List[B])] = Array(
    ADD -> r(FN_ADD),
    ADDI -> i(FN_ADD),
    ADDU -> r(FN_ADD),
    ADDIU -> r(FN_ADD),
    SUB -> r(FN_SUB),
    SUBU -> r(FN_SUB),
    SLT -> r(FN_SLT),
    SLTI -> i(FN_SLT),
    SLTU -> r(FN_SLTU),
    SLTIU -> i(FN_SLTU),
    DIV -> m(FN_DIV, div = 1),
    DIVU -> m(FN_DIVU, div = 1),
    MULT -> m(FN_MULT, mul = 1),
    MULTU -> m(FN_MULTU, mul = 1),
    AND -> r(FN_AND),
    ANDI -> i(FN_AND),
    LUI -> i(FN_SL, lui = true),
    NOR -> r(FN_OR, 1),
    OR -> r(FN_OR),
    ORI -> i(FN_OR),
    XOR -> r(FN_XOR),
    XORI -> i(FN_XOR),
    SLLV -> sft(FN_SL, v = true),
    SLL -> sft(FN_SL),
    SRAV -> sft(FN_SRA, v = true),
    SRA -> sft(FN_SRA),
    SRLV -> sft(FN_SR),
    SRL -> sft(FN_SR, v = true),
    BEQ -> b(BR_TYPE_EQ),
    BGEZ -> b(BR_TYPE_GE),
    BGTZ -> b(BR_TYPE_GT),
    BLEZ -> b(BR_TYPE_LE),
    BLTZ -> b(BR_TYPE_LT),
    BGEZAL -> b(BR_TYPE_GE, 1),
    BLTZAL -> b(BR_TYPE_LT, 1),
    J -> j(),
    JR -> j(r = true),
    JAL -> j(l = 1),
    JALR -> j(r = true, 1),
    MFHI -> mf(),
    MFLO -> mf(false),
    MTHI -> mt(),
    MTLO -> mt(0),
    LB -> l(MEM_B),
    LBU -> l(MEM_BU),
    LH -> l(MEM_H),
    LHU -> l(MEM_HU),
    LW -> l(MEM_W),
    SB -> s(MEM_B),
    SH -> s(MEM_H),
    SW -> s(MEM_W),
  )

  private def r(alu_fn: B, alu_n: B = 0): List[B] = d(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_X, alu_fn, 0, 0, 0, 1, SEL_REG_WADDR_RD, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0, alu_n)

  private def mf(hi: Boolean = true): List[B] = d(SEL_ALU1_X, SEL_ALU2_X, SEL_IMM_X, FN_X, 0, 0, 0, 1, SEL_REG_WADDR_RD, if (hi) SEL_REG_WDATA_HI else SEL_REG_WDATA_LO, BR_TYPE_X, MEM_X, 0, 0, 0)

  private def mt(hi: Int = 1): List[B] = d(SEL_ALU1_RS, SEL_ALU2_X, SEL_IMM_X, FN_X, 0, 0, 0, 0, SEL_REG_WADDR_X, SEL_REG_WDATA_X, BR_TYPE_X, MEM_X, 0, hi, 1 - hi)

  private def i(alu_fn: B, lui: Boolean = false): List[B] = d(SEL_ALU1_RS, SEL_ALU2_IMM, if (lui) SEL_IMM_LUI else SEL_IMM_S, alu_fn, 0, 0, 0, 1, SEL_REG_WADDR_RT, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0)

  private def l(mem_size: B): List[B] = d(SEL_ALU1_RS, SEL_ALU2_IMM, SEL_IMM_S, FN_ADD, 0, 0, 0, 1, SEL_REG_WADDR_RT, SEL_REG_WDATA_MEM, BR_TYPE_NO, mem_size, 1, 0, 0)

  private def m(alu_fn: B, div: Int = 0, mul: Int = 0): List[B] = d(SEL_ALU1_RS, SEL_ALU2_IMM, SEL_IMM_S, alu_fn, mul, div, 0, 1, SEL_REG_WADDR_RT, SEL_REG_WDATA_MEM, BR_TYPE_NO, MEM_X, 1, 0, 0)

  private def d(sel_alu1: B, sel_alu2: B, sel_imm: B, alu_fn: B, mul: B, div: B, mem_wen: B, reg_wen: B, sel_reg_waddr: B, sel_reg_wdata: B, br_type: B, mem_size: B, load: B, hi_wen: B, lo_wen: B, alu_n: B = 0): List[B] = List(sel_alu1, sel_alu2, sel_imm, alu_fn, alu_n, mul, div, mem_wen, reg_wen, sel_reg_waddr, sel_reg_wdata, br_type, mem_size, load, hi_wen, lo_wen)

  private def s(mem_size: B): List[B] = d(SEL_ALU1_RS, SEL_ALU2_IMM, SEL_IMM_S, FN_ADD, 0, 0, 1, 0, SEL_REG_WADDR_X, SEL_REG_WDATA_X, BR_TYPE_NO, mem_size, 0, 0, 0)

  private def sft(alu_fn: B, v: Boolean = false): List[B] = d(if (v) SEL_ALU1_RS else SEL_ALU1_SA, SEL_ALU2_RT, SEL_IMM_SH, alu_fn, 0, 0, 0, 1, SEL_REG_WADDR_RD, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0)

  private def b(br_type: B, link: Int = 0): List[B] = d(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_B, FN_SLT, 0, 0, 0, link, SEL_REG_WADDR_31, SEL_REG_WDATA_LNK, br_type, MEM_X, 0, 0, 0)

  private def j(r: Boolean = false, l: Int = 0): List[B] = d(if (r) SEL_ALU1_SA else SEL_ALU1_X, SEL_ALU2_X, SEL_IMM_J, FN_X, 0, 0, 0, l, SEL_REG_WADDR_31, SEL_REG_WDATA_LNK, BR_TYPE_X, MEM_X, 0, 0, 0)

  def decode(inst: UInt): this.type = {
    val decoder = DecodeLogic(inst, default, table)
    val sigs = Seq(sel_alu1, sel_alu2, sel_imm, alu_fn, alu_n, mul, div, mem_wen,
      reg_wen, sel_reg_waddr, sel_reg_wdata, br_type, mem_size, load, hi_wen, lo_wen)
    sigs zip decoder foreach { case (s, d) => s := d }
    this
  }
}

class CU(implicit c: Config = DefCon) extends MultiIOModule {
  val inst = IO(Input(UInt(32.W)))
  val ctrl = IO(Output(new CtrlSigs))

  ctrl := Wire(new CtrlSigs).decode(inst)
}