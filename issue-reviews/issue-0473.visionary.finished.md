# Issue #473: TASK-0042: one elaboration pass turns definitions plus bindings into a resolved design, and every binding it cannot resolve is a coded diagnostic naming the instance path — never a silent default
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

Two goals are bundled here, and they have different value and different natural shapes.
The first is **make a bound parameter mean something** — the half of FEAT-017 (#357) without
which #447's declarations are decoration. The second is **give JLS a machine-readable
diagnostic vocabulary**, because a grading harness (#369) cannot regex prose. Both are right
and both strengthen the project's arc: the parameterization half unblocks the FPGA-deployment
trajectory (`docs/grand-architecture.md` §2, #59/#61/#385), and the diagnostics half is the
generalization of `ARCHITECTURE.md`'s "Error-reporting contracts" section that #404 and #466
are separately reaching for.

The design proposed to serve them, however, cuts at a seam that does not exist in this
codebase, and it invents a second diagnostic vocabulary rather than the one the project needs.

## 1. The central defect: the issue never decides whether elaboration flattens

Stage 4 (§7.10) is a fold of `copy(σ(D(def(i))))` over **every instance** into one `Circuit`,
followed by one `partition`. That is flattening — Verilator's elaborate-to-flat. The prose
around it (H2: byte-identity on save; §7.12: "every existing fixture's bytes are unchanged")
requires the exact opposite: hierarchy preserved, because `Circuit.save` writes nested
`CIRCUIT` blocks and `SubCircuit.save` writes the body inline. You cannot fold N nested
definitions into one flat element list and re-save the file you loaded.

Three independent facts say flattening is the wrong answer here:

- **`ARCHITECTURE.md` already records the decision against it** ("Simulation execution
  strategy: discrete-event interpreter is the sole strategy", #221): a levelized
  elaborate-to-flat pass is explicitly *not* built now, and if ever built it is a
  core-internal second strategy that must be observably identical to the event model.
  A flattening `Elaborator` on the `Circuit.finishLoad` path is that pass arriving through
  the side door, un-benchmarked and un-gated by that decision's revisit trigger.
- **JLS simulates hierarchically today.** `SubCircuit.initSim` (src/jls/elem/SubCircuit.java:577)
  and `initInputs` (:422) recurse into `getSubCircuit().getElementsInStableOrder()`; wire
  nets are per-`Circuit` and the `SubCircuit` element bridges its own puts to the inner pins.
  Nothing flattens, ever. Flattening at load would change probe/watched-element naming, which
  `docs/batch-interface.md` freezes, and would change what `CircuitSnapshot` (undo) restores.
- **#447 already answered the byte question in the opposite direction.** Its Open Question 5:
  *"Does an instance with no bindings and a definition with no parameters save byte-identically
  to a pre-split inlined instance? **It cannot — the body moves.**"* This issue calls H2 "the
  single most load-bearing statement," and its own blocker has already refuted it.

## 2. Reframing A — elaboration is *definition specialization*, not circuit construction

Change the output type and the whole design collapses into something smaller and truer:

```
elaborate : (DefinitionTable, Instances) -> (DefinitionTable', Diagnostics)
```

where every definition in `DefinitionTable'` is parameter-free (one specialization per distinct
binding vector, deduplicated), and every instance points at a specialized `DefinitionId`. This
is monomorphization, which is what "elaboration" means in every HDL toolchain the project cites.

What that buys, concretely:

- **H2 stops needing a test.** A design with no parameters has one specialization per definition,
  identical to the input by construction; identity is structural, not an empirical byte
  comparison against a fixture. P7 — the criterion #447 says cannot hold — is deleted, not
  argued about.
- **H3 (idempotence) is a fixpoint property of a parameter-free table.** The Yosys no-op (#61)
  is free rather than a claim needing a test.
- **Hierarchy survives**, so `finishLoad`, save, undo, probes, and the batch contract are all
  untouched. The scary blast radius disappears.
- **#385 stops being "related" and starts being served.** A hierarchical HDL IR wants exactly
  this: one module per specialized definition, N instances. A flattening elaborator would fight
  #385; a specializing one *is* its front half.
- **Cost is bounded by distinct binding vectors, not instance count.** Ten instances of an
  8-gate block with the same width produce one specialized body, not ten copies — which is
  precisely the duplication #447 exists to remove. The flattening design re-creates it in memory.

## 3. `Util.copy` is the clipboard, not a latent elaborator

O3 and H4 rest on the claim that `Util.copy`/`Util.partition` are "an implicit elaboration phase
that has never had inputs." The three call sites say otherwise: `SimpleEditor:4902` (copy to
clipboard), `SimpleEditor:5480` (import a file as a subcircuit), `SubCircuit.copy:351`
(duplicate an element). None is on a simulate or export path. They are a deep-clone utility for
editor gestures. Adopting them as "the elaborator's implementation substrate" (H4) imports
`HashSet` iteration order and geometry-normalization semantics into a pass whose entire value
proposition is determinism (H1). Under Reframing A the substrate question mostly evaporates:
specialization rewrites attributes on a definition body, and a copy is needed only when two
binding vectors must not share one body.

P9 also mis-specifies its rule: "no class outside `jls.elab` calls `Util.copy` on a subcircuit's
element set" either forbids `SimpleEditor:5480`'s import path — which is a legitimate editor
gesture, not an elaboration — or is vacuous. The property worth enforcing is "only one component
resolves bindings", which is a check on the resolver, not on `Util.copy`.

## 4. Reframing B — one project diagnostic type, not a package-local one

The issue states "there is no diagnostic type anywhere in the tree carrying a stable code."
`jls.LoadError` is a record with a frozen `Category` taxonomy, location, detail and an actionable
hint, published through `JLSInfo.setLoadError`, rendered by a documented CLI contract, and pinned
by `CircuitLoadErrorTest` / `LoadErrorReportingTest`. It is 90% of the thing being invented, one
field short (a structured location richer than `int line`) — and #472's `ItemKey` is exactly that
field.

Defining `jls.elab.Diagnostic` with its own `Severity` and `Code` guarantees a tree with a load
diagnostic, an elaboration diagnostic, whatever #466's batch report channel invents, and an
`HdlExportException` string — four shapes for one grading harness to consume. That is the
opposite of the issue's own stated purpose. The higher-leverage move, and the one that makes this
issue a spine item rather than a leaf: **land one `Diagnostic` value type in a shared headless
package, make `LoadError` a coded instance of it, and let elaboration be its first `ItemKey`-carrying
producer.** #404 and #466 then consume a type that already exists instead of each growing one.

Related: pre-numbering `E-ELAB-001..006` before #447's parameter type/range grammar is settled
designs the error vocabulary before the language, and the append-only rule makes an early bad cut
permanent. A harness pinning `ELAB_UNKNOWN_PARAMETER` gets everything a number gives it plus
self-documentation; the numeric form buys nothing here.

## 5. `E-ELAB-006` is a design-rule check, not elaboration

Stage 5 compares a resolved pin width against the attached net's width — which requires the
partition, which is why §7.3 and threat T4 exist, why #468 is a hidden dependency, and why H1's
purity claim needs an asterisk. But that check is not about resolving bindings at all; it is a
DRC over an already-resolved netlist, and the same check is valuable on designs with no
parameters whatsoever. Split it out: elaboration is a pure function of (definitions, bindings)
with no knowledge of wiring, and a separate lint/DRC pass consumes the elaborated design and the
partition. Purity becomes structural, T4 vanishes, stage ordering stops being a contract clause,
and JLS gains a check-the-drawing pass that instructors want independently of parameters.

## 6. Placement and sequencing

`jls.elab` as a new top-level sibling adds a package #77 will have to relocate. Elaboration is
model semantics; it belongs under the headless kernel (`jls.core`) that `grand-architecture.md`
§3 names as the keystone, not beside it.

On sequencing: #447 ships a format bump, a two-form reader epoch, and a parameter dialog that its
own Method step tells the author to label "does not yet reach pin widths." This issue is the only
thing that makes that dialog honest, and it sits behind two blockers. If the pair slips, the tree
ships a user-visible dead feature plus an irreversible format epoch. The risk-inverted route:
prove parameter→width resolution and its diagnostics **in memory, on the existing inlined model,
with no format change at all** — a subcircuit whose pin widths derive from a binding evaluated at
instantiation — then pay for the definition table once the semantics and the diagnostic vocabulary
are already proven. The expensive irreversible step lands last.

## 7. The headline this issue buries

`HdlExporter.buildModel` rejects `SubCircuit` outright today (src/jls/hdl/HdlExporter.java:88,
:191-199) — a hierarchical circuit cannot be exported at all. The pass being proposed is what
changes that, and that is the FPGA-deployment trajectory in `grand-architecture.md` §2. Framed as
"unresolvable bindings become coded diagnostics," this reads as a leaf hygiene task. Framed as
"hierarchical designs can reach Verilog and Yosys," it is a spine item — and it suggests a first
slice that needs neither #447 nor #472: a specialization/flattening adapter used **only** by the
export path, where losing hierarchy is legitimate and the batch contract is not at stake.

## What I am disregarding, and why

Explicitly setting aside these acceptance criteria: **P7** (`elaboratingALegacyInlinedFileIsByte
IdenticalToItsPreSplitSave`) and **H2** as the load-bearing claim — #447 OQ5 already establishes
the body moves, so the criterion is unsatisfiable as written and is replaced by structural
identity under Reframing A; **H4/P9** as stated, because `Util.copy` is the clipboard path and the
invariant worth enforcing is single-resolver, not single-copier; **stage 5 / `E-ELAB-006`**, which
belongs to a separate DRC pass; and the `jls.elab`-local `Diagnostic`, which should be a project
type or this issue makes the fragmentation it exists to prevent.

Kept intact and endorsed without change: elaboration reports and never repairs (O2's inversion is
exactly right); diagnostics are data, not prose (O4 is a fair indictment); ordered output (P5/O7);
totality of the entry point with the diagnostic-vs-exception split (§7.11); explicit-stack cycle
detection over the definition graph, which is real new obligation created by #447 (O6) and is
correct under either design; and the headless, `@NullMarked`, no-new-`BASELINE` discipline (P10).

**Verdict: rethink.** The goal is right and well-argued; the design should be recut as definition
specialization producing a parameter-free definition table, with the diagnostic type promoted out
of the package and the width check moved to a separate pass.
