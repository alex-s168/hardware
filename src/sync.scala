//-----------------------------------------------------------------------------
// Copyright (C) 2025 Alexander Nutz
//
// license is in the closest parent directory
//-----------------------------------------------------------------------------


package alex_s168_hw

import chisel3._

class PulseSync extends Module {
  val io = IO(new Bundle {
    val inClock  = Input(Clock())
    val inReset  = Input(Reset())
    val inPulse  = Input(Bool())

    val outPulse = Output(Bool())
  })

  val toggle = withClockAndReset (io.inClock, io.inReset) {
    val t = RegInit(false.B)
    when (io.inPulse) {
      t := ~t
    }
    t
  }

  val sync0 = RegNext(toggle)
  val sync1 = RegNext(sync0)

  io.outPulse := sync0 =/= sync1
}
