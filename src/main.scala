//-----------------------------------------------------------------------------
// Copyright (C) 2025 Alexander Nutz
//
// license is in the closest parent directory
//-----------------------------------------------------------------------------

//> using scala "2.13.12"
//> using dep "org.chipsalliance::chisel:6.7.0"
//> using plugin "org.chipsalliance:::chisel-plugin:6.7.0"
//> using options "-unchecked", "-deprecation", "-language:reflectiveCalls", "-feature", "-Xcheckinit", "-Xfatal-warnings", "-Ywarn-dead-code", "-Ywarn-unused", "-Ymacro-annotations"

package alex_s168_hw

import chisel3._
import chisel3.experimental._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
import _root_.circt.stage.ChiselStage
import alex_s168_hw._

// from view of extension card
class X16Ext extends Bundle {
  val PHI2 = Input(Clock())

  // bus enable
  val BE = Analog(1.W)

  // connected to RWB
  // when this is high, the CPU is reading from mem
  // only valid when BE
  val read = Analog(1.W)

  // set this to low when interrupt, and not already low (pulled up)
  // unset only after interrupt processed
  val IRQB = Analog(1.W)

  // see https://github.com/X16Community/x16-docs/blob/master/X16%20Reference%20-%2008%20-%20Memory%20Map.md
  val addr = Analog(16.W)

  val data = Analog(8.W)

  // processor halted while low
  val RDY = Analog(1.W)

  // processor resets for two cycles when low
  // need to check for this when doing mem copy
  val notRES = Input(UInt(1.W))
}

// from view of FPGA
//
// notCE on the RAM chip is always low, so it doesn't power down
// notBHE and notBLE is always low
//
class ExternalRam(addr_width: Int) extends Bundle {
  val addr = Output(UInt(addr_width.W))
  val data = Analog(16.W)

  val notOE = Output(Bool())
  val notWE = Output(Bool())
}

object Main extends App {
  ChiselStage.emitSystemVerilog(
    gen = new MAC(true),
    firtoolOpts = Array(
      "-disable-all-randomization",
      //"-strip-debug-info",
      "--split-verilog",
      "-o=build"
    )
  )
}
