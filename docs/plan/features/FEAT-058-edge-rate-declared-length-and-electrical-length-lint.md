# FEAT-058 - Edge rate, declared physical length and the electrical-length lint

**Status:** proposed | **Cost:** 3-6 mw | **Owner program:** P4 |
**Spine rank:** -

## Capability delivered

A design can state the two physical facts that decide whether a drawn net is
still a wire - how fast a driver's output changes, and how long the net actually
is - and a design check reads both and says whether the lumped model it is
simulated under is still valid. `jls -check design.jls` prints, per net that
declared both, the declared length, the declared edge rate, the critical length
it computed and the ratio between them. A net missing either declared fact
reports **"not assessable"**, never "PASS", so the check is honest about the
designs it cannot judge - which at HEAD is all of them. Nothing is drawn,
nothing is simulated differently, and no element type, palette entry, format
version of its own, GUI surface or solver is added.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-18 | required | the two declared physical facts that put a design in this regime, and the verdict that names it; nothing else in that capstone is assessable without them, and this is its whole demo slice |
| CAP-04 | beneficial | a 150 mm breadboard jumper is 2.1x critical length for a 74AC part and 0.24x for a 74LS part - the mechanical answer to "it works in the simulator and fails on the breadboard" |
| CAP-05 | beneficial | the same verdict applies to a routed trace once its length is back-annotated from the board |
| CAP-07 | beneficial | timing handed to a fabrication flow carries transition times as well as delays; the edge-rate half is owed to that path independently |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-047 | The lint multiplies a time by a velocity. A 50 ps edge and a 1.446e8 m/s propagation velocity are meaningless against a dimensionless tick, and `docs/simulation-semantics.md:26` at `2d0ca9d` is normative that simulation time has no unit. The declared length also rides FEAT-047's FORMAT bump rather than minting one |
| FEAT-004 | The verdict names a net. A net whose synthesized name changes on an unrelated edit produces a report whose lines cannot be compared between two runs of the same design |
| FEAT-013 | The declared attributes ride as an OPTIONAL per-section-versioned block, so an older reader opens an annotated circuit structurally with a clean diagnostic. Without it K18-5 fires and the attributes go to a sidecar instead |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| - | No task id. The registry's task space is closed at TASK-0112 and this feature was minted with CAP-18 after it closed, following the recorded FEAT-054..FEAT-057 precedent. Minting its tasks is a maintainer decision | - |

## Acceptance criteria

1. A delay model may carry a transition time (`t_r`/`t_f`) beside its delay, at
   the same arc granularity the delay uses, absent by default. Absent means
   today's behavior exactly, and every existing golden stays byte-identical.
2. A net may carry an OPTIONAL DECLARED physical length. It is **never derived
   from drawn pixel length** - see Design notes - and absent by default.
3. Over a table of (edge rate, declared length, propagation velocity) the
   verdict and the computed critical length are exact to the stated formula
   `l_crit = v * t_r / 6`, and the strictness constant is a **declared
   parameter, not a literal**, because every critical-length rule in circulation
   is that one rule at a different divisor.
4. A net missing either declared attribute reports **"not assessable"**, never
   "PASS". A family whose check is vacuous must say so.
5. Run over the shipped `examples/` corpus with default attributes, the lint
   reports "not assessable" on every circuit and no other verdict. A lint that
   fires on designs that never opted in is a lint students learn to ignore
   (K18-3).
6. A circuit carrying the declared attributes opens in a reader that predates
   them **structurally**, with a clean diagnostic naming the skipped optional
   section - not a refusal and not a silent drop. This criterion is shared with
   FEAT-013 and neither feature can assert both halves alone.
7. The report is machine-readable as well as human-readable, so a capstone
   acceptance test can assert on it rather than on a rendered sentence.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 89 | SDF timing annotation | overlaps - SDF carries transition times as well as delays, so the edge-rate half of this feature is owed to that issue independently of this capstone |
| 87 | Liberty cell library | overlaps - `docs/capability-roadmap/sweep-02-timing.md:110` states the gap verbatim: "JLS has one integer, no slew, no load, no fanout awareness". **Slew is the edge rate** |
| 93 | SDC constraints | informs - a constraint object model is the same shape as the declared-attribute set, and both want a time base |
| - | the declared length attribute | **no issue** - no owner exists for LENGTH anywhere in the 57-feature roster; this feature is where it lands |

## Design notes

**Why the edge-rate half is cheap and the length half is new.** P4 already owes
the transition time and does not know it: `docs/capability-roadmap/README.md`
§P4 specifies a `DelayModel` keyed by (input pin, output pin) with rise/fall
delays and a min:typ:max triple - **delays, no transition times** - while the
Liberty sweep states the missing slew in its own words. So the expensive half is
already owed by SDF, Liberty and SDC, and this feature adds the length
attribute, the lint and its report on top of work three other rows already
justify. That is why the band is 3-6 mw and not 8-9.

**Length is DECLARED, never derived from the canvas.** At 1 mm per grid square
the shortest drawable wire is already 0.133 lambda at 20 GHz - electrically long
before anything is drawn - and at 0.1 in per square it is 0.339 lambda. A scale
small enough to stay lumped (<= 0.5 mm/square, valid to 30 GHz) makes the
1000-square canvas a half-metre board, i.e. a layout rather than a schematic.
And schematic wire length is unrelated to routed trace length at any scale.
Three layers instead: a length parameter on a drawn element (zero format
version), an optional declared per-net length (inside FEAT-047's bump), and
back-annotation from the routed board (with CAP-05 and FEAT-060). The declared
route also degrades correctly - an undeclared net reports "not assessable"
rather than a fabricated verdict.

**The rule is time-domain and keyed to edge rate, not clock rate.**
`l_crit = v * t_r / 6`, equivalently lambda/10 at the knee `f_knee = 0.5 / t_r`
(Johnson & Graham, *High-Speed Digital Design*, Prentice Hall 1993). Computed:
`t_r` = 20 ps gives `f_knee` 25 GHz and `l_crit` 0.48 mm; `t_r` = 2 ns (74AC)
gives 48.2 mm on FR-4 and 70 mm on a breadboard; `t_r` = 18 ns (74LS) gives
434 mm and 630 mm. **Clock frequency does not appear in the derivation**, which
is the whole lesson and is the reason the lint is keyed on a transition time
this tree does not yet have.

**One rule, one exposed constant.** With `D = v * t_r`, `lambda_knee = 2D`, so
`lambda_knee/10 = D/5`, `lambda_knee/12 = D/6` (Johnson's rule) and
`lambda_knee/4 = D/2` (the round-trip rule). The three "competing" rules differ
only in the divisor. Verified numerically. Hard-coding one of them would bake a
tolerance choice into a diagnostic; criterion 3 exposes it instead.

**The format hazard, and the door that expires.** `docs/file-format.md:220` at
`2d0ca9d` is normative - "Unknown attribute names are silently ignored" - and
`:222` records that the value is then dropped without error. A dropped **lint
input** is fail-open and harmless; a dropped **constraint** is not, which is why
FEAT-060 cannot ride the same valve. Shipping the attribute inside FEAT-047's
bump is priced at 1.5-3 mw; retrofitting it behind FEAT-013's must-understand
sections later is priced at 4-7 mw. **That sequencing door expires when FEAT-047
merges**, and it is worth 4-7 mw.

**This feature ships FIRST of the three CAP-18 rungs, and the ordering is by
permanence rather than by cost.** It commits no element type, no palette entry,
no frozen save tag and no K9 obligation; FEAT-059 commits all four and is
cheaper. If the programme stalls after this rung, JLS owns a correct, permanent
sentence about its own domain of validity, which is a complete deliverable.

## Risks

- **A default that is not absent breaks every golden in the tree.** Both
  attributes must be optional and unset by default. This is the single decision
  that keeps the band at 3-6 mw.
- **Lint noise is a pedagogy failure in a new costume (K18-3).** If the default
  produces any verdict other than "not assessable" on `examples/`, the default
  is wrong, and a lint students learn to ignore is worse than no lint.
- **This is the calibration experiment for the whole programme (K18-6).** Every
  figure in CAP-18 divides by a delivery rate of ~200-250 shipped-and-tested
  lines per maintainer-week at the 93.0/92.0/84.5 JaCoCo aggregate plus the
  80/82 PIT bar - the corpus's own weakest number. If this feature exceeds 12
  maintainer-weeks the rate is at least 2x wrong and the whole programme
  re-costs. That is 2x the top of this band, so the trigger does not fire
  inside the estimate.
- **Scope creep into the structured delay model.** A transition time invites
  rewriting element delays in physical units in the same change. That is P4's
  own structured-delay work and an order of magnitude larger; this feature adds
  the transition time beside the existing delay and stops.

## Evidence

- **No length and no impedance exist at HEAD.** Verified at `2d0ca9d`:
  `src/jls/elem/WireNet.java:22-30` is the whole field set - `ends`, `wires`,
  `bits`, `hasinput`, `triState` - with no length, no impedance and no delay.
  Landmark: the five field declarations immediately below the `issue #98, S1`
  comment on multi-driver resolution.
- **Element delay is a unitless integer with no transition time.** Verified at
  `2d0ca9d`: `src/jls/elem/Adder.java:33` is
  `private static final int defaultPropDelay = 30;` and `:261`, inside
  `resetPropDelay()`, is `propDelay = bits * defaultPropDelay;`.
- **Time has no unit.** Verified at `2d0ca9d`:
  `docs/simulation-semantics.md:26` - "Simulation time is a dimensionless
  non-negative 64-bit integer" - under the `## 1. Time model` heading.
- **The silent-drop valve this feature must not rely on.** Verified at
  `2d0ca9d`: `docs/file-format.md:220-222`, "Unknown attribute names are
  silently ignored", with the value "dropped without error".
- **The roadmap already states the gap.** `docs/capability-roadmap/README.md`
  §P4's `DelayModel` (rise/fall delays, min:typ:max, no transition time) and
  `docs/capability-roadmap/sweep-02-timing.md:110`, "JLS has one integer, no
  slew, no load, no fanout awareness".
- **No signal-integrity vocabulary exists in the planning corpus.** At
  `2d0ca9d`, a case-insensitive grep for impedance, transmission line,
  S-parameter, Touchstone, eye diagram, signal integrity and crosstalk across
  `docs/plan/` returned five files, **all false positives** - four saying
  "transimpedance amplifier" and one saying "high-impedance". This is a
  measurement of a gap and never an argument against filling it (D10 rule 2).
- **Scope, cost and owner.** `docs/plan/capstones/CAP-18-net-that-stopped-being-a-wire.md`
  §7.1 (landmark: the table under the heading "### 7.1 The four new features"),
  read in the working tree at `839fb3a`; the same table is at
  `docs/plan/evidence/highfreq-determination.md:1451` in that tree. **Neither
  path resolves at `2d0ca9d`** - the CAP-18 document predates the evidence
  directory's landing and the determination landed in `3a81a4a` - which is
  recorded here per D12 rather than left as a citation that does not resolve.
- **Cost reconciliation.** Band **3-6 mw**, taken from CAP-18 §7.1's feature
  table, which is the figure #313 cites and the figure other issues resolve
  against. **The staged arithmetic in the same source does not reproduce it and
  is printed rather than adjusted:** `highfreq-determination.md` §6.3 prices
  stage H1 (transition time + the lint) at **2-4 mw** and §2.2 prices the
  declared length attribute at **1.5-3 mw** incremental inside FEAT-047's bump,
  summing to **3.5-7 mw**; §6.2's own itemisation (lint 1-2, length/edge-rate
  attributes 1.5-3) sums to **2.5-5 mw**; and §6.3's stage H0 is 3.5-6 mw of
  which FEAT-047's 2-3 is owed anyway, leaving **0.5-3 mw** for the length half.
  Three derivations, three answers, none of which is 3-6. The feature-tier
  figure is carried because it is the one the filed issues cite, and the
  disagreement is recorded here so a re-costing pass does not rediscover it.
  No task rollup exists: this feature has no task ids.
