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
		assertTrue(error.contains("ENDCIRCUIT"), "got: " + error);
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

	/**
	 * Two different wire ends attaching to the same put is a malformed net:
	 * a connection point takes a single wire net, and fan-out is several
	 * wires leaving one end. Such a file used to load silently with only the
	 * last attachment honored and the other net left dead (read as undriven),
	 * so the circuit simulated subtly wrong with no diagnostic. It must now be
	 * rejected during assembly (issue #58).
	 */
	@Test
	void duplicateWireEndAttachmentIsRejected() throws Exception {
		String text = "CIRCUIT dup\n"
				+ "ELEMENT Constant\n int id 0\n Int value 1\n int base 10\n"
				+ " String orient \"RIGHT\"\nEND\n"
				+ "ELEMENT OutputPin\n int id 1\n String name \"a\"\n int bits 1\n"
				+ " int watch 1\n String orient \"RIGHT\"\nEND\n"
				+ "ELEMENT OutputPin\n int id 2\n String name \"b\"\n int bits 1\n"
				+ " int watch 1\n String orient \"RIGHT\"\nEND\n"
				+ "ELEMENT WireEnd\n int id 3\n String put \"output\"\n"
				+ " ref attach 0\n ref wire 4\nEND\n"
				+ "ELEMENT WireEnd\n int id 4\n String put \"input\"\n"
				+ " ref attach 1\n ref wire 3\nEND\n"
				+ "ELEMENT WireEnd\n int id 5\n String put \"output\"\n"
				+ " ref attach 0\n ref wire 6\nEND\n"
				+ "ELEMENT WireEnd\n int id 6\n String put \"input\"\n"
				+ " ref attach 2\n ref wire 5\nEND\n"
				+ "ENDCIRCUIT\n";
		JLSInfo.loadError = "";
		Circuit circuit = new Circuit("dup");
		assertTrue(circuit.load(new Scanner(text)),
				"the tokens parse; the fault is at assembly time");
		assertFalse(circuit.finishLoad(null),
				"two wire ends on one put must fail assembly");
		assertTrue(JLSInfo.loadError.contains("more than one wire end"),
				"got: " + JLSInfo.loadError);
	}
}
