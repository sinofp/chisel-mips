// See LICENSE for license details.

package cpu.writeback

import chisel3._
import chisel3.util.MuxLookup
import cpu.decode.CtrlSigDef._
import cpu.port.core.Core2WriteBack
import cpu.port.hazard.Writeback2Hazard
import cpu.port.stage.{Decode2WriteBack, Memory2WriteBack, WriteBack2Execute}
import cpu.util.{Config, DefCon}

class WriteBack(implicit c: Config = DefCon) extends MultiIOModule {
  val decode = IO(new Decode2WriteBack)
  val execute = IO(Flipped(new WriteBack2Execute))
  val memory = IO(Flipped(new Memory2WriteBack))
  val hazard = IO(Flipped(new Writeback2Hazard))
  val core = IO(new Core2WriteBack)

  // RegNext
  val pcp8 = RegNext(memory.pcp8)
  val pc_now = pcp8 - 8.U
  val sel_reg_wdata = RegNext(memory.sel_reg_wdata)
  val mem_rdata = RegNext(memory.mem_rdata)
  val alu_out = RegNext(memory.alu_out)
  val hi_wen = RegNext(Mux(hazard.flush, 0.U, memory.hi.wen))
  val hi = RegNext(memory.hi)
  val lo_wen = RegNext(Mux(hazard.flush, 0.U, memory.lo.wen))
  val lo = RegNext(memory.lo)
  // 在writeback flush的信号，是memory中的，也就是触发异常的指令。它可能也要写CP0
  // 但发生异常时，对CP0的修改应当是通过except_type给出的，所以这个wen也要被flush掉
  val c0_wen = RegNext(Mux(hazard.flush, 0.U, memory.c0.wen))
  val c0_waddr = RegNext(memory.c0.waddr)
  val c0_wdata = RegNext(memory.c0.wdata)
  // 这些也要RegNext么？
  val except_type = RegNext(memory.except_type)
  val is_in_delayslot = RegNext(memory.is_in_delayslot)

  // hilo
  val hilo = Module(new HILO)
  locally {
    import hilo.{io => hl}
    hl.hi_wen := hi_wen
    hl.lo_wen := lo_wen
    hl._hi := hi.wdata
    hl._lo := lo.wdata
  }

  // cp0
  val cp0 = Module(new CP0)
  locally {
    cp0.i.except_type := except_type
    cp0.i.pc_now := pc_now
    cp0.i.is_in_delayslot := is_in_delayslot
    import cp0.i._
    import cp0.o._
    wen := c0_wen
    waddr := c0_waddr
    wdata := c0_wdata
    raddr := execute.c0_raddr
    execute.c0_rdata := rdata
    int := core.int
    BadVAddr := DontCare
    Count := DontCare
    Compare := DontCare
    memory.c0_status := Status
    memory.c0_cause := Cause
    memory.c0_epc := EPC
    core.timer_int := timer_int
  }

  // write rf (& forward reg)
  decode.wen := RegNext(memory.rf.wen, false.B)
  decode.waddr := RegNext(memory.rf.waddr)
  decode.wdata := MuxLookup(sel_reg_wdata, 0.U, Array(
    SEL_REG_WDATA_EX -> alu_out,
    SEL_REG_WDATA_LNK -> pcp8,
    SEL_REG_WDATA_MEM -> mem_rdata,
  ))

  // forward reg
  hazard.rf.wen := decode.wen
  hazard.rf.waddr := decode.waddr
  // forward hilo
  hazard.hi.wen := hi_wen
  hazard.lo.wen := lo_wen
  execute.hi.wdata := hilo.io.hi
  execute.lo.wdata := hilo.io.lo
  // forward c0 to execute
  hazard.c0.wen := c0_wen
  hazard.c0.waddr := c0_waddr
  execute.c0_data := c0_wdata
  // forward c0 to memory
  memory.fwd_c0.wen := c0_wen
  memory.fwd_c0.waddr := c0_waddr
  memory.fwd_c0.wdata := c0_wdata

  core.junk_output := decode.wdata | hilo.io.lo | memory.alu_out | memory.mem_rdata | c0_wdata
}