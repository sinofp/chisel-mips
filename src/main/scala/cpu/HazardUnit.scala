// See LICENSE for license details.

package cpu

import chisel3._
import chisel3.util.MuxCase
import cpu.decode.CtrlSigDef._
import cpu.port.hazard._
import cpu.util.{Config, DefCon}

class HazardUnit(readPorts: Int)(implicit c: Config = DefCon) extends MultiIOModule {
  require(readPorts >= 0)
  val fh = IO(new FHPort)
  val dh = IO(new DHPort(readPorts))
  val eh = IO(new EHPort)
  val mh = IO(new MHPort)
  val wh = IO(new WHPort)

  // RegFile 数据前推
  val forward_port = (i: Int) => MuxCase(FORWARD_DEF, Array(
    (eh.wen && (eh.waddr =/= 0.U) && dh.raddr(i) === eh.waddr) -> FORWARD_EXE,
    (mh.wen && (mh.waddr =/= 0.U) && dh.raddr(i) === mh.waddr) -> FORWARD_MEM,
    (wh.wen && (wh.waddr =/= 0.U) && dh.raddr(i) === wh.waddr) -> FORWARD_WB,
  ))

  for (i <- 0 until readPorts) {
    dh.forward(i) := forward_port(i)
  }

  // load stall
  val stall = dh.forward.exists((_: UInt) === FORWARD_EXE) && dh.prev_load
  fh.stall := stall
  dh.stall := stall
  eh.flush := stall

  fh.flush := DontCare
  dh.flush := DontCare
  eh.branch := DontCare
}
