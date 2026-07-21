import java.awt.Component;
import java.awt.Frame;

import jls.elem.Element;
import jls.elem.Put;

/**
 * RiscvCpu -- a minimal single-cycle RISC-V (RV32I) accumulator CPU built as
 * a real JLS circuit, STRICTLY THROUGH THE GUI (real java.awt.Robot input
 * driving the live editor; see riscv/gui/GuiDriver.java).
 *
 * Datapath, clocked, executing the RV32I instruction "addi x1, x1, K":
 *
 *     program counter                    accumulator (x1)
 *     ---------------                    ----------------
 *     PC(8b) --+--> [PC + 1] --> PC       ACC(32b) --+--> [ACC + K] --> ACC
 *              |                                     |
 *     clock ---+-------------------------------------+
 *
 * Every rising clock edge the machine executes one addi: the ALU adds the
 * immediate K to x1 and writes it back, while the program counter advances to
 * the next instruction. After N cycles x1 == N*K and PC == N -- exactly what
 * the reference emulator (riscv_ref.py) computes for N addi instructions.
 *
 * Every element and every connection here is placed and wired by real GUI
 * gestures: elements chosen from the palette and configured in their real
 * creation dialogs, connections made by coincidence and named-wire jumps, and
 * the observed registers marked watched with the editor's watch action -- no
 * .jls text is authored by hand.
 *
 * Scope note: this accumulator uses a single hardwired addi immediate (a
 * visible Constant on the canvas) rather than a program memory selected by the
 * PC. A PC-indexed instruction memory (Mux- or ROM-based) needs mid-body
 * selector/enable ports (Mux.select, Memory.CS/OE, ShiftRegister.amount) that
 * JLS's coincidence/overlap rules do not let an automated driver connect
 * without free-hand wire routing; that boundary is documented in the README.
 */
public final class RiscvCpu {

	static final String SCR = GuiDriver.outDir();
	static final int IMM = 3; // addi x1, x1, 3
	static GuiDriver g;

	public static void main(String[] a) throws Exception {
		try {
			run();
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			System.out.flush();
			System.exit(0);
		}
	}

	static Put out(Element e) {
		for (Put p : e.getAllPuts()) {
			if (p instanceof jls.elem.Output) {
				return p;
			}
		}
		return e.getAllPuts().iterator().next();
	}

	static Put in(Element e, String name) {
		for (Put p : e.getAllPuts()) {
			if (name.equals(p.getName())) {
				return p;
			}
		}
		throw new RuntimeException("no port " + name + " on " + e);
	}

	/** One named net: a JumpStart coincident on the driver output, then a
	 *  JumpEnd coincident on each sink input. */
	static void net(String name, int bits, Put driver, Put... sinks)
			throws Exception {
		g.placeExact("name a wire", new String[] {name, String.valueOf(bits)},
				null, driver.getX(), driver.getY());
		for (Put s : sinks) {
			g.placeJumpEnd(name, s.getX(), s.getY());
		}
	}

	static void run() throws Exception {
		Frame f = GuiDriver.boot();
		g = new GuiDriver();
		g.placeFrame(f, 1900, 1180);
		g.menu("File", "New");
		Component fld = g.waitFind(GuiDriver.anyTextField(), "name", 6000);
		g.click(fld);
		g.type("riscvcpu");
		g.click(g.waitFind(GuiDriver.buttonText("OK"), "OK", 4000));
		g.robot.delay(600);

		// ---- elements (all coordinates are multiples of the 12px grid) ----
		// a slow clock (cycle 2000, half-period 1000) so combinational
		// signals settle before each rising edge -- matches build_cpu.py
		Element clk = g.placeExact("clock", new String[] {"2000", "1000"},
				null, 120, 120);

		// program counter: PC register + increment adder + constant 1
		Element pc = g.placeExact("register (various triggering)",
				new String[] {"PC", "8", "0"}, new String[] {"Pos-Trig"},
				360, 300);
		g.watch(pc);
		Element pcAdd = g.placeExact("adder", new String[] {"8"}, null,
				360, 600);
		Element one = g.placeExact("constant value", new String[] {"1"},
				new String[] {"10"}, 120, 720);

		// accumulator (x1): ACC register + ALU adder + immediate constant
		Element acc = g.placeExact("register (various triggering)",
				new String[] {"ACC", "32", "0"}, new String[] {"Pos-Trig"},
				1080, 300);
		g.watch(acc);
		Element alu = g.placeExact("adder", new String[] {"32"}, null,
				1080, 600);
		Element imm = g.placeExact("constant value",
				new String[] {String.valueOf(IMM)}, new String[] {"10"},
				840, 720);

		System.out.println("elements placed = "
				+ g.currentCircuit().getElements().size());

		// ---- nets (all side ports; adder Cin defaults to 0 unconnected) ----
		net("clk", 1, out(clk), in(pc, "C"), in(acc, "C"));
		net("pcq", 8, in(pc, "Q"), in(pcAdd, "A"));
		net("one", 8, out(one), in(pcAdd, "B"));
		net("pcnext", 8, in(pcAdd, "S"), in(pc, "D"));
		net("accq", 32, in(acc, "Q"), in(alu, "A"));
		net("imm", 32, out(imm), in(alu, "B"));
		net("accnext", 32, in(alu, "S"), in(acc, "D"));

		// ---- report + save ----
		int attached = 0, total = 0;
		for (Element e : new Element[] {clk, pc, pcAdd, one, acc, alu, imm}) {
			for (Put p : e.getAllPuts()) {
				String nm = p.getName();
				// Cin/Cout/notQ are intentionally left unconnected
				boolean expected = !(nm.equals("Cin") || nm.equals("Cout")
						|| nm.equals("notQ"));
				total++;
				if (p.isAttached()) {
					attached++;
				} else if (expected) {
					System.out.println("UNATTACHED " + e.getClass()
							.getSimpleName() + "." + nm);
				}
			}
		}
		System.out.println("ports attached " + attached + "/" + total
				+ " (Cin/Cout/notQ intentionally open)");
		System.out.println("PC watched=" + pc.isWatched()
				+ " ACC watched=" + acc.isWatched());
		try (java.io.PrintWriter w = new java.io.PrintWriter(SCR + "cpu.jls")) {
			g.currentCircuit().save(w);
		}
		System.out.println("saved " + SCR + "cpu.jls");
		g.shot(SCR + "cpu.png");
	}
}
