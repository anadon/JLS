# Issue #387: TASK-0058: the strongest driver wins regardless of file order, and an open-drain bus without a pull-up floats to Z instead of reading zero
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The destination is right and well-aligned: JLS is aiming at a breadboard canvas (#329),
board-netlist export (#298/#307) and mixed-signal work (#305), and none of those are
honest without open-drain, pull-ups and a contention model. #341 states that arc
correctly. This task is where it becomes drawable.

But the title is not the work. Both of its clauses belong to other issues:

- **"the strongest driver wins regardless of file order."** Order-independence is
  #391's (TASK-0057) fold, which this issue is blocked by and which the maintainer's
  comment now chains behind #881 → #391. The moment the fold lands, O1 stops
  reproducing — the issue's own § Method step 1 says exactly that and narrows scope
  accordingly. So the demonstrated bug (P1/O1) is not this task's to fix, and the
  measured evidence that gives the issue its urgency is evidence for its *prerequisite*.
- **"an open-drain bus without a pull-up floats to Z instead of reading zero."** At
  HEAD `WireNet.propagate` already sets `value = actual` with `actual == null` when every
  driver is off (`src/jls/elem/WireNet.java`, the `if (triState)` block), and §9 plus
  `batch-interface.md` already render that as `HiZ`/`z`. P4 is satisfied *today* for any
  tri-state net. The reason a student sees a zero is the downstream coercion of §2
  ("nearly every element's `react` treats a null (HiZ) input as zero"), and §7.11 hands
  that to FEAT-026 explicitly. So P4 is a no-op at the net and out of scope past it.

Strip both and what remains — the real payload — is: **a second axis on the fold, a
saved driver vocabulary, a derived net classification, and two drawable elements.**
That is worth doing. The route proposed to get there is heavier than the destination
needs, and the comment thread is already showing the strain: H3 refuted, `DriverKind`
promoted to a saved must-understand attribute, a global `FORMAT` bump, a refusal path
and a hand-crafted fixture — all acquired after filing, all outside the 2 wk row.

## The reframing: open-drain is a drawing, not a saved enum

The whole H3 collapse traces to one assumption nobody in #341 or #387 examined: that
*open-drain must be a property stamped on an `Output`*. It need not be. JLS already ships
`Constant` and `TriState` (`src/jls/elem/`), and `TriState.react` drives its data input
when control is non-zero and `null` otherwise. An open-drain driver is exactly that:

```
Constant(0) ──data──▶ TriState ──▶ bus
      enable ─ctrl──▶
```

This is drawable at HEAD, with zero new elements, zero new saved attributes and zero
format versions. It is also the *pedagogically correct* picture — open-drain literally
means "pull down, or let go", and drawing it makes that visible instead of hiding it
behind a dropdown on an output pin. Under it:

- **P3's fixture is constructible today.** The comment's central objection — "under H3 as
  written, P3's fixture cannot be constructed" — dissolves. There is nothing to mark.
- **H3 survives intact.** The only strength an element needs to *carry* is `PULL`, and it
  is carried by the identity of `PullUp`/`PullDown`, exactly as the comment concedes.
- **No `FORMAT` bump, no must-understand attribute, no refusal path, no hand-crafted
  refusal fixture.** §7.7 and §7.12 stand as originally written; the comment's
  supersession is unnecessary rather than wrong.
- **#341 Open Question 2 (TTL vs CMOS technology axis) stops blocking.** It only "blocks
  harder" because driver kind became a *saved* vocabulary. Keep it derived and the
  technology axis is a later enum edit, not a second format decision.

The cost is one dropdown a student does not get. That is the right trade for a tool whose
whole thesis is that circuits are drawn.

## Two more trims that follow from the same cut

**The lattice ships three unused levels.** `HIGHZ < WEAK < PULL < STRONG < SUPPLY` has
exactly two reachable values in everything this task ships (PULL from the two new
elements, STRONG from everything else) plus absence. Open Question 4 already admits
`SUPPLY` rides along unused, and `WEAK` has no producer either. #341 OQ3 then asks
whether `PULL` should be two levels. This is a vocabulary being frozen before its shape
is known. Ship the *ordering* — an integer or a two-member enum with documented room —
and let levels be added by the elements that need them. An unused level is cheap only
while it is not also a saved code point; keep it out of the file and it stays cheap.

**`NetKind` has no consumer in this task.** §7.10 derives WAND/WOR/TRI/WIRE and then
never uses it: resolution is pure max-strength, and the issue says so with some pride
("wired-AND as a consequence, not a special case"). Its only consumers are TASK-0093's
`C6_CONTENTION` and the board emitters, neither of which exists. Deriving a
classification nothing reads is speculative generality inside a task already over its
row. Defer `NetKind` to its first consumer; the derivation rule is three lines whenever
it is wanted, and deferring it also removes the last thing #341's criterion 5 (reader
refusal on an unknown net kind) was pointing at.

## The alternative the issue never considers: use iverilog as the oracle

`HIGHZ < WEAK < PULL < STRONG < SUPPLY` is IEEE 1364's strength ladder with the small/
medium/large levels dropped. The issue invents it as a JLS lattice and proposes to
validate it against "a hand-derived truth table" (#341 §5 criterion 2). That is the one
place a pedagogy tool most reliably teaches a falsehood — strength resolution has famous
corners (strength reduction through `buf`/`not`, strength ranges, `trireg` decay) and a
single maintainer deriving the table by hand will get one of them wrong quietly.

Meanwhile the project already ships `-export out.v` (structural Verilog-2005), already
installs `iverilog` in CI (`.github/workflows/ci.yml:73`), already compiles generated
Verilog with it in the HDL-export tests, and README explicitly flags "note JLS's
two-state-plus-HiZ semantics" as the caveat on that bridge. So:

1. **Name the levels as 1364's** (`highz`/`weak`/`pull`/`strong`/`supply`) rather than
   inventing a parallel vocabulary. Free, and it is the vocabulary every downstream
   consumer — netlist export, EVCD, HDL import — will speak anyway.
2. **Make the resolution table a differential test against `iverilog`**, skipping cleanly
   when absent, exactly as the existing HDL-export validation tests already do. The
   wired-AND bus, the equal-strength X, the pull-vs-strong cases — all are three-line
   Verilog modules. This converts §9's "hand-derived truth table" from the weakest link
   into the strongest evidence in the feature, and it does it with infrastructure that
   already exists.
3. **Emit strength on export.** An open-drain net exported as a push-pull `assign` is
   wrong, and the moment `PullUp` is drawable the exporter's silence becomes a defect.
   That is a one-line obligation this issue should record on #33/#59 rather than
   discover later.

This is also the strongest *alignment* argument available and the issue makes none of it:
the strength model is not a private JLS invention, it is the project finally speaking the
same value language as the toolchain it already shells out to.

## What I am disregarding, and why

The goal stands; several acceptance criteria do not.

- **Disregard the comment's ruling that `DriverKind` becomes a saved, must-understand
  attribute with a `FORMAT` bump and a refusal path.** It is a correct deduction from a
  premise ("open-drain must be marked on an output") that the codebase does not force.
  Model open-drain as a drawn `Constant`+`TriState` and the entire branch — bump,
  refusal, fixture, and #341 OQ2's escalation — is not needed. §7.7 and §7.12 as
  originally filed are the better answer and should be restored.
- **Disregard the five-level enum and `NetKind`** as shipping requirements, per above.
- **Keep, unchanged and non-negotiable:** the algebraic laws of §7.10 and their
  exhaustive permutation test (the actual specification); the once-per-conflict
  `conflictReported` discipline from #98 S1 and the S6 "don't repost an unchanged off"
  logic; the model tests shipping in the same commit as the elements (the `jls.elem`
  coverage floor of O8 is real); the `docs/simulation-semantics.md` §9 edit in the same
  commit per #221; and §7.11's boundary statement that P2/P4 are claims about the *net*
  and not about downstream logic. The last one is the single most important sentence in
  the issue and the one most likely to be dropped.
- **Retitle.** The current title promises two things this task does not deliver. What it
  delivers is: *a pull-up and a pull-down you can draw, and a strength ordering that
  makes them behave.* Say that; the order-independence headline belongs on #391.

## Where this sits in the arc

Funding this issue does not fund #341 — the comment says so plainly (2 wk exclusive row
against a 6-9 mw band). Under the reframing the gap narrows honestly rather than being
absorbed: the two elements plus a two-valued ordering on top of #391's fold is a genuine
small task, and the residual that #341 is really pricing (the technology axis, per-driver
output impedance for #490, the board emitters) stays visible as the feature's residual
instead of being smuggled into a task row via a format bump. That is the healthier shape
for a single-maintainer project with a 57-feature registry: ship the drawable thing
early, keep the vocabulary out of the file format until a consumer forces it, and let the
external toolchain — which this project already runs in CI — be the arbiter of what the
strengths mean.
