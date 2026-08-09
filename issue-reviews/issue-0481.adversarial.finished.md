# Issue #481: TASK-0098: the analog solver produces the same bytes on Linux, macOS and Windows — five determinism controls, each a failing build rather than a convention
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## 1. [CONFIRMED, severity: high] The whole evidentiary base — `evidence_commit` and the cited prior-work document — is unreachable from `master` and slated for deletion

The issue pins every observation (O1–O7) and every GitHub permalink to
`evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`, and its "Background & Prior
Work" section leans entirely on `docs/plan/evidence/analog-determination.md` §§1.3, 4.1, 4.3
("Do not restate that document; this issue implements it").

Verified against the actual repo:

```
$ git merge-base --is-ancestor 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7 master
# NOT an ancestor of master
$ git branch -a --contains 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7
  remotes/origin/claude/jls-virtual-hardware-linux-njsoma
$ find docs -iname '*analog*'
# (nothing — the file does not exist anywhere in the checked-out tree)
```

`docs/plan/evidence/analog-determination.md` exists only on commit `3a81a4a` of the branch
`claude/jls-virtual-hardware-linux-njsoma`, and that branch's own tip commit
(`742da74`, "docs: remove the planning corpus now that it is encoded in issues") says
explicitly: *"The maintainer ruled that this branch will not be merged and will be
deleted."* Issue #485 (the maintainer's own D1–D16 decision record, rescued from the same
branch) states the identical fact and goes further — **D12** rules directly on this:
*"the planning evidence directory landed in `3a81a4a`, after the `2d0ca9d` evidence commit
the issues declare, so citations to it do not resolve at the commit they are pinned to... A
bare `file:line` with no commit and no landmark is not a citation and does not satisfy the
evidence rule,"* and *"D12's 'strongest form', the permalink, does not survive deletion of
the branch it points into... the only preservation is inlining the content."*

Issue #481 does exactly what D12 warns against: it cites `analog-determination.md` §§1.3/4.1/4.3
by section number and explicitly declines to restate them, instead of inlining. Once the
branch is deleted (which the maintainer has already ordered), an implementer cannot
resolve `2d0ca9d` at all — `git show 2d0ca9d:...` becomes "unknown revision," every
`github.com/anadon/jls/blob/2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7/...` permalink 404s, and
the seven-configuration digest-stability measurement that H1 rests on ("one Java kernel
produced an identical digest across JDK 25 (twice), `-Xint`, ... and JDK 21") is gone with
no surviving primary source — only the one-paragraph narrative summary repeated across
#481/#463/#351 survives.

**Recommendation:** before this task is picked up, someone must either (a) inline the
relevant sections of `analog-determination.md` into this issue or into
`docs/analog-determinism.md` directly (the deliverable this task already plans to write —
do it from the primary source while it's still fetchable, not from memory of the summary),
or (b) explicitly downgrade every citation to "H1's evidentiary support is a lost primary
source; treat the digest-stability claim as asserted, not verifiable" and adjust confidence
accordingly. Filing the DoD item "Every cited evidence document and permalink resolves on the
default branch at close" is necessary but not sufficient — the *code* citations can be
re-derived against real HEAD; the *document* citations cannot, because the document itself
is gone.

## 2. [CONFIRMED, severity: medium] The "no differences outside ISSUE_TEMPLATE" supersession check is already false, and the drifted files bear directly on this task's own subject matter

Section 2 states: *"`git diff --stat 2d0ca9d HEAD -- src test pom.xml .github` reports no
differences outside `.github/ISSUE_TEMPLATE/`, so the probes below observe the evidence
commit's code and workflows."* Re-run against current HEAD:

```
$ git diff --stat 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7 HEAD -- src test pom.xml .github
 .github/workflows/ci.yml                | 26 +++++++-------
 ...
 src/jls/elem/ElementId.java             | 15 -----------
 src/jls/elem/SaveTags.java              |  2 --
 src/jls/hdl/HdlExporter.java            | 60 +----------------------
 test/jls/ElementRegistryTest.java       | 54 ------------------
 test/jls/FileFormatSpecTest.java        |  6 ----
 test/jls/elem/ElementIdReplicaTest.java | 53 ------------------
 test/jls/hdl/HdlPolicyTest.java         | 38 --------------
 14 files changed, 26 insertions(+), 252 deletions(-)
```

The `.github/workflows/*.yml` hunks are dependabot action-pin bumps (cosmetic). But
`src/jls/elem/ElementId.java` — the exact file O3 cites for the replica-resolution hazard —
has genuinely diverged: `2d0ca9d` carries a counter-advance safeguard in `parse()` (`if
(replica.equals(processReplica)) { ... NEXT_COUNTER.getAndUpdate(...) }`) that current
`master` does not have. This is not noise: issue #485's D6 identifies this exact code as fix
`36cbd37` ("advance the creation counter past stable ids already in use"), landed **only on
the dying branch** and explicitly flagged there as at risk: *"If the branch is deleted
before they are cherry-picked or re-landed, the two fixes themselves are lost with it."*
That has now happened — `master` has neither `36cbd37` nor its sibling `970db41` (the
`SaveTags` `RegisterFile`/`FieldExtend` registration fix, also missing from current
`SaveTags.java`).

The consequence for #481 specifically: O3's `ElementId.java` line citations (`L41-L57`) are
already off by a few lines against current HEAD (the field javadoc is now at line 42, the
field assignment at line 54), and — more importantly — the file whose "documented order"
this task's D-3/hazard-3 reasoning depends on is not in the state the issue describes it in.
Anyone picking up this task must re-run O1/O3/O4 against real HEAD before writing a line of
code, exactly as the issue's own DoD says ("Not superseded... citations re-derived if HEAD
had moved") — but the issue asserts, in the body, that this re-derivation is unnecessary
("citations re-derived at `2d0ca9d`... the probes below observe the evidence commit's code
and workflows"), which is no longer true.

## 3. [CONFIRMED, severity: medium] P9's "green" acceptance criterion is satisfiable without the underlying claim holding

The digest job is explicitly `continue-on-error` while advisory (§7.1, Method step 8), and
the DoD requires *"`CrossRuntimeDigestTest` green on every platform leg, with each leg's
digest and JDK version recorded side by side (P9)."* With `continue-on-error: true`, GitHub
Actions reports the *job* as successful in the checks UI regardless of whether the digest
step itself passed or failed — the step can go red and the job conclusion still reads green.
So "P9 green" is ambiguous between "the digest assertion actually passed on every leg" and
"the job didn't block the PR," and only the former is the claim this whole task exists to
gate. Threat T4 half-acknowledges the adjacent risk ("an advisory job read as a gate") but
frames it as a *communication* problem ("say so in the PR") rather than a *verification*
problem: nothing forces the closing PR to paste the actual per-leg digest values rather than
just the green checkmark, and under the two-week schedule pressure T7 names as the single
biggest threat to this task's purpose, a green checkmark is the path of least resistance.
**Recommendation:** make the DoD item unambiguous — require the pasted digest values per leg
(which section 9's Data Collection & Analysis already asks for) as the acceptance artifact,
not the CI status badge, and say so in the DoD line itself rather than only in §9.

## 4. [PLAUSIBLE, severity: low] "No exemption list" is stated as an absolute but D-1 already ships one

§7.12 states *"The five rules start from a clean baseline (O2), so no exemption list ships,"*
and the DoD repeats *"No exemption list in any of the five rules, or each exemption recorded
with what it is for."* But P4 itself is: *"`jls.analog` references no `java.lang.Math`
except `sqrt` and `abs`."* That `except` clause is an exemption list of exactly two entries,
just written inline in the rule's own definition instead of as an external data structure.
The distinction the issue is actually drawing — a *reasoned, fixed, two-item* exception
(both IEEE-754-exact and hence platform-independent) versus an *open-ended, code-review-time*
exemption list that grows — is defensible, but the DoD wording as written ("no exemption
list... in any of the five rules") is falsified by the rule it's describing on a literal
reading. Tighten the wording to "no *growable* exemption list" or similar so a reviewer
checking this box against P4's own text doesn't have to guess the intended distinction.

## 5. [PLAUSIBLE, severity: low] Full completion is gated on ~4-6 weeks of unstarted, harder work, but the DoD reads as if it's all in scope for "this task"

The issue is honest about cost ("2 weeks is the controls-plus-matrix slice... the stage is
4-6 maintainer-weeks including `docs/analog-determinism.md` in full and the complete
4-platform x 2-JDK wiring"), but the 18-item Completion Criteria checklist that follows
doesn't visibly partition into "closes in 2 weeks" vs. "closes only once the 4-6 week stage
does." Item 9 ("`CrossRuntimeDigestTest` green on every platform leg, with each leg's digest
and JDK version recorded side by side") reads as a hard gate for *this* PR, yet is explicitly
described elsewhere as only 3-platform (not 4), 1-JDK-matrix-slice work in the 2-week cut.
A contributor optimizing for "close the issue" rather than "do the honest 2-week slice" has
an incentive to either pad the PR into the full 4-6 weeks (scope creep) or quietly redefine
"every platform leg" down to what the 2-week slice actually covers (silent scope reduction).
Recommend explicitly marking which DoD items belong to the 2-week slice versus the larger
stage, the way the "cost honesty" section already distinguishes them narratively.

## What's solid

- **The dependency graph checks out.** #463 (blocked_by, the solver) and #374 (blocked_by,
  workflow timeouts) are both open, as claimed; #351 (part_of/blocks) is open and correctly
  lists #481's controls as a required child on its own critical path (`TASK-0097 →
  TASK-0098 → TASK-0100`).
- **O1, O4, and the `HashSet`/`getElementsInStableOrder` claim (O6/O3-in-#463) all reproduce
  against current HEAD**, independent of the evidence-commit problem above: no `jls.analog`
  package exists, zero `timeout-minutes` across all six workflow files, `Circuit.elements` is
  still a plain `HashSet` at `src/jls/Circuit.java:48` with the sorted accessor at
  `getElementsInStableOrder()` (`:479-485`) exactly as described.
- **The `SocketConfinementRatchetTest` mention-vs-construct idiom is real and matches the
  file** (`test/jls/SocketConfinementRatchetTest.java:35-44`), and it's a reasonable pattern
  to reuse for D-5's scan.
- **H4's three-valued grid-flip outcome and P13's raw-bits-not-`Double.toString` discipline
  are well-motivated and specific** — concrete failure modes (JDK 19 `toString` format
  change, `-0.0` vs `0.0` bit patterns) rather than vague caution.
- **The falsification criteria (§10) genuinely commit to "loosening the comparison is
  forbidden"** and name the specific failure mode (T7) rather than leaving it implicit —
  this is a real strength given how tempting a tolerance-based fallback would be under
  schedule pressure.
