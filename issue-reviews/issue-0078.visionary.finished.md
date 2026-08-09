# Issue #78: Element authoring contract, final stage (H2): make the Element/LogicElement runtime-throw stubs compile-time obligations
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The stated deliverable is four deletions. The real end, stated in §13, is
"no obligation is discovered at runtime" — and behind that, the arc the
whole #78 program serves: an element is a *described* thing, so that the
registry can be the seed of a module system (#220/#223/#212), so that a
headless author or HDL importer (#61/#62, the RV32I comment of 2026-07-21)
can build a circuit from metadata instead of archaeology. Judged against
that arc, the goal is right and three of the four sub-decisions the issue
leaves open are being framed the wrong way.

I verified the census the issue's §8 asks for, at working-tree HEAD
(`5311625`; the three stub greps of P1 still match, so the task is live).
It differs from §2 in ways that change the design:

- `Element.init` — **zero** concrete reliers. Every concrete leaf overrides
  it (`Wire.java:53`, `Text.java:59`, every `LogicElement` leaf; `SigSim`
  has none but both its leaves `SigGen`/`TestGen` do). Abstract-ifying is
  free.
- `LogicElement.initSim` — **zero** concrete reliers. The print at
  `LogicElement.java:469` is dead code. P4 is not a risk check, it is a
  formality.
- `LogicElement.react` — **three** reliers, not one: `SigGen`, `TestGen`,
  and `WireEnd`. §2 names only `WireEnd` because `Group` and `Pin` (the
  other classes with no `react`) are abstract, and `SigSim`'s leaves were
  not walked. Three is not an edge case — it is a category.
- `Element.copy()` — reliers are `Wire` and `TestGen`. Note
  `jls/Util.java:65` does `Objects.requireNonNull(el.copy())` on any
  non-wire element, so the `TestGen` null is a latent NPE the placeholder
  is currently hiding.

## Reframe 1: don't force `react` bodies — make the throw unreachable by type

`SimEvent` already holds `private final Reacts callBack`
(`src/jls/sim/SimEvent.java:100`) and `Simulator.java:239` dispatches
`event.getCallBack().react(...)`. The type that an event can address is
*already* `Reacts`. The only reason a non-reactor can be addressed is that
`LogicElement implements Reacts` for all 35 types, and one cast launders it:

    src/jls/elem/WireNet.java:507
        sim.post(new SimEvent(now, (Reacts) element, new SimEvent.PinChanged()));

That cast is the bug. Every other `new SimEvent(...)` site passes `this` or
a `Pin`. So the elegant route is not "give `WireEnd` an honest no-op"
(Open Question 1a) nor "restructure so `WireEnd` doesn't implement `Reacts`"
(1b, scoped to one class) — it is to split the interface along the roles the
census just revealed:

- `Simulated { void initSim(Simulator) }` — implemented by all 35; the
  `Simulator.java:198` seed loop becomes `instanceof Simulated`.
- `Reacts { void react(...) }` — implemented by the 32 that actually
  consume events. `SigGen`/`TestGen` are pure event *sources*; `WireEnd`
  is wiring plumbing. They simply don't implement it.

Then `sim.post(new SimEvent(t, this, ...))` inside a source is a compile
error, `WireNet`'s cast becomes `if (element instanceof Reacts r)`, and the
`"no react"` throw has no expressible caller. That is strictly stronger than
abstract-ification: abstract methods guarantee a body exists, not that the
body is honest — three `throw new AssertionError("never dispatched")` bodies
would satisfy §5's P1/P2 while preserving exactly the failure mode the issue
exists to remove. §10's fallback ("the fallback is interface segregation")
should be promoted to the primary design.

## Reframe 2: `copy()` should become `final` and generic, not abstract

H2b poses a binary — abstract it, or keep the documented null — and both
arms are wrong, because **`save` already made this journey**. §1 records
that `Element.save` stopped being a stub not by becoming abstract but by
becoming a real generic implementation over `savedAttributes()`
(`Element.java:659-663`, the #23/#52 lineage). `copy()` is the same shape
and should take the same road.

The evidence that it works is already in the tree: `Gate.copy()` is generic
today —

    src/jls/elem/Gate.java (copy)
        Gate it = getClass().getConstructor(Circuit.class).newInstance(circuit);
        copy(it); return it;

— covering 8 of 35 types, *by reflection*, which is precisely what the
registry was built to abolish (`ElementRegistry`'s own javadoc; the #167
collab work "eliminating reflection from `jls.collab` entirely"). Promote
that body to `Element`, swap the reflection for
`ElementRegistry.forClass(getClass()).create(circuit)` (a small addition —
the registry has `forTag`/`all()` only), and 26 of the 28 `copy()`
overrides — all of which are `new X(circuit); super.copy(it); return it`
with at most a put-copy line — delete themselves. `SubCircuit` (53 lines),
`StateMachine` (31), `TruthTable` (27), `Memory` (20) keep a hook, or
better, express their extra state as `Attribute`s and follow `save`'s path
the rest of the way.

Payoff: the nullable placeholder disappears, the last reflection site in
`jls.elem` disappears, `Util.java:65`'s `requireNonNull` becomes honest,
and ARCHITECTURE's authoring touchpoint #7 (`copy`) is deleted rather than
made compulsory. Contrast the issue's route, which would make every future
element author *write* a `copy()` — adding a mandatory touchpoint to the
program whose thesis is that touchpoints should vanish. An even more
radical variant worth one experiment: define copy as save-text →
load, since `CircuitSnapshot` already treats save/load as the canonical
clone for undo; that would make `AllElementsRoundTripTest#copyPreserves
EverySavedAttribute` true by construction. (Both variants must mint a fresh
`ElementId` per `StableElementIdTest#copyMintsAFreshId`.)

**I am explicitly disregarding H2b's stated falsification criterion.** §10
says "if the editor demonstrably requires a nullable base `copy()`, H2b is
refuted and the placeholder stands." That test can only ever be run against
the current per-element design. The right question is whether `copy()`
should be a per-element method at all, and the answer the codebase gives is
no.

## Reframe 3: obligations belong on interfaces, not on the sealed base

§7.4 offers "abstract methods or interface obligations" as interchangeable.
They are not, for this project's trajectory. `Element` is
`sealed permits DisplayElement, LogicElement, Wire`, and `LogicElement`'s
permits clause is a closed in-tree list. #212 — scoped as ServiceLoader
discovery of external `ElementType` descriptors "atop the #78 registry",
and named on this very issue as load-bearing on it — requires that an
out-of-tree provider be able to *state* what it implements. Under a sealed
base, no external class can ever be an `Element`; every obligation parked
as an `abstract` method on `Element`/`LogicElement` is an obligation that
has no expressible form across the future module boundary. Every obligation
expressed as an interface (`Simulated`, `Reacts`, and the landed
`Rotatable`/`Editable`/`Timed`/`Watchable`) is one that survives it. The
capability-interface stage (#238) already chose interfaces over base
predicates for exactly this reason; H2 should not quietly choose the other
way for the four obligations that matter most.

## What actually finishes the program (and is not in §8)

ARCHITECTURE.md §"Adding an element today (the honest list)" still opens
with *"There is no element registry yet — issue #78 will introduce one"*
and lists sixteen touchpoints, several now false: item 12 (a palette entry
in `SimpleEditor.makeElements`) became a `Palette` row in #246, item 15's
hand-maintained `HelpTopicsTest` list is now derived, and the registry it
says does not exist has been shipped since 2026-07-18. The program's stated
payoff is "~16 → ≤6 touchpoints"; that payoff is delivered to a contributor
only through the document they read. Rewriting that section — six honest
steps, each naming the build gate that catches its omission — is worth more
to the issue's declared audience than any of the four deletions, costs an
hour, and is currently owned by nobody. It should be a §8 checkbox here,
not deferred to the M4 line.

## Verdict

**endorse-with-reframing.** Do the work; it is small, the goal is right, and
the trajectory needs it before #212. But: (a) split `Reacts` into
`Simulated` + `Reacts` and let `SigGen`/`TestGen`/`WireEnd` fall out by
type, rather than abstract-ifying `react` and writing three dishonest
bodies; (b) make `copy()` final and registry-generic, deleting ~26
overrides, rather than abstract or placeholder; (c) prefer interfaces to
abstract members on the sealed base wherever both work; (d) correct §2's
relier census before the inventory step trusts it; (e) update
ARCHITECTURE.md's authoring list in the same PR. `init` and `initSim` have
zero reliers and can be deleted on sight.
