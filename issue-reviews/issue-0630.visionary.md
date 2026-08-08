# Issue #630: TASK-C523-2: where the isomorphism cannot be proven, the parity claim is narrowed in writing before release — the artifact says what parity means and the docs name the fixture classes it holds for
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Stripped of its provenance chain (#307 AC-5 → #298 verification tier → #523 AC-4),
#630 asks for one thing: **the guarantee JLS publishes must be no wider than the
evidence JLS holds.** That is not a new discipline in this project. It is the
project's single most consistent editorial habit, and #630 is roughly its fifth
instance:

- `README.md` lines 53–60 — "Note the scope of each guarantee": checksums identify
  bytes, attestation proves origin, and installers are explicitly *not*
  byte-reproducible while the jar and `bom.json* are.
- `README.md` lines 315–320 — the forward-compatibility caveat that JLS 4.1
  silently drops RLE memory contents.
- `ARCHITECTURE.md` "Recorded decisions" — every non-goal carries a rationale and
  a named revisit trigger.
- `docs/standards-adoption/01-iec-ieee-symbols.md` lines 25–58 — a fully worked
  instance of exactly what #630 wants: "The claim to make is: …", then
  "Explicitly **not** claimed:", then a clause-by-clause conformance matrix where
  each row names the test that pins it, and the closing line "Without that
  document there is no conformance claim, only rectangles."
- `src/jls/hdl/VerilogEmitter.java:70-71` — the emitted banner already narrows a
  parity claim in the artifact: "JLS simulates two states plus HiZ: this module
  drives 0/1/z, never x."

So the *goal* is squarely on the project's arc and I endorse it. The framing is
where it pulls sideways: #630 is written as a bespoke, one-format, one-release
chore, and the repo already contains a better-generalized mechanism for the same
end that this task neither cites nor reuses.

## Reframing 1 — put the claim on the seam, not in the KiCad emitter

`docs/extension-points.md` types `hdl.exporter` over `jls.hdl.HdlEmitter`,
cardinality **many**, with #213/#215 extending it and #212 admitting external
providers. `HdlEmitter` today declares exactly two methods: `emit(HdlModel)` and
`fileExtension()`. AC-1 as written adds one hand-typed sentence to one emitter's
`header()` method and pins it with that emitter's golden — buying honesty for
`.net` only, while Verilog, VHDL, the `.pcf` board export, and every future
exporter (IP-XACT per `docs/standards-adoption/08-ipxact-export.md`) each
re-litigate it by hand or forget.

Concretely: add a third method to the seam contract — `ParityClaim claim()`,
returning a typed record (claim sentence, value-domain deltas, the conformance-doc
anchor). Render the banner *from* the claim rather than writing it beside the
claim. Then:

- Every exporter, present and future, first-party and external, must state a claim
  to compile. "An exporter that says nothing about what it guarantees" becomes
  impossible, not merely discouraged.
- The golden still pins the emitted sentence, because the sentence is derived.
- The catalog row in `docs/extension-points.md` grows a "claim" column and
  `ExtensionPointCatalogTest` — which already cross-checks constants against that
  table **in both directions** — enforces it.

Same effort, one format's worth of scope becomes the whole export surface's.

## Reframing 2 — generate the matrix from the check; do not cross-check prose against corpus

AC-2, AC-4 and AC-5 are three faces of one drift problem: a hand-written list of
fixture classes falling behind the corpus it describes. This repo's answer to
drift is never "a review step" — it is a two-directional catalog test
(`ExtensionPointCatalogTest`), a ratchet (`HeadlessCoreRatchetTest`,
`NotificationRatchetTest`, `NullMarkedRatchetTest`, `SocketConfinementRatchetTest`),
or a golden (`VerilogExportGoldenTest`, `PcfGoldenTest`).

Take the shortest route: `NetPartitionIsomorphismTest` (#627) already computes,
per fixture class, exactly the fact the document needs — proven / unprovable with
a named KiCad-or-gEDA semantic / not yet covered. Have it emit that as a
machine-readable record, render the conformance matrix from it, commit the
rendered file, and byte-compare it in `mvn verify` the way goldens are compared.
AC-4 then cannot fail: a new fixture class changes the rendering, the comparison
goes red, and the author must classify it. AC-5's "one place" is automatic
because there is only one producer.

This also lets AC-3 shed its weakest clause. "**a test or a documented review
step**" offers an escape hatch into a human promise, and a human promise is the
one gate this repository does not otherwise use. Strike the review step.

## Reframing 3 — the honesty belongs where the user is, not in a release note

This is the alternative #630 never considers. As written, the narrowing lands in
two places nobody reads at the moment of risk: a comment header inside a file
KiCad consumes and discards, and release documentation read once. Meanwhile the
project's own established idiom for "the user must be told a limit" is a
structured, actionable diagnostic at the point of use — the `LoadError` taxonomy
(#58), `NEWER_FORMAT` refusing rather than misparsing (#79), the single
`jls: error: …` line on stderr (#42).

Apply that idiom: `-export out.net circuit.jls` on a circuit containing a
construct belonging to an unproven class prints one line naming the construct and
the class, driven by the *same* table that renders the doc. That is #307's
migration-report idea, which #523 explicitly parks on the readback direction,
turned around and applied to the emit direction — where it is cheaper, because
the emitter already knows every element it walked. A student who exports a
tri-state bus and hears nothing has been told the claim is narrow only in a file
they will never open.

I would keep AC-1's header sentence (it is cheap and it is what the Verilog
emitter already does) but I do not believe it is where the honesty does its work.

## Two smaller course corrections

**"Before release" is the wrong trigger.** Releases here are a tag push into an
automated workflow with reproducible builds and attestations. A criterion gated on
a pre-release human step is the only unautomated gate in a repository full of
automated ones, and it will be the one that is skipped. Make it *before merge*,
inside `mvn verify`.

**AC-3's repo-wide prohibition needs a reserved phrase, not the word "parity".**
`grep -rn parity docs/` already returns sixteen unrelated uses: picocli CLI
parity, performance parity with Digital in
`docs/capability-roadmap/keystone-c-performance.md`, the distinctive-shape symbol
"parity test" in `01-iec-ieee-symbols.md`. A ratchet over the bare word is a
false-positive machine and will be weakened or deleted within a month — which is
worse than never writing it. Reserve **"net parity"** (or "export parity") as a
term of art, require every occurrence to link the conformance-doc anchor, and
leave roadmap prose alone.

## Sequencing: this should lead, not trail

`ordering_after: [TASK-C523-1]` gets the dependency backwards for the part that
matters. The conformance document is the artifact whose *absence* is precisely why
#307, #298 and #366 each assumed someone else owned the partition check — #523's
own body says "No filed row owns it today." Write `docs/export-conformance.md`
first, in the `01-iec-ieee-symbols.md` house style, with every row marked
unproven, and let #627 flip rows green as it lands. The document then constrains
the emitter's design (which classes can be claimed at all) instead of describing
it after the fact. Only AC-1's golden-pinned header genuinely needs the emitter to
exist.

One last alignment note: the value-domain boundary — two states plus HiZ, no x, no
analog, no timing, no electrical — is not KiCad-specific. It already appears
hand-written in `VerilogEmitter.header()`, in `VhdlEmitter.header()`, in
`README.md`'s `-export` description, and in `ARCHITECTURE.md`'s equivalence
criterion for any future simulation strategy. State it **once**, normatively, in
`docs/simulation-semantics.md`, and let each export claim delta from it. Otherwise
#630 adds a fifth slightly-different sentence about the same fact, which is the
opposite of AC-5's "a reader finds the answer in one place."
