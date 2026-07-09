package jls.hdl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The VHDL emitter's own policies (issue #60), pinned at the unit
 * level: the second legalization pass that VHDL identifiers need on
 * top of the shared walker's Verilog-oriented one (underscore
 * placement, reserved words, case-insensitive collisions), the header
 * documentation of every composed rename, and the VHDL-specific
 * template choices (non-chained nand/nor, 'Z' tri-state). Everything
 * here goes through the same {@link HdlExporter} walk as Verilog, so
 * element policy (skip/reject/warn) needs no re-testing - HdlPolicyTest
 * already pins it language-independently.
 */
class VhdlEmitterPolicyTest {

	private static String export(HdlCircuitBuilder cb) throws Exception {

		HdlExporter.Result result =
				HdlExporter.export(cb.load(), new VhdlEmitter());
		VhdlStructure.assertSane(result.text);
		return result.text;
	}

	@Test
	void vhdlReservedWordsAreSuffixedAndDocumented() throws Exception {
		// "signal" passes the Verilog pass untouched but is reserved in
		// VHDL; case-insensitively so
		HdlCircuitBuilder cb = new HdlCircuitBuilder("res");
		int a = cb.inputPin("signal", 1);
		int b = cb.inputPin("Entity", 1);
		int g = cb.gate("AndGate", 1, 2);
		int y = cb.outputPin("y", 1);
		cb.wire(a, "output", g, "input0");
		cb.wire(b, "output", g, "input1");
		cb.wire(g, "output", y, "input");

		String text = export(cb);
		assertTrue(text.contains("signal_v : in std_logic"), text);
		assertTrue(text.contains("Entity_v : in std_logic"), text);
		assertTrue(text.contains("\"signal\" -> signal_v"), text);
		assertTrue(text.contains("\"Entity\" -> Entity_v"), text);
	}

	@Test
	void underscorePlacementIsRepairedAndCollisionsUniquified()
			throws Exception {
		// "x!" legalizes to "x_" for Verilog; VHDL forbids the trailing
		// underscore, so it becomes "x" - which now collides with the
		// real pin "x" and must be uniquified deterministically
		HdlCircuitBuilder cb = new HdlCircuitBuilder("und");
		int a = cb.inputPin("x", 1);
		int b = cb.inputPin("x!", 1);
		int g = cb.gate("OrGate", 1, 2);
		int y = cb.outputPin("y", 1);
		cb.wire(a, "output", g, "input0");
		cb.wire(b, "output", g, "input1");
		cb.wire(g, "output", y, "input");

		String text = export(cb);
		assertTrue(text.contains("x : in std_logic"), text);
		assertTrue(text.contains("x_2 : in std_logic"), text);
		assertTrue(text.contains("\"x!\" -> x_2"), text);
		assertFalse(text.contains("x_ :"),
				"a trailing underscore is not a legal VHDL identifier:\n"
						+ text);
	}

	@Test
	void caseInsensitiveCollisionsAreUniquified() throws Exception {
		// VHDL identifiers that differ only in case are the same name
		HdlCircuitBuilder cb = new HdlCircuitBuilder("cases");
		int a = cb.inputPin("data", 1);
		int b = cb.inputPin("DATA", 1);
		int g = cb.gate("XorGate", 1, 2);
		int y = cb.outputPin("y", 1);
		cb.wire(a, "output", g, "input0");
		cb.wire(b, "output", g, "input1");
		cb.wire(g, "output", y, "input");

		String text = export(cb);
		// ports are claimed in sorted order, so "DATA" wins the name
		// and "data" is uniquified
		assertTrue(text.contains("DATA : in std_logic"), text);
		assertTrue(text.contains("data_2 : in std_logic"), text);
		assertTrue(text.contains("\"data\" -> data_2"), text);
	}

	@Test
	void nandAndNorDoNotChain() throws Exception {
		// "a nand b nand c" is illegal VHDL; the n-input form must be
		// not (a and b and c)
		HdlCircuitBuilder cb = new HdlCircuitBuilder("wide");
		int a = cb.inputPin("a", 1);
		int b = cb.inputPin("b", 1);
		int c = cb.inputPin("c", 1);
		int g = cb.gate("NandGate", 1, 3);
		int y = cb.outputPin("y", 1);
		cb.wire(a, "output", g, "input0");
		cb.wire(b, "output", g, "input1");
		cb.wire(c, "output", g, "input2");
		cb.wire(g, "output", y, "input");

		String text = export(cb);
		assertTrue(text.contains("not (a and b and c)"), text);
		assertFalse(text.contains(" nand "), text);
	}

	@Test
	void triStateDrivesCapitalZ() throws Exception {
		HdlCircuitBuilder cb = new HdlCircuitBuilder("tz");
		int a = cb.inputPin("a", 1);
		int en = cb.inputPin("en", 1);
		int tri = cb.triState(1);
		int y = cb.outputPin("y", 1);
		cb.wire(a, "output", tri, "input");
		cb.wire(en, "output", tri, "control");
		cb.wire(tri, "output", y, "input");

		String text = export(cb);
		assertTrue(text.contains("a when en = '1' else 'Z';"), text);
	}

	@Test
	void verilogAndVhdlShareOneModelWalk() throws Exception {
		// the language-neutral model is built once per export by the
		// same code path: both emitters must agree on the (Verilog-pass)
		// net names and on the warning set
		HdlCircuitBuilder cb = new HdlCircuitBuilder("shared");
		int a = cb.inputPin("a", 1);
		int display = cb.display(1);
		int y = cb.outputPin("y", 1);
		cb.wire3(a, "output", display, "input0", y, "input");

		HdlExporter.Result verilog =
				HdlExporter.export(cb.load(), new VerilogEmitter());
		HdlExporter.Result vhdl =
				HdlExporter.export(cb.load(), new VhdlEmitter());
		org.junit.jupiter.api.Assertions.assertEquals(verilog.warnings,
				vhdl.warnings,
				"one walk, one policy: warnings must match across languages");
	}

} // end of VhdlEmitterPolicyTest class
