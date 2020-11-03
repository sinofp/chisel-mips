// See LICENSE for license details.

package cpu.fetch

import chisel3._
import chisel3.util.MuxCase
import cpu.port.hazard.FHPort
import cpu.port.stage.FDPort
import cpu.util.{Config, DefCon}

class Fetch(implicit c: Config = DefCon) extends MultiIOModule {
  val debug = c.debugFetch
  val ef = IO(new Bundle() {
    val pc_jump = Input(UInt(32.W))
    val jump = Input(Bool())
  })
  val fd = IO(Output(new FDPort))
  val hf = IO(Flipped(new FHPort))

  val pc_now = Wire(UInt(32.W))
  val pc_next = MuxCase(fd.pcp4, Array(hf.stall -> pc_now, ef.jump -> ef.pc_jump))
  pc_now := RegNext(pc_next, 0.U)
  fd.pcp4 := pc_now + 4.U

  val inst_mem = Module(new InstMem())
  inst_mem.io.pc := pc_now
  fd.inst := inst_mem.io.inst

  if (debug) {
    printf(p"[log Fetch]\n\tpc_now = ${Hexadecimal(pc_now)}, pc_next = ${Hexadecimal(pc_next)}, inst = ${Hexadecimal(fd.inst)}\n")
  }
}