package jls;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jls.elem.Element;

/**
 * A uniform-grid spatial index over element bounding boxes (issues #3, #17).
 *
 * Elements are stored in every grid cell their index bounds
 * ({@link Element#getIndexBounds()}) overlap, so a query for a rectangle
 * only visits the cells that rectangle covers instead of scanning the whole
 * circuit. Queries return a superset check against the recorded bounds, and
 * callers apply their exact geometric predicates (contains/intersects/
 * touches) to the candidates, so accept/reject decisions are identical to a
 * full scan.
 *
 * The index is deliberately forgiving about staleness: any structural or
 * geometric change may simply {@link #invalidate()} it, and the next query
 * rebuilds from the authoritative element set in one pass. Only the
 * per-mouse-event drag path keeps it incrementally up to date via
 * {@link #update(Element)}, which is what turns O(S x N) drag work into
 * O(S).
 *
 * Query correctness -- that this grid scheme returns exactly the elements
 * a brute-force boundsTouch scan would, so the exact-predicate callers
 * behave identically to full scans -- is machine-checked: THEOREM 1
 * (query-parity) in proofs/SpatialIndexCorrectness.agda, whose modelling
 * assumptions (interval clamping, floorDiv monotonicity, the boundsTouch
 * formula) are pinned to this class by jls.ProofBridgeTest.
 */
public final class SpatialIndex {

	/**
	 * Cell edge in pixels. A multiple of the 12px snap spacing: big enough
	 * that typical elements cover only a few cells, small enough that a
	 * cell holds few elements.
	 */
	private static final int CELL = 4 * JLSInfo.spacing;

	/** Grid cells, keyed by packed cell coordinates, holding the elements that overlap them. */
	private final Map<Long, List<Element>> cells = new HashMap<Long, List<Element>>();
	/** The bounds each tracked element was indexed under. */
	private final Map<Element, Rectangle> indexed = new HashMap<Element, Rectangle>();
	/** True when the index is stale and must be rebuilt before answering queries. */
	private boolean dirty = true;

	/**
	 * Create an empty index. It starts dirty, so the first query pays
	 * one rebuild pass over the authoritative element set.
	 */
	public SpatialIndex() {
	} // end of constructor

	/**
	 * Pack a cell coordinate pair into a map key.
	 *
	 * @param cx The cell x-coordinate.
	 * @param cy The cell y-coordinate.
	 *
	 * @return a single long combining both coordinates.
	 */
	private static long key(int cx, int cy) {

		return ((long) cx << 32) ^ (cy & 0xffffffffL);
	} // end of key method

	/**
	 * Whether the index needs a rebuild before it can answer queries.
	 *
	 * @return true if the index is stale and must be rebuilt, false if
	 *         it is current.
	 */
	public boolean isDirty() {

		return dirty;
	} // end of isDirty method

	/**
	 * Mark the index stale. Cheap; the next query pays one rebuild pass.
	 */
	public void invalidate() {

		dirty = true;
	} // end of invalidate method

	/**
	 * Rebuild the index from scratch over the given elements.
	 *
	 * @param elements The authoritative element collection.
	 */
	public void rebuild(Collection<Element> elements) {

		cells.clear();
		indexed.clear();
		for (Element el : elements) {
			insert(el);
		}
		dirty = false;
	} // end of rebuild method

	/**
	 * Add an element to the index under its current bounds.
	 *
	 * @param el The element to add.
	 */
	public void insert(Element el) {

		Rectangle b = el.getIndexBounds();
		indexed.put(el, b);
		int cx1 = Math.floorDiv(b.x, CELL);
		int cy1 = Math.floorDiv(b.y, CELL);
		int cx2 = Math.floorDiv(b.x + Math.max(b.width, 0), CELL);
		int cy2 = Math.floorDiv(b.y + Math.max(b.height, 0), CELL);
		for (int cx = cx1; cx <= cx2; cx += 1) {
			for (int cy = cy1; cy <= cy2; cy += 1) {
				Long k = key(cx, cy);
				List<Element> cell = cells.get(k);
				if (cell == null) {
					cell = new ArrayList<Element>(4);
					cells.put(k, cell);
				}
				cell.add(el);
			}
		}
	} // end of insert method

	/**
	 * Remove an element from the index (using the bounds it was indexed
	 * under, which may differ from its current bounds).
	 *
	 * @param el The element to remove.
	 */
	public void remove(Element el) {

		Rectangle b = indexed.remove(el);
		if (b == null) {
			return;
		}
		int cx1 = Math.floorDiv(b.x, CELL);
		int cy1 = Math.floorDiv(b.y, CELL);
		int cx2 = Math.floorDiv(b.x + Math.max(b.width, 0), CELL);
		int cy2 = Math.floorDiv(b.y + Math.max(b.height, 0), CELL);
		for (int cx = cx1; cx <= cx2; cx += 1) {
			for (int cy = cy1; cy <= cy2; cy += 1) {
				Long k = key(cx, cy);
				List<Element> cell = cells.get(k);
				if (cell != null) {
					cell.remove(el);
					if (cell.isEmpty()) {
						cells.remove(k);
					}
				}
			}
		}
	} // end of remove method

	/**
	 * Refresh a tracked element's cells after its geometry changed.
	 * Untracked elements are ignored (a pending rebuild will pick them up).
	 *
	 * @param el The element that moved or resized.
	 */
	public void update(Element el) {

		if (!indexed.containsKey(el)) {
			return;
		}
		remove(el);
		insert(el);
	} // end of update method

	/**
	 * All elements whose recorded index bounds intersect or touch the given
	 * rectangle. The caller applies exact predicates to the result.
	 *
	 * @param rect The query rectangle.
	 *
	 * @return the candidate set.
	 */
	public Set<Element> query(Rectangle rect) {

		Set<Element> result = new HashSet<Element>();
		int cx1 = Math.floorDiv(rect.x, CELL);
		int cy1 = Math.floorDiv(rect.y, CELL);
		int cx2 = Math.floorDiv(rect.x + Math.max(rect.width, 0), CELL);
		int cy2 = Math.floorDiv(rect.y + Math.max(rect.height, 0), CELL);

		// a huge query rectangle (e.g. select-all rubber band) can cover
		// more cells than there are elements; walk the elements instead
		long cellCount = (long) (cx2 - cx1 + 1) * (cy2 - cy1 + 1);
		if (cellCount > indexed.size()) {
			for (Map.Entry<Element, Rectangle> e : indexed.entrySet()) {
				if (boundsTouch(e.getValue(), rect)) {
					result.add(e.getKey());
				}
			}
			return result;
		}

		for (int cx = cx1; cx <= cx2; cx += 1) {
			for (int cy = cy1; cy <= cy2; cy += 1) {
				List<Element> cell = cells.get(key(cx, cy));
				if (cell == null) {
					continue;
				}
				for (Element el : cell) {
					if (boundsTouch(indexed.get(el), rect)) {
						result.add(el);
					}
				}
			}
		}
		return result;
	} // end of query method

	/**
	 * Rectangle intersection that, unlike Rectangle.intersects, also counts
	 * zero-area contact (shared edges, zero-width wires): put-alignment
	 * checks depend on edge-touching elements being candidates.
	 *
	 * @param a The first rectangle.
	 * @param b The second rectangle.
	 *
	 * @return true if the rectangles overlap or touch, false otherwise.
	 */
	private static boolean boundsTouch(Rectangle a, Rectangle b) {

		return a.x <= b.x + b.width && b.x <= a.x + a.width
				&& a.y <= b.y + b.height && b.y <= a.y + a.height;
	} // end of boundsTouch method

} // end of SpatialIndex class
