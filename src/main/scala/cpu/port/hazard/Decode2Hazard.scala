// See LICENSE for license details.

package cpu.port.hazard

import chisel3._
import cpu.decode.CtrlSigDef.SZ_FORWARD
import cpu.port.{Flush, Stall}

class Decode2Hazard(val readPorts: Int) extends Bundle with Stall with Flush {
  val raddr     = Input(Vec(readPorts, UInt(5.W)))
  val prev_load = Input(Bool())
  val forward   = Output(Vec(readPorts, UInt(SZ_FORWARD)))
}
