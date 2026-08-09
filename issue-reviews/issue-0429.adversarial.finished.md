# Issue #429: TASK-0094: one export writes the design, a tt_um_* wrapper matching the shuttle's fixed signature and its info.yaml — or writes none of them and names every problem
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The engineering shape of this task — reuse `PcfEmitter`'s all-or-nothing
validation, reuse `-board`/`-pins`/`PinBindings` unchanged, no `default` arm
on `Board.Format`, no new dependency — is sound and matches the codebase as
it actually exists today (verified against `src/jls/hdl/board/Board.java`,
`PcfEmitter.java`, and `src/jls/JLSStart.java:420-475,908-916`, all
consistent with the issue's quotes). The problems are not in the mechanism;
they are in (1) a dependency graph that is materially stale as of *today*,
in a way the issue's own machine metadata cannot see, and (2) a Definition
of Done that lets an executor ship a wrapper the issue itself calls a lie.

## Findings, most severe first

**1. (High) The machine `blocked_by` field is incomplete, and the gap is not hypothetical — it is currently live.**
`blocked_by: [416]` is the only machine-readable prerequisite, but the
issue's own prose treats two more issues as hard gates: the Falsification
Criteria say plainly "If the wrapper is generated while `Direction` is
still two-valued (O3): stop," and the Method's first bullet requires
confirming "TASK-0049 ... and TASK-0077 ... have landed, or record
waivers." Both of those were, per the issue's own text, "NOT YET FILEABLE
AS EDGES ... being filed concurrently; a link pass adds them." I checked
whether that link pass happened: TASK-0049 was filed as **#474** on
2026-08-03, then closed **today** (2026-08-08, `state_reason: duplicate`)
as absorbed into #339, whose own migration comment states "#339's
`planned_tasks` roster now resolves to an absorbed issue, so the feature
has no filed child. It must be executed directly against the migrated
method, or a successor task filed — the empty roster must not be read as
'unplanned'." TASK-0077 is filed and open as **#478**. Neither of these
edges was ever added to #429's `blocked_by`/`related` fields, and now one
of them points at an issue number (#474) that no longer represents live
work. Anyone scheduling off the machine block alone — which is the field
this whole corpus convention exists to make authoritative — will see one
blocker (#416) and miss two, one of which currently has no committed
owner at all.
*Recommendation:* before this issue is picked up, add #339 (or its
successor task once filed) and #478 to `blocked_by`/`related` with
mirrored edges, exactly as the issue itself says a link pass should do.

**2. (High) The Definition of Done is gameable in exactly the failure mode the issue calls "the worst available failure location."**
§10 states unconditionally that generating the wrapper while `Direction`
is two-valued means "stop," because `uio_oe`'s binding is "unevaluable
rather than false ... a worse failure because it is silent," and a
foundry-discovered failure is explicitly ranked worse than a CI failure.
Yet the Completion Criteria checklist immediately supplies an escape
hatch: "`blocked_by` #416 has landed, and TASK-0049 and TASK-0077 have
landed — **or the waiver records that the wrapper's `uio_oe` and `rst_n`
bindings are fictions that fail at the foundry rather than in CI**." A
`WAIVED:` comment is all that is required by the checklist to close this
issue with a wrapper the issue's own hypothesis section says produces "a
chip that never resets" and a bidirectional binding that is silently
wrong. Every other box (P1–P10, H1–H3, §10, §7) can be ticked while this
one is waived, so the stated acceptance mechanism does not actually
block the outcome the issue spends two paragraphs warning against.
*Recommendation:* make landing #339's successor and #478 (or their
functional equivalent — a real third `Direction` and a real `Register`
reset) a **hard** precondition in `blocked_by`, not a waivable DoD line;
if a genuine waiver process is wanted, require silicon-submission tooling
to refuse the run rather than merely recording a comment.

**3. (Medium) Silent scope narrowing against the issue's own cited roadmap source.**
`docs/capability-roadmap/sweep-06-physical-boundary.md` §F — the design
document `part_of_feature: 328` traces back to — names **three** shuttle
artifacts: the `tt_um_*` wrapper, `info.yaml`, and "a LibreLane/OpenLane
config stanza" (line 373). #429 redefines "three files" as the design's
own Verilog plus the wrapper plus `info.yaml` (P7, §13) — silently
dropping the flow-config stanza and substituting the ordinary export
output for it, with no comment anywhere in the issue acknowledging the
deviation from its own source document. A reader who traced #429 back to
§F, as the issue's citation trail invites, would reasonably expect a
LibreLane config to be produced too. It may be intentionally deferred to
TASK-0095 (#432), but the issue never says so.
*Recommendation:* add one sentence to §7.1 or §13 stating explicitly that
the flow-config stanza is out of scope here and naming which issue owns
it.

**4. (Medium) `Board.pins()`'s "location" field has no defined meaning for the shuttle entry, and the issue's own Open Question 2 says this "blocks execution" — but that gate is not in `blocked_by` either.**
O9 candidly flags that the shuttle's "pins" are fixed signal names, not a
package's physical pads, and explicitly declines to resolve the
resulting representational stretch: "That stretches the abstraction and
is #328's Open Question 2; it is **not** re-decided here." Open Question
2 in turn says the choice "**Blocks execution** of the entry's shape.
Decide on #328, not here." So a decision this issue cannot start without
lives entirely in prose on a different issue, again invisible to
`blocked_by`. This is the same failure pattern as Finding 1 (real gates
recorded only in prose), recurring for a second, independent prerequisite.
*Recommendation:* either resolve Open Question 2 before filing further
downstream work, or record it as a machine-readable blocking edge.

**5. (Low) The three-file "atomic set" framing partially obscures a pre-existing, unfixed gap in the two-file case.**
Current `JLSStart.java:438-475` writes the HDL file via temp-and-rename,
then separately writes the derived constraint file via its own
temp-and-rename. If the second write fails (disk full, permission
change mid-run), the first file is already on disk — a partial two-file
export, today, unconditionally. §7.10's "honest limit" (rename phase not
atomic across files) is presented as a new disclosure earned by
generalizing to three files, and §7.12 explicitly says "the existing
one-file and two-file cases keep their temp-and-rename semantics" —
i.e., this task inherits the two-file gap unfixed rather than closing it.
That's a defensible scope boundary, but the issue frames the honesty
disclosure as new territory rather than naming that the same defect
already exists un-flagged in shipped code.
*Recommendation:* one sentence noting the two-file case has the same
residual limit today, so a reader doesn't conclude the atomicity
discussion is purely prospective.

**6. (Low) H2's falsification condition is not actually covered by the stated test plan.**
H2 is refuted by "a field's escaping or nesting" being wrong in the
hand-emitted `info.yaml`. The only test named for this (P2,
`counterInfoYamlMatchesTheGolden()`) is a byte-equality check against one
fixed golden built from one fixture design. YAML escaping bugs
(unquoted colons, leading-zero/boolean-like scalar coercion, embedded
quotes in a student's project title/description) will not surface unless
the golden fixture happens to contain such characters — nothing in §9
requires an adversarial-input case for the free-text fields
(title/author/description) that H2 identifies as the risk.
*Recommendation:* add at least one golden or unit test with a
description/title containing a colon, a quote, and a leading-zero-like
token, to make H2's falsification criterion actually checkable rather
than incidentally checkable.

## What's solid (no rework needed)

- Reusing `PcfEmitter`'s all-or-nothing aggregation and the
  `-board`/`-pins` pair rule unchanged (O6, O7) is verified against the
  live code and is good reuse, not aspirational reuse.
- The no-`default`-arm trap on `Board.Format` is correctly identified
  against `Board.java:35-63` and `PcfEmitter.java:61-64`; forcing a
  compile break is the right mechanism and is already established
  practice in this codebase (same pattern in #416).
- Declining to add a YAML dependency for eleven scalar fields (H2) is a
  reasonable call given the project's stated offline-jar/BOM discipline.

## Verdict rationale

The engineering plan is not wrong, but the issue is not currently
executable in good faith: its own stop condition (two-valued `Direction`)
holds today, its own DoD lets that stop condition be waived away instead
of enforced, and two of its three real prerequisites live only in prose
where a scheduler working from the machine block will miss them —
one of which was reshuffled by the tracker's own deduplication pass in
the hours before this review. Needs-rework: tighten `blocked_by`, remove
or harden the waiver escape hatch, and reconcile the artifact count
against the issue's own cited roadmap source before this is picked up.
