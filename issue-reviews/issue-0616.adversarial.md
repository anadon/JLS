# Issue #616: TASK-C487-4: the routed length comes back — a board's real geometry returns as a datum about the same net, so the lint judges the number the board actually has
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

`#616` is TASK-C487-4, the fourth (and last, independent) planned task under
FEAT-060 (`#487`, SI constraint authorship + PCB constraint export), itself
the second rung of CAP-18. Its job: import a routed board's measured
per-net length and make FEAT-058's (`#486`) electrical-length lint re-run
against it instead of the declared value. Read against `#487`, `#486`,
`#318` (FEAT-014, addressing) and `#336` (FEAT-004, net partition/naming) —
all fetched and cross-checked — the task's scope boundary and its two
"distinguishability" criteria are sound. But acceptance criterion 1 cites
the wrong prerequisite feature for the very mechanism it depends on, in a
way that would actively steer an implementer toward building the thing the
criterion explicitly says to avoid; one criterion is satisfiable by prose
alone with no code enforcement; and the task imports externally-sourced
data with no acceptance criterion addressing hostile-input handling, which
this codebase treats as a hard invariant for every new reader.

## Findings

### 1. [High] AC1 attributes "stable net identity" to the wrong feature — the one that explicitly does not provide it

AC1: *"matched by stable net identity (#336) rather than by name string."*
This is backwards. `#336`'s (FEAT-004) own body states under **Tracks
durably**: *"Nothing new in the save format. No new record kind, no new
element, no format-version bump. **Net names are derived at emission time,
never stored.**"* Its naming formula (`name(n) = u(n)` if user-supplied,
else `net_<digest>(min sid of driving elements)`) produces exactly a **name
string**, recomputed on demand — the thing AC1 says the match must *not*
be keyed on.

The actual persisted, minted-not-derived net identity lives in TASK-0035
(`#472`, filed under `#318`), whose own text draws this exact line:
*"#373 owns net naming; this owns net identity. A name is chosen for
humans and can change; an id is minted and must not."* (`#472`, §12
Related Work). `#472` defines `WireNet.getNetId()` backed by a new `nid`
save attribute, minted once and persisted — the mechanism AC1 actually
needs.

`#487`'s own machine block gets this distinction right elsewhere in the
same document: `blocked_by` lists `#336` for the constraint-vocabulary and
emitter tasks ("a name that does not survive save, load and export names
nothing") and separately lists `#318` for back-annotation ("the
back-annotated routed length is a second view's datum about a first
view's net"). `#616` is that back-annotation task, and its own
`ordering_after: [318, "TASK-C487-1"]` correctly omits `#336` — but its
prose acceptance criterion cites `#336` anyway, contradicting both its own
ordering list and its parent feature's careful separation.

**Why this matters, not just as a typo:** an implementer who trusts the
parenthetical over independently reading `#472`/`#318` would build the
matcher against `#336`'s derived, unstored name function — i.e. build
"matched by name string," the exact failure mode AC1 was written to rule
out. **Recommendation:** fix the citation to `#472`/`#318`, and add a test
fixture where the net's synthesized name changes (e.g. an unrelated
insertion that shifts which element's `sid` is the digest input, or a
before/after rename) while the routed-length match still succeeds — the
only way to actually falsify "matched by identity, not by name."

### 2. [Medium] "Only lengths" is satisfiable by documentation alone, with no code guard

AC4: *"a **test or a documented refusal** makes clear that traces, vias
and placement do not enter JLS through this path."* The disjunction means
a single sentence in `docs/file-format.md` discharges this criterion with
zero enforcement in code. A later PR that starts parsing trace/via geometry
out of the same back-annotation input (plausible, since board tools emit
all of it together) would violate the stated non-goal without breaking
any test — nothing here would go red. This is the same class of gap
`#487` itself is careful about elsewhere (e.g. its own Global Invariant 6,
"no SI-constraint surface is reachable from the default experience," is
backed by a named test, `SiPaletteVisibilityTest`, not a doc sentence).
**Recommendation:** require a negative test — parse a fixture containing
extra geometry fields and assert they are rejected or silently dropped
with a diagnostic, not merely documented as unsupported.

### 3. [Medium] No acceptance criterion addresses hostile input on a brand-new external-data reader

`#616` never specifies the input mechanism at all: no file format, no CLI
flag, no parser class is named anywhere in the issue (contrast `#487`,
which at least names `jls -export clk.net -si clk.kicad_dru`). Whatever
form "a board's real geometry" arrives in, it is by definition **untrusted
data from an external tool** — the same category ARCHITECTURE.md is
explicit about: *"Every reader enforces hostile-input caps (issue #38,
`UntrustedFileHardeningTest`)"* and load failures route through the
`LoadError` taxonomy rather than a stack trace. `#472` (the sibling task
this issue depends on for identity) is scrupulous about this for its own
new attribute (`nid`) — its §7.11 has a full failure-mode table for
malformed input. `#616` has none of that: none of its five acceptance
criteria mention a malformed, oversized, or maliciously-crafted
back-annotation input. **Recommendation:** add a criterion requiring the
import path to route failures through the existing `LoadError`/size-cap
discipline rather than leaving it unstated (and implicitly, unenforced).

### 4. [Medium] The foundation this task cites may close without ever proving the property AC1 needs

`#616` depends on `#318` (FEAT-014) for exactly the addressing/identity
half. But `#318`'s own Definition of Done allows its headline uniqueness
criterion — identity surviving a **shared, not copied**, subcircuit
instance, "the case flat ids fail" — to close as **"vacuous"** if no
shared-definition fixture exists yet (gated on `#357`/FEAT-017, which may
never land; flagged as a gameable criterion in this reviewer's own
`#318` adversarial pass). If `#318` closes with that criterion vacuous,
"stable net identity" is proven only in the easy, already-injective case.
A routed board built from a design using shared subcircuit definitions —
plausible for any nontrivial PCB — is exactly the case where `#616`'s
identity-matching promise (AC1) would be unverified. `#616` neither
acknowledges this dependency risk nor states what happens to its own DoD
if it inherits a `#318` closed with criterion 2 vacuous.
**Recommendation:** add a note to `#616`'s Definition of Done requiring
`#318`'s criterion-2 status (vacuous vs. live) to be recorded before
`#616` itself is counted closed.

### 5. [Low] No grounding in the actual repository

Every sibling issue fetched for this review (`#486`, `#487`, `#318`,
`#336`, `#472`) carries an "Evidence (re-verified at `<commit>`)" section
with concrete file/line citations proving the described gap is real at a
pinned commit (e.g. `#472`'s O1-O7, each a command transcript or a
`file.java:L119-126` quote). `#616` has none: no evidence commit, no file
path, no code citation anywhere in the body. For a task whose acceptance
criteria hinge on specific mechanisms in `WireNet`, the save-format
attribute grammar, and the batch `-check` entry point, this is a real gap
relative to the standard the rest of this planning corpus sets for itself
— nothing here is independently checkable against HEAD without first
reading three other issues. **Recommendation:** add at minimum a pointer
to the exact code this task will touch (`WireNet.java`, the batch
`-check` handler in `JLSStart`/wherever it lives) and confirm the ABSENT
state at a pinned commit, matching `#472`'s pattern.

### 6. [Low] Cost band is asserted with no reconciliation

`band_mw: "1-1.5"` is stated with no derivation shown, unlike every
feature-tier sibling in this corpus (`#486`, `#487`, `#336`, `#318` all
print multiple independent cost derivations and reconcile or flag
disagreement explicitly). Given the actual dependency depth — a new
persisted per-net identity mechanism this task consumes but does not own
(`#472`, priced separately at 2 wk and itself unlanded), a new import
parser, and a joint test with `#486`'s not-yet-built lint — 1-1.5 mw for
"one task" reads optimistic and is unaudited against any of the three
different derivation methods the parent documents use for the neighboring
figures.

## What's solid

- AC2 (declared value and routed value are always distinguishable, no
  silent overwrite path) is precise, testable, and matches the same
  fail-open-vs-fail-closed distinction `#487` itself draws between lint
  inputs and constraints.
- AC3's framing — "spans this task and #486, the test states which half
  it asserts" — correctly acknowledges shared ownership of a joint test
  rather than pretending this task alone can prove the loop closes; this
  mirrors `#487`'s own Integration Criterion 5 verbatim.
- The "lengths only in v1, never traces/vias/placement" scope boundary is
  a defensible, well-reasoned non-goal, consistent with `#487`'s Open
  Question 4 recommendation and stated for the right reason (avoiding
  turning JLS into a layout tool by accident).
- AC5 (absent back-annotation falls back to the declared value; a net
  with neither still reports "not assessable") is a meaningful
  regression guard specific to the new import path, not a trivial
  restatement of `#486`'s pre-existing default.

## Verdict rationale

Not `should-not-proceed`: the scope is coherent, the non-goals are
defensible, and two of the five criteria are genuinely solid. Not
`sound-with-concerns`: finding 1 is not a background nit — it sits
inside the acceptance-criterion text itself and, if followed literally,
would guide an implementer to build the exact anti-pattern ("matched by
name string") the criterion exists to forbid, because it cites the
feature that explicitly produces derived, unstored names instead of the
feature that mints persisted identity. Combined with a criterion
satisfiable by documentation alone (finding 2) and a wholly unaddressed
hostile-input surface for a brand-new external-data reader in a codebase
that treats that discipline as load-bearing (finding 3), this needs a
rework of the acceptance criteria before implementation starts, not just
monitoring during it.
