# Issue #484: Measured ground truth for the virtual-hardware / virtual-logic parity study: engine constants, boot arithmetic, element count, live-console limit, parity contract
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## What this issue actually is

Not a work ticket. It is a ~15,000-word "rescue" of a measurement corpus from a
doomed branch (`claude/jls-virtual-hardware-linux-njsoma`), reproduced in full
because the branch and its backing files (`docs/plan/evidence/*.md`) "will not
be merged and will be deleted." It has no acceptance criteria, no assignee
action, and no stated definition of done — it exists to be *cited* by other
issues. That framing itself is the main thing to attack.

## Findings, most severe first

**1. [Critical] The core "these citations survive" claim is false — the anchor commit is not on master and will vanish with the branch it disclaims.**
The issue states: *"Citations of the form `src/...`, `test/...`, `pom.xml`,
`docs/.md` **do** survive; they are on `master` and are pinned here to
`2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`."* I checked this directly:
`git merge-base --is-ancestor 2d0ca9d master` fails, and `git branch --all
--contains 2d0ca9d` returns only `remotes/origin/claude/jls-virtual-hardware-linux-njsoma`
— the exact branch this issue says "will not be merged and will be deleted."
`git show -s 2d0ca9d` confirms it's a merge-master-into-branch commit that
lives *only* on the doomed branch. So the one commit this document relies on
to make its file/line citations checkable is scheduled to become unreachable
(and eventually GC-eligible) the moment the stated cleanup happens — the
document's citation-preservation strategy defeats itself. Recommend re-pinning
to an actual master commit, or tagging `2d0ca9d` (or a cherry-picked point) so
it survives the branch deletion the issue itself calls for.

**2. [Major] No acceptance criteria, no closer, no consumer named.** Every
other open issue in this tracker presumably has "done when X"; this one has a
status table ("stands" / "superseded" / "narrowed") describing itself, not a
task. Under adversarial review that means it can never fail verification —
which is a defect, not a virtue: there is no way to tell whether this issue
should ever be closed, or what "acting on it" means procedurally. Recommend
either closing it once its content is merged into a real `docs/*.md` file (see
#3), or explicitly stating which issue(s) are blocked/unblocked by it.

**3. [Major] Load-bearing performance numbers are sourced from files that are being deleted, with no reproduction path left in the repo.** Every headline
figure — 3.14M events/s, 318 ns/event, nommu boot 1.66–1.72 h, live-console
19,500–96,000 cycles/s — cites `keystone-c-performance.md:NNN` explicitly
marked `(dead path)`. §12 confirms the backing corpus (`00-fact-base.md`,
`recon-*.md`, `art-*.md`, `c2-*.md`, JFR traces) is deleted with the branch.
No benchmark script, fixture, or raw JFR data survives in `docs/plan/evidence/`
(confirmed: `Glob docs/plan/evidence/**` returns nothing in this checkout).
Anyone who later disputes "3.14 M events/s" has no way to rerun the
measurement against this document — only against the number itself. Titling
this "measured ground truth" overstates what's actually preserved: it's an
*assertion* of a measurement, not a reproducible one.

**4. [Major] Ships a self-identified unresolved contradiction with no resolution proposed.** §9 says verbatim: *"`docs/vcd-interop.md:19-24` REJECTS
live co-simulation under #63... This **directly contradicts** the goal and
must be explicitly reopened or reconciled."* I verified this: `docs/vcd-interop.md:18-23`
does say *"Not offered: live co-simulation... Graders must not depend on
interacting with a running simulation"* and links #63 as the rejection. #484
correctly surfaces this conflict but takes no position and files no
reconciliation issue — it just leaves a live contradiction between the
virtual-hardware direction and a normative doc sitting in the tracker,
citable by future issues in either direction. This needs an owner and a
resolution path before anything downstream builds on it.

**5. [Moderate] Ambiguous about whether it satisfies #221's own filing requirement.** §9: *"the recorded process requires FILING the follow-up issue
first (it 'deliberately does not exist yet')"* — yet §7's corrections table
asserts *"The trigger is now quantitatively met and instrumented for the
first time."* #484 never states whether it **is** that required follow-up
filing (in which case it should say so and follow #221's format) or is merely
supporting evidence for one not yet filed (in which case the strong claim that
the trigger is "met" is premature — the process gate hasn't been passed).
Pick one and say so explicitly.

**6. [Minor] Dangling, unnumbered cross-reference.** *"The binding maintainer
rulings... are rescued separately — see the decision record issue for
D1–D16"* — no issue number or link. `search_issues` finds it in one query
(#485, "Maintainer decision record: D1-D16..."), so this is a trivial fix, but
as written it's exactly the kind of unreachable-pointer problem §0 claims to
have eliminated everywhere else in the document.

**7. [Minor] Scope/process: normative-weight content living only in an issue body.** At ~15K words with a table of contents and a "supersedes §§2-6"
correction mechanism, this reads as a policy/reference document, not an issue.
Issue bodies aren't diffable, aren't reviewed line-by-line, and aren't listed
in ARCHITECTURE.md's documentation index the way `docs/*.md` files are, yet
other issues are told to cite it as "BRIEF.md section N." Recommend landing
the surviving content as an actual `docs/` file via a normal PR so it gets
the same review/CI/blame trail as everything else load-bearing in this repo.

## What holds up

- The in-tree forensic citations I spot-checked are accurate: `JLSInfo.java:69`
  (`defaultTimeLimit = 100000000`, exact), `FileAbstractor.java` `MAX_CIRCUIT_TEXT_BYTES
  = 64L << 20` (matches the "64 MiB load cap" claim), `Memory.java:1396-1397`
  (`MemoryRead` posted at `now+accessTime`, exact match), and `RegisterFile.java`'s
  javadoc literally says it collapses "~95 elements" into one — consistent
  with §7's regfile-element-count correction.
- `ElementRegistry.java`'s `ALL` list contains exactly **35** `ElementType`
  entries — the "35 types" figure in §7 is exactly right, not rounded.
- `JlsModules.java`'s javadoc independently confirms the "wired and
  UNCONSUMED... nothing reads it for dispatch yet" characterization almost
  verbatim — this is a well-sourced, non-cherry-picked claim.
- The two-tier (behavioral/structural) framing and the parity-contract
  MUST/PERMITTED split (§6) are internally consistent and cite real
  precedent (ARM PV docs, gem5 TLB-flush behavior) rather than assertion.

## Note

Current `ARCHITECTURE.md` at HEAD still says *"There is no element registry
yet — issue #78 will introduce one"* even though `ElementRegistry.java`
already implements #78 in full — that's staleness in `ARCHITECTURE.md`
itself, not a defect in #484, but a reviewer cross-checking #484 against
`ARCHITECTURE.md` will hit what looks like a contradiction that is actually
just drift elsewhere in the repo.
