# Issue #698: TASK-C534-2: a programmable word generator plays a stimulus table from the canvas, and the same program recorded interactively and headlessly is byte-identical
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This task (part of FEAT-C23-5 / #534, itself PF-5 of capstone CAP-23 / #504)
asks for a drawable "programmable word generator" element whose interactive
and headless VCD recordings are byte-identical. Two problems block it as
filed: it needs interactive-mode VCD capture that does not exist anywhere in
the codebase today and that a sibling task appears to own instead, and it
never acknowledges that a fully working, placeable, save/load-round-tripping
stimulus-table element (`SigGen`) already ships in this repo. Both raise real
questions about scope, cost, and duplicate work rather than nitpicks.

## Findings, most severe first

### 1. AC-3 requires interactive VCD recording, which does not exist, and a sibling task appears to own that plumbing — yet `ordering_after: []` declares no dependency

VCD export today is exclusively a `BatchSimulator` capability:
`toVcd()`/`writeVcd()`/`setVcdFile()` all live in
`src/jls/sim/BatchSimulator.java`, and the only caller is
`src/jls/JLSStart.java` lines 254/273, gated behind the batch `-vcd` flag.
`grep -rn "toVcd\|writeVcd\|setVcdFile" src` outside `BatchSimulator.java`
returns nothing, and `src/jls/edit/InteractiveSimulator.java` and
`src/jls/edit/Trace.java` contain zero references to VCD at all. So "the VCD
recorded from an interactive run" (AC-3's own wording) is not a comparison
between two existing recorders — it is new plumbing this task would have to
invent from scratch.

That plumbing looks like it was assigned elsewhere. Sibling task #700
(TASK-C534-3, same `part_of_feature: 534`) states its outcome as "the
capture is exported through the existing VCD writer" and its AC-2 is
literally "a test asserts no second trace writer exists for instrument
captures" — i.e. #700 is explicitly the task guarding against exactly the
duplicate-VCD-writer risk that #698 would create if implemented in
isolation. #700 also declares `ordering_after: [TASK-C534-1, TASK-C527-2]`,
showing the authoring process does track cross-task ordering when it matters
— but #698, which needs the same not-yet-built interactive-VCD capability,
declares `ordering_after: []`.

**Recommendation:** either state explicitly that #698 owns building
interactive VCD capture (and cost it accordingly — this is materially more
than "1-1.5 mw"), or add an `ordering_after` dependency on whichever task
actually lands that capability (#700, or a shared PF-1 chronogram
prerequisite), and have #700 build it once, not twice.

### 2. The issue never mentions `SigGen`/`SigSim`/`TestGen` — extensive, directly relevant prior art already in the codebase

`src/jls/elem/SigSim.java` is an abstract base (`permits SigGen, TestGen`)
that already parses a textual per-signal stimulus spec (`signal value [for N
| until T value]... end`) and posts timed events to the simulator.
`src/jls/elem/SigGen.java` is already: placeable via the ordinary element
path (registered in `Palette.java`, `BuiltinElementRenderers.java`,
`SigGenDialog.java`, `SigGenRenderer.java`), `Editable`, and saved/loaded via
the standard `Attribute` mechanism — already covered by
`AllElementsRoundTripTest` (line 99). Critically, `InteractiveSimulator`
(`runSim`, ~line 590-606) and `BatchSimulator.addTestGen` both drive stimulus
generation through the exact same `SigSim.initSim(sim, input)` code path
parsing the same text, which is most of the way to AC-3's determinism claim
already, for the generator that exists today.

The issue's Outcome section describes "a stimulus table — value, width,
hold" without saying whether this is meant to extend/replace `SigGen`, wrap
it with a new table-widget UI, or build an unrelated parallel element (e.g.
one wide-bus output rather than SigGen's per-named-pin addressing). That
ambiguity is load-bearing: it changes both the honest cost of the task (far
less if it's a UI layer over `SigSim`, far more if it reimplements event
posting from scratch) and the duplication risk — two element types serving
overlapping purposes wouldn't be caught by `ElementRegistryTest`'s
per-class totality check, only by a human noticing.

**Recommendation:** name `SigGen`/`SigSim` in the issue and state explicitly
whether this task extends them or supersedes them; if it supersedes them,
say what happens to `SigGen` (deprecated? merged? kept for simpler use
cases?).

### 3. AC-5's "AC-5 baseline" is undefined and forward-references a measurement the capstone says must land *before* this task is funded

No `ChronogramClosedCostTest` (or any AC-5 tolerance number) exists anywhere
in the repo (`grep -rln ChronogramClosedCostTest` — no hits under `test/`).
CAP-23's (#504) own Completion Criteria state: "KC-23-1's tap-cost
measurement recorded from the demo slice before PF-3..PF-6 are funded" — and
PF-5 (this task's parent, #534) is one of PF-3..PF-6. By the capstone's own
governance, #698 (and #534) should not be funded until that measurement
exists, yet #698 declares `ordering_after: []`, and neither #698 nor #534
cites the KC-23-1 gate at all. As written, AC-5 asks an implementer to test
against a tolerance nobody has recorded yet.

**Recommendation:** either cite KC-23-1 as a blocking precondition, or state
explicitly (with reasoning) why PF-5's idle-cost claim doesn't need
KC-23-1's number and can use its own baseline instead.

### 4. AC-1, AC-2, AC-4, AC-5 name no test, unlike AC-3 and unlike the pattern set by the parent capstone

AC-3 pins a concrete oracle (`InstrumentGoldenTest`). AC-2 only says
"asserted against a fixture with known expected transitions" — no class
named, so any private, hand-picked fixture technically satisfies the letter
of the AC even if it doesn't stress edge cases (e.g. mid-hold reprogramming,
zero-hold rows, max-width values). CAP-23 (#504) itself names a test for
nearly every AC (`HazardDiagnosisWalkthroughTest`, `RewindEqualsReplayTest`,
`InstrumentGoldenTest`, `ViewerSyncTest`, `ChronogramClosedCostTest`,
`CauseChainCompletenessTest`); this task partially inherits that discipline
(AC-3) but drops it everywhere else, leaving those criteria gameable by a
minimal fixture that trivially passes.

**Recommendation:** name a test class for AC-1, AC-2, AC-4 and AC-5, the
same way AC-3 does.

### 5. Minor: sibling band_mw sums slightly overshoot the parent feature's declared range

#534 declares a 3-4 mw band for all of PF-5. The three filed sub-tasks are
#696 (1.5-2), #698 (1-1.5), #700 (1), summing to 3.5-4.5 mw — the high end
exceeds #534's stated ceiling by 0.5 mw. Not fatal on its own, but combined
with finding 1 (this task's true cost is understated because interactive
VCD plumbing isn't priced in), the actual total is probably higher still.

### 6. Minor: "value, width, hold" is undefined vocabulary

Contrast `SigSim`'s existing grammar, which is concrete and tested
end-to-end. The issue's three column names are never defined: is "width"
the bit-width of that row's value, or the element's whole output port
width (fixed once, not per-row)? Is "hold" a duration in simulation time
units, matching `SigSim`'s `for`/`until`, or something else? Left
undefined, "the element drives its outputs from it" in AC-1 could be
satisfied by an implementation with almost any semantics, since no fixture
is named to pin the interpretation (see finding 4).

## What's solid

- **AC-4's "no new trace format" scoping is realistic and already
  reflected in the codebase's design**: `BatchSimulator.toVcd()`'s own
  comment states the format deliberately omits `$date`/`$version` headers
  "so the same run always produces the same bytes" — the batch half of the
  byte-identical claim is low-risk because the existing VCD writer was
  already built with this invariant in mind.
- **task_id/part_of_feature/labels are internally consistent** with #534
  and the CAP-23 naming scheme; the task correctly cites "CAP-23 AC-3" for
  its own AC-3.
- **AC-1's "ordinary element path, round-trips byte-identically" framing**
  is the right bar to set, given `AllElementsRoundTripTest` already exists
  as the enforcement mechanism for exactly this property on other elements.
