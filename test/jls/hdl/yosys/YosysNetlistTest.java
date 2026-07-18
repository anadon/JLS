package jls.hdl.yosys;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

/**
 * The typed netlist model over {@code write_json} output (issue #61):
 * a realistic two-bit-counter netlist decodes into modules, ports,
 * cells, connections and net names; both numeric parameter encodings
 * (JSON number and fixed-width binary string) decode; constant bits
 * become the sentinels; and schema violations are reported with the
 * module/cell/port path.
 */
class YosysNetlistTest {

	/** Fixtures read better with single quotes; swap them for real ones. */
	private static String json(String singleQuoted) {
		return singleQuoted.replace('\'', '"');
	}

	/** A hand-checked netlist shaped like real Yosys counter output. */
	private static final String COUNTER = json(
			"{ 'creator': 'Yosys 0.38+92 (git sha1 deadbee)',"
			+ " 'modules': {"
			+ "  'counter': {"
			+ "   'attributes': { 'top': 1, 'src': 'counter.v:1.1-12.10' },"
			+ "   'ports': {"
			+ "    'clk': { 'direction': 'input', 'bits': [2] },"
			+ "    'q': { 'direction': 'output', 'bits': [3, 4] }"
			+ "   },"
			+ "   'memories': { },"
			+ "   'cells': {"
			+ "    '$procdff$8': {"
			+ "     'hide_name': 1,"
			+ "     'type': '$dff',"
			+ "     'parameters': { 'CLK_POLARITY': '1', 'WIDTH': 2 },"
			+ "     'attributes': { 'src': 'counter.v:8.3-10.6' },"
			+ "     'port_directions':"
			+ "      { 'CLK': 'input', 'D': 'input', 'Q': 'output' },"
			+ "     'connections':"
			+ "      { 'CLK': [2], 'D': [5, 6], 'Q': [3, 4] }"
			+ "    },"
			+ "    '$add$counter.v:9$2': {"
			+ "     'hide_name': 1,"
			+ "     'type': '$add',"
			+ "     'parameters': {"
			+ "      'A_SIGNED': 0, 'A_WIDTH': 2, 'B_SIGNED': 0,"
			+ "      'B_WIDTH': '00000000000000000000000000000001',"
			+ "      'Y_WIDTH': 2, 'NAME': 'adder '"
			+ "     },"
			+ "     'attributes': { },"
			+ "     'port_directions':"
			+ "      { 'A': 'input', 'B': 'input', 'Y': 'output' },"
			+ "     'connections':"
			+ "      { 'A': [3, 4], 'B': ['1'], 'Y': [5, 6] }"
			+ "    }"
			+ "   },"
			+ "   'netnames': {"
			+ "    'q': { 'hide_name': 0, 'bits': [3, 4],"
			+ "     'attributes': { 'src': 'counter.v:3.16-3.17' } },"
			+ "    '$junk': { 'hide_name': 1, 'bits': ['x', 'z', 7] }"
			+ "   }"
			+ "  }"
			+ " }"
			+ "}");

	@Test
	void decodesTheWholeModel() throws Exception {
		YosysNetlist netlist = YosysNetlist.parse(COUNTER);
		assertEquals("Yosys 0.38+92 (git sha1 deadbee)",
				netlist.creator());
		assertEquals(List.of("counter"),
				new ArrayList<String>(netlist.modules().keySet()));

		YosysNetlist.Module counter =
				netlist.modules().get("counter");
		assertEquals("1", counter.attributes().get("top"),
				"number attributes canonicalize to decimal strings");
		assertEquals("counter.v:1.1-12.10",
				counter.attributes().get("src"));

		YosysNetlist.Port clk = counter.ports().get("clk");
		assertEquals(YosysNetlist.PortDirection.INPUT,
				clk.direction());
		assertArrayEquals(new int[] { 2 }, clk.bits());
		assertEquals(YosysNetlist.PortDirection.OUTPUT,
				counter.ports().get("q").direction());

		YosysNetlist.Cell dff = counter.cells().get("$procdff$8");
		assertEquals("$dff", dff.type());
		assertTrue(dff.hideName());
		assertEquals("counter.v:8.3-10.6", dff.sourceLocation());
		assertEquals(YosysNetlist.PortDirection.OUTPUT,
				dff.portDirections().get("Q"));
		assertArrayEquals(new int[] { 5, 6 },
				dff.connections().get("D"));

		YosysNetlist.NetName q = counter.netNames().get("q");
		assertArrayEquals(new int[] { 3, 4 }, q.bits());
	}

	@Test
	void decodesBothParameterEncodings() throws Exception {
		YosysNetlist.Cell add = YosysNetlist.parse(COUNTER)
				.modules().get("counter").cells()
				.get("$add$counter.v:9$2");
		assertEquals(2, add.param("A_WIDTH"),
				"JSON-number parameter");
		assertEquals(1, add.param("B_WIDTH"),
				"32-digit binary-string parameter");
		assertEquals(1, add.param("CLK_POLARITY", 1),
				"absent parameter takes the default");
		YosysNetlist.Cell dff = YosysNetlist.parse(COUNTER)
				.modules().get("counter").cells().get("$procdff$8");
		assertEquals(1, dff.param("CLK_POLARITY"),
				"short binary-string parameter");
	}

	@Test
	void constantBitsBecomeSentinels() throws Exception {
		YosysNetlist.Module counter = YosysNetlist.parse(COUNTER)
				.modules().get("counter");
		assertArrayEquals(new int[] { YosysNetlist.BIT_1 },
				counter.cells().get("$add$counter.v:9$2")
						.connections().get("B"));
		assertArrayEquals(new int[] { YosysNetlist.BIT_X,
				YosysNetlist.BIT_Z, 7 },
				counter.netNames().get("$junk").bits());
	}

	@Test
	void missingParameterIsANamedError() throws Exception {
		YosysNetlist.Cell dff = YosysNetlist.parse(COUNTER)
				.modules().get("counter").cells().get("$procdff$8");
		NetlistFormatException e = assertThrows(
				NetlistFormatException.class,
				() -> dff.param("NO_SUCH"));
		assertTrue(e.getMessage().contains("NO_SUCH"),
				e.getMessage());
	}

	@Test
	void stringParameterIsNotNumeric() throws Exception {
		// write_json marks string parameters with a trailing space
		YosysNetlist.Cell add = YosysNetlist.parse(COUNTER)
				.modules().get("counter").cells()
				.get("$add$counter.v:9$2");
		NetlistFormatException e = assertThrows(
				NetlistFormatException.class,
				() -> add.param("NAME"));
		assertTrue(e.getMessage().contains("string parameter"),
				e.getMessage());
	}

	@Test
	void missingModulesObjectIsRejected() {
		NetlistFormatException e = assertThrows(
				NetlistFormatException.class,
				() -> YosysNetlist.parse("{}"));
		assertTrue(e.getMessage().contains("modules"),
				e.getMessage());
	}

	@Test
	void badDirectionNamesThePort() {
		String bad = json("{ 'modules': { 'm': { 'ports': {"
				+ " 'p': { 'direction': 'sideways', 'bits': [] }"
				+ " } } } }");
		NetlistFormatException e = assertThrows(
				NetlistFormatException.class,
				() -> YosysNetlist.parse(bad));
		assertTrue(e.getMessage().contains("module \"m\""),
				e.getMessage());
		assertTrue(e.getMessage().contains("port \"p\""),
				e.getMessage());
		assertTrue(e.getMessage().contains("sideways"),
				e.getMessage());
	}

	@Test
	void badBitEntryNamesTheLocation() {
		String bad = json("{ 'modules': { 'm': { 'ports': {"
				+ " 'p': { 'direction': 'input', 'bits': ['q'] }"
				+ " } } } }");
		NetlistFormatException e = assertThrows(
				NetlistFormatException.class,
				() -> YosysNetlist.parse(bad));
		assertTrue(e.getMessage().contains("bit 0"), e.getMessage());
		assertTrue(e.getMessage().contains("\"q\""), e.getMessage());
	}

	@Test
	void cellWithoutTypeIsRejected() {
		String bad = json("{ 'modules': { 'm': { 'cells': {"
				+ " 'c': { 'connections': { } }"
				+ " } } } }");
		NetlistFormatException e = assertThrows(
				NetlistFormatException.class,
				() -> YosysNetlist.parse(bad));
		assertTrue(e.getMessage().contains("cell \"c\""),
				e.getMessage());
		assertTrue(e.getMessage().contains("type"), e.getMessage());
	}

} // end of YosysNetlistTest class
