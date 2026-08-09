# Issue #674: TASK-C350-1: a campaign is a committed, diffable file naming its jobs, their inputs and their expected artifacts — and a malformed one is refused, never repaired
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: should-not-proceed

## Grounding

#674 is `TASK-C350-1`, the first of six `TASK-C350-*` children of feature
#350 (FEAT-057, campaign execution and aggregation) — confirmed by #350's
own roster comment (`issuecomment-5227057942`), which maps `TASK-C350-1` to
`#674` explicitly. `git grep -i campaign -- src/` at HEAD returns nothing;
there is no campaign, job, or artifact-naming concept anywhere in the tree.
The only real prior art is `src/jls/sim/BatchSimulator.java` (headless per
issue #77, `HeadlessCoreRatchetTest`) and the frozen `docs/batch-interface.md`
contract, both of which #674 correctly treats as unchanged inputs. The
project's one existing "committed, diffable text" precedent is
`docs/file-format.md` — a hand-rolled, dependency-free line grammar, not a
third-party serialization.

## Findings

**1. (Critical) The parent feature's own record, dated the same day as this
review, explicitly says this issue should not proceed.** #350's comment
`issuecomment-5227057942` (2026-08-08T16:43:08Z) states: *"Until it is
answered, **#674 should not land a description format** — it is the
artifact being duplicated, and the body already marks Open Question 1 as
blocking that exact scope."* That comment further explains the duplication
risk is not hypothetical: a CAP-21 grading-harness roster (#524/#525/#526/
#528/#530/#531, especially #697 and #724) now independently implements
"dispatch many jobs, produce an order-independent aggregate" — precisely
the shape #350 §7 calls the named failure mode of shipping two independent
implementations of the same format. #674 itself, however, is open, carries
no blocked/on-hold label, and its own checklist (AC4, see finding 2) only
asks that Open Question 1 be "answered in writing before the format is
called stable" — a bar an implementer can clear alone, without the
maintainer decision the escalation comment says is actually required. As
filed, a contributor who reads only #674 (not #350's latest comment) has
every reason to believe this is ready to pick up. **Recommendation:** do
not schedule implementation until #350 records the Open Question 1 decision
via an owner-authored `STATUS:`/decision comment (and #369 receives the
mirroring comment §7 requires); until then this issue should carry an
explicit blocked label pointing at #350's comment.

**2. (High) AC4's Open-Question-1 gate is weaker than the parent's stated
rule, and is self-certifiable.** #674 acceptance criterion 4 reads: *"Open
Question 1 ... is answered in writing before the format is called
stable."* #350's own text calls this "a maintainer decision, not a dedup
one" that "requires a maintainer to say which is shipping first." Nothing
in #674 requires that the "writing" be maintainer-authored, or that
"stable" mean anything auditable — an implementer can add a paragraph to
the format's design doc asserting an answer, tick the box, and the letter
of AC4 is satisfied while the actual ownership dispute (#350 vs #369) goes
unresolved. **Recommendation:** replace the OQ1 clause with a hard
precondition — an owner-authored decision comment on #350 exists — rather
than a text-exists-somewhere checkbox owned by the assignee.

**3. (High) The format is missing fields two of #350's own Open Questions
require it to carry.** #350 OQ3: *"an explicit worker cap and per-job
memory bound in the description, recommended [...] Blocks integration
criterion 5."* OQ4: *"the description carrying a field that later selects
resume"* (vs. restart, for checkpoint recovery). Both place a requirement
on the description format specifically — #674's exact deliverable. #674's
own framing (`ordering_after: []   # first cut; everything else in #350 is
defined over this format`) means every later task inherits whatever schema
ships here. Yet none of #674's five acceptance criteria mention resource
bounds or a resume/restart selector. If the schema ships without them, the
project either (a) has to break and re-version a format that AC2 insists
must never silently repair or default a malformed file — i.e. old
committed campaign files become malformed under v2 — or (b) bolts the
fields on informally later, undermining AC1's promise of a complete,
in-tree spec. **Recommendation:** AC1 should require reserved/stubbed
fields for worker cap, memory bound, and a resume-vs-restart selector, even
if their runtime semantics are deferred to later tasks.

**4. (Medium) Read-time collision checking is specified as spanning two
tasks, but #674 never states the reuse contract.** #350's integration
criterion 3 — the one AC3 cites verbatim — is explicitly marked *"Spans the
description reader and collection"* (i.e., #674 and TASK-C350-3/#677), and
the naming function is defined as a pure function of the description alone
(`path(j) = f(desc(j))`). That implies #674 must implement `f` to do
read-time injectivity checking, but the issue text never states that this
`f` is the canonical function #677 is required to consume rather than
reimplement. Left implicit, this recreates — internally, between two
sibling tasks — exactly the "two independent implementations of the same
shape" anti-pattern #350 §7 names as the risk between #350 and #369.
**Recommendation:** #674 should export/spec `f` explicitly as the shared
naming function and state that #677 must call it, not re-derive it.

**5. (Medium) Open Question 2 (serialization choice) carries a
dependency/license cost the acceptance criteria never asks to be weighed.**
`pom.xml` currently has no YAML/JSON parsing dependency; the project's one
existing "diffable text" precedent (`docs/file-format.md`) is a
hand-rolled, dependency-free grammar, consistent with the README's heavy
emphasis on BOM transparency, reproducible builds, and sha256/attestation
verification. #674's own body models its worked example as a YAML block,
and OQ2 option (b) is "an existing serialization" — implicitly pulling in a
new third-party parser. AC4 only requires that OQ2 be "resolved ... and the
reasoning recorded," not that the reasoning weigh the new dependency's
license or its effect on the BOM the project treats as a trust surface.
**Recommendation:** require the OQ2 write-up to state the chosen
serialization's license and its BOM/reproducibility impact, or justify why
the hand-rolled-grammar precedent was rejected.

**6. (Low) AC2's "named, located diagnostic" has no concrete oracle.**
"Located" is undefined — field name, line number, byte offset are all
consistent readings, and a minimal implementation could satisfy a naive
test with just a field name and no position. The existing precedent
(`Circuit.load`/`LoadError` for `.jls` files) presumably already has a
diagnostic convention; #674 doesn't anchor to it.
**Recommendation:** pin the diagnostic shape to that existing precedent,
or specify one explicitly (e.g., "file, 1-based line, field name").

## What's solid

- AC5 (no AWT/Swing/`jls.edit` imports) is concretely testable by the same
  ratchet-test pattern `HeadlessCoreRatchetTest` already established for
  `BatchSimulator` (issue #77) — low risk, straightforward to enforce.
- The Boundary section ("Format and reader only — no dispatch, no
  execution") keeps this task's actual code surface small and reviewable,
  and correctly declines to touch `docs/batch-interface.md`.
- AC1's requirement that the job list be "derived from the description
  rather than from any runtime event" is a sharp, well-chosen guard against
  the exact ordering-leak failure mode #350's global invariant 1 names.

## Bottom line

Two of #674's five checkboxes (AC2, AC5) are sound and cheap to verify.
But the issue's own parent feature carries a same-day, explicit "should not
land a description format" hold pending a maintainer decision this task
does not require anyone with maintainer authority to make, and the schema
this task is chartered to finalize is missing fields two of that parent's
Open Questions already say it must carry. Proceeding on #674 as currently
worded risks a mergeable, checkbox-complete PR that (a) directly
contradicts #350's own recorded instruction and (b) ships a format that
has to break its own no-silent-repair promise the moment #676/#681 need
the fields it omitted.
