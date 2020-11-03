// See LICENSE for license details.

package cpu.writeback

import chisel3._
import chisel3.util.MuxCase
import cpu.decode.CtrlSigDef._
import cpu.port.hazard.WHPort
import cpu.port.stage.{MWPort, WDPort}
import cpu.util.{Config, DefCon}

class WriteBack(implicit c: Config = DefCon) extends MultiIOModule {
  val mw = IO(Input(new MWPort))
  val wd = IO(Output(new WDPort))
  val hw = IO(Flipped(new WHPort))

  val pcp8 = RegNext(mw.pcp8 - 8.U)
  val pc = pcp8 - 8.U
  val sel_reg_wdata = RegNext(mw.sel_reg_wdata)
  val mem_rdata = RegNext(mw.mem_rdata)
  val alu_out = RegNext(mw.alu_out)

  wd.wen := RegNext(mw.reg_wen, false.B)
  wd.waddr := RegNext(mw.reg_waddr)
  wd.wdata := {
    val from = sel => sel_reg_wdata === sel
    MuxCase(0.U, Array(
      from(SEL_REG_WDATA_ALU) -> alu_out,
      from(SEL_REG_WDATA_LNK) -> pcp8,
      from(SEL_REG_WDATA_MEM) -> mem_rdata,
    ))
  }
  hw.wen := wd.wen
  hw.waddr := wd.waddr

  val hilo = Module(new HILO)
  locally {
    import hilo.io._
    wen := DontCare
    _hi := DontCare
    _lo := DontCare
    hi := DontCare
    lo := DontCare
  }
}