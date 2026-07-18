package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.BitSet;

import org.junit.jupiter.api.Test;

import jls.Circuit;

/**
 * The second visual channel for issue #76: a wire's value state must
 * be readable without color vision.  A wire carrying no value
 * (tri-state HiZ) draws dashed, a wire carrying a non-zero value
 * draws thick, an all-zero wire draws as the classic thin line - and
 * a wire end that is touching (about to connect) draws an open ring
 * glyph around the point.  These tests pin the stroke mapping and
 * prove, by rendering, that each state leaves a different footprint
 * on the canvas independent of the colors chosen.
 */
class WireValueChannelTest {

	@Test
	void strokeMappingCoversAllThreeValueStates() {
		BasicStroke off = Wire.strokeFor(null);
		assertNotNull(off.getDashArray(),
				"a HiZ wire must draw dashed");
		assertEquals(1.0f, off.getLineWidth(),
				"a HiZ wire stays thin");

		BitSet nonZero = new BitSet();
		nonZero.set(0);
		BasicStroke carrying = Wire.strokeFor(nonZero);
		assertNull(carrying.getDashArray(),
				"a value-carrying wire draws solid");
		assertTrue(carrying.getLineWidth() > 1.0f,
				"a non-zero wire must draw thicker than a zero wire");

		BasicStroke zero = Wire.strokeFor(new BitSet());
		assertNull(zero.getDashArray(), "an all-zero wire draws solid");
		assertEquals(1.0f, zero.getLineWidth(),
				"an all-zero wire draws as the classic thin line");
	}

	@Test
	void renderedInkDiffersByValueStateAloneNotJustColor() {
		BitSet one = new BitSet();
		one.set(0);
		int thick = inkFor(one);
		int thin = inkFor(new BitSet());
		int dashed = inkFor(null);
		assertTrue(thick > 2 * thin,
				"non-zero wire ink (" + thick + ") must clearly exceed "
						+ "zero wire ink (" + thin + ")");
		assertTrue(dashed < thin,
				"HiZ dashes (" + dashed + ") must leave gaps a solid "
						+ "line (" + thin + ") does not");
	}

	@Test
	void touchingWireEndGrowsARingGlyph() {
		int plain = endInk(false);
		int touching = endInk(true);
		assertTrue(touching > plain,
				"touching end (" + touching + " px) must add a ring "
						+ "around the plain point (" + plain + " px)");
	}

	/** Non-background pixels left by one 84px wire with a value. */
	private static int inkFor(BitSet value) {
		Circuit circuit = new Circuit("");
		WireEnd e1 = new WireEnd(circuit);
		e1.setXY(12, 24);
		WireEnd e2 = new WireEnd(circuit);
		e2.setXY(96, 24);
		Wire wire = new Wire(e1, e2);
		e1.addWire(wire);
		e2.addWire(wire);
		WireNet net = new WireNet();
		net.add(wire);
		wire.setNet(net);
		net.setValue(value);
		return render(g -> wire.draw(g));
	}

	/** Non-background pixels left by a single wire end point. */
	private static int endInk(boolean touching) {
		Circuit circuit = new Circuit("");
		WireEnd end = new WireEnd(circuit);
		end.setXY(48, 24);
		end.setTouching(touching);
		return render(g -> end.draw(g));
	}

	/** Run a draw callback on a white canvas and count non-white. */
	private static int render(java.util.function.Consumer<Graphics2D> draw) {
		BufferedImage image = new BufferedImage(120, 48,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		draw.accept(g);
		g.dispose();
		int white = Color.white.getRGB();
		int ink = 0;
		for (int y = 0; y < image.getHeight(); y += 1) {
			for (int x = 0; x < image.getWidth(); x += 1) {
				if (image.getRGB(x, y) != white) {
					ink += 1;
				}
			}
		}
		return ink;
	}

} // end of WireValueChannelTest class
