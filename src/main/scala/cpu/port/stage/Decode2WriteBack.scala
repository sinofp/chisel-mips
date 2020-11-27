// See LICENSE for license details.

package cpu.port.stage

import chisel3._
import cpu.port.{WAddr, WData, WEn}

class Decode2WriteBack extends Bundle with WEn with WData with WAddr
