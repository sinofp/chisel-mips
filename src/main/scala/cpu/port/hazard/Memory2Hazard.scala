// See LICENSE for license details.

package cpu.port.hazard

import chisel3._
import cpu.writeback.CP0.SZ_EXCEPT_TYPE

class Memory2Hazard extends Bundle with Flush {
  val hi_wen = Input(Bool())
  val lo_wen = Input(Bool())
  val c0_wen = Input(Bool())
  val c0_waddr = Input(UInt(5.W))
  val wen = Input(Bool())
  val waddr = Input(UInt(5.W))
  val except_type = Input(UInt(SZ_EXCEPT_TYPE))
  val EPC = Input(UInt(32.W))
}