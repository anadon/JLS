# Issue #495: Rescued from a deleted branch: docs/parity-contract.md in full — the proposed (UNRATIFIED) parity contract defining what "virtual logic and virtual hardware agree" means
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Stripped of its 940-line payload, #495 is a **storage decision**: a document that ~25 filed
issues dereference lives only on a branch that will be deleted, so the text is copied into an
issue body. The preservation instinct is correct and is the maintainer's own — #485's D12 note
says in terms that "for anything on a branch that will not be merged, the only preservation is
inlining the content." Judged as an act, this is right. Judged as a *destination*, it is the
one place the project has already decided contracts do not live.

`ARCHITECTURE.md` ("Help delivery" decision) states it plainly: *"Repo documents (`README`,
`docs/*.md`, this file) are already web-readable on GitHub and are the normative home for
contracts."* #495 takes a document written in the register of `simulation-semantics.md` and
`batch-interface.md`, and homes it in a container with no diff review, no `grep` from a
checkout, no offline availability inside the jar's repo, no line stability, no CI, and no way
for a test to assert anything about it. The issue then documents its own defeat: the "defect
noticed while rescuing" section concedes that whoever executes #423 "must first re-home the
text into `docs/parity-contract.md` on `master`, or the criterion is unsatisfiable." The
endgame is a file. This issue is a detour to it.

## Reframing 1 — the rescue is a commit, not an issue

Nothing about the branch's deletion requires the document to leave the repository. The
argument against landing it on `master` is presumably that an unratified normative-shaped
document on `master` reads as adopted. That argument does not survive contact with the tree:
`docs/capability-roadmap/**` (19 files), `docs/standards-adoption/**` (12 files),
`docs/grand-architecture.md`, `docs/hdl-support-research.md`,
`docs/flatlaf-evaluation-2026-07.md`, `docs/collaborative-editing-research.md` are all
explicitly non-normative study material already tracked on `master`. There is a well-worn
in-tree home for exactly this class of document. And the contract is *unusually* safe to land:
its first paragraph is a self-executing non-ratification clause — it binds nothing until an
`ARCHITECTURE.md` decision block exists, and #495 itself takes pains to say so.

Landing `docs/proposals/parity-contract.md` on `master` with the status paragraph intact costs
one commit and buys: live relative links to `simulation-semantics.md`, `batch-interface.md`,
`file-format.md`, `reproducibility.md` (four of the six siblings are already tracked — only
`machine-calibration.md` and `virtual-hardware-parity.md` are branch-only, and those are
themselves rescued in #494/#496 and #497/#498/#499); stable line anchors, which makes the
concordance table unnecessary; `grep` reachability for the next contributor; and a
ratification act that becomes a one-line diff instead of a re-homing project. It also fixes
the cluster problem at once: six rescue issues (#485, #494–#499) currently constitute a
parallel architecture corpus that lives outside the artifact it governs.

## Reframing 2 — the contract's durable form is a type and four tests

This is the reframing I would actually pursue, and the document argues for it better than I
can. §3.1: *"The record is a Java `record` with no field for cycles, simulated time, pipeline
state, or cache state. **This is the enforcement mechanism, not a stylistic preference**: §4's
permitted-to-differ set is made unrepresentable by the type, so over-constraining parity is a
compile error rather than a code review finding."* §2.5 rule 1 makes `E` a ratcheted test.
§5.2 makes the "not a second execution strategy" claim a reflective queue guard — *"a fact
rather than a claim, and it is what §8 rests on."* §5.3 makes non-vacuity a committed
knowingly-wrong implementation whose test asserts failure.

Four of the contract's five load-bearing commitments are already specified as *code*. JLS's
whole culture is that boundaries are enforced rather than written down —
`HeadlessCoreRatchetTest`, `NotificationRatchetTest`, `ElementConstructorContractTest`,
`SaveTagsTest`, `ExtensionPointCatalogTest`, `HelpTopicsTest`'s completeness check;
`grand-architecture.md` §10 names this as the reason the architecture is holdable by one
maintainer. A 940-line prose contract for a mechanism that does not exist is the exact inverse
of that culture, and its fragility just proved itself by dying with a branch.

Three of the four are writable **today, against nothing**, with no simulator, no boundary
mechanism, and no #77 dependency:

- `RetireRecord` — a 12-field record, RVFI-named, with no time field. Pure leaf type; this is
  already the shape #477 (TASK-0070) asks for.
- The exclusion set — a four-member enum plus the both-directions ratchet test. `E` is
  `{mcycle, minstret, mtime, mtimecmp}`; the test is a dozen lines.
- The null-test discipline — a `Differ` over two `RetireRecord` lists plus a family of subtly
  wrong record streams it must reject, asserting *report text*, not a boolean.

That is a small, self-contained, testable package that cannot be lost with a branch, cannot
rot into unratified prose, and makes the prose its javadoc rather than its substance. It is
also strictly compatible with #495's stated goal: the citing issues would then dereference a
type and a test, which are harder anchors than either a file path or an issue number.

## Reframing 3 — the concordance table treats the symptom

The "line-anchor concordance" exists because ~10 issues cite this document by bare line range.
#485's D12 already ruled: *"A bare `file:line` with no commit and no landmark is not a citation
and does not satisfy the evidence rule."* The concordance makes those citations resolve **one
more time** and thereby preserves the habit that caused the problem. The cheaper, terminal fix
is to normalize the citing issues once to landmark anchors (`§2.5`, `§5.3`, `§8.3`) — after
which the document's location genuinely stops mattering, and the *next* relocation (which #495
itself says is coming, via #423) costs nothing. Doing that first would have made most of this
issue unnecessary.

## The sequencing this rescue inverts

The document names four experiments, each of which it describes as roughly an afternoon, and
each of which could invalidate large parts of it: cross-platform/JDK run determinism (§5.1,
§9.8 OQ4 — *"if it is not, that finding outranks most of this document"*); the §3.3 settling
experiment (two clock rates on a reference emulator, byte-identical console or the parity
clock does not work); `InteractiveSimulator` per-event cost (§9.7 — every interactive figure
in the corpus is measured on the wrong simulator); the reset-fiction question (§9.4). Roughly
one maintainer-day of measurement gates a document whose preservation has consumed a
1,100-line issue. If the corpus is worth rescuing, those four results are worth more than the
prose that depends on them, and they land as tests on `master` rather than as issue bodies.

I am not disregarding the acceptance criteria here — #495 has none; it is a reference issue —
but I am explicitly disregarding its framing that the choice was "inline or lose it." The real
choice was "inline into an issue or commit to `master`," and the second dominates on every
axis the project itself has recorded.

## Two master-side facts buried at the bottom, both verified in-tree

These are the only parts of the payload that describe defects in shipped code, and they are at
lines ~145 and ~888 of an 1,103-line issue nobody will read to the end of.

1. **The normative delay table is incomplete.** `docs/simulation-semantics.md` §7 lists neither
   `RegisterFile` nor `FieldExtend` in either the delayed table or the zero-delay row, while
   both ship an editable, saved `delay` attribute with no simulated effect —
   `src/jls/elem/RegisterFile.java:559` propagates at `now`. Confirmed at HEAD. Anyone deriving
   a zero-delay closure from that table derives a wrong one. This is a two-line documentation
   fix with no relationship to the virtual-hardware programme, and it deserves its own issue.
   (#488 covers the adjacent `SaveTags` half; this half appears unhomed.)
2. **`HdlExporter.EXPORTED` is 22 classes and includes neither** (`src/jls/hdl/HdlExporter.java:422-428`,
   confirmed). So no drawn machine built on `RegisterFile` round-trips to Verilog today — a
   claim worth stating in the HDL docs rather than in a rescued contract's §6.5.

The `RiscvCpuGoldenTest` regeneration-path rot (`{@code}` spans, doclint-silent) is real and is
already homed in #413.

## Verdict

**endorse-with-reframing.** Preserve the text — that judgment is sound and the alternative
loses a genuinely good document; §2.4's retirement-indexed input log and §7's
"event-accurate versus settled-value" reframing are the two best ideas in the whole corpus and
neither should die on a branch. But land it as `docs/proposals/parity-contract.md` on `master`
with its unratified status paragraph intact, close this issue pointing at the file, normalize
the citing issues to landmark anchors, and then convert the contract's four enforceable clauses
into `RetireRecord`, the `E` ratchet, the queue guard and the null-test family — the form in
which a contract in this project is actually held.
