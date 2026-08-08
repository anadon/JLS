# Issue #546: FEAT-C26-4: one export emits what disability services can emboss — a part-to-whole prose narrative and a tactile-lint-clean SVG sized for swell paper
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is well-scoped in prose and correctly cross-references its capstone
(#507 CAP-26), its data dependency (#542, FEAT-C26-1), its neighbor (#540/#536,
CAP-24 print export), and a closed non-goal (#85, i18n). But three of its four
acceptance criteria rest on machinery the repository does not have and the
issue does not fund, one criterion is self-flagged as unresolved by the
issue's own comment thread and left that way, and the tactile-guideline
sourcing has a licensing question nobody asked. Sound project hygiene, weak
engineering spec.

## Findings, most severe first

### 1. AC-4 (byte-identical SVG across CI platforms) is asserted, not shown feasible, and the one place in the codebase that renders SVG text has the exact failure mode this criterion forbids

The issue states flatly: "Accessible exports are byte-identical across CI
platforms (CAP-26 AC-6 `AccessibleExportDeterminismTest`, per the project
determinism convention)." No such "project determinism convention" document
exists — `grep -rn "determinism convention" docs/ src/ test/` returns nothing.
The nearest real precedent is issue #536 (FEAT-C24-1, the sibling print-SVG
feature), which names the *actual* mechanism required: "achieved by owning
text metrics via a bundled deterministic font path — no OS font fallback in
the render." #546's tactile SVG will need labels/symbol substitutions too
(the acceptance criteria mention "symbol substitutions"), and the renderers
it will almost certainly reuse already draw text with the platform font:
`src/jls/edit/CircuitRenderer.java:201,215` (`gg.getFontMetrics()` /
`gg.drawString(nm, 0, ascent)`) and `src/jls/edit/StateRenderer.java:50,174,343,369,387,402,405,408`
all call `FontMetrics`/`drawString` against whatever `Graphics2D` is passed
in — no bundled font path exists anywhere in the tree today (a repo-wide
search for a bundled/embedded font turns up nothing). #546 does not mention
fonts, text metrics, or the OS-fallback hazard at all, despite requiring the
byte-identical guarantee that #536 explicitly calls out as its hardest
problem ("text metrics is most of it" — #536's own cost note). Either #546
silently assumes #536 lands first and solves this for free (not stated, and
#536 is unordered relative to #546 — `ordering_after` only names FEAT-C26-1),
or #546 has to solve font determinism itself, which its 3-4 mw band does not
appear to price in.

**Recommendation:** Either add an explicit `ordering_after: [FEAT-C24-1]` (or
equivalent) so #546 inherits a solved deterministic-text-rendering path, or
scope AC-4/AC-6 down to "no rendered text in the tactile SVG" (glyphs only,
via path outlines the lint already needs to check anyway) so the determinism
claim doesn't depend on unbuilt font infrastructure.

### 2. The issue's own comment identifies an unresolved shared-exporter-ownership risk and the acceptance criteria were never updated to account for it

The single comment on #546 (2026-08-04, from the repo owner via automation)
states: "Three issues now describe transformations of one exporter. The risk
is not duplicated outcomes — it is one exporter seam specified three times,
with three incompatible styling contracts and no named owner for the seam
itself. Recommended before either band is funded: name one owner for the SVG
export seam... and have #536 and this issue declare themselves *profiles*
over it." That recommendation is dated the same day the issue was filed and
nothing in the issue body reflects it — the acceptance criteria still read as
if #546 owns a free-standing renderer ("a simplified high-contrast SVG").
Filing an issue whose own comment thread flags an un-actioned architectural
risk, without a tracking item for resolving it, means implementation can
start, build a private renderer, and then have to be reworked once the seam
gets a real owner. This is exactly the decay mode the comment warns against.

**Recommendation:** Add a blocking dependency (or at least a tracked
checkbox) on "name the SVG-export-seam owner" before work starts, per the
comment's own recommendation. Don't let the acceptance criteria imply a
private renderer is acceptable when the issue's own audit trail says it
probably isn't.

### 3. "BANA, cited by edition" names no edition, and BANA's tactile-graphics guidelines are copyrighted — the issue doesn't address whether encoding their numeric rules into an open-source lint is permissible

The issue commits to "the recommended default (BANA, cited by edition)" but
never states which edition (BANA has published multiple editions of its
*Guidelines and Standards for Tactile Graphics*, most recently a 2010-era
edition with addenda). Compare this to the issue's sibling decision on WCAG
(CAP-26 OQ-2: "WCAG 2.2 AA... blocks PF-5's filing") which *is* pinned to a
specific version before filing; BANA is not, despite AC text requiring "the
guideline edition cited in the lint's rules" — the issue asserts this will
happen without doing it, and without it, the lint's normative source is
undefined until implementation time. Separately: BANA's guideline documents
are copyrighted publications, not a public standard like WCAG; transcribing
"line widths, spacing, symbol substitutions" rules as numeric constants into
GPL-licensed lint source plausibly requires either a license/permissions
check or reformulating the rules as originally-derived facts (measurements),
neither of which the issue mentions. `SECURITY.md`/`docs/standards-adoption/`
already show this project is careful about exactly this kind of sourcing
question for other standards (see the VPAT section's repeated "Unverified
external fact, check before authoring" callouts) — #546 has no equivalent
caution for BANA.

**Recommendation:** Pin the BANA edition in the issue text now (not "at
filing time"), and add an explicit line to the acceptance criteria requiring
a licensing/permissions check (or an independently-derived rule table) before
BANA rule values are committed to lint source.

### 4. "Narrative ordering passes the guideline checklist test" is underspecified to the point of being gameable

The acceptance criterion never defines what "part-to-whole" ordering means
algorithmically for an arbitrary circuit graph. JLS circuits can have cycles
(feedback loops through registers/latches — normal, documented in
`docs/simulation-semantics.md`), multiple disconnected top-level elements,
and nested subcircuits. A "guideline checklist test" with no stated ordering
algorithm can be satisfied by handling only the trivial cases exercised by
its own fixture (e.g., a single combinational chain) while failing on any
circuit with a cycle or a disconnected component — the class of circuit most
likely to defeat a naive part-to-whole traversal. The issue gives no fixture
requirements (must include a cycle, a subcircuit, a disconnected element) the
way, e.g., #79's `FormatHeaderTest` or #78's totality tests are pinned by
name to a specific decay mode.

**Recommendation:** Name at least the fixture classes the checklist test must
cover (cyclic, subcircuit-nested, multi-component) before calling the
criterion satisfied, the same way FEAT-C26-1 (#542) pins its totality test to
"a new element type that lacks an encoding fails the build" rather than
leaving totality implicit.

### 5. "Compliance is checked, not asserted" overclaims relative to the capstone's own stated honesty bar

#546's outcome section says: "A lint enforces the tactile rules... so
compliance is checked, not asserted." But its own parent, CAP-26 (#507),
states the honest framing explicitly in Risk 1: "Automated checks are
necessary, not sufficient. Screenshot analysis and lints prove guideline
compliance, not usability. The honest system-level claim is
'guideline-compliant and machine-verified'... upgrading it requires
validation with actual CVD and BLV users (Open Question 1)." #546 flattens
that nuance into an unqualified "compliance is checked" — a static SVG lint
cannot verify that a physical swell-paper embosser (heat-sensitive paper,
device-specific calibration, DPI/viewBox scale) actually produces a usable
tactile image; it can only verify the vector geometry meets the numeric
guideline the lint encodes. This is a documentation/framing defect, not a
test defect, but it sets up the closing comment on this issue to overclaim
what got verified, which is precisely the failure mode CAP-26 §3's honesty
rule (and its citation of the BSDL costing precedent) exists to prevent.

**Recommendation:** Restate the outcome as "guideline-compliant and
machine-verified" (CAP-26's own wording), and explicitly note that physical
embosser/disability-services validation is out of scope for this issue and
tracked under CAP-26 Open Question 1, not silently subsumed.

### 6. Cost band looks tight against the comparable sibling, no fixture/registry dependency cost is priced in

Band is 3-4 mw for: a new prose-narrative generator with graph-ordering logic,
a new "simplified high-contrast" SVG render profile, a tactile lint encoding
multiple categories of BANA numeric rules, cross-platform byte-determinism
(finding 1), and consuming #542's registry-keyed state-to-encoding data —
which does not exist yet (#542 is open, unstarted, 3-5 mw itself, and gated
on Theme-seam work). By contrast #536 — arguably a narrower "one output
format, one styling profile, reuse the existing exporter" feature — is
budgeted 4-6 mw with the maintainer's own note that "text metrics is most of
it." #546 does two output formats plus a lint plus a determinism guarantee
for less budget than a narrower neighbor spends on text metrics alone.

**Recommendation:** Re-derive the band once finding 1 (font determinism) and
finding 2 (seam ownership) are resolved; the current 3-4 mw likely does not
survive contact with either.

## What's solid

- The i18n boundary is correctly handled: it cites the closed #85 and
  faithfully carries forward its "reopen condition" rather than re-litigating
  it — consistent with #85's actual closed state and content (confirmed).
- The CAP-24/CAP-26 boundary note ("Neighbor, not owner... this feature owns
  the accessible bundle") accurately reflects #536/#540's stated scope; no
  contradiction found there beyond finding 2's seam-ownership gap.
- Consuming #542's (FEAT-C26-1) registry-keyed encoding data via
  `ordering_after` rather than duplicating it is the right dependency
  direction and matches the project's stated anti-decay pattern
  (`ARCHITECTURE.md`'s registry-totality discussion, #78 lineage).

## Note on process

The issue states CAP-26 "marks Open Question 3 as blocking PF-4's filing"
and then says it was "filed under today's capstone-coverage directive using
the recommended default" — i.e., filed despite the blocking marker, with an
explicit escape valve ("a different guideline choice resolves by REPLAN on
#507"). That's a coherent process if REPLAN discipline is actually followed,
but it means any implementation started against the current BANA-default
acceptance criteria is provisional and could be invalidated by a REPLAN
before or during the work — worth flagging to whoever picks this up, not a
defect in the issue text itself.
