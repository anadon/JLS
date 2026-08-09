# Issue #578: FEAT-C33-4: a third party can author a course kit from the documented convention, and one complete worked course proves the convention holds
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the packaging language and the ask is: **make "a JLS course" a thing that exists
outside this repository.** CAP-33 (#517) says the prize is a DEEDS instructor at a forced
migration moment — someone whose `.pbs` files can never be imported, so the *course* must
port. #509 says the one live counterparty (CSE 260M, Dr. Siever) is conditional on
maturity. Neither of those people is blocked by not knowing what directory to put a lab
in. They are blocked by there being no such thing as a portable JLS course at all: no
noun, no address, no channel.

So the convention is the right instinct. But #578 as written builds the *least
load-bearing third* of that noun — a directory schema, a repo-CI linter, and a license
line — while leaving the two parts that actually make a kit exist for strangers
(distribution/discovery, and enforcement that runs on the stranger's machine) unowned by
any issue in the cluster. Nothing in the tree contradicts this: `grep -ri "course.kit"`
over the repo returns zero, so this is greenfield and the seam is still free to choose.

## Where the seam is cut wrong, and where it should be cut

### 1. The validator is shipped to the wrong audience

AC-2 puts the validator in CI, over "every shipped kit." That is the weakest possible
consumer: the kits inside this repo are the ones a maintainer authored and least likely to
be malformed. The third party the title is about has no access to this CI. They get prose.

JLS's whole distribution identity is a self-contained artifact — one jar, one
`ghcr.io/anadon/jls` image, batch mode as the headless surface (README "Running JLS from
the jar"). The validator belongs *there*: `jls --validate-kit <dir>` (or a `kit` verb),
in the jar and the container, so the authoring doc's error messages are delivered by the
tool at the moment of the mistake. That single move collapses AC-2 and half of the
authoring doc into one artifact and makes the title's claim mechanically true instead of
aspirational.

This has a hard ordering consequence the issue does not see: **#502 CAP-21 PF-1 intends
to freeze the headless CLI contract**, with a conformance suite and a compatibility
ratchet, and calls it "the first formally frozen public interface of JLS." A kit verb
added after that freeze is a compatibility event; added before, it is free. `FLAGS` in
`src/jls/JLSStart.java:759` is already the single authoritative table with its own
`CliFlagTableTest`. If the validator is going to be a CLI surface — and it should be —
that decision has to be made against #502 PF-1, now, not discovered later.

### 2. Structural validity is the wrong definition of validity

A schema check answers "is this shaped right." An adopting instructor is asking "does this
course work." Those come apart immediately: a perfectly-shaped kit whose vectors do not
grade its own reference solution is worthless and passes AC-2.

The better definition is already written down elsewhere in the cluster. #575 AC-2 requires
a CI lane that "grades the reference solution green and a planted-defect variant red" per
lab. Generalize exactly that into the conformance criterion:

> **A kit is conforming iff `jls kit verify <dir>` can find each lab, grade its reference
> solution green, and grade its planted-defect variant red.**

Missing, misnamed, or malformed parts then surface as *named failures of that run* — you
get AC-2's "reports what is missing or malformed by name" for free, because the runner
cannot proceed without naming what it could not find. Structural requirements shrink to
"whatever the verifier must read to do that," which is the correct minimal schema and the
only one that cannot rot away from the tooling. Under this framing #578's validator stops
duplicating #575's CI lane and becomes the thing #575's lane is an instance of.

Diagnostics should be `LoadError`-shaped, not a new vocabulary: category taxonomy +
location + detail + actionable hint (ARCHITECTURE.md "Error-reporting contracts", issue
#58). And the kit manifest should carry a version header with the `FORMAT 1` /
`NEWER_FORMAT` discipline of issue #79 — a kit authored against a later convention must be
refused by name, never misparsed. Both are house patterns; inventing parallel ones here
would be the real architectural cost of this issue.

### 3. "Kit" must not become a third lab format

#502's abstract already speaks of "a lab in the **CAP-06 lab-as-data format**" — #300's
whole premise is the instructor writing the spec *as data* inside the tool. If #578
defines its own notion of what a lab's metadata looks like, JLS ends up with three
overlapping content descriptions: CAP-06 lab-as-data, CAP-33 kit, and whatever CAP-21's
platform templates consume.

The alignment-preserving definition is purely compositional:

> kit = manifest + N **CAP-06 lab-as-data units** + course-level prose (schedule, rubric).

#578 owns the container and the course level only; "lab" stays CAP-06's noun. And the kit
directory should be declared the *input* format for #502's adapters, so a Gradescope or
PrairieLearn template consumes a kit rather than inventing a fourth packaging. Say this in
the boundary notes; the current notes push the grading engine to #300 and delivery to #502
but never state that the kit is the shared currency between them.

Related: validate only what a machine reads. A 14-week schedule and a point rubric are
institution-specific and no tool consumes them; making them required parts of a validated
schema guarantees every kit ships a schedule wrong for everyone but its author. Those
belong in the authoring doc as *templates*, not in the validator.

### 4. In-tree kits recreate the bottleneck the issue exists to remove

"CI runs it over every shipped kit" quietly assumes kits live here. But kits have a
different license (AC-4 concedes this), a different authorship population, a semesterly
lifecycle, and no reason to be gated on JLS's release train. If every third-party kit must
land in this repo to be validated and shipped, the maintainer is once again the person you
ask — exactly what the Outcome forbids.

The project has a house move for this: the recorded decision with a revisit trigger
(ARCHITECTURE.md "Help delivery", "Plugin trust boundary"). Apply it here:

- the **spec** and the **verifier** ship in-tree and in the jar;
- **exactly one** worked kit ships in-tree, as the reference implementation;
- other kits are ordinary external artifacts (a repo, a release tarball) that verify
  themselves with the shipped verb;
- this repo carries an **index file** of known kits — the cheapest possible discovery
  channel, and the piece that turns a convention into an ecosystem.

That index is, in my judgment, worth more to CAP-33's outcome than AC-1 and AC-2 combined,
and no issue in the cluster owns it.

## Disregarding AC-3 as written

I am explicitly setting aside AC-3's designation of the Donzellini pack as *the* worked
instance. The comment thread escalates "which kit is the flagship" to #517 as a positioning
question; from the trajectory it is not close, and it is not really about flagship status —
it is about **where a convention is derived from.**

A packaging convention derived from content you authored yourself fits your content and
nothing else. That is the standard failure mode of every plugin/kit format that never got
a second implementor. #575's Donzellini pack is a hypothesis with no named counterparty;
#577's CSE 260M corpus is a real course, taught by a real instructor, authored against a
*different fork* of JLS, and it is the first external demand signal the tracker has (#509).
It is the only content in this project that can falsify a packaging convention.

So: **derive the convention from the CSE 260M corpus and prove it by expressing the
Donzellini pack in it.** Restated criterion —

> AC-3′: the convention expresses at least two courses, at least one of which was authored
> outside this project, and the verifier passes on both.

Two independent instances is the minimum that distinguishes a convention from a
description of one artifact. This also disarms the escalated positioning question rather
than deferring it: neither kit is "the flagship"; one is the derivation source and the
other is the falsification test, and the roles are stated. It does add a hard dependency on
#577's external licensing gate (Dr. Siever's written agreement), which should be named here
as a risk rather than discovered when AC-3′ is attempted.

## What I would keep unchanged

- The four-way split with #575/#576/#577 is genuinely well cut, and the comment's refusal
  to merge is correct — convention and enforcement are a different kind of thing from
  content, workflow, and corpus.
- AC-4's insistence on a content license distinct from GPLv3 is right and unusually
  farsighted for a filing this early; a project that preserved `pop_GPLv3.pdf` should get
  this right the first time.
- The refusal to own platform delivery (#502) or the grading engine (#300) is exactly the
  discipline that keeps this cluster tractable.

## Net

The goal is one of the highest-leverage things in the tracker: JLS stops being a tool
someone evaluates and becomes a course someone adopts. The mechanism is aimed one notch
too low — a repo-internal linter over maintainer-authored content, when what the outcome
needs is a self-verifying kit format, a verifier that ships to the author, a convention
derived from a course this project did not write, and an index so the second kit can exist
without asking permission.
