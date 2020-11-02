// See LICENSE for license details.

package cpu

import chisel3._
import chiseltest._
import cpu.util.Config
import org.scalatest._

class TopTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Top"

  it should "work" in {
    val insts = Array(
      "20080064", // addi $t0, $0, 100
      "20090000", // addi $t1, $0, 0
      "01095020", // add $t2, $t0, $t1
    ).map("h" + _).map(_.U)
    implicit val c: Config = Config(insts = insts, debugRegFile = true)
    test(new Top) { c =>
      // todo 看看RegFile的内容对不对
      c.clock.step(7)
    }
  }
}
