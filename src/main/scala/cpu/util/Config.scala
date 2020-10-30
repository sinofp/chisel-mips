// See LICENSE for license details.

package cpu.util

import chisel3._

class Config(
    val debugFetch: Boolean = false,
    val debugPC: Boolean = false,
    val debugInstMem: Boolean = false,
    val debugDecode: Boolean = false,
    val debugCU: Boolean = false,
    val debugRegFile: Boolean = false,
    val debugExecute: Boolean = false,
    val debugALU: Boolean = false,
    val debugBrUnit: Boolean = false,
    val debugMemory: Boolean = false,
    val debugDataMem: Boolean = false,
    val debugWriteback: Boolean = false,
    val debugHILO: Boolean = false,
    val insts: Seq[UInt] = Seq(
      "20080064",
      "32290001",
      "21290001",
      "1528fffe",
      "ac090000"
    ).map("h" + _).map(_.U)
) {}

object DefCon extends Config
