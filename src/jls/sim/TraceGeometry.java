package jls.sim;

/**
 * Pure display geometry for the timing-trace viewer (issue #121): the
 * tic-increment and label-spacing computations extracted from
 * Trace.paintComponent so their properties (minimum tic pitch, label
 * non-overlap, label density) are unit-testable.  Deliberately free of
 * AWT so it stays inside the headless core (HeadlessCoreRatchetTest).
 *
 * @author David A. Poplawski
 */
public final class TraceGeometry {

	/** The minimum pixel gap between adjacent tic marks. */
	public static final int MIN_TIC_GAP = 50;

	/**
	 * Never instantiated.
	 */
	private TraceGeometry() {
	} // end of constructor

	/**
	 * Compute the simulation-time increment between tic marks: the
	 * smallest of 50, 100, 200 or 250 times a power of ten whose pixel
	 * pitch (increment / scaleFactor, integer division) is at least
	 * MIN_TIC_GAP pixels.  Moved verbatim from Trace.paintComponent.
	 *
	 * @param scaleFactor Simulation time units per pixel (at least 1).
	 *
	 * @return The tic increment, in simulation time units.
	 *
	 * @jls.testedby jls.sim.TraceGeometryTest#adjacentLabelsNeverOverlap()
	 * @jls.testedby jls.sim.TraceGeometryTest#labeledTicsStayDense()
	 * @jls.testedby jls.sim.TraceGeometryTest#realisticHeaderLabelsClearEachOtherInRealFontMetrics()
	 * @jls.testedby jls.sim.TraceGeometryTest#ticIncrementMatchesTheInlineOriginal()
	 * @jls.testedby jls.sim.TraceGeometryTest#ticPitchIsAtLeastFiftyPixelsAtEveryScaleFactor()
	 */
	public static int ticIncrement(int scaleFactor) {

		int t = 0;
		int m = 1;
		int inc = 0;
		while (t < MIN_TIC_GAP) {
			inc = m*50;
			t = inc/scaleFactor;
			if (t < MIN_TIC_GAP) {
				inc = m*100;
				t = inc/scaleFactor;
			}
			if (t < MIN_TIC_GAP) {
				inc = m*200;
				t = inc/scaleFactor;
			}
			if (t < MIN_TIC_GAP) {
				inc = m*250;
				t = inc/scaleFactor;
			}
			m *= 10;
		}
		return inc;
	} // end of ticIncrement method

	/**
	 * How many tics apart time labels must be drawn so adjacent labels
	 * can never overlap: the smallest stride whose pixel span
	 * (stride * ticPitch) is at least the widest label's width.
	 *
	 * @param labelWidth The widest label's pixel width (from font
	 *                   metrics, never hard-coded - issue #111).
	 * @param ticPitch The pixel gap between adjacent tics (positive).
	 *
	 * @return The stride (at least 1) between labeled tics.
	 *
	 * @jls.testedby jls.sim.TraceGeometryTest#adjacentLabelsNeverOverlap()
	 * @jls.testedby jls.sim.TraceGeometryTest#labeledTicsStayDense()
	 * @jls.testedby jls.sim.TraceGeometryTest#realisticHeaderLabelsClearEachOtherInRealFontMetrics()
	 */
	public static int labelStride(int labelWidth, double ticPitch) {

		if (labelWidth <= 0)
			return 1;
		return Math.max(1,(int)Math.ceil(labelWidth/ticPitch));
	} // end of labelStride method

} // end of TraceGeometry class
