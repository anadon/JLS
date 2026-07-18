package jls.hdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The exporter's element policy (issue #60), pinned: simulation
 * control and annotation elements are skipped with a warning;
 * inexpressible elements reject the whole export with one message
 * naming every offender; and the jump-alias net walk fuses same-named
 * nets into one HDL net.
 */
class HdlPolicyTest {

	@Test
	void displayIsSkippedWithAWarning() throws Exception {
		HdlCircuitBuilder cb = new HdlCircuitBuilder("policy");
		int a = cb.inputPin("a", 1);
		int display = cb.display(1);
		int out = cb.outputPin("y", 1);
		cb.wire3(a, "output", display, "input0", out, "input");

		HdlExporter.Result result =
				HdlExporter.export(cb.load(), new VerilogEmitter());
		assertEquals(1, result.warnings.size(), "one skip warning expected");
		assertTrue(result.warnings.get(0).contains("Display"),
				result.warnings.get(0));
		assertTrue(result.warnings.get(0).contains("skipped"),
				result.warnings.get(0));
		assertFalse(result.text.contains("Display"),
				"a skipped element must leave no trace in the output");
		// the rest of the circuit still exports
		assertTrue(result.text.contains("assign y = a;"), result.text);
	}

	@Test
	void simulationControlElementsAreAllSkipped() throws Exception {
		HdlCircuitBuilder cb = new HdlCircuitBuilder("controls");
		int a = cb.inputPin("a", 1);
		cb.sigGen(); // no puts: it drives pins by name at sim time
		int stop = cb.stop();
		int out = cb.outputPin("y", 1);
		cb.wire3(a, "output", stop, "input0", out, "input");

		HdlExporter.Result result =
				HdlExporter.export(cb.load(), new VerilogEmitter());
		List<String> kinds = new ArrayList<String>();
		for (String warning : result.warnings) {
			kinds.add(warning.split(" ")[0]);
		}
		assertEquals(List.of("SigGen", "Stop"), kinds,
				"both control elements skip, in element order");
	}

	@Test
	void memoryIsRejectedByName() throws Exception {
		HdlCircuitBuilder cb = new HdlCircuitBuilder("hasmem");
		cb.memory("scratch", 8, 16);

		HdlExportException rejection = assertThrows(HdlExportException.class,
				() -> HdlExporter.export(cb.load(), new VerilogEmitter()));
		assertTrue(rejection.getMessage().contains("Memory \"scratch\""),
				rejection.getMessage());
		assertTrue(rejection.getMessage().contains("at ("),
				"the rejection must locate the element: "
						+ rejection.getMessage());
	}

	@Test
	void subCircuitIsRejectedCleanly() throws Exception {
		HdlCircuitBuilder cb = new HdlCircuitBuilder("hassub");
		cb.subCircuit("sub1");

		HdlExportException rejection = assertThrows(HdlExportException.class,
				() -> HdlExporter.export(cb.load(), new VerilogEmitter()));
		assertTrue(rejection.getMessage().contains("SubCircuit"),
				rejection.getMessage());
	}

	@Test
	void rejectionListsEveryOffenderInOneMessage() throws Exception {
		// the issue's user-surface spec: export fails before writing
		// anything and lists EVERY offending element, not just the first
		HdlCircuitBuilder cb = new HdlCircuitBuilder("bad");
		cb.memory("mem", 8, 16);
		cb.subCircuit("sub1");

		HdlExportException rejection = assertThrows(HdlExportException.class,
				() -> HdlExporter.export(cb.load(), new VerilogEmitter()));
		assertTrue(rejection.getMessage().contains("Memory"),
				rejection.getMessage());
		assertTrue(rejection.getMessage().contains("SubCircuit"),
				rejection.getMessage());
	}

	@Test
	void muxWithUnattachedSelectPassesInputZeroThrough() throws Exception {
		// JLS reads an unattached select as 0, so the mux degenerates
		// to a buffer of input0 instead of a constant-selector case
		HdlCircuitBuilder cb = new HdlCircuitBuilder("muxnosel");
		int a = cb.inputPin("a", 1);
		int b = cb.inputPin("b", 1);
		int mux = cb.mux(2, 1);
		int y = cb.outputPin("y", 1);
		cb.wire(a, "output", mux, "input0");
		cb.wire(b, "output", mux, "input1");
		cb.wire(mux, "output", y, "input");

		HdlExporter.Result result =
				HdlExporter.export(cb.load(), new VerilogEmitter());
		VerilogStructure.assertSane(result.text);
		assertTrue(result.text.contains("select unattached, reads 0"),
				result.text);
		assertTrue(result.text.contains("assign net_2 = a;"), result.text);
		assertTrue(result.text.contains("assign y = net_2;"), result.text);
		assertFalse(result.text.contains("case"),
				"a folded mux must not emit a case: " + result.text);
	}

	@Test
	void decoderWithUnattachedInputConstantlyDrivesBitZero()
			throws Exception {
		// JLS reads an unattached decoder input as 0, so exactly output
		// bit 0 is set - a constant driver, not a case
		HdlCircuitBuilder cb = new HdlCircuitBuilder("decnoin");
		int dec = cb.decoder(1);
		int y = cb.outputPin("y", 2);
		cb.wire(dec, "output", y, "input");

		HdlExporter.Result result =
				HdlExporter.export(cb.load(), new VerilogEmitter());
		VerilogStructure.assertSane(result.text);
		assertTrue(result.text.contains("input unattached, reads 0"),
				result.text);
		assertTrue(result.text.contains("assign net_0 = 2'h1;"),
				result.text);
		assertTrue(result.text.contains("assign y = net_0;"), result.text);
	}

	@Test
	void twoNetsBridgedByJumpsBecomeOneVerilogNet() throws Exception {
		// three separate wire nets - source->JumpStart, JumpEnd->sink1,
		// second JumpEnd->sink2 - all alias "mid" and must fuse into
		// ONE declared net feeding both sinks
		HdlCircuitBuilder cb = new HdlCircuitBuilder("fanout");
		int a = cb.inputPin("a", 1);
		int n1 = cb.gate("NotGate", 1, 1);
		int js = cb.jumpStart("mid", 1);
		int je1 = cb.jumpEnd("mid", 1);
		int je2 = cb.jumpEnd("mid", 1);
		int y1 = cb.outputPin("y1", 1);
		int y2 = cb.outputPin("y2", 1);
		cb.wire(a, "output", n1, "input0");
		cb.wire(n1, "output", js, "input");
		cb.wire(je1, "output", y1, "input");
		cb.wire(je2, "output", y2, "input");

		HdlModel model = HdlExporter.buildModel(cb.load());
		assertEquals(1, model.nets().size(),
				"the three jump-aliased wire nets must fuse into one");
		assertEquals("mid", model.nets().get(0).name,
				"the fused net carries the jump name");

		String text = new VerilogEmitter().emit(model);
		VerilogStructure.assertSane(text);
		assertTrue(text.contains("assign mid = ~a;"), text);
		assertTrue(text.contains("assign y1 = mid;"), text);
		assertTrue(text.contains("assign y2 = mid;"), text);
	}

	@Test
	void unconnectedOutputPinWarnsAndLeavesThePortUndriven()
			throws Exception {
		HdlCircuitBuilder cb = new HdlCircuitBuilder("floating");
		cb.inputPin("a", 1);
		cb.outputPin("y", 1);

		HdlExporter.Result result =
				HdlExporter.export(cb.load(), new VerilogEmitter());
		assertEquals(1, result.warnings.size());
		assertTrue(result.warnings.get(0).contains("OutputPin \"y\""),
				result.warnings.get(0));
		assertFalse(result.text.contains("assign y"), result.text);
	}

} // end of HdlPolicyTest class
