// See LICENSE for license details.

package cpu.decode

import chisel3._
import chisel3.util._
import cpu.decode.CtrlSigDef._
import cpu.port.hazard.Decode2Hazard
import cpu.port.stage.{Decode2Execute, Decode2Fetch, Decode2WriteBack, Memory2Decode}
import cpu.util.{Config, DefCon}

class Decode(implicit c: Config = DefCon) extends MultiIOModule {
  val fetch = IO(new Decode2Fetch)
  val execute = IO(new Decode2Execute)
  val memory = IO(Flipped(new Memory2Decode))
  val writeBack = IO(Flipped(new Decode2WriteBack))
  val hazard = IO(Flipped(new Decode2Hazard(2)))

  val inst = Wire(UInt(32.W))
  inst := RegNext(MuxCase(fetch.inst, Array(hazard.stall -> inst, hazard.flush -> 0.U)), 0.U)
  val pcp4 = Wire(UInt(32.W))
  pcp4 := RegNext(Mux(hazard.stall, pcp4, fetch.pcp4), 0.U)

  // cu
  val cu = Module(new CU)
  cu.inst := inst
  locally {
    import cu.ctrl._
    execute.alu_fn := alu_fn
    execute.alu_n := alu_n
    execute.mul := mul
    execute.div := div
    execute.mem.wen := mem_wen
    execute.rf.wen := reg_wen
    execute.sel_reg_wdata := sel_reg_wdata
    execute.br_type := br_type
    execute.mem.size := mem_size
    execute.hi.wen := hi_wen
    execute.lo.wen := lo_wen
    execute.c0.wen := c0_wen
    execute.sel_move := sel_move
  }
  val sel_alu1 = cu.ctrl.sel_alu1
  val sel_alu2 = cu.ctrl.sel_alu2
  val sel_imm = cu.ctrl.sel_imm
  val sel_reg_waddr = cu.ctrl.sel_reg_waddr
  val inst_invalid = cu.ctrl.inst_invalid
  val is_eret = cu.ctrl.is_eret
  val is_syscall = cu.ctrl.is_syscall
  val is_break = cu.ctrl.is_break

  // inst part
  val rs = inst(25, 21)
  val rt = inst(20, 16)
  val rd = inst(15, 11)
  val imm = inst(15, 0)

  // rf
  val reg_file = Module(new RegFile(2))
  locally {
    import reg_file.in._
    import reg_file.io._
    wen := writeBack.wen
    waddr := writeBack.waddr
    wdata := writeBack.wdata
    raddr(0) := rs
    raddr(1) := rt
  }

  // forward
  hazard.raddr(0) := rs
  hazard.raddr(1) := rt
  hazard.prev_load := RegNext(Mux(hazard.stall, false.B, cu.ctrl.load), false.B)
  val forward_reg = (i: Int) => MuxLookup(hazard.forward(i), reg_file.io.rdata(i), Array(
    FORWARD_EXE -> execute.wdata,
    FORWARD_MEM -> memory.wdata,
    FORWARD_WB -> writeBack.wdata,
  ))
  val rdata1 = forward_reg(0)
  val rdata2 = forward_reg(1)

  val imm_ext = MuxLookup(sel_imm, 0.U, Array(
    SEL_IMM_U -> Cat(0.U(16.W), imm),
    SEL_IMM_S -> Cat(Fill(16, imm(15)), imm),
    SEL_IMM_B -> Cat(Fill(14, imm(15)), imm, 0.U(2.W)),
    SEL_IMM_J -> Cat(pcp4(31, 28), inst(25, 0), 0.U(2.W)),
    SEL_IMM_SH -> Cat(0.U(27.W), inst(10, 6)),
    SEL_IMM_LUI -> imm ## 0.U(16.W),
  ))

  // misc output
  // J型指令中，如果alu1是SA，那就是JR，反之是J —— 多加CtrlSig？
  fetch.j_addr := Mux(sel_alu1 === SEL_ALU1_SA, rdata1, imm_ext)
  fetch.jump := sel_imm === SEL_IMM_J
  execute.c0.waddr := rd
  execute.mem.wdata := rdata2
  execute.br_addr := pcp4 + imm_ext // 这个是不是应该放在imm_ext里？
  execute.pcp8 := pcp4 + 4.U // for link
  execute.num1 := Mux(sel_alu1 === SEL_ALU1_SA, imm_ext, rdata1)
  execute.num2 := MuxLookup(sel_alu2, 0.U, Array(
    SEL_ALU2_IMM -> imm_ext,
    SEL_ALU2_RT -> rdata2
  ))
  execute.rf.waddr := MuxLookup(sel_reg_waddr, 0.U(32.W), Array(
    SEL_REG_WADDR_RD -> rd,
    SEL_REG_WADDR_RT -> rt,
    SEL_REG_WADDR_31 -> 31.U,
  ))
  execute.except_type := Cat(0.U(18.W), is_break, is_eret, 0.U(2.W), inst_invalid, is_syscall, 0.U(8.W))
  execute.is_in_delayslot := RegNext(sel_imm === SEL_IMM_J | execute.br_type =/= BR_TYPE_NO)
}