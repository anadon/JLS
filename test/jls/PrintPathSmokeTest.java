package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

/**
 * Headless smoke coverage of the printing pipeline (issue #91 layer 1):
 * Circuit implements Printable and addToBook collects the circuit, its
 * state machines (plus their output-summary pages), truth tables, and
 * nested subcircuits into a print Book. None of that needs a printer or
 * a display - Printable.print draws into any Graphics2D, so rendering
 * every booked page into a BufferedImage exercises the whole path.
 */
class PrintPathSmokeTest {

	private static Circuit load() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.constant(5);
		cb.clock(20, 10);
		cb.stateMachine(1, 1);
		cb.truthTable("tt", new String[] {"a"}, new String[] {"z"},
				new int[][] {{0, 1}, {1, 0}});
		cb.subCircuit("sub");
		int in = cb.inputPin("in1", 1);
		int out = cb.outputPin("out", 1);
		cb.wire(in, "output", out, "input");

		Circuit circuit = new Circuit("golden");
		assertTrue(circuit.load(new Scanner(cb.build())),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	@Test
	void everyBookedPagePrintsIntoAGraphics() throws Exception {
		Circuit circuit = load();
		Book book = new Book();
		PageFormat format = new PageFormat();
		circuit.addToBook(book, format);

		// circuit + state machine + its output summary + truth table
		// + the nested subcircuit's page
		assertTrue(book.getNumberOfPages() >= 4,
				"expected the fixture to book at least 4 pages, got "
						+ book.getNumberOfPages());

		BufferedImage image = new BufferedImage(850, 1100,
				BufferedImage.TYPE_INT_RGB);
		for (int page = 0; page < book.getNumberOfPages(); page++) {
			Graphics2D g = image.createGraphics();
			try {
				int result = book.getPrintable(page)
						.print(g, book.getPageFormat(page), page);
				assertEquals(Printable.PAGE_EXISTS, result,
						"page " + page + " must render");
			} finally {
				g.dispose();
			}
		}
	}

	@Test
	void printingTheCircuitDirectlyRenders() throws Exception {
		Circuit circuit = load();
		BufferedImage image = new BufferedImage(850, 1100,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		try {
			assertEquals(Printable.PAGE_EXISTS,
					circuit.print(g, new PageFormat(), 0));
		} finally {
			g.dispose();
		}
	}
}
