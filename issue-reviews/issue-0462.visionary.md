# Issue #462: TASK-0096: a drawn circuit makes a sound and hears one — through a door granted at invocation, with every test deterministic and no sound card
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this is really for

Strip the governance framing and the analog programme framing away and one honest goal
remains: **a student should be able to hear the circuit they drew, and an instructor should
be able to grade that by comparing bytes.** That goal is squarely on JLS's arc. JLS is a
pedagogy tool whose whole culture is "draw it, run it, and pin the result with a golden"
(`docs/batch-interface.md` is a declared stability contract; `VcdExportGoldenTest` pins a
byte-deterministic waveform writer). Audio is a legitimate, delightful addition to that.

The design chosen to deliver it, however, cuts along three seams that I think are the wrong
ones — and the strongest evidence is internal to the issue: **none of the three expensive
architectural commitments are exercised by the acceptance artifact.** P2, the golden WAV, is
file mode, single-threaded, on the sim thread. It needs no host door, no element type, and no
floating-point interpolation.

## Seam 1 — audio is a trace consumer, not two element types

JLS already owns the exact pipeline this needs. Watched elements and probed nets (#200)
accumulate into `BatchSimulator.getTraceSamples` as `TraceSample(long time, BitSet value)`;
`toVcd()` renders them "deterministic by construction: signals declared and dumped in
full-name order, **no `$date`/`$version` headers**, one JLS time unit per VCD time unit"
(`src/jls/sim/BatchSimulator.java:372-383`). That comment is the WAV requirement in §7.6
already solved once, in a shipped writer, with a golden test behind it.

A `-wav` flag that renders a named watched signal to PCM is *the same class of artifact as
`-vcd`* — the roadmap says so itself about STIL/WGL: "JLS has a test-vector engine, accumulated
samples (`BatchSimulator.getTraceSamples`), and a byte-deterministic waveform writer already
conformed to IEEE 1364 §18. STIL is the same class of artifact… a printer"
(`docs/capability-roadmap/sweep-06-physical-boundary.md:90`). Audio out is a printer too.

What the element framing costs that the printer framing does not:

- **Two `.jls` element tags forever.** §7.1 calls this "zero `FORMAT` version cost" — true of
  the version number, false of the surface. Every future reader of the format, every fork,
  every `AllElementsRoundTripTest` fixture now carries `HostAudioSink`.
- **The full registration tax, which the issue undercounts.** It names `SaveTags`,
  `ElementRegistry`, `Palette`, renderers, dialogs, `HdlExporter.SKIPPED`. It never names
  `HelpTopicsTest.everyPaletteElementTypeHasAMappedHelpTopic`, which derives its expectation
  from `Palette.entries()` — a palette entry with no authored HTML 3.2 page under
  `resources/help/elements/**` plus `Map.jhm` and `JLSHelpTOC.xml` rows is a **red build**
  (ARCHITECTURE.md "Adding an element today", items 13–16). Nor `AllElementsRoundTripTest`.
- **P9 exists only because of this seam.** `HdlExporter` has three buckets on master
  (`:422`, `:431`, `:436`); a printer needs no bucket at all because it is not an element.

Concretely, the reframe: `-wav out.wav -wav-signal <fullname> [-wav-rate N -wav-bits N]`
consuming the same folded time→value history `toVcd()` builds, quantizing on the tick lattice.
Nearly every prediction survives — P1, P2, P5, P6, P11, P12 unchanged; P9, P10 and the whole
registration checklist evaporate. It is days of work on shipped infrastructure rather than
two weeks, and it does not spend the file format.

## Seam 2 — the grant model makes the headline audience unreachable

This is the finding I would most want the author to sit with. §"Intended Audience" leads with
**"Students drawing and simulating circuits in the editor. A circuit that makes a sound is a
circuit you can *hear* being wrong."** But the grant is defined as *an invocation flag*, and
§7.11 says: no flag → file mode if a file was named, otherwise **silently discard**.

A student in the editor launched JLS by double-clicking a `.jls` file from the `.deb`/`.msi`/
AppImage association (README, "Installing JLS"). No flags. Therefore: **silence.** The named
primary audience gets exactly nothing from this task; only the batch grader does.

The cause is a conflation. BRIEF §12 D7, as quoted in the issue, says the grant is *"a human
grants it, never a property of the circuit file."* The issue narrows that to "granted at
invocation," and invocation-time is only the CLI's version of a human act. The GUI's version
is a menu item or a simulator-toolbar toggle: **off by default, never read from the `.jls`,
never persisted next to the circuit, re-granted per session.** That satisfies D7 exactly as
written and satisfies the audience the issue leads with.

The better precedent — and this is what #424 (sealed host byte seam) actually needs to share
with this issue — is not "one flag per door" but **one `HostGrant` session capability**: a
typed, per-run, human-originated token that the CLI parser and a GUI action can both mint,
that no load path can mint, and that the one device-acquiring class demands as an argument.
That is stronger than the P8 source-scanning ratchet, because it converts "audit one file" into
"the acquisition site cannot be called without a token that file content cannot produce" —
which is the H2 claim the issue admits its ratchet does not prove
(§11: "the confinement ratchet proves confinement, not unreachability"). A grant *object* is
the reachability argument; a grep is not. It is also the artifact #346 and #424 can genuinely
share, rather than a policy paragraph two issues promise to keep in agreement by hand.

## Seam 3 — `TickResampler` is speculative infrastructure whose programme is unrecorded

In this task, samples are taken at lattice points `t ≡ 0 (mod D)` (§7.10 Stage 1), values are
`BitSet`s, and PCM is integer-only. **Nothing in this task interpolates anything.** The
interpolation expression, its fixed evaluation order, H4, P7, and the `Double.toHexString`
golden exist solely to serve #434 and #402. The issue's own rule — "one resampler rather than
discovering it has three that disagree" — is a good rule *applied at the second consumer*.
Applied at the zeroth, it is the abstraction-before-use failure wearing the rule's clothes.

Worse, §8 mandates writing that expression into **`docs/simulation-semantics.md`**, which
ARCHITECTURE.md calls the normative spec of the simulation model and which #221's recorded
decision binds any future execution strategy to. Putting an analog resampler's
floating-point association into the normative digital semantics, before any analog code is
approved, mortgages the project's most load-bearing document.

And the programme it serves has no recorded standing. The repository's own strategy documents
say the opposite twice: *"No continuous-time solver, and none should be added"*
(`sweep-06-physical-boundary.md:83`), and *"Supporting these means being a SPICE-class solver —
a different tool, not a deeper digital model"* (`docs/capability-roadmap/README.md:1037-1041`).
ARCHITECTURE.md's "Recorded decisions" section contains no analog entry. An analog programme
may well be the right future — but it is a **trajectory reversal that needs its own recorded
decision with a revisit trigger**, in ARCHITECTURE.md's established form, before its first
task lands infrastructure in a normative spec on its behalf. Note the issue itself observes
"No issue covers the analog programme… #346 is the first record." That is the tell.

## What I would keep verbatim

- **P6, no zero-filling.** "Silence that looks like a working circuit is the worst outcome"
  is exactly right and generalizes past audio.
- **P2 asserted on bytes, and the recorded first hand review of the golden.** Correct culture.
- **H3 / P12 — integers only, no `double` item kind.** Right refusal, right reason.
- **The `Double.toString` JDK-19 note.** Worth keeping wherever a float golden ever appears.
- **§11's honesty about what a ratchet proves.** Best sentence in the issue; it is also the
  argument for replacing the ratchet with a grant object.

## What I am disregarding, and why

I am explicitly setting aside the acceptance criteria that presuppose the element/device
design: P3, P4, P7, P9, P10's palette/tags half, the `HostAudioModelTest`/`TickResamplerTest`/
`HostAudioGrantTest` deliverables, and Open Questions 1, 3, 4. They are internally coherent —
the issue is unusually rigorous — but they are rigor spent defending a shape I do not think
JLS should take on. The three execution-blocking open questions (what does a sink do with no
grant; which class holds acquisition; is the evaluation order a contract) all *dissolve* under
the printer reframing: there is no sink, no acquisition, and no interpolation.

## Recommended sequencing

1. **Ship `-wav` as a trace printer** over `getTraceSamples`, alongside `-vcd`, with `Wav.java`
   exactly as specced (integer PCM, typed chunk rejection) and the golden as P2 describes.
   No element, no format tag, no host door, no resampler. Small, aligned, immediately gradeable.
2. **File a separate host-door design issue** shared with #424, delivering one `HostGrant`
   capability mintable from the CLI *and* from an explicit GUI action, plus the one confinement
   class — so the door's precedent is set once, by an issue whose subject is the door.
3. **A drawn Speaker element, once (1) and (2) exist**, is then a genuinely small addition with
   a real story for the editor audience — and it will have the grant it needs to be audible.
4. **Record the analog programme's standing in ARCHITECTURE.md, or amend sweep-06**, before any
   task lands resampler contracts in `docs/simulation-semantics.md` on its behalf.
