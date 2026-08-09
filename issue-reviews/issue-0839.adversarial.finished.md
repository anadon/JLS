# Issue #839: TASK-C333-6: a partitioned run checkpoints at a barrier and resumes to a byte-identical continuation, so a cluster run can be suspended and rescheduled
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The task's scoping instinct is good — it explicitly declines to re-litigate FEAT-035's
serialization mechanism and confines itself to the coherence argument ("a checkpoint is
only coherent at a barrier"). But as written it omits its single hardest prerequisite
(partitioning doesn't exist in the codebase at all), its byte-identical acceptance
criteria silently inherit an unresolved cross-platform-determinism gap from its parent
feature, and its cost estimate is unreconciled against the pattern already visible in its
own sibling tasks. None of this is fatal to the underlying design, but the issue is not
safe to hand to an implementer as written.

## Findings, most severe first

### 1. The task's own AC-2 requires an artifact (a "count-4 run") that cannot exist yet, and the issue never says so

AC-2: "a mid-flight checkpoint of a count-4 run, resumed, produces output byte-identical
to the uninterrupted run over at least one committed fixture." A "count-4 run" is a
design split into 4 partitions running as a distributed simulation. I verified directly
against this checkout that nothing partition-shaped exists anywhere:

```
$ git grep -rliE 'PartitionSet|BoundaryDescription|streamingElaborat' -- src/ test/
(no output)
```

This matches #332's (FEAT-055) own citation of the same command at its evidence commit.
FEAT-055 — "a circuit exists as parts that load independently" — is explicitly listed as
a hard blocker of #333 (FEAT-056, this task's parent feature), which states plainly: "**there
is nothing to synchronize between until the design exists as parts with named
boundaries**." FEAT-055 itself carries zero landed scopes ("ABSENT at 2d0ca9d" against
every one of its five planned scopes). Yet #839's `ordering_after` list names only
`TASK-C333-3` and `#363` — FEAT-055 (#332) appears nowhere. An implementer picking up
#839 today has no design to partition, no partitioner, and no boundary-event transport to
run four partitions across barriers with. AC-2 is currently untestable by construction,
and the issue gives no indication it knows this.

**Recommendation:** add `#332` (FEAT-055) and the marshalling/protocol scopes it
transitively requires to `ordering_after`, or state explicitly that this task cannot be
started until a partitioned run exists to checkpoint at all.

### 2. AC-2/AC-3's byte-identical claims inherit an unresolved cross-platform-determinism gap, and the controlling document doesn't exist in this repo

#333 states that every byte-identity claim for a distributed/partitioned run "rests on"
an assumption tracked in `docs/parity-contract.md:469-477` — "nothing in the tree asserts
that a simulation is bit-identical across a JDK upgrade or across operating systems." I
searched this checkout directly:

```
$ find /home/user/JLS -iname "*parity-contract*"
(no output)
$ ls /home/user/JLS/docs/
architecture-project-setup.md batch-interface.md ... simulation-semantics.md
standards-adoption standards-landscape.md vcd-interop.md ...
```

The file does not exist anywhere in the tree, in any case variant — a finding already
recorded independently against #333 itself (`issue-reviews/issue-0333.adversarial.md`
finding #1). A "cluster run" (the issue's own framing, and #312's deployment context) is
precisely the case most likely to span heterogeneous hosts — different JDK builds, or
even different OSes on a shared cluster. AC-2 and AC-3 both assert byte-identical output
across a checkpoint/resume of such a run with no caveat about platform, and no reference
to the (missing) cross-platform experiment #333 flags as a precondition of its own
byte-identity criteria 1, 2 and 5 — one of which (5) is this task's own scope
("checkpoint a partitioned run mid-flight, resume it, and compare against the
uninterrupted run"). AC-2 as worded is either quietly assuming single-platform clusters
(unstated) or is unverifiable until that experiment exists.

**Recommendation:** either restate AC-2/AC-3 as explicitly single-platform (matching
whatever restatement #333's own criteria 1/2/5 eventually get), or make the cross-platform
determinism experiment (#333's Open Question 3 / #830 per the sibling review) an explicit
prerequisite here too.

### 3. `ordering_after` cites `TASK-C333-3` with no issue number, inconsistent with how `#363` is cited in the same block

The machine block cites two prerequisites in one list: `"TASK-C333-3 (a checkpoint is
only coherent at a barrier)"` and `"#363 FEAT-035 checkpoint mechanism (#426 write-back,
#456 resume gate)"`. The second entry is fully resolved to filed issue numbers; the first
is not — a reader of #839 alone cannot tell whether `TASK-C333-3` is a filed issue, a
planned-but-unfiled scope (as #333's body describes all four of its own children, at the
time #333 was last edited), or an internal label with no tracker entry at all. Checking
the sibling review of #333 confirms it does resolve to a real, currently-open issue
(#834), so the underlying dependency is legitimate — but that resolution is not
discoverable from #839's own text, and #333's body (last edited before these children
were filed) still shows all four of its scopes as "not filed." An implementer trusting
only #839 and #333, without independently locating #834, cannot verify this prerequisite
exists at all.

**Recommendation:** cite the resolved issue number (`#834`) alongside `TASK-C333-3` in
the machine block, matching the citation style already used for `#363`.

### 4. Cost estimate is unreconciled against the pattern already visible among its own siblings

`band_mw: "3-5"` is stated with no supporting arithmetic. For context: TASK-0074 (#426),
the *unpartitioned* write-back task this task explicitly builds on top of ("the
serialization is #363's mechanism... consumed as-is"), is itself costed at 2 weeks and
covers only a single-process checkpoint. This task adds barrier-coherence reasoning across
N processes on separate hosts, a refusal path for partition-count mismatches (AC-3), and
integration with a barrier protocol that does not exist yet — a strictly harder problem —
for a comparable or only slightly larger estimate. The independent review of #333 found
that its six filed children (including this one) already sum to 16-25 mw against #333's
own stated 10-18 mw band, over budget at the high end before any task has landed. #839's
"3-5" is one line of that already-overrun sum and carries no basis of its own here.

**Recommendation:** show the arithmetic (what specifically the 3-5 mw covers, given the
barrier-protocol dependency), or flag the same band-vs-sum reconciliation #333's own
Completion Criteria already requires.

### 5. AC-1's "refused by name or deferred... explicitly" leaves the actual required behavior unspecified

AC-1: "a checkpoint may only be taken at a barrier; a request mid-interval is refused by
name or deferred to the next barrier explicitly, never taken at an inconsistent cut." The
two permitted responses (refuse vs. defer) are presented as an open choice with no
criterion for which applies when, and no requirement that the choice be consistent or
documented per call site. An implementation that always silently defers (never refuses)
and one that always refuses (never defers) both satisfy the letter of AC-1 while
producing very different operator experiences — a defer-only implementation could hang
indefinitely awaiting a barrier that a stalled partition never reaches (a real
possibility given #333's own conservative-discipline stall risk, which #333 names as a
"will be hit" failure mode). Nothing in #839 requires a bound on the deferral, or ties the
choice back to #333's own by-name refusal discipline for degenerate designs.

**Recommendation:** either pick one behavior, or specify the criterion by which an
implementation chooses (e.g., always defer, but bound the wait and refuse-by-name past a
deadline), so the acceptance test cannot pass on a defer-forever implementation.

### 6. AC-3's "no partition count... a resumed run could observe" is checked how? Unspecified, and the closest known precedent is gameable

AC-3 requires that the checkpoint "carries no partition count or identifier a resumed run
could observe." This directly mirrors #333's own invariant 4 (good instinct — see "solid"
below) — but the independent review of #333's sibling invariance-suite task (#838) already
found that a structural absence check (grep the artifact format for an explicit field)
does not catch partition-count-correlated side channels such as timing jitter in event
ordering or incidental byte-size differences. #839 gives no indication of how AC-3 will be
verified, and if it reuses the same structural-check pattern it inherits the same
weakness for the checkpoint artifact specifically.

**Recommendation:** state the verification method for AC-3 explicitly, and consider
requiring the same checkpoint bytes (not just the same simulation output) across two
different partition counts of the same design at the same barrier — a stronger, more
falsifiable claim than "the reader found no count field."

## What's solid (one line each)

- Framing the coherence argument ("a checkpoint is only coherent at a barrier") as this
  task's sole contribution, with serialization explicitly out of scope, is a clean and
  minimal scope boundary that avoids re-litigating FEAT-035.
- AC-4's requirement that any new field ride FEAT-035's schema/gate rather than a private
  side file is well-motivated and consistent with #363/#426's stated design (a private
  side file would bypass TASK-0075's round-trip gate entirely).
- AC-3's "byte-identical or refused by name — never silently different" for a
  partition-count change correctly generalizes #333's invariant 4 into the checkpoint
  case; the underlying design judgment is right even though its verification is
  underspecified (finding #6).
- AC-5's regression guard (whole-design checkpoint behavior unchanged) is the correct
  cheap check against this task quietly breaking the non-partitioned case.
- The `part_of_feature: 333` / `ordering_after` linkage to `#363` (checkpoint mechanism)
  is accurately described against #426 and #456's actual content, verified directly.

## Verdict rationale

The design judgment inside this issue's narrow scope is sound, but the issue is not
currently actionable: its acceptance criteria describe testing something (a partitioned
run) that has no path to existing yet and that the issue never flags as a blocking
absence, its byte-identical claims quietly inherit a cross-platform-determinism gap whose
controlling document doesn't exist in this repository, and its cost and dependency
citations don't hold up against what its own siblings and parent feature already say.
`needs-rework`: fix the missing FEAT-055 dependency and the parity-contract gap before
this is safe to schedule, even though the core coherence argument itself is not in
question.
