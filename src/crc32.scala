//-----------------------------------------------------------------------------
// Copyright (C) 2009 OutputLogic.com
//
// This source file may be used and distributed without restriction
// provided that this copyright statement is not removed from the file
// and that any derivative work contains the original copyright notice
// and the associated disclaimer.
//
// THIS SOURCE FILE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS
// OR IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
// WARRANTIES OF MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.
//-----------------------------------------------------------------------------
// CRC module for data[7:0] ,   crc[31:0]=1+x^1+x^2+x^4+x^5+x^7+x^8+x^10+x^11+x^12+x^16+x^22+x^23+x^26+x^32;
//-----------------------------------------------------------------------------
//
// ported to chisel by https://github.com/whutddk


package alex_s168_hw

import chisel3._
import chisel3.util._

class CRC32_8 extends Module{
  val io = IO(new Bundle {
    val isEnable = Input(Bool())
    val dataIn = Input(UInt(8.W))

    val crc = Output(UInt(32.W))
  })

  val lfsr_c = for (_ <- 0 until 32) yield Wire(Bool())
  val lfsr_q = for (i <- 0 until 32) yield RegEnable( lfsr_c(i), true.B, io.isEnable )
  io.crc := Cat( lfsr_q )

  val dataIn = Wire(UInt(8.W))
  dataIn := Cat(io.dataIn.asBools)

  lfsr_c( 0) := lfsr_q(24) ^ lfsr_q(30) ^ dataIn(0) ^ dataIn(6)
  lfsr_c( 1) := lfsr_q(24) ^ lfsr_q(25) ^ lfsr_q(30) ^ lfsr_q(31) ^ dataIn(0) ^ dataIn(1) ^ dataIn(6) ^ dataIn(7)
  lfsr_c( 2) := lfsr_q(24) ^ lfsr_q(25) ^ lfsr_q(26) ^ lfsr_q(30) ^ lfsr_q(31) ^ dataIn(0) ^ dataIn(1) ^ dataIn(2) ^ dataIn(6) ^ dataIn(7)
  lfsr_c( 3) := lfsr_q(25) ^ lfsr_q(26) ^ lfsr_q(27) ^ lfsr_q(31) ^ dataIn(1) ^ dataIn(2) ^ dataIn(3) ^ dataIn(7)
  lfsr_c( 4) := lfsr_q(24) ^ lfsr_q(26) ^ lfsr_q(27) ^ lfsr_q(28) ^ lfsr_q(30) ^ dataIn(0) ^ dataIn(2) ^ dataIn(3) ^ dataIn(4) ^ dataIn(6)
  lfsr_c( 5) := lfsr_q(24) ^ lfsr_q(25) ^ lfsr_q(27) ^ lfsr_q(28) ^ lfsr_q(29) ^ lfsr_q(30) ^ lfsr_q(31) ^ dataIn(0) ^ dataIn(1) ^ dataIn(3) ^ dataIn(4) ^ dataIn(5) ^ dataIn(6) ^ dataIn(7)
  lfsr_c( 6) := lfsr_q(25) ^ lfsr_q(26) ^ lfsr_q(28) ^ lfsr_q(29) ^ lfsr_q(30) ^ lfsr_q(31) ^ dataIn(1) ^ dataIn(2) ^ dataIn(4) ^ dataIn(5) ^ dataIn(6) ^ dataIn(7)
  lfsr_c( 7) := lfsr_q(24) ^ lfsr_q(26) ^ lfsr_q(27) ^ lfsr_q(29) ^ lfsr_q(31) ^ dataIn(0) ^ dataIn(2) ^ dataIn(3) ^ dataIn(5) ^ dataIn(7)
  lfsr_c( 8) := lfsr_q( 0) ^ lfsr_q(24) ^ lfsr_q(25) ^ lfsr_q(27) ^ lfsr_q(28) ^ dataIn(0) ^ dataIn(1) ^ dataIn(3) ^ dataIn(4)
  lfsr_c( 9) := lfsr_q( 1) ^ lfsr_q(25) ^ lfsr_q(26) ^ lfsr_q(28) ^ lfsr_q(29) ^ dataIn(1) ^ dataIn(2) ^ dataIn(4) ^ dataIn(5)
  lfsr_c(10) := lfsr_q( 2) ^ lfsr_q(24) ^ lfsr_q(26) ^ lfsr_q(27) ^ lfsr_q(29) ^ dataIn(0) ^ dataIn(2) ^ dataIn(3) ^ dataIn(5)
  lfsr_c(11) := lfsr_q( 3) ^ lfsr_q(24) ^ lfsr_q(25) ^ lfsr_q(27) ^ lfsr_q(28) ^ dataIn(0) ^ dataIn(1) ^ dataIn(3) ^ dataIn(4)
  lfsr_c(12) := lfsr_q( 4) ^ lfsr_q(24) ^ lfsr_q(25) ^ lfsr_q(26) ^ lfsr_q(28) ^ lfsr_q(29) ^ lfsr_q(30) ^ dataIn(0) ^ dataIn(1) ^ dataIn(2) ^ dataIn(4) ^ dataIn(5) ^ dataIn(6)
  lfsr_c(13) := lfsr_q( 5) ^ lfsr_q(25) ^ lfsr_q(26) ^ lfsr_q(27) ^ lfsr_q(29) ^ lfsr_q(30) ^ lfsr_q(31) ^ dataIn(1) ^ dataIn(2) ^ dataIn(3) ^ dataIn(5) ^ dataIn(6) ^ dataIn(7)
  lfsr_c(14) := lfsr_q( 6) ^ lfsr_q(26) ^ lfsr_q(27) ^ lfsr_q(28) ^ lfsr_q(30) ^ lfsr_q(31) ^ dataIn(2) ^ dataIn(3) ^ dataIn(4) ^ dataIn(6) ^ dataIn(7)
  lfsr_c(15) := lfsr_q( 7) ^ lfsr_q(27) ^ lfsr_q(28) ^ lfsr_q(29) ^ lfsr_q(31) ^ dataIn(3) ^ dataIn(4) ^ dataIn(5) ^ dataIn(7)
  lfsr_c(16) := lfsr_q( 8) ^ lfsr_q(24) ^ lfsr_q(28) ^ lfsr_q(29) ^ dataIn(0) ^ dataIn(4) ^ dataIn(5)
  lfsr_c(17) := lfsr_q( 9) ^ lfsr_q(25) ^ lfsr_q(29) ^ lfsr_q(30) ^ dataIn(1) ^ dataIn(5) ^ dataIn(6)
  lfsr_c(18) := lfsr_q(10) ^ lfsr_q(26) ^ lfsr_q(30) ^ lfsr_q(31) ^ dataIn(2) ^ dataIn(6) ^ dataIn(7)
  lfsr_c(19) := lfsr_q(11) ^ lfsr_q(27) ^ lfsr_q(31) ^ dataIn(3) ^ dataIn(7)
  lfsr_c(20) := lfsr_q(12) ^ lfsr_q(28) ^ dataIn(4)
  lfsr_c(21) := lfsr_q(13) ^ lfsr_q(29) ^ dataIn(5)
  lfsr_c(22) := lfsr_q(14) ^ lfsr_q(24) ^ dataIn(0)
  lfsr_c(23) := lfsr_q(15) ^ lfsr_q(24) ^ lfsr_q(25) ^ lfsr_q(30) ^ dataIn(0) ^ dataIn(1) ^ dataIn(6)
  lfsr_c(24) := lfsr_q(16) ^ lfsr_q(25) ^ lfsr_q(26) ^ lfsr_q(31) ^ dataIn(1) ^ dataIn(2) ^ dataIn(7)
  lfsr_c(25) := lfsr_q(17) ^ lfsr_q(26) ^ lfsr_q(27) ^ dataIn(2) ^ dataIn(3)
  lfsr_c(26) := lfsr_q(18) ^ lfsr_q(24) ^ lfsr_q(27) ^ lfsr_q(28) ^ lfsr_q(30) ^ dataIn(0) ^ dataIn(3) ^ dataIn(4) ^ dataIn(6)
  lfsr_c(27) := lfsr_q(19) ^ lfsr_q(25) ^ lfsr_q(28) ^ lfsr_q(29) ^ lfsr_q(31) ^ dataIn(1) ^ dataIn(4) ^ dataIn(5) ^ dataIn(7)
  lfsr_c(28) := lfsr_q(20) ^ lfsr_q(26) ^ lfsr_q(29) ^ lfsr_q(30) ^ dataIn(2) ^ dataIn(5) ^ dataIn(6)
  lfsr_c(29) := lfsr_q(21) ^ lfsr_q(27) ^ lfsr_q(30) ^ lfsr_q(31) ^ dataIn(3) ^ dataIn(6) ^ dataIn(7)
  lfsr_c(30) := lfsr_q(22) ^ lfsr_q(28) ^ lfsr_q(31) ^ dataIn(4) ^ dataIn(7)
  lfsr_c(31) := lfsr_q(23) ^ lfsr_q(29) ^ dataIn(5)

}
