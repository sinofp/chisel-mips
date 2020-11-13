// See LICENSE for license details.

package cpu.decode

import chisel3._
import chisel3.util.Counter
import chisel3.util.experimental.BoringUtils
import cpu.port.debug.TRegWindow
import cpu.port.stage.WDPort
import cpu.util.{Config, DefCon}

class RegFile(readPorts: Int)(implicit c: Config = DefCon) extends MultiIOModule {
  require(readPorts >= 0)
  val in = IO(Input(new WDPort))
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
    if (c.debugRegFile) {
      val cnt = Counter(true.B, 100)
      printf(p"[log RegFile]\n" +
        p"\tcycle = ${cnt._1}\n" +
        p"\t$$t0 = ${Hexadecimal(reg(8.U))}\n" +
        p"\t$$t1 = ${Hexadecimal(reg(9.U))}\n" +
        p"\t$$t2 = ${Hexadecimal(reg(10.U))}\n" +
        p"\t$$t3 = ${Hexadecimal(reg(11.U))}\n" +
        p"\t$$t4 = ${Hexadecimal(reg(12.U))}\n" +
        p"\t$$t5 = ${Hexadecimal(reg(13.U))}\n" +
        p"\t$$t6 = ${Hexadecimal(reg(14.U))}\n" +
        p"\t$$t7 = ${Hexadecimal(reg(15.U))}\n" +
        p"\t$$t8 = ${Hexadecimal(reg(24.U))}\n" +
        p"\t$$t9 = ${Hexadecimal(reg(25.U))}\n")
    }
  }

  val t_regs = if (c.debugTReg) Some(Wire(new TRegWindow())) else None
  if (t_regs.isDefined) {
    val t = t_regs.get
    import t._
    t0 := reg(8.U)
    t1 := reg(9.U)
    t2 := reg(10.U)
    t3 := reg(11.U)
    t4 := reg(12.U)
    t5 := reg(13.U)
    t6 := reg(14.U)
    t7 := reg(15.U)
    t8 := reg(24.U)
    t9 := reg(25.U)
    //    (t_regs.get.getElements, (8 to 15) ++ (24 to 25)).zipped.foreach { case (src, idx) => src := reg(idx.U) }
    t_regs.get.getElements.zipWithIndex.foreach { case (src, idx) => BoringUtils.addSource(src, s"reg$idx") }
  }
}