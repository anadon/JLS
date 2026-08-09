# Issue #609: TASK-C487-1: a net carries an authored SI constraint set that cannot silently vanish — an optional versioned section, additive-only under an unrelated edit
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

One property, and it is a good one: **a manufacturable requirement must survive
the crossing from schematic to board.** Everything else here — max length, stub
length, the v1 vocabulary freeze, the section framing — is machinery in service
of that. FEAT-060's real differentiator is the external adjudicator (#487 §5.1),
and the constraint only has to be durable from the moment a student authors it
to the moment `jls -export` reads it. I endorse that end without reservation.

I do not endorse this task's route to it. The issue locates the fragility in the
**serialization slot** and spends its whole justification there
(`docs/file-format.md:220-222`, silent-ignore of unknown attribute names). That
is the wrong door. I am explicitly disregarding acceptance criteria 2 and 4, and
the `ordering_after: [486, 319]` line, for the reasons below.

## The door the issue did not lock: there is no net to attach a constraint to

`src/jls/elem/WireNet.java:22-30` is the whole field set — `ends`, `wires`,
`bits`, `hasinput`, `triState`. `WireNet` is not an `Element`, has no `sid`, has
no `save`, and never appears in the file: nets are *reconstructed* at
`finishLoad` from `WireEnd` mutual references (`docs/file-format.md` §7,
ARCHITECTURE "The save/load pipeline" step 4). So "a net carries an authored
constraint set" is not a serialization question yet. It is a modelling question
that nothing in this task's scope answers.

The precedent is already in the tree and it already fails the way this task
fears. JLS has exactly one authored per-net annotation today — the probe name —
and it lives on a **wire segment**, not a net: `Wire.probeName`
(`src/jls/elem/Wire.java:31`), serialized on both endpoints as
`probe <other-end-id> "<name>"` (`src/jls/elem/WireEnd.java:613`). A `WireNet`
holds a `Set<Wire>`, so one net can carry several probe names, and splitting a
segment gives the annotation to one half arbitrarily. Now put a max-length there
instead. The section is intact, byte-identical, must-understand flag set,
criterion 3's additive-only diff green — and the constraint is silently attached
to a different net than the author meant. **That is the silent-vanish this issue
exists to stop, arriving through the door it left open.**

Which is why `ordering_after: [486, 319]` is the most consequential line in the
task. The parent (#487) lists `blocked_by: [486, 336, 366, 318, 319]` and says
why #336 is there: *"a name that does not survive save, load and export names
nothing."* This task drops #336. Its ordering is a strict weakening of its
parent's, and the dependency it drops is the only one that makes criterion 3
mean what it says.

## Reframing 1: cut at the net record, not at the version mechanism

The seam worth cutting is **"a net becomes a saved thing with a stable
identity"** — #336's territory, plus the `sid` machinery that already ships
(`docs/file-format.md` §8, issue #165) and the canonical-order rule (#166) that
makes save a pure function of content.

Land that, and this task mostly evaporates:

- A constraint is one more attribute on a record that already exists. No new
  carrier, no new vocabulary home.
- Criterion 3 ("additive-only diff after inserting one unrelated gate") stops
  being a bespoke test and becomes a *consequence* of canonical order — the
  property #166 already asserts for every other datum in the file.
- The same record is what #486 needs for declared length and what #487's fourth
  scope needs for back-annotated routed length. Three consumers, one record,
  built once. As scoped, #609 builds a per-net data carrier that is not a net,
  and #486 builds a second one beside it.

## Reframing 2: most of the v1 vocabulary is derivable, and derived data cannot vanish

FEAT-058 computes `l_crit = v·t_r/k` (#486 §3). **That is a maximum length.** The
constraint CAP-18's demo actually exercises is the lint's own number.

If v1 exports `l_crit` as the DRC rule rather than storing a parallel authored
copy, then: criterion 1 is trivially satisfied because nothing is stored;
criterion 3 is vacuous because there is nothing to lose; criterion 2's whole
must-understand argument dissolves, because *you cannot silently drop a number
you recompute*. The pedagogy also sharpens — the rule the board tool enforces is
the same rule `jls -check` printed, so the student sees one piece of physics
rather than two numbers that can disagree.

The genuinely authored residue is small and worth naming precisely: a strictness
or margin override (`k`, already a declared parameter in #486), and stub length,
which is not derivable. Making *that* durable is a much smaller thing than
making a vocabulary durable. I would write v1 as "export the lint's threshold;
author only deviations from it."

## Reframing 3: refuse-by-name already exists, twice, and neither costs #319

AC 2 asks for a reader that "refuses by name rather than dropping silently."
`docs/file-format.md` already specifies two mechanisms with exactly that
behaviour, and the issue's evidence quote stops one section short of both:

- **§3:** *"A reader encountering anything else where an item kind is expected
  MUST fail the load: unknown item kinds are a format extension and require a
  version bump (§9)."*
- **§9:** *"adding a new element type — older readers fail loudly with 'no
  element type named X', which is detectable, not a misparse"* — **no bump
  required**, and §7 requires the diagnostic to *name the tag*. `LoadError`
  already has the `UNKNOWN_ELEMENT` category for it (ARCHITECTURE, "Error-
  reporting contracts").

And §9 closes with the spec's own prescription for precisely this case, written
long before CAP-18: *"Writers SHOULD prefer a version bump over an 'ignorable'
attribute whenever dropping the attribute would change simulation behavior."*

So a saved `NetConstraints` tag, or a new item kind plus `FORMAT 3`, buys
refuse-by-name **today**, with mechanism that ships and is tested
(`FormatHeaderTest`, `SaveTagsTest`, `FileFormatSpecTest`). #487's invariant 8
("no new element type and no new palette entry") does not block this: those are
different things — a palette entry is step 12 of ARCHITECTURE's sixteen, save is
step 5, and `TestGen` is standing precedent for a tag with no palette presence.

The honest cost is granularity: whole-file refusal means a student who authors a
constraint cannot open the file in the lab's older build at all. Real, and worth
weighing. But note that per-section must-understand does not fix that for any
reader that exists — a section frame is itself an unknown item kind, so every
build shipping today refuses either design identically. The finer granularity
only helps builds strictly newer than an unlanded mechanism.

## The dependency this task takes on is not merely unbuilt — it is undecided

#319's three tasks (TASK-0033/0034/0071) are all "not filed." #319 is itself
`blocked_by: [334]`. Its Open Question 1 — frame inside the text grammar, or a
multi-member container — is marked **"Blocks filing children."** So the *shape*
of the section #609 must write into does not exist as a decision. Starting #609
means specifying FEAT-013's frame by consuming it, which #319 reserves for
itself ("this feature provides the frame and one worked example" — the raw bulk
image, not this).

Meanwhile #319 records maintainer ruling D15 verbatim: the guest image is a
**sidecar whose digest the circuit records**, and what protects it is "#314
FEAT-002's fail-loud loader, not this feature's must-understand policy." That is
this repository's own answer to "how does a sidecar stay in step with the thing
it describes" — the question #487 uses to demote the sidecar to fallback. A
constraint sidecar with a recorded digest gets refuse-rather-than-drop with no
format mechanism, no #319 gate, and no bytes added to the save of every user who
never authors a constraint. It also matches the shipped precedent #487 itself
cites: `src/jls/hdl/board/PcfEmitter.java` carries FPGA pin constraints as data
*beside* the design, not inside `.jls`. The section/sidecar ranking is inverted
relative to a ruling this project already made on a structurally identical
question.

## AC 4 inverts a strategic dependency

AC 4 freezes the v1 vocabulary because "each additional kind is a keyword whose
external-parser acceptance must be re-verified on every version bump." That
makes the JLS save format — which `docs/file-format.md` declares a
third-party-implementable normative spec with *frozen* tags — a projection of
one external tool's DRC keyword list. Wrong seam. JLS should author intent in
its own vocabulary and units (per #367's time base), and the **emitter** — the
second scope of #487, which already owns a parser-acceptance golden — should own
the mapping and refuse to emit what it cannot map. Then adding `skew` is not a
format-freeze event at all.

## What I would build instead, in order

1. The saved net record with stable identity (#336's core), with the probe name
   migrated onto it — that fixes a live per-segment defect as a side effect.
2. #486's declared length and edge rate as attributes of that record.
3. Export `l_crit` as the v1 DRC rule; author only the margin override and stub
   length, carried either as a new save tag (refuse-by-name today, no #319) or a
   digest-recorded sidecar (D15's precedent, no format change at all).
4. Revisit a per-section frame only if a *second* consumer needs skip-and-
   preserve — which is #319's job to justify, not this task's to presuppose.

The end is right. The route is aimed at the format when it should be aimed at
the model, and it mortgages a small, demonstrable capability against the largest
unlanded, undecided mechanism in the corpus.
