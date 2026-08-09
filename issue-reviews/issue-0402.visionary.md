# Issue #402: TASK-0099: controlled sources, time-varying waveforms and a small model-card grammar turn the bare analog solver into something a teaching lab can build circuits with
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Stripped of the stamps and the breakpoints, #402 claims: *JLS should become a tool where a
student can express an interesting analog circuit.* That claim is worth having. What follows
disputes the route, the seam, and one premise — not the ambition.

## 1. The enabling ruling is not in the tree, and the tree says the opposite

The issue rests its entire right to exist on **D8** in `docs/plan/evidence/BRIEF.md`
("scope the models, not the solver"). That path does not exist in this checkout — there is no
`docs/plan/` at all, and no `BRIEF.md` anywhere. The issue says so honestly, and #351 says the
same of `analog-determination.md`.

What *is* in the tree is a large, code-anchored, deliberately re-derived roadmap that rules the
other way, three independent times:

- `docs/capability-roadmap/README.md` §6(a): "**Continuous-time and analog** … Supporting these
  means being a SPICE-class solver — a different tool, not a deeper digital model."
- `docs/capability-roadmap/sweep-06-physical-boundary.md:83`: "No continuous-time solver, **and
  none should be added**", and `:553-556`: "Adding one is building SPICE, which is ground (a)."
- `docs/capability-roadmap/sweep-03-elements-and-hdl.md:633`: same exclusion for VHDL-AMS /
  Verilog-AMS / Verilog-A.

So the project currently carries **two mutually contradicting normative roadmaps**, and the one
that authorises this work is the one nobody can read. That is not a documentation nit under this
lens; it is the difference between "JLS is a schematic-first digital teaching simulator that
delegates" and "JLS is a mixed-signal tool". The repository has a shipped form for exactly this
kind of reversal: ARCHITECTURE.md "Recorded decisions", each with a rationale and a **revisit
trigger** (see the #221 simulation-execution-strategy entry, lines 341-368). §6's exclusion has
no revisit trigger written for it; supplying one and firing it is the actual prerequisite here.

**Concrete demand, ahead of any code:** land the amendment that reverses §6(a) in
`docs/capability-roadmap/` and records the decision in ARCHITECTURE.md, citing the evidence
BRIEF.md is supposed to hold. Until then TASK-0099 is a task whose parent programme the repo's
own normative documents decline.

## 2. The cheaper route to most of the stated goal: P1, not MNA

The issue's audience argument is "without dependent sources and time-varying stimuli … nothing
interesting is expressible." Test that against what the roadmap says students actually cannot do
today (`README.md` §P1, "Pedagogical capabilities unlocked"):

- bus contention resolves by **drawing order** and a student learns a falsehood;
- a floating input is silently zero across 27 sites in 17 element classes;
- open-drain / wired-AND / pull-ups — **the whole I²C lab** — are inexpressible;
- don't-know vs don't-care are collapsed, so K-map don't-cares are not real end to end.

Every one of those *feels* analog to a student, and none needs a solver. P1 delivers them via a
strength-carrying `LogicValue` and a real resolution fold, and it unlocks 28 standards rows,
sits on the critical path of P2/P4/P6, and is the named revisit trigger for #67 EVCD and the
IEEE-91 output qualifiers. FEAT-046 alone is banded at **17.5–26 maintainer-weeks** and unlocks
nothing else in the tree; #402 is a slice of that with an unfiled hard prerequisite.

I am not arguing analog is worthless. I am arguing that "a teaching lab can build circuits with
it" is a claim about *marginal pedagogy per week*, and on that measure the analog programme is
the most expensive route to a large fraction of its own stated benefit. The visionary reading:
**P1 first, and let the analog programme's real justification narrow to what P1 provably cannot
give** — RC time constants, filters, the photodiode front end, the reconstruction filter. That
is a smaller, honest, defensible claim, and it survives the amendment argument in §1 far better.

## 3. This task is cut at the wrong seam

#351 §2 states the correct cutting rule and then #402 violates it: *"Cards are a parsing-and-data
problem with a dialect-compatibility surface; the solver is numerics. They fail differently and
are tested differently."*

By that rule, #402 contains **two tasks with nothing in common**:

| Piece | Failure class | Where it belongs |
|---|---|---|
| `Devices` — E/F/G/H stamps, `POLY(n)` Jacobian, S/W hysteresis | numerics; determinism of accumulation order; matrix entries | TASK-0097 |
| `Waveforms` — six functions + breakpoint sets | the timestep controller's own accept/reject contract | TASK-0097 |
| `DcSweep` — warm starts over the escape ladder | a driver loop over TASK-0097's Newton + ladder | TASK-0097 |
| `SpiceNumber` + `CardReader` + normative suffix table | hostile text parsing, dialect surface | **a task with no prerequisites** |

The issue argues *itself* into this: §7.11 already forbids duplicating the convergence strategy;
§7.6 says stamps and breakpoints "read structures only TASK-0097 creates"; and H2's own framing —
"a waveform that reports no breakpoints makes the accept/reject rule meaningless" — is an argument
that the breakpoint set is **part of the controller's contract**, not a separable deliverable.
Splitting the stamps from the solver they stamp into buys nothing and costs a real dependency.

Re-cut, the text half becomes the one piece of the entire analog programme that is **buildable and
falsifiable today, with no solver in existence**. H3 (is a small grammar enough?) and H4 (one
parser, one diagnostic?) can be refuted months before week forty, on the same "falsify early"
logic #351 §6 uses to insist TASK-0098 is not scheduled late. That is a strictly better plan
than the one written.

## 4. `SpiceNumber` should not live in `jls.analog`

The issue names three surfaces (dialogs, `.jls` loader, card reader) and calls a fourth
implementation a defect guarded by `git grep -c 'MEG'`. But the pressure for engineering notation
is **already present and pre-analog**: `jls.NumericField` / `TextFilter` parse the interactive
time-scale, step and time-limit fields; `SigSim` has its own token/radix parser; per-element
propagation delays are bare integers (`docs/simulation-semantics.md` §6-7); the batch `-t` grammar
is a documented stability contract. A student typing `10n` into a delay field is the *same* user
error as `10n` in a resistor field.

Put the parser in `jls.core` (headless, AWT-free, already the home of `Geometry`/`Bounds`) as an
engineering-notation-and-units reader, make the suffix table normative once, and the "fourth
implementation" the issue fears becomes structurally impossible rather than grep-guarded. Scoping
it to `jls.analog` guarantees the divergence it was created to prevent. This is the one piece of
#402 I would ship regardless of what happens to the analog programme.

## 5. The `.model` card premise runs against the shipped precedent

#402 cites #349 for "a library is data, extensible with a text file and no Java." The shipped
example goes the other way: `src/jls/hdl/board/Boards.java` — *"Kept deliberately tiny (hypothesis
H2 of #213): a board is one `Board` value here, with its pin map transcribed from the vendor
documentation, and the table grows on demand **rather than through a general board-description
format**."* That is the same problem shape — vendor data, teaching scale, dialect tail — solved by
declining the grammar.

The out-of-the-box alternative: **ship zero card grammar.** A Java device-parameter table (the
#351 Open Question 3 answer is 14-15 parameters, not 88) plus the shared suffix parser expresses
the teaching set today. The first instructor who arrives with a real vendor card that the table
cannot express is *evidence*, and #402's own §10 already prescribes what to do with it. This
inverts the burden of H3: instead of defending a "deliberately small" grammar card-by-card forever
against exactly the drift §11 names as "the scope creep risk in this whole programme", you defend
nothing and let refusals accumulate as data. Note also that `CardReader` is a new hostile-input
surface (SECURITY.md, `UntrustedFileHardeningTest`) acquired for a user who has not yet appeared.

## 6. "Zero format version cost" optimises the wrong quantity

O4/P7 treat a `FORMAT` bump as a cost to be avoided. `docs/file-format.md` §9 says the opposite for
exactly this case: *"Writers SHOULD prefer a version bump over an 'ignorable' attribute whenever
dropping the attribute would change simulation behavior."* Dropping `String r "4.7k"` does not
change behaviour — it **deletes the resistor**. The issue half-sees this in §7.7 and then declares
victory on cost anyway.

The reframing that dissolves the question: analog devices are **new element types**, and §9 already
rules that new element types need no bump because older readers fail *loudly* — "no element type
named X … detectable, not a misparse." The safety comes free from the type tag; the item-kind
debate (`String` vs `double`) was never load-bearing. Drop the zero-format-cost framing entirely;
it is arguing for a property the format already grants by a different mechanism.

## What I would file instead

1. **Roadmap reconciliation** — amend §6(a) and the two sweeps, record the decision with a revisit
   trigger in ARCHITECTURE.md. Blocks the whole programme, not just this task.
2. **`jls.core` engineering-notation parser** — one implementation, normative table in
   `docs/file-format.md`, wired to the existing numeric surfaces *and* ready for analog. No
   prerequisites; shippable this week; carries `1M` vs `1MEG` for the whole project.
3. **Fold `Devices`, `Waveforms` and `DcSweep` into TASK-0097** where the matrix, the ladder and
   the accept/reject rule live, with the stamp-entry and breakpoint assertions as that task's
   tests (they are already IC-3 on #351).
4. **Defer `CardReader`** behind a demand gate in the `Boards` shape: a Java parameter table now, a
   grammar when a refused card exists.
5. **Re-examine ordering against P1** before spending 17.5–26 weeks, and say plainly which
   pedagogical capabilities only the solver can deliver.
