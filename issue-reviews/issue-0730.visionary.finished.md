# Issue #730: TASK-C554-3: the suite's output is machine-readable, so the perf doc and the scheduled lane consume it without anyone hand-editing a number
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The diagnosis is right and it is not about benchmarking. CAP-28 (#512) says the deficit
is epistemic — JLS scored 2/5 on scale/perf "for lack of receipts, not lack of speed" —
and #730 is the one task in that tree that decides what a receipt physically *is*. Every
other task in the chain (#554 measures, #555 publishes, #557 defends) consumes bytes this
issue defines. So this is the load-bearing task, and "output contract only" undersells it.

Read against the project's arc, this is not a new kind of work at all. JLS already ships
`bom.json` (CycloneDX), `SHA256SUMS`, `.buildinfo` in the reproducible-builds JVM format,
cosign signatures and provenance attestations, and `docs/reproducibility.md` explains
with unusual care *what each guarantee does and does not cover*. The differentiator this
project is actually building is **checkability**, and a performance record is another
attestable artifact in that line — not a benchmark side-file. Every reframing below
follows from taking that seriously.

## Reframing 1 — this is a run record on the shipped batch surface, not a benchmark output

`docs/batch-interface.md` is already a normative stability contract ("any change requires
a CHANGELOG entry and either a major version bump or a compatibility flag"), and the
container image's stated audience is autograders and CI. Emit the record from the product
under that contract — a file flag shaped like `-vcd`, e.g. `-stats run.json`, so §1's
"stdout carries only the simulation results" is untouched — rather than from a private
harness format only the suite understands.

The payoff is that the format acquires consumers who are not the perf doc: #442's
event-count ratchet, #413's census, #393's zero-delay work, autograders wanting "how many
events did your adder take to settle" (a gradeable hazard/glitch metric no competitor
publishes), and #560's head-to-head. AC-2 as written — "every field #555 publishes and
#557 compares is present" — is a two-consumer requirement, and a two-consumer format gets
designed to two consumers. Designing to the batch contract's audience produces a better
artifact for the same effort, and buys the CHANGELOG discipline that keeps it stable.

## Reframing 2 — the format needs its own version, and the issue never mentions one

This is the concrete structural gap. #557's lane will compare a record written today
against one written months later; #555's doc will cite records older than the code that
reads them. The repo has a first-class, recorded answer to exactly this (#79): a `FORMAT`
version line ahead of the payload, a frozen tag table so text never reaches
`Class.forName`, and an explicit `NEWER_FORMAT` refusal — "this file needs a newer JLS" —
instead of misparsing. `FormatHeaderTest` and `SaveTags` pin it.

A perf record without that will be silently misread the first time a field's meaning
changes, which is the same class of failure the issue exists to prevent, one level down.
**Add to acceptance: the record carries a format version; a reader refuses a newer version
loudly rather than parsing it partially.** Also decide the emitter deliberately: the only
JSON code in tree is `jls.hdl.yosys.JsonValue`, a *parser*, so JSON means writing and
testing an emitter. That is fine, but it is a cost the issue does not price, and a
line-oriented `key value` grammar in the style of the save format is a real alternative
with existing tooling in tree.

## Reframing 3 — AC-3 is unachievable as worded; provenance-tag fields instead

"The environment fields (hardware, JDK, flags) are captured by the harness rather than
typed in" cannot be met for *hardware*. A JVM sees `os.arch`, `availableProcessors`,
`maxMemory` — not the CPU model, not the memory clock, not whether turbo was on. Yet #555
AC-2 demands an independent party reproduce the number "from the doc alone", which needs
the CPU model. Something must be typed in, so an absolutist AC produces either a lie or a
`/proc/cpuinfo` hack that is wrong on macOS and Windows.

The better goal — and it is the project's own epistemic habit, visible in the README's
careful scoping of checksums vs attestation and in keystone-c's explicit **unmeasured**
markers — is: **every field carries its provenance.** `measured` (harness-captured, never
typed), `declared` (operator-supplied, marked as such and carried through to the doc with
that marking). A typed-in field that announces itself is honest; a typed-in field that
looks measured is exactly the failure #730 names. I am disregarding AC-3's literal
wording in favor of that.

## Reframing 4 — the boundary excludes the one transcription most likely to go wrong

"Ceiling bands and `simulation-budget.properties` stay #442's." But look at the two key
spaces. #442's file carries, per fixture: path, clocking regime, expected event count,
events-per-cycle, ns/event ceiling, bytes/event ceiling. #730's record carries, per
fixture: throughput, node count, clocking regime, environment. Same fixtures, same regime
vocabulary, overlapping numbers, two files, two owners — and the re-baseline step ("a
reviewable one-file diff with a stated reason", #442 §7.7) is *precisely a human retyping
a measured number into a declared file*. #730 abolishes hand-editing everywhere except
the place where it is institutionalized.

The elegant cut is one key space, two roles: **measured records** (many, timestamped,
produced) and **the declared file** (one, reviewed, asserted) — with the declared file a
mechanical *projection* of a record. A re-baseline becomes "regenerate from record R,
review the diff, state the reason", and a drift test asserts the two artifacts use the
same fixture identifiers and the same regime vocabulary, in both directions, the way
`ExtensionPointCatalogTest` cross-checks the seam catalog. That is a small addition to
#730's scope and it removes a whole category of future divergence. Without it, #442 and
#730 will disagree about what "testgen-vector" means within a year.

## Reframing 5 — replace AC-4 with a drift test, because AC-4 is unfalsifiable

"A consumer reading the file requires no hand-editing step, demonstrated by at least one
committed consumer" proves nothing about the second consumer, and "no consumer needs a
value the format does not carry" quantifies over consumers that do not exist yet. The
repo already has the right instrument and even the right name for it: **documentation
drift tests** (`CliFlagTableTest`, #71: "the flag table in JLSStart is the single
authoritative CLI specification, usage() is generated from it… these tests fail if anyone
reverts to a hand-maintained list on either side"; `FileFormatSpecTest`, #79, for
`docs/file-format.md`).

Make `docs/performance.md`'s number blocks generated from committed records, and add
`PerformanceDocDriftTest` asserting the regenerated block is byte-identical to the
committed one. Then "no hand-editing" stops being a property someone demonstrates once
and becomes a property CI enforces on every push — with **no benchmarking in the fast
lane at all**, since the check is a pure regeneration diff. That also dissolves a large
part of #557's noise problem: the deterministic half of a published claim (fixture
sha256, census, event counts) is checkable exactly and cheaply; only the noisy half needs
a scheduled machine.

## Fields the issue's list is missing, concretely

- **"Node count" is not a scalar.** keystone-c's census of the CPU fixture reads
  `elements(all,recursive)=1551, wireNets=297, maxBits=32`, of which 225 are logic
  elements, plus 810 `WireEnd` and 513 `Wire`. Publishing one number without saying which
  makes #560's head-to-head against Digital and Logisim-Evolution meaningless. Carry the
  whole census vector — the projection #413 already defines.
- **Fixture identity by content hash, and jar identity.** A record that cannot be tied to
  a specific fixture and a specific jar has no provenance chain, and the project hashes
  and attests everything else it publishes. Free and idiomatic here.
- **The deterministic anchor: retired event count.** It is the one exactly reproducible
  quantity in the whole measurement (#442 O4: 194, three runs) and it is what lets a
  reader distinguish "the engine changed" from "the machine was noisy".
- **Phase split.** `initSimulation` vs `runEventLoop` — keystone-c §2 measures 0.568 s vs
  0.742 s at 6004 cycles, and flags `SigSim`'s O(n²) `String +=` as dominating end-to-end
  wall time. A record that reports one aggregate number will publish that bug as JLS's
  simulation speed.

## Alternatives I considered and would not take

- **JMH's `-rf json` schema.** Attractive because `jmh-visualizer` and friends consume it
  free and its environment block (`jdkVersion`, `vmName`, `vmVersion`, `jvmArgs`) is
  exactly AC-3's captured half. But JLS's workload is a whole-program run, not a
  microbenchmark, and keystone-c already records that JMH is not a dependency. **Crib its
  environment field names; do not take the dependency.**
- **`github-action-benchmark` + `customSmallerIsBetter` JSON.** Would hand #555 charts and
  #557 alerting nearly free. Rejected: it stores history in a `gh-pages` branch outside
  the reviewed tree — the opposite of "the number is a reviewed diff" — and adds a
  third-party action to a repo that tracks its OpenSSF Scorecard.
- **Java `.properties`, matching #442.** Rejected for the record itself: the environment
  block is nested and per-fixture, and flattening it is where hand-editing creeps back in.
  Keep properties for the *declared* file, per Reframing 4.

## Verdict

**endorse-with-reframing.** The premise — published numbers go wrong at the transcription
step — is correct and this is the right task to own the fix. Build it, with: a versioned
record with a loud refusal on newer versions (#79's pattern); provenance-tagged fields
replacing AC-3's absolutism; the census vector, fixture and jar hashes, event count and
phase split added to the field list; a drift test replacing AC-4's demonstration; and the
boundary with #442 redrawn so the declared file is a projection of a record rather than a
second, independently-typed number file. Emitting it from the shipped batch surface under
`docs/batch-interface.md`'s stability contract is the version of this task that leaves the
project stronger than a benchmark needed it to be.
