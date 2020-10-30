// See LICENSE for license details.

package cpu.fetch

import chisel3._
import chiseltest._
import cpu.util.Config
import org.scalatest.{FlatSpec, Matchers}

class FetchTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Fetch"

  it should "work" in {
    implicit val c: Option[Config] = Some(new Config(inputInst = true))
    test(new Fetch) { c =>
      // 输入指令
      val insts = Array(
        "20080064", // addi $t0, $0, 100
        "20090000", // addi $t1, $0, 0
        "01095020", // addi $t0, $0, 100
      ).map("h" + _).map(_.U)
      locally {
        assert(c.ii.isDefined)
        assert(c.inst_mem.ii.isDefined)
        val ii = c.ii.get
        import ii._
        for (i <- insts.indices) {
          wen.poke(true.B)
          waddr.poke(i.U)
          wdata.poke(insts(i))
          c.clock.step(1)
        }
        // todo
//        import c.inst_mem.io._
        // 看看指令对不对，别被别的冲了
//        for (i <- insts.indices) {
//          pc.poke((i * 4).U)
//          c.clock.step(1)
//          inst.expect(insts(i))
//        }
      }
    }
  }

  it should "connect with InstMem" in {
    implicit val conf = Some(new Config(inputInst = true, debugInstMem = true, debugFetch = true))
    test(new Fetch) { c =>
      for (x <- 0 to 10) {
        c.ii.get.wen.poke(true.B)
        c.ii.get.waddr.poke(x.U)
        c.ii.get.wdata.poke(x.U)
        c.clock.step(1)
      }
      c.reset.poke(true.B) // 让PC的pc_now归零
      c.clock.step(1)
      c.reset.poke(false.B)
      for (x <- 0 to 10) {
        println(s"[log] x = $x")
        c.fd.inst.expect(x.U)
        c.clock.step(1)
      }
    }
  }
  it should "increase PC" in {
    implicit val conf = Some(new Config(debugFetch = true))
    test(new Fetch) { c =>
      for (x <- 1 to 10) {
        c.fd.pcp4.expect((x*4).U)
        c.clock.step(1)
      }
      c.reset.poke(true.B) // 让PC的pc_now归零
      c.clock.step(1)
      c.reset.poke(false.B)
      for (x <- 1 to 10) {
        c.fd.pcp4.expect((x*4).U)
        c.clock.step(1)
      }
    }
  }
}
