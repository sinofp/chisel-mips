// See LICENSE for license details.

package cpu.execute

import chisel3._
import chisel3.util.MuxCase
import cpu.decode.CtrlSigDef._
import cpu.execute.ALU.{FN_DIV, FN_MULT, SZ_ALU_FN}
import cpu.port.hazard.{EHPort, WdataPort}
import cpu.port.stage._
import cpu.util.{Config, DefCon}

class Execute(implicit c: Config = DefCon) extends MultiIOModule {
  val de = IO(Input(new DEPort))
  val em = IO(Output(new EMPort))
  val ef = IO(new EFPort)
  // forward
  val he = IO(Flipped(new EHPort))
  he.wen := em.reg_wen
  he.waddr := em.reg_waddr
  he.hi_wen := em.hi_wen
  he.lo_wen := em.lo_wen
  val ed = IO(Output(new WdataPort))
  ed.wdata := MuxCase(0.U, Array(
    (em.sel_reg_wdata === SEL_REG_WDATA_ALU) -> em.alu_out,
    (em.sel_reg_wdata === SEL_REG_WDATA_LNK) -> em.pcp8,
    (em.sel_reg_wdata === SEL_REG_WDATA_HI) -> em.hi,
    (em.sel_reg_wdata === SEL_REG_WDATA_LO) -> em.lo,
  ))
  val me = IO(Input(new MEPort))
  val we = IO(Input(new WEPort))

  val cu_mul = Wire(Bool())
  cu_mul := RegNext(Mux(he.stall, cu_mul, de.mul))
  val cu_div = Wire(Bool())
  cu_div := RegNext(Mux(he.stall, cu_div, de.div))
  val alu_fn = Wire(UInt(SZ_ALU_FN))
  alu_fn := RegNext(Mux(he.stall, alu_fn, de.alu_fn))
  val num1 = Wire(UInt(32.W))
  num1 := RegNext(Mux(he.stall, num1, de.num1))
  val num2 = Wire(UInt(32.W))
  num2 := RegNext(Mux(he.stall, num2, de.num2))
  val br_t = Wire(UInt(SZ_BR_TYPE))
  br_t := RegNext(Mux(he.stall, br_t, Mux(he.flush, BR_TYPE_NO, de.br_type)))

  em.pcp8 := RegNext(Mux(he.stall, em.pcp8, de.pcp8))
  em.mem_wen := RegNext(Mux(he.stall, em.mem_wen, Mux(he.flush, 0.U, de.mem_wen)), 0.U)
  em.reg_wen := RegNext(Mux(he.stall, em.reg_wen, Mux(he.flush, 0.U, de.reg_wen)), 0.U)
  em.sel_reg_wdata := RegNext(Mux(he.stall, em.sel_reg_wdata, de.sel_reg_wdata), 0.U)
  em.reg_waddr := RegNext(Mux(he.stall, em.reg_waddr, de.reg_waddr), 0.U)
  ef.br_addr := RegNext(Mux(he.stall, ef.br_addr, de.br_addr))
  em.mem_wdata := RegNext(Mux(he.stall, em.mem_wdata, de.mem_wdata))
  em.mem_size := RegNext(Mux(he.stall, em.mem_size, de.mem_size))
  em.hi_wen := RegNext(Mux(he.stall, em.hi_wen, de.hi_wen), 0.U)
  em.lo_wen := RegNext(Mux(he.stall, em.lo_wen, de.lo_wen), 0.U)

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
    start := cu_div
    sign := alu_fn === FN_DIV
    he.div_not_ready := cu_div && !ready
  }

  val mul = Module(new Mul)
  locally {
    import mul.io._
    multiplicand := num1
    multiplier := num2
    sign := alu_fn === FN_MULT
  }

  em.hi := MuxCase(num1, Array( // 默认num1是mthi, rs读出来的值 -- 要加上em.hi_wen做条件限定么？
    cu_div -> div.io.quotient,
    cu_mul -> mul.io.product(63, 32),
    (he.forward_hi === FORWARD_HILO_MEM) -> me.hi, // 前推时都是em.hi_wen=0的时候，所以改了向后传的hi也无所谓
    (he.forward_hi === FORWARD_HILO_WB) -> we.hi,
  ))
  em.lo := MuxCase(num1, Array(
    cu_div -> div.io.quotient,
    cu_mul -> mul.io.product(31, 0),
    (he.forward_lo === FORWARD_HILO_MEM) -> me.lo,
    (he.forward_lo === FORWARD_HILO_WB) -> we.lo,
  ))

  if (c.dExecute) {
    printf(p"[log execute]\n\tin1 = ${Hexadecimal(de.num1)}, in2 = ${Hexadecimal(de.num2)}, adder_out = ${Hexadecimal(adder_out)}\n")
  }
  if (c.dBrUnit) {
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