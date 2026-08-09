# Issue #496: Machine calibration, part 1 of 2: re-homing preconditions, measured engine constants, element-cost table, boot-cost model (rescued from a branch that will be deleted)
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what this issue actually is

Not a work item — a verbatim copy of sections 1–4 of a 1,124-line
`docs/machine-calibration.md` that lived only on a branch
(`claude/jls-virtual-hardware-linux-njsoma`) the maintainer has decided to
delete. It is glued to a large dependency graph: part 2 is #494, companion
rescues are #484/#485, and #494 states "at least 77 filed issues cite it"
for acceptance/kill criteria. Below I verify the parts that are checkable
against the working tree and attack the parts that are load-bearing but not
self-evidently sound.

## What checks out (say once, move on)

Every anchor I spot-checked against HEAD matched exactly: `git ls-files
riscv` returns 26 files (issue claims 26); `riscv/.gitignore` line 1 is
`build/` and `riscv/build/` does not exist in the checkout (matches the
untracked-fixture claim); `test/jls/RiscvCpuGoldenTest.java:25` and `:38`
are `{@code}` (not `{@link}`) spans naming `riscv/examples/sum1to10.s` and
`riscv/README.md`; `Memory.java:1384-1386` is the `MemoryWrite` post and
`:1393-1403` the read/tristate branch; `RegisterFile.java:72` and
`FieldExtend.java:64` both declare `propDelay`, and neither class appears in
`HdlExporter.java`'s 22-entry `EXPORTED` set (`:422-428`) or in
`simulation-semantics.md`'s §6.2 zero-delay set / §7 delay table;
`docs/capability-roadmap/README.md:88-90` and
`keystone-c-performance.md:150` match the quoted text verbatim; `git
cat-file -e 8288226:docs/machine-calibration.md` fails as claimed. The
internal arithmetic (§4.4's boot-time table, §4.6's live-console chain,
§2.3's percentage groupings) is self-consistent and re-derivable. This is
an unusually well-grounded rescue document, not a hand-wavy one.

## Findings, most severe first

**1. The issue's own critical precondition is unexecuted, and nothing in the issue's structure forces it to be.**
§1.2 states, correctly, that `riscv/build/k2000.jls` — "the performance
anchor for every number in section 2" — was never tracked and "must be
regenerated and committed as a fixture before `bench_kernel.py` is deleted,
or section 2 becomes permanently unreproducible." That is exactly the state
today: the fixture does not exist in this checkout, and this issue (which
only copies prose into a GitHub issue body) does not commit it, fix the
`RiscvCpuGoldenTest` javadoc `{@code}`→`{@link}` rot it flags, move the
regeneration recipe out of the doomed directory, or decide the fate of
`riscv/gui/cpu.jls`. The issue title promises "re-homing preconditions" but
the issue itself performs zero of the four concrete actions its own table
prescribes. There is no explicit "Definition of Done" separating "the
evidence text is preserved" (true today, trivially, by having filed the
issue) from "the preconditions are satisfied" (not true). A closer could
mark this issue Done the moment it is filed — the stated goal is met by its
own existence — while the actions that matter (committing the fixture
before deletion) never happen and nobody notices, because nothing tests for
it.
*Recommendation:* split this into (a) the text-preservation issue (this
one, fine as-is, close on merge) and (b) a checklist issue/PR with one
checkbox per §1.2 row, each closed only by an artifact in the tree (the
committed `k2000.jls` fixture, the fixed javadoc, an explicit decision
recorded for `gui/cpu.jls`) — not by prose asserting the row was handled.

**2. Downstream "kill criteria" are pinned to a boot-cost model whose own inputs the document says do not close.**
§4.1's formula has two structurally unresolved inputs: `k` (1.07 vs 1.8,
"measured twice, differently," 1.68× apart) and `α` ("0.18/0.40/0.56 —
never measured," 3.1× band), plus a documented consistency-check *failure*
(§4.2: the shipped 468 ev/instr figure implies α = 0.155, below the band's
own floor — "unresolved and... the single most important open question in
this document"). Yet the issue text is explicit that #301 (18 citations),
#335, #377, #295, #407 and #413 use *this same table* by line number "for
acceptance criteria and kill criteria that have no other source." A kill
criterion sourced from a 5.2× honest uncertainty band (§4.2: 1.15h–6.0h) is
gameable in both directions — a downstream implementer can hit whichever
end of the band supports their preferred conclusion, and nothing in #496
flags that the citing issues should treat these numbers as provisional
pending the §6 experiments in #494 rather than as settled gates.
*Recommendation:* the citing issues (#301/#335/#377/#295/#407/#413) should
each state explicitly which arm of the k/α sweep their kill criterion uses
and be re-validated once #494 §6.1/§6.2's experiments land — this issue
should say so in-line rather than leaving it implicit in a cross-reference.

**3. No enforcement ties the ~77-issue dependency graph to this issue's continued stability.**
Unlike the repository's own normative docs (protected by `HelpTopicsTest`,
`FileFormatSpecTest`, `ElementConstructorContractTest`, etc., per
ARCHITECTURE.md), a GitHub issue body has no test, no diff review on edit,
and no git-blame line history visible to citers. The issue does mitigate
part of this well — citations resolve by *section heading* ("§2.5", "§4.2")
rather than by raw line number, which survives re-formatting — but the
claim "which are unchanged" is an assertion, not an enforced invariant. If
this issue body is later edited (even to fix a typo) in a way that
renumbers or retitles a subsection, every one of the (per #494) ~77 citing
issues silently desyncs with no CI signal, in a project that otherwise
treats exactly this kind of cross-reference rot as a testable defect (cf.
`HelpTopicsTest`'s link checker, the doclint gate this very issue complains
is blind to `{@code}` rot).
*Recommendation:* at minimum, lock/freeze this issue (or convert it to a
PR restoring a real `docs/machine-calibration.md` file on `master`, which
*would* get git blame, diffability, and could be covered by a link-checker
test analogous to `HelpTopicsTest`) rather than leaving it as a mutable
issue body 77 other issues silently trust.

**4. Minor arithmetic gap in an otherwise scrupulous document.**
§2.3's JFR profile-share table sums to 91.7% (37.6+17.1+15.3+10.1+5.2+4.9+1.5)
and 291.7 of the stated 318 ns baseline (119.6+54.4+48.7+32.1+16.5+15.6+4.8),
not 100%/318 ns, with no "other/unattributed" row acknowledging the missing
~8.3% (~26.3 ns). This is a small thing, but the document explicitly polices
this exact class of error elsewhere ("never quote a ns/node figure without
[stating which of the two]," "the range is not reproducible from the numbers
printed beside it" for a rejected keystone-c figure) — its own headline
table doesn't clear the bar it holds others to.
*Recommendation:* add the missing ~8.3% as an explicit "other/unattributed"
row, or correct the visible categories so they sum to the stated total.

**5. The "authorizes nothing" framing understates the document's actual function.**
The status line says: "This document is not a plan, it recommends no
architecture, and it authorizes nothing." In the same breath it declares
five sections "normative" for how numbers "may be quoted," and the rest of
the issue documents that ~6+ (per #494, ~77) other issues derive binding
acceptance/kill criteria from it. A document that gates other issues' pass/
fail conditions is functioning normatively regardless of its disclaimer;
a reader skimming only the status line could under-weight how load-bearing
these numbers already are.
*Recommendation:* replace "authorizes nothing" with an accurate statement,
e.g. "this document authorizes no architecture, but its §2–§4 figures are
already cited as binding acceptance/kill-criteria inputs elsewhere — treat
disagreements with those citing issues as an open conflict, not settled."

## Verdict rationale

The rescued content is unusually well-sourced — every file/line anchor I
checked against the current tree was accurate, and the internal arithmetic
holds up. That argues against "needs-rework" or "should-not-proceed": there
is no evidence of fabrication or sloppy citation. But the issue conflates
"the evidence is preserved" (true, and low-risk) with "the preconditions
are executed" (false, and the highest-severity item in the issue's own
table), offers no acceptance criteria that would catch that gap, and sits
at the root of a large, untested citation graph that is already treating
self-admittedly unreconciled numbers (`k`, `α`) as decision inputs. That
combination is exactly what "sound-with-concerns" is for: proceed with the
rescue, but do not let it be closed until the §1.2 action items produce
actual artifacts, and flag the downstream kill-criteria issues as
provisional pending #494 §6.1/§6.2.
