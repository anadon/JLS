# Issue #444: TASK-0033: a saved file stops being one indivisible unit — sections carry their own version and a must-understand flag, and the epoch policy is written down
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Not "per-section version integers". The end is that **JLS stops paying a
whole-file epoch every time somebody needs to put something new next to a
circuit**. Seven features and six capstones queue behind that (#319's `blocks`:
#318, #332, #340, #356, #357, #363, #364). The arc is larger than this issue
admits: the roadmap already has **four independent claimants on "FORMAT 3"**
(`keystone-b-migration.md:234` four-state values, `lf-01-parameterization.md:358`
parameters, `AMENDMENT.md:434` diff-stable serialization, `README.md:378` reuse
identity). The prize is not one more epoch spent well — it is **retiring the
epoch as a unit of planning.** Judged against that end the filed design does not
arrive, and the cause is a single foreclosed decision.

## The decision this task forecloses

#319 Open Question 1 — *"frame inside the text grammar, or a multi-member
container?"* — is marked **"Must be answered in TASK-0033, not discovered"** and
**"Blocks filing children."** This is TASK-0033. It does not answer it. Every
line of §7 (a section tokenizer, a skip-to-end-of-section scanner, section
keywords rejected inside element bodies, per-kind byte bounds, nesting refusal,
duplicate-kind policy) silently assumes option (a). The six Open Questions at the
bottom are all consequences of that assumption; the assumption itself is not
among them.

Option (b) is better, and the evidence is in the tree.

**The container already does what the frame is being built to do.**
`FileAbstractor.readZip` (`src/jls/FileAbstractor.java:298-302`) fetches the
entry named `JLSCircuit`, falls back to `JLSCheckpoint`, and **ignores every
other member**. Skip-an-unknown-optional-section-by-name is not a feature to
design; it is behaviour JLS has shipped since 4.1, and it arrives with per-member
lengths, per-member digests (#319's I4 wants SHA-256; the central directory
already carries CRC and both sizes), unique names (this issue's Open Question 1
dissolves) and no nesting (Open Question 2 dissolves).

**The text-frame route converges on being an archive, badly.** #319's Open
Question 3 concedes it: whole-file compression means one edit invalidates every
following byte, defeating independence criterion I1 — so the text frame must drop
compression or compress per section. Per-section framing + compression + lengths
+ digests *is* an archive, reimplemented inside a `java.util.Scanner` token
grammar whose one line-sensitive rule (§2) is already why a blob cannot be a
string (O7).

**And the container route costs no epoch at all.** `Circuit.stateHash()` is the
SHA-256 of the canonical save *text* (`src/jls/Circuit.java:1548-1569`). Members
outside that text change no text, so: no golden regenerates, no stateHash moves,
`FORMAT_VERSION` stays 2, no file needs rewriting, and `-migrate` — §7.7's
"substance of this task" — **has nothing to do**. The largest stated scheduling
risk ("two mandatory bumps, one epoch") and the blocking edge on #436 both
evaporate, because container framing does not touch the text #436 rewrites. Note
the irony in the filed design: a mechanism whose purpose is graceful degradation,
delivered by a change that makes every new file unopenable by every JLS ever
shipped (O5/§7.12 shrug this off as "designed") — where the container route
degrades gracefully back to 4.1.

The honest cost: the default container becomes an archive for files carrying
non-mergeable members, and the **plain-text container stays the canonical
single-section form** — exactly what #334/lf-06 want git to track. That line is
the one this issue itself draws in §7.6 (structural sections merge; blobs and
checkpoints are hashed, never merged). What never merges does not belong in the
diffable text.

## The property the task dropped, which is the one worth an epoch

#319 says **skip *and preserve*** — criterion 2, criterion 6, invariant 3, and
integration criterion I2, which adds *"write this test first — it is the guard
against a preserving-but-reordering reader"*, plus the formal
`save ∘ open (Sᵢ) = Sᵢ`. #444's disposition function (§7.10 Stage 2) has three
outcomes — read, refuse, skip+diagnose — and **the word "preserve" does not
appear in the issue.** §7.11 says a skipped section "loads successfully" with a
diagnostic; nothing requires its bytes to survive the next save.

That is the whole point, not a detail. PNG ancillary chunks are valuable because
editors copy them through. A reader that skips-and-drops silently destroys a
third-party tool's data on the student's next Ctrl-S — #47's silent-drop caveat
reproduced at section granularity, which is the defect the programme exists to
end. Preserve is also *hard* in the text grammar (raw bytes stashed and re-emitted
in position, fighting #166 canonical order and #171's convergence oracle) and
*trivial* in an archive (copy the unread member). The seam choice and the missing
property are one finding.

## Why the stated acceptance test proves nothing

§7.6 ships exactly one section: `CIRCUIT` (required). A file with one section has
a per-section version that is, byte for byte and semantically, the file version.
So P9 — "`Memory.sync` is refused rather than silently mis-loaded" — is satisfied
by bumping the `CIRCUIT` section's version, which is `formatVersionNeeded()`
under a new name. Open Question 4 says so out loud: *"Under sections this becomes
'the CIRCUIT section's version'."* The issue nominates as "the honest test of the
machinery" a test the machinery cannot fail *and cannot pass on its merits*.

I am explicitly disregarding P9 as an acceptance criterion. The `Memory.sync`
and `initrle` question is real, is worth closing, and needs none of this: it is
one override of `Element.saveFormatVersion()` (default `1` at
`src/jls/elem/Element.java:819`; `Group.java:459` is the worked precedent) on the
element that writes `int sync 1` (`src/jls/elem/Memory.java:445-448`), plus a
sentence in §9. That is a day, it is independently shippable today, and
keystone-b already schedules it (`keystone-b-migration.md:542`). Filing it here
makes a two-week frame the prerequisite for a one-day policy answer.

The honest acceptance test of a section frame is #319's I2 and criterion 6: a
file carrying a member this build has never heard of survives open → save
byte-identically. That test is writable against a second, real tenant — which is
why "do not fold TASK-0034 in" (§12) is backwards. **A frame with one tenant is
untested**, as §11 concedes; the fix is a second tenant, not a paragraph promising
to design against three imaginary ones.

## One observation that is a bug, not a format problem

O4-D — trailing content silently discarded — is not evidence about the format.
`docs/file-format.md:144-147` already says trailing content MUST be rejected, and
every production entry point does: `JLSStart` repeats the check at each
`FileAbstractor.openCircuit` call site (`:208`, `:321`, `:588`, `:2296`, `:2523`,
`:2909`). The probe reached the silent path by calling `Circuit.load` directly.
The defect is layering — the invariant lives in six CLI copies instead of once in
the loader, which is why `CircuitSnapshot` (`src/jls/edit/CircuitSnapshot.java:90`,
the undo path) lacks it. Move the check into `Circuit.load`: one small commit, no
epoch, and a third of the motivating story is gone. Do that first, so the frame is
argued on the two dispositions that actually remain.

## What I would build instead

1. **Answer #319 OQ1 for the container** (option b), with the plain-text
   container as the canonical single-section form. Sections are members: a small
   manifest member carries per-member kind, version, must-understand, identity-hash
   participation, and merge participation; unknown members are skipped, preserved
   verbatim, and diagnosed. No text-grammar change, no `FORMAT` bump, no goldens,
   no `-migrate`, no new hostile token parser, and #319's I1 (byte independence)
   and I5 (hash excludes non-participating members) hold by construction.
2. **Land it with its first real tenant** (TASK-0034's image, or #426's
   checkpoint) so preserve-round-trip is tested against something that exists.
3. **Re-home the rest.** `-migrate` belongs to #436, which is the change that
   actually rewrites files. Merge participation belongs with #415's table — the
   roadmap's C4 says that table is one object built once, and #444's own H4
   refutation condition (a kind whose merge behaviour depends on the other side)
   is the case #415 will meet on day one; declaring a format field for a consumer
   that does not exist is how formats acquire dead fields. `Memory.sync` ships on
   its own.
4. **If option (a) survives review anyway**, then ship only the *skip rule* in
   the epoch — the lexical form of an ancillary block, the refusal of a critical
   one, preserve-on-save, and a diagnostic. A reader only needs to know how to
   skip *before* the data exists; registry, per-kind bounds, dispatch and tooling
   can all arrive later without a second epoch. That is a ~150-line change plus
   one test file riding along with #436, not a two-week task blocked behind it.

## Verdict

**rethink.** The capability is right, load-bearing, and I would fund it tomorrow.
The task as written skips its parent's blocking design decision, omits the
parent's load-bearing property (preserve), nominates an acceptance test its own
Open Question 4 shows to be vacuous, and buys — with a mandatory epoch and a
rewrite of every user file — a behaviour the zip container has given away free
since 4.1. Answer #319's Open Question 1 on the record first; most of this
issue's scope, risk and cost is downstream of answering it wrong by default.
