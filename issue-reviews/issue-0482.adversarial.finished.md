# Issue #482: TASK-0105: the palette gains a view dimension, so a registered element type stops meaning a first-year toolbar button — enforced by a currently-passing test that must be rewritten
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The mechanical plan — add `view` to `PaletteEntry`/`Palette.Group`, rewrite
`paletteIsTotalOverTheElementRegistry` as a two-conjunct check, convert the
toolbar builder to a view-scoped accessor, reconcile `ElementVocabulary` as
registry-minus-deny-list — is real, well grounded in the current tree, and
every line-numbered citation I checked (`Palette.java` L214-235,
`PaletteContractTest.java` L44-66, `SimpleEditor.java` L2312,
`ElementVocabulary.java`, `ElementRegistry.java`'s 35-entry list, the 32
`Palette` rows) reproduces exactly as quoted. That part is sound. The
problems are in what the issue asks a future implementer to depend on and
sign off against, not in the code diff itself.

## Findings, most severe first

**1. A mandatory completion-criterion action item points at a file that does not exist anywhere in the repository, and never existed on `master`.**
Section 8's Method and section 14's Definition of Done both require:
`"Update docs/extension-points.md's gui.palette-contributor contract note
and docs/virtual-hardware-parity.md's K9 clause. K9 stops being prose."`
`docs/virtual-hardware-parity.md` is not present at HEAD (`find` over the
whole checkout returns nothing) and, per the repo's own commit
`742da745c6e5eac3da161ef6d4a1fee9ac2e38ee` ("docs: remove the planning
corpus... Removed: docs/plan/ (206 files) and the three docs/ deliverables,
**none of which existed on master**"), it never existed on `master` at all
— it lived only on a working branch the maintainer explicitly ruled "will
not be merged and will be deleted" (issue #485, the sole surviving record).
An implementer following this issue's own completion checklist literally
cannot check that box without first inventing an un-scoped new document.
**Recommendation:** strike the `docs/virtual-hardware-parity.md` references
and redirect the K9-ratchet documentation obligation to a file that
actually exists (e.g. `ARCHITECTURE.md`'s "Recorded decisions" section, or
a new normative doc filed and landed *before* this task, not invented mid-task).

**2. The issue's central motivating narrative (K9 "restated... to gate visibility rather than existence", decision D-A10) is sourced entirely from documents that are permanently unrecoverable.**
O1/O2 and the Background section cite `docs/plan/evidence/BRIEF.md` section
12 and `docs/plan/evidence/analog-determination.md` section 2.8, both
"landed `3a81a4a7...`, not present at `2d0ca9d`". I confirmed `3a81a4a` is a
real commit but is **not an ancestor of current HEAD** — it belongs to the
same dying branch removed by `742da74`. Issue #485 (filed specifically to
rescue this material before deletion) inlines D1-D16 in full but contains
no D-A10 and no analog-specific ruling; `docs/plan/evidence/analog-determination.md`'s
content is simply gone. The 22-type/69%-growth arithmetic this task exists
to prevent is independently re-derivable from `ElementRegistry`/`Palette`
today (I verified 35/32, and 22 additional types against 32 rows is indeed
≈69%), so that number survives on its own merits — but the claim that a
specific maintainer-ratified decision "D-A10" backs the visibility-not-existence
design is not independently checkable by any future reviewer. **Recommendation:**
either re-derive and inline the actual analog-visibility rationale here (as
#485 did for D1-D16), or drop the D-A10 citation and argue the design on
its own merits.

**3. The proposed public interface silently assumes a class→record conversion the issue never budgets or flags.**
Section 7.4 specifies `public record PaletteEntry(ElementType type,
Palette.Group group, String view, String iconName, String fallbackText,
String tooltip, String helpTopic) { }`. The actual `PaletteEntry`
(`src/jls/edit/PaletteEntry.java`) is **not a record** — it is a plain
`final class` with private fields, a package-private constructor that
throws `IllegalArgumentException` on blank/null components, and
hand-written accessor methods. Section 6 ("Materials & Apparatus") lists
`PaletteEntry` as pre-existing material to extend, not to rewrite. Adding
`view` to the existing class is a one-line change to the constructor and
one accessor; converting it to a canonical record (losing the custom
validation constructor's current shape, or requiring a compact constructor
rewrite) is a different, larger change with its own review surface. The
"roughly two days of the two weeks" effort estimate and the threat list
(T1-T7) never mention this. **Recommendation:** either fix section 7.4 to
match the actual class shape, or explicitly scope the record conversion
and its constructor-validation migration as part of the two days.

**4. The K9 ratchet's headline assertions (P7, P8) are unfalsifiable at landing time.**
Section 7.4 declares the `"analog"` and `"breadboard"` views **empty** on
purpose ("Declaring them here and leaving them empty is deliberate"). But
P7 (`analogGroupIsAbsentUnlessTheContextIsAnAnalogSubcircuit`) and P8
(`noPreferenceCanMakeTheAnalogGroupVisibleInTheDefaultView`) are exactly
the tests meant to prove the pedagogy floor holds — and with zero analog
buttons registered, "no analog button is visible" is true by vacuous
construction regardless of whether the visibility predicate is implemented
correctly. A broken `isVisible` that always returns `false` for `"analog"`
passes P7/P8 identically to a correct model-derived one. The issue is
self-aware of an analogous risk for totality (H1/T1: "the obstacle is a
*passing* test... a mistake makes the build greener rather than redder")
but does not apply the same scrutiny to P7/P8. **Recommendation:** add a
synthetic non-empty test-only view (or a stub analog element type used only
in test scope) so P7/P8 exercise real hide/reveal behavior rather than an
empty set.

**5. A cross-referenced coordination note is already stale.** Threat T6 and
the `related` block point at #474 (TASK-0049) as the task whose landing
will move the default-view count from 32 to 33, "the two tasks must agree
on the number." #474 was **closed as a duplicate** on 2026-08-08 — before
this review and plausibly before pickup of #482. Nothing in #482 will
surface that automatically; whoever picks this up must manually re-walk
every `related`/`blocks`/`blocked_by` entry's live state rather than trust
the YAML snapshot, since this large batch of generated issues is not kept
in sync with real-time GitHub state.

**6. Real, multi-hop blocking chain understated relative to the "two days" framing.**
`blocked_by: [383]`, and #383 is itself open and `blocked_by: [319, 337]`
(also open), each with its own full-size formal-spec issue. The Method
checklist's first line is "Confirm #383 has landed and read its view
vocabulary" — but #383 cannot land until #319 and #337 land first. The
issue's effort framing ("roughly two days of the two weeks is the palette
change itself") describes only the in-task work and gives no visibility
into this critical-path wait, which could dominate calendar time to ship
FEAT-049's close-out that #482 gates.

**7. Scope bundling of a security-relevant surface with a GUI refactor.**
The `ElementVocabulary` reconciliation (network-facing element allowlist,
explicitly flagged "security-relevant" in section 7.1) is required to land
"in the same change" as the palette-view work, purely so the three-way
cross-check stays green. That's a reasonable technical constraint, but it
means a reviewer focused on the visible toolbar diff is also the one who
must catch a silent widening of what a network peer may instantiate (the
exact `TestGen` hazard O5 names). Recommend flagging the
`ElementVocabulary`/`ElementVocabularyTest` hunk for separate, explicit
security sign-off within the PR review, since it won't be visually obvious
alongside 30+ lines of palette/toolbar diff.

## What's solid (no further comment needed)

- O1-O5's code citations (line numbers, method bodies, counts) all
  reproduce exactly against HEAD.
- The default-view-byte-identical design (H2/P6) is a sound, low-risk
  compatibility invariant and is provable by construction (32 existing
  rows all become `"schematic"`).
- Reusing #383's view vocabulary instead of minting a second one (H5) is
  the right call and is explicitly guarded (P11, Threat T2).
- The registry-minus-deny-list reconciliation, scoped to exactly
  `{TestGen}` at landing (H4/P9), is a correct, narrow fix to a real gap
  (O5's 34-vs-35 mismatch).
