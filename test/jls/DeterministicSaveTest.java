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
	void canonicalBytesAreIdenticalWhateverThePlatformNewline()
			throws Exception {
		// #111 failure 1: println follows the platform line separator,
		// so unwrapped saves would differ between Windows and Linux.
		// Simulate a \r\n platform by overriding println() exactly the
		// way PrintWriter specializes it per platform; the save path
		// must never let it reach the output.
		Circuit circuit = load(fixtureText());
		StringWriter plain = new StringWriter();
		try (PrintWriter writer = new PrintWriter(plain)) {
			circuit.save(writer);
		}
		StringWriter crlf = new StringWriter();
		try (PrintWriter writer = new PrintWriter(crlf) {
			@Override
			public void println() {
				write("\r\n");
			}
		}) {
			circuit.save(writer);
		}
		assertEquals(plain.toString(), crlf.toString(),
				"canonical bytes must not depend on the platform line "
						+ "separator");
		assertTrue(plain.toString().indexOf('\r') < 0,
				"canonical saves must use bare \\n line endings");
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

	// ---------------------------------------------------------------
	// State machines (issue #180): StateMachine.save and State.save
	// used to iterate HashSets whose members override no hashCode, so
	// state and transition blocks were emitted in identity-hash order -
	// per-run-variable bytes for the same machine. The fixture has
	// three states declared out of name order, each with a conditional
	// and an "else" transition declared out of canonical order, so any
	// regression to set-order iteration shows up as a byte difference
	// or a non-canonical block sequence.
	// ---------------------------------------------------------------

	/** A circuit whose one element is a three-state machine. */
	private static String stateMachineFixtureText() {
		StringBuilder text = new StringBuilder("CIRCUIT smdet\n")
				.append("ELEMENT StateMachine\n")
				.append(" int id 0\n int x 240\n int y 360\n")
				.append(" int width 96\n int height 96\n")
				.append(" String name \"sm0\"\n int delay 5\n int trig 1\n");
		// declared SB, SA, SC - not name order - to expose ordering
		appendState(text, "SB", 2, "SA", 140);
		appendState(text, "SA", 3, "SC", 40);
		appendState(text, "SC", 1, "SB", 240);
		return text.append("END\nENDCIRCUIT\n").toString();
	}

	/** One state: output z, a conditional transition, an else self-loop. */
	private static void appendState(StringBuilder text, String name,
			long zValue, String next, int x) {
		text.append(" String state \"").append(name).append("\"\n")
				.append("  int x ").append(x).append("\n  int y 40\n")
				.append("  int diameter 40\n  int init ")
				.append(name.equals("SA") ? 1 : 0).append('\n')
				.append("  String output \"z\"\n   long value ").append(zValue)
				.append("\n   int bits 2\n")
				// conditional before else: canonical save order is the
				// reverse (unconditional, else, then conditionals)
				.append("  String trans \"en\"\n   int eq 0\n   int value 1\n")
				.append("   int bits 1\n   String next \"").append(next)
				.append("\"\n")
				.append("  String trans \"else\"\n   String next \"")
				.append(name).append("\"\n");
	}

	@Test
	void twoLoadsOfAStateMachineSaveByteIdentically() throws Exception {
		String text = stateMachineFixtureText();
		assertEquals(save(load(text)), save(load(text)),
				"two load instances of the same state machine must save "
						+ "byte-identically, states and transitions included");
	}

	@Test
	void stateMachineSaveLoadSaveIsAByteFixedPoint() throws Exception {
		String savedOnce = save(load(stateMachineFixtureText()));
		String savedTwice = save(load(savedOnce));
		assertEquals(savedOnce, savedTwice,
				"save -> load -> save of a state machine must be "
						+ "byte-identical");
	}

	@Test
	void stateMachineSavesStatesInNameOrder() throws Exception {
		String saved = save(load(stateMachineFixtureText()));
		int sa = saved.indexOf(" String state \"SA\"");
		int sb = saved.indexOf(" String state \"SB\"");
		int sc = saved.indexOf(" String state \"SC\"");
		assertTrue(sa >= 0 && sb >= 0 && sc >= 0,
				"all three states must be saved");
		assertTrue(sa < sb && sb < sc,
				"state blocks must be saved in name order, got offsets SA="
						+ sa + " SB=" + sb + " SC=" + sc);

		// within each state block, "else" precedes the conditional
		int[] starts = {sa, sb, sc};
		int[] ends = {sb, sc, saved.length()};
		for (int i = 0; i < starts.length; i++) {
			String block = saved.substring(starts[i], ends[i]);
			int elseAt = block.indexOf("String trans \"else\"");
			int condAt = block.indexOf("String trans \"en\"");
			assertTrue(elseAt >= 0 && condAt >= 0,
					"each state must save both transitions");
			assertTrue(elseAt < condAt,
					"canonical transition order is else before "
							+ "conditionals, violated in block " + i);
		}
	}

	@Test
	void stateMachineStateHashIsContentDetermined() throws Exception {
		String text = stateMachineFixtureText();
		assertEquals(load(text).stateHash(), load(text).stateHash(),
				"identical state-machine content must hash identically "
						+ "across instances");
	}
}
