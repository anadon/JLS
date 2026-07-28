# §13 Second-Reader Review: `jls.collab.net` Handshake (AES-256-GCM form)

Issue #163 / #168 Completion Criterion §13 requires the collab-transport
handshake to be reviewed against its published pattern by a second reader
before merge. This document is that review. It reviews the construction as
it stands on master (tip `19b1448`) in its **AES-256-GCM** cipher form (the
cipher swap landed via PR #194, commit `9114898`; see issue #168 comment
5013310070). The full protocol spec is the class javadoc of
`src/jls/collab/net/Handshake.java`.

- **Reviewer role:** second reader (the transport was written by another
  agent in an earlier cycle; this is an independent read).
- **Published pattern reviewed against:** TLS 1.3 with raw public keys /
  the SIGMA "sign-and-MAC" construction that TLS 1.3 instantiates
  (ephemeral (EC)DHE, `CertificateVerify`-shape identity signatures over the
  transcript, `Finished` MACs for key confirmation), plus the TLS 1.3 /
  Noise sequence-number nonce for the AEAD.
- **Scope:** transcript-binding completeness, the CertificateVerify-shape
  signatures and Finished MACs, the key schedule (per-direction application
  keys and the SAS from the final hash), and — the AES-GCM-specific risk —
  nonce management (no nonce reuse under any key).
- **Verdict: CLEAN.** No defect found; no change to `Handshake.java` or
  `Crypto.java`. Evidence with exact citations below.

The protocol is three messages. The joiner is the **initiator**, the session
starter is the **responder**.

```
m1  initiator -> responder : MAGIC, version, 32B random, X25519 ephemeral (DER)
m2  responder -> initiator : version, 16B session id, 32B random, X25519
                             ephemeral (DER); then AEAD-sealed under the
                             responder handshake key: responder identity key,
                             display name, signature over "verify responder"
                             + transcript hash, Finished MAC over transcript
m3  initiator -> responder : AEAD-sealed under the initiator handshake key:
                             initiator identity key, display name, signature
                             over "verify initiator" + transcript hash,
                             Finished MAC over transcript
```

---

## 1. Transcript-binding completeness

**Claim reviewed:** each field is appended to the running transcript exactly
once, in the same order, on both sides, so the two transcripts are
byte-identical and every derived key/signature/MAC binds the whole exchange.

The transcript is a single append-only `ByteArrayOutputStream`
(`Handshake.java:138`); `transcriptHash()` is SHA-256 of its current contents
(`Handshake.java:666-669`). Tracing both sides:

| Step | Initiator writes | Responder writes |
|------|------------------|------------------|
| m1 | full m1 message, in `firstMessage()` (`:233`) | full received m1, in `acceptFirst()` (`:273`) |
| m2 clear | received clear prefix, `transcript.write(m2, 0, clearLength)` (`:351`) | built `clearPart`, `transcript.writeBytes(clearPart)` (`:291`) |
| responder auth | identityDer, name, signature, finished — appended in `verifyAuth(..., false)` (`:522, :523, :531, :541`) | same four, appended in `buildAuth(false)` (`:443, :444, :447, :453`) |
| initiator auth | identityDer, name, signature, finished — appended in `buildAuth(true)` (`:443, :444, :447, :453`) | same four, appended in `verifyAuth(..., true)` (`:522, :523, :531, :541`) |

Key points confirmed:

- **m1 identical on both sides.** The initiator writes the exact bytes it put
  on the wire; the responder writes the exact bytes it received. Same bytes.
- **m2 clear prefix identical.** The responder appends the `clearPart` it
  builds (`:283-291`); the initiator appends exactly `clearLength` bytes,
  where `clearLength = reply.position()` after parsing version + session id +
  random + length-prefixed ephemeral (`:333-347`) — i.e. precisely the clear
  prefix, not the sealed part. The sealed part is deliberately **excluded**
  from the transcript (it is redundant: its plaintext is appended field by
  field via `verifyAuth`, and appending the ciphertext too would just bind the
  same content under the AEAD nonce, adding nothing). This matches the TLS 1.3
  transcript, which hashes handshake message *contents*, not record-layer
  encryption.
- **Auth fields appended in lockstep.** `buildAuth` appends identityDer and
  name, then signs against the hash *at that point*, appends the signature,
  computes the Finished MAC against the new hash, appends it. `verifyAuth`
  mirrors exactly: it appends identityDer and name, verifies the signature
  against the same hash, appends the signature, verifies the Finished MAC
  against the same hash, appends it. Because appends and hash-reads interleave
  identically, the signer's and verifier's hashes coincide at each step.
- **Exactly once.** No field is appended on more than one code path per side;
  each row above is the sole writer of that field on that side.

**Result: complete and mirrored.** The two transcripts are byte-identical at
every derivation point. Any tamper to any field diverges the hash on at least
one side, which propagates into every later key, signature, and MAC — and into
the SAS (§3). This is the property H1 requires and the tamper-every-byte
property test (518 trials, 100% detected, per issue #168 comment 5013111304)
exercises.

## 2. CertificateVerify-shape signatures and Finished MACs

**Signatures.** Each side signs `role-label || transcriptHash` with its
long-term Ed25519 identity key (`signaturePayload`, `:648-659`; signing at
`:445-446`). The hash covers the full transcript to that point, which includes
**both** ephemerals, both randoms, the session id, and the signer's own
identity key and name (appended just before signing). Binding the identity
signature over a transcript that includes the peer's ephemeral is exactly the
TLS 1.3 `CertificateVerify` shape and is what defeats a key-compromise /
unknown-key-share reflection: the signature is meaningless outside this exact
session.

- **Role separation.** The initiator signs under `"JLSCOLLAB1 verify
  initiator"` and the responder under `"JLSCOLLAB1 verify responder"`
  (`:651-652`). Distinct, ASCII, domain-prefixed labels prevent a signature
  produced in one role from being replayed as the other role's signature.
- **Verification uses the *claimed* key from the same message**
  (`parseIdentity`, `:521`, then `IdentityKey.verify(claimed, ...)` at `:524`),
  and the authenticated key becomes `peerIdentity` only after both the
  signature and the Finished MAC pass (`:542`). The `KnownPeers`
  VERIFIED/UNKNOWN/KEY_CHANGED trust decision (P4) is layered on the
  fingerprint of that authenticated key — correct: the handshake authenticates
  "the holder of this key is live in this session," and trust-on-first-use is a
  separate policy.

**Finished MACs.** Each side computes HMAC-SHA256 over the current transcript
hash under a per-role, per-direction finished key (`buildAuth` `:448-453`;
verified in `verifyAuth` `:532-540`, compared with the constant-time
`MessageDigest.isEqual`). This is the TLS 1.3 `Finished` key-confirmation step:
it proves the peer derived the same handshake secrets (hence completed the same
DH with the same transcript), closing the gap between "signature is valid" and
"peer actually holds the session keys."

**Ordering / downgrade.** `version` is the first field of both m1 and m2, is
checked equal to `VERSION` before anything else (`:262-266`, `:334-338`), and —
critically — is part of the transcript, so a downgrade tamper is caught by the
MAC/signature even if a future multi-version negotiation is added. `MAGIC` is
compared with the constant-time `MessageDigest.isEqual` (`:257`). The `State`
machine (`:849-855`) enforces that each message method runs exactly once, in
order, per role, and any rejection drives the handshake permanently to `FAILED`
(`:308-311, :375-378, :407-410`) — rejection, never repair (#38 discipline).

**Result: correct.** Signatures are transcript-bound CertificateVerify-shape
with domain-separated role labels; Finished MACs provide key confirmation and
use constant-time comparison.

## 3. Key schedule

**Claim reviewed:** handshake keys derive from the post-hello hash; the two
per-direction application keys and the SAS secret derive from the final hash;
directions are assigned consistently by role.

All keys come from `Crypto.hkdf(sharedSecret, label, SALT, hash, length)`
(`Crypto.java:63-84`), HKDF-SHA256 with the DH shared secret as IKM, a fixed
per-version salt (`SALT`, `Handshake.java:84-85`), and the transcript hash
bound into the expand `info` alongside a purpose label (`Crypto.java:66-71`).
Two derivation points:

- **Post-hello** (`deriveHandshakeKeys`, `:620-637`), from `helloHash` = hash
  after m1 + m2-clear: `responderHandshakeKey` ("hs resp"),
  `initiatorHandshakeKey` ("hs init"), `responderFinishedKey` ("fin resp"),
  `initiatorFinishedKey` ("fin init"). Four distinct ASCII labels ⇒ four
  independent 32-byte keys.
- **Final** (`completeLink`, `:550-583`), from `finalHash` = hash after both
  auth blocks: `initiatorToResponder` ("app i2r"), `responderToInitiator`
  ("app r2i"), and the 8-byte `sasSecret` ("sas").

**Direction assignment** (`:576-582`): a link's `sendKey`/`receiveKey` are
`(initiatorToResponder, responderToInitiator)` for the initiator and the swap
for the responder. So one side's send key is the other's receive key, per
direction, with no key shared across directions. `SecureLink`'s constructor
stores them as `sendKey`/`receiveKey` (`SecureLink.java:80-90`) and never
crosses them.

**SAS binding.** The SAS derives from `finalHash`, which covers both identity
keys, both ephemerals, both randoms, and the session id (§1). A MITM cannot
produce matching SAS on both sides without matching the entire transcript,
which the CertificateVerify signatures forbid. This is the H1 property.

Distinctness across the whole schedule is guaranteed by distinct HKDF labels
*and*, between the two derivation points, distinct transcript hashes; no label
is reused, so no two keys collide, and no handshake key is ever reused as an
application key.

**Result: correct.** Per-direction application keys and SAS derive from the
full-transcript final hash; all keys are label- and hash-separated.

## 4. Nonce management (AES-256-GCM-specific)

**This is the review's highest-risk item:** AES-GCM catastrophically fails
(loss of confidentiality *and* authentication) if a (key, nonce) pair is ever
reused. The nonce here is a counter nonce: 4 zero bytes || big-endian 64-bit
counter (`Crypto.nonce`, `:230-236`). Uniqueness therefore reduces to: *is the
counter unique for every use of each key?*

**Handshake phase.** Exactly two AEAD seals occur, each under a **different**
key and each with counter `0`:

- m2 sealed with `responderHandshakeKey`, counter `0` (`:299-300`); opened by
  the initiator with the same key and counter `0` (`openAuth` → `aeadOpen(...,
  0, ...)`, `:359, :481`).
- m3 sealed with `initiatorHandshakeKey`, counter `0` (`:367-368`); opened by
  the responder with the same key and counter `0` (`:404, :481`).

`responderHandshakeKey` and `initiatorHandshakeKey` are distinct HKDF outputs
(§3). Each handshake key seals **exactly one** message. Reusing counter `0` is
safe **because the keys differ** — nonce uniqueness is a per-key property.
There is no third seal under either key anywhere in the class. ✔ no reuse.

**Data phase** (`SecureLink`). `sendCounter` and `receiveCounter` start at 0
and increment strictly by one after every successful seal/open (`seal`
`:157-159`; `openCiphertext` `:273-275`). `sendKey` ≠ `receiveKey` (§3), so the
two counters index two different keys — the send stream and receive stream
never collide even though both start at 0. Within one direction the counter is
strictly increasing, so each (key, counter) — hence each (key, nonce) — pair is
unique. The 64-bit counter cannot realistically wrap (2⁶⁴ frames at a 1 MiB
cap is not reachable), and there is no reset path: the counters are only ever
incremented, never rewound.

Additional AES-GCM hygiene confirmed:

- **Fail-closed on the first bad tag.** Any `AEADBadTagException` poisons the
  link (`poison`, `:331-335`); `requireHealthy` (`:315-321`) then rejects every
  later seal and open. Because the receive counter is **not** advanced on a
  failed open (the increment at `:275` is after the successful `aeadOpen`
  returns), an attacker cannot burn a nonce or desynchronize the counters with
  a forged frame — the link is simply dead. This also gives replay/reorder/drop
  resistance for free: a frame that is not the next expected one decrypts under
  the wrong counter and fails the tag.
- **Tag length pinned.** 128-bit tag (`TAG_BITS = TAG_BYTES * 8 = 128`,
  `Crypto.java:38, 44`), the GCM maximum, applied to both seal and open.
- **AAD domain separation.** Handshake seals bind `"JLSCOLLAB1 m2"` / `"…m3"`
  as AAD (`aad`, `:678-682`); data frames bind `"JLSCOLLAB1 frame"`
  (`SecureLink.java:39-40`). A handshake ciphertext cannot be replayed as a
  data frame (different AAD *and* different key).
- **Over-cap rejection before allocation.** `SecureLink.open` /`openFrame`
  reject an over-cap or under-tag length *before* allocating the ciphertext
  buffer (`SecureLink.java:193-201, 230-238`), so a hostile length prefix
  cannot exhaust memory (#38 discipline; P2).

**Result: no nonce reuse is possible** under any key, in either phase. The
counter-nonce construction is the standard TLS 1.3 / Noise sequence-number
nonce and is used correctly for AES-256-GCM.

---

## Findings

None. The construction is a faithful TLS-1.3-RPK / SIGMA instantiation over
JDK primitives:

1. Transcript binding is complete, mirrored, and appended-exactly-once on both
   sides (§1).
2. Identity signatures are transcript-bound CertificateVerify-shape with
   domain-separated role labels, and Finished MACs give key confirmation with
   constant-time comparison (§2).
3. The key schedule derives handshake keys from the post-hello hash and
   per-direction application keys plus the SAS from the full-transcript final
   hash, all label- and hash-separated (§3).
4. Nonce management is safe: each handshake key seals exactly one message
   (distinct keys, counter 0), and `SecureLink` uses a strictly-increasing
   per-direction counter nonce over per-direction keys — no (key, nonce) pair
   can ever repeat (§4).

No change to `Handshake.java` or `Crypto.java` is warranted. The runtime
listener-hygiene half of P3 is pinned separately by
`test/jls/BootListenerHygieneTest.java`; the structural confinement is pinned
by `SocketConfinementRatchetTest` and
`ArchitectureRulesTest.socketEndpointsAreConfinedToCollabNet`.

**Reviewed against pattern:** TLS 1.3 raw-public-key / SIGMA. **Verdict:
CLEAN.**
