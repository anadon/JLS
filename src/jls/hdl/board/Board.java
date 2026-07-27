package jls.hdl.board;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * One supported FPGA development board (issue #213): the name the user
 * gives on the command line, the FPGA it carries, the constraint-file
 * format its toolchain consumes, and the board's named pins. A board is
 * deliberately just data — (name, format, pin map) per hypothesis H2 of
 * #213 — so adding a board is adding a table entry in {@link Boards},
 * never new code.
 *
 * @param name User-facing board name, matched case-insensitively
 *             against the {@code -board} operand; stored lower-case.
 * @param fpga Human description of the FPGA part and package, for
 *             generated-file headers and documentation.
 * @param format The constraint-file format the board's toolchain
 *               consumes.
 * @param pins Board pin names (e.g. {@code LED1}, {@code CLK}) to
 *             physical locations (e.g. {@code 99}); pinned to a sorted
 *             immutable copy.
 */
public record Board(String name, String fpga, Format format,
		Map<String, String> pins) {

	/**
	 * The constraint-file formats JLS can emit. Only PCF (the iCE40
	 * open flow) exists today; XDC (AMD/Xilinx) and QSF (Intel/Altera)
	 * are #213 follow-ups and get enum constants when their emitters
	 * land.
	 */
	public enum Format {

		/**
		 * Lattice iCE40 Physical Constraint File, consumed by the open
		 * icestorm flow ({@code nextpnr-ice40 --pcf}).
		 */
		PCF("pcf");

		/** The format's conventional file extension, without the dot. */
		private final String extension;

		/**
		 * Creates a format constant.
		 *
		 * @param extension The conventional file extension, no dot.
		 */
		Format(String extension) {
			this.extension = extension;
		} // end of Format constructor

		/**
		 * The conventional file extension for this format.
		 *
		 * @return the extension, without the leading dot.
		 */
		public String extension() {
			return extension;
		} // end of extension method
	} // end of Format enum

	/**
	 * Orders pin names naturally — digit runs compare by numeric value,
	 * so {@code PMOD2} precedes {@code PMOD10} — with a plain lexical
	 * tie-break so distinct names (e.g. {@code A1} vs {@code A01})
	 * never collapse into one map key. Used to sort the pin map so the
	 * unknown-pin error listing scans in the order a user expects.
	 */
	static final Comparator<String> NATURAL_PIN_ORDER =
			Board::compareNaturally;

	/**
	 * Pins the pin map to a naturally-sorted immutable copy, so a board
	 * stays a value and its pin listing (errors, documentation) is
	 * deterministic no matter what map the table handed in.
	 */
	public Board {
		TreeMap<String, String> sorted =
				new TreeMap<String, String>(NATURAL_PIN_ORDER);
		sorted.putAll(pins);
		pins = Collections.unmodifiableSortedMap(sorted);
	} // end of Board compact constructor

	/**
	 * The comparison behind {@link #NATURAL_PIN_ORDER}.
	 *
	 * @param a One pin name.
	 * @param b The other pin name.
	 *
	 * @return negative, zero, or positive per the comparator contract.
	 */
	private static int compareNaturally(String a, String b) {

		int i = 0;
		int j = 0;
		while (i < a.length() && j < b.length()) {
			char ca = a.charAt(i);
			char cb = b.charAt(j);
			if (Character.isDigit(ca) && Character.isDigit(cb)) {
				// compare the whole digit runs as numbers: strip
				// leading zeros, then a longer run is a larger number
				// and equal-length runs compare digit by digit
				int aStart = i;
				int bStart = j;
				while (i < a.length()
						&& Character.isDigit(a.charAt(i))) {
					i += 1;
				}
				while (j < b.length()
						&& Character.isDigit(b.charAt(j))) {
					j += 1;
				}
				while (aStart < i - 1 && a.charAt(aStart) == '0') {
					aStart += 1;
				}
				while (bStart < j - 1 && b.charAt(bStart) == '0') {
					bStart += 1;
				}
				int byLength =
						Integer.compare(i - aStart, j - bStart);
				if (byLength != 0) {
					return byLength;
				}
				int byDigits = a.substring(aStart, i)
						.compareTo(b.substring(bStart, j));
				if (byDigits != 0) {
					return byDigits;
				}
			} else {
				if (ca != cb) {
					return Character.compare(ca, cb);
				}
				i += 1;
				j += 1;
			}
		}
		int byRemainder =
				Integer.compare(a.length() - i, b.length() - j);
		if (byRemainder != 0) {
			return byRemainder;
		}
		return a.compareTo(b); // e.g. A1 vs A01: distinct keys
	} // end of compareNaturally method

	/**
	 * The board's pin names as one comma-separated string, in natural
	 * order (so {@code PMOD2} before {@code PMOD10}), for actionable
	 * unknown-pin error messages.
	 *
	 * @return the available pin names, comma-joined.
	 */
	public String pinNames() {
		return String.join(", ", pins.keySet());
	} // end of pinNames method

} // end of Board record
