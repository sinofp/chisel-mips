// See LICENSE for license details.

package cpu.decode

import chisel3._
import chisel3.util._
import cpu.decode.CtrlSigDef._
import cpu.port.{DEPort, FDPort, WritePort}
import cpu.util.{Config, DefCon}

class Decode(implicit c: Config = DefCon) extends MultiIOModule {
  val fd = IO(Input(new FDPort))
  // forward
  val wd = IO(Input(new WritePort))
  val ed = IO(Input(new WritePort))
  val md = IO(Input(new WritePort))
  val de = IO(Output(new DEPort))

  val inst = RegNext(fd.inst, 0.U(32.W))
  val pcp4 = RegNext(fd.pcp4, 0.U)

  val cu = Module(new CU)
  cu.inst := inst
  locally {
    import cu.ctrl._
    de.alu_fn := alu_fn
    de.mul := mul
    de.div := div
    de.mem_wen := mem_wen
    de.reg_wen := reg_wen
    de.sel_reg_wdata := sel_reg_wdata
    de.br_type := br_type
    de.mem_size := mem_size
  }
  val sel_alu1 = cu.ctrl.sel_alu1
  val sel_alu2 = cu.ctrl.sel_alu2
  val sel_imm = cu.ctrl.sel_imm
  val sel_reg_waddr = cu.ctrl.sel_reg_waddr

  val rs = inst(25, 21)
  val rt = inst(20, 16)
  val rd = inst(15, 11)
  val imm = inst(15, 0)

  val readPorts = 2
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
  val hazard_unit = Module(new HazardUnit(readPorts))
  hazard_unit.raddr(0) := rs
  hazard_unit.raddr(1) := rt
  hazard_unit.ed <> ed
  hazard_unit.md <> md
  hazard_unit.wd <> wd
  val forward = hazard_unit.forward

  val forward_reg = (i: Int) => {
    val is = (forward_type: UInt) => forward(i) === forward_type
    MuxCase(reg_file.io.rdata(i), Array(
      is(FORWARD_EXE) -> ed.wdata,
      is(FORWARD_MEM) -> md.wdata,
      is(FORWARD_WB) -> wd.wdata,
    ))
  }
  val rdata1 = forward_reg(0)
  val rdata2 = forward_reg(1)

  de.mem_wdata := rdata2
  val imm_ext = {
    val imm_is = sel => sel_imm === sel
    MuxCase(0.U, Array(
      imm_is(SEL_IMM_U) -> Cat(0.U(16.W), imm),
      imm_is(SEL_IMM_S) -> Cat(Fill(16, imm(15)), imm),
      imm_is(SEL_IMM_B) -> Cat(Fill(14, imm(15)), imm, 0.U(2.W)),
      imm_is(SEL_IMM_J) -> Cat(pcp4(31, 28), inst(25, 0), 0.U(2.W)),
      imm_is(SEL_IMM_SH) -> Cat(0.U(27.W), inst(10, 6)),
    ))
  }

  de.br_addr := pcp4 + imm_ext // 这个是不是应该放在imm_ext里？
  de.pcp8 := pcp4 + 4.U // for link
  locally {
    val alu_is = (no: Int, sel: UInt) => sel === (if (no == 1) sel_alu1 else sel_alu2)
    de.num1 := Mux(alu_is(1, SEL_ALU1_SA), imm_ext, rdata1)
    de.num2 := MuxCase(rdata2, Array(
      alu_is(2, SEL_ALU2_IMM) -> imm_ext,
      alu_is(2, SEL_ALU2_ZERO) -> 0.U)
    )
  }

  de.reg_waddr := {
    val reg_waddr_is = sel => sel_reg_waddr === sel
    MuxCase(0.U(32.W), Array(
      reg_waddr_is(SEL_REG_WADDR_31) -> 31.U,
      reg_waddr_is(SEL_REG_WADDR_RD) -> rd,
      reg_waddr_is(SEL_REG_WADDR_RT) -> rt,
    ))
  }
}