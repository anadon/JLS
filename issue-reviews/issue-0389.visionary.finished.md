# Issue #389: TASK-0065: a subcircuit instance names which implementation runs, the choice survives a save, and no file can change fidelity silently
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

The claim underneath #389 is FEAT-031's (#325) claim, and it is a good one: **fidelity
should be a property of a boundary, not of a program.** If "the fast thing agrees with
the drawn thing" is anchored to one `SubCircuit` instance, it becomes a diffable test
instead of an assertion; a 580-element machine comes up block by block; and CAP-02/CAP-03
get a shared golden instead of two artifacts nobody can compare. That claim is aligned
with JLS's trajectory — it is the same instinct as `RegisterFile`'s "collapse ~95 elements
into one first-class element" and `Adder`'s lumped `30 * bits`, made testable instead of
asserted. I endorse it without reservation.

#389 is not that claim. #389 is the *persistence and CLI surface* of that claim, filed
first, and it ships with the one thing that would give the surface meaning explicitly
forbidden: "`SubCircuitImpl`'s permits list contains exactly `StructuralImpl` at close;
no second implementation rode along."

## The load-bearing observation: at close, almost everything this task ships is unreachable

Compose two of the issue's own rules. §7.11: "An `impl` id outside the closed set: **refuse
by name** at load." §14: the closed set at close is `{structural}`. Therefore no load can
ever produce an in-memory circuit whose `impl` is non-structural, and therefore:

- `req(f)` of §7.10 is identically 2. **The writer can never emit `FORMAT 3`.**
- $\mathcal{N}$ is identically empty. **The manifest of §7.6 can never print.** P10 is
  vacuously true, and Open Question 1 (stdout vs stderr) blocks execution on a decision
  about output that cannot occur.
- `-fidelity` accepts exactly one value, which is also the default. `-fidelity structural`
  — named in §Intended Audience as "the single most important affordance here" — is a
  no-op flag.
- The `-s` `FIDELITY` directive can name exactly one value, which is also the default.
- The four-level precedence resolver of §7.10 resolves four levels to the same constant.
  P6's "four-way table with the winner recorded at each removal" has one value in every
  cell.
- P5 exercises `Circuit.readFormatHeader` refusing a newer version — which the issue
  itself says is "already implemented at `src/jls/Circuit.java:776-779`; this task only
  makes files that trigger it," except that it does not make such files; the fixture must
  be hand-written, and the assertion is the one `FormatHeaderTest` already makes.

Net observable delivery: a sealed interface with one implementation (a behaviour-preserving
refactor), plus two attributes that can only hold their defaults, plus five mechanisms with
no reachable path. The most exacting completion criteria in §14 — six exact-save-bytes
suites unmodified, all-structural stdout byte-identical — are satisfied by *changing
nothing observable*, which is precisely what the task guarantees. This is a lot of
apparatus bought against a future that a later task is free to shape differently, and JLS
has a recorded institutional preference against exactly that: the plugin loader was
*removed* in 5.0.0 because it was unreachable in every build (#80); out-of-process
isolation is "reserved for a future untrusted-provider case; it is not built speculatively"
(#222); #221 declines a second execution strategy and says the follow-up issue
"deliberately does not exist yet."

## The reframing: cut the seam where the value is, defer the file question until it has content

**Do now — the part that is real today.** Extract today's subcircuit boundary
(`SubCircuit.react` `:621-636` inbound, `send` `:646-652` outbound) behind
`jls.sim.SubCircuitImpl`/`StructuralImpl`, with the `Boundary` value type its signature
needs. Zero format change, zero CLI change, zero manifest, zero goldens touched, and a
mechanical proof of no behavioural change. That is a week of honest, immediately useful
work: it is the seam every later arm needs, and it is falsifiable on its own terms.

**Defer — the file and CLI surface.** They should land in the same task as the first real
second implementation, when there is something for `impl` to *name*, a real refusal set to
test, a manifest with a line in it, and a `-fidelity` flag with two values. At that point
the version question is answered with the semantics in hand instead of in advance.

**Why this does not break FEAT-031's sequencing.** #325 §6 says "TASK-0065 first, by
necessity. There is nothing to switch between until the selection type **and the saved
attribute** exist." The first conjunct is true; the second is not. TASK-0066's null-toggle
gate — switch a boundary to the same implementation at instant $t_n$, observe the identity
on all later observations — is expressible entirely against the in-memory seam. Persistence
is not a prerequisite for any harness; the feature's own sequencing argument conflates the
selection type with its serialization, and #389 inherits the conflation as its whole scope.

**A dependency the "no prerequisite, and that is verified" claim misses.** §7.4 fixes the
signature as `initSim(Boundary, Simulator)` and `react(long, Simulator, SimEvent.Payload,
Boundary)`. `Boundary` is TASK-0066's deliverable per #325 §2/§3 ("TASK-0066 … provides
`Boundary` plus the harness"), it is not in §6 Materials, and it does not exist at
`2d0ca9d`. #389 either defines a type it says it does not, or freezes a public signature
over a type another task owns, while declaring `blocked_by: []`. Under the reframing this
resolves cleanly: the seam task owns `Boundary`, because the seam is what needs it.

## Second reframing: the version question has an owner, and #389 is not it

The conditional gate is not the novelty the issue presents it as — §4:191-195 already
requires "the header with the highest version whose features the file uses," and `FORMAT 2`
is already emitted only for files containing a vertical `Binder`/`Splitter`. So H2 is
conformance, not invention, and the blast-radius drama of §11 is smaller than stated. The
real problem is the opposite of the one the issue guards against.

`docs/file-format.md` §9 records **the same question, already open, for two shipped
attributes**: `Memory.initrle` (silently drops initial contents in JLS 4.1) and
`Memory.sync` (#199) — "whether files containing it should declare a bumped `FORMAT`
version is an open question tracked with issue #199's follow-ups." #325's own Open
Question 2 says to "declare it in whatever epoch the section-versioning feature opens, and
say so in TASK-0065 **rather than assuming a version bump protects it**." #389 does the
opposite: it settles the policy unilaterally, for one not-yet-meaningful attribute, and
spends the global `FORMAT` scalar on it — leaving JLS with `sync` (behavioural, ungated,
shipped) and `impl` (behavioural, gated, inert). A reader implementing from the spec now
sees two rules.

The better task, and it is genuinely a *better goal*: **settle the behavioural-attribute
versioning policy once**, as a `docs/file-format.md` §9 rule that covers `sync`, `initrle`
and any future `impl` — "an attribute whose absence changes simulation behaviour must gate
a version, conditionally emitted." That closes an existing open question, fixes two real
shipped hazards for graders *today*, and makes `impl`'s treatment a consequence rather than
a precedent. It also exposes the deeper structural issue the section-versioning feature is
circling: a single monotone scalar is a global lock on a format growing in independent
directions, so `FORMAT 3` will mean "may contain a non-structural subcircuit" and nothing
else, and the next independent feature must jump to 4 for reasons unrelated to fidelity.
If a capability line (`REQUIRES <feature-id>` records, refused by name when unknown) is
where that feature is heading, #389 should not pre-empt it with a scalar bump for a
mechanism that cannot yet be exercised.

**Out-of-the-box alternative worth pricing before the scalar is spent.** §9 already
documents a channel that is loud in every existing reader and needs *no* version bump:
"adding a new element type — older readers fail loudly with 'no element type named X',
which is detectable, not a misparse." A bound instance written under a second `SaveTags`
tag (structural instances keep `ELEMENT SubCircuit`, byte-identically) gets refusal-by-name
from every reader ever shipped, including pre-versioning ones the `FORMAT` header cannot
reach, and gets the closed-set check for free from the frozen tag table. It costs a
`SaveTags` row and a FileFormatSpecTest row — not a new class, since tags name semantics
and map to compile-time class references. It is a worse *diagnostic* than `NEWER_FORMAT`
("needs a newer JLS" beats "no element type named X"), which may well decide it against.
But the issue never considers it, and it is the only free loud channel the format has.

## Third: the catalog row is a category error, and the flag points the wrong way

**`docs/extension-points.md` is the wrong home.** That document is "the catalog of those
seams: the concrete API surface of the module program," ids prefixed by home area
(`elem.`, `hdl.`, `collab.`, `gui.`, `app.`), and "pending seams are named here first" means
*will become a seam*. §7.4 says `SubCircuitImpl` "is core-internal and **not** an extension
point," and `grand-architecture.md` §6 plus #221 put the simulation inner loop inside core
with zero plugin indirection — it will *never* become a seam. A pending row saying
otherwise is worse than no row: it tells the next contributor this is somewhere to plug in.
The right home for "this is deliberately not a seam, and here is why" is ARCHITECTURE.md's
Recorded Decisions, beside #221 and #222 — where readers already look for exactly this
shape of statement. Drop P9.

**`-fidelity <id>` manufactures the hazard it claims to prevent.** Precedence puts the flag
above the file attribute, so `-fidelity behavioural` on a grader's command line silently
overrides every instance's saved structural choice — a fidelity change with no file
evidence, aimed at the exact audience §Intended Audience names first. The manifest is the
mitigation, and it is conditional output on the stdout stream graders parse. What a grader
actually needs is not a selector but an **assertion**: `-require-fidelity structural`, which
exits non-zero and names the offending instance if any binding is non-structural. That is
FEAT-031's criterion 7 ("an instructor can restrict the permitted set so a lab must be
drawn") and I6, it can never change a run, it is meaningful the day it lands, and it
subsumes the "run the reference" affordance because structural is already the default.
Ship that instead of `-fidelity <id>` and Open Question 4 dissolves.

**And put the manifest on stderr.** §3 of `docs/batch-interface.md` — not §3.2, which is
the element whitelist; the issue miscites it five times — says "batch mode prints exactly
two things to stdout." Provenance of a run is not a result; a conditional third result line
is a worse trade than a diagnostic line, and #325's banner on the outcome line already
carries the visibility obligation. Prefer H4's fallback as the default, not the fallback.

## Alignment with #221, stated plainly

The issue's escape hatch — "a second *implementation of a subcircuit* is a model change,
not a second execution strategy" — is sound for a **lumped behavioural** binding: one
element, one `react()`, one delay, indistinguishable in kind from `Memory`'s access time or
`RegisterFile`'s collapse of ~95 elements. It is not sound for a levelized/compiled pass,
which is verbatim what #221 declined and gated behind a named revisit trigger. And the
issue's own O1 fixture writes `String impl "levelized"` — a fixture becomes a de-facto name.
Whatever shape this work finally takes, it should (a) use a behavioural id in its fixtures,
not `levelized`, and (b) record in the accessor javadoc that admitting a levelized id
requires #221's revisit trigger to fire first. Otherwise the per-element attribute is the
route by which a recorded architectural decision gets amended without being reopened.

## What I am disregarding, and why

I am disregarding §8's steps for the attribute parsing/writing, the `FORMAT 3` gate and
`Circuit.FORMAT_VERSION` raise, the `-fidelity` `FlagSpec` row, the `-s FIDELITY` directive,
the precedence resolver, the manifest, and the `docs/extension-points.md` pending row —
along with P2, P4, P5, P6, P7, P9, P10 and their completion criteria. Not because they are
wrong in themselves, but because every one of them is inert until a second implementation
exists, and the two that are not inert (the scalar version bump and the catalog row) commit
the project to answers that belong to other owners: the section-versioning epoch and #199's
open question for the first, ARCHITECTURE.md's decision log for the second.

Keep: the sealed `SubCircuitImpl`/`StructuralImpl` extraction, `Boundary` alongside it, the
javadoc note that `impl` stays on the reference site under #357, and the discipline that
refusals are by name. Those are the parts that make the *boundary* real, and the boundary
is what the whole feature is for.

## Minor, concrete

- `SubCircuit.save` writes `String orient` *before* `super.save()`, so it already deviates
  from §5's canonical "base attributes, then the type's own" order; whichever attributes
  eventually land beside it inherit that deviation. Worth a one-line note in the spec.
- §7.11 says the parameter-file path refuses "following the parameter-file path's existing
  diagnostics." Those diagnostics (`JLSStart.java:2691-2726`) print to **stdout** and call
  `System.exit(1)` — the known deviation from the `jls: error:`-on-stderr contract in
  ARCHITECTURE.md's CLI section. Following them propagates the defect; if the directive
  ever lands it should use `usageError`.
- Open Question 2 (the `implDelay` default as "the drawn boundary's computed critical
  path") is a substantial new computation on the drawn contents, unpriced anywhere in the
  1.5-week estimate, and meaningless while the only implementation is the drawn one.
- `docs/parity-contract.md`, which #325 §3 makes normative for the observation function,
  does not exist in this checkout — nor does `docs/plan/`. The seam can be cut without it;
  any *binding* cannot.
