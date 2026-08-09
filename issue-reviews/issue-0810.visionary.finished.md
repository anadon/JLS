# Issue #810: TASK-C595-2: identifier rules relax to what engineers actually write, including active-low — and every remaining restriction cites the grammar constraint that forces it
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The end is right and squarely on CAP-37's (#521) arc: JLS loses switchers because it
refuses things every other tool accepts, and `SIG_n` / `/CS` / `RESET#` is a refusal that
reads as the tool not knowing the trade. Keep the outcome. What I am reframing is the
*mechanism*, because the issue's causal story — "the `.jls` grammar forces the
restriction, so relax to the grammar's edge and REFUSE the rest with a grammar citation"
— does not survive contact with the code. It is close to exactly backwards.

## The load-bearing fact: the file format is the most permissive layer, not the binding one

`Util.isValidName` (`/home/user/JLS/src/jls/Util.java:219`) is one global predicate —
`letter (letter|digit|"_")*` — and it is consulted by nine unrelated call sites
(`PinDialog`, `RegisterDialog`, `MemoryDialog`, `SubCircuitDialog`, `JumpStartDialog`,
`TruthTableEditor`, `Editor.java:161`, `JLSStart.java:2191/3024/3052`). Those call sites
have *three different* real constraint sets, and the predicate serves the intersection of
all three:

1. **Element names** (Pin, Register, Memory, JumpStart, probes) are persisted as quoted,
   escaped `String` items — `Pin.java:167` is an `Attribute.StringAttribute("name")`, and
   `docs/file-format.md` §6 escapes `\`, `"`, and newline. The format already accepts
   `/CS`, `CS#`, `~RESET`, and spaces in these names *today*. There is no grammar
   constraint here to cite. Zero.
2. **Circuit / subcircuit names** are the one genuinely bare token: `Circuit.java:1480`
   writes `out.println("CIRCUIT " + name)` and the reader is `Scanner`-token based. That
   forbids whitespace — and nothing else.
3. **The `.jls` base filename** (`Util.isValidFileName`, `Editor.java:161`) is a
   *filesystem* constraint, not a format one: Windows reserved names, `<>:"/\|?*`,
   trailing dot/space. `/CS` is unusable as a filename for reasons that have nothing to do
   with JLS's grammar.

So AC-2's "each remaining restriction is justified by a stated file-format or grammar
constraint" will, if executed honestly, produce a nearly empty list — and the restrictions
that *do* survive will need to cite `docs/batch-interface.md` and IEEE 1364 VCD, not
`docs/file-format.md`. An implementer following the issue literally will go looking for
grammar citations, find none, and either invent them or stall.

## Where the real constraints live — and why AC-3 as written re-creates the problem

The genuinely binding rules are downstream consumers, and they are documented stability
contracts, which is worse than the grammar, not better:

- **Batch `-t` vectors** (`docs/batch-interface.md`): the file is *tokenized on
  whitespace*, `#` starts a comment and the parser "joins each line's tokens and truncates
  at the first `#`", any token matching `-?0[xX][0-9a-fA-F]+` is rewritten to decimal, and
  the grammar is `signal ::= name initial { step } "end"`. That means `RESET#` is
  unreferenceable, a pin named `end` is ambiguous, and `0xFACE` silently becomes `64206`.
  These are the constraints that actually forbid the sigil forms — and none of them is the
  `.jls` grammar.
- **VCD export**: `$var wire <bits> <code> <name> $end`, `<name>` is the dotted fully
  qualified path and consumers "render the hierarchy" by splitting on `.`. Whitespace and
  `.` are load-bearing there.
- **HDL** — and here is the key: it is *already solved by translation, not restriction*.
  `HdlNames.sanitize` (`/home/user/JLS/src/jls/hdl/HdlNames.java:136`) maps any character
  outside `[A-Za-z0-9_]` to `_`, prefixes `id_` on a leading digit, suffixes `_` on a
  Verilog keyword, uniquifies with `_2`/`_3`, and records every change in a `renames()`
  map surfaced through `HdlModel`. The HDL path does not care what the editor accepts. It
  never did.

That is why **AC-3 as written is the criterion I am explicitly setting aside.** "Any
identifier the editor accepts is accepted by the batch and HDL paths, or the editor
refuses it up front" is precisely the lowest-common-denominator gate that produced today's
strictness. Implementing it faithfully re-derives an intersection rule — just a marginally
wider one — and permanently binds the editor's expressiveness to the crudest consumer JLS
will ever grow. The next consumer (PCF/XDC constraint emission in `jls.hdl.board`, IP-XACT
in `docs/standards-adoption/08-ipxact-export.md`) then *narrows* it again. The architecture
already contains the right answer one directory over.

## Reframing 1 (primary): split the predicate by consumer; translate, don't gate

Replace the single `isValidName` with a small typed set of rules, each owning its own
citation, and make every non-HDL consumer do what `HdlNames` does:

- `Names.label(...)` — element display names. Rule: non-empty, no unescaped line
  terminator (§6), no leading/trailing space. Essentially everything else allowed.
- `Names.circuitToken(...)` — the `CIRCUIT` line. Rule: no whitespace. Citation:
  `docs/file-format.md` §3.1 + `Circuit.java:1480`.
- `Names.fileBase(...)` — the OS rule, honestly labelled as an OS rule.
- `BatchNames` / `VcdNames` — *legalizers* modelled directly on `HdlNames`: deterministic,
  collision-free, with a rename map printed once at the head of batch output ("input pin
  `/CS` is addressed as `CS_n` in test vectors"). The batch interface contract gains a
  documented legalization section rather than the editor gaining a veto.

This is strictly the same shape as the seam JLS already ratified for HDL, and it makes
"grade the row REFUSE with a grammar citation" mostly unnecessary: almost nothing needs
refusing once translation exists.

## Reframing 2: the ask is typographic, and the file format can already carry it

Engineers do not "actually write" `/CS`. They write C̄S̄ — an overbar. `/`, `#`, `~`, `_n`
are all ASCII *transliterations* of a bar, forced by media that could not draw one. JLS is
a drawing program; it can draw the bar. A boolean `activeLow` attribute (ordinary
`Attribute` plumbing, one `int`/`String` item, no format version bump, `SaveTags`
untouched) plus an overbar in the element's `drawString` delivers the convention *more*
faithfully than permitting a slash in a string, and it does so with a stable, machine-
legal underlying name — which means batch, VCD, PCF and Verilog all keep working with no
legalization at all. Accept `/CS`, `CS#`, `~CS`, `nCS`, `CS_N` at the dialog as *input
syntax* that sets the flag and normalizes the stored name; render the bar. The problem
disappears rather than being negotiated.

Worth stating plainly in the issue: `RESET_N`, `nCS`, `CS_L` and `RESET_n` are **already
accepted today**. The active-low gap is narrower than the title implies — it is the sigil
and overbar forms only — and Reframing 2 covers all of them.

## Reframing 3: Verilog escaped identifiers make one REFUSE row disappear

`HdlNames.sanitize` mangles `/CS` to `_CS`. Verilog-2005 escaped identifiers (`\/CS `,
backslash-introduced, whitespace-terminated) and VHDL extended identifiers (`\/CS\`) would
round-trip it *exactly*. If the HDL path must preserve user names faithfully, that is a
~15-line change in one method, not a reason to restrict the editor.

## What the issue gets right, and one thing it gets right for the wrong reason

Right: relaxation must be pinned by a headless test failing at the pre-change commit;
byte-faithful save/load round-trip over a corpus of newly permitted forms (AC-2 in the
parent) is exactly the correct oracle, because the escaping path in §6 is the thing most
likely to break on `\` and `"` in a name. Keep both.

Right for the wrong reason: AC-5's "validation lands model-side, not in `SimpleEditor`
(KC-37-1)". Validation is *already* model-side — `Util.isValidName` is in `jls`, and no
call site is in `SimpleEditor.java`. For this task the #316/#84 decomposition gate that
FEAT-C37-4 declares "hard" is essentially vacuous, and #810 is being needlessly blocked
behind a god-class decomposition it does not touch. Say so and decouple it; #809 is where
that gate genuinely bites.

## Two gaps neither #810 nor #595 sees

- **JLS is simultaneously over-restrictive and over-permissive.** `isValidName` uses
  `Character.isLetter`, which accepts the entire Unicode letter category. Cyrillic `С` and
  Latin `C` are both valid, distinct names today — and names are identity-bearing lookup
  keys, not just labels (`Circuit.hasName`, `getJumpStart`, and the collab collision check
  at `AddElements.java:127-150`). Any relaxation work should decide a normalization /
  confusable policy, or it widens a key space that already has a homoglyph hazard.
- **`docs/file-format.md` §3.1 is under-specified for its own conformance claim.** It says
  names MUST match `letter (letter|digit|"_")*` without defining "letter", in a document
  that promises a third party can implement a reader from it alone. `UtilFunctionsTest`
  (`test/jls/UtilFunctionsTest.java:140-146`) pins only ASCII cases, so the ambiguity is
  unpinned in both the spec and the tests. Fix that in this task — it is cheap and it is
  the actual documentation debt behind the issue's premise.

## Bottom line

Endorse the outcome; reframe the mechanism. Keep AC-1 (relax), AC-2 (justify each
remaining rule) with the citations redirected to `docs/batch-interface.md`, VCD and the
filesystem, AC-4 (dispose of bsiever-fork #4 by name), and the round-trip and pre-change
test pinning. Set aside AC-3's editor-refuses-what-downstream-rejects gate in favour of
per-consumer legalization following `HdlNames`, and drop the KC-37-1/#316 block for this
task specifically. If only one thing lands, make it the overbar attribute: it is the
smallest change that gives engineers what they actually write.
