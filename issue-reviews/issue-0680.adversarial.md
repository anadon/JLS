# Issue #680: TASK-C527-2: a docked chronogram panel draws live waveforms off the event tap — signals groupable and reorderable, buses in a chosen radix
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

Second of three tasks under FEAT-C23-1 (#527, band 4-6): re-source the trace
window's data from TASK-C527-1's event tap (#678, open/unbuilt,
`ordering_after` correctly declares the dependency), add named/reorderable
signal groups, per-lane radix (bin/hex/dec), a fixture-asserted rendering
test, and a headless-inertness test. `band_mw: 2-3`. I checked the issue
against `src/jls/edit/Trace.java` and `src/jls/edit/InteractiveSimulator.java`
(the files it cites), the sibling tasks #678/#682, the parent feature #527,
and `test/jls/HeadlessCoreRatchetTest.java`.

## Findings, by severity

**1. (High) The Outcome's baseline claim is false against the current code, and the band is priced against that false baseline.**
"A docked chronogram panel replaces 'the trace window shows recorded samples
and that is all'" — but at HEAD, `InteractiveSimulator`'s trace window
already: (a) is docked, not a separate popup — `window` is a `JPanel`
(`InteractiveSimulator.java:72`) embedded via a `JSplitPane`-style divider
("Drag divider up to see signal traces", line ~181) inside the sim's own
window, not a standalone `JFrame`; (b) draws **live**, updating on every
`addValue` call as the simulation runs (`Trace.java:180`); (c) already
renders a multi-bit bus as **one labelled row**, not "N unlabelled rows" —
`Trace.java:328-349` draws a single value string per bus lane via
`BitSetUtils.ToString(change.value(), base)`, exactly the behavior the issue
presents as new; (d) already supports bin/dec/hex selection via `b2`/`b10`/
`b16` buttons (`InteractiveSimulator.java:240-271`) that call the already
per-instance `Trace.setBase` (`Trace.java:90,620`); (e) already supports
lane reordering (`Traces.move`, `InteractiveSimulator.java:1340-1364`) with
positions persisted per-element via `Element.setTracePosition`/
`getTracePosition` (`src/jls/elem/Element.java:182-195`). The only genuinely
new work is: event-tap sourcing instead of direct `addValue` calls from
wire/element watchers, **per-lane** (vs. global) radix — a small change since
`Trace.base` is already per-instance — and **named groups** (vs. flat
reorder). Recommend: rewrite the Outcome to state the real delta, and re-derive
`band_mw` against it; as written the band may be inflated by crediting
mechanism this task gets almost for free, or — if the event-tap re-plumbing
of `Trace`'s update path turns out to be a real rewrite, not a swap — it may
be under-priced. Either way the current text doesn't let a reviewer tell
which.

**2. (High) Acceptance criterion 4's test, as literally described, is a tautology under the existing architecture.**
> "a headless/batch run constructs no chronogram class, asserted by a test that fails if the batch path touches the panel's package"

`test/jls/HeadlessCoreRatchetTest.java` (`CORE_PACKAGE_PREFIXES`, lines
21/74/114/142) already statically forbids `jls.sim` and the other core
packages from importing `jls.edit`, `java.awt`, or `javax.swing`, and the
batch path (`BatchSimulator`, `JLSStart`'s batch mode) already never
references `jls.edit` — that's the existing headless guarantee, not
something #680 adds. If the new chronogram classes are added inside
`jls.edit` (where `Trace`/`Traces`/`InteractiveSimulator` already live), "a
test that fails if the batch path touches the panel's package" passes
automatically regardless of whether the new panel is actually gated
correctly — it only re-proves the pre-existing package boundary. Recommend:
either put the chronogram in its own sub-package so a package-touch test is
meaningful, or replace AC4 with a runtime construction assertion (a counter,
or a classloading check) exercised by an actual batch run, distinct from the
static import ratchet.

**3. (Medium) No acceptance criterion covers the "opened GUI, panel left closed" cost case — the actual point of #678/#527's zero-cost design.**
#527 (the parent feature) frames the whole point as "the tap must be
zero-cost in the default configuration" and assigns `ChronogramClosedCostTest`
to itself (its own AC4), leaving #680 with only the headless/batch case
(finding 2). But #680 is the task that actually constructs a Swing
component tree and (per its own text) "consumes TASK-C527-1's tap" — if the
panel's tap-consumer registers unconditionally whenever `InteractiveSimulator`
starts (mirroring how today's `Traces`/`Trace` objects are already
constructed unconditionally per probed wire in `findTraces`,
`InteractiveSimulator.java:979-1017`, with no default-hidden gate visible in
that path today), then "default-hidden" could legally mean "not painted"
while still subscribed and paying per-event cost — the exact failure mode
#527 exists to prevent, just one layer up (interactive-but-unopened rather
than headless). Recommend adding a criterion here, not just deferring to
#527: an interactive run with the panel never opened registers zero
consumers with #678's tap.

**4. (Medium) "Named groups" vs. the existing per-element `TracePosition` model is underspecified.**
Today's ordering is one `int` per `Element` (`Element.java:182-195`,
consumed by `Traces.setup()`/`move()`), not saved with the circuit (no
`Attribute`/save entry found for it), and recomputed by `findTraces` on every
simulation start. The issue never says whether "groups" are layered beside
`TracePosition`, replace it, or whether group membership survives a full
stop/restart of the simulation — AC2 only promises survival across
"pause/resume within the session," which pause already does for free today
(pausing doesn't tear down or reconstruct the trace panel; see the `paused`
gate at `InteractiveSimulator.java` `mouseMoved`). A naive implementation
that resets groups on every "Run" click, or that never persists them past a
single run, satisfies AC2 as written. Recommend stating explicitly whether
groups persist through simulation restart, and tightening AC2 to something a
naive implementation could actually fail.

**5. (Low) ARCHITECTURE.md misnames this issue's own central file, a landmine for whoever picks this up.**
ARCHITECTURE.md states `InteractiveSimulator` lives at
`src/jls/sim/InteractiveSimulator.java` (lines 58, 170) and is part of
`jls.sim`; at HEAD the file is `src/jls/edit/InteractiveSimulator.java`,
package `jls.edit` — matching what #680's own body correctly cites. Not
#680's defect, but worth a one-line note in the issue (or a doc fix riding
along) so an implementer trusting the architecture doc over `git grep`
doesn't look in the wrong package.

**6. (Low) Default per-lane radix on a newly-added lane mid-run is unstated.**
Moving from one global `displayBase` to per-lane radix (finding 1) raises an
question the issue doesn't answer: what radix does a lane get when a signal
is probed after the panel is already showing others in mixed radixes —
inherit the last-used global default, or a fixed default (e.g. decimal)?
Minor, resolvable during implementation, but worth a sentence so it isn't
decided ad hoc.

## What's solid

- `ordering_after: [TASK-C527-1]` correctly appears in the machine-readable
  frontmatter (unlike #682's sibling issue, which recorded its equivalent
  dependency only in a comment) — this is the right pattern and it's
  followed here.
- The `band_mw: 2-3` sums cleanly with its siblings (#678's 1-2, #682's 1)
  to #527's own stated `band_mw: 4-6` — no hidden budget mismatch at the
  feature level, unlike the pattern found in the #350 decomposition.
- AC3's "the rendered text for a known fixture bus is asserted rather than
  eyeballed" correctly heads off the GUI-testing failure mode (a screenshot
  nobody actually checks) before it starts.
- AC5's scope cut (consume only #678's consumer interface, never a concrete
  queue type) is the right discipline and is verified consistent against
  #678's own AC4, which makes the identical promise about #476/TASK-0063.

## Recommendation

Rewrite the Outcome section against the actual current behavior (finding 1)
so the band and the "what's new" framing are honest; replace or sharpen AC4
so it tests something #680 adds rather than something `HeadlessCoreRatchetTest`
already guarantees (finding 2); add an explicit closed-panel-while-running
cost criterion instead of assuming #527 covers it (finding 3); and state the
group-persistence and default-radix questions (findings 4, 6) before
implementation starts.
