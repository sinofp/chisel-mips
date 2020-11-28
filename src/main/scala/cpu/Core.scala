// See LICENSE for license details.

package cpu

import chisel3._
import cpu.decode.Decode
import cpu.execute.Execute
import cpu.fetch.Fetch
import cpu.memory.Memory
import cpu.port.core.{Core2DataMem, Core2InstMem, Core2WriteBack}
import cpu.util.{Config, DefCon}
import cpu.writeback.WriteBack

class Core(implicit c: Config = DefCon) extends MultiIOModule {
  val inst_mem = IO(new Core2InstMem)
  val data_mem = IO(new Core2DataMem)
  val io = IO(new Bundle() {
    val interrupt = new Core2WriteBack
  })

  val fetch = Module(new Fetch)
  val decode = Module(new Decode)
  val execute = Module(new Execute)
  val memory = Module(new Memory)
  val writeback = Module(new WriteBack)
  val hazard = Module(new HazardUnit(2))

  // br & j
  fetch.execute <> execute.fetch
  fetch.decode <> decode.fetch

  decode.writeBack <> writeback.decode
  decode.memory <> memory.decode
  execute.decode <> decode.execute
  execute.memory <> memory.execute
  execute.writeback <> writeback.execute
  writeback.memory <> memory.writeback

  hazard.fetch <> fetch.hazard
  hazard.decode <> decode.hazard
  hazard.execute <> execute.hazard
  hazard.memory <> memory.hazard
  hazard.writeback <> writeback.hazard

  // IO
  io.interrupt <> writeback.core
  fetch.inst_mem <> inst_mem
  memory.data_mem <> data_mem
}
