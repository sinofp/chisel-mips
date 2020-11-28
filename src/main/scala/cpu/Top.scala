// See LICENSE for license details.

package cpu

import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import chisel3.util.experimental.BoringUtils
import cpu.fetch.InstMem
import cpu.memory.DataMem
import cpu.port.debug.TRegWindow
import cpu.util.{Config, DefCon}

class Top(implicit c: Config = DefCon) extends MultiIOModule {
  val core = Module(new Core)
  core.io.interrupt.int := DontCare

  val inst_mem = Module(new InstMem)
  inst_mem.io <> core.inst_mem

  val data_mem = Module(new DataMem)
  data_mem.io <> core.data_mem

  val t_regs = if (c.dTReg) Some(IO(Output(new TRegWindow()))) else None
  if (c.dTReg) {
    t_regs.get.getElements.foreach(_ := 1.U)
    t_regs.get.getElements.reverse.zipWithIndex.foreach { case (sink, idx) => BoringUtils.addSink(sink, s"reg$idx") }
  }
}

object Top extends App {
  //  implicit val c: Config = Config(debugTReg = true)
  //  new ChiselStage emitVerilog new Top
  new ChiselStage execute(Array("--target-dir", "chisel-mips.srcs/sources_1/new"), Seq(ChiselGeneratorAnnotation(() => new Top)))
}