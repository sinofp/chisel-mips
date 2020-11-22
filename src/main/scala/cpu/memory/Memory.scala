// See LICENSE for license details.

package cpu.memory

import chisel3._
import chisel3.util.MuxCase
import cpu.decode.CtrlSigDef._
import cpu.port.hazard.Memory2Hazard
import cpu.port.stage.{Decode2Memory, Execute2Memory, Memory2WriteBack}
import cpu.util.{Config, DefCon}

class Memory(implicit c: Config = DefCon) extends MultiIOModule {
  val decode = IO(Flipped(new Decode2Memory))
  val execute = IO(new Execute2Memory)
  val writeback = IO(new Memory2WriteBack)
  val hazard = IO(Flipped(new Memory2Hazard))

  // RegNext
  writeback.pcp8 := RegNext(execute.pcp8)
  writeback.reg_wen := RegNext(execute.reg_wen, 0.U)
  writeback.sel_reg_wdata := RegNext(execute.sel_reg_wdata)
  writeback.reg_waddr := RegNext(execute.reg_waddr)
  writeback.alu_out := RegNext(execute.alu_out)
  writeback.hi_wen := RegNext(execute.hi_wen)
  writeback.hi := RegNext(execute.hi)
  writeback.lo_wen := RegNext(execute.lo_wen)
  writeback.lo := RegNext(execute.lo)
  writeback.c0_wen := RegNext(execute.c0_wen)
  writeback.c0_waddr := RegNext(execute.c0_waddr)
  writeback.c0_wdata := RegNext(execute.c0_wdata)

  // data mem
  val data_mem = Module(new DataMem)
  locally {
    import data_mem.io._
    wen := RegNext(execute.mem_wen, 0.U)
    addr := RegNext(execute.alu_out)
    wdata := RegNext(execute.mem_wdata)
    writeback.mem_rdata := rdata
    size := execute.mem_size
  }

  // forward reg
  hazard.wen := writeback.reg_wen
  hazard.waddr := writeback.reg_waddr
  // forward hilo
  hazard.hi_wen := writeback.hi_wen
  hazard.lo_wen := writeback.lo_wen
  // forward cp0
  hazard.c0_wen := writeback.c0_waddr
  hazard.c0_waddr := writeback.c0_waddr
  execute.hi_forward := writeback.hi
  execute.lo_forward := writeback.lo
  execute.c0_data := writeback.c0_wdata
  decode.wdata := MuxCase(0.U, Array(
    (writeback.sel_reg_wdata === SEL_REG_WDATA_EX) -> writeback.alu_out,
    (writeback.sel_reg_wdata === SEL_REG_WDATA_LNK) -> writeback.pcp8,
    (writeback.sel_reg_wdata === SEL_REG_WDATA_MEM) -> writeback.mem_rdata,
  ))
}
