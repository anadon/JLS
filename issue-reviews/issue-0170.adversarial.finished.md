# Issue #170: Collaboration security hardening: closed op vocabulary, element-type allowlist for network input, caps, ratchet tests (collab cross-cutting)
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

This is a feature-tier tracking issue for the security-hardening slice of the
collaborative-editing program (#163). The body claims four pieces of H1/H2/P3/P4
hardening are already landed (allowlist, three of four cap dimensions, ratchets,
listener-hygiene pin), and lists four remaining slices (fuzz-lane re-landing,
per-peer rate cap, misbehavior policy, snapshot wiring). Against the checked-out
tree at HEAD (53116252), the "landed" claims substantially check out. The
problems are (a) a factual/numeric error in the flagship "landed" artifact's own
description, (b) a security-relevant follow-on obligation that surfaced in later
comments but was never folded back into the issue's own scope block, and (c) a
completion-criteria design that is largely self-attested rather than
test-enforced — which the issue's own comment history has already exploited
once (the vanished-evidence episode) and remains structurally exposed to again.

## Findings, most severe first

### 1. [HIGH] The machine-readable scope block is already stale relative to the issue's own most recent comments

The issue body's YAML carries `blocked_by: []`, `serves_capstones: [163, 224]`,
and a four-item `planned_tasks` list (fuzz lane, rate cap, misbehavior policy,
snapshot wiring). But the two most recent comments on this very issue
(2026-08-04 `#5176087052`, 2026-08-08 `#5227020572`/`#5227466777`) say, in the
issue's own words:

> "Add #299 to `serves_capstones`... #163 is the pure-P2P tracking issue, not
> one of the nineteen filed capstones (#295–#313) — so on the filed-capstone
> roster this feature currently has **no required consumer of record**."

> "Inbound ordering to add, mirrored: `blocked_by: [315]`... a genuine
> upstream, because delegating the allowlist to the registry without a tested
> deny list silently admits every type the registry gains."

> "**#467 (TASK-0110)**... has been given the matching `part_of_feature: 170`
> correction."

None of these three corrections (capstone fix, new `blocked_by` edge, adoption
of task #467) appear in the issue body's machine block, `planned_tasks`, or
Completion Criteria checklist. The issue's own Definition of Done requires
"Machine block, roster table, and mermaid graph agree with reality at close" —
by that self-imposed bar, the issue is already out of sync with its own
history while still open. Anyone triaging or picking up work from the body
alone (rather than reading all 13 comments) will get an incomplete picture of
current scope and dependencies.

**Recommendation:** before further work lands, edit the issue body (not just
comment on it) to fold the three REPLAN corrections into the actual YAML block,
`planned_tasks`, and Completion Criteria — the artifact the template treats as
authoritative should not require reconstructing 13 comments to trust.

### 2. [HIGH] The H1 "landed" description miscounts its own flagship artifact

Body text: *"[`ElementVocabulary`]... is the closed 32-token vocabulary (31
palette elements + `WireEnd`)"*, citing `ElementVocabulary.java#L31` and `#L87`.

Verified against `src/jls/collab/op/ElementVocabulary.java:39-46` at HEAD: the
`ALLOWED` set literal contains **34** tokens, not 32:

```
Adder, AndGate, Binder, Clock, Constant, Decoder, DelayGate, Display,
Extend, FieldExtend, InputPin, JumpEnd, JumpStart, Memory, Mux, NandGate,
NorGate, NotGate, OrGate, OutputPin, Pause, Register, RegisterFile,
ShiftRegister, SigGen, Splitter, StateMachine, Stop, SubCircuit, Text,
TriState, TruthTable, WireEnd, XorGate   (count = 34)
```

The adjacent test itself (`test/jls/collab/op/ElementVocabularyTest.java:119-133`,
`vocabularyIsThePaletteSetPlusWireEnd`) documents the real composition as
"palette set... plus SubCircuit (user-creatable through the hand-coded Import
button rather than a palette entry) plus WireEnd" — i.e. 32 palette elements +
SubCircuit + WireEnd = 34, not "31 palette + WireEnd = 32" as the issue body
states. Later comments on this same issue (`#5175818190`, `#5227020572`)
correctly cite 34 (against a 35-token registry), so the error is isolated to
the top-level body's H1 bullet, but it is the exact security allowlist under
review, and it is wrong about its own size and composition.

**Recommendation:** correct the H1 bullet's token count and composition
description to match the code and the later comments' own (correct) 34-vs-35
arithmetic, so a reader trusting only the body isn't misled about what the
allowlist actually admits.

### 3. [HIGH] A documented self-widening landmine on the security boundary is not tracked as issue scope

`ElementVocabulary.java:26-29`'s own javadoc instructs a future maintainer:

> "when the registry lands, this class should delegate to it and the
> reconciliation is to be recorded on issue #78."

The registry (#78) **has landed**. Following that instruction literally would
change the allowlist from a hand-written 34-token list to a bare delegation to
the 35-token registry, silently admitting `TestGen` — a batch-only,
non-palette type the editor cannot even create, and exactly the class of
network-hostile input this issue exists to keep out. This is not speculative:
the issue's own later comments and the adopted task #467 call it "a security
regression" and state "the javadoc is actively misleading until it is
updated" (#467 §11 Threats to Validity).

Yet the issue body's H1 bullet reproduces this exact javadoc passage as if it
were merely archival ("this is the stopgap constant list... until the
registry... lands"), and neither `planned_tasks` nor the Completion Criteria
checklist in the body names "retire the delegate-to-registry instruction" or
"add a tested, non-empty deny list (IC-5/IC-6)" as outstanding work. A reader
who trusts only the body would reasonably conclude H1 is finished and closed —
it is finished only in the narrow sense that nobody has yet followed the
instruction it contains to weaken itself.

**Recommendation:** add an explicit `planned_tasks` / Completion Criteria
entry for the deny-list fix and the javadoc correction, independent of
whatever happens with task #467's much larger CRDT-convergence scope (see
finding 6) — this item should not be allowed to ride along on an unrelated
task's critical path.

### 4. [MEDIUM] Completion Criteria are largely procedural attestations, not falsifiable checks

Most of the Definition of Done bullets are self-reported process claims rather
than tests: "every capstone... notified with a `STATUS:` comment," "every
skipped or waived criterion carries a `WAIVED:` comment," "machine block...
agree with reality at close." None of these is CI-checked; all can be
satisfied by writing a comment. The concrete, test-backed criteria (P2 fuzz
lane exists with recorded seeds and RSS/thread bounds; H2 rate cap; misbehavior
disconnect policy; snapshot wiring) are appropriately concrete, but they sit
alongside criteria that are gameable by construction. Finding 1 above is a
live demonstration: the "machine block agrees with reality" bullet is
currently false while the issue is open, and nothing enforces it.

**Recommendation:** for the process-only bullets, at minimum require the
closing comment to quote or diff the current machine block against the
roster of REPLAN comments, rather than accepting a bare checkbox tick.

### 5. [MEDIUM] Vanished-evidence risk is handled reactively, not preventively, despite one confirmed occurrence in this issue's own history

The Background section documents, admirably honestly, that a 2026-07-19
comment (`#5016880492`) claimed the P2 fuzz lane complete on branch
`wip/issue-170` at commit `a671ae2` with detailed run statistics — and that
branch/commit exist in no ref; the claim was void. Confirmed independently
here: `git ls-files | grep -i CollabFrameFuzz` returns nothing, and
`git log --all --oneline` shows no commit `a671ae2`. The self-correction
(8 days later, `#5093080408`) is good practice, but the Re-planning Protocol's
only stated response to a recurrence is retrospective ("treat the claim as
void... record it... keep scope open") — there is no proposed independent
verification gate (e.g., requiring a CI run link, a PR number, or a
maintainer-reproducible command before a "landed" claim is accepted) to make a
second occurrence less likely. Given it already happened once in exactly this
issue, "detect and record" is a weaker mitigation than the issue's own
"vanished-evidence" framing implies is needed.

### 6. [MEDIUM] Newly adopted completion criterion is over-blocked by an unrelated task's dependency chain

Comment `#5227466777` adopts task #467 (`part_of_feature: 170`) to carry
forward the deny-list/allowlist-correctness work (finding 3). But #467's own
body shows it is primarily a #171 (CRDT convergence, per-user undo) deliverable
— of its ~15 predictions (P1–P15) and ~20 Completion Criteria items, only one
(P11, the allowlist deny-list assertion) is this issue's. #467 is itself
`blocked_by: [415, 435]` (an unfiled merge-rule table and an unfiled two-replica
harness), neither of which the deny-list fix actually needs — adding a named
constant `DENIED = Set.of("TestGen")` and a `requireAllowed` check against
`ElementVocabulary.allowedTypes() \ DENIED` has no dependency on CRDT merge
semantics. By inheriting #467 wholesale as its vehicle for IC-5/IC-6, this
issue has implicitly gated a small, independently-landable security fix behind
a much larger and currently unfiled body of Stage-2 replication work. The
issue's own boundary comments (`#5181642971`, `#5227020572`) explicitly warn
against exactly this kind of straddle ("do not merge #467 into #171... do not
merge #171 into this issue") but do not resolve the sequencing hazard it
creates for #170's slice specifically.

**Recommendation:** split the allowlist deny-list fix out as its own
small, unblocked task under #170 rather than relying on #467 to eventually
discharge it as a side effect of unrelated CRDT work.

### 7. [LOW, positive] Core "landed" evidence checks out against the tree

Spot-verified and correct: `ElementVocabulary.requireAllowed` (`ElementBlocks.java:116`
calls it before `ElementRegistry.forTag`), `Circuit.java:918` routing through
`ElementRegistry.forTag` rather than `Class.forName`, `SecureLink.MAX_PAYLOAD_BYTES`
= 1<<20 with enforcement at line 153, `CausalBuffer.MAX_PENDING` = 10_000 with
refusal at line 75, `CircuitOpReader.MAX_IDS/MAX_STRING/MAX_BLOCKS`, `ElementBlocks.MAX_BLOCK`,
`CollabSecurityRatchetTest` (ObjectInputStream ban, socket confinement,
zero-reflection-under-jls.collab), `ArchitectureRulesTest`'s bytecode mirrors, and the
`ElementVocabularyTest` TestGen witness (`jls.elem.TestGen` passes the file-load
gate's checks but is rejected by the allowlist) all match what the body
describes and cite correctly by file/line. This is solid, checkable citation
discipline for the parts that are actually landed.

### 8. [LOW, positive] The vanished-evidence episode is preserved rather than scrubbed

The Background section keeps the record of the false P2 claim rather than
quietly editing it away, and explicitly downgrades its corpus/seed design to
"a usable spec... not evidence." This is good integrity practice worth noting
independent of finding 5's critique of the *systemic* mitigation.

## Verdict rationale

The issue is not broken in the sense of contradicting the codebase on its core
claims — the landed evidence is real and well-cited. It is
sound-with-concerns because: its own "landed" H1 evidence contains a factual
error about the very artifact under review; a security-relevant follow-on
obligation (the deny-list fix) that the issue's own comments identify as
urgent ("the javadoc is actively misleading until it is updated") has not been
folded into the issue's tracked scope; the issue's own self-monitoring
mechanism (machine block matching reality) has already lapsed once while open;
and its completion criteria are largely unenforced attestations. None of these
rises to "needs-rework" — the remaining planned slices (fuzz lane, rate cap,
misbehavior policy, snapshot wiring) are well-scoped and independently
landable — but a maintainer picking this up should not treat the body as a
complete or current statement of scope without also reading the comment
thread, and should split out the deny-list fix rather than let it ride on
#467.
