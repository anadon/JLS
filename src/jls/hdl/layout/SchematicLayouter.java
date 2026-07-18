package jls.hdl.layout;

/**
 * Places and routes one imported netlist (issue #62). This is the
 * fixed seam between the Stage 2 importer (issue #61), which builds
 * the {@link LayoutGraph} and realizes the {@link LayoutResult} as
 * placed elements and WireEnd chains, and whichever layout engine
 * solves the geometry - per the 2026-07-17 adjudication, first an
 * out-of-process ELK Layered runner, later possibly in-process ELK if
 * its GPLv3 secondary license ships. Engines are swappable without
 * importer changes; that swappability is prediction P3 of the issue.
 */
public interface SchematicLayouter {

	/**
	 * Solves the layout: places every node and routes every edge.
	 * The returned result must be {@link LayoutResult#isComplete()
	 * complete}, must pass {@link LayoutInvariants#check} (12-px grid,
	 * routes anchored at exact port offsets, orthogonal segments,
	 * non-overlapping element bodies), and must be deterministic:
	 * same graph, same result.
	 *
	 * @param graph The placement problem.
	 *
	 * @return the solved layout.
	 *
	 * @throws LayoutException if the engine fails or produces output
	 *		that violates the contract above.
	 */
	LayoutResult layout(LayoutGraph graph) throws LayoutException;

} // end of SchematicLayouter interface
