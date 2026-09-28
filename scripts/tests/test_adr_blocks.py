#!/usr/bin/env python3
"""The three issue templates carry one ADR-1 block each, duplicated on
purpose so a reader never follows an indirection. Nothing else keeps the
copies aligned, so this test does:

  - the `adr:` version is the same in all three;
  - the YAML machine keys of the block are the same, in the same order;
  - the nine axes appear in the same order with the same anchor numbers
    (0–5 per axis, plus the ED and DA cap lines);
  - the band thresholds and the debt-tag names and triggers agree.

Run: python3 scripts/tests/test_adr_blocks.py  (exit 0 = pass)
Stdlib only, plain asserts.
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
TDIR = os.path.join(ROOT, ".github", "ISSUE_TEMPLATE")
FILES = {"task": "scientific_task.md", "feature": "feature.md",
         "capstone": "capstone.md"}

def adr_section(text):
    i = text.index("## Agentic Delegability")
    return text[i:]


def yaml_block(section):
    m = re.search(r"```yaml\n(.*?)```", section, re.S)
    assert m, "no yaml block in the ADR section"
    keys, version = [], None
    for line in m.group(1).splitlines():
        km = re.match(r"([a-z_]+):", line)
        if km:
            keys.append(km.group(1))
        vm = re.match(r"adr:\s*(\d+)", line)
        if vm:
            version = int(vm.group(1))
    return version, keys


def axes(section):
    """[(code, [anchor numbers in order])] for the nine axes."""
    out, cur = [], None
    for line in section.splitlines():
        m = re.match(r"  ([A-Z]{2})  [A-Z /]+ —", line)
        if m:
            cur = (m.group(1), [])
            out.append(cur)
            continue
        if cur and re.match(r"\s{4,8}([0-5]) \S", line):
            cur[1].append(int(re.match(r"\s+([0-5])", line).group(1)))
        if line.startswith("  BAND"):
            cur = None
    return out


def band_and_tags(section):
    """Band thresholds and the debt-tag names with their triggers."""
    i = section.index("  BAND")
    j = section.index("```yaml", i)
    body = section[i:j]
    out = []
    for line in body.splitlines():
        if line.startswith("  BAND") or line.startswith("  FINAL BAND"):
            out.append(line.strip())
        m = re.match(r"    ([A-Z][A-Z-]+)\s+(\S.*)$", line)
        if m:
            trig = m.group(2).strip()
            # the trigger is the axis expression before the description
            tm = re.match(r"((?:[A-Z]{2}[<=>]+\d|nothing above|or the 2-point|"
                          r"[A-Z]{2}=\d)[^ ]*(?: or [A-Z]{2}[<=>]+\d)?)", trig)
            out.append((m.group(1), tm.group(1) if tm else trig.split("  ")[0]))
    return out


def main():
    secs = {t: adr_section(open(os.path.join(TDIR, f)).read())
            for t, f in FILES.items()}
    versions, keys = {}, {}
    for t, s in secs.items():
        versions[t], keys[t] = yaml_block(s)
    assert len(set(versions.values())) == 1, f"adr: versions differ: {versions}"
    # `roster_delegable` exists only where there is a roster.
    for t in ("feature", "capstone"):
        assert "roster_delegable" in keys[t], f"{t}: roster_delegable missing"
    common = {t: [k for k in ks if k != "roster_delegable"]
              for t, ks in keys.items()}
    assert len({tuple(k) for k in common.values()}) == 1, \
        f"ADR yaml keys differ: {common}"

    ax = {t: axes(s) for t, s in secs.items()}
    codes = {t: [c for c, _ in a] for t, a in ax.items()}
    assert len({tuple(c) for c in codes.values()}) == 1, \
        f"axis order differs: {codes}"
    assert codes["task"] == ["SC", "OS", "BR", "HL", "PD", "CF", "RD",
                             "ED", "DA"], codes["task"]
    for i, code in enumerate(codes["task"]):
        nums = {t: ax[t][i][1] for t in FILES}
        assert len({tuple(n) for n in nums.values()}) == 1, \
            f"axis {code} anchors differ: {nums}"
        assert nums["task"], f"axis {code} has no anchors"

    bt = {t: band_and_tags(s) for t, s in secs.items()}
    for t in ("feature", "capstone"):
        diff = [(a, b) for a, b in zip(bt["task"], bt[t]) if a != b]
        assert len(bt["task"]) == len(bt[t]) and not diff, \
            f"band table / debt tags differ (task vs {t}): {diff[:3]}"

    print(f"adr blocks: OK (adr {versions['task']}, "
          f"{len(codes['task'])} axes, {len(keys['task'])} keys)")


if __name__ == "__main__":
    main()
