// See LICENSE for license details.

package cpu.fetch

import chisel3._
import chisel3.stage.ChiselStage
import cpu.util.Config

class InstMem(implicit c: Option[Config] = None) extends Module {
  val io = IO(new Bundle() {
    val pc = Input(UInt(32.W))
    val inst = Output(UInt(32.W))
  })
  val preInst = Seq(
    "h20080064",
    "h32290001",
    "h21290001",
    "h1528fffe",
    "hac090000",
  ).map(_.U)

  val mem = RegInit(VecInit(preInst ++ Seq.fill(32 - preInst.length)(0.U(32.W))))

  io.inst := mem(io.pc / 4.U)
  // Chisel是不是优化过头了
}

object InstMem extends App {
  (new ChiselStage).emitVerilog(new InstMem)
}
