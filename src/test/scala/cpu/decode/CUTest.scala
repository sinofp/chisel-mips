// See LICENSE for license details.

package cpu.decode

import chisel3._
import chiseltest._
import cpu.execute.ALU._
import cpu.decode.CtrlSigDef._
import org.scalatest._

class CUTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "CU"

  it should "output correct control signal" in {
    test(new CU) { c =>
      c.inst.poke("h20080064".U) // addi $t0, $0, 100
      c.ctrl.alu_fn.expect(FN_ADD)
      c.ctrl.sel_alu1.expect(SEL_ALU1_RS)
      c.ctrl.sel_alu2.expect(SEL_ALU2_IMM)
      c.ctrl.sel_imm.expect(SEL_IMM_S)
    }
  }
}
