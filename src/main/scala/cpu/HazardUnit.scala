// See LICENSE for license details.

package cpu

import chisel3._
import chisel3.util.{Counter, MuxCase, MuxLookup}
import cpu.decode.CtrlSigDef._
import cpu.port.hazard._
import cpu.util.{Config, DefCon}
import cpu.writeback.CP0._

class HazardUnit(readPorts: Int)(implicit c: Config = DefCon) extends MultiIOModule {
  require(readPorts >= 0)
  val fetch = IO(new Fetch2Hazard)
  val decode = IO(new Decode2Hazard(readPorts))
  val execute = IO(new Execute2Hazard)
  val memory = IO(new Memory2Hazard)
  val writeback = IO(new Writeback2Hazard)

  // RegFile 数据前推
  val forward_port = (i: Int) => MuxCase(FORWARD_NO, Array(
    (execute.wen && (execute.waddr =/= 0.U) && decode.raddr(i) === execute.waddr) -> FORWARD_EXE,
    (memory.wen && (memory.waddr =/= 0.U) && decode.raddr(i) === memory.waddr) -> FORWARD_MEM,
    (writeback.wen && (writeback.waddr =/= 0.U) && decode.raddr(i) === writeback.waddr) -> FORWARD_WB,
  ))
  if (c.dForward) {
    val cnt = Counter(true.B, 100)
    printf(p"[log HazardUnit]\n\tcycle = ${cnt._1}\n" +
      p"\tFORWARD(1): EXE = ${forward_port(0) === FORWARD_EXE}, MEM = ${forward_port(0) === FORWARD_MEM}, WB = ${forward_port(0) === FORWARD_WB}\n" +
      p"\tFORWARD(2): EXE = ${forward_port(1) === FORWARD_EXE}, MEM = ${forward_port(1) === FORWARD_MEM}, WB = ${forward_port(1) === FORWARD_WB}\n")
  }

  for (i <- 0 until readPorts) {
    decode.forward(i) := forward_port(i)
  }

  // cp0数据前推
  execute.forward_c0 := MuxCase(FORWARD_C0_NO, Array(
    (memory.c0_wen && memory.c0_waddr === execute.c0_raddr) -> FORWARD_C0_MEM,
    (writeback.c0_wen && writeback.c0_waddr === execute.c0_raddr) -> FORWARD_C0_WB,
  ))

  // HILO 数据前推到 Execute，主要为了 mfhi $1 后面的指令用到 $1
  execute.forward_hi := MuxCase(FORWARD_HILO_NO, Array(
    (memory.hi_wen && !execute.hi_wen) -> FORWARD_HILO_MEM,
    (writeback.hi_wen && !execute.hi_wen) -> FORWARD_HILO_WB,
  ))
  execute.forward_lo := MuxCase(FORWARD_HILO_NO, Array(
    (memory.lo_wen && !execute.lo_wen) -> FORWARD_HILO_MEM,
    (writeback.lo_wen && !execute.lo_wen) -> FORWARD_HILO_WB,
  ))
  //  eh.forward_hi := FORWARD_HILO_NO
  //  eh.forward_lo := FORWARD_HILO_NO
  //        ↓ load stall
  // c1 c2 c3 c4 c5 c6 c7 (cycle)
  // f1 d1 e1 m1 w1
  //    f2 d2 xx xx xx
  //       f2 d2 e2 m2 w2
  // c3 时发生 load stall, c4 时 fetch 应该还是 f2, decode 应该还是 d2, execute 以及其后应该被冲刷
  // 所以 c3 时 fetch, decode, execute 应该分别保留 pc_now, 保留 inst, 下周期输出 mem/reg_wen 为 0, br_type 为 no
  val load_stall = decode.forward.exists((_: UInt) === FORWARD_EXE) && decode.prev_load
  fetch.stall := (load_stall || execute.div_not_ready) && !fetch.estart
  decode.stall := (load_stall || execute.div_not_ready) && !fetch.estart
  execute.flush := load_stall || fetch.estart
  execute.stall := execute.div_not_ready && !fetch.estart

  //                        ↓ branch flush
  // cycle         : c1 c2 c3 c4 c5
  // branch        : f1 d1 e1 m1 w1
  // delay slot    :    f2 d2 e2 m2 w2
  // inst to flush :       f3 xx xx xx xx
  // target        :          f4 d4 e4 m4 w4
  // 在 c3, execute 中的 br_unit 判断出要 branch
  // c4 时, fetch 照样刷新, 但 decode 要关掉各种副作用
  decode.flush := execute.branch || fetch.estart

  // exception
  val entry = c.dExceptEntry.getOrElse("hbfc00380".U)
  fetch.estart := memory.except_type =/= 0.U
  fetch.newpc := MuxLookup(memory.except_type, 0.U, Array(
    EXCEPT_INT -> entry,
    EXCEPT_SYSCALL -> entry,
    EXCEPT_INST_INVALID -> entry,
    EXCEPT_TRAP -> entry,
    EXCEPT_OVERFLOW -> entry,
    EXCEPT_ERET -> memory.EPC,
  ))
  memory.flush := fetch.estart
  writeback.flush := fetch.estart
}
