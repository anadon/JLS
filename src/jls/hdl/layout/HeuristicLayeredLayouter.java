package jls.hdl.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jls.JLSInfo;

/**
 * The dependency-free heuristic layered placer and orthogonal router
 * for imported Yosys netlists (issue #62) - the "seed" engine the
 * Stage 2 importer (#61) uses out of the box, before (and unless) the
 * optional out-of-process ELK runner is ever wired in behind the same
 * {@link SchematicLayouter} seam. It is a classic Sugiyama-style
 * pipeline sized for classroom-scale circuits:
 *
 * <ol>
 * <li><b>Layering.</b> Longest-path layering from the sources over the
 * forward (non-{@link LayoutGraph.Edge#feedback feedback}) edges, so
 * every forward edge spans at least one column left-to-right - the
 * flow convention JLS drawings and textbooks share (§4).</li>
 * <li><b>Ordering.</b> Barycenter crossing reduction: rows within each
 * layer are reordered toward the average row of their predecessors,
 * swept down then up a fixed number of times. Deterministic - a
 * requirement of the {@link SchematicLayouter} contract.</li>
 * <li><b>Coordinates.</b> Each layer is a disjoint vertical band on
 * the 12-px grid ({@link JLSInfo#spacing}); a uniform row pitch keeps
 * element bodies from overlapping without any collision search.</li>
 * <li><b>Routing.</b> Every net gets its own vertical lane in the gap
 * before its target layer, so different nets never share a channel
 * column; each edge is a grid-aligned orthogonal dogleg anchored
 * exactly at its two port attachment points.</li>
 * </ol>
 *
 * <p>The result is guaranteed {@link LayoutResult#isComplete()
 * complete} and to pass {@link LayoutInvariants#check} (on-grid,
 * anchored, orthogonal, non-overlapping bodies); readability against
 * the {@link LayoutMetrics} rubric is the tuning target, good at
 * adjacent-layer classroom scale and degrading gracefully (extra
 * wire, some crossings) on long multi-layer spans - at which point the
 * issue's escalation path is the ELK runner, not more heuristics
 * here.</p>
 */
public final class HeuristicLayeredLayouter implements SchematicLayouter {

	/** The snap grid; every emitted coordinate is a multiple of it. */
	private static final int GRID = JLSInfo.spacing;
	/** Minimum horizontal gap between adjacent layer bands, in px. */
	private static final int MIN_GAP = GRID * 6;
	/** Vertical clearance added below the tallest element, in px. */
	private static final int ROW_GAP = GRID * 3;
	/** Top-left canvas margin so every waypoint stays on-canvas. */
	private static final int MARGIN = GRID * 2;
	/** Barycenter down+up sweep count; two passes settle small graphs. */
	private static final int SWEEPS = 4;

	/**
	 * Creates the stateless layouter; one instance solves any number of
	 * independent placement problems.
	 */
	public HeuristicLayeredLayouter() {

		// stateless: all working state lives in local variables
	} // end of constructor

	/**
	 * Solves one placement problem end to end.
	 *
	 * @param graph the placement problem
	 *
	 * @return the solved, complete, invariant-clean layout
	 *
	 * @throws LayoutException if the produced layout would violate a
	 *		drawing invariant (a bug guard: the engine never emits one)
	 */
	@Override
	public LayoutResult layout(LayoutGraph graph) throws LayoutException {
		List<LayoutGraph.Node> nodes = graph.nodes();
		LayoutResult result = new LayoutResult(graph);
		if (nodes.isEmpty()) {
			return result;
		}

		Map<String, Integer> layerOf = assignLayers(graph);
		List<List<LayoutGraph.Node>> layers = groupByLayer(nodes, layerOf);
		orderLayers(graph, layers);

		Map<String, Integer> rowOf = new HashMap<String, Integer>();
		for (List<LayoutGraph.Node> layer : layers) {
			for (int row = 0; row < layer.size(); row += 1) {
				rowOf.put(layer.get(row).id, row);
			}
		}

		Map<String, Integer> laneOf = assignLanes(graph, layerOf);
		int[] laneCount = laneCounts(graph, layerOf, layers.size());
		int[] columnX = columnPositions(layers, laneCount);
		int rowPitch = rowPitch(nodes);

		for (int index = 0; index < layers.size(); index += 1) {
			List<LayoutGraph.Node> layer = layers.get(index);
			for (LayoutGraph.Node node : layer) {
				int x = columnX[index];
				int y = MARGIN + rowOf.get(node.id) * rowPitch;
				result.place(node.id, x, y);
			}
		}

		for (LayoutGraph.Edge edge : graph.edges()) {
			int targetLayer = layerOf.get(edge.targetNode.id);
			int lane = laneOf.get(laneKey(edge, targetLayer));
			int channelX = columnX[targetLayer] - GRID * (lane + 1);
			LayoutResult.Point start =
					result.portLocation(edge.sourceNode, edge.sourcePort);
			LayoutResult.Point end =
					result.portLocation(edge.targetNode, edge.targetPort);
			result.route(edge, dogleg(start, end, channelX));
		}

		List<String> violations = LayoutInvariants.check(result);
		if (!violations.isEmpty()) {
			throw new LayoutException("heuristic layouter produced an"
					+ " invalid layout: " + violations);
		}
		return result;
	}

	/**
	 * Longest-path layering over forward edges: a node's layer is one
	 * more than the deepest of its forward predecessors, sources at 0.
	 * Feedback edges are ignored so the problem is acyclic; any
	 * residual cycle is broken by a recursion-stack guard that treats
	 * the back reference as depth 0.
	 *
	 * @param graph the placement problem
	 *
	 * @return each node id mapped to its layer index
	 */
	private static Map<String, Integer> assignLayers(LayoutGraph graph) {
		Map<String, List<String>> preds =
				new HashMap<String, List<String>>();
		for (LayoutGraph.Node node : graph.nodes()) {
			preds.put(node.id, new ArrayList<String>());
		}
		for (LayoutGraph.Edge edge : graph.edges()) {
			if (!edge.feedback) {
				preds.get(edge.targetNode.id).add(edge.sourceNode.id);
			}
		}
		Map<String, Integer> layer = new HashMap<String, Integer>();
		Set<String> onStack = new HashSet<String>();
		for (LayoutGraph.Node node : graph.nodes()) {
			longestPath(node.id, preds, layer, onStack);
		}
		return layer;
	}

	/**
	 * Memoized longest-path depth of one node, with a cycle guard.
	 *
	 * @param id the node under evaluation
	 * @param preds forward-predecessor adjacency
	 * @param layer memo of already-computed depths (filled in place)
	 * @param onStack ids currently on the recursion stack (cycle guard)
	 *
	 * @return the node's layer depth
	 */
	private static int longestPath(String id, Map<String, List<String>> preds,
			Map<String, Integer> layer, Set<String> onStack) {
		Integer done = layer.get(id);
		if (done != null) {
			return done;
		}
		if (!onStack.add(id)) {
			return 0;
		}
		int depth = 0;
		for (String pred : preds.get(id)) {
			depth = Math.max(depth,
					longestPath(pred, preds, layer, onStack) + 1);
		}
		onStack.remove(id);
		layer.put(id, depth);
		return depth;
	}

	/**
	 * Buckets nodes into per-layer lists, preserving graph insertion
	 * order within each layer as the deterministic starting order.
	 *
	 * @param nodes all nodes in insertion order
	 * @param layerOf each node's layer index
	 *
	 * @return one node list per layer, index 0 leftmost
	 */
	private static List<List<LayoutGraph.Node>> groupByLayer(
			List<LayoutGraph.Node> nodes, Map<String, Integer> layerOf) {
		int max = 0;
		for (int depth : layerOf.values()) {
			max = Math.max(max, depth);
		}
		List<List<LayoutGraph.Node>> layers =
				new ArrayList<List<LayoutGraph.Node>>();
		for (int i = 0; i <= max; i += 1) {
			layers.add(new ArrayList<LayoutGraph.Node>());
		}
		for (LayoutGraph.Node node : nodes) {
			layers.get(layerOf.get(node.id)).add(node);
		}
		return layers;
	}

	/**
	 * Barycenter crossing reduction: sweep the layers left-to-right
	 * then right-to-left a fixed number of times, each pass reordering
	 * a layer by the mean row of its neighbours in the adjacent layer.
	 * Nodes with no neighbour in that direction keep their place (a
	 * stable sort on the barycenter key preserves order for ties).
	 *
	 * @param graph the placement problem (for adjacency)
	 * @param layers per-layer node lists, reordered in place
	 */
	private static void orderLayers(LayoutGraph graph,
			List<List<LayoutGraph.Node>> layers) {
		Map<String, List<String>> forwardPreds =
				new HashMap<String, List<String>>();
		Map<String, List<String>> forwardSuccs =
				new HashMap<String, List<String>>();
		for (LayoutGraph.Node node : graph.nodes()) {
			forwardPreds.put(node.id, new ArrayList<String>());
			forwardSuccs.put(node.id, new ArrayList<String>());
		}
		for (LayoutGraph.Edge edge : graph.edges()) {
			if (!edge.feedback) {
				forwardPreds.get(edge.targetNode.id)
						.add(edge.sourceNode.id);
				forwardSuccs.get(edge.sourceNode.id)
						.add(edge.targetNode.id);
			}
		}
		for (int sweep = 0; sweep < SWEEPS; sweep += 1) {
			boolean down = sweep % 2 == 0;
			if (down) {
				for (int i = 1; i < layers.size(); i += 1) {
					reorder(layers.get(i), rowIndex(layers), forwardPreds);
				}
			} else {
				for (int i = layers.size() - 2; i >= 0; i -= 1) {
					reorder(layers.get(i), rowIndex(layers), forwardSuccs);
				}
			}
		}
	}

	/**
	 * Snapshots the current row index of every node across all layers.
	 *
	 * @param layers the current ordering
	 *
	 * @return each node id mapped to its position within its layer
	 */
	private static Map<String, Integer> rowIndex(
			List<List<LayoutGraph.Node>> layers) {
		Map<String, Integer> rows = new HashMap<String, Integer>();
		for (List<LayoutGraph.Node> layer : layers) {
			for (int row = 0; row < layer.size(); row += 1) {
				rows.put(layer.get(row).id, row);
			}
		}
		return rows;
	}

	/**
	 * Stably reorders one layer by the barycenter of each node's
	 * neighbours in the fixed adjacent layer.
	 *
	 * @param layer the layer to reorder in place
	 * @param rows current row indices of all nodes
	 * @param neighbours predecessor or successor adjacency to average
	 */
	private static void reorder(List<LayoutGraph.Node> layer,
			Map<String, Integer> rows,
			Map<String, List<String>> neighbours) {
		Map<String, Double> key = new HashMap<String, Double>();
		for (int position = 0; position < layer.size(); position += 1) {
			LayoutGraph.Node node = layer.get(position);
			List<String> adj = neighbours.get(node.id);
			double bary;
			if (adj.isEmpty()) {
				bary = position;
			} else {
				double sum = 0;
				for (String other : adj) {
					sum += rows.get(other);
				}
				bary = sum / adj.size();
			}
			key.put(node.id, bary);
		}
		layer.sort(Comparator.comparingDouble(node -> key.get(node.id)));
	}

	/**
	 * Assigns each net entering a given target layer its own lane
	 * index, so different nets never share a vertical channel column
	 * (the source of collinear wire overlap the rubric hard-fails).
	 * Lanes are numbered in first-appearance order for determinism.
	 *
	 * @param graph the placement problem
	 * @param layerOf each node's layer index
	 *
	 * @return map from "targetLayer|netName" to lane index
	 */
	private static Map<String, Integer> assignLanes(LayoutGraph graph,
			Map<String, Integer> layerOf) {
		Map<Integer, Map<String, Integer>> perLayer =
				new HashMap<Integer, Map<String, Integer>>();
		Map<String, Integer> lanes = new HashMap<String, Integer>();
		for (LayoutGraph.Edge edge : graph.edges()) {
			int target = layerOf.get(edge.targetNode.id);
			Map<String, Integer> netLanes = perLayer.get(target);
			if (netLanes == null) {
				netLanes = new LinkedHashMap<String, Integer>();
				perLayer.put(target, netLanes);
			}
			Integer lane = netLanes.get(edge.netName);
			if (lane == null) {
				lane = netLanes.size();
				netLanes.put(edge.netName, lane);
			}
			lanes.put(laneKey(edge, target), lane);
		}
		return lanes;
	}

	/**
	 * Counts the distinct nets entering each layer - the number of
	 * vertical lanes the gap before that layer must hold.
	 *
	 * @param graph the placement problem
	 * @param layerOf each node's layer index
	 * @param layerCount total number of layers
	 *
	 * @return lane count per layer index
	 */
	private static int[] laneCounts(LayoutGraph graph,
			Map<String, Integer> layerOf, int layerCount) {
		List<Set<String>> nets = new ArrayList<Set<String>>();
		for (int i = 0; i < layerCount; i += 1) {
			nets.add(new LinkedHashSet<String>());
		}
		for (LayoutGraph.Edge edge : graph.edges()) {
			nets.get(layerOf.get(edge.targetNode.id)).add(edge.netName);
		}
		int[] counts = new int[layerCount];
		for (int i = 0; i < layerCount; i += 1) {
			counts[i] = nets.get(i).size();
		}
		return counts;
	}

	/**
	 * X coordinate of each layer band's left edge. Every band is a
	 * grid-aligned column; the gap before a layer is widened so all of
	 * that layer's vertical lanes fit between it and the previous band.
	 *
	 * @param layers per-layer node lists
	 * @param laneCount lanes entering each layer
	 *
	 * @return left-edge x of each layer, index 0 leftmost
	 */
	private static int[] columnPositions(
			List<List<LayoutGraph.Node>> layers, int[] laneCount) {
		int[] columnX = new int[layers.size()];
		int x = Math.max(MARGIN, (laneCount[0] + 2) * GRID);
		for (int i = 0; i < layers.size(); i += 1) {
			columnX[i] = x;
			int width = 0;
			for (LayoutGraph.Node node : layers.get(i)) {
				width = Math.max(width, node.width);
			}
			int nextLanes = i + 1 < laneCount.length ? laneCount[i + 1] : 0;
			int gap = Math.max(MIN_GAP, (nextLanes + 2) * GRID);
			x += width + gap;
		}
		return columnX;
	}

	/**
	 * Uniform vertical distance between successive rows: the tallest
	 * element plus a clearance gap, so no two bodies in a column ever
	 * touch. On the grid because both terms are.
	 *
	 * @param nodes all nodes
	 *
	 * @return the row pitch in pixels
	 */
	private static int rowPitch(List<LayoutGraph.Node> nodes) {
		int tallest = 0;
		for (LayoutGraph.Node node : nodes) {
			tallest = Math.max(tallest, node.height);
		}
		return tallest + ROW_GAP;
	}

	/**
	 * Builds an orthogonal dogleg from a source port to a target port
	 * through a given vertical channel: horizontal to the channel,
	 * vertical to the target row, horizontal to the target. Degenerate
	 * (collinear or zero-length) steps are cleaned away so the route
	 * always satisfies {@link LayoutInvariants} - at least two points,
	 * anchored at both ports, every segment orthogonal and nonzero.
	 *
	 * @param start the source port attachment point
	 * @param end the target port attachment point
	 * @param channelX x of the vertical channel column
	 *
	 * @return the cleaned waypoint chain, start first and end last
	 */
	private static List<LayoutResult.Point> dogleg(LayoutResult.Point start,
			LayoutResult.Point end, int channelX) {
		List<LayoutResult.Point> raw = new ArrayList<LayoutResult.Point>();
		raw.add(start);
		raw.add(new LayoutResult.Point(channelX, start.y));
		raw.add(new LayoutResult.Point(channelX, end.y));
		raw.add(end);
		return clean(raw);
	}

	/**
	 * Removes consecutive duplicate points and merges collinear runs,
	 * keeping the first and last point fixed.
	 *
	 * @param points the raw orthogonal chain
	 *
	 * @return a minimal chain of at least two points
	 */
	private static List<LayoutResult.Point> clean(
			List<LayoutResult.Point> points) {
		List<LayoutResult.Point> dedup =
				new ArrayList<LayoutResult.Point>();
		for (LayoutResult.Point point : points) {
			if (dedup.isEmpty()
					|| !dedup.get(dedup.size() - 1).equals(point)) {
				dedup.add(point);
			}
		}
		List<LayoutResult.Point> merged =
				new ArrayList<LayoutResult.Point>();
		for (int i = 0; i < dedup.size(); i += 1) {
			if (i == 0 || i == dedup.size() - 1) {
				merged.add(dedup.get(i));
				continue;
			}
			LayoutResult.Point prev = dedup.get(i - 1);
			LayoutResult.Point cur = dedup.get(i);
			LayoutResult.Point next = dedup.get(i + 1);
			boolean collinearX = prev.x == cur.x && cur.x == next.x;
			boolean collinearY = prev.y == cur.y && cur.y == next.y;
			if (!collinearX && !collinearY) {
				merged.add(cur);
			}
		}
		if (merged.size() < 2) {
			// start == end after cleaning cannot happen for distinct
			// ports, but keep the contract's two-point minimum anyway.
			merged.clear();
			merged.add(points.get(0));
			merged.add(points.get(points.size() - 1));
		}
		return Collections.unmodifiableList(merged);
	}

	/**
	 * The per-layer lane map key for one edge.
	 *
	 * @param edge the connection
	 * @param targetLayer the layer the edge enters
	 *
	 * @return the "layer|net" key
	 */
	private static String laneKey(LayoutGraph.Edge edge, int targetLayer) {
		return targetLayer + "|" + edge.netName;
	}

} // end of HeuristicLayeredLayouter class
