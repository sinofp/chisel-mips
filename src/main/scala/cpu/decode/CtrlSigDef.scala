// See LICENSE for license details.

package cpu.decode

import chisel3._

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

  // Forward CP0
  val SZ_FORWARD_C0 = 2.W
  val FORWARD_C0_NO = 0.U(SZ_FORWARD_C0)
  val FORWARD_C0_MEM = 1.U(SZ_FORWARD_C0)
  val FORWARD_C0_WB = 2.U(SZ_FORWARD_C0)

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
