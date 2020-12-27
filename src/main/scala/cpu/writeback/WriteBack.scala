// See LICENSE for license details.

package cpu.writeback

import Chisel.Fill
import chisel3._
import chisel3.util.MuxLookup
import chisel3.util.experimental.BoringUtils
import cpu.decode.CtrlSigDef._
import cpu.port.debug.DebugWb
import cpu.port.hazard.Writeback2Hazard
import cpu.port.stage.{Decode2WriteBack, Memory2WriteBack, WriteBack2Execute}
import cpu.util.{Config, DefCon}

class WriteBack(implicit c: Config = DefCon) extends MultiIOModule {
  val decode = IO(new Decode2WriteBack)
  val execute = IO(Flipped(new WriteBack2Execute))
  val memory = IO(Flipped(new Memory2WriteBack))
  val hazard = IO(Flipped(new Writeback2Hazard))
  val int = IO(Input(UInt(6.W)))

  val pcp8 = Wire(UInt(32.W))
  val pc_now = pcp8 - 8.U
  val sel_reg_wdata = Wire(UInt(32.W))
  val mem_rdata = Wire(UInt(32.W))
  val alu_out = Wire(UInt(32.W))
  val hi_wen = Wire(Bool())
  val hi = Wire(chiselTypeOf(memory.hi))
  val lo_wen = Wire(Bool())
  val lo = Wire(chiselTypeOf(memory.lo))
  // 在writeback flush的信号，是memory中的，也就是触发异常的指令。它可能也要写CP0
  // 但发生异常时，对CP0的修改应当是通过except_type给出的，所以这个wen也要被flush掉
  val c0_wen = Wire(Bool())
  val c0_waddr = Wire(UInt(32.W))
  val c0_wdata = Wire(UInt(32.W))
  // 这些也要RegNext么？
  val except_type = Wire(UInt(32.W))
  val is_in_delayslot = Wire(Bool())

  // RegStallOrNext
  Seq(
    pcp8 -> memory.pcp8,
    sel_reg_wdata -> memory.sel_reg_wdata,
    mem_rdata -> memory.mem_rdata,
    alu_out -> memory.alu_out,
    hi_wen -> Mux(hazard.flush, 0.U, memory.hi.wen),
    hi -> memory.hi,
    lo_wen -> Mux(hazard.flush, 0.U, memory.lo.wen),
    lo -> memory.lo,
    c0_wen -> Mux(hazard.flush, 0.U, memory.c0.wen),
    c0_waddr -> memory.c0.waddr,
    c0_wdata -> memory.c0.wdata,
    except_type -> memory.except_type,
    is_in_delayslot -> memory.is_in_delayslot,
  ).foreach { case (reg, next) => reg := RegNext(Mux(hazard.stall, reg, next), 0.U.asTypeOf(reg)) }

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
    cp0.i.int := int
    import cp0.i._
    import cp0.o._
    wen := c0_wen
    waddr := c0_waddr
    wdata := c0_wdata
    raddr := execute.c0_raddr
    execute.c0_rdata := rdata
    BadVAddr := DontCare
    Count := DontCare
    Compare := DontCare
    memory.c0_status := Status
    memory.c0_cause := Cause
    memory.c0_epc := EPC
    timer_int := DontCare
    //    core.timer_int := timer_int
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

  val debug_wb = if(c.oTeachSoc) Some(IO(new DebugWb)) else None
  if (c.oTeachSoc) {
    debug_wb.get.pc := pc_now
    debug_wb.get.rf_wdata := decode.wdata
    debug_wb.get.rf_wnum := decode.waddr
    debug_wb.get.rf_wen := Fill(4, decode.wen)
    debug_wb.get.getElements.reverse.zipWithIndex.foreach { case (sink, n) => BoringUtils.addSource(sink, s"debugwb$n")}
  }
}