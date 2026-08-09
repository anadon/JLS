# Issue #810: TASK-C595-2: identifier rules relax to what engineers actually write, including active-low — and every remaining restriction cites the grammar constraint that forces it
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Findings, most severe first

**1. AC-3's "HDL path accepts it" clause is structurally vacuous, and the real risk it should be testing goes unaddressed.**
`src/jls/hdl/HdlNames.java:136-159` (`sanitize`) already maps *every* character
outside `[A-Za-z0-9_]` to `_`, and `unique()` (`:120-129`) resolves any
resulting collision with a numeric suffix. This legalizer never refuses —
by construction it "accepts" literally any `String` a JLS name could ever
be. So the HDL half of AC-3 ("accepted by... the HDL path... asserted, not
assumed") can be pinned by a test that is incapable of failing, regardless
of what #810 changes; it proves nothing. Meanwhile the actual hazard this
relaxation introduces is invisible to a binary accept/refuse test: distinct
new identifiers that only differ in punctuation — `RST#`, `RST!`, `RST~`,
`RST/`, all plausible "active-low" spellings under a loosened rule — all
sanitize to the same `RST_` and get arbitrarily disambiguated to
`RST_`/`RST__2`/`RST__3` in emitted Verilog, destroying the visible
correspondence between a student's JLS names and the exported HDL that is
the entire point of "conventional practice."
**Recommendation:** replace the HDL clause of AC-3 with a naming-fidelity
assertion (e.g., legalized names stay recognizably traceable to their JLS
source, or the emitter's rename map — already recorded, `HdlNames.renames()`
— is surfaced/asserted in the export), not a mere presence-of-a-mapping
check.

**2. A real, documented downstream landmine goes unnamed: `#` is the batch test-vector format's comment delimiter.**
`docs/batch-interface.md:69-72`: "The file is tokenized on whitespace...
the... parser joins each line's tokens and truncates at the first `#`."
`RST#` is exactly the sort of "what engineers actually write" active-low
spelling AC-1/AC-2 gesture at accepting. If the editor accepts it (nothing
in the `.jls` grammar's quoted-string element names forbids it — see
finding 3), using that pin in a `-t` test-vector file doesn't get
*refused* — it gets silently truncated at the `#`, so the parser looks for
a signal named `RST` instead. That is neither "accepted" nor "refused up
front with the downstream reason" as AC-3 frames the only two outcomes; it
is silent misinterpretation, a worse failure mode than either. Nothing in
#810, its parent #595, or its capstone #521 names this specific collision,
despite it being discoverable by reading the batch-interface doc the
issue's own AC-3 depends on.
**Recommendation:** name `#` (and any other batch-grammar metacharacter)
explicitly as a REFUSE row with the citation to `docs/batch-interface.md`
§ tokenization, or make the editor refuse identifiers containing it before
this ships — "accepted or refused up front" must include this class of
character, not just ones the `.jls` grammar itself restricts.

**3. The issue conflates several distinct "identifier" surfaces that have different actual constraint sources — "the grammar" is not one boundary.**
`docs/file-format.md` §3.1 (lines 149-155) scopes the letter-first
`letter (letter|digit|"_")*` grammar rule (`Util.isValidName`) to exactly
two things: nested `CIRCUIT` (subcircuit) name tokens, and the `.jls` file
base name. But the *same* `Util.isValidName` (`src/jls/Util.java:219-234`)
is also the unconditional gate for pin names (`PinDialog.java:181`),
register names (`RegisterDialog.java:325`), memory names
(`MemoryDialog.java:279`), truth-table names (`TruthTableEditor.java:163`)
and JumpStart names (`JumpStartDialog.java:149`) — all of which are saved
as `string-item = "String" attr-name quoted` (`docs/file-format.md:132`),
i.e. already free-form quoted strings the grammar places *no* character
restriction on (escaping is defined in §6 for exactly this reason). For
most of the surfaces the issue is nominally about, there is no grammar
constraint to cite at all — the restriction is pure inherited editor
policy wearing the grammar's letter-first rule by accident of code reuse.
AC-1's demand that "each remaining restriction is justified by a stated
file-format or grammar constraint" is honestly unsatisfiable for those
rows (nothing to cite) — which invites the opposite of what the issue
wants: citing the CIRCUIT-name/file-name grammar rule as if it also
constrained pin/register/memory names, when it demonstrably doesn't.
**Recommendation:** split `Util.isValidName` into per-surface validators
before scoring restrictions — (a) subcircuit/CIRCUIT name token (grammar-
constrained), (b) `.jls` file base name (OS-constrained, see finding 4),
(c) element/pin/label names (currently unconstrained by the grammar, only
by editor policy) — and grade each surface's rows separately.

**4. Reusing one relaxed validator for both element names and the on-disk file name is a portability/security hazard the issue never separates out.**
`Editor.java:161` and `JLSStart.java:3024`/`:2191` use the identical
`Util.isValidName` to validate the actual save-as file name and circuit
name entered by the user. "Conventional practice" for bus/active-low
notation plausibly reaches for `/`, `\`, `:`, or a leading `.` — several of
which are path separators or reserved/traversal-relevant on Windows,
macOS, and Linux. If the obvious minimal-diff implementation just loosens
`isValidName` itself (the function the issue talks about relaxing), it
risks reopening a filename-injection-class bug next to work this project
already treats seriously (`UntrustedFileHardeningTest`,
`ARCHITECTURE.md`'s "hostile-input caps" for loaders). Neither #810's body
nor #595's names this coupling.
**Recommendation:** make finding 3's per-surface split explicit in the
acceptance criteria so the file-name validator is called out as staying
conservative regardless of what pin/label names gain.

**5. The core premise is asserted, not shown, and the cited authority is unverifiable in this session — while the codebase suggests the premise is only partly true.**
`bsiever-fork #4` cannot be fetched: `mcp__github__issue_read` against
`bsiever/JLS#4` returns "repository... is not configured for this
session," so its specific content is unverifiable here. It is at least a
real, repeatedly-cited internal motivation (`#521` body: "over-restrictive
identifier rules, bsiever #4"; `#802` AC-1/AC-3 name it as a catalog row),
not fabricated — but its *content* is nowhere spelled out in-repo, only
"conventional practice... active-low naming" in prose. Reading the actual
rule (`Util.isValidName`: letters/digits/underscore, must start with a
letter) shows the two most common ASCII active-low conventions already
validate today: trailing `_N`/`_n` (`RST_N`) and leading `n` (`nRST`).
`test/jls/UtilFunctionsTest.java:139-147` pins today's genuine rejections:
leading digit, leading underscore, space, hyphen — punctuation-heavy
notations (`RST#`, `RST-`, `~RST`), not the common HDL-style ones. Because
the issue never states which characters bsiever-fork #4 is actually about,
AC-1 is satisfiable by an implementer who changes nothing, documents that
`_N`/`n`-prefix forms already pass, and closes the row HAVE — which may
even be the right call, but the issue can't distinguish that from someone
dodging the harder punctuation question, since it never poses the question
concretely.
**Recommendation:** before funding, restate in #810 (or its catalog row)
which specific characters/forms are in scope — cite the actual bsiever-
fork #4 text if it can be retrieved outside this tool's repo allowlist, or
enumerate the target forms directly in the issue body.

**6. Dependency chain is transitively unstartable today, and the issue's outcome text speaks of a catalog row that does not yet exist.**
`ordering_after: [TASK-C595-1]` (#809) is accurate as far as it goes, but
#809 itself is `ordering_after: [TASK-C592-2]` (#803, open), and #803 is
`ordering_after: [TASK-C592-1]` (#802, open, whose own body says "This
task changes no editor code" — it only *creates* the catalog document).
All three are open. #810's outcome text reads in the present tense — "the
catalog row is graded REFUSE with the grammar citation... rather than left
as a GAP that never closes" — as though a graded identifier-rules row
already exists to inherit. It doesn't: #802 AC-3 is the task that will
*first* create that row ("bsiever-fork ergonomic issues #18... and #4
(identifier rules) each appear as named rows"), and #803 is what adds the
REFUSE/GAP scoring machinery #810 presupposes. This is the identical
defect a sibling review found in #811 (also gated behind #803): the task
is filed as workable now, but nothing it can "grade" exists yet.
**Recommendation:** add #802 and #803 to `ordering_after` explicitly, or
rewrite the outcome text to stop presupposing a catalog row that has not
been published.

**7. Existing pinned test is silently in tension with the new AC, and the issue gives no guidance on resolving it.**
`test/jls/UtilFunctionsTest.java:139-147` asserts today's rejections —
leading digit, leading underscore, space, hyphen — are *correct*
behavior. AC-1/AC-2 never say which of these four should flip to accepted
vs. remain a cited REFUSE (e.g. leading underscore is a common "internal
signal" convention many engineers do write, and isn't grammar-mandated for
pin/element names per finding 3 — only for CIRCUIT/file names). An
implementer can satisfy the letter of AC-1 by leaving all four rejected
and citing *something*, or by flipping all four; both are defensible reads
of "conventional practice."
**Recommendation:** enumerate, per rejected case in the existing test,
whether it becomes accepted-with-justification or REFUSE-with-grammar-
citation, so the corpus test in AC-1/AC-2 has an unambiguous target.

## What's solid

- AC-5/KC-37-1 ("validation lands model-side, not in `SimpleEditor`") is
  already true today: `Util.isValidName` lives in `src/jls/Util.java`
  (core, not `jls.edit`), called from dialogs rather than owning the
  decision itself — this constraint costs the task nothing new to satisfy.
- `ordering_after: [TASK-C595-1]` correctly names its one direct sibling
  predecessor, and #809 is a real, open issue (unlike a same-severity issue
  found by a sibling review, this citation isn't fabricated or mis-scoped
  at the direct-dependency level).
- AC-2's byte-faithful round-trip requirement is realistic given existing
  infrastructure the project already relies on for exactly this kind of
  claim (`CircuitRoundTripTest`, `AllElementsRoundTripTest` per
  `ARCHITECTURE.md`'s save/load section) — extending an existing harness,
  not inventing a new verification mechanism.
