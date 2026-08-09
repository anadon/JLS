# Issue #495: Rescued from a deleted branch: docs/parity-contract.md in full — the proposed (UNRATIFIED) parity contract defining what "virtual logic and virtual hardware agree" means
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what the issue actually is

Filed by the repo owner (`anadon`, `author_association: OWNER`), open, labeled
`documentation`. It is not a task: it inlines all 940 lines of
`docs/parity-contract.md`, a file that existed only on a branch
(`claude/jls-virtual-hardware-linux-njsoma`) the maintainer has decided to
delete, so that ~14–25 other filed issues that cite the document by clause or
line number keep a resolvable target. The issue explicitly disclaims
ratifying the contract by filing it. This review checks the rescue's
integrity claims and its downstream consequences, not the contract's
technical content (out of scope — the contract is explicitly non-normative).

## Findings, most severe first

**1. [High] The "byte-for-byte, verbatim" claim is contradicted by the delivered text — systematic HTML-entity corruption.**
The issue's own "What was inlined, what was dropped" section states: "Dropped:
nothing. All 940 lines are below, verbatim, byte-for-byte, with no elision...
A paraphrase would have destroyed it." But the body as stored (fetched via
`issue_read`) contains **zero** literal apostrophes and **zero** literal
double-quote characters across all 61,797 characters — every one has been
replaced by `&#39;` (45 occurrences) or `&#34;` (56 occurrences), and every
leading blockquote marker is `&gt;` (13 occurrences) rather than a literal
`>`. This corruption is in the *title* too (`&#34;virtual logic and virtual
hardware agree&#34;`), which is plain text, not markdown — GitHub does not
markdown-render titles, so the entity codes are likely to display literally
rather than resolve to quote marks. Two concrete consequences: (a) the
leading `&gt;` lines in the "Why this issue exists" framing will not render
as blockquotes on GitHub, since blockquote syntax requires a literal `>`
byte, not the six-character entity; (b) the "Provenance and integrity" table's
SHA-256 (`439ec872db466aed95a1f5b798a076c53921aa9b658c1f51f8264d75860c4a72`)
and "byte-identical to `2d0ca9d:docs/parity-contract.md`" claim cannot both be
true of the text actually stored in this issue, since a hash computed over
the genuine document (with real `'`, `"`, `>`) will not match text with those
bytes substituted. For a rescue whose entire stated purpose is exact-wording
preservation for other issues to quote, this is a direct hit on the core
claim. **Recommendation:** re-paste the body without HTML-escaping (fix
whatever tool produced this — it looks like `html.escape()`/similar was run
before the API call), and re-verify the hash against what's actually stored.

**2. [High] Wrong tool for the actual problem — the file should have been restored to `master`, not pasted into an issue.**
The real problem this issue is trying to solve is that ~14 issues cite
`docs/parity-contract.md` *by line number* and #423's completion criteria
requires ratifying it via an `ARCHITECTURE.md` §8.3 decision block naming
that file. The issue's own "A defect noticed while rescuing" section (item 1)
admits this: "#423's completion criteria require ratifying this document in
the implementing merge commit. The document it would ratify is now this
issue, not a file. Whoever executes #423 must first re-home the text into
`docs/parity-contract.md` on `master`, or the criterion is unsatisfiable."
That is a correct diagnosis — and the issue does nothing to fix it, only
narrates it in prose (no checkbox, no linked follow-up issue, no PR). The
repo already carries multiple *other* unratified proposal/research documents
directly on `master` (confirmed present: `docs/grand-architecture.md`,
`docs/hdl-support-research.md`, `docs/library-survey-2026-07.md`, etc.), so
"unratified doc lives in `docs/`" is not a status this project avoids — a PR
restoring the file (still stamped "proposed, not ratified") would have been
strictly better than an issue body: it avoids finding 1's corruption
entirely, keeps the original line numbers the citing issues already use
(making the "line-anchor concordance" remapping table in this issue
unnecessary), keeps the sibling relative links live instead of "dead by
design," and is diffable/version-controlled like everything else in a
codebase whose culture (per `ARCHITECTURE.md`) is ratchet tests over prose
promises. No rationale is given for why this route wasn't taken.
**Recommendation:** open a PR restoring `docs/parity-contract.md` (still
unratified) to `master`; treat this issue as superseded once it lands.

**3. [Medium] The central "provenance" claim is unverifiable by any reader, including this review.**
The five branch-only commit hashes the issue cites for provenance
(`2d0ca9d`, `d2e4d91`, `36cbd37`, `b299d63`, `64c137d`) do not resolve in this
checkout (`git cat-file -t <hash>` fails on all five — expected, since the
source branch is gone, but this repo's clone is also shallow, 273 commits).
The one commit it cites that *is* reachable, `8288226`, does independently
confirm the one claim that's actually checkable — `git cat-file -e
8288226:docs/parity-contract.md` indeed fails, matching the issue's stated
verification — but the SHA-256/"byte-identical" claim about the deleted
branch rests entirely on the OWNER's own assertion with no independent copy
anyone can check it against. That's a reasonable position for the maintainer
to take on their own authority, but the "Provenance and integrity" table
presents it with the same epistemic weight as the genuinely-verifiable row
above it, which overstates its own checkability.

**4. [Medium] No lifecycle contract for the issue itself, despite ~14–25 issues now depending on it staying open and unedited.**
Unlike a task issue, #495 carries no acceptance criteria, no "do not close"
marker, and only the generic `documentation` label. Any future edit to this
body (by anyone with write access) or well-intentioned closure ("stale
cleanup", "not planned") silently breaks every citing issue's line/clause
references, with nothing to catch it — no CI check equivalent to
`FileFormatSpecTest` or `ExtensionPointCatalogTest` can run against an issue
body. This is the one place in an otherwise ratchet-test-disciplined project
where load-bearing text lives entirely outside version control and outside
any test harness. (No stale-issue bot was found in `.github/workflows`, which
somewhat lowers the accidental-closure risk, but doesn't address accidental
edits.) **Recommendation:** resolved automatically by finding 2's fix;
absent that, at minimum flag the issue as pinned/protected in its own text.

**5. [Low] The line-anchor concordance table is a permanent patch over a problem it should instead be tracking as a follow-up.**
It exists because "[t]hose line numbers refer to the blob above, which no
longer exists" — i.e., every citing issue that quotes `docs/parity-contract.md`
by line number is now citing dead coordinates, silently remapped by this
table rather than by updating the citing issues. Acceptable as an immediate
stopgap; there's no tracked follow-up to actually fix the citing issues'
text (or, per finding 2, make the remap moot by restoring the file).

## What's solid

- The code anchors the rescued document cites check out precisely against
  current HEAD: `Adder.defaultPropDelay = 30` (`src/jls/elem/Adder.java:33`,
  matching the claimed "30 × bits" adder delay); `HdlExporter.EXPORTED`
  contains exactly 22 classes and excludes both `RegisterFile` and
  `FieldExtend` (`src/jls/hdl/HdlExporter.java:422-428`); and
  `Simulator.java`'s event-eviction-before-limit-check
  (`src/jls/sim/Simulator.java:224-233`) and the `beforeEvent` hook
  (`:252-255`) match the cited ranges exactly, including the eviction-before-
  limit-test ordering the document's open question 9.8.3 hinges on. This is
  unusually well-grounded for an archival issue.
- The one provenance claim actually checkable today — that
  `docs/parity-contract.md` is absent from `master` (via `8288226`, and from
  the full `git log --all` in this checkout) — is true.
- The cross-referenced issue numbers spot-checked (#423, #395, #347, #425,
  #477, #343, #493) are all real, open issues in this tracker, not invented.
- The "status not changed by this issue" framing and the disclaimer that
  filing does not ratify are clear and consistent with `ARCHITECTURE.md`'s
  actual ratification mechanism (a recorded decision block, none of which
  exists for this contract).

## Bottom line

The rescue's factual grounding (code anchors, the master-absence claim, the
cited issue numbers) holds up. But the issue fails its own stated bar on the
one property it says matters most — exact, byte-for-byte preservation — via
systematic HTML-entity corruption of every quote, apostrophe, and blockquote
marker in the text, and it identifies but does not fix the concrete breakage
it leaves behind for #423. Both are fixable without re-litigating the
contract's content: clean up the encoding, and prefer restoring the actual
file to `master` over parking normative-adjacent text inside an issue body
indefinitely.
