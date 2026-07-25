package jls.core;

/**
 * Headless text measurement (issue #77). Elements size themselves from
 * the width and height of their labels; today they read those from an
 * AWT {@code FontMetrics} obtained from a live
 * {@code Graphics}, which forces the whole element hierarchy to depend
 * on AWT. This interface is the seam that breaks that dependency: an
 * element's layout takes a {@code TextMetrics} instead of a
 * {@code Graphics}, and the GUI supplies a {@code FontMetrics}-backed
 * implementation ({@code jls.edit.SwingTextMetrics}) at the call site.
 *
 * <p>The four measurements are exactly those the elements use across the
 * hierarchy (verified: {@code stringWidth}, {@code getAscent},
 * {@code getDescent}, {@code getHeight}); an implementation MUST return
 * the same integer pixel values the backing {@code FontMetrics} would,
 * so element geometry — and therefore saved-file bytes — is unchanged.
 */
public interface TextMetrics {

	/**
	 * The advance width of a string in this font, in pixels.
	 *
	 * @param s The string to measure.
	 * @return the pixel width, as {@code FontMetrics.stringWidth} gives it.
	 */
	int stringWidth(String s);

	/**
	 * The font ascent (baseline to top), in pixels.
	 *
	 * @return the ascent, as {@code FontMetrics.getAscent} gives it.
	 */
	int ascent();

	/**
	 * The font descent (baseline to bottom), in pixels.
	 *
	 * @return the descent, as {@code FontMetrics.getDescent} gives it.
	 */
	int descent();

	/**
	 * The font height (ascent + descent + leading), in pixels.
	 *
	 * @return the height, as {@code FontMetrics.getHeight} gives it.
	 */
	int height();

	/**
	 * Supplies {@code TextMetrics} for specifically-named fonts, for the
	 * one element ({@code Text}) that chooses its own font family, style,
	 * and size rather than measuring in the ambient editor font. Also
	 * exposes that ambient font's family and size, which {@code Text}
	 * uses as its defaults.
	 */
	interface Provider {

		/**
		 * Metrics for the named font at the given style and size.
		 *
		 * @param name The font family name.
		 * @param bold Whether the font is bold.
		 * @param italic Whether the font is italic.
		 * @param size The point size.
		 * @return metrics for that font.
		 */
		TextMetrics forFont(String name, boolean bold, boolean italic,
				int size);

		/**
		 * The ambient font's family name (the {@code Text} default when a
		 * file names no font).
		 *
		 * @return the default font family name.
		 */
		String defaultFontName();

		/**
		 * The ambient font's point size (the {@code Text} default when a
		 * file gives no size).
		 *
		 * @return the default font size.
		 */
		int defaultFontSize();
	}
} // end of TextMetrics interface
