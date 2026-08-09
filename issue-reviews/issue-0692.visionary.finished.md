# Issue #692: TASK-C524-4: the verdict envelope is byte-identical across container boundaries — no timestamp, no ordering wobble, no locale in a grading artifact
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the wrapper and #692 asks for one thing: **a grading verdict must be a pure
function of (circuit, vectors, expectations), and nothing else.** Byte-identity is
the observable; purity is the property. The four CAP-21 adapters are the stated
beneficiary, but they are not the deepest one — an adapter only needs *stable
semantics* to parse. The capability that byte-identity actually unlocks is
**auditable grades**: an instructor can publish an artifact, a student can
regenerate it, and a disagreement is a diff rather than an argument. #466 §7.9
already gestures at this ("determinism is a contract item and not a nicety") and
#466's own worked-lab framing (200 submissions, one command) only pays off if a
re-run is checkable.

That end is worth having and sits squarely on the project's arc. The route #692
picks to reach it is the wrong one, and — this is the load-bearing observation —
**this repository has already solved this exact class of problem twice, better,
and #692 cites neither solution.**

## The two prior solutions #692 re-derives from scratch

**1. Canonical serialization (#166), in-tree today.** `test/jls/DeterministicSaveTest.java`
opens with the thesis verbatim: *"a circuit's serialized form is a pure function of
its content."* The mechanism is not an enumeration of hazards. It is a **canonical
form** — elements emitted sorted by stable id, file-local ids assigned in that same
order (`src/jls/elem/StateMachine.java:251` sorts states "by the states' own
content" for exactly this reason; `src/jls/Circuit.java:1471` records a
platform-normalization decision made "or determinism (and stateHash) would" break)
— plus a **content digest surfaced from the model**, `Circuit.stateHash()`
(`src/jls/Circuit.java:1548`), pinned by `stateHashIsContentDetermined()`.
Pre-change, the same fixture saved differently in 261 of 276 lines. That defect was
larger than anything in an xUnit envelope, and it was closed by *design*, not by a
checklist of nondeterminism sources.

**2. The perturbed-rebuild gate (#185), in CI today.** `docs/reproducibility.md` §4
is #692's AC1, already built and already green: rebuild the same commit *from a
differently-named workspace path with `TZ=Pacific/Kiritimati`, `LC_ALL=C`, and
`umask 077`*, one runner, byte-compare, upload both artifacts to `diffoscope` on
divergence. §2's "Verified irrelevant" list — build path, timezone, locale, umask,
wall-clock time — is #692's AC4 ("elements outside the guarantee, listed by name")
in a vocabulary the project already speaks, next to §1's "specified artifacts" table
that AC4's list belongs *in*.

#692 proposes instead: enumerate hazards one at a time (timestamp, hostname,
duration, map iteration, locale, path separators), then stand up a bespoke
two-container matrix varying hostname/locale/timezone/uid. That is a *sampling*
instrument aimed at a *structural* property. It can only ever prove the two points
you happened to sample; it can never close the list, because the list has no
natural end (Turkish-i, NFC/NFD in student filenames, `hi-IN` grouping, `%s` on a
`Formatter` that took the default locale three call-frames down). And it is
strictly weaker than the gate the project already runs on every push.

## The reframing: make the property structural, then make it disappear

**I am disregarding AC1 as written and rewriting AC2–AC4.** Same outcome, three
different mechanisms, all of which already have in-tree prior art.

**(a) One canonical-form spec, one chokepoint, one purity ratchet.** Declare in
`docs/batch-interface.md` that the envelope has a *canonical form* — UTF-8, LF, a
declared element and attribute order, `Locale.ROOT` numeric formatting, logical
forward-slash artifact paths relative to a declared root — and that `GradeReport` is
the sole path from verdicts to bytes. Then enforce purity **statically**, in
`test/jls/ArchitectureRulesTest.java`, which already imports `target/classes` with
ArchUnit and already runs zero-tolerance rules of exactly this shape
(`onlyTellUserDependsOnJOptionPane`, `hdlInternalsAreOnlyWiredFromTheCli`). One new
rule: *no class in the report-writing package may depend on* `System.currentTimeMillis`,
`java.util.Date`/`Instant.now`, `Locale.getDefault`, `TimeZone`, `InetAddress`,
`System.getenv`, `File.separator`, `HashMap`/`HashSet` iteration, or the
locale-defaulting `String.format(String, Object...)` overload. That is a **total**
guarantee over the ambient-input space, checked in milliseconds, versus a sampled
one costing a CI matrix. It is also the only form of the guarantee that survives
contact with future contributors: a new `String.format` in the writer fails the
build, rather than waiting for someone to install a comma-decimal locale.

**(b) Perturb in-process, not across containers.** For the dynamic half, reuse
#185's shape at runtime instead of building new infrastructure: one JUnit extension
that sets `Locale.setDefault`, `TimeZone.setDefault`, `user.name`, `user.dir`,
`line.separator` and `file.encoding` to adversarial values around a single
`TestVectorRunner` invocation, and asserts byte equality against the unperturbed
run. Cheap, deterministic, and it covers a dozen axes rather than two.

**(c) If you want a *container* claim, make it a real one.** The genuinely
interesting cross-boundary test is not two hostnames — it is **two instruction set
architectures**. The README already ships `ghcr.io/anadon/jls` as `linux/amd64`,
`linux/arm64` and `linux/riscv64` under one tag, and that image is the actual
autograder deployment vehicle. Running the same lab on amd64 and riscv64 and
demanding byte-identical envelopes tests word size, byte order and library
formatting differences that no locale variation reaches, on infrastructure that
already exists. If #692 keeps one cross-boundary AC, it should be this one.

## The bigger prize the issue never reaches for: a self-attesting envelope

Byte-identity as #692 frames it is a *CI-time* property — proven once, in the
project's own lab, and thereafter unobservable by the people who care. Lift it into
the product, exactly as #166 lifted canonical bytes into `Circuit.stateHash()`:

> `jls -b -t v.txt -check e.txt -report r.xml c.jls` also emits a
> `report-digest` — a SHA-256 over the canonical envelope bytes, with the frozen
> contract version (#690's queryable version) mixed in.

The consequences compound across the whole CAP-21 program:

- **The four adapters stop diffing files.** Interop conformance becomes one hex
  string compare. #524's `CliContractConformanceTest` shrinks accordingly.
- **Grades become auditable in the field.** An instructor publishes a digest with
  the rubric; a student's re-run either matches or does not. That is a capability
  the enumeration route never delivers, and it is the thing `examples/autograde/`'s
  string-diff pattern (#466 O8) has been approximating badly for years.
- **AC4's escape hatch gets teeth.** "Outside the guarantee" stops being prose and
  becomes *not in the digest input* — a mechanically checkable boundary.
- **It composes with the existing provenance story.** The project already reasons in
  digests and attestations (`SHA256SUMS`, `bom.json`, cosign, `.buildinfo`); a
  verdict digest is the same idea one layer up, and `docs/reproducibility.md` §6
  "Future work" is where the row belongs.

One honest caveat: a digest is only as strong as the canonical form beneath it, so
(a) is a genuine prerequisite for this, not an alternative to it. Digest first
without canonicalization would be false confidence.

## Duplication and pull

- **Duplicates #466.** AC2's timestamp/hostname/duration clause *is* #466 H5, P7,
  §7.6 and an unchecked Definition-of-Done box. As written, #692's only original
  content is locale, ordering, paths and the harness — and the reframing above is
  what makes those four into one coherent deliverable instead of four more
  checkboxes on someone else's issue.
- **Pulls against CI discipline.** This project is visibly careful about CI weight
  (advisory-only newest-JDK lane, `gui-wayland` on a nightly cron). A
  four-axis multi-container matrix is the heaviest possible instrument for the
  lightest possible property, and (a)+(b) get more coverage for near-zero runtime.
- **Strengthens the arc, if reframed.** "Canonical form + purity ratchet + content
  digest" is now the project's third application of one idea (save format, build
  artifacts, verdict envelope). Naming it as a *recurring architectural pattern* in
  `ARCHITECTURE.md`'s recorded scope decisions is worth more than any single AC
  here, and would let #524's sibling tasks and the later CAP-21 adapters inherit it
  without re-arguing determinism each time.

## What I would keep verbatim

AC4's escape-hatch discipline — *listed by name in the contract, rather than left to
be discovered by an adapter* — is the best sentence in the issue, and it is the
right instinct: a bounded, declared exception list beats an unfalsifiable promise of
totality. The Boundary line ("what a test *says* is CAP-06's; how it is serialized
is here") is the correct seam. Keep both; replace the machinery between them.
