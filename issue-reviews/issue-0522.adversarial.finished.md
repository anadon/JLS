# Issue #522: CAP-38: a drawn circuit lands on the FPGA board a classroom actually has — from the GUI, through the open toolchain, with the pin map checked before the cable is touched
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what this issue actually is

#522 is a "capstone" issue (`tier:capstone`) that stitches together four
unfiled planned features (PF-1..PF-4) into one classroom outcome: draw →
assign pins in a GUI dialog → programmed board via the open toolchain. It
does not carry code-level work itself; it names an outcome and cites the
in-tree headless `-board`/`-pins`/`PcfEmitter` machinery (verified present:
`src/jls/JLSStart.java:111-114, 782-786, 908-916, 1096-1109`,
`src/jls/hdl/board/{Board,Boards,PcfEmitter,PinBindings}.java`) as the
substrate. Unusually for this fleet, the issue's own single comment
(posted by the same maintainer account four hours after filing) is itself
an adversarial coverage audit that found and disclosed three of the
contradictions below before I looked — a genuinely good practice, but it
also means the body was never edited to fix what its own author-agent
found wrong with it, and two of the four planned features it names were
filed as new issues (#597, #598, #599) without #522 being updated to
reflect that its own scope now already has children.

## Findings, most severe first

**1. The capstone's demo-slice premise — a working end-to-end iCEstick path already exists to build a GUI on top of — is false, and the issue never says so.**
The outcome text asserts "the ingredients exist — PCF emitter, `-board`/
`-pins`, the scripted iCEstick handoff, #264's staged plan" and the
`demo_slice` field promises "PF-1 GUI flow over the **existing headless
path** for the iCEstick, 2-3 mw" as if that headless path is proven. But
#264 (the feature #522 cites and orders itself after) says the opposite in
its own body: "**Evidence correction**... Only the hermetic stub-PATH
selftest runs in CI — fake tools, control-flow assertions only. No real
synthesis, place-and-route, or bitstream production has been evidenced
anywhere." #264's own Definition of Done still has "Handoff script
produces a real bitstream for the sample circuit on each board" unchecked,
and #416 (O5) confirms "no nextpnr is installed anywhere" in CI at the
time of filing. AC-1 of #522 ("draw → pins → programmed board demonstrated
end-to-end... no terminal") is therefore not a GUI-layering exercise over
a working headless flow; it inherits an unresolved feasibility question
(does the real iCE40 toolchain even accept JLS's PCF/Verilog output?) that
#264 has not yet answered. Costing PF-1 at 2-3 mw assumes that question
resolves for free.
**Recommendation:** either make #522 explicitly `blocked_by: [264]`'s
"iCEstick real-toolchain evidence" planned task, or strip the "existing
headless path" framing from `demo_slice` and price in the risk that the
GUI layer is built on a headless path that has never produced a real
bitstream.

**2. `ordering_after` names a task, not the feature tier it claims to order against — a tier violation the issue's own comment already found and left unfixed.**
The machine block orders #522 after `"#288 (GUI HDL export menu entry —
the seam the board flow extends)"`. I fetched #288: `tier: task`,
`part_of_feature: 75`. Its actual owning feature is #75 (confirmed:
`tier: feature`, `requires_tasks: [288]`), not #288 itself. The issue's
own comment (`5175568258`) states this plainly: "A capstone ordering edge
pointing at a task is a tier violation under the three-tier model — the
feature-tier referent is **#75**." That comment is four hours old at the
time of this review and the issue body still reads #288. A tier violation
in an ordering edge is not cosmetic in this project's process — other
capstones' machine blocks are walked programmatically for cycle/tier
checks (see #264, #416's own "ordering-cycle walk" sections), so a
dangling task-level edge is exactly the kind of thing that silently
breaks that tooling.
**Recommendation:** edit `ordering_after` to `#75` (or add both, with #288
marked non-blocking/informational) before this issue is used to drive any
scheduling automation.

**3. PF-3's load-bearing dependency (#416) is both openly contested in ownership and blocked by an open issue — "rides this" overstates how settled that ground is.**
The body states "#416 TASK-0052 owns the second board" and PF-3 says
"#416's second board rides this." But #416's own machine block declares
`part_of_feature: 359` while stating in the same block that "#359's Open
Question 3 recommends re-homing it to #264" and that #416's own Open
Question 1 leaves the re-homing undecided. #416 is also `blocked_by:
[386]`, and #386 is open. So PF-3 is built on a second-board task whose
owning feature is unresolved and whose landing is gated on an open
prerequisite that (per #416 O5) has no toolchain installed anywhere yet.
None of this is fatal to filing the capstone, but "rides this" reads as
settled when the cited issue itself repeatedly flags that it is not.
**Recommendation:** state PF-3's Basys-3-vs-open-toolchain-board scope
independently of #416's landing, so PF-3 doesn't inherit #416's ownership
uncertainty by citation.

**4. AC-3's CI-lane requirement may be satisfiable by a lane that is allowed to skip — which is the exact failure mode the issue's own cited standard exists to forbid.**
AC-3 reads: "The bitstream-minus-programming path is a CI lane on every
push touching the flow." PF-4 cites "no lane passes by not running
(TASK-0043 #386 standard)" — already flagged by the issue's own comment as
misnumbered (#386 is TASK-0051, not TASK-0043) but substantively pointing
at the right issue. However #264's own Open Question 2 — the feature this
capstone orders itself after — says: "can a CI lane install oss-cad-suite
for a true synth+P&R smoke, or does that evidence stay manual-only?
Recommended default: attempt a non-required CI lane... fall back to a
recorded manual run." A "non-required" lane that "falls back to manual"
is precisely a lane that can pass, or simply not gate anything, without
running the real toolchain — the opposite of the #386/#359 "a skip is
never silently a pass" invariant AC-3 is supposed to inherit. AC-3 as
worded does not say the lane must be required/blocking, only that it must
exist "on every push," so it can be satisfied by a lane identical in
spirit to #264's own hedge.
**Recommendation:** AC-3 should say explicitly whether the lane must be a
required (blocking) check or may be advisory-with-recorded-skip-count,
and cross-reference #359 §4.3/§5.1's skip-accounting mechanism by name
rather than leaving it implicit.

**5. AC-2's "specific diagnostic" tests are gameable via the same stubbed-tool pattern the sibling board issues already use, and the issue does not rule that out.**
AC-2: "Every pre-flight failure mode has a test asserting its specific
diagnostic." Nothing in the acceptance criteria requires the diagnostic
tests to run against real tools rather than the hermetic stub-PATH
pattern #264/#416 already use for their handoff-script selftests (control
flow over fake `yosys`/`nextpnr-*` binaries). A test suite can satisfy
AC-2 in full while never exercising a real toolchain's actual failure
messages (version mismatches, obscure yosys errors, etc.) — the same gap
#416 names explicitly for its own `FlashRecordTest`: "asserts presence and
shape, never truth." PF-2's text ("Logisim-Evolution's #91-class silent
toolchain failures are the anti-pattern this PF exists to not have") sets
a bar that AC-2 as worded doesn't actually enforce.
**Recommendation:** add a prediction that at least one pre-flight
diagnostic is exercised against the real installed toolchain (gated the
same way #264/#416 gate real-toolchain evidence), distinct from the
stubbed control-flow tests, or explicitly accept the weaker stub-only bar
and say so.

**6. AC-5's evidentiary artifact (a photograph) is not machine-checkable and has no named owner for staleness.**
AC-5: "The flow's docs page carries the CAP-35 screenshot pipeline output
and a photo of the real board doing it." A photo in a docs page is not
pinned by any test, golden, or CI check — nothing catches it going stale
when the flow changes, unlike every other artifact this capstone leans on
(PCF goldens, `FlashRecordTest`'s presence-only check, etc.). The sibling
issue #416 is unusually careful to say out loud that its own analogous
artifact ("the flash record... asserts presence and shape, never truth")
can't be verified by CI — #522 imports the same category of unverifiable
evidence for AC-5 without that caveat.
**Recommendation:** either drop the photo requirement in favor of the
already-adequate real-hardware CI/manual-record trail #264/#416 define,
or explicitly document (as #416 does) that AC-5 is presence/staleness-only
and name who re-checks it per release.

**7. KC-38-1 (no vendor-toolchain automation) and PF-3's Basys-3 scope pull in opposite directions, and the cost estimate doesn't disentangle them.**
KC-38-1 rules Vivado/Quartus process-driving out of scope for v1. PF-3
prices "Basys-3 support... requires the Xilinx openXC7/vendor-handoff
decision priced and recorded, possibly as a documented-recipe refusal per
D8" at 2-4 mw, in the same PF as "the open-toolchain boards (#416's second
board rides this)." Since KC-38-1 already forecloses the most likely
reason Basys-3 would need real engineering (driving Vivado), the 2-4 mw
figure is either mostly documentation work (a "refused, here's why" write-
up) or it silently assumes openXC7 (Xilinx Artix-7 open synthesis) is
mature enough to avoid the vendor tool — a nontrivial technical bet that
AC-4 only requires be "written," not validated to actually work. The
issue conflates "decide and document" cost with "build and demonstrate"
cost inside one PF and one band.
**Recommendation:** split PF-3's cost into a small, bounded "decision
memo" line item (which is what AC-4 actually requires) and a separate,
explicitly gated line item for openXC7 engineering if the decision comes
back "supported."

**8. `related: ["#443 TASK-0027 (installer legs)"]` is a weak, unexplained citation.**
I read #443: it is entirely about promoting native installer CI legs from
experimental, pinning the bundled JetBrains Runtime by digest, and
asserting `.jls` file-association on Windows/macOS. Nothing in #443
touches HDL export, boards, pins, or the open FPGA toolchain. The capstone
gives no rationale for why installer packaging is `related` to an FPGA
board flow (perhaps "the whole pipeline needs a trustworthy installer,"
but that applies to every capstone equally and isn't stated). This is a
minor citation-hygiene issue, not a blocker, but it is the kind of loose
relation that makes the `related` field's purpose ambiguous.
**Recommendation:** either state the actual relationship (if any) or drop
the citation — an unexplained `related` entry costs a reader time for no
signal.

**9. `filed_by` discloses the issue is intentionally incomplete ("lean filing; supplementary Phase-B coverage run will complete the feature set"), which undercuts treating AC-1..AC-5 as final today.**
This is disclosed honestly rather than hidden, so it is not a
contradiction, but it does mean a reader evaluating #522 today is
evaluating a known-partial draft. Combined with Finding 1 (feasibility of
the demo slice is actually open) and the fact that two of the four PFs
were filed as separate issues within hours (#597, #598) without #522
being updated to link them, the issue as it stands is a snapshot mid-
process rather than a stable spec.
**Recommendation:** once the Phase-B coverage run completes, edit #522's
body to link #597/#598/#599 by number in Planned Features (not just leave
them to a comment) so the capstone document is self-contained.

## What holds up

- The evidence claim that JLS's board/pin machinery is real but
unreachable from the GUI is accurate: `grep` over `src/jls/JLSStart.java`
confirms `-board`/`-pins` are CLI-only flag-driven code paths
(`JLSStart.java:387-473`), and #288's own body independently confirms "No
HDL export menu item exists" and explicitly puts "board/pin-constraint
selection UI" out of its own scope — so the gap PF-1 targets is real and
not already covered elsewhere, matching the maintainer comment's own
coverage-search conclusion.
- KC-38-1 (excluding Vivado/Quartus process-driving from v1, citing
Logisim-Evolution's #91 toolchain-detection pain as the cautionary
example) is a sound, concretely-justified scope boundary.
- KC-38-2's REPLAN trigger (shrink to PF-2..4 if #264 lands the GUI flow
itself) is exactly the right kind of self-correcting kill criterion, and
correctly identifies the actual duplication risk between #522 and #264.
- The issue's own comment doing an adversarial coverage pass against open
issues before filing new ones (rather than filing blind duplicates) is a
good practice this fleet should note as a positive pattern — it caught
three of this review's own findings independently.
