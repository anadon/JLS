# Issue #605: TASK-C486-2: a net can say how long it physically is — an optional declared length that is never derived from drawn pixels, and an older reader that still opens the circuit
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

A `WireNet` gains an optional declared physical length (absent by default), written
as a #319-style "optional per-section-versioned section" riding #367's format bump,
never derived from the drawn wire's pixel length, with a pre-#605 reader opening an
annotated file structurally instead of refusing or silently dropping the data. This is
task 2 of #486 (FEAT-058)'s three-scope decomposition.

## Findings, most severe first

### 1. AC1 invents a dependency #367 does not provide — "FEAT-047's time/length base" names a base that only covers time

AC1: *"A net accepts an optional declared length in physical units against FEAT-047's
time/length base."* I read #367 (FEAT-047) in full: its entire deliverable is a
`TimeBase` record — `$timescale`-style magnitude (1/10/100) × decimal exponent from
seconds to femtoseconds, a non-accumulating `seconds(ticks)` conversion, and a version
policy. The word "length" does not appear anywhere in #367 except in the unrelated
phrase "propagation delay over a physical **length** is a statement about seconds"
(a capability-impact bullet about #313, not a grammar). There is no length unit, no
distance exponent table, no `LengthBase` type, nothing a net's declared length could be
"against." #605 conflates a time base that exists in spec with a length base that does
not exist anywhere in the tree or in any cited issue. **Recommendation:** either point
AC1 at the actual source of a length-unit grammar (none is cited anywhere in #486's own
interface section either — it never mentions millimetres as a *type*, only as example
values) or file/cite the missing piece explicitly instead of asserting it rides #367.

### 2. AC2 mandates the expensive mechanism for data the parent issue's own evidence calls safe to drop silently

AC2 requires the declared length be "an optional per-section-versioned section... not
... an ordinary attribute... refusable by name rather than swallowed by the reader's
silent-ignore valve." But #486 (the parent feature, evidence item 4) argues the exact
opposite for this same data: *"The silent-drop valve this feature may ride and FEAT-060
may not... A dropped lint input is fail-open and harmless. A dropped constraint is a
silently unmanufactured requirement, which is #487's problem and the reason the two are
separate features."* The declared length feeds a lint that already degrades to "not
assessable" whenever an input is missing (#486 §3, §4 invariant 5) — silently losing
it on an old-reader load produces the identical, already-correct "not assessable"
output, unlike #367's TimeBase (whose silent loss misreads every duration in the file,
the actual case a must-understand mechanism is *for*). #605 does not engage with its
own parent's argument that this specific data is fail-open-safe; it just asserts the
heavier requirement. **Recommendation:** either rebut #486 evidence-4's harmlessness
argument explicitly (why does length need to be refusable-by-name when the lint already
treats absence as a valid state?) or drop AC2 to an ordinary attribute and save the
#319 dependency entirely.

### 3. The infrastructure AC2/AC3 require does not exist and is not settled design

`docs/file-format.md` has no section/frame concept — §5's forward-compat policy is
only "unknown attribute name → silently dropped" / "unknown item kind or tag → hard
error" (`docs/file-format.md:220-243`); a repo-wide grep for "section-versioned" or
"optional section" outside issue bodies returns nothing. The mechanism AC2/AC3 assume
is #319 (FEAT-013), which is itself open, `blocked_by: [334]` → `blocked_by: [315]`
(both open, per #319's own adversarial review in this repo, `issue-reviews/issue-0319.adversarial.md`
finding 5, "roughly 10+ mw end-to-end before ... this issue [#319] itself can start").
Worse, #319's *visionary* review in this same repo
(`issue-reviews/issue-0319.visionary.md`, §"(A) Must-understand as a marker...")
explicitly recommends **deleting** the whole per-section-version-integer design in
favor of a lighter must-understand marker on ordinary items, and names
"signal-integrity attributes" — this task's own payload — as one of the four capstone
payloads that fits the lighter form "directly." #605 is written as if #319's mechanism
is a stable foundation to build on; it is a live, contested design with a credible
proposal on record to remove the exact feature (a distinct section construct) AC2
requires. **Recommendation:** do not word AC2 against #319's current draft shape;
word it against the *property* needed ("refusable by name, not silently dropped, if
that turns out to be required at all — see finding 2") and let the task adapt to
whatever #319 lands as.

### 4. Evidence commit is unresolvable, inheriting a defect already caught and documented for a sibling issue

`2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` is cited as the pin for the `WireNet.java:22-30`
claim. It does not resolve in this checkout:

```
$ git log -1 2d0ca9d
fatal: ambiguous argument '2d0ca9d': unknown revision or path not in the working tree.
```

This is the identical commit already flagged as broken in `issue-reviews/issue-0319.adversarial.md`
finding 2 ("exists only on a branch scheduled for deletion... not an ancestor of
`master`"), which #605 inherits unchanged with no correction and no fallback pin. I
independently re-verified the underlying claim against current HEAD — `src/jls/elem/WireNet.java:22-30`
is indeed `ends`/`wires`/`bits`/`hasinput`/`triState` with no length field — so the
substantive claim holds, but the citation itself cannot be used to re-verify anything
else in the issue that leans on "at `2d0ca9d`." **Recommendation:** re-pin to a
`master`-reachable commit before work starts, matching the fix already recommended for
#319.

### 5. Net identity is presupposed but not in scope, and not obviously solvable by a per-WireEnd attribute

The declared length must attach to "a net," but nets have no identity in the save
format: `docs/file-format.md` §7 states wires are not saved as elements at all — a net
is reconstructed at load time purely from `WireEnd` `attach`/`wire` references, and the
only net-level property saved today (`tristate`) is duplicated onto *each* `WireEnd` of
the net (`docs/file-format.md`: *"a tri-state net marks its ends with `int tristate 1`"*).
#486 recognizes stable identity as a real dependency — but scopes it (via #336, FEAT-004
net-partition IR + stable naming) only to the *lint* scope ("the verdict names a net"),
and #605's own `ordering_after: [367, 319]` omits #336 entirely. If the declared length
is meant to be one section-level record per net rather than a duplicated per-`WireEnd`
value (which AC2 seems to imply, given it explicitly rejects "an ordinary attribute"),
the issue needs to say what a net *is* in that section's key space, and it doesn't.
**Recommendation:** state explicitly whether the length is stored per-`WireEnd` (like
`tristate`, using existing element identity) or per-net-as-a-new-first-class-thing
(needing #336 or an equivalent), and add that dependency if the latter.

### 6. Cost band is optimistic relative to the parent's own reconciliation

`band_mw: "1-2"` sits below two of #486's own three cost derivations for this exact
scope: the staged-path derivation gives 1.5-3 mw and the permanence-itemization
derivation also gives 1.5-3 mw for "the length/edge-rate attributes"; only the
format-bump-remainder derivation (0.5-3 mw) contains 1-2. #486 itself flags that its
three derivations disagree and "no number was adjusted to fit" — #605 picks a figure
that undercuts the majority of them rather than the parent's own carried (higher)
number. Not fatal, but worth a maintainer's eyes given #486 already documents the
discrepancy is unresolved. **Recommendation:** either justify 1-2 explicitly or use
1.5-3 to match the two majority derivations.

## What's solid

- **AC4's refusal-from-geometry ban is well-argued and cites real, checkable
  arithmetic** — the "1 mm/grid square ⇒ 0.133 λ at 20 GHz" figure matches #486's Open
  Question 1 exactly, and refusing to derive a physical length from a schematic
  (as opposed to a routed board) is the correct call for the stated reason.
- **AC5 (byte-identical goldens, unchanged legacy loads when absent) is concrete and
  mechanically testable**, matching the pattern already used successfully for #367's
  and #486's own "absent by default" invariants.
- **AC3's "spans this task and #319, so the test says which half it is asserting"**
  correctly avoids claiming a single test can prove a cross-issue integration property
  alone — this is the right instinct, it just currently rests on the unsettled #319
  mechanism (finding 3).
