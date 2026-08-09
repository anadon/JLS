# Issue #722: TASK-C539-2: N clock cycles of a selected canvas region become an animation with signal-value overlays, from a recorded run and no other source
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this task is really for

Sibling #720 owns the encoder; #722 owns everything else, and "everything else"
is one genuinely new capability wearing a costume: **render the canvas as it
was at simulated time t, from something other than the live simulator.** The
animation is a `for` loop around that. The GIF is a container. The region is a
crop. Strip those away and #722 is asking JLS to grow a *time-addressable
figure renderer* — and that primitive is worth far more than the artifact this
task spends it on.

I am reviewing the task under that reading, which means I disregard AC-1 and
AC-2 (the APNG/GIF pairing and the size budget) as packaging decisions
belonging to a layer JLS should not own, and treat AC-3 and AC-4 as the load-
bearing content. They are the good half: *values come from a run that already
happened* and *frames come from the figure renderer, not a screen grab*.

## The duplication nobody in this chain has noticed

CAP-23 (#504) already owns this primitive, from the other side:

- **PF-6**: "time-travel display via deterministic re-simulation to a target
  timestamp (no per-element checkpoint)", with **AC-2 `RewindEqualsReplayTest`**
  — displayed canvas state after cursor rewind is byte-identical to a fresh run
  truncated at T.
- **PF-4**: "single-event / wavefront stepping mode with **animated
  propagation along wires**."

That is #722's requirement — canvas state at cycle k, provably not live-sim
mush, animated — filed under a different capstone, with a stronger invariant
(byte-identity against truncated replay, over randomized fixtures) than #722's
AC-3 (a ratchet test asserting no code path *reads* live state, which proves an
absence, not a correspondence). And #508 places CAP-23 in **keep-core** and
funds a slice now (direction item 6, "debug-loop parity"), while placing CAP-24
in keep-strategic and recommending **PF-4 be cut**. So the tracker is currently
positioned to build canvas-state-at-time-T twice: once as a funded interactive
debugger seam, once as an unfunded frame source for a movie.

Cut along that seam instead. One primitive, `stateAt(run, t)` → the render
path, with two consumers: the chronogram cursor (interactive) and the figure
exporter (headless). #722 then costs a loop and an output mode, and CAP-24's
animated figure inherits CAP-23's determinism proof for free.

## Reframing 1: inputs are the contract; the "recorded run artifact" is not needed

AC-3 makes the recorded-run artifact the whole point of the task ("and no other
source"), and the adversarial review is right that no such type exists in the
tree. But the deeper observation is that **JLS does not need one.** This
simulator is deterministic by construction and by ratchet:
`docs/simulation-semantics.md` is normative, `BatchSimulationGoldenTest` /
`SequentialGoldenTest` / `VcdExportGoldenTest` pin it, saves are canonical
(#166, `DeterministicSaveTest`), and CAP-23 PF-6 stakes an entire feature on
"replay to T reproduces state exactly." For such a system, *the run is a pure
function of (circuit bytes, stimulus, time window)*.

So the consistency guarantee CAP-24 §3 risk 4 actually wants — "the animation
cannot come from a different run than the other figures" — is obtainable
without inventing a recording format:

> every exported figure carries a provenance header of `circuit sha256 +
> stimulus sha256 + time window`, and a test regenerates the bundle and asserts
> the artifacts are byte-identical and their provenance triples equal.

That is stronger than #722's AC-3. AC-3 can only prove no live-state read; the
provenance triple proves the *figures agree with each other and with the
circuit that shipped*, which is the actual failure mode ("the figure-vs-behavior
mismatch every instructor has shipped at least once", CAP-24 Intended Audience).
It also removes an unbuilt, unratified dependency (#498 §7.2 is explicitly
non-normative) from the critical path of a task banded at 1–1.5 mw. Recording
becomes an optimization for expensive runs, not a precondition for figures.

## Reframing 2: the region must be text, not a mouse gesture

"An instructor selects a canvas region" is a GUI act, and it is the one thing
in this pipeline that **cannot be committed to a course repo or re-run in CI** —
against a capstone whose §1 step 4 is "export the same bundle on Linux, macOS
and Windows CI" and whose premise is "figures live in course repos under
version control like everything else in this project." A hand-dragged rectangle
is not versionable, so the handout is not regenerable, so the whole
determinism apparatus guards an artifact whose scope is re-authored by hand
every time the circuit moves.

JLS already has the vocabulary to fix this. Elements carry **stable ids**
(#165; `Element.getStableId`, the addressing scheme the entire op layer uses),
and **`SubCircuit` is a first-class, named, savable region**. So define the
figure's subject as either a subcircuit name or a set of stable ids, and the
GUI gesture becomes an *authoring* action that emits that spec — not the
producer of the artifact.

Push it one step further and the whole of PF-6 falls out: a checked-in
**figure spec** (source circuit, stimulus, region, window, formats), and
`jls -figures figures.spec` regenerating every artifact headlessly. The
handout bundle stops being a command with flags and becomes a file in the
course repo that CI rebuilds — which is what an instructor with a lab that
changes every semester actually needs. The region-selection UI, the bundle
command, and the animation all collapse into "author a spec / run a spec."

## Reframing 3: the pedagogically right artifact is scrubbable, not looping

A GIF of a hazard plays past the hazard. The teaching moment is *stopping* on
the runt pulse. Since frames are SVG from an already byte-identical path
(`CircuitRenderer.exportImage` fixes the `defs` prefix and imposes a total draw
order precisely so goldens hold, `SvgExportTest#exportingTwiceIsByteIdentical`),
N frames plus a slider is a single self-contained HTML/SVG file: diffable,
zoomable, keyboard-navigable, embeddable in any course site, and produced with
no encoder at all. For the two cases that genuinely need a movie — a README
loop, a slide deck — `ffmpeg`/`magick` over the frame directory produces APNG,
GIF *and* MP4, dissolving CAP-24 Open Question 4's exclusion rather than
enforcing it. This is the same stance CAP-24 already took one feature earlier
(KC-24-3: the WaveJSON file is the product, the renderer is a dev-time pin) and
the same stance ARCHITECTURE.md records for every external toolchain
(iverilog, GHDL, Yosys, ELK sit outside the jar and stay there).

I concur with the frame-sequence direction in the #539 visionary review; what I
add is that under it, #722 is not a smaller task — it is a *different* task,
and #720 has no reason to exist.

## Does this strengthen the arc, or pull against it?

Pulls against it, as filed. #508's measurement is adoption zero and the live
user base on someone else's fork; its two-quarter direction is grading
integrity, prominence, accessibility, debug-loop parity, distribution. #722 is
a leaf on the one CAP-24 feature that review recommends cutting, gated on a
REPLAN that (per #505's and #508's own comments) has not happened, sequenced
behind an encoder task, and depending unstated on an unlanded print renderer
(#536). Everything durable inside it — time-addressable state, provenance,
versioned figure scope — either already belongs to CAP-23 or is cheaper than
the packaging it is wrapped in.

## Concrete recommendation for the #505 REPLAN

1. **Adopt #508's cut of PF-4 as a shipped animation feature.** Close #720 and
   #722 as superseded, not deferred.
2. **Record the primitive where it belongs:** CAP-23 PF-6 delivers
   `stateAt(run, t)` → render path, and CAP-24 declares the animated figure a
   *consumer* of it via REPLAN on both issues (CAP-23 §5 already has the
   adopt-don't-parallel rule for exactly this).
3. **File one small task in CAP-24's lane instead:** a headless
   `-frames out/frame-%04d.svg` mode over a figure spec (region by stable ids
   or subcircuit name; provenance header of circuit+stimulus+window), plus an
   informative `docs/figure-interop.md` in the shape of `docs/vcd-interop.md`
   carrying the ffmpeg/magick/`\animategraphics` recipes. ≈0.5–0.7 mw, no jar
   growth, and byte-identity extends *to* the animation instead of excusing it.
4. **Promote the figure spec into PF-6's definition** so the bundle is a
   versioned file rather than a command invocation.
5. If an in-jar single-file animation is still wanted after that, make it the
   **scrubbable SVG/HTML** built from the same frames — a text artifact, no
   encoder, no size budget, no format hedge.

## What I would keep verbatim from #722

AC-4. "Frame rendering reuses the print/figure render path rather than
screen-grabbing the editor, so an animation and a static figure of the same
region agree" is the one sentence in this chain that states an architectural
invariant rather than a deliverable, and it survives every reframing above. It
should be lifted out of this task and recorded on #505 as a capstone-level
rule: **there is exactly one renderer; every figure kind is a theme and a
composition over it.**
