# Issue #683: TASK-C350-6: going multi-host is a worker-count change, not a second implementation — the same campaign produces the same bytes across hosts
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

#683 is `TASK-C350-6`, the last of six `TASK-C350-*` children of #350 (FEAT-057,
campaign execution and aggregation) — the multi-host dispatch scope. It wants a
remote worker source substituted behind the local runner's existing job
interface, reusing the runner/artifact/aggregation vocabulary unchanged, such
that a campaign run locally and across hosts produces byte-identical output,
plus failure accounting for a host that dies mid-campaign.
`ordering_after: ["TASK-C350-4", "#333 (the distributed transport this scope
reuses)"]`. TASK-C350-4 is #679, itself still open. No comments exist on #683
yet. Nothing "campaign"-shaped exists in the tree today
(`git grep -ic campaign -- src/` finds no real hit — the only text matches are
an unrelated capability-roadmap doc and a test-fixture directory name), so
this task, like its five siblings, is pure greenfield.

## Findings (most severe first)

**1. [HIGH] `ordering_after` reintroduces a dependency edge that #350's own link-phase text explicitly rejected, and #333 does not mirror it back.**
#683's front matter puts `"#333 (the distributed transport this scope
reuses)"` directly into `ordering_after`. But #350 (the parent feature) says,
verbatim, in its own Link-phase comment on the mermaid diagram: *"#333 FEAT-056
supplies the transport the multi-host dispatch scope reuses. It is `related`,
not an ordering edge: acceptance criterion 5 requires the campaign runner to
run on one machine's cores with no distributed transport at all."* #350's DAG
walk further states its own closure contains *"none"* — #333 is not reachable
from #350 through `blocks`, and #350 does not appear in #333's `blocked_by`
either (#333's `blocked_by: [318, 332, 348, 363]` — #350/#683 absent; #333's
`blocks: []`). This tracker's own stated convention (used consistently in
#350, #333, #332) is *"Every edge in `blocked_by` and `blocks` above is
written on both issues; each named issue carries the mirror."* #683 creates a
real, task-level ordering constraint on #333 that #333 itself has no idea
about — a one-directional, unmirrored dependency the parent feature went out
of its way to declare should not exist as an ordering edge.
*Recommendation:* either drop #333 from `ordering_after` (per #350's own
"related, not an ordering edge" ruling) and rewrite AC1 to not require the
actual FEAT-056 transport, or, if the dependency is genuinely load-bearing,
get #350 and #333 amended in the same edit to reflect it and add the mirror on
#333's side.

**2. [HIGH] The named prerequisite (#333) is itself blocked by four large, entirely unstarted features, so the task's 1.5-2 mw band is meaningless without disclosing the real critical path.**
#333 (FEAT-056) is `blocked_by: [318, 332, 348, 363]`. #332 (FEAT-055,
partitioned model) has a 10-16 mw band with **five** `planned_tasks`, every one
marked "not filed, no id" and independently verified absent in source at the
evidence commit. #333 itself has a 10-18 mw band with **four**
`planned_tasks`, every one marked "not filed" too. Neither #318 nor #348 was
fetched here, but #333 cites #348's "shipped half" as already satisfied
(the `Transport` seam) — the barrier-protocol and boundary-marshalling scopes
that actually constitute #333 are not. If `ordering_after: [..., "#333"]` is
taken at face value, #683 cannot start until a chain worth well over 20
maintainer-weeks of entirely unbuilt work (#332 + #333 alone, ignoring #318
and whatever #348 still owes) lands first. Presenting #683 as a `band_mw:
1.5-2` task without any note of this makes the estimate actively misleading
for anyone scheduling off it.
*Recommendation:* if the #333 dependency stands, state the real wait
explicitly in the task body (e.g. "not schedulable before #332/#333 land;
band_mw covers only the incremental work once they do") rather than presenting
a bare 1.5-2 mw figure next to an ordering edge that implies a much longer
critical path.

**3. [HIGH] "The distributed transport this scope reuses" conflates a generic, already-shipped transport with a heavyweight feature purpose-built for a different problem.**
`src/jls/collab/net/Transport.java:38` (`public interface Transport extends
Closeable`) plus `LoopbackTransport.java`, `SecureLink.java`, `Handshake.java`
already exist in the tree — the generic session-frame transport is FEAT-051
(#348), and per #333's own text that half is already satisfied. #333/FEAT-056
is not "the transport": it is a conservative **barrier-synchronization
protocol** with lookahead computation and a by-name refusal for
low-lookahead designs, built specifically so that partitions of **one**
simulation can exchange causally-ordered boundary events without ever
un-committing simulated time. Campaign dispatch, by contrast, is
embarrassingly parallel — #350 says so explicitly ("many independent runs...
jobs share no state," and separately, "this runs many whole simulations,
those [#332/#333] run one partitioned one"). Independent job dispatch to
remote workers needs a job queue / RPC pattern, not causal barrier
synchronization with a lookahead-based admission refusal. By naming #333
rather than the already-shipped `Transport` seam (#348) as its prerequisite,
#683 risks chaining a small worker-count change to the wrong, far larger
feature — or, if the intent really is to reuse #333's barrier machinery, the
task never explains why an embarrassingly-parallel dispatch problem needs a
causal-consistency protocol at all.
*Recommendation:* name the actual seam being reused. If it is `Transport`
(#348), say so and drop #333 from `ordering_after` entirely — #683 would then
only need a session-frame channel to ship job descriptions and collect
results, which is a much smaller ask than "wait for FEAT-056."

**4. [MEDIUM] AC1's byte-identity claim rests on a cross-host determinism assumption that #333 itself flags as unverified, and the cited evidence document does not exist in this tree.**
#333 §5 criterion 8 says every byte-identity claim in that feature "rests on"
an assumption stated in `docs/parity-contract.md:469-477`: *"nothing in the
tree asserts that a simulation is bit-identical across a JDK upgrade or across
operating systems... 'bit-identical' in this document means bit-identical on
one platform"* until a cross-platform experiment is run. A repo-wide search
(`grep -ril parity docs/ src/ test/`) finds no `docs/parity-contract.md`
anywhere in this checkout — the document #333 cites specific line numbers from
does not exist at HEAD. #683's own AC1 — "the same campaign run on one
machine's cores and across multiple hosts produces a byte-identical
aggregate" — is exactly the scenario that assumption governs (multiple real
hosts plausibly differ in OS/JDK build), yet #683 neither cites the
determinism risk nor restates AC1 as conditional on it, and the source
document that would let a reader check the current state of that risk cannot
be found.
*Recommendation:* either locate/restore `docs/parity-contract.md` and confirm
its cross-platform-determinism section, or have #683 state AC1 as a
single-platform guarantee until that experiment (#333 criterion 8) is actually
run, matching the honesty #333 itself demands of its own claims.

**5. [MEDIUM] AC1's verification bar ("the procedure is recorded... until a grid exists in CI") is unenforced and gameable.**
Nothing specifies what "recorded" means (a script? a transcript? a
maintainer's word?), nor what counts as a genuinely separate "host" versus
multiple processes/containers on one physical machine masquerading as
several. Since there is no CI grid to force the real scenario, this criterion
can be checked off with a documented dry run that never actually exercises
network-separated hosts — which is precisely the "test that it is not a
second implementation" the issue's own title stakes everything on. This
inherits, without tightening, the same weakness #350's own criterion 4 has
("Recorded procedure until a grid is available in CI").
*Recommendation:* require a minimum falsifiable artifact — e.g., two
containers on isolated Docker networks with no shared filesystem, or two CI
runners in different jobs — rather than accepting an unspecified "recorded
procedure" as satisfying the criterion.

**6. [MEDIUM] AC4 ("a host that dies mid-campaign produces failure rows... denominator unchanged") requires liveness/failure-detection semantics that no cited dependency owns.**
Detecting that a remote host has died — as opposed to a job merely erroring
out, which TASK-C350-5 already covers per #350's decomposition — needs some
form of heartbeat, timeout, or liveness check. #350's own "Out of scope"
section places exactly that category of mechanism ("pause distinct from stop,
heartbeat, clean interrupt") in FEAT-006 (#354), a separate, not-yet-started
feature, and says a campaign "depends on those, but they are a separate
feature." #333's checkpoint/resume scope is about a barrier-coherent
partition being suspended and later resumed — a different problem from
detecting an unresponsive worker mid-dispatch and converting its in-flight
jobs to failure rows. Neither #333 nor #350's declared task chain (TASK-C350-1
through -5) visibly owns this detection mechanism, so AC4 is a real
requirement without a supplier.
*Recommendation:* name which task/feature owns host-liveness detection before
work starts on AC4, or narrow it to "a host that reports failure" (an
error path already reachable) rather than "a host that dies" (silent,
requires positive detection).

**7. [LOW] The Outcome's framing overstates what the acceptance criteria actually require.**
The prose says *"the test that it is not a second implementation is that the
same campaign, run locally and across hosts, produces byte-identical
output"* — presented as a hard empirical test. But per finding 5, the actual
acceptance bar only requires a "recorded procedure," not an automated,
repeatable, CI-enforced comparison. The narrative claim is stronger than what
a reader can hold the implementation to.
*Recommendation:* align the Outcome language with the weaker, actually-checkable
AC1 wording, or strengthen AC1 to match the Outcome's claim.

## What's solid

- The Boundary section is a clean, well-drawn cut: "worker source only,"
  explicitly deferring the transport itself to #333 and single-simulation
  partitioning to #332 — no scope creep into either.
- AC2 ("reuses the runner's job interface and the artifact-naming and
  aggregation code paths unchanged — no parallel implementation... exists")
  is genuinely checkable by code review/diff and directly targets the
  real failure mode the issue title names.
- AC3 ("a campaign still runs with no distributed transport in the path
  after this lands") is a good regression guard, consistent with and
  correctly citing #350 invariant 3, and mechanically testable.
- Sequencing this task last, after the local runner/collection/aggregation
  tasks, is the right call and matches #350 §2's own stated rationale
  ("multi-host dispatch is last because it must not be a second
  implementation").

## Verdict rationale

The scope boundary and two of the four acceptance criteria are sound and
checkable. But the task's central dependency claim — `ordering_after` naming
#333 as "the distributed transport this scope reuses" — directly contradicts
the parent feature's own recorded decision that #333 is "related, not an
ordering edge," is unmirrored on #333's side, likely names the wrong
prerequisite (the already-shipped generic `Transport` seam versus the
unbuilt, purpose-mismatched barrier protocol), and — if taken at face value —
hides a critical path of 20+ unbuilt maintainer-weeks behind a 1.5-2 mw
label. Layered on top, the headline byte-identity criterion rests on a
cross-host determinism assumption the project's own cited evidence document
cannot even be found in this tree, and the host-death criterion requires a
detection mechanism nothing in the cited chain supplies. These are
resolvable with edits to the dependency declaration and the acceptance
criteria, not a sign the underlying idea is wrong — hence needs-rework, not
should-not-proceed.
