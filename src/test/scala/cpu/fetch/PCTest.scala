// See LICENSE for license details.

package cpu.fetch

import chisel3._
import chiseltest._
import org.scalatest._

class PCTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "PC"

  it should "be able to change" in {
    test(new PC) { c =>
      val last = 10
      for (x <- 0 to last) {
        c.io.pc_next.poke(x.U)
        c.clock.step(1)
        c.io.pc_now.expect(x.U)
      }
      // rst下个周期，pc归零
      c.reset.poke(true.B)
      c.io.pc_now.expect(last.U)
      c.clock.step(1)
      c.reset.poke(false.B) // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
      c.io.pc_now.expect(0.U)
      for (x <- 0 to last) {
        c.io.pc_next.poke(x.U)
        c.clock.step(1)
        c.io.pc_now.expect(x.U)
      }
    }
  }
}
