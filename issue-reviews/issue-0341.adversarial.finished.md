# Issue #341: FEAT-027: a net has a kind and a driver has a strength, so open-drain buses, pull-ups and floating inputs behave the way the bench behaves
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

The underlying engineering idea — a strength lattice (`HIGHZ < WEAK < PULL <
STRONG < SUPPLY`) feeding a commutative/associative resolution fold, replacing
the current first-driver-in-net-order scan — is a legitimate, well-understood
design (it is essentially VHDL/Verilog `std_logic` resolution). The code
citations against `WireNet.propagate` and `docs/simulation-semantics.md` §9
check out exactly against the current tree. The problems are not with the
target design; they are with the issue's evidentiary basis, its internal
consistency, and the fact that the issue body has already been overtaken by
its own comment thread in ways that leave the body actively misleading if
read alone.

## Findings, most severe first

**1. [High] The pinned `evidence_commit` does not exist in this repository, and the issue's own later comment admits it.**
The machine block declares `evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`,
and every "ABSENT at 2d0ca9d" / line-number claim in the body is stated
against that pin. `git cat-file -e 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`
fails in this checkout (unknown revision). This is not just a shallow-clone
artifact of my checkout: the issue's own 2026-08-08 "RULING" comment
independently confirms it — *"This issue declares `evidence_commit: 2d0ca9d…`,
which does not resolve — verified, `git cat-file -t` returns 'could not get
object info'. Per #493 the durable equivalent is
`828822672fc3a8e2cb6da25192472079f04c29dd`."* I confirmed that replacement
commit does resolve (`git cat-file -e 82882267… `→ exit 0, a real merge
commit authored by the repo owner). So the issue shipped with a fabricated or
unreachable evidence pin, and it took a later self-review pass to notice and
patch it via comment rather than by editing the body. **Recommendation:**
before funding, re-pin the machine block to a commit that actually resolves
in this repository, not just in a comment three weeks later.

**2. [High] `docs/plan/features/` — the cited source of every ordering edge in this issue — does not exist anywhere in the repo.**
The "LINK PASS 2026-08-02" paragraph asserts `blocked_by: [322]` and
`blocks: [329]` are "derived from the corpus ordering record — the §
Prerequisite features table of every one of the 57 feature documents under
`docs/plan/features/`, read in both directions." `ls docs/` shows no `plan/`
subdirectory at all (`architecture-project-setup.md`, `simulation-semantics.md`,
`file-format.md`, etc. exist; `plan/` does not), and `git log --all
--diff-filter=A --name-only` finds no commit that ever added a `docs/plan/`
path. The scheduling claims this issue is built on top of (its position in
the dependency graph, which capstones require it, the cost-band rollup) all
trace back to a document corpus that is unauditable from this tree. This is
the same defect independently flagged on sibling issues in this batch (see
`issue-reviews/issue-0347.adversarial.md` and `issue-0359.adversarial.md`,
which hit the identical missing-`docs/plan/`-and-bad-evidence-commit pattern
on other FEAT issues by the same author/process) — it is a systemic
authoring problem in this planning corpus, not a one-off typo.
**Recommendation:** either point at the actual repository/branch this
planning corpus lives in, or strip every `blocked_by`/`blocks`/cost-band
claim sourced from it and re-derive scope and sequencing from what is
verifiably in-tree.

**3. [High] The issue body is stale in ways its own comments admit but never fold back into the body — a reader who stops at the body will verify the wrong thing.**
The 2026-08-08 "RULING" comment overturns §1 criterion 5 as literally
written: the body says *"Net kind is a saved, versioned property, and a
reader that does not understand a net kind refuses the file rather than
silently treating it as an ordinary net"* (with Open Question 4 recommending
a format-version bump for net kind). The RULING comment decides the opposite
for net kind — *"`NetKind` is DERIVED, not saved… §1 criterion 5 is
superseded in its 'saved, versioned property' clause for net kind"* — and
retargets the reader-refusal requirement to `DriverKind` instead. But §1,
§5, and the Definition-of-Done checklist in the **body itself** were never
edited to say this; they still read as originally written. Someone using
this issue's checklist to sign off on completion would be checking off the
wrong artifact (a saved, versioned net kind) against a design that a later
authoritative comment says must NOT exist that way. **Recommendation:**
fold the RULING comment's decision into §1/§5/DoD by editing the body, not
by leaving a third comment as the only place the current truth lives.

**4. [Medium] The cost band is presented as 6-9 maintainer-weeks, but the issue's own arithmetic (later confirmed by its own comments) shows this feature's actual remaining scope prices out to ~2 weeks against an unrevised 6-9 week band.**
§ Cost states: *"if Open Question 1 resolves as recommended, this feature's
*exclusive* row sum drops to 2 wk (TASK-0058 alone)… and that gap becomes
this feature's real residual."* The 2026-08-08 RULING comment confirms Open
Question 1 *did* resolve as recommended (TASK-0057 → #322, TASK-0049 →
#339, TASK-0093 → #329, leaving TASK-0058/#387 as this issue's sole child)
and restates the same 2-wk-vs-6-9-wk gap as "this feature's live state."
Nothing in the body or comments ever re-derives or defends the 6-9 week
figure for what this issue now actually owns — it is inherited wholesale
from a program-level roadmap total that mostly prices work now owned by
#322, #339, and #329. A maintainer approving "6-9 weeks" for #341 today is
approving a number that was already known, by the issue's own math, to
substantially overstate this issue's remaining scope. **Recommendation:**
publish a corrected cost line for the issue as currently scoped (TASK-0058
alone, plus the two new obligations layered on afterward — see #5, #6
below) rather than leaving the reader to reconstruct it from three comments.

**5. [Medium] Acceptance criterion 3 (the wired-AND bus test) is gameable as worded.**
§1 criterion 3: *"a wired-AND bus of N open-drain drivers plus one pull-up
resolves correctly for all 2^N driver combinations at small N (exhaustively)
and by sampling above."* "Small N" has no stated bound, and "sampling above"
has no stated sample size or generation methodology (random? adversarial?
how many trials?). As written, an implementer can satisfy the letter of this
criterion with N=2 exhaustive and a single arbitrary "sample" above that,
while a subtle strength-lattice tie-break bug at N=5 or N=8 goes undetected.
**Recommendation:** name a concrete N for exhaustive coverage (e.g. N≤6) and
a concrete sampling budget/method (e.g. "≥1000 random driver-value/strength
combinations at N∈{8,16,32}") before this is treated as a real gate.

**6. [Medium] Open Question 2 (does the driver-kind vocabulary cover TTL only or TTL+CMOS) is marked as blocking TASK-0058, but TASK-0058 has already been filed (as #387) without resolving it — and the issue's own comment thread flags this as unresolved twice without correcting course.**
Body: *"Options: (a) TTL and CMOS both… recommended; (b) TTL only… **Blocks
TASK-0058**."* The 2026-08-04 boundary comment finds that #387's `DriverKind`
enum (`PUSH_PULL, OPEN_DRAIN, OPEN_SOURCE, PULL`) "has no technology axis at
all," and the 2026-08-08 RULING comment repeats, unresolved, that *"this gap
survives #387's contract unchanged and is still a decision this feature owes
before #387 executes."* So a criterion explicitly marked as blocking child
work was bypassed in practice — the blocking child was filed and is already
in flight without it. Given the issue's own pedagogical framing ("Modeling
one and not the other teaches a falsehood"), shipping TASK-0058 without this
decided risks landing exactly the falsehood the issue warns against.
**Recommendation:** either genuinely block #387 on this question or drop the
word "blocks" from Open Question 2 and admit it is advisory.

**7. [Medium] Contradiction between #341 and its own filed child #387 over whether `DriverKind`/`NetKind` are saved — flagged, "decided," but the decision is itself a scope increase never priced.**
The 2026-08-08 RULING resolves the #341-vs-#387 conflict (§4a-4c of that
comment) by deciding `DriverKind` **is** a saved, must-understand attribute
requiring a `FORMAT_VERSION` bump — reversing #387's original "no format
bump" position — and states plainly: *"This is a scope increase on #387…
a saved attribute, its loader path, its refusal path and the FORMAT bump
were not in its 2 wk row."* That statement doubles as an admission that the
already-cited 6-9 week band for #341 (per finding 4) is now stale in the
other direction too — it needs to go up to cover work just added to #387,
not just down for work moved to #322/#339/#329. Two of the issue's own
comments move the cost estimate in opposite directions and neither produces
a reconciled number.

**8. [Low, positive] The core technical claims check out against the actual codebase.**
- `src/jls/elem/WireNet.java:454-485` is exactly the `if (triState)` block
  described, taking the first non-null `Output` value in net (insertion)
  order and warning once on disagreement — matches the issue's characterization
  precisely.
- `docs/simulation-semantics.md:422-425` and `:439-440` (confirmed by direct
  grep) say exactly what the issue quotes, at the exact line numbers cited.
- `src/jls/elem/TriProp.java` is a 2-method interface exactly as described,
  and the "tri-state-ness propagates at edit time" precedent the issue
  leans on for net kind is accurately characterized.
- `git grep -lE "PullUp|PullDown|Strength" -- src/` returns nothing at HEAD,
  consistent with the issue's "scope verified ABSENT" claim (even though the
  specific commit hash used to make that claim doesn't resolve — see finding 1).

**9. [Low] The out-of-scope boundary against real resistive/analog behavior is well drawn and self-aware.**
"A pull-up is a resistor, and nothing here may introduce a resistance value
the discrete-event engine then has to pretend to solve" (invariant 5, and
repeated in the scope-boundary section) is a clean, defensible line, and the
issue is explicit that it must be documented in element help text so users
don't expect divider ratios. No concerns here.

**10. [Low] Interaction with #329's contention reporting is asserted but not verified end-to-end.**
FEAT-027 criterion 4 says unequal-strength contention "is not reported as a
conflict," while FEAT-043 (#329) needs to report "contention the schematic
hides" on a placed breadboard. These are compatible in principle (equal-
strength X is still a conflict; unequal-strength resolution isn't), but the
composition is only asserted, never jointly tested by either issue's own
criteria — §5 criterion 4 here is "spans TASK-0093 and TASK-0058" but is
recorded as "New fixture at close-out" with no interim check. Minor scope
risk if the two features land far apart in time.

## Bottom line

The technical design is sound and the code-level evidence, where checkable
against the live tree, is accurate. The verdict is "sound-with-concerns"
rather than "needs-rework" because the most damaging defects (bad evidence
commit, unresolvable planning corpus, the #387 net-kind contradiction) have
already been caught and addressed — but only in comments, not in the issue
body, which still contains claims its own author has since retracted. Before
this is treated as ready to execute against, the body should be edited to
match the 2026-08-08 RULING (§1 criterion 5, Open Question 4, the cost
figure), and Open Question 2 (TTL vs CMOS) should be genuinely resolved
before #387 proceeds further, not merely re-flagged a third time.
