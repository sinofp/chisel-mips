// See LICENSE for license details.

package cpu.execute

import chisel3._
import chisel3.tester.testableData
import chiseltest.ChiselScalatestTester
import org.scalatest.{FlatSpec, Matchers}

class MulTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Mul"

  it should "do multu" in {
    test(new Mul) { c =>
      val testValues = for {x <- 10 to 20; y <- 0 to 10} yield (x, y)
      import c.io._
      testValues.foreach { case (x, y) =>
        sign.poke(false.B)
        multiplicand.poke(x.U)
        multiplier.poke(y.U)
        product.expect((x * y).U)
      }
    }
  }

  it should "do mult" in {
    test(new Mul) { c =>
      val testValues = for {x <- -5 to 5; y <- -5 to 5} yield (x, y)
      import c.io._
      testValues.foreach { case (x, y) =>
        sign.poke(true.B)
        multiplicand.poke(("b" + x.toBinaryString).U)
        multiplier.poke(("b" + y.toBinaryString).U)
        val p = x * y // Int::toBinaryString负数只有32位，👇这里符号扩展
        val pp = if (p < 0) ("b" + "1" * 32 + p.toBinaryString).U else p.U
        product.expect(pp)
      }
    }
  }
}

