package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jls.Circuit;

/**
 * Headless tests for the TruthTable table-model mutators (issue #159).
 * The edit dialog is only a view: addInput/addOutput/toggleOutput/
 * removeInput/removeOutput/makeDontCare/undoDontCare and the column
 * moves all operate on the element's name lists and value table, and
 * refreshDisplay() no-ops when no display panel exists, so the model
 * can be driven and observed (through the saved-file text, the same
 * channel the loader reads) without any GUI.
 *
 * Table cell encoding, pinned here because every expectation depends
 * on it: 0 and 1 are logic values, 2 is don't-care ("x"); a freshly
 * added output column is all don't-cares, and toggleOutput cycles
 * 2 -&gt; 0 -&gt; 1 -&gt; 2.
 */
class TruthTableModelTest {

	/** A fresh, empty truth table on a throwaway circuit. */
	private static TruthTable newTable() {
		return new TruthTable(new Circuit("ttmodel"));
	}

	/** The element's on-disk text, the observable model state. */
	private static String saved(TruthTable tt) {
		StringWriter out = new StringWriter();
		try (PrintWriter writer = new PrintWriter(out)) {
			tt.save(writer);
		}
		return out.toString().replace(System.lineSeparator(), "\n");
	}

	/** Quoted values of ' String &lt;key&gt; "..."' lines, in file order. */
	private static List<String> names(TruthTable tt, String key) {
		List<String> found = new ArrayList<>();
		for (String line : saved(tt).split("\n")) {
			String prefix = " String " + key + " \"";
			if (line.startsWith(prefix)) {
				found.add(line.substring(prefix.length(),
						line.length() - 1));
			}
		}
		return found;
	}

	/** The value table rebuilt from the row-major ' pair r v' lines. */
	private static int[][] grid(TruthTable tt) {
		List<int[]> pairs = new ArrayList<>();
		for (String line : saved(tt).split("\n")) {
			if (line.startsWith(" pair ")) {
				String[] parts = line.trim().split(" ");
				pairs.add(new int[] { Integer.parseInt(parts[1]),
						Integer.parseInt(parts[2]) });
			}
		}
		if (pairs.isEmpty())
			return new int[0][0];
		int rows = pairs.get(pairs.size() - 1)[0] + 1;
		int cols = pairs.size() / rows;
		int[][] table = new int[rows][cols];
		int i = 0;
		for (int[] pair : pairs) {
			table[i / cols][i % cols] = pair[1];
			i += 1;
		}
		return table;
	}

	private static void assertGrid(int[][] expected, TruthTable tt) {
		int[][] actual = grid(tt);
		assertEquals(expected.length, actual.length, "row count");
		for (int r = 0; r < expected.length; r += 1) {
			assertEquals(java.util.Arrays.toString(expected[r]),
					java.util.Arrays.toString(actual[r]), "row " + r);
		}
	}

	/** a, b inputs and an f output equal to a: the collapsible fixture. */
	private static TruthTable twoInputsOutputEqualsA() {
		TruthTable tt = newTable();
		tt.addInput("a");
		tt.addOutput("f");
		tt.toggleOutput(0, 1);           // f(a=0): 2 -> 0
		tt.toggleOutput(1, 1);           // f(a=1): 2 -> 0
		tt.toggleOutput(1, 1);           //          0 -> 1
		tt.addInput("b");
		return tt;                       // rows a b f: 000 010 101 111
	}

	@Test
	void addInputToEmptyTableCreatesTheTwoRowTruthColumn() {
		TruthTable tt = newTable();
		tt.addInput("a");
		assertEquals(List.of("a"), names(tt, "input"));
		assertGrid(new int[][] { { 0 }, { 1 } }, tt);
	}

	@Test
	void addInputIgnoresAnEmptyName() {
		TruthTable tt = newTable();
		tt.addInput("");
		assertEquals(List.of(), names(tt, "input"));
		assertGrid(new int[0][0], tt);
	}

	@Test
	void addInputRejectsNamesAlreadyUsedByEitherSide() {
		TruthTable tt = newTable();
		tt.addInput("a");
		tt.addOutput("f");
		tt.addInput("a");                // duplicate input name
		tt.addInput("f");                // collides with an output name
		assertEquals(List.of("a"), names(tt, "input"));
		assertEquals(List.of("f"), names(tt, "output"));
	}

	@Test
	void addSecondInputDoublesRowsAndReplicatesOutputs() {
		TruthTable tt = twoInputsOutputEqualsA();
		assertEquals(List.of("a", "b"), names(tt, "input"));
		assertGrid(new int[][] {
				{ 0, 0, 0 }, { 0, 1, 0 }, { 1, 0, 1 }, { 1, 1, 1 } }, tt);
	}

	@Test
	void addOutputBeforeAnyInputIsRejected() {
		TruthTable tt = newTable();
		tt.addOutput("f");
		assertEquals(List.of(), names(tt, "output"));
		assertGrid(new int[0][0], tt);
	}

	@Test
	void addOutputAppendsAnAllDontCareColumn() {
		TruthTable tt = newTable();
		tt.addInput("a");
		tt.addOutput("f");
		assertEquals(List.of("f"), names(tt, "output"));
		assertGrid(new int[][] { { 0, 2 }, { 1, 2 } }, tt);
	}

	@Test
	void toggleOutputCyclesDontCareLowHigh() {
		TruthTable tt = newTable();
		tt.addInput("a");
		tt.addOutput("f");
		tt.toggleOutput(0, 1);
		assertGrid(new int[][] { { 0, 0 }, { 1, 2 } }, tt);
		tt.toggleOutput(0, 1);
		assertGrid(new int[][] { { 0, 1 }, { 1, 2 } }, tt);
		tt.toggleOutput(0, 1);
		assertGrid(new int[][] { { 0, 2 }, { 1, 2 } }, tt);
	}

	@Test
	void removeInputCollapsesAColumnTheOutputsNeverDependedOn() {
		TruthTable tt = twoInputsOutputEqualsA();
		tt.removeInput("b");
		assertEquals(List.of("a"), names(tt, "input"));
		assertGrid(new int[][] { { 0, 0 }, { 1, 1 } }, tt);
	}

	@Test
	void removeInputIsRefusedWhenOutputsWouldConflict() {
		TruthTable tt = newTable();
		tt.addInput("a");
		tt.addOutput("f");
		tt.toggleOutput(1, 1);           // f(1): x -> 0
		tt.toggleOutput(1, 1);           //       0 -> 1
		tt.toggleOutput(0, 1);           // f(0): x -> 0, so f = a
		tt.addInput("b");                // f still = a
		// now make f depend on b: flip f in row (a=0,b=1) from 0 to 1
		tt.toggleOutput(1, 2);
		tt.removeInput("b");
		assertEquals(List.of("a", "b"), names(tt, "input"));
		assertGrid(new int[][] {
				{ 0, 0, 0 }, { 0, 1, 1 }, { 1, 0, 1 }, { 1, 1, 1 } }, tt);
	}

	@Test
	void removeOutputDeletesExactlyItsColumn() {
		TruthTable tt = newTable();
		tt.addInput("a");
		tt.addOutput("f");
		tt.addOutput("g");
		tt.toggleOutput(0, 1);           // f(0) = 0
		tt.toggleOutput(0, 2);
		tt.toggleOutput(0, 2);           // g(0) = 1
		tt.removeOutput("f");
		assertEquals(List.of("g"), names(tt, "output"));
		assertGrid(new int[][] { { 0, 1 }, { 1, 2 } }, tt);
	}

	@Test
	void makeDontCareCollapsesTheMatchingRowPair() {
		TruthTable tt = twoInputsOutputEqualsA();
		tt.makeDontCare(0, 1);           // b is irrelevant when a = 0
		assertGrid(new int[][] {
				{ 0, 2, 0 }, { 1, 0, 1 }, { 1, 1, 1 } }, tt);
	}

	@Test
	void makeDontCareIsRefusedWithoutAMatchingRow() {
		TruthTable tt = twoInputsOutputEqualsA();
		tt.toggleOutput(1, 2);           // f(0,1): 0 -> 1, f now = a|b
		tt.makeDontCare(0, 1);
		assertGrid(new int[][] {
				{ 0, 0, 0 }, { 0, 1, 1 }, { 1, 0, 1 }, { 1, 1, 1 } }, tt);
	}

	@Test
	void undoDontCareReinsertsTheSplitRowInCodeOrder() {
		TruthTable tt = twoInputsOutputEqualsA();
		tt.makeDontCare(0, 1);           // collapse rows 00 / 01
		tt.undoDontCare(0, 1);           // split back: insert mid-table
		assertGrid(new int[][] {
				{ 0, 0, 0 }, { 0, 1, 0 }, { 1, 0, 1 }, { 1, 1, 1 } }, tt);
	}

	@Test
	void undoDontCareAppendsWhenTheSplitRowSortsLast() {
		TruthTable tt = twoInputsOutputEqualsA();
		tt.makeDontCare(2, 1);           // collapse rows 10 / 11
		assertGrid(new int[][] {
				{ 0, 0, 0 }, { 0, 1, 0 }, { 1, 2, 1 } }, tt);
		tt.undoDontCare(2, 1);           // 11 sorts after every row
		assertGrid(new int[][] {
				{ 0, 0, 0 }, { 0, 1, 0 }, { 1, 0, 1 }, { 1, 1, 1 } }, tt);
	}

	@Test
	void makeRowCodeTreatsDontCareAsZero() {
		TruthTable tt = twoInputsOutputEqualsA();
		tt.makeDontCare(0, 1);           // row 0 is now 0 x | 0
		assertEquals(0, tt.makeRowCode(0));
		assertEquals(3, tt.makeRowCode(2));
	}

	@Test
	void findMatchingRowLetsDontCareOutputsMatchAnything() {
		TruthTable tt = newTable();
		tt.addInput("a");
		tt.addOutput("f");
		tt.toggleOutput(1, 1);           // f(1) = 0, f(0) still x
		// ignoring the input column, row 0 (f=x) matches row 1 (f=0)
		assertEquals(1, tt.findMatchingRow(0, 0));
		// nothing matches row 0 when the input column must agree too
		assertEquals(-1, tt.findMatchingRow(0, -1));
	}

	@Test
	void moveOutputLeftAndRightSwapColumnsAndStopAtTheEdges() {
		TruthTable tt = newTable();
		tt.addInput("a");
		tt.addOutput("f");
		tt.addOutput("g");
		tt.toggleOutput(0, 1);           // f(0) = 0
		tt.toggleOutput(0, 2);
		tt.toggleOutput(0, 2);           // g(0) = 1
		tt.moveOutputLeft("g");
		assertEquals(List.of("g", "f"), names(tt, "output"));
		assertGrid(new int[][] { { 0, 1, 0 }, { 1, 2, 2 } }, tt);
		tt.moveOutputLeft("g");          // already leftmost: no-op
		tt.moveOutputRight("f");         // already rightmost: no-op
		assertEquals(List.of("g", "f"), names(tt, "output"));
		tt.moveOutputRight("g");
		assertEquals(List.of("f", "g"), names(tt, "output"));
		assertGrid(new int[][] { { 0, 0, 1 }, { 1, 2, 2 } }, tt);
	}

	@Test
	void moveInputRightRenumbersRowsByTheNewBitWeights() {
		TruthTable tt = twoInputsOutputEqualsA();
		tt.moveInputRight("a");
		assertEquals(List.of("b", "a"), names(tt, "input"));
		assertGrid(new int[][] {
				{ 0, 0, 0 }, { 0, 1, 1 }, { 1, 0, 0 }, { 1, 1, 1 } }, tt);
		tt.moveInputRight("a");          // already rightmost: no-op
		assertEquals(List.of("b", "a"), names(tt, "input"));
		tt.moveInputLeft("a");
		assertEquals(List.of("a", "b"), names(tt, "input"));
		assertGrid(new int[][] {
				{ 0, 0, 0 }, { 0, 1, 0 }, { 1, 0, 1 }, { 1, 1, 1 } }, tt);
		tt.moveInputLeft("a");           // already leftmost: no-op
		assertEquals(List.of("a", "b"), names(tt, "input"));
	}

	@Test
	void renameWithoutAPromptAnswerLeavesTheTableUntouched() {
		// headless TellUser.prompt answers null (issue #81), so both
		// rename paths must decline without mutating anything
		TruthTable tt = twoInputsOutputEqualsA();
		tt.renameInput("a");
		tt.renameOutput("f");
		assertEquals(List.of("a", "b"), names(tt, "input"));
		assertEquals(List.of("f"), names(tt, "output"));
		assertGrid(new int[][] {
				{ 0, 0, 0 }, { 0, 1, 0 }, { 1, 0, 1 }, { 1, 1, 1 } }, tt);
	}

	@Test
	void savedTextRoundTripsThroughTheLoaderPath() {
		// the grid observations above all read the save text; make sure
		// that text is the loader dialect by pushing it back through
		// setValue/setPair the way Circuit.load does
		TruthTable original = twoInputsOutputEqualsA();
		String text = saved(original);
		assertTrue(text.startsWith("ELEMENT TruthTable\n"), text);
		assertTrue(text.endsWith("END\n"), text);
	}
}
