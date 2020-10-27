// See LICENSE for license details.

package cpu

import chisel3._
import chisel3.stage.ChiselStage
import cpu.ALU.SZ_ALU_FN

class Decode extends Module {
  val io = IO(new Bundle() {
    val _inst = Input(UInt(32.W))
    val pcp4 = Input(UInt(32.W))
    val wb_wen = Input(Bool())
    val wb_waddr = Input(UInt(5.W))
    val wb_wdata = Input(UInt(32.W))
    val inst = Output(UInt(32.W))
    val rdata1 = Output(UInt(32.W))
    val rdata2 = Output(UInt(32.W))
    val imm_ext = Output(UInt(32.W))
    val alu_op = Output(UInt(SZ_ALU_FN))
    val alu_src = Output(UInt(2.W))
    val pcp8 = Output(UInt(32.W))
    val reg_wen = Output(Bool())
    val mem_wen = Output(Bool())
    val reg_waddr = Output(UInt(5.W))
    val reg_wdata_src = Output(UInt(3.W))
  })

  import io._

  imm_ext := DontCare
  alu_op := DontCare
  alu_src := DontCare
  reg_wen := DontCare
  mem_wen := DontCare
  reg_waddr := DontCare
  reg_wdata_src := DontCare

  val rs = inst(25, 21)
  val rt = inst(20, 16)
  val rd = inst(15, 11)

  inst := RegNext(_inst, 0.U(32.W))
  pcp8 := pcp4 + 4.U

  val reg_file = Module(new RegFile(2))
  locally {
    import reg_file.io._
    wen := wb_wen
    waddr := wb_waddr
    wdata := wb_wdata
    raddr(0) := rs
    raddr(1) := rt
  }
  rdata1 := reg_file.io.rdata(0)
  rdata2 := reg_file.io.rdata(1)
}

object Decode extends App {
  (new ChiselStage).emitVerilog(new Decode)
}