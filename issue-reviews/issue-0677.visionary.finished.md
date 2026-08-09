# Issue #677: TASK-C350-3: every per-job artifact lands at a path derived from the job description, never from dispatch order
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this task is really for

Strip the four criteria away and one sentence remains, and it is the right one:
*the aggregator must be able to pull a job's result out of the store by naming the
job, and must never be handed results by whatever finished first.* Everything else
in #677 — the path scheme, the injectivity re-check, the 1-vs-N path diff — is
scaffolding around that single interface decision. #350 says as much itself: "that
is the interface that makes criterion 2 achievable rather than aspirational."

The end is squarely on the project's arc. JLS has now applied the same idea three
times: the jar and BOM are bit-reproducible with a `.buildinfo` and an
independent-rebuild recipe (`docs/reproducibility.md`); the save format is a pure
function of circuit content, surfaced as `Circuit.stateHash()` — SHA-256 over the
canonical save text (`src/jls/Circuit.java:1548`, `DeterministicSaveTest`, #163/#166);
the VCD emitter omits `$date`/`$version` and sorts signals through a
`TreeMap<String,Sig>` (`src/jls/sim/BatchSimulator.java:393-426`) so its bytes are
content-determined and golden-pinned. The repository is even a Nix flake. This is a
project that already believes artifacts should be named by what they *are*.

Which is why I want to change the route. #677 as written names artifacts by what
they were *asked for* (the description entry) rather than by what they *are* (the
inputs that determine the bytes), and it treats a filesystem path as the interface.
Both are one notch short of what the project already knows how to do.

## Reframing 1 — content-address the store: identity is the digest of the job's input closure

Replace `path(j) = f(desc(j))` with

```
key(j) = SHA-256( circuit stateHash ‖ test-vector bytes ‖ normalized flag vector ‖ JLS version )
```

using `Circuit.stateHash()` for the circuit term and `version.properties`
(`src-filtered/`) for the last — the version term is not optional; without it a
release bump silently serves stale bytes.

What this buys that the description-derived scheme does not:

- **Injectivity stops being a check.** Under #674's scheme, two jobs colliding is a
  defect to be caught at read time and re-caught at write time (#677 AC-4). Under
  content addressing a collision means *the two jobs are the same job* — the campaign
  is over-specified, not broken, and the store is still correct. The injectivity
  machinery survives only as a check that two description entries do not claim the
  same human-facing *label*, which is a much smaller and more honest obligation.
- **AC-3 becomes a cache, not just an assertion.** "Running the same campaign twice
  produces byte-identical per-job artifacts" is currently a property you pay full
  price to observe. Content-addressed, a second run *skips* every job whose key is
  already present. An instructor who fixes one job's inputs and re-runs pays for one
  job. That is the difference between a campaign being a pipeline and being a cache,
  and it is the same argument the Nix flake in this tree already makes about builds.
- **Open Question 4 largely dissolves.** #350 asks whether an evicted job resumes from
  a checkpoint or restarts, and defers to #363. With a content-addressed store the
  answer for the first landing is "restart, and it costs nothing at campaign scale,
  because every job that finished is already banked." Checkpointing stops being a
  prerequisite for campaign survivability and becomes an optimization for one very
  long job.
- **#683 (multi-host) becomes a merge instead of a protocol.** Two hosts running
  disjoint job sets produce two stores that union without coordination, because no
  host is a naming authority and no key can mean two things. The current framing —
  "the same vocabulary carried over the transport" — still needs someone to arbitrate
  collisions and completion. This one does not. That is a large simplification of the
  hardest scope in #350, bought here.

Honest cost: caching and determinism-checking are in tension. If completed jobs are
never re-run, AC-3 is vacuous. Resolve it the way #350 invariant 5 already points:
a `--recompute` mode that re-runs and diffs against the banked bytes, in the
scheduled lane, never the required gate.

## Reframing 2 — the path is not the interface; a manifest is

AC-1 asks for a test that "no artifact path, filename or directory component varies
between a 1-worker and an N-worker run." That test pins the *layout*, and pinning the
layout is exactly what you do not want to promise: it freezes the store's directory
shape into the feature's compatibility surface before anyone has run a 5000-job
campaign on it and discovered they want a zip, a tmpfs, or a shard prefix.

The interface #350 actually needs is `lookup(job) -> present(bytes) | absent`. Give
the store one **manifest** — a single byte-deterministic index emitted next to the
artifacts, one row per description entry, in description order, carrying the job
label, the key, the status, and the digest of each artifact class the job produced.
Then:

- the 1-vs-N test diffs *the manifest*, one file, byte for byte — a stronger and
  cheaper oracle than walking two trees and comparing path components;
- the layout underneath is free to change without touching #679's aggregator, which
  reads the manifest and never the directory;
- the aggregate becomes a pure function of (description, manifest), which is precisely
  the property the sibling review of #679 wants in order to make its offline reporter
  possible;
- content-addressed keys stay machine-facing, and the human-facing view an instructor
  wants (`out/alice/`, `out/bob/`) is *derived* from the manifest — a rendered index or
  a tree of links, regenerable, never load-bearing. That separation of identity from
  presentation is the thing #677 currently conflates, and it is why the issue feels
  forced to make the path carry both jobs at once.

## Reframing 3 — AC-4 asks for detection where the project's idiom is impossibility

"A job that produces no artifact is distinguishable from a job whose artifact was
overwritten" accepts overwriting as a state the system can be in and then asks for
forensics. This project does not normally settle for that: `FileAbstractor` writes
through a temp file and rename so a crash cannot destroy a save; `HeadlessCoreRatchetTest`
and `ArchitectureRulesTest` make the headless discipline unavailable to violate rather
than detectable after the fact.

Make the write **create-exclusive** — `Files.newOutputStream(p, CREATE_NEW)`, or an
atomic no-clobber rename — and a second writer to the same key fails loudly at the
syscall, with both job labels named. "Overwritten" then is not a state that exists,
and no defence-in-depth check is needed because there is nothing left to defend.
"Produced no artifact" is answered by the manifest row, which exists for every
description entry whether or not the job wrote anything. Restated, AC-4 is two
mechanical facts, not an observability property.

## Reframing 4 — AC-3 is not this task's to satisfy, and most of it is already done

Byte-identity of per-job artifacts is a property of the *batch surface*, not of
collection. Collection can only avoid destroying it. The good news is how much is
already there: `JLSStart.displayResults` sorts elements by name before printing
(`src/jls/JLSStart.java:672-677`), the VCD path is `TreeMap`-ordered with no `$date`,
and `BatchSimulationGoldenTest`/`VcdExportGoldenTest` pin both. The exposure is at the
edges — `BatchSimulator.eventTrace` is a `HashMap<LogicElement,…>` keyed on identity
(`:24-25`), and image export goes through `ImageIO` in `jls.edit.CircuitRenderer`,
neither of which has a determinism test the way the save format does.

So AC-3 should not be an acceptance criterion of a storage task. Split it: this task
owns "the store contributes no nondeterminism" (which under reframing 1 is a theorem —
the path is a digest of inputs); a separate small task owns a per-job determinism
harness that runs one job twice, in one JVM and in two, and diffs every artifact class
the campaign is allowed to collect. If image export turns out not to be byte-stable,
that is a finding worth having *before* a campaign promises byte-identity over it, not
discovered when the 1-vs-N gate goes red for an unrelated reason.

## The alternative I will name and not recommend

The maximal version of reframing 1 deletes this task entirely: emit a Nix derivation
(or a Bazel/`make` graph) per job and let an existing build system own naming, caching,
dispatch and multi-host substitution. The store, the injectivity property, the worker
bound, the resume-on-eviction question and #683 all come from a tool that solved them a
decade ago, and the repository already ships a flake. I do not recommend it as the
mandate for the same reason the sibling review rejects a hard `make` dependency: JLS
ships MSIs to Windows students and a campaign must run where they are. But #674 and
#676 should record it as evaluated, because the *shape* it implies — keys, a manifest,
substitution — is what reframings 1 and 2 import without importing the dependency.

## Restated acceptance criteria

1. Artifact identity is `SHA-256(circuit stateHash ‖ test-vector bytes ‖ normalized
   flags ‖ JLS version)`; the derivation is specified in tree and golden-pinned.
2. The store's only consumer-facing interface is `lookup(job) -> present | absent` over
   a byte-deterministic manifest, one row per description entry, in description order.
   No consumer reads the directory layout, and the layout carries no compatibility
   promise.
3. Writes are create-exclusive; a second write to an existing key fails naming both
   jobs. No overwrite-detection logic exists.
4. The 1-vs-N test diffs the manifest byte for byte. Per-job artifact determinism moves
   to its own harness, and a `--recompute` mode re-runs banked jobs and diffs, in the
   scheduled lane.
5. The human-facing view (labelled directories, index) is derived from the manifest and
   regenerable; nothing depends on it.

## Bottom line

Endorse the seam — lookup-by-job, never a completion stream — and rebuild the naming.
Name artifacts by their input closure rather than by their description entry, publish a
manifest rather than a path convention, make overwriting impossible rather than
detectable, and move byte-identity of the artifacts themselves to the surface that
actually produces them. That version is a fourth application of the determinism doctrine
this project already lives by, it turns the second run of a campaign from a cost into a
cache, and it hands #683 a merge where the current framing hands it a protocol.
