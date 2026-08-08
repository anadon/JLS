# Issue #629: TASK-C561-1: Falstad's compact text format parses as untrusted input — a non-XML, non-JSON source refuses loudly on malformed lines
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#629 is the parsing task of FEAT-C29-4 (#561): turn Falstad's line-oriented
circuit text into a hardened, in-memory model that TASK-C561-2 (#631) later
maps to JLS elements and TASK-C561-3 (#633) later reports losses from. No
Falstad-related code, fixture, grammar, or reference exists anywhere in this
checkout (`grep -ril falstad src/ test/ docs/` returns only prior review
files under `issue-reviews/`), so every claim below is about the spec. The
sharpest problem is internal to this issue's own AC-1: it names "unknown
element codes" as a hostile-input attack vector to refuse loudly on, but the
entire feature family's premise — stated in #561's boundary notes and
exercised by #631/#633 — is that legitimate-but-unmapped Falstad codes
(analog elements) must survive parsing to become named, reported losses
downstream, not parse-time refusals. As written, AC-1 can be satisfied by an
implementation that forecloses the feature it belongs to.

## Findings, most severe first

### 1. [HIGH] AC-1's "unknown element codes" attack vector is not distinguished from "known-but-unmapped" codes, and satisfying it as written can break the parent feature's core premise

AC-1: *"Malformed, truncated and hostile inputs refuse loudly ... — unbounded
line length, unbounded line count, out-of-range numeric fields, unknown
element codes ..."* This groups "unknown element codes" with the other three
genuinely hostile-input vectors, implying a parser that refuses the whole
parse the moment it meets an element code it doesn't recognize.

But #561 (the parent feature, this issue's own `part_of_feature`) states in
its boundary notes: *"Analog import is out of scope permanently for this
feature: analog elements are named losses by design"* — meaning a real
Falstad file containing a resistor, capacitor, or op-amp is not malformed
input, it is an ordinary file the feature must accept and later report on
(TASK-C561-3, #633). Those analog element codes are *known to Falstad*, just
unmapped in JLS's logic subset. If TASK-C561-1's own element grammar only
"claims" the logic-subset codes (gates, FFs, counters, labeled nodes,
logic I/O — the list #631 enumerates) and treats every other real Falstad
code as an AC-1 "unknown element code" to refuse loudly on, then no
Falstad file containing so much as one resistor could ever reach TASK-C561-2's
mapping stage or TASK-C561-3's loss report — the entire "analog is a named
loss, not a failure" premise never fires, because the file was already
rejected wholesale at parse time. The issue never states which of these two
readings is intended, and nothing forces the "grammar it claims" (Outcome
paragraph) to be the *full* Falstad element set rather than only the mapped
subset.

**Recommendation:** AC-1 must explicitly split "unknown element code" into
two disjoint cases with different outcomes: a code absent from Falstad's own
format (genuinely hostile/malformed — refuse) versus a code Falstad defines
but JLS's logic subset doesn't map (legitimate — parse successfully into the
model, let TASK-C561-2/TASK-C561-3 classify and report it as a loss). Name
which element codes belong to each set, or explicitly commit to parsing
against the *entire* real Falstad grammar (digital and analog) rather than
only the mapped subset.

### 2. [HIGH] The issue's one stated dependency doesn't match what its own AC-4 needs, and the dependency it does name is not obviously load-bearing for what this task does

`ordering_after: [314]` — the only machine-block dependency #629 declares.
But AC-4 requires *"a source location per element expressive enough for
#556's `location` field"* — a schema #629 does not order after at all (#556
is FEAT-C29-1, absent from `ordering_after`). #556 itself is open and, per
its own review, `needs-rework` with an unresolved schema; #629 can be built
and closed against a location-field shape #556 has not yet fixed, exactly
the "designed against a guess" hazard the #633 review already flagged one
level downstream between #608 and #629 — except here it's #629 itself doing
the guessing, not being guessed about.

Conversely, #314 (FEAT-002, the fail-loud `Element.setValue` dispatch for
`.jls` attribute loading) is itself open and unstarted (its own body records
`TASK-0003 ... not filed`). It is unclear why TASK-C561-1 needs it at all:
this task's own boundary notes say *"Parsing only"* and #629 builds an
independent in-memory model of a foreign text format — it does not touch
`Element.setValue` or the `.jls` loader path #314 modifies. If the intent is
only "follow the same loud-failure discipline as #314 establishes," that is
a design-precedent citation, not a hard code dependency, and listing it in
`ordering_after` as if #629 cannot start until #314 lands is either overbroad
or unexplained.

**Recommendation:** add #556 to `ordering_after` (AC-4 cannot be honestly
implemented without its schema existing), and either justify the #314
dependency concretely (what code or contract does TASK-C561-1 actually reuse
from it) or drop it and cite it as a precedent instead.

### 3. [MED] No target Falstad format version or grammar reference is named, and this is the task where that gap has the sharpest consequences

The parent feature review (#561 Finding 4) already flags that "the Falstad
text format" spans multiple lineages (the original applet exporter vs. the
actively-developed CircuitJS1 fork, whose element codes and `$`-flag header
scheme have shifted across releases) with no version pinned anywhere in the
task family. That gap lands hardest here: #629 is the task that actually has
to write "the element grammar it claims" (Outcome text) and validate every
line against it (AC-1) — without a pinned dialect, there is no fixed
definition of what counts as a valid element code, what its numeric fields
are, or what their legal ranges are (AC-2's per-field bound fixtures). Two
implementers targeting different upstream snapshots could each fully satisfy
AC-1–AC-4 while parsing incompatible grammars.

**Recommendation:** pin a specific upstream format version/commit as the
target spec (mirroring `docs/file-format.md`'s own `FORMAT 1` versioning
discipline) before AC-1's grammar-validation claim can be verified against
anything concrete.

### 4. [MED] Cost band looks tight once Finding 1 is resolved either way

`band_mw: "1"`. Whichever way Finding 1 resolves, the scope is larger than
one maintainer-week: a full element grammar (once analog codes are included
per Finding 1's likely fix, that's on the order of 20+ Falstad element types,
not just the logic subset), a numeric-bound fixture per field per AC-2, four
attack-vector tests per AC-1, and a source-location data structure precise
enough for AC-4 once #556 exists (Finding 2). The parent feature (#561)
prices its whole three-task family at 2-3 mw with a 4.5 mw stop-loss
(KC-29-1); if TASK-C561-1 alone consumes 1 mw as declared, TASK-C561-2 (net
merging, refuse-vs-loss classification per the #631 review) and TASK-C561-3
(acceptance run, CI fixture, licensing per the #633 review) must together fit
in the remaining 1-2 mw, which both sibling reviews already found their own
ACs undersized for independently.

**Recommendation:** re-cost after Finding 1 is resolved (analog-inclusive
grammar vs. logic-only grammar are very different sizes), and state whether
`band_mw: "1"` assumes #556's schema and #314's precedent are already
landed.

### 5. [MED] No integration with the project's existing `LoadError` taxonomy is stated, risking a second, incompatible error-reporting path for one format

ARCHITECTURE.md documents `LoadError` (`src/jls/LoadError.java`) as "a fixed
category taxonomy tests assert on (`IO_ERROR`, `NOT_A_CIRCUIT`, `MALFORMED`,
`NEWER_FORMAT`, `UNKNOWN_ELEMENT`, `ELEMENT_ERROR`, `LIMIT_EXCEEDED`) ...
Published through `JLSInfo.setLoadError` ... so every front end shows the
same message." AC-1's four attack vectors map almost one-to-one onto existing
categories (unbounded line count/length → `LIMIT_EXCEEDED`, out-of-range
numeric fields → `MALFORMED`/`ELEMENT_ERROR`, unknown element codes →
`UNKNOWN_ELEMENT`), yet the issue never says whether TASK-C561-1's parser is
expected to raise through this existing taxonomy or invent its own. Given
`FileAbstractor`'s `MAX_CIRCUIT_TEXT_BYTES` (64 MiB, `src/jls/FileAbstractor.java:65`)
and `UntrustedFileHardeningTest` already establish the project's concrete
hardening pattern and numbers for the `.jls` container, a Falstad-specific
parser that reinvents bounds and error types from scratch — rather than
reusing `LoadError` categories and citing concrete byte/line numbers the way
`FileAbstractor` does — would fragment the "every front end shows the same
message" invariant this repository has already decided matters.

**Recommendation:** state explicitly that Falstad parse failures route
through `LoadError`/`TellUser` with the existing category taxonomy (adding a
category only if none fits), and give AC-1's bounds concrete numbers rather
than leaving "bounded in line count and line length" unquantified.

### 6. [LOW] No entry point for Falstad text into this parser is named anywhere in the task family

"Parsing only" (boundary notes) is a defensible scope cut, but across #629,
#631, and #633, no issue names how a user's Falstad text ever reaches this
code — no menu action, `File > Import` item, CLI flag, or clipboard-paste
path is mentioned. A fully spec-compliant, fully tested parser could be
merged with literally no way for a JLS user to invoke it, since the
UI/CLI plumbing question is not assigned to any of the three tasks.

**Recommendation:** name the entry point (even provisionally, e.g. "a menu
item is TASK-C561-3's concern") so the three-task decomposition is known to
cover the full path from user action to imported circuit, not just the
model transformation in the middle.

### 7. AC-2 and AC-3 are solid — no issue

AC-2 (fixture with extreme numeric values proving each field's bound) and
AC-3 (no partial model on failed parse) are both concrete and mechanically
checkable once a grammar exists. AC-3 in particular mirrors the project's
existing atomic-load discipline (`FileAbstractor.writeCircuit`'s
rename-over-target pattern, `docs/file-format.md` §1) applied to a new
source format — the right precedent to inherit.

## Verdict rationale

AC-2 and AC-3 are well-formed. But AC-1 as written — the issue's core
hostile-input claim — can be satisfied by an implementation that refuses
every real-world Falstad file containing an analog element, which is not a
hostile-input rejection but a wholesale defeat of the feature this task is
part of (Finding 1); the issue's own dependency graph names the wrong
prerequisite for what AC-4 actually needs and doesn't obviously need the one
it names (Finding 2); and the cost, grammar-version, and error-taxonomy gaps
(Findings 3-5) compound rather than stand alone, since none of them can be
priced or resolved until Finding 1 is settled. These are fixable by
sharpening AC-1's scope and the dependency list, not by discarding the task.
**needs-rework.**
