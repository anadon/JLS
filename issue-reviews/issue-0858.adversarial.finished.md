# Issue #858: TASK-C581-1: `brew install --cask jls` installs the published dmg by its attested sha256, and the tap-versus-homebrew-cask decision is recorded with its reasons
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

First of three TASK-C581 tasks under FEAT-C34-3 (#581, "brew install
--cask jls"). This task creates the Homebrew cask itself (sha256-pinned
to the published dmg), verifies `brew install --cask jls` once on a
clean macOS machine, records the tap-vs-`homebrew/cask` decision with
its review-cost reasoning, and passes `brew audit --cask`. #859
(caveat-text drift check) and #860 (automatic version/sha256 bump on
release) both order after this one.

## Findings, most severe first

**1. AC-5 cites a section of #82 that does not exist.** AC-5: "The
unsigned-by-choice stance is inherited unchanged from #82 §10 / #128 /
#338." I fetched #82 in full: its body has exactly seven numbered
sections (`## 1. Capability Statement & Scope Boundary` through `## 7.
Re-planning Protocol`), followed by unnumbered `## Open Questions &
Decisions Needed` and `## Completion Criteria`. There is no `§10`
anywhere in #82. The macOS-unsigned content actually lives in §1
("macOS signing: closed won't-fix (#128, #135): the .dmg ships unsigned
by choice, documented") and in the "Adjudication record" paragraph.
This is not unique to #858 — the same phantom `#82 §10` citation also
appears in #443 (twice) and in #581 itself (this task's own parent),
so it is a systemic citation defect in this corpus, not a typo
original to #858. But #858 is the one place it lands inside a binding,
checkable acceptance criterion: an implementer who goes looking for
"§10" to confirm what "inherited unchanged" means will not find it.
Note also that #858's AC-5 drops `#135` (Developer ID signing,
closed not_planned) even though README.md:40, `#82`'s own §1, and
`docs/standards-landscape.md:472` all cite the unsigned decision as
`(#128, #135)` jointly — an unexplained narrowing of the citation set
inherited from the parent chain. **Recommendation:** fix the citation
to `#82 §1 / #128 / #135 / #338` (or wherever the eventual correct
anchor is) before this becomes the text a reader is asked to verify
"unchanged" against.

**2. AC-3's required "review-cost arithmetic" traces to a threshold
(KC-34-1) that does not exist anywhere in the repository.** AC-3: "...
including the review-cost arithmetic that AC-5 of #581 requires."
#581's AC-5 reads: "The per-release cask update-and-review cost is
recorded against KC-34-1's 0.5 mw threshold." I grepped the full tree
for `KC-34`, `CAP-34`, and `CAP-27` (also cited by #581 AC-4) outside
the issue-reviews folder and found zero matches — no ledger file, no
threshold definition, no CAP-34/CAP-27 document. The sibling review of
#860 (issue-reviews/issue-0860.adversarial.md, finding 2) independently
confirms the same threshold and the "shared per-channel maintenance
ledger" it's meant to be recorded against are both undefined, calling
out that a similar sibling task (#857/TASK-C580-3) references "the same
ledger every channel reports to" without it existing either. AC-3 as
written asks this task to produce arithmetic checked against a number
that isn't written down anywhere yet — a verification step that cannot
actually be performed as specified. **Recommendation:** either drop the
"review-cost arithmetic that AC-5 of #581 requires" clause from AC-3
until KC-34-1 is defined by whichever issue owns it, or add that issue
as an explicit dependency here (this task currently declares
`ordering_after: []`).

**3. Neither AC-1 nor AC-2 addresses architecture scoping, and the
underlying dmg is Apple-silicon-only.** README.md:37 is explicit: "macOS:
`JLS-<version>-aarch64.dmg` (Apple silicon) ... Intel Macs: use the jar
below." There is no x86_64 dmg. A Homebrew cask that only has an arm64
artifact must either gate on `depends_on arch: :arm64` (so `brew
install --cask jls` fails cleanly with a clear message on Intel) or the
task is silent on what happens when an Intel user runs the literal
command the issue's own title promises: "`brew install --cask jls`
installs the published dmg." Left unstated, `brew audit --cask` (AC-4)
may or may not catch this depending on how the definition is written,
and "on a clean macOS machine" in AC-2 doesn't specify which
architecture the verification runs against — so AC-2 could be satisfied
by testing only Apple silicon while the Intel path ships broken or
undocumented. **Recommendation:** add an explicit AC (or amend AC-1)
requiring `depends_on arch: :arm64` with a documented, tested failure
message on Intel, and state which architecture AC-2's clean-machine
verification covers.

**4. AC-2's Gatekeeper workaround is presupposed to keep working, but a
directly related closed issue says that's getting less true.** AC-2:
"installs a JLS that launches after the documented Gatekeeper step."
#135 (closed not_planned, "Developer ID signing and notarization for
the macOS dmg") states in its own Abstract: "on recent macOS the
[right-click-Open] bypass itself is getting harder." #858 does not
acknowledge this risk or specify what "the documented Gatekeeper step"
means if a future macOS release tightens the bypass further — the
acceptance criterion is written as if the workaround's reliability is
a constant, when the one issue that studied it flagged the opposite.
This doesn't block work today, but a task whose whole premise is
"install via brew, then walk the user through Gatekeeper" should at
least note the workaround is on a decaying foundation shared with #82
and #135's stance, so a future maintainer isn't surprised when AC-2's
verification stops reproducing on a newer OS. **Recommendation:** note
the macOS version tested against in the AC-2 verification record (the
issue doesn't currently ask for this), consistent with the pattern
`docs/standards-adoption/02-openssf-badge.md:304` already uses for the
same caveat.

**5. AC-2 substantially duplicates #581's own AC-1, and the overlap is
undeclared.** #581 AC-1: "`brew install --cask jls` on a clean macOS
machine installs a JLS that launches after the documented Gatekeeper
step, and the downloaded dmg's sha256 matches the attested release
asset (CAP-34 AC-1)." #858 AC-2: "`brew install --cask jls` on a clean
macOS machine installs a JLS that launches after the documented
Gatekeeper step, verified once and recorded." These are the same test
run twice under two different issue numbers, with #858's version
dropping only the explicit sha256-match assertion (which AC-1 already
covers). Since #859 changes the cask's `caveats` text after #858 lands
(ordering_after: ["TASK-C581-1"]), and #860 changes the cask's
version/sha256 mechanism after that, the "clean machine install" that
AC-2 verifies at #858's close is not the final state of the cask #581
ultimately certifies — meaning #581's AC-1 verification is either a
redundant re-run of #858's AC-2, or #581 silently reuses #858's
evidence for a cask that has since changed underneath it. Neither
issue says which. **Recommendation:** state explicitly in #858 (or
#581) that AC-2's install verification is superseded by #581's
close-out check post-#859/#860, so the "verified once" evidence isn't
mistaken for the final acceptance record.

**6. Solid parts, briefly.** The core ask — a sha256-pinned cask
definition, a documented tap-vs-`homebrew/cask` choice, and a passing
`brew audit --cask` — is concrete, testable, and correctly scoped
against real repository conventions (`SHA256SUMS-installers-*` is the
actual asset naming per README.md:49-50). `ordering_after: []` is
correct: nothing genuinely blocks this task from starting, and the
parent #581 explicitly recorded ("the dmg already ships") why it
doesn't wait on #443/TASK-0027's promotion of the dmg leg out of
`experimental`. AC-4 (`brew audit --cask` from a committed script or CI
lane) is unambiguous and directly falsifiable.

## Bottom line

The task is buildable and its core artifact (the cask) is well-defined,
but two of five acceptance criteria point at things that don't exist as
written — a phantom section in #82 (AC-5) and an undefined threshold
inherited through #581 (AC-3) — and the architecture/Gatekeeper-decay
gaps (findings 3–4) are the kind of thing that ships a broken or
misleading Intel-Mac experience unless someone notices them before,
not after, implementation.
