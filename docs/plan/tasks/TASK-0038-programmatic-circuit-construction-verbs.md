# TASK-0038 - Programmatic circuit construction verbs

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0037

## Deliverable

A documented, tested, public way to build a circuit from a program - through
ops, not through emitted save text - and the in-tree consumers of the
emit-text-and-reparse idiom migrated onto it.

1. **The verb set.** A public builder in the headless kernel (`jls.api`, or
   `jls.collab.op` if `jls.api` does not yet exist) exposing exactly:
   `newCircuit(name)`, `place(typeToken, attributes) -> ElementId`,
   `connect(ElementId, putName, ElementId, putName)`, `configure(ElementId,
   attributes)`, `remove(ElementId)`, `probe(ElementId, name)`, and
   `build() -> Circuit`. Every verb constructs a `CircuitOp` and submits it
   through an `OpSink`; **no verb writes save text and no verb reparses one.**
2. **Attributes are typed, not stringly.** `place` and `configure` take a map of
   attribute name to a typed value matching the four item kinds the format
   carries (`int`, `long`, `Int`, `String`); the builder renders them into the
   element's block through the same escaping the writer uses
   (`src/jls/collab/op/Ops.java:47-54`), and rejects an attribute name the
   target type does not declare in `savedAttributes()`
   (`src/jls/elem/Element.java:316-319`) - so a typo is a diagnostic here
   instead of a silently dropped value at load
   (`docs/file-format.md:222-228`).
3. **Wiring is a first-class verb, at net granularity.** `connect` composes into
   `AddWire`, and connecting a put onto an existing net travels as `RemoveWire`
   of the old net plus `AddWire` of the merged one - the composition
   `docs/operation-layer.md` already specifies. The caller never sees wire ends
   or file-local ids.
4. **The consumers migrate.** `test/jls/CircuitTextBuilder.java:14-45` and
   `test/jls/hdl/HdlCircuitBuilder.java:17-45` are reimplemented over the verbs,
   keeping their existing method signatures so the golden suites that use
   them are untouched. `riscv/jlsbuild.py` - the out-of-tree Python netlist
   compiler that emits `.jls` text - is named in the migration note as
   superseded; D5 deletes `riscv/` and TASK-0025 owns the deletion.
5. **The documentation.** A new `docs/programmatic-construction.md` (or a
   section in `docs/operation-layer.md`) giving the verb list, one worked
   example that builds and simulates a two-input gate, and the explicit
   statement that emitting save text from a program is not supported.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-015 | The public verb set *is* the second half of the feature; the op layer without it has no supported caller outside the editor. |
| FEAT-038 | A drawn RV32 machine is built and re-built hundreds of times during bring-up; doing that by hand-emitting element blocks is how `riscv/jlsbuild.py` came to exist, and D5 deletes it. |
| FEAT-039 | The same, for the ternary machine, which additionally has no drawn precedent to copy from. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0037 | Every verb applies a `CircuitOp`. Until `apply` takes a `TextMetrics` rather than a `Graphics`, a program with no display cannot call one - which is the entire use case. |

## Acceptance test

`test/jls/api/CircuitBuilderTest.java`, new:

- `aBuiltCircuitSavesIdenticallyToTheSameCircuitBuiltFromText()` - build the
  `gate_and` fixture with the verbs and with `HdlCircuitBuilder`'s pre-migration
  text, pin `ElementId` (`ElementId.pinForTesting`,
  `src/jls/elem/ElementId.java:170-181`) so both mint the same sids, and assert
  the canonical saves are byte-identical. This is the proof that the verbs are
  not a second, divergent construction path.
- `aBuiltCircuitSimulatesIdentically()` - run the same batch vectors over both
  and compare the trace output byte for byte.
- `anUndeclaredAttributeNameIsRejected()` - `place("AndGate", {"bts": 2})`
  throws with a message naming the element type and the attribute; must fail
  today, where the loader drops it silently.
- `aTypeTokenOutsideTheVocabularyIsRejected()` - naming a type absent from
  `ElementVocabulary.ALLOWED` (`src/jls/collab/op/ElementVocabulary.java:38-45`)
  is a rejection, not a reflective load attempt.
- `connectingTwoPutsOnOneNetProducesOneNet()` - the fan-out case, asserting one
  `WireNet` and not two.
- `everyVerbIsExercised()` - a reflective enumeration of the builder's public
  methods asserting each appears in at least one test, so a verb cannot ship
  untested.

`test/jls/hdl/VerilogExportGoldenTest` and `VhdlExportGoldenTest` are the
regression gate: every HDL export golden under `test/resources/hdl/` (32
`.v` and 32 `.vhdl` files) must stay byte-identical after
`HdlCircuitBuilder` is reimplemented over the verbs. A golden that moves means
the verbs build a different circuit.

## Related GitHub issues

**No issue** for the public verb set itself; the programmatic-construction half
of FEAT-015 is unfiled.

| # | title | relationship |
|---:|---|---|
| 167 | Operation layer: reify editor mutations as invertible, serializable commands behind one entry point (collab Stage 0b) | depends on - the verbs are a façade over #167's vocabulary and add no second grammar |
| 170 | Collaboration security hardening: closed op vocabulary, element-type allowlist for network input, caps, ratchet tests | overlaps - the builder must go through the same closed vocabulary, or it becomes the hole #170 closed |
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | overlaps - the worked example is the first real consumer of the verbs |
| 212 | Element-provider plugin API: discover external `ElementType` descriptors via `ServiceLoader` atop the #78 registry | informs - a provider-contributed type must appear to the verbs exactly as a built-in does |

## Notes

- **This is a façade, not a second grammar.** `10-capstone-plan.md`'s
  deduplication record folds the headless op layer and the construction verbs
  into one feature precisely because splitting them "invites two op grammars".
  If a verb needs a mutation the op vocabulary cannot express, the fix is a new
  op kind in `CircuitOp`'s permits list, not a bypass.
- **`ElementBlocks.MAX_BLOCK = 100_000`** (`src/jls/collab/op/ElementBlocks.java:28`)
  caps one serialized block. A `StateMachine` or a `Memory` with a large `init`
  can approach it; the builder must surface that as a diagnostic naming the
  element, not as a parse failure.
- **`AddElements` rejects wire, wire-end and subcircuit blocks** by design, so
  `place` cannot be used for wiring or for hierarchy; `connect` and (later) an
  import verb are the paths. Say so in the documentation rather than letting a
  caller discover it as a rejection.
- **The `ElementVocabulary` list is a stopgap that will move.** Its own javadoc
  says it should delegate to the element registry (issue #78) when that lands
  (`src/jls/collab/op/ElementVocabulary.java:25-29`); `ElementVocabularyTest`
  cross-checks three views today. Route the builder through the vocabulary, not
  around it, so the later delegation is one edit.
- **Reproducible construction needs a pinned replica.** Two runs of a builder
  program mint different `sid`s unless `jls.replicaId` is set
  (`src/jls/elem/ElementId.java:54-57`); the documentation must say so, because
  a generated circuit that is not byte-reproducible cannot be a golden.
- **Do not put the verbs in `jls.edit`.** The whole point is that they run
  headless; `HeadlessCoreRatchetTest` must cover their package from birth, with
  no baseline entry, the way `jls.core`, `jls.hdl` and `jls.module` were.

## Evidence

- `test/jls/CircuitTextBuilder.java:14-45` - the shared in-test builder that
  appends `ELEMENT` blocks to a `StringBuilder`; `test/jls/hdl/HdlCircuitBuilder.java:17-45`
  - its HDL sibling, "text exactly as the editor saves it, loaded through the
  real loader".
- `riscv/jlsbuild.py:1-19` - "a small netlist compiler that emits JLS `.jls`
  circuit text", the out-of-tree instance of the idiom this task replaces; D5 in
  `BRIEF.md` §11 deletes `riscv/` and requires its replacement to be "a
  first-class, in-tree, tested JLS mechanism".
- `src/jls/collab/op/Ops.java:24-54` - stable-id resolution and the save-format
  escaping the builder must reuse.
- `src/jls/collab/op/ElementVocabulary.java:31-45` - the 34-token closed type
  list and its three-way cross-check.
- `src/jls/elem/Element.java:316-319` (`savedAttributes()`) and
  `docs/file-format.md:222-228` (unknown attribute names are silently ignored) -
  why the builder must validate names itself.
- `docs/operation-layer.md` - the element and wire-net transplant contracts,
  including the `RemoveWire` + `AddWire` composition for a net merge.
- Do not restate: `docs/operation-layer.md` owns the op contract;
  `CONTRIBUTING.md` owns the coverage-floor rules a new package enters under.
