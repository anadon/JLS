package jls.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Properties of the extracted trace-display geometry (issue #121):
 * the tic increment keeps a minimum pixel pitch, and the label stride
 * guarantees adjacent time labels can neither overlap (P1) nor thin
 * out so far that a viewport shows none (the H1 falsification guard
 * from section 9 of the issue).
 */
class TraceGeometryTest {

	/**
	 * Scale factors to sweep: 1..1000 exhaustively, then decades with
	 * offsets up to 10^7.  (Above ~4*10^7 the increment search
	 * overflows int; that defect predates #121 and is out of its
	 * scope.)
	 */
	private static int[] scaleFactors() {

		List<Integer> scales = new ArrayList<>();
		for (int s = 1; s <= 1000; s++)
			scales.add(s);
		for (int decade = 10_000; decade <= 10_000_000; decade *= 10) {
			scales.add(decade - 1);
			scales.add(decade);
			scales.add(decade + 7);
		}
		int[] out = new int[scales.size()];
		for (int i = 0; i < out.length; i++)
			out[i] = scales.get(i);
		return out;
	}

	@Test
	void ticPitchIsAtLeastFiftyPixelsAtEveryScaleFactor() {

		for (int scale : scaleFactors()) {
			int inc = TraceGeometry.ticIncrement(scale);
			assertTrue(inc > 0, "increment must be positive at scale "
					+ scale);
			assertTrue(1.0 * inc / scale >= TraceGeometry.MIN_TIC_GAP,
					"pitch below minimum at scale " + scale + " (inc="
							+ inc + ")");
		}
	}

	@Test
	void ticIncrementMatchesTheInlineOriginal() {

		// the extraction from Trace.paintComponent is verbatim; pin
		// hand-computed values of the original loop
		assertEquals(50, TraceGeometry.ticIncrement(1));
		assertEquals(100, TraceGeometry.ticIncrement(2));
		assertEquals(200, TraceGeometry.ticIncrement(3));
		assertEquals(250, TraceGeometry.ticIncrement(5));
		assertEquals(500, TraceGeometry.ticIncrement(10));
		assertEquals(5_000_000, TraceGeometry.ticIncrement(100_000));
	}

	@Test
	void adjacentLabelsNeverOverlap() {

		// P1's post-fix half: consecutive drawn labels sit
		// stride*pitch pixels apart, which must cover the label width
		for (int scale : scaleFactors()) {
			double pitch = 1.0 * TraceGeometry.ticIncrement(scale) / scale;
			for (int labelWidth = 1; labelWidth <= 400; labelWidth++) {
				int stride = TraceGeometry.labelStride(labelWidth, pitch);
				assertTrue(stride >= 1);
				assertTrue(stride * pitch >= labelWidth,
						"overlap: scale " + scale + " labelWidth "
								+ labelWidth + " stride " + stride
								+ " pitch " + pitch);
			}
		}
	}

	@Test
	void labeledTicsStayDense() {

		// H1 falsification guard (section 9 of the issue): skipping
		// may never leave a viewport without labels.  Since
		// stride = ceil(labelWidth/pitch), consecutive labels are
		// less than labelWidth + pitch apart, so any viewport at
		// least labelWidth + 2*pitch wide contains a whole label.
		for (int scale : scaleFactors()) {
			double pitch = 1.0 * TraceGeometry.ticIncrement(scale) / scale;
			for (int labelWidth = 1; labelWidth <= 400; labelWidth++) {
				int stride = TraceGeometry.labelStride(labelWidth, pitch);
				assertTrue(stride * pitch < labelWidth + pitch,
						"labels too sparse: scale " + scale
								+ " labelWidth " + labelWidth);
			}
		}
	}

	@Test
	void realisticHeaderLabelsClearEachOtherInRealFontMetrics() {

		// the same digit-count width bound Trace.paintComponent uses,
		// with a real font (metrics, not hard-coded pixels - #111):
		// every label drawn for a default-length run must fit inside
		// the stride at every scale factor
		FontMetrics fm = new BufferedImage(1, 1,
				BufferedImage.TYPE_INT_RGB).createGraphics()
						.getFontMetrics(
								new Font(Font.DIALOG, Font.PLAIN, 12));
		int digitWidth = fm.charWidth(' ');
		for (char c = '0'; c <= '9'; c++)
			digitWidth = Math.max(digitWidth, fm.charWidth(c));

		long now = 100_000_000L; // the default run limit
		for (int scale : new int[] { 1, 3, 7, 100, 999, 12_345,
				1_000_000, 10_000_000 }) {
			int inc = TraceGeometry.ticIncrement(scale);
			double pitch = 1.0 * inc / scale;
			long lastTic = now / inc + 1;
			int labelWidth = (String.valueOf(lastTic * inc).length() + 1)
					* digitWidth;
			int stride = TraceGeometry.labelStride(labelWidth, pitch);

			// check real rendered widths of a sample of the labels
			// actually drawn (multiples of stride), including the
			// widest ones at the end of the run
			long[] sample = { 0, stride, 2L * stride,
					(lastTic / stride / 2) * stride,
					(lastTic / stride - 1) * stride,
					(lastTic / stride) * stride };
			for (long tic : sample) {
				if (tic < 0)
					continue;
				int w = fm.stringWidth(String.valueOf(tic * inc));
				assertTrue(w <= stride * pitch,
						"label " + (tic * inc) + " overlaps its"
								+ " neighbour at scale " + scale
								+ ": width " + w + " > stride span "
								+ stride * pitch);
			}
		}
	}
}
