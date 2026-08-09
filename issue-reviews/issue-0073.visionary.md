# Issue #73: First-run onboarding: welcome/empty state, sample circuits, tutorial discoverability, applet-era cleanup, README screenshots
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

The goal is right and is the cheapest high-leverage lever on the board: a student who
types `java -jar` should reach a running circuit without a human guide. #511 (CAP-27)
independently scored the on-ramp 2/5 and made the same finding. Nothing below disputes
the outcome.

What I dispute is that #73 is still the vehicle for it. As of the 2026-08-08 comment,
all three `planned_tasks` are owned by #381/#545/#548/#550/#552, the tutorial slice
landed in PR #194, the Edit menu landed in #75, and the applet clause is measured down
to one stale javadoc line (`src/jls/edit/SimpleEditor.java:452`). The issue's own
comment says it plainly: "a superseded-scope pointer, not a work item." What remains is
IC1/IC2/IC3 — and I think all three are the wrong instruments, which is why the verdict
is redirect rather than endorse-with-reframing. The residual should be replaced, not
carried.

## IC1: replace a one-shot human study with an executable ten-minute claim

IC1 asks for five screen-recorded volunteers, unaided, within ten minutes, ≥4/5. The
issue already concedes it has no baseline and is forward-only; #381 §14 requires the PR
to "say plainly that n=5 is directional." So the criterion is: expensive, unrepeatable,
externally blocked on volunteer recruitment (the one open question), runnable only after
five separate features land, and it protects nothing afterward. Meanwhile #511 AC-2
already specifies the better instrument — "a scripted fresh-user protocol (documented,
re-runnable)" across three OSes — and #550 AC-1 makes the launch path a per-commit gate.

The project can go further than either, because it already owns every piece:

- `scripts/wayland-rig.sh` boots the real GUI under headless sway and screenshots it on
  every push and nightly (the `gui-wayland` lane), with a self-test pinning its exit-code
  classification.
- `test/jls/ui/` Layer 2 drives real listener chains under Xvfb with an EDT-violation
  detector (`EditorGestureTest`, `MenuBarSpecTest`, `InteractiveSimulatorSmokeTest`).
- `BatchSimulator` + `-t` + `-vcd` can assert the counter actually counts.

A "first-run walk" test — launch from an empty working directory, assert the welcome
surface, fire its `Open sample` action, run the counter, assert the trace — is squarely
Layer-2 territory and runs every commit. That is the ten-minute claim made *structural*:
the path cannot be longer than N clicks because a test walks exactly those clicks.
**I am explicitly disregarding IC1 as an acceptance gate.** Keep the human trial, but
demote it to what it is good at — qualitative discovery of *where* people stall, run once
for design input, findings recorded as prose. Gating a five-feature close-out on
volunteer recruitment is how this issue stays open for another two quarters.

## The samples should be a build output, not a checked-in asset directory

`resources/samples/` as five hand-drawn `.jls` files is the plan in #73, #381 and #548
(scaled to ten). Hand-drawn circuits are XZ or plain-text blobs that no reviewer can
diff, no test can explain, and nobody will regenerate when the format moves. Ten of them
is ten unreviewable artifacts, and #548 then wants captions and exercises attached to
each by convention.

The repository already contains the answer and does not use it: `riscv/jlsbuild.py` is a
322-line netlist compiler that emits FORMAT 1 plain-text `.jls`, with every emitter
validated against the real batch simulator by `riscv/test_primitives.py`, and it built a
full single-cycle RV32I CPU (`riscv/gui/cpu.jls`, 477-line `build_cpu.py`) that #511
correctly calls "the unsurfaced RV32I showcase." The 2026-07-17 "author fresh so
licensing is clean" decision is satisfied *more* cleanly by generated circuits: the
source of truth is a reviewable program, licensing is unambiguous, and the corpus is
regenerable.

The honest caveat, and it is the interesting part: `jlsbuild.py`'s docstring says
"geometry is irrelevant to simulation," so a netlist-compiled full adder would render as
spaghetti — useless as a teaching drawing. So the right shape is hybrid, and it names an
architectural seam the issue never reaches:

- The 3–5 *teaching* circuits (adder, counter, mux, subcircuit, FSM) are hand-laid-out,
  because the layout **is** the pedagogy.
- The *showcase* circuits (RV32I, ALU, register file) are generated, because hand layout
  at that scale is infeasible and is why the showcase is unsurfaced today.
- Geometry for generated circuits is a placement problem, and #62 (ELK, already recorded
  as a subprocess integration in ARCHITECTURE.md's plugin-trust decision) is the owner.

Stating that seam now costs one paragraph; discovering it after ten circuits are drawn by
hand costs the ten circuits.

## README figures should be rendered, not screenshotted

`-i out.svg` ships today (`CircuitRenderer.exportImage`, README lines 130-135) and #511
PF-4 already names it for the gallery. Yet #73 and #545 both plan *captured screenshots*,
and #545 invents AC-4: "a drift check that fails the build when a README-referenced image
path does not exist." Existence is the weakest drift check imaginable — it passes forever
on a stale-but-present PNG showing a circuit that no longer loads.

Generate the circuit figures from the sample corpus at build time with `-i out.svg` and
the failure mode vanishes: a figure cannot depict a circuit that does not load, because
producing the figure loads it. #73's IC2 ("every sample the README or panel names appears
in File→Open Sample and loads on a clean install") stops being a manual close-out
procedure and becomes a build invariant. Only genuine *chrome* shots — the welcome pane,
the trace window, the palette — need a raster, and the `gui-wayland` lane already takes
screenshots of the running GUI on every push. Point it at two named states and the
screenshot chore disappears into a lane that exists.

That is a concrete alternative to IC2 as written: replace "verified by a scripted or
recorded procedure at close-out" with "figures are build outputs."

## The README's real defect is a document boundary, not a missing image

Of 368 README lines, roughly 275 are installers, checksums, cosign, attestation,
reproducibility, Wayland toolkits, dev containers and the rig. The student-facing
description of what JLS *is* runs from line 5 to line 10. That provenance material is
genuinely excellent and I would not delete a word of it — but this is a supply-chain
README on an educational tool, and every filed successor (#73, #545, #760) proposes
adding *more* sections to the same document.

The reframing: split it. A front page for the student and the evaluating instructor —
what it is, a picture, one run command, first circuit, honest positioning — and
`docs/install.md` plus `docs/verification.md` for the packaging and provenance material,
where the people who verify attestations actually look. The project has already made
exactly this move for contracts (`docs/batch-interface.md`, `docs/file-format.md`
normative, README informative); the install surface is the last thing that never got the
treatment. Two screenshots pasted above 275 lines of `cosign verify` will not make an
instructor's first thirty seconds work.

## The seam #73 is uniquely positioned to record before it closes

Five filed features now consume the same corpus: #548 (ten circuits + caption +
exercise), #551 (SVG gallery), #545 (README figures), #552 (stepped lessons), and the
in-jar help tree under `resources/help/**` that `HelpTopicsTest` already keeps truthful.
Each is filed as an independent feature producing its own presentation of the same
material, which guarantees five drifting copies of every caption.

The project's own instincts point the other way: `SaveTags` gave the loader one table
instead of a per-element switch, `Attribute` gave persistence one registry,
`ExtensionPoint`/`ExtensionPointCatalogTest` gave seams one catalog cross-checked in both
directions. A **teaching corpus** — circuit, caption, suggested exercise, lesson steps,
in one declared source — with the Examples menu, the gallery, the README figures, the
help page and the lessons all being *renderings* of it, is the same move applied to
content. ARCHITECTURE.md's recorded "help delivery: in-jar now, hosted docs planned"
direction (and #584/#585, which now own it) makes one source mandatory eventually
anyway; paying for it once, now, while the corpus is empty, is free.

#73 is the last document where all five consumers are visible together. Recording that
contract is worth more than running the usability trial.

## Concrete disposition

1. Retire IC1 as a gate. Replace with #511 AC-2's scripted protocol plus a Layer-2
   first-run walk. Keep the n=5 trial as a one-off qualitative study, not a blocker.
2. Retire IC2 as a procedure. Make README/gallery figures build outputs of `-i out.svg`
   over the corpus; supersedes #545 AC-4.
3. Drop IC3. `TutorialContentTest.appletEraCopyIsGone()` already pins it on every commit;
   restating it as a feature criterion adds a re-verification chore and no protection.
4. Sweep the one surviving javadoc line at `SimpleEditor.java:452` inside #84.
5. Record the corpus contract and the generated-vs-hand-laid-out sample split (with #62
   named for geometry), then re-tier #73 from `tier:feature` to a CAP-27 pointer with
   `serves_capstones: [511]` — which also removes the "open feature with zero open
   children" anomaly the tracker keeps having to explain in a comment each cycle.

None of this is a criticism of the outcome. It is a claim that the outcome is now owned
by better-shaped work, and that #73's last useful act is to hand over a seam rather than
a checklist.
