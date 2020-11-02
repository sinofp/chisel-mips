// See LICENSE for license details.

package cpu.decode

import chisel3._
import chisel3.util.MuxCase
import cpu.decode.CtrlSigDef._
import cpu.port.WritePort
import cpu.util.{Config, DefCon}

class HazardUnit(readPorts: Int)(implicit c: Config = DefCon) extends MultiIOModule {
  require(readPorts >= 0)
  val raddr = IO(Input(Vec(readPorts, UInt(5.W))))
  val ed = IO(Input(new WritePort))
  val md = IO(Input(new WritePort))
  val wd = IO(Input(new WritePort))
  val forward = IO(Output(Vec(readPorts, UInt(SZ_FORWARD))))
  //    val stall = Bool()
  val forward_port = (i: Int) => MuxCase(FORWARD_DEF, Array(
    (ed.wen && (ed.waddr =/= 0.U) && raddr(i) === ed.waddr) -> FORWARD_EXE,
    (md.wen && (md.waddr =/= 0.U) && raddr(i) === md.waddr) -> FORWARD_MEM,
    (wd.wen && (wd.waddr =/= 0.U) && raddr(i) === wd.waddr) -> FORWARD_WB,
  ))

  for (i <- 0 until readPorts) {
    forward(i) := forward_port(i)
  }
  //  stall := (forward1 === FORWARD_EXE || forward2 === FORWARD_EXE) && (prevop === load)
}
