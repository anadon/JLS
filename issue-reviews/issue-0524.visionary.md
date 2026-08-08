# Issue #524: FEAT-C21-1: the headless CLI stops being observed behavior and becomes a frozen, versioned promise — invocation, exit codes including status 3, artifact paths and xUnit schema, held by a conformance suite and a compatibility ratchet
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the CAP-21 vocabulary away and the purpose is one sentence: **four platform
adapters must be able to depend on JLS grading output without reading JLS's source
or guessing.** That purpose is right, it serves the project's stated arc
(`docs/grand-architecture.md:36-38` calls the headless surface a *co-equal front
end*, not a side door), and nothing else in the tree owns it.

But the title's premise — that the CLI "stops being observed behavior" — is false at
HEAD, and the falseness is load-bearing, because it determines what the issue thinks
it must build.

- `docs/batch-interface.md:3-11` already declares itself **"normative, and a
  stability contract"**, with §6 spelling out the ratchet policy in full: any
  observable change requires a CHANGELOG entry **and** a major bump **or** a
  compatibility flag. That is semver-plus-deprecation-window, already written,
  already applied.
- It is already held executably: `CliFlagTableTest` (flag table vs. `usage()` vs.
  parser), `CliSmokeTest` (streams and exit statuses), `BatchSimulationGoldenTest.
  watchedElementsPrintInNameOrder` (stdout, byte-exact), `VcdExportGoldenTest`
  (§4 byte-exact, *and* re-parsed "from this document rather than from the
  emitter"), `TellUserBatchContractTest` (stderr discipline).
- The project has shipped this exact idiom three times over (`docs/file-format.md`
  + `FileFormatSpecTest`, `docs/simulation-semantics.md`, the batch interface) and
  carries seven `*RatchetTest` classes. Freezing an interface is not a new
  capability here; it is house style.

And the *content* of AC-1 and AC-4 is already inside #466. Read #466 §7.1 and
§7.12: it adds the status-3 row to §1, adds §2.5, specifies the xUnit report with
**no `timestamp`, `hostname` or `time`**, golden-pins it (`GradeReportGoldenTest`),
adds `CliFlagTableTest` rows for `-check`/`-report`, and writes the CHANGELOG entry
under §6's promise. When #466 lands, "the contract document specifies invocation,
every exit status including status 3, and the xUnit schema" is *done*, and so is
"no timestamps, ordering, or locale nondeterminism in verdict output."

So the honest residue of #524 after its own prerequisite is three things:

1. a **queryable contract identity** (AC-5) — genuinely absent, genuinely valuable;
2. a **doc-derived** conformance test rather than a hand-written one (part of AC-1);
3. the **seeded-violation transcript** (AC-2).

"Artifact paths" is close to vacuous: every artifact path in the grammar is an
explicit operand (`-report r.xml`, `-vcd f`, `-export f`), and the one defaulted
path (`-i` → `circuit_file.png`) is not on any grading path. Freezing it costs a
clause and buys nothing.

That is a much smaller, much sharper issue than the one filed — and the reframings
below make it smaller still while making it do more.

## Reframing A (headline): freeze the artifact, not the process

The issue cuts the seam at the **process interface** — argv shape, exit statuses,
paths. But look at what the four adapters actually consume, per CAP-21 §1: one
xUnit file and one exit status. And CAP-21 **AC-4** is explicit that every verdict
on every platform must derive **from recorded batch artifacts, with no adapter
opening a live session** — nbgrader's whole PF-5 witness is "grade from a file JLS
already wrote."

Under that constraint, AC-5 as written is the wrong shape. *"The contract version is
queryable from the CLI itself"* requires **invoking the binary** to learn what the
binary promises. An nbgrader hidden cell holding only `results.xml` cannot ask. A
Gradescope container that received a pre-built artifact cannot ask. The version
query and the artifact travel separately and can therefore disagree — which is
precisely the failure mode AC-5 exists to prevent.

**Put the contract version inside the artifact.** The report's root element carries
it (`<testsuites jls-contract="1" …>`, or a `<properties>` entry if strict xUnit
consumers matter), and the CLI *also* prints it for symmetry. Consequences:

- **AC-5 gets strictly stronger** and works offline, in a notebook, from a tarball.
  A consumer that holds the artifact holds the promise.
- **AC-1's four-way parity claim collapses to a byte-diff of one file.** All four
  adapters read the same bytes; there is no per-platform re-derivation to drift.
- **The CLI stays free.** Invocation is the least stable and least valuable surface
  to freeze — every adapter wraps it in its own `run_autograder`/Action/`externalGrader`
  shim anyway — and it is exactly the surface that must keep growing (`-check`,
  `-report` now; `--serve` in the lf-07 roadmap). Freeze the outputs and the
  statuses; keep invocation **additive-only** under §6's existing "additions that
  cannot break a conforming consumer" clause, and do *not* freeze the argv grammar
  as a closed set.
- **The conformance suite gets a real definition**: a checked-in schema (XSD or
  RELAX-NG) for the report plus the §1 status table. "Each clause exercised by a
  test" stops being a review claim and becomes schema validation.

Note the asymmetry this exposes: JLS has **no CLI version flag at all** today.
`JLSInfo.versionString` is single-sourced from the pom (`src-filtered/jls/version.properties`)
and surfaces in the About dialog, the window title, and the crash handler
(`About.java:26`, `JLSStart.java:1281`, `DefaultExceptionHandler.java:218`) — never
on stdout. A headless caller cannot learn *which JLS* it just ran. That is a
one-line-of-scope gap worth fixing here regardless of which reframing wins.

## Reframing B: use the `FORMAT 1` precedent; do not mint a second semver

AC-3 asks for "a written versioning policy (semver + deprecation window)" specific
to the CLI contract. The project already has two version namespaces: product semver
(`pom.xml:11` → `5.0.5-SNAPSHOT`, CHANGELOG §semver) and the save-format integer
(`docs/file-format.md` §4, `FORMAT 1`, with documented negotiation rules). A third,
CLI-specific semver stream is one namespace too many, and it re-litigates a policy
§6 already settled.

The elegant move is to copy the format-header precedent exactly:

- a **monotone integer**, independent of product version — `BATCH-CONTRACT 1`;
- **compatibility = §6's existing escape**, verbatim: a breaking change bumps the
  integer *and* ships a flag preserving contract *N-1*. That *is* the deprecation
  window; it needs no new prose.
- an adapter pins the integer and refuses a mismatch by name. Integer comparison,
  not semver range logic, in four different adapter languages.

This also disposes of **KC-21-4** (the kill criterion where the freeze blocks
FEAT-053-lineage evolution). Under an integer + compat-flag rule, verdict evolution
is a bump, not a negotiation — and #466 has already proved the pattern works, since
its entire new observable surface is gated behind `-check` so that the three
existing goldens pass **unmodified**.

## Reframing C: build one adapter *before* the freeze — I am disregarding CAP-21's PF1→PF2 ordering

CAP-21's graph runs `PF1 --> PF2..PF6`: freeze first, adapt after. I think that is
backwards, and CAP-21's own Cost section agrees with me — its demo slice is *"PF-1
draft contract + PF-2 Gradescope template **over the existing three-exit-status
behavior**."*

**Freezing an interface with zero external consumers is how you freeze the wrong
clauses.** Nobody has yet written a Gradescope adapter against JLS, so nobody knows
which clauses an adapter leans on and which are decoration. The evidence that this
guess goes wrong is already in the tree: `examples/autograde/autograde.py` pins
`EXPECTED_STDOUT_LINES` — someone built a consumer against the most fragile
observable available, because nothing better was frozen *and nothing told them which
part mattered*.

Concrete inversion: ship the Gradescope adapter (#525) against the un-frozen
contract as soon as #466 lands. Have it emit, as a build artifact, the list of
contract clauses it actually depends on. #524 then freezes **exactly that set**, and
the conformance suite is derived from a real consumer's dependency list rather than
from a careful reading of a markdown file. The freeze arrives one adapter later and
is worth an order of magnitude more.

If the maintainer keeps the filed ordering, the mitigation is cheaper but weaker:
mark every clause in the frozen document `frozen` or `provisional`, and let the
first adapter promote clauses from provisional to frozen.

## Reframing D: the conformance suite should be a doc-drift test, not a new apparatus

AC-1 names `CliContractConformanceTest` — a new, hand-written suite. The project's
own idiom is better and is sitting right there: `FileFormatSpecTest` *parses*
`docs/file-format.md` and checks its claims against real output; `VcdExportGoldenTest`
re-checks structure "with a parser written from this document rather than from the
emitter."

Do the same and name it `BatchInterfaceSpecTest`: parse §1's exit-status **markdown
table** out of the document, drive the CLI to produce each row, assert status and
stream placement. Then:

- "each clause is exercised by a conformance test" is **mechanically true**, not
  asserted in a review comment;
- **AC-2 falls out for free** — change an exit code and the doc-derived table goes
  red without anyone seeding anything by hand, and the transcript is the CI log;
- JLS does not grow a sixth parallel contract-checking apparatus alongside
  `CliFlagTableTest`, `CliSmokeTest`, `FileFormatSpecTest` and the goldens.

The disputed AC-2/#531-AC-3 split (resolved in the second comment) also dissolves
partly: with a doc-derived table there is no bespoke "seeded violation" fixture for
two issues to fight over — #531 owns lane ordering, and the violation is a one-line
scratch-branch edit anybody can reproduce.

## Alignment with the larger arc — one real pull against it

CAP-21 Open Question 2 asks whether this is "the first formally frozen public
interface of JLS" and recommends **yes**. At HEAD that is simply false —
`docs/batch-interface.md` and `docs/file-format.md` are both frozen public
interfaces with enforcement tests. More importantly, `docs/capability-roadmap/lf-07-api-and-platform.md:14-16`
says of the batch freeze: *"the project conceding the principle and then stopping at
the smallest possible instance of it."* lf-07 goes on to design `jls.api` and a
`jls --serve` NDJSON protocol, each with its own normative doc **"under the identical
promise `docs/batch-interface.md` §6."**

So #524 risks canonizing the one-verb CLI as *the* extension point at the exact
moment the roadmap wants to move past it, and — worse — building a CLI-specific
ratchet regime that `docs/api-interface.md` will then have to duplicate.

**Resolve Open Question 2 as "no," and make the ratchet a project asset rather than
a CAP-21 asset.** The deliverable is a single generalized interface-stability policy
(§6, lifted to its own short doc) plus a *reusable* spec-drift test harness that
`batch-interface`, `file-format`, and later `api-interface` all instantiate. Same
work, one-third the eventual surface, and it strengthens the arc instead of pulling
sideways on it.

## Verdict

**endorse-with-reframing.** The end is right and unowned; keep the issue. Change
four things: freeze the **artifact** and its embedded contract integer rather than
the process shape (A); reuse `FORMAT`'s integer + §6's compat-flag rule instead of
minting a second semver (B); let one real adapter tell you which clauses matter
before freezing them (C, which contradicts the capstone's PF1→PF2 edge deliberately);
and implement the conformance suite as a doc-drift test in the house idiom rather
than a new apparatus (D). Drop "artifact paths" from the frozen surface, and add a
plain CLI version flag, which the tree lacks entirely.
