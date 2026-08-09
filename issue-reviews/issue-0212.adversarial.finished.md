# Issue #212: Element-provider plugin API: discover external ElementType descriptors via ServiceLoader atop the #78 registry (the recorded replacement for the removed XML loader, #80 H2)
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what was checked

Fetched the issue body and all 14 comments (through 2026-08-08T18:17:40Z). Verified
against HEAD (`5311625`, not the `29afb26`/`2d0ca9d` commits the issue cites, but
the cited facts were spot-checked against those commits via `git cat-file`/`git show`
where relevant): `ElementRegistry.ALL` is a closed `List.of(...)` of 35 entries
(`src/jls/elem/ElementRegistry.java:38`, `grep -c "new ElementType(" = 35`, matching
the issue's own count); `Element`/`LogicElement` are sealed with the exact permits
lists quoted in comment #8 (`src/jls/elem/Element.java:17-18`,
`src/jls/elem/LogicElement.java:17-21`); `grep -rn ServiceLoader src/ test/` returns
only the `JLS.java:15` javadoc comment; `ElementExtensionPoints.ELEMENT_PROVIDER` and
`CoreModule` exist and match the described contribution wiring; no `-providers` flag,
`ForeignElement` class, `ElementProvider.java`, or `ServiceLoaderDiscovery.java` exist
anywhere in the tree; `ARCHITECTURE.md#L295`'s #222 trust-boundary section is real and
matches its quotations. The absence/presence claims in the issue are, individually,
accurate as of the commits they cite.

## Findings, most severe first

### 1. The central open question ("build now or hold for demand?") is resolved by a citation to a file that has been deleted from the repository

Comment #10 (2026-08-08) states: *"§ Open Questions 'Build now or hold for demand?' is
answered by D10 rather than by a named requester"*, quoting **D10**,
`docs/plan/evidence/BRIEF.md` §13, *"landed in `3a81a4a7d6a0f108ec201e632732d308cc02b3fc`"*:
*"Demand gates apply to third-party asks, NOT to the maintainer's roadmap."* This is the
single ruling that overrides the issue body's own standing recommendation
(*"Recommended default (standing decision from #80, reaffirmed each cycle): **hold**"*)
and unblocks filing all three planned tasks.

That commit is real (`git cat-file -t 3a81a4a7d6a0f108ec201e632732d308cc02b3fc` →
`commit`), but its own message says it *adds* `docs/plan/evidence/BRIEF.md`, and a later
commit on the same side-history, `742da745`, is titled *"docs: remove the planning corpus
now that it is encoded in issues"* and explicitly deletes `docs/plan/evidence/` (206
files), stating *"D1-D16 rulings -> #485"*. `git show HEAD:docs/plan/evidence/BRIEF.md`
fails with "does not exist in 'HEAD'". So the one document that grounds the decision to
stop holding this feature for demand is not reachable from the working tree by the path
the issue cites, and the issue never updated the citation to point at #485 (the ruling's
new home per the deletion commit's own migration table). A reviewer — or an executor —
following the issue as written cannot verify D10 exists at all without independently
discovering #485. **Recommendation:** re-cite D10 against #485 (or wherever the ruling
now lives) with a live permalink, not a deleted planning-corpus path, before treating the
demand gate as lifted.

### 2. The issue body (the nominal "design record") was never updated to match what the comments say is the consolidated scope — spec now lives only in comment prose

The frozen body's §3 Interface and Completion Criteria describe a three-piece plan: an
`ElementTypeProvider` SPI returning `List<ElementType>`, `ServiceLoader` discovery in the
register phase, and a "P1/P2/P3" test battery (round-trip, byte-identical-without-provider,
faulty-provider-non-fatal). Comment #9 (2026-08-04) and comment #12 (2026-08-08), by
contrast, describe a materially larger and different mechanism as this issue's *actual*
current scope: an SPI with **both** `Collection<ElementType> types()` *and*
`String providerId()`; a **new in-tree class `ForeignElement`** breaching the sealed
`LogicElement` hierarchy via a `ForeignElementBehavior` delegate (a real, non-trivial
design decision recorded only in a comment, never merged into §3 or §4 of the body); a
new **`-providers <dir>` CLI flag**, default-closed; **namespaced tags** (`providerId:tag`)
requiring a `docs/file-format.md` grammar change (the body's §3 explicitly says *"Does not
modify: save-file syntax"* — directly contradicted by comment #12's own table entry
"Namespacing | `providerId:tag`, and the `docs/file-format.md` grammar for it"); a **child
classloader** as the trust surface; and a **"P1–P10" test battery** (comment #9) versus the
body's P1-P3. None of this is reflected in the Completion Criteria checklist, the §3 Data
Contract, or the §4 Global Invariants that an executor or a closing reviewer would actually
check against. This is a real internal contradiction (save-format-unchanged vs.
save-format-namespacing-added), not just staleness, and it means the document that is
supposed to be authoritative ("this issue is the design record until a real
external-provider request arrives") is not the one carrying the current design.
**Recommendation:** before any execution, do a REPLAN that rewrites §1/§3/§4/Completion
Criteria in the body itself to the comment #12 "consolidated scope" table, resolving the
save-format contradiction explicitly.

### 3. Process malfunction: the same child issue (#399) is recorded as being "absorbed" twice, and a task was filed under the demand gate before the ruling that lifts the gate was invoked

Comment #8 (2026-08-04T16:05) is headed "## Absorbing #399 (feature/task deduplication)"
and treats #399 as newly discovered and orphaned. Comment #10 (2026-08-08T16:29), five days
later, is headed identically, "## Absorbing #399 (feature/task deduplication)", and again
walks through re-homing #399 as though for the first time (*"#399 was re-homed here by the
#330 → #212 merge, so it is the task orphaned by that merge"*). Only comment #12
(2026-08-08T16:49) actually closes #399 ("`state_reason: duplicate`, `duplicate_of: 212`").
Two independent "absorption" passes ran against the same not-yet-closed child five days
apart without either checking whether the other had already happened — evidence that the
automated dedup process generating these comments is not idempotent and not reading its own
prior output reliably. Separately, comment #6 (2026-08-04) records #399 as *filed* on
2026-08-03, i.e. **before** comment #10's D10 citation (2026-08-08) is invoked to argue the
demand gate no longer blocks filing. The issue's own §7 Re-planning Protocol says the gate
lifting must *precede* filing ("REPLAN here recording who asked; file the three tasks");
here a task was filed first and the gate-lifting justification produced five days later.
**Recommendation:** treat the comment thread as unreliable process history; before further
execution, have a human (not another automated pass) audit the actual current state of
#399/#403/#277/#569 against this issue's dependency graph, since the automated record has
already demonstrably drifted from its own stated protocol once.

### 4. Unfileable/untestable acceptance criteria as currently written

Completion Criteria item: *"Demand gate explicitly resolved — built because a named
requester asked (REPLAN records who), or this feature closed/parked with that noted; never
built speculatively."* Per D10 as comment #10 states it (*"Demand gates apply to
third-party asks, NOT to the maintainer's roadmap"*), this work is *never* going to have "a
named requester" — it is maintainer-initiated roadmap work by construction. As written, this
checkbox can only ever be satisfied via the "closed/parked" branch, never the "named
requester" branch, yet the body still presents both as live options and doesn't say which
one D10 forces. A gameable reading: anyone can tick this box by writing any REPLAN comment
that says "closed per D10" without an actual requester, and the box passes — the checklist
item currently has no way to distinguish "legitimately re-scoped by ruling" from "nobody
ever asked and nobody ever will." **Recommendation:** rewrite the item to name D10 and #485
directly as the resolution path, removing the now-dead "named requester" branch, or the
box is decorative.

### 5. Scope creep risk baked into the machine block itself

`serves_capstones: [224]` plus the extensive absorption of #330/#399 plus boundary notes
against #569/#403/#277 means this single issue now carries: an SPI, a sealed-class
workaround, a CLI flag, a save-format namespacing grammar change, a classloader trust
boundary, a security ratchet-test entry, and (per comment #12's table) explicit ownership
of "the ratchet and allowlist separation." That is substantially more than the original
three-task decomposition (SPI+discovery / tests / docs) the body's own §2 argues *against*
merging into one task ("Rejected alternatives: a single monolithic task"). The issue has,
through comment-level absorption, become closer to the monolith its own rationale rejected.
**Recommendation:** when the demand-gate REPLAN happens, re-split into the tasks the body's
§2 already argued for, now against the comment #12 scope table rather than the stale body
table.

### 6. Minor: dead/stale evidence pins

The body is pinned at `29afb26`; comment #6 re-derives at `2d0ca9d`; comment #10 re-derives
again at `2d0ca9d` explicitly noting it is "not present at `2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`"
for the D10 ruling — i.e., the body's own evidence commit predates the ruling that changes
its central open question. This is consistent with finding #1/#2 above rather than a new
defect, but flags that "evidence_commit: 29afb26" in the machine block is stale and should
be bumped as part of any REPLAN.

## What is solid

- The technical premises are accurate and well-evidenced: the closed `ElementRegistry`
  literal, the sealed `Element`/`LogicElement` hierarchy, the live but unused
  `ELEMENT_PROVIDER` extension point, and the total absence of `ServiceLoader` discovery
  code are all verifiably true against the current tree, down to line numbers.
- The #222 trust-boundary framing (in-process trusted-extension opt-in, no sandboxing
  promised) is a defensible, already-ratified design decision (`ARCHITECTURE.md:295`) and
  this issue correctly treats it as settled rather than re-litigating it.
- The boundary-drawing against #569 (mechanism vs. publication/stability-promise) and
  against #223 (catalog vs. external discovery) is genuinely well-reasoned and testable —
  each cites a concrete acceptance criterion that would fail if the boundary were wrong.
- The `ForeignElement` design alternative analysis (comment #8) — unseal vs. one bounded
  in-tree adapter class — correctly identifies the real cost (losing exhaustive-switch
  totality) of the rejected option and picks the cheaper one.

## Verdict rationale

The engineering substrate this issue sits on is real and accurately described. But the
issue's actual go/no-go logic depends on a citation that no longer resolves in the
repository (#1), the authoritative body has drifted materially from the scope its own
comments now claim (#2, including a direct contradiction on whether save-file syntax
changes), the automated process maintaining this issue has already run its own protocol
out of order and duplicated work on itself (#3), and a Definition-of-Done item is
currently unfalsifiable (#4). None of this blocks the *technical* design, but all of it
blocks trusting this issue, as currently written, as an executable plan. **needs-rework**:
resync the body to the comment-12 scope table, fix the D10 citation, and audit the
absorption history before filing or executing further children.
