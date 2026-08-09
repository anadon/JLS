# Issue #497: Virtual-hardware / virtual-logic parity, part 2 of 3: layers L5-L9, the governance band, the ranked gap list, and the eight unowned programmes P14-P21
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Two different claims are stacked in one body, and they should be judged separately.

1. **A preservation claim:** branch-only design documents must survive the deletion of
   `claude/jls-virtual-hardware-linux-njsoma`, and the way to make them survive is to
   paste them verbatim into GitHub issue bodies.
2. **A product claim:** JLS should become a machine that boots Linux, organised as two
   tiers joined by a fidelity boundary, at a stated honest total of **155–250
   maintainer-weeks** on top of a roadmap that already costs 288–424 (`docs/capability-roadmap/README.md`
   §7 and `AMENDMENT.md` §"Honest totals").

I endorse the *ends* of both and reject the *means* of both. Hence: redirect.

## The preservation claim: the medium contradicts everything this project is

JLS is, more than any other trait, a project about **byte-exact, verifiable artifacts**.
The README spends ~2,000 words on SHA256SUMS, CycloneDX BOMs, `.buildinfo` files,
cosign, provenance attestations, and `docs/reproducibility.md`'s independent-rebuild
recipe. `ARCHITECTURE.md`'s opening rule is "Everything here describes HEAD; specifics
carry a `file` / method anchor so you can verify rather than trust."

Against that arc, storing the corpus that justifies JLS's entire future in **GitHub issue
bodies** is the single most misaligned decision in this rescue. An issue body is the one
artifact class in this repository that is not versioned, not diffable, not reviewable,
not greppable from a clone, not covered by any ratchet test, not in the release tarball,
not mirrored by the flake, and not owned by the project. A maintainer who clones the repo
offline — the exact user JLS ships a self-contained jar for — cannot read a word of it.

**And it is already lossy.** #493 records (via #489) that the tracker's read tool
"corrupts tag-shaped runs" and that writing a body back persists the corruption. That is
not hypothetical here. Reading #497 through the API today returns, in the governance band:

> "**There is no default rule for a new package** — `jls.mach`, `jls.parity`, … inherit
> only the bundle floor until someone writes a rule whose `` matches them."

The `<include>` has been eaten. It is the exact token the sentence is about, and
`pom.xml:429,452,475,498` is where the real ones live. #493 itself carries
`Map, String> REJECTED = Map.of(` where the source says `Map<Class<?>, String>`. **The
archive corrupted the archive on its first read**, and it corrupts precisely the content
class — generics, globs, XML tags, `<`-bearing code — that a 124 KB architecture document
is dense with. A rescue whose fidelity is worse than the thing it rescues is not a rescue.

### The alternative the issue never considers

`git` is already the durable, byte-exact, line-number-preserving, offline, verifiable,
free store this project trusts with everything else. Three routes, any of which is
strictly better and takes minutes:

- `git tag -a archive/virtual-hardware-parity 36cbd37` (or
  `git push origin 36cbd37:refs/archive/parity-study`) — the branch dies, the commit does
  not, every one of the **195** branch-only planning files survives, and `:859-890`
  resolves *literally* instead of through hand-added `*(original file lines N–M)*`
  markers.
- Land the file on `master` under `docs/attic/` or `docs/design-studies/` with its
  non-normative status banner intact. **The repository already has this pattern and uses
  it a dozen times**: `docs/library-survey-2026-07.md`, `docs/flatlaf-evaluation-2026-07.md`,
  `docs/mutation-testing-trial-2026-07.md`, `docs/collaborative-editing-research.md`, and
  `docs/pointer-geometry-census.md` — the last of which opens with exactly this issue's
  problem, solved correctly: "All 'old' line numbers are at commit `6e2b95f` (the
  pre-conversion tree)." A dated study preserved in-tree with a status line is the house
  style. Inlining into an issue invents a second, worse one.
- Both. A tag costs nothing and makes the in-tree copy auditable against the original.

The scaling argument settles it. This rescue saved **3 of 195** branch-only documents
(#493's own table: `docs/plan/**` is 192 files including `REGISTRY.md`, `capstones/`,
`features/`, `tasks/`) at a cost of three enormous issue bodies and a three-way manual
split with hand-maintained line markers. A single ref would have saved **all 195**, byte
for byte, at zero marginal cost per document. The chosen mechanism does not scale to its
own problem, and the 192 files it cannot reach are the registry and task corpus that the
198 filed issues most need.

**I am explicitly disregarding this issue's implicit acceptance criterion** ("the rescue
is complete: every line appears across parts 1–3"). It is not complete — the read path is
lossy, and completeness was never the binding property. Durability, verifiability and
offline availability were, and git delivers all three for free.

## The product claim: the best idea in the document is not Linux

Read as a claim about what JLS should *become*, this document buries its own lead. The
strongest sentence in it is L6's aside:

> *"JLS names the exact instruction where your drawn CPU first disagreed with the
> reference, and prints both records."*

The document calls this a byproduct that "justifies the program on its own" — and then
organises 155–250 maintainer-weeks around booting Linux anyway. Invert that.

**The reframing: the deliverable is the fidelity boundary, generalised; Linux is one
demo.** Strip "RISC-V" and "Linux" out of L4/L6/L17 and what remains is a general
mechanism with no equal in this tool class:

> *Any subcircuit may declare a behavioral reference — an element, a truth table, a state
> machine, or another `.jls` circuit — and JLS can run either binding and report the first
> point at which they disagree, with both records printed.*

That is not a CPU feature. It is **the grading and teaching primitive** for a course tool:
"your ALU vs. the spec ALU", "your traffic controller vs. the reference FSM", "your
carry-lookahead adder vs. your ripple-carry adder", each with first-divergence attribution
instead of a pass/fail at end-of-run. Today `docs/batch-interface.md`'s `-t` grammar and
every comparator in the tree are **end-state only** — the document says so itself in P16.
Moving from end-state comparison to first-divergence comparison over time is the leapfrog,
and it is worth more to JLS's actual users than a Linux prompt ever will be.

Three things fall out of the reframe, and they are the reason to prefer it:

- **It costs a fraction.** The document proves its own cheapness: the trace recorder is a
  *third* `Simulator` subclass using the already-overridden `beforeEvent`/`afterEvent`/
  `probeSample` hooks (`src/jls/sim/Simulator.java:252,269,285` — verified at HEAD), with
  **zero changes to `jls.sim`**. None of the five *fatal* gaps except gap 2 is on its path:
  no host I/O (P14), no guest stack (P21), no byte lanes (P2), no checkpointing (P15), no
  Mode C (L9). Gap 2 is the cheapest of the five and the only one with a consumer outside
  Linux.
- **It stops inventing a parallel ontology.** Viewed through JLS's own architecture, the
  "two-tier architecture joined by a fidelity boundary" is: *a SubCircuit may be swapped
  for an element with the same port signature, chosen at elaboration, and the claim is
  checkable.* That is `SubCircuit` + the #78 registry + the #223 extension-point catalog —
  existing seams. Naming it L0–L9 / P14–P21 / K1–K9 / M1–M9 / D1–D5 plus a "governance
  band" erects a second planning vocabulary beside P1–P13, for one maintainer at bus
  factor 1. Every new plan document adds citation surface that the next branch deletion
  orphans — which is the problem #493 exists to clean up. **The corpus is now growing
  faster than the code, and this issue is evidence of the cost of that.**
- **It converges with the roadmap instead of competing.** Generalised, P16 *is* P5's
  verification program with a stronger oracle; P17's toggle *is* P7/P8's elaboration
  switch with the compiled side stubbed; P20's behavioral machine tier becomes one
  instance of a general mechanism rather than a bespoke 3,000-line ISA package needing a
  new coverage floor, a new `package-info.java`, new NullAway/PackageInfo ratchet rows and
  javadoc `-Werror` down to private members.

## What I would actually adopt from P14–P21

Not eight programmes and not 51–90 weeks. Two items, and a footnote:

- **P19 — the in-tree CPU-scale calibration fixture (4–8 wk, floor 2–3).** The document is
  right that this is "the highest-goodwill item on offer" and it is right that ordering
  matters: it must land **before** `riscv/` is deleted, or P1's stage-5 baseline, P11's
  headline acceptance criterion and every measured number in `keystone-c-performance.md`
  become unreproducible, and P12's whole-program criterion becomes vacuously satisfied by
  `rm -rf riscv/`. This is needed whether or not anyone ever boots Linux. Adopt as filed.
- **A generalised P16 — the differential boundary harness (floor 4–6 wk).** As reframed
  above: not "retirement-indexed", not RVFI-shaped, not RISC-V-coupled. Keep the two rules
  the document contributes — *sampling at the commit boundary, not at settling* and *sync
  point zero* (power-on state must be specified, or two machines agree on every step but
  the first) — because both generalise verbatim to any reference-vs-implementation pair.
  Keep the verdict lattice's one non-negotiable: `UNKNOWN` and `NOT_COMPARABLE` are never
  passes.
- **P18's useful residue is one line.** `timeout-minutes` on the existing nightly lane
  (`.github/workflows/ci.yml`) plus a large-fixture policy is an afternoon, not a 3–5 week
  programme. Do it now, detached from all of this.

**Defer P14, P15, P20, P21 entirely** until a person appears who wants to boot Linux. They
are the Linux-specific ~60% of the budget and they have no second consumer. **Fold P17
into P7/P8.** And leave L9 where the document's own arithmetic leaves it: a 0.5–2× payoff
against an optimised interpreter, purchased with a reopening of the #221 decision recorded
in `ARCHITECTURE.md:341` — the document argues against itself here, honestly, and the
right response is to believe it.

## Two smaller signals worth recording

- **The document's HEAD-anchoring discipline already broke inside the rescue.** L6 states
  "`parity-contract.md` — which exists at HEAD as an unratified proposal". It does not:
  there is no `docs/parity-contract.md` in this tree, and #493 lists it among the 195
  files that "never existed on `master`". A document that describes a branch as if it were
  HEAD decays the moment the branch dies — which is a second argument for the git-ref
  route, where the frame of reference is unambiguous by construction.
- **The honest self-criticism in L7 is the most valuable governance content here and
  should be promoted out of the archive.** "The flagship user-facing deliverable lands in
  `src/jls/edit/`, the one package with no coverage floor and no headless test path." That
  observation applies to *every* future GUI-facing headline feature, not just this one, and
  belongs in `CONTRIBUTING.md` next to the existing #84/#91 note — not entombed at line
  942 of a rescued design study.

## Concrete redirect

1. Push `refs/archive/virtual-hardware-parity` (or a tag) at `36cbd37` before the branch
   is deleted; **all 195** files survive byte-exact with original line numbers. Amend
   #493, #494, #495, #496 and this issue to name that ref in one line each.
2. Land `docs/attic/virtual-hardware-parity.md` on `master`, verbatim, with the existing
   non-normative status banner and one added line: "line numbers as at `36cbd37`" — the
   `pointer-geometry-census.md` pattern.
3. Close #494–#497 as superseded by (1)+(2), keeping the bodies as-is. Do **not** rewrite
   them: #489's corruption hazard makes every full-body update lossy.
4. File exactly two forward issues: *P19, in-tree calibration fixture, blocks the `riscv/`
   deletion* and *reference-model subcircuits: a subcircuit may declare a behavioral
   equivalent, and JLS reports first divergence*. The second is the one that changes what
   JLS is, and it does not mention Linux once.
