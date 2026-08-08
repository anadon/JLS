# Issue #317: FEAT-007: every CI job is time-bounded, an hours-long run has a lane that is not the required gate, and all three platforms block a merge
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the four tasks away and the claim underneath is: *JLS is becoming a project
whose central evidence no longer fits in a 141-second gate.* The roadmap's capstones
are a drawn RV32 machine booting for 1.66–1.72 h (#354's own measurement), oracle
runs against external toolchains, installer smoke legs on three OSes. The tool that
produced today's CI — "run `mvn verify` on push" — was sized for a classroom
simulator, and the plan has outgrown it. That is a real and load-bearing observation,
and it is worth a feature.

But the issue answers "our evidence got big" with "buy a bigger lane," and that is
the one part of the answer this project's own recorded taste rejects everywhere else.
ARCHITECTURE.md's decision log is a catalogue of *declining* to build infrastructure
before demand exists: no second simulation strategy (#221), out-of-process plugin
isolation reserved rather than built (#222), i18n declined outright. A standing
multi-hour nightly, built before a single capstone acceptance test exists, is exactly
the speculative commitment those decisions decline — and the issue half-knows it.
Open Question 1 asks "whose budget pays," Open Question 4 says the feature is
"UNOWNED at 3-6 maintainer-weeks" and "the clearest case in the plan of shared
infrastructure with no committed owner." A feature that has to argue for its own
funding twice in its own text is telling you where the seam is wrong.

## The basket is three different things

Verified at HEAD (`git grep -c "timeout-minutes" -- .github/workflows/` returns
nothing; 23 jobs across six files — ci.yml 14, release.yml 5, one each in codeql,
mutation, repro-installers, scorecard). The premise holds. But the four tasks belong
to three unrelated evidence classes, and the only thing binding them is that they all
edit files under `.github/`:

- **TASK-0015** is a repo-wide invariant, one day, blocks nothing, and slots into an
  idiom the project already has eight instances of (`HeadlessCoreRatchetTest`,
  `NotificationRatchetTest`, `SocketConfinementRatchetTest`, `PointerApiRatchetTest`,
  `ExtensionPointCatalogTest`, …).
- **TASK-0017** is platform parity, and by the issue's own dedup comment it is owned
  twice over: #406 *and* the eight slices #661–#664 / #667–#670 "own the same four
  `ci.yml` edits," with the comment conceding "executing both paths lands them twice."
- **TASK-0018** is the residual of a shipped, already-gating rig.
- **TASK-0016** is the only genuinely new capability, and it is the one whose design
  I think is wrong.

§2 argues each cut individually and never argues why the four are *one feature*. They
aren't. They're one directory.

## Alternative A: make the long-run lane unnecessary (the reframe)

**The strongest thing in this corpus is invisible to this issue.** #363 (FEAT-035) is
titled *"a running simulation can be written to disk and resumed as the byte-identical
continuation — same time, same pending events, same memory and register contents."*
#354 §5 criterion 2 independently commits to proving that a paused-and-resumed batch
run is byte-identical to an uninterrupted control run. #317 cites neither: #363 appears
nowhere in `blocked_by`, `blocks`, or `related`, and #354 is cited only as "a lane
cannot host a run that cannot be paused."

That is backwards. Checkpoint-and-resume is not a nicety the lane needs; it is the
thing that **deletes the lane problem**. If a 1.7-hour boot can be cut into segments
that each resume from the prior segment's serialized state, then:

- No job ever approaches the six-hour hosted ceiling — not by cron scheduling, but
  *by construction*. `timeout-minutes` on a 20-minute segment is a real bound, not a
  ritual.
- A failure localizes to a segment. Today a 1.7-hour red run tells you "it broke
  somewhere in 1.7 hours."
- A re-run costs one segment, not the whole boot. The economics that make Open
  Question 1 hard stop being hard.
- The segments are ordinary bounded jobs, so they can run on `workflow_dispatch`, on
  a release tag, or on a label — not on a standing nightly nobody funded.
- The checkpoint chain *is* the artifact. Each segment's serialized state has a
  checksum, and the required gate can verify a cheap prefix (segment 1) against it
  bit-identically, exactly the way this project already treats the reproducible jar
  (`.buildinfo` + independent-rebuild recipe) and the simulation goldens
  (`BatchSimulationGoldenTest`, `VcdExportGoldenTest`, and #221's binding
  differential-oracle clause).

This converts "we need somewhere to run for hours" into "we need a reproducible
producer and a cheap verifier" — a shape JLS is already fluent in. It also makes the
fixture-storage question (TASK-0016's second half, $s_{\max}$ and $\mathcal{M}$) mostly
evaporate: a checkpoint derived from a pinned recipe plus a checksum is the
generate-on-demand answer the issue already lists as its recommended default, and it
scales to guest images too.

**Concretely, I would re-cut the long-run half of this feature as:** *"a run larger
than one CI job is expressible as a checkpoint chain."* Its prerequisites are #354 and
#363, not a cron edit; its deliverable is a documented segmentation invocation plus one
representative chained run; its acceptance evidence is byte-identity across the seam,
which #354 is already on the hook to prove. The `longrun` JUnit tag and the
default-skip surefire execution survive unchanged — they're the right mechanism for
"this test is too slow for the gate" regardless. What I would drop is the widened
nightly cron as the *primary* answer.

## Alternative B: the promotion ledger as code

`ci.yml` is now 1145 lines, and its most distinctive content is ~30 lines of
hand-written promotion prose per GUI lane (`:320-352` Wayland, `:478-500` X11, with
near-verbatim duplicates for macOS `:574-576` and Windows `:703-706`) — 20-run green
records listed as raw GitHub run IDs, the intervening-failure exculpation, the
"maintainer then registers the NEW job name" step that lives outside the repository.
This is genuinely good practice and it is *entirely unverifiable by any test*. The
duplication is already visible; a fifth lane makes it worse.

One artifact collapses most of this feature. A `.github/ci-lanes.yml` policy file with
one row per job — name, `timeout-minutes`, required-or-advisory, owning issue, accrual
record, promotion rule — plus one ratchet test cross-checking it against
`.github/workflows/**` in both directions. That is precisely the
`docs/extension-points.md` ↔ `ExtensionPointCatalogTest` pattern the project already
runs, which the issue never notices it has.

What that single mechanism delivers: TASK-0015's invariant and criterion I1 (a job
with no timeout has no row, so the test fails naming file and job); TASK-0017's
promotion discipline and criterion I4 (an advisory lane is *tracked debt with an
owning issue*, not a permanent state, and a job-wide `continue-on-error` with no row
fails the build); §4 invariant 5 (the 20-run rule becomes machine-checked rather than
prose); and §4 invariant 3's headroom convention, which today lives in a `pom.xml`
comment at `:395-407`. It also gives the maintainer's out-of-repo branch-protection
action a checkable in-repo counterpart — the issue's §1 "explicitly out of scope"
concession that "the plan cannot assert it from the tree" is true of the *setting*,
but the *intent* is perfectly assertable, and the drift between them is the failure
mode worth catching.

## Where the issue pulls against the arc

- **TASK-0017 duplicates work already sliced.** The dedup comment names the collision
  and then defers it as "a maintainer call, not a dedup call." From the trajectory
  view it is neither: #265 and #111 are *platform* features with nine and six stages
  of platform-specific substance (WindowServer, TCC, `PATHEXT`, oss-cad-suite pinning).
  #317 is a *policy* feature. Policy should not own four line edits inside someone
  else's platform work. Cede the edits; keep the policy row in the ledger above, which
  is what makes the promotion legible across all four lanes at once.
- **`.gitattributes` already carries a fixture policy** (`*.jls -text`,
  `test/resources/** -text`, both naming #111). So the "no fixture policy exists"
  premise is half stale: the *line-ending* policy shipped, the *size* policy did not.
  Worth saying, because it shows the storage question is a genuinely separate axis
  from anything else in this feature.
- **The display-suite retry masking** (`pom.xml:293`, `rerunFailingTestsCount` 2) is
  a required-gate lie that this feature depends on removing (#162 → #91, per the
  roster-addition comment). A gate that retries its flakiest suite twice and then
  claims cross-platform determinism is the deeper version of the problem #317 is
  about, and it deserves higher billing than a dependency footnote.

## What I would keep verbatim

Capability claims 1, 3 and 5 (bound every job; a stated gate budget that fails as a
*budget violation* rather than passing slowly; a fixture guard) are excellent and
under-served by the plan's shape. Claim 3 in particular is the sharpest idea in the
issue and appears nowhere else in the corpus: most projects let the gate rot from 141 s
to 20 minutes one commit at a time. TASK-0018 is correctly scoped, correctly marked
residual, and correctly independent — `JBR_SHA256` sitting at
`"UNVERIFIED-PLACEHOLDER"` while the lane hard-gates is a live fail-open on a network
download, and pinning it should not wait on any of this.

## Disregarding acceptance criterion 2 and I3 as written

Explicitly: I am setting aside §1 claim 2 ("a scheduled long-run lane exists … and
hosts at least one run measured in hours") and criterion I3. The better goal is *"a run
too large for one job is expressible, resumable, and cheaply verifiable,"* which is
strictly stronger — it also covers the case where the run outgrows six hours, which
the nightly-lane design cannot — and it is buildable out of #354 and #363, which the
plan is already committed to. The nightly cron stays what it is today: the accrual
cadence for stability records, which is a good use of it.

## Verdict

**endorse-with-reframing.** The problem is real, the timeout invariant is overdue, and
the gate-budget idea is better than the issue gives itself credit for. But this should
be three things, not one: a one-day ratchet (ship now, blocks nothing), a lane-policy
ledger that makes promotion machine-checked and cedes the per-platform edits to #265
and #111, and a genuinely new "runs larger than one job" capability built on
checkpoint-resume (#363) rather than on a nightly nobody has agreed to fund. Done that
way the 3–6 maintainer-week unowned block dissolves into one day of work that lands
tomorrow, one week of ledger work that pays for itself immediately, and one capability
that correctly waits for its real prerequisite.
