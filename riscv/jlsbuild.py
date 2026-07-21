"""jlsbuild -- a small netlist compiler that emits JLS ``.jls`` circuit text.

JLS (the Java Logic Simulator in this repository) stores circuits as
line-oriented text (see docs/file-format.md).  Crucially, the simulator wires
elements purely by (element-id, put-name) references -- geometry is irrelevant
to simulation.  That lets us treat a JLS circuit as a plain netlist: create
elements, then connect output puts to input puts.  Fan-out (one driver, many
sinks) is a single wire net whose driver WireEnd carries several ``ref wire``
segments (a star), which the loader accepts because a WireEnd holds a set of
wires.

Every element emitter below matches the exact save format and put names used by
the JLS element classes under src/jls/elem/.  The companion test file
test_primitives.py validates each primitive against the real batch simulator.

This file is pure stdlib and emits the plain-text ``.jls`` container (FORMAT 1),
which the JLS loader accepts directly.
"""

from __future__ import annotations

import math
from typing import List, Optional, Tuple


def sel_bits(n: int) -> int:
    """Selector width for an n-way choice: ceil(log2(n)), matching JLS
    (32 - Integer.numberOfLeadingZeros(n-1))."""
    if n <= 1:
        return 0
    return (n - 1).bit_length()


class Port:
    __slots__ = ("el", "name", "bits")

    def __init__(self, el: "El", name: str, bits: int):
        self.el = el
        self.name = name
        self.bits = bits

    def __repr__(self):
        return f"<Port {self.el.tag}#{self.el.id}.{self.name}[{self.bits}]>"


class El:
    """A placed element with a stable id and named ports."""

    def __init__(self, circ: "Circuit", tag: str, attr_lines: List[str],
                 outs: List[Tuple[str, int]], ins: List[Tuple[str, int]]):
        self.circ = circ
        self.tag = tag
        self.attr_lines = attr_lines
        self.id = circ._new_id()
        self.ports = {}
        for n, b in outs:
            self.ports[n] = Port(self, n, b)
        for n, b in ins:
            self.ports[n] = Port(self, n, b)
        circ.elements.append(self)

    def p(self, name: str) -> Port:
        return self.ports[name]

    # convenience for single-output elements
    @property
    def out(self) -> Port:
        return self.ports["output"]


class Net:
    __slots__ = ("driver", "sinks", "tristate")

    def __init__(self, driver: Port, sinks: List[Port], tristate: bool = False):
        self.driver = driver
        self.sinks = sinks
        self.tristate = tristate


class Circuit:
    def __init__(self, name: str = "circuit"):
        self.name = name
        self.elements: List[El] = []
        self.nets: List[Net] = []
        self._net_by_driver = {}
        self._id = 0
        # a spread-out grid position, purely cosmetic
        self._x = 60
        self._y = 60

    def _new_id(self) -> int:
        i = self._id
        self._id += 1
        return i

    def _pos(self) -> Tuple[int, int]:
        x, y = self._x, self._y
        self._x += 120
        if self._x > 3000:
            self._x = 60
            self._y += 120
        return x, y

    # ---- connection ----
    def connect(self, driver: Port, *sinks: Port, tristate: bool = False):
        """Wire one driver put to one or more sink puts.

        All connections sharing a driver put are merged into ONE wire net --
        a JLS put carries a single attachment, and fan-out happens within a net
        (one driver WireEnd with several wire segments), not by re-attaching the
        put in multiple nets.  Bit widths are checked leniently (Constants
        report width 0 and adapt)."""
        for s in sinks:
            if driver.bits and s.bits and driver.bits != s.bits:
                raise ValueError(
                    f"width mismatch: {driver} ({driver.bits}) -> {s} ({s.bits})")
        key = id(driver)
        net = self._net_by_driver.get(key)
        if net is None:
            net = Net(driver, [], tristate)
            self._net_by_driver[key] = net
            self.nets.append(net)
        net.sinks.extend(sinks)

    # ---- element factories ----
    def _base(self, extra: str = "") -> List[str]:
        x, y = self._pos()
        return [f" int x {x}", f" int y {y}"] + ([extra] if extra else [])

    def constant(self, value: int, bits: int = 0, base: int = 10) -> El:
        # Constant output width is 0 (adapts to attached wire).  We still record
        # a nominal bits for our own width checking when the caller knows it.
        a = self._base() + [f" Int value {value}", f" int base {base}",
                            ' String orient "RIGHT"']
        return El(self, "Constant", a, outs=[("output", bits)], ins=[])

    def input_pin(self, name: str, bits: int, watch: int = 0) -> El:
        a = self._base() + [f' String name "{name}"', f" int bits {bits}",
                            f" int watch {watch}", ' String orient "RIGHT"']
        return El(self, "InputPin", a, outs=[("output", bits)], ins=[])

    def output_pin(self, name: str, bits: int, watch: int = 1) -> El:
        a = self._base() + [f' String name "{name}"', f" int bits {bits}",
                            f" int watch {watch}", ' String orient "RIGHT"']
        return El(self, "OutputPin", a, outs=[], ins=[("input", bits)])

    def clock(self, cycle: int, one: Optional[int] = None) -> El:
        if one is None:
            one = cycle // 2
        a = self._base() + [f" int cycle {cycle}", f" int one {one}",
                            ' String orient "RIGHT"']
        return El(self, "Clock", a, outs=[("output", 1)], ins=[])

    def gate(self, kind: str, bits: int, num_inputs: int, delay: int = 1) -> El:
        # kind in AndGate/OrGate/NandGate/NorGate/XorGate/NotGate/DelayGate
        a = self._base() + [f" int bits {bits}", f" int numInputs {num_inputs}",
                            ' String orientation "right"', f" int delay {delay}"]
        ins = [(f"input{i}", bits) for i in range(num_inputs)]
        return El(self, kind, a, outs=[("output", bits)], ins=ins)

    def AND(self, bits, n=2, delay=1): return self.gate("AndGate", bits, n, delay)
    def OR(self, bits, n=2, delay=1): return self.gate("OrGate", bits, n, delay)
    def NAND(self, bits, n=2, delay=1): return self.gate("NandGate", bits, n, delay)
    def NOR(self, bits, n=2, delay=1): return self.gate("NorGate", bits, n, delay)
    def XOR(self, bits, n=2, delay=1): return self.gate("XorGate", bits, n, delay)
    def NOT(self, bits, delay=1): return self.gate("NotGate", bits, 1, delay)

    def extend(self, bits: int, delay: int = 0) -> El:
        """1-bit input replicated to `bits` output bits (sign/bit extend)."""
        a = self._base() + [f" int bits {bits}", " int numInputs 1",
                            ' String orientation "right"', f" int delay {delay}"]
        return El(self, "Extend", a, outs=[("output", bits)], ins=[("input0", 1)])

    def adder(self, bits: int, delay: int = 1) -> El:
        a = self._base() + [f" int bits {bits}", ' String orient "RIGHT"',
                            f" int delay {delay}"]
        return El(self, "Adder", a,
                  outs=[("S", bits), ("Cout", 1)],
                  ins=[("A", bits), ("B", bits), ("Cin", 1)])

    def mux(self, inputs: int, bits: int, delay: int = 1) -> El:
        sb = sel_bits(inputs)
        a = self._base() + [f" int inputs {inputs}", f" int bits {bits}",
                            f" int delay {delay}", ' String iOrient "RIGHT"',
                            ' String sOrient "DOWN"']
        ins = [("select", sb)] + [(f"input{i}", bits) for i in range(inputs)]
        return El(self, "Mux", a, outs=[("output", bits)], ins=ins)

    def decoder(self, bits: int, delay: int = 1) -> El:
        a = self._base() + [f" int bits {bits}", f" int delay {delay}",
                            ' String orient "RIGHT"']
        return El(self, "Decoder", a, outs=[("output", 1 << bits)],
                  ins=[("input", bits)])

    def shifter(self, kind: str, bits: int, delay: int = 1) -> El:
        # kind in LogicalLeft / LogicalRight / ArithmeticRight
        sb = sel_bits(bits)
        a = self._base() + [f' String type "{kind}"', f" int bits {bits}",
                            f" int delay {delay}", ' String iOrient "RIGHT"',
                            ' String sOrient "DOWN"']
        return El(self, "ShiftRegister", a, outs=[("output", bits)],
                  ins=[("amount", sb), ("input", bits)])

    def tristate(self, bits: int, delay: int = 1) -> El:
        a = self._base() + [f" int bits {bits}", f" int delay {delay}",
                            ' String Gorient "RIGHT"', ' String Corient "DOWN"']
        return El(self, "TriState", a, outs=[("output", bits)],
                  ins=[("input", bits), ("control", 1)])

    def register(self, name: str, bits: int, init: int = 0, kind: str = "pff",
                 delay: int = 1, watch: int = 0) -> El:
        a = self._base() + [f' String name "{name}"', f" int bits {bits}",
                            f" Int init {init}", ' String orient "RIGHT"',
                            f" int delay {delay}", f' String type "{kind}"',
                            f" int watch {watch}"]
        return El(self, "Register", a,
                  outs=[("Q", bits), ("notQ", bits)],
                  ins=[("D", bits), ("C", 1)])

    def memory(self, name: str, bits: int, cap: int, kind: str = "RAM",
               init: str = "", time: int = 10, watch: int = 0) -> El:
        """RAM or ROM.  Active-low CS/OE/WE.  init is raw 'addr value\\n' hex text
        (already escaped for the file, i.e. use literal \\n)."""
        abits = sel_bits(cap)
        a = self._base() + [f' String name "{name}"', f' String type "{kind}"',
                            f" int bits {bits}", f" int cap {cap}",
                            f" int time {time}", f" int watch {watch}",
                            ' String file ""', f' String init "{init}"']
        ins = [("address", abits)]
        if kind == "RAM":
            ins += [("input", bits), ("WE", 1)]
        ins += [("OE", 1), ("CS", 1)]
        return El(self, "Memory", a, outs=[("output", bits)], ins=ins)

    def splitter(self, bits: int, ranges: List[Tuple[int, int]],
                 delay: int = 0) -> El:
        """Unbundle a `bits`-wide input into narrow outputs.

        ranges: list of (hi, lo) inclusive bit ranges (hi >= lo).  Output put
        names follow JLS Entry.toCircuitString: single bit -> "N", range ->
        "HI-LO".  Emits pair items mapping narrow bit -> bundle bit."""
        a = self._base() + [f" int bits {bits}", ' String orient "RIGHT"',
                            ' String noncontig "true"', " int tristate 0"]
        pairs = []
        outs = []
        for idx, (hi, lo) in enumerate(ranges):
            name = _range_name(hi, lo)
            outs.append((name, hi - lo + 1))
            for b in range(lo, hi + 1):     # narrow bit 0 == lo
                pairs.append(f" pair {idx} {b}")
        a += pairs
        return El(self, "Splitter", a, outs=outs, ins=[("input", bits)])

    def binder(self, bits: int, ranges: List[Tuple[int, int]],
               delay: int = 0) -> El:
        """Bundle narrow inputs into a `bits`-wide output.  Same naming/pair
        rules as splitter but reversed direction."""
        a = self._base() + [f" int bits {bits}", ' String orient "RIGHT"',
                            ' String noncontig "true"', " int tristate 0"]
        pairs = []
        ins = []
        for idx, (hi, lo) in enumerate(ranges):
            name = _range_name(hi, lo)
            ins.append((name, hi - lo + 1))
            for b in range(lo, hi + 1):
                pairs.append(f" pair {idx} {b}")
        a += pairs
        return El(self, "Binder", a, outs=[("output", bits)], ins=ins)

    # ---- emit ----
    def emit(self) -> str:
        out = ["FORMAT 1", f"CIRCUIT {self.name}"]
        for el in self.elements:
            out.append(f"ELEMENT {el.tag}")
            out.append(f" int id {el.id}")
            out.extend(el.attr_lines)
            out.append("END")
        # wire ends
        wid = self._id
        for net in self.nets:
            driver_end = wid
            wid += 1
            sink_ends = []
            for _ in net.sinks:
                sink_ends.append(wid)
                wid += 1
            ts = " int tristate 1" if net.tristate else None
            # driver end: refs all sink ends
            out.append("ELEMENT WireEnd")
            out.append(f" int id {driver_end}")
            out.append(" int x 0")
            out.append(" int y 0")
            out.append(f' String put "{net.driver.name}"')
            out.append(f" ref attach {net.driver.el.id}")
            for se in sink_ends:
                out.append(f" ref wire {se}")
            if ts:
                out.append(ts)
            out.append("END")
            for se, sink in zip(sink_ends, net.sinks):
                out.append("ELEMENT WireEnd")
                out.append(f" int id {se}")
                out.append(" int x 0")
                out.append(" int y 0")
                out.append(f' String put "{sink.name}"')
                out.append(f" ref attach {sink.el.id}")
                out.append(f" ref wire {driver_end}")
                if ts:
                    out.append(ts)
                out.append("END")
        out.append("ENDCIRCUIT")
        return "\n".join(out) + "\n"

    def save(self, path: str):
        with open(path, "w") as f:
            f.write(self.emit())


def _range_name(hi: int, lo: int) -> str:
    if hi == lo:
        return str(hi)
    return f"{hi}-{lo}"
