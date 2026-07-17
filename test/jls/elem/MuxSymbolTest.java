package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;

/**
 * Pins the Mux body symbol as the conventional trapezoid (issue #123):
 * wide side at the inputs, narrow side at the output, in every output
 * orientation. Rendering is probed at a handful of geometrically
 * meaningful points (corners and slant lattice points) rather than compared
 * to a pixel golden, per the jls.ui harness discipline (issue #91): the
 * probes are exact integer line endpoints, deterministic under Java2D's
 * default (non-antialiased) 1px stroke on any platform.
 *
 * The probe coordinates are written out per orientation by hand - NOT
 * derived through GridTransform - so this test is an independent check
 * of the canonical-geometry drawing (#24), not a tautology.
 *
 * Selector orientations are the pairings the creation dialog itself
 * offers (MuxCreate.actionPerformed: Up/Down selectors for Left/Right
 * outputs, Left/Right selectors for Up/Down outputs), so every fixture
 * is UI-creatable, and rotate()/flip() preserve that perpendicularity.
 * Every probe is at least 9 px from every put center; the put ring is
 * a 6x6 fillOval (radius pointDiameter/2 = 3), so no probe is within
 * 6 px of any put ink. Where a slant's midpoint would fall near the
 * selector put, the probe is another exact lattice point on the same
 * slant instead. All probes are also clear of the input-number labels,
 * so font metrics (#111) cannot move them.
 */
class MuxSymbolTest {

	/** Element origin inside the test image; on the snap grid. */
	private static final int EX = 60, EY = 60;

	private static String muxCircuit(String iOrient, String sOrient) {
		return "CIRCUIT sym\nELEMENT Mux\n int id 0\n int x " + EX
				+ "\n int y " + EY + "\n int inputs 4\n int bits 1\n"
				+ " int delay 10\n String iOrient \"" + iOrient
				+ "\"\n String sOrient \"" + sOrient + "\"\nEND\nENDCIRCUIT\n";
	}

	/** Load a 4-input Mux headlessly and paint it onto a white image. */
	private static BufferedImage render(String iOrient, String sOrient)
			throws Exception {
		Circuit circuit = new Circuit("sym");
		assertTrue(circuit.load(new Scanner(muxCircuit(iOrient, sOrient))),
				() -> "load failed: " + JLSInfo.loadError);
		BufferedImage img = new BufferedImage(160, 160, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
		assertTrue(circuit.finishLoad(g),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		Element mux = null;
		for (Element e : circuit.getElements()) {
			if (e instanceof Mux) {
				mux = e;
			}
		}
		assertNotNull(mux, "Mux not found after load");
		mux.draw(g);
		g.dispose();
		return img;
	}

	private static void assertInk(BufferedImage img, int dx, int dy, String what) {
		assertEquals(Color.BLACK.getRGB(), img.getRGB(EX + dx, EY + dy),
				what + " should be drawn at element offset (" + dx + "," + dy + ")");
	}

	private static void assertNoInk(BufferedImage img, int dx, int dy, String what) {
		assertNotEquals(Color.BLACK.getRGB(), img.getRGB(EX + dx, EY + dy),
				what + " should NOT be drawn at element offset (" + dx + "," + dy + ")");
	}

	// A 4-input Mux is 24x60 horizontal (RIGHT/LEFT) and 60x24 vertical
	// (UP/DOWN); spacing=12, so the trapezoid slant is 6 and every
	// slant line has slope +-1/4: its exact lattice points sit 4 px
	// apart along the long axis, so each slant offers points >= 9 px
	// from the selector put.

	@Test
	void rightFacingMuxIsATrapezoid() throws Exception {
		BufferedImage img = render("RIGHT", "DOWN");
		assertInk(img, 0, 0, "wide-side (input) corner");
		assertInk(img, 0, 60, "wide-side (input) corner");
		assertInk(img, 24, 6, "narrow-side (output) corner, inset by the slant");
		assertInk(img, 24, 54, "narrow-side (output) corner, inset by the slant");
		assertInk(img, 12, 3, "top slant midpoint");
		// bottom slant (0,60)-(24,54): lattice point away from the
		// selector put at (12,60)
		assertInk(img, 20, 55, "bottom slant point");
		assertNoInk(img, 12, 0, "bounding-box top midpoint (old arc apex)");
	}

	@Test
	void leftFacingMuxIsATrapezoid() throws Exception {
		BufferedImage img = render("LEFT", "DOWN");
		assertInk(img, 24, 0, "wide-side (input) corner");
		assertInk(img, 24, 60, "wide-side (input) corner");
		assertInk(img, 0, 6, "narrow-side (output) corner, inset by the slant");
		assertInk(img, 0, 54, "narrow-side (output) corner, inset by the slant");
		assertInk(img, 12, 3, "top slant midpoint");
		// bottom slant (24,60)-(0,54): lattice point away from the
		// selector put at (12,60)
		assertInk(img, 4, 55, "bottom slant point");
		assertNoInk(img, 12, 0, "bounding-box top midpoint (old arc apex)");
	}

	@Test
	void upFacingMuxIsATrapezoid() throws Exception {
		BufferedImage img = render("UP", "RIGHT");
		assertInk(img, 0, 24, "wide-side (input) corner");
		assertInk(img, 60, 24, "wide-side (input) corner");
		assertInk(img, 6, 0, "narrow-side (output) corner, inset by the slant");
		assertInk(img, 54, 0, "narrow-side (output) corner, inset by the slant");
		assertInk(img, 3, 12, "left slant midpoint");
		// right slant (54,0)-(60,24): lattice point away from the
		// selector put at (60,12)
		assertInk(img, 55, 4, "right slant point");
		assertNoInk(img, 0, 12, "bounding-box left midpoint (old arc apex)");
	}

	@Test
	void downFacingMuxIsATrapezoid() throws Exception {
		BufferedImage img = render("DOWN", "RIGHT");
		assertInk(img, 0, 0, "wide-side (input) corner");
		assertInk(img, 60, 0, "wide-side (input) corner");
		assertInk(img, 6, 24, "narrow-side (output) corner, inset by the slant");
		assertInk(img, 54, 24, "narrow-side (output) corner, inset by the slant");
		assertInk(img, 3, 12, "left slant midpoint");
		// right slant (60,0)-(54,24): lattice point away from the
		// selector put at (60,12)
		assertInk(img, 55, 20, "right slant point");
		assertNoInk(img, 0, 12, "bounding-box left midpoint (old arc apex)");
	}
}
