// See LICENSE for license details.

package cpu.writeback

import chisel3._
import chisel3.util.Counter
import cpu.util.{Config, DefCon}

class HILO(implicit c: Config = DefCon) extends Module {
  val io = IO(new Bundle() {
    val hi_wen = Input(Bool())
    val lo_wen = Input(Bool())
    val _hi    = Input(UInt(32.W))
    val _lo    = Input(UInt(32.W))
    val hi     = Output(UInt(32.W))
    val lo     = Output(UInt(32.W))
  })

  import io._

  hi := RegNext(Mux(hi_wen, _hi, hi), 0.U)
  lo := RegNext(Mux(lo_wen, _lo, lo), 0.U)

  if (c.dHILO) {
    val cnt = Counter(true.B, 100)
    printf(p"[log HILO]\n\tcycle = ${cnt._1}\n\thi = ${Hexadecimal(hi)}, lo = ${Hexadecimal(lo)}\n")
  }
}
