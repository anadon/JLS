# Issue #454: TASK-0068: a drawn circuit gets the three-address polled serial port a guest kernel actually drives — and deliberately no interrupt line
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of the 16550, the claim is: **a drawn circuit should be able to hand a byte
to a human and take one back.** That is the right claim. It is the missing half of
JLS's observability story — today the only observables are watched registers and VCD
dumps — and it is what #202, #214 (grading on output), TASK-0084 and every student
who wants a circuit that *prints* are actually waiting for.

The issue then answers that claim with a specific artifact: a Java class that
hardcodes the register map of an emulator's UART, frozen by test into the palette
and the save format forever.

## Where it aligns with the project's arc

- The host-door discipline is right and the issue respects it: sealed seam, grant at
  invocation, no `java.awt`/`javax.swing`/`jls.edit` in `jls.elem`, no `TellUser` on
  `react`. Deference to #424 is correct and #424 genuinely blocks.
- O5 is correct even against `master`: `ARCHITECTURE.md:115-119` still opens with
  "There is no element registry yet" while `src/jls/elem/ElementRegistry.java` ships
  35 types. Fixing that sentence in passing is free and right.
- The evidence discipline (measured decode, falsification criteria, a test for the
  *absence* of a pin) is the best of this project's issue culture.

## Where it pulls against the arc

**1. It compiles into Java the one device the roadmap wants drawn.**
`docs/capability-roadmap/sweep-05-system-and-interfaces.md:288-294` states the target
in the project's own words: a student draws "a CPU, a ROM, a RAM, a **UART** and a
timer, each a reusable component, each attached to one Wishbone bus with one wire
each, with an address decoder in between — and then writes a program that talks to
the UART through a memory-mapped register. That is the entire content of a
computer-organisation course's second half." Six lines later (`:299`) it names
address decoding as the part that "stays hand-drawn comparators (fine, that's the
lesson)." `riscv/README.md:9-15` makes the same point about the CPU: "not a special
JLS mode or a plugin: it is an ordinary circuit made of the elements JLS already
ships … which is exactly what JLS is *for*." #454's P5 takes the lesson — a 256-entry
address decode — and turns it into a Java `@ParameterizedTest`. The first UART a JLS
student meets would be the one thing on the bus they cannot open.

**2. The specification is aimed at a guest that cannot reach it yet.**
By the project's own rescued calibration (#494 §6.8): "The minimum SoC's UART is
three *byte* addresses on a 32-bit bus… **Without sub-word access there is no UART
driver and no Linux.**" The issue declares byte lanes explicitly not a prerequisite
because the element decodes its own address pins — true of the element in isolation,
false of H1, which is a claim about a `writeb` from a drawn machine that cannot
currently do a byte store. Add §8: structural nommu boot ~1.7 h (band 1.2–6 h),
`JLSInfo.defaultTimeLimit` 1,920–2,300× short, live console 1.5 s/char on an echo
path never measured within 10×. So the exactness that justifies every hard-frozen
detail — the `0x100` window, `0x60 | data_ready`, the missing `irq` — buys nothing
testable for years, while the general capability buys #202 and #214 immediately.

**3. It pays a per-device tax with two more devices already known and unfiled.**
The same §5.3 that supplies the UART decode also specifies a CLINT (`mtime`,
`mtimecmp`, and an interrupt line that "cannot be avoided") and a syscon. Under
#454's framing each is another Java element at the measured ~65 lines of registration
across sixteen places plus icon, dialog, renderer, help topic and goldens. Three
devices, three rituals, three frozen save tags — and #78's registry, which was
supposed to collapse this, is used here as an excuse for the tax rather than as
pressure on it. Note also that P6's rhetoric overreaches: `irq = 0` removes the
*PLIC*, but the minimum SoC still carries a mandatory CLINT timer interrupt, so "the
absence of an interrupt output is what keeps the SoC minimal" is true of one device,
not of the machine.

## Reframing A (primary): cut at the byte seam, not the device seam

Ship `jls.elem.BytePort` instead of `jls.elem.Console`: the irreducible host
endpoint, shaped exactly like #424's door. Pins: `data`(8) in, `send` (edge), `data`
(8, tri-state) out, `take` (edge), `avail`(1). **No address, no offsets, no window,
no LSR, no `pollPeriod`.** Then ship the 16550 register map as a *drawn subcircuit*
— `examples/uart16550.jls`, buildable by `riscv/jlsbuild.py` the same way the CPU is
— from `Decoder`, `Splitter`, `TriState`, `Register`, all of which already exist and
already have goldens.

What this buys:
- The decode becomes editable, inspectable and gradeable. H1's refutation path ("the
  driver needs a different offset") becomes an edit to a circuit, not a Java change
  plus a new golden plus a frozen tag.
- One registration ritual serves the UART, the syscon (a drawn comparator plus this
  port), a keyboard, a block device and a framebuffer — the last two of which #324
  already promises as permits on the same seam.
- The element stays honest about what it is: JLS's first host endpoint, not JLS's
  opinion about what a serial port is.
- It ships **before** #424's downstream work needs a machine, and serves #202, #214
  and students at any bus width, including the current word-granular one.

The cost is that a drawn decoder is ~10–20 more elements on the bus path; against a
~580-element machine at 318 ns/event that is noise, and it is exactly the cost
sweep-05 already priced as the lesson.

Note this is *not* the "make `Console` a `SubCircuit`" alternative #324 §2 rejected
(that was about inheriting FEAT-031's fidelity toggle). This splits the device into a
primitive plus a drawn map; the primitive is still an element.

## Reframing B (fallback): if it must be Java, build the general device

If the maintainer wants the register map in code, build the element the roadmap
already specifies — `docs/capability-roadmap/sweep-03-elements-and-hdl.md:451-470`,
C8: an addressable register block with declared offsets, per-register fields, and
access policies (`rw`/`ro`/`wo`/`w1c`/`rclr`). `Console` becomes an *instance*: two
registers whose data ports bind to the host port. The same element then is the CLINT,
the syscon, and every future peripheral, and it unlocks #38 SystemRDL and #4 IP-XACT
memory maps as a side effect. One ritual, one tag, one help topic, one dialog — and
the 254 dead offsets are a property of a declaration rather than 254 test cases.

## Simplification C: delete the receive poll and `pollPeriod` entirely

This one makes a whole section of the issue disappear. #424 drains the ring at
`Simulator.beforeEvent`, on the simulation thread, before every event. A polled
16550 is read by the guest — the guest's own LSR read *is* the react. There is
nothing for a self-scheduled event to do except re-post itself forever: a 10⁸-tick
run with `pollPeriod` 100 posts 10⁶ events that observe nothing. Removing it kills
H3, O4, Stage 4, P4's ordering hazard, a saved attribute, a `FORMAT`-visible tuning
knob that changes every receive golden, and the entire argument about whether a
self-posted poll may masquerade as `PinChanged`.

Keep a wake-up only if a level `avail` output pin is wanted (it is, under Reframing
A). Then the right mechanism is the drain — already on the sim thread — waking the
element once on the ring's empty→nonempty transition, not a free-running timer.

## A design smell the acceptance criteria would freeze

P3/Stage 3 define exactly-once transmit as "emit iff the pin vector differs from the
previous react." That is not a bus protocol, it is a level comparison, and it drops
a legitimate repeated byte: same address, same data, `WE` held across two bus cycles
produces no pin change, so `"aa"` prints `"a"`. A rising-edge strobe — which JLS
already has everywhere via `Register`'s clock handling — is unambiguous and is what
the hardware does. Under Reframing A this is a drawn edge-triggered register and the
problem cannot exist. Under the issue as written it becomes a golden-blessed rule.

Relatedly, O3 lets the absence of a `default` arm across 27 `react` implementations
dictate the event vocabulary. Overloading `PinChanged` to mean "a timer fired" makes
the event log lie, which matters precisely where this feature is heading:
TASK-0069's stamped transcripts, #425's determinism assertion, and lf-03's causal
debug all read that log. Fix the switch or accept the payload; do not encode a lie to
avoid touching 27 files.

## What I am disregarding, and why

I am setting aside **P5** (254 offsets asserted in Java), **P6 as framed** (the
absence of `irq` as a machine-architecture guarantee), **P7's `Console` golden**,
**H3/O4/Stage 4 and `pollPeriod`**, and the §7.4 pin list. They are all internally
consistent and all downstream of the device-in-Java choice, which is the choice I am
contesting. What I am not setting aside: the sealed one-door grant, the headless
ratchet, the `NullPort` no-op, the `TellUser`/`System.out` prohibition on `react`,
the append-only pin ordering, the zero-`FORMAT`-bump argument, and the
`ARCHITECTURE.md` correction. Those are load-bearing under every framing above.

## Verdict

**endorse-with-reframing.** The capability is right, the timing behind #424 is right,
and the security shape is right. The artifact is wrong for JLS: it puts the one
peripheral the project's own roadmap wants students to draw behind a Java black box,
freezes an emulator's register map into the save format on behalf of a guest that
cannot reach it until byte lanes land, and pays a per-device registration tax with
two more devices from the same measured device tree already waiting and unfiled.
Ship `BytePort` plus a drawn 16550 (Reframing A), or the C8 register block
(Reframing B); delete the receive poll either way (Simplification C).
