package jls.hdl.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The quantified readability rubric from the issue #62 addendum,
 * pinned metric by metric on tiny synthetic layouts with known
 * geometry: overlaps (collinear and through-body) hard-fail, crossings
 * are counted per net, routed length is compared to the per-net
 * Manhattan lower bound, and right-to-left flow fails unless the net
 * is a register back-edge.
 */
class LayoutMetricsTest {

	/** A 12x12 element with one output port at offset (dx,dy). */
	private static LayoutGraph.Node source(String id, int dx, int dy) {
		return new LayoutGraph.Node(id, "Src", 12, 12, List.of(
				new LayoutGraph.Port("out",
						LayoutGraph.Direction.OUTPUT, dx, dy)));
	}

	/** A 12x12 element with one input port at offset (dx,dy). */
	private static LayoutGraph.Node sink(String id, int dx, int dy) {
		return new LayoutGraph.Node(id, "Snk", 12, 12, List.of(
				new LayoutGraph.Port("in",
						LayoutGraph.Direction.INPUT, dx, dy)));
	}

	@Test
	void metricsNeedACompleteLayout() {
		LayoutGraph graph = new LayoutGraph();
		graph.add(source("a", 12, 6));
		LayoutResult result = new LayoutResult(graph);
		assertThrows(IllegalArgumentException.class,
				() -> LayoutMetrics.measure(result));
	}

	@Test
	void aStraightWireMeetsTheRubric() {
		LayoutGraph graph = new LayoutGraph();
		graph.add(source("a", 12, 6));
		graph.add(sink("b", 0, 6));
		LayoutGraph.Edge edge =
				graph.connect("n1", "a", "out", "b", "in", false);
		LayoutResult result = new LayoutResult(graph);
		result.place("a", 0, 0);
		result.place("b", 48, 0);
		result.route(edge, List.of(new LayoutResult.Point(12, 6),
				new LayoutResult.Point(48, 6)));
		LayoutMetrics metrics = LayoutMetrics.measure(result);
		assertEquals(0, metrics.overlapCount);
		assertEquals(0, metrics.crossingCount);
		assertEquals(36, metrics.routedLength);
		assertEquals(36, metrics.manhattanLowerBound);
		assertEquals(1.0, metrics.wireLengthRatio);
		assertEquals(1.0, metrics.leftToRightFraction);
		assertEquals(60L * 12L, metrics.boundingBoxArea);
		assertTrue(metrics.meetsRubric(),
				metrics.rubricFailures().toString());
	}

	@Test
	void aCrossingCountsAgainstBothNets() {
		LayoutGraph graph = new LayoutGraph();
		graph.add(source("a", 12, 6));
		graph.add(sink("b", 0, 6));
		graph.add(source("c", 6, 12));
		graph.add(sink("d", 6, 0));
		LayoutGraph.Edge horizontal =
				graph.connect("n1", "a", "out", "b", "in", false);
		LayoutGraph.Edge vertical =
				graph.connect("n2", "c", "out", "d", "in", false);
		LayoutResult result = new LayoutResult(graph);
		result.place("a", 0, 54);
		result.place("b", 108, 54);
		result.place("c", 54, 0);
		result.place("d", 54, 108);
		result.route(horizontal, List.of(
				new LayoutResult.Point(12, 60),
				new LayoutResult.Point(108, 60)));
		result.route(vertical, List.of(
				new LayoutResult.Point(60, 12),
				new LayoutResult.Point(60, 108)));
		LayoutMetrics metrics = LayoutMetrics.measure(result);
		assertEquals(0, metrics.overlapCount);
		assertEquals(1, metrics.crossingCount);
		// one crossing involves both nets: 2 involvements / 2 nets
		assertEquals(1.0, metrics.crossingsPerNet);
		assertEquals(1, metrics.maxCrossingsOnOneNet);
		assertFalse(metrics.meetsRubric());
		assertTrue(metrics.rubricFailures().stream()
						.anyMatch(f -> f.contains("crossings per net")),
				metrics.rubricFailures().toString());
	}

	@Test
	void collinearOverlapBetweenDifferentNetsIsAHardFail() {
		LayoutGraph graph = new LayoutGraph();
		graph.add(source("a", 12, 6));
		graph.add(sink("b", 0, 6));
		// ports reach up so the second wire is collinear with the first
		graph.add(source("c", 0, -42));
		graph.add(sink("d", 0, -42));
		LayoutGraph.Edge first =
				graph.connect("n1", "a", "out", "b", "in", false);
		LayoutGraph.Edge second =
				graph.connect("n2", "c", "out", "d", "in", false);
		LayoutResult result = new LayoutResult(graph);
		result.place("a", 0, 0);
		result.place("b", 48, 0);
		result.place("c", 24, 48);
		result.place("d", 44, 48);
		result.route(first, List.of(new LayoutResult.Point(12, 6),
				new LayoutResult.Point(48, 6)));
		result.route(second, List.of(new LayoutResult.Point(24, 6),
				new LayoutResult.Point(44, 6)));
		LayoutMetrics metrics = LayoutMetrics.measure(result);
		assertEquals(1, metrics.overlapCount);
		assertFalse(metrics.meetsRubric());
		assertTrue(metrics.rubricFailures().stream()
						.anyMatch(f -> f.contains("hard fail")),
				metrics.rubricFailures().toString());
	}

	@Test
	void aWireThroughAnElementBodyIsAHardFail() {
		LayoutGraph graph = new LayoutGraph();
		graph.add(source("a", 12, 6));
		graph.add(sink("b", 0, 6));
		// portless bystander parked on top of the wire
		graph.add(new LayoutGraph.Node("c", "Gate", 12, 12,
				List.of()));
		LayoutGraph.Edge edge =
				graph.connect("n1", "a", "out", "b", "in", false);
		LayoutResult result = new LayoutResult(graph);
		result.place("a", 0, 0);
		result.place("b", 48, 0);
		result.place("c", 24, 0);
		result.route(edge, List.of(new LayoutResult.Point(12, 6),
				new LayoutResult.Point(48, 6)));
		LayoutMetrics metrics = LayoutMetrics.measure(result);
		assertEquals(1, metrics.overlapCount);
		assertFalse(metrics.meetsRubric());
	}

	@Test
	void aLongDetourFailsTheWireLengthRatio() {
		LayoutGraph graph = new LayoutGraph();
		graph.add(source("a", 12, 6));
		graph.add(sink("b", 0, 6));
		LayoutGraph.Edge edge =
				graph.connect("n1", "a", "out", "b", "in", false);
		LayoutResult result = new LayoutResult(graph);
		result.place("a", 0, 0);
		result.place("b", 48, 0);
		result.route(edge, List.of(new LayoutResult.Point(12, 6),
				new LayoutResult.Point(12, 102),
				new LayoutResult.Point(48, 102),
				new LayoutResult.Point(48, 6)));
		LayoutMetrics metrics = LayoutMetrics.measure(result);
		assertEquals(228, metrics.routedLength);
		assertEquals(36, metrics.manhattanLowerBound);
		assertEquals(0, metrics.overlapCount);
		assertFalse(metrics.meetsRubric());
		assertTrue(metrics.rubricFailures().stream()
						.anyMatch(f -> f.contains("Manhattan")),
				metrics.rubricFailures().toString());
	}

	@Test
	void rightToLeftFlowFailsUnlessTheNetIsFeedback() {
		LayoutMetrics forward = measureRightToLeft(false);
		assertEquals(0.0, forward.leftToRightFraction);
		assertFalse(forward.meetsRubric());
		assertTrue(forward.rubricFailures().stream()
						.anyMatch(f -> f.contains("left-to-right")),
				forward.rubricFailures().toString());

		LayoutMetrics feedback = measureRightToLeft(true);
		// no non-feedback nets left: the requirement is vacuous
		assertEquals(1.0, feedback.leftToRightFraction);
		assertTrue(feedback.meetsRubric(),
				feedback.rubricFailures().toString());
	}

	/**
	 * Lays out one net flowing right-to-left and measures it.
	 *
	 * @param feedback whether the net is marked as a register back-edge
	 *
	 * @return the metrics
	 */
	private static LayoutMetrics measureRightToLeft(boolean feedback) {
		LayoutGraph graph = new LayoutGraph();
		graph.add(source("s", 0, 6));
		graph.add(sink("t", 12, 6));
		LayoutGraph.Edge edge =
				graph.connect("n1", "s", "out", "t", "in", feedback);
		LayoutResult result = new LayoutResult(graph);
		result.place("s", 108, 0);
		result.place("t", 0, 0);
		result.route(edge, List.of(new LayoutResult.Point(108, 6),
				new LayoutResult.Point(12, 6)));
		return LayoutMetrics.measure(result);
	}

} // end of LayoutMetricsTest class
