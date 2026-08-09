# Issue #386: TASK-0051: the external-tool suites stop passing by not running, and the tools the board and parity work need are actually installed
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The core diagnosis is real and independently verified: `.github/workflows/ci.yml:73`'s
`|| echo "some optional tools unavailable; their tests will skip"` does let a build stay
green while `IverilogCompileTest`/`GhdlCompileTest` self-skip via `Assumptions.assumeTrue`
(`test/jls/hdl/IverilogCompileTest.java:34`, `GhdlCompileTest.java:35`,
`test/jls/hdl/scan/YosysGroundTruthTest.java:44`, `test/jls/hdl/imp/ImportPipelineTest.java:89`
— all confirmed at those exact lines). The `|| echo` and the required-vs-advisory job split
(`ci.yml:41`, `continue-on-error: ${{ matrix.java != 25 }}`) are also as described. But the
issue bundles that narrow, well-scoped CI fix with a real-hardware synthesis test that another
open issue already claims, a brand-new GUI feature with no owner in the governing parent
feature's task roster, and an unverified apt package name for exactly the kind of "cost
judgment" the issue's own D8 quote says should be made carefully. It should not proceed to
implementation until the ownership collision with #264 is resolved and the package names are
checked against the repo's own documented install line.

## Findings (most severe first)

### 1. [HIGH] Undeclared scope collision with #264's own claimed "iCEstick real-toolchain evidence" task

This issue's Method adds `test/jls/hdl/board/IcestickSynthesisSmokeTest.java`, "running the
real chain over the committed `.pcf` golden" — i.e. a real `yosys` → `nextpnr-ice40` →
`icepack` run over `test/resources/hdl/board/blinky_icestick.pcf` (P6). That is *verbatim*
the scope #264 already lists as its own unfiled `planned_tasks` entry: *"iCEstick
real-toolchain evidence: run icestick-handoff.sh with the real iCE40 toolchain (P&R smoke,
#213 P2)"*, and #264's completion criteria still show `- [ ] Handoff script produces a real
bitstream for the sample circuit on each board (... real-toolchain run outstanding)`. Worse,
the shared parent feature #359 (FEAT-023, which this issue is `part_of_feature: 359`) already
adjudicated exactly this class of overlap in its own **Open Question 3**: *"Who owns
TASK-0052 — this feature or #264? ... Recommended default: file TASK-0052 as #264's child ...
Filing it under both is a single-owner violation, since a task is `part_of` at most one
feature."* #386 lists #264 only under `related` (non-blocking), never engages with Open
Question 3's governance rule, and never states why a P&R smoke test embedded in TASK-0051 is
not the same deliverable Open Question 3 was written to prevent from being claimed twice.
**Recommendation:** either strike `IcestickSynthesisSmokeTest`/P6 from this issue and let
#264 own the real-toolchain P&R evidence (its own planned task), or file a REPLAN comment on
#359 and #264 explicitly re-homing that scope onto #386 before work starts — not silently.

### 2. [HIGH] The issue's own comment thread cites a prediction ("P9") that does not exist in the issue

The 2026-08-08 "ABSORBED" comment (posted by the issue's own author while closing #662/#668 as
duplicates) says: *"That is the exact opposite of this task's P9 and of #406's P4/P9, both of
which make the install fail-closed."* #386's Predictions section (§5) runs only **P1
through P8** — there is no P9 anywhere in the issue body. The P9 being described (install
failure → job goes red, verified by deliberately breaking the install on a branch) is #406's
P9, not #386's. Attributing a fail-closed P9 to *this* issue is either a copy/paste error from
#406 or evidence the author intended to add one and didn't. Either way, an implementer
checking off "every post-fix prediction in §5 verified" (a DoD line item) has no P9 to verify,
while the comment thread implies one is owed. **Recommendation:** correct the comment (or add
the missing prediction to §5 if one was actually intended) before using this issue as the
audit trail its own DoD checklist requires.

### 3. [MEDIUM] Apt package name for `icestorm` is unverified and contradicts the repo's own documented install line

Method step 3 says: *"Add `nextpnr-ice40`, `icestorm` and `verilator` to `ci.yml:73`'s
package list."* But `docs/icestick-bitstream-handoff.md:26-27` — a document this very issue
cites as authoritative ("`docs/icestick-bitstream-handoff.md:19-31` is the documented tool
list, none of which CI installs today") — gives the actual example install line as:
`apt-get install yosys nextpnr fpga-icestorm`, i.e. **`fpga-icestorm`**, not `icestorm`.
Debian/Ubuntu package the icestorm toolchain (which provides `icepack`) as `fpga-icestorm`;
there is no `icestorm` source or binary package. The issue's own §11 (Threats to Validity)
carefully hedges the *macOS* brew names ("`nextpnr` is not in core homebrew under an obvious
name") but performs no equivalent check on the Linux apt names it directly instructs someone
to type into `ci.yml`. Since this same task is what removes `|| echo` and makes that install
step fatal on the required LTS leg, shipping the wrong package name would turn the very step
this task hardens into the first thing that breaks CI — the "RULE 3 RED STATE" pattern the
issue exists to eliminate, self-inflicted on day one. **Recommendation:** verify
`apt-cache policy nextpnr-ice40 fpga-icestorm verilator` on the actual runner image before
writing the workflow edit, and reconcile the Method step's tool list with
`docs/icestick-bitstream-handoff.md`'s existing example line (or correct whichever one is
wrong).

### 4. [MEDIUM] Scope creep: a new GUI feature is glued onto a CI-hardening task with no assigned owner in the parent feature's roster

The parent feature #359 lists five `planned_tasks`, and the one that maps to this issue
(TASK-0051) is described there, in full, as: *"the required Linux lane's tool install stops
ending in `|| echo`, a `JLS_REQUIRE_HDL_TOOLS` mode makes the four self-skipping suites fail
instead of skip when arming is demanded, and `nextpnr-ice40`, `icestorm` and `verilator` get
installed on the lanes that need them."* Nowhere in that description, nor in any of #359's
other four roster rows, does a File→Export HDL menu action or `HdlExportMenuTest` appear.
#386 nonetheless adds it (P3, `test/jls/edit/HdlExportMenuTest.java`), justified only by its
own Open Question 3 ("rides along") — a self-authorized scope addition, not one traced to the
feature's own decomposition. This mixes a narrow, low-risk CI/workflow change with a new
interactive Swing feature that has its own EDT-threading obligations (§7.9: *"The editor
action runs on the EDT and must perform the export off the EDT if it is long enough to
block"*) and its own display-tagged test infra, in the same PR. That roughly doubles the
review surface and the ways the PR can go wrong, for two deliverables whose only real
connection is that both happen to touch HDL export. **Recommendation:** split the GUI menu
action into its own issue (or explicitly REPLAN #359's roster to add it to TASK-0051's
contract) so a CI-policy reviewer isn't also reviewing new Swing UI code, and vice versa.

### 5. [LOW] P7's recorded-version list is narrower than the tool set this task arms

P7 requires each armed job's summary to carry `yosys -V`, `iverilog -V`, `ghdl --version` and
`nextpnr-ice40 --version` — four tools. But this same task installs and arms **six**:
`yosys`, `iverilog`, `ghdl`, `nextpnr-ice40`, `icepack`, `verilator` (P4's own list). §7.6
("Data provided") separately promises "one line per tool" in the version block, which implies
all six, not four. `icepack` in particular is newly exercised by this task's own P6 smoke test
and is exactly the kind of tool whose silent version drift the issue's own Threats section
(§11) warns about ("Leaving versions unpinned is deliberate and has a cost ... the mitigation
is P7's recorded versions") — yet P7 as written doesn't cover it, nor `verilator`, which the
issue admits is installed with "no test to arm yet." **Recommendation:** either extend P7 to
name all six tools or explicitly note in §11 why `icepack`/`verilator` versions aren't
recorded.

### 6. [LOW] H2's falsification criterion is unfalsifiable as written

"**H2 refuted** if a legitimate local workflow sets the variable inadvertently" has no
observation procedure: there's no way to enumerate "every legitimate local workflow" and
confirm none of them happens to export `JLS_REQUIRE_HDL_TOOLS=1`. As written this criterion
can be asserted-by-omission forever but never actually tested against, which is a weaker gate
than the rest of the issue's otherwise concrete, grep-and-rerun-able predictions (P1-P8 are
all reproducible commands). **Recommendation:** replace with something checkable, e.g. "no
existing script, Makefile target, or `.env.example` in this repository or `CONTRIBUTING.md`
sets `JLS_REQUIRE_HDL_TOOLS`" — a grep, not a belief.

## What holds up

- The central defect (O1-O3: `|| echo` plus four `assumeTrue`-gated suites means a green
  build proves nothing about whether the Verilog/VHDL/Yosys legs ran) is real, reproduced
  independently against the current tree, and precisely located.
- O4 (`verilator` absent from `src/`, `test/`, `.github/`, `scripts/`, `pom.xml`) and O6
  (`nextpnr-ice40`/`icepack` named in shipped source and docs but installed by no Linux/macOS
  step) both check out via direct grep against the working tree.
- O7's claim that `ci.yml:47-57`'s self-test exercises only a *stubbed* toolchain, not a real
  one, is accurate — `scripts/icestick-handoff-selftest.sh` explicitly builds a hermetic fake
  `PATH` and says so in its own header comment.
- The contributor-case guardrail (P8: a machine with no tools and the arming variable unset
  must still get a green `mvn verify` with clean skips) is sound and correctly identified as
  the thing that must not regress.
- §7.9's "`ProcessBuilder` stays in `test/`, never in `src/`" invariant is correctly grounded
  — confirmed absent from `src/` in this checkout, and worth keeping as a hard constraint on
  both the new smoke test and the GUI export action.
