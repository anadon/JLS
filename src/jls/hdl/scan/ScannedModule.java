package jls.hdl.scan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One Verilog module or VHDL entity found by a header scan (issue
 * #63): its name, its ports in declaration order, and the evaluated
 * integer defaults of its parameters/generics (used both to resolve
 * port widths and to show the user what the file's knobs are).
 * Immutable; produced by {@link VerilogHeaderScanner} and
 * {@link VhdlEntityScanner}.
 */
public final class ScannedModule {

	/** The module/entity name exactly as written in the source file. */
	public final String name;

	/** The ports in declaration order. */
	private final List<ScannedPort> ports;

	/** Evaluated parameter/generic defaults, in declaration order. */
	private final Map<String, Long> parameters;

	/**
	 * Builds an immutable scanned-module descriptor.
	 * @param name the module/entity name exactly as written
	 * @param ports the ports in declaration order (defensively copied)
	 * @param parameters evaluated parameter/generic defaults in
	 *        declaration order (defensively copied)
	 */
	ScannedModule(String name, List<ScannedPort> ports,
			Map<String, Long> parameters) {
		this.name = name;
		this.ports = Collections.unmodifiableList(
				new ArrayList<ScannedPort>(ports));
		this.parameters = Collections.unmodifiableMap(
				new LinkedHashMap<String, Long>(parameters));
	} // end of ScannedModule constructor

	/**
	 * Lists the module's ports.
	 *
	 * @return the ports in declaration order, unmodifiable
	 */
	public List<ScannedPort> ports() {
		return ports;
	} // end of ports method

	/**
	 * Lists the module's parameter/generic defaults.
	 *
	 * @return evaluated parameter/generic defaults in declaration
	 *         order, unmodifiable
	 */
	public Map<String, Long> parameters() {
		return parameters;
	} // end of parameters method

} // end of ScannedModule class
