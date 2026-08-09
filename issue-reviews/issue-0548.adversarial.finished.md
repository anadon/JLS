# Issue #548: FEAT-C27-2: an Examples menu ships at least ten curated circuits — each one loads, simulates, and carries a caption and a suggested exercise
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what the issue claims

An Examples menu with ≥10 curated circuits (combinational, sequential, FSM,
datapath, plus an RV32I showcase), each carrying a caption and a one-line
exercise, loading through the standard classpath reader and simulating under
`BatchSimulator` in a headless test. It explicitly claims to build on
`resources/samples/` work from #73 and #381 rather than forking a second
mechanism.

## Findings, most severe first

**1. The "RV32I showcase" AC-2 requires is, by the project's own other issues, not shippable as a GUI sample yet — and #548 never cites the blocker.**
`riscv/README.md` describes a *generated* full RV32I CPU built by
`make_cpu.py`/`jlsbuild.py`. Issue #202 (the RV32I worked-example feature)
lists as an explicitly gated planned task: *"Curriculum/sample circuit: ship
the CPU as a first-run sample; **GATED on auto-layout #62** (generated .jls
uses nominal overlapping coordinates)."* Issue #62 itself says plainly:
*"a generated netlist (e.g. the RV32I CPU, #202) opens as an unusable overlap
pile"* and lists *"Layout entry point for programmatically generated .jls
netlists (generator consumers: #202 CPU, #73 sample circuits)"* as an
**unfiled** planned task, itself blocked on #290 (open, not yet run) for the
rubric that would validate the layouter at CPU scale. #548's YAML frontmatter
declares only `ordering_after: [381]` — #62 and #202 are absent from its
dependency graph, its boundary notes, and its `related` references. AC-2 ("…
plus the RV32I showcase … are in the set") is therefore asking for an artifact
that, per the project's own recorded state, cannot be placed onto a readable
schematic without work AC-2 does not order after.
*Recommendation:* add #62 (or at minimum its generated-netlist layout entry
point) to `ordering_after`, or explicitly descope to the separate,
already-laid-out `riscv/gui/cpu.jls` artifact (see Finding 2) and say so.

**2. Two different things could plausibly be "the RV32I showcase," and the issue never disambiguates them.**
The repo actually contains two RV32I artifacts: (a) `riscv/` — the full,
faithful RV32I CPU produced by `make_cpu.py` (whole ISA, ROM/RAM,
overlap-laid-out per Finding 1), and (b) `riscv/gui/cpu.jls` (8.8 KB, 530
lines) — a much smaller demo built purely to exercise a GUI-automation test
harness, executing a single instruction (`addi x1, x1, 3`) in an infinite
loop, with no ALU, decoder, or memory. It *was* placed via real GUI clicks
(`GuiDriver`), so it plausibly sidesteps the #62 layout blocker — but as a
pedagogical "RV32I showcase" it demonstrates almost nothing about RV32I
beyond one instruction's datapath, and it lives under `riscv/gui/` as test
tooling, not curated content. #548's single phrase "the RV32I showcase that
today sits unsurfaced in the repo" is compatible with either reading, and the
two have wildly different cost and pedagogical value. *Recommendation: name
the exact file/artifact in the issue body before this is actionable.*

**3. The corpus mechanism this issue says it "extends" doesn't exist yet, and two sibling issues both claim to create it — #548 orders after only one of them.**
`resources/samples/` does not exist in the tree (`ls resources/` → `help`,
`packaging` only — confirmed by #381's own O3 observation, still true at
HEAD). `SampleCircuitsTest` does not exist (`find . -iname "*SampleCircuit*"`
→ no results). #73 lists "Five bundled sample circuits under
`resources/samples/`" as its own *unfiled* planned task; #381 lists the same
mechanism as part of its residual (also unfiled/undone, P2/P3 both "FAILS,
must hold after"). #548's boundary note only handles one ordering: *"If #381
lands its samples first, extend that set in place."* It says nothing about
what happens if #73's own samples task lands first (equally plausible — #73
is a still-open sibling with the identical planned task), or if both land
independently and diverge. This is a real coordination gap, not a hypothetical:
nothing in the repo today prevents two agents from independently creating
`resources/samples/` under #73 and #381.
*Recommendation:* #548 should name a single authoritative parent for
`resources/samples/`'s creation (or a REPLAN-style tie-break rule), not just
one of the two candidates.

**4. AC-3's acceptance test is a citation to a test that is itself only a plan.**
AC-3 says the loader/simulate/caption check is "the `SampleCircuitsTest`
shape #381 P8 plans" — but #381 P8 is a *prediction*, marked "must hold
after," for a test file that does not exist. If #381 stalls (it bundles a
110-call-site dark-theme color sweep, a scaling/screenshot matrix across five
platforms, and a five-subject human usability trial — none of which is
sample-related), #548 has no independent, self-contained acceptance test of
its own to fall back on; it is contractually borrowing another issue's
not-yet-written harness. *Recommendation:* #548 should specify its own test
class/shape inline (even if it reuses #381's once it exists) rather than
pointing at a plan.

**5. AC-5 ("K9/D9 holds … no other default-view conceptual load") has no stated verification method and is easy to satisfy on paper while failing in spirit.**
Contrast with #381, which ties the identical K9/D9 concern to an explicit
completion-criteria line: *"stated and justified in the PR."* #548's AC-5 says
only "the menu adds one top-level entry and no other default-view conceptual
load" with no test, no review checklist item, and no PR-writeup requirement
named. An implementation could add exactly one "Examples" top-level menu that
is itself dense with ten category-labeled submenus, icons, and tooltips —
technically "one top-level entry," while clearly increasing what a first-year
sees. As written, AC-5 is not falsifiable by anything the issue names.
*Recommendation:* either cite a concrete check (a #91-harness menu-bar
item-count assertion, or a required PR justification paragraph as #381 does)
or drop the AC in favor of review judgment stated explicitly as such.

**6. AC-4's length bound is undefined, making it gameable.**
"No longer than a caption" is quoted from CAP-27 but neither #548 nor #511
gives a caption a word/character bound anywhere. Nothing stops an
implementation from writing a three-sentence "caption" and a three-sentence
"exercise" of matching length and technically satisfying AC-4 while violating
the "without reading anything longer than a caption" spirit the whole
capstone is built on. *Recommendation:* state a concrete bound (e.g. ≤120
characters) the way #381 and other AC-heavy issues in this tracker do
elsewhere.

**7. Where the caption is actually shown before commitment is unspecified, weakening AC-1's "no prior knowledge" claim.**
#381 P8 designs the caption as "a header Text element" living *inside* the
circuit file. AC-1 requires the ≥10 examples be "discoverable from the menu
bar with no prior knowledge," and AC-4 implies the caption is read as part of
deciding what to open ("no longer than a caption" is a *reading-budget*
constraint on the decision, per CAP-27's framing "without reading anything
longer than a caption"). If the only place the caption lives is a Text
element rendered after the circuit is opened, a user has already committed
before they can read it, which undercuts the ten-minutes/caption-only
on-ramp CAP-27 (#511) is chartered around. #548 never says whether the menu
item itself carries a tooltip/description or whether "caption" solely means
the in-circuit element from #381's design.
*Recommendation:* specify where the caption text is surfaced (menu tooltip,
submenu label, or pre-open preview) rather than assuming the in-circuit Text
element satisfies AC-1's "no prior knowledge" bar.

**8. No stability contract for the corpus, despite three other filed features consuming it verbatim.**
The issue's own comment records that #551 (SVG gallery), #552 (build-along
lessons), and #573 (browser demo) all consume this issue's exact circuit set
by name/content — #551's AC is literally regenerated from #548's circuits,
#573's AC-4 "explicitly names this issue's set as its content." Yet #548
gives no interface-stability statement (contrast with `docs/batch-interface.md`
and `docs/file-format.md`, which the repo treats as normative contracts).
Nothing in #548 says the ten circuit names/files are frozen once downstream
work begins, so a late substitution here (e.g. resolving Finding 1 by
swapping the RV32I entry) silently invalidates work in three other issues
with no stated propagation mechanism.
*Recommendation:* add a line committing the published set (file names, at
minimum) as stable once #551/#552/#573 begin consuming it, with a REPLAN-style
escape hatch if it must change.

## What's solid

- AC-3's core mechanics — classpath read, standard open path, headless
  `BatchSimulator` run — is concrete, testable, and correctly grounded in the
  repo's actual loader (`FileAbstractor`/`Circuit.load`) and headless
  simulator boundary (`ARCHITECTURE.md`'s `HeadlessCoreRatchetTest`
  discipline).
- The de-duplication against #551/#552/#573 (corpus-vs-surface boundary) is
  well-reasoned and avoids scope creep in the other direction.
- Citing #130 ("never `user.dir`") is accurate: #130 is closed/completed and
  its seed-directory rule is exactly the failure mode a sample opener could
  reintroduce.
- `band_mw: "2-3"` matches CAP-27's own PF-2 estimate in #511 — no
  inflation there.

## Bottom line

The issue is well-scoped relative to its sibling features but rests on two
foundations — the `resources/samples/` mechanism and a presentable RV32I
CPU — that the project's own other open issues (#73, #381, #202, #62)
document as not yet built, and in the RV32I case, explicitly gated behind
work #548 doesn't cite. AC-2 as written cannot be verified true today without
either resolving #62/#202 first or narrowing which "RV32I showcase" is meant.
Needs rework on the dependency graph and AC wording before this is
actionable, not a redesign of the outcome itself.
