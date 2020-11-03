// See LICENSE for license details.

package cpu

import chisel3._
import chisel3.util.MuxCase
import cpu.decode.CtrlSigDef._
import cpu.port.{ForwardPort, HDPort}
import cpu.util.{Config, DefCon}

class HazardUnit(readPorts: Int)(implicit c: Config = DefCon) extends MultiIOModule {
  require(readPorts >= 0)
  val eh = IO(Input(new ForwardPort))
  val mh = IO(Input(new ForwardPort))
  val wh = IO(Input(new ForwardPort))
  val hd = IO(new HDPort(readPorts))
  //    val stall = Bool()
  val forward_port = (i: Int) => MuxCase(FORWARD_DEF, Array(
    (eh.wen && (eh.waddr =/= 0.U) && hd.raddr(i) === eh.waddr) -> FORWARD_EXE,
    (mh.wen && (mh.waddr =/= 0.U) && hd.raddr(i) === mh.waddr) -> FORWARD_MEM,
    (wh.wen && (wh.waddr =/= 0.U) && hd.raddr(i) === wh.waddr) -> FORWARD_WB,
  ))

  for (i <- 0 until readPorts) {
    hd.forward(i) := forward_port(i)
  }
  //  stall := (forward1 === FORWARD_EXE || forward2 === FORWARD_EXE) && (prevop === load)
}
