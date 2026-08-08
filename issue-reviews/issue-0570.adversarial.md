# Issue #570: FEAT-C30-5: the three features Digital's users spent years asking for — dark mode, diving into a live subcircuit mid-simulation, and rebindable keys — exist in JLS on their own merit
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

Issue #570 bundles three items behind one "Digital-wishlist" framing: dark
mode (disclaimed to #289, fine), a genuinely new live subcircuit-dive
feature (AC-2), and a genuinely new rebindable-keybindings feature (AC-3).
The issue itself is labeled provisional ("renumbered in the adversarial
phase"), and it reads that way: it is far thinner than its own sibling
issues in the same filing batch (#593, #594, #596, #76, #289), missing
sections those carry — no Interface & Data Contract, no concurrency
statement, no test-pinning requirement, and critically, no mention of the
SimpleEditor decomposition boundary that every other editor-touching
sibling issue in this batch treats as a hard gate. The technical premise
(each `SubCircuit` instance owns independent state, so a live per-instance
view is architecturally sound) checks out. What doesn't hold up is the
acceptance-criteria rigor and the omitted architectural conflicts below.

## Findings, most severe first

### 1. Missing the #316/#84 "hard boundary" its own sibling issues enforce
Issues #593, #594, and #596 — filed the same day, same author, same
"Digital/incumbent parity" program — each carry a verbatim clause:
*"Decomposition boundary (#316 FEAT-008 / #84) — hard gate: nothing...
lands inside `SimpleEditor`... if #316 stalls, this feature waits."*
#570 has no such clause anywhere, despite AC-2 (open/navigate a live
subcircuit view mid-simulation) and AC-3 (a rebinding UI wired into the
canvas key-binding system) being squarely editor/interaction-state work —
the exact category #316 exists to keep out of the 5,852-line god class
(`src/jls/edit/SimpleEditor.java`, confirmed at that line count via #84's
own body and reproducible with `wc -l`). #316 is `blocked_by: [317, 337]`
and its own extraction task (TASK-0020) is "not filed" — i.e., not started.
If #570 is executed today under the same "hard boundary" norm its siblings
accept, it either grows the god class (violating the norm silently) or
stalls behind #316 with no one having said so.
**Recommendation:** add the same boundary clause #593/#594/#596 carry, or
explicitly record why #570 is exempt (e.g., "AC-2/AC-3 land as new
collaborators, not SimpleEditor edits" — but that claim needs to survive
contact with `SimpleEditor.java:5153-5178`'s `doModify()`, which is exactly
where subcircuit-opening logic lives today).

### 2. AC-3's premise conflicts with `MenuAcceleratorPolicy`'s actual design
`src/jls/MenuAcceleratorPolicy.java` (confirmed by reading it) is
documented and tested as "a pure function of an injected `os.name` value" —
21 static methods each returning a fixed `KeyStroke`/mnemonic, with no
per-user override, no map, no persistence hook. The issue's own
consolidation comment insists "the rebinding path must render through the
same policy" (to keep `HotkeysHelpAccuracyTest` and `#75`'s invariant 3
honest), but a stateless, os-name-only pure-function policy has nothing
to attach a user override to — that's a structural redesign (policy
becomes: default-from-os-name, overridable-by-persisted-map), not an
additive feature. AC-3 and the comment both assume this seam already
exists or is trivial; it doesn't and isn't. This cost is not visible
anywhere in the single "4-7 mw" band shared with AC-2.
**Recommendation:** scope AC-3's D10 justification (AC-4) explicitly
against redesigning `MenuAcceleratorPolicy`'s override model, not just
against building a settings-dialog page.

### 3. The comment's "keymap-aware hotkeys.html" directive conflicts with a recorded architecture decision
The issue's own consolidation comment (finding #3) requires "`hotkeys.html`
generation made keymap-aware rather than left asserting defaults." But
`ARCHITECTURE.md` records, as a settled decision: help content "stays
plain HTML 3.2 with relative links and no viewer-specific markup, and the
`HotkeysHelpAccuracyTest` link checker... keeps it truthful, so **the same
tree can be published to the web without rewriting**." A hotkeys page that
must reflect a given user's live, persisted keymap cannot simultaneously be
the one static tree shipped in the jar and mirrored to a future hosted-docs
site — the two invariants are in direct tension, and neither #570 nor the
comment reconciles it (e.g., by scoping the "keymap-aware" behavior to a
generated, non-canonical view rather than the shipped `resources/help/**`
tree).
**Recommendation:** resolve explicitly: either the shipped help page keeps
showing defaults (with the settings dialog itself, not help, showing live
bindings), or the portability invariant in ARCHITECTURE.md needs its own
revisit-trigger recorded.

### 4. AC-2 is silent on read/write semantics and collides with existing subcircuit-edit locking
`SimpleEditor.java` already has a mechanism for "entering" a subcircuit:
`doModify()` (`:5153-5178`) opens a new editor tab and calls
`disableForSubcircuit(name)` on the parent, which shows a banner and
disables editing there (`:703-739`, `:5192-5195`). AC-2 introduces a
second, different "enter a subcircuit" flow (live, read presumably,
mid-simulation) but never states: whether the dive view is read-only or
editable; whether entering it should disable the instance's own editing
the way the existing tab mechanism does; whether two simultaneous dives
(two instances of the same subcircuit type, or nested dives N levels deep)
are supported; or what "live" means quantitatively (refresh rate,
staleness bound). As worded, "the user can open a subcircuit instance and
watch its internal signal states... then navigate back up" is satisfiable
by an implementation that is technically compliant but nearly useless
(e.g., a view that repaints only on explicit user refresh, or that quietly
permits edits mid-sim with undefined results).
**Recommendation:** state read-only-ness explicitly, define the
consistency/refresh contract, and state the nesting/multi-instance
cardinality.

### 5. No test-pinning discipline, unlike every sibling in the batch
#593 AC-2, #594 AC-4, and #596 AC-3 each require: "pinned by a #91-harness
or headless test that fails at the pre-change commit — no behaviour is
asserted only by a screenshot or a manual pass." #570 has no equivalent
requirement for AC-2 or AC-3. AC-4 — the closest thing to a verification
gate — only asks for "its own D10 path-and-cost justification recorded
before implementation," a prose/process criterion with no rubric, no
named reviewer, and no falsification condition. A live-mid-simulation
render path racing a background "Runner" thread (see finding 6) is
mechanically *harder* to verify correctly than the selection/palette work
in the sibling issues that do impose a harness-test bar — the absence of
that bar here, on the riskier feature, is backwards.
**Recommendation:** add a concrete AC (or amend AC-4) requiring a #91 or
headless test per sub-feature that fails pre-change, matching the sibling
issues' bar.

### 6. Concurrency contract for AC-2 is unstated
`ARCHITECTURE.md`'s threading model is explicit: "Interactive simulation
runs on a dedicated thread (the `"Runner"` thread...). Control state shared
between the EDT and the sim thread... is `volatile`... UI work initiated
from the sim thread is routed through `SwingUtilities.invokeLater`... —
Follow this discipline for any new sim-thread → UI interaction." AC-2 is
precisely "a new sim-thread → UI interaction" (rendering internal
subcircuit wire/element values live, sourced from simulation state, into a
newly-opened Swing view), yet #570 never cites this invariant or commits
to it — contrast #76/#289, which both restate their EDT-only concurrency
contract explicitly in a §7.9/Global Invariants section. #570 has no such
section at all.
**Recommendation:** add an explicit concurrency clause binding AC-2 to the
existing `invokeLater`/`volatile` discipline, and name it as a completion
criterion (a natural target for `EdtViolationDetector`, already used
elsewhere in the test tree).

### 7. Minor — AC-4's referential phrasing is a clarity nit
"Each of the two new sub-features (AC-2, AC-3)" only resolves by re-reading
AC-2/AC-3's text; a first-time implementer skimming criteria top-to-bottom
could misparse it as referring to a third, unlisted thing. Not
load-bearing — just tighten the wording (e.g., "AC-2 and AC-3 each carry
their own...").

## What's solid

- AC-1's dark-mode disclaimer is unambiguous and consistent with #289 and
  #76's own scope statements — no double-ownership, no scope creep there.
- The "no existing owner" search claim checks out against the tree: no
  `MouseMachine`/`InteractionState`/persisted-keymap type exists anywhere
  under `src/`, and `UserPrefs.java` (read in full) stores exactly the four
  keys the comment claims (`theme`, `gridColor`, `backgroundColor`,
  `undoDepth`) — the comment's file/line citations for `UserPrefs.java`
  are accurate.
- Per-instance `SubCircuit` state is real (`SubCircuit.java:337-352`'s
  `copy()` gives each instance its own `Circuit`), so AC-2's core premise
  — that "instance" state is a meaningful, distinguishable thing to dive
  into — is architecturally sound, not hand-waved.
- No licensing or security hazard identified: keybinding persistence would
  ride the existing sandboxed `UserPrefs`/`java.util.prefs` pattern, which
  already degrades safely with no backing store.

## Verdict rationale

The feature ideas are legitimate and the "genuinely unowned scope" claim is
verified true against the codebase — this is not a duplicate or a
fabricated need. But as filed, #570 omits an architectural boundary its
own sibling issues treat as non-negotiable (finding 1), asserts a
rebinding path through a policy class that cannot currently support it
without a redesign (finding 2), directs an approach that collides with a
recorded architecture decision (finding 3), and sets a materially weaker
verification bar than every comparable issue in its own filing batch
(findings 5-6) on features whose acceptance criteria are themselves
underspecified enough to be satisfied by a hollow implementation
(finding 4). These are fixable by rewording/adding sections, not by
abandoning the feature — hence needs-rework rather than
should-not-proceed.
