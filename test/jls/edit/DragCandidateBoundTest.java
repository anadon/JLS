package jls.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;
import jls.elem.Element;
import jls.elem.LogicElement;
import jls.elem.Output;
import jls.elem.Put;

/**
 * Headless micro-benchmark for the wire-drag connect path (issue #43).
 *
 * <p>SimpleEditor.canConnect(WireEnd, Put) used to scan every element in
 * the circuit for every wire end in the dragged net on every mouse-drag
 * event -- O(netEnds x N). It now asks the spatial index for the elements
 * near each net end ({@link Circuit#elementsAt}) and applies the exact
 * same getPut/instanceof-Output predicates to that candidate set.</p>
 *
 * <p>Timing assertions flake in CI, so the evidence here is operation
 * counts: the candidate set the rewritten loop inspects per net end is
 * bounded by a small constant that does not grow when the circuit grows
 * 10x (100 -> 1000 elements), and the puts it finds are exactly the puts
 * the full scan found, at every put location and at random probe points.</p>
 */
class DragCandidateBoundTest {

	/** Deterministic seed: failures must reproduce. */
	private static final long SEED = 20260709L;

	/**
	 * Per-probe candidate bound. Grid pitch is 36px with 24x24 elements
	 * and the point query pads by one snap spacing (12px), so at most a
	 * 2x2 block of neighbors can be candidates; 8 leaves slack for edge
	 * effects without ever scaling with circuit size.
	 */
	private static final int MAX_CANDIDATES = 8;

	/**
	 * A circuit of cols x rows constants on the same 36px grid the
	 * SpatialIndexTest big-circuit fixture uses; each constant has an
	 * output put, so every grid cell is a realistic connect target.
	 */
	private static Circuit grid(int cols, int rows) throws Exception {
		StringBuilder text = new StringBuilder("CIRCUIT grid\n");
		int id = 0;
		for (int gx = 0; gx < cols; gx += 1) {
			for (int gy = 0; gy < rows; gy += 1) {
				text.append("ELEMENT Constant\n int id ").append(id++)
					.append("\n int x ").append(60 + gx * 36)
					.append("\n int y ").append(60 + gy * 36)
					.append("\n int width 24\n int height 24\n Int value 1\n int base 10\n")
					.append(" String orient \"RIGHT\"\nEND\n");
			}
		}
		text.append("ENDCIRCUIT\n");
		Circuit circuit = new Circuit("");
		assertTrue(circuit.load(new Scanner(text.toString())),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	/** Every put location in the circuit -- the points canConnect probes. */
	private static Set<int[]> putLocations(Circuit circuit) {
		Set<int[]> points = new HashSet<int[]>();
		for (Element el : circuit.getElements()) {
			for (Put put : el.getAllPuts()) {
				points.add(new int[] { put.getX(), put.getY() });
			}
		}
		return points;
	}

	/**
	 * The work the rewritten canConnect loop does per net end: the size
	 * of the candidate set it inspects. Returns the worst case over all
	 * put locations.
	 */
	private static int worstCandidateCount(Circuit circuit) {
		int worst = 0;
		for (int[] p : putLocations(circuit)) {
			worst = Math.max(worst, circuit.elementsAt(p[0], p[1]).size());
		}
		return worst;
	}

	@Test
	void candidateSetPerNetEndIsBoundedIndependentOfCircuitSize() throws Exception {
		Circuit small = grid(10, 10);   // N = 100
		Circuit large = grid(25, 40);   // N = 1000
		int n100 = small.getElements().size();
		int n1000 = large.getElements().size();
		assertEquals(100, n100);
		assertEquals(1000, n1000);

		int worstSmall = worstCandidateCount(small);
		int worstLarge = worstCandidateCount(large);

		// the old loop inspected every element (N) per net end; the index
		// query inspects a handful regardless of N
		assertTrue(worstSmall >= 1, "probes at put locations must see the put's element");
		assertTrue(worstLarge <= MAX_CANDIDATES,
				"candidate set per net end must stay constant-sized, got "
						+ worstLarge + " for N=" + n1000);

		// growing the circuit 10x must not grow the per-probe work at all
		assertEquals(worstSmall, worstLarge,
				"per-net-end candidate work must be independent of circuit size");

		System.out.printf(
				"[#43] connect-candidate work per net end: N=%d -> %d inspected, "
						+ "N=%d -> %d inspected (old scan inspected N)%n",
				n100, worstSmall, n1000, worstLarge);
	}

	/**
	 * Exact parity of the rewritten scan: at every put location and at
	 * random probe points, iterating the index candidates finds exactly
	 * the same puts (and the same Output/tri-state classification inputs)
	 * as iterating every element, which is what preserves canConnect's
	 * accept/reject decisions.
	 */
	@Test
	void indexCandidatesFindExactlyTheSamePutsAsAFullScan() throws Exception {
		Circuit circuit = grid(10, 10);
		Random random = new Random(SEED);

		Set<int[]> probes = putLocations(circuit);
		for (int i = 0; i < 2000; i += 1) {
			probes.add(new int[] { random.nextInt(600), random.nextInt(600) });
		}

		for (int[] probe : probes) {
			int x = probe[0];
			int y = probe[1];

			// the old loop body: every element, exact predicates
			Set<Put> fullScan = new HashSet<Put>();
			int fullOutputs = 0;
			for (Element el : circuit.getElements()) {
				Put p = el.getPut(x, y);
				if (p != null) {
					fullScan.add(p);
					if (p instanceof Output) {
						fullOutputs += 1;
					}
				}
			}

			// the new loop body: index candidates, same exact predicates
			Set<Put> indexScan = new HashSet<Put>();
			int indexOutputs = 0;
			for (Element el : circuit.elementsAt(x, y)) {
				Put p = el.getPut(x, y);
				if (p != null) {
					indexScan.add(p);
					if (p instanceof Output) {
						indexOutputs += 1;
					}
				}
			}

			assertEquals(fullScan, indexScan,
					"puts found at (" + x + "," + y + ") must match the full scan");
			assertEquals(fullOutputs, indexOutputs,
					"output-put count at (" + x + "," + y + ") must match the full scan");
		}

		// sanity: the fixture actually exercises hits, not just misses
		boolean anyHit = false;
		for (Element el : circuit.getElements()) {
			if (el instanceof LogicElement) {
				for (Put p : el.getAllPuts()) {
					if (el.getPut(p.getX(), p.getY()) != null) {
						anyHit = true;
					}
				}
			}
		}
		assertTrue(anyHit, "fixture must contain probe points that hit puts");
	}
}
