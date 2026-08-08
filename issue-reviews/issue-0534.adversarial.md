# Issue #534: FEAT-C23-5: a triggering logic analyzer and a programmable word generator are drawable elements — they serialize with the circuit, fire on edge/pattern/duration with pre-trigger capture, and export through the existing VCD path
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#534 is PF-5 of capstone CAP-23 (#504): a logic-analyzer element and a
word-generator element, both drawable, both serializing with the circuit,
both headless-drivable. The shape of the issue (metadata block, AC list,
boundary notes) is disciplined and the AC-1/AC-4 criteria are genuinely
solid. But two of the five acceptance criteria are either unverifiable as
written or self-referential, the issue asserts a dependency-free ordering
that its own AC-2 contradicts, and it never engages with an existing
element (`SigGen`) that already does most of what the "word generator" is
described as doing.

## Findings, most severe first

### 1. AC-2 requires an asset ("the shipped hazard-demo") that is not shipped

> "firing on the shipped hazard-demo's runt pulse opens the chronogram
> centered on the capture (CAP-23 AC-1 leg)"

I searched the working tree for any hazard/glitch demo circuit:
`grep -ril hazard` over the repo, `find . -iname "*hazard*"`, and a listing
of every `.jls` file (`test/fixtures/riscv-sum1to10.jls`,
`test/fixtures/fork-4.6-shiftregister.jls`,
`test/fixtures/headless-canary-gate.jls`, `riscv/gui/cpu.jls`) — none is a
hazard/glitch demo, and `examples/` contains only `examples/autograde/`.
No such fixture exists today. CAP-23 (#504) §1 step 1 makes the same
presupposition ("Open the shipped hazard-demo circuit (a classic static-1
hazard)"), so this isn't unique to #534, but #534 treats "the shipped
hazard-demo" as ground truth for its own AC-2 without listing "author and
ship a hazard-demo fixture circuit" as in-scope work or a dependency.
**Recommendation:** either add fixture creation to this issue's scope
explicitly, or add an `ordering_after` / boundary-note dependency on
whichever issue is expected to ship it, and cite that issue by number.

### 2. AC-2 depends on the chronogram (PF-1), but `ordering_after: []` claims no dependency

AC-2's pass condition is "opens the chronogram centered on the capture."
The chronogram is PF-1 in #504, a **different, not-yet-filed** planned
feature (#504's `requires_features: []`, with `planned_features` "resolve
via REPLAN when filed"). #504's own mermaid graph has no edge from PF-1
into PF-5, so #534 is right that *implementation* doesn't need PF-1 — but
its acceptance criterion does: there is nothing to "open… centered on the
capture" until PF-1 exists. #534's front-matter declares
`ordering_after: []`, i.e. no filing/landing order is asserted against any
other feature. As written, AC-2 cannot be verified until PF-1 lands,
regardless of what `ordering_after` says. **Recommendation:** either scope
AC-2 down to what PF-5 alone can prove (arm/capture/pre-trigger-history
correctness, independent of any panel) and move the "opens the chronogram"
half into whichever issue lands PF-1 or into CAP-23's own AC-1, or add PF-1
to `ordering_after` and accept the sequencing cost.

### 3. AC-5's tolerance is circular and never quantified

> "kernel throughput without an armed instrument matches baseline within
> AC-5's tolerance."

The clause names its own criterion as the source of the number it needs.
I searched `ARCHITECTURE.md`, every file under `docs/`, and the whole repo
for a definition of the "K9" tag CAP-23 AC-5 also invokes
(`grep -rn "\bK9\b"` over all markdown — zero hits) and for any existing
performance-ratchet test pattern to borrow a convention from
(`grep -rln "PerfRatchet|BenchmarkTest|nanoTime|throughput" test/jls/*.java`
— only `SpatialIndexTest.java`, which is not a throughput ratchet). There
is no numeric tolerance anywhere in the repo, and no precedent
timing-based test to model one on. This is not just underspecified, it's
gameable: an implementer can write any threshold they like and call it "AC-5's
tolerance," and a CI reviewer has nothing outside the PR itself to check it
against. It's also a real feasibility risk on its own terms: this would be
the project's first wall-clock-timing test, on shared CI runners, with the
usual flakiness that brings. **Recommendation:** either pin a concrete
number/method (e.g., "≤2% regression vs. `BatchSimulationGoldenTest`'s
existing timing harness, median of N runs") before work starts, or state
plainly that this is deferred to implementation-time REPLAN — but don't
let the AC cite itself as its own definition.

### 4. "Without an armed instrument" is ambiguous between two very different tests

Does the baseline-throughput comparison mean (a) a circuit with **no**
analyzer/word-generator placed at all, or (b) a circuit with an instrument
**present but not armed**? (a) is a trivial no-op comparison that proves
nothing about the feature's actual cost and would pass even if merely
*placing* an unarmed instrument silently taxes every simulation tick. (b)
is the real claim CAP-23's own risk #2 is worried about ("a
consumer-observing tap must be zero-cost when no chronogram is open").
As written, a submitter can satisfy AC-5 by testing (a) and never touch
(b). **Recommendation:** state explicitly that the baseline circuit
contains a placed-but-unarmed instrument, matching CAP-23 risk #2's
framing.

### 5. No engagement with `SigGen`/`TestGen`, the element the "word generator" overlaps with

`src/jls/elem/SigGen.java` is already a drawable, saveable "Signal
Generator" element ("Title drawn inside the element's box... Signal
Generator") that drives outputs from a user-entered signal specification,
and `src/jls/elem/TestGen.java` is its headless/batch-mode stand-in,
explicitly documented as "used only in batch mode as a replacement for a
signal generator" and consumed by the `-t` test-vector path
(`docs/batch-interface.md:63`: "any signal generators in the top-level
circuit are…"). #534 describes the word generator as "a stimulus table
driving its outputs," which is functionally what `SigGen`/`TestGen`
already do end-to-end — placed, wired, saved/loaded, headless-drivable.
Neither #534's body nor its boundary notes mention `SigGen`/`TestGen` at
all, so a reviewer cannot tell whether this issue means to (a) extend
`SigGen` with word-table semantics and trigger integration, (b) replace it
outright, or (c) ship a second, parallel stimulus mechanism that
duplicates existing save-format surface and batch-path plumbing. Given
`SaveTags`/`ElementRegistry`'s totality requirements
(`src/jls/elem/ElementRegistry.java`: "every concrete Element subclass...
must be registered"), landing a redundant element is a real, checkable
cost, not a hypothetical — and duplicated stimulus mechanisms is exactly
the kind of drift `AllElementsRoundTripTest`/the tag-freeze rule exists to
catch late, not early. **Recommendation:** the issue must state its
relationship to `SigGen`/`TestGen` explicitly before work starts.

### 6. Cost band likely undersells scope for two new elements plus a new test category

`band_mw: 3-4` matches #504's own PF-5 line item ("PF-5: 3–4 mw"), so it's
internally consistent with the capstone table — that's a point in the
issue's favor, not against it. But ARCHITECTURE.md's own "Adding an
element today" list still enumerates roughly a dozen touch points per new
element type even with `ElementRegistry` now landed (confirmed present at
`src/jls/elem/ElementRegistry.java`, though `ARCHITECTURE.md:117` still
says "There is no element registry yet — issue #78 will introduce one",
i.e. the doc is stale and undercounts what's already available — a wash,
not a point against #534 specifically). #534 bundles **two** new element
types (analyzer + word generator), three trigger modes (edge/pattern/
duration) with a configurable pre-trigger ring buffer, a new headless/
interactive equivalence golden (`InstrumentGoldenTest`), and the first
throughput-ratchet test in the project (see #3 above) — all inside the
same 3-4 mw band that CAP-23's sibling PF-1 (a chronogram panel alone,
no triggering logic) costs 4-6 mw for. The unbounded "configurable
pre-trigger history" additionally carries the same unbounded-memory-vs.
-lost-window risk CAP-23 flags explicitly for PF-3's cause-chain retention
(risk/kill-criterion KC-23-2), but #534 sets no size cap or kill criterion
of its own for the analyzer's capture buffer. **Recommendation:** either
tighten scope (e.g., defer pattern/duration triggers to a follow-up, ship
edge-trigger only for the capstone's step 1) or revisit the band with the
per-element checklist and the new test category priced in explicitly, and
add an explicit bound (or an explicit "unbounded, revisit if memory
becomes a problem" decision) for pre-trigger history size.

## What's solid

- **AC-1** (byte-identical save/load round-trip) reuses the existing,
  proven pattern (`CircuitRoundTripTest`, `AllElementsRoundTripTest`) and
  is concretely testable as stated.
- **AC-3**'s naming (`InstrumentGoldenTest`) and byte-for-byte
  interactive-vs-headless VCD equivalence claim match CAP-23 AC-3
  precisely and follow the same golden-comparison idiom already used by
  `VcdExportGoldenTest`/`BatchSimulationGoldenTest`.
- **AC-4** (VCD-only, no FST) is correctly and precisely traced to CAP-23
  Open Question 5's recorded stance — no drift, no new format proposed.
- **Grading/CI boundary** ("grading builds only on recordings, #498 §7.2")
  is stated correctly and doesn't ask these instruments to touch the
  grading path — a real, checked non-goal, not just an assertion.
- **The #534/#538 cross-issue comment** on this issue correctly scopes the
  VCD producer/consumer boundary and declines to over-adjudicate the other
  issue from here — good issue hygiene, no action needed from this review.
