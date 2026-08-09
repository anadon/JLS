# Issue #787: TASK-C570-1: each Digital-wishlist item gets its D10 path-and-cost justification in writing before any of it is implemented
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#787 is a small (`band_mw: 0.5`), no-code gate task under feature #570:
before AC-2 (mid-simulation subcircuit dive) or AC-3 (rebindable keys) of
#570 gets implemented, each must have a written "D10 path-and-cost
justification" so wishlist items that would ship only to copy a
competitor (Digital) are cut rather than built. The gating intent is
sound and traces correctly to a real kill criterion (KC-30-1, #514). But
the criteria that are supposed to make the gate "a deliverable rather
than an intention" (the issue's own words) are themselves unfalsifiable
or point at a dependency that does not yet exist, which undermines the
issue's central claim.

## Findings, most severe first

### 1. "D10" is undefined anywhere in the repository
AC-1 requires "a recorded **D10** path-and-cost justification," and the
Outcome repeats the term twice. A repo-wide search (README.md,
ARCHITECTURE.md, every file under `docs/`, every other open issue
fetched for this review — #570, #289, #592, #514) turns up zero
definitions of "D10" as a methodology, template, or rubric; the only
places the string appears are other reviewers' issue-review output
files (i.e., artifacts of this same review fleet, not source material).
Contrast #592, which defines its own rubric inline ("graded HAVE / GAP /
REFUSE with a reason... Rows carry a funding score"). Without a
definition, AC-1 cannot be verified or falsified: any paragraph
asserting JLS-side benefit trivially "is" a D10 justification because no
required sections, evidence bar, or reviewer sign-off is specified
anywhere.
**Recommendation:** either link/define the D10 template (if it exists in
a maintainer's private process, put a stub in `docs/`) or replace "D10"
with an explicit, spelled-out list of required fields (path, cost,
beneficiary, alternative-considered) inline in this issue.

### 2. AC-4 requires reconciling against a catalog that does not exist yet, while the front matter declares no ordering dependency
AC-4: "Ownership overlap with CAP-37's parity catalog (#592) is
reconciled here before task work starts: an item scored in that catalog
is cross-referenced, never funded twice." But #592 is itself an open,
unimplemented feature whose entire outcome *is* that catalog: "A reader
opens `docs/` and finds a scored inventory... Nothing in PF-2..5 is
funded until it has a row here and a score; this catalog is the gate."
`docs/` currently contains no such file (confirmed via directory
listing: `architecture-project-setup.md`, `batch-interface.md`, ...,
`vcd-interop.md`, `windows-msi-determinism.md` — no ergonomics/parity
catalog). There is nothing to cross-reference against yet. Meanwhile
#787's own YAML front matter declares `ordering_after: []` — i.e., no
prerequisite issues — which directly contradicts AC-4's implicit
dependency on #592 landing first. Note #592's own boundary note actually
resolves the ownership question in #787's favor already ("the
Digital-wishlist headline items... are owned by #570 and are catalogued
here only as HAVE-elsewhere cross-references, never re-funded") — so
AC-4 as worded asks for a reconciliation step against an artifact that
(a) doesn't exist and (b) has already stated, in the sibling issue's own
text, that it defers to #570. AC-4 should either be dropped as already
satisfied by #592's text, or reworded to state the real dependency and
add #592 to `ordering_after`.
**Recommendation:** add `#592` (or "#592's catalog, once published") to
`ordering_after`, or rewrite AC-4 to note it is pre-satisfied by #592's
own boundary note and needs only a citation, not new reconciliation work.

### 3. The cost side of "path-and-cost" is being priced against acceptance criteria that are themselves unstable
#787 gates implementation of #570 AC-2/AC-3. An independent review of
#570 (issue-reviews/issue-0570.adversarial.md, same batch) found: AC-2
states no read/write semantics, no concurrency contract against the
documented EDT/Runner-thread discipline in ARCHITECTURE.md, and no
nesting/multi-instance cardinality; AC-3 assumes a rebinding path through
`MenuAcceleratorPolicy` (`src/jls/MenuAcceleratorPolicy.java`), which is
implemented as a stateless pure function of `os.name` with no per-user
override hook — meaning AC-3 as scoped requires a structural redesign of
that class, not an additive feature. A "cost" justification written
against acceptance criteria this underspecified cannot be a real cost
estimate — the actual engineering cost of AC-2/AC-3 is unknown until
#570 states concurrency/read-write/redesign scope. #787 does not require
flagging this uncertainty; a false-precision cost figure would pass AC-1
as written.
**Recommendation:** make #787 order after (or explicitly contingent on)
#570 having a stated concurrency contract and a stated
`MenuAcceleratorPolicy` redesign scope, or require the cost writeup to
state its own confidence/assumptions where #570 is silent.

### 4. No specified artifact location or format for the "recording"
AC-1 says "a recorded justification exists," AC-2 says "recorded as
cut," AC-3 refers to "the dark-mode row." None of these say *where*:
a new `docs/` file, a comment on #570, a comment on #787 itself, or a
section added to #570's body. "Row" borrows #592's tabular
catalog vocabulary without #787 ever establishing that its own
deliverable is a table. This matters because the issue's own framing is
that this gate should be "discharged as a deliverable rather than as an
intention" — but a deliverable with no fixed location or format is
exactly the kind of thing that becomes an ephemeral GitHub comment,
un-auditable in a year, defeating the stated purpose.
**Recommendation:** name the artifact explicitly, e.g. "a new
`docs/wishlist-justifications.md` section per item, cross-linked from
#570."

### 5. The kill test ("does not stand on JLS merit") has no reviewer and no objective check
AC-1/AC-2 turn on "who benefits independent of any competitor's
wishlist" and "does not stand on JLS merit is cut" — both are prose
judgment calls with no named reviewer, no second-party sign-off, and no
falsification condition (contrast the harness/test-pinning bar #570's
own sibling issues #593/#594/#596 impose on their acceptance criteria,
per the earlier #570 review). Since the same person filing the
capstone, the feature, and this gate task is also the one who would
write the justification, the gate as worded has no adversarial check
built in — a motivated one-paragraph rationalization satisfies it just
as well as a genuine cost/benefit analysis. This is precisely the
poaching risk KC-30-1 is meant to catch, so the absence of an
independent check is a real weakness, not a nitpick.
**Recommendation:** require the justification to name a counterfactual
("what JLS-side problem does this solve if Digital had never shipped
it?") and/or require a second reviewer's explicit concurrence recorded
alongside the author's writeup.

### 6. Minor — `band_mw: 0.5` is asserted with no basis, and likely underscoped
Two justification writeups, a cut-and-record path, and a cross-reference
reconciliation against #592 (finding 2) is asked to fit in half a
maintainer-week, while comparable process-only siblings in this batch
(#592 itself, the #570 review's own comparison) run substantially longer
and more structured. Given finding 1 (no defined template) the true size
is unknowable, but 0.5 mw reads optimistic if "D10" turns out to mean
anything like the multi-section rigor #289/#570/#592 apply to themselves.
**Recommendation:** revisit the estimate once finding 1 is resolved.

## What's solid

- The dark-mode boundary (AC-3's core claim) is correct and consistent:
  #289 and #570 both independently disclaim dark-mode scope the same
  way, so there is no double-ownership or scope creep on that item.
- The underlying kill criterion this task discharges (KC-30-1) is real
  and accurately quoted/sourced — verified directly against #514's body,
  which does say "PF-5 features must each stand on their own JLS merit
  (D10 path-and-cost, not marketing); any that would ship *only* to poach
  is cut."
- The general instinct — write the justification and the cut record
  *before* code lands, not after — is a reasonable, cheap gate to insert
  ahead of a genuinely speculative feature pair (AC-2/AC-3 of #570).

## Verdict rationale

The gate is worth having, and its provenance (KC-30-1) checks out. But
as filed it leans on an undefined term ("D10") that makes its primary
acceptance criterion unfalsifiable, asks for reconciliation against a
sibling catalog (#592) that does not exist yet while declaring no
ordering dependency on it, and prices "cost" against a parent feature
(#570) whose own acceptance criteria are independently unstable. These
are specification defects fixable by rewording and adding an ordering
edge, not a reason to abandon the gate — hence needs-rework rather than
should-not-proceed.
