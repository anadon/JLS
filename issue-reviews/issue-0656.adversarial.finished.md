# Issue #656: TASK-C565-5: synthesis runs headless — a table file in, a circuit file out
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This is a task-tier issue (part of FEAT-C31-3 / #565, which is itself part of CAP-31 / #515)
asking for a `-synth`-style batch flag: table file in, `.jls` circuit out. The batch-headless
direction is architecturally sound — JLS's simulator core is already headless by construction
(issue #77) and the CLI already has this exact flag shape (`-export`, `-i`, `-savetext`). But the
issue leans on two contracts (a shared table format, a frozen CLI stability promise) that neither
exist yet, inherits an unresolved determinism gap from its own dependency chain, and was filed
without the sibling-task coordination notes that already exist one issue away on #646. None of
this is disqualifying, but none of it is pickup-ready either.

## Findings, most severe first

**1. HIGH — AC-3's "same format" claim has no format to be the same as.**
Quoted: *"AC-3: The table input format is the same one TASK-C563-4 writes, so extraction and
synthesis compose on the command line without a converter."* TASK-C563-4 is #646, and its own
AC-1 only says extraction *"writes a machine-readable table"* — no grammar, column order, radix,
don't-care symbol, header line, or multi-bit encoding is specified in either issue. A comment
already on #646 (`#5181368255`, "Boundary: #646 vs #651 vs #656") states the actual resolution
mechanism: *"Whichever of the three lands first sets the output encoding; the other two match
it."* That is a real coordination plan — but it lives entirely on #646, is not quoted or
cross-referenced in #656's body, and is not something #656's own acceptance criteria can be
checked against today. As filed, "the same format" is an assertion about a document that does not
exist. **Recommendation:** either #656 pins the grammar itself (making it the format-setting task)
or its AC-3 explicitly defers to and cites #646's coordination comment, so an executor knows where
the real spec will come from.

**2. HIGH — the cited CLI stability promise (#524) is itself unbuilt, and the timing obligation this creates is unaddressed.**
Quoted: *"The batch CLI stability promise is #524; this adds a flag under it."* Fetching #524
(FEAT-C21-1) shows it is an **open, unimplemented** proposal to freeze the CLI contract — its own
body states `docs/batch-interface.md` (from closed #72) is *"the starting material, not the frozen
artifact,"* and a later comment on #524 confirms `CliContractConformanceTest` does not exist yet at
review time. The *actual* landed stability contract today is `docs/batch-interface.md` itself
("Status: normative, and a stability contract," pinned by `CliFlagTableTest`/`CliSmokeTest`). Worse,
the coordination comment on #646 spells out the consequence #656 doesn't mention: *"Three new flags
landing after that freeze are contract additions and owe it a conformance case each. Landing before
it means the freeze must enumerate them."* #656's `ordering_after` names only `TASK-C565-3` and
`TASK-C563-4` — #524/#686 (the freeze itself) is absent, so nothing in #656 commits to either side
of that obligation. **Recommendation:** cite `docs/batch-interface.md`/#72 as the contract this flag
is added under today, and add an explicit ordering/coordination note resolving the
before-freeze-vs-after-freeze question the same way #646 already has to.

**3. HIGH — AC-2's "byte-deterministic" promise inherits an unresolved tie-break gap from two unbuilt upstream stages.**
Quoted: *"AC-2: The written circuit is byte-deterministic for a given table, so it can be committed
as a golden."* Determinism here is not this task's to guarantee in isolation — it composes
Quine–McCluskey minimization (FEAT-C31-2 / #564) and layout (TASK-C565-3 / #654). #564's AC-2 only
requires minimization to be *"exact within the stated bound"*; nowhere does #564 (or #656) specify
a canonical tie-break when a function has multiple equally-minimal prime-implicant covers, a
routine QM outcome. #654's own AC-4 ("layout is deterministic for a given netlist") is itself an
unbuilt, unproven claim (per the adversarial review of #565, #654's layouter dependency is
unverified against #62/#290's legibility rubric). `grep -rn "QuineMcCluskey\|Quine-McCluskey"
src/` returns nothing — none of this machinery exists yet to check against. As worded, an
implementer can satisfy AC-2 by pinning one golden per table and calling any consistent-with-itself
output "deterministic," without ever resolving the cover-selection ambiguity a second, differently-
implemented minimizer could legitimately break the other way. **Recommendation:** state the
tie-break rule (e.g., lexicographic on implicant bit-pattern) explicitly in #564 or here, and note
that AC-2 cannot be considered proven until #564 and #654 land with that rule honored.

**4. MEDIUM — filed without the sibling-coordination notes that already exist for this exact task set.**
A comment on #646 (`#5181368255`, filed the same day as #656, 31 minutes after it) explicitly
enumerates #646/#651/#656 as three headless surfaces on one CLI and states three coordination
points: a shared flag mutual-exclusion mechanism (#372/TASK-0001, `FlagSpec`), the CLI-freeze
timing question (finding 2 above), and "whichever lands first sets the output encoding" (finding 1
above). None of this is mirrored onto #656, even though #656 is one of the three issues the note is
about. A later addendum (`#5226962958`) adds a fourth sibling, #660. An executor who opens #656
alone — which is the normal way to pick up a task-tier issue — has no way to discover any of this
without independently finding and reading #646's comment thread. **Recommendation:** mirror the
coordination note (or a pointer to it) onto #656, as the addendum comment on #646 already models
doing for a newly-discovered sibling.

**5. MEDIUM — unaddressed conflict with the sibling visionary review's explicit recommendation against inventing a table file format.**
The visionary review already on file for the parent feature (`issue-reviews/issue-0565.visionary.md`,
"Reframing B") argues AC-5 of #565 (the ancestor of this task's "table file in, circuit file out"
framing) should be struck because it implies inventing a new truth-table file format, and
`docs/standards-landscape.md:203` records "deliberately no standard for truth tables" — meaning
any such format is bespoke, unhardened surface this project would have to design, harden (per issue
#38's hostile-input-cap precedent that every other reader follows), and document from scratch. That
review recommends instead using a `.jls` file containing a `TruthTable` element as the interchange
format, reusing `FileAbstractor`'s existing sniffing/hardening. #656 goes the opposite direction (a
new machine-readable table format, matched to #646's separate new format) with no acknowledgment of
that recommendation or rebuttal of it. Whether or not the reframing is adopted, #656 as filed is
silently taking one side of a live, already-recorded design disagreement. **Recommendation:**
either address the visionary reframing explicitly (accept/reject with reasons) before this task is
picked up, or note that #656 and its format choice are provisional pending that resolution.

**6. LOW — AC-4's "documented non-zero exit status" doesn't distinguish the two named failure classes.**
Quoted: *"AC-4: Bound violations and malformed table files refuse with a documented non-zero exit
status..."* Under the current three-code contract (0/1/2, `docs/batch-interface.md` §1), both
failure classes almost certainly land on exit 1 ("runtime failure") — which is a defensible
outcome, but the AC as worded is satisfied by returning exit 1 for both a table with too many
inputs and a syntactically corrupt table file, conflating two failure classes a grading script
might reasonably want to tell apart (a policy problem vs. a data problem). This is not disqualifying
— TASK-C563-4 (#646) has the identically-shaped AC-3 with the same gap — but it means the criterion
is gameable by any single non-zero code, including one that discards the distinction entirely.
**Recommendation:** state whether bound violations and malformed input are expected to share exit
1, or should be distinguishable (which would require #524/#686's status-3-class work to land first,
tying back to finding 2).

**7. LOW — the issue doesn't flag that it sits three unbuilt stages deep.**
This is TASK-C565-5, `ordering_after` TASK-C565-3 (#654, unbuilt: no layouter-driven synthesis
code exists), which is itself ordered after TASK-C565-2 (netlist construction, unbuilt) and #62's
legibility rubric (#290, unverified per the #565 adversarial review's finding 5). #656 is the CLI
wrapper around a pipeline none of which exists yet; that's normal for task-tier planning but #656's
own text gives no hint of how much has to land first, unlike #646 and #524's boundary notes, which
at least name their prerequisite chains explicitly. **Recommendation:** no blocking action; note at
pickup time that #653/#654/#564 must be green first.

## What's solid

- **AC-1's headless premise is architecturally consistent**: `Simulator`/`BatchSimulator` are
  already AWT/Swing-free by construction (issue #77, `HeadlessCoreRatchetTest`), and batch mode
  already runs off the main thread with no display — adding a synthesis flag here is additive to a
  pattern that's already proven, not a new headless surface class.
- **AC-4's "write no partial circuit file" asks for an existing, proven primitive**:
  `FileAbstractor.writeCircuit` already writes atomically via temp-file-then-rename
  (ARCHITECTURE.md, "The save/load pipeline"), so this criterion is satisfiable by reuse rather than
  new mechanism.
- **Scoping this as a narrow, single-flag task** (rather than re-litigating the GUI/editor side of
  synthesis) is appropriately sized for a task-tier issue and matches how `-export`/`-savetext`/`-i`
  were each added incrementally.
- **"Adds a flag under" rather than "amends" the CLI contract is the right instinct** even though
  the target issue's own readiness is questionable (finding 2) — the issue correctly avoids trying
  to redefine the batch interface itself.

## Verdict rationale

`needs-rework`: the direction (a headless synthesis flag matching the existing `-export`/`-i`
pattern) is sound, but the issue leans on a table format that doesn't exist yet, cites a CLI
stability promise that is itself an open, unbuilt proposal without addressing the before/after-
freeze obligation that creates, promises byte-determinism that depends on an unresolved
minimization tie-break, and was filed without the sibling-coordination notes already known to apply
to it. Each is fixable without changing the task's premise; none should be silently assumed away by
whoever picks this up.
