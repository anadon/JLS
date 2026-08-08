# Issue #447: TASK-0041: one subcircuit definition is stored once and referenced by N instances with bound parameters, instead of ten instances storing ten copies
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Two different goods are bundled under one title. The first is **storage**: a `.jls`
whose size is linear in instance count. The second is **semantics**: an instance
that *is* a definition rather than a photocopy of one, so an edit lands everywhere
and `#340`/`#358`/`#163` have something real to name. The title, the abstract, the
O4 probe, the §7.10 asymptotics and half the acceptance criteria are about the
first. The `Intended Audience` section, P3, and every downstream consumer are about
the second. They are not the same problem and they do not want the same solution.

## 1. The headline harm is measured on a plane no user ever sees

O4 measures `Circuit.save` into a `StringWriter`. A `.jls` is XZ
(`FileAbstractor`, `README.md` "Circuit files"; plain text is opt-in via `-savetext`).
N near-identical blocks are the single most compressible thing a file can contain.
I synthesized a top-level circuit shaped like the O4 probe (an 8-`AndGate`
definition, distinct per-instance `sid`s so the copies are *not* byte-identical) and
compressed it (`xz -9`):

| instances | plain chars | `.jls` bytes |
|---:|---:|---:|
| 1 | 1,172 | 260 |
| 10 | 11,450 | 424 |
| 50 | 57,610 | 976 |

Ten instances cost **1.63x**, not 9.83x; fifty cost 3.75x. The measurement is
synthetic and does not reproduce JLS's exact bytes, but the compressibility of
repeated blocks is not in doubt, and `lf-01` already reached the same conclusion in
this tree — *"the container is XZ and repeated blocks compress hard, and the
alternative trades the entire compatibility property for bytes."* The issue's
"measurable harm today: a file whose size is linear in instance count" is true of an
intermediate string and roughly false of the artifact. A block-structure format
break, a `FORMAT` bump, a two-form reader epoch with an undecided end date, and a
full golden regeneration are being bought with a number that the container already
pays for. Where duplication genuinely still bites is plain-text saves in version
control — a real but much narrower harm than the one stated, and one that deserves
to be argued on its own terms rather than smuggled in behind a size table.

## 2. The compatibility budget is spent protecting the wrong party

§7.12 makes "every flat and inlined file stays readable by current JLS" the
load-bearing compatibility claim, and P5 spends the whole `formatVersionNeeded`
mechanism on it. That protects *other readers* from a representation they cannot
parse. It does nothing about the change that actually reaches a user: after this
lands, opening a file whose ten copies happened to be structurally identical and
nudging one gate can move ten instances. A version header cannot warn you about
your own file changing meaning in your own editor. #357's Open Question 1 is exactly
this and it is still unratified — yet #447 declares itself merely blocked on #417's
digest. **The pivot of this whole feature is a user-facing linkage model — "this
instance is linked to definition X" versus "this instance is a detached copy" —
and it is nowhere in this issue.** Today's `Import` genuinely makes a copy, and some
students rely on tweaking one. H2's falsification note waves this away ("that caller
is a latent bug today"); it is not a bug, it is the current contract.

## 3. Alternative framing: share in memory, materialize on disk

The cheaper cut, and I think the better one:

- **Intern definitions at load.** `Circuit` gains the definition table exactly as
  §7.4 describes, but it is populated by hash-consing the inlined bodies as they
  load, keyed by #417's digest. Two structurally identical nested blocks resolve to
  one `Circuit` object.
- **Keep the file exactly as it is.** The writer re-inlines the shared definition
  once per instance. Zero grammar change, zero `FORMAT` bump, zero two-form reader
  epoch, zero `docs/file-format.md` §7/§9 restatement, zero third-party reader
  breakage, and — critically — **zero golden regeneration**, so #357's Invariant 1
  ("every existing simulation golden is byte-identical") is trivially preserved
  instead of being the thing most at risk.
- **Ship the linkage UI in the same change**, because it is now the only user-visible
  behaviour change: the instance dialog states "shared with 9 other instances", with
  `Detach this instance` as one undoable gesture. That is #357's Open Question 1
  answered as an affordance rather than as a load-time policy.

This delivers P2 (one object for two instances), P3 (a definition edit changes every
instance, asserted through simulation), and the whole of #357's capability sentence
except the file-size clause. It also delivers everything the downstream consumers
actually asked for: #358's hierarchical export, #340's library, #163's "which
definition are we editing" all read the *model*, not the text. The reference form
then becomes what it should have been all along — a size-and-diff optimization that
can ride #319's must-understand section framing whenever that lands, or never.

The ordering inverts, and that is the point: **semantics first at near-zero
compatibility cost; representation second, optional, deferrable.** Today the issue
has it backwards, and pays the entire format-break cost up front for the half of the
value the container was already giving away.

## 4. A smaller, sharper alternative inside the issue's own Open Question 2

Open Question 2 asks how `Circuit.subElement` is replaced and recommends "a set of
sub-elements." Both options keep a back-pointer from a shared thing to its users,
which is precisely the aliasing hazard the issue calls "the single most invasive
consequence." The elegant move is to **delete it**. `subElement` is read in three
places (`Circuit.java:297`, `:1479`, `:1676`) and only ever to answer two questions
at save time: am I a top-level block, and what name does this block carry. Both are
arguments, not state: `save(PrintWriter, @Nullable String blockName)`. A shared
definition then has no opinion about who instantiates it, `setImported` disappears
along with its whole class of staleness, and the "N sub-elements" crisis never
arises. This is worth doing under either framing and would make the change smaller,
not larger.

## 5. The parameter half contradicts the project's own worked design

`docs/capability-roadmap/lf-01-parameterization.md` is the most thought-through
document in the tree on this subject, and #447 cites only its "what is missing"
paragraphs while silently reversing two of its decisions:

- **D1**: parameters are declared by a drawable `Parameter` element, argued from the
  grammar — *"a `CIRCUIT` block has no attribute items, only elements. A declaration
  that is not an element cannot be saved without restructuring the grammar."* #447's
  §7.6 puts declarations in a `DEFINITION` section, i.e. restructures the grammar.
- **D4**: the elaborated int is always written beside any expression, which is what
  makes the whole programme migration-free. #447 does not engage with it.

More importantly, §7.10 concedes that bindings "do not reach any pin width" and §8
requires the dialog to say so. **A parameter dialog that visibly does nothing is a
negative deliverable.** lf-01 names the useful floor precisely — expression language
+ `Parameter` element + expression-backed `bits` + bindings on `SubCircuit` — as the
slice that yields "one adder drawing, any width," which is the thing an instructor
would notice. Splitting the parameter work at "persisted and validated but inert"
optimizes for a clean task boundary at the cost of shipping a feature to nobody.
Under the reframing above, the parameter half should be deferred out of this task
entirely and taken up as lf-01's floor, where it is independently shippable.

## 6. Trajectory

The direction is right and it is the maintainer's own recorded biggest win (D7:
libraries are data; the split is the prerequisite). But the route is long: #447 sits
under #357, which is blocked by #318, #319 and #340, needs an unfiled TASK-0033 for
its section frame, hands off to an unfiled TASK-0042, and precedes a residual #357
itself prices at 6-9x the sum of its filed tasks. Nothing user-visible exists until
the far end of that chain. The reframing detaches the semantic win from #319 and
#340 entirely and makes it landable behind #417 alone.

## 7. Acceptance criteria I am explicitly disregarding

I would not ship, in this task: the `DEFINITION` section (§7.1, §7.6), the
`FORMAT_VERSION` advance and P5, the two-form reader epoch and its undecided end
(§7.12, Open Question 1), the pre-split fixture built to test the backward half, the
`docs/file-format.md` §7/§9 restatement, and the golden regeneration. None of them
buys a user anything the XZ container does not already provide, and each is
irreversible in a way the in-memory design is not. I would keep, and sharpen: the
definition table with a content-derived canonical order, the three copy sites
becoming references, digest-keyed registration, the cycle refusal, `(instancePath,
sid)` identity (P8), determinism (P9), and P2/P3 as the real tests — plus a linkage
affordance the issue does not currently have.

## What would change my mind

A measurement on the delivered artifact: take a real multi-instance design, save it
as an actual `.jls`, and show XZ growth that is materially super-linear. Or a
concrete plain-text/VCS workflow where the inlined diff is the blocking problem. If
either holds, the reference form earns its format break — but it should still land
*after* the semantics, on #319's must-understand frame, not in front of them.
