# FEAT-027 - Strength lattice, driver kinds and net kinds

**Status:** proposed | **Cost:** 6-9 mw | **Owner program:** P1 |
**Spine rank:** -

## Capability delivered

A net can be told what kind of net it is and a driver can be told how hard it
drives, so the arrangements that dominate real boards - open-drain with a
pull-up, a wired-AND bus, a floating TTL input that reads high, a resistor
divider on a control line - are things a student can draw and see behave the way
the bench behaves. Contention stops being a warning about a value JLS picked
anyway and becomes a resolved result whose strength ordering explains itself.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-04 | required | floating-HIGH TTL, open-drain, pull-ups and contention are the pedagogical payload of the breadboard |
| CAP-05 | required | open-drain and pull-ups are ordinary board practice; a check that calls them unconnected is wrong |
| CAP-12 | required | the comparator output driving into digital logic needs an honest drive model, not an assumed ideal one |
| CAP-13 | required | a netlist that cannot express a pull-up is not a board netlist |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-026 | Strength is a second axis on the value the fold resolves. Over a two-state value with a first-driver-wins rule there is nothing for a lattice to order; the fold is where strength arbitrates |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0057 | The resolution fold | Shared with FEAT-026: the fold is where strength arbitration lives |
| TASK-0058 | Strength lattice and pull elements | The lattice, driver kinds, net kinds, and the drawable pull-up and pull-down |
| TASK-0049 | Bidirectional ports end to end | Shared with FEAT-021: a bidirectional pin reads back meaningfully only once drivers have kinds |
| TASK-0093 | Breadboard consistency check and physical binding | Shared with FEAT-043 and FEAT-041: the first consumer that exercises net kinds on a placed physical arrangement, including contention the schematic hides |

## Acceptance criteria

1. A drive strength ordering is specified in the normative semantics document
   and implemented, and the resolution fold consumes it: a stronger driver wins
   over a weaker one regardless of net order.
2. `PullUp` and `PullDown` are drawable elements. A net with only a pull-up
   attached reads 1; adding an active low driver makes it read 0; removing that
   driver returns it to 1.
3. Open-drain is expressible as a driver kind and a wired-AND bus of N
   open-drain drivers plus one pull-up resolves correctly for all 2^N driver
   combinations at small N (exhaustive) and by sampling above.
4. Contention between two drivers *of equal strength* with different values
   resolves to X (FEAT-026's criterion 4); contention between unequal strengths
   resolves to the stronger value and is **not** reported as a conflict.
5. Net kind is a saved, versioned property, and a reader that does not
   understand a net kind refuses the file rather than silently treating it as an
   ordinary net.
6. Every existing golden is either byte-identical or named with the semantic
   change that justifies it - a circuit with no pulls and no open-drain drivers
   must be untouched.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the strength lattice, driver kinds, net kinds and the pull elements | **no issue** |

Two *recorded decisions* (not open issues - do not cite them as open) name this
feature as their own re-open condition. The EVCD rejection in
`docs/standards-adoption/07-waveform-formats.md:150-151` states its revisit
trigger verbatim as "JLS gains a drive-strength value domain **or** a
bidirectional pin element", and the roadmap README identifies this feature and
FEAT-021 as exactly those two triggers (`docs/capability-roadmap/README.md:53`).

## Design notes

The lattice is where the *cost* is, not the elements. `PullUp` and `PullDown`
are small elements by the project's own per-element rate; what costs 6-9 weeks
is that strength enters the resolution path, which is the same two methods
FEAT-026 is already rewriting (`WireNet.makeNet`/`recheck`/`propagate`). Land
this immediately after TASK-0057 or pay for the same rewrite twice.

Tri-state-ness is currently a **static property of the drawing**, propagated at
edit time through `TriProp` implementors and settled before any simulation runs
(`docs/simulation-semantics.md:409-415`). Net kind should follow that precedent
rather than inventing a simulation-time mechanism: it is an edit-time property,
checked at edit time, saved with the net.

The roadmap prices this at 6-9 wk as "strength + driver kinds + drawable pulls"
inside P1's 28-36 total (`docs/capability-roadmap/README.md:217-220`); the
registry carries it as a separate feature because its consumers (CAP-04, CAP-05,
CAP-12, CAP-13) are entirely different from FEAT-026's and a maintainer may
reasonably fund one without the other.

## Risks

- **Two arbitration rules is worse than one.** If strength arbitration is
  implemented anywhere other than inside the fold, JLS acquires a second
  resolution path that can disagree with the first. The acceptance criteria
  deliberately test strength through the same fixture shape as FEAT-026's
  permutation property test.
- **Pedagogy.** A floating TTL input that reads high is a real bench behavior
  and a real exam question; a floating CMOS input that reads X is a different
  real behavior. Modeling one and not the other teaches a falsehood. Decide
  which technologies the driver-kind vocabulary covers before shipping it, and
  say so in the element help.
- **Scope creep into analog.** A pull-up is a resistor. Nothing here may
  introduce a resistance value that the discrete-event engine then has to
  pretend to solve; real resistive behavior is FEAT-046's, and the boundary
  needs to be stated in the element documentation so nobody expects a divider
  ratio.

## Evidence

- Tri-state as an edit-time static property, and resolution as first-active-
  driver-in-net-order with no conflict state: `docs/simulation-semantics.md:409-447`;
  `src/jls/elem/WireNet.java:443-490`.
- Cost band: `docs/capability-roadmap/README.md:217-220` (strength + driver
  kinds + drawable pulls, 6-9 wk, inside P1).
- The recorded EVCD revisit trigger this feature satisfies:
  `docs/capability-roadmap/README.md:53`, quoting
  `docs/standards-adoption/07-waveform-formats.md:150-151`.
- Board practice as the consumer: `cap-c4-breadboard.md` and `cap-c5-pcb.md`
  (the breadboard and PCB determinations) treat open-drain and pull-ups as
  ordinary content, not as an advanced feature.
- **Cost reconciliation.** Band 6-9 mw. Tasks named for it: TASK-0049,
  TASK-0057, TASK-0058, TASK-0093, totalling 8 wk. Band and task sum agree; no
  reconciliation is needed. Shared tasks counted once at the task level:
  TASK-0049, TASK-0057, TASK-0093.
