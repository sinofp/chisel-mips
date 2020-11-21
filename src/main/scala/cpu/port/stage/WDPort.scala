// See LICENSE for license details.

package cpu.port.stage

import chisel3._
import cpu.port.{Wdata, WenWaddr}

class WDPort extends Bundle with WenWaddr with Wdata