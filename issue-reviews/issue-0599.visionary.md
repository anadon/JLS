# Issue #599: FEAT-C38-3: the Basys-3 question gets a written answer — supported with its toolchain named, or refused with the cost arithmetic — so the board the ASEE courses actually own stops being unaddressed
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its acceptance criteria, #599 asks for one thing: *stop letting the
most-owned classroom board be a silence*. That is squarely aligned with the
project's strongest habit — the one that separates it from Logisim-Evolution,
whose board-download flow is simultaneously its best feature and its top
reliability complaint (#522's own framing, and `docs/hdl-support-research.md`
§7.1 on `VendorSoftware.java`). JLS's counter-position is that every claim about
hardware carries its evidence and every refusal carries its price. A Basys-3
answer belongs in that spine. Endorsed on outcome.

But the issue frames the question three ways I think are wrong, and each
reframing makes work disappear or makes the answer better.

## Reframe 1 — the axis is not "vendor board vs open board"; it is "is there a checkable consumer?"

The issue treats Basys-3 as categorically outside the flow because it is an
Artix-7, and offers exactly two doors: Vivado-flavored support or a priced
refusal. It names openXC7 in one parenthetical and then never opens that door.
It is the interesting door.

The project's real admission rule is not open-silicon; it is stated in
`docs/standards-adoption/06-fpga-constraint-formats.md` under "The artifact a
claim rests on": evidence level 2 is an *automated* external-tool check
(`nextpnr-ecp5`/`nextpnr-ice40` parsing the emitted file against a real device
database, armed via `jls.hdl.ToolLocator` + `assumeTrue`), and level 3 — the
manual dated vendor run — is a fallback for formats with no such consumer. Doc
06 asserts flatly that "XDC and QSF emission will be golden-pinned but never
machine-validated" and orders XDC "only once a Vivado rig exists." That premise
predates the yosys → `nextpnr-xilinx` → prjxray line maturing; `nextpnr-xilinx`
consumes XDC directly (`--xdc`) and xc7a35t/Basys-3 is its most-walked target.
The XDC subset it honors — `PACKAGE_PIN` and `IOSTANDARD` on ports — is
*exactly and only* what JLS emits, because #213 H2 refused everything else.

If that holds on inspection, the shape of the answer changes completely:
Basys-3 becomes an ordinary open-flow board, `Board.Format.XDC` joins the total
dispatch #416 is already building, and the file gets a CI oracle the vendor
formats were assumed never to have. KC-38-1 is untouched — openXC7 is not
driving Vivado as a process, it is the same delegate-to-external-tools rule
`docs/icestick-bitstream-handoff.md` already lives by. **This is the concrete
alternative I would spend the first half-day on before writing any refusal.**

Honest caveats, because this is the reframing's own failure mode: openXC7's
chipdb build for a 7-series part is heavy (multi-GB, long) and belongs in an
armed-when-present lane, not the required gate — which is precisely the policy
#386 already owns. And *acceptance by nextpnr-xilinx is not acceptance by
Vivado*: those are two claims, and if the ASEE courses in #522's evidence run
Vivado, an openXC7-only pass must never be quoted as vendor acceptance. The
right output is a two-column claim — "parses under openXC7 (CI, dated)" and
"Vivado: not attempted" — which the ladder below expresses natively.

## Reframe 2 — a per-board decision document does not scale; a board-admission ladder does

AC-1 through AC-5 produce one artifact about one board. The same unanswered
question exists verbatim for DE10-Lite/Quartus, Tang Nano/Gowin, TinyFPGA, and
whatever the next course owns; doc 06 already sketches DE10-Lite and ULX3S
entries. Writing a bespoke verdict document per board means re-litigating the
policy per board, which is the thing AC-5 says it wants to prevent — and it
prevents it only for readers who find that one file.

The better cut: make "supported" graded and machine-derived instead of binary.
The rungs already exist, scattered across four places:

1. **emitted** — a `Boards` entry with a byte-pinned golden (`PcfGoldenTest`);
2. **parsed** — an open place-and-route tool accepts the file in CI
   (doc 06 evidence level 2; `ToolLocator` + `assumeTrue`);
3. **vendor-accepted** — a dated manual run per doc 06's certification section;
4. **flashed** — a row in `docs/board-flash-record.md`, whose presence #416's
   `FlashRecordTest` already makes a build requirement.

Give `Board` a status/evidence component, put the four rungs in one table in
doc 06 or §7.5 of `docs/hdl-support-research.md`, and test that every entry's
claimed rung has its artifact. Then Basys-3 is not a decision at all — it is a
row that starts at rung 1 or rung 0 and advances as evidence accrues, and board
N+1 costs a table row rather than a feature issue. #264's "both halves" rule
survives intact as the definition of the top rung.

## Reframe 3 — make the decision executable, not merely greppable

AC-1's discoverability test is "searching the repository for Basys-3." That is
the weakest discovery channel the project has: the student who owns a Basys-3
has a jar, not a clone. The moment of the question is `jls -export d.v -board
basys3 …`, and today that produces `unknown board` with a list that says
`icestick`. A ~20-line known-but-unsupported table consulted by that error path
("basys3 — AMD Artix-7 XC7A35T; JLS does not emit XDC today; see
docs/board-handoff.md §Basys-3") delivers the verdict at the point of need,
cannot rot silently (a test asserts each entry names a live doc anchor), and
costs a fraction of a maintainer-day. The prose document remains, but it stops
being the only carrier of the answer.

## Where I am disregarding the stated acceptance criteria

- **AC-1's implied new standalone document.** Board policy is already spread
  across `README`, `docs/hdl-support-research.md` §7.5, doc 06, the handoff doc,
  and (per #416) a forthcoming `board-handoff.md` and `board-flash-record.md`.
  A sixth home is how one truth becomes six drifting ones. Land the verdict as
  (a) an amendment to doc 06 — where the arithmetic already is — plus (b) a
  short "Recorded decisions" entry in `ARCHITECTURE.md` with a revisit trigger,
  the exact house pattern used for i18n, plugins, and simulation strategy.
- **`ordering_after: [264, 416]`.** The decision costs ~0.5 md as
  consolidation and should come *first*, because its answer changes #416's
  shape: if openXC7 is the answer, XDC belongs in the same emitter
  generalization as LPF rather than as a later bolt-on onto a dispatch that has
  already frozen around two open formats.
- **The "refused" branch's novelty.** Doc 06 already contains the refusal and
  its arithmetic: 8–10 maintainer-days across three formats, ~1 day for the XDC
  emitter and Basys-3 entry, 1.5–3 days for the first vendor acceptance run,
  "Vivado and Quartus cannot be in CI, and that is final," and "XDC + Basys 3,
  only once a Vivado rig exists." Do not re-derive it; cite it, and note that
  #522's ASEE evidence *satisfies* doc 06's own "do NOT do this if no user has
  named a board" gate — which is the one genuinely new input this issue brings.

## Smaller notes

- The `D8` / `D10` vocabulary and the "D8 cost table" are not resolvable
  anywhere in-tree; the only in-repo priced-rejection register is
  `docs/standards-adoption/11-costed-rejections.md`, whose four-part
  self-assertion definition is the right template for this verdict. If "D8
  form" means something else, the executor cannot check compliance.
- AC-2's "no GUI change" is right and worth keeping under any framing: the
  constraint filename is already format-driven (`board.format().extension()`),
  so a new format needs zero CLI surface. That existing design is the reason
  this whole question is cheap.
- The one hazard no reframing removes: a transcribed Basys-3 pin table is
  syntactically perfect and physically wrong until a human flashes it. Both doc
  06 and #416 say so; the ladder makes it a visible rung instead of a footnote.

## Verdict

**endorse-with-reframing.** The outcome — the board with the largest installed
classroom base stops being an unanswered question — is correct and overdue.
Reframe the work as: test the openXC7 premise first (half a day, potentially
converts a refusal into a supported board with a CI oracle); express the result
as a rung on a general board-support ladder rather than a bespoke per-board
verdict; surface it in the unknown-board error path, not only in a file a
student will never grep; and file it as an amendment to the documents that
already carry the arithmetic instead of a new one.
