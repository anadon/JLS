"""Randomized differential test: generate random RV32I programs, run each on
both the reference emulator and the JLS hardware, and require identical final
architectural state.  JLS runs are a few seconds each, so programs are checked
concurrently in a thread pool (each is an independent java subprocess).

Usage: python3 fuzz_diff.py [num_programs] [seed] [workers]
"""
import os
import random
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed

from riscv_ref import assemble, RV32I
from build_cpu import build_cpu_circuit
from jlsrun import run_circuit, BUILD
from verify import gen_clock, HALF

DMEM_WORDS = 64      # data memory words (addresses 0..252 byte, word-aligned)
IMEM_CAP = 256

RR_OPS = ["add", "sub", "and", "or", "xor", "slt", "sltu", "sll", "srl", "sra"]
RI_OPS = ["addi", "andi", "ori", "xori", "slti", "sltiu"]
SH_OPS = ["slli", "srli", "srai"]


def rand_program(rng: random.Random, n: int):
    """A random straight-line program.  Register x0 stays the memory base (0);
    memory ops use word-aligned, in-range offsets so nothing traps.  No control
    flow (covered by the directed suite) so length is exactly n."""
    lines = []
    def rd():   # avoid x0 as a destination we care about (writes ignored anyway)
        return rng.randint(1, 31)
    def rs():
        return rng.randint(0, 31)
    for _ in range(n):
        kind = rng.choices(
            ["rr", "ri", "sh", "lui", "auipc", "lw", "sw"],
            weights=[30, 30, 12, 8, 4, 8, 8])[0]
        if kind == "rr":
            lines.append(f"{rng.choice(RR_OPS)} x{rd()}, x{rs()}, x{rs()}")
        elif kind == "ri":
            lines.append(f"{rng.choice(RI_OPS)} x{rd()}, x{rs()}, {rng.randint(-2048, 2047)}")
        elif kind == "sh":
            lines.append(f"{rng.choice(SH_OPS)} x{rd()}, x{rs()}, {rng.randint(0, 31)}")
        elif kind == "lui":
            lines.append(f"lui x{rd()}, {rng.randint(0, 0xFFFFF)}")
        elif kind == "auipc":
            lines.append(f"auipc x{rd()}, {rng.randint(0, 0xFFFFF)}")
        elif kind == "lw":
            off = 4 * rng.randint(0, DMEM_WORDS - 1)
            lines.append(f"lw x{rd()}, {off}(x0)")
        elif kind == "sw":
            off = 4 * rng.randint(0, DMEM_WORDS - 1)
            lines.append(f"sw x{rs()}, {off}(x0)")
    return "\n".join(lines)


def check_one(idx: int, source: str):
    words = assemble(source)
    cpu = RV32I(words, dmem_bytes=DMEM_WORDS * 4)
    steps = cpu.run(max_steps=10000)
    c = build_cpu_circuit(words, imem_cap=IMEM_CAP, dmem_cap=DMEM_WORDS)
    name = f"fz{idx}"
    vf = os.path.join(BUILD, name + "_clk.txt")
    with open(vf, "w") as f:
        f.write(gen_clock(steps))
    _, err, parsed = run_circuit(c.emit(), name=name,
                                 time_limit=2 * steps * HALF, testfile=vf)
    if parsed["rc"] != 0:
        return idx, False, f"sim rc={parsed['rc']} {err[:200]}", source
    ref_regs = {i: cpu.regs[i] & 0xFFFFFFFF for i in range(1, 32)}
    hw_regs = {}
    for k, v in parsed["regs"].items():
        if k.startswith("x") and k[1:].isdigit():
            hw_regs[int(k[1:])] = v
    ref_mem = cpu.dump_dmem_words()
    hw_mem = parsed["mem"].get("dmem", {})
    diffs = []
    for i in range(1, 32):
        if ref_regs.get(i, 0) != hw_regs.get(i, 0):
            diffs.append(f"x{i}: ref={ref_regs.get(i,0):#x} hw={hw_regs.get(i,0):#x}")
    allmem = set(ref_mem) | set(hw_mem)
    for a in sorted(allmem):
        if ref_mem.get(a, 0) != hw_mem.get(a, 0):
            diffs.append(f"dmem[{a}]: ref={ref_mem.get(a,0):#x} hw={hw_mem.get(a,0):#x}")
    return idx, not diffs, "; ".join(diffs[:12]), source


def main():
    num = int(sys.argv[1]) if len(sys.argv) > 1 else 30
    seed = int(sys.argv[2]) if len(sys.argv) > 2 else 1234
    workers = int(sys.argv[3]) if len(sys.argv) > 3 else 6
    rng = random.Random(seed)
    progs = [(i, rand_program(rng, rng.randint(6, 24))) for i in range(num)]
    npass = nfail = 0
    with ThreadPoolExecutor(max_workers=workers) as ex:
        futs = [ex.submit(check_one, i, src) for i, src in progs]
        for fut in as_completed(futs):
            idx, ok, detail, source = fut.result()
            if ok:
                npass += 1
            else:
                nfail += 1
                print(f"[FAIL] program {idx}:\n{source}\n  -> {detail}\n")
    print(f"\nfuzz: {npass} passed, {nfail} failed (seed={seed}, num={num})")
    sys.exit(1 if nfail else 0)


if __name__ == "__main__":
    main()
