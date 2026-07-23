package jls.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import jls.core.TextMetrics;

/**
 * Pins that {@link SwingTextMetrics} forwards {@link FontMetrics} values
 * verbatim (issue #77). Element geometry — and therefore saved-file
 * bytes — is derived from these measurements, so when the element wave
 * moves each element's sizing from a live {@code Graphics} onto the
 * {@link TextMetrics} seam, the value it reads must be pixel-identical to
 * the value it reads today. This test is that guarantee; it is headless
 * (no window — a 1x1 image's graphics supplies the metrics).
 */
class TextMetricsParityTest {

	private static final String[] SAMPLES = {
		"", "0", "42", "1010", "Q3", "address", "WE",
		"a very long label with spaces", "-16777216", "b101",
	};

	private static Graphics graphics() {
		BufferedImage img =
				new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		return img.getGraphics();
	}

	@Test
	void ambientMetricsForwardVerbatim() {
		Graphics g = graphics();
		FontMetrics fm = g.getFontMetrics();
		SwingTextMetrics tm = SwingTextMetrics.of(g);

		assertEquals(fm.getAscent(), tm.ascent(), "ascent");
		assertEquals(fm.getDescent(), tm.descent(), "descent");
		assertEquals(fm.getHeight(), tm.height(), "height");
		for (String s : SAMPLES) {
			assertEquals(fm.stringWidth(s), tm.stringWidth(s),
					"stringWidth of \"" + s + "\"");
		}
	}

	@Test
	void providerForFontMatchesLiveFontMetrics() {
		Graphics g = graphics();
		TextMetrics.Provider provider = new SwingTextMetrics.GraphicsProvider(g);

		int[] styles = { Font.PLAIN, Font.BOLD, Font.ITALIC,
				Font.BOLD | Font.ITALIC };
		for (String family : new String[] { "Dialog", "Monospaced", "Serif" }) {
			for (int style : styles) {
				for (int size : new int[] { 10, 12, 18 }) {
					Font f = new Font(family, style, size);
					FontMetrics fm = g.getFontMetrics(f);
					TextMetrics tm = provider.forFont(family,
							(style & Font.BOLD) != 0,
							(style & Font.ITALIC) != 0, size);
					String where = family + "/" + style + "/" + size;
					assertEquals(fm.getAscent(), tm.ascent(), "ascent " + where);
					assertEquals(fm.getDescent(), tm.descent(),
							"descent " + where);
					assertEquals(fm.getHeight(), tm.height(), "height " + where);
					for (String s : SAMPLES) {
						assertEquals(fm.stringWidth(s), tm.stringWidth(s),
								"width \"" + s + "\" " + where);
					}
				}
			}
		}
	}

	@Test
	void providerDefaultsMatchTheAmbientFont() {
		Graphics g = graphics();
		TextMetrics.Provider provider = new SwingTextMetrics.GraphicsProvider(g);
		assertEquals(g.getFont().getFamily(), provider.defaultFontName(),
				"default font family");
		assertEquals(g.getFont().getSize(), provider.defaultFontSize(),
				"default font size");
	}
}
