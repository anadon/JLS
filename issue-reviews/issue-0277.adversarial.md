# Issue #277: Registry-driven dispatch: element, palette, and exporter consumers read the boot ExtensionRegistry snapshot instead of their static tables
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The research question, the four named dispatch sites, and most of the
acceptance predicates are sound and verified against the current tree. The
issue's fatal problem is not its content but its **currency**: the
machine-readable `blocked_by: []` in its own body is contradicted by its own
comment thread, the accessor it plans to build in §8 step 1 is claimed by a
same-day comment to belong to a different issue (#403) — and #403's own body,
fetched independently, contradicts *that* comment too, requiring a second
correcting comment on #403 itself. A contributor reading only #277's body
would start building something another issue's comment says they must not
build, guided by a `blocked_by: []` its own comment says is wrong. That is a
process defect the issue as currently constituted does not resolve.

## Findings, most severe first

### 1. [High] `blocked_by: []` and §8 step 1 are stale, and the correction is inconsistent across three documents

The body ships with:
```yaml
blocked_by: []          # boot assembly (PR #272) and runtime (#220) already landed
```
and §8 (Method) step 1: *"Decide and implement the snapshot accessor (§7.4),
keeping `jls.module` AWT-free."*

A comment on #277 (2026-08-08T17:45:17Z, the issue's only comment) supersedes
both: *"`blocked_by: []` is wrong — #403 must land first"* and strikes §8
step 1, assigning the accessor to #403. I fetched #403 independently to
verify. Its **body** (not yet edited) says the opposite:
```yaml
blocked_by: []         # gates on nothing: ... This is consumption, not construction.
blocks: []              # #324 waits on the FEATURE, not on this task alone
```
and states in §12, bolded: *"Its machine block carries `blocked_by: []` and
`blocks: []`, so it is not an ordering prerequisite in either direction; it
is a sibling."* A second comment, posted on **#403** (not #277) eleven
minutes later at 17:44:47Z, then argues #403's own body is self-contradictory
and corrects #403's `blocks` field to `[277]` — but that correction lives on
#403, and nothing on #277 reflects it beyond the first comment's assertion.
Net state: the issue body, the issue's comment, and the blocking issue's body
disagree with each other, and reconciling them requires reading four
documents across two issues. A contributor who reads #277 top-to-bottom and
stops at its single comment does not know whether #403's body has since been
corrected without a separate fetch of #403. **Recommendation:** before this
issue is picked up, either edit the `blocked_by` field in #277's body
directly (comments are not a reliable channel for machine-read dependency
metadata) or close the loop with a comment on #277 that explicitly confirms
#403's body has been corrected and cites the correcting comment's URL.

### 2. [Medium] The accessor this issue depends on already exists at the layer below what's being disputed

Independent of the ownership dispute: `ModuleRuntime.extensionRegistry()`
(`src/jls/module/ModuleRuntime.java:305`) already returns the populated,
post-register snapshot, and `JlsModules.boot()` (`src/jls/boot/JlsModules.java:83-87`)
already returns that `ModuleRuntime`. The only actual gap is that
`src/jls/JLS.java:60` discards it: `JlsModules.boot();` with no assignment —
confirmed by reading the file. So "the accessor" that #403's comment claims
doesn't exist is, at the `ModuleRuntime` level, already built; what's
missing is a two-to-five-line change (store the returned runtime somewhere
statically reachable, e.g. a field on `JlsModules`) — not the large
`JlsModules.runtime()` + `bootedOrBuiltIn()` + named-exception +
determinism-apparatus bundle #403 packages it with. Treating "the accessor"
as a large blocking prerequisite conflates a trivial wiring fix with #403's
much larger and legitimately separable scope (op-observer fan-out, the three
pending catalog rows, determinism tests). **Recommendation:** if #277 is
kept blocked on #403, scope that dependency explicitly to "the
`JLS.java:60` assignment + a reachable accessor," not to #403's full DoD —
otherwise #277 is de facto blocked on op-observer fan-out and catalog-row
housekeeping that have nothing to do with its own four dispatch sites.

### 3. [Medium] P2's grep criterion is gameable; P3's coverage across four sites is underspecified

P2: *"the same grep shows consumer call sites in `jls.edit`/`jls.elem`/`jls.hdl`
(or an equivalent accessor)"* is a textual/structural check. A change could
add a no-op call to `ExtensionRegistry.contributions(...)` at each of the
four sites — satisfying the grep — while dispatch logic still resolves off
the static tables underneath. The issue is partly self-aware of this (§11:
*"Tests that call the static tables directly would stay green even if
dispatch were mis-wired — the P3 test must go through the consumer-visible
path"*), but P3 and the DoD both describe **one** new "consumer-visibility
test" ("*a* test that contributes one extra well-typed entry... observes it
in consumer-visible dispatch (e.g. palette row present / emitter selectable)"),
with "e.g." suggesting any single example — palette **or** emitter —
suffices. Nothing in §5/§14 requires an independent behavioral proof for
each of the four named sites (`Circuit.java:918` load-path resolution,
`Palette.java:218`, the `SimpleEditor.java` toolbar, and the
`JLSStart.java` emitter ternary). A change could wire three of the four
sites for real and leave one on the static table with only the grep-based P2
and a single-path P3 both green. **Recommendation:** require one
consumer-visibility assertion per dispatch site (four, not one), or at
minimum name in the DoD which single site the required test must cover and
require the PR description to state how the other three were verified.

### 4. [Low] Keyed/grouped lookup structures are hand-waved, with a latent performance/behavior trap

`Circuit.java:918` currently does an O(1) map lookup (`ElementRegistry.forTag`)
potentially thousands of times when loading a large circuit; `SimpleEditor.java:2312-2321`
iterates `Palette.groups()` then `Palette.entries(group)` — grouped
structures, not the flat list `ExtensionRegistry.contributions()` returns.
§7.10 acknowledges consumers need "the grouped/keyed lookup structures
consumers use today" derived from the flat contribution list, but never says
whether that derivation happens once (cached at/after boot) or is rebuilt
per call. A naive per-`Circuit.load()` linear scan over the contribution
list to resolve each element's tag would be a silent complexity regression
(O(elements × types) instead of O(elements)) that the golden suite — which
checks output bytes, not lookup cost — would not catch, and no predicate in
§5 or the DoD would catch it either. `PaletteEntry` does carry a
`Palette.Group` field (`src/jls/edit/PaletteEntry.java:32`), so the grouped
UI is reconstructible from the flat list, but reconstructing it per-call
(every menu rebuild) versus once is left to implementer judgment.
**Recommendation:** add an explicit acceptance note that any derived lookup
structure is built once from the snapshot (matching §7.9's "snapshot is
immutable after boot" framing) and reused, not rebuilt per load/per repaint.

### 5. [Low] Doc/code drift adjacent to this issue's own recommended default

`docs/extension-points.md`'s "Built-in contributions" section claims
`collab.op-observer` already carries *"the editor-side `OpSink` that applies
and records ops today"* — but `CollabModule.register()`
(`src/jls/boot/CollabModule.java:37-41`) is an empty method body; nothing is
contributed. This doesn't block #277 (whose own Open Question recommends
wiring that seam's read path over an empty list, which matches the code, not
the doc), but a contributor skimming the doc for "what's already wired"
would be misled about this specific seam. Not #277's defect to fix, but
worth a one-line flag since #277's Open Question touches the same seam.

## What's solid (unchanged, verified)

- **P1 and the O2 dispatch grep both hold on the current tree.**
  `grep -rn "contributions(" src/ | grep -v "src/jls/module/\|src/jls/boot/"`
  returns nothing beyond test files — re-run independently, same result as
  the issue claims at `29afb26`.
- **All four cited dispatch-site line numbers are current and accurate**:
  `Circuit.java:918` (`ElementType descriptor = ElementRegistry.forTag(elementType);`),
  `Palette.java:218` (`ElementType type = ElementRegistry.forTag(tag);`),
  `SimpleEditor.java:2312` (`for (Palette.Group group : Palette.groups())`),
  `JLSStart.java:382-385` (the `.v`/`.vhd` emitter ternary) — none have
  drifted.
- **The scope boundary (four named sites, no touch of the static tables
  themselves) is concrete and testable**, and the "no new
  `config/spotbugs-exclude.xml` entries" / "golden suite green unmodified"
  DoD items give a hard, ungameable backstop against silently loosening
  pins.
- **Contribution order determinism is not actually an open risk the way
  #403's comment implies**: `JlsModulesBootTest`
  (`test/jls/JlsModulesBootTest.java:33-37, 64-73`) already pins the
  four-module boot order identically across 25 shuffled input permutations
  with a `Random(220)` seed — the ordering mechanism this issue's dispatch
  will read is already deterministic and already tested, even though no
  *behavioral* golden depends on it yet.
- **§11's own threat-to-validity entry anticipating the P2/P3 gaming risk**
  (finding 3 above) shows the author already saw part of this problem;
  the issue is transparent about known risk rather than hiding it.

## Note on scope of this review

This review evaluates #277 as written plus its directly cited/linked
issues (#403, #223, #272) and the checked-out source at
`/home/user/JLS` (HEAD `5311625`, descending from the `29afb26` the issue
cites). It does not evaluate #403, #223, or #212 on their own merits beyond
what's needed to check #277's claims about them.
