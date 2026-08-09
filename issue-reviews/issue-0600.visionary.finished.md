# Issue #600: TASK-C332-1: a design is expressible as N part files plus one boundary description that names the cut nets the author declared
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Strip the vocabulary and #600 asks for one thing: **a design must stop being one
file.** Everything else in #332 — streaming elaboration, net identity across a
cut, refusal, the equivalence harness — is downstream of that. #600 is the
artifact-form task, and #332/§6 says correctly that nothing else can start until
it lands.

The claim I want to test is not "should a design be a file set" (yes) but
"**should JLS grow a second, orthogonal decomposition axis to get there?**"
#600 invents a *partition* axis — `PartitionSet`, `BoundaryDescription`, declared
cuts, a derived per-part net set — parallel to a decomposition axis JLS has
shipped since Poplawski: the subcircuit.

## JLS already has an author-declared cut with named boundary nets

`jls.elem.SubCircuit` is a cut. The author declares it by drawing a box. The
boundary nets are already named, by the author, as `InputPin`/`OutputPin`
elements inside the definition, and the correspondence across the cut is already
a materialized map:

```java
private Map<Input,InputPin> inmap = new HashMap<Input,InputPin>();
private Map<OutputPin,Output> outmap = new HashMap<OutputPin,Output>();
```
(`/home/user/JLS/src/jls/elem/SubCircuit.java:32-34`)

The one reason this does not already relieve the single-file ceiling is that a
subcircuit is stored **by value, once per instance**:

```java
public void save(PrintWriter output) {
        output.println("ELEMENT SubCircuit");
        ...
        getSubCircuit().save(output);
```
(`/home/user/JLS/src/jls/elem/SubCircuit.java:282-289`), matched on load by the
nested-`CIRCUIT` branch at `/home/user/JLS/src/jls/Circuit.java:1013-1024`, and
by `SubCircuit.copy` deep-copying the whole body (`:328-352`).

#447 (TASK-0041) has already measured what that costs: ten instances of an
eight-gate definition grow the saved text from 1,547 to 15,209 characters
(**9.83x**), and the file carries 80 `ELEMENT AndGate` blocks for an eight-gate
drawing. So the "single-file ceiling" #600 exists to remove is measured against a
representation that spends an order of magnitude of it on duplication. #312's own
headline number — ~694,709 elements at the cap after #353 — is a count of *saved
text* elements, i.e. of copies, not of distinct design content.

## The reframing: the part **is** a definition; the boundary **is** the port list

Do not build a partition artifact. Build **external definition references**, on
top of work that is already filed and already sequenced:

- **#417** (TASK-0039) definition digest + version identity — a definition gets an
  identity worth referencing.
- **#447** (TASK-0041) one definition stored once, N instances reference it —
  already blocked only by #417, and already scoped to "bump `FORMAT` only for
  files that actually use a shared definition".
- **#340 / #446** a set of circuits as a distributable, digest-checked artifact
  **a circuit can reference by name** — i.e. definitions resolved out of file.

Add one step those three do not yet cover: let a definition reference resolve to
another `.jls` file (or a #446 library member) by name + digest instead of to an
in-file table entry. Then #600's outcome falls out rather than being built:

- **N part files** = the root file plus each externally referenced definition
  file. No new container, no `PartitionSet` type, no second save path.
- **The boundary description** = the referenced definition's `InputPin`/
  `OutputPin` names, which are authored, stable, and already the thing both sides
  agree on. AC-2's "names exactly the cut nets, as an equality in both
  directions" becomes a statement about a port list rather than a new equality to
  maintain between two artifacts.
- **AC-3 for free.** A design with no external references saves exactly as today,
  because nothing on the save path changed for it. #600's AC-3 needs a test;
  here it needs no code.
- **AC-5 for free.** Each part *is* a `.jls` file opened through
  `FileAbstractor.openCircuit`, so `MAX_CIRCUIT_TEXT_BYTES`
  (`/home/user/JLS/src/jls/FileAbstractor.java:65`) applies per part by
  construction, with zero new enforcement code. The aggregate question (#332 OQ2)
  degenerates from a format question to a policy knob.
- **A strictly better streaming bound.** #332's criterion 3 is
  `max_i M(D_i) + M(B) + c`. With shared definitions, an instantiated definition
  is resident *once* however many parts instantiate it, so the bound is over
  distinct definitions, not over parts. The partition axis cannot say that; the
  hierarchy axis gets it as a side effect.

## The dependency collapse this buys, and why it matters most

AC-4 is the expensive criterion: it drags in #319's section frame and
must-understand semantics, and #319 is itself `blocked_by: [334]`, which is
gated by #315. With `ordering_after: [319, 336]`, #600 cannot start until a
four-feature chain lands.

But the property AC-4 actually wants — *a reader that does not understand the
part form must refuse rather than open one part and believe it has the design* —
is already deliverable by the mechanism #79 shipped. A file that references an
external definition declares a higher `FORMAT` version, and every existing reader
refuses it by name:

```java
if (version > FORMAT_VERSION) {
        return failLoad(LoadError.Category.NEWER_FORMAT, ...
```
(`/home/user/JLS/src/jls/Circuit.java:765-771`, `FORMAT_VERSION = 2` at `:102`).
That is whole-file must-understand, which is exactly the granularity AC-4 needs;
per-*section* granularity is what #319 exists for, and this criterion does not
need it. #447 already commits to the conditional-bump discipline. **Under the
reframing, #600's work no longer depends on #319 or #334 or #315 at all**, and
its dependence on #336 softens to "cut nets are named by the author's pins", a
property that holds today.

That is the whole argument in one line: the reframing takes the first task on
#312's critical path from *four features deep* to *one filed task (#417) deep*.

## Does #600 strengthen the arc, duplicate it, or pull against it?

**Duplicates.** #340, #357, #446, #447, #472 are a filed, measured, five-issue
program whose end state is "a design is a set of files referenced by identity
with a stable addressing key". #600 is a second program with the same end state
and a different noun. Two decomposition axes means two save forms, two
boundary-naming schemes, two streaming stories, and the perpetual question of
what a subcircuit that straddles a partition means. #332/§7 already names "two
representations of one design" as a `REPLAN:` hazard — it does not notice it is
proposing a *third*, since hierarchy is already a second.

**Pulls against, mildly.** ARCHITECTURE.md's recorded decisions consistently
decline speculative scale work (interpreter-only simulation, §"Simulation
execution strategy", whose revisit trigger is a *concrete* `riscv/` design that is
unusably slow). #312's 10^10-gate commercial-bridge target is outside that
posture. The reframing is the version of this work that survives the posture,
because every step of it is independently useful to the users JLS actually has:
`riscv/`'s CPU stops paying per-instance for its ALU slices, an instructor can
ship a course parts library (#340/#446), and editing a definition finally changes
every instance (#447's student-visible bug).

**Strengthens, genuinely.** One piece of #600's sibling set is better than its
framing suggests: the uncuttable-construct refusal. A combinational cycle
crossing a boundary is a real defect *today*, in single-file designs, and JLS's
event-queue simulator will happily oscillate on it. Filed as "combinational loop
detection, named by element", it is a pedagogy feature a first-year student
benefits from, not a distributed-simulation prerequisite. It should not wait on
#600 in either framing.

## Two smaller alternatives worth naming

1. **The boundary description is derived data; do not treat it as an artifact.**
   Given the parts, the cut set is computable — the only reason to persist it is
   that computing it requires all parts resident, which streaming forbids. So it
   is an *index*, and should be modeled as one: written on save, digest-checked
   on load, rebuildable. AC-2 as written ("equality in both directions") is a
   cache-coherence test wearing a correctness test's clothes, and persisting an
   index as a co-equal artifact is how the two silently disagree later.
2. **The 64 MiB cap is a hostile-input policy, not a capacity decision.** Its own
   comment says so (`FileAbstractor.java:60-65`, issue #38): "a tiny archive
   inflating to gigabytes is treated as hostile, not as a big circuit". A
   user-opened, user-authored file is not the threat that constant defends
   against. Making the cap scale with available heap for the interactive
   File > Open path — while keeping the constant for anything fetched or
   embedded — removes a chunk of what #600 calls "the single-file ceiling" for
   roughly the cost of a config knob. It does not deliver #333's distributed
   run, and I am not proposing it as a substitute; I am proposing it because
   three of #600's five acceptance criteria are about that cap and that form,
   and one of them may not need a 10-16 maintainer-week feature to satisfy.

## Where the reframing is weaker, stated honestly

Hierarchy cuts are **tree** cuts. A partition chosen for load balance may want to
split a 32-bit carry chain in half, which no subcircuit boundary expresses. This
is a real loss — but it is a loss of *cut quality*, and #600 explicitly defers cut
quality (#312 open decision 3) and writes for the author-declared case precisely
to avoid solving it. An author declaring cuts by hand will declare them at module
boundaries anyway, because that is where they can name them. If a measurement
later shows hierarchical cuts are unusable for load balance, *that* is the moment
to introduce a partition axis — with a number in hand, and on top of a
definition/instance split that will have to exist regardless.

## Disregarding the stated acceptance criteria

I am explicitly setting aside AC-1 through AC-5 as the shape of the work. AC-3
and AC-5 become vacuous under the reframing rather than satisfied; AC-4's stated
dependency on #319 is, I claim, unnecessary and is the single most expensive
sentence in the issue; AC-1 and AC-2 survive in substance but are restated over
definitions and port lists rather than over parts and a boundary artifact.

**Recommended disposition.** Close #600 with a `REPLAN:` on #332 re-homing the
"part-file set and boundary description" scope onto the definition/instance line
(#417 → #447 → external definition references → #340/#446), and file one new task
for the missing step: *a definition reference may resolve to another file by name
and digest, with the `FORMAT` bump reaching only files that use one.* Keep the
uncuttable-construct scope, refile it as combinational-loop detection independent
of partitioning, and let #332's streaming-elaboration and equivalence-harness
scopes re-derive their contracts against the definition seam.
