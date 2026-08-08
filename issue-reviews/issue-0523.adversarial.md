# Issue #523: FEAT-C05-1: the netlist KiCad receives is provably the circuit JLS simulated — a stable-id net-partition isomorphism gates the export, and parity is narrowed in writing where it cannot be proven
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what's being attacked

#523 files `NetPartitionIsomorphismTest`: a standing check that the net
partition recovered from JLS's own re-parsed KiCad `.net` output is
isomorphic (matched by stable id) to the partition JLS actually simulated.
It positions itself as picking up an orphaned acceptance criterion (AC-2)
from #307 (CAP-13), which closed 2026-08-03 as a duplicate of #298 (CAP-05)
and named this exact check as what "survives... as the acceptance spine of
#298's export." I verified that quote and the surrounding chain (#307 §4,
#298's 2026-08-04 coverage-verification comment) directly against the
fetched issues; the provenance story #523 tells about itself is accurate.
The core technical diagnosis — that a byte-golden test cannot catch a
structurally-wrong-but-internally-consistent netlist, and that a second
connected-component walk would make the isomorphism circular — is real and
matches the identical hazard #307 §3 and #298's KC-05-2 already flag. The
codebase confirms the premise: no `jls.netlist`, `jls.pcb`, or netlist
emitter exists anywhere in `src/` today (checked directly), so this is
fully pre-implementation groundwork.

## Findings, most severe first

**1. The machine block uses a field name (`ordering_after`) the project's own
scheduling tooling does not recognize, and the edge is unmirrored on all
three cited issues — verified live.** #523 carries `labels: [...,
"tier:feature"]` and `ordering_after: [336, 365, 366]` in place of the
`blocked_by`/`blocks` pair every other `tier:feature` issue in this
tracker uses (#336, #365, #366 all declare both fields explicitly). This
matters concretely: #307's and #298's own REPLAN/adjudication comments
describe a mechanical process that "checked every mermaid ordering edge in
all nineteen capstones... against the two endpoint features' own filed
`blocked_by`/`blocks`" and states the rule plainly — "an edge asserted...
that neither endpoint declares is not an edge." I fetched #336, #365, and
#366 directly: #365's `blocks: [329, 366]` and #366's `blocks: []` (per
its own body) contain no reference to #523, and #307's roster comments
quote #336's `blocks` list as `[321, 327, 332, 358, 365, 366]` — also no
523. By this project's own stated rule, **#523's dependency on #336/#365/
#366 landing first is currently not an edge at all** from the perspective
of any tool or reviewer that walks `blocked_by`/`blocks` across
`tier:feature` issues, which #307/#298's history shows is exactly how this
project audits its DAG. This is not cosmetic: the entire point of AC-2 is
to prevent the isomorphism from being asserted before the single shared
partition (#336) exists — an issue whose own ordering claim is invisible
to the tooling that would normally catch someone scheduling it early is
undermining the property it exists to guarantee. *Recommendation:* rename
`ordering_after` to `blocked_by`, and add `523` to the `blocks` list of
#336, #365, and #366 in the same edit.

**2. The cost band is inherited from a scope this issue only half-covers,
and the narrower scope was never re-priced.** `band_mw: 2-3` is sourced,
by the machine block's own comment, from "the claims-1-and-2 demo-slice
band #307's closing comment... carried into #298." But #298's 2026-08-04
coverage-verification comment splits that demo slice explicitly: "claim 1
(accepted) is #366's §5 criteria 1-2; claim 2 (structure preserved) is the
isomorphism check, filed now as #523." Claim 1 (getting `lepton-netlist`
to accept the emitted file with exit 0) is not #523's work — it is already
budgeted inside #366's own 5-10 mw band via TASK-0089's acceptance
criteria. #523 only builds claim 2. Reusing the *combined* claims-1-and-2
price tag for a scope that has since shrunk to claim 2 alone is exactly
the kind of stale-inherited-figure problem this project's own review
process has already caught elsewhere (see `issue-0524.adversarial.md`,
finding 1: numbers corrected in a comment but never re-derived in the
body). #523 does flag its wider 6-12 mw marginal band as "unverified until
#366's emitter split is priced" — so the discipline exists — but applies
no equivalent caveat to the 2-3 mw figure it keeps whole. *Recommendation:*
either re-derive 2-3 mw against claim-2-only scope, or state explicitly
why the full combined figure still applies once claim 1 moved elsewhere.

**3. AC-4 places a new requirement on an emitter owned by an already-filed,
already-detailed sibling task, with no edge forcing the coordination.**
AC-4 requires "the emitted artifact's header states that parity is an
isomorphism on nets." The schematic emitter this would apply to is already
filed in detail as #461 (TASK-0090), whose own §14 Completion Criteria
specifies a *different* header requirement verbatim: "The emitted file's
header comment states that its pin numbers are **schematic** pin numbers,
not package pin numbers" — nothing about parity being an isomorphism on
nets appears anywhere in #461's interface contract (§7.4-7.6) or its
Completion Criteria. #523 does not declare `related: [461]` or any other
mechanism that would land this new obligation on #461's roster, and #461
was filed a day before #523 with no forward knowledge of it. As written,
AC-4 is a requirement #523 cannot itself discharge (it owns no emitter
code) and that the actual emitter owner has no record of. *Recommendation:*
either add the header-text obligation to #461's (and TASK-0089's)
Completion Criteria directly, with a cross-reference, or scope AC-4 down
to "the release documentation" only and drop the emitted-header clause
until the emitter issues are updated to carry it.

**4. AC-1's "committed fixture corpus" is undefined by this issue and is
gameable toward a narrow pass.** Nothing in #523 sets a floor on what the
corpus must contain — no minimum count, no requirement that it include a
cascaded (word-decomposed) design, a bidirectional-port design, or a
sub-circuit — despite AC-3 specifically calling out cascade-synthesized
nets as a case the check must not dodge. "Green across the committed
fixture corpus" is trivially satisfiable by a corpus of one or two
degenerate two-gate designs while genuinely riskier shapes (the exact
cases FEAT-041's cascade and FEAT-021's bidirectional ports introduce) go
untested — at which point AC-4's release-note narrowing ("names the
fixture classes the check is green for") is the only thing standing
between a thin corpus and an overbroad parity claim, and that safeguard is
manual prose, not enforced by AC-1's own test. *Recommendation:* name a
minimum corpus composition in AC-1 itself (at minimum: one cascaded
element, one bidirectional port once #339 lands, one flattened
sub-circuit), not just in the release note after the fact.

**5. The title's "gates the export" overstates what AC-1 actually
specifies.** The title frames the isomorphism as gating export, evoking
the runtime manufacturability gate #366's TASK-0091 already owns ("a
design that fails the gate does not silently emit a netlist a fab house
would reject" — #366 §4 invariant 3 / §1). But AC-1 specifies a CI
regression test ("Green across the committed fixture corpus"), not a
runtime precondition the emitter checks before writing a file for an end
user. A design outside the fixture corpus that happens to break the
isomorphism would still be exported to a real student with no refusal —
the "gate" here is a merge gate on this repository, not an export-time
gate in the shipped tool. This is a naming/expectation mismatch more than
a functional defect, but it invites a reader (or a future implementer
skimming the title) to conflate this with TASK-0091's actual runtime gate.
*Recommendation:* retitle or add one clarifying sentence distinguishing
"gates the repository's release process" from "gates a user's export."

**6. A forward citation into a closed, superseded issue's specific
criterion ID.** The boundary notes bind future readback-direction work to
"per #307's AC-3," but #307 is `state: closed, state_reason: duplicate`.
#298's own 2026-08-04 coverage-verification comment records claim 3
(readback) as `GAP-NOTED` with no owning feature and no restatement of
AC-3's specific text under a #298-native ID. A future implementer chasing
"#307's AC-3" lands on a closed duplicate with only prose, not a live
criterion, to work from. Minor, since the technical content is preserved
verbatim in #307's body regardless of its closed state, but it is a
loose thread consistent with the traceability gaps #298's own audit
comment already found elsewhere in this cluster (stale `serves_capstones`
entries still naming #307 post-closure).

## What's solid

- The provenance chain #523 tells about itself — that #307 closed as a
  duplicate of #298 and named this isomorphism check as an orphaned
  criterion — is accurate against both issues' actual text, not invented.
- AC-2's "artifact against source, never walk against walk" rule, with an
  explicit ban on comparing the emitter's in-memory state, correctly
  targets the single highest-consequence hazard #307 §3 named ("the
  single highest-consequence ordering hazard in the capstone").
- AC-3's refusal to let the isomorphism degrade to an approximate match
  when cascade-synthesized nets are involved is consistent with the
  project's own established stance (KC-05-2 in #298, cited correctly).
- Deliberately keeping G-1 (embedded-symbol falsification) and the
  readback direction out of scope, rather than re-litigating settled
  boundaries from the just-closed #307, is a clean, low-risk cut that
  avoids recreating the exact duplicate-ownership problem that got #307
  closed in the first place.
- The premise is verified against the live tree, not just against cited
  commits: no `jls.netlist`, `jls.pcb`, or isomorphism test exists in
  `src/` or `test/` today, so the "nothing owns this check yet" claim
  holds.

## Verdict rationale

`sound-with-concerns`: the check this issue proposes is the right check,
correctly diagnosed, and correctly scoped away from the readback and
embedded-symbol questions that don't belong to it. But the issue's own
bookkeeping has concrete, verifiable defects — an ordering field the
project's own DAG-walking process cannot see and that is unmirrored on
all three prerequisites, a cost figure inherited from a wider scope
without being re-priced for the narrower one it now covers, and an
acceptance criterion (AC-4) that reaches into an already-filed sibling
issue's interface contract without landing there. None of these invalidate
the design, but as filed this issue could be scheduled before its
prerequisites by any process that trusts `blocked_by`/`blocks` the way
this project's own tooling demonstrably does, and AC-4 as written cannot
be verified against the artifact it names without a coordinated edit
elsewhere.
