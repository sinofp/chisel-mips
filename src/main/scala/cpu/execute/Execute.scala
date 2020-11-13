// See LICENSE for license details.

package cpu.execute

import chisel3._
import chisel3.util.MuxCase
import cpu.decode.CtrlSigDef._
import cpu.execute.ALU.{FN_DIV, FN_DIVU}
import cpu.port.hazard.{EHPort, WdataPort}
import cpu.port.stage.{DEPort, EMPort}
import cpu.util.{Config, DefCon}

class Execute(implicit c: Config = DefCon) extends MultiIOModule {
  val de = IO(Input(new DEPort))
  val em = IO(Output(new EMPort))
  val ef = IO(new Bundle() {
    val branch = Output(Bool())
    val br_addr = Output(UInt(32.W))
  })
  // forward
  val he = IO(Flipped(new EHPort))
  he.wen := em.reg_wen
  he.waddr := em.reg_waddr
  val ed = IO(Output(new WdataPort))
  ed.wdata := MuxCase(0.U, Array(
    (em.sel_reg_wdata === SEL_REG_WDATA_ALU) -> em.alu_out,
    (em.sel_reg_wdata === SEL_REG_WDATA_LNK) -> em.pcp8
  ))

  val cu_mul = RegNext(de.mul)
  val cu_div = RegNext(de.div)
  val alu_fn = RegNext(de.alu_fn)
  val num1 = RegNext(de.num1)
  val num2 = RegNext(de.num2)
  val br_t = RegNext(Mux(he.flush, BR_TYPE_NO, de.br_type))

  em.pcp8 := RegNext(de.pcp8)
  em.mem_wen := RegNext(Mux(he.flush, 0.U, de.mem_wen), 0.U)
  em.reg_wen := RegNext(Mux(he.flush, 0.U, de.reg_wen), 0.U)
  em.sel_reg_wdata := RegNext(de.sel_reg_wdata, 0.U)
  em.reg_waddr := RegNext(de.reg_waddr, 0.U)
  ef.br_addr := RegNext(de.br_addr)
  em.mem_wdata := RegNext(de.mem_wdata)
  em.mem_size := RegNext(de.mem_size)

  val alu = Module(new ALU)
  locally {
    import alu.io._
    fn := alu_fn
    in1 := num1
    in2 := num2
    em.alu_out := out
    cmp_out := DontCare
  }
  val adder_out = Wire(UInt(32.W))
  adder_out := alu.io.adder_out

  val div = Module(new Div)
  locally {
    import div.io._
    dividend := num1
    divider := num2
    start := (alu_fn === FN_DIV) || (alu_fn === FN_DIVU)
    sign := alu_fn === FN_DIV
    ready := DontCare
    quotient := DontCare
    remainder := DontCare
  }

  if (c.debugExecute) {
    printf(p"[log execute]\n\tin1 = ${Hexadecimal(de.num1)}, in2 = ${Hexadecimal(de.num2)}, adder_out = ${Hexadecimal(adder_out)}\n")
  }
  if (c.debugBrUnit) {
    printf(p"[log execute]\n\tbranch = ${ef.branch}, br_addr >> 2 = ${ef.br_addr / 4.U}\n")
  }

  val br_unit = Module(new BrUnit)
  br_unit.io.num1 := num1
  br_unit.io.num2 := num2
  locally {
    import br_unit.io._
    slt_res := alu.io.out
    br_type := br_t
    ef.branch := branch
  }
  he.branch := ef.branch
}