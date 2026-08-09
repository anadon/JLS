# Issue #619: TASK-C558-5: one real published .dig circuit imports as a single undoable operation with zero unexplained losses
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The stated deliverable is atomicity plus a report. The actual goal, stated in
#513 and #510 §3, is narrower and harder: **an instructor with a decade of
Digital course material believes the import**. #619 is the belief gate — the
one task in the `.dig` chain (#612 parse, #614 map, #615 generics, #617 layout)
whose acceptance runs against a real published file rather than a fixture
written to pass. That role is right and worth keeping. What it *asserts* in
that role is mostly wrong, and where it puts the machinery is wrong.

## Two facts in the tree that change the shape of this task

**1. Atomic, undoable import already exists in JLS, and this task can inherit
it rather than build it.** `SimpleEditor.finishImport(Circuit)`
(`/home/user/JLS/src/jls/edit/SimpleEditor.java:679`) wraps a detached
`Circuit` in a `SubCircuit` and hands it to `ew.setup(sub, true)` — the
`placing` gesture. Nothing enters the edited circuit until the drop; the drop
is one `markChanged()`, hence exactly one `CircuitSnapshot` (`UndoManager`,
`/home/user/JLS/src/jls/edit/UndoManager.java`); an abandoned import mutates
nothing at all. AC-2 in full — one undo step, exact restore, no partial
circuit — is *structurally* satisfied by terminating the `.dig` pipeline at
`finishImport`, with zero new transaction machinery.

**2. The precedent importer is already a pure function to a detached
artifact.** `NetlistImporter.importNetlist` returns `ImportResult.saveText()`
(`/home/user/JLS/src/jls/hdl/imp/NetlistImporter.java`,
`.../ImportResult.java`) — bytes in, save text out, throws otherwise. Nothing
partial can escape because nothing is ever installed incrementally. (Worth
noting: this is the shape #323 §2 alternative 2 explicitly *rejected*, and it
is what actually shipped. The rejection reasoning is stale and #619 inherits
the stale version of it.)

Together: AC-2 is not engineering work, it is a **reuse obligation** — "the
`.dig` importer produces a detached `Circuit` and installs it through the
existing placement/`finishImport` seam" — and it is a property of the import
*kernel*, not of `.dig`. #619 as written re-litigates it for one format, and
the `.cv` (#559-family) and Falstad tasks will re-litigate it twice more.

## Reframing 1 (headline): make the gate behavioral, not bookkeeping

I am explicitly disregarding AC-1's framing. "Zero unexplained losses" is a
*set-equality on constructs*: `C_src \ C_out = R`. A `.dig` import can satisfy
it perfectly and still be a wrong circuit — the roadmap's own verified finding
is that `.dig` "elements carry no identifier at all, identity is `<pos x= y=>`,
and wires are `<wire><p1/><p2/></wire>` with connectivity implied by coordinate
coincidence" (`/home/user/JLS/docs/capability-roadmap/lf-06-diff-merge-vcs.md:650`).
Every element can map, nothing be dropped, and the circuit arrive **silently
disconnected** because a port-offset rule was off by one grid unit. #323 §3
names this as "the worst failure mode available"; #619's acceptance does not
catch it (#614 AC-4 asserts net-partition equality, but over its own fixtures,
not over the real published circuit this task exists to run).

The better gate is sitting inside the file being imported. A `.dig` carries its
own embedded test cases — an oracle shipped with the artifact. #612 AC-4
already guarantees those sections survive parse byte-recoverably. So:

> **AC-1′: the real published `.dig` circuit imports, and the circuit JLS
> produces reproduces the source file's own embedded test verdicts.**

This subsumes construct-totality (a dropped construct that mattered fails a
vector), catches the disconnection class totality cannot see, and is the only
statement an instructor actually cares about. It needs a read-only slice of
#562 — evaluate the embedded cases against the imported circuit in-test — not
#562's whole deliverable (deterministic `-t` file emission, loss reporting for
untranslatable test constructs). Pull that slice forward; keep totality as a
secondary assertion, not the gate. The ordering "#562 after #558" is backwards
for the one task in #558 whose job is to be trusted.

## Reframing 2: the properties belong to a kernel, asserted once

AC-2 (atomicity/undo), AC-3 (shared report dialect), AC-5 (`mapped-with-caveat`
rather than silent rewrite) are **format-agnostic invariants**. Three of the
five ACs here are properties every importer must have, written into the task of
one format. The elegant cut: #556 ships not just a report *schema* but an
**importer conformance kit** — a parameterized test suite over the seam
`byte[] → (detached Circuit | refusal) + Report`, which any format adapter is
registered against. Then per-format tasks own only what is genuinely
per-format: the mapping table (data), the fixture and its provenance, and the
oracle. #619 shrinks to "register the `.dig` adapter with the kit; here is the
real circuit; here is its verdict parity." That is a ~0.3 mw task, and it makes
`.cv` and Falstad nearly free — which was CAP-29's entire economic premise
("the marginal cost of each additional format is small"). As currently filed,
the marginal cost is *not* small, because each format re-derives the invariants.

## Reframing 3: does undo belong here at all?

"Undo restores the workspace exactly" — JLS has no workspace. It has one
`Circuit` per `Editor` window plus a subcircuit tree. Two dispositions are
possible and the issue never chooses: import-as-new-document (a fresh editor
window; undo is irrelevant, close it), or import-as-subcircuit (the existing
`finishImport` path). I would choose **new document by default**, subcircuit on
request: the `.dig` stays the source of truth during evaluation, nothing in the
instructor's live work can be damaged by a bad import, and the entire AC-2
surface evaporates. State the disposition explicitly; "undoable" without it is
untestable.

## The silent loss already in the tree

`finishImport` deletes every `SigGen` from the imported circuit
(`/home/user/JLS/src/jls/edit/SimpleEditor.java:686-694`) with no `TellUser`,
no report, no record — a silent construct loss in JLS's *own* import gesture,
under the exact standard #619 wants to hold `.dig` to. Instructors lose data on
this path today. Routing `finishImport`'s removals through #556's report is a
handful of lines, validates the shared contract against a real consumer before
any foreign format depends on it, and fixes a live defect. That is a better
first use of the shared contract than a format JLS cannot yet read.

## Two smaller structural notes

- **The totality assertion needs a per-format identity function, and `.dig` is
  the format that proves it.** Set-equality presumes constructs are
  distinguishable; `.dig` constructs are anonymous and position-keyed. #556's
  contract (shaped by `.circ`, whose components carry library+name+attributes)
  will need a keying seam. That argues `.dig` should *shape* #556 rather than
  consume it "unchanged" (AC-3) — invert or at least overlap the ordering.
- **KC-29-1's fallback already half-exists.** Digital exports Verilog; JLS has
  `jls.hdl.yosys` + `NetlistImporter`. `.dig` → Digital's Verilog → Yosys JSON
  → JLS is an end-to-end path buildable today, at zero mapping cost. It is a
  bad *product* (synthesized gates, names and layout gone — the "pile of
  correctly-wired elements" #617 rightly refuses), but it is an excellent
  **documented escape recipe** to ship now, and a **differential oracle** for
  the native importer. Writing it first converts KC-29-1 from a cliff into a
  fallback that already exists.
- **Fixture provenance.** AC-4 commits a real published `.dig`. hneemann/Digital
  is GPLv3; committing its examples reruns #323 Open Question 2 (GPLv3-only
  absorbed into a `-or-later` tree). Follow the `test/fixtures/legacy-4.1/README.md`
  precedent and record provenance and licence with the fixture from its first
  commit.

## What I would file instead

Keep #619 as the belief gate; restate it as: (1) the `.dig` adapter is
registered against #556's importer conformance kit — atomicity, undo,
no-partial-state and report shape are asserted there, not here; (2) the real
published circuit imports and **reproduces its own embedded test verdicts**;
(3) net-partition equality is asserted over that same real circuit; (4) the
disposition — new document vs. placed subcircuit — is recorded; (5) the fixture
carries provenance and licence. Construct-totality stays, as corroboration
rather than as the gate.
