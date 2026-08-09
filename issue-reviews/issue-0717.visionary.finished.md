# Issue #717: TASK-C531-1: one fixture lab, 300 committed submissions and golden per-student score vectors — the apparatus the parity claim is measured against
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the acceptance criteria and one instinct remains, and it is a good one: **build the
measuring apparatus before the claim, and let exactly one thing in the tree be the referent.**
CAP-21 (#502) asserts four adapters produce byte-identical per-student score vectors; without a
single shared fixture that assertion degenerates into four artifacts agreeing with each other,
where an adapter divergence can hide inside a fixture divergence. The mirror comment's ruling —
one corpus, this issue owns it, #697 consumes it — is correct and should stand. The same
discipline appears in #883 (30-submission similarity corpus, apparatus first, manifest as sole
ground truth), and the two issues should be read as one house style.

My disagreement is not with the instinct. It is with **what layer the apparatus is cut at**.

## The seam is in the wrong place

#719 states the claim precisely: the four adapters produce identical score vectors *"from the
same xUnit input."* The adapters never see a `.jls` file. They consume xUnit XML produced by
#524's frozen CLI and emit a platform envelope. The 300 circuits exist in this issue only as a
machine for manufacturing xUnit documents — an expensive, slow, opaque machine that drags the
entire simulation engine and CAP-06's verdict semantics inside the boundary of a claim about
output formatting.

**Reframing A — the fixture is a corpus of frozen xUnit documents, not of circuits.**
Record the xUnit output of the 300-submission grading run once, commit *that*, and measure every
adapter against it. What this buys:

- **AC-4 evaporates.** A few hundred KB of XML has no large-fixture problem. 300 `.jls` files do,
  and `.gitattributes` already marks `*.jls -text` — 300 blobs that neither `git diff` nor a
  human reviewer can read, which is the opposite of "recorded provenance."
- **The parity claim stops being hostage to grading evolution.** Goldens derived from the live
  grader mean every CAP-06 improvement — counterexample text, coverage figures, and eventually
  the don't-care-aware and formal grading that `docs/capability-roadmap/lf-04-formal-and-grading.md`
  argues for at length — rewrites 300 goldens, and nobody can distinguish a fix from a regression.
  That is KC-21-4's objection ("a frozen contract that blocks the verdict engine's own roadmap")
  transposed from the contract onto the fixture, where nobody has noticed it. lf-04 says plainly
  that vector grading is *"not merely weak but wrong"*; this issue proposes to cast 300 vector-graded
  score vectors into golden bytes at the exact moment the roadmap is arguing to replace the model.
  At the xUnit layer the grading model can change freely underneath and CAP-21's claim is untouched.
- **Coverage improves rather than degrades.** Platform divergence does not live in circuits; it
  lives in score representation. Gradescope takes float `score`/`max_score` per test, PrairieLearn
  wants a 0..1 fraction, nbgrader wants integer points, and the Action reports Classroom points.
  Rounding boundaries, ties, zero-weight tests, a submission that fails to load, a suite with zero
  tests, unicode in a test name, an enormous failure message against Gradescope's output limits —
  these are the cases that break byte-identity, and *no plausible set of 300 student circuits
  produces them on purpose*. As xUnit documents you can author them deliberately in an afternoon.
- **It matches the kit's own normative constraint.** #498 §7.2 / KC-21-2 forbid adapters from
  driving a live session: they must grade from recorded artifacts only. A fixture made of recorded
  artifacts is the same rule applied one level up, self-consistently.

The circuit corpus does not disappear — it belongs to CAP-06 (#300/#369), whose §1 walkthrough
already requires 300 `.jls` files graded in one invocation and byte-identical across OS and JDK.
CAP-21 is ordered *behind* CAP-06; having CAP-21 author the circuit corpus that CAP-06's own demo
needs is the same inversion the mirror comment found between #697 and #531, one tier higher.

## Reframing B — parity by construction, measured only as a regression

Make this issue's primary deliverable the **canonical score-vector schema** — an ordered,
canonically-serialized (student, test, score, max) sequence — plus the single extraction
definition #719 AC-2 already asks for. Then each adapter is a total function from that canonical
vector into a platform envelope, and each extractor is its inverse. Byte-identity becomes a
round-trip law, `extract_i(emit_i(v)) == v` for all four `i`, which property-based generation over
the score domain proves far better than 300 fixed samples ever will. The corpus becomes a
regression witness and a demo, not the proof. Four hand-written normalizers papering over
divergence — the failure #719 AC-2 fears — is impossible when there is one type in the middle.

## Reframing C — do the parity analysis on paper, first, here

The highest-leverage hour in all of CAP-21 is not in this issue's scope and should be. KC-21-1
says the four-way claim may be irreconcilable in principle. As planned, that is discovered by
#719, which is ordered after #525, #526, #528 and #530 — roughly 8–10 mw of adapter work already
spent. Writing the canonical vector and the four mappings by hand from each platform's *published*
spec costs hours and can kill or re-scope AC-1 before any of it. This task is titled "the
apparatus the parity claim is measured against"; a falsification you can perform with a text
editor belongs in it. Note also that this issue is presently gated behind #524 and #300 — neither
of which exists (no `xunit` string appears anywhere in `src/` or `docs/batch-interface.md`, which
still documents three exit statuses) — so the paper analysis and the schema are the only parts of
it that can be started at all today. #883 made exactly this move: `blocked_by: []`, "authoring
fixtures needs nothing built first."

## On "300 committed submissions" — I am disregarding AC-1 as written

Two things are being conflated under one number. **Scale** (300 submissions in one invocation,
inside a wall-time budget) is CAP-06's claim and #697's budget. **Diversity** (enough distinct
score-vector shapes that agreement is meaningful) is what parity needs, and it is not a function
of volume. Copying 300 from #300's title into #531, then into this issue, then into #697's AC-1
and #506 makes a cohort-size figure load-bearing in four places for a property it does not
measure. #880/#883 chose 30 deliberately and explicitly refused to grow to 300 to rescue a result;
that is the better precedent. State the corpus size as a count of covered equivalence classes and
let CI pay cohort-scale cost only in the one lane where scale is the actual claim.

Two smaller consequences the issue should absorb from #883 regardless of which framing wins:

- **Generator plus manifest, not blobs.** AC-2 already requires a single documented command that
  regenerates the goldens byte-identically — so a generator exists. Then commit the generator, the
  seed and a hash manifest, and materialize the corpus in CI. That is *better* provenance: mutation
  rules are auditable prose, 300 opaque XZ blobs are not. The repo's own reproducibility posture
  (README: jar and `bom.json` bit-reproducible from a recorded `.buildinfo` recipe) already prefers
  a recipe and a checksum to shipped bytes.
- **Fixtures name fixtures, not people** (#883 AC-6). "Per-student score vectors" will need
  identities; adopt the no-student-like-names rule from the first commit rather than retrofitting.
- **"Adversarial" submissions are two different tests.** A malformed `.jls` exercises #524's exit-
  status contract, not any adapter. What the adapters must agree on is how an *unloadable* result
  is represented — zero versus ungraded versus error — and platforms differ sharply there. At the
  xUnit layer that case is one document, stated exactly, instead of a corrupted circuit whose
  meaning depends on the loader.

## Verdict

Endorse the issue's existence, its ordering-first position, and its sole ownership of the referent.
Reframe the artifact: this task should ship (1) the canonical score-vector schema and the one
extraction definition, (2) a committed corpus of frozen **xUnit documents** covering the score-
representation edge cases, generated by a committed generator with a manifest, and (3) a written
four-way mapping analysis against the published platform specs that can falsify AC-1 before any
adapter is built. The 300 `.jls` submissions stay in CAP-06, where the 300 is a claim rather than
an inherited number.
