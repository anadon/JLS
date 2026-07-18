package jls.hdl.scan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link VhdlEntityScanner} (issue #63). Beyond the
 * inline cases, the committed VHDL export goldens under
 * {@code test/resources/hdl/} double as a free corpus: every golden is
 * real, ghdl-analyzed VHDL (see GhdlCompileTest) that the scanner must
 * read back, closing the loop between JLS's own emitter and its own
 * scanner. Out-of-subset constructs must fail with a one-line reason,
 * routing such files to external extraction instead of growing the
 * scanner into a parser (issue #63 &#167;9).
 */
class VhdlEntityScannerTest {

	/**
	 * Asserts one port's full shape.
	 * @param port the scanned port
	 * @param name the expected name
	 * @param direction the expected direction
	 * @param bits the expected width
	 */
	private static void assertPort(ScannedPort port, String name,
			ScannedPort.Direction direction, int bits) {
		assertEquals(name, port.name, "port name");
		assertEquals(direction, port.direction,
				"direction of " + name);
		assertEquals(bits, port.bits, "width of " + name);
	} // end of assertPort method

	@Test
	void adderGoldenScansToItsExactPorts() throws Exception {
		String source = Files.readString(Path.of("test", "resources",
				"hdl", "adder.vhdl"));
		List<ScannedModule> entities = VhdlEntityScanner.scan(source);
		assertEquals(1, entities.size());
		ScannedModule m = entities.get(0);
		assertEquals("adder", m.name);
		assertEquals(5, m.ports().size());
		assertPort(m.ports().get(0), "a", ScannedPort.Direction.IN, 4);
		assertPort(m.ports().get(1), "b", ScannedPort.Direction.IN, 4);
		assertPort(m.ports().get(2), "cin",
				ScannedPort.Direction.IN, 1);
		assertPort(m.ports().get(3), "cout",
				ScannedPort.Direction.OUT, 1);
		assertPort(m.ports().get(4), "s",
				ScannedPort.Direction.OUT, 4);
	} // end of adderGoldenScansToItsExactPorts method

	@Test
	void everyCommittedGoldenScansToOneEntityWithPorts()
			throws Exception {
		List<Path> goldens = new ArrayList<Path>();
		try (DirectoryStream<Path> dir = Files.newDirectoryStream(
				Path.of("test", "resources", "hdl"), "*.vhdl")) {
			dir.forEach(goldens::add);
		}
		Collections.sort(goldens);
		assertEquals(false, goldens.isEmpty(), "no goldens found");
		for (Path golden : goldens) {
			List<ScannedModule> entities =
					VhdlEntityScanner.scan(Files.readString(golden));
			assertEquals(1, entities.size(),
					golden + " should declare exactly one entity");
			assertTrue(entities.get(0).ports().size() > 0,
					golden + " should have ports");
		}
	} // end of everyCommittedGoldenScansToOneEntityWithPorts method

	@Test
	void genericsResolvePortWidthsInBothRangeDirections()
			throws Exception {
		String source = Files.readString(Path.of("test", "resources",
				"hdl", "scan", "generic_regfile.vhdl"));
		List<ScannedModule> entities = VhdlEntityScanner.scan(source);
		assertEquals(1, entities.size());
		ScannedModule m = entities.get(0);
		assertEquals("generic_regfile", m.name);
		assertEquals(Long.valueOf(16), m.parameters().get("WIDTH"));
		assertEquals(Long.valueOf(8), m.parameters().get("DEPTH"));
		assertEquals(Long.valueOf(3), m.parameters().get("ABITS"));
		assertEquals(8, m.ports().size());
		assertPort(m.ports().get(0), "clk",
				ScannedPort.Direction.IN, 1);
		assertPort(m.ports().get(1), "we",
				ScannedPort.Direction.IN, 1);
		assertPort(m.ports().get(2), "waddr",
				ScannedPort.Direction.IN, 3);
		assertPort(m.ports().get(3), "wdata",
				ScannedPort.Direction.IN, 16);
		assertPort(m.ports().get(4), "raddr",
				ScannedPort.Direction.IN, 3);
		assertPort(m.ports().get(5), "rdata",
				ScannedPort.Direction.OUT, 16);
		assertPort(m.ports().get(6), "status",
				ScannedPort.Direction.OUT, 1);
		assertPort(m.ports().get(7), "scan_io",
				ScannedPort.Direction.INOUT, 8);
	} // end of genericsResolvePortWidthsInBothRangeDirections method

	@Test
	void modeDefaultsToInWhenOmitted() throws Exception {
		ScannedModule m = VhdlEntityScanner.scan(
				"entity e is port (a : std_logic);"
						+ " end entity e;").get(0);
		assertPort(m.ports().get(0), "a", ScannedPort.Direction.IN, 1);
	} // end of modeDefaultsToInWhenOmitted method

	@Test
	void keywordsAndGenericReferencesAreCaseInsensitive()
			throws Exception {
		ScannedModule m = VhdlEntityScanner.scan(
				"ENTITY shouty IS\n"
						+ "GENERIC (Width : INTEGER := 12);\n"
						+ "PORT (d : IN STD_LOGIC_VECTOR"
						+ "(WIDTH - 1 DOWNTO 0));\n"
						+ "END ENTITY shouty;\n").get(0);
		assertEquals("shouty", m.name);
		assertPort(m.ports().get(0), "d",
				ScannedPort.Direction.IN, 12);
	} // end of keywordsAndGenericReferencesAreCaseInsensitive method

	@Test
	void sharedDeclarationYieldsOnePortPerName() throws Exception {
		ScannedModule m = VhdlEntityScanner.scan(
				"entity e is port (a, b : in bit_vector(3 downto 0));"
						+ " end e;").get(0);
		assertEquals(2, m.ports().size());
		assertPort(m.ports().get(0), "a", ScannedPort.Direction.IN, 4);
		assertPort(m.ports().get(1), "b", ScannedPort.Direction.IN, 4);
	} // end of sharedDeclarationYieldsOnePortPerName method

	@Test
	void defaultValuesAfterTheTypeAreIgnored() throws Exception {
		ScannedModule m = VhdlEntityScanner.scan(
				"entity e is port (en : in std_logic := '0');"
						+ " end e;").get(0);
		assertPort(m.ports().get(0), "en",
				ScannedPort.Direction.IN, 1);
	} // end of defaultValuesAfterTheTypeAreIgnored method

	@Test
	void directEntityInstantiationIsNotADeclaration() throws Exception {
		List<ScannedModule> entities = VhdlEntityScanner.scan(
				"entity top is port (x : in std_logic); end top;\n"
						+ "architecture a of top is begin\n"
						+ "u1: entity work.child port map (p => x);\n"
						+ "end architecture a;\n");
		assertEquals(1, entities.size());
		assertEquals("top", entities.get(0).name);
	} // end of directEntityInstantiationIsNotADeclaration method

	@Test
	void vhdl2008BlockCommentsAreStripped() throws Exception {
		ScannedModule m = VhdlEntityScanner.scan(
				"entity e is /* block\ncomment */ port"
						+ " (q : out std_logic); end e;").get(0);
		assertPort(m.ports().get(0), "q",
				ScannedPort.Direction.OUT, 1);
	} // end of vhdl2008BlockCommentsAreStripped method

	@Test
	void recordPortTypesAreRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VhdlEntityScanner.scan(
						"entity e is port (r : in my_bus_t);"
								+ " end e;"));
		assertTrue(e.getMessage().contains("my_bus_t"),
				e.getMessage());
	} // end of recordPortTypesAreRejected method

	@Test
	void unconstrainedVectorsAreRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VhdlEntityScanner.scan(
						"entity e is port"
								+ " (d : in std_logic_vector);"
								+ " end e;"));
		assertTrue(e.getMessage().contains("unconstrained"),
				e.getMessage());
	} // end of unconstrainedVectorsAreRejected method

	@Test
	void nullRangesAreRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VhdlEntityScanner.scan(
						"entity e is port"
								+ " (d : in std_logic_vector"
								+ "(0 downto 3)); end e;"));
		assertTrue(e.getMessage().contains("unreasonable"),
				e.getMessage());
	} // end of nullRangesAreRejected method

	@Test
	void linkagePortsAreRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VhdlEntityScanner.scan(
						"entity e is port (l : linkage std_logic);"
								+ " end e;"));
		assertTrue(e.getMessage().contains("linkage"),
				e.getMessage());
	} // end of linkagePortsAreRejected method

	@Test
	void widthNeedingAGenericWithoutDefaultIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VhdlEntityScanner.scan(
						"entity e is generic (W : integer);\n"
								+ "port (d : in std_logic_vector"
								+ "(W-1 downto 0)); end e;"));
		assertTrue(e.getMessage().contains("W"), e.getMessage());
	} // end of widthNeedingAGenericWithoutDefaultIsRejected method

	@Test
	void missingEndIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VhdlEntityScanner.scan(
						"entity e is port (a : in std_logic);"));
		assertTrue(e.getMessage().contains("missing end"),
				e.getMessage());
	} // end of missingEndIsRejected method

} // end of VhdlEntityScannerTest class
