// See LICENSE for license details.

package cpu.execute

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.MuxCase
import cpu.decode.CtrlSigDef._
import cpu.util.{Config, DefCon}

class BrUnit(implicit c: Config = DefCon) extends Module {
  val io = IO(new Bundle() {
    val sub_res = Input(UInt(32.W))
    val br_type = Input(UInt(SZ_BR_TYPE))
    val branch = Output(Bool())
  })

  val isNotZero = io.sub_res =/= 0.U
  val isPos = io.sub_res(31) === 0.U
  if (c.debugBrUnit) {
    printf(p"[log BrUint] io.sub_res = ${Binary(io.sub_res)}, isPos = $isPos, isNotZero = $isNotZero\n")
  }

  io.branch := {
    val is = (tpe: UInt) => io.br_type === tpe
    MuxCase(!isNotZero, Array(
      is(BR_TYPE_EQ) -> isNotZero,
      is(BR_TYPE_GE) -> isPos,
      is(BR_TYPE_GT) -> (isPos && isNotZero),
      is(BR_TYPE_LT) -> (!isPos),
      is(BR_TYPE_LT) -> (!isPos && isNotZero),
    ))
  }
}

object BrUnit extends App {
  (new ChiselStage).emitVerilog(new BrUnit)
}
