// See LICENSE for license details.

package cpu.decode

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._
import cpu.decode.CtrlSigDef._
import cpu.execute.ALU.SZ_ALU_FN

class Decode extends Module {
  val io = IO(new Bundle() {
    val fd_inst = Input(UInt(32.W))
    val pcp4 = Input(UInt(32.W))
    val wd_wen = Input(Bool())
    val wd_waddr = Input(UInt(5.W))
    val wd_wdata = Input(UInt(32.W))
    val de_alu_fn = Output(UInt(SZ_ALU_FN))
    val de_mul = Output(Bool())
    val de_div = Output(Bool())
    val de_mem_wen = Output(Bool())
    val de_reg_wen = Output(Bool()) // link包含于其中
    val de_sel_reg_wdata = Output(UInt(SZ_SEL_REG_WDATA))
    val de_br_type = Output(UInt(SZ_BR_TYPE))
    val de_num1 = Output(UInt(32.W))
    val de_num2 = Output(UInt(32.W))
    val de_reg_waddr = Output(UInt(5.W))
  })

  import io._

  val inst = RegNext(fd_inst, 0.U(32.W))

  val cu = Module(new CU)
  cu.inst := inst
  locally {
    import cu.ctrl._
    de_alu_fn := alu_fn
    de_mul := mul
    de_div := div
    de_mem_wen := mem_wen
    de_reg_wen := reg_wen
    de_sel_reg_wdata := sel_reg_wdata
    de_br_type := br_type
  }
  val sel_alu1 = cu.ctrl.sel_alu1
  val sel_alu2 = cu.ctrl.sel_alu2
  val sel_imm = cu.ctrl.sel_imm
  val sel_reg_waddr = cu.ctrl.sel_reg_waddr

  val rs = inst(25, 21)
  val rt = inst(20, 16)
  val rd = inst(15, 11)
  val imm = inst(15, 0)

  val reg_file = Module(new RegFile(2))
  locally {
    import reg_file.io._
    wen := wd_wen
    waddr := wd_waddr
    wdata := wd_wdata
    raddr(0) := rs
    raddr(1) := rt
  }
  val rdata1 = reg_file.io.rdata(0)
  val rdata2 = reg_file.io.rdata(1)

  val imm_ext = MuxCase(0.U, Array(
    (sel_imm === SEL_IMM_J) -> Cat(pcp4(31, 28), inst(25, 0), 0.U(2.W)),
    (sel_imm === SEL_IMM_LUI) -> Cat(imm, 0.U(16.W)),
    (sel_imm === SEL_IMM_S) -> Cat(Fill(16, imm(15)), imm),
    (sel_imm === SEL_IMM_U) -> Cat(0.U(16.W), imm),
    (sel_imm === SEL_IMM_SH) -> Cat(0.U(27.W), inst(10, 6)),
  ))

  de_num1 := Mux(sel_alu1 === SEL_ALU1_SA, imm_ext, rdata1)
  de_num2 := MuxCase(rdata2, Array(
    (sel_alu2 === SEL_ALU2_IMM) -> imm_ext, (sel_alu2 === SEL_ALU2_ZERO) -> 0.U)
  )

  de_reg_waddr := MuxCase(0.U(32.W), Array(
    (sel_reg_waddr === SEL_REG_WADDR_31) -> 31.U,
    (sel_reg_waddr === SEL_REG_WADDR_RD) -> rd,
    (sel_reg_waddr === SEL_REG_WADDR_RT) -> rt,
  ))
}

object Decode extends App {
  (new ChiselStage).emitVerilog(new Decode)
}