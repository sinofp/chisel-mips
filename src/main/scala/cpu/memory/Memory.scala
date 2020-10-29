// See LICENSE for license details.

package cpu.memory

import chisel3._
import cpu.decode.CtrlSigDef.{SZ_MEM_TYPE, SZ_SEL_REG_WDATA}

class Memory extends MultiIOModule {
  val em = IO(new Bundle() {
    val mem_wen = Input(Bool())
    val mem_wdata = Input(UInt(32.W))
    val mem_size = Input(UInt(SZ_MEM_TYPE))
    val reg_wen = Input(Bool())
    val sel_reg_wdata = Input(UInt(SZ_SEL_REG_WDATA))
    val reg_waddr = Input(UInt(5.W))
    val alu_out = Input(UInt(32.W))
    val pc = Input(UInt(32.W))
  })
  val mw = IO(new Bundle() {
    val pc = Output(UInt(32.W))
    val reg_wen = Output(Bool())
    val sel_reg_wdata = Output(UInt(SZ_SEL_REG_WDATA))
    val reg_waddr = Output(UInt(5.W))
    val mem_rdata = Output(UInt(32.W))
    val alu_out = Output(UInt(32.W))
  })

  mw.pc := RegNext(em.pc)
  mw.reg_wen := RegNext(em.reg_wen, 0.U)
  mw.sel_reg_wdata := RegNext(em.sel_reg_wdata)
  mw.reg_waddr := RegNext(em.reg_waddr)
  mw.alu_out := RegNext(em.alu_out)

  val data_mem = Module(new DataMem)
  locally {
    import data_mem.io._
    wen := RegNext(em.mem_wen, 0.U)
    addr := RegNext(em.alu_out)
    wdata := RegNext(em.mem_wdata)
    mw.mem_rdata := rdata
    size := em.mem_size
  }
}
