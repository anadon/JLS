# Issue #615: TASK-C558-3: a parameterized Digital circuit either maps with its parameters bound or refuses by name — never a silent flattening
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

The purpose is not "support generics." It is: **an instructor migrating a course
off Digital must never be handed a circuit that looks imported and is quietly a
different design.** That purpose is the same one that produced the loud loader
(#314), the `LoadError` taxonomy (ARCHITECTURE.md "Error-reporting contracts"),
and #558's whole framing. On that axis the issue is aligned with the project's
arc and I endorse the goal without reservation.

The route it picks is where it goes wrong. It builds a binary — bind the
parameters, or refuse by name — and then hangs the good half on #357, a 25–36 mw
program with three unlanded format prerequisites (#340, #318, #319) and an
unratified Open Question 1. What ships in the near term is therefore only the
refusal half: an instructor whose Digital course uses the feature Digital's own
README advertises ("a barrel shifter with a selectable bit width") gets a report
and no circuit.

## The false dichotomy, and the third option the project already chose

There is a third disposition, and JLS's own roadmap already ratified it.
`docs/capability-roadmap/AMENDMENT.md` (P7, "Compatibility, which is unusual in
this roadmap: there is no migration") states the native representation of a
parameterized JLS circuit: **the `.jls` always contains the fully elaborated
circuit, and the parameterization is additive metadata beside it.**
`docs/capability-roadmap/lf-01-parameterization.md` §D4 says the same at
attribute granularity — `int bits 32` always written and always holding the
elaborated value, with `String bitsExpr "N"` an optional sibling.

So an elaborated body is not the failure mode JLS is defending against; it is
JLS's chosen normal form. Two further facts close the argument:

- **Digital already flattens.** `ResolveGenerics` deep-copies the template and
  returns a `CircuitHolder` of the *concrete* circuit plus the args that made it
  (quoted in lf-01 §"hneemann's Digital"). An elaborated import is faithful to
  the source tool's own semantics, not an approximation of them.
- **JLS already flattens.** lf-01 measures three independent deep-copy sites —
  `SubCircuit.copy`, `SimpleEditor.doImport`, `Circuit.loadElementItems` — and
  concludes "JLS's hierarchy is elaborated-by-copy already." An imported
  8-instance Digital generic is exactly as much a set of unrelated object graphs
  as an 8-instance *native* JLS design is today.

The sin the outcome paragraph actually names — "N copies that look imported and
then diverge the moment anyone edits one" — is **undeclared** flattening, not
flattening. AC-2 outlaws the mechanism instead of the silence.

## Concrete alternative: elaborate-and-declare

Replace the two dispositions with three, and the task ships whole today with no
dependency on #357:

1. **`mapped`** — a generic whose bindings are plain integer values that land in
   JLS's existing `bits`/`numInputs`/`cap` ints. Elaborate; nothing is lost.
2. **`elaborated-at`** — a generic resolved at its instantiated arguments into
   concrete elements, with the parameterization recorded as inert provenance on
   the imported subcircuit (source generic text, argument bindings, definition
   identity) and one report entry per instance: *"resolved at N=8; the
   parameterization itself is not yet representable — #357/P7 will lift it."*
   The instructor gets a running circuit **and** the truth about it.
3. **`refused`** — structural generics whose HGS calls `addComponent`/`addWire`,
   loops, or reads signal values. These are refused, with the recipe below.

Disposition 2 is not a compromise; it is the P7 compatibility story applied one
release early, and it makes the eventual #357/P7 migration a *local* pass over
recorded metadata rather than a re-read of the `.dig`. It also dissolves the
collision with sibling #619 AC-2 (import is atomic — whole circuit or nothing):
under map-or-refuse, a refused generic inside an otherwise-mappable circuit is
precisely the partial circuit #619 forbids, and neither issue says who wins.
Under elaborate-and-declare there is no hole to arbitrate.

## AC-3 is a promise the roadmap has already decided not to keep

I am explicitly disregarding AC-3 as written. It requires the refusal to name
#357 as the successor "so the report tells the instructor what would unblock
it." For the class of generics that will actually be refused — the procedural
ones — **#357 is never the successor, and neither is P7.** lf-01 §D3 rejects
Digital's expressiveness on purpose: "total integer arithmetic, frozen,
specified… No loops. No recursion. No user-defined functions," because
`docs/file-format.md:67-71` says circuit files are exchanged between untrusting
parties and *a file whose meaning requires running a program is not a data
format.* A report that tells an instructor to wait for #357 is telling them to
wait for something that has already been designed to refuse them.

The honest successor text is the one KC-29-1 already contemplates: **"open this
circuit in Digital, resolve the generic there, save the resolved circuit, and
import that."** One documented recipe converts the hardest construct class into
the already-solved class, costs a paragraph, and is available now. AC-3 should
name that recipe first and #357/P7 only for the value-parameter case where it is
genuinely the unblocker.

## Two under-specifications the ACs hide

- **The branch predicate is the whole task, and is unwritten.** "Parameters JLS
  can express" is not decidable by inspection; deciding it requires knowing what
  the HGS does. AC-4's two-fixture test pins the branches but not the predicate,
  so the implementation is free to define "expressible" as "no `GENERIC`
  attribute" and stay green. Specify a closed whitelist — `GenericInitCode`
  containing only `name := <int literal>` assignments, `GenericCode` containing
  only attribute sets — with any `addComponent`/`addWire`, loop, string, or
  signal reference forcing the structural class. That predicate belongs as rows
  in **#614's** reviewable `docs/` table, beside the element rows; it is the same
  artifact and should not become a second one.
- **The ordering edge points at the wrong issue.** `ordering_after: [357]` buys
  one-definition-N-instances with *value* bindings (#357 Open Question 2: "values
  only"). Digital's generics parameterize widths, which is P7's §D4
  expression-backed attributes, not #357's definition table. The mappable half's
  real prerequisite is P7; #357 is neither sufficient nor, for disposition 1,
  necessary. A `band_mw: "1"` task should not carry a pointer into a 25–36 mw
  program at all.

## What I would keep verbatim

The outcome sentence, AC-4's both-halves fixture (it is the right instinct —
apply it to all three dispositions so `elaborated-at` cannot rot into either
neighbour), and the boundary note's honesty that the refusal half is what makes
the task shippable alone. Under the reframing, *every* half is shippable alone,
which is the point.
