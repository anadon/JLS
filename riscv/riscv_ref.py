#!/usr/bin/env python3
"""
riscv_ref.py -- Golden-reference toolchain for the RV32I base integer ISA.

Pure Python 3 standard library only. No external dependencies.

This module provides two things that together form an executable oracle for
verifying a hardware RV32I implementation:

  Part 1 -- assemble(source, base_addr=0) -> list[int]
      A small two-pass assembler.  It turns RISC-V assembly text into a list of
      32-bit machine-code words (raw encoded instructions, each a Python int in
      the range 0 .. 2**32-1).  The words are the exact bit patterns the CPU
      fetches; on a little-endian machine, word i lives at byte address
      base_addr + 4*i.

  Part 2 -- class RV32I
      A cycle-agnostic functional emulator.  Feed it the assembled words and it
      reproduces the architectural state (registers + data memory) that a
      spec-compliant RV32I core would reach.

Memory model (Harvard split, documented per the task spec)
----------------------------------------------------------
Instructions and data live in SEPARATE address spaces, BOTH starting at byte
address 0:
  * Instruction memory:  read-only, holds the assembled words.  Instruction
    fetch reads word (pc // 4).  Word i occupies byte address 4*i.
  * Data memory:         a flat byte-addressable RAM of `dmem_bytes` bytes,
    little-endian, zero-initialized.  Loads/stores (LB/LH/LW/... , SB/SH/SW)
    target THIS space only.
This mirrors a classic Harvard microarchitecture and keeps code and data from
aliasing, which is convenient for a reference oracle.

All integer state is stored unsigned and masked to 32 bits.  Arithmetic uses
two's-complement wraparound.  x0 always reads as 0 and ignores writes.
"""

from __future__ import annotations

import re

# ---------------------------------------------------------------------------
# Bit / integer helpers
# ---------------------------------------------------------------------------

MASK32 = 0xFFFFFFFF


def _u32(v: int) -> int:
    """Reduce an arbitrary Python int to an unsigned 32-bit value."""
    return v & MASK32


def _sext(value: int, bits: int) -> int:
    """Sign-extend the low `bits` bits of `value` to a signed Python int."""
    value &= (1 << bits) - 1
    sign = 1 << (bits - 1)
    return (value ^ sign) - sign


def _to_signed32(v: int) -> int:
    """Interpret an unsigned 32-bit value as signed two's complement."""
    return _sext(v, 32)


def _fits_signed(value: int, bits: int) -> bool:
    lo = -(1 << (bits - 1))
    hi = (1 << (bits - 1)) - 1
    return lo <= value <= hi


def _fits_unsigned(value: int, bits: int) -> bool:
    return 0 <= value < (1 << bits)


# ---------------------------------------------------------------------------
# Register name table (numeric x0-x31 plus ABI names)
# ---------------------------------------------------------------------------

_REG_ALIASES = {
    "zero": 0, "ra": 1, "sp": 2, "gp": 3, "tp": 4,
    "t0": 5, "t1": 6, "t2": 7,
    "s0": 8, "fp": 8, "s1": 9,
    "a0": 10, "a1": 11, "a2": 12, "a3": 13,
    "a4": 14, "a5": 15, "a6": 16, "a7": 17,
    "s2": 18, "s3": 19, "s4": 20, "s5": 21, "s6": 22, "s7": 23,
    "s8": 24, "s9": 25, "s10": 26, "s11": 27,
    "t3": 28, "t4": 29, "t5": 30, "t6": 31,
}


def _parse_reg(tok: str, line: str) -> int:
    """Resolve a register token (x0..x31 or an ABI name) to its index 0..31."""
    t = tok.strip().lower()
    if t in _REG_ALIASES:
        return _REG_ALIASES[t]
    m = re.fullmatch(r"x(\d+)", t)
    if m:
        n = int(m.group(1))
        if 0 <= n <= 31:
            return n
    raise AssemblerError(f"invalid register '{tok}'", line)


def _parse_imm(tok: str, line: str) -> int:
    """Parse an immediate: decimal, hex (0x...), or negative of either."""
    t = tok.strip()
    try:
        # int(x, 0) handles 0x.. / 0b.. / 0o.. and plain decimal, with sign.
        return int(t, 0)
    except ValueError:
        raise AssemblerError(f"invalid immediate '{tok}'", line)


# ---------------------------------------------------------------------------
# Errors
# ---------------------------------------------------------------------------

class AssemblerError(Exception):
    """Raised on any assembly parse/encoding error; carries the source line."""

    def __init__(self, msg: str, line: str = ""):
        self.msg = msg
        self.line = line
        if line:
            super().__init__(f"{msg}   [in: '{line.strip()}']")
        else:
            super().__init__(msg)


# ---------------------------------------------------------------------------
# Part 1 -- Assembler
# ---------------------------------------------------------------------------
#
# Opcodes and function codes (RV32I).
#
OPC_LUI    = 0b0110111
OPC_AUIPC  = 0b0010111
OPC_JAL    = 0b1101111
OPC_JALR   = 0b1100111
OPC_BRANCH = 0b1100011
OPC_LOAD   = 0b0000011
OPC_STORE  = 0b0100011
OPC_IMM    = 0b0010011
OPC_REG    = 0b0110011

# Instruction format classification for each real mnemonic.
_BRANCH_F3 = {"beq": 0b000, "bne": 0b001, "blt": 0b100,
              "bge": 0b101, "bltu": 0b110, "bgeu": 0b111}
_LOAD_F3   = {"lb": 0b000, "lh": 0b001, "lw": 0b010,
              "lbu": 0b100, "lhu": 0b101}
_STORE_F3  = {"sb": 0b000, "sh": 0b001, "sw": 0b010}
_IMM_F3    = {"addi": 0b000, "slti": 0b010, "sltiu": 0b011,
              "xori": 0b100, "ori": 0b110, "andi": 0b111}
_SHIFT_I   = {"slli": (0b001, 0b0000000),
              "srli": (0b101, 0b0000000),
              "srai": (0b101, 0b0100000)}
_REG_OPS   = {"add": (0b000, 0b0000000), "sub": (0b000, 0b0100000),
              "sll": (0b001, 0b0000000), "slt": (0b010, 0b0000000),
              "sltu": (0b011, 0b0000000), "xor": (0b100, 0b0000000),
              "srl": (0b101, 0b0000000), "sra": (0b101, 0b0100000),
              "or": (0b110, 0b0000000), "and": (0b111, 0b0000000)}

# Full list of supported *real* mnemonics (pseudo-instructions expand to these).
SUPPORTED_MNEMONICS = sorted(
    ["lui", "auipc", "jal", "jalr"]
    + list(_BRANCH_F3)
    + list(_LOAD_F3)
    + list(_STORE_F3)
    + list(_IMM_F3)
    + list(_SHIFT_I)
    + list(_REG_OPS)
)
SUPPORTED_PSEUDO = ["nop", "mv", "li", "j", "ret"]


# --- field encoders (all return a full 32-bit word) -----------------------

def _enc_r(opcode, rd, funct3, rs1, rs2, funct7):
    return _u32((funct7 << 25) | (rs2 << 20) | (rs1 << 15)
                | (funct3 << 12) | (rd << 7) | opcode)


def _enc_i(opcode, rd, funct3, rs1, imm, line):
    if not _fits_signed(imm, 12):
        raise AssemblerError(f"I-type immediate {imm} out of range [-2048,2047]", line)
    return _u32(((imm & 0xFFF) << 20) | (rs1 << 15)
                | (funct3 << 12) | (rd << 7) | opcode)


def _enc_s(opcode, funct3, rs1, rs2, imm, line):
    if not _fits_signed(imm, 12):
        raise AssemblerError(f"S-type immediate {imm} out of range [-2048,2047]", line)
    imm &= 0xFFF
    imm_hi = (imm >> 5) & 0x7F      # bits [11:5]
    imm_lo = imm & 0x1F             # bits [4:0]
    return _u32((imm_hi << 25) | (rs2 << 20) | (rs1 << 15)
                | (funct3 << 12) | (imm_lo << 7) | opcode)


def _enc_b(opcode, funct3, rs1, rs2, imm, line):
    # Branch offsets are signed, 13-bit, and always even (bit 0 == 0).
    if imm & 1:
        raise AssemblerError(f"branch offset {imm} must be even", line)
    if not _fits_signed(imm, 13):
        raise AssemblerError(f"branch offset {imm} out of range [-4096,4094]", line)
    imm &= 0x1FFF
    b12 = (imm >> 12) & 1
    b11 = (imm >> 11) & 1
    b10_5 = (imm >> 5) & 0x3F
    b4_1 = (imm >> 1) & 0xF
    return _u32((b12 << 31) | (b10_5 << 25) | (rs2 << 20) | (rs1 << 15)
                | (funct3 << 12) | (b4_1 << 8) | (b11 << 7) | opcode)


def _enc_u(opcode, rd, imm20, line):
    # imm20 is the raw 20-bit value that occupies bits [31:12].
    if not _fits_unsigned(imm20, 20):
        raise AssemblerError(f"U-type immediate {imm20} out of 20-bit range [0,0xFFFFF]", line)
    return _u32(((imm20 & 0xFFFFF) << 12) | (rd << 7) | opcode)


def _enc_j(opcode, rd, imm, line):
    # Jump offsets are signed, 21-bit, and always even.
    if imm & 1:
        raise AssemblerError(f"jump offset {imm} must be even", line)
    if not _fits_signed(imm, 21):
        raise AssemblerError(f"jump offset {imm} out of range [-1048576,1048574]", line)
    imm &= 0x1FFFFF
    j20 = (imm >> 20) & 1
    j10_1 = (imm >> 1) & 0x3FF
    j11 = (imm >> 11) & 1
    j19_12 = (imm >> 12) & 0xFF
    return _u32((j20 << 31) | (j10_1 << 21) | (j11 << 20) | (j19_12 << 12)
                | (rd << 7) | opcode)


# --- operand parsing helpers ----------------------------------------------

_MEM_RE = re.compile(r"^\s*(.+?)\s*\(\s*(\S+?)\s*\)\s*$")


def _parse_mem(operand: str, line: str):
    """Parse an `imm(rs1)` memory operand -> (imm_int, rs1_index)."""
    m = _MEM_RE.match(operand)
    if not m:
        raise AssemblerError(f"expected imm(reg) memory operand, got '{operand}'", line)
    imm = _parse_imm(m.group(1), line)
    rs1 = _parse_reg(m.group(2), line)
    return imm, rs1


def _split_operands(rest: str):
    """Split the operand portion of an instruction on commas."""
    rest = rest.strip()
    if not rest:
        return []
    return [p.strip() for p in rest.split(",")]


def _li_expansion(imm: int):
    """
    Return the list of (mnemonic, operand-list) real instructions LI expands to.

    LI expansion rule
    -----------------
    * If `imm` fits in a signed 12-bit field (-2048..2047), emit a SINGLE
        ADDI rd, x0, imm.
    * Otherwise emit the canonical two-instruction LUI + ADDI pair:
          LUI  rd, upper20
          ADDI rd, rd, lower12(signed)
      where lower12 is the sign-extended low 12 bits and upper20 is chosen so
      that (upper20 << 12) + lower12 == imm (a +1 carry is folded into upper20
      when the low 12 bits are "negative", i.e. bit 11 is set).
    Caller supplies rd; this helper just decides the shape/immediates.
    Returns a list of tuples: [("addi", [imm]) ] or [("lui",[u]),("addi",[l])].
    """
    imm32 = imm & MASK32  # normalize into the 32-bit value being materialized
    signed = _to_signed32(imm32)
    if _fits_signed(signed, 12):
        return [("li_addi", [signed])]
    lower = imm32 & 0xFFF
    lower_signed = _sext(lower, 12)
    upper = ((imm32 - lower_signed) >> 12) & 0xFFFFF
    return [("li_lui", [upper]), ("li_addi2", [lower_signed])]


# --- the assembler --------------------------------------------------------

class _Item:
    """One expanded, real (non-pseudo) instruction awaiting label resolution."""
    __slots__ = ("mnem", "ops", "line", "addr")

    def __init__(self, mnem, ops, line):
        self.mnem = mnem
        self.ops = ops
        self.line = line
        self.addr = 0  # filled in during layout


def assemble(source: str, base_addr: int = 0) -> list[int]:
    """
    Assemble RISC-V (RV32I) `source` text into a list of 32-bit words.

    Pass 1: tokenize lines, strip comments, record labels -> instruction index,
            and expand pseudo-instructions (whose expansion size is known from
            literal immediates, so label math stays exact).
    Pass 2: assign each item a byte address (base_addr + 4*index) and encode,
            resolving label references for JAL/J/branches.
    """
    items: list[_Item] = []
    labels: dict[str, int] = {}

    # ---- Pass 1: parse + expand -----------------------------------------
    for raw in source.splitlines():
        line = raw
        # strip comments ('#' or '//')
        line = re.sub(r"(#|//).*$", "", line)
        line = line.strip()
        if not line:
            continue

        # Peel off any leading labels ("name:" possibly repeated / with an
        # instruction following on the same line).
        while True:
            m = re.match(r"^([A-Za-z_.$][\w.$]*)\s*:\s*(.*)$", line)
            if not m:
                break
            label = m.group(1)
            if label in labels:
                raise AssemblerError(f"duplicate label '{label}'", raw)
            labels[label] = len(items)  # points at the next instruction index
            line = m.group(2).strip()
            if not line:
                break
        if not line:
            continue

        # Split mnemonic from operands.
        parts = line.split(None, 1)
        mnem = parts[0].lower()
        rest = parts[1] if len(parts) > 1 else ""

        for exp_mnem, exp_ops in _expand(mnem, rest, raw):
            items.append(_Item(exp_mnem, exp_ops, raw))

    # ---- layout: assign addresses ---------------------------------------
    for i, it in enumerate(items):
        it.addr = base_addr + 4 * i

    # Map a label to its byte address.
    def label_addr(name: str, line: str) -> int:
        if name not in labels:
            raise AssemblerError(f"undefined label '{name}'", line)
        return base_addr + 4 * labels[name]

    # ---- Pass 2: encode --------------------------------------------------
    words: list[int] = []
    for it in items:
        words.append(_encode(it, label_addr))
    return words


def _expand(mnem: str, rest: str, raw: str):
    """
    Yield (real_mnemonic, operand_list) tuples for one source instruction.

    Pseudo-instructions expand here; real instructions pass through with their
    operand strings kept intact (label references stay as strings so pass 2 can
    resolve them against the final layout).
    """
    ops = _split_operands(rest)

    if mnem == "nop":
        # NOP == ADDI x0, x0, 0
        yield ("addi", ["x0", "x0", "0"])
        return
    if mnem == "mv":
        if len(ops) != 2:
            raise AssemblerError("MV expects rd, rs1", raw)
        yield ("addi", [ops[0], ops[1], "0"])
        return
    if mnem == "j":
        if len(ops) != 1:
            raise AssemblerError("J expects a single label/offset", raw)
        # J label == JAL x0, label
        yield ("jal", ["x0", ops[0]])
        return
    if mnem == "ret":
        # RET == JALR x0, 0(ra)
        yield ("jalr", ["x0", "0(ra)"])
        return
    if mnem == "li":
        if len(ops) != 2:
            raise AssemblerError("LI expects rd, imm", raw)
        rd = ops[0]
        imm = _parse_imm(ops[1], raw)
        for sub_mnem, sub_ops in _li_expansion(imm):
            if sub_mnem == "li_addi":
                yield ("addi", [rd, "x0", str(sub_ops[0])])
            elif sub_mnem == "li_lui":
                yield ("lui", [rd, str(sub_ops[0])])
            elif sub_mnem == "li_addi2":
                yield ("addi", [rd, rd, str(sub_ops[0])])
        return

    # Not a pseudo -> must be a known real mnemonic.
    if mnem not in SUPPORTED_MNEMONICS:
        raise AssemblerError(f"unknown mnemonic '{mnem}'", raw)
    yield (mnem, ops)


def _encode(it: _Item, label_addr) -> int:
    """Encode one expanded real instruction into a 32-bit word."""
    mnem = it.mnem
    ops = it.ops
    line = it.line

    def reg(i):
        return _parse_reg(ops[i], line)

    def need(n):
        if len(ops) != n:
            raise AssemblerError(f"{mnem.upper()} expects {n} operand(s), got {len(ops)}", line)

    # U-type: LUI / AUIPC  (raw 20-bit immediate goes into bits [31:12])
    if mnem in ("lui", "auipc"):
        need(2)
        rd = reg(0)
        imm20 = _parse_imm(ops[1], line)
        opcode = OPC_LUI if mnem == "lui" else OPC_AUIPC
        return _enc_u(opcode, rd, imm20, line)

    # J-type: JAL rd, label
    if mnem == "jal":
        need(2)
        rd = reg(0)
        target = _resolve_target(ops[1], it, label_addr, line)
        offset = target - it.addr
        return _enc_j(OPC_JAL, rd, offset, line)

    # I-type: JALR rd, imm(rs1)
    if mnem == "jalr":
        need(2)
        rd = reg(0)
        imm, rs1 = _parse_mem(ops[1], line)
        return _enc_i(OPC_JALR, rd, 0b000, rs1, imm, line)

    # B-type: branches rs1, rs2, label
    if mnem in _BRANCH_F3:
        need(3)
        rs1 = reg(0)
        rs2 = reg(1)
        target = _resolve_target(ops[2], it, label_addr, line)
        offset = target - it.addr
        return _enc_b(OPC_BRANCH, _BRANCH_F3[mnem], rs1, rs2, offset, line)

    # I-type loads: rd, imm(rs1)
    if mnem in _LOAD_F3:
        need(2)
        rd = reg(0)
        imm, rs1 = _parse_mem(ops[1], line)
        return _enc_i(OPC_LOAD, rd, _LOAD_F3[mnem], rs1, imm, line)

    # S-type stores: rs2, imm(rs1)
    if mnem in _STORE_F3:
        need(2)
        rs2 = reg(0)
        imm, rs1 = _parse_mem(ops[1], line)
        return _enc_s(OPC_STORE, _STORE_F3[mnem], rs1, rs2, imm, line)

    # I-type ALU: rd, rs1, imm
    if mnem in _IMM_F3:
        need(3)
        rd = reg(0)
        rs1 = reg(1)
        imm = _parse_imm(ops[2], line)
        return _enc_i(OPC_IMM, rd, _IMM_F3[mnem], rs1, imm, line)

    # I-type shift-immediate: rd, rs1, shamt (5-bit)
    if mnem in _SHIFT_I:
        need(3)
        rd = reg(0)
        rs1 = reg(1)
        shamt = _parse_imm(ops[2], line)
        if not (0 <= shamt <= 31):
            raise AssemblerError(f"shift amount {shamt} out of range [0,31]", line)
        funct3, funct7 = _SHIFT_I[mnem]
        # SLLI/SRLI/SRAI encode as I-type with funct7 in the top imm bits.
        imm_field = (funct7 << 5) | shamt
        return _u32(((imm_field & 0xFFF) << 20) | (rs1 << 15)
                    | (funct3 << 12) | (rd << 7) | OPC_IMM)

    # R-type reg ALU: rd, rs1, rs2
    if mnem in _REG_OPS:
        need(3)
        rd = reg(0)
        rs1 = reg(1)
        rs2 = reg(2)
        funct3, funct7 = _REG_OPS[mnem]
        return _enc_r(OPC_REG, rd, funct3, rs1, rs2, funct7)

    raise AssemblerError(f"unhandled mnemonic '{mnem}'", line)


def _resolve_target(tok: str, it: _Item, label_addr, line: str) -> int:
    """
    Resolve a branch/jump target token to an absolute byte address.

    Accepts either a label name or a numeric PC-relative offset (rare, but
    handy).  A numeric token is treated as an offset relative to this
    instruction's address.
    """
    tok = tok.strip()
    if re.fullmatch(r"[+-]?(0[xX][0-9a-fA-F]+|\d+)", tok):
        return it.addr + _parse_imm(tok, line)
    return label_addr(tok, line)


# ---------------------------------------------------------------------------
# Part 2 -- Emulator
# ---------------------------------------------------------------------------

class RV32I:
    """
    Functional RV32I emulator (Harvard split memory; see module docstring).

    Registers are stored unsigned and masked to 32 bits.  x0 reads 0 and
    ignores writes.  Arithmetic is two's-complement with 32-bit wraparound.
    """

    def __init__(self, imem_words: list[int], dmem_bytes: int = 4096, pc: int = 0):
        self.imem = [w & MASK32 for w in imem_words]
        self.dmem = bytearray(dmem_bytes)
        self.regs = [0] * 32
        self.pc = pc & MASK32

    # --- register access ---------------------------------------------------
    def _rd(self, i: int) -> int:
        return self.regs[i] & MASK32 if i != 0 else 0

    def _wr(self, i: int, val: int) -> None:
        if i != 0:
            self.regs[i] = val & MASK32

    # --- data-memory access (little-endian) --------------------------------
    def _load(self, addr: int, width: int, signed: bool) -> int:
        addr &= MASK32
        if addr + width > len(self.dmem):
            raise IndexError(f"load out of data-memory range: addr=0x{addr:08x} width={width}")
        val = 0
        for k in range(width):
            val |= self.dmem[addr + k] << (8 * k)
        if signed:
            val = _sext(val, 8 * width) & MASK32
        return val & MASK32

    def _store(self, addr: int, width: int, val: int) -> None:
        addr &= MASK32
        if addr + width > len(self.dmem):
            raise IndexError(f"store out of data-memory range: addr=0x{addr:08x} width={width}")
        for k in range(width):
            self.dmem[addr + k] = (val >> (8 * k)) & 0xFF

    # --- instruction field decoders ---------------------------------------
    @staticmethod
    def _fields(w: int):
        opcode = w & 0x7F
        rd = (w >> 7) & 0x1F
        funct3 = (w >> 12) & 0x7
        rs1 = (w >> 15) & 0x1F
        rs2 = (w >> 20) & 0x1F
        funct7 = (w >> 25) & 0x7F
        return opcode, rd, funct3, rs1, rs2, funct7

    @staticmethod
    def _imm_i(w):
        return _sext((w >> 20) & 0xFFF, 12)

    @staticmethod
    def _imm_s(w):
        imm = ((w >> 25) & 0x7F) << 5 | ((w >> 7) & 0x1F)
        return _sext(imm, 12)

    @staticmethod
    def _imm_b(w):
        imm = (((w >> 31) & 1) << 12
               | ((w >> 7) & 1) << 11
               | ((w >> 25) & 0x3F) << 5
               | ((w >> 8) & 0xF) << 1)
        return _sext(imm, 13)

    @staticmethod
    def _imm_u(w):
        return w & 0xFFFFF000  # bits [31:12], already shifted into place

    @staticmethod
    def _imm_j(w):
        imm = (((w >> 31) & 1) << 20
               | ((w >> 12) & 0xFF) << 12
               | ((w >> 20) & 1) << 11
               | ((w >> 21) & 0x3FF) << 1)
        return _sext(imm, 21)

    # --- single-step execution --------------------------------------------
    def step(self) -> None:
        """Execute exactly one instruction at self.pc and advance self.pc."""
        idx = self.pc // 4
        if self.pc & 3:
            raise ValueError(f"misaligned PC 0x{self.pc:08x}")
        if idx >= len(self.imem):
            raise IndexError(f"PC 0x{self.pc:08x} outside instruction memory")
        w = self.imem[idx]
        opcode, rd, funct3, rs1, rs2, funct7 = self._fields(w)
        next_pc = _u32(self.pc + 4)

        if opcode == OPC_LUI:
            self._wr(rd, self._imm_u(w))

        elif opcode == OPC_AUIPC:
            self._wr(rd, _u32(self.pc + self._imm_u(w)))

        elif opcode == OPC_JAL:
            self._wr(rd, next_pc)
            next_pc = _u32(self.pc + self._imm_j(w))

        elif opcode == OPC_JALR:
            target = _u32(self._rd(rs1) + self._imm_i(w))
            target &= ~1 & MASK32          # clear low bit per spec
            self._wr(rd, next_pc)
            next_pc = target

        elif opcode == OPC_BRANCH:
            a = self._rd(rs1)
            b = self._rd(rs2)
            sa = _to_signed32(a)
            sb = _to_signed32(b)
            taken = {
                0b000: a == b,            # BEQ
                0b001: a != b,            # BNE
                0b100: sa < sb,           # BLT  (signed)
                0b101: sa >= sb,          # BGE  (signed)
                0b110: a < b,             # BLTU (unsigned)
                0b111: a >= b,            # BGEU (unsigned)
            }.get(funct3)
            if taken is None:
                raise ValueError(f"illegal branch funct3={funct3:03b}")
            if taken:
                next_pc = _u32(self.pc + self._imm_b(w))

        elif opcode == OPC_LOAD:
            addr = _u32(self._rd(rs1) + self._imm_i(w))
            if funct3 == 0b000:   result = self._load(addr, 1, True)   # LB
            elif funct3 == 0b001: result = self._load(addr, 2, True)   # LH
            elif funct3 == 0b010: result = self._load(addr, 4, False)  # LW
            elif funct3 == 0b100: result = self._load(addr, 1, False)  # LBU
            elif funct3 == 0b101: result = self._load(addr, 2, False)  # LHU
            else:
                raise ValueError(f"illegal load funct3={funct3:03b}")
            self._wr(rd, result)

        elif opcode == OPC_STORE:
            addr = _u32(self._rd(rs1) + self._imm_s(w))
            val = self._rd(rs2)
            if funct3 == 0b000:   self._store(addr, 1, val)  # SB
            elif funct3 == 0b001: self._store(addr, 2, val)  # SH
            elif funct3 == 0b010: self._store(addr, 4, val)  # SW
            else:
                raise ValueError(f"illegal store funct3={funct3:03b}")

        elif opcode == OPC_IMM:
            a = self._rd(rs1)
            imm = self._imm_i(w)
            if funct3 == 0b000:   res = _u32(a + imm)                          # ADDI
            elif funct3 == 0b010: res = 1 if _to_signed32(a) < imm else 0      # SLTI
            elif funct3 == 0b011: res = 1 if a < (imm & MASK32) else 0         # SLTIU
            elif funct3 == 0b100: res = a ^ (imm & MASK32)                     # XORI
            elif funct3 == 0b110: res = a | (imm & MASK32)                     # ORI
            elif funct3 == 0b111: res = a & (imm & MASK32)                     # ANDI
            elif funct3 == 0b001:                                             # SLLI
                res = _u32(a << (imm & 0x1F))
            elif funct3 == 0b101:                                             # SRLI / SRAI
                shamt = imm & 0x1F
                if (w >> 30) & 1:   # funct7 bit 5 set -> arithmetic
                    res = _u32(_to_signed32(a) >> shamt)
                else:
                    res = a >> shamt
            else:
                raise ValueError(f"illegal imm-alu funct3={funct3:03b}")
            self._wr(rd, res)

        elif opcode == OPC_REG:
            a = self._rd(rs1)
            b = self._rd(rs2)
            shamt = b & 0x1F
            if funct3 == 0b000:
                res = _u32(a - b) if funct7 == 0b0100000 else _u32(a + b)  # SUB / ADD
            elif funct3 == 0b001:
                res = _u32(a << shamt)                                     # SLL
            elif funct3 == 0b010:
                res = 1 if _to_signed32(a) < _to_signed32(b) else 0        # SLT
            elif funct3 == 0b011:
                res = 1 if a < b else 0                                    # SLTU
            elif funct3 == 0b100:
                res = a ^ b                                                # XOR
            elif funct3 == 0b101:
                if funct7 == 0b0100000:
                    res = _u32(_to_signed32(a) >> shamt)                   # SRA
                else:
                    res = a >> shamt                                       # SRL
            elif funct3 == 0b110:
                res = a | b                                                # OR
            elif funct3 == 0b111:
                res = a & b                                                # AND
            else:
                raise ValueError(f"illegal reg-alu funct3={funct3:03b}")
            self._wr(rd, res)

        else:
            raise ValueError(f"illegal/unsupported opcode 0x{opcode:02x} "
                             f"(instr 0x{w:08x} at pc 0x{self.pc:08x})")

        self.regs[0] = 0  # enforce x0 == 0 invariant
        self.pc = next_pc

    # --- run loop ----------------------------------------------------------
    def run(self, max_steps: int = 10000) -> int:
        """
        Step until one of:
          * PC runs off the end of instruction memory (pc >= 4*len(imem)),
          * an all-zero instruction word (0x00000000) is fetched (halt
            sentinel), or
          * max_steps is reached.
        Returns the number of instructions executed.
        """
        steps = 0
        while steps < max_steps:
            idx = self.pc // 4
            if self.pc >= 4 * len(self.imem):
                break
            if self.imem[idx] == 0x00000000:  # halt sentinel
                break
            self.step()
            steps += 1
        return steps

    # --- state dumps -------------------------------------------------------
    def dump_regs(self) -> dict:
        """Return {reg_index: unsigned_value} for x1..x31 (x0 skipped)."""
        return {i: self.regs[i] & MASK32 for i in range(1, 32)}

    def dump_dmem_words(self) -> dict:
        """Return {word_address: value} for every non-zero 4-byte data word."""
        out = {}
        for base in range(0, len(self.dmem) - 3, 4):
            val = (self.dmem[base]
                   | self.dmem[base + 1] << 8
                   | self.dmem[base + 2] << 16
                   | self.dmem[base + 3] << 24)
            if val != 0:
                out[base // 4] = val
        return out


# ---------------------------------------------------------------------------
# Convenience helper
# ---------------------------------------------------------------------------

def assemble_and_run(source: str, dmem_bytes: int = 4096, max_steps: int = 10000) -> RV32I:
    """Assemble `source`, run it to completion, and return the RV32I instance."""
    words = assemble(source)
    cpu = RV32I(words, dmem_bytes=dmem_bytes)
    cpu.run(max_steps=max_steps)
    return cpu


# ---------------------------------------------------------------------------
# Part 3 -- Self-test
# ---------------------------------------------------------------------------

def _selftest():
    # --- arithmetic: ADDI / ADD / SUB -----------------------------------
    cpu = assemble_and_run("""
        addi t0, zero, 100
        addi t1, zero, 40
        add  t2, t0, t1      # 140
        sub  t3, t0, t1      # 60
        addi t4, t0, -150    # -50 -> 0xFFFFFFCE
    """)
    r = cpu.dump_regs()
    assert r[5] == 100, r[5]
    assert r[6] == 40, r[6]
    assert r[7] == 140, r[7]
    assert r[28] == 60, r[28]
    assert r[29] == _u32(-50), hex(r[29])

    # --- logic: AND / OR / XOR / ANDI -----------------------------------
    cpu = assemble_and_run("""
        li   t0, 0x0F0F
        li   t1, 0x00FF
        and  t2, t0, t1      # 0x000F
        or   t3, t0, t1      # 0x0FFF
        xor  t4, t0, t1      # 0x0FF0
        andi t5, t0, 0x0F0   # 0x000
    """)
    r = cpu.dump_regs()
    assert r[7] == 0x000F, hex(r[7])
    assert r[28] == 0x0FFF, hex(r[28])
    assert r[29] == 0x0FF0, hex(r[29])
    assert r[30] == 0x0000, hex(r[30])

    # --- shifts: SLL / SRL / SRA / SLLI / SRAI (with sign) --------------
    cpu = assemble_and_run("""
        li   t0, 1
        li   t1, 4
        sll  t2, t0, t1       # 1 << 4 = 16
        li   t3, -16          # 0xFFFFFFF0
        li   t4, 2
        srl  t5, t3, t4       # logical: 0x3FFFFFFC
        sra  t6, t3, t4       # arithmetic: -4 = 0xFFFFFFFC
        slli s0, t0, 31       # 0x80000000
        srai s1, s0, 31       # arithmetic shift of 0x80000000 by 31 = -1
    """)
    r = cpu.dump_regs()
    assert r[7] == 16, hex(r[7])
    assert r[30] == 0x3FFFFFFC, hex(r[30])
    assert r[31] == _u32(-4), hex(r[31])
    assert r[8] == 0x80000000, hex(r[8])
    assert r[9] == _u32(-1), hex(r[9])

    # --- SLT / SLTU signed vs unsigned ----------------------------------
    cpu = assemble_and_run("""
        li   t0, -1           # 0xFFFFFFFF
        li   t1, 1
        slt  t2, t0, t1       # signed: -1 < 1 -> 1
        sltu t3, t0, t1       # unsigned: 0xFFFFFFFF < 1 -> 0
        slti t4, t0, 0        # signed: -1 < 0 -> 1
        sltiu t5, t0, 1       # unsigned: huge < 1 -> 0
    """)
    r = cpu.dump_regs()
    assert r[7] == 1, r[7]
    assert r[28] == 0, r[28]
    assert r[29] == 1, r[29]
    assert r[30] == 0, r[30]

    # --- LUI / AUIPC -----------------------------------------------------
    # LUI loads 0x12345 into bits [31:12] -> 0x12345000.
    # AUIPC at instruction index 1 (byte addr 4): pc + (imm<<12).
    cpu = assemble_and_run("""
        lui   t0, 0x12345      # 0x12345000
        auipc t1, 0x1          # pc(=4) + 0x1000 = 0x1004
    """)
    r = cpu.dump_regs()
    assert r[5] == 0x12345000, hex(r[5])
    assert r[6] == 0x00001004, hex(r[6])

    # --- taken and not-taken branch -------------------------------------
    # BEQ taken skips the "addi a0, zero, 111"; BNE not-taken falls through.
    cpu = assemble_and_run("""
        li   t0, 5
        li   t1, 5
        beq  t0, t1, taken     # equal -> taken
        li   a0, 111           # should be skipped
    taken:
        li   a1, 222
        li   t2, 7
        bne  t0, t2, nottaken_target_wrong   # 5 != 7 -> taken branch...
        li   a2, 333           # skipped because bne taken
    nottaken_target_wrong:
        li   a3, 444
    """)
    r = cpu.dump_regs()
    assert r[10] == 0, ("a0 should stay 0 (branch taken skipped it)", r[10])
    assert r[11] == 222, r[11]
    assert r[12] == 0, ("a2 skipped", r[12])
    assert r[13] == 444, r[13]

    # Explicit not-taken case: BEQ with unequal operands falls through.
    cpu = assemble_and_run("""
        li   t0, 1
        li   t1, 2
        beq  t0, t1, skip      # not equal -> NOT taken
        li   a0, 99            # executes
    skip:
        li   a1, 100
    """)
    r = cpu.dump_regs()
    assert r[10] == 99, r[10]
    assert r[11] == 100, r[11]

    # --- JAL / JALR call & return ---------------------------------------
    # main: set a0=10, call double (a0 = a0*... here a0+a0), then stop.
    cpu = assemble_and_run("""
        li   a0, 10
        jal  ra, dbl          # call; ra = return addr
        li   a1, 777          # runs after return
        j    done
    dbl:
        add  a0, a0, a0       # a0 = 20
        ret                   # jalr x0, 0(ra)
    done:
        li   a2, 1
    """)
    r = cpu.dump_regs()
    assert r[10] == 20, r[10]
    assert r[11] == 777, r[11]
    assert r[12] == 1, r[12]

    # --- loop: sum 1..10 into a register --------------------------------
    cpu = assemble_and_run("""
        li   t0, 0            # accumulator
        li   t1, 1            # i
        li   t2, 11           # limit (exclusive)
    loop:
        bge  t1, t2, endloop  # while i < 11
        add  t0, t0, t1       # acc += i
        addi t1, t1, 1        # i++
        j    loop
    endloop:
        mv   a0, t0
    """)
    r = cpu.dump_regs()
    assert r[5] == 55, r[5]
    assert r[10] == 55, r[10]

    # --- LW / SW round-trip through data memory -------------------------
    cpu = assemble_and_run("""
        li   t0, 0xDEADBEEF
        li   t1, 16           # data address
        sw   t0, 0(t1)        # store word at dmem[16]
        lw   t2, 0(t1)        # load it back
        li   t3, 0x1234
        sh   t3, 8(t1)        # store halfword at dmem[24]
        lhu  t4, 8(t1)        # load halfword unsigned
        li   t5, 0x7F
        sb   t5, 4(t1)        # store byte at dmem[20]
        lb   t6, 4(t1)        # load byte signed
    """)
    r = cpu.dump_regs()
    assert r[7] == 0xDEADBEEF, hex(r[7])       # loaded word matches stored
    assert r[29] == 0x1234, hex(r[29])          # halfword
    assert r[31] == 0x7F, hex(r[31])            # byte
    dm = cpu.dump_dmem_words()
    assert dm[16 // 4] == 0xDEADBEEF, dm         # word at byte addr 16 -> word idx 4
    assert dm[24 // 4] == 0x00001234, dm         # halfword store
    assert dm[20 // 4] == 0x0000007F, dm         # byte store

    # --- signed byte load sign-extension --------------------------------
    cpu = assemble_and_run("""
        li   t1, 32
        li   t0, 0x80         # -128 as a byte
        sb   t0, 0(t1)
        lb   t2, 0(t1)        # sign-extended -> 0xFFFFFF80
        lbu  t3, 0(t1)        # zero-extended -> 0x00000080
    """)
    r = cpu.dump_regs()
    assert r[7] == _u32(-128), hex(r[7])
    assert r[28] == 0x80, hex(r[28])

    # --- LI single-ADDI vs LUI+ADDI expansion ---------------------------
    # Small immediate -> single ADDI (1 word).
    assert len(assemble("li a0, 5")) == 1
    # Large immediate -> LUI + ADDI (2 words).
    assert len(assemble("li a0, 0x12345")) == 2
    # Verify the large expansion actually materializes the right value.
    cpu = assemble_and_run("li a0, 0x12345678")
    assert cpu.dump_regs()[10] == 0x12345678, hex(cpu.dump_regs()[10])
    cpu = assemble_and_run("li a0, -100000")
    assert cpu.dump_regs()[10] == _u32(-100000), hex(cpu.dump_regs()[10])

    print("ALL REFERENCE TESTS PASSED")


if __name__ == "__main__":
    _selftest()
