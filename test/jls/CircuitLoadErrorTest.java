package jls;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Scanner;

import org.junit.jupiter.api.Test;

/**
 * Circuit.load error-path tests (issue #14): malformed input must be
 * rejected with a JLSInfo.loadError message that identifies the problem,
 * never accepted or crashed on.
 */
class CircuitLoadErrorTest {

	private static String rejectAndGetError(String text) {
		JLSInfo.loadError = "";
		Circuit circuit = new Circuit("bad");
		assertFalse(circuit.load(new Scanner(text)), "malformed circuit must be rejected");
		return JLSInfo.loadError;
	}

	@Test
	void emptyInputIsRejected() {
		assertTrue(rejectAndGetError("").contains("no header"),
				"got: " + JLSInfo.loadError);
	}

	@Test
	void wrongHeaderIsRejected() {
		assertTrue(rejectAndGetError("NOTACIRCUIT foo\n").contains("CIRCUIT"),
				"got: " + JLSInfo.loadError);
	}

	@Test
	void missingTrailerIsRejected() {
		String error = rejectAndGetError("CIRCUIT foo\n");
		assertTrue(error.contains("no trailer"), "got: " + error);
	}

	@Test
	void junkInsteadOfElementIsRejected() {
		String error = rejectAndGetError("CIRCUIT foo\nJUNK\nENDCIRCUIT\n");
		assertTrue(error.contains("ELEMENT"), "got: " + error);
	}

	@Test
	void unknownElementTypeIsRejected() {
		Circuit circuit = new Circuit("bad");
		assertFalse(circuit.load(new Scanner(
						"CIRCUIT foo\nELEMENT NoSuchElement\nEND\nENDCIRCUIT\n")),
				"unknown element type must be rejected");
	}
}
