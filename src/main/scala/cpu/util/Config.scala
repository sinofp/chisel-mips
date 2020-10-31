// See LICENSE for license details.

package cpu.util

import chisel3._

// @formatter:off
case class Config(
    debugFetch: Boolean = false,
    debugPC: Boolean = false,
    debugInstMem: Boolean = false,
    debugDecode: Boolean = false,
    debugCU: Boolean = false,
    debugRegFile: Boolean = false,
    debugExecute: Boolean = false,
    debugALU: Boolean = false,
    debugBrUnit: Boolean = false,
    debugMemory: Boolean = false,
    debugDataMem: Boolean = false,
    debugWriteback: Boolean = false,
    debugHILO: Boolean = false,
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
