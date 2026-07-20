package jls.elem;

import java.util.ArrayList;
import java.util.List;

/**
 * Headless outline geometry for a gate body symbol (issues #77, #78).
 *
 * <p>This is the model half of the gate renderer/model split. A gate leaf
 * declares the shape it draws as pure coordinate data - straight lines,
 * open arcs, cubic curves, and inversion bubbles, all measured in the
 * gate-local pixel frame - with no dependency on AWT, Swing, or the
 * editor. The Swing half, {@link Gate}, turns that data into a
 * {@code java.awt.geom.GeneralPath} the first time the gate is drawn.
 *
 * <p>Because the geometry is data rather than {@code java.awt.geom} calls,
 * a converted gate leaf (AndGate, OrGate, NandGate, NorGate, NotGate,
 * XorGate) carries no forbidden import and drops out of the headless-core
 * ratchet baseline in {@code jls.HeadlessCoreRatchetTest}. The reverse
 * translation lives entirely in {@link Gate#gatePathFrom(GateOutline)},
 * which reconstructs the exact same {@code Line2D}/{@code Arc2D}/
 * {@code CubicCurve2D}/{@code Ellipse2D} appends the leaves used to make
 * inline, so rendering is byte-for-byte unchanged.
 *
 * <p>This is the pattern later waves continue across the rest of
 * {@code jls.elem}: express an element's drawn geometry as a headless
 * description the model side owns, and keep the {@code Graphics}/
 * {@code GeneralPath} translation on the GUI side.
 */
public final class GateOutline {

	/** The kind of geometric primitive one {@link Segment} describes. */
	public enum Kind {
		/** A straight line: {@code x1, y1, x2, y2}. */
		LINE,
		/** An open elliptical arc: {@code x, y, w, h, startDeg, extentDeg}. */
		ARC,
		/** A cubic Bezier: {@code x1, y1, cx1, cy1, cx2, cy2, x2, y2}. */
		CUBIC,
		/** An ellipse (an inversion bubble or dot): {@code x, y, w, h}. */
		ELLIPSE
	}

	/**
	 * One primitive in a gate outline, together with how it joins the
	 * path being built.
	 *
	 * @param kind Which primitive {@code coords} describes.
	 * @param coords The primitive's parameters, in the order documented on
	 *        the matching {@link Kind} constant.
	 * @param connect Whether this primitive continues the current subpath
	 *        (an {@code append(shape, true)}) or starts a fresh one; the
	 *        flag is ignored for the first segment, which opens the path.
	 * @param closeAfter Whether the subpath is closed immediately after
	 *        this primitive (a {@code closePath()}).
	 */
	public record Segment(Kind kind, double[] coords, boolean connect,
			boolean closeAfter) {
	}

	/** The primitives that make up this outline, in draw order. */
	private final List<Segment> segments;

	/**
	 * Wrap a finished segment list.
	 *
	 * @param segments The primitives in draw order.
	 */
	private GateOutline(List<Segment> segments) {

		this.segments = segments;
	} // end of constructor

	/**
	 * The primitives that make up this outline, in draw order.
	 *
	 * @return an unmodifiable list of segments.
	 */
	public List<Segment> segments() {

		return segments;
	} // end of segments method

	/**
	 * Start building a gate outline.
	 *
	 * @return a fresh, empty builder.
	 */
	public static Builder builder() {

		return new Builder();
	} // end of builder method

	/**
	 * Accumulates the primitives of one gate outline in draw order. Each
	 * {@code line}/{@code arc}/{@code cubic}/{@code ellipse} call appends
	 * one primitive; the first opens the path and later ones join it per
	 * their {@code connect} flag. {@link #close()} marks the most recently
	 * added primitive as closing its subpath.
	 */
	public static final class Builder {

		/** The primitives collected so far. */
		private final List<Segment> segs = new ArrayList<>();

		/** Restrict construction to {@link GateOutline#builder()}. */
		private Builder() {

		} // end of constructor

		/**
		 * Append one primitive.
		 *
		 * @param kind The primitive kind.
		 * @param connect Whether it continues the current subpath.
		 * @param coords The primitive's parameters.
		 *
		 * @return this builder, for chaining.
		 */
		private Builder add(Kind kind, boolean connect, double... coords) {

			segs.add(new Segment(kind, coords, connect, false));
			return this;
		} // end of add method

		/**
		 * Append a straight line.
		 *
		 * @param connect Whether it continues the current subpath (ignored
		 *        for the first segment).
		 * @param x1 The start x-coordinate.
		 * @param y1 The start y-coordinate.
		 * @param x2 The end x-coordinate.
		 * @param y2 The end y-coordinate.
		 *
		 * @return this builder, for chaining.
		 */
		public Builder line(boolean connect, double x1, double y1,
				double x2, double y2) {

			return add(Kind.LINE, connect, x1, y1, x2, y2);
		} // end of line method

		/**
		 * Append an open elliptical arc.
		 *
		 * @param connect Whether it continues the current subpath.
		 * @param x The x-coordinate of the arc's bounding box.
		 * @param y The y-coordinate of the arc's bounding box.
		 * @param w The width of the arc's bounding box.
		 * @param h The height of the arc's bounding box.
		 * @param startDeg The starting angle in degrees.
		 * @param extentDeg The angular extent in degrees.
		 *
		 * @return this builder, for chaining.
		 */
		public Builder arc(boolean connect, double x, double y, double w,
				double h, double startDeg, double extentDeg) {

			return add(Kind.ARC, connect, x, y, w, h, startDeg, extentDeg);
		} // end of arc method

		/**
		 * Append a cubic Bezier curve.
		 *
		 * @param connect Whether it continues the current subpath.
		 * @param x1 The start x-coordinate.
		 * @param y1 The start y-coordinate.
		 * @param cx1 The first control-point x-coordinate.
		 * @param cy1 The first control-point y-coordinate.
		 * @param cx2 The second control-point x-coordinate.
		 * @param cy2 The second control-point y-coordinate.
		 * @param x2 The end x-coordinate.
		 * @param y2 The end y-coordinate.
		 *
		 * @return this builder, for chaining.
		 */
		public Builder cubic(boolean connect, double x1, double y1,
				double cx1, double cy1, double cx2, double cy2,
				double x2, double y2) {

			return add(Kind.CUBIC, connect, x1, y1, cx1, cy1, cx2, cy2,
					x2, y2);
		} // end of cubic method

		/**
		 * Append an ellipse (an inversion bubble or dot).
		 *
		 * @param connect Whether it continues the current subpath.
		 * @param x The x-coordinate of the ellipse's bounding box.
		 * @param y The y-coordinate of the ellipse's bounding box.
		 * @param w The width of the ellipse's bounding box.
		 * @param h The height of the ellipse's bounding box.
		 *
		 * @return this builder, for chaining.
		 */
		public Builder ellipse(boolean connect, double x, double y,
				double w, double h) {

			return add(Kind.ELLIPSE, connect, x, y, w, h);
		} // end of ellipse method

		/**
		 * Mark the most recently added primitive as closing its subpath.
		 *
		 * @return this builder, for chaining.
		 *
		 * @throws IllegalStateException if no primitive has been added yet.
		 */
		public Builder close() {

			if (segs.isEmpty()) {
				throw new IllegalStateException(
						"close() before any primitive");
			}
			int last = segs.size() - 1;
			Segment s = segs.get(last);
			segs.set(last, new Segment(s.kind(), s.coords(), s.connect(),
					true));
			return this;
		} // end of close method

		/**
		 * Finish the outline.
		 *
		 * @return an immutable {@link GateOutline} of the primitives added
		 *         so far.
		 */
		public GateOutline build() {

			return new GateOutline(List.copyOf(segs));
		} // end of build method

	} // end of Builder class

} // end of GateOutline class
