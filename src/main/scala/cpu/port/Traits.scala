// See LICENSE for license details.

package cpu.port

import chisel3._

trait Flush extends Bundle {
  val flush = Output(Bool())
}

trait Stall extends Bundle {
  val stall = Output(Bool())
}