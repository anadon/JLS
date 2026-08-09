# Issue #349: FEAT-040: JLS knows what a real part is — pinouts, sections, footprints and loading are queryable data, extensible with a text file and no Java
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Context

#349 is a coordination/planning issue ("feature") in a heavily formalized
DAG of FEAT-/TASK-/CAP- issues, generated in the repo's ongoing "capstone
plan" process. At time of review it has already spawned two child task
issues (#400 = TASK-0085, schema+binding; #450 = TASK-0055, data
transcription), per its own comments. Its job product *is* the
bookkeeping — dependency edges, integration criteria, cost basis,
provenance discipline — so internal consistency of that bookkeeping is
the correctness bar for this issue specifically, not just the eventual
code. Several such inconsistencies were found (below), which is why the
verdict is needs-rework rather than sound-with-concerns despite the
technical schema design being largely solid.

## Findings, most severe first

**1. (High) IC-1 as written cannot be honestly verified without either a cycle or an unstated stub.**
Quote: *"IC-1 — a new part is visible without recompilation. Do: add a
part number to a text file on the classpath; without rebuilding, query
it from the packing pass, from an emitter, and from the loading check.
Observe: all three see it... Built by this issue's close-out."* But
"the packing pass" and "an emitter" are #365 (FEAT-041) and #366
(FEAT-042) — and the machine block records `blocks: [323, 365, 366]`,
i.e. #365/#366 are *blocked by* #349, not the other way round. #349
cannot close after verifying IC-1 against the real packing pass and a
real emitter, because those don't exist until after #349 closes; doing
so would recreate the cycle the Link-phase DAG walk explicitly claims
does not exist ("#349 is not in that closure ... the combined graph ...
stays a DAG"). The issue never says IC-1 will instead be checked against
throwaway test-double consumers built solely for this criterion — if
that's the plan, it needs to be named, because a stub "packing pass"
proves nothing about the real one; if it isn't the plan, IC-1 is
infeasible on the stated schedule.
*Recommendation:* state explicitly that IC-1 is exercised via minimal
harness consumers owned by this issue, and add a second, separate
verification obligation on #365/#366 ("re-confirm IC-1 against the
production pass") so the criterion isn't quietly satisfied by a fake
that can drift from the real integration.

**2. (High) The Open-Question filing gates this issue declares are already violated, and the violation is hand-waved rather than corrected.**
§ Open Questions states, verbatim: O1 *"**Blocks filing TASK-0085**, since
the schema's shape depends on it"* and O5 *"**Blocks filing TASK-0085.**"*
Comment 3 (2026-08-08, same issue) says: *"#400 filed against this
feature's Open Questions 1 and 5 rather than waiting on them, on the
ground that withholding an issue pending a decision the issue exists to
frame is circular (D10)."* #400 (TASK-0085) exists and is open as of the
first comment (2026-08-04), before any comment records O1 or O5 as
ratified. "D10" is asserted with no definition or link anywhere in the
fetched issue or comments. The Completion Criteria checklist still reads
*"Open Questions 1, 2, 4 and 5 answered on this issue before the
corresponding child is filed"* — a box that is already false, since the
child was filed first and the questions answered (if at all) after.
*Recommendation:* either strike the "Blocks filing" language since it is
evidently advisory in practice, or actually enforce it. Leaving a
written rule that the same issue's own history contradicts, defended by
an uncited abbreviation, undermines the credibility of every other
"Blocks" annotation in this issue (O4 also says "Blocks filing
TASK-0055").

**3. (High) The machine block, roster and mermaid graph are stale against the issue's own comments — violating a Definition-of-Done item that is supposed to hold continuously, not just at close.**
The issue body's `planned_tasks` list, § 2 roster table, and mermaid
graph all still label TASK-0085 and TASK-0055 as *"(unfiled)"* / *"planned
— scope absent at 2d0ca9d"*. But comments dated 2026-08-04 and
2026-08-08 establish both were filed as #400 and #450 and both remain
open. Completion Criteria explicitly requires: *"Machine block, roster
table, and mermaid graph agree with reality at close."* They do not
agree with reality now. For an issue whose entire value proposition is
that the DAG can be walked and trusted (§ "Link phase... every edge...
is written on both issues"), a body that still says "unfiled" for tasks
that have PRs/issues open against them for four days is exactly the kind
of drift the process is supposed to prevent.
*Recommendation:* edit the body to reference #400/#450 directly, or
explicitly mark the machine block as a frozen point-in-time snapshot if
that's intentional — right now it silently looks current.

**4. (Medium) The cost basis cites a document and two commits that are not resolvable in this checkout.**
The Cost section anchors the "4–8 maintainer-week" band and the
"S21 6–10 → 4–8" reduction entirely to `docs/plan/evidence/capstone-plan.md`
at commit `3a81a4a7d6a0f108ec201e632732d308cc02b3fc`, and pins the "ABSENT
at 2d0ca9d" evidence to `2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`.
Neither commit resolves (`git cat-file -t <hash>` fails for both), and
`docs/plan/evidence/capstone-plan.md` does not exist anywhere — there is
no `docs/plan/` directory at all in this tree, and `git log --all -- docs/plan/evidence/capstone-plan.md`
returns nothing. (Caveat: this is a shallow clone, 268 commits back to
2026-07-16, so a commit older than the horizon can't be conclusively
ruled out — but a *tracked path* would still show in a shallow clone's
current tree if it existed at HEAD or any reachable commit, and it does
not.) The entire quantitative justification for this issue's cost is
therefore unverifiable from inside the repository a contributor would
actually check out.
*Recommendation:* either the evidence doc needs to be committed
somewhere a reviewer can reach, or the citation needs to point at
something that resolves in a normal (even shallow) clone.

**5. (Medium) Licensing analysis (Open Question 4) asserts source licenses without citation and skips a real GPL compatibility subtlety.**
The issue asserts *"Two GPL-3.0 Java simulators carry exactly this
data: Logisim-Evolution's `std/ttl/`... and hneemann's Digital... D8
permits absorbing GPL-compatible code into this GPL-3.0-or-later
project"* with no link to either project's actual license file or a
commit establishing the claim. More substantively: combining
GPL-3.0-*only* (no "or later") licensed data into a GPL-3.0-*or-later*
project constrains the combined/derived portion to GPL-3.0 terms — the
project's own "or later" election doesn't retroactively cover absorbed
GPL-3.0-only content. The issue's licensing discussion (and both options
under O4) never distinguishes "GPL-3.0" from "GPL-3.0-only" vs.
"GPL-3.0-or-later" for the two sources, despite the issue's own stated
ethos being that *"the library survives an audit rather than being an
unattributable pile."*
*Recommendation:* before TASK-0055 starts absorbing, get and cite the
exact license variant (with a permalink) for each of the two sources,
and note the or-later interaction explicitly rather than treating
"GPL-3.0" as a single monolithic bucket.

**6. (Medium) The KiCad footprint-gate claim underlying Open Question 5's "recommended default" is an unsourced external technical claim.**
*"`pcbnew` discards any netlist component with an empty footprint field
(`board_netlist_updater.cpp:151-160`, KiCad ref 10.0)"* is stated as
established fact and used as the leverage argument for shipping the
footprint column from entry one — but there is no URL/permalink, and it
cites line numbers in a file outside this repository that cannot be
checked from here. Given the issue explicitly asks that absorbed *parts*
data survive an audit, its own load-bearing external claims should meet
the same bar.
*Recommendation:* link the actual KiCad source permalink (tag/commit) so
this specific figure and behavior claim is checkable, not just asserted.

**7. (Low) Criterion 3's provenance check is gameable as literally stated.**
*"A test asserts no entry has an empty provenance field."* Non-emptiness
is a weak bar — an entry with `provenance: "?"` or `"unknown"` passes.
IC-2 additionally requires the license notice to "resolve," which
narrows this some, but the provenance string itself has no format or
enum constraint.
*Recommendation:* constrain provenance to a small closed vocabulary
(named absorbed source, or a datasheet identifier pattern) rather than
"non-empty."

**8. (Solid) Scope boundary against #365 (packing/BOM/wiring) and #366 (emitters) is clear and consistently drawn.**
§ 1's "out of scope, with owner named" list and comment 1's boundary
note against #365 agree with each other and with what's on disk
(`PinBindings.java`'s aggregation idiom is real and matches the citation
at `src/jls/hdl/board/PinBindings.java:38-83`, verified above). No
overlap risk.

**9. (Solid) The shared-pin / disjointness relaxation in § 3 is a genuine, well-anticipated hardware modeling problem** (dual multiplexers/decoders with a common select line), correctly flagged as blocking the schema's shape rather than left implicit.

**10. (Solid) IC-4's "total refusal, never the parseable prefix" rule matches existing project precedent** — `src/jls/Circuit.java:092-108,716-754` already implements exactly this pattern for the `.jls` FORMAT header (version too new → refused, not partially read), so the proposed mechanism is not inventing a new idiom.

## Bottom line

The schema design instincts (shared pins, inert electrical columns, no
geometry, versioned total refusal, aggregated malformed-entry reporting)
are sound and reuse real precedent in the codebase. But this issue's job
is to be a trustworthy coordination artifact, and by its own stated
standards (DAG consistency, "machine block agrees with reality," gates
that are supposed to block filing) it currently does not meet that bar:
a verification criterion (IC-1) that can't be honestly satisfied without
either a cycle or an unstated stub, a filing gate that was overridden
without formally amending the rule, a stale machine block/roster/graph,
and load-bearing evidence citations (cost basis, external license
claims, external KiCad behavior) that don't resolve from inside this
repository. Recommend a housekeeping pass on #349 itself before more
children are spawned off it, since drift here propagates to every issue
that mirrors these edges.
