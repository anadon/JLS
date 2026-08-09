# Issue #590: FEAT-C36-3: the successor positioning statement has a published home, and a flare-moment release ships with a writeup submitted somewhere instead of a bare tag
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Two things are bundled here that only look related. The first is *identity*: JLS
should be able to say what it is in one paragraph that a stranger can check. The
second is *distribution*: a release should leave the repository as something a
human reads, not as a git tag nobody sees. Both are correct ends, both are cheap,
and both are the kind of near-zero-engineering leverage #508 explicitly asked for
against an arithmetic that does not close (~1,100 mw filed, bus factor 1). I
endorse the destination.

What I want to argue with is the identity the issue picked, and the fact that
every acceptance criterion here is a human promise in a repository that has spent
its entire modernization arc converting human promises into machine-checked ones.

## Reframe 1 — the successor claim borrows a lineage JLS does not need

#510 §5 proposes: *"the maintained, modern successor in the Digital tradition."*
Read that against AC-5 — every clause maps to something a reader can verify — and
it fails its own test today. There is no `.dig` importer (#510 names it as the
thing the successor play *depends on*, and CAP-29 #513 has not landed); #510's own
matrix scores JLS *below* Digital on hierarchy (2 vs 5), scale/perf (2 vs 5) and
HDL interop (3 vs 5). A reader who verifies "successor in the Digital tradition"
finds a tool that cannot open a Digital file and loses three of the axes Digital
is admired for. Under AC-5 the honest edit is to cut the clause, and AC-5 says cut
rather than soften — so as filed, the feature's headline deliverable is largely
self-deleting.

Worse, the claim is *structurally rentable*. It is a bet on someone else's
decline, and #510 says the window "is not permanent — Digital is one motivated
fork away." A positioning statement whose truth value is controlled by another
maintainer's commit cadence is the one kind of claim this project should never
make, because it will need retracting at the exact moment it is working.

JLS has a better lineage and it is sitting in the README already: an educational
simulator written at Michigan Tech, released under an actual 2014 university
consent letter (`pop_GPLv3.pdf`), **still maintained by one of its named
authors**, with a documented grading contract (`docs/batch-interface.md`), a
normative semantics spec (`docs/simulation-semantics.md`), byte-reproducible jars
(`docs/reproducibility.md`), signed provenance, and a headless multi-arch
container. Every one of those clauses is verifiable *from this repository, today,
without reference to any competitor*. That is the statement to publish. Digital's
stranded contributors are still reachable — but you reach them with a
*recruitment* message (#510 §5's play 3/4), not by re-titling yourself their
successor in the README.

**I am disregarding the "successor positioning" framing in AC-1's stated form.**
The goal — one checkable paragraph in the first place people look — survives
intact and is better served by a claim about JLS.

## Reframe 2 — publish a claim ledger, not a paragraph

AC-1's "worded identically in both places" is a hand-maintained duplication
invariant, and AC-4's "checked against their current release at posting time" is a
hand-maintained freshness invariant. This repository does not tolerate either
shape anywhere else: `HeadlessCoreRatchetTest` enforces a boundary that could have
been a comment; `PinFaceContractTest` pins a geometric rule; #545 AC-4 already
demands a drift check that fails the build when a README-referenced image path
does not exist; `scripts/wayland-rig-selftest.sh` and
`scripts/icestick-handoff-selftest.sh` are *selftests for demo rigs*. A project
that writes a selftest for its screenshot rig should not ship its central public
claim as prose two humans must keep in sync.

The concrete alternative: **`docs/positioning.md` as a claims ledger.** One table,
one row per clause of the paragraph, each row carrying (a) the clause text,
(b) the evidence link, (c) the checkable form of the evidence. The prose paragraph
is generated from — or asserted equal to — the ledger's clause column, and a test
in the `ReadmeOnboardingTest` shape fails the build when the README paragraph
drifts from the ledger, when a cited path does not exist, or when a competitor row
lacks a `checked-at` date newer than the release being cut. That single artifact
subsumes AC-1 (one source, rendered to N surfaces), AC-4 (staleness becomes a
build failure rather than a discipline), and AC-5 (a clause with no evidence
column literally cannot be committed). It also gives CAP-36's other features a
place to deposit their output: PF-1's comparison notes and PF-2's white paper
become evidence rows, not separate documents someone must remember to link.

This is the reframing that makes most of the issue's work disappear rather than
adding to it.

## Reframe 3 — "the site" does not exist, and README ownership is already spoken for

AC-1 requires the statement in "README and site 'about'". There is no site: no
Pages workflow in `.github/workflows/` (ci, codeql, mutation, release,
repro-installers, scorecard), no site source in the tree. AC-1 therefore quietly
mints a second publishing surface — with its own build, hosting, and drift
surface — inside a 0.5-1 mw non-code feature. Either drop the site clause (the
README *is* the shop window per #545) or file the site as its own thing.

Meanwhile the README paragraph is triple-owned: #545 FEAT-C27-1 owns the shop
window, and its boundary note records that #73's README onboarding pass and #381
TASK-0030 already plan "a positioning paragraph" as the baseline. The pass-2
dedup comment on this issue checked #582 and #553 but not #545/#73/#381. Cleanest
seam: **#590 owns the ledger and the announcement practice; #545 owns placement.**

## Reframe 4 — the announcement channel the project's own review already found

AC-2's "named target venues" imports a broadcast model — post to the places, hope
it lands. But #508's finding is the opposite: adoption here is zero, the one live
user base is *elsewhere and named* (bsiever's fork, WashU CSE 260M, an ACM CF'25
paper), and item 1 of its direction is "contact Bill Siever — ≈0 mw, highest
leverage." #510 §5 likewise names individuals: the authors of Digital's #1464 and
#1470, this repo's two bounced 2026 PR authors. In a niche of a few hundred
instructors, a writeup posted to a venue is the weaker half of the job.

So make the checklist a **dispatch list, not a venue list**: named humans and
lists first (fork maintainers, the MTU/GVSU lineage, the rejected-contributor
pool, SIGCSE-members/discipline mailing lists), public venues second. Same 0.5 mw,
strictly higher expected value, and it converts the vaguest AC in the issue into
something with actual rows.

## Reframe 5 — do not debut the process on the flare

AC-3 is the right instinct (a checklist nobody ran is a file, not a practice) but
it couples closure of a 0.5-1 mw feature to a release event that depends on
CAP-02's ≈10 mw Linux-boot slice. That makes the cheap feature unclosable for a
quarter or more, and — worse — schedules the *first ever* run of an untested
process onto the single release where getting it wrong costs the most.

Rehearse it on the next ordinary release (5.0.5 is already accumulating in
CHANGELOG). AC-3 then reads: "exercised on a release that is not the flare
moment." The flare release becomes the second run, not the first.

## Reframe 6 — make the demo the announcement

The strongest version of a JLS flare writeup is not prose about a Linux boot; it
is a *reproduction command that cannot rot*. `ghcr.io/anadon/jls` already runs
headless multi-arch; the batch interface is a stability contract; the jar is
byte-reproducible. If AC-2's "demonstrable artifact" is sharpened to "a one-command
reproduction, exercised by CI on every release," the writeup's central claim is
self-verifying, the artifact survives the news cycle, and the announcement
inherits the only property this project is genuinely category-best at. That is
also the honest answer to #510's "publish the benchmark" gate (#508 item 4 and the
existing `riscv/bench_kernel.py`): the number goes in the ledger with the command
that regenerates it.

## What I would keep verbatim

- The refusal to make reception an acceptance criterion. Correct, and the pass-2
  comment's instinct to push reception metrics to #582's KPI policy is right.
- AC-3's principle that a checklist is only real once run.
- AC-5's cut-don't-soften rule — it is the best line in the issue, and it is what
  condemns AC-1's own headline clause.

## Verdict

**endorse-with-reframing.** The ends — one checkable paragraph, and releases that
ship with words — are aligned with the project's arc and cost almost nothing.
The means need three changes: publish a claim about JLS rather than a claim about
Digital's decline; make the statement a single-sourced, drift-checked ledger
instead of two hand-synced paragraphs and a discipline; and rehearse the
announcement on an ordinary release, dispatched to named humans, with a
one-command reproduction as its evidence.
