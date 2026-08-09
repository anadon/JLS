# Issue #531: FEAT-C21-6: one lab, a 300-submission corpus and golden score vectors prove all four adapters byte-identical in CI — hermetic, containerized, no platform account required
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not "four numbers match." The real end is *instructor trust*: the score a student
sees in Gradescope is JLS's verdict, unmodified — no platform re-rounded it, no
adapter dropped a hidden test, no ordering shuffled the rubric. Byte-identity of
score vectors is a proxy for "every adapter is an information-preserving renderer
of one verdict." That end is squarely on JLS's arc. The project already sells
trust through reproducibility everywhere else: byte-reproducible jar and
`bom.json` with `.buildinfo` (README), `DeterministicSaveTest`,
`BatchSimulationGoldenTest`, `VcdExportGoldenTest`, cosign/attestation on
releases. Extending that discipline to grading artifacts is the right instinct.

The apparatus proposed to get there, however, is the empirical version of a
guarantee this codebase normally obtains structurally — and one of its stated
outcomes ("platform drift surfaces as a red CI lane") is not achievable by a
hermetic fixture at all.

## Reframing A (the main one): parity by construction, not by comparison

#466 already builds `GradeReport` as a byte-deterministic canonical artifact,
deliberately omitting `timestamp`/`hostname`/`time`. And #466 §7.10 makes exactly
the argument this issue declines to make one level up:

> Because both factor through the same runner, verdict-list equality is a
> theorem rather than a test target.

Apply the project's own idiom to the adapters. Define a canonical `ScoreVector`
as a documented projection of `GradeReport` (per-student, per-test, ordered,
emitted by JLS itself — the natural home is a `-report` sibling, not adapter
code). Then require of each adapter one local property: **parse my platform
output back and recover the canonical `ScoreVector` exactly.** Four independent,
cheap, adapter-local round-trip tests replace one four-way comparison.

Why this is better, concretely:

1. **Failure attribution.** A red `CrossPlatformScoreParityTest` says "four
   outputs disagree." A red `PrairieLearnAdapterRoundTripTest` says which adapter
   is wrong, in that adapter's own PR, before the other three exist.
2. **Ordering falls out.** Today #531 cannot be exercised until all four adapters
   land (#525, #526, #528, #530). Under round-trip, each adapter ships with its
   own proof and CAP-21 AC-1 is discharged incrementally.
3. **It dissolves the AC-3 / #524 AC-2 double-ownership** that pass 2 escalated
   and the 2026-08-08 revision had to split in prose. If parity is structural,
   this issue's remaining unique content is the lane-ordering assertion and the
   corpus — nothing to co-own.
4. **It survives KC-21-1.** If a platform rounds or reorders, the round-trip test
   fails at that adapter and names the lossy field, which is precisely the signal
   KC-21-1 wants, delivered per-platform instead of as one undifferentiated red.

I would disregard AC-1 as written ("byte-identical per-student score vectors
across four platform outputs for 300 submissions") as the *primary* evidence. It
is a weaker statement than "each adapter is a lossless renderer of one canonical
vector," it costs more to run, and it is harder to debug when red.

## Reframing B: hermeticity and drift detection are in tension — split them

The Outcome claims "platform drift surfaces as a red CI lane, not as a broken
course mid-semester," and AC-2 pins "each platform's *documented* contract" in
the fixture. Those cannot both hold. A hermetic fixture pins **JLS's
transcription** of a vendor spec. When Gradescope changes `results.json`, nothing
in the hermetic lane changes colour — it stays green while every real course
breaks. Hermeticity buys maintainability (a single maintainer with no four
platform accounts — genuinely valuable, keep it); it cannot buy drift detection.

The project has already cut this seam correctly once, for Wayland: a blocking
per-push hermetic rig (`scripts/wayland-rig.sh`, headless sway) *plus* a scripted
once-per-release check on real hardware (`docs/wayland-desktop-checklist.md`),
because "a headless software-rendered rig can diverge from real GPU-backed
compositors." That is the same epistemic situation, and the same answer applies:

- **Blocking, hermetic, per-push:** adapter conformance against the pinned spec
  transcription. What #531 actually builds.
- **Advisory, scheduled, non-blocking:** a spec-watch lane that fetches each
  vendor's documented contract by URL and fails on a content-hash change. Cheap,
  and it is the only mechanism that can detect drift.
- **Once-per-release, human, credentialed:** a `docs/grading-platform-checklist.md`
  in the shape of the Wayland desktop checklist — the maintainer or a
  participating instructor runs one real submission through one real course.

Rewriting the Outcome sentence to say what the fixture can prove, and moving the
drift claim into a scheduled watch lane, is a one-paragraph edit that makes the
issue honest and gains a capability it currently lacks.

## Reframing C: one generated corpus, not three hand-held 300s

Three separate "300 submissions" now exist (#300, #506, this issue), a
duplication the pass-1 note already flagged for the maintainer. Three hundred
checked-in `.jls` files is repo weight, a merge-conflict surface, and — ironically
— its own byte-determinism liability.

JLS's idiom is generators plus goldens: `GenerativeRoundTripFuzzTest`,
`ContainerMutationFuzzTest`, `CircuitTextBuilder`. Build one seeded
`SubmissionCorpus` generator: a golden lab circuit plus a declared mutation
taxonomy — correct, off-by-one bit width, inverted output, missing wire, HiZ
contention, memory left uninitialized, timeout (status 1), unparseable file,
empty file, wrong top-level name. Deterministic from a seed, N=300 for the full
lane, N≈30 taxonomy representatives for the fast lane. One owner, consumed by
#300, #506 and this issue alike.

Note also that "300" is a narrative number (a course of 300 students), not a
testing argument. Two hundred and seventy near-duplicate correct submissions add
seconds, not coverage. The number that matters is the count of *distinct failure
classes* an adapter must render faithfully — including the ugly ones (exit 3 vs
exit 1, a submission that fails to load, a report that cannot be written) that a
prettily-generated corpus of plausible student work will not contain unless the
taxonomy names them.

## Reframing D: discharge determinism by content-addressing, in the project's idiom

AC-4 ("two consecutive full corpus runs produce identical bytes") is a bespoke
determinism check invented inside one fixture. The project already has a better
shape: emit a manifest of per-student report SHA-256s and golden it, exactly as
`SHA256SUMS`/`.buildinfo` work for releases. Then (a) the check is a one-line
golden diff rather than a second full corpus run, (b) the manifest is an artifact
instructors can diff between their run and the reference — which is the actual
user-facing value of determinism, and (c) it composes with the reproducible-build
story the README already tells.

Relatedly, the 2026-08-08 comment worries that determinism must hold *across
machines*, not just across runs. Under reframing A that property lives where the
function lives: `GradeReport` is a pure function of (circuit, vectors,
expectations), pinned by #466's P6/P7 and by running that one golden on the
existing Linux/Windows/macOS CI legs. Re-proving environment-independence inside
a four-adapter container fixture is expensive and indirect.

## The out-of-the-box alternative, stated plainly

If the canonical `ScoreVector` is right, JLS does not need to own four vendor
contracts forever. Ship **one** adapter (Gradescope, largest install base), the
canonical artifact, and a documented ~50-line reference adapter template — and
let instructors write PrairieLearn and nbgrader glue in an afternoon. CAP-21's
four-way parity claim is marketing surface; the student-facing outcome is "my
score is JLS's verdict." KC-21-3 already contemplates shipping three platforms
rather than one scraped one; the same logic extends to one platform plus a
contract. For a single-maintainer project carrying a 1,145-line `ci.yml`, three
GUI-boot rigs, Agda proofs and multi-arch release plumbing, permanently owning
four vendor integrations is the largest non-JLS surface in the tree. I would not
block CAP-21 on this, but the decision deserves to be made deliberately rather
than inherited from the capstone's abstract.

## What I endorse without change

- Dedicated adapter lanes, not entries in the core toolchain matrix. Correct and
  consistent with the no-toolchain-matrix constraint.
- No platform account, no network dependency on any platform service. This is
  what makes the kit survivable by one maintainer.
- The lane-ordering guarantee (contract conformance evaluated before adapter
  lanes) as this issue's own, post-split. It is the right seam: one named
  conformance failure beats four confusing adapter failures.

## If this issue is rewritten

It shrinks, and gains leverage: (1) define and golden the canonical
`ScoreVector`; (2) build the seeded `SubmissionCorpus` generator with its failure
taxonomy, owned once for #300/#506/#531; (3) assert the conformance-before-
adapters lane ordering; (4) emit and golden the per-student hash manifest. Per-
adapter round-trip parity moves into #525/#526/#528/#530, where it can be proved
the day each adapter lands. The drift claim moves to a scheduled spec-watch lane
plus a release-time checklist. That is a 2 mw issue with more assurance than the
3–4 mw one filed.
