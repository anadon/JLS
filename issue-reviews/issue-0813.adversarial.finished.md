# Issue #813: TASK-C596-3: viewport and zoom polish lands last, and the bucket closes with an estimate-versus-actual record per item instead of running forever
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

TASK-C596-3 is the third and closing item of #596 (FEAT-C37-5), which itself
is gated by #592's parity catalog and by the #316/#84 `SimpleEditor`
decomposition. As written, the issue omits its two most consequential
dependencies from its own `ordering_after` field, asks for work to be
verified against a document that does not exist, and sets an acceptance
criterion (AC-4, "none lands inside `SimpleEditor`") that the very
subsystem it extends currently violates wholesale. Several of its named
deliverables (zoom-to-fit, pan, scroll) already shipped under #74 and are
already wired into `SimpleEditor`, which makes the issue's framing of "the
third item lands" materially misleading about what remains to be built.

## Findings, most severe first

### 1. Missing hard dependency: #316 (SimpleEditor decomposition) is not in `ordering_after`, and AC-4 is very likely infeasible as scoped

The issue's YAML front matter lists only `ordering_after: [TASK-C596-2]`
(#812). But #596 — this task's own parent feature — states in its body:
"no item here lands inside `SimpleEditor`; rendering and viewport
behaviour go into the decomposed collaborators #316 produces. **If #316
stalls, this feature waits** rather than growing the god class (KC-37-1)."
#596's own maintainer comment restates it even more explicitly as a "shared
hard gate, not duplication": "KC-37-1 — nothing lands inside `SimpleEditor`
… If #316 stalls, this feature waits."

#316 is open, unstarted, and explicitly blocked: its own body states "The
mouse machine is not extracted" (`SimpleEditor.java` is 5,852 lines per
`wc -l` here today) and lists `TASK-0020` ("not filed") as the extraction
task, itself blocked by #337 and #317. There is no decomposed collaborator
for rendering/viewport work to land in yet.

Verified against the actual code: `src/jls/edit/Viewport.java` (449 lines,
package-private, pure — good, this part is already outside
`SimpleEditor`) is real and does hold the zoom math. But every piece of
*editor wiring* that consumes it — `zoomIn()`/`zoomOut()`/`zoomToFit()`
(`SimpleEditor.java:629-671`), the `EditWindow` inner class that owns the
`Viewport` instance (`private class EditWindow … ` at
`SimpleEditor.java:1121`, `private final Viewport viewport = new
Viewport();` at `:1133`), the mouse-wheel listener, the keyboard zoom
accelerators (`:1499-1554`), and the pan-drag state (`:2585`) — all live
**inside** `SimpleEditor.java`. A "zoom-to-selection" feature needs
selection bounds, which live in the same `EditWindow` state machine
(`selecting`/`selected` states). There is no plausible way to add
zoom-to-selection or "pan behaviour that matches what a switcher expects"
without touching `EditWindow`/`SimpleEditor`, short of first doing #316's
extraction — which this task does not budget, schedule, or even
acknowledge as a precondition.

**Recommendation:** add `316` (or its relevant `TASK-0020` slice) to
`ordering_after`, or explicitly state why this task believes it can be
exempted from the hard gate #596 states applies to every item in this
bucket. As written, a literal-minded implementer has two bad options: (a)
violate AC-4 by extending `EditWindow`, the same way #74's work already
did, or (b) satisfy AC-4 only by cosmetic relocation (new methods added to
`Viewport.java` while the call sites and event wiring stay in
`SimpleEditor`), which is a letter-of-the-law dodge, not a real fix to
KC-37-1's stated concern.

### 2. AC-3's "score order" and "process violation" test point at a catalog that does not exist yet

AC-3 requires: "The order actually followed is recorded against #592's
score order, and an item worked with no catalog row is recorded as a
process violation rather than absorbed." #592 (FEAT-C37-1) is open, and
its own AC-1 is "The catalog is published under `docs/`…" — that
publication is #592's deliverable, not a precondition it has already met.
A repo-wide search (`find . -iname "*592*"`, `grep -rl "band_mw\|catalog-score" docs/`)
turns up nothing: no such catalog exists anywhere under `docs/` today.

So AC-1 ("Viewport/zoom polish items land or are refused by name with a
reason") and AC-3 both presuppose a scored, published catalog that #813
does not list as a blocker. Either #813 cannot start (per #596's ordering
note, which names #592 in its own `ordering_after`) until #592 lands, or
the "score order" language in AC-1/AC-3 is unenforceable filler that an
implementer can satisfy by writing down any order and calling it
consistent with a catalog nobody can check against. This is the same
missing-dependency problem as Finding 1, applied to #592 instead of #316.

### 3. Two of the three named viewport items already shipped, under #74, inside `SimpleEditor` — the issue misrepresents how much is left

The issue frames viewport/zoom polish as work "landing" for the first
time ("lands in catalog-score order over #74's `Viewport`"). But #74 is
**closed** ("completed", 2026-07-20), and its shipped scope already
includes: zoom-to-fit (`SimpleEditor.zoomToFit()` →
`ew.zoomFit()`, wired to a "Fit to Circuit" menu item at
`JLSStart.java:1848`), mouse-wheel zoom-at-cursor
(`Viewport.WHEEL_STEP`, wired at `SimpleEditor.java:3915-3916`), a
keyboard zoom ladder with accelerators, and pan-drag (`panStartView` at
`SimpleEditor.java:2585`). `test/jls/ui/EditorZoomTest.java` (a
`@Tag("display")` Layer-2 test) already pins hit-testing and element
movement under a non-identity view transform.

The only named sub-item with no existing implementation is
"zoom-to-selection" — `grep` for `zoomToSelection`/"Zoom to Selection"
across `src/jls/edit/SimpleEditor.java`, `src/jls/JLSStart.java`, and
`test/jls/ui/EditorZoomTest.java` returns nothing. `Viewport.fit(Rectangle,
int, int)` is already generic over the bounds rectangle, so the
"new" work is arguably a small glue change (pass selection bounds instead
of circuit bounds) — the opposite problem from what the issue's framing
implies (a from-scratch third feature item). The band estimate (1-1.5 mw,
inherited from #596's PF-5 "2-4 mw" split three ways) should be checked
against this — it may be scoped for work that's already 2/3 done, which
either means it's overfunded or the estimate was set without checking
what #74 actually shipped.

**Recommendation:** state explicitly which of {zoom-to-selection,
zoom-to-fit, scroll, pan} are net-new versus already-shipped-under-#74,
and size/estimate only the delta. As written, "or are refused by name
with a reason" in AC-1 could be trivially satisfied by writing up
zoom-to-fit/pan/scroll as "already landed under #74" with no new work
done, which is a legitimate reading but one the issue's own framing
("lands... over #74's Viewport", implying #74 is a foundation for new
work, not a place three-quarters of the ask already lives) does not
prepare a reader for.

### 4. AC-5 ("K9 holds") points at cost ratchets that do not exist in this repository

AC-5: "K9 holds: the per-edit and startup cost ratchets are unmoved by the
rendering and viewport changes." K9 is defined only at the capstone level
(#521 AC-5: "no new default-visible complexity; startup and per-edit cost
ratchets hold"). A search of the repo (`grep -rln "startup.*cost\|per-edit
cost\|StartupCost\|PerEditCost"` across `test/`, `src/`, `docs/`) finds no
such test, benchmark, or ratchet anywhere in the tree — contrast with real
ratchets that do exist and are named (`HeadlessCoreRatchetTest`,
`NotificationRatchetTest`, `SocketConfinementRatchetTest`,
`DialogCoverageRatchetTest`, the `jls.edit` JaCoCo-floor convention at
`pom.xml:400-418`). As stated, AC-5 cannot be mechanically checked; it can
only be satisfied by assertion in prose, which invites exactly the kind of
unverifiable "we checked, it's fine" close-out the rest of the issue's
family (#91, #316) is otherwise careful to avoid via re-derivable
`git grep`/measured evidence. This is inconsistent with the surrounding
tasks' own rigor.

**Recommendation:** either name the concrete artifact AC-5 checks (a
`#43`-style drag-latency benchmark? a startup-time smoke test?), or drop
the criterion / replace it with something the existing test suite can
actually gate on.

### 5. "A committed record" (AC-2/AC-3) is ambiguous about format and location, unlike the precedent this issue's own family sets

#91 (cited elsewhere in this cluster) is explicit about what its
equivalent record looks like: "a table (run #, date, CI run link or local
log digest, pass/fail, rerun count) posted as a `STATUS:` comment on this
issue." #813's AC-2 says only "a committed record lists every item worked
under #596 with its estimate, its actual, and its disposition" — "committed"
is ambiguous between "git-committed" (a tracked file under `docs/`, durable
and diffable) and "recorded/logged" (e.g. a GitHub issue comment, which is
not part of the repository and isn't covered by `mvn verify` or code
review). Given AC-4 also demands a pinning test "per landed item," and the
record is supposed to be the mechanism that enforces KC-37-2 ("in the
record, not in guidance"), leaving its storage medium unspecified is a
real gap: an implementer could satisfy the letter of AC-2 with a
throwaway PR-description table that nothing re-checks on the next task
pickup — exactly the "silently absorbed" failure mode AC-3 is trying to
rule out for catalog rows.

**Recommendation:** name the artifact location explicitly (e.g. a
`docs/`-tracked table, following the pattern `docs/` already uses for
other adjudicated records like `docs/flatlaf-evaluation-2026-07.md`),
not an issue comment.

### 6. Minor: "closing discipline" framing implies #813 is the last item, but nothing enforces that #596 won't reopen

The title and Outcome claim "the feature ends when the scored set is
exhausted rather than accumulating new wishes," and AC-2 asks for a
disposition per item "so the feature ends." But #813 itself has no
mechanism to close #596 — it is a task, not the feature issue — and #592's
catalog (per Finding 2) doesn't exist yet, so "the scored set" is not
even enumerable at filing time. The closing-discipline framing is
aspirational relative to #592's completion, not something #813 can itself
guarantee. This is a soft finding (framing, not a testable defect) but
worth flagging so the closing PR doesn't overclaim "the bucket is closed"
when in fact #592's catalog was never finished or was incomplete when
#813 shipped.

## What's solid

- The `ordering_after: [TASK-C596-2]` (#812) edge is correct and verified:
  #812 exists, is open, and is indeed the second item in the #596
  sequence with a matching `ordering_after: [TASK-C596-1]`.
- AC-4's *intent* (per-item test that fails pre-change, no
  `SimpleEditor` landings) is the right discipline in principle — it's
  the codebase-grounding of the request, not the goal, that's the problem
  (Finding 1).
- The `Viewport` class this task extends is well-designed for the
  purpose: pure, headless-testable, already generic over an arbitrary
  bounds rectangle (`fit(Rectangle, int, int)`), which is exactly the
  primitive zoom-to-selection needs. Building on it is the right call.
- KC-37-2 (1.5x stop-loss, estimate-vs-actual) is a reasonable, concrete
  anti-scope-creep mechanism in principle, and its presence here (vs. an
  open-ended "polish" task) is a genuine strength relative to how such
  tasks are often filed.

## Verdict rationale

`needs-rework`: the issue is buildable in spirit, and the pieces it
depends on (`Viewport`, #592's intended catalog, #316's intended
decomposition) are individually sound designs — but #813 as filed omits
its two load-bearing dependencies from its own ordering metadata, asks for
verification against a document that doesn't exist, sets a hard
acceptance gate (AC-4) that the code it must extend currently and
pervasively violates, and points one acceptance criterion (AC-5) at
tooling that isn't in the repository. None of these are fatal to the
underlying feature, but starting work on #813 exactly as scoped today
would either stall immediately (correctly, per #596's own stated rule) or
produce a result that games AC-4/AC-5 by paperwork rather than by
substance.
