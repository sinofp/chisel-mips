// See LICENSE for license details.

package cpu.port.hazard

import chisel3._
import cpu.port.{HILOWenIn, WenWaddr}

class WHPort extends Bundle with WenWaddr with HILOWenIn

