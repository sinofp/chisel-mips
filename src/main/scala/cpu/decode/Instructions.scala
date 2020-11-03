package cpu.decode

import chisel3.util.BitPat

object Instructions {
  // @formatter:off
  // 算术运算指令
  def ADD = BitPat("b000000_?????_?????_?????_00000_100000")
  def ADDI = BitPat("b001000_?????_?????_????????????????")
  def ADDU = BitPat("b000000_?????_?????_?????_00000_100001")
  def ADDIU = BitPat("b001001_?????_?????_????????????????")
  def SUB = BitPat("b000000_?????_?????_?????_00000_100010")
  def SUBU = BitPat("b000000_?????_?????_?????_00000_100011")
  def SLT = BitPat("b000000_?????_?????_?????_00000_101010")
  def SLTI = BitPat("b001010_?????_?????_????????????????")
  def SLTU = BitPat("b000000_?????_?????_?????_00000_101011")
  def SLTIU = BitPat("b001011_?????_?????_????????????????")
  def DIV = BitPat("b000000_?????_?????_0000000000_011010")
  def DIVU = BitPat("b000000_?????_?????_0000000000_011011")
  def MULT = BitPat("b000000_?????_?????_0000000000_011000")
  def MULTU = BitPat("b000000_?????_?????_0000000000_011001")

  // 逻辑运算指令
  def AND = BitPat("b000000_?????_?????_?????_00000_100100")
  def ANDI = BitPat("b001100_?????_?????_????????????????")
  def LUI = BitPat("b001111_00000_?????_????????????????")
  def NOR = BitPat("b000000_?????_?????_?????_00000_100111")
  def OR = BitPat("b000000_?????_?????_?????_00000_100101")
  def ORI = BitPat("b001101_?????_?????_????????????????")
  def XOR = BitPat("b000000_?????_?????_?????_00000_100110")
  def XORI = BitPat("b001110_?????_?????_????????????????")

  // 移位指令
  def SLLV = BitPat("b000000_?????_?????_?????_00000_000100")
  def SLL = BitPat("b000000_00000_?????_?????_?????_000100")
  def SRAV = BitPat("b000000_?????_?????_?????_00000_000111")
  def SRA = BitPat("b000000_00000_?????_?????_?????_000111")
  def SRLV = BitPat("b000000_?????_?????_?????_00000_000110")
  def SRL = BitPat("b000000_00000_?????_?????_?????_000010")

  // 分支跳转指令
  def BEQ = BitPat("b000100_?????_?????_????????????????")
  def BNE = BitPat("b000101_?????_?????_????????????????")
  def BGEZ = BitPat("b000001_?????_00001_????????????????")
  def BGTZ = BitPat("b000111_?????_00000_????????????????")
  def BLEZ = BitPat("b000110_?????_00000_????????????????")
  def BLTZ = BitPat("b000001_?????_00000_????????????????")
  def BGEZAL = BitPat("b000001_?????_10001_????????????????")
  def BLTZAL = BitPat("b000001_?????_10000_????????????????")
  def J = BitPat("b000010_" + "?"*26)
  def JAL = BitPat("b000011_" + "?"*26)
  def JR = BitPat("b000000_?????_0000000000_00000_000100")
  def JALR = BitPat("b000000_?????_00000_?????_00000_001001")

  // 数据移动指令
  def MFHI = BitPat("b000000_0000000000_?????_00000_010000")
  def MFLO = BitPat("b000000_0000000000_?????_00000_010010")
  def MTHI = BitPat("b000000_?????_000000000000000_010001")
  def MTLO = BitPat("b000000_?????_000000000000000_010011")

  // todo 自陷指令
  // todo 访存指令
  def LW = BitPat("b100011_?????_?????_????????????????")
  def SW = BitPat("b101011_?????_?????_????????????????")
  // todo 特权指令
  // todo 检查/测试
  // @formatter:on
}
