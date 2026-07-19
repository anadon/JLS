package jls.hdl.layout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The quantified readability rubric for imported-netlist layouts,
 * fixed in the issue #62 addendum before any tuning began: overlap
 * count (hard zero), wire crossings per net, routed length against the
 * per-net Manhattan lower bound, and left-to-right signal flow with
 * register back-edges exempt. {@link #measure} computes the numbers
 * for one complete {@link LayoutResult}; {@link #rubricFailures} says
 * which launch thresholds a layout misses. The bounding-box area is
 * exposed for the compactness check, whose 4x threshold is relative to
 * a hand-drawn golden and therefore applied by the caller.
 *
 * <p>These numbers are what layout tuning optimizes and what CI can
 * regress against; the human trace-three-signals trial stays the
 * validity check on the rubric itself.</p>
 */
public final class LayoutMetrics {

	/** Launch threshold: overlaps allowed (hard requirement). */
	public static final int MAX_OVERLAPS = 0;
	/** Launch threshold: average crossings per net. */
	public static final double MAX_CROSSINGS_PER_NET = 0.5;
	/** Launch threshold: crossings on any single net. */
	public static final int MAX_CROSSINGS_ON_ONE_NET = 4;
	/** Launch threshold: routed length over Manhattan lower bound. */
	public static final double MAX_WIRE_LENGTH_RATIO = 2.0;
	/** Launch threshold: fraction of non-feedback nets flowing
	 *  left-to-right. */
	public static final double MIN_LEFT_TO_RIGHT_FRACTION = 0.9;

	/**
	 * Collinear same-axis overlaps between segments of different nets,
	 * plus wire segments passing through an element body's interior.
	 * The rubric hard-fails any layout where this is nonzero.
	 */
	public final int overlapCount;
	/** Interior intersections between segments of different nets. */
	public final int crossingCount;
	/**
	 * Average per-net crossing involvement: each crossing counts once
	 * for each of the two nets involved, averaged over all nets.
	 */
	public final double crossingsPerNet;
	/** Crossing involvement of the worst single net. */
	public final int maxCrossingsOnOneNet;
	/** Total routed wire length in pixels. */
	public final long routedLength;
	/**
	 * Sum over nets of the half-perimeter of the bounding box of the
	 * net's port attachment points - the standard Manhattan lower
	 * bound on the length needed to connect them.
	 */
	public final long manhattanLowerBound;
	/**
	 * {@link #routedLength} over {@link #manhattanLowerBound}; 1.0 for
	 * a layout with no wire length to compare (degenerate).
	 */
	public final double wireLengthRatio;
	/**
	 * Fraction of non-feedback nets whose every connection ends at or
	 * to the right of where it starts; 1.0 when every net is feedback.
	 */
	public final double leftToRightFraction;
	/**
	 * Area in square pixels of the bounding box of all element bodies
	 * and wire waypoints. The compactness threshold (at most 4x a
	 * hand-drawn golden of the same circuit) is applied by the caller.
	 */
	public final long boundingBoxArea;

	/**
	 * Wraps computed metric values; use {@link #measure}.
	 *
	 * @param overlapCount overlaps, including body pass-throughs
	 * @param crossingCount interior crossings between different nets
	 * @param crossingsPerNet average per-net crossing involvement
	 * @param maxCrossingsOnOneNet worst single net's involvement
	 * @param routedLength total routed length in pixels
	 * @param manhattanLowerBound summed per-net half-perimeter bound
	 * @param wireLengthRatio routed length over the lower bound
	 * @param leftToRightFraction left-to-right non-feedback nets
	 * @param boundingBoxArea layout bounding-box area, square pixels
	 */
	private LayoutMetrics(int overlapCount, int crossingCount,
			double crossingsPerNet, int maxCrossingsOnOneNet,
			long routedLength, long manhattanLowerBound,
			double wireLengthRatio, double leftToRightFraction,
			long boundingBoxArea) {
		this.overlapCount = overlapCount;
		this.crossingCount = crossingCount;
		this.crossingsPerNet = crossingsPerNet;
		this.maxCrossingsOnOneNet = maxCrossingsOnOneNet;
		this.routedLength = routedLength;
		this.manhattanLowerBound = manhattanLowerBound;
		this.wireLengthRatio = wireLengthRatio;
		this.leftToRightFraction = leftToRightFraction;
		this.boundingBoxArea = boundingBoxArea;
	}

	/** One axis-aligned wire segment, endpoints normalized. */
	private static final class Segment {

		/** The net this segment belongs to. */
		final String netName;
		/** The smaller x endpoint coordinate. */
		final int x1;
		/** The smaller y endpoint coordinate. */
		final int y1;
		/** The larger x endpoint coordinate. */
		final int x2;
		/** The larger y endpoint coordinate. */
		final int y2;

		/**
		 * Normalizes so x1 &lt;= x2 and y1 &lt;= y2.
		 *
		 * @param netName net the segment belongs to
		 * @param from one endpoint
		 * @param to the other endpoint
		 */
		Segment(String netName, LayoutResult.Point from,
				LayoutResult.Point to) {
			this.netName = netName;
			this.x1 = Math.min(from.x, to.x);
			this.y1 = Math.min(from.y, to.y);
			this.x2 = Math.max(from.x, to.x);
			this.y2 = Math.max(from.y, to.y);
		}

		/**
		 * Tells whether this segment runs horizontally.
		 *
		 * @return true if the segment is horizontal
		 */
		boolean horizontal() {
			return y1 == y2;
		}

		/**
		 * The length of this segment.
		 *
		 * @return the segment's Manhattan length in pixels
		 */
		long length() {
			return (long) (x2 - x1) + (y2 - y1);
		}
	} // end of Segment class

	/**
	 * Computes the rubric metrics for one complete layout.
	 *
	 * @param result the layout to score
	 *
	 * @return the computed metrics
	 *
	 * @throws IllegalArgumentException if the layout is not complete
	 */
	public static LayoutMetrics measure(LayoutResult result) {
		if (!result.isComplete()) {
			throw new IllegalArgumentException("metrics need a complete"
					+ " layout: every node placed, every edge routed");
		}
		LayoutGraph graph = result.graph();

		// group edges into nets, flatten routes into segments
		Map<String, List<LayoutGraph.Edge>> nets =
				new LinkedHashMap<String, List<LayoutGraph.Edge>>();
		List<Segment> segments = new ArrayList<Segment>();
		long routedLength = 0;
		for (LayoutGraph.Edge edge : graph.edges()) {
			List<LayoutGraph.Edge> net = nets.get(edge.netName);
			if (net == null) {
				net = new ArrayList<LayoutGraph.Edge>();
				nets.put(edge.netName, net);
			}
			net.add(edge);
			List<LayoutResult.Point> waypoints = result.route(edge);
			for (int i = 1; i < waypoints.size(); i += 1) {
				Segment segment = new Segment(edge.netName,
						waypoints.get(i - 1), waypoints.get(i));
				if (segment.length() > 0) {
					segments.add(segment);
					routedLength += segment.length();
				}
			}
		}

		int overlapCount = countOverlaps(graph, result, segments);

		// crossings: interior intersections between different nets
		int crossingCount = 0;
		Map<String, Integer> perNetCrossings =
				new LinkedHashMap<String, Integer>();
		for (String netName : nets.keySet()) {
			perNetCrossings.put(netName, Integer.valueOf(0));
		}
		for (int first = 0; first < segments.size(); first += 1) {
			for (int second = first + 1; second < segments.size();
					second += 1) {
				Segment a = segments.get(first);
				Segment b = segments.get(second);
				if (a.netName.equals(b.netName)
						|| a.horizontal() == b.horizontal()) {
					continue;
				}
				Segment h = a.horizontal() ? a : b;
				Segment v = a.horizontal() ? b : a;
				if (h.x1 < v.x1 && v.x1 < h.x2
						&& v.y1 < h.y1 && h.y1 < v.y2) {
					crossingCount += 1;
					bump(perNetCrossings, h.netName);
					bump(perNetCrossings, v.netName);
				}
			}
		}
		int maxCrossingsOnOneNet = 0;
		long totalInvolvement = 0;
		for (Integer count : perNetCrossings.values()) {
			totalInvolvement += count.intValue();
			maxCrossingsOnOneNet =
					Math.max(maxCrossingsOnOneNet, count.intValue());
		}
		double crossingsPerNet = nets.isEmpty() ? 0.0
				: (double) totalInvolvement / nets.size();

		// wire length against the per-net Manhattan lower bound, and
		// left-to-right flow with feedback nets exempt
		long manhattanLowerBound = 0;
		int eligibleNets = 0;
		int leftToRightNets = 0;
		for (List<LayoutGraph.Edge> net : nets.values()) {
			int minX = Integer.MAX_VALUE;
			int minY = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			int maxY = Integer.MIN_VALUE;
			boolean feedback = false;
			boolean leftToRight = true;
			for (LayoutGraph.Edge edge : net) {
				LayoutResult.Point start = result.portLocation(
						edge.sourceNode, edge.sourcePort);
				LayoutResult.Point end = result.portLocation(
						edge.targetNode, edge.targetPort);
				minX = Math.min(minX, Math.min(start.x, end.x));
				minY = Math.min(minY, Math.min(start.y, end.y));
				maxX = Math.max(maxX, Math.max(start.x, end.x));
				maxY = Math.max(maxY, Math.max(start.y, end.y));
				feedback |= edge.feedback;
				leftToRight &= end.x >= start.x;
			}
			manhattanLowerBound +=
					(long) (maxX - minX) + (maxY - minY);
			if (!feedback) {
				eligibleNets += 1;
				if (leftToRight) {
					leftToRightNets += 1;
				}
			}
		}
		double wireLengthRatio;
		if (manhattanLowerBound == 0) {
			wireLengthRatio = routedLength == 0 ? 1.0
					: Double.POSITIVE_INFINITY;
		} else {
			wireLengthRatio =
					(double) routedLength / manhattanLowerBound;
		}
		double leftToRightFraction = eligibleNets == 0 ? 1.0
				: (double) leftToRightNets / eligibleNets;

		return new LayoutMetrics(overlapCount, crossingCount,
				crossingsPerNet, maxCrossingsOnOneNet, routedLength,
				manhattanLowerBound, wireLengthRatio,
				leftToRightFraction,
				boundingBoxArea(graph, result, segments));
	}

	/**
	 * Counts the rubric's hard-fail geometry: collinear overlaps
	 * between segments of different nets, and segments passing through
	 * an element body's interior (touching a body's boundary - where
	 * ports attach - is fine).
	 *
	 * @param graph the placement problem
	 * @param result the layout under measurement
	 * @param segments all nonzero-length wire segments
	 *
	 * @return the overlap count
	 */
	private static int countOverlaps(LayoutGraph graph,
			LayoutResult result, List<Segment> segments) {
		int overlaps = 0;
		for (int first = 0; first < segments.size(); first += 1) {
			for (int second = first + 1; second < segments.size();
					second += 1) {
				Segment a = segments.get(first);
				Segment b = segments.get(second);
				if (a.netName.equals(b.netName)
						|| a.horizontal() != b.horizontal()) {
					continue;
				}
				if (a.horizontal() && a.y1 == b.y1
						&& Math.max(a.x1, b.x1) < Math.min(a.x2, b.x2)) {
					overlaps += 1;
				} else if (!a.horizontal() && a.x1 == b.x1
						&& Math.max(a.y1, b.y1) < Math.min(a.y2, b.y2)) {
					overlaps += 1;
				}
			}
		}
		for (Segment segment : segments) {
			for (LayoutGraph.Node node : graph.nodes()) {
				LayoutResult.Point position = result.position(node.id);
				int left = position.x;
				int right = position.x + node.width;
				int top = position.y;
				int bottom = position.y + node.height;
				if (segment.horizontal()) {
					if (top < segment.y1 && segment.y1 < bottom
							&& Math.max(segment.x1, left)
									< Math.min(segment.x2, right)) {
						overlaps += 1;
					}
				} else {
					if (left < segment.x1 && segment.x1 < right
							&& Math.max(segment.y1, top)
									< Math.min(segment.y2, bottom)) {
						overlaps += 1;
					}
				}
			}
		}
		return overlaps;
	}

	/**
	 * Computes the area of the bounding box around all element bodies
	 * and wire waypoints.
	 *
	 * @param graph the placement problem
	 * @param result the layout under measurement
	 * @param segments all nonzero-length wire segments
	 *
	 * @return the bounding-box area in square pixels, 0 for an empty
	 *		layout
	 */
	private static long boundingBoxArea(LayoutGraph graph,
			LayoutResult result, List<Segment> segments) {
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		for (LayoutGraph.Node node : graph.nodes()) {
			LayoutResult.Point position = result.position(node.id);
			minX = Math.min(minX, position.x);
			minY = Math.min(minY, position.y);
			maxX = Math.max(maxX, position.x + node.width);
			maxY = Math.max(maxY, position.y + node.height);
		}
		for (Segment segment : segments) {
			minX = Math.min(minX, segment.x1);
			minY = Math.min(minY, segment.y1);
			maxX = Math.max(maxX, segment.x2);
			maxY = Math.max(maxY, segment.y2);
		}
		if (minX > maxX || minY > maxY) {
			return 0;
		}
		return (long) (maxX - minX) * (maxY - minY);
	}

	/**
	 * Increments one net's crossing-involvement count.
	 *
	 * @param counts per-net counts
	 * @param netName the net to increment
	 */
	private static void bump(Map<String, Integer> counts,
			String netName) {
		counts.put(netName,
				Integer.valueOf(counts.get(netName).intValue() + 1));
	}

	/**
	 * Says which launch thresholds this layout misses. Compactness is
	 * excluded: its threshold is relative to a hand-drawn golden and
	 * is applied by the caller against {@link #boundingBoxArea}.
	 *
	 * @return one description per missed threshold; empty means the
	 *		layout meets the numeric rubric
	 */
	public List<String> rubricFailures() {
		List<String> failures = new ArrayList<String>();
		if (overlapCount > MAX_OVERLAPS) {
			failures.add("hard fail: " + overlapCount
					+ " overlap(s) - collinear wire overlap or wire"
					+ " through an element body");
		}
		if (crossingsPerNet > MAX_CROSSINGS_PER_NET) {
			failures.add("average crossings per net "
					+ crossingsPerNet + " exceeds "
					+ MAX_CROSSINGS_PER_NET);
		}
		if (maxCrossingsOnOneNet > MAX_CROSSINGS_ON_ONE_NET) {
			failures.add("worst net has " + maxCrossingsOnOneNet
					+ " crossings, more than "
					+ MAX_CROSSINGS_ON_ONE_NET);
		}
		if (wireLengthRatio > MAX_WIRE_LENGTH_RATIO) {
			failures.add("routed length is " + wireLengthRatio
					+ "x the Manhattan lower bound, more than "
					+ MAX_WIRE_LENGTH_RATIO + "x");
		}
		if (leftToRightFraction < MIN_LEFT_TO_RIGHT_FRACTION) {
			failures.add("only " + leftToRightFraction
					+ " of non-feedback nets flow left-to-right,"
					+ " below " + MIN_LEFT_TO_RIGHT_FRACTION);
		}
		return failures;
	}

	/**
	 * Tells whether the layout passes the quality rubric.
	 *
	 * @return true if the layout meets every numeric launch threshold
	 */
	public boolean meetsRubric() {
		return rubricFailures().isEmpty();
	}

} // end of LayoutMetrics class
