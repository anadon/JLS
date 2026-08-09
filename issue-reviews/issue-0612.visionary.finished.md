# Issue #612: TASK-C558-1: a .dig file parses as untrusted XML into an in-memory Digital model — XXE-proof, bounded, with its test sections preserved
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its format, #612 is not "parse XML safely." It is the second
instance of a pattern JLS is about to instantiate four times: read a foreign
circuit document written by someone you do not trust, refuse loudly, and hand
a model to a mapper. #451 (`.circ`) is the first, #621 (`.cv`) the third, #629
(Falstad) the fourth, and CAP-29 (#513) explicitly promises the marginal cost
of each additional format is small because "the `.circ` importer builds the
report machinery once."

The report machinery is being built once. The *ingest* machinery is being built
four times. #612 is where that becomes visible, and the coordination comment
already half-sees it: #451 §7.5 deliberately deferred promoting the hardened
parser configuration until "a second caller fixes it," and names #612 as that
caller. So the trajectory-correct reading of this task is **#612 is the
promotion task, not the second copy** — and its acceptance criteria, which are
written entirely as "the .dig parse does X," never say so.

## Reframing 1 — make the confinement an architectural proof, not a vector suite

AC-1 asks for one test per attack vector; the boundary note asks that "no code
path may lead from document content to the filesystem or the network." The
second sentence is far stronger than the first, and JLS already knows how to
enforce it. `test/jls/SocketConfinementRatchetTest.java` proves a
repository-wide *absence* — socket construction may appear only under
`src/jls/collab/net/` — and `test/jls/ArchitectureRulesTest.java` already
carries ArchUnit (`com.tngtech.archunit`) doing the bytecode half of the same
job for `JOptionPane` and `jls.hdl`.

The elegant route is therefore:

1. One package — call it `jls.imp.xml` — owning the only `XMLInputFactory` /
   `DocumentBuilderFactory` construction in `src/`, configured once.
2. An ArchUnit rule: no class outside that package may depend on
   `javax.xml.parsers`, `javax.xml.stream`, or `org.xml.sax`; and no class
   under `jls.imp..` may depend on `java.net.URL`, `java.net.URI#toURL`,
   `java.nio.file.Files`, or `java.io.FileInputStream`.
3. The four-vector suite runs **once**, against that factory, not once per
   format.

That converts "one test per attack vector, per importer, forever" into a proof
that holds for #621, #629, and the fifth format nobody has filed. It also
makes the boundary note's absolute claim mechanically true instead of
aspirational — today it is a promise an executor can violate with one
`Files.readString` and no failing test.

## Reframing 2 — AC-2 has already chosen a streaming parser; say so

"Bounded depth" is not implementable over a DOM. `DocumentBuilder.parse`
materializes the tree before you can count anything; the only bounds available
are JAXP's global limit properties, which fail with a JAXP-worded message, not
"a named bound" as AC-2 requires. A StAX pull parse (`XMLInputFactory` with
`SUPPORT_DTD=false`, `IS_SUPPORTING_EXTERNAL_ENTITIES=false`,
`setXMLResolver` to a throwing resolver) makes depth, element count, attribute
count and text length ordinary counters in the read loop, each refusable with
its own named limit and its own `LoadError` category — and never allocates the
document. AC-2 stops being a check and becomes a structural property, the same
way `FileAbstractor.BoundedInputStream` (`src/jls/FileAbstractor.java:347-409`)
makes the XZ bomb structurally impossible rather than detected.

Note this diverges from #451 §7.9, which speaks of a per-parse
`DocumentBuilderFactory`. If #612 is the promotion task, the promotion should
be to StAX, and #451's DOM shape should be the thing that changes.

## Reframing 3 — the missing seam is `format.importer`, not a fourth reader

`docs/extension-points.md` lists **Importer** as a *pending* seam with the
contract "to be defined," home package `jls.hdl.imp`, owning issues #61/#62.
Meanwhile CAP-29 is filing four readers that will each need: a file-extension
gate, a content sniffer, a File→Import menu wiring, a CLI path, and a help
page. `JLSStart` already hardcodes `is not a valid circuit file name` at four
separate sites (lines 189, 303, 501, 572) and `.jls` suffix tests at lines
1119, 2242, 2494. Four formats × those sites is exactly the
"adding an element touches sixteen places" pathology ARCHITECTURE.md records
as the thing #78 exists to kill — reappearing one dimension over, in the
importer axis, while the registry that would prevent it sits in the catalog
marked *pending*.

The higher-leverage version of this task is: #612 defines
`format.importer` as a typed `ExtensionPoint` (id, display name, extensions,
`sniff(bytes)`, `read(source) -> Model | Refusal`), contributes the `.dig`
reader through it, and gets the extension gate, the file chooser filter and
the CLI dispatch from the registry. Then #621 and #629 are a mapping table and
a construct-map document each — which is what CAP-29's economics already
assume, and currently nothing makes true.

## Reframing 4 — I am disregarding the atomicity coupling for test sections

AC-4 preserves Digital's test sections so #562 can translate them, and
TASK-C558-5 makes import atomic: any unmappable construct refuses the whole
file. Compose those and you get a bad outcome for the exact user CAP-29 exists
for. An instructor with a decade of `.dig` material has circuits that will
contain something JLS cannot map — that is the *premise* of the loss report.
Under atomic import, that instructor gets no circuit **and no test vectors**,
even though the test sections are pure text with no dependency on whether the
circuit mapped.

The reframing: test-section extraction is a separate, independently reachable
operation over the same parse — `jls -export-tests out.t circuit.dig` — that
succeeds whenever the *document* parses, regardless of whether the *circuit*
maps. Atomicity is the right rule for "produce a circuit"; it is the wrong
rule for "recover the grading suite." This costs nothing in #612 (the parse is
shared) and it changes AC-4 from "preserve for a downstream consumer" to
"preserve as a first-class output," which is a much easier thing to test and a
much more valuable thing to ship. PF-5 is called "the piece that actually
converts courses"; it should not be gated on the piece most likely to refuse.

## Reframing 5 — the alternative that would delete this task, and why it should be priced first

KC-29-1 already contemplates downgrading a format to "a documented external-
conversion recipe." Nobody has priced that branch for `.dig`, and JLS has an
asset that makes it unusually cheap: `src/jls/hdl/imp/NetlistImporter.java`
already imports circuits from an external tool's output via the Yosys
subprocess boundary, with the aggregate-and-refuse discipline and the
`ImportSummary` report shape (#61). Digital ships HDL export and a headless
mode. If `digital` → Verilog → `yosys write_json` → `NetlistImporter` recovers
a usable circuit, then the whole 4–6 mw `.dig` branch — this task included —
collapses to a documentation page and a mapping-gap table.

I do not think this wins, and I am not recommending it: it loses layout
(the instructor's circuit becomes unrecognizable), it loses the test sections
entirely, and it requires students to install Digital. Those are exactly the
three things that make the native importer worth building. But that argument
does not appear anywhere in #513, #558 or #612, and a half-day spike that
records it would either kill 4–6 maintainer-weeks or convert the strongest
"why not just…" objection into a cited answer. That is a good trade at any
price, and it belongs *before* #612, not after.

## Concrete gaps in the criteria as written

- **XStream is named and not prohibited.** The title and #513 both say "XStream
  XML." An executor reading that may reach for the XStream library, which is a
  Java object-graph deserializer with a long CVE history (the CVE-2021-39144
  family) — precisely the remote-code-execution primitive
  `SocketConfinementRatchetTest`'s second ratchet bans under `jls.collab`.
  #451's DoD carries "No new Maven dependency added for XML parsing"; #612
  carries no equivalent line. Add it, and add the positive statement: `.dig` is
  read as plain XML over a known element vocabulary; XStream's `class` and
  reference attributes are data to be reported, never dispatched on.
- **"Byte-recoverable" (AC-4) is not achievable through a conforming parser.**
  XML 1.0 §2.11 mandates CRLF→LF normalization, character references expand,
  and attribute values are normalized. Any parser that satisfies AC-1 will
  therefore fail a literal byte-equality test. Either restate the contract as
  "character-recoverable after XML normalization, round-trip asserted," or —
  better, and free under Reframing 2 — capture `Location.getCharacterOffset()`
  at the start and end of the test section and slice the source bytes, which
  gives real byte recovery *and* gives #562 a source location for its loss
  report.
- **`ordering_after: [314]` omits #451/#323.** The whole "second caller
  promotes the configuration" arrangement is recorded only in a comment. If
  #612 lands first there is nothing to promote and it silently becomes the
  tree's first XML parse — with none of #451's five-vector `CircHardeningTest`,
  because that test lives in the other task. Encode the edge.
- **No `LoadError` category is named.** `src/jls/LoadError.java` owns the
  refusal taxonomy (`MALFORMED`, `LIMIT_EXCEEDED`, `NOT_A_CIRCUIT`, …) and
  ARCHITECTURE.md makes it the single contract every front end renders. AC-2
  and AC-3 describe refusals in prose without saying they land in that
  taxonomy. They should, or importers will grow a parallel error vocabulary.

## Verdict

Endorse the work; reframe the deliverable. The `.dig` parse must exist, must be
hostile-input-hardened, and must preserve the test sections — none of that is
in doubt. What is wrong is the *unit*: as written, #612 produces a `.dig`-
shaped parser and a `.dig`-shaped test suite, and leaves #621 and #629 to
rediscover the same discipline by prose inheritance. As the second XML caller
in a tree that already carries ArchUnit, a confinement-ratchet idiom, a typed
extension-point catalog with `Importer` marked pending, and a bounded-stream
precedent in `FileAbstractor`, this task is standing on every piece it needs to
ship a *substrate* instead. Retitle it accordingly, keep all four acceptance
criteria, and add: one hardened-XML package, one ArchUnit confinement rule
covering filesystem and network reachability from `jls.imp..`, a StAX read loop
whose bounds are named counters, and a `format.importer` extension point that
the `.dig` reader is merely the first contribution to.
