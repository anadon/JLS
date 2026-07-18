package jls.hdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;

/**
 * Golden-file tests of the VHDL exporter (issue #60), the mirror of
 * {@link VerilogExportGoldenTest} over the same fixture circuits: every
 * supported element's template, plus composed combinational and
 * sequential circuits, must render byte-for-byte to the committed
 * goldens in test/resources/hdl. The JLS version in the generated
 * header is the only version-dependent text; goldens hold the token
 * {@code @VERSION@} where it appears. Every export is also run through
 * {@link VhdlStructure#assertSane} so a wrong-but-stable golden cannot
 * hide undeclared identifiers or unbalanced design units.
 *
 * <p>To regenerate after an intentional format change:
 * {@code mvn test -Dtest=VhdlExportGoldenTest
 * -Djls.hdl.regenerate=true}, then review the diff like source.</p>
 */
class VhdlExportGoldenTest {

	private static final Path GOLDEN_DIR = Path.of("test", "resources", "hdl");
	private static final String VERSION_TOKEN = "@VERSION@";

	private static void assertGolden(String goldenName, HdlCircuitBuilder cb)
			throws Exception {

		Circuit circuit = cb.load();
		HdlExporter.Result result =
				HdlExporter.export(circuit, new VhdlEmitter());
		VhdlStructure.assertSane(result.text);

		String tokenized =
				result.text.replace(JLSInfo.versionString, VERSION_TOKEN);
		Path golden = GOLDEN_DIR.resolve(goldenName + ".vhdl");
		if (Boolean.getBoolean("jls.hdl.regenerate")) {
			Files.writeString(golden, tokenized, StandardCharsets.UTF_8);
		}
		assertTrue(Files.isRegularFile(golden),
				"missing golden " + golden + " (regenerate with"
						+ " -Djls.hdl.regenerate=true and review the diff)");
		String expected = Files.readString(golden, StandardCharsets.UTF_8);
		assertEquals(expected, tokenized,
				"export of " + goldenName + " diverged from " + golden);
	} // end of assertGolden helper

	// ------------------------------------------------------------------
	// per-element templates (same fixture circuits as the Verilog suite)
	// ------------------------------------------------------------------

	/** Two-input, two-bit gate wired between pins. */
	private static HdlCircuitBuilder gateFixture(String gateType) {

		HdlCircuitBuilder cb = new HdlCircuitBuilder(
				gateType.toLowerCase(java.util.Locale.ROOT));
		int a = cb.inputPin("a", 2);
		int b = cb.inputPin("b", 2);
		int gate = cb.gate(gateType, 2, 2);
		int y = cb.outputPin("y", 2);
		cb.wire(a, "output", gate, "input0");
		cb.wire(b, "output", gate, "input1");
		cb.wire(gate, "output", y, "input");
		return cb;
	}

	@Test
	void andGateTemplate() throws Exception {
		assertGolden("gate_and", gateFixture("AndGate"));
	}

	@Test
	void orGateTemplate() throws Exception {
		assertGolden("gate_or", gateFixture("OrGate"));
	}

	@Test
	void nandGateTemplate() throws Exception {
		assertGolden("gate_nand", gateFixture("NandGate"));
	}

	@Test
	void norGateTemplate() throws Exception {
		assertGolden("gate_nor", gateFixture("NorGate"));
	}

	@Test
	void xorGateTemplate() throws Exception {
		assertGolden("gate_xor", gateFixture("XorGate"));
	}

	/** One-input gate (NOT / DELAY buffer) wired between pins. */
	private static HdlCircuitBuilder unaryFixture(String gateType,
			String name) {

		HdlCircuitBuilder cb = new HdlCircuitBuilder(name);
		int a = cb.inputPin("a", 1);
		int gate = cb.gate(gateType, 1, 1);
		int y = cb.outputPin("y", 1);
		cb.wire(a, "output", gate, "input0");
		cb.wire(gate, "output", y, "input");
		return cb;
	}

	@Test
	void notGateTemplate() throws Exception {
		assertGolden("gate_not", unaryFixture("NotGate", "notgate"));
	}

	@Test
	void delayGateTemplateIsAPlainBuffer() throws Exception {
		// propagation delay is simulation time, not structure: DELAY
		// exports as a buffer
		assertGolden("gate_delay", unaryFixture("DelayGate", "delaygate"));
	}

	@Test
	void constantTemplate() throws Exception {
		HdlCircuitBuilder cb = new HdlCircuitBuilder("constant");
		int k = cb.constant(11);
		int y = cb.outputPin("y", 4);
		cb.wire(k, "output", y, "input");
		assertGolden("constant", cb);
	}

	@Test
	void extendTemplateReplicatesTheBit() throws Exception {
		HdlCircuitBuilder cb = new HdlCircuitBuilder("extend");
		int a = cb.inputPin("a", 1);
		int ext = cb.extend(4);
		int y = cb.outputPin("y", 4);
		cb.wire(a, "output", ext, "input0");
		cb.wire(ext, "output", y, "input");
		assertGolden("extend", cb);
	}

	@Test
	void triStateTemplateDrivesZWhenOff() throws Exception {
		HdlCircuitBuilder cb = new HdlCircuitBuilder("tristate");
		int a = cb.inputPin("a", 4);
		int en = cb.inputPin("en", 1);
		int tri = cb.triState(4);
		int y = cb.outputPin("y", 4);
		cb.wire(a, "output", tri, "input");
		cb.wire(en, "output", tri, "control");
		cb.wire(tri, "output", y, "input");
		assertGolden("tristate", cb);
	}

	@Test
	void adderTemplateCarriesInAndOut() throws Exception {
		HdlCircuitBuilder cb = new HdlCircuitBuilder("adder");
		int a = cb.inputPin("a", 4);
		int b = cb.inputPin("b", 4);
		int cin = cb.inputPin("cin", 1);
		int add = cb.adder(4);
		int s = cb.outputPin("s", 4);
		int cout = cb.outputPin("cout", 1);
		cb.wire(a, "output", add, "A");
		cb.wire(b, "output", add, "B");
		cb.wire(cin, "output", add, "Cin");
		cb.wire(add, "S", s, "input");
		cb.wire(add, "Cout", cout, "input");
		assertGolden("adder", cb);
	}

	/** Register with both outputs used; the clock source varies. */
	private static HdlCircuitBuilder registerFixture(String name,
			String type, boolean clockElement) {

		HdlCircuitBuilder cb = new HdlCircuitBuilder(name);
		int d = cb.inputPin("d", 4);
		int clk = clockElement ? cb.clock(20, 10) : cb.inputPin("ck", 1);
		int reg = cb.register("r", 4, 5, type);
		int q = cb.outputPin("q", 4);
		int nq = cb.outputPin("nq", 4);
		cb.wire(d, "output", reg, "D");
		cb.wire(clk, "output", reg, "C");
		cb.wire(reg, "Q", q, "input");
		cb.wire(reg, "notQ", nq, "input");
		return cb;
	}

	@Test
	void registerPffTemplateWithClockElement() throws Exception {
		// also the Clock template: the clock becomes an entity input
		// port (testbench-friendly; see the port comment in the golden)
		assertGolden("register_pff",
				registerFixture("regpff", "pff", true));
	}

	@Test
	void registerNffTemplate() throws Exception {
		assertGolden("register_nff",
				registerFixture("regnff", "nff", false));
	}

	@Test
	void registerLatchTemplate() throws Exception {
		assertGolden("register_latch",
				registerFixture("reglatch", "latch", false));
	}

	/** Two-way, two-bit mux wired between pins. */
	private static HdlCircuitBuilder muxFixture() {

		HdlCircuitBuilder cb = new HdlCircuitBuilder("mux");
		int a = cb.inputPin("a", 2);
		int b = cb.inputPin("b", 2);
		int sel = cb.inputPin("sel", 1);
		int mux = cb.mux(2, 2);
		int y = cb.outputPin("y", 2);
		cb.wire(a, "output", mux, "input0");
		cb.wire(b, "output", mux, "input1");
		cb.wire(sel, "output", mux, "select");
		cb.wire(mux, "output", y, "input");
		return cb;
	}

	/**
	 * Three-way, one-bit mux: the two-bit selector has an out-of-range
	 * value (3), which JLS reads as 0 - the exported others choice.
	 */
	private static HdlCircuitBuilder mux3Fixture() {

		HdlCircuitBuilder cb = new HdlCircuitBuilder("mux3");
		int a = cb.inputPin("a", 1);
		int b = cb.inputPin("b", 1);
		int c = cb.inputPin("c", 1);
		int sel = cb.inputPin("sel", 2);
		int mux = cb.mux(3, 1);
		int y = cb.outputPin("y", 1);
		cb.wire(a, "output", mux, "input0");
		cb.wire(b, "output", mux, "input1");
		cb.wire(c, "output", mux, "input2");
		cb.wire(sel, "output", mux, "select");
		cb.wire(mux, "output", y, "input");
		return cb;
	}

	/** Two-to-four decoder wired between pins. */
	private static HdlCircuitBuilder decoderFixture() {

		HdlCircuitBuilder cb = new HdlCircuitBuilder("decoder");
		int d = cb.inputPin("d", 2);
		int dec = cb.decoder(2);
		int y = cb.outputPin("y", 4);
		cb.wire(d, "output", dec, "input");
		cb.wire(dec, "output", y, "input");
		return cb;
	}

	@Test
	void muxTemplateIsAWithSelect() throws Exception {
		assertGolden("mux", muxFixture());
	}

	@Test
	void muxOutOfRangeSelectReadsZeroInTheOthersChoice() throws Exception {
		assertGolden("mux3", mux3Fixture());
	}

	@Test
	void decoderTemplateIsOneHot() throws Exception {
		assertGolden("decoder", decoderFixture());
	}

	@Test
	void binderAndSplitterTemplates() throws Exception {
		HdlCircuitBuilder cb = new HdlCircuitBuilder("bundles");
		int hi = cb.inputPin("hi", 2);
		int lo = cb.inputPin("lo", 2);
		int bind = cb.binder(4, new int[][] { {2, 3}, {0, 1} });
		int split = cb.splitter(4, new int[][] { {3}, {0, 1, 2} });
		int top = cb.outputPin("top", 1);
		int rest = cb.outputPin("rest", 3);
		cb.wire(hi, "output", bind, "3-2");
		cb.wire(lo, "output", bind, "1-0");
		cb.wire(bind, "output", split, "input");
		cb.wire(split, "3", top, "input");
		cb.wire(split, "2-0", rest, "input");
		assertGolden("bundles", cb);
	}

	// ------------------------------------------------------------------
	// composed circuits
	// ------------------------------------------------------------------

	@Test
	void composedCombinationalCircuit() throws Exception {
		// y = (a and b) or (not c)
		HdlCircuitBuilder cb = new HdlCircuitBuilder("comb");
		int a = cb.inputPin("a", 1);
		int b = cb.inputPin("b", 1);
		int c = cb.inputPin("c", 1);
		int and = cb.gate("AndGate", 1, 2);
		int not = cb.gate("NotGate", 1, 1);
		int or = cb.gate("OrGate", 1, 2);
		int y = cb.outputPin("y", 1);
		cb.wire(a, "output", and, "input0");
		cb.wire(b, "output", and, "input1");
		cb.wire(c, "output", not, "input0");
		cb.wire(and, "output", or, "input0");
		cb.wire(not, "output", or, "input1");
		cb.wire(or, "output", y, "input");
		assertGolden("comb", cb);
	}

	@Test
	void composedRegisterCircuitIsACounter() throws Exception {
		// count <= count + 1 on every clock edge, q watches count
		HdlCircuitBuilder cb = new HdlCircuitBuilder("counter");
		int clk = cb.clock(20, 10);
		int one = cb.constant(1);
		int add = cb.adder(4);
		int reg = cb.register("count", 4, 0, "pff");
		int q = cb.outputPin("q", 4);
		cb.wire(clk, "output", reg, "C");
		cb.wire(one, "output", add, "B");
		cb.wire(add, "S", reg, "D");
		cb.wire3(reg, "Q", add, "A", q, "input");
		assertGolden("counter", cb);
	}

	@Test
	void jumpAliasedNetsExportAsOneNamedNet() throws Exception {
		// gate -> JumpStart "mid" ... JumpEnd "mid" -> gate: the two
		// wire nets are one electrical net and must export as ONE
		// VHDL signal carrying the jump's name (see also HdlPolicyTest)
		HdlCircuitBuilder cb = new HdlCircuitBuilder("aliased");
		int a = cb.inputPin("a", 1);
		int n1 = cb.gate("NotGate", 1, 1);
		int js = cb.jumpStart("mid", 1);
		int je = cb.jumpEnd("mid", 1);
		int n2 = cb.gate("NotGate", 1, 1);
		int y = cb.outputPin("y", 1);
		cb.wire(a, "output", n1, "input0");
		cb.wire(n1, "output", js, "input");
		cb.wire(je, "output", n2, "input0");
		cb.wire(n2, "output", y, "input");
		assertGolden("jump_alias", cb);
	}

	@Test
	void verilogKeywordNamesPassThroughUnmangled() throws Exception {
		// a pin named "wire" is a Verilog keyword (the shared walker
		// renames it "wire_") but a perfectly good VHDL identifier: the
		// VHDL pass strips the trailing underscore back off, and no
		// rename table appears because the user's name survived intact
		HdlCircuitBuilder cb = new HdlCircuitBuilder("keyword");
		int a = cb.inputPin("wire", 1);
		int y = cb.outputPin("y", 1);
		cb.wire(a, "output", y, "input");
		assertGolden("keyword", cb);
	}

	@Test
	void vhdlKeywordNamesAreLegalizedAndDocumented() throws Exception {
		// "in" is fine in Verilog but reserved in VHDL; the VHDL pass
		// must rename it (deterministically, "_v" suffix) and the
		// header must document the mapping
		HdlCircuitBuilder cb = new HdlCircuitBuilder("keyword_vhdl");
		int a = cb.inputPin("in", 1);
		int y = cb.outputPin("y", 1);
		cb.wire(a, "output", y, "input");
		assertGolden("keyword_vhdl", cb);
	}

} // end of VhdlExportGoldenTest class
