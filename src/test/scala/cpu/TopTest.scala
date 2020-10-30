// See LICENSE for license details.

package cpu

import chisel3._
import chiseltest._
import cpu.util.Config
import org.scalatest._

class TopTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Top"

  it should "work" in {
    implicit val c: Option[Config] = Some(new Config(inputInst = true))
    test(new Top) { c =>
      // 输入指令
      val insts = Array(
        "20080064",
        "20090000",
        "01095020",
      ).map("h" + _).map(_.U)

      for (i <- insts.indices) {
        c.ii.get.wen.poke(true.B)
        c.ii.get.waddr.poke(i.U)
        c.ii.get.wdata.poke(insts(i))
        c.clock.step(1)
      }
      //todo
//      import c.fetch.inst_mem.io._
//      // 看看指令对不对，别被别的冲了
//      for (i <- insts.indices) {
//        pc.poke((i * 4).U)
//        c.clock.step(1)
//        inst.expect(insts(i))
//      }
    }
  }
}
