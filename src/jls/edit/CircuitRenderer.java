package jls.edit;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jspecify.annotations.Nullable;

import jls.Circuit;
import jls.core.Geometry;
import jls.elem.Element;
import jls.elem.StateMachine;
import jls.elem.SubCircuit;
import jls.elem.TruthTable;
import jls.elem.Wire;

/**
 * GUI-side rendering of a {@link Circuit} (issue #77). Drawing, printing,
 * and image export are view concerns that used to live on the model class
 * (Circuit implemented Printable and imported AWT/Swing to draw itself);
 * they move here so the model stays headless. The circuit exposes only
 * the model seams this needs — its elements, spatial-index queries,
 * bounds, and load-finishing — none of which touch AWT.
 */
public final class CircuitRenderer implements Printable {

	/**
	 * How far outside its index bounds an element may draw (labels and
	 * similar decorations). Draw culling pads by this margin on both the
	 * index query and the exact visibility check.
	 */
	private static final int DRAW_MARGIN = 8 * Geometry.SPACING;

	private final Circuit circuit;

	private CircuitRenderer(Circuit circuit) {
		this.circuit = circuit;
	} // end of constructor

	/**
	 * A renderer for a circuit.
	 *
	 * @param circuit The circuit to render.
	 * @return a renderer over it.
	 */
	public static CircuitRenderer of(Circuit circuit) {
		return new CircuitRenderer(circuit);
	} // end of of method

	/**
	 * Draw the circuit by drawing every element. First the set of elements
	 * not in the second set are drawn, then the ones in the second set are
	 * drawn. Wires are drawn first in each set. Moved verbatim from
	 * {@code Circuit.draw} (issue #77).
	 *
	 * @param g The graphics object to draw with.
	 * @param second The second set of elements to draw.
	 * @param ed The editor window doing the drawing.
	 * @throws Exception if an unexpected problem stops the deferred
	 *             finish-load or the drawing pass.
	 *
	 * @jls.testedby jls.DrawCullingParityTest#tiledClippedDrawsMatchFullDraw()
	 */
	public void draw(Graphics g, Set<Element> second, @Nullable SimpleEditor ed)
			throws Exception {

		// finish up loading process if necessary
		if (circuit.isLoadPending()) {
			if (!circuit.finishLoad(g)) {
				// report once instead of silently re-failing on every
				// repaint (#58)
				circuit.reportDeferredFinishFailure();
			}

			// set circuit size to the largest of the default area or the
			// needed area
			Rectangle rect = new Rectangle(0, 0, Geometry.CIRCUITSIZE,
					Geometry.CIRCUITSIZE);
			rect.add(circuit.getBounds());
			if (ed != null) {
				ed.setCircuitSize(rect.getSize());
			}
		}

		// partition into draw layers in one pass instead of four full
		// scans (#27 S3): wires under non-wires, the second (selected)
		// set on top of both. Elements far outside the clip cannot be
		// visible and are skipped, so a scrolled view pays for what it
		// shows, not for the whole circuit (#17). The candidates come
		// from the spatial index, not a full scan, so a dirty-region
		// repaint during a drag costs O(visible), not O(circuit); the
		// query pads the clip by the same margin mayBeVisible allows
		// for labels, so its exact check below accepts the same
		// elements a full scan would. That parity is machine-checked:
		// THEOREM 2 (culling-parity) in
		// proofs/SpatialIndexCorrectness.agda, with the margin/grow/
		// intersects assumptions pinned by jls.ProofBridgeTest.
		Rectangle clip = g.getClipBounds();
		Collection<Element> candidates;
		if (clip == null) {
			candidates = circuit.getElements();
		} else {
			Rectangle query = new Rectangle(clip);
			query.grow(DRAW_MARGIN, DRAW_MARGIN);
			candidates = circuit.elementsNear(query);
		}
		List<Element> wires = new ArrayList<Element>();
		List<Element> parts = new ArrayList<Element>();
		List<Element> secondWires = new ArrayList<Element>();
		List<Element> secondParts = new ArrayList<Element>();
		for (Element el : candidates) {
			if (clip != null && !mayBeVisible(el, clip)) {
				continue;
			}
			if (el instanceof Wire) {
				(second.contains(el) ? secondWires : wires).add(el);
			} else {
				(second.contains(el) ? secondParts : parts).add(el);
			}
		}
		for (Element el : wires) {
			ElementRenderers.draw(g, el);
		}
		for (Element el : parts) {
			ElementRenderers.draw(g, el);
		}
		for (Element el : secondWires) {
			ElementRenderers.draw(g, el);
		}
		for (Element el : secondParts) {
			ElementRenderers.draw(g, el);
		}
	} // end of draw method

	/**
	 * Whether an element could draw inside the clip. The margin generously
	 * covers labels drawn near (but outside) an element's bounds.
	 *
	 * @param el The element to test.
	 * @param clip The clip rectangle.
	 *
	 * @return true if the element's margin-padded bounds intersect the clip.
	 */
	private static boolean mayBeVisible(Element el, Rectangle clip) {

		Rectangle b = el.getIndexBounds();
		b.grow(DRAW_MARGIN, DRAW_MARGIN);
		return b.intersects(clip);
	} // end of mayBeVisible method

	/**
	 * Print the circuit. Moved verbatim from {@code Circuit.print} (issue
	 * #77); the circuit no longer implements {@link Printable} - this
	 * renderer does.
	 *
	 * @param g The graphics object to use.
	 * @param format Page format info.
	 * @param pagenum Ignored.
	 * @return Printable.PAGE_EXISTS.
	 *
	 * @jls.testedby jls.PrintPathSmokeTest#printingTheCircuitDirectlyRenders()
	 */
	@Override
	public int print(Graphics g, PageFormat format, int pagenum) {

		// use better graphics
		Graphics2D gg = (Graphics2D) g;

		// construct name
		Circuit c = circuit;
		String nm = circuit.getName();
		SubCircuit se;
		while ((se = c.getSubElement()) != null) {
			c = se.getCircuit();
			nm += " in " + c.getName();
		}

		// set up
		FontMetrics fm = gg.getFontMetrics();
		int ascent = fm.getAscent();
		int descent = fm.getDescent();
		int fontHeight = ascent + descent;

		// get bounds of actual circuit
		Rectangle rect = circuit.getBounds();

		// translate to page area
		double width = format.getImageableWidth();
		double height = format.getImageableHeight();
		gg.translate(format.getImageableX(), format.getImageableY());

		// draw title
		gg.drawString(nm, 0, ascent);

		// translate and scale to fit circuit to remaining page area
		gg.translate(0, fontHeight * 2);
		height -= fontHeight * 2;
		double scale = 1.0;
		if (rect.width > width) {
			scale = width / rect.width;
		}
		if (rect.height + Geometry.POINT_DIAMETER > height) {
			scale = Math.min(scale, height
					/ (rect.height + Geometry.POINT_DIAMETER));
		}
		gg.scale(scale, scale);
		gg.translate(-rect.x + Geometry.POINT_DIAMETER / 2, -rect.y
				+ Geometry.POINT_DIAMETER / 2);

		// print
		try {
			draw(gg, new HashSet<Element>(), null);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Printable.PAGE_EXISTS;
	} // end of print method

	/**
	 * Add this circuit to the print book, add any of its state machines,
	 * truth tables and all subcircuits. Moved verbatim from
	 * {@code Circuit.addToBook} (issue #77); the circuit's page is this
	 * renderer (a {@link Printable}), and subcircuits recurse through
	 * their own renderers.
	 *
	 * @param book The book to add to.
	 * @param format The page format to use.
	 *
	 * @jls.testedby jls.PrintPathSmokeTest#everyBookedPagePrintsIntoAGraphics()
	 */
	public void addToBook(Book book, PageFormat format) {

		// add this circuit
		book.append(this, format);

		// canonical page order (#182): iterating the element HashSet
		// would order the pages by identity hash, which varies between
		// runs - two prints of one circuit could page differently
		List<Element> ordered = circuit.getElementsInStableOrder();

		// add state machines
		for (Element el : ordered) {
			if (el instanceof StateMachine sm) {
				book.append(sm, format);
				Printable p = sm.makeOutSum();
				if (p != null)
					book.append(p, format);
			}
		}

		// add truth tables
		for (Element el : ordered) {
			if (el instanceof TruthTable tt) {
				book.append(tt, format);
			}
		}

		// add subcircuits
		for (Element el : ordered) {
			if (el instanceof SubCircuit sub) {
				CircuitRenderer.of(sub.getSubCircuit()).addToBook(book, format);
			}
		}
	} // end of addToBook method

	/**
	 * Export an image of the circuit. Moved verbatim from
	 * {@code Circuit.exportImage} (issue #77).
	 *
	 * @param file The name of the file to write to.
	 * @throws Exception if the image file cannot be written.
	 *
	 * @jls.testedby jls.ElementDrawSmokeTest#everyElementDrawsOnTheRasterExportPath()
	 * @jls.testedby jls.ElementDrawSmokeTest#everyElementDrawsOnTheSvgExportPath()
	 * @jls.testedby jls.SvgExportTest#exportingTwiceIsByteIdentical()
	 * @jls.testedby jls.SvgExportTest#theDocumentIsAnSvgImageWithDrawnContent()
	 */
	public void exportImage(String file) throws Exception {

		// get bounds of actual circuit
		Rectangle rect = circuit.getBounds();

		// add 10 pixels on all edges
		int border = 10;
		rect = new Rectangle(rect.x - border, rect.y - border, rect.width + 2
				* border, rect.height + 2 * border);

		// vector export (issue #154): the same element paint path that
		// fills a bitmap below draws into JFreeSVG's Graphics2D instead,
		// so .svg output needs no per-element work
		if (file.toLowerCase(java.util.Locale.ROOT).endsWith(".svg")) {
			org.jfree.svg.SVGGraphics2D svg =
					new org.jfree.svg.SVGGraphics2D(rect.width, rect.height);
			// a fixed defs prefix keeps two exports of the same circuit
			// byte-identical (the default prefix is instance-derived)
			svg.setDefsKeyPrefix("jls");
			AffineTransform svgTranslate = new AffineTransform();
			svgTranslate.translate(-rect.x, -rect.y);
			svg.setTransform(svgTranslate);
			svg.setColor(Color.white);
			svg.fill(rect);
			// draw in a deterministic order: elements live in a
			// HashSet, and while raster export doesn't care (same
			// pixels either way, overlaps aside), SVG serializes the
			// draw order into the file - an unstable order would break
			// byte-identical goldens across load instances. Wires
			// under non-wires, like the interactive draw path.
			List<Element> wireLayer = new ArrayList<Element>();
			List<Element> partLayer = new ArrayList<Element>();
			for (Element el : circuit.getElements()) {
				(el instanceof Wire ? wireLayer : partLayer).add(el);
			}
			// order on index bounds, not x/y: wires keep x/y at their
			// defaults, but their bounds are derived from their ends
			java.util.Comparator<Element> drawOrder = java.util.Comparator
					.comparingInt((Element el) -> el.getIndexBounds().x)
					.thenComparingInt(el -> el.getIndexBounds().y)
					.thenComparingInt(el -> el.getIndexBounds().width)
					.thenComparingInt(el -> el.getIndexBounds().height)
					.thenComparing(el -> el.getClass().getName())
					.thenComparingInt(Element::getID);
			wireLayer.sort(drawOrder);
			partLayer.sort(drawOrder);
			for (Element el : wireLayer) {
				ElementRenderers.draw(svg, el);
			}
			for (Element el : partLayer) {
				ElementRenderers.draw(svg, el);
			}
			try {
				java.nio.file.Files.writeString(java.nio.file.Path.of(file),
						svg.getSVGDocument(),
						java.nio.charset.StandardCharsets.UTF_8);
			} finally {
				svg.dispose();
			}
			return;
		}

		// set up image
		BufferedImage image = new BufferedImage(rect.width, rect.height,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		AffineTransform translate = new AffineTransform();
		translate.translate(-rect.x, -rect.y);
		g.setTransform(translate);

		// draw the image
		g.setColor(Color.white);
		g.fill(rect);
		draw(g, new HashSet<Element>(), null);

		// write the image, the format following the file extension
		// (issue #71): .png produces PNG, anything else the legacy JPEG
		try {
			String format = file.toLowerCase(java.util.Locale.ROOT)
					.endsWith(".png") ? "png" : "jpg";
			if (!ImageIO.write(image, format, new File(file))) {
				throw new IOException("no " + format + " image writer available");
			}
		} finally {
			// clean up
			g.dispose();
			image.flush();
		}

	} // end of exportImage method

} // end of CircuitRenderer class
