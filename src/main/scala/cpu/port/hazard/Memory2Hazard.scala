// See LICENSE for license details.

package cpu.port.hazard

import chisel3._
import cpu.port.{Flush, Stall, WAddr, WEn}
import cpu.writeback.CP0.SZ_EXCEPT_TYPE

class Memory2Hazard extends Bundle with Flush with Stall {
  val hi = Flipped(new Bundle with WEn)
  val lo = Flipped(new Bundle with WEn)
  val c0 = Flipped(new Bundle with WEn with WAddr)
  val rf = Flipped(new Bundle with WEn with WAddr)
  val except_type = Input(UInt(SZ_EXCEPT_TYPE))
  val EPC = Input(UInt(32.W))
}