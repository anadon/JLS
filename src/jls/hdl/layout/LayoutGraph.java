package jls.hdl.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The placement problem handed to a {@link SchematicLayouter} (issue
 * #62): elements with fixed per-port pixel offsets, and point-to-point
 * connections grouped into nets by name. The Stage 2 importer (issue
 * #61) builds one of these after its netlist-to-element conversion
 * (Splitter/Binder meshes, Constants) is already done, so a layouter
 * sees only opaque rectangles with ports - never netlist semantics.
 *
 * <p>Register feedback is broken here, not in the layouter: the
 * builder marks every connection leaving a register output that closes
 * a combinational cycle as a {@link Edge#feedback back-edge}, which
 * layouters route against the flow direction and the quality rubric
 * exempts from its left-to-right requirement.</p>
 *
 * <p>All coordinates are pixels in JLS's coordinate system (y grows
 * downward). Port offsets are relative to the element's top-left
 * corner and, like all JLS element geometry, are multiples of the
 * 12-px grid unit ({@link jls.JLSInfo#spacing}).</p>
 */
public final class LayoutGraph {

	/** Which way a port carries signal, from the element's viewpoint. */
	public enum Direction {
		/** The element reads this port; wires end here. */
		INPUT,
		/** The element drives this port; wires start here. */
		OUTPUT
	} // end of Direction enum

	/** One fixed connection point on an element. */
	public static final class Port {

		/** Port name, unique within its element (e.g. "input0", "q"). */
		public final String name;
		/** Whether the element reads or drives this port. */
		public final Direction direction;
		/** X offset from the element's top-left corner, in pixels. */
		public final int dx;
		/** Y offset from the element's top-left corner, in pixels. */
		public final int dy;

		/**
		 * Builds an immutable port descriptor.
		 *
		 * @param name port name, unique within its element
		 * @param direction whether the element reads or drives the port
		 * @param dx x offset from the element's top-left corner, pixels
		 * @param dy y offset from the element's top-left corner, pixels
		 */
		public Port(String name, Direction direction, int dx, int dy) {
			if (name == null || name.isEmpty()) {
				throw new IllegalArgumentException("port needs a name");
			}
			if (direction == null) {
				throw new IllegalArgumentException(
						"port " + name + " needs a direction");
			}
			this.name = name;
			this.direction = direction;
			this.dx = dx;
			this.dy = dy;
		}
	} // end of Port class

	/** One element to place: an opaque rectangle with ports. */
	public static final class Node {

		/** Stable identifier, unique within the graph. */
		public final String id;
		/** Human context for diagnostics (element kind, e.g. "AndGate"). */
		public final String kind;
		/** Element width in pixels (a positive multiple of the grid). */
		public final int width;
		/** Element height in pixels (a positive multiple of the grid). */
		public final int height;
		private final Map<String, Port> ports =
				new LinkedHashMap<String, Port>();

		/**
		 * Builds an immutable node descriptor.
		 *
		 * @param id stable identifier, unique within the graph
		 * @param kind human context for diagnostics (element kind)
		 * @param width element width in pixels, positive
		 * @param height element height in pixels, positive
		 * @param ports the element's ports; names must be unique
		 */
		public Node(String id, String kind, int width, int height,
				List<Port> ports) {
			if (id == null || id.isEmpty()) {
				throw new IllegalArgumentException("node needs an id");
			}
			if (width <= 0 || height <= 0) {
				throw new IllegalArgumentException("node " + id
						+ " needs a positive size, got " + width + "x"
						+ height);
			}
			this.id = id;
			this.kind = kind == null ? "" : kind;
			this.width = width;
			this.height = height;
			for (Port port : ports) {
				if (this.ports.put(port.name, port) != null) {
					throw new IllegalArgumentException("node " + id
							+ " has two ports named " + port.name);
				}
			}
		}

		/**
		 * Lists this element's ports.
		 *
		 * @return the node's ports in declaration order, unmodifiable
		 */
		public List<Port> ports() {
			return Collections.unmodifiableList(
					new ArrayList<Port>(ports.values()));
		}

		/**
		 * Looks up a port by name.
		 *
		 * @param name the port name
		 *
		 * @return the port
		 *
		 * @throws IllegalArgumentException if no such port exists
		 */
		public Port port(String name) {
			Port port = ports.get(name);
			if (port == null) {
				throw new IllegalArgumentException("node " + id
						+ " has no port named " + name);
			}
			return port;
		}
	} // end of Node class

	/**
	 * One point-to-point connection from an output port to an input
	 * port. A multi-fanout net is several edges sharing one
	 * {@link #netName}; metrics and routing group them by that name.
	 */
	public static final class Edge {

		/** Net this connection belongs to (groups fanout edges). */
		public final String netName;
		/** Element driving the connection. */
		public final Node sourceNode;
		/** Output port the connection starts at. */
		public final Port sourcePort;
		/** Element reading the connection. */
		public final Node targetNode;
		/** Input port the connection ends at. */
		public final Port targetPort;
		/**
		 * True for a register back-edge: a connection the graph builder
		 * broke to keep the layered problem acyclic. Layouters route
		 * these against the flow direction; the quality rubric exempts
		 * them from its left-to-right requirement.
		 */
		public final boolean feedback;

		/**
		 * Built only by {@link LayoutGraph#connect}, which has already
		 * resolved and direction-checked both endpoints.
		 *
		 * @param netName net this connection belongs to
		 * @param sourceNode element driving the connection
		 * @param sourcePort output port the connection starts at
		 * @param targetNode element reading the connection
		 * @param targetPort input port the connection ends at
		 * @param feedback true for a register back-edge
		 */
		Edge(String netName, Node sourceNode, Port sourcePort,
				Node targetNode, Port targetPort, boolean feedback) {
			this.netName = netName;
			this.sourceNode = sourceNode;
			this.sourcePort = sourcePort;
			this.targetNode = targetNode;
			this.targetPort = targetPort;
			this.feedback = feedback;
		}
	} // end of Edge class

	private final Map<String, Node> nodes =
			new LinkedHashMap<String, Node>();
	private final List<Edge> edges = new ArrayList<Edge>();

	/**
	 * Creates an empty graph; the builder populates it with
	 * {@link #add} and {@link #connect}.
	 */
	public LayoutGraph() {
	}

	/**
	 * Adds an element to the graph.
	 *
	 * @param node the element to add
	 *
	 * @throws IllegalArgumentException if the graph already has a node
	 *		with the same id
	 */
	public void add(Node node) {
		if (nodes.put(node.id, node) != null) {
			throw new IllegalArgumentException(
					"duplicate node id " + node.id);
		}
	}

	/**
	 * Connects an output port to an input port.
	 *
	 * @param netName net the connection belongs to
	 * @param sourceNodeId id of the element driving the connection
	 * @param sourcePortName output port the connection starts at
	 * @param targetNodeId id of the element reading the connection
	 * @param targetPortName input port the connection ends at
	 * @param feedback true for a register back-edge
	 *
	 * @return the new edge
	 *
	 * @throws IllegalArgumentException if either node or port is
	 *		unknown, if the source port is not an output, or if the
	 *		target port is not an input
	 */
	public Edge connect(String netName, String sourceNodeId,
			String sourcePortName, String targetNodeId,
			String targetPortName, boolean feedback) {
		if (netName == null || netName.isEmpty()) {
			throw new IllegalArgumentException("edge needs a net name");
		}
		Node source = node(sourceNodeId);
		Node target = node(targetNodeId);
		Port sourcePort = source.port(sourcePortName);
		Port targetPort = target.port(targetPortName);
		if (sourcePort.direction != Direction.OUTPUT) {
			throw new IllegalArgumentException("edge on net " + netName
					+ " starts at " + sourceNodeId + "." + sourcePortName
					+ ", which is not an output port");
		}
		if (targetPort.direction != Direction.INPUT) {
			throw new IllegalArgumentException("edge on net " + netName
					+ " ends at " + targetNodeId + "." + targetPortName
					+ ", which is not an input port");
		}
		Edge edge = new Edge(netName, source, sourcePort, target,
				targetPort, feedback);
		edges.add(edge);
		return edge;
	}

	/**
	 * Lists the elements to place.
	 *
	 * @return the elements in insertion order, unmodifiable
	 */
	public List<Node> nodes() {
		return Collections.unmodifiableList(
				new ArrayList<Node>(nodes.values()));
	}

	/**
	 * Looks up an element by id.
	 *
	 * @param id the node id
	 *
	 * @return the node
	 *
	 * @throws IllegalArgumentException if no such node exists
	 */
	public Node node(String id) {
		Node node = nodes.get(id);
		if (node == null) {
			throw new IllegalArgumentException("no node with id " + id);
		}
		return node;
	}

	/**
	 * Lists the point-to-point connections.
	 *
	 * @return the connections in insertion order, unmodifiable
	 */
	public List<Edge> edges() {
		return Collections.unmodifiableList(edges);
	}

} // end of LayoutGraph class
