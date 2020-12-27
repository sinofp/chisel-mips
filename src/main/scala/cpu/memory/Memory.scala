// See LICENSE for license details.

package cpu.memory

import chisel3._
import chisel3.util._
import cpu.decode.CtrlSigDef._
import cpu.port.core.SramIO
import cpu.port.hazard.Memory2Hazard
import cpu.port.stage.{Execute2Memory, Memory2Decode, Memory2WriteBack}
import cpu.util.{Config, DefCon}
import cpu.writeback.CP0._
import cpu.writeback.{Cause, Status}

class Memory(implicit c: Config = DefCon) extends MultiIOModule {
  val decode = IO(new Memory2Decode)
  val execute = IO(new Execute2Memory)
  val writeback = IO(new Memory2WriteBack)
  val hazard = IO(Flipped(new Memory2Hazard))
  val data_sram = IO(new SramIO)

  val except_type = Wire(UInt(32.W))
  val mem_wen = Wire(Bool())
  val mem_size = Wire(UInt(SZ_MEM_TYPE))
  val mem_wdata = Wire(UInt(32.W))
  val data_sram_en = Wire(Bool())

  // RegStallOrNext
  Seq(
    writeback.pcp8 -> execute.pcp8,
    writeback.rf.wen -> Mux(hazard.flush, false.B, execute.rf.wen),
    writeback.sel_reg_wdata -> execute.sel_reg_wdata,
    writeback.rf.waddr -> execute.rf.waddr,
    writeback.alu_out -> execute.alu_out,
    writeback.hi.wen -> Mux(hazard.flush, false.B, execute.hi.wen),
    writeback.hi -> execute.hi,
    writeback.lo.wen -> Mux(hazard.flush, false.B, execute.lo.wen),
    writeback.lo -> execute.lo,
    writeback.c0.wen -> Mux(hazard.flush, false.B, execute.c0.wen),
    writeback.c0.waddr -> execute.c0.waddr,
    writeback.c0.wdata -> execute.c0.wdata,
    writeback.is_in_delayslot -> execute.is_in_delayslot,
    except_type -> Mux(hazard.flush, 0.U, execute.except_type),
    mem_wen -> execute.mem.wen,
    mem_size -> execute.mem.size,
    mem_wdata -> execute.mem.wdata,
    data_sram_en -> execute.data_sram_en,
  ).foreach { case (reg, next) => reg := RegNext(Mux(hazard.stall, reg, next.asTypeOf(reg)), 0.U.asTypeOf(reg)) }
  // todo asTypeOf 是为了 {writeback,execute}{hi,lo} 准备的，因为它们都是 new Bundle with XXX，我要不要改成单个类？

  locally {
    import data_sram._
    en := data_sram_en
    wen := Mux(writeback.except_type === 0.U, Mux(mem_wen, MuxLookup(mem_size, "b1111".U, Array(
      MEM_H -> "b0011".U,
      MEM_B -> "b0001".U,
    )), 0.U), 0.U)
    addr := writeback.alu_out
    wdata := mem_wdata
    writeback.mem_rdata := MuxLookup(mem_size, rdata, Array(
      MEM_HU -> Cat(Fill(16, 0.U), rdata(15, 0)),
      MEM_H -> Cat(Fill(16, rdata(15)), rdata(15, 0)),
      MEM_BU -> Cat(Fill(24, 0.U), rdata(7, 0)),
      MEM_B -> Cat(Fill(24, rdata(7)), rdata(7, 0)),
    ))
    //    hazard.sram_stall := en && !wen.orR // 读
    //    if (c.dBuiltinMem) hazard.sram_stall := false.B
  }

  // forward reg
  hazard.rf.wen := writeback.rf.wen
  hazard.rf.waddr := writeback.rf.waddr
  // forward hilo
  hazard.hi.wen := writeback.hi.wen
  hazard.lo.wen := writeback.lo.wen
  // forward cp0 to execute
  hazard.c0.wen := writeback.c0.waddr
  hazard.c0.waddr := writeback.c0.waddr
  execute.fwd_hi.wdata := writeback.hi.wdata
  execute.fwd_lo.wdata := writeback.lo.wdata
  execute.fwd_c0.wdata := writeback.c0.wdata
  decode.wdata := MuxCase(0.U, Array(
    (writeback.sel_reg_wdata === SEL_REG_WDATA_EX) -> writeback.alu_out,
    (writeback.sel_reg_wdata === SEL_REG_WDATA_LNK) -> writeback.pcp8,
    (writeback.sel_reg_wdata === SEL_REG_WDATA_MEM) -> writeback.mem_rdata,
  ))

  // forward cp0 to here
  // 不能直接在Input上:=，所以得用asTypeOf创建一个Wire，在Wire上:=
  val Cause = Mux(writeback.fwd_c0.wen && writeback.fwd_c0.waddr === CP0_CAUSE,
    writeback.c0_cause.asTypeOf(new Cause).update(writeback.fwd_c0.wdata), writeback.c0_cause)
  val EPC = Mux(writeback.fwd_c0.wen && writeback.fwd_c0.waddr === CP0_EPC, writeback.fwd_c0.wdata, writeback.c0_epc)
  val Status = Mux(writeback.fwd_c0.wen && writeback.fwd_c0.waddr === CP0_STATUS,
    writeback.c0_status.asTypeOf(new Status).update(writeback.fwd_c0.wdata), writeback.c0_status)
  // exception
  writeback.except_type := MuxCase(0.U, Array(
    ((((Cause.IP7_IP2 ## Cause.IP1_IP0) & Status.IM7_IM0) =/= 0.U) && Status.EXL === 0.U && Status.IE === 1.U) -> EXCEPT_INT,
    except_type(8).asBool -> EXCEPT_SYSCALL,
    except_type(9).asBool -> EXCEPT_INST_INVALID,
    except_type(10).asBool -> EXCEPT_TRAP,
    except_type(11).asBool -> EXCEPT_OVERFLOW,
    except_type(12).asBool -> EXCEPT_ERET,
    except_type(13).asBool -> EXCEPT_BREAK,
  ))
  hazard.EPC := EPC
  hazard.except_type := writeback.except_type

  // debug
  if (c.dExcept) {
    val cnt = Counter(true.B, 100)
    printf(p"[log Memory]\n\tcycle = ${cnt._1}\n " +
      p"\tEXCEPT_TYPE = ${Hexadecimal(writeback.except_type)}, " +
      p"EPC = ${Hexadecimal(writeback.fwd_c0.wdata)}\n")
  }
}
