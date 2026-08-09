# Issue #498: Virtual-hardware / virtual-logic parity, part 3 of 3: recorded decisions, the exclusion set, kill criteria K1-K9, and milestones M1-M9
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This issue is not a proposal; it is a verbatim "rescue" of §7–§10 of a 124 KB
design document that lived only on a branch (`claude/jls-virtual-hardware-linux-njsoma`
at `36cbd37`) that "will not be merged." Its own header says it is
**"explicitly non-normative"** and that **"nothing in it may be cited as
settled policy."** The body then spends four sections issuing numbered kill
criteria and milestones in unmistakably normative language. That tension is
the central defect, and everything below flows from it.

## Findings, most severe first

**1. The issue disclaims normativity, then writes normative gates anyway — and admits the keystone question is unresolved.**
Quoted: "It is explicitly non-normative... Nothing in it may be cited as
settled policy." Yet: "K9 outranks everything above it," "the consequence is
taken, not argued with" (§9 preamble), "Nothing downstream of L4 may merge
until the null test fails on demand" (K4), and §7.8 states outright: "the
keystone contradiction, unresolved... **It must be resolved by the
maintainer before M3**... neither of the documents that assert opposite
answers noticed the other." A document that concedes its own central design
question is unresolved should not simultaneously hand out consequences like
"the Linux target is abandoned" (K5) or "cut the full structural boot claim"
(K2) as if they were adjudicated. Recommendation: before any of K1–K9/M1–M9
is treated as actionable, the maintainer must explicitly ratify (or amend)
them in a real `ARCHITECTURE.md` decision block — the issue itself, being
non-normative and a rescue of a never-merged branch, cannot be that
ratification.

**2. Feasibility/cost: the programme is ~3–5 maintainer-years bolted onto a single-maintainer pedagogy tool.**
§10's own honest total: "roughly 155–250 maintainer-weeks — three to five
maintainer-years at bus factor 1." For comparison, the entire post-audit
correctness-and-ship programme (#33, 28 sub-issues, 26 completed) ran
2026-07-08 → 2026-07-27 — about three weeks. This issue proposes a
programme roughly 50–80× larger than the largest programme JLS has actually
executed, for a "flagship demo" whose own K9 says the target audience (a
first-year student drawing an adder) is not the one who benefits.
Recommendation: any go-ahead needs an explicit maintainer sizing decision,
not implicit adoption via three inlined GitHub issues.

**3. Single point of failure for ~17 other open issues.** The issue states
plainly: "Issues citing this document, by name or by line number, include
#379... #407... #392 and #459... #456... #477 and #478... #458... #479 and
#308... #425... #482 and #331... #495... and #493/#494/#496. **Each of
those carries acceptance or kill criteria whose numbers come from here.**"
A document that is (a) frozen at a single branch commit, (b) explicitly
non-normative, and (c) self-admittedly internally contradictory at its
keystone (§7.8) is now the numeric foundation for at least 17 other filed
issues' acceptance criteria. Any correction here (and finding 6 below shows
at least one is needed) cascades into all of them, with no mechanism in the
issue for propagating that correction. Recommendation: treat #498 as
provisional/historical until ratified, and do not let dependent issues cite
its K/M numbers as fixed until it is.

**4. The "rescue" was already stale when filed, and nothing prevents further drift.** §7.7 corrects two "process facts" — that #77 and #33 are
closed — which I verified: #77 closed 2026-07-25 ("completed"), #33 closed
2026-07-27 ("completed," `sub_issues_summary` shows **28 total, 26
completed**). The issue text itself says "**28 sub-issues, 25 completed**"
— a small but real mismatch with GitHub's current count. This is exactly
the failure mode the "rescue" was supposed to fix (branch content going
stale against `master`) recurring one layer up: the issue is itself already
slightly wrong about the state it was correcting, filed 2026-08-03, six
days after #33 closed. Nothing in #498 proposes an owner or a cadence for
re-verifying its own numbers as `master` continues to move.

**5. Gameable acceptance test for the highest-ranked kill criterion (K9).**
K9 is stated as: "**Any regression to the first-year student drawing an
adder stops the responsible layer, regardless of what it costs the
flagship.**" But its own prescribed enforcement is admittedly minimal: "one
headless assertion on palette row count... plus one timing test with a
**generous band** on the existing 10k-element path." A generous-band timing
test lets small per-PR regressions (well inside the band) accumulate across
a multi-year, multi-milestone programme while never once failing the
ratchet, directly defeating the "any regression... stops" language two
paragraphs earlier. A palette *row-count* ratchet also only catches added
palette entries, not other pedagogy regressions (dialog complexity, new
required clicks, GUI startup time drift) that K9's own prose claims to
guard. Recommendation: either narrow K9's prose to match what the ratchet
actually checks, or specify a tightening (not generous) band tied to a
measured baseline with a documented revisit trigger, per this repo's own
`CONTRIBUTING.md` convention (cited in #497's governance band section).

**6. §7.5's diff-stability claim is inconsistent with code already at HEAD, and with the companion issue's own more careful phrasing.**
§7.5 (D2) frames "referencing by stable id" as *future* structural work:
"`Circuit.save` reassigns dense file-local ids on every save
(`src/jls/Circuit.java:1499-1503`)... Referencing by stable id is the
structural fix." I verified `src/jls/Circuit.java`: elements are already
sorted `.thenComparing(Element::getStableId())` (line 1496) *before* the
dense per-file ids are assigned (line 1501), and `Element.java:24` already
mints a "permanent identity" stable id (`ElementId.mintFresh()`, tied to
issue #165) on every element. This is precisely the fix §7.5 describes as
outstanding, and §7's own preamble lists #165/#166/#167 as decisions the
architecture "respects" — i.e., already landed. The companion document,
issue #497 (part 2, gap-list item 20), is more careful: "Stable-id minting
can collide... **Partly fixed at HEAD by `36cbd37`**; the persisted
per-install counter and the save-time uniqueness assertion are still
absent." Part 3 (#498) does not carry that nuance forward, so a reader of
#498 in isolation will overstate how much of D2 remains undone.
Recommendation: cross-reference #497 gap-list item 20 from #498 §7.5 so the
two parts agree on what's actually still missing (the persisted per-install
counter and save-time uniqueness assertion — not stable-id minting itself).

**7. K8's acceptance criterion is a self-timed, unwitnessed claim with no CI enforcement.** "If the pinned kernel and initramfs cannot be rebuilt from
their documented recipe **by the maintainer alone in under 2 hours**... the
Linux target is demoted." There is no proposed mechanism (timestamped CI
job, recorded log) to make this checkable by anyone other than the single
maintainer self-attesting after the fact — for the "existential risk"
criterion of a programme costing multiple maintainer-years, this is thin.
Recommendation: at minimum, log wall-clock start/end in the CHANGELOG entry
this criterion already implies elsewhere in the document, so the 2-hour
claim is auditable later rather than trusted.

## What's solid

- The file:line citations I could check are accurate at HEAD:
  `FileAbstractor.java:65,197` (`MAX_CIRCUIT_TEXT_BYTES`, XZ default),
  `Circuit.java`'s stable-id sort and dense-id assignment, and
  `ARCHITECTURE.md`'s `riscv/`-trajectory revisit-trigger wording (lines
  354–355) all match the issue's quotes. The document's forensic grounding
  is trustworthy where verifiable, which is unusual for a document this
  size.
- §7.2's correction of `vcd-interop.md` against #63 is accurate: I fetched
  #63 and confirmed it is open, milestoned "M3 — Consumer modules," and
  plans exactly the external GHDL/Icarus co-simulation the informative doc
  calls "rejected." The proposed replacement text ("the recording, not the
  session, is the contract") is a clean, minimal fix that doesn't touch the
  normative `batch-interface.md`.
- §7.7's #77/#33-closed correction is directionally right (see finding 4 for
  the one-off count mismatch) and is a useful, cheap drive-by fix to
  `CONTRIBUTING.md`.
- K4 (the null test) is well-specified and not obviously gameable: a
  deliberately-wrong fidelity binding either gets rejected by the harness or
  it doesn't — no room for a partial pass.
- §8's exclusion list is honest about its own internal tension (item 2 flags
  that P15's checkpoint requirement partly contradicts the "no
  checkpoint/restore" exclusion, and item 8 does the same for the guest
  image vs. "no committed multi-MB artifacts") rather than hiding it —
  that kind of self-flagging is exactly what an adversarial reviewer wants
  to see, even though the contradictions themselves are unresolved.

## Recommendation

Do not let this issue (or its siblings #493–#497) be treated as ratified
scope. Before any milestone work is scheduled against it: (a) the
maintainer formally resolves §7.8's keystone contradiction and §7.1's L1
exception, since M3 is explicitly blocked on both; (b) K9's acceptance test
is tightened or its prose scoped down to match; (c) #498 is reconciled with
#497's more precise gap-list wording on stable-id status; and (d) the ~17
dependent issues are told their cited K/M numbers are provisional until (a)
happens.
