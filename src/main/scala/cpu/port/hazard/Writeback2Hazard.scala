// See LICENSE for license details.

package cpu.port.hazard

import chisel3._
import cpu.port.{Flush, WAddr, WEn}

class Writeback2Hazard extends Bundle with Flush {
  val rf = Flipped(new Bundle with WEn with WAddr)
  val hi = Flipped(new Bundle with WEn)
  val lo = Flipped(new Bundle with WEn)
  val c0 = Flipped(new Bundle with WEn with WAddr)
}
