# Issue #826: TASK-C569-2: each seam is labelled frozen, evolving or internal, and a breaking change owes a stated notice period — the guarantee Digital never gave its component authors
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this is really for

Strip the framing and #826 asks one question on behalf of a stranger: *if I write a
plugin against JLS, how much of my work is this project undertaking to keep working?*
That is the right question, it is unanswered today, and answering it before #212's
demand gate opens is the correct order — a promise not made in advance accretes
implicitly, and an implicit promise is the worst kind. Endorse the intent.

But the issue answers it over the wrong noun, invents a third compatibility vocabulary
for a project that already has two, builds enforcement machinery for a set that will be
empty on landing, and creates a second source of truth whose reconciliation it then has
to police (AC-5). Every one of those is avoidable, and avoiding them makes the task
smaller, not larger.

## The trajectory this sits in

- The project already ships **two published stability promises**, in one voice:
  `docs/batch-interface.md` §6 ("frozen as specified… a change requires a CHANGELOG
  entry **and** a major bump **or** a compatibility flag") and `docs/file-format.md` §9
  (version-bump rules, readers accept all older versions indefinitely). Neither uses
  frozen/evolving/internal. Both bind to semver + CHANGELOG, which `CHANGELOG.md:5`
  declares repo-wide from 4.3.0.
- It already ships the **enforcement pattern** #826 wants: `FileFormatSpecTest`,
  `ExtensionPointCatalogTest`'s bidirectional doc↔code cross-check, `ArchitectureRulesTest`
  (bytecode rules), `HeadlessCoreRatchetTest` (shrinking baseline). Doc-binds-code is a
  solved, house-standard problem here.
- It already ships an **in-code compatibility carrier**: `ModuleManifest.apiVersion` —
  "a single integer checked only for external modules, where major means break"
  (`src/jls/module/ModuleManifest.java:22-28`).
- `docs/capability-roadmap/lf-07-api-and-platform.md:213-221` already **designs this
  exact deliverable**: a normative doc "under the identical promise `docs/batch-interface.md`
  §6", enforced by "an **API-surface ratchet test**: a checked-in signature file of every
  public member… regenerated only alongside the CHANGELOG entry." #826 appears unaware of it.

So the seam to cut along is not new. It is: *extend the promise the project already
makes, with the mechanism the project already uses, over the surface plugin authors
actually touch.*

## Four reframings

**1. Label types, not seams — the current noun is not where breakage lives.**
A seam is an id plus a contract token (`ExtensionPoint("elem.element-provider",
ElementType.class)`). Freezing that id guarantees nothing: an author's jar breaks when
`ElementType`'s shape changes, or `PaletteEntry`'s, or `Put`'s, or `Element`'s — the
transitive closure reachable from the four contract types. AC-3 as written ("pins the
signatures of frozen seams") pins the seven-row table, which cannot break; the closure
underneath it can and will.

Worse, for the flagship case the closure does not exist yet. `Element` is sealed
(`permits DisplayElement, LogicElement, Wire`) and `LogicElement` permits 24 — an
out-of-tree element author **cannot implement `ElementType`'s target at all**. The type
they will actually implement is `ForeignElementBehavior`, per the `ForeignElement`
delegation design in #399 — which is closed as *duplicate* (2026-08-08), leaving
`ForeignElementBehavior`'s shape an explicitly execution-blocking open question. #826's
`ordering_after` chain and #569's both still cite #399 as if live; that premise is stale.
Labelling `elem.element-provider` while the type an author writes is undesigned is
labelling the doorframe and not the door.

**2. Delete the separate stability record; make the label a column.**
AC-5 ("the stability record and the #223 catalog agree; disagreement is a build failure")
exists *only because* the issue posits a second artifact. Put the label and its one-line
justification as an eighth column in `docs/extension-points.md`'s normative table and
AC-5 becomes structurally impossible to violate — no third consistency check, no drift
class to police. `ExtensionPointCatalogTest` already parses that table with `DOC_ID`;
asserting "every row carries exactly one of frozen|evolving|internal" is roughly ten
lines added to a test that already runs. One document, one truth, one check.

**3. The notice channel should be the compiler and the resolver, not prose.**
AC-2 asks for a "notice period". A single-maintainer, tag-triggered-release project
cannot honour a *calendar* notice, and AC-4 half-knows this. The honest, checkable and
strictly stronger form is release-based and machine-delivered:
- `@Deprecated(since = "5.2.0", forRemoval = true)` on the member. This is the only
  notice mechanism that reaches the plugin author *at their own compile*, months before
  they hit a `NoSuchMethodError`; javac emits it whether or not they read the changelog.
  The project already runs warnings-as-errors, so its own uses stay policed.
- Bind removal to `ModuleManifest.apiVersion`: a break to anything in the API closure
  bumps `apiVersion`, and the resolver refuses (or loudly warns on) an external module
  declaring an older one. That converts "we promise to give notice" into a diagnostic
  naming the module and the version — the same loud-at-the-boundary discipline
  `ExtensionRegistry.contribute` already applies to wrong-typed contributions.
A doc line is a promise; a resolver check is a guarantee. Digital's real failure was not
the absence of a stability paragraph, it was that a component author found out by
breaking. Prose does not fix that; the two mechanisms above do.

**4. Publish one compatibility policy, not a fourth bespoke one.**
JLS's promise surface is already plural: `.jls` format, the `-t`/stdout/VCD batch
contract, the extension API, soon `jls.api` (lf-07). Today two of those carry promises
buried at §6 and §9 of long normative specs — an outside developer will never find them.
The higher-leverage artifact is a single top-level `COMPATIBILITY.md` naming *every*
public surface with its tier and its break rule, with the three specs pointing at it and
the README's Documentation list linking it. Same effort as writing a bespoke extension-API
policy; it answers the stranger's question for the whole product and it pre-fits lf-07's
`jls.api` instead of forcing a fifth vocabulary later. Reuse the existing wording
("frozen as specified; a break requires a CHANGELOG entry and a major bump or a
compatibility flag") rather than minting frozen/evolving/internal, or state explicitly
how the new three-label vocabulary maps onto the semver rule the other surfaces already use.

## What I am disregarding, and why

**AC-3, as written, should not be built.** By AC-4's own honesty test, at current
capacity — with #212 demand-gated, no `ServiceLoader` in `src/`, `ForeignElementBehavior`
undesigned, and three of seven catalog rows still `pending` — the correct label for
essentially everything is *evolving*, and `internal` for everything not in the table.
AC-3 therefore commissions a build-failing signature-pinning mechanism whose enforced
set is empty on the day it lands. That is machinery maintained for a hypothetical.

Replace it with the two checks that are non-empty today:
- (a) the label column + one-label-per-row assertion of reframing 2, and
- (b) lf-07's **API-surface signature golden** over the *closure* — a checked-in
  `docs/api-signatures.txt` of every public member reachable from the contract types,
  regenerated only alongside a CHANGELOG entry. This one is non-empty immediately, it
  catches the breakage that actually strands plugin authors, and it is the same
  regenerate-with-justification mechanic as the existing ratchets. When a seam is later
  promoted to frozen, the golden already pins it; nothing new gets built.

## Strategic weight

Be clear-eyed about CAP-30's arithmetic. #514's own evidence says Digital repels
contributors through rejected PRs and a discouraging CONTRIBUTING, not through a missing
stability paragraph; AC-6 is moved by #827, not by this. #826 is the third slice of the
fourth-ranked feature of a capstone whose leverage sits in PF-1/PF-2/PF-6. That argues
for the *small* version of this task, which is exactly what the reframings produce:
one column, one policy document, one signature golden — roughly the 0.5 end of the
stated 0.5–1 band, with the second half spent on `COMPATIBILITY.md` covering all four
surfaces rather than on frozen-seam machinery guarding nothing.

## Revised acceptance criteria (proposed)

- AC-1′: `docs/extension-points.md` gains a **Stability** column; every row carries
  exactly one of frozen/evolving/internal with a one-line justification; typed-now rows
  default to *evolving* and `pending` rows to *not yet API*.
- AC-2′: A top-level `COMPATIBILITY.md` states the break rule for every public surface
  (`.jls` format, batch interface, extension API, and any future `jls.api`), reusing
  `batch-interface.md` §6's wording; the three specs and the README link it.
- AC-3′: Deprecation notice is delivered as `@Deprecated(since, forRemoval = true)` plus
  a CHANGELOG entry; removal may not land before the next MAJOR, and bumps
  `ModuleManifest.apiVersion`, which the resolver checks for external modules.
- AC-4′: `ExtensionPointCatalogTest` asserts the label column (one legal label per row);
  a `docs/api-signatures.txt` golden pins every public member reachable from the contract
  types and fails the build when regenerated without a CHANGELOG entry.
- AC-5′: (dissolved — the record *is* the catalog.)
- AC-6′: The policy names `ForeignElementBehavior` as undesigned and therefore
  unpromised, and #569's stale `#399` ordering is repointed at the issue that superseded it.

## Where I could be wrong

If the maintainer's near-term intent is to freeze `HdlEmitter` and `OpSink` outright —
both are small, closed, and stable — then AC-3's set is not empty and the pinning check
earns its keep on day one. Even then it should be the closure golden of AC-4′ rather than
a seam-signature pin, because `OpSink` breaks through `Op`, not through its own name.
