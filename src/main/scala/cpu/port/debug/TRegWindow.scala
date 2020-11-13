// See LICENSE for license details.

package cpu.port.debug

import chisel3._

class TRegWindow(val debug: Boolean = true) extends Bundle {
  val w = if (debug) 32.W else 0.W
  val t0 = UInt(w)
  val t1 = UInt(w)
  val t2 = UInt(w)
  val t3 = UInt(w)
  val t4 = UInt(w)
  val t5 = UInt(w)
  val t6 = UInt(w)
  val t7 = UInt(w)
  val t8 = UInt(w)
  val t9 = UInt(w)
}
