// See LICENSE for license details.

package cpu

import chisel3._

object CU {
  // BR
  val SZ_BR_TYPE = 3.W

  def isBeq = (x: UInt) => x === 1.U(SZ_BR_TYPE)

  def isBne = (x: UInt) => x === 2.U(SZ_BR_TYPE)

  def isBgez = (x: UInt) => x === 3.U(SZ_BR_TYPE)

  def isBgtz = (x: UInt) => x === 4.U(SZ_BR_TYPE)

  def isBlez = (x: UInt) => x === 5.U(SZ_BR_TYPE)

  def isBltz = (x: UInt) => x === 6.U(SZ_BR_TYPE)

}

class CU extends Module {
  val io = IO(new Bundle() {
  })
}
