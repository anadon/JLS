# Issue #577: FEAT-C33-3: a real course's labs live in the tree — the CSE 260M corpus lands as compatibility fixtures and ships as the first course kit
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Two artifacts are fused under one feature id, and they serve different ends:

- **A compatibility oracle.** AC-1/AC-2: real `.jls` files, produced by a *foreign JLS
  lineage*, load / simulate / grade here, and every divergence from the origin fork is
  named. The value is evidence about this fork's fidelity. It exists whether or not a
  course kit ever ships.
- **A content product.** AC-3/AC-4: JLS adapts someone else's course into a JLS-branded
  kit, gated on a content-licensing negotiation.

The ordering-correction comment already found the seam between them (per-criterion
`ordering_after`, AC-1/AC-2 unblocked, kit half behind #509). It stopped one step short:
having established that the two halves have different dependencies, different gates and
different failure modes, the natural conclusion is that they are different *work*, and
each is better served by a framing the issue never considers.

## Check against the project's arc

The repository already does the compatibility-oracle thing, twice, and has a house style
for it:

- `test/fixtures/fork-4.6-shiftregister.jls` is a fixture **written by bsiever's own
  loader+writer at a pinned revision** — `test/jls/ShiftRegisterTest.java:37-43` records
  the provenance in the test's own javadoc and states the obligation ("must load upstream,
  wire up, and simulate to the fork semantics"). This is #577 AC-1/AC-2 at n=1, already
  shipped, already against *this exact fork*.
- `test/fixtures/legacy-4.1/README.md` is a corpus directory that is deliberately empty,
  with provenance policy, a named fallback source, and a refusal to fabricate synthetic
  files and mislabel them as authentic. It has stayed empty because it was one-off
  maintainer work with no mechanism attached.

So the fixture half is not new territory — it is the third instance of a pattern the
project keeps hand-rolling. Meanwhile #311 (CAP-16, Logisim-Evolution corpus, "every loss
named") and #513 (CAP-29, three more importers on "shared loss-report infrastructure")
need the same machinery for foreign *formats*, and #509's item 2 needs it for the same
files #577 owns. Four consumers, four bespoke harnesses, unless someone cuts the seam.

## Reframing 1 — build the harness, not the corpus (this is the load-bearing one)

Cut along **foreign lineage**, not along **CSE 260M**. The reusable artifact is a
corpus harness that takes a directory of third-party circuits plus a provenance manifest
(source URL, retrieval date, sha256, license/permission record, disposition) and produces
a dispositioned report: loaded / failed-to-load with `LoadError` category / simulated /
graded / diverged. AC-2's "named, dispositioned finding — never silently dropped" is
exactly the loss-report vocabulary #311 and #513 are going to build anyway; sharing it is
cheaper than reconciling three of them later.

Stronger still, AC-2 as written is a *manual* comparison against "the origin fork" — it
presupposes someone ran bsiever/JLS by hand and remembered the answer. JLS has everything
needed to make that mechanical instead: headless batch mode, a stdout format that is a
declared stability contract (`docs/batch-interface.md`), VCD export, golden tests, and a
pinned container runner (`ghcr.io/anadon/jls`). Run **two jars over one corpus and diff**.
That turns AC-2 into a differential oracle that reruns on every change, and it partially
*executes* #509's AC-2 fork-delta audit rather than only reading it: behavioural deltas
fall out of the diff, and only source deltas need human reading. It also retroactively
gives `legacy-4.1/` a way to become non-empty when a 4.1 jar is obtainable, and gives
CAP-16 a differential target (Logisim-Evolution's own CLI) it currently lacks.

Risk to name: bsiever's fork may not emit byte-identical stdout, so the diff needs a
normalization layer, and that layer is where a real divergence can hide. Keep it explicit
and small, and record every normalization rule as a finding in its own right.

## Reframing 2 — the deliverable is a report, not a green lane

AC-1 ends at "a CI lane fails the build on a regression." A green lane is invisible to
Dr. Siever. The artifact that moves #509 is a **published, human-readable compatibility
report**: your Spring-2025 labs, N of them, M behave identically here, these three do not
and here is why, each named. That is the document the "well enough matured" conversation
(#509 AC-1) should be conducted *against*, and it is the only output of this issue that a
non-contributor can evaluate. It should be a release artifact or a docs page, regenerated
by the lane, not a buried assertion. The ordering correction got fixtures ahead of the
conversation; this makes the fixtures *speak* to the conversation.

## Reframing 3 — I am disregarding AC-4, and here is why

AC-4 says the adapted kit conforms to #578's convention and runs through #576's workflow.
I would not ship an adapted CSE 260M kit at all. Reasons, in the order I weight them:

1. **It creates a second flagship.** The dedup comment on this issue already recorded the
   unresolved positioning question: #575's Donzellini pack is #578's designated worked
   instance, while this issue calls itself "the first course kit" and the capstone's demo
   slice. Shipping both leaves JLS publishing two competing course products with one
   maintainer and KC-33-2's per-lab review obligation on each.
2. **It proves the weaker thing.** #578 AC-2/AC-5's real goal is that *a third party* can
   author a conforming kit without asking a maintainer. A kit that JLS itself adapted
   proves nothing about that. A kit that Dr. Siever publishes, that our validator accepts
   unmodified, proves it completely.
3. **It puts a negotiation on the critical path for no gain.** The ask "license us your
   course materials so we can adapt and republish them" is large. The ask "run the
   validator over your existing repo; if it passes, we link it from the kit index" is
   nearly free, and he keeps ownership, hosting and the ability to revise mid-semester —
   which is what an instructor actually wants.

So: **the kit half becomes an external-kit index entry, not an in-tree kit.** #578 grows a
notion of a *referenced* kit (name, author, URL, license as stated by the author, last
validated commit/tag, validator verdict); CSE 260M is entry #1; CAP-33 AC-3 and #578 AC-5
are discharged by a real external author rather than by a review of our own work. If the
index needs a worked in-tree example, #575 already is one, with clean original content.

## Reframing 4 — pin, don't necessarily commit

AC-3's licensing gate exists because committing the corpus is redistribution. For any file
whose published licence is not already clear, the corpus does not need the file in-tree —
it needs the file's *behaviour*: URL + sha256 + committed golden outputs. Goldens are small
facts about behaviour, not the labs. A fetch-and-verify lane then reproduces the check
without the project redistributing anything, and the licensing gate on those files
evaporates rather than being negotiated. Honest cost: this trades hermetic CI, which this
repo values highly (reproducible jar, `repro-installers.yml`, pinned everything), for
licensing cleanliness. The middle path is the right one — commit what is clearly
redistributable, pin-and-fetch the rest on a scheduled (non-required) lane, and let AC-2's
disposition machinery record *why* each file is in whichever bucket. That is the same
mechanism, used on a licence ground rather than a load ground, exactly as the ordering
comment §4 suggests.

A second reason to prefer this: if the corpus contains reference solutions, committing
them publicly damages the course. That alone could make the answer to AC-3 "no" for
reasons that have nothing to do with copyright.

## What I would keep unchanged

AC-1 and AC-2 as outcomes, and the ordering correction that unblocks them. The instinct
that real files from a real course are worth more than any synthetic fixture set is right,
and it is the same instinct that produced `ShiftRegisterTest`'s fork-interop fixture.
The `-t` grammar and stdout contract are already stable enough to grade against, and
`examples/autograde/autograde.py` + `test/jls/AutogradeBridgeExampleTest.java` show the
subprocess-grading pattern this lane should reuse rather than reinvent.

## What would change my mind

If Dr. Siever's own preference, once asked, is that JLS host and maintain the kit — some
instructors want exactly that — then Reframing 3 is wrong and AC-3/AC-4 stand as written.
That is a question to put to him in the #509 conversation, alongside the compatibility
report, and it is cheap to ask. Ask it before building either kit half.
