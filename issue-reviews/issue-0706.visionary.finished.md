# Issue #706: TASK-C528-1: the in-tree PrairieLearn external-grader image drives batch mode and nothing else
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the CAP-21 vocabulary away and the ask is small and good: *an instructor
running PrairieLearn should be able to grade a drawn circuit without writing a
harness.* That is squarely on the project's arc. `grand-architecture.md` §1
already names the headless batch surface as a **co-equal front end**, not a
side door, and `docs/batch-interface.md` is already a normative stability
contract with its own §6 ratchet. The README already advertises the delivery
mechanism — `docker run --rm -v "$PWD:/work" ghcr.io/anadon/jls -b -t tests
circuit.jls`. CAP-21 is the missing last inch: nobody has walked that command
onto a platform an actual course uses. The goal is endorsed without
reservation.

What I want to contest is the **artifact**. The issue makes "an image" the
outcome. Three things follow from that choice that I think are wrong, and one
reframing dissolves all three.

## Reframing A — the deliverable is an adapter file; the image is one `FROM` line

JLS already ships a headless grading image, and it is not a trivial one.
`resources/packaging/Dockerfile` carries: a digest-pinned `ubuntu:26.04`, an
`APT::Snapshot` pin so package versions cannot float, a two-stage
`jdeps`/`jlink` runtime (~50 MB instead of a JDK), `binutils` because the
archive JDK ships unstripped natives, `fontconfig` + DejaVu because headless
`-i` export still needs a font, `SOURCE_DATE_EPOCH` from the HEAD commit, and
a comment explaining that Ubuntu is the base *only* because Temurin and
Liberica publish no `riscv64`. `scripts/build-container.sh` exists so the
local and CI recipes "cannot drift". The published image is multi-arch,
cosign-signed, and attested.

#706 AC-1 asks for a second image that "pins the JLS build by digest and
builds reproducibly from the committed definition". #694 (Gradescope) asks for
a third. If #526 and #530 grow one each, the tree carries four platform images
plus the shipping image plus the devcontainer, at bus factor 1 — five places
to re-learn the snapshot pin, five signing stories, five multi-arch matrices.
That pulls directly against the one-recipe discipline the existing scripts
were written to enforce.

The whole PrairieLearn-specific surface is: read `/grade/data/data.json`,
find the submission under `/grade/student`, invoke batch, write
`/grade/results/results.json` in PL's schema. That is a script. Concretely:

```dockerfile
FROM ghcr.io/anadon/jls@sha256:<digest>
COPY prairielearn/grade /opt/jls/adapters/prairielearn/grade
ENTRYPOINT ["/opt/jls/adapters/prairielearn/grade"]
```

Every AC in this issue is then satisfied by inheritance rather than by
re-derivation. "Pins the JLS build by digest" — the `FROM` *is* the pin.
"Builds reproducibly" — the base is already reproducible; the delta is one
`COPY`. "Structurally incapable of driving an interactive session" — the base
has no display stack and its entrypoint is `java -Djava.awt.headless=true`;
the property is inherited, not re-established.

Better still, ship the adapter as a **standalone file plus a copy-in
snippet**, and let the in-tree image be the reference build CI exercises. A
course that grades Verilog *and* JLS cannot use a JLS-only grader image; it
needs JLS inside *its* image, which the existing jlink layout makes a
three-line move:

```dockerfile
COPY --from=ghcr.io/anadon/jls@sha256:<digest> /opt/jls /opt/jls
```

That serves more courses than a monolithic image, and it keeps JLS out of the
base-image business — which is what §9's "no server, no install step ahead of
demand" spirit points at.

## Reframing B — put the score reduction in the CLI so parity is structural, not tested

CAP-21's headline claim is byte-identical score vectors across four platforms
(AC-1, #531), and KC-21-1 concedes the claim may simply die. But that risk is
manufactured by the decomposition. Four independently authored adapters each
read xUnit, each apply the lab's visible/hidden split and partial-credit
weights, and then #531 checks after the fact that four separate
implementations agree. That is testing your way to a property you could have
*built*.

The elegant seam is one layer down, in #524: have the frozen CLI emit a
canonical, normalized **score vector** artifact alongside xUnit — per test,
per point value, visible/hidden flag, already reduced. Then every adapter is a
pure format translation of one canonical document, containing no scoring logic
at all, and byte-identity is a theorem rather than an experiment. Note that
#708 AC-4 is already groping toward this ("partial credit and the visible/
hidden split are expressed by the lab, mapped by the kit, and not invented by
the adapter") — it is stating as a per-kit convention what should be a
first-class artifact produced once. Four repetitions of a convention is
exactly the shape that drifts.

Under B, #706 shrinks to roughly 60 lines of JSON reshaping and a path map,
and #531's fixture becomes a much smaller claim.

## Reframing C — the ordering is inside-out; there is a shippable slice today

#706 waits on #524, which waits on #369/#466, which are CAP-06's verdict
machinery. There is no xUnit anywhere in the tree today. So this issue is
gated behind freezing a contract for artifacts that do not exist, on behalf of
a user who has never used the thing.

But JLS *already has* a frozen contract: `batch-interface.md` §§1–4 pins exit
statuses, the stdout report, and the VCD profile, with §6's major-bump ratchet
already written. `examples/autograde/autograde.py` already grades both
surfaces. A PrairieLearn question kit over *that* is buildable this week, and
it would put a real instructor in contact with the delivery shape before
#524's freeze hardens. CAP-21's own §Cost names a demo slice of exactly this
kind — but assigns it to Gradescope. PrairieLearn is the better first target
precisely because it is the smallest surface: an image with an entrypoint and
a JSON file, no zip packaging, no Actions runner.

I would ship the PrairieLearn kit *first and early*, over the existing
contract, and let what it teaches inform #524 rather than the reverse.

## Two things the issue leaves unowned

1. **Who publishes the image, and where.** PrairieLearn resolves
   `externalGradingOptions.image` from a registry; "committed in tree" does
   not make a grading job able to pull it. Either JLS gains a second published,
   signed, per-release container artifact (`ghcr.io/anadon/jls-prairielearn`),
   or instructors build and push their own — and only in the second case does
   the reproducibility AC earn its keep. This is a real release-surface
   decision hiding under a filing-time word. Under Reframing A it collapses:
   one image, one digest, one signature, and the question config selects the
   adapter via PL's documented `entrypoint` option (verify that against the
   spec version AC-4 already requires be recorded).

2. **AC-2's instrumented no-interactive-session assertion buys little today.**
   #498 §7.2 says it plainly: *"Today 'don't interact' is enforced by there
   being no way to."* The image cannot open a session because JLS has no
   console; that only changes after M2 (`Console`/`HostBytePort`), a milestone
   nobody has started. Per-adapter instrumentation asserts, expensively and
   four times, a property that one ratchet test in JLS proper would hold for
   every consumer forever — and would keep holding after M2, which is the only
   moment it matters. Move it to #524 or #531; it does not belong in a
   Dockerfile task.

## What I would keep exactly as written

The refusal to scrape ("consumes only the frozen CLI contract… scrapes no
incidental output") is the right instinct and is the same instinct that
produced `batch-interface.md`. Targeting only PL's documented `externalGrader`
contract, and recording the pinned spec version alongside the entry point, is
correct and cheap. The static-kit boundary with generators deferred is right.
And the sibling-dedup note on #528 is right that this is a distinct platform
outcome — my objection is not that the outcome should be merged away, it is
that the *artifact* per outcome should be ~100 lines, not a Dockerfile
lineage.

## Suggested restatement of the outcome

> A PrairieLearn `externalGrader` adapter ships in tree as a single script plus
> a reference image derived by digest from `ghcr.io/anadon/jls`. The adapter
> contains no scoring logic — it translates the canonical score vector emitted
> by the frozen CLI into PL's results schema — and the docs show both paths:
> use the reference image, or `COPY --from` JLS into a course-owned grader
> image.

I am explicitly setting aside AC-1's "the image… builds reproducibly from the
committed definition" as a goal to be re-achieved: reproducibility should be
inherited from the one image the project already builds reproducibly, not
re-proved per platform. Everything else in the issue survives the reframing
intact and gets cheaper.
