// See LICENSE for license details.

package cpu.memory

import chisel3._
import chisel3.util._
import cpu.decode.CtrlSigDef._
import cpu.util.{Config, DefCon}

class DataMem(implicit c: Config = DefCon) extends Module {
  val io = IO(new Bundle() {
    val addr = Input(UInt(32.W))
    val wen = Input(Bool())
    val wdata = Input(UInt(32.W))
    val size = Input(UInt(SZ_MEM_TYPE))
    val rdata = Output(UInt(32.W))
  })

  val mem = SyncReadMem(256, UInt(32.W))

  import io._

  when(wen) {
    mem.write(addr, MuxCase(wdata, Array(
      (size === MEM_H) -> Cat(Fill(16, wdata(15)), wdata(15, 0)),
      (size === MEM_B) -> Cat(Fill(24, wdata(7)), wdata(7, 0)),
    )))
  }

  val rword = mem.read(addr)

  rdata := MuxCase(rword, Array(
    (size === MEM_HU) -> Cat(Fill(16, 0.U), rword(15, 0)),
    (size === MEM_H) -> Cat(Fill(16, rword(15)), rword(15, 0)),
    (size === MEM_BU) -> Cat(Fill(24, 0.U), rword(7, 0)),
    (size === MEM_B) -> Cat(Fill(24, rword(7)), rword(7, 0)),
  ))

  if (c.dDataMem) {
    val cnt = Counter(true.B, 100)
    printf(p"[log DataMem]\n\tcycle=${cnt._1}\n" +
      p"\twen = $wen, addr = ${Hexadecimal(addr)}, wdata = ${Hexadecimal(wdata)}, " +
      p"size = ${Hexadecimal(size)}, rdata_word = ${Hexadecimal(rword)},  rdata = ${Hexadecimal(rdata)}\n" +
      p"\t0($$0) = ${Hexadecimal(mem.read(0.U))}\n")
  }
}