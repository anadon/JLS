package jls.hdl.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Filling rules of a solved layout (issue #62): placements and routes
 * bind to the result's own graph, are write-once, and the port
 * attachment arithmetic the importer will anchor WireEnds to is
 * position plus fixed offset.
 */
class LayoutResultTest {

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

	@Test
	void placementsAreWriteOnceAndBoundToTheGraph() {
		LayoutGraph graph = twoBuffers();
		LayoutResult result = new LayoutResult(graph);
		assertThrows(IllegalArgumentException.class,
				() -> result.place("ghost", 0, 0));
		result.place("a", 0, 0);
		assertThrows(IllegalStateException.class,
				() -> result.place("a", 12, 12));
		assertEquals(new LayoutResult.Point(0, 0),
				result.position("a"));
		assertNull(result.position("b"));
	}

	@Test
	void routesAreWriteOnceAndBoundToTheGraph() {
		LayoutGraph graph = twoBuffers();
		LayoutGraph.Edge edge = graph.edges().get(0);
		LayoutResult result = new LayoutResult(graph);
		// an equal-shaped edge from a different graph is rejected
		LayoutGraph other = twoBuffers();
		assertThrows(IllegalArgumentException.class,
				() -> result.route(other.edges().get(0), List.of(
						new LayoutResult.Point(24, 12),
						new LayoutResult.Point(48, 12))));
		// a route needs at least two waypoints
		assertThrows(IllegalArgumentException.class,
				() -> result.route(edge, List.of(
						new LayoutResult.Point(24, 12))));
		result.route(edge, List.of(new LayoutResult.Point(24, 12),
				new LayoutResult.Point(48, 12)));
		assertThrows(IllegalStateException.class,
				() -> result.route(edge, List.of(
						new LayoutResult.Point(24, 12),
						new LayoutResult.Point(48, 12))));
		assertEquals(2, result.route(edge).size());
		assertThrows(UnsupportedOperationException.class,
				() -> result.route(edge).clear());
	}

	@Test
	void portLocationIsPositionPlusOffset() {
		LayoutGraph graph = twoBuffers();
		LayoutResult result = new LayoutResult(graph);
		LayoutGraph.Node a = graph.node("a");
		assertThrows(IllegalStateException.class,
				() -> result.portLocation(a, a.port("out")));
		result.place("a", 36, 48);
		assertEquals(new LayoutResult.Point(60, 60),
				result.portLocation(a, a.port("out")));
		assertEquals(new LayoutResult.Point(36, 60),
				result.portLocation(a, a.port("in")));
	}

	@Test
	void completenessNeedsEveryNodePlacedAndEveryEdgeRouted() {
		LayoutGraph graph = twoBuffers();
		LayoutResult result = new LayoutResult(graph);
		assertFalse(result.isComplete());
		result.place("a", 0, 0);
		result.place("b", 48, 0);
		assertFalse(result.isComplete());
		result.route(graph.edges().get(0), List.of(
				new LayoutResult.Point(24, 12),
				new LayoutResult.Point(48, 12)));
		assertTrue(result.isComplete());
	}

} // end of LayoutResultTest class
