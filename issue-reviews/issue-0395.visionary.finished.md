# Issue #395: TASK-0071: the guest image is built by a pinned, byte-reproducible recipe, its determinism requirements are machine-definition fields rather than prose, and where it lives is a decided, tested answer
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Three things, and the issue treats them as one artifact:

1. **An oracle.** The console byte stream the parity harness compares must be a
   function of architecture, program and input only — not of simulated time or
   measured throughput. This is what #301 CAP-02 and #295 CAP-03 actually rest on.
2. **A demo.** A person presses Run and watches Linux boot to `/ #`. This is what
   makes CAP-02 legible to a reader, and it needs a real kernel.
3. **An insurance policy.** RV32 nommu is on a published removal trajectory, so a
   rebuild recipe turns an upstream deletion into documentation rot.

The issue is well made — the O3 arithmetic, the §7.10 statement of reproducibility
as a function of pinned inputs, and the refusal to put a multi-hour buildroot job on
a required lane are all better than this tracker's norm. My objection is not to any
of its parts. It is that fusing (1) and (2) into one artifact imports the hardest
reproducibility problem in the tree into the exact place where it buys nothing, and
then bills that problem to the critical path of a capstone already priced at
**159–253 maintainer-weeks** (#301, sixteen required rows).

## 1. The parity claim never needed a reproducible build

§9 says P3 (two hosts, identical digests) is "the load-bearing one." Load-bearing
for *what*? Not for parity. Parity needs the two tiers to run **the same bytes** —
which one blob and one recorded digest satisfy completely. Byte-identity of a
*rebuild* on a second host is a property of goal (3), the insurance policy, and
of nothing else. The issue never separates them, so a nice-to-have becomes a
`must hold after` prediction and a Definition-of-Done checkbox.

That matters because H1 is optimistic in a way the repo already knows better than.
`docs/reproducibility.md` §1 is the mature pattern here: a **declared** artifact
set with honest `No` rows — the msi, the dmg and *the container image* are all
recorded as not reproducing, each with a different integrity story (attestation +
`SHA256SUMS`). #395 asks for byte-identity across hosts of a Linux kernel plus a
buildroot uClibc/elf2flt nommu rootfs — the hardest class in the project — with no
`No` row available, and with the pinning strategy resting on a container image
this very table declares non-reproducible. When P3 fails, §10's remedy ("bisect by
digest, name the unpinned input") bottoms out in host readdir order leaking into a
tar, and that hole has no floor. There is no `WAIVED:` path drawn for it.

## 2. The reframing: two guests, two disciplines

**I am disregarding P3 as a completion criterion, and H1 as stated.** Split the
deliverable:

**(a) A parity corpus — the oracle.** Small hand-authored bare-metal RV32 programs
whose console output is architecture-only *by construction*: no `printk`, no
`loops_per_jiffy`, no hostname, no shell prompt. Assembled by a checked-in
assembler over checked-in text. This is byte-reproducible **trivially and forever**:
no upstream, no container, no cross toolchain, no digest bisection, no deprecation
risk. It is also the artifact that can live in the required fast lane, run in
seconds, and be *extended* by anyone who finds a new divergence.

Note what happens to §2 O2 under this split: **all four determinism requirements
become unnecessary rather than "recorded as fields with reasons."** There is nothing
to set `printk.time=0` on. The problem disappears instead of being managed. That is
the test of a good reframing.

**(b) A Linux blob — the demo.** One image, digest-recorded in the sidecar the D15
ruling already chose, published as a release asset next to the jar rather than
tracked in `.git`, with `scripts/build-guest-image.sh` as an *informational*
recipe — exactly the status §6 and §7.11 already grant the toolchain job. It boots
in the long-run lane. It is not the oracle; it is the thing that proves the oracle's
machine is a real computer.

The two are already treated differently everywhere in this issue (§6 "never on a
required CI lane", §7.12 "long-run lane with its length stated"). The split just
makes that separation structural instead of a caveat.

## 3. Three of the four "machine-definition fields" cannot be enforced

§7.4 makes the contract "the fields are data, not comments; a run that ignores one
is not comparable, so the runner must fail rather than default." Check that against
the four rows of O2: `printk.time=0` and the pinned `lpj=` are kernel command-line
state, and the fixed hostname and time-free prompt are rootfs contents. All three
are baked into the image at build time. The runner **cannot check any of them** —
it can read a field that claims `printk.time=0` and boot an image built without it.
Only `timebase-frequency` is a machine property the runner enforces.

The enforceable form of H2 is already in this issue and is stronger than the record:
**P5 — run the same image at two declared clock rates and diff the console.** That
single observation subsumes all four rows, and it finds the "fifth source" H2's
refutation clause worries about *automatically, on any guest, forever*, instead of
requiring a human to notice and add a fifth field. Promote P5 from a prediction to
a standing property test on the runner — "console output is invariant under declared
clock rate" — and demote the four-field table to what it is: build-time
documentation plus one runner-enforced field.

## 4. The residence question is already answered, and the live half dissolves

#343 was amended on 2026-08-03 by maintainer ruling **D15**: the image is a sidecar
file with a recorded digest, `blocked_by: [319]` is removed, and Open Question 2 is
closed. #395 was filed the same afternoon and still presents residence as an
execution-blocking three-option question, still frames itself as "explicitly
reopening the 'no committed guest images' exclusion," and still puts that reopening
in its own title. The one thing D15 does *not* settle — commit the blob or rebuild
it — is precisely what the §2 split answers: corpus (a) is committed and tiny; blob
(b) is a release asset referenced by digest. `.git` never grows, so P7's ".git growth
basis" measurement (whose source figure §11 admits was taken outside this repository)
has nothing left to measure.

Two more citations to check before pickup: `docs/parity-contract.md`,
`docs/machine-calibration.md`, `docs/plan/evidence/BRIEF.md` and the layer-stack
document carrying the "no committed guest images" exclusion are **none of them
present in the checkout**. A contributor picking this up cannot read the constraint
they are being asked to withdraw, cannot read §3.2's digest requirement, and cannot
verify a single one of the calibration figures (16 MiB, 4.0×10⁷ instructions,
15.87 bytes/word, 56,849,791 vs 61,233,095). Whatever else happens, the numbers this
task designs to should be restated in-tree.

## 5. Reuse the supply chain that already exists

§1 says of #184: "**Do not conflate the two build pipelines.**" For the *build* half,
agreed. For the *distribution and verification* half that instinct is backwards. JLS
already ships digest-verified, provenance-attested artifacts with `SHA256SUMS` files
and `gh attestation verify` (README §Installing, `docs/reproducibility.md` §5). A
guest image published that way inherits a working, audited integrity story and a
distribution shape a classroom already understands — download `rv32-soc.jls` and
`guest-<digest>.img` the same way you download the jar and its `SHA256SUMS`. #395
instead grows a second, parallel `SHA256SUMS` discipline inside `scripts/`, invisible
to the release workflow, the attestation, and the BOM. Add a row to
`docs/reproducibility.md` §1's declared set — stating its scope honestly, `Yes` only
within the pinned container — rather than starting a private one.

## 6. One thing outside this issue that P9 makes binding

P9 forbids anything being homed under `riscv/`, because TASK-0025 deletes it. That
directory contains `riscv_ref.py` (975 lines: an independent RV32I assembler and
emulator), `fuzz_diff.py` (randomized differential testing over hundreds of generated
programs), `verify.py`, and `test_primitives.py` — i.e. a *working* independent
counterparty and its differential harness, being deleted while #343 spends 14–22
maintainer-weeks rebuilding one in Java. Python-versus-Java is the strongest possible
form of #343's criterion 6: the two implementations cannot share a type, a helper, or
a transcription. An architecture rule inside one language is weaker than the
separation being discarded.

#343 is right about one part of this and should say so explicitly: `riscv/README.md`
admits the control ROM's "contents are generated by the same Python decode function
that drives the reference emulator, so hardware and reference agree by construction"
— which *is* the self-comparison criterion 6 forbids, for the decode. That argues for
regenerating the decode independently, not for deleting the emulator and the fuzzer.
The corpus of §2(a) is the natural home for what survives.

## 7. What I would keep exactly as written

O3's arithmetic and P8 (66.6 MB against a 64 MiB cap, so the image can never ride
save text) — that is the load-bearing constraint of the whole residence question and
it is derived, not asserted. P6's named-diagnostic requirement, and D15's added
digest-mismatch arm. §6's 12 MiB floor with its two failure modes recorded. The
refusal to put the toolchain build on a required lane. §7.10's statement of the build
as a function of its inputs. And the instinct in §10 not to normalize output to make
a comparison pass.

## Verdict

**endorse-with-reframing.** The task should exist and most of its parts land. But the
oracle and the demo are not the same guest, and merging them is what makes this a
two-week task with an unbounded tail. Split them, drop P3 and H1 from the Definition
of Done into an informational rebuild procedure, promote P5 into a standing
clock-rate-invariance property on the runner, publish the blob through the release
pipeline the project already operates, and update the abstract and Open Question 1 to
reflect D15. What remains is a task whose every completion criterion is inside the
project's control.
