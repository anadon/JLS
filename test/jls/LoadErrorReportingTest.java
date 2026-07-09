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
 *
 * The addendum's message contract is also pinned here: every failure
 * message carries a category from the fixed taxonomy, the line and
 * element where known, and an actionable next step - not the old terse
 * internals ("no trailer", "expecting int value", "null findInLine").
 */
class LoadErrorReportingTest {

	/** Load a malformed fixture and return the reported message. */
	private static String reject(String text) {
		Circuit circuit = new Circuit("fixture");
		assertFalse(circuit.load(new Scanner(text)),
				"a malformed fixture must be rejected");
		assertTrue(JLSInfo.lastLoadError != null,
				"a failed load must publish a structured error");
		return JLSInfo.loadError;
	}

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
		assertTrue(JLSInfo.lastLoadError != null
						&& JLSInfo.lastLoadError.getCategory()
								== LoadError.Category.IO_ERROR,
				"category must be IO_ERROR");
		assertTrue(JLSInfo.loadError.contains("checkpoint")
						|| JLSInfo.loadError.contains("backup"),
				"an I/O failure must point at recovery, got: "
						+ JLSInfo.loadError);
	}

	// ---- the addendum's four-part message contract ----

	@Test
	void truncatedFileNamesCategoryLineElementAndNextStep() {
		// the file stops in the middle of a Clock element
		String error = reject("CIRCUIT c\nELEMENT Clock\n int id 0\n int x 60\n");
		assertTrue(JLSInfo.lastLoadError.getCategory()
						== LoadError.Category.MALFORMED,
				"category must be MALFORMED, got: " + error);
		assertTrue(error.contains("malformed file"),
				"the category label must be shown, got: " + error);
		assertTrue(error.contains("line " + JLSInfo.lastLoadError.getLine()),
				"the line must be shown, got: " + error);
		assertTrue(JLSInfo.lastLoadError.getLine() > 1,
				"the line must be past the header, got: "
						+ JLSInfo.lastLoadError.getLine());
		assertTrue(error.contains("Clock"),
				"the element must be named, got: " + error);
		assertTrue(error.contains("truncated"),
				"the next step must mention truncation, got: " + error);
		assertFalse(error.contains("abnormal loadElement termination"),
				"the old terse token must be gone: " + error);
	}

	@Test
	void garbageBytesAreReportedAsNotACircuitFile() {
		String error = reject("@#$ garbage bytes ~~~");
		assertTrue(JLSInfo.lastLoadError.getCategory()
						== LoadError.Category.NOT_A_CIRCUIT,
				"category must be NOT_A_CIRCUIT, got: " + error);
		assertTrue(error.contains("not a circuit file"),
				"the category label must be shown, got: " + error);
		assertTrue(error.contains("saved by JLS"),
				"the next step must say what to open instead, got: " + error);
	}

	@Test
	void badElementParameterNamesElementLineAndConstraint() {
		// Clock validates its cycle time on load (issue #52); the
		// rejection must carry element + line + the constraint itself
		String error = reject("CIRCUIT c\nELEMENT Clock\n int id 0\n"
				+ " int x 60\n int y 60\n int cycle 0\n int one 0\nEND\n"
				+ "ENDCIRCUIT\n");
		assertTrue(JLSInfo.lastLoadError.getCategory()
						== LoadError.Category.ELEMENT_ERROR,
				"category must be ELEMENT_ERROR, got: " + error);
		assertTrue(error.contains("invalid element"),
				"the category label must be shown, got: " + error);
		assertTrue(error.contains("Clock"),
				"the element type must be named, got: " + error);
		assertTrue(error.contains("line"),
				"the line must be shown, got: " + error);
		assertTrue(error.contains("Cycle time must be a positive"),
				"the violated constraint is the why, got: " + error);
		assertFalse(error.contains("Exception"),
				"no raw exception class name may appear (P6), got: "
						+ error);
	}

	@Test
	void unknownElementTagNamesTagCategoryAndUpgradeHint() {
		String error = reject(
				"CIRCUIT c\nELEMENT FluxCapacitor\nEND\nENDCIRCUIT\n");
		assertTrue(JLSInfo.lastLoadError.getCategory()
						== LoadError.Category.UNKNOWN_ELEMENT,
				"category must be UNKNOWN_ELEMENT, got: " + error);
		assertTrue(error.contains("unknown element"),
				"the category label must be shown, got: " + error);
		assertTrue(error.contains("FluxCapacitor"),
				"the unrecognized tag must be named, got: " + error);
		assertTrue(error.contains("newer version"),
				"the next step must mention a version mismatch, got: "
						+ error);
	}

	@Test
	void nonIntegerAttributeValueReportsAttributeAndLine() {
		String error = reject("CIRCUIT c\nELEMENT Clock\n int id 0\n"
				+ " int cycle banana\nEND\nENDCIRCUIT\n");
		assertTrue(JLSInfo.lastLoadError.getCategory()
						== LoadError.Category.MALFORMED,
				"category must be MALFORMED, got: " + error);
		assertTrue(error.contains("'cycle'"),
				"the attribute must be named, got: " + error);
		assertTrue(error.contains("integer"),
				"the expected kind must be named, got: " + error);
		assertTrue(error.contains("line"),
				"the line must be shown, got: " + error);
		assertFalse(error.contains("expecting int value"),
				"the old terse token must be gone: " + error);
	}
}
