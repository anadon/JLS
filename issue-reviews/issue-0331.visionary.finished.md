# Issue #331: FEAT-049: a student draws analog devices the way they already draw gates, and the circuit they drew converges
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the machine block and the three unfiled tasks away and #331 makes one
claim: *a drawn analog circuit should behave like a drawn digital circuit —
placed from a palette, saved in the same file, and when it fails, told about
in terms of the thing you drew.* That claim is right, and the two best ideas
in the issue (diagnostics named in drawn elements rather than matrix rows;
vendor models read as data rather than curated in tree) are ideas the rest of
JLS should steal. What I do not accept is the shape: a fourth permit on a
sealed hierarchy, twenty-two registered element types, and a palette "view
dimension" invented to contain them. The tree already has a better seam, it
already ships, and cutting there makes most of this feature's stated work —
including the prerequisite it puts on its own critical path — stop existing.

## 1. The arc conflict has to be settled above this issue, not inside it

`docs/capability-roadmap/README.md:1037-1044` puts continuous-time and analog
in §6 "What still stays out", ground (a), *different tool class*;
`sweep-06-physical-boundary.md:553-556` says outright that adding a
continuous-time solver "is building SPICE, which is ground (a)".
`docs/grand-architecture.md` — the document that claims to name "the single
most correct target architecture to converge on" — names three funded
trajectories (CPU teaching, FPGA bridge, collaborative editing) and does not
contain the word *analog* anywhere. Neither document is amended in this tree.
So the repository currently asserts, in its own normative planning documents,
that the program #331 belongs to is out of scope, while scheduling 50+
maintainer-weeks of it across #303/#305/#309/#331/#351/#368.

That is not an argument that analog is wrong. It is an argument that the
decision was made in `docs/plan/evidence/` (a directory that does not exist
here) and never propagated to the documents a contributor actually reads.
Concretely: before any analog code lands, amend capability-roadmap §6(a) and
grand-architecture §2, and add a "Recorded decisions" entry in
`ARCHITECTURE.md` in the house form — rationale plus revisit trigger — the
way internationalization, plugin trust and #221's execution strategy are
recorded. One paragraph of documentation is the cheapest possible thing here,
and without it every reviewer of every analog PR re-litigates the premise.

## 2. Progressive disclosure is already solved, by a mechanism this issue does not use

TASK-0105 — "palette totality gains a view dimension" — sits on this issue's
critical path, blocks TASK-0103 by necessity, and is shared with #329. It is
unnecessary. `src/jls/edit/GuiExtensionPoints.java:25` declares
`gui.palette-contributor` (cardinality *many*), `src/jls/boot/GuiModule.java:41`
contributes one `PaletteEntry` per row of the static table through it, and
`docs/extension-points.md:31` catalogues it as typed and shipped. Palette
content is already a per-module contribution pulled from a seam on demand;
`jls.module.ModuleManifest` already carries an `Activation` policy.

An analog module that is not activated contributes nothing. The first-year's
toolbar is unchanged *by construction* — no view enumeration, no preference,
nothing to leave on, which is exactly the property §2 rejection 3 wanted and
built a new dimension to get. The totality ratchet then reframes from
"exactly one entry in exactly one view's palette" to the simpler and more
general **"every type a module provides has exactly one palette entry from the
same module, and the core module's row count is pinned"**. That composes with
#329's breadboard, with #212's external providers and with #84 without any of
them agreeing on an enumeration of views.

I am explicitly disregarding two stated acceptance criteria here: §4
invariant 2's `|pal(v_default)| = 32` as a *view*-scoped pin (keep the number,
scope it to the core module), and §6's "TASK-0105 before TASK-0103, by
necessity". Under module-scoped totality there is no ordering edge, TASK-0105
disappears as an analog prerequisite, and the residual shrinks by a task.

## 3. Twenty-two element types is the wrong quantity — the right quantity is one

§2 rejection 1 already proves, on coverage arithmetic, that there must be
**one generic dialog and one generic renderer** for all devices. The issue
stops the genericity there and still registers a class per device. Follow the
argument one layer down: if the dialog is driven by parameter descriptors and
the renderer by static symbol paths, then a device *is* its model card, and
the element type is a **`Part`** whose parameter is a card reference — with
perhaps a handful of topological variants (2-, 3-, 4-terminal) rather than
twenty-two registry rows. §2 rejection 5 already commits to vendor `.subckt`
models being read as data; this is the same decision applied to the primitives.

What disappears when you cut here:

- The 69% palette-growth problem, and with it most of the pressure behind §2.
- Twenty-two `SaveTags` rows and twenty-two `ElementRegistry` entries, each of
  which is a permanent forward-compatibility commitment under
  `ARCHITECTURE.md`'s frozen tag table.
- The `UNKNOWN_ELEMENT` cliff: a file using a device this build has no card
  for currently cannot load at all. With a card reference it loads and reports
  an unresolved-model diagnostic — strictly better behaviour, and the same
  shape as the `NEWER_FORMAT` refusal the loader already does well.
- "The remaining fourteen device types" residual, which becomes fourteen data
  files and stops being engineering work at all.
- Most of the 8,250-line coverage exposure §2 is defending against.

This also lands the project's **first data-only extension surface**, which is
precisely what `ARCHITECTURE.md`'s plugin-trust decision says it prefers ("the
closed, typed provider API is the plugin analogue of collab's closed data-only
op vocabulary"). A device card carries no JVM authority, so unlike #212's
trusted-jar opt-in it needs no trust statement — only hostile-input hardening.
That last point is missing from the issue: a downloaded `.subckt` is untrusted
input crossing a boundary and belongs in `SECURITY.md` with caps in the shape
of #38's `UntrustedFileHardeningTest`. The "inspector" in TASK-0103 is a
usability feature; it is not the hardening.

## 4. The fourth permit hardcodes an optional capability into the core type system

`Element` is `sealed permits DisplayElement, LogicElement, Wire`;
`LogicElement` is sealed over ~30 named classes. That means the catalogued
`elem.element-provider` seam (`many`, typed, "external #212") **cannot
actually be fed from outside the tree today** — the seam is nominal. Adding a
fourth permit for a domain whose own §4 invariant 2 says the primary audience
must never see it entrenches that: the core type system grows a branch per
domain, forever, for capabilities that are meant to be optional.

The change that *is* independently justified is one level down and the issue
says so itself: `Put.element` is typed `LogicElement` (`src/jls/elem/Put.java:25`)
so only reacting elements may own terminals, and `WireNet.java:507` blind-casts
to `Reacts`. That is a live coupling defect with value today — it is what
stops `DisplayElement` (sealed to `Text`, with a comment promising more) or a
breadboard jumper or a probe from owning a terminal. My concrete proposal:
**file the `Put` widening plus the `Reacts` guard as its own small issue in
#78's element-authoring arc and land it now**, not gated behind #351, not
carried by a feature that may never start. #331 already requires it to be a
standalone *commit*; making it a standalone *issue* costs nothing and banks the
correctness fix regardless of the analog program's fate.

## 5. The best idea in the issue deserves to be the interface, not an invariant

"No student-reachable path emits a matrix-singularity message" is the claim
that separates this from every hobby SPICE front end, and §4 invariant 5
enforces it by asserting the absence of text. Make it structural instead:
define the solver→editor boundary as a **`SolverDiagnostic` carrying
`ElementId` provenance** (JLS has stable ids since #165) and no matrix
coordinates at all. Then the invariant holds by construction — the API
literally cannot express a row index — the datum and partition diagnostics of
§1.2 are instances of one type rather than special cases, and the whole
diagnostic layer becomes **engine-agnostic**: it survives unchanged if the
byte-identity gate in #351 fails, if the Java port is later swapped, or if a
future maintainer revisits the external-oracle boundary. Given #351 explicitly
schedules its determinism claim for early falsification, owning an
engine-independent diagnostic contract is cheap insurance for this issue's
capstones.

## 6. Sequencing pulls against the keystone

§6 concedes this feature cannot be *finished honestly* without the editor
decomposition and a floored `jls.edit`, and grand-architecture §3 calls the
headless-kernel extraction (#77) "the highest-leverage single change in the
tracker". So the analog program's completion is gated on the keystone anyway —
which makes it the most expensive imaginable forcing function for editor
decomposition. #329's breadboard needs the same editor work, is a second
canvas over the *existing* value model, and requires no new numerical engine.
If the goal is to force the editor seam, force it with the cheap consumer.

One more tell worth naming plainly: a feature whose hardest invariant is "the
primary audience must never see any of it" is, by its own construction, built
for a secondary audience. That is a legitimate thing to build — as an
activated module, in the mechanism §2 above already provides. It is not a
reason to reshape the core hierarchy of the first-year tool.

## What I would keep exactly as written

The convergence-hardening emphasis over device breadth (§2 rejection 4) is the
single most mature judgement in the whole analog program, and the recorded
stop conditions in §7 — publish the corpus as the refusal, 5% ceiling, the
stop-before-transistors rule — are the right way to run a speculative program.
Parameter tiering asserted as exact reproduction rather than approximation
(§3) is better engineering than most shipping simulators manage. The limiting
protocol as an asserted implication rather than a convention is correct and
should be lifted verbatim into #351's architectural-rule tests. Keep all of it.

## Verdict

**endorse-with-reframing.** The end — analog that a student draws, and
failures explained in what they drew — is worth pursuing and is argued better
here than in most of the tracker. The design should be re-cut: settle the arc
conflict in the two documents that currently forbid it; drop TASK-0105 in
favour of module-scoped palette totality over the shipped
`gui.palette-contributor` seam; make the device library one `Part` type over
model cards instead of twenty-two registry rows; extract the `Put` widening as
its own issue and land it now; and make the drawn-element diagnostic an
engine-agnostic API type. What survives is a smaller feature whose expensive
half is data, whose disclosure property is structural, and whose one core
change is worth making even if no analog device is ever drawn.
