# Issue #746: TASK-C575-2: the sequential and FSM labs ship — the chapters where a grading vector file has to drive state, not just inputs
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's being asked

Ship "at least three" sequential labs plus "at least one" FSM lab (latches,
flip-flops, counters, registers, one designed state machine), each with
grading vectors that drive clocked behaviour over multiple cycles and a
reference-green/planted-defect-red CI lane, conforming to a lab layout that
TASK-C575-1 (#744) is supposed to have already defined.

## Findings, most severe first

**1. The issue's own conformance criterion is unactionable today: TASK-C575-1
(#744) has shipped no layout to conform to.** AC-1 says every lab must
conform "to the layout from TASK-C575-1," but #744 (the issue that defines
that layout) is itself still open with no artifacts merged. A repo-wide
search confirms it: no directory under `examples/` or elsewhere matches
`*lab*` except unrelated `collab`/`CollabModule` hits; `examples/` contains
only `examples/autograde/` (a single unrelated shift-register grading demo,
not a lab); grepping the tree for "C575", "lab pack", or "planted-defect"
outside `issue-reviews/` returns nothing. Whoever picks up #746 without #744
already merged has to invent the layout it claims to conform to, silently
expanding this issue's scope. `ordering_after: ["TASK-C575-1"]` declares the
dependency in YAML frontmatter, but the issue body never says "blocked on
#744 merging first" in prose a human triager will actually read.

**2. AC-1's "at least three ... and at least one FSM lab" is ambiguous about
whether the FSM lab counts toward the three or is a fourth lab on top of
them — a gameable floor.** Quoted: *"At least three labs covering sequential
elements and at least one FSM lab ship."* An FSM/state-machine lab plainly
*is* a lab "covering sequential elements" in the ordinary sense (a state
machine synthesizes to registers and logic — see `StateMachine.java`'s own
`Timed`/edge-trigger model), so a literal reading lets three labs total (two
flip-flop/counter labs plus one FSM lab) satisfy both clauses at once. A
stricter reading — three sequential-but-not-FSM labs plus a separate FSM
lab, i.e. four — is also defensible and is what sibling issue #748's review
assumed when computing the pack's running total ("a floor of 4, not a
target"). The two readings differ by a whole lab, and nothing in the issue
disambiguates them. Recommend rewriting as an explicit count: e.g. "at least
four labs: at least three non-FSM sequential labs, plus at least one
separate FSM lab."

**3. No AC numbering, inconsistent with the parent issue's traceability
scheme.** The body lists four checkbox items with no `AC-1`/`AC-2` labels at
all, while parent #575 numbers its five criteria AC-1 through AC-5 and
sibling #748 partially numbers its own (and was flagged for the gaps). A
tracker or later issue that wants to cite "did #746 satisfy its AC-2" has no
stable identifier to point at. Recommend numbering all four bullets before
work starts.

**4. "Records any limitation it hits as a finding rather than fixing it
here" names no destination or mechanism for that finding.** Quoted from the
Boundary: *"this task uses whatever the shipped element does today and
records any limitation it hits as a finding rather than fixing it here."*
There is no instruction to file a new issue, comment on #566 (the FSM
element's own workflow-parity feature, itself still open with no assessment
document yet per its own AC-1), or write to a specific doc. As written this
is satisfied by literally nothing happening — no reviewer can point to a
missing record and call the AC unmet, because no record location was ever
specified. Recommend requiring the finding to be filed as a comment on #566
or a new linked issue, named explicitly.

**5. No slack against the pack-wide quality gate (#751) that can pull labs
post-hoc.** #575 AC-4 (via KC-33-2, cited in #751) pulls any lab that fails
two consecutive non-author reviews rather than let it pad the count. #746
ships at its stated floor (3 or 4 labs, see finding 2) with no stated
margin. If TASK-C575-1 (#744, 2 labs) and this issue both land at their
bare minimums and #751 pulls even one lab from this issue's set, the
pack-wide "at least 8" (#575 AC-1) can only be restored by work this issue
does not scope or own. This is the same failure mode sibling issue #748's
review flagged for its own slice of the pack; #746 has it too and should
either ship one lab of margin or explicitly note it is not responsible for
backfilling.

**6. The motivating claim about a competitor's `-test` mode is asserted, not
sourced, and isn't load-bearing but is written as if it were.** Quoted:
*"the exact capability Logisim-Evolution's `-test` never had (their #598 and
#950) and the reason a JLS lab pack can exist at all."* No link is given,
and Logisim-Evolution is a different repository this session did not
inspect (fetching it was outside this task's scope). This is background
motivation rather than an acceptance criterion, so it doesn't gate the
work — but a specific, falsifiable-sounding claim ("never had") stated as
settled fact, cited only by bare issue numbers in someone else's tracker,
is exactly the kind of thing that should carry a URL if it's going to be
used to justify why the feature is worth building. Recommend either linking
the two issues or softening to "as far as we've observed."

**7. "Several clock cycles" and "student time budget" are unitless,
gameable floors.** The planted-defect AC requires a defect "only observable
after several clock cycles" — two cycles arguably satisfies "several" and
would trivially pass a reviewer who isn't looking for the spirit of the
requirement (defects that only show up in accumulated state, e.g. a counter
wraparound or an off-by-one in an FSM's last transition, need more than
two or three cycles to be distinguishable from a wiring bug that happens to
have one cycle of pipeline delay). Likewise "student time budget" states no
unit (minutes vs. hours) though #744/#575 don't specify it either, so this
is a pack-wide gap this issue inherits rather than introduces. Low severity,
cheap to fix: name a minimum cycle count (e.g. "≥5") and a unit for the time
budget.

## What's solid

- The technical premise — that JLS's own `-t` grammar can drive a pin's
  value over time via `for`/`until` steps rather than only enumerating a
  static input space — is real and already proven in-tree:
  `docs/batch-interface.md` §2.3 documents exactly this, and
  `test/jls/SequentialGoldenTest.java` already exercises Register and
  StateMachine goldens over multiple clock cycles using this mechanism. The
  issue isn't asking for new simulator capability, just content built on an
  existing, tested one.
- The Boundary section correctly defers FSM-element workflow gaps to #566
  rather than scope-creeping into fixing the element here, and correctly
  scopes out platform/grading-engine plumbing (owned elsewhere per #575).
- `ordering_after: ["TASK-C575-1"]` correctly names the one real prerequisite
  (content layout), and the task_id/part_of_feature linkage to #575 is
  internally consistent with how #744, #748, and #575 itself reference each
  other.

## Bottom line

The simulation/batch-interface premise this issue rests on is sound and
already demonstrated in the test suite, and the Boundary section is
disciplined about not scope-creeping into #566's element work. But the
acceptance criteria have a genuine counting ambiguity (three vs. four labs),
no numbering, an unsourced "finding" mechanism with no destination, and no
margin against the pack-wide pull-on-failure gate — and the issue depends on
a prerequisite (#744) that has shipped nothing yet, which it does not flag
loudly enough for an implementer picking this up in isolation. Tighten the
AC wording and name the missing mechanisms before implementation starts.
