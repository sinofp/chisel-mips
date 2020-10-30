// See LICENSE for license details.

package cpu.fetch

import chisel3._
import chiseltest._
import cpu.util.Config
import org.scalatest._

class InstMemTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "InstMem"

  it should "be able to write inst" in {
    implicit val conf = Some(new Config(inputInst = true, debugInstMem = true))
    test(new InstMem) { c =>
      import c.io._
      val ii = c.ii.get
      import ii._
      for (x <- 1 to 10) {
        wen.poke(true.B)
        waddr.poke(x.U)
        wdata.poke(x.U)
        pc.poke((x*4).U)
        c.clock.step(1)
        inst.expect(x.U)
        pc.poke(((x-1)*4).U)
        c.clock.step(1)
        inst.expect((x-1).U)
      }
    }
  }
}
