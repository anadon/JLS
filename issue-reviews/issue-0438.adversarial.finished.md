# Issue #438: TASK-0012: a batch run can be asked to end when the queue drains, and the default ceiling stops being silent
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what checks out

All source citations were re-derived against current HEAD (`d6bc8dd`) and match exactly: `JLSInfo.java:69` (`defaultTimeLimit = 100000000`), `Simulator.java:38,104-107,217,230-233` (unguarded `setTimeLimit`, loop guard, post-poll clamp), `JLSStart.java:770-771,1061-1073,1204,1209` (`-d` FlagSpec, parse/reject order, the two `-d10000` usage strings), `BatchSimulator.java:562-571` (`displayOutcome` precedence chain), `InteractiveSimulator.java:78,550-556,401,657` (the field, the `int`-clamp comment, the two other `now >= maxTime` sites). `test/jls/BatchTimeLimitTest.java` does not yet exist, as claimed. `docs/batch-interface.md:22,51-52,334-336` quote correctly. Referenced issues #410 and #354 are open and their content matches what #438 attributes to them. The math in §7.10 (Stage 1-3) is correct and the H1 argument (loop guard unfalsifiable at `Long.MAX_VALUE`) holds. This is an unusually well-grounded issue; the findings below are about what it gets wrong or leaves dangerously open, not about fabricated evidence.

## Findings

### 1. (High) The stderr notice contradicts the batch interface's own normative stability contract, and the issue never proposes amending the clause it breaks

`docs/batch-interface.md` is headed "**Status: normative, and a stability contract**" and its exit-status table at line 38 states, as part of that contract:

```
| 0      | run completed    | results on stdout (section 3), stderr empty    |
```

The issue's P3/H3 design requires a run that exits 0 (the default-limit case, "Exit status is unchanged (0); this is an advisory, not an error") to write a line to stderr. That is a direct violation of the row the doc calls normative — not an addition a conforming consumer can ignore, but a promise ("stderr empty" on status 0) that becomes false. The issue justifies this by quoting a *different* sentence from the doc ("grading scripts should treat exit status, not stream placement, as the failure signal") — but that sentence is scoped to the "Known deviation" paragraph about `-t` test-file errors printing to stdout instead of stderr; it is not a general license to add stderr output on a zero-exit success path. §8's task list amends `docs/batch-interface.md:51-52` (documenting the `unlimited` operand) but never touches the exit-status table at line 38, so the deliverable as specified leaves the doc self-contradicting: one section says status 0 means empty stderr, another (implicitly, by shipping the feature) makes that false with no accompanying edit.
- **Recommendation:** Either (a) add an explicit amendment to the line-38 table (e.g. footnote: "advisory stderr may accompany status 0 when the default limit fires") as a required checklist item, or (b) reconsider the channel — e.g. gate the notice behind a flag rather than making it unconditional on every unflagged long run, so status-0-implies-silent-stderr remains true for users who never asked for the new behavior. As written, a strict "conforming consumer" that asserts empty stderr on exit 0 (which the doc invites it to do) breaks the day this ships, and nothing in the Definition of Done catches that because no golden test asserts on stderr-emptiness for a non-`-dunlimited` run that happens to hit the default.

### 2. (Medium) H4 is very likely already refuted by the current code, but the issue ships without deciding it

O(bservation): `JLSStart.timeLimit` (`JLSStart.java:98`) is initialized to `JLSInfo.defaultTimeLimit` and is only ever reassigned inside `case "d":` when `-d` is supplied. There is no boolean recording "was `-d` given." An explicit `-d 100000000` and no `-d` at all produce **byte-identical** state at the point `BatchSimulator` would need to decide whether to emit the notice — `timeLimit == 100000000` either way. P7 requires the two to be distinguishable (explicit `-d 100000000` → no stderr; no `-d` → stderr). The issue is honest that this is an open hypothesis (H4) rather than claiming it's solved, and correctly flags "public interfaces provided" as growing if refuted — but the task list still treats "determine H4" as a single checklist bullet alongside implementation bullets, when the evidence already collected in this same issue (O1, JLSStart.java:98) is sufficient to answer it now: a `wasDefaulted` boolean (or equivalent) is needed. Leaving it as "to be determined by the executor" when the issue's own observations already answer it is either padding or a missed step in its own rigor.
- **Recommendation:** Resolve H4 in the issue text before filing as ready-to-execute (it changes the public-interface section, §7.4, materially), rather than deferring a decision the cited evidence already settles.

### 3. (Medium) Acceptance criteria for the stderr notice are underspecified enough to be gameable

P3 only requires "at least one line to stderr naming both `100000000` and `-dunlimited`." §7.6 explicitly disclaims any parseable grammar ("Deliberately *not* given a parseable structure"). Combined with P7's requirement that a user-supplied `-d 100000000` produce *zero* stderr bytes, an implementation could satisfy every literal prediction by, e.g., emitting the notice on **every** run whose `now >= maxTime` at exit *and* `timeLimit` was never explicitly parsed away from its initial value — which is exactly the H4 gap in finding #2, not a distinguishing test of "was defaulted" vs "numerically coincides with default." Because there's no committed fixture/oracle for "a run whose activity exhausts the queue exactly at 100000000 with `-d` NOT given" vs "given," a sloppy implementation that keys off `timeLimit == JLSInfo.defaultTimeLimit` (which P7's own falsification note calls out as the wrong approximation) could pass every stated prediction as written, because P7 uses the *same* numeric value the default has, but no prediction exercises a case where `-d` is omitted and the run stops for a reason *other* than the time limit (e.g., queue drains at exactly tick 100000000) to confirm the notice is gated on "ended at the limit" and not merely "maxTime equals the default."
- **Recommendation:** Add an explicit negative case: no `-d`, fixture whose queue drains at some t ≠ 100000000, asserting stderr is empty (already implied by O6's fixture family but not stated as its own prediction), and a case where `-d 100000000` is given and the run is stopped by `stop()` rather than the time limit, to pin the "ended at the limit AND defaulted" conjunction rather than either disjunct alone.

### 4. (Medium) `-dunlimited` foot-gun is acknowledged but the mitigation is only a documentation sentence, and its wording is deferred to "Open Questions"

§7.11 and Open Question 3 both admit: no shutdown hook exists yet (verified: `git grep -c addShutdownHook -- src/ test/` returns nothing), so `-dunlimited` gives batch operators a flag that can hang a process indefinitely with Ctrl-C as the only exit, which truncates output mid-record (an already-documented failure mode per #354). The issue's own completion criteria do not require the doc sentence to exist before merge — Open Question 3 says "Blocks execution of the doc edit, not the code," meaning the code (the actual foot-gun) can land before the warning sentence is even guaranteed to be written, since it's phrased as a decision still needing resolution rather than a checklist item in §14.
- **Recommendation:** Promote the "requires an external timeout, no clean interrupt yet" warning to a hard entry in §14 Definition of Done (it currently is not one), and make it a compile-time or at least runtime-observable caveat (e.g., a one-line stderr notice on `-dunlimited` startup, symmetric with the default-limit notice), not just prose in `docs/batch-interface.md` a scripting user won't read.

### 5. (Low) Scope-boundary risk: "no case-insensitivity" decision is reasonable but unverified against existing FlagSpec/parsing conventions elsewhere in the codebase

The issue asserts (§7.11) that `-dinfinite`/`-dUNLIMITED` must fall through to `Long.parseLong` and exit 2, and explicitly rules out case-insensitivity "because widening it silently would put more strings in the new case than the contract declares." This is internally consistent, but the issue doesn't check whether any other flag in `JLSStart.java`'s `FLAGS` table already treats a literal keyword operand case-insensitively (which would make this an inconsistent convention within the same file). Not checked here beyond a grep for `equalsIgnoreCase` in JLSStart.java — none found — so the decision as stated is at least self-consistent with current file conventions; flagging only because the issue makes a normative claim about the *contract* without checking codebase precedent for similar literal-operand flags.
- **Recommendation:** Low priority; either drop the claim of principle ("more strings... than the contract declares") or verify there's no sibling flag with different case-handling that would make `-d unlimited` inconsistent with itself.

### 6. (Low) `docs/batch-interface.md:334-336` classification as "minor-version, CHANGELOG-only" is asserted, not argued against the actual stderr-contract break

O10 cites §6's "additions that cannot break a conforming consumer" clause to justify skipping a major-version bump. But finding #1 shows the change *can* break a conforming consumer that asserts on stderr emptiness at status 0 — which is precisely the kind of consumer the doc's own table (line 38) invites. The "cannot break" premise needs re-examination in light of finding #1 before the versioning conclusion is safe to keep as stated.

## What's solid (one-liners)

- H1/H2/Stage 1-3 math for the sentinel and the `unlimited`-before-`Long.parseLong` ordering is correct and directly falsifiable; no issue found.
- The dependency graph (`blocked_by: 410`, `blocks: 350`, `part_of: 354`) is accurate against the live issues; #410's own content genuinely does own the past-limit-event adjudication this task depends on.
- The `Long.MAX_VALUE`-as-sentinel choice reusing the loop's existing `<=` comparison (no engine-loop edit needed) is a clean, low-risk design with correct reasoning about the post-poll clamp becoming unreachable.
- GUI-field fix (O9) correctly identifies a real bug-in-waiting (the `int` clamp would silently render `2147483647` under a `long` sentinel) before any code changes — good defensive citation.
- Golden-suite regression guardrails (four named suites, byte-identical stdout requirement) are concrete and checkable, not hand-wavy.

## Verdict rationale

The issue is exceptionally well-evidenced and its core mechanism (H1/H2) is sound. It does not proceed cleanly, however: finding #1 is a genuine, unaddressed contradiction between the proposed change and an explicit clause in the same normative document the issue itself cites (`docs/batch-interface.md:38`), and finding #2 shows the issue ships an unresolved design question (H4) that its own evidence already answers, leaving the public-interface contract (§7.4) incomplete at the point of "ready to execute." Neither is fatal to the underlying idea, but both should be resolved in the issue text — not left to the executor to discover mid-implementation — before work starts.
