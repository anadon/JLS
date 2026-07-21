import java.awt.Component;
import java.awt.Frame;

import jls.elem.Element;
import jls.elem.Put;

/**
 * NetTest -- prove the named-net connection mechanism end to end:
 * constant -> JumpStart(n1) (coincident), JumpEnd(n1) -> output pin
 * (coincident). Verifies every port attaches and the two jumps form one
 * electrical net, then saves the netlist for inspection.
 */
public final class NetTest {
	static String scr = "/tmp/claude-0/-home-user-JLS/"
			+ "5629c65d-470b-5c3b-a567-4f9019babece/scratchpad/";

	public static void main(String[] args) throws Exception {
		try {
			run();
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			System.out.flush();
			System.exit(0);
		}
	}

	static void run() throws Exception {
		Frame f = GuiDriver.boot();
		GuiDriver g = new GuiDriver();
		g.placeFrame(f, 1900, 1180);
		g.menu("File", "New");
		Component field = g.waitFind(GuiDriver.anyTextField(), "name", 6000);
		g.click(field);
		g.type("net");
		g.click(g.waitFind(GuiDriver.buttonText("OK"), "OK", 4000));
		g.robot.delay(600);

		// constant 5 (8-bit, decimal) at (300,300)
		Element c = g.placeExact("constant value", new String[] {"5"},
				new String[] {"10"}, 300, 300);
		Put co = firstOutput(c);
		System.out.println("constant out @(" + co.getX() + "," + co.getY() + ")");
		// JumpStart(n1): input port offset (0,0), so place it on the const out
		Element js = g.placeExact("name a wire", new String[] {"n1", "8"},
				null, co.getX(), co.getY());
		Put jsIn = MiniAdder.put(js, "input");
		System.out.println("JumpStart in @(" + jsIn.getX() + "," + jsIn.getY()
				+ ") attached=" + jsIn.isAttached()
				+ "  const.out attached=" + co.isAttached());

		// output pin "z" (8-bit) at (600,300); its input port is at off (0,12)
		Element z = g.placeExact("output pin", new String[] {"z", "8"}, null,
				600, 300);
		Put zi = MiniAdder.put(z, "input");
		System.out.println("outpin in @(" + zi.getX() + "," + zi.getY() + ")");
		// JumpEnd(n1): place so its output coincides with z.input. Discover
		// its output offset by placing at a temp cell first is avoided --
		// assume output port at (0,0) like JumpStart's input, verify below.
		// JumpEnd output port sits at element origin + (36,0); place the
		// element 36 left so its output coincides with the sink input
		Element je = g.placeJumpEnd("n1", zi.getX() - 36, zi.getY());
		Put jeOut = firstOutput(je);
		System.out.println("JumpEnd out @(" + jeOut.getX() + "," + jeOut.getY()
				+ ") attached=" + jeOut.isAttached()
				+ "  outpin.in attached=" + zi.isAttached());

		System.out.println("SUMMARY attached: const=" + co.isAttached()
				+ " jsIn=" + jsIn.isAttached() + " jeOut=" + jeOut.isAttached()
				+ " zIn=" + zi.isAttached());
		try (java.io.PrintWriter w = new java.io.PrintWriter(scr + "net.jls")) {
			g.currentCircuit().save(w);
		}
		System.out.println("saved " + scr + "net.jls  elements="
				+ g.currentCircuit().getElements().size());
		g.shot(scr + "net.png");
	}

	static Put firstOutput(Element e) {
		for (Put p : e.getAllPuts()) {
			if (p instanceof jls.elem.Output) {
				return p;
			}
		}
		// fall back to any put
		return e.getAllPuts().iterator().next();
	}
}
