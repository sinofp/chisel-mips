// See LICENSE for license details.

package cpu.writeback

import chisel3._
import chiseltest._
import org.scalatest._

class HILOTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "HILO"

  it should "work as register files" in {
    test(new HILO) { c =>
      val testValues = for {x <- 0 to 10; y <- 0 to 10} yield (x, y)

      for (wen <- 0 to 1) {
        testValues.foreach { case (x, y) =>
          c.io.hi_wen.poke(wen.B)
          c.io._hi.poke(x.U)
          c.io._lo.poke(y.U)
          c.clock.step(1)
          c.io.hi.expect(if (wen == 1) x.U else 0.U)
          c.io.lo.expect(0.U)
        }
      }
    }
  }
}
