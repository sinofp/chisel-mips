// See LICENSE for license details.

package cpu.decode

import chisel3._
import chisel3.util.{BitPat => B}
import cpu.decode.CtrlSigDef._
import cpu.decode.Instructions._
import cpu.execute.ALU._
import cpu.util._

class CtrlSigs extends Bundle {
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
  val hi_wen = Bool()
  val lo_wen = Bool()
  val c0_wen = Bool()
  val sel_move = UInt(SZ_SEL_MOVE)
  val inst_invalid = Bool()
  val is_syscall = Bool()
  val is_eret = Bool()
  val is_break = Bool()
  val check_overflow = Bool()
  val data_sram_en = Bool()

  private implicit def uint2B(x: UInt): B = B(x)

  private implicit def int2B(x: Int): B = B(x.U)

  private val table: Array[(B, List[B])] = Array(
    ADD -> r(FN_ADD, check_overflow = 1),
    ADDI -> i(FN_ADD, check_overflow = 1),
    ADDU -> r(FN_ADD),
    ADDIU -> i(FN_ADD),
    SUB -> r(FN_SUB, check_overflow = 1),
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
    LUI -> i(FN_ADD, lui = true),
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
    BREAK -> dft(is_break = 1),
    SYSCALL -> dft(is_syscall = 1),
    LB -> l(MEM_B),
    LBU -> l(MEM_BU),
    LH -> l(MEM_H),
    LHU -> l(MEM_HU),
    LW -> l(MEM_W),
    SB -> s(MEM_B),
    SH -> s(MEM_H),
    SW -> s(MEM_W),
    ERET -> dft(is_eret = 1),
    MFC0 -> mf(c0 = true),
    MTC0 -> mt(c0 = 1),
  )

  def decode(inst: UInt): this.type = {
    val decoder = DecodeLogic(inst, dft(inst_invalid = 1), table)
    val sigs = Seq(sel_alu1, sel_alu2, sel_imm, alu_fn, alu_n, mul, div, mem_wen, reg_wen, sel_reg_waddr, sel_reg_wdata, br_type, mem_size, load, hi_wen, lo_wen, c0_wen, sel_move, inst_invalid, is_eret, is_syscall, is_break, check_overflow, data_sram_en)
    sigs zip decoder foreach { case (s, d) => s := d }
    this
  }

  private def r(alu_fn: B, alu_n: B = 0, check_overflow: B = 0): List[B] = dft(alu_fn = alu_fn, alu_n = alu_n, reg_wen = 1, sel_reg_waddr = SEL_REG_WADDR_RD, sel_reg_wdata = SEL_REG_WDATA_EX, check_overflow = check_overflow)

  private def mf(hi: Boolean = false, lo: Boolean = false, c0: Boolean = false): List[B] = dft(reg_wen = 1, sel_reg_waddr = if (c0) SEL_REG_WADDR_RT else SEL_REG_WADDR_RD, sel_reg_wdata = SEL_REG_WDATA_EX, sel_move = if (c0) SEL_MOVE_C0 else if (hi) SEL_MOVE_HI else SEL_MOVE_LO)

  private def mt(hi: Int = 0, lo: Int = 0, c0: Int = 0): List[B] = dft(hi_wen = hi, lo_wen = lo, c0_wen = c0)

  private def l(mem_size: B): List[B] = dft(sel_alu2 = SEL_ALU2_IMM, sel_imm = SEL_IMM_S, alu_fn = FN_ADD, reg_wen = 1, sel_reg_waddr = SEL_REG_WADDR_RT, sel_reg_wdata = SEL_REG_WDATA_MEM, mem_size = mem_size, load = 1, data_sram_en = 1)

  private def i(alu_fn: B, lui: Boolean = false, check_overflow: B = 0): List[B] = dft(sel_alu2 = SEL_ALU2_IMM, sel_imm = if (lui) SEL_IMM_LUI else SEL_IMM_S, alu_fn = alu_fn, reg_wen = 1, sel_reg_waddr = SEL_REG_WADDR_RT, sel_reg_wdata = SEL_REG_WDATA_EX, check_overflow = check_overflow)

  private def dft(sel_alu1: B = SEL_ALU1_RS, sel_alu2: B = SEL_ALU2_RT, sel_imm: B = SEL_IMM_U, alu_fn: B = FN_X, mul: B = 0, div: B = 0, mem_wen: B = 0, reg_wen: B = 0, sel_reg_waddr: B = SEL_REG_WADDR_RD, sel_reg_wdata: B = SEL_REG_WDATA_EX, br_type: B = BR_TYPE_NO, mem_size: B = MEM_W, load: B = 0, hi_wen: B = 0, lo_wen: B = 0, alu_n: B = 0, c0_wen: B = 0, sel_move: B = SEL_MOVE_NO, inst_invalid: B = 0, is_eret: B = 0, is_syscall: B = 0, is_break: B = 0, check_overflow: B = 0, data_sram_en: B = 0): List[B] = List(sel_alu1, sel_alu2, sel_imm, alu_fn, alu_n, mul, div, mem_wen, reg_wen, sel_reg_waddr, sel_reg_wdata, br_type, mem_size, load, hi_wen, lo_wen, c0_wen, sel_move, inst_invalid, is_eret, is_syscall, is_break, check_overflow, data_sram_en)

  private def m(alu_fn: B, div: Int = 0, mul: Int = 0): List[B] = dft(sel_alu2 = SEL_ALU2_IMM, sel_imm = SEL_IMM_S, alu_fn = alu_fn, mul = mul, div = div, reg_wen = 1, sel_reg_waddr = SEL_REG_WADDR_RT, sel_reg_wdata = SEL_REG_WDATA_MEM)

  private def s(mem_size: B): List[B] = dft(sel_alu2 = SEL_ALU2_IMM, sel_imm = SEL_IMM_S, alu_fn = FN_ADD, mem_wen = 1, mem_size = mem_size, data_sram_en = 1)

  private def sft(alu_fn: B, v: Boolean = false): List[B] = dft(sel_alu1 = if (v) SEL_ALU1_RS else SEL_ALU1_SA, sel_alu2 = SEL_IMM_SH, alu_fn = alu_fn, reg_wen = 1, sel_reg_waddr = SEL_REG_WADDR_RD, sel_reg_wdata = SEL_REG_WDATA_EX)

  private def b(br_type: B, link: Int = 0): List[B] = dft(sel_imm = SEL_IMM_B, alu_fn = FN_SLT, reg_wen = link, sel_reg_waddr = SEL_REG_WADDR_31, sel_reg_wdata = SEL_REG_WDATA_LNK, br_type = br_type)

  private def j(r: Boolean = false, l: Int = 0): List[B] = dft(sel_alu1 = if (r) SEL_ALU1_SA else SEL_ALU1_RS, sel_imm = SEL_IMM_J, reg_wen = l, sel_reg_waddr = SEL_REG_WADDR_31, sel_reg_wdata = SEL_REG_WDATA_LNK)
}

class CU(implicit c: Config = DefCon) extends MultiIOModule {
  val inst = IO(Input(UInt(32.W)))
  val ctrl = IO(Output(new CtrlSigs))

  ctrl := Wire(new CtrlSigs).decode(inst)
}
