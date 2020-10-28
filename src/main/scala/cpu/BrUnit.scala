// See LICENSE for license details.

package cpu

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.MuxCase
import cpu.CtrlSigDef._

class BrUnit extends Module {
  val io = IO(new Bundle() {
    val sub_res = Input(UInt(32.W))
    val br_type = Input(UInt(SZ_BR_TYPE))
    val branch = Output(Bool())
  })

  import io._

  branch := MuxCase(sub_res === 0.U, Array(
    isBne(br_type) -> (sub_res =/= 0.U),
    isBgez(br_type) -> (sub_res >= 0.U),
    isBgtz(br_type) -> (sub_res > 0.U),
    isBlez(br_type) -> (sub_res <= 0.U),
    isBltz(br_type) -> (sub_res < 0.U),
  ))
}

object BrUnit extends App {
  (new ChiselStage).emitVerilog(new BrUnit)
}
