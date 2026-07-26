package jls.edit;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import jls.core.TextMetrics;

/**
 * The Swing/AWT-backed {@link TextMetrics} the GUI supplies to headless
 * element layout (issue #77). It forwards each measurement verbatim to a
 * {@link FontMetrics}, so an element sized through this seam gets exactly
 * the pixel values it would have read from the {@code Graphics} directly
 * — element geometry, and therefore saved-file bytes, are unchanged.
 * {@code TextMetricsParityTest} pins that forwarding.
 */
public final class SwingTextMetrics implements TextMetrics, TextMetrics.Provider {

	/** The backing font metrics. */
	private final FontMetrics fm;
	/** The graphics used to derive metrics for other fonts, or null. */
	private final @org.jspecify.annotations.Nullable Graphics g;

	/**
	 * Wrap a font's metrics.
	 *
	 * @param fm The backing font metrics.
	 */
	public SwingTextMetrics(FontMetrics fm) {
		this.fm = fm;
		this.g = null;
	} // end of constructor

	/**
	 * Wrap a graphics context, measuring its current font.
	 *
	 * @param g The graphics to measure in.
	 */
	private SwingTextMetrics(Graphics g) {
		this.g = g;
		this.fm = g.getFontMetrics();
	} // end of constructor

	/**
	 * Metrics for the ambient font of a graphics context — the common
	 * case, replacing an element's {@code g.getFontMetrics()}. The result
	 * also serves as a {@link TextMetrics.Provider} for the {@code Text}
	 * element's own named fonts, so a single object can size every element
	 * during a load or draw.
	 *
	 * @param g The graphics whose current font to measure in.
	 * @return metrics for that graphics' font.
	 */
	public static SwingTextMetrics of(Graphics g) {
		return new SwingTextMetrics(g);
	} // end of of method

	/**
	 * Null-safe metrics for a graphics context: the metrics for {@code g},
	 * or null when {@code g} is null. Element sizing treats a null
	 * {@link TextMetrics} the same way it treated a null {@code Graphics}
	 * (skip sizing), so this is the seam the relocated dialogs and the
	 * loader use in place of a bare {@code element.init(g)}.
	 *
	 * @param g The graphics to measure in, or null.
	 * @return metrics for that graphics, or null if it was null.
	 */
	public static @org.jspecify.annotations.Nullable TextMetrics forGraphics(
			@org.jspecify.annotations.Nullable Graphics g) {
		return g == null ? null : new SwingTextMetrics(g);
	} // end of forGraphics method

	@Override
	public TextMetrics forFont(String name, boolean bold, boolean italic,
			int size) {
		Graphics gg = requireGraphics();
		int style = (bold ? Font.BOLD : 0) | (italic ? Font.ITALIC : 0);
		return new SwingTextMetrics(gg.getFontMetrics(new Font(name, style, size)));
	} // end of forFont method

	@Override
	public String defaultFontName() {
		return requireGraphics().getFont().getFamily();
	} // end of defaultFontName method

	@Override
	public int defaultFontSize() {
		return requireGraphics().getFont().getSize();
	} // end of defaultFontSize method

	/**
	 * The backing graphics, required by the {@link TextMetrics.Provider}
	 * role. Only a graphics-backed instance (from {@link #of(Graphics)}) can
	 * serve as a provider; the metrics-only {@link #SwingTextMetrics(FontMetrics)}
	 * instance has no graphics.
	 *
	 * @return the non-null backing graphics.
	 * @throws IllegalStateException if this is a metrics-only instance.
	 */
	private Graphics requireGraphics() {
		Graphics gg = g;
		if (gg == null) {
			throw new IllegalStateException(
					"font provider requires a graphics-backed SwingTextMetrics");
		}
		return gg;
	} // end of requireGraphics method

	@Override
	public int stringWidth(String s) {
		return fm.stringWidth(s);
	} // end of stringWidth method

	@Override
	public int ascent() {
		return fm.getAscent();
	} // end of ascent method

	@Override
	public int descent() {
		return fm.getDescent();
	} // end of descent method

	@Override
	public int height() {
		return fm.getHeight();
	} // end of height method

	/**
	 * A {@link TextMetrics.Provider} over a graphics context, for the
	 * {@code Text} element's chosen fonts. Mirrors what {@code Text.init}
	 * does today: measure a named font via {@code g.getFontMetrics(font)}
	 * and default the family/size from the graphics' current font.
	 */
	public static final class GraphicsProvider
			implements TextMetrics.Provider {

		/** The graphics whose fonts this provider measures. */
		private final Graphics g;

		/**
		 * Wrap a graphics context.
		 *
		 * @param g The graphics to measure fonts against.
		 */
		public GraphicsProvider(Graphics g) {
			this.g = g;
		} // end of constructor

		@Override
		public TextMetrics forFont(String name, boolean bold,
				boolean italic, int size) {
			int style = (bold ? Font.BOLD : 0) | (italic ? Font.ITALIC : 0);
			return new SwingTextMetrics(
					g.getFontMetrics(new Font(name, style, size)));
		} // end of forFont method

		@Override
		public String defaultFontName() {
			return g.getFont().getFamily();
		} // end of defaultFontName method

		@Override
		public int defaultFontSize() {
			return g.getFont().getSize();
		} // end of defaultFontSize method
	} // end of GraphicsProvider class

} // end of SwingTextMetrics class
