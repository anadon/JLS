# Issue #485: Maintainer decision record: D1-D16, binding on all planning and implementation work
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its sixteen sections, #485 is a **durability play**. A branch that will never
merge holds the only copy of rulings that ~200 filed issues cite as binding, so the content
is hand-copied into a GitHub issue before the ref disappears. The instinct is right and the
transcription is unusually careful (verbatim blockquotes ranked above elaboration; dead
paths marked dead; the live obligations enumerated at the end).

The vessel is wrong, and it is wrong by the record's own rules.

A GitHub issue body is mutable, unversioned, invisible to `grep` in a clone, absent from the
offline jar culture this project treats as load-bearing, and **structurally incapable of
satisfying D12** — you cannot pin a citation to a commit of an issue body, and there is no
landmark-relative anchor for text that lives outside the tree. D0 directive 2 says to anchor
to HEAD source and the normative docs; #485 asks every future reader to anchor to a web page
instead. The record rescues its content from one medium the project says is not authority
into a second medium the project says is not authority.

## Reframing 1 — the one that makes most of the problem disappear: archive the ref

The premise of both #484 and #485 is stated flatly: *"a link into it preserves nothing."*
That is true of a **deleted** ref and false of an **archived** one. Not merging a branch and
not keeping its commits reachable are independent decisions that have been silently fused.

```
git tag archive/virtual-hardware-study-2026-08 <branch-head> && git push origin --tags
```

One command, permanent, and it buys everything the transcription cannot:

- `docs/plan/evidence/BRIEF.md` and its whole backing corpus (`00-fact-base.md`,
  `02-element-count-determination.md`, `07-mvl-determination.md`, `08-views-determination.md`,
  the `recon-*`/`art-*`/`c2-*` reports) stay readable and **commit-pinnable** — D12's
  "strongest form" starts working again instead of being apologized for in a footnote.
- The two fixes stranded in D6 (`970db41` unregistered `RegisterFile`/`FieldExtend` save
  tags; `36cbd37` creation counter colliding with stable ids) become `git cherry-pick`
  targets forever, rather than defects to be **re-fixed from a prose description**. That
  obligation, as written, is a guaranteed regression with a countdown on it.
- D14's caveat evaporates: CAP-18's source content for FEAT-058/059/060 is still there
  whether or not anyone writes them this week.
- #484 §12's "sections judged not worth rescuing" stops being a permanent loss. Someone
  decided, under time pressure, which measurements a future reader would never want. A tag
  costs nothing and un-makes that decision.

Nothing about an archive tag implies the work merges, ships, or acquires status. It is
bookkeeping, and D11 is on record that bookkeeping is not worth a decision cycle. This is
the reframing I would take first, because it converts a lossy manual rescue into a
zero-effort one.

## Reframing 2 — put the surviving rulings where the project already keeps rulings

ARCHITECTURE.md already has exactly the artifact #485 needs: a **"Recorded decisions"**
section, in-house form, one heading per decision, each carrying rationale and an explicit
revisit trigger, already holding #80, #153, #221, #222, #223 and the i18n non-goal. That
section is the project's constitution and it lives in the tree. `docs/capability-roadmap/
AMENDMENT.md` is the second precedent: an in-tree amendment that supersedes named sections
of an in-tree parent without replacing it.

Sixteen rulings do not belong in one document, though, because they are four different
kinds of thing and each kind has a different natural home:

| Kind | Rulings | Home |
|---|---|---|
| Format/design requirements | D1, D2, D3, D15 | `docs/file-format.md` (a "direction" section) |
| Architectural stances | D4, D7, D8, D9 | ARCHITECTURE.md *Recorded decisions* / `grand-architecture.md` |
| Process rules for planners | D10, D12, D13, D16 | CONTRIBUTING.md — read *before* work, not when following a citation |
| Live work with a deadline | D6, D11, D14, obligations 1–5 | Filed issues with acceptance criteria |

D10 is the highest-value ruling in the set and the worst served by its current placement.
It governs the reasoning of every future contributor and agent; parked in an issue body it
is read only by someone who already followed a citation to it. In CONTRIBUTING.md it is
read by everyone before they write a determination. That relocation alone is worth more
than the rest of the rescue.

## Reframing 3 — a revocation that leaves the revoked text standing is not a revocation

This is where the "reference document, no work to be done" framing does real damage. I
checked the tree at HEAD: **the documents D8 names as carriers of the revoked stance are on
`master`, not on the dying branch.**

- `docs/grand-architecture.md:58` — *"Settled stance: orchestrate external tools, never
  reimplement HDL semantics."*
- `docs/standards-landscape.md:836`, `docs/capability-roadmap/sweep-04-verification.md:156`,
  `docs/capability-roadmap/lf-05-fault-and-power.md:332`.

D8 declares *"it binds nothing."* The tree still says it is settled. The next agent, obeying
D0 directive 2 and anchoring to the normative docs, will re-derive the revoked stance from
primary sources and will be correct to do so. The same holds for D1: README.md still says
*"saves stay XZ-compressed unless you opt in"* and `docs/file-format.md:56-58` still states
XZ as the conformant default. And D16's own rule — *fix the document* — applies to these
documents word for word. **Under its own D16, this issue owes four one-line edits, and they
are the only part of the record that would still be true after everyone forgets the issue
number.**

There is also a conflict D5 does not see. ARCHITECTURE.md's sole recorded simulation-strategy
decision (#221) has exactly one revisit trigger: *"a concrete CPU-scale design on the
`riscv/` trajectory (#200/#201/#202) that is unusably slow interactively."* #484 §9 says that
trigger is now quantitatively met. Deleting `riscv/` under D5 deletes the only named evidence
route for the only trigger on the only strategy decision in the file. D5 is still right — the
Python builders must not be a deliverable mechanism — but its consequence is a required edit
to ARCHITECTURE.md's #221 trigger, naming the "first-class, in-tree, tested JLS mechanism"
D0.1 demands in `riscv/`'s place. Record the ruling anywhere you like; the trigger has to be
rewritten in the tree or #221 silently loses its escape hatch.

## The uncomfortable observation about the whole arc

Two of the sixteen rulings are the maintainer asking for **less apparatus**: D13 (*"I don't
think that these require such fuss. Just make something that works."*) and D11 (*"It really
doesn't matter what so long as the ultimate outcome is good."*). D16 and D14 are both "this
is mechanical, stop asking." Four of sixteen rulings, and arguably D10 as a fifth, are the
project owner pushing back on a planning machine that had begun consuming its own output.

Faithfully transcribing that pushback into a sixteen-section constitution with a precedence
order, a verbatim-vs-elaboration weighting rule, and a table of contents is — gently — the
behaviour the pushback was aimed at. The issue concedes as much: everything outside a
blockquote *"carries less weight."* Take that seriously and the durable artifact is much
smaller than what was filed:

1. **The blockquotes.** Nine short maintainer quotes. They are the primary source, they fit
   on one screen, and they are the only part with unambiguous authority.
2. **The obligations**, as filed issues with owners and criteria — not as a closing list in a
   document that explicitly disclaims being work.
3. **The four textual corrections** D8/D16 imply against surviving `master` documents.

Everything else is the study's own elaboration of the maintainer's words: valuable while the
study ran, and precisely the corpus overhead D13 and D16 were issued to stop generating.

## Verdict and what I would actually do

**endorse-with-reframing.** The rescue is legitimate and the transcription is honest work;
I am not asking for it to be discarded. I am asking for the medium and the granularity to
change, in this order:

1. Push an archive tag on the branch before it is deleted. Highest value, near-zero cost,
   recovers the two stranded fixes and everything #484 §12 abandoned.
2. Land D0–D16 in the tree — CONTRIBUTING.md for the process rules, ARCHITECTURE.md's
   existing *Recorded decisions* section for the stances, `docs/file-format.md` for the
   format rulings — and reduce #485 to a pointer at those, or close it.
3. Execute D8's and D1's implied corrections against the surviving normative documents, and
   rewrite #221's revisit trigger before `riscv/` goes.
4. File the five obligations. A deadline that races a branch deletion cannot live in a
   document whose header says nothing here is work to be done.

The issue has no acceptance criteria to disregard; I have read it as a claim about where
this project's governance memory should live, and on that claim the answer is: in the
repository, pinned to a commit, where every rule in D0, D10 and D12 says authority belongs.
