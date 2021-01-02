// See LICENSE for license details.

package cpu.util

import chisel3._
import chisel3.internal.sourceinfo.SourceInfo
import chisel3.util._

import scala.math._

object Chisel3IsBad {

  implicit class SubFieldAssign(lhs: Data) {
    def subassign(i: Int, rhs: Data): Unit = subassign(i, i, rhs)

    def subassign(high: Int, low: Int, rhs: Data): Unit = {
      val w = lhs.getWidth
      require(w > high, "lhs.width should > high")
      require(low >= 0, "low should >= 0")
      require(high >= low, "high should > low")
      require(high - low == rhs.getWidth - 1, "subfield length doesn't match")
      require(rhs.getWidth != w, "use := instead")

      lhs := {
        if (w == high + 1) {
          rhs.asUInt() ## lhs.asUInt()(low - 1, 0)
        } else if (0 == low) {
          lhs.asUInt()(w - 1, high + 1) ## rhs.asUInt()
        } else {
          Cat(lhs.asUInt()(w - 1, high + 1), rhs.asUInt, lhs.asUInt()(low - 1, 0))
        }
      }
    }
  }

  implicit class PartialBulkConnect(lhs: Bundle) {
    def ?=(rhs: Bundle)(implicit sourceInfo: SourceInfo, connectionCompileOptions: CompileOptions): Unit = {
      val lPorts = lhs.elements
      val rPorts = rhs.elements
      (lPorts.keySet & rPorts.keySet).foreach(name => lPorts(name) := rPorts(name))
    }
  }

  def log2I(x: Int): Int = (log10(x) / log10(2)).toInt
}
