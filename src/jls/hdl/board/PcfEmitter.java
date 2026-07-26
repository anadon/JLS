package jls.hdl.board;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jls.hdl.HdlExportException;
import jls.hdl.HdlModel;

/**
 * Renders a PCF pin-constraint file (icestorm/nextpnr-ice40 syntax)
 * for a named {@link Board}, walking exactly the port set
 * {@link jls.hdl.HdlExporter#buildModel} produced — the same ports the
 * Verilog/VHDL emitters render, so the constraint file and the HDL can
 * never disagree about the module's interface (issue #213, hypothesis
 * H1). Output is deterministic: ports in model order, bits ascending,
 * one {@code set_io} line per port bit, each preceded by a provenance
 * comment.
 *
 * <p>Binding is all-or-nothing (prediction P3 of #213): every problem
 * — an unbound port bit, a binding for a port the module does not
 * have, a wrong indexed/scalar form, an unknown board pin, one pin
 * claimed twice — is collected and reported in a single
 * {@link HdlExportException}, and no text is returned, so a partial or
 * invalid constraint file can never reach disk.</p>
 */
public final class PcfEmitter {

	/** Not instantiable; all entry points are static. */
	private PcfEmitter() {
	} // not instantiable

	/** Shape of an indexed binding key: {@code <port>[<bit>]}. */
	private static final Pattern INDEXED =
			Pattern.compile("^(.+)\\[(\\d+)]$");

	/**
	 * Render the PCF text binding the model's ports to the board's
	 * pins.
	 *
	 * @param model The model the HDL emitters render; only its ports
	 * and identity (module name, JLS version) are read.
	 * @param board The target board; must use the PCF format.
	 * @param bindings The user's port-to-pin bindings.
	 *
	 * @return the complete, valid PCF text.
	 *
	 * @throws HdlExportException if any port cannot be bound; the
	 * message names every problem and nothing is returned.
	 *
	 * @jls.testedby jls.hdl.board.PcfGoldenTest#icestickConstraintsMatchTheGolden()
	 * @jls.testedby jls.hdl.board.UnbindablePortsTest
	 */
	public static String emit(HdlModel model, Board board,
			PinBindings bindings) throws HdlExportException {

		if (board.format() != Board.Format.PCF) {
			throw new IllegalArgumentException("board " + board.name()
					+ " uses " + board.format() + " constraints, not PCF");
		}

		Map<String, String> unclaimed =
				new LinkedHashMap<String, String>(bindings.asMap());
		Map<String, String> pinToKey = new TreeMap<String, String>();
		List<String> errors = new ArrayList<String>();
		StringBuilder body = new StringBuilder();

		// the same port set, in the same order, as the HDL emitters
		for (HdlModel.Port port : model.ports()) {
			String direction =
					port.direction() == HdlModel.Direction.INPUT
							? "input" : "output";
			int bits = Math.max(port.bits(), 1);
			for (int bit = 0; bit < bits; bit += 1) {
				String key = bits == 1 ? port.name()
						: port.name() + "[" + bit + "]";
				String pin = unclaimed.remove(key);
				if (pin == null) {
					errors.add("port \"" + key + "\" (" + port.comment()
							+ ") has no pin binding; add a line \"" + key
							+ " <pin>\" to the bindings file");
					continue;
				}
				String location = board.pins().get(pin);
				if (location == null) {
					errors.add("port \"" + key + "\" is bound to \"" + pin
							+ "\", which is not a pin of " + board.name()
							+ "; available pins: " + board.pinNames());
					continue;
				}
				String prior = pinToKey.putIfAbsent(pin, key);
				if (prior != null) {
					errors.add("pin " + pin + " (location " + location
							+ ") is bound to both \"" + prior + "\" and \""
							+ key + "\"");
					continue;
				}
				body.append("# ").append(direction).append(' ').append(key)
						.append(" <- pin ").append(pin).append('\n');
				body.append("set_io ").append(key).append(' ')
						.append(location).append('\n');
			}
		}

		// bindings that claimed no port bit: unknown ports, or the
		// wrong scalar/indexed form for a port that does exist
		for (String key : unclaimed.keySet()) {
			errors.add(diagnoseLeftover(key, model));
		}

		if (!errors.isEmpty()) {
			throw new HdlExportException("cannot bind module \""
					+ model.moduleName + "\" to board " + board.name()
					+ ": " + String.join("; ", errors));
		}

		StringBuilder out = new StringBuilder();
		out.append("# Board: ").append(board.name()).append(" (")
				.append(board.fpga()).append(")\n");
		out.append("# Module: ").append(model.moduleName)
				.append(", exported by JLS ").append(model.jlsVersion)
				.append('\n');
		out.append("# Format: PCF, for the open iCE40 flow"
				+ " (yosys + nextpnr-ice40 --pcf)\n");
		out.append('\n');
		out.append(body);
		return out.toString();
	} // end of emit method

	/**
	 * Explain one binding key that matched no port bit: it names a
	 * port the module does not have, or uses the wrong scalar/indexed
	 * form, or indexes past the port's width. (An in-range key for an
	 * existing port is always claimed by the port walk, so those three
	 * cases are exhaustive here.)
	 *
	 * @param key The unclaimed binding key.
	 * @param model The model whose ports were walked.
	 *
	 * @return an actionable error message for the key.
	 */
	private static String diagnoseLeftover(String key, HdlModel model) {

		String base = key;
		boolean indexed = false;
		Matcher m = INDEXED.matcher(key);
		if (m.matches()) {
			base = m.group(1);
			indexed = true;
		}
		for (HdlModel.Port port : model.ports()) {
			if (!port.name().equals(base)) {
				continue;
			}
			int bits = Math.max(port.bits(), 1);
			if (indexed && bits == 1) {
				return "\"" + key + "\": port \"" + base
						+ "\" is 1 bit wide; bind it without an index (\""
						+ base + " <pin>\")";
			}
			if (!indexed && bits > 1) {
				return "\"" + key + "\": port \"" + base + "\" is " + bits
						+ " bits wide; bind each bit (\"" + base
						+ "[0] <pin>\" ... \"" + base + "[" + (bits - 1)
						+ "] <pin>\")";
			}
			return "\"" + key + "\": port \"" + base
					+ "\" has bits 0.." + (bits - 1);
		}
		return "\"" + key
				+ "\" does not name a module port; the ports are: "
				+ portList(model);
	} // end of diagnoseLeftover method

	/**
	 * The module's ports as one comma-separated listing with widths
	 * and directions, for unknown-port error messages.
	 *
	 * @param model The model whose ports to list.
	 *
	 * @return the listing, in model port order.
	 */
	private static String portList(HdlModel model) {

		List<String> ports = new ArrayList<String>();
		for (HdlModel.Port port : model.ports()) {
			ports.add(port.name() + " (" + Math.max(port.bits(), 1)
					+ " bit" + (Math.max(port.bits(), 1) == 1 ? "" : "s")
					+ ", " + (port.direction() == HdlModel.Direction.INPUT
							? "input" : "output") + ")");
		}
		return String.join(", ", ports);
	} // end of portList method

} // end of PcfEmitter class
