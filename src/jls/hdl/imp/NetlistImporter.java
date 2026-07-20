package jls.hdl.imp;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jls.JLSInfo;
import jls.hdl.layout.HeuristicLayeredLayouter;
import jls.hdl.layout.LayoutException;
import jls.hdl.layout.LayoutGraph;
import jls.hdl.layout.LayoutResult;
import jls.hdl.yosys.CellValidator;
import jls.hdl.yosys.CellViolation;
import jls.hdl.yosys.NetlistFormatException;
import jls.hdl.yosys.YosysNetlist;

/**
 * The Stage 2 cell-to-element mapper and save-format emitter (issue
 * #61): turns a validated Yosys {@code write_json} netlist into an
 * editable JLS circuit in plain-text save format. It closes the loop
 * between the netlist model ({@link YosysNetlist}), the gatekeeper
 * ({@link CellValidator}), and the auto-layout seam (issue #62) - it
 * builds a {@link LayoutGraph}, solves it with the
 * {@link HeuristicLayeredLayouter}, then realizes the placed
 * {@link LayoutResult} as {@code ELEMENT} blocks and {@code WireEnd}
 * chains anchored at the exact port attachment points, so the emitted
 * text loads through JLS's real loader and re-saves identically to a
 * hand-drawn circuit.
 *
 * <p><b>Scope of this increment.</b> The mapper realizes the directly
 * mappable combinational core of the report's §6 table - module ports
 * ({@code InputPin}/{@code OutputPin}), the word-level bitwise gates
 * {@code $not}/{@code $and}/{@code $or}/{@code $xor}, the 2:1
 * {@code $mux}, and constant drivers - wired word-for-word: two ports
 * connect exactly when they carry the same Yosys bit vector. Cells the
 * {@link CellValidator} accepts but this increment does not yet realize
 * ({@code $add}, {@code $dff}, {@code $dlatch}, {@code $tribuf}, the
 * reductions, {@code $bmux}, and hierarchy instances), bit-level slices
 * and concatenations that would need a Splitter/Binder mesh, and width
 * mismatches that would need an {@code Extend}, are all reported as
 * import problems rather than silently mis-mapped (issue #61 P2): no
 * partial circuit is ever emitted.</p>
 */
public final class NetlistImporter {

	/** The snap grid; all emitted geometry is a multiple of it. */
	private static final int GRID = JLSInfo.spacing;

	/** No instances; the class is one static entry point. */
	private NetlistImporter() {

	} // end of constructor

	/**
	 * Imports a parsed netlist into JLS save-format text.
	 *
	 * @param netlist The parsed, post-pipeline netlist.
	 *
	 * @return the save text and mapping summary.
	 *
	 * @throws ImportException if any cell is unmappable, any connection
	 * is not a whole-vector match, or the netlist has no single top
	 * module - with every problem enumerated in one message.
	 */
	public static ImportResult importNetlist(YosysNetlist netlist)
			throws ImportException {

		List<String> problems = new ArrayList<String>();

		List<CellViolation> violations;
		try {
			violations = CellValidator.validate(netlist);
		}
		catch (NetlistFormatException e) {
			throw new ImportException(
					"the netlist could not be validated: "
							+ e.getMessage());
		}
		for (CellViolation violation : violations) {
			problems.add(violation.describe());
		}

		YosysNetlist.Module module = selectModule(netlist, problems);
		if (module == null) {
			throw problem(problems);
		}

		Builder builder = new Builder(module.name());
		mapPorts(module, builder);
		mapCells(module, builder, problems);
		builder.resolveReaders(problems);

		if (!problems.isEmpty()) {
			throw problem(problems);
		}

		LayoutResult layout;
		try {
			layout = new HeuristicLayeredLayouter().layout(builder.graph());
		}
		catch (LayoutException e) {
			throw new ImportException(
					"the imported circuit could not be laid out: "
							+ e.getMessage());
		}

		String text = builder.emit(layout);
		return new ImportResult(text, builder.summary());
	} // end of importNetlist method

	/**
	 * Picks the single top module to import.
	 *
	 * @param netlist The netlist.
	 * @param problems Receives a problem when the choice is ambiguous.
	 *
	 * @return the module to import, or null when there is not exactly
	 * one importable top.
	 */
	private static YosysNetlist.Module selectModule(YosysNetlist netlist,
			List<String> problems) {

		List<YosysNetlist.Module> all =
				new ArrayList<YosysNetlist.Module>(
						netlist.modules().values());
		if (all.isEmpty()) {
			problems.add("the netlist has no modules to import");
			return null;
		}
		if (all.size() == 1) {
			return all.get(0);
		}
		List<YosysNetlist.Module> tops =
				new ArrayList<YosysNetlist.Module>();
		for (YosysNetlist.Module module : all) {
			String top = module.attributes().get("top");
			if (top != null && !top.isEmpty() && !top.equals("0")) {
				tops.add(module);
			}
		}
		if (tops.size() == 1) {
			return tops.get(0);
		}
		StringBuilder names = new StringBuilder();
		for (YosysNetlist.Module module : all) {
			if (names.length() > 0) {
				names.append(", ");
			}
			names.append(module.name());
		}
		problems.add("the netlist has " + all.size() + " modules ("
				+ names + ") and no single top; multi-module (hierarchy)"
				+ " import is not built in this increment - flatten the"
				+ " design (Yosys \"flatten\") or import one module");
		return null;
	} // end of selectModule method

	/**
	 * Maps each module port to an InputPin or OutputPin.
	 *
	 * @param module The module.
	 * @param builder The circuit builder.
	 */
	private static void mapPorts(YosysNetlist.Module module,
			Builder builder) {

		for (YosysNetlist.Port port : module.ports().values()) {
			int width = port.bits().length;
			switch (port.direction()) {
			case INPUT:
				Builder.Elem in = builder.addInputPin(port.name(), width);
				builder.driver(port.bits(), in, "output");
				break;
			case OUTPUT:
				Builder.Elem out =
						builder.addOutputPin(port.name(), width);
				builder.reader(out, "input", port.bits());
				break;
			default:
				builder.problem("module port \"" + port.name()
						+ "\" is an inout (tri-state bus); JLS realizes"
						+ " tri-state through the TriState element, which"
						+ " this importer increment does not yet emit");
				break;
			}
		}
	} // end of mapPorts method

	/**
	 * Maps each cell to its JLS element(s), or records a problem.
	 *
	 * @param module The module.
	 * @param builder The circuit builder.
	 * @param problems Receives one problem per unmappable cell.
	 */
	private static void mapCells(YosysNetlist.Module module,
			Builder builder, List<String> problems) {

		for (YosysNetlist.Cell cell : module.cells().values()) {
			try {
				mapCell(cell, builder);
			}
			catch (NetlistFormatException e) {
				problems.add("cell \"" + cell.name() + "\" (" + cell.type()
						+ "): " + e.getMessage());
			}
		}
	} // end of mapCells method

	/**
	 * Maps one cell.
	 *
	 * @param cell The cell.
	 * @param builder The circuit builder.
	 *
	 * @throws NetlistFormatException if a width parameter is malformed.
	 */
	private static void mapCell(YosysNetlist.Cell cell, Builder builder)
			throws NetlistFormatException {

		String type = cell.type();
		if (!type.startsWith("$")) {
			builder.problem("cell \"" + cell.name() + "\" instantiates"
					+ " module \"" + type + "\"; hierarchy (subcircuit)"
					+ " import is not built in this increment - flatten"
					+ " the design in Yosys and re-import");
			return;
		}
		switch (type) {
		case "$not":
			mapUnaryGate(cell, builder, "NotGate");
			break;
		case "$and":
			mapBinaryGate(cell, builder, "AndGate");
			break;
		case "$or":
			mapBinaryGate(cell, builder, "OrGate");
			break;
		case "$xor":
			mapBinaryGate(cell, builder, "XorGate");
			break;
		case "$mux":
			mapMux(cell, builder);
			break;
		default:
			builder.problem("cell \"" + cell.name() + "\" (" + type
					+ ") is a recognized cell that this importer"
					+ " increment does not yet realize; the mapper so far"
					+ " covers module ports, $not/$and/$or/$xor, $mux, and"
					+ " constants (issue #61 - later increments add the"
					+ " Adder, Register, TriState, reductions and"
					+ " hierarchy)");
			break;
		}
	} // end of mapCell method

	/**
	 * Maps a one-input word-level gate ({@code $not}).
	 *
	 * @param cell The cell.
	 * @param builder The circuit builder.
	 * @param saveType The JLS gate save type.
	 *
	 * @throws NetlistFormatException if a width parameter is malformed.
	 */
	private static void mapUnaryGate(YosysNetlist.Cell cell,
			Builder builder, String saveType)
			throws NetlistFormatException {

		int aw = (int) cell.param("A_WIDTH", 1);
		int yw = (int) cell.param("Y_WIDTH", 1);
		int[] a = connection(cell, "A");
		int[] y = connection(cell, "Y");
		if (aw != yw || a.length != y.length) {
			builder.problem("cell \"" + cell.name() + "\" (" + cell.type()
					+ ") has mismatched input/output widths (" + aw + " vs "
					+ yw + "); width conversion needs an Extend, not built"
					+ " in this increment");
			return;
		}
		Builder.Elem gate = builder.addGate(saveType, y.length, 1);
		builder.reader(gate, "input0", a);
		builder.driver(y, gate, "output");
	} // end of mapUnaryGate method

	/**
	 * Maps a two-input word-level gate ({@code $and}/{@code $or}/
	 * {@code $xor}).
	 *
	 * @param cell The cell.
	 * @param builder The circuit builder.
	 * @param saveType The JLS gate save type.
	 *
	 * @throws NetlistFormatException if a width parameter is malformed.
	 */
	private static void mapBinaryGate(YosysNetlist.Cell cell,
			Builder builder, String saveType)
			throws NetlistFormatException {

		int aw = (int) cell.param("A_WIDTH", 1);
		int bw = (int) cell.param("B_WIDTH", 1);
		int yw = (int) cell.param("Y_WIDTH", 1);
		int[] a = connection(cell, "A");
		int[] b = connection(cell, "B");
		int[] y = connection(cell, "Y");
		if (aw != yw || bw != yw || a.length != y.length
				|| b.length != y.length) {
			builder.problem("cell \"" + cell.name() + "\" (" + cell.type()
					+ ") has operands of unequal width (A=" + aw + " B=" + bw
					+ " Y=" + yw + "); JLS gates operate on equal-width"
					+ " inputs, so the width alignment Yosys expresses"
					+ " needs an Extend, not built in this increment");
			return;
		}
		Builder.Elem gate = builder.addGate(saveType, y.length, 2);
		builder.reader(gate, "input0", a);
		builder.reader(gate, "input1", b);
		builder.driver(y, gate, "output");
	} // end of mapBinaryGate method

	/**
	 * Maps a 2:1 multiplexer ({@code $mux}: {@code Y = S ? B : A}).
	 *
	 * @param cell The cell.
	 * @param builder The circuit builder.
	 *
	 * @throws NetlistFormatException if a width parameter is malformed.
	 */
	private static void mapMux(YosysNetlist.Cell cell, Builder builder)
			throws NetlistFormatException {

		int width = (int) cell.param("WIDTH", 1);
		int[] a = connection(cell, "A");
		int[] b = connection(cell, "B");
		int[] s = connection(cell, "S");
		int[] y = connection(cell, "Y");
		if (a.length != width || b.length != width || y.length != width) {
			builder.problem("cell \"" + cell.name() + "\" ($mux) has data"
					+ " ports whose width disagrees with WIDTH=" + width
					+ "; not realizable without an Extend");
			return;
		}
		if (s.length != 1) {
			builder.problem("cell \"" + cell.name() + "\" ($mux) has a "
					+ s.length + "-bit select; only the 2:1 mux (1-bit"
					+ " select) is realized in this increment - a wide"
					+ " $bmux is a later increment");
			return;
		}
		// $mux: Y = S ? B : A. JLS Mux selects input0 when select == 0,
		// so A -> input0 and B -> input1 reproduces the semantics.
		Builder.Elem mux = builder.addMux(width);
		builder.reader(mux, "input0", a);
		builder.reader(mux, "input1", b);
		builder.reader(mux, "select", s);
		builder.driver(y, mux, "output");
	} // end of mapMux method

	/**
	 * A cell's connection bit vector.
	 *
	 * @param cell The cell.
	 * @param port The port name.
	 *
	 * @return the bit vector.
	 *
	 * @throws NetlistFormatException if the port is not connected.
	 */
	private static int[] connection(YosysNetlist.Cell cell, String port)
			throws NetlistFormatException {

		int[] bits = cell.connections().get(port);
		if (bits == null) {
			throw new NetlistFormatException("missing connection for port"
					+ " \"" + port + "\"");
		}
		return bits;
	} // end of connection method

	/**
	 * Joins all problems into one {@link ImportException}.
	 *
	 * @param problems The collected problems (must be non-empty).
	 *
	 * @return the exception to throw.
	 */
	private static ImportException problem(List<String> problems) {

		StringBuilder message = new StringBuilder("cannot import this"
				+ " design (" + problems.size() + " problem"
				+ (problems.size() == 1 ? "" : "s") + "):");
		for (String p : problems) {
			message.append("\n  - ").append(p);
		}
		return new ImportException(message.toString());
	} // end of problem method

	/**
	 * Accumulates the elements, drivers, readers and wiring of one
	 * imported circuit, then emits the JLS save text once a layout is
	 * solved. Element ids are assigned by creation order, matching the
	 * {@link LayoutGraph} node ids, so a layout position maps straight
	 * back to the element it places.
	 */
	private static final class Builder {

		/** One JLS element to emit and to place. */
		private static final class Elem {

			/** Stable id, unique in the circuit and the layout graph. */
			private final String id;
			/** JLS save type ("AndGate", "InputPin", ...). */
			private final String saveType;
			/** The attribute lines, each already "\n"-terminated. */
			private final String attrs;
			/** Body width in pixels. */
			private final int width;
			/** Body height in pixels. */
			private final int height;
			/** Ports for the layout graph, in declaration order. */
			private final List<LayoutGraph.Port> ports;

			/**
			 * Creates an element.
			 *
			 * @param id The stable id.
			 * @param saveType The JLS save type.
			 * @param attrs The attribute lines.
			 * @param width The body width.
			 * @param height The body height.
			 * @param ports The layout ports.
			 */
			private Elem(String id, String saveType, String attrs,
					int width, int height, List<LayoutGraph.Port> ports) {

				this.id = id;
				this.saveType = saveType;
				this.attrs = attrs;
				this.width = width;
				this.height = height;
				this.ports = ports;
			} // end of constructor
		} // end of Elem class

		/**
		 * One (element, port) endpoint.
		 *
		 * @param elem The element.
		 * @param port The port name on that element.
		 */
		private record Endpoint(Elem elem, String port) {
		} // end of Endpoint record

		/**
		 * One input port waiting to be wired to a driver.
		 *
		 * @param endpoint The reading (element, input port) endpoint.
		 * @param vector The bit vector the port reads.
		 */
		private record Reader(Endpoint endpoint, int[] vector) {
		} // end of Reader record

		/** The imported module name (the circuit name). */
		private final String moduleName;
		/** Elements in creation order; index is the numeric save id. */
		private final List<Elem> elems = new ArrayList<Elem>();
		/** Bit vector (as a key) to the endpoint that drives it. */
		private final Map<String, Endpoint> drivers =
				new LinkedHashMap<String, Endpoint>();
		/** Input endpoints awaiting resolution. */
		private final List<Reader> readers = new ArrayList<Reader>();
		/** The layout graph, populated as elements and edges are added. */
		private final LayoutGraph graph = new LayoutGraph();
		/** Resolved connections: driver endpoint to its reader endpoints. */
		private final List<LayoutGraph.Edge> edgeOrder =
				new ArrayList<LayoutGraph.Edge>();
		/** The running mapping/coercion report. */
		private final ImportSummary summary;
		/** Problems found during resolution. */
		private final List<String> localProblems = new ArrayList<String>();
		/** Sequential suffix making synthesized net names unique. */
		private int netSeq;

		/**
		 * Creates a builder for one module.
		 *
		 * @param moduleName The module (circuit) name.
		 */
		private Builder(String moduleName) {

			this.moduleName = moduleName;
			this.summary = new ImportSummary(moduleName);
		} // end of constructor

		/**
		 * The placement problem accumulated so far.
		 *
		 * @return the layout graph being built.
		 */
		private LayoutGraph graph() {

			return graph;
		} // end of graph method

		/**
		 * The running import report.
		 *
		 * @return the mapping/coercion summary.
		 */
		private ImportSummary summary() {

			return summary;
		} // end of summary method

		/**
		 * Records an import problem discovered while building.
		 *
		 * @param message The problem description.
		 */
		private void problem(String message) {

			localProblems.add(message);
		} // end of problem method

		/**
		 * Adds an InputPin element.
		 *
		 * @param name The (unlegalized) port name.
		 * @param bits The bit width.
		 *
		 * @return the created element.
		 */
		private Elem addInputPin(String name, int bits) {

			String attrs = " String name \"" + legalize(name) + "\"\n"
					+ " int bits " + bits + "\n int watch 0\n"
					+ " String orient \"RIGHT\"\n";
			List<LayoutGraph.Port> ports = new ArrayList<LayoutGraph.Port>();
			ports.add(new LayoutGraph.Port("output",
					LayoutGraph.Direction.OUTPUT, GRID * 4, GRID));
			return add("InputPin", attrs, GRID * 4, GRID * 2, ports, "port");
		} // end of addInputPin method

		/**
		 * Adds an OutputPin element.
		 *
		 * @param name The (unlegalized) port name.
		 * @param bits The bit width.
		 *
		 * @return the created element.
		 */
		private Elem addOutputPin(String name, int bits) {

			String attrs = " String name \"" + legalize(name) + "\"\n"
					+ " int bits " + bits + "\n int watch 0\n"
					+ " String orient \"RIGHT\"\n";
			List<LayoutGraph.Port> ports = new ArrayList<LayoutGraph.Port>();
			ports.add(new LayoutGraph.Port("input",
					LayoutGraph.Direction.INPUT, 0, GRID));
			return add("OutputPin", attrs, GRID * 4, GRID * 2, ports, "port");
		} // end of addOutputPin method

		/**
		 * Adds a gate element (one or two inputs, word-level).
		 *
		 * @param saveType The JLS gate save type.
		 * @param bits The bit width per input/output.
		 * @param numInputs 1 or 2.
		 *
		 * @return the created element.
		 */
		private Elem addGate(String saveType, int bits, int numInputs) {

			String attrs = " int bits " + bits + "\n int numInputs "
					+ numInputs + "\n String orientation \"right\"\n"
					+ " int delay 10\n";
			List<LayoutGraph.Port> ports = new ArrayList<LayoutGraph.Port>();
			if (numInputs == 1) {
				ports.add(new LayoutGraph.Port("input0",
						LayoutGraph.Direction.INPUT, 0, GRID));
			}
			else {
				ports.add(new LayoutGraph.Port("input0",
						LayoutGraph.Direction.INPUT, 0, 0));
				ports.add(new LayoutGraph.Port("input1",
						LayoutGraph.Direction.INPUT, 0, GRID * 2));
			}
			ports.add(new LayoutGraph.Port("output",
					LayoutGraph.Direction.OUTPUT, GRID * 4, GRID));
			return add(saveType, attrs, GRID * 4, GRID * 2, ports,
					gateCell(saveType));
		} // end of addGate method

		/**
		 * Adds a 2:1 Mux element.
		 *
		 * @param bits The data width.
		 *
		 * @return the created element.
		 */
		private Elem addMux(int bits) {

			String attrs = " int inputs 2\n int bits " + bits
					+ "\n int delay 10\n String iOrient \"RIGHT\"\n"
					+ " String sOrient \"DOWN\"\n";
			List<LayoutGraph.Port> ports = new ArrayList<LayoutGraph.Port>();
			ports.add(new LayoutGraph.Port("select",
					LayoutGraph.Direction.INPUT, 0, 0));
			ports.add(new LayoutGraph.Port("input0",
					LayoutGraph.Direction.INPUT, 0, GRID));
			ports.add(new LayoutGraph.Port("input1",
					LayoutGraph.Direction.INPUT, 0, GRID * 3));
			ports.add(new LayoutGraph.Port("output",
					LayoutGraph.Direction.OUTPUT, GRID * 4, GRID * 2));
			return add("Mux", attrs, GRID * 4, GRID * 4, ports, "$mux");
		} // end of addMux method

		/**
		 * Adds a Constant element with a decimal value.
		 *
		 * @param value The value it drives.
		 *
		 * @return the created element.
		 */
		private Elem addConstant(BigInteger value) {

			String attrs = " Int value " + value + "\n int base 10\n"
					+ " String orient \"RIGHT\"\n";
			List<LayoutGraph.Port> ports = new ArrayList<LayoutGraph.Port>();
			ports.add(new LayoutGraph.Port("output",
					LayoutGraph.Direction.OUTPUT, GRID * 2, GRID));
			return add("Constant", attrs, GRID * 2, GRID * 2, ports,
					"constant");
		} // end of addConstant method

		/**
		 * Adds an element to both the emit list and the layout graph.
		 *
		 * @param saveType The JLS save type.
		 * @param attrs The attribute lines.
		 * @param width The body width.
		 * @param height The body height.
		 * @param ports The layout ports.
		 * @param category The summary category to count under.
		 *
		 * @return the created element.
		 */
		private Elem add(String saveType, String attrs, int width,
				int height, List<LayoutGraph.Port> ports, String category) {

			String id = "e" + elems.size();
			Elem elem = new Elem(id, saveType, attrs, width, height, ports);
			elems.add(elem);
			graph.add(new LayoutGraph.Node(id, saveType, width, height,
					ports));
			summary.countElement(category);
			return elem;
		} // end of add method

		/**
		 * Registers an endpoint as the driver of a bit vector.
		 *
		 * @param vector The driven bit vector.
		 * @param elem The driving element.
		 * @param port The driving output port.
		 */
		private void driver(int[] vector, Elem elem, String port) {

			String key = key(vector);
			if (drivers.containsKey(key)) {
				problem("net " + key + " is driven by more than one"
						+ " source; JLS forbids multiply-driven nets");
				return;
			}
			drivers.put(key, new Endpoint(elem, port));
		} // end of driver method

		/**
		 * Registers an input endpoint awaiting a driver.
		 *
		 * @param elem The reading element.
		 * @param port The reading input port.
		 * @param vector The bit vector it reads.
		 */
		private void reader(Elem elem, String port, int[] vector) {

			readers.add(new Reader(new Endpoint(elem, port), vector));
		} // end of reader method

		/**
		 * Resolves every reader into a connection: a whole-vector match
		 * with a driver, a fresh Constant for an all-constant vector, or
		 * a problem for anything else.
		 *
		 * @param problems Receives resolution problems (and any found
		 * while building).
		 */
		private void resolveReaders(List<String> problems) {

			problems.addAll(localProblems);
			for (Reader reader : readers) {
				resolveReader(reader, problems);
			}
			realizeEdges();
		} // end of resolveReaders method

		/**
		 * Resolves one reader.
		 *
		 * @param reader The reader to resolve.
		 * @param problems Receives a problem when it cannot be resolved.
		 */
		private void resolveReader(Reader reader, List<String> problems) {

			int[] vector = reader.vector();
			for (int bit : vector) {
				if (bit == YosysNetlist.BIT_Z) {
					problems.add("input " + describe(reader.endpoint())
							+ " is driven by a high-impedance (z) bit; that"
							+ " needs a TriState element, not built in this"
							+ " increment");
					return;
				}
			}
			if (isAllConstant(vector)) {
				connectConstant(reader);
				return;
			}
			Endpoint source = drivers.get(key(vector));
			if (source == null) {
				problems.add("input " + describe(reader.endpoint()) + " reads"
						+ " a bit pattern that no single element drives as a"
						+ " whole - a bit-level slice or concatenation. JLS"
						+ " expresses those with a Splitter/Binder mesh, which"
						+ " this importer increment does not yet build");
				return;
			}
			pendingSource.add(source);
			pendingTarget.add(reader.endpoint());
		} // end of resolveReader method

		/** Sources of resolved edges, paired with pendingTarget. */
		private final List<Endpoint> pendingSource = new ArrayList<Endpoint>();
		/** Targets of resolved edges, paired with pendingSource. */
		private final List<Endpoint> pendingTarget = new ArrayList<Endpoint>();

		/**
		 * Builds a Constant for an all-constant reader and wires it in.
		 *
		 * @param reader The all-constant reader.
		 */
		private void connectConstant(Reader reader) {

			int coerced = 0;
			BigInteger value = BigInteger.ZERO;
			int[] vector = reader.vector();
			for (int i = vector.length - 1; i >= 0; i -= 1) {
				value = value.shiftLeft(1);
				if (vector[i] == YosysNetlist.BIT_1) {
					value = value.or(BigInteger.ONE);
				}
				else if (vector[i] == YosysNetlist.BIT_X) {
					coerced += 1;
				}
			}
			summary.countCoercedX(coerced);
			Elem constant = addConstant(value);
			pendingSource.add(new Endpoint(constant, "output"));
			pendingTarget.add(reader.endpoint());
		} // end of connectConstant method

		/**
		 * Turns the resolved source/target endpoint pairs into layout
		 * graph edges (done after all elements exist so the graph's node
		 * lookups succeed).
		 */
		private void realizeEdges() {

			for (int i = 0; i < pendingSource.size(); i += 1) {
				Endpoint source = pendingSource.get(i);
				Endpoint target = pendingTarget.get(i);
				String net = "n" + (netSeq += 1) + "_" + source.elem().id;
				LayoutGraph.Edge edge = graph.connect(net,
						source.elem().id, source.port(),
						target.elem().id, target.port(), false);
				edgeOrder.add(edge);
			}
		} // end of realizeEdges method

		/**
		 * Emits the whole circuit as JLS save text over a solved layout.
		 *
		 * @param layout The solved, invariant-clean layout.
		 *
		 * @return the save-format text.
		 */
		private String emit(LayoutResult layout) {

			StringBuilder out = new StringBuilder();
			out.append("CIRCUIT ").append(moduleName).append('\n');
			int id = 0;
			Map<String, Integer> idOf = new LinkedHashMap<String, Integer>();
			for (Elem elem : elems) {
				idOf.put(elem.id, id);
				LayoutResult.Point p = layout.position(elem.id);
				out.append("ELEMENT ").append(elem.saveType).append('\n')
						.append(" int id ").append(id).append('\n')
						.append(" int x ").append(p.x).append('\n')
						.append(" int y ").append(p.y).append('\n')
						.append(" int width ").append(elem.width).append('\n')
						.append(" int height ").append(elem.height)
						.append('\n')
						.append(elem.attrs)
						.append("END\n");
				id += 1;
			}
			emitWires(out, layout, idOf, id);
			out.append("ENDCIRCUIT\n");
			return out.toString();
		} // end of emit method

		/**
		 * Emits the WireEnd chains for every routed edge, sharing one
		 * attached end per port so a fanned-out output drives all its
		 * readers from a single put (issue #62: chains anchored at the
		 * exact port attachment points).
		 *
		 * @param out The output buffer.
		 * @param layout The solved layout.
		 * @param idOf Element id to numeric save id.
		 * @param firstWireId The next free numeric id.
		 */
		private void emitWires(StringBuilder out, LayoutResult layout,
				Map<String, Integer> idOf, int firstWireId) {

			List<WireEnd> ends = new ArrayList<WireEnd>();
			Map<String, WireEnd> portEnds = new LinkedHashMap<String, WireEnd>();
			int[] nextId = {firstWireId};
			for (LayoutGraph.Edge edge : edgeOrder) {
				List<LayoutResult.Point> route = layout.route(edge);
				WireEnd source = portEnd(ends, portEnds, edge.sourceNode.id,
						edge.sourcePort.name, idOf, route.get(0), nextId);
				WireEnd target = portEnd(ends, portEnds, edge.targetNode.id,
						edge.targetPort.name, idOf,
						route.get(route.size() - 1), nextId);
				WireEnd prev = source;
				for (int i = 1; i < route.size() - 1; i += 1) {
					WireEnd bend = new WireEnd(nextId[0]++, route.get(i));
					ends.add(bend);
					link(prev, bend);
					prev = bend;
				}
				link(prev, target);
			}
			for (WireEnd end : ends) {
				end.save(out);
			}
		} // end of emitWires method

		/**
		 * Gets or creates the single attached WireEnd for one port.
		 *
		 * @param ends All wire ends so far (creation order = save order).
		 * @param portEnds Cache of port endpoint to its attached end.
		 * @param nodeId The element id.
		 * @param portName The port name.
		 * @param idOf Element id to numeric save id.
		 * @param at The port attachment point.
		 * @param nextId Single-cell mutable next-free-id counter.
		 *
		 * @return the attached wire end for that port.
		 */
		private WireEnd portEnd(List<WireEnd> ends,
				Map<String, WireEnd> portEnds, String nodeId, String portName,
				Map<String, Integer> idOf, LayoutResult.Point at,
				int[] nextId) {

			String key = nodeId + "." + portName;
			WireEnd existing = portEnds.get(key);
			if (existing != null) {
				return existing;
			}
			WireEnd end = new WireEnd(nextId[0]++, at);
			end.attachTo = idOf.get(nodeId);
			end.putName = portName;
			ends.add(end);
			portEnds.put(key, end);
			return end;
		} // end of portEnd method

		/**
		 * Links two wire ends into one wire (a symmetric ref pair).
		 *
		 * @param a One end.
		 * @param b The other end.
		 */
		private void link(WireEnd a, WireEnd b) {

			a.wires.add(b);
			b.wires.add(a);
		} // end of link method

		/**
		 * One emitted WireEnd: an id, a coordinate, an optional port
		 * attachment, and its wire neighbours.
		 */
		private static final class WireEnd {

			/** Numeric save id. */
			private final int id;
			/** Grid position. */
			private final LayoutResult.Point at;
			/** Element save id this end attaches to, or -1. */
			private int attachTo = -1;
			/** Attached put name, or null. */
			private String putName;
			/** Neighbour ends (one wire each). */
			private final List<WireEnd> wires = new ArrayList<WireEnd>();

			/**
			 * Creates a wire end.
			 *
			 * @param id The numeric save id.
			 * @param at The grid position.
			 */
			private WireEnd(int id, LayoutResult.Point at) {

				this.id = id;
				this.at = at;
			} // end of constructor

			/**
			 * Emits this end in JLS save format.
			 *
			 * @param out The output buffer.
			 */
			private void save(StringBuilder out) {

				out.append("ELEMENT WireEnd\n")
						.append(" int id ").append(id).append('\n')
						.append(" int x ").append(at.x).append('\n')
						.append(" int y ").append(at.y).append('\n')
						.append(" int width 8\n int height 8\n");
				if (putName != null) {
					out.append(" String put \"").append(putName)
							.append("\"\n")
							.append(" ref attach ").append(attachTo)
							.append('\n');
				}
				for (WireEnd neighbour : wires) {
					out.append(" ref wire ").append(neighbour.id).append('\n');
				}
				out.append("END\n");
			} // end of save method
		} // end of WireEnd class

		/**
		 * A stable string key for a bit vector.
		 *
		 * @param vector The bit vector.
		 *
		 * @return the key.
		 */
		private static String key(int[] vector) {

			return Arrays.toString(vector);
		} // end of key method

		/**
		 * A human description of an endpoint for diagnostics.
		 *
		 * @param endpoint An endpoint.
		 *
		 * @return a human description "type.port" for messages.
		 */
		private static String describe(Endpoint endpoint) {

			return endpoint.elem().saveType + "." + endpoint.port();
		} // end of describe method

	} // end of Builder class

	/**
	 * Whether every bit of a vector is a constant (0/1/x), so it needs a
	 * Constant driver rather than a wire.
	 *
	 * @param vector The bit vector.
	 *
	 * @return true if the vector is entirely constant.
	 */
	private static boolean isAllConstant(int[] vector) {

		for (int bit : vector) {
			if (bit == YosysNetlist.BIT_0 || bit == YosysNetlist.BIT_1
					|| bit == YosysNetlist.BIT_X) {
				continue;
			}
			return false;
		}
		return vector.length > 0;
	} // end of isAllConstant method

	/**
	 * The summary category for a gate save type.
	 *
	 * @param saveType The JLS gate save type.
	 *
	 * @return the Yosys cell type it came from.
	 */
	private static String gateCell(String saveType) {

		switch (saveType) {
		case "NotGate":
			return "$not";
		case "AndGate":
			return "$and";
		case "OrGate":
			return "$or";
		case "XorGate":
			return "$xor";
		default:
			return saveType;
		}
	} // end of gateCell method

	/**
	 * Legalizes a Yosys signal name into a safe JLS pin name: strips a
	 * leading {@code \}, escapes backslashes and quotes for the string
	 * attribute writer, and substitutes a placeholder for an empty name.
	 *
	 * @param name The raw Yosys name.
	 *
	 * @return the legalized, escape-safe name.
	 */
	private static String legalize(String name) {

		String cleaned = name;
		if (cleaned.startsWith("\\")) {
			cleaned = cleaned.substring(1);
		}
		if (cleaned.isEmpty()) {
			cleaned = "net";
		}
		return cleaned.replace("\\", "\\\\").replace("\"", "\\\"");
	} // end of legalize method

} // end of NetlistImporter class
