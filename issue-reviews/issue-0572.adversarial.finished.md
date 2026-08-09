# Issue #572: FEAT-C32-1: the browser-demo go/no-go lands on measurement — a CheerpJ-wrapped jar runs the Swing GUI read-only on the three biggest examples, or a ranked fallback is chosen without re-litigation
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is a well-bounded feasibility spike (PF-1 of capstone CAP-32, #516):
wrap the JLS jar with CheerpJ, measure load time and interaction fidelity on
"the three biggest example circuits," and either declare go or rank the
pre-declared fallbacks. The framing is disciplined and the binding-verdict /
no-relitigation structure is sound project practice. But the spike depends on
an input corpus that does not exist yet, omits a licensing/hosting check for
the one commercial dependency it introduces, ignores a directly relevant
prior decision in this repo's own history, and its acceptance criteria are
loose enough to be gamed in both directions (false go and false no-go).

## Findings, most severe first

### 1. [High] The "three biggest example circuits" do not exist in this repo, and the issue doesn't say so

No `resources/samples/` directory exists (`find . -ipath "*resources/samples*"`
returns nothing), and the curated example set this demo is supposed to
surface — FEAT-C27-2, **#548** — is still **open**, not landed. #548's own
body says plainly: "the curated set CAP-27 (#511) would surface in the
browser [i.e. this demo]." The parent capstone #516 declares
`ordering_after: ["#511 CAP-27 PF-2 (the curated example library is the
content this demo serves)"]` — but #572 itself declares `ordering_after: []`.
Sibling issue #573 (FEAT-C32-2, the actual demo page) gets this right and
lists `"FEAT-C27-2 (#548) — the curated example set is the content this page
serves"` as an ordering dependency; #572 silently drops it.

Concretely, the only `.jls` files in the tree today are test fixtures
(`test/fixtures/riscv-sum1to10.jls` at 120 KB, `fork-4.6-shiftregister.jls`,
`headless-canary-gate.jls`) and `riscv/gui/cpu.jls` — none of which are
"curated examples" (#73's fresh-authorship-for-licensing rule explicitly
restricts reusing test fixtures as samples). Running the spike today means
either inventing an ad hoc corpus with no claim to being representative, or
blocking on #548 landing first — and the issue records neither choice.

**Recommendation:** add #548 (or #511) to `ordering_after`, or explicitly
declare which three circuits are the spike's corpus and why they stand in
for the not-yet-landed curated set.

### 2. [High] No acceptance criterion covers CheerpJ's license or hosting dependency

CheerpJ is a proprietary product with usage-tier licensing (Leaning
Technologies); its runtime is typically loaded via a third-party CDN unless
a paid/self-hosted license is used. AC-5 says "Everything produced runs from
static files only — no backend is stood up even for the spike," but the
issue never asks whether CheerpJ's own runtime download counts as a
dependency outside that boundary, nor whether its license permits the
intended use (a public demo linked from the README, not a private
evaluation). This project is unusually careful about exactly this class of
concern elsewhere — README.md documents SHA256 checksums, signed build
provenance, Authenticode signing via SignPath, and even a paragraph on *why*
the rpm/AppImage forgo a project GPG signature (#136) — yet the spike that
introduces the project's first third-party commercial-license dependency has
no AC checking it. A "go" could be declared on load-time/interaction numbers
alone while the licensing question is never asked, only to surface as a
blocker when #573 tries to actually ship the page.

**Recommendation:** add an AC (or a documented sub-step) verifying CheerpJ's
license permits the intended public, static-hosted use, and stating whether
the runtime is vendored/self-hosted or CDN-loaded at demo time.

### 3. [High] The issue doesn't engage the closed #500 (CAP-19) precedent, which already evaluated and rejected this class of mechanism

#500 (closed `not_planned`, cited in #572's own provenance note for a
*different* reason) directly asked "Hand-written JS interpreter vs compiling
the Java kernel (TeaVM/**CheerpJ**)?" and recommended *against* compiling the
kernel: "compiling the kernel drags a toolchain into the build and produces a
megabyte-scale artifact." That's the same mechanism #572 proposes to spike,
in the same repository, days apart in the issue timeline. #572 quotes #500's
*editor/export* framing (KC-32-2, "no export feature… cites the CAP-19
closure") but never mentions that #500 already looked at CheerpJ specifically
and passed on it. The two contexts differ (per-circuit export artifact vs.
whole-jar demo — CheerpJ here doesn't create the "second execution engine"
problem that was #500's real kill criterion, KC-19-1), and that distinction
is a legitimate reason the two decisions could differ. But #572 doesn't make
that argument; it leaves a gap for the maintainer to reasonably ask "didn't
we already decide against CheerpJ here?"

**Recommendation:** add a line distinguishing this use (execute the actual
JVM bytecode unmodified, no second semantics to maintain) from #500's
rejected option (compile-and-vendor a second runtime), so the reviewer isn't
left to reconstruct that argument themselves.

### 4. [Medium] The threading model this spike must reproduce isn't checked against CheerpJ's constraints

ARCHITECTURE.md is explicit: "Interactive simulation runs on a dedicated
thread (the `"Runner"` thread, `InteractiveSimulator`...). Control state
shared between the EDT and the sim thread... is `volatile`... UI work
initiated from the sim thread is routed through `SwingUtilities.invokeLater`."
CheerpJ's Java-thread support has historically required
`SharedArrayBuffer`/WebAssembly threads, which need cross-origin-isolation
response headers (COOP/COEP) — headers many static hosts (including plain
GitHub Pages) cannot set without extra configuration. If threading is
degraded or emulated, "toggle inputs, observe signal trace" could visibly
work while updates are silently out of order or delayed — i.e., AC-2 could
report "pass" on a build that is subtly wrong, not just slow. The issue
never mentions checking this.

**Recommendation:** name the hosting target for the spike explicitly (can it
set COOP/COEP?) and add a check that sim-thread → EDT ordering is preserved,
not just that the trace eventually updates.

### 5. [Medium] "Biggest" is an undefined, ungameable-by-omission metric

Neither AC-1 nor the outcome text says whether "three biggest" means element
count, wire count, file size, or subcircuit depth. Absent a definition, the
spike operator picks the metric that produces whichever verdict they expect,
and a later reviewer cannot check the claim. Combined with finding #1 (the
corpus doesn't exist yet), this is currently unfalsifiable.

**Recommendation:** name the metric (e.g., element count from the `.jls`
`CIRCUIT`/`ELEMENT` records) once #548's set exists.

### 6. [Medium] AC-2's "interaction fidelity... pass/fail" has no rubric

"toggle inputs and observe the signal trace" only tests that some pixels
change after a click — it doesn't distinguish "renders correctly and stays
in sync" from "renders something that looks plausible but drifts/lags." A
sequential circuit (register, memory, clock) could visually "pass" while
silently desynchronized from the real event queue, and nothing in AC-2 would
catch it. Given #221's recorded discrete-event-interpreter contract and the
project's general severity about semantic drift (see #500 risk 2 and PF-4's
golden-replay gate for the export case), a demo mechanism with zero
correctness check is an inconsistency with how seriously this codebase
treats "looks right vs. is right" elsewhere.

**Recommendation:** at minimum, script one deterministic input sequence per
circuit and eyeball its final state against the desktop trace, not just
"toggle and watch."

### 7. [Low] AC-1's measurement environment is unspecified despite a hard numeric threshold

"load time from click to interactive is measured and recorded (go
threshold: ≤15s)" — but browser, network conditions, cache state (cold vs.
warm), and hardware are not pinned. A 15s/30s cliff decided by an
unreproducible one-off measurement is a weak basis for a "binding, no
relitigation" verdict (KC-32-1 as inherited from #516).

**Recommendation:** specify cold-cache, throttled-network conditions (or
explicitly note both cold and warm numbers), and the test machine class.

### 8. [Low] "Time-boxed" is asserted, not enforced

The outcome line calls this "a time-boxed feasibility spike" but no duration
appears in the issue body; only the YAML header's `band_mw: "1-2"` implies a
budget, and that field is a sizing estimate elsewhere in this issue family,
not a stated cutoff rule. If the CheerpJ integration turns out to be a deep
rabbit hole (plausible given finding #4), nothing in the text forces a
stop-and-fall-back decision at a fixed point.

## What's solid

- The binding go/no-go structure (KC-32-1, no re-litigating in PF-2) is
  good practice for this repo given its history of scope thrash around
  CAP-19/CAP-32.
- AC-3/AC-5 (read-only by construction, no new #38 threat-model surface, no
  backend) are consistent with the actual threat model in #38 and with the
  capstone's own AC-2/AC-3.
- The self-aware hedge ("if the maintainer judges even this inside the
  CAP-19 refusal's intent, close this too") is a mature scope-conflict
  release valve and correctly anticipates part of finding #3, even though it
  doesn't cover the CheerpJ-specific precedent.
- Narrowing to a measurement-only spike before committing PF-2's build
  effort is the right shape of ticket, independent of the content gaps above.
