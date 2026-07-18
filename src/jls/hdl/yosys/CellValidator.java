package jls.hdl.yosys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The importer's gatekeeper (issue #61): checks every cell of a
 * post-pipeline netlist against the restricted set the cell-to-element
 * mapper understands, and collects a violation for each cell outside
 * it. All violations are gathered in one pass — the user learns the
 * full repair job from a single failed import — and per the issue's
 * addendum no circuit is ever built from a netlist with violations.
 *
 * Cells fall into four categories:
 * <ul>
 * <li><b>Supported.</b> The survivors of the fixed pass pipeline
 * (research report §6): word-level gates, reductions, {@code $mux} /
 * {@code $bmux}, {@code $dff} / {@code $dlatch}, {@code $tribuf},
 * {@code $add}, plus hierarchy instances (any non-{@code $} type)
 * and the read-only async-read memory described below.</li>
 * <li><b>Teachable rejects.</b> Constructs genuinely outside JLS's
 * teaching element set: asynchronous-reset storage, set/reset
 * storage, wide arithmetic, clocked or multi-port memories. Each
 * carries a message written for a student — what the construct is,
 * why JLS cannot represent it, and the rewrite that will import.</li>
 * <li><b>Memories, decided by parameters.</b> A {@code $mem}/
 * {@code $mem_v2} with no write ports and one combinational
 * (unclocked) read port is the async-read ROM that maps faithfully
 * onto JLS Memory (per the resolved 2026-07-17 decisions, imported
 * with access time 0); anything else is a teachable reject.</li>
 * <li><b>Pipeline leftovers.</b> Internal cells the pass pipeline
 * should have eliminated ({@code $sub}, {@code $eq}, {@code $pmux},
 * ...). Their appearance means a techmap rule is missing — a JLS
 * bug, not a user error — and the message says exactly that, still
 * naming the cell and source location. Nothing is ever silently
 * mis-mapped (issue #61 prediction P2).</li>
 * </ul>
 */
public final class CellValidator {

	/**
	 * Cell types the mapper consumes directly (research report §6
	 * mapping table). {@code $pos} is a word-level buffer that
	 * {@code opt_clean} usually removes; when one survives it maps
	 * to plain wiring.
	 */
	private static final Set<String> SUPPORTED =
			new HashSet<String>(Arrays.asList(
			"$not", "$and", "$or", "$xor",
			"$reduce_and", "$reduce_or", "$reduce_xor",
			"$reduce_xnor", "$reduce_bool",
			"$logic_not", "$logic_and", "$logic_or",
			"$mux", "$bmux",
			"$dff", "$dlatch",
			"$tribuf",
			"$add",
			"$pos"));

	/**
	 * Teachable rejects: cell type to the message explaining the
	 * construct, why it is out of scope, and the rewrite. Memory
	 * cells are handled separately (parameters decide).
	 */
	private static final Map<String, String> TEACHABLE =
			buildTeachable();

	/** The teachable-reject message for asynchronous-reset storage. */
	private static final String ASYNC_RESET_MESSAGE =
			"asynchronous reset (the \"always @(posedge clk or"
			+ " posedge rst)\" idiom): JLS registers reset"
			+ " synchronously. Move the reset into the clocked block"
			+ " - \"always @(posedge clk) if (rst) q <= 0; else"
			+ " ...\" - and the design will import exactly";

	/** The teachable-reject message for set/reset storage. */
	private static final String SET_RESET_MESSAGE =
			"set/reset storage: JLS has no latch or flip-flop with"
			+ " set/reset inputs. Rewrite the storage as a plain"
			+ " latch or as a register with a synchronous reset";

	/** The teachable-reject message for wide arithmetic. */
	private static final String ARITHMETIC_MESSAGE =
			"multiply/divide/modulo/power (*, /, %, **): JLS's"
			+ " element set has no arithmetic beyond the Adder."
			+ " Build the operation structurally (for example,"
			+ " shift-and-add multiplication) or precompute constant"
			+ " results";

	/** The teachable-reject message for unsupported memories. */
	private static final String MEMORY_MESSAGE =
			"clocked or multi-port memory: JLS Memory is a"
			+ " single-port memory read asynchronously. Only"
			+ " ROM-style arrays - initialized, read with a plain"
			+ " assign from one place, and never written - import"
			+ " today";

	/** Memory cell types, decided by their parameters. */
	private static final Set<String> MEMORY_TYPES =
			new HashSet<String>(Arrays.asList("$mem", "$mem_v2"));

	/**
	 * Memory-building cell types that only appear when the pipeline's
	 * {@code memory -nomap} collection step did not run or did not
	 * finish; treated as unsupported-memory rejects since the user's
	 * construct is the same.
	 */
	private static final Set<String> MEMORY_FRAGMENT_TYPES =
			new HashSet<String>(Arrays.asList(
			"$memrd", "$memrd_v2", "$memwr", "$memwr_v2",
			"$meminit", "$meminit_v2"));

	/** No instances; the class is two static methods and tables. */
	private CellValidator() {

	} // end of constructor

	/**
	 * Builds the teachable-reject table.
	 *
	 * @return cell type mapped to its teaching message.
	 */
	private static Map<String, String> buildTeachable() {

		Map<String, String> t = new LinkedHashMap<String, String>();
		// async-reset / async-load flip-flops and latches ($adff
		// family): the one common idiom the pipeline cannot
		// legalize, kept on the reject list per the 2026-07-17
		// decision until corpus evidence justifies a Register pin
		for (String type : Arrays.asList("$adff", "$adffe",
				"$aldff", "$aldffe", "$adlatch")) {
			t.put(type, ASYNC_RESET_MESSAGE);
		}
		for (String type : Arrays.asList("$sr", "$dffsr", "$dffsre",
				"$dlatchsr")) {
			t.put(type, SET_RESET_MESSAGE);
		}
		for (String type : Arrays.asList("$mul", "$div", "$divfloor",
				"$mod", "$modfloor", "$pow")) {
			t.put(type, ARITHMETIC_MESSAGE);
		}
		return t;
	} // end of buildTeachable method

	/**
	 * Checks every cell of every module against the restricted set.
	 *
	 * @param netlist The parsed post-pipeline netlist.
	 *
	 * @return all violations found, in document order; an empty list
	 * means every cell is mappable.
	 *
	 * @throws NetlistFormatException if a memory cell's deciding
	 * parameters are missing or malformed.
	 */
	public static List<CellViolation> validate(YosysNetlist netlist)
			throws NetlistFormatException {

		List<CellViolation> violations =
				new ArrayList<CellViolation>();
		for (YosysNetlist.Module module
				: netlist.modules().values()) {
			for (YosysNetlist.Cell cell : module.cells().values()) {
				String message = check(cell);
				if (message != null) {
					violations.add(new CellViolation(module.name(),
							cell.name(), cell.type(),
							cell.sourceLocation(), message));
				}
			}
		}
		return violations;
	} // end of validate method

	/**
	 * Checks one cell.
	 *
	 * @param cell The cell to check.
	 *
	 * @return null when the cell is mappable, else the violation
	 * message.
	 *
	 * @throws NetlistFormatException if a memory cell's deciding
	 * parameters are missing or malformed.
	 */
	private static String check(YosysNetlist.Cell cell)
			throws NetlistFormatException {

		String type = cell.type();
		if (!type.startsWith("$")) {
			// a hierarchy instance of a user module; the mapper
			// realizes it as a JLS subcircuit
			return null;
		}
		if (SUPPORTED.contains(type)) {
			return null;
		}
		String teachable = TEACHABLE.get(type);
		if (teachable != null) {
			return teachable;
		}
		if (MEMORY_TYPES.contains(type)) {
			return checkMemory(cell);
		}
		if (MEMORY_FRAGMENT_TYPES.contains(type)) {
			return MEMORY_MESSAGE;
		}
		return "internal cell \"" + type + "\" should have been"
				+ " eliminated by the import pass pipeline. This is"
				+ " a JLS import bug, not an error in your Verilog -"
				+ " please report it, including the source file";
	} // end of check method

	/**
	 * Decides a {@code $mem}/{@code $mem_v2} cell: the async-read
	 * ROM shape (no write ports, exactly one read port, read port
	 * unclocked) is supported; everything else gets the memory
	 * teaching message.
	 *
	 * @param cell The memory cell.
	 *
	 * @return null when supported, else the violation message.
	 *
	 * @throws NetlistFormatException if the deciding parameters are
	 * missing or malformed.
	 */
	private static String checkMemory(YosysNetlist.Cell cell)
			throws NetlistFormatException {

		long writePorts = cell.param("WR_PORTS");
		long readPorts = cell.param("RD_PORTS");
		// one bit per read port; nonzero means some port is clocked
		long readClockEnable = cell.param("RD_CLK_ENABLE");
		if (writePorts == 0 && readPorts == 1
				&& readClockEnable == 0) {
			return null;
		}
		return MEMORY_MESSAGE;
	} // end of checkMemory method

} // end of CellValidator class
