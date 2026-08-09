# Issue #837: TASK-C572-3: the verdict is written down with its numbers and its ranked fallback, so PF-2 starts building instead of re-arguing
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its yaml, #837 asks for one thing: a durable, dated, in-tree record of a
mechanism choice, with the reversal condition stated, so the next person re-checks
instead of re-arguing. That instinct is the single best habit this project has. It is
already institutionalized: `ARCHITECTURE.md` §"Recorded decisions" (line 233) holds
entries in exactly the schema #837 describes — "Each carries its rationale and the
trigger that reopens it" — and each entry points at a deeper evaluation document
(`docs/flatlaf-evaluation-2026-07.md`, `docs/library-survey-2026-07.md`,
`docs/picocli-evaluation-2026-07.md`). The i18n entry is literally a pre-declared
refusal with revisit triggers and a standing "PRs adding partial scaffolding will be
declined." That is #837's AC-1 and AC-3, already shipped as a genre.

So the first reframing is cheap and I would take it regardless of everything below:
**AC-1 should not invent a new "decision document" — it should add one entry to
`ARCHITECTURE.md` §Recorded decisions and put the measurements in
`docs/browser-demo-evaluation-2026-08.md`.** Two artifacts the project already
maintains, one of them the file a new contributor actually reads. A standalone
decision doc filed nowhere in particular is a document that gets re-litigated because
nobody finds it — the exact failure #837 exists to prevent.

## The larger claim, and where it pulls against the arc

The deeper problem is not the document. It is what the document is being asked to
decide, and in which order. The chain #572 → #833 → #835 → #837 treats CheerpJ as the
default and "headless-rendered SVG driven by a pre-computed VCD player" as fallback
(a). On this repository's own evidence that ranking is inverted, and #837 is where the
inversion gets frozen into the record.

**CheerpJ ships the editor.** Wrapping the JLS jar puts `jls.edit.Editor` — File→Open,
Save, Save As, the full palette — into the browser. CAP-32's KC-32-2 says anything that
turns the demo into a browser *editor* is out of scope *by construction*, citing the
CAP-19 (#500) `not_planned` closure. #835 AC-3 demands read-only "verified by inspection
of what the wrapper exposes, not by the absence of a button," but the wrapper exposes
the whole jar; the only thing making it read-only is the browser sandbox's virtual
filesystem. That is read-only by *accident of hosting*, which is precisely the
distinction #835 was written to refuse. No load-time measurement resolves this. It is a
design contradiction, decidable today, and it sits on the go path rather than the no-go
path.

**It pulls against the distribution arc.** Half of `README.md` is about reproducible,
attested, SBOM-carrying artifacts: byte-reproducible jar re-checked on every push,
`gh attestation verify`, SignPath signing, and honest prose about which guarantees do
*not* hold. The `area:distribution` label on this very issue reads "Reproducible/attested
packaging & supply chain." CheerpJ is a closed-source proprietary WASM JVM whose standard
delivery is a vendor-CDN loader — against a capstone AC of static files only and #835
AC-4's zero-external-requests check. #833 AC-5 correctly flags licensing and payload,
but again: those are answerable from the vendor's terms in an afternoon, not from
timing three circuits.

**It validates nothing and can't be gated.** JLS's architecture is a headless core with
frozen public contracts: `jls.sim` imports no AWT/Swing and `HeadlessCoreRatchetTest`
enforces it; the `-vcd` profile and batch stdout are a documented stability promise
(`docs/batch-interface.md` §4, §"frozen as specified"); `SvgExportTest#exportingTwiceIsByteIdentical`
holds the SVG path deterministic. A CheerpJ demo consumes none of that and can be
regression-tested by nothing; every JDK or Swing change is a silent risk to it. This is
the same failure mode CAP-19 §3.1 built PF-4 to prevent, arriving from the other side.

## The alternative the chain never costs: (a) is nearly built

Fallback (a) is not a consolation prize. It is the option that rides the project's own
seams:

- **Rendering ships.** `-i out.svg` works today — `CircuitRenderer.exportImage`
  (`src/jls/edit/CircuitRenderer.java:314-360`) draws every element through
  `ElementRenderers.draw` into JFreeSVG with a fixed defs prefix and a deterministic
  draw order, byte-identical across runs.
- **Signals ship.** `jls -b -vcd out.vcd circuit.jls` emits IEEE 1364-2001 VCD,
  deterministic and CI-verified by a spec-derived parser (`VcdExportGoldenTest`), with
  a frozen profile.
- **Identity ships.** Stable element ids (#165, `Element.getID`).

The one missing piece is that the exported SVG is a *flat* Graphics2D dump: no `<g id>`
per element or per net, so JS has nothing to recolor. The seam is the two
`ElementRenderers.draw(svg, el)` loops — wrapping each in a JFreeSVG group keyed by
element id makes the picture addressable. That is the honest spike: "can a grouped SVG
plus a VCD-driven JS scrubber reproduce the toggle-and-watch-the-trace experience?" It
is smaller than the CheerpJ spike, it is testable in CI, it makes the SVG export and
the VCD profile *second consumers of their own contracts* — the exact argument CAP-19
§2 made for PF-1 — and its payload is a few hundred KB, which makes the ≤15 s threshold
and the <30 s capstone AC non-discriminating. When the load-time question stops
mattering, the whole go/no-go apparatus in #833 collapses to a fidelity question, and
the ranking becomes a design decision instead of a stopwatch reading.

Note what nobody in the chain does: **cost alternative (a) at all.** #833 and #835
measure only CheerpJ; #837 would then declare (a) the winner having measured nothing
about it. A verdict document whose winner is un-evaluated is not the end of
re-litigation, it is its guarantee.

One more note on premise: the "three biggest example circuits" do not exist.
`examples/` is 16 KB containing only `autograde/`; `test/fixtures/` holds four files.
The curated set is #548, unfiled work. The measurement is being specified over a corpus
that has to be authored first — another sign this is a design decision wearing a
spike's clothes.

## What I am disregarding, and why

I am disregarding **AC-2's premise** that the ranking is something the document does
"on no-go," and **AC-4's** verbatim-quotation requirement. AC-2 encodes the inverted
default; if the mechanism is chosen on architectural grounds, the document ranks *all*
candidates once, with CheerpJ recorded as considered-and-refused and the refusal's
reversal trigger stated (e.g. "an Apache/MIT-licensed, self-hostable JVM-in-WASM with a
published reproducible build"). AC-4's verbatim quoting is cargo cult: a recorded
decision states the scope cliff in its own words and cites #500 — that is what every
existing entry in §Recorded decisions does, and it is more durable than a quotation.

I would also fold **AC-5** and this issue's separate existence into #833/#835. A spike's
deliverable *is* its verdict; splitting "write down the conclusion" into a third,
last-ordered task is how projects end up with the experiment landed and the conclusion
never written — which is the precise failure #837 names in its own title.

## One further opportunity

The project's binding refusals currently live only in GitHub issue bodies and comments:
CAP-19's `not_planned` closure, KC-32-1/KC-32-2, #63's rejection of live co-simulation,
#221's sole-execution-strategy ruling. `docs/vcd-interop.md` already has to re-state #63
in prose because the issue is not a durable artifact. #837 is a natural moment to make
§Recorded decisions the register of record for kill criteria and closures too — one
entry per refusal, each with its reversal trigger. That converts a one-off document
into the mechanism the whole capstone program has been asking for.

## Verdict

**endorse-with-reframing.** Keep the artifact and the anti-relitigation discipline;
retarget it at `ARCHITECTURE.md` §Recorded decisions plus a dated evaluation doc; invert
the default so the SVG+VCD mechanism is the candidate and CheerpJ the refusal with a
stated reversal trigger; and fold the writing of the verdict into the spike that
produces it rather than leaving it as a separable last task.
