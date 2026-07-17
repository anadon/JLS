package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jls.elem.Element;

/**
 * Parity tests for the spatial index behind editor interaction (issues #3,
 * #17). The contract under test: for any query rectangle, the index returns
 * exactly the elements whose index bounds touch it, which is a superset of
 * every element an exact predicate (contains / isInside / intersects) can
 * accept. The editor keeps the exact predicates, so index-backed decisions
 * match full-scan decisions if and only if this contract holds -- including
 * after elements move.
 */
class SpatialIndexTest {

	/** Deterministic seed: failures must reproduce. */
	private static final long SEED = 20260708L;

	/**
	 * A wired circuit: constants feeding a gate feeding a pin, as saved by
	 * the editor, so the element set includes WireEnds and Wires (whose
	 * index bounds are derived from their ends, not x/y/width/height).
	 */
	static String wiredCircuitText() {
		StringBuilder text = new StringBuilder("CIRCUIT indexed\n");
		int id = 0;
		int constA = id++;
		text.append("ELEMENT Constant\n int id ").append(constA)
			.append("\n int x 60\n int y 60\n int width 24\n int height 24\n")
			.append(" Int value 5\n int base 10\n String orient \"RIGHT\"\nEND\n");
		int constB = id++;
		text.append("ELEMENT Constant\n int id ").append(constB)
			.append("\n int x 60\n int y 108\n int width 24\n int height 24\n")
			.append(" Int value 3\n int base 10\n String orient \"RIGHT\"\nEND\n");
		int gate = id++;
		text.append("ELEMENT AndGate\n int id ").append(gate)
			.append("\n int x 240\n int y 120\n int width 48\n int height 24\n")
			.append(" int bits 1\n int numInputs 2\n String orientation \"right\"\n int delay 10\nEND\n");
		int pin = id++;
		text.append("ELEMENT OutputPin\n int id ").append(pin)
			.append("\n int x 480\n int y 120\n String name \"out\"\n int bits 1\n")
			.append(" int watch 1\n String orient \"RIGHT\"\nEND\n");
		id = wire(text, id, constA, "output", gate, "input0");
		id = wire(text, id, constB, "output", gate, "input1");
		id = wire(text, id, gate, "output", pin, "input");
		text.append("ENDCIRCUIT\n");
		return text.toString();
	}

	private static int wire(StringBuilder text, int nextId,
			int fromElement, String fromPut, int toElement, String toPut) {
		int endA = nextId++;
		int endB = nextId++;
		wireEnd(text, endA, fromElement, fromPut, endB, 120, 60);
		wireEnd(text, endB, toElement, toPut, endA, 240, 132);
		return nextId;
	}

	private static void wireEnd(StringBuilder text, int id, int attachTo,
			String put, int otherEnd, int x, int y) {
		text.append("ELEMENT WireEnd\n")
			.append(" int id ").append(id).append('\n')
			.append(" int x ").append(x).append('\n')
			.append(" int y ").append(y).append('\n')
			.append(" String put \"").append(put).append("\"\n")
			.append(" ref attach ").append(attachTo).append('\n')
			.append(" ref wire ").append(otherEnd).append('\n')
			.append("END\n");
	}

	static Circuit loadWired() throws Exception {
		Circuit circuit = new Circuit("");
		assertTrue(circuit.load(new Scanner(wiredCircuitText())),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	/** The exact contract the index promises for a rectangle query. */
	private static Set<Element> bruteForceNear(Circuit circuit, Rectangle rect) {
		Set<Element> expected = new HashSet<Element>();
		for (Element el : circuit.getElements()) {
			Rectangle b = el.getIndexBounds();
			boolean touch = b.x <= rect.x + rect.width && rect.x <= b.x + b.width
					&& b.y <= rect.y + rect.height && rect.y <= b.y + b.height;
			if (touch) {
				expected.add(el);
			}
		}
		return expected;
	}

	private static void assertQueryParity(Circuit circuit, Random random, int trials) {
		for (int i = 0; i < trials; i += 1) {
			Rectangle rect = new Rectangle(random.nextInt(700) - 50,
					random.nextInt(400) - 50, random.nextInt(200), random.nextInt(200));
			assertEquals(bruteForceNear(circuit, rect), circuit.elementsNear(rect),
					"index query must match brute force for " + rect);
		}
	}

	@Test
	void queriesMatchBruteForceOnWiredCircuit() throws Exception {
		Circuit circuit = loadWired();
		assertTrue(circuit.getElements().size() > 10, "circuit should include wires and ends");
		assertQueryParity(circuit, new Random(SEED), 500);
	}

	@Test
	void everyContainingElementIsACandidate() throws Exception {
		Circuit circuit = loadWired();
		Random random = new Random(SEED + 1);
		for (int i = 0; i < 2000; i += 1) {
			int x = random.nextInt(700) - 50;
			int y = random.nextInt(400) - 50;
			Set<Element> candidates = circuit.elementsAt(x, y);
			for (Element el : circuit.getElements()) {
				if (el.contains(x, y)) {
					assertTrue(candidates.contains(el),
							"element containing (" + x + "," + y + ") missing from candidates: " + el);
				}
			}
		}
	}

	@Test
	void everyInsideElementIsACandidate() throws Exception {
		Circuit circuit = loadWired();
		Random random = new Random(SEED + 2);
		for (int i = 0; i < 500; i += 1) {
			Rectangle rect = new Rectangle(random.nextInt(700) - 50,
					random.nextInt(400) - 50, random.nextInt(300), random.nextInt(300));
			Set<Element> candidates = circuit.elementsNear(rect);
			for (Element el : circuit.getElements()) {
				if (el.isInside(rect)) {
					assertTrue(candidates.contains(el),
							"element inside " + rect + " missing from candidates: " + el);
				}
			}
		}
	}

	@Test
	void staysExactAfterMovesAndInvalidation() throws Exception {
		Circuit circuit = loadWired();
		Random random = new Random(SEED + 3);

		// prime the index
		circuit.elementsNear(new Rectangle(0, 0, 100, 100));

		// move a changing subset the way a drag does, keeping the index
		// current incrementally, and re-check exactness each round
		for (int round = 0; round < 20; round += 1) {
			Set<Element> moved = new HashSet<Element>();
			for (Element el : circuit.getElements()) {
				if (random.nextBoolean()) {
					el.move(JLSInfo.spacing * (random.nextInt(5) - 2),
							JLSInfo.spacing * (random.nextInt(5) - 2));
					moved.add(el);
				}
			}
			circuit.reindexAfterMove(moved);
			assertQueryParity(circuit, random, 50);
		}

		// an invalidation (gesture end) must also restore exactness
		circuit.invalidateIndex();
		assertQueryParity(circuit, random, 100);
	}

	/**
	 * Not an assertion-bearing benchmark (CI timing is noisy); prints the
	 * point-query cost of index vs full scan on a large synthetic circuit
	 * so the numbers land in the build log for issue #17/#3.
	 */
	@Test
	void reportsIndexVsScanTiming() throws Exception {
		StringBuilder text = new StringBuilder("CIRCUIT big\n");
		int id = 0;
		for (int gx = 0; gx < 70; gx += 1) {
			for (int gy = 0; gy < 70; gy += 1) {
				text.append("ELEMENT Constant\n int id ").append(id++)
					.append("\n int x ").append(60 + gx * 36)
					.append("\n int y ").append(60 + gy * 36)
					.append("\n int width 24\n int height 24\n Int value 1\n int base 10\n")
					.append(" String orient \"RIGHT\"\nEND\n");
			}
		}
		text.append("ENDCIRCUIT\n");
		Circuit circuit = new Circuit("");
		assertTrue(circuit.load(new Scanner(text.toString())));
		assertTrue(circuit.finishLoad(null));
		int n = circuit.getElements().size();

		Random random = new Random(SEED + 4);
		int queries = 2000;
		int[] xs = new int[queries];
		int[] ys = new int[queries];
		for (int i = 0; i < queries; i += 1) {
			xs[i] = random.nextInt(2600);
			ys[i] = random.nextInt(2600);
		}

		// full scan cost, as the old hover loop paid it
		long scanStart = System.nanoTime();
		long scanHits = 0;
		for (int i = 0; i < queries; i += 1) {
			for (Element el : circuit.getElements()) {
				if (el.contains(xs[i], ys[i])) {
					scanHits += 1;
				}
			}
		}
		long scanNanos = System.nanoTime() - scanStart;

		// index cost (includes its one-time build on the first query)
		long indexStart = System.nanoTime();
		long indexHits = 0;
		for (int i = 0; i < queries; i += 1) {
			for (Element el : circuit.elementsAt(xs[i], ys[i])) {
				if (el.contains(xs[i], ys[i])) {
					indexHits += 1;
				}
			}
		}
		long indexNanos = System.nanoTime() - indexStart;

		assertEquals(scanHits, indexHits, "index-backed hover must hit the same elements");
		System.out.printf("[#17/#3] N=%d, %d point queries: full scan %.1f ms, index %.1f ms (%.0fx)%n",
				n, queries, scanNanos / 1e6, indexNanos / 1e6,
				(double) scanNanos / Math.max(indexNanos, 1));
	}
}
