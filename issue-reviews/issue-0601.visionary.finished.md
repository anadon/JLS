# Issue #601: TASK-C332-2: loading a part set holds one part resident at a time, and the peak-memory bound is a recorded measurement rather than a claim
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Stripped of the partition vocabulary, #601 wants one thing: **the load path should
stop being the place where a design's whole size becomes a single heap
allocation.** That is a good and durable goal. #332 says so itself — "streaming
elaboration is a correctness-and-capacity property that benefits every large
single-file design too, not only partitioned ones." Everything else in the issue
(part sets, boundary descriptions, `max_i M(D_i) + M(B) + c`) is scaffolding
around that goal, and the scaffolding is where the trouble is.

## The claim as written cannot be true, and that is not a nitpick

AC-1 asserts peak resident memory during load of an N-part design is bounded by
the largest part. But FEAT-055 keeps elaboration **in one process** (transport is
#333), and its own criterion 4 — plus TASK-C332-5 (#606), which says the harness
"runs both forms in-process" — requires the loaded design to then *simulate*. A
load that ends with a simulatable N-part design has all N parts live at the end
of load. The peak is therefore `Σ M(D_i)`, not `max_i M(D_i)`, unless an
elaborated part leaves the heap before the next one arrives — and nothing in
FEAT-055 provides a place for it to go.

The issue's own refutation clause anticipates the wrong culprit. It says a
`REPLAN:` is owed "if the bound cannot be reached without a whole-design index."
The thing that will force the bound to break is not an index. It is the
elaborated parts themselves. A task that has pre-named the wrong failure mode
will, at measurement time, either quietly redefine "resident" to mean
load-transient scratch (which is a different and smaller claim than #332
criterion 3 makes) or produce a number that is true only for a load whose result
is immediately thrown away.

**Reframing 1 — say the honest bound.** There are two separable properties here:

- (a) *transient* load cost — the scratch structures and the decoded text — which
  genuinely can be bounded by one part plus a constant, and
- (b) *retained* design cost, which is `Σ M(D_i)` in-process by construction, and
  only becomes `max_i M(D_i)` once there is a per-host consumer (CAP-17 #312 /
  FEAT-056 #333).

Property (a) is worth a task on its own and is achievable now. Property (b) is
not this feature's to claim, and #332 criterion 3 should be re-worded to say so
rather than left for #601 to discover with a profiler.

## The load path already materialises the design before `finishLoad` is reached

#601 anchors exclusively on `Circuit.finishLoad`
(`/home/user/JLS/src/jls/Circuit.java:1300-1422`). That is not where the first
whole-design allocation happens. Every container reader in
`/home/user/JLS/src/jls/FileAbstractor.java` decodes the entire circuit text into
a `byte[]` or `String` and hands back a `Scanner` over a `ByteArrayInputStream`:
`readXz` drains through `BoundedInputStream.readAllBytes()` (~:276), `readZip`
uses `readNBytes(MAX_CIRCUIT_TEXT_BYTES + 1)` (:310-312), `readText` does
`Files.readAllBytes` plus a full `String` decode (:334-342). Up to 64 MiB of text
is resident before a single element object exists. Then `loadedElements`
(`Circuit.java:79`) and `elementMap` (`:84`) hold the entire design again as
objects until `:1397-1400` clears them. A "streaming elaboration" task scoped to
`finishLoad` fixes the third copy and leaves the first two.

Worse, the buffering is *deliberate*: the drain-before-return comments cite
issue #111 — a `Scanner` over a live stream keeps a file handle open, and on
Windows an open handle blocks deletion. So "stream the load" is in direct tension
with a recorded decision, and #601 does not know that. The part-file form is
actually the *escape* from that tension (open → drain → close, one part at a
time, handle never outlives the part), which is a real argument for #600 that the
feature never makes. Any streaming design that stays single-file has to answer
#111 with bounded read windows instead.

The save side is symmetric and unmentioned: `FileAbstractor.writeCircuit`
takes `String circuitText` (:219), so a design large enough to need a streaming
loader cannot be written at all. Capacity work that fixes read and not write
buys nothing.

## Reframing 2 — cut at the consumer seam, not at the file-format seam

The elegant version of this task is not "make `finishLoad` incremental over
parts." It is **make elaboration produce a stream that a consumer decides what to
retain.** Today `Circuit.load` → `finishLoad` has exactly one possible output: a
fully-populated editor-shaped `Circuit`. Replace that with a loader that emits
elements and resolved nets to a `Consumer`-style sink, and:

- the batch/HDL/image paths (`-b`, `-export`, `-i`) stop building an editor model
  they never draw — an immediate, user-visible win on designs that exist today;
- FEAT-054's flat element representation (#370) becomes a *sink*, not a rewrite
  of the loader;
- the per-host partition builder CAP-17 needs is another sink, and *that* is
  where `max_i M(D_i)` becomes a true statement;
- AC-2 stops needing a test. The prohibited "convenience index over the whole
  design" is currently `loadedElements`/`elementMap`, which are **fields of
  `Circuit`**. Move them to locals of a `CircuitLoader` scoped to one part and
  the whole-design index becomes *inexpressible*. An API shape that cannot hold
  the wrong thing is a stronger guarantee than a heap probe asserting it did not.

This is the "different seam" answer: the memory bound is a consequence of who
owns the retained structure, not of how the reader is written.

## Reframing 3 — put the constraint in the writer

The reason the loader needs a whole-design id map at all is forward references:
wire ends resolve to elements by integer id (`elementMap.put` at
`Circuit.java:1065`, `getElement(id)` at `:1609`) during `end.init(this)`, and the
file does not promise declaration-before-use. Make the *saver* emit in
dependency order and record that promise in a section version (#319 gives the
per-section must-understand flag for free), and a single-pass loader needs only a
bounded window — no map, no second walk, no measurement argument. Old files keep
loading through today's buffered path, so compatibility costs nothing. That is a
one-time, cheap constraint on the write side replacing an ongoing memory battle
on the read side. #601 never considers it.

## Alignment with the project's arc

The README describes a maintained educational simulator whose energy has gone into
packaging, signing, reproducibility, Wayland, and a documented batch/autograder
contract. Nothing there suggests capacity is the binding constraint on JLS's
usefulness. 64 MiB of *decompressed circuit text* is on the order of millions of
elements — far past anything a student draws. FEAT-055 is banded at 10-16
maintainer-weeks against a bus factor of 1, and #601 alone is 3-4, gated behind a
new multi-file format (#600), section framing (#319), stable naming (#336) and
#353. That is a large bet on a user who may not exist.

The half of #601 that *does* serve the real arc — a load path that is linear,
allocation-honest, and not coupled to the editor model — serves every existing
user, and is available today with none of those four gates. Sequencing it last is
backwards.

## What I would do instead

I am explicitly setting aside AC-1 as written. Concretely:

1. **Split out and land first**, unblocked and on single-file designs: the
   loader-as-producer refactor (Reframing 2) plus bounded container reads that
   respect #111. Evidence: allocation counting and a live-set assertion, not RSS
   — JVM peak RSS is a function of `-Xmx` and GC heuristics, and an RSS number in
   a PR is neither reproducible across runners nor falsifiable.
2. **Fix the writer's ordering** (Reframing 3) behind a section version, which
   deletes the whole-design id map rather than bounding it.
3. **Keep AC-3 and AC-4 verbatim** — the single-file byte-identity guard and the
   no-quadratic-`LinkedList.remove` guard are exactly right, and both are
   testable at step 1.
4. **Re-scope the residue** of #601 to "adapt the streaming loader to a part set"
   once #600 exists. With steps 1-3 done, that is days, not 3-4 maintainer-weeks,
   and it inherits a bound it does not have to argue for.
5. **Raise a `REPLAN:` on #332 now**, before #601 is worked, to restate criterion
   3 as a transient-cost bound in-process and a retained-cost bound only under
   #333. Discovering that at measurement time is how criteria get quietly
   rewritten to match the number, which #332's own protocol forbids.
