# Issue #863: TASK-C582-3: one line of policy names release-asset downloads as the adoption KPI and says whether store counters are in the number — so stars stop being quoted
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of the ask

A one-line(ish) in-tree policy document naming release-asset downloads as
the adoption KPI, stating whether Flathub/winget/Homebrew counters are
folded in, naming the metric's limitations, updating any other in-repo
place that quotes stars, and stating a revisit trigger. It is the third of
three sibling tasks under FEAT-C34-4 (#582): #861 builds the collector +
data file, #862 schedules it, #863 (this issue) writes the policy prose.
`ordering_after: TASK-C582-1` correctly ties it to #861.

## Findings, most severe first

**1. AC-4 targets a place that does not exist in the repository — the
acceptance criterion is either vacuous or scope-ambiguous.**
AC-4 reads: "Any other place in the repository that quotes stars as
adoption is updated to point at this metric instead." A repo-wide,
case-insensitive search for `star` across every tracked `.md` file
(README.md, ARCHITECTURE.md, SECURITY.md, CONTRIBUTING.md, CHANGELOG.md,
all of `docs/**`) turns up exactly one hit outside `issue-reviews/`:
`riscv/README.md:132`, "Fan-out becomes a single star net" — a netlist
term, unrelated to adoption. The "3 stars" figure the issue is clearly
reacting to lives in **#508's issue body** ("anadon/JLS: 3 stars, 9
forks…"), which is GitHub issue text, not a file in the git tree this
task can edit. As written, AC-4 is satisfiable by doing nothing and
noting "found none" — which is not distinguishable from an implementer
who never searched. Recommend: either drop AC-4 (nothing to do) or
narrow it explicitly to "repo-tracked files (README, docs/, SECURITY.md,
etc.), not GitHub issues," and require the PR to show the grep that
proves the negative.

**2. The "one line" framing in the title and AC-1 contradicts what AC-2,
AC-3, and AC-5 actually demand.**
The issue title says "one line of policy," and AC-1 says "in one line."
But AC-2 requires stating, per channel, whether Flathub/winget/Homebrew
counters are included, excluded, or separate — and handling the
unavailable-channel case explicitly; AC-3 requires naming four named
limitations (cumulative counts, bots, mirrors, re-downloads); AC-5
requires stating what would make the metric wrong enough to replace. None
of that fits in one line without becoming a dense, unreadable run-on.
Either the "one line" language is rhetorical (likely, given the outcome
prose calls it "the smallest possible artifact," not literally one line)
or a literal implementer will produce a single-sentence document that
fails AC-2/3/5. Recommend: rewrite AC-1 to something like "one line
names the KPI; a short paragraph beneath it covers scope and limits" so
the ACs stop contradicting each other.

**3. AC-3 duplicates ground that #861's own AC-4 already claims, with no
stated canonical source.**
#861 (TASK-C582-1) AC-4: "The file's schema is documented in a header or
companion note, including what each count means and its known
limitations (counts are cumulative, mirrors are not visible, and so
on)." #863's AC-3: "It names the metric's known limitations (cumulative
counts, bots, mirrors, re-downloads)." Two sibling tasks each independently
own a near-identical caveat list — one in the data file's header, one in
the policy doc. Nothing says which is authoritative, so a future edit to
one (e.g., someone discovers re-downloads aren't actually visible in the
API and fixes the data-file header) can silently drift from the other.
Recommend: #863's doc should state the limitations once and have #861's
header point at it (or vice versa) — not maintain two independent copies.

**4. AC-1's required "pointer to the data file and the collector" can
dangle, since ordering is advisory, not enforced.**
`ordering_after: ["TASK-C582-1 (there must be a number to name)"]` is
correct reasoning, but it is metadata in a YAML fence, not a merge gate —
nothing in the repo's CI stops #863 from being implemented and merged
before #861 lands, which would leave the "pointer to the data file and
the collector" referencing paths that don't exist yet. Given this repo's
existing convention of ratchet tests protecting exactly this kind of
cross-reference (`HelpTopicsTest`'s link checker, `NotificationRatchetTest`),
the natural mitigation is already precedented. Recommend: either block
#863 in review until #861 is merged (process control), or add a trivial
test asserting the paths named in the policy doc exist on disk.

**5. AC-2's substantive question is currently unanswerable because none
of the three channels exist yet — worth calling out, not fatal.**
Flathub (#579, FEAT-C34-1), winget (#580, FEAT-C34-2), and Homebrew
(#581, FEAT-C34-3) are all still open/unimplemented. So the "in / out /
separate" decision AC-2 asks for reduces, at the time this task is
picked up, to "state that none of the three exist yet" for all three —
correct and covered by the issue's own "if unavailable, state that"
clause, but it means this document will need substantive follow-up edits
at least three more times (once per channel landing), which the issue
doesn't flag as a maintenance obligation. Not a blocker, just an
undersold cost — worth one sentence in the doc itself ("revisit this
table when #579/#580/#581 ship") so it doesn't silently go stale.

**6. AC-5 is unfalsifiable as worded.**
"The document states what would make the metric wrong enough to replace"
sets no bar. ARCHITECTURE.md's own "Recorded decisions" section
demonstrates the higher standard this repo already holds itself to —
e.g. the discrete-event-interpreter decision's "Revisit trigger: a
concrete CPU-scale design on the `riscv/` trajectory that is unusably
slow interactively" is specific and checkable. As written, AC-5 is
satisfied by any hand-wavy sentence ("we'd reconsider if this proves
misleading"), which provides none of the "re-decision, not drift"
value the outcome paragraph asks for. Recommend: point AC-5 at the
ARCHITECTURE.md convention explicitly and require a concrete trigger,
not just a sentence that exists.

## What's solid

- The dependency reasoning (`ordering_after: TASK-C582-1`) is correct:
  you cannot name a KPI's pointer before the KPI's data file exists.
- AC-2's "if a channel's counter is unavailable, that is stated rather
  than assumed zero" is a good, specific, testable requirement — it
  mirrors #582's own AC-5 (fail loud, don't silently zero) and closes a
  real gap.
- Scope is appropriately small (doc-only, `band_mw: 0.25`, no product
  code) and the boundary notes in the parent #582 ("this is the
  measurement, not the channels") keep it from creeping into the FEAT-C34
  channel work.
- No security, licensing, or compatibility exposure — this is prose in a
  markdown file.

## Recommendation

Proceed, but tighten AC-1/AC-2/AC-3/AC-5 as above before implementation
starts, and treat AC-4 as most likely a no-op given the current repo
state (confirm via grep in the PR rather than silently skipping it).
