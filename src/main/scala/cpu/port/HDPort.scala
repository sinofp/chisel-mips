// See LICENSE for license details.

package cpu.port

import chisel3._
import cpu.decode.CtrlSigDef.SZ_FORWARD

class HDPort(val readPorts: Int) extends Bundle {
  val raddr = Input(Vec(readPorts, UInt(5.W)))
  val forward = Output(Vec(readPorts, UInt(SZ_FORWARD)))
}
