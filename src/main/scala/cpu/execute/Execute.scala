// See LICENSE for license details.

package cpu.execute

import chisel3._
import chisel3.util.{MuxCase, MuxLookup}
import cpu.decode.CtrlSigDef._
import cpu.execute.ALU.{FN_DIV, FN_MULT, SZ_ALU_FN}
import cpu.port.hazard.Execute2Hazard
import cpu.port.stage._
import cpu.util.{Config, DefCon}

class Execute(implicit c: Config = DefCon) extends MultiIOModule {
  val decode = IO(Flipped(new Decode2Execute))
  val memory = IO(Flipped(new Execute2Memory))
  val fetch = IO(new Execute2Fetch)
  // forward
  val hazard = IO(Flipped(new Execute2Hazard))
  hazard.wen := memory.reg_wen
  hazard.waddr := memory.reg_waddr
  hazard.hi_wen := memory.hi_wen
  hazard.lo_wen := memory.lo_wen
  decode.wdata := MuxLookup(memory.sel_reg_wdata, 0.U, Array(
    SEL_REG_WDATA_EX -> memory.alu_out,
    SEL_REG_WDATA_LNK -> memory.pcp8,
  ))
  val writeback = IO(new WriteBack2Execute)

  val cu_mul = Wire(Bool())
  cu_mul := RegNext(Mux(hazard.stall, cu_mul, decode.mul))
  val cu_div = Wire(Bool())
  cu_div := RegNext(Mux(hazard.stall, cu_div, decode.div))
  val alu_fn = Wire(UInt(SZ_ALU_FN))
  alu_fn := RegNext(Mux(hazard.stall, alu_fn, decode.alu_fn))
  val alu_n = Wire(Bool())
  alu_n := RegNext(Mux(hazard.stall, alu_n, decode.alu_n))
  val num1 = Wire(UInt(32.W))
  num1 := RegNext(Mux(hazard.stall, num1, decode.num1))
  val num2 = Wire(UInt(32.W))
  num2 := RegNext(Mux(hazard.stall, num2, decode.num2))
  val br_t = Wire(UInt(SZ_BR_TYPE))
  br_t := RegNext(Mux(hazard.stall, br_t, Mux(hazard.flush, BR_TYPE_NO, decode.br_type)))
  val sel_move = Wire(UInt(SZ_SEL_MOVE))
  sel_move := RegNext(Mux(hazard.stall, sel_move, decode.sel_move))

  memory.pcp8 := RegNext(Mux(hazard.stall, memory.pcp8, decode.pcp8))
  memory.mem_wen := RegNext(Mux(hazard.stall, memory.mem_wen, Mux(hazard.flush, 0.U, decode.mem_wen)), 0.U)
  memory.reg_wen := RegNext(Mux(hazard.stall, memory.reg_wen, Mux(hazard.flush, 0.U, decode.reg_wen)), 0.U)
  memory.sel_reg_wdata := RegNext(Mux(hazard.stall, memory.sel_reg_wdata, decode.sel_reg_wdata), 0.U)
  memory.reg_waddr := RegNext(Mux(hazard.stall, memory.reg_waddr, decode.reg_waddr), 0.U)
  fetch.br_addr := RegNext(Mux(hazard.stall, fetch.br_addr, decode.br_addr))
  memory.mem_wdata := RegNext(Mux(hazard.stall, memory.mem_wdata, decode.mem_wdata))
  memory.mem_size := RegNext(Mux(hazard.stall, memory.mem_size, decode.mem_size))
  memory.hi_wen := RegNext(Mux(hazard.stall, memory.hi_wen, decode.hi_wen), 0.U)
  memory.lo_wen := RegNext(Mux(hazard.stall, memory.lo_wen, decode.lo_wen), 0.U)
  memory.c0_wen := RegNext(Mux(hazard.stall, memory.c0_wen, decode.c0_wen), 0.U)
  memory.c0_wdata := num2 // $rt
  memory.c0_waddr := RegNext(Mux(hazard.stall, memory.c0_waddr, decode.c0_addr))

  writeback.c0_raddr := memory.c0_waddr // 都是rd
  hazard.c0_raddr := writeback.c0_raddr
  val c0 = MuxCase(writeback.c0_rdata, Array(
    (hazard.forward_c0 === FORWARD_C0_MEM) -> memory.c0_data,
    (hazard.forward_c0 === FORWARD_HILO_WB) -> writeback.c0_data,
  ))

  val alu = Module(new ALU)
  locally {
    import alu.io._
    fn := alu_fn
    in1 := num1
    in2 := num2
    memory.alu_out := MuxCase(out, Array(
      alu_n -> ~out,
      (sel_move === SEL_MOVE_HI) -> memory.hi,
      (sel_move === SEL_MOVE_LO) -> memory.lo,
      (sel_move === SEL_MOVE_C0) -> c0,
    ))
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
    hazard.div_not_ready := cu_div && !ready
  }

  val mul = Module(new Mul)
  locally {
    import mul.io._
    multiplicand := num1
    multiplier := num2
    sign := alu_fn === FN_MULT
  }

  memory.hi := MuxCase(num1, Array( // 默认num1是mthi, rs读出来的值 -- 要加上em.hi_wen做条件限定么？
    cu_div -> div.io.quotient,
    cu_mul -> mul.io.product(63, 32),
    (hazard.forward_hi === FORWARD_HILO_MEM) -> memory.hi_forward, // 前推时都是em.hi_wen=0的时候，所以改了向后传的hi也无所谓
    (hazard.forward_hi === FORWARD_HILO_WB) -> writeback.hi,
  ))
  memory.lo := MuxCase(num1, Array(
    cu_div -> div.io.quotient,
    cu_mul -> mul.io.product(31, 0),
    (hazard.forward_lo === FORWARD_HILO_MEM) -> memory.lo_forward,
    (hazard.forward_lo === FORWARD_HILO_WB) -> writeback.lo,
  ))

  if (c.dExecute) {
    printf(p"[log execute]\n\tin1 = ${Hexadecimal(decode.num1)}, in2 = ${Hexadecimal(decode.num2)}, adder_out = ${Hexadecimal(adder_out)}\n")
  }
  if (c.dBrUnit) {
    printf(p"[log execute]\n\tbranch = ${fetch.branch}, br_addr >> 2 = ${fetch.br_addr / 4.U}\n")
  }

  val br_unit = Module(new BrUnit)
  br_unit.io.num1 := num1
  br_unit.io.num2 := num2
  locally {
    import br_unit.io._
    slt_res := alu.io.out
    br_type := br_t
    fetch.branch := branch
  }
  hazard.branch := fetch.branch
}