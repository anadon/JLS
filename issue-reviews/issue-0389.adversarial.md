# Issue #389: TASK-0065: a subcircuit instance names which implementation runs, the choice survives a save, and no file can change fidelity silently
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue's forensic evidence (§ Observations O1–O6) is unusually well
grounded: every cited `SubCircuit.java`/`Circuit.java`/`JLSStart.java` line
number, code snippet, and test-class name was independently checked against
HEAD and matches exactly (`SubCircuit.save` 282–289, `saveFormatVersion`
299–302, `setValue` 311, `Circuit.FORMAT_VERSION` 102, the five
`el.setValue(name, value)` call sites at 1067/1078/1089/1105/1116, the
14-row `FLAGS` table at 759–789, and all six exact-save-bytes suites named
in §7.12). The `evidence_commit` hash exists in this repository's history.
That rigor makes the design-level problems below more concerning, not
less: this is a carefully-researched issue whose central mechanism does
not survive contact with its own stated scope boundary.

## Findings, most severe first

### 1. P3 (and, on inspection, P4) cannot be verified within this task's own declared scope — the closed permits set has exactly one member

`jls.sim.SubCircuitImpl` is specified as "**sealed** interface permitting
`StructuralImpl` (the only implementation this task ships)" (§7.4), and the
Completion Criteria require "`SubCircuitImpl`'s permits list contains
exactly `StructuralImpl` at close; no second implementation rode along."
§11 reinforces this: *"This task ships no second implementation... Do not
let a 'small' second implementation ride along — the sealed permits list
is the guard."* Combined with §7.11 ("An `impl` id outside the closed set:
**refuse by name** at load... Do not default to structural"), the closed,
legal set of `impl` values at the close of this task is `{"structural"}`
— cardinality one.

P3 requires: *"Save a circuit with two instances of one definition
carrying different `impl` values; reload; observe each instance kept its
own value..."* This demands at least two distinct, legally-round-trippable
values. With only one legal value, P3 as written is either (a) untestable
without violating the task's own scope (fabricating a second value that
isn't a real, sealed-permitted implementation), or (b) testable only by
constructing a file with an illegal id (e.g. the issue's own example,
`"levelized"`) — which §7.11 says must be **refused by name at load**, not
silently round-tripped. A refused load cannot also be a load that
"observe[s] each instance kept its own value." P4's parallel claim — "a
file with any other value emits FORMAT 3" — has the same problem in the
forward direction: nothing in this task gives a user (via `-fidelity`, the
`-s` parameter file, or the GUI) a legitimate way to select any `impl`
other than `"structural"`, so the writer can never legitimately emit a
FORMAT 3 file through ordinary use; the only way to construct one is to
hand-author a raw text fixture exactly as O1 does today — meaning P4 is
really re-testing O1's bug-reproduction technique, not a new capability.

**Recommendation:** Either (a) explicitly scope P3/P4's fixtures as
white-box test-only constructions that bypass the normal write path (and
say so, so a reviewer isn't misled into thinking `-fidelity`/the parameter
file can produce them), or (b) restate P3 to use `"structural"` on both
instances (which proves nothing about per-instance independence beyond
what O5 already establishes), or (c) fold a second, trivial no-op
implementation into scope so the predicates are actually exercisable
end-to-end — which contradicts the explicit "no second implementation"
scope cut and would need to be renegotiated with FEAT-031 (#325).

### 2. Direct conflict with the parent feature's own "blocks integration" open question on format versioning

FEAT-031 (#325), which `part_of_feature: 325` in this issue's own header
names as the owner, has this as **Open Question 2**, explicitly marked
"**Blocks integration**":

> *"Which epoch does the fidelity attribute rely on? Recommended default:
> declare it in whatever epoch the section-versioning feature opens, and
> **say so in TASK-0065 rather than assuming a version bump protects
> it**."*

and, in #325's own Re-planning Protocol:

> *"The epoch interaction. The 'costs no format version' argument rests
> on unknown attribute names being silently ignored — which is exactly
> the behaviour the fail-loud-loader feature removes... The two features
> must **agree on the epoch** (the section-versioning feature owns it)
> **rather than each assuming**. A change to that epoch is a `REPLAN:`
> trigger here."*

The "section-versioning feature" is FEAT-013 (#319, confirmed open,
`TASK-0033`/`0034`/`0071` all "not filed"), whose entire point is
per-section must-understand versioning as the alternative to a whole-file
version bump. TASK-0065 (#389) does exactly what #325 told it not to do:
it "assumes a version bump protects it," bumping `Circuit.FORMAT_VERSION`
from 2 to 3 unconditionally as the mechanism (H2, §7.10, Method step 5),
and neither cites #319/FEAT-013 nor records a decision about which epoch
the two saved attributes rely on. This isn't a stylistic gap — #325 flags
exactly this failure mode as a `REPLAN:` trigger for the feature that
owns TASK-0065. Landing #389 as specified either needs a documented
resolution of #325's Open Question 2 (e.g., "FEAT-013 is unfilled and out
of critical path, so we accept a whole-file bump now and revisit at the
section-versioning epoch") or a rework to defer to that epoch. As written,
the issue is silent about a decision its own parent explicitly requires
before proceeding.

**Recommendation:** Add a paragraph resolving #325 Open Question 2 (why a
whole-file `FORMAT 3` bump is acceptable now given FEAT-013 is unfiled),
and cite #319 in § Related Work. Absent that, this task should not be
picked up ahead of FEAT-013 without a recorded maintainer decision.

### 3. `implDelay`'s "structural critical-path default" is a nontrivial, unbudgeted algorithm — and, in this task, has no consumer

Open Question 2 ("Blocks execution"): *"What is the structural
critical-path default for `implDelay`? ... Recommended default: the
drawn boundary's computed critical path..."* Computing a per-boundary
critical path over propagation delays is a real graph algorithm (a
topological longest-path walk over the subcircuit's element/wire delay
graph). Neither § 6 Materials & Apparatus ("Everything needed ships...
New: `jls.sim.SubCircuitImpl` and `StructuralImpl`; two attributes; the
conditional gate; one `FlagSpec` row; one `-s` parameter-file directive;
the manifest; and the tests") nor the § Method checklist contains a line
item for writing or testing this computation — it appears only inside an
"Open Questions" answer. Separately, since this task ships no second
implementation and `StructuralImpl` simulates the drawn circuit element-
by-element (never collapsing it to a single lumped delay), nothing in
this task's own code path ever *reads* `implDelay` to change behavior —
it is saved, gated by FORMAT 3, and printed in a manifest that (per
Finding 1) can never legitimately fire. §7.12 point 6 insists `implDelay`
"must never be ignorable on its own," but for the duration of this task
it is, functionally, inert.

**Recommendation:** Either drop `implDelay` from this task's Materials and
defer it to whichever task ships the first non-structural implementation
(it has no meaning until then), or add an explicit Method step for the
critical-path computation and its test, and reconcile the "no second
implementation" scope cut against introducing a delay-modeling algorithm
that only a second implementation would consume.

### 4. `sim.subcircuit-implementation` doesn't fit the extension-point catalog's own documented conventions, and is declared permanently non-pluggable

`docs/extension-points.md`'s stated id convention is "kebab-case,
dot-prefixed by its home area (`elem.`, `hdl.`, `collab.`, `gui.`,
`app.`)" — there is no `sim.` area in the current table, and the issue
doesn't propose adding one to the convention list, just a `pending` row
under it. More importantly, #223 (the catalog's owning issue) describes
"pending" as: *"the seam is named and owned by the listed issue, and its
contract lands with that issue"* — i.e., pending implies an eventual
typed contract via `jls.module.ExtensionRegistry`. This issue's own §11
says the opposite: *"`SubCircuitImpl` is not an extension point... it
gets a pending catalog row so the seam is named before it is populated,
**not so it can be plugged into**."* Combined with #221's closed decision
that the sim inner loop has "zero plugin indirection" (permanent, by
design), this seam can never actually resolve to "typed now" the way
every other pending row in the catalog is expected to. Filing a
permanently-pending row in a catalog whose own purpose statement is "the
concrete API surface of the *module program*" is a category mismatch
that a reviewer familiar with #223's intent is likely to push back on,
and it risks the row rotting as the one entry that never resolves either
way.

**Recommendation:** Either get explicit maintainer sign-off (a short
addendum to #223 or #224) that a permanently-pending, never-typed row is
an accepted use of the catalog, or drop the extension-points.md
requirement (P9) from this task and instead document the seam directly in
ARCHITECTURE.md/grand-architecture.md §6, which is where the issue's own
argument ("core-internal, zero plugin indirection") actually lives.

### 5. Two "blocks execution" open questions are left as recommendations, while the rest of the issue writes code as if they were settled

Open Questions 1 and 2 are each marked "**Blocks execution**," yet
Predictions, the Method checklist, and the Completion Criteria all proceed
as though both were already decided (manifest on stdout; `implDelay`
default = computed critical path). A checklist item can't honestly be
checked "done" while an issue-defined blocker for the same work remains a
"recommended default" rather than a recorded decision. This is process
debt the issue itself names but doesn't discharge.

**Recommendation:** Convert both into an explicit maintainer ruling (a
`STATUS:`/decision comment) before implementation starts, per the issue's
own rule 6/rule 10 vocabulary used elsewhere in this corpus.

### 6. Batch stdout is a named "stability contract"; this issue's own audience (graders) is the group most exposed to changing it

`docs/batch-interface.md` §3.2 and the README both call the two-line
stdout format a documented stability contract. This issue's own "Intended
Audience & Impact" section opens with *"Instructors grading in batch (-b)
mode"* as the primary audience, then proposes a third, conditionally-
printed stdout block (the fidelity manifest) gated on a per-run condition
external to the grading script's control (whether any instance in a
*student-supplied* file happens to be non-structural). P10 protects the
all-structural case byte-for-byte, but says nothing about graders whose
parsers assume exactly two stdout artifacts and choke on an unexpected
extra block the moment a student's file trips the condition — which,
notably, could happen through the illegitimate-load path in Finding 1
even without a deliberate choice by the instructor. This is called out
honestly as Open Question 1, but given the audience the issue leads with,
it deserves to be treated as a compatibility risk in § Threats to
Validity, not just an open question with "rides along" left unresolved
until H4's refutation branch.

### 7. Unacknowledged prior art: `docs/file-format.md`'s `sync` precedent (issue #199) left the identical policy question open

`docs/file-format.md:472–479` documents `Memory`'s `sync` attribute as
"a known instance of this class" (ignorable-but-behavioral) and states
*"whether files containing it should declare a bumped `FORMAT` version is
an open question tracked with issue #199's follow-ups."* TASK-0065 is
solving the exact same category of problem for a second attribute and
adopts a specific, confident answer (conditional version bump) without
citing #199 or explaining why that precedent's open question gets
resolved here differently/first. At minimum this should be cross-
referenced so the two decisions don't diverge for structurally identical
cases.

## What's solid

- **O1–O6's code citations are all accurate** — independently re-verified
  against HEAD, including exact line numbers and the evidence-commit hash.
- **The six exact-save-bytes test suites named in §7.12 all exist** in
  `test/` (`DeterministicSaveTest`, `CircuitRoundTripTest`,
  `AllElementsRoundTripTest`, `FileFormatSpecTest`, `CliTextSaveTest`,
  `GenerativeRoundTripFuzzTest`), confirmed by direct search.
- **The grammar claim (H1)** — `String`/`int` items need no new record
  kind — is correct against `docs/file-format.md`'s item grammar (§3,
  lines 125–132).
- **Cross-references to concurrently-filed sibling tasks check out**:
  TASK-0079 and TASK-0003 are both filed and open (#392, #404
  respectively), matching the "filed concurrently" claims even though
  #389 doesn't give their numbers directly.
- **The "no prerequisite" claim (Status & Dependencies) is genuinely
  verified**, not asserted — the cited methods all exist at the pinned
  commit exactly as described.

## Verdict rationale

The evidentiary discipline is high, but two of the findings (1 and 2) are
not nitpicks: Finding 1 means a stated falsifiable prediction cannot be
honestly satisfied inside the task's own scope fence, and Finding 2 is a
documented, explicit contradiction with the owning feature's own
"blocks integration" decision gate. Both are fixable by rescoping the
predictions and by writing (not assuming) the epoch decision — hence
"needs-rework" rather than "should-not-proceed."
