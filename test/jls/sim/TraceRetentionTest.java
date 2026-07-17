package jls.sim;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.BitSet;

import org.junit.jupiter.api.Test;

import jls.JLSInfo;

/**
 * History retention at the Trace model level (issue #121, O1/P2):
 * values older than the panel width used to be discarded as they
 * arrived ({@code pendingChanges.removeLast()}), so a finished run
 * could not be scrolled back.  Post-fix a trace retains a documented
 * count of changes independent of the panel width.
 *
 * <p>Deliberately written against the pre-#121 public surface
 * (addValue/commit/paintComponent + pixels) so the same source
 * compiles at the pre-change commit, where the first two tests fail
 * (rule-3 regression evidence).</p>
 */
class TraceRetentionTest {

	private static final int HEIGHT = 40;

	/** The documented retention cap; literal (not a reference to
	 *  Trace.MAX_RETAINED_CHANGES) so this file compiles at the
	 *  pre-change commit. */
	private static final int CAP = 100_000;

	/**
	 * A real Traces parent (nameSpace 0), built with the GUI-free
	 * batch path so the test stays headless.
	 */
	private static InteractiveSimulator.Traces parent() {

		boolean oldBatch = JLSInfo.batch;
		JLSInfo.batch = true;
		try {
			return new InteractiveSimulator().new Traces();
		}
		finally {
			JLSInfo.batch = oldBatch;
		}
	}

	private static BitSet bit(boolean on) {

		BitSet value = new BitSet(2);
		if (on)
			value.set(0);
		return value;
	}

	private static BufferedImage paint(Trace trace, int width) {

		BufferedImage image = new BufferedImage(width, HEIGHT,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, width, HEIGHT);
		trace.paintComponent(g);
		g.dispose();
		return image;
	}

	/** Any non-white pixel in columns [x0,x1]? */
	private static boolean hasInk(BufferedImage image, int x0, int x1) {

		int white = Color.white.getRGB();
		for (int x = x0; x <= x1; x++)
			for (int y = 0; y < image.getHeight(); y++)
				if (image.getRGB(x, y) != white)
					return true;
		return false;
	}

	@Test
	void historyBeyondThePanelWidthSurvivesForScrollback() {

		// values arrive while the panel is 510px wide...
		Trace trace = new Trace("sig", null, 1, 500, parent());
		trace.setSize(510, HEIGHT);
		int last = 5000;
		for (int t = 1; t <= last; t++)
			trace.addValue(bit(t % 2 == 0), t);
		trace.commit(last);

		// ...then the run is over and the viewer grows for scrollback
		trace.setSize(last + 100, HEIGHT);
		BufferedImage image = paint(trace, last + 100);

		// drawing width w = 5090; time t sits at x = w-(5000-t) = 90+t,
		// so x in [195,205] is t in [105,115] - early history.  The
		// pre-#121 width cap kept only t >= 4491, leaving this blank.
		// (Tic gridlines are at x = 90+50k: 190 and 240 both excluded.)
		assertTrue(hasInk(image, 195, 205),
				"activity at t~110 must still be drawn after a "
						+ last + "-step run");

		// sanity: recent history is drawn in both versions
		assertTrue(hasInk(image, 4700, 4710),
				"recent history must be drawn");
	}

	@Test
	void retentionIsBoundedAtTheDocumentedCap() {

		// a panel wider than the whole run: the pre-#121 width cap
		// never binds, so at the pre-change commit everything is
		// retained and the dropped-region assertion below fails
		int total = CAP + 50;
		int panel = total + 9960;
		Trace trace = new Trace("sig", null, 1, 500, parent());
		trace.setSize(panel, HEIGHT);
		for (int t = 1; t <= total; t++)
			trace.addValue(bit(t % 2 == 0), t);
		trace.commit(total);

		BufferedImage image = paint(trace, panel);

		// drawing width w = panel-10; x(t) = w-(total-t) = 9950+t+...
		int w = panel - 10;
		int xOf1 = w - (total - 1);      // oldest added change
		int xOf51 = w - (total - 51);    // oldest RETAINED change

		// t in [1,50] was dropped by the cap: blank.  The checked
		// columns [xOf1+4, xOf51-5] exclude the tic gridlines, which
		// land at multiples of 50 from tzero = w-total.
		assertFalse(hasInk(image, xOf1 + 4, xOf51 - 5),
				"changes older than the retention cap must be dropped");

		// t just inside the cap is drawn (columns chosen between tic
		// gridlines so only signal ink can satisfy the assertion)
		assertTrue(hasInk(image, xOf51 + 54, xOf51 + 69),
				"changes inside the retention cap must be drawn");
	}

	@Test
	void unchangedValuesAreStillDeduplicated() {

		// pre-existing behavior kept: re-adding an equal value adds
		// no change, so a constant signal costs one entry, not one
		// per event
		Trace trace = new Trace("sig", null, 1, 500, parent());
		trace.setSize(1010, HEIGHT);
		trace.addValue(bit(true), 1);
		for (int t = 2; t <= 900; t++)
			trace.addValue(bit(true), t);
		trace.commit(900);
		BufferedImage image = paint(trace, 1010);

		// one long high segment, no transitions: the 'top' row (y=10)
		// has black signal ink, the 'bottom' row (y=30) has none
		// (tic gridlines are light gray, so checking for black keys
		// on signal ink only)
		boolean topInk = false;
		boolean bottomInk = false;
		int black = Color.black.getRGB();
		for (int x = 0; x < 1000; x++) {
			if (image.getRGB(x, 10) == black)
				topInk = true;
			if (image.getRGB(x, 30) == black)
				bottomInk = true;
		}
		assertTrue(topInk, "the high level must be drawn");
		assertFalse(bottomInk, "a constant-high signal must never "
				+ "draw at the low level");
	}
}
