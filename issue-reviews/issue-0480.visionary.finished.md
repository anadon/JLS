# Issue #480: TASK-0091: "can this design be built as a board?" gets a named-rule answer per finding, with the gaps JLS cannot close reported rather than hidden
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the PCB vocabulary away and #480 is not a board issue at all. It is the
first serious attempt in this tree to give JLS a **design-rule reporting
discipline**: a finding is a (stable rule id, severity, element, remedy) tuple;
findings are total over a closed rule enum enforced by an exhaustive switch;
the report is byte-stable, orderable, machine-readable, and round-trips through
its own reader; and rules JLS structurally cannot resolve are *named as gaps*
rather than silently passed. That discipline is excellent, it is exactly what a
grading-oriented tool needs, and nothing like it exists in the tree.

The problem is that #480 spends that discipline on the single narrowest
consumer JLS has — a PCB path gated behind three unlanded tasks (#400, #394,
#430) which are themselves under three unlanded features (#336, #349, #365) —
and builds it as `jls.pcb`-private machinery that no other consumer can reach.
The vocabulary is the asset; the eight rules are one client of it.

## Reframe 1: the spine is `jls.check`, and manufacturability is a rule pack

JLS already has **four** incompatible ways of answering "what is wrong with
this circuit", each invented locally:

- `src/jls/LoadError.java` — the only good one: fixed category taxonomy,
  location, detail, actionable hint, one publication point
  (`JLSInfo.setLoadError`), asserted on by tests (ARCHITECTURE.md, "Error-reporting contracts").
- `src/jls/hdl/HdlExporter.java:194-196` — a prose sentence assembled from
  per-element descriptions (`"Memory \"imem\" at (300,60); ShiftRegister at (2460,780)"`,
  built at `:1345-1350`) and thrown as `HdlExportException`. That *is* a
  findings list — with no rule id, no severity, no machine rendering, and no
  way for a grader to consume it. `docs/capability-roadmap/sweep-06-physical-boundary.md`
  quotes it verbatim as the reason the flagship RV32I design "cannot reach step
  one of the open flow".
- `src/jls/elem/WireNet.java:443-460` — the bus-conflict notification (#98 S1),
  told to the user once, at runtime, through `TellUser`.
- #430's `LoadingReport` and #480's `GateReport` — shapes five and six, arriving
  simultaneously and by construction unable to share a renderer.

The issue itself argues that two loading checks that can disagree is worse than
one (T3). The same argument, one level up, says six *report shapes* that cannot
be composed is worse than one. The reframe:

```java
package jls.check;                 // AWT-free, core-adjacent, no jls.pcb dependency
public interface Rule { String id(); Severity severity(); String rationale(); }
public record Finding(Rule rule, String elementStableId, String message) {}
public final class CheckReport { List<Finding> findings(); String renderText();
                                 String renderMachine(); boolean hasErrors(); }
public interface RulePack { String name(); List<Finding> check(CheckContext ctx); }
```

Everything #480 fights for survives verbatim — stable ids, severity, ordering,
byte stability, tab-separated rendering with a reader, report-don't-throw,
totality — but it is now available to the HDL exporter (whose refusal message
becomes `HDL1_UNSUPPORTED_ELEMENT` findings a grader can parse), to #430's
loading check (which stops being a bespoke `LoadingReport` and becomes a rule
pack, making M6's "re-report, don't recompute" structural instead of a
prohibition in prose), and to whatever else lands. `ManufacturabilityRules`
becomes a rule pack that contributes M1–M8. This is also the shape the project
already committed to for seams: typed constants in their home package,
contributions through a registry, cross-checked by a catalog test
(ARCHITECTURE.md, "Extension points: the typed seam catalog", #223,
`docs/extension-points.md`). #480 reinvents that pattern privately.

The exhaustive-switch trick (H4/P10) does not survive a general `Rule`
interface — but it does not need to: put `severity()` and `rationale()` on the
rule itself, and a rule *cannot* exist without a message. That is a stronger
mechanism than a switch with no default arm, because it is unforgeable rather
than merely uncompilable-if-forgotten.

## Reframe 2: the emitter should not have its own refusal

7.4 and the #460 relationship are stated as a clean division: "the emitter
refuses, the gate explains." Read structurally, that is two implementations of
"is this design buildable" — the exact failure the issue forbids for loading.
A design that the emitter accepts and the gate rejects (or the reverse) is the
T3 hazard with a different noun. The elegant version: **the emitter's
precondition *is* the gate.** `KiCadNetlistEmitter.emit` calls
`ManufacturabilityRules.check(...)`, and refuses on `hasErrors()`, returning the
findings inside `HdlExportException` (or its `jls.check` successor). Then there
is exactly one predicate for buildability, `-manufacturability` is "run it and
print", and FEAT-042's integration criterion 6 ("a fixture that violates a named
rule is refused before the artifact is written") is satisfied by construction
rather than by two code paths agreeing.

## Reframe 3: M7 ships now, and it is not a PCB rule

`M7_BIDIRECTIONAL_UNMODELED` — a `TriState` on a net that also has a
non-tri-state driver — needs **none** of the four blocked inputs. JLS computes
the ingredients on every edit today: `WireNet.recheck()`
(`src/jls/elem/WireNet.java:272-300`) walks a net's ends and sets `triState`
when *any* attached output reports `isTriState()`. A mixed net therefore takes
the tri-state path in `propagate` (`:454`), where the comment states the
semantics plainly: "the first active driver in net order (the order the wire
ends were added to the net — **file order for a loaded circuit**)". A student
with a mixed net has a circuit whose simulated behavior depends on save order.
That is a silent-wrong *simulation* bug, live in the tree, affecting every user
— not a PCB-export concern affecting the handful who will ever emit a netlist.

Under #480 as filed, that detector is unreachable until three tasks and three
features land. Under the reframe it is one rule in a `jls.check` core pack,
shippable this week, and it becomes M7 for free when the PCB path arrives.
The same is true of M8's structural half (a word-level `Adder`/`Mux`/`Memory`/
`RegisterFile` that no cascade decomposition covers) and, in weaker form, of
M2 — "this element has no physical realization" is a total-disposition question
about the element registry, which FEAT-042 §5 criterion 5 already says belongs
at the registry boundary, not in a PCB gate.

**Sequencing consequence:** the highest-value hour in #480 is not blocked by
#400/#394/#430 at all. Land the spine plus the two unblocked rules first; the
PCB pack then costs the six predicates that genuinely need the new data.

## The CLI seam is wrong, and one bit of it damages a stability contract

- **Exit 2 is not available.** `docs/batch-interface.md:36-42` and
  ARCHITECTURE.md fix the classes: 0 = run completed, 1 = runtime failure,
  2 = **usage error**. §7.1/P8 call 2 "the established batch exit-code class"
  and reuse it for "the design has ERROR findings". A grading script then cannot
  distinguish a flag typo from an unbuildable board — and the document says
  "grading scripts should treat exit status as the failure signal". Exit 1 is
  the honest reuse ("the run completed, the answer is no" is arguably 0 with
  findings on stdout); 2 is a contract regression dressed as reuse.
- **A flag per analysis does not scale.** `JLSStart.FLAGS`
  (`src/jls/JLSStart.java:759-788`) already carries thirteen rows. Adding
  `-manufacturability`, then `-loading`, then whatever #427 wants, gives JLS a
  CLI that grows one verb per rule pack. One `-check[=pack,...] <out>` flag over
  the `jls.check` registry covers all of them, keeps `CliFlagTableTest` green,
  and gives graders one output format to learn instead of N.

## What I would keep untouched

The issue's judgment is good where it matters most, and none of the reframe
touches these: M4 as a WARNING with the honesty sentence (H3) is exactly right
and is the best paragraph in the issue — a gate that fails every real design is
a gate people disable; `check` reporting rather than throwing (H2); rule ids as
an append-only published vocabulary (§7.12); the refusal to grow into DRC/LVS
(T1), which matches `sweep-06`'s finding that computing physical data is another
tool class while *reporting on* it is not; and P3's rule-to-fixture totality
assertion, which is the deliverable's real spine.

## Disregarded acceptance criteria, and why

I am explicitly setting aside three of §14's items:

1. **"Every `blocked_by` entry has landed"** — as written this makes the whole
   issue undeliverable for months, including the parts that are not blocked.
   Split: the spine and the unblocked rules (M7, M8-structural) have no
   dependencies and should not wait on them.
2. **"Adding a ninth `GateRule` constant fails compilation" (P10/H4)** — a good
   mechanism for a closed enum, obsolete under an open `Rule` interface where
   severity and rationale are constructor obligations. Keep the *property*
   (no rule without a message), drop the specific mechanism.
3. **"`jls.pcb` package"** — the placement, not the content. `jls.pcb` should
   hold the PCB rule pack; the report vocabulary belongs somewhere every
   consumer can reach.

## Verdict

**endorse-with-reframing.** The claim — that "it exported" and "it can be built"
are different sentences, and that the difference must be said per named rule
with the gaps admitted — is right and strengthens the project's arc. But the
arc is larger than PCB: JLS's whole batch/grading identity wants one
machine-readable finding vocabulary, and #480 is the first issue with the taste
to build one. Build it as `jls.check`, make manufacturability its first rule
pack, let the netlist emitter refuse by asking it, ship M7 immediately as the
simulation-order hazard it actually is, and put the CLI on one `-check` verb
with an exit code that does not collide with usage errors.
