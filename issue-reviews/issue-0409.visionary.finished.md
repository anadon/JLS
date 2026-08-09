# Issue #409: TASK-0031: a file that parses but is structurally corrupt is reported by name, so a merge can never hand back a circuit nobody drew
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the apparatus away and the claim is: *JLS knows what a legal circuit is, but only
the editor's mouse knows it.* Every other way a `Circuit` comes into existence — file
load, undo restore through `CircuitSnapshot`, a netlist import, a collab op from a peer,
a git merge — bypasses that knowledge. The end state worth wanting is not "a checker
exists"; it is **one authority on circuit legality, consulted by every constructor of a
circuit.** Judged against that, #409 is directionally right and structurally wrong in
three places.

## Reframe 1 — the flagship defect is a loader bug, not a missing reporting layer

Open Question 1 recommends default **(a)**: have `WireEnd.init` stash an
unresolved-declaration residue so `SemanticCheck` can *narrate* the loss, and file the
nesting fix separately. That is backwards, and the issue's own O2 says why: the load
"succeeds," the attachment is dropped, **and the re-save persists the loss**. Option (a)
buys an end state where JLS prints a finding about the corruption *and still writes the
corrupted file back to disk*. A loud diagnostic beside silent data destruction is worse
than either alone.

Option (b) — hoist the `if (loadPut != null)` block out of `next:for (int elid :
loadWires)` at `src/jls/elem/WireEnd.java:102-104` — is rejected as "changes load
behaviour and would make previously-lossy files load differently." That objection has
the sign inverted. Loading them *losslessly* is the repair. And the blast radius is
provably nil for files JLS itself writes: an end carrying `attach`+`put` and zero `ref
wire` items is not editor-producible (the editor creates ends in wire pairs; the four
`SimpleEditor.canConnect` overloads at `:3992`, `:4109`, `:4229`, `:4346` never yield
one), so §7.12 claim 1 and P5 hold trivially. The fix also deletes the N-times
`p.setAttached(this)` re-entry the issue notes as "harmless only because."

**Concretely:** the flagship deliverable of TASK-0031 should be a ~5-line nesting fix
plus a regression test asserting the round-trip preserves `ref attach`. That is a
same-day PR. Then `SemanticCheck` asserts the invariant rather than inventing a
breadcrumb field to report its violation — which is what a check layer is *for*, and it
removes the "blocks execution" flag from OQ1 entirely.

## Reframe 2 — do not mint a fourth encoding of "legal"; extract the first

Legality is currently written down in at least three independent dialects:

1. `SimpleEditor.canConnect` ×4 — gesture time, `overlapMessage` strings. The width rule
   `if (bits1 > 0 && bits2 > 0 && bits1 != bits2)` is copy-pasted four times (#356 §1
   pins the line numbers).
2. `jls.collab.op.*` validators — `AddWire.validate` (`src/jls/collab/op/AddWire.java`
   ~`:320-365`) already checks named-put existence, put-is-free, single-claim, and
   per-net tri-state agreement; `AddElements.validate` (`:95-159`) already checks
   stable-id freshness and **element-name collision**, which is one of #356's two named
   silent classes. Both throw `OpRejected`.
3. `jls.SemanticCheck` — proposed, with its own record, its own enum, its own text.

So four of #409's six checks are *already implemented*, in the op layer, against a live
`Circuit`, with the same "validate before mutate" discipline. §12 claims "this task owns
the post-load check and nothing else," but nothing in the issue reconciles it with
`AddWire.validate`. §11's threat — "drift between `SemanticCheck` and the simulator would
surface first on tri-state circuits" — is a self-inflicted wound: drift is only possible
because a second copy is being written.

This project's whole architectural style is single-authority-per-contract:
`SaveTags.resolve` is the only tag table, `JLSStart.FLAGS` is the only flag table,
`TellUser` is the only dialog site, `ElementRegistry` collapsed the per-element switch,
and `ArchitectureRulesTest`/`NotificationRatchetTest` exist to keep those singletons
singular. A `SemanticCheck` that restates `canConnect` and `AddWire.validate` pulls
directly against that arc.

**The seam to cut along:** `jls.CircuitInvariants` — a headless, AWT-free module holding
each rule *once*, expressed over circuit state rather than over a gesture. Then:
`canConnect` calls it (four width copies collapse to one), `AddWire`/`AddElements`
validators call it, the post-load pass calls it, `-check` calls it, and TASK-0032's merge
driver calls it. The load-path deliverable becomes a thin *consumer*, which is a smaller
task than the one filed, not a larger one. Add a ratchet test in the
`ArchitectureRulesTest` family asserting the width/attachment predicates appear at
exactly one call site — that is how JLS keeps authorities single, and it is how §11's
drift threat is *eliminated* rather than "watched."

## Reframe 3 — one diagnostic record with a severity, not a second taxonomy

§7.1 says findings report "through the existing `LoadError` channel, not a second
reporting mechanism." §7.4 then defines a new `Finding` record. Those contradict. The
STAGE-3 comment already flags that three diagnostic vocabularies (#409, #404, #608) are
being minted concurrently over adjacent surfaces and hopes they will agree by convention.

`LoadError` is already `(Category, detail, line, element, hint)` — category, location,
explanation, remedy. Adding `severity` (`REFUSED` | `REPORTED`) plus a stable-id set
covers everything §7.6 asks for, publishes through `JLSInfo.setLoadError` unchanged, and
makes P8 (the four existing rejections stay load failures) a *field value* rather than a
layering rule that O6's catch-all keeps threatening. It also gives #404 and #608 a home
instead of a coordination memo. The "no `default:` arm so a seventh category is a compile
error" ritual becomes unnecessary — `LoadError.Category` already carries that property
and already has tests asserting on it.

## Reframe 4 — the totality question has a better form than "six classes"

H1 enumerates six corruption classes and §10 concedes it is refuted by any scenario
nobody thought of. An enumeration can never be shown total. But #356 §4 invariant 4
already states the sharper property: *a merge expressed as ops cannot produce a file JLS
refuses to load.* Turn that around and the load-path question becomes: **is this circuit
reachable by some sequence of legal `CircuitOp`s?** That is total by construction, it is
the same predicate the collab layer must enforce anyway (#170, #171, #279), and it makes
"which invariants are format-level rather than editor-level" (#356 OQ3) answerable rather
than a standing debate. I am not proposing #409 implement op-reachability — that is a
feature. I am proposing the check set be *derived from* the op validators' preconditions
rather than from a list, so that every rule the op layer enforces is automatically a rule
the load path enforces, and the six classes become an observation about today's
validators rather than a hypothesis about corruption.

## Reframe 5 — the TASK-0005 (#436) dependency is spec-induced, not natural

`blocked_by` cites #436 because *one* of six checks — dangling `sref` — is phrased over
an item kind that does not exist. Five of six need nothing from it. #356 §6 says the
three-link chain TASK-0005 → 0031 → 0032 "is not parallel, and that is a real property."
It is not: restating the reference-integrity finding over whatever reference form is
current, or deferring that one check to #436's own PR, unblocks this task today and
deletes a link from the only chain on the feature's critical path. Given #356 OQ2's own
recommendation — "ship the validator first and independently" — the ordering as filed
contradicts the parent's stated strategy.

## The corpus is not an oracle

§11 admits it: three fixtures, one of which #356's sibling D5 deletes. H2 as written is
close to unfalsifiable-in-practice. The repo already has far better false-positive
oracles that the issue does not name: `AllElementsRoundTripTest`,
`GenerativeRoundTripFuzzTest`, `ElementSimulationGoldenTest`/`BatchSimulationGoldenTest`
circuits, `RiscvCpuGoldenTest`, and — best of all — the op layer, which can *construct*
arbitrary legal circuits. "Every circuit any sequence of accepted ops can build produces
zero findings" is a real H2; "three files are clean" is not.

Related: `HdlExporter` folds net width with `Math.max` too (`:254`, and per-element
`Math.max(...,1)` throughout). So the width disagreement of O3 does not merely mis-size a
net — it silently propagates into exported Verilog. That is a second consumer that has
independently guessed at an invariant nobody owns, and it is the strongest argument in
the issue that #409's *width* check is genuinely needed. It belongs in the issue as
evidence.

## What I endorse unchanged

- The `-check` flag joining `JLSStart.FLAGS` with a distinct exit code, findings on
  stderr, stdout reserved (§7.1, OQ3). Correct, and consistent with `docs/batch-interface.md`.
- Purity (P6/H3) asserted against `DeterministicSaveTest`'s canonical writer.
- Reporting rather than rejecting, so `P_accepted` shrinks only via #356's arming ratchet.
- Wiring `CircuitSnapshot` restore (OQ2, default yes). Anything less leaves undo unguarded.
- Not paraphrasing multi-driver legality from `docs/simulation-semantics.md`.

## Acceptance criteria I am explicitly disregarding

- **OQ1 default (a).** Take (b) and fix `WireEnd.init`. Reporting a bug you could fix, in
  a run that then persists the damage, is not the end state this project wants.
- **§7.4's new `Finding` record and its category enum.** Extend `LoadError` with a
  severity instead; it satisfies §7.6, §7.11 and P8 with less surface and folds #404/#608 in.
- **§12's "this task owns the post-load check and nothing else."** The task should own the
  *extraction* of `jls.CircuitInvariants` from `canConnect` and the op validators. That is
  the change that makes every later consumer — merge driver, CRDT, importer — correct for
  free, and without it TASK-0031 ships a fourth opinion about what a legal circuit is.
- **The `blocked_by` on #436** for five of the six checks. Only the dangling-reference
  finding (P3) needs it, and this issue's own DoD already licenses the waiver — "P3
  explicitly deferred and its successor named." The successor is named already: #436
  §7.11 commits to *refusing* a dangling `sref` at load, and #436's Related Work calls
  that "this task's local case" of the general pass #409 owns. Keeping the edge instead
  inherits #436's own `blocked_by: [315]`, so the real chain is 315 → 436 → 409 → 415 —
  four deep, on the critical path of the one feature #356 already concedes is not
  parallel.

## Verdict

**endorse-with-reframing.** The capability is real and the flagship evidence is
excellent: O2 catches a data-destruction bug red-handed, and O3's silent widening is
worse than the issue knows, because `HdlExporter` folds net width the same way. But the
issue prices a reporting layer where the first defect wants a five-line fix, and it mints
a fourth statement of "legal circuit" in a codebase whose entire style is one authority
per contract — `canConnect` says it in four copies, `AddWire.validate` and
`AddElements.validate` say it against a live circuit, and `SemanticCheck` would say it
again with its own record and its own enum. Re-cut it: fix the `WireEnd.init` nesting;
extract `jls.CircuitInvariants` and make `canConnect`, the op validators, the load path,
`-check` and TASK-0032's merge driver consumers of it; give `LoadError` a severity rather
than stand up a second taxonomy; derive the check set from the op validators'
preconditions instead of from a list of six; and let the op layer construct H2's corpus.
What is left is a smaller task than the one filed, it is unblocked today, and every later
constructor of a circuit — merge, CRDT, importer, undo — gets the same answer for free.
