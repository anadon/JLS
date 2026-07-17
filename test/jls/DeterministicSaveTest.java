package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.elem.Element;

/**
 * Deterministic canonical serialization (issue #166, collab Stage 0c):
 * a circuit's serialized form is a pure function of its content.
 * Elements are emitted sorted by stable id (#165) with the file-local
 * ids assigned in that same order, so two loads of the same file - in
 * one process or two - save byte-identically, wires and refs included.
 * Pre-change, the same fixture loaded twice saved differently in 261 of
 * 276 lines (issue #166 observation 4). Canonical bytes are the
 * convergence oracle for collaborative editing (#163), surfaced as
 * {@link Circuit#stateHash()}.
 */
class DeterministicSaveTest {

	/** The wire-and-ref-heavy fixture from the issue's probe. */
	private static final Path FORK_FIXTURE =
			Path.of("test", "fixtures", "fork-4.6-shiftregister.jls");

	private static Circuit load(String text) throws Exception {
		Circuit circuit = new Circuit("");
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

	private static String fixtureText() throws Exception {
		return Files.readString(FORK_FIXTURE, StandardCharsets.UTF_8);
	}

	@Test
	void savingTheSameInstanceTwiceIsByteIdentical() throws Exception {
		Circuit circuit = load(fixtureText());
		assertEquals(save(circuit), save(circuit),
				"saving one instance twice must be byte-identical");
	}

	@Test
	void twoLoadInstancesSaveByteIdentically() throws Exception {
		// the issue's probe (P1): pre-change, 261 of 276 lines differed
		String text = fixtureText();
		String first = save(load(text));
		String second = save(load(text));
		assertEquals(first, second,
				"two load instances of the same file must save "
						+ "byte-identically, wires and refs included");
	}

	@Test
	void saveLoadSaveIsAByteFixedPoint() throws Exception {
		// P2, on the wire-heavy fixture with no canonicalization at all
		String savedOnce = save(load(fixtureText()));
		String savedTwice = save(load(savedOnce));
		assertEquals(savedOnce, savedTwice,
				"save -> load -> save must be byte-identical");
	}

	@Test
	void savedBlocksAreSortedByStableId() throws Exception {
		Circuit circuit = load(fixtureText());
		String saved = save(circuit);
		String previous = null;
		for (String line : saved.split("\n")) {
			if (!line.startsWith(" String sid \"")) {
				continue;
			}
			String sid = line.substring(" String sid \"".length(),
					line.length() - 1);
			if (previous != null) {
				assertTrue(jls.elem.ElementId.parse(previous)
						.compareTo(jls.elem.ElementId.parse(sid)) < 0,
						"blocks must be emitted in stable-id order: '"
								+ previous + "' before '" + sid + "'");
			}
			previous = sid;
		}
		assertTrue(previous != null, "the save must contain sid lines");
	}

	@Test
	void stateHashIsContentDetermined() throws Exception {
		String text = fixtureText();
		Circuit first = load(text);
		Circuit second = load(text);
		assertEquals(first.stateHash(), second.stateHash(),
				"identical content must hash identically across instances");

		// and a real change must change the hash
		String before = first.stateHash();
		for (Element el : first.getElements()) {
			if (el instanceof jls.elem.Wire) {
				continue; // wires don't move (they follow their ends)
			}
			el.move(JLSInfo.spacing, 0);
			break;
		}
		assertNotEquals(before, first.stateHash(),
				"a moved element must change the state hash");
	}
}
