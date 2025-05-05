//-----------------------------------------------------------------------------
// Copyright (C) 2025 Alexander Nutz
//
// license is in the closest parent directory
//-----------------------------------------------------------------------------


package alex_s168_hw

import chisel3._
import chisel3.util._

// from view of MAC
class RmiiT extends Bundle {
  val TXD = Output(UInt(2.W))
  val TXEN = Output(UInt(1.W))
}

// from view of MAC
class RmiiR extends Bundle {
  val RXD = Input(UInt(2.W))
  val RXER = Input(UInt(1.W))
  val CRS_DV = Input(UInt(1.W))
}

// needs to be clocked at RMII speed (50 MHz)
//
// the outputted data is without preamble and SFD
class RmiiRecv extends Module {
  val io = IO(new Bundle {
    val rmii_r = new RmiiR()

    // have to reset both MAC and PHY when this changes
    val is10mbits = Input(UInt(1.W))

    val in_frame = Output(UInt(1.W))

    // when this is true, byte has to be consumed immediately
    // at most every 4th clock cycle true
    val valid_byte = Output(UInt(1.W))
    val byte = Output(UInt(8.W))

    // only on for one cycle
    val info_carrier_lost_during_packet = Output(UInt(1.W))
  })

  io.valid_byte := 0.U
  io.info_carrier_lost_during_packet := 0.U

  val byte_shift_reg = RegInit(0.U(10.W))
  
  val bit_unaligned = RegInit(0.U(1.W))
  when (bit_unaligned === 1.U) {
    io.byte := byte_shift_reg(8,1)
  }.otherwise {
    io.byte := byte_shift_reg(9,2)
  }

  val valid_frame = RegInit(0.U(1.W))
  io.in_frame := valid_frame
  val dibit_counter = RegInit(0.U(2.W))

  val last_crs_dv = RegNext(io.rmii_r.CRS_DV)

  val (counter10mbits, _) = Counter(true.B, 5)

  val data_this_cycle = Wire(UInt(1.W))
  when (io.is10mbits === 1.U) {
    data_this_cycle := counter10mbits === 0.U
  }.otherwise {
    data_this_cycle := 1.U
  }

  when (data_this_cycle === 1.U) {
    byte_shift_reg := Cat(io.rmii_r.RXD, byte_shift_reg)
    when (io.rmii_r.CRS_DV === 1.U) {
      when (valid_frame === 1.U) {
        when (dibit_counter === 0.U) {
          io.valid_byte := 1.U
        }
        dibit_counter := dibit_counter + 1.U
      }
      .otherwise {
        when (byte_shift_reg === BitPat("b10101011??")) {
          dibit_counter := 3.U
          valid_frame := 1.U
          bit_unaligned := 0.U
        }

        when (byte_shift_reg === BitPat("b?10101011?")) {
          dibit_counter := 3.U
          valid_frame := 1.U
          bit_unaligned := 1.U
        }
      }
    }
    .otherwise {
      // carrier lost, but phy still has data
      when (last_crs_dv === 1.U) {
        io.info_carrier_lost_during_packet := 1.U
      }
      // frame end
      .otherwise {
        valid_frame := 0.U
        dibit_counter := 0.U
        bit_unaligned := 0.U
      }
    }
  }
}

// needs to be clocked at RMII speed (50 MHz)
//
// the input data HAS TO contain preamble and SFD
class RmiiTransmit extends Module {
  val io = IO(new Bundle {
    val rmii_t = new RmiiT()

    // have to reset both MAC and PHY when this changes
    val is10mbits = Input(UInt(1.W))

    // needs to be hold high while transmitting
    val tx_frame = Input(UInt(1.W))

    val byte = Input(UInt(8.W))
    // at fastest every 4th RMII clock
    val byte_sent = Output(UInt(1.W))
  })

  io.rmii_t.TXD := 0.U
  io.byte_sent := 0.U

  io.rmii_t.TXEN := io.tx_frame

  val (counter10mbits, _) = Counter(true.B, 5)

  val data_this_cycle = Wire(UInt(1.W))
  when (io.is10mbits === 1.U) {
    data_this_cycle := counter10mbits === 0.U
  }.otherwise {
    data_this_cycle := 1.U
  }

  val (dibit_counter, _) = Counter(data_this_cycle === 1.U, 4)

  val byte_rem = Reg(UInt(6.W))

  when (data_this_cycle === 1.U) {
    val dibit = Wire(UInt(2.W))
    when (dibit_counter === 0.U) {
      dibit := io.byte
      byte_rem := io.byte >> 2
    }
    .otherwise {
      dibit := byte_rem(1,0)
      byte_rem := byte_rem >> 2
    }

    when (dibit_counter === 3.U)  {
      io.byte_sent := 1.U
    }

    io.rmii_t.TXD := dibit
  }
}
