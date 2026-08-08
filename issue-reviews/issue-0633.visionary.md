# Issue #633: TASK-C561-3: every analog element in a real Falstad circuit is a named, located, explained loss by design — and the report dialect is #556's, unchanged
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this task is really for

Two jobs, neither of which is "import a file." First, it is the **acceptance run** for
#561 — the one place the whole Falstad slice is judged against a real artifact. Second,
it is the **falsification test of #556**: AC-3's negative claim ("no Falstad-specific
field or renderer exists") is the evidence that the shared contract is a contract and
not a `.circ` dialect with three consumers.

Both jobs are worth doing. Almost none of the four acceptance criteria as written
actually do them, and AC-1 can be satisfied by an import that is wrong. Hence rethink
rather than endorse-with-reframing: keep the outcome sentence, replace the oracle.

## AC-1 is satisfiable by a semantically wrong import — and Falstad is the one format where that is the *normal* case

AC-1 pairs "zero unexplained losses" with TASK-C556-2's totality assertion. Totality
ranges over *dropped* constructs: dropped set == reported set, both directions. A
circuit in which every gate maps successfully has an empty dropped set, a trivially
satisfied totality assertion, and zero unexplained losses — and may still not behave
like the source.

That is not a corner case here. #510 §3 records that "Falstad's digital behavior is
emergent analog" and that its own ring counters are unpredictable (their #364). JLS is
a discrete-event simulator with per-element propagation delays
(`docs/simulation-semantics.md` §6–7). So the Falstad ring counter that imports with
zero losses will behave *more* deterministically than the original. That is a silent
semantic rewrite — in the user's favour, but a rewrite — and AC-4's "never silently
rewrites semantics" is breached by the **successful** path, not the failure path.
#556's per-construct ledger structurally cannot see it: nothing was dropped, so nothing
is reported.

This task is the only place in CAP-29 where that gap is discoverable cheaply, on a real
circuit, before three more importers are built on the same ledger. The deliverable that
matters most here is not the Falstad importer — it is a **model-delta statement**: a
whole-import declaration of which semantic model changed and which class of circuit is
affected (settling-time-dependent behaviour, analog threshold levels, convergence-
dependent correctness), fed back to #556. #559's CircuitVerse `delay`-is-queue-priority
case (#510 §3) is the same species and would reuse it immediately.

## AC-2 makes the acceptance run anti-correlated with the outcome it certifies

AC-2 passes when every analog element carries `dropped-by-design`. Whoever picks the
fixture therefore picks the verdict, and the *cleanest* pass is the circuit with the
most analog elements dropped — which is precisely the circuit that imports least
usefully. In real Falstad logic circuits the analog elements are not decoration: the
voltage source, ground, switch, pull-up and LED **are the I/O boundary**. Drop them all
as named losses and you get a correct gate netlist with nothing driving it and nothing
observing it: it "opens" and does not work, contradicting #561's own title and CAP-29's
outcome sentence, while scoring 100% on AC-1 and AC-2.

The vocabulary is the root cause. AC-2 forces one disposition ("not an error and not an
omission") where the semantics need two:

- **`boundary-mapped`** — analog at the digital boundary, mapped by intent and named as
  a mapping in the report: ground/Vcc → `Constant`, switch or source into a logic input
  → `InputPin`, LED/probe → `OutputPin`, clock source → `Clock`, pull-up into a gate
  input → `Constant`.
- **`dropped-by-design`** — analog *as computation*: op-amps, RC networks,
  transistor-level logic. Permanently out of scope, exactly as the boundary note says.

Then add the liveness criterion the task is missing: the acceptance run fails if the
imported circuit has no drivable input or no observable output, and it must *simulate*
against the source circuit's expected truth — #631 AC-3 asks for that on a synthetic
fixture but #633's real-circuit run, the one that counts, does not.

## AC-3 is an absence claim, and this repository already knows that absence claims are architecture, not tests

"A test asserts no Falstad-specific field or renderer exists" cannot be discharged by
comparing two golden files; two reports over different circuits are byte-different, and
"byte-comparable in shape" is not a checkable property. What is checkable is the thing
JLS does seven times already: `HeadlessCoreRatchetTest`, `NotificationRatchetTest`,
`NullMarkedRatchetTest`, `PointerApiRatchetTest`, `CollabSecurityRatchetTest`,
`SocketConfinementRatchetTest`, `PackageInfoRatchetTest`, plus
`ExtensionPointCatalogTest`'s both-directions cross-check. The genre exists; use it.

Stronger still, and the reframing I would actually take: make the dialect
**unrepresentable**. If #556/#608's report is a closed record over a closed disposition
enum with one serializer, a Falstad-specific field is a compile error and a
Falstad-specific renderer is a call-graph violation a ratchet test names. This is the
same move JLS made with `SaveTags.resolve` (tag text never reaches `Class.forName`) and
with `ElementConstructorContractTest`: prefer a shape that cannot express the bug. AC-3
then stops being #633's burden and becomes a property every future importer inherits
for free — which is what "contract" was supposed to mean.

And the *better* falsification oracle is already in tree and unmentioned:
`src/jls/hdl/imp/ImportSummary.java` is a shipped, independently written loss report
(category→count plus `coercedX`) for a source — Yosys cells — unlike all four planned
formats, authored before #556 existed. An adopter designed alongside the contract by
the same author in the same week is a weak oracle; a retrofit of `ImportSummary` is a
strong one and costs nearly nothing. Move that proof to #608/#556 and #633 sheds AC-3
entirely.

## AC-4 restates a problem this repository has already solved once, generically

"A single undoable operation that never silently rewrites semantics and emits no
partial circuit" is, verbatim, the contract of `jls.collab.op`:
`OpSink.submitAll` (`src/jls/collab/op/OpSink.java:42`) is documented in
`docs/operation-layer.md` as "one gesture, one undo snapshot, however many ops express
it"; `CircuitOp.apply` validates before mutating and a rejection leaves the circuit
byte-identical (`CircuitOpTest.rejectionsLeaveTheCircuitUnchanged`,
`test/jls/collab/op/CircuitOpTest.java:761`); the vocabulary is closed
(`AddElements`, `AddWire`, `SetElementConfig`, …), which is exactly "never silently
rewrites semantics" expressed as a type rather than a promise.

Meanwhile the only importer that exists builds circuits by emitting save text and
reparsing it (`ImportResult.saveText()`) — the approach #323 §2 alternative 2 rejects.
If AC-4 stays a per-format acceptance criterion, the fourth importer will hand-roll
undo on top of that. Replace it with a design statement: **import is an
`OpSink.submitAll` of the closed op vocabulary.** Undoability, atomicity and
no-partial-circuit then hold for `.circ`, `.dig`, `.cv` and Falstad at once, by
construction, and CAP-29 AC-4 is discharged in one place instead of four.

## The alternative the issue never considers: triage the circuit, don't ledger the elements

The whole element-by-element loss ledger presumes the right answer to a Falstad file is
always "import the digital part." A cheaper and more honest design is a **circuit-level
verdict** computed before construction:

- analog content is boundary-only → import, with the boundary mappings named;
- analog content is computational → **refuse**, and print what *would* survive if the
  user rebuilt the digital part, plus the one-line reason JLS is not an analog solver.

A refusal that lists what would survive respects #510's explicit instruction on Falstad
("the analog/intuition core is not winnable — do not contest it") far better than a
half-circuit with a forty-line loss report, it cannot produce the dead-circuit pass
above, and it is less code than the ledger. The ledger remains for the import path; the
triage verdict is what a switcher actually needs.

## Two smaller misalignments worth fixing at filing time

**"A place a user reads" is unfunded.** The boundary note says this task is where the
permanence of the analog non-goal "is written down in a place a user reads." A report
emitted at import time is read only by someone who already imported — after they paid
the switching cost. The place a prospective switcher reads is the "from Falstad"
migration page (#510 §3 minimum bar) and the in-jar help tree, which has a completeness
test (`HelpTopicsTest`) that would keep it honest. But CAP-29 **PF-6 already owns the
per-format migration pages**. Either #633 cedes the user-facing statement to PF-6 or
PF-6 cedes it here; as filed, both claim it.

**"One real published Falstad circuit" has no provenance plan.** This repository treats
provenance as first-class (`pop_GPLv3.pdf`, SBOM, signed attestations, reproducibility
docs), and there is no Falstad anything in the tree today — I grepped. Committing a
third-party circuit as a GPL-3.0-or-later test fixture needs a recorded source and
licence. Make it an asset rather than a fixture: a `test/fixtures/migration/<format>/`
corpus with a provenance record per file, shared by #323, #558, #559 and #633. CAP-16's
KC-16-1 corpus measurement needs the same thing, and a corpus turns this task's
sample-of-one demo into the measurement the capstone's economics already depend on.

## Recommended shape

Keep the outcome. Replace the criteria with: **(1)** analog is triaged, not blanket-
dropped — `boundary-mapped` versus `dropped-by-design`, with a liveness assertion that
the imported circuit has drivable inputs and observable outputs and simulates to the
source's expected truth; **(2)** the acceptance run is over a provenance-recorded corpus,
not one circuit; **(3)** a model-delta statement accompanies the per-construct ledger,
and is fed back to #556 as a second axis; **(4)** AC-3's no-dialect proof moves to #608
as the `ImportSummary` retrofit, and what remains is a ratchet test over a closed report
type; **(5)** AC-4 becomes "import is an `OpSink.submitAll`," discharged once for the
whole importer family. What I am explicitly disregarding is AC-1's "zero unexplained
losses" as a sufficient pass condition and AC-2's single blanket disposition — both are
satisfiable by an import that opens and does not work, which is the opposite of what
this task exists to certify.
