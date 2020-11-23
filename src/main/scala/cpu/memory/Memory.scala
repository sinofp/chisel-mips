// See LICENSE for license details.

package cpu.memory

import chisel3._
import chisel3.util.{Counter, MuxCase}
import cpu.decode.CtrlSigDef._
import cpu.port.hazard.Memory2Hazard
import cpu.port.stage.{Decode2Memory, Execute2Memory, Memory2WriteBack}
import cpu.util.{Config, DefCon}
import cpu.writeback.CP0._
import cpu.writeback.{Cause, Status}

class Memory(implicit c: Config = DefCon) extends MultiIOModule {
  val decode = IO(Flipped(new Decode2Memory))
  val execute = IO(new Execute2Memory)
  val writeback = IO(new Memory2WriteBack)
  val hazard = IO(Flipped(new Memory2Hazard))

  // RegNext
  writeback.pcp8 := RegNext(execute.pcp8)
  writeback.reg_wen := RegNext(Mux(hazard.flush, 0.U, execute.reg_wen), 0.U)
  writeback.sel_reg_wdata := RegNext(execute.sel_reg_wdata)
  writeback.reg_waddr := RegNext(execute.reg_waddr)
  writeback.alu_out := RegNext(execute.alu_out)
  writeback.hi_wen := RegNext(Mux(hazard.flush, 0.U, execute.hi_wen))
  writeback.hi := RegNext(execute.hi)
  writeback.lo_wen := RegNext(Mux(hazard.flush, 0.U, execute.lo_wen))
  writeback.lo := RegNext(execute.lo)
  writeback.c0_wen := RegNext(Mux(hazard.flush, 0.U, execute.c0_wen))
  writeback.c0_waddr := RegNext(execute.c0_waddr)
  writeback.c0_wdata := RegNext(execute.c0_wdata)
  writeback.is_in_delayslot := RegNext(execute.is_in_delayslot)
  val except_type = RegNext(Mux(hazard.flush, 0.U, execute.except_type))

  // data mem
  val data_mem = Module(new DataMem)
  locally {
    import data_mem.io._
    wen := Mux(writeback.except_type =/= 0.U, false.B, RegNext(execute.mem_wen, 0.U))
    addr := RegNext(execute.alu_out)
    wdata := RegNext(execute.mem_wdata)
    writeback.mem_rdata := rdata
    size := execute.mem_size
  }

  // forward reg
  hazard.wen := writeback.reg_wen
  hazard.waddr := writeback.reg_waddr
  // forward hilo
  hazard.hi_wen := writeback.hi_wen
  hazard.lo_wen := writeback.lo_wen
  // forward cp0 to execute
  hazard.c0_wen := writeback.c0_waddr
  hazard.c0_waddr := writeback.c0_waddr
  execute.hi_forward := writeback.hi
  execute.lo_forward := writeback.lo
  execute.c0_data := writeback.c0_wdata
  decode.wdata := MuxCase(0.U, Array(
    (writeback.sel_reg_wdata === SEL_REG_WDATA_EX) -> writeback.alu_out,
    (writeback.sel_reg_wdata === SEL_REG_WDATA_LNK) -> writeback.pcp8,
    (writeback.sel_reg_wdata === SEL_REG_WDATA_MEM) -> writeback.mem_rdata,
  ))

  // forward cp0 to here
  // 不能直接在Input上:=，所以得用asTypeOf创建一个Wire，在Wire上:=
  val Cause = Mux(writeback.wm_c0_wen && writeback.wm_c0_waddr === CP0_CAUSE,
    writeback.c0_cause.asTypeOf(new Cause).update(writeback.wm_c0_wdata), writeback.c0_cause)
  val EPC = Mux(writeback.wm_c0_wen && writeback.wm_c0_waddr === CP0_EPC, writeback.wm_c0_wdata, writeback.c0_epc)
  val Status = Mux(writeback.wm_c0_wen && writeback.wm_c0_waddr === CP0_STATUS,
    writeback.c0_status.asTypeOf(new Status).update(writeback.wm_c0_wdata), writeback.c0_status)
  // exception
  writeback.except_type := MuxCase(0.U, Array(
    ((((Cause.IP7_IP2 ## Cause.IP1_IP0) & Status.IM7_IM0) =/= 0.U) && Status.EXL === 0.U && Status.IE === 1.U) -> EXCEPT_INT,
    except_type(8).asBool -> EXCEPT_SYSCALL,
    except_type(9).asBool -> EXCEPT_INST_INVALID,
    except_type(10).asBool -> EXCEPT_TRAP,
    except_type(11).asBool -> EXCEPT_OVERFLOW,
    except_type(12).asBool -> EXCEPT_ERET,
  ))
  hazard.EPC := EPC
  hazard.except_type := writeback.except_type

  // debug
  if (c.dExcept) {
    val cnt = Counter(true.B, 100)
    printf(p"[log Memory]\n\tcycle = ${cnt._1}\n " +
      p"\tEXCEPT_TYPE = ${Hexadecimal(writeback.except_type)}, " +
      p"EPC = ${Hexadecimal(writeback.wm_c0_wdata)}\n")
  }
}
