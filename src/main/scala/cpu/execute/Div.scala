// See LICENSE for license details.

package cpu.execute

import chisel3._
import cpu.util.{Config, DefCon}

class Div(implicit c: Config = DefCon) extends Module {
  val io = IO(new Bundle() {
    val start = Input(Bool())
    val sign = Input(Bool())
    val dividend = Input(UInt(32.W))
    val divider = Input(UInt(32.W))
    val busy = Output(Bool()) // todo 取消输出busy？
    val ready = Output(Bool())
    val quotient = Output(UInt(32.W))
    val remainder = Output(UInt(32.W))
  })

  val quotient = Reg(UInt(32.W))
  val remainder = Reg(UInt(32.W))
  val divider = Reg(UInt(32.W))
  val cnt = RegInit(UInt(5.W), 0.U)
  val busy = RegInit(Bool(), false.B)
  val busy2 = RegNext(busy, false.B)
  val r_sign = Reg(Bool())

  io.remainder := {
    val ioru = Mux(r_sign, remainder + divider, remainder)
    Mux(io.sign && io.dividend(31), -ioru, ioru) // 如果被除数为负，余数为负
  }
  io.quotient := Mux(io.sign && io.dividend(31) ^ io.divider(31), -quotient, quotient) // 源操作数符号相反，商为负
  io.busy := busy
  io.ready := !busy && busy2

  when(io.start && !busy) {
    remainder := 0.U
    r_sign := 0.U
    quotient := Mux(io.sign && io.dividend(31), -io.dividend, io.dividend)
    divider := Mux(io.sign && io.divider(31), -io.divider, io.divider)
    cnt := 0.U
    busy := true.B
  }.elsewhen(busy) {
    val sub_add = (remainder ## quotient(31)) + Mux(r_sign, 0.U ## divider, -(0.U ## divider))
    remainder := sub_add(31, 0)
    r_sign := sub_add(32)
    quotient := quotient(30, 0) ## ~sub_add(32)
    cnt := cnt + 1.U
    busy := ~(cnt === 31.U)
  }
}
