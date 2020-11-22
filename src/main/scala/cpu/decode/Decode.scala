// See LICENSE for license details.

package cpu.decode

import chisel3._
import chisel3.util._
import cpu.decode.CtrlSigDef._
import cpu.port.hazard.{DHPort, WdataPort}
import cpu.port.stage.{Decode2Execute, Decode2Fetch, Fetch2Decode, WriteBack2Decode}
import cpu.util.{Config, DefCon}

class Decode(implicit c: Config = DefCon) extends MultiIOModule {
  val fd = IO(Input(new Fetch2Decode))
  val de = IO(Output(new Decode2Execute))
  val df = IO(new Decode2Fetch)
  val wd = IO(Input(new WriteBack2Decode))
  // forward
  val ed = IO(Input(new WdataPort))
  val md = IO(Input(new WdataPort))
  val readPorts = 2
  val hd = IO(Flipped(new DHPort(readPorts)))

  val inst = Wire(UInt(32.W))
  inst := RegNext(MuxCase(fd.inst, Array(hd.stall -> inst, hd.flush -> 0.U)), 0.U)
  val pcp4 = Wire(UInt(32.W))
  pcp4 := RegNext(Mux(hd.stall, pcp4, fd.pcp4), 0.U)

  val cu = Module(new CU)
  cu.inst := inst
  locally {
    import cu.ctrl._
    de.alu_fn := alu_fn
    de.alu_n := alu_n
    de.mul := mul
    de.div := div
    de.mem_wen := mem_wen
    de.reg_wen := reg_wen
    de.sel_reg_wdata := sel_reg_wdata
    de.br_type := br_type
    de.mem_size := mem_size
    de.hi_wen := hi_wen
    de.lo_wen := lo_wen
    de.c0_wen := c0_wen
    de.sel_move := sel_move
  }
  val sel_alu1 = cu.ctrl.sel_alu1
  val sel_alu2 = cu.ctrl.sel_alu2
  val sel_imm = cu.ctrl.sel_imm
  val sel_reg_waddr = cu.ctrl.sel_reg_waddr

  val rs = inst(25, 21)
  val rt = inst(20, 16)
  val rd = inst(15, 11)
  val imm = inst(15, 0)
  de.c0_addr := rd

  val reg_file = Module(new RegFile(readPorts))
  locally {
    import reg_file.in._
    import reg_file.io._
    wen := wd.wen
    waddr := wd.waddr
    wdata := wd.wdata
    raddr(0) := rs
    raddr(1) := rt
  }

  // forward
  hd.raddr(0) := rs
  hd.raddr(1) := rt
  hd.prev_load := RegNext(Mux(hd.stall, false.B, cu.ctrl.load), false.B)
  val forward_reg = (i: Int) => MuxLookup(hd.forward(i), reg_file.io.rdata(i), Array(
    FORWARD_EXE -> ed.wdata,
    FORWARD_MEM -> md.wdata,
    FORWARD_WB -> wd.wdata,
  ))
  val rdata1 = forward_reg(0)
  val rdata2 = forward_reg(1)

  de.mem_wdata := rdata2
  val imm_ext = MuxLookup(sel_imm, 0.U, Array(
    SEL_IMM_U -> Cat(0.U(16.W), imm),
    SEL_IMM_S -> Cat(Fill(16, imm(15)), imm),
    SEL_IMM_B -> Cat(Fill(14, imm(15)), imm, 0.U(2.W)),
    SEL_IMM_J -> Cat(pcp4(31, 28), inst(25, 0), 0.U(2.W)),
    SEL_IMM_SH -> Cat(0.U(27.W), inst(10, 6)),
    SEL_IMM_LUI -> imm ## 0.U(16.W),
  ))

  // J型指令中，如果alu1是SA，那就是JR，反之是J —— 多加CtrlSig？
  df.j_addr := Mux(sel_alu1 === SEL_ALU1_SA, rdata1, imm_ext)
  df.jump := sel_imm === SEL_IMM_J

  de.br_addr := pcp4 + imm_ext // 这个是不是应该放在imm_ext里？
  de.pcp8 := pcp4 + 4.U // for link
  de.num1 := Mux(sel_alu1 === SEL_ALU1_SA, imm_ext, rdata1)
  de.num2 := MuxLookup(sel_alu2, 0.U, Array(
    SEL_ALU2_IMM -> imm_ext,
    SEL_ALU2_RT -> rdata2
  ))

  de.reg_waddr := MuxLookup(sel_reg_waddr, 0.U(32.W), Array(
    SEL_REG_WADDR_RD -> rd,
    SEL_REG_WADDR_RT -> rt,
    SEL_REG_WADDR_31 -> 31.U,
  ))
}