**Capstone:** CAP-10 (#308) — audio out, audio in, WAV render, no analog solver
**Reviewed:** 2026-08-16, against live issue bodies and the master checkout (local HEAD `33c0b71`)
**Verdict: NOT-READY** — the required feature set is filed, open, cheap and startable, but it no longer composes to the capstone's promised outcome. Both required features were rewritten (2026-08-08/08-10 review cycle) into a strictly file-mode scope, and the capstone body was never reconciled: the live-audio door that CAP-10's title, three of its seven outcome steps, two of its five ACs, and three of its ten DoD items depend on now has **no owner anywhere in the tree**, and the capstone's §3 architecture mandates apparatus its own required child explicitly refuses to build. This is a parent–child scope conflict that a body rewrite must settle before implementation is orchestrated.

---

## 1. Decomposition

**What is filed and correct.**

- `requires_features: [346, 367]` — both open, both cross-referenced back (`serves_capstones` includes 308 in both). An exhaustive search of the corpus finds **no other issue** declaring service to #308: no double-ownership, no missing sibling.
- #346 (FEAT-045) is the sole native child of #308; #367 (FEAT-047) is correctly held as a shared, non-native feature (it serves four capstones: #308, #305, #309, #313).
- Task coverage exists at the leaf tier: #462 (TASK-0096) under #346, #882 (TASK-C367-1) under #367. Both open, both rewritten to match their parents' narrowed scopes.
- `requires_capstones: []`, `requires_tasks_exception: []` — clean. The deliberately-excluded analog features (#331, #351, #368) are all open and correctly *not* required; the FEAT-048 `blocked_by: [331, 351]` chain the body discusses genuinely no longer reaches this capstone.

**The decomposition failure.**

CAP-10's §2 claims FEAT-045 contributes "the sink, the source, the PCM WAV codec, the tick-resampler, the `-wav` flag, and the **invocation-time grant**" and that the two features "alone deliver every step in §1." That was true of FEAT-045's *previous* body. FEAT-045's **current** body (post-review rewrite) states the opposite, explicitly: *"No `HostAudioSink`/`HostAudioSource` element, no live device, no invocation-time grant, no confinement ratchet, no extension-point row: none of that machinery is needed to make a circuit audible, and none of it ships here"* — and further that live playback *"is a separate, small, future GUI affordance with no owning task here."* FEAT-045 is now `-wav` file export + `wav2t` file import, file-mode only, by construction.

Consequently the following CAP-10 commitments are **orphaned** — no required feature, no planned feature, no task anywhere owns them:

- §1 step 3 (live playback through host speakers, granted) and step 4's grant-refusal half (the "falsification guard" the body calls the security cornerstone of the whole host-door programme);
- §1 step 5's live-microphone half;
- the entire §3 architecture: drawn `Speaker`/`Mic` `Watchable` elements, the `--allow audio-out` / `--allow audio-in` capability vocabulary, the extended `HeadlessCoreRatchetTest` boundary rule for `javax.sound`, the `docs/extension-points.md` rows, the default-deny test;
- AC-5 in full, AC-2's behavioral half, AC-4's live-mic half (see §2 below);
- DoD items 2 (`docs/vcd-interop.md` + `SECURITY.md` amendments — FEAT-045 now states, with a verbatim quote from `docs/vcd-interop.md`, that `-wav` fits the recorded posture and **"requires no amendment"**) and 3 (extension-point rows — FEAT-045: *"#223. Not touched."*).

This is not a mere gap but a **conflict of theories**: FEAT-045's rewrite argues (with evidence: `-vcd`, `-t`, `-i`, `-export` are all invocation-named host files with no grant apparatus, and #38's ".jls is DATA" premise) that the door machinery is unnecessary for audibility; CAP-10's body argues the door *is* the strategic point ("designing one capability-grant vocabulary now… retires that review for every door that follows"). #508's actual disposition text ("keep-strategic: cheap slice now, rest gated") names no door mechanics either way — CAP-10's "behind one invocation-time grant" gloss is its own interpretation, not #508's ruling. #485's D7 constrains the *shape* of a host door if one is built ("ONE DOOR GRANTED AT INVOCATION… never a property of the circuit file"); FEAT-045 building *no* door does not violate D7. Someone with authority must pick: rescope the capstone to file-mode (and move the live door to explicit deferred/new-scope status, as was already done for the analog rung), or file a new feature owning the live door and overrule FEAT-045's review-backed rejection. Until then, orchestrating implementation from these issues builds to two different definitions of done.

**Secondary decomposition staleness.**

- #346's machine block says `requires_tasks: []`, `planned_tasks: []` and its body says *"No task is filed against this scope yet — the previously filed TASK-0096 (#462) specifies the superseded drawable-element/device-grant design."* But #462 has **since been redisposed** to exactly the current scope ("`-wav` renders a watched signal to PCM… no element, no device door") and awaits maintainer disposition per its own text. #346's body and machine block are one rewrite behind their own child.
- The FEAT-047 minimality argument in CAP-10 §2 ("Remove FEAT-047 and the resampler has no ratio to compute") no longer holds: FEAT-045's lattice is integer decimation with a bare divisor, and its Open Question 3 states #367 is *"not a duplicate and not a dependency… non-blocking."* #367 is still worth requiring (it is what makes "44,100 samples/s" a physical claim, it is cheap at ~1 mw, and the mutual edges exist), but the capstone's stated reason for requiring it is dead.

## 2. Acceptance-criteria composition

| CAP-10 AC | Owner in the required set | Verdict |
|---|---|---|
| AC-1 byte-identical WAV across platform/JDK matrix | FEAT-045 Integration Criteria 1 & 3 + DoD ("byte-identical across the three CI platform legs and two JDKs"), riding #265's lanes | **Composes.** (Naming drift only: CAP-10 says `BatchSimulator.toWav()`; FEAT-045 plans `setWavFile`/`writeWav` mirroring the actual `setVcdFile`/`writeVcd` at `src/jls/sim/BatchSimulator.java:344/359` — immaterial.) |
| AC-2 ".jls can never open an audio device" | Structural half: trivially true under FEAT-045 (no `javax.sound` anywhere, Invariant 1) — though the promised `HeadlessCoreRatchetTest` extension is not in FEAT-045's DoD. Behavioral half ("with no grant, the sink refuses with a diagnostic naming the drawn element"): **no owner** — there is no grant, no sink element, no diagnostic in any child. | **Half-orphaned; behavioral half unsatisfiable as written.** |
| AC-3 ≥44,100 samples/s real time with 2× margin | FEAT-045 IC-5 measures **offline `-wav` render throughput** with method recorded. "Real time" as a gate only means something for live playback, which no child ships. | **Partially composes; the gate's referent is gone.** |
| AC-4 audio round-trips (file + live mic) | File half: FEAT-045 IC-2 (`wav2t` round-trip, asserted on the sample sequence). Live-mic half: **no owner.** | **Half-orphaned.** |
| AC-5 live playback + live capture on real hardware, artifact attached | **No owner.** FEAT-045 excludes live devices wholesale. | **Fully orphaned.** |

So under the current children, **every required child can close green while the capstone fails its own title**, AC-2 (behavioral), AC-4 (live half), AC-5, and DoD items 1 (as scoped), 2, 3 and 8. That is precisely the "every child passes, capstone fails" failure mode this gate exists to catch. Conversely there is no overlap: nothing is double-owned.

KC-10-2's fallback ("drop live playback/capture and ship file-in/file-out only") describes the state the children already *start* in — the kill criterion is pre-triggered by scope, which is the clearest one-line symptom of the misalignment.

## 3. Dependency chains

- **Real and acyclic.** #346 (`blocked_by: []`) and #367 (`blocked_by: []`, `blocks: [486, 490]`) are both open; their tasks #462 and #882 are open with `blocked_by: []`. Every seam they ride was verified present on master: `BatchSimulator.getTraceSamples()` at `src/jls/sim/BatchSimulator.java:329` (line-exact), `setVcdFile`/`writeVcd`/`toVcd`, `test/jls/VcdExportGoldenTest.java`, `test/jls/HeadlessCoreRatchetTest.java`, the `-t`/`-s` grammar docs, `Circuit.FORMAT_VERSION = 2` (`src/jls/Circuit.java:102`). No unfunded external prerequisite is on the critical path; the deferred analog chain (#331/#351/#368) correctly does not reach this capstone.
- **One edge at a closed/redirected issue, inside #346:** its yaml correctly says `serves_capstones: [308]`, but its mermaid graph (`F45 --> C303`), §7 re-planning text, §Related, and its DoD line *"Every capstone in `serves_capstones` (#303, #308) notified"* all still treat **#303 (CAP-11, CLOSED as duplicate of #308)** as a live serving capstone. A close-out following #346's own DoD would post a STATUS comment to a closed duplicate and could miscount "losing both required consumers." Internal inconsistency to fix in #346.
- **Mirror hygiene is otherwise good:** #367's `blocks: [486, 490]` matches those issues' `blocked_by`; #367↔#882 and #346↔#462 parent/child links agree (modulo the #346 machine-block staleness above); #882's declined edge to #682 is documented on both sides per its text.
- Coordination edges (#405 streaming-VCD vs #367's `$timescale` content; #265 platform legs) are named on both sides and are ordering-neutral.

## 4. Staleness and open questions

- **CAP-10's `evidence_commit` (`2d0ca9d…`) does not resolve in a clone.** `git cat-file` on the local master checkout fails; #367's own body records that *"the earlier 2d0ca9d evidence branch was deleted and no longer resolves"* and re-anchored itself to `29afb26`. The commit still answers via the GitHub API only as a dangling object, which is exactly the citation fragility D12 (#485) forbids. Mitigating: the claims it pins were re-verified true at current master (`grep -rn "javax.sound" src/` → 0; `grep -rn "System.in" src/` → 0; `docs/simulation-semantics.md` §1 "dimensionless" at ~line 26; `docs/vcd-interop.md` #63 rejection at lines ~18–23). Re-anchor to a master commit.
- **Cost band contradicted by both children.** CAP-10: 7–10 mw (FEAT-045 5–7 + FEAT-047 2–3), and "the pure solver-free file-out sub-slice inside FEAT-045 alone is 3–4.5 mw per that issue's own Cost section." FEAT-045's current Cost section says **~1 mw** and explicitly retires the 5–7 figure; FEAT-047 is now ≤1 day (step 1) + 3–5 days (#882, `band_mw: "1"`). True current band is roughly **2 mw**, ~4× below the capstone's stated floor. The file-mode capstone is far *cheaper* than advertised — which strengthens the case for the rescope rather than against it.
- **Dead citation in AC-3.** The "~209,000 samples/s (4.7× real time)" ceiling is attributed to "FEAT-045 (#346)'s own Cost section"; no such figure exists anywhere in #346's current body. AC-3 already half-knows this ("not independently reproduced"); after the rewrite the citation is not merely unreproduced but nonexistent. FEAT-045 IC-5's measure-and-record criterion is the correct replacement.
- **§4 cross-feature note stale:** "the tick-resampler… any future analog audio path reuses it verbatim" — FEAT-045 explicitly refuses to build an interpolating resampler ahead of a real consumer and ships integer decimation only, with the resampler deferred to whichever analog feature first needs it.
- **Open Question 1 blocks start as written.** CAP-10 itself says OQ-1 (does the host audio door survive review; generalized `--allow` vocabulary or bespoke) "blocks the live halves of steps 3–5." Under the children's current scope the question has no owner and no vehicle — it is unanswerable from this tree. Resolving it *is* the rescope decision in §1 above; it cannot be deferred past start because it decides what "done" means.
- **Open Question 2** (analog rung returns vs new scope) is genuinely non-blocking; the recommended "new scope, proposed then" answer is consistent with everything else.

## What "ready" requires (concretely)

1. Maintainer decision on the door: **(a)** rescope CAP-10 to the file-mode outcome its children actually deliver — retitle, rewrite §1 steps 3–5, §3, AC-2/AC-3/AC-4/AC-5, KC-10-2, DoD 2/3/8, and record the live door as deferred future scope (the same move already made for the analog rung) — or **(b)** file and require a new live-audio-door feature, explicitly overruling FEAT-045's review-backed rejection, and restore the grant/ratchet/catalog obligations to that feature. Option (a) matches #508's "cheap slice" disposition and both 2026-08 reviews; option (b) matches the current capstone body. Either is coherent; the superposition is not.
2. Re-anchor CAP-10's `evidence_commit` to a commit reachable from master and re-derive the Cost section from the children's current bands (~2 mw).
3. Reconcile #346's body/machine block with the redisposed #462 (list it in `requires_tasks` or dispose of it), and purge #303 from #346's mermaid/§7/DoD.
4. Delete or reattribute AC-3's 209 k samples/s citation.

None of this is implementation work; it is one decision plus two body edits. After item 1 lands, this capstone would be **ready** — the surviving scope is well-decomposed, fully task-covered, dependency-clean, and cheap.
