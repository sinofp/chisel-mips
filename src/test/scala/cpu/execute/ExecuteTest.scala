// See LICENSE for license details.

package cpu.execute

import chisel3._
import chiseltest._
import cpu.decode.CtrlSigDef._
import cpu.execute.ALU._
import org.scalatest._

class ExecuteTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Execute"

  it should "capable to sub" in {
    test(new Execute) { c =>
      import c.io._
      val testValues = for {x <- 10 to 20; y <- 0 to 10} yield (x, y)

      testValues.foreach { case (x, y) =>
        de_alu_fn.poke(FN_SUB)
        de_num1.poke(x.U)
        de_num2.poke(y.U)
        c.clock.step(1)
        adder_out.expect((x - y).U)
      }
    }
  }

  it should "handle branch properly" in {
    test(new Execute) { c =>
      import c.io._

      for (x <- 0 to 1) {
        de_br_type.poke(BR_TYPE_GE)
        de_alu_fn.poke(FN_SUB)
        de_num1.poke(x.U)
        de_num2.poke(0.U)
        c.clock.step(1)
        ef_branch.expect((x >= 0).B)
      }
    }
  }
}
