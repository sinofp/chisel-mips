// See LICENSE for license details.

package cpu.port.hazard

import chisel3._
import cpu.port.{Flush, Stall}

class FHPort extends Bundle with Stall with Flush