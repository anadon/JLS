# Issue #336: FEAT-004: exactly one net partition in JLS, and a synthesized net name that survives an unrelated edit
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

Read at `master` `5b05d67`, with the issue's own re-pin comment (`8288226`) as the anchor
convention. Line numbers below are what I opened locally.

## What this issue is actually for

Strip the machine block away and the claim is: *JLS is becoming a design tool whose outputs
other tools consume, so the thing an output names must be a real object with a durable name,
not an accident of the emitter that happened to print it.* That is the right claim, and it is
load-bearing — eight capstones and six features hang off it. The trajectory in
`docs/grand-architecture.md` §5 (core provides model + sim + persistence; "batch/services & hdl"
sits on top and nothing else) is exactly the shape that requires this: every emitter above the
line must read one answer from below it. Endorsed on purpose.

What I want to reframe is *which* seam is being cut, because the issue's own §3 formula and its
Background section describe the code inaccurately, and the inaccuracy sets the two children
loose on the wrong 1.5 weeks each.

## Reframing 1 — there is one partition already; what is duplicated is the *fold*

The Background says "two partition passes exist, not one." I do not find that in the code.

- `Circuit.finishLoad` (`src/jls/Circuit.java`, the `LinkedList<WireEnd> ends` walk) is the only
  connected-component walk over wire ends. It produces `WireNet` objects that are live
  simulation state — `WireNet` holds bits, `hasinput`, `triState`, and is what value propagation
  runs over (`src/jls/elem/WireNet.java:16-31`).
- `HdlExporter.UnionFind` (`src/jls/hdl/HdlExporter.java:1103`) is `Map<WireNet, WireNet>`. It is
  a **coarsening over the load walk's output**, not a re-derivation of it. It never touches a
  `WireEnd` adjacency.
- `CircuitAssert.reaches` (`test/jls/ui/CircuitAssert.java:125`) is the second implementation of
  that same coarsening — again over `WireNet`, again folding same-named `JumpStart`/`JumpEnd`.

So the duplicated thing is roughly eighty lines of jump-alias fold, done twice, once in
production and once in a test helper. The fine partition is already singular and already shared.

This matters because §3's stated invariant, $\mathrm{Nets}_{\text{load}} = \mathrm{Nets}_{\text{emit}}$
with $\sim$ defined to include jump aliasing, is **false by design and must stay false**. A jump
pair is not an electrical short in JLS: `JumpStart`/`JumpEnd` are simulated elements with
propagation. Making `finishLoad` return the coarse quotient changes simulation semantics — a
`docs/simulation-semantics.md` change, not a refactor. So TASK-0007 as literally written is
either a no-op for `Circuit` (move the same walk, change nothing) or a semantic break.

**The reframing:** name two levels, not one.

- **Net** = a `WireNet`. The simulator's own quotient. Already unique, already in core. Nothing
  moves.
- **Signal** = a `WireNet` set closed under jump aliasing. This is what every *emitter*, every
  external annotation, and every netlist-shaped consumer actually means by "net". It exists
  three times informally today and zero times as a type.

Under that framing the feature's value is unchanged but the work collapses: define `Signal`
(members, bits, tri-state, name) and one `Signals.of(Circuit)` producer; point `HdlExporter` and
`CircuitAssert` at it. IC-2's purity proof becomes trivially available because `finishLoad` is
untouched, so no golden *can* move. IC-1's architecture test gets a sharper predicate than "a
second connected-component walk": *no code outside the package may enumerate elements looking for
`JumpStart`/`JumpEnd` by name* — that is the actual violation shape, and it is grep-checkable.

I would also put it in `jls.core`, not a new top-level `jls.netlist`. §5 of the grand
architecture places `Circuit · WireNet` in the core kernel and has no slot for a peer package;
and Global Invariant 5 ("stays a headless leaf, no awt/swing/edit/sim") is verbatim the #77 rule
that `HeadlessCoreRatchetTest` already enforces. Reusing that ratchet is strictly better than
minting a second architecture test that says the same thing about a second package.

## Reframing 2 — the naming formula is not total, and the promise it sells is not the one users want

$\mathrm{name}(n) = \texttt{net\_}\delta(\min_\preceq \mathrm{sid}(\mathrm{drv}(n)))$ is defined
only where $\mathrm{drv}(n) \neq \emptyset$. The code has three synthesis sites, and the formula
covers one:

- `:346` driven nets — covered.
- `:374` **undriven-but-read** nets: `net_u<id>`, chosen after `undriven.sort(...)` on the key
  `String.format("%09d_%s", el.getID(), put.getName())`. This is a *rank* over save ids, not a
  function of one id. Insert an element with a low save index and every subsequent undriven name
  shifts. This is the single most volatile name in the exporter and §3 does not mention it.
- `:580`, `:1025`, `:667`, `:695`, `:812`, `:945`, `:1255` — `reg_`, `mux_`, `dec_`, `tt_`, `sm_`,
  `unc_` helper identifiers, all `getID()`-derived. §1 claim 2 says "net names and register
  names"; the other five kinds live in the same emitted namespace and will still churn.

And the namespace itself defeats IC-3 independently of the formula. `HdlNames.synth` uniquifies
by appending `_2`, `_3` **in claim order** (`src/jls/hdl/HdlNames.java:98`, `unique`). A user wire
named `net_a1b2c3`, or a digest collision, can push an existing net from `X` to `X_2` when
something unrelated is inserted. "Zero pre-existing net names moved" is a property of the
*allocator*, not of the formula, and the feature has to say so: either reserve the synthesized
shape against user names, or make uniquification keyed on the sid rather than on request order.

Then the promise itself. `ElementId.mintFresh()` embeds this install's replica id, and
`ElementId.legacy(long)` mints from **file order** (`Circuit.java:1333`,
`assignLegacyStableId`). Two consequences the epoch document must state or it is not honest:

1. A pre-#165 `.jls` that has never been re-saved gets file-order sids on *every* load. Net names
   for such a file are still a function of save order — the property being removed. The promise
   begins at the first re-save under current JLS. For a classroom where the instructor
   distributes a legacy circuit, that is the common case, not the corner.
2. The name is stable per *file lineage*, not per *circuit*. Two students who draw the same
   circuit get different net names, because their replica ids differ. So "an autograder keys an
   annotation on a JLS net name" works for re-exports of one file and fails across authors.

Open Question 1 asks digest-vs-raw-id. The more consequential question, which the issue never
asks, is **identity-derived vs. content-derived**. A structural name — digest of (driver element
tag, output index, sorted member put descriptors) — is stable across installs, across authors,
and across a save-order change, and is *unstable* under edits that change the net's own
neighborhood. Identity-derived is the mirror: stable under any edit, unstable across authors.
Neither dominates; they serve different downstream consumers (#523's isomorphism check wants
identity; a reference-solution diff wants content). Since the epoch is a one-way door once an
external format keys on it, the feature should decide this fork explicitly and can plausibly
ship both — `net_<idDigest>` as the emitted name, with the content digest exported as a comment
or sidecar for cross-author comparison. That decision belongs on this issue, not inside
TASK-0008 (#373).

## Reframing 3 — the probe/VCD strand should legalize, not reject

Scope item 5 and IC-5 are a different feature riding along, and I think they ride along in the
wrong direction. `Wire.attachProbe` (`src/jls/elem/Wire.java:462`) assigns without validating;
`Util.isValidName` (`src/jls/Util.java:219`) accepts any Unicode letter, which VCD does not.
Validating at attach does nothing for the file that already contains `my probe.name` — and JLS's
whole load philosophy is that historical files load (Global Invariant 1). Refusing to write a
dump for a file that loads fine is a regression for the student it is meant to help.

JLS already solved this exact problem once: `HdlNames` legalizes deterministically, uniquifies,
and surfaces a rename map the caller reports (`model.addRename`). The elegant move is to apply
the same mechanism to the waveform writer — the dump is always writable, never contains a space,
and the run reports "probe `my probe.name` written as `my_probe_name`". Attach-time validation
then becomes an optional nicety rather than the load-bearing guard, and IC-5's "removing either
leaves the other red" pairing is replaced by one total function with one test. This also removes
the odd coupling in §2's rationale ("the same rule at two moments"), because there is one rule at
one moment: emission.

## Where it duplicates or pulls against the arc

- **Duplicates nothing.** The #523 boundary comment is right; that issue consumes this one.
- **Pulls slightly against §5** only in package placement, addressed above.
- **IC-6 is the weakest part.** "Delete `HdlExporter` and compile `PcfEmitter`" does not falsify a
  decorative boundary: `PcfEmitter` lives in `jls.hdl.board` and is an emitter by any reading, so
  the test passes while the boundary stays decorative. Under the Net/Signal reframing there is a
  real non-emitter consumer sitting right there: `CircuitAssert`, which is the *oracle* for
  connectivity in the test suite and today reimplements the relation it is supposed to check —
  a self-fulfilling oracle, which is a correctness argument, not just a tidiness one. A second
  production candidate worth weighing is editor-side connectivity DRC (multi-driver, undriven
  input), which is exactly a Signal-level question and does not exist yet.

## What I am disregarding, and what I would keep

I am disregarding the §2 decomposition rationale and the two-task roster as filed (#468, #373).
The purity/rename sequencing argument is real *given* the premise that TASK-0007 moves the load
walk — and that premise is what I think is wrong. With the Signal reframing, extraction touches
no golden by construction, so the reason to keep the two apart evaporates, and the natural cut
becomes: (a) `Signal` + one producer + three call sites converted, (b) one total, documented
naming function covering all seven synthesis sites plus the allocator, (c) waveform legalization.
Roughly the same cost band, better boundaries.

I would keep, unchanged: the insistence that the convention is *normative shipped documentation
with an epoch* (this is the real deliverable), IC-3 as the user-visible acceptance test, and the
refusal to widen the exporter's element policy here.
