# Issue #633: TASK-C561-3: every analog element in a real Falstad circuit is a named, located, explained loss by design — and the report dialect is #556's, unchanged
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#633 is the closing task of FEAT-C29-4 (#561): it runs the Falstad importer
built by TASK-C561-1 (#629, parsing) and TASK-C561-2 (#631, logic mapping)
against one real circuit and asserts the loss-report and undo contracts hold.
Its two direct siblings in the same CAP-29 task family — TASK-C558-5 (#619,
`.dig`) and TASK-C559-5 (#628, `.cv`) — establish the pattern this exact task
shape should follow. #633 departs from that pattern in a way that is not
argued for, only omitted: it drops the one AC that turns "passed once" into
"stays true," and it inherits, unfixed, the single-circuit gameability its
own parent feature review already flagged. No Falstad-related code, fixture,
or test exists anywhere in this checkout (confirmed by `grep -ril falstad`
across the tree), so every claim below is about the spec, not an
implementation.

## Findings, most severe first

### 1. [HIGH] No CI-regression fixture AC — inconsistent with both sibling tasks, and it is the exact gap that lets AC-1 be true once and false forever after

TASK-C558-5 (#619) AC-4: *"The real-circuit import is a committed CI fixture,
so a later mapping change that starts dropping constructs turns the build
red."* TASK-C559-5 (#628) AC-4: identical wording, same clause. #633 has no
equivalent AC. Its four ACs cover zero-unexplained-loss (AC-1),
dropped-by-design disposition (AC-2), report shape (AC-3), and undoable
import (AC-4) — none of them requires the proving circuit to become a
permanent, CI-enforced regression fixture. Concretely: TASK-C561-2 (#631)
AC-1 requires "a written, reviewable table" that can be edited later by
anyone touching the Falstad mapping; without #633 pinning the real circuit as
a committed golden test, a future edit to that table can silently start
dropping a construct the original acceptance run covered, and nothing in
this task's own AC set would notice. This is the identical mechanism CAP-29's
own doctrine (named-loss, never-silent) exists to prevent, reproduced inside
the very task meant to prove the doctrine.

**Recommendation:** add an AC identical in force to #619/#628's AC-4 —
commit the real circuit (and its expected report) as a CI fixture that fails
the build if a later change reintroduces an unreported drop.

### 2. [HIGH] AC-1's "one circuit" bar is reproduced verbatim from #561, where it was already found gameable — and #633 is the task where that gap becomes concrete, testable behavior, not prose

#561 AC-1 (parent feature, already reviewed `needs-rework`): *"One real
published Falstad circuit imports with zero unexplained losses ... logic
subset realised, every analog element named, located, explained."* #633 AC-1
restates the same single-circuit bar and adds only the bidirectional-totality
clause from TASK-C556-2. A single implementer-chosen circuit can still avoid
every hard case — multi-driver labeled nodes, a "custom logic" truth-table
block, an analog element whose only consumer is a logic element (see Finding
4 below) — while satisfying AC-1 to the letter. Since #633, not #561, is
where this AC is actually discharged and closed, this is where the
corpus-of-one weakness stops being a planning-document concern and becomes
the literal acceptance bar a real PR gets merged against.

**Recommendation:** require a small corpus (N≥3, drawn from independently
authored sources) here, at the task that actually closes the claim, not just
upstream in #561's language.

### 3. [MED] The task's declared 1 mw band hides that its true prerequisite chain runs through CAP-16's unstarted 30-50 mw capstone

`ordering_after: ["TASK-C561-2", "TASK-C556-2"]`. TASK-C556-2 (#610) itself
orders after `["TASK-C556-1", 323, 451]` — i.e. #323 (FEAT-025, CAP-16's
centerpiece `.circ` importer, itself unbuilt: no `src/jls/imp/` package
exists anywhere in this tree) and #451. So #633's real earliest-start date is
gated by CAP-16's required set landing first, the same structural problem
the #561 review already flagged one level up (its Finding 1). #633 states
none of this explicitly — a contributor reading only #633 sees a `band_mw:
"1"` task with two named prerequisites and no signal that one of those two
prerequisites transitively depends on a ~30-50 mw capstone that has not been
started.

**Recommendation:** either state the transitive dependency explicitly in
#633's own ordering notes ("blocked in practice on #323/#451 via #610"), or
have TASK-C556-2 surface its own blocking status loudly enough that every
downstream consumer (#619, #628, #633, #562 per #610's own "consumers" list)
doesn't have to re-derive it independently.

### 4. [MED] TASK-C556-1's `location` schema was designed against Falstad as a hypothetical "worked example," not against what TASK-C561-1 actually emits — the mismatch, if any, surfaces for the first time in #633's acceptance run

TASK-C556-1 (#608) AC-4: *"`location` is expressive enough to name a position
in a non-XML source; the Falstad text format (#561) is the worked example the
document uses to prove that."* But #608's own `ordering_after` is `[323,
314]` only — it does not order after TASK-C561-1 (#629), the task that
actually defines what a Falstad source location looks like once real parsing
exists. #608 is therefore free to land before #629 does, using a guessed
shape for Falstad's `location` field. If that guess doesn't match what #629
actually produces (e.g. line+token index vs. #629 AC-4's actual "source
location per element" representation), the first place the mismatch would be
caught is #633's real-circuit acceptance run — not #608's or #629's own
tests, since neither is ordered against the other.

**Recommendation:** either add `ordering_after: 629` to #608, or have #633
explicitly test-pin that the location shape TASK-C561-1 emits round-trips
through TASK-C556-1's schema before relying on it for the real-circuit
acceptance run.

### 5. [MED] Licensing/provenance of the "real published Falstad circuit" is unaddressed anywhere in the task family

#633 (and its siblings #619, #628) commit a third-party-authored circuit —
"real published," i.e. not written by this project — into a GPLv3 repository
as a CI-enforced test fixture (per Finding 1's recommendation, which mirrors
#619/#628's existing AC-4). Nothing in #633, #619, #628, `LICENSE`,
`CONTRIBUTING.md`, or `SECURITY.md` states what license or redistribution
right the source circuit is published under, or what happens if the original
author's terms don't permit redistribution inside this repository's test
tree. There is no existing precedent to fall back on either: `grep` finds no
`.circ`/`.dig`/`.cv` fixtures or attribution notes anywhere in the checkout
today (`examples/` holds none). CAP-16's own corpus-sourcing work (KC-16-1,
per the #561 review) faces the identical question for `.circ` files and
neither issue resolves it.

**Recommendation:** name a source with clear redistribution terms (e.g. a
circuit the maintainer authored and published personally, or one under a
license compatible with the repo), and record the license/attribution
alongside the fixture — do not let this be discovered at PR-review time.

### 6. [LOW] AC-4's undo language is measurably weaker than the same clause in both sibling tasks

#619 AC-2 / #628 AC-2: *"Import is a single undoable operation; undo restores
the workspace exactly, and a mid-import failure leaves no partial circuit."*
#633 AC-4: *"Import is a single undoable operation that never silently
rewrites semantics and emits no partial circuit."* The "undo restores the
workspace exactly" clause — the actual falsifiable undo assertion — is
dropped in #633 and replaced with "never silently rewrites semantics," a
different (also valid, but not equivalent) property. As written, #633 could
be satisfied by a single-shot undo that removes the imported circuit without
being shown to restore prior editor state exactly.

**Recommendation:** carry the "restores the workspace exactly" language
forward from #619/#628 rather than substituting a different claim under the
same AC number.

### 7. [LOW] "One-line explanation" per analog element does not require per-construct specificity

AC-2 requires each analog element get "a one-line explanation of why analog
is out of scope" — satisfiable by stamping the same canned sentence ("analog
is out of scope by design") on every analog element in the circuit, which
technically produces N named-and-explained entries without any of them
saying anything about the specific element. This is a low-severity
gameability note, not a blocker, since #556/#608's disposition vocabulary
already forces the *category* (`dropped-by-design`) to be correct; only the
free-text explanation field is at risk of being boilerplate.

**Recommendation:** clarify whether "explanation" may be format-level
boilerplate or must reference the specific construct (e.g. "op-amp U3 —
analog, out of scope" vs. a bare repeated sentence).

## What's solid

- `dropped-by-design` is not an invented term: it is the exact closed
  vocabulary value TASK-C556-1 (#608 AC-1) defines
  (`mapped`/`mapped-with-caveat`/`refused`/`dropped-by-design`), so AC-2 is
  correctly grounded in the shared schema rather than free text.
- The two named prerequisites (TASK-C561-2 for the mapping table that
  produces a working circuit, TASK-C556-2 for the totality assertion this
  task's AC-1 leans on) are the right pair of direct dependencies for what
  this task actually needs — nothing obviously necessary is missing from the
  immediate (non-transitive) edge set.
- AC-3's "no Falstad-specific field or renderer exists" is concretely
  testable today's way JLS already tests absence-of-forking (e.g.
  `ElementConstructorContractTest`, `SaveTagsTest` style negative checks) —
  a reasonable ask, not a vague one.

## Verdict rationale

The task correctly narrows FEAT-C29-4 to a closeable acceptance run and
grounds its terminology in the shared schema rather than inventing its own.
But set beside its own two direct siblings in the same generated task family
(#619, #628), it visibly drops the regression-guard AC that keeps "zero
unexplained losses" true after the PR merges (Finding 1), reproduces the
already-flagged single-circuit gameability at the exact point it becomes
binding (Finding 2), and leaves a licensing question unaddressed that any of
the three sibling tasks would trigger identically the day someone tries to
commit a real third-party circuit into the tree (Finding 5). These are
narrow, concrete gaps fixable by copying language that already exists two
issues over — not a rejection of the task's premise. **needs-rework.**
