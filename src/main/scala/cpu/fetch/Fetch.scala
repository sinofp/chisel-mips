// See LICENSE for license details.

package cpu.fetch

import chisel3._
import chisel3.util.{Counter, MuxCase}
import cpu.port.hazard.Fetch2Hazard
import cpu.port.stage.{Decode2Fetch, Execute2Fetch}
import cpu.util.{Config, DefCon}

class Fetch(implicit c: Config = DefCon) extends MultiIOModule {
  val debug = c.dFetch
  val execute = IO(Flipped(new Execute2Fetch))
  val decode = IO(Flipped(new Decode2Fetch))
  val hazard = IO(Flipped(new Fetch2Hazard))

  val pc_now = Wire(UInt(32.W))
  val pc_next = MuxCase(decode.pcp4, Array(hazard.stall -> pc_now, decode.jump -> decode.j_addr, execute.branch -> execute.br_addr))
  pc_now := RegNext(pc_next, 0.U)
  decode.pcp4 := pc_now + 4.U

  val inst_mem = Module(new InstMem())
  inst_mem.io.pc := pc_now
  decode.inst := inst_mem.io.inst

  if (debug) {
    val cnt = Counter(true.B, 100)
    printf(p"[log Fetch]\n\tcycle = ${cnt._1}\n\tpc_now >> 2 = ${Decimal(pc_now / 4.U)}, pc_next >> 2 = ${Decimal(pc_next / 4.U)}, inst = ${Hexadecimal(decode.inst)}\n\tstall = ${hazard.stall},\n" +
      p"\tbranch = ${execute.branch}, pc_jump >> 2 = ${Decimal(execute.br_addr / 4.U)}, jump = ${decode.jump}, pc_jump >> 2 = ${Decimal(decode.j_addr / 4.U)}\n")
  }
}