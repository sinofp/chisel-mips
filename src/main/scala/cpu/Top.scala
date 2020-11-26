// See LICENSE for license details.

package cpu

import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import chisel3.util.experimental.BoringUtils
import cpu.decode.Decode
import cpu.execute.Execute
import cpu.fetch.Fetch
import cpu.memory.Memory
import cpu.port.core.Core2WriteBack
import cpu.port.debug.TRegWindow
import cpu.util.{Config, DefCon}
import cpu.writeback.WriteBack

class Top(implicit c: Config = DefCon) extends MultiIOModule {
  val io = IO(new Bundle() {
    val interrupt = new Core2WriteBack
  })

  val fetch = Module(new Fetch)
  val decode = Module(new Decode)
  val execute = Module(new Execute)
  val memory = Module(new Memory)
  val writeback = Module(new WriteBack)
  val hazard = Module(new HazardUnit(2))

  // br & j
  fetch.execute <> execute.fetch
  fetch.decode <> decode.fetch

  decode.writeBack <> writeback.decode
  decode.memory <> memory.decode
  execute.decode <> decode.execute
  execute.memory <> memory.execute
  execute.writeback <> writeback.execute
  writeback.memory <> memory.writeback

  hazard.fetch <> fetch.hazard
  hazard.decode <> decode.hazard
  hazard.execute <> execute.hazard
  hazard.memory <> memory.hazard
  hazard.writeback <> writeback.hazard

  io.interrupt <> writeback.core

  // debug
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