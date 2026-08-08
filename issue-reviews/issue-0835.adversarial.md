# Issue #835: TASK-C572-2: toggling an input and watching the trace either works in the browser or does not, per circuit — and the demo path is read-only by construction, not by intention
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#835 is TASK-C572-2, the second of three tasks under FEAT-C32-1 (#572):
after TASK-C572-1 (#833) produces a CheerpJ-wrapped build and times its
load, #835 checks whether the running demo is actually usable — toggle an
input, watch the trace — and whether it is genuinely read-only. The
instinct to separate "loads fast" from "works" is correct and the explicit
failure-mode naming (AC-1) and "not by the absence of a button" instruction
(AC-3) are real discipline. But the task presupposes in its own title a
property its own AC-3 hasn't verified yet, its network/read-only criteria
are written against an idealized artifact that the sibling task handing it
the build (#833) never commits to producing, and two of its five ACs have
no rubric, making "pass" gameable in the direction of a false go.

## Findings, most severe first

### 1. [High] AC-4 ("no network endpoint beyond static assets") is checked against an artifact that, as #833 specifies it, likely fails that check by default

AC-4: "No network endpoint is contacted at runtime beyond fetching the
static assets; this is checked in the browser's network panel and
recorded." CheerpJ's standard (community/free) deployment loads its own
runtime loader (`cheerpjloader.js` and related assets) from Leaning
Technologies' CDN at page-load time unless the runtime is explicitly
vendored/self-hosted. #833 (TASK-C572-1), the task this one depends on,
never requires self-hosting — its AC-1 only says "served from static files
only — no backend process is stood up," which describes the *demo's own*
hosting, not whether the CheerpJ runtime itself is fetched from a
third-party origin. As written, AC-4 can be satisfied only by silently
redefining "static assets" to include a third-party CDN fetch (which
weakens the criterion to meaninglessness) or the AC fails outright the
first time it's actually run against #833's likely output — and #835 gives
the implementer no guidance for either outcome.

**Recommendation:** either amend #833 to require a self-hosted CheerpJ
runtime, or amend #835 AC-4 to explicitly allow (and separately record) a
named CDN dependency rather than lumping it silently into "static assets."

### 2. [High] AC-3's "reachable" is ambiguous for the actual artifact in play, and the issue's own title presupposes the answer

AC-3: "The demo build has no save, upload, or user-content path reachable —
verified by inspection of what the wrapper exposes, not by the absence of a
button." Nothing in #833 or #835 calls for a demo-restricted build (menus
stripped, `File > Open`/`Save`/`Print` disabled); #833 wraps "the JLS jar"
— the ordinary interactive Swing GUI. CheerpJ virtualizes a filesystem
inside the browser tab, so `File > Save` will very plausibly still be
clickable and "work" against that virtual FS — nothing persists across a
reload and nothing crosses the network, but the menu path *is* reachable
and functional in the UI sense the phrase "not by the absence of a button"
seems designed to catch. The AC never defines whether "reachable" means
"UI-clickable" or "persists/exfiltrates data," so a functioning-but-local
Save menu could be recorded as either a pass or a fail depending on who's
asking — and the issue's own **title** ("the demo path is read-only by
construction, not by intention") already asserts the conclusion AC-3 is
supposed to test, biasing whoever executes this task toward confirming
rather than actually checking.

**Recommendation:** define "reachable" concretely (e.g., "any action that
writes outside the browser tab's own memory, or that a user could mistake
for saving their work"), and reword the title to a question, not an
asserted fact, until AC-3 has actually run once.

### 3. [Medium] The "three example circuits" in AC-1/AC-2 are never anchored to the same three circuits #833 measured

#833 AC-2 measures load time on "the three biggest example circuits."
#835's AC-1/AC-2 say only "the three example circuits" — dropping
"biggest" and never stating "the same three circuits used in #833." If
#835 is executed by a different person, or even the same person on a
different day, nothing in the text prevents fidelity being checked on a
different trio than the one whose load time was measured, which would
leave #837's decision document (per its own AC-1, "per-circuit load times
and fidelity results cited inline") citing two tables that don't actually
describe the same circuits.

**Recommendation:** state explicitly "the same three circuits selected in
TASK-C572-1 (#833)," or better, have #833 name and commit the corpus list
so both tasks read from one source.

### 4. [Medium] The corpus problem is inherited, not solved: no curated example set exists yet

No `resources/samples/` directory exists in this checkout, and the curated
set that's supposed to supply "the" example circuits — #548 (FEAT-C27-2) —
is still open. The only `.jls` files present (`test/fixtures/*.jls`,
`riscv/gui/cpu.jls`) are test fixtures, explicitly off-limits as samples
per #73's fresh-authorship rule (per the earlier review of #572, finding
1). #835 silently assumes #833 will have picked *something* representative
by the time this task runs, but neither task names a selection metric, so
a "pass" recorded here carries no claim to being representative of the
real curated set once #548 lands.

**Recommendation:** add `#548`/`#511` to `ordering_after`, or have #835
explicitly flag that its corpus is provisional pending the curated set.

### 5. [Medium] AC-1's failure taxonomy is closed and omits plausible CheerpJ-specific failure modes

"the failure modes named (missing rendering, dropped events, unusable
timing)" — three categories. The prior review of #572 (finding 4) already
flagged that CheerpJ's threading model (historically requiring
`SharedArrayBuffer`/COOP-COEP headers for real Java thread support) can
degrade silently: updates could arrive *visibly* but out of order relative
to the sim-thread → EDT discipline ARCHITECTURE.md documents, which is
neither "missing rendering" nor "dropped events" nor "unusable timing" —
it's "wrong but plausible-looking," the exact failure mode AC-1's taxonomy
has no slot for. An uncaught JS/WASM exception freezing the tab is another
gap. A failure outside the three named buckets has no prescribed home in
the record.

**Recommendation:** add an explicit "other (describe)" category, and name
desynchronized-but-visible updates as a specific thing to look for, not
just "unusable timing."

### 6. [Medium] AC-2's screenshot comparison has no rubric

"Rendering fidelity against the desktop program is compared side by side
(screenshot pairs)... so 'it runs' is distinguished from 'it looks like
JLS.'" No method is specified: pixel diff vs. eyeball, matching window
size, matching OS, or matching look-and-feel. ARCHITECTURE.md records that
FlatLaf light is now the default LaF (#153) and that "~126 hardcoded
chrome/canvas color call sites... still fight every look-and-feel" — a
real, documented source of visual divergence unrelated to CheerpJ fidelity
at all. Without pinning comparison conditions, "looks like JLS" is a
subjective call that two reviewers could score oppositely on the same
screenshots.

**Recommendation:** pin OS, window size, and LaF for the desktop-side
screenshots, and state whether the comparison is human-eyeball or a
diffing tool.

### 7. [Low] AC-5's promise to #837 is one-sided

AC-5: "Findings are recorded on #572 in a form the go/no-go (TASK-C572-3)
can cite directly, per circuit and per criterion." The already-reviewed
#837 (TASK-C572-3) only requires its decision document to cite "per-circuit
load times and fidelity results" (its own AC-1) — not #835's AC-3
(read-only verification) or AC-4 (network panel) findings specifically.
#835 can produce a rich, well-organized per-criterion record and still have
#837 legally ignore the licensing/read-only/network parts of it, since
nothing on the #837 side is obligated to pull them in (this gap is also
flagged from the #837 side in the existing #837 review, finding 1).

**Recommendation:** cross-reference #837 AC-1 explicitly and request it be
amended to require citing #835's AC-3/AC-4 results, not just AC-1/AC-2.

### 8. [Low] `ordering_after` names its dependency by description only, not by number

`ordering_after: ["TASK-C572-1 (the wrapped build being measured)"]` — every
sibling in this cluster (#572, #573, #574, #837) cites dependencies with a
`#nnn` issue number; this one doesn't. Harmless once resolved (confirmed
via search: TASK-C572-1 is #833) but a minor, avoidable inconsistency in a
family of issues that is otherwise careful about exact cross-references.

**Recommendation:** add `#833` explicitly.

## What's solid

- Separating "loads fast" (#833) from "actually works" (#835) is the right
  task split — a demo that appears in 8 seconds and can't be interacted
  with is correctly treated as a no-go regardless of AC-1's timing pass.
- AC-3's "not by the absence of a button" instruction is a genuinely good
  verification discipline — it explicitly rejects the weakest possible
  check (nothing observed = pass) in favor of inspecting what the wrapper
  actually exposes, even though finding 2 shows the target property itself
  is underspecified.
- Correctly sequenced after TASK-C572-1 (`ordering_after`) — you cannot
  assess interaction fidelity before a build exists.
- Requiring a named failure mode rather than a bare pass/fail (AC-1) is
  good practice, even though its taxonomy is incomplete (finding 5).
- AC-3's framing that #38's threat model "gains no new surface" because
  the path is read-only is the right question to be asking given this
  project's documented history of malicious-attachment attacks (#38,
  SECURITY.md) — it's the verification of that claim that's underspecified,
  not the instinct to ask it.
