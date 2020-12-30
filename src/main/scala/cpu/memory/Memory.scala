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

  // RegNext
  writeback.pcp8 := RegNext(execute.pcp8)
  writeback.rf.wen := RegNext(Mux(hazard.flush, 0.U, execute.rf.wen), 0.U)
  writeback.sel_reg_wdata := RegNext(execute.sel_reg_wdata)
  writeback.rf.waddr := RegNext(execute.rf.waddr)
  writeback.alu_out := RegNext(execute.alu_out)
  writeback.hi.wen := RegNext(Mux(hazard.flush, 0.U, execute.hi.wen))
  writeback.hi := RegNext(execute.hi)
  writeback.lo.wen := RegNext(Mux(hazard.flush, 0.U, execute.lo.wen))
  writeback.lo := RegNext(execute.lo)
  writeback.c0.wen := RegNext(Mux(hazard.flush, 0.U, execute.c0.wen))
  writeback.c0.waddr := RegNext(execute.c0.waddr)
  writeback.c0.wdata := RegNext(execute.c0.wdata)
  writeback.is_in_delayslot := RegNext(execute.is_in_delayslot)
  val except_type = RegNext(Mux(hazard.flush, 0.U, execute.except_type))
  val mem_wen = RegNext(execute.mem.wen, false.B)
  val mem_size = RegNext(execute.mem.size, 0.U)
  val mem_wdata = RegNext(execute.mem.wdata, 0.U)
  val data_sram_en = RegNext(execute.data_sram_en, false.B)

  locally {
    import data_sram._
    en := true.B
    wen := Mux(writeback.except_type === 0.U, Mux(mem_wen, MuxLookup(mem_size, "b1111".U, Array(
      MEM_H -> "b0011".U,
      MEM_B -> "b0001".U,
    )), 0.U), 0.U)
    // 要写的话，用本阶段地址；要读的话，用上阶段地址，保证读这周期（=上阶段的下周期）给出正确值
    addr := Mux(mem_wen, writeback.alu_out, execute.alu_out)
    wdata := mem_wdata
    writeback.mem_rdata := MuxLookup(mem_size, rdata, Array(
      MEM_HU -> Cat(Fill(16, 0.U), rdata(15, 0)),
      MEM_H -> Cat(Fill(16, rdata(15)), rdata(15, 0)),
      MEM_BU -> Cat(Fill(24, 0.U), rdata(7, 0)),
      MEM_B -> Cat(Fill(24, rdata(7)), rdata(7, 0)),
    ))
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
