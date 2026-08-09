# Issue #400: TASK-0085: a versioned schema says what a chip is and a drawn element can say which chip it becomes — data on the classpath, no geometry, no Java to extend it
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

A well-structured task spec for a new `src/jls/pkg/` package (part schema,
`.parts` grammar, `-parts` flag, realization policy) that is a child of
feature #349. The prose discipline (falsifiable hypotheses, negative
controls, an explicit falsification table) is real strength. But the
issue's own evidentiary basis has a confirmed, acknowledged defect, one of
its five "blocking" open questions is not actually closed on the record it
points to, and two of its core acceptance predicates (P2/P3, the "total
realization policy") are modeled on code that does not exist on `master`.

## Findings, most severe first

**1. (Confirmed by the repo's own maintainer, not just by me) O4's central evidence is fabricated relative to `master` — the pattern P2/P3 are supposed to copy does not exist.**
O4 claims: *"The realization policy here is the same shape over the same 35
registered types... with `classifiedElementClasses()` as the accessor its
totality test reads"* and cites `src/jls/hdl/HdlExporter.java:428-495`. I
verified directly: `grep -rn classifiedElementClasses src/ test/` returns
zero hits anywhere in the tree, and `HdlExporter.java` (1364 lines) defines
only three classification sets — `EXPORTED` (:422), `SKIPPED` (:431),
`TOPOLOGY` (:436) — each `private`, with no public accessor and no
totality test. The union of the three covers 21+6+4=31 classes; `Memory`,
`RegisterFile`, `FieldExtend`, and `SubCircuit` (4 of the 35 registered
types) appear nowhere in `HdlExporter.java` at all — they are rejected by
a fallback path, not classified by a named, exhaustive bucket. Issue #493
(filed by the repo owner against this same sweep) independently confirms
this exact gap: `classifiedElementClasses()`, the `REJECTED` map, and
`exportPolicyIsTotalOverTheElementRegistry()` are **branch-only** code that
never merged to `master`; issue #400 is explicitly named in #493 §5 as one
of the 29 issues that "quotes or relies on branch-only code." A one-line
comment was left on #400 acknowledging this, but the issue body itself is
unedited and still asserts the pattern "ships" as precedent. **Recommendation:** before work starts, either (a) rewrite O4/§Materials to
point at the real precedent (the three-bucket `EXPORTED`/`SKIPPED`/`TOPOLOGY`
split with an implicit catch-all, which is *not* total-with-named-reasons),
or (b) make TASK-0085 explicitly dependent on #492 (the issue that actually
proposes the `REJECTED`/totality mechanism) landing first, since P2/P3 as
written assume a mechanism this task would otherwise have to invent from
scratch under a different name — which is scope creep this task doesn't
budget for.

**2. `blocked_by: []` is asserted but contradicted by the issue's own §8 first line.**
The machine block states `blocked_by: []` with the comment "nothing
precedes this: the schema can be authored before any consumer." But §8
Method's very first checklist item is: *"Answer #349 Open Questions 1 and
5 first... #349 marks both as **blocking the filing of this task**."*
Open Question 1 (shared pins) and Open Question 5 (footprint column
present from entry one) are both marked, on #349 itself, as items that
"Blocks execution" here in #400 too (see §Open Questions items 1 and 2,
each ending "**Blocks execution.**"). A ratification that hasn't happened
is a real blocking dependency, whatever the YAML says. The issue's move —
"filing proceeded... because withholding the issue pending a decision the
issue exists to frame is the circularity D10 forbids" — is a self-serving
resolution: it lets the task be filed as unblocked while its own
completion criteria (§14, item 1) still require "#349 Open Questions 1 and
5 ratified... before implementation starts." That's two different
gating states for the same two decisions, in the same document.
**Recommendation:** either set `blocked_by: [349]` honestly, or strike the
"blocks: []" framing and replace it with "implementation blocked on two
named ratifications, tracked here" so a picker-upper doesn't start coding
against an assumed default that could still flip to option (b).

**3. H1's "recommended default" (shared pins) is adopted mid-document without being ratified, and the predicate P11/§7.10 self-consistency check is stated in two mutually exclusive forms.**
§7.10 gives the *strict* disjointness predicate as the one "asserted over
every shipped entry," then in the very next paragraph says if shared pins
are admitted "it becomes" the relaxed predicate — but doesn't say which one
ships. §10 (Falsification) then treats H1's refutation as a live
possibility requiring "a schema version bump." So the schema shape that
Predictions P1–P11 are supposed to validate is genuinely undecided at
filing time, yet H1 and P1 are written as if the shape is fixed. A
contributor picking this up has to make the shared-pins decision
themselves before they can write a single test, despite the issue
presenting it as settled ("Filing proceeded"). **Recommendation:** resolve
Open Question 1 for real (a one-line "shared pins: yes/no, `shared`
keyword syntax: `<pin>*`") before assigning this to an implementer, or the
first PR review cycle will re-litigate the schema's most consequential
decision.

**4. The realization-policy totality claim (H3/P2, "35 registered types") is real but the test target is underspecified in a way that's gameable.**
I confirmed 35 registered types in `ElementRegistry.ALL`
(`src/jls/elem/ElementRegistry.java:38-77`) — that count is accurate on the
checked-out tree, so P2's premise is sound. But P2's actual assertion —
"every one of the 35 registered types lands in exactly one bucket... a
type in none fails the build" — can be satisfied by a `NO_DEFAULT_REALIZATION`
bucket that swallows every type that isn't a `74xx`-mappable gate (i.e.
`Memory`, `RegisterFile`, `SubCircuit`, `Display`, `SigGen`, `Pause`,
`Stop`, `Text`, `TestGen`, `WireEnd`, `JumpStart`, `JumpEnd`, `Clock`,
`FieldExtend`, ...) with a boilerplate reason string, trivially passing
"total" and "every entry names what a `.parts` row would supply" (P3)
without a human ever checking whether those reasons are actually
actionable. P3's own verification method (§9) just says "the reason string
is actionable, never 'unsupported'" — that's a string-content assertion an
implementer can satisfy with `"a .parts row does not exist for " + type`
for all 20+ non-gate types, which is not meaningfully more actionable than
"unsupported" was. **Recommendation:** either name a minimum number of
buckets (the HdlExporter shape has 3-4; a part-realization policy
plausibly needs more granularity — e.g. distinguish "never a discrete
part" (Wire/Text/Display) from "could be a part someday" (Memory,
RegisterFile)) or require each `NO_DEFAULT_REALIZATION` reason to name a
concrete missing schema feature, checked by a test that greps for banned
generic phrases, not just the literal word "unsupported."

**5. Internal contradiction: "no interpreted electrical figure" invariant vs. the Electrical struct having units and a "vacuous fan-out check" flag.**
§7.4 says `Electrical` carries "drive capability and input load... plus a
flag for families where the DC fan-out check is vacuous." A "vacuous
fan-out check" flag is itself an interpretation decision about what the
electrical figures *mean* semantically (which families' DC checks don't
apply) — that's exactly the kind of judgment §7.12/P10 forbids ("no pass
may interpret them until the strength model (#341) exists"). Storing a
boolean that pre-judges applicability of a not-yet-built check is a soft
violation of the stated invariant, even if literally "no non-test reader"
exists yet. **Recommendation:** either drop the vacuous-check flag from
this task's schema (defer it to TASK-0088, the declared consumer) or
explicitly carve it out of P10's "no interpretation" claim with a
one-line justification, because as written a reviewer could plausibly
argue P10 already fails at schema-design time.

**6. Feasibility: acceptance criteria size is large relative to a "2-week" task, and cost figures live entirely in an unreachable planning corpus.**
§349's cost section cites `docs/plan/evidence/capstone-plan.md` for the
"TASK-0085 at 2 wk" estimate. That document is one of the 195 planning
files issue #493 §2 identifies as **absent from `master` and
unrecoverable by re-reading** ("docs/plan/**... 195... Unrecoverable by
re-reading. These files never existed on `master`."). So the entire cost
justification for "2 weeks" cannot be checked or re-derived by anyone
working from this checkout — it exists only as prose quoted inside issues.
Meanwhile the Definition of Done (§14) lists 22 checklist items spanning a
new package, a new grammar, a new CLI flag with documented resolution
order, 11 predictions, 5 falsification checks, a JaCoCo floor, and 5 open
questions to close — for a package whose own precedent (`Boards.java`)
is 90-odd lines. That is plausible for 2 weeks only if the schema
questions (finding 3) are resolved before the clock starts; as filed, the
implementer inherits that design work inside the estimate.
**Recommendation:** treat the "2 wk" figure as unverifiable and flag it
for re-estimation once Open Questions 1/2/3/4 are actually closed.

**7. Gameable acceptance criterion: P6 ("unknown SCHEMA version yields zero entries") is judged "on the count," which a lazy implementation can satisfy by refusing *every* version, not just unknown ones.**
§9 states P6 "requires zero entries, asserted on the count, not on the
presence of a message." An implementation that returns zero entries for
*all* inputs (a parser stub that never actually loads anything) passes P6
as stated, and would also vacuously make P4/P5 (which operate on "a file")
harder to fail in a way distinguishable from "the parser never worked."
The predictions don't cross-check that a *known*-version file with no
errors returns a *non-zero* count as a companion assertion to P6 — P1
covers the general "observe a `PartPackage`" case but isn't explicitly
tied to P6 as a paired positive control. **Recommendation:** add an
explicit P6b: "a file declaring the current understood SCHEMA version
with zero malformations parses to a non-zero, exact-expected entry
count," run in the same test as P6, so an over-eager rejector can't pass
both P1 and P6 independently while still being broken on the version
comparison itself.

## What's solid

- The `-parts` override/extend resolution formula (§7.10, P7) is precise,
  unambiguous, and testable as written — no notes.
- The "no geometry" scope boundary (H2, P9, invariant §7.12) is clearly
  stated, has a concrete refutation trigger, and is checked by both a
  positive prediction and a named architecture-test intent.
- The provenance-totality requirement (P10... actually §7.10 "Provenance
  totality") is simple and unambiguous: every entry has non-null
  provenance, test-asserted rather than reviewed.
- Reuse of the `PinBindings` aggregated-error-reporting idiom (O3) is a
  real, currently-existing pattern (verified at
  `src/jls/hdl/board/PinBindings.java:38-83`) and is a sound thing to
  copy — unlike O4's, this citation checks out.
- Migration/compatibility story (§7.12: binding is optional state,
  historical files load unchanged, schema versioned independently of
  `FORMAT`) is coherent and low-risk.

## Net assessment

The task is conceptually sound and the author is aware of at least one of
its own defects (the branch-only-evidence comment is already posted). But
"needs-rework" rather than "sound-with-concerns" because: two of the
eleven predictions (P2, P3) are explicitly modeled on a mechanism
(`classifiedElementClasses`/`REJECTED`) that does not exist on `master`
and is proposed by a *different*, unrelated issue (#492) that this task
does not depend on in its `blocked_by` list; and the "blocked_by: []"
claim is inconsistent with the issue's own §8/§14 gating language on two
named open questions. Both should be fixed in the issue body — not just
patched with a drive-by comment — before an implementer picks this up.
