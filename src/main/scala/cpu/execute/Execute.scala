// See LICENSE for license details.

package cpu.execute

import chisel3._
import chisel3.util._
import cpu.decode.CtrlSigDef._
import cpu.execute.ALU.{FN_DIV, FN_MULT, SZ_ALU_FN}
import cpu.port.hazard.Execute2Hazard
import cpu.port.stage._
import cpu.util.{Config, DefCon}

class Execute(implicit c: Config = DefCon) extends MultiIOModule {
  val fetch = IO(new Execute2Fetch)
  val decode = IO(Flipped(new Decode2Execute))
  val memory = IO(Flipped(new Execute2Memory))
  val writeback = IO(new WriteBack2Execute)
  val hazard = IO(Flipped(new Execute2Hazard))

  // 用lazy val在定义中RegStallOrNext自己？
  val cu_mul = Wire(Bool())
  val cu_div = Wire(Bool())
  val alu_fn = Wire(UInt(SZ_ALU_FN))
  val alu_n = Wire(Bool())
  val num1 = Wire(UInt(32.W))
  val num2 = Wire(UInt(32.W))
  val br_t = Wire(UInt(SZ_BR_TYPE))
  val sel_move = Wire(UInt(SZ_SEL_MOVE))
  val except_type = Wire(UInt(32.W))
  val overflow = WireInit(Bool(), false.B)
  val trap = WireInit(Bool(), false.B)

  // RegStallOrNext
  Seq(
    cu_mul -> decode.mul,
    cu_div -> decode.div,
    alu_fn -> decode.alu_fn,
    alu_n -> decode.alu_n,
    num1 -> decode.num1,
    num2 -> decode.num2,
    br_t -> Mux(hazard.flush, BR_TYPE_NO, decode.br_type),
    fetch.br_addr -> decode.br_addr,
    sel_move -> decode.sel_move,
    memory.pcp8 -> decode.pcp8,
    memory.mem_wen -> Mux(hazard.flush, 0.U, decode.mem_wen), // 把flush逻辑提取成函数？整个动态作用域hazard.flush都不用写了
    memory.reg_wen -> Mux(hazard.flush, 0.U, decode.reg_wen),
    memory.sel_reg_wdata -> decode.sel_reg_wdata,
    memory.reg_waddr -> decode.reg_waddr,
    memory.mem_wdata -> decode.mem_wdata,
    memory.mem_size -> decode.mem_size,
    memory.hi_wen -> Mux(hazard.flush, 0.U, decode.hi_wen),
    memory.lo_wen -> Mux(hazard.flush, 0.U, decode.lo_wen),
    memory.c0_wen -> Mux(hazard.flush, 0.U, decode.c0_wen),
    memory.c0_waddr -> decode.c0_addr,
    memory.is_in_delayslot -> Mux(hazard.flush, false.B, br_t =/= BR_TYPE_NO), // todo J 的延迟槽
    except_type -> Mux(hazard.flush, 0.U, decode.except_type),
  ).foreach { case (reg, next) => reg := RegNext(Mux(hazard.stall, reg, next), 0.U) }

  // alu
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

  // div
  val div = Module(new Div)
  locally {
    import div.io._
    dividend := num1
    divider := num2
    start := cu_div && !hazard.flush // todo 这取消对么
    sign := alu_fn === FN_DIV
    hazard.div_not_ready := cu_div && !ready
  }

  // mul
  val mul = Module(new Mul)
  locally {
    import mul.io._
    multiplicand := num1
    multiplier := num2
    sign := alu_fn === FN_MULT
  }

  // br
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

  // forward
  hazard.wen := memory.reg_wen
  hazard.waddr := memory.reg_waddr
  hazard.hi_wen := memory.hi_wen
  hazard.lo_wen := memory.lo_wen
  decode.wdata := MuxLookup(memory.sel_reg_wdata, 0.U, Array(
    SEL_REG_WDATA_EX -> memory.alu_out,
    SEL_REG_WDATA_LNK -> memory.pcp8,
  ))
  writeback.c0_raddr := memory.c0_waddr // 都是rd
  hazard.c0_raddr := writeback.c0_raddr

  private def c0 = MuxCase(writeback.c0_rdata, Array(
    (hazard.forward_c0 === FORWARD_C0_MEM) -> memory.c0_data,
    (hazard.forward_c0 === FORWARD_HILO_WB) -> writeback.c0_data,
  ))

  // misc output
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
  memory.c0_wdata := num2 // $rt
  memory.except_type := Cat(except_type(31, 12), overflow, trap, except_type(9, 0))
  // todo overflow

  // debug
  if (c.dExecute) {
    printf(p"[log execute]\n\tin1 = ${Hexadecimal(decode.num1)}, in2 = ${Hexadecimal(decode.num2)}, adder_out = ${Hexadecimal(adder_out)}\n")
  }
  if (c.dBrUnit) {
    printf(p"[log execute]\n\tbranch = ${fetch.branch}, br_addr >> 2 = ${fetch.br_addr / 4.U}\n")
  }
}