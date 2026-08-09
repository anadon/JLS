# Issue #660: TASK-C566-4: the FSM analysis the assessment says should be scriptable is callable with no display present
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the issue as filed

TASK-C566-4 (part of feature #566 / capstone CAP-31 #515) commits to making
"whatever FSM analysis exists after the gaps are disposed of" headless-callable,
with output "machine-readable and byte-deterministic," documented under the
existing batch-interface contract, citing `docs/batch-interface.md` and #524.
It explicitly depends on TASK-C566-1 (#657, the parity assessment document)
and TASK-C566-2 (#658, gap disposition) via `ordering_after`, and allows for
closing as verified-with-no-op if the assessment finds nothing to expose.

## Findings, most severe first

### 1. The issue's entire scope is undefined at filing time, yet it is already budgeted (band_mw: "1")

The task's content — which capabilities get a flag, what the flag names are,
what the output schema is — is 100% deferred to TASK-C566-1's assessment
document (#657), which does not exist yet: `mcp__github__issue_read` on #657
shows 0 comments and an open state with no linked artifact. Quoting #660
itself: *"Whatever FSM analysis exists after the gaps are disposed of is
reachable from a script wherever TASK-C566-1's assessment says it should
be."* An issue whose acceptance criteria are entirely indexed to a document
that doesn't exist cannot be scoped, estimated, or reviewed for feasibility
today — yet the YAML frontmatter already commits `band_mw: "1"` (one
machine-week). That is a concrete estimate for unknown-sized work: the
assessment could find zero capabilities needing headless access (the
Boundary notes explicitly allow closing with no code), or it could find five
distinct analyses each needing their own flag, exit-code row, and schema
section. Both extremes get the same 1-mw budget. **Recommendation:** either
strip the `band_mw` estimate until #657 lands and re-estimate against the
actual gap list, or state explicitly that the estimate is a placeholder not
to be trusted for scheduling.

### 2. Dependency on #657/#658 is asserted only in freeform YAML, not enforced by GitHub issue relationships

`ordering_after: ["TASK-C566-1 (which names what should be headless)",
"TASK-C566-2"]` is prose inside a fenced code block in the body — it is not
a GitHub "blocked by" link. `issue_read` on #660 reports `has_parent: false,
has_children: false`, confirming no structural linkage. Nothing stops a
contributor (or an automated agent picking issues off a label queue) from
picking up #660 before #657's document exists or before #658 has disposed of
the gaps, since the tracker itself shows no hard block. **Recommendation:**
use GitHub's native issue-dependency mechanism (or at minimum a pinned
"blocked by #657, #658" line at the top of the body) so tooling that
respects blocking relationships won't surface this as ready work prematurely.

### 3. AC-3 is a documentation-citation criterion with no verification mechanism

AC-3: *"A capability the assessment explicitly marked as GUI-only is not
exposed here, and that decision is cited to the assessment rather than left
implicit."* This can be satisfied by writing a citation next to an omission
— there is no proposed test, lint, or cross-check that the cited assessment
row actually says GUI-only, or that the set of omitted capabilities in the
implementation matches the set the assessment marked GUI-only. Contrast with
the rest of the batch-interface contract style in this repo, where every
clause has a named golden test (`docs/batch-interface.md` §5 lists
`BatchSimulationGoldenTest`, `VcdExportGoldenTest`, `CliFlagTableTest` per
clause). #660 names no analogous test for AC-1–AC-3. A reviewer could accept
a PR that under-implements (silently drops a capability the assessment
wanted headless) by writing a plausible-sounding citation, and nothing in
the acceptance criteria would catch it. **Recommendation:** require a table
(assessment row ID → exposed/refused/GUI-only, with the flag or the
citation) checked by a test that walks the assessment doc's numbered gap
list (#657 AC-4 promises the list is "explicit and numbered") and asserts
every row has a disposition in the CLI surface or an explicit "GUI-only"
tag — mirroring how #658 is supposed to guarantee no gap "leaves the
assessment without a disposition."

### 4. AC-2 ("byte-deterministic") names no oracle, and the codebase has a known landmine for exactly this failure mode

AC-2: *"Output is machine-readable and byte-deterministic for a given
circuit."* No test class, fixture, or golden is named — again inconsistent
with the rest of this project's batch-interface documentation discipline.
This matters concretely here: `StateMachine.states` is declared
`private Set<State> states = new HashSet<State>();`
(`src/jls/elem/StateMachine.java:54`), and `State.trans` is
`private Set<Transition> trans = new HashSet<Transition>();`
(`src/jls/elem/State.java:31`). Both are HashSets with no `hashCode`
override on `Transition` (per the comment at `State.java:364-367`), so
direct iteration is identity-hash order and non-deterministic across JVM
runs — this is precisely the bug class issue #72 already had to fix once
for watched-element stdout order (`docs/batch-interface.md` §3.2: *"Before
issue #72 this order was HashSet iteration order and therefore unstable;
it is now pinned"*), and issue #180 already fixed it for save output and
HDL export via `State.saveOrder()` / `State.getTransitionsInSaveOrder()`
(`State.java:288-345`). #660 does not cite #180 or point an implementer at
the existing canonical-order comparators, so whoever implements the new FSM
listing/output-mode-reporting flag could easily reintroduce the exact
nondeterminism bug this codebase has already paid down twice, and violate
its own AC-2 on day one. **Recommendation:** the issue should explicitly
require reuse of `State.saveOrder()` / `getTransitionsInSaveOrder()` (or an
equivalent canonical order consistent with the existing save/HDL-export
order, not a fourth independent ordering convention) and should name a
determinism golden test analogous to `BatchSimulationGoldenTest
.watchedElementsPrintInNameOrder`.

### 5. The #524 citation misdescribes the current state of the contract it claims to build on

Boundary notes: *"The batch CLI stability promise is #524; this adds flags
under it."* But `docs/batch-interface.md` line 3 already states: *"Status:
normative, and a stability contract"* — the promise already exists today,
independent of #524. #524 (open, unimplemented, 0 landed work per its own
body) is a *separate* effort to turn that existing informal-but-normative
document into a **formally frozen, conformance-tested, semver-ratcheted**
interface (`CliContractConformanceTest`, a seeded-violation CI check, a
written versioning policy — none of which exist yet). Citing #524 as "the
stability promise" conflates "a promise exists" (true today) with "the
promise is executably enforced" (not true until #524 lands). This creates a
real sequencing hazard: if #660 ships new batch flags before #524's
conformance suite exists, those flags enter the surface #524 will later have
to retroactively freeze, possibly under different discipline than what #660
's implementer chose ad hoc. **Recommendation:** either state plainly that
#660 is *not* blocked on #524 landing (and accept the flags may need
retrofitting into #524's conformance suite later), or make the dependency
explicit and order #660 after #524 the way it's already ordered after #657
and #658.

### 6. "whatever the assessment concluded a grader needs" is open-ended scope language inside a task tier

AC-1 reads: *"state/transition listing, output-mode reporting, or whatever
the assessment concluded a grader needs."* Labeling this a `tier:task`
(the most granular tracker tier, per the repo's label taxonomy alongside
`tier:feature`/`tier:capstone`) while leaving its content an open
"whatever" is scope creep dressed as precision — a task should be the
concretely-scoped leaf, not a second capstone-shaped open door. If the
assessment lists ten scriptable capabilities, this "task" silently becomes
feature-sized work under a task label, which undermines the `band_mw`
estimate (finding #1) a second time. **Recommendation:** cap this task to a
named subset (e.g. state/transition listing only) and file follow-up tasks
for anything the assessment adds beyond that, rather than letting one task
absorb an unbounded assessment output.

## What's solid

- Requiring headless-callability under the *existing* batch-interface
  contract style (flags documented alongside current ones, machine-readable
  output) is consistent with this repo's actual architecture — `Simulator`
  and `BatchSimulator` are already headless-by-construction
  (`ARCHITECTURE.md` "Simulation": *"Headless by construction (issue #77)"*)
  , so there's no structural reason an FSM analysis couldn't follow the same
  path as truth-table/VCD export.
- The verified-close escape hatch ("if the assessment concludes no FSM
  analysis needs headless access, this closes as verified with citation") is
  a genuinely good design against manufactured work — it correctly avoids
  shipping a flag nobody asked for, and is consistent with CAP-31's
  KC-31-2 kill criterion.
- AC-4 (document flags/exit codes/schema alongside the existing batch flags)
  correctly anchors documentation location to `docs/batch-interface.md`
  rather than inventing a new doc, matching how VCD export (#72) was
  integrated into the same document rather than split out.

## Net assessment

The issue is well-intentioned and mechanically fits the codebase's headless
architecture, but as filed it is not actionable: its scope is fully deferred
to a document that doesn't exist (#657), yet it is pre-estimated and
labeled as a fine-grained task; its determinism acceptance criterion has no
named oracle and sits directly on top of a known HashSet-ordering hazard
this codebase has already been bitten by twice (#72, #180); its GUI-only
exclusion criterion (AC-3) is unverifiable beyond eyeballing a citation; and
its own boundary note misstates the status of the contract (#524) it claims
to build under. None of this makes the goal wrong — it makes the issue
premature to start and under-specified for anyone who does pick it up before
#657 lands.
