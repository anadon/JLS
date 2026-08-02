# Diff and merge stability of the saved file format

Integration of four independent measured analyses
(`diff-vcs-reality.md`, `diff-identity-and-refs.md`,
`diff-blobs-and-container.md`, `diff-merge-and-review.md`), with the
load-bearing numbers re-run by the integrator against HEAD `803d716`.

**Integrator re-verification (this session, independent of all four angles):**

```
$ JLS_REPLICA_ID=56fa4130aaaaaaaaaaaaaaaaaaaaaaaa Churn2 test/fixtures/riscv-sum1to10.jls add
  baseLines=10744 modLines=10754  changed=5312
$ JLS_REPLICA_ID=zzzzzzzzzzzz             Churn2 test/fixtures/riscv-sum1to10.jls add
  baseLines=10744 modLines=10754  changed=10
$ JLS_REPLICA_ID=lab Collide collide.jls out.jls
  newSid=lab:405
  reloadOfOwnSave=false error=invalid element: two elements declare the same stable id 'lab:405'
  (grep -c 'String sid "lab:405"' out.jls  ->  2)
$ git merge-file -p tail-insert base front-insert  ->  exit 0, 0 conflict markers
  1040 ELEMENT blocks, 1040 `int id` lines but only 1039 distinct
  LoadCheck: finishLoad=false, ClassCastException AndGate->WireEnd at WireEnd.java:133,
  reported to the user as "The file may be truncated - recover from the .jls~ checkpoint"
$ od -An -tx1 -N6 on all four `git ls-files '*.jls'`
  46 4f 52 4d 41 54 ("FORMAT") x2, 43 49 52 43 55 49 ("CIRCUI") x2 — zero XZ
```

All four headline claims reproduce. **One cross-angle contradiction resolved
against Angle 3:** `diff-blobs-and-container.md` §1.2 states "Two are plain
text, two are XZ. Confirmed." That is wrong — the magic-byte check above shows
all four tracked `.jls` files are plain text. Angles 1, 2 and 4 agree with the
integrator. Angle 3's other measurements are unaffected (none depend on it).

---

## 1. The direct answer

> **What about diff stability on the saved file format?**

**Today, the format has determinism but not locality, and that is the wrong
one of the two.** Canonical order (#166) is genuinely delivered: identical
content saves byte-identically regardless of edit history, and
`Circuit.stateHash()` (`src/jls/Circuit.java:1548-1569`) is SHA-256 of exactly
those bytes. But #166 shipped only half of its own design. The half that
shipped sorts blocks by stable id (`Circuit.java:1494-1496`). The half that did
not is recorded verbatim at `docs/collaborative-editing-research.md:175` —
*"saves emit elements sorted by stable element id … and `ref` lines use stable
ids."* The shipped writer does the first clause and not the second.

So references are still the dense save-time index
(`src/jls/elem/Element.java:21-22`: *"The file-local reference index, reassigned
on every save"*), handed out `0..n-1` in canonical order at
`Circuit.java:1499-1503` and emitted by `WireEnd.save`
(`src/jls/elem/WireEnd.java:606,611`). The consequence is that the id of every
element is a pure function of **every other element's identity**. That is
determinism without locality, and determinism without locality is what turns a
one-hunk edit into a 234-hunk diff.

### What a user actually gets in git right now

Five measured facts, in the order they bite.

**1. Adding one element to a shipped in-repo fixture rewrites a quarter of the
file.** `test/fixtures/riscv-sum1to10.jls`, 1,038 element blocks, 10,744
canonical lines. Insert one `AndGate`: **5,312 changed lines in 234 hunks**,
where the correct answer is 10 lines in 1 hunk. **531x line amplification,
234x hunk amplification.** Composition: 2,023 `int id` lines, 3,204 `ref`
lines, plus 53 `String sid` lines that did not actually change — the uniform
renumbering defeated diff's block-alignment heuristic, so the delivered diff is
*worse* than the true minimal edit script.

**2. It is not a worst case. It is the only case, for every file that exists.**
`ElementId.compareTo` (`src/jls/elem/ElementId.java:278-285`) orders by replica
*string* first. A fresh install draws 32 hex digits
(`ElementId.java:124-125`, `UUID.randomUUID().toString().replace("-","")`), so
its replica is drawn from `[0-9a-f]`. The reserved legacy replica is the
literal `"legacy"` (`ElementId.java:38`), and `'f' < 'l'`. **Every element any
install ever adds to any pre-#165 file sorts to position 0 and renumbers the
entire file.** All four tracked `.jls` files carry `legacy:N` sids. Angle 4
measured a real clean-install draw (`913aa3d93b2649a6addc2c1547f21340`) and
confirmed it. The roadmap doc's estimate of "roughly five sixths of new
installs" (`docs/capability-roadmap/lf-06-diff-merge-vcs.md:104-107`) is
optimistic; it is 100%, by construction. For a *second author* on a
sid-bearing file it is a genuine coin flip decided by a random hex draw —
measured at 234 of 530 lines on the 52-block `riscv/gui/cpu.jls`.

**3. A three-way merge does not conflict — it silently corrupts.** The
integrator reproduced it above: two independent one-element additions,
`git merge-file` exit 0, **zero conflict markers**, 1,040 blocks, one duplicate
`int id`, and JLS then refuses the result with *"The file may be truncated —
recover from the `.jls~` checkpoint"*. Angle 4 built two worse cases in real
git repositories: one merged cleanly and wired a pin to `spare1` instead of
`spare2`; the other merged cleanly, **loaded, and simulated**, printing
`Output Pin out: 0xFFF (4095 unsigned, -4081 signed)` on a pin the file
declares as 4 bits — a circuit `SimpleEditor.canConnect`
(`src/jls/edit/SimpleEditor.java:4245-4250`, "Bits don't match") would not let
anyone draw. A third measured merge case conflicts on the `sid` line *only*,
because two added `AndGate` blocks are otherwise line-identical; resolving it
either way silently discards one author's element.

**4. JLS today writes files it then refuses to open.** Reproduced above.
`ElementId.mintFresh` (`ElementId.java:210-213`) draws from `NEXT_COUNTER`, a
process-wide `AtomicLong` starting at 0 every JVM (`ElementId.java:60`), and
`ElementId.parse` does not advance it. One load, one add, one save on a circuit
with any hole in its counter sequence writes a duplicate `sid`;
`Circuit.finishLoad` (`Circuit.java:1309-1319`) rejects it, as
`docs/file-format.md:392-394` requires. `CircuitSnapshot` stores save-format
text (`src/jls/edit/CircuitSnapshot.java:19-27`), so the same collision poisons
the undo stack. Worse for any future tooling: Angle 4 measured **two branches
of the same install, from the same parent file, minting the identical id**
(`r:39` on the fork fixture, `r:1551` on riscv-sum1to10) — the ordinary
`git checkout -b` workflow, which `lf-06:395-397` declares impossible.

**5. Only then does the container matter — and it matters a lot.** XZ's delta
behaviour is a cliff, not a slope: LZMA2's output prefix is a pure function of
its input prefix, so the compressed stream is byte-identical up to the first
changed byte and uncorrelated after it. A **2-byte** text change at offset 226
makes **97.1%** of the compressed bytes differ. Over 100 revisions of the
1,038-block fixture with the commonest edit (nudge one element): XZ costs
**7,191 B/rev with 0/100 deltas**, plain text **482 B/rev at default gc** and
**48.8 B/rev after `git gc --aggressive`** — 15x and 147x. At 100k elements the
gap is ~194 KB/rev vs 618 B/rev. `git gc --aggressive` is load-bearing for
plain text and changes the XZ case by literally nothing; users get the default.
And on a 16 MiB memory image the container is not even the problem: one word
changed produces **51,223,498 bytes of `git diff`**, because
`Memory.save` (`src/jls/elem/Memory.java:455-465`) writes the whole image as
one quoted line, and `docs/file-format.md` §2 makes that normative.

### The sting in the tail

**Uncompressing the container without fixing `int id` makes repo bloat
worse.** Measured, front-insert case (i.e. the guaranteed legacy case), 100
revisions, default gc: plain text **11,003 B/rev**, XZ **7,531 B/rev**. 93.7%
churn on a 150 KB text file exceeds a full copy of a 7 KB blob. The two fixes
are not independent and the id fix is the prerequisite.

**And D1 alone is the single worst cell in the safety matrix.** XZ always
conflicts on merge — safe, if useless. Plain text with dense ids removes that
guard and keeps the renumbering hazard, which is exactly the configuration that
produced the clean-but-wrong merges. Angle 4's table:

| container / format | independent edits | quiet edits |
|---|---|---|
| XZ (today) | conflict (binary) | conflict (binary) |
| plain text, dense ids (**D1 alone**) | conflict, 3 hunks | **clean merge, silently wrong** |
| plain text, sid refs (D1+D2) | clean, correct | clean, correct |
| plain text, sid refs, **no validator** | — | bit-width and name collisions still wrong, **and now reachable more often** |

Fixing diff stability *removes* the accidental conflict protection that
renumbering churn currently provides. That inverts the roadmap's ordering: the
semantic validator must ship **before** the format fix, not five to eight weeks
after it inside C4.

### What must change

In one sentence: **stop writing any identifier that is computed at save time,
make the reader enforce every invariant the editor enforces, and treat the
compressed container as transport rather than as the file.**

Everything else in this document is detail on those three.

---

## 2. Ranked requirements for the successor format

Ranked by measured pain removed per unit of design cost. Each is traceable to a
measurement.

**R1 — No identifier anywhere in the format is assigned at save time. Every
reference carries a stable id.** *(fatal; the root cause of every other diff
and merge failure below)*
Wire attach, wire segment, probe target, subcircuit port binding, definition
reference — all stable ids, never a positional index, never "position in the
preceding list". **Measured: 2,651/2,661 lines in 234 hunks → 0/9 lines in 1
hunk on the shipped fixture (295x lines, 234x hunks); 622/650 lines in 100
hunks → 1/26 in 2 hunks for a realistic wired splice.** The sid-referencing
file is also 9.7% *shorter* (10,744 → 9,706 lines) because ` int id` disappears
— identity is already carried by the `sid` line every element writes anyway.
Size objection does not survive: sid refs are +2.8% raw and **−13.7%
compressed** (`legacy:` sids), +29.3% raw and −4.3% compressed (32-hex
replicas), because dense incrementing ints are near-maximum-entropy for a
dictionary coder while sids repeat a modellable replica prefix. Performance
objection is factually wrong about the baseline: `Circuit.java:84` is already
`HashMap<Integer, Element>` with autoboxing; measured cost is **+3.6 to +4.3 ns
per reference**, i.e. **+5.3 ms at the 695k-element load cap** — 0.03% of a
load. This requirement also deletes the workaround the format forced on the
collaboration layer (`docs/operation-layer.md:82-86`) and unblocks the
`AddElements` op kinds that must currently reject wire/wire-end/subcircuit
blocks (`docs/operation-layer.md:60-63`).

**R2 — Stable-id minting must be collision-proof across loads and across
branches.** *(fatal; JLS writes files it refuses to load, reproduced)*
`ElementId.parse` must advance the process counter, minting must be seeded from
`max(counter)` per replica over the loaded circuit **and** from a persisted
per-install counter, and `Circuit.save` must assert `sid` uniqueness before
writing. **Measured: `newSid=lab:405` into a circuit already containing
`lab:405`; the saved file has two identical `sid` lines; reload fails.**
Separately measured: **two branches of one install mint `r:1551` for two
different elements** — so `(path, sid)` as a differ/merger primary key is
unsound for *current* files, not only legacy ones. And a secondary consequence
threatens the convergence oracle itself: `Circuit.elements` is a `HashSet`
(`Circuit.java:48`) and `List.sort` is stable, so two elements sharing a sid
have their relative save order decided by hash iteration order — which would
make the canonical bytes, and therefore `stateHash()`, stop being a pure
function of content.

**R3 — The reader must enforce every invariant the editor enforces, and the
spec must declare which those are.** *(fatal; R1 makes this *more* urgent, not
less)*
`Circuit.finishLoad` + `WireEnd.init` enforce **referential** integrity — refs
resolve (`WireEnd.java:107`), the named put exists (`:112`), puts are singly
attached (`:122`), sids are unique (`Circuit.java:1309-1319`). They enforce
**no semantic** integrity. **Measured: 4 of 6 invariant classes fail loudly, 2
fail silently — and the two silent ones are exactly the merge hazards.** Net
bit-width disagreement loads *and simulates* (`0xFFF` on a 4-bit pin);
duplicate element names load silently from any source. Both checks already
exist on the editor path (`SimpleEditor.java:4231-4265`) and in the op layer's
paste rules (`docs/operation-layer.md:58-64`). Any invariant enforced only by
the editor is a silent-merge hazard by construction, and a merged file enters
through the load path, bypassing all of them.

**R4 — Sort keys must be immutable, and no reserved identity may sort after
every real one.** *(fatal for legacy files; one comparator)*
**Measured: `sid` and `(tag, sid)` are identical on every diff metric (insert
0/9 lines 1 hunk; rename 1/1 lines 1 hunk); `(name, sid)` turns a one-line
rename into a 14-line, 2-hunk block move** — a move/edit conflict in a
three-way merge. Never `name`, never geometry, never anything a dialog can
change. Prefer `(tag, sid)`: identical on diffs, dramatically more readable
(pure sid interleaves WireEnds with logic elements by accident of drawing
order), and it subsumes the existing `isWire ? 1 : 0` special case at
`Circuit.java:1495` into the general rule. Fix the ordering trap with a
per-file **replica alias table** (`REPLICA 0 "legacy"`, `REPLICA 1 "deadbeef…"`,
sids written `0:41`) ordered by first appearance: this makes `legacy` alias 0,
converting the guaranteed worst case into the best case for 3 of 4 tracked
files, *and* recovers the entire 29.3% raw-size penalty of 32-hex replicas.
**Measured: same file, same edit — replica sorting before `legacy` = 5,312
lines; replica sorting after = 10 lines.**

**R5 — The uncompressed canonical text is normative for hashing, diffing,
merging and convergence. A container is transport and never enters the file's
identity.** *(major; already half-true, must be stated as a rule)*
`Circuit.stateHash()` already hashes the save text, not the container bytes.
Write it down so no future container decision can perturb identity. Corollary:
**the canonical form must not depend on the file's name.** `-savetext` fails
this today — `JLSStart.java:495-505` does `circ.setName(textSaveName)`, so
`jls -savetext st1.jls rv.jls` emits `CIRCUIT st1` where the source says
`CIRCUIT riscv`. A git clean filter built on it would rewrite the `CIRCUIT`
line to a mktemp stem on every commit.

**R6 — No bulk binary payload in the structural text section; blobs are
content-addressed, separately stored, and validated on load.** *(fatal for the
memory/kernel-image goal)*
**Measured: one word changed in a 16 MiB image produces 51,223,498 B of
`git diff` today, and two disjoint edits to that image always CONFLICT** (and
the conflicted file doubles, because both whole lines are written between the
markers). Also **measured: a single 16 MiB high-entropy image as escaped `init`
is 62,542,822 B = 93.2% of `MAX_CIRCUIT_TEXT_BYTES`** (`FileAbstractor.java:65`)
— one memory element from binding. Blobs referenced by `sha256 + length`,
stored as STORED (uncompressed) container members, and **rejected on load if
length or hash disagrees** with the structure section's declaration. Content
addressing also dedups identical images across instances (the register-file
mirroring trick ships the same image twice) and makes the reference *be*
content equality, which is what keeps the convergence oracle sound.

**R7 — Any inline text blob must be positionally independent: line *k* covers
a range determined by *k* alone.** *(major; the obvious fix does not work)*
**Measured: keeping `initrle` but breaking it every 64 tokens changed 16,387
lines and produced 10,504,749 B of diff for one word flipped inside a zero
band, because splitting a run reflows every subsequent line.** Fixed-size
chunks have positional independence by construction; variable-length token
streams do not. Recommended encoding: **sparse address-anchored base64, 64
words per line** (256 B at 32 bits, word-aligned for any `Memory.bits` when
words are padded to `ceil(bits/8)`), absolute hex address prefix, all-default
lines omitted. **Measured: 1.03x raw over a 16 MiB address space, 2 changed
lines, ~2 KB of diff per one-word edit.** Chunk size sets diff cost linearly
(`~8 × ceil(C/3) × 4` bytes under `diff -u`); going from 3072 B/line to
192 B/line costs 2.9% file size and buys a 14.6x smaller diff. Cap inline blobs
at ~64 KiB; larger ones become members. Keep `initrle`'s *absolute addressing*
idea, discard its *stream framing*.

**R8 — Simulation checkpoints are derived content: separate, optional,
excluded from `stateHash`, and `.gitignore`-able.** *(major)*
A checkpoint of the ~600-element machine after a Linux boot carries the full
16 MiB RAM plus register and event-queue state — larger than everything else
combined, regenerated on every run, reviewed by nobody, and losing it costs a
re-run rather than data. Structure is a pure function of content; a checkpoint
is a function of a *run*; they must not share a hash. The cautionary precedent
is in-tree: `.jls~` autosave fires every 10 edits
(`src/jls/JLSInfo.java:65`, `SimpleEditor.java:5388`), in the XZ container, and
**is not gitignored** while every `.jls` is — measured, `foo.jls` ignored,
`foo.jls~` untracked-and-showing.

**R9 — Subcircuit definitions are top-level sections referenced by a
definition-level stable id; instances carry `ref def <did>` plus per-instance
parameter bindings.** *(major; required by D4 anyway)*
`SubCircuit.save` (`src/jls/elem/SubCircuit.java:283-289`) inlines the entire
nested circuit into every instance body. **Measured: 8 instances of one
22-element definition, change one attribute in the definition → 8 lines
removed, 8 added, in 8 SEPARATE HUNKS, in a 4,907-line file of which ~99.4% is
duplicated definition text.** And because each inlined block has its own id
space, R1's churn multiplies by the instance count. Definitions ordered by
`did`; nesting by reference forming a DAG with a cycle check at load;
serialized size becomes O(distinct definitions) rather than O(instances).

**R10 — Per-section versioning is declared per section *kind* in the header,
and each section declares whether it participates in merge.** *(major; the best
part of the .jlsx proposal)*
A version integer repeated in every one of N definition sections makes a format
bump an N-line diff. Declare `SECTION elements 3`, `SECTION defs 1` once.
Add **merge-participation** as a first-class per-section property alongside
must-understand/optional: structural content is diffed and merged; blobs and
checkpoints are **hashed but never merged** — a merge driver resolves them to
one side with a recorded conflict. A three-way line merge of a memory image is
the worst case in the entire design space. Replace the single whole-file
`stateHash` with per-section hashes plus a hash over the section-hash list;
this is what per-section versioning wants anyway, and it lets a peer sync one
changed definition instead of a whole circuit. `docs/reproducibility.md` and
`DeterministicSaveTest` need a matching statement.

**R11 — If compression is used at all, frames must be independent: an unchanged
section compresses to byte-identical output whatever changed elsewhere.**
*(moderate)*
**Measured: a 1-byte text change leaves only 26 of 2,108 XZ bytes identical
(1.2% common prefix); a 2-byte change at offset 226 makes 97.1% of the
compressed stream differ; git finds 0/100 deltas across 100 such revisions at
default *and* aggressive gc.** Framing measurably helps: `zstd --rsyncable`
cut a 10-revision repo from 40,114,706 B to 10,571,354 B (3.8x) at a 0.1%
file-size cost, and `xz --block-size=1MiB` cuts rsync literal data 17x for 4.8%
size. Document precisely what this does and does not buy: it restores git
**delta** and restores **zero** textual diff and **zero** mergeability.

**R12 — The writer must enforce every invariant the reader enforces.** *(major)*
Two write-then-refuse asymmetries already exist at HEAD: duplicate `sid`
(reproduced above) and the 64 MiB save-unbounded / load-capped gap
(`FileAbstractor.java:65`). A sectioned format with blobs and checkpoints has
far more invariants to violate. A save→immediately-reload assertion should be a
**format-level requirement**, not a test convention. When blobs move to
members, `MAX_CIRCUIT_TEXT_BYTES` must be re-specified as **per-member plus a
total**, or issue #38's decompression-bomb hardening quietly weakens.

**R13 — The format ships with its own git tooling, in-tree and tested.**
*(major; the format is unreviewable without it)*
An in-tree `.gitattributes` stanza, a `diff=jls` driver with
`xfuncname = ^ELEMENT ` (**measured: without it git labels a hunk inside a
`Splitter` block as `ELEMENT Binder`, the preceding record**), a textconv
driver that handles *every* accepted container — not bare `xz -dc`, which
breaks on the zip and plain-text containers the loader accepts
(`docs/file-format.md:42-52`) — and a merge driver that merges by stable id.
And the doc must state explicitly that **textconv does not restore delta
compression**: measured, enabling it left the pack at 726,334 bytes, 0 deltas,
7,191 B/rev, unchanged to the byte. It also does nothing for `git merge`,
`git blame -C`, `git add -p`, or GitHub's server-side diff.

**R14 — Do not adopt Git LFS.** *(minor, but decide it now)*
LFS stores each version whole; 100 revisions of a 214 KB XZ machine is 21.4 MB
in the LFS store regardless of edit size. It converts repo bloat into an
equal-sized LFS store plus a network dependency, against the single
self-contained offline jar / bus-factor-1 constraint, and does not make diffs
readable.

---

## 3. The tensions, decided

### 3.1 Diff-friendliness vs. compression

**DECISION: the uncompressed canonical text is normative and is the default
saved form. Compression is an explicit, per-member, opt-in transport envelope,
and where it is used it is framed per section. XZ is retained as the archival
codec; zstd is rejected.**

**Reason.** The objective function is wrong in the framing of the tension. Git
stores *deltas*, so the quantity that matters is `size(rev0) + N × marginal`,
not `size(rev0)`. Measured at N=100 on a 100k-element machine that is ~0.99 MB
of plain text against ~19.6 MB of XZ — the compressed container is **20x worse
in the repository while being 98x smaller on disk**. On the blob case the same
inversion: XZ makes the file 5.6x smaller and `.git` 4.9x larger, with a
~6,500x per-revision amplification (4,551,896 B vs ~700 B) because
`git verify-pack` finds **eight of ten XZ blobs stored as full base objects**.
The maintainer's own D1 reasoning holds independently: transparent filesystem
compression and user-side `zip` already provide the on-disk win, and the
mechanism already exists and is already tested
(`FileAbstractor.Container.PLAIN_TEXT`, `FileAbstractor.java:50-57, 218-243`).

The algorithm is not the variable. zstd −19 gives 40.1 MB against XZ's 41.6 MB
— 3.6%, i.e. noise. zstd's only measured advantage is **compression latency**
(406 ms vs 9,628 ms, 23.7x) at a 31.5% size cost — and once the default is
uncompressed, the compressor is off the interactive path entirely, so that
advantage is paid to nobody. Against it: `zstd-jni` is 7,574,312 B of jar
containing 18 per-platform native libraries against a 2,608,994 B shaded jar
(3.9x, plus `.so` extraction and native code inside a trust boundary whose
threat model is untrusted exchanged files) — disqualified outright by the
single-offline-jar constraint. The pure-Java alternative,
`io.airlift:aircompressor:2.0.3` (255,256 B, zero natives), works but emits
`sun.misc.Unsafe::objectFieldOffset will be removed in a future release` on the
project's own JDK 25 target, and the vendor's answer reintroduces 30 bundled
natives.

**Falsifiable revisit trigger for zstd:** adopt it if and only if (a)
checkpoint writing lands and profiling shows compression latency on the
interactive save/autosave path is user-visible, **and** (b) a pure-Java
implementation exists that does not depend on `sun.misc.Unsafe`.

**The one caveat that makes this ordering-dependent:** uncompressing without
fixing R1 makes repo bloat *worse* (11,003 B/rev text vs 7,531 B/rev XZ in the
front-insert case). The container flip must not land before R1, R2 and R3.

### 3.2 One-file convenience vs. sidecar cleanliness

**DECISION: split by *derivability*, not by size. Constitutive payloads
(memory / kernel / ROM images) are STORED members inside a single container
file. Derived payloads (simulation checkpoints, autosave) are sidecars,
gitignored by default, and their absence is a clean diagnostic rather than a
refusal.**

**Reason.** The tension is measurably not a tension for constitutive content:
**zip with STORED members is 2,397,301 B of `.git` over 10 revisions against a
raw sidecar's 2,396,453 B — within 0.04%.** You get the single emailable
artifact and full git delta simultaneously. (Deflating the members costs 8.2x:
19,556,469 B. STORED is load-bearing.) So one-file convenience is *free* for
anything that must travel with the circuit — emailing a CPU without its ROM
ships a circuit that does not work.

The only thing a sidecar buys that a member cannot is **`.gitignore`-ability**,
and that is exactly what derived content needs. A checkpoint is larger than
everything else combined, regenerated every run, reviewed by nobody, and
reproducible. `*.jlsck` in `.gitignore` is one line; excluding a zip member
from a commit is impossible.

This is also the lowest-friction path in this codebase: `FileAbstractor.readZip`
(`FileAbstractor.java:295-314`) already sniffs zip and already resolves members
**by name** (`JLSCircuit`, falling back to `JLSCheckpoint`). Multi-member is an
extension of a tested reader, not a new container project.

**Management rule that makes the pair safe:** the container records each
external sidecar's name, byte length and content hash; a reader that cannot
find one opens the circuit structurally with a diagnostic. This is exactly D3's
OPTIONAL-section discipline, and a checkpoint is its archetype.

### 3.3 Textual merge vs. operation-level merge

**DECISION: textual *diff* is the review and audit surface and must be
excellent. Textual *merge* is a hazard and must be actively prevented — today
with `*.jls merge=binary`, later with a semantic driver keyed on stable id.
The op vocabulary is the merge algebra. But the thing that actually makes
either safe is the validating load path, and it must ship first.**

**Reason.** Three measured facts force this shape.

*First*, a textual merge does not fail safe. Reproduced by the integrator: exit
0, zero conflicts, corrupt file, misdiagnosed to the user as truncation. Angle 4
built two worse: one wires the wrong pin, one loads *and simulates* a
bit-width-inconsistent circuit that the editor would refuse to draw. And the
roadmap's premise — that a line merge is *"guaranteed to conflict on
essentially every hunk"* (`lf-06:112-115`) — is measurably false, and the truth
is worse than the claim: git conflicts on the noisy edits and merges the quiet
ones silently wrong. The danger is invisible exactly where the diff looks
clean.

*Second*, the CRDT does not and structurally cannot cover this. Verified at
HEAD: `grep -rln 'add-wins|LastWriter|OR-set|ORSet|RGA' src/ test/` returns
exactly one file — `src/jls/collab/crdt/package-info.java:14-17`, the prose
*promising* them. `CausalBuffer.java:21-24` disclaims merge explicitly. The
only convergence test in the tree
(`test/jls/collab/session/RosterConvergenceTest.java:15-26`) converges session
membership, not circuits. And `VectorClock` compares *peer observation states*;
two offline git branch tips carry none, and no op log is persisted
(`OpExtensionPoints.java:25-27` has zero contributors). What shipped is causal,
exactly-once **delivery** plus a convergence **oracle**.

*Third* — and this is the ordering finding — **fixing diff stability removes
the accidental conflict protection that renumbering churn currently provides.**
Measured: the same branch pair conflicts (3 hunks) under today's dense ids and
merges cleanly under sid-keyed refs. Under sid refs that particular merge was
*correct*; the bit-width and duplicate-name cases were not, and they become
reachable **more** often once merges stop conflicting. So R1 increases the need
for R3 rather than reducing it, and the roadmap's C1 → C2 → (C3‖C4) → C5
sequence ships the hazard five to eight weeks before the guard.

The op layer is the right algebra and is genuinely close — `CircuitOp` sealed
over 11 kinds (`src/jls/collab/op/CircuitOp.java:34-37`), validate-then-mutate
atomic, exact inverses, addressed by stable id only
(`docs/collab-vocabulary.md:23`). But it is **not yet closed over circuit
content**: `AddElements` rejects subcircuit blocks
(`docs/operation-layer.md:64-68`) and `ImportSubcircuit`/`EditOrderedRows` are
deferred, so an op-expressed merge cannot represent a subcircuit add/remove.
That must be closed or documented as a tested refusal class before "a merge
expressed as ops cannot produce a file JLS refuses to load" is true.

**Prefer refusal over degradation** in the eventual driver, for three classes:
legacy stable ids, duplicate stable ids, and any change to subcircuit
population.

**Corollary worth recording separately:** the rendered SVG is **already
byte-invariant to exactly the id churn that destroys the text diff** —
measured, two variants whose canonical texts differ by 5,313 lines render to
byte-identical SVG (`CircuitRenderer.java:301-360`, pinned by
`SvgExportTest.exportingTwiceIsByteIdentical`), at 512 ms and 407 KB. For the
*review* use case specifically, the image view outranks the format fix. It
cannot help merge correctness, which is why both are needed.

### 3.4 Blob locality vs. simplicity

**DECISION: pay the complexity. Blobs get fixed-size, address-anchored,
positionally-independent framing (64 words per line inline, up to ~64 KiB;
content-addressed STORED members above that). The one-quoted-line idiom is not
carried forward under any circumstances.**

**Reason.** Simplicity is what produced both measured failures. The simplest
possible thing — one quoted line — costs **51,223,498 bytes of `git diff` for a
4-byte change** and makes two disjoint edits **always conflict**, doubling the
file. And the *obvious simple fix* — keep `initrle`, break it every N tokens —
measured **16,387 changed lines and 10,504,749 B of diff** for one word,
because the token stream reflows. That trap is easy to miss precisely because
an edit in an incompressible region or at the end of the file behaves fine
under both framings (3 lines, ~2 KB). Simplicity here is not cheap; it is a
correctness cliff with a benign-looking test case.

The complexity being bought is small and bounded: an absolute hex address
prefix per line, all-default lines omitted, words padded to `ceil(bits/8)`
bytes. **Measured cost 1.03x raw over a 16 MiB address space** — better than
`initrle` on sparse images once you count the framing, and with none of its
reflow hazard. It preserves `initrle`'s genuinely good idea (absolute
addressing keeps edits local — measured, one word changes **1 byte of a
25,611,604-byte encoding**) and discards the bad one.

Two things to state plainly so they are not oversold. **Line framing is a
human-diff and merge measure, not a storage measure** — `framed-rle-64` is
8,596,239 B of `.git` against one-line's 8,565,409 B, i.e. **0.36% worse**;
git's delta indexes bytes via a rolling block hash, not lines. And **"git does
not delta binary well" is false**: git delta'd a raw 16 MiB binary image to
457–526 B per revision. What git cannot delta is **compressed** data. That
distinction is the whole reason the STORED-member and sidecar recommendations
work while the compressed container does not.

### 3.5 Canonical bytes as convergence oracle vs. diff-friendly ordering

**DECISION: keep canonical order in the bytes; move the oracle to per-section
hashes.**

**Reason.** The tension dissolves once R1 lands. Measured: with stable refs, a
new element's block is *inserted*, never moved, under any immutable key — `sid`,
`(tag, sid)` and `(name, sid)` all give **1 hunk** for an insert. Block position
becomes a readability question, not a diff question, so there is nothing to buy
by relaxing the order. What does need to move is the *granularity* of the
oracle: a single whole-file `stateHash` cannot express "this definition
changed and nothing else did", which is what R9 and R10 both want.

### 3.6 Reproducibility vs. identity

**DECISION: pinning `jls.replicaId` / `JLS_REPLICA_ID` is an export-and-CI knob
only, and the docs must say so explicitly.**

**Reason.** `docs/file-format.md:383-386` currently *recommends* pinning it for
"CI byte comparison and reproducible export". Measured, two branches of one
install already mint the same id; pinning the replica across machines converts
a per-install collision into a **cross-user** one, defeating the identity
property every merge tool must key on. Also worth a line in
`docs/version-control.md` before any grading guidance mentions it: the replica
id is a persistent per-install fingerprint written into every save (measured, a
clean install persisted `913aa3d93b2649a6addc2c1547f21340` to
`~/.config/jls/replica-id`), and it becomes the install's cryptographic
identity when #168 lands. It is simultaneously the strongest plagiarism signal
available and a disclosure students are not told about.

---

## 4. Verdict on the `.jlsx` direction

**Diff stability argues FOR a successor format — but for a reason the proposal
does not state, and the two headline features (JSON/XML-like syntax, zstd) are
the two least valuable parts of it. One of them is actively harmful.**

Feature by feature:

**Internal per-section versioning — STRONGLY FOR. This is the part that earns
the successor.** It is the only feature in the proposal that pays for itself
four separate ways: it gives natural **compression frame boundaries** (R11), it
gives a place to declare **merge participation** per section (R10), it gives
**per-section hashes** to replace the single whole-file `stateHash` so a peer
syncs one changed definition instead of a whole circuit, and its
optional/must-understand semantics mean a reader that knows nothing about
checkpoints still opens the circuit structurally with a clean diagnostic. One
caveat with a measurement behind it: version the section **kind** once in the
header, not each section instance, or a format bump is itself an N-line diff.

**The version bump as a vehicle — FOR, and this is the real argument.** Three
of the highest-ranked requirements are *unreachable* in `.jls` without a bump
under `docs/file-format.md` §9. Stable-id references change "the meaning of an
existing record". A multi-line blob is a new item kind, and §2 makes
one-quoted-line normative. Definitions-by-reference changes block structure.
So the diff fix and the successor are coupled by the evolution policy — **but
the coupling runs the other way from what is usually assumed**: what is needed
is *a* version bump, not *this* container. See §5.

**Sectioned structure with separated blobs and checkpoints — FOR.** Measured
payoff is the largest of any container decision: a 16 MiB image out of the text
budget takes `.git` from 41.6 MB to 2.40 MB over 10 revisions, takes the
one-word `git diff` from 51,223,498 B to ~2 KB, and takes two disjoint edits
from an unresolvable conflict to a clean merge. It also unbinds the 64 MiB cap,
which one 16 MiB image currently occupies to **93.2%**.

**Shared/parameterized subcircuit definitions — FOR, on diff grounds alone,
independently of D4.** Measured: 8 hunks for one logical edit at 8 instances,
with 99.4% of the file duplicated, and R1's churn multiplying by instance
count. Definitions-by-reference makes it 1 hunk at any instance count. Note
this is a genuine rewrite of subcircuit semantics (`SubCircuit.java:283-289`
inlines the whole nested circuit; measured sharing factor exactly 1.00x), not a
serialization tweak.

**JSON/XML-like syntax — NEUTRAL AT BEST, MILDLY AGAINST. Do not do this for
diff reasons; there are none.** The current line-oriented `kind name value`
grammar already reaches the semantic floor: a move is **1 line in 1 hunk**, an
attribute edit is **1 line in 1 hunk**, and an insert under sid refs is
**0 removed / 9 added in 1 hunk**. No serialization syntax can beat one line
per attribute; syntax can only lose ground. JSON loses it in three specific
ways: (i) trailing-comma sensitivity makes appending to a list touch the
*previous* line, converting a 1-line insert into a 2-line diff and a spurious
merge conflict at every list tail; (ii) canonical JSON needs a pinned key order
*and* a pinned number format, which is *more* normative spec surface than the
current grammar, not less; (iii) brace-per-object pretty printing inflates line
counts and dilutes `xfuncname`. XML is strictly worse — closing tags roughly
double the line count for zero diff benefit. **If a structured syntax is
adopted for other reasons (schema tooling, third-party readers), mandate
one-value-per-line emission with sorted keys and a record-per-line container
(JSON Lines) so that appending never touches a neighbouring line.** Judge this
on the ecosystem argument, not on diff.

**zstd compression — AGAINST as proposed, on measured grounds.** Three
independent reasons. (i) *It does not help the problem it is being adopted
for*: zstd −19 gives 40.1 MB of `.git` against XZ's 41.6 MB — 3.6%, noise —
because whole-stream compression is the delta killer regardless of algorithm.
(ii) *Its real advantage is off the path*: 23.7x faster compression matters only
if compression is on the interactive save path, and D1 takes it off. (iii) *It
costs the governance constraint*: `zstd-jni` is 7.57 MB of 18 bundled
per-platform natives against a 2.61 MB shaded jar, and the only pure-Java
option warns today on the project's own JDK 25 target that
`sun.misc.Unsafe::objectFieldOffset` will be removed. **Keep XZ as the opt-in
archival codec; if any compression is applied to a version-controlled artifact,
turn on block framing (`--rsyncable` / `--block-size`) — measured 3.8x better
git packing for a 0.1% size cost — and document that it restores delta and
never diff or merge.**

**Ranked, then: per-section versioning ≫ separated/content-addressed blobs ≈
definitions-by-reference > container structure > record syntax > compression
codec.** The proposal's two named features sit at the bottom of that list.

**And the sharpest thing to say to the maintainer:** *none of the diff-stability
value requires waiting for `.jlsx`.* The 531x fix is `sref` under `.jls`
FORMAT 3 — a contained change to one writer method
(`WireEnd.java:605-613`), two reader sites (`Circuit.java:1107-1114`, `:1607-1610`)
and four spec sections. Shipping it first is also the honest way to *validate*
the successor's central design decision on real files before committing it to a
new container, and it is worth doing on its own merits even if `.jlsx` never
ships.

---

## 5. Cheapest high-value changes, ordered

### Part A — no successor format required, no format-version bump required

Ordered. Items 1–3 are prerequisites for everything after them.

**A1. Fix stable-id minting.** Seed `NEXT_COUNTER` from `max(counter)` per
replica over the loaded circuit and from a persisted per-install counter; make
`ElementId.parse` advance it; assert `sid` uniqueness in `Circuit.save`.
*(`ElementId.java:60, 210-213, 245-270`; `Element.java:24`; `Circuit.save`.)*
Hours to days. JLS currently writes files it refuses to reopen — reproduced —
and the same collision poisons the undo stack. Every identity-keyed tool
downstream is unsound until this lands.

**A2. Headless `Circuit.validate()`, called at the end of `finishLoad`, plus a
`-check` flag with a distinct exit status.** Lift the two missing checks — net
bit-width agreement and element-name uniqueness — from
`SimpleEditor.canConnect:4231-4265` and the op layer's paste rules. Days, not
weeks. Closes the two measured silent-corruption classes from **every** entry
path (merge, hand edit, generated file, peer snapshot, HDL import), and it is
the acceptance criterion any future merge driver needs anyway. **This must land
before A6 and before A7.**

**A3. Fix the legacy sort order.** One comparator change at
`ElementId.java:278-285` (or via the replica-alias approach). **Measured:
converts the guaranteed worst case into the best case — 5,312 changed lines →
10 — for the 3 of 4 tracked files that carry no sids.** Highest
measured-benefit-per-line in the entire area.

**A4. Two `.gitattributes` / `.gitignore` lines.**
Add `*.jls~` to `.gitignore` and narrow the blanket `*.jls` (measured: the
repo's own template ignores every circuit and exposes every autosave artifact;
`riscv/gui/cpu.jls` is tracked only by `git add -f`). And add
`*.jls merge=binary` — useless but never wrong — **in the same commit as any
container default flip**, to be removed in the same commit as the semantic
merge driver.

**A5. Add `-canon [file|-]`:** writes to stdout, accepts any input path, and
does **not** rename the circuit. A few dozen lines. Unblocks the git clean
filter, the textconv driver, the grader hash oracle, and any future
`-diff`/`-merge3` simultaneously. Do **not** build any of those on `-savetext`
as it stands: it renames the circuit to the output file's stem
(`JLSStart.java:495-505`) and validates the *input* operand's name
(`JLSStart.java:181-189, :293-301`), so git's bare temp path is rejected.

**A6. Flip the default save container to plain text** (D1). One enum default
plus doc and test updates; the mechanism is already implemented and pinned by
`FileAbstractorTest`. **Only after A1–A4.** Measured: 15x cheaper in git at
default gc, 147x after aggressive, and it restores real hunks and real merges.
Cost: ~98x larger working-tree files at 100k elements, which also brings the
64 MiB text cap ~98x closer — acceptable for a version-controlled file,
and mitigated by R6 moving blobs out of the text.

**A7. Ship the git integration in-tree and document its limits.**
`.gitattributes` with `diff=jls`, `xfuncname = ^ELEMENT ` (fixes the measured
wrong hunk header for free), a textconv driver built on `-canon` that handles
every accepted container, and optionally the clean/smudge filter — which works
unusually well here because `FileAbstractor` sniffs *content* not filenames, so
the smudge side can be the identity `cat` and the checked-out plain-text `.jls`
still opens in JLS. **Measured: 62,730 B of `.git` over 100 move revisions with
97 deltas, against 726,334 B and 0 deltas raw.** State in the doc, in bold,
that textconv does **not** restore delta compression.

**A8. Ship the grader hash oracle.** With A5 done, `sha256sum` over canonical
text equals `Circuit.stateHash()` exactly (verified:
`9983dd53e26caa6662b4e2f9131fc015cfd33c9e2df1a67443a49dd6046d25f8`), and legacy
sid minting is deterministic in file order, so a distributed skeleton hashes
identically for every student. Document its limit sharply: it answers
*"unmodified?"* and nothing else.

### Part B — needs a format-version bump, but **not** a successor format

**B1. `sref` item kind under `.jls` FORMAT 3, and drop the ` int id` line.**
The single most valuable line item in this entire report. **Measured: 5,312
changed lines / 234 hunks → 0 removed / 9 added / 1 hunk (295x lines, 234x
hunks), with the file 9.7% shorter and 13.7% smaller compressed.** A new item
kind requires a bump per §9, and the pre-bump failure mode ("unknown item
kind") is exactly the one §3 calls intended. Old files keep `ref` forever;
readers accept both. *(Writer `WireEnd.java:605-613`; reader
`Circuit.java:1107-1114`, `:1607-1610`; spec §3, §5, §8, §9.)*

**B2. Change canonical order to `(tag, sid)` in the same bump,** while the file
is being rewritten anyway. Identical on every diff metric, far more readable,
and it subsumes the `isWire ? 1 : 0` special case into the general rule.

**B3. An explicit, separately-committed migration** (`-migrate` batch mode) for
pre-sid files. The rewrite is unavoidable — every element gains a `sid` line
and every ref changes form — and it must not be discovered accidentally inside
a user's first edit.

### Part C — genuinely requires the successor

These cannot be retrofitted to `.jls` without becoming a different format.

**C1. Multi-line blob item kind.** `docs/file-format.md` §2 makes
"a quoted value MUST begin and end on the line of its item" normative. There is
no in-`.jls` fix for the 51 MB diff.

**C2. Container members: blobs out of the text budget, STORED, content-addressed
(`sha256` + length), validated on load.** Requires a multi-member container and
a re-specified `MAX_CIRCUIT_TEXT_BYTES` (per-member plus total).

**C3. Sidecar checkpoints with OPTIONAL/must-understand semantics**, referenced
by name+length+hash, absence a diagnostic not a refusal, explicitly outside
`stateHash`. Requires D3's section machinery.

**C4. Per-section-kind versioning declared once in the header, and per-section
hashes replacing the single whole-file `stateHash`.** Requires
`docs/reproducibility.md` and `DeterministicSaveTest` to be restated.

**C5. Merge-participation as a declared per-section property** (structural =
merged; blobs and checkpoints = hashed, never merged, resolved to one side with
a recorded conflict).

**C6. Subcircuit definitions as top-level sections with `ref def <did>` and
per-instance parameter bindings.** A rewrite of subcircuit semantics, coupled
to D4's parameterization goal.

**C7. Per-file replica alias table** and framed per-section compression.

---

## 6. Tests that pin diff stability as a property

The existing suite pins **content determinism** (`DeterministicSaveTest`),
which the format already has. Nothing anywhere pins **locality**, which is the
property that is missing. These are executable assertions with thresholds taken
from measurements; the ones marked FAILS TODAY are the point.

**T1 — Insert locality, parametrized over size and replica. FAILS TODAY.**
For N ∈ {100, 1000, 4000} and `JLS_REPLICA_ID` ∈ {a hex draw, a string sorting
before `legacy`, a string sorting after}: load an N-element circuit, add one
element, canonically re-save, and assert **≤ 12 changed lines and ≤ 2 hunks**,
independent of N and independent of the replica. Measured today: 10 lines /
1 hunk in the lucky case and 5,312 lines / 234 hunks in the guaranteed case.
The replica axis is what makes this test worth writing — a test that pins only
the lucky case pins nothing.

**T2 — Delete locality. FAILS TODAY.** The exact inverse; same thresholds.
Measured today: 5,310 of 10,744 lines for an early delete.

**T3 — Move locality. PASSES TODAY — pin it before it regresses.** A pure
geometry edit changes **≤ 4 lines in 1 hunk**. This is the control that proves
churn is an identity problem and not an ordering problem, and it is the
property any successor ordering must preserve.

**T4 — Save→reload assertion, applied to every save in the round-trip suite.
FAILS TODAY.** Every file JLS writes must `load` **and** `finishLoad` **and**
`validate`. Reproduced failure: one load, one add, one save produces
`finishLoad=false … two elements declare the same stable id 'lab:405'`.
Elevate this from a test convention to a format-level requirement (R12).

**T5 — Mint-collision property test. FAILS TODAY.** Load a circuit with a hole
in its counter sequence under the same replica, add an element, save; assert no
duplicate `sid`. Generalize with a small generative fuzz over
(delete-then-add, paste, undo-redo) sequences.

**T6 — Branch independence. FAILS TODAY.** Two separate JVMs, same pinned
replica, same parent file, one element added in each → **distinct** sids.
Measured today: both mint `r:1551`. This is the assertion that makes
`(path, sid)` sound as a differ/merger primary key.

**T7 — Merge safety matrix. 2 of 9 CASES FAIL TODAY.** Table-driven over the
nine constructed scenarios (independent adds; delete-vs-append; insert-early
vs wire-spares; independent width edits; dangling ref; unknown put;
double-attached put; duplicate sid; duplicate name). For each, assert
**either** git conflicts **or** the merged file loads, validates, and its block
multiset equals the intended result. Today scenarios "insert-early vs
wire-spares" and "independent width edits" merge cleanly and are wrong; the
second one *simulates*.

**T8 — Validator coverage. 2 of 6 FAIL TODAY.** One hand-built malformed file
per invariant class, each asserted refused with a distinct diagnostic. The two
that pass silently today are net bit-width agreement and element-name
uniqueness. This test is the specification of R3.

**T9 — Repo-growth budget, at DEFAULT gc. FAILS TODAY for XZ.** Commit 100
revisions of a move edit and assert `.git` grows **< 1 KB per revision** with
`git gc --prune=now` (not `--aggressive`, because users get the default).
Measured today: plain text 482 B/rev, XZ **7,191 B/rev with 0/100 deltas**.

**T10 — Canonical-form purity. FAILS TODAY.** Canonical bytes must be
independent of the input file's name and path: `canon(copy of X at path P1)`
== `canon(copy of X at path P2)`. `-savetext` fails this (`CIRCUIT riscv` →
`CIRCUIT st1`).

**T11 (successor) — Blob locality.** Re-emit `riscv-sum1to10.jls`'s two
`Memory` elements as 64-word-per-line sparse addressed base64 in a plain-text
container; commit 10 revisions each changing one instruction word; assert
(a) `git diff` output **< 4 KB**, (b) two disjoint edits **merge clean**,
(c) `.git` grows **< 1 KB per revision**. All three thresholds are met by the
recommended design and **all three fail today**.

**T12 (successor) — Section frame independence.** Edit section A; assert
section B's stored/compressed frame is **byte-identical**. This is the
executable form of R11 and the only thing that stops the LZMA2 cliff from
reappearing inside `.jlsx`.

**T13 (successor) — Definition fan-out.** For instance counts N ∈ {1, 8, 32} of
one shared definition, changing one attribute in the definition produces
**exactly 1 hunk**, independent of N. Measured today: 8 hunks at N=8.

**T14 (successor) — Merge-participation is honoured.** A merge driver presented
with divergent blob or checkpoint sections must record a conflict and resolve
to one side, never combine them line-wise.
