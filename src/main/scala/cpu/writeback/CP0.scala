// See LICENSE for license details.

package cpu.writeback

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._
import cpu.util.{Config, DefCon}

class Status extends Bundle {
  val zeros_1 = UInt(9.W)
  val Bev = Bool()
  val zeros_2 = UInt(6.W)
  val IM7_IM0 = UInt(8.W)
  val zeros_3 = UInt(6.W)
  val EXL = Bool()
  val IE = Bool()
}

class Cause extends Bundle {
  val BD = Bool()
  val TI = Bool()
  val zeros_1 = UInt(15.W)
  val IP7_IP2 = UInt(5.W)
  val IP1_IP0 = UInt(2.W)
  val zeros_2 = UInt(1.W)
  val ExcCode = UInt(5.W)
  val zeros_3 = UInt(2.W)
}

object CP0 {
  val CP0_BADVADDR = 8.U
  val CP0_COUNT = 9.U
  val CP0_COMPARE = 11.U
  val CP0_STATUS = 12.U
  val CP0_CAUSE = 13.U
  val CP0_EPC = 14.U
  val SZ_EXCEPT_TYPE = 32.W
  val EXCEPT_INT = 1.U(SZ_EXCEPT_TYPE)
  val EXCEPT_SYSCALL = 8.U(SZ_EXCEPT_TYPE)
  val EXCEPT_INST_INVALID = 0xa.U(SZ_EXCEPT_TYPE)
  val EXCEPT_TRAP = 0xd.U(SZ_EXCEPT_TYPE)
  val EXCEPT_OVERFLOW = 0xc.U(SZ_EXCEPT_TYPE)
  val EXCEPT_ERET = 0xe.U(SZ_EXCEPT_TYPE)
}

import cpu.writeback.CP0._

class CP0(implicit c: Config = DefCon) extends MultiIOModule {
  val i = IO(new Bundle() {
    val wen = Input(Bool())
    val raddr = Input(UInt(5.W))
    val waddr = Input(UInt(5.W))
    val wdata = Input(UInt(32.W))
    val int = Input(UInt(6.W))
    val except_type = Input(UInt(32.W))
    val pc_now = Input(UInt(32.W))
    val is_in_delayslot = Input(Bool())
  })
  val o = IO(new Bundle() {
    val rdata = Output(UInt(32.W))
    val BadVAddr = Output(UInt(32.W))
    val Count = Output(UInt(32.W))
    val Compare = Output(UInt(32.W))
    val Status = Output(new Status)
    val Cause = Output(new Cause)
    val EPC = Output(UInt(32.W))
    val timer_int = Output(Bool())
  })

  import i._
  import o._

  val BadVAddr = RegInit(0.U)
  val Count = RegInit(0.U)
  val Compare = RegInit(0.U)
  val Status = RegInit(0.U.asTypeOf(new Status))
  Status.Bev := 1.U
  val Cause = RegInit(0.U.asTypeOf(new Cause))
  val EPC = RegInit(0.U)
  val timer_int = RegInit(false.B)
  o.getElements.zip(Seq(timer_int, EPC, Cause, Status, Compare, Count, BadVAddr))
    .foreach { case (x, y) => x := y }

  Count := Count + 1.U
  Cause.IP7_IP2 := int

  when(Compare =/= 0.U && Count === Compare) {
    timer_int := true.B
  }

  when(wen) {
    switch(waddr) {
      is(CP0_BADVADDR) {
        BadVAddr := wdata
      }
      is(CP0_COUNT) {
        Count := wdata
      }
      is(CP0_STATUS) {
        val newStatus = wdata.asTypeOf(new Status)
        Status.IM7_IM0 := newStatus.IM7_IM0
        Status.EXL := newStatus.EXL
        Status.IE := newStatus.IE
      }
      is(CP0_COMPARE) {
        Compare := wdata
      }
      is(CP0_CAUSE) {
        Cause.IP1_IP0 := wdata.asTypeOf(new Cause).IP1_IP0
      }
      is(CP0_EPC) {
        EPC := wdata
      }
    }
  }

  switch(except_type) {
    is(EXCEPT_INT) {
      EPC := pc_now - Mux(is_in_delayslot, 4.U, 0.U)
      Cause.BD := is_in_delayslot
      Status.EXL := 1.U
      Cause.ExcCode := 0.U
    }
    is(EXCEPT_SYSCALL) {
      when(Status.EXL === 0.U) {
        EPC := pc_now - Mux(is_in_delayslot, 4.U, 0.U)
        Cause.BD := is_in_delayslot
        Status.EXL := 1.U
        Cause.ExcCode := "b01000".U
      }
    }
    is(EXCEPT_INST_INVALID) {
      when(Status.EXL === 0.U) {
        EPC := pc_now - Mux(is_in_delayslot, 4.U, 0.U)
        Cause.BD := is_in_delayslot
        Status.EXL := 1.U
        Cause.ExcCode := "b01010".U
      }
    }
    is(EXCEPT_TRAP) {
      when(Status.EXL === 0.U) {
        EPC := pc_now - Mux(is_in_delayslot, 4.U, 0.U)
        Cause.BD := is_in_delayslot
        Status.EXL := 1.U
        Cause.ExcCode := "b01101".U
      }
    }
    is(EXCEPT_OVERFLOW) {
      when(Status.EXL === 0.U) {
        EPC := pc_now - Mux(is_in_delayslot, 4.U, 0.U)
        Cause.BD := is_in_delayslot
        Status.EXL := 1.U
        Cause.ExcCode := "b01100".U
      }
    }
    is(EXCEPT_ERET) {
      Status.EXL := 0.U
    }
  }

  // 直接写成regfile那样岂不是更方便？
  rdata := MuxLookup(raddr, 0.U, Array(
    CP0_BADVADDR -> BadVAddr,
    CP0_COUNT -> Count,
    CP0_COMPARE -> Compare,
    CP0_STATUS -> Status.asUInt,
    CP0_CAUSE -> Cause.asUInt,
    CP0_EPC -> EPC,
  ))

  if (c.dCP0) {
    val cnt = Counter(true.B, 100)
    printf(p"[log CP0]\n\tcycle = ${cnt._1}\n" +
      p"\tBadVaddr = ${Hexadecimal(BadVAddr)}\n" +
      p"\tCount = ${Hexadecimal(Count)}\n" +
      p"\tCompare = ${Hexadecimal(Compare)}\n" +
      p"\tStatus = ${Hexadecimal(Status.asUInt)}\n" +
      p"\tCause = ${Hexadecimal(Cause.asUInt)}\n" +
      p"\tEPC = ${Hexadecimal(EPC)}\n" +
      p"\ttimer_int = ${Binary(timer_int)}\n")
  }
}

object CP00 extends App {
  new ChiselStage emitVerilog new CP0
}