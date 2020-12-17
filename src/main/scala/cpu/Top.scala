// See LICENSE for license details.

package cpu

import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import chisel3.util.experimental.BoringUtils
import cpu.fetch.InstMem
import cpu.memory.DataMem
import cpu.port.core.SramIO
import cpu.port.debug.TRegWindow
import cpu.util.{Config, DefCon}

class Top(implicit c: Config = DefCon) extends MultiIOModule {
  val core = Module(new Core)
  core.io.interrupt.int := DontCare
  val inst_sram = IO(if (c.oTeachSoc) new SramIO else Output(UInt(0.W)))
  val data_sram = IO(if (c.oTeachSoc) new SramIO else Output(UInt(0.W)))

  inst_sram := DontCare
  data_sram := DontCare
  if (c.oTeachSoc) {
    inst_sram <> core.inst_sram
    data_sram <> core.data_sram
  } else {
    val inst_mem = Module(new InstMem)
    inst_mem.io <> core.inst_sram

    val data_mem = Module(new DataMem)
    data_mem.io <> core.data_sram
  }

  val t_regs = if (c.dTReg) Some(IO(Output(new TRegWindow()))) else None
  if (c.dTReg) {
    t_regs.get.getElements.foreach(_ := 1.U)
    t_regs.get.getElements.reverse.zipWithIndex.foreach { case (sink, idx) => BoringUtils.addSink(sink, s"reg$idx") }
  }
}

object Top extends App {
  implicit val c: Config = Config(oTeachSoc = true)
  new ChiselStage execute(Array("--target-dir", "chisel-mips.srcs/sources_1/new"), Seq(ChiselGeneratorAnnotation(() => new Top)))
}