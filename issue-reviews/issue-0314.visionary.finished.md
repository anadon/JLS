# Issue #314: FEAT-002: a saved attribute no element declares becomes a named diagnostic instead of vanishing at load
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The issue says it plainly: *"Every later format change, importer and collaborative merge relies on
'if it loaded, it is all there' being true."* That sentence — not the `boolean` return type, not the
five loader arms — is the deliverable. Four capstones and four downstream features (#323 migration
report, FEAT-003 canonical save, FEAT-012 merge safety, #319 per-section versioning) are all leaning
on round-trip fidelity.

The issue then proposes to make that sentence **checkable** rather than **true**. A diagnostic tells
the user their data was dropped; it does not stop the drop. That gap is the whole of my review.

The README already tells the motivating story in the project's own voice (`README.md:315-320`):
upstream JLS 4.1 opens a current file, *"silently drops initial memory contents"*, and the user's
memories come back empty. We cannot ship a diagnostic into JLS 4.1. What we can do is guarantee that
*our* loader never does that to the next fork's files. The issue's framing cannot reach the case that
motivates it.

## Where the issue pulls against the project's arc

**1. It contradicts a normative spec section without saying so.** `docs/file-format.md` §5 does not
say unknown attribute names are "handled" (as this issue's Related-issues section paraphrases). It
says:

> **Unknown attribute names are silently ignored.** … This is the format's main forward-compatibility
> valve (§9): a newer JLS may add attributes to an element without breaking older readers — at the
> cost that the older reader silently drops that data.

That is #79's designed behaviour, deliberately contrasted two paragraphs earlier with unknown *item
kinds* and *tags*, which are hard errors. #314 proposes to reclassify the valve as a defect while
declaring in §4 that nothing on disk changes and no version bump occurs. A normative spec edit is a
required, unlisted deliverable — and "the valve is now noisy" is a strictly worse valve unless
something replaces what the valve was buying.

**2. The mechanism cements the duplication #23 set out to remove.** `Attribute` already carries the
attribute name as a field (`src/jls/elem/Attribute.java:26`), and `savedAttributes()` already returns
the element's full declaration list (`Element.java:316`). The declared name set `A(e)` is *already
computable in one place*. The issue instead threads a `boolean` up through every hand-written
override — and those overrides are precisely the classes #23 has not yet converted:
`LogicElement`, `WireEnd`, `Group`, `StateMachine`, `Memory`, `Extend`, `State`, `JumpEnd`,
`TruthTable`, `SubCircuit`, `Pin`. Making all of them return `boolean` is an investment in keeping
them. ARCHITECTURE.md's honest list still reads *"`setValue`/`Attribute` entries"* at step 6 —
the dual path is a known debt, and this feature would harden it.

(For scale: the cited "26 declarations" is 20. The other six are `setValues(int[])`,
`setValues(int,int,boolean)` and three `setValue(BitSet)` runtime setters that have nothing to do with
load dispatch. The final DoD grep — `git grep "public void setValue" -- src/jls/` returns zero — is
therefore aimed at a set that includes unrelated methods, and would be satisfied by renaming them.)

**3. The `boolean` signal cannot distinguish the two misses that matter.** `IntAttribute` implements
`setInt` and inherits `setLong`/`setBigInt`/`setString` returning `false`. So a file carrying
`String bits "8"` for an int-declared `bits` produces exactly the same signal as a file carrying an
attribute no element has ever heard of. The first is a type-tag error in a name JLS *does* know; the
second is forward-compat data. Reported identically, both become noise, and #323's migration report —
the named first consumer — inherits the conflation. A name-set membership test separates them for
free; a `boolean` from the dispatch loop structurally cannot.

**4. It stops one seam short of the sinks it doesn't name.** `Element.setPair(int,int)`
(`Element.java:404`) has an **empty body** and exactly three overrides (`Group`, `StateMachine`,
`TruthTable`). Every other element type silently swallows a `pair` item whole. This is a worse silent
sink than the attribute case — it is not even name-keyed, so there is nothing for a `boolean` to
report. §1's "four methods and five call sites" is presented as a closed bound on the mechanism; it
isn't. The item-level seam covers `pair` uniformly; the dispatch-level seam cannot.

**5. It works at the opposite granularity from #319 while claiming distinctness.** The dedup comment
correctly says #319 owns sections and #314 owns attributes. But #319's *policy* is "unknown optional →
skip, **preserve verbatim**, informational diagnostic." #314's policy for the same class of unknown,
one level down, is "drop it, but say so." Two granularities of the same forward-compat problem
answered with two different philosophies is the kind of split that a maintainer three years out has to
reconcile anyway. Harmonising them now costs nothing and buys architectural coherence.

## The reframing: preserve, don't merely report

**Set aside the stated acceptance criteria's mechanism** — the `void`→`boolean` change, the
20-override ripple, and the DoD line demanding compiler-enforced totality. Keep the capability
sentence, the aggregation requirement, the fixture corpus, and integration criteria 1-4 verbatim.

Concretely:

1. **Ask, don't signal.** Expose the declared attribute name set: `Attribute` gains a public `name()`;
   `Element` gains `declaredAttributeNames()` (union of `savedAttributes()` names plus, transitionally,
   an explicitly declared name list for the not-yet-converted classes). The loader tests membership
   *before* dispatching. One new method on `Element`, one on `Attribute`, five call sites in
   `Circuit.loadElementItems`. Zero signature changes, zero ripple, no `SpotBugs`/`sealed` churn.
   The change is small enough to land in a day, which matters because #404 and #408 are currently
   scheduled behind a 2.1-week prerequisite.

2. **Stash the miss instead of dropping it.** On a miss the loader appends
   `(kind, name, rawValue, line)` to a per-element `unknownItems` list; `Circuit.save` re-emits those
   lines in canonical position after the element's declared attributes. That single change makes
   "if it loaded, it is all there" **true**, not merely observable:
   - FEAT-003, FEAT-012, #319 and #323 get a premise that holds rather than one that is checkable;
   - the file-format.md §5 valve is *upgraded* rather than contradicted — a newer JLS's attributes now
     survive a trip through an older reader, which is what a forward-compatibility valve was always
     supposed to mean;
   - `#166`'s byte-identity oracle now proves something about foreign files, not just our own;
   - the diagnostic is a free by-product: the same `unknownItems` list is the miss list, so §5
     criteria 1, 3 and 4 land unchanged.

3. **Reclassify the diagnostic honestly.** `LoadError` is a *failure* record — `failLoad` sets
   `JLSInfo.setLoadError` and returns `false`. There is no non-fatal severity in that taxonomy, so
   "load and report" has no home there today. `TellUser.warn` is the right channel and is already
   headless-aware (stderr, stdout stays pure — `docs/batch-interface.md` is safe). With preservation
   in place, Open Question 1 ("reject or load-and-report?") **dissolves**: nothing is lost, so nothing
   needs rejecting, and the FEAT-013 epoch coordination in §7 stops being a blocking dependency. That
   is the reframing's clearest tell that it cuts along a better seam.

4. **Route the real bug where it belongs.** With preservation, a miss on a *tracked* fixture is
   unambiguous evidence that some element's `savedAttributes()` is incomplete — a #315/#372 finding —
   because our own writer produced the name. #404's H3 already anticipates exactly this; preservation
   turns it from a caveat into a clean classifier.

## Alternative framing considered and rejected

**Reject the load on any miss, full stop.** Simplest to specify, and it makes the invariant airtight.
Rejected for the reason the issue itself gives: it turns a user's working file into a file that will
not open, and JLS's audience is instructors and students mid-lab. Preservation gets the same invariant
without ever refusing a file.

## What I would keep exactly as filed

The audience analysis, the aggregation requirement (§5 criterion 3, which is also the #38 DoS
answer), the insistence that the corpus assert *diagnostic text* rather than a flag, and §5 criterion
2 — "add an element type that under-declares, observe red" — which is genuinely the criterion neither
child asserts alone and is the single best idea in the issue. All four survive the reframing intact.

## Bottom line

The goal is right and the project needs it. The chosen mechanism buys the weaker half of the outcome
at the higher price, entrenches a duplication the project has already decided to retire, and answers
the same question differently from its sibling one level up. Reframe from *"report the loss"* to
*"don't lose it"*, cut at the loader's item seam rather than the dispatch's return type, and the
feature gets smaller, unblocks itself, and delivers more.
