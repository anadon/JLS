# Issue #609: TASK-C487-1: a net carries an authored SI constraint set that cannot silently vanish — an optional versioned section, additive-only under an unrelated edit
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's being proposed

TASK-C487-1 is the "vocabulary and section" scope of FEAT-060 (#487): a net gains an
authored, optional, per-section-versioned constraint set (v1: maximum length, stub
length) that must survive save/load/edit round-trips losslessly, riding the section
mechanism FEAT-013 (#319) is supposed to introduce, and authored only over the two
attributes FEAT-058 (#486) is supposed to add to `WireNet` (declared length, driver
edge rate).

Verified against HEAD: neither prerequisite exists yet.
`grep -rliE 'must.understand|SectionFrame|sectionVersion' src/ test/` returns nothing,
`WireNet.java:22-30` still has only `ends`, `wires`, `bits`, `hasinput`, `triState` (no
length field), and `Circuit.FORMAT_VERSION` (`src/jls/Circuit.java:102`) is still the
single whole-file integer `docs/file-format.md` §4-9 describes. The issue's own
`docs/file-format.md:220-222` citation is accurate — that is indeed where "unknown
attribute names are silently ignored" lives today.

## Findings, most severe first

**1. Internal contradiction: "optional" vs. "must-understand" is not a resolvable pair under the mechanism it claims to consume.**
AC2: *"The set is carried as an optional per-section-versioned section with a
must-understand flag, so a reader that does not understand it refuses by name rather
than dropping it silently."* But FEAT-013 (#319), which this task explicitly "consumes
rather than growing a second one," defines exactly two mutually exclusive states for
$m_i$: `optional` → unknown section is **skipped and preserved**, informational
diagnostic only; `required` → unknown section is **refused by name**. There is no
third state that is simultaneously "optional" and "refuses by name." AC2 asks for
behavior that only the `required` flag produces, while calling the section "optional."
This is the same ambiguity FEAT-060 (#487) itself only half-resolves — its Open
Question 3 says the section "adopts FEAT-013's per-section flag" but never states
which value — and #609 inherits the ambiguity instead of closing it. As written, an
implementer can satisfy the literal words of AC2 ("optional... section") by setting
$m=$`optional`, which then makes AC2's second half ("refuses by name") false, or set
$m=$`required` and make AC1's "absent by default, absent means today's behaviour
exactly" awkward to phrase (a `required` section that is merely *absent* from a file
is fine per the formula in #319, but a reviewer reading only #609 has no way to know
that's the resolution intended). **Recommendation:** rewrite AC2 to state the flag
value explicitly — "the section's must-understand flag is `required`" — and drop the
word "optional" from the section's *behavior* description, reserving it for "present
only when authored."

**2. "Stub length" is not shown to be expressible over FEAT-058's two attributes, contradicting AC5's own claim.**
AC5: *"No third attribute is introduced: the set is expressible over FEAT-058's
declared length and edge rate, checked against #486's interface section and
recorded."* FEAT-058 (#486) declares exactly two attributes: a per-net **declared
physical length**, and a per-arc **transition time** (edge rate), the latter
explicitly scoped to live "on the arc" (Open Question 3 of #486), not on the net.
Nothing in #486 defines a "stub" — a branch or sub-segment of a net distinct from its
total length — so "stub length" (one of exactly two v1 constraint kinds this task must
ship per AC1) has no attribute in #486 to be "expressible over." Mapping "maximum
length" onto #486's declared length is plausible; mapping "stub length" onto either of
#486's two attributes is not shown and does not appear to be possible without
inventing net-topology structure (which sub-segment is "the stub") that #486 never
defines. AC5 asserts a property that the v1 vocabulary in AC1 appears to falsify.
**Recommendation:** either drop "stub length" from v1 pending a real definition of
what a stub is in JLS's net model, or add the missing sub-segment addressing as an
explicit, reviewed dependency instead of asserting by fiat that no third concept is
needed.

**3. No authoring surface is specified or required by any acceptance criterion — "authored" is never actually reachable by a user.**
All five ACs are about persistence mechanics (default-absence, section framing,
round-trip losslessness, vocabulary closure, attribute closure). None requires that a
human user, CLI flag, or scripting surface can actually set the constraint values.
JLS's net objects (`WireNet`) are not `Element`s and carry no creation dialog in
`SimpleEditor` (see `ARCHITECTURE.md`'s "Adding an element today" list, which is
element-specific and has no analogue for nets); ARCHITECTURE.md documents no existing
mechanism for attaching arbitrary authored metadata to a net. As written, a developer
can satisfy every AC with an internal setter exercised only by round-trip unit tests
and never build a way for a real user to author the constraint — which technically
discharges "an authored constraint set... absent by default" while leaving the stated
outcome (a net "carries an authored... constraint set") permanently false in the
shipped product. This is exactly the "acceptance criteria pass, real goal fails"
pattern the adversarial lens is asked to hunt for. Note the sibling issue #487
explicitly wants "no SI-constraint dialog anywhere" reachable from the default
experience (K9), so *some* deliberately narrow surface is intended — but #609 never
names it (dialog on a wire/wire-end? a properties panel? a CLI-only path?), so "done"
is unfalsifiable on this axis. **Recommendation:** add an AC naming the concrete
authoring entry point (even if it's minimal/hidden), or explicitly defer it to a named
follow-up task and say so.

**4. The task is not actionable yet, and the issue doesn't say so.**
`ordering_after: [486, 319]` acknowledges the dependency, but the body's present-tense
framing — "The section mechanism is FEAT-013's (#319) and this task consumes it" — and
the `band_mw: "1.5-2.5"` estimate read as though this is buildable work, not work
blocked on two large, unstarted features (#319 has three unfiled planned tasks and a
5.5-week band by itself; #486 has three unfiled scopes and a disputed 2.5-7 week band
across its own three cost derivations). If either #319 or #486 lands with a shape
different from what's assumed here (#319's Open Question 1 — text-grammar frame vs.
multi-member container — is explicitly unresolved; #486's Open Question 1 on where
length lives is "recommended default," not decided), #609's AC2 and AC5 need to be
rewritten, and nothing in #609 says what triggers that re-check. **Recommendation:**
either hold this issue until #319 and #486 both close, or add an explicit "re-verify
AC2/AC5 against the landed shape of #319/#486, not the proposed shape" gate.

**5. AC5's verification step ("recorded") names no artifact.**
"checked against #486's interface section and recorded" doesn't say where the record
lives — a code comment, a test, a closing issue comment, a doc. Every other AC in this
issue is pinned to a test ("a test," "every existing golden," "the round trip"); AC5's
verification is the one criterion an implementer could discharge with an offhand PR
description and nothing durable. **Recommendation:** name the artifact (e.g., a
comment block in the constraint class citing #486's attribute names, checked by a
cross-reference test analogous to how #486 itself pins its own criterion 3).

## What's solid

- AC1 (absent-by-default, byte-identical goldens) and AC3 (author/save/load/insert/save
  additive-only diff, pinned by a test) are concrete, testable, and correctly framed —
  no notes.
- The core rationale — citing `docs/file-format.md:220-222`'s silent-drop valve to
  justify why a *constraint* (unlike a *lint input*) cannot ride an ordinary attribute
  — is accurate against the current spec text and is a genuinely sound distinction,
  not hand-waved.
- Scoping this task to storage/round-trip only, and leaving the KiCad rule-file
  emission and external-parser acceptance to a separate scope, is a reasonable cut
  (mirrors #487's own task decomposition).
