# Issue #498: Virtual-hardware / virtual-logic parity, part 3 of 3: recorded decisions, the exclusion set, kill criteria K1-K9, and milestones M1-M9
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Two things are stacked in one body, and they have different half-lives.

The **archival act** — preserve, verbatim, part 3 of a branch-only design study so that
~15 filed issues whose acceptance and kill criteria are *numbered from it* (#379, #407,
#392, #459, #456, #477, #478, #458, #479, #308, #425, #482, #331, #495) do not become
unreadable when `claude/jls-virtual-hardware-linux-njsoma` is deleted. That goal is
correct and urgent; #493 documents the class of failure and this is one instance.

The **substantive claim** — that JLS should become a two-tier machine joined by a
machine-checked fidelity boundary, with a Linux boot as the headline. That claim is
already largely *right*, but it is filed in a container that guarantees it can never be
acted on.

The reframing below accepts the first goal entirely and changes where and how it is
discharged.

## The reframing: this belongs in `docs/capability-roadmap/`, not in an issue body

The repository already has a home for exactly this genre, and the rescued document
proves it by numbering itself into that home's namespace.

- `docs/capability-roadmap/` holds 15,229 lines of non-normative capability study —
  six sweeps, three keystones, a README and an `AMENDMENT.md` that supersedes rather
  than duplicates. Its own header states the discipline: *"Every claim about JLS is
  anchored to a path in the tree at HEAD."*
- `AMENDMENT.md:151` owns **P13**. The rescued document defines **P14–P21** (#497) and
  quotes `keystone-c-performance.md`'s measured 8,090 cycles/s (`:655`) as its own
  keystone input. It is a continuation of an in-tree numbered series, authored outside
  the tree.

So the honest description of the situation is not "a branch document was lost" but
"a chapter of the in-tree roadmap was written on a branch and filed as a ticket."
Landing it as `docs/capability-roadmap/parity-01…03.md` (status line preserved
verbatim, `AMENDMENT.md`-style banner pointing at it) buys everything the issue wants
and three things it cannot have:

1. **The citations resolve from a clone**, forever, with no GitHub round-trip and no
   dependence on issue bodies surviving the tag-corruption hazard #489 documents.
2. **The text is correctable.** An issue body is frozen prose about a moving tree; a
   tracked file is diffable and reviewable, and the repo's link/anchor discipline can
   reach it.
3. **Cost.** Three issue bodies against one directory addition. The document's own
   §"what was dropped" section — the split criterion, the part table, the flattened
   markdown links, the `*(original file lines N–M)*` markers — is ~120 lines of
   scaffolding that exists *solely* to work around the container. In a file, all of it
   disappears, including the link flattening, because relative links resolve again.

## The deeper reframing: JLS already solved this problem, in code

§7.5's D2 argument is that `Circuit.save` reassigning dense file-local ids on every
save is why one inserted element produces 5,312 changed lines in 234 hunks, and that
**referencing by stable id is the structural fix**. That argument is correct, it landed
(#165/#166: `ElementId.mintFresh()`, `src/jls/Circuit.java:1490-1503` now sorts by
`Element::getStableId`, `DeterministicSaveTest` pins canonical bytes), and its javadoc
states the principle in one sentence: *"Unlike the save-time `int id` — a file-local
reference index reassigned on every save — a stable id is minted once."*

The corpus this rescue serves is failing the identical test one level up. Issues cite
`:418-500`, `:491-495`, `:706`, `:859-890`, `:1909-1914`, `:1917` — **file-local
reference indices into a mutable document**. The rescue's own workaround, injecting
`*(original file lines N–M)*` headings, is the renumbering hazard admitted in prose.
And drift is already measurable: §7.5 cites `Circuit.java:1482` for `FORMAT`, which is
`:102` at HEAD and now reads `FORMAT_VERSION = 2`, not 1; `ARCHITECTURE.md:52-61` still
places `InteractiveSimulator` in `jls.sim` when it is `src/jls/edit/`; `ARCHITECTURE.md`
still says "there is no element registry yet" against a live `jls.elem.ElementRegistry`.

**Concrete alternative: cite the identifiers, never the lines.** The document already
mints stable ids — L0–L9, P14–P21, K1–K9, M1–M9, D1–D7 — and every issue that quotes it
is quoting one of those. A one-time pass rewriting `:1909-1914` to "K9" and `:491-495`
to "L0(c)" makes the citations survive not only the branch deletion but every future
edit of the document, and makes the three-part split invisible to citers. That pass is
cheaper than this rescue was, and it is the only version of this work that does not have
to be redone the next time the text moves.

## What is buried, and should not be

§7 is not a parity chapter. It is a **governance-conflict audit** of the live repository,
and roughly six of its findings are cheap, falsifiable, independent of Linux, and
verifiable at HEAD right now:

- `CONTRIBUTING.md:21` — *"#33 is the tracking issue that orders the current program of
  work"* — points at a closed tracker. Confirmed at HEAD. One-line fix.
- `docs/vcd-interop.md`'s "live co-simulation … rejected — see #63" against an open #63
  whose body plans lockstep co-simulation, and against `grand-architecture.md` §9 which
  lists subprocess co-sim as sanctioned. §7.2's replacement wording — *"an interactive
  session is a recording device; the recording, not the session, is the contract"* — is
  the single best idea in this part, and it is worth its own issue regardless of whether
  any RISC-V work ever happens. The clock on it is real:
  `docs/capability-roadmap/lf-07-api-and-platform.md:309,595` already proposes writing
  *"no callback direction"* into the API contract.
- §7.3's extension-point defects: one device seam specified twice with different ids and
  homes, an illegal cardinality value, an `elem.` prefix on a `jls.sim.equiv` seam. Point
  ids never change once shipped, so these are cheapest before filing, and
  `ExtensionPointCatalogTest` is the enforcement that already exists.
- K9's palette ratchet. Verified: `Palette.java` has exactly **32** `entry(` rows against
  **35** `ElementRegistry.ALL` types, difference exactly `NON_PALETTE_TAGS`. But
  `PaletteContractTest.paletteIsTotalOverTheElementRegistry()` already enforces the
  registry↔palette relation, so a count pin adds only bloat detection. The *actual*
  hole K9 names is the one nobody has: **no performance ratchet exists in `test/`** —
  `SpatialIndexTest:242` prints timings and asserts nothing. That is the missing test,
  and it is the highest-ranked criterion in the document.

Filed as part of a 124 KB verbatim archive labelled `documentation` and self-declared
non-normative, none of these can be picked up. Split them out as ordinary issues with
acceptance criteria and they are a good month of work that strengthens the product with
zero commitment to the flagship.

## On the claim itself

The programme's arc is aligned with the project's, up to a point, and the document says
so more clearly than its own headline does: K9 ranks the first-year student above the
Linux boot, and M3's note observes that stopping there leaves *"a console, deterministic
replay, a normative abstraction-level policy applied retroactively to seven shipped
elements, and a machine-checked fidelity toggle — none of it wreckage."* Meanwhile the
honest total is **155–250 maintainer-weeks at bus factor 1**.

That is the reframe the document circles but never states: **the deliverable is the
fidelity boundary and the replay contract, not the boot.** Under that framing M1–M3 plus
the two standing tracks (L1 constant factors; stable-id → validate → sort-order →
`.gitattributes` → container flip, in that order) are the whole product, and M4–M9 are
one demonstration of it. Every existential kill criterion — K1 (events/instruction), K2
(α and the 12-hour boot), K5 (the Linux target cut), K8 (RV32 nommu removal) — then
threatens only the demo. A programme whose four sharpest kill criteria cannot kill it is
a better-shaped programme, and it is the same work in a different order.

I am not disregarding this issue's acceptance criteria; a verbatim archival rescue has
none to disregard. I am saying the rescue succeeded at preserving text and failed at
preserving *usability*, and that the fix is a directory, a citation scheme keyed to
K/L/M/P ids, and six extracted issues.

## Verdict

**endorse-with-reframing.** Preserve the text — but land parts 1–3 as
`docs/capability-roadmap/parity-*.md` with the status line intact, close #496/#497/#498
pointing at those paths, rewrite the dependent issues' line-range citations to the
document's own stable ids, and extract §7.2, §7.3, §7.7 and the K9 performance ratchet
as separate actionable issues. Do that and the branch deletion stops mattering, for this
document and for the next one.
