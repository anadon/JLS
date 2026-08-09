# Issue #309: CAP-14: JLS's own analog answer and real ngspice's agree to a declared 3.16e-3 envelope nightly, so "the solver is correct" stops resting on JLS's own tests
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the ngspice specifics and the claim is: **no assertion JLS makes about the
meaning of a drawn circuit should rest only on JLS's own tests.** That is a
first-rate ambition and it is one of the few things in this tracker that would
change what a JLS number *means* rather than what JLS can draw. The three claims
(accepted / agreement within a declared envelope / bounded-and-not-too-good) are
the right decomposition, and AC-3 (statistics as equalities, not bounds) and AC-4
(the anti-cheat lower bound) are genuine verification inventions.

My objection is not to the purpose. It is that the purpose has been welded to the
most expensive available vehicle — a nightly live external solver, a bespoke deck
emitter, and a capstone whose entire value is gated behind 44.5–68 mw of analog
spine it does not itself fund — when the project already contains the cheaper,
more general form of the same idea and does not recognize it as such.

## 1. The generalizable capstone is hiding inside the analog one

"Compare our implementation against one we did not write, on a corpus, within a
declared envelope, with a lower bound so agreement cannot be spurious" is already
shipped three times in this tree:

- `test/jls/hdl/scan/YosysGroundTruthTest.java` — the scanner's port sets checked
  against `yosys write_json`.
- `test/jls/hdl/IverilogCompileTest.java`, `GhdlCompileTest.java` — emitted HDL
  checked by real compilers.
- `riscv/fuzz_diff.py` + `riscv/riscv_ref.py` — differential fuzzing of the drawn
  CPU against an independent reference simulator.
- `ARCHITECTURE.md:360-368` already makes a differential oracle *binding law* for
  any future simulation strategy: it "must agree bit-for-bit with the #202 RV32I
  integration golden run as a differential oracle."

CAP-14 rebuilds the harness, the corpus discipline, the envelope vocabulary, the
promotion-to-required ritual and the lane wiring for a single client, under a
name that ties all of it to ngspice, and none of it becomes reusable. **The
reframing: file the capstone as "every JLS claim about circuit meaning carries a
second opinion from an implementation JLS did not write," with instances for HDL
(iverilog/GHDL/Yosys), for RV32I (`riscv_ref.py`), and — later, if the analog bet
is taken — for ngspice.** Two of the three instances exist today at roughly zero
marginal cost; the shared vocabulary (envelope, lower bound, statistics ratchet,
promotion policy) is written once. As filed, 100% of this capstone's value is
unavailable until an analog solver exists. Reframed, most of it is available now,
and the analog instance inherits a discipline that has been exercised for a year
rather than invented on the day the solver lands.

Corollary worth acting on immediately: AC-3 and AC-4 are not analog ideas.
An equality-pinned statistics header is exactly the detector #232 (sim hot path)
and any future compiled-evaluation strategy (#221) will need, and AC-4's
"agreement too good means you are comparing yourself to yourself" applies
verbatim to `fuzz_diff.py`, where JLS and the reference could share an author's
misreading of the ISA spec. Extract both now; do not hold them hostage to analog.

## 2. The nightly live oracle is the least informative shape available

This is the reframing I would push hardest, and it disregards AC-2's nightly-lane
criterion and OQ-3's "loose oracle" default deliberately.

JLS's own side is bit-deterministic by construction (AC-5). ngspice's side was
measured to drift **5.38e-4** across versions on a two-element linear RC. A
nightly comparison between a zero-variance side and a drifting side can report
exactly two things: *JLS changed* — which JLS's own goldens already report, on
every push, exactly rather than within an envelope, and far more cheaply — or
*ngspice changed*, which is not JLS's business and is the thing OQ-2, OQ-3,
KC-14-2 and the entire §4 envelope derivation exist to neutralize. The nightly
schedule buys near-zero information and costs a CI lane, a self-skip idiom, a
"did it actually execute" DoD line, an out-of-tree re-measurement (AC-8), and a
dependency on #317 which is itself `blocked_by [353, 354]`.

The oracle's irreplaceable value is at **fixture admission**: proving that a new
fixture's expected waveform is physics and not JLS's own bug. That is a one-time
act per fixture, not a nightly act.

**Alternative design — provenance-carrying fixtures.** Each fixture commits four
things in-tree: the emitted deck, the oracle build identifier, the raw oracle
output, and the derived reference waveform. The envelope comparator then runs
offline inside `mvn verify`, on every push, on every platform, with no external
tool installed, never skipping. A maintainer-run refresh *script* (not a CI lane)
re-runs the oracle when a fixture is added or a device model changes, and a
`REPLAN:` records any envelope move. This is not a compromise; it is the shape
the repo already uses one tier up, and CAP-14 cites the file that does it:
`VerilogHeaderScannerTest` hard-codes the corpus expectations so the scanner is
exercised *without* yosys, and `YosysGroundTruthTest` proves those expectations
against the independent implementation when the tool is present. CAP-14 copies
the self-skip half of that idiom and drops the committed-expectations half —
which is the half that makes it work every day.

KC-14-2 calls this "degenerating into a second golden." It *is* a golden, and
that is honest and correct: the failure mode a golden actually has is a forgotten
provenance, and committing the provenance dissolves it. The payoff is not only
simplicity. Pinning removes the 5.38e-4 across-version term — the single term
that forced the envelope out to 3.16e-3. A provenanced oracle can sit near the
`RELTOL` floor of 1e-3, restoring roughly half a decade of detection power that
§4 currently hands to AC-3 alone, and it does so while making the AC-2/AC-4 band
*wider*, not thinner.

## 3. The seam is cut at the file format, not at the model

CAP-14 treats "the deck emitter" as capstone-owned integration work and §3 then
worries at length that it becomes "a fourth renderer" that might fork the net
partition. That worry is a symptom of cutting the seam in the wrong place. A
SPICE deck is a *serialization*. The asset is a **device-level instance IR** —
and the capability roadmap has already argued for exactly that IR on three
independent grounds: `docs/capability-roadmap/README.md:350, :862, :964` (EDIF,
BLIF, structural SystemC and SPICE printers all falling out of one instance IR /
hierarchy), and `sweep-06-physical-boundary.md:82`, which prices "a structural
`.subckt` printer over `HdlExporter.buildModel`" at about a week. `HdlModel` has
ten statement kinds and none of them instantiates a module; that hole is the real
blocker, and it is shared with #307 (KiCad) and with EDIF/BLIF.

Generalize FEAT-004 (#336) from "shared net-partition IR" into that instance IR
and the fourth-renderer risk does not get mitigated — it ceases to exist. There
is one IR and N printers, the deck emitter costs a week rather than an unbounded
share of capstone-owned integration, and the same move pays #307 and three
roadmap programs. That is the architectural seam this capstone should be cutting
along, and it is the one place where CAP-14's own dependency (#336) is *nearly*
right and stops one step short.

## 4. The digital half is the deliverable, and it exists today

OQ-7 option (a) — `yosys write_spice` over the shipped `VerilogEmitter` — buys
claim 1 (a real ngspice accepts a JLS-derived deck) for 0.25–0.5 mw with no JLS
code at all, and it gives a student a self-service verification path this
semester. The issue costs it correctly and then files it as a demo-slice footnote
inside a 50.5–79 mw capstone. Ship it as its own small issue with a documented
recipe and a test. A capability that is available now and gated behind nothing
should not be carried as a bullet under a capability gated behind everything.

## 5. Where this pulls against the project's arc

The project's own most careful analysis says analog is out, in four separate
places, on the same ground each time: `sweep-03-elements-and-hdl.md:633` and
`capability-roadmap/README.md:1037-1041` — "Supporting these means being a
SPICE-class solver — a different tool, not a deeper digital model."
`docs/grand-architecture.md` names three funded trajectories (datapath/CPU
teaching, the FPGA-deployment bridge, the collaborative editor) and analog is
none of them.

AC-7 handles this more honestly than most issues would — it names the refusal,
declines to ignore it, and scopes it correctly ("stands unchanged for any JLS
build that does not fund FEAT-046/049"). But it still resolves a program-level
determination by fiat inside a validation capstone: "this capstone is the
decision to acquire that content." **A capstone whose job is to make an analog
claim believable cannot also be the vehicle that decides JLS should make analog
claims.** That decision belongs in the form this repo already writes well —
an `ARCHITECTURE.md` recorded decision with rationale and a revisit trigger, or a
capability-roadmap amendment — argued on its merits against the three funded
trajectories it competes with for a single maintainer's weeks. Until that
determination exists, #309, #305 and #303 are three capstones resting on an
unmade decision, each pointing at the others.

## What I would keep, unchanged

The claim decomposition; AC-3's insistence on equalities rather than bounds;
AC-4's lower bound; KC-14-5 (frozen positional pin order as a stop condition —
a mis-ordered device line that parses and simulates is the genuinely frightening
failure here); AC-6's refusal to present the mixed-signal boundary as an oracle
result; and the ratified constraint that no external tool ever enters `src/`.
Those six survive every reframing above and should be lifted into whichever
vehicle replaces this one.
