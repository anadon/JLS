package jls.hdl.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Construction rules of the placement problem (issue #62): ids and
 * port names are unique, edges resolve to real ports with the right
 * directions at build time - so a layout engine never sees a dangling
 * or backwards connection - and the collections handed out are
 * unmodifiable.
 */
class LayoutGraphTest {

	/** A 24x24 element with one input at (0,12), one output at (24,12). */
	private static LayoutGraph.Node buffer(String id) {
		return new LayoutGraph.Node(id, "Buffer", 24, 24, List.of(
				new LayoutGraph.Port("in",
						LayoutGraph.Direction.INPUT, 0, 12),
				new LayoutGraph.Port("out",
						LayoutGraph.Direction.OUTPUT, 24, 12)));
	}

	@Test
	void duplicateNodeIdsAreRejected() {
		LayoutGraph graph = new LayoutGraph();
		graph.add(buffer("a"));
		IllegalArgumentException thrown = assertThrows(
				IllegalArgumentException.class,
				() -> graph.add(buffer("a")));
		assertTrue(thrown.getMessage().contains("a"),
				thrown.getMessage());
	}

	@Test
	void duplicatePortNamesWithinANodeAreRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> new LayoutGraph.Node("n", "Gate", 24, 24, List.of(
						new LayoutGraph.Port("p",
								LayoutGraph.Direction.INPUT, 0, 12),
						new LayoutGraph.Port("p",
								LayoutGraph.Direction.OUTPUT, 24, 12))));
	}

	@Test
	void nonPositiveSizesAreRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> new LayoutGraph.Node("n", "Gate", 0, 24,
						List.of()));
		assertThrows(IllegalArgumentException.class,
				() -> new LayoutGraph.Node("n", "Gate", 24, -12,
						List.of()));
	}

	@Test
	void edgesResolveBothEndpointsAtBuildTime() {
		LayoutGraph graph = new LayoutGraph();
		graph.add(buffer("a"));
		graph.add(buffer("b"));
		// unknown node
		assertThrows(IllegalArgumentException.class,
				() -> graph.connect("n1", "ghost", "out", "b", "in",
						false));
		// unknown port
		assertThrows(IllegalArgumentException.class,
				() -> graph.connect("n1", "a", "q", "b", "in", false));
		// resolved edge carries the real objects
		LayoutGraph.Edge edge =
				graph.connect("n1", "a", "out", "b", "in", false);
		assertEquals("n1", edge.netName);
		assertEquals("a", edge.sourceNode.id);
		assertEquals(24, edge.sourcePort.dx);
		assertEquals("b", edge.targetNode.id);
		assertEquals(0, edge.targetPort.dx);
		assertFalse(edge.feedback);
		assertEquals(1, graph.edges().size());
	}

	@Test
	void edgeDirectionsAreEnforced() {
		LayoutGraph graph = new LayoutGraph();
		graph.add(buffer("a"));
		graph.add(buffer("b"));
		// source must be an output port
		assertThrows(IllegalArgumentException.class,
				() -> graph.connect("n1", "a", "in", "b", "in", false));
		// target must be an input port
		assertThrows(IllegalArgumentException.class,
				() -> graph.connect("n1", "a", "out", "b", "out",
						false));
	}

	@Test
	void handedOutCollectionsAreUnmodifiable() {
		LayoutGraph graph = new LayoutGraph();
		graph.add(buffer("a"));
		graph.add(buffer("b"));
		LayoutGraph.Edge edge =
				graph.connect("n1", "a", "out", "b", "in", false);
		assertThrows(UnsupportedOperationException.class,
				() -> graph.edges().remove(edge));
		assertThrows(UnsupportedOperationException.class,
				() -> graph.nodes().clear());
		assertThrows(UnsupportedOperationException.class,
				() -> graph.node("a").ports().clear());
	}

} // end of LayoutGraphTest class
