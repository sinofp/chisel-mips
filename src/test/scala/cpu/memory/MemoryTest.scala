// See LICENSE for license details.

package cpu.memory

import chisel3._
import chiseltest._
import cpu.decode.CtrlSigDef._
import org.scalatest._

class MemoryTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Memory"

  it should "work as memory" in {
    test(new Memory) { c =>
      val testValues = for {x <- 0 to 10; y <- 5 to 10} yield (x, y)

      for (wen <- 0 to 1) {
        testValues.foreach { case (x, y) =>
          c.em.mem_wen.poke(wen.B)
          c.em.mem_size.poke(MEM_W)
          c.em.alu_out.poke(x.U)
          c.em.mem_wdata.poke(y.U)
          c.clock.step(2) // 一个周期缓存上个流水线，一个周期写入
          c.mw.mem_rdata.expect(if (wen == 0) 0.U else y.U)
        }
      }
    }
  }
}
