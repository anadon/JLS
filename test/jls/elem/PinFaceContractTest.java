package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.core.Orientation;
import jls.edit.SwingTextMetrics;

/**
 * The pin-face contract (issue #78, pin-face stage of the descriptor
 * plan): every put of every registered element type reports a face -
 * the outward normal of the element edge it sits on, as a
 * {@link jls.core.Orientation} - and that face is geometrically honest:
 * the put's center lies on the {@link Element#getRect()} edge the face
 * names. The face is what a wire router or a rotation-as-transform
 * needs to know about a pin without re-deriving element geometry.
 *
 * <p>Registry-driven totality, following the
 * {@code jls.edit.PaletteContractTest} precedent: the walk is over
 * {@link ElementRegistry#all()}, so a newly registered element type is
 * automatically under contract - it either satisfies the face
 * invariants or must be added to one of the small documented exception
 * sets below, making the gap visible in review instead of latent.
 *
 * <p>Gates additionally carry <i>declared</i> faces, seeded at put
 * creation in {@link Gate#init}: inputs face opposite the gate's
 * orientation, the output faces with it. The declared face must agree
 * with the geometric derivation at every orientation, so the
 * declaration can later replace derivation without a behavior change.
 *
 * <p>Everything here is headless: elements are created raw through
 * {@link ElementType#create} and initialized with the same
 * BufferedImage-backed {@link SwingTextMetrics} the orientation
 * geometry baseline ({@code OrientationGeometryTest}) uses.
 */
class PinFaceContractTest {

	/**
	 * Registered types whose raw create-then-init cannot run headlessly
	 * because init() requires load-time state a blank element lacks:
	 * JumpStart's init registers the jump name with the circuit and
	 * throws when the name is unset; SubCircuit's init walks the pins
	 * of the inner circuit, which only loading attaches. Their puts are
	 * covered indirectly: both use the same edge geometry as JumpEnd
	 * and the pins, and their faces ride the shared derivation in
	 * {@link Put#getFace()}. Pinned exactly - if a migration makes one
	 * constructible raw, it must leave this set and join the contract.
	 */
	private static final Set<String> KNOWN_UNINITIALIZABLE =
			Set.of("JumpStart", "SubCircuit");

	/**
	 * Registered types that have no puts after a raw init, enumerated
	 * for totality but exempt from the face assertions: Text is not
	 * even a LogicElement, WireEnd's connectivity is wires rather than
	 * puts, the signal sources (SigGen, TestGen) only grow outputs
	 * when a dialog or load attaches watched pins, and TruthTable's
	 * puts come from its loaded table columns. Pinned exactly so a
	 * type that gains puts automatically joins the contract.
	 */
	private static final Set<String> KNOWN_PUTLESS =
			Set.of("SigGen", "TestGen", "Text", "TruthTable", "WireEnd");

	/**
	 * Puts whose center is on no edge of their element's bounding
	 * rectangle, as "tag putName" pairs: {@link Put#getFace()} throws
	 * for these (a wire would visually enter the element body). Empty
	 * today - every registered element keeps its puts on its rect edge
	 * - and pinned exactly so an off-edge put shows up in review as a
	 * documented geometry exception instead of a silent contract hole.
	 */
	private static final Set<String> KNOWN_OFF_EDGE = Set.of();

	/** The image backing the shared metrics; kept alive for the run. */
	private static BufferedImage image;

	/** The graphics context backing the shared metrics. */
	private static Graphics2D graphics;

	/** The text metrics elements are initialized with. */
	private static jls.core.TextMetrics metrics;

	/**
	 * Create the BufferedImage-backed text metrics all elements are
	 * initialized with (the OrientationGeometryTest apparatus).
	 */
	@BeforeAll
	static void createMetrics() {

		image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
		graphics = image.createGraphics();
		metrics = SwingTextMetrics.of(graphics);
	}

	/**
	 * Release the graphics context backing the shared metrics.
	 */
	@AfterAll
	static void disposeMetrics() {

		graphics.dispose();
	}

	/**
	 * Create a blank element of the given type in its own circuit and
	 * run init with the shared metrics, or return null if the type is
	 * one whose raw init is documented not to run headlessly.
	 *
	 * @param type The registered element type.
	 * @param uninitializable Collects tags whose raw init threw.
	 *
	 * @return the initialized element, or null if init threw.
	 */
	private static Element initialized(ElementType type,
			Set<String> uninitializable) {

		Circuit circuit = new Circuit("pinface");
		Element made = type.create(circuit);
		if (made instanceof Group group) {
			// a group with zero bit ranges is a degenerate one-grid-unit
			// box neither the creation dialog nor the loader can produce
			// (both require at least one range), and its lone bundle put
			// lands exactly on a rect corner; seed the smallest real
			// group - two single-bit ranges - the way the dialog does
			group.setBits(2);
			group.addRange(new Group.Entry(1, 1));
			group.addRange(new Group.Entry(0, 0));
		}
		try {
			made.init(metrics);
		} catch (Exception ex) {
			uninitializable.add(type.tag());
			return null;
		}
		return made;
	}

	/**
	 * Assert every put of the element has a face on the rect edge that
	 * face names, collecting documented off-edge puts instead of
	 * failing on them.
	 *
	 * @param tag The registered tag, for messages and the off-edge set.
	 * @param el The initialized element.
	 * @param offEdge Collects "tag putName" for puts on no edge.
	 */
	private static void assertFacesOnEdges(String tag, Element el,
			Set<String> offEdge) {

		for (Put put : el.getAllPuts()) {
			Orientation face;
			try {
				face = put.getFace();
			} catch (IllegalStateException ex) {
				offEdge.add(tag + " " + put.getName());
				continue;
			}
			assertNotNull(face, "tag '" + tag + "' put " + put.getName()
					+ ": getFace() must never return null");
			assertOnEdge(tag, el, put, face);
		}
	}

	/**
	 * Assert one put's center lies on the element-rect edge its face
	 * names (on the edge line, within the edge's extent).
	 *
	 * @param tag The registered tag, for messages.
	 * @param el The element owning the put.
	 * @param put The put to check.
	 * @param face The face the put reported.
	 */
	private static void assertOnEdge(String tag, Element el, Put put,
			Orientation face) {

		jls.core.Bounds r = el.getRect();
		int cx = put.getX();
		int cy = put.getY();
		String at = "tag '" + tag + "' put " + put.getName() + " at ("
				+ cx + "," + cy + ") faces " + face + " but is not on that "
				+ "edge of " + r;
		switch (face) {
		case LEFT:
			assertEquals(r.x(), cx, at);
			assertTrue(cy >= r.y() && cy <= r.y() + r.height(), at);
			break;
		case RIGHT:
			assertEquals(r.x() + r.width(), cx, at);
			assertTrue(cy >= r.y() && cy <= r.y() + r.height(), at);
			break;
		case UP:
			assertEquals(r.y(), cy, at);
			assertTrue(cx >= r.x() && cx <= r.x() + r.width(), at);
			break;
		default: // DOWN
			assertEquals(r.y() + r.height(), cy, at);
			assertTrue(cx >= r.x() && cx <= r.x() + r.width(), at);
			break;
		}
	}

	/**
	 * The geometric face derivation, replicated from
	 * {@link Put#getFace()}'s documented priority (left, right, top,
	 * bottom - LEFT/RIGHT win corner ties) so the test can compare a
	 * gate's declared face against what pure geometry says.
	 *
	 * @param el The element owning the put.
	 * @param put The put to derive a face for.
	 *
	 * @return the derived face.
	 */
	private static Orientation derive(Element el, Put put) {

		jls.core.Bounds r = el.getRect();
		int cx = put.getX();
		int cy = put.getY();
		if (cx == r.x()) {
			return Orientation.LEFT;
		}
		if (cx == r.x() + r.width()) {
			return Orientation.RIGHT;
		}
		if (cy == r.y()) {
			return Orientation.UP;
		}
		if (cy == r.y() + r.height()) {
			return Orientation.DOWN;
		}
		throw new IllegalStateException("put " + put.getName() + " of "
				+ el.getClass().getSimpleName() + " is on no edge");
	}

	/**
	 * The element's faces as a sorted multiset (put order is a HashSet
	 * walk, so comparisons must not depend on it).
	 *
	 * @param el The element to read faces from.
	 *
	 * @return the sorted face list.
	 */
	private static List<Orientation> faceMultiset(Element el) {

		List<Orientation> faces = new ArrayList<Orientation>();
		for (Put put : el.getAllPuts()) {
			faces.add(put.getFace());
		}
		faces.sort(null);
		return faces;
	}

	/**
	 * The given faces each turned a quarter turn, as a sorted multiset.
	 *
	 * @param faces The faces before the turn.
	 * @param ccw True for a counterclockwise turn, false for clockwise.
	 *
	 * @return the sorted turned face list.
	 */
	private static List<Orientation> turned(List<Orientation> faces,
			boolean ccw) {

		List<Orientation> next = new ArrayList<Orientation>();
		for (Orientation face : faces) {
			next.add(ccw ? face.ccw() : face.cw());
		}
		next.sort(null);
		return next;
	}

	/**
	 * Contract (a)+(b): every put of every registered type has a
	 * non-null face naming the rect edge the put's center lies on, with
	 * the documented exception sets pinned exactly in both directions.
	 */
	@Test
	void everyPutHasAFaceOnItsElementEdge() {

		Set<String> uninitializable = new TreeSet<String>();
		Set<String> putless = new TreeSet<String>();
		Set<String> offEdge = new TreeSet<String>();
		for (ElementType type : ElementRegistry.all()) {
			Element made = initialized(type, uninitializable);
			if (made == null) {
				continue;
			}
			if (made.getAllPuts().isEmpty()) {
				putless.add(type.tag());
				continue;
			}
			assertFacesOnEdges(type.tag(), made, offEdge);
		}
		assertEquals(new TreeSet<String>(KNOWN_UNINITIALIZABLE),
				uninitializable,
				"the types whose raw init cannot run headlessly must be "
						+ "exactly the documented set");
		assertEquals(new TreeSet<String>(KNOWN_PUTLESS), putless,
				"the types with no puts after raw init must be exactly "
						+ "the documented set");
		assertEquals(new TreeSet<String>(KNOWN_OFF_EDGE), offEdge,
				"puts off every rect edge must be exactly the documented "
						+ "set (getFace() throws for these)");
	}

	/**
	 * Contract (c): rotating a rotatable element one quarter turn
	 * permutes its face multiset by exactly {@link Orientation#ccw()}
	 * (direction LEFT) or {@link Orientation#cw()} (direction RIGHT),
	 * with the edge invariant re-checked at every step. Four turns each
	 * way visit every reachable orientation and return to the start.
	 */
	@Test
	void rotationPermutesFacesByAQuarterTurn() {

		Set<String> uninitializable = new TreeSet<String>();
		for (ElementType type : ElementRegistry.all()) {
			Element made = initialized(type, uninitializable);
			if (made == null || made.getAllPuts().isEmpty()
					|| !(made instanceof Rotatable rotatable)
					|| !rotatable.canRotate()) {
				continue;
			}
			List<Orientation> start = faceMultiset(made);
			List<Orientation> before = start;
			for (Orientation direction : new Orientation[] {
					Orientation.LEFT, Orientation.RIGHT}) {
				boolean ccw = direction == Orientation.LEFT;
				for (int step = 1; step <= 4; step += 1) {
					rotatable.rotate(direction, metrics);
					assertFacesOnEdges(type.tag(), made,
							new TreeSet<String>());
					List<Orientation> after = faceMultiset(made);
					assertEquals(turned(before, ccw), after,
							"tag '" + type.tag() + "': rotate("
									+ direction + ") step " + step
									+ " must permute every face by one "
									+ "quarter turn");
					before = after;
				}
				assertEquals(start, before, "tag '" + type.tag()
						+ "': four " + direction
						+ " rotations must restore the face multiset");
			}
		}
	}

	/**
	 * Contract (d): a gate's declared faces - seeded at put creation in
	 * {@link Gate#init} - agree with both the orientation rule (inputs
	 * face opposite the gate, the output faces with it) and the pure
	 * geometric derivation, at all four orientations. This is what lets
	 * declaration later replace derivation with no behavior change.
	 */
	@Test
	void gateDeclaredFacesMatchDerivationAtAllOrientations() {

		Set<String> gates = new TreeSet<String>();
		for (ElementType type : ElementRegistry.all()) {
			if (!Gate.class.isAssignableFrom(type.elementClass())) {
				continue;
			}
			gates.add(type.tag());
			for (Orientation orientation : Orientation.values()) {
				Circuit circuit = new Circuit("pinface");
				Gate gate = (Gate) type.create(circuit);
				gate.setOrientation(orientation);
				gate.init(metrics);
				for (Put put : gate.getAllPuts()) {
					Orientation expected = put instanceof Output
							? orientation : orientation.flipped();
					String at = "tag '" + type.tag() + "' at "
							+ orientation + ", put " + put.getName();
					assertEquals(expected, put.getFace(),
							at + ": face must follow the gate rule");
					assertEquals(derive(gate, put), put.getFace(),
							at + ": declared face must equal the "
									+ "geometric derivation");
				}
			}
		}
		assertTrue(gates.size() >= 8,
				"the registry must still contain the eight gate kinds; "
						+ "found only " + gates);
	}
}
