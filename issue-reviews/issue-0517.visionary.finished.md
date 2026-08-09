# Issue #517: CAP-33: an instructor adopts JLS by adopting a course, not a tool — a textbook-mapped lab pack, guided lessons, and an assignment starter/submit workflow ship in-tree
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The title carries the whole thesis and it is the best sentence in the capstone
set: **an instructor does not adopt a simulator, they adopt a course.** Every
other capstone in this tracker improves the tool; this one is the only one that
touches the actual unit of adoption. On the arc of the project — a maintained
fork trying to become the default JLS, with exactly one named external
counterparty (#509) — that thesis is correct and probably the highest-leverage
claim anywhere in the roadmap.

The route the issue picks to get there is where I part company. PF-1 (3–5 mw,
the single largest slice) has *this project* author a lab pack mapped
chapter-by-chapter to one commercial textbook. That makes JLS a course author.
The thesis only requires JLS to become a place where courses live and stay
alive. Those are very different institutions, and the second one is smaller,
cheaper, and scales past a single maintainer.

## Ground truth at HEAD

- The entire shipped "course" surface of this repository is
  `/home/user/JLS/examples/autograde/autograde.py` and its CI pin
  `/home/user/JLS/test/jls/AutogradeBridgeExampleTest.java`. `examples/`
  contains nothing else.
- Grading has no representation of correctness.
  `/home/user/JLS/docs/batch-interface.md` §1 defines three exit statuses and
  none of them means "ran fine, answer wrong"; §2.2's `-t` grammar has four
  productions and no expectation side.
  `/home/user/JLS/docs/capability-roadmap/lf-04-formal-and-grading.md` states it
  outright: "JLS has no representation of 'correct.' It has a representation of
  'what happened,' and grading is a string diff over that."
- There *is* a working in-tree content pipeline with a CI truthfulness gate:
  `resources/help/**` + `Map.jhm` + `JLSHelpTOC.xml`, policed by
  `test/jls/HelpTopicsTest` (link checker, reachability, palette coverage). Any
  lab pack should be built as a second instance of that pattern, not a new one.
- `ARCHITECTURE.md`'s recorded decisions are a consistent doctrine about
  *ongoing taxes*: i18n is a non-goal because "a large, ongoing tax with no
  requesting user"; help stays in-jar because the deployment model demands it,
  with hosted docs as the recorded future. A hand-authored 8-lab pack is the
  same tax shape as i18n at larger scale — and unlike i18n, it rots
  pedagogically as well as technically.

## Three structural problems with the route as written

**1. The kit format is being invented in four places at once.** CAP-21 (#502)
already speaks of "the CAP-06 lab-as-data format" and ships PF-6's fixture lab
plus a 300-submission corpus. CAP-06 (#300) owns the verdict artifact. #548
ships curated examples each with a "suggested exercise". #552 ships stepped
build-along lessons. #517 PF-4 then proposes "kit = labs + vectors + schedule +
rubric" as *its own* packaging convention. Five streams, five content shapes,
one maintainer. This is the fragmentation the project has been careful to avoid
everywhere else (one `SaveTags` table, one `LoadError` taxonomy, one `TellUser`,
one extension registry).

**2. KC-33-1 ossifies the weakest interface in the tree.** It permits PF-2 to
ship "against today's three-exit-status contract rather than waiting". Read
alongside `docs/batch-interface.md`'s explicit stability promise, that means
authoring eight labs' worth of grading criteria as literal stdout-byte
comparisons — the `EXPECTED_STDOUT_LINES` anti-pattern that lf-04 uses as the
canonical example of what is broken — and then having eight labs, a CI lane, and
possibly a live course depending on those bytes precisely when CAP-06 wants to
change them. Content is the strongest compatibility anchor a project can create,
and this kill criterion drops the anchor on the interface the roadmap most wants
to move.

**3. In-tree content collides with the release policy #509 asks for.** #509 item
4 wants "a tagged release per academic term, patch-only within a term". Course
content moves on a different clock than the simulator: a lab typo, a clarified
rubric, a re-timed exercise are weekly events. Shipping kits inside the jar and
the release tag means every prose fix is a release event, and every simulator
patch drags course-content churn through the same gate.

## The reframing I would take

**A. Make the kit object PF-0, and make it serve every consumer.** Define one
content unit — a directory with a manifest, a starter `.jls`, a solution `.jls`,
`-t` vectors, expectations, prose, and a stated time budget — and require the
Examples menu (#548), the guided lessons (#552), CAP-21's fixture lab (#502
PF-6), CAP-06's lab-as-data, and this capstone's labs to all be *instances of
it*. One validator, one CI gate (`KitIntegrityTest`, the `HelpTopicsTest` of
course content: every kit loads, simulates, grades, and its stated budget and
rubric are present). This is the seam the whole content cluster is missing, and
it is the part of #517 that only #517 is positioned to deliver. If nothing else
in this capstone ships, this should.

**B. Host courses; do not author them.** The reference customer (#509) already
*has* a course: eight-plus years of CSE 260M labs, written by an instructor who
maintains them and whose fork's 13 open issues #509 rightly calls "the most
valuable requirements document this project has ever had access to." The
high-leverage move is to make Dr. Siever's course the first kit, authored and
owned by him, published as a versioned kit artifact (release asset or sibling
repo `anadon/jls-kits`), validated by JLS's CI against every tagged release —
and then to publish an index page so the second and third instructors can add
theirs. That turns a 9–14 mw authoring project into a ~4 mw platform project
whose content grows without the maintainer. PF-3 becomes the centre of the
capstone rather than its smallest slice, and AC-3's "named external instructor
reviews the kit" stops being a merge gate on work we did and becomes the natural
consequence of work they did.

**C. Drop the textbook mapping; index by concept and ship crosswalks.** Mapping
JLS's flagship content chapter-by-chapter to the Donzellini Springer text buys
one vendor's instructors and taxes everyone else — a Harris & Harris, Mano, or
CSE 260M instructor gets a pack whose organizing principle is a book they do not
assign. Index labs by *concept* (adder, decoder, mux, latch, counter, FSM, ALU,
datapath) and ship per-textbook crosswalk files: twenty lines of YAML mapping
chapters to concept ids, one per book, contributable by whoever uses that book.
One pack, N syllabi, no derivation from any copyrighted table of contents, and
AC-4's licensing question shrinks to the prose we actually wrote.

**D. For the DEEDS wedge specifically, the lever is a porting recipe, not a lab
pack.** The issue's own evidence says the *course* must port because the files
cannot. But what stops a DEEDS instructor on a Tuesday is "my Lab 4 ALU only
exists as a `.pbs`" — and eight labs about someone else's ALU do not solve that.
JLS already ships the bridge: `src/jls/hdl/imp/NetlistImporter.java` imports
Yosys JSON netlists. A documented recipe — *write your reference solution as
Verilog (you already have a testbench), run Yosys, import, get the starter and
solution circuits, derive `-t` vectors from the testbench* — collapses most of
PF-1's hand-authoring into a page of docs and reuses an existing architectural
seam. Its honest limits are already catalogued in
`src/jls/hdl/yosys/CellValidator.java` (async reset, set/reset, wide arithmetic,
clocked memory), which bound the recipe to combinational and simple sequential
labs — precisely the range AC-1's first two thirds cover.

## Acceptance criteria I am explicitly disregarding

- **AC-1 (≥8 labs authored here).** Replace with: *one* kit format, its
  validator in CI, and *two* real kits — CSE 260M's, plus the porting recipe
  demonstrated end to end on one lab. Eight is a padded count, and KC-33-2
  already admits the pack will shrink under review; better to not inflate it.
- **AC-3 (external instructor reviews our kit).** Invert it: the external
  instructor *authors* kit #1 and JLS reviews it against the format. A merge
  gate that depends on a third party's unpaid review time is a gate that stalls.
- **KC-33-1.** Delete it. Do not author graded content against the
  three-exit-status contract; order strictly after CAP-06's verdict slice
  (#300/#369/#466). If #300 slips, ship PF-0 (format + validator) and PF-3
  (corpus as fixtures) — both are useful with no verdict engine at all — and let
  the graded labs wait.

## What survives untouched

PF-2's workflow conventions (distribute → work → submit → cohort grade) are
real, cheap, and orthogonal to all of the above; AC-2's planted-failure CI walk
is the right test of them. PF-4's "a third party can author one" is the actual
capstone outcome — it is just listed fourth and priced last when it is the
thing.

## Verdict

**endorse-with-reframing.** The claim — instructors adopt courses, not tools — is
right and under-served everywhere else in the tracker. The deliverable should
change from *a course we wrote* to *a kit format, a validator, and one real
course its own instructor keeps alive*, with the DEEDS migration served by an
HDL-import porting recipe rather than by original labs. That is roughly 4–6 mw
instead of 9–14, it removes a duplicate content pipeline instead of adding a
fifth, and it does not anchor eight labs to an interface the roadmap intends to
replace.
