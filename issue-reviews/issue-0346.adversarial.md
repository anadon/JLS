# Issue #346: FEAT-045: a drawn circuit makes a sound and hears one — host audio in and out, with no solver, and deterministic in CI
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

A well-specified feature (in-tree WAV sink/source elements, file-mode-only
determinism, an invocation-time device grant) undermined by three things a
senior reviewer would block on: it silently proposes changing a documented
*stability contract* rather than the CLI surface it claims to be additive
to; its own concurrency plan ("safe for concurrent agents" with #324) has no
policy document to converge on yet, despite the issue text asserting one
"can be written before either starts"; and its cost/throughput numbers cite
planning documents that do not exist anywhere in this repository.

## Findings, most severe first

**1. The grant-naming criterion contradicts a documented stability promise it never names as in scope.**
Criterion 3 requires the invocation-time grant to be "named on the run's
outcome line," and Integration Criterion 3 requires a run granting both the
console door (#324) and the audio door to "name both on **one** outcome
line, in one format, from one mechanism." But the outcome line is not a
free-form status message — it is a frozen, tested format:
`src/jls/sim/BatchSimulator.java:562-571` (`displayOutcome`) prints exactly
`<reason> at <time>` where `<reason>` is one of four fixed strings, and
`docs/capability-roadmap/sweep-04-verification.md:476-478` independently
records that `docs/batch-interface.md` §3 defines "an outcome line with
exactly four frozen reasons in precedence order" and calls a change to it
"a change to a **stability promise**." Neither §3 (Interface & Data
Contract) nor §4 (Global Invariants) nor the Completion Criteria of #346
lists `docs/batch-interface.md` §3.1 among the documents that must be
amended — only `SECURITY.md`, `docs/vcd-interop.md`, and the *flag table*
half of `docs/batch-interface.md` are named. A contributor who ships
exactly what's asked (append grant text to the existing outcome line) either
breaks a documented stability contract silently, or the feature can't
actually satisfy its own Integration Criterion 3 without a format change
this issue never scoped or costed. **Recommendation:** either (a) scope and
cost the outcome-line format change explicitly and reconcile it with the
frozen-format language in `docs/batch-interface.md` §3.1, or (b) record the
grant separately (a new stderr/status line, not the stdout outcome line)
and correct criterion 3 and Integration Criterion 3 to match.

**2. "Safe for concurrent agents" is asserted for a coordination problem that has no shared artifact yet.**
§6 states #346 and #324 are "mutually independent, safe for concurrent
agents... they must only agree on the one-page grant policy, which can be
written before either starts." But that one-page policy is Open Question 1
here, unresolved, and #324's nearest analogue (D7, quoted secondhand from a
document — see finding 4) is a paragraph, not a page, and doesn't specify
the outcome-line mechanism, the ratchet's exact name/idiom, or how two
simultaneous grants are jointly rendered (finding 1). §7's mitigation is
purely reactive: "whichever lands first defines the mechanism... a
`REPLAN:` comment" after the fact. Telling two independent workstreams they
may proceed concurrently while the thing they must agree on doesn't exist
yet is an invitation to build two grant models and two ratchets and
discover the conflict only at whichever issue closes second — exactly the
"one door, not two" failure the issue spends several paragraphs warning
against. **Recommendation:** file and land the one-page host-door policy
(named in the DoD as a bullet, with no owning task) as an actual
prerequisite issue with a `blocked_by` edge on both #346 and #324, rather
than trusting parallel execution to converge.

**3. The cost basis and throughput figures cite documents that do not exist in this repository.**
§Cost's 5-7 maintainer-week band is attributed to "the analog
determination's stage S0... measured throughput ceiling of about 209,000
samples/s... Audio input was measured at about 0.84 s of Java per second of
audio," sourced to `11-analog-determination.md` and (in the absorbed
TASK-0096 material) `spice-jls-integration.md` §6.5. Neither file exists
anywhere under `/home/user/JLS` (`find` over the whole tree and `docs/`
listing both come up empty; the only hits for "analog-determination" or
"209,000" in the tree are *other* issue-review output files, not source
documents). The grant-model quotation attributed to "`docs/plan/evidence/BRIEF.md` §12 D7" is in the same position — no `docs/plan/` directory
exists in this checkout at all. The issue presents these as measured,
citable facts ("Basis:", "measured", with a specific ratio arithmetic in
§Cost), but a reviewer working from this repository cannot verify a single
one of them. **Recommendation:** either commit the cited planning documents
(even as a stub with the extracted figures) or strip the "measured"
framing and mark the cost band and throughput margin explicitly as
inherited/unverified pending that document's availability.

**4. Evidence-commit citations are unverifiable in this checkout (shallow-clone caveat, not proof of error, but load-bearing).**
`evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` and the D7
decision commit `3a81a4a7d6a0f108ec201e632732d308cc02b3fc` do not resolve
(`git cat-file -t` fails for both) against this checkout, which is shallow
(`git rev-parse --is-shallow-repository` → true), so this is inconclusive
rather than damning by itself. But essentially every falsifiable claim in
§2 ("Scope verified ABSENT," the `git grep` transcripts, the D7 quote) rests
entirely on these two hashes, and the issue offers no permalink or blob URL
as a fallback. Given finding 3, the pattern is consistent enough to be
worth a flag: verify both commits resolve on the default branch, or replace
short hashes with permalinks, before treating §2's evidence as settled.

**5. Invariant 3's "same mechanism as the console door" dependency is asymmetric and likely to be built unilaterally.**
Invariant 3 requires "a golden produced with live audio granted is refused
by CI — the same rule and the same mechanism as for a live console
transcript" (i.e., #324's TASK-0069). But #324's own `blocked_by` is
`[316, 330, 354]` — three real feature dependencies — while #346's is `[]`.
Both issues' own sequencing sections predict #346 lands first (§6 here:
"critical path: TASK-0096 alone... nothing upstream of it"). If that
prediction holds, #346 must invent the CI-refusal ratchet from nothing,
and #324 is the one that later "conforms" — which is the reverse of how
§7's re-planning protocol frames the risk ("#324 lands first with a
different grant model. Then this feature conforms to it"). The one-sided
framing under-prepares whoever picks up #346 for the fact that they are
very likely setting the actual precedent, not following one.

**6. A known-mandatory build step for new elements is missing from every task list, prediction, and DoD line.**
`ARCHITECTURE.md`'s "Adding an element today" list (items 14-15) states a
new palette element needs "a help page under `resources/help/elements/**`"
and "a topic in `resources/help/Map.jhm`, an entry in
`resources/help/JLSHelpTOC.xml`, and the palette list in
`test/jls/HelpTopicsTest`" — and that "the completeness test will fail
until the topic exists." `test/jls/HelpTopicsTest.java:145-167` confirms
this is a real, currently-passing completeness ratchet over every palette
element. Neither #346's scope items/DoD nor #462's Predictions/Materials/
Method/DoD (which otherwise enumerates SpotBugs exclusions,
`SaveTagsTest`, `PaletteContractTest`, and the `HdlExporter` bucket in
granular detail) mentions help topics anywhere. `mvn verify` is listed as a
green-bar requirement in both issues' invariants, but as scoped, landing
`HostAudioSink`/`HostAudioSource` as palette entries makes that red until
someone independently notices and writes the two help pages. **Recommendation:**
add help-topic authorship and `HelpTopicsTest` as an explicit checklist item.

**7. Internal contradiction on the extension-points catalog row, resolved only in a comment a body-only reader would miss.**
The issue body's §1 states "the audio door belongs in the same catalog row
set as the byte port's, or that catalog is not total" (hedged as
belonging to #223, but stated as a should). The absorbed TASK-0096 comment
(2026-08-04) flatly reverses this: "**No `docs/extension-points.md` row is
added — this is not a seam.** That last is a correction to how §1's
relationship to #223 might otherwise be executed." Meanwhile #324, the
"closest relative" this issue insists shares one policy, explicitly *does*
add a catalog row for the byte port (§3: "`docs/extension-points.md` gains
the host-port row"). So the two "same governance question, same grant
model" doors end up asymmetric in the one artifact (`docs/extension-points.md`) that's supposed to make host doors enumerable — and the correction
lives three comments deep rather than in the issue body itself. A
contributor reading only the body would add a row that a later pass would
have to remove.

## What's solid

- The file-mode-only determinism boundary and the confinement-ratchet plan
  (copying `test/jls/SocketConfinementRatchetTest.java`'s "mentioning vs.
  constructing" idiom, verified real at `test/jls/SocketConfinementRatchetTest.java:33-44`) is a genuinely reusable, already-proven pattern in this
  codebase — good reuse, not aspirational.
- The registration-tax numbers are accurate: `LogicElement.java:17-21`
  really does have exactly 24 sealed permits and `ElementRegistry.java:39-77` really does register exactly 35 types, so the "~70-line registration
  tax" framing is grounded rather than hand-waved.
- The 44.1 kHz / decimal-tick-lattice arithmetic (`44100 = 2²·3²·5²·7²`,
  hence no exact decimal-tick expression) is correct and is a genuinely
  useful thing to put in front of a student before they file a bug about
  perceived pitch drift.
- Scoping "no solver, no drawn DAC, no host byte port" out, and citing the
  specific boundary each belongs to, is well-reasoned and matches the
  actual absence of `javax.sound`/`System.in`/`ProcessBuilder` in `src/`.

## Net assessment

The technical design (file-mode-first, integer-only PCM, one resampler
reused verbatim) is sound. What isn't sound is the process scaffolding
around it: a load-bearing dependency on a stability-frozen output format
that's never named as such, a concurrency plan resting on a policy document
that doesn't exist, and cost/evidence citations to files absent from the
tree. None of these require re-scoping the feature — they require the
issue (or a REPLAN comment) to name and resolve them before TASK-0096
starts, which is exactly the discipline the issue otherwise holds itself to.
