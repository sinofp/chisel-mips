// See LICENSE for license details.

package cpu

import chisel3._
import chisel3.stage.ChiselStage
import cpu.decode.Decode
import cpu.execute.Execute
import cpu.fetch.Fetch
import cpu.memory.Memory
import cpu.util.{Config, DefCon}
import cpu.writeback.WriteBack

class Top(implicit c: Config = DefCon) extends MultiIOModule {
  val junk_output = IO(Output(Bool())) // 随便加个输出，防止优化成空电路

  val fetch = Module(new Fetch)
  val decode = Module(new Decode)
  val execute = Module(new Execute)
  val memory = Module(new Memory)
  val writeback = Module(new WriteBack)
  val hazard = Module(new HazardUnit(2))

  //  fetch.ef <> execute.ef
  locally {
    import execute.ef._
    import fetch.ef._
    pc_jump := br_addr
    jump := branch // todo J
    junk_output := branch
  }
  decode.fd <> fetch.fd
  decode.wd <> writeback.wd
  decode.ed <> execute.ed
  decode.md <> memory.md
  execute.de <> decode.de
  memory.em <> execute.em
  writeback.mw <> memory.mw

  hazard.fh <> fetch.hf
  hazard.dh <> decode.hd
  hazard.eh <> execute.he
  hazard.mh <> memory.hm
  hazard.wh <> writeback.hw
}

object Top extends App {
  new ChiselStage emitVerilog new Top
}