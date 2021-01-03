// See LICENSE for license details.

package cpu.execute

import chisel3._
import chisel3.tester.{testableClock, testableData}
import chiseltest.ChiselScalatestTester
import org.scalatest.{FlatSpec, Matchers}

class DivTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Div"

  it should "do divu" in {
    test(new Div) { c =>
      val testValues = for { x <- 10 to 20; y <- 1 to 20 } yield (x, y)
      import c.io._
      testValues.foreach { case (x, y) =>
        dividend.poke(x.U)
        divider.poke(y.U)
        start.poke(true.B)
        c.clock.step(33)
        ready.expect(true.B)
        quotient.expect((x / y).U)
        remainder.expect((x % y).U)
      }
    }
  }

  it should "do div" in {
    test(new Div) { c =>
      val testValues = for { x <- 10 to 20; y <- 1 to 20 } yield (x, y)
      import c.io._
      testValues.foreach { case (x, y) =>
        dividend.poke(("b" + x.toBinaryString).U)
        divider.poke(("b" + y.toBinaryString).U)
        sign.poke(true.B)
        start.poke(true.B)
        c.clock.step(33)
        ready.expect(true.B)
        quotient.expect(("b" + (x / y).toBinaryString).U)
        remainder.expect(("b" + (x % y).toBinaryString).U)
      }
    }
  }
}
