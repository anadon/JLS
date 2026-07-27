package jls.elem;

import java.util.*;

import org.jspecify.annotations.Nullable;

import jls.*;
import jls.core.Orientation;

/**
 * Superclass for input and output points.
 * Contains common data and methods.
 *
 * @author David A. Poplawski
 */
public abstract sealed class Put
		permits Input, Output {

	// properties
	/** The name of this put; null for the invisible-input sentinel that
	 * marks a wire end as internally attached (see SimpleEditor). */
	protected @Nullable String name;	// name
	/** The element this put is a part of; null for the invisible-input
	 * sentinel that marks a wire end as internally attached. */
	protected @Nullable LogicElement element;	// the element it is a part of
	/** The x-coordinate of the center of this put relative to the element. */
	protected int xr;					// x-coordinate of center relative to element
	/** The y-coordinate of the center of this put relative to the element. */
	protected int yr;					// y-coordinate of center relative to element
	/** The x-coordinate saved by savePosition, for restorePosition. */
	private int savex;
	/** The y-coordinate saved by savePosition, for restorePosition. */
	private int savey;
	/** The number of bits in this put (0 implies arbitrary). */
	protected int bits;					// number of bits
	/** True if this put is touching a WireEnd. */
	private boolean touching = false;	// touching a WireEnd?
	/** The WireEnd this put is attached to, or null if unattached. */
	private @Nullable WireEnd wireEnd = null;	// the WireEnd this put attached to
	/** The copy of this put, to help cut/paste (null until copied). */
	protected @Nullable Put myCopy;				// to help cut/paste
	/** The declared face of this put (issue #78), or null to derive it
	 * geometrically from the owning element's bounding rectangle. */
	private @Nullable Orientation face;

	/**
	 * Create a new put.
	 *
	 * @param name The name of this put (e.g., "input0").
	 * @param element The element this put is part of.
	 * @param xr The x-coordinate of the center of the put relative to the upper left
	 * 		corner of the element this put is in.
	 * @param yr The y-coordinate of the center of the put relative to the upper left
	 * 		corner of the element this put is in.
	 * @param bits The number of bits in this put.  0 implies arbitrary.
	 */
	public Put(@Nullable String name, @Nullable LogicElement element,
			int xr, int yr, int bits) {

		this.name = name;
		this.element = element;
		this.xr = xr;
		this.yr = yr;
		this.bits = bits;
	} // end of constructor

	/**
	 * Return a string version of the properties of this element.
	 *
	 * @return the string.
	 */
	@Override
	public String toString() {

		return name + ",bits=" + bits + ",x=" + xr + ",y=" + yr;
	} // end of toString method

	/**
	 * Get the put's name;
	 *
	 * @return the put's name.
	 *
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#initInputsReachesInsideSubcircuits()
	 * @jls.testedby jls.elem.GroupOrientationTest#puts()
	 * @jls.testedby jls.elem.OrientationGeometryTest#describe()
	 */
	public @Nullable String getName() {

		return name;
	} // end of getName method

	/**
	 * The element this put belongs to, asserted present. Positional and
	 * geometric queries are never made on the invisible-input sentinel
	 * (whose element is null), so a null here is a programming error.
	 *
	 * @return the owning element, never null.
	 */
	private LogicElement requireElement() {

		if (element == null) {
			throw new IllegalStateException(
					"put has no element (invisible-input sentinel)");
		}
		return element;
	} // end of requireElement method

	/**
	 * Get put's x-coordinate.
	 *
	 * @return The x-coordinate.
	 *
	 * @jls.testedby jls.edit.DragCandidateBoundTest#indexCandidatesFindExactlyTheSamePutsAsAFullScan()
	 * @jls.testedby jls.edit.DragCandidateBoundTest#putLocations()
	 * @jls.testedby jls.elem.GroupOrientationTest#puts()
	 * @jls.testedby jls.elem.OrientationGeometryTest#describe()
	 */
	public int getX() {

		return requireElement().getX()+xr;
	} // end of getX method

	/**
	 * Get put's y-coordinate.
	 *
	 * @return The y-coordinate.
	 *
	 * @jls.testedby jls.edit.DragCandidateBoundTest#indexCandidatesFindExactlyTheSamePutsAsAFullScan()
	 * @jls.testedby jls.edit.DragCandidateBoundTest#putLocations()
	 * @jls.testedby jls.elem.GroupOrientationTest#puts()
	 * @jls.testedby jls.elem.OrientationGeometryTest#describe()
	 */
	public int getY() {

		return requireElement().getY()+yr;
	} // end of getY method

	/**
	 * Get number of bits in this put.
	 *
	 * @return the number of bits.
	 *
	 * @jls.testedby jls.ui.CircuitAssert#assertPutBits()
	 */
	public int getBits() {

		return bits;
	} // end of getBits method

	/**
	 * Get the element this put is part of.
	 *
	 * @return the element.
	 *
	 * @jls.testedby jls.UtilFunctionsTest#copyOfAPartialSelectionDropsDanglingWires()
	 * @jls.testedby jls.ui.CircuitAssert#reaches()
	 */
	public @Nullable LogicElement getElement() {

		return element;
	} // end of getElement method

	/**
	 * The x offset of this put's center relative to its element (issue
	 * #77: read by the GUI-side put renderer).
	 *
	 * @return the relative x offset.
	 */
	public int getXr() {

		return xr;
	} // end of getXr method

	/**
	 * The y offset of this put's center relative to its element (issue
	 * #77: read by the GUI-side put renderer).
	 *
	 * @return the relative y offset.
	 */
	public int getYr() {

		return yr;
	} // end of getYr method

	/**
	 * Declare which element edge this put sits on (issue #78, pin-face
	 * stage of the descriptor plan). The face is the outward normal of
	 * the edge: a put on the left edge of its element faces
	 * {@code LEFT}. Elements that know their put layout (today the
	 * gates, in {@link Gate#init}) declare faces at put-creation time;
	 * every other element rides the geometric derivation in
	 * {@link #getFace()} until its migration lands.
	 *
	 * @param face The outward normal of the element edge this put is on.
	 */
	void setFace(Orientation face) {

		this.face = face;
	} // end of setFace method

	/**
	 * The face of this put (issue #78): the outward normal of the edge
	 * of the owning element's bounding rectangle the put's center sits
	 * on - the direction a wire leaves the element from this put.
	 *
	 * <p>When a face was declared at put creation ({@link #setFace}),
	 * that declaration is returned. Otherwise the face is derived from
	 * the put's absolute center against the element's
	 * {@link Element#getRect()}, testing edges in the documented
	 * deterministic priority order left, right, top, bottom - so a
	 * corner put resolves LEFT/RIGHT before UP/DOWN.
	 *
	 * @return the face of this put, never null.
	 *
	 * @throws IllegalStateException if the put's center lies on no edge
	 *         of the owning element's bounding rectangle (a put layout
	 *         bug: wires would visually enter the element body).
	 *
	 * @jls.testedby jls.elem.PinFaceContractTest#everyPutHasAFaceOnItsElementEdge()
	 * @jls.testedby jls.elem.PinFaceContractTest#gateDeclaredFacesMatchDerivationAtAllOrientations()
	 * @jls.testedby jls.elem.PinFaceContractTest#rotationPermutesFacesByAQuarterTurn()
	 */
	public Orientation getFace() {

		if (face != null) {
			return face;
		}
		jls.core.Bounds r = requireElement().getRect();
		int cx = getX();
		int cy = getY();
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
		throw new IllegalStateException("put " + name + " of "
				+ requireElement().getClass().getSimpleName()
				+ " at (" + cx + "," + cy + ") is on no edge of " + r);
	} // end of getFace method

	/**
	 * Get wire end this put is attached to, or null if not attached.
	 *
	 * @return the wire end this put is attached to, or null if not attached.
	 *
	 * @jls.testedby jls.edit.TriStateBundleConnectTest#freshWireMayAttachToTriStateBundle()
	 * @jls.testedby jls.ui.CircuitAssert#reaches()
	 */
	public @Nullable WireEnd getWireEnd() {

		return wireEnd;
	} // end of getWireEnd method

	/**
	 * See if this put is attached to a WireEnd.
	 *
	 * @return true if it is attached, false if not.
	 *
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#pausePausesOnlyOnNonZeroInput()
	 * @jls.testedby jls.edit.TriStateBundleConnectTest#freeInput()
	 * @jls.testedby jls.edit.TriStateBundleConnectTest#freshWireMayAttachToTriStateBundle()
	 * @jls.testedby jls.ui.CircuitAssert#reaches()
	 */
	public boolean isAttached() {

		return wireEnd != null;
	} // end of isAttached method

	/**
	 * Record that this put is attached to a WireEnd.
	 * Wire end can be null, indicating that the put is to become unattached.
	 *
	 * @param end The wire end it is to attach to, or null if detaching.
	 */
	public void setAttached(@Nullable WireEnd end) {

		wireEnd = end;
	} // end of setAttached method

	/**
	 * Get the wire end this put is attached to, if any.
	 *
	 * @return the attached wire end, or null if this put is unattached.
	 *
	 * @jls.testedby jls.CircuitLoadErrorTest#duplicateWireEndAttachmentIsRejected()
	 */
	public @Nullable WireEnd getAttached() {

		return wireEnd;
	} // end of getAttached method

	/**
	 * Record that this put is touching or not touching a WireEnd.
	 *
	 * @param is True if touching, false if not.
	 */
	public void setTouching(boolean is) {

		touching = is;
	} // end of setTouching method

	/**
	 * See if this put is touching something.
	 *
	 * @return True if touching, false if not.
	 */
	public boolean isTouching() {

		return touching;
	} // end of isTouching method

	/**
	 * Set new position.
	 *
	 * @param x New x-coordinate.
	 * @param y New y-coordinate.
	 */
	public void setPosition(int x, int y) {

		this.xr = x;
		this.yr = y;
	} // end of setPosition method

	/**
	 * Save the position of this put.
	 */
	public void savePosition() {

		savex = xr;
		savey = yr;
	} // end of savePosition method

	/**
	 * Restore the position of this put.
	 */
	public void restorePosition() {

		xr = savex;
		yr = savey;
	} // end of restorePosition method

	/**
	 * See if this put is at the same place as another put.
	 *
	 * @param other The other put.
	 *
	 * @return true if at the same place, false if not.
	 */
	public boolean intersects(Put other) {

		int thisx = requireElement().getX() + xr;
		int thisy = requireElement().getY() + yr;
		int otherx = other.requireElement().getX() + other.xr;
		int othery = other.requireElement().getY() + other.yr;
		if (this != other && thisx == otherx && thisy == othery) {
			return true;
		}
		return false;
	} // end of intersects method

	/**
	 * Get the copy of this put.
	 *
	 * @return the copy.
	 */
	public Put getCopy() {

		if (myCopy == null)
			throw new IllegalStateException("put has not been copied");
		return myCopy;
	} // end of getCopy method

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------

	/** The current simulated value of this put, or null for high-impedance (tri-state). */
	protected @Nullable BitSet currentValue;

} // end of Put class
