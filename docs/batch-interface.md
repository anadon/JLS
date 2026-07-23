# The JLS Batch Interface

**Status: normative, and a stability contract.** This document specifies
the batch-mode ("grading") interface of JLS: the `-t` test-vector input
format, the watched-element output format printed to stdout, the process
exit/stream contract, and the `-vcd` waveform export (issue #72). The
formats specified here are a compatibility promise: **any change to them
requires a CHANGELOG entry and either a major version bump or a
compatibility flag that preserves the old behavior.** Scripts may parse
these formats; JLS may not break them silently.

Every claim below is stated from the implementation and carries a code
anchor (`file` / method). If code and document disagree, that is a bug
in one of them; golden tests (`test/jls/VcdExportGoldenTest.java`,
`test/jls/BatchSimulationGoldenTest.java`) pin the load-bearing parts.

## 1. Invocation, streams, and exit codes

Batch mode is selected with `-b` and takes one circuit file operand:

```
jls -b [-s paramfile] [-t testfile] [-d limit] [-vcd file] [-r printer] [--] circuit.jls
```

The flag table in `src/jls/JLSStart.java` (`FLAGS`) is the single
authoritative flag list; `jls -h` prints usage generated from it. Flag
operands may be attached (`-tvectors.txt`) or separated (`-t
vectors.txt`); `--` ends flag processing. When one flag name is a prefix
of another (`-v` / `-vcd`), the longest name wins, so `-vcd` is always
the VCD flag and never `-v` with an attached operand
(`JLSStart.parseCommandLine`).

Stream and exit-status contract (issue #42, `JLSStart.usageError` and
the batch branch of `JLSStart.start`):

| status | meaning          | streams                                        |
|--------|------------------|------------------------------------------------|
| 0      | run completed    | results on stdout (section 3), stderr empty    |
| 1      | runtime failure  | one-line diagnostic `jls: error: ...` on stderr |
| 2      | usage error      | one-line diagnostic `jls: error: ...` on stderr |

stdout carries *only* the simulation results, so it can be piped and
diffed. **Known deviation:** errors found while parsing the `-t` test
file are printed to *stdout* (two lines: `error in test file` and the
message), then the process exits 1 (`TestGen.specError`,
`src/jls/elem/TestGen.java`); an unopenable test file likewise prints
`Can't open test file: <name>` to stdout and exits 1 (`TestGen.initSim`).
Grading scripts should treat exit status, not stream placement, as the
failure signal for test-file problems.

`-d limit` sets the simulation time limit (a positive integer; default
`JLSInfo.defaultTimeLimit`). `-s paramfile` applies a parameter file
(`JLSStart.processParamFile`) which can, among other things, set
`ELEMENT <name> WATCHED true` — the batch way to select outputs without
editing the circuit.

## 2. Test-vector input format (`-t`)

The `-t` file drives the circuit's *top-level input pins*. It is parsed
by `SigSim.initSim(Simulator, Scanner)` (`src/jls/elem/SigSim.java`);
`TestGen` (created by `BatchSimulator.addTestGen`) merely opens the file
and delegates, so the grammar below is the one implemented there. When a
`-t` file is given, any signal generators in the top-level circuit are
removed and replaced by the test generator
(`BatchSimulator.addTestGen`, `src/jls/sim/BatchSimulator.java`).

### 2.1 Lexical rules

- The file is tokenized on whitespace; line boundaries carry no meaning
  beyond ending comments. Encoding is UTF-8 (`TestGen.initSim`).
- `#` begins a comment that runs to the end of the line. (Precisely: the
  parser joins each line's tokens and truncates at the first `#`
  character, so `#` need not be preceded by whitespace.)
- Any token matching `-?0[xX][0-9a-fA-F]+` is rewritten to its decimal
  value before parsing, so values may be written in hex. This rewrite
  pass runs *before* comment stripping, which is harmless but means a
  malformed hex-like token inside a comment is still rewritten.

### 2.2 Grammar

```
file    ::= { signal }
signal  ::= name initial { step } "end"
step    ::= ( "for" duration | "until" time ) value
initial ::= value
```

- `name` must exactly match the name of an `InputPin` in the *top-level*
  circuit (input pins inside subcircuits are not reachable). An unknown
  name is an error ("no input pin for signal ... - signal ignored" —
  and in batch mode every error is fatal, see 2.4).
- `value` is an integer (decimal, or hex per 2.1; a leading `-` makes it
  negative), read as an unbounded `BigInteger`.
- `duration` and `time` are 64-bit integers (`Scanner.hasNextLong`).
  They are *not* range-checked: a zero or negative `for` duration is
  accepted and produces a non-increasing event time whose behavior is
  unspecified — use positive durations. `until` times *are* checked:
  each must be strictly greater than the previous event time for that
  signal.

### 2.3 Timing semantics

For each signal, the parser posts one simulation event per value
(`sim.post(new SimEvent(t, pin, value))`):

- The `initial` value is posted at time 0.
- `for d v` posts `v` at *previous event time + d*: the previous value
  holds **for** `d` time units, then the pin becomes `v`.
- `until t v` posts `v` at absolute time `t`: the previous value holds
  **until** `t`, then the pin becomes `v`.
- `end` closes the signal; after its last event the pin holds its final
  value for the rest of the run.

Example (from `test/jls/VcdExportGoldenTest.java`): with
`a 0 for 10 0x1 until 30 0 end`, pin `a` is 0 over [0,10), 1 over
[10,30), and 0 from 30 on.

### 2.4 Value width and error handling

- A non-negative value must satisfy `bitLength() <= bits` of the pin; a
  negative value must satisfy `bitLength() + 1 <= bits`. Anything wider
  is an error ("value ... will not fit in signal ...").
- Negative values are converted to two's complement: `v + 2^bits`.
- In batch mode every parse error is fatal: message to stdout (see the
  deviation in section 1) and exit 1. Nothing after the offending token
  is processed.

## 3. Watched-element output format (stdout)

After the run, batch mode prints exactly two things to stdout: one
outcome line, then the watched-element report.

### 3.1 Outcome line

`BatchSimulator.displayOutcome` (`src/jls/sim/BatchSimulator.java`)
prints one line, `<reason> at <time>`, where `<time>` is the final
simulation time and `<reason>` is exactly one of (in precedence order):

1. `Simulation Stopped` — the simulator was stopped;
2. `Simulation Time Limit` — the `-d` limit was reached;
3. `Simulation: No More Activity` — the event queue drained;
4. `Simulation Complete` — none of the above.

### 3.2 Element whitelist and order

`JLSStart.displayResults` (`src/jls/JLSStart.java`) walks the circuit
recursively and prints **only** watched elements of exactly three
types: `Register`, `Memory`, and `OutputPin`. This whitelist is part of
the contract: any other element (including an `InputPin`) never prints
here even if it is marked watched, and probes never print in batch
mode. (Watched elements of *any* type do appear in the VCD, section 4.)

Order: the elements of each circuit level are visited in **element-name
order** (Unicode code point order, `String.compareTo`). A subcircuit is
descended at its own name's position in that order, and its contents
are printed (name-ordered, recursively) at that point. Before issue
#72 this order was `HashSet` iteration order and therefore unstable;
it is now pinned, and `BatchSimulationGoldenTest.
watchedElementsPrintInNameOrder` guards it byte-exactly.

### 3.3 Line formats

With `QUAL` denoting the dotted subcircuit qualifier (empty at top
level, in which case the leading `QUAL.` is omitted), and `VALUE`
denoting the value display of 3.4:

- `OutputPin` (`Pin.printValue`, `src/jls/elem/Pin.java`):

  ```
  Output Pin QUAL.name: VALUE
  ```

- `Register` (`Register.printValue`, `src/jls/elem/Register.java`):

  ```
  Register QUAL.name: VALUE
  ```

- `Memory` (`Memory.printChangedValues`, `src/jls/elem/Memory.java`):
  a ROM prints **nothing**. A RAM prints, if any words differ from
  their initial contents, a heading followed by one line per changed
  address in ascending address order:

  ```
  Changed locations in memory QUAL.name
   0xA: OLD -> NEW
  ```

  where `A` is the address in lower-case hex (`%x`, note the single
  leading space) and `OLD`/`NEW` are value displays (3.4). If nothing
  changed it prints exactly `No changes in memory QUAL.name`.

The qualifier component for a subcircuit is the *name of the imported
circuit instance* (`subCirc.getName()` in `displayResults`), joined
with `.`.

### 3.4 Value display

`BitSetUtils.toDisplay` (`src/jls/BitSetUtils.java`):

- A high-impedance (undriven) value prints as `HiZ`.
- Otherwise: `0xH (U unsigned, S signed)` where `H` is upper-case hex,
  `U` is the unsigned decimal value, and `S` is the signed decimal
  value under two's complement at the element's declared bit width.

Example: a 4-bit register holding 13 prints
`Register r: 0xD (13 unsigned, -3 signed)`.

## 4. VCD waveform export (`-vcd file`)

`-vcd file` writes the batch run's value-change history as a Value
Change Dump per IEEE 1364-2001 section 18, readable by GTKWave, Surfer,
and standard VCD parsers. Emitter: `BatchSimulator.toVcd` /
`BatchSimulator.writeVcd` (`src/jls/sim/BatchSimulator.java`). The
output is deterministic: two identical runs produce identical bytes,
and the golden tests compare byte-for-byte.

### 4.1 Signal set

The VCD contains one signal per **watched element**, at any depth of
the subcircuit hierarchy (`BatchSimulator.findWatched`), **plus one
signal per probed wire net** (`BatchSimulator.findProbes`, issue #200).
Note the watched-element set is *broader* than the stdout whitelist of
3.2: any watched element that the simulator traces appears (e.g. a
watched `InputPin`).

A **probe** is the interactive named-net feature (a name attached to a
wire, saved as a `probe` item on a `WireEnd`, section 7 of
`docs/file-format.md`); its VCD signal is a `wire` named by the probe
and carries the net's value history. This lets a circuit name an
internal net and observe it headlessly without splicing in an
`OutputPin` tap. A probe still never prints to **stdout** — the 3.2
whitelist is unchanged; probes are a VCD-only signal. If a probe name
happens to equal a watched element's full name, the probe signal is
suffixed (`_probe`) so neither is dropped.

Trace accumulation (`BatchSimulator.afterEvent` for elements,
`BatchSimulator.probeSample` for probed nets) is enabled whenever
`-vcd` or `-r` is given; the consumers share one trace and neither
requires the other's flag. A circuit with no probes produces
byte-identical output to before this addition.

### 4.2 Header

In order, one line each (no `$date`/`$version` sections — both are
optional in the standard, and omitting them keeps output
byte-deterministic):

```
$comment JLS batch simulation trace $end
$timescale 1 ns $end
$scope module <top-circuit-name> $end
$var wire <bits> <code> <name> $end            (one per 1-bit signal)
$var wire <bits> <code> <name> [<bits-1>:0] $end   (one per multi-bit signal)
$upscope $end
$enddefinitions $end
```

- **Timescale:** one VCD time unit represents **one JLS simulation
  time unit**. JLS time units are abstract; the nominal `1 ns` is the
  mapping chosen for tool compatibility, and timestamps are the raw
  JLS event times.
- **Scope:** a single flat module scope named after the top circuit.
  `<name>` is the element's fully qualified dotted name
  (`LogicElement.getFullName`, e.g. `adder.carry`); viewers that split
  reference names on `.` will render the hierarchy from it.
- **Declaration order** is `<name>` order (Unicode code point order).
- **Identifier codes** are assigned in that same order: printable
  ASCII `!` (33) through `~` (126), extending to multi-character
  base-94 codes after 94 signals.

### 4.3 Value section

```
#0
$dumpvars
<one value entry per signal, in name order>
$end
#<t>
<value entries for signals that changed at t, in name order>
...
```

- `#0`/`$dumpvars` dumps every signal's value at time 0.
- Subsequent `#<t>` timestamps are strictly increasing and appear only
  when at least one signal changed at `<t>`; only changed values are
  re-emitted (consecutive equal values are deduplicated at recording
  time, `BatchSimulator.afterEvent`).
- If the run ends later than the last change, a final bare `#<endtime>`
  line records the full simulated duration.

**Value mapping.** JLS values are two-state plus high impedance, so the
four-state VCD alphabet is used as `0`, `1`, `z` — **`x` never
appears** (`BatchSimulator.vcdValue`):

- 1-bit signal: `0<code>`, `1<code>`, or `z<code>` (HiZ).
- multi-bit signal: `b<binary> <code>` with the value in binary, most
  significant bit first, leading zeros omitted (zero is `b0`); a signal
  whose whole value is HiZ is `bz <code>` (per the standard, the `z`
  left-extends across the vector). JLS has no per-bit HiZ: a value is
  either fully driven or fully HiZ, so mixed vectors like `b1z0` cannot
  occur.

Newlines are `\n` and the file is written as UTF-8 (all content is
ASCII).

## 5. Relationship to the golden tests

- `test/jls/BatchSimulationGoldenTest.java` and
  `test/jls/SequentialGoldenTest.java` pin *simulation semantics*
  (issue #14: gate truth tables, memory, registers, state machines).
- `BatchSimulationGoldenTest.watchedElementsPrintInNameOrder` pins the
  stdout element order and line format of section 3 byte-exactly.
- `test/jls/VcdExportGoldenTest.java` pins section 4 byte-exactly for a
  clocked-register fixture and a `-t`-driven fixture (which also
  exercises the section 2 grammar: comments, hex, `for`, `until`,
  `end`), and re-checks structural well-formedness with a parser
  written from this document rather than from the emitter.
- `test/jls/CliFlagTableTest.java` and `test/jls/CliSmokeTest.java` pin
  the section 1 flag table, stream, and exit-status contract.

## 6. Stability promise

The `-t` grammar (section 2), the stdout format (section 3), and the
VCD profile (section 4) are frozen as specified. A change that alters
any byte a conforming consumer could observe requires:

1. a CHANGELOG entry describing the change, **and**
2. a major version bump, **or** a compatibility flag that keeps the
   format specified here available unchanged.

Additions that cannot break a conforming consumer (a new flag, a new
optional output gated behind a new flag) are minor-version material but
still belong in the CHANGELOG.
