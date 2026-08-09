# Issue #361: FEAT-029: a balanced-ternary datapath is something you draw, clock, probe, dump and test with the same palette, viewer and grammar as any binary circuit
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is open, well-organized as a "feature" tier document, and its
top-level pedagogy invariants (default palette unchanged, radix-2
byte-identical, no golden moves) are genuinely good discipline. But the
issue as currently *filed* — its YAML machine block, §3 data contract, and
Completion Criteria — describes a design that its own second comment says
is no longer the plan, and part of its cited "Evidence" section is
code that does not exist on `master` at all. Filing tasks against the body
as written would build the wrong kernel and cite a nonexistent HdlExporter
policy.

## Findings, most severe first

**1. [Critical] Global Invariant 6 rests on an `HdlExporter` policy that does not exist in this tree.**
The body's "Evidence" section and §3 both assert a *four*-way class-set
policy — `EXPORTED` (`:429`), `SKIPPED` (`:438`), `TOPOLOGY` (`:443`) and
`REJECTED` (`:460`) — with a throw reading
`String reason = REJECTED.get(el.getClass());` at `:196`. I read
`src/jls/hdl/HdlExporter.java` directly: it has exactly **three** sets
(`EXPORTED` at `:422`, `SKIPPED` at `:431`, `TOPOLOGY` at `:436`); there is
no `REJECTED` set anywhere in the file (`grep -n REJECTED` returns
nothing), and the actual throw at `:194` builds a generic
`"contains elements HDL export does not support yet"` message from an
`offenders` list, not a `REJECTED` lookup. This is not a stale reading on
my part — the issue's own first comment (from the repo owner, citing
#493) confirms it: the `evidence_commit`
(`2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`) "exists only on a branch that
will not be merged and will be deleted," and explicitly names
`HdlExporter.java:452-460` and the symbol `REJECTED.get(el` as
**branch-only, absent from master**. Invariant 6 as written ("every new
class has an explicit row in exactly one of HdlExporter's four class
sets") cannot be satisfied because the fourth set is fiction on the
branch this repository actually builds from.
**Recommendation:** before TASK-0062 is filed, either add the `REJECTED`
bucket for real (tracked as its own change, referencing the issue that
owns it — the comment names #492) or rewrite invariant 6 and §3 around the
three sets that actually exist.

**2. [Critical] A substantive redesign was posted as a comment and never folded into the authoritative body.**
The issue's second comment ("Amendment — kernel ops restated over
intervals; bridge element added; new beneficiary") replaces the entire
§3 mathematical contract — the three-plane code-point encoding
$c = a+2b+4u$, the radix≥6 refusal arithmetic, the
$\mathrm{enc}:\{-,0,+\}\to\{0,1\}^2$ lowering — with an interval model
(`min`/`max`/`reflect`/`cycle`/`literal` over an ordered range, balanced
ternary as the native `[-1,+1]` interval rather than a rendering). It also
drops TASK-0083 (shared with FEAT-039) and swaps the sole beneficiary from
CAP-03 (#295) to a new capstone CAP-39 (#888). I verified #295 is in fact
**closed, `state_reason: not_planned`**, and #888 exists and explicitly
says "supersedes_disposition: #295 (CAP-03) stays CLOSED." None of this
made it back into the issue body: the YAML machine block still reads
`serves_capstones: [295]`, the roster table still lists TASK-0083, the
"Why one capstone" prose still argues from CAP-03, and the Completion
Criteria still require reconciling §3 as originally written. The issue's
own Definition of Done says "Machine block, roster table, and mermaid
graph agree with reality at close," and §7 requires a capstone-descope
REPLAN — neither has happened. As filed, a reader who stops at the body
(most of it) will plan the wrong kernel against a dead capstone.
**Recommendation:** this issue needs an actual `REPLAN:` edit to the body
— YAML, §3 math, roster, mermaid, and `serves_capstones` — not a
trailing comment, before any child task is filed.

**3. [High] The whole dependency chain underneath #361 is itself unbuilt and in motion.**
`blocked_by: [344]` is real and correctly mirrored (I confirmed #344 is
open and names `blocks: [361]`), but #344's own three tasks are all
"unfiled"/"ABSENT at `2d0ca9d`," and per the CAP-39 issue, #344 itself "as
replanned to the signed-interval model" — i.e., #344 is *also* mid-REPLAN,
also not reflected in its own body when I fetched it. #344 in turn is
blocked by #322 (four-state value core), which per its own text has "no
Word record" yet. #361 therefore sits at the end of a three-feature chain
where none of the three has landed code and at least two of the three
have an unreconciled redesign in flight. Filing #361's tasks now risks
building against a kernel contract (plane encoding) that its own
prerequisite has already moved away from.

**4. [High] The "-t literal reuse" claim glosses over a real grammar collision.**
§3 and the "Consumes" section assert higher-radix literals "arrive the
same way" as hex, through "the existing token-rewrite pre-pass." I read
the actual mechanism: `docs/batch-interface.md` §2.1 and
`src/jls/elem/SigSim.java:48-51` show the pre-pass is a narrow regex,
`-?0[xX][0-9a-fA-F]+`, rewritten to decimal before a `BigInteger` parse
that already treats a leading `-` as sign (§2.4). A balanced-ternary
literal that legitimately starts with `-` (the digit, not the sign) is
ambiguous with that same character used as negation — the issue never
addresses this collision, and "the same way, not a new grammar
production" is not accurate for a genuinely new digit alphabet. (The
amendment comment tacitly concedes this by changing tack to "signed
decimal bundle numerals... no per-digit vector syntax in v1" — again,
a change the body doesn't carry.)

**5. [Medium] Integration Criterion 2's completeness census is not yet specifiable, let alone gameable-proofed.**
"The family is complete, measured against a census... checked against
TASK-0083's per-boundary census file rather than a wish list" is a good
idea in principle, but per the amendment TASK-0083 is dropped/re-homed and
CAP-39 lists its own walkthrough content as **Open Question 6, marked
BLOCKING**. A criterion whose measuring instrument is explicitly not yet
written cannot be evaluated as "verified" or "refuted" today; as filed it
reads as satisfiable when it isn't yet well-defined.

**6. [Low] Label surface is narrower than the actual diff.**
Labeled `area:sim` only, but §3's "Modifies" list touches `jls.edit`
(`Palette.java`, `PaletteEntry.java`, `SimpleEditor.java`'s connection
sites), `jls.hdl` (`HdlExporter.java`), and `pom.xml` coverage floors at
least as much as `jls.sim`. Not blocking, just likely to under-route
reviewers.

## What's solid

- Global Invariants 1–3 (no radix on existing types, default palette
  unchanged, every existing golden byte-identical) are precise and
  mechanically testable — exactly the kind of criterion that can't be
  gamed by a "looks done" PR.
- The export-is-a-lowering framing ("An external tool consuming that
  netlist is simulating an encoding, not ternary") is an honest scope
  statement, and matches how `HdlExporter` actually behaves today (a
  single-pass, headless, subprocess-boundary exporter with no partial
  netlist concept).
- Reusing the existing hex-literal rewrite mechanism (finding 4 aside on
  the ambiguity) is at least grounded in a real, working piece of the
  codebase rather than invented machinery.
- `blocked_by`/`blocks` mirroring between #344 and #361 is done correctly
  at the edge level, even though the *content* behind those edges has
  since moved (finding 3).

## Verdict rationale

`needs-rework`. The issue cannot be actioned as filed: its central
technical citation for Global Invariant 6 doesn't exist on `master`
(finding 1, confirmed independently and by the project's own tracking
comment), and a real design pivot — kernel model, dropped task, dead
capstone — exists only as an unreconciled trailing comment rather than in
the body a task-filer would actually read (finding 2). Both are fixable
by editing the issue, not by starting implementation; nothing here rises
to "should-not-proceed" since the underlying feature concept and its
pedagogy invariants are sound.
