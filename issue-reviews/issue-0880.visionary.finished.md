# Issue #880: FEAT-C25-0: the schematic-similarity premise is measured before it is funded — a 30-submission synthetic corpus with planted pairs separates from independent solutions, or CAP-25 stops
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is actually for

Stripped of the tier machinery, #880 is one good instinct: **do not spend 14–21 mw
building a discriminator until you have measured that discrimination is possible.**
That instinct is right, it is rare, and nothing below is an argument against it. The
redirect is about *what* gets measured, *what it is measured on*, and *what question
the answer would actually settle*.

## The finding that reframes everything: the project already refused this capability

CAP-25 sources its canonical form from lf-06/P11. That same document, and the
amendment that ratifies it, contain an explicit refusal of CAP-25's outcome:

- `docs/capability-roadmap/AMENDMENT.md:478` — "**Deliberately refused: any
  similarity score, cohort ranking or automated plagiarism flag.** … Ship the
  pairwise comparison; a comparison is a tool a human uses after forming a
  suspicion, a score is a machine forming the suspicion."
- `docs/capability-roadmap/lf-06-diff-merge-vcs.md:615-624` — the same
  recommendation at length, plus the reason it is not a caveat: the error profile
  "fires on the honest student in the shared lab" and is "defeated by the copier who
  knows about it", and lf-06 asks that `docs/version-control.md` state "JLS does not
  and will not compute a plagiarism verdict."

CAP-25 (#506) is a similarity score, a cohort-wide ranking, and an automated flag.
It never cites this refusal, and neither does #880. A recorded refusal can absolutely
be reversed — but by argument, not by silence, and the reversal is the *first* thing a
premise gate should test. #880 as written measures the technical premise ("can label
refinement separate?") and leaves the refused premise ("should a machine form the
suspicion?") entirely untouched. A green #880 would therefore fund PF-3 through a gate
that never looked at the reason the capability was refused in the first place.

Note also lf-06's fourth objection, which is a **direct hit on this issue's corpus
design**: "A provided skeleton, a worked example from the lecture, and a canonical
textbook circuit all produce high structural similarity between honest submissions."
That is the real-world null distribution, and #880's AC-1 forbids it. AC-1's ban on
"mutating a common ancestor" was written to stop a *circular* null (fake independence
manufactured from one solution) — correct — but the same words also ban the *realistic*
null (honest independent work that legitimately starts from an instructor skeleton).
Two different things wearing one phrase. The corpus as specified is the most favorable
null the world can produce, and a separation result on it is not evidence about the
setting the tool would ship into.

## The gate cannot decide in either direction

- **The "fail" branch is not conclusive.** AC-2 builds only "as much erasure as three
  transform classes at scale 30 require", explicitly not PF-1. If it fails to separate,
  the obvious rebuttal is "of course — you built a partial canonicalizer." A kill gate
  must be an **upper-bound** experiment, not a representative slice, or its negative
  result will not hold.
- **The "pass" branch is not conclusive either.** 3 planted pairs against 432 null
  pairs is an order statistic on n=3. Perfect separation at n=3 is a coin-flip-grade
  result, and on an unrealistically clean null it is close to guaranteed. The gate is
  currently symmetric in language ("separation achieved, PF-3 may be funded") and
  asymmetric in evidential force.
- **The planted count is cheap and arbitrarily small.** KC-25-0-1 rightly refuses to
  grow the corpus to 300 — but that constraint bites on the *null* side, where each
  independent solution costs authoring effort. Planting is mechanical: apply a transform
  to an existing solution. 3 planted pairs is 1 per transform class for no reason but
  the phrase "demo slice" in #506's Cost. 15 (5 per class) costs essentially nothing and
  multiplies the statistical content of the only side that is currently starved.

## The substrate the issue waits for does not exist — and a better one already ships

AC-2 and KC-25-0-2 chain the cheap premise test to #356's canonical form. Verified at
HEAD: there is no `sref`/`sprobe` item kind (`grep -rn "sref\|sprobe" src/ test/` — no
hits), no validator, no `jls.diff`, no canonicalizer of the kind #356 describes. #356
itself is `blocked_by: [319, 334]`, has zero filed tasks, and carries an unresolved
1.6–2.4x cost gap. So the realistic outcome of #880 as written is that KC-25-0-2 fires
on day one, the feature "discharges" by reporting an ordering fact everyone already
knows, and the premise stays untested indefinitely. **A gate that predictably produces
no measurement is not a gate.**

Meanwhile the tree already contains a shipped, tested, deterministic, geometry-free
structural model of a circuit:

- `src/jls/hdl/HdlModel.java` — ports, nets, and per-element statements over net-name
  and literal operands. Geometry is gone by construction; names are already legalized
  and the `renames` map records the mapping (i.e. name erasure is a one-line
  substitution over an existing seam, not a new layer).
- `src/jls/hdl/HdlExporter.java:170 buildModel` — deterministic (`:313`, `:372`,
  `:1292`, `:1319` all sort explicitly), headless, and already pinned by golden tests.
- `src/jls/hdl/yosys/` + `-export out.v` — Yosys is already an in-tree dependency and
  CI installs `iverilog`/`yosys`. `yosys -p 'proc; opt_clean; write_json'` **erases
  inserted no-op buffers by construction** and rewrites wire names canonically — two of
  the three transform classes #880 plans to plant are defeated for free by a tool the
  project already integrates.

Using that path is not the second canonicalizer KC-25-2 forbids. KC-25-2 exists to stop
two *shipping* canonicalizers disagreeing about whether two circuits are the same. An
HDL lowering that already exists for an unrelated reason, and that will never be a
candidate for the shipped canonical form, is not a fork of anything.

## Alternative framings

**A — the upper-bound experiment (replaces #880 as filed).** Answer the premise
question with zero new architecture: `jls -export` each submission, normalize through
Yosys, score all pairs with an off-the-shelf WL kernel in a script under `scripts/` or
`examples/`, and report the two distributions. Sub-1 mw, no dependency on #356, nothing
committed to the tree that has to be maintained. Because a real synthesis tool is a
*stronger* canonicalizer than PF-1 will ever be, a failure here is a genuine upper
bound and KC-25-1 fires with authority the bespoke version cannot supply. Pair it with
a skeleton-derived null cohort (AC-1 amended to distinguish the circular ancestor from
the legitimate one) and 15 planted pairs, and both branches of the gate become decisive.

**B — fund the comparator, not the discriminator (the durable reframing).** The asset
worth building is lf-06's C2: *circuit equality as a graded predicate* — identical /
identical-modulo-geometry / identical-modulo-ids / different-here. Its consumers are
already queued and mostly outside CAP-25: semantic diff (#356), P3's headline round-trip
claim `export → yosys → import → save` "equal modulo element ids" (which lf-06 records
has **no oracle in the tree today**), regression triage on the 10k-line RISC-V goldens,
the collab convergence oracle, and — as a human-driven pairwise comparison — the
instructor integrity workflow lf-06 actually endorses. Reframed this way the measurement
in #880 has value on *both* branches: if separation fails, the comparator still ships and
still serves five consumers. As filed, a KC-25-1 fire leaves behind a corpus and a
throwaway script.

**C — condition on function, which is the actual operating regime.** A grading pipeline
only reaches the integrity pass for submissions that already *pass the test vectors*, so
every pair in the real null is functionally equivalent. JLS's core competence — the batch
simulator and `-t` vectors — can enforce that on the fixture corpus for free. #880 never
states it, and if the independent solutions differ functionally the separation number is
inflated by a signal the deployed tool would never see. This is a one-line strengthening
that only JLS is positioned to make, and it is a better use of the project's actual
substrate than reimplementing MOSS.

## What I am disregarding, and why

I am disregarding AC-2, AC-5 and KC-25-0-2 as written. AC-2's ordering against #356 is a
correct rule for the *shipping* canonicalizer imported wholesale onto a *throwaway
measurement*, and its only realistic effect is to guarantee no measurement happens.
AC-5's binary verdict ("separation achieved, PF-3 may be funded" / "KC-25-1 fires") is
the wrong shape: the gate should be explicitly one-sided — a pass authorizes PF-1/PF-2
(structural work with independent value under framing B) and never PF-3, whose
calibration needs evidence at a scale this corpus cannot supply. And before any of it,
#506 owes a written rebuttal of `AMENDMENT.md:478`. If that rebuttal cannot be written,
the correct discharge of #880 is to fire KC-25-1 on ethical grounds without running the
measurement at all — which would be the cheapest and most honest outcome available.

## What survives unchanged

AC-6 (no judgement about a person, adopted from the start rather than retrofitted), AC-3
(determinism), the refusal-is-a-pass discipline in all three task bodies, and the
pre-registered separation criterion added by the 2026-08-08 roster comment. Those are
right and should carry into whatever replaces this issue.
