package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;

/**
 * Collision semantics of a hollow square drawn with four wires versus a
 * filled square container element (a SubCircuit block), through the
 * exact predicate the drag overlap check uses --
 * {@link Element#intersects(Element)}, as called by
 * SimpleEditor.overlap() on each dragged element against its spatial
 * index candidates (#3, #17).
 *
 * The contract under test:
 *
 *  - a square OUTLINE of wires collides only on its edges: an element
 *    dropped in the hollow interior overlaps nothing, an element
 *    crossing an edge overlaps that wire, and an element covering a
 *    corner overlaps the corner's WireEnd (wires deliberately exclude
 *    their endpoints, so corner contact is attributed to the end);
 *
 *  - a square CONTAINER element collides across its whole filled area,
 *    interior included, because every non-wire element's collision
 *    surface is its bounding rectangle;
 *
 *  - the spatial index shortlists by bounding box only (THEOREM 1 in
 *    proofs/SpatialIndexCorrectness.agda: candidates are a superset,
 *    the exact predicate decides). For axis-aligned wires the boxes
 *    are thin bands, so the hollow interior is not even a candidate;
 *    for a diagonal wire the box covers area the wire does not, and
 *    the exact predicate is what rejects the non-collision.
 */
class HollowVsFilledCollisionTest {

	private static final int S = JLSInfo.spacing; // 12

	/** A rectangular probe element standing in for a dragged selection. */
	private static SubCircuit probe(int x, int y, int w, int h) {
		SubCircuit p = new SubCircuit(null);
		p.setXY(x, y);
		p.width = w;
		p.height = h;
		return p;
	}

	private WireEnd endA, endB, endC, endD;      // square corners
	private Wire top, right, bottom, left;       // square edges
	private WireEnd diagE1, diagE2;              // a separate diagonal wire
	private Wire diag;
	private SubCircuit container;                // the filled square block
	private Circuit circuit;

	/**
	 * corners A(120,120) B(216,120) C(216,216) D(120,216); container at
	 * x 360..456, y 120..216; diagonal wire (480,120)-(576,216). All far
	 * enough apart that candidate sets below are exact.
	 */
	private void build() {
		circuit = new Circuit("hollowVsFilled");

		endA = corner(120, 120);
		endB = corner(216, 120);
		endC = corner(216, 216);
		endD = corner(120, 216);
		top = edge(endA, endB);
		right = edge(endB, endC);
		bottom = edge(endC, endD);
		left = edge(endD, endA);

		container = probe(360, 120, 8 * S, 8 * S);
		circuit.addElement(container);

		diagE1 = corner(480, 120);
		diagE2 = corner(576, 216);
		diag = edge(diagE1, diagE2);
	}

	private WireEnd corner(int x, int y) {
		WireEnd end = new WireEnd(circuit);
		end.setXY(x, y);
		circuit.addElement(end);
		return end;
	}

	private Wire edge(WireEnd e1, WireEnd e2) {
		Wire wire = new Wire(e1, e2);
		e1.addWire(wire);
		e2.addWire(wire);
		circuit.addElement(wire);
		return wire;
	}

	/**
	 * An element well inside the wire square's hollow overlaps none of
	 * the four wires and none of the corners -- and the hollow's center
	 * point is contained by nothing, so hover finds nothing there either.
	 */
	@Test
	void hollowInteriorDoesNotCollide() {
		build();
		SubCircuit inside = probe(156, 156, 2 * S, 2 * S);

		for (Wire wire : new Wire[] { top, right, bottom, left }) {
			assertFalse(inside.intersects(wire),
					"interior probe must not collide with an outline wire");
		}
		for (WireEnd end : new WireEnd[] { endA, endB, endC, endD }) {
			assertFalse(inside.intersects(end),
					"interior probe must not collide with a corner end");
		}
		for (Element el : circuit.getElements()) {
			assertFalse(el.contains(168, 168),
					"nothing may claim the hollow center point: " + el);
		}
	}

	/**
	 * An element crossing one edge of the square collides with exactly
	 * that wire.
	 */
	@Test
	void edgeCrossingCollidesWithThatWireOnly() {
		build();
		SubCircuit acrossTop = probe(150, 108, 2 * S, 2 * S);

		assertTrue(acrossTop.intersects(top),
				"probe across the top edge must collide with the top wire");
		assertFalse(acrossTop.intersects(right), "right edge is far away");
		assertFalse(acrossTop.intersects(bottom), "bottom edge is far away");
		assertFalse(acrossTop.intersects(left), "left edge is far away");
	}

	/**
	 * An element covering a corner collides with the corner's WireEnd,
	 * not with the wires: Wire.intersects deliberately excludes its
	 * endpoints, so corner contact is the end's to report. (In the drag
	 * paths that end IS part of the collision surface -- a corner drop
	 * is still flagged, just attributed to the end.)
	 */
	@Test
	void cornerCollisionIsAttributedToTheWireEnd() {
		build();
		SubCircuit onCorner = probe(108, 108, 2 * S, 2 * S);

		assertTrue(onCorner.intersects(endA),
				"probe on the corner must collide with the corner end");
		assertFalse(onCorner.intersects(top),
				"wires exclude their endpoints: corner is not the top wire's");
		assertFalse(onCorner.intersects(left),
				"wires exclude their endpoints: corner is not the left wire's");
	}

	/**
	 * The container element is filled: the same interior placement that
	 * is legal inside the wire square collides inside the block, and the
	 * block's center point is contained (hover would find it).
	 */
	@Test
	void containerInteriorDoesCollide() {
		build();
		SubCircuit inside = probe(396, 156, 2 * S, 2 * S);

		assertTrue(inside.intersects(container),
				"a probe inside the container block must collide");
		assertTrue(container.intersects(inside),
				"and symmetrically from the container's side");
		assertTrue(container.contains(408, 168),
				"the container contains its center point");
	}

	/**
	 * The filled-square semantics above are structural, not incidental:
	 * SubCircuit takes its whole collision surface (intersects/contains/
	 * getRect) unmodified from Element, like every non-wire element. If
	 * an override ever appears, this test flags that the hollow-vs-filled
	 * contract needs re-examination.
	 */
	@Test
	void containerCollisionSurfaceIsTheInheritedRectangle()
			throws Exception {
		assertEquals(Element.class,
				SubCircuit.class.getMethod("intersects", Element.class)
						.getDeclaringClass(),
				"SubCircuit must inherit Element.intersects");
		assertEquals(Element.class,
				SubCircuit.class.getMethod("contains", int.class, int.class)
						.getDeclaringClass(),
				"SubCircuit must inherit Element.contains");
		assertEquals(Element.class,
				SubCircuit.class.getMethod("getRect").getDeclaringClass(),
				"SubCircuit must inherit Element.getRect");
	}

	/**
	 * The index-vs-predicate division of labor (THEOREM 1's shape) on
	 * these exact fixtures. Axis-aligned outline wires have thin bounding
	 * bands, so the hollow interior yields NO candidates at all -- the
	 * index never even shortlists the square for an interior drop. An
	 * edge placement shortlists exactly the crossed wire. A corner
	 * placement shortlists the two adjacent wires and the corner end,
	 * and the exact predicate then rejects the wires (endpoint
	 * exclusion) while keeping the end.
	 */
	@Test
	void indexShortlistsMatchOutlineGeometry() {
		build();

		assertEquals(Set.of(),
				circuit.elementsNear(probe(156, 156, 2 * S, 2 * S).getRect()),
				"hollow interior must produce no candidates");

		assertEquals(Set.of(top),
				circuit.elementsNear(probe(150, 108, 2 * S, 2 * S).getRect()),
				"edge placement must shortlist exactly the crossed wire");

		Set<Element> corner = circuit
				.elementsNear(probe(108, 108, 2 * S, 2 * S).getRect());
		assertEquals(Set.of(top, left, endA), corner,
				"corner placement must shortlist adjacent wires and the end");
	}

	/**
	 * A diagonal wire is where bounding boxes over-approximate: its box
	 * covers the whole square between its endpoints, so a probe in the
	 * off-diagonal region IS a candidate -- and the exact predicate is
	 * what rejects the collision. Candidate supersets plus exact
	 * predicates are precisely the contract THEOREM 1 (query-parity)
	 * verifies; this pins the concrete case where they differ.
	 */
	@Test
	void diagonalWireIsCandidateButNotCollisionOffTheDiagonal() {
		build();
		SubCircuit offDiagonal = probe(552, 132, 2 * S, 2 * S);

		Set<Element> candidates = new HashSet<Element>(
				circuit.elementsNear(offDiagonal.getRect()));
		assertTrue(candidates.contains(diag),
				"the diagonal wire's bounding box makes it a candidate");
		assertFalse(offDiagonal.intersects(diag),
				"but the exact predicate rejects the off-diagonal placement");

		Rectangle onPath = new Rectangle(516, 144, 2 * S, 2 * S);
		SubCircuit onDiagonal = probe(onPath.x, onPath.y, onPath.width,
				onPath.height);
		assertTrue(circuit.elementsNear(onPath).contains(diag),
				"an on-diagonal placement is also a candidate");
		assertTrue(onDiagonal.intersects(diag),
				"and the exact predicate confirms the collision");
	}
}
