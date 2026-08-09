# Issue #364: FEAT-036: a drawn core does a single-cycle sub-word store, and memory capacity is a stated byte budget with headroom instead of a word-count cliff
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the machinery and there are two wants. (1) A drawn RV32 core should be able to
execute `sb`/`sh` — `riscv/README.md`'s "Scope note (sub-word memory)" is the standing
admission that it cannot. (2) A `Memory` element should be able to hold a guest-sized
image without the user reasoning about a magic word count.

Both wants are squarely on the project's arc. `docs/capability-roadmap/sweep-05-system-and-interfaces.md`
§C calls byte lanes "the cheapest change in this sweep with the highest ratio of unlocked
standards to weeks" and names four standards that need them (Wishbone `SEL_O`, APB
`PSTRB`, RISC-V privileged CSR masked writes, RV32I completion). `docs/capability-roadmap/README.md:275-294`
makes the same case pedagogically ("Endianness becomes concrete via byte lanes").
Nothing below disputes the goal. What follows disputes the shape, the bundle, and — for
the storage half — the target.

## 1. The bundle is the largest defect, and it is self-diagnosed

The issue's own §2 says the quiet part: TASK-0013 is FEAT-006's work, TASK-0034 is
FEAT-013's work, and "3 of those weeks are already committed elsewhere." Of 4.5 stated
weeks, **1.5 are unshared** — TASK-0076, the mask. Everything this feature uniquely owns
is one task, and to it the issue has attached a hard `blocked_by: [319]` on a
file-format feature that has not landed, plus a shared open question that must be
"answered once, not twice" with #354.

That is coupling manufactured by the write-up, not present in the work. The mask touches
`Memory.init`/`react`/`save`/`setValue`; the storage work touches `newWordStore`/`initSim`;
the section touches the container. The issue says as much in §6 ("TASK-0076 is fully
independent of the other two"). A feature whose parts are independent, whose two larger
parts are owned elsewhere, and whose only owned part is blocked by neither, is three
issues wearing one hat. Evidence that the hat is already slipping: one day after filing,
the sole `blocks` edge (#326) was closed as a duplicate of #202, and the graph, the
mermaid block and the comment now disagree about the far endpoint.

**Alternative framing A — dissolve the feature.** File TASK-0076 as a standalone element
issue and land it next week; it needs nothing from #319, #354 or #202. Let TASK-0013 be
owned outright by #354 and TASK-0034 by #319. FEAT-036 then either ceases to exist or
survives as a thin capability note. Nothing in §5 criteria 1, 4, 5 or 6 requires the
bundle; only criterion 2 does, and the re-planning protocol already concedes criterion 2
is waivable.

## 2. The stated motivation for the mask appears to be false, and the true one is stronger

§ Intended Audience: "a sub-word store today costs a read cycle, a merge and a write
cycle. After this feature it costs one clock." That premise assumes a synchronous-read
memory. JLS's `Memory` reads asynchronously: `react`'s `PinChanged` arm posts a
`MemoryRead` on any react with `CS`/`OE` low, gated only by `accessTime`
(`src/jls/elem/Memory.java:1391-1398`). `riscv/build_cpu.py` already exploits this — the
data RAM's `WE` is gated with `NOT clk` so a write commits in the clock-low phase after
the datapath settles. A drawn read-modify-write therefore already fits inside one clock
period today: async read, mux/merge, masked write on the low phase. What it costs is
*elements and settling time*, not cycles.

If that reading is right, integration criterion 1 ("a sub-word store completes in one
clock, asserted by a simulation golden") is **non-discriminating** — the drawn baseline
passes it too. The criterion cannot distinguish the feature from its absence, which is
the one thing an integration criterion exists to do.

The real case for byte lanes is the one sweep-05 makes and this issue never states:
`WSTRB`/`PSTRB`/`SEL_O` are *mandatory fields* of the bus protocols on the roadmap;
Yosys `$mem_v2` write ports carry per-lane enables (`CellValidator.checkMemory` rejects
every real RAM today); HDL export of `Memory` (#291) needs a port that maps to a byte
enable rather than to open-coded merge logic; and the endianness lesson needs a lane to
point at. Rewrite the motivation around interoperability and element economy — the
27%-of-the-processor figure at `docs/capability-roadmap/README.md:275-283` is the honest
rhetorical weapon — and give criterion 1 a discriminating form: *the drawn RMW merge
network disappears from the fixture* (element-count delta on a `sb`/`sh`-capable core),
not a cycle count.

## 3. The mask is the fourth bolt-on boolean on a class the roadmap says to re-cut

`docs/capability-roadmap/README.md:236-240` and `sweep-03-elements-and-hdl.md` §C3 both
record the direction plainly: **replace `Memory`'s fixed pin set with a port list** — N
read and M write ports, each clocked or combinational, each with its own address/data/
enable, per-port byte/bit write masks, and a declared read-during-write policy. Sizing
there is 4–7 weeks with the instruction "schedule it alone."

This issue instead adopts the #199 precedent — one saved boolean, one input appended
last — as *the* discipline (§4 invariants 2–3). That precedent was correct for one
attribute. Applied a second time it produces a second appended port whose position
depends on which booleans are on; applied to the dual-port work as well it produces a
port ordering that is a function of a bitmask of attributes, on a 1547-line class holding
51 of the tree's 417 `BitSet` references. "Append last" is a compatibility trick with a
half-life, and the roadmap already recorded its replacement.

There is a cross-issue symptom of the split. #320 (FEAT-020, since closed as duplicate)
listed FEAT-036 as the provider of "memory cells with independent read and write ports"
and put its invariant 4 exclusion on that basis — but #364 delivers no second port at
all. The importer's exclusion set is currently pointed at a provider that does not
provide.

**Alternative framing B — one port descriptor, legacy as its default value.** Instead of
`int lanes 1`, save a single port-model descriptor and build `init`'s inputs from it,
with the pre-existing pin set as the descriptor's default so every existing file loads
and re-saves byte-identically without a per-attribute conditional. Lanes become a field
of a write port; #320's second read port becomes another entry; the CSR masked-write
element becomes a descriptor value rather than a fifth boolean. This costs more than 1.5
weeks and should be honestly priced against the roadmap's 4–7. The reason to pay it now
is that the mask is precisely the change that makes the port ordering combinatorial, so
this is the last cheap moment to cut along the right seam.

## 4. The byte budget measures the wrong bytes — I am disregarding Open Question 1

I am explicitly setting aside the issue's Open Question 1 (choose $B_{\max}$, state
headroom over a named guest image, expose it as a system property) and the completion
criterion built on it. The arithmetic is correct and the target is wrong.

$B_{\text{dense}}(n)=8.125n$ describes `DenseWordStore` alone. Initializing a 4M-word
memory from the shipped paths costs, at peak and concurrently:

- `initialValue`, a retained `String` of the whole `addr value` dump (~18 chars/word ⇒ ~70 MB);
- `decodeInitRLE`, which materializes that canonical text again in a `StringBuilder`
  (`:623-650`) before anyone parses it;
- `initOK`, which then re-parses it with `new Scanner(nextLine)` **per line** plus a
  `BigInteger` per word (`:841-871`) — 4M regex-backed `Scanner` instances;
- `initMem` at 34 MB, and `mem = initMem.copy()` at another 34 MB (`:1309`).

TASK-0013 targets the last item. A "byte budget" of $8.125n$ with 2x headroom would
therefore certify a configuration whose actual peak is several times the certified
number, and §5 criterion 3's structural COW witness would go green while the dominant
allocations are untouched. A budget that does not predict the failure it exists to
prevent is worse than no budget: it is a number a user will trust.

Worse, the same paths defeat the goal at the other end. `DenseWordStore.addresses()`
(`:1132-1140`) walks the presence bits into a `TreeSet<Integer>` of boxed addresses, and
it is called by `printChangedValues` (`:918`) and `storedAddresses` (`:1544`, the GUI
contents dialog). On a densely-initialized guest image that is ~4M boxed `Integer`s in a
red-black tree — hundreds of megabytes, from a `-r` print or from opening a dialog. The
"guest-scale image" the feature promises is not reachable through the API the feature
leaves in place.

**Alternative framing C — a paged copy-on-write store, and the budget question
evaporates.** Replace `DenseWordStore` *and* `SparseWordStore` with one paged store:
pages of 1024 words (`long[1024]` + a 1024-bit presence mask ≈ 8.25 KB), held in a
`HashMap<Integer,Page>` or a two-level table, allocated on first touch. Then:

- Fully-populated cost is unchanged at ~8.125 bytes/word; untouched capacity costs zero.
- `DENSE_CAPACITY_LIMIT`, `newWordStore`'s branch, the dense/sparse duality (two classes,
  two copy constructors, two `addresses()`), the "reported rather than silent fallback"
  invariant (§4.6), integration criterion 4, and Open Question 1 **all cease to exist**.
  There is no cliff to report crossing.
- `copy()` becomes a page-table copy with pages shared and cloned on first write. That is
  the copy-on-write TASK-0013 wants, obtained as a property of the representation rather
  than as a special initialization path — and the "package-visible witness" becomes page
  identity, cheap and honest.
- Capacities above $2^{22}$ words stop degrading to a `HashMap<Integer,BitSet>` with its
  ~100 bytes/word (the very cost #20 recorded), so large *sparse* memories get better too.

**Alternative framing D — the initial image is a value, not a `String`.** Parse the image
once, at load, into the store; keep text/RLE as *encodings* of it, and re-derive the
saved form from the store. This deletes the retained `String`, the decode-to-text-then-
re-parse round trip, and the per-line `Scanner`. It also retires `encodeInitRLE`'s
"canonical dump or bail to raw" heuristic (`:471-486`), which exists only because the
element's truth is currently a blob of text — and with it the class of save/load
fixed-point bugs #160 found. Paired with C, this is a smaller change than TASK-0013 as
written and addresses the cost TASK-0013 misses.

## 5. The bulk section is premature, and its own arithmetic is missing a term

TASK-0034 buys an independently versioned binary section, a payload hash, a mutual-
exclusion diagnostic, and a hard dependency on #319. Before paying that:

- **The sidecar already exists.** `Memory` has `String file` and reads it at `initSim`
  (`:1251-1268`). A guest image can be an external file today, at zero format cost. The
  issue never mentions it — and §3's mutual-exclusion rule enumerates three image forms
  while ignoring this fourth, which today resolves by *precedence* (file wins over
  `init`). §4 invariant 5 ("never resolved by precedence") is thus already violated by
  shipped behavior it does not acknowledge.
- **The threshold arithmetic omits RLE and XZ.** §3 computes $\kappa G$ for escaped hex
  text against $C = 64$ MiB, but the writer emits `initrle` when it is shorter (`:459-462`),
  and the container is XZ. A boot image with long zero runs may encode to kilobytes. The
  issue asserts "a 16 MiB image alone exceeds $C$" without computing the encoded size of a
  *realistic* image. That number should be measured before a new section kind is
  justified by it.
- **If the cap is genuinely the binding constraint**, raising `MAX_CIRCUIT_TEXT_BYTES`
  (`src/jls/FileAbstractor.java:65`) under the existing #38 hardening discipline is one
  constant, not a versioned section with hashes.

Defer TASK-0034 entirely. Revisit it when a measured image with a measured encoded size
demonstrates the need — at which point it is FEAT-013's section kind and should be filed
there, not here.

## 6. What I would do

1. **Now (≈1–1.5 wk):** land the byte-lane write port, under framing B if the maintainer
   accepts the port-descriptor cut, otherwise under the #199 pattern as written. Keep §4
   invariants 1–3 and Open Question 3's answer (runtime writes only) verbatim; both are
   right. Replace criterion 1 with an element-count delta on a `sb`/`sh` fixture.
2. **Now, cheaply (≈0.5–1 wk):** framings C and D inside #354 — the paged COW store and
   the parsed image. Retire `DENSE_CAPACITY_LIMIT`, and fix `addresses()` to stream rather
   than materialize a boxed `TreeSet`.
3. **Not now:** the bulk section. Reopen only against a measured encoded size.
4. **Then:** the port model as one scheduled piece of work (roadmap P2, 4–7 wk), absorbing
   the dual-port shape #320's successor needs.

What survives the rethink is the best-argued material in the issue: the absence evidence
pinned to a commit, the mask-absent-means-all-ones hazard (§5.6 — genuinely the most
likely silent-wrong behavior, keep that test), the refusal to re-baseline goldens, and the
insistence that the dense/sparse choice not be silent. That last one I am declining only
because framing C removes the choice rather than hiding it.
