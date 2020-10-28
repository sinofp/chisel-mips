// See LICENSE for license details.

package cpu

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._
import cpu.Instructions.ADD

object CU {
  // @formatter:off
  // BR
  val SZ_BR_TYPE = 3.W
  def isBeq = (x: UInt) => x === 1.U(SZ_BR_TYPE)
  def isBne = (x: UInt) => x === 2.U(SZ_BR_TYPE)
  def isBgez = (x: UInt) => x === 3.U(SZ_BR_TYPE)
  def isBgtz = (x: UInt) => x === 4.U(SZ_BR_TYPE)
  def isBlez = (x: UInt) => x === 5.U(SZ_BR_TYPE)
  def isBltz = (x: UInt) => x === 6.U(SZ_BR_TYPE)
  // @formatter:on
}

import cpu.Instructions._

class CU extends Module {
  val io = IO(new Bundle() {
    val inst = Input(UInt(32.W))
    val out = Output(UInt(32.W))
  })

  import io._

  private def =?(bp: BitPat) = inst === bp

  when(=?(ADD)) {
    out := 1.U
  }.elsewhen(=?(ADDI)) {
    out := 2.U
  }.elsewhen(=?(ADDIU)) {
    out := 3.U
  }.elsewhen(=?(J)) {
    out := 4.U
  }.otherwise {
    out := 4.U
  }
}

object CU1 extends App {
  (new ChiselStage).emitVerilog(new CU)
}
