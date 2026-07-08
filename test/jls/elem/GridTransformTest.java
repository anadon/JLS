package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Point;

import org.junit.jupiter.api.Test;

/**
 * GridTransform (#24) validated against real relationships from the
 * committed orientation-geometry baseline: the existing hand-enumerated
 * orientations of AndGate and Adder are exact transforms of one another,
 * which is the hypothesis the canonical-geometry conversion rests on.
 */
class GridTransformTest {

	@Test
	void andGateOrientationsAreTransformsOfEachOther() {
		// from the baseline: right (48x24 put frame) has puts
		// input0@0,0  input1@0,24  output@48,12
		int w = 48, h = 24;

		// left = mirrorX(right): input0@48,0  input1@48,24  output@0,12
		assertEquals(new Point(48, 0), GridTransform.mirrorX(0, 0, w, h));
		assertEquals(new Point(48, 24), GridTransform.mirrorX(0, 24, w, h));
		assertEquals(new Point(0, 12), GridTransform.mirrorX(48, 12, w, h));

		// up = rotateCCW(right): input0@0,48  input1@24,48  output@12,0
		assertEquals(new Point(0, 48), GridTransform.rotateCCW(0, 0, w, h));
		assertEquals(new Point(24, 48), GridTransform.rotateCCW(0, 24, w, h));
		assertEquals(new Point(12, 0), GridTransform.rotateCCW(48, 12, w, h));

		// down = mirrorY(up), in the rotated 24x48 frame:
		// input0@0,0  input1@24,0  output@12,48
		assertEquals(new Point(0, 0), GridTransform.mirrorY(0, 48, h, w));
		assertEquals(new Point(24, 0), GridTransform.mirrorY(24, 48, h, w));
		assertEquals(new Point(12, 48), GridTransform.mirrorY(12, 0, h, w));
	}

	@Test
	void adderLeftIsMirrorOfRight() {
		// right (48x48): A@0,12  B@0,36  Cin@24,0  Cout@24,48  S@48,24
		// left:          A@48,12 B@48,36 Cin@24,0  Cout@24,48  S@0,24
		int w = 48, h = 48;
		assertEquals(new Point(48, 12), GridTransform.mirrorX(0, 12, w, h));
		assertEquals(new Point(48, 36), GridTransform.mirrorX(0, 36, w, h));
		assertEquals(new Point(24, 0), GridTransform.mirrorX(24, 0, w, h));
		assertEquals(new Point(24, 48), GridTransform.mirrorX(24, 48, w, h));
		assertEquals(new Point(0, 24), GridTransform.mirrorX(48, 24, w, h));
	}

	@Test
	void quarterTurnsCompose() {
		// CW then CCW is identity; two CWs are a 180
		int w = 36, h = 60;
		for (int px = 0; px <= w; px += 12) {
			for (int py = 0; py <= h; py += 12) {
				Point cw = GridTransform.rotateCW(px, py, w, h);
				assertEquals(new Point(px, py),
						GridTransform.rotateCCW(cw.x, cw.y, h, w));
				Point cw2 = GridTransform.rotateCW(cw.x, cw.y, h, w);
				assertEquals(GridTransform.rotate180(px, py, w, h), cw2);
			}
		}
		assertEquals(new java.awt.Dimension(h, w), GridTransform.rotatedSize(w, h));
	}
}
