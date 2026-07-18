package jls.hdl.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A solved layout for one {@link LayoutGraph}: a grid position for
 * every element and an orthogonal waypoint chain for every connection
 * (issue #62). The importer (issue #61) realizes a complete result as
 * placed elements and WireEnd chains; {@link LayoutInvariants} checks
 * the drawing invariants a result must satisfy before realization, and
 * {@link LayoutMetrics} scores its readability.
 *
 * <p>Coordinates are pixels in JLS's coordinate system (y grows
 * downward). A node's position is its top-left corner. A route runs
 * from the source port's attachment point to the target port's
 * attachment point, waypoint to waypoint, each pair of consecutive
 * waypoints forming one horizontal or vertical wire segment.</p>
 */
public final class LayoutResult {

	/** An immutable pixel coordinate. */
	public static final class Point {

		/** X coordinate in pixels. */
		public final int x;
		/** Y coordinate in pixels. */
		public final int y;

		/**
		 * Builds an immutable point.
		 *
		 * @param x x coordinate in pixels
		 * @param y y coordinate in pixels
		 */
		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}

		/**
		 * Value equality on both coordinates.
		 *
		 * @param other the object to compare against
		 *
		 * @return true if other is a Point at the same coordinates
		 */
		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Point)) {
				return false;
			}
			Point point = (Point) other;
			return x == point.x && y == point.y;
		}

		/**
		 * @return a hash consistent with {@link #equals}
		 */
		@Override
		public int hashCode() {
			return 31 * x + y;
		}

		/**
		 * @return the point as "(x,y)" for diagnostics
		 */
		@Override
		public String toString() {
			return "(" + x + "," + y + ")";
		}
	} // end of Point class

	private final LayoutGraph graph;
	private final Map<String, Point> positions =
			new LinkedHashMap<String, Point>();
	private final Map<LayoutGraph.Edge, List<Point>> routes =
			new IdentityHashMap<LayoutGraph.Edge, List<Point>>();

	/**
	 * Creates an empty result for one graph; the layouter fills it via
	 * {@link #place} and {@link #route(LayoutGraph.Edge, List)}.
	 *
	 * @param graph the placement problem this result solves
	 */
	public LayoutResult(LayoutGraph graph) {
		if (graph == null) {
			throw new IllegalArgumentException("result needs a graph");
		}
		this.graph = graph;
	}

	/**
	 * @return the placement problem this result solves
	 */
	public LayoutGraph graph() {
		return graph;
	}

	/**
	 * Places an element's top-left corner.
	 *
	 * @param nodeId id of the element to place
	 * @param x x coordinate of the top-left corner, pixels
	 * @param y y coordinate of the top-left corner, pixels
	 *
	 * @throws IllegalArgumentException if the graph has no such node
	 * @throws IllegalStateException if the node is already placed
	 */
	public void place(String nodeId, int x, int y) {
		graph.node(nodeId);
		if (positions.containsKey(nodeId)) {
			throw new IllegalStateException(
					"node " + nodeId + " is already placed");
		}
		positions.put(nodeId, new Point(x, y));
	}

	/**
	 * Records the waypoint chain for one connection.
	 *
	 * @param edge the connection, from this result's graph
	 * @param waypoints at least two points, source attachment first,
	 *		target attachment last (defensively copied)
	 *
	 * @throws IllegalArgumentException if the edge is not from this
	 *		result's graph or fewer than two waypoints are given
	 * @throws IllegalStateException if the edge is already routed
	 */
	public void route(LayoutGraph.Edge edge, List<Point> waypoints) {
		if (!graph.edges().contains(edge)) {
			throw new IllegalArgumentException(
					"edge is not from this result's graph");
		}
		if (waypoints == null || waypoints.size() < 2) {
			throw new IllegalArgumentException("a route needs at least"
					+ " two waypoints (net " + edge.netName + ")");
		}
		if (routes.containsKey(edge)) {
			throw new IllegalStateException("edge on net " + edge.netName
					+ " is already routed");
		}
		routes.put(edge, Collections.unmodifiableList(
				new ArrayList<Point>(waypoints)));
	}

	/**
	 * Reads back an element's placed position.
	 *
	 * @param nodeId id of the element
	 *
	 * @return the top-left corner, or null if not placed yet
	 *
	 * @throws IllegalArgumentException if the graph has no such node
	 */
	public Point position(String nodeId) {
		graph.node(nodeId);
		return positions.get(nodeId);
	}

	/**
	 * Reads back a connection's waypoint chain.
	 *
	 * @param edge the connection, from this result's graph
	 *
	 * @return the waypoints, unmodifiable, or null if not routed yet
	 *
	 * @throws IllegalArgumentException if the edge is not from this
	 *		result's graph
	 */
	public List<Point> route(LayoutGraph.Edge edge) {
		if (!graph.edges().contains(edge)) {
			throw new IllegalArgumentException(
					"edge is not from this result's graph");
		}
		return routes.get(edge);
	}

	/**
	 * Computes where a port attaches once its element is placed:
	 * element position plus the port's fixed offset. Routes must start
	 * and end exactly here, or WireEnds detach when drawn (issue #62).
	 *
	 * @param node the element, from this result's graph
	 * @param port one of the element's ports
	 *
	 * @return the attachment point
	 *
	 * @throws IllegalStateException if the element is not placed yet
	 */
	public Point portLocation(LayoutGraph.Node node,
			LayoutGraph.Port port) {
		Point position = position(node.id);
		if (position == null) {
			throw new IllegalStateException(
					"node " + node.id + " is not placed yet");
		}
		return new Point(position.x + port.dx, position.y + port.dy);
	}

	/**
	 * @return true once every node is placed and every edge routed
	 */
	public boolean isComplete() {
		return positions.size() == graph.nodes().size()
				&& routes.size() == graph.edges().size();
	}

} // end of LayoutResult class
