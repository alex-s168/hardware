//-----------------------------------------------------------------------------
// Copyright (C) 2025 Alexander Nutz
//
// license is in the closest parent directory
//-----------------------------------------------------------------------------


package alex_s168_hw

import chisel3._
import chisel3.util._
import alex_s168_hw._

class MAC(
  // adds about 120 cells on iCE40 FPGAs
  supports_full_duplex: Boolean
) extends Module {
  val io = IO(new Bundle {
    val rmii_r = new RmiiR()
    val rmii_t = new RmiiT()

    // self clock needs to be at slowest a fourth of the RMII speed (50 MHz), but can also be a multiple
    val rmii_clk = Input(Clock())

    // have to reset both MAC and PHY when these change
    val is10mbits = Input(UInt(1.W))
    val full_duplex = Input(UInt(1.W))
  })

  val xmit_io = IO(new Bundle {
    val transmit = Input(UInt(1.W))
    val byte_sent = Output(UInt(1.W))
    val byte = Input(UInt(8.W))
  })
  xmit_io.byte_sent := 0.U

  // outputted packets contain CRC, but that should be ignored (last 4 bytes)
  val recv_io = IO(new Bundle {
    // only outputs for one tick
    val frame_start = Output(UInt(1.W))

    // triggered when already sent frame is invalid
    // only outputs for one tick
    // can be on at the same time as valid_byte!!
    val abort_frame = Output(UInt(1.W))

    val valid_byte = Output(UInt(1.W))
    val byte = Output(UInt(8.W))

    // only on for one cycle
    // comes directly from PHY
    val info_carrier_lost_during_packet = Output(UInt(1.W))
  })
  recv_io.frame_start := 0.U
  recv_io.valid_byte := 0.U
  recv_io.byte := 0.U
  recv_io.abort_frame := 0.U

  val recv = withClock (io.rmii_clk) {
    Module(new RmiiRecv)
  }
  recv.io.rmii_r <> io.rmii_r
  recv.io.is10mbits := io.is10mbits

  val info_carrier_lost_during_packet_sync = Module(new PulseSync)
  info_carrier_lost_during_packet_sync.io.inClock := io.rmii_clk
  info_carrier_lost_during_packet_sync.io.inReset := reset
  info_carrier_lost_during_packet_sync.io.inPulse := recv.io.info_carrier_lost_during_packet
  recv_io.info_carrier_lost_during_packet := info_carrier_lost_during_packet_sync.io.outPulse

  val xmit = withClock (io.rmii_clk) {
    Module(new RmiiTransmit)
  }
  xmit.io.rmii_t <> io.rmii_t
  xmit.io.is10mbits := io.is10mbits
  xmit.io.byte := 0.U

  object XmitState extends ChiselEnum {
    val Idle = Value
    val Preamble, SFD, Data = Value
    val CRC0, CRC1, CRC2, CRC3 = Value
  }

  val xmit_state = RegInit(XmitState.Idle)

  val do_full_duplex = io.full_duplex & (if (supports_full_duplex) 1.U else 0.U)

  val (recv_crc_out, xmit_crc_out) = if (supports_full_duplex) {
    val recv_crc = withReset (reset.asBool || !recv.io.in_frame) { Module(new CRC32_8) }
    recv_crc.io.isEnable := recv.io.valid_byte
    recv_crc.io.dataIn := recv.io.byte

    val xmit_crc = withReset (reset.asBool || xmit_state === XmitState.Data) { Module(new CRC32_8) }
    xmit_crc.io.isEnable := xmit_io.byte_sent // this triggers on byte write done instead of new byte, but that's the same here
    xmit_crc.io.dataIn := xmit_io.byte

    (recv_crc.io.crc, xmit_crc.io.crc)
  } else {
    // TODO: only use one crc checker
    (0.U, 0.U)
  }

  xmit.io.tx_frame := xmit_state =/= XmitState.Idle

  val xmit_preamble_counter = RegInit(0.U(log2Ceil(7).W))

  // TODO: do clock domain crossings break state transitions here?
  // could and transition check with rmii clock as bool

  switch (xmit_state) {
    is (XmitState.Idle) {
      when (xmit_io.transmit === 1.U) {
        xmit_state := XmitState.Preamble
        xmit_preamble_counter := 0.U
      }
    }

    is (XmitState.Preamble) {
      xmit.io.byte := "b10101010".U
      when (xmit.io.byte_sent === 1.U) {
        when (xmit_preamble_counter === 6.U) {
          xmit_state := XmitState.SFD
        }.otherwise {
          xmit_preamble_counter := xmit_preamble_counter + 1.U
        }
      }
    }

    is (XmitState.SFD) {
      xmit.io.byte := "b10101011".U
      when (xmit.io.byte_sent === 1.U) {
        xmit_state := XmitState.Data
      }
    }

    is (XmitState.Data) {
      when (xmit_io.transmit === 1.U) {
        xmit.io.byte := xmit_io.byte
        xmit_io.byte_sent := xmit.io.byte_sent
      }
      .otherwise {
        xmit_state := XmitState.CRC0
      }
    }

    // xmit big endian CRC
    is (XmitState.CRC0) {
      val begin = 0 *8
      xmit.io.byte := xmit_crc_out(begin+7,begin)
      when (xmit.io.byte_sent === 1.U) {
        xmit_state := xmit_state.next
      }
    }
    is (XmitState.CRC1) {
      val begin = 1 *8
      xmit.io.byte := xmit_crc_out(begin+7,begin)
      when (xmit.io.byte_sent === 1.U) {
        xmit_state := xmit_state.next
      }
    }
    is (XmitState.CRC2) {
      val begin = 2 *8
      xmit.io.byte := xmit_crc_out(begin+7,begin)
      when (xmit.io.byte_sent === 1.U) {
        xmit_state := xmit_state.next
      }
    }
    is (XmitState.CRC3) {
      val begin = 3 *8
      xmit.io.byte := xmit_crc_out(begin+7,begin)
      when (xmit.io.byte_sent === 1.U) {
        xmit_state := xmit_state.next
      }
    }
  }

  recv_io.abort_frame := io.rmii_r.RXER

  val lastBytes = RegInit(0.U(24.W))
  val frame_end = WireInit(0.U(1.W))
  recv_io.frame_start := RegNext(frame_end)
  val num_recv_by_after_valid_crc = RegInit(0.U(log2Ceil(1600).W))
  when (recv.io.valid_byte === 1.U) {
    val by = Cat(lastBytes, recv.io.byte)

    when (by === recv_crc_out) {
      frame_end := 1.U
      num_recv_by_after_valid_crc := 0.U
    }

    recv_io.valid_byte := 1.U
    recv_io.byte := recv.io.byte

    lastBytes := by
  }
  .elsewhen (recv.io.in_frame === 0.U) {
    lastBytes := 0.U
    when (num_recv_by_after_valid_crc > 0.U) {
      recv_io.abort_frame := 1.U
      num_recv_by_after_valid_crc := 0.U
    }
  }
}
