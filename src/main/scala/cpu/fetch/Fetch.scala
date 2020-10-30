// See LICENSE for license details.

package cpu.fetch

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.MuxCase
import cpu.util.{Config, DefCon}

class Fetch(implicit c: Option[Config] = None) extends MultiIOModule {
  val debug = c.getOrElse(DefCon).debugFetch
  val ef = IO(new Bundle() {
    val pc_jump = Input(UInt(32.W))
    val jump = Input(Bool())
  })
  val fd = IO(new Bundle() {
    //    val stall = Input(Bool())
    val pcp4 = Output(UInt(32.W))
    val inst = Output(UInt(32.W))
  })

  val pc_now = Wire(UInt(32.W))
  fd.pcp4 := pc_now + 4.U

  val pc = Module(new PC())
  pc_now := pc.io.pc_now
  pc.io.pc_next := MuxCase(fd.pcp4, Array(/*stall -> pc_now, */ ef.jump -> ef.pc_jump))

  val inst_mem = Module(new InstMem())
  inst_mem.io.pc := pc_now
  fd.inst := inst_mem.io.inst

  if (debug) {
    printf(p"[log Fetch] pc_now = $pc_now, pcp4 = ${fd.pcp4}\n")
  }
}

object Fetch extends App {
//  implicit val conf = Some(new Config(inputInst = true, debugInstMem = true))
  (new ChiselStage).emitVerilog(new Fetch)
}