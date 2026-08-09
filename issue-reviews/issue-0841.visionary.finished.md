# Issue #841: TASK-C573-2: the whole curated example set is served with its captions, each reaching interactive in under thirty seconds from the click
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the acceptance criteria away and #841 is the moment CAP-32 (#516) stops being a
demo and becomes a *shop window*: the point where a stranger who has never heard of JLS
can poke ten real circuits instead of one, and where the project's answer to "can I try
it without installing?" changes from no to yes. That goal is right, it is the cheapest
adoption lever on the board (#511's survey scored the on-ramp 2/5), and it does not pull
against the architecture: `docs/grand-architecture.md` §1 forbids the *product* from
assuming a network or a server, and a read-only page built from a tagged jar assumes
nothing about the desktop tool at all.

But #841 is also the first issue in the CAP-32 stack where the *scale* of the catalogue
interacts with the mechanism chosen at N=1. Three of its five ACs are shaped by that
collision, and two of them accept the collision instead of resolving it. That is where
I would cut differently.

## Reframing 1 — do not drop examples to fit the mechanism; tier the mechanism to fit the examples

AC-3 is the tell. It pre-authorizes deleting curated content from the demo when an
example cannot reach interactive in 30 s, and calls that good hygiene. It is not: #548's
set is deliberately spanning (combinational, sequential, FSM, datapath, **the RV32I
showcase**), and the examples most likely to blow the budget are exactly the ones that
prove JLS is not a gate toy. A demo that silently omits the CPU because CheerpJ boots
slowly on it has cut the single most differentiating circuit in the repository
(`riscv/gui/cpu.jls`) in order to protect a mechanism decision made against a
one-example page.

The out-of-the-box route is that **the demo does not need one mechanism.** #572 already
ranked a fallback — headless-rendered interactive SVG driven by a pre-computed VCD — and
treats it as the consolation prize on no-go. At catalogue scale the ranking arguably
inverts:

- **Tier A (every example, instant):** `-i example.svg` + a VCD-derived value-change
  track + the caption. Bundle is tens of KB, click-to-interactive is sub-second, it
  works on the phone-class devices CAP-32's K-12/GCSE segment actually uses, and it is
  byte-reproducible for free (#840 AC-5) because it is produced by two flags that are
  already a **stability contract** (`docs/batch-interface.md` §4, `-i` per #154,
  `-vcd` per #72, both golden-tested).
- **Tier B (a small number of showcase circuits, full fidelity):** the #572 mechanism
  running the real Swing surface, where paying 15–25 s of boot buys a genuinely
  unrestricted circuit.

Under this framing the AC-3 exclusion machinery disappears: nothing is dropped, because
nothing is forced through a single pipe. What remains is a *placement* decision per
example, which is content curation rather than content loss.

Concretely, Tier A is a committed generator that is a loop over shipped flags:

```
jls -b -t vectors/<ex>.txt -vcd out/<ex>.vcd -i out/<ex>.svg examples/<ex>.jls
```

For combinational examples the generator enumerates the input space (a 4-input adder is
16 vectors); for sequential/datapath ones it replays the example's shipped test vectors
and the page offers scrub/step rather than free toggling. That distinction should be
stated on the page, not hidden — "this one you drive, this one you replay" is honest and
still answers the evaluator's real question, which is *agency over a live circuit*, not
pixel-fidelity to a Swing frame.

## Reframing 2 — AC-4's drift check is a duplication smell; make drift impossible instead

AC-4 asks for "a committed check [that] fails when the demo's example list and the
shipped curated set disagree." That is CI paying rent on a second list that should not
exist. Note what the project does everywhere else: `HeadlessCoreRatchetTest`,
`SaveTags.resolve` instead of `Class.forName`, `ElementConstructorContractTest` —
`docs/grand-architecture.md` §10 states the ethos outright ("boundaries are enforced,
not aspirational"). The corresponding move here is one enumerator, not two lists plus a
comparator.

Right now **four** open issues each independently need "the list of curated examples and
their captions": #548 (Examples menu), #551 (SVG gallery), #841 (demo catalogue), #574
("Try it in your browser" links). #841 as written builds the third copy and then adds a
test to keep the copies honest. The better seam — and it belongs in #548, not here — is
a tiny classpath-backed catalogue in the headless core (an index resource under
`resources/samples/` plus caption extraction from the circuit itself, honoring the #130
never-`user.dir` rule). Every downstream surface then *renders* the catalogue rather than
restating it, and a renamed example cannot miss the demo because there is nothing to miss
it from.

This also resolves a latent conflict #841 does not notice: #548 AC-3 puts the caption in
a **caption element inside the .jls** (consistent with #73's 2026-07-17 "header Text
element" resolution), while #841 speaks of "the same example manifest." If #841 invents a
sidecar manifest, it forks the source of truth that #548 deliberately put inside the
circuit. Extract captions from the circuit through the ordinary reader; the manifest, if
any, is generated, never authored.

## Reframing 3 — measure a boot budget and a switch budget, not ten copies of one number

AC-2 wants click-to-interactive measured *per example*. Under any jar-in-browser
mechanism the dominant term is the runtime + jar download and JVM warm-up, which is
constant across examples; the per-circuit delta is load + `finishLoad` + first paint,
almost certainly under a second for everything except the CPU. Ten recorded numbers that
are the same number, plus cache-warmth noise, is measurement theater.

The shape that follows: **one demo instance with an in-page example picker**, not N pages
each re-paying boot. Then the honest instrument is two numbers — cold boot (once, on a
stated reference browser/connection) and per-example switch (warm) — and the 30 s
capstone bar is met by construction for examples 2..N. This is also strictly better UX:
an evaluator browsing the catalogue is the exact user CAP-32 is for, and making them
re-boot a JVM per click is the friction the capstone exists to delete. #574's per-example
links still work; they deep-link into the one instance.

## The property AC-5 asserts may not survive the mechanism — check it here

AC-5 says the "read-only, no-backend, static-files properties of TASK-C573-1 hold
unchanged across the full set." Worth naming plainly, because #841 is where it becomes
load-bearing: the presumed #572 substrate is normally bootstrapped from its vendor's own
CDN, and self-hosting its runtime is a licensing question, not merely a `cp -r`. If the
shipped page fetches a third-party runtime at load time, then CAP-32 AC-3 — "nothing to
operate, nothing that can die," the explicit anti-simulator.io promise — is false, and
falsified by a dependency the project does not control. That must be verified and
recorded *at catalogue scale* (it is the difference between one page and the whole shop
window going dark), and if self-hosting is not permitted, that alone promotes Tier A from
fallback to primary.

## Where the reframing costs something (stated honestly)

- **#886 (share-a-circuit-by-link) needs a real in-browser simulator.** A pre-computed
  SVG+VCD page cannot run an arbitrary circuit arrived from a URL fragment. The tiering
  above keeps Tier B alive precisely so #886 retains a substrate; a pure Tier A demo
  would void #886 as written (its own KC-32-4-1).
- **Tier A is two artifacts to keep in step** (render + trace). That is #551's render-drift
  risk again, and its answer applies unchanged: one scripted regeneration command, run in
  CI, from the circuit files themselves.
- **Free input toggling on sequential examples is not free.** Enumerating input space is
  fine for small combinational circuits and wrong for a CPU. Say so on the page rather
  than pretending otherwise.

## What I would keep from the issue exactly as written

The intent behind AC-1 — every curated example reachable, with its in-tree caption,
generated rather than hand-kept — is the correct instinct and the most important
sentence in the issue. The refusal to invent demo-only captions is right. The insistence
that a bad first impression is worse than a missing one is right; I only disagree about
which lever fixes it. And the ordering (`after #840`, `after #548`) is correct: proving
one page before scaling, and never authoring circuits here.

## What I am explicitly disregarding

- **AC-3 as written.** I would not ship an exclusion mechanism. Replace with: every #548
  example appears in the demo; each is placed in Tier A or Tier B with the measurement
  and the reason recorded; zero examples are absent.
- **AC-4 as written.** I would not ship a list-comparison test. Replace with: the demo
  enumerates the shipped catalogue at build time from the same core enumerator #548
  ships, so a divergent list is unrepresentable; the CI check that remains is that the
  generator ran and its output is byte-identical on rebuild.
- **AC-2 as written.** Replace per-example click-to-interactive with cold-boot plus
  warm-switch budgets against a stated reference browser and connection.

Endorse the outcome, re-cut the route: the catalogue is the first place JLS's zero-install
story meets its own content, and it should bend the mechanism around the RV32I CPU rather
than bend the example set around the mechanism.
