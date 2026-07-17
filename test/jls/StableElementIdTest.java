package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jls.edit.CircuitSnapshot;
import jls.elem.Element;
import jls.elem.ElementId;
import jls.elem.Wire;

/**
 * Stable element identity (issue #165, collab Stage 0a). Every element
 * carries a permanent id minted at creation: elements loaded from files
 * that predate the {@code sid} attribute get one minted deterministically
 * in file order, saved files persist it, undo restore preserves it, and
 * a copy is a new element with a fresh id. The historical {@code int id}
 * stays what it always was - a file-local reference index reassigned on
 * every save.
 */
class StableElementIdTest {

	/** A legacy (pre-sid) circuit with wires, so refs are exercised. */
	private static final String LEGACY_TEXT =
			"CIRCUIT sids\n"
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
		Circuit circuit = new Circuit("sids");
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
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

	/**
	 * The stable id of each saved element, keyed by its logical identity
	 * (type and position) rather than block position - save order is not
	 * part of this issue's contract.
	 */
	private static Map<String, String> sidsByLogicalElement(Circuit circuit) {
		Map<String, String> sids = new HashMap<String, String>();
		for (Element el : circuit.getElements()) {
			if (el instanceof Wire) {
				continue; // wires are reconstructed, never saved
			}
			String key = el.getClass().getSimpleName()
					+ "@" + el.getX() + "," + el.getY();
			assertFalse(sids.containsKey(key),
					"fixture elements must be logically distinct: " + key);
			sids.put(key, el.getStableId().toString());
		}
		return sids;
	}

	@Test
	void legacyLoadsMintTheSameIdsInEveryInstance() throws Exception {
		// P1: pre-change, ids differed between load instances; now the
		// legacy minting is deterministic in file order
		Map<String, String> first = sidsByLogicalElement(load(LEGACY_TEXT));
		Map<String, String> second = sidsByLogicalElement(load(LEGACY_TEXT));
		assertEquals(first, second,
				"two loads of the same legacy file must mint identical "
						+ "stable ids for the same logical elements");
	}

	@Test
	void savedIdsSurviveReload() throws Exception {
		Circuit first = load(LEGACY_TEXT);
		Map<String, String> minted = sidsByLogicalElement(first);
		String saved = save(first);
		assertTrue(saved.contains(" String sid \""),
				"a save must persist the stable ids:\n" + saved);
		Map<String, String> reloaded = sidsByLogicalElement(load(saved));
		assertEquals(minted, reloaded,
				"stable ids must survive save -> load unchanged");
	}

	@Test
	void undoRestorePreservesIds() throws Exception {
		// P2: undo snapshots restore through the load path, so identity
		// must ride along
		Circuit circuit = load(LEGACY_TEXT);
		Circuit restored = CircuitSnapshot.capture(circuit)
				.restore("sids", null);
		assertNotNull(restored, () -> "restore failed: " + JLSInfo.loadError);
		assertEquals(sidsByLogicalElement(circuit),
				sidsByLogicalElement(restored),
				"undo restore must preserve every element's stable id");
	}

	@Test
	void copyMintsAFreshId() throws Exception {
		// P3: a pasted element is a new element
		Circuit circuit = load(LEGACY_TEXT);
		for (Element el : circuit.getElements()) {
			Element copy = el.copy();
			if (copy == null) {
				continue; // wires/wire ends copy through the editor
			}
			assertNotEquals(el.getStableId(), copy.getStableId(),
					copy.getClass().getSimpleName()
							+ ": a copy must mint a fresh stable id");
		}
	}

	@Test
	void idsAreUniqueWithinACircuit() throws Exception {
		Circuit circuit = load(LEGACY_TEXT);
		Set<ElementId> seen = new HashSet<ElementId>();
		for (Element el : circuit.getElements()) {
			assertTrue(seen.add(el.getStableId()),
					"duplicate stable id in circuit: " + el.getStableId());
		}
	}

	@Test
	void duplicateFileIdsAreRejected() throws Exception {
		String duplicated = LEGACY_TEXT
				.replace(" Int value 5\n", " Int value 5\n String sid \"legacy:7\"\n")
				.replace(" int delay 10\n", " int delay 10\n String sid \"legacy:7\"\n");
		Circuit circuit = new Circuit("sids");
		assertTrue(circuit.load(new Scanner(duplicated)),
				() -> "load failed early: " + JLSInfo.loadError);
		assertFalse(circuit.finishLoad(null),
				"a file declaring the same stable id twice must be refused");
		assertNotNull(JLSInfo.lastLoadError,
				"the refusal must publish a structured LoadError");
		assertTrue(JLSInfo.lastLoadError.render().contains("legacy:7"),
				"the error must name the duplicated id: "
						+ JLSInfo.lastLoadError.render());
	}

	@Test
	void mintedIdsSkipIdsTheFileAlreadyUses() throws Exception {
		// a mixed file: one element already carries the id legacy minting
		// would otherwise hand out first
		String mixed = LEGACY_TEXT.replace(" Int value 5\n",
				" Int value 5\n String sid \"legacy:0\"\n");
		Circuit circuit = load(mixed);
		Set<ElementId> seen = new HashSet<ElementId>();
		for (Element el : circuit.getElements()) {
			assertTrue(seen.add(el.getStableId()),
					"minting must skip file-declared ids, got duplicate "
							+ el.getStableId());
		}
	}

	@Test
	void malformedIdsAreClassifiedNotThrown() {
		for (String bad : new String[] {"nocounter", ":", "up:down",
				"UPPER:1", "legacy:-4", "legacy:", ":9",
				"way too spacey:1"}) {
			String text = LEGACY_TEXT.replace(" Int value 5\n",
					" Int value 5\n String sid \"" + bad + "\"\n");
			Circuit circuit = new Circuit("sids");
			assertFalse(circuit.load(new Scanner(text)),
					"sid '" + bad + "' must be refused");
			assertNotNull(JLSInfo.lastLoadError,
					"the refusal must publish a structured LoadError");
		}
	}

	@Test
	void parseIsTheInverseOfToString() {
		for (String form : new String[] {"legacy:0", "legacy:41",
				"0a1b2c3d4e5f6789:9007199254740991"}) {
			assertEquals(form, ElementId.parse(form).toString());
		}
		assertThrows(IllegalArgumentException.class,
				() -> ElementId.parse(null));
	}

	@Test
	void freshMintsAreDistinctAndOrdered() {
		ElementId a = ElementId.mintFresh();
		ElementId b = ElementId.mintFresh();
		assertNotEquals(a, b, "every fresh mint must be distinct");
		assertTrue(a.compareTo(b) != 0, "distinct ids must order apart");
		assertEquals(0, a.compareTo(ElementId.parse(a.toString())),
				"an id must equal its parsed string form");
	}
}
