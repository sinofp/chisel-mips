// See LICENSE for license details.

package cpu

import chisel3._
import chiseltest._
import cpu.ALU._
import org.scalatest._

class CUTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "CU"

  it should "output FN_ADD when inst is ADD" in {
    test(new CU) { c =>
      c.inst.poke("b000000_11111_11111_11111_00000_100000".U)
      c.ctrl.alu_fn.expect(FN_ADD)
    }
  }
}