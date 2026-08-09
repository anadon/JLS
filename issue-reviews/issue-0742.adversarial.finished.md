# Issue #742: TASK-C560-2: the head-to-head table publishes with at least one workload a competitor wins, and Digital's 120 kHz claim gets a measurement rather than a counter-claim
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Findings

### 1. [HIGH] The target file directly contradicts sibling issue #555's stated boundary
#742's outcome text says: "The comparison table lands in `docs/performance.md`", and AC-1 requires
"A comparison table is published in `docs/performance.md` reporting all three tools across the harness's
workloads." But #742 itself lists `ordering_after: ["TASK-C560-1", 555]` — i.e. it depends on #555
(FEAT-C28-2), which owns `docs/performance.md`. #555's own "Boundary / reference notes" say, verbatim:

> "The head-to-head competitor table is **FEAT-C28-4**, not this doc; this doc may link it once it exists."

"Not this doc" is #555 explicitly disclaiming that the head-to-head table lives inside `docs/performance.md`
— it says `docs/performance.md` will *link to* the table, implying the table is a separate artifact (its own
file, plausibly under something like `docs/comparisons/`, parallel to #588's `docs/comparisons/*.md`).
#742 (the task that implements FEAT-C28-4, #560) asserts the opposite: the table *is* content inside
`docs/performance.md`. #560 itself (the parent feature) never states a path, so it doesn't resolve the
conflict either way. Whoever implements #742 literally will violate #555's boundary; whoever implements
#555 literally will produce a doc `docs/performance.md` that #742 cannot correctly add content to. This
needs a resolving edit before work starts — either #742's outcome/AC-1 text is wrong about the path, or
#555's boundary note is stale.
**Recommendation:** change #742 to publish the table at a distinct path (e.g. `docs/comparisons/head-to-head.md`)
with a link from `docs/performance.md`, matching #555's boundary language, or explicitly amend #555's boundary
note if the intent really changed to "same file."

### 2. [HIGH] "Comparable workload" for Digital's 120 kHz claim is undefined and unverifiable
AC-3 requires "Digital's 120 kHz claim is addressed by a measured row for the comparable workload." Nothing
in #742, #560, or #512 (CAP-28) cites *where* Digital's "120 kHz simulated processor clock" figure comes from
— what circuit Digital ran, what host, what iteration/settling methodology. #512's evidence field only says
"Digital teardown (published 120 kHz processor claim...)" without a source link. Without Digital's own
methodology as a written reference, "comparable workload" is a subjective judgment call made unilaterally by
whoever builds the harness (in #740) — there is no way for a third party (or a later reviewer checking this
AC) to confirm the workload used is actually "the same workload" Digital measured, only that it is *a*
CPU-scale-ish workload someone decided was close enough. This is exactly the kind of comparison CAP-36 (#588)
is designed to guard against ("no strawman setups... a comparison whose competitor configuration cannot be
defended is worth less than no comparison" — #740's own boundary language). As written, the AC can be
satisfied by measuring almost any circuit and asserting it answers the 120 kHz claim, while the real epistemic
goal (an apples-to-apples answer to Digital's specific claim) silently fails.
**Recommendation:** add an AC requiring the specific source of Digital's 120 kHz claim (URL/release note/wiki
page) to be cited in the table, with the chosen JLS/Logisim workload's divergence from that source's exact
setup stated explicitly (design size, host class, JDK vs Digital's JVM, etc.), so "comparable" is falsifiable
rather than asserted.

### 3. [MEDIUM] "stated plainly, with no softening qualifier" is untestable
AC-2: "At least one workload where a competitor beats JLS is present in the table and stated plainly, with no
softening qualifier (CAP-28 AC-5)." "No softening qualifier" has no operational definition — a footnote, an
adjacent caveat sentence, or a differently-worded column header could all be argued either way at review time.
This is a checkbox that cannot be mechanically or even consistently manually verified, which invites exactly
the dispute the criterion is trying to prevent.
**Recommendation:** replace with a concrete, checkable rule, e.g. "the losing number appears unfootnoted in the
main table row; no sentence in the same section characterizes the loss as insignificant, unrepresentative, or
about to be fixed."

### 4. [MEDIUM] Citation is imprecise: the "no softening qualifier" language is not actually in CAP-28 AC-5
Checked against #512 (CAP-28) directly: AC-5 reads only "The head-to-head is published with the harness,
including at least one workload where a competitor wins." It says nothing about hedging or plain statement.
The "stated plainly" phrasing actually originates one level down, in #560's (FEAT-C28-4) own AC-3: "...with
the result stated plainly (CAP-28 AC-5; KC-28-1 forbids withholding)." #742 copies the stronger, narrower
claim but cites the weaker, broader parent AC as its source. A reviewer chasing the citation in #742 to check
it against #512 will find #512 doesn't say what #742 implies it says.
**Recommendation:** cite #560 AC-3 (where "stated plainly" actually lives) in addition to, or instead of,
CAP-28 AC-5.

### 5. [MEDIUM] The staleness-discipline hook to #557 likely doesn't cover the competitor rows
AC-4 justifies recording measurement date + harness commit "so a later re-run under #557's staleness
discipline is a re-publication rather than a silent edit." But #557 (FEAT-C28-3)'s own scope is explicitly
"runs the committed benchmark suite (FEAT-C28-1, #554) against the ceiling bands" — i.e. it re-runs *JLS's*
suite in a scheduled CI lane. There is no indication anywhere (#557, #740, #742) that CI will have Digital and
Logisim-Evolution installed and licensed to auto-detect staleness in the *competitor* rows of the head-to-head
table; #740's AC-2 only requires recording "hardware, tool versions and settings... per tool in the harness
output," not a CI-runnable competitor re-check. So the date+commit fields buy a *manual* paper trail (good),
but AC-4's phrasing implies they plug into an *automated* safety net that, on the evidence collected, doesn't
extend to the competitor half of the table. This should be stated as a known gap rather than implied to be
solved.
**Recommendation:** soften AC-4's framing to "...so a re-run is auditable as a re-publication" and separately
flag (in #557 or #560) whether/how competitor-side staleness will ever be automatically caught, since today it
reads like it will be and probably won't.

### 6. [LOW] Tight effort estimate given full dependence on unfinished upstream work
`band_mw: 0.25-0.5` (roughly the smallest task in this cluster) assumes #740's harness lands cleanly, fairly
configured for two GUI-first third-party Java tools, and that it can produce Digital output for a workload
"comparable" to the 120 kHz claim (see Finding 2). If #740 marks the CPU-scale workload not-applicable for
either competitor (which its own AC explicitly allows — "Workloads that a competitor genuinely cannot express
are recorded as not-applicable"), #742's AC-3 as written has no fallback and could become unsatisfiable through
no fault of #742's own execution. Low severity because it's a scheduling/estimation risk rather than a defect
in the issue text, but worth flagging before committing to the band.

## What's solid
- The KC-28-1 quote ("an unfavourable result publishes anyway — withholding is what the criterion exists to
  forbid") is an accurate, correctly-attributed quote from #512.
- Dependency ordering (`ordering_after: ["TASK-C560-1", 555]`) is structurally sound: TASK-C560-1 (#740)
  builds the harness this task consumes, and #555 is the doc this task extends — the transitive chain through
  #554/#413 for the CPU-scale fixture's re-homing is intact.
- The measurement-date-and-harness-commit requirement (AC-4) is concrete and independently checkable, unlike
  AC-2's "no softening qualifier" language.
- The Boundary section correctly defers comparison *prose* to #588 (CAP-36) rather than scope-creeping into it.
