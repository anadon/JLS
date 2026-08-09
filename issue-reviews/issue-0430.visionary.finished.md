# Issue #430: TASK-0088: a net that drives more load than its weakest driver can carry is reported with both numbers, a floating input is caught statically, and a family whose DC check is vacuous never reads "PASS"
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Two different capabilities are welded together here, and they have nothing in common except
the word "electrical".

1. **A topology fact about a drawn circuit**: this input is attached to no driver; this net
   has two push-pull drivers. Its input is the `WireNet` partition and nothing else. It needs
   no part library, no packing, no slice decomposition, no unit loads, no datasheet.
2. **Arithmetic over a part table**: $\sum \ell(p) > \min c(q)$ per physical net. Its input is
   the `Electrical` columns plus a packed netlist over `(refdes, pin)`.

The issue's *motivating* evidence — O1, O2, H2, the whole "a floating input is invisible in
simulation because `initInputs()` writes 0" argument, which is the strongest paragraph in the
body — belongs entirely to capability 1. The issue's *cost*, its three unlanded prerequisites
(#400, #394, unfiled TASK-0087), and its collision with #349's invariant 3 belong entirely to
capability 2. The seam has been cut along the pipeline stage ("everything that happens after
packing") rather than along the data dependency ("what does this verdict actually need to be
true"). That is why the issue reads as 1.5 weeks and is in fact months out.

O1 is real and I verified its mechanism at HEAD: `LogicElement.initInputs`
(`/home/user/JLS/src/jls/elem/LogicElement.java:473-482`) writes a zero BitSet into every
`Input`. But H2's conclusion does not follow. H2 says a floating input "cannot be found by
simulating" and therefore that *this* check is "the only honest report" of it. The premise is
about the **value domain**; the conclusion is about **JLS's ability to detect it**, and the
second is cheap and already sitting in the tree. `Put.isAttached()` exists at
`/home/user/JLS/src/jls/elem/Put.java:271`, `getWireEnd()` returns `@Nullable` at `:256`,
`Put` is `sealed permits Input, Output` at `:16-17` (the issue's own O4), and
`Circuit.finishLoad` already walks every element resolving wire ends. "Every `Input` whose
net contains no `Output`" is a union-find pass of maybe fifty lines over structures that have
existed since the fork. Nothing about it requires a package, a refdes, or `U3.6`.

## The reframing: cut along data dependency, and ship the half that has none

**I am explicitly disregarding this issue's scoping of P1 (`anUndrivenSectionInputIsReported`)
and P6 (two push-pull drivers) to the packed netlist.** Those two predictions should be met on
the *drawn* circuit, in a task with zero `blocked_by`, landing before #400 rather than after
three tasks that do not exist. Concretely:

- **#365's acceptance criterion 8** — *"Every input pin of every used section is in a net with
  at least one driver or one pull; violations are reported before any simulation runs, and
  therefore before FEAT-027 exists"* — is satisfiable today for drawn elements. The
  "used section" qualifier is what forces it after packing, and that qualifier is carrying the
  entire dependency chain for a case (an unused gate in a placed DIP) that is one rule among
  ten. Keep P7 and P9 in the packed task where they belong; free the other eight.
- **Contention** likewise: `WireNet` already knows every `Output` attached to it. "More than
  one non-`TriState` driver" is decidable on the drawn circuit. It does not become more true
  after packing; it only becomes reportable as `U3.6` instead of as a gate the student can
  see on the canvas — and on the canvas is *where the student is*.
- **The fan-out half stays exactly where the issue puts it**, gated behind #400/#394/TASK-0087,
  because it genuinely is arithmetic over data that does not exist. Nothing is lost by
  splitting; what is gained is that the project's single most-cited student failure mode stops
  being hostage to the physical program's critical path.

There is even a cheap 80% of the fan-out claim available pre-packing that the issue never
considers: JLS already knows how many `Input` puts hang off each `Output`'s net. A raw
**sink count** per net, reported honestly as a sink count and not as unit loads, catches
"one NOT gate driving two hundred inputs" — the roadmap's own example at
`docs/capability-roadmap/README.md:653-657` — with no library at all. It is not a datasheet
claim and must not be dressed as one, but the issue's own verdict vocabulary is precisely the
machinery for saying "counted, not rated".

## The channel is wrong, and the issue's own comments already say so

`loading.txt` in the `-pack` output directory is a **third** diagnostic channel. ARCHITECTURE.md
"Error-reporting contracts" pins the two that exist: structured `LoadError` categories, and
`TellUser` as the sole message-dialog boundary with `NotificationRatchetTest` enforcing it.
Meanwhile #486/#607 is building `jls -check` with its own "not assessable" vocabulary. The
maintainer's second comment on this very issue names the hazard exactly — *"students get two
vocabularies for the same idea and a green summary line will eventually paper over a vacuous
check"* — and then resolves it with a coordination note asking whoever lands second to copy
whoever lands first. That is the correct diagnosis and the weakest possible remedy.

The reframing that makes the problem disappear: **one static-check rule engine, and every
check is a rule in it.** A rule declares an id, a severity default, and the data it requires;
it returns structured `Finding`s (location, both numbers, the missing datum) rather than text.
Then:

- `NOT_DC_LIMITED` stops being a bespoke enum member and becomes the general **NOT_ASSESSED**
  outcome *carrying which datum was absent* — the same outcome #607 needs for an un-annotated
  net, and the same one a future combinational-loop or clock-into-data-pin rule will need.
  H3's truth claim is then structural rather than a discipline everyone must remember.
- The **exit mapping of P11 becomes per-rule severity policy** (error / warn / off) instead of
  a hardcoded verdict→status table. This is what makes the check adoptable: an autograder wants
  a subset, a course wants to warn this term and fail next, and Open Question 1 ("does
  `UNDECIDABLE` exit 1?") stops being a blocking decision and becomes a default.
- **TASK-0091's duplication threat evaporates.** §11 worries, correctly, that the
  manufacturability gate will re-implement fan-out. A gate that consumes a *report file* must
  parse it and will be tempted to recompute; a gate that consumes `List<Finding>` from a rule
  registry cannot re-implement anything. §7.5's answer — package privacy plus a §12 ownership
  sentence — is a social fix for a structural problem.
- **Rendering follows the findings, not the reverse.** `loading.txt` becomes one formatter over
  the finding list; a canvas marker on the floating pin becomes another. The roadmap's own
  item G (`docs/capability-roadmap/sweep-06-physical-boundary.md`, "Electrical rule checking —
  the honest, small answer") specifies exactly this: *"reports through the existing
  `LoadError`-shaped structured-diagnostic discipline and the `TellUser` boundary"*, sized at
  3–5 weeks and called *"the one thing JLS most conspicuously lacks against Digital and
  Logisim-evolution."* #430 is item G's fan-out clause, delivered without item G, into a file
  in a directory that only exists for users doing physical packaging. JLS is an educational
  editor; the highest-leverage delivery of "your input is floating" is red on the pin while
  the student is drawing it, and this design has structurally excluded that
  (`jls.pkg`, zero GUI, headless leaf).

## Where the issue pulls against the arc, and where it is right

**The one-way constraint from #349/#400 is the real risk, and the issue's rebuttal is its
weakest paragraph.** #349 §4 invariant 3 ships the electrical columns inert — *"no pass may
interpret them until the strength model exists… a fan-out check reporting numbers the
simulator contradicts"* — pinned by #400's architecture test asserting no non-test reader. The
issue answers by declaring FEAT-027 a "deliberate non-dependency". The failure mode that
invariant is protecting against is real and pedagogically corrosive: a student is told
`OVER_FANOUT: 14 unit loads on a driver rated 10`, runs the simulator, watches it work
perfectly, and learns that JLS's reports are noise. That lesson is *worse* than no check.

The escape is not to gate on #341. It is to make the report visibly **not a claim about the
simulation**. §7.12 gestures at this; P12's header lists what is not *modelled*. Strengthen it
to name the **divergence**, not just the omissions: the header should say that JLS's simulator
does not model DC loading and will therefore never disagree with this report — the numbers
describe the parts, not the run. That single sentence is what makes IC-7 relaxable by a named,
tested exception rather than by deletion, and it is what makes the check honest as a
*build-plan* check rather than a half-implemented simulator feature. The issue is already
90% of the way there in temperament; it just has not said the load-bearing sentence.

**What I would keep verbatim and promote to house style.** The guard order of §7.10
(`UNDECIDABLE` before `NOT_DC_LIMITED` before `OVER_FANOUT`, with the reason stated as
semantics rather than convenience); minimum-not-sum for governing capacity; integral unit
loads with no floating point; both numbers on every failure rather than a verdict; the
exact-string assertion in P4; the report header naming its own non-coverage. These are better
than most of what ships in commercial ERC, and they should not stay the private property of
one task — they are the spec for every static check JLS will ever emit, including #607's.

## Summary

Endorse the capability; reject the packaging. Split at the data seam: one unblocked task for
undriven-input and contention over the `WireNet` graph, one gated task for unit-load arithmetic
over the packed netlist. Land both as rules in a single `jls -check` engine returning structured
findings with per-rule severity, so the fan-out verdict, #607's transmission-line verdict, and
the canvas marker are three consumers of one vocabulary rather than three vocabularies. Add one
sentence to the report header stating that the simulator does not model this and will not
disagree — which is what converts #349's invariant from a blocker into a satisfied precondition.
