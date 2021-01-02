// See LICENSE for license details.

package cpu

import chisel3._
import chiseltest._
import cpu.util.Config
import org.scalatest._

import java.io.{File, PrintWriter}
import scala.io.Source
import scala.sys.process._

class TopTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Top"

  def marsDump(asm: String) = {
    val writer = new PrintWriter(new File("mips.S"))
    try writer.write(asm)
    finally writer.close()
    "java -jar Mars4_5.jar mc CompactTextAtZero a dump .text HexText inst.txt mips.S" !
    val source = Source.fromFile("inst.txt")
    try source.getLines.toArray.map("h" + _).map(_.U) finally source.close
  }

  it should "work" in {
    implicit val c: Config = Config(insts = Array(
      "20080064", // addi $t0, $0, 100
      "20090000", // addi $t1, $0, 0
      "21290001", // loop: addi $t1, $t1, 1
      "01285022", // sub $t2, $t1, $t0
      "0540fffd", // bltz $t2, loop，后面全是初始化的NOP，延迟槽不用担心
    ).map("h" + _).map(_.U), dRegFile = true, dBuiltinMem = true, dBrUnit = true, dExecute = true, dTReg = true)
    test(new Top) { c =>
      c.clock.step(99)
      c.t_regs.get.t0.expect(100.U)
      c.t_regs.get.t1.expect("h13".U)
    }
  }

  it should "handle load stall" in {
    implicit val c: Config = Config(insts = marsDump(
      """addi $t1, $0, 0x1234 # cycle 5 (0开始数) $t1 更新
        |sw $t1, 0($0)        # cycle 7 0($0) 更新
        |addi $t2, $0, 0x1234 # cycle 7
        |addi $t1, $0, 0      # cycle 8
        |lw $t1, 0($0)        # cycle 9 $t1 更新 ∴ cycle 8 是这条的 writeback
        |beq $t1, $t2, label  # ∵ ↑  ∴ cycle 7 是这条的原定 execute, c8 是这条的被 load stall 推迟的 execute
        |nop
        |addi $t1, $0, 0x4567
        |label:
        |addi $t1, $0, 0x7654
        |nop
        |_loop:
        |beq $0, $0, _loop
        |nop
        |""".stripMargin), dBuiltinMem = true, dFetch = true, dTReg = true, dExcept = true)
    test(new Top) { c =>
      def t1 = c.t_regs.get.t1.peek.litValue

      while (0x7654 != t1) {
        assert(t1 != 0x4567, "beq跳转不成功")
        c.clock.step(1)
      }
    }
  }

  it should "handle hilo" in {
    implicit val c: Config = Config(insts = Array(
      "20091234", // addi $t1, $0, 0x1234
      "01200011", // mthi $t1 -- cycle 6
      "00005010", // mfhi $t2 -- cycle 7
      // "00000000", // 测试从m前推到d
      "01494820", // add $t1, $t2, $t1 -- execute 在 cycle 5
      "00000011", // mthi $0
      "01200011", // mthi $t1
      "01400011", // mthi $t2
      "00006010", // mfhi $t4
    ).map("h" + _).map(_.U), dRegFile = true, dTReg = true,
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
    implicit val c: Config = Config(insts = Array(
      "08100003", // j aaa
      "20090001", // addi $t1, $0, 1
      "200a0002", // addi $t2, $0, 2 -- 跳过
      "200b0003", // aaa: addi $t3, $0, 3
    ).map("h" + _).map(_.U), dRegFile = true, dBuiltinMem = true, dTReg = true)
    test(new Top) { c =>
      c.clock.step(7)
      c.t_regs.get.t2.expect(0.U)
      c.t_regs.get.t3.expect(3.U)
    }
  }

  it should "handle syscall and eret" in {
    implicit val c: Config = Config(insts = marsDump(
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
        |""".stripMargin), dTReg = true, dBuiltinMem = true,
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

  it should "fragment 1" in {
    implicit val c: Config = Config(insts = marsDump(
      """lui $9, 0xbfc0
        |addiu $9, $9, 0x704
        |lui $10, 0x2000
        |subu $25, $9, $10
        |""".stripMargin), dTReg = true, dBuiltinMem = true,
      dForward = true, dRegFile = true, dExecute = true)
    test(new Top) { c =>
      c.clock.step(5) // lui
      c.t_regs.get.t1.expect("hbfc00000".U)
      c.clock.step(1) // addiu
      c.t_regs.get.t1.expect("hbfc00704".U)
      c.clock.step(1) // lui
      c.t_regs.get.t2.expect("h20000000".U)
      c.clock.step(1) // subu
      c.t_regs.get.t9.expect("h9fc00704".U)
    }
  }
}
