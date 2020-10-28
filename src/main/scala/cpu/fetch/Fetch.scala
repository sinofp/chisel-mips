// See LICENSE for license details.

package cpu.fetch

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.MuxCase

class Fetch extends Module {
  val io = IO(new Bundle() {
    val pc_jump = Input(UInt(32.W))
    val jump = Input(Bool())
    val stall = Input(Bool())
    val pcp4 = Output(UInt(32.W))
    val inst = Output(UInt(32.W))
  })

  import io._

  val pc_now = Wire(UInt(32.W))
  pcp4 := pc_now + 4.U

  val pc = Module(new PC())
  pc_now := pc.io.pc_now
  pc.io.pc_next := MuxCase(pcp4, Array(stall -> pc_now, jump -> pc_jump))

  val inst_mem = Module(new InstMem())
  inst_mem.io.pc := pc_now
  inst := inst_mem.io.inst
}

object Fetch extends App {
  (new ChiselStage).emitVerilog(new Fetch)
}