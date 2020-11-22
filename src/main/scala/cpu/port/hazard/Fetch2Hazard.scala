// See LICENSE for license details.

package cpu.port.hazard

import chisel3._
import cpu.port.Stall

class Fetch2Hazard extends Bundle with Stall