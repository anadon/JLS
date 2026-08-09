# Issue #808: TASK-C594-2: a recently-used set and full keyboard palette navigation, over #75's shared Action layer rather than a second focus model
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Findings, most severe first

**1. AC-4's "model-side collaborator, not a field on `SimpleEditor`" leans on an extraction seam that is unfiled, and #808's own `ordering_after` drops the edge its parent declares.**
AC-4 reads: "The recently-used model and the keyboard navigation state are
model-side collaborators, not fields on `SimpleEditor` (KC-37-1)." But the
palette toolbar those collaborators must attach to is built entirely inside
`SimpleEditor`: `EditWindow.makeElements()` (`src/jls/edit/SimpleEditor.java:2306-2333`,
a private inner-class method) constructs the `JPanel`/`JButton` tree from
`Palette.entries(group)` directly on the outer 5,852-line class (`wc -l
src/jls/edit/SimpleEditor.java` → 5852). The sanctioned seam for pulling
interaction/focus state out of that class is #316 (FEAT-008)'s TASK-0020
("the nine-state machine becomes a class... no drawing calls in its
transitions"), and #316's own body states plainly that TASK-0020 is **not
filed** at its evidence commit, and the feature itself is `blocked_by: [317,
337]`, both open. #808's parent, #594, lists `ordering_after: ["#592
FEAT-C37-1", "#316 FEAT-008 / #84 (SimpleEditor decomposition — hard
boundary)"]` and calls it explicitly a hard gate: "Nothing here lands in the
god class; if #316 stalls, this feature waits (KC-37-1)." #808's own machine
block, by contrast, carries only `ordering_after: [TASK-C594-1]` (#807) —
the #316 edge that its parent treats as load-bearing is simply absent from
the task that actually has to satisfy KC-37-1. This is the same defect
pattern already found in sibling task #809 in this fleet (see
`issue-reviews/issue-0809.adversarial.md` finding 1): a task inherits a
hard architectural gate in prose but not in its own ordering metadata,
so nothing stops it from being picked up before the gate clears.
**Recommendation:** add #316 (or its TASK-0020, once filed) to
`ordering_after`, and split AC-4 explicitly: the *data* half (the
recently-used list itself) can plausibly be a headless collaborator today
(see finding 6, solid part), but the *keyboard focus/navigation* half lives
in code that #316 has not yet extracted, and the issue should say which
half is blocked rather than asserting both are achievable now.

**2. AC-5's "a scored GAP row in #592's catalog" points at an artifact that does not exist in this checkout, and #592's own comments say the palette rows aren't scoping-ready either.**
`find docs -iname "*parity*" -o -iname "*ergonom*" -o -iname "*catalog*"`
returns nothing; #592 (the catalog issue) is **open**, with no `docs/`
deliverable published. Its two comments (2026-08-04) — the deduplication
pass against #596 — flag exactly the gap that would swallow #808's rows: no
"owning feature" column exists yet on any catalog row, so a
recently-used/keyboard-palette-nav GAP row cannot presently be distinguished
from #596's forever-bucket rows even once the catalog is written. AC-5 asks
#808's tests to "correspond to a scored GAP row in #592's catalog" — a
requirement that cannot be checked today because the row, the score, and
the column that would disambiguate the row from a different feature's claim
all remain unwritten. **Recommendation:** either block #808 on #592
publishing the catalog with a scored, feature-attributed row for this
behavior, or drop the catalog-correspondence clause from AC-5 until #592
lands, tracked as a debt item.

**3. AC-3's mandate to route through #75's `Action` layer, with a test banning any parallel accelerator scheme, may be structurally incompatible with what "keyboard palette navigation" actually requires.**
`EditOp` (`src/jls/edit/EditOp.java:31-68`) is a closed, hand-curated
18-member enum of *discrete, context-free, global* operations — Probe,
Watch, Cut, Copy, Rotate CW, etc. — each with exactly one fixed
`KeyStroke` returned from a `switch` in `accelerator()` (`EditOp.java:107-152`),
registered `WHEN_IN_FOCUSED_WINDOW` per #75's own body. Arrow-key movement
among palette buttons is a different shape of interaction: it is
focus-relative (which button gets focus next depends on which one currently
has it), not a single global keystroke → single operation mapping. Swing's
own `JToolBar` already provides arrow-key traversal among its children for
free, through the platform's focus-traversal machinery — a mechanism that
is neither `EditOp` nor a "second focus model" in the sense #75/#808 warn
against, but is also not literally "the same shared `Action` layer." The
issue's title frames the choice as binary ("rather than a second focus
model"), but the actual design space has at least three candidates —
extend `EditOp` (awkward fit for continuous navigation), reuse Swing's
built-in traversal (not `EditOp`, but not a bespoke scheme either), or
build bespoke navigation state anyway and call it "not `EditOp`, but still
not a duplicate" — and AC-3's test ("asserts no parallel accelerator scheme
is introduced") does not say which of these counts as compliant. A
reasonable implementation using ordinary focus-traversal keys for palette
movement could be read as satisfying the spirit while failing a literal
"only `EditOp`-sourced keystrokes" test, or vice versa.
**Recommendation:** state explicitly whether "keyboard palette navigation"
means new `EditOp` constants, reuse of Swing's built-in toolbar traversal,
or a bespoke non-`EditOp` navigation state — and if the latter two, define
what distinguishes "reuses #75's focus policy" from "a second focus model"
precisely enough for a test to check.

**4. "Bounded in size" and "persists across a session" are unquantified and ambiguous, making AC-1 gameable.**
AC-1: "A recently-used set persists across a session, is bounded in size,
and is reachable by both mouse and keyboard." No bound is given — a
one-element cap technically satisfies "bounded in size." "Persists across a
session" is ambiguous between (a) survives only for the lifetime of one
running JLS process, and (b) survives an application restart (which would
be new durable state, something #75 explicitly disclaims for its own
scope: "Durable state: none — no persisted keymap"). The Outcome section's
promise — "the last handful of placed elements sit one gesture away" —
implies real workflow value across a working session, but nothing in AC-1
forces an implementation to deliver more than a same-launch, single-item,
trivially-satisfying list.
**Recommendation:** state a concrete bound (e.g., "at least N and at most M
entries") and state explicitly whether persistence crosses process restarts
(and if so, name the storage mechanism and confirm it is not a `.jls`
format change).

**5. The parent feature's K9 invariant ("no new default-visible chrome") is not restated here, even though AC-1's mouse-reachable recently-used set is the ACs most likely to violate it.**
#594 (the parent feature) states AC-5 explicitly: "K9 holds — no new
default-visible chrome, and the search index's construction does not move
the startup cost ratchet." #808 inherits this feature's scope but its own
acceptance criteria never mention K9 or default-visible chrome. A
recently-used set "reachable by... mouse" (AC-1) most naturally reads as a
new toolbar region or panel, i.e., new default-visible chrome — exactly
what K9 forbids elsewhere in this same feature family (see
`issue-reviews/issue-0312.adversarial.md`, `issue-reviews/issue-0322.adversarial.md`
for how seriously this fleet treats K9 elsewhere). Because #808 doesn't
carry the constraint, an implementation could satisfy every AC #808 states
and still break its parent's K9 criterion, undetected by any test #808
itself commissions.
**Recommendation:** restate K9 (or an explicit design note — e.g. "the
recently-used set lives inside the existing palette area / is opt-in via a
menu toggle, not a new always-visible strip") as an acceptance criterion or
constraint here, not only in the parent.

**6. No "confirmed absent at HEAD" evidence is offered for the central claim that palette keyboard access is currently deficient — unusual for this issue family's normally rigorous citation style.**
`test/jls/ui/PaletteButtonAccessibilityTest.java` (already in the tree, not
new work this issue proposes) asserts "every tool-bar palette button
exposes a non-blank accessible name and is keyboard-focusable" and that
representative buttons are reachable and identifiable by AT — i.e., plain
Tab-to-focus, Space/Enter-to-activate keyboard placement of a palette
element already works today via ordinary Swing button semantics, shipped
under #75. #808 never states what is concretely missing beyond that
baseline (arrow-key micro-navigation instead of Tab? a shorter path to the
palette? navigation into a not-yet-existent recently-used region?). Compare
this to #316 and #75's own bodies, which pin every "absent at HEAD" claim to
a `grep`/line-number citation before proposing work. #808's AC-2
("Palette navigation, selection and placement are completable from the
keyboard alone") could be read as already substantially true today, which
makes it easy to claim trivially satisfied by tests that re-assert existing
behavior rather than deliver the new capability the Outcome section
promises.
**Recommendation:** cite the specific gap in the current keyboard path
(e.g., "N tab-stops to reach the palette from circuit focus," or "no way to
jump directly to a specific palette group/entry by keystroke") the way
sibling issues in this family do.

**7. Cost band (`band_mw: 0.5-1`, inherited from the parent's task table) looks tight against the compound, partially-blocked scope.** One line each on the parts that are solid, so this isn't padding:
- Palette.java/PaletteEntry.java (`src/jls/edit/Palette.java`,
  `src/jls/edit/PaletteEntry.java`) already demonstrate that a
  headless, `SimpleEditor`-free model class is achievable for palette
  *data* today — a real, working precedent for at least the recently-used
  list's data half of AC-4.
- `EditOp`'s label/accelerator split (`src/jls/edit/EditOp.java`) is a
  genuinely well-tested, reusable substrate (`EditActionMatrixTest`) for
  whatever discrete recently-used commands do turn out to fit its shape
  (e.g., "place the most-recent element").
- Sequencing after TASK-C594-1 (#807, the search index task) is sound in
  isolation: building the registry-driven search index first, then
  layering recency and navigation on top, is a reasonable order even
  though the #316 edge (finding 1) is missing from the same list.

## Summary

The issue is coherent about *what* it wants (a bounded MRU list plus
keyboard-complete palette interaction, without inventing a second focus
model) but two of its five acceptance criteria point at artifacts that do
not exist yet in this repository — an unfiled extraction task (#316's
TASK-0020) and an unpublished, unscored catalog (#592) — without carrying
the ordering edges that would block premature work, and a third (AC-3) sets
a test bar ("no parallel accelerator scheme") against a design question the
issue never actually resolves. Combined with an unquantified "bounded in
size" and a K9 constraint silently dropped from the parent, the issue as
written can be marked done by an implementation that is bounded-but-trivial,
routes through a debatable interpretation of "the Action layer," and adds
chrome its own parent feature forbids.
