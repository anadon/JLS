package jls.hdl.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The drawing invariants an engine's output must satisfy before the
 * importer realizes it (issue #62 §2): 12-px grid, routes anchored at
 * exact port attachment points, orthogonal nonzero segments, and
 * non-overlapping element bodies. Each test breaks exactly one
 * invariant and expects it named.
 */
class LayoutInvariantsTest {

	/** A 24x24 element with one input at (0,12), one output at (24,12). */
	private static LayoutGraph.Node buffer(String id) {
		return new LayoutGraph.Node(id, "Buffer", 24, 24, List.of(
				new LayoutGraph.Port("in",
						LayoutGraph.Direction.INPUT, 0, 12),
				new LayoutGraph.Port("out",
						LayoutGraph.Direction.OUTPUT, 24, 12)));
	}

	/** Two buffers a→b on net n1. */
	private static LayoutGraph twoBuffers() {
		LayoutGraph graph = new LayoutGraph();
		graph.add(buffer("a"));
		graph.add(buffer("b"));
		graph.connect("n1", "a", "out", "b", "in", false);
		return graph;
	}

	/** a at (0,0), b at (48,0), straight wire (24,12)→(48,12). */
	private static LayoutResult cleanLayout(LayoutGraph graph) {
		LayoutResult result = new LayoutResult(graph);
		result.place("a", 0, 0);
		result.place("b", 48, 0);
		result.route(graph.edges().get(0), List.of(
				new LayoutResult.Point(24, 12),
				new LayoutResult.Point(48, 12)));
		return result;
	}

	@Test
	void aCleanLayoutHasNoViolations() {
		LayoutResult result = cleanLayout(twoBuffers());
		assertEquals(List.of(), LayoutInvariants.check(result));
	}

	@Test
	void missingPlacementsAndRoutesAreViolations() {
		LayoutGraph graph = twoBuffers();
		LayoutResult result = new LayoutResult(graph);
		result.place("a", 0, 0);
		List<String> violations = LayoutInvariants.check(result);
		assertTrue(violations.stream()
						.anyMatch(v -> v.contains("b")
								&& v.contains("not placed")),
				violations.toString());
		assertTrue(violations.stream()
						.anyMatch(v -> v.contains("n1")
								&& v.contains("missing")),
				violations.toString());
	}

	@Test
	void offGridPlacementIsAViolation() {
		LayoutGraph graph = twoBuffers();
		LayoutResult result = new LayoutResult(graph);
		result.place("a", 5, 0);		// not a multiple of 12
		result.place("b", 48, 0);
		result.route(graph.edges().get(0), List.of(
				new LayoutResult.Point(29, 12),
				new LayoutResult.Point(48, 12)));
		List<String> violations = LayoutInvariants.check(result);
		assertTrue(violations.stream()
						.anyMatch(v -> v.contains("a")
								&& v.contains("grid")),
				violations.toString());
	}

	@Test
	void offCanvasPlacementIsAViolation() {
		LayoutGraph graph = twoBuffers();
		LayoutResult result = new LayoutResult(graph);
		result.place("a", 0, -24);
		result.place("b", 48, 0);
		result.route(graph.edges().get(0), List.of(
				new LayoutResult.Point(24, -12),
				new LayoutResult.Point(48, -12),
				new LayoutResult.Point(48, 12)));
		List<String> violations = LayoutInvariants.check(result);
		assertTrue(violations.stream()
						.anyMatch(v -> v.contains("off-canvas")),
				violations.toString());
	}

	@Test
	void routeMustAnchorAtExactPortAttachmentPoints() {
		LayoutGraph graph = twoBuffers();
		LayoutResult result = new LayoutResult(graph);
		result.place("a", 0, 0);
		result.place("b", 48, 0);
		// starts one grid unit below the source port
		result.route(graph.edges().get(0), List.of(
				new LayoutResult.Point(24, 24),
				new LayoutResult.Point(48, 24),
				new LayoutResult.Point(48, 12)));
		List<String> violations = LayoutInvariants.check(result);
		assertTrue(violations.stream()
						.anyMatch(v -> v.contains("attaches at (24,12)")),
				violations.toString());
	}

	@Test
	void nonOrthogonalAndZeroLengthSegmentsAreViolations() {
		LayoutGraph graph = twoBuffers();
		LayoutResult result = new LayoutResult(graph);
		result.place("a", 0, 0);
		result.place("b", 48, 24);
		// diagonal straight to the target port, with a stutter first
		result.route(graph.edges().get(0), List.of(
				new LayoutResult.Point(24, 12),
				new LayoutResult.Point(24, 12),
				new LayoutResult.Point(48, 36)));
		List<String> violations = LayoutInvariants.check(result);
		assertTrue(violations.stream()
						.anyMatch(v -> v.contains("zero-length")),
				violations.toString());
		assertTrue(violations.stream()
						.anyMatch(v -> v.contains("non-orthogonal")),
				violations.toString());
	}

	@Test
	void overlappingElementBodiesAreAViolation() {
		LayoutGraph graph = twoBuffers();
		LayoutResult result = new LayoutResult(graph);
		result.place("a", 0, 0);
		result.place("b", 12, 12);		// overlaps a's body
		result.route(graph.edges().get(0), List.of(
				new LayoutResult.Point(24, 12),
				new LayoutResult.Point(12, 12),
				new LayoutResult.Point(12, 24)));
		List<String> violations = LayoutInvariants.check(result);
		assertTrue(violations.stream()
						.anyMatch(v -> v.contains("bodies")
								&& v.contains("overlap")),
				violations.toString());
	}

	@Test
	void touchingElementBodiesAreNotAnOverlap() {
		LayoutGraph graph = twoBuffers();
		LayoutResult result = new LayoutResult(graph);
		result.place("a", 0, 0);
		result.place("b", 24, 0);		// flush against a, not inside
		result.route(graph.edges().get(0), List.of(
				new LayoutResult.Point(24, 12),
				new LayoutResult.Point(24, 12)));
		List<String> violations = LayoutInvariants.check(result);
		assertTrue(violations.stream()
						.noneMatch(v -> v.contains("bodies")),
				violations.toString());
	}

} // end of LayoutInvariantsTest class
