# Issue #825: TASK-C569-1: every supported extension point is enumerated in one published document with its type and a compiling example
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## What's being asked

A doc task (`part_of_feature: 569`, `ordering_after: #223`) commissioning one
published document that lists every seam from the #223 catalog with type,
runtime behavior, and a compiling example (AC-1); a build-time doc↔catalog
binding (AC-2); CI-compiled examples (AC-3); an explicit non-extension-point
section (AC-4); and a no-new-mechanism boundary against #223/#212/#399
(AC-5). Note: a same-day OWNER comment already ran an adversarial pass on
this issue (AC-1/AC-2/AC-5 corrections). I independently verified that
comment's claims against the repo and they check out — `docTableAndConstantsAgreeBothWays`
in `test/jls/ExtensionPointCatalogTest.java:165-186` does exactly what AC-2
asks for, and `#399` is indeed closed (`state_reason: duplicate`, closed
2026-08-08). I did not re-derive those below except where I extend them;
this review focuses on gaps that comment did not cover.

## Findings, most severe first

**1. [Critical] AC-1/AC-3 can be satisfied to the letter while giving readers a false picture of what the seams currently do.**
"What the runtime does with it" (AC-1) and "compiles against the published
API" (AC-3) both presuppose contributions have an observable effect. They
do not, for any of the four typed-now seams, right now:
`grep -rn "ExtensionRegistry" src/ | grep -v "src/jls/boot/\|src/jls/module/"`
returns nothing — no production code outside the boot/module packages ever
calls `ExtensionRegistry.contributions(...)`. `src/jls/boot/JlsModules.java:31-34`
says so in its own javadoc ("the registry is populated but nothing reads it
for dispatch yet"), and issue #277 (open, the dispatch-consumption task,
`part_of_feature: 223`) is the fix for exactly this — but #277 is not in
#825's `ordering_after`, and #825's boundary clause (AC-5) never mentions
it. An author following AC-3's letter can write a compiling example that
constructs an `ExtensionRegistry`, contributes to it, and asserts
`contributions()` sees the entry — real, CI-compiled Java — while the
statement "here is what the runtime does with your contribution" is false
for every one of the four seams: `Circuit.java:918`, `Palette.java:218`,
and `JLSStart.java:382-385` still dispatch off static tables (per #277's
own O2/O3 and #403's O1). This directly undercuts #569's stated purpose
("the plugin story Digital never offered") — the published document risks
either quietly admitting contributions are currently inert (weakening the
deliverable's value) or glossing over it (misleading readers), and neither
AC-1 nor AC-2's existing test would catch the omission either way.
**Recommendation:** add #277 to `ordering_after` (or explicitly scope AC-1
to state, per seam, whether the seam is dispatch-consumed yet), and make
AC-1's "what the runtime does with it" require the honest current answer
rather than the aspirational one.

**2. [High] AC-5's `#399` boundary reference is dangling, and the same stale reference is inherited by parent issue #569 — fixing #825 alone leaves it live.**
Confirmed independently: `#399` (`state: closed, state_reason: duplicate`,
closed 2026-08-08T16:48:11Z, absorbed into #212) is exactly what AC-5 cites
("the single in-tree loading class stays in #399"). But #569's own Notes
section carries the identical sentence verbatim ("the single in-tree
loading class stays in #399") and was still showing it as of this review
(`updated_at: 2026-08-08T17:42:54Z`). Since #825 is a child task of #569,
correcting only #825's AC-5 without a companion fix on #569 leaves the
parent feature issue pointing new readers at the same stale reference.
**Recommendation:** the correction (`#399` → `#212`) needs to land on
#569 too, not just be noted in an #825 comment.

**3. [Medium] AC-4 is not falsifiable and has no test analogous to AC-2's.**
AC-2's catalog↔doc binding is enforced by a reflective, bidirectional
test. AC-4 ("states which seams are *not* extension points … as explicitly
as it states which are") has no comparable mechanism proposed or possible
without a definition of the universe of "internal surfaces a plugin must
not reach for" — there is no closed list in the codebase to check against
(unlike the `ExtensionPoint` constants AC-2 checks). A minimal paragraph
naming one or two internal classes would satisfy AC-4's text without being
exhaustive, and nothing in CI would notice. This is compounded by the
three `pending` rows in `docs/extension-points.md` (`hdl.importer`,
`app.command`, `gui.theme`) which are neither "an extension point a
plugin may use" nor "an internal surface" — a third bucket AC-4 doesn't
name (already flagged in the same-day OWNER comment; I confirm it's real
by reading the table directly). **Recommendation:** either scope AC-4 to
an enumerable set (e.g., every non-`ExtensionPoint`-constant public class
in `jls.module`/home packages) so it can gain a test, or downgrade it from
an acceptance criterion to prose reviewed at PR time, stated as such.

**4. [Medium] `band_mw: "0.5-1"` doesn't price in new CI infrastructure this task actually needs.**
AC-3 requires compiled-in-CI examples. The repo's existing convention for
doc code samples (e.g. `docs/batch-interface.md:114`, `docs/picocli-evaluation-2026-07.md:172`)
is either narrative excerpts from real test files or a one-off spike file —
`grep` across `docs/*.md` and `test/` turns up no existing "extract and
compile markdown code fences" harness. Building and wiring that harness
(or, more cheaply, adopting the excerpt-from-real-test-file pattern and
asserting the excerpt matches the source) is unbudgeted work distinct from
writing prose, on top of the already-flagged reduction of AC-1/AC-2's
scope. **Recommendation:** name the chosen mechanism (excerpt-from-test vs.
markdown-fence extraction) explicitly in the issue and re-derive the
band estimate once findings #1 and #3 are resolved, since they change the
work's shape, not just its size.

**5. [Low] `ordering_after: #223` is advisory, and #223 is not close to closed.**
The machine block carries no `blocked_by`/hard-gate field, only
`ordering_after`. #223 itself is `state: open` with `blocked_by: [61, 62,
84, 76]` gating its own close-out (the "API freeze" #825 is meant to wait
for). If #825 is picked up before those land, AC-3's compiling examples
for the three currently-`pending` seams (or any contract shape change in
the four typed-now ones) risk needing rework mid-task. **Recommendation:**
state explicitly whether #825 may start before #223's close-out, or make
the ordering a hard blocker.

## What's solid

- AC-5's no-new-mechanism boundary is the right instinct — this genuinely
  should be a pure documentation task riding on #223/#212's shipped
  mechanism, not a chance to smuggle in scope.
- AC-3's "not pasted prose" requirement has a workable, already-precedented
  path in this repo (real test-file excerpts, as `docs/batch-interface.md`
  already does) — it doesn't need net-new tooling if that pattern is
  adopted deliberately (see finding 4).
- The same-day OWNER comment's AC-1/AC-2 rescoping (examples-only residual)
  is accurate and should be treated as the current spec, not the original
  issue body.
