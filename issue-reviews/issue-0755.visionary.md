# Issue #755: TASK-C576-1: the distribution and submission layouts are specified in tree, with a worked example an instructor copies
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip away the tree diagrams and this task is the project deciding that **an
assignment cycle is a pure function from bytes to verdicts**, with no living
infrastructure anywhere in it. Everything else in the CAP-33 cluster —
#757's one-command grader, #576 AC-3's CI walk — is downstream machinery. What
#755 actually fixes is the *input alphabet* of that function. That is a much
bigger job than 0.5 mw of prose implies, and it is the right job: the README
already sells JLS on offline, self-contained, version-locked deployment, and
`docs/batch-interface.md` already treats "scripts may parse this" as a
compatibility promise. A course layout is the missing third normative document
next to `simulation-semantics.md` and `batch-interface.md`.

The ordering correction in the comment thread is right and I would go further:
this should lead not only #757 but **#502 CAP-21 PF-1**, whose "frozen,
versioned headless CLI contract" will have to name the on-disk shapes its four
platform adapters consume. If #755 lands as a repo-level normative spec, PF-1
freezes *over* it; if it lands as a CAP-33-local convention, PF-1 invents a
second one and the two drift.

## Reframing 1 — there are three trees, not two, and two of them are projections

The issue names a distribution layout and a submission layout. The instructor's
own tree — starter circuit, *hidden* grading vectors, reference solution,
rubric — is the one that actually exists first, and it is the one that holds the
answers. #502 explicitly needs a visible/hidden test split (Gradescope
`results.json`, nbgrader hidden cells); #517 PF-1 gives every lab "starter .jls,
exercise prose, and `-t` grading vectors". Nothing in #755's four acceptance
criteria says the distributed tree must *exclude* the vectors, and an instructor
copying a worked example that does not say so will ship the oracle to the class.

The elegant form: specify **one tree with a projection rule**.

- The instructor lab tree is the source of truth.
- The distributed starter tree is a stated projection of it (drop `hidden/`,
  drop `solution/`).
- A submission is the distributed tree plus the student's edits plus one
  identity file.

Two of the three layouts then need no independent specification — they are
derived, and the derivation is testable (`distribute(lab)` is a set operation).
Malformed/missing/renamed submissions (#576 AC-4) collapse into a set difference
against the starter tree instead of bespoke error handling, and #757's AC-4
determinism requirement gets a free ordering rule: enumerate identifiers in
byte-wise sort order, fixed by the spec rather than by the tool.

## Reframing 2 — the identifier must never appear in a path JLS opens

This is the concrete landmine the issue walks straight at. `Util.isValidName`
(`/home/user/JLS/src/jls/Util.java:218-233`) requires a circuit file's base name
to match `[letter][letter|digit|_]*`, and `FileAbstractor.openCircuit`
(`/home/user/JLS/src/jls/FileAbstractor.java:101-111`) refuses to load anything
else with `NOT_A_CIRCUIT`. Directory components are explicitly exempt
(`Util.isValidFileName:247-262` strips everything before the last separator).
So:

- `800123456.jls` — an all-numeric SIS student ID — **cannot be opened by JLS**
  (digits are rejected in position 0).
- `lab3-jdoe.jls` — the GitHub Classroom repo-name convention #502 PF-3 targets
  — **cannot be opened** (hyphen).
- `jdoe@uni.edu.jls`, `obrien_jane.jls` from `O'Brien` — same.
- `李明.jls` opens fine, because `Character.isLetter` is Unicode-aware. The
  alphabet is permissive about scripts and hostile to punctuation, which is
  exactly backwards from what student identifiers look like.

AC-1's "how a submission identifies its student" is therefore not a naming
preference; it is a hard constraint with a code anchor. The spec must state:
**circuit filenames are fixed by the assignment, never derived from the
student**, identity lives in a directory name (unvalidated) *and* in a one-line
manifest inside the submission, and **the manifest wins** when the two disagree.
That last clause satisfies half of #576 AC-4 ("renamed submissions produce a
named per-student result") by construction rather than by error handling. The
worked example should include one submission whose directory has been renamed,
so the property is visible in the artifact an instructor copies.

## Reframing 3 — express the contract as data, not prose

AC-3 says the specification "states what is stable and what an instructor may
vary, so the grading command in TASK-C576-2 has a contract to rely on." A prose
statement of variability is not a contract #757 can rely on; it is a contract
#757 must re-implement by reading. Put a single machine-readable manifest at the
root of the lab tree naming the assignment id, the required circuit files, the
vector files, the fixed/editable split, and the identity field. Then:

- `#757`'s command becomes argument-free (`jls-grade <dir>`), which is what
  "one command" in #576's title actually wants.
- #502's four adapters read one schema and can plausibly produce the
  byte-identical score vectors AC-1 demands; four adapters reading prose cannot.
- "What an instructor may vary" becomes a value in a file, so relaxing it later
  (the comment's under-promise-is-safe advice) is a schema addition rather than
  a spec rewrite.

This does not violate the issue's own constraint — a manifest is still files,
nothing can die holding it. The objection worth taking seriously is that it adds
a format to freeze; but the project already knows how to freeze formats on
purpose (`docs/batch-interface.md` §6), and a schema is far cheaper to freeze
than a directory convention discovered by grepping paths.

## Reframing 4 — do not duplicate the lab/kit format that is already owned

Four issues are converging on overlapping directory conventions: this one,
#517 PF-4 ("kit = labs + vectors + schedule + rubric" as a packaging convention
a third party can author), #502's references to a "CAP-06 lab-as-data format",
and #502 PF-3's Classroom starter-repo template. Nothing in the tree defines any
of them yet (`grep -rn "lab-as-data|course-kit" docs/` returns nothing). #755 is
first and cheapest and should therefore *claim the single-assignment unit* and
explicitly disclaim everything above it: one assignment's tree here, a pack of
them in #517 PF-4, platform packaging in #502. Say so in the Boundary section,
or the same tree gets specified three more times.

## Reframing 5 — the worked example belongs where a test can consume it

`examples/autograde/autograde.py` is the precedent and it is a good one: it is
pinned by `test/jls/AutogradeBridgeExampleTest.java`, so it cannot rot. AC-2's
"complete enough to copy without editing anything but names" is only checkable
if something copies it and edits nothing but names — which is exactly #576
AC-3's CI walk (TASK-C576-3). #755 should therefore commit the example at a path
that walk consumes *unchanged*, and name that path in the spec, rather than
leaving placement to the later task. An example the end-to-end test has to
adapt is an example instructors will also have to adapt.

## The one way this task damages the arc

Freezing. At 0.5 mw, with no consumer and no golden test, a layout spec labelled
a stability contract would be the project's first frozen interface with nothing
behind it — the opposite of the discipline in `batch-interface.md` §5, where
every frozen claim is pinned byte-exactly by a named test. Mark the document
**normative but provisional** until #757 consumes it and one real course (#509's
CSE 260M corpus, #517 PF-3) has been expressed in it. Then freeze it, and let
#502 PF-1 fold it into the versioned public contract.

## Verdict

**endorse-with-reframing.** The task is correctly identified as unblocked,
cheap, and foundational; I am not disregarding its acceptance criteria, but AC-1
and AC-3 need to be read much more strongly than written. Concretely: three
trees with projection rules rather than two independent layouts; identity kept
out of every path JLS opens, with the code anchor cited in the spec; the
stable/variable split expressed as a manifest schema rather than prose; explicit
disclaimers against #517 PF-4 and #502 PF-3's territory; the example committed
where TASK-C576-3 consumes it verbatim; and the document marked provisional
until it has a consumer.
