// See LICENSE for license details.

package cpu.fetch

import chisel3._
import cpu.port.core.Core2InstMem
import cpu.util.{Config, DefCon}

class InstMem(implicit c: Config = DefCon) extends MultiIOModule {
  val io = IO(Flipped(new Core2InstMem))

  val mem = VecInit(c.insts ++ Seq.fill(32 - c.insts.length)(0.U(32.W)))

  io.inst := mem(io.pc / 4.U)

  if (c.dInstMem) {
    printf(p"[log InstMem]\n" +
      p"\tinst 1 = ${Hexadecimal(mem(0.U))}\n" +
      p"\tinst 2 = ${Hexadecimal(mem(1.U))}\n" +
      p"\tinst 3 = ${Hexadecimal(mem(2.U))}\n" +
      p"\tinst 4 = ${Hexadecimal(mem(3.U))}\n" +
      p"\tinst 5 = ${Hexadecimal(mem(4.U))}\n")
  }
}