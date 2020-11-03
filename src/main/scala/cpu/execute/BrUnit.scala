// See LICENSE for license details.

package cpu.execute

import chisel3._
import chisel3.util.MuxCase
import cpu.decode.CtrlSigDef._
import cpu.util.{Config, DefCon}

class BrUnit(implicit c: Config = DefCon) extends Module {
  val io = IO(new Bundle() {
    val num1 = Input(UInt(32.W))
    val num2 = Input(UInt(32.W))
    val slt_res = Input(UInt(32.W))
    val br_type = Input(UInt(SZ_BR_TYPE))
    val branch = Output(Bool())
  })

  val equal = io.num1 === io.num2
  val less = io.slt_res(0)
  if (c.debugBrUnit) {
    printf(p"[log BrUint]\n\tnum1 = ${Hexadecimal(io.num1)}, num2 = ${Hexadecimal(io.num2)}, br_type = ${Hexadecimal(io.br_type)}\n" +
      p"\tio.slt_res = ${Hexadecimal(io.slt_res)}, less = $less, equal = $equal, branch = ${io.branch}\n")
  }

  io.branch := {
    val is = (tpe: UInt) => io.br_type === tpe
    MuxCase(false.B, Array(
      is(BR_TYPE_EQ) -> equal,
      is(BR_TYPE_NE) -> !equal,
      is(BR_TYPE_GE) -> !less,
      is(BR_TYPE_GT) -> (!less && !equal),
      is(BR_TYPE_LE) -> (less || equal),
      is(BR_TYPE_LT) -> less,
    ))
  }
}