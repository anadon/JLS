# Issue #630: TASK-C523-2: where the isomorphism cannot be proven, the parity claim is narrowed in writing before release — the artifact says what parity means and the docs name the fixture classes it holds for
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#630 (TASK-C523-2) is the documentation-honesty half of FEAT-C05-1 (#523), sibling
to TASK-C523-1 (#627, the `NetPartitionIsomorphismTest` itself). It asks for four
things: (1) a pinned header sentence in the emitted netlist stating parity is
"an isomorphism on nets"; (2) release docs naming which fixture classes the
isomorphism check is green for, and which aren't with the blocking semantic
named; (3) a repo-wide check that no fixture class without isomorphism evidence
is described as having parity; (4) a completeness check so a new fixture class
can't silently fall off the narrowed claim. None of the underlying
infrastructure exists yet — confirmed by direct search of this checkout.

## Findings, most severe first

### 1. The premise contradicts its own sibling task's acceptance criterion
`ordering_after: ["TASK-C523-1"]` (#627). #627's AC-1 requires
`NetPartitionIsomorphismTest` to be "green across the committed fixture
corpus" — i.e. every fixture actually in the corpus must pass. But #630's
entire reason to exist is AC-2/AC-4 here: "names any class it is not green
for together with the KiCad/gEDA semantic that prevents it" and "a fixture
class with no isomorphism evidence may not be described as having parity."
Those two constraints cannot both hold for the same corpus: if a fixture
class is in the corpus, #627 requires it green; if a class is known
unprovable, #627's own AC-1 implies it never enters the corpus in the first
place, in which case #630 has nothing to name — "not green for" and "absent
from corpus" are different states that neither issue distinguishes.
**Recommendation:** before either task starts, define whether an "unproven"
fixture class is (a) committed-but-expected-red, or (b) deliberately excluded
from the corpus and tracked only in prose. The two issues currently assume
different answers.

### 2. "KiCad or gEDA semantic" references a target that isn't in scope
The phrase (issue body, AC-2 wording carried from #523 AC-4) treats gEDA as a
live second export target alongside KiCad. But `grep -rli geda` and
`grep -rli lepton` return zero hits anywhere in this repository — including
`docs/standards-adoption/`, `docs/hdl-support-research.md`, and the capstone
issue #298 itself, whose title and scope ("File → Import Netlist… in KiCad")
name KiCad exclusively. #307 (closed as duplicate of #298) is the one place
that ran the `grep -rli geda` check, found nothing, and did not carry gEDA
forward as a target — #298's scope is KiCad-only. #630 inherited "KiCad or
gEDA" boilerplate from #523 without checking whether gEDA is actually an
export target. As written, AC-2 asks the release docs to name a "gEDA
semantic" blocking a fixture class for a format JLS has no emitter for and no
plan to build one for.
**Recommendation:** strike "or gEDA" unless a gEDA emitter is actually in
scope somewhere in this dependency chain (#336/#365/#366/#460/#468) — verify
before shipping the doc language, not after.

### 3. "Fixture class" is used as load-bearing taxonomy but is defined nowhere
AC-2/AC-3/AC-4 all key off "fixture classes," but the string "fixture class"
appears in zero `.java` or `.md` file in this repository outside this
issue and its siblings (#523, #627) — no enumeration, no naming convention, no
existing grouping in `test/jls/hdl/**` or `test/resources/hdl/**` that this
task could point at. Whoever implements #630 has to invent the taxonomy
(per-directory? per-construct, e.g. "tri-state," "subcircuit," "bus"? per
KiCad symbol mapping?) with no guidance, and AC-4's completeness check ("a
fixture class without a corresponding line... is caught") cannot be written
as a mechanical ratchet test until that taxonomy is fixed — unlike this
repo's existing completeness ratchets (e.g. `HelpTopicsTest`'s palette
coverage), which key off an enumerable, already-existing set (the element
palette).
**Recommendation:** either #627 or #630 needs to define "fixture class" as a
concrete, enumerable partition (e.g., a directory-per-class convention under
`test/resources/hdl/kicad/`) before AC-4 is implementable, not left implicit.

### 4. AC-3's repo-wide claim scan is underspecified and will be either impossible or gamed
"A fixture class with no isomorphism evidence may not be described as having
parity **anywhere in the repository**" — caught by "a test or a documented
review step." This repo already uses the word "parity" ~40+ times for
unrelated concepts: `query-parity`/`culling-parity` (spatial index, see
`proofs/README.md`), byte-parity of undo/op paths (`CHANGELOG.md`),
CLI contract-parity, `GateOutlineParityTest`, feature-parity-with-competitors
language throughout `docs/capability-roadmap/*.md`. A literal
keyword grep for "parity" (the only mechanical way to implement this without
an NLP semantic-strength judge) would produce dozens of false positives
unrelated to net-partition export, or — if scoped down to "docs about the
KiCad export" — would miss any semantically-stronger claim phrased without
the word "parity" ("the exported net structure exactly matches the drawn
circuit," "round-trips losslessly," etc.), which is exactly the overclaim the
AC exists to prevent. As stated, the check is either unimplementable as a
mechanical test or trivially gameable by wording around the trigger word.
**Recommendation:** scope AC-3 explicitly to a defined set of files/sections
(e.g., only the netlist-export doc(s) and the emitter header), and specify
the check as "these N files are the only place net-partition parity may be
claimed, and each claim in them must cite a fixture-class line" rather than
an unbounded repo-wide semantic scan.

### 5. Nothing in this chain exists yet; #630 is two dependency hops from any code
There is no `jls.netlist` package (#336), no KiCad/gEDA emitter (#366, #365),
no `NetPartitionIsomorphismTest` (#627/TASK-C523-1, still open), and — per
`grep -rli kicad src/` (as cited by #307/#298 themselves) — zero KiCad-related
code anywhere in `src/`. #630's `ordering_after` names only #627, but #627
itself lists `ordering_after: [336, 365, 366, 460, 468]` — five more open
issues. #630 is therefore ordered after a chain of at least six unimplemented
prerequisites. AC-1 ("pinned by the emitter golden") requires an emitter and
a golden test that don't exist; AC-2 requires the test from #627 to already
be running and know its own green/red fixture set. This is fine as a backlog
entry but the issue reads as independently actionable — a task an engineer
could pick up today — when in truth almost none of its acceptance criteria
can be satisfied, let alone verified, until the rest of the chain lands.
**Recommendation:** no correctness issue, just a clarity one — the issue
should say plainly (as #627 does in its own ordering block) that this cannot
be started, only drafted in prose form, before #627 merges.

### 6. AC-1's "does not imply anything stronger" is a subjective judgment pinned by a byte-exact golden — the golden protects drift, not correctness
"The emitted netlist's header states what parity is claimed... and does not
imply anything stronger. Pinned by the emitter golden, so the sentence cannot
drift out silently." A golden test can only assert the header text matches a
fixed string; it cannot judge whether that string is itself an overclaim.
If the initial sentence approved in review is subtly too strong (e.g. omits
that timing/value semantics are unchecked), the golden will happily keep it
byte-pinned forever, giving false confidence that the wording is being
verified when only its stability is. This is a real but modest risk —
flagging it rather than blocking on it.

### 7. Effort estimate looks light against the actual deliverable
`band_mw: "0.5-1"` for: an emitter header sentence, a release doc section
naming fixture classes with per-class blocking semantics, a new repo-scoped
prose/claim-consistency check (mechanical or reviewed), and a corpus-vs-doc
completeness ratchet test — while its prerequisite #627 (a single test file)
is separately estimated at 1.5-2 mw. Two ratchet-style tests plus two
documentation artifacts for 0.5-1 mw, on top of an already-unbuilt
dependency chain, likely under-costs the work, particularly the AC-3 scanner
(finding 4) which is the hardest part to get right.

## What's solid
- The core motivation — don't let a netlist emitter imply semantic/timing
  parity it never checked — is a legitimate and well-articulated engineering
  discipline, consistent with this repo's existing culture of ratchet tests
  (`HelpTopicsTest`, `ElementConstructorContractTest`, etc.).
- AC-2's "artifact against source, never walk against walk" instinct
  (inherited from #627/#336) is sound engineering practice for this kind of
  check.
- Explicitly absorbing #307's AC-5 discipline into #298's verification tier,
  rather than letting it drop when #307 closed as a duplicate, is good
  bookkeeping and traceable via the linked closing comment.
