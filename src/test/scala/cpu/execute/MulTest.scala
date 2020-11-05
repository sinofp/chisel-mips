// See LICENSE for license details.

package cpu.execute

import chisel3._
import chiseltest._
import cpu.util.Config
import org.scalatest._

class MulTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Mul"

  it should "Multiply unsigned integers" in {
    implicit val conf: Config = Config(debugMul = true)
    test(new Mul) { c =>
      for {x <- 0 to 10; y <- 0 to 10} {
        c.io.multiplicand.poke(x.U)
        c.io.multiplier.poke(y.U)
        c.io.start.poke(true.B)
        c.clock.step(1)
        c.io.start.poke(false.B)
        c.clock.step(16)
        c.io.ready.expect(true.B)
        c.io.prod.expect((x * y).U)
      }
    }
  }

  it should "Multiply signed integers" in {
    implicit val conf: Config = Config(debugMul = true)
    test(new Mul) { c =>
      for {x <- -10 to -5; y <- -10 to -5} {
        c.io.multiplicand.poke(("b" + x.toBinaryString).U) // 对负数，toBinaryString 给的就是 32 位补码
        c.io.multiplier.poke(("b" + y.toBinaryString).U)
        c.io.start.poke(true.B)
        c.clock.step(1)
        c.io.start.poke(false.B)
        c.clock.step(16)
        c.io.ready.expect(true.B)
        c.io.prod.expect((x * y).U)
      }
    }
  }
}
