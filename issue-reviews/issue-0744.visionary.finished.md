# Issue #744: TASK-C575-1: the lab format is defined and the first combinational labs ship — starter circuit, exercise prose, grading vectors, and a CI lane that grades the reference green and a planted defect red
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

The title says "two labs." The body says "the shape of a lab is settled by
building two of them." Those are different deliverables, and the second is the
load-bearing one. #744 is the **format-defining pilot** for everything CAP-33
(#517) ever ships: #575 needs ≥8 labs conforming to it, #578 will write a
validator against it, #577 wants a real CSE 260M corpus to land in it, and #502
will deliver it onto four platforms. Two `.jls` files are ~1 mw of value; a lab
contract that eight labs and an unknown number of third-party kits are authored
against is the highest-leverage 1.5 mw in the CAP-33 subtree.

That reading is what the rest of this review optimizes for, and it is why the
issue's own boundary paragraph ("Content, not plumbing") is the sentence I most
want to reject. #744's AC-1 ("a lab directory layout is specified in tree") and
AC-2 (a CI lane that discriminates right from wrong) are *both* convention and
enforcement — the two things #578's adjudication comment on #575 assigned to
#578. The boundary as drawn is not stable, and pretending it is will produce a
layout invented by content authors in passing that #578 must then either bless
or break.

## The reframing: the pilot's two samples are drawn from the same stratum

This is the single change I would make if I could make only one.

A two-sample pilot settles a format only if the two samples stress it
differently. Two combinational labs from adjacent chapters of one text are
near-duplicates: same clockless circuit, same "drive inputs, read output pins,"
same time budget shape. Every format question that actually has open answers
lives on the sequential side and is untested by this pilot:

- how a lab declares a clock and its period, and whether the starter circuit
  ships one;
- how many cycles to run, and where the `-d` time limit lives (lab metadata? the
  vector file? the CI lane?);
- reset convention — does the student's circuit have to accept a named reset pin
  for the grader to establish a known state?
- whether `Register` and `Memory` are watched surfaces or only `OutputPin`
  (`docs/batch-interface.md` §3.2's three-type whitelist makes this a real
  choice with real consequences);
- how an expected *trace* is expressed at all, versus an expected final value.

#575 AC-1 spans combinational → sequential → FSM → small datapath. If the pilot
is two combinational labs, the format is revised at lab 3, and #578's validator
gets written against a schema that is already known to be provisional.

**Concretely: ship one combinational lab and one sequential lab (a small FSM or
a shift register / counter).** Same band, same two artifacts, dramatically more
information per unit of work. I am explicitly disregarding the issue's title and
AC-1's "two combinational labs" here — the stated criterion optimizes for
chapter-order tidiness and against the outcome the issue itself names.

## The graded surface: stdout cannot carry a vector sweep

A design constraint the issue does not appear to have priced in.
`JLSStart.java:257-261` runs the simulation once, then calls `displayOutcome()`
and `displayResults(circ,"")` **once, after the run**. The watched-element report
is an end-of-run snapshot: one line per watched pin, carrying the final value.
So a `-t` file that sweeps 64 input combinations produces exactly one graded
observation — the 64th. `examples/autograde/autograde.py`'s
`EXPECTED_STDOUT_LINES` is not a lazy example; it is the shape stdout forces.

The per-step record exists, and it is the VCD (`docs/batch-interface.md` §4,
deterministic and byte-golden-tested). **Make the VCD the graded surface for
labs, not stdout.** That one decision buys:

- Real vector sweeps. Both pilot labs are small enough for **exhaustive**
  grading today, with no new engine: a 4-bit-in decoder is 16 points, a 9-bit
  ALU slice is 512. Generate the `-t` sweep, diff the VCD restricted to the
  lab's declared output pins. "Did the instructor pick good vectors?" simply
  stops being a question for the entire combinational chapter range.
- A reference-circuit oracle instead of a hand-written expectations file. The
  lab ships `reference.jls`; the grader runs reference and submission on the
  same stimulus and compares traces. That halves the authoring cost per lab and
  deletes the failure mode where prose, circuit, and expected values disagree —
  which is the defect class #575 AC-4's non-author review would otherwise have
  to catch by hand, eight times.
- A clean growth path that #300/#369 and the `jls.formal` capability
  (`docs/capability-roadmap/lf-04-formal-and-grading.md`) slot into without a
  format break: exhaustive trace diff → sampled + mutation score → equivalence
  proof. Same lab file, better engine underneath.

## Replace the planted defect with a mutation score

AC-2's "a deliberately planted-defect variant is red" proves exactly one thing:
the vectors are not vacuous. It does not tell an instructor whether to trust the
grader with 300 students' grades, and the planted variant is a static artifact
that will rot the first time the reference is edited.

This project already has the better idea in the tree:
`docs/mutation-testing-trial-2026-07.md` and `.github/workflows/mutation.yml`
adopted PIT for the Java suite. Apply the same discipline to circuits. Define a
small **circuit mutation operator set** — swap AND↔OR, invert an output, delete a
wire, stuck-at-0/1 a pin, off-by-one a constant — mechanically generate mutants
of `reference.jls`, and have the CI lane report the fraction the lab's grading
vectors kill. Then AC-2 becomes a *measurement* with a threshold rather than a
demo, it generalizes free to all eight labs of #575 and to every third-party kit
#578 validates, and survivors are actionable feedback to the lab author ("your
vectors never exercise the carry-in"). Under exhaustive combinational grading the
score is trivially 100% and the lane is nearly free; on the sequential lab it is
genuinely informative. Keeping one hand-planted defect as a smoke test alongside
it costs nothing — but it should not be the criterion.

## Make the layout a manifest, not a directory convention

AC-3 (chapter mapping, time budget) and AC-5 (provenance statement) are prose
obligations as written, and prose obligations are not enforceable. Ship a
machine-readable `lab.toml`/`lab.json` per lab carrying `chapter`,
`time_budget_minutes`, `inputs`/`outputs` with widths, `stimulus`, `reference`,
`license` (SPDX), and `provenance`. #578's validator then has a schema to check
instead of a convention to re-derive from two examples, and #575 AC-4's
completion review has a field to write its verdict into.

Also: pick the **content license now**, in that manifest. The repository is
GPL-3.0-or-later; that is a poor fit for exercise prose, and #578 AC-4 requires a
distinct content license. Whatever #744 does becomes the precedent for every lab
that follows. CC-BY-SA-4.0 with the provenance statement inline is the obvious
call and costs one field.

## Ordering: drop `ordering_after: [300]`

#300 is a 12–20 mw capstone composing eight features (#317, #334, #337, #340,
#353, #354, #357, #369). #744 is 1–1.5 mw. Sequencing the cheapest, highest-signal
artifact in CAP-33 behind that stack inverts the project's own stated priority:
#509 — updated 2026-08-08, a named instructor, the only external demand signal in
the tracker — says the adoption conversation is "Item 0 in the priority queue,"
and #517's KC-33-1 already licenses shipping against today's three-exit-status
contract. Two labs that grade green/red on today's `-t` + VCD are something you
can send Dr. Siever this month; the same two labs behind #300 are a 2027 artifact.
The `-t` grammar and the VCD profile are both documented stability contracts, so
building on them is not technical debt — it is building on the promise.

Related: consider shaping the *second* pilot lab after a real CSE 260M
competency (original prose and circuits — you copy nothing, you target the same
skill) rather than a second Donzellini chapter. A format settled against a
hypothetical and then met with a real corpus (#577) usually needs a v2; a format
that already survived one real course's shape usually does not.

## Alignment with the larger arc

This pulls with the project, not against it. `docs/grand-architecture.md` names
the headless core (#77) as the keystone; a CI lane that loads, simulates and
grades real circuits is precisely a headless-core consumer and will apply useful
pressure to that boundary. The one place to be careful is the "single
self-contained jar" constraint: lab content belongs in-tree and as a release
asset, **not inside the jar**. ARCHITECTURE.md's help-delivery decision is the
cautionary precedent — content welded to the binary cannot be versioned on the
academic calendar, and a course kit must be pinnable per semester independently
of the simulator release.

One hazard worth naming, since these are the *first* labs: lf-04 documents that
`TruthTable` destroys don't-care outputs (`TruthTable.java:1446-1449`), so a
reference specified with don't-cares marks wrong exactly the student who took the
Karnaugh-map lecture seriously. Combinational labs — decoders, BCD-to-seven-
segment, priority logic — are where don't-cares live. Either avoid don't-care
specifications in the pilot labs, or record the limitation in the manifest's
provenance/notes field so lab 3 does not walk into it blind.

## Endorsement

Endorsed as to purpose and priority; reshape the execution: one combinational +
one sequential lab, a VCD-trace/reference-circuit oracle instead of hand-written
stdout expectations, a mutation score instead of a single planted defect, a
machine-readable manifest with the content license decided, and no ordering
dependency on #300.
