# Issue #415: TASK-0032: every record kind declares how it merges, in one table with a test per row, and the offline merger becomes the CRDT's executable specification
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the apparatus and the claim is: *a `.jls` that two people edited should never
silently become a circuit neither drew.* That is the right goal, it is measured (O1 is
real and reproduces), and it is the kind of capability `docs/capability-roadmap/lf-06-diff-merge-vcs.md`
correctly identifies as absent from every surveyed peer tool.

But the issue equates that goal with a *merge semantics*: a two-column table of outcomes
per `(record kind, situation)`, a `jls.merge` package, a git merge driver, and a property
test binding it to the CRDT. That equation is the thing I want to break. Safety here is a
**validity property of the result**, not a **decision procedure over the inputs**. Those
are different artifacts with wildly different costs, and the cheaper one is already filed.

## 1. Both of this issue's measured failures are closed by its own prerequisites

This is the load-bearing observation, and the issue does not make it.

- **O1** (12-bit gate driving a 4-bit pin, settling to `0xfff`) is a net whose member puts
  disagree on width. `src/jls/elem/WireNet.java:230-233` folds widths with
  `Math.max(this.bits, bits)` — no comparison, no report. That disagreement is exactly
  `f_width` in **#409 §7.10** (TASK-0031), whose own O3 is *the same class*: "a 1-bit
  output wired to a 4-bit input loading clean with the net silently widened to 4."
  #409's `-check` reports O1 by name. No row of #415's table is required.
- **O2** (two elements claiming `int id 5`) disappears when **#436** (TASK-0005) lands:
  lf-06 C1 removes the `int id` line entirely and references name stable ids, and
  `Circuit.java:1310-1320` *already* rejects a duplicate `sid` as a hard `ELEMENT_ERROR`
  load failure. O2 is not a merge-rule gap; it is an artifact of positional identity that
  a filed prerequisite deletes.

So the issue's entire evidentiary base motivates **#436 + #409**, not **#415**. That is not
a defect in the measurements — they are excellent — it is a mis-attribution of what they
demand. §11 half-notices it ("the cheaper half — the validator — delivers most of the
safety and none of the surface") and then proceeds anyway.

## 2. The reframing: verify, don't merge

FEAT-012's acceptance criterion — *loads and elaborates, or is reported as a conflict;
no third outcome* — is a predicate on the **output**. Any merge procedure whatsoever
satisfies it if a validator sits behind it. Concretely:

```
merge = git's ordinary textual merge  →  jls -check merged.jls  →  refuse on findings
```

Cost: **#436 + #409 + roughly thirty lines** of driver glue. No `jls.merge` package, no
situation classifier, no rule table, no reflective totality test, no `ThreeWayMerge`, no
op-emission path, no `CausalBuffer` property harness. The stated failure mode is closed
for both measured cases on day one.

What the table buys *beyond* that is strictly narrower than the issue implies: it converts
some **refusals into successful merges**. That is a real and desirable improvement — but
it is an *optimization of the conflict rate*, and its value is a number nobody has
measured. Ship the gate, run it over a semester of real two-student edits, count the
refusals and bucket them by kind, and then build exactly the rows that pay. That ordering
also matches the project's recorded temperament everywhere else: plugins removed until
demand appears (#80), i18n a non-goal with a named revisit trigger, one simulation
strategy until a CPU-scale circuit is unusably slow (#221). #415 is the one place the
tracker proposes speculative mechanism ahead of measured demand.

**Reach asymmetry, which decides it.** A git merge driver is defined in `.git/config`, not
in `.gitattributes` — lf-06 says so in its own words ("hand-editing `.git/config` is where
adoption dies"). Every student, on every fresh clone, must run an install step or get
none of this. The validator runs **inside the load path**: #409 OQ4 recommends always-on
after `finishLoad`, which means every student who opens a merged file in JLS is protected
with zero configuration, and `CircuitSnapshot` restore is covered for free because undo
round-trips through save/load. For the audience this issue names — students on a shared
lab, an instructor merging a skeleton — the load-path gate has strictly higher reach than
the driver, at a fraction of the surface.

## 3. If the table is built anyway: cut it along a different seam

Two design changes I would insist on, independent of ordering.

**(a) STRICT and AUTO are one function and a parameter, not two columns.**
§7.10 already writes `R_auto = R_strict` with `conflict` substituted by a tiebreak — and
then §4 H2 makes that relation a *hypothesis to be tested*, with §10 planning what to do
when it is refuted. That is self-inflicted. Make it a construction:

```java
sealed interface Tiebreak permits Refuse, LastWriter { }
List<CircuitOp> merge3(Circuit base, Circuit ours, Circuit theirs, Tiebreak t)
```

`Refuse` returns a `Conflict`; `LastWriter(lamport, peer)` returns a winner. One code path,
two instantiations. H2 stops being falsifiable because there is nothing left to falsify;
P5 ("every AUTO outcome is total") becomes a type property rather than a test; and the
"two incompatible answers to the same question" risk the Abstract exists to prevent is
structurally impossible rather than conventionally avoided. This also dissolves the
ownership tangle with **#279** that the adversarial comment had to legislate: whoever lands
first writes the single resolver, and the other supplies its `Tiebreak`.

**(b) The policy belongs on the declaration, not in a side table.**
`jls.merge.MergeRules` as a standalone table keyed by record kind must be kept in sync,
by hand, with 35 element types, a frozen `SaveTags` table, a coming element registry (#78),
a format bump (#436) and per-section framing (#319). §11 admits it is "writing rules for
records about to be re-framed." The project already solved this exact drift class once:
`src/jls/elem/Attribute.java` — *"A single declaration drives saving, copying, and load
dispatch, so the three can no longer drift apart (the historical defect class: an attribute
added to save/load but missed in copy broke only paste and undo)."* Merge policy is the
fourth member of that family, and the sentence above was written about the fifth-place
defect it prevents.

Add `mergePolicy()` to `Attribute` and the sixteen-step "adding an element" checklist in
`ARCHITECTURE.md` gains it at the site where an attribute is born. "Undeclared is not a
valid state for a kind" — #415's own criterion — is then enforced by the compiler and by
element construction, and the reflective `theTableHasNoRowWithoutATest()` becomes
unnecessary scaffolding around a weaker guarantee. It also shrinks the table dramatically:
`docs/file-format.md:125-137` has **seven item kinds**, and the genuinely ordered payloads
are the five named in lf-06 C2.3 (`StateMachine` sequence, `TruthTable` pairs,
`Binder`/`Splitter` pairs, `Memory` init blob, `WireEnd` probes). That is ~7 generic rules
plus 5 element-owned exceptions — not a per-record-kind matrix, and every row lives next
to the code that saves it.

## 4. The best idea in lf-06 is the one this issue drops

lf-06 C5 proposes that on conflict the driver writes `alu.MERGE.jls` — the auto-resolvable
part merged, with **a `Text` annotation element placed at each conflict site**, plus a
machine-readable list and an overlay SVG. `Text` is an existing frozen tag; this needs no
format change and opens in stock JLS. The student *sees the conflict on the drawing* and
resolves it by editing a circuit, which is the thing they already know how to do.

§7.11 of #415 replaces that with "write nothing over the working tree, exit 1." Defensible
as a safety rule, but it discards the only part of this capability that is pedagogically
differentiating and that no competitor can copy — because it requires owning both the
format and the renderer, which JLS uniquely does (`CircuitRenderer.exportImage` already
emits byte-deterministic SVG, pinned by `SvgExportTest`). If any part of this work should
be prioritized on vision rather than measurement, it is the annotated conflict artifact,
not the rule table. Note it composes perfectly with §2's gate: `-check`'s findings already
carry the stable ids involved, so annotating them onto a circuit is a small function over
an existing typed output.

## 5. Where this pulls against the arc

- It opens a new top-level package (`jls.merge`) with its own record-kind classifier while
  #78 has not yet unified element metadata and #436/#319 are about to reshape what a record
  *is*. The classifier is a third opinion about element structure alongside `SaveTags` and
  `ElementRegistry`.
- It adds a permanent maintenance surface (a git driver, on three platforms, with the
  `recursive = jls` subtlety lf-06 flags) to a single-maintainer project whose own
  ARCHITECTURE.md records deferral as the default posture for speculative mechanism.
- Its headline justification (P8, the CRDT cross-check oracle) is not executable: #279 must
  land first, as the adversarial comment establishes. An issue whose Abstract rests on an
  oracle it cannot yet run is sequenced wrong regardless of its merits.

## Recommendation

I am **disregarding the stated acceptance criteria** in §14 as the near-term plan. They
describe a well-built version of the wrong next artifact.

1. Land **#436** then **#409**. Wire `-check` always-on after `finishLoad` and into the
   git merge path as a post-merge gate. FEAT-012's acceptance criterion is met, both
   measured failures are closed, and no new package exists.
2. Instrument it. Count refusals over real two-author edits, bucketed by finding category.
   That number is the specification for everything below.
3. Build the annotated `.MERGE.jls` conflict artifact (lf-06 C5) — the differentiating
   half — on top of `-check`'s typed findings.
4. Only then, and only for the buckets step 2 shows to be common, add merge policy **as a
   `mergePolicy()` declaration on `Attribute` and the element registry**, resolved by a
   single `merge3(..., Tiebreak)` that #279 and the offline path share.
5. Keep this issue open as the owner of step 4, retitled around the resolver and the
   declaration site rather than around a table; move the git driver to its own issue with
   an explicit demand gate.

The goal in the title is right and worth keeping verbatim. The deliverable underneath it
is the wrong shape, in the wrong order, at the wrong seam.
