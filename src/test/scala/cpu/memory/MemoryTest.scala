//// See LICENSE for license details.
//
//package cpu.memory
//
//import chisel3._
//import chisel3.tester.{testableClock, testableData}
//import chiseltest.ChiselScalatestTester
//import org.scalatest.{FlatSpec, Matchers}
//
//class MemoryTest extends FlatSpec with ChiselScalatestTester with Matchers {
//  behavior of "Memory"
//
//  it should "stall after read" in {
//    test(new Memory) { c =>
//      c.execute.data_sram_en.poke(true.B); c.clock.step(1)
//      c.hazard.sram_stall.expect(true.B)
//      c.clock.step(1)
//      c.execute.data_sram_en.poke(true.B)
//      c.execute.mem.wen.poke(true.B); c.clock.step(1)
//      c.hazard.sram_stall.expect(false.B)
//    }
//  }
//}
