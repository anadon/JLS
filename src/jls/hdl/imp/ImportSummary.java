package jls.hdl.imp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The post-import report data for one successful import (issue #61
 * addendum, "post-import report"): how many JLS elements were created,
 * the cell-to-element mapping table with counts, and any two-state
 * coercions applied. {@link NetlistImporter} fills one of these
 * alongside the save text; the File-&gt;Import UI (not built in this
 * increment) renders it as the one-shot summary dialog.
 */
public final class ImportSummary {

	/** The imported top module's name. */
	private final String moduleName;

	/** Total JLS elements created (excluding wires). */
	private int elementCount;

	/** Yosys cell type (or "port"/"constant") to element count. */
	private final LinkedHashMap<String, Integer> mapping =
			new LinkedHashMap<String, Integer>();

	/** Count of {@code x} bits coerced to 0 (issue #61 decision). */
	private int coercedX;

	/**
	 * Creates an empty summary for one module.
	 *
	 * @param moduleName The imported module's name.
	 */
	ImportSummary(String moduleName) {

		this.moduleName = moduleName;
	} // end of constructor

	/**
	 * Records one element created for a given source category.
	 *
	 * @param category The Yosys cell type, or "port" / "constant".
	 */
	void countElement(String category) {

		elementCount += 1;
		Integer prior = mapping.get(category);
		mapping.put(category, prior == null ? 1 : prior + 1);
	} // end of countElement method

	/**
	 * Records that {@code count} {@code x} bits were coerced to 0.
	 *
	 * @param count How many bits were coerced.
	 */
	void countCoercedX(int count) {

		coercedX += count;
	} // end of countCoercedX method

	/**
	 * The imported module's name.
	 *
	 * @return the module name.
	 */
	public String moduleName() {

		return moduleName;
	} // end of moduleName method

	/**
	 * The total number of JLS elements created (wires excluded).
	 *
	 * @return the element count.
	 */
	public int elementCount() {

		return elementCount;
	} // end of elementCount method

	/**
	 * The mapping table: source category to number of elements created.
	 *
	 * @return an unmodifiable, insertion-ordered map.
	 */
	public Map<String, Integer> mapping() {

		return Collections.unmodifiableMap(mapping);
	} // end of mapping method

	/**
	 * How many {@code x} bits were coerced to constant 0.
	 *
	 * @return the coercion count.
	 */
	public int coercedX() {

		return coercedX;
	} // end of coercedX method

} // end of ImportSummary class
