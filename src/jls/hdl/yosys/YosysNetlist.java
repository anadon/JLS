package jls.hdl.yosys;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A parsed Yosys {@code write_json} netlist (issue #61): the modules,
 * ports, cells, and net names of a design after the restricted pass
 * pipeline has run. This is the importer's input model — everything
 * downstream (the cell validator, the cell-to-element mapper, the
 * layout pass) reads the design through this class and never touches
 * JSON again.
 *
 * The {@code write_json} schema represents every connection as a
 * vector of bits, where each bit is either a nonnegative net number
 * or one of the constant strings {@code "0"}, {@code "1"},
 * {@code "x"}, {@code "z"}. Here a bit is an {@code int}: net numbers
 * are kept as-is (Yosys never emits negative ones; the parser
 * enforces that) and the four constants become the negative
 * sentinels {@link #BIT_0}, {@link #BIT_1}, {@link #BIT_X} and
 * {@link #BIT_Z}. Per the resolved issue-#61 decisions, the mapper
 * later coerces {@code x} to 0 and {@code z} to a disabled TriState
 * (JLS is two-state); this class just preserves what Yosys said.
 *
 * Unrecognized members (schema additions from newer Yosys versions,
 * such as per-module {@code "memories"} sections) are ignored rather
 * than rejected, so a Yosys upgrade degrades to a validator decision
 * instead of a parse failure.
 */
public final class YosysNetlist {

	/** The constant-0 bit sentinel (JSON {@code "0"}). */
	public static final int BIT_0 = -1;

	/** The constant-1 bit sentinel (JSON {@code "1"}). */
	public static final int BIT_1 = -2;

	/** The undefined-bit sentinel (JSON {@code "x"}). */
	public static final int BIT_X = -3;

	/** The high-impedance bit sentinel (JSON {@code "z"}). */
	public static final int BIT_Z = -4;

	/** The creator banner Yosys wrote, or "" when absent. */
	private final String creator;

	/** The modules of the design, in document order. */
	private final LinkedHashMap<String, Module> modules;

	/**
	 * Creates the netlist; instances only come from {@link #parse}.
	 *
	 * @param creator The creator banner, or "".
	 * @param modules The modules, in document order.
	 */
	private YosysNetlist(String creator,
			LinkedHashMap<String, Module> modules) {

		this.creator = creator;
		this.modules = modules;
	} // end of constructor

	/**
	 * Parses the text of a {@code write_json} file.
	 *
	 * @param jsonText The full file text.
	 *
	 * @return the parsed netlist.
	 *
	 * @throws NetlistFormatException if the text is not well-formed
	 * JSON or does not match the {@code write_json} schema.
	 */
	public static YosysNetlist parse(String jsonText)
			throws NetlistFormatException {

		JsonValue root = JsonValue.parse(jsonText);
		if (!root.isObject()) {
			throw new NetlistFormatException(
					"netlist root is not a JSON object");
		}
		Map<String, JsonValue> rootMembers = root.asObject();
		String creator = "";
		JsonValue creatorValue = rootMembers.get("creator");
		if (creatorValue != null && creatorValue.isString()) {
			creator = creatorValue.asString();
		}
		JsonValue modulesValue = rootMembers.get("modules");
		if (modulesValue == null || !modulesValue.isObject()) {
			throw new NetlistFormatException(
					"netlist has no \"modules\" object");
		}
		LinkedHashMap<String, Module> modules =
				new LinkedHashMap<String, Module>();
		for (Map.Entry<String, JsonValue> entry
				: modulesValue.asObject().entrySet()) {
			modules.put(entry.getKey(), Module.parse(entry.getKey(),
					entry.getValue()));
		}
		return new YosysNetlist(creator, modules);
	} // end of parse method

	/**
	 * The creator banner Yosys wrote into the file.
	 *
	 * @return the banner, or "" when the file had none.
	 */
	public String creator() {

		return creator;
	} // end of creator method

	/**
	 * The modules of the design.
	 *
	 * @return an unmodifiable name-to-module map, in document order.
	 */
	public Map<String, Module> modules() {

		return Collections.unmodifiableMap(modules);
	} // end of modules method

	/** The direction of a module port or cell port. */
	public enum PortDirection {

		/** Driven from outside the module or cell. */
		INPUT,

		/** Driven by the module or cell. */
		OUTPUT,

		/** Driven from both sides (tri-state buses). */
		INOUT;

		/**
		 * Parses a schema direction string.
		 *
		 * @param text The direction text from the file.
		 * @param where The path context for the error message.
		 *
		 * @return the direction.
		 *
		 * @throws NetlistFormatException if the text is not one of
		 * "input", "output", or "inout".
		 */
		static PortDirection parse(String text, String where)
				throws NetlistFormatException {

			if (text.equals("input")) {
				return INPUT;
			}
			if (text.equals("output")) {
				return OUTPUT;
			}
			if (text.equals("inout")) {
				return INOUT;
			}
			throw new NetlistFormatException(where
					+ ": unknown port direction \"" + text + "\"");
		} // end of parse method

	} // end of PortDirection enum

	/** One module: its ports, cells, and named nets. */
	public static final class Module {

		/** The module's name (the key in the modules object). */
		private final String name;

		/** The module's attributes, canonicalized to strings. */
		private final LinkedHashMap<String, String> attributes;

		/** The module's ports, in document order. */
		private final LinkedHashMap<String, Port> ports;

		/** The module's cells, in document order. */
		private final LinkedHashMap<String, Cell> cells;

		/** The module's named nets, in document order. */
		private final LinkedHashMap<String, NetName> netNames;

		/**
		 * Creates a module; instances only come from {@link #parse}.
		 *
		 * @param name The module name.
		 * @param attributes The canonicalized attributes.
		 * @param ports The ports, in document order.
		 * @param cells The cells, in document order.
		 * @param netNames The named nets, in document order.
		 */
		private Module(String name,
				LinkedHashMap<String, String> attributes,
				LinkedHashMap<String, Port> ports,
				LinkedHashMap<String, Cell> cells,
				LinkedHashMap<String, NetName> netNames) {

			this.name = name;
			this.attributes = attributes;
			this.ports = ports;
			this.cells = cells;
			this.netNames = netNames;
		} // end of constructor

		/**
		 * Parses one module body.
		 *
		 * @param name The module name.
		 * @param body The module's JSON value.
		 *
		 * @return the parsed module.
		 *
		 * @throws NetlistFormatException if the body does not match
		 * the schema.
		 */
		static Module parse(String name, JsonValue body)
				throws NetlistFormatException {

			String where = "module \"" + name + "\"";
			if (!body.isObject()) {
				throw new NetlistFormatException(where
						+ ": body is not a JSON object");
			}
			Map<String, JsonValue> members = body.asObject();
			LinkedHashMap<String, String> attributes =
					parseAttributes(members.get("attributes"), where);
			LinkedHashMap<String, Port> ports =
					new LinkedHashMap<String, Port>();
			JsonValue portsValue = members.get("ports");
			if (portsValue != null) {
				if (!portsValue.isObject()) {
					throw new NetlistFormatException(where
							+ ": \"ports\" is not a JSON object");
				}
				for (Map.Entry<String, JsonValue> entry
						: portsValue.asObject().entrySet()) {
					ports.put(entry.getKey(), Port.parse(
							entry.getKey(), entry.getValue(), where));
				}
			}
			LinkedHashMap<String, Cell> cells =
					new LinkedHashMap<String, Cell>();
			JsonValue cellsValue = members.get("cells");
			if (cellsValue != null) {
				if (!cellsValue.isObject()) {
					throw new NetlistFormatException(where
							+ ": \"cells\" is not a JSON object");
				}
				for (Map.Entry<String, JsonValue> entry
						: cellsValue.asObject().entrySet()) {
					cells.put(entry.getKey(), Cell.parse(
							entry.getKey(), entry.getValue(), where));
				}
			}
			LinkedHashMap<String, NetName> netNames =
					new LinkedHashMap<String, NetName>();
			JsonValue netsValue = members.get("netnames");
			if (netsValue != null) {
				if (!netsValue.isObject()) {
					throw new NetlistFormatException(where
							+ ": \"netnames\" is not a JSON object");
				}
				for (Map.Entry<String, JsonValue> entry
						: netsValue.asObject().entrySet()) {
					netNames.put(entry.getKey(), NetName.parse(
							entry.getKey(), entry.getValue(), where));
				}
			}
			return new Module(name, attributes, ports, cells,
					netNames);
		} // end of parse method

		/**
		 * The module's name.
		 *
		 * @return the name.
		 */
		public String name() {

			return name;
		} // end of name method

		/**
		 * The module's attributes ({@code src}, {@code top}, ...),
		 * with number values canonicalized to decimal strings.
		 *
		 * @return an unmodifiable attribute map.
		 */
		public Map<String, String> attributes() {

			return Collections.unmodifiableMap(attributes);
		} // end of attributes method

		/**
		 * The module's ports.
		 *
		 * @return an unmodifiable name-to-port map, in document
		 * order.
		 */
		public Map<String, Port> ports() {

			return Collections.unmodifiableMap(ports);
		} // end of ports method

		/**
		 * The module's cells.
		 *
		 * @return an unmodifiable name-to-cell map, in document
		 * order.
		 */
		public Map<String, Cell> cells() {

			return Collections.unmodifiableMap(cells);
		} // end of cells method

		/**
		 * The module's named nets.
		 *
		 * @return an unmodifiable name-to-net map, in document
		 * order.
		 */
		public Map<String, NetName> netNames() {

			return Collections.unmodifiableMap(netNames);
		} // end of netNames method

	} // end of Module class

	/** One module port: a direction and a bit vector. */
	public static final class Port {

		/** The port's name (the key in the ports object). */
		private final String name;

		/** The port's direction. */
		private final PortDirection direction;

		/** The port's bits, least significant first. */
		private final int[] bits;

		/**
		 * Creates a port; instances only come from {@link #parse}.
		 *
		 * @param name The port name.
		 * @param direction The port direction.
		 * @param bits The bit vector.
		 */
		private Port(String name, PortDirection direction,
				int[] bits) {

			this.name = name;
			this.direction = direction;
			this.bits = bits;
		} // end of constructor

		/**
		 * Parses one port body.
		 *
		 * @param name The port name.
		 * @param body The port's JSON value.
		 * @param moduleWhere The enclosing module's path context.
		 *
		 * @return the parsed port.
		 *
		 * @throws NetlistFormatException if the body does not match
		 * the schema.
		 */
		static Port parse(String name, JsonValue body,
				String moduleWhere) throws NetlistFormatException {

			String where = moduleWhere + ", port \"" + name + "\"";
			if (!body.isObject()) {
				throw new NetlistFormatException(where
						+ ": body is not a JSON object");
			}
			Map<String, JsonValue> members = body.asObject();
			JsonValue directionValue = members.get("direction");
			if (directionValue == null || !directionValue.isString()) {
				throw new NetlistFormatException(where
						+ ": missing \"direction\"");
			}
			PortDirection direction = PortDirection.parse(
					directionValue.asString(), where);
			int[] bits = parseBits(members.get("bits"), where);
			return new Port(name, direction, bits);
		} // end of parse method

		/**
		 * The port's name.
		 *
		 * @return the name.
		 */
		public String name() {

			return name;
		} // end of name method

		/**
		 * The port's direction.
		 *
		 * @return the direction.
		 */
		public PortDirection direction() {

			return direction;
		} // end of direction method

		/**
		 * The port's bit vector, least significant bit first: net
		 * numbers, or the negative {@code BIT_*} sentinels.
		 *
		 * @return a fresh copy of the bit vector.
		 */
		public int[] bits() {

			return bits.clone();
		} // end of bits method

	} // end of Port class

	/** One cell: a typed instance with parameters and connections. */
	public static final class Cell {

		/** The cell's name (the key in the cells object). */
		private final String name;

		/** The cell's type ({@code $dff}, a module name, ...). */
		private final String type;

		/** Whether Yosys marked the name as auto-generated. */
		private final boolean hideName;

		/** The cell's attributes, canonicalized to strings. */
		private final LinkedHashMap<String, String> attributes;

		/** The cell's parameters, as raw JSON values. */
		private final LinkedHashMap<String, JsonValue> parameters;

		/** The direction of each connected port. */
		private final LinkedHashMap<String, PortDirection> portDirections;

		/** The bit vector connected to each port. */
		private final LinkedHashMap<String, int[]> connections;

		/**
		 * Creates a cell; instances only come from {@link #parse}.
		 *
		 * @param name The cell name.
		 * @param type The cell type.
		 * @param hideName Whether the name is auto-generated.
		 * @param attributes The canonicalized attributes.
		 * @param parameters The raw parameters.
		 * @param portDirections The port directions.
		 * @param connections The port connections.
		 */
		private Cell(String name, String type, boolean hideName,
				LinkedHashMap<String, String> attributes,
				LinkedHashMap<String, JsonValue> parameters,
				LinkedHashMap<String, PortDirection> portDirections,
				LinkedHashMap<String, int[]> connections) {

			this.name = name;
			this.type = type;
			this.hideName = hideName;
			this.attributes = attributes;
			this.parameters = parameters;
			this.portDirections = portDirections;
			this.connections = connections;
		} // end of constructor

		/**
		 * Parses one cell body.
		 *
		 * @param name The cell name.
		 * @param body The cell's JSON value.
		 * @param moduleWhere The enclosing module's path context.
		 *
		 * @return the parsed cell.
		 *
		 * @throws NetlistFormatException if the body does not match
		 * the schema.
		 */
		static Cell parse(String name, JsonValue body,
				String moduleWhere) throws NetlistFormatException {

			String where = moduleWhere + ", cell \"" + name + "\"";
			if (!body.isObject()) {
				throw new NetlistFormatException(where
						+ ": body is not a JSON object");
			}
			Map<String, JsonValue> members = body.asObject();
			JsonValue typeValue = members.get("type");
			if (typeValue == null || !typeValue.isString()) {
				throw new NetlistFormatException(where
						+ ": missing \"type\"");
			}
			boolean hideName = false;
			JsonValue hideValue = members.get("hide_name");
			if (hideValue != null && hideValue.isNumber()) {
				hideName = hideValue.asLong() != 0;
			}
			LinkedHashMap<String, String> attributes =
					parseAttributes(members.get("attributes"), where);
			LinkedHashMap<String, JsonValue> parameters =
					new LinkedHashMap<String, JsonValue>();
			JsonValue parametersValue = members.get("parameters");
			if (parametersValue != null) {
				if (!parametersValue.isObject()) {
					throw new NetlistFormatException(where
							+ ": \"parameters\" is not a JSON object");
				}
				parameters.putAll(parametersValue.asObject());
			}
			LinkedHashMap<String, PortDirection> portDirections =
					new LinkedHashMap<String, PortDirection>();
			JsonValue directionsValue = members.get("port_directions");
			if (directionsValue != null) {
				if (!directionsValue.isObject()) {
					throw new NetlistFormatException(where
							+ ": \"port_directions\" is not a JSON"
							+ " object");
				}
				for (Map.Entry<String, JsonValue> entry
						: directionsValue.asObject().entrySet()) {
					String portWhere = where + ", port \""
							+ entry.getKey() + "\"";
					if (!entry.getValue().isString()) {
						throw new NetlistFormatException(portWhere
								+ ": direction is not a string");
					}
					portDirections.put(entry.getKey(),
							PortDirection.parse(
									entry.getValue().asString(),
									portWhere));
				}
			}
			LinkedHashMap<String, int[]> connections =
					new LinkedHashMap<String, int[]>();
			JsonValue connectionsValue = members.get("connections");
			if (connectionsValue != null) {
				if (!connectionsValue.isObject()) {
					throw new NetlistFormatException(where
							+ ": \"connections\" is not a JSON"
							+ " object");
				}
				for (Map.Entry<String, JsonValue> entry
						: connectionsValue.asObject().entrySet()) {
					connections.put(entry.getKey(), parseBits(
							entry.getValue(), where + ", port \""
									+ entry.getKey() + "\""));
				}
			}
			return new Cell(name, typeValue.asString(), hideName,
					attributes, parameters, portDirections,
					connections);
		} // end of parse method

		/**
		 * The cell's name.
		 *
		 * @return the name.
		 */
		public String name() {

			return name;
		} // end of name method

		/**
		 * The cell's type: a {@code $}-prefixed internal cell name,
		 * or a user module name for a hierarchy instance.
		 *
		 * @return the type.
		 */
		public String type() {

			return type;
		} // end of type method

		/**
		 * Whether Yosys marked the cell name as auto-generated
		 * (its {@code hide_name} flag).
		 *
		 * @return true when the name is auto-generated.
		 */
		public boolean hideName() {

			return hideName;
		} // end of hideName method

		/**
		 * The cell's attributes, with number values canonicalized to
		 * decimal strings.
		 *
		 * @return an unmodifiable attribute map.
		 */
		public Map<String, String> attributes() {

			return Collections.unmodifiableMap(attributes);
		} // end of attributes method

		/**
		 * The Verilog source location Yosys recorded for this cell
		 * (its {@code src} attribute, e.g. {@code "counter.v:8.3-10.6"}),
		 * for pointing error messages at the user's own code.
		 *
		 * @return the location, or "" when Yosys recorded none.
		 */
		public String sourceLocation() {

			String src = attributes.get("src");
			return src == null ? "" : src;
		} // end of sourceLocation method

		/**
		 * The cell's parameters as raw JSON values; use
		 * {@link #param(String)} for numeric ones.
		 *
		 * @return an unmodifiable parameter map.
		 */
		public Map<String, JsonValue> parameters() {

			return Collections.unmodifiableMap(parameters);
		} // end of parameters method

		/**
		 * The direction of each connected port.
		 *
		 * @return an unmodifiable port-to-direction map.
		 */
		public Map<String, PortDirection> portDirections() {

			return Collections.unmodifiableMap(portDirections);
		} // end of portDirections method

		/**
		 * The bit vector connected to each port; entries follow the
		 * {@link Port#bits()} encoding. The arrays are the live ones,
		 * shared with this cell — do not modify them.
		 *
		 * @return an unmodifiable port-to-bits map.
		 */
		public Map<String, int[]> connections() {

			return Collections.unmodifiableMap(connections);
		} // end of connections method

		/**
		 * A numeric parameter's value. {@code write_json} emits
		 * numeric parameters either as JSON numbers or as fixed-width
		 * binary digit strings (with {@code x}/{@code z} digits read
		 * as 0, matching the two-state coercion the whole importer
		 * applies); both forms are accepted.
		 *
		 * @param name The parameter name (e.g. {@code "WIDTH"}).
		 *
		 * @return the decoded value.
		 *
		 * @throws NetlistFormatException if the parameter is absent
		 * or is not numeric (a string parameter, or a binary string
		 * wider than 63 significant bits).
		 */
		public long param(String name) throws NetlistFormatException {

			JsonValue raw = parameters.get(name);
			if (raw == null) {
				throw new NetlistFormatException("cell \"" + name()
						+ "\" (" + type + "): missing parameter \""
						+ name + "\"");
			}
			return decodeParam(name, raw);
		} // end of param method

		/**
		 * A numeric parameter's value, with a default for absence.
		 *
		 * @param name The parameter name.
		 * @param absent The value to return when the cell does not
		 * carry the parameter.
		 *
		 * @return the decoded value, or {@code absent}.
		 *
		 * @throws NetlistFormatException if the parameter is present
		 * but not numeric.
		 */
		public long param(String name, long absent)
				throws NetlistFormatException {

			JsonValue raw = parameters.get(name);
			if (raw == null) {
				return absent;
			}
			return decodeParam(name, raw);
		} // end of param method

		/**
		 * Decodes one parameter value (see {@link #param(String)}).
		 *
		 * @param paramName The parameter name, for error messages.
		 * @param raw The raw JSON value.
		 *
		 * @return the decoded value.
		 *
		 * @throws NetlistFormatException if the value is not numeric.
		 */
		private long decodeParam(String paramName, JsonValue raw)
				throws NetlistFormatException {

			String where = "cell \"" + name + "\" (" + type
					+ "), parameter \"" + paramName + "\"";
			if (raw.isNumber()) {
				return raw.asLong();
			}
			if (!raw.isString()) {
				throw new NetlistFormatException(where
						+ ": not a number or digit string");
			}
			String digits = raw.asString();
			// write_json appends one space to a *string-valued*
			// parameter that happens to look like binary digits;
			// such a parameter is genuinely non-numeric here
			if (digits.isEmpty() || digits.endsWith(" ")) {
				throw new NetlistFormatException(where
						+ ": string parameter where a number was"
						+ " expected");
			}
			long value = 0;
			int significant = 0;
			for (int i = 0; i < digits.length(); i += 1) {
				char c = digits.charAt(i);
				int bit;
				if (c == '1') {
					bit = 1;
				}
				else if (c == '0' || c == 'x' || c == 'z') {
					bit = 0;
				}
				else {
					throw new NetlistFormatException(where
							+ ": non-binary digit '" + c + "'");
				}
				if (significant > 0 || bit != 0) {
					significant += 1;
				}
				if (significant > 63) {
					throw new NetlistFormatException(where
							+ ": value wider than 63 bits");
				}
				value = (value << 1) | bit;
			}
			return value;
		} // end of decodeParam method

	} // end of Cell class

	/** One named net: a user-visible name for a bit vector. */
	public static final class NetName {

		/** The net's name (the key in the netnames object). */
		private final String name;

		/** Whether Yosys marked the name as auto-generated. */
		private final boolean hideName;

		/** The net's bits, least significant first. */
		private final int[] bits;

		/**
		 * Creates a net name; instances only come from
		 * {@link #parse}.
		 *
		 * @param name The net name.
		 * @param hideName Whether the name is auto-generated.
		 * @param bits The bit vector.
		 */
		private NetName(String name, boolean hideName, int[] bits) {

			this.name = name;
			this.hideName = hideName;
			this.bits = bits;
		} // end of constructor

		/**
		 * Parses one net-name body.
		 *
		 * @param name The net name.
		 * @param body The net's JSON value.
		 * @param moduleWhere The enclosing module's path context.
		 *
		 * @return the parsed net name.
		 *
		 * @throws NetlistFormatException if the body does not match
		 * the schema.
		 */
		static NetName parse(String name, JsonValue body,
				String moduleWhere) throws NetlistFormatException {

			String where = moduleWhere + ", net \"" + name + "\"";
			if (!body.isObject()) {
				throw new NetlistFormatException(where
						+ ": body is not a JSON object");
			}
			Map<String, JsonValue> members = body.asObject();
			boolean hideName = false;
			JsonValue hideValue = members.get("hide_name");
			if (hideValue != null && hideValue.isNumber()) {
				hideName = hideValue.asLong() != 0;
			}
			int[] bits = parseBits(members.get("bits"), where);
			return new NetName(name, hideName, bits);
		} // end of parse method

		/**
		 * The net's name.
		 *
		 * @return the name.
		 */
		public String name() {

			return name;
		} // end of name method

		/**
		 * Whether Yosys marked the net name as auto-generated
		 * (its {@code hide_name} flag).
		 *
		 * @return true when the name is auto-generated.
		 */
		public boolean hideName() {

			return hideName;
		} // end of hideName method

		/**
		 * The net's bit vector, in the {@link Port#bits()} encoding.
		 *
		 * @return a fresh copy of the bit vector.
		 */
		public int[] bits() {

			return bits.clone();
		} // end of bits method

	} // end of NetName class

	/**
	 * Parses a schema bit vector: an array whose entries are
	 * nonnegative net numbers or the constant strings "0", "1", "x",
	 * "z".
	 *
	 * @param value The JSON value holding the vector.
	 * @param where The path context for error messages.
	 *
	 * @return the encoded bit vector.
	 *
	 * @throws NetlistFormatException if the vector is missing or any
	 * entry is malformed.
	 */
	private static int[] parseBits(JsonValue value, String where)
			throws NetlistFormatException {

		if (value == null || !value.isArray()) {
			throw new NetlistFormatException(where
					+ ": missing \"bits\" array");
		}
		List<JsonValue> entries = value.asArray();
		int[] bits = new int[entries.size()];
		for (int i = 0; i < bits.length; i += 1) {
			JsonValue entry = entries.get(i);
			if (entry.isNumber()) {
				long net = entry.asLong();
				if (net < 0 || net > Integer.MAX_VALUE) {
					throw new NetlistFormatException(where
							+ ": bit " + i + " has net number "
							+ net + " outside the valid range");
				}
				bits[i] = (int) net;
			}
			else if (entry.isString()) {
				String text = entry.asString();
				if (text.equals("0")) {
					bits[i] = BIT_0;
				}
				else if (text.equals("1")) {
					bits[i] = BIT_1;
				}
				else if (text.equals("x") || text.equals("X")) {
					bits[i] = BIT_X;
				}
				else if (text.equals("z") || text.equals("Z")) {
					bits[i] = BIT_Z;
				}
				else {
					throw new NetlistFormatException(where
							+ ": bit " + i + " is \"" + text
							+ "\", not a net number or 0/1/x/z");
				}
			}
			else {
				throw new NetlistFormatException(where + ": bit "
						+ i + " is not a net number or constant");
			}
		}
		return bits;
	} // end of parseBits method

	/**
	 * Parses an attributes object into a string map: string values
	 * kept as-is, number values canonicalized to decimal.
	 *
	 * @param value The JSON value holding the attributes, or null.
	 * @param where The path context for error messages.
	 *
	 * @return the attribute map (empty when absent).
	 *
	 * @throws NetlistFormatException if the attributes are not an
	 * object of strings and numbers.
	 */
	private static LinkedHashMap<String, String> parseAttributes(
			JsonValue value, String where)
			throws NetlistFormatException {

		LinkedHashMap<String, String> attributes =
				new LinkedHashMap<String, String>();
		if (value == null) {
			return attributes;
		}
		if (!value.isObject()) {
			throw new NetlistFormatException(where
					+ ": \"attributes\" is not a JSON object");
		}
		for (Map.Entry<String, JsonValue> entry
				: value.asObject().entrySet()) {
			JsonValue attribute = entry.getValue();
			if (attribute.isString()) {
				attributes.put(entry.getKey(), attribute.asString());
			}
			else if (attribute.isNumber()) {
				attributes.put(entry.getKey(),
						Long.toString(attribute.asLong()));
			}
			else {
				throw new NetlistFormatException(where
						+ ": attribute \"" + entry.getKey()
						+ "\" is not a string or number");
			}
		}
		return attributes;
	} // end of parseAttributes method

} // end of YosysNetlist class
