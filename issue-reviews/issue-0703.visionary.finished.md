# Issue #703: TASK-C526-2: a failing test annotates the exact circuit file it failed on, so a student sees the problem where the problem is
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The title states the goal better than the acceptance criteria do: *a student sees the
problem where the problem is*. The criteria then quietly narrow that to "an annotation is
attached to the right `.jls` path." Those are not the same outcome, and the gap between
them is where the whole value of this task lives.

For a JLS student, "where the problem is" is a gate on a canvas. The nearest thing GitHub
can point at is a line in a text blob. The issue never asks whether that pointer resolves
to anything a student can look at — and at HEAD it does not.

## The unexamined fact that decides the design

`README.md` and `docs/file-format.md` §1 are unambiguous: current JLS writes `.jls` as an
**XZ stream** by default (magic `FD 37 7A 58 5A 00`). Plain text is written only on
explicit request — `File > Save As` with the plain-text type, or `jls -savetext`.

A student's committed submission is therefore, by default, a binary blob. GitHub renders it
as "Binary file not shown." A check annotation on line 1 of that blob is a pointer at a
wall. Every one of AC-1 through AC-4 can pass green while the student experience is
strictly *worse* than the log this issue is replacing, because the log at least contained
readable text. The acceptance criteria are unfalsifiable with respect to the outcome in the
title.

This is fixable and cheap, but not inside this task's stated boundary:

- The starter template (TASK-C526-3, #705) must mandate plain-text saves for submitted
  circuits, and the lab README must tell students how. `README.md` already frames plain
  text as the version-control interchange form — this is using a shipped capability, not
  inventing one.
- That makes #705 a **hard prerequisite** of #703's outcome, not a sibling. The stated
  `ordering_after: ["TASK-C526-1"]` is missing the edge that actually matters.

## Reframing 1 (primary): the verdict gets a location; annotations are one renderer

#703 is filed as GitHub-Action work. It is not. The thing that has to exist is one field on
the verdict record defined in #466 §7.5 — `(expectation, observed, passed)` becomes
`(expectation, observed, passed, where)` — and `where` is a *stable, environment-free*
locator, not a path.

The tree already has everything needed to make `where` precise rather than file-granular:

- `docs/file-format.md` §8: every element carries a permanent `sid` (#165), minted
  deterministically even for files that predate it.
- §8 canonical order (#166): element blocks are emitted sorted by stable id, "making the
  serialized form a pure function of circuit content." So `sid → line number` in a
  plain-text save is a deterministic, recomputable mapping, not a guess.
- `docs/batch-interface.md` §3.2: expectations name a watched element from the three-type
  whitelist, and #466 makes an unresolvable name a located parse error. The element
  identity is already in hand at verdict time.

So the honest locator is `(unit, sid)` — and the Action resolves it to `(path, line)` by
loading the submitted circuit and asking the loader, exactly the way the tool itself would.
That is the criterion AC-2 is reaching for ("does not guess a file by name matching") and
it is stronger than what AC-2 asks for.

Once `where` exists on the verdict, **four consumers get it free**, and only one of them is
GitHub:

1. `TestPanel` (#466 §7.4) can select and flash the failing element on the canvas. This is
   the maximal version of "sees the problem where the problem is," and it is the version
   that works for the student who never pushes anything.
2. `jls -b -check` can print `alu.jls:412: FAIL vector 7: expected 0xD, observed 0x5` — the
   compiler-diagnostic idiom every editor's quickfix list already parses.
3. The Gradescope adapter's per-test output (#502 PF-2) gets the same anchor.
4. The Classroom Action renders it as an annotation.

Filing this as "Action annotations" makes attribution look like adapter code. It is verdict
code, and the leverage ratio is roughly four to one.

## Reframing 2: I am disregarding AC-2 as written — the artifact must not carry paths

AC-2 requires attribution "derived from the batch artifact's own file references." I would
not build that. Putting repo-relative (let alone absolute) paths inside the xUnit report
sets it directly against two constraints the same capstone imposes:

- #524 AC-4: "no timestamps, ordering, or locale nondeterminism in verdict output **across
  container boundaries**."
- #719 / CAP-21 AC-1: score vectors byte-identical across a Gradescope Docker image, an
  Actions runner, a PrairieLearn grader image, and an nbgrader kernel.

A filesystem path is the single most environment-dependent value available. Four platforms
mount the submission at four different roots. The first divergence #719 discovers would be
a path prefix — a false alarm that costs a REPLAN on #502 and teaches everyone to distrust
the parity test.

The clean seam: the artifact carries the lab's **declared unit id plus the element `sid`**;
the *lab manifest* (CAP-06's lab-as-data format, named in #502 §Background) already binds
unit id → circuit file, because that binding is what told the runner which file to grade in
the first place. Attribution becomes a lookup in data the Action authored, not an inference
from a string in the artifact. Hermetic artifact, exact attribution, no name matching.

Worth stating plainly, because AC-2 obscures it: under #466's invocation form
(`jls -b -t v -check e -report r.xml circuit.jls`) **one process grades exactly one circuit
file**. File-level attribution is not a hard problem the artifact must solve — it is a fact
of the invocation plan. The genuinely hard and genuinely valuable part is *sub-file*
attribution, which the issue does not ask for at all.

## Reframing 3: don't write a Checks API client

The simplest implementation of AC-1/AC-3 is not an API integration. It is
`::error file=alu.jls,line=412,title=vector 7::expected 0xD, observed 0x5` on stdout.
GitHub converts workflow commands into annotations with no token, no `checks: write`
permission, and no HTTP client in the Action. `ci.yml` already uses this idiom in nine
places, and `$GITHUB_STEP_SUMMARY` at `ci.yml:107`.

The architectural payoff is larger than the code saved: annotation output becomes a **pure
function of the xUnit report plus a formatter**, which means it can be pinned as a
byte-golden in the Java test suite in exactly the `GradeReportGoldenTest` style #466
establishes — no live GitHub, no fixture repo, no network. Compare that to testing a Checks
API client.

Two constraints the design must absorb, and the issue mentions neither:

- GitHub displays a documented maximum of **10 annotations per level per step**. A
  submission failing 40 vectors annotates 10 and silently drops 30. The formatter must
  therefore *rank* — first failure per file, then by vector order — and route the complete
  table to `$GITHUB_STEP_SUMMARY`, whose budget is orders of magnitude larger and which
  renders Markdown tables. (The Checks API's higher cap is the only reason to prefer it;
  weigh that explicitly rather than by default.)
- The full circuit image is available: `jls -i out.svg` (#154) is a shipped flag. Exporting
  the failing circuit as an SVG workflow artifact and linking it from the step summary gives
  the student the picture, which is the actual medium of the subject. That costs one CLI
  call and belongs in this task far more than a second annotation code path does.

## One route explicitly costed and rejected

SARIF via `github/codeql-action/upload-sarif` is the obvious "richer" alternative — real
file/line/region model, persistent results, PR-review integration, rich Markdown help text
per rule. Reject it: code-scanning upload requires Code Security on **private** repositories,
and GitHub Classroom assignment repos are private by default. It would pass in a maintainer's
public test repo and fail in every real course — the exact failure shape CAP-21 KC-21-3 is
written to prevent. `docs/standards-adoption/11-costed-rejections.md` is the right home for
that record.

## Ordering defect worth escalating

#703 orders behind TASK-C526-1 (#701), which orders behind #524 — the issue that **freezes**
the xUnit schema, artifact paths, and the compatibility ratchet. But #703's whole premise is
that the artifact carries attribution. If attribution is not in the schema before #524
freezes it, then #703's first commit is a compatibility event under a ratchet that is one
week old, with a deprecation window it must honour.

The attribution field belongs upstream: in #466's verdict record and #524's frozen schema.
What remains in #703 is real and worth its band — the ranking formatter, the step-summary
table, the SVG link, the repo-level fallback of AC-4, and the plain-text mandate coordinated
with #705. Ship it as *presentation over an attribution field that already exists*, which is
what its own Boundary section claims it is.

## What to keep exactly as written

AC-4. "A failure that cannot be attributed produces a repo-level annotation that says so,
rather than being attached to an arbitrary file" is the honest-failure discipline this
repository runs on everywhere else (the `LoadError` taxonomy, the rig's 0/1/2 exit
classification, `TellUser`). Do not soften it, and make the unattributable case a fixture in
the golden set rather than a code path nothing exercises.
