// See LICENSE for license details.

package cpu.execute

import chisel3._
import chiseltest._
import cpu.decode.CtrlSigDef._
import cpu.execute.ALU._
import org.scalatest._

import scala.math.pow

class ExecuteTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Execute"

  it should "capable to sub" in {
    test(new Execute) { c =>
      val testValues = for { x <- 10 to 20; y <- 0 to 10 } yield (x, y)

      import c._
      testValues.foreach { case (x, y) =>
        de.alu_fn.poke(FN_SUB)
        de.num1.poke(x.U)
        de.num2.poke(y.U)
        clock.step(1)
        em.alu_out.expect((x - y).U)
      }
    }
  }

  it should "handle branch properly" in {
    test(new Execute) { c =>
      import c._
      // 0, 1, -1, -2
      Seq("0" * 32, "0" * 31 + "1", "1" * 32, "1" * 31 + "0").foreach(s => {
        de.br_type.poke(BR_TYPE_GE)
        de.alu_fn.poke(FN_SUB)
        de.num1.poke(("b" + s).U)
        de.num2.poke(0.U)
        clock.step(1)
        ef.branch.expect((s(0) == '0').B)
      })
    }
  }
}
