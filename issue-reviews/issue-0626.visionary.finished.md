# Issue #626: TASK-C559-4: CircuitVerse&#39;s queue-priority delay imports flagged, never presented as equivalent — the constructs that only look preserved get named
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this task is really for

The subtitle is the thesis, and it is the best sentence in the CAP-29 family: **the
dangerous loss is the one that leaves no hole.** Everything else in #559's decomposition
(#621 parse, #622 element map, #624 subcircuits, #628 integration) is machinery; #626 is
the value statement. It is also the only place in the family where JLS's actual
differentiator shows up — a normative `docs/simulation-semantics.md`, golden-pinned, with
an RV32I differential oracle recorded in ARCHITECTURE.md as the binding equivalence
criterion for any future engine. An importer that admits what it could not preserve is
that same doctrine spoken at the migration boundary. Endorsed in substance.

What I do not endorse is the shape: one attribute, hand-flagged, with a prose sentence
per element instance and a survey promised as a side artifact. That shape is a feature
where a **column** would do, and it is scoped to a task that owns neither the vocabulary
(#556) nor the table the vocabulary would live in (#622).

## Reframe 1 — fidelity is a column on #622's table, not a task of its own

#622 already commits to "a written table" mapping every CircuitVerse element to a JLS
element *by semantics*. #626 AC-3 then asks for a second written list: a survey of the
remaining constructs sharing `delay`'s failure shape. Two tables describing the same
domain, maintained by two issues, is exactly how the `mapped-with-caveat` disposition
becomes stale six months after it ships.

Make the mapping table a **machine-readable resource with a mandatory disposition cell
per row** (`realized` / `mapped-with-caveat` / `refused` / `dropped`, from #556's closed
vocabulary), plus a delta-clause id where the disposition is not `realized`. Then:

- The report is *generated* from the table, not authored per code path. #626's AC-1
  becomes "the importer emits the row's disposition", i.e. nothing to implement.
- AC-3's survey is not a deliverable anyone can forget — a row with no disposition
  fails the build, so the survey is the totality of the table.
- The bidirectional cross-check is a pattern this repo already runs twice:
  `ExtensionPointCatalogTest` (constants ↔ catalog, both directions) and
  `HelpTopicsTest` (palette ↔ topics ↔ links). A third instance costs almost nothing and
  is far stronger than AC-4's single fixture.

Under this framing #626 stops being "implement the delay caveat" and becomes "the
mapping table carries a fidelity column and the report projects it" — which is a
paragraph in #622 and a schema cell in #556, and probably should be filed as such.

## Reframe 2 — divergences are clauses in a normative document, not sentences in code

AC-1 requires "the sentence that the source semantics are queue priority rather than
propagation delay." A sentence stored in code is a sentence nobody reviews. JLS's habit
is the opposite: put the claim in a normative document, give it an anchor, and have the
code cite it (`docs/simulation-semantics.md` §6.2, `docs/file-format.md` §9,
`docs/batch-interface.md` §1 all work this way, each with a test that fails on drift).

Concretely: `docs/import-deltas/circuitverse.md`, one clause per divergence with a stable
id (`CV-DELTA-001` …), and the report entry carries `{construct, disposition, location,
delta_id}`. The grading script in AC-2 then matches on an id rather than on prose *or* on
a bare disposition — which matters, because "some non-equivalent translation happened" is
much less useful to an autograder than "queue-priority delay was reinterpreted here."

The same registry is immediately reusable in three places that today carry their
divergence in free text:

- `HdlExporter`'s warn-and-skip / reject buckets, whose machine-readable half is
  `record Result(String text, List<String> warnings)` — untyped prose;
- `VhdlEmitter`'s three-site two-state disclaimer, already indicted as duplication in
  `docs/capability-roadmap/keystone-b-migration.md` §1;
- the README/`docs/file-format.md` §9 caveat that JLS 4.1 **silently drops** run-length
  memory initial contents — JLS's own format, its own canonical silent loss, currently
  machine-readable nowhere.

That is the "greater alignment" answer: the class #626 names is not a CircuitVerse class.
It is every boundary JLS has.

## Reframe 3 — the mapping decision the task assumes but never argues

The task takes as given that the value should be carried across and then apologized for.
That is one of three options, and it is not obviously the best:

- **(a) map value → JLS delay, flag it.** The issue's choice.
- **(b) drop the value, use JLS's per-element defaults, report `dropped`.** Requires no
  vocabulary extension, keeps #556 AC-2's totality equality (`C_src \ C_out = R`) intact
  — note that (a) puts a construct in *both* `C_out` and `R`, which the inherited
  contract does not currently permit — and fabricates no quantity.
- **(c) map the *order*, not the magnitude:** normalize CV's priorities to a rank and
  scale ranks onto JLS delays. Relative ordering is the one thing a priority number
  actually means; absolute time is the one thing it does not. Rank preservation is
  testable in a way "delay 10 means delay 10" never is.

The concrete hazard favouring (b)/(c): JLS delays are deliberately non-uniform and
calibrated per element (`docs/simulation-semantics.md` §7 — AND 10, NAND 5, Mux 25,
Register 50, Memory 100, Adder 30×bits), while CircuitVerse's attribute is a mostly
uniform small integer. Copying it across does not merely change semantics abstractly; it
**overwrites JLS's calibrated timing** — a register that responded in 50 now responds in
10 while the memory beside it still takes 100. Sequential circuits that worked upstream
can fail here for reasons no report sentence explains. Whichever option is chosen, the
report should name the escape hatch JLS already ships: **Global → Reset Propagation
Delays** (`LogicElement.resetPropDelay`, `Circuit.java:1728`) restores every default in
one action. That single sentence is worth more to a migrating instructor than the
etymology of the source attribute.

## Reframe 4 — divergence is conditional, and JLS can compute the condition

AC-1 fires on *every* imported `delay` value. On a project of any size that is hundreds
of identical entries, and #323 §3's own warning applies: a construct reported but
actually realized is a report that trains instructors to ignore it.

The condition is computable. JLS's own model already resolves same-instant ordering:
wires are ideal and zero-time, receiving elements read latest values, and same-time events
fire in posting order via `SimEvent`'s `(time, seq)` comparator with stable-id seeding
(#181, `Simulator.java:190-194`). So JLS and CircuitVerse are not "time" versus
"priority" — both are event simulators that must break ties, differing in what the number
means. For a feed-forward, single-driver, combinational region, a monotone priority and a
transport delay induce the same firing order and the same settled truth table; the
divergence class is glitches, races, feedback, and multi-driver nets.

So emit **one clause-level entry per circuit**, listing the affected elements, and gate
its severity on structure: does the imported netlist contain combinational feedback,
reconvergent paths with differing delays, or multiply-driven nets? That analysis is
reusable far beyond import — a race/ordering-sensitivity check over any circuit is a
pedagogy feature JLS lacks natively, and it would give the honesty doctrine teeth for
students who never touch CircuitVerse.

## Where this pulls against the arc

Only in ownership. #559's dedup comment binds the feature to "must not fork,
re-implement or vary" #556's schema — yet AC-2 here needs a disposition (`mapped-with-caveat`)
that #556's totality equality does not currently admit, and AC-3 needs a survey artifact
that #622 is already producing in another shape. As written, the task's two hardest
obligations are both edits to *other* issues. That is not fatal, but it means #626
cannot start until #556 gains the third disposition and #622 gains the fidelity column —
a stronger ordering constraint than the `ordering_after` line records.

## What I am disregarding, and why

- **AC-1's per-value granularity.** True statement, wrong altitude; it manufactures the
  ignorable report the whole doctrine exists to prevent. Per-circuit, clause-cited, with
  the element list attached, is the same honesty at a granularity that survives contact
  with a real project.
- **AC-3 as a separate written list.** It should be the disposition column of #622's
  table, enforced by totality, not a prose survey with no test.
- **AC-4's fixture as specified** ("a version that imports silently fails the test") —
  correct instinct, but a mutation-style negative test on one fixture is weaker than a
  bidirectional table↔report cross-check that cannot be satisfied by a single golden.
- **The boundary note's premise that mapping is a given.** The option set above should be
  decided explicitly and recorded; "imported, but flagged" is a conclusion, not a
  requirement.

**Verdict: endorse-with-reframing.** Keep the thesis — it is the most valuable sentence
in CAP-29 and it should outlive this task by becoming a project-wide fidelity ledger with
stable delta-clause ids covering import *and* export *and* JLS's own format caveats.
Recast the implementation as a mandatory disposition column on #622's mapping table plus
a per-circuit, structurally-gated divergence clause; settle the map/drop/rank question
before the mapping table freezes; and surface Global → Reset Propagation Delays as the
remedy the report hands the instructor.
