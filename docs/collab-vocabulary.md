# The collaboration payload vocabulary (issue #170)

Normative contract for what a byte arriving from a session peer may
*mean*. Versioned with the protocol: any change to this contract is a
protocol version change. The transport that carries these payloads is
Stage 1a's (#168); session lifecycle payloads are Stage 1b's (#169);
this document owns content.

The design rule, from the #163 threat model (research doc
`docs/collaborative-editing-research.md` §6): **the remote surface is
a closed, data-only vocabulary**. Every network-delivered payload
either parses to one of the meanings below or dies as a typed
rejection. There is no best-effort repair, no extension point, no
escape to code.

## Payload kinds

1. **Operations** — the `jls.collab.op` grammar, exactly as
   `CircuitOpReader` accepts it (`docs/operation-layer.md` has the
   grammar). The op vocabulary is the sealed `CircuitOp` interface;
   unknown kinds, unknown fields, malformed values, duplicated
   fields, and oversized blocks are rejections (`OpRejected`).
   Elements are addressed by stable id (#165) only — never by object
   reference, index, or class name.
2. **Snapshots** (Stage 1b) — a circuit in the textual save format
   (`docs/file-format.md`). Before any element type token from a
   snapshot reaches the reflective instantiation in `Circuit.load`,
   it must pass `jls.collab.op.ElementVocabulary.requireAllowed`:
   the closed element-type allowlist. The local file-open path is
   unchanged — a file the user chose is gated by package prefix,
   `Element` subclass, and `(Circuit)` constructor checks — but a
   peer is not the user, and gets the strictly tighter list.

## The element-type allowlist

`ElementVocabulary` is the list: every palette-creatable element
(pinned against the help system by `HelpTopicsTest`) plus `WireEnd`,
the one non-palette token the save format writes (wires are rebuilt
from WireEnd references; `Wire` is never an `ELEMENT` token).
`ElementVocabularyTest` mechanically cross-checks the list against
both the save-format writer literals in `jls.elem` and the palette
contract, and pins the P1 witness: `TestGen` passes every check the
reflective loader applies and is still rejected, because it is not a
token legitimate payloads contain.

The list is the stopgap constant sanctioned by the #170 plan until
the element registry (#78) exists; when the registry lands,
`ElementVocabulary` delegates to it and the reconciliation is
recorded on #78.

## Explicit prohibitions

Payloads may never contain, and receivers may never honor:

- **file system paths** — snapshots and ops describe circuit model
  state only; nothing a peer sends selects a file;
- **class names outside the allowlist** — no token a peer supplies
  may reach `Class.forName` (or any class selection) except through
  `ElementVocabulary`;
- **code** — no scripts, expressions, or serialized objects; Java
  object serialization (`ObjectInputStream`/`ObjectOutputStream`) is
  banned repo-wide;
- **settings** — a peer cannot change the receiver's configuration,
  preferences, or environment.

Enforcement is structural, not procedural, and the build gates it:

| Prohibition | Ratchet |
| --- | --- |
| No Java object serialization, anywhere, ever | `CollabSecurityRatchetTest.javaObjectSerializationIsBannedEverywhere` (source text) + `ArchitectureRulesTest.nothingUsesJavaObjectSerializationStreams` (bytecode) |
| Sockets only in `jls.collab.net` (so batch/GUI starts bind no listener — P4) | `CollabSecurityRatchetTest.socketsAppearOnlyUnderCollabNet` + `ArchitectureRulesTest.socketEndpointsAreConfinedToCollabNet` |
| No reflection in `jls.collab`; `Class.forName` pinned to its three pre-collab sites | `CollabSecurityRatchetTest.collabDoesNoReflection`, `.classForNameStaysAtItsPinnedSites` + `ArchitectureRulesTest.collabDependsOnNoReflection` |
| Allowlist correctness (both directions) | `ElementVocabularyTest` (writer-set equality, palette cross-check, P1 witness, typed rejections) |

## Caps on remote input (#38 taxonomy, extended)

Per-payload caps ship with the parser: `CircuitOpReader` bounds ids
per op (10,000), string length (10,000), and lines per block. The
save-format loader's #38 caps bound snapshots. Streaming caps —
frame size, per-peer rate, op backlog, element-count ceilings — are
new surface that lands with the Stage 1a frame layer, wired into
frame handling with typed rejection; thresholds will be documented
here and tested at boundary values when that layer exists.

## Peer misbehavior (planned, with Stage 1a)

A rejection is attributable: rejections are counted per peer, a
threshold disconnects, and the session log records the event.
Vandalism *within* the vocabulary (valid ops that wreck the circuit)
is a recovery problem, not a prevention problem — revert and eject
belong to Stage 1b/2 (see SECURITY.md).
