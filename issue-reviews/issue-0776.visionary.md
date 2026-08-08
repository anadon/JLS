# Issue #776: TASK-C588-2: the timing-honesty note publishes, sourced to CircuitVerse's and Logisim-Evolution's own reports of their timing behaviour
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

#520 diagnosed correctly: in this niche, prominence flows from citable documents,
not features, and JLS's genuine edges are *claims* until a stranger can check
them. #776 is the timing half of that. The deliverable — one short, sourced,
publishable note — is right, and the tone constraint ("written to be quoted back
at us without embarrassment") is exactly the right instinct.

But the acceptance criteria describe a document whose center of gravity is three
competitor tracker issues (AC-3), with JLS's own limits admitted only through
AC-5's single-competitor-advantage token. That ordering is backwards for this
particular topic, and the project's own files say why.

## The problem: JLS's timing model is the weakest place to attack from

`docs/capability-roadmap/sweep-02-timing.md` §0 is the most rigorous self-audit in
this repository. Its verdict, in its own table: of fourteen timing concepts, JLS
can express **one** — per-instance scalar delay. It cannot express per-arc delay,
rise/fall asymmetry, min:typ:max, inertial delay, interconnect/fanout delay,
setup/hold, timing violations, physical time units, clock intent, or static timing
analysis. `docs/simulation-semantics.md` §1 states plainly that simulation time is
"a dimensionless non-negative 64-bit integer… nothing binds them to seconds," and
that VCD's `1 ns` is "a tool-compatibility mapping only." §6.2 states that delay is
pure transport with no glitch suppression. Wires are ideal (`WireNet.propagate`,
synchronous, no fanout term). And sweep-02's ripple-carry answer is devastating in
the right way: the carry glitch **is** simulated, is recorded by the probe, and
then `Trace.paintComponent`'s `int rlen = (int)Math.round(len)` rounds it to zero
pixels — "simulated, invisible in practice, and unexplained."

Now put CircuitVerse #1412 next to that. Their sin is that per-element "delay" is
queue priority rather than simulated time. JLS's tick is *also* not physical time;
JLS's advantage is that its ticks are ordered, additive, and specified, not that
they are real. That is a genuine and important difference — but it is a difference
of *degree and specification*, not of kind, and a note that leads with #1412 while
burying §1's dimensionlessness is precisely the note that gets quoted back at us.
The risk is not that the citations are wrong; it is that the frame invites a reply
we cannot answer: "your nanosecond is fake too, and you don't even draw the glitch."

## Reframing 1: invert the note — self-disclosure first, comparison as consequence

**I am disregarding AC-3's primacy** (not the citations — the ordering). The note
should open with JLS's own timing model and its limits, stated as flatly as
sweep-02 §0 already states them, and reach the competitors only as the second
move. Roughly:

1. **What JLS's clock means.** Dimensionless ticks; transport delay; per-instance
   scalar; ideal wires; two states plus HiZ; no X. Cite `simulation-semantics.md`
   §1/§6.2/§7 by section, and lift sweep-02 §0's fourteen-row table nearly whole —
   thirteen "no"s in a published comparison note is the single most credible
   paragraph the project could print. This *is* the timing-honesty claim.
2. **What that buys.** Ordering is deterministic and specified (§3), delays
   compose additively and are per-instance and persisted, transients survive
   (transport, not queue priority), goldens pin it, and a batch run is
   byte-reproducible.
3. **Where the competitors sit**, with their own citations: CircuitVerse #1412,
   #5328/#1753/#2198, Logisim-Evolution #2454.
4. **Where they are ahead**, named plainly and with more than one entry.

Under this ordering AC-5 stops being a fig leaf. The honest version of this note is
roughly half about JLS's own limits, and it is *stronger* for it — an instructor
who reads a vendor admitting thirteen missing timing concepts believes the
fourteenth claim.

## Reframing 2: the durable axis is specification, not behavior

Behavioral claims about competitors rot — that is why #588/#778 need a freshness
gate and a retraction policy, and why KC-36-1 exists. But the claim JLS should
actually be making does not rot at all:

> JLS is the only tool in this comparison with a normative, code-anchored
> specification of what a simulation *means*, plus tests that fail when the
> document and the code diverge.

That is `docs/simulation-semantics.md` (526 lines, every claim carrying a `file`/
method anchor, with an appendix listing behaviors nobody would have specified
deliberately as candidate bugs), `SimulationSemanticsRegressionTest`, and three
golden suites. It is a **does-this-artifact-exist** claim, checkable in thirty
seconds by following a link, and it stays true whether or not CircuitVerse fixes
#1412 tomorrow. When they fix it, the note gets *better*: "they fixed it — and
note that you could only tell because we read their tracker; there is still no
document to check the fix against."

Practical consequence: pivot the note's spine to this axis and most of #778's
per-release recheck machinery becomes unnecessary *for this note*. Only the section
3 paragraphs are perishable; sections 1, 2 and the specification claim are not.
That is a real reduction in ongoing cost for a 0.5–1 mw task that would otherwise
buy a permanent maintenance obligation.

## Reframing 3 (the out-of-the-box one): ship a semantics quiz, not an argument

The most citable thing here is not prose. It is a small, tool-neutral **timing
questionnaire with committed circuits**: does a pulse narrower than a gate's delay
survive? does a NAND latch trip an oscillation heuristic? is delay per-instance or
global? does the exported HDL simulate like the drawing? Publish it as
`examples/semantics-quiz/` alongside `examples/autograde/` (which already has
`AutogradeBridgeExampleTest` as the executable-docs precedent this repo uses), with
JLS's answers recorded as golden output and an explicit invitation for other
projects to record theirs.

That converts advocacy into an instrument. It is exactly the artifact shape #778
wants, but pointed outward and reusable; it makes AC-5 mechanical rather than
diplomatic (a competitor's better answer is just a row); and it is the kind of
thing a SIGCSE/WCAE reviewer cites, which is #520 PF-4's actual goal. A note that
says "here is the quiz, here are our answers, run it yourself" cannot be accused of
cherry-picking.

## Two alignment hazards worth naming

**Duplication.** sweep-02-timing.md is 862 lines and already contains everything
this note needs. The note must be a ~1500-word front door that *links* into
sweep-02 §0 and simulation-semantics §1/§6.2/§7, not a lossy paraphrase — the
project's established pattern (README → normative docs, ARCHITECTURE → semantics)
already handles this. Two documents drifting on the same audit would be a
self-inflicted wound on a project whose whole identity is that documents don't
drift.

**Forward compatibility with the roadmap.** sweep-02 contemplates changes A
(per-arc/edge/corner delay), D (timing checks), E (STA), F (time units) — and #221
binds any future execution strategy to bit-identical agreement with today's
semantics. A note that markets "delay-accurate semantics" as a fixed virtue makes
those changes read as contradictions later. Fix: version-pin the note ("what JLS
5.x means") and add a short "what we intend to change, and why it isn't built yet"
section pointing at sweep-02. That turns the marketing liability into a roadmap
billboard, and it is the same move `docs/reproducibility.md` makes with its table
of what does *not* reproduce.

## One precondition the issue assumes and the tree does not have

#588 sources the required citations from `docs/reviews/evidence/2026-08-niche-survey/`.
That directory does not exist at HEAD (`docs/reviews` is absent entirely). Either
#510's evidence files land first or the note re-derives the citations from the
competitor trackers directly — worth resolving before this task starts, since
"cited to the competitor's own tracker" is the load-bearing AC.

## Restated acceptance criteria

- AC-1 unchanged (path, ~2500 words, dated "checked against" line).
- **New AC-0:** the note states JLS's own timing limits before it states anyone
  else's, reproducing sweep-02 §0's expressible/not-expressible table.
- AC-3 kept in full — the citation set is right — but demoted from spine to
  section 3.
- **AC-5 strengthened:** at least *two* competitor advantages, and the note
  explicitly answers "is a JLS tick physical time?" with "no."
- **New AC-6:** the note is version-pinned and carries a "what we intend to
  change" pointer to sweep-02, so roadmap growth is not a later contradiction.
- **New AC-7 (stretch, or split to a sibling task):** `examples/semantics-quiz/`
  with committed circuits, JLS's recorded answers, and one documented command.
