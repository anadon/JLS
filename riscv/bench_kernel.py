"""Hot-loop baseline: build the RV32I CPU circuit, run it headless for a
given number of clock cycles, and report wall time.

Not part of the verification suite - verify.py owns correctness.  This is
the largest real simulation workload in the tree, so it is the honest
baseline for any change to the event loop or the value representation.
It produced the kernel measurements in
docs/capability-roadmap/keystone-c-performance.md; re-run it before and
after any such change rather than trusting those numbers.

    python3 bench_kernel.py [iterations]   # default 100

Prints the reference emulator's result beside the circuit's, so a run
that is fast because it stopped computing is visible rather than silent.
"""
import os
import sys
import time

from riscv_ref import assemble, RV32I
from build_cpu import build_cpu_circuit
from jlsrun import run_circuit, BUILD, jar_path

HALF = 1000

PROG = """
        addi x1, x0, 0
        addi x2, x0, 1
        addi x3, x0, %d
loop:
        add  x1, x1, x2
        addi x2, x2, 1
        blt  x2, x3, loop
        sw   x1, 0(x0)
"""


def gen_clock(steps):
    parts = ["clk 0"]
    for k in range(1, 2 * steps + 1):
        parts.append("until %d %d" % (k * HALF, 1 if k % 2 == 1 else 0))
    parts.append("end")
    return " ".join(parts) + "\n"


def main():
    iters = int(sys.argv[1]) if len(sys.argv) > 1 else 100
    src = PROG % (iters + 1)
    words = assemble(src)
    cpu = RV32I(words, dmem_bytes=64)
    steps = cpu.run(max_steps=1000000)
    print("instructions retired (= clocked cycles): %d" % steps)
    c = build_cpu_circuit(words, imem_cap=16, dmem_cap=16)
    vec = gen_clock(steps)
    vf = os.path.join(BUILD, "kbench_clk.txt")
    with open(vf, "w") as f:
        f.write(vec)
    limit = 2 * steps * HALF
    t0 = time.time()
    out, err, parsed = run_circuit(c.emit(), name="kbench", time_limit=limit,
                                   testfile=vf)
    t1 = time.time()
    print("wall: %.3f s   outcome: %s  rc=%s" % (t1 - t0, parsed["outcome"],
                                                 parsed["rc"]))
    print("x1 = %s (ref %s)" % (parsed["regs"].get("x1"),
                                cpu.dump_regs().get(1)))
    print("jar: %s" % jar_path())


if __name__ == "__main__":
    main()
