# Issue #803: TASK-C592-2: each catalog row carries a funding score, a named acceptance vehicle and a stop-loss column — and the timed counter task gets its "before"
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the vocabulary and #803 asks for one thing: **make the parity catalog load-bearing.**
#802 produces a survey (rows, citations, HAVE/GAP/REFUSE). #803 is supposed to convert that
survey into the mechanism that decides what gets funded and when funding stops. That goal is
right and it is the best part of CAP-37 — the capstone's own KC-37-2 admits ergonomic polish
"has no natural end," and a scored gate is the only honest answer to that.

I endorse the goal. I am reframing all four acceptance criteria, because each one reaches for
the goal with an instrument the project has already outgrown, and in three cases the better
instrument is *already sitting in the tree*.

## Reframe 1 — the acceptance-vehicle column is a two-valued enum where a three-valued taxonomy already exists

AC-2 makes every row name "#91 harness or #441 headless interaction machine." Two problems,
one fatal and one deeper.

The fatal one: **#441 was closed as a duplicate on 2026-08-08**, absorbed into #84
(`https://github.com/anadon/JLS/issues/441#issuecomment-5181495157`). A column whose allowed
values are issue numbers went stale four days after the issue was filed. Issue numbers are the
wrong domain for a durable column in a document that gates five features.

The deeper one: `test/jls/ui/package-info.java` already defines the correct vocabulary, and it
is not binary. It is a three-layer cost ladder with an explicit discipline —
*"the cheapest layer preferred per assertion"*:

- **Layer 1** — headless model assertions (`CircuitAssert`, `GeometryAssert`); no display, safe
  on any runner.
- **Layer 2** — Swing harness driving the real listener chain under the #162 Xvfb substrate
  (`EditorGestureSupport`, `EditorGestureTest`, `PopupOperationBehaviorTest`); `@Tag("display")`,
  carries `rerunFailingTestsCount=2` for popup-timing flake.
- **Layer 3** — render-to-image semantic assertions (`RenderAssert`, `RenderBoundsTest`).

And there is a *fourth* vehicle the package-info predates: **extracted headless policy objects**.
`src/jls/edit/DeleteKeyPolicy.java`, `KeyboardConstructionPolicy.java`, `OptionMenuPolicy.java`
are editor decisions lifted out of the god class and pinned by plain unit tests
(`test/jls/edit/DeleteKeyPolicyTest.java`, `OptionMenuPolicyTest.java`,
`KeyboardConstructionPolicyTest.java`) with no harness and no display at all. That is the
cheapest and most durable vehicle in the repository, and #803's binary column cannot express it.

**Concrete alternative:** the column is `vehicle ∈ {policy, layer-1, layer-2, layer-3}`, not an
issue number. This is strictly more informative for a funder — it *is* the cost estimate, since
layer 2 is where flake and Xvfb time live — it survives issue renumbering, and it inherits an
in-tree discipline instead of inventing one. A row that can only be pinned at layer 2 is a row
whose real price includes the retry-masking debt #91's residual is still carrying.

## Reframe 2 — the timed 4-bit-counter baseline is the wrong instrument, and AC-3 says so itself

AC-3 asks for a wall-clock measurement "with the procedure, the operator and the conditions
stated so it can be re-run comparably." That sentence is the design's own confession: a
single-operator stopwatch number is not comparable to anything a year later, on a different
machine, with a different operator, after the operator has learned the editor. CAP-37 AC-4's
claim is "the after is not slower." A stopwatch can neither confirm nor refute that; it can only
produce a number nobody will contest and nobody can reproduce.

**This is where I am explicitly disregarding the stated acceptance criterion.** JLS does not
verify things with prose measurements. It verifies them with ratchets:
`PointerApiRatchetTest`, `DialogCoverageRatchetTest`, `HeadlessCoreRatchetTest`,
`NullMarkedRatchetTest`, `SocketConfinementRatchetTest`, `NotificationRatchetTest`,
`CollabSecurityRatchetTest`. That is the project's actual verification culture, and the counter
baseline should join it.

**Concrete alternative:** script the 4-bit counter build as a Layer-2 gesture sequence over
`EditorGestureSupport` and measure **interaction cost, not seconds** — gesture count, keystroke
count, modal dialog round-trips, and mode switches. Land it as
`test/jls/ui/EditingCostRatchetTest.java` with the counter's cost recorded as a ceiling.

Why this is radically better rather than merely different:

- It is deterministic. Synthetic events, no `Robot`, no human — the harness javadoc already
  argues exactly this ("deterministic and fast instead of fighting Robot/Xvfb timing").
- It converts AC-4 from a once-per-capstone claim into a **per-PR build failure**. Any PF-2..5
  fix that adds a dialog round-trip to counter construction goes red immediately, which is the
  actual thing "the after is not slower" is trying to protect.
- Interaction cost is the quantity ergonomic parity is *about*. Logisim-Evolution #1234 (no
  component search) and bsiever #18 (compound selection) are complaints about gesture count;
  seconds are a noisy proxy for them.
- Wall-clock can still be reported once as an informative footnote for the human-facing story.
  It just must not be the pin.

## Reframe 3 — the catalog wants to be checked data, not a hand-maintained Markdown table

Count what this table is now being asked to carry: grade, citation, prose reason, owning feature
(#592's dedup pass 1), corrected dark-mode owner #289 (pass 2), a `SimpleEditor`-blocked flag
(#802 AC-4), funding score, stop-loss, acceptance vehicle. Nine columns of structured metadata,
hand-maintained in Markdown, gating the funding of five features — and pass 2 already found two
rows that misfire before a single score exists.

JLS has a strong in-tree precedent for exactly this problem: tables that would rot are pinned by
tests. `HelpTopicsTest` link-checks and completeness-checks the help topics; `ElementRegistryTest`
enforces registry totality; `CliFlagTableTest` pins the flag table; `MenuBarSpecTest` pins the
menu-bar expectation table. #441's own text names the pattern — *"the FEAT-001 registry-totality
discipline reused rather than a new mechanism."*

**Concrete alternative:** publish the catalog as a parsed table with
`test/jls/docs/ParityCatalogTest.java` asserting, at build time:

1. every row's grade ∈ {HAVE, GAP, REFUSE}, and every REFUSE has non-empty prose (#802 AC-2);
2. every row names an owning feature (#593/#594/#595/#596/#570/#289) — the subtraction that
   bounds #596's forever-bucket;
3. every row names a vehicle from the four-value taxonomy above;
4. every row carries an estimate and a score;
5. every citation link is well-formed (the `HelpTopicsTest` link-checker idiom).

That last item matters most for AC-4. **A gate that the catalog merely *states* is not a gate.**
AC-4 asks the document to say "no PF-2..5 work is funded before its row exists and is scored."
Nothing stops a PR from violating a sentence. A test asserting that every PF-2..5 fix issue
referenced by a landed change has a scored row *is* the gate, and it costs one test class.

## Reframe 4 — store the estimate, derive the stop-loss

AC-1 insists the 1.5x stop-loss be "a column rather than guidance in prose." I would invert the
emphasis: store `estimate_mw` and `score`; the stop-loss is `1.5 × estimate`, derived. A
hand-maintained derived column in Markdown drifts from its input the first time an estimate is
revised, and then the gate is enforcing a stale number. State the 1.5x rule once in prose (it is
KC-37-2 and belongs to the capstone, not to each row) and let the checker compute the threshold.
This is a small point, but it is the same disease as reframe 3: data the document restates
instead of deriving.

## Does this strengthen the arc, or spend it?

One honest reservation about the whole PF-1 slice, #802 and #803 together. CAP-37 is gated on
#316/#84 — the decomposition of a **5,852-line** `SimpleEditor` — and KC-37-1 says the capstone
*waits* if that stalls. Meanwhile PF-1's demo slice is a document, now split across two tasks
that both edit the same table, and the second task rewrites the first's columns. That is 1–2 mw
of a 10–16 mw capstone spent on scoring bureaucracy in front of work that cannot start.

The visionary read: **merge #802 and #803 into one task that publishes the catalog once with its
full column set and its checker.** Splitting a single artifact along the parent feature's
acceptance criteria is the wrong seam — it produces two PRs that conflict on the same file for no
review benefit. Then split off the counter measurement as its own task, because it is a different
*kind* of artifact (an executable ratchet, not a survey row) and it is the one piece of PF-1 that
delivers value even if the catalog never gates anything. Spend what that saves on #84, which is
what actually unblocks PF-2..5.

## What I would keep unchanged

- The gate direction (catalog before fixes) is right and both #592 and #596 already agree on it.
- Requiring every row to name *how* it will be pinned, before it is scored, is the single best
  idea in this issue. A score without a pin is a wish. I am only changing the vocabulary the
  column speaks.
- Recording a "before" for the counter task is right. Only the instrument changes.
