// See LICENSE for license details.

package cpu.execute

import chisel3._
import chisel3.stage.ChiselStage
import cpu.decode.CtrlSigDef.{SZ_BR_TYPE, SZ_MEM_TYPE, SZ_SEL_REG_WDATA}
import cpu.execute.ALU.SZ_ALU_FN
import cpu.util.{Config, DefCon}

class Execute(implicit c: Option[Config] = None) extends MultiIOModule {
  val de = IO(new Bundle() {
    val pc = Input(UInt(32.W))
    val alu_fn = Input(UInt(SZ_ALU_FN))
    val mul = Input(Bool())
    val div = Input(Bool())
    val mem_wen = Input(Bool())
    val mem_wdata = Input(UInt(32.W))
    val reg_wen = Input(Bool()) // link包含于其中
    val sel_reg_wdata = Input(UInt(SZ_SEL_REG_WDATA))
    val br_type = Input(UInt(SZ_BR_TYPE))
    val br_addr = Input(UInt(32.W))
    val num1 = Input(UInt(32.W))
    val num2 = Input(UInt(32.W))
    val reg_waddr = Input(UInt(5.W))
    val mem_size = Input(UInt(SZ_MEM_TYPE))
  })
  val em = IO(new Bundle() {
    val mem_wen = Output(Bool())
    val reg_wen = Output(Bool())
    val sel_reg_wdata = Output(UInt(SZ_SEL_REG_WDATA))
    val reg_waddr = Output(UInt(5.W))
    val alu_out = Output(UInt(32.W))
    val pc = Output(UInt(32.W))
    val mem_wdata = Output(UInt(32.W))
    val mem_size = Output(UInt(SZ_MEM_TYPE))
  })
  val ef = IO(new Bundle() {
    val branch = Output(Bool())
    val br_addr = Output(UInt(32.W))
  })

  val mul = RegNext(de.mul)
  val div = RegNext(de.div)
  val alu_fn = RegNext(de.alu_fn)
  val num1 = RegNext(de.num1)
  val num2 = RegNext(de.num2)
  val br_t = RegNext(de.br_type)

  em.pc := RegNext(de.pc)
  em.mem_wen := RegNext(de.mem_wen, 0.U)
  em.reg_wen := RegNext(de.reg_wen, 0.U)
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

  if (c.getOrElse(DefCon).debugExecute) {
    printf(p"[log execute] in1 = ${Binary(de.num1)}, in2 = ${Binary(de.num2)}, adder_out = ${Binary(adder_out)}\n")
  }

  val br_unit = Module(new BrUnit)
  locally {
    import br_unit.io._
    sub_res := adder_out
    br_type := br_t
    ef.branch := branch
  }
}

object Execute extends App {
  new ChiselStage emitVerilog new Execute
}