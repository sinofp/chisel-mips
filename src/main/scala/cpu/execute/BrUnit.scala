// See LICENSE for license details.

package cpu.execute

import Chisel.{BitPat, Fill}
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.MuxCase
import cpu.decode.CtrlSigDef._

class BrUnit extends Module {
  val io = IO(new Bundle() {
    val sub_res = Input(UInt(32.W))
    val br_type = Input(UInt(SZ_BR_TYPE))
    val branch = Output(Bool())
  })

  val isNotZero = io.sub_res =/= 0.U
//  val isPos = io.sub_res === BitPat("b0" + "?"*31)
//  val isPos = Wire(UInt(32.W))
//  when(io.sub_res === BitPat("b0" + "?"*31)) {
//    isPos := true.B
//  }.otherwise {
//    isPos := false.B
//  }
//  val isPos = Mux(io.sub_res(31), true.B, false.B)
//  val isPos = Fill(32, io.sub_res(31)) === 0.U
//  val isPos = io.sub_res(31) === false.B
  val isPos = io.sub_res(31) === 0.U
  printf(p"[log BrUint] io.sub_res = ${Binary(io.sub_res)}, isPos = $isPos, isNotZero = $isNotZero\n")

  io.branch := MuxCase(!isNotZero, Array(
    isBne(io.br_type) -> isNotZero,
    isBgez(io.br_type) -> isPos,
    isBgtz(io.br_type) -> (isPos && isNotZero),
    isBlez(io.br_type) -> (!isPos),
    isBltz(io.br_type) -> (!isPos && isNotZero),
  ))
}

object BrUnit extends App {
  (new ChiselStage).emitVerilog(new BrUnit)
}
