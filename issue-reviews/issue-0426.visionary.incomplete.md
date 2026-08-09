# Issue #426: TASK-0074: the queue, the clock, the dedup set and every element's running contents are written back rather than rebuilt from init text
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Underneath the eleven predictions there is one capability: **a running simulation
stops being a process and becomes an object** — small enough to copy, cheap enough
to take often, re-enterable many times. Everything else in the issue is packaging.
That capability is genuinely missing, genuinely wanted (#301, #312, #333, #350 all
consume it), and the two hardest technical claims — that `SimEvent.sequence` must
stop being `static`, and that a *nearly* identical resume is worse than none — are
correct and well argued. Endorse the goal.

The reframing is about the packaging, and it is not cosmetic: as written, the issue
picks a container that fights the capability, and it re-derives a state census that
the repository already contains and already got right.

## The project already designed this, and #426 does not cite it

`docs/capability-roadmap/lf-03-causal-debug.md` §(b) "Time travel — checkpoint and
replay" is a worked design for exactly this task, with measured numbers, and #426
references neither it nor `AMENDMENT.md`. What it records:

- **A recorded format decision, opposite to this issue's.** lf-03:281 — *"Format.
  Decision: a new `jls.sim.SimCheckpoint`, not the `.jls` save format."* Names
  `CircuitSnapshot` as the right precedent for the *technique* (deflate at
  `BEST_SPEED`, byte-equality `sameAs` as the oracle) and the wrong one for the
  *content*.
- **A measured size, three orders of magnitude below this issue's arithmetic.**
  The complete simulation state of the 1,551-element RV32I CPU is **≈9 KB raw,
  2–3 KB deflated** — "it fits in a network packet" — because memory rides
  `WordStore.present` deltas (the `present` BitSet and `getPresentAddresses` already
  exist, `src/jls/elem/Memory.java:1077,1129`) and because most of the queue is
  regenerable.
- **A retention policy**: adaptive cadence by fired-event count, plus a logarithmic
  retention ladder — i.e. *many* checkpoints per run.
- **A scheduling constraint**: put the checkpoint element pass *inside* P1's element
  pass, since both walk the same ~28 `initSim` / 25 `react` implementations, and
  "doing them six months apart is precisely the mistake keystone C warns about."

The parent feature agrees with lf-03 and not with this issue. #363 Open Question 1
recommends **(b) sidecar or (c) directory** and states that the answer *"Blocks
filing TASK-0074, because it determines what the loader reads."* #426 was filed
anyway, choosing (a) — the in-file section — and hardens it into an imperative in
§7.1: *"never a new file kind and never a second grammar."* That is a fork from the
parent's recorded default, taken without a `REPLAN:` and without argument.

## Reframing 1 — the checkpoint should be its own artifact, not a `.jls` section

**I am explicitly disregarding P9 and P10 and the §7.1 "never a new file kind"
imperative.** They are downstream of the container choice, and the container choice
is wrong for four reasons the issue never weighs:

1. **A section admits exactly one checkpoint; the capability needs many.** §7.4 asks
   for a `pinned` flag "exempt from any retention policy" — retention policy implies
   a ladder of snapshots, which lf-03 sizes at 16 per long run. A single optional
   section inside the circuit file cannot hold sixteen, and taking one means
   rewriting the whole circuit file. The issue has imported lf-03's vocabulary
   without lf-03's container, and the two do not fit.
2. **It buys two unfiled blockers for nothing.** TASK-0033 (section framing,
   not filed) and TASK-0034 (raw binary section, not filed) exist in this task's
   critical path *only* because the payload was put inside `.jls`. A standalone
   `JLSCKPT` artifact needs neither: its version is its own header, and an old
   reader "skips it" by never seeing it. P9 becomes vacuous rather than tested.
3. **It couples a deliberately non-portable artifact to a deliberately portable
   one.** §7.7 says checkpoints are never portable across JLS versions. `.jls` is
   the format whose whole discipline (`docs/file-format.md`, `FORMAT` header, #79,
   frozen `SaveTags`) is about opening files from 2014. Putting a never-portable
   payload inside the always-portable file makes one artifact with two
   compatibility promises, which is how a format acquires a permanent asterisk.
4. **The 64 MiB cap argument evaporates without it.** O9's ~66.6 MB figure is
   computed against `MAX_CIRCUIT_TEXT_BYTES`, a cap that only applies because the
   payload was put in circuit text.

Concretely: `jls.sim.SimCheckpoint` (lf-03's name — adopt it and Open Question 2
answers itself, with `.jls~` keeping "checkpoint" and this being a *snapshot* in
prose), magic + version header, sorted tables, deflated, byte-identical for
identical state. That last property is the correctness oracle FEAT-035 §5 criterion 1
demands, and a sorted canonical encoding is the only way `replay(ckpt[i]) ==
ckpt[i+1]` *byte for byte* is even meaningful.

## Reframing 2 — write the delta, not the image

§7.6 captures `mem` **and** `initMem` for every `Memory`. `initMem` is derivable
from the authored init text that is already in the circuit file — except in the one
case §7.11 correctly flags, where `initSim` read it from a host file. So:

- capture `initMem` **only** when it came from a host path (that case alone justifies
  a bulk payload, and it is rare);
- capture `mem` as a **delta over `initMem` restricted to `present` addresses**, which
  `WordStore` already exposes;
- bound it, and refuse by name past the bound rather than growing a format.

A 16 MiB memory in which a run has touched 4,000 words is 16 KB of delta, not 66 MB
of hex. The pathological fully-written case still exists and still deserves a raw
payload — but as a property of the snapshot artifact, not as a dependency on an
unfiled `.jls` binary-section task.

## Reframing 3 — two pieces of "state" in the title are not state

- **`dupCheck` is a derived index, not state.** In the whole tree, `eventQueue` and
  `dupCheck` are mutated in exactly three places (`Simulator.java:167-168` post,
  `:224-225` poll, `:181-182` clear) and `post` adds to the set only when absent,
  so the set is *by construction* the queue's element set. Serializing it is
  redundant; **rebuild it from the restored queue** and P3 stops being a test and
  becomes an invariant. This also dodges an unpleasant corner the issue does not
  mention: `SimEvent.hashCode` mixes `System.identityHashCode(callBack)`, so a
  `HashSet` of events has no canonical iteration order to write down — serializing
  it at all would need a sort key the type does not currently have.
- **The `seq` *base* is not state either; the *relative* order is.** Renormalize the
  surviving events' sequence numbers at write time (subtract the minimum). Two
  snapshots of the same state then encode identically regardless of what the run did
  to get there, which is exactly what FEAT-035's byte-identity oracle requires and
  what an absolute base would break for no gain.

## Reframing 4 — make the dedup key structural and H4 disappears

§7.10 elevates `callback(dec(enc(e))) ≡_ref callback(e)` into a formal obligation,
and §7.11 adds a refusal for ids that resolve to nothing, all because
`SimEvent.equals` compares the callback by reference. But `ElementId` (#165,
`Element.java:24`) is unique within a circuit — `StableElementIdTest#idsAreUnique-
WithinACircuit` pins it — and a simulation holds one instance per id. Therefore
keying `equals`/`hashCode` on `(time, stableId, payload)` is **observationally
identical** to today's behaviour for every reachable state, while making the dedup
relation a function of the modelled state rather than of JVM addresses. Consequences:
H4 becomes trivially true, the identity obligation leaves §7.10, the events become
sortable and canonically encodable, and `System.identityHashCode` leaves the
serialized path — which FEAT-035 invariant 4 ("nothing added here may depend on hash
order... or replica identity") arguably already requires. This is the reframing that
makes the hardest-looking part of the issue stop being hard.

## The state census is materially wrong, and the right one is already in-tree

**H3 is false as stated.** O5 claims *"`Input.getValue`/`Output.getValue` delegate to
the net, so puts need no separate capture."* They do not delegate. `Put.currentValue`
is an independent field (`src/jls/elem/Put.java:385`); `Input.getValue` returns it
directly (`Input.java:74`); `Output.getValue` returns it directly (`Output.java:123`);
and `Output.propagate` uses it as the **change-suppression latch** — `if
(currentValue.equals(value)) return;` (`Output.java:143`). A resume that rebuilds puts
empty will emit a propagation the uninterrupted run suppressed, at the first
re-drive of an unchanged value. That is a silent, content-dependent divergence in
precisely the class the issue says is worse than no checkpoint at all.

lf-03's census lists `Put.currentValue` **first**, and also lists the `toBeValue`
shadow across seven classes (`Gate`, `Mux`, `Adder`, `Register`, `TriState`,
`Decoder`, `ShiftRegister`) where §7.6's table names only `Register`. The issue
re-derived a smaller and wrong census at `2d0ca9d` rather than starting from the one
the repository already holds. H3 should be marked refuted before work starts, and
the field map (P5) should be seeded from lf-03's table.

## Alignment: one duplication worth avoiding

FEAT-035 §5 criterion 4 wants element-kind totality "swept over `ElementRegistry`".
There is no `ElementRegistry` — `ARCHITECTURE.md` records that #78 will introduce one
and collapse today's sixteen-place element checklist. #426 responds by inventing a
*second* per-element metadata mechanism (a "declared state mapping" plus a committed
field-map document) that would become checklist item seventeen and would have to be
migrated into #78 later. The state mapping is a natural **facet of the registry**:
land it there and the totality sweep is free, the refusal list is generated rather
than hand-maintained, and the field-map document — which H5 correctly admits "decays
every time an element ships" — never needs to exist.

## What I would keep unchanged

- **H1 and the per-`Simulator` sequence counter.** Correct, agreed by lf-03 and by
  `AMENDMENT.md` (which makes it P9's single hard prerequisite), and the compile
  break at every `new SimEvent(...)` site is a feature.
- **H2 and the no-`default` payload switch.** Right, and consistent with
  `CONTRIBUTING.md`'s sealed-dispatch rule and `SealedHierarchyTest`.
- **The refusal discipline.** Naming what cannot be captured is the single most
  valuable inheritance from gem5 and should survive every reframing above.
- **TASK-0075 written first.** The strongest structural claim in the issue.
- **`RegisterFile`'s golden before its checkpoint path** (O7/O8) — and note that its
  content initialization and write-back is a separable, independently valuable change
  that does not need any of the checkpoint machinery to land.

## Grounding note

`docs/parity-contract.md` (cited in §6 and §8) and `docs/plan/evidence/BRIEF.md`
(cited in §7.7) are not present in this checkout, so those two claims could not be
checked against the tree; `docs/capability-roadmap/lf-03-causal-debug.md` and
`AMENDMENT.md`, which cover the same ground and disagree with the issue, are.
