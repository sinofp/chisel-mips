// See LICENSE for license details.

package cpu
import chisel3._

class PC extends Module {
  val io = IO(new Bundle() {
    val pc_next = Input(UInt(32.W))
    val pc_now = Output(UInt(32.W))
  })

  io.pc_now := RegNext(io.pc_next, 0.U(32.W))
}
