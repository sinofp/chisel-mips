// See LICENSE for license details.

package cpu.fetch

import chisel3._
import chiseltest._
import cpu.util.Config
import org.scalatest.{FlatSpec, Matchers}

class FetchTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Fetch"

  it should "issue inst one by one" in {
    val insts = Array(
      "20080064", // addi $t0, $0, 100
      "20090000", // addi $t1, $0, 0
      "01095020", // add $t2, $t0, $t1
    ).map("h" + _).map(_.U)
    implicit val c: Config = Config(insts = insts)
    test(new Fetch) { c =>
      insts.foreach({ inst =>
        c.fd.inst.expect(inst)
        c.clock.step(1)
      })
    }
  }

  it should "increase PC" in {
    implicit val conf: Config = Config(dFetch = true)
    test(new Fetch) { c =>
      for (x <- 1 to 10) {
        c.fd.pcp4.expect((x * 4).U)
        c.clock.step(1)
      }
      c.reset.poke(true.B) // 让pc_now归零
      c.clock.step(1)
      c.reset.poke(false.B)
      for (x <- 1 to 10) {
        c.fd.pcp4.expect((x * 4).U)
        c.clock.step(1)
      }
    }
  }
}
