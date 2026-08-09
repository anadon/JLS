# Issue #367: FEAT-047: a circuit can declare what one tick means in physical time, once, and everything that reports time reports it in that unit — declaring nothing keeps today's behaviour exactly
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

Strip the machinery and one sentence remains: **JLS's numbers should mean something
physical.** Everything else — the `TimeBase` record, the `$timescale` grammar, the
rejecting parse, the `FORMAT` bump, the imported-circuit suppression rule, the
must-understand policy, Open Questions 1–3 — exists to serve a second, unexamined
premise: *that each circuit should be able to choose its own meaning.*

Those two are not the same claim, and the whole cost of this issue sits in the second
one. The value sits entirely in the first.

## The fact the issue steps over

The tick is already a nanosecond. Not nominally — actually, in the author's head and in
the tree. `docs/simulation-semantics.md:271-285` publishes the delay table:

| AND/OR/XOR | NAND/NOR/NOT | TriState | Decoder | Mux | Register | Memory |
|---|---|---|---|---|---|---|
| 10 | 5 | 5 | 15 | 25 | 50 | 100 |

Those are not abstract counts. Those are a 1980s TTL/CMOS datasheet in nanoseconds —
10 ns for a gate, 5 ns for an inverter, 50 ns for a flop, 100 ns memory access. The
roadmap says so out loud (`docs/capability-roadmap/README.md:474-478`): "GTKWave reads
that file and confidently labels the axis in nanoseconds." `BatchSimulator.java:423`
writes `$timescale 1 ns $end` and has for years. `VcdExportGoldenTest` byte-pins it in
three places.

So the tree does not have an *undeclared* unit. It has a **declared unit that the
normative spec refuses to admit to.** `simulation-semantics.md:26-30` is not describing
reality; it is disclaiming it.

That reframing matters because it changes what "resolving the tension" costs. The issue
prices it at 2–3 maintainer-weeks plus a permanently-open residual. The reframing prices
the same tension at one paragraph.

## Alternative 1 (the cheap one): ratify, don't parameterize

Delete the disclaimer. Amend `simulation-semantics.md` §1 to say the JLS tick **is**
one nanosecond, amend `batch-interface.md` §4.2 to stop calling `1 ns` nominal, and stop.

- Zero bytes change anywhere. Every golden holds — not by an invariant that has to be
  defended, but because nothing was touched.
- No `FORMAT` bump, no loader arm, no `NEWER_FORMAT` path, no version-mechanism choice,
  no entanglement with #319, no invariant 6, no Open Questions 1/2/3.
- **Every existing file becomes true**, retroactively. Compare with the issue as written,
  where after all that work the default path still emits the same fiction to the same
  third-party viewers, and only opted-in circuits get honesty. Criterion 6 helps nobody
  who has a `.jls` file today. This helps everybody.
- The delay table stops being "a library datasheet published as a normative simulator
  document" and becomes a normative claim the simulator can be held to.

This is available this week and it is strictly better than the issue's default path at
the issue's own headline goal ("an external tool reading the output is being told the
truth").

## Alternative 2 (the right one): fix the quantum at 1 ps, once

Alternative 1's ceiling is resolution. #486/#490 want a `Td` of 345.6 ps; #351's solver
wants sub-ns steps. At a 1 ns quantum those round to zero.

So pick the quantum with headroom and fix it globally: **1 picosecond**. Then
`seconds(n) = n × 10⁻¹²` — a constant, not a record; no grammar, no parse, no rejection
path, no per-file variation to test, no second conversion possible because there is no
conversion object to duplicate.

The migration is one bounded mechanical step, paid with the *same single* `FORMAT` bump
this issue already budgets: on load of a pre-bump file, multiply stored delays by 1000
(legacy integers meant nanoseconds, and the delay table proves it). Goldens change once,
by a factor everyone can verify by eye.

Check it against the five serving capstones:

- **#308 audio** — 44.1 kHz period = 22,675,737 ps, 1.7 ppb error. The issue's own figure,
  its best case, obtained without a declaration.
- **#313 transmission lines** — 345.6 ps = 346 ticks. Expressible.
- **#309 analog parity** — parity in seconds, which a fixed quantum supplies.
- **#305 spec-in-seconds** — a 0.16 Hz corner is 6.25e12 ticks; range is 2⁶³ ps ≈ 107 days.
- **#368** — the issue's own boundary comment already concedes this one doesn't consume
  #367: its crossing time is "terminated by the integer tick lattice, not by a declared
  physical unit."

The 2⁵³ exactness limit (≈2.5 h at 1 ps) survives unchanged and still deserves its
assertion — that criterion is good and I'd keep it verbatim.

**What is genuinely lost:** a design that wants femtoseconds, and a design that wants a
year of simulated time. Neither has a requesting user, and neither appears in the five
capstones. The roadmap asks for "physical time units" (`README.md:405-406`), not for
*per-circuit variable* physical time units. The declarability is an inference from VCD's
grammar, not from a JLS need.

**What is genuinely gained:** the entire §5 residual disappears. There is nothing to
"carry through every consumer" when the conversion is a compile-time constant every
consumer can inline. Criterion 5 ("one conversion, tree-wide") becomes tautological.

## Why "either order works" with #319 is wrong

§6 says the interaction with #319 is a sequencing question and both orders are fine.
They are not.

#319 carries `blocked_by: [334]` — the section frame sits behind the canonical-text /
stable-id container rewrite. It is not landing soon. So "wait for #319" is not a live
option, and "land first and oblige #319 to adopt" is a bet that a `REPLAN:` obligation
survives a multi-quarter gap. The issue's own §7 names the losing branch ("two version
mechanisms forever") and then leaves the coin in the air.

Worse, the real defect is one level down and unnamed anywhere in the issue: **the
`CIRCUIT` block has no forward-compatibility valve at all.** Elements have one
(`file-format.md:220-222`, unknown attribute names dropped). The circuit block's token
loop accepts exactly `ELEMENT` and `ENDCIRCUIT` and fails `MALFORMED` on anything else
(`Circuit.java:886-900`). That is not a fact about time bases. It is a fact about *every
circuit-level property JLS will ever want* — a default voltage rail, a design ruleset, a
radix manifest, a provenance record, a schema URI. Each will hit the identical trap and
each will cost a whole-file bump and a whole-file refusal.

So **if declarability is kept anyway**, do not spend `FORMAT 3` on a bespoke `TIMEBASE`
keyword. Spend it on a generic circuit-level attribute item with a must-understand
marking — twenty lines in the same token loop — and make the time base its first tenant.
That is the minimal, non-speculative slice of #319 that does not require #334, it pays
the bump once instead of four times, and it makes #319's later arrival a consolidation
rather than a competing mechanism. This is the different seam to cut along.

## The residual is a structural defect, not an estimate gap

The comment thread has spent four passes on a roster that cannot close: TASK-0101 was
absorbed, then #882 was filed to un-absorb it, and this issue is kept at `tier:feature`
explicitly to hold a residual whose consumers "do not exist yet." A feature whose
completion depends on two features it does not block will stay open indefinitely by
construction. That is not a bookkeeping problem the next pass fixes; it is the plan
telling you the decomposition is wrong.

The project already owns the correct instrument, seven times over: `HeadlessCoreRatchetTest`,
`NotificationRatchetTest`, `PointerApiRatchetTest`, `SocketConfinementRatchetTest`, and
three more. **The residual is a ratchet test, not an issue.** "No consumer computes seconds
from ticks except through the one conversion" is exactly the shape those tests enforce —
enforcement that lives forever, needs no open issue, and binds #351 and #346 the day they
land without either issue having to remember. Land the ratchet with the declaration and
close the feature.

## Disregarding the stated acceptance criteria

I am setting aside criteria 1–4 and 7 and Open Questions 1–3 as artifacts of the
declarability premise rather than of the goal. Under Alternative 2 there is no absent
default to sweep for (nothing is optional), no grammar to confirm, no version-mechanism
choice, and no parse to reject — those criteria are answers to questions the design
should not be asking. What I would keep, unchanged and load-bearing:

- the 2⁵³ assertion firing rather than silently losing precision (criterion 5 / §5.2);
- the no-accumulation rule, and the test written at a scale where the summed and
  recomputed values actually differ — that hazard is real under any design;
- the joint code-and-documentation obligation (§5.4) — the doc amendment *is* the feature
  under Alternative 1, and must land in the same commit under Alternative 2;
- the manual GTKWave/Surfer verification from the #431 follow-up. Under a fixed quantum
  it is a five-minute check, and it is still the only evidence a real viewer agrees.

## Recommendation

Ask the maintainer one question before any code is written: **does any JLS user need two
circuits with different time quanta, or do they need JLS's one quantum to be physical?**
The roadmap, the delay table, the VCD header and all five serving capstones answer the
second. If that holds, land Alternative 1 this week as the honest floor, and schedule
Alternative 2 as the single migration that serves the analog and transmission-line
program — and close #882's scope down to the constant, the 2⁵³ assertion, the ratchet
test and the two doc amendments. If the maintainer genuinely wants declarability, then
this issue should not be about time at all: it should be the circuit-level attribute slot
with must-understand marking, filed against #319's program, with the time base as its
worked example.
