# Issue #398: TASK-0078: a clock stops being an ordinary wire — a Clocked capability, declared domains, and every unsynchronised crossing reported by name
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The claim underneath #398 is correct and the project has already ratified it twice:
`docs/capability-roadmap/lf-08-clocks-and-cdc.md:11` — *"JLS has a clock element. It does
not have a clock"* — and #327's criteria 4-6. Everything downstream of "a wire is a clock"
is currently blocked on that absence: P4's STA computes a meaningless number over two
clocks, P5's ERC lists *"clock nets on data pins"* with nothing to check against
(lf-08:122-125), P3's port roles have no consumer, and `HdlModel.java:414` files the
sentence *"a literal clock never ticks"* under commentary. Naming the clock is the
keystone. I endorse the direction without reservation.

What I do not endorse is the shape. Four things below; the first two I would act on before
a line is written, and I am explicitly disregarding two of §14's criteria to do it.

## 1. The seam is cut in the wrong place, and the cut inverts the roadmap's own ordering

#398 bundles six things: the `Clocked` capability, the edge-detector consolidation,
`Clock.phase`, the whole of `jls.timing` (inference + report + crossing check), IR domain
carriage, and a CLI surface. lf-08 prices those as C1 (2-3 wk) + C2 (3-4) + C3 (3-4) =
8-11 weeks. #327 §2 prices this same issue at **2 weeks** and its own Open Question 1
admits the band exceeds the row sum by 3.7-5.1x. A `tier: task` label on 8-11 weeks of
roadmap work is not a scheduling detail — it is the reason the dependency graph came out
wrong.

The first two pieces are gated on **nothing**. `Clocked` + name-based `clockPin()` needs no
net partition and no reset pin; `Clock.phase` needs neither. Only inference and the
crossing check need #336's partition, and only the cross-domain reset rule needs
TASK-0077's `CLR`/`PRE`. By bundling, #398 makes a zero-dependency refactor wait on two
prerequisites it does not need — and, worse, **inverts the recorded ordering**. lf-08:432
says: *"C1 should precede P2's register control pins … doing that after the `Clocked`
interface exists is one edit, doing it before is two."* #398 declares TASK-0077 (exactly
those pins) as its unfiled prerequisite. The roadmap says C1 → reset pins; the issue says
reset pins → C1. Both directions cannot be right, and the roadmap's is the cheaper one.

**Concrete alternative: split at C1.**

- **TASK-0078a — "the clock has a name."** `jls.elem.Clocked` (name-based `clockPin()`,
  `activeEdge()`), the four private edge detectors deleted, `Clock.phase`, and
  `HdlExporter`'s `ins.get(1)` replaced by `clockPin()`. `blocked_by: []`. Ships now, in
  parallel with #336. Carries P8, P9, P10 and H1, H2, H5 unchanged — the whole
  byte-identity argument lives here and nowhere else, so a moved golden is diagnosed
  against a 300-line diff instead of a 3,000-line one.
- **TASK-0078b — "the clock has a domain."** `jls.timing`, the report, the crossing check,
  IR carriage, the batch surface. `blocked_by: [336, 0078a]`, picks up the reset rule when
  TASK-0077 lands. Carries P2-P7.

TASK-0077 then lands *between* them and is written against `Clocked` from the start — the
one edit lf-08 asked for. This is not a process quibble: 0078a is the piece with a
byte-identity gate and no unresolved design question, and #398 as written cannot ship it
until five open questions and two prerequisites resolve.

## 2. The FORMAT decision contradicts the project's own normative policy, and reversing it
   deletes a blocking open question

O7 asserts "no FORMAT bump, and that is the hazard," quotes `docs/file-format.md` §9's
*no-bump-needed* bullet (`:429-435`), then spends §7.12, §11 and Open Question 2 building
scaffolding around the resulting silent-corruption case — routing the protection through
#314 and #319 and marking it **blocks execution**.

§9 does not end where O7 stops reading. Twenty lines later (`docs/file-format.md:466-469`):

> *"Writers SHOULD therefore prefer a version bump over an 'ignorable' attribute whenever
> dropping the attribute would change simulation behavior."*

Dropping `phase` changes simulation behavior — that is precisely what O7 argues. The
project's normative spec already answers the question, in the direction lf-08:144 assumed
(*"one FORMAT bump, byte-identical for every existing file"*). The mechanism already exists
and already has this exact precedent: FORMAT 2 is written **only for files that contain a
vertical group** (`file-format.md:454-457`). Apply it verbatim — emit `FORMAT 3` only when
some `Clock` carries a non-zero phase — and every existing file stays byte-identical, every
phase-0 file stays FORMAT-1-loadable, and a phase-shifted file meets `NEWER_FORMAT` in an
older post-versioning reader instead of quietly simulating a different circuit.

The knock-on is large: §7.12's first bullet, §11's fourth threat, the `docs/file-format.md`
hazard note in §8, and **Open Question 2 in its entirety** all disappear, along with the
issue's coupling to #314 and #319. One of the two "blocks execution" questions is not a
question; it is a spec the issue misread. **I am disregarding the §14 criterion that
requires the silent-drop hazard be recorded in `docs/file-format.md`**, because the right
outcome is that there is no hazard to record.

## 3. The domain model is missing a root, and the only realistic design in the tree has none

§7.10 defines roots as *"`Clock` outputs and top-level `InputPin`s carrying a declared
clock role."* That role does not exist:

```
$ grep -rn "clock role\|PortRole\|portRole\|clockRole" src/ docs/   # nothing
```

It is P3's port metadata (lf-08:157), which is neither filed here nor in `blocked_by`. So
the second half of the root rule is unimplementable at pickup, and the consequence is not
abstract. The repository contains four `.jls` files. The only substantial sequential design
among them is `test/fixtures/riscv-sum1to10.jls`: **32 `Register` elements, 0 `Clock`
elements**, clocked from an `InputPin` — exactly the workaround lf-08:102-110 records
(`riscv/build_cpu.py:125`, *"clock is an input pin so batch -t vectors can step it
deterministically"*). Run `-cdc` on the flagship and all 32 registers classify `UNDRIVEN`.
`riscv/gui/cpu.jls` has one `Clock` and two `Register`s — a demo, not a corpus.

That collides with the issue's own §11 threat #1 (*"a CDC tool with a wrong domain model
produces confidently wrong violations"*) and with §9's acceptance figure. **There is no
corpus over which to publish a false-positive rate.** H4 is unfalsifiable as written, and
building the corpus is unbudgeted work that is *more* valuable than the checker: two-clock
fixtures, a button-crossing fixture, a gated-clock fixture, a divide-by-two, and a
single-clock first-year circuit are the artifacts that make every later claim in this area
checkable — including P4's STA and P5's ERC.

Two concrete moves. (a) Fold the minimal clock-role declaration into TASK-0078b — a
`boolean clock` attribute on `InputPin` under the same FORMAT-3-when-set precedent — or
declare P3 as a real prerequisite; do not ship a root rule half of which cannot execute.
(b) Make "no declared root anywhere" a first-class report state that refuses to run the
crossing check and says *declare one*, rather than emitting 32 `UNDRIVEN` rows. lf-08:475
already wrote the rule: *"the honest fallback is 'I cannot determine this clock's
relationship; declare it,' not a default."*

## 4. The issue ships the half that is not the leapfrog, and does not say so

lf-08's competitive section is unusually blunt about where JLS wins: inference without a
constraint file (architectural), **the crossing marked red on a drawing the user drew**
(lf-08:359-364, *"the strongest one in this area"*), and seeded non-reproducibility as a
measurable lab. #398 delivers the first, states *"No GUI surface is added here"* (§7.1),
defers the second, and does not mention the third. lf-08:486-490 wrote the kill criterion
for exactly this state: *"If the metastability half is refused or indefinitely deferred …
this is not a program — it is two ERC rules and a clock-domain map inside P5, worth about
3-4 weeks."*

I do not think that means don't do it — the structural half is genuinely parallel-safe and
is a prerequisite for four other programs. I think it means the issue is claiming the wrong
thing. As written, the deliverable is a batch lint with three exit codes, which is parity
with the one axis lf-08:342-347 explicitly says JLS cannot win on (*"JLS's advantage here
is 'small designs,' not 'better tool,' and it should not be claimed as the latter"*). Two
cheap changes fix the framing:

- **Give the clock report its own flag.** P5 requires the report *before* any violation;
  #398 makes it a preamble to `-cdc`. Ship `-clocks` as a separate mode. A student or
  instructor can then inspect the domain model with no checker involved, which is the
  actual precondition for trusting anything the checker later says — and it is a shippable,
  reviewable artifact the day TASK-0078b lands, independent of false-positive tuning.
- **Name the editor consumer, even if it is out of scope.** `jls.timing` staying a headless
  leaf is exactly right and I would not weaken it. But `DomainMap` is already the whole
  input a "color nets by domain" view needs, and O8's *"note it and leave it"* on the
  combinational palette drawer is the same reflex: every user-facing surface deferred, so
  the increment can only ever be evaluated by CI. State in §13 that the `DomainMap` is the
  GUI's future input and that the drawing-side marking is the capability's payoff, so the
  interface is not shaped for CI alone.

## 5. One design answer the issue leaves open that its own dependencies already supply

Open Question 3 (how a crossing is suppressed) is left open, but the hard part is unstated:
domain assignment is derived (§7.7, correct), while a per-crossing suppression must be
*saved* and must survive edits. Without a stable key it silently goes stale — strictly
worse than the global off switch it exists to prevent. #336 supplies the key: FEAT-004's
**stable net naming**, plus the stable ids at `Circuit.getElementsInStableOrder`
(`src/jls/Circuit.java:479`). Key a suppression on `(stable net name, source root stable
id, sink element stable id, sink pin name)` and it either matches the same crossing or
matches nothing and is reported as a stale waiver.

Better still, reframe the primary mechanism. Most real suppressions are not *"ignore this
wire"* but *"these two domains are known-asynchronous and handshaked."* That is
`set_clock_groups -asynchronous`, which lf-08:167-168 already commits JLS's vocabulary to,
and it is a **design-level declaration** — the same place Open Question 1 recommends the
domain declaration live. Make domain-group declaration the primary mechanism and per-crossing
waivers the rare escape hatch, and OQ3 mostly answers itself, OQ1 gains a second
independent reason for its recommended default, and the concept transfers to Vivado
unchanged.

## Verdict

**endorse-with-reframing.** The capability claim is right, well-grounded, and load-bearing
for four other programs. The issue's evidence discipline is excellent — O1's absence
output, P3's both-directions requirement, and the fan-out condition are all things a weaker
issue would have missed. But as filed it is one `tier: task` issue carrying 8-11 roadmap
weeks, blocked on a prerequisite the roadmap says should come *after* it, containing a
FORMAT decision its own normative spec argues against, a root rule half of which is
unimplementable today, and an acceptance number with no corpus to measure. Split at C1,
bump FORMAT for `phase`, budget the fixture corpus as the deliverable it is, and name the
drawing as the destination.

Disregarded acceptance criteria, stated plainly: (i) §14's *"the `phase` silent-drop hazard
is recorded in `docs/file-format.md`"* — bump instead, per §9's own SHOULD, and delete Open
Question 2; (ii) §14's implicit single-issue framing — the `Clocked` capability and
`Clock.phase` should land as their own unblocked issue before TASK-0077, not behind it.
