# Issue #793: TASK-C584-3: today's HTML migrates mechanically with a byte-auditable diff report, and the same source emits the static site target
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this task is really for

Strip the mechanism and the end is: **no student-facing sentence is lost or
silently altered while help stops being a hand-maintained HTML tree**, and
**the jar's manual and the web manual can never disagree**. Those ends are
right, serve CAP-35 and FEAT-C35-1 (#584), and close the orphaned "hosted
docs are the planned future" decision in ARCHITECTURE.md. I endorse them.

I am disregarding all four acceptance criteria as written. Three measure the
wrong invariant; the fourth asserts a property that should be unrepresentable
rather than tested. The corpus is smaller and worse than they assume:
`resources/help` is 83 HTML pages, 9 GIFs, 1 JPEG, 488 KB, **16,356 words**.

## 1. The byte-audit measures an invariant the migration necessarily breaks

Today's pages are 4.1-era HTML 3.2: unquoted attributes (41 of 83 files —
`<a href=../keypad.html>`), not one closing `</p>` in the tree, `<font
color=…>` in 36 files, hand-wrapped prose. Any generator emits a normalized
template shell with quoted attributes, closed tags, and its own line breaks,
so a page-by-page byte diff will be **~100% divergent on all 83 pages**.
AC-2's "every intentional difference is named — an unexplained difference
fails the migration" then collapses into either naming every line of every
file (re-transcription, not audit), or smuggling normalization into the
differ — at which point the audit is not byte-level and the criterion is
lying about itself.

**Alternative: audit the semantic projection, and make the audit permanent.**
Compare a normalized per-topic projection — heading text, the ordered
block-level text runs with whitespace collapsed, link targets, link anchor
text, image sources. That projection *is* the contract with the student;
bytes are not. Divergences in it are few, real, and genuinely nameable.

Then invert what gets committed. AC-1 wants the **converter** kept forever —
a one-shot tool with no ongoing consumer, kept compiling against a build that
treats warnings as errors and runs SpotBugs at threshold High. What deserves
permanence is the **checker**: freeze today's `resources/help` as a 488 KB
golden fixture under `test/` and make "generated tree is projection-equivalent
to the 4.1 golden, modulo a committed exception list" a standing test. Every
future intentional divergence becomes a reviewed edit to that list — forever —
instead of a report true for one afternoon in 2026. That turns the issue's
best idea (name your differences) into a durable ratchet, which is how this
repo already works (`HeadlessCoreRatchetTest`, `NotificationRatchetTest`,
`ExtensionPointCatalogTest`).

## 2. "Mechanically" is a means promoted to an end, and it launders defects

The issue wants mechanical conversion *because* it wants losslessness. But
mechanical is neither necessary nor sufficient: a scripted conversion can
silently drop a `<font>`-wrapped clause, and a hand conversion verified by the
projection checker cannot. With the checker in place the mechanism is an
implementation detail — scripted first pass plus hand repair is fine, and for
16k words that is a day's work.

Insisting on mechanical does active harm, because the corpus should not
survive intact. It carries defects a faithful migration preserves and then
requires someone to *justify fixing*:

- **26 low-contrast font tags** on `JEditorPane`'s white default background:
  8 `color=yellow`, 16 `color=pink`, 2 `color=cyan` (plus 46 `green`, 16
  `fuchsia`). `<font color=yellow>Repeat Previous AND Gate</font>` is
  effectively invisible. This is a live accessibility defect in a project
  that maintains `docs/keyboard-a11y-verification.md`.
- **Presentational color standing in for semantics**: these tags are naming
  *buttons* ("the green OK button"). In a source form worth having, that is a
  UI-affordance role, not a color literal — and it is the thing that should
  be asserted against the real dialog, not a hex value inherited from 2014.
- **Prose describing the 4.1 UI**, written before FlatLaf (#153) changed the
  chrome the pages describe.

Under AC-2 as written, every one of those fixes becomes a line item in a
migration report defending why the new tree differs from the old. That is
precisely backwards: the migration is the cheapest moment JLS will ever have
to fix them, and the criteria price that moment as paperwork.

## 3. Two targets is one target too many

AC-3/AC-4 build two output trees and then test that their topic sets match.
But ARCHITECTURE.md already records the settled position that the in-jar tree
is written *so that* "the same tree can be published to the web without
rewriting." The divergence AC-4 guards against is one the design invents.

**Alternative: one page-set, two skins.** Emit one content tree; drive both
renderings from a single iteration over the topic list derived from `Map.jhm`.
The in-jar skin gets the HTML 3.2 subset `JEditorPane` tolerates; the site
skin gets HTML5, CSS, a search index, and the version banner FEAT-C35-2 (#585)
needs. A topic in one and not the other is then not a test failure but
unrepresentable, since both walk the same list. AC-4 survives as a cheap
tautology guard, but the guarantee comes from construction — strictly stronger
than assertion. It also makes FEAT-C35-2 AC-3 ("every hosted URL derivable
from its in-jar topic id") fall out for free instead of being a second mapping
to maintain.

## 4. The largest slice of this corpus should not be migrated at all

39 of the 83 pages live under `resources/help/elements/**`. ARCHITECTURE.md's
"Adding an element today (the honest list)" makes those pages steps 14–15 of a
sixteen-step ritual and names #78's element registry as the recorded direction
that "will collapse most of this." FEAT-C35-4 (#587) AC-2 then wants each
element page's ports, parameters and behavior asserted against the registry
descriptor.

Compose those and the conclusion is sharp: **element reference pages should be
build products of the registry, not prose asserted against it.** Ports,
bit-widths, parameters, orientation support and delay behavior are registry
data; only the pedagogical commentary ("what a mux is for") is authored. Split
each element topic into generated tables plus a short authored block, and
#587's AC-2 becomes unnecessary for the generated half — a generated port list
cannot drift — while step 14 of the honest list shrinks toward zero.

Migrating those 39 pages now as flat prose hardens exactly the shape #78 and
#587 want to dissolve, and hands #587 a drift problem this task created. The
right cut: **migrate the 44 non-element pages now; carve `elements/**` out
with a stated dependency on #78, exactly as FEAT-C35-3 (#586) carves out
interaction scripting with a stated dependency on #91.**

## What I would keep

- Content loss must be provable, not asserted in review.
- The single-goal, single-source constraint — the whole point of #584.
- Ordering after #792, and its AC-3 offline-completeness floor, which nothing
  here may erode.

## Restated criteria

1. A committed projection-equivalence checker compares the generated tree to a
   frozen `resources/help@HEAD` golden fixture on headings, block text runs,
   link targets, link anchor text, and image sources.
2. A committed exception list names every intentional divergence with a reason;
   an unlisted divergence fails the build, permanently — not once.
3. Migration covers the 44 non-element pages. `elements/**` is deferred to a
   follow-up that generates port/parameter tables from #78's registry, and the
   dependency is stated in-tree rather than discovered by #587.
4. The 26 low-contrast font tags are fixed in the migration, listed in the
   exception file as deliberate accessibility corrections, not preserved.
5. One goal emits both renderings by walking one topic list, so a topic cannot
   exist in one target and not the other.
