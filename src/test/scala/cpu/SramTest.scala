// See LICENSE for license details.

package cpu

import chisel3._
import chisel3.tester.{testableClock, testableData}
import chiseltest.ChiselScalatestTester
import cpu.fetch.InstMem
import cpu.memory.DataMem
import cpu.util.Config
import org.scalatest.{FlatSpec, Matchers}

class SramTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Sram"

  it should "return read value after request cycle" in {
    implicit val c: Config = Config(insts = (0 to 10).toArray.map(_.U))
    test(new InstMem) { c =>
      for (i <- 1 to 10) {
        c.io.addr.poke((i * 4).U)
        assert(c.io.rdata.peek.litValue != i, "下周期才能读出数据来")
        c.clock.step(1)
        c.io.rdata.expect(i.U)
      }
    }
    test(new DataMem) { c =>
      c.io.wen.poke("b1111".U)
      for (i <- 1 to 10) {
        c.io.addr.poke(i.U)
        c.io.wdata.poke(i.U)
        c.clock.step(1)
      }
      c.io.wen.poke(0.U)

      for (i <- 1 to 10) {
        c.io.addr.poke(i.U)
        assert(c.io.rdata.peek.litValue != i, "下周期才能读出数据来")
        c.clock.step(1)
        c.io.rdata.expect(i.U)
      }
    }
  }
}
