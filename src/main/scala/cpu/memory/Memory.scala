// See LICENSE for license details.

package cpu.memory

import chisel3._
import chisel3.util.MuxCase
import cpu.decode.CtrlSigDef._
import cpu.port.hazard.{MHPort, WdataPort}
import cpu.port.stage.{Execute2Memory, Memory2Execute, Memory2WriteBack}
import cpu.util.{Config, DefCon}

class Memory(implicit c: Config = DefCon) extends MultiIOModule {
  val em = IO(Input(new Execute2Memory))
  val mw = IO(Output(new Memory2WriteBack))
  // forward
  val hm = IO(Flipped(new MHPort))
  hm.wen := mw.reg_wen
  hm.waddr := mw.reg_waddr
  // forward hilo
  hm.hi_wen := mw.hi_wen
  hm.lo_wen := mw.lo_wen
  // forward cp0
  hm.c0_wen := mw.c0_waddr
  hm.c0_waddr := mw.c0_waddr
  val me = IO(Output(new Memory2Execute))
  me.hi := mw.hi
  me.lo := mw.lo
  me.c0_data := mw.c0_wdata
  val md = IO(Output(new WdataPort))
  md.wdata := MuxCase(0.U, Array(
    (mw.sel_reg_wdata === SEL_REG_WDATA_EX) -> mw.alu_out,
    (mw.sel_reg_wdata === SEL_REG_WDATA_LNK) -> mw.pcp8,
    (mw.sel_reg_wdata === SEL_REG_WDATA_MEM) -> mw.mem_rdata,
  ))

  mw.pcp8 := RegNext(em.pcp8)
  mw.reg_wen := RegNext(em.reg_wen, 0.U)
  mw.sel_reg_wdata := RegNext(em.sel_reg_wdata)
  mw.reg_waddr := RegNext(em.reg_waddr)
  mw.alu_out := RegNext(em.alu_out)
  mw.hi_wen := RegNext(em.hi_wen)
  mw.hi := RegNext(em.hi)
  mw.lo_wen := RegNext(em.lo_wen)
  mw.lo := RegNext(em.lo)
  mw.c0_wen := RegNext(em.c0_wen)
  mw.c0_waddr := RegNext(em.c0_waddr)
  mw.c0_wdata := RegNext(em.c0_wdata)

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
