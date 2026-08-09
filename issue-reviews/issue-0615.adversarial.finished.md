# Issue #615: TASK-C558-3: a parameterized Digital circuit either maps with its parameters bound or refuses by name — never a silent flattening
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the issue

#615 is TASK-C558-3, one of five child tasks (#612, #614, #615, #617, #619)
under FEAT-C29-2 (#558, the `.dig` importer), itself under capstone CAP-29
(#513). It has four ACs: map a parameterized Digital circuit when JLS can
express its parameters (AC-1), refuse by name with reason when it cannot
(AC-2), name #357 as the successor in the refusal (AC-3), and cover both
branches in a test fixture (AC-4). Nothing `.dig`/XStream/Digital-related, no
XML parser, and no parameter/definition-instance representation exists
anywhere in `src/` today (confirmed via `grep -rli "digital"` and via #357's
own evidence section, reproduced below) — this is a from-scratch design on
top of a prerequisite feature that is itself unbuilt.

## Findings, most severe first

**1. AC-1 and AC-4 depend on machinery (#357) that does not exist, is not
declared as a blocker, and is priced at 25-36 mw — 25-36x this task's own
1 mw band — with three more unfinished prerequisites of its own.** #357
(FEAT-017, "one subcircuit definition, N instances... with bound
parameters") is `blocked_by: [318, 319, 340]`, none of which are closed, and
its own evidence section states: *"There is no parameter surface to extend;
this feature creates it"* and *"There is no definition identity in the tree"*
(`git grep -c "defid\|DefinitionId" 2d0ca9d -- src/` exits 1). AC-1 requires
importing "a definition plus parameter-bound instances, with the parameter
values preserved" — literally the representation #357 has not built yet.
AC-4 requires a test fixture covering "one mappable" case. Neither AC-1 nor
AC-4 can be satisfied until #357 lands, full stop — yet #615's own Boundary
notes claim *"the refusal half is deliverable before #357 lands and is what
makes this task shippable on its own,"* implying the task can close now.
Those two statements are in direct tension: if the task ships now,
AC-1/AC-4's mappable half is unsatisfiable; if it waits for #357, the
"shippable on its own" claim is false advertising and the boundary note
should instead say "not shippable, blocked." **Recommendation:** either
split AC-1/AC-4's mappable half into a follow-up task explicitly
`blocked_by: [357]`, and scope #615 itself to refusal-only (matching what
the boundary note actually argues), or state plainly in the issue that this
task cannot close until #357 closes.

**2. The dependency on #357 is an unmirrored half-edge, which is exactly the
defect class #357's own text says must never happen.** #615 lists `357` in
`ordering_after` — a real, load-bearing dependency per finding 1 above — but
#357's own machine block states `blocks: []` and explains why: *"no feature
declares this one a required prerequisite... a beneficial row is carried in
`related`... never as an ordering edge"* and, in its Re-planning Protocol,
*"a half-edge is the defect this Link pass exists to prevent."* #615 (via
#558) is a concrete case of exactly the half-edge #357 claims does not
exist: #615 treats #357 as blocking for the mappable half, but #357 records
no corresponding `blocks` entry for #615 or #558. This is a genuine
cross-issue contradiction, not a stylistic nit — anyone reading #357 alone
would conclude nothing downstream depends on it yet. **Recommendation:** add
a mirrored edge (`blocks: [615]` or `blocks: [558]`) to #357, or record why
task-tier `ordering_after` is deliberately exempt from the mirroring
discipline that feature-tier `blocked_by`/`blocks` enforces.

**3. "Where JLS semantics agree" (AC-1) is undefined, which makes the AC
gameable in the direction that defeats its own purpose.** Nothing in #615,
#558, or #357 enumerates which parameterized Digital constructs JLS can
express once #357 lands (bit-width parameters? gate-count parameters?
nested generic instantiation?). Because AC-1 only fires on circuits where
"JLS semantics agree," an implementer can satisfy the letter of AC-1 by
defining that set as empty and refusing everything — which is
indistinguishable, from the test suite's perspective, from an honest
implementation that happens to find no expressible case yet. AC-4's "one
mappable" fixture is the only check against this, and per finding 1 it
cannot exist before #357. Until #357 ships there is no way to verify AC-1
was implemented in good faith rather than trivially vacuous.
**Recommendation:** name at least one concrete parameter class (e.g. Digital
bit-width generics) that must map once #357 lands, so AC-1 has a non-empty,
checkable target instead of an open-ended "wherever it works out."

**4. #615 never references #556's shared report contract, even though its
own report obligations (AC-2, AC-3) plainly emit into it.** Sibling task
#614 (TASK-C558-2) is explicit: *"The report entries this produces are
emitted in #556's shared contract; this task supplies the content, not the
carrier."* Sibling task #619 (TASK-C558-5) is explicit: *"AC-3: The report
uses #556's schema unchanged — no `.dig`-specific report dialect."* #615's
refusal entries ("the parameter and the reason in the report," AC-2) and its
successor-naming clause (AC-3) describe report content but cite no schema at
all — not #556, not a diagnostic-code convention analogous to #357's
`(unresolved, path, param)` / `(unknown-parameter, path, param)` tuples. As
written, an implementer could free-text the refusal reason in a format
#556's shared report renderer doesn't recognize, and every AC in #615 would
still read as satisfied. **Recommendation:** add a clause tying AC-2/AC-3's
report entries to #556's schema, matching #614 and #619.

**5. AC-3 hardcodes a specific issue number into user-facing diagnostic
text with no update mechanism.** AC-3: *"The refusal names #357 as the
successor... so the report tells the instructor what would unblock it."*
#357's own Re-planning Protocol anticipates exactly the scenario that would
break this: *"A child split (HANDOFF)... If the residual is ever cut into
filed tasks, each gets a `part_of_feature` pointing here"* and *"A child
dropped or this feature descoped... re-homed, freed, or closed."* If #357
is ever split, renumbered, or superseded, every refusal message #615 ships
still points instructors at a closed or stale issue, and nothing in #615
requires that text to be revisited when #357's status changes.
**Recommendation:** either state that the successor reference must be
kept current as part of #357's own re-planning protocol, or point at a
stable capability name/tracking label rather than a bare issue number.

**6. Cost plausibility: 1 mw is priced without stating what it actually
buys.** Unlike #357 and #323, which each show a task-row sum reconciled
against their band (#357 explicitly: *"the band exceeds the row sum by
6.25x... the largest such gap in the plan"*), #615 gives no breakdown of
what the 1 mw covers. Per finding 1, if 1 mw is meant to cover only the
refusal half, that should be stated; if it's meant to include a working
AC-1 mappable path once #357 lands, 1 mw for wiring against a not-yet-built
definition/binding API is implausibly cheap given #357 prices the whole
representation change at 25-36 mw. **Recommendation:** state explicitly
whether the 1 mw band is refusal-only or includes the mappable half, and
size accordingly.

## What's solid

- **AC-2's "no flattened copy" rule** is a real, well-motivated defect class
  — it correctly forecloses the tempting shortcut of silently emitting N
  diverging subcircuit copies for a construct JLS can't yet express
  natively, and it correctly cites FEAT-C29-2 AC-3 as its source rather than
  inventing new policy.
- **AC-4's "both halves" framing** (one mappable, one refused fixture) is
  the right shape for preventing the refusal path from rotting into
  always-refuse or always-flatten — its only defect is that it's currently
  unsatisfiable pre-#357 (finding 1), not that the idea is wrong.
- **The refusal-before-#357 sequencing decision itself** — ship an honest
  "refused, here's why, here's what unblocks it" now rather than wait for
  #357 or ship a silent flattening in the interim — is the correct strategic
  call, independent of the drafting gaps above.
