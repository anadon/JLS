# Issue #376: TASK-0009: a long stimulus file and a wire-dense circuit load in time proportional to their size, not to its square
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not "three quadratics." The real claim is that **JLS's cost model should be
stated and predictable**, because batch mode is graduating from a convenience
into infrastructure: autograders (`ghcr.io/anadon/jls`, `docs/vcd-interop.md`),
the `riscv/` CPU-scale trajectory (`docs/grand-architecture.md` §2, §6), and
FEAT-009's measurement gate (#335) all price against it. Judged against that
arc the work belongs. All four sites reproduce at HEAD: `SigSim.java:43,64,67,71,74`,
`SigSim.java:85-91`, `Circuit.java:1345,1359,1369`, `WireEnd.java:102-130`.

Judged against that arc, the issue is also **under-ambitious in one dimension
and over-engineered in another**, and both are fixable inside its own scope.

## Reframe A — delete the materialized string; do not speed it up

`initSim` builds `newSignals`, a normalized copy of the *entire* `-t` file, then
opens a second `Scanner` over it. `StringBuilder` turns Θ(N²) time into Θ(N)
time and leaves Θ(N) heap residency of user-controlled hostile input untouched.
The JFR finding the Abstract cites — "the largest allocator of the whole run is
`byte[]` from `SigSim`'s quadratic string concatenation" — becomes a *smaller*
`byte[]`, not an absent one.

This is precisely the anti-pattern the parent feature deletes elsewhere.
FEAT-005 §5 criterion 3 makes `toVcd()`'s absence a `grep`-checkable
requirement, with the reasoning: *"a bypassed-but-present materializer is the
one that gets called next year."* TASK-0009 leaves the mirror-image
materializer on the input side and calls it fixed. FEAT-005's capability
statement is "linear time **and bounded memory**"; TASK-0009 asserts nothing
about memory at all.

The structural route: **a streaming normalized-token iterator.** Wrap the input
`Scanner` in a small iterator that (a) skips from `#` to end of line and
(b) rewrites hex tokens to decimal, and feed the existing parse loop directly
from it. Then `newSignals` disappears, the second `Scanner` disappears, and the
`StringBuilder` is never introduced. Token-sequence identity is by construction
rather than by golden, which is a *stronger* guarantee than P5 asks for. Cost:
comparable to the proposed diff. Payoff: the fix is structural, and FEAT-005's
"no residual materializing path" criterion becomes uniform across all three
children instead of applying only to TASK-0010.

Corollary the issue should adopt either way: add a peak-heap criterion for the
parse, mirroring TASK-0010's. As written, §14 can be fully satisfied while
`-t` memory still scales with file size.

## Reframe B — delete the worklist; do not replace it

The issue offers three replacements for `ends` (`LinkedHashSet`, a `marked`
flag, an `ArrayList` + flag) and calls the choice "the single highest-risk line
in the task." All three are unnecessary. `visited` is allocated **inside** the
outer loop at `Circuit.java:1364`, once per net; that is the only reason
`ends.remove(vend)` at `:1369` has to exist at all — it is the cross-net
visited check in disguise.

Hoist `visited` above `while (!ends.isEmpty())` and the outer loop becomes one
ordered pass:

```
for (WireEnd end : ends) {          // already file order — built at :1346-1352
    if (!visited.add(end)) continue;
    ... BFS as today ...
}
```

`ends.remove()` and `ends.remove(vend)` both vanish. No new collection type, no
`marked` flag, no reset hazard — Threats-to-Validity item 2 evaporates rather
than being managed. Most importantly, §7.10 stage 3's order side condition
("blocks emitted in ascending order of their minimum file position") becomes
**manifest from reading the loop** instead of something P6 has to detect after
the fact. H2's "the iteration order the partition sees is unchanged" stops
being a hypothesis. That is the difference between a fix that is correct and a
fix that is *evidently* correct, and #98's determinism guarantee deserves the
latter.

## Reframe C — sequence after TASK-0007, do not declare the order free

§ Status calls the TASK-0007 relationship symmetric: "whichever lands second
inherits the other's result." Under the project's own trajectory it is not
symmetric. Today the order contract of the partition is a *comment on a field*
(`Circuit.java:76-79`) and the walk lives inside a 120-line `try` that catches
both `Exception` and `Error` (`:1402-1420`). Extract first, and the partition
becomes a pure function with a name — `partition(List<WireEnd>) → List<WireNet>` —
where #98's ordering is a unit test on a component rather than a golden-save
side effect, and where the scaling test measures the walk instead of measuring
`-savetext` end to end (JVM startup, XZ, format negotiation, and 233 ms of
baseline all included, per O3's own numbers). Optimizing first means writing an
end-to-end timing test that TASK-0007 then has to re-home. Recommend stating
0007 → 0009 as the preferred order, with the current text as the fallback.

## The correctness bug is filed in the wrong place

O8/H4 is real, but § Intended Audience scopes it to "circuit-file authors and
third-party tools," and that scoping is probably wrong. `WireEnd.save` writes
` String put` + ` ref attach` for any attached end and emits ` ref wire` lines
only from a loop over `wires` — so **JLS itself serializes the degree-zero
attached end**, and nothing in the saver prevents it. Before this lands,
someone must answer: can the shipped editor reach that state? If yes, this is
silent data loss on ordinary save/load, it warrants a CHANGELOG entry and its
own severity, and "rides along" (Open Question 3) is the wrong disposition.

The larger point: a hand-written P4 fixture pins one instance of a *class*.
`AllElementsRoundTripTest` and `GenerativeRoundTripFuzzTest` exist and did not
catch this, because the generator's shape vocabulary never emits a put-attached
end with no wires. The higher-leverage successor is extending that vocabulary —
degree-zero attached ends, ends with attachment and probe but no wire, and so
on — so the whole "declared-but-unattached" family is found once. Fix the four
lines here; file the fuzz-coverage issue as the real remedy.

## One missing observation that will make the fix look better than it is

`token.matches("-?0[xX][0-9a-fA-F]+")` (`SigSim.java:52`) compiles a fresh
`Pattern` per token, and `new Scanner(line)` (`:49`) allocates a full `Scanner`
per line. Both are linear, so neither shows up in O2's ratios — and both will
plausibly dominate the remaining constant once the concatenation is gone. The
proposed acceptance bound `t(4N) < 8·t(N)` passes comfortably for an
implementation that is linear-but-slow, so §14 can go green over a parse that
is still doing megabytes of needless regex compilation. § Falsification names
these only as an H1-refuted fallback; they should be preconditions. Reframe A
kills both incidentally (one hoisted `static final Pattern`, one `Scanner`).

## On the acceptance bound itself

`t(4N) < 8·t(N)` is the midpoint between linear and quadratic *in the exponent*
— it admits O(N^1.5) permanently. If the point of this work is that JLS's cost
model becomes stated, the durable artifact is not a bound but a **recorded
coefficient** (bytes/ms parsed, ends/ms partitioned) on a named fixture, handed
to the ratchet #335 already exists to hold. Recommend: keep the loose bound as
a cheap required-lane smoke test, and make "coefficient recorded and handed to
#335" the criterion that actually carries the project forward. §14 already
gestures at this with the `riscv-sum1to10.jls` line; promote it.

## Alignment verdict

Strengthens the arc, duplicates nothing (#232 is genuinely a different scope —
in-loop vs. whole-run — and the issue is right to refuse to close it), and
pulls against nothing architectural. One honest observation about proportion:
FEAT-005 budgets 3.8 weeks and this issue carries fourteen sections and sixteen
DoD checkboxes for what reframes to roughly sixty lines of changed code plus
tests, against a bus factor of one. The ceremony is load-bearing for the
determinism constraint (#98) and for golden byte-identity; it is not
load-bearing for `StringBuilder`. Spending the saved review budget on the fuzz
shape-coverage gap above would buy more than another checklist row.

Endorse, with Reframes A and B adopted into § Method, C into § Status, and the
`WireEnd` defect promoted out of "rides along."
