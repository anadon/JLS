# Issue #162: UI display lane hardening: fail-closed xvfb on the required Linux Build leg and the 20-run stability record for the display-tagged suite
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what I checked

Fetched issue #162 (open, 10 comments) plus related issues #91 and #317 it
cites, and cross-checked the issue's technical claims against the live
tree (HEAD `5311625`, well past the issue's pinned `evidence_commit`
`29afb26` from 2026-07-29). The core technical claim holds: `ci.yml:73`
still installs xvfb best-effort in the same line as the HDL toolchain
(`... yosys xvfb || echo "some optional tools unavailable..."`), `ci.yml`
`:82-86` still silently falls back to plain `mvn -B verify` (excluding the
`display` group per `pom.xml:271`) when `xvfb-run` is absent, and
`grep -rn "20 consecutive" docs/ test/` returns nothing — so P1's failure
and Observation 3 both still reproduce today, not just at the pinned
commit. Good, solid framing overall — but several things a skeptical
reviewer should push back on before work starts.

## Findings, most severe first

### 1. (High) The H2 stability-record acceptance criterion can be satisfied by a record contaminated by retry-masking, and the issue's own body doesn't gate against it
`pom.xml:293` still sets `<rerunFailingTestsCount>2</rerunFailingTestsCount>`
on the `display-tests` execution — confirmed live, not just at
`29afb26`. Issue #91 (open, this issue's own `related` list) documents
that this directly contradicts a recorded "no bounded-retry masking"
decision, and that a run which fails once and passes on rerun shows as
green in CI history. #162's own §8 Method says: "Audit the last 20
consecutive `Build (Linux, JDK 25)` runs on master ... record the count
(H2's bar: at most one failure)" — but nothing in §8, §9, §11
(Threats to Validity), or §14 (Completion Criteria) instructs the auditor
to distinguish a clean pass from a masked-retry pass. §11 flags a related
but different risk ("it cannot observe how often the silent-downgrade
path actually fired") without ever mentioning retry contamination. The
most recent comment (2026-08-08, `#issuecomment-5227473221`) *does*
recognize this exact problem — "A record accrued with retries still in
place measures the retries" — and concludes ordering must be
"this issue -> #91" (fail-closed lands first, then #91's retry-removal,
implying the 20-run record should really be taken post-#91). But that
sequencing claim lives only in a comment; the issue body's own
`blocked_by: []` and its Method checklist are unchanged, so an
implementer working strictly from the issue body (as the body instructs:
"actionable now") can produce a "P2 verified" stability table that is
exactly the artifact #91 exists to invalidate. **Recommendation:** either
add `blocked_by: [91]`-style sequencing to the body itself (not just a
comment) or explicitly state in §8/§14 that the audit must exclude/flag
any run whose surefire report shows a rerun, so the record isn't gamed by
the very mechanism #91 is filed to remove.

### 2. (High) The issue's own machine-readable status block is stale relative to its own comment thread
The fetched body's `Status & Dependencies` YAML still reads
`part_of_feature: none      # free-standing; former umbrella #33 closed
2026-07-27`. The final comment (same day, 2026-08-08) titled
"CHAIN-INTEGRITY CORRECTION" states "**Corrected field:**" and shows a
YAML block setting `part_of_feature: 317`, plus "Mirror posted on #317."
But the live issue body was never actually edited — it still says `none`.
Any tool or agent (this fleet included) that parses the body's machine
block programmatically rather than reading every comment will get the
wrong routing (`none` instead of `317`), and #317 in turn lists #162 only
under `related` ("reference-only and never blocking") while its own text
says "#91 (open) and #162 (open) carry the display-lane residuals ...
which TASK-0017's 'armed rather than best-effort' clause depends on" —
so the two documents (162's stale body, 317's live text) currently
disagree about whether 162 is `part_of_feature: 317` or free-standing.
**Recommendation:** edit the issue body's frontmatter to match what the
correction comment claims, not just post a comment about it — the
pattern here (claim-a-correction-via-comment, body left untouched) is a
recurring integrity gap this fleet should watch for across other issues
too.

### 3. (Medium) P2's deliverable is unverifiable prose with no reproducible check
§7.6 defines the stability record's "Data provided (structure)" as
"a dated note (run range, pass/fail count, promotion verdict) ...
prose, no schema." There is no companion test, script, or CI step that
re-derives or spot-checks this count later — once written into
`package-info.java` it is trusted forever. Combined with Finding #1, this
means the single load-bearing artifact that justifies promoting the
required gate's behavior is a hand-typed table nothing re-verifies.
Contrast with this repo's own convention elsewhere in the same file tree:
`scripts/wayland-rig-selftest.sh` exists specifically so the *classification
logic* backing an analogous promotion (the Wayland lane) is guarded by a
stub-driven test independent of a live rig. §14's checklist has no
equivalent item for this issue's fail-closed logic (see Finding #4).
**Recommendation:** at minimum, require the 20-run table to link real
run IDs/permalinks (not just a count) so the claim is spot-checkable, and
say so explicitly in §14 rather than leaving "recorded" undefined.

### 4. (Medium) No lasting regression guard for the fail-closed behavior itself
§8's last bullet ("Verify P1/P3 on a branch: a run with the install
forced to fail must go red...") is a one-time manual demonstration on a
throwaway branch. Nothing in §7 (Interface & Data Contract) or §14
(Definition of Done) asks for a permanent, checked-in test analogous to
`wayland-rig-selftest.sh` that would catch a future edit accidentally
reintroducing `|| echo` or the `else mvn -B verify` softening. Given this
codebase's own stated practice of self-testing exactly this class of
CI-config regression (cited above), the omission here is a real gap, not
a stylistic quibble — the fix this issue proposes is otherwise a one-line
YAML edit that a routine `ci.yml` refactor could silently undo with
nothing to fail red.

### 5. (Low) The "harden in place vs. dedicated leg" open question has an unenforced fallback
§10 Falsification Criteria's H1-refuted fallback ("fall back to a
dedicated display leg ... requires the maintainer to register a new
required check name") is an out-of-repo, GitHub-branch-protection action.
§14's checklist item for this ("The open decision above resolved ... in
the PR") only requires the *decision* be recorded, not that the
branch-protection registration actually happened if that's the chosen
path — so a PR could close with "dedicated leg" as the recorded decision
while the new check is still non-required, leaving the exact
silent-coverage-hole problem this issue exists to fix, just relocated to
a different check name. Low severity because the default path
(harden-in-place) sidesteps this entirely and is explicitly recommended.

### 6. (Low) Evidence pinned to a stale commit, but self-aware and still accurate
`evidence_commit: 29afb26` (2026-07-29) is well behind current HEAD
(`5311625`, many commits later per `git log`). The issue explicitly flags
this ("Line numbers here are pinned at 29afb26 — re-derive before
trusting if HEAD has moved (rule 6)"), and I verified the two load-bearing
citations (`ci.yml:73`'s `|| echo`, and the absence of "20 consecutive" in
docs/test) still reproduce byte-for-byte at HEAD, so this isn't an actual
defect — just a maintenance note for whoever picks this up.

## What's solid (no changes needed)

- The core defect (Observation 1/2: best-effort xvfb install + silent
  headless fallback on the *required* leg) is real, current, and
  precisely cited — verified independently against HEAD, not just the
  pinned commit.
- Scope is honestly narrow: the issue correctly identifies that the
  feature-sized original work has landed (verified via #194/#196/#176/#193
  in git log: `43834b9`, `64edf01`, `0193e21`, `9ebb556` etc. all
  reference #162) and this residue really is just the two remaining
  checkboxes.
- The boundary against #91 (arming vs. flake-quarantine/retry-masking)
  is a reasonable separation of concerns on paper, even though its
  practical sequencing isn't yet enforced (Finding #1).
- Falsification Criteria (§10) are genuinely falsifiable and the
  fallback for H1 (dedicated leg) and H2 (refuted stability) are honest
  "this might not work, and that's a valid outcome" framings rather than
  success-only criteria.

## Verdict rationale

The technical premise and remaining scope are sound and independently
verified against the live tree. The verdict is not `needs-rework` because
the P1/P3 fail-closed work (the more consequential half) is well-specified
and low-risk. It is `sound-with-concerns` rather than plain `sound`
because the H2 stability-record acceptance criterion (Finding #1) can
currently be satisfied by a contaminated count without anyone noticing,
and the issue's own governance layer (its machine-readable status block,
Finding #2) is already out of sync with its own comment thread before any
code work has even started.
