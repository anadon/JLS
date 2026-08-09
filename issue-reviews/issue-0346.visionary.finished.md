# Issue #346: FEAT-045: a drawn circuit makes a sound and hears one — host audio in and out, with no solver, and deterministic in CI
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Two things are bundled here and they have very different value.

1. **"A student draws a counter and a wavetable, runs it, and hears the result."** This is
   excellent and I want it. It is the only output JLS has ever offered that reaches a human sense
   directly; every other one is a number, a picture, or a file for another tool.
2. **"Retire the governance question — does a host door survive review — in 5-7 weeks instead of
   thirty."** § Intended Audience calls this "its strategic value and the reason to build it
   first."

The whole architecture of the issue — two drawable elements, an invocation-time grant, a
confinement class, a reaction-seam ratchet, an outcome-line liveness record, a cross-issue policy
document — is downstream of (2), not of (1). And (2) is largely a false premise: **JLS already has
host doors, granted at invocation, and nobody has ever objected to them.** Meanwhile (1) is
reachable through machinery that already exists, at a fraction of the cost, with no new element,
no new grant, and no new door of any kind.

So: keep the capability, rebuild the plan. I am explicitly disregarding the stated acceptance
criteria that presuppose drawable `HostAudioSink`/`HostAudioSource` elements and a device-grant
apparatus (§1 criteria 1, 3, 4, 6; invariants 3 and 4; and the §5 criteria that depend on them).
The reasons follow.

## Reframe 1 — audio out is an export format, not an element

The issue never considers the seam JLS already cut. Batch mode accumulates
`Map<LogicElement,List<TraceSample>>` for every watched element and probed net
(`src/jls/sim/BatchSimulator.java:24,33,329`), and `-vcd` is nothing but a *renderer* of that
accumulation (`writeVcd`, `BatchSimulator.java:359`; flag plumbing at
`src/jls/JLSStart.java:107,254,271,778,1078`). A WAV is the same accumulation rendered onto a
uniform sample lattice instead of a value-change lattice. `-wav out.wav` is structurally
`-vcd out.vcd` with a different writer: one `FlagSpec` row, one static field, one `setWavFile`,
one `writeWav`, and a ~150-line RIFF writer the issue already scopes.

This is not a saving of effort only; it is a better design for a *pedagogy* tool.

- **The circuit stays a circuit.** Hearing it is an act of observation, exactly like watching it.
  The palette of an educational logic simulator is its curriculum; putting `HostAudioSink` next to
  AND and Register teaches that speakers are a circuit element. They are not.
- **It composes with everything already drawn.** `-s paramfile` can set
  `ELEMENT <name> WATCHED true` — documented as "the batch way to select outputs without editing
  the circuit" (`docs/batch-interface.md` §1). Under this reframe, *any* existing circuit becomes
  audible with no edit: the #202 RV32I core, the shift-register fixture, a student's homework from
  last week. Under the issue as written, a circuit is audible only if someone drew a sink into it.
- **It costs nothing in the registration surface.** No `LogicElement` permits change, no
  `ElementRegistry` row, no `SaveTags` row, no palette entry, no dialog, no renderer, no help
  topic, no `HdlExporter` bucket (the absorbed #462 comment enumerates all of this as "the
  registration tax"), and — the part that outlives everyone — **no new element tags in the save
  format that JLS must honor forever.**

## Reframe 2 — audio in is a test-vector source, not a read-side door

`-t` already reads an arbitrary file named on the command line and drives the top-level input pins
from it, with a fully specified grammar and timing semantics (`docs/batch-interface.md` §2,
`SigSim.initSim`). A WAV *is* a test vector: one signal, one value per frame, uniform spacing. The
two honest options are a converter (`wav` → `-t` text, which a student can read and edit — a
genuine teaching artifact) or an alternate reader behind the same seam (`-t audio.wav`, sniffed by
content the way `FileAbstractor` already sniffs `.jls` containers).

Either way there is **no `HostAudioSource` element, and no read-side host door.** The issue's
§7 names the read-side door as "the real risk" and the reason this ships first. Under this reframe
the risk is not mitigated — it is *absent*, because nothing new is being read.

## The governance premise does not survive contact with the tree

§2 asserts "The policy this feature sets is the first one of its kind in this tree," evidenced by
`git grep javax.sound` and `git grep System.in` returning nothing. Those greps establish that JLS
touches no *device* and no *stdin*. They do not establish what the § claims. JLS already writes
host files at paths a human names at invocation, in five places: `-vcd`, `-i` (PNG/JPEG/SVG),
`-export` (Verilog), `-savetext`, and the editor's ordinary save. It already reads host files a
human names: the circuit operand, `-t`, `-s`. Each is a host door granted at invocation. None has
a grant flag, a ratchet, a confinement class, or a liveness record on the outcome line — and none
needs one, because *the authority is the user's, exercised at the command line, and the `.jls`
file never chooses the path.* That is the #38 premise ("a `.jls` file is DATA") and file-mode
audio does not touch it.

What is genuinely novel is narrower than the issue says: **opening a device**, and **reading from
one**. File-mode audio — which is the whole of what CI tests (§1 criterion 2), the whole of what
the golden asserts, and the whole of what a grader or a student on a lab machine can rely on —
introduces *zero* new authority. The governance apparatus is being priced into the feature that
needs it least.

## Reframe 3 — if live audio ships, the door is asymmetric and belongs in the GUI

The issue treats playback and capture symmetrically under one heavy model. They are not
symmetric. Playback's worst outcome is noise; capture's worst outcome is a recording of a room.
And the natural home for live playback is not batch mode at all — it is the interactive editor,
where a human is sitting at the machine they launched the app on and pressing Run. That is
consent, in the same sense that `TellUser` dialogs and the FlatLaf chrome are. Batch mode then
keeps the property that actually matters to graders: **`jls -b` never opens a device, by
construction, not by flag.**

Under that split, the entire grant/ratchet/outcome-line apparatus leaves this issue and stays with
**#324**, where it is genuinely load-bearing (a byte port to `System.in`/a subprocess in *batch*
mode is a real escalation of a headless grader's authority). One door model, one place it is
defined, and it is defined by the issue whose payload requires it. The issue's invariant 4 wants
exactly this outcome; it just assigns the work to the wrong half.

## Reframe 4 — the resampler's "longest reach" is speculative, and already self-contradictory

§3 makes the resampler a separately tested unit "because every later analog output reuses it
verbatim," and §1 criterion 5 elevates that to a landing gate. But the two specifications on this
issue are not the same object. The body specifies a **frame-index map**,
`n(k) = ⌊k·T·f + ½⌋`. The absorbed #462 comment specifies a **linear value interpolator**,
`v(t) = v0 + ((v1-v0)*(t-t0))/(t1-t0)`, with a fixed floating-point association mandated so later
consumers agree in the last bit. And the same comment states that in this scope "both elements act
on this [integer divisor] lattice and nowhere else" — meaning **the interpolator has no consumer
in this feature at all.** Its only consumers are #434 and #402, which do not exist.

Worse, the float association rule sits awkwardly beside this scope's own excellent constraint that
every saved parameter is an integer and no `double` item kind is introduced. A resampler designed
entirely from digital-lattice requirements, for an analog consumer that has not yet stated its
requirements, is the classic way to get the wrong abstraction and then be held to it bit-for-bit.

Concretely: ship integer decimation (`D` ticks per sample, hold-last-value — which is what a
digital net actually does), document it in `docs/simulation-semantics.md`, and let #368/#434 own
the interpolator when a second real consumer exists and can be consulted. "One resampler, not two"
is the right rule. "Define it before its second consumer exists" is how you end up with two.

## Where this pulls against the project's arc

`docs/vcd-interop.md:18-23` records JLS's posture: *JLS runs a batch simulation to completion;
external tools consume the finished outputs.* The reframed feature is that posture, verbatim —
`jls -b -wav out.wav design.jls && aplay out.wav` is one shell line and needs no amendment to any
document. The issue instead makes amending that clause a DoD line, distinguishing "host door" from
"co-simulation transport." The distinction is real, but reaching for it is a signal: the plan is
arguing around a recorded decision rather than standing on it. The project has a mechanism for
this — ARCHITECTURE.md's "Recorded decisions," each with rationale and a revisit trigger. If a
device door is ever wanted, that is where it belongs, as a one-paragraph decision, not as the
strategic justification for a 5-7 maintainer-week feature.

Cost is the other arc question. Three issues (#308, #346, #462) now describe one deliverable, at
3-4.5 mw, 5-7 mw and 2 wk respectively, and the tracker has already absorbed and un-absorbed one
of them. The reframed version — `-wav` beside `-vcd`, a WAV↔`-t` bridge, one golden, one
round-trip test — is plausibly a week, delivers the counter-plus-wavetable synthesizer that
#308 exists for, and leaves the governance question to the issue that actually raises it.

## What I would keep, unchanged

The student framing (#308's "a counter, a `Memory` and a `Register` produce a tone"); integer PCM
only with no float and no compressed formats; the no-`double`-item-kind rule; typed rejection at
every RIFF chunk boundary with **no zero-filling** (that failure mode analysis is the best writing
on the issue); the byte-identical golden WAV as the acceptance artifact; and the honesty about
44.1 kHz not being expressible on a decimal tick lattice, documented rather than bug-reported.

## Verdict: rethink

The capability is right and under-served by the plan built for it. Rebuild the issue as: **"a
watched signal can be rendered to WAV, and a WAV can drive an input pin"** — an export format and
an import bridge on seams JLS already owns, with no new element, no new grant model, and no device
code. Live playback, if wanted, becomes a small separate GUI affordance. The host-door governance
question returns to #324, which is the issue whose payload genuinely needs the answer.
