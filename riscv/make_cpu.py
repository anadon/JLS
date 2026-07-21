#!/usr/bin/env python3
"""make_cpu.py -- compile a RISC-V assembly program into a runnable JLS circuit.

    python3 make_cpu.py program.s [-o out.jls] [--imem N] [--dmem N]

Produces:
  * out.jls        -- the single-cycle RV32I CPU with the program baked into
                      its instruction ROM (open it in the JLS GUI, or run it
                      headless in batch mode).
  * out.clk.txt    -- a -t clock vector that steps the CPU for exactly as many
                      cycles as the program needs (computed by the reference
                      emulator), then holds.

It prints the exact batch command to run, and the reference emulator's expected
final register/memory state so you can check the hardware against it.
"""
import argparse
import os
import sys

from riscv_ref import assemble, RV32I
from build_cpu import build_cpu_circuit
from verify import gen_clock, HALF
from jlsrun import jar_path


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("source", help="assembly (.s) file")
    ap.add_argument("-o", "--out", default=None, help="output .jls path")
    ap.add_argument("--imem", type=int, default=256, help="instruction words")
    ap.add_argument("--dmem", type=int, default=256, help="data words")
    ap.add_argument("--max-steps", type=int, default=100000)
    args = ap.parse_args()

    with open(args.source) as f:
        src = f.read()
    words = assemble(src)
    if len(words) > args.imem:
        print(f"program has {len(words)} instructions but --imem is {args.imem}",
              file=sys.stderr)
        sys.exit(1)

    cpu = RV32I(words, dmem_bytes=args.dmem * 4)
    steps = cpu.run(max_steps=args.max_steps)

    out = args.out or os.path.splitext(args.source)[0] + ".jls"
    clk = os.path.splitext(out)[0] + ".clk.txt"
    c = build_cpu_circuit(words, imem_cap=args.imem, dmem_cap=args.dmem)
    c.save(out)
    with open(clk, "w") as f:
        f.write(gen_clock(steps))

    print(f"wrote {out} ({len(words)} instructions, {steps} dynamic steps)")
    print(f"wrote {clk} (clock vector, {steps} rising edges)")
    print()
    print("Run headless in batch mode:")
    print(f"  java -jar {os.path.relpath(jar_path())} -b "
          f"-d {2*steps*HALF} -t {clk} {out}")
    print()
    print("Reference emulator expects (non-zero architectural state):")
    regs = {i: v for i, v in cpu.dump_regs().items() if v}
    for i in sorted(regs):
        v = regs[i]
        s = v - (1 << 32) if v >> 31 else v
        print(f"  x{i:<2} = 0x{v:08x} ({v} unsigned, {s} signed)")
    mem = cpu.dump_dmem_words()
    for a in sorted(mem):
        print(f"  dmem word[{a}] (byte 0x{a*4:x}) = 0x{mem[a]:08x}")
    print(f"  PC (final) = 0x{cpu.pc:08x}")


if __name__ == "__main__":
    main()
