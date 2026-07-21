#!/usr/bin/env python3
"""Verify the GUI-built accumulator CPU (riscv/gui/RiscvCpu.java output,
cpu.jls) against the independent reference emulator riscv_ref.py.

The circuit executes `addi x1, x1, 3` every clock edge, so after N cycles the
hardware register ACC must equal what the reference computes after running N
copies of that instruction, and PC must equal N. We drive the JLS circuit in
batch mode at several durations, read the watched PC/ACC registers, and for
each observed cycle count run the reference on that many addi words.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
RISCV = os.path.dirname(HERE)
sys.path.insert(0, RISCV)

from riscv_ref import RV32I           # noqa: E402
import jlsrun                          # noqa: E402

IMM = 3
ADDI = (IMM << 20) | 0x8093            # addi x1, x1, 3  ==  0x00308093

CPU_JLS = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
    "/tmp/claude-0/-home-user-JLS/"
    "5629c65d-470b-5c3b-a567-4f9019babece/scratchpad", "cpu.jls")

# (duration -> expected cycle count) for the cycle=2000 clock
DURATIONS = [3000, 9000, 15000, 21000, 41000]

def ref_x1(cycles):
    cpu = RV32I([ADDI] * cycles + [0])   # halt sentinel after the program
    cpu.run()
    return cpu.dump_regs()[1]

def main():
    with open(CPU_JLS) as f:
        text = f.read()
    print(f"instruction addi x1,x1,{IMM} = 0x{ADDI:08x}\n")
    ok = True
    for d in DURATIONS:
        out, err, parsed = jlsrun.run_circuit(text, name="gui_cpu",
                                              time_limit=d)
        pc = parsed["regs"].get("PC")
        acc = parsed["regs"].get("ACC")
        if pc is None or acc is None:
            print(f"-d {d:6d}: FAILED to read registers ({parsed['regs']})")
            ok = False
            continue
        expected = ref_x1(pc)
        good = (acc == expected)
        ok = ok and good
        print(f"-d {d:6d}: hardware PC={pc:2d} ACC={acc:3d} | "
              f"reference x1 after {pc} addi = {expected:3d} | "
              f"{'MATCH' if good else 'MISMATCH'}")
    print()
    print("RESULT:", "ALL MATCH -- GUI-built CPU matches the reference"
          if ok else "MISMATCH")
    sys.exit(0 if ok else 1)

if __name__ == "__main__":
    main()
