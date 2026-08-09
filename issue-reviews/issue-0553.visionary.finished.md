# Issue #553: FEAT-C27-6: a switcher from Logisim-Evolution, Digital, CircuitVerse or Falstad gets a one-page map from the tool they know to JLS
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The stated job is "keep a switcher oriented through the ten-minute window where they
bounce (#510 §2)." That framing is borrowed from CAP-27 (#511) and it is the one part of
this issue that does not survive contact with the evidence. #510's bounce is a *push*
failure: empty `JTabbedPane` on launch, zero README screenshots, no discoverable examples
(`examples/` holds one Python script). A person who quits in ten minutes did not read four
Markdown pages; they never opened `docs/`. The bounce is owned by #545 (README shop
window), #548 (Examples menu) and PF-3 (first run). Prose cannot do product's job.

The audience these pages *actually* serve is later and different: someone who has already
decided to evaluate a move and is now asking "does my mental model transfer, and what
happens to my files?" That is a genuinely valuable reader — the instructor at a
forced-migration moment is #510's highest-value segment. But the issue never names them,
and as a result its acceptance criteria optimize for the wrong reader. AC-1's
discoverability test is "from the README" — i.e. from the surface a person only reaches
*after* arriving. There is no `pages` workflow in `.github/workflows/`, so a doc in
`docs/` is not an arrival surface at all. If these pages exist to catch organic search
("logisim alternative", "hneemann Digital successor"), they belong on the published
gallery site PF-4/#551 will stand up, not in `docs/`. State which reader you are serving
and the acceptance criterion changes.

## Where it pulls against the project's grain

JLS's distinctive documentation culture is **normative docs pinned by tests**:
`docs/file-format.md` ↔ `FileFormatSpecTest`, `docs/extension-points.md` ↔
`ExtensionPointCatalogTest` (which checks the doc against the constants *in both
directions*, "so the doc can never drift from the code"), `docs/batch-interface.md` ↔
the batch goldens, `docs/simulation-semantics.md` ↔ `SimulationSemanticsRegressionTest`.
Even #545 AC-4 asks for a drift check on README image paths. This project does not ship
unpinnable claims.

Four hand-authored pages of claims about four competitors are structurally unpinnable.
`docs/hdl-support-research.md` — the honesty source AC-3 names — opens by warning that its
own facts are "moving targets" as of 2026-07-08. Competitor gesture-and-concept prose ages
faster than that. The failure mode is not hypothetical: #510 §3 lists "stale Burch-era
docs" as a Logisim-Evolution *user complaint*. Shipping four pages nobody can mechanically
falsify is importing the defect we are citing against a rival.

## Duplication the dedup comment missed

The comment on this issue adjudicates #553 against #545 carefully and correctly. It does
not adjudicate against **CAP-29 (#513) PF-6 — "Migration documentation (0.5–1 mw):
per-format 'what survives, what doesn't' pages (joint with CAP-27 PF-6)."** That is the
same four pages, owned twice, on two different theories: prose-now here, importer-derived
later there. AC-4's "marked slot for the importer link" is precisely the seam where the two
collide — and when CAP-16/#323 lands, the slot's contents will be a *better version of this
whole page*, because CAP-16 AC-1 commits a per-construct coverage table over ≥30 real
`.circ` files from ≥3 course repositories: every construct classified mapped / approximated
/ refused, with reason and location. That machine-generated table *is* the honest "what
survives, what doesn't" content, corpus-measured rather than asserted.

## Concrete alternative A — author the page as a shell around generated data

Do not write four essays. Write one thin, stable, human preamble per tool plus **generated
sections**:

- a per-concept mapping table whose JLS-side cells are checked against the element registry
  by a test in the `ExtensionPointCatalogTest` / `PaletteContractTest` idiom — a cell naming
  an element or menu path that does not exist fails the build;
- the importer's construct-coverage table, injected when CAP-16/CAP-29 land, replacing the
  AC-4 slot rather than sitting next to it;
- the comparison-table transclusion #545 owns.

This collapses #553 and CAP-29 PF-6 into one artifact, makes the page improve automatically
as the importers improve, and gives the claims a CI defender — which is the only kind of doc
this project has ever kept honest. The prose that remains is the part that legitimately
cannot be generated: positioning, and the plain statement of what JLS does not do.

## Concrete alternative B — stop treating the four tools as four equal audiences

#510 §3 says four different things, and this issue's symmetric four-one-pagers shape erases
all of it:

- **Falstad**: "the analog/intuition core is not winnable — do not contest it." An honest
  Falstad page is mostly a redirect: *if you are teaching analog intuition, stay.* Ten lines,
  not a one-pager.
- **CircuitVerse**: browser-first students "not winnable without a web story." The lever for
  that audience is CAP-32's demo link (#573/#574), not a concept map. A CircuitVerse page
  written before the demo exists is a page whose central answer is missing.
- **Logisim-Evolution**: "head-on user pull otherwise unrealistic"; the winnable segment is
  autograding instructors. That page should be a *grading-contract* page — `-t` grammar, VCD
  profile, deterministic exit codes, the container — against their own tracker's admitted
  weakness (#1546/#598). It is a different document from a gesture map.
- **Digital**: §5 is the survey's clearest strategic finding — the winnable-now audience is
  **contributors**, not users, and it costs near-zero engineering. A "coming from Digital"
  page aimed at users is the wrong page for the one segment available today. Aim it at the
  rejected-PR pool: maintained-successor positioning, `docs/extension-points.md`, a
  good-first-issue funnel, demonstrated PR turnaround.

Four pages sharing a template is a tidy deliverable and a strategically flat one. Four pages
each shaped by its own winnable segment is the same budget spent on the actual levers. I am
explicitly disregarding AC-1's "one page each, same shape, each under five minutes" as the
success measure: uniformity is not the goal, conversion of a named segment is.

## Concrete alternative C — put the orientation where the switcher already is

The cheapest orientation asset is not a document. #548 ships ≥10 curated examples with
captions. Add one line to the caption of the canonical few (ripple adder, traffic-light FSM,
register file): *"Logisim calls this a Tunnel; Digital calls it a Wire label."* Recognition
in front of a running circuit beats reading in `docs/`, it lands inside the ten-minute
window this issue claims to serve, and it costs a caption. That is the version of PF-6 that
actually addresses the bounce; the docs pages then honestly serve the evaluator, not the
bouncer.

## On funding and ordering

`ordering_after: []` / "startable now" is the issue's weakest claim, and not merely as
bookkeeping. AC-2 requires per-concept example links (#548), AC-3 requires honest
comparative claims (#545's table), AC-4 requires importer slots (#311/#513), and the
CircuitVerse story wants the demo (#573). Strip those and what remains is glue with nothing
to glue. That is the real signal: this is not an independent 1–2 mw feature but the trailing
half-week of #545/#548/#551, plus a re-aimed Digital contributor page that could ship
tomorrow on its own merits and does not need this issue at all.

## What I would keep unchanged

The instinct behind AC-3 — name plainly what JLS does not do for that audience — is the best
line in the issue and the most aligned with this project's character (`SECURITY.md`'s custody
rationale, the README's unsigned-macOS paragraph, #510's self-scoring at 2/5). Keep it, and
make it the *first* section of each page rather than a closing disclaimer. A switcher who
finds the honest limits at the top trusts everything below them.
