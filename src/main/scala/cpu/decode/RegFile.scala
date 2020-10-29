// See LICENSE for license details.

package cpu.decode

import chisel3._
import chisel3.stage.ChiselStage
import cpu.util.Config

class RegFile(readPorts: Int)(implicit c: Option[Config] = None) extends Module {
  require(readPorts >= 0)
  val io = IO(new Bundle() {
    val wen = Input(Bool())
    val waddr = Input(UInt(5.W))
    val wdata = Input(UInt(32.W))
    val raddr = Input(Vec(readPorts, UInt(5.W)))
    val rdata = Output(Vec(readPorts, UInt(32.W)))
  })

  val reg = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  import io._

  when(wen) {
    reg(waddr) := wdata
  }

  for (i <- 0 until readPorts) {
    when(raddr(i) === 0.U) {
      rdata(i) := 0.U
    }.otherwise {
      rdata(i) := reg(raddr(i))
    }
  }
}

object RegFile extends App {
  (new ChiselStage).emitVerilog(new RegFile(2))
}
