# Issue #516: CAP-32: a prospective user runs a curated JLS example in the browser, read-only, without installing anything — the evaluation-cost gap to web tools closes without JLS becoming a web app
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the mechanism away and CAP-32 is one sentence: *an instructor evaluating JLS should
be able to see a real circuit doing something in under thirty seconds, before deciding to
install anything.* That end is aligned with the project's arc. The README already opens with
installers, checksums and attestations — a distribution story for people who have already
decided — and the shop-window arc (#545 README, #548 curated examples, #551 SVG gallery)
is the project admitting it has no story for people who have not. CAP-32 is the last rung
of that ladder, not a new direction.

Everything below accepts the outcome and rejects the route. **I am explicitly disregarding
PF-1 and its acceptance criteria as written**, and inverting the capstone's ordering.

## PF-1's preferred mechanism contradicts the capstone's own acceptance criteria

CheerpJ-wrapping the Swing jar fails three of CAP-32's four ACs before a stopwatch is
started, so the "measurement, not opinion" framing of #572 is measuring the wrong thing.

1. **AC-3 (static files only, nothing that can die).** CheerpJ's runtime is proprietary and
   loaded at page load from the vendor's CDN; self-hosting is a licensing question, not a
   `cp`. The capstone's whole permanence pitch — *the anti-simulator.io property* — would
   rest on a third-party service that can be discontinued or repriced. That is precisely the
   hazard #886 forswears three paragraphs later under KC-32-4-3 ("no shortener, no
   redirector, no third-party link service"). One rule cannot forbid a link service and
   permit a runtime service.
2. **AC-2 (read-only by construction).** Wrapping the jar ships the *entire editor* — File
   menu, save, load, every dialog — and then makes it read-only by disabling things. That is
   read-only by *policy*, and policy lapses. KC-32-2's "scope cliff" then has to be defended
   by hand forever, against a build that already contains a browser editor.
3. **Architectural direction.** `docs/grand-architecture.md` §3 names the highest-leverage
   change in the tracker as demoting the Swing GUI to *one consumer* of a headless core
   (#77). CheerpJ-in-the-browser does the opposite: it makes the Swing GUI the project's
   public web face, adds a second distribution channel for the most entangled 69k lines in
   the tree, and gives every future GUI change a browser-compatibility obligation nobody
   signed up for. It pulls against the spine.

The load-time threshold in KC-32-1 is the least of it. A go/no-go spike that can only be
answered "no" for reasons already knowable is 1–2 mw spent to reach a conclusion available
today for free.

## The reframing: the demo is a build product of the batch contract, not a second runtime

PF-1's ranked *fallback* (a) — SVG plus a VCD-driven player — is not a consolation prize.
It is the design that falls out of JLS's actual architecture, and it should be funded
directly.

Both halves already ship, headless, from the container image the README documents:

- `-i out.svg` renders the schematic through `CircuitRenderer` on JFreeSVG
  (`src/jls/edit/CircuitRenderer.java:314-358`), byte-identical across runs
  (`SvgExportTest#exportingTwiceIsByteIdentical`).
- `-vcd` emits the signal history from `BatchSimulator`, and `docs/vcd-interop.md` already
  treats that stream as a consumable interop artifact.
- `docs/batch-interface.md` makes the `-t` grammar and the VCD profile a *stability
  contract*.

So the demo becomes: run the shipped batch surface over the curated examples in CI, emit
SVG + trace, and ship a small static player that scrubs the trace and recolors the wires.
Three properties follow that CheerpJ can never have:

- **Fidelity is exact by construction.** The frames the browser shows were computed by the
  real `jls.sim.Simulator`. There is no second engine, so there is no semantic drift — the
  hazard that killed #500 (KC-19-2) simply has no place to live. `docs/simulation-semantics.md`
  gains no second implementation to defend.
- **AC-2 is structural.** There is nothing in the page that *can* mutate a circuit, because
  there is no evaluator in the page. The scope cliff stops being a rule and becomes a fact.
- **It pays rent elsewhere.** A published, regenerated tour is a continuous consumer of the
  batch stability contract and of the SVG exporter — the same artifacts serve #551's gallery,
  #536's lab handouts, and #519's hosted manual. One mechanism, four consumers, versus one
  mechanism serving one page.

**The one honest loss, and how to buy most of it back.** A recorded tour is not "poke the
inputs." Name that plainly rather than pretending. But the interactivity budget can be
converted into a *precomputation* budget: for a curated demo with k toggleable inputs,
precompute the response to every input combination with the real simulator and ship the
table. At k ≤ 12 that is 4096 frames of a handful of nets — kilobytes after gzip — and
"poking" becomes a table lookup that is bit-exact with desktop JLS. Sequential examples get
a scripted stimulus with a scrubber plus a few branch points. The curation constraint this
imposes ("demo circuits have few free inputs") is a *good* constraint for a shop window;
sprawling examples make bad demos anyway.

Concrete enabling change, small and independently useful: emit per-element `<g id=…>`
groups in the SVG export, keyed on the stable element ids that already ship (#165), via
JFreeSVG's begin/end-group rendering hints. The gallery (#551) gets hoverable elements for
free; the player gets its handles; nothing else in the tree changes.

## Sequencing: the capstone is funded ahead of its own content

`examples/` today contains one file, `examples/autograde/autograde.py`. The curated example
library (#548) does not exist, and #551's gallery does not exist. The evaluation-cost gap is
being attacked from the top of the stack down. My strong recommendation:

- Ship #548 + #551 + the README link first, then **measure** — do prospective users bounce
  at a gallery of real circuits with captions and a one-line install? A published gallery
  plus a 90-second screencast plausibly captures most of the win at ~2–3 mw.
- Make CAP-32's runtime work `blocked_by` that measurement, not merely `ordering_after` the
  content. A capstone whose premise is "evaluation friction" should be gated on evidence
  that friction persists after the cheap fixes land.

And the ranked-last fallback (b), recorded walkthroughs, deserves to ship *now*, because
the infrastructure exists: `scripts/wayland-rig.sh` already boots the real GUI under headless
sway in CI and screenshots it with `grim`, and `wtype` already provides synthetic input.
A scripted screencast is a modest extension of a rig that runs on every push — which makes
it a *regenerated artifact that cannot go stale*, not a marketing chore someone re-records
after each release. That is the out-of-the-box move the issue's ranking buries at the bottom.

## Two things in the record that should be corrected

- **The K-12/GCSE clause should be struck from the rationale.** Reaching browser-only school
  students requires a browser *editor* — the thing KC-32-2 and #500's closure refuse. An
  argument that only succeeds if the refused thing is built cannot justify the narrow thing.
  The evaluating instructor is the whole honest audience here, and that audience can install
  software; what they lack is thirty seconds of evidence, which is exactly what the tour gives.
- **#886 (share-by-link) is where this capstone stops being an evaluation funnel.** Its
  justifying comment on this issue quotes this body as recording share-by-link as "a
  candidate extension (viewer + URL)" — that sentence is not in #516. The record is drifting
  toward the refused direction under a new name: user-supplied circuit content contradicts
  AC-2's "no user content" in letter, and the CircuitVerse network-effect argument advanced
  for it is an argument for a hosted community, not for a demo page. Under the reframing
  above #886 becomes void automatically — a link can only carry an arbitrary circuit if
  something in the page can simulate an arbitrary circuit — which is another reason to prefer
  a design where the scope cliff is enforced by physics rather than by vigilance.

## Verdict

**endorse-with-reframing.** The outcome is right and belongs in the roadmap. Kill #572's
CheerpJ go/no-go as posed and fund the SVG-plus-precomputed-trace tour directly as PF-1;
ship the screencast immediately off the existing Wayland CI rig; gate the hosted-demo work
on a measurement taken after #548/#551 land; strike the K-12 rationale; and treat #886 as
out of scope until a maintainer decides, separately and explicitly, that JLS wants
user-content sharing at all.
