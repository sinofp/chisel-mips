// See LICENSE for license details.

package cpu.execute

import chisel3._
import chiseltest._
import cpu.decode.CtrlSigDef._
import org.scalatest._

import scala.math.pow

class BrUnitTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "BrUnit"

  it should "compare with zero as signed" in {
    test(new BrUnit) { c =>

      for (neg <- 0 to 1) {
        for (i <- 0 to 1) {
          val x = if (neg == 0) i else pow(2, 32).toInt - i // 0, 1, -1, -2
          println("[log] neg is " + neg + ", x is " + x)
          c.io.br_type.poke(BR_TYPE_GE)
          c.io.sub_res.poke(x.U)
          c.clock.step(5)
//          c.isNotZero.expect((x == 0).B)
//          c.isPos.expect((neg == 0).B)
//          branch.expect((neg == 0).B)
        }
      }
    }
  }
}
