# Issue #547: FEAT-C26-5: the VPAT writes itself from the test suite — no criterion claimed without a named passing test, and Swing limits listed as exceptions by name
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Not a document. The end it serves is **an instructor being able to answer their IT
department without lying**, and — because CAP-26 (#507) declares its outcome to be
"lab completion, not checkbox compliance" — a mechanism that makes it *structurally
impossible* for JLS's accessibility claims to drift ahead of JLS's accessibility.
The generator is one candidate mechanism. It is the wrong one, and the project has
already written down why, in its own voice.

`docs/standards-adoption/03-accessibility-conformance.md` is a 920-line playbook for
exactly this deliverable. Its "Top three ways this goes wrong", item 1, reads:

> **The ACR is written from the test suite and never from a screen reader.** Every
> automated signal in this repo reads the Java Accessibility API. Not one of them
> proves a *bridge* delivers anything to a *user*.

Issue #547's title is that failure mode stated as a feature. That is the whole review
in one line; the rest is what to build instead.

## Three ways the stated design pulls against the project

**1. "No test, no claim" is mechanically honest and substantively false — today,
provably.** `scripts/build-installer.sh:145` still derives the jlink module set from
`jdeps --print-module-deps`, which can never emit `jdk.accessibility` because nothing
in the jar references it. Every shipped `.msi` therefore bundles a runtime with **no
Java Access Bridge**: NVDA and JAWS read nothing from an installed JLS. Meanwhile
`MenuMnemonicAndAccessibleNameTest`, `PaletteButtonAccessibilityTest`,
`KeyPadAccessibilityTest` are green — they call `getAccessibleContext()` in-process,
where no bridge is involved. A generator obeying AC-1 literally would emit
"Supports" for 502.3.\*/11.5.2.\*/4.1.2 on the primary Windows distribution, each row
citing a named passing test, and every word of it false to a user. The bug is not in
the generator; it is in treating "a passing test exists" as the *type* of evidence a
conformance rating rests on. Test-existence is a necessary condition being used as a
sufficient one.

**2. An ACR is mostly non-claims, and a generator cannot emit those.** The playbook's
own rating table is `Partially Supports` six times over, with a verbatim
canvas-limitation remark to reuse; four WCAG SC are `Not Applicable` by E207.2; whole
EN clauses (6, 7, 8, 9, 13) are Not Applicable; "Not Evaluated is only permitted for
AAA — so every A/AA row needs a real answer". The rows that carry the document's
credibility ("Does Not Support" used at least once, per the playbook) are precisely
the rows with no passing test. A "no test, no claim" generator emits a document whose
omissions are its most important content — and a procurement reviewer reads an omitted
row as evasion. The invariant needs to be **totality of rows with typed evidence**, not
selectivity of rows by test.

**3. The target is scoped to the wrong table.** CAP-26 OQ-2's default ("WCAG 2.2 AA,
desktop mapping") is what #547 filed under. The playbook says the artifact procurement
asks for is **VPAT 2.5 INT**, carrying three tables — WCAG 2.2, Revised 508 (WCAG 2.0
incorporated), EN 301 549 (WCAG 2.1 in V3.2.1) — and that "**the criteria that carry
the real weight are not WCAG at all**": 508 §502.3.1–502.3.14 and EN 11.5.2.\*, which
is exactly where JLS's canvas gap lands. A WCAG-2.2-AA-only ACR omits the rows a
university reviewer actually reads, at roughly zero saved effort since the tables
share the WCAG rows. This is not a detail: it is the difference between answering the
request and answering an adjacent question.

## The alternative: an evidence ledger with rating ceilings, not a generator

JLS already has the right seam and #547 walks past it. The house pattern for
document truth is **hand-authored normative doc + drift test**:
`FileFormatSpecTest` (docs/file-format.md), `CliFlagTableTest` (#71),
`HotkeysHelpAccuracyTest` (hotkeys.html vs `EditOp` accelerators), `HelpTopicsTest`,
`ExtensionPointCatalogTest`, `KeyPadAccessibilityPinTest` (source-grep pin). The doc
stays reviewable, diffable, and human-written; the test makes drift impossible. The
playbook independently reaches the same place: "`docs/accessibility-conformance-report.md`
— canonical, versioned, diffable, reviewed like any other change." Generated documents
are the one thing that seam does not produce.

Concretely, replace the generator with:

- **`docs/accessibility-conformance-report.md`** — authored, one row per criterion over
  the *union* {WCAG 2.2 A+AA, 508 Ch.3/5/6, EN clauses 5/10/11/12}, each row carrying a
  machine-readable evidence annotation, e.g.
  `rating=partially-supports; class=automated; tests=jls.ui.AccessibleNameCoverageTest#everyFocusableIsNamed,jls.ThemeContrastTest#defaultRolesMeetNonTextContrast; surfaces=chrome,canvas`
  or `rating=supports; class=at-session; log=docs/accessibility-at-checklist.md#2026-08-nvda-win11`.
- **`AcrEvidenceTest`** (headless, joins `mvn verify`) enforcing, in order of value:
  1. **Totality** — every criterion in a pinned criteria list has exactly one row; a
     missing row is red. (A generator structurally cannot give you this.)
  2. **Citation resolution** — every cited test FQN resolves to a real `@Test` method
     on the test classpath; a renamed or deleted test reddens the row that cites it.
     This is #547's real content, and it is ~40 lines of reflection.
  3. **Rating ceiling by evidence class** — the load-bearing rule. `class=automated`
     cannot rate 502.3.\*/11.5.2.\*/4.1.2 above `Not Evaluated`; those require
     `class=at-session` with a dated log naming AT and OS versions. `Supports`
     requires the row's `surfaces` set to cover all five surfaces (chrome, canvas,
     simulator+trace, help, CLI) — which today it cannot, so the trace window
     (`src/jls/edit/Trace.java:20`: mouse-only, 626 lines) can never be forgotten into
     a 2.1.1 "Supports".
  4. **Staleness** — an `at-session` citation older than the current minor release, or
     naming a commit that is not an ancestor, demotes to `Not Evaluated` and goes red
     until re-run or re-rated.
  5. **Named exceptions are rows, not footnotes** — a Swing limit found by #544's spike
     is a row with `rating=does-not-support; class=platform-limit; reason=...`, so
     "papered over" is a build failure rather than an editorial virtue.

The ITI-layout VPAT then becomes a **formatter** over this doc for the release asset —
a printer, not an authority. KC-26-3 survives in stronger form: there is no path by
which a prose edit raises a rating, because the rating field is typed and the type is
checked. That is what #547 wanted; it just tried to get it by removing the human
instead of by constraining them.

## Sequencing: file it first, red, as the capstone's scoreboard

`ordering_after: [FEAT-C26-1, FEAT-C26-3]` (and #549's suggested addition) treats the
ACR as a trophy minted after the work. Invert it. The ledger is cheapest and most
useful **first**, filed with every row at its honest current rating — mostly
`Partially Supports`, `Does Not Support` for the canvas and trace, `Not Evaluated` for
everything interoperability-shaped. Then:

- #542 (grayscale/tritanopia) lands and flips 1.4.1/1.4.11 rows with named tests.
- #549's ratchet lands and flips 2.1.1/4.1.2 chrome rows.
- #544's Orca spike lands and flips — or, on KC-26-2, permanently pins as a
  platform-limit row — the 502.3 live-announcement rows.
- #546's bundle lands and flips 1.1.1 via E101.2 Equivalent Facilitation.

Every one of those features then has a *single, shared, mechanically-checked
statement of what it bought*, and CAP-26's §1 step 4 becomes "the ledger is green and
these rows moved" rather than "run the generator". That is a capstone scoreboard, and
it is a strictly better fit for a capstone that says its outcome is lab completion
rather than compliance. It also costs less, because the criteria-set desk work
(playbook §5, ~2 days) is the dominant cost either way and is done once, early, where
it can steer the other five features instead of auditing them.

Two prerequisites #547 does not name and should own or block on:

- **The `jdk.accessibility` module fix** (one line in `scripts/build-installer.sh`,
  plus `assistive_technologies=` in the jlink image's `conf/accessibility.properties`,
  pinned by a source-grep test in the `KeyPadAccessibilityPinTest` style). The playbook
  is explicit: publishing an ACR before this means publishing either "Does Not Support"
  on the flagship rows or a false claim. `ordering_after` should carry this, not #542.
- **`docs/accessibility-at-checklist.md`** — the three-platform manual AT audit
  (Orca/NVDA/VoiceOver) that the rating ceiling makes load-bearing. **No CAP-26 feature
  currently owns it**: #544 owns NVDA *documentation*, nobody owns the dated session
  logs. Without it, the ledger correctly holds every interoperability row at
  `Not Evaluated` forever — honest, but not the outcome CAP-26 wants.

## If the band shrinks

The playbook's fallback is the right one and #547 should record it: if the maintainer
cannot run real AT on all three platforms, ship **`ACCESSIBILITY.md`** — what is
keyboard operable, what the canvas limitation is, how to report problems — and stop.
"A half-audited ACR is worse than no ACR." And the harder line, which I think is
correct and which #547 should quote rather than dodge: *"If you only have budget for
one, build the outline view and fix the contrast — that helps an actual blind student;
the ACR only helps a procurement officer."* Today `Theme.DEFAULT` paints the #75
keyboard caret at **1.14:1** against white. A document about that is worth less than
fixing it.

## What I am disregarding, and what I would keep verbatim

Disregarded: **AC-1 as written** ("no claimed criterion without a named passing
automated test, checked mechanically") as the governing invariant, and the "generator"
framing of the outcome. Reason: the invariant is satisfiable today by a document that
is false on the primary Windows distribution, and it cannot express the negative and
Not-Applicable rows that make an ACR credible. Also disregarded: OQ-2's WCAG-2.2-AA
scoping, in favour of VPAT 2.5 INT's three tables — that needs a `REPLAN:` on #507,
not an edit here.

Kept, and worth defending: the kill criterion (KC-26-3), the fixed claim strength
("guideline-compliant, machine-verified", never upgraded by prose edit), the
manual-vs-automated distinction, and the refusal to paper over Swing limits. Those
four are the issue's actual insight, and the ledger design above implements all four
more strictly than the generator does. The goal stands; the mechanism needs replacing.
