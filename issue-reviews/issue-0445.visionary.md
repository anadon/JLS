# Issue #445: TASK-0034: a memory image rides in an optional, length- and hash-checked binary section instead of consuming the whole 64 MiB text budget as escaped hex
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of the frame machinery, #445 asks for one thing: **a memory's contents
should stop being circuit *text***. Everything else in it — the SHA-256, the
declared length, the deterministic id minter, the mutual-exclusion refusals, the
`imgref` attribute — is scaffolding for one particular answer to *where the bytes
go*. The end is right and the measurements (O3, O4) are the strongest evidence in
the whole issue: JLS writes a 99 MB file and then refuses to read it. That is a
real defect and the project should not ship another release with it.

The reframing is about the seam. #445 cuts a brand-new bespoke binary section
into a file format that **already has two mechanisms for exactly this**, and it
cites neither.

## Missed mechanism 1: `Memory` already has a sidecar, and it is upstream's

`Memory` carries a `String file` attribute — the initialization-file path —
inherited from Poplawski's JLS (`src/jls/elem/Memory.java:22`, `:105-106`), set
at `:416-417`, written **unconditionally on every save** at `:451`, and read at
`initSim` (`:1251-1290`). `test/jls/elem/MemoryModelTest.java:130,142` pins its
round-trip. `riscv/jlsbuild.py:228` emits it on every generated Memory.

O1's claim that "the only way to save a memory image is escaped text in an
attribute" is therefore false as stated: it is the only way to save an image
*inside the document*. The sidecar seam exists, is written by every save, and is
the seam BRIEF.md D15 already ruled for guest images ("File.") and the one #319's
own Open Question 4 recommends ("a sidecar referenced by content hash for
payloads that are derivable from a pinned recipe").

That seam is in bad shape, and *that* is the task hiding inside this one:

- **It zero-fills on failure.** `:1280-1291`: unreadable file → "all zeros
  assumed". This is precisely the failure mode #445 rightly refuses for `imgref`
  (P6, §7.11) — except it is shipping today, in the mechanism the maintainer has
  already chosen.
- **It is unhardened.** `:1257-1260` does `int length = (int) file.length()`,
  allocates `char[length]`, and ignores `read`'s return value. Truncating cast,
  unbounded allocation, short-read garbage. #38's hardening never reached here
  because the read happens at `initSim`, not at load.
- **It is undocumented.** `docs/file-format.md:308` (the row O7 proposes to
  extend) lists `init`, `initrle`, `sync` — and not `file`. The normative spec
  omits an attribute every writer emits.
- **It has no digest and no path policy.** A `.jls` can name any absolute path
  on the opener's machine and pull its bytes into a displayable memory.

Hardening `file` — digest, resolution relative to the circuit file, a size bound,
refusal instead of zero-fill — delivers #445's capability (`Memory.save` emits
O(1) text for an image of any size) with **no new section kind, no new frame, no
`FORMAT` epoch, and no dependency on the unfiled TASK-0033 or on #319's
`blocked_by: [334] → [315]` chain**. And it is the only option on the table with
*zero* silent-drop exposure: `file` is JLS 4.1's own attribute, so a file-backed
memory round-trips through the 4.1 reader correctly, where `initrle`, `sync`, and
now `imgref` each add another row to the §9 caveat list (O6).

## Missed mechanism 2: the container is already multi-member

`FileAbstractor.readZip` (`src/jls/FileAbstractor.java:295-308`) opens the file
as a zip and takes `archive.getEntry("JLSCircuit")` — **every other member is
already ignored**. The zip container is in the `.jls` lineage, documented in the
README, and read by upstream 4.1 the same way.

So if bytes must be *inside* the file, the elegant route is a container member,
not an invented frame. A zip carrying `JLSCircuit` plus `image/<id>` gives, for
free and with no new spec text:

- the section id (the entry name),
- the declared length (the entry's uncompressed size in the central directory),
- an integrity check (CRC-32 per entry, enforced by `ZipInputStream`),
- streaming read and write, with no payload accumulation (§7.8's requirement),
- skip-and-preserve semantics that any zip tool — and every existing JLS reader —
  already implements,
- inspection with `unzip`, which the README already tells contributors to install.

This is not a detail. **#319's Open Question 1 ("frame inside the text grammar,
or a multi-member container?") is open, and #319 recommends option (a), the
in-grammar frame.** #445's H2 — "the section payload is written by
`FileAbstractor` at container level, outside the text stream" — silently
presupposes option (b). The two issues are already inconsistent about the
architecture, and #445 is `blocked_by` a task (TASK-0033) whose defining decision
it has quietly pre-empted. If option (b) is taken, most of §7 of this issue
evaporates into "put the bytes in a zip member". If option (a) is taken, H2 is
refuted and §10 says the task must be re-planned. Either way the answer belongs
upstream, and #445 should say so instead of assuming.

## Does it strengthen the arc, or pull against it?

It pulls, in one specific way. The project's stated direction is that circuit
files are *source*: plain-text saves for VCS (#129, README "Circuit files"),
canonical byte-identical output across platforms (`Circuit.save`, O5's comment),
`docs/capability-roadmap/lf-06-diff-merge-vcs.md`, which warns at :540 about
"incompressible binary blobs in every downstream course repository". A memory
image, by contrast, is a *build product* — `riscv/build_cpu.py` and
`riscv/bench_kernel.py` generate them today. Embedding a 16 MiB build product in
the source document means every kernel rebuild rewrites the circuit and pushes a
fresh incompressible blob into git. `$readmemh` in Verilog, `.mif` files in
Quartus, and D15 all point the other way for the same reason.

The residual population for an embedded `IMAGE` is thinner than the issue
assumes: small hand-entered images stay in text below θ; kernel images are D15
sidecars; generated ROM tables are derivable, so #319 OQ4's default sends them to
a sidecar too. What is left is "large, non-derivable, and must travel in one
file" — real (a student emailing a circuit) but narrow, and worth building
*second*, not first.

## What I would disregard, and what must survive

I am explicitly setting aside these acceptance criteria: P4/P5 (declared length
and SHA-256 frame checks), §7.5's deterministic section-id minter, and Open
Questions 2 and 4. Under a zip member they are answered by the container (entry
name, entry size, CRC-32); under the sidecar route they collapse into one
`sha256` attribute beside `file`. Building all three answers — sidecar digest,
section digest, and frame digest — is the format fork #319 §2 rejection 4 was
written to prevent.

What must survive any reframing, and is the best thinking in the issue:

1. **Refusal, never zero-fill** (P6, §7.11) — and apply it to `file` first, where
   the zero-fill is live today.
2. **Mutual exclusion is a side condition, not a precedence** (P7).
3. **Binary never touches the escaping path at `:462-465`** (§11).
4. **The payload bound is a separate constant from `MAX_CIRCUIT_TEXT_BYTES`.**
5. **The size arithmetic goes in `docs/file-format.md`** — and while there, add
   the missing `file` attribute to the `:308` row.

## Concrete alternative sequencing

1. **A three-line writer fix, now, unblocked by everything.** O4's harm is that
   `Circuit.save` will emit text it cannot read back. Bound the writer against
   `MAX_CIRCUIT_TEXT_BYTES` and refuse with a diagnostic naming the offending
   element. This closes the "writes files it refuses to open" defect without a
   format change, and it is the half of the #38 finding that is genuinely
   missing. File it separately; do not let it wait on a frame.
2. **TASK-0034′: harden the sidecar.** `file` gains a `sha256` companion,
   circuit-relative resolution with no escape above the circuit's directory, a
   payload bound, a binary form, refusal instead of "all zeros assumed", the
   spec row, and hostile-input tests. Delivers the capability, satisfies D15,
   serves #202 and the `riscv/` toolchain as they already work, and needs no
   frame at all.
3. **Answer #319 OQ1 as "multi-member container" and then re-scope this issue**
   to the residual self-contained case as a zip member — at which point it is
   roughly a day of work, not 1.5 weeks, and TASK-0033 is no longer its
   prerequisite.

The end #445 names is right and the evidence for it is excellent. The mechanism
should be the one the format already has.
