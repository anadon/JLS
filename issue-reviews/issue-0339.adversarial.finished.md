# Issue #339: FEAT-021: JLS can declare a bidirectional port — the third direction exists in the IR, every emitter renders it or refuses it by name, and a bidirectional pin reads the net
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

The technical core is unusually well grounded: every code citation in the
issue body and its three comments checks out against HEAD
(`src/jls/hdl/HdlModel.java:27-33`, `src/jls/hdl/scan/ScannedPort.java:12-20`,
`src/jls/hdl/imp/NetlistImporter.java:184-189`, the four direction ternaries
in `VerilogEmitter.java`, `VhdlEmitter.java` and `PcfEmitter.java`, the sealed
`Pin` class at `src/jls/elem/Pin.java:18-20`, and the tri-state resolution
block at `src/jls/elem/WireNet.java:440-485` matching
`docs/simulation-semantics.md` §9). The "no `default` arm" totality mechanism
is a genuinely strong, hard-to-game acceptance device. That said, the issue
carries real process-hygiene defects and a couple of underspecified,
gameable criteria that a skeptical reviewer should not wave through.

## Findings, most severe first

**1. (High) Cross-issue ordering contradiction with #341, unresolved as of the latest edit on either issue.**
#339's machine block declares `blocked_by: []` and its latest comment
(2026-08-08T16:38) resolves Open Question 1 in favor of shipping "over
HEAD's two-state semantics" immediately, not waiting on the strength
lattice. But #341 (FEAT-027), last touched 2026-08-08T18:26 — *after*
#339's absorption comment — still carries TASK-0049 in its own
`planned_tasks` roster and its mermaid graph explicitly sequences
`T58[TASK-0058: lattice] --> T49[TASK-0049]`, i.e. it models the
bidirectional-port task as gated behind the strength lattice. #341's own
Open Question 1 ("Which feature owns each shared task?") is still open and
recommends #339 as owner but has not been acted on. Both issues claim
"ordering edges written with both mirrors" (the 2026-08-02 Link Pass), yet
the mirror is stale on #341's side even after a later touch. A scheduler
reading #341 alone would believe TASK-0049 cannot start before the 6-9 mw
lattice lands; a scheduler reading #339 alone would believe it can start
today. Recommendation: post a `REPLAN:`/mirror-fix comment on #341 removing
TASK-0049 from its roster and mermaid before treating either issue's DAG as
authoritative.

**2. (High) The issue body's own Definition-of-Done is currently false, and the body was never edited to match.**
The Completion Criteria require `planned_tasks` to resolve to "a filed
issue or descoped," but the machine block still literally reads
`planned_tasks: - "TASK-0049 ..."` with no filed-issue reference. The three
comments establish that TASK-0049 *was* filed (as #474), then closed
2026-08-08 as a duplicate absorbed into #339 itself — so the feature "now
has no filed child," per the issue's own stage-3 verification comment. That
fact is buried in comment prose and never reflected in the body's YAML or
roster table. Anyone triaging by issue body alone (a common workflow, and
the one the "Machine block, roster table, and mermaid graph agree with
reality at close" DoD line exists to guard against) will misread this
issue's state. Recommendation: edit the body's roster/machine block now,
not only at close-out, or add a top-of-issue pointer to the absorbing
comments.

**3. (Medium) Two of four §5 integration criteria are effectively unfalsifiable as written.**
Criterion 2 ("Refusal totality census... Recorded as a table in the closing
comment, produced by a command, not by inspection") never names the
command, script, or tool that produces the census — there is no committed
artifact today, so "produced by a command" is unverifiable until someone
invents that command at close-out, and a closer could satisfy the letter of
this criterion with any ad hoc one-off script. Criterion 4 (board path
carries the direction) is explicitly "a Recorded manual procedure until
#264 lands a harness" — i.e. self-attested, no CI gate, no reproducible
artifact required by the DoD beyond a written claim. Both are real gaps
between "criterion sounds rigorous" and "criterion is actually checkable by
someone other than the closer." Recommendation: name the census-generation
mechanism (e.g. a small test that reflectively walks `HdlModel.Direction`
consumers) before close, and require a screenshot/log artifact for the
manual board-path check rather than prose alone.

**4. (Medium) The "Six external formats need this" headline claim is unenumerated at filing time — the issue admits it.**
The Impact section leads with "Six external formats need this to be
expressible at all," used to justify urgency and audience breadth, but
Open Question 2 of the same issue says plainly: "Which formats express
bidirectional in their constraint files, and which must refuse? This must
be enumerated before criterion 4 is checkable." A specific, memorable
number is doing rhetorical work in the abstract before the underlying list
exists. This isn't fatal but it is exactly the kind of number that should
not ship in a headline before it is backed by the table Open Question 2
itself requires.

**5. (Medium) The priced cost band excludes work the issue itself says no child covers.**
The Cost section prices "Sum of this feature's own task rows: 2 wk
(TASK-0049 at 2 wk)" and treats the band's remaining 0-2 wk as "headroom"
for Open Question 1's risk. But §5 states outright that "TASK-0049's own
deliverable list does not assert" any of the four integration criteria
(the round-trip test, the census, the contention fixture corpus, the board
path) — this issue's own close-out work, which by Rule B must exist
precisely because no child covers it. That close-out work has no cost line
of its own anywhere in the issue. The 2-4 mw band may be adequate in
practice, but as written it prices only the (now-absorbed) implementation
task and silently assumes the integration criteria are free.

**6. (Low) The essential implementation obstacle is absent from the issue body and exists only in comments.**
`Pin`'s sealed `permits InputPin, OutputPin` clause (`src/jls/elem/Pin.java:18-20`)
is, by the absorbing comment's own description, "the trap that bites
first... a reviewer who does not know the class is sealed will read that
error as unrelated." The original issue body's §2/§3 never mentions the
sealed hierarchy at all — it was discovered and recorded only in the later
#474-absorption comments. A contributor who reads the issue body top to
bottom (reasonably, since it is the nominal spec) and starts from
`HdlModel.Direction` will hit a confusing, unexplained compile error before
ever learning the real first step is widening `permits`.

**7. (Low) Minor internal inconsistency in the DAG self-audit.**
The "Forward from every `blocks` entry" closure text names #342 (FEAT-022)
as part of the downstream closure, but #342 appears in neither the
`blocks:` list (`[320, 328, 360]`) nor the mermaid diagram directly below
it. This may be a legitimate transitive-closure result (via #320's own
`blocks`), but it is not verifiable from this issue in isolation and the
issue's mermaid diagram — meant to be the visual mirror of the same
graph — silently omits it. Low stakes, but it is exactly the kind of
"machine block, roster table, and mermaid graph agree with reality"
mismatch the issue's own DoD explicitly polices elsewhere.

## What's solid (no rework needed)

- Every code citation checked (HdlModel, ScannedPort, YosysNetlist,
  NetlistImporter, the four emitter ternaries, Pin's sealed clause,
  WireNet's tri-state resolution, docs/simulation-semantics.md §9) is
  accurate at HEAD.
- The no-`default`-arm totality mechanism is a strong, compiler-enforced
  acceptance criterion that resists silent regression — a rare example of
  a hard-to-game test in this corpus.
- The explicit scope fence against strength/drive-contention work (owned by
  #341/#322, not this issue) is sound and internally consistent within
  #339 itself, and the §7 pre-authorized re-plan for Open Question 1 shows
  real self-awareness about the biggest technical risk.
- No new dependency, licensing, or security surface is introduced; nothing
  to flag on that axis.
