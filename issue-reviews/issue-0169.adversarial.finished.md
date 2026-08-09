# Issue #169: Shared session v1: membership lifecycle, snapshot sync, floor control, presence, peer panel (collab Stage 1b)
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what was checked

Fetched #169 (open, feature tier) and its 9 comments, its declared sub-issue
#281, and issue #435 which the final comment (2026-08-08T18:18:03Z) claims to
have adopted. Cross-checked line citations against commit `29afb26` (pinned
evidence commit) and current `HEAD` (`5311625`), confirmed `29afb26` is an
ancestor of `HEAD`, and grepped `src/`, `test/`, `docs/collaborative-editing-
research.md` for the classes and sections cited.

## Findings, most severe first

**1 (High) — the issue's own latest "structural correction" is not true of the target it corrects.**
The final comment states: *"Adopted roster. #435 (TASK-0109)... It has been
given the matching `part_of_feature: 169` correction."* Fetching #435 live
shows its YAML block still reads `part_of_feature: 348   # FEAT-051
(RESIDUAL)`, and the API reports `"has_parent": false` for #435 — no GitHub
parent-child edge exists. #169's own machine block still lists
`requires_tasks: [281]` only (not 281+435), and `sub_issues_summary` on #169
reports `"total":1` — i.e. #281 is the only real sub-issue. The claimed
correction was written but never executed anywhere a reader could verify it.
Recommendation: either perform the actual GitHub sub-issue re-parenting and
update the YAML `requires_tasks`/roster table, or strike the claim. Do not
let "a comment says so" stand in for a checked box — this issue's own
Definition of Done ("`requires_tasks` closed... roster updated... machine
block agrees with reality at close") is exactly the property this violates.

**2 (High) — the thing just "adopted" contradicts this issue's own scope boundary.**
§1 states explicitly: *"Out of scope: concurrent editing and op-level sync
(#171)... No merge logic: one writer at a time."* #435 is a headless
**CRDT** replica-convergence harness over `CausalBuffer`/`OpEnvelope`/
`VectorClock` — it is `blocked_by: [382]` (headless op-apply, absent from
#169's dependency graph entirely) and its own Related Work section says its
convergence assertions feed **TASK-0110 under FEAT-052/#352** and must be
reconciled with **#279**, both explicitly Stage-2 (#171) issues. #435 even
says of itself: *"#171 itself is closed by TASK-0110, not by this task."*
Either adopting #435 silently drags Stage-2 CRDT scope into a "no merge
logic, one writer" feature (contradicting §1), or the adoption claim (finding
1) is simply mistaken about what #435 is. Either way the graph in §"Status &
Dependency Graph" — asserted to be a DAG with `blocked_by: [168]` as the only
edge in — is now stale and unaudited if the adoption stands.

**3 (Medium) — I5 / DoD manual criteria are self-attested and unfalsifiable by design.**
*"Manual three-machine LAN session on Linux recorded... panel screenshots via
the sway rig; Stage 1 pilot protocol... executed with exit criteria met"* has
no CI gate, no specified artifact-retention location, and no named verifier
other than the filer themself. Given finding 1 — a structural claim already
made and left unverified in this very issue — there is no basis to expect I5
gets independently checked rather than merely asserted in a closing comment.
Recommendation: require the screenshot/log artifacts to be attached to the
closing comment or linked to a permanent location before I5 can be marked
done, not just described in prose.

**4 (Medium) — load-bearing timing constants are deferred as "rides along."**
Heartbeat interval and token-reclaim timeout are listed under Open Questions
as blocking neither filing nor integration, yet they directly gate **I2**
(reclaim-after-crash timing) and **I4** (unreachable-vs-removed threshold) —
two of the five feature-level integration criteria that close this issue.
Different child slices (anti-entropy binding, token gating) implemented
without a pinned value risk incompatible assumptions discovered only at the
runner/pilot stage, i.e. late and expensively. Recommendation: pin the
recommended defaults (2s/3 intervals/3×) in the body now, not as a rider.

**5 (Medium) — the acknowledged eject-abuse surface is not covered by any I1-I5 criterion.**
`EntryKind.EJECT`'s own javadoc (verified in `src/jls/collab/session/
EntryKind.java`) states plainly: *"the check on abuse is social visibility,
not cryptographic governance."* Any admitted member — including a student in
a paired lab session — can eject any other member, including the instructor,
with no permission tier and (per the body) no stated rate limit; presence
frames get rate-limiting called out explicitly, roster churn does not. This
is cross-referenced to #170 ("caps/allowlist enforcement on these payloads"),
but #170 is not in `requires_tasks` or `blocked_by` — #169 can complete and
close its Definition of Done with this abuse path still wide open.
Recommendation: add an eject/roster-churn rate-limit or cooldown to I3's
read-only-affordance testing, or explicitly add #170 as a `requires_tasks`
gate rather than a loose `related` mention.

**6 (Low) — scope-creep pattern via repeated "absorption," working against the issue's own stated rationale.**
The body itself records: *"Rejected alternative: one monolithic task (the
original filing)... work lands in exactly these slices."* Yet three
successive comments (2026-08-04, 2026-08-04, 2026-08-08) each "absorb" a
separate large issue (#348, #433, and now #435) back into this single
feature's scope narrative. Re-accumulating scope into the parent after
having decomposed away from a monolith specifically to avoid this pattern is
worth a REPLAN sanity check on whether the decomposition is still real or is
being slowly undone by dedup passes.

**7 (Low) — audience/cost proportionality.** The stated beneficiary problem
is "pair work means passing files around" (a file-sharing inconvenience) for
a project ARCHITECTURE.md describes as a single-maintainer pedagogy tool. The
delivered mechanism is a full epoch-consensus roster, encrypted P2P
transport (already landed in #168), chaos-tested anti-entropy, a floor-
control token, presence overlays, a peer panel, and a planned CRDT stage
after this one. Not disqualifying — the parent tracking issue (#163) frames
this as a deliberate capstone-scale investment — but worth a maintainer gut
check against effort actually available, especially given the process
overhead visible in this issue alone (9 comments, 3 absorbed duplicates, a
7-item re-planning protocol) relative to the "landed" code (`Roster`,
`ReachabilityTracker`, two small classes).

## What holds up

- Dependency graph against **live** GitHub state checks out for the edges
  the body actually declares: `requires_tasks:[281]` (open, correctly
  unblocked), `blocked_by:[168]` (open, correctly a close-out-only gate),
  `blocks:[171]` (open) are all consistent with current issue states.
- Code citations are accurate and reproducible: `Roster.java`,
  `EntryKind.java` (ADMIT/LEAVE/EJECT/TOKEN_GRANT/TOKEN_CLAIM),
  `Circuit.stateHash()` at L1548, and `SimpleEditor.markChanged`/`submit`
  near L5497/L5549 all match both the pinned commit `29afb26` and current
  `HEAD` — the evidence discipline this issue claims (line citations
  re-derived, greps re-run) is genuinely followed here, in contrast to
  finding 1.
- `docs/collaborative-editing-research.md` §5.5 ("The NAT reality...")
  exists exactly as cited for the any-member-forwarding rationale.
- The Scope Boundary section is a real, useful discipline (explicit
  out-of-scope list with owners) — it is just violated by the issue's own
  most recent action (finding 2).
- Token gating is genuinely unimplemented in `src/jls/edit` today (no
  `tokenHolder`/`TOKEN_GRANT` references) — the "planned, unfiled" status
  for that row is honest, not overstated.
