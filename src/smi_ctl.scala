//-----------------------------------------------------------------------------
// Copyright (C) 2025 Alexander Nutz
//
// license is in the closest parent directory
//-----------------------------------------------------------------------------


package alex_s168_hw

import chisel3._
import chisel3.util._
import chisel3.experimental._
import alex_s168_hw.{UnshiftReg,BidirAssign}

// from view of LAN8720A PHY
class SMI extends Bundle {
  // minimum time between edges: 160ns, minimum cycle time: 400ns (<2.5MHz)
  val MDC = Input(Clock())

  val MDIO = Analog(1.W)
}

class SmiCtlCmd extends Bundle {
  val dev_addr = Input(UInt(5.W))
  val reg_addr = Input(UInt(5.W))

  val is_write = Input(Bool())
  val wr_data = Input(UInt(16.W))
}

class SmiCtl extends Module {
  val io = IO(new Bundle {
    val smi = Flipped(new SMI)

    // any clk, max 2.5 MHz
    // has to be divided from this clock
    val smi_clk = Input(Clock())

    // only ALLOWED to be true for one tick
    // ignored when already processing command
    val has_cmd = Input(Bool())
    val cmd = new SmiCtlCmd

    // valid until next read
    val cmd_done = Output(Bool())
    val cmd_out_data = Output(UInt(16.W))
  })

  io.smi.MDC := io.smi_clk

  val s_has_cmd = withClock (io.smi.MDC) {
    RegInit(0.U(1.W))
  }

  val s_shreg = withClock (io.smi.MDC) {
    Module(new UnshiftReg(32))
  }
  s_shreg.io.load := 0.U
  s_shreg.io.in := 0.U
  s_shreg.io.shift := 0.U

  val s_counter = withClock (io.smi.MDC) {
    Reg(UInt(log2Ceil(64).W))
  }

  val s_read_data = withClock (io.smi.MDC) {
    Reg(UInt(16.W))
  }

  val s_cmd_done = withClock (io.smi.MDC) {
    RegInit(0.U(1.W))
  }

  val s_iswr = withClock (io.smi.MDC) {
    Reg(UInt(1.W))
  }

  io.cmd_done := s_cmd_done
  io.cmd_out_data := s_read_data

  when (io.has_cmd && !s_has_cmd) {
    s_has_cmd := 1.U
    s_counter := 0.U
    s_iswr := io.cmd.is_write

    // opcode
    val opcode = Wire(UInt(2.W))
    when (io.cmd.is_write) {
      opcode := "b01".U
    }.otherwise {
      opcode := "b10".U
    }

    val packet = Cat(
      "b01".U,
      opcode,
      Reverse(io.cmd.dev_addr),
      Reverse(io.cmd.reg_addr),
      "b00".U,
      io.cmd.wr_data
    )

    s_shreg.io.load := 1.U
    s_shreg.io.in := packet
  }

  withClock (io.smi.MDC) {
    val mdio = Module(new BidirAssign)
    mdio.io.is_write := 0.U
    mdio.io.write_v := 0.U
    io.smi.MDIO <> mdio.io.analog

    s_cmd_done := 0.U
    when (s_has_cmd === 1.U) {

      // preamble
      when (s_counter < 32.U) {
        mdio.io.is_write := 1.U
        mdio.io.write_v := 1.U
      }
      // command
      .otherwise {
        s_shreg.io.shift := 1.U
        
        when ((s_iswr === 0.U) && (s_counter >= (32+16).U)) {
          s_read_data := Cat(s_read_data << 1, mdio.io.out_v)
        }
        .otherwise {
          mdio.io.is_write := 1.U
          mdio.io.write_v := s_shreg.io.out
        }
      }

      s_counter := s_counter + 1.U

      when (s_counter === 63.U) {
        s_has_cmd := 0.U
        s_cmd_done := 1.U
      }
    }
  }
}
