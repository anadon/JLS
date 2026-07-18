package jls.ui;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.CircuitTextBuilder;
import jls.JLSInfo;
import jls.elem.Element;
import jls.elem.WireEnd;

/**
 * Layer-3 starter (issue #162): the first render-to-BufferedImage
 * semantic assertion, opening the layer reserved in the package javadoc.
 * Each fixture element is painted alone onto a white canvas - no
 * window, no display, plain Graphics2D - and everything it painted must
 * land inside its index bounds grown by the paint pipeline's own draw
 * margin ({@code Circuit.DRAW_MARGIN}, mirrored here the same way
 * {@code jls.DrawCullingParityTest} mirrors it). An element painting
 * outside that envelope would be silently truncated by clipped
 * repaints, so this is a semantic contract, not a pixel golden.
 *
 * Attached {@link WireEnd}s are exempt from the sweep: their draw()
 * deliberately paints nothing (the connection point disappears into
 * the put), which would make the containment assertion vacuous.
 */
class RenderBoundsTest {

	/** Mirrors Circuit.DRAW_MARGIN: the exact culling margin. */
	private static final int DRAW_MARGIN = 8 * JLSInfo.spacing;

	/** Slack past the allowed envelope so strays are detectable. */
	private static final int SLACK = 40;

	/** The canvas fill, packed as TYPE_INT_RGB pixels report it. */
	private static final int BACKGROUND = Color.WHITE.getRGB();

	/**
	 * A representative palette slice: gates, a source, a clock, pins
	 * (one watched), free text, and a live wire between two pins.
	 *
	 * @return the circuit save-format text.
	 */
	private static String fixture() {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.gate("AndGate", 2, 2);
		cb.gate("NotGate", 1, 1);
		cb.constant(0xAB);
		cb.clock(20, 10);
		cb.text("hello \\\"render\\\"");
		int in = cb.inputPin("in1", 1);
		int out = cb.outputPin("watched", 1);
		cb.wire(in, "output", out, "input");
		return cb.build();
	} // end of fixture method

	/**
	 * Load the fixture through the real loader.
	 *
	 * @return the loaded circuit.
	 * @throws Exception If the loader fails unexpectedly.
	 */
	private static Circuit load() throws Exception {
		Circuit circuit = new Circuit("golden");
		assertTrue(circuit.load(new Scanner(fixture())),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	} // end of load method

	/**
	 * Every fixture element paints, and paints only inside its
	 * index bounds plus the culling margin.
	 *
	 * @throws Exception If the fixture fails to load.
	 */
	@Test
	void everyFixtureElementPaintsWithinItsCullingEnvelope()
			throws Exception {
		Circuit circuit = load();

		// one canvas size covering every element's envelope plus slack,
		// so out-of-envelope painting lands on pixels we can inspect
		int width = 0;
		int height = 0;
		for (Element el : circuit.getElements()) {
			Rectangle allowed = grown(el);
			width = Math.max(width, allowed.x + allowed.width + SLACK);
			height = Math.max(height, allowed.y + allowed.height + SLACK);
		}

		for (Element el : circuit.getElements()) {
			if (el instanceof WireEnd) {
				continue; // attached ends paint nothing by design
			}
			BufferedImage image = new BufferedImage(width, height,
					BufferedImage.TYPE_INT_RGB);
			Graphics2D g = image.createGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, width, height);
			el.draw(g);
			g.dispose();
			RenderAssert.assertPaintsWithinBounds(image, BACKGROUND,
					grown(el), el.getClass().getSimpleName());
		}
	} // end of everyFixtureElementPaintsWithinItsCullingEnvelope method

	/**
	 * The element's index bounds grown by the culling margin.
	 *
	 * @param el The element.
	 * @return the allowed painting envelope.
	 */
	private static Rectangle grown(Element el) {
		Rectangle allowed = el.getIndexBounds();
		allowed.grow(DRAW_MARGIN, DRAW_MARGIN);
		return allowed;
	} // end of grown method

	// ------------------------------------------------------------------
	// assert-the-assertion pins for RenderAssert (package discipline)
	// ------------------------------------------------------------------

	/**
	 * An untouched canvas must fail: containment over nothing is
	 * vacuous, and a silently-blank render is itself a defect.
	 */
	@Test
	void assertionRejectsABlankCanvas() {
		BufferedImage image = blank(100, 100);
		assertThrows(AssertionError.class,
				() -> RenderAssert.assertPaintsWithinBounds(image,
						BACKGROUND, new Rectangle(0, 0, 100, 100), "pin"));
	} // end of assertionRejectsABlankCanvas method

	/**
	 * A pixel outside the allowed rectangle must fail.
	 */
	@Test
	void assertionRejectsAStrayPixel() {
		BufferedImage image = blank(100, 100);
		image.setRGB(90, 90, Color.BLACK.getRGB());
		assertThrows(AssertionError.class,
				() -> RenderAssert.assertPaintsWithinBounds(image,
						BACKGROUND, new Rectangle(0, 0, 50, 50), "pin"));
	} // end of assertionRejectsAStrayPixel method

	/**
	 * Painting inside the allowed rectangle passes.
	 */
	@Test
	void assertionAcceptsContainedPainting() {
		BufferedImage image = blank(100, 100);
		image.setRGB(25, 25, Color.BLACK.getRGB());
		RenderAssert.assertPaintsWithinBounds(image, BACKGROUND,
				new Rectangle(0, 0, 50, 50), "pin");
	} // end of assertionAcceptsContainedPainting method

	/**
	 * A white canvas of the given size.
	 *
	 * @param width Canvas width.
	 * @param height Canvas height.
	 * @return the canvas.
	 */
	private static BufferedImage blank(int width, int height) {
		BufferedImage image = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);
		g.dispose();
		return image;
	} // end of blank method

} // end of RenderBoundsTest class
