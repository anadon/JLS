"""Helpers to run a JLS circuit in batch mode and parse the watched output."""
import os
import re
import subprocess
import glob

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BUILD = os.path.join(os.path.dirname(os.path.abspath(__file__)), "build")
os.makedirs(BUILD, exist_ok=True)


def jar_path() -> str:
    jars = [j for j in glob.glob(os.path.join(ROOT, "target", "jls-*.jar"))
            if "original" not in j]
    if not jars:
        raise RuntimeError("JLS jar not built; run mvn package -DskipTests")
    return jars[0]


_PIN = re.compile(r"^Output Pin (.+?): 0x([0-9A-Fa-f]+) \((\d+) unsigned, (-?\d+) signed\)$")
_REG = re.compile(r"^Register (.+?): 0x([0-9A-Fa-f]+) \((\d+) unsigned, (-?\d+) signed\)$")
_HIZ_PIN = re.compile(r"^Output Pin (.+?): HiZ$")
_MEMHEAD = re.compile(r"^Changed locations in memory (.+)$")
_MEMLINE = re.compile(r"^ 0x([0-9a-f]+): .* -> 0x([0-9A-Fa-f]+) \((\d+) unsigned")


def run_circuit(text: str, name: str = "c", time_limit: int = 200000,
                testfile: str = None, extra_args=None):
    """Run circuit text in batch mode.  Returns (stdout, stderr, parsed).

    parsed = {"pins": {name: unsigned}, "regs": {name: unsigned},
              "mem": {memname: {word_addr: unsigned}}, "outcome": str,
              "raw": stdout}."""
    path = os.path.join(BUILD, name + ".jls")
    with open(path, "w") as f:
        f.write(text)
    cmd = ["java", "-jar", jar_path(), "-b", "-d", str(time_limit)]
    if testfile:
        cmd += ["-t", testfile]
    if extra_args:
        cmd += extra_args
    cmd += [path]
    r = subprocess.run(cmd, capture_output=True, text=True, timeout=600)
    out = "\n".join(l for l in r.stdout.splitlines() if "Picked up" not in l)
    err = "\n".join(l for l in r.stderr.splitlines() if "Picked up" not in l)
    parsed = {"pins": {}, "regs": {}, "mem": {}, "outcome": None, "raw": out,
              "hiz": set(), "rc": r.returncode}
    curmem = None
    for line in out.splitlines():
        m = _PIN.match(line)
        if m:
            parsed["pins"][m.group(1)] = int(m.group(3))
            curmem = None
            continue
        m = _HIZ_PIN.match(line)
        if m:
            parsed["hiz"].add(m.group(1))
            curmem = None
            continue
        m = _REG.match(line)
        if m:
            parsed["regs"][m.group(1)] = int(m.group(3))
            curmem = None
            continue
        m = _MEMHEAD.match(line)
        if m:
            curmem = m.group(1)
            parsed["mem"][curmem] = {}
            continue
        m = _MEMLINE.match(line)
        if m and curmem is not None:
            parsed["mem"][curmem][int(m.group(1), 16)] = int(m.group(3))
            continue
        if line.startswith("No changes in memory"):
            curmem = None
            continue
        if any(line.startswith(k) for k in
               ("Simulation", )):
            parsed["outcome"] = line
    return out, err, parsed
