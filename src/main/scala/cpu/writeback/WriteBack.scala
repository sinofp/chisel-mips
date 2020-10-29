// See LICENSE for license details.

package cpu.writeback

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.MuxCase
import cpu.decode.CtrlSigDef._

class WriteBack extends Module {
  val io = IO(new Bundle() {
    val mw_pc = Input(UInt(32.W))
    val mw_reg_wen = Input(Bool())
    val mw_sel_reg_wdata = Input(UInt(SZ_SEL_REG_WDATA))
    val mw_reg_waddr = Input(UInt(5.W))
    val mw_mem_rdata = Input(UInt(32.W))
    val mw_alu_out = Input(UInt(32.W))
    val wd_wen = Output(Bool())
    val wd_waddr = Output(UInt(5.W))
    val wd_wdata = Output(UInt(32.W))
  })

  import io._

  val pc = RegNext(mw_pc) // for trace & pc + 8
  val sel_reg_wdata = RegNext(mw_sel_reg_wdata)
  val mem_rdata = RegNext(mw_mem_rdata)
  val alu_out = RegNext(mw_alu_out)

  wd_wen := RegNext(mw_reg_wen, false.B)
  wd_waddr := RegNext(mw_reg_waddr)

  private val from = sel => sel_reg_wdata === sel
  wd_wdata := MuxCase(0.U, Array(
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