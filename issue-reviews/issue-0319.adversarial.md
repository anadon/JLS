# Issue #319: FEAT-013: a saved file stops being accepted or refused as one unit — unknown optional sections are skipped and preserved, unknown required sections are refused by name
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of the ask

Replace the single global `FORMAT` version integer with a header followed by
named, independently-versioned sections, each carrying a must-understand
(optional/required) flag. Unknown optional sections round-trip verbatim;
unknown required sections are refused by name. Three not-yet-filed children
(TASK-0033/0034/0071) plus an epoch policy and a raw binary `IMAGE` section.
The core code citations I could check hold on current `master`:
`FORMAT_VERSION = 2` at `src/jls/Circuit.java:102`, the refuse-newer check at
`:765` (`if (version > FORMAT_VERSION)`), `MAX_CIRCUIT_TEXT_BYTES = 64L << 20`
at `src/jls/FileAbstractor.java:65`, and the `initrle` silent-drop path at
`src/jls/elem/Memory.java:420-460`. The design itself — must-understand
semantics, the identity-hash fold restricted to participating sections, the
62.5 MB/64 MB size arithmetic for a 16 MiB image — is careful and internally
consistent. The problems below are about governance/traceability of the
issue text itself and about scope/feasibility, not about the core idea.

## Findings, most severe first

### 1. The issue body is already stale on its own most basic status field — contradicted by its own comment thread

The body's §2 roster and the `planned_tasks` YAML list all three children as
**"not filed"**:

> `TASK-0033 (planned, not filed) | ... | not filed`
> `TASK-0034 (planned, not filed) | ... | not filed`
> `TASK-0071 (planned, not filed) | ... | not filed`

But the issue's own third comment (2026-08-04, `#issuecomment-5181347953`)
states plainly that all three have already been filed:

> "TASK-0033 | Filed as **#444** ... | TASK-0034 | Filed as **#445** ... |
> TASK-0071 | Filed as **#395**"

I confirmed #444 exists and is titled *"TASK-0033: a saved file stops being
one indivisible unit — sections carry their own version and a must-understand
flag..."* — a real, filed, more-detailed elaboration of the same task, not a
duplicate note. So the document a reviewer or implementer reads first (the
issue body) tells them to go file three sub-issues that already exist. A
scheduler acting literally on the body risks re-filing duplicates; a human
has to know to scroll to comment 3 to learn the roster is wrong.
This also means the issue currently fails its own Definition-of-Done item
*"planned_tasks empty (each resolved to a filed issue or descoped)"* in a way
that a body edit — not a new comment — should have fixed at the time #444/
#445/#395 were filed. **Recommendation:** edit the body's roster table and
`planned_tasks` block to point at #444/#445/#395 (or close them out) rather
than leaving the correction buried in a later comment; this is exactly the
kind of half-edge the issue's own §7 protocol says to avoid ("a half-edge is
the defect this Link pass exists to prevent" — stated there for DAG edges,
but the same failure mode applies to the roster table).

### 2. The evidence commit is unverifiable today, a fact the issue already knows and has not fixed

`evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` is cited roughly a
dozen times in the body as the pin for every code claim. In this checkout:

```
$ git log -1 2d0ca9d
fatal: ambiguous argument '2d0ca9d': unknown revision or path not in the working tree.
$ git merge-base --is-ancestor 2d0ca9d HEAD
not ancestor  (object does not even resolve)
```

The issue's own first comment (2026-08-03) already documents why: that commit
"exists only on a branch scheduled for deletion and is **not** an ancestor of
`master`... once the branch is deleted, every `2d0ca9d`-pinned permalink and
every 're-derive at this commit' instruction in this issue resolves to
nothing." That comment only patches the machine-block/mermaid content it
touched; the body's ~12 other `2d0ca9d` pins, including the size-arithmetic
citation to `docs/plan/evidence/diff-stability.md` R6, are left dangling.
The issue's own Completion Criteria requires *"Every cited evidence document
and permalink resolves on the default branch at close"* — a bar the primary
evidence commit already fails to clear, before any child has even landed.
Practically this doesn't invalidate the claims (I independently re-verified
the surviving ones against current `master` and they hold), but it means
nobody can cheaply re-verify the rest of the citations (the diff-stability
measurement, the `2d0ca9d`-pinned "ABSENT at 2d0ca9d" `git grep` outputs)
without reconstructing a deleted branch. **Recommendation:** re-pin the
evidence commit to a `master`-reachable commit before work starts, or add an
explicit `REPLAN:` doing so now rather than waiting for close.

### 3. Three of the seven acceptance criteria are explicitly orphaned — assigned to "this issue's close-out" with no task that owns writing them

§5's table marks I1 (byte independence), I5 (identity hash unaffected by a
non-participating checkpoint section), and I6 (migration test) as "Does not
exist; this issue's close-out" — as opposed to being pinned to TASK-0033/34/71
like every other row. The prose explains why (they're properties of the
*assembled* frame, not of any one child), which is a defensible design
argument, but it also means these three tests have no owner in the roster:
TASK-0033 (#444), TASK-0034 (#445) and TASK-0071 (#395) each have their own
completion criteria, and none of the three bodies I'd expect to check (I
didn't fetch #445/#395 in full, but #444's stated scope is the frame +
epoch policy + migration, which overlaps I6 but not I1/I5) is on record as
committing to write I1 or I5. If every child lands "green" per its own
issue, there's a real risk this issue's close-out discovers I1/I5 were never
built by anyone and has to invent them under time pressure at the end of the
critical path. **Recommendation:** either assign I1/I5/I6 explicitly to one
of the three filed children now, or file a fourth "integration" task that
owns exactly the rule-B criteria, so "this issue's close-out" isn't a bucket
nobody is resourced against.

### 4. Preservation criterion has a gameable edge at the frame/section boundary

Global invariant 3 states: *"A preserved unknown section is preserved
exactly: same bytes, same position relative to the sections around it.
Reordering or re-indenting it is a correctness defect, not a formatting
choice."* This is precise about the section's own payload bytes. It is silent
about the section's **surrounding frame syntax** — the delimiter/header
tokens the reader itself writes around a preserved section (e.g. any
re-serialized length prefix, whitespace between the frame keyword and the
opaque payload, or a normalized required/optional flag spelling). A build
could satisfy "same bytes, same position" for the payload while still
reformatting the frame wrapper, and I1's byte-comparison test as described
("editing section A and comparing section B's stored frame byte for byte")
only tests non-adjacent sections, not whether an untouched section's own
*frame* bytes survive a save that touches a different section. **Recommendation:**
tighten invariant 3 (or I2) to state explicitly that the *entire* on-disk span
for an unknown/untouched section — frame delimiters included, not just the
opaque payload — must be byte-identical after a save that edits elsewhere,
and add that specific case to I2's fixture rather than leaving it implied.

### 5. Feasibility: the issue reports only its own cost, not the true critical-path cost before it is even startable

§5 (Cost) states *"Band 4-7 maintainer-weeks... TASK-0033 (2 wk) + TASK-0034
(1.5 wk) + TASK-0071 (2 wk) = 5.5 wk... no reconciliation is needed."* That
is #319's own spend. But `blocked_by: [334]`, and #334 is itself
`blocked_by: [315]`; I confirmed both #334 and #315 are **open**, with their
own unfiled/unlanded task pairs (#334: 3 wk banded 2-4 mw; #315: 2.1 wk banded
1-2 mw, itself already over its own stated band per #315's Open Question 1).
So the real lead time before #319's own 5.5 mw can start is #315's ~2.1 mw
plus #334's ~3 mw plus whatever coordination tax three sequential
single-maintainer features carry — roughly 10+ mw end-to-end before the six
capstones and seven downstream features this issue gates on can proceed, none
of which is stated anywhere in #319 itself. For a project the README
describes as "a single-maintainer pedagogy tool," a ten-week-plus serial
critical path sitting in front of six capstones is a real scheduling risk
that the issue's own "Cost" section — scoped only to itself — actively
obscures. **Recommendation:** state the cumulative blocked-by cost (or link to
wherever the master plan totals it) so anyone deciding whether to greenlight
#319 now isn't misled by a "no reconciliation needed" note that only covers
a third of the real wait.

### 6. Minor: process risk visible elsewhere in the same document, not just in finding #1

The DAG-walk prose, the mermaid graph, and the roster table are three
independent hand/LLM-maintained restatements of the same dependency
information (already flagged by the issue's own §7 as needing synchronized
`REPLAN:` edits on both sides of a mirrored edge). Finding #1 shows the
roster table can drift from reality (comments correcting it while the body
stays stale); nothing here suggests the DAG walk or mermaid graph have
drifted too — I checked the currently-declared `blocks`/`blocked_by`
edges against #334 and #315's own bodies and they agree — but the pattern
that already caused one drift (three restatements, one source of truth
implied but not enforced) is a standing risk for this issue's remaining
lifetime, not a one-off.

## What's solid (brief)

- The core section-frame formalism (§3) is mathematically precise and
  internally consistent — the load-dispatch case split, the byte-independence
  claim, and the identity-hash fold restricted to `h_i = 1` sections are all
  well-specified enough to implement against directly.
- The `blocked_by: [334]` edge is justified with a concrete, checkable reason
  (avoiding a double golden-corpus rewrite) and is mirrored correctly in
  #334's own `blocks` list — I verified this by reading #334's body.
  Sequencing reasoning here is sound.
- The out-of-scope list (§1) is disciplined: it explicitly declines to
  redesign the whole grammar, flip the default container, or define any
  section's content besides the one raw-image worked example, each with a
  one-line reason. This keeps the issue from creeping into #334's or #363's
  territory, and the boundary comments against #334/#363/#314/#340 (comment 2)
  back this up with a specific, checked-against-each-target argument rather
  than an assertion.
- The 62.5 MB / 93.2% of the 64 MiB text cap arithmetic for a single 16 MiB
  memory image is a genuinely load-bearing, concrete number that motivates
  the raw-section mechanism — this isn't a hand-wavy justification.
