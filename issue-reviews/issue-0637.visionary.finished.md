# Issue #637: TASK-C562-2: a Digital test construct with no -t equivalent is a named loss in the report, never a quietly thinner vector file
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this task is really for

Not a construct list. The sentence that matters is in the Outcome: *"instead of
discovering it when a wrong submission passes."* The asset being protected is an
instructor's **trust that green means correct**. Everything else here — four
fields per entry, a closed disposition vocabulary, a set equality — is machinery
in service of that, and machinery is the part most likely to be built correctly
and still fail the purpose.

Held against that purpose, the task as written has a hole big enough to walk the
original failure mode back through: AC-2 says a partially translated case is
reported `mapped-with-caveat` and **still emitted**. An instructor who does not
read the report gets a vector file that runs, passes, and tests less. The task
prevents *silent* thinning; it does not prevent thinning. The difference between
the two is one document nobody is required to open.

## The framing problem: "no `-t` equivalent" is a property of a mechanism, not of Digital

The unit of accounting is "Digital test-language construct with no `-t`
equivalent." That set is not a fact about Digital — it is a fact about the choice
in #635 to emit `-t` text. Under the source-to-source emitter, `let`, `loop`,
`repeat`, `bits` and expressions are all losses, because §2.2's grammar
(`file ::= { signal }`) has no iteration or binding. Under the executor design
that the fleet's #562 review redirects toward, none of them are: an interpreter
elaborates them and the loss set collapses to genuine semantic residue —
expected-value columns, don't-care `x`, HiZ/bidirectional, the cycle-based `C`
clock against `-t`'s absolute `for`/`until` timeline, `program`/memory init,
anything data-dependent on circuit output.

Two mechanisms, two disjoint loss vocabularies, and this task enumerates one of
them at 1 mw. Specifying the loss ledger before the translation mechanism is
settled is building the schema for an answer nobody has chosen yet. That alone is
reason not to start it in the stated order.

(Factual correction to the record: `TASK-C556-1` and `TASK-C556-2` *are* filed —
#608 and #610. The prerequisites exist; the ordering concern above is about
#635, not about them.)

## Reframing 1 — exhaustiveness by construction, not totality by assertion

AC-3 wants "the set dropped equals the set reported, in both directions." That
equality is only load-bearing if the two sets have **independent derivations**.
If the same pass both drops and reports, the assertion is a tautology wearing a
proof's clothes, and AC-4's hand-written mutant is an admission of it — you need
a mutation test precisely because the property cannot fail on its own.

This repository already knows the better move, and uses it everywhere: make the
omission fail the build *structurally*. `SaveTags` + `SaveTagsTest` cross-check
tags and classes in both directions; `ExtensionPointCatalogTest` cross-checks
constants against the `docs/extension-points.md` table both ways;
`ElementConstructorContractTest` pins a contract no reviewer has to remember;
`HeadlessCoreRatchetTest` and `NotificationRatchetTest` make a regression a
compile-adjacent event rather than a judgement call.

The same idiom applies here, and it is already the codebase's dialect.
`src/jls/collab/op/CircuitOp.java` is a *sealed interface over data-only
records*; `Element`, `Gate`, `Put`, `SimEvent`, and `SigSim` itself are sealed.
So:

- The parsed Digital test program is a **sealed** node hierarchy
  (`sealed interface DigTestNode permits Let, Loop, Repeat, Row, Decl, …`).
- Translation is a **total function** `DigTestNode -> Disposition`, where
  `Disposition` is itself sealed over #608's closed vocabulary
  (`Translated | Caveat | Refused | DroppedByDesign`), each carrying location and
  explanation.
- The dispatch is one exhaustive pattern `switch`. Adding a Digital construct
  without giving it a disposition **fails compilation** — and this build treats
  warnings as errors, so there is no "we'll add it later" path.

AC-3 and AC-4 then stop being tests and become the type system. Nothing can be
dropped without producing an entry, because dropping *is* producing an entry.
Better still, this belongs in #608's shared infrastructure, where all four
importers (#558, #559, #561, #562) inherit it once, instead of four hand-rolled
mutants that each have to be kept armed. **I am disregarding AC-3 and AC-4 as
written**: not because the properties are wrong, but because asserting at runtime
what the compiler can refuse is the weaker instrument, and it is not this
project's habit.

## Reframing 2 — ship a ledger and an UNRUN verdict, not a caveat

Replace per-construct entries as the *primary* artifact with per-assertion
accounting, because that is the instructor's actual question:

```
alu.dig  test "add"       16/16 assertions carried
         test "overflow"   0/8  carried — expected-value columns (refused)
         test "bus"         8/12 carried — 4 HiZ expectations (refused)
suite: 24 of 36 assertions carried
```

One number — *this suite now checks 24 of your 36 assertions* — does more for
trust than a hundred perfectly-formed construct entries, and it is mechanically
derivable from the same dispositions.

Then close the hole in AC-2. A test case that loses any verdict-bearing assertion
should not be emitted as a runnable-and-passing thinner file with a footnote. It
should be **refused**, or emitted marked incomplete and mapped onto the third
state that #369's PASS/FAIL/**UNRUN** lattice already has for exactly this. An
incomplete case that cannot report PASS makes the failure mode impossible rather
than merely documented — safety by construction again, not safety by diligence.
This also produces, honestly and immediately, the number that decides whether the
verdict channel is worth its 9-15 mw: under today's expectation-free `-t`, nearly
every verdict-bearing Digital test is `refused`, and a suite that reports "0 of
36 assertions carried" is a far more useful research result than a suite that
reports green.

## Reframing 3 — one diagnostics spine, not a second one

`src/jls/LoadError.java` is a record with a closed `Category` enum, a `detail`, a
`line`, and an actionable `hint`, published through one channel so every front
end says the same thing (#58, #81). #608's proposed schema is *construct,
disposition, location, explanation* — the same four fields, the same closed
vocabulary discipline, one severity band away. JLS is about to have two parallel
structured-diagnostic taxonomies that differ mainly in field names.

The larger move: one `jls.diag` spine — subject, disposition/category, location,
explanation — with `LoadError` as its refusal end and importer losses as its
degraded-but-loaded end. The payoff is not tidiness: it is that the GUI (#81's
`TellUser`), batch stdout, and any grading script learn *one* rendering, and the
migration report inherits the loader's headless-awareness and its golden-test
harness for free. If that unification is out of scope for a 1 mw task — it is —
then at minimum #608's schema should be specified as an extension of
`LoadError`'s shape, with the reason for every divergence written down.

## What survives unchanged

The Outcome. "A translated suite that runs green because it tests less" is the
right enemy, it is named precisely, and it is the failure mode that would
actually cost a student a grade. AC-1's four fields are the right fields. AC-2's
naming of *which* assertions were dropped is right; only its disposition default
is wrong.

## Verdict

**rethink.** The goal is correct and worth the mw. The specification is aimed at
a loss vocabulary that a still-unsettled mechanism (#635) defines, accounts in
constructs where instructors count assertions, and enforces at runtime what this
codebase's own sealed-hierarchy and ratchet idioms enforce at compile time.
Re-spec it as: (1) exhaustive disposition dispatch over a sealed test AST,
contributed to #608 so all four importers inherit it; (2) an assertion-coverage
ledger as the instructor-facing artifact; (3) incomplete test cases refused or
mapped to UNRUN rather than emitted with a caveat. Hold it until #562/#635 settle
emitter-versus-executor — its content is a function of that answer.
