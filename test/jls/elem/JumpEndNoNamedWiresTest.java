package jls.elem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.HeadlessException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;

/**
 * Fail-fast guard on the wire-name END gesture (issue #131): placing a
 * JumpEnd when the circuit has no named wires must report an error and
 * never open the selection dialog, because the dialog's name list would
 * be empty and impossible to complete.
 *
 * <p>These tests run headless (surefire sets java.awt.headless=true), so
 * "the dialog opens" is observable as a HeadlessException from the
 * JDialog constructor, and "no dialog, one message" is observable as a
 * false return plus the TellUser stderr line. P2 of the issue (the
 * dialog itself being byte-for-byte unchanged with one or more named
 * wires) is GUI-interactive and pinned only up to the dialog-construction
 * boundary here.</p>
 */
class JumpEndNoNamedWiresTest {

	/**
	 * A circuit containing a single named wire "js" (and nothing else),
	 * in the on-disk text format, loaded headless.
	 */
	private static Circuit circuitWithNamedWire() throws Exception {
		String text = "CIRCUIT named\n"
				+ "ELEMENT JumpStart\n int id 0\n int x 180\n int y 60\n"
				+ " String name \"js\"\n int bits 1\n int watch 0\n"
				+ " String orientation \"LEFT\"\nEND\n"
				+ "ENDCIRCUIT\n";
		Circuit circuit = new Circuit("");
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	@Test
	void endGestureFailsFastWhenNoNamedWiresExist() {
		Circuit circuit = new Circuit("empty");
		JumpEnd end = new JumpEnd(circuit);

		PrintStream oldErr = System.err;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		boolean ok;
		try {
			System.setErr(new PrintStream(captured, true,
					StandardCharsets.UTF_8));
			// pre-fix this throws HeadlessException: the empty selection
			// dialog is constructed anyway
			ok = end.setup(null, null, 100, 100);
		} finally {
			System.setErr(oldErr);
		}
		assertFalse(ok, "setup must report the gesture as not completed");

		// assert a stable fragment, not the full phrasing: the message
		// wording is #81's to evolve, the guard is this issue's substance
		String err = captured.toString(StandardCharsets.UTF_8);
		assertTrue(err.contains("jls: error: ")
				&& err.contains("No named wires"),
				"one clear message must explain why, got: " + err);

		// pure pre-condition: no state change (nothing registered or added)
		assertTrue(circuit.getElements().isEmpty(),
				"the circuit must be untouched");
	}

	@Test
	void endGestureStillReachesDialogWithANamedWire() throws Exception {
		Circuit circuit = circuitWithNamedWire();
		JumpEnd end = new JumpEnd(circuit);
		// with a named wire present the guard must not fire: the gesture
		// proceeds to dialog construction, which headless surfaces as
		// HeadlessException from the JDialog constructor
		assertThrows(HeadlessException.class,
				() -> end.setup(null, null, 100, 100),
				"the selection dialog must still be offered");
	}

	@Test
	void matchGesturePresetNameBypassesGuardAndDialog() throws Exception {
		// the editor's "match" gesture (SimpleEditor matchJump) presets the
		// name before calling setup; that path never opens the dialog and
		// must complete unchanged
		Circuit circuit = circuitWithNamedWire();
		JumpEnd end = new JumpEnd(circuit);
		end.setName("js");
		assertTrue(end.setup(null, null, 120, 120),
				"a preset name must still place without any dialog");
	}
}
