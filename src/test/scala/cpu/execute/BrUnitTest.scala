// See LICENSE for license details.

package cpu.execute

import chisel3._
import chiseltest._
import cpu.decode.CtrlSigDef._
import org.scalatest._

class BrUnitTest extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "BrUnit"

  it should "compare with zero as signed" in {
    test(new BrUnit) { c =>
      // 0, 1, -1, -2
      // 负数的话，用scala的Int转UInt有问题
      Seq("0" * 32, "0" * 31 + "1", "1" * 32, "1" * 31 + "0").foreach(s => {
        c.io.br_type.poke(BR_TYPE_GE)
        c.io.sub_res.poke(("b" + s).U)
        c.io.branch.expect((s(0) == '0').B)
      })
    }
  }
}
