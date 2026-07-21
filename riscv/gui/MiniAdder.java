import java.awt.Component;
import java.awt.Frame;

import jls.elem.Element;
import jls.elem.Put;

/**
 * MiniAdder -- end-to-end proof of the construction pipeline: build an
 * 8-bit adder (two input pins, an Adder, an output pin) entirely through
 * real GUI input, then report every element's pins so wiring can follow.
 */
public final class MiniAdder {

	static String scr = "/tmp/claude-0/-home-user-JLS/"
			+ "5629c65d-470b-5c3b-a567-4f9019babece/scratchpad/";

	public static void main(String[] args) throws Exception {
		Frame f = GuiDriver.boot();
		GuiDriver g = new GuiDriver();
		g.placeFrame(f, 1900, 1180);

		g.menu("File", "New");
		Component field = g.waitFind(GuiDriver.anyTextField(), "name", 6000);
		g.click(field);
		g.type("adder8");
		g.click(g.waitFind(GuiDriver.buttonText("OK"), "OK", 4000));
		g.robot.delay(600);

		Element a = g.placeElement("input pin",
				new String[] {"a", "8"}, null, 240, 300);
		Element b = g.placeElement("input pin",
				new String[] {"b", "8"}, null, 240, 420);
		Element add = g.placeElement("adder",
				new String[] {"8"}, null, 480, 348);
		Element s = g.placeElement("output pin",
				new String[] {"s", "8"}, null, 720, 360);

		g.shot(scr + "mini-placed.png");

		Put ao = put(a, "output");
		Put bo = put(b, "output");
		Put addA = put(add, "A");
		Put addB = put(add, "B");
		Put addS = put(add, "S");
		Put si = put(s, "input");

		g.wire(ao, addA);
		g.wire(bo, addB);
		g.wire(addS, si);
		g.shot(scr + "mini-wired.png");

		System.out.println("attached: a.output=" + ao.isAttached()
				+ " b.output=" + bo.isAttached()
				+ " add.A=" + addA.isAttached() + " add.B=" + addB.isAttached()
				+ " add.S=" + addS.isAttached() + " s.input=" + si.isAttached());
		System.out.println("total elements (with wires) = "
				+ g.currentCircuit().getElements().size());

		// persist what the GUI built so we can inspect the netlist
		try (java.io.PrintWriter w = new java.io.PrintWriter(scr + "adder8.jls")) {
			g.currentCircuit().save(w);
		}
		System.out.println("saved " + scr + "adder8.jls");
		System.out.flush();
		System.exit(0);
	}

	static Put put(Element e, String name) {
		for (Put p : e.getAllPuts()) {
			if (name.equals(p.getName())) {
				return p;
			}
		}
		throw new RuntimeException("no put " + name + " on " + e);
	}
}
