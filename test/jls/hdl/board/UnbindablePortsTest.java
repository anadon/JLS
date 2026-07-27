package jls.hdl.board;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jls.hdl.HdlExportException;
import jls.hdl.HdlExporter;
import jls.hdl.HdlModel;

/**
 * The fail-clean side of board-aware export (issue #213, prediction
 * P3): a design whose ports cannot all be bound must fail with a
 * specific, actionable message — and because {@link PcfEmitter#emit}
 * throws instead of returning, a partial or invalid constraint file
 * can never exist. Every binding problem in one attempt is reported in
 * one exception, mirroring the exporter's all-offenders-at-once
 * rejection style.
 */
class UnbindablePortsTest {

	/** The blinky model (ports sw[1:0], clk, led[1:0]). */
	private HdlModel model;
	/** The iCEstick board. */
	private Board board;

	@BeforeEach
	void setUp() throws Exception {
		model = HdlExporter.buildModel(BoardFixtures.blinky());
		Board icestick = Boards.byName("icestick");
		assertNotNull(icestick);
		board = icestick;
	}

	/** Emit with the given binding lines, expecting a failure. */
	private String failToEmit(String... lines) throws Exception {
		PinBindings bindings = PinBindings.parse(List.of(lines));
		HdlExportException e = assertThrows(HdlExportException.class,
				() -> PcfEmitter.emit(model, board, bindings));
		return e.getMessage();
	}

	@Test
	void aMissingBindingNamesThePortAndTheRepair() throws Exception {

		String message = failToEmit(
				"sw[0] PMOD1", "sw[1] PMOD2", "clk CLK", "led[0] LED1");
		assertTrue(message.contains("\"led[1]\""), message);
		assertTrue(message.contains("led[1] <pin>"),
				"the message must show the exact line to add: " + message);
	}

	@Test
	void anUnknownPinNameListsTheAvailablePins() throws Exception {

		String message = failToEmit(
				"sw[0] PMOD1", "sw[1] PMOD2", "clk CLOCK",
				"led[0] LED1", "led[1] LED2");
		assertTrue(message.contains("\"CLOCK\""), message);
		assertTrue(message.contains("available pins"), message);
		assertTrue(message.contains("CLK"),
				"the repair (the real pin name) must be visible: "
						+ message);
	}

	@Test
	void aBindingForANonexistentPortListsTheRealPorts()
			throws Exception {

		String message = failToEmit(
				"sw[0] PMOD1", "sw[1] PMOD2", "clk CLK",
				"led[0] LED1", "led[1] LED2", "btn PMOD3");
		assertTrue(message.contains("\"btn\""), message);
		assertTrue(message.contains("the ports are"), message);
		assertTrue(message.contains("led (2 bits, output)"), message);
	}

	@Test
	void anIndexedBindingOnAOneBitPortSaysSo() throws Exception {

		String message = failToEmit(
				"sw[0] PMOD1", "sw[1] PMOD2", "clk[0] CLK",
				"led[0] LED1", "led[1] LED2");
		assertTrue(message.contains("\"clk[0]\""), message);
		assertTrue(message.contains("1 bit wide"), message);
		assertTrue(message.contains("without an index"), message);
	}

	@Test
	void anUnindexedBindingOnAWidePortSaysToBindEachBit()
			throws Exception {

		String message = failToEmit(
				"sw PMOD1", "clk CLK", "led[0] LED1", "led[1] LED2");
		assertTrue(message.contains("2 bits wide"), message);
		assertTrue(message.contains("sw[0] <pin>"), message);
		assertTrue(message.contains("sw[1] <pin>"), message);
	}

	@Test
	void anOutOfRangeBitIndexShowsTheValidRange() throws Exception {

		String message = failToEmit(
				"sw[0] PMOD1", "sw[1] PMOD2", "sw[2] PMOD3", "clk CLK",
				"led[0] LED1", "led[1] LED2");
		assertTrue(message.contains("\"sw[2]\""), message);
		assertTrue(message.contains("has bits 0..1"), message);
	}

	@Test
	void onePinBoundToTwoPortsNamesBothClaimants() throws Exception {

		String message = failToEmit(
				"sw[0] PMOD1", "sw[1] PMOD2", "clk CLK",
				"led[0] LED1", "led[1] LED1");
		assertTrue(message.contains("LED1"), message);
		assertTrue(message.contains("\"led[0]\""), message);
		assertTrue(message.contains("\"led[1]\""), message);
	}

	@Test
	void everyProblemIsReportedInOneMessage() throws Exception {

		// four distinct problems at once: unknown pin, missing binding
		// (led[1]), unknown port, and a doubly-claimed pin must all
		// surface in the single failure
		String message = failToEmit(
				"sw[0] PMOD1", "sw[1] PMOD1", "clk CLOCK",
				"led[0] LED1", "btn PMOD3");
		assertTrue(message.contains("\"CLOCK\""), message);
		assertTrue(message.contains("\"led[1]\""), message);
		assertTrue(message.contains("\"btn\""), message);
		assertTrue(message.contains("PMOD1"), message);
	}

	@Test
	void malformedBindingLinesAreAllReportedWithLineNumbers() {

		HdlExportException e = assertThrows(HdlExportException.class,
				() -> PinBindings.parse(List.of(
						"# a comment line",
						"sw[0]",
						"",
						"clk CLK extra")));
		String message = e.getMessage();
		assertTrue(message.contains("line 2"), message);
		assertTrue(message.contains("line 4"), message);
		assertTrue(message.contains("<port> <pin>"), message);
	}

	@Test
	void aKeyBoundTwiceIsAParseError() {

		HdlExportException e = assertThrows(HdlExportException.class,
				() -> PinBindings.parse(List.of(
						"clk CLK",
						"clk LED1")));
		String message = e.getMessage();
		assertTrue(message.contains("line 2"), message);
		assertTrue(message.contains("bound twice"), message);
	}

	@Test
	void commentsAndBlankLinesAreIgnored() throws Exception {

		PinBindings bindings = PinBindings.parse(List.of(
				"# full-line comment",
				"",
				"   ",
				"clk CLK # trailing comment"));
		List<String> keys =
				new ArrayList<String>(bindings.asMap().keySet());
		assertTrue(keys.equals(List.of("clk")), keys.toString());
	}
}
