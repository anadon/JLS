# Issue #451: TASK-0054: a Logisim-Evolution .circ file opens in JLS, and every construct that did not survive is named, located and explained
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is open, well-evidenced against the current tree (O1–O6 all
reproduce), and its central claim — the completeness equality is false
against today's loader — is verified correct against
`src/jls/elem/Element.java:344-351` and `src/jls/Circuit.java:1067-1116`.
That rigor is real. But the document leans on several citations and
dependency claims that do not hold up, and it defers the one measurement
(net-connectivity correctness over real files) that the parent feature
calls the actual gate. Findings below, most severe first.

## Findings

**1. Hard blocker is itself unresolved, and a second, undeclared blocker exists.**
`blocked_by: [404]` in the YAML, and #404 (TASK-0003) is **open**, not
merged — confirmed by fetching it directly. The issue is honest about this
(§8 first bullet: "if O1 or O3 no longer fails, stop and comment"), so this
is not a contradiction, but it means #451 is not actually startable today;
any executor picking it up must first verify #404 landed, which it has not.
More seriously: the parent feature #323 (FEAT-025) states in its own
Sequencing section, in these words, "**TASK-0055 before TASK-0054 is
necessity, not convention.** The part data the imported designs reference
has to exist as data before an importer can bind to it" — yet #451's own
`blocked_by`/`related` lists (`related: [61, 314, 323, 337, 412, 408, 342,
448]`) omit #450 (TASK-0055) entirely. The only place #450 is named as a
prerequisite is the issue-#608 boundary comment, added after filing, not
in the issue body or its dependency YAML. Either #451's scope genuinely
does not need part-binding (in which case the parent feature's "necessity"
language is stale and should be corrected on #323), or #451 is missing a
real blocking edge. **Recommendation:** resolve which is true and fix
whichever document is wrong before execution starts.

**2. The cited prior-research document does not cover what Materials & Apparatus claims it covers.**
"`docs/hdl-support-research.md:151-195` carries the verified account of
Logisim-Evolution as the other Java reference point... Reference it; do
not restate it." Reading that section (verified at those lines):
it documents Logisim-Evolution's **HDL export direction** (VHDL/Verilog
generators, FPGA board flow, the VHDL/Questa co-simulation gap) — it
contains zero information about the `.circ` **file format**: no XML
schema, no attribute vocabulary, no geometric connectivity rule. The
parent feature #323 itself states the correctness-critical rule this task
must replicate — port offsets as a function $\delta(k,n,s)$ of component
kind, input count and body size, geometric connectivity by coincident
coordinates — and calls a failure to replicate it exactly "the worst
failure mode available... circuits that import silently disconnected."
Nowhere in #451 or the cited research doc is that rule's actual content,
or even a citation to where in Logisim-Evolution's source it lives. This
is the single most safety-critical piece of the format and the issue's
"Materials" section treats it as already covered when it is not.
**Recommendation:** either attach the missing schema/connectivity research
as a prerequisite deliverable, or explicitly scope it into this task's
Method with its own step and time.

**3. A cited test precedent is misdescribed.**
§5 P5 and §12 both assert: "Every imported circuit loads through the real
`Circuit.load` + `finishLoad` and re-saves identically — the assertion
shape `test/jls/hdl/imp/ImportPipelineTest.java:70-79` already uses."
Reading those exact lines: the test asserts `circuit.load(...)` returns
true, `circuit.finishLoad(...)` returns true, and
`result.summary().elementCount() > 0`. There is **no re-save step and no
identity comparison anywhere in that file** (`grep -i "re-sav|resave"`
returns nothing). The "assertion shape" cited as precedent for P5's
re-save-identically requirement does not exist yet — P5 is a genuinely new
test pattern being introduced, not a reuse of an established one. This
matters because the issue repeatedly argues its own soundness by pointing
at "shapes that already exist"; here that argument is false, which should
make a reviewer re-check the other such claims (O4/O5's shapes do check
out on inspection).

**4. The task's own completion bar is weaker than the capability it's sold on, and the gap is a Waived-by-design deferral.**
The parent feature #323's actual acceptance test (I1) is "Import each file
in a corpus of public `.circ` files and compare net counts and net
membership against the source's own computed nets... They agree per
file, mechanically" and its Sequencing section calls the corpus run "the
gate on the estimate, not a follow-up." But #451's own Open Question 3
marks the corpus's provenance/composition as "Blocks execution of
`CircCorpusTest`, not of the reader" — i.e. #451's Definition of Done can
be fully checked off (P1-P8, P10, hardening, name-collision, construct-map
pinning) using three hand-written fixtures without ever validating
connectivity against a real corpus file. P9's corpus tables are listed in
the DoD checklist, so they are nominally required — but "what counts as
published" being an unresolved, non-blocking open question means an
executor could satisfy P9 with a token 2-3 file "corpus" scraped in an
afternoon and technically close the issue while the feature-level claim
(I1: net partition matches source over real files) remains unmeasured.
**Recommendation:** make corpus size/provenance a genuine completion gate
with a stated minimum (e.g. "N files across M distinct component
libraries"), not a rides-along open question.

**5. Refusal scope (whole-file vs. whole-collision) is stated as "recommended default" but drives P8's testability.**
Open Question 1 recommends "refuse the whole import" on a name collision,
and marks it "Blocks execution" — good, that's explicit. But P8's test
("no partial circuit is ever produced on a failed import") and the
`aNameCollisionIsRefusedNotMapped()` test interact: nothing in §9 (Data
Collection & Analysis) specifies whether the collision test also asserts
*other, unrelated, cleanly-mappable* constructs in the same file are
still refused (whole-file) rather than silently accepted around the
collision. A sloppy implementation could satisfy "the collision is
refused" literally while still emitting a same-run partial circuit for
everything else in the file if the refuse-boundary is implemented
per-construct instead of per-file — exactly the ambiguity Open Question 1
flags as unresolved between Stage 4's rule text and its own recommended
default. The recommended default reads as settled; the test list does not
confirm it is asserted.

**6. Gameable metric: "the completeness equality" fixture has only three unmappable components.**
§11 (Threats to Validity) is honest that "Fixtures are not a corpus," but
P2's assertion — `Reported == Seen - Realized` as full set equality — is
tested over a single seeded fixture with exactly three deliberately
unmappable components. A reader implementation could special-case those
three named constructs (hard-coded unmapped list) and pass P2 while a
fourth, unforeseen construct type is silently dropped in a real file. The
equality is the right *shape* of assertion, but with N=3 known items it is
weak evidence of general correctness, and nothing in the Method requires
property-style or corpus-driven expansion of that set before this task's
own DoD is satisfied (that expansion is explicitly the residual, no-task-id
work under #323, not #451).

**7. Underspecified CLI/editor routing for the new format.**
O1's mechanism, once traced (`FileAbstractor.openCircuit`,
`src/jls/FileAbstractor.java:99-110`), is **not** an extension blacklist —
it's `Util.isValidFileName` applied to the path after stripping only
`.jls`/`.jls~` suffixes; a `.circ` name fails because the literal `.circ`
substring (with its dot) never gets stripped and dots are invalid in a
circuit name. §7.1 says only "the extension gate itself changes" without
specifying whether `.circ` becomes a second branch in
`FileAbstractor.openCircuit`, a wholly separate code path invoked earlier
in `JLSStart`/`Editor`'s file-open dispatch (bypassing `FileAbstractor`
entirely, which seems structurally required since `.circ` is XML, not the
line-oriented grammar `FileAbstractor` sniffs), or something else. This is
a real design decision with test-layout consequences (which class owns
the `-b foo.circ` CLI dispatch, and does the existing `NOT_A_CIRCUIT`
`LoadError` category get reused or does `.circ` get its own failure
taxonomy) that the Interface & Data Contract section does not settle.

## What's solid (one line each)

- O2–O5 (XML-parse absence, the `setValue` silent-drop mechanism, the
  `NetlistImporter` aggregate-and-refuse precedent, `ImportSummary`'s
  existing but UI-less shape) are all verified accurate against the tree.
- The five hardening vectors in H3/P6 are a correct, standard XML-bomb
  threat list and each gets its own falsifiable test — good discipline.
- The name-collision "refuse, never guess" rule (Stage 4) is the right
  call given #451's own cited evidence (`ShiftRegister` in
  `HdlExporter.java:84`) that same-named constructs can have divergent
  semantics.
- GPLv3-to-GPLv3 licensing compatibility for consulting Logisim-Evolution
  source is correctly reasoned, with an explicit provenance obligation.
- The scope boundary against #608/#610 (shared loss-report schema) is
  handled cleanly in the follow-up comment: no work is duplicated, and
  either landing order is stated to be sound.

## Verdict rationale

`needs-rework`, not `should-not-proceed`: the core engineering plan
(hardened parser, aggregate-refuse, three-category report, written
construct map pinned both ways) is sound and closely modeled on real
in-tree precedent. But the issue cannot be executed as written without
first resolving the #450 dependency-omission (finding 1), sourcing the
missing connectivity-rule research it currently claims is already done
(finding 2), and correcting the false test-precedent citation (finding 3)
— plus tightening the corpus gate (finding 4) so the task's DoD does not
diverge from the capability it is sold as delivering.
