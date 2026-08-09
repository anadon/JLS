# Issue #784: TASK-C553-2: "coming from CircuitVerse" and "coming from Falstad", each with a marked importer slot no page may fill before the importer exists
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Stripped of its wrapper, #784 buys one thing: **evaluation-stage credibility**. The four
switcher pages are not tutorials; they are the artifact a skeptical instructor reads
before deciding whether JLS is worth a download. The honesty mechanism (AC-2/AC-3) exists
because an over-claiming page destroys exactly the currency the page was minted to earn.
That instinct is correct and is the strongest idea in the issue — it is worth more than
the two pages it is attached to.

The two things I would change are the *mechanism's scope* and the *pages' audience*.

## Reframing 1 — the importer slot is the wrong unit; the project needs a capability ledger

AC-3 specifies a check "keyed to importer slots on migration pages." That is a bespoke
guard for four markdown files, and it points in only one direction (doc claims more than
code has). JLS already has a house idiom for exactly this class of problem, and it is a
*catalog*, not a per-page assertion:

- `test/jls/ExtensionPointCatalogTest.java` — the doc table in `docs/extension-points.md`
  and the `ExtensionPoint` constants must agree **in both directions**.
- `test/jls/CliFlagTableTest.java` — the flag table in `JLSStart` is authoritative; usage
  text is generated from it, so a hand-maintained second list cannot exist.
- `test/jls/FileFormatSpecTest.java`, `HelpTopicsTest` (link checker + palette-coverage
  completeness), `HotkeysHelpAccuracyTest` — same shape.

The one-directional check #784 asks for would have missed the failure #510's adversarial
pass actually found in this repo. `src/jls/hdl/imp/NetlistImporter.java` (#61) exists,
is test-covered (`test/jls/hdl/imp/NetlistImporterTest.java`,
`ImportPipelineTest.java`), and is referenced by **nothing** outside its own package and
those tests — no CLI flag, no menu. That is code outrunning the docs, and #510 §1 note 2
scores it as free points left on the table. A guard that only prevents docs from
outrunning code institutionalizes half the problem.

**Concrete alternative:** file the mechanism as its own small infrastructure item — a
single machine-readable **capability-claim ledger** (say `docs/capability-claims.md` plus
one test) where each claim id (`import.circ`, `import.dig`, `import.cv`,
`import.falstad`, `import.yosys-json`, `waveform.chronogram`, `web.demo`) carries: its
state (`shipped` / `recipe-only` / `planned:#NNN` / `not-planned`), the *code predicate*
that proves `shipped` (a flag in the `JLSStart` table, a registry entry, a reachable menu
action), and the owning issue. Every claim-bearing surface then renders from it:
#545's README comparison table, all four #553 pages, the #589/#783 white paper, CAP-21's
course kits, in-jar help. One test fails when a surface asserts a state the predicate
does not support **and** when a predicate goes true with no surface updated.

This is not speculative generalization. #783 is independently asking for the same
machinery from the other end ("every guarantee names the test that would fail if it
stopped being true"), and #513 PF-1 wants a format-agnostic import-report contract
(partly already present as `jls.hdl.imp.ImportSummary`/`ImportResult`). Three issues
converging on "claims must be derived, not typed" is the signal that the seam belongs one
level down from any of them.

## Reframing 2 — "not yet available" is the wrong end state to design around

AC-2 fixes the slot at two states: filled, or "not yet available." Design a document
around its empty state and you ship four pages that each say, prominently, *the thing you
came here for does not exist*. That is honest and self-defeating at once.

The project's own plan already names the better third state: CAP-29's KC-29-1 says a
format that blows its band is "downgraded to a documented external-conversion recipe."
So make the field a **migration-path field with three first-class states** — *recipe
today* (redraw guide, external converter, or the `-savetext`/plain-text path for
diffable hand-porting), *importer* (link, once the ledger says shipped), *not planned*
(said once, plainly) — and let the check verify the rendered state against the ledger.
Every page then always answers "how do I get my circuits in?" with something actionable.
The Falstad logic subset is small enough (#513 PF-4, 2–3 mw) that a hand-port recipe for
gates/FFs/labeled nodes is genuinely usable in the interim.

## Reframing 3 — these two pages, as scoped, address segments #510 says are unwinnable

#782 covers the two incumbents where a gesture dictionary is the right form. #784's two
are different, and the survey says so explicitly:

- Falstad: *"The analog/intuition core is not winnable — do not contest it."* The winnable
  segment is the digital-coursework overflow — a course that outgrew an analog solver.
- CircuitVerse: browser-first students are *"not winnable without a web story"* (CAP-19
  closed). The winnable segments are grade-at-scale instructors, timing/HDL courses burned
  by their #1412/#5328, and offline/locked-down labs.

Both winnable segments are **instructors**, not users mid-gesture. A page whose spine is
"the CircuitVerse gesture X is JLS's gesture Y" is written for the segment that is not
coming. Reorganize the spine by segment and keep the gesture map as an appendix:

- *Coming from CircuitVerse* → "your circuits, your grades, your lab machines": the
  documented `-t` grammar and VCD profile (`docs/batch-interface.md` — JLS's only 5/5
  axis, and no rival documents grading semantics at all), the headless container for
  autograders, and offline permanence versus a hosted tool that can freeze. Say plainly:
  *if your students need zero-install in a browser, JLS is not that today.*
- *Coming from Falstad* → "when your course outgrows the analog sandbox": deterministic
  event-driven timing with a normative spec (`docs/simulation-semantics.md`), working
  hierarchy, grading. And a sentence I would insist on: *if what you want is analog
  intuition-building, keep using Falstad — it is better at that than we will be.*

That sentence buys more credibility than four "not yet available" slots. It is the same
move as the README's macOS-signing and GPG-signing paragraphs, which are the most
trustworthy prose in the repository precisely because they concede.

## Seam and sequencing

The current split gives #782 two pages with no honesty mechanism and #784 the other two
plus a retrofit of all four. Cut the seam by *concern*, not by count: (a) ledger + one
page proving it end-to-end, (b) the remaining three. As filed, whatever #782 ships must be
reopened here — small, but the wrong dependency direction for a mechanism three other
issues want.

**Placement.** These pages live in `docs/` and are "discoverable from the README." That
reaches the pre-install evaluator, which is real value — but #510's ten-minute bounce
happens *after* launch, at the empty `JTabbedPane`. The cheapest large win available is a
row of "Coming from Logisim-Evolution / Digital / CircuitVerse / Falstad?" links on
CAP-27 PF-3's first-run pane (#511). Note the constraint ARCHITECTURE.md's help-delivery
decision imposes: in-jar help is plain HTML 3.2 with relative links, checked by
`HelpTopicsTest`. Either author the pages so they render to both surfaces, or have the
first-run pane link out — decide it now rather than discovering it when PF-3 lands.

## What I am disregarding, explicitly

**AC-3 as written.** A check scoped to "importer slots on migration pages" should not be
built; it is the special case of a general facility that this codebase already has the
idiom and the demand for, and it guards the less dangerous direction. Replace with ledger
conformance. **AC-1's implied gesture-dictionary shape** for these two tools, per
Reframing 3. AC-4 — state what JLS does not yet do — is the best line in the issue and
should be promoted from a trailing criterion to the organizing principle of both pages.
