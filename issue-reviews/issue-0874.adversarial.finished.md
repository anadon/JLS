# Issue #874: TASK-C541-1: the handout bundle is one command over one circuit and one recorded run — a second run, or none, is refused by name
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what this issue actually is

TASK-C541-1 is one of a two-task roster (sibling: #875 TASK-C541-2) filed to
resolve an empty-roster defect left on feature #541 after its sole task #727
was closed as a near-verbatim duplicate. The lineage checks out: I read
#541, #727 (and its absorbing/verification comments), #505 (capstone
CAP-24), #508 (referenced PF-4 cut recommendation, not independently
fetched but consistently cited), #711, #714, #718, #722, and #875. Every
cross-reference in #874's body — the disposition history, the AC-2 refusal
clause and AC-3 PDF inclusion inherited from #727, the `ordering_after`
table resolving to #711/#714/#718/#722 as each producing feature's *last*
task — matches what those issues actually say. This is unusually
well-grounded for a task filed at this depth of a synthetic roadmap, and
several things below are flagged as solid precisely because they held up
under checking rather than being taken on faith.

## Findings, most severe first

**1. A prerequisite this task orders after is explicitly flagged "do not
fund," and #874 doesn't surface that risk.** `ordering_after` includes
#722 (TASK-C539-2, the animation capture). #722's own body states, in a
bolded line: *"**Recorded before work starts:** #508 recommends cutting
FEAT-C24-4 entirely. Do not fund ahead of an explicit REPLAN on #505."*
#874 treats the animation as in-scope-by-default (AC-1's "all five
artifact kinds," the ordering_after edge to #722) and only hedges with the
REPLAN contingency for *dropping* it later. But #722 — the task that
actually produces the animation artifact #874 depends on — is currently
told not to be funded at all pending that same REPLAN. That means #874 is
ordered after a task that may never be worked in its current form, and
#874's body gives no indication that this upstream uncertainty exists.
Recommendation: either #874 states plainly that it cannot start until
#505's REPLAN on PF-4 lands (one way or the other), or the ordering_after
edge to #722 is annotated with the same caution #722 itself carries.
Right now a contributor reading only #874 would not learn this.

**2. AC-5's "run identity" mechanism is unspecified, and the obvious ways
to implement it conflict with AC-3's byte-identical determinism.**
Criterion 5 requires "every artifact in the bundle records the identity of
the run it derived from, so criterion 2 of TASK-C541-2 can be checked
mechanically." Criterion 3 requires SVG/PDF/TikZ/WaveJSON to be
byte-identical across re-runs on the *same* inputs, and — via #711 — the
SVG/PDF further have to be byte-identical across three OSes. These two
criteria are only compatible if "run identity" is derived deterministically
from the recorded-run artifact's *content* (e.g., a content hash), never
from wall-clock time or a freshly-generated UUID. The issue never says
this. As written, an implementer could satisfy AC-5 by stamping each file
with `System.currentTimeMillis()` at export time — which trivially breaks
AC-3 — or could satisfy AC-3 by omitting any embedded identity and pushing
the entire mechanism into #875 with nothing left in #874's artifacts to
check, which would make #875 criterion 2 unimplementable. Neither failure
mode is ruled out by the stated criteria. Recommendation: AC-5 should name
the identity source explicitly (e.g., "a content hash of the recorded-run
artifact, embedded identically on every re-run") so the two criteria are
provably compatible rather than accidentally compatible.

**3. "Recorded-run artifact" is used as a settled term with no settled
format anywhere in the tree.** I grepped the whole repo (`recorded-run`,
`recorded run`, `TraceSample`) and the only persisted, on-disk trace
artifact that exists today is the VCD export (`BatchSimulator.java:373+`,
deterministic by construction — confirmed no `$date`/`$version` headers,
which is good evidence VCD *could* serve this role). `TraceSample`
(`src/jls/sim/TraceSample.java`) is in-memory only. Neither #874 nor any
of its four cited prerequisites (#711/#714/#718/#722) defines what a
"recorded-run artifact" *is* as a file — #718 talks about "a batch-produced
VCD," #722 talks about "a recorded run artifact" as if it's a different,
richer thing. #874's own arity contract ("exactly one circuit and exactly
one recorded-run artifact") is unimplementable as a CLI signature until
this is pinned down, and it isn't owned by any issue in `ordering_after`.
Recommendation: either confirm the recorded-run artifact is simply the VCD
file and say so, or point to whichever issue is meant to settle that
format before #874 can be picked up.

**4. AC-2's exit behavior is looser than the repo's own CLI contract.**
"Exits non-zero with a diagnostic naming the problem" is satisfiable by
exit code 1. But ARCHITECTURE.md's CLI contract (issue #42,
`JLSStart.usageError`) reserves exit 1 for runtime failure and exit 2 for
usage errors — and an arity violation (wrong number of run artifacts) is
unambiguously a usage error, not a runtime one. As written, a
conforming-per-the-letter implementation could return 1 and be
inconsistent with every other usage-error path in the tool, and
`CliFlagTableTest`/`CliSmokeTest`-style tests elsewhere in the suite would
have no textual basis in this issue to object. Recommendation: AC-2 should
say "exits with usage-error status (2), per the CLI contract."

**5. Criterion 3's "SVG" is ambiguous between two different SVG files the
bundle produces.** The Outcome names five artifact kinds including "the
WaveJSON plus rendered timing SVG for the chosen window" — a second,
distinct SVG from the print-styled schematic SVG. AC-3's determinism list
("SVG, PDF, TikZ and WaveJSON") names only one SVG. It's plausible the
timing SVG's determinism is meant to be covered by #718's own
`VcdToWaveJsonGoldenTest`, but #874 doesn't say so, and as written a test
suite could satisfy AC-3's letter by re-checking only the schematic SVG's
bytes, leaving the timing SVG's determinism unverified inside the bundle
composition itself — which is exactly the "weaker than the artifact it
composes" failure mode AC-3's own justification paragraph argues against
for PDF. Recommendation: either name both SVGs explicitly or state
plainly that the timing SVG's determinism is inherited from #718 and out
of scope here.

**6. No REPLAN-propagation clause for a determinism weakening upstream,
only for the PF-4 cut.** #711 (a hard prerequisite) carries its own kill
criterion KC-24-1: if byte-identical SVG/PDF across platforms proves
unachievable, #505 gets re-planned to "name the residual (e.g. PDF
excluded from byte-identity)." #874's AC-3 folds PDF into its own
determinism set specifically because #711 currently owns that guarantee
unconditionally — but if KC-24-1 fires and #711's guarantee is narrowed by
REPLAN, #874's AC-3 goes stale the same way its own animation clause
worries about ("never by silent omission") except no equivalent clause
exists for this case. Lower severity than finding 1 because it's a
contingency on a contingency, but it's the same shape of gap the issue is
otherwise careful about.

**7. Minor label mismatch.** The issue is billed throughout as headless
("no display required... a course repository and CI can both call it")
and every cited producer artifact is batch/headless in nature, yet the
issue carries `area:gui`. Nothing in the body asks for a GUI entry point.
Low stakes, but worth a triage correction so a picker-upper doesn't assume
GUI wiring is in scope.

## What holds up

- The arity-refusal requirement (AC-2) is concrete, testable by exit code
  and stderr text, and directly closes the defect #727's absorbing comment
  called "the load-bearing half" — solid, and correctly inherited verbatim
  from #727 rather than #541's weaker original wording.
- The composition-only boundary ("adds no rendering code of its own") is
  consistent across #874, #541, and #727's migration comment; no scope
  creep detected relative to that lineage.
- The `ordering_after` → issue-number table is accurate: I independently
  fetched #711/#714/#718/#722 and each does produce what the table claims
  (PDF-from-deterministic-renderer, TikZ approximation table + CI sample,
  headless VCD→WaveJSON, and the animation capture, respectively), and
  each is indeed the *last* task of its producing feature as claimed.
- Criterion 4 (bundle layout documented in-tree) is a clean, checkable
  requirement with an obvious verification (a doc file plus a test that
  reads it) and closes a real gap #541 originally lacked.

## Net assessment

The provenance and internal cross-referencing are unusually rigorous for
an issue at this depth — most of what looks alarming on first read
("band_mw," "ordering_after," "CAP-24 risk 4") checks out against the
actual referenced issues rather than being invented. The concerns above
are real gaps an implementer would hit on day one, not nitpicks: the
run-identity/determinism tension (finding 2) and the undefined
recorded-run-artifact format (finding 3) would each stall or misdirect an
implementation, and finding 1 means the task may not be legitimately
startable yet. None of these require re-splitting the roster or
re-opening the dedup decision — they're specification holes inside an
otherwise sound task, which is why this lands at sound-with-concerns
rather than needs-rework.
