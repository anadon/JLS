# Issue #372: TASK-0001: every registry-keyed table is named in one inventory and pinned by a totality test — starting with the two that already dropped RegisterFile
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its apparatus, #372 says: *an element type is one concept, and JLS has shredded
it across N tables, so a type can be born half-alive.* The measured symptom is real and it is
still real on the default branch, not only at the (branch-only) evidence commit:

- `src/jls/edit/BuiltinElementRenderers.java` registers no renderer and no dialog for
  `RegisterFile` or `FieldExtend` (grep of that file for either name returns nothing).
- `src/jls/edit/Palette.java:156,160` *does* carry palette rows for both, so a student can
  click the button and place the element.
- `src/jls/elem/Element.java` declares **no** `draw` at all, so the fallback arm at
  `src/jls/edit/ElementRenderers.java:52-58` is not "the element draws itself" as its own
  javadoc (lines 11-20) still claims — it paints a highlight and nothing else. O4's "zero
  pixels" is structural on master, not an artifact of the probe.
- `src/jls/elem/SaveTags.java` still omits both tags, exactly as the #488 comment measured.
- `src/jls/collab/op/ElementVocabulary.java:39-46` carries both, and its own javadoc (lines
  26-29) says it should have been deleted when the registry landed. It did land.

So the *end* is not in question. What I am judging is the *route*, and the route pulls
against the project's recorded arc in three places.

## The arc this issue is not standing on

The repository already decided what an element type is. `docs/grand-architecture.md:293-296`:
`ElementType` records "a deliberate *two-layer descriptor* split — a headless core half
(tag/class/factory/aliases) and a separate GUI-side palette entry
(icon/category/help/**dialog**)". `src/jls/edit/PaletteEntry.java:1-25` repeats it: the GUI
half carries "the icon resource, the fallback button text, the tooltip, the toolbar group,
and the help topic" — and the class javadoc of `ElementType` names "creation dialog" as
belonging to that GUI half. `src/jls/boot/GuiModule.java:41-46` already contributes every
`PaletteEntry` to `GuiExtensionPoints.PALETTE_CONTRIBUTOR`, and `CoreModule` does the same
for `ElementType`. `docs/extension-points.md:30-31` catalogs both seams as *typed now*.

Against that: `ElementRenderers.BY_TYPE`, `ElementDialogs.BY_TYPE` and
`ElementDialogs.CHANGE_BY_TYPE` are three `HashMap`s filled imperatively by a 156-line
installer, keyed by `Class`, invisible to the module system, and **absent from
`docs/extension-points.md` in every status including `pending`**. That document's own rule is
"Pending seams are named here first." The renderer/dialog pair is a live, load-bearing,
uncatalogued seam. That — not "the table has no test" — is the defect at the altitude this
project works at.

#372 responds to an uncatalogued, undesigned seam by *writing it down in a new document* and
*asserting it in a new test*. Both are true improvements over nothing. Neither closes the
seam, and both add hand-maintained state of exactly the kind that produced the bug.

## Reframing 1 (primary): make the GUI half whole, and the tables become projections

Put `renderer`, `creationDialog` and `changeDialog` on `PaletteEntry`, where the architecture
document already says the dialog lives. `PaletteEntry`'s constructor is already the
all-arguments-required, blank-rejecting gate (`PaletteEntry.java:63-93`) and is
package-private with `Palette` as the only author. Then:

- `Palette.ENTRIES` is the single authoring site. A new element type is one row that does not
  compile until it names its renderer.
- `ElementRenderers.BY_TYPE` and both dialog maps are *derived* — built once by projecting
  `Palette.entries()` (or, better, the boot `PALETTE_CONTRIBUTOR` snapshot, which is #277's
  target and already exists at `GuiModule.register`). `BuiltinElementRenderers` is deleted.
- The non-palette renderables (`Wire`, `WireEnd`, `SubCircuit`) are one short, named list
  beside the projection, not a per-table exemption set repeated in three test classes.
- `PaletteContractTest.paletteIsTotalOverTheElementRegistry` (`test/jls/edit/PaletteContractTest.java:48`)
  becomes the totality test for *four* tables at once, because they are one table.
- The miss arm disappears. `ElementRenderers.draw` can throw, because the projection is total
  by construction — which is precisely the fail-loud conversion #314 wants, delivered by
  deletion rather than by enumerating fall-through sites for someone else to convert.

Under this framing P1/P2/P3 still hold and still fail today; §7.6's inventory row format,
`ElementRendererContractTest`, and the per-table exemption constants of §7.5 are all
unnecessary. **I am explicitly disregarding those acceptance criteria.** A totality test is
the right instrument for a table you cannot merge into its source of truth; it is the wrong
instrument for two tables whose merge the project already designed and half-built.

## Reframing 2: replace the inventory document with the ratchet the repo already invented

§11 concedes the fatal property of `docs/registry-keyed-tables.md`: "A table nobody listed is
a table nobody tests." An inventory of tables is itself a table with a totality problem, and
the fix proposed for that (TASK-0002's `CONTRIBUTING.md` rule) is a review habit — the exact
thing §"Intended Audience" promises to convert *into* a test.

The repo already solved this shape twice. `test/jls/ui/DialogCoverageRatchetTest.java` reads
`target/classes` with ArchUnit and fails when an `ElementFormDialog` subclass appears without
sweep coverage; `test/jls/ArchitectureRulesTest.java` is the same idiom for layering. The
in-idiom deliverable is `RegistryKeyedTableRatchetTest`: sweep bytecode for static
`Map`/`Set` fields keyed by `Class<? extends Element>` or by tag `String` in `src/`, and fail
unless each is either (a) built by projection from the registry/palette snapshot or (b) named
in an explicit exemption constant with a reason. That is a self-maintaining inventory whose
information cannot rot, and it discharges TASK-0002's standing-rule goal without a base class
and without a `CONTRIBUTING.md` clause. Ship the Markdown as a *generated* artifact from that
test if a human-readable list is still wanted — never as a hand-maintained one.

## Reframing 3: `ElementVocabulary` — the recommended default is the wrong one

Open Question 4 recommends keeping the literal plus a derived-equality test, on the ground
that #170's threat model resists coupling the allowlist to the registry. That reasoning
inverts the threat. The property collab needs is *"a peer cannot name a type this install
does not have"* — and deriving the allowlist from this install's own registry is exactly that
property, enforced by construction. A hand-copied literal cannot make the install safer than
its own registry; it can only be *wrong*, in one of two directions, both bad (a stale extra
token widens the surface; a missing token silently drops a peer's legitimate element). The
`Set.of` at `ElementVocabulary.java:39-46` is already a duplicate the class's own javadoc
calls a stopgap. Delete it, delegate to `ElementRegistry`, and if #170 wants a policy knob,
make it a *subtractive* deny-list, not a re-typed copy of the allow-list.

## Reframing 4: the CLI flag work does not belong here

§8's `FlagSpec` mutual-exclusion item, P6, and the §7.1 behavioral break share nothing with
this issue except the word "table". They key off no registry; the drift mechanism is
different; the affected users (instructors scripting `-b`) are different; and the one
behavioral break in the whole task lives there. Worse, `docs/picocli-evaluation-2026-07.md`
is an existing, recorded evaluation of a library whose `ArgGroup` gives mutual exclusion for
free — a decision that ought to be reconciled before hand-rolling a pair table. Split it out.
Its presence here inflates the band, mixes a compatibility break into an otherwise
pure-restoration task, and lets a green `mvn verify` on the CLI half mask the GUI half.

## What I would keep verbatim

- Registering `RegisterFile` and `FieldExtend` (fix now; do not wait on the reframing).
- The pixel-count floor (P3). `test/jls/ElementDrawSmokeTest.java` already places a
  `registerFile` in its fixture and passes today, which proves smoke-grade "did not throw"
  cannot see this class of defect. A positive-ink assertion is the missing floor and is worth
  having independently of any restructuring.
- The registry-driven rewrite of `DialogConstructionSmokeTest`, with the parameter-count
  assertion of §7.12 — that is a projection, and it is the same move as Reframing 1.
- The equality-not-containment rule (§7.10). It is the correct rule wherever a totality test
  survives the reframing.

## Risk of the reframing

Merging renderer/dialog into `PaletteEntry` touches the boot seam and `SimpleEditor`'s
palette construction, which #84 also has hands on; it is a larger diff than #372 as written
and it is not a task-tier change. The honest sequencing is: land the two registrations and
the ink floor as a defect fix *now* (unblocked, small, restores a broken element), and file
the descriptor merge as a feature under #315 with #277 and #84 named as co-owners — rather
than spending the same effort building an inventory document and three exemption sets that
the merge would then delete.
