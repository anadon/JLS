# N-ary wires: tracker actions, decision log, and open questions

**Status: handoff brief for the session that drafts/updates issues.
Nothing has been filed or edited on the tracker yet — the maintainer
asked for discussion first; this document is the outcome of that
discussion (2026-08-08).** Companions:
[`nary-01-value-model.md`](nary-01-value-model.md) (the model),
[`nary-02-format-and-migration.md`](nary-02-format-and-migration.md)
(format/migration).

## 1. Context the next session must load first

- **The N-ary program already exists on the tracker, elaborated and
  priced.** Chain: **#322** (FEAT-026, four-state value core) →
  **#344** (FEAT-028, radix on ports/nets, bounded radix 2–5 via
  three-plane arithmetic) → **#361** (FEAT-029, N-ary element family)
  → #345 (FEAT-039, JLS-T3 — part of the closed capstone). #322, #344,
  #361 are **open**; read all three in full before editing anything.
- **Their capstone, CAP-03 (#295, balanced-ternary CPU), was closed
  *not planned*** by the August 2026 product review (#508), with a
  recorded re-open trigger ("CAP-02 completes and a real course
  requests MVL content"). #344's own re-planning protocol says that
  with CAP-03 descoped, #344/#361 "need a new beneficiary or a
  disposition." **This program is that disposition** — maintainer
  intent, superseding the recorded trigger; the REPLAN comments must
  say so explicitly so the record stays coherent (see §4).
- Design studies to cite rather than re-derive:
  `docs/capability-roadmap/keystone-a-design.md` (value type,
  representation benchmarks), `keystone-b-migration.md` (migration
  census and stay-green mechanics), `keystone-c-performance.md` (hot
  loop measurements that make the fast-tier constraint binding),
  `sweep-01-values-and-logic.md`. #344 cites
  `docs/plan/evidence/mvl-determination.md` for its stage table and
  the 9.79 ns/op lane-packed adder benchmark — **that file is not on
  the default branch at HEAD**; treat #344's quotations as the source
  of record unless the file is recovered from its landing commit
  (`3a81a4a`, per #344's own caveat).

## 2. Decision log (maintainer decisions from this discussion)

| # | Decision | Supersedes |
|---|---|---|
| D1 | The value model is a **contiguous bounded signed integer interval `[lo, hi]`** per net, one value held per position at a time, plus X/Z/U meta-states. "Truly generic" N is a hard requirement. | #344's radix `0..r-1` bounded at 5 |
| D2 | **Arbitrary real-valued (possibly discontiguous) level sets: considered and rejected.** Levels-as-reals belong to a future analog boundary, not the logic value domain. Recorded in `nary-01` §2 with rationale. | The intermediate design iteration of this discussion |
| D3 | **Negative digit values are in scope** (balanced ternary `[-1,+1]` native). | Balanced-as-display-convention |
| D4 | **Wire bundles are retained**: a net is `(interval, width)`; uniform interval per net; mixed alphabets compose through explicit bridge elements only. | — |
| D5 | **Generic contract, tiered representation**: #322's plane layout stays the binary fast path; a generic per-position symbol tier serves arbitrary N; a seeded differential oracle binds the two from the first dual-tier commit. Kernel-level radix cliff removed; **cliffs are element-level and refused per element with the arithmetic stated**. | #344's kernel-level refusal at radix ≥ 6 |
| D6 | **Non-binary nets are single-driver initially**; tri-state/strength/wired-logic stay binary-only. Resolution-as-data is a possible later additive feature, not scoped now. | — |
| D7 | Existing element types never gain an interval attribute; every pre-existing type reports `[0,1]` (registry-swept). New capability = new element types, default-hidden palette. | (carried unchanged from #361) |
| D8 | No file migration converter; FORMAT bump deferred to the first commit that can write a non-binary file; binary circuits byte-identical forever. | (carried unchanged from #344) |

## 3. Actions on existing issues

**#322 (FEAT-026) — scope unchanged, one amendment.** It remains the
prerequisite and continues to serve its other beneficiaries. Amendment
to its TASK-0056 scope (a comment, coordinated with #344's REPLAN): the
reservation made inside the value migration is **`lo()`/`hi()` interval
accessors returning `0`/`1`** (plus the frozen field list naming the
generic-tier extension point), superseding the `int radix()` returning
2 formulation. Same one-migration-not-two rationale, quoted from #344
§6.

**#344 (FEAT-028) — REPLAN, not replacement.** The feature's spine
survives: validated-never-widened net alphabet, the four editor refusal
sites with both alphabets named, load-time refusal, registry totality,
radix-2/`[0,1]` byte-identity, warm-loop benchmark gate, operator
kernel. Edits:

1. Retitle/reword radix → signed interval `[lo, hi]` (D1, D3).
2. Remove the radix-6 kernel refusal and the `ceil(log2(r+3))`
   scope bound; replace with the two-tier representation and the
   element-level refusal policy (D5). The plane arithmetic moves from
   scope-limit to fast-path implementation note.
3. Add the differential-oracle integration criterion (fast tier vs.
   generic tier, seeded corpus, seed recorded).
4. Add the single-driver-refusal rule for non-binary nets to the net
   validation scope (D6).
5. Record the new beneficiary (the capstone of §5) in
   `serves_capstones`, resolving the dangling-beneficiary state left by
   #295's closure; answer #344's Open Questions 2 and 3 (recommended
   defaults stand: reopen #221 once; enforced call-site policy with an
   architecture test).
6. Cost band: re-derive. The filed 8–12 mw band predates the generic
   tier; a rough planning delta for the generic tier + differential
   oracle + element-level refusal plumbing is +4–8 mw on this feature
   (the element-family and interop costs stay on #361).

**#361 (FEAT-029) — amend in place.** Keep: element family shape,
coverage-batch discipline, TASK-0105 palette-view prerequisite,
default-palette invariant, export-lowering-with-named-encoding, radix
manifest, `-t` pre-pass. Edits: kernel ops restated over intervals
(reflect/cyclic/literal per `nary-01` §4); balanced ternary becomes the
native `[-1,+1]` interval (balanced *rendering* stays, now as the
natural display); add the **bridge element** (generalized
splitter/binder across alphabets, `nary-01` §7) to the family roster;
add large-N notes (truth-table budget refusal; `Display`/`Constant`
interval-aware); drop the CAP-03-specific census task (TASK-0083) or
re-home it to whatever the new capstone's walkthrough needs.

**#295 / #508 — do not reopen CAP-03.** The new capstone (§5) is
deliberately smaller (no ISA, no emulator, no monitor, no filesystem).
Post a cross-reference comment on #295 noting that the value-domain
half of its territory proceeds under the new capstone by maintainer
decision, leaving the T3 platform closed and the re-open trigger
otherwise intact.

## 4. Governance motions (each is one recorded comment, done once)

1. **Reopen recorded decision #221's equivalence criterion once** —
   `ARCHITECTURE.md` "Recorded decisions" binds any execution strategy
   to "the two-states-plus-HiZ value domain"; re-anchor to the
   alphabet-parameterized §2 that #322+this-program produce. #344's
   Stage-0 costing ("the week is the #221 reopening at bus factor 1,
   not the code") already prices this.
2. **State the #508 trigger bypass** (§3 last item).
3. **`docs/simulation-semantics.md` §2 rewrite** lands with the code
   per its own if-code-and-doc-disagree rule — scheduled, not
   discovered (`sweep-01` "Ripple effects" is the checklist).

## 5. The capstone to draft (the one genuinely new filing)

Per the tier model (`.github/ISSUE_TEMPLATE/capstone.md`), the filing
needs a reviewer-executable walkthrough. Recommended shape, sized well
below CAP-03:

> **CAP-NN: a mixed-alphabet drawing — balanced-ternary datapath,
> byte-symbol peripheral wire, binary control — simulates, probes,
> autogrades and refuses honestly in one circuit, while every binary
> circuit on earth is byte-identical.**

Candidate walkthrough steps (to be tuned by the maintainer): load a
committed mixed fixture (ternary ALU slice + `[0,255]` bus wire through
a bridge + binary control); run batch against a byte-for-byte golden;
probe a ternary net and read `-`, `0`, `+`; disconnect a driver and
observe X propagate (not silent zero); attempt a ternary-to-binary
direct connection and read the refusal naming both intervals; attempt a
second driver on the ternary net and read that refusal; run the full
pre-existing golden corpus and observe zero byte moved; `lint radix`
over the fixture reporting zero implicit crossings (the CAP-03 step-5
lint, inherited).

Required features: #322 (shared with its existing beneficiaries), #344
as replanned, #361 as amended. No new feature issues are obviously
needed — the generic tier lands inside #344's REPLAN and the bridge
element inside #361 — but the drafting session should check that
against the task-space rules (#344 Open Question 1's closed task-space
caveat) and file a new feature only if the REPLAN would otherwise
absorb un-owned scope.

## 6. Open questions for the maintainer (recommended defaults marked)

1. **Max-N policy.** Default: endpoints are `int`, `N <= 2^31`,
   no kernel cliff; element-level budgets stated per element (D5).
   Decide the truth-table row budget number.
2. **Small-N fast path.** Does the plane fast tier cover `N <= 5` from
   day one, or binary-only first with `N <= 5` as a later optimization?
   Default: **binary-only first** — simplest correct thing; the
   differential oracle makes widening the fast tier safe later.
3. **`Extend` and sign-extension semantics for non-binary bundles.**
   Default: binary-only until a numeral-system-aware extension rule is
   specified (`nary-01` §7).
4. **`-t` literal syntax for N-ary pins.** Default: signed decimal
   bundle numerals via the token-rewrite pre-pass; no per-digit vector
   syntax in v1.
5. **VCD manifest form.** Default: machine-parseable `$comment`
   manifest (`nary-02` §3), documented in `docs/vcd-interop.md` as a
   profile extension.
6. **Capstone walkthrough final content** (§5) — the only blocking
   question: the acceptance demo defines the element roster's
   completeness census, replacing CAP-03's drawn-CPU census.
7. **First shipped intervals.** Default: `[0,1]` implicit, `[-1,+1]`
   and `[0,2]` and `[0,3]` in the first element batch, `[0,255]` with
   the bridge in the second (mirrors #361 Open Question 1's "kernel
   general, shipped elements narrow" stance).
