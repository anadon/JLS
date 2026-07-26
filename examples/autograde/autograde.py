#!/usr/bin/env python3
"""Example autograde bridge over the JLS batch CLI (issue #216).

This script demonstrates the supported grading pattern documented in
docs/vcd-interop.md: run ``jls -b -vcd out.vcd circuit.jls`` as a
subprocess, then grade two stable surfaces of the run:

  1. the watched-element stdout report (docs/batch-interface.md
     section 3 -- a documented stability contract), and
  2. the emitted IEEE 1364-2001 section 18 VCD waveform
     (docs/batch-interface.md section 4), parsed here with a
     deliberately minimal, dependency-free parser.

This is **not co-simulation**: JLS runs to completion on its own and
this script inspects the finished outputs. A live co-simulation
transport (cocotb/VPI/DPI style, stepping JLS from an external
testbench) was evaluated and rejected -- see issue #63. Do not build
graders that expect to interact with a running JLS simulation.

Usage:

    python3 examples/autograde/autograde.py
        (runs the ``jls`` command found on PATH)

    python3 examples/autograde/autograde.py -- <jls command tokens...>
        (e.g. ``-- java -jar jls-5.0.5.jar`` or, as the CI test
        test/jls/AutogradeBridgeExampleTest.java does,
        ``-- java -cp <classpath> jls.JLS``)

The graded fixture is test/fixtures/fork-4.6-shiftregister.jls: three
combinational barrel shifters computing 181 shifted by 2, with watched
output pins ll (logical left), lr (logical right) and ar (arithmetic
right). Exit status: 0 when every check passes, 1 on a grading
failure, 2 on a usage/setup error.
"""

import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

# Expected final values of the watched pins (the same oracle values
# test/jls/ShiftRegisterTest.java pins): 181 = 0b10110101, shifted by 2.
EXPECTED_FINALS = {
    "ll": 0b11010100,  # logical left
    "lr": 0b00101101,  # logical right
    "ar": 0b11101101,  # arithmetic right (sign fill)
}

# The stdout report is a stability contract (batch-interface.md 3.2/3.4):
# "Output Pin <name>: 0xH (U unsigned, S signed)" in name order.
EXPECTED_STDOUT_LINES = [
    "Output Pin ar: 0xED (237 unsigned, -19 signed)",
    "Output Pin ll: 0xD4 (212 unsigned, -44 signed)",
    "Output Pin lr: 0x2D (45 unsigned, 45 signed)",
]


def jls_command(argv):
    """The JLS command tokens: everything after ``--``, default ``jls``."""
    if "--" in argv:
        tokens = argv[argv.index("--") + 1:]
        if tokens:
            return tokens
    if argv and argv[0] in ("-h", "--help"):
        print(__doc__)
        raise SystemExit(0)
    if argv:
        print("usage: autograde.py [-- <jls command tokens...>]",
              file=sys.stderr)
        raise SystemExit(2)
    return ["jls"]


def parse_vcd_final_values(text):
    """Final value of every signal in a JLS batch VCD, by signal name.

    Minimal parser for the profile batch-interface.md section 4
    specifies: ``$var`` declarations name the signals, then ``#<time>``
    stamps interleave scalar changes (``0!``, ``1!``, ``z!``) and
    vector changes (``b101101 #``). The last change per identifier
    code wins. Values made only of 0/1 come back as ints; anything
    containing HiZ stays a string (JLS never emits 'x').
    """
    names = {}   # identifier code -> declared signal name
    finals = {}  # identifier code -> last value string
    in_header = True
    for raw in text.splitlines():
        line = raw.strip()
        if not line:
            continue
        if in_header:
            if line.startswith("$var"):
                # $var wire <bits> <code> <name> [msb:0] $end
                parts = line.split()
                names[parts[3]] = parts[4]
            elif line.startswith("$enddefinitions"):
                in_header = False
            continue
        if line.startswith("#") or line.startswith("$"):
            continue  # timestamp, or $dumpvars/$end block markers
        if line[0] in "bB":
            value, code = line[1:].split()
            finals[code] = value
        else:
            finals[line[1:]] = line[0]
    result = {}
    for code, value in finals.items():
        name = names.get(code, code)
        result[name] = int(value, 2) if set(value) <= {"0", "1"} else value
    return result


def main():
    jls = jls_command(sys.argv[1:])
    repo_root = Path(__file__).resolve().parents[2]
    fixture = repo_root / "test" / "fixtures" / "fork-4.6-shiftregister.jls"
    if not fixture.is_file():
        print(f"FAIL (setup): fixture not found: {fixture}", file=sys.stderr)
        return 2

    with tempfile.TemporaryDirectory() as tmp:
        # JLS requires the circuit file name (minus .jls) to start with
        # a letter and contain only letters, digits and underscores, so
        # copy the hyphenated fixture under a conforming name.
        circuit = Path(tmp) / "shiftdemo.jls"
        shutil.copyfile(fixture, circuit)
        vcd_file = Path(tmp) / "out.vcd"

        cmd = jls + ["-b", "-vcd", str(vcd_file), str(circuit)]
        try:
            proc = subprocess.run(cmd, capture_output=True, text=True,
                                  timeout=120)
        except FileNotFoundError:
            print(f"FAIL (setup): cannot run {cmd[0]!r}; pass the JLS "
                  "command after '--'", file=sys.stderr)
            return 2

        # Exit status is the failure signal (batch-interface.md 1).
        if proc.returncode != 0:
            print(f"FAIL: jls exited {proc.returncode}\n"
                  f"stdout:\n{proc.stdout}\nstderr:\n{proc.stderr}",
                  file=sys.stderr)
            return 1

        failures = []

        # Surface 1: the watched-element stdout report.
        for line in EXPECTED_STDOUT_LINES:
            if line not in proc.stdout:
                failures.append(f"stdout is missing {line!r}")

        # Surface 2: final signal values from the VCD waveform.
        finals = parse_vcd_final_values(vcd_file.read_text())
        for name, want in sorted(EXPECTED_FINALS.items()):
            got = finals.get(name)
            if got != want:
                failures.append(f"VCD signal {name}: expected {want!r}, "
                                f"got {got!r}")

        if failures:
            print("FAIL:", file=sys.stderr)
            for f in failures:
                print(f"  - {f}", file=sys.stderr)
            print(f"jls stdout was:\n{proc.stdout}", file=sys.stderr)
            return 1

    print("PASS: batch stdout and VCD final values match the oracle "
          f"({', '.join(sorted(EXPECTED_FINALS))})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
