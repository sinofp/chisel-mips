// See LICENSE for license details.

package cpu.decode

import chisel3._
import chisel3.stage.ChiselStage
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
  val SZ_MEM_TYPE = 2.W
  val MEM_WORD = 0.U(SZ_MEM_TYPE)
  val MEM_X = MEM_WORD
  val MEM_HALF = 1.U(SZ_MEM_TYPE)
  val MEM_BYTE = 2.U(SZ_MEM_TYPE)

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

  private val default: List[B] = l(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_X, FN_X, 0, 0, 0, 0, SEL_REG_WADDR_X, SEL_REG_WDATA_X, BR_TYPE_X, MEM_X, 0, 0, 0)

  private val table: Array[(B, List[B])] = Array(
    ADD -> l(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_X, FN_ADD, 0, 0, 0, 1, SEL_REG_WADDR_RD, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0),
    ADDI -> l(SEL_ALU1_RS, SEL_ALU2_IMM, SEL_IMM_S, FN_ADD, 0, 0, 0, 1, SEL_REG_WADDR_RT, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0),
    ADDU -> l(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_X, FN_ADD, 0, 0, 0, 1, SEL_REG_WADDR_RD, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0),
    ADDIU -> l(SEL_ALU1_RS, SEL_ALU2_IMM, SEL_IMM_S, FN_ADD, 0, 0, 0, 1, SEL_REG_WADDR_RT, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0),
    SUB -> l(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_X, FN_SUB, 0, 0, 0, 1, SEL_REG_WADDR_RD, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0),
    SUBU -> l(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_X, FN_SUB, 0, 0, 0, 1, SEL_REG_WADDR_RD, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0),
    SLT -> l(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_X, FN_SLT, 0, 0, 0, 1, SEL_REG_WADDR_RD, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0),
    SLTI -> l(SEL_ALU1_RS, SEL_ALU2_IMM, SEL_IMM_S, FN_SLT, 0, 0, 0, 1, SEL_REG_WADDR_RT, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0),
    SLTU -> l(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_X, FN_SLTU, 0, 0, 0, 1, SEL_REG_WADDR_RD, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0),
    SLTIU -> l(SEL_ALU1_RS, SEL_ALU2_IMM, SEL_IMM_S, FN_SLTU, 0, 0, 0, 1, SEL_REG_WADDR_RT, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0),
    DIV -> l(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_X, FN_DIV, 0, 1, 0, 0, SEL_REG_WADDR_X, SEL_REG_WDATA_X, BR_TYPE_X, MEM_X, 0, 1, 1),
    DIVU -> l(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_X, FN_DIVU, 0, 1, 0, 0, SEL_REG_WADDR_X, SEL_REG_WDATA_X, BR_TYPE_X, MEM_X, 0, 1, 1),
    MULT -> l(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_X, FN_MULT, 1, 0, 0, 0, SEL_REG_WADDR_X, SEL_REG_WDATA_X, BR_TYPE_X, MEM_X, 0, 1, 1),
    MULTU -> l(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_X, FN_MULTU, 1, 0, 0, 0, SEL_REG_WADDR_X, SEL_REG_WDATA_X, BR_TYPE_X, MEM_X, 0, 1, 1),
    AND -> l(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_X, FN_AND, 0, 0, 0, 1, SEL_REG_WADDR_RD, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0),
    ANDI -> l(SEL_ALU1_RS, SEL_ALU2_IMM, SEL_IMM_S, FN_AND, 0, 0, 0, 1, SEL_REG_WADDR_RT, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0),
    LUI -> l(SEL_ALU1_RS, SEL_ALU2_IMM, SEL_IMM_U, FN_SL, 0, 0, 0, 1, SEL_REG_WADDR_RT, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0),
    //    NOR -> l(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_X, FN_NOR, 0, 0, 0, 1, SEL_REG_WADDR_RD, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0),
    OR -> l(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_X, FN_OR, 0, 0, 0, 1, SEL_REG_WADDR_RD, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0),
    ORI -> l(SEL_ALU1_RS, SEL_ALU2_IMM, SEL_IMM_S, FN_OR, 0, 0, 0, 1, SEL_REG_WADDR_RT, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0),
    XOR -> l(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_X, FN_XOR, 0, 0, 0, 1, SEL_REG_WADDR_RD, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0),
    XORI -> l(SEL_ALU1_RS, SEL_ALU2_IMM, SEL_IMM_S, FN_XOR, 0, 0, 0, 1, SEL_REG_WADDR_RT, SEL_REG_WDATA_ALU, BR_TYPE_X, MEM_X, 0, 0, 0),
    BEQ -> l(SEL_ALU1_RS, SEL_ALU2_RT, SEL_IMM_B, FN_SLT, 0, 0, 0, 0, SEL_REG_WADDR_X, SEL_REG_WDATA_X, BR_TYPE_NE, MEM_X, 0, 0, 0),
    BGEZ -> l(SEL_ALU1_RS, SEL_ALU2_ZERO, SEL_IMM_B, FN_SLT, 0, 0, 0, 0, SEL_REG_WADDR_X, SEL_REG_WDATA_X, BR_TYPE_GE, MEM_X, 0, 0, 0),
    BGTZ -> l(SEL_ALU1_RS, SEL_ALU2_ZERO, SEL_IMM_B, FN_SLT, 0, 0, 0, 0, SEL_REG_WADDR_X, SEL_REG_WDATA_X, BR_TYPE_GT, MEM_X, 0, 0, 0),
    BLEZ -> l(SEL_ALU1_RS, SEL_ALU2_ZERO, SEL_IMM_B, FN_SLT, 0, 0, 0, 0, SEL_REG_WADDR_X, SEL_REG_WDATA_X, BR_TYPE_LE, MEM_X, 0, 0, 0),
    BLTZ -> l(SEL_ALU1_RS, SEL_ALU2_ZERO, SEL_IMM_B, FN_SLT, 0, 0, 0, 0, SEL_REG_WADDR_X, SEL_REG_WDATA_X, BR_TYPE_LT, MEM_X, 0, 0, 0),
    BGEZAL -> l(SEL_ALU1_RS, SEL_ALU2_ZERO, SEL_IMM_B, FN_SLT, 0, 0, 0, 1, SEL_REG_WADDR_31, SEL_REG_WDATA_LNK, BR_TYPE_GE, MEM_X, 0, 0, 0),
    BLTZAL -> l(SEL_ALU1_RS, SEL_ALU2_ZERO, SEL_IMM_B, FN_SLT, 0, 0, 0, 1, SEL_REG_WADDR_31, SEL_REG_WDATA_LNK, BR_TYPE_LT, MEM_X, 0, 0, 0),
    J -> l(SEL_ALU1_X, SEL_ALU2_X, SEL_IMM_J, FN_X, 0, 0, 0, 0, SEL_REG_WADDR_X, SEL_REG_WDATA_X, BR_TYPE_X, MEM_X, 0, 0, 0),
    JR -> l(SEL_ALU1_SA, SEL_ALU2_X, SEL_IMM_J, FN_X, 0, 0, 0, 0, SEL_REG_WADDR_X, SEL_REG_WDATA_X, BR_TYPE_X, MEM_X, 0, 0, 0),
    JAL -> l(SEL_ALU1_X, SEL_ALU2_X, SEL_IMM_J, FN_X, 0, 0, 0, 1, SEL_REG_WADDR_31, SEL_REG_WDATA_LNK, BR_TYPE_X, MEM_X, 0, 0, 0),
    JALR -> l(SEL_ALU1_SA, SEL_ALU2_X, SEL_IMM_J, FN_X, 0, 0, 0, 1, SEL_REG_WADDR_31, SEL_REG_WDATA_LNK, BR_TYPE_X, MEM_X, 0, 0, 0),
    MFHI -> l(SEL_ALU1_X, SEL_ALU2_X, SEL_IMM_X, FN_X, 0, 0, 0, 1, SEL_REG_WADDR_RD, SEL_REG_WDATA_HI, BR_TYPE_X, MEM_X, 0, 0, 0),
    MFLO -> l(SEL_ALU1_X, SEL_ALU2_X, SEL_IMM_X, FN_X, 0, 0, 0, 1, SEL_REG_WADDR_RD, SEL_REG_WDATA_LO, BR_TYPE_X, MEM_X, 0, 0, 0),
    MTHI -> l(SEL_ALU1_RS, SEL_ALU2_X, SEL_IMM_X, FN_X, 0, 0, 0, 0, SEL_REG_WADDR_X, SEL_REG_WDATA_X, BR_TYPE_X, MEM_X, 0, 1, 0),
    MTLO -> l(SEL_ALU1_RS, SEL_ALU2_X, SEL_IMM_X, FN_X, 0, 0, 0, 0, SEL_REG_WADDR_X, SEL_REG_WDATA_X, BR_TYPE_X, MEM_X, 0, 0, 1),
    LW -> l(SEL_ALU1_RS, SEL_ALU2_IMM, SEL_IMM_S, FN_ADD, 0, 0, 0, 1, SEL_REG_WADDR_RT, SEL_REG_WDATA_MEM, BR_TYPE_NO, MEM_WORD, 1, 0, 0),
    SW -> l(SEL_ALU1_RS, SEL_ALU2_IMM, SEL_IMM_S, FN_ADD, 0, 0, 1, 0, SEL_REG_WADDR_X, SEL_REG_WDATA_X, BR_TYPE_NO, MEM_WORD, 0, 0, 0),
  )

  // 为了IDE的参数提醒
  private def l(sel_alu1: B, sel_alu2: B, sel_imm: B, alu_fn: B, mul: B, div: B, mem_wen: B, reg_wen: B, sel_reg_waddr: B, sel_reg_wdata: B, br_type: B, mem_size: B, load: B, hi_wen: B, lo_wen: B): List[B] = {
    List(sel_alu1, sel_alu2, sel_imm, alu_fn, mul, div, mem_wen, reg_wen, sel_reg_waddr, sel_reg_wdata, br_type, mem_size, load, hi_wen, lo_wen)
  }

  def decode(inst: UInt): this.type = {
    val decoder = DecodeLogic(inst, default, table)
    val sigs = Seq(sel_alu1, sel_alu2, sel_imm, alu_fn, mul, div, mem_wen,
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