import java.awt.Component;
import java.awt.Frame;

import jls.elem.Element;
import jls.elem.Put;

/**
 * ConnTest -- verify exact keyboard placement and test coincidence
 * connection: drop an adder so its A input lands exactly on an input pin's
 * output, and check the two auto-connect on placement (no wire drawn).
 */
public final class ConnTest {
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
		g.type("conn");
		g.click(g.waitFind(GuiDriver.buttonText("OK"), "OK", 4000));
		g.robot.delay(600);

		// exact placement of an input pin (bits 1) at (300,300)
		Element a = g.placeExact("input pin", new String[] {"a", "1"}, null,
				300, 300);
		Put ao = MiniAdder.put(a, "output");
		System.out.println("input a @(" + a.getX() + "," + a.getY()
				+ ") output put @(" + ao.getX() + "," + ao.getY() + ")");

		// place the adder (bits 1) so its A input coincides with a.output.
		// A sits at (elemX, elemY+12); to land A on a.output, place the adder
		// at (ao.x, ao.y-12).
		int adX = ao.getX();
		int adY = ao.getY() - 12;
		Element add = g.placeExact("adder", new String[] {"1"}, null, adX, adY);
		Put addA = MiniAdder.put(add, "A");
		System.out.println("adder @(" + add.getX() + "," + add.getY()
				+ ") A put @(" + addA.getX() + "," + addA.getY() + ")");
		System.out.println("coincide? a.output(" + ao.getX() + "," + ao.getY()
				+ ") vs adder.A(" + addA.getX() + "," + addA.getY() + ")");
		System.out.println("COINCIDENCE CONNECT: a.output attached="
				+ ao.isAttached() + " adder.A attached=" + addA.isAttached());
		g.shot(scr + "conn.png");
	}
}
