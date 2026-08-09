# Issue #492: HDL export policy is not total over the element registry: an element type nobody classified is rejected with no reason, indistinguishable from a deliberate refusal
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Two goals are bundled under one defect. (a) **Totality**: a registry-keyed table in
`jls.hdl` must have a row per element type, so adding a thirty-sixth type is a build
failure rather than a user's failed export. (b) **Diagnostic quality**: a refusal should
carry the reason and the next step, not just a name and a grid coordinate. Both are right,
both serve the project's stated arc — (a) is verbatim the mandate of the parent #315
(FEAT-001), and (b) matches the error-contract discipline the README and ARCHITECTURE.md
already hold the batch surface to. Nothing here should be dropped.

What I take issue with is the *shape* of the fix: it grows a fourth hard-coded
`Set<Class<?>>` inside the consumer, adds a test-only accessor to reach it, and encodes the
reason as prose concatenated into one exception string. Each of those is the locally
cheapest move and each pulls slightly against where the codebase is going.

## 1. The totality assertion contradicts its own parent's contract

#315 §3 fixes the predicate for every registry-keyed table as **equality against the
registry minus a named exemption set**, and rejects containment by name: *"Containment is
explicitly rejected as the assertion: it is exactly the weaker form that allows a stale row
to survive."* #492 §7.10 asserts exactly containment ($R \subseteq C$), and H4 spends a
hypothesis, a threat-to-validity paragraph and a falsification clause defending that
weakening — all because `Wire.class` sits in `TOPOLOGY` and is not registered.

The reframe makes H4 evaporate: **key the assertion on tags, not classes.** The shipped
template is `test/jls/edit/PaletteContractTest.java:44-66` — it derives the expected tag set
from `ElementRegistry.all()`, subtracts a documented `NON_PALETTE_TAGS` set with a written
reason per entry, and asserts `assertEquals`. `Wire` has no tag, so it simply is not in the
comparison; `WireEnd` is registered and stays in `TOPOLOGY` legitimately. You get the stale-row
direction for free, you lose H4/O-4 of the threat list, and — decisively — the test is then
in the exact `covered()` / `exempt()` / `remedy()` shape #375 (TASK-0002) will require, so it
is *adopted* by the shared base rather than rewritten onto it. §12 already concedes the
rewrite is coming; writing it in the house form now avoids paying for it twice.

Corollary: `classifiedElementClasses()` returning `Set<Class<?>>` is the wrong return type
for the same reason. If an accessor must exist, it should hand back the disposition keyed by
`ElementType`.

## 2. A better seam: dispositions keyed by ElementType, not a fourth class-set

The real disease is visible from one grep: `ElementRenderers.BY_TYPE`, `ElementDialogs.BY_TYPE`
and `CHANGE_BY_TYPE` (`src/jls/edit/`), `SaveTags.WRITABLE` (`src/jls/elem/SaveTags.java:41`),
`Palette.entries()`, and now four buckets in `HdlExporter` — six-plus tables, each keyed on
`Class<?>` or a tag string, each maintained by hand in a different file, each with its own
drift story. #201 broke three of them at once (this issue, the sibling `SaveTags` defect, and
`PaletteContractTest`'s `KNOWN_MISSING_ICONS` already carries `RegisterFile` and `FieldExtend`).
#492 fixes the sixth table the same way the other five were fixed. That is duplication of
effort, not of code.

A concrete alternative that cuts along a different seam, and stays inside this issue's cost:

```java
// jls.hdl — dependency direction preserved (hdl -> elem), jls.elem gains nothing
public sealed interface HdlDisposition {
    record Exported()                                   implements HdlDisposition {}
    record Skipped(String why)                          implements HdlDisposition {}
    record Topology()                                   implements HdlDisposition {}
    record Refused(String gap, @Nullable String workaround, int issue)
                                                        implements HdlDisposition {}
}

final class HdlElementPolicy {
    private static final Map<ElementType, HdlDisposition> BY_TYPE = build();
    // build() walks ElementRegistry.all() and requires a disposition per entry.
    static HdlDisposition of(Element el) { ... }
}
```

What this buys that the four-`Set` version does not:

- **The "no decision" state is not representable** in the table's construction, so totality is
  a property of how the map is built, not a claim a separate test has to re-derive. The test
  shrinks to asserting `build()` succeeds and to pinning the per-type dispositions (which is
  what `CapabilityInterfaceTest` already does for `Timed`/`Watchable`/`Rotatable`).
- **The reason stops being prose.** `Refused(gap, workaround, issue)` is structured; the
  successor issue number that Open Question 3 wants to add later is a field, not a follow-up
  string edit.
- **`getClass()` exactness stops being a hazard.** §11 correctly flags that a future subclass
  of a classified element looks classified to a reader and is not. Keying on the descriptor the
  loader already uses (`ElementType.elementClass()`) removes the second, parallel notion of
  "what type is this".
- **It survives the two refactors already scheduled over this code.** #336 (FEAT-004) extracts
  the shared net-partition IR and #315's own graph notes it *"inherits `HdlExporter`'s
  three-bucket element policy"*; #277 moves consumers onto the boot `ExtensionRegistry` snapshot.
  Three hard-coded class literals sets are the thing both of those have to unpick. A table keyed
  on the descriptor moves in one piece.
- **It leaves a door where the architecture says a door goes.** `HdlExtensionPoints.EXPORTER`
  (#223) and the staged external element providers (#212, ARCHITECTURE.md "Plugin trust
  boundary") mean a contributed element type will eventually arrive that `jls.hdl` cannot have a
  compiled-in literal for. §7.11's null-reason arm is the right instinct; a descriptor-keyed
  table is where a contributed type declares its own disposition instead of falling into it.

## 3. The refusal should be data, rendered at the edge — not a concatenated string

`HdlExportException` today carries one `String`. §7.11 already notices the consequence
(*"the message can get long… if this becomes a real complaint, dedupe by class rather than
truncating"*) and Open Question 4 defers the same question again. Both disappear if the
exception carries `List<Refusal>` and the *edge* renders it: the batch surface keeps its one
`jls: error: ...` line (README's stated contract), the GUI dialog can render one bullet per
refused type with the workaround on its own line, and grouping-by-class becomes a rendering
choice nobody has to re-litigate as policy. This is a smaller diff than it sounds — there is
exactly one construction site (`HdlExporter.java:193-196`) and the message-shape assertions in
`HdlPolicyTest` are `contains(...)`-based (P4), so they survive.

## 4. Prose reasons rot; anchors do not

§11 admits it: *"`SubCircuit`'s reason will become false the day #385 lands module
instantiation, and no test will notice."* A refusal that asserts a fact about the current
implementation is a liability in a file nobody re-reads. A refusal that points — "not
exportable yet; see issue N" — stays true until the entry is deleted, which is exactly when it
should change. Better still, `docs/hdl-support-research.md` has **no per-element support
matrix** today; generating (or pinning) one from this table gives students the answer to "will
this ever work?" in the place they would look, and gives the reason strings one home instead of
thirty-five inline paragraphs.

## 5. Disregarding one acceptance criterion: export `FieldExtend`, do not explain it

I am explicitly setting aside the DoD line *"Each of the four `REJECTED` entries carries a
reason… `FieldExtend`'s named workaround has been verified, not assumed"* — and Open Question
2(b), which already blocks execution on it. The proposed remedy is a paragraph telling a student
to hand-build sign extension from Splitter + Binder + Extend, plus the verification work to prove
that hand-build is semantically equivalent. That is more effort than exporting the element.

`FieldExtend` (`src/jls/elem/FieldExtend.java:22-33`) is a k-bit field widened to n bits, filled
with the sign bit or with zeros. The IR already has both halves: `BitMapStatement`
(`HdlModel.java:468-522`) routes arbitrary source bit positions to arbitrary target positions —
sign extension is `sourceIndex = [0..k-1, k-1, k-1, …]` — and multi-range `Binder` already
establishes the precedent of several statements writing disjoint ranges of one target net (the
comment at `:483-485` says so). Zero fill is a `ConstantStatement` into the high range.
`Extend` is handled in eleven lines at `HdlExporter.java:539-547`. So the plausible shape is a
handful of lines in `buildModel` and **no new statement kind and no emitter change** — worth
confirming against partial-net-assignment semantics before committing, but the primitives are
demonstrably there. Deleting a bucket entry beats writing an unverifiable workaround into a
message that looks authoritative. The same question deserves asking of `RegisterFile`: the
adversarial comment notes #291 refutes the memory argument for the single-port case, and the
proposed reason is a copy of `Memory`'s claim.

## 6. What I would keep exactly as the issue has it

All-or-nothing refusal before any model is built (O6 — this is right and the reasoning is
correct); appending rather than substituting so the existing `contains(...)` assertions hold
(P4); the null-disposition arm reporting an unknown class by name rather than NPE-ing (§7.11);
and the insistence that the totality test be observed red before the table is populated (§8).
The refusal to invent a fifth bucket to make a test pass (§10, H1) is the healthiest sentence
in the document.

## Recommended shape

1. Land the `HdlDisposition` table keyed on `ElementType`, replacing the three `Set<Class<?>>`
   literals — same commit, not a follow-up; the four sets and their inversion are the diff.
2. Write the totality test in `PaletteContractTest` form: tags, equality, named exemptions with
   written reasons. It then satisfies #375 rather than owing it a rewrite.
3. Structure the refusal on `HdlExportException`; render at the CLI and GUI edges.
4. Export `FieldExtend` in the same change; refuse `Memory`, `RegisterFile`, `SubCircuit` with
   an issue anchor rather than a prose claim about current internals.

Endorsed as a goal. The stated diff is the version of that goal that will be rewritten twice
(#375, #336) before it settles; the version above settles once.
