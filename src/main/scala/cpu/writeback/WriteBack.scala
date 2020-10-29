// See LICENSE for license details.

package cpu.writeback

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.MuxCase
import cpu.decode.CtrlSigDef._
import cpu.util.Config

class WriteBack(implicit c: Option[Config] = None) extends MultiIOModule {
  val mw = IO(new Bundle() {
    val pc = Input(UInt(32.W))
    val reg_wen = Input(Bool())
    val sel_reg_wdata = Input(UInt(SZ_SEL_REG_WDATA))
    val reg_waddr = Input(UInt(5.W))
    val mem_rdata = Input(UInt(32.W))
    val alu_out = Input(UInt(32.W))
  })
  val wd = IO(new Bundle() {
    val wen = Output(Bool())
    val waddr = Output(UInt(5.W))
    val wdata = Output(UInt(32.W))
  })

  val pc = RegNext(mw.pc) // for trace & pc + 8
  val sel_reg_wdata = RegNext(mw.sel_reg_wdata)
  val mem_rdata = RegNext(mw.mem_rdata)
  val alu_out = RegNext(mw.alu_out)

  wd.wen := RegNext(mw.reg_wen, false.B)
  wd.waddr := RegNext(mw.reg_waddr)

  private val from = sel => sel_reg_wdata === sel
  wd.wdata := MuxCase(0.U, Array(
    from(SEL_REG_WDATA_ALU) -> alu_out,
    from(SEL_REG_WDATA_LNK) -> (pc + 8.U), // 这个+8不知放哪好
    from(SEL_REG_WDATA_MEM) -> mem_rdata,
  ))

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

object WriteBack extends App {
  new ChiselStage emitVerilog new WriteBack
}