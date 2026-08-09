# Issue #483: TASK-0112: a design you did not write can be checked, proved equivalent (or not) with a replayable counterexample, and told which parts the vectors never touched
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

This is an unusually rigorous issue by this repo's own standard (falsification
criteria, a confirmation step for counterexamples, exit-status discipline,
explicit non-goals). Most citations re-derive correctly at current HEAD. The
findings below are about staleness introduced by repo changes made *after*
filing, one real misdescription of the precedent this task leans on, an
unbounded and unbudgeted scope compared to the roadmap it claims to implement,
and gameability in the acceptance criteria around the two riskiest exit codes.

## Findings, most severe first

### 1. Scope is roughly 2-3x the roadmap's own sizing for the same work, with no cost stated in the issue

`docs/capability-roadmap/sweep-04-verification.md` sizes the pieces this issue
bundles separately: change B (drawable `Assert`, "the element itself is small")
= 2-3 maintainer-weeks; change D (`CoverageCollector`, all four measures plus
a native report) = 2-3 weeks; change F (equivalence checking on top of B) =
1-2 weeks; the counterexample-writer half of change G = part of a 3-4 week
band. Summed, the pieces this issue claims (minus G's SMT-LIB/AIGER export,
which it doesn't do) land north of 8-10 maintainer-weeks, and that is before
building an AIG library, a Tseitin encoder, and a bundled SAT solver from
scratch in Java — none of which the roadmap costs at all, because the
roadmap's own change G explicitly assumes solving is *delegated* to ABC/
btormc/SymbiYosys, not built in-tree (`sweep-04-verification.md:428-471`,
quoted below in finding 2). The issue's "## 8. Method" section has thirteen
checklist items spanning a new package (`jls.formal` with four+ classes), a
new collector, two new drawn elements with full registration tax, five new
CLI flags, and thirteen acceptance tests — with **no cost estimate anywhere
in the issue**, unlike its sibling #482 (which the roadmap doesn't cost
either, but at least scopes to one mechanical change) and unlike the
roadmap's own per-change sizing discipline that this issue otherwise leans on
so heavily. A reviewer picking this up has no way to tell "two days" from
"six weeks" from the issue text itself.
**Recommendation:** split `jls.formal` (AIG/Tseitin/SAT/Miter/equivalence)
out as its own task from `CoverageCollector` + `Assert`/`Cover` registration.
They have almost no shared code — the interface section itself says the two
only meet at "build the AIG once, and both fall out of it" for the *optional*
exact-uncovered-set query, which is called out as a nice-to-have, not core.
Ship coverage and the drawn elements (closer to the roadmap's B+D, ~5-6
weeks) first; the SAT/equivalence half is a genuinely separate, larger,
riskier project and should get its own falsification criteria and DoD.

### 2. The "orchestrate vs. reimplement" framing silently omits a more permissive path the repo's own decision record already opened

Open Question 1 frames the SAT solver choice as: (a) reimplement/absorb a
"GPL-compatible" solver in-jar (recommended), or (b) an external process,
rejected because "the single self-contained offline jar is load-bearing and
an external solver breaks it." That is a legitimate call given the offline-jar
constraint, but the issue never mentions that a **third**, cheaper option is
sanctioned by the same decision record it partially relies on: issue #485
("D8. 'Orchestrate external tools, never reimplement' is NOT a maintainer
decision. REVOKED as policy") lays out the actual cost axis and explicitly
says JLS being GPL-3.0-or-later "can ABSORB most open-source EDA code
outright" (BSD, ISC, LGPL, MIT are all fine; "the genuine hazard is narrow
and specific: GPL-INCOMPATIBLE licences"). The issue's own section 6 already
says roughly this ("licensing is more permissive than the corpus assumed")
but attributes it to a citation that is now dead (finding 3) rather than to
#485, and doesn't consider a middle path — e.g. porting/absorbing an existing
*permissively-licensed* small solver (MiniSat is MIT) wholesale rather than
"reimplementing" one from a paper, which changes the size estimate
materially. This is not wrong, just underspecified: "reimplemented or
absorbed" conflates two very different costs and the issue picks neither
explicitly.
**Recommendation:** decide explicitly whether this is a from-scratch DPLL/CDCL
implementation or a straight port of MiniSat/PicoSAT (both MIT), state which,
and cite #485 (D8) rather than the dead BRIEF.md path.

### 3. Citations to `docs/plan/evidence/BRIEF.md` are dead at current HEAD; the correct replacement (#485) already existed when this issue was filed

Section 6 cites "`docs/plan/evidence/BRIEF.md` section 13, landed
`3a81a4a7d6a0f108ec201e632732d308cc02b3fc`, not present at `2d0ca9d`" for the
licensing determination, and the Related Work table cites the same file's
section 12 for "D10" (the gap-not-a-blocker note). Verified against the repo:
`docs/plan/evidence/` does not exist at HEAD — it and the rest of
`docs/plan/` were deleted in commit `742da745c6e5eac3da161ef6d4a1fee9ac2e38ee`
("docs: remove the planning corpus now that it is encoded in issues"), which
landed the same day as this issue, roughly 2.5 hours after it was filed. That
commit's own message says the D1-D16 rulings were migrated to issue **#485**,
which was filed at `18:52:38Z` — twenty minutes *after* this issue
(`18:32:33Z`) and over two hours *before* the deleting commit. So a correct,
resolvable citation (#485) already existed at filing time and was not used;
the issue instead points at a path that a reviewer following it today gets a
404 on. This is exactly the failure mode the issue's own Completion Criteria
guard against ("Every cited permalink resolves on the default branch at
close — no branch-path links") and that rule 6 ("supersession check") is
supposed to catch on pickup — it will be caught, but only by the executor
re-deriving citations from scratch, which the issue does not flag as needed
here specifically.
**Recommendation:** replace both `docs/plan/evidence/BRIEF.md` citations with
pointers into #485 (D8 for licensing, and note D10's "gap, not a blocker"
language covers the "no issue covers this" citation in Related Work) before
this issue is picked up.

### 4. The stated precedent ("Stop's existing condition input") is not accurate to the code

Section 1 and the interface section both lean on: "`Stop`'s existing condition
input is already a property assembled out of gates, which is the precedent
this task generalises," and the public-interface sketch for `Assert` says
"one condition input, like Stop's". Reading `src/jls/elem/Stop.java:42-46`,
Stop actually has **four** unlabeled inputs (`input0`-`input3`), each wired
independently, pruned down to only the attached ones
(`pruneDetachedInputs`, `:54-82`) or reset to all four if none are attached;
`react` (`:149-159`) stops the simulator if *any* attached input is
non-zero — an implicit OR over up to four wires, not "a condition input"
(singular). The actual source of the "assembled out of gates" framing is
`docs/capability-roadmap/sweep-04-verification.md:180-183`, which is about an
*external* magic-address comparator feeding into Stop in the RISC-V
compliance flow, not about Stop itself having one condition port. This
doesn't threaten the technical plan — `Assert extends LogicElement` with one
input is a fine, arguably better, design — but the issue's own precedent
citation for why that's the right shape doesn't match the class it names,
and a reviewer who checks it (as this review did) loses a bit of trust in
the rest of the citation work.
**Recommendation:** fix the precedent citation to describe Stop accurately
(multiple inputs, implicit OR, no gate logic of its own) or drop the
"Stop's existing condition input" framing and just cite the RISC-V
magic-address-comparator-into-Stop pattern directly, which is the actual
precedent for "a property assembled out of gates."

### 5. Exit statuses 4 and 5 are the one place the acceptance criteria are honest about gameability — but P13 only tests one harness's default mapping

The issue is unusually self-aware about this risk (T1, P13, the DoD item
"unknownAndNotCheckableAreNeverReportedAsPasses() exercised through the
grading harness"). But P13 as written only pins the harness's *default*
mapping table for the six statuses; it does not require that the harness
*reject* or *flag* a config that maps 4/5 to pass, nor that `-cov`/`-equiv`
output make it structurally hard for a downstream script to treat "exit
code != 1 and != 2" as "graded". A grading script is exactly the kind of
thing instructors will hand-roll outside JLS's own harness (the issue's own
O8/riscv precedent shows this happens today with `autograde.py`), and
nothing in this task stops a third-party script from doing
`if exit_code in (0, 3): fail else: pass` — 4 and 5 both fall into the "else"
bucket only if the script enumerates all four failure codes, which is
exactly the mistake T1 warns about and which this task cannot enforce
outside its own harness.
**Recommendation:** this is inherent to shipping exit codes at all and can't
be fully closed, but `docs/batch-interface.md`'s new status table should
carry a loud, quotable warning sentence (not just "never passes" in a table
cell) recommending scripts branch on `== 0` for success rather than
enumerating failure codes, since that's the only mapping that's safe by
construction against a *future* status 6, 7, etc. too. Worth stating in the
issue as an explicit doc requirement rather than leaving it implicit in P13.

### 6. `blocked_by` #466 and #482 are both still open — this issue is not actionable as filed, and #482 is a hard *technical* blocker, not just organizational

Verified: #466 (open) owns the report channel and exit status 3 that this
issue's `-cov`/`-equiv` output and exit 4/5 need to land on. #482 (open) owns
turning `PaletteContractTest.paletteIsTotalOverTheElementRegistry` from "one
entry per registered type" into a per-view contract. This matters more than
a typical `blocked_by` note: right now, that test enforces "every registered
type has exactly one palette entry, full stop" — confirmed still true at
current HEAD (`test/jls/edit/PaletteContractTest.java` still has the global
form; `ElementRegistry` currently registers 35 types). Registering `Assert`
and `Cover` today, before #482 lands, would force two new toolbar buttons
into the default first-year palette (satisfying the current, unmodified
totality test) rather than the "non-default view" placement section 7.4
promises — i.e. the plan **cannot** be executed correctly against the
current tree; it structurally requires #482's rewritten contract to exist
first. This is stated in the issue (Method step 1) but stated as a
process/sequencing note rather than flagged as "this issue will produce
wrong behavior, not just a merge conflict, if built against current HEAD."
**Recommendation:** no action needed beyond what's already there, but worth
the executor double-checking `PaletteContractTest`'s shape at pickup time
before writing `Assert`/`Cover`'s palette rows, since a naive implementation
against stale local branches would pass the *current* test while violating
the design intent.

## What's solid (no action needed)

- O1-O5's failure-mode observations (no `jls.formal`, `-cov` rejected, no
  `Assert`/`Cover` in the 35-entry registry) all reproduce exactly as
  described against current HEAD, including the specific `TruthTable`
  silent-hold code path at `src/jls/elem/TruthTable.java:1404-1434`.
- The mandatory-confirmation design for counterexamples (stage 3, H4, P4) is
  the strongest part of the issue: refusing to report status 3 without
  replaying the vector through the ordinary batch path against both designs
  is exactly the right discipline for a grading tool, and it's tested, not
  just asserted in prose.
- The partiality-first ordering in Method ("implement stage 1's partiality
  first ... before implementing the successful path") is a good sequencing
  call that reduces the risk of shipping a checker that proves things it
  shouldn't.
- The explicit refusal of unbounded model checking (T5, "Do not become a
  model checker") is well-scoped and testable via "no induction, PDR/IC3 or
  reachability anywhere in the diff."
- The four coverage measures and their instrumentation points (toggle at
  `WireNet.propagate`, transition at `State.getNextState()`, row at
  `TruthTable`'s `matchingRow`) all check out against
  `sweep-04-verification.md`'s change D and against current source line
  ranges close enough to be usable as landmarks.
