# Issue #687: TASK-C524-2: breaking the CLI contract becomes a versioned event — a semver-plus-deprecation-window ratchet, and a seeded violation fails CI before any adapter test runs
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#687 is the second of three tasks under FEAT-C21-1 (#524): TASK-C524-1
(#686) writes the CLI contract and its conformance test, TASK-C524-2
(this issue) wraps it in a semver-plus-deprecation-window ratchet, and
TASK-C524-3 (#690) makes the version queryable. The boundary against
#686 is clean and the `ordering_after: ["TASK-C524-1"]` edge is correct
— you cannot ratchet a contract that does not exist yet. The problems
are (1) a cross-reference that points at the wrong issue for the
sibling apparatus this task's headline claim depends on, and (2) an
AC-2 whose literal wording is satisfiable by a one-time demonstration
against zero adapters, when both the issue's own title and this repo's
established "ratchet" convention imply a permanent, structurally
enforced check.

## Findings, most severe first

### 1. The boundary note cites the wrong issue for the ordering apparatus it hands off to

The Boundary section reads: *"The cross-adapter fixture that hosts the
conformance ordering in CI is #531 (TASK-C531-4)."* I fetched both.
**#531** is `FEAT-C21-6`, the *feature*-tier parent (`serves_capstones:
[502]`, `band_mw: 3-4`) — its body contains no mention of "TASK-C531-4"
anywhere, and its own acceptance criteria are written at the fixture
level (`CrossPlatformScoreParityTest`, containerization, determinism),
not the specific seeded-violation-ordering task. **TASK-C531-4 is a
different, already-filed issue: #724** (created 2026-08-04T15:23:25Z,
three minutes after this issue), titled *"a seeded CLI-contract
violation fails the build before any adapter test runs, and two full
corpus runs are byte-identical end to end"* — the exact apparatus
#687's own AC-2 half-restates. #724's body explicitly says *"the
ratchet mechanics themselves are TASK-C524-2"*, i.e. it already points
back at #687 correctly; #687 does not return the favor. This reads as
a forward reference written before TASK-C531-4 had a number (the same
provisional-id pattern seen in #524's `feat_id: FEAT-C21-1 #
provisional id`) that was never repaired once #724 was filed — the
same class of defect a sibling review (`issue-0524.adversarial.md`
finding 1) found live in #524's `ordering_after`. A reader who follows
"#531" looking for the ordering fixture lands on a feature-level issue
with no such content and has to hunt for #724 themselves. **Recommend:
edit the Boundary line to read `#724 (TASK-C531-4)`.**

### 2. AC-2 is satisfiable against an empty set of adapters, and nothing in #687 requires it to be re-verified once adapters exist

AC-2: *"A seeded contract violation on a scratch branch (e.g. a changed
exit code) fails CI, and the failure occurs before any adapter test
runs; the falsification transcript is recorded (CAP-21 AC-2)."*
Verified against the tree: `grep -rli "gradescope\|prairielearn\|nbgrader"` over
the whole repository returns nothing — CAP-21's own parent issue (#502)
says the same thing in its Background section ("No platform name
appears anywhere in the tree"). So at the point #687 is realistically
implemented, "before any adapter test runs" is vacuously true: there
are no adapter tests, of any kind, anywhere in this repository, and
#687's own `ordering_after` names only `["TASK-C524-1"]` (#686) — none
of the TASK-C531-* tasks (#717/#719/#721/#724) that actually build the
four adapter lanes. An implementer can close #687 by seeding one
violation on a scratch branch, observing the lone `build` CI job fail,
and writing that up as "the transcript" — a real demonstration of
*something*, but not a demonstration that the CLI-contract check runs
*before* anything, since nothing else exists to be before. Contrast
#724's own AC for the identical clause: *"The lane ordering is enforced
structurally — an adapter lane cannot be scheduled ahead of the
conformance gate by reordering a workflow file."* That is the
falsifiable, permanent version of the claim; #687's copy of the same
sentence has no such teeth. **Recommend:** either drop the "before any
adapter test runs" clause from #687's AC-2 entirely (since #724 already
owns it, correctly, at fixture scale) and leave #687 with the plain
scratch-branch falsification proof, or make explicit that AC-2's
"before any adapter test" claim is provisional here and gets its
binding, structural version at #724.

### 3. "Ratchet" is the title's promise; AC-2's text describes a one-time demo, not a persisting check

Every other `*RatchetTest.java` in this tree — `HeadlessCoreRatchetTest`,
`NotificationRatchetTest`, `NullMarkedRatchetTest`,
`PointerApiRatchetTest`, `CollabSecurityRatchetTest`,
`SocketConfinementRatchetTest`, `PackageInfoRatchetTest`,
`DialogCoverageRatchetTest` (all under `test/jls/`, confirmed via
`grep -rl Ratchet`) — is an in-tree JUnit class that runs on every
`mvn verify` and keeps catching the regression indefinitely, the same
pattern `scripts/wayland-rig-selftest.sh` uses for CI *logic* (a stub
harness that stays in the repo and is re-run on every event, not a
one-time manual proof). #687's AC-2, by contrast, only asks for "a
seeded contract violation on a scratch branch... fails CI... the
falsification transcript is recorded" — phrasing that is fully
satisfied by a single manual push-and-observe cycle whose evidence is
a pasted transcript in a PR description. Nothing in the four checkboxes
requires a standing, repository-resident test (or a CI structural
constraint, e.g. a `needs:` job dependency that a later contributor
cannot quietly remove) that keeps re-proving the property. As worded, a
maintainer could satisfy AC-2 once, merge, and have the ordering
guarantee silently defeated six months later by an innocuous workflow
YAML edit — exactly the failure mode #724's "enforced structurally...
by reordering a workflow file" clause exists to rule out, on the
sibling issue, not this one. **Recommend:** add a checkbox naming the
concrete, permanent artifact (e.g. a `CliContractRatchetTest` or a
`needs:` dependency asserted by a lint/selftest script), matching this
repo's own convention rather than leaving the check as a described,
one-time event.

### 4. AC-3's "refuses... without the corresponding version bump" is not reconciled against the existing compatibility-flag escape hatch

`docs/batch-interface.md` §6 (Stability promise, verified at lines
324-336) already states the versioning discipline for the existing
`-t`/stdout/VCD surfaces: a breaking change requires *"a major version
bump, **or** a compatibility flag that keeps the format specified here
available unchanged."* AC-3 of #687 says the new ratchet *"refuses to
let a breaking change land without the corresponding version bump"* —
no compatibility-flag alternative is mentioned. If the new CLI-wide
ratchet is meant to generalize §6 (a reasonable reading, since the CLI
contract being frozen here is the same batch interface plus invocation
and artifact paths), then AC-3 as worded is a strictly *stricter* rule
than the one already in force, and the issue never says whether that
tightening is deliberate. If it is not deliberate, the ratchet as
built could reject a change that ships behind a compatibility flag
under the existing, still-live §6 promise — a real conflict between
two written policies governing overlapping surface, not a hypothetical
one. **Recommend:** the written versioning policy (AC-1) state
explicitly whether it supersedes, extends, or is a strict subset of
`docs/batch-interface.md` §6, and if the compatibility-flag route is
being dropped project-wide, say so and update §6 in the same change
rather than leaving two documents making different promises about the
same interface.

### 5. AC-4's "admits a concrete planned verdict evolution from #369/#466" rests on upstream design that has not settled

AC-4: *"The policy is shown to admit at least one concrete planned
verdict evolution from #369/#466 without a major bump."* Status 3 —
the concrete evolution both #369 and #466 propose — **does not exist
anywhere in the tree today** (`docs/batch-interface.md:36-40`
documents exactly three exit statuses; #466's own Observations section
O1 reproduces `unknown option -check` at the evidence commit). #466 is
itself open and unimplemented, and a companion review in this same
fleet (`issue-0524.adversarial.md` finding 4) already found #466's own
Definition of Done self-contradictory across its body and a revision
comment. Demonstrating that a versioning policy "admits" a concrete
evolution that is still an open design question is either (a) done
against a hypothetical (status 3's eventual shape), which is
unfalsifiable as an acceptance check, or (b) requires #466 to actually
land first, which #687's own `ordering_after: ["TASK-C524-1"]` does not
require — an implementer following #687's machine block alone could
start (and finish) this task before #466 exists in any form.
**Recommend:** either add #466 to `ordering_after` (band and sequencing
both already assume CAP-21 sits behind CAP-06's verdict machinery per
#524's own `ordering_after: [369, 466]`), or reword AC-4 to accept a
synthetic/hypothetical evolution scenario explicitly, so the check does
not quietly wait on a currently-contradictory sibling issue.

## What's solid

- **The task-to-parent boundary is clean.** "Mechanism and policy
  only; the contract's content is TASK-C524-1" matches #686's own
  framing exactly, and the `ordering_after: ["TASK-C524-1"]` edge is
  the correct one — a ratchet needs a contract to ratchet.
- **AC-2's core wording is an accurate quote of CAP-21 AC-2** as
  filed on #502 ("the conformance suite passes at HEAD, and a seeded
  contract violation (changed exit code) fails CI before adapter
  tests execute... falsification requirement: the seeded-violation
  run's transcript recorded") — not a garbled restatement.
- **Cost accounting is consistent one level up.** #687's `band_mw:
  0.5-1` sums with #686's `1-1.5` and #690's `0.5` to exactly #524's
  own `band_mw: 2-3`, as already verified in the #524 review.
- **The additive-vs-breaking distinction (AC-3's first clause)** is a
  reasonable, checkable design goal on its own terms, independent of
  the version-bump-only wording problem in finding 4.

## Verdict rationale

The core idea — a written policy plus a CI-enforced falsification test
that proves the ratchet actually bites — is sound and appropriately
scoped against its siblings. But the issue as filed sends a reader to
the wrong issue number for the apparatus its headline claim depends on
(finding 1), and its central acceptance criterion is worded so that a
single scratch-branch demonstration against zero real adapters
discharges a promise the title states as a standing, permanent ratchet
(findings 2-3) — a gap the project's own sibling task (#724) already
closes correctly, just not by reference from here. These are body-text
and AC-wording fixes, not a redesign, but an implementer working from
#687 alone today would produce something weaker than what the issue's
own title promises. **needs-rework.**
