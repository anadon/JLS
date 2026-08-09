# Issue #888: CAP-39: a mixed-alphabet drawing — balanced-ternary datapath, byte-symbol bus, binary control — simulates, probes, autogrades and refuses honestly in one circuit, while every binary circuit stays byte-identical
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

#888 is not a pull from the trajectory; it is a **disposition for orphaned work**. It
says so in its own first paragraph: CAP-03 (#295) was closed *not planned*, #344's §7
re-planning protocol then required that #344/#361 "need a new beneficiary or a
disposition," and this capstone supplies one. But #344 §7 asks for something sharper
than a beneficiary — it asks to "re-derive whether it survives on its own terms rather
than to keep it by inertia." #888 skips that re-derivation. It substitutes a *fixture*
for a *user*: the outcome is one committed mixed-alphabet circuit that runs against a
golden and produces two refusals.

AC-7 states the arithmetic plainly and I take it at face value: for the entire existing
user base, "the experience is *nothing happened*." So the capstone's whole value accrues
to people drawing balanced ternary, a population the program has never evidenced — #344
records it: "No open issue covers the N-ary programme… the absence of any prior request
is the premise of the question." Meanwhile the cost lands where KC-39-1 says it lands:
"the value domain is the hottest code in the program."

Set that against what the project's own ranking says. `docs/capability-roadmap/README.md`
§3 names the keystone as "a signal has a width, an immutable identity, and **plane-encoded
multi-value state**; a driver has strength; a net has a resolution function," with reach
of "sixteen survey entries directly blocked, twenty-four counting dependents." Every one
of those is unlocked by `{0,1,X,Z,U}` + strength (keystone-a §3, candidate **A4**) — none
by radix N. The roadmap's "multi-value alphabet is nearly free" line (README:724) means
*four-state*, not *arbitrary-alphabet*. #888 borrows the keystone's rhetoric for a
different axis.

**I am explicitly disregarding AC-1, AC-5 and AC-9** — the mixed fixture, the dual-tier
differential oracle, and the declared `[0,1]`→`[-1,+1]` crossing — because the goal they
serve is the wrong goal, and because two of them are self-inflicted. Reasons below.

## Reframing 1 — the `[0,255]` bus is width wearing an alphabet costume, and it is the leg that costs everything

A `[0,255]` "byte-as-symbol" wire crossing through a bridge into binary is an 8-bit bus
crossing through `Splitter`/`Binder`. Both classes exist (`src/jls/elem/Splitter.java`,
`Binder.java`); the widths already fold correctly (`WireNet.java` `recheck`, the
`Math.max` idiom #344 §3 quotes); the mis-connection is already refused at all four
editor sites, verbatim `overlapMessage = "Bits don't match"`
(`src/jls/edit/SimpleEditor.java:4015, 4142, 4247, 4358`). Nothing in the walkthrough's
`[0,255]` leg is a capability JLS lacks. It is a *renaming* of one JLS has.

And it is expensive precisely in proportion to how little it adds. #344 §3's plane
arithmetic — `P(r) = ceil(log2(r+3))`, cliff at r = 6 — is what makes radix 2 "cost
literally nothing," because 3, 4 and 5 ride the three planes #322's record already
allocates. #888's open question 1 discards that bound (`endpoints are int, N ≤ 2^31, no
kernel cliff`), and the price is stated in KC-39-2: "two operator implementations that
must agree forever." That permanent obligation exists **only to carry `[0,255]`**.

**Concrete change, worth taking even if everything else here is rejected: delete the
`[0,255]` leg and the `int` endpoint policy; cap the domain at the plane cliff.** Then
AC-5 evaporates, KC-39-2 evaporates, the generic tier is never written, the bridge
element in #361 shrinks to the `Splitter`/`Binder` it already is, and open questions 1,
3 and 7's second batch all resolve to "no." One deletion removes the largest new
permanent maintenance surface in the issue.

## Reframing 2 — balanced ternary is drawable today, without touching the kernel

The out-of-the-box route the issue never considers: **encode the digit and draw the
encoding.** A balanced-ternary digit is a 2-bit bundle; the min/max/complement/T-gate
family is a set of `SubCircuit`s over ordinary gates or `TruthTable`s (both exist;
`TruthTable` already stores don't-care as `2`, keystone-a §3 A2). Then, checked against
the eight walkthrough steps:

- Step 1–3 (load, batch, golden): work today, unchanged. `SubCircuit` round-trips, batch
  runs, goldens hold.
- Step 5 (refuse ternary-to-binary): **already refused today** — a 2-bit digit into a
  1-bit control is `Bits don't match` at all four sites, with both widths nameable.
- Step 6 (single-driver): already the multi-driver rule.
- Step 8 (`lint radix` reports zero implicit crossings): becomes "no `Splitter` on a
  digit bundle," a lint over existing structure.
- Step 4 (X, not silent zero) is **#322's deliverable, not this capstone's** — it is the
  27–29 `null → new BitSet()` coercion sites keystone-b enumerates, and it is owed to
  binary users first.
- Step 3's `-`/`0`/`+` rendering is the one genuine gap, and it is a *display* concern.
  `Display` already carries a radix field (`src/jls/elem/Display.java:31`); adding a
  balanced rendering mode plus a probe/trace formatter is weeks, in GUI-side code that
  KC-39-1 cannot be violated from.

What is lost: the digit is two wires, not one; X is per-bit rather than per-digit; the
encoding is visible. For a **pedagogy tool** the last of those is a feature, not a
defect — and #361 §3 already concedes the point at the other end: export is "a
*lowering*, not a ternary netlist… an external tool consuming that netlist is simulating
an **encoding**, not ternary." If the exported artifact is an encoding and the
autograded artifact is an encoding, insisting the *kernel* not be an encoding buys
purity, not capability. A ternary-encoding element library plus a balanced renderer is
the same student experience at roughly a hundredth of the risk, and it can ship before
#322 lands.

## Reframing 3 — the durable seam is "a port declares its domain," and its first inhabitant should be four-state

The best thing in this issue is not in the body; it is the maintainer's comment. "A net
carries values and width and **no alphabet**; a `Put` carries the domain, for one
direction only; validation is port-to-port." That is the same instinct as #453/#419's
types-not-attributes argument and the same shape as `docs/extension-points.md`'s typed
seams — and AC-10 ("no `WireNet` carries a domain field at any commit, asserted
structurally") is the single best-designed criterion in the issue. Keep both verbatim
under any framing.

But the seam is being introduced with the *wrong first inhabitant*. Domains on ports is
exactly the mechanism that four-state-plus-strength needs: keystone-a §2's axis 2 —
"strength is a property of a **driver** and an observable of a net; it is not a property
of a signal" — is the identical claim, one axis over, and its §5.2 puts `strength0`/
`strength1` on `Output`, i.e. on a `Put`, for one direction only. Land the domain seam
with `Binary4State` and driver strength as its only inhabitants, and you get: EVCD's
named revisit trigger, I²C's wired-AND, open-drain, honest bus conflict, `x` in VCD,
faithful Yosys import, cocotb interop — the 16–24 entries. Radix-N then arrives later as
one more inhabitant of a seam that already earned itself, on a code path already proven,
with the plane cliff intact.

## The demo I would ship instead

Replace the mixed-alphabet fixture with the **coercion fixture**, and keep every other
discipline:

1. An unconnected input into an `AndGate`. Today `computeOutput` reads it as 0 and the
   circuit lies. After: the output is X, in the status line, in `-t` stdout, and as `x`
   in the VCD.
2. Two drivers fighting on a bus. Today: silently resolved. After: X, at the conflicting
   bits only, per-bit rather than whole-signal.
3. A `Register` clocked by a floating clock. After: X, not a phantom edge.
4. The full pre-existing golden corpus, byte-identical, warm loop within noise of a
   named baseline — **AC-4 and KC-39-1 carried over unchanged**, because they are right.

That demo has a constituency of every JLS user and every autograder, it exercises the
same port-domain seam, it discharges `docs/simulation-semantics.md` §2's rewrite for a
reason the document's own appendix already flags, and — the governance point — it makes
the #221 reopening *worth its week*. Reopening recorded decision #221's equivalence
criterion at bus factor 1 to re-anchor "the two-states-plus-HiZ value domain" is a real,
irreversible governance cost (#344 prices it: "the week is the #221 reopening… not the
code"). Spending it to widen the normative semantics to arbitrary alphabets, for one
drawn fixture, is the wrong purchase. Spending it once to re-anchor §2 to five states
plus strength is the purchase the roadmap already justifies with 24 dependents.

## What survives regardless

- **KC-39-1, absolute.** No golden byte, no warm-loop regression, revert not renegotiate.
- **AC-10**, structural, and the port-not-net model that produced it.
- **AC-8's structural form** — a non-binary value cannot *reach* a pre-existing gate
  because it does not typecheck, not because a hot-path branch stops it.
- **#889's "never guess a crossing"**, which is the anti-`coercedX` discipline
  (`src/jls/hdl/imp/ImportSummary.java:28`) and is correct at any N — including N = 2,
  where the crossings that matter are `Z`→`0` and `X`→`0`.
- Open question 8 (inout ports) dissolves under this framing: if domains never leave
  binary, there is no bidirectional non-binary case to answer.

The one-line version: **#888 is the right seam cut at the wrong altitude for the wrong
first user.** Keep the port-domain model, the refusal discipline and the byte-identity
kill criterion; move the capstone's outcome from "a mixed-alphabet drawing simulates" to
"no value is ever silently coerced, and a binary user can see the difference"; and let
ternary arrive as a drawn encoding library now and a kernel domain later, if anyone asks.
