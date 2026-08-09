# Issue #792: TASK-C584-2: one mvn goal emits the in-jar help tree from source, preserving the Map.jhm topic-id and TOC contract, with HelpTopicsTest unchanged and green
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's being asked

#792 is `TASK-C584-2`, child of `FEAT-C35-1` (#584), ordered after `TASK-C584-1` (#791). It asks for a single `mvn`-reachable generator goal that emits the in-jar help tree from a not-yet-existing plain-text source tree, with the generated tree required to pass `test/jls/HelpTopicsTest.java` **unchanged** — the title's own words.

## Findings, most severe first

### 1. [High] AC-2 cannot be honestly satisfied at #792's own completion — the full-corpus source it needs is explicitly someone else's job, ordered later

`HelpTopicsTest` is not a smoke test; it validates the **entire real corpus**: every one of the 88 `Map.jhm` topics resolves (`test/jls/HelpTopicsTest.java:232-240`), every inline `href`/`img src` across all 83 bundled HTML pages resolves case-sensitively (`:296-315`), every bundled page is reachable from the topic map (`:320-349`), and every palette element type (`jls.edit.Palette.entries()`) has a mapped, resolving topic (`:181-208`). Passing it "unchanged" therefore requires the *full* migrated source corpus to already exist as input to #792's generator.

But the full-corpus migration is explicitly not this task's job — it is `TASK-C584-3` (#793): "All existing help content is migrated to the source form by a mechanical conversion" (#793 AC-1), ordered **after** #792 (`ordering_after: [TASK-C584-2]`). And the task ordered **before** #792, `TASK-C584-1` (#791), only records the source-format decision and enforces the viewer-safe markup subset — it explicitly does not migrate content ("The decision the rest of the documentation system rests on... Alongside it, the in-jar viewer's renderable subset... becomes a written contract the build enforces"; no migration AC anywhere in #791). So by the dependency graph as written, no task ordered at or before #792 produces the 83-page source corpus #792's own AC-2 requires. Either #792 silently has to redo #793's stated deliverable (making #793's AC-1 a duplicate), or #792's AC-2 can only be checked against a toy/fixture subset and the literal claim "passes today's HelpTopicsTest unchanged" is unverifiable when #792 is actually closed. This is the same structural defect the sibling review of #793 flagged from the other side (`issue-reviews/issue-0793.adversarial.md`, finding 1); it is #792's own AC-2 that is unmeetable, not just #793's downstream framing. Recommend: name one task as the full-corpus-migration owner explicitly, and have the *other* task's DoD reference a fixture-scale HelpTopicsTest run (or defer the "unchanged and green" claim to whichever task runs last in the chain).

### 2. [Medium-High] The source tree's implicit file-per-page model doesn't obviously encode Map.jhm's many-to-one topic/page mapping

`Map.jhm` is not a 1:1 topic↔page map: `create.elem` and `picking` both resolve to `editor/editing/picking.html` (`resources/help/Map.jhm:11,36`), and the TOC nests the same page under multiple contexts (e.g. `circuit.over`/`wires`/`wire.bundles` form a 3-level chain of distinct topics landing on two different files under `elements/wiring/`, `JLSHelpTOC.xml:48-57`). Nothing in #792's outcome or ACs says how a "plain-text source tree" — which reads naturally as one source file per output page — is meant to declare *multiple* topic ids for one page, or the TOC's arbitrary nesting depth (up to 4 levels, `JLSHelpTOC.xml:48-96`) independent of directory layout. If the generator's design assumes topic-id ↔ file is 1:1, `create.elem`/`picking` silently breaks on regeneration — exactly the "code reference…cannot break" property AC-4 claims to guarantee. Recommend the AC spell out how the source format models many-to-one topic mappings and TOC nesting, not leave it to implementation discovery.

### 3. [Medium] AC-1's "no file hand-edited" claim has no defined verification mechanism — gameable

"no file in the output is hand-edited (asserted, not asked for in review)" names *what* must be true but not *how* it's checked. Without a defined mechanism (e.g., a provenance header the generator stamps and a build check that rejects any generated file lacking it, or output living only under `target/` so a checked-in hand edit is structurally impossible), this is self-certifying: a contributor could hand-write output once, declare it "what the generator produces," and nothing catches the substitution. Recommend a concrete mechanical check (generated-file marker + build assertion, or non-committed output directory) rather than a bare prose claim.

### 4. [Medium] AC-3's "no network fetch" predicate is undefined and easy to satisfy without truthfully proving the property

"a test asserts no generated in-jar page depends on a network fetch to render" — the obvious implementation is a grep for `http://`/`https://` in `href`/`src`. That misses protocol-relative URLs (`//host/...`), `<iframe>`/`<object>`/CSS `@import` (if the generator's output markup isn't as constrained as today's hand-written HTML 3.2), or a link that resolves locally today but was authored assuming a CDN fallback. ARCHITECTURE.md already documents plain HTML 3.2 with relative links as the current portability discipline; this AC would benefit from citing that discipline directly and enumerating the checked constructs rather than restating the goal as if it were self-evidently testable.

### 5. [Medium] AC-4 ("topic ids stable across regeneration") names a property with no test described to establish it

Nothing in the ACs describes running the generator twice (or against a perturbed source ordering) and diffing topic ids — the natural way to prove "stable across regeneration" rather than merely "correct once." As written, a single successful build run satisfies the letter of AC-4 without ever exercising the regeneration-stability claim it makes.

### 6. [Low-Medium] Feasibility/cost: band_mw 1-1.5 is tight given the generator must reproduce Map.jhm/TOC fidelity (finding 2) *and* whatever viewer-safe-subset constraint #791 lands on

#791's own AC-4 requires pricing the case where the viewer's renderable subset is too narrow for the chosen source format ("the recorded answer is the cost of upgrading the in-app viewer, not a degraded source format") — a decision #792 has no visibility into or control over at estimation time, since #791 is a sibling task rather than a strict predecessor with a settled outcome documented before #792 is scoped. If that pricing lands on "upgrade the viewer," #792's generator work is invalidated or expanded after estimation.

### 7. [Low] Tooling/license note

Whatever Markdown/AsciiDoc processing library the generator adopts needs a GPLv3-compatible license — the project has form here (ARCHITECTURE.md's FlatLaf evaluation, and the ELK EPL-2.0 in-process-linking hazard flagged for a *different* subsystem). Worth one explicit sentence in the AC so the generator doesn't quietly pull in a copyleft-incompatible dependency the way ELK was called out as a hazard elsewhere.

## What's solid

- Depending on `TASK-C584-1` (#791) before building the generator is the right order in principle — the source format should be chosen before the tool that consumes it is written; only the *content* timing (finding 1) is broken, not this dependency's existence.
- AC-4's underlying goal (topic ids must not shift under regeneration, so code references stay stable) is exactly the right property to protect — it's the verification method that's missing (finding 5), not the requirement itself.
- Scoping this task as "generator only," separate from #793's "migration + static site," is a reasonable decomposition of a large feature into task-sized slices — it just needs the corpus-ownership contradiction (finding 1) resolved for the boundary to actually hold.

## Recommendation

Do not start implementation against this issue as written. Resolve the cross-task corpus-migration ownership contradiction first (finding 1) — it determines whether AC-2 is even checkable at #792's completion — then make AC-1, AC-3, and AC-4 mechanically verifiable (findings 3-5) and spell out how the source-tree format represents Map.jhm's many-to-one topic mappings (finding 2) before treating this as a buildable task.
