// See LICENSE for license details.

package cpu.util

import chisel3._

// @formatter:off
case class Config(
    dFetch: Boolean = false,
    dInstMem: Boolean = false,
    dDecode: Boolean = false,
    dCU: Boolean = false,
    dRegFile: Boolean = false,
    dExecute: Boolean = false,
    dALU: Boolean = false,
    dDiv: Boolean = false,
    dBrUnit: Boolean = false,
    dMemory: Boolean = false,
    dDataMem: Boolean = false,
    dWriteback: Boolean = false,
    dHILO: Boolean = false,
    dTReg: Boolean = false,
    dForward: Boolean = false,
    insts: Seq[UInt] = Seq(
      "20080064",
      "32290001",
      "21290001",
      "1528fffe",
      "ac090000"
    ).map("h" + _).map(_.U)
)
// @formatter:on

object DefCon extends Config
