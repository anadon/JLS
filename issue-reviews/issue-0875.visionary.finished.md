# Issue #875: TASK-C541-2: the in-tree LaTeX handout builds in CI from one bundle, and two figures in it cannot come from two runs
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the roster politics (the #727 dedup, the empty-feature disposition on #541, the
split seam justification — all of which is bookkeeping about *why an issue exists*, not
about what JLS becomes) and one durable claim remains, criterion 2:

> figures that ship together must be provably derived from the same run, and that must
> be asserted mechanically.

That claim is the whole reason CAP-24 (#505) is more than a pile of exporters. #505's
risk 4 and §1's "figures derived from real runs cannot silently disagree with what the
tool does" are the pedagogical payload; the SVG/TikZ/WaveJSON emitters are plumbing.
#541's own AC-1 cannot fail on that defect — five files from five runs satisfy it — and
the disposition comment on #541 correctly identifies that gap. This issue is where the
project's most interesting property gets teeth. Endorse the intent without reservation.

What I would reframe is everything around it: the vehicle (an in-tree LaTeX handout),
the mechanism (a test that reads metadata), and the cost (a second TeX toolchain lane).

## Reframing 1 — provenance is a product, not a test fixture

Criterion 2 asks a *test* to read run identity off each artifact and compare. Criterion 4
plants a defect by "regenerating one figure from a second run." Note what that means: the
planted defect is a state the command of #874 cannot produce, because #874 criterion 2
refuses a second run by name. The test therefore simulates a **hand-tampered or
partially-regenerated directory** — exactly the state a real course repo lands in six
months later, when the circuit changed, the instructor re-exported the schematic, and
the timing figure is stale.

So the thing being built is not a test. It is a **bundle verifier**, and its user is the
instructor, not CI. Make it one:

- `#874` writes a `MANIFEST` (or `bundle.json`) at the bundle root: run identity, circuit
  identity, one row per artifact with its kind, filename and digest. That single file
  *is* criterion 4 of #874 ("the layout is documented in-tree") in machine-readable form,
  not merely alongside it.
- `jls --verify-bundle handout/` re-reads it, checks every artifact's digest, and fails
  by name on a mismatch or a missing run identity. Exit 0/1/2 per the existing CLI
  contract (ARCHITECTURE.md, "Error-reporting contracts"), documented in
  `docs/batch-interface.md` like every other headless surface.
- `HandoutBundleTest` then shells that command — three assertions — and criterion 4's
  planted defect becomes a fixture directory checked into `test/`, permanently green as
  a negative case rather than a transcript pasted once in a PR and never re-run again.

This is strictly more valuable for the same work. An instructor's course repo can put
`jls --verify-bundle figures/` in its own CI and get the CAP-24 guarantee in the place it
actually matters — the repo where the figures are consumed — instead of only inside JLS's
test suite. #505's Intended Audience explicitly wants "figures live in course repos under
version control"; a verifier is what makes that claim checkable by the audience.

## Reframing 2 — run identity must be a content hash, or #874 contradicts itself

#874 criterion 3 requires SVG/PDF/TikZ/WaveJSON to be byte-identical on re-run over the
same inputs. #874 criterion 5 requires every artifact to record its run identity. If that
identity is a UUID, a timestamp or a session id, the two criteria are in direct
contradiction and one of them will quietly lose during implementation — almost certainly
criterion 5, downgraded to "the manifest records it, the artifacts don't," which
re-opens exactly the tamper case #875 exists to close.

The resolution is to pin it now, in this issue's language: **run identity is a digest of
the recorded-run artifact bytes (and the circuit bytes), and nothing else.** It is then
pure, deterministic, embeddable in each format's native metadata channel (SVG
`<metadata>`, PDF `/Info`, a TikZ comment line, a WaveJSON key, an APNG `tEXt` chunk),
and byte-determinism survives. It also upgrades the assertion for free: with a content
hash, the verifier checks not only that the five artifacts agree *with each other* but
that they agree *with a run file that exists and has not been edited* — a failure class
criterion 2 as written cannot catch, and the more likely one in practice.

## Reframing 3 — one LaTeX lane in the repository, owned once, or none

There is not one LaTeX reference anywhere in this tree today (`.github`, `docs`,
`scripts`, `src`, `test` — zero hits for latex/tectonic/texlive). #714 needs a LaTeX CI
leg; #875 needs a LaTeX CI leg. The issue calls sharing "a welcome simplification." I
would make it a precondition rather than a nicety, because the alternative is that JLS
acquires two independent multi-gigabyte toolchain installs in a repository whose whole
posture — single self-contained jar, no Node in the shipped artifact, KC-24-3 explicitly
refusing to let the WaveDrom renderer into the required matrix — is about *not* doing
that. Criterion 5's worry ("a LaTeX toolchain is the kind of dependency that turns a lane
into a silent multi-hour job") is real, and #374 documents that at its evidence commit
**23 of 23 jobs have no `timeout-minutes` at all** — so a stopwatch is not currently a
mechanism that exists. Bounding a lane you should not have built twice is the wrong fix.

Concretely: a `docs/figures/` (or `examples/handout/`) corpus with one builder script and
one CI job that compiles every document registered in it; #714 registers its CircuiTikZ
sample, #875 registers the handout. #714's document keeps failing for #714's reasons
because it is its own build target with its own diagnostics. Pin **Tectonic** rather
than `texlive-full`: one static binary with a pinned, cacheable bundle, which is the
option consistent with this project's reproducibility discipline (`docs/reproducibility.md`,
the `.buildinfo` habit) and keeps the lane in seconds, not minutes.

## Reframing 4 — separate what LaTeX actually proves from what it is being asked to prove

Three distinct claims are bundled into "the LaTeX document builds clean in CI":

1. *The TikZ compiles standalone.* Already #714's, owned there, tested there. #875
   re-proving it is duplication.
2. *The bundle's layout resolves and the documented paths are the real paths.* This needs
   no TeX at all — it is a manifest-schema assertion, milliseconds, safe on the required
   gate.
3. *The figures agree on their run.* Also no TeX — reframing 1.

Only (1) genuinely needs a toolchain, and it is not this issue's. That is the reframing
that makes criterion 5's anxiety structural rather than procedural: put (2) and (3) on the
required gate where they belong, and let the handout compile on the shared LaTeX lane as a
*documentation deliverable*. Criterion 3's intent ("a layout change breaks the build
loudly") is better served anyway by having the exporter emit a `figures.tex` fragment
defining `\jlsSchematic`, `\jlsTikz`, `\jlsTiming` — then the document has zero hardcoded
paths and a layout change is a compile error *by construction*, not by a reviewer
promising not to hardcode.

## Reframing 5 — the handout should be a shipped example, not test scaffolding

An "in-tree LaTeX handout" that exists only so a test can compile it is an unowned
document that bit-rots. JLS already has the better pattern and it is the closest precedent
in the tree: `examples/autograde/autograde.py` is a runnable consumer example, documented
for instructors in `docs/vcd-interop.md`, and pinned by `test/jls/AutogradeBridgeExampleTest`
(which also demonstrates the right toolchain gating — `Assumptions.assumeTrue` on
`ToolLocator.findOnPath`, skip locally, armed on CI runners). Put the handout at
`examples/handout/` with its circuit, its recorded run, its `figures.tex` and a README, and
`HandoutBundleTest` becomes the exact analogue of `AutogradeBridgeExampleTest`. The
deliverable is then a thing an instructor copies, which is CAP-24's stated audience, and
the test is a side effect of shipping it rather than its only reason to exist.

One caution inherited with that pattern: a skipping test asserts nothing. If the LaTeX
leg uses assumptions, CI must assert it did **not** skip, in the spirit of #374's
anti-vacuity clause.

## Alignment and sequencing

The work strengthens the project's arc — it is the only place in CAP-24 where
"self-consistency" stops being prose. But note where it sits: #875 → #874 → #711/#714/
#718/#722 → KC-24-1's determinism demo slice, none of which has landed, inside a capstone
whose PF-4 arity may still change. Meanwhile the *most* valuable piece — every exported
artifact records the run it came from, and a verifier checks it — depends on none of that.
It could land now against the surfaces that already ship: `-i` SVG/PNG export and `-vcd`.
That would give #505's own goal ("every figure in `docs/` and in issues can come from the
tool, regenerable at a commit") a mechanism years before the bundle exists, and reduce
this task to the thin composition it claims to be.

I am not disregarding the acceptance criteria — criterion 2 is the best sentence in the
CAP-24 tree and should be kept verbatim. I am saying criteria 1, 3 and 4 describe a test
harness where the project wants a product feature, and criterion 5 tries to bound with a
stopwatch a cost that reframing 3 removes.

## Concrete asks if this proceeds as filed

1. Pin run identity as a content digest in #874 criterion 5, explicitly, before either
   task starts — otherwise determinism and provenance collide silently.
2. Promote the bundle manifest and `--verify-bundle` to shipped surface; make
   `HandoutBundleTest` call it rather than reimplement it.
3. Make the shared LaTeX lane with #714 a precondition, not an option, and pin Tectonic.
4. Keep criteria 2 and 3's assertions off the LaTeX lane so they can be required checks.
5. Relocate the handout to `examples/`, modelled on `examples/autograde` +
   `AutogradeBridgeExampleTest`, and assert the LaTeX leg did not skip in CI.
