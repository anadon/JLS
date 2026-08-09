# Issue #455: TASK-0069: host input becomes a replayable transcript indexed by retirement rather than wall clock, a golden produced against a live human becomes a build failure, and the exchange gets a console pane
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

One sentence in §11 carries the whole thing: **"the recording, not the session, is the
artifact."** That is correct, it is the project's central arc — `docs/batch-interface.md`
is a *stability contract* because grading is the load-bearing use, and every oracle in
`test/jls/` is a golden — and it is the one thing that lets interactivity arrive in JLS
without dissolving the grading culture. Endorse the goal without reservation.

Everything else in the issue is mechanism, and the mechanism is cut along the wrong seam.

## The reframing: index the recording by port observations, not by time

The issue's format is `(stamp, kind, dir, byte)` with `kind ∈ {RETIREMENT, SIMULATED_TIME}`.
Three things follow from that choice, all bad, and all avoidable:

1. **`RETIREMENT` is undefined for almost every JLS circuit.** `grep -ri retirement docs/`
   returns nothing: the concept exists only in the parity work (#390/#347) and presupposes a
   drawn CPU with a defined commit point. JLS is a gate-level simulator whose median
   console-bearing circuit will be a state machine, not an RV32 core. A durable format
   (§7.7) whose *preferred* stamp kind is meaningful for one circuit archetype is not the
   format of a general seam — which is why §7.11 has to invent a "named refusal" for reading
   a file this very build cannot interpret, and why §Status has to import a co-requisite on
   #390 into a task that otherwise has none.
2. **`SIMULATED_TIME`, the kind that actually ships, fails the issue's own load-bearing
   test.** §7.10 concedes it: the clock-period invariance "holds for `SIMULATED_TIME` only up
   to the reparameterization t ↦ (c'/c)t". P2 is declared the discriminating experiment, and
   in the configuration this task ships it does not pass. That is not a detail; it means the
   design's central claim is only true in the mode gated behind an unlanded issue.
3. **A time-shaped field invites the wall clock.** §11 says so ("every logging idiom a
   contributor knows stamps in wall time"), and then defends the field with a ratchet (P4)
   instead of removing it.

**Alternative: stamp each input byte with the number of guest-observable port reads that
preceded its arrival.** The port is the seam; index by the seam's own events. Concretely,
`(readIndex, byte)` for the input side — "byte 0x41 becomes available before the guest's 7th
`poll()`" — with the output side a plain byte sequence.

Why this is strictly better on the issue's own criteria:

- **P2 passes exactly, in the shipping configuration, with no dependency on #390.** A polled
  guest executes the same sequence of status polls and buffer reads regardless of
  `Clock.cycle`, so identical guest bytes come out while simulated times differ. Both halves,
  no reparameterization caveat.
- **P4 becomes vacuous rather than ratcheted.** There is no time-shaped field, so no field
  can parse as an epoch millisecond. The wall-clock mistake is made structurally impossible
  instead of textually policed — which is precisely the argument #324 §2 makes for sealing
  `HostBytePort` ("sealing is structural rather than stylistic"). Apply the parent's own
  principle one level up.
- **It closes the fidelity hole that a naive byte-file replay would open.** A recording must
  *withhold* byte i until the point where it actually arrived, or a guest that saw
  "not ready" live sees "ready" on replay and diverges — P3 fails. A read-index stamp is the
  minimal thing that preserves that, and it preserves it under retiming, under changed
  propagation delays, and under a future levelized engine (ARCHITECTURE.md's #221 decision
  reserves one, and binds it to observable equivalence — a time-stamped transcript would be
  a fresh obstacle there; a read-indexed one would not).
- **It kills three of the six blocking open questions.** Q6 (versioning) shrinks to nothing
  worth versioning; the #390 co-requisite and the `RETIREMENT` refusal path disappear.

## The second cut: there is no new file type on the input side

#424 §7.4 already ships `FilePort` as a permit and `-serial <path>` as its grant. Input
replay is therefore *already implemented by the prerequisite* — a threadless port serving
recorded bytes from the simulation thread, which is exactly H3's conclusion. Capture on the
output side is a tee on `emit`. So the honest decomposition of this task is:

- replay = `-serial FILE` (exists in #424), plus the read-index gating above;
- capture = one flag that tees the port's two directions to a sidecar;
- provenance = one `$comment` line and one appended outcome line (keep this; it is cheap,
  additive, and correctly disciplined against O6's frozen strings).

That makes Open Question 2 answer itself: **do not add a `replay:` form.** A file grant *is*
replay. Two syntaxes for one behaviour is the kind of thing #324 §2 rejects elsewhere ("two
grant models and two ratchet stories") and should reject here.

## Disregarding one acceptance criterion outright: the provenance ratchet's scope

§11 names the in-source VCD goldens as the biggest threat and P5 widens the scan to
`test/**.java` to cover them. That inverts the actual risk. `VcdExportGoldenTest` at
`:199-214` builds `new BatchSimulator()`, runs it in-process, and byte-compares `toVcd()`
against the constant; `:330-360` re-runs the whole thing as a subprocess with an explicit
argv containing no `-serial`. **A golden produced against a live human cannot survive either
comparison.** The in-source goldens are the structurally protected ones. The scan protects
what is already protected, and it buys that with Open Question 3 — the mention-versus-
provenance heuristic that the issue itself predicts will get the ratchet disabled.

I would drop P5's source-tree scan as written. The equivalent guarantee, for less: make the
golden-producing path refuse a live door (a usage error, one CLI test), keep the mode marker
in the artifact for humans, and let the existing regeneration-and-compare mechanism be the
detector it already is. Structural impossibility over textual scanning — again, #324's own
argument.

## Two smaller things the issue gets factually wrong or half-right

- **O7 amends the wrong sentence.** `docs/simulation-semantics.md` §3's "fully deterministic
  — a pure function of circuit content" has *event ordering* as its subject: the paragraph is
  about FIFO-within-timestamp and `getElementsInStableOrder` seeding. Host input does not
  make that claim false — the ordering rule is unchanged. Editing that sentence weakens a
  statement that stays true. The honest amendment is a new clause: with a granted door the
  run's *outcome* is a function of circuit content and the delivered input sequence; with the
  default `NullPort` the existing statement stands verbatim.
- **The parent already owns the format question, and disagrees.** #324 criterion 5 says
  `(stamp, byte)` "stamped in simulated time"; #324 Open Q2 asks whether the canonical form is
  text or binary; #455 Open Q1 and Q6 ask the same question again and mark it "Blocks
  execution." One undecided format question filed at two tiers is a governance smell. Decide
  it once, in #324, and this task stops blocking on itself.

## Scope: this is a feature wearing a task's label

Five separable deliverables (format, capture, replay, provenance+ratchet, GUI pane), nine
predictions, three normative doc amendments, sixteen DoD boxes, four questions marked "blocks
execution," and a hard prerequisite that has no issue number. The pane in particular is
coupled to #440's coverage floor, TASK-0021's harness, and #162/#91's display lanes, and has
no bearing on the replay contract. The issue already concedes the split three times
("pane half only"). Make it real: the pane is its own issue under #316, and this one is the
headless recording contract. That is also the split that lets the reframed version above be
one small, verifiable change.

## What to keep

The append-never-edit discipline against O6's frozen strings; the no-`$date` precedent from
O4; the incremental-write requirement borrowed from #405; the `HeadlessCoreRatchetTest`
prefix for `src/jls/io/`; and the insistence that the reconciliation with #63's recorded
rejection be *written down* rather than inferred. All of that is squarely in the project's
grain.
