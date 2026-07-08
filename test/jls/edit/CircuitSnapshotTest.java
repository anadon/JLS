package jls.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;
import jls.elem.Element;

/**
 * Undo snapshot tests (issue #18, stage 1). Undo now stores the circuit
 * serialized in the save format instead of a live deep copy, so undo
 * semantics are exactly save/load semantics: capture -> restore -> save
 * must reproduce the original save text, an unchanged circuit must
 * snapshot identically (that is what drops no-op undo entries), and the
 * retained bytes must be far below a deep copy's object-graph cost.
 */
class CircuitSnapshotTest {

	private static final String CIRCUIT_TEXT =
			"CIRCUIT snaptest\n"
			+ "ELEMENT Constant\n"
			+ " int id 0\n int x 60\n int y 60\n int width 24\n int height 24\n"
			+ " Int value 5\n int base 10\n String orient \"RIGHT\"\nEND\n"
			+ "ELEMENT AndGate\n"
			+ " int id 1\n int x 240\n int y 120\n int width 48\n int height 24\n"
			+ " int bits 1\n int numInputs 2\n String orientation \"right\"\n int delay 10\nEND\n"
			+ "ELEMENT WireEnd\n"
			+ " int id 2\n int x 120\n int y 72\n String put \"output\"\n"
			+ " ref attach 0\n ref wire 3\nEND\n"
			+ "ELEMENT WireEnd\n"
			+ " int id 3\n int x 240\n int y 132\n String put \"input0\"\n"
			+ " ref attach 1\n ref wire 2\nEND\n"
			+ "ENDCIRCUIT\n";

	private static Circuit load(String text) throws Exception {
		Circuit circuit = new Circuit("snaptest");
		assertTrue(circuit.load(new Scanner(text)), () -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null), () -> "finishLoad: " + JLSInfo.loadError);
		return circuit;
	}

	private static String save(Circuit circuit) {
		StringWriter out = new StringWriter();
		try (PrintWriter writer = new PrintWriter(out)) {
			circuit.save(writer);
		}
		return out.toString();
	}

	@Test
	void captureRestoreReproducesTheCircuit() throws Exception {
		Circuit circuit = load(CIRCUIT_TEXT);
		String before = save(circuit);

		CircuitSnapshot snap = CircuitSnapshot.capture(circuit);
		Circuit restored = snap.restore("snaptest", null);
		assertNotNull(restored, () -> "restore failed: " + JLSInfo.loadError);
		assertEquals(circuit.getElements().size(), restored.getElements().size());

		// elements live in a HashSet, so block order and the ids/refs
		// assigned at save time differ between circuit instances; compare
		// the order- and id-independent canonical form
		assertEquals(canonicalize(before), canonicalize(save(restored)),
				"a restored snapshot must save to equivalent content");
	}

	/**
	 * Canonical form of a saved circuit: ELEMENT..END blocks sorted, with
	 * save-order-dependent id and ref lines removed.
	 */
	private static String canonicalize(String saved) {
		java.util.List<String> blocks = new java.util.ArrayList<>();
		StringBuilder current = null;
		StringBuilder header = new StringBuilder();
		for (String line : saved.split("\n")) {
			if (line.startsWith("ELEMENT")) {
				current = new StringBuilder();
			}
			if (current == null) {
				header.append(line).append('\n');
			} else if (!line.startsWith(" int id ") && !line.startsWith(" ref ")) {
				current.append(line).append('\n');
			}
			if (line.equals("END") && current != null) {
				blocks.add(current.toString());
				current = null;
			}
		}
		java.util.Collections.sort(blocks);
		return header + String.join("", blocks);
	}

	@Test
	void capturePreservesTheChangedFlag() throws Exception {
		Circuit circuit = load(CIRCUIT_TEXT);
		circuit.markChanged();
		assertTrue(circuit.hasChanged());
		CircuitSnapshot.capture(circuit);
		assertTrue(circuit.hasChanged(),
				"capturing must not clear the circuit's changed flag");
	}

	@Test
	void unchangedCircuitSnapshotsIdentically() throws Exception {
		Circuit circuit = load(CIRCUIT_TEXT);
		CircuitSnapshot first = CircuitSnapshot.capture(circuit);
		CircuitSnapshot second = CircuitSnapshot.capture(circuit);
		assertTrue(first.sameAs(second),
				"no-op changes must produce identical snapshots (undo skips them)");
	}

	@Test
	void changedCircuitSnapshotsDifferently() throws Exception {
		Circuit circuit = load(CIRCUIT_TEXT);
		CircuitSnapshot first = CircuitSnapshot.capture(circuit);
		for (Element el : circuit.getElements()) {
			el.move(JLSInfo.spacing, 0);
			break;
		}
		CircuitSnapshot second = CircuitSnapshot.capture(circuit);
		assertFalse(first.sameAs(second),
				"a real change must produce a different snapshot");
	}

	@Test
	void snapshotIsCompact() throws Exception {
		// a large circuit: 500 constants
		StringBuilder text = new StringBuilder("CIRCUIT big\n");
		for (int i = 0; i < 500; i += 1) {
			text.append("ELEMENT Constant\n int id ").append(i)
				.append("\n int x ").append(60 + (i % 20) * 36)
				.append("\n int y ").append(60 + (i / 20) * 36)
				.append("\n int width 24\n int height 24\n Int value ").append(i)
				.append("\n int base 10\n String orient \"RIGHT\"\nEND\n");
		}
		text.append("ENDCIRCUIT\n");
		Circuit circuit = new Circuit("big");
		assertTrue(circuit.load(new Scanner(text.toString())));
		assertTrue(circuit.finishLoad(null));

		CircuitSnapshot snap = CircuitSnapshot.capture(circuit);
		int perElement = snap.retainedBytes() / circuit.getElements().size();
		System.out.printf("[#18] snapshot of %d elements retains %d bytes (%d/element)%n",
				circuit.getElements().size(), snap.retainedBytes(), perElement);

		// a deep copy retains hundreds of bytes per element (object headers,
		// field storage, collection nodes); the deflated save text must be
		// well under that
		assertTrue(perElement < 100,
				"deflated snapshot should cost <100 bytes/element, was " + perElement);

		Circuit restored = snap.restore("big", null);
		assertNotNull(restored);
		assertEquals(circuit.getElements().size(), restored.getElements().size());
	}
}
