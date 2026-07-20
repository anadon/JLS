package jls.elem;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;

/**
 * Pins the renderer/model split of the six pure gate leaves (issue #77):
 * the {@link GateOutline} each gate now returns must reproduce the exact
 * {@code GeneralPath} the gate's constructor used to build inline, so the
 * rendered symbol is byte-for-byte unchanged.
 *
 * <p>Each test builds a reference path from a verbatim copy of the former
 * inline construction, then flattens both it and the path produced from
 * the gate's {@link GateOutline} and asserts the point streams are
 * identical. The reference code is intentionally duplicated here so the
 * test is an independent golden, not a tautology against the production
 * translation.
 */
class GateOutlineParityTest {

	/** The grid spacing the gate symbols are measured in. */
	private static final int S = JLSInfo.spacing;

	/** The inversion-bubble diameter the gate symbols use. */
	private static final int D = JLSInfo.pointDiameter;

	/**
	 * Translate a headless outline into a {@link GeneralPath} the same way
	 * {@code Gate.gatePathFrom} does, so the assertion compares geometry,
	 * not translation.
	 *
	 * @param outline The gate outline to translate.
	 *
	 * @return the reconstructed path.
	 */
	private static GeneralPath pathOf(GateOutline outline) {

		GeneralPath path = null;
		for (GateOutline.Segment seg : outline.segments()) {
			double[] c = seg.coords();
			java.awt.Shape shape = switch (seg.kind()) {
				case LINE -> new Line2D.Double(c[0], c[1], c[2], c[3]);
				case ARC -> new Arc2D.Double(c[0], c[1], c[2], c[3], c[4],
						c[5], Arc2D.OPEN);
				case CUBIC -> new CubicCurve2D.Double(c[0], c[1], c[2], c[3],
						c[4], c[5], c[6], c[7]);
				case ELLIPSE -> new Ellipse2D.Double(c[0], c[1], c[2], c[3]);
			};
			if (path == null) {
				path = new GeneralPath(shape);
			}
			else {
				path.append(shape, seg.connect());
			}
			if (seg.closeAfter()) {
				path.closePath();
			}
		}
		return path;
	} // end of pathOf method

	/**
	 * Flatten a path into its segment-type and coordinate stream.
	 *
	 * @param path The path to flatten.
	 *
	 * @return a list of doubles: each segment's type code followed by its
	 *         six coordinate slots (unused slots are zero).
	 */
	private static double[] flatten(GeneralPath path) {

		List<Double> out = new ArrayList<>();
		double[] c = new double[6];
		for (PathIterator it = path.getPathIterator(null); !it.isDone();
				it.next()) {
			int type = it.currentSegment(c);
			out.add((double) type);
			for (double v : c) {
				out.add(v);
			}
		}
		double[] arr = new double[out.size()];
		for (int i = 0; i < arr.length; i += 1) {
			arr[i] = out.get(i);
		}
		return arr;
	} // end of flatten method

	/**
	 * Assert a gate's model-driven outline flattens identically to a
	 * reference path built from the former inline construction.
	 *
	 * @param gate The gate whose {@link GateOutline} to check.
	 * @param reference The verbatim former inline path.
	 */
	private static void assertParity(Gate gate, GeneralPath reference) {

		assertArrayEquals(flatten(reference), flatten(pathOf(gate.outline())),
				gate.getClass().getSimpleName()
						+ " outline must match the former inline geometry");
	} // end of assertParity method

	/** A throwaway circuit for constructing bare gates. */
	private static final Circuit CIRCUIT = new Circuit("outline");

	@Test
	void andGateOutlineMatches() {

		int s = S;
		GeneralPath ref = new GeneralPath(new Line2D.Double(s, 0, 0, 0));
		ref.append(new Line2D.Double(0, 0, 0, s * 2), true);
		ref.append(new Line2D.Double(0, s * 2, s, s * 2), true);
		ref.append(new Arc2D.Double(0, 0, s * 2, s * 2, -90, 180,
				Arc2D.OPEN), true);
		ref.closePath();
		assertParity(new AndGate(CIRCUIT), ref);
	} // end of andGateOutlineMatches method

	@Test
	void notGateOutlineMatches() {

		int s = S;
		int d = D;
		GeneralPath ref = new GeneralPath(
				new Line2D.Double(0, 0, 2 * s - d, s));
		ref.append(new Line2D.Double(2 * s - d, s, 0, 2 * s), true);
		ref.append(new Line2D.Double(0, 2 * s, 0, 0), true);
		ref.closePath();
		ref.append(new Ellipse2D.Double(2 * s - d, s - d / 2, d, d), false);
		assertParity(new NotGate(CIRCUIT), ref);
	} // end of notGateOutlineMatches method

	@Test
	void nandGateOutlineMatches() {

		int s = S;
		int d = D;
		GeneralPath ref = new GeneralPath(new Line2D.Double(s / 2, 0, 0, 0));
		ref.append(new Line2D.Double(0, 0, 0, s * 2), true);
		ref.append(new Line2D.Double(0, s * 2, s / 2, s * 2), true);
		ref.append(new Arc2D.Double(-s / 2, 0, s * 2, s * 2, -90, 180,
				Arc2D.OPEN), true);
		ref.closePath();
		ref.append(new Ellipse2D.Double(s + d, s - d / 2, d, d), false);
		assertParity(new NandGate(CIRCUIT), ref);
	} // end of nandGateOutlineMatches method

	@Test
	void orGateOutlineMatches() {

		int s = S;
		GeneralPath ref = new GeneralPath(
				new CubicCurve2D.Double(2 * s, s, 2 * s - 1, s - 3, s, 0,
						0, 0));
		ref.append(new CubicCurve2D.Double(0, 0, s / 4, s, s / 4, s, 0,
				2 * s), true);
		ref.append(new CubicCurve2D.Double(0, 2 * s, s, 2 * s, 2 * s - 1,
				s + 3, 2 * s, s), true);
		ref.closePath();
		assertParity(new OrGate(CIRCUIT), ref);
	} // end of orGateOutlineMatches method

	@Test
	void norGateOutlineMatches() {

		int s = S;
		int d = D;
		GeneralPath ref = new GeneralPath(
				new CubicCurve2D.Double(2 * s - d, s, 2 * s - d - 1, s - 3,
						s, 0, 0, 0));
		ref.append(new CubicCurve2D.Double(0, 0, s / 4, s, s / 4, s, 0,
				2 * s), true);
		ref.append(new CubicCurve2D.Double(0, 2 * s, s, 2 * s, 2 * s - d - 1,
				s + 3, 2 * s - d, s), true);
		ref.closePath();
		ref.append(new Ellipse2D.Double(2 * s - d, s - d / 2, d, d), false);
		assertParity(new NorGate(CIRCUIT), ref);
	} // end of norGateOutlineMatches method

	@Test
	void xorGateOutlineMatches() {

		int s = S;
		int gap = 2;
		GeneralPath ref = new GeneralPath(
				new CubicCurve2D.Double(2 * s, s, 2 * s - 1, s - 3, gap + s,
						0, gap, 0));
		ref.append(new CubicCurve2D.Double(gap, 0, gap + s / 4, s,
				gap + s / 4, s, gap, 2 * s), true);
		ref.append(new CubicCurve2D.Double(gap, 2 * s, gap + s, 2 * s,
				2 * s - 1, s + 3, 2 * s, s), true);
		ref.closePath();
		ref.append(new CubicCurve2D.Double(0, 0, s / 4, s, s / 4, s, 0,
				2 * s), false);
		assertParity(new XorGate(CIRCUIT), ref);
	} // end of xorGateOutlineMatches method

	@Test
	void inlineGatesReportNoOutline() {

		// DelayGate and Extend still build gateShape in their constructor;
		// they inherit the null default so Gate.draw keeps using their
		// inline path
		assertNull(new DelayGate(CIRCUIT).outline(),
				"DelayGate still builds gateShape inline");
		assertNull(new Extend(CIRCUIT).outline(),
				"Extend still builds gateShape inline");
	} // end of inlineGatesReportNoOutline method

} // end of GateOutlineParityTest class
