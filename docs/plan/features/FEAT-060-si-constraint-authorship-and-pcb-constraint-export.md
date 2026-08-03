# FEAT-060 - Signal-integrity constraint authorship and PCB constraint export

**Status:** proposed | **Cost:** 5.5-9.5 mw | **Owner program:** P3 |
**Spine rank:** -

## Capability delivered

Electrical intent stops living in a student's head. A net carries an authored
signal-integrity constraint set - a maximum length, a stub length, a skew
budget - as an optional versioned section in the saved file, and that constraint
set leaves JLS as a rule file the target board tool's own design-rule checker
enforces on a real board. The routed length comes back the other way, so the
number the lint judges is the number the board actually has. This is the only
claim in CAP-18 checkable outside JLS: acceptance is not "JLS emits a file" but
**"a tool JLS does not control fails a board it should fail and passes one it
should pass"**.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-18 | required | the fix leaves JLS as something a real tool enforces, which is the only claim in that capstone with an external adjudicator |
| CAP-05 | beneficial | the constraint file rides alongside the netlist that capstone already emits, and the routed-length back-annotation is what makes a board's real geometry visible in JLS |
| CAP-13 | beneficial | parity with the board tool gains a second axis - not only "the netlist imports" but "the constraints are honoured" |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-058 | The constraint set is authored against the same two declared attributes the lint reads, and adds no third. It is also the permanence ordering: the reversible rungs ship first |
| FEAT-004 | A constraint is attached to a net. A constraint on a net whose name does not survive save, load and export names nothing, and the DRC violation cannot reliably name the net back |
| FEAT-042 | The constraint file rides alongside the netlist and names the same nets. A constraint on a net the board tool never heard of is inert by construction |
| FEAT-013 | The constraint block must be an OPTIONAL per-section-versioned section. A **dropped constraint** is a silently unmanufactured requirement, which is the case the format's silent-ignore valve must not be allowed to reach (K18-5) |
| FEAT-014 | The back-annotated routed length is a second view's datum about a first view's net, and it needs the same addressing scheme every other view uses |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| - | No task id. The registry's task space is closed at TASK-0112 and this feature was minted with CAP-18 after it closed, following the recorded FEAT-054..FEAT-057 precedent. Minting its tasks is a maintainer decision | - |

## Acceptance criteria

1. A net may carry an authored constraint set - at minimum a maximum length and
   a stub length - stored as an OPTIONAL per-section-versioned section, absent
   by default, with every existing golden byte-identical when absent.
2. The emitted rule file is byte-identical to a golden, and **every emitted
   keyword is one the target tool's rule parser accepts**. Emitting a keyword
   the parser rejects is a failure of this criterion, not of the tool.
3. The constraint set round-trips through save and load with an **additive-only**
   diff after inserting one unrelated gate.
4. **The external adjudicator, with the failing direction asserted first.** A
   committed board fixture routed 25% over the declared maximum length **must
   fail** the external DRC, naming the net; the same board shortened **must
   pass**. Opt-in through the shipped tool-locator plus assumption idiom, with
   the container pinned by digest.
5. Routed length arrives back from the board and is addressable as a datum about
   the same net the constraint was authored on, so FEAT-058's verdict can be
   re-run against the real geometry rather than the declared one.
6. The impedance target is emitted as an **annotation plus a resolved track
   width on the netclass**, and the documentation says so. It is not claimed as
   a checkable impedance constraint, because no such constraint exists in the
   target tool's model - see Design notes.
7. A first-year drawing an adder never meets an SI-constraint dialog.
   Visibility is derived from context and asserted by a test.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 213 | Board export | informs - the `PcfEmitter`/`PinBindings` pair is the data-not-code precedent this emitter copies. Precedent, not scope: it is an FPGA pin-constraint emitter, and the PCB constraint writer does not exist |
| 93 | SDC constraints | overlaps - a constraint object model is the same shape as this one, and both want a time base. Whichever lands first should be the model the other adopts |
| 87 | Liberty cell library | informs - the same authored-electrical-intent axis, one tier down |
| - | signal-integrity constraint authorship and PCB constraint export | **no issue** |

## Design notes

**The impedance claim that must not be made, verified first-hand.** The target
tool's DRC rule parser accepts `length`, `net_chain_length`, `stub_length`,
`skew`, `return_path`, `diff_pair_gap` and `diff_pair_uncoupled`, and its
constraint enumeration carries `LENGTH_CONSTRAINT`, `SKEW_CONSTRAINT`,
`NET_CHAIN_STUB_LENGTH_CONSTRAINT`, `NET_CHAIN_RETURN_PATH_CONSTRAINT`,
`DIFF_PAIR_GAP_CONSTRAINT`, `MAX_UNCOUPLED_CONSTRAINT` and
`DIFF_PAIR_INTRA_SKEW_CONSTRAINT`. Its netclass record carries `m_TrackWidth`,
`m_diffPairWidth`, `m_diffPairGap`, `m_diffPairViaGap` and `m_tuningProfile` -
and **no impedance field anywhere**. Claiming JLS exports a controlled-impedance
*constraint* would therefore be a conformance claim no tool checks. Criterion 6
emits the annotation and the resolved width instead, and says which is which.

**A dropped constraint is worse than a dropped lint input, and the format's
default is to drop.** `docs/file-format.md:220` at `2d0ca9d` is normative -
"Unknown attribute names are silently ignored" - with the value "dropped without
error" at `:222`. FEAT-058's attributes can ride that valve because a missing
lint input is fail-open. A constraint cannot: a constraint that vanishes on a
save/load round trip is a requirement the board is then manufactured without.
That is the whole of why FEAT-013 is a prerequisite here and only a convenience
there, and it is what K18-5 stops.

**This rung is second, and it is the expensive one.** The permanence ordering is
lint -> constraint export -> element. This feature commits one optional
versioned section (reversible: the section can be dropped by a later reader
without breaking anything already saved) and an emitted file format that is
external and therefore not JLS's to freeze. It commits no element type, no
palette entry and no K9 obligation, which is why it precedes the cheaper
FEAT-059.

**The external adjudicator is the reason to build this and the reason it can
fail.** No other feature in CAP-18 has an acceptance test a tool outside JLS
renders. That is a stronger bar than any golden. It is also outside JLS's
control: if the external DRC cannot be made to honour an emitted rule file
against a digest-pinned container (K18-2), the tool-side check demotes to
advisory, the claim narrows in writing from "a constraint a real tool enforces"
to "a constraint file in a documented format", and **this feature re-costs
downward** - the back-annotation is then the only part still carrying value.
That is a planned degradation with a named trigger, not a risk to absorb
silently.

**It rides an acceptance harness that is already funded.** CAP-05's own
acceptance test already invokes the external DRC with
`--severity-error --exit-code-violations`. This feature adds a rule file to a
harness that exists, which is a large part of why the band is 5.5-9.5 mw and not
larger, and it makes CAP-05 worth more rather than later.

## Risks

- **Acceptance cannot be asserted from inside JLS.** Criterion 4 needs an
  external tool armed in CI. Unarmed, it is the weakest check in the capstone;
  armed and pinned by digest, it is the strongest.
- **K18-2 is a real, priced failure mode**, not a formality. The response is
  written above and must be executed as a REPLAN with the claim narrowed in
  writing, never by quietly weakening the test.
- **FEAT-014's shipped half is the wrong half.** Per-element permanent identity
  landed; stable **net** identity did not. A constraint is attached to a net, so
  anyone re-costing FEAT-014 downward on the strength of the shipped half will
  under-fund exactly the part this feature reads.
- **Two version mechanisms.** If FEAT-047's whole-file bump lands first and
  FEAT-013's per-section flags later, the constraint section must adopt one of
  them rather than leaving both in the tree.
- **The constraint vocabulary is a public surface with an external referent.**
  Emitting a keyword the target parser once accepted and later removed turns a
  passing golden into a failing DRC. The golden and the parser-acceptance check
  in criterion 2 are two assertions on purpose.

## Evidence

- **No PCB emitter and no constraint vocabulary exist at HEAD.** Verified at
  `2d0ca9d`: a listing of `src/` matches nothing for `kicad` or `geda`, there is
  no `jls.netlist` package, and the partition walk is still inside the HDL
  exporter.
- **The silent-drop valve this feature must not ride.** Verified at `2d0ca9d`:
  `docs/file-format.md:220-222`.
- **The data-not-code precedent that does ship.** At `2d0ca9d`,
  `src/jls/hdl/board/` holds `Board.java`, `Boards.java`, `PcfEmitter.java` and
  `PinBindings.java`, with a byte-deterministic golden under
  `test/resources/hdl/board/`. That is the shape to copy; it is an FPGA
  pin-constraint emitter and not this scope.
- **The external tool's constraint model, read first-hand**, including the
  absence of any impedance field on the netclass record - recorded in
  `docs/plan/capstones/CAP-18-net-that-stopped-being-a-wire.md`, open decision 4
  and the Evidence section, read in the working tree at `839fb3a`. The same
  material is in `docs/plan/evidence/highfreq-determination.md` in that tree.
  **Neither path resolves at `2d0ca9d`** - the determination landed in
  `3a81a4a` - which is stated here per D12 rather than left as a citation that
  does not resolve.
- **The harness this rides.** CAP-05's acceptance test already invokes the
  external DRC with `--severity-error --exit-code-violations`.
- Owner: **P3** (interchange), per CAP-18 §7.1.
- **Cost reconciliation.** Band **5.5-9.5 mw**, from CAP-18 §7.1's feature
  table. **This is the one of the three CAP-18 features whose two derivations
  agree**: `highfreq-determination.md` §6.3 prices stage H3 (constraint
  authorship + PCB constraint export) at 5.5-9.5 mw and §6.2's permanence table
  prices "constraint export" at 5.5-9.5 mw, both matching the feature table.
  No task rollup exists: this feature has no task ids.
