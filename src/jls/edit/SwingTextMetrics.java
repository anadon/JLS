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
public final class SwingTextMetrics implements TextMetrics {

	private final FontMetrics fm;

	/**
	 * Wrap a font's metrics.
	 *
	 * @param fm The backing font metrics.
	 */
	public SwingTextMetrics(FontMetrics fm) {
		this.fm = fm;
	} // end of constructor

	/**
	 * Metrics for the ambient font of a graphics context — the common
	 * case, replacing an element's {@code g.getFontMetrics()}.
	 *
	 * @param g The graphics whose current font to measure in.
	 * @return metrics for that graphics' font.
	 */
	public static SwingTextMetrics of(Graphics g) {
		return new SwingTextMetrics(g.getFontMetrics());
	} // end of of method

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

		private final Graphics g;

		/**
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
