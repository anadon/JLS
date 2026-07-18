package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jls.elem.LogicElement;
import jls.sim.BatchSimulator;
import jls.sim.TraceSample;

/**
 * The batch trace pipeline across the issue #77 core/GUI boundary:
 * the headless BatchSimulator records TraceSample lists for watched
 * elements and exposes them through getTraceSamples, and the GUI-side
 * BatchTracePrinter renders those samples without needing a real
 * printer (its Printable draws into any Graphics2D, the same headless
 * technique as PrintPathSmokeTest).
 */
class BatchTracePrinterTest {

	@TempDir
	java.nio.file.Path tmp;

	/**
	 * A 4-bit positive-edge register (delay 8) clocked from a
	 * free-running clock (cycle 20, high 10) capturing constant 5,
	 * with a watched 4-bit pin q on Q. The first rising edge is at 10,
	 * so q settles at 5 at time 18 (the same clocked-register fixture
	 * as VcdExportGoldenTest, via CircuitTextBuilder).
	 */
	private static Circuit load() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		int clock = cb.clock(20, 10);
		int constant = cb.constant(5);
		int register = cb.register(4, 0, "pff");
		int q = cb.outputPin("q", 4);
		cb.wire(clock, "output", register, "C");
		cb.wire(constant, "output", register, "D");
		cb.wire(register, "Q", q, "input");

		Circuit circuit = new Circuit("wave");
		assertTrue(circuit.load(new Scanner(cb.build())),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	/**
	 * Run the fixture for 100 time units with trace accumulation
	 * enabled (a VCD consumer is set; nothing is written).
	 */
	private BatchSimulator run() throws Exception {
		Circuit circuit = load();
		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(100);
		sim.setVcdFile(tmp.resolve("unused.vcd").toString());
		sim.runSim();
		return sim;
	}

	@Test
	void traceSamplesRecordTheWatchedRun() throws Exception {
		BatchSimulator sim = run();
		Map<LogicElement,List<TraceSample>> traces = sim.getTraceSamples();

		// every watched element's trace opens with a time-0 sample
		assertTrue(traces.size() >= 2,
				"register and q pin must both be watched, got "
						+ traces.size() + " traces");
		for (List<TraceSample> samples : traces.values()) {
			assertEquals(0, samples.getFirst().time(),
					"every trace must start with a time-0 sample");
		}

		// q: 0 at time 0, then 5 (first rising edge 10 + delay 8)
		List<TraceSample> q = null;
		for (Map.Entry<LogicElement,List<TraceSample>> e
				: traces.entrySet()) {
			if (e.getKey().getName().equals("q")) {
				q = e.getValue();
			}
		}
		assertTrue(q != null, "the watched pin q must have a trace");
		BitSet five = new BitSet();
		five.set(0);
		five.set(2);
		assertEquals(2, q.size(),
				"q changes exactly once after its initial sample");
		assertEquals(new TraceSample(0, new BitSet()), q.get(0),
				"q starts at 0");
		assertEquals(new TraceSample(18, five), q.get(1),
				"q captures 5 one register delay after the first edge");

		// the exposed map is a read-only view
		final Map<LogicElement,List<TraceSample>> view = traces;
		assertThrows(UnsupportedOperationException.class,
				() -> view.clear(),
				"getTraceSamples must return a read-only view");
	}

	@Test
	void tracePrintableRendersTheRecordedSamples() throws Exception {
		BatchSimulator sim = run();
		Printable pr = BatchTracePrinter.tracePrintable(
				sim.getTraceSamples());

		BufferedImage image = new BufferedImage(1100, 850,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		try {
			g.setColor(java.awt.Color.white);
			g.fillRect(0, 0, image.getWidth(), image.getHeight());
			g.setColor(java.awt.Color.black);
			assertEquals(Printable.PAGE_EXISTS,
					pr.print(g, new PageFormat(), 0),
					"the single trace page must render");
			assertEquals(Printable.NO_SUCH_PAGE,
					pr.print(g, new PageFormat(), 1),
					"there is only one trace page");
		} finally {
			g.dispose();
		}

		// the page actually drew something: after filling the page
		// white, the rendered tic marks, trace lines, and labels must
		// leave at least one non-white pixel
		boolean drewSomething = false;
		for (int x = 0; x < image.getWidth() && !drewSomething; x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				if ((image.getRGB(x, y) & 0xffffff) != 0xffffff) {
					drewSomething = true;
					break;
				}
			}
		}
		assertTrue(drewSomething,
				"rendering the traces must mark the page");
	}
}
