# Issue #452: TASK-0062: a higher-radix design exports, dumps a VCD a third-party viewer can interpret, and is testable with -t vectors — while a binary circuit's bytes stay identical on all three paths
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the apparatus and the claim is: *JLS has exactly three doors out — HDL
export, VCD, `-t` — and each one hardcodes "binary" in a different place, so a
non-binary design is drawable but not shippable.* That claim is true and it is
the right thing for FEAT-029 (#361) to own. I endorse the goal.

But the issue answers it with **five independent mechanisms** — policy rows, BET
lowering tables, a `$comment` manifest, three regex branches, a glyph formatter —
each cut at the boundary where the problem was noticed rather than at the seam
where it lives. That is why the task carries five Open Questions, a "must be
sequenced against #376" threat, and a hard prerequisite (TASK-0061) that is not
filed. Below: one reframing, three seam corrections, and an alternative goal that
lands most of the value before either prerequisite exists.

## The reframing: a radix codec is the missing peer to the kernel

FEAT-029 already posits **TASK-0060, the kernel** — one table, one place,
"because the event-driven engine and any later levelized pass must read the same
table or they will disagree six months apart." That argument is exactly correct
and it applies verbatim to *representation at boundaries*, which this task
touches at five sites and unifies at none:

| Site | What it needs to know | What #452 gives it |
|---|---|---|
| `SigSim` pre-pass | how to read a literal in radix r | a hand-written `matches` branch per radix |
| `BitSetUtils` render | how to write a digit in radix r | a new hand-written balanced formatter |
| `BatchSimulator` `$var` | `b(s) = d(s)·⌈log₂ r⌉` | arithmetic re-derived in §7.10 stage 3 |
| `vcdValue` HiZ marker | the binary width | a guard against being handed the wrong one |
| HDL header comment | the encoding ε | prose, written by hand, in the emitter |

Every row is the same question — *how does radix r cross this boundary* — and the
answers are five artifacts that can drift from each other. The elegant shape is a
`Radix` codec beside the kernel owning exactly four things: parse a literal,
render a digit, the binary width function, and the encoding ε as **data**. Then
the pre-pass branch, the formatter, the `$var` width, `vcdValue`'s guard and the
emitted header declaration are all *the same table read five times*, and the
header comment is generated from ε rather than asserted about it — which is what
#361's §7 re-planning protocol demands ("a header that names an encoding the
emitter no longer uses is worse than none") and which no test in §9 can currently
enforce.

The tell that the codec is missing is already in the issue: it adds a **`0q`
base-4 token for which FEAT-029 ships no element** (its Open Question 1
recommends "2 and 3 only in the shipped elements"). Without a table, generality
is done by copy-paste, so one extra copy got pasted. With a table, radices 4 and
5 are rows nobody writes code for.

## Seam correction 1: lower in `HdlModel`, not in the exporter's rendering path

§7.5 puts the BET lowering tables "`private static final`, inside the exporter's
rendering path." That is the wrong side of a seam this repo already defends.
There are **two** emitters — `src/jls/hdl/VerilogEmitter.java` (752 lines) and
`src/jls/hdl/VhdlEmitter.java` (1149 lines) — sharing one emitter-neutral
`src/jls/hdl/HdlModel.java` (1005 lines), and
`test/jls/hdl/VhdlEmitterPolicyTest.java:129`
(`verilogAndVhdlShareOneModelWalk`) exists specifically to pin that they do.

BET lowering is a *model* transformation: a width-n trit net becomes a width-2n
bit net, and each N-ary operator becomes a binary statement over it. Done in
`HdlModel`, it costs both emitters zero changes, VHDL comes along free, the
encoding becomes a model attribute each emitter renders in its own comment
syntax, and #63's black-box path and #61's Yosys path inherit it. Done as private
statics in `HdlExporter`, VHDL silently gets nothing — which the issue half-
admits, since §7.6 promises "Lowered Verilog/VHDL" while §8 and P4 build only a
Verilog golden and extend only `HdlPolicyTest`.

The same lowering is where the VCD's `b(s) = d(s)·⌈log₂ r⌉` should come from.
One lowering, three consumers, instead of one lowering plus one re-derivation
plus one guard against the re-derivation being wrong (P10).

## Seam correction 2: the `-t` pre-pass should be deleted, not extended

H3 says radix tokens are "three more branches in the existing `matches(...)`
chain — no new parser." Syntactically true; semantically false. The pre-pass
(`src/jls/elem/SigSim.java:43-75`) rewrites a token to **decimal text** and
re-scans the whole file; the real parse at `:100-130` then does
`input.nextBigInteger()`, checks `value.bitLength() <= bits`, and converts
negatives by `value + 2^bits`. That pipeline is two's-complement binary all the
way down. A balanced-ternary literal has no two's complement and its width is in
*trits*, so `0t+0-` would arrive as a signed `BigInteger` at a width check
measured in bits — the same class of silent mis-sizing the issue correctly fears
at O6, arriving through the door it just opened. The `ToStringSigned` hazard the
issue names at O4 reading 3 is not a rendering accident; it is this pipeline's
assumption showing up on the output side.

The radically simpler route: **delete the pre-pass.** Parse each value token at
the point of use, where the pin is already resolved and its radix and width are
known — `input.next()` plus a codec lookup, instead of `hasNextBigInteger()`.
That single change:

- makes radix literals typed rather than laundered through base 10;
- **removes the quadratic string concatenation outright**, so it subsumes #376
  (TASK-0009) rather than needing to be sequenced against it — Open Question 5
  disappears;
- fixes the documented wart in `docs/batch-interface.md` §2.1, that the rewrite
  runs *before* comment stripping so "a malformed hex-like token inside a
  comment is still rewritten";
- makes the width check the codec's, in digits, which is what a ternary pin
  actually needs.

I am explicitly disregarding H3 and its falsification criterion here. "Do not
extend the concatenation" (§11) is the right instinct pointed at the wrong
target: the concatenation is not the defect, the rewrite-to-decimal *stage* is.

## Seam correction 3: the manifest does not deliver the issue's own headline

The title promises a VCD "a third-party viewer can interpret." A second
`$comment` line does not do that. GTKWave and Surfer parse `$comment` as opaque
free text; neither has any notion of a JLS radix manifest, and neither will grow
one. A student opening the dump sees `b1001` and reads it as binary 9. The
manifest serves a hypothetical *future JLS-aware* parser, and P6 asserts only
that the line exists — the capability statement is oversold in exactly the way
#361 §1 warns against for the lowering.

The out-of-the-box alternative: emit a **companion `$var string`** signal for
each non-binary signal, carrying `-0+` glyphs as its value changes, alongside the
existing binary `$var wire`. `string` is a Verilator-originated de-facto
extension rather than IEEE 1364-2001 (say so in the docs), but GTKWave and Surfer
both display it, which means the viewer literally shows `+0-` with no manifest
protocol at all. Properties this buys:

- **Byte-identity for binary circuits becomes structural, not conditional.** No
  non-binary signal ⇒ no extra `$var` ⇒ no extra bytes. H2's "the single easiest
  way to fail this task" (an accidentally unconditional line) stops being a
  failure mode, because there is no header line to condition.
- The standard-conformant binary vector stays exactly where it is, so P5 and the
  goldens are untouched and the machine-checkable path is unchanged.
- Open Question 2 ("what exactly does a manifest entry contain, and in what
  order?" — recommended answer: "zip the two lists") evaporates, along with its
  fragility.

Keep a manifest too if you like; it is then cheap and non-load-bearing rather
than the whole interop story.

## The alternative goal: make radix-generality pay rent in the binary world first

This task sits two levels of not-yet-real deep. TASK-0061 is unfiled; #344
(FEAT-028), FEAT-029's absolute prerequisite, is unlanded; and per the
evidence-pin comment and #492, the `REJECTED` bucket that H1 rests on **does not
exist on master** — `src/jls/hdl/HdlExporter.java:420-437` has three buckets and
an unnamed fall-through. FEAT-029 itself concedes ~2 release cycles of calendar
for the family, one serving capstone, and (§11) that ternary hardware never won.
Against ARCHITECTURE.md's recorded pattern of *declining* speculative generality
— i18n as a non-goal, the plugin loader removed as unreachable, one simulation
strategy with an explicit revisit trigger — a five-mechanism interop task for
elements that do not exist pulls against the arc.

It does not have to. There is a binary-world gap sitting in plain sight:
**`-t` accepts hex but not binary.** `-?0[xX][0-9a-fA-F]+` is the entire literal
vocabulary of the test-vector grammar of a *logic* simulator. `Display` offers
radix 2/10/16; a student can read a value in binary and cannot write one. So:

> Land the codec now, in the binary world. `0b`/`0o` literals in `-t`; the
> width/sign check moved onto the codec; the pre-pass deleted (subsuming #376);
> a per-signal display-format hint in the VCD via the `$var string` companion.
> Every existing user gains something the week it ships. Ternary then arrives as
> **rows in a table**, not as a program.

That reordering costs FEAT-029 nothing — it needs the codec regardless — and it
converts this task from "blocked on two unfiled/unlanded prerequisites" into
"deliverable today, and the reason TASK-0061 is cheap when it lands." It also
gives the honest answer to §11's framing obligation: the work justifies itself by
what it does for binary users, so the ternary case never has to carry it.

## What I would keep unchanged

The fail-closed exact-class policy and the instruction not to "improve" it into
an `isAssignableFrom` walk; the refusal to re-baseline a VCD golden; the
insistence that the encoding be *declared* rather than implied; and the O4 probe
as a model of evidence. Those are right, and they are the parts of this issue
worth copying into whatever replaces its middle.

## Concrete asks

1. Add `blocked_by: 492` — H1's fourth bucket is #492's deliverable, not present.
2. Move the BET lowering into `HdlModel`; assert VHDL and Verilog both carry it
   (extend `verilogAndVhdlShareOneModelWalk`, not just `HdlPolicyTest`).
3. Replace the `$comment` manifest with a `$var string` companion; drop P6's
   manifest-shape assertion for a "the viewer shows `+0-`" one.
4. Split out the codec + pre-pass deletion + `0b` literals as a task that can
   land before TASK-0061 and #344, and close #376 with it.
5. Drop `0q` until an element consumes it.
