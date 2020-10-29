// See LICENSE for license details.

package cpu.fetch

import chisel3._
import chisel3.stage.ChiselStage
import cpu.util.{Config, DefCon}

class InstMem(implicit c: Option[Config] = None) extends Module {

  val inputInst = c.getOrElse(DefCon).inputInst

  val io = IO(new Bundle() {
    val pc = Input(UInt(32.W))
    val inst = Output(UInt(32.W))
    val wen = Input(if (inputInst) Bool() else UInt(0.W))
    val waddr = Input(if (inputInst) UInt(5.W) else UInt(0.W))
    val wdata = Input(if (inputInst) UInt(32.W) else UInt(0.W))
  })

  val mem = if (inputInst) {
    RegInit(VecInit(Seq.fill(32)(0.U(32.W))))
  } else {
    val preInst = Seq(
      "20080064",
      "32290001",
      "21290001",
      "1528fffe",
      "ac090000",
    ).map("h" + _).map(_.U)
    RegInit(VecInit(preInst ++ Seq.fill(32 - preInst.length)(0.U(32.W))))
  }

  io.inst := mem(io.pc / 4.U)

  when(io.wen === 1.U) {
    mem(io.waddr) := io.wdata // 别写覆盖了
  }

  if (c.getOrElse(DefCon).debugInstMem) {
    printf(p"[log InstMem] inputList = $inputInst\n" +
      p"\tinst 1 = ${Hexadecimal(mem(0.U))}\n" +
      p"\tinst 2 = ${Hexadecimal(mem(1.U))}\n" +
      p"\tinst 3 = ${Hexadecimal(mem(2.U))}\n" +
      p"\tinst 4 = ${Hexadecimal(mem(3.U))}\n" +
      p"\tinst 5 = ${Hexadecimal(mem(4.U))}\n")
  }
}

object InstMem extends App {
  (new ChiselStage).emitVerilog(new InstMem)
}
