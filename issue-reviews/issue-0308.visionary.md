# Issue #308: CAP-10: a circuit drawn in JLS makes a sound you hear from the host speakers, and renders to WAV when the fidelity you chose is too expensive to play live
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the 260 lines of machine block, DAG re-walks and cost bands and one
sentence is left, and it is a good one: **JLS's output surface has never
reached a human sense.** Every artifact the tool produces today is a number
(`BatchTracePrinter`), a picture (`-i`), a file (`-vcd`, `-export`) or a
pane. A first-year student who draws a counter, a `Memory` wavetable and a
`Register` and *hears a tone* has closed the draw→feel loop in a way nothing
else in the tracker does. That is a real, cheap, identity-strengthening
capability, and the issue is right that it is the fastest capstone-shaped
artifact in the plan.

It is also the vehicle for a genuinely load-bearing governance decision. At
HEAD `grep -rn "System.in" src/` is empty and `#38`'s hardening premise is
that a `.jls` file is inert data. The first host door in the project's
history deserves to be decided deliberately, early, cheaply. Open Question 1
and §1 step 4 are the best parts of this issue.

Everything else in it belongs somewhere else.

## The structural problem: two capstones fused by one demo circuit

The required set is FEAT-045 (#346, 5–7 mw) + FEAT-047 (#367, 2–3) +
FEAT-046 (#351, 17.5–26) + FEAT-048 (#368, 4–6) = 28.5–42 mw, and #368
declares `blocked_by: [331, 351]` with #331 priced at 21–33 mw — so the real
closure chain is roughly 50–75 maintainer-weeks. The value-delivering slice
is stated in the issue's own Cost section as **3–4.5 mw**, and it "satisfies
AC-1, AC-2 and AC-3 outright."

An outcome whose demonstrable payoff is 6% of the cost of closing it is not
one capstone. It is two, welded together by exactly one artifact: §1 step
5's R-2R ladder into an LC reconstruction filter. Remove that one circuit
and FEAT-046, FEAT-048, FEAT-049 and the entire analog spine fall out of the
required set. What remains — sink, codec, tick resampler, `-wav`, the
invocation grant, and a declared time base — is the whole audible outcome,
and it is dependency-free.

**This pulls against a stance the tree records repeatedly and derives from
scratch each time.** `docs/capability-roadmap/README.md:1036-1041`:
"Continuous-time and analog … Supporting these means being a SPICE-class
solver — a different tool, not a deeper digital model."
`sweep-03-elements-and-hdl.md:635-638`: "a different tool class, not a bigger
version of this one." `sweep-02-timing.md:726-727`: "Consuming them requires
an analogue solver, which is a different tool (SPICE), not a deeper digital
model." `docs/grand-architecture.md:419-443` excludes in-house HDL simulation
on the identical ground. Every one of those documents is **in the tree**;
the CAP-10 source document (`docs/plan/capstones/CAP-10-audio-output.md`) is
not — `docs/plan/` does not exist at the checked-out HEAD, and neither does
`docs/virtual-hardware-parity.md`, cited for D7. The in-tree corpus and the
out-of-tree corpus disagree about whether JLS becomes a SPICE-class solver,
and this issue silently sides with the one that is not in the repository.

I am not adjudicating that here. I am saying: **do not make the audible
outcome hostage to it.** Whatever the analog programme's fate, "hear your
circuit" should not be blocked on it.

## Reframing 1 — audio is a trace consumer, not an analog capability

The most elegant route to the outcome is already built and the issue never
considers it. `BatchSimulator` accumulates `Map<LogicElement, List<TraceSample>>`
over `Watchable` elements and probed nets (`src/jls/sim/BatchSimulator.java:315-332`,
`TraceSample(long time, BitSet value)`). `toVcd()` (`:372-400`) turns that map
into an IEEE 1364-2001 dump that is **deterministic by construction** —
"signals are declared and dumped in full-name order, no `$date`/`$version`
headers" — and pinned byte-for-byte by `VcdExportGoldenTest`.

A WAV writer is the same class of object as that VCD writer: a pure function
from a folded time→value history to bytes. Not a new subsystem — a second
printer over an existing, already-deterministic seam.

Consequences that make problems disappear rather than solve them:

- **AC-1 stops being a new claim.** Byte-identical PCM across the 4×2 matrix
  is the guarantee `toVcd()` already delivers on the same data structure, by
  the same mechanism (deterministic ordering over integer sample values).
  KC-10-4 becomes near-unreachable rather than a risk that "every later
  golden becomes a tolerance comparison."
- **AC-3's 44,100 samples/s budget largely evaporates** for the file path.
  Rendering is offline over an accumulated trace; the sim need not run in
  real time to produce a correct WAV. Only the *live* path has a rate budget,
  and the issue's own measurement (~209,000 samples/s, 4.7× real time) says
  it clears with margin.
- **The `-vcd`→`-wav` symmetry is the documentation.** `docs/batch-interface.md`
  already specifies a VCD profile as a stability contract; a WAV profile is
  one more section in the same document, in the same voice, with the same
  golden discipline.

Concretely: `-wav out.wav --audio-signal <fullname> --rate 44100 --width 16`,
implemented as `BatchSimulator.toWav(...)` beside `toVcd()`, tested by
`WavExportGoldenTest` beside `VcdExportGoldenTest`. That is days, not weeks,
and it reuses the project's strongest existing discipline instead of opening
a new one.

## Reframing 2 — make AC-2 true by construction, not by test

AC-2 is an elaborate proof obligation ("assert there is no code path —
reflection, `ServiceLoader`, or otherwise — by which the device opens without
the grant") that exists **only because the issue decided the sink is an
element that owns a device**. Negative-capability assertions over a whole
binary are the hardest kind of test to keep true; this one would have to be
re-proved on every refactor forever.

Split declaration from binding:

- The **drawn** `Speaker` element is pure. It declares intent and rate,
  behaves exactly like a watched `Pin`, and touches nothing. It is
  `Watchable`, nothing more. No `javax.sound` import anywhere in `jls.elem`.
- The **binding** lives in `JLSStart`/the editor: `-wav` binds the declared
  speaker to a file, `--audio-out` binds it to `javax.sound.sampled`.

Now "a `.jls` file can never open an audio device" is not a test result, it
is a **type-level fact**: no class reachable from the load path has an audio
dependency. `HeadlessCoreRatchetTest` already enforces exactly this shape of
invariant for AWT/Swing (`ARCHITECTURE.md`, `jls.sim`) and can be extended by
one package rule instead of by a bespoke reflection audit. The student still
draws a speaker; §1 step 4's diagnostic can still name the drawn element,
because the element is what declared the intent.

This also retires the element tax the issue prices at "~65 lines across 12
files": a pure `Watchable` output element skips `initSim`/`react` complexity,
skips the delay model, skips HDL export concerns.

## Reframing 3 — design the host *door*, not the audio door

The issue treats "does a host audio door survive review" as a one-off
question. Look at the arc and it plainly is not:

- CAP-11 (#303) wants microphone in — same door, read side.
- #213/#215 carry a drawn circuit to a bitstream on a named board; the
  `-serial stdio` model the issue itself cites is a second door.
- The collaboration stack (#163/#170) is a third untrusted boundary.
- `docs/extension-points.md` already types six seams and #223 requires a seam
  to be catalogued before it is contributed to.

Designing a bespoke audio grant means re-litigating the identical review for
door two, three and four — and D7's prize property, "a closed and known set
of host doors," is exactly the thing a per-door design cannot guarantee.

The reframing: **one capability-grant vocabulary, declared at invocation,
enumerated in a single table, with audio-out as its first tenant.**
`--allow audio-out`, `--allow audio-in`, `--allow serial:/dev/ttyUSB0`; one
row per capability in `docs/extension-points.md` and one section in
`SECURITY.md`; one test asserting the default-deny path for every registered
capability, so door two is covered by door one's test. This costs perhaps a
week more than the bespoke door and retires the governance question for the
whole project rather than for one element — which is precisely the argument
the issue makes for doing the review in week 4 instead of week 25, applied
one level up.

## Reframing 4 — the drawn rung should be a drawn *digital* filter

§1 step 5 (R-2R + LC, live, with a trace on the filter node) is what drags
17.5–26 mw of nodal solver, 4–6 mw of A2D/D2A bridges and 21–33 mw of device
models into a capstone about hearing things. Ask what the step is *for*: to
let a student hear that the circuit they drew shapes the sound.

An LC tank is not the only way to make that point, and for a schematic-first
**logic** simulator it is the worst way — it teaches through an engine the
tool does not have, about a domain the tree has three times declared out of
class. The alternative that keeps the entire pedagogic claim:

**Let the student draw the reconstruction filter digitally.** An FIR or a
sigma-delta/PWM stage built from the `Adder`, `Register`, `Memory`, `Splitter`
and `ShiftRegister` elements that already exist. Draw a 4-tap moving average
in front of the speaker; hear the aliasing go away. Draw it wrong; hear it
come back. Change one coefficient in the `Memory`; hear the timbre change.
Turn the PWM carrier down; hear the buzz appear.

Measured against the stated goals this is *better*, not merely cheaper:

| | Issue's step 5 | Drawn digital filter |
|---|---|---|
| Cost | 17.5–26 + 4–6 + (21–33 ordering) mw | 0 new features |
| Determinism | floating point across 4×2 matrix (AC-6 exists solely for this) | integer; AC-1's equality assertion covers it |
| What it teaches | analog network theory | sampling, quantization, DSP built from gates |
| Fits the tool's identity | no — "a different tool class" | yes — it is what the element set is *for* |
| Real-time budget | AC-4 + factorization-cache hit rate + KC-10-3 | inside AC-3's existing 4.7× margin |

The step-6 class-D THD lab, at 2.8–4.9 minutes of wall clock per audio
second, is not a capability — it is an argument that the analog rung does not
belong in an audible capstone, made by the issue against itself.

If the analog programme ships on its own merits (via #305/#309, where it
belongs), an analog output stage becomes a *later, additive* rung on an
already-closed CAP-10. Nothing is lost by sequencing it that way; a
50–75 mw block on the critical path of a 3–4.5 mw payoff is lost by not.

## What I am disregarding, and why

Explicitly setting aside, rather than failing to satisfy:

- **§1 steps 5 and 6, AC-4, AC-6, AC-7, AC-8, KC-10-3, KC-10-5**, and the
  FEAT-046/047-adjacent halves of AC-5. All of them exist to serve the drawn
  *analog* rung. Under this reframing they are not struck by a REPLAN
  argument about beneficial scope — they simply belong to #305 and #309,
  which already own the solver and its determinism gate. The issue's own §3
  names the consequence of keeping them: "two acceptance criteria reach
  beneficial scope, and that is a closability hazard," and #368's
  `blocked_by: [331, 351]` means a capstone graded "cheapest in the whole
  plan" cannot close until a 21–33 mw feature it calls optional has landed.
  The right fix is not a strike clause. It is a different seam.
- **The 28.5–42 mw standalone band.** It is arithmetically correct and
  strategically irrelevant, because it is the cost of a required set I am
  arguing should have two members, not four.
- **The DAG bookkeeping** (three REPLANs, one adjudication, one coverage
  audit, one D13 ruling — all on ordering edges between features that would
  not be in this capstone's required set). Roughly half the issue's mass and
  all four of its comments are process about a dependency structure that a
  different scope decision dissolves.

## What survives intact, and should be built next week

The audible outcome, the host-door review, and the demo slice. Concretely,
the version of CAP-10 I would endorse without reservation:

> **CAP-10 — a drawn circuit reaches a human sense.** Required: FEAT-045
> (#346) reduced to the pure-element + trace-consumer shape of Reframing 1/2,
> and FEAT-047 (#367), because a sample rate over dimensionless ticks
> (`docs/simulation-semantics.md:26`) has no referent. Steps 1, 2, 3, 4 and 7
> unchanged; step 5 becomes a drawn digital filter; step 6 is deleted.
> AC-1, AC-2, AC-3 unchanged. Standalone band: 7–10 mw. Closes without a
> single line of analog code.

That is the issue's own demo slice promoted to be the whole capstone, plus a
door design that serves the next three doors, plus a drawn rung that makes
the tool more itself instead of less. It strengthens the project's arc — the
headless core (#77), the `Watchable`/trace seam, the batch-contract culture,
the extension-point catalog — where the issue as written attaches all of that
to an analog programme the in-tree roadmap has three times placed outside the
tool's class.

Endorse the outcome. Reframe the required set out of the analog programme.
