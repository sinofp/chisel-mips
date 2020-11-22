// See LICENSE for license details.

package cpu.writeback

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._
import cpu.util.{Config, DefCon}

class CP0(implicit c: Config = DefCon) extends MultiIOModule {
  val i = IO(new Bundle() {
    val wen = Input(Bool())
    val raddr = Input(UInt(5.W))
    val waddr = Input(UInt(5.W))
    val wdata = Input(UInt(32.W))
    val int = Input(UInt(6.W))
  })
  val o = IO(new Bundle() {
    val rdata = Output(UInt(32.W))
    val BadVAddr = Output(UInt(32.W))
    val Count = Output(UInt(32.W))
    val Compare = Output(UInt(32.W))
    val Status = Output(UInt(32.W))
    val Cause = Output(UInt(32.W))
    val EPC = Output(UInt(32.W))
    val timer_int = Output(Bool())
  })

  val BadVAddr = RegInit(0.U)
  val Count = RegInit(0.U)
  val Compare = RegInit(0.U)
  val Status = RegInit(("b" + "0" * 7 + "1" + "0" * 24).U)
  val Cause = RegInit(0.U(32.W))
  val EPC = RegInit(0.U)
  val timer_int = RegInit(false.B)
  o.getElements.zip(Seq(timer_int, EPC, Cause, Status, Compare, Count, BadVAddr))
    .foreach { case (x, y) => x := y.asUInt() }

  Count := Count + 1.U
  //  Cause(15, 10) := i.int
  Cause := Cat(Cause(31, 16), i.int, Cause(9, 0))
  //  Cause := {
  //    val bools = VecInit(Cause.asBools)
  //    (10 to 15).foreach(x => bools(x) := i.int(x - 10))
  //    bools.asUInt
  //  }

  when(Compare =/= 0.U && Count === Compare) {
    timer_int := true.B
  }

  val CP0_BADVADDR = 8.U
  val CP0_COUNT = 9.U
  val CP0_COMPARE = 11.U
  val CP0_STATUS = 12.U
  val CP0_CAUSE = 13.U
  val CP0_EPC = 14.U
  when(i.wen) {
    switch(i.waddr) {
      is(CP0_BADVADDR) {
        BadVAddr := i.wdata
      }
      is(CP0_COUNT) {
        Count := i.wdata
      }
      is(CP0_STATUS) {
        Status := i.wdata
      }
      is(CP0_COMPARE) {
        Compare := i.wdata
      }
      is(CP0_CAUSE) {
        // 只有部分(9,8)可写
        Cause := Cat(Cause(31, 10), i.wdata(9, 8), Cause(7, 0))
      }
      is(CP0_EPC) {
        EPC := i.wdata
      }
    }
  }

  // 直接写成regfile那样岂不是更方便？
  o.rdata := MuxLookup(i.raddr, 0.U, Array(
    CP0_BADVADDR -> BadVAddr,
    CP0_COUNT -> Count,
    CP0_COMPARE -> Compare,
    CP0_STATUS -> Status,
    CP0_CAUSE -> Cause,
    CP0_EPC -> EPC,
  ))
}

object CP0 extends App {
  new ChiselStage emitVerilog new CP0
}