package jls;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

/**
 * Load failures must report their own cause (issue #58, audit finding
 * M6): unknown element types used to leave JLSInfo.loadError stale from
 * a previous failure, and Scanner swallows IOException so a truncated
 * stream read as a clean (format) failure instead of an I/O error.
 */
class LoadErrorReportingTest {

	@Test
	void unknownElementTypeReportsItsOwnError() {
		// first, fail a load with a distinctive message
		Circuit first = new Circuit("stale");
		assertFalse(first.load(new Scanner("garbage")));
		String firstError = JLSInfo.loadError;
		assertTrue(firstError != null && !firstError.isEmpty());

		// then fail on an unknown element type: the message must be
		// about the unknown type, not the previous failure
		Circuit second = new Circuit("unknown");
		assertFalse(second.load(new Scanner(
				"CIRCUIT unknown\nELEMENT NoSuchElement\nEND\nENDCIRCUIT\n")));
		assertTrue(JLSInfo.loadError.contains("NoSuchElement"),
				"expected a message about the unknown element, got: "
						+ JLSInfo.loadError);
	}

	@Test
	void loadErrorIsResetBetweenLoads() {
		Circuit bad = new Circuit("bad");
		assertFalse(bad.load(new Scanner("garbage")));
		assertTrue(!JLSInfo.loadError.isEmpty());

		Circuit good = new Circuit("good");
		assertTrue(good.load(new Scanner("CIRCUIT good\nENDCIRCUIT\n")));
		assertTrue(JLSInfo.loadError.isEmpty(),
				"a successful load must not leave a stale error: "
						+ JLSInfo.loadError);
	}

	@Test
	void midStreamIOExceptionIsReportedAsIOError() {
		// a reader that dies after the header: Scanner presents this as
		// end of input, which must be reported as I/O, not format
		Reader broken = new Reader() {
			private final StringReader head =
					new StringReader("CIRCUIT broken\nELEMENT ");
			private int handedOut = 0;

			@Override
			public int read(char[] cbuf, int off, int len) throws IOException {
				int n = head.read(cbuf, off, len);
				if (n < 0) {
					throw new IOException("disk on fire");
				}
				handedOut += n;
				return n;
			}

			@Override
			public void close() {
			}
		};
		Circuit circuit = new Circuit("broken");
		assertFalse(circuit.load(new Scanner(broken)));
		assertTrue(JLSInfo.loadError.contains("I/O error"),
				"a mid-stream IOException must be reported as an I/O error, got: "
						+ JLSInfo.loadError);
	}
}
