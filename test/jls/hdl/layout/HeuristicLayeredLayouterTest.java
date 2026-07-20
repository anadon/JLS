package jls.hdl.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The heuristic layered layouter's contract (issue #62): it turns any
 * {@link LayoutGraph} into a {@link LayoutResult} that is complete,
 * clean against {@link LayoutInvariants}, deterministic, and - at
 * adjacent-layer classroom scale - overlap-free and left-to-right per
 * the {@link LayoutMetrics} rubric. Every test drives the real engine;
 * none needs an external tool.
 */
class HeuristicLayeredLayouterTest {

	/** A 24x24 buffer: one input at (0,12), one output at (24,12). */
	private static LayoutGraph.Node buffer(String id) {
		return new LayoutGraph.Node(id, "Buffer", 24, 24, List.of(
				new LayoutGraph.Port("in",
						LayoutGraph.Direction.INPUT, 0, 12),
				new LayoutGraph.Port("out",
						LayoutGraph.Direction.OUTPUT, 24, 12)));
	}

	/** A 24x24 two-input gate: ins at (0,0)/(0,24), out at (24,12). */
	private static LayoutGraph.Node gate(String id) {
		return new LayoutGraph.Node(id, "AndGate", 24, 24, List.of(
				new LayoutGraph.Port("in0",
						LayoutGraph.Direction.INPUT, 0, 0),
				new LayoutGraph.Port("in1",
						LayoutGraph.Direction.INPUT, 0, 24),
				new LayoutGraph.Port("out",
						LayoutGraph.Direction.OUTPUT, 24, 12)));
	}

	private final HeuristicLayeredLayouter layouter =
			new HeuristicLayeredLayouter();

	@Test
	void anEmptyGraphLaysOutToACompleteEmptyResult() throws Exception {
		LayoutResult result = layouter.layout(new LayoutGraph());
		assertTrue(result.isComplete());
		assertEquals(List.of(), LayoutInvariants.check(result));
	}

	@Test
	void aChainIsCompleteInvariantCleanAndFlowsLeftToRight()
			throws Exception {
		LayoutGraph graph = new LayoutGraph();
		graph.add(buffer("a"));
		graph.add(buffer("b"));
		graph.add(buffer("c"));
		graph.connect("n1", "a", "out", "b", "in", false);
		graph.connect("n2", "b", "out", "c", "in", false);

		LayoutResult result = layouter.layout(graph);

		assertTrue(result.isComplete(), "every node placed, every edge routed");
		assertEquals(List.of(), LayoutInvariants.check(result));
		// longest-path layering marches strictly rightward.
		int xa = result.position("a").x;
		int xb = result.position("b").x;
		int xc = result.position("c").x;
		assertTrue(xa < xb && xb < xc,
				"layers advance left-to-right: " + xa + "," + xb + "," + xc);
	}

	@Test
	void fanoutOnOneNetSharesAChannelWithoutOverlap() throws Exception {
		LayoutGraph graph = new LayoutGraph();
		graph.add(buffer("s"));
		graph.add(buffer("t1"));
		graph.add(buffer("t2"));
		graph.add(buffer("t3"));
		graph.connect("fan", "s", "out", "t1", "in", false);
		graph.connect("fan", "s", "out", "t2", "in", false);
		graph.connect("fan", "s", "out", "t3", "in", false);

		LayoutResult result = layouter.layout(graph);

		assertEquals(List.of(), LayoutInvariants.check(result));
		LayoutMetrics metrics = LayoutMetrics.measure(result);
		assertEquals(0, metrics.overlapCount,
				"one fanout net never overlaps itself");
	}

	@Test
	void aSmallCombinationalBlockMeetsTheOverlapAndFlowRubric()
			throws Exception {
		LayoutGraph graph = new LayoutGraph();
		graph.add(buffer("inA"));
		graph.add(buffer("inB"));
		graph.add(gate("g"));
		graph.add(buffer("out"));
		graph.connect("a", "inA", "out", "g", "in0", false);
		graph.connect("b", "inB", "out", "g", "in1", false);
		graph.connect("y", "g", "out", "out", "in", false);

		LayoutResult result = layouter.layout(graph);

		assertEquals(List.of(), LayoutInvariants.check(result));
		LayoutMetrics metrics = LayoutMetrics.measure(result);
		assertEquals(0, metrics.overlapCount, metrics.rubricFailures().toString());
		assertEquals(1.0, metrics.leftToRightFraction, 1e-9,
				"all non-feedback nets flow left-to-right");
	}

	@Test
	void aRegisterFeedbackLoopLaysOutWithoutError() throws Exception {
		// r -> g -> r, the last edge a broken back-edge.
		LayoutGraph graph = new LayoutGraph();
		graph.add(buffer("r"));
		graph.add(gate("g"));
		graph.connect("q", "r", "out", "g", "in0", false);
		graph.connect("k", "r", "out", "g", "in1", false);
		graph.connect("d", "g", "out", "r", "in", true);

		LayoutResult result = layouter.layout(graph);

		assertTrue(result.isComplete());
		assertEquals(List.of(), LayoutInvariants.check(result));
		// the feedback edge is still routed.
		assertNotNull(result.route(graph.edges().get(2)));
	}

	@Test
	void theSameGraphAlwaysProducesTheSameLayout() throws Exception {
		LayoutGraph graph = new LayoutGraph();
		graph.add(buffer("a"));
		graph.add(gate("g"));
		graph.add(buffer("z"));
		graph.connect("n1", "a", "out", "g", "in0", false);
		graph.connect("n2", "a", "out", "g", "in1", false);
		graph.connect("n3", "g", "out", "z", "in", false);

		LayoutResult first = layouter.layout(graph);
		LayoutResult second =
				new HeuristicLayeredLayouter().layout(graph);

		for (LayoutGraph.Node node : graph.nodes()) {
			assertEquals(first.position(node.id), second.position(node.id),
					"placement of " + node.id + " is deterministic");
		}
		for (LayoutGraph.Edge edge : graph.edges()) {
			assertEquals(first.route(edge), second.route(edge),
					"route of net " + edge.netName + " is deterministic");
		}
	}

} // end of HeuristicLayeredLayouterTest class
