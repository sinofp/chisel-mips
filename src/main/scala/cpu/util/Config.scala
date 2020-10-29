// See LICENSE for license details.

package cpu.util

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
    val inputInst: Boolean = false,
) {}

object DefCon extends Config
