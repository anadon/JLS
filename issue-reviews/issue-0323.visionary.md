# Issue #323: FEAT-025: a course's existing Logisim-Evolution material opens in JLS with its structure intact and every loss named, located and explained
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

The end is not "JLS can parse `.circ`". The end, stated plainly in #311's abstract,
is that **an instructor moves a decade of course material instead of rewriting it**.
Everything else — the XML reader, the construct map, the report equality, the part
data — is a chosen means. I judge the means, and I think the issue has cut along the
most expensive seam available, in a direction that partly contradicts the project's
own recorded architecture, while leaving the load-bearing obstacle unmentioned.

The goal is right and well aligned. JLS's whole arc (README, `docs/batch-interface.md`
as a stability contract, the container image for autograders, `docs/vcd-interop.md`,
the Yosys pipeline) is a tool that wants to be adoptable inside an existing course.
A migration path belongs on that arc. The plan for getting there does not.

## The obstacle the issue never names: the value domain

`docs/simulation-semantics.md` §2 is normative and blunt: JLS is **two-state plus
whole-signal HiZ**, "There is no unknown/X state anywhere in the system", and "HiZ is
all-or-nothing per signal … there is no per-bit HiZ". Logisim-Evolution's value domain
is per-bit `{0, 1, U, E}`, and its test-vector files (`docs/test_vector.md`) routinely
carry don't-cares and expect the error value on conflicting drivers.

So CAP-16 §1 step 2 — "replay those same vectors against the imported JLS circuit,
observe identical outputs" — is not gated on this issue at all. It is gated on
`docs/capability-roadmap/keystone-b-migration.md`, which prices the four-state core at
**17–22 maintainer-weeks** and calls the value domain the keystone. A structurally
perfect `.circ` reader lands into an engine that cannot express half of what the
incumbent's vectors assert. #311 §3 risk 6 gestures at this ("a mismatch may be a
correct JLS answer to a differently-meant question") and then routes it to a prose
write-up. It is not a write-up problem; it is a representability problem, and it sits
under the one criterion that would actually convince an instructor.

This does not kill the feature. It reorders it: **the parser is not the spine of
CAP-16, the value domain is.** The reader can land first, but the issue should say so,
and CAP-16's sufficiency argument should stop implying the reader buys step 2.

## Alternative framing 1: use the incumbent as the ETL front end

The issue asserts "No converter from this format to any other format exists anywhere."
That is true of `.circ` → anything, and false of the thing that matters:
`docs/hdl-support-research.md` §7.1 records, from a source-level read, that
Logisim-Evolution **exports both VHDL and Verilog** via its own per-component generator
framework, and that it ships a **headless CLI** (`--test-vector` with exit codes,
`--tty csv/table`, `--substitute`). JLS ships the other half of that pipe today:
`src/jls/hdl/yosys/` (`YosysNetlist`, `CellValidator`, `YosysLocator`) and
`src/jls/hdl/imp/NetlistImporter.java`, already wired to the layouter at line ~104.

`.circ` → LE's own HDL export → Yosys → `write_json` → the shipped `NetlistImporter`
is a working path whose only missing piece is glue. Its limits are real and should be
stated, not hidden: it covers only what LE can generate HDL for, it discards appearance
entirely, and it requires the instructor to run the old tool once. But look at what it
buys against #323's own §5:

- **I6 (the corpus run) becomes a weekend, not a gate on 6–18 mw.** Run LE headless
  over 30+ public `.circ` files and record which ones export HDL at all, and which
  cells Yosys then emits. That is a real measured construct distribution — exactly what
  #311's KC-16-1 demands — obtained before a single line of XML parser exists.
- **AC-3 gets an oracle for free.** LE's `--tty table` on the same vectors gives a
  differential harness that will surface the four-state divergence above as data rather
  than as an argument.
- **The geometric-connectivity hazard (KC-16-2, silent disconnection) disappears
  entirely on this path**, because connectivity comes from LE's own elaboration, not
  from JLS's guess at `δ(k, n, s)`.

I am not proposing this as the migration product. LE's HDL generator does not cover
pedagogical elements (probes, LEDs, buttons, its TTL library) and the result reads as a
synthesized netlist, not as the author's schematic. I am proposing it as the
**measurement instrument and the differential oracle**, and as a shipping fallback for
files the native reader refuses. It costs days, it uses two subsystems JLS already
owns, and it retires the single largest unknown in a 30–50 mw capstone before that
capstone is funded. The issue's §6 says the corpus run "is the gate on the estimate,
not a follow-up" and then puts it behind two tasks. This inverts that correctly.

## Alternative framing 2: measure δ, do not absorb it

Open Question 2 blocks filing a child on a licence decision, and offers absorb / ask /
clean-room, recommending against clean-room. There is a fourth option the issue never
considers: **derive the port-offset table by observation.** Generate probe `.circ` files
(one component, swept over input count and body size), open them in the incumbent, and
read back where its own ports land — via its HDL export's port ordering, its
`--tty table` behaviour on deliberately abutted components, or its saved geometry.
What you commit is a table of measurements plus the generator that reproduces it.

Engineering-wise this is strictly better than transcription: the table is
**re-derivable** against a new upstream release, it comes with its own regression
harness, and it fails loudly on a version bump instead of silently drifting. Whether it
also changes the licence posture is a question for counsel, not for me — but it is the
option most likely to dissolve OQ2 rather than answer it, and OQ2 currently blocks
filing children. Worth putting in front of the maintainer before either of the three
recorded options is chosen.

## Alternative framing 3: quarantine, don't drop-and-report

§4 invariant 1 says "no partial circuit is ever emitted", inherited from
`NetlistImporter.java:41-47`. §1 criterion 2 says the import proceeds and names what it
dropped. **Those are the same sentence pointing in opposite directions** — an import
that omits constructs and lists them *is* a partial circuit; the issue survives the
contradiction only by defining "partial" narrowly. The shipped precedent resolves it the
other way: `ImportSummary` is a *mapping* summary with no losses side, because
`NetlistImporter` refuses rather than degrades.

The elegant resolution is to make the loss set empty by construction: import every
unmapped construct as a typed, inert **`ForeignConstruct` placeholder** carrying its
original source fragment, its location, and the reason it could not be realised. Then:

- Report totality (`C_src \ C_out = R`) stops being a test obligation and becomes an
  invariant — the report is a *query over the circuit*, so the two cannot disagree.
- The circuit refuses to simulate while any placeholder is live. That is a stronger and
  more useful gate than refusing to open the file, and it preserves the "no silent
  mis-map" discipline honestly.
- The migration stops being one-way (#311 OQ4) in the only sense that matters: a
  construct mapped later can be re-materialised from a file already in the tree, and
  the instructor's edits to the rest of the circuit are not lost.
- It rides seams the project is already building: the element registry (#78), stable
  ids (#165), and per-section versioning (#319) so a placeholder is skippable data.

This also gives #556 something much better to generalize than a report format: a
*disposition model* every importer shares.

## Where the decomposition pulls against the project

1. **TASK-0055 is not this feature's task.** #349 (FEAT-040), which is a hard
   `blocked_by` of this issue, lists TASK-0055 in its own `planned_tasks` and books it
   at 2 wk. #323 lists the identical task and books it at 2 wk as one of its two
   leading slices. So half of the "committed 4 wk slice" is work that must already be
   complete before this issue can start. Strike it from §2 and §6; this feature
   *consumes* the library, it does not build it.
2. **The dependency it calls "necessity, not convention" is probably false for the
   corpus.** §6: "The part data the imported designs reference has to exist as data
   before an importer can bind to it." Typical `.circ` course material is gates, pins,
   splitters, subcircuits — LE's `std/ttl/` is a minority library. Sequencing the
   reader behind the 74-series transcription puts the least-evidenced work on the
   critical path. #311 grades FEAT-040 *beneficial* for exactly this reason; #323's own
   §6 grades it a hard gate. Those two readings should be reconciled downward.
3. **The precedent is cited and repudiated in the same document.** §2 alternative 2
   rejects "emit save text and reparse it"; `NetlistImporter` — named as "the structural
   precedent to reuse" — does precisely that, returning `new ImportResult(text, …)`.
   Meanwhile #337 (the op layer that would make programmatic construction possible) is
   carried as merely beneficial. Pick one: either promote #337 to a hard gate, or reuse
   the shipped mechanism and delete the rejection.

## What I would fund instead, in order

1. The LE-headless corpus probe (days): construct distribution, HDL-exportability rate,
   and a first four-state divergence census over vector-bearing designs.
2. The `.circ` reader as **structure only** — gates, wires, pins, splitters,
   subcircuits — with quarantine placeholders instead of a drop-and-report contract, and
   the XXE hardening (I3/AC-5) exactly as written, which is the one part of this issue
   I would not change a word of.
3. The differential vector harness against LE's own CLI, feeding the value-domain
   decision rather than waiting on it.
4. Part-data binding, only if step 1 says the corpus needs it, and only as a consumer
   of #349.

I am explicitly setting aside this issue's §6 critical path (TASK-0055 → TASK-0054 →
residual) and its framing of the report as a drop-list, because both are downstream of
choices I think are wrong. The Definition of Done's substantive lines — construct map
as a document, per-vector hardening tests, committed accept/reject tables — survive the
reframing intact and should be kept verbatim.
