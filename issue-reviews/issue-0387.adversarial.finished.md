# Issue #387: TASK-0058: the strongest driver wins regardless of file order, and an open-drain bus without a pull-up floats to Z instead of reading zero
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The observational core (O1/O2/O3) is real and independently reproducible against
the current tree: `src/jls/elem/WireNet.java:443-485` matches the quoted code
and line numbers almost exactly, and `Output.java:19` / `WireNet.java:29-30`
confirm the "one boolean" driver model. That part of the issue is solid. But
the issue is not safe to execute as written: its own thread already contains
a same-day owner comment that refutes a load-bearing hypothesis (H3) and
mandates a `FORMAT` version bump the body's Definition of Done explicitly
forbids, the headline acceptance test (P3) cannot be built from the elements
the issue itself proposes to ship, and the "one missing edge" the body
advertises is actually a two-hop chain of unlanded prerequisite work.

## Findings, most severe first

**1. [Critical] The issue's own Definition of Done is already contradicted by its own comment thread, uncorrected in the body.**
The DoD lists "No format version bump; `Circuit.FORMAT_VERSION` unmoved" and
§7.7 states "No new saved attributes on existing elements." The owner's
2026-08-08 comment on this very issue rules the opposite: `DriverKind`
"becomes a saved, must-understand attribute on the driving element, and this
task takes the `FORMAT` version bump knowingly," and explicitly lists §7.7,
§7.12 item 1, and the "No format version bump" DoD line as **superseded**.
The issue body was not edited to match. Anyone implementing straight from
the body ships a design the tracker itself has already disowned.
Recommendation: edit §7.1/§7.7/§7.12 and the DoD in the issue body before
work starts, rather than leaving the correction only in a comment a reader
could miss.

**2. [Critical] P3 — the test that proves the headline claim — cannot be constructed from this issue's own Materials.**
P3 requires "three open-drain drivers plus one `PullUp`," and §7.10's
`netKind` formula case-splits on `OPEN_DRAIN ∈ K`. But § Materials &
Apparatus ships exactly two new element types, `PullUp` and `PullDown`, both
at `PULL` strength — no `OPEN_DRAIN`/`OPEN_SOURCE` driver kind or element
exists anywhere (`grep -rn 'Strength|OPEN_DRAIN|PullUp|PullDown' src/` is
empty), and the one existing tri-state element, `TriState`
(`src/jls/elem/TriState.java`), is a symmetric enable/disable buffer that
drives either 0 or 1 when active — it is not open-drain (which can only
actively pull one rail and floats otherwise). As written, P3's fixture does
not exist and cannot be built with what this task delivers. This is also
independently caught in the issue's own comment thread ("your P3 requires
… under H3 as written, P3's fixture cannot be constructed").
Recommendation: either add an `OPEN_DRAIN`-kind element/attribute to this
task's scope, or rewrite P3 to use only the elements actually shipped.

**3. [High] The "one missing edge" framing understates a two-hop unlanded dependency chain.**
The body presents the situation as a single named-but-unfiled edge:
"TASK-0057 … is being filed concurrently; the edge is `blocked_by:
[TASK-0057]`." TASK-0057 itself (now #391) states plainly that its fold is
"inexpressible" without a *further* prerequisite, TASK-0056 (the per-bit
value type), which at #391's own filing time was itself only "being filed
concurrently." The corrective comment on #387 confirms the real chain is
"`#881 → #391 → this issue`" — i.e. two full unlanded tasks stand between
this issue and buildable ground, not the one edge the Abstract and §12
imply. § Materials & Apparatus only says "Requires TASK-0057's fold,"
never surfacing that TASK-0057 is itself blocked. A reader who stops at
this issue's body will underestimate the critical path by one full task.
Recommendation: name the full chain (`#881 → #391 → #387`) in Status &
Dependencies, not just the immediate edge.

**4. [High] H2's falsification condition is real but the Method checklist tests a weaker proxy than it claims.**
H2 says redefining `isTriState()` as `kind != PUSH_PULL` is safe unless a
call site needs `PullUp` to be *unattachable*; § Threats to Validity names
exactly this risk for the four `SimpleEditor` connection checks among the
49 call sites (verified: `grep -rn 'isTriState()' src/ | wc -l` → 49). But
§8's only corresponding step is "verify all 49 call sites compile and
behave unchanged" — compilation is guaranteed by the signature staying
`boolean`, and "behave unchanged" is never turned into a per-site assertion
or test. A `PullUp` could be silently permitted (or refused) at a
connection boundary that used to gate on `triState` and the build would
still go green, because none of P1-P8 exercises editor-side connection
legality at all. Recommendation: add an explicit editor-level test (or
enumerate the four `SimpleEditor` sites with a targeted assertion each)
rather than relying on "compiles."

**5. [Medium] §7.10's net-kind formula promises capability the shipped Materials don't provide (independent of the comment's H3 refutation).**
Even reading the issue body in isolation, before the corrective comment: it
defines `WAND`/`WOR` net kinds from `OPEN_DRAIN`/`OPEN_SOURCE` membership in
§7.10, lists `Output.DriverKind { PUSH_PULL, OPEN_DRAIN, OPEN_SOURCE, PULL }`
as a public interface in §7.4, yet supplies no path (element, attribute, or
otherwise) for a user or file to ever produce an `OPEN_DRAIN`/`OPEN_SOURCE`
driver. This is a scope hole in the contract itself, separate from the
saved-attribute question the comment raises. Recommendation: either scope
those two enum values out of this task (ship `PUSH_PULL`/`PULL` only) or
add the open-drain/open-source-capable element(s) the contract already
assumes exist.

**6. [Medium] The Abstract's headline claim is narrower than it reads, and the fine print undercuts it.**
"Done means … the same bus without the pull-up floats to `Z` rather than to
`0`" reads as an end-to-end simulator claim. §7.11/§10 narrow it
considerably: "Elements that read the net still coerce [HiZ] per O5 … that
coercion is FEAT-026's, not this task's." O5 itself documents that "nearly
every element's `react` treats a null (HiZ) input as zero"
(confirmed at `docs/simulation-semantics.md`, the quoted §2 text). So a
student watching an `OutputPin` fed by the floating bus — the ordinary way
anyone would observe this — will still see 0 after this task ships; only a
raw probe on the net itself sees `Z`. The Abstract should say so plainly
instead of relying on readers to reach §7.11/Falsification §10 for the
caveat. Recommendation: qualify the Abstract's claim to "the net's own
resolved value is Z; downstream logic still coerces to 0 until FEAT-026."

**7. [Medium] The "zero format versions" reasoning is reused across two different questions and that conflation is what produced Finding 1.**
§7.1/§7.12 correctly argue new element *tags* cost zero format versions
because an unknown tag is already a hard error. The body then applies the
same "zero versions" conclusion to the entirely different question of
adding a new saved *attribute value* (`DriverKind`) to an *existing*,
already-understood tag — where the unknown-tag defense does not apply, as
the comment thread had to point out. The reasoning gap is in the body, not
only the fact pattern. Recommendation: treat "new tag" and "new attribute
on an old tag" as separate versioning questions in §7.12 going forward.

**8. [Medium] The `TriProp` decision is a named risk with no prediction that tests it.**
§ Threats to Validity correctly flags that if `PullUp` implements
`TriProp`, `WireNet`'s un-tri-stating walk (`WireNet.setTriState`,
confirmed at `src/jls/elem/WireNet.java` — the walk calls
`tel.setTriState(which)` on every attached `TriProp` element when a net's
tri-state-ness changes) would silently disable the pull element when the
net loses its last tri-state driver. This is called out as "Blocks
execution" in Open Questions, but none of P1-P8 or the Method checklist
actually constructs "remove the last TriProp driver from a net with an
attached PullUp and confirm it isn't disabled." DoD line 1 ("every
post-fix prediction verified") can pass while this named risk goes
untested. Recommendation: add a P-numbered prediction for this exact
scenario, not just an Open Question checkbox.

**9. [Medium] Cost is stale relative to the same-day correction, and the issue doesn't update its own estimate.**
The comment states outright: "a saved attribute, its loader and refusal
paths, and a `FORMAT` bump were not in this issue's 2 wk row." That 2-week
figure comes from #341's task table and this issue does not carry or
revise a Cost section of its own, so the scope growth documented in the
same thread as this issue is invisible to anyone reading only #387.
Recommendation: this issue should carry its own (even rough) cost estimate
and update it when the scope changes, rather than relying entirely on the
parent feature issue to reconcile.

**10. [Low] The evidence-freshness check silently excludes the documents most of the doc-line citations depend on.**
The "IDENTICAL" reproducibility gate in §2 is `git diff --quiet
2d0ca9d…HEAD -- src test pom.xml .github/workflows` — it does not cover
`docs/`. Yet O5's evidence is line-pinned quotes from
`docs/simulation-semantics.md:44-49,60-66`. A `docs/` edit between the
evidence commit and a picker-upper's checkout would not trip the freshness
check even though it could invalidate O5's citations. Recommendation:
include `docs` in the identity check, or drop line-number pinning for docs
citations in favor of anchor-text search.

## What's solid (brief)

- O1/O2 (the file-order-flips-the-answer demonstration and its code cause)
  are concrete, reproducible, and verified line-for-line against the current
  `WireNet.java`.
- H4/P8 (byte-identical goldens for no-diversity circuits, any mover treated
  as a bug rather than re-blessed) is a well-designed, hard-to-game
  acceptance criterion.
- O6/O8's registration-surface and coverage-floor citations check out
  against `pom.xml:475-491` and the current registry/permits counts (24
  `LogicElement` permits, 35 `ElementRegistry` entries, 49 `isTriState()`
  call sites all confirmed).

## Verdict rationale

The direction (a strength lattice inside an order-independent fold) is
architecturally sound and the observational grounding is real. But the
issue is not currently safe to hand to an implementer: its own DoD
contradicts a same-day ratified correction on the same thread (Finding 1),
its flagship acceptance test is unbuildable from its own shipped scope
(Finding 2), and its dependency framing hides a two-task-deep unlanded
chain (Finding 3). These are fixable by editing the body to absorb the
comment's corrections and closing the P3/TriProp test gaps — hence
**needs-rework** rather than should-not-proceed.
