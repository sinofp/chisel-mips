// See LICENSE for license details.

package cpu.memory

import chisel3._
import cpu.port.{EMPort, MWPort, WritePort}
import cpu.util.{Config, DefCon}

class Memory(implicit c: Config = DefCon) extends MultiIOModule {
  val em = IO(Input(new EMPort))
  val mw = IO(Output(new MWPort))
  // forward
  val md = IO(Output(new WritePort))
  md.wen := mw.reg_wen
  md.waddr := mw.reg_waddr
  md.wdata := mw.mem_rdata

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
