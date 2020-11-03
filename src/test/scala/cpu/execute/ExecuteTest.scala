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
      val testValues = for {x <- 10 to 20; y <- 0 to 10} yield (x, y)

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
      // 0, 1, -1, -2
      // 负数的话，用scala的Int转UInt有问题
      Seq("0" * 32, "0" * 31 + "1", "1" * 32, "1" * 31 + "0").foreach(s => {
        c.de.num1.poke(("b" + s).U)
        c.de.num2.poke(0.U)
        c.de.br_type.poke(BR_TYPE_GE)
        c.de.alu_fn.poke(FN_SLT)
        c.clock.step(1)
        c.ef.branch.expect((s(0) == '0').B)
      })
    }
  }
}
