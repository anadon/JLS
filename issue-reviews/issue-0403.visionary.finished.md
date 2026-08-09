# Issue #403: TASK-0106 (RESIDUAL): the booted module runtime becomes reachable, the op-observer seam actually fans out, and the three pending catalog rows get typed or get an owner
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the machine blocks and the residual-boundary arithmetic and one sentence
remains: **#220 built a module program that does not run the program.**
`src/jls/boot/JlsModules.java:83` boots four modules into a typed registry,
`src/jls/JLS.java:60` drops the result on the floor, and no file outside
`src/jls/module/` and `src/jls/boot/` reads a contribution. That is the right
thing to be upset about, and it is squarely on the project's arc:
`docs/grand-architecture.md` §2 wants a course to ship an element or an exporter
instead of forking JLS, and §4.3 makes the host-publishes-seams inversion the
mechanism. I endorse the end.

I do not endorse the route. Three of this issue's load-bearing pieces are, on
inspection of HEAD, either untestable as specified or aimed at a seam that is
mistyped. Below, then a concrete alternative cut.

## 1. The two central hypotheses are vacuous by construction

**H3/P4 (determinism).** P4 asks: permute the declaration order of
`JlsModules.modules()`, run the goldens, assert byte-identity. But this issue
explicitly does not land any dispatch consumption — the four sites of O3 are
#277's, and §14 forbids touching them. So after this task, as before it, the
goldens are computed from `ElementRegistry.forTag` and the `JLSStart.java:382`
ternary, and **no permutation of the module list can move a golden**. P4 passes
today, passes after, and would keep passing if the fold were implemented with
`HashSet` iteration order. §11 calls the sequencing "the single most important
fact here"; the sequencing intuition is right and the instrument is wrong. The
golden corpus is not the pin — the *registry contents* are. The non-vacuous test
is three lines and needs no goldens at all:

    permute JlsModules.modules(); assert registry.contributions(ELEMENT_PROVIDER)
      is element-wise equal across permutations

That test can fail today. Write that one; delete P4.

**H2 (op-observer fan-out with an empty list).** "Refuted if wiring it changes
any golden." A fan-out over zero observers cannot change anything, in any
program. H2 cannot be refuted, and the specific defect it should catch is
guaranteed invisible precisely because the list is empty — see §2.

An issue whose falsification criteria cannot fire is not cheap; it is expensive,
because it buys the *feeling* of a safety net before #277 removes the real one.

## 2. The op-observer seam is mistyped, and an empty fan-out hides it

`OpExtensionPoints.OP_OBSERVER` is declared `ExtensionPoint<OpSink>`
(`src/jls/collab/op/OpExtensionPoints.java:25`). `OpSink.submit` is specified to
**apply the op to the circuit** and to **throw `OpRejected`**
(`src/jls/collab/op/OpSink.java:14-30`). `docs/extension-points.md:34` gives that
point cardinality **many**, and `:64-66` names the built-in contribution as "the
editor-side `OpSink` that applies and records ops today".

Put those together: the catalog says many appliers may be contributed to the
seam the editor's applier is contributed to. Fan out a submit over that list with
two contributions and the op is applied twice. Meanwhile #403 §7.9/§7.11 specify
the opposite contract for the same objects — observers must not mutate the
circuit, and an observer that throws must not abort the submit. **The declared
type and the intended contract are exact opposites, and `ExtensionPoint` carries
no cardinality field to arbitrate** (`src/jls/module/ExtensionPoint.java:26` is
`(String id, Class<T> contract)` only; cardinality lives in prose).

The design that makes this disappear is a split, not a fan-out:

- `OpSink` stays the single applier. Cardinality **one**, and it is not an
  extension point at all — it is the editor's own collaborator.
- A new `OpObserver` — `void observed(CircuitOp op)`, no throws clause, handed
  an already-applied op and no circuit reference — becomes
  `collab.op-observer`, cardinality many. Then §7.11's "an observer cannot fail
  a mutation" is enforced by the *signature* rather than by a table row, and
  §7.9's "observers must not mutate" is enforced by not passing the circuit.

That is a smaller change than what §8 proposes and it is structurally
falsifiable, which is what this issue says it wants everywhere else.

## 3. The stream is not total, so the fan-out does not unblock replication

The stated beneficiary is the collaboration/replication stack (now #163/#167 per
the 2026-08-08 chain comment). P3 promises an observer sees "every submitted
mutation". True and beside the point: the interesting property is *every
mutation*, and at HEAD the editor's op migration is partial. `submitOp`/
`submitOps` call sites in `src/jls/edit/SimpleEditor.java` cover toggle-watched
(1690), rotate (2265), flip (2288), move (3161, 3411), delete (4953) and probes
(5232, 5247) — eight. `AddElements`, `AddWire` and `SetElementConfig` have **no
editor construction site at all** (only `CircuitOpReader` and inverse-op
factories). Element placement and wire drawing — the two commonest gestures in a
circuit editor — still mutate inline; `SimpleEditor.java:5545` says so ("gestures
migrate to it one at a time").

So a #171 replication consumer built against this seam gets a stream that is
silently lossy for creation and wiring. **Silently lossy is worse than absent**:
absent, the replication work is blocked and visibly so; lossy, it diverges and
the divergence is attributed to the replication layer. Delivering the fan-out
before the op layer is total inverts the risk this issue is otherwise careful
about. Sequence it after gesture migration completes, or ship it with a
structural totality check (no circuit mutation reachable from the editor except
through the sink) so the incompleteness is a build failure rather than a
runtime surprise.

## 4. The accessor fights the architecture it serves

§7.4 wants `JlsModules.runtime()`: a process-global static, set once, throwing
before boot, plus `bootedOrBuiltIn()` for callers that legitimately have no boot.
That is a service locator with an escape hatch — and `bootedOrBuiltIn()` **is**
the silent fallback that §7.10 spends a page proving fatal, renamed. It will be
called from every test in the suite, because JUnit never runs `JLS.main`; within
a release the escape hatch is the common path and the fail-fast arm is the rare
one, which is the opposite of the intended pressure.

The alternative the issue never considers: **don't have an accessor.** There are
exactly four consumers (O3) and one producer. `JLS.main` already holds the
runtime at the moment it is created; pass it — `JLSStart.start(runtime, exh)`
(today `src/jls/JLSStart.java:154` takes only the handler), and on to the
`Editor`/`Palette`/emitter selection. Circuit loading is the one genuinely static
path, and the honest fix there is that `Circuit` receives an `ElementType`
lookup, not that it reaches into a global. Four parameters versus a global with a
two-state lifecycle, a named exception, an asserted exception *message*, an
escape hatch, and the tests to pin all of it. The parameter version also makes
"this run has no module boot" a representable, type-checked state instead of an
exception.

## 5. The cut itself is in the wrong direction

The #403/#277 boundary is horizontal: this issue owns the accessor, #277 owns the
four dispatch sites. The adversarial comment of 2026-08-08 already found the
symptom — both issues specify the same accessor, and the one likely to land first
specifies it without the property that matters. That is not a filing error to be
patched with a `blocks:` edge; **it is what a horizontal cut through a single
design decision always produces.** An accessor with no consumer cannot be
validated, and a consumer with no accessor cannot be written.

The vertical cut, and my concrete counter-proposal:

> **Slice 1 — one seam, end to end.** `elem.element-provider` only. Boot returns
> a runtime; the runtime is threaded (not globalled) to `Circuit`'s tag
> resolution and to `Palette`; `ElementRegistry`'s static table becomes
> `CoreModule`'s contribution and nothing else reads it. Determinism is pinned at
> the registry (permuted `modules()`, equal contribution lists). Acceptance is
> **not** "goldens unchanged" — it is a test that adds a fifth, test-only module
> contributing one element descriptor and asserts that element appears in the
> palette and loads from a `.jls` file. That test fails today for the right
> reason and is the first evidence in this project's history that the module
> program does anything.
>
> **Slice 2 — `hdl.exporter`**, replacing the `JLSStart.java:382` ternary. Same
> shape, one afternoon, now that slice 1 settled the threading.
>
> **Slice 3 — the op seam**, after the split of §2 and after gesture migration is
> total.

I am explicitly disregarding two of this issue's acceptance criteria. "The entire
golden corpus is byte-identical" is the wrong success signal: a change that reads
the registry for dispatch and produces identical goldens is exactly what you
want, but the corpus is equally identical when the change does nothing, which is
the state we are trying to leave. And "no file named in O3 is touched" enforces a
boundary that should not exist — touching `Circuit.java:918` in the same commit
as the accessor is the point, not scope creep.

## 6. What survives untouched

The catalog clause (O4/H4/P5) is good, small, and independent: three `pending`
rows, each needing an owner and a reason. It is also mis-bundled here — it shares
nothing with the accessor but a sprint. File it as its own five-line issue under
#223 and close it this week. Two corrections while in there: `:64-66` claims a
built-in `collab.op-observer` contribution that `CollabModule.register` does not
make, and `:68-71`'s deferral note should name whichever slice actually lands.

O1, O2, O5 reproduce at HEAD (`5311625`) and are stated fairly; the refusal to
re-author the four shipped rows is right; the hot-plane guard (H5/P6) is right
and cheap. The instinct that the mechanism must be consumed before more mechanism
is built is the correct instinct — it just points at a vertical slice, not at a
residual.
