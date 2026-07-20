package jls.hdl.imp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;
import jls.elem.Element;
import jls.elem.Input;
import jls.elem.LogicElement;
import jls.hdl.yosys.YosysNetlist;

/**
 * End-to-end coverage for the Stage 2 cell-to-element mapper and
 * save-format emitter (issue #61), driven entirely by committed JSON
 * fixtures so the suite runs with no external tools (Yosys is exercised
 * separately by the CI import leg). Each fixture is parsed, imported,
 * and the emitted save text is loaded back through JLS's <em>real</em>
 * loader: a fixture that imports must produce a circuit that loads,
 * re-saves as a byte fixed point (proving it is a canonical, hand-drawn
 * -equivalent circuit, issue #62), and has every input fully wired.
 * Reject fixtures must raise {@link ImportException} with a teachable
 * message and emit nothing.
 */
class NetlistImporterTest {

	/** Where the committed netlist fixtures live. */
	private static final Path FIXTURES =
			Path.of("test", "resources", "hdl", "import");

	/**
	 * Parses one fixture and imports it.
	 *
	 * @param name The fixture file name.
	 *
	 * @return the import result.
	 *
	 * @throws Exception if reading, parsing, or importing fails.
	 */
	private static ImportResult imp(String name) throws Exception {
		String json = Files.readString(FIXTURES.resolve(name));
		YosysNetlist netlist = YosysNetlist.parse(json);
		return NetlistImporter.importNetlist(netlist);
	}

	/**
	 * Loads emitted save text through the real loader.
	 *
	 * @param text The JLS save-format text.
	 *
	 * @return the loaded, finished circuit.
	 *
	 * @throws Exception if load or finishLoad fails.
	 */
	private static Circuit load(String text) throws Exception {
		Circuit circuit = new Circuit("imported");
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	/**
	 * Re-saves a circuit through the canonical writer.
	 *
	 * @param circuit The circuit.
	 *
	 * @return the save text.
	 */
	private static String save(Circuit circuit) {
		StringWriter out = new StringWriter();
		try (PrintWriter writer = new PrintWriter(out)) {
			circuit.save(writer);
		}
		return out.toString();
	}

	/** Counts loaded elements by simple class name. */
	private static Map<String, Integer> census(Circuit circuit) {
		Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
		for (Element el : circuit.getElements()) {
			String kind = el.getClass().getSimpleName();
			counts.merge(kind, 1, Integer::sum);
		}
		return counts;
	}

	/** Asserts every input put of every logic element is wired. */
	private static void assertFullyWired(Circuit circuit) {
		for (Element el : circuit.getElements()) {
			if (!(el instanceof LogicElement)) {
				continue;
			}
			LogicElement logic = (LogicElement) el;
			for (Input in : logic.getInputList()) {
				assertTrue(in.isAttached(), () -> el.getClass()
						.getSimpleName() + " input \"" + in.getName()
						+ "\" was left unwired by the importer");
			}
		}
	}

	/** A loaded circuit re-saves as a byte fixed point. */
	private static void assertReloadFixedPoint(String emitted)
			throws Exception {
		Circuit first = load(emitted);
		String once = save(first);
		Circuit second = load(once);
		assertEquals(once, save(second),
				"imported circuit must re-save as a fixed point");
	}

	@Test
	void singleAndGateImportsWiresAndReloads() throws Exception {
		ImportResult result = imp("and2.json");
		Circuit circuit = load(result.saveText());

		Map<String, Integer> counts = census(circuit);
		assertEquals(2, counts.getOrDefault("InputPin", 0),
				"two module inputs -> two InputPins");
		assertEquals(1, counts.getOrDefault("OutputPin", 0),
				"one module output -> one OutputPin");
		assertEquals(1, counts.getOrDefault("AndGate", 0),
				"one $and -> one AndGate");
		assertTrue(counts.getOrDefault("Wire", 0) >= 3,
				"three nets -> at least three wires, got " + counts);

		assertFullyWired(circuit);
		assertReloadFixedPoint(result.saveText());

		// the AndGate output and the OutputPin input share one net
		LogicElement gate = find(circuit, "AndGate");
		LogicElement out = find(circuit, "OutputPin");
		assertSame(gate.getOutputList().get(0).getWireEnd().getNet(),
				out.getInputList().get(0).getWireEnd().getNet(),
				"gate output and output pin must be the same net");
	}

	@Test
	void multiBitAoiChainWithFanoutImports() throws Exception {
		ImportResult result = imp("aoi.json");
		Circuit circuit = load(result.saveText());

		Map<String, Integer> counts = census(circuit);
		assertEquals(3, counts.getOrDefault("InputPin", 0));
		assertEquals(2, counts.getOrDefault("OutputPin", 0));
		assertEquals(1, counts.getOrDefault("AndGate", 0));
		assertEquals(1, counts.getOrDefault("OrGate", 0));
		assertEquals(1, counts.getOrDefault("NotGate", 0));

		assertFullyWired(circuit);
		assertReloadFixedPoint(result.saveText());

		// the $and output fans out to both the OR and the "p" output pin:
		// one net, two sinks - assert the AndGate output net is shared by
		// the OrGate's first input
		LogicElement and = find(circuit, "AndGate");
		LogicElement or = find(circuit, "OrGate");
		boolean andFeedsOr = false;
		for (Input in : or.getInputList()) {
			if (in.getWireEnd().getNet()
					== and.getOutputList().get(0).getWireEnd().getNet()) {
				andFeedsOr = true;
			}
		}
		assertTrue(andFeedsOr, "AND output must feed an OR input (fanout)");
	}

	@Test
	void muxImportsWithSelectAndData() throws Exception {
		ImportResult result = imp("mux2.json");
		Circuit circuit = load(result.saveText());

		Map<String, Integer> counts = census(circuit);
		assertEquals(3, counts.getOrDefault("InputPin", 0),
				"a, b, s -> three InputPins");
		assertEquals(1, counts.getOrDefault("OutputPin", 0));
		assertEquals(1, counts.getOrDefault("Mux", 0));

		assertFullyWired(circuit);
		assertReloadFixedPoint(result.saveText());
	}

	@Test
	void constantDrivenInputGetsAConstantElement() throws Exception {
		ImportResult result = imp("const_and.json");
		Circuit circuit = load(result.saveText());

		Map<String, Integer> counts = census(circuit);
		assertEquals(1, counts.getOrDefault("Constant", 0),
				"the 1'b1 operand becomes a Constant, got " + counts);
		assertEquals(1, counts.getOrDefault("AndGate", 0));
		assertFullyWired(circuit);
		assertReloadFixedPoint(result.saveText());

		assertEquals(1, result.summary().mapping().getOrDefault("constant", 0));
	}

	@Test
	void summaryReportsMappingCounts() throws Exception {
		ImportResult result = imp("aoi.json");
		ImportSummary summary = result.summary();
		assertEquals("aoi", summary.moduleName());
		assertEquals(5, summary.mapping().getOrDefault("port", 0),
				"3 inputs + 2 outputs = 5 ports");
		assertEquals(1, summary.mapping().getOrDefault("$and", 0));
		assertEquals(1, summary.mapping().getOrDefault("$or", 0));
		assertEquals(1, summary.mapping().getOrDefault("$not", 0));
		assertEquals(0, summary.coercedX());
	}

	@Test
	void unrealizedButValidCellIsRejectedNotMismapped() {
		ImportException ex = assertThrows(ImportException.class,
				() -> imp("reject_dff.json"));
		assertTrue(ex.getMessage().contains("$dff"),
				"message must name the cell: " + ex.getMessage());
		assertTrue(ex.getMessage().contains("not yet realize"),
				"message must explain it is a not-yet-built cell: "
						+ ex.getMessage());
	}

	@Test
	void teachableRejectRelaysValidatorMessage() {
		ImportException ex = assertThrows(ImportException.class,
				() -> imp("reject_adff.json"));
		assertTrue(ex.getMessage().contains("asynchronous reset"),
				"async-reset teaching message must survive: "
						+ ex.getMessage());
	}

	@Test
	void bitSliceWithoutWholeDriverIsRejected() {
		ImportException ex = assertThrows(ImportException.class,
				() -> imp("reject_slice.json"));
		assertTrue(ex.getMessage().contains("slice")
						|| ex.getMessage().contains("Splitter"),
				"slice reject must mention the mesh limitation: "
						+ ex.getMessage());
	}

	/** Finds the one element of a given simple class name. */
	private static LogicElement find(Circuit circuit, String kind) {
		for (Element el : circuit.getElements()) {
			if (el.getClass().getSimpleName().equals(kind)) {
				return (LogicElement) el;
			}
		}
		assertNotNull(null, "no " + kind + " in circuit");
		return null;
	}

} // end of NetlistImporterTest class
