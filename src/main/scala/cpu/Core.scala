// See LICENSE for license details.

package cpu

import chisel3._
import cpu.decode.Decode
import cpu.execute.Execute
import cpu.fetch.Fetch
import cpu.memory.Memory
import cpu.port.core.SramIO
import cpu.util.{Config, DefCon}
import cpu.writeback.WriteBack

class Core(implicit c: Config = DefCon) extends MultiIOModule {
  val inst_sram = IO(new SramIO)
  val data_sram = IO(new SramIO)
  val int       = IO(Input(UInt(6.W)))

  val fetch     = Module(new Fetch)
  val decode    = Module(new Decode)
  val execute   = Module(new Execute)
  val memory    = Module(new Memory)
  val writeback = Module(new WriteBack)
  val hazard    = Module(new HazardUnit(2))

  // br & j
  fetch.execute <> execute.fetch
  fetch.decode  <> decode.fetch

  decode.writeBack  <> writeback.decode
  decode.memory     <> memory.decode
  execute.decode    <> decode.execute
  execute.memory    <> memory.execute
  execute.writeback <> writeback.execute
  writeback.memory  <> memory.writeback

  hazard.fetch     <> fetch.hazard
  hazard.decode    <> decode.hazard
  hazard.execute   <> execute.hazard
  hazard.memory    <> memory.hazard
  hazard.writeback <> writeback.hazard

  // IO
  int              <> writeback.int
  fetch.inst_sram  <> inst_sram
  memory.data_sram <> data_sram
}
