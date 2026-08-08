# Issue #322: FEAT-026: a signal can say "unknown" and "undriven" per bit, and contention resolves as a property of the driver set rather than of draw order
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary judgment

The core technical proposal — replace `@Nullable BitSet` with a per-bit
four-state value and an algebraic (commutative/associative/idempotent) fold
for multi-driver resolution — is sound engineering, and the code-anchor
claims that were checked against the current tree hold up exactly (see
"What checks out" below). But the issue is one node in a large, actively
mutating dependency graph maintained across issue comments, and at the
moment reviewed that graph is broken: the prerequisite this issue's own
critical path depends on has no open tracking issue, its `blocked_by: []`
claim is false in substance, and a key numeric justification in the
decomposition rationale traces to a document the project's own records say
never existed on `master`. None of these are hypothetical risks — they are
verifiable as of today against the live issue tracker.

## Findings, most severe first

### 1. The critical-path prerequisite (TASK-0056 / the value type) currently has no open filed issue — the graph dead-ends today

The issue's own comment thread documents the chain: TASK-0056 was filed as
#475, closed 2026-08-08 as a duplicate of #232 (`tier:feature`, which
"absorbed" a task it cannot itself close out). The gap was "discharged" by
filing #881 (`tier:task`, `part_of_feature: 232`, `blocks: [322, 344, 391,
422]`) — but I fetched #881 directly and **it is `state: closed`,
`state_reason: duplicate`, closed 2026-08-08T18:53:58Z**, roughly 25 minutes
after the #322 comment (18:28:01Z) that told readers to treat #881 as
TASK-0056's new home. #391 (TASK-0057, the resolution fold, which *is* part
of this feature) states explicitly under "Materials & Apparatus": "Must
exist first: TASK-0056's value type. Without it the fold is inexpressible
(O3)." Right now that prerequisite traces to a closed duplicate with no
successor issue number given anywhere in #322, #391, or #881 itself.
**Recommendation:** before this issue is picked up, resolve where
TASK-0056 actually lives (re-open #881, file a fresh task, or fold it back
into #232 with an explicit artifact-producing child) and update #322's
`planned_tasks` accordingly. Anyone starting "TASK-0057" today, per #391's
own stated order, cannot legally start.

### 2. `blocked_by: []` / "this feature gates on nothing" is false in substance, and the issue's own follow-up comment admits the gap without closing it

The YAML block asserts `blocked_by: []` and the prose leans on
`docs/capability-roadmap/README.md:222` ("Dependencies. None on other
programs.") to argue this feature can start immediately. But the value type
its critical path requires (Finding 1) is gated behind **#232's own
falsification gate**: #232 §1 states outcome (a) is a legitimate close —
"a profile shows BitSet allocation/GC is *not* a meaningful share of
hot-loop cost; the representation stands and **no swap is built**." The
#322 comment thread concedes this directly: "If #232's H1 is refuted, #881
is descoped and this feature needs a different route to a per-bit value
type. **That contingency belongs in this feature's re-planning protocol,
not in a scheduler's head.**" — i.e., the author has identified a live risk
to §7's re-planning protocol and left it unaddressed. An issue that can be
silently orphaned by an unrelated feature's benchmark result is not
"gating on nothing." **Recommendation:** add the #232→#881(or successor)
edge to `blocked_by` honestly, and add the H1-refutation contingency to §7
before treating this as unblocked, ready-to-start work.

### 3. A load-bearing numeric justification (the rejected tagged-union alternative) cites an unrecoverable, possibly-never-existed document

§2 "Alternatives considered and rejected" states the tagged-union design
was "Rejected on measured cost... it costs 9.24 vs 7.01 ns/op (+32%) and
+16 bytes per value in a *pure binary* circuit." That figure is the entire
justification for committing to a plane-encoded representation over the
simpler tagged union. Tracing it: #391 cites it to
`docs/plan/evidence/BRIEF.md` at commit `3a81a4a7d6a0f108ec201e632732d308cc02b3fc`;
#881 states plainly: *"`3a81a4a` does not resolve, and `docs/plan/**` is in
#493's unrecoverable set — 195 planning documents that never existed on
`master` and cannot be re-pinned at all,"* backed by a pasted
`git cat-file -t` failure, and instructs that the figure be treated as "an
unrecoverable quotation," never a live citation, and **re-measured** rather
than trusted. I confirmed independently: `docs/plan/` does not exist
anywhere in this checkout (`find /home/user/JLS -type d -iname plan` is
empty), and the `docs/plan/features/` directory the LINK PASS comment cites
as the source of the entire dependency-graph ordering ("the § Prerequisite
features table of every one of the 57 feature documents under
`docs/plan/features/`") also does not exist. The issue text presents
"Rejected on measured cost" as a closed, "frozen" decision; the number
behind it is conceded elsewhere in the same corpus to be unverifiable.
**Recommendation:** either produce a reproducible benchmark for the
9.24-vs-7.01 ns/op claim before landing TASK-0056, or state plainly in this
issue (not three hops away in a task-level comment) that the plane-encoding
choice rests on an unrecoverable number and needs re-measurement as a
blocking step.

### 4. Acceptance criterion 7 (K9 / pedagogy floor) is unfalsifiable as written

§5's criterion 7 — the one the issue itself flags as "the criterion most
likely to be skipped" — is: "Do: open a first-year adder circuit... Observe:
conceptual load unchanged... Pinned by: a recorded manual procedure that
does not exist at [evidence commit]; this issue's close-out builds it."
Every other criterion in §5 is pinned by a structural test, a fixture, or a
byte-comparison; this one is pinned by a procedure that is simultaneously
(a) not yet written and (b) the artifact whose adequacy nobody but the
closing engineer will judge. A "verification" that consists of the same
person who did the work later writing the manual check that grades it is
gameable by construction — it can pass regardless of whether a first-year
student's actual experience changed. **Recommendation:** define K9's check
before the feature starts (e.g., a fixed screenshot/golden-render diff of
the default adder-circuit trace/stdout output, byte-compared, so a change
in default visibility is caught the same way any other golden is) rather
than deferring the entire definition of "pass" to close-out prose.

### 5. Criterion 1's grep is narrower than the claim it's meant to prove, and is gameable

§5 criterion 1: "`git grep -n '@Nullable BitSet' -- src/jls/elem/` after
the migration... no hits on the value channel." This only searches
`src/jls/elem/`. The stated goal is that "`null` is no longer the currency
of high impedance anywhere on the value channel," but the value channel per
§3 spans `Put`, `Input`, `Output`, `WireNet` (all under `src/jls/elem/`,
so that part is fine) **and** the three `SimEvent` payloads and
`TraceSample` in `src/jls/sim/`, which the grep does not cover. It is also
satisfiable by simply dropping the `@Nullable` annotation while leaving the
field nullable in practice (annotation-driven static analysis is only as
good as its coverage; SpotBugs/NullAway enforcement isn't cited as part of
this check). **Recommendation:** widen the grep to `src/jls/sim/` as well,
and pin the "no null HiZ anywhere" claim to a runtime assertion or a
compile-time-enforced non-null type rather than an annotation grep alone.

### 6. Open Question 1 in the issue body is now stale relative to the issue's own comment thread, and nobody has reconciled it

The original issue body's Open Question 1 says the three-vs-two-plane
disagreement "**Blocks TASK-0056's filing** — it decides the type's
shape." But comment 2 records that TASK-0056 is no longer this issue's
child at all — it left for #232/#881 (now closed, per Finding 1). The
open question that the issue body still presents as blocking is therefore
blocking a filing decision that this feature no longer makes. Comment 2
does say the two open questions "are now answered on #881, not here," but
the issue body itself (the part a new contributor reads first) was not
edited to reflect that, so a reader following the body literally will
think #322 still owns and blocks on this decision. **Recommendation:**
edit the issue body's Open Questions section to point at wherever OQ1/OQ2
now actually live, rather than leaving the correction buried in a comment
three states removed from the original text.

### 7. The cost band admits a 7-9x gap between the registry total and the two named tasks, and ships as "Open Question 4: blocks nothing"

"[T]he band exceeds the row sum by 7x to 9x, and both numbers are printed
rather than a row adjusted... Do not read 4 wk as the feature, and do not
read 28-36 mw as this feature's alone... nothing here is adjusted to make a
total true." This is disclosed rather than hidden, which is to the issue's
credit, but tagging it "blocks nothing" while the completion criteria
require "[e]very entry in requires_tasks closed... planned_tasks empty" is
a soft contradiction: the issue can reach Definition-of-Done with its own
cost accounting still admittedly wrong by an order of magnitude, because
"funded" and "done" are different gates that the issue never actually
requires to reconcile. For a program-sized migration (338 `BitSet`
references, 27 `react` coercion decisions each needing individual review,
`Memory.java` at 1,547 lines scheduled alone) this is a real planning risk,
not a nitpick. **Recommendation:** make cost-band reconciliation a
completion-criterion checkbox, not a floating open question with no owner.

## What checks out (verified directly against the repo, HEAD `d6bc8dd7`)

- `src/jls/elem/Put.java:385` is exactly `protected @Nullable BitSet
  currentValue;` as claimed.
- `src/jls/elem/WireNet.java`: `propagate` at line 443, the `if
  (triState) {` block opening at line 454, matching the issue's `:443` /
  `:454-485` anchors; the first-non-null-driver-wins logic and the
  once-until-cleared warning are exactly as described.
- `git grep -c "BitSet" -- 'src/jls/elem/*.java'` sums to **338**, and
  `git grep -c "public void react" -- 'src/jls/elem/*.java' | wc -l` is
  **27** — both match the issue's cited figures exactly.
- `ElementRegistry` has **35** `ElementType(` entries, matching the
  issue's "35 registered element types" (note: a different roadmap doc,
  `docs/capability-roadmap/README.md:227`, says "33 registered types" for
  a different program — a minor staleness in that doc, not in #322).
- `src/jls/core/` currently holds only `Bounds`, `Geometry`, `GridPoint`,
  `GridSize`, `Orientation`, `SegmentGeometry`, `TextMetrics`,
  `package-info` — matching the issue's "ABSENT at [evidence commit]"
  claim about `LogicVector`/`LogicValue` not existing yet.
- `docs/simulation-semantics.md` §2 and §9 read exactly as quoted (two-state
  bits, whole-signal HiZ via `null`, first-driver-in-net-order resolution,
  "There is no wired-AND/OR and no conflict (X) state").
- The #322-vs-#341 boundary (value domain vs. strength lattice) argued in
  the deduplication comment is coherent and the "strength is meaningless
  over a two-state value" ownership split is a reasonable cut — no
  objection there.
- Scope exclusions (strength → FEAT-027, radix → FEAT-028, engine constant
  factors → FEAT-030 with the explicit no-goldens-together rule) are
  clearly stated and internally consistent with what's checked in Finding 6.

## Feasibility note

Independent of the graph problems above, the underlying migration is
large and single-bus-factor risky by the issue's own admission ("28-36 mw
... six to eight months ... bus factor 1"). That's disclosed, not hidden,
so it isn't scored as a separate finding — but combined with Findings 1-3
(broken prerequisite chain, false "gates on nothing" claim, unverifiable
cost number behind a core design decision), the issue is not currently in
a startable state even though its target design is reasonable.
