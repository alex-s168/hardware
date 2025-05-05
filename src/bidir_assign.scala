//-----------------------------------------------------------------------------
// Copyright (C) 2025 Alexander Nutz
//
// license is in the closest parent directory
//-----------------------------------------------------------------------------

package alex_s168_hw

import chisel3._
import chisel3.util._
import chisel3.experimental._

class BidirAssign extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val is_write = Input(UInt(1.W))
    val write_v = Input(UInt(1.W))
    val out_v = Output(UInt(1.W))

    val analog = Analog(1.W)
  })
  setInline("BidirAssign.sv",
    """module BidirAssign(
      |  input  logic [0:0] is_write,
      |  input  logic [0:0] write_v,
      |  output logic [0:0] out_v,
      |  inout  wire  [0:0] analog
      |);
      |
      |wire write_enable;
      |assign write_enable = is_write[0];
      |assign analog = write_enable ? write_v : 1'bz;
      |assign out_v = analog;
      |endmodule
    """.stripMargin)
}
