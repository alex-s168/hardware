//-----------------------------------------------------------------------------
// Copyright (C) 2025 Alexander Nutz
//
// license is in the closest parent directory
//-----------------------------------------------------------------------------


package alex_s168_hw

import chisel3._

class UnshiftReg(width: Int) extends Module {
  val io = IO(new Bundle {
    val load   = Input(Bool())
    val in     = Input(UInt(width.W))

    val shift  = Input(Bool())
    val out    = Output(UInt(1.W))
  })

  val reg = RegInit(0.U(width.W))

  when (io.load) {
    reg := io.in
  } .otherwise {
    when (io.shift) {
      reg := reg >> 1
    }
  }

  io.out := reg(0)
}
