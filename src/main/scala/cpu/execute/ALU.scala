// See LICENSE.SiFive for license details.
// See LICENSE.Berkeley for license details.
// Modifications see LICENSE for license details.

package cpu.execute

import Chisel._
import cpu.util.{Config, DefCon}

object ALU {
  // @formatter:off
  val SZ_ALU_FN = 4.W
  def FN_X = BitPat("b????")
  def FN_SUB = UInt(10)
  def FN_SRA = UInt(11)
  def FN_SGE = UInt(13)
  def FN_SLTU = UInt(14)
  def FN_SGEU = UInt(15)
  def FN_DIV = FN_XOR
  def FN_XOR = UInt(4)
  def FN_DIVU = FN_SR
  def FN_SR = UInt(5)
  def FN_REM = FN_OR
  def FN_OR = UInt(6)
  def FN_REMU = FN_AND
  def FN_AND = UInt(7)
  def FN_MUL = FN_ADD
  def FN_ADD = UInt(0)
  def FN_MULH = FN_SL
  def FN_SL = UInt(1)
  def FN_MULHSU = FN_SEQ
  def FN_SEQ = UInt(2)
  def FN_MULHU = FN_SNE
  def FN_SNE = UInt(3)
  def isMulFN(fn: UInt, cmp: UInt) = fn(1, 0) === cmp(1, 0)
  def isSub(cmd: UInt) = cmd(3)
  def isCmp(cmd: UInt) = cmd >= FN_SLT
  def FN_SLT = UInt(12)
  def cmpUnsigned(cmd: UInt) = cmd(1)
  def cmpInverted(cmd: UInt) = cmd(0)
  def cmpEq(cmd: UInt) = !cmd(3)
  // @formatter:on
}

import cpu.execute.ALU._

class ALU(implicit c: Config = DefCon) extends Module {
  val xLen = 32
  val io = new Bundle {
    val fn = Bits(INPUT, SZ_ALU_FN)
    val in2 = UInt(INPUT, xLen)
    val in1 = UInt(INPUT, xLen)
    val out = UInt(OUTPUT, xLen)
    val adder_out = UInt(OUTPUT, xLen)
    val cmp_out = Bool(OUTPUT)
  }

  // ADD, SUB
  val in2_inv = Mux(isSub(io.fn), ~io.in2, io.in2).asInstanceOf[UInt] // 其实不用转，只为了告诉Idea
  val in1_xor_in2 = io.in1 ^ in2_inv
  io.adder_out := io.in1 + in2_inv + isSub(io.fn)

  // SLT, SLTU
  val slt =
    Mux(io.in1(xLen - 1) === io.in2(xLen - 1), io.adder_out(xLen - 1),
      Mux(cmpUnsigned(io.fn), io.in2(xLen - 1), io.in1(xLen - 1)))
  io.cmp_out := cmpInverted(io.fn) ^ Mux(cmpEq(io.fn), in1_xor_in2 === UInt(0), slt)

  // SLL, SRL, SRA
  val (shamt, shin_r) = (io.in2(4, 0), io.in1)
  val shin = Mux(io.fn === FN_SR || io.fn === FN_SRA, shin_r, Reverse(shin_r))
  val shout_r = (Cat(isSub(io.fn) & shin(xLen - 1), shin).asSInt >> shamt) (xLen - 1, 0)
  val shout_l = Reverse(shout_r)
  val shout = Mux(io.fn === FN_SR || io.fn === FN_SRA, shout_r, UInt(0)) |
    Mux(io.fn === FN_SL, shout_l, UInt(0))

  // AND, OR, XOR
  val logic = Mux(io.fn === FN_XOR || io.fn === FN_OR, in1_xor_in2, UInt(0)) |
    Mux(io.fn === FN_OR || io.fn === FN_AND, io.in1 & io.in2, UInt(0))
  val shift_logic = (isCmp(io.fn) && slt) | logic | shout
  val out = Mux(io.fn === FN_ADD || io.fn === FN_SUB, io.adder_out, shift_logic)

  io.out := out
}