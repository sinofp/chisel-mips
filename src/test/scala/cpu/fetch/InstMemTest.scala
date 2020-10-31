// See LICENSE for license details.

package cpu.fetch

import chisel3._
import chiseltest._
import cpu.util.Config
import org.scalatest._

class InstMemTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "InstMem"

  it should "be able to write inst rom" in {
    val last = 10
    val insts = (0 to last).map(_.U)
    implicit val conf: Config = Config(insts = insts, debugInstMem = true)
    test(new InstMem) { c =>
      import c.io._
      for (x <- 1 to last) {
        pc.poke((x * 4).U)
        c.clock.step(1)
        inst.expect(x.U)
        pc.poke(((x - 1) * 4).U)
        c.clock.step(1)
        inst.expect((x - 1).U)
      }
    }
  }
}
