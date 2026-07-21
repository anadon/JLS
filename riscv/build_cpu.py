"""build_cpu.py -- construct a single-cycle RV32I CPU as a JLS circuit.

The datapath is fully structural: adders, muxes, barrel shifters, a register
file built from 31 flip-flops and read multiplexers, an instruction ROM and a
data RAM.  The control unit is a decode ROM keyed on (opcode[6:2], funct3,
funct7[5]) that emits a packed control word -- the standard ROM-controlled
decode used in real and teaching CPUs.  Everything is generated from the same
tables as riscv_ref.py so the two agree by construction.

Clocking: a single Clock element drives every flip-flop's rising edge.  On each
rising edge the PC latches the next PC and the written register latches the
write-back value (both computed combinationally from the *previous* cycle, the
usual single-cycle discipline).  Data-memory writes are gated so they commit
only in the clock-low phase, after combinational signals have settled, which
prevents spurious writes to transient addresses.

Verification: the 31 architectural registers and the data RAM are marked
watched, so batch mode prints their final values for comparison against the
reference emulator.
"""

from __future__ import annotations
from typing import List, Dict
from jlsbuild import Circuit, sel_bits, _range_name

# ---- ALU operation codes (must match the mux input order below) ----
ADD, SUB, AND, OR, XOR, SLL, SRL, SRA, SLT, SLTU, PASSB = range(11)

# ---- control-word field layout (LSB first), 16 bits ----
# RegWrite(1) ALUSrc(1) MemRead(1) MemWrite(1) WBSel(2) Branch(1) JumpSel(2)
# ALUctrl(4) ImmSel(3)
def pack_ctrl(RegWrite=0, ALUSrc=0, MemRead=0, MemWrite=0, WBSel=0,
              Branch=0, JumpSel=0, ALUctrl=0, ImmSel=0) -> int:
    v = RegWrite & 1
    v |= (ALUSrc & 1) << 1
    v |= (MemRead & 1) << 2
    v |= (MemWrite & 1) << 3
    v |= (WBSel & 3) << 4
    v |= (Branch & 1) << 6
    v |= (JumpSel & 3) << 7
    v |= (ALUctrl & 0xF) << 9
    v |= (ImmSel & 7) << 13
    return v

# control-word bit fields for the splitter that unpacks the ROM output
CTRL_FIELDS = {
    "RegWrite": (0, 0), "ALUSrc": (1, 1), "MemRead": (2, 2), "MemWrite": (3, 3),
    "WBSel": (5, 4), "Branch": (6, 6), "JumpSel": (8, 7), "ALUctrl": (12, 9),
    "ImmSel": (15, 13),
}

# immediate-format codes
IMM_I, IMM_S, IMM_B, IMM_U, IMM_J = range(5)

# opcode[6:2] values (low 2 bits are always 11 for RV32)
OP52 = {
    "LUI": 0b01101, "AUIPC": 0b00101, "JAL": 0b11011, "JALR": 0b11001,
    "BRANCH": 0b11000, "LOAD": 0b00000, "STORE": 0b01000,
    "OPIMM": 0b00100, "OP": 0b01100,
}

# funct3 -> ALU op (for OP / OP-IMM)
F3_ALU = {0: ADD, 1: SLL, 2: SLT, 3: SLTU, 4: XOR, 5: SRL, 6: OR, 7: AND}


def decode(op52: int, funct3: int, f7b5: int) -> int:
    """Return the packed control word for a decode-ROM index, or 0 (an
    inert NOP with RegWrite=0) for undefined encodings."""
    if op52 == OP52["LUI"]:
        return pack_ctrl(RegWrite=1, ALUSrc=1, ALUctrl=PASSB, ImmSel=IMM_U, WBSel=0)
    if op52 == OP52["AUIPC"]:
        return pack_ctrl(RegWrite=1, ImmSel=IMM_U, WBSel=3)          # PC+imm
    if op52 == OP52["JAL"]:
        return pack_ctrl(RegWrite=1, JumpSel=1, ImmSel=IMM_J, WBSel=2)  # link PC+4
    if op52 == OP52["JALR"]:
        return pack_ctrl(RegWrite=1, ALUSrc=1, ALUctrl=ADD, ImmSel=IMM_I,
                         JumpSel=2, WBSel=2)
    if op52 == OP52["BRANCH"]:
        return pack_ctrl(Branch=1, ImmSel=IMM_B)
    if op52 == OP52["LOAD"]:
        return pack_ctrl(RegWrite=1, ALUSrc=1, ALUctrl=ADD, ImmSel=IMM_I,
                         MemRead=1, WBSel=1)
    if op52 == OP52["STORE"]:
        return pack_ctrl(ALUSrc=1, ALUctrl=ADD, ImmSel=IMM_S, MemWrite=1)
    if op52 == OP52["OPIMM"]:
        alu = F3_ALU[funct3]
        if funct3 == 5 and f7b5:
            alu = SRA
        return pack_ctrl(RegWrite=1, ALUSrc=1, ALUctrl=alu, ImmSel=IMM_I, WBSel=0)
    if op52 == OP52["OP"]:
        alu = F3_ALU[funct3]
        if funct3 == 0 and f7b5:
            alu = SUB
        if funct3 == 5 and f7b5:
            alu = SRA
        return pack_ctrl(RegWrite=1, ALUSrc=0, ALUctrl=alu, ImmSel=0, WBSel=0)
    return 0


def control_rom_text() -> str:
    """9-bit decode ROM: index = (op52<<4)|(funct3<<1)|f7b5."""
    lines = []
    for op52 in range(32):
        for funct3 in range(8):
            for f7b5 in range(2):
                idx = (op52 << 4) | (funct3 << 1) | f7b5
                word = decode(op52, funct3, f7b5)
                if word:
                    lines.append(f"{idx:x} {word:x}")
    return "\\n".join(lines)


# =====================================================================
# datapath construction helpers
# =====================================================================
class CPU:
    def __init__(self, c: Circuit, imem_words: List[int], imem_cap: int,
                 dmem_cap: int, clock_cycle: int = 2000, debug: bool = False):
        self.c = c
        self.debug = debug
        self.imem_cap = imem_cap
        self.dmem_cap = dmem_cap
        self.iabits = sel_bits(imem_cap)
        self.dabits = sel_bits(dmem_cap)
        # clock is an input pin so batch -t vectors can step it deterministically
        self.clk = c.input_pin("clk", 1).out
        self._const = {}
        self.build(imem_words)

    # a shared constant of a given (value,bits); fanned out via one net each use
    def k(self, value: int, bits: int):
        e = self.c.constant(value & ((1 << bits) - 1) if bits else value, bits)
        return e.out

    def lut(self, sel, values: List[int], out_bits: int = 1):
        """A mux with constant inputs = a small lookup table indexed by sel."""
        n = 1 << sel.bits
        m = self.c.mux(n, out_bits)
        self.c.connect(sel, m.p("select"))
        for i in range(n):
            v = values[i] if i < len(values) else 0
            self.c.connect(self.k(v, out_bits), m.p(f"input{i}"))
        return m.out

    def or_reduce(self, port):
        """OR all bits of a bus into a single bit."""
        n = port.bits
        sp = self.c.splitter(n, [(i, i) for i in range(n - 1, -1, -1)])
        self.c.connect(port, sp.p("input"))
        g = self.c.OR(1, n)
        for i in range(n):
            self.c.connect(sp.p(str(i)), g.p(f"input{i}"))
        return g.out

    def zext1(self, bit, width: int):
        """Zero-extend a 1-bit signal to `width` bits."""
        b = self.c.binder(width, [(0, 0), (width - 1, 1)])
        self.c.connect(bit, b.p("0"))
        self.c.connect(self.k(0, width - 1), b.p(f"{width - 1}-1"))
        return b.out

    def slice(self, port, hi: int, lo: int):
        sp = self.c.splitter(port.bits, [(hi, lo)])
        self.c.connect(port, sp.p("input"))
        return sp.p(_range_name(hi, lo))

    def bit(self, port, i: int):
        return self.slice(port, i, i)

    # ---- immediate generator ----
    def immgen(self, instr):
        """Produce the five immediate formats and mux by ImmSel.  Each format
        is assembled with a binder from instruction bit-ranges plus a
        sign-extension of instr[31]."""
        c = self.c
        # sign extension source: instr[31] replicated
        sign = self.bit(instr, 31)

        def ext(width):
            e = c.extend(width)
            c.connect(sign, e.p("input0"))
            return e.out

        # I-immediate: imm[11:0] = instr[31:20]; sign-extend
        #   bits 0..10 = instr[20..30], bit11.. = sign
        i_imm = c.binder(32, [(10, 0), (31, 11)])
        c.connect(self.slice(instr, 30, 20), i_imm.p("10-0"))
        c.connect(ext(21), i_imm.p("31-11"))

        # S-immediate: imm[11:5]=instr[31:25], imm[4:0]=instr[11:7]
        s_imm = c.binder(32, [(4, 0), (10, 5), (31, 11)])
        c.connect(self.slice(instr, 11, 7), s_imm.p("4-0"))
        c.connect(self.slice(instr, 30, 25), s_imm.p("10-5"))
        c.connect(ext(21), s_imm.p("31-11"))

        # B-immediate: imm[12]=instr[31], imm[11]=instr[7], imm[10:5]=instr[30:25],
        #   imm[4:1]=instr[11:8], imm[0]=0
        b_imm = c.binder(32, [(0, 0), (4, 1), (10, 5), (11, 11), (31, 12)])
        c.connect(self.k(0, 1), b_imm.p("0"))
        c.connect(self.slice(instr, 11, 8), b_imm.p("4-1"))
        c.connect(self.slice(instr, 30, 25), b_imm.p("10-5"))
        c.connect(self.bit(instr, 7), b_imm.p("11"))
        c.connect(ext(20), b_imm.p("31-12"))

        # U-immediate: imm[31:12]=instr[31:12], low 12 bits 0
        u_imm = c.binder(32, [(11, 0), (31, 12)])
        c.connect(self.k(0, 12), u_imm.p("11-0"))
        c.connect(self.slice(instr, 31, 12), u_imm.p("31-12"))

        # J-immediate: imm[20]=instr[31], imm[10:1]=instr[30:21], imm[11]=instr[20],
        #   imm[19:12]=instr[19:12], imm[0]=0
        j_imm = c.binder(32, [(0, 0), (10, 1), (11, 11), (19, 12), (20, 20), (31, 21)])
        c.connect(self.k(0, 1), j_imm.p("0"))
        c.connect(self.slice(instr, 30, 21), j_imm.p("10-1"))
        c.connect(self.bit(instr, 20), j_imm.p("11"))
        c.connect(self.slice(instr, 19, 12), j_imm.p("19-12"))
        c.connect(self.bit(instr, 31), j_imm.p("20"))
        c.connect(ext(11), j_imm.p("31-21"))

        m = c.mux(8, 32)
        c.connect(self._immsel, m.p("select"))
        for i, im in enumerate([i_imm.out, s_imm.out, b_imm.out, u_imm.out,
                                j_imm.out]):
            c.connect(im, m.p(f"input{i}"))
        for i in range(5, 8):
            c.connect(self.k(0, 32), m.p(f"input{i}"))
        return m.out

    # ---- register file ----
    def regfile(self, rs1, rs2, rd, write_data, reg_write):
        c = self.c
        # write enable one-hot
        dec = c.decoder(5)
        c.connect(rd, dec.p("input"))
        onehot = dec.out  # 32 bits
        sp = c.splitter(32, [(i, i) for i in range(31, -1, -1)])
        c.connect(onehot, sp.p("input"))

        qs = [self.k(0, 32)]  # x0 = 0
        self.reg_els = []
        for i in range(1, 32):
            we = c.AND(1, 2)
            c.connect(sp.p(str(i)), we.p("input0"))
            c.connect(reg_write, we.p("input1"))
            dmux = c.mux(2, 32)
            c.connect(we.out, dmux.p("select"))
            reg = c.register(f"x{i}", 32, init=0, kind="pff", watch=1)
            c.connect(reg.p("Q"), dmux.p("input0"))     # hold
            c.connect(write_data, dmux.p("input1"))     # load
            c.connect(dmux.out, reg.p("D"))
            c.connect(self.clk, reg.p("C"))
            qs.append(reg.p("Q"))
            self.reg_els.append(reg)

        def read_mux(sel):
            m = c.mux(32, 32)
            c.connect(sel, m.p("select"))
            for i in range(32):
                c.connect(qs[i], m.p(f"input{i}"))
            return m.out
        return read_mux(rs1), read_mux(rs2)

    # ---- ALU ----
    def alu(self, a, b, aluctrl):
        c = self.c
        sub = self.lut(aluctrl, [0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0])  # SUB/SLT/SLTU
        # B xor sub-mask (invert B when subtracting)
        submask = self.c.extend(32)
        c.connect(sub, submask.p("input0"))
        bx = c.XOR(32, 2)
        c.connect(b, bx.p("input0"))
        c.connect(submask.out, bx.p("input1"))
        add = c.adder(32)
        c.connect(a, add.p("A"))
        c.connect(bx.out, add.p("B"))
        c.connect(sub, add.p("Cin"))
        s = add.p("S")
        cout = add.p("Cout")
        # logic
        ag = c.AND(32, 2); c.connect(a, ag.p("input0")); c.connect(b, ag.p("input1"))
        og = c.OR(32, 2);  c.connect(a, og.p("input0")); c.connect(b, og.p("input1"))
        xg = c.XOR(32, 2); c.connect(a, xg.p("input0")); c.connect(b, xg.p("input1"))
        # shifts: amount = b[4:0]
        amt = self.slice(b, 4, 0)
        sll = c.shifter("LogicalLeft", 32);  c.connect(a, sll.p("input")); c.connect(amt, sll.p("amount"))
        srl = c.shifter("LogicalRight", 32); c.connect(a, srl.p("input")); c.connect(amt, srl.p("amount"))
        sra = c.shifter("ArithmeticRight", 32); c.connect(a, sra.p("input")); c.connect(amt, sra.p("amount"))
        # SLT (signed): (a[31]^b[31]) ? a[31] : diff[31]
        asign = self.bit(a, 31); bsign = self.bit(b, 31); dsign = self.bit(s, 31)
        diff = c.XOR(1, 2); c.connect(asign, diff.p("input0")); c.connect(bsign, diff.p("input1"))
        sltmux = c.mux(2, 1)
        c.connect(diff.out, sltmux.p("select"))
        c.connect(dsign, sltmux.p("input0"))
        c.connect(asign, sltmux.p("input1"))
        slt = self.zext1(sltmux.out, 32)
        # SLTU: NOT(cout) (borrow)
        nborrow = c.NOT(1); c.connect(cout, nborrow.p("input0"))
        sltu = self.zext1(nborrow.out, 32)
        # result mux
        m = c.mux(16, 32)
        c.connect(aluctrl, m.p("select"))
        outs = [s, s, ag.out, og.out, xg.out, sll.out, srl.out, sra.out,
                slt, sltu, b]
        for i in range(16):
            c.connect(outs[i] if i < len(outs) else self.k(0, 32), m.p(f"input{i}"))
        return m.out

    # ---- branch comparator on rs1,rs2 ----
    def branch_taken(self, rs1, rs2, funct3, branch):
        c = self.c
        xr = c.XOR(32, 2); c.connect(rs1, xr.p("input0")); c.connect(rs2, xr.p("input1"))
        neq = self.or_reduce(xr.out)
        eq = c.NOT(1); c.connect(neq, eq.p("input0"))
        # rs1 - rs2
        nb = c.NOT(32); c.connect(rs2, nb.p("input0"))
        add = c.adder(32)
        c.connect(rs1, add.p("A")); c.connect(nb.out, add.p("B")); c.connect(self.k(1, 1), add.p("Cin"))
        dsign = self.bit(add.p("S"), 31)
        asign = self.bit(rs1, 31); bsign = self.bit(rs2, 31)
        signdiff = c.XOR(1, 2); c.connect(asign, signdiff.p("input0")); c.connect(bsign, signdiff.p("input1"))
        ltmux = c.mux(2, 1)
        c.connect(signdiff.out, ltmux.p("select"))
        c.connect(dsign, ltmux.p("input0"))
        c.connect(asign, ltmux.p("input1"))
        lt = ltmux.out
        nlt = c.NOT(1); c.connect(lt, nlt.p("input0"))
        ltu = c.NOT(1); c.connect(add.p("Cout"), ltu.p("input0"))
        nltu = c.NOT(1); c.connect(ltu.out, nltu.p("input0"))
        # mux by funct3: [eq, !eq, x, x, lt, !lt, ltu, !ltu]
        m = c.mux(8, 1)
        c.connect(funct3, m.p("select"))
        conds = [eq.out, neq, self.k(0, 1), self.k(0, 1), lt, nlt.out, ltu.out, nltu.out]
        for i in range(8):
            c.connect(conds[i], m.p(f"input{i}"))
        taken = c.AND(1, 2)
        c.connect(branch, taken.p("input0"))
        c.connect(m.out, taken.p("input1"))
        return taken.out

    # =================================================================
    def build(self, imem_words: List[int]):
        c = self.c
        # ---- PC ----
        pc_reg = c.register("PC", 32, init=0, kind="pff", watch=1)
        pc = pc_reg.p("Q")

        # ---- instruction fetch (word-addressed ROM) ----
        init = "\\n".join(f"{i:x} {w:x}" for i, w in enumerate(imem_words))
        irom = c.memory("imem", 32, self.imem_cap, kind="ROM", init=init, time=10)
        iaddr = self.slice(pc, self.iabits + 1, 2)
        c.connect(iaddr, irom.p("address"))
        c.connect(self.k(0, 1), irom.p("CS"))
        c.connect(self.k(0, 1), irom.p("OE"))
        instr = irom.out

        # ---- decode fields ----
        opcode = self.slice(instr, 6, 0)
        op52 = self.slice(instr, 6, 2)
        rd = self.slice(instr, 11, 7)
        funct3 = self.slice(instr, 14, 12)
        rs1 = self.slice(instr, 19, 15)
        rs2 = self.slice(instr, 24, 20)
        f7b5 = self.bit(instr, 30)

        # ---- control ROM ----
        crom = c.memory("ctrl", 32, 512, kind="ROM", init=control_rom_text(), time=5)
        cidx = c.binder(9, [(8, 4), (3, 1), (0, 0)])
        c.connect(op52, cidx.p("8-4"))
        c.connect(funct3, cidx.p("3-1"))
        c.connect(f7b5, cidx.p("0"))
        c.connect(cidx.out, crom.p("address"))
        c.connect(self.k(0, 1), crom.p("CS"))
        c.connect(self.k(0, 1), crom.p("OE"))
        cw = crom.out
        # unpack control fields
        csp = c.splitter(32, [CTRL_FIELDS[n] for n in CTRL_FIELDS])
        c.connect(cw, csp.p("input"))
        ctl = {n: csp.p(_range_name(*CTRL_FIELDS[n])) for n in CTRL_FIELDS}
        self._immsel = ctl["ImmSel"]

        imm = self.immgen(instr)

        # ---- register read (write-back wired later) ----
        # placeholders resolved by building writeback first is circular; instead
        # build regfile with write_data net created now.
        wb_mux = c.mux(4, 32)     # 0=ALU 1=MEM 2=PC+4 3=PCimm
        write_data = wb_mux.out
        rs1v, rs2v = self.regfile(rs1, rs2, rd, write_data, ctl["RegWrite"])

        # ---- ALU ----
        b_op = c.mux(2, 32)
        c.connect(ctl["ALUSrc"], b_op.p("select"))
        c.connect(rs2v, b_op.p("input0"))
        c.connect(imm, b_op.p("input1"))
        alu_out = self.alu(rs1v, b_op.out, ctl["ALUctrl"])

        # ---- data memory ----
        dram = c.memory("dmem", 32, self.dmem_cap, kind="RAM", watch=1, time=10)
        daddr = self.slice(alu_out, self.dabits + 1, 2)
        c.connect(daddr, dram.p("address"))
        c.connect(rs2v, dram.p("input"))
        c.connect(self.k(0, 1), dram.p("CS"))
        c.connect(self.k(0, 1), dram.p("OE"))
        # WE (active low) = NOT(MemWrite AND NOT clk): write only in clk-low phase
        nclk = c.NOT(1); c.connect(self.clk, nclk.p("input0"))
        we_a = c.AND(1, 2); c.connect(ctl["MemWrite"], we_a.p("input0")); c.connect(nclk.out, we_a.p("input1"))
        we_n = c.NOT(1); c.connect(we_a.out, we_n.p("input0"))
        c.connect(we_n.out, dram.p("WE"))
        mem_data = dram.out

        # ---- next PC ----
        pc4 = c.adder(32)
        c.connect(pc, pc4.p("A")); c.connect(self.k(4, 32), pc4.p("B")); c.connect(self.k(0, 1), pc4.p("Cin"))
        pcimm = c.adder(32)
        c.connect(pc, pcimm.p("A")); c.connect(imm, pcimm.p("B")); c.connect(self.k(0, 1), pcimm.p("Cin"))
        # jalr target = alu_out with low bit cleared
        jalr = c.binder(32, [(0, 0), (31, 1)])
        c.connect(self.k(0, 1), jalr.p("0"))
        c.connect(self.slice(alu_out, 31, 1), jalr.p("31-1"))

        taken = self.branch_taken(rs1v, rs2v, funct3, ctl["Branch"])
        jal = self.bit(ctl["JumpSel"], 0)   # JumpSel==1
        jalr_sel = self.bit(ctl["JumpSel"], 1)  # JumpSel==2
        take_pcimm = c.OR(1, 2)
        c.connect(taken, take_pcimm.p("input0"))
        c.connect(jal, take_pcimm.p("input1"))
        n1 = c.mux(2, 32)
        c.connect(take_pcimm.out, n1.p("select"))
        c.connect(pc4.p("S"), n1.p("input0"))
        c.connect(pcimm.p("S"), n1.p("input1"))
        n2 = c.mux(2, 32)
        c.connect(jalr_sel, n2.p("select"))
        c.connect(n1.out, n2.p("input0"))
        c.connect(jalr.out, n2.p("input1"))
        c.connect(n2.out, pc_reg.p("D"))
        c.connect(self.clk, pc_reg.p("C"))

        # ---- write-back mux inputs ----
        c.connect(ctl["WBSel"], wb_mux.p("select"))
        c.connect(alu_out, wb_mux.p("input0"))
        c.connect(mem_data, wb_mux.p("input1"))
        c.connect(pc4.p("S"), wb_mux.p("input2"))
        c.connect(pcimm.p("S"), wb_mux.p("input3"))

        if self.debug:
            def tap(port, nm, bits):
                op = c.output_pin(nm, bits, watch=1)
                c.connect(port, op.p("input"))
            tap(instr, "d_instr", 32)
            tap(cw, "d_cw", 32)
            tap(imm, "d_imm", 32)
            tap(rs1v, "d_rs1", 32)
            tap(alu_out, "d_alu", 32)
            tap(pc4.p("S"), "d_pc4", 32)
            tap(pcimm.p("S"), "d_pcimm", 32)
            tap(n2.out, "d_nextpc", 32)
            tap(write_data, "d_wd", 32)
            tap(ctl["RegWrite"], "d_rw", 1)
            tap(ctl["WBSel"], "d_wbsel", 2)
            tap(ctl["ALUctrl"], "d_actrl", 4)
            tap(b_op.out, "d_bop", 32)


def build_cpu_circuit(imem_words, imem_cap=256, dmem_cap=256, clock_cycle=2000):
    c = Circuit("riscv")
    CPU(c, imem_words, imem_cap, dmem_cap, clock_cycle)
    return c


if __name__ == "__main__":
    import sys
    # smoke: just emit for a tiny program and print element count
    words = [0x00500093, 0x00A00113]  # addi x1,x0,5 ; addi x2,x0,10
    c = build_cpu_circuit(words, imem_cap=8, dmem_cap=8)
    txt = c.emit()
    print(f"elements+wireends emitted; text bytes = {len(txt)}")
    print(f"control ROM entries: {control_rom_text().count(chr(92)+'n')+1}")
