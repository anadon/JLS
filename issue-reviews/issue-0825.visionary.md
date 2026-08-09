# Issue #825: TASK-C569-1: every supported extension point is enumerated in one published document with its type and a compiling example
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Trace the chain up. #825 serves #569, which serves capstone #514 (PF-4), whose
AC-6 is the only criterion with teeth: *"one external element jar built from the
published docs **by someone other than the maintainer** loads and runs."* The
end this task serves is not "a document exists." It is: **a stranger, holding
only what we published, gets code of theirs running inside JLS.** #569 AC-1
turned that into "enumerate the seams with an example each," and #825 turned
*that* into "the examples compile in CI." Each hop is a fair-looking
translation, and the last one drops the load-bearing verb. Compiling is not
loading.

## The fact that reframes the task

At HEAD the four typed seams are **fully typed and fully inert**, and there is
no path into them from outside the jar.

- `src/jls/JLS.java:60` calls `JlsModules.boot()` and **discards the return
  value**. Its own comment says why: *"this populates the typed extension
  registry with every built-in contribution … but nothing reads it for dispatch
  yet."* `JlsModules`' class javadoc repeats it: *"the registry is populated but
  nothing reads it for dispatch."*
- `JlsModules.modules()` (`src/jls/boot/JlsModules.java:64`) is a closed
  `List.of(new CoreModule(), new GuiModule(), new HdlModule(), new
  CollabModule())`. No `ServiceLoader`, no module path, no classpath scan.
- #212 — the only mechanism that would open any of this to an external jar — is
  **execution-gated on demonstrated demand** (milestone M4, standing "hold"
  decision inherited from #80). Its three tasks are deliberately unfiled.

So AC-1 asks the document to state, per seam, "what the runtime does with it."
The truthful answer today, for all four seams, is *nothing yet — the
contribution is accumulated at boot and never read*. And AC-3's "one worked
example each" can only be: construct your own `ExtensionRegistry`, contribute to
it, read it back — which is `ExtensionPointCatalogTest.bothHdlEmittersAreAcceptableExporterContributions`
(`test/jls/ExtensionPointCatalogTest.java:189`) already, relabelled as
documentation. That example compiles, passes CI, and misleads: a reader who
follows it produces a jar JLS will never load, and `-export` will never consult
their `HdlEmitter`.

**Publishing an example that compiles but cannot run is worse than publishing
nothing.** The adversarial comment correctly shrank AC-1/AC-2 to "examples plus
a check extension." I am going further: the examples are the wrong artifact at
the wrong altitude, and AC-3 as written should be disregarded.

## Reframing 1 (primary): the example is the deliverable; the document narrates it

This project has already solved this exact problem once, and better than #825
proposes. `examples/autograde/autograde.py` is a real, committed, runnable
consumer of a published contract; `test/jls/AutogradeBridgeExampleTest.java`
drives it in CI **over the real CLI** (spawning `jls.JLS`, grading a committed
fixture green, skipping cleanly when `python3` is absent); and
`docs/vcd-interop.md` is the informative prose *around* the working artifact.
That is the house pattern, and it sets a bar #825 falls below: the autograde
example is not compiled, it is **executed against the shipped surface**.

Concretely, invert the deliverable:

1. Ship `examples/extension/` — one provider, structured as an *out-of-tree*
   consumer would be (own `pom.xml`, depends on the published jar coordinates
   `io.github.anadon:jls`, no access to `test/` helpers, no in-tree shortcuts).
   Building it against the reactor and never against the source tree is what
   makes it a proxy for AC-6.
2. Pin it with an `ExtensionExampleTest` in the `AutogradeBridgeExampleTest`
   mould: boot the **real** path (`JlsModules.boot()` / `ModuleRuntime.boot`),
   assert the example's contribution is present in
   `ModuleRuntime.extensionRegistry()` and — the part that matters — that a
   *behavioural* consequence follows.
3. That last step is where the reframing pays: it forces the smallest possible
   slice of "the registry is read for dispatch." Make **one** seam live. Best
   candidate is `hdl.exporter`: it is on-command, headless, already contract-typed
   (`jls.hdl.HdlEmitter`), has two in-tree contributions, and routing
   `-export`'s emitter lookup through `registry.contributions(EXPORTER)` is a
   contained change with `HeadlessCoreRatchetTest` and `ArchitectureRulesTest`
   already guarding the blast radius.

A document whose examples merely compile costs a week and cannot fail in a way
that matters. A document whose single example *runs a third emitter through the
real export path* is the first honest evidence for #569 AC-4 and #514 AC-6, and
it converts a passive doc slice into the thing that finally makes the module
runtime observable. **Deliverable inversion: build the example, then write the
document from it.**

## Reframing 2 (mechanism): let the toolchain hold the binding, not a bespoke test

AC-2 (and the adversarial AC-2′) proposes extending `ExtensionPointCatalogTest`
so examples that stop compiling fail the build. The JDK already does this, and
this repo is already configured for it:

- `maven.compiler.release` is **25**, so `{@snippet file="…" region="…"}`
  (JEP 413) is fully available.
- `maven-javadoc-plugin` runs at `verify` with `doclint=all`,
  `failOnWarnings=true`, `-Werror`, and a deliberate cache-buster so the run
  can never be silently skipped (`pom.xml:543-585`).

Put the worked code in `examples/extension/` as ordinary compiled source, mark
regions, and reference those regions from each `ExtensionPoint` constant's
javadoc via `{@snippet file=…}` with `--snippet-path`. A missing file or region
is a doclint error under `-Werror` — a build failure, no new test, no
markdown-parsing regex. A drifting example fails to compile in its own module.

That also fixes the source-of-truth direction. `docs/extension-points.md` today
duplicates in a markdown table what
`ElementExtensionPoints.ELEMENT_PROVIDER` et al. already say in typed code with
good javadoc, and pays `ExtensionPointCatalogTest.docTableAndConstantsAgreeBothWays`
(~40 lines of table parsing) to keep the copy honest. Adding per-seam signatures
and examples to the markdown doubles down on the copy. The elegant move is the
opposite: **the constant's javadoc is normative per-seam; the markdown shrinks
to an index plus the three things javadoc structurally cannot say** — how you
get into the process at all, what the stability promise is (#569 AC-2), and
what you must not touch (AC-4). Every one of those three is a *whole-surface*
statement, not a per-seam one, which is exactly why the current table has no
column for any of them.

## Reframing 3 (goal): audience and gate

There is a live contradiction the task inherits without noticing. ARCHITECTURE.md
and #212 record a deliberate refusal to build external providers speculatively;
#212's "hold" is a standing decision reaffirmed each cycle. #825 proposes to
publish the front door of a house whose door is not built. Two coherent
resolutions, and the task should pick one explicitly:

- **(a) Retarget the audience to in-tree module authors.** The immediate,
  real, ungated readership is contributors writing the *next* `JlsModule` —
  #84's command module, #76's theme module, lf-07's `app.api` module (which
  proposes two further seams, `app.api-verb` and `app.report-writer`, and
  explicitly invokes `docs/extension-points.md`'s name-pending-seams-first
  rule). Write a module-author guide, keep it in `docs/`, and let #569 own the
  outward-facing publication when #212's gate lifts. This is honest, useful
  tomorrow, and costs nothing to redirect later.
- **(b) Declare #825 the demand-gate lever** — publish outward precisely to
  generate the demand #212 waits on. Defensible, but then the minimum honest
  unit is a *live* seam (Reframing 1's step 3), not a document, and #212's
  REPLAN protocol should record #825 as the requester.

Recommend (a) plus the one live seam. Do not publish an outward-facing "how to
extend JLS" page while `ServiceLoader` appears nowhere in `src/`.

## AC-4: the taxonomy is missing an axis, not a bucket

The adversarial comment notes `pending` is a third state beside "extension
point" and "internal surface." The deeper issue is that AC-4 collapses two
independent axes into one list. A reader needs:

- **Axis 1 — contract:** typed now / named-but-pending / internal (never).
- **Axis 2 — reachability:** contributable by an in-tree module / contributable
  by an external jar / read by the runtime at all.

Today *every* seam is `in-tree module only` on axis 2, and *no* seam is `read by
the runtime` — facts the current table cannot express because it has no such
column. A document that states axis 1 alone will read, to an outsider, as a
promise about axis 2. Add the column; it is one word per row and it is the only
part of this document that a stranger's afternoon actually depends on.

## Alignment with the larger arc

`docs/capability-roadmap/lf-07-api-and-platform.md` is emphatic that the
platform play is `jls.api` plus an NDJSON stdio face, and that the boundary
stays: *"scripts compose existing elements; new element behaviours are a Java
module."* Extension-point publication is the small tributary of that; the
valuable, transferable asset in #569 is not the enumeration but **the stability
statement** (#569 AC-2: frozen / evolving / internal, plus deprecation notice
policy). lf-07 independently specifies the same discipline for `jls.api`
(`docs/api-interface.md`, an API-surface ratchet, the `batch-interface.md` §6
promise). If #825 invents a per-seam stability vocabulary now and lf-07 invents
another later, the project ends with two incompatible promise languages over one
codebase — the same failure mode lf-07 already flags for structured-result
formats (its risk #2, and its P5/P7 co-design instruction). **Design the
stability/deprecation vocabulary once, in a form both surfaces adopt**, and have
#825 consume it rather than mint it. That single decision is worth more than
everything else in this task.

## What I am disregarding, and why

- **AC-3 ("examples are compiled in CI") — insufficient, replace.** Compiling
  proves the API's shape; it proves nothing about reachability, and for four
  seams that nothing reads, shape is the part that was never in doubt. Replace
  with: *at least one example is executed through the real boot and dispatch
  path, and the others are `{@snippet}`-bound compiled source.*
- **AC-1's "one worked example each" — forbid it for `pending` rows.** An
  example for `hdl.importer`, `app.command` or `gui.theme` would have to invent
  the contract those issues own, violating AC-5's own boundary rule. Pending
  rows get their owning issue and a "no contract yet — do not bind" line, never
  a sample.
- **AC-2 as a bespoke check — replace with the toolchain** (Reframing 2).
- AC-5's dangling `#399` is already corrected to #212 by the adversarial
  comment; I concur and add that with #399 absorbed, #825 and #569 AC-3 (the
  external-jar walkthrough) now overlap on the same unbuilt mechanism — worth
  deciding once, at #569, which of the two owns the walkthrough.

## Bottom line

Keep the outcome; rebuild the task. The residual after the adversarial comment
is small, and after this reframing it is small *and* aimed at the criterion that
actually matters: not "a seam is documented" but "a stranger's code ran." One
running example beats seven compiling ones, the JDK's own snippet mechanism
beats a second doc-binding test, and the reachability column beats another
signature table.
