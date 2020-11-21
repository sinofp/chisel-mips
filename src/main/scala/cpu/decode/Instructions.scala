package cpu.decode

import chisel3.util.BitPat

object Instructions {
  // @formatter:off
  // 算术运算指令
  val ADD = BitPat("b000000_?????_?????_?????_00000_100000")
  val ADDI = BitPat("b001000_?????_?????_????????????????")
  val ADDU = BitPat("b000000_?????_?????_?????_00000_100001")
  val ADDIU = BitPat("b001001_?????_?????_????????????????")
  val SUB = BitPat("b000000_?????_?????_?????_00000_100010")
  val SUBU = BitPat("b000000_?????_?????_?????_00000_100011")
  val SLT = BitPat("b000000_?????_?????_?????_00000_101010")
  val SLTI = BitPat("b001010_?????_?????_????????????????")
  val SLTU = BitPat("b000000_?????_?????_?????_00000_101011")
  val SLTIU = BitPat("b001011_?????_?????_????????????????")
  val DIV = BitPat("b000000_?????_?????_0000000000_011010")
  val DIVU = BitPat("b000000_?????_?????_0000000000_011011")
  val MULT = BitPat("b000000_?????_?????_0000000000_011000")
  val MULTU = BitPat("b000000_?????_?????_0000000000_011001")

  // 逻辑运算指令
  val AND = BitPat("b000000_?????_?????_?????_00000_100100")
  val ANDI = BitPat("b001100_?????_?????_????????????????")
  val LUI = BitPat("b001111_00000_?????_????????????????")
  val NOR = BitPat("b000000_?????_?????_?????_00000_100111")
  val OR = BitPat("b000000_?????_?????_?????_00000_100101")
  val ORI = BitPat("b001101_?????_?????_????????????????")
  val XOR = BitPat("b000000_?????_?????_?????_00000_100110")
  val XORI = BitPat("b001110_?????_?????_????????????????")

  // 移位指令
  val SLLV = BitPat("b000000_?????_?????_?????_00000_000100")
  val SLL = BitPat("b000000_00000_?????_?????_?????_000000")
  val SRAV = BitPat("b000000_?????_?????_?????_00000_000111")
  val SRA = BitPat("b000000_00000_?????_?????_?????_000011")
  val SRLV = BitPat("b000000_?????_?????_?????_00000_000110")
  val SRL = BitPat("b000000_00000_?????_?????_?????_000010")

  // 分支跳转指令
  val BEQ = BitPat("b000100_?????_?????_????????????????")
  val BNE = BitPat("b000101_?????_?????_????????????????")
  val BGEZ = BitPat("b000001_?????_00001_????????????????")
  val BGTZ = BitPat("b000111_?????_00000_????????????????")
  val BLEZ = BitPat("b000110_?????_00000_????????????????")
  val BLTZ = BitPat("b000001_?????_00000_????????????????")
  val BGEZAL = BitPat("b000001_?????_10001_????????????????")
  val BLTZAL = BitPat("b000001_?????_10000_????????????????")
  val J = BitPat("b000010_" + "?"*26)
  val JAL = BitPat("b000011_" + "?"*26)
  val JR = BitPat("b000000_?????_0000000000_00000_001000")
  val JALR = BitPat("b000000_?????_00000_?????_00000_001001")

  // 数据移动指令
  val MFHI = BitPat("b000000_0000000000_?????_00000_010000")
  val MFLO = BitPat("b000000_0000000000_?????_00000_010010")
  val MTHI = BitPat("b000000_?????_000000000000000_010001")
  val MTLO = BitPat("b000000_?????_000000000000000_010011")

  // todo 自陷指令
  val LB = BitPat("b100000_?????_?????_????????????????")
  val LBU = BitPat("b100100_?????_?????_????????????????")
  val LH = BitPat("b100001_?????_?????_????????????????")
  val LHU = BitPat("b100101_?????_?????_????????????????")
  val LW = BitPat("b100011_?????_?????_????????????????")
  val SB = BitPat("b101000_?????_?????_????????????????")
  val SH = BitPat("b101001_?????_?????_????????????????")
  val SW = BitPat("b101011_?????_?????_????????????????")

  val ERET = BitPat("b010000_1_" + "0"*19 + "011000")
  val MFC0 = BitPat("b010000_00000_?????_?????_00000000_???")
  val MTC0 = BitPat("b010000_00100_?????_?????_00000000_???")

  // todo 检查/测试
  // @formatter:on
}
