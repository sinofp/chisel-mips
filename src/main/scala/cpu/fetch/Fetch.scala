// See LICENSE for license details.

package cpu.fetch

import chisel3._
import chisel3.util.{Counter, MuxCase}
import cpu.port.hazard.FHPort
import cpu.port.stage.{Decode2Fetch, Execute2Fetch, Fetch2Decode}
import cpu.util.{Config, DefCon}

class Fetch(implicit c: Config = DefCon) extends MultiIOModule {
  val debug = c.dFetch
  val ef = IO(Flipped(new Execute2Fetch))
  val df = IO(Flipped(new Decode2Fetch))
  val fd = IO(Output(new Fetch2Decode))
  val hf = IO(Flipped(new FHPort))

  val pc_now = Wire(UInt(32.W))
  val pc_next = MuxCase(fd.pcp4, Array(hf.stall -> pc_now, df.jump -> df.j_addr, ef.branch -> ef.br_addr))
  pc_now := RegNext(pc_next, 0.U)
  fd.pcp4 := pc_now + 4.U

  val inst_mem = Module(new InstMem())
  inst_mem.io.pc := pc_now
  fd.inst := inst_mem.io.inst

  if (debug) {
    val cnt = Counter(true.B, 100)
    printf(p"[log Fetch]\n\tcycle = ${cnt._1}\n\tpc_now >> 2 = ${Decimal(pc_now / 4.U)}, pc_next >> 2 = ${Decimal(pc_next / 4.U)}, inst = ${Hexadecimal(fd.inst)}\n\tstall = ${hf.stall},\n" +
      p"\tbranch = ${ef.branch}, pc_jump >> 2 = ${Decimal(ef.br_addr / 4.U)}, jump = ${df.jump}, pc_jump >> 2 = ${Decimal(df.j_addr / 4.U)}\n")
  }
}