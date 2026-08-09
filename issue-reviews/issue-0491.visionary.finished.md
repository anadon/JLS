# Issue #491: ElementId.parse never advances the creation counter, so the second run of one install saves a circuit JLS then refuses to open
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## The defect is real; the framing is one layer too low

Every citation checks out at the working tree: `NEXT_COUNTER` is a bare
`new AtomicLong()` (`src/jls/elem/ElementId.java:60`), `parse` returns without
touching it (`:245-269`), `Element` mints in a field initializer
(`src/jls/elem/Element.java:24`), the `sid` setter overwrites that field
(`:293-296`), and `finishLoad` refuses duplicates (`src/jls/Circuit.java:1310-1320`).
The three-line remedy works. Nothing below argues for leaving the bug in place.

What the issue does not ask is *why a value decoder has to mutate a global
counter at all*. That question has a better answer than the diff, and asking it
exposes two larger problems the fix leaves untouched.

## Reframing 1 — the seam is `finishLoad`, not `parse`

`parse` is a pure decoder of a persisted string. §7.4 asserts that
`Element`'s `sid` setter "is the only production caller." That is false at
HEAD: `src/jls/collab/op/CircuitOpReader.java:212` and
`src/jls/collab/op/NetBlocks.java:410` both call `ElementId.parse` on bytes
arriving off the wire. So the diff does not add a side effect to one loader — it
adds a hidden global mutation to *every present and future call site that decodes
an id*, including the untrusted network path. §7.11 worries about a hostile
`.jls` declaring `:9223372036854775807`; the same lever is now reachable by a
collaboration peer, with no file involved and nothing warning a future importer
author that decoding a string moves process state.

`Circuit.finishLoad` is the seam that already owns this invariant. It already
walks `loadedElements`, already builds `usedIds`, already fails the file when
uniqueness breaks, and is already the single funnel for *every* load path — file
open, `CircuitSnapshot` undo restore, checkpoint recovery, `CircuitRenderer`,
`HdlCircuitBuilder` (`grep -n finishLoad src/`). One saturating advance over the
own-replica maximum of `usedIds`, placed beside the check it protects, gives:

- `parse` stays a pure function; the collab reader gains no counter lever;
- `Long.MAX_VALUE` is handled once, by saturation, in the place that can also
  simply refuse the file — instead of being an open decision (Open Question 3)
  that has to be re-litigated at a validation site;
- the postcondition is stateable where a reader will find it: *after
  `finishLoad` succeeds, no subsequent mint collides with this circuit*;
- the cost is one small public method on `ElementId` (`Circuit` is in `jls`,
  `ElementId` in `jls.elem`) — which is a feature, not a tax: it forces the
  invariant to be **named** rather than smuggled in as an assignment inside a
  parser.

## Reframing 2 — the identity scheme is unsound for the feature this was just parented to

The 2026-08-08 comment re-parents this to **#356** (a merged `.jls` means what
both authors meant, or is refused). Under the fix as written, #356 still cannot
be built on `sid`:

Run 1: build circuit A from scratch, save. Ids are `feedface:0..9`.
Run 2 (same install): build circuit B from scratch, save. Ids are `feedface:0..9`
again. Nothing was loaded, so the fix never fires. Two files written by one
install now use the same permanent identities for different elements.
`docs/file-format.md:393` licenses this — "Stable ids MUST be unique **within
their `CIRCUIT` block**" — but #165's javadoc, #163's op addressing, #436's
`sref` references and #356's merge rules all read `sid` as globally permanent.
The format promises per-block uniqueness; the consumers assume install-wide
permanence; #491 closes only the third case, load-then-edit-then-save.

Worse for #163 specifically: two concurrent JVMs of one install are **the same
replica**. H2's necessity argument ("ids arriving from another replica share no
counter space with ours") assumes one actor per replica id, and #183 made that
untrue. Two windows of one install in a collaboration session mint colliding ids
in parallel, and no amount of parse-time advancing repairs ids already handed
out. That is a design hole in the pairing, not a bug in `parse`.

## Reframing 3 — #183 bought a property nobody needs, and this is the bill

The issue itself records that the defect could not exist while the replica was a
per-process draw. Read #183's own predictions: P2 (byte-identity **under an
explicit pin**) is the CI/reproducible-export value, and it is delivered entirely
by `jls.replicaId` / `JLS_REPLICA_ID`. P3 — from-scratch saves byte-identical
across runs of one install *without* any pin — is the only thing the persisted
config file adds, and it was already conditional, because the counter is
process-global: build any other circuit first, or open any file first, and the
bytes move. After #491's fix P3 gets strictly weaker (opening a file now shifts
the counter further), which quietly contradicts #334's Global Invariant 2
("byte-identical for identical circuit content, **independent of load/edit
history**").

So the trade is: an unpinned, already-conditional reproducibility nicety, paid
for with a data-loss defect, a hostile-input overflow hole, a side-effecting
parser, and a broken CRDT actor assumption.

The out-of-the-box route: **make the replica per-actor again by default.** Derive
it from the #168 install key that already exists
(`src/jls/collab/net/IdentityKey.java`, persisted in the same config directory,
and which `ElementId`'s own javadoc at `:23` says the replica "becomes") plus a
per-process nonce — or simply restore the pre-#183 random draw. Keep the two
override knobs verbatim for CI and reproducible export. Under that model this
issue's defect is *impossible*, `parse` stays pure, the `Long.MAX_VALUE`
residual vanishes, one actor has one replica id again, and #183's real value —
byte-pinnable CI — is untouched. What is lost is P3, which should be renegotiated
in `docs/file-format.md` rather than defended with a counter patch.

Two fallbacks if the maintainer will not give up P3, in order of preference:

1. **Persist the counter too**, hi/lo style: reserve a block of N, write the
   high-water mark next to `jls/replica-id`, allocate in memory. Ids from one
   install become unique across runs — which is what "permanent identity" was
   supposed to mean — and #356/#436 get a substrate that holds.
2. **Declare the id space in the file.** #334 Open Question 5 already proposes a
   per-file replica alias table; extend it with a per-replica high-water mark.
   Load-time seeding then *reads a declared number* instead of inferring one, the
   file becomes self-describing about its identity space, and the `'f' < 'l'`
   ordering trap that same open question names gets fixed in the same stroke.

## A smell worth recording either way

`private ElementId stableId = ElementId.mintFresh();` mints a *permanent
identity* from `new`. Every cancelled creation dialog burns one
(`SimpleEditor.setup(new Foo(circuit), …)` constructs before the dialog — and
ARCHITECTURE.md's note that byte-identical snapshots make cancelled gestures "drop
out for free" is only true of the circuit, not of the counter). Every loaded
element burns one before the file overwrites it. Every HDL scaffold and test
fixture burns one. The counter therefore measures Java object churn, not elements
that ever existed in a document — which is exactly why the determinism built on
it is so brittle. Identity belongs to *membership in a document*, assigned at
`Circuit.addElement` or by an explicit factory, from an allocator the document
owns. That is the same conclusion Reframing 1 reaches from the other end, and it
collapses this bug into "the allocator is seeded from the file it loaded."

## On the acceptance criteria — partly disregarded

§14 is disproportionate to a one-call change: a mutation run (P6), cross-platform
manual verification, and fourteen checkboxes. I would drop most of it and keep
two things:

- **Answer Open Question 2 with (b) and make it the primary test.** The
  end-to-end save-then-reopen form is the only assertion that states the property
  a user has, and it would have caught this without anyone reasoning about
  counters. The mint-level test is a localization aid, not the contract.
- **Add the test the issue is missing**, and expect it red: build a circuit from
  scratch under a pinned replica, save; restore the pin (fresh-JVM equivalent);
  build a *different* circuit from scratch; assert the two files' `sid` sets are
  disjoint. It will fail after the fix. That failure is the real issue, and it is
  the one that decides whether #356 is buildable.

## Verdict

**endorse-with-reframing.** Land a fix — this is a write-then-refuse data-loss
bug and it should not wait on architecture. But land it in `finishLoad`, not in
`parse`, so the decoder stays pure and the untrusted collab path gains no lever;
and file the successor that this issue's §13 does not contemplate: `sid` is not
unique across runs of one install, `#183`'s per-install replica is the wrong
granularity for a CRDT actor, and #356 and #163 both assume otherwise. The
strongest single move available is not in this issue at all — restore a per-actor
replica and keep the pins for CI, which makes this defect and its hostile-input
residual disappear rather than be patched.
