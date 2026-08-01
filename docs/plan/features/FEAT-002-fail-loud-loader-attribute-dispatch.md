# FEAT-002 - Fail-loud loader and attribute dispatch

**Status:** proposed | **Cost:** 1-2 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

A saved attribute that no element declares stops being silently discarded at
load time and becomes a diagnostic naming the file, the line, the element and
the attribute. Today a `.jls` file can be loaded, edited and re-saved with data
missing and nothing anywhere reports it - not the loader, not a test, not the
user. Making this path loud is what turns every later format change, every
importer and every collaborative merge from a guess into a checkable operation,
because each of them relies on "if it loaded, it is all there" being true.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-00 | required | This is the highest-severity silent-data-loss path at HEAD and it has no issue and no test |
| CAP-01 | required | A merge that produces an attribute the target does not declare must be rejected, not quietly trimmed; the merge validator has nothing to call today |
| CAP-08 | required | An imported third-party core carries attributes JLS may not model; "silently dropped" and "faithfully imported" must be distinguishable |
| CAP-16 | required | The migration report's entire claim is that nothing was dropped silently, which is unassertable while the loader drops silently |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-001 | The attribute dispatch is itself a registry-keyed table: TASK-0001's inventory is what establishes that `savedAttributes()` is total per element type before the dispatch is made to fail on a miss |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0001 | Audit and pin every registry-keyed table | Shared with FEAT-001; the attribute tables are in its inventory |
| TASK-0003 | Make attribute dispatch total and the loader check it | The code change: the `setValue` overloads report an unmatched name and the five loader call sites raise a diagnostic |
| TASK-0004 | Silent-data-loss regression corpus | Fixture files carrying undeclared attributes, asserting a diagnostic rather than a quiet load |
| TASK-0054 | The foreign-tool reader and its migration report | Shared with FEAT-025; the migration report is the first consumer that needs "dropped" to be an observable event |

## Acceptance criteria

1. Each `setValue` overload reports whether any attribute matched. The signature
   change is visible to the compiler, so no call site can ignore it by
   inheritance.
2. All five loader call sites raise a diagnostic on a miss, and the diagnostic
   text names file, line number, element identity and attribute name. Tests
   assert the **text**, not a boolean.
3. A committed corpus of fixture files carrying attributes no element declares
   produces a diagnostic per file. Adding a new element type without declaring
   one of its saved attributes is caught by the same corpus.
4. Legacy files that load cleanly today still load cleanly, byte-identical on
   re-save. The `test/fixtures/legacy-4.1/` corpus is the witness.
5. The behavior on a miss is a decided policy - reject the load, or load and
   report - written down once and tested both ways. Silence is not one of the
   options.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the silent-data-loss path, the loader diagnostic and its corpus | **no issue** |

The absence is itself the finding: this is the highest-severity silent path
verified at HEAD and the tracker has never carried it. Do not create the issue
as part of this plan.

## Design notes

The mechanism is four methods and five call sites, and it is small enough to
state exactly. `Element.setValue` has four overloads - `int` at
`src/jls/elem/Element.java:344`, `long` at `:359`, `BigInteger` at `:374`,
`String` at `:389`. Each iterates `savedAttributes()`, returns on the first
attribute that accepts the name, and **falls off the end returning `void`** when
none does. `src/jls/Circuit.java` calls them unconditionally at `:1067`,
`:1078`, `:1089`, `:1105` and `:1116`, one per parsed value kind, and has no way
to learn that nothing matched.

The fix is a return value, not a new subsystem, which is why the band is 1-2 mw
including the corpus. The reason to spend it early rather than late is
ordering: FEAT-003, FEAT-012, FEAT-013 and FEAT-025 all assert properties of the
form "what was written is what is read", and each of them is asserting against a
loader that is currently permitted to disagree.

Decision D6 applies: this lands immediately as ordinary defect work and is not
sequenced behind the core extraction.

A second-order consequence worth stating for the task author: making the
dispatch total will surface attributes that are written but never read, which is
data loss in the other direction. Those are findings, not failures of this
feature - record them, and route the ones that matter to FEAT-013.

## Risks

- **Existing files in the wild may carry undeclared attributes.** If the policy
  chosen in criterion 5 is "reject", a user's working file stops opening. The
  recommendation is load-and-report for one format epoch with the diagnostic
  mandatory, then reject - and the epoch policy belongs to FEAT-013, so the two
  features must agree on it rather than each deciding.
- **The diagnostic can become noise.** A file with a thousand stale attributes
  produces a thousand lines. The corpus in TASK-0004 must include that case and
  the report must aggregate.
- **UNOWNED.** No committed roadmap program pays for this. At 1-2 mw it is the
  cheapest thing in the plan to fund, which is an argument for funding it, not a
  reason to defer it.

## Evidence

- The four silent overloads: `src/jls/elem/Element.java:344`, `:359`, `:374`,
  `:389`. Each loops `savedAttributes()` and returns without signaling a miss.
- The five unconditional loader call sites: `src/jls/Circuit.java:1067`,
  `:1078`, `:1089`, `:1105`, `:1116`.
- The legacy corpus that must keep loading: `test/fixtures/legacy-4.1/`.
- Element type count at HEAD (35 types, 27 `react` implementations):
  `BRIEF.md` §13; `src/jls/elem/ElementRegistry.java:38-77`.
- Sequencing: decision D6, `BRIEF.md` §12.
- Normative format reference (not restated here): `docs/file-format.md`.
