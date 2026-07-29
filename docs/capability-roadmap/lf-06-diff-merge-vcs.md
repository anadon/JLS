## Semantic schematic diff, three-way merge, and version control

*Area LF-06. Written against the tree at HEAD. Every JLS claim carries a path and,
where the line matters, a line number. External claims are marked **[verified]**,
**[partly verified]** or **[unverified]**.*

---

### 0. The one-paragraph version

JLS has, already shipped and already tested, four of the five things a semantic
schematic merge needs: a plain-text save format with a normative specification
(`docs/file-format.md`), permanent per-element identity (`sid`, #165), a
byte-canonical serializer whose output is a pure function of circuit content
(#166), and a closed, validated, invertible, serializable edit algebra
(`src/jls/collab/op/`, #167). The fifth thing — a function that compares two
circuits and a function that merges three — does not exist anywhere in the tree.
`grep -rn "diff" --include=*.java src/` returns thirteen hits, every one of them
the English word "differ" inside a doc comment. There is no `jls.diff` package, no
merge driver, no `.gitattributes` entry beyond a CRLF guard, and no persisted op
log. The gap between what JLS has and what it needs is roughly 18–27
maintainer-weeks, and closing it produces a capability that no schematic tool in
any of the three surveyed classes ships in the box: a `git merge` driver that
cannot produce a file the tool refuses to load.

This capability appears in **none** of the 304 survey entries
(`grep -in "version control\|revision control\|design data management"
docs/standards-landscape.md` returns nothing). It is exactly the class of gap the
sweep frame said it could not see.

---

### What is missing today

#### 1. There is no differ, no merger, and no version-control integration. At all.

- No `jls.diff`, no `jls.merge`, no `jls.vcs` package: `ls src/jls/` shows
  `collab core edit elem hdl images module sim tutorial util`.
- `.gitattributes` (repo root) has exactly two stanzas, both CRLF guards:
  `*.jls -text` / `*.jls~ -text` and `test/resources/** -text`. No `diff=`, no
  `merge=`, no `filter=`.
- Nothing implements `OpSink` outside the editor. `grep -rn "OpSink"
  --include=*.java src/` finds the interface, the extension point
  (`src/jls/collab/op/OpExtensionPoints.java:25-27`, seam `collab.op-observer`,
  status "typed now"), and one anonymous implementation at
  `src/jls/edit/SimpleEditor.java:5407`. **The op-observer seam exists and has
  zero contributors**, so no op log is written next to a save, and there is
  nothing on disk from which a merge could reconstruct causal history.

#### 2. The plain-text path is aspirational, not exercised

Three places in the tree state the version-control intent, and none of them is
backed by tooling:

- `src/jls/FileAbstractor.java:46` — "wrapper, so version control gets meaningful
  diffs and readers…"
- `src/jls/edit/Editor.java:149` — the Save As file-type label:
  `"JLS Circuit Files (plain text: diffable, fork-readable)"`.
- `README.md:304` — "Plain-text saves diff cleanly in version control and open in
  JLS forks".

Against which:

- **The default container is XZ.** `docs/file-format.md:45-52`: a `.jls` is XZ,
  zip, or plain text, distinguished by sniffing; "JLS writes XZ by default and
  plain text on explicit request". A git repository of `.jls` files therefore
  stores opaque compressed blobs that re-encode wholesale on every save. Git
  cannot delta-compress them, GitHub renders "Binary file not shown", and repo
  size grows by the full file size per commit.
- **`-savetext` cannot write to a pipe.** `src/jls/JLSStart.java:1112-1128`
  requires the operand to end in `.jls` *and* pass `Util.isValidFileName` on the
  stem, so `-savetext -` and `-savetext /dev/stdout` are both usage errors. A git
  `textconv` or `clean` filter must write to stdout. There is no flag that does.
- **`-b -savetext out.jls` silently does nothing.** `JLSStart.start` is a mode
  chain — `if (JLSInfo.batch)` at `:168`, `else if (JLSInfo.imgexport)` at `:282`,
  `else if (JLSInfo.hdlexport)` at `:363`, `else if (JLSInfo.textsave)` at `:478`
  — so batch wins and the text save is dropped without a diagnostic. I ran it:
  exit status 0, no file written. That defect has survived because nothing
  downstream consumes the text path.

#### 3. The canonical save is content-deterministic but **not diff-stable** — measured

`Circuit.save` (`src/jls/Circuit.java:1486-1500`) sorts elements by stable id
(wires last) and then assigns the file-local `int id` **in that sorted order**.
`WireEnd.save` writes `ref attach <int id>`, `ref wire <int id>` and
`probe <int id> "..."` using those positional ids
(`src/jls/elem/WireEnd.java:606,611,613`). `docs/file-format.md:369-374` states the
rule: ids are "assigned at save time, dense from 0, scoped to their own `CIRCUIT`
block … not stable across saves."

Consequence: **inserting one element renumbers every element after it and every
reference to them.** `ElementId.compareTo`
(`src/jls/elem/ElementId.java:278-285`) orders by replica string then counter, and
a fresh replica id is 32 hex digits (`ElementId.java:44-52`), so roughly five
sixths of new installs sort *before* a file whose elements were minted under
`legacy` or a higher-sorting replica. New elements therefore land at the *front*.

Measured on the tree at HEAD, using the shipped jar:

```
$ jls -savetext base.jls  riscv/build/addi.jls      # 1038 elements, 10 751 lines
# hand-insert one NotGate carrying sid "0b1c:0"
$ jls -savetext base2c.jls base2.jls
$ diff -u base.jls base2c.jls | grep -c '^[+-]'
5314
$ diff -u base.jls base2c.jls | grep '^[+-]' | grep -c 'int id\|ref \|probe '
5227
```

**One added gate produces a 5 314-line textual diff, 5 227 lines of which are pure
renumbering churn.** The semantically meaningful change is nine lines. A
line-oriented three-way merge over that file is not merely unhelpful; it is
guaranteed to conflict on essentially every hunk, and any hunk it *did* auto-merge
would splice `ref` lines from two different numbering schemes and produce a file
that either fails to load or silently rewires the circuit.

A minimal reproduction on a three-element circuit shows the same shape:

```
-ELEMENT InputPin        +ELEMENT NotGate
- int id 0               + int id 0        ...
                          +ELEMENT InputPin
                          + int id 1
 ELEMENT AndGate
- int id 1               + int id 2
 ELEMENT OutputPin
- int id 2               + int id 3
```

#### 4. Legacy identity is positional, and every circuit in the tree is legacy

`Circuit.finishLoad` (`src/jls/Circuit.java:1321-1334`) mints
`legacy:0, legacy:1, …` **in file order** for elements without an `sid`, and
`docs/file-format.md:395-398` blesses it as deterministic. It is deterministic
*per file*, which is the wrong property here: two students who each edit the same
pre-#165 circuit produce files whose `legacy:7` elements are unrelated. Every
fixture in the tree is in this class — `jls -savetext` on `riscv/build/addi.jls`
emits `String sid "legacy:0"` … `"legacy:1037"`. A differ that matches on `sid`
alone would silently pair unrelated elements across two legacy edits. **This is
the single largest correctness hazard in the whole capability and it applies to
100% of existing content.**

#### 5. Structural facts that will constrain any differ

- **Subcircuits are inlined copies, not references.** `SubCircuit.save`
  (`src/jls/elem/SubCircuit.java:282-289`) writes the entire nested `CIRCUIT` block
  inline; `Circuit.load` constructs a fresh `Circuit` per instance
  (`src/jls/Circuit.java:1015`). Two instances of "the same" block are two
  independent element populations with two independent sid namespaces. A diff of
  a circuit using one block twice reports the same edit twice and cannot say they
  are the same edit. (P3's reuse identity fixes this; see §Relationship.)
- **Stable ids are unique per `CIRCUIT` block, not per file.**
  `Circuit.finishLoad:1310-1319` rejects a duplicate `sid` within a block, so
  nested blocks restart the namespace. A differ's element key is
  `(path-of-subcircuit-instances, sid)`.
- **The op vocabulary's change unit is the whole element block, not an
  attribute.** `SetElementConfig`'s own javadoc
  (`src/jls/collab/op/SetElementConfig.java:26-31`) says why: *"JLS elements have
  no uniform typed-attribute API: an element's whole reconfigured save block **is**
  the change unit."* Taken literally that would make two students editing two
  different attributes of the same gate a conflict. It is not literally true at
  the *format* level — the block is a typed `(kind, name, value)` item list
  (`docs/file-format.md:125-137`, seven item kinds) — and 19 of the element classes
  do declare `savedAttributes()` (`src/jls/elem/Element.java:200-319`,
  `src/jls/elem/Attribute.java`). The merge should work on the parsed item list.
  See the design decision below.
- **`SetElementConfig` refuses wired elements**
  (`SetElementConfig.java:174-189`) and `RemoveElements` refuses elements with
  wires attached (`docs/operation-layer.md:62-73`). A merge expressed purely as
  ops must therefore express "change a wired gate's delay" as
  `RemoveWire` + `SetElementConfig` + `AddWire`, which the vocabulary supports at
  net granularity but which is not free.

#### 6. What users do instead — the workaround evidence

- **The maintainer's own research doc already names the gap and defers it.**
  `docs/collaborative-editing-research.md:579-582`: *"Stage 3 — asynchronous
  collaboration. Persist the op log next to saves; 'merge changes from file'
  becomes anti-entropy against a file instead of a socket. **The separate semantic
  three-way merge tool for plain-text saves remains a cheaper alternative if Stage
  2 never ships.**"* That sentence is this document's thesis, written down and then
  put behind a networking program that has not landed.
- **`CausalBuffer` states the missing piece in its own class javadoc.**
  `src/jls/collab/crdt/CausalBuffer.java:21-24`: *"Concurrent envelopes carry no
  order between them … making concurrent deliveries converge across replicas is
  the job of the per-kind CRDT merge rules layered above this buffer, not of
  delivery."* **Those per-kind merge rules do not exist in the tree.** Delivery
  shipped; merge did not.
- **The peer-tool workaround is "don't".** NYU's Computer Systems Organization lab
  page tells students using Logisim-evolution that ".circ files use the XML format,
  which makes it hard for Git to automerge", and that they should "work on a single
  computer at a time" **[verified via search snippet;
  https://nyu-cso.github.io/labs/l5-logism.html]**. That is the state of the art
  in the peer educational class: serialize the humans.
- **The industry workaround is pessimistic locking.** Keysight's design-data-
  management material states plainly that "the inability to automate merging for
  schematics and layouts needs a centralized repository to prevent manual merging
  errors due to accidental overwrites" and that engineers "need a locking mechanism
  to ensure that fellow engineers are not making changes to the same file at the
  same time" **[verified via search snippet;
  keysight.com/blogs/en/tech/sim-des/optimizing-ic-design-data-management]**.
- **The commercial workaround is a whole company.** AllSpice.io exists because
  "the diff tool [was] ubiquitous in the software industry but nonexistent in
  hardware" (their words) **[verified via search snippet; allspice.io]**.

---

### The capability

Five parts. Each is independently shippable and each is stated with a
recommendation, not a menu.

#### C1 — Diff-stable serialization: FORMAT 3, `-canon`, and a git clean filter

**Recommendation: bump the save format to version 3, in which `ref`, `probe` and
`pair`-anchor items carry stable ids and the `int id` line disappears entirely.**

`docs/file-format.md:434-443` already classifies this correctly: "any change to
the block structure … or the meaning of an existing record" requires a bump. A
version-3 file has no positional identity anywhere; a version-3 reader keeps
accepting 0, 1 and 2 forever (`:444-446`). The concrete edits are
`WireEnd.save`/`setValue` (`src/jls/elem/WireEnd.java:588-616` and the `ref`
resolution in `Circuit.load`), `Circuit.save`'s id-assignment loop
(`src/jls/Circuit.java:1497-1503`), and the two op-layer transplant helpers that
already do local renumbering by hand and would *shrink*
(`src/jls/collab/op/NetBlocks.java` — `docs/operation-layer.md:82-86` describes
exactly the renumbering that FORMAT 3 deletes).

Acceptance criterion, stated as a number because the current number is measured:
**re-run the `addi.jls` insertion experiment; the diff must fall from 5 314 lines
to 9.**

Two small companions:

- **`-canon [file|-]`**: read any container, write canonical text to stdout.
  Fixes the `-savetext` stdout hole (`JLSStart.java:1112-1128`) and the
  `-b -savetext` silent no-op (`JLSStart.java:168-478`).
- **A git clean filter, not a smudge filter.** Because `docs/file-format.md:42-52`
  makes the reader sniff the container rather than trust the name, plain text
  *is* a valid `.jls`. So `clean = jls -canon -` stores canonical text in the git
  object database while the working tree keeps whatever the user saved, and the
  smudge side is the identity. Repos become diffable, delta-compressible and
  reviewable on GitHub with no change to how JLS opens files. This is the
  cheapest single item in the whole area.

#### C2 — `jls.diff`: the semantic differ

A new headless package `src/jls/diff/` (AWT-free, so it lands inside the
`HeadlessCoreRatchetTest` envelope) producing a typed `CircuitDelta`:

```
CircuitDelta
  ├─ ElementAdded(path, sid, tag, block)
  ├─ ElementRemoved(path, sid, tag, block)
  ├─ ElementMoved(path, sid, from, to)                 // x/y only
  ├─ AttributesChanged(path, sid, List<AttrChange>)    // per (kind,name)
  ├─ SubstructureChanged(path, sid, kind)              // ordered items, §C2.3
  ├─ NetAdded(path, netKey, blocks)
  ├─ NetRemoved(path, netKey, blocks)
  ├─ NetRerouted(path, netKey, segmentsBefore, segmentsAfter)
  └─ SubcircuitChanged(path, sid, CircuitDelta)        // recursive
```

**C2.1 Matching.** Primary key is `(subcircuit instance path, sid)` — direct,
O(n), no heuristics. That is the whole payoff of #165 and it is why JLS is
unusually close to this.

**C2.2 The legacy fallback, which is mandatory, not optional.** When either side
carries `legacy:` ids the primary key is unsound (§4 above). Recommendation:
detect it and switch to a **structural matcher** — greedy bipartite matching over
`(element tag, port arity, bit width, neighbourhood signature to depth 2,
position)` with a similarity threshold, reporting an *unmatched* bucket rather
than guessing. Then ship `jls -adopt file.jls`, a one-time rewrite that mints
fresh per-install ids for every `legacy:` element, and tell users in
`docs/version-control.md` to run it once before putting a circuit under version
control. Diff on legacy files is best-effort and says so; merge on legacy files is
**refused**. That refusal is the correct behaviour and must be a documented,
tested rejection, not a silent degradation.

**C2.3 Attribute granularity.** Diff the *parsed item list* of each element block,
not its bytes. `docs/file-format.md:125-137` gives the seven item kinds; `int`,
`long`, `Int`, `String` and `ref` items form a map keyed by attribute name and
merge as a map. The exceptions are named and finite:

| Element | Ordered/repeated payload | v1 treatment |
|---|---|---|
| `StateMachine` | item *sequence* is significant (`file-format.md:319`), canonicalized by #180 | whole-substructure conflict if both sides touched it |
| `TruthTable` | `pair` cells, row-major (`:324`) | row-keyed map merge (rows are addressable) |
| `Binder`/`Splitter` | `pair` (index, bundle bit) + `noncontig` flag (`:343-354`) | whole-element conflict |
| `Memory` | `String init` / `String initrle` — one opaque blob (`:307`) | decode RLE, merge as an address→value map, re-encode |
| `WireEnd` | `probe` items, `ref wire` list | handled at net granularity, C2.4 |

The `Memory` row is worth stating as a *feature*: two students editing different
regions of the same ROM image merge cleanly, which is precisely the case a text
merge over a run-length-encoded dump cannot do. The research doc's own table
already prescribes it (`collaborative-editing-research.md:243`: "index-addressed →
a map of address→LWW value, not a sequence").

**C2.4 Wires.** The unit is the **net**, matching `AddWire`/`RemoveWire`
(`docs/operation-layer.md:87-110`, "Net granularity is deliberate"). A net's key is
the sorted set of `(sid, put-name)` attachment anchors — which is exactly what
`NetBlocks` already serializes. Rerouting (same anchors, different segment
geometry) is classified separately from rewiring (different anchors), because the
first is cosmetic and the second is the design change.

**C2.5 Noise suppression, on by default with a flag to disable.** `width`/`height`
are "omitted when the type recomputes size on load" (`file-format.md:214`) and are
a function of font metrics; a diff must not report them. Neither should it report
`trpos` (trace-window position) unless asked. Recommend `--include=geometry,trace`
to opt back in.

#### C3 — Rendering the difference

Three surfaces, in build order:

1. **A text report** — the terminal and PR-body form:

   ```
   circuits/alu.jls
     + AndGate   "and3"   at (300,180)
     + net       and3.out -> mux1.in1              (3 segments)
     ~ Register  "acc"    delay 5 -> 12
     ~ Mux       "mux1"   moved (240,120) -> (240,156)
     - NotGate   "inv2"   at (300,240)  and the net that drove it
     ~ SubCircuit "alu32" -> 4 changes inside
     = 1029 elements unchanged
   ```

2. **SVG, side-by-side and overlaid.** This is nearly free and it is the single
   most under-appreciated asset in the tree: `CircuitRenderer.exportImage`
   (`src/jls/edit/CircuitRenderer.java:301-360`) already renders any circuit to
   **byte-deterministic SVG** through JFreeSVG, with a fixed defs prefix and an
   explicit deterministic draw order chosen *because* an unstable order would break
   byte-identical goldens (`:329-345`), pinned by
   `SvgExportTest.exportingTwiceIsByteIdentical`. Per-element colour is a
   `Graphics2D` wrapper around `ElementRenderers.draw(svg, el)` that forces the
   pen. Overlay semantics: unchanged grey, added green, removed red ghost, moved
   with a displacement arrow, attribute-changed amber outline with the changed
   attribute drawn as a label. **Recommendation: overlay is the default and
   side-by-side is the flag**, because overlay answers "what changed" in one image
   and side-by-side answers "what does each look like", and reviewers ask the first
   question.

3. **A GUI diff view** — two `CircuitRenderer`s with linked pan/zoom and a change
   list that scrolls both. Build last; see the risk section.

#### C4 — Three-way merge, and whether the CRDT work already gives it

**The direct answer: no, `src/jls/collab/crdt/` does not give merge semantics, and
being precise about why is what produces the useful insight.**

- `VectorClock` (`src/jls/collab/crdt/VectorClock.java`) answers
  BEFORE/AFTER/CONCURRENT over *peer observation states*. Two git branch tips
  carry no such state: nothing in `Circuit`, `Element` or the save format stores a
  counter, and no op log is persisted (§1 above). A vector clock could only be
  reconstructed from a log that does not exist.
- `CausalBuffer` is a **delivery** discipline — exactly-once, dependencies-first.
  Git already delivers exactly once, and there is nothing to buffer. Its own
  javadoc disclaims the merge (`:21-24`).
- `OpEnvelope` is transport metadata: origin, sequence, clock. Offline there is no
  origin and no sequence.

**What genuinely transfers — and this is the architectural insight, stated as
one:**

> **The per-kind merge rule table is the same object for the online collaborative
> merge and the offline git merge. Neither exists yet. Build it once, in a
> state-based form, offline first — and the offline three-way merge tool becomes
> the executable specification and the test oracle of the online CRDT.**

The chain of reasoning, each link anchored:

1. Both directions merge the *same* data model with the *same* rules: the research
   doc's §3 table (`collaborative-editing-research.md:237-244`) — add-wins
   observed-remove set of elements keyed by stable id; per-attribute registers;
   OR-set of wire objects whose endpoints are `(stable element id, put name)`;
   sequence CRDTs only for the genuinely ordered substructures. That table
   describes both a network merge and a file merge without a word of change.
2. Both directions produce the *same* output type: a `List<CircuitOp>` submitted
   through `OpSink.submitAll`. The offline merger therefore inherits, free, every
   invariant the editor enforces — atomic validate-then-mutate
   (`CircuitOp.java:39-51`), name-collision rejection
   (`SetElementConfig.java:203-217`, `AddElements`' paste rules,
   `docs/operation-layer.md:58-64`), jump-start/jump-end cascade rules, tri-state
   net re-arming (`docs/operation-layer.md:89-96`). **A merge expressed as ops
   cannot produce a file JLS refuses to load.** No text merge can make that
   promise; that promise is the product.
3. Both directions share the *same* oracle: canonical save (#166) and
   `DeterministicSaveTest`. `collaborative-editing-research.md:177-182` already
   names byte-equality of canonical saves as "the convergence oracle for all
   replication testing".
4. The offline direction is the *harder and more informative* one, so it should be
   built first. Online merge must be **total** — you cannot pause a peer's typing
   to ask a question — so it resolves every concurrency by a deterministic
   tiebreak. Offline merge may be **partial**, because git has a first-class
   notion of conflict. A partial merge that reports a conflict strictly dominates
   a total one that silently picks a winner. **Recommendation: implement the
   STRICT (partial) rule table, and derive the AUTO (total) online policy from it
   by appending one deterministic tiebreak — `(Lamport counter, peer id)` — to
   each conflict class.** The online-specific code is then that tiebreak and
   nothing else.
5. The differ is an **anti-entropy primitive the online design already needs**.
   `collaborative-editing-research.md:78-79` calls for "snapshot catch-up past the
   log-compaction horizon", which today means broadcasting an entire circuit. With
   C2, that becomes a delta. A peer whose laptop slept for an hour is caught up by
   a diff, not a full snapshot.
6. The cross-check that makes (4) real, and it is a property test with no socket in
   it: for any two op sequences `A` and `B` from a common base,
   `merge3(base, apply(base,A), apply(base,B))` must canonical-save
   byte-identically to the state two replicas reach after exchanging `A` and `B`
   through `CausalBuffer`. If that property holds, **the offline tool tests the
   online tool**, and Stage 2 of the collaboration program gets an oracle it does
   not currently have.

**The conflict taxonomy, concretely.** STRICT policy in the left column, the AUTO
tiebreak that derives the online behaviour in the right:

| Situation | STRICT (git) | AUTO (online) |
|---|---|---|
| both sides add different elements | merge both | merge both |
| both sides add elements with the *same* sid | refuse the whole merge (only reachable on legacy ids) | impossible: fresh ids carry distinct replicas |
| one side edits attribute *a*, other edits attribute *b*, same element | merge both | merge both |
| both edit attribute *a* to different values | **conflict** | LWW by (counter, peer) |
| both change only `x`/`y` | resolve to *ours*, note it | LWW |
| one deletes an element the other edited | **conflict** | add-wins: keep the element |
| one deletes an element the other wired to | **conflict** | add-wins on the element; the net survives |
| both reroute the same net | **conflict** at net granularity | LWW on the whole net |
| each side independently names a pin `out` | *not a conflict in the delta* — two distinct sids — but the merged circuit is invalid | same; caught identically |
| ordered substructure touched by both | **conflict** on the whole substructure | LWW on the whole substructure |

The penultimate row is the one that justifies the whole design: a textual merge
merges it cleanly and produces a circuit that will not load. A semantic merge
catches it only because the merged result is pushed through the same validation an
editor gesture goes through. **State the acceptance criterion that way: the merge
driver's output either loads and elaborates (`Circuit.finishLoad`,
`WireNet.makeNet`) or is reported as a conflict. There is no third outcome.**

#### C5 — Git integration

**`.gitattributes`** (shipped in the repo *and* emitted by a
`jls --install-git-config` helper, because hand-editing `.git/config` is where
adoption dies):

```gitattributes
*.jls  -text  filter=jlscanon  diff=jls  merge=jls
```

**`.git/config`:**

```ini
[filter "jlscanon"]
    clean    = jls -canon -
    smudge   = cat
    required = true
[diff "jls"]
    command = jls -diff --git
[merge "jls"]
    name      = JLS semantic three-way merge
    driver    = jls -merge3 --base %O --ours %A --theirs %B --out %A --path %P
    recursive = jls
```

`recursive = jls` matters and is easy to get wrong: git's recursive strategy
merges the *virtual ancestors* of criss-cross histories with a driver, and the
default when the key is absent is `binary` — which would degrade every non-trivial
history back to "conflict". Point the recursion at the same semantic driver.

**Exit statuses — a real contract question, not a detail.**
`docs/batch-interface.md:36-41` defines exactly three: 0 completed, 1 runtime
failure, 2 usage error, and §6 (`:324-336`) makes them a stability promise. A
differ needs `diff(1)`'s "differences found", which must not collide with "runtime
failure". **Recommendation: `-diff` returns 0 identical, **3** differences found,
1 failure, 2 usage; `-merge3` returns 0 clean merge, **4** conflicts, 1 failure,
2 usage.** New statuses are an *addition* to the §1 table and are minor-version
material under §6's own rule, but they must be written into
`docs/batch-interface.md` in the same commit — that document is normative and the
CI test `CliFlagTableTest` pins the table.

**Conflict artifacts.** `.jls` has no conflict-marker syntax and must not grow
one — a half-merged circuit that loads and simulates wrongly is worse than one
that does not exist. **Recommendation: on conflict the driver leaves `%A`
untouched (git's normal conflicted state) and writes three siblings:**

- `alu.MERGE.jls` — the auto-resolvable part merged, conflicts resolved to *ours*,
  and **one `Text` annotation element placed at each conflict site** naming the
  alternative. `Text` is an existing frozen tag (`file-format.md:326`), so this
  needs no format change and the file opens in stock JLS.
- `alu.MERGE.txt` — the machine-readable conflict list, one line per conflict,
  keyed by `(path, sid, attribute)`.
- `alu.MERGE.svg` — the overlay render with conflicts in red.

The student opens `alu.MERGE.jls`, sees the conflicts annotated on the drawing,
fixes them, saves over `alu.jls`, `git add`. That workflow is strictly better than
reading `<<<<<<<` markers, and it is only available to a tool that owns both the
format and the renderer.

**What a reviewer sees in a pull request.** Three layers, all falling out of the
above:

1. Because of the clean filter and FORMAT 3, GitHub's own textual diff is real and
   is O(change) — the `addi.jls` case goes from 5 314 changed lines to 9.
2. `git diff` in a terminal runs the `diff=jls` driver and prints the C3 text
   report, so the reviewer reads *"+ AndGate and3; ~ Register acc delay 5 → 12"*
   rather than attribute lines.
3. A CI job (a ~30-line GitHub Action, no new JLS code) runs
   `jls -diff --format=svg --mode=overlay $BASE $HEAD -o diff.svg` per changed
   `.jls` and posts the SVGs plus the text report as a review comment. This is the
   thing that does not exist anywhere in the open-source world and the thing a
   maintainer can demo in ten seconds.

---

### What it unlocks

**Survey entries.** Honestly: **none.** The 304-entry survey contains no
version-control, diff, merge or design-data-management row (verified by grep over
`docs/standards-landscape.md`). The nearest neighbour is **#4 IP-XACT**, whose
VLNV vendor/library/name/version tuple is a *reuse-identity and versioning* concept
— and that belongs to P3, not here. The absence is the point: this is the
capability the standards-ranked sweep was structurally unable to see.

**Engineering capabilities.**

- **Circuit equality as a first-class, testable predicate.** JLS currently has no
  way to ask "are these two circuits the same design?" except byte-comparing
  canonical saves, which is far too strict (it fails on a moved element). C2 gives
  a graded answer: identical / identical-modulo-geometry / identical-modulo-ids /
  different-here.
- **P3's round-trip CI property becomes checkable.** The roadmap's headline
  interchange claim is *"`export → yosys → import → save` equal to the original
  **modulo element ids**"* (`docs/capability-roadmap/README.md:335-338`). "Modulo
  element ids" is exactly a structural comparison, and **nothing in the tree can
  perform it today**. C2's structural matcher (built anyway for the legacy
  fallback) is precisely that comparator. P3's most differentiating claim
  currently has no oracle; this supplies it.
- **Regression triage on the golden suites.** When `RiscvCpuGoldenTest` or a
  fixture changes, the maintainer currently reads a 10 751-line file. C2 turns
  "what did the last commit do to `addi.jls`" into six lines.
- **Repo health.** The clean filter alone stops `.jls` files from being
  incompressible binary blobs in every downstream course repository.
- **Anti-entropy for #163 Stage 2**, as argued in C4.5, which is currently
  specified as a whole-circuit snapshot broadcast.

**Teaching capabilities — what a student can do afterwards that they cannot do
today.**

- **Two students can work on one circuit on two laptops.** Today the only correct
  protocol is the Logisim protocol: one person edits, push before switching. After
  C4/C5 a lab pair branches, each builds a half — one the datapath, one the control
  — and `git merge` combines them, or names the exact element where they disagree.
  That is the first time a hardware course can teach the software course's
  collaboration workflow on hardware artifacts.
- **A student can see their own history.** `git log -p alu.jls` becomes readable:
  "Tuesday I added the carry chain; Wednesday I changed the register delay;
  Thursday I deleted the inverter that broke it." Undo is transient; version
  control is a narrative, and students currently have no way to get one for a
  drawing.
- **An instructor can diff two submissions from the same student.** "What changed
  between your draft and your final?" is currently unanswerable except by opening
  both and squinting.
- **An instructor can diff a submission against the provided skeleton.** And,
  crucially, JLS *already has the instructor-lock concept*: `fixed` in the base
  attributes (`docs/file-format.md:214`, `src/jls/elem/Element.java:258-271`,
  "element is not editable"). `jls -diff skeleton.jls submission.jls
  --assert-fixed-unchanged` exits non-zero if any `fixed` element differs. That is
  a ~2-day feature on top of C2 and it closes a real autograding hole: today a
  student can rewire the parts of a template they were told not to touch and the
  grader sees only the output.
- **Code review as a taught practice, on schematics.** "Open a PR, have a
  classmate review the change, address the comments" is an ordinary software-course
  exercise and is currently impossible for a drawing. With the overlay SVG in the
  PR it becomes an ordinary exercise on hardware.
- **Merge conflicts become a teachable phenomenon rather than a punishment.** "You
  both changed the mux selector width. The tool cannot know which you meant.
  Decide, together." That is a good five minutes of a lab, and it is a lesson about
  *shared mutable design state* that transfers directly to their software courses.

**Similarity and plagiarism detection — handled last, and deliberately hedged.**

C2's structural matcher makes similarity computation trivial: two circuits'
matched-fraction under the legacy structural matcher is a similarity score, and it
is robust to renaming, repositioning and re-saving. Separately and more sharply,
`sid` replica ids are a **provenance trail**: a copied file carries the *original
author's install replica id* in every element's `sid`, and
`docs/file-format.md:384-393` documents that the replica is per-install and
persisted in `~/.config/jls/replica-id`.

That second property is tempting and should be treated with real care:

1. **It is not a watermark and must never be sold as one.** It is a documented
   part of a public normative file format (`docs/file-format.md:379-402`), not a
   covert identifier — but students do not read the file format specification. If
   this capability ships, the identifier's existence and its provenance
   consequences belong in user-facing documentation and, arguably, in a first-run
   notice. Anything else converts a legitimate engineering identifier into a
   hidden tracker.
2. **It is disqualified as evidence in the most common institutional setting.**
   The replica id is *per install*, not per user. **Every student on a shared lab
   machine or a common VM image has the same replica id.** A "matching replica id"
   in that environment is not weak evidence of copying; it is no evidence at all,
   and it will systematically implicate exactly the students who cannot afford
   their own laptop. This is not a caveat to bury in a footnote — it is the reason
   the feature must not exist as a score.
3. **False negatives are trivial.** Redraw from scratch, or paste (paste mints
   fresh ids, `file-format.md:383`), and the trail vanishes. A detector that is
   defeated by the copier who knows about it, and that fires on the honest student
   in the shared lab, has the worst possible error profile.
4. **Legitimate identity is common.** A provided skeleton, a worked example from
   the lecture, and a canonical textbook circuit all produce high structural
   similarity between honest submissions.

**Recommendation: ship the pairwise structural *comparison* — an instructor can
diff any two submissions and look at the result — and do not ship a similarity
score, a cohort-wide ranking, or any automated flagging.** The difference is not
cosmetic: a comparison is a tool a human uses after forming a suspicion; a score is
a machine forming the suspicion. Document the replica-id provenance property
prominently so nobody mistakes it for forensic evidence, and state in
`docs/version-control.md` that JLS does not and will not compute a plagiarism
verdict.

---

### Competitive position

**Commercial.**

| Tool | Schematic diff | Schematic merge | Notes |
|---|---|---|---|
| **Altium Designer / 365** | **Yes** — graphical compare with a Differences panel, side-by-side in the editor; Altium 365 "Schematic Compare" **[verified via vendor docs search snippets; the pages themselves 403 to automated fetch]** | **Partly** — a "Collaborate, Compare and Merge" panel; a 3-way comparison is described in the context of **PCB CoDesign**, i.e. the board editor **[partly verified; I could not confirm whether 3-way merge covers schematic documents]** | The strongest incumbent by a distance. Bound to Altium's own VCS integration and workspace, not to a git merge driver. |
| **Cadence Virtuoso** | **Yes, via a third party** — Visual Design Diff with a Differences Browser, delivered by **Keysight SOS Core**'s integration, not by base Virtuoso **[verified via Keysight blog search snippet]** | **No** — the same vendor's material argues merging schematics cannot be automated and prescribes a locking DM system instead **[verified]** | The diff is a selling point of a *design-data-management* product layered on the EDA tool. |
| **AMD Vivado** | **Yes, report only** — Reports → Compare Block Designs produces a text or HTML diff report of two `.bd` files **[verified via AMD UG994 search snippet]** | **No** — the documented practice is to store `write_bd_tcl` output and *regenerate* rather than merge **[verified]** | The vendor's own answer is "don't version the graphical artifact." |
| **Siemens Questa/Verdi, Synopsys, Verilator, Icarus** | n/a | n/a | Not schematic editors. Their designs are HDL text and merge as text — which is the real incumbent answer, see below. |

**Open source.**

- **KiCad.** Structurally the best-positioned open tool and still does not have
  this. `.kicad_sch` is s-expression text and **every symbol instance carries a
  UUID** **[verified via KiCad developer file-format docs search snippet]** — so
  KiCad has JLS's `sid` equivalent. But there is no built-in schematic revision
  diff (KiCad 9's "Compare Symbol with Library" compares a symbol to its library,
  not two revisions) **[verified via search]**, and no merge driver. The ecosystem
  answer is a cluster of third-party tools — KiRI, KiCad-Diff, plotkicadsch,
  CADLAB.io — which **render both revisions and compare the pictures**
  **[verified via search]**. Even the best-placed open schematic tool does *image*
  diff, not *semantic* diff, and nobody does merge.
- **Yosys, GTKWave, Surfer.** Not schematic editors; nothing to merge.

**Peer educational — and this is where the gap is widest.**

- **hneemann's Digital.** `.dig` is XML, and I fetched one
  (`src/main/dig/sequential/JK-MS.dig`): **elements carry no identifier at all**,
  identity is `<pos x= y=>`, and wires are `<wire><p1 x= y=/><p2 x= y=/></wire>`
  with connectivity implied by coordinate coincidence **[verified by direct
  fetch]**. Semantic merge is not merely unimplemented there; it is structurally
  out of reach, because moving a component changes what it is connected to, so no
  identity-keyed merge is definable without first inferring identity from geometry.
- **Logisim-evolution.** `.circ` is XML; the documented course-level advice is
  "hard for Git to automerge … work on a single computer at a time" **[verified via
  the NYU CSO lab page snippet]**. Whether `.circ` carries per-component ids I did
  not verify **[unverified]**.
- **DigitalJS.** The schematic is *generated* from a Yosys netlist, so the source
  of truth is HDL text and merges as text. That is a legitimate way to dodge the
  problem, and worth saying so — but it also means DigitalJS is not a schematic
  *editor* in the sense JLS is.
- **AllSpice.io** deserves naming even though it is not an educational tool: a
  venture-funded company whose first product was a hardware diff tool, on the
  explicit premise that diff was "ubiquitous in software and nonexistent in
  hardware" **[verified via their own material]**. Third-party confirmation that
  the gap is real, valuable, and unclaimed.

**The honest counter-argument, stated before someone else states it.** The
mainstream escape from schematic merge is **to stop drawing schematics**. HDL is
text, text merges, and the industry's answer to "our schematics won't merge" has
largely been "use RTL." That is a genuine and successful answer, and it is why the
pain persists mostly in analog/custom IC and PCB, where schematics remain the
source of truth. JLS cannot take that escape: it is schematic-first by identity and
by pedagogical purpose. **That constraint is exactly why solving it here is
differentiating rather than redundant** — JLS is in the one design point where the
problem is unavoidable and the tool is small enough to actually fix it.

**Verdict: LEAPFROG, and probably the strongest single one available — but the
claim has to be precise.**

JLS's differentiator is *not* "has a schematic diff." Altium has a better one and
will keep having a better one; JLS should not claim otherwise. The four things
JLS's version would have that no surveyed tool has, in any class:

1. **It is a `git merge` driver.** Every incumbent's diff/merge is captive to that
   vendor's own design-data-management layer (Altium 365, Keysight SOS, Vivado's
   project system). None of them plugs into the version control the rest of the
   world already uses. A `.gitattributes` line is the entire integration surface.
2. **The merge output is validated by the editor's own operation vocabulary**, so
   a merged file cannot be a file the tool refuses to load. Nobody claims that,
   because nobody else has a closed, validated, invertible edit algebra sitting in
   their tree from a *different* project (collaborative editing) that happens to be
   exactly the right abstraction.
3. **One rule table serves both the online collaborative merge and the offline git
   merge**, with the offline one acting as the online one's oracle. That is an
   architectural position, not a feature.
4. **It is free, offline, single-jar and gradeable from a shell script**, which is
   what makes it usable by a course rather than by a company.

**Where JLS cannot plausibly lead**, said plainly: the interactive quality of a
side-by-side graphical compare UI in a mature commercial editor, and anything that
requires understanding a proprietary binary format. JLS leads on *composition with
git* and on *correctness of the merged artifact*, not on UI polish.

---

### Relationship to the existing programs

**This is a new program — call it P7 — and it is the least entangled one in the
roadmap.**

- **It depends on none of P1–P6.** It touches no `react` implementation, no value
  domain, no timing model, no element vocabulary, no HDL path. It shares no code
  with the keystone. Everything it needs already shipped, under a *different*
  program: #165 stable ids, #166 canonical save, #167 the op layer, #163's
  vocabulary and layering rules.
- **It extends the collaboration program (#163) rather than P1–P6**, and it should
  be understood as **pulling #163's Stage 3 forward in front of Stage 2**. The
  research doc itself sanctions this (`collaborative-editing-research.md:579-582`:
  the offline merge tool "remains a cheaper alternative if Stage 2 never ships").
  The argument to make explicitly to the maintainer is stronger than "cheaper
  alternative": the offline merger is *worth building even if Stage 2 does ship*,
  because it is Stage 2's specification, its oracle, and its anti-entropy
  primitive.
- **Two things it gives P3, which is the only genuine coupling.**
  1. **P3's round-trip CI property has no comparator today** and this builds it
     (see §What it unlocks). That is a real dependency running *from* P3 *to* P7.
  2. **P3's reuse identity changes what "the same subcircuit" means.** Today
     subcircuits are inlined copies (`SubCircuit.java:282-289`); after P3's
     component table they are references. Ordering constraint: **P7's differ must
     be written against a subcircuit-identity interface** (a `SubcircuitKey`
     abstraction) so P3 can substitute component identity for instance-path
     identity without a rewrite. That is a design constraint on P7, not a
     scheduling dependency — P7 need not wait.
- **P5 borrows from it.** P5's autograding story ("did the submission change what
  it was told not to change") is C2 plus the `fixed` attribute, and P5's report
  channel and exit-status design (`README.md:537-541`, "This must be designed
  first, because it is a change to a promise") is the *same* conversation as the
  `-diff`/`-merge3` exit statuses. **Recommendation: design the exit-status
  extension once, jointly with P5's report channel**, rather than twice.
- **Ordering within P7:** C1 → C2 → (C3 ∥ C4) → C5. C1 first because the measured
  5 314-line churn makes every later demo unconvincing until it is fixed, and
  because the clean filter is a one-week win that can ship alone. C4 must not
  start before C2, because the merge consumes the delta.
- **Extension points.** C2's differ and C4's merger want one new catalogued seam
  — call it `vcs.merge-policy`, the rule table — and one consumer of the existing
  `collab.op-observer` seam (`OpExtensionPoints.java:25-27`). Per
  `docs/extension-points.md`'s own rule that pending seams get a row before any
  code, add the row first; `ExtensionPointCatalogTest` cross-checks both
  directions and will fail the build otherwise.

---

### Size and risk

**18–27 maintainer-weeks (4–6.5 maintainer-months).** Reasoning by stage, estimated
by analogy to the shipped work the repo records (#166's canonical save, #167's op
layer with its transplant helpers, #154's SVG export):

| Stage | Content | Weeks |
|---|---|---|
| **C1a** | `-canon` to stdout; fix the `-b -savetext` no-op; git clean filter; `docs/version-control.md` | 1–1.5 |
| **C1b** | **FORMAT 3**: refs by stable id, `int id` dropped. `WireEnd` save/load, `Circuit.load` ref resolution, `NetBlocks` renumbering *deleted*, `FileFormatSpecTest` + all goldens re-baselined, reader keeps v0–v2 forever | 2–3 |
| **C2** | `jls.diff`: delta model, sid matcher, attribute-item diff, net-granularity wire diff, subcircuit recursion, legacy structural matcher, `jls -adopt` | 4–6 |
| **C3a** | Text report + SVG overlay/side-by-side over `CircuitRenderer` | 1.5–2.5 |
| **C3b** | GUI diff view (linked pan/zoom, change list) | 1.5–2.5 |
| **C4** | Three-way merge: STRICT rule table, conflict model, ops synthesis, validation through `OpSink`, convergence property tests incl. the CausalBuffer cross-check | 5–8 |
| **C5** | Merge driver, diff driver, conflict artifacts, `--install-git-config`, exit-status contract in `docs/batch-interface.md`, PR-render CI action | 2–3 |
| **C6** | Teaching surface: `--assert-fixed-unchanged`, instructor diff modes, structural comparison, the ethics documentation | 2–3 |

Two of those are lower-confidence than the rest: **C2's legacy structural matcher**
(a similarity heuristic with a threshold, which is the only place in the whole
program where the answer is not exact) and **C4's convergence property tests**
(random concurrent schedules; the research doc anticipates this burden and says to
"revisit if the bespoke core's testing burden surprises",
`collaborative-editing-research.md:576-577`).

**The three ways it goes wrong.**

1. **Legacy identity, and it is not hypothetical.** Every `.jls` in the tree and
   in the world today has `legacy:N` ids minted positionally at load
   (`Circuit.java:1321-1334`). A differ that trusts `sid` will confidently match
   two unrelated gates across two edits of the same legacy file, and a merger built
   on it will silently corrupt circuits. The mitigation must be structural and
   loud: **refuse to merge any file whose ids are `legacy:`**, ship `jls -adopt`,
   and make the refusal a documented, tested rejection. If this is treated as an
   edge case rather than as the default case, the feature ships broken.
2. **A merge that loads but is wrong.** Add-wins on elements plus independent net
   merging can produce a net with two drivers neither student drew, or an element
   whose ports are wired by one side and reconfigured by the other. The mitigation
   is the C4 acceptance rule — merged output must load *and* elaborate
   (`finishLoad`, `WireNet.makeNet`) or be reported as a conflict — plus, once P5
   exists, an ERC pass over the merged result. Without that rule the feature is
   actively dangerous, because a silently-wrong circuit is worse than a conflict.
3. **The GUI eats the budget.** C3b and a hypothetical interactive merge editor are
   where 2 weeks becomes 8, and they are the *least* necessary parts: the actual
   workflows (PR review, instructor grading, `git merge` on a lab pair's branch)
   are served entirely by headless artifacts. **Mitigation: ship headless first,
   build C3b last, and be willing never to build an interactive merge editor at
   all.**

A fourth, smaller: **FORMAT 3 is a version bump on a normative format with a
compatibility promise** (`docs/file-format.md:420-446`, `docs/batch-interface.md`
§6). It is the correct kind of bump — old readers refuse rather than misread, which
is the entire point of the header — but it is a promise, and it must go through the
document-first process the repo already mandates.

**What would make it not worth doing.** One thing, and it should be checked before
committing the weeks: **if `.jls` files are in practice never shared under version
control** — if every assignment is one file, one student, submitted as an
attachment to an LMS — then C4 and C5 have no users and only the diff half
survives. Note carefully that the diff half survives *fully*: instructor review of
two submissions, diff against a skeleton, the `fixed`-element assertion, regression
triage, and P3's round-trip comparator all stand alone and cost **C1a+C2+C3a+C6 ≈
9–13 weeks**. So the decision decomposes cleanly:

- **Diff (9–13 weeks) is worth doing regardless**, because its grading and CI uses
  need no collaborative workflow to exist.
- **Merge (+9–14 weeks) is worth doing if — and it is the honest question —
  the maintainer intends pairs, repos, or the collaborative-editing program.** If
  #163 is going to happen at all, the merge half is not optional; it is the part of
  #163 that ships without a socket.

---

### Sources

**Repository (all paths relative to `/home/user/JLS`, HEAD).**

- `docs/capability-roadmap/README.md` — the six programs; :335-338 P3's round-trip
  claim "modulo element ids"; :537-541 P5's report-channel-first rule; :944-970 the
  extension-point discipline.
- `docs/file-format.md` — :42-52 containers and sniffing; :61 `-savetext` as the
  interchange form; :125-137 item grammar; :208-227 base attributes and the
  unknown-attribute valve; :214 `fixed`, `width`/`height` recomputation; :291-327
  the frozen tag table incl. `Text`, `StateMachine`, `Memory` `init`/`initrle`;
  :343-354 `noncontig`; :364-416 §8 ids, stable ids, canonical order; :420-446 §9
  evolution policy.
- `docs/batch-interface.md` — :17-49 §1 invocation, streams, three exit statuses;
  :324-336 §6 stability promise.
- `docs/operation-layer.md` — :16-30 the op contract; :48-73 `ElementBlocks` and
  the add/remove inverse rules; :76-110 `NetBlocks`, net granularity, local
  renumbering; :146-151 the headless layering rule.
- `docs/collab-vocabulary.md` — :16-49 the closed payload vocabulary and
  `ElementVocabulary`.
- `docs/collaborative-editing-research.md` — :121-203 §2 determinism audit and what
  #165/#166 fixed; :237-244 §3 the per-shape CRDT table; :534-582 §7 the staged
  path, incl. :579-582 Stage 3 naming this tool.
- `docs/extension-points.md` — :28-36 the seam catalog; `collab.op-observer` row.
- `docs/standards-landscape.md` — grepped for version-control/diff terms; **no
  matching entry** among the 304.
- `.gitattributes` — the two CRLF stanzas, no diff/merge/filter attribute.
- `src/jls/Circuit.java` — :465-485 `getElementsInStableOrder`; :1015 fresh
  `Circuit` per subcircuit instance; :1300-1334 `finishLoad`, sid uniqueness check
  and positional `legacy:` minting; :1478-1512 canonical save order and the
  positional `int id` assignment.
- `src/jls/elem/WireEnd.java` — :588-616 `save`, `ref attach` / `ref wire` /
  `probe` all by positional id.
- `src/jls/elem/SubCircuit.java` — :282-289 inline nested-circuit save.
- `src/jls/elem/ElementId.java` — :36-56 replica resolution and the persisted
  per-install id; :278-285 `compareTo` (replica, then counter).
- `src/jls/elem/Element.java` — :200-319 `BASE_ATTRIBUTES` and the declarative
  `Attribute` mechanism; :258-271 the `fixed`/uneditable attribute.
- `src/jls/elem/Attribute.java` — the four attribute kinds.
- `src/jls/collab/op/CircuitOp.java` — :34-37 the sealed 11-kind vocabulary;
  :39-64 the apply/invert contract.
- `src/jls/collab/op/OpSink.java` — :24-47 `submit` / `submitAll`.
- `src/jls/collab/op/OpExtensionPoints.java` — :17-27 the `collab.op-observer`
  seam, no contributors.
- `src/jls/collab/op/SetElementConfig.java` — :26-31 "no uniform typed-attribute
  API"; :174-189 the wired-element rejection; :203-217 name-collision validation.
- `src/jls/collab/op/ElementBlocks.java` — :48-64 canonical block serialization;
  :87-140 strict load.
- `src/jls/collab/crdt/VectorClock.java` — :26-52 the causal order;
  :159-206 merge and compare.
- `src/jls/collab/crdt/CausalBuffer.java` — :21-24 **the javadoc stating that the
  per-kind merge rules are not this class's job and do not exist**; :68-122
  delivery.
- `src/jls/collab/crdt/OpEnvelope.java` — :21-46 the envelope grammar.
- `src/jls/edit/CircuitRenderer.java` — :86-153 the layered draw path with a
  "second" (highlight) set; :301-360 the deterministic JFreeSVG export.
- `src/jls/JLSStart.java` — :168/:282/:363/:478 the mode chain that makes
  `-b -savetext` a silent no-op; :478-521 the text-save path; :1112-1128 the
  `-savetext` operand validation that forbids stdout.
- `src/jls/FileAbstractor.java:46`, `src/jls/edit/Editor.java:149`,
  `README.md:304` — the three unbacked version-control claims.
- `test/jls/DeterministicSaveTest.java`, `test/jls/SvgExportTest.java`,
  `test/jls/FileFormatSpecTest.java`, `test/jls/CliFlagTableTest.java` — the
  contracts a FORMAT 3 bump and new flags must move through.

**Measurements performed for this document** (shipped jar
`target/jls-5.0.5-SNAPSHOT.jar`, scratchpad
`/tmp/claude-0/-home-user-JLS/c7a97eb3-cab3-5b44-a3e3-b63071913715/scratchpad/`):

- `jls -savetext base.jls riscv/build/addi.jls` → 10 751 lines, 1 038 `ELEMENT`
  blocks, every `sid` of the form `legacy:N`.
- Inserting one `NotGate` with `sid "0b1c:0"` and re-canonicalizing → `diff -u`
  reports **5 314** changed lines, **5 227** of which are `int id` / `ref` /
  `probe` renumbering.
- `jls -b -savetext out.jls addi.jls` → exit 0, **no file written**.
- `jls -savetext -` → usage error.

**External claims.**

- KiCad has no built-in schematic revision diff; third-party tools (KiRI,
  KiCad-Diff, plotkicadsch, CADLAB.io) render revisions and compare images —
  **[verified via search results, docs.kicad.org 9.0 documentation and KiCad forum
  threads; forum pages 403 to automated fetch]**.
- `.kicad_sch` symbol instances carry UUIDs — **[verified via
  dev-docs.kicad.org file-format documentation search snippet]**.
- Vivado's Compare Block Designs produces a text/HTML report; the documented
  practice for version control is `write_bd_tcl` regeneration — **[verified via AMD
  UG994 search snippets]**. I found no evidence of a Vivado block-design *merge*
  capability — **[unverified negative]**.
- Altium has graphical schematic compare and a "Collaborate, Compare and Merge"
  panel; a 3-way comparison is described in PCB CoDesign — **[partly verified;
  altium.com pages return 403 to automated fetch, so whether 3-way *merge* extends
  to schematic documents is **unverified**]**.
- Cadence Virtuoso's Visual Design Diff is delivered through Keysight SOS Core
  integration, with a Differences Browser — **[verified via Keysight blog search
  snippet]**. Whether base Virtuoso ships an equivalent without a third-party DM
  layer is **unverified**.
- Keysight's material states the inability to automate schematic/layout merging and
  prescribes locking — **[verified via search snippet; the page 403s to automated
  fetch]**.
- hneemann's Digital `.dig`: no element identifiers, coordinate-implied
  connectivity — **[verified by direct fetch of
  `raw.githubusercontent.com/hneemann/Digital/master/src/main/dig/sequential/JK-MS.dig`]**.
- Logisim-evolution `.circ` is XML and "hard for Git to automerge"; students are
  advised to work on one computer at a time — **[verified via the NYU CSO lab page
  search snippet]**. Whether `.circ` carries per-component identifiers —
  **[unverified]**.
- AllSpice.io's first product was a hardware diff tool, framed as filling a gap
  that was "ubiquitous in the software industry but nonexistent in hardware" —
  **[verified via their own published material, search snippet]**.
- Git merge-driver mechanics (`%O %A %B`, the `recursive` key defaulting to
  `binary`, clean/smudge filters) — **[from memory; should be re-checked against
  gitattributes(5) before implementation]**.
