"""Validate each jlsbuild primitive against the real JLS batch simulator."""
import sys
from jlsbuild import Circuit, sel_bits
from jlsrun import run_circuit, BUILD
import os

passed = 0
failed = 0


def check(desc, cond, detail=""):
    global passed, failed
    if cond:
        passed += 1
        print(f"  ok  {desc}")
    else:
        failed += 1
        print(f"FAIL  {desc}   {detail}")


def test_fanout():
    c = Circuit("fanout")
    k = c.constant(5, 4)
    p1 = c.output_pin("a", 4)
    p2 = c.output_pin("b", 4)
    p3 = c.output_pin("cc", 4)
    c.connect(k.out, p1.p("input"), p2.p("input"), p3.p("input"))
    _, err, r = run_circuit(c.emit(), "fanout")
    check("fanout const->3 pins", r["pins"].get("a") == 5 and
          r["pins"].get("b") == 5 and r["pins"].get("cc") == 5, str(r["pins"]) + err)


def test_adder():
    c = Circuit("adder")
    a = c.constant(200, 8)
    b = c.constant(100, 8)
    cin = c.constant(0, 1)
    ad = c.adder(8)
    s = c.output_pin("s", 8)
    co = c.output_pin("co", 1)
    c.connect(a.out, ad.p("A"))
    c.connect(b.out, ad.p("B"))
    c.connect(cin.out, ad.p("Cin"))
    c.connect(ad.p("S"), s.p("input"))
    c.connect(ad.p("Cout"), co.p("input"))
    _, err, r = run_circuit(c.emit(), "adder")
    # 200+100 = 300 = 0x12C -> low 8 bits 0x2C=44, carry 1
    check("adder sum low8", r["pins"].get("s") == 44, str(r["pins"]) + err)
    check("adder carry out", r["pins"].get("co") == 1, str(r["pins"]))


def test_sub_via_adder():
    # A - B = A + ~B + 1
    c = Circuit("sub")
    a = c.constant(50, 8)
    b = c.constant(20, 8)
    nb = c.NOT(8)
    one = c.constant(1, 1)
    ad = c.adder(8)
    s = c.output_pin("d", 8)
    c.connect(b.out, nb.p("input0"))
    c.connect(a.out, ad.p("A"))
    c.connect(nb.out, ad.p("B"))
    c.connect(one.out, ad.p("Cin"))
    c.connect(ad.p("S"), s.p("input"))
    _, err, r = run_circuit(c.emit(), "sub")
    check("subtract 50-20=30", r["pins"].get("d") == 30, str(r["pins"]) + err)


def test_mux():
    c = Circuit("mux")
    m = c.mux(4, 8)
    ins = [c.constant(v, 8) for v in (11, 22, 33, 44)]
    sel = c.constant(2, sel_bits(4))
    o = c.output_pin("m", 8)
    c.connect(sel.out, m.p("select"))
    for i, k in enumerate(ins):
        c.connect(k.out, m.p(f"input{i}"))
    c.connect(m.out, o.p("input"))
    _, err, r = run_circuit(c.emit(), "mux")
    check("mux select 2 -> 33", r["pins"].get("m") == 33, str(r["pins"]) + err)


def test_splitter_binder():
    # split 0xABCD (16 bits) into nibbles and rebind swapped
    c = Circuit("splitbind")
    k = c.constant(0xABCD, 16, base=16)
    sp = c.splitter(16, [(3, 0), (7, 4), (11, 8), (15, 12)])
    bd = c.binder(16, [(3, 0), (7, 4), (11, 8), (15, 12)])
    o = c.output_pin("z", 16)
    c.connect(k.out, sp.p("input"))
    # identity re-bind: splitter output "3-0" -> binder input "3-0", etc.
    for hi, lo in [(3, 0), (7, 4), (11, 8), (15, 12)]:
        nm = f"{hi}-{lo}"
        c.connect(sp.p(nm), bd.p(nm))
    c.connect(bd.out, o.p("input"))
    _, err, r = run_circuit(c.emit(), "splitbind")
    check("split+bind identity 0xABCD", r["pins"].get("z") == 0xABCD,
          hex(r["pins"].get("z", -1)) + " " + err)


def test_sign_extend():
    # take a 4-bit value 0xF (=-1) and sign-extend bit3 to a 16-bit result
    c = Circuit("sext")
    k = c.constant(0xF, 4, base=16)
    sp = c.splitter(4, [(3, 3), (2, 0)])   # sign bit and low 3 bits
    ext = c.extend(13)                     # replicate sign into 13 bits (bits 3..15)
    bd = c.binder(16, [(2, 0), (15, 3)])   # low3 -> bits0..2, ext(13) -> bits3..15
    o = c.output_pin("z", 16)
    c.connect(k.out, sp.p("input"))
    c.connect(sp.p("3"), ext.p("input0"))
    c.connect(sp.p("2-0"), bd.p("2-0"))
    c.connect(ext.out, bd.p("15-3"))
    c.connect(bd.out, o.p("input"))
    _, err, r = run_circuit(c.emit(), "sext")
    check("sign-extend 0xF(4b) -> 0xFFFF", r["pins"].get("z") == 0xFFFF,
          hex(r["pins"].get("z", -1)) + " " + err)


def test_shifter():
    for kind, val, amt, bits, exp in [
        ("LogicalLeft", 0x01, 4, 8, 0x10),
        ("LogicalRight", 0x80, 3, 8, 0x10),
        ("ArithmeticRight", 0x80, 3, 8, 0xF0),  # sign bit set -> fill ones
    ]:
        c = Circuit("shift")
        v = c.constant(val, bits, base=16)
        a = c.constant(amt, sel_bits(bits))
        sh = c.shifter(kind, bits)
        o = c.output_pin("z", bits)
        c.connect(v.out, sh.p("input"))
        c.connect(a.out, sh.p("amount"))
        c.connect(sh.out, o.p("input"))
        _, err, r = run_circuit(c.emit(), "shift")
        check(f"{kind} {hex(val)}>>{amt} = {hex(exp)}",
              r["pins"].get("z") == exp, hex(r["pins"].get("z", -1)) + " " + err)


def test_decoder():
    c = Circuit("dec")
    k = c.constant(3, 3)
    d = c.decoder(3)
    o = c.output_pin("z", 8)
    c.connect(k.out, d.p("input"))
    c.connect(d.out, o.p("input"))
    _, err, r = run_circuit(c.emit(), "dec")
    check("decoder 3 -> one-hot bit3 (0x08)", r["pins"].get("z") == 0x08,
          hex(r["pins"].get("z", -1)) + " " + err)


def test_rom():
    c = Circuit("rom")
    rom = c.memory("m", 32, 8, kind="ROM", init="0 aa\\n1 bb\\n2 cc")
    addr = c.constant(2, sel_bits(8))
    lo = c.constant(0, 1)
    o = c.output_pin("z", 32)
    c.connect(addr.out, rom.p("address"))
    c.connect(lo.out, rom.p("CS"))
    c.connect(lo.out, rom.p("OE"))
    c.connect(rom.out, o.p("input"))
    _, err, r = run_circuit(c.emit(), "rom")
    check("rom[2] = 0xcc", r["pins"].get("z") == 0xcc,
          hex(r["pins"].get("z", -1)) + " " + err)


def test_counter():
    # a register that increments each clock; watch it, run 5 cycles
    c = Circuit("counter")
    clk = c.clock(1000)
    reg = c.register("cnt", 8, init=0, kind="pff", watch=1)
    one = c.constant(1, 8)
    ad = c.adder(8)
    czero = c.constant(0, 1)
    c.connect(reg.p("Q"), ad.p("A"))
    c.connect(one.out, ad.p("B"))
    c.connect(czero.out, ad.p("Cin"))
    c.connect(ad.p("S"), reg.p("D"))
    c.connect(clk.out, reg.p("C"))
    # run ~5.5 cycles
    _, err, r = run_circuit(c.emit(), "counter", time_limit=5500)
    # after N rising edges the register holds N
    check("counter increments (reg in 4..6)",
          4 <= r["regs"].get("cnt", -1) <= 6, str(r["regs"]) + err)


def test_ram_write():
    # drive a RAM write with a clock-gated WE, then read back not needed --
    # watch RAM changed locations.  WE active low; gate: WE = NOT(clk) so it
    # writes while clock high... simpler: static write like the golden test.
    c = Circuit("ram")
    ram = c.memory("dm", 32, 8, kind="RAM", watch=1)
    addr = c.constant(3, sel_bits(8))
    data = c.constant(0x1234, 32, base=16)
    lo = c.constant(0, 1)
    hi = c.constant(1, 1)
    c.connect(addr.out, ram.p("address"))
    c.connect(data.out, ram.p("input"))
    c.connect(lo.out, ram.p("CS"))
    c.connect(lo.out, ram.p("WE"))
    c.connect(hi.out, ram.p("OE"))
    _, err, r = run_circuit(c.emit(), "ram")
    check("ram[3] = 0x1234", r["mem"].get("dm", {}).get(3) == 0x1234,
          str(r["mem"]) + err)


if __name__ == "__main__":
    for t in [test_fanout, test_adder, test_sub_via_adder, test_mux,
              test_splitter_binder, test_sign_extend, test_shifter,
              test_decoder, test_rom, test_counter, test_ram_write]:
        print(f"== {t.__name__} ==")
        try:
            t()
        except Exception as e:
            failed += 1
            print(f"FAIL  {t.__name__} raised {type(e).__name__}: {e}")
    print(f"\n{passed} passed, {failed} failed")
    sys.exit(1 if failed else 0)
