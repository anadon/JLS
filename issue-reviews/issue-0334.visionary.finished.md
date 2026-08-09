# Issue #334: FEAT-003: a saved circuit is a reviewable text artifact whose diff is proportional to the edit, not to the file
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not "readable `git diff`". The real defect is that **JLS emits positional
identity into artifacts that outlive the process that wrote them.** Every
element block carries both a permanent id (`String sid`, already shipped under
#165 — see the worked example at `docs/file-format.md:500-548`) and a
save-time dense index (`int id`, `src/jls/elem/Element.java:202-215`), and
every reference resolves through the *transient* one
(`src/jls/elem/WireEnd.java:606,611,613`). The file therefore states each
element's identity twice, in two incompatible id spaces, and wires itself up
using the space that changes when anything else changes. Diff amplification,
the `NetBlocks` renumbering dance (`src/jls/collab/op/NetBlocks.java:99-125,
204-250`), #167's recorded workaround, #356's clean-but-wrong merges, and
#171's convergence oracle are all one consequence of that one fact.

Stated that way the goal is right, load-bearing, and squarely on the project's
arc: `docs/grand-architecture.md` §2 names the collaborative editor as a funded
trajectory, and `docs/capability-roadmap/lf-06-diff-merge-vcs.md` §0 says JLS
already has four of the five prerequisites for semantic merge. I endorse the
end. I do not endorse the mechanism, the cut, or two of the acceptance
criteria, and the reason is that **the repository already contains a better
design for exactly this work and the issue does not cite it.**

## The design already in the tree, versus the one filed

`docs/capability-roadmap/lf-06-diff-merge-vcs.md` §C1 is the same feature,
designed differently and measured:

| | #334 as filed | lf-06 §C1 |
|---|---|---|
| Reference form | new `sref` item **beside** `ref` | `ref`/`probe`/`pair` anchors carry sids |
| `int id` line | never mentioned | **deleted from the format** |
| Compatibility | "both forms for one declared epoch with a written end" (Open Question 1, *blocks filing TASK-0005*) | `FORMAT 3`; readers keep v0–v2 forever, per `docs/file-format.md:420-446` |
| Target number | "at most 12 lines in at most 2 hunks" (underived) | 5,314 → **9** lines, measured on `riscv/build/addi.jls` at HEAD |
| Container work | welded to the reference change, "one shipment" | `-canon` + git clean filter, **ships alone, ~1 week** |

Four things follow.

**1. The epoch question is self-inflicted.** `docs/file-format.md` §9 already
requires a version bump for "the meaning of an existing record" and already
promises that readers accept every prior version forever. Bumping to `FORMAT 3`
*is* the epoch mechanism, it is declared in the file rather than inferred from
grammar sniffing, and it has no end date to argue about. Open Question 1 — the
thing the issue says blocks filing TASK-0005 — dissolves. A `sref` item beside
`ref` gives the reader two live reference grammars with no version discriminator,
which is strictly the worse half of the choice the project already made.

**2. The stated acceptance criterion is unreachable as written.** §1 criterion 3
bounds a one-element insert at 12 lines/2 hunks, but `int id` is emitted for
every element (`Element.java` `BASE_ATTRIBUTES`, first entry) and assigned by
position (`Circuit.java:1498-1502`). Adding `sref` without deleting `int id`
leaves ~1,000 changed `int id` lines on the measured fixture. Either the issue
silently intends to drop `int id` — in which case §3's "Modifies" list is
incomplete and the `sref`-beside-`ref` framing is wrong — or the ratchet cannot
pass. Deleting `int id` is the whole point; say so.

**3. The format gets simpler, not richer.** Post-change, `docs/file-format.md`
§8 stops explaining two opposed id concepts and explains one. `NetBlocks`'
local renumbering (its class comment, lines 32-34, and the save/restore of
`priorIds`) is *deleted*, not adapted. `AddWire` stops carrying anchors as a
stable-id sidecar beside blocks that address them positionally
(`docs/operation-layer.md:82-86`). A feature that removes a concept is worth
more than one that adds a third grammar item, and the issue should be priced
and reviewed as a deletion.

**4. The measurement already exists.** The issue's evidence commit `2d0ca9d`
is on a planning branch being deleted (#493), and its `blocked_by` rationale
cites `970db41`, which is not in the repository (recorded in the 2026-08-08
comment). Meanwhile a reproducible 5,314→9 measurement sits in-tree. Re-anchor
§5 criterion 1 on that experiment and the underived "12 lines / 2 hunks"
disappears too.

## Reframing 1 (primary): decouple the container, ship the filter first

§2 rejects "one task" and "three tasks", but never considers the alternative
that dominates both: **make the canonical text reachable from the command line
and let git store it, without touching the default container at all.**
`clean = jls -canon -`, identity smudge, one `.gitattributes` stanza. Because
the loader sniffs content (`FileAbstractor.openCircuit`, ARCHITECTURE.md
"save/load pipeline"), plain text *is* a valid `.jls`, so the working tree keeps
whatever the user saved while the object database holds canonical text. This
delivers reviewable diffs, delta compression, and GitHub-rendered reviews to
**every existing repository of XZ `.jls` files**, needs no format epoch, and
regenerates no goldens. lf-06 prices it at ~1–1.5 weeks and explicitly says it
can ship alone; it also carries two real bug fixes the issue never mentions —
`-savetext` cannot write to stdout (`JLSStart.java:1112-1128`) and
`-b -savetext` is a silent no-op (the mode chain at `:168-478`).

The issue's argument for welding the halves ("flipping the container first
exposes the amplification to git without fixing it") is true only of flipping
the *default*. It is not true of adding `-canon` and a filter. Recommended cut:

- **A — `-canon` to stdout, the two CLI bugs, git filter, `docs/version-control.md`.** Ships alone, immediately.
- **B — `FORMAT 3`: refs by sid, `int id` deleted, `NetBlocks` renumbering removed, goldens re-baselined once.** The one epoch, the one golden regeneration.
- **C — default container flips to plain text.** Now a one-line policy change with a trivial rollback, decided on its own merits.

That ordering satisfies §5 criterion 4 ("goldens regenerate exactly once") more
cleanly than the filed order, because A touches no golden at all.

## Reframing 2: assert locality, not a line count

§5 criterion 1 measures a proxy with magic numbers that will rot the moment an
element type gains a saved attribute. The property actually wanted is exact and
has no numbers in it:

> **Block locality.** The bytes of element *e*'s save block are a function of
> *e*'s own state and the sids it references — of nothing else in the circuit.

Test: save a fixture, split into blocks keyed by sid, insert/delete/move one
element, re-split, assert every surviving block is byte-identical. It needs no
`git diff --stat`, no diff parser, no per-fixture calibration; it generalizes
over all three replica-id classes for free; the 9-line diff bound is a corollary
rather than a separate assertion; and it survives #319's section reframe intact.
I would replace criterion 1 with this and keep the 5,314→9 reproduction as
evidence in the closing comment rather than as the ratchet.

## Reframing 3: the grading oracle should be container-independent

§5 criterion 2 — `sha256sum <file>` equals `Circuit.stateHash()` — welds a
semantic identity to a packaging choice. It holds only while the student did not
pick XZ in Save As, and it is silently false for every file in existence today.
`stateHash()` is already SHA-256 of the canonical *text*
(`Circuit.java:1548-1569`). The right instructor-facing contract is
`jls -canon file | sha256sum == Circuit.stateHash()`, which is true under every
container, true for legacy files, and true whether or not C ever lands.
**I am disregarding §5 criterion 2 as stated**, because the criterion it should
be replaced by is strictly stronger and is available a week from now instead of
after a format epoch. It also retires Open Question 4: whether XZ write survives
stops mattering to anyone.

## Reframing 4: reject the per-file replica alias table

Open Question 5 recommends an alias table to recover the byte cost of 32-hex
replicas. An alias table is a file-local indirection whose token assignment
depends on which replicas happen to appear — that is the positional-identity
failure mode reintroduced one level up: a new replica shifts tokens and the
churn comes back. If replica width is genuinely a problem, either shorten the
draw (64 bits, 16 hex, is collision-free at this scale) or accept it, since a
clean-filtered, delta-compressible file makes the raw byte count uninteresting.
If a table is kept anyway, tokens must be *derived from* the replica string
(e.g. a hash prefix), never assigned in order of appearance.

## Where the issue's scope is drawn too narrowly

**Verilog export has the same defect.** `HdlExporter` synthesizes net, register,
mux, decoder, truth-table and state-machine names from `getID()`
(`src/jls/hdl/HdlExporter.java:346,367,581,667,695,812,945,1026,1255`), so
`-export out.v` renames a large fraction of the module on any insert, for the
same reason and with the same review and grading audience. The capability
statement should read "no emitted artifact carries positional identity", and
this pass should either re-base HDL names on a sanitized sid hash (`sid` has a
colon, so it needs one) or record the leak against a named owning issue.

**The legacy-identity assessment is inconsistent with the tree.** Open Question 5
treats `legacy:` purely as an ordering trap (`'f' < 'l'`). lf-06 §4 calls
positional legacy minting "the single largest correctness hazard in the whole
capability", applying to 100% of existing content. The honest reconciliation:
two loads of the *same ancestor bytes* mint identical legacy ids, so the hazard
is confined to independently-derived lineages and to files never re-saved by a
post-#165 JLS — narrower than lf-06 says, wider than an ordering fix addresses.
The `FORMAT 3` rewrite is the one free migration window this project will get
(every file is rewritten once anyway); deciding legacy adoption there, rather
than deferring it, is worth more than the alias table it is currently bundled with.

## Bottom line

The destination is right and the project's whole collaboration/VCS arc runs
through it. The route as filed adds a grammar item, invents an epoch mechanism
the format already has, welds a one-week win to a multi-week epoch, and states
two acceptance criteria that are respectively unreachable and container-dependent
— while a measured, cheaper, subtractive design for the same capability is
already written in `docs/capability-roadmap/lf-06-diff-merge-vcs.md` §C1. Adopt
that design, re-cut into A/B/C above, replace criterion 1 with block locality
and criterion 2 with the container-independent hash, and this becomes the
highest-leverage feature in the cluster rather than a format epoch with an open
end date.
