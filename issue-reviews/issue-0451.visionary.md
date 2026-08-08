# Issue #451: TASK-0054: a Logisim-Evolution .circ file opens in JLS, and every construct that did not survive is named, located and explained
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this is really for

Not a `.circ` parser. The end is: **an instructor with a decade of Logisim labs can find
out, before committing, exactly what they lose by moving to JLS — and then move.** #323
says this plainly ("the only item in the plan that moves *users* rather than files"). I
endorse that goal without reservation; it is the highest-leverage thing on the roadmap and
the one capability no amount of simulator polish substitutes for.

The issue, however, has picked the wrong body for that goal. Its mass sits on a
bookkeeping identity (`Reported = Seen \ Realized`) and on a report artifact, while the
thing that decides whether a lab actually migrates — that the imported circuit is
*connected the same way the source was* — appears only as a one-fixture aside (P1) and as
a snap-vs-relayout report field (Stage 5). #323 §3 is unambiguous about which is the
correctness assertion: `P(imp(f)) ≅ P_src(f)`, compared by net count **and** membership,
per file, mechanically, because Logisim's connectivity is *purely geometric* through a
per-component port-offset rule `δ(k, n, s)`, and "an importer that does not replicate δ
exactly produces circuits that import **silently disconnected** — the worst failure mode
available, because the file opens and looks right." #451 never names δ, never names the
partition comparison as a harness, and its corpus test (P9) records accept/reject counts
rather than partition agreement (#323's I1). A report that is provably total about a
circuit that is silently disconnected is a beautifully audited wrong answer.

## Where it pulls against the project's arc

**1. It performs the emission route its own parent explicitly rejected.** #323 §2
alternative 2: *"Emit save text and reparse it, instead of building circuits
programmatically. **Rejected**: it makes the importer's correctness depend on the save
grammar and produces a circuit nobody validated structurally."* #451 §7.4 does exactly
that and defers the fix to #412. But the construction verbs are not hypothetical: eleven
of them ship today in `src/jls/collab/op/` — `AddElements`, `AddWire`, `RemoveWire`,
`SetElementConfig`, with `ElementBlocks`/`NetBlocks` as the transplant helpers and
`OpSink` as the single entry point. `CircuitOp.apply` is validate-first-mutate-after by
contract ("throws `OpRejected` having changed nothing"), and `jls.collab.op` is
Swing-free and enforced so (`ArchitectureRulesTest.collabLayersAreHeadless`). What #412
adds is polish on that layer, not its existence.

**2. It forks the builder #323 forbade forking.** #323 §1: promoting
`NetlistImporter.Builder` is a dependency and *"this feature ... must not fork a second
private builder"* (Open Question 4 makes it an integration blocker). #451's graph says
"reuses its shape, does not extend it" — i.e. a second private builder, a second emitter,
a second summary type. Three duplications of a spine the parent asked to be promoted once.

**3. Its hardening and its report are sealed off precisely where the project has since
generalized.** §7.5 forbids the parser configuration from becoming shared *in this task*.
That was defensible on 2026-08-03. By 2026-08-04 the issue's own boundary comment records
#612 (`.dig`, also XML, "inherits the same hardening discipline") and #608/#610 (one
loss-report schema, the equality asserted once in shared infrastructure), under a whole
importer suite (#556, #558, #559, #561, #562). Under that trajectory, "write the
hardened config privately and let the second XML importer copy it" is how a codebase ends
up with two subtly different security postures and no single place to fix either.

**4. The loss ledger is the fourth ad-hoc instance of one idea.** JLS already reports
fidelity loss in four unrelated shapes: `ImportSummary.coercedX` (which
`docs/capability-roadmap/keystone-b-migration.md` §1.4 calls "a counter whose only job is
to report information the importer destroyed"), the loader's silent attribute drop
(#404/#408), `docs/file-format.md` §9's silent-drop caveat, and the README's JLS-4.1
memory-contents warning. The project keeps rediscovering "a transformation must account
for what it destroyed" and building it bespoke each time. That is the reusable asset here,
not the `.circ` reader.

## The reframing: cut along three seams, not one

**A — Intake (`jls.imp.xml`, tiny, shared from birth).** One hardened
document-to-node-tree front door: doctype disallowed, external general and parameter
entities off, XInclude off, neutralized resolver, secure processing, bounded expansion,
per-parse factory. `.circ` is its first client, `.dig` (#612) its second, and the five
vectors get one test suite instead of two drifting ones. Note in passing that the tree
*does* already parse XML — `Help.loadToc` matches `<tocitem>` with a regex over a
classpath resource (`src/jls/Help.java:143-169`) — which argues the same way: JLS
hand-rolls parsers when there is no front door, and this is the moment to build one.

**B — Fidelity (the actual body of this task).** Replicate δ, build the source's own net
partition from geometric adjacency, and assert `P(imp) ≅ P_src` per file over the corpus.
This is where the maintainer-weeks belong and where a refutation would reorder the whole
feature. Snap-vs-relayout stops being a presentational footnote and becomes what it
really is: *any* geometry transform that changes the partition is a fidelity failure,
detected by comparing partitions before and after the transform, not by trusting H5.

**C — Emission (an op plan, applied to a scratch circuit, adopted whole).** The reader
produces `List<CircuitOp>`; the importer applies it to a scratch `Circuit` (the trick
`ElementBlocks` already uses) and hands the result to the editor or the save path only on
complete success. P8's "no partial circuit, no window" stops being a discipline the
reviewer must audit and becomes structural. Note `OpSink.submitAll`'s default is per-op,
not plan-atomic — the scratch-circuit apply is what supplies plan atomicity, and building
it here is a gift to #163 and #412 rather than a debt to them.

Seam C also dissolves the blocking edge. #451 is blocked on #404 solely because a
save-text pipe routes every attribute through the string-keyed `Element.setValue`, whose
falling-off-the-end return is O3. If the reader instead *constructs* elements against
`ElementRegistry`/`Attribute` and asks whether an attribute has a home before setting it,
the loss becomes observable at the boundary the reader owns, at the moment it occurs, with
the source location still in hand — which is strictly better information than a loader
diagnostic could ever carry. #404 remains worth doing; it stops being this task's gate.

## The deeper reframe: `Seen` is self-reported — parse with receipts

The completeness equality has a hole neither H1 nor §11 names. `Seen` is *the reader's own
census of what it noticed*. A `.circ` element the reader's model has no case for, an
attribute it never looked up, a Logisim library it does not enumerate — none of these enter
`Seen`, so `Seen \ Realized` stays empty and the equality passes while real content is
lost. The reader is both actor and auditor, and the test only checks it against itself.

The fix is elegant and mechanical: **define `Seen` from the parsed document, not from the
reader.** Every node and attribute in the intake tree starts unconsumed; every mapping
step marks what it consumed; the report is the *residue* — whatever the parse did not
account for, by construction. Loss by omission becomes impossible rather than untested,
the equality becomes a tautology you get for free (which is the right shape for an
invariant), and the interesting assertion moves back to where it belongs: partition
agreement. This is also exactly the shape #608 needs to transcribe into a
format-agnostic schema, and it generalizes to `.dig`, `.cv` and Falstad without change.

One corollary: make the ledger's **machine-readable serialization primary** and the dialog
and stdout text renderings of it. The stated audience works in `-b` batch mode; a report
that exists as a dismissible dialog plus prose cannot be consumed by the migration script
an instructor will actually write, and Open Question 5 is then answered by construction.

## The route the issue never considers

Logisim-Evolution ships its own per-component HDL generators and a headless
`--test-fpga … HDLONLY` flow (`docs/hdl-support-research.md:151-195`), and JLS already
imports Yosys JSON netlists (#61). So `.circ → Logisim's own VHDL/Verilog → Yosys →
NetlistImporter` is a zero-new-parser, zero-new-attack-surface migration path that exists
today. I am **not** proposing it as the answer: it flattens hierarchy and layout, produces
a circuit unrecognizable to the student who drew it, and requires two external tools — the
recognizability is the whole point of seam B. But it deserves a paragraph in the task as
(a) a differential oracle for partition agreement on hard files, and (b) an escape hatch
for subcircuits the reader refuses. Not considering it at all is a gap in the design space.

## Acceptance criteria I am explicitly disregarding

- **"Emission is isolated in one class [of save text], so #412's verbs can replace it."**
  Disregard: the verbs exist; emit an op plan now. Rationale above (#323 §2 alt. 2).
- **"The parser configuration must not become a shared utility in this task."** Disregard:
  #612 lands the second XML importer under the same discipline; one config, one test suite.
- **`blocked_by: 404`.** Disregard as a gate under seam C, keeping O3's probe as a
  post-condition check rather than a prerequisite.
- **P9 as accept/reject tables.** Insufficient: the corpus run must report partition
  agreement per file (#323 I1), which no criterion in #451 currently demands.

## What survives verbatim, and should

The loud-reject-on-name-collision rule with both definitions stated; three categories
rather than two (`mapped`/`approximated`/`unmapped`); the construct map as a *document*
because a switch statement is not reviewable by an instructor; the source file being
read-only always; the corpus as procurement on the critical path; and the refusal to add
an XML dependency. Those are exactly right, and none of them depends on the seams I am
asking to re-cut.
