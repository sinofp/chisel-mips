// See LICENSE for license details.

package cpu.port.hazard

import chisel3._

class Writeback2Hazard extends Bundle {
  val wen = Input(Bool())
  val waddr = Input(UInt(5.W))
  val hi_wen = Input(Bool())
  val lo_wen = Input(Bool())
  val c0_wen = Input(Bool())
  val c0_waddr = Input(UInt(5.W))
}
