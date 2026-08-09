# Issue #462: TASK-0096: a drawn circuit makes a sound and hears one — through a door granted at invocation, with every test deterministic and no sound card
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

This is a large, unusually well-specified task issue (12 sections, 13
predictions, 5 hypotheses, a full data contract) proposing
`HostAudioSink`/`HostAudioSource`, a WAV codec, and a tick-lattice
resampler as JLS's first read-side host door. Most of its concrete
numeric claims against the codebase check out. The real problems are
(1) an unenforced coordination race with a sibling issue making the
same "first host door" claim, (2) load-bearing evidence documents that
do not exist in the checked-out tree, and (3) a governance-gate test
that the issue itself admits doesn't prove what it's being used to
gate.

## Findings

**1. (High) Unenforced race with #424 over "the first host door" and the SECURITY.md text it produces.**
Issue text: *"This is the first read-side host door in JLS's history,
and that is the point of doing it now."* Issue #424 (TASK-0067, also
open, also `blocked_by: []`/`blocks: []`) opens with the identical
claim: *"JLS has never had a read-side host door... This task adds the
door."* #462 lists #424 only as `related`, calling it "methodologically
adjacent" and asserting "**neither blocks the other, and they should
agree on the grant model**" — but names no mechanism for that
agreement. Both issues independently spec a `SECURITY.md` paragraph
stating "a door granted at invocation." Verified: neither `-serial`
nor `-wav`/`-audio-in` exist yet (`grep -n '\-serial\|HostBytePort'
src/jls/JLSStart.java` and the FLAGS table both empty of both), so this
is a live race, not settled history. The actual tie-break protocol
("#324 lands first with a different grant model → this feature
conforms; the reverse also holds") exists only in parent issue #346's
Re-planning Protocol — #462 never cites it. Two contributors picking up
#462 and #424 in parallel will each edit `SECURITY.md` under the "first
door" framing and collide.
*Recommendation:* add `related` cross-reference to #346 §7's
reconciliation clause directly in #462, or make one of the two tasks
`blocked_by` the other until a single shared grant-model doc exists.

**2. (High) Load-bearing citations are not present in the repository.**
The task's entire grant model rests on `docs/plan/evidence/BRIEF.md`
§12 D7, quoted at length and pinned to landing commit
`3a81a4a7d6a0f108ec201e632732d308cc02b3fc`; the throughput claim (H5,
"~209,000 samples/s, 4.7x margin") is sourced to
`11-analog-determination.md` §5 stage S0; the one-resampler rule and
WAV-writer estimate cite `spice-jls-integration.md` §6.5. None of these
three files exist anywhere in the working tree (`find /home/user/JLS
-iname '*brief*' -o -iname '*analog-determination*' -o -iname
'*spice-jls*'` returns nothing), and `3a81a4a7d6a0f108ec201e632732d308cc02b3fc`
is not a reachable git object in this checkout. Unlike
`docs/vcd-interop.md` or `docs/file-format.md` (both real, both
verified below), these are cited as ordinary "Reference, do not
restate" material but cannot actually be read or checked by an
implementer — the D7 quote and the throughput figure must be taken on
faith.
*Recommendation:* either commit these evidence documents to the repo
(even under a `docs/plan/` path) or mark them explicitly as
external/generated planning artifacts not expected in-tree, so a
reader isn't left assuming a `find` failure is their own mistake.

**3. (Medium) O6's HdlExporter citation is already known-wrong and was never fixed in the issue body.**
O6 claims the exporter aborts on any element in none of
`EXPORTED`/`SKIPPED`/`TOPOLOGY`/`REJECTED` at lines `:429`/`:438`/`:443`/`:460`.
Independently verified against current `src/jls/hdl/HdlExporter.java`:
there are only **three** named buckets (`EXPORTED` line ~424, `SKIPPED`
~432, `TOPOLOGY` ~438); unbucketed elements fall into a local
`offenders` list and throw — there is no `REJECTED` set anywhere in the
file. This exact discrepancy was already flagged by the project's own
evidence-pin bot in a comment on this issue (2026-08-03,
"the load-bearing conclusion is unaffected... there is no `REJECTED` at
`:460` — that map is branch-only"), but the issue body itself was never
edited — Method step 6 and the Completion Criteria still reference the
stale four-bucket structure. An implementer working from the body
alone (rather than digging through 3 comments) will hunt for a
`REJECTED` bucket that was never real on `master`.
*Recommendation:* edit O6 and the Method checklist to read
"EXPORTED/SKIPPED/TOPOLOGY, offenders otherwise" per the bot's
correction, rather than leaving the fix stranded in a comment.

**4. (Medium) The confinement ratchet (P8) is weaker than the hypothesis (H2) it's used to close out.**
H2 states the strong claim: *"no `.jls` content can cause a device to
open."* P8's actual mechanism (following `SocketConfinementRatchetTest`'s
idiom, O4) is a source-scan asserting `javax.sound.sampled` device
*construction* syntax appears in exactly one class — a string-match
ratchet, not a data-flow proof. The issue is candid about this in
Threats to Validity ("the confinement ratchet proves confinement, not
unreachability... it does not prove no `.jls` content reaches that
class"), which is good practice — but the Completion Criteria still
list `aCircuitWithoutTheFlagNeverOpensAHostDevice()` as a hard gate
without any companion test that actually round-trips a crafted `.jls`
attribute through to the confined class and asserts it cannot select
device-vs-file mode. As specified, an implementation where a saved
element attribute (legally an `int`, per O5/H3) selects live-vs-file
mode inside the one permitted class would pass P8 (one construction
site, allowlisted) while silently violating H2 — exactly the
"acceptance criterion passes while the real goal fails" pattern.
*Recommendation:* add a P8-companion test that specifically asserts no
`Element`/`Attribute`/saved field on `HostAudioSink`/`Source` is read
by the branch that decides device-vs-file, not just that the
`javax.sound` call site is singular.

**5. (Medium) P2's determinism bar is materially weaker than what the parent feature requires.**
P2 asserts the golden WAV is "byte-identical... on two runs" — same
machine, same run. Parent #346's own Integration Criterion 2 requires
"byte-identical WAV across the three CI platform legs and two JDKs...
determinism claimed on one platform is not determinism" (explicitly
spans #265). #462's Definition of Done can be fully checked off with
only single-machine, single-JDK byte-identity, silently falling short
of the standard #346 states is necessary — visible only by
cross-referencing the parent issue, not from #462 alone.
*Recommendation:* either add a minimal cross-JDK/cross-platform check
to #462's own DoD, or add one explicit line stating the platform-parity
gap is deliberately deferred to #346/#265 so a reviewer doesn't assume
#462 alone proves determinism.

**6. (Low) Status churn: superseded, then withdrawn, same conversation.**
The issue was marked "Superseded by #346 (feature/task deduplication)"
in one comment, then that supersession was explicitly reversed
("WITHDRAWN... this task stays open") in a later comment on the same
day this issue was last updated (2026-08-08). Net effect is fine (task
stays open, correctly, since a feature absorbing its sole planned task
is not true duplication), but scope is now reconstructed across the
issue body plus three comments layering corrections and reversals — a
new implementer must read all of it, not just the body, to get current
scope right.

## Solid parts (verified, no issue)

- Numeric claims check out precisely against current `HEAD`: `LogicElement`'s sealed `permits` clause has exactly 24 types; `ElementRegistry.ALL` has exactly 35; `Element.setValue` has exactly the four overloads claimed (`int`/`long`/`BigInteger`/`String`) — matching O5's "no real-number item kind" claim.
- `docs/file-format.md`'s item-kind grammar (`int-item | long-item | bigint-item | string-item | ref-item | pair-item | probe-item | circuit-block`) matches O5's citation.
- `test/jls/SocketConfinementRatchetTest.java`, `SaveTagsTest`, `PaletteContractTest`, `CliFlagTableTest`, and `WaylandStartupCliTest#helpIsUnaffectedAndDocumentsTheEscapeHatch` all exist exactly as cited.
- `javax.sound`/`System.in` are genuinely absent from `src/`/`test/` today (O1 reproduces), and there is genuinely no `module-info.java` (O2 reproduces) — the stated starting conditions are real.
- `docs/vcd-interop.md:18-24`'s co-simulation rejection under #63 is real and the reconciliation obligation the issue calls out is a legitimate, well-targeted requirement.
- Scope boundary (no analog solver, no `double` item kind, no `extension-points.md` row) is crisp and consistently enforced through the doc.

## Overall

The design is thoughtful and unusually self-critical (the Threats to
Validity section already names its own weakest points), and the
concrete facts about the current codebase are accurate where they can
be checked. The verdict is held to sound-with-concerns rather than
sound because of the unenforced #424 race (finding 1), the
unverifiable evidence base (finding 2), and a stale, bot-flagged
citation left uncorrected in the normative body for five days
(finding 3) — all fixable without re-scoping the task, but real enough
that a reviewer should ask for them to be addressed before
implementation starts.
