# Issue #446: TASK-0040: a set of circuits becomes one distributable, license-carrying, digest-checked artifact that a circuit can reference by name
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

A new `.jlslib` container format (a `LIBRARY` index + `DEFINITION` sections
+ a required `PROVENANCE` section) plus a headless `jls.lib` reader/writer
and `-lib list` / `-lib extract` batch CLI verbs. Blocked on #417
(definition digest/identity) and #444 (section frame), both open and
unlanded.

## What checks out

Every code citation I could verify against the current tree is accurate:
`ElementVocabulary.ALLOWED` really has 34 tags and `ElementRegistry`
really has 35, and the sole difference is exactly `TestGen`
(`src/jls/collab/op/ElementVocabulary.java:39-46`,
`src/jls/elem/ElementRegistry.java:39-77`); `docs/file-format.md:355-360`
documents the `TestGen` asymmetry and the `SubCircuit` nested-grammar
recursion essentially verbatim as quoted; `HeadlessCoreRatchetTest`'s
`CORE_PACKAGE_PREFIXES` (`test/jls/HeadlessCoreRatchetTest.java:74-79`)
really has no `src/jls/lib/` entry, confirming O1/O2; `FileAbstractor`'s
`MAX_CIRCUIT_TEXT_BYTES` and `BoundedInputStream` exist as quoted
(`src/jls/FileAbstractor.java:65,347-353`); `ElementId`'s XDG convention
and `pinForTesting` exist as quoted. `jls -b -lib list` does fail as
described (the flag doesn't exist in `JLSStart.FLAGS`). The P4/P7 test
design (assert the vocabulary check ran *before* reflection by using
`TestGen` — a valid registry tag outside `ALLOWED`, so a registry-only
gate would wrongly pass) is a genuinely good adversarial test design and
is sound. One line each — moving on.

## Findings, most severe first

**1. The issue's central authority — `docs/plan/evidence/BRIEF.md` — does not exist in this repository, at any commit reachable from either branch checked out.**
The whole "why this is allowed to proceed" argument rests on quoting
decisions D7 ("Circuit libraries are DATA, not plugins... the biggest
single win") and D10 ("a demand gate does not bind the maintainer's own
roadmap") from `docs/plan/evidence/BRIEF.md §12`, said to have "landed in
commit `3a81a4a7d6a0f108ec201e632732d308cc02b3fc`." That path does not
exist anywhere in the repo — not on `master` HEAD (`c5cee1b`), not on the
review branch, and `git cat-file -t 3a81a4a7d6a0f108ec201e632732d308cc02b3fc`
returns "bad object" (not merely a shallow-clone gap, since the *path*
`docs/plan/evidence/BRIEF.md` is simply absent from the tree at `master`'s
full, unshallowed tip). Likewise `docs/plan/tasks/TASK-0040-circuit-library-container-and-provenance.md`
— the "corpus document that proposes it" cited by O1 as the sole
occurrence of the token `jlslib` in the tree at the evidence commit — is
absent from the working tree entirely. A reviewer or contributor working
from what's actually in `anadon/JLS` cannot verify D7, D10, or that a
prior task document ever said what the issue claims it said; the
evidentiary chain this whole apparatus (rule 1's "quoted command output is
evidence," rule 3, rule 6, rule 10, all invoked throughout) depends on is
unauditable from the repository. **Recommendation:** either commit the
planning corpus (`docs/plan/**`) so citations are checkable, or stop
treating an external/unshared document as a load-bearing authority for
"this bypasses the demand gate" — that argument is exactly the kind of
claim a skeptical maintainer should require to be self-contained in the
tracked repo before treating it as settled.

**2. Three items are marked "Blocks execution" in Open Questions & Decisions Needed, yet the issue offers only "recommended defaults" for them — it is not actually ready to implement.**
Quoted: "1. Is `MAX_CIRCUIT_TEXT_BYTES` per definition or per library? ...
**Blocks execution**" / "2. Are SPDX identifiers validated or merely
recorded? ... **Blocks execution**" / "3. What happens when two libraries
in the per-install directory define the same `defid`? ... **Blocks
execution.**" A "Recommended default" is not a decision; it is the issue
author declining to decide and pushing the choice to whoever picks up the
task. For #2 in particular, "validate against a bundled snapshot of the
SPDX identifier list" is a new external vocabulary with its own update
cadence (SPDX adds identifiers regularly) that isn't scoped anywhere in
Materials & Apparatus, Interfaces, or Completion Criteria beyond the one
recommendation — no owner for keeping the bundled snapshot current is
named, and "LicenseRef-" escape-hatch parsing is hand-waved in one
clause. **Recommendation:** resolve all three before this is filed as
implementable, or split "decide the SPDX/size-cap/tie-break policy" into
its own preceding design task.

**3. Internal contradiction: compression is declared out of scope, then required as a tested attack surface.**
§13 Conclusion states explicitly: "a compression story for libraries (D1
says the user compresses; if it returns it is framed per section, which
is #444's open question)" is **explicitly out of scope**. Yet §6 Method
requires building "a compressed decompression bomb" fixture, §5 predicts
P13 "A hostile compressed library cannot decompress past the declared
bound; the `BoundedInputStream` guard (O8) is exercised by a test," and
§14 Definition of Done requires "The hostile-compressed-library test
exercises `BoundedInputStream`, and #38's bound is re-argued for the
multi-member shape in the PR." If `.jlslib` compression is genuinely
undecided and deferred to #444, there is no concrete container shape to
write a decompression-bomb test against yet; if `.jlslib` in fact reuses
`FileAbstractor`'s XZ/zip/plain-text sniffing (the natural reading, since
O8 is cited as apparatus), then compression is *not* out of scope and the
Conclusion's disclaimer is misleading. As written, an implementer hits
this contradiction on day one and has to silently resolve it themselves —
exactly the kind of decision-by-omission the issue elsewhere warns
against ("the kind of thing that gets decided by omission," of the size
cap). **Recommendation:** state plainly whether `.jlslib` is an XZ/zip/
plain container like `.jls` (reusing `FileAbstractor`) or a bespoke
uncompressed format, before Method's fixture list is written.

**4. P11 ("no `ServiceLoader`/`URLClassLoader`/`ProcessBuilder`/`defineClass` under `src/jls/lib/`") is a gameable acceptance criterion the issue's own text admits is weak, yet it is enshrined as a hard Definition-of-Done checkbox.**
Quoted from § Threats to Validity: "P11's grep is a weak guard — it
catches the obvious forms and not a hand-rolled equivalent." That's
correct and understated: `Class.forName(tag).getDeclaredConstructor().newInstance()`
— the exact pre-#78 reflection pattern the codebase moved away from
elsewhere in this file (O4) — trips none of the four greppable tokens and
would sail through P11 while violating D7 ("no ABI, no trust boundary")
in substance. The issue still lists "P11 verified and its empty grep
output pasted in the PR ... D7 makes this a correctness criterion" as an
unconditional Definition-of-Done line, with no secondary check (e.g. a
`git grep -n 'Class\.forName\|\.newInstance()'` scoped to `src/jls/lib/`,
or routing every construction through the existing `ElementRegistry`
factory table so no reflection call site exists to hide behind a
different name). **Recommendation:** either add the reflection-pattern
grep alongside the four named APIs, or require (and test) that
`jls.lib`'s element construction path is literally `ElementRegistry.forTag`
plus the vocabulary gate and nothing else — making a hand-rolled
equivalent structurally impossible rather than merely un-grepped.

**5. Scope and cost: this is an unusually large unit of work to land and review as one task.**
One new package, a new file format section grammar (3 section kinds), a
byte-identical deterministic writer with two build-time pins asserted in
tests, a 4-level resolution order, an SPDX validation subsystem, 2 new
CLI verbs with their own help-pinning-test updates, 13 predictions, 5
falsifiable hypotheses, and 7+ named hostile fixtures — while also being
blocked on two other open, equally large, equally speculative,
un-landed tasks (#417, #444) whose own shapes are not final ("if either
#417's digest definition or #444's frame changes shape during execution,
the container's format changes with it," per § Threats to Validity,
correctly self-identified but not mitigated). A single PR attempting all
of Predictions P1-P13 plus Falsification H1-H5 plus 18 Definition-of-Done
items is expensive to review holistically and risks exactly the kind of
"adjacent work discovered en route" scope creep the issue tells its own
implementer to avoid. **Recommendation:** the issue would be sounder split
along its own Stage boundaries — e.g. (a) grammar + read/write + digest
validation, (b) resolution + CLI, (c) provenance/SPDX validation — each
independently landable and each with its own red/green test set, rather
than one omnibus task gated on two other omnibus tasks.

**6. Minor: the H4/Stage-3 resolution order names three levels in prose ("open file → libraries named on the command line → per-install directory") but the CLI surface added in this same issue (`-lib list`, `-lib extract`) never introduces a flag for "libraries named on the command line."**
§7.1 says only `-lib list <lib>` and `-lib extract <lib> <defid> <out>`
are added; nowhere does this issue add the flag by which a `.jls` load
would be given one or more command-line libraries to search (level 2 of
Stage 3). P8 ("three libraries each defining `local:local:adder:1.0.0`...
open file, then command line, then install directory") is therefore
untestable through any interface this issue actually ships — the
command-line-library flag is assumed to exist but is never specified or
added to `JLSStart.FLAGS`. **Recommendation:** either add the missing
flag to §7.1's External interfaces modified list, or drop the
command-line tier from Stage 3 / P8 and note it as a follow-up task's
responsibility.

## Verdict rationale

The document is unusually rigorous in form (falsifiable hypotheses, named
test methods, explicit failure-mode table) and the code citations that
*are* checkable against this repository hold up precisely. But it rests
its core justification on a document that isn't in the repository, leaves
three self-declared execution-blocking decisions unresolved, contains a
direct contradiction about whether compression is in scope, and ships an
acceptance criterion (P11) it simultaneously admits is gameable without
closing the gap. These are fixable with edits to the issue text, not
evidence that the feature is wrong to build — hence `needs-rework` rather
than `should-not-proceed`.
