# Issue #660: TASK-C566-4: the FSM analysis the assessment says should be scriptable is callable with no display present
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Strip the YAML and the citations and #660 says: **a grader should be able to ask
JLS questions about a state machine without a display.** That is the right
ambition and it is exactly on the project's arc — `jls.sim` is headless by
construction (#77, `HeadlessCoreRatchetTest`), the container image ships
batch-only, and `docs/capability-roadmap/lf-04-formal-and-grading.md` is an
entire document about JLS learning to answer questions rather than merely
replay events.

But the issue's own guess at *which* questions — "state/transition listing,
output-mode reporting" (Outcome, and AC-1/AC-2) — is the least valuable answer
available, and two of the four acceptance criteria describe work that either
already shipped or should never be a task. I am disregarding AC-1 through AC-4
as written. The reasons are below, and so is what I would put in their place.

## AC-1/AC-2 are already satisfied by a surface that is already frozen

A byte-deterministic, machine-readable listing of a state machine's states,
outputs and transitions **exists today** and is normatively specified:

- `docs/file-format.md:319` gives the `StateMachine` record grammar —
  `state`/`output`/`trans`/`next` items, sequence significant.
- `docs/file-format.md:405-415` (issue #180) pins the *canonical order*: state
  sub-records sorted by state name with grid position as tie-break; transitions
  unconditional first, then `else`, then conditionals by (signal, eq flag,
  value, bits), tie-broken by next state's name. `State.getTransitionsInSaveOrder`
  (`/home/user/JLS/src/jls/elem/State.java:339`) is that comparator.
- `jls -savetext out.jls circuit.jls` (README, "Command-line options") writes
  it uncompressed, headlessly, with no display, to a path the caller names.

So AC-1 (callable with no windowing system), AC-2 (machine-readable,
byte-deterministic for a given circuit) and AC-4 (documented alongside the
batch flags) are met by `-savetext` plus `docs/file-format.md` §5/§8. Building
a second FSM report to restate the same facts creates a **second normative
description of the same data**, under a contract (#524) that is at this very
moment being *frozen*. Two frozen descriptions of one structure is a divergence
bug with a schedule attached.

AC-3 is stranger still: "a capability the assessment explicitly marked as
GUI-only is *not* exposed here." That inverts JLS's own recorded discipline.
The project does not decide per-capability which side of the display boundary
to expose; it decides **where the code lives** and lets the boundary fall out
(#77, ARCHITECTURE.md "Module layout" — `Simulator`/`BatchSimulator` import no
AWT and a ratchet test enforces it). A capability is GUI-only when it is a
*rendering* of something, not when a document says so. Framed that way, AC-3
stops being an acceptance criterion and becomes a placement rule the existing
ratchet already enforces.

## What a grader actually cannot do today

Two concrete gaps, neither named by #660, both worth more than the listing:

**1. The FSM's state is invisible in every headless output.** `StateMachine`
does not implement `Watchable` — `grep -n 'watch' src/jls/elem/StateMachine.java`
returns nothing. `BatchSimulator.findWatched` (line 220) only registers
`Watchable` elements, so a state machine appears in neither the stdout report
(`docs/batch-interface.md` §3.2 whitelists `Register`, `Memory`, `OutputPin`)
nor the VCD (§4.1, "one signal per watched element"). A grader can see the
machine's *output pins* and nothing else. This directly weakens the sibling
task #659: a Moore machine and a mis-stated equivalent that happen to agree on
outputs produce identical traces, and #659's harness cannot tell them apart.

Fix: make `StateMachine` implement `Watchable`, emitting the current state name
(or its ordinal) as a VCD signal. **Zero new flags.** It flows into the frozen
§4 VCD profile as an addition that no conforming consumer can observe today
(there is no such signal to break), and `VcdExportGoldenTest` stays green on
existing fixtures. One seam, inside the contract, instead of beside it.

**2. Nobody can ask whether the machine is well-formed.** The two properties
that matter pedagogically are *completeness* (every state has a matching
transition for every input) and *determinism* (no two conditions overlap).
JLS handles both at runtime, badly:

- Incompleteness surfaces as a once-per-run `TellUser.warn` from `react`
  (`src/jls/elem/StateMachine.java:772-780`, issue #98 S5) — discovered only if
  the stimulus happens to reach that state, and reported on stderr where the
  batch contract says diagnostics live, not as a verdict.
- Overlap is resolved silently by first-match-wins in `State.getNextState`
  (`src/jls/elem/State.java:1278-1302`), iterating `trans` in **insertion
  order** — which is load order, not the #180 canonical order. A machine with
  overlapping conditions can therefore simulate one way before a save/reload
  and another way after, with no diagnostic either time.

Both are decidable statically on the explicit state graph in milliseconds — no
BDDs, no state-space explosion, nothing on lf-04's tier-3 delegation list. Add
unreachable-state and dead-end detection (a graph reachability from the initial
state, which `checkInitialState` already identifies) and you have an analysis
that is genuinely *analysis*, that answers a question the .jls file does not
already answer, and that partially discharges lf-04's own honest caveat at
line 700-707 ("differs only in state 1011, which may be unreachable —
reachability is not checked"). That caveat is about the sequential equivalence
checker, but the FSM element's state graph is exactly the case where
reachability is cheap and need not be delegated.

## The architectural seam #660 is cutting along the wrong axis

CAP-31 (#515) AC-5 says *all* analysis is headless-callable. There are four
planned features: truth-table extraction, minimization, table→circuit
synthesis, FSM parity. If each PF spawns its own "make it headless" task in
#660's shape, JLS ends up with four bespoke flags, four ad-hoc output shapes
and four §-additions to `docs/batch-interface.md` — accreting onto
`JLSStart.FLAGS`, a table already carrying `-i`, `-export`, `-savetext`,
`-board`, `-pins` inside a 3,069-line class, while #524 tries to freeze it.

The better seam is one analysis surface, not N:

> `jls -analyze <analysis-id> [--format json] circuit.jls`, where analyses are
> **contributions to a declared extension point** rather than flags. The
> mechanism already exists and is already ratified: `jls.module.ExtensionPoint`
> constants, `ExtensionRegistry`, and `ExtensionPointCatalogTest` cross-checking
> the catalog in both directions (ARCHITECTURE.md, "Extension points: the typed
> seam catalog", #223, `docs/extension-points.md`). One flag, one result
> envelope, one exit-status story, one §8 in the batch document — frozen once by
> #524 with a *shape* rather than with N unrelated dialects.

Under that framing #660 shrinks from "design and freeze a public FSM report
interface" to "register the FSM analyzer at the analysis point, with these
three properties," which is genuinely the band-1 task the YAML claims it is.
And the truth-table PFs get their headless surface for free instead of each
filing this issue again.

The output-format question deserves deciding *once*, here, before four
consumers each pick: §3 stdout is line-oriented human-ish text, §4 is IEEE 1364
VCD, and #524 introduces an xUnit schema. A fourth ad-hoc dialect is the
default outcome if nobody chooses; JSON behind `--format json` with the
line-oriented form as default is the choice I would make, but the point is that
it should be made at the analysis-surface level, not inside an FSM task.

## Ordering: this task cannot honestly exist yet

#660 is `ordering_after` #657's assessment, which does not exist, and its own
boundary note concedes the task may close as a no-op with a citation. Three of
#566's four tasks are gated on a document nobody has written, yet all four are
filed with acceptance criteria and effort bands. Filing a task whose content is
defined by an unwritten document is how a roadmap acquires phantom work.

The redirect above dissolves that too: the FSM observability gap (#1) and the
well-formedness analysis (#2) are justified by JLS's *own* trajectory — #659's
harness needs the first, #98's runtime warning and the save/load hazard argue
the second — and neither waits on what Digital, DEEDS or Issie turn out to
ship. They are true regardless of the assessment's findings.

## Concrete alternative, stated as a replacement

1. **Close #660 as filed.** Cite `-savetext` + `docs/file-format.md` §5/§8 for
   the structural-listing half; it already meets AC-1/2/4.
2. **File "StateMachine becomes Watchable"** — one interface, current state in
   the VCD, `docs/batch-interface.md` §4.1 note, a golden. Do it *before* #659
   so its harness can compare state sequences, not just outputs. This is the
   whole of "FSM analysis is callable headlessly" that #659 actually needs.
3. **File "FSM well-formedness analysis"** — completeness, determinism/overlap,
   unreachable and dead-end states, computed in `jls.elem` (headless by
   placement, guarded by `HeadlessCoreRatchetTest`), consumed by both the
   `StateMachineDialog` and the batch surface. Note that today's dialog has no
   such check (`grep -i 'unreach\|ambigu\|overlap'` on
   `src/jls/edit/StateMachineDialog.java` finds nothing), so the GUI gains from
   it as much as the grader does.
4. **Hold the CLI shape open in #515, not here** — one `-analyze` point across
   CAP-31's four PFs, declared through #223's catalog, frozen once by #524.

If the eventual #657 assessment names some FSM capability that genuinely is
neither structure-in-the-save-file nor a state-graph property, that finding
files its own task. It should not be pre-committed to a flag today.
