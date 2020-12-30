// See LICENSE for license details.

package cpu

import chisel3._
import chiseltest._
import cpu.util.Config
import org.scalatest._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.VerilatorBackendAnnotation

import java.io.{File, PrintWriter}
import scala.io.Source
import scala.sys.process._

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
    implicit val c: Config = Config(insts = insts, dRegFile = true, dBuiltinMem = true, dBrUnit = true, dExecute = true, dTReg = true)
    test(new Top) { c =>
      c.clock.step(99)
      c.t_regs.get.t0.expect(100.U)
      c.t_regs.get.t1.expect("h13".U)
    }
  }

  it should "handle load stall" in {
    // addi $t1, $0, 0x1234 -- cycle 5 (0开始数) $t1 更新
    // sw $t1, 0($0) -- cycle 7 0($0) 更新
    // addi $t2, $0, 0x1234 -- cycle 7
    // addi $t1, $0, 0 -- cycle 8
    // lw $t1, 0($0) -- cycle 9 $t1 更新 ∴ cycle 8 是这条的 writeback
    // beq $t1, $t2, label -- ∵ ↑  ∴ cycle 7 是这条的原定 execute, c8 是这条的被 load stall 推迟的 execute
    // nop
    // addi $t1, $0, 0x4567
    // label:
    // addi $t1, $0, 0x89ab
    // nop
    // _loop:
    // beq $0, $0, _loop
    // nop
    val insts = Array(
      "20091234",
      "ac090000",
      "200a1234",
      "20090000",
      "8c090000",
      "112a0002",
      "00000000",
      "20094567",
      "200989ab",
      "00000000",
      "1000ffff",
      "00000000"
    ).map("h" + _).map(_.U)
    implicit val c: Config = Config(insts = insts, dBuiltinMem = true,
      dFetch = true, dTReg = true, dExcept = true)
    test(new Top) { c =>
      c.clock.step(14)
      c.t_regs.get.t1.expect("hffff89ab".U)
    }
  }

  it should "handle hilo" in {
    val insts = Array(
      "20091234", // addi $t1, $0, 0x1234
      "01200011", // mthi $t1 -- cycle 6
      "00005010", // mfhi $t2 -- cycle 7
      // "00000000", // 测试从m前推到d
      "01494820", // add $t1, $t2, $t1 -- execute 在 cycle 5
      "00000011", // mthi $0
      "01200011", // mthi $t1
      "01400011", // mthi $t2
      "00006010", // mfhi $t4
    ).map("h" + _).map(_.U)
    implicit val c: Config = Config(insts = insts, dRegFile = true, dTReg = true,
      dHILO = true, dExecute = true, dForward = true, dBuiltinMem = true)
    test(new Top) { c =>
      c.clock.step(7)
      c.t_regs.get.t2.expect("h1234".U)
      c.clock.step(1)
      // c.clock.step(1)
      c.t_regs.get.t1.expect("h2468".U)
      c.clock.step(4)
      c.t_regs.get.t4.expect("h1234".U)
    }
  }

  it should "jump" in {
    val insts = Array(
      "08100003", // j aaa
      "20090001", // addi $t1, $0, 1
      "200a0002", // addi $t2, $0, 2 -- 跳过
      "200b0003", // aaa: addi $t3, $0, 3
    ).map("h" + _).map(_.U)
    implicit val c: Config = Config(insts = insts, dRegFile = true, dBuiltinMem = true, dTReg = true)
    test(new Top) { c =>
      c.clock.step(7)
      c.t_regs.get.t2.expect(0.U)
      c.t_regs.get.t3.expect(3.U)
    }
  }

  it should "handle syscall and eret" in {
    val writer = new PrintWriter(new File("mips.S"))
    try writer.write(
      """j _start
        |nop
        |addi $t2, $0, 200
        |mfc0 $t9, $14
        |addi $t9, $t9, 4
        |mtc0 $t9, $14
        |eret
        |
        |_start:
        |addi $t1, $0, 100
        |syscall
        |add $t3, $t2, $t1
        |""".stripMargin)
    finally writer.close()
    "java -jar Mars4_5.jar mc CompactTextAtZero a dump .text HexText inst.txt mips.S" !
    val source = Source.fromFile("inst.txt")
    val insts = try source.getLines.toArray.map("h" + _).map(_.U) finally source.close
    implicit val c: Config = Config(insts = insts, dTReg = true, dBuiltinMem = true,
      dExcept = true, dExceptEntry = Some((2 * 4).U), dFetch = true)
    test(new Top) { c =>
      c.clock.step(7)
      c.t_regs.get.t1.expect(100.U)
      c.clock.step(5)
      c.t_regs.get.t2.expect(200.U)
      c.clock.step(8)
      c.t_regs.get.t3.expect(300.U)
    }
  }

  it should "pass bitmips_experiments lab5" in {
    println("Test start:")
    val instMemSize = 131072
    val coe = Source.fromFile("inst_ram.coe")
    val insts = try coe.getLines.drop(2).take(instMemSize).toArray.map("h" + _).map(_.U) finally coe.close
    val trace = Source.fromFile("golden_trace.txt")
    val ref = try trace.getLines.drop(1) finally trace.close
    implicit val c: Config = Config(insts = insts, instMemSize = instMemSize, dTeachSoc = true)
    test(new Top)//.withAnnotations(Seq(VerilatorBackendAnnotation))
    { c =>
      println("Test begin:")
      while (c.debug_wb.get.pc.peek.litValue != 0xbfc00004) {
        println(s"I'm not dead... pc @ ${c.debug_wb.get.pc.peek.litValue}")
        c.clock.step(1)
      }
      while (c.debug_wb.get.pc.peek.litValue != 0xbfc00100) {
        c.clock.step(1)
        val _ :: pc:: wnum:: wdata :: _ = ref.next.split(' ').toList.map("h" + _).map(_.U)
        c.debug_wb.get.pc.expect(pc)
        c.debug_wb.get.rf_wnum.expect(wnum)
        c.debug_wb.get.rf_wdata.expect(wdata)
      }
    }
  }
}
