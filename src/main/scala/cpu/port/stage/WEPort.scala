// See LICENSE for license details.

package cpu.port.stage

import chisel3._
import cpu.port.{C0UN, HILO}

class WEPort extends Bundle with HILO with C0UN
