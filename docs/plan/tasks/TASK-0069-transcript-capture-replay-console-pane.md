# TASK-0069 - Transcript capture, replay and the console pane

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0068

## Deliverable

Host input becomes a recorded, replayable artifact; a golden produced with a
live human attached becomes a build failure; and the exchange gets a GUI
surface through the existing runner/event-thread seam.

1. **`src/jls/io/Transcript`** - an ordered sequence of
   `record Stamped(long stamp, StampKind kind, Direction dir, byte value)` where
   `StampKind` is `RETIREMENT` or `SIMULATED_TIME` and **never** wall clock.
   Canonical text form, one entry per line, deterministic field order, no
   `$date`-equivalent header - the same decision `BatchSimulator.toVcd` already
   made (`src/jls/sim/BatchSimulator.java:420-422`).

2. **Capture.** The port wrapper records every byte crossing in both directions.
   Capture is on whenever a non-`NullPort` grant exists; the transcript file is
   named by a new `-transcript FILE` entry in `JLSStart.FLAGS`
   (`src/jls/JLSStart.java:759-788`) with the matching `case` in `apply`
   (`:1024-1134`).

3. **Replay.** `-serial replay:FILE` binds a `FilePort` that serves the recorded
   input bytes at their recorded stamps and asserts nothing about output. Replay
   is threadless: no ring, no host thread, no `PanelPort`.

4. **Run-mode provenance, recorded in the artifact.** Every generated artifact
   carries the mode: a `$comment` line in the VCD next to the existing one
   (`src/jls/sim/BatchSimulator.java:422`), and a second outcome line after
   `displayOutcome`'s frozen string (`:562-572`), naming the granted door and
   whether the run was `LIVE` or `REPLAY` or `NONE`.

5. **`test/jls/GoldenProvenanceRatchetTest`** - scans every committed golden -
   `test/resources/**`, `test/fixtures/**`, **and the in-source golden constants
   under `test/**.java`** - for the `LIVE` marker and fails on any hit. A golden
   produced against a human is not reproducible and must never enter the tree.

6. **The GUI console pane** - a `jls.edit` panel bound through the seam that
   already exists: the `"Runner"` thread
   (`src/jls/edit/InteractiveSimulator.java:626`) never touches Swing directly;
   every UI effect goes through `SwingUtilities.invokeLater`
   (`:745,786,828,860,921`). Emitted bytes append to the pane on the EDT.
   Keystrokes go **into the ring** (TASK-0067 deliverable 3) and never call
   `Simulator.post`.

7. **The normative edits, made deliberately.**
   `docs/simulation-semantics.md` §3's determinism invariant - "every simulated
   value is a pure function of circuit content" - becomes "...of circuit content
   **and the transcript**". `docs/vcd-interop.md` records the console decision
   explicitly instead of leaving it to be inferred from #63. Both are observable
   behavior changes and take a CHANGELOG entry under `docs/batch-interface.md`
   §6.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-032 | Determinism. Without a recorded transcript an interactive run is unreproducible and cannot produce a golden at all. |
| FEAT-034 | The guest-visible output byte stream is one of the four things the parity contract requires to be bit-identical; a transcript is how it is captured on both sides. |
| FEAT-008 | The console pane is the first new editor surface that is bound rather than bolted on; it is the worked example the decomposition plan needs. |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0068 | The console element | A transcript records bytes crossing the console; there is nothing to record before the element exists. |

TASK-0072 is a **co-requisite for one stamp kind only**: `RETIREMENT` stamps
read a retirement index that only TASK-0072 produces. `SIMULATED_TIME` stamps
work at HEAD, so this task ships without it and the retirement path lights up
when TASK-0072 lands. TASK-0021 (the UI harness) is a co-requisite for the pane
half only; the headless capture and replay do not wait on it.

## Acceptance test

`test/jls/io/TranscriptReplayTest`:

- `replayReproducesTheLiveRunByteForByte()` - record against a `PipePort`
  scripted with a fixed byte sequence, then replay the resulting transcript with
  no producer attached, and assert the emitted byte stream is identical.
- `replayIsInvariantUnderTheClockPeriod()` - **the load-bearing test**. Replay
  the same transcript with the driving `Clock`'s `cycle` doubled
  (`docs/simulation-semantics.md` §8.3) and assert the guest output bytes are
  identical while the recorded simulated times differ. This is the test that
  fails the moment someone re-indexes the log in nanoseconds or in wall clock;
  `docs/parity-contract.md` §2.4 states it as the replay invariant.
- `aTranscriptCarriesNoWallClock()` - a scan of the canonical text asserting no
  field parses as an epoch millisecond or an ISO timestamp.

`test/jls/GoldenProvenanceRatchetTest#noCommittedGoldenWasProducedInLiveMode()`
- asserts the empty baseline, in the `SocketConfinementRatchetTest` idiom.

`test/jls/ui/ConsolePaneTest#typedKeysReachTheRingAndNeverThePostQueue()` -
asserts that a simulated key event increments the ring's offer count and leaves
`eventQueue.size()` unchanged. Until TASK-0021's harness exists this runs against
the pane's model object rather than the widget.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | transcript capture, replay and the console pane | **no issue** |
| 162 | UI-layer coverage: a CI display substrate for #91 layers 2-3, dialog-construction coverage for all 23 element dialogs, and interactive-simulator smoke | depends on - the pane is a new interactive-simulator surface and needs that substrate to be covered at all |
| 91 | Automated UI test harness: assert element presence, geometry, relations, actions, menus, and mouse interactions | depends on - `ConsolePaneTest` is a harness client |
| 63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | informs - `docs/vcd-interop.md` cites #63 as the recorded rejection of live co-simulation. The console **pulls** and is never called back into, so there is no substantive conflict; the wording must be settled here rather than left to be inferred |
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | overlaps - the worked example's transcript is the first artifact this produces |

## Notes

- **The recording, not the session, is the contract.** A live session is
  threaded, unreproducible and un-goldenable; its only legitimate output is a
  transcript. Any test, golden or CI lane that consumes a live session directly
  is a defect, which is what the provenance ratchet exists to catch.
- **`displayOutcome`'s four strings are frozen** by `docs/batch-interface.md`
  §3.1. Append a line; do not edit one.
- **The VCD `$comment` line is inside a byte-compared golden, and the goldens
  are Java string constants, not files.** `WAVE_GOLDEN`
  (`test/jls/VcdExportGoldenTest.java:72`), `WAVE_STDOUT_GOLDEN` (`:109`) and
  `STIM_GOLDEN` (`:152`) are compared whole (`:229,245`), so adding a
  provenance comment edits test *source*. The provenance ratchet must therefore
  scan `test/**` including `.java`, not only `test/resources/`. Regenerate in
  one commit with the diff visible, not spread over several.
- **`InteractiveSimulator` is `jls.edit`, not core.** The pane lives beside it.
  Nothing in `jls.io` may import Swing - `HeadlessCoreRatchetTest` covers
  `src/jls/io/` once TASK-0067 adds the prefix.
- **`jls.edit` has no coverage floor at HEAD.** TASK-0019 installs one at the
  measured value; a pane added before that floor exists is untracked area, which
  is exactly the drift FEAT-008 is about. Land the pane after the floor or
  accept that it is unratcheted and say so in the commit.
- **Bounded transcripts.** A multi-hour boot emits ~10^5-10^6 console bytes.
  Write incrementally to a sink; do not accumulate in a `StringBuilder` - that
  is the exact defect TASK-0010 is removing from the waveform dump.

## Evidence

- `docs/parity-contract.md` §2.4 - the input log is indexed by retirement, the
  replay invariant, and the explicit rule that input outside retirement index is
  outside the contract.
- `src/jls/edit/InteractiveSimulator.java:626` (runner thread), `:736-810`
  (`beforeEvent`), `:745,786` (`invokeLater` idiom).
- `src/jls/sim/BatchSimulator.java:420-422` - the deliberately timestamp-free
  VCD header; `:562-572` - `displayOutcome`.
- `docs/batch-interface.md` §3.1 (outcome line), §4 (VCD profile), §6 (stability
  promise and the CHANGELOG rule).
- `docs/simulation-semantics.md` §3 - the determinism invariant this task
  amends; §8.3 - `Clock`'s `cycle`/`one` timing, which the invariance test
  perturbs.
- `test/jls/SocketConfinementRatchetTest.java:34-49` - the ratchet idiom.
- `docs/virtual-hardware-parity.md` L3 - "record and replay", and the statement
  that this layer proposes to change a normative determinism invariant.
