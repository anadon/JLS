package jls.hdl.board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jls.hdl.HdlExportException;

/**
 * The user's port-to-pin bindings, parsed from the {@code -pins} file
 * (issue #213, binding UX option (a): headless and autograder-friendly).
 * The format is one binding per line — {@code <port> <pin>} for one-bit
 * ports, {@code <port>[<bit>] <pin>} per bit for wider ones — with
 * blank lines and {@code #} comments ignored. Parsing checks only the
 * file's own shape (two tokens per line, no key bound twice) and
 * reports every malformed line in one exception; matching the keys
 * against the module's actual ports and the board's actual pins is
 * {@link PcfEmitter}'s job, where the {@link jls.hdl.HdlModel} and
 * {@link Board} are both in hand.
 */
public final class PinBindings {

	/** Binding key ({@code port} or {@code port[bit]}) to board pin name, in file order. */
	private final Map<String, String> bindings;

	/**
	 * Wraps a parsed binding map; only {@link #parse} constructs one.
	 *
	 * @param bindings The parsed key-to-pin map.
	 */
	private PinBindings(Map<String, String> bindings) {
		this.bindings = bindings;
	} // end of PinBindings constructor

	/**
	 * Parse a bindings file's lines. Every malformed line is collected
	 * and reported together, so the user learns the full repair job
	 * from a single failure.
	 *
	 * @param lines The file's lines, in order.
	 *
	 * @return the parsed bindings.
	 *
	 * @throws HdlExportException if any line is malformed or any key is
	 * bound twice; the message names every offending line by number.
	 *
	 * @jls.testedby jls.hdl.board.UnbindablePortsTest#malformedBindingLinesAreAllReportedWithLineNumbers()
	 * @jls.testedby jls.hdl.board.UnbindablePortsTest#aKeyBoundTwiceIsAParseError()
	 */
	public static PinBindings parse(List<String> lines)
			throws HdlExportException {

		Map<String, String> bindings =
				new LinkedHashMap<String, String>();
		List<String> errors = new ArrayList<String>();
		for (int n = 0; n < lines.size(); n += 1) {
			String line = lines.get(n);
			int hash = line.indexOf('#');
			if (hash >= 0) {
				line = line.substring(0, hash);
			}
			line = line.trim();
			if (line.isEmpty()) {
				continue;
			}
			String[] tokens = line.split("\\s+");
			if (tokens.length != 2) {
				errors.add("line " + (n + 1)
						+ ": expected '<port> <pin>', got \"" + line + "\"");
				continue;
			}
			if (bindings.containsKey(tokens[0])) {
				errors.add("line " + (n + 1) + ": \"" + tokens[0]
						+ "\" is bound twice");
				continue;
			}
			bindings.put(tokens[0], tokens[1]);
		}
		if (!errors.isEmpty()) {
			throw new HdlExportException("pin bindings file is malformed: "
					+ String.join("; ", errors));
		}
		return new PinBindings(bindings);
	} // end of parse method

	/**
	 * The parsed bindings.
	 *
	 * @return binding key ({@code port} or {@code port[bit]}) to board
	 * pin name, unmodifiable, in file order.
	 */
	public Map<String, String> asMap() {
		return Collections.unmodifiableMap(bindings);
	} // end of asMap method

} // end of PinBindings class
