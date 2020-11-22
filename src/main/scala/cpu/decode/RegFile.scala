// See LICENSE for license details.

package cpu.decode

import chisel3._
import chisel3.util.Counter
import chisel3.util.experimental.BoringUtils
import cpu.port.debug.TRegWindow
import cpu.port.stage.WriteBack2Decode
import cpu.util.{Config, DefCon}

class RegFile(readPorts: Int)(implicit c: Config = DefCon) extends MultiIOModule {
  require(readPorts >= 0)
  val in = IO(new WriteBack2Decode)
  val io = IO(new Bundle() {
    val raddr = Input(Vec(readPorts, UInt(5.W)))
    val rdata = Output(Vec(readPorts, UInt(32.W)))
  })

  val reg = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  import in._
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

  val t_regs = if (c.dTReg || c.dRegFile) Some(Wire(new TRegWindow())) else None
  if (t_regs.isDefined) {
    (t_regs.get.getElements.reverse, (8 to 15) ++ (24 to 25)).zipped.foreach { case (src, idx) => src := reg(idx.U) }
  }
  if (c.dTReg) {
    t_regs.get.getElements.reverse.zipWithIndex.foreach { case (src, idx) => BoringUtils.addSource(src, s"reg$idx") }
  }
  if (c.dRegFile) {
    val cnt = Counter(true.B, 100)
    printf(p"[log RegFile]\n\tcycle = ${cnt._1}\n" + t_regs.get.getElements.reverse
      .zipWithIndex.foldLeft(p"") { case (old, (treg: UInt, idx)) => old + p"\t$$t$idx = ${Hexadecimal(treg)}\n" })
  }
}