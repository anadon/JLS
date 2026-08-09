# Issue #485: Maintainer decision record: D1-D16, binding on all planning and implementation work
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

This is not a work item — it is an 800+ line governance document rescued verbatim from a
doomed branch (`claude/jls-virtual-hardware-linux-njsoma`) into a GitHub issue, self-described
as carrying "no acceptance criteria." Spot-checking its citations against HEAD (`master`,
current at review time) shows it is unusually well-grounded for a document of this size:
every `docs/*.md` path it cites exists, the `FileAbstractor`/`Element.java` line anchors and
quoted comments match verbatim, and the D8 "orchestrate external tools, never reimplement"
quote is verified at `docs/grand-architecture.md:58`. That said, the format and process
around it — an unbounded, permanently-open reference issue asserted as binding — creates its
own risks, and one of the risks it explicitly warns about is independently confirmed to be
live right now.

## Findings, most severe first

### 1. The defect D6 warns about losing is *already* unfixed on master, and this issue does not escalate it as a bug

D6 says two fixes exist only on the dying branch: `970db41` (register `RegisterFile`/
`FieldExtend` in the frozen `SaveTags` table) and `36cbd37` (advance the creation counter past
stable ids in use). Verified directly against this checkout:

- `git merge-base --is-ancestor 970db41 origin/master` → **not an ancestor**. The commit is
  real (`970db41 fix(format): register RegisterFile and FieldExtend in the frozen tag table`,
  reachable only via the doomed branch) but has not landed on `master`.
- `src/jls/elem/RegisterFile.java` and `src/jls/elem/FieldExtend.java` exist as concrete
  element classes, but `grep -in register src/jls/elem/SaveTags.java` finds only `Register`
  and `ShiftRegister` — **`RegisterFile` and `FieldExtend` are absent from the tag table
  today**, matching D6's description of the bug exactly.
- `test/jls/elem/SaveTagsTest.java` has no totality check against the `jls.elem` class list
  (only integrity checks over tags already in the table), confirming D6's own claim that "the
  missing registry→SaveTags totality test" is why this wasn't caught.

So this is not a hypothetical loss-on-branch-deletion risk — it is a **currently live,
CI-invisible correctness defect in this element's persistence** (two element types added by
`38a0544` cannot round-trip through their canonical save tag), and the only fix for it lives
on a branch this very issue says "will not be merged and will be deleted." The issue records
this as item 5 of "Obligations this record leaves live" — a section it explicitly disclaims
as "not this issue's scope to execute" — rather than as a filed, owned bug with reproduction
steps. A reader treating #485 as read-only reference (as instructed) has no next action to
click on for a defect that is real today. **Recommendation:** file a dedicated bug issue for
the `SaveTags` registration gap now, independent of whether/when the branch is deleted, and
link it from D6/obligation-5 instead of leaving it as prose.

### 2. D6's own premise ("the programme waits on #77") is already stale at time of filing

D6 states "Everything else in this programme sequences behind #77." Issue #77 ("Extract a
headless `jls.core`") is **closed, `state_reason: completed`, closed 2026-07-25T03:47:09Z** —
nine days before #485 was filed (2026-08-03T18:52:38Z). The issue never says the gate has
already opened; a reader relying on D6 as current binding guidance has to independently check
#77's status to learn the blocker it names is gone. For a document whose entire purpose is to
be the *complete* surviving record so nothing has to be reconstructed from context, leaving
the reader to discover this is a real gap. **Recommendation:** add one sentence to D6 noting
#77 closed 2026-07-25 and the sequencing gate is cleared.

### 3. The rescue vehicle has none of the durability properties the rescued content demands

D2 makes diff stability "a first-class requirement," D12 requires citations to survive via
commit-pin or landmark rather than a link into a dying branch, and the issue's whole reason
for existing is that the original file "lived in exactly one file... on the working branch...
[which] will not be merged and will be deleted." Yet the rescue target is a GitHub issue body
— not a merged doc under `docs/`, not a PR, not anything tracked in `git log`. It has no diff
history, no review, is editable/deletable by anyone with issue-edit rights, and is not present
in any local clone of the repository (confirmed: no `docs/plan/**` on `master`, and this
content exists nowhere in the checked-out tree). The document that most insists on
citation permanence and format durability is itself stored in the one place in this project's
toolchain that offers neither. **Recommendation:** land this content as a normative doc under
`docs/` (e.g. `docs/decisions/D1-D16.md`) via a real PR, and keep the issue as a pointer/stub
if a durable home is wanted.

### 4. Elaboration is formatted with the same rhetorical force as verbatim rulings

The issue is careful to say blockquotes are primary and "everything outside a blockquote is
the study's elaboration... and carries less weight," which is a genuinely good hedge. But the
elaboration is written in the same bolded, imperative register as the rulings themselves
("**Required:**", "Engineering consequence that MUST be designed for," "**BUT**... structural
fix," full tables of "verdicts") and is often several times longer than the blockquote it
elaborates (e.g. D1's maintainer quote is three sentences; the surrounding analysis, including
the D1/D15 "interaction" note and the still-open bullet list, runs to several paragraphs). A
future contributor skimming for "what did the maintainer decide" is likely to cite the
elaboration — e.g. the 15.87-bytes/word cap-collision arithmetic, or the sidecar-vs-in-format
table under D1 — as itself maintainer-ratified, when only the one-line ruling actually is.
**Recommendation:** visually separate elaboration from ruling more sharply (e.g. a distinct
"Analysis" heading rather than continuous bolded prose) rather than relying on a single
disclaimer sentence at the top to carry the weight for 800 lines.

### 5. Hard dependency on #484's numbers, and #484 has already undergone one correction pass

The issue states "Several rulings below depend on numbers in it" (#484). #484's own §7 is
titled "CORRECTIONS (supersede sections 2–6)" and revises several of the very figures D1 uses
(e.g. the 15.87 bytes/word density carries through unchanged, but adjacent load-bearing
numbers like events/s, ~600 vs ~580 elements, and the live-console cost were all revised once
already, within the same rescue effort). Given #484 has already been wrong once and corrected
in place, and #485 has no mechanism (no test, no linkage beyond prose) to notice if #484
changes again, D1's "Engineering consequence that MUST be designed for" table is one #484 edit
away from resting on a stale number with no signal to #485's readers. **Recommendation:** if
#484 is revised again, #485 should get a dated addendum noting which D-ruling's supporting
arithmetic moved.

### 6. D8's licensing claim is stated with more confidence than the underlying law supports

D8 says GPL-3.0-or-later "can **ABSORB** most open-source EDA code outright: ngspice (BSD),
Yosys (ISC), Verilator (LGPL-3 / Artistic-2), KLayout (GPL-2-or-later)," and frames this as the
enabler for "actual code reuse and porting, not merely reading for reference." This is
directionally right (LGPLv3 §4 does permit conveying under plain GPL terms; GPL-2-or-later is
upgrade-compatible with GPL-3) but "absorb... outright" glosses over real per-file mechanics:
Verilator is dual LGPL-3/Artistic-2 and porting substantial chunks of it into JLS still
requires per-contribution attribution tracking and picking a consistent path through the dual
license, not a blanket "it's compatible, go ahead." The mitigating sentence ("Any absorbed
code must carry its attribution and licence notice") is present but under-specifies what that
means in practice (NOTICE file? per-file header? SBOM entry — note the repo already ships a
CycloneDX `bom.json` per README, which absorbed code would need to appear in). Given this
ruling is cited as removing "the last objection" to reimplementation-by-porting, it deserves a
sharper licensing-compliance path, not a one-line caveat. **Recommendation:** before any
porting happens under D8, get a per-dependency compliance checklist (attribution mechanism,
SBOM entry, file-header policy) written down — this is exactly the kind of concrete follow-up
D10 demands ("a path and a cost") that D8 itself doesn't fully supply for its own licensing
claim.

## What's solid (brief)

- Every `docs/*.md` file D0 anchors to (`ARCHITECTURE.md`, `grand-architecture.md`,
  `simulation-semantics.md`, `batch-interface.md`, `extension-points.md`, `file-format.md`,
  `reproducibility.md`) exists at HEAD, and the ones checked in depth (D1, D2, D8) quote their
  targets accurately.
- D1's `FileAbstractor` claims (`Container` enum with `XZ`/`PLAIN_TEXT`, the two
  `FileAbstractorTest` method names, `MAX_CIRCUIT_TEXT_BYTES` at line 65) all verify
  byte-for-byte against `src/jls/FileAbstractor.java`.
- D2's dense-id claim ("reassigned on every save") matches `Element.java:21-22` verbatim,
  including the inline comment.
- D11's #59 disposition matches the live GitHub state exactly (closed,
  `state_reason: not_planned`, closed 2026-08-03).
- D10's self-flagged tension with D7 (demand gates legitimate against third parties, forbidden
  against the maintainer's own roadmap) is handled honestly — the issue names the tension and
  resolves it rather than leaving it silently contradictory.
- The commit-pin discipline D12 demands is actually followed for `src/`/`test/` citations
  (`2d0ca9d`), and that commit really exists in this repo's history.

## Verdict rationale

Sound as a record of what was said — the verification sample found no fabricated quotes or
broken citations. "sound-with-concerns" rather than "sound" because: (a) it surfaces a live,
unfixed correctness bug and treats escalation of it as someone else's problem, (b) its own
storage medium contradicts the durability principles (D2, D12) it imposes on everyone else,
and (c) at least one directive (D6's #77 gate) is already stale on arrival. None of these
invalidate the rulings themselves, but a document whose stated purpose is to be the complete,
reliable, single surviving source should not itself introduce new staleness and untracked risk
on day one.
