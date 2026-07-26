package jls.hdl.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import jls.JLSInfo;
import jls.hdl.HdlExporter;
import jls.hdl.HdlModel;

/**
 * Golden-file test of the PCF constraint emitter (issue #213): the
 * blinky fixture bound to the iCEstick must render byte-for-byte to
 * the committed golden in test/resources/hdl/board, the same regime as
 * the Verilog/VHDL golden suites. The JLS version in the generated
 * header is the only version-dependent text; the golden holds the
 * token {@code @VERSION@} where it appears.
 *
 * <p>To regenerate after an intentional format change:
 * {@code mvn test -Dtest=PcfGoldenTest -Djls.hdl.regenerate=true},
 * then review the diff like source.</p>
 */
class PcfGoldenTest {

	private static final Path GOLDEN_DIR =
			Path.of("test", "resources", "hdl", "board");
	private static final String VERSION_TOKEN = "@VERSION@";

	/** The bindings that place blinky on the iCEstick. */
	private static PinBindings blinkyBindings() throws Exception {
		return PinBindings.parse(List.of(
				"# blinky on the iCEstick: switches on the Pmod,",
				"# LEDs on the board LEDs, clock on the 12 MHz oscillator",
				"sw[0] PMOD1",
				"sw[1] PMOD2",
				"clk CLK",
				"led[0] LED1",
				"led[1] LED2"));
	}

	@Test
	void icestickConstraintsMatchTheGolden() throws Exception {

		Board board = Boards.byName("icestick");
		assertNotNull(board, "icestick must be in the built-in table");
		HdlModel model = HdlExporter.buildModel(BoardFixtures.blinky());
		String pcf = PcfEmitter.emit(model, board, blinkyBindings());

		String tokenized =
				pcf.replace(JLSInfo.versionString, VERSION_TOKEN);
		Path golden = GOLDEN_DIR.resolve("blinky_icestick.pcf");
		if (Boolean.getBoolean("jls.hdl.regenerate")) {
			Files.createDirectories(GOLDEN_DIR);
			Files.writeString(golden, tokenized, StandardCharsets.UTF_8);
		}
		assertTrue(Files.isRegularFile(golden),
				"missing golden " + golden + " (regenerate with"
						+ " -Djls.hdl.regenerate=true and review the diff)");
		String expected = Files.readString(golden, StandardCharsets.UTF_8);
		assertEquals(expected, tokenized,
				"PCF emission diverged from " + golden);
	}

	@Test
	void emissionIsDeterministic() throws Exception {

		Board board = Boards.byName("icestick");
		assertNotNull(board);
		// two independent walks of two independently loaded circuits
		// must produce identical bytes (issue #213 requires the golden
		// to be byte-deterministic, not merely stable within one run)
		String first = PcfEmitter.emit(
				HdlExporter.buildModel(BoardFixtures.blinky()), board,
				blinkyBindings());
		String second = PcfEmitter.emit(
				HdlExporter.buildModel(BoardFixtures.blinky()), board,
				blinkyBindings());
		assertEquals(first, second);
	}

	@Test
	void boardLookupIsCaseInsensitive() {

		assertNotNull(Boards.byName("iCEstick"),
				"-board matching is documented as case-insensitive");
	}
}
