"""End-to-end verification: run a RISC-V program on both the reference
emulator and the JLS hardware simulation, and compare architectural state.

Usage:  python3 verify.py            # run the built-in program suite
        python3 verify.py -v prog    # verbose single program by name
"""
import os
import sys
import tempfile

from riscv_ref import assemble, RV32I
from build_cpu import build_cpu_circuit
from jlsrun import run_circuit, BUILD

HALF = 1000  # half clock period in sim time units


def gen_clock(steps: int) -> str:
    """A clk waveform with exactly `steps` rising edges (at odd multiples of
    HALF), then held low.  Rising edge j at t=(2j-1)*HALF delimits cycle j."""
    parts = ["clk 0"]
    for k in range(1, 2 * steps + 1):
        parts.append(f"until {k * HALF} {1 if k % 2 == 1 else 0}")
    parts.append("end")
    return " ".join(parts) + "\n"


def run_reference(source: str, dmem_words: int):
    words = assemble(source)
    cpu = RV32I(words, dmem_bytes=dmem_words * 4)
    steps = cpu.run(max_steps=10000)
    return words, cpu, steps


def run_hardware(words, steps, imem_cap, dmem_cap, name="prog"):
    c = build_cpu_circuit(words, imem_cap=imem_cap, dmem_cap=dmem_cap)
    vec = gen_clock(steps)
    vf = os.path.join(BUILD, name + "_clk.txt")
    with open(vf, "w") as f:
        f.write(vec)
    limit = 2 * steps * HALF
    out, err, parsed = run_circuit(c.emit(), name=name, time_limit=limit,
                                   testfile=vf)
    return parsed, err, out


def compare(source: str, name="prog", imem_cap=256, dmem_cap=256, verbose=False):
    words, cpu, steps = run_reference(source, dmem_words=dmem_cap)
    # size imem to hold the program with margin, power-of-two-ish
    need = max(8, 1 << max(3, (len(words) + 2 - 1).bit_length()))
    imem_cap = max(imem_cap, need)
    parsed, err, out = run_hardware(words, steps, imem_cap, dmem_cap, name)

    ref_regs = cpu.dump_regs()               # {i: val} for x1..x31 nonzero-tracked
    hw_regs = {}
    for k, v in parsed["regs"].items():
        # names like "x5" or "PC"
        if k.startswith("x"):
            try:
                hw_regs[int(k[1:])] = v
            except ValueError:
                pass
    ref_mem = cpu.dump_dmem_words()
    hw_mem = parsed["mem"].get("dmem", {})

    problems = []
    for i in range(1, 32):
        r = ref_regs.get(i, 0)
        h = hw_regs.get(i, 0)
        if r != h:
            problems.append(f"x{i}: ref={r:#x} hw={h:#x}")
    for addr, val in ref_mem.items():
        if hw_mem.get(addr, 0) != val:
            problems.append(f"dmem[{addr}]: ref={val:#x} hw={hw_mem.get(addr,0):#x}")
    for addr, val in hw_mem.items():
        if ref_mem.get(addr, 0) != val:
            problems.append(f"dmem[{addr}]: ref={ref_mem.get(addr,0):#x} hw={val:#x} (extra)")

    ok = not problems and parsed["rc"] == 0
    status = "PASS" if ok else "FAIL"
    print(f"[{status}] {name}  ({steps} steps, {len(words)} instrs)")
    if verbose or not ok:
        if err.strip():
            print("  stderr:", err.strip()[:400])
        if problems:
            for p in problems[:40]:
                print("   ", p)
        if verbose:
            print("  ref regs:", {f"x{i}": hex(v) for i, v in sorted(ref_regs.items()) if v})
            print("  hw  regs:", {k: hex(v) for k, v in sorted(hw_regs.items()) if v})
            if ref_mem or hw_mem:
                print("  ref mem:", {a: hex(v) for a, v in sorted(ref_mem.items())})
                print("  hw  mem:", {a: hex(v) for a, v in sorted(hw_mem.items())})
    return ok


PROGRAMS = {
    "addi": """
        addi x1, x0, 5
        addi x2, x0, 10
        addi x3, x1, -3
    """,
    "arith": """
        addi x1, x0, 100
        addi x2, x0, 40
        add  x3, x1, x2
        sub  x4, x1, x2
        and  x5, x1, x2
        or   x6, x1, x2
        xor  x7, x1, x2
    """,
    "logic_imm": """
        addi x1, x0, 0xF0
        andi x2, x1, 0x0F
        ori  x3, x1, 0x0F
        xori x4, x1, 0xFF
    """,
    "shifts": """
        addi x1, x0, 1
        slli x2, x1, 4
        addi x3, x0, -1
        srli x4, x3, 28
        srai x5, x3, 28
        addi x6, x0, 8
        sll  x7, x1, x6
    """,
    "slt": """
        addi x1, x0, -1
        addi x2, x0, 1
        slt  x3, x1, x2
        sltu x4, x1, x2
        slti x5, x1, 0
        sltiu x6, x1, 0
    """,
    "branch_taken": """
        addi x1, x0, 5
        addi x2, x0, 5
        beq  x1, x2, eq
        addi x3, x0, 111
        eq:
        addi x4, x0, 222
    """,
    "branch_nottaken": """
        addi x1, x0, 5
        addi x2, x0, 6
        beq  x1, x2, eq
        addi x3, x0, 111
        eq:
        addi x4, x0, 222
    """,
    "loop_sum": """
        addi x1, x0, 0
        addi x2, x0, 1
        addi x3, x0, 11
        loop:
        add  x1, x1, x2
        addi x2, x2, 1
        blt  x2, x3, loop
    """,
    "lui_auipc": """
        lui   x1, 0x12345
        auipc x2, 0
        addi  x3, x2, 8
    """,
    "jal_ret": """
        jal  x1, func
        addi x5, x0, 99
        beq  x0, x0, done
        func:
        addi x6, x0, 7
        ret
        done:
        addi x7, x0, 55
    """,
    "mem": """
        addi x1, x0, 0x123
        addi x2, x0, 16
        sw   x1, 0(x2)
        lw   x3, 0(x2)
        addi x4, x0, 0x456
        sw   x4, 4(x2)
        lw   x5, 4(x2)
        add  x6, x3, x5
    """,
}


if __name__ == "__main__":
    args = sys.argv[1:]
    verbose = False
    only = None
    if args and args[0] == "-v":
        verbose = True
        args = args[1:]
    if args:
        only = args[0]
    npass = nfail = 0
    for name, src in PROGRAMS.items():
        if only and name != only:
            continue
        if compare(src, name=name, verbose=verbose):
            npass += 1
        else:
            nfail += 1
    print(f"\n{npass} passed, {nfail} failed")
    sys.exit(1 if nfail else 0)
