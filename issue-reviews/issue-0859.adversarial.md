# Issue #859: TASK-C581-2: the cask's Gatekeeper caveat is the README's paragraph, asserted equal by a drift check — the user reads the workaround before macOS refuses the app
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

Second of three TASK-C581 tasks under FEAT-C34-3 (#581, "brew install
--cask jls"). #858 (TASK-C581-1, still open, cask location undecided)
creates the cask; this issue makes its `caveats` block byte-equal to
"the README's Gatekeeper paragraph" via a committed, CI-enforced drift
check anchored to a stable README marker, with a seeded-divergence test
proving the guard actually catches drift.

## Findings, most severe first

**1. "The README's Gatekeeper paragraph" does not exist as a distinct
unit — the source is one markdown list item mixing Gatekeeper content
with unrelated material, and the issue never draws the boundary.**
Evidence, `README.md:37-43`:
```
- **macOS:** `JLS-<version>-aarch64.dmg` (Apple silicon). The app is
  unsigned by choice, not oversight — signing requires paid Apple
  Developer Program enrollment, which this free university tool
  deliberately forgoes (#128, #135). Gatekeeper therefore blocks a
  plain double-click the first time: right-click (Control-click) the
  app and choose "Open", then confirm — needed only once. Intel Macs:
  use the jar below.
```
This is a single Markdown paragraph node (one list item, no blank
line, no sub-headers) containing: the dmg filename, the
unsigned-by-choice rationale with issue cross-refs (#128, #135), the
actual Gatekeeper workaround, and an unrelated Intel-Mac fallback
note. AC-1 says "byte-equal to the README's Gatekeeper paragraph,"
AC-3 says "anchored to a stable marker" — but there is no marker,
Markdown or otherwise, that isolates the Gatekeeper sentences from
the rest of the bullet today, and the issue doesn't specify where the
extraction should start/stop. Three different implementers extracting
"the paragraph" could reasonably produce three different caveat
texts (workaround sentence only; workaround + rationale; the whole
bullet including the Intel-Mac note). **Recommendation:** quote the
exact intended span in the issue (e.g. by adding the anchor markers
in the issue body itself), not just gesture at "the paragraph."

**2. "Byte-equal" is in direct tension with the two sides' formatting
conventions, and the issue resolves neither.** The README is
hand-wrapped prose at ~70 columns with a mid-sentence line break —
"Gatekeeper therefore blocks a" ends line 40, "plain double-click the
first time…" continues on line 41 (`README.md:40-41`). A Homebrew
cask's `caveats` block is a Ruby string/heredoc, conventionally
formatted to `brew style`'s own rules, which #858's AC-4 requires to
pass `brew audit --cask`. True byte-equality between "prose wrapped
for a README" and "a string formatted for `brew style`" forces one of:
(a) the cask literally reproduces README's mid-sentence line breaks in
its terminal output (ugly, and likely an audit/style violation,
conflicting with #858 AC-4), or (b) the drift check normalizes
whitespace before comparing, which is no longer "byte-equal." The
issue asserts both properties (AC-1's literal byte-equality, and
implicitly #858 AC-4's audit-passing cask) without acknowledging they
may be mutually exclusive. **Recommendation:** define equality on
normalized text (e.g. whitespace-collapsed) explicitly, drop "byte"
from AC-1, and cross-reference #858 AC-4 so the extraction is checked
against `brew audit --cask` before it's called done.

**3. AC-1's core claim — "extracted... rather than copied by hand" —
has no verification of its own and is trivially gameable.** AC-2 and
AC-4 only test that *some* check fires on a seeded divergence between
two strings; nothing tests that the cask's string is actually derived
by parsing README.md at build/CI time rather than being two
independently-maintained literals a developer keeps in sync by hand
(exactly the failure mode the issue exists to prevent). A check
implemented as "compare this hardcoded copy in the cask-update script
against this other hardcoded copy" would pass AC-2 and AC-4's red-then-
green test while leaving AC-1's actual promise ("extracted... rather
than copied by hand") unbuilt, because nothing in the acceptance
criteria distinguishes "parses README.md" from "duplicates a
literal the author kept in sync." **Recommendation:** add an AC (or
fold into AC-4) that the seeded-divergence test edits only
`README.md` and asserts the check fails — never touching whatever file
holds the cask's copy — which would at least prove the check reads
the README as its source of truth rather than a second static copy.

**4. Hard dependency on #858's still-open, still-undecided cask
location, and AC-2's CI requirement may be unsatisfiable on one of the
two branches #858 is explicitly choosing between.** `ordering_after`
correctly names `"TASK-C581-1 (the cask)"`, but #858's own body frames
"tap vs. `homebrew/cask`" as an open executor decision (#858 AC-3).
If the cask ends up in the third-party `homebrew/cask` repository
(not this repo), then "a committed check... runs in CI on every
change to either file" (AC-2) cannot watch the cask file at all —
this repository's CI has no visibility into commits made to
`homebrew/cask`. The issue never states which branch it assumes, and
under one of the two branches its central AC is not achievable as
written. (The sibling review of #860 flags the identical unresolved
fork for the version/sha256-bump automation; it is the same gap here,
one task earlier.) **Recommendation:** either make #859 explicitly
conditional on the tap outcome of #858, or specify how the drift
check works when the cask lives outside this repo (e.g., a scheduled
job that fetches the external cask file rather than a same-repo CI
trigger).

**5. AC-5 restates AC-1+AC-2's consequence rather than adding
verifiable behavior.** *"If signing later lands and the README
paragraph changes, the recorded behaviour is that the cask follows
the README — no independent copy of the stance is maintained
anywhere."* Once AC-1 (equality) and AC-2 (CI-enforced, fails on
divergence) exist, this is already true by construction — there is no
additional artifact or test implied here that AC-1/AC-2 don't already
produce. As its own AC it's unfalsifiable filler; an implementer
can't point to anything distinct they built to satisfy it.
**Recommendation:** drop it, or turn it into something checkable (e.g.
a comment in the cask file pointing back at the README anchor, tested
by the same drift check).

**6. Minor scope wrinkle: a verbatim "whole paragraph" copy would
surface information dead in the cask's context.** The bullet's Intel-
Mac fallback ("Intel Macs: use the jar below") is meaningless in a
Homebrew cask caveat, since #858 AC-1 has the cask install only the
aarch64 dmg — an Apple-Silicon Homebrew installer has no reason to be
told about Intel Macs. This compounds finding 1: whichever span gets
extracted, "the whole bullet, verbatim" (which the parent #581's
Outcome text — "carries the Gatekeeper right-click-Open note
**verbatim from the README**" — could support) drags in a sentence
irrelevant to the audience receiving the caveat.

## What's solid

- The dependency ordering on #858 is correctly declared in principle
  (cask must exist before its caveats can be checked), even though
  finding 4 shows the declared dependency doesn't cover the CI-
  location contingency.
- The fail-loud-not-silently-stale intent matches this repo's general
  error-reporting philosophy (`ARCHITECTURE.md` "Error-reporting
  contracts": structured, non-silent failure taxonomies elsewhere in
  the codebase) — the right instinct even if the mechanics are
  underspecified.
- AC-4's "shown red once before being trusted green" is a genuinely
  good practice for any new guard and needs no rework.
- The underlying motivation is real and already documented: README.md
  itself records the unsigned-by-choice stance and the Gatekeeper
  workaround (`README.md:37-43`, citing #128/#135), so this task isn't
  inventing a problem — it's automating an existing, correct piece of
  user guidance.

## Bottom line

The instinct — don't let a hand-copied caveat drift from the README's
documented stance — is sound, and it correctly inherits #581 AC-3
almost verbatim. But "the README's Gatekeeper paragraph" points at
text that isn't actually a self-contained paragraph (finding 1),
"byte-equal" is in tension with the two sides' formatting conventions
and with #858's own `brew audit --cask` requirement (finding 2), the
issue's own claim that the text is "extracted... rather than copied by
hand" is unverified by any of its ACs (finding 3), and AC-2's CI
requirement may not survive whichever way #858 resolves the tap-vs-
homebrew/cask question (finding 4). These need to be nailed down
before an implementer can build against this issue without silently
choosing among several incompatible readings.
