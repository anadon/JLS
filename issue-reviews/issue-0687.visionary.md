# Issue #687: TASK-C524-2: breaking the CLI contract becomes a versioned event — a semver-plus-deprecation-window ratchet, and a seeded violation fails CI before any adapter test runs
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the machine block and #687 asks for one guarantee: **an instructor's autograder,
pinned in September, still produces the same scores in April.** Everything else — semver
prose, deprecation windows, CI lane ordering — is apparatus for that sentence. CAP-21
(#502) needs it because four adapters consume one interface; the deeper reason is that a
grading contract is a promise made to people who will not read the CHANGELOG.

The goal is right and it is squarely on the project's arc. My objection is to the
*mechanism vocabulary*: "semver plus a deprecation window" is a library-ecosystem idiom
imported wholesale, and JLS has already invented — and shipped — a better-fitting
discipline for exactly this problem, one interface over.

## Most of the policy already exists, and the part that exists is wrong in a load-bearing way

`docs/batch-interface.md` §6 is already a written versioning policy for this surface:

> A change that alters any byte a conforming consumer could observe requires: (1) a
> CHANGELOG entry, **and** (2) a major version bump, **or** a compatibility flag that
> keeps the format specified here available unchanged.

Plus SemVer 2.0.0 is already an adopted standard (#169; `docs/standards-landscape.md:96`,
HAVE), `CHANGELOG.md:4-6` binds releases to it, and the standards-adoption docs already
run a per-change "**Stability contracts touched:** …" ritual by hand
(`04-tool-qualification-and-scope.md:306`, `07-waveform-formats.md:543`,
`01-iec-ieee-symbols.md:302`). So AC-1 is largely a rewrite, not a greenfield write — and
#687 should say so, because whoever picks it up will otherwise author a second, subtly
divergent policy next to §6.

The load-bearing defect is in the clause §6 already has: **it ties contract compatibility
to the product's major version.** JLS is at 5.0.5-SNAPSHOT and ships a GUI. Under §6,
adding exit status 3 or growing the xUnit schema — the exact FEAT-053/#466 evolution AC-4
must admit — is an observable byte change and therefore demands JLS 6.0.0. That is
KC-21-4 ("a frozen contract that blocks the verdict engine's own roadmap is worse than no
freeze") firing on the policy the issue is about to codify. The escape hatch §6 offers,
"or a compatibility flag", is the right instinct with no structure behind it.

## Reframe 1 (the main one): version the contract, not the product — and copy §9 of the file format

JLS already solved this for its other frozen public interface. `docs/file-format.md` §9:

- an **independent monotone version** (`FORMAT 1`/`2`, bound by `Circuit.FORMAT_VERSION`),
  orthogonal to the product's semver;
- "**A reader MUST keep accepting all older versions … indefinitely; version support is
  only ever added**";
- "**A writer MUST emit the header with the highest version whose features the file
  uses**", so files that avoid new features stay readable by older JLS.

Transpose that to the CLI and the problem #687 is trying to police mostly disappears:

1. **`contract 1` is a version number JLS owns, not JLS's release number.** Status 3 and a
   grown xUnit schema are `contract 2`. No product major bump, no tension with #466,
   AC-4 satisfied structurally rather than argued in prose.
2. **Emit the lowest contract that suffices; let the adapter pin.** `jls --contract=1 -b …`
   emits exactly the frozen contract-1 surface forever. Byte-identity then survives *JLS
   upgrades*, not merely platform boundaries — a strictly stronger claim than CAP-21 AC-1
   currently makes, and the one that matters when Gradescope's cached image and the lab
   machine's jar drift apart mid-semester.
3. **The deprecation window dissolves.** Support is only ever added; nothing is removed on
   a clock. This matters concretely here: `ISSUE-AMBIGUITIES-2026-07.md:456` records that
   this project's *last* prose deprecation window (#80/#48) rotted into "calendar dates
   are unrecorded". A policy whose enforcement is a date in a markdown file has already
   failed once in this repo. Do not write a second one.

**I am explicitly disregarding AC-1 as written** — "the length and mechanics of the
deprecation window" — and proposing it be replaced by: *the previous contract major
remains implemented and conformance-tested; removal requires a separate, filed decision
with a named successor clause.* That is enforceable by a green test suite, not by a
calendar. If a window is still wanted, denominate it in *contract versions supported
concurrently* (≥2), never in months.

This also fixes #690's shape. "Which version do you implement" (singular) forces refusal —
a failure mode. "Which versions can you emit" plus a selector makes the old adapter keep
working. Refusal is the fallback, not the feature.

## Reframe 2: make the ratchet mechanical — one generated manifest is the source of truth

AC-3 wants the ratchet to "distinguish an additive change from a breaking one". As stated
that is a human judgement in a review checklist, i.e. the same thing the standards-adoption
docs already do by hand and the same thing that rots.

The tree is one small step from mechanizing it. `JLSStart.FLAGS`
(`src/jls/JLSStart.java:759`) is already the single authoritative flag list, and
`CliFlagTableTest` already fails if anyone maintains a second one (#71). So:

- **Generate** `docs/cli-contract-1.json` from the code: flag table (name, arity, operand),
  exit statuses, stream assignment, artifact path rules, xUnit XSD digest, contract version.
- **Check it in as a golden**, the discipline this project already runs everywhere.
- **The ratchet is a diff classifier**, not prose: a removed or altered entry is breaking
  and requires a new contract major with the old manifest retained; a new entry is additive.
- `jls --contract-version` (#690) prints from the same file — discharging #690's AC-4
  ("derived from the same source of truth the ratchet enforces") by construction rather
  than by vigilance.

This collapses #686, #687 and #690 onto one artifact. #686's AC-3 already asks for the
xUnit schema to be "pinned as a schema artifact, not described in prose alone" — the
manifest is that idea applied to the whole contract instead of one clause of it.

## Reframe 3: the falsification should be permanent, and the ordering should be dataflow

AC-2 as written produces a *transcript recorded once on a scratch branch*. This project
already knows the better pattern and uses it twice:
`scripts/wayland-rig-selftest.sh` and `scripts/x11-rig-selftest.sh` drive the unmodified
rig against a stub and assert each scenario gets its documented exit code, on every push;
`NotificationRatchetTest` does the same for `TellUser`. A `CliContractRatchetSelfTest` that
mutates a copy of the manifest in a temp dir and asserts the ratchet goes red is a
permanent guarantee. A transcript is an anecdote about one commit.

On ordering: `.github/workflows/ci.yml` today has **zero `needs:` edges** — every lane is
independent. "The violation fails before any adapter test runs" is therefore a new
structural claim, and asserting lane order in CI config is the brittle way to make it. The
elegant way is dataflow: the conformance gate produces the contract-stamped jar artifact
the adapter lanes consume. Red gate → no artifact → adapters cannot start. Ordering
becomes a fact about the graph, not a rule someone can reorder.

That reframing also dissolves the double-owned criterion #524's own review comment
escalated (#687 AC-2 ↔ #531/#724 AC-3, both citing CAP-21 AC-2): with a dataflow gate
there is no separate "ordering guarantee" to own, because the ordering is not an assertion
anyone writes. #531 then owns only the four-way parity fixture, which is what it is for.

## Scope note: build the mechanism generic, instantiate it for the CLI

JLS has four documented stability contracts (batch interface, `.jls` format, simulation
semantics, and the VCD profile inside the first). CAP-21 Open Question 2 asks whether the
CLI is "the first formally frozen public interface"; §6 and file-format §9 say plainly
that it is the *third*. The manifest-plus-classifier shape costs almost nothing to make
contract-agnostic and would retire the hand-run "Stability contracts touched" prose ritual
across `docs/standards-adoption/`. Instantiate it for the CLI now — but do not let CAP-21
mint a bespoke governance regime for one surface while the older two keep theirs.

## First real test case, and one thing the issue gets exactly right

The ratchet's first exercise should not be a hypothetical changed exit code. It should be
the **known deviation already documented in `docs/batch-interface.md` §1**: test-file
parse errors print to *stdout* and exit 1 (`TestGen.specError`). That is a real bug the
project will want to fix, it is observable, and it is frozen the moment #686 lands. Under
the reframe it is contract-2 behavior with contract-1 preserving the wart — which proves
the policy admits progress far better than a synthetic seeded violation does.

And the issue's central instinct — that *ordering matters as much as existence*, that four
adapters must never chase a failure whose cause is upstream — is exactly right, and is the
sentence I would keep verbatim.

## Cost of the reframe

Emitting two contract versions is real maintenance, and I would cap it: at most two live
majors, the older one pinned solely by its retained goldens (which #686 produces anyway,
so the marginal cost is a flag branch, not a second implementation). The manifest generator
is perhaps a day's work on top of machinery `CliFlagTableTest` already contains. Net, this
is not more work than the issue as written — it is the same work with the enforcement moved
from prose into the suite, which is where everything else in this repository already lives.
