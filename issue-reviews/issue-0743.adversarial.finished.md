# Issue #743: TASK-C544-4: a scripted Orca session builds and simulates a two-gate circuit by keyboard alone, in the Wayland CI rig, with the spoken output asserted
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#743 is the fourth task in the `TASK-C544-*` chain under FEAT-C26-3 (#544): it wants
`OrcaLabSessionTest` to drive a *real, scripted Orca screen-reader session* against a
Wayland CI rig and assert the *spoken output* at each step. That is a materially
different testing strategy from the two tasks it is ordered after — both of which
deliberately test the accessible model headlessly, without any screen reader — and it
depends on CI infrastructure (a running Orca process, AT-SPI, a speech backend, and the
JVM's accessibility bridge) that does not exist anywhere in this repository today. None
of that gap is named, costed, or tasked in the issue body.

## Findings, most severe first

### 1. (High) No Orca / AT-SPI / speech-synthesis infrastructure exists anywhere in the repo, and #743 neither builds it nor names the gap

`grep -i 'orca|espeak|speech-dispatcher|at-spi|atspi|dbus'` over `.github/workflows/ci.yml`
returns nothing. `scripts/wayland-rig.sh`'s tool preflight (lines 66-68) checks only
`sway swaymsg grim jq`; there is no `orca`, no `speech-dispatcher`/`espeak`, no AT-SPI
bus. #101 — the feature that owns this exact rig, fetched in full — documents precisely
two capabilities in its Capability Statement: booting the GUI under `WLToolkit` and
screenshot/pixel-diffing it. It contains zero mentions of Orca, AT-SPI, or any
accessibility bridge anywhere in its abstract, interface contract, or completion
criteria. This is not a new observation — this fleet's own review of the parent feature
(#544, `issue-reviews/issue-0544.adversarial.md`, finding 2) already flagged that "the
named CI substrate for the central test has no screen-reader capability... a materially
larger and differently-owned piece of work than a thin slice proving Swing can deliver a
live announcement." #743, filed the same day as #544, inherits that exact gap for its
own headline deliverable and says nothing about it.

**Recommendation:** either (a) file and land the Orca/AT-SPI CI infrastructure as its own
task before #743 can be attempted, with its own cost estimate and owner, or (b) rescope
#743 to what #739/#741 already build toward (see finding 2) and drop the "scripted Orca
session" framing from the title and AC entirely.

### 2. (High) Contradicts the explicit, stated testing strategy of both tasks it depends on

TASK-C544-2 (#739) AC-3, verbatim: *"Traversal is asserted headlessly against the
accessible model, so the assertion does not require a screen reader to run."* TASK-C544-3
(#741) AC-2: *"Announcements are emitted through the same accessible model TASK-C544-2
traverses; no second announcement channel exists."* Both upstream tasks in this exact
chain deliberately chose headless accessible-model assertions specifically to avoid
needing a live screen reader. #743 then requires "a scripted Orca session" that "asserts
the spoken output" — a real AT-SPI consumer producing real speech (or at minimum real
AT-SPI announcement events), which is precisely the mechanism #739 states its own tests
were built to *not* require. Nothing in #743 explains why this one task breaks from the
pattern its two prerequisites established, or how "spoken output" is captured (see
finding 4).

**Recommendation:** state explicitly why `OrcaLabSessionTest` needs a live Orca process
rather than the same headless accessible-model assertion #739/#741 already use — or drop
"scripted Orca session" and reuse that established, cheaper mechanism, reserving real
Orca for the manual/NVDA-style checklist #544 already carves out as non-automated.

### 3. (High) AC-1/AC-3 don't branch on #741's own documented fallback outcome

#741 AC-3 explicitly allows: *"If TASK-C544-1's spike found live announcement
unreachable through Swing, this task instead delivers the on-demand state query and the
reduced set is recorded by name as a VPAT exception."* That is a real, tracked possible
outcome one task upstream of #743. But #743 AC-1 unconditionally requires the test to
assert "the state-change announcements," and AC-3 requires "a deliberately removed
announcement turns the test red" — both presuppose live state-change announcements exist
to be asserted and to be removable. If #741 landed via its documented reduced-set
fallback instead, #743's AC-1/AC-3 become unsatisfiable as written, and the issue gives
no branch, no re-scope note, and no pointer to a REPLAN. This is the same
contradiction-shape this fleet's review of #544 already found between that issue's AC
bullet 1 and bullet 3 (`issue-0544.adversarial.md` finding 1) — it has propagated one
level down into #743 without being resolved.

**Recommendation:** add the same conditional #741 itself carries: "...asserting the
state-change announcements, or the on-demand query if #741 recorded the reduced-set VPAT
exception" — and make AC-3's "deliberately removed announcement" scenario explicitly
apply only to whichever mechanism actually shipped.

### 4. (Medium) "The spoken output" is not defined as an assertion target — underspecified and gameable

AC-1/AC-2 require asserting "the spoken output... content, not merely that speech
occurred," but never say what is actually captured: synthesized audio (which would need
speech-dispatcher/espeak wired into the rig, plus audio capture and comparison or STT —
none of which exists), or AT-SPI announcement/accessible-name events intercepted before
they ever reach a speech engine (which is functionally identical to what #739/#741
already assert headlessly, just renamed). A test can satisfy the literal AC text by
querying `AccessibleContext` strings directly through an AT-SPI shim while never running
a real Orca process or producing real speech — which would make the issue's own title
("a scripted Orca session... with the spoken output asserted") false advertising for what
actually shipped, and undermines the honesty AC-4 asks for elsewhere in the same issue.

**Recommendation:** name the actual capture mechanism (AT-SPI event stream, Orca's own
log/braille output, or synthesized audio) as part of the acceptance criteria, not left to
implementer discretion.

### 5. (Medium) The JVM's accessibility bridge — a prerequisite for any AT-SPI consumer, Orca included, to see Swing at all — is never enabled anywhere in the tree

`scripts/build-installer.sh` has no `accessibility.properties` entry and no
`-Djavax.accessibility.assistive_technologies=...` anywhere in the repo (confirmed by
grep). Without enabling Java's assistive-technology bridge, no AT-SPI consumer —
including a real Orca — receives any events from a Swing application on Linux, in CI or
on a real desktop. #743 depends on this being switched on somewhere in the launch path it
tests, but the issue never names it as a prerequisite, a cost, or a task.

**Recommendation:** add enabling the accessibility bridge (with its own small task or as
an explicit prerequisite of #743) to the dependency graph rather than assuming it is
already reachable.

### 6. (Medium) `ordering_after` omits #411, which AC-4 requires the test to reckon with

AC-4 requires the test to "reuse the #101 rig's provisioning and exit-code contract and
not inherit its known weaknesses silently — if it depends on the pixel gate or the
download fail-open, it says so and names #411." That's a good instinct (see "What's
solid"), but #411 — the task that will change the rig's pixel-gate and download-fail-open
behavior — is not in `ordering_after`, only `[TASK-C544-3]`. If #411 lands after #743, the
"named" caveat #743 recorded can silently go stale (the weakness it named may no longer
exist, or a new one may appear) with nothing forcing re-validation.

**Recommendation:** add #411 to `ordering_after` (or explicitly note it is a soft
reference only, with a re-check obligation), consistent with how #745's review flagged
the identical omission pattern for a sibling task in this same chain.

### 7. (Low) `band_mw: 1.5-2` likely does not cost what AC-1 literally asks for

If AC-1 is read literally — a genuinely scripted, real Orca session with content-level
speech assertions in CI — that includes building AT-SPI/speech infrastructure in the rig
(findings 1, 5) that the #544 review already characterized as "a materially larger and
differently-owned piece of work" than the per-feature cost band accounts for. A 1.5-2 mw
estimate for one task suggests the author intends the cheaper, headless-model-only
interpretation (finding 2/4), in which case the title and AC-1's literal wording are
misleading about what is actually being built.

**Recommendation:** reconcile the cost estimate with whichever interpretation of
"scripted Orca session" the issue commits to (see finding 4); if it is the full literal
version, the band needs re-costing.

## What's solid

- AC-3's red-before-green discipline ("a deliberately removed announcement turns the test
  red, and that red run is recorded before any pass is counted") is good practice and
  matches this fleet's established falsification convention elsewhere in the tree.
- AC-4's instinct — name a dependency on the rig's known weaknesses (pixel gate,
  download fail-open) rather than silently inheriting them — is honest and consistent
  with the project's stated documentation norms; it just needs the ordering edge (finding 6).
- `ordering_after: [TASK-C544-3]` is directionally correct: building the announcement
  source of truth (#741) before writing an integration test on top of it is the right
  sequencing, even though the test's own methodology (finding 2) is in question.

## Verdict rationale

The core deliverable — an end-to-end acceptance test proving the blind-lab-path claim —
is the right thing to want, and the issue's own AC-3/AC-4 show real engineering
discipline (falsification evidence, naming known weaknesses). But the issue asks for a
"scripted Orca session" against CI infrastructure that provably does not exist (finding
1), using a testing methodology that contradicts what its own two prerequisite tasks
explicitly chose to avoid (finding 2), without defining what "spoken output" even means
as an assertion target (finding 4), and without branching on a fallback outcome its
direct prerequisite (#741) explicitly documents as possible (finding 3). These are
specification gaps fixable by editing the issue — reconcile the testing strategy with
#739/#741, define the capture mechanism, add the missing conditional and ordering edges —
not reasons to abandon the goal. **needs-rework.**
