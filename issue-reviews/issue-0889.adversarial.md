# Issue #889: TASK-C361-1: a value-range crossing is declared data — the numeral rule is the default, everything else is a named mapping that is total or refuses by value, and no crossing is ever implicit
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

A "mapping" data structure `(sourceInterval, targetInterval, kind, data)` — later corrected in-thread to `(source port domain, target port domain, kind, data)` — with four kinds (`numeral`, `ordinal`, `table`, `partial`), load-time totality checking, global meta-state handling (X/Z/U), a digest for identity, a size guard, and zero-cost-when-unused storage. Filed 2026-08-08, same day as its own capstone (#888), its declared ordering predecessor #419, and a same-day self-correcting comment.

## Findings, most severe first

### 1. [Critical] AC-4 requires an X state that the current, enforced simulation semantics explicitly forbid — and the issue's own dependency list doesn't include the feature that would introduce it

AC-4: *"X in → X out and **Z in → X out** on every mapping kind, asserted per kind; no mapping can override meta-state handling."* The "Meta-states" paragraph adds: *"X in → X out; **Z in → X out** (a bridge cannot invent a driver); U per #322's stage machinery."*

`docs/simulation-semantics.md:47-49` (verified at HEAD, not a stale citation): *"Bits are / two-state: 0 or 1. **There is no unknown/X state anywhere in the / simulator**"* — pinned by `VcdExportGoldenTest.vcdIsStructurallyWellFormedAndTwoStatePlusHiZ`. I confirmed that test is real: `test/jls/VcdExportGoldenTest.java:321` — `assertFalse(line.contains("x"), "JLS values are 0/1/z only: " + line);`. This is a live, currently-passing, currently-enforced invariant of the codebase, not a stale doc claim.

X only becomes representable once **#322** (FEAT-026, "a signal can say 'unknown' and 'undriven' per bit") lands. I fetched #322: it is **open**, gated on two **unfiled** tasks (TASK-0056, TASK-0057), and its own Open Questions are explicitly unresolved and blocking: "Two planes or three?" (undecided), and **"Does `U` ship in this feature or in FEAT-037?"** (undecided). #889's `ordering_after` field lists only `[878, 419]` — **#322 is not in it**, despite AC-4 being unsatisfiable without it. This is not a stylistic gap: the issue's headline acceptance criterion depends on infrastructure the issue does not declare as a prerequisite and that does not exist and is not yet designed.

**Recommendation:** add #322 to `ordering_after` (or `blocked_by`), and do not accept this issue for execution until #322's X/Z/U design questions (plane count, whether U ships here) are actually resolved — right now AC-4 is testing against an alphabet that isn't defined yet.

### 2. [High] The "U per #322's stage machinery" citation does not correspond to anything in #322

I read #322 in full. It never uses the phrase "stage machinery," and it never assigns U's semantics to a mechanism named anything like that — it says U is an **open question** ("reserve the code point here, ship the semantics with reset" is the *recommended default*, not a decision) about whether U ships in #322 at all or is deferred to a separate, unfiled FEAT-037. Presenting an unresolved, disputed design point as settled prior art ("per #322's...") misleads whoever picks this task up into thinking U-handling is a solved problem they can simply call into.

**Recommendation:** cite #322's actual state ("open question, not yet decided") rather than a specific mechanism it doesn't contain.

### 3. [High] The issue's own vocabulary conflicts with its declared ordering prerequisite (#419), and the pivot between them is unacknowledged

#889 is written entirely in terms of `sourceInterval`/`targetInterval` and `lo()/hi()` (signed, bounded intervals like `[-1,+1]`, `[0,2]`, `[0,255]`). Its stated prerequisite, **#419** ("`ordering_after: [878, 419]`... 419 makes the interval enforced rather than merely stated"), is a **radix** model: `Put.getRadix()`/`getDigits()`, digit alphabets `{0,...,r-1}`, radix-mismatch refusal. #419's body never mentions `lo()`/`hi()` or intervals at all — its data model is a bare digit count, not a lo/hi range, and it explicitly states *"Every put is born radix 2; there is no 'unknown radix'... Do not invent `radix == 0`"* with no notion of negative or offset ranges.

#888 (the capstone, also fetched) resolves this by noting `#344 (FEAT-028) — as replanned to the signed-interval model (see its REPLAN comment)` — i.e., there was a mid-August pivot from radix to signed-interval that superseded #419's model. But #889 never states this pivot, never cites the REPLAN, and describes #419 as if it already "makes the interval enforced" — which is not what #419, read on its own, says it does (#419 enforces *radix agreement*, not *interval agreement*; a radix-3 alphabet and a `[-1,+1]` interval are not the same object, and #419 doesn't reserve `lo()/hi()` — it reserves `getRadix()/getDigits()`). Anyone implementing #889 by reading #419 as instructed will build against the wrong primitive.

**Recommendation:** #889 should cite the actual REPLAN comment on #344 that performed the radix→interval pivot, not describe #419 (a radix-only task) as though it already delivers interval enforcement.

### 4. [Medium] The issue contradicts itself within one comment thread, and the acceptance criteria were not updated to match

The sole comment (posted 3 minutes after the issue, same maintainer) overwrites the core tuple: *"Read this issue's `(sourceInterval, targetInterval, kind, data)` as `(source port domain, target port domain, kind, data)`"* — because "[n]ets carry values and width and no alphabet." This is a substantive model change (mapping attaches to ports, not net-level intervals; direction becomes structural rather than incidental). Yet the **Acceptance Criteria section of the issue body was never edited** to reflect it — AC-1 through AC-7 still read in terms of the pre-correction model, and the "Boundaries" section's dependency on #453 (the bridge element, whose *ports* are the actual attachment point per the correction) is only implicitly reconciled. A reviewer verifying AC-1/AC-5 has to manually apply the comment as a patch over the body; nothing in the acceptance criteria text itself was updated, which is exactly the kind of drift the issue's own "totality is checked once, not a runtime hope" ethos argues against applying to its own criteria.

**Recommendation:** edit the AC section in place rather than leaving a correcting comment as an unmerged patch.

### 5. [Medium] AC-6's size guard has no number, and the two references naming its source ("D5", "D7") do not resolve anywhere in the repository

AC-6: *"A `table` above the stated budget is refused with the arithmetic printed and names `numeral`/`ordinal` as the alternative."* No budget value is given anywhere in the issue — contrast this with sibling issues in the same corpus (#453's coverage floors are literal numbers — `0.730/0.700/0.585`; #322's cost bands are literal week ranges). "D5's element-level-cliff policy" and "D7['s]... discipline" are cited as the source of the budget and the storage rule, but `grep -rn "D5\b|D7\b"` across the repository returns **zero hits** outside unrelated false-positive matches (radix's own grep noise) — these documents/decisions are not in the tree I can check against. As written, AC-6 cannot be executed or graded: an implementer must invent the budget number themselves, and there is no way to tell whether the eventual number satisfies "D5" because D5 isn't checkable.

**Recommendation:** either paste the actual number (as every sibling task in this corpus does) or point to a resolvable location for D5/D7.

### 6. [Medium] "No new item kind" addresses only half of the save-format's own bump rule, and the other half is never discussed

`docs/file-format.md:436-443` (verified, real file) states a FORMAT bump is required for **either** (a) a new item kind, **or** (b) *"any change to the block structure... of an existing record."* #889 argues only the item-kind half: *"Two `int` items for each interval plus a value list — no new item kind."* But AC-5 requires *"two circuits declaring the same mapping produce the same digest"* — which only makes sense if a mapping is a **named, shared declaration** two circuits can both reference (so they can be compared), not merely a private attribute bag inline on one bridge element. A shared, cross-referenced, digest-identified declaration is exactly the kind of thing that plausibly needs new block structure in the save grammar (a new top-level record alongside `CIRCUIT`/`ELEMENT`), which is bump-triggering per file-format.md's own rule (b). #889 never raises this half of the question, unlike #453 (its sibling task), whose O3/§7.1 explicitly works through both branches of the same rule for its own feature. This is a real gap: the "no FORMAT bump" implication throughout #889 is asserted, not demonstrated.

**Recommendation:** state explicitly whether a mapping is (i) purely per-element inline attribute data (in which case the digest claim in AC-5 needs a different justification — comparing two independent inline copies isn't the same problem "definition-identity" usually solves) or (ii) a shared, referenced declaration (in which case address the block-structure bump question directly).

### 7. [Low-Medium] AC-1 is tied to a fixture that cannot exist for a long time, inviting a narrower substitute to be waved through as compliant

AC-1: *"...drives binary control into a ternary datapath in the **CAP-39 fixture**, and the resulting run matches a committed byte-for-byte golden."* CAP-39 is #888, which requires #322 (unlanded, unfiled sub-tasks), #344-as-replanned, and #361-as-amended — none landed. Until then, "the CAP-39 fixture" doesn't exist to test against. As stated, whoever picks up #889 either blocks indefinitely on #888's landing, or substitutes a smaller ad hoc fixture and asserts AC-1 is "met" against something that isn't actually the named fixture — which is exactly the kind of gap this corpus's own discipline (e.g. #419's insistence that predictions be checked against the *real* commit, not an approximation) warns against.

**Recommendation:** either give #889 its own standalone fixture independent of CAP-39's eventual one, or explicitly sequence #889 to start only after #888's fixture exists.

### 8. [Low] Scope size relative to the stated trigger

The entire mechanism — four mapping kinds, load-time totality, a digest/identity scheme borrowed from #340 (a separate, unverified feature), a size-guard budget system, and global meta-state wiring across every kind — is scaffolded from a single quoted maintainer sentence: *"make sure there's a generic mapping table for other value range mappings to use."* That may well be exactly what's wanted, but given how much unlanded machinery this pulls in (#322, #340, #419's replanned successor, #453's port model), it is worth a maintainer sanity check that the scope matches the one-line directive before committing implementation weeks to it.

## What's solid

- The "not a coercion" argument (an explicit, named, digest-carrying mapping vs. #419's forbidden implicit coercion) is a coherent, well-reasoned distinction and doesn't actually contradict #419's O9 as feared.
- AC-7 (binary circuits' bytes never move; a mapping is only written when actually used) matches this codebase's established, tested discipline for additive features (mirrors #419/#453's byte-identical-golden pattern) and is the right target.
- The three-way boundary split against #453 (element), #419 (enforcement), and #422 (operator kernel) is clearly drawn and avoids the obvious overlaps.

## Bottom line

The mechanism design is reasonable in outline, but as filed it (a) makes a headline acceptance criterion (AC-4, X/Z meta-states) depend on a feature (#322) it doesn't list as a dependency and that is itself unresolved on the exact question #889 needs answered, (b) attributes to #322 a mechanism ("stage machinery") that isn't in #322's text, (c) describes its actual stated prerequisite (#419) as delivering something (interval enforcement) that #419's own text doesn't deliver, (d) contains an unmerged self-correction in the comments that the acceptance criteria were never updated to match, and (e) leaves its own size-guard criterion (AC-6) with no checkable number. These need to be fixed before this is executable work rather than a compelling-sounding design sketch.
