# Issue #782: TASK-C553-1: "coming from Logisim-Evolution" and "coming from Digital" — two one-pagers mapping the gestures a switcher already knows
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

A small, well-bounded documentation task (two migration one-pagers, band 0.5-1 mw)
that nonetheless declares `ordering_after: []` while two of its five acceptance
criteria (AC-2, AC-4) hand it hard content dependencies on two sibling features
that are themselves unshipped open issues. It also silently drops a safety
requirement its sibling task carries explicitly. Neither defect is expensive to
fix, but both should be fixed before work starts, since they change what
"done" can honestly mean for this task.

## Findings, most severe first

### 1. [High] `ordering_after: []` contradicts the issue's own acceptance criteria — AC-2 and AC-4 both depend on unshipped sibling work

The machine block states plainly:

> `ordering_after: []`

But AC-2 requires:

> "links at least one shipped example (#548) per major concept"

and AC-4 requires:

> "The comparison content is referenced from #545's table rather than restated"

I fetched both #548 and #545: both are **open**, unmerged features, not landed
work.

- #548 ("Examples menu... at least ten curated circuits") is the only mechanism
  that would make any JLS example "shipped" and discoverable; today there is no
  `resources/samples/` directory in the repo at all (confirmed by
  `Glob resources/samples/**` → no files), and #510's own audit says plainly:
  "**no example circuits ship where a user can find them** (all .jls files are
  test fixtures or the unsurfaced RV32I showcase)." AC-2's word "shipped" cannot
  currently be satisfied by anything — there is nothing to link to.
- #545 ("the README shows the product... a feature comparison table") is the
  table AC-4 says this task must reference rather than restate. It is open too,
  so the referenced table does not yet exist either.

This is not a hypothetical risk, it is a same-day fact: both dependencies were
open as of this issue's own filing date (2026-08-04) and remain open now
(2026-08-08). An `ordering_after: []` claim that the task is "startable now"
is only true for the writing of prose; it is false for satisfying AC-2 and
AC-4 as literally written. `#553` (the parent feature) is more careful here —
its own `ordering_after` comment reads "importer links are appended when
CAP-16/CAP-29 land, they do not gate the pages" — i.e. the parent explicitly
distinguishes what gates the page from what doesn't. #782 does not carry the
same distinction for #548/#545.

**Recommendation:** either add `#548` and `#545` to `ordering_after` (honest
sequencing), or explicitly scope AC-2/AC-4 down to "link to the example(s)
that exist at authoring time, updated when #548 lands" and "draft the
comparison content locally if #545 has not landed, then fold it into #545's
table and delete the local copy" — mirroring the flexibility #553 gives
importer links. As written, a literal implementation either blocks
indefinitely on two unrelated open issues, or is forced to invent shipped-
sounding examples/tables that don't exist yet, which undermines the honesty
goal AC-3 is trying to protect.

### 2. [High] The issue omits the "marked importer slot" safeguard its sibling task carries explicitly, despite covering the higher-risk half of the pair

The sibling task, #784, is titled: *"'coming from CircuitVerse' and 'coming
from Falstad', **each with a marked importer slot no page may fill before the
importer exists**."* That safeguard is inherited from parent #553's AC-4:
"Each page carries a marked slot for its importer link, filled when the
corresponding importer lands; no page implies an importer exists before it
does."

#782 covers Logisim-Evolution and Digital — the two formats with **active,
heavily-scoped importer capstones already filed** (CAP-16 #311 for
Logisim-Evolution `.circ`, CAP-29 #513 for Digital `.dig`), i.e. the two
audiences most likely to ask "can I import my old file yet?" while reading
this exact page. Yet #782's own acceptance criteria contain no equivalent of
#784's marked-slot requirement — AC-5 only says "No importer work is
specified or started here," which constrains the *implementer*, not the
*page's wording*. A page can satisfy AC-5 by simply never mentioning an
importer, or by mentioning one ambiguously, and still pass every stated
criterion while implying more than is true.

I checked #311 (CAP-16) directly: at head of main it is 30-50 mw of unstarted
work — `grep -rli logisim src/` returns 0, all six required sub-features are
open, and its own text says "the reader would introduce the first [XML
parsing in shipped code]." A migration page for this exact audience, written
without a mandated "no importer yet" marker, is the single most likely place
in the whole repo for an over-eager sentence to accidentally read as "you can
import your `.circ` file into JLS."

**Recommendation:** add an AC to #782 identical in force to #784's title
guarantee — a marked, testable importer-slot placeholder on both pages — 
rather than relying on AC-5's weaker "don't start importer work" framing.

### 3. [Medium] AC-3's honesty anchor is a poor fit for half its own scope

AC-3 says claims must be honest "per `docs/hdl-support-research.md` and the
#510 teardowns." I read `docs/hdl-support-research.md`: it is a July 2026
research report scoped entirely to **VHDL/Verilog/SystemC export-import**
feasibility (Yosys netlists, GHDL/Icarus co-simulation, synthesizable-subset
parsing) — it says nothing about Logisim-Evolution or Digital as GUI tools,
subcircuit models, wire-bundle/splitter semantics, or any of the actual
"gestures a switcher already knows" this task is about. Its only real
relevance is the narrow HDL-export sliver of the comparison (both Logisim-Evo
and Digital have Verilog/VHDL export; JLS's own export/import status is
exactly what that doc documents).

The task's real honesty source for the two competitor-specific claims is
#510's teardown table and per-competitor verdict rows (which the issue does
cite) — not the HDL doc. As written, AC-3 could mislead an implementer into
treating the HDL doc as the primary fact-check source for two GUI-migration
pages, when it covers maybe one bullet point on each page. This isn't fatal,
but it's a citation that oversells its own coverage.

**Recommendation:** reword AC-3 to name #510 as the primary source and the
HDL doc as secondary/for-HDL-claims-only, so a reviewer checking "was this
page honest" knows which document actually adjudicates most of the content.

### 4. [Medium] "Discoverable from the README" and "readable in under five minutes" (AC-1) are asserted, not verified — no test is named

Compare with sibling capstone work in this same feature family: #545 (the
README shop-window task) explicitly commits to "A drift check fails the build
when a README-referenced image path does not exist on disk (the
`ReadmeOnboardingTest` shape #381 §8 already plans)." #782 has no equivalent
mechanism. "Discoverable from the README" and "readable in under five
minutes" are both real, checkable-in-principle properties (a link that exists
vs. doesn't; a word count vs. a floor), but the issue names no test, no word-
count ceiling, and no link-existence check. As written, a PR could add a
single low-visibility link buried in a "See also" list at the bottom of a
600-line README and technically satisfy "discoverable," or write a
1,500-word page and self-certify "readable in five minutes" with nothing to
contradict the claim. ARCHITECTURE.md's own pattern for this kind of
documentation contract (`HelpTopicsTest`, `ReadmeOnboardingTest`) shows the
project has the tooling habit to make this checkable; this issue doesn't
invoke it.

**Recommendation:** add a concrete, testable proxy — a word-count ceiling
(e.g. ≤900 words per page, roughly a 5-minute read) and a README-link
existence assertion analogous to `ReadmeOnboardingTest`.

### 5. [Low] "Major concept" (AC-2) is undefined and gameable

AC-2 requires mapping "the source tool's core concepts and gestures" and
linking an example "per major concept," but nowhere does the issue enumerate
what counts as a major concept for either tool. Logisim-Evolution alone has
tunnels, splitters, sub-circuits-as-components, wire bundles, the "poke tool,"
and analysis-to-truth-table; Digital has parameterized/generic circuits,
measurement probes, and its automatic test framework. A minimal-effort
implementation could declare only two or three "major concepts" per tool and
technically satisfy AC-2 while leaving the actual bounce-causing gaps (per
#510: hierarchy/parameterization, chronogram-equivalent, HDL export
reachability) unaddressed. Since #510 already enumerates per-competitor pain
points and JLS's real gaps, the issue should have pulled a fixed concept list
from that source rather than leaving "major" to the implementer's judgment.

**Recommendation:** enumerate the concept list per tool in the issue body (or
explicitly delegate to a named table/section in #510), so AC-2 has a fixed
checklist rather than an implementer-chosen one.

## What's solid

- **Scope fence (AC-5) is sound and consistent with the rest of the plan.**
  Explicitly refusing to start `.circ`/Digital importer work here, and citing
  the correct owning issues (#311/#323/#451 for CAP-16, #513 for CAP-29),
  matches CAP-16's and CAP-29's own machine blocks — no numbering or ownership
  error found.
- **The band (0.5-1 mw) is plausible** for two static documentation pages once
  the underlying facts (concept lists, honest gap statements) are assembled;
  the risk in this task is content-dependency and honesty-framing, not raw
  effort.
- **Splitting Logisim-Evolution/Digital from CircuitVerse/Falstad (#784) is a
  reasonable task decomposition** — Logisim-Evolution and Digital are the two
  competitors with #510's most detailed, most actionable teardowns, so pairing
  them in one task is defensible triage, even though (per finding 2) the split
  lost a safeguard in the process.

## Verdict rationale

`needs-rework`: the task's intent and scope are sound, but as specified it
(a) contains an internal contradiction between its own `ordering_after: []`
and two of its acceptance criteria, and (b) drops a specific safety
requirement ("no page implies an importer exists before it does") that its
sibling task and parent feature both carry explicitly, for the exact two
competitors whose importers are furthest along in planning. Both are
cheap, mechanical fixes to the issue text before implementation starts.
