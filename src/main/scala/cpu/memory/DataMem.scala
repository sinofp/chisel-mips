// See LICENSE for license details.

package cpu.memory

import chisel3._
import chisel3.util._
import cpu.port.core.SramIO
import cpu.util.{Config, DefCon}

class DataMem(implicit c: Config = DefCon) extends Module {
  val io = IO(Flipped(new SramIO))

  val mem = SyncReadMem(256, UInt(32.W))

  import io._

  when(wen.orR) {
    mem.write(addr, MuxLookup(wen, wdata, Array(
      "b0011".U -> Cat(Fill(16, wdata(15)), wdata(15, 0)),
      "b0001".U -> Cat(Fill(24, wdata(7)), wdata(7, 0)),
    )))
  }

  rdata := mem.read(addr)

  if (c.dDataMem) {
    val cnt = Counter(true.B, 100)
    printf(p"[log DataMem]\n\tcycle=${cnt._1}\n" +
      p"\twen = $wen, addr = ${Hexadecimal(addr)}, wdata = ${Hexadecimal(wdata)}, rdata = ${Hexadecimal(rdata)}\n" +
      p"\t0($$0) = ${Hexadecimal(mem.read(0.U))}\n")
  }
}