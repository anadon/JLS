import java.awt.Component;
import java.awt.Frame;

/**
 * Probe -- an exploratory driver run: open a new circuit, click a palette
 * element, and dump the creation dialog's real widget layout so the CPU
 * builder knows exactly which fields to fill and in what order. Reading the
 * UI, not building anything.
 */
public final class Probe {

	public static void main(String[] args) throws Exception {
		String scr = "/tmp/claude-0/-home-user-JLS/"
				+ "5629c65d-470b-5c3b-a567-4f9019babece/scratchpad/";
		String tip = args.length > 0 ? args[0] : "adder";

		Frame f = GuiDriver.boot();
		GuiDriver g = new GuiDriver();
		g.placeFrame(f, 1900, 1180);

		g.menu("File", "New");
		Component field = g.waitFind(GuiDriver.anyTextField(),
				"name field", 6000);
		g.click(field);
		g.type("riscv");
		Component ok = g.waitFind(GuiDriver.buttonText("OK"), "OK", 4000);
		g.click(ok);
		g.robot.delay(600);

		Component canvas = g.canvas();
		System.out.println("circuit = "
				+ (g.currentCircuit() == null ? "null"
						: g.currentCircuit().getName()));
		if (canvas != null) {
			System.out.println("canvas loc="
					+ canvas.getLocationOnScreen() + " size="
					+ canvas.getSize());
		} else {
			System.out.println("canvas = null");
		}

		// click the palette element button by tooltip
		Component palette = g.waitFind(GuiDriver.paletteTip(tip),
				"palette " + tip, 5000);
		System.out.println("palette '" + tip + "' at "
				+ palette.getLocationOnScreen());
		g.click(palette);
		g.robot.delay(800);

		g.dumpDialogs();
		g.shot(scr + "probe-" + tip.replaceAll("[^a-z]", "") + "-dialog.png");
		System.out.flush();
		System.exit(0);
	}
}
