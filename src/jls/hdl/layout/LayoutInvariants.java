package jls.hdl.layout;

import java.util.ArrayList;
import java.util.List;

import jls.JLSInfo;

/**
 * The hard drawing invariants every layout must satisfy before the
 * importer realizes it as elements and WireEnds (issue #62 §2): every
 * coordinate on the 12-px grid ({@link jls.JLSInfo#spacing}), every
 * route anchored at the exact port attachment points, every segment
 * horizontal or vertical with nonzero length, and no two element
 * bodies overlapping. A layout that satisfies these re-saves and
 * re-opens identically to a hand-drawn circuit; one that violates any
 * of them renders wrong, so a violating engine output is rejected
 * wholesale ({@link LayoutException}), never patched up.
 */
public final class LayoutInvariants {

	private LayoutInvariants() {
		// static checks only
	}

	/**
	 * Checks the drawing invariants.
	 *
	 * @param result the layout to check; need not be complete -
	 *		missing placements and routes are reported as violations
	 *
	 * @return violation descriptions, one per finding; empty means the
	 *		layout is safe to realize
	 */
	public static List<String> check(LayoutResult result) {
		List<String> violations = new ArrayList<String>();
		LayoutGraph graph = result.graph();
		checkPlacements(graph, result, violations);
		checkRoutes(graph, result, violations);
		return violations;
	}

	/**
	 * Checks that every node is placed, on-grid, on-canvas, and that
	 * no two element bodies overlap.
	 *
	 * @param graph the placement problem
	 * @param result the layout under check
	 * @param violations receives one description per finding
	 */
	private static void checkPlacements(LayoutGraph graph,
			LayoutResult result, List<String> violations) {
		List<LayoutGraph.Node> nodes = graph.nodes();
		for (LayoutGraph.Node node : nodes) {
			LayoutResult.Point position = result.position(node.id);
			if (position == null) {
				violations.add("node " + node.id + " is not placed");
				continue;
			}
			if (position.x < 0 || position.y < 0) {
				violations.add("node " + node.id + " is off-canvas at "
						+ position);
			}
			if (!onGrid(position.x) || !onGrid(position.y)) {
				violations.add("node " + node.id + " is off the "
						+ JLSInfo.spacing + "-px grid at " + position);
			}
		}
		for (int first = 0; first < nodes.size(); first += 1) {
			LayoutGraph.Node a = nodes.get(first);
			LayoutResult.Point pa = result.position(a.id);
			if (pa == null) {
				continue;
			}
			for (int second = first + 1; second < nodes.size();
					second += 1) {
				LayoutGraph.Node b = nodes.get(second);
				LayoutResult.Point pb = result.position(b.id);
				if (pb == null) {
					continue;
				}
				boolean xOverlap = Math.max(pa.x, pb.x)
						< Math.min(pa.x + a.width, pb.x + b.width);
				boolean yOverlap = Math.max(pa.y, pb.y)
						< Math.min(pa.y + a.height, pb.y + b.height);
				if (xOverlap && yOverlap) {
					violations.add("element bodies of " + a.id + " and "
							+ b.id + " overlap");
				}
			}
		}
	}

	/**
	 * Checks that every edge is routed, anchored at its exact port
	 * attachment points, orthogonal, on-grid, with no zero-length
	 * segments.
	 *
	 * @param graph the placement problem
	 * @param result the layout under check
	 * @param violations receives one description per finding
	 */
	private static void checkRoutes(LayoutGraph graph,
			LayoutResult result, List<String> violations) {
		for (LayoutGraph.Edge edge : graph.edges()) {
			String where = "route on net " + edge.netName + " ("
					+ edge.sourceNode.id + "." + edge.sourcePort.name
					+ " to " + edge.targetNode.id + "."
					+ edge.targetPort.name + ")";
			List<LayoutResult.Point> waypoints = result.route(edge);
			if (waypoints == null) {
				violations.add(where + " is missing");
				continue;
			}
			if (result.position(edge.sourceNode.id) != null) {
				LayoutResult.Point start = result.portLocation(
						edge.sourceNode, edge.sourcePort);
				if (!start.equals(waypoints.get(0))) {
					violations.add(where + " starts at "
							+ waypoints.get(0)
							+ " but the port attaches at " + start);
				}
			}
			if (result.position(edge.targetNode.id) != null) {
				LayoutResult.Point end = result.portLocation(
						edge.targetNode, edge.targetPort);
				if (!end.equals(waypoints.get(waypoints.size() - 1))) {
					violations.add(where + " ends at "
							+ waypoints.get(waypoints.size() - 1)
							+ " but the port attaches at " + end);
				}
			}
			for (LayoutResult.Point waypoint : waypoints) {
				if (!onGrid(waypoint.x) || !onGrid(waypoint.y)) {
					violations.add(where + " has off-grid waypoint "
							+ waypoint);
				}
			}
			for (int i = 1; i < waypoints.size(); i += 1) {
				LayoutResult.Point from = waypoints.get(i - 1);
				LayoutResult.Point to = waypoints.get(i);
				if (from.equals(to)) {
					violations.add(where + " has a zero-length segment"
							+ " at " + from);
				} else if (from.x != to.x && from.y != to.y) {
					violations.add(where + " has a non-orthogonal"
							+ " segment " + from + " to " + to);
				}
			}
		}
	}

	/**
	 * @param coordinate a pixel coordinate
	 *
	 * @return true if the coordinate lies on the snap grid
	 */
	private static boolean onGrid(int coordinate) {
		return coordinate % JLSInfo.spacing == 0;
	}

} // end of LayoutInvariants class
