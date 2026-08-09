# Issue #572: FEAT-C32-1: the browser-demo go/no-go lands on measurement — a CheerpJ-wrapped jar runs the Swing GUI read-only on the three biggest examples, or a ranked fallback is chosen without re-litigation
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Not "does CheerpJ work." The real job is: **name one browser mechanism the project can
stand behind for a decade, and make that name binding** so #573, #574 and now #886 stop
re-arguing it. The discipline — time-boxed, ends in a written verdict, verdict is binding
(KC-32-1) — is exactly right and I endorse it without reservation. What I do not endorse is
the shape of the measurement: **a load-time number cannot decide a supply-chain question**,
and the issue has quietly made the load-time number the whole decision procedure.

I am explicitly disregarding AC-1's corpus ("the three biggest examples") and AC-4's
candidate ordering (CheerpJ default, SVG+VCD as fallback). Reasons below.

## The measurement measures the wrong axis

Every distribution decision this project has ever recorded turns on provenance and
permanence, not on speed. README lines 51–60: reproducible jar and `bom.json`, signed
attestations, checksums whose scope is spelled out. Lines 62–70: the rpm/AppImage carry
*no* project GPG key because single-maintainer key custody would add risk without adding a
guarantee (#136). Lines 37–43: macOS is left unsigned on principle rather than pay Apple.
The capstone itself encodes the same value as AC-3, "nothing that can die and take user data
with it — the anti-simulator.io property."

CheerpJ is a closed-source commercial product from a single vendor whose standard
integration pulls a multi-megabyte runtime from that vendor's CDN, and whose self-hosting
and licensing terms are the vendor's to change. Set aside whether GPL-3.0-or-later
aggregation is clean (it probably is) — the structural facts are that a CheerpJ demo would
be **the first artifact this project publishes that cannot be rebuilt from its own source**,
and #572 as written contains no criterion that would catch that. AC-5 says "static files
only — no backend is stood up," which a CheerpJ page satisfies on a literal reading while
depending on a third-party runtime host that is a backend in every way that matters to AC-3.

A spike that returns "go" at 12 seconds is therefore capable of binding #573, #574 and #886
to the one mechanism most at odds with the project's own arc. That is the failure mode worth
designing against, and it is cheap to design against: **add a pre-numeric gate.** Before any
stopwatch, the spike must answer, in writing — can the runtime be vendored in-tree and served
from our own static host under a license we may rely on indefinitely; does the published page
rebuild deterministically from a tagged commit the way the jar does (`docs/reproducibility.md`);
does it carry a BOM entry. Any "no" is a no-go regardless of load time, and that verdict is
knowable in an afternoon of license reading, before a single measurement.

## The reframing: the fallback is the frontrunner, and it is nearly built

"Alternative (a)" is described as a consolation prize. In this codebase it is the natural
mechanism, and both halves already ship as contract-bound, byte-deterministic outputs:

- `src/jls/edit/CircuitRenderer.java:313-358` — SVG export via JFreeSVG 5.0.7 (~50 KB, zero
  transitive deps, same license as JLS; `pom.xml:68-73`). It is *deliberately* deterministic:
  fixed `defsKeyPrefix`, wires and parts split into two layers, each sorted by a total order
  on index bounds, class name and stable element id — the comment says byte-identical goldens
  are the point.
- `src/jls/sim/BatchSimulator.java` `toVcd` — IEEE 1364-2001 VCD, deterministic byte-for-byte,
  guarded in CI by a spec-derived parser (`VcdExportGoldenTest`), and covered by
  `docs/batch-interface.md`, which the README calls a documented stability contract.

The missing piece is small and lands on a seam that already exists. The SVG writer's per-layer
draw loop is exactly where a `SVGHints.KEY_BEGIN_GROUP` / `KEY_END_GROUP` pair goes, emitting
`<g id="jls-net-…">` around the wires of each named net so a player can recolor them. The
identity to key on is already stable (element ids, #165), and the nets that matter are the
watched/probed ones that already become VCD signals (`BatchSimulator` ~line 120). Then perhaps
200 lines of vanilla JS: parse the VCD, scrub a timeline, set `stroke` on the matching groups.
No framework, no build step, no vendor, `file://`-openable, and reproducible from a tagged
commit by the same recipe as everything else JLS ships.

This is not a smaller version of the CheerpJ demo. It is a **different and better artifact**:
it is read-only *by construction*, which is what AC-3 claims and what only this option
delivers. A CheerpJ page ships the entire Swing editor — including `FileAbstractor`'s
untrusted-container sniffing surface, the whole point of #38's hardening — into a browser tab,
and then asks the demo to promise not to expose the file menu. Read-only becomes a
configuration, one dialog away from being wrong. The SVG+VCD player has no loader to disable.
The same asymmetry applies to KC-32-2's scope cliff: under CheerpJ the cliff is a policy
holding back a page that already *is* the editor; under (a) it is physics.

## The out-of-the-box move: precompute the interaction, don't simulate it

The stated reason (a) ranks second is that a VCD is a recording — you can scrub it, you cannot
poke it, and CAP-32's minimum bar is "toggle inputs and observe the trace." That gap closes
without a browser simulator, because **the demo set is curated**, and curation is a license to
precompute.

For a small curated example (half-adder, mux, D flip-flop, the 4-bit ALU), enumerate the input
space — and for sequential parts the reachable (state, input) transitions — headlessly, in CI,
using the `-t` test-vector grammar that is already a stability contract, and ship the result as
a compact lookup table beside the SVG. The browser then does table lookup. Toggling an input is
genuinely live; the user cannot tell the difference; and there is **no second engine** — every
value in the table was produced by the one discrete-event interpreter that #221/#498 §7.1 makes
the sole strategy. This is precisely the objection that killed CAP-19: KC-19-1 fired on the
derived-runtime framing, and KC-19-2 feared "a plausible-looking circuit that quietly disagrees
with JLS." A lookup table cannot disagree with JLS; it is JLS's output. The refusal that closed
#500 does not reach this design, and #572 should say so out loud rather than inheriting the
nervousness.

Where enumeration does not fit (the RISC-V CPU), the honest answer is scripted playback with
scrubbing and a caption — which is a better first 30 seconds for an evaluator than a CheerpJ
tab slowly booting a CPU anyway.

## Corpus: measure the smallest, not the biggest

AC-1 picks the three biggest examples. For a go/no-go on a *feasibility* substrate that is a
reasonable worst case; for the demo it is the wrong content and it silently sets the bar the
whole capstone is judged by. A prospective evaluator's first click should land on the smallest,
most legible circuit — that is where the evaluation-cost win actually lives. Measure the three
the demo will *lead with*, plus one large circuit as a declared ceiling probe, and record the
ceiling as a content constraint rather than a mechanism verdict.

Practical note: the corpus does not exist. `examples/` holds only `autograde/autograde.py`;
the curated set is #548 (open). The only committed circuits are `test/fixtures/*.jls` (654 B to
120 KB) and `riscv/gui/cpu.jls`. AC-1 is unexecutable today; the spike must either name its
corpus from those fixtures or state that it is ordered behind #548 — the capstone records that
ordering, this issue's `ordering_after: []` drops it.

## Trajectory: which choice strengthens the arc

Choosing (a) collapses effort into a lane the project is already walking: #551 publishes static
SVG renders of examples, and the player is one increment on that surface rather than a second,
unrelated web technology sitting beside it. #573 becomes "add a `<script>` to the gallery," and
#574 becomes near-free. Choosing CheerpJ forks the web presence into two stacks that share
nothing.

It also settles #886 (share-a-circuit-by-link) the right way. #886 is `blocked_by` this issue
and, under CheerpJ, is a full circuit simulator in a URL fragment — CAP-19's refused Open
Question 2 returning through the back door with the maintainer's closure still on the record.
Under (a) it is simply impossible: you cannot precompute a stranger's circuit. That #572's
verdict silently decides this should be stated in the issue, because it is the largest thing
riding on the answer and it is invisible from the acceptance criteria as written.

## Concrete restatement of the spike

Same time box, same binding-verdict discipline, three changes: (1) a provenance/permanence gate
that runs before the stopwatch and can veto on its own; (2) candidate order inverted — build the
SVG+VCD player skeleton on one fixture *first*, since it is a day's work on existing outputs, and
measure CheerpJ only if that fails a bar it is very unlikely to fail; (3) corpus = the three
smallest curated circuits plus one ceiling probe. If (a) clears the bar, the verdict is written
and CheerpJ is never measured — which is a better outcome than a measured tie, and is the version
of this issue that leaves the project with an artifact it owns.
