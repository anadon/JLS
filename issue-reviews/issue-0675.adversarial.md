# Issue #675: TASK-C101-1: the Wayland rig's first light is published — a green run's artifacts and every startup exception verbatim, as an in-tree findings document
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of the task

Take one green `gui-wayland` CI run's artifact bundle
(`desktop-before.png`/`desktop-after.png`, `tree.json`, `control-*.log`,
`jls-stderr.log`, `control-verdict.txt`, `pixel-diff.txt`) and turn it into
an in-tree `docs/` findings document plus a summary comment on #101,
discharging #101's "First-light report posted as a comment here" completion
criterion. Small, docs-only, no code or test changes implied by the
acceptance criteria as written.

## Findings, most severe first

### 1. (High) A sibling issue already claims this work is done — and its claim is checkably wrong

#411 ("TASK-0018 (RESIDUAL)"), filed one day before this issue and covering
the same feature (#101), states in its own Background section:

> "First-light findings are **published**: `docs/wayland-desktop-checklist.md`,
> plus the gsettings-schemas finding recorded at ci.yml L401-L403."

This is false, or at best a category error. `docs/wayland-desktop-checklist.md`
is not a CI first-light findings document — its own header says "**Status:
release procedure**" and it is the *manual, per-release spot-check checklist
on a physical GNOME/KDE desktop* that #100 §10 requires (confirmed by
reading the file: it is a fill-in-the-blanks checklist to be run by a human
and posted as a comment on **#100**, not #101, and it contains no run id, no
screenshot, no `get_tree` excerpt, and no exception log). The "gsettings-schemas
finding" at `ci.yml` L401-403 is a one-line inline comment
(`gsettings-desktop-schemas provides the org.gnome.desktop.* GSettings
schemas...`), not a findings document with verbatim exceptions and a
baseline screenshot.

Cross-checking against #101 itself resolves the ambiguity: #101's own
machine block (`updated_at: 2026-08-04T07:29:05Z`, the same day #675 was
filed) still lists the first-light report as `planned_tasks`, "Status: Not
filed," and its most recent comment (2026-08-04, the boundary-note against
#586) reiterates the "three remaining close-outs" including the report.
So #675's premise — the report has never been published — is correct, and
**#411's Background section is the thing that's wrong**, not #675. But
#675 does not name or reconcile this conflict anywhere, and #411 is `related`
to #101/#675's feature family. A reader who works #411 first (or reads it
first, since both issues sat open simultaneously) could reasonably believe
this task is already satisfied and skip it, or file a duplicate/contradictory
closing comment on #101. **Recommendation:** #675 (or its implementer) should
flag #411's Background section as needing correction before or alongside
doing this work, so the two issues don't produce inconsistent closing
narratives on #101.

### 2. (Medium-High) Feasibility depends on tooling the issue never names, and this repo's own history shows that tooling can be blocked

Acceptance criterion 1 requires pulling a green run's actual artifact bundle
(screenshot PNG, `get_tree` JSON, logs) and committing it in-tree. That
requires downloading a completed GitHub Actions artifact (e.g. `gh run
list` / `gh run download`), which is not mentioned anywhere in the issue.
This is not hypothetical friction: the comment history on #101 itself
records that the JetBrains CDN (`cache-redirector.jetbrains.com`) returned
403 from "the authoring sandbox" for weeks (comments from 2026-07-09
through 2026-07-19), blocking the JBR checksum fill-in purely on network
egress grounds. An implementer working in a similarly sandboxed environment
has no guarantee `gh run download` (which needs GitHub API egress and a
token with `actions:read`) will succeed either, and the issue gives no
fallback (e.g., "if artifact retrieval is blocked, ask a maintainer to
attach the bundle"). **Recommendation:** state the retrieval method and a
fallback path explicitly, given this repo's demonstrated history of sandbox
egress failures on adjacent steps of the exact same feature.

### 3. (Medium) AC1 asks for "the sway version," which no rig step actually records

AC1: "A findings document lands in `docs/` naming the run id, the JBR pin,
**the sway version** and the commit under test..." Checked
`scripts/wayland-rig.sh` and the `gui-wayland` job in `.github/workflows/ci.yml`
(L353-464): the job installs `sway` via `apt-get install -y
--no-install-recommends sway ...` with no version pin, and no step anywhere
runs `sway --version` or otherwise captures it into an uploaded artifact.
`sway.log` (the compositor's own log) may or may not print its version
banner depending on sway's build — it isn't guaranteed, and it isn't listed
among the artifacts the rig documents in its own header comment
(`wayland-rig.sh` L10-20). So satisfying AC1 literally requires the
implementer to go outside the artifact bundle entirely — e.g., re-derive the
Ubuntu `sway` package version from the raw Actions job log's `apt-get
install` output for that specific run, which is a much less durable source
than the artifact bundle the rest of AC1 is built around. **Recommendation:**
either have the rig capture `sway --version` into an artifact (cheap, one
line) before requiring it in the findings doc, or drop it from AC1 as
unenforceable from the stated inputs.

### 4. (Low-Medium) The Outcome narrative promises "interaction" exceptions the rig cannot produce; AC2 quietly (correctly) drops them

The Outcome section asks for "every startup **and interaction** exception
quoted verbatim" (language pulled from #101's own `planned_tasks` entry:
"every startup/interaction exception verbatim with stack traces"). But
`scripts/wayland-rig.sh` performs no interaction whatsoever — it boots the
control frame, boots JLS, screenshots, and exits; there is no `wtype`
keystroke/mouse scripting anywhere in the rig, and #101 §1 explicitly scopes
"interaction scripting with wtype" **out**, assigning it to #91. AC2, as
actually written in #675, correctly narrows scope to "control and JLS
stderr logs" only (no mention of interaction) — so the concrete acceptance
criterion is fine, but the Outcome prose sets an expectation the rig cannot
fulfill, inherited uncritically from #101's stale planned-task wording.
**Recommendation:** drop "and interaction" from the Outcome paragraph, or
add one sentence explaining why AC2 doesn't cover it (no interaction step
exists yet; that's #91's territory).

### 5. (Low) "Every exception... reproduced verbatim" has no definition of what counts as an exception, inviting a gameable pass

AC2's escape hatch ("an empty exception set is stated as such, not omitted")
is good — it forecloses silently skipping the section. But there's no
criterion for what qualifies as an "exception" to begin with. `stderr` from
a Swing/AWT boot on an experimental toolkit routinely carries benign
toolkit warnings that aren't crash-causing stack traces (the companion
`docs/wayland-desktop-checklist.md` even names this class explicitly:
"no toolkit warning other than JBR's known experimental-toolkit notices").
A shallow pass could satisfy AC2 by grepping literally for the string
`Exception` and missing borderline cases (a caught throwable logged via
`printStackTrace()` without "Exception" in its first line, or a JUL/SLF4J
warning that is functionally a swallowed error). Conversely, an
over-literal pass could dump every JBR experimental-toolkit notice as if it
were a "finding," padding the document with noise AC4 already asks to be
stated as a known limitation instead. **Recommendation:** define "exception"
in the doc's own template as "anything with a Java stack trace (`at
<package>.<Class>.<method>(...)`) in the stderr logs," which is checkable
mechanically and matches what the rig actually captures.

## What's solid (no action needed)

- The requested artifact list (screenshot, `get_tree` excerpt, control/JLS
  stderr, `control-verdict.txt`, `pixel-diff.txt`) maps exactly to what
  `scripts/wayland-rig.sh`'s header comment (L10-20) says it produces — no
  invented inputs.
- AC1's insistence on **committing** the screenshot/tree rather than linking
  to the Actions run is a correct hedge against exactly the artifact-retention
  risk Finding 2 raises — the issue already defends against the failure mode
  it can't fully eliminate.
- AC3/AC4 (record the AE as a number; name the two known lane weaknesses and
  point at #411 as the closer) are concrete, easily falsifiable, and
  consistent with #101's actual `pixel-diff.txt` semantics and #411's actual
  (correctly cited) scope for the pixel-gate and fail-open closures.
- The task is small, additive, and touches no code path (`part_of_feature:
  101`, `band_mw: 0.5-1`), so blast radius on a docs-only deliverable is low
  even if some of the above is wrong on first pass.

## Verdict rationale

Not `needs-rework`: the acceptance criteria are concrete and mostly
verifiable against the real rig, and the core premise (report not yet
published) is correct once cross-checked against #101's live state, despite
#411's contradicting claim. `sound-with-concerns` because Finding 1 is a
genuine cross-issue inconsistency that could cause duplicated or conflicting
work on #101 if not resolved first, and Findings 2-3 are concrete feasibility
gaps (undocumented tooling dependency with a demonstrated history of failing
in this exact repo; a required data point the rig doesn't capture) that
should be closed before someone picks this up expecting a one-sitting task.
