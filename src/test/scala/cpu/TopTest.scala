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
      "3c010000",
      "342189ab",
      "00014820",
      "00000000",
      "1000ffff",
      "00000000"
    ).map("h" + _).map(_.U)
    implicit val c: Config = Config(insts = insts, debugRegFile = true, debugDataMem = true, debugBrUnit = true, debugFetch = true)
    test(new Top) { c =>
      c.clock.step(10) // 16)
    }
  }
}
