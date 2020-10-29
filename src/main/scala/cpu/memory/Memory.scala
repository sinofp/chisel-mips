// See LICENSE for license details.

package cpu.memory

import chisel3._
import cpu.decode.CtrlSigDef.{SZ_MEM_TYPE, SZ_SEL_REG_WDATA}

class Memory extends Module {
  val io = IO(new Bundle() {
    val em_mem_wen = Input(Bool())
    val em_mem_wdata = Input(UInt(32.W))
    val em_mem_size = Input(UInt(SZ_MEM_TYPE))
    val em_reg_wen = Input(Bool())
    val em_sel_reg_wdata = Input(UInt(SZ_SEL_REG_WDATA))
    val em_reg_waddr = Input(UInt(5.W))
    val em_alu_out = Input(UInt(32.W))
    val em_pc = Input(UInt(32.W))
    val mw_pc = Output(UInt(32.W))
    val mw_reg_wen = Output(Bool())
    val mw_sel_reg_wdata = Output(UInt(SZ_SEL_REG_WDATA))
    val mw_reg_waddr = Output(UInt(5.W))
    val mw_mem_rdata = Output(UInt(32.W))
    val mw_alu_out = Output(UInt(32.W))
  })

  import io._

  mw_pc := RegNext(em_pc)
  mw_reg_wen := RegNext(em_reg_wen, 0.U)
  mw_sel_reg_wdata := RegNext(em_sel_reg_wdata)
  mw_reg_waddr := RegNext(em_reg_waddr)
  mw_alu_out := RegNext(em_alu_out)

  val data_mem = Module(new DataMem)
  locally {
    import data_mem.io._
    wen := RegNext(em_mem_wen, 0.U)
    addr := RegNext(em_alu_out)
    wdata := RegNext(em_mem_wdata)
    mw_mem_rdata := rdata
    size := em_mem_size
  }
}
