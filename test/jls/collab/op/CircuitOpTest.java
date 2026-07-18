package jls.collab.op;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;
import jls.elem.Element;
import jls.elem.ElementId;
import jls.elem.Pin;
import jls.elem.ShiftRegister;
import jls.elem.Wire;

/**
 * The operation layer's contract (issue #167, collab Stage 0b), pinned
 * against the canonical-serialization oracle (issue #166):
 *
 * <ul>
 * <li>P1 - applying an op produces the same canonical bytes as the
 * inline mutation it reifies;</li>
 * <li>P2 - applying an op and then its inverse returns the canonical
 * save to its prior bytes, on freshly loaded and on save/load-restored
 * circuits alike (issue #167 section 10);</li>
 * <li>P3 - op serialization round-trips through
 * {@link CircuitOpReader}, and the reader rejects malformed input
 * rather than repairing it;</li>
 * <li>rejection - an op that fails validation leaves the circuit's
 * bytes untouched, including when validation fails partway through a
 * multi-element op.</li>
 * </ul>
 */
class CircuitOpTest {

	/** The wire-and-ref-heavy fixture (same as DeterministicSaveTest). */
	private static final Path FORK_FIXTURE =
			Path.of("test", "fixtures", "fork-4.6-shiftregister.jls");

	private static Circuit load() throws Exception {
		Circuit circuit = new Circuit("");
		assertTrue(circuit.load(new Scanner(
				Files.readString(FORK_FIXTURE, StandardCharsets.UTF_8))),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(graphics()),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	/** A circuit restored through a save/load round trip (section 10). */
	private static Circuit restored(Circuit circuit) throws Exception {
		Circuit copy = new Circuit("");
		assertTrue(copy.load(new Scanner(save(circuit))),
				() -> "restore load failed: " + JLSInfo.loadError);
		assertTrue(copy.finishLoad(graphics()),
				() -> "restore finishLoad failed: " + JLSInfo.loadError);
		return copy;
	}

	/**
	 * A one-element circuit whose element is unwired, so rotate and
	 * flip are permitted (both are rejected while puts are attached -
	 * the same invariant the editor's menu enforces).
	 */
	private static Circuit loadAdder() throws Exception {
		Circuit circuit = new Circuit("");
		assertTrue(circuit.load(new Scanner(
				"CIRCUIT ops\nELEMENT Adder\n int id 0\n int x 240\n"
						+ " int y 240\n int bits 4\n String orient \"LEFT\"\n"
						+ " int delay 10\nEND\nENDCIRCUIT\n")),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(graphics()),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	private static String save(Circuit circuit) {
		StringWriter out = new StringWriter();
		try (PrintWriter writer = new PrintWriter(out)) {
			circuit.save(writer);
		}
		return out.toString();
	}

	private static Graphics2D graphics() {
		return new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB)
				.createGraphics();
	}

	/**
	 * The first matching element in canonical (stable-id) order, so
	 * every run and every load picks the same one.
	 */
	private static Element find(Circuit circuit, Predicate<Element> p) {
		List<Element> matches = new ArrayList<>();
		for (Element el : circuit.getElements()) {
			if (p.test(el)) {
				matches.add(el);
			}
		}
		matches.sort(Comparator.comparing(Element::getStableId));
		assertTrue(!matches.isEmpty(), "fixture lacks a needed element");
		return matches.get(0);
	}

	private static String serialize(CircuitOp op) {
		StringWriter out = new StringWriter();
		try (PrintWriter writer = new PrintWriter(out)) {
			op.save(writer);
		}
		return out.toString();
	}

	// ------------------------------------------------------------------
	// P1: op-vs-inline parity
	// ------------------------------------------------------------------

	@Test
	void toggleWatchedMatchesInlineMutation() throws Exception {
		Circuit viaOp = load();
		Circuit inline = load();
		Element pin = find(viaOp, el -> el instanceof Pin && el.canWatch());
		new ToggleWatched(pin.getStableId()).apply(viaOp, graphics());
		Element inlinePin = find(inline,
				el -> el instanceof Pin && el.canWatch());
		inlinePin.setWatched(!inlinePin.isWatched());
		assertEquals(save(inline), save(viaOp),
				"op and inline watch toggle must produce identical bytes");
	}

	@Test
	void rotateMatchesInlineMutation() throws Exception {
		Circuit viaOp = loadAdder();
		Circuit inline = loadAdder();
		Element adder = find(viaOp, Element::canRotate);
		new RotateElement(adder.getStableId(), true)
				.apply(viaOp, graphics());
		find(inline, Element::canRotate)
				.rotate(JLSInfo.Orientation.RIGHT, graphics());
		assertEquals(save(inline), save(viaOp),
				"op and inline rotation must produce identical bytes");
	}

	// ------------------------------------------------------------------
	// P2: apply then invert restores the canonical bytes
	// ------------------------------------------------------------------

	private static void assertInverseRestores(Circuit circuit,
			CircuitOp op) throws Exception {
		String before = save(circuit);
		CircuitOp inverse = op.invert(circuit);
		op.apply(circuit, graphics());
		assertNotEquals(before, save(circuit),
				"the op must change the canonical bytes");
		inverse.apply(circuit, graphics());
		assertEquals(before, save(circuit),
				"applying the inverse must restore the canonical bytes");
	}

	@Test
	void toggleWatchedInverseRestoresBytes() throws Exception {
		Circuit circuit = load();
		Element pin = find(circuit,
				el -> el instanceof Pin && el.canWatch());
		assertInverseRestores(circuit,
				new ToggleWatched(pin.getStableId()));
	}

	@Test
	void rotateInverseRestoresBytes() throws Exception {
		Circuit circuit = loadAdder();
		Element adder = find(circuit, Element::canRotate);
		assertInverseRestores(circuit,
				new RotateElement(adder.getStableId(), true));
		assertInverseRestores(circuit,
				new RotateElement(adder.getStableId(), false));
	}

	@Test
	void flipInverseRestoresBytes() throws Exception {
		Circuit circuit = loadAdder();
		Element adder = find(circuit, Element::canFlip);
		assertInverseRestores(circuit,
				new FlipElement(adder.getStableId()));
	}

	@Test
	void probeAttachAndRemoveAreMutualInverses() throws Exception {
		Circuit circuit = load();
		Element wire = find(circuit, el -> el instanceof Wire);
		assertInverseRestores(circuit,
				new AttachProbe(wire.getStableId(), "p0"));
		// now with the probe attached, removal inverts back to it
		new AttachProbe(wire.getStableId(), "p0")
				.apply(circuit, graphics());
		assertInverseRestores(circuit,
				new RemoveProbe(wire.getStableId()));
	}

	@Test
	void moveInverseRestoresBytes() throws Exception {
		Circuit circuit = load();
		Element reg = find(circuit, el -> el instanceof ShiftRegister);
		assertInverseRestores(circuit, new MoveElements(
				List.of(reg.getStableId()), 24, 12));
	}

	@Test
	void inversesHoldOnARestoredCircuit() throws Exception {
		// section 10 threat: ops validated on a live circuit may behave
		// differently on a restored object graph
		Circuit circuit = restored(load());
		Element pin = find(circuit,
				el -> el instanceof Pin && el.canWatch());
		assertInverseRestores(circuit,
				new ToggleWatched(pin.getStableId()));
		Circuit adders = restored(loadAdder());
		Element adder = find(adders, Element::canRotate);
		assertInverseRestores(adders,
				new RotateElement(adder.getStableId(), true));
	}

	// ------------------------------------------------------------------
	// P3: serialization round-trips; the reader is strict
	// ------------------------------------------------------------------

	@Test
	void serializationRoundTripsForEveryKind() throws Exception {
		ElementId id = ElementId.parse("legacy:7");
		ElementId other = ElementId.parse("legacy:9");
		List<CircuitOp> ops = List.of(
				new ToggleWatched(id),
				new AttachProbe(id, "a \"quoted\\\" name\nwith lines"),
				new RemoveProbe(id),
				new RotateElement(id, true),
				new RotateElement(id, false),
				new FlipElement(id),
				new MoveElements(List.of(id, other), -24, 36));
		for (CircuitOp op : ops) {
			String text = serialize(op);
			CircuitOp parsed = CircuitOpReader.read(new Scanner(text));
			assertEquals(op, parsed,
					"parsed op must equal the one that was saved");
			assertEquals(text, serialize(parsed),
					"save -> read -> save must be byte-identical");
		}
	}

	@Test
	void readerRejectsMalformedInput() {
		String[] hostile = {
				// not an op block at all
				"ELEMENT AndGate\nEND\n",
				// unknown kind
				"OP FormatDisk\n String id \"legacy:1\"\nEND\n",
				// missing END
				"OP ToggleWatched\n String id \"legacy:1\"\n",
				// malformed id
				"OP ToggleWatched\n String id \"::::\"\nEND\n",
				// unknown field
				"OP ToggleWatched\n String id \"legacy:1\"\n int evil 1\nEND\n",
				// wrong field shape for the kind
				"OP MoveElements\n String id \"legacy:1\"\n int dx 4\nEND\n",
				// out-of-range rotate flag
				"OP RotateElement\n String id \"legacy:1\"\n int cw 2\nEND\n",
				// duplicate int field
				"OP MoveElements\n String id \"legacy:1\"\n"
						+ " int dx 4\n int dx 6\n int dy 0\nEND\n",
				// probe without a name
				"OP AttachProbe\n String id \"legacy:1\"\nEND\n",
				// oversized string value
				"OP AttachProbe\n String id \"legacy:1\"\n String name \""
						+ "x".repeat(10_001) + "\"\nEND\n",
		};
		for (String text : hostile) {
			assertThrows(OpRejected.class,
					() -> CircuitOpReader.read(new Scanner(text)),
					() -> "reader accepted: " + text.substring(0,
							Math.min(40, text.length())));
		}
	}

	// ------------------------------------------------------------------
	// Rejection leaves the circuit untouched
	// ------------------------------------------------------------------

	@Test
	void rejectionsLeaveTheCircuitUnchanged() throws Exception {
		Circuit circuit = load();
		String before = save(circuit);
		ElementId unknown = ElementId.parse("nosuch:1");
		Element pin = find(circuit,
				el -> el instanceof Pin && el.canWatch());
		Element wire = find(circuit, el -> el instanceof Wire);
		Element constant = find(circuit,
				el -> !el.canWatch() && !(el instanceof Wire)
						&& !el.canRotate());

		CircuitOp[] invalid = {
				new ToggleWatched(unknown),
				new ToggleWatched(constant.getStableId()),
				new AttachProbe(pin.getStableId(), "p"),
				new AttachProbe(wire.getStableId(), ""),
				new RemoveProbe(wire.getStableId()),
				new RotateElement(constant.getStableId(), true),
				new FlipElement(constant.getStableId()),
				new MoveElements(List.of(), 8, 8),
				new MoveElements(List.of(pin.getStableId(),
						pin.getStableId()), 8, 8),
				new MoveElements(List.of(pin.getStableId(), unknown),
						8, 8),
				new MoveElements(List.of(pin.getStableId()),
						-100_000, 0),
		};
		for (CircuitOp op : invalid) {
			assertThrows(OpRejected.class,
					() -> op.apply(circuit, graphics()));
			assertEquals(before, save(circuit),
					"a rejected op must not change the circuit");
		}
	}

} // end of CircuitOpTest class
