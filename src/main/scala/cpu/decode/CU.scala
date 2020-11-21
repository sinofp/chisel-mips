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
  val BR_TYPE_EQ = 1.U(SZ_BR_TYPE)
  val BR_TYPE_NE = 2.U(SZ_BR_TYPE)
  val BR_TYPE_GE = 3.U(SZ_BR_TYPE)
  val BR_TYPE_GT = 4.U(SZ_BR_TYPE)
  val BR_TYPE_LE = 5.U(SZ_BR_TYPE)
  val BR_TYPE_LT = 6.U(SZ_BR_TYPE)

  // Mem
  val SZ_MEM_TYPE = 3.W
  val MEM_W = 0.U(SZ_MEM_TYPE)
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
  val SEL_IMM_LUI = 5.U(SZ_SEL_IMM)

  val SZ_SEL_REG_WADDR = 2.W
  val SEL_REG_WADDR_RD = 0.U(SZ_SEL_REG_WADDR)
  val SEL_REG_WADDR_RT = 1.U(SZ_SEL_REG_WADDR)
  val SEL_REG_WADDR_31 = 2.U(SZ_SEL_REG_WADDR)

  val SZ_SEL_REG_WDATA = 3.W
  val SEL_REG_WDATA_EX = 0.U(SZ_SEL_REG_WDATA)
  val SEL_REG_WDATA_MEM = 1.U(SZ_SEL_REG_WDATA)
  val SEL_REG_WDATA_LNK = 2.U(SZ_SEL_REG_WDATA)

  val SZ_SEL_MOVE = 2.W
  val SEL_MOVE_NO = 0.U(SZ_SEL_MOVE)
  val SEL_MOVE_HI = 1.U(SZ_SEL_MOVE)
  val SEL_MOVE_LO = 2.U(SZ_SEL_MOVE)
  val SEL_MOVE_C0 = 3.U(SZ_SEL_MOVE)
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
  val c0_wen = Bool()
  val sel_move = UInt(SZ_SEL_MOVE)

  private implicit def uint2B(x: UInt): B = B(x)

  private implicit def int2B(x: Int): B = B(x.U)

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
    MFHI -> mf(hi = true),
    MFLO -> mf(lo = true),
    MTHI -> mt(hi = 1),
    MTLO -> mt(lo = 1),
    LB -> l(MEM_B),
    LBU -> l(MEM_BU),
    LH -> l(MEM_H),
    LHU -> l(MEM_HU),
    LW -> l(MEM_W),
    SB -> s(MEM_B),
    SH -> s(MEM_H),
    SW -> s(MEM_W),
    // eret
    MFC0 -> mf(c0 = true),
    MTC0 -> mt(c0 = 1),
  )

  def decode(inst: UInt): this.type = {
    val decoder = DecodeLogic(inst, dft(), table)
    val sigs = Seq(sel_alu1, sel_alu2, sel_imm, alu_fn, alu_n, mul, div, mem_wen,
      reg_wen, sel_reg_waddr, sel_reg_wdata, br_type, mem_size, load,
      hi_wen, lo_wen, c0_wen, sel_move)
    sigs zip decoder foreach { case (s, d) => s := d }
    this
  }

  private def r(alu_fn: B, alu_n: B = 0): List[B] = dft(alu_fn = alu_fn, alu_n = alu_n, reg_wen = 1, sel_reg_waddr = SEL_REG_WADDR_RD, sel_reg_wdata = SEL_REG_WDATA_EX)

  private def mf(hi: Boolean = false, lo: Boolean = false, c0: Boolean = false): List[B] = dft(reg_wen = 1, sel_reg_waddr = SEL_REG_WADDR_RD, sel_reg_wdata = SEL_REG_WDATA_EX, sel_move = if (c0) SEL_MOVE_C0 else {
    if (hi) SEL_MOVE_HI else SEL_MOVE_LO
  })

  private def mt(hi: Int = 0, lo: Int = 0, c0: Int = 0): List[B] = dft(hi_wen = hi, lo_wen = lo, c0_wen = c0)

  private def dft(sel_alu1: B = SEL_ALU1_RS, sel_alu2: B = SEL_ALU2_RT, sel_imm: B = SEL_IMM_U, alu_fn: B = FN_X, mul: B = 0, div: B = 0, mem_wen: B = 0, reg_wen: B = 0, sel_reg_waddr: B = SEL_REG_WADDR_RD, sel_reg_wdata: B = SEL_REG_WDATA_EX, br_type: B = BR_TYPE_NO, mem_size: B = MEM_W, load: B = 0, hi_wen: B = 0, lo_wen: B = 0, alu_n: B = 0, c0_wen: B = 0, sel_move: B = SEL_MOVE_NO): List[B] = List(sel_alu1, sel_alu2, sel_imm, alu_fn, alu_n, mul, div, mem_wen, reg_wen, sel_reg_waddr, sel_reg_wdata, br_type, mem_size, load, hi_wen, lo_wen, c0_wen, sel_move)

  private def i(alu_fn: B, lui: Boolean = false): List[B] = dft(sel_alu2 = SEL_ALU2_IMM, sel_imm = if (lui) SEL_IMM_LUI else SEL_IMM_S, alu_fn = alu_fn, reg_wen = 1, sel_reg_waddr = SEL_REG_WADDR_RT, sel_reg_wdata = SEL_REG_WDATA_EX)

  private def l(mem_size: B): List[B] = dft(sel_alu2 = SEL_ALU2_IMM, sel_imm = SEL_IMM_S, alu_fn = FN_ADD, reg_wen = 1, sel_reg_waddr = SEL_REG_WADDR_RT, sel_reg_wdata = SEL_REG_WDATA_MEM, mem_size = mem_size, load = 1)

  private def m(alu_fn: B, div: Int = 0, mul: Int = 0): List[B] = dft(sel_alu2 = SEL_ALU2_IMM, sel_imm = SEL_IMM_S, alu_fn = alu_fn, mul = mul, div = div, reg_wen = 1, sel_reg_waddr = SEL_REG_WADDR_RT, sel_reg_wdata = SEL_REG_WDATA_MEM)

  private def s(mem_size: B): List[B] = dft(sel_alu2 = SEL_ALU2_IMM, sel_imm = SEL_IMM_S, alu_fn = FN_ADD, mem_wen = 1, mem_size = mem_size)

  private def sft(alu_fn: B, v: Boolean = false): List[B] = dft(sel_alu1 = if (v) SEL_ALU1_RS else SEL_ALU1_SA, sel_alu2 = SEL_IMM_SH, alu_fn = alu_fn, reg_wen = 1, sel_reg_waddr = SEL_REG_WADDR_RD, sel_reg_wdata = SEL_REG_WDATA_EX)

  private def b(br_type: B, link: Int = 0): List[B] = dft(sel_imm = SEL_IMM_B, alu_fn = FN_SLT, reg_wen = link, sel_reg_waddr = SEL_REG_WADDR_31, sel_reg_wdata = SEL_REG_WDATA_LNK, br_type = br_type)

  private def j(r: Boolean = false, l: Int = 0): List[B] = dft(sel_alu1 = if (r) SEL_ALU1_SA else SEL_ALU1_RS, sel_imm = SEL_IMM_J, reg_wen = l, sel_reg_waddr = SEL_REG_WADDR_31, sel_reg_wdata = SEL_REG_WDATA_LNK)
}

class CU(implicit c: Config = DefCon) extends MultiIOModule {
  val inst = IO(Input(UInt(32.W)))
  val ctrl = IO(Output(new CtrlSigs))

  ctrl := Wire(new CtrlSigs).decode(inst)
}
