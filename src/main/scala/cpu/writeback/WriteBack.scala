// See LICENSE for license details.

package cpu.writeback

import chisel3._
import chisel3.util.MuxLookup
import cpu.decode.CtrlSigDef._
import cpu.port.core.Core2WriteBack
import cpu.port.hazard.Writeback2Hazard
import cpu.port.stage.{Memory2WriteBack, WriteBack2Decode, WriteBack2Execute}
import cpu.util.{Config, DefCon}

class WriteBack(implicit c: Config = DefCon) extends MultiIOModule {
  val decode = IO(Flipped(new WriteBack2Decode))
  val execute = IO(Flipped(new WriteBack2Execute))
  val memory = IO(Flipped(new Memory2WriteBack))
  val hazard = IO(Flipped(new Writeback2Hazard))
  val core = IO(new Core2WriteBack)

  // RegNext
  val pcp8 = RegNext(memory.pcp8)
  val pc = pcp8 - 8.U
  val sel_reg_wdata = RegNext(memory.sel_reg_wdata)
  val mem_rdata = RegNext(memory.mem_rdata)
  val alu_out = RegNext(memory.alu_out)
  val hi_wen = RegNext(memory.hi_wen)
  val hi = RegNext(memory.hi)
  val lo_wen = RegNext(memory.lo_wen)
  val lo = RegNext(memory.lo)
  val c0_wen = RegNext(memory.c0_wen)
  val c0_waddr = RegNext(memory.c0_waddr)
  val c0_wdata = RegNext(memory.c0_wdata)
  // 这些也要RegNext么？
  val except_type = RegNext(memory.except_type)
  val pc_now = RegNext(memory.pc_now)
  val is_in_delayslot = RegNext(memory.is_in_delayslot)

  // hilo
  val hilo = Module(new HILO)
  locally {
    import hilo.{io => hl}
    hl.hi_wen := hi_wen
    hl.lo_wen := lo_wen
    hl._hi := hi
    hl._lo := lo
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
  decode.wen := RegNext(memory.reg_wen, false.B)
  decode.waddr := RegNext(memory.reg_waddr)
  decode.wdata := MuxLookup(sel_reg_wdata, 0.U, Array(
    SEL_REG_WDATA_EX -> alu_out,
    SEL_REG_WDATA_LNK -> pcp8,
    SEL_REG_WDATA_MEM -> mem_rdata,
  ))

  // forward reg
  hazard.wen := decode.wen
  hazard.waddr := decode.waddr
  // forward hilo
  hazard.hi_wen := hi_wen
  hazard.lo_wen := lo_wen
  execute.hi := hilo.io.hi
  execute.lo := hilo.io.lo
  // forward c0 to execute
  hazard.c0_wen := c0_wen
  hazard.c0_waddr := c0_waddr
  execute.c0_data := c0_wdata
  // forward c0 to memory
  memory.c0_wen_f := c0_wen
  memory.c0_waddr_f := c0_waddr
  memory.c0_wdata_f := c0_wdata
}