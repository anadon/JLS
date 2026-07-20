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
import jls.elem.JumpEnd;
import jls.elem.JumpStart;
import jls.elem.Pin;
import jls.elem.ShiftRegister;
import jls.elem.Wire;
import jls.elem.WireEnd;

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

	/** A circuit loaded headlessly from inline save-format text. */
	private static Circuit loadText(String text) throws Exception {
		Circuit circuit = new Circuit("");
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(graphics()),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	/**
	 * A jump start and its jump end, both unwired and with declared
	 * stable ids, for the removal-group closure rules.
	 */
	private static Circuit loadJumpPair() throws Exception {
		return loadText("CIRCUIT jumps\n"
				+ "ELEMENT JumpStart\n int id 0\n int x 180\n int y 60\n"
				+ " String sid \"js:1\"\n String name \"js\"\n"
				+ " int bits 1\n int watch 0\n"
				+ " String orientation \"LEFT\"\nEND\n"
				+ "ELEMENT JumpEnd\n int id 1\n int x 300\n int y 60\n"
				+ " String sid \"je:2\"\n String name \"js\"\n"
				+ " int bits 1\n String orientation \"LEFT\"\nEND\n"
				+ "ENDCIRCUIT\n");
	}

	/** A donor element block: an unwired adder with a declared sid. */
	private static String donorAdderBlock() throws Exception {
		Circuit donor = loadText("CIRCUIT donor\nELEMENT Adder\n"
				+ " int id 0\n int x 480\n int y 480\n"
				+ " String sid \"donor:1\"\n int bits 4\n"
				+ " String orient \"LEFT\"\n int delay 10\nEND\n"
				+ "ENDCIRCUIT\n");
		return ElementBlocks.saveBlock(
				find(donor, el -> el.canRotate()));
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

	@Test
	void removeMatchesInlineMutation() throws Exception {
		Circuit viaOp = loadAdder();
		Circuit inline = loadAdder();
		Element adder = find(viaOp, Element::canRotate);
		new RemoveElements(List.of(adder.getStableId()))
				.apply(viaOp, graphics());
		Element inlineAdder = find(inline, Element::canRotate);
		inlineAdder.remove(inline);
		assertEquals(save(inline), save(viaOp),
				"op and inline removal must produce identical bytes");
	}

	/**
	 * A reconfigure installs exactly the block it carries: the element
	 * the op addresses serializes, after the op, to the reconfigured
	 * bytes and nothing else (P1 for the commit-time attribute/ordered-
	 * row op kind).
	 */
	@Test
	void configReplaceInstallsExactlyTheReconfiguredBlock()
			throws Exception {
		Circuit circuit = loadAdder();
		Element adder = find(circuit, Element::canRotate);
		ElementId id = adder.getStableId();
		String reconfigured = ElementBlocks.saveBlock(adder)
				.replace("int bits 4", "int bits 8");
		assertNotEquals(ElementBlocks.saveBlock(adder), reconfigured,
				"the fixture must actually change under reconfigure");
		new SetElementConfig(id, reconfigured).apply(circuit, graphics());
		assertEquals(reconfigured,
				ElementBlocks.saveBlock(byId(circuit, id)),
				"the op must install exactly the reconfigured block");
	}

	/**
	 * Renaming an element through a reconfigure frees the old name and
	 * registers the new one - the invariant a byte comparison of the
	 * circuit save cannot see, since the name registry is not itself in
	 * the save format.
	 */
	@Test
	void configReplaceRenameUpdatesTheNameRegistry() throws Exception {
		Circuit circuit = loadText("CIRCUIT named\nELEMENT InputPin\n"
				+ " int id 0\n int x 120\n int y 120\n"
				+ " String sid \"pin:1\"\n String name \"A\"\n"
				+ " int bits 1\n int watch 0\n"
				+ " String orient \"RIGHT\"\nEND\nENDCIRCUIT\n");
		Element pin = find(circuit, el -> true);
		String renamed = ElementBlocks.saveBlock(pin)
				.replace("String name \"A\"", "String name \"B\"");
		new SetElementConfig(pin.getStableId(), renamed)
				.apply(circuit, graphics());
		assertTrue(!circuit.hasName("A"),
				"the reconfigured-away name must be freed");
		assertTrue(circuit.hasName("B"),
				"the reconfigured-in name must be registered");
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
	void removeInverseRestoresBytes() throws Exception {
		Circuit circuit = loadAdder();
		Element adder = find(circuit, Element::canRotate);
		assertInverseRestores(circuit,
				new RemoveElements(List.of(adder.getStableId())));
	}

	@Test
	void addInverseRestoresBytes() throws Exception {
		Circuit circuit = loadAdder();
		assertInverseRestores(circuit,
				new AddElements(List.of(donorAdderBlock())));
	}

	@Test
	void configReplaceInverseRestoresBytes() throws Exception {
		Circuit circuit = loadAdder();
		Element adder = find(circuit, Element::canRotate);
		String reconfigured = ElementBlocks.saveBlock(adder)
				.replace("int bits 4", "int bits 8");
		assertInverseRestores(circuit,
				new SetElementConfig(adder.getStableId(), reconfigured));
	}

	@Test
	void jumpEndAloneIsRemovableAndRestorable() throws Exception {
		Circuit circuit = loadJumpPair();
		Element end = find(circuit, el -> el instanceof JumpEnd);
		assertInverseRestores(circuit,
				new RemoveElements(List.of(end.getStableId())));
	}

	@Test
	void jumpStartWithItsEndsIsRemovableAndRestorable()
			throws Exception {
		Circuit circuit = loadJumpPair();
		Element start = find(circuit, el -> el instanceof JumpStart);
		Element end = find(circuit, el -> el instanceof JumpEnd);
		assertInverseRestores(circuit, new RemoveElements(
				List.of(start.getStableId(), end.getStableId())));
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
		assertInverseRestores(adders,
				new RemoveElements(List.of(adder.getStableId())));
		assertInverseRestores(adders,
				new AddElements(List.of(donorAdderBlock())));
		Element restoredAdder = find(adders, Element::canRotate);
		assertInverseRestores(adders, new SetElementConfig(
				restoredAdder.getStableId(),
				ElementBlocks.saveBlock(restoredAdder)
						.replace("int bits 4", "int bits 8")));
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
				new MoveElements(List.of(id, other), -24, 36),
				new AddElements(List.of(donorAdderBlock(),
						"ELEMENT Text\n String text \"multi\nline "
								+ "\\\"quoted\\\"\"\nEND\n")),
				new RemoveElements(List.of(id, other)),
				new SetElementConfig(id, "ELEMENT Adder\n int id 0\n"
						+ " int x 60\n int y 60\n String sid \"legacy:7\"\n"
						+ " int bits 4\n String orient \"LEFT\"\n"
						+ " int delay 10\nEND\n"));
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
				// stray parse-legal int fields on kinds that take
				// none: these survive the line parser and must die in
				// the per-kind field-shape check (requireFields — its
				// removal survived the issue #161 PIT trial for
				// exactly these three kinds)
				"OP ToggleWatched\n String id \"legacy:1\"\n"
						+ " int dx 1\nEND\n",
				"OP RemoveProbe\n String id \"legacy:1\"\n"
						+ " int dy 1\nEND\n",
				"OP FlipElement\n String id \"legacy:1\"\n"
						+ " int cw 1\nEND\n",
				// oversized string value
				"OP AttachProbe\n String id \"legacy:1\"\n String name \""
						+ "x".repeat(10_001) + "\"\nEND\n",
				// add without any blocks
				"OP AddElements\nEND\n",
				// remove with a block instead of ids
				"OP RemoveElements\n String block \"x\"\nEND\n",
				// stray block field on a kind that takes none
				"OP ToggleWatched\n String id \"legacy:1\"\n"
						+ " String block \"x\"\nEND\n",
				// oversized element block
				"OP AddElements\n String block \""
						+ "x".repeat(100_001) + "\"\nEND\n",
				// reconfigure with no block
				"OP SetElementConfig\n String id \"legacy:1\"\nEND\n",
				// reconfigure with two blocks
				"OP SetElementConfig\n String id \"legacy:1\"\n"
						+ " String block \"a\"\n String block \"b\"\nEND\n",
				// reconfigure with two ids
				"OP SetElementConfig\n String id \"legacy:1\"\n"
						+ " String id \"legacy:2\"\n String block \"a\"\n"
						+ "END\n",
				// reconfigure with a stray name field
				"OP SetElementConfig\n String id \"legacy:1\"\n"
						+ " String block \"a\"\n String name \"x\"\nEND\n",
		};
		for (String text : hostile) {
			assertThrows(OpRejected.class,
					() -> CircuitOpReader.read(new Scanner(text)),
					() -> "reader accepted: " + text.substring(0,
							Math.min(40, text.length())));
		}
	}

	/**
	 * The id-count limit is exact (issue #161 PIT trial: the
	 * {@code >=} boundary at the MAX_IDS check had no test on either
	 * side): a block listing exactly 10,000 ids parses, one more is
	 * rejected.
	 */
	@Test
	void readerEnforcesTheIdLimitExactly() throws Exception {
		StringBuilder atLimit = new StringBuilder("OP MoveElements\n");
		for (int i = 0; i < 10_000; i++) {
			atLimit.append(" String id \"legacy:1\"\n");
		}
		atLimit.append(" int dx 4\n int dy 0\nEND\n");
		String overLimit = atLimit.toString().replaceFirst(
				" int dx", " String id \"legacy:1\"\n int dx");
		assertTrue(CircuitOpReader.read(new Scanner(atLimit.toString()))
				instanceof MoveElements,
				"exactly 10,000 ids must parse");
		assertThrows(OpRejected.class,
				() -> CircuitOpReader.read(new Scanner(overLimit)),
				"10,001 ids must be rejected");
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

	@Test
	void addRejectionsLeaveTheCircuitUnchanged() throws Exception {
		Circuit circuit = loadAdder();
		String before = save(circuit);
		Element adder = find(circuit, Element::canRotate);
		String donor = donorAdderBlock();

		CircuitOp[] invalid = {
				// no blocks at all
				new AddElements(List.of()),
				// stable id already present in the circuit
				new AddElements(
						List.of(ElementBlocks.saveBlock(adder))),
				// same stable id twice within one op
				new AddElements(List.of(donor, donor)),
				// not an element block
				new AddElements(List.of("garbage")),
				// wiring may not travel through element blocks
				new AddElements(List.of("ELEMENT WireEnd\nEND\n")),
				// nor may subcircuits
				new AddElements(List.of("ELEMENT SubCircuit\nEND\n")),
				// unknown element type
				new AddElements(List.of("ELEMENT NoSuchThing\nEND\n")),
				// no declared stable id
				new AddElements(List.of("ELEMENT Adder\n int id 0\n"
						+ " int x 60\n int y 60\n int bits 4\n"
						+ " String orient \"LEFT\"\n int delay 10\n"
						+ "END\n")),
				// arrives off the canvas
				new AddElements(List.of("ELEMENT Adder\n int id 0\n"
						+ " int x -60\n int y 60\n"
						+ " String sid \"neg:1\"\n int bits 4\n"
						+ " String orient \"LEFT\"\n int delay 10\n"
						+ "END\n")),
				// a jump end with no source anywhere
				new AddElements(List.of("ELEMENT JumpEnd\n int id 0\n"
						+ " int x 60\n int y 60\n String sid \"je:9\"\n"
						+ " String name \"nowhere\"\n int bits 1\n"
						+ " String orientation \"LEFT\"\nEND\n")),
				// atomicity: a good block first, then a bad one
				new AddElements(List.of(donor,
						ElementBlocks.saveBlock(adder))),
		};
		for (CircuitOp op : invalid) {
			assertThrows(OpRejected.class,
					() -> op.apply(circuit, graphics()),
					() -> "accepted: " + op);
			assertEquals(before, save(circuit),
					"a rejected add must not change the circuit");
		}
	}

	@Test
	void addRejectsDuplicateNames() throws Exception {
		Circuit circuit = loadText("CIRCUIT named\nELEMENT InputPin\n"
				+ " int id 0\n int x 120\n int y 120\n"
				+ " String sid \"pin:1\"\n String name \"A\"\n"
				+ " int bits 1\n int watch 0\n"
				+ " String orient \"RIGHT\"\nEND\nENDCIRCUIT\n");
		String before = save(circuit);
		String samePinName = "ELEMENT InputPin\n int id 0\n"
				+ " int x 240\n int y 240\n String sid \"pin:2\"\n"
				+ " String name \"A\"\n int bits 1\n int watch 0\n"
				+ " String orient \"RIGHT\"\nEND\n";
		String otherName = samePinName.replace("\"A\"", "\"B\"");

		// a name already used in the circuit
		assertThrows(OpRejected.class,
				() -> new AddElements(List.of(samePinName))
						.apply(circuit, graphics()));
		assertEquals(before, save(circuit));

		// the same name twice within one op
		assertThrows(OpRejected.class, () -> new AddElements(
				List.of(otherName, otherName.replace("pin:2", "pin:3")))
						.apply(circuit, graphics()));
		assertEquals(before, save(circuit));

		// a jump start name already taken by an existing jump start
		Circuit jumps = loadJumpPair();
		String jumpsBefore = save(jumps);
		Element start = find(jumps, el -> el instanceof JumpStart);
		assertThrows(OpRejected.class, () -> new AddElements(
				List.of(ElementBlocks.saveBlock(start)
						.replace("js:1", "js:9")))
								.apply(jumps, graphics()));
		assertEquals(jumpsBefore, save(jumps));
	}

	@Test
	void removeRejectionsLeaveTheCircuitUnchanged() throws Exception {
		Circuit circuit = load();
		String before = save(circuit);
		ElementId unknown = ElementId.parse("nosuch:1");
		Element adderless = find(circuit,
				el -> el instanceof ShiftRegister);
		Element wire = find(circuit, el -> el instanceof Wire);
		Element end = find(circuit, el -> el instanceof WireEnd);
		Element wired = findWiredElement(circuit);

		CircuitOp[] invalid = {
				new RemoveElements(List.of()),
				new RemoveElements(List.of(unknown)),
				new RemoveElements(List.of(adderless.getStableId(),
						adderless.getStableId())),
				new RemoveElements(List.of(wire.getStableId())),
				new RemoveElements(List.of(end.getStableId())),
				new RemoveElements(List.of(wired.getStableId())),
		};
		for (CircuitOp op : invalid) {
			assertThrows(OpRejected.class,
					() -> op.apply(circuit, graphics()),
					() -> "accepted: " + op);
			assertEquals(before, save(circuit),
					"a rejected removal must not change the circuit");
		}

		// a jump start may not leave its jump ends behind
		Circuit jumps = loadJumpPair();
		String jumpsBefore = save(jumps);
		Element start = find(jumps, el -> el instanceof JumpStart);
		assertThrows(OpRejected.class,
				() -> new RemoveElements(List.of(start.getStableId()))
						.apply(jumps, graphics()));
		assertEquals(jumpsBefore, save(jumps));
	}

	@Test
	void removeRejectsUneditableElements() throws Exception {
		Circuit circuit = loadAdder();
		new AddElements(List.of("ELEMENT Adder\n int id 0\n"
				+ " int x 480\n int y 480\n String sid \"fx:1\"\n"
				+ " int fixed 1\n int bits 4\n String orient \"LEFT\"\n"
				+ " int delay 10\nEND\n")).apply(circuit, graphics());
		String before = save(circuit);
		assertThrows(OpRejected.class, () -> new RemoveElements(
				List.of(ElementId.parse("fx:1")))
						.apply(circuit, graphics()));
		assertEquals(before, save(circuit),
				"a rejected removal must not change the circuit");
	}

	@Test
	void configReplaceRejectionsLeaveTheCircuitUnchanged()
			throws Exception {
		// unknown id: resolution fails before the block is examined
		Circuit adders = loadAdder();
		String addersBefore = save(adders);
		Element adder = find(adders, Element::canRotate);
		ElementId adderId = adder.getStableId();
		String adderBlock = ElementBlocks.saveBlock(adder);
		assertThrows(OpRejected.class,
				() -> new SetElementConfig(ElementId.parse("nosuch:1"),
						adderBlock).apply(adders, graphics()));
		assertEquals(addersBefore, save(adders));

		// the block declares a stable id other than the one addressed
		String otherSid = adderBlock.replaceAll("String sid \"[^\"]*\"",
				"String sid \"other:9\"");
		assertThrows(OpRejected.class,
				() -> new SetElementConfig(adderId, otherSid)
						.apply(adders, graphics()));
		assertEquals(addersBefore, save(adders));

		// a wired element may not be reconfigured (fresh puts would
		// orphan its wire)
		Circuit fork = load();
		String forkBefore = save(fork);
		Element wired = findWiredElement(fork);
		assertThrows(OpRejected.class,
				() -> new SetElementConfig(wired.getStableId(),
						ElementBlocks.saveBlock(wired))
								.apply(fork, graphics()));
		assertEquals(forkBefore, save(fork));

		// a jump's name links across elements: out of scope
		Circuit jumps = loadJumpPair();
		String jumpsBefore = save(jumps);
		Element start = find(jumps, el -> el instanceof JumpStart);
		assertThrows(OpRejected.class,
				() -> new SetElementConfig(start.getStableId(),
						ElementBlocks.saveBlock(start))
								.apply(jumps, graphics()));
		assertEquals(jumpsBefore, save(jumps));
	}

	@Test
	void configReplaceRejectsTypeChange() throws Exception {
		Circuit circuit = loadText("CIRCUIT named\nELEMENT InputPin\n"
				+ " int id 0\n int x 120\n int y 120\n"
				+ " String sid \"pin:1\"\n String name \"A\"\n"
				+ " int bits 1\n int watch 0\n"
				+ " String orient \"RIGHT\"\nEND\nENDCIRCUIT\n");
		String before = save(circuit);
		Element pin = find(circuit, el -> true);
		// a well-formed Adder block carrying the pin's stable id: the
		// stable id matches, but the type does not
		String adderAtPinId = "ELEMENT Adder\n int id 0\n int x 240\n"
				+ " int y 240\n String sid \"pin:1\"\n int bits 4\n"
				+ " String orient \"LEFT\"\n int delay 10\nEND\n";
		assertThrows(OpRejected.class,
				() -> new SetElementConfig(pin.getStableId(), adderAtPinId)
						.apply(circuit, graphics()));
		assertEquals(before, save(circuit),
				"a rejected reconfigure must not change the circuit");
	}

	@Test
	void configReplaceRejectsNameCollision() throws Exception {
		Circuit circuit = loadText("CIRCUIT named\nELEMENT InputPin\n"
				+ " int id 0\n int x 120\n int y 120\n"
				+ " String sid \"pin:1\"\n String name \"A\"\n"
				+ " int bits 1\n int watch 0\n"
				+ " String orient \"RIGHT\"\nEND\n"
				+ "ELEMENT InputPin\n int id 1\n int x 120\n int y 240\n"
				+ " String sid \"pin:2\"\n String name \"B\"\n"
				+ " int bits 1\n int watch 0\n"
				+ " String orient \"RIGHT\"\nEND\nENDCIRCUIT\n");
		String before = save(circuit);
		Element a = find(circuit,
				el -> "A".equals(el.getName()));
		String renamed = ElementBlocks.saveBlock(a)
				.replace("String name \"A\"", "String name \"B\"");
		assertThrows(OpRejected.class,
				() -> new SetElementConfig(a.getStableId(), renamed)
						.apply(circuit, graphics()));
		assertEquals(before, save(circuit),
				"a rejected reconfigure must not change the circuit");
	}

	/**
	 * The element in the circuit whose stable id matches, in canonical
	 * order (there is exactly one).
	 */
	private static Element byId(Circuit circuit, ElementId id) {
		return find(circuit, el -> el.getStableId().equals(id));
	}

	/**
	 * An element in the circuit that has a wire attached to one of its
	 * puts, located from the wire-end side (the puts themselves are not
	 * enumerable through the public element API).
	 */
	private static Element findWiredElement(Circuit circuit) {
		WireEnd attached = (WireEnd) find(circuit,
				el -> el instanceof WireEnd
						&& ((WireEnd) el).isAttached()
						&& ((WireEnd) el).getPut().getElement() != null);
		return attached.getPut().getElement();
	}

} // end of CircuitOpTest class
