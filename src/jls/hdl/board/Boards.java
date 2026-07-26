package jls.hdl.board;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The built-in board table (issue #213): the named cheap development
 * boards JLS can emit pin constraints for. Kept deliberately tiny
 * (hypothesis H2 of #213): a board is one {@link Board} value here,
 * with its pin map transcribed from the vendor documentation, and the
 * table grows on demand rather than through a general board-description
 * format. First (and so far only) entry: the Lattice iCEstick, the
 * classic $30 iCE40 teaching board with a fully open toolchain
 * (yosys + nextpnr-ice40 + icepack + iceprog).
 */
public final class Boards {

	/** Not instantiable; the table is static. */
	private Boards() {
	} // not instantiable

	/**
	 * Lattice iCEstick evaluation kit (iCE40-HX1K, TQ144 package).
	 * Pin map transcribed from the iCEstick user guide and the
	 * icestorm example constraints: the 12 MHz clock, the five LEDs
	 * (D1–D4 red, D5 green), the FTDI UART, the IrDA transceiver, the
	 * Pmod connector J2 (signal pins 1–4 and 7–10), and headers J1 and
	 * J3 (signal pins 3–10 on each).
	 */
	private static final Board ICESTICK = new Board("icestick",
			"Lattice iCE40-HX1K, TQ144 package", Board.Format.PCF,
			Map.ofEntries(
					// 12 MHz oscillator
					Map.entry("CLK", "21"),
					// LEDs D1..D4 (red) and D5 (green)
					Map.entry("LED1", "99"),
					Map.entry("LED2", "98"),
					Map.entry("LED3", "97"),
					Map.entry("LED4", "96"),
					Map.entry("LED5", "95"),
					// FTDI channel B UART (TTL side)
					Map.entry("UART_RX", "9"),
					Map.entry("UART_TX", "8"),
					// IrDA transceiver
					Map.entry("IR_TXD", "105"),
					Map.entry("IR_RXD", "106"),
					Map.entry("IR_SD", "107"),
					// Pmod connector J2, signal pins 1-4 and 7-10
					Map.entry("PMOD1", "78"),
					Map.entry("PMOD2", "79"),
					Map.entry("PMOD3", "80"),
					Map.entry("PMOD4", "81"),
					Map.entry("PMOD7", "87"),
					Map.entry("PMOD8", "88"),
					Map.entry("PMOD9", "90"),
					Map.entry("PMOD10", "91"),
					// header J1, signal pins 3-10
					Map.entry("J1_3", "112"),
					Map.entry("J1_4", "113"),
					Map.entry("J1_5", "114"),
					Map.entry("J1_6", "115"),
					Map.entry("J1_7", "116"),
					Map.entry("J1_8", "117"),
					Map.entry("J1_9", "118"),
					Map.entry("J1_10", "119"),
					// header J3, signal pins 3-10
					Map.entry("J3_3", "62"),
					Map.entry("J3_4", "61"),
					Map.entry("J3_5", "60"),
					Map.entry("J3_6", "56"),
					Map.entry("J3_7", "48"),
					Map.entry("J3_8", "47"),
					Map.entry("J3_9", "45"),
					Map.entry("J3_10", "44")));

	/** Every built-in board, in documentation order. */
	private static final List<Board> ALL = List.of(ICESTICK);

	/**
	 * Every built-in board, in documentation order.
	 *
	 * @return the boards, immutable.
	 */
	public static List<Board> all() {
		return ALL;
	} // end of all method

	/**
	 * Look a board up by name, case-insensitively.
	 *
	 * @param name The board name to look up.
	 *
	 * @return the board, or null if no built-in board has that name.
	 */
	public static @Nullable Board byName(String name) {

		String wanted = name.toLowerCase(Locale.ROOT);
		for (Board board : ALL) {
			if (board.name().equals(wanted)) {
				return board;
			}
		}
		return null;
	} // end of byName method

	/**
	 * The supported board names as one comma-separated string, for
	 * usage text and unknown-board error messages.
	 *
	 * @return the board names, comma-joined, in documentation order.
	 */
	public static String names() {

		List<String> names = new ArrayList<String>();
		for (Board board : ALL) {
			names.add(board.name());
		}
		return String.join(", ", names);
	} // end of names method

} // end of Boards class
