// See LICENSE for license details.

package cpu

import chisel3._
import chisel3.util.{Counter, MuxCase}
import cpu.decode.CtrlSigDef._
import cpu.port.hazard._
import cpu.util.{Config, DefCon}

class HazardUnit(readPorts: Int)(implicit c: Config = DefCon) extends MultiIOModule {
  require(readPorts >= 0)
  val fh = IO(new FHPort)
  val dh = IO(new DHPort(readPorts))
  val eh = IO(new EHPort)
  val mh = IO(new MHPort)
  val wh = IO(new WHPort)

  // RegFile 数据前推
  val forward_port = (i: Int) => MuxCase(FORWARD_NO, Array(
    (eh.wen && (eh.waddr =/= 0.U) && dh.raddr(i) === eh.waddr) -> FORWARD_EXE,
    (mh.wen && (mh.waddr =/= 0.U) && dh.raddr(i) === mh.waddr) -> FORWARD_MEM,
    (wh.wen && (wh.waddr =/= 0.U) && dh.raddr(i) === wh.waddr) -> FORWARD_WB,
  ))
  if (c.dForward) {
    val cnt = Counter(true.B, 100)
    printf(p"[log HazardUnit]\n\tcycle = ${cnt._1}\n" +
      p"\tFORWARD(1): EXE = ${forward_port(0) === FORWARD_EXE}, MEM = ${forward_port(0) === FORWARD_MEM}, WB = ${forward_port(0) === FORWARD_WB}\n" +
      p"\tFORWARD(2): EXE = ${forward_port(1) === FORWARD_EXE}, MEM = ${forward_port(1) === FORWARD_MEM}, WB = ${forward_port(1) === FORWARD_WB}\n")
  }

  for (i <- 0 until readPorts) {
    dh.forward(i) := forward_port(i)
  }

  // cp0数据前推
  eh.forward_c0 := MuxCase(FORWARD_C0_NO, Array(
    (mh.c0_wen && mh.c0_waddr === eh.c0_raddr) -> FORWARD_C0_MEM,
    (wh.c0_wen && wh.c0_waddr === eh.c0_raddr) -> FORWARD_C0_WB,
  ))

  // HILO 数据前推到 Execute，主要为了 mfhi $1 后面的指令用到 $1
  eh.forward_hi := MuxCase(FORWARD_HILO_NO, Array(
    (mh.hi_wen && !eh.hi_wen) -> FORWARD_HILO_MEM,
    (wh.hi_wen && !eh.hi_wen) -> FORWARD_HILO_WB,
  ))
  eh.forward_lo := MuxCase(FORWARD_HILO_NO, Array(
    (mh.lo_wen && !eh.lo_wen) -> FORWARD_HILO_MEM,
    (wh.lo_wen && !eh.lo_wen) -> FORWARD_HILO_WB,
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
  val load_stall = dh.forward.exists((_: UInt) === FORWARD_EXE) && dh.prev_load
  fh.stall := load_stall || eh.div_not_ready
  dh.stall := load_stall || eh.div_not_ready
  eh.flush := load_stall
  eh.stall := eh.div_not_ready

  //                        ↓ branch flush
  // cycle         : c1 c2 c3 c4 c5
  // branch        : f1 d1 e1 m1 w1
  // delay slot    :    f2 d2 e2 m2 w2
  // inst to flush :       f3 xx xx xx xx
  // target        :          f4 d4 e4 m4 w4
  // 在 c3, execute 中的 br_unit 判断出要 branch
  // c4 时, fetch 照样刷新, 但 decode 要关掉各种副作用
  dh.flush := eh.branch
}
