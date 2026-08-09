# Issue #490: FEAT-059: a drawn line reflects — 5.500 V on a 3.3 V rail, a flat 3.300 V when terminated, and 4.368 V when only the edge rate moves
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the machine block and the issue is one sentence: **make a student see that a
wire is not a wire, and that the regime is entered by edge rate, not clock rate.**
The 5.500 V / 3.300 V / 4.368 V triple is the evidence, not the goal. CAP-18 (#313)
states the goal even more precisely in its own title — *"a drawn **net** that is
electrically long says so, shows its reflections…"*. Hold that word: the capstone
promises a report about the net the student already drew.

The issue delivers that promise through a different object: a **new registered
element type** placed *between* two ordinary nets, plus **the first real numbers in
JLS's value surface**. That substitution is where the whole review lives.

## Judgment against the project's arc

**1. The element re-tells the capstone's sentence in the wrong grammar.** #490 correctly
refuses to make `WireNet` distributed (`src/jls/elem/WireNet.java` holds one `BitSet`
per net; equipotentiality is its definition). But the conclusion it draws — "therefore
a new drawable component" — is not the only one available. A third option was never
considered: **do not model the net at all; compute over it.** The student draws the
same net they always drew, declares its length and their driver's edge rate (#486
already owns both attributes), and JLS *reports* what that net does. Nothing is placed;
nothing is re-modelled; the capstone's sentence is delivered literally rather than by
analogy. As filed, a student who draws a 150 mm jumper still sees nothing — they have
to know to reach for a component they have never heard of and hand-enter a Z0.

**2. The real-valued trace row is the load-bearing architectural decision here, and it
is priced as an incidental (0.5–1 mw, "shared with #303/#305").** It is not incidental.
`src/jls/sim/TraceSample.java:19` is `record TraceSample(long time, BitSet value)`;
`src/jls/edit/Trace.java:51` is `record Change(BitSet value, long when)`; the VCD
profile in `docs/batch-interface.md:255-256` emits `$var wire` and nothing else, and is
byte-pinned. `docs/capability-roadmap/README.md` §6 rules continuous-time and analog out
under ground (a) — *different tool class* — and names the **digital shadow** (strength
lattice, pull-up/pull-down, open-drain; P1, i.e. #341/FEAT-027) as the in-scope answer
to exactly this family of questions. #490's own §Evidence 7 reports the SI vocabulary
gap and declares itself **UNOWNED** by any roadmap programme. That is not fatal on its
own — the roadmap's "no analog" reasoning is about *solvers*, and a closed form is not a
solver — but the roadmap's ruling protects something the solver argument does not reach:
once the trace window can render volts, every subsequent refusal in §1's out-of-scope
list (lossy, coupled, reactive, eye diagrams) becomes an arbitrary line rather than a
principled one. The permanence argument the issue makes about the save tag applies with
*more* force to the value surface, and the issue does not make it there.

**3. Everything the issue is proud of argues one step further than it goes.** §6 says
this rung is scheduled last because its surface cannot be withdrawn. §7 K18-4 says that
if palette visibility cannot be built, **"stop at the headless CSV form"** — i.e. the
issue already concedes the headless artifact carries the lesson intact. Put those two
together and the conclusion is not "schedule the permanent surface last"; it is **"do
not commit permanent surface for a teaching demo at all."** The fallback is the better
v1.

## The reframing I would build instead

**A. Ship the reflection lab as an analysis, not an element.** A headless
`jls.si`-style check (sibling to P4's planned headless `jls.timing`) reads the two
attributes #486 declares plus the drawn topology, computes the exact lattice
superposition — the same eight lines of textbook theory, the same 52-term truncation,
the same 1e-12 cross-check — and reports per net: *"IC3.CLK is 2.1× critical length for
this driver; the far end reaches 5.50 V, 166.7% of a 3.3 V rail; a 40 Ω series resistor
flattens it."* Cost: **zero new element types, zero save tags, zero palette entries,
zero format-version exposure, zero change to `Element`'s `permits` clause
(`src/jls/elem/Element.java:17-18`), zero K9 risk, and no invariant 4 fight at all** —
the registry stays at 35, the palette at 32, and `PaletteContractTest`'s three-tag
exclusion set never needs a fourth member or a visibility rule. The entire "context-derived
palette visibility" scope, its K18-4 stop condition, and integration criterion 4
evaporate rather than being satisfied. I am explicitly **disregarding integration
criteria 3 and 4 and the palette lines in the Definition of Done**: they are the cost of
a delivery vehicle I am proposing to drop, not of the lesson.

**B. Emit the waveform as a VCD `real` variable, not as a new CSV plus a new GUI row.**
IEEE 1364 §18 has `$var real` and `r<value> <id>`; GTKWave and Surfer both render real
vars as analog traces, and the README already teaches students to point those tools at
JLS output (`docs/vcd-interop.md`). This gets the picture — the *actual* deliverable, the
thing that makes the sentence mean something — into the viewer students already use,
**without inventing a second waveform format, without touching `Trace.java`, and without
putting a real number into the interactive value surface.** It also keeps the profile
extension honest and reviewable as one documented addition to `docs/batch-interface.md`
§4, rather than a fork of it. The interactive trace row then becomes what it should be:
a separately-justified feature paid for by #303/#305 when *those* capstones need it, on
their own architectural argument, not a side effect of a transmission-line demo.

**C. The threshold is the product; the numbers are its footnote.** #490 already knows
this — "the ladder stays correct and free where the structure is electrically short
(`t_flight <~ t_r/6`)… computable at elaboration." Then make *that* the artifact: every
net in the student's circuit gets a ratio, and the ones over 1.0 get the lattice
attached. Under reframing A, rungs 1 and 3 of CAP-18 collapse into one deliverable with
one story — #486 says *which* net stopped being a wire, #490 says *what that costs in
volts* — instead of a lint and an unrelated placeable component that share two attributes
and a "no third attribute" cross-check. Criterion 5 stops being a defensive assertion
between two features and becomes structurally true.

**D. If the drawable element is still wanted, gate it on demand, not on a capstone.**
ARCHITECTURE.md's "Recorded decisions" section is the right home for the ruling:
*volts are reported, never simulated; a real-valued simulation surface is out until a
named course asks for it.* That is the shape of the i18n and plugin-mechanism entries,
with a revisit trigger. The element then arrives, if ever, with a requester attached —
and by then #341's strength lattice has landed and Open Question 2's "defaulted
override" migration is a non-event rather than a two-contract obligation carried by one
side.

## What I would keep unchanged

The physics, the worked fixture, and the truncation bound are correct and should be
lifted verbatim into the analysis. The two-independent-assertions discipline (golden
**and** analytic, never regenerate the golden to fix the analytic) is exactly right and
is the most valuable paragraph in the issue — keep it, along with the unusually honest
note that 1e-12 is a round-trip bound and not a physics bound. The lumped-ladder
rejection is measured, not preferred, and is the best-evidenced decision in the document.
The edge-rate criterion (166.7% → 132.3% → <105%) survives reframing A untouched and is
still the single test that carries the lesson.

## Verdict

**endorse-with-reframing.** The goal is right, well-evidenced, and genuinely serves the
capstone and CAP-04's breadboard students. The delivery vehicle is not: it commits the
project's first real-valued value surface and its first new element type in years to
teach a lesson that a headless analysis plus a VCD real var teaches identically, more
cheaply, more discoverably (it fires on the net the student *already drew*), and with
nothing that cannot be withdrawn. Fund the lesson; drop the component.
