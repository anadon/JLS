# FEAT-032 - The host byte port, a Console element and transcripts

**Status:** proposed | **Cost:** 10-16 mw | **Owner program:** UNOWNED |
**Spine rank:** S22

## Capability delivered

A running circuit can exchange bytes with a human or with a script, through one
door that a person grants at invocation and that is never a property of the
`.jls` file, and the exchange is recorded so that the run can be replayed
byte-for-byte without the human. A drawn state machine can print. A drawn CPU
can host a serial terminal, take keystrokes, and be graded on what it printed.
The host door is a closed, enumerated set, so the golden-test culture and the
"a circuit file is data that cannot touch your machine" hardening both survive
the arrival of interactivity.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-02 | required | the shell prompt and the commands typed into it are the entire observable; without a byte door there is nothing to witness |
| CAP-03 | required | the monitor program's output and the QDOS-like console are the demo; TASK-0084 writes to this element |
| CAP-04 | beneficial | a breadboard SAP-1 can print its accumulator instead of being read off a display |
| CAP-06 | beneficial | autograding a submission on what it printed needs a transcript, not a screenshot |
| CAP-09 | required | the differential comparison against a reference machine is a comparison of transcripts (TASK-0073) |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-006 | A console session has nothing to talk to inside a run that stops at tick 1e8 or that cannot be paused. Long-lived batch with pause, heartbeat and clean interrupt (TASK-0014) is the loop the port drains inside |
| FEAT-050 | The `elem.host-port` extension-point row is typed in the catalog that FEAT-050 makes real; contributing a typed seam to a registry nothing reads leaves the device subsystem invisible |
| FEAT-008 | The GUI console pane only. The pane is a Swing surface with no test substrate today; TASK-0069's pane half is unratchetable until the UI harness exists. The headless port and the transcript do not wait on it |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0067 | The host byte port seam | The sealed seam, its permits, the invocation-time grant and the session-boundary ratchet |
| TASK-0068 | The console element | The three-address polled serial element that the guest software talks to |
| TASK-0069 | Transcript capture, replay and the console pane | Deterministic replay, the goldens-in-live-mode ratchet, and the GUI pane |

## Acceptance criteria

1. `HostBytePort` is a sealed interface. A `.jls` file, however malformed or
   hostile, cannot cause a host handle to be opened: the grant comes from a
   command-line flag or an explicit GUI action, and the run's outcome line names
   which door was granted.
2. Nothing reachable from `Reacts.react()` touches `java.io`, a host handle or
   an extension lookup, asserted by a ratchet test in the shipped
   `SocketConfinementRatchetTest` idiom.
3. No foreign thread calls `Simulator.post`. The host thread offers bytes to a
   ring; the simulation thread drains it at a declared boundary
   (`Simulator.beforeEvent`, `src/jls/sim/Simulator.java:220,252`), and the
   receive side self-schedules its next poll exactly as `Clock` does.
4. A `Console` element decodes three byte addresses - transmit holding, receive
   buffer, and a status read returning `0x60 | data_ready` - is polled, and
   raises no interrupt.
5. A transcript is a sequence of `(stamp, byte)` where the stamp is a retirement
   index or a simulated time, never a wall clock. Replaying a transcript with no
   human attached reproduces the run's output byte-for-byte.
6. A golden produced while a live host door was granted is refused by CI. The
   run mode is recorded in the artifact and the ratchet reads it.
7. `docs/extension-points.md` gains the `elem.host-port` row, pinned in both
   directions by the catalog test, and `docs/vcd-interop.md` records the console
   decision explicitly rather than leaving it to be inferred.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the host byte port seam, the `Console` element and the transcript | **no issue** |
| 223 | Extension-point catalog: enumerate and type the seams modules contribute to | depends on - the `elem.host-port` row is typed here |
| 63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | informs - `docs/vcd-interop.md:19-24` cites #63 as the recorded rejection of live co-simulation. There is no substantive conflict: the console **pulls** and is never called back into. The wording must be settled before any scripting-API text becomes normative |
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | overlaps - the worked example is the first design whose observable is a transcript |

## Design notes

The permits are the design. `NullPort` (the default, and what every existing
test gets), `StdioPort`, `FilePort`, `PipePort` (the in-memory test double) and
`PanelPort` (the GUI pane). Sealing is structural rather than stylistic: an
external element provider cannot implement a sealed contract, so no loaded file
can acquire host I/O even after FEAT-050 opens a discovery path. If that is ever
overruled, host-touching providers must go out-of-process; in-process is the one
variant that cannot be walked back.

`Console` is an element, not a subcircuit, so the per-instance fidelity
attribute of FEAT-031 does not reach it - a point the capstone plan records as a
real and unfixed asymmetry rather than an oversight.

**Two further doors belong on this seam and have no feature id of their own.** A
`BlockDevice` element over a `FilePort`-shaped permit (priced 3-5 mw; a hard
prerequisite of CAP-03's DOOM stretch, not a nicety) and a framebuffer element
(priced 3-5 mw) are the same mechanism with different payload shapes. They must
be built as permits here. Building either as its own host mechanism gives JLS two
grant models and two ratchet stories.

Ordering, and it is binding: the console work must precede any scripting-API
specification, so that the `docs/vcd-interop.md` amendment records a decision
that was never recorded rather than reversing a normative clause.

## Risks

- **Thread correctness is the whole feature.** A cross-thread `post` would be
  invisible in a fast test and fatal in a long run. The ring, the drain slot and
  the self-scheduled poll are not implementation detail; they are the acceptance
  criteria.
- **Determinism erosion by convenience.** The moment a transcript stamp is a
  wall clock, every console-bearing golden becomes unreproducible. Stamp policy
  is a ratchet, not a review habit.
- **The seam's sealing will be argued against** by whoever wants an out-of-tree
  device. The answer is the out-of-process variant, priced when a second
  consumer exists - not a widening of the permits.
- **Live-mode cycle rate.** A structural CPU echoes a character in roughly
  0.3-1.5 s depending on which engine work has landed. That is a usability fact
  to document in the element help, not a defect of this feature.

## Evidence

- L3 "the host boundary: one door, granted at invocation", including the sealed
  permits, the ring, the self-scheduled poll and the transcript:
  `03-determination.md:240-283`; the typed catalog row `elem.host-port` at
  `03-determination.md:803`.
- D7, verbatim: `HostBytePort` is not a plugin seam; host access is one door
  granted at invocation: `BRIEF.md` §12 D7.
- Verified at HEAD `addc6c5`: `grep -rn "System.in" src/` returns 0 - this is
  the first read-side host door in the project's history.
- The drain slot exists: `src/jls/sim/Simulator.java:210-220,252` (the
  `beforeEvent` hook, with a javadoc that already sanctions a peek-based hook).
- The self-scheduling idiom to copy: `src/jls/elem/Clock.java:392,421`.
- The run-length ceiling this feature needs FEAT-006 to remove:
  `src/jls/JLSInfo.java:69` (`defaultTimeLimit = 100000000`).
- Live co-simulation as currently documented: `docs/vcd-interop.md:19-24`.
- Cost band and spine rank: `10-capstone-plan.md:618` (S22, 10-16 wk),
  `:961` (the console floor inside wave 5), `:973` (the ordering constraint).
