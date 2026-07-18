package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The color-vision experiment for issue #76 (hypothesis H1), run as a
 * unit test instead of a screenshot matrix: simulate deuteranopia and
 * protanopia over the semantic palette directly (the same LMS-matrix
 * filter a screenshot pass would apply per-pixel, Vienot et al. 1999)
 * and require every pair of wire states to stay apart in CIE76
 * delta-E.  The classic palette's red/green value/touch pair provably
 * collides under deuteranopia - the defect at HEAD - while the
 * adopted default palette keeps every pair distinguishable, on top of
 * the color-independent second channel (wire stroke, endpoint ring)
 * tested in {@code WireValueChannelTest}.
 *
 * Also pins the theme mechanics: the classic theme reproduces the
 * original hardcoded palette exactly (no user is forced off the old
 * colors), and applying a theme rewrites the {@link JLSInfo} statics
 * that every draw call site reads.
 */
class ThemeTest {

	/** Two colors are "clearly different" above this CIE76 delta-E. */
	private static final double DISTINGUISHABLE = 25.0;

	@AfterEach
	void restoreDefaultTheme() {
		Theme.DEFAULT.apply();
	}

	@Test
	void classicReproducesTheLegacyPalette() {
		Theme t = Theme.CLASSIC;
		assertEquals(Color.green, t.touch(), "touch");
		assertEquals(Color.pink, t.highlight(), "highlight");
		assertEquals(new Color(240, 240, 240), t.selection(), "selection");
		assertEquals(Color.cyan, t.watch(), "watch");
		assertEquals(Color.red, t.nonZero(), "nonZero");
		assertEquals(Color.lightGray, t.initialState(), "initialState");
		assertEquals(Color.blue, t.wireOff(), "wireOff");
		assertEquals(Color.black, t.wireZero(), "wireZero");
		assertEquals(new Color(240, 240, 240), t.grid(), "grid");
		assertEquals(Color.white, t.background(), "background");
	}

	@Test
	void applyRewritesTheJLSInfoStatics() {
		Theme.CLASSIC.apply();
		assertSame(Theme.CLASSIC, Theme.active());
		assertEquals(Color.green, JLSInfo.touchColor);
		assertEquals(Color.red, JLSInfo.nonZeroColor);
		assertEquals(Color.blue, JLSInfo.wireOffColor);
		Theme.DEFAULT.apply();
		assertSame(Theme.DEFAULT, Theme.active());
		assertEquals(Theme.DEFAULT.touch(), JLSInfo.touchColor);
		assertEquals(Theme.DEFAULT.nonZero(), JLSInfo.nonZeroColor);
		assertEquals(Theme.DEFAULT.grid(), JLSInfo.gridColor);
		assertEquals(Theme.DEFAULT.background(), JLSInfo.backgroundColor);
	}

	@Test
	void byNameResolvesEveryThemeAndFallsBackToDefault() {
		for (Theme t : Theme.all()) {
			assertSame(t, Theme.byName(t.name()));
		}
		assertSame(Theme.DEFAULT, Theme.byName("no-such-theme"));
		assertSame(Theme.DEFAULT, Theme.byName(null));
	}

	@Test
	void defaultWireStatesStayDistinguishableForDichromats() {
		Map<String, Color> states = wireStates(Theme.DEFAULT);
		String[] names = states.keySet().toArray(new String[0]);
		for (double[][] filter : new double[][][] {null, DEUTERANOPIA,
				PROTANOPIA}) {
			for (int i = 0; i < names.length; i += 1) {
				for (int j = i + 1; j < names.length; j += 1) {
					double d = filteredDeltaE(states.get(names[i]),
							states.get(names[j]), filter);
					assertTrue(d > DISTINGUISHABLE,
							names[i] + " vs " + names[j] + " only "
									+ d + " delta-E apart under "
									+ filterName(filter));
				}
			}
		}
	}

	@Test
	void defaultValueVersusTouchPairIsFarApartForDichromats() {
		// the flagship pair: the two most important runtime meanings
		for (double[][] filter : new double[][][] {null, DEUTERANOPIA,
				PROTANOPIA}) {
			double d = filteredDeltaE(Theme.DEFAULT.nonZero(),
					Theme.DEFAULT.touch(), filter);
			assertTrue(d > 100.0, "nonZero vs touch only " + d
					+ " delta-E apart under " + filterName(filter));
		}
	}

	@Test
	void classicValueVersusTouchPairCollidesUnderDeuteranopia() {
		// the provable defect at HEAD (issue #76 prediction P1): the
		// classic red/green pair falls below the distinguishability
		// bar once deuteranopia is simulated
		double normal = filteredDeltaE(Theme.CLASSIC.nonZero(),
				Theme.CLASSIC.touch(), null);
		double filtered = filteredDeltaE(Theme.CLASSIC.nonZero(),
				Theme.CLASSIC.touch(), DEUTERANOPIA);
		assertTrue(normal > 100.0,
				"red vs green is vivid for trichromats: " + normal);
		assertTrue(filtered < DISTINGUISHABLE,
				"red vs green should collide under deuteranopia but "
						+ "measured " + filtered);
	}

	/**
	 * The wire/point states a user must tell apart on the canvas.
	 * The watch fill is excluded: it only ever appears as a large
	 * fill behind element text, never as a wire or point color.
	 */
	private static Map<String, Color> wireStates(Theme t) {
		Map<String, Color> states = new LinkedHashMap<String, Color>();
		states.put("touch", t.touch());
		states.put("highlight", t.highlight());
		states.put("nonZero", t.nonZero());
		states.put("wireOff", t.wireOff());
		states.put("wireZero", t.wireZero());
		states.put("background", t.background());
		return states;
	}

	// ---- Vienot et al. 1999 dichromat simulation ----

	/** linear RGB to LMS (Hunt-Pointer-Estevez, D65-normalized). */
	private static final double[][] RGB2LMS = {
			{0.31399022, 0.63951294, 0.04649755},
			{0.15537241, 0.75789446, 0.08670142},
			{0.01775239, 0.10944209, 0.87256922}};

	/** LMS back to linear RGB (inverse of RGB2LMS). */
	private static final double[][] LMS2RGB = {
			{5.47221206, -4.6419601, 0.16963708},
			{-1.1252419, 2.29317094, -0.1678952},
			{0.02980165, -0.19318073, 1.16364789}};

	/** Missing-M-cone projection in LMS space. */
	private static final double[][] DEUTERANOPIA = {
			{1, 0, 0},
			{0.9513092, 0, 0.04866992},
			{0, 0, 1}};

	/** Missing-L-cone projection in LMS space. */
	private static final double[][] PROTANOPIA = {
			{0, 1.05118294, -0.05116099},
			{0, 1, 0},
			{0, 0, 1}};

	private static String filterName(double[][] filter) {
		if (filter == null) {
			return "normal vision";
		}
		return filter == DEUTERANOPIA ? "deuteranopia" : "protanopia";
	}

	private static double[] multiply(double[][] m, double[] v) {
		double[] out = new double[3];
		for (int i = 0; i < 3; i += 1) {
			out[i] = m[i][0] * v[0] + m[i][1] * v[1] + m[i][2] * v[2];
		}
		return out;
	}

	/** sRGB component (0-255) to linear light. */
	private static double linear(int c) {
		double s = c / 255.0;
		return s <= 0.04045 ? s / 12.92
				: Math.pow((s + 0.055) / 1.055, 2.4);
	}

	/** A color as linear RGB, optionally filtered through dichromacy. */
	private static double[] linearRgb(Color c, double[][] filter) {
		double[] rgb = {linear(c.getRed()), linear(c.getGreen()),
				linear(c.getBlue())};
		if (filter == null) {
			return rgb;
		}
		double[] lms = multiply(filter, multiply(RGB2LMS, rgb));
		double[] out = multiply(LMS2RGB, lms);
		for (int i = 0; i < 3; i += 1) {
			out[i] = Math.min(1.0, Math.max(0.0, out[i]));
		}
		return out;
	}

	/** Linear RGB to CIE Lab (D65 white). */
	private static double[] lab(double[] rgb) {
		double x = 0.4124564 * rgb[0] + 0.3575761 * rgb[1]
				+ 0.1804375 * rgb[2];
		double y = 0.2126729 * rgb[0] + 0.7151522 * rgb[1]
				+ 0.0721750 * rgb[2];
		double z = 0.0193339 * rgb[0] + 0.1191920 * rgb[1]
				+ 0.9503041 * rgb[2];
		double fx = labF(x / 0.95047);
		double fy = labF(y / 1.0);
		double fz = labF(z / 1.08883);
		return new double[] {116 * fy - 16, 500 * (fx - fy),
				200 * (fy - fz)};
	}

	private static double labF(double t) {
		return t > 0.008856 ? Math.cbrt(t) : 7.787 * t + 16.0 / 116.0;
	}

	/** CIE76 delta-E between two colors under an optional filter. */
	private static double filteredDeltaE(Color a, Color b,
			double[][] filter) {
		double[] la = lab(linearRgb(a, filter));
		double[] lb = lab(linearRgb(b, filter));
		double dl = la[0] - lb[0];
		double da = la[1] - lb[1];
		double db = la[2] - lb[2];
		return Math.sqrt(dl * dl + da * da + db * db);
	}

} // end of ThemeTest class
