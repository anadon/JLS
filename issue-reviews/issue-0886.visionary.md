# Issue #886: FEAT-C32-4: a circuit becomes a link a colleague opens — the whole design travelling inside the URL, with no server, no account and a refusal when it will not fit
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the traceability-row provenance and #886 wants one thing: *a person's own circuit
should be reachable by someone who has installed nothing.* That is the same want CAP-32
serves for curated examples, aimed at user content instead of project content. It is a
real want and it is on the project's arc.

The arc is worth naming, because it is not "JLS gets a web presence." Read the shipped
surfaces: plain-text save (`-savetext`), `Circuit.stateHash()` as SHA-256 of the canonical
save text, byte-reproducible jar and BOM, VCD export for autograders, structural Verilog
export, a headless container image, and #334's whole premise that a `.jls` is *a reviewable
text artifact*. The through-line is **a circuit is a portable, deterministic, verifiable
piece of text**, and every capability the project has added for years has been another
consumer of that text. A shareable link is a legitimate next consumer.

But #886 does not file the consumer. It files the *transport*, and binds its entire
existence to a substrate (#572's CheerpJ go/no-go) that has not been measured yet.

## The seam is cut one layer too high

`blocked_by: [572]` plus KC-32-4-1 says: if the browser substrate fails, this feature is
void, "a link with nowhere to land is a file with extra steps." That is true of the *link*.
It is not true of the thing the link carries.

The durable deliverable here is **a portable circuit text**: a versioned, self-describing,
deflate-compressed, base64url-encoded rendering of the canonical save text. Once that
exists in `src/jls/core` (AWT-free, headless-ratcheted, testable in the JVM suite that
already exists), every transport is a thin adapter over it:

- the URL fragment — `demo.html#c=<blob>`, which is what #886 asks for;
- paste-between-JLS-instances, which needs no browser at all and was CAP-19's PF-5;
- paste-into-an-issue/forum/email, where the recipient may not want a browser demo;
- a `-share` / `-fromshare` CLI pair, which the batch/autograder audience gets for free.

Cost of the encoder itself is close to nothing: `Deflater`, `Base64.getUrlEncoder()` and
`FileAbstractor`'s existing bounded reader are all JDK/in-tree, and on the browser side
`DecompressionStream('deflate')` is native — no vendored JS, no CDN, nothing that can die.
(This also settles the compression choice on a hard constraint the issue does not state:
**it cannot be XZ**, the project's own default container, because no browser can inflate
it natively. Deflate or gzip, or you ship a decompressor.)

Cut here and `blocked_by: [572]` inverts. The encoding is `blocked_by: []`; only the
*fragment adapter* waits on the demo page. KC-32-4-1 stops being a death sentence and
becomes "the link adapter is void; the sharing text survives." That is strictly more value
for strictly less coupling, and it hands #334 the second consumer of the canonical-form
discipline that CAP-19's PF-1 wanted precisely to prove that discipline outside the save path.

**The honesty cost, stated plainly:** this reframing revives CAP-19's PF-1/PF-5 in miniature,
and #886 already revives CAP-19's Open Question 2 ("URL-fragment encoding — in scope?
rides along") while its Boundary says "Not #500." Both should be admitted rather than
routed around. CAP-19 was refused for KC-19-1 — *a second JS execution engine JLS would
then have to defend*. A serialization is not an engine. Say that on #500 and get it
recorded; do not let a serialization sneak in as a side effect of a link.

## The measurement the issue defers, run now

AC-2 wants a measured ceiling. Half the measurement is already available from the tracked
fixtures (canonical text → `zlib -9` → base64url, no padding):

| file | canonical text | deflate | base64url chars |
|---|---|---|---|
| `test/fixtures/headless-canary-gate.jls` | 654 | 276 | **368** |
| `test/fixtures/fork-4.6-shiftregister.jls` | 4,132 | 630 | **840** |
| `riscv/gui/cpu.jls` | 8,878 | 1,104 | **1,472** |
| `test/fixtures/riscv-sum1to10.jls` | 120,179 | 10,384 | **13,846** |

Two conclusions the issue should absorb before execution. First, deflate already returns
6-9x on real JLS text, so there is no second, leaner "share format" worth inventing — which
is a positive argument for AC-3's byte-identity, not a concession. Second, the interesting
threshold is not a browser's: fragments never enter a request, so no 8 KiB header limit and
no 2,083-char legacy limit applies, and current Chrome/Firefox carry fragments far past
100 KiB. The binding limits are *paste channels* — a Discord message caps at 2,000 characters
total, plain-text email wraps at 78 columns, a QR code tops out near 2,900 bytes. `cpu.jls`
clears all of them; the RISC-V CPU clears none of them and would still work perfectly if
pasted into a browser bar.

## Disregarding AC-2 as written: refuse nothing, make the link self-verifying

AC-2 asks for one measured number, published, and *enforced by refusal*. I would not ship
that, and I am saying so against the stated criterion.

A single sender-side ceiling is wrong in both directions. It is too strict for the user
whose channel is a Google Doc, and it is too loose for the user whose channel is a Discord
message — and JLS cannot observe which channel the user is about to paste into. Worse, the
measurement campaign AC-2 mandates (every CAP-32 browser plus "at least one messaging client
that rewrites links") produces a number that goes stale the first time Slack changes its
unfurler, and the number is then load-bearing for correctness.

The elegant version moves the check to the only place that can actually see the damage —
**the recipient**. Put the decoded length and a short checksum in the fragment ahead of the
payload. Then any truncation or mangling by any channel, including every channel nobody
measured, surfaces on arrival as *"this link was cut short in transit — ask the sender for
the .jls file"* rather than as a malformed-payload error. AC-5 is already committing to a
legible decode failure; this costs one header field and upgrades AC-5 from "says something
broke" to "says who broke it."

The sender side then keeps one *advisory* line — "this link is 13,846 characters; it will
survive a browser and a document, and will be cut by a chat message" — which is AC-6's job
anyway, and needs no shipped ceiling to be true. The measurement becomes an afternoon's
informational note in the docs instead of an enforcement contract, and AC-2's brittleness
disappears.

## Making KC-32-4-2 non-fatal: the paste box, not the refusal

KC-32-4-2 says: if the measured ceiling excludes the shipped examples, stop and report,
because adding storage is what CAP-32 exists to refuse. Correct on the storage, but the
dichotomy is false. There is a third option that stores nothing: **the demo page accepts the
same portable text pasted into a box.** A user whose circuit is too big for a link does not
fall back to "download the .jls and install JLS" — they fall back to "paste this block into
the demo page," which preserves the entire zero-install property that motivated the feature.

This inverts the build order productively. The paste box is the general mechanism and the
fragment is a one-click convenience over it; ship the paste box first and the link is
~10 lines on top. It also keeps the KC-32-2 scope cliff intact — loading is not editing —
though that should be written down before someone argues otherwise.

## AC-1 asserts the wrong property

AC-1 calls "the fragment never leaves the browser" the whole architectural claim and wants a
test on the request line. The request line is guaranteed by RFC 3986 and by every HTTP stack
in existence; a test there passes on day one and protects nothing.

The property actually at risk is that **the page has no outbound channel at all**. The
fragment sits in `document.location`, readable by any script the page loads — and the
mechanism #572 favors is a CheerpJ-wrapped jar whose standard deployment fetches its runtime
and JRE class data from a third party at page load. If that deployment ships, AC-1's privacy
claim rests on trusting a vendor's CDN, not on the `#`. Replace AC-1's test with an extension
of #835's AC-4 (zero network activity after the static assets load, asserted from the network
log with a fragment present), and make it a blocking criterion rather than a recorded
observation. That is the test that can fail, which is the only kind worth writing.

## Two smaller places the issue can shed work

**AC-5 is mostly already shipped.** ARCHITECTURE.md's load pipeline already enforces
hostile-input caps at every reader (`FileAbstractor`, issue #38, `UntrustedFileHardeningTest`),
already refuses rather than repairs, and already lands failures in the `LoadError` taxonomy
with `NEWER_FORMAT` distinguished from `MALFORMED`. A link decoder that inflates into the
existing bounded reader and then calls the existing loader inherits all of it. Only the
base64+inflate prefix stage is new, and it needs exactly one thing the tree already models:
a byte cap on the inflated stream, mirroring `MAX_CIRCUIT_TEXT_BYTES`. Writing a
link-specific decoder instead would be the tree's second hardening surface, which is the
failure it has spent #38's whole budget avoiding.

**Open Question 1's blocker is overstated.** It says AC-3 depends on #334/#437's canonical
text landing, and blocks on it. It does not. Determinism of the *text* already ships:
`Circuit.save` canonicalizes newlines and sorts by `(tag, stableId)` (#166,
`DeterministicSaveTest.canonicalBytesAreIdenticalWhateverThePlatformNewline`,
`stateHashIsContentDetermined`), and `stateHash()` is defined as SHA-256 of exactly those
bytes. #334 changes the *reference form* and the *on-disk container default* — diffability,
not determinism. Encode `Circuit.save`'s output and AC-3 is satisfiable at HEAD, with the
round-trip provable by the tests already in `test/jls/` (`CircuitRoundTripTest`,
`GenerativeRoundTripFuzzTest`). Deleting this dependency removes a chain through two unfiled
tasks under a feature that is itself `blocked_by: [315]`.

## Where the issue pulls against the record

CAP-32 AC-2 and #835 AC-3 both say the demo is read-only by construction with **"no user
content"**, and #835 verifies it "by inspection of what the wrapper exposes." #886's whole
point is user content reaching that page. AC-4 cites #835's rule as its own while making
#835's criterion false. The browser sandbox keeps the real risk low, but this project's
scarcest asset is a record that stays coherent, and this is a hole in it.

The fix is not to soften #886 — it is a REPLAN on #516 amending AC-2 from "no user content"
to something honest: *no user content is persisted, transmitted or executed as code; circuit
data arrives only through the fragment and is parsed by the same bounded loader the desktop
program uses.* That is still a strong claim and it is still true. Leaving the old wording
means the capstone's completeness claim is right about everything except the one feature
that changed it — the exact failure mode #886's own opening paragraph is written to prevent.

## KC-32-4-1 is too soft

"It re-plans against whatever fallback #572 chose" is a hope, not a plan. Both declared
fallbacks — (a) interactive SVG driven by a *pre-computed* VCD, (b) recorded video — are
precomputed-content mechanisms with no simulator in the page. Neither can run a stranger's
arbitrary circuit, ever. Under a no-go, the link adapter does not re-plan; it closes. Sharpen
KC-32-4-1 to say that, and the reason to cut at the encoding rather than the link becomes
self-evident: the encoding is the only part of this feature that survives the outcome the
issue itself admits is live.

## What I would file instead

1. **TASK — portable circuit text in `src/jls/core`**: `JLS1:` prefix, FORMAT version,
   declared length + checksum, deflate, base64url; encoder and bounded decoder; round-trip
   and determinism tests over the existing fixtures. `blocked_by: []`. Lands independent of
   CAP-32 and pays for itself in clipboard and CLI sharing alone.
2. **TASK — the demo page consumes it**: paste box first, `#c=` fragment second, decode
   failures legible, zero-network assertion extended from #835 AC-4. `blocked_by: [572, 573]`,
   dies with them without taking (1) down.
3. **REPLAN on #516** amending AC-2's "no user content", and a note on #500 recording that
   the serialization was never what CAP-19 was refused for.

Same outcome, one measurable dependency instead of a total one, and the ceiling stops being
a contract the project has to defend.
