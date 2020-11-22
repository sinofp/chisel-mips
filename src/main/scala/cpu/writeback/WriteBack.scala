// See LICENSE for license details.

package cpu.writeback

import chisel3._
import chisel3.util.MuxLookup
import cpu.decode.CtrlSigDef._
import cpu.port.hazard.WHPort
import cpu.port.stage.{Memory2WriteBack, WriteBack2Decode, WriteBack2Execute}
import cpu.util.{Config, DefCon}

class WriteBack(implicit c: Config = DefCon) extends MultiIOModule {
  val mw = IO(Input(new Memory2WriteBack))
  val wd = IO(Output(new WriteBack2Decode))
  val hw = IO(Flipped(new WHPort))

  val pcp8 = RegNext(mw.pcp8)
  val pc = pcp8 - 8.U
  val sel_reg_wdata = RegNext(mw.sel_reg_wdata)
  val mem_rdata = RegNext(mw.mem_rdata)
  val alu_out = RegNext(mw.alu_out)
  val hi_wen = RegNext(mw.hi_wen)
  val hi = RegNext(mw.hi)
  val lo_wen = RegNext(mw.lo_wen)
  val lo = RegNext(mw.lo)
  val c0_wen = RegNext(mw.c0_wen)
  val c0_waddr = RegNext(mw.c0_waddr)
  val c0_wdata = RegNext(mw.c0_wdata)

  val hilo = Module(new HILO)
  locally {
    import hilo.{io => hl}
    hl.hi_wen := hi_wen
    hl.lo_wen := lo_wen
    hl._hi := hi
    hl._lo := lo
  }

  wd.wen := RegNext(mw.reg_wen, false.B)
  wd.waddr := RegNext(mw.reg_waddr)
  wd.wdata := MuxLookup(sel_reg_wdata, 0.U, Array(
    SEL_REG_WDATA_EX -> alu_out,
    SEL_REG_WDATA_LNK -> pcp8,
    SEL_REG_WDATA_MEM -> mem_rdata,
  ))
  hw.wen := wd.wen
  hw.waddr := wd.waddr
  hw.hi_wen := hi_wen
  hw.lo_wen := lo_wen
  hw.c0_wen := c0_wen
  hw.c0_waddr := c0_waddr
  val we = IO(Flipped(new WriteBack2Execute))
  we.hi := hilo.io.hi
  we.lo := hilo.io.lo
  we.c0_data := c0_wdata

  val cp0 = Module(new CP0)
  locally {
    import cp0.i._
    import cp0.o._
    wen := c0_wen
    waddr := c0_waddr
    wdata := c0_wdata
    raddr := we.c0_raddr
    we.c0_rdata := rdata
    int := DontCare
    BadVAddr := DontCare
    Count := DontCare
    Compare := DontCare
    Status := DontCare
    Cause := DontCare
    EPC := DontCare
    timer_int := DontCare
  }
}