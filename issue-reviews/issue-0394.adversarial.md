# Issue #394: TASK-0086: a drawn circuit becomes a parts order — refdes keyed on stable id, a BOM, a point-to-point wiring list, and a diff that is additive when you add a gate
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is unusually well-instrumented (code anchors, falsifiable
hypotheses, an explicit acceptance test), but it rests its central
selling point — additive-only refdes diffs (H2/P5) — on a premise about
`ElementId` ordering that the actual comparator contradicts. It also
ships with three "concurrently filed" prerequisite tasks that in fact
already exist as filed, open issues and are not wired into
`blocked_by` in either direction, and it leaves four items in its own
Open-Questions list marked "Blocks execution" unresolved. Any of these
three alone would justify a rework pass before someone starts coding.

## Findings, most severe first

### 1. [Critical] H2's additivity proof is false whenever the inserted element's replica id differs from an existing element's — which is the normal collaborative-editing and legacy-file case

The issue's whole selling point is: *"inserting a new element $e^{*}$ whose
stable id is fresh appends it after every existing element in $\prec$
(stable ids are minted monotonically and never reassigned, O3)"* — and
P5 makes this the **load-bearing acceptance test**: *"A single changed
`U`-number anywhere fails it."*

That claim is only true within one replica. `src/jls/elem/ElementId.java:278-285`:

```java
public int compareTo(ElementId other) {
    int byReplica = replica.compareTo(other.replica);
    if (byReplica != 0) {
        return byReplica;
    }
    return Long.compare(counter, other.counter);
}
```

Ordering is **lexicographic on the replica string first**, counter
second. `mintFresh()` uses this install's `processReplica` (a random
32-hex-digit UUID unless pinned, `ElementId.java:42-51`) and a
process-wide `AtomicLong` counter that starts at 0 per process, not
seeded from the maximum counter already present in a loaded circuit.
Legacy (pre-#165) files mint all their elements under the fixed
replica string `"legacy"` (`ElementId.java:38,224-226`).

Concretely: `"legacy"` begins with `'l'` (ASCII 108); every hex digit
`[0-9a-f]` is ≤ `'f'` (102). So **any freshly minted element on any
install sorts before every element of a legacy-loaded file**, no
matter when it was inserted. Load a course-provided legacy fixture,
draw one new gate, save — and `getElementsInStableOrder()` now returns
the new gate *first*, not last. Under §7.10's refdes formula, that
element's package becomes `U1`, and **every pre-existing package's
refdes shifts down one** — the exact failure H2/P5 were written to
prevent ("Refuted if inserting one gate renumbers an existing
designator... This is the failure the whole task is shaped to
prevent").

The same failure mode recurs for any two non-legacy replicas whose
random UUID strings happen to compare the "wrong" way — i.e., for
ordinary collaborative editing (`jls.collab` already exists) between
two installs, not just legacy files. There is no scenario carve-out in
§7.10, H2, or the Open Questions for cross-replica insertion order;
the math silently assumes a single, globally monotonic counter that
`ElementId` does not provide.

**Recommendation:** Before P5 can be trusted as "the load-bearing
test," add a fixture that inserts a fresh-replica element into a file
containing elements from a different (or `legacy`) replica, and either
(a) prove the refdes/packing order must key off something other than
raw `ElementId` order (e.g., a secondary monotonic "insertion epoch"
recorded per file), or (b) explicitly scope this task to
single-replica files and say so in Threats to Validity — right now it
says neither, and ships a formal proof that is wrong as stated.

### 2. [High] The three "concurrently filed, numbers not yet known" prerequisites already exist as filed, open issues, and no link pass has connected them — in either direction

The issue states: *"Three task-to-task edges are in scope and cannot
yet be named, because those tasks are being filed concurrently; a link
pass adds their numbers"* — TASK-0007, TASK-0008, TASK-0085 — and
`blocked_by: []`.

All three already exist:
- TASK-0007 → **#468** (open, created 2026-08-03T17:42Z)
- TASK-0008 → **#373** (open, created 2026-08-03T14:04Z — **35 minutes
  before #394 itself, at 14:39Z**)
- TASK-0085 → **#400** (open, created 2026-08-03T14:47Z, 8 minutes
  after #394)

TASK-0008 (#373) was filed *before* #394 was opened, so the claim that
its number "does not exist yet" was already stale at filing time, not
merely overtaken since. As of today (2026-08-09), six days later,
`blocked_by` is still `[]` and none of #468/#373/#400 list #394 in
their own `related`/`blocks` fields either — the promised "link pass"
never ran on either side. A contributor or scheduler filtering on
`blocked_by` (which the issue's own machinery elsewhere treats as
authoritative — see #468's and #400's completion checklists, which
gate closure on `blocked_by` being accurate) would conclude #394 has
no blockers and is pickable now, when the issue body itself asserts it
hard-depends on three separate, still-open, not-yet-landed tasks (the
net-partition extraction, stable-id-keyed net naming, and the package
schema this task packs into — "There is nothing to pack into without
it").

**Recommendation:** Update `blocked_by` to `[468, 373, 400]` and add
the mirror edges on those three issues before this is treated as
scheduleable. This is a mechanical fix but its absence is exactly the
kind of drift that lets someone start TASK-0086 without TASK-0085's
schema existing.

### 3. [High] The issue leaves four "Blocks execution" open questions unresolved while presenting a fully fleshed-out Method/checklist as if ready to start

Open Questions & Decisions Needed lists four items, three explicitly
marked **"Blocks execution"**: (1) does `SubCircuit` flatten before
packing, (2) what are the refdes prefixes, (3) where is the assignment
rule documented. Recommended defaults are given for all three, but
none is marked resolved/ratified — contrast with sibling issue #400
(TASK-0085), whose completion checklist opens with *"#349 Open
Questions 1 and 5 ratified and recorded here before implementation
starts."* #394 has no equivalent gate in its own Completion Criteria;
its checklist item is only *"Every decision in Open Questions &
Decisions Needed is resolved (or explicitly deferred), none left
blocking"* — placed at the very end, after the implementation steps in
§8 Method, rather than before them. An implementer following §8's
ordered checklist top-to-bottom would build the packer, the fixture,
and the emitters before ever confirming whether `SubCircuit` flattens
— exactly the scope-defining decision H3 says must not be "discovered
by a student ordering parts."

**Recommendation:** Move the three "Blocks execution" resolutions to
the front of §8 Method (as #400 does), or explicitly state they are
pre-resolved by the "recommended default" language and drop the
"Blocks execution" tag if that's the actual intent — right now the two
signals (tag says blocking, checklist ordering says proceed) conflict.

### 4. [Medium] P3's "every wiring.net line must include power/ground pins" conflicts with TASK-0085's own escape hatch for parts with no declared power pins

P3 states unconditionally: *"VCC and GND of every placed package appear
in the supply nets in `wiring.net`. A wiring list a person builds from
must include the pins that make the chip work."* But the upstream
schema (TASK-0085/#400, §7.4) defines `Π_supply` as part of
`PartPackage` with no totality guarantee — it's just "the power pins
(VCC, GND by pin number)" on the data class, and #400 never asserts
every shipped part declares them (P10/P11 in #400 check electrical
inertness and ordering, not power-pin presence). If a `.parts` entry
under `-parts` (hostile, user-authored input per #400 §7.3) omits
power pins, P3 as written has no defined behavior: is it a `pack.log`
diagnostic, an aggregated error, or a silent gap in `wiring.net`? §7.11
of #394 enumerates failure modes for unrealizable elements,
double-booked sections, unbound pins, and unwritable directories — but
not "package with no declared supply pins," despite P3 treating supply
completeness as load-bearing.

**Recommendation:** Add a §7.11 row for a part library entry lacking
declared VCC/GND, and cross-reference it against #400's schema so the
two issues don't diverge on whether that's even representable.

### 5. [Medium] The refdes-prefix decision embeds a forward compatibility promise ("R/C/J reserved... a later part class does not renumber existing designators") that is not tested by anything in this task's own Method or Predictions

Open Question 2's recommended default: "define all four [prefixes] now,
use only `U`, and record the mapping in the assignment rule so a later
part class does not renumber existing designators." That's a real
design constraint (per-prefix counters, per §7.10's formula, are
already scoped per-prefix so this mostly falls out for free) but
nothing in §8 Method or the Predictions actually exercises "define R/C/J
now" — there's no fixture with a non-`U` part, no test that reserving
an unused prefix doesn't perturb `U`'s numbering. It's asserted as a
decision but never falsified per the issue's own H1-H4 discipline.

**Recommendation:** Either add a one-line prediction (e.g., "declaring
R/C/J with zero members does not change any U-prefixed refdes") or
drop the "define all four now" clause as decorative, since nothing
checks it.

## What's solid

- The code anchors (O1-O6) are accurate and reproduce verbatim on the
  current tree modulo the line-number drift already flagged and
  corrected in the issue's own pinned comment (HdlExporter's UnionFind
  moved from `:1160-1167` to `:1102-1109`) — good self-correction
  discipline.
- The nine-type non-decomposable enumeration (O7) is independently
  verified against `ElementRegistry.java` (35 registered types,
  matching exactly) and is consistent with sibling issue #365's own
  count.
- The determinism concerns (map iteration order, §7.9/§11) are
  well-founded given the codebase's existing `WireNet` and `Board`
  precedents, and the "first-fit, not an optimizer" framing correctly
  avoids overpromising.
- Scope boundaries (no GUI, no PCB netlist, no geometry, no `.jls`
  format change) are clearly and consistently stated across this issue
  and its siblings (#365, #400), reducing the chance of scope creep
  once work starts.

## Bottom line

The mechanical parts of this task (packing, BOM/wiring emission,
aggregated diagnostics) are well specified. But its one truly novel,
load-bearing claim — that stable-id order makes refdes assignment
additive under insertion — is backed by a proof that the codebase's
actual `ElementId.compareTo` contradicts in the collaborative and
legacy-file cases the project already supports. Combined with the
stale/missing prerequisite links and the unresolved "blocks execution"
open questions, this needs a rework pass (fix the H2 premise or narrow
its scope, wire up `blocked_by`, front-load the open-question
ratification) before it's safe to hand to an implementer.
