package riscv
import chisel3._
class Core extends Module{
  val io = IO(new Bundle{
    //val input = Output(UInt(32.W))
    //val pc = Output(UInt(32.W))
    val instruction = Output(UInt(32.W))
    val AluOut = Output(SInt(32.W))
    val branchCheck = Output(UInt(1.W))
  })
  val control = Module(new Control)
  val imm = Module(new immGen)
  val aluCtrl = Module(new AluControl)
  val alu = Module(new Alu)
  val reg = Module(new registerFile)
  val InsMem = Module(new InsMem)
  val PC = Module(new PC)
  val jalr = Module(new JalrTarget)
  val dataMem = Module(new DataMem)

  //Jalr Target Connection
  when(control.io.extendSel === 0.U){
    jalr.io.imm := imm.io.i
  }.elsewhen(control.io.extendSel === 2.U){
    jalr.io.imm := imm.io.S
  }.elsewhen(control.io.extendSel === 1.U){
    jalr.io.imm := imm.io.u
  }.otherwise{
    jalr.io.imm := DontCare
  }
  jalr.io.rs1 := reg.io.rs1

  //Pc Connection
  PC.io.input := PC.io.pc4
  //io.pc := PC.io.pc(11,2)

  //Instruction Memory Connection
  InsMem.io.wrAddr := PC.io.pc(11,2)
  io.instruction := InsMem.io.rdData

  //Control Connection
  control.io.opcode := io.instruction(6,0) //InsMem.io.rdData(6,0)

  //Immediate Generation Connection
  imm.io.ins := io.instruction
  imm.io.pc :=  PC.io.pc

  //RegisterFile Connection
  reg.io.rs1_sel := io.instruction(19,15)
  reg.io.rs2_sel := io.instruction(24,20)
  reg.io.rd_sel := io.instruction(11,7)
  reg.io.regWrite := control.io.regWrite

  //Alu Control Connection
  aluCtrl.io.ALUop := control.io.aluOp
  aluCtrl.io.func3 := io.instruction(14,12)
  aluCtrl.io.func7 := io.instruction(30)

  //ALU Connection
  when(control.io.oprA === 0.U || control.io.oprA === 3.U){
    alu.io.a := reg.io.rs1
  }.elsewhen(control.io.oprA === 2.U){
    alu.io.a := PC.io.pc4.asSInt //+ 4.U).asSInt
  }.otherwise{
    alu.io.a := DontCare
  }

  when(control.io.oprB === 0.U){
    alu.io.b := reg.io.rs2
  }.elsewhen(control.io.oprB === 1.U){
    when(control.io.extendSel === 0.U){
      alu.io.b := imm.io.i
    }.elsewhen(control.io.extendSel === 2.U){
      alu.io.b := imm.io.S
    }.elsewhen(control.io.extendSel === 1.U){
      alu.io.b := imm.io.u
    }.otherwise{
      alu.io.b := DontCare
    }
  }.otherwise{
    alu.io.b := DontCare
  }

  alu.io.aluControl := aluCtrl.io.out
  io.AluOut := alu.io.aluOut
  io.branchCheck := alu.io.branch

  //Data Memory Connection
  dataMem.io.store := control.io.memWrite
  dataMem.io.load := control.io.memRead
  dataMem.io.addrr := alu.io.aluOut(9,2).asUInt
  dataMem.io.storedata := reg.io.rs2
  when(control.io.memToReg === 0.U){
    reg.io.writeData := io.AluOut
  }.elsewhen(control.io.memToReg === 1.U){
    reg.io.writeData := dataMem.io.dataOut
  }.otherwise{
    reg.io.writeData := dataMem.io.dataOut
  }

  //when(control.io.memToReg === 1.U){
  //reg.io.writeData := io.AluOut
  //}.otherwise{
  //  reg.io.writeData := DontCare
  //}

  when((io.branchCheck  & control.io.branch) === 1.U && control.io.nextPcSel === 1.U){
    PC.io.input := imm.io.sb.asUInt
  }.elsewhen((io.branchCheck  & control.io.branch) === 0.U && control.io.nextPcSel === 1.U){
    PC.io.input := PC.io.pc4
  }.elsewhen(control.io.nextPcSel === 0.U){
    PC.io.input := PC.io.pc4
  }.elsewhen(control.io.nextPcSel === 2.U){
    PC.io.input := imm.io.uj.asUInt
  }.elsewhen(control.io.nextPcSel === 3.U){
    PC.io.input := jalr.io.out.asUInt
  }.otherwise{
    PC.io.input := DontCare
  }

}
