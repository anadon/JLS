package jls.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import jls.JLSInfo;

/**
 * Windowed trace drawing (issue #121, H2/P3): with the panel spanning
 * the whole retained run, paintComponent limits its work to the clip.
 * These tests pin (a) that the clip-window fast-forward (a binary
 * search over the committed changes) agrees with the linear scan it
 * replaced, and (b) that a windowed repaint produces exactly the same
 * pixels inside the clip as a full repaint - so the windowing is
 * purely a cost optimization, never a rendering change.
 */
class TraceWindowingTest {

	private static final int HEIGHT = 40;

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

	private static BitSet bits(int value, int width) {

		BitSet out = new BitSet(width + 1);
		for (int i = 0; i < width; i++)
			if ((value >> i & 1) != 0)
				out.set(i);
		return out;
	}

	private static BufferedImage paint(Trace trace, int width,
			Rectangle clip) {

		BufferedImage image = new BufferedImage(width, HEIGHT,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, width, HEIGHT);
		if (clip != null)
			g.clip(clip);
		trace.paintComponent(g);
		g.dispose();
		return image;
	}

	/** Assert pixel equality of columns [x0,x0+w) and that the region
	 *  is not trivially blank. */
	private static void assertRegionEqualAndInked(BufferedImage full,
			BufferedImage part, int x0, int w) {

		int white = Color.white.getRGB();
		boolean ink = false;
		for (int x = x0; x < x0 + w; x++)
			for (int y = 0; y < HEIGHT; y++) {
				int expected = full.getRGB(x, y);
				assertEquals(expected, part.getRGB(x, y),
						"pixel mismatch at (" + x + "," + y
								+ ") for clip x0=" + x0);
				if (expected != white)
					ink = true;
			}
		assertTrue(ink, "vacuous comparison: clip region at " + x0
				+ " is entirely blank");
	}

	// ------------------------------------------------------------------
	// binary search vs the linear scan it replaced
	// ------------------------------------------------------------------

	@Test
	void firstChangeAtOrBeforeMatchesTheLinearScan() {

		Trace trace = new Trace("sig", null, 4, 500, parent());
		trace.setSize(4000, HEIGHT);

		// deterministic times with duplicates (dt may be 0) and a
		// value that always differs from its predecessor, so nothing
		// is deduplicated away; track the retained order ourselves
		Random random = new Random(42);
		List<Long> whensNewestFirst = new ArrayList<>();
		long t = 0;
		for (int i = 0; i < 3000; i++) {
			t += random.nextInt(3); // 0..2: duplicates happen
			whensNewestFirst.add(0, t);
			trace.addValue(bits(i % 3 + 1, 4), t);
		}
		trace.commit(t + 5);

		// probe below, at, around and above every distinct time
		List<Long> probes = new ArrayList<>();
		probes.add(-5L);
		probes.add(0L);
		probes.add(t + 10);
		for (int i = 0; i < whensNewestFirst.size(); i += 37) {
			long when = whensNewestFirst.get(i);
			probes.add(when - 1);
			probes.add(when);
			probes.add(when + 1);
		}
		for (long probe : probes) {
			int expected = whensNewestFirst.size();
			for (int i = 0; i < whensNewestFirst.size(); i++)
				if (whensNewestFirst.get(i) <= probe) {
					expected = i;
					break;
				}
			assertEquals(expected, trace.firstChangeAtOrBefore(probe),
					"probe time " + probe);
		}
	}

	// ------------------------------------------------------------------
	// windowed repaint == full repaint, inside the clip
	// ------------------------------------------------------------------

	@Test
	void windowedRepaintMatchesFullRepaintForASingleBitTrace() {

		Trace trace = new Trace("sig", null, 1, 500, parent());
		Random random = new Random(7);
		long t = 0;
		boolean level = false;
		for (int i = 0; i < 20_000; i++) {
			t += 1 + random.nextInt(3);
			level = !level;
			trace.addValue(bits(level ? 1 : 0, 1), t);
		}
		long now = t + 5;
		int width = (int) now + 10; // nameSpace is 0
		trace.setSize(width, HEIGHT);
		trace.commit(now);

		BufferedImage full = paint(trace, width, null);
		int clipWidth = 350;
		for (int x0 : new int[] { 0, 137, width / 2,
				width - clipWidth }) {
			BufferedImage part = paint(trace, width,
					new Rectangle(x0, 0, clipWidth, HEIGHT));
			assertRegionEqualAndInked(full, part, x0, clipWidth);
		}
	}

	@Test
	void windowedRepaintMatchesFullRepaintForAMultiBitTrace() {

		// multi-bit traces also draw value strings and HiZ segments
		Trace trace = new Trace("bus", null, 4, 500, parent());
		trace.setScaleFactor(3);
		Random random = new Random(11);
		long t = 0;
		int previous = -1;
		for (int i = 0; i < 20_000; i++) {
			t += 1 + random.nextInt(9);
			int value = random.nextInt(17); // 16 -> HiZ (off)
			if (value == previous)
				value = (value + 1) % 17;
			previous = value;
			if (value == 16)
				trace.addValue(null, t); // null -> off (HiZ)
			else
				trace.addValue(bits(value, 4), t);
		}
		long now = t + 5;
		int width = (int) (now / 3) + 10;
		trace.setSize(width, HEIGHT);
		trace.commit(now);

		BufferedImage full = paint(trace, width, null);
		int clipWidth = 400;
		for (int x0 : new int[] { 0, 613, width / 3, width / 2,
				width - clipWidth }) {
			BufferedImage part = paint(trace, width,
					new Rectangle(x0, 0, clipWidth, HEIGHT));
			assertRegionEqualAndInked(full, part, x0, clipWidth);
		}
	}

	@Test
	void windowedRepaintMatchesFullRepaintForTheHeaderLabels() {

		// a trace with no changes is the header: it draws the time
		// labels, whose stride-skipping must also be clip-invariant
		Trace header = new Trace("", null, 0, 500, parent());
		header.setScaleFactor(250);
		long now = 1_000_000;
		int width = (int) (now / 250) + 10;
		header.setSize(width, HEIGHT);
		header.commit(now);

		BufferedImage full = paint(header, width, null);
		int clipWidth = 300;
		for (int x0 : new int[] { 0, 41, width / 2,
				width - clipWidth }) {
			BufferedImage part = paint(header, width,
					new Rectangle(x0, 0, clipWidth, HEIGHT));
			assertRegionEqualAndInked(full, part, x0, clipWidth);
		}
	}
}
