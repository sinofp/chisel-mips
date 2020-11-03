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
      "21290001", // loop: addi $t1, $t1, 1
      "01285022", // sub $t2, $t1, $t0
      "0540fffd", // bltz $t2, loop，后面全是初始化的NOP，延迟槽不用担心
    ).map("h" + _).map(_.U)
    implicit val c: Config = Config(insts = insts, debugRegFile = true, debugBrUnit = true, debugExecute = true)
    test(new Top) { c =>
      // todo 看看RegFile的内容对不对
      c.clock.step(99)
    }
  }
}
