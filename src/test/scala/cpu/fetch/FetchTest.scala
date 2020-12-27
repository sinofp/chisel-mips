// See LICENSE for license details.

package cpu.fetch

import chisel3._
import chisel3.tester.{testableClock, testableData}
import chiseltest.ChiselScalatestTester
import org.scalatest.{FlatSpec, Matchers}

class FetchTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Fetch"

  it should "stall every two cycles" in {
    test(new Fetch) { c =>
      for (i <- 0 to 10) {
        c.hazard.sram_stall.expect((i % 2 == 0).B)
        c.clock.step(1)
      }
    }
  }
}
