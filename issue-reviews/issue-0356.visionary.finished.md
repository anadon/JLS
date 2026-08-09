# Issue #356: FEAT-012: a merged .jls file either means what both authors meant or is refused by name — no third outcome where it loads and simulates a circuit neither drew
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of the tier apparatus, #356 buys one thing: **the model has two entry
paths and only one of them checks anything.** The editor gesture path enforces
bit-width agreement in four hand-copied places (`src/jls/edit/SimpleEditor.java`
lines 4015, 4142, 4247, 4358 — one rule, four call sites, no shared predicate)
and name uniqueness in ten dialog/paste sites. The load path
(`Circuit.load` → `Circuit.finishLoad`) enforces referential integrity and `sid`
uniqueness and nothing else. Merge is not the disease; it is the first symptom
loud enough to get an issue filed. The same hole is walked by `CircuitSnapshot`
undo restore (the issue notices this), by checkpoint recovery, by paste, by
network ops (#170), by a converged CRDT replica (#279/#280), by HDL import
(#33/#59), and by anything the `riscv/` build emits.

The issue half-sees this — Open Question 3 states the general rule ("any
invariant enforced only by the editor is a silent-merge hazard by construction")
and then files it as a question, blocking only §4 invariant 1. **That question is
the feature.** Everything else in #356 is scaffolding around it.

## Where this sits in the project's arc

`docs/capability-roadmap/lf-06-diff-merge-vcs.md` is the long-form version of
this territory and is the better-argued document. It sizes the whole
diff+merge+VCS capability at 18–27 mw and decomposes it as **diff 9–13 mw (worth
doing regardless, because grading and CI need no collaborative workflow to
exist)** and **merge +9–14 mw (worth doing only if the maintainer intends pairs,
repos, or #163)**. #356 takes the merge half's legality slice, prices it at
9–13 mw — the same budget lf-06 assigns to the entire diff half — and carries a
3.5–7.5 mw residual with no owner (its own Open Question 1).

That is a portfolio inversion. The README's arc is installers, reproducibility,
batch autograding, VCD/HDL bridges: a single-maintainer tool serving students and
instructors. What those users would notice tomorrow is `jls -diff` printing
*"+ AndGate and3; ~ Register acc delay 5 → 12"* and an SVG overlay (lf-06 C2/C3).
What #356 ships is a merge that refuses by name.

## Finding 1 — the capability statement is satisfiable by one line, today

`.gitattributes` already exists at the repo root and already carries a `*.jls`
stanza (`*.jls -text`, for the CRLF guard). Adding `-merge` to it makes git
refuse to auto-merge `.jls` at all. The entire measured hazard — `git merge-file`
exit 0, zero conflict markers, a file that loads and simulates and prints
`0xFFF` on a pin the file declares as 4 bits — **stops existing**, at zero code
and zero maintenance surface.

This matters because §5 criterion 1 asserts *"either the textual merge conflicts,
or the merged file loads, validates, and its element multiset equals the intended
result."* Under `-merge` the first disjunct holds for all nine rows
unconditionally. **The flagship integration criterion of a 9–13 mw feature is
vacuously satisfied by a one-line change to a file already in the tree.**

The real gap is distribution, not capability: JLS's `.gitattributes` does not
travel into a student's lab repo. The shippable version is
`jls --install-git-config` (lf-06 C5 already names it) plus one line in the lab
skeleton — days, not months. The issue never considers refusal-by-default as a
resolution of its own disjunction, and it should, because that is what separates
the cheap guarantee ("no corrupt merges") from the expensive one ("good merges")
that Open Question 2 already doubts will be funded.

## Finding 2 — the premise "references name their referent" is false today

Every `.jls` in this tree and in the world has `sid` values of the form
`legacy:N`, minted **positionally at load** for files predating #165
(`docs/file-format.md` §8; `Circuit.finishLoad`). Deterministic per file, and
therefore worthless across two independent edits of one skeleton: two students'
`legacy:7` are unrelated elements. #356 keys its whole rule table on stable ids
and treats id collision as one of nine matrix rows. lf-06 is blunter and, I
think, right: *"if this is treated as an edge case rather than as the default
case, the feature ships broken,"* with the mitigation being a structural refusal
to merge `legacy:`-id files plus a `jls -adopt` migration.

There is no adoption task on the roster (TASK-0005/#436, TASK-0031/#409,
TASK-0032/#415, plus the adopted #491). That is a missing critical-path item, not
a detail — and it is plausibly a chunk of the unowned 3.5–7.5 mw residual.

## Finding 3 — a different seam: merge op logs, not files

`src/jls/collab/op/` already ships a sealed, closed, invertible, validate-then-
mutate edit algebra (11 ops; `CircuitOp.apply` documented "validation failures
throw before any mutation happens"; `SetElementConfig` rejects a name already
taken, `:214`). lf-06 §1 records that the `collab.op-observer` seam exists with
**zero contributors** and no op log is persisted anywhere.

Persist one. An append-only `alu.jls.ops` sidecar, plain text, marked
`merge=union` in `.gitattributes`, costs nothing to merge — git's union driver
concatenates — and the three-way merge becomes: load base, replay the union of
both suffixes through `OpSink`, canonical-save. Every invariant the editor
enforces is inherited free, which is precisely the property #356's §4 invariant 4
tries to legislate. The per-record-kind table then shrinks from "a row per record
kind with a totality test against the format spec" to "a tiebreak policy for
genuinely concurrent writes to one attribute" — a handful of rules over an
add-wins element set, per-attribute registers, and an OR-set of wires, which
`docs/collaborative-editing-research.md` §3 already writes down.

Note what this does to the tracker anxiety: three comments on this issue exist to
keep #279's online table and #415's offline table from becoming two answers,
complete with a §7 inversion protocol and a "do not file a third rule table"
injunction. **When a decomposition needs an inversion protocol to stop one object
from being built twice, it is cutting across the grain.** One table, one owner,
two columns of one data class.

## Concrete alternative framing — FEAT-012′, "one invariant set, every entry path"

Same end, different shape, roughly a third the budget:

1. **Deduplicate the four `Bits don't match` sites into one model-level
   predicate** outside `jls.edit`. A day, not 1.5 weeks, and it makes the
   flagship silent class shared by construction rather than reimplemented on the
   load path.
2. **A declarative invariant registry + a ratchet test** asserting no invariant
   is reachable only from `jls.edit` — the structural form of Open Question 3.
   #356 instead freezes `|S| = 2` (today's measured classes) into its
   transformation equation and Definition of Done: a snapshot of a moving target.
3. **TASK-0031 (#409) unchanged** — the strongest child, needing neither the rule
   table nor `sref`. Run it on snapshot restore too, per §4 invariant 5.
4. **`--install-git-config` + `-merge`** as the interim guarantee, shipped now.
5. **TASK-0005 (#436) reframed as diff-stability, not merge-enablement** — it is
   lf-06 C1b, it turns a 5,314-line diff into 9, and it pays for itself on PR
   review of `riscv/` circuits whether or not anyone ever merges. #334 is its
   honest home.
6. **Defer the rule table behind the differ.** Fund `jls.diff` first; if "two
   students on one lab in git" proves to be a real workflow rather than an
   assumed one, the table follows with an op log under it.

## Acceptance criteria I am explicitly disregarding

- **§5 criterion 1** (nine-row merge-safety matrix) — vacuously satisfiable, per
  Finding 1. Replace with: *every non-editor entry path into the model runs the
  same invariant set as the editor gesture path*, asserted per path (load,
  snapshot restore, checkpoint recovery, paste, op apply), not per merge scenario.
- **§5 criterion 2** (totality over record kinds) — the wrong totality. Totality
  over *invariants and entry paths* is the property that cannot silently rot;
  totality over record kinds is a table whose row count depends on a section
  frame (#319) that does not exist yet, which is exactly why Open Question 1's
  residual cannot be sized.
- **The `blocked_by: [319, 334]` chain** — correct for the feature as written,
  and the reason nothing here is parallel. Under the reframing, items 1–4 above
  are unblocked today and can land while #334 and #319 are still in flight.

## What I would not change

The core diagnosis is right and undersold: two entry paths, one validator, and
an undo stack that inherits whatever the load path accepts. The insistence that a
merge express itself as ops rather than text is exactly right and is the one
place the existing architecture already hands the project a win. The refusal to
derive the offline table from #279's CRDT design is right. TASK-0031 should be
funded regardless of everything above. The issue's own Open Question 2
("does the merge driver ship at all?") already contains most of this review;
I am arguing it should be answered before the feature is funded, not after.
