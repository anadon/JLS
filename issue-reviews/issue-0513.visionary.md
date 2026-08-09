# Issue #513: CAP-29: a course arriving from Digital, CircuitVerse or Falstad opens its existing circuits in JLS with every loss named — the .circ path gains three siblings on shared loss-report infrastructure
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the six PFs away and the claim is: **JLS should be the tool that never silently
loses your work, and can prove it.** That identity is real, unclaimed in this niche, and
consonant with everything the project already is — a normative `docs/simulation-semantics.md`,
a stability-contracted `-t` interface, a `LoadError` taxonomy, golden tests as oracles.
"Every loss named, located, explained" is the same discipline pointed at foreign input.
I endorse that. Three formats' worth of parsers is the *implementation* of that claim, and it
is the part I want to change.

## Where the issue pulls against the project's own arc

**1. The seam already exists, is declared, and is undefined — and this issue never mentions it.**
`docs/extension-points.md` line 33 carries a live row: `Importer | hdl.importer | cell-map/layout
contract to be defined | jls.hdl.imp | many | on-command (import) | pending (#61/#62)`. That is
PF-1's job, already reserved, in the catalog `ExtensionPointCatalogTest` cross-checks in both
directions. PF-1 as written ("generalize CAP-16/FEAT-025's loss-naming report") instead makes the
contract a *derivative* of one unbuilt importer, ordered after it. That is the textbook way to get
a generalization shaped like its single instance. Reframe PF-1 as: **define `hdl.importer`'s
contract now, against the two consumers that already exist** — `jls.hdl.imp.NetlistImporter`
(shipped, 1,067 lines) and the `.jls` loader's own silent-drop behavior
(`docs/file-format.md:220`, "Unknown attribute names are silently ignored"). Retrofitting
`ImportSummary` (which today counts cell types and coerced `x` bits — a *tally*, not a
disposition report) to the source-construct → disposition → location → explanation shape is
work available today, needs no new parser, no XML, no corpus, and no CAP-16. It is also the
only way the contract can be shown format-agnostic rather than asserted to be.

**2. The shipped importer is unreachable, and this issue proposes three more.**
`grep` over `src/` finds no call site for `NetlistImporter.importNetlist` outside its own
package — no CLI flag, no menu (#510 §1 footnote 2 says the same). JLS has an importer, a
layouter, a result type and a summary type that no user can invoke. Adding .dig, .cv and
Falstad parsers to that is building the second, third and fourth floor of a building with no
door. The cheapest capstone-serving increment in this entire family is a `-import` flag and a
File→Import entry over code that already exists, and it belongs ahead of PF-2.

**3. Import is the wrong verb; the shipped code already knows it.**
`ImportResult` holds a `String saveText` — "the JLS plain-text save format for the imported
circuit, ready to load through the real loader … loading it and re-saving yields a circuit
indistinguishable from a hand-drawn one." The shipped importer is a **pure text→text
converter** with zero coupling to `Circuit`, the editor, or undo. That is the elegant seam, and
this issue's AC-4 ("import is undoable") cuts across it — undoability only becomes a problem if
import mutates an open document. Keep the converter framing and **AC-4 disappears entirely**:
the source file is untouched, the output is a new `.jls` next to it, the report is a sibling
artifact, and "undo" is `rm`. This also matches the project's own recorded boundary decision
(ARCHITECTURE.md, #222): external tool integrations — Yosys, GHDL, ELK — sit on a subprocess
boundary, deliberately. A `.dig`/`.circ`/`.cv` parser is untrusted third-party input with a
per-format maintenance obligation to a format JLS does not control; it has more in common with
Yosys than with `Element`.

**Concrete alternative: `jls-convert`, a separate artifact.** One converter binary (or a
headless mode of the same jar, but a distinct module with no AWT and no `jls.edit` dependency,
the way `jls.sim` already is) that reads a foreign file and writes `circuit.jls` +
`circuit.migration.json`. Consequences, all of them wins:
- The XXE surface (AC-5) never enters the GUI process; hostile-input hardening is scoped to a
  tool users run deliberately on files they chose.
- CAP-16's Open Question 1/2 — absorbing Logisim's GPLv3-*only* port geometry costs JLS its
  "or later" — becomes a per-artifact licensing question. A separately-licensed converter can be
  GPL-3.0-only without changing JLS's own posture. That unblocks the single decision CAP-16
  says blocks #323 from starting, and it is worth more than everything else in this review.
- Converters are the natural home for community contribution: a format JLS's maintainer has
  never used can be contributed and abandoned without touching core, which is what actually
  makes an "importer family" affordable for one maintainer.
- Batch migration (an instructor with 200 files) is a shell loop, not a UI flow — which
  CAP-16 Open Question 6 already concluded is the right shape.

## Ordering: the highest-value item is fifth

PF-5 (.dig embedded tests → `-t` vectors) is described in this issue's own text as "the piece
that actually converts courses," and #510 scores testing/grading as JLS's **only** 5/5 axis
while every rival's tracker documents the pain there. It is also the one PF that is *separable
from schematics*: an instructor can redraw a circuit in an afternoon, but a semester of
grading vectors is the asset they cannot rebuild. A standalone `.dig`-testcase → `-t`
translator is a 2-3 mw artifact with no XML circuit importer behind it, it exercises the
report contract on a small domain, and it sells JLS on its strength instead of asking a
migrating instructor to first evaluate JLS on its weakest axis. **Put PF-5 first and let it be
the demo slice.**

The nominated demo slice (PF-4, Falstad) is chosen for size, not for representativeness, and
it is the least representative of the three. Falstad is an analog netlist whose digital
behavior is emergent; "analog elements are named losses by design" means the report for a
typical file is one enormous undifferentiated loss line. That proves nothing about whether the
contract generalizes — it proves the contract can say "no." Per #510's own verdict, Falstad's
core "is not winnable — do not contest it"; the winnable slice there is the *overflow* course,
which is better served by a "coming from Falstad" concept page (PF-6/#553) than by a parser.

## Format triage I would actually fund

- **.dig — yes, but understand what gates it.** PF-2 says generics "map or refuse by name until
  FEAT-017 lands." Digital's parameterized circuits are, per #510, one of Digital's headline
  advantages over JLS. So the importer that refuses them imports the *easy half* of Digital's
  corpus, and the users worth winning — the ones running a real course library — are
  disproportionately in the refused half. That is a coverage cliff, not a footnote; PF-2's value
  is tied to #357 far more tightly than the band admits, and pricing PF-2 at 4-6 mw as if
  FEAT-017 were optional understates it.
- **.cv — defer, and say why in public.** #510's own verdict: browser-first students are "not
  winnable without a web story," and CircuitVerse's delay is a queue priority. A .cv import can
  only ever be structural; the report line "delay is not semantics-preserving" means every
  timing-dependent circuit arrives with unstated fidelity. 3-5 mw to reach a segment the
  evidence says is structurally unreachable is the weakest row in the capstone.
- **Falstad — a documented conversion recipe, which is KC-29-1's own fallback.** Reach that
  conclusion by analysis now rather than by burning 1.5× the band to get there.

## What I am disregarding, explicitly

- **AC-4 (undoable import)** — dropped, not weakened. It is an artifact of the wrong verb; see
  above. Replace with "the source file is never written, and the output is a new file."
- **AC-1 (one real circuit per format, zero unexplained losses)** — as a *gate on all three
  formats simultaneously* it forces the capstone to be all-or-nothing. Make it per-format, so
  a format can be dropped to a recipe under KC-29-1 without holding the others hostage.
- **The sibling/split with CAP-16 (#311)** — this issue already offers "if CAP-16's REPLAN
  prefers absorbing this scope, merge there." Take that offer. Two capstones, both named
  migration parity, sharing report infrastructure, one demo slice, a jointly-owned PF-6, an
  `ordering_after` that already cites the wrong issue number (per the coverage comment: #323,
  not #311), and a "lower number wins" tiebreak rule is bookkeeping that exceeds its own
  subject. One program, one report contract, N converters, one kill criterion per converter.

## The precondition nobody in this thread owns

#510 §4 ranks four universal gates ahead of every migration lever: shop window, first-run
experience, chronogram, published benchmark. A converted course lands today in a tool that
opens to an empty `JTabbedPane`, ships no discoverable examples, and has no waveform view — and
#510 says every teardown independently found the switcher leaves in the first ten minutes.
Ingest capacity multiplied by zero retention is zero. **Import work should be gated on the
on-ramp gates landing, not merely ordered after CAP-16.** That is a scheduling claim, but it is
the one that decides whether 13-20 mw here produces users or only produces files.

## Verdict

**endorse-with-reframing.** The outcome — no silent losses, ever, provably — is the right thing
for JLS to become and the cheapest genuine differentiator on the board. Reshape the delivery:
define `hdl.importer`'s contract first against shipped consumers, surface the importer that
already exists, ship converters as a separate artifact on the recorded subprocess boundary,
lead with .dig test vectors, fund .dig circuits second, and convert .cv and Falstad to
documented recipes until evidence moves.
