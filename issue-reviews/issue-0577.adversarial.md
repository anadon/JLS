# Issue #577: FEAT-C33-3: a real course's labs live in the tree — the CSE 260M corpus lands as compatibility fixtures and ships as the first course kit
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue bundles two very different projects under one number: (A) ingest an
external circuit corpus as versioned CI fixtures, and (B) ship an adapted
"course kit" derived from someone else's copyrighted course materials. (A) is
concretely scoped and — per the issue's own second comment — unblocked today.
(B) is gated on a real, unresolved negotiation with a third party (Dr.
Siever) and on two other unshipped features (#576, #578). The title asserts
the combined outcome ("...ships as the first course kit") as if it were
settled, while AC-3 says in the same body that the kit half may never ship
("the kit half is held"). That framing problem, plus a real content-licensing
verification gap and an undeclared dependency on unshipped grading
infrastructure, are the load-bearing defects below.

## Findings, most severe first

### 1. Title/body contradiction: the outcome is asserted as certain, the acceptance criteria say it's conditional
The title states flatly: "the CSE 260M corpus lands as compatibility fixtures
**and ships as the first course kit**." But AC-3 reads: "Content licensing is
settled in writing with Dr. Siever **before** any adapted material ships;
**absent agreement, the fixtures land as compatibility fixtures only and the
kit half is held**." The second round-2 comment reinforces this further,
explicitly forbidding "no adaptation, no rewriting, no reuse of course
prose, and no shipping of anything as a JLS 'course kit'" until AC-3 clears.
A closer/triager skimming only the title will believe the kit is a committed
deliverable; it is in fact contingent on an unresolved external conversation
that neither this issue nor #509 has any lever to force. **Recommendation:**
retitle to something conditional ("...corpus lands as compatibility fixtures;
kit ships if licensing clears") or split into two issues — one for the
unconditional fixture work, one for the licensing-gated kit — matching what
the round-2 comment already did operationally by separating AC-1/AC-2 from
AC-3/AC-4.

### 2. AC-1 silently drops part of the criterion it claims to satisfy in #509
Comment 2 states as settled fact: "**#509's AC-3 (corpus passes load +
simulate + grade) is satisfied by AC-1 here**." But #509's actual AC-3 text
is: "The full CSE 260M lab corpus passes load + simulate + grade **on a
tagged release of this fork, byte-stable across the course's platforms**."
#577's AC-1 only promises "a CI lane loads + simulates + grades every
fixture, failing the build on a regression" — a per-commit CI check, with no
mention of tagged releases or cross-platform byte-stability. Those are not
equivalent claims: CI green on `main` says nothing about a *tagged release*
being byte-stable across Windows/macOS/Linux, which is exactly the platform
concern #509 itself calls out (`README.md`'s installer matrix, the msi/dmg
"dependable" language referenced in #509's item 3). **Recommendation:**
either add a platform/tagged-release dimension to AC-1, or stop claiming
AC-1 discharges #509 AC-3 in full — say explicitly which slice of #509 AC-3
remains open.

### 3. AC-3's licensing gate is not verifiable by CI or by a reviewer — it is a real hazard, not a formality
"Content licensing is settled in writing with Dr. Siever" is unfalsifiable
from inside the repository: there is no artifact type specified (an email
thread? a signed license file committed to the tree? a maintainer
attestation in a PR description?) that a reviewer or CI could check for. Per
comment 2 §4, AC-1/AC-2 are authorized *now*, before that agreement exists,
with the safety valve worded as: "If any file's redistribution licence is
not already clear from its published source, that file waits for AC-3." That
test — "already clear from its published source" — is a legal judgment call
with no defined bar, made by whoever files the PR, checked by no automated
gate. A public course website is not automatically a redistribution license;
default copyright applies to Dr. Siever's (and WashU's) lab content unless
stated otherwise, and the issue supplies no mechanism to distinguish
"published so I can see it" from "published so I can redistribute it in this
project's fixtures." This is exactly the kind of criterion that can be
gamed: a contributor commits files in good faith believing the public
course site implies permission, CI is green, and the project has silently
taken on a copyright-infringement risk before AC-3 ever closes.
**Recommendation:** require a per-file provenance record naming the specific
license/permission basis for redistribution (not just "recorded provenance"
generically, per AC-1) before any file lands under AC-1/AC-2, and make that
record a structural part of the fixture (e.g. a manifest CI validates for
completeness) rather than a judgment call left to the PR author.

### 4. AC-1's "grades" clause has a hidden, undeclared dependency on unshipped work (CAP-06 / #300)
AC-1 requires the CI lane to "load + simulate + **grade**" every fixture.
`docs/batch-interface.md` §1 documents today's batch contract as three exit
statuses (0/1/2) with no verdict semantics; CAP-06 (#300, open, 12-20 mw,
`tier:capstone`) is explicitly the work that introduces "an exit status that
means 'the run completed and the answer was wrong'" — its own abstract says
today's grading story is a three-line Python string diff
(`examples/autograde/autograde.py:53-57`). #577's machine block lists only
`ordering_after: ["#509"]` (later corrected to `[]` for AC-1/AC-2) — CAP-06
is never named as a dependency anywhere in #577, yet "grades" in AC-1 has no
meaning without it (or without falling back to the same fragile string-diff
approach CAP-06 exists to replace). Either AC-1 is scoped to today's
load+simulate-only capability and "grades" is aspirational filler, or it
silently depends on #300 landing first and that dependency is missing from
`ordering_after`. **Recommendation:** state explicitly which grading
mechanism AC-1's CI lane uses today (string-diff `-t` output comparison, the
only thing that currently exists) and add #300 to `ordering_after` if
"grades" is meant to mean CAP-06's verdict semantics.

### 5. AC-2's "origin fork" comparison is an external, unmaintained oracle with unstated cost and reachability risk
AC-2 requires flagging any circuit that "grades differently than on the
origin fork" (bsiever/JLS). This implies either (a) building and running
bsiever/JLS as a live comparison target in CI, or (b) recording a one-time
snapshot of its output as a golden baseline. Neither is specified. (a) adds
a second, foreign Java codebase (unknown JDK version, unknown build
tooling, no CI relationship with this project) as a standing CI dependency
that can break for reasons outside this project's control. (b) is cheaper
but then "grades differently than on the origin fork" is really "differs
from a snapshot taken once," which needs to be stated so nobody assumes
live differential testing. This repo has direct precedent for the
egress/feasibility risk: `test/fixtures/legacy-4.1/README.md` records that
an *earlier* attempt to acquire an authentic external corpus (JLS 4.1 from
`pages.mtu.edu`) was deferred specifically because the sandboxed dev
environment's network egress was blocked, and the task was kicked to "the
maintainer... with normal (unblocked) egress." The same risk applies here:
fetching the WashU course site and/or bsiever/JLS releases, and possibly
building bsiever/JLS, may not be executable by whichever agent or CI runner
picks up AC-1/AC-2. **Recommendation:** specify snapshot vs. live-diff, and
flag the fetch/build of external material as a precondition that may need
maintainer-side, unblocked egress — as #56 already had to do once.

### 6. AC-4 promises conformance to two features that don't exist yet, understating the real remaining cost
AC-4 requires the adapted kit to "conform to the packaging convention of
FEAT-C33-4 [#578] and run through the assignment workflow of FEAT-C33-2
[#576]." Both are open, unshipped `tier:feature` issues (#578: 3-4 mw, #576:
2-3 mw) that themselves have their own acceptance criteria and dependencies
(#576 depends on CAP-06/#369; #578 depends on #575/FEAT-C33-1). #577's own
`band_mw: "1-2"` cannot possibly cover AC-4's work — it is entirely
downstream of 5-7 mw of other unshipped features, on top of an
indeterminate real-world negotiation (AC-3). The mw estimate is only
honest if read as "AC-1/AC-2 only," which the issue never says outright.
**Recommendation:** either give AC-3/AC-4 their own mw estimate (openly
marked as unbounded/TBD pending #576, #578, and the Siever conversation) or
strike AC-4 from this issue's estimate and note it rides on those two
features' own bands.

### 7. "Recorded provenance" (AC-1) is underspecified beyond the licensing question in #3
Even setting licensing aside, "recorded provenance" has no defined shape:
per-fixture header comment, a manifest file, a directory README? Compare
`test/fixtures/legacy-4.1/README.md`, which is the only existing provenance
precedent in this repo and is a prose README, not a machine-checkable
artifact — nothing in `test/jls/` enforces its claims. If AC-1's "recorded
provenance" is left equally informal, "failing the build on a regression"
(the CI-lane part of AC-1) will be enforced by tests, but the provenance
record itself won't be — so a fixture could be added with stale or wrong
provenance and nothing would catch it, undermining exactly the
audit trail AC-2's "named, dispositioned finding" process depends on.
**Recommendation:** define a minimal provenance schema (source URL,
retrieval date, upstream commit/tag if from bsiever/JLS, license basis) and
have a test assert every fixture under the corpus directory has one.

## What's solid

- Scoping the "reference #509, don't absorb it" boundary is done well and
  consistently — the boundary note and the round-2 correction comment both
  reflect this cleanly, and the dependency-cycle catch (#577 → #509 → #577)
  in comment 2 is a genuinely good piece of self-review that materially
  improved the issue.
- AC-2's "never silently dropped from the corpus" principle (named,
  dispositioned findings for any non-matching circuit) is the right
  instinct for a migration-fidelity fixture set and mirrors patterns
  already established in this repo (`LoadError` taxonomy, `SaveTagsTest`).
- Deferring the kit's shipping behind a licensing fallback (rather than
  assuming permission) is the ethically correct default, even though its
  implementation (finding #3) needs tightening.

## Bottom line

The fixture-ingestion half (AC-1/AC-2) is a reasonable, close-to-actionable
piece of work once the licensing-verification gap (#3), the grading
dependency (#4), and the external-oracle mechanics (#5) are nailed down.
The kit half (AC-3/AC-4) is correctly gated behind a real external
negotiation but the issue's title overclaims it as a settled deliverable,
and its cost is not reflected in the stated mw band. Rework the title/body
framing and tighten the four underspecified criteria before this is
actionable as filed.
