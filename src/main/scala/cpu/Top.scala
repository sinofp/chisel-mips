// See LICENSE for license details.

package cpu

import java.io.{File, PrintWriter}

import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import chisel3.util.experimental.BoringUtils
import cpu.fetch.InstMem
import cpu.memory.DataMem
import cpu.port.core.SramIO
import cpu.port.debug.{DebugWb, TRegWindow}
import cpu.util.{Config, DefCon}

import scala.io.Source

class Top(implicit c: Config = DefCon) extends MultiIOModule {
  val int = IO(Input(UInt(6.W)))
  val inst_sram = IO(if (c.oLoongSoc) new SramIO else Output(UInt(0.W)))
  val data_sram = IO(if (c.oLoongSoc) new SramIO else Output(UInt(0.W)))

  val core = Module(new Core)

  int <> core.int
  inst_sram := DontCare
  data_sram := DontCare
  if (c.oLoongSoc) {
    clock.suggestName("clk")
    reset.suggestName("resetn")
    core.reset := ~reset.asBool
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
  val debug_wb = if(c.oLoongSoc || c.dTeachSoc) Some(IO(new DebugWb)) else None
  if (c.oLoongSoc || c.dTeachSoc) {
    debug_wb.get.getElements.foreach(_ := 1.U)
    debug_wb.get.getElements.reverse.zipWithIndex.foreach { case (sink, n) => BoringUtils.addSink(sink, s"debugwb$n")}
  }
}

object Top extends App {
  implicit val c: Config = Config(oLoongSoc = true)
  new ChiselStage execute(Array("--target-dir", "chisel-mips.srcs/sources_1/new"), Seq(ChiselGeneratorAnnotation(() => new Top)))
  if (c.oLoongSoc) {
    val src = Source.fromFile("chisel-mips.srcs/sources_1/new/Top.v")
    val txt = try src.mkString finally src.close
    val writer = new PrintWriter(new File("chisel-mips.srcs/sources_1/new/Top.v"))
    try writer.write(txt.replaceAll("int_", "int").replaceAll("Top", "mycpu_top")) finally writer.close()
  }
}