// See LICENSE for license details.

package cpu.execute

import chisel3._
import chisel3.stage.ChiselStage
import cpu.decode.CtrlSigDef.{SZ_BR_TYPE, SZ_MEM_TYPE, SZ_SEL_REG_WDATA}
import cpu.execute.ALU.SZ_ALU_FN

class Execute extends Module {
  val io = IO(new Bundle() {
    val de_pc = Input(UInt(32.W))
    val de_alu_fn = Input(UInt(SZ_ALU_FN))
    val de_mul = Input(Bool())
    val de_div = Input(Bool())
    val de_mem_wen = Input(Bool())
    val de_mem_wdata = Input(UInt(32.W))
    val de_reg_wen = Input(Bool()) // link包含于其中
    val de_sel_reg_wdata = Input(UInt(SZ_SEL_REG_WDATA))
    val de_br_type = Input(UInt(SZ_BR_TYPE))
    val de_br_addr = Input(UInt(32.W))
    val de_num1 = Input(UInt(32.W))
    val de_num2 = Input(UInt(32.W))
    val de_reg_waddr = Input(UInt(5.W))
    val de_mem_size = Input(UInt(SZ_MEM_TYPE))
    val em_mem_wen = Output(Bool())
    val em_reg_wen = Output(Bool())
    val em_sel_reg_wdata = Output(UInt(SZ_SEL_REG_WDATA))
    val em_reg_waddr = Output(UInt(5.W))
    val em_alu_out = Output(UInt(32.W))
//    val adder_out = Output(UInt(32.W))
//    val cmp_out = Output(UInt(32.W))
    val ef_branch = Output(Bool())
    val ef_br_addr = Output(UInt(32.W))
    val em_pc = Output(UInt(32.W))
    val em_mem_wdata = Output(UInt(32.W))
    val em_mem_size = Output(UInt(SZ_MEM_TYPE))
  })

  import io._

  val mul = RegNext(de_mul)
  val div = RegNext(de_div)
  val alu_fn = RegNext(de_alu_fn)
  val num1 = RegNext(de_num1)
  val num2 = RegNext(de_num2)
  val br_t = RegNext(de_br_type)

  em_pc := RegNext(de_pc)
  em_mem_wen := RegNext(de_mem_wen, 0.U)
  em_reg_wen := RegNext(de_reg_wen, 0.U)
  em_sel_reg_wdata := RegNext(de_sel_reg_wdata, 0.U)
  em_reg_waddr := RegNext(de_reg_waddr, 0.U)
  ef_br_addr := RegNext(de_br_addr)
  em_mem_wdata := RegNext(de_mem_wdata)
  em_mem_size := RegNext(de_mem_size)

  val alu = Module(new ALU)
  locally {
    import alu.io._
    fn := alu_fn
    in1 := num1
    in2 := num2
    em_alu_out := out
    cmp_out := DontCare
  }
  val adder_out = Wire(UInt(32.W))
  adder_out := alu.io.adder_out

  printf(p"[log execute] in1 = ${Binary(de_num1)}, in2 = ${Binary(de_num2)}, adder_out = ${Binary(adder_out)}\n")

  val br_unit = Module(new BrUnit)
  locally {
    import br_unit.io._
    sub_res := adder_out
    br_type := br_t
    ef_branch := branch
  }
}

object Execute extends App {
  new ChiselStage emitVerilog new Execute
}