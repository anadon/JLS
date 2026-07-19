package jls.elem;

import jls.*;
import jls.edit.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

import java.util.*;
import java.math.*;

/**
 * Super class for all logic elements (including non-active ones).
 * Contains common display info and methods.
 * 
 * @author David A. Poplawski
 */
public abstract sealed class Element
		permits DisplayElement, LogicElement, Wire {

	// saved properties
	/** The file-local reference index, reassigned on every save. */
	private int id; 						// file-local reference index, reassigned on every save
	/** The permanent identity of this element (#165). */
	private ElementId stableId = ElementId.mintFresh(); // permanent identity (#165)
	/** Whether stableId was declared by the loaded file. */
	private boolean stableIdFromFile = false; // whether stableId was declared by the loaded file
	/** The x-coordinate of the upper left corner of this element. */
	protected int x; 						// upper left corner of element
	/** The y-coordinate of the upper left corner of this element. */
	protected int y;						//   (snap-to position for most elements)
	/** The width of this element in pixels. */
	protected int width = 0; 				// size of element
	/** The height of this element in pixels. */
	protected int height = 0;
	/** Whether editing of this element is disallowed. */
	private boolean uneditable = false;		// to keep others from editing this element
	/** The position of this element in the signal trace (-1 if none). */
	private int tracePosition = -1;			// position in signal trace (-1 if none)

	// running properties
	/** Whether this element should be drawn highlighted. */
	protected boolean highlight = false;	// whether the elements should be drawn highlighted
	/** The saved x-coordinate, restored after an aborted move. */
	private int savex;						// so it can be put back after an aborted move
	/** The saved y-coordinate, restored after an aborted move. */
	private int savey;
	/** The circuit this element is part of. */
	protected Circuit circuit;				// the circuit this element is part of

	/**
	 * Create a new Element object.
	 * 
	 * @param circuit The circuit this element is part of.
	 *
	 * @jls.testedby jls.ui.UiHarnessPilotTest.EveryAssertionCanFail#onGridFails()
	 */
	public Element(Circuit circuit) {

		this.circuit = circuit;
	} // end of constructor

	/**
	 * Set up this element (overridden by most elements).
	 *
	 * @param g The Graphics object to use to initialize sizes.
	 * @param editWindow The editor window this element will be displayed in.
	 * @param x The x-coordinate of the last known mouse position.
	 * @param y The y-coordinate of the last known mouse position.
	 *
	 * @return false if cancelled, true otherwise (this default implementation always returns false).
	 *
	 * @jls.testedby jls.ui.DialogConstructionSmokeTest#constructAndCancel()
	 */
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {

		return false;
	} // end of init method

	/**
	 * Set coordinates of this element.
	 * 
	 * @param x The x-coordinate of the upper left corner of this element.
	 * @param y The y-coordinate of the upper left corner of this element.
	 *
	 * @jls.testedby jls.ui.UiHarnessPilotTest.EveryAssertionCanFail#onGridFails()
	 */
	public void setXY(int x, int y) {

		this.x = x;
		this.y = y;
	} // end of setXY method

	/**
	 * Change the circuit this element is in.
	 * 
	 * @param circuit The new circuit.
	 */
	public void setCircuit(Circuit circuit) {

		this.circuit = circuit;
	} // end of setCircuit method

	/**
	 * Get the circuit this element is part of.
	 * 
	 * @return the circuit.
	 */
	public Circuit getCircuit() {

		return circuit;
	} // end of getCircuit method

	/**
	 * Get the element's id.
	 * 
	 * @return the id.
	 */
	public int getID() {

		return id;
	} // end of getID method

	/**
	 * Get x-coordinate of this element.
	 * 
	 * @return the x-coordinate.
	 *
	 * @jls.testedby jls.StableElementIdTest#sidsByLogicalElement()
	 * @jls.testedby jls.edit.TriStateBundleConnectTest#elementAt()
	 * @jls.testedby jls.elem.GroupOrientationTest#puts()
	 * @jls.testedby jls.elem.OrientationGeometryTest#describe()
	 * @jls.testedby jls.ui.CircuitAssert#describe()
	 * @jls.testedby jls.ui.EditorGestureTest#centerX()
	 * @jls.testedby jls.ui.EditorGestureTest#movingOneOfTwoElementsLeavesTheOtherPut()
	 * @jls.testedby jls.ui.EditorGestureTest#pressAndDragMovesAnElement()
	 * @jls.testedby jls.ui.EditorGestureTest#rubberBandSelectHighlightsEnclosedElements()
	 * @jls.testedby jls.ui.GeometryAssert#assertElementAt()
	 * @jls.testedby jls.ui.GeometryAssert#assertOnGrid()
	 */
	public int getX() {

		return x;
	} // end of getX method

	/**
	 * Get y-coordinate of this element.
	 * 
	 * @return the y-coordinate.
	 *
	 * @jls.testedby jls.StableElementIdTest#sidsByLogicalElement()
	 * @jls.testedby jls.edit.TriStateBundleConnectTest#elementAt()
	 * @jls.testedby jls.elem.GroupOrientationTest#puts()
	 * @jls.testedby jls.elem.OrientationGeometryTest#describe()
	 * @jls.testedby jls.ui.CircuitAssert#describe()
	 * @jls.testedby jls.ui.EditorGestureTest#centerY()
	 * @jls.testedby jls.ui.EditorGestureTest#movingOneOfTwoElementsLeavesTheOtherPut()
	 * @jls.testedby jls.ui.EditorGestureTest#pressAndDragMovesAnElement()
	 * @jls.testedby jls.ui.EditorGestureTest#rubberBandSelectHighlightsEnclosedElements()
	 * @jls.testedby jls.ui.GeometryAssert#assertElementAt()
	 * @jls.testedby jls.ui.GeometryAssert#assertOnGrid()
	 */
	public int getY() {

		return y;
	} // end of getY method
	
	/**
	 * Get the trace position of this element.
	 * 
	 * @return the trace position (-1 if not traced)
	 */
	public int getTracePosition() {
		
		return tracePosition;
	} // end of getTracePosition method
	
	/**
	 * Set the trace position of this element.
	 * 
	 * @param position The position.
	 */
	public void setTracePosition(int position) {
		
		tracePosition = position;
	} // end of setTracePosition method

	// The attributes every element saves, in their historical save order
	// (#23). One declaration drives save, copy, and load dispatch.
	/** The attributes every element saves, in historical save order (#23). */
	private static final java.util.List<Attribute> BASE_ATTRIBUTES =
			java.util.List.of(
		new Attribute.IntAttribute("id") {
			/** Reads the element's file-local id for the "id" attribute. */
			@Override
			protected int get(Element el) { return el.id; }
			/** Stores a loaded value into the element's file-local id. */
			@Override
			protected void set(Element el, int value) { el.id = value; }
			/** No-op: ids are assigned at save time, never copied. */
			@Override
			public void copy(Element from, Element to) {
				// ids are assigned at save time, never copied
			}
		},
		new Attribute.IntAttribute("x") {
			/** Reads the element's x-coordinate for the "x" attribute. */
			@Override
			protected int get(Element el) { return el.x; }
			/** Stores a loaded value into the element's x-coordinate. */
			@Override
			protected void set(Element el, int value) { el.x = value; }
		},
		new Attribute.IntAttribute("y") {
			/** Reads the element's y-coordinate for the "y" attribute. */
			@Override
			protected int get(Element el) { return el.y; }
			/** Stores a loaded value into the element's y-coordinate. */
			@Override
			protected void set(Element el, int value) { el.y = value; }
		},
		new Attribute.IntAttribute("width") {
			/** Reads the element's width for the "width" attribute. */
			@Override
			protected int get(Element el) { return el.width; }
			/** Stores a loaded value into the element's width. */
			@Override
			protected void set(Element el, int value) { el.width = value; }
			/** Whether the width is omitted from the save (recomputed on load). */
			@Override
			protected boolean omitted(Element el) {
				// recomputed by init() on load for some elements (#21)
				return el.sizeIsRecomputedOnLoad();
			}
		},
		new Attribute.IntAttribute("height") {
			/** Reads the element's height for the "height" attribute. */
			@Override
			protected int get(Element el) { return el.height; }
			/** Stores a loaded value into the element's height. */
			@Override
			protected void set(Element el, int value) { el.height = value; }
			/** Whether the height is omitted from the save (recomputed on load). */
			@Override
			protected boolean omitted(Element el) {
				return el.sizeIsRecomputedOnLoad();
			}
		},
		new Attribute.IntAttribute("fixed") {
			/** Reads the uneditable flag as 1 or 0 for the "fixed" attribute. */
			@Override
			protected int get(Element el) { return el.uneditable ? 1 : 0; }
			/** Marks the element uneditable when any "fixed" value is loaded. */
			@Override
			protected void set(Element el, int value) {
				// any saved value means uneditable, as the loader always did
				el.uneditable = true;
			}
			/** Whether "fixed" is omitted (only editable elements omit it). */
			@Override
			protected boolean omitted(Element el) { return !el.uneditable; }
			/** Copies the uneditable flag to the target element. */
			@Override
			public void copy(Element from, Element to) {
				to.uneditable = from.uneditable;
			}
		},
		new Attribute.IntAttribute("trpos") {
			/** Reads the trace position for the "trpos" attribute. */
			@Override
			protected int get(Element el) { return el.tracePosition; }
			/** Stores a loaded value into the element's trace position. */
			@Override
			protected void set(Element el, int value) { el.tracePosition = value; }
			/** Whether "trpos" is omitted (untraced elements omit it). */
			@Override
			protected boolean omitted(Element el) { return el.tracePosition == -1; }
		},
		new Attribute.StringAttribute("sid") {
			/** Reads the stable id as text for the "sid" attribute. */
			@Override
			protected String get(Element el) { return el.stableId.toString(); }
			/** Parses a loaded stable id and marks it as file-declared. */
			@Override
			protected void set(Element el, String value) {
				el.stableId = ElementId.parse(value);
				el.stableIdFromFile = true;
			}
			/** No-op: identity is never copied (a copy mints a fresh id). */
			@Override
			public void copy(Element from, Element to) {
				// identity is never copied: a pasted element is a new
				// element and keeps the fresh id minted at its
				// construction (#165 P3)
			}
		}
	);

	/**
	 * The attributes this element saves, in save order (#23). Subclasses
	 * that declare their own attributes return the base list plus their
	 * own; unconverted subclasses keep their handwritten save/copy/
	 * setValue methods and only inherit the base list.
	 *
	 * @return the attribute list.
	 */
	protected java.util.List<Attribute> savedAttributes() {

		return BASE_ATTRIBUTES;
	} // end of savedAttributes method

	/**
	 * Build a full attribute list for a subclass: the base attributes
	 * followed by the subclass's own, preserving save order (#23).
	 *
	 * @param own The subclass's own attributes.
	 *
	 * @return an immutable combined list.
	 */
	protected static java.util.List<Attribute> concatAttributes(
			java.util.List<Attribute> own) {

		java.util.List<Attribute> all =
				new java.util.ArrayList<Attribute>(BASE_ATTRIBUTES);
		all.addAll(own);
		return java.util.Collections.unmodifiableList(all);
	} // end of concatAttributes method

	/**
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The name of the variable.
	 * @param value The value of the variable.
	 */
	public void setValue(String name, int value) {

		for (Attribute attr : savedAttributes()) {
			if (attr.setInt(this, name, value)) {
				return;
			}
		}
	} // end of setValue method

	/**
	 * Set a long instance variable value (during a load).
	 * 
	 * @param name The name of the variable.
	 * @param value The value of the variable.
	 */
	public void setValue(String name, long value) {

		for (Attribute attr : savedAttributes()) {
			if (attr.setLong(this, name, value)) {
				return;
			}
		}
	} // end of setValue method

	/**
	 * Set a BigInteger instance variable value (during a load).
	 * 
	 * @param name The name of the variable.
	 * @param value The value of the variable.
	 */
	public void setValue(String name, BigInteger value) {

		for (Attribute attr : savedAttributes()) {
			if (attr.setBigInt(this, name, value)) {
				return;
			}
		}
	} // end of setValue method

	/**
	 * Set a String instance variable value (during a load).
	 * 
	 * @param name The name of the variable.
	 * @param value The value of the variable.
	 */
	public void setValue(String name, String value) {

		for (Attribute attr : savedAttributes()) {
			if (attr.setString(this, name, value)) {
				return;
			}
		}
	} // end of setValue method

	/**
	 * Set a pair of int instance variable values (during a load).
	 * 
	 * @param v1 The first value.
	 * @param v2 The second value.
	 */
	public void setPair(int v1, int v2) {

	} // end of setPair method

	/**
	 * Initialize internal information for this element.
	 *
	 * @param g The graphics object to use.
	 *
	 * @throws Exception always, unless overridden by a subclass.
	 */
	public void init(Graphics g) throws Exception{
		throw new Exception("ERROR: using undefined function from " + this.getName());
	}

	/**
	 * Placeholder for element copies.
	 *
	 * @return a copy of this element, or null from this placeholder implementation.
	 *
	 * @jls.testedby jls.AllElementsRoundTripTest#copyPreservesEverySavedAttribute()
	 * @jls.testedby jls.StableElementIdTest#copyMintsAFreshId()
	 * @jls.testedby jls.elem.AttributePersistenceTest#copyIsFieldEquivalent()
	 */
	public Element copy() {

		return null;
	} // end of copy method

	/**
	 * Make a copy of this element in the parameter object.
	 * 
	 * @param it The element to copy info to.
	 */
	public void copy(Element it) {

		for (Attribute attr : savedAttributes()) {
			attr.copy(this, it);
		}
	} // end of copy method

	/**
	 * Save current coordinates in case move doesn't work.
	 */
	public void savePosition() {

		savex = x;
		savey = y;
	} // end of savePosition method

	/**
	 * Restore saved coordinates when move doesn't work.
	 */
	public void restorePosition() {

		x = savex;
		y = savey;
	} // end of restorePosition method

	/**
	 * Move element.
	 * 
	 * @param dx Distance to move in the x-direction.
	 * @param dy Distance to move in the y-direction.
	 *
	 * @jls.testedby jls.DeterministicSaveTest#stateHashIsContentDetermined()
	 * @jls.testedby jls.DrawCullingParityTest#culledCandidatesMatchFullScan()
	 * @jls.testedby jls.ProofBridgeTest#a1IndexIntervalsAreNonEmpty()
	 * @jls.testedby jls.SpatialIndexTest#staysExactAfterMovesAndInvalidation()
	 * @jls.testedby jls.edit.CircuitSnapshotTest#changedCircuitSnapshotsDifferently()
	 */
	public void move(int dx, int dy) {

		x += dx;
		y += dy;
	} // end of move method

	/**
	 * Fix position of this element (overridden).
	 */
	public void fixPosition() {

	} // end of fixPosition method

	/**
	 * See if the given point is inside the element's display area.
	 * 
	 * @param x The x-coordinate of the given point.
	 * @param y The y-coordinate of the given point.
	 * 
	 * @return true if the point is in the display area, false if not.
	 *
	 * @jls.testedby jls.SpatialIndexTest#everyContainingElementIsACandidate()
	 * @jls.testedby jls.SpatialIndexTest#reportsIndexVsScanTiming()
	 * @jls.testedby jls.elem.HollowVsFilledCollisionTest#containerInteriorDoesCollide()
	 * @jls.testedby jls.elem.HollowVsFilledCollisionTest#hollowInteriorDoesNotCollide()
	 */
	public boolean contains(int x, int y) {

		Rectangle thisRect = getRect();
		return thisRect.contains(x,y);
	} // end of contains method

	/**
	 * See if this element intersects another.
	 *
	 * @param other The other element.
	 *
	 * @return true if this element intersects the other, false if not.
	 *
	 * @jls.testedby jls.edit.WireSweepSymmetryTest#clearWireCollidesInNeitherDirection()
	 * @jls.testedby jls.edit.WireSweepSymmetryTest#wireHangingOffAnElementDoesNotCollideWithIt()
	 * @jls.testedby jls.edit.WireSweepSymmetryTest#wireSweepingAcrossElementCollidesLikeTheReverseDrag()
	 * @jls.testedby jls.elem.HollowVsFilledCollisionTest#containerInteriorDoesCollide()
	 * @jls.testedby jls.elem.HollowVsFilledCollisionTest#cornerCollisionIsAttributedToTheWireEnd()
	 * @jls.testedby jls.elem.HollowVsFilledCollisionTest#diagonalWireIsCandidateButNotCollisionOffTheDiagonal()
	 * @jls.testedby jls.elem.HollowVsFilledCollisionTest#edgeCrossingCollidesWithThatWireOnly()
	 * @jls.testedby jls.elem.HollowVsFilledCollisionTest#hollowInteriorDoesNotCollide()
	 */
	public boolean intersects(Element other) {

		// special case for intersecting with a wire
		if (other instanceof Wire) {
			Wire wire = (Wire)other;
			WireEnd end1 = wire.getEnd();
			WireEnd end2 = wire.getOtherEnd(end1);

			// no problem if this element is one end of the wire
			if (this instanceof WireEnd && (this == end1 || this == end2)) {
				return false;
			}

			// otherwise check for wire intersection with this element's bounding rectangle
			return wire.intersects(getRect());
		}

		// simply see if the elements' bounding rectangles intersect
		Rectangle thisRect = getRect();
		Rectangle otherRect = other.getRect();
		return thisRect.intersects(otherRect);
	} // end of intersects method

	/**
	 * See if this element is completely inside a given rectangle.
	 * 
	 * @param rect The given rectangle.
	 * 
	 * @return true if the element is inside, false if not.
	 *
	 * @jls.testedby jls.SpatialIndexTest#everyInsideElementIsACandidate()
	 */
	public boolean isInside(Rectangle rect) {

		Rectangle me = getRect();
		return rect.contains(me);
	} // end of isInside method

	/**
	 * Set/reset highlight.
	 * 
	 * @param light True if item should be highlighted, false otherwise.
	 *
	 * @jls.testedby jls.edit.CtrlWGestureTest#hoverSelect()
	 */
	public void setHighlight(boolean light) {

		highlight = light;
		if (circuit != null) {
			circuit.noteHighlight(this, light);
		}
	} // end of setHightlight method

	/**
	 * Whether this element is currently drawn highlighted (selected).
	 *
	 * @return true if highlighted.
	 *
	 * @jls.testedby jls.ui.EditorGestureTest#rubberBandSelectHighlightsEnclosedElements()
	 */
	public boolean isHighlighted() {

		return highlight;
	} // end of isHighlighted method

	/**
	 * Set id of this element (for file save).
	 *
	 * @param id The id.
	 *
	 * @jls.testedby jls.StringEscapeRoundTripTest#roundTrip()
	 * @jls.testedby jls.elem.AttributePersistenceTest#copyIsFieldEquivalent()
	 * @jls.testedby jls.elem.AttributePersistenceTest#savedBytesMatchTheHandwrittenFormat()
	 * @jls.testedby jls.elem.DisplayLegacyOrientTest#loadAndSaveElement()
	 * @jls.testedby jls.elem.GroupOrientationTest#orientOf()
	 */
	public void setID(int id) {

		this.id = id;
	} // end of setID method

	/**
	 * Get this element's permanent identity (#165). Minted at creation,
	 * persisted through saves, preserved by load and undo restore; a
	 * copy gets a fresh one.
	 *
	 * @return the stable id.
	 *
	 * @jls.testedby jls.StableElementIdTest#copyMintsAFreshId()
	 * @jls.testedby jls.StableElementIdTest#idsAreUniqueWithinACircuit()
	 * @jls.testedby jls.StableElementIdTest#mintedIdsSkipIdsTheFileAlreadyUses()
	 * @jls.testedby jls.StableElementIdTest#sidsByLogicalElement()
	 */
	public ElementId getStableId() {

		return stableId;
	} // end of getStableId method

	/**
	 * Whether this element's stable id was declared by the file it was
	 * loaded from, as opposed to minted at construction. Drives legacy
	 * minting in Circuit.finishLoad: only elements without a
	 * file-declared id get a deterministic legacy id.
	 *
	 * @return true if the id came from the loaded file.
	 */
	public boolean hasFileStableId() {

		return stableIdFromFile;
	} // end of hasFileStableId method

	/**
	 * Replace this element's stable id with a deterministically minted
	 * legacy id (#165). Called only by Circuit.finishLoad for elements
	 * read from files that predate stable ids.
	 *
	 * @param id The minted id.
	 */
	public void assignLegacyStableId(ElementId id) {

		stableId = id;
	} // end of assignLegacyStableId method

	/**
	 * Save all information about this element in a file.
	 * 
	 * @param output The print writer to use.
	 *
	 * @jls.testedby jls.AllElementsRoundTripTest#saveElement()
	 * @jls.testedby jls.StringEscapeRoundTripTest#roundTrip()
	 * @jls.testedby jls.elem.AttributePersistenceTest#saveElement()
	 * @jls.testedby jls.elem.DisplayLegacyOrientTest#loadAndSaveElement()
	 */
	public void save(PrintWriter output) {

		for (Attribute attr : savedAttributes()) {
			attr.save(this, output);
		}
	} // end of save method

	/**
	 * Whether this element's init() recomputes width/height
	 * unconditionally on load. If so, saving them is redundant and they
	 * are omitted to shrink saved files (#21); the loader has always
	 * tolerated absent attributes, so older JLS versions still read the
	 * files.
	 *
	 * @return false unless a subclass overrides.
	 */
	protected boolean sizeIsRecomputedOnLoad() {

		return false;
	} // end of sizeIsRecomputedOnLoad method

	/**
	 * Highlight this element on the screen.
	 * Subclasses draw the element itself.
	 * 
	 * @param g The Graphics object to draw with.
	 *
	 * @jls.testedby jls.elem.MuxSymbolTest#render()
	 */
	public void draw(Graphics g) {

		// highlight if necessary
		if (highlight) {
			g.setColor(Color.pink);
			Graphics2D gg = (Graphics2D)g;
			gg.fill(getRect());
		}
	} // end of draw method

	/**
	 * This element will be removed, so do whatever is needed.
	 * 
	 * @param circ A reference back to the circuit the element is in.
	 */
	public void remove(Circuit circ) {

		circ.remove(this);
	} // end of remove method

	/**
	 * Get put near given x,y position (if one) - overridden.
	 *
	 * @param x The x-coordinate of the position.
	 * @param y The y-coordinate of the position.
	 *
	 * @return the put near the given position, or null if there is none
	 * 		(this default implementation always returns null).
	 *
	 * @jls.testedby jls.edit.DragCandidateBoundTest#indexCandidatesFindExactlyTheSamePutsAsAFullScan()
	 */
	public Put getPut(int x, int y) {

		return null;
	} // end of getPut method

	/**
	 * Display infomation about element (overridden).
	 * 
	 * @param info A JLabel to display with.
	 */
	public void showInfo(JLabel info) {

	} // end of showInfo method

	/**
	 * See if this element is touching (for possible connections).
	 * This is overridden in wire ends.
	 *
	 * @return true if touching, false if not (this default implementation always returns false).
	 */
	public boolean isTouching() {

		return false;
	} // end of isTouching method

	/**
	 * Untouch all inputs and outputs.
	 */
	public void untouchPuts() {}

	/**
	 * Get all inputs and outputs.
	 * Generally overridden.
	 * 
	 * @return a set of all inputs and outputs.
	 *
	 * @jls.testedby jls.edit.DragCandidateBoundTest#indexCandidatesFindExactlyTheSamePutsAsAFullScan()
	 * @jls.testedby jls.edit.DragCandidateBoundTest#putLocations()
	 * @jls.testedby jls.elem.GroupOrientationTest#puts()
	 * @jls.testedby jls.elem.OrientationGeometryTest#describe()
	 */
	public Set<Put> getAllPuts() { return new HashSet<Put>(); }

	/**
	 * Get the rectangle bounding this element.
	 *  
	 * @return the bounding rectangle.
	 *
	 * @jls.testedby jls.elem.GroupOrientationTest#horizontalGeometryIsUnchanged()
	 * @jls.testedby jls.elem.GroupOrientationTest#verticalBinderLoadsWithVerticalGeometry()
	 * @jls.testedby jls.elem.GroupOrientationTest#verticalSplitterLoadsWithVerticalGeometry()
	 * @jls.testedby jls.elem.HollowVsFilledCollisionTest#diagonalWireIsCandidateButNotCollisionOffTheDiagonal()
	 * @jls.testedby jls.elem.HollowVsFilledCollisionTest#indexShortlistsMatchOutlineGeometry()
	 * @jls.testedby jls.elem.OrientationGeometryTest#describe()
	 * @jls.testedby jls.ui.EditorGestureTest#centerX()
	 * @jls.testedby jls.ui.EditorGestureTest#centerY()
	 * @jls.testedby jls.ui.GeometryAssert#assertAbove()
	 * @jls.testedby jls.ui.GeometryAssert#assertDimensions()
	 * @jls.testedby jls.ui.GeometryAssert#assertLeftOf()
	 * @jls.testedby jls.ui.GeometryAssert#assertWithinGridUnits()
	 */
	public Rectangle getRect() {

		return new Rectangle(x,y,width,height);
	} // end of getRect method

	/**
	 * The bounds this element occupies in the spatial index (#3, #17).
	 * Must contain every point for which contains() can be true and every
	 * rectangle this element can intersect. The default is the bounding
	 * rectangle; wires override it since their extent comes from their
	 * ends, not from x/y/width/height.
	 *
	 * @return the index bounds.
	 *
	 * @jls.testedby jls.DrawCullingParityTest#mayBeVisible()
	 * @jls.testedby jls.ProofBridgeTest#a1IndexIntervalsAreNonEmpty()
	 * @jls.testedby jls.ProofBridgeTest#a5DrawMarginAndMayBeVisibleMatchModel()
	 * @jls.testedby jls.SpatialIndexTest#bruteForceNear()
	 */
	public Rectangle getIndexBounds() {

		return getRect();
	} // end of getIndexBounds method

	/**
	 * Set/reset touching flag(s) for this element.
	 * Overridden by wire ends and logic elements.
	 * 
	 * @param setting True to set, false to reset.
	 */
	public void setTouching(boolean setting) {}

	/**
	 * Get put by name.
	 * 
	 * @param name Name of the put.
	 * 
	 * @return The put.
	 *
	 * @jls.testedby jls.edit.TriStateBundleConnectTest#freeInput()
	 * @jls.testedby jls.edit.TriStateBundleConnectTest#nonGroupPutsNeverMix()
	 */
	public Put getPut(String name) {

		return null;
	} // end of getPut method

	/**
	 * Check whether the element can be changed after it is created and placed.
	 * Default is not.
	 * Overriden by elements that can.
	 * 
	 * @return true if element can be change, false otherwise.
	 */
	public boolean canChange() {

		return false;
	} // end of canChange method

	/**
	 * Change element characteristics.
	 * Overridden in elements that can change.
	 * 
	 * @param g A Graphics object to use for sizing.
	 * @param editWindow The editor window.
	 * @param x The current x-coordinate of the cursor.
	 * @param y The current y-coordinate of the cursor.
	 * 
	 * @return true if the element did change, false if not.
	 */
	public boolean change(Graphics g, JPanel editWindow, int x, int y) {

		return false;
	} // end of change method

	/**
	 * Check whether element has a quick change (shortcut) option.
	 * Overridden by elements than can.
	 * 
	 * @return true if is does, false if not.
	 */
	public boolean quickChange() {

		return false;
	} // end of quicChange method

	/**
	 * Set up menu item for quick changes.
	 * Overridden by elements that can do it.
	 * Should never be called.
	 * 
	 * @param sed The simple editor the menu item will be used in.
	 *
	 * @return a menu item (can be a menu with submenu items)
	 *
	 * @throws UnsupportedOperationException if called and not overridden.
	 */
	public JMenuItem setupQuickMenu(SimpleEditor sed) {

		throw new UnsupportedOperationException("setupQuickMenu");
	} // end of setupQuickMenu method

	/**
	 * Check whether the element has timing info, i.e., propagation delay or access time.
	 * Default is do not.
	 * Overridden by elements tht do.
	 * 
	 * @return true if the element has timing info, false if not.
	 */
	public boolean hasTiming() {

		return false;
	} // end of hasTiming method

	/**
	 * Show timing change dialog.
	 */
	public void changeTiming() {

		// display dialog
		new DelayChange(this instanceof Memory);

	} // end of changeTiming method

	/**
	 * The save-format version this element's current state requires
	 * (issue #79 evolution policy: a writer emits the highest version
	 * whose features the file uses). Almost everything is expressible
	 * in version 1; an element whose state older readers would
	 * silently mis-load overrides this (issue #124: vertical groups).
	 *
	 * @return the minimum format version that can carry this element.
	 */
	public int saveFormatVersion()
	{
		return 1;
	}

	/**
	 * Tells if an element is capable of rotatating, defaults to false.
	 * This method may be overridden if an element supports rotation
	 * @return True if an element supports rotation otherwise false
	 */
	public boolean canRotate()
	{
		return false;
	}

	/**
	 *  This method will rotate the element if it is supported.
	 *  This must be overridden by supporting classes.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	public void rotate(JLSInfo.Orientation direction, Graphics g)
	{
		throw new UnsupportedOperationException("Rotate");
	}

	/**
	 * Tells if an element is capable of flipping, defaults to false.
	 * This method may be overridden if an element supports flipping
	 * @return True if an element supports flipping otherwise false
	 */
	public boolean canFlip()
	{
		return false;
	}

	/**
	 * This method will flip an element, it must be overridden by classes that support it
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	public void flip(Graphics g)
	{
		throw new UnsupportedOperationException("Flip");
	}

	/**
	 * Display dialog letting user change the propagation delay or access time.
	 */
	@SuppressWarnings("serial")
	private class DelayChange extends ElementDialog {

		// properties
		/** Field to enter the delay or access time. */
		private JTextField delayField = new JTextField(10);
		/** Keypad for the delay field. */
		private KeyPad delayPad = new KeyPad(delayField,10,0,this);

		/**
		 * Set up create dialog window.
		 * 
		 * @param isMemory True if this is a memory element, false if not.
		 */
		private DelayChange(boolean isMemory) {

			// set up window title
			super("Change Timing",null);

			// set up window
			Container window = getContentPane();

			// set up input
			JPanel info = new JPanel(new BorderLayout());
			JLabel delay;
			if (isMemory) {
				delay = new JLabel("Memory access time: ",SwingConstants.RIGHT);
			}
			else {
				delay = new JLabel("Propagation delay: ",SwingConstants.RIGHT);
			}
			info.add(delay,BorderLayout.WEST);
			info.add(delayField,BorderLayout.CENTER);
			delayField.setText(getDelay()+"");
			info.add(delayPad,BorderLayout.EAST);
			window.add(info);

			confirmOnEnter(delayField);
			finishDialog();
		} // end of constructor

		/**
		 * Validate and apply the new delay.
		 */
		@Override
		protected void validateAndAccept() {

			int temp = 0;
			try {
				temp = Integer.parseInt(delayField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Value not numeric, try again");
				return;
			}
			if (temp <= 0) {
				reject("Propagation delay must be greater than 0");
				return;
			}
			setDelay(temp);
			dispose();
		} // end of validateAndAccept method

	} // end of DelayChange class

	/**
	 * Get the propagation delay or access time in this element.
	 * Overridden by elements with timing info.
	 * 
	 * @return the current delay.
	 */
	public int getDelay() {

		return 0;
	} // end of getDelay method

	/**
	 * Set the propagation delay or access time in this element.
	 * Overridden by elements with timing info.
	 * 
	 * @param temp The new delay amount.
	 *        Must be Integer, don't change to int!
	 */
	public void setDelay(int temp) {

		// do nothing
	} // end of setDelay method

	/**
	 * Check wether the element can be watched.
	 * Default is not.
	 * Overridden by elements that can be.
	 * 
	 * @return false;
	 */
	public boolean canWatch() {

		return false;
	} // end of canWatch method

	/**
	 * See if element is currently watched.
	 * Default is not.
	 * Overridden by elements that can be watched.
	 * 
	 * @return false.
	 *
	 * @jls.testedby jls.ui.CircuitAssert#assertWatched()
	 */
	public boolean isWatched() {

		return false;
	} // end of isWatched method

	/**
	 * Set whether this element is watched or not.
	 * Overridden by elements that can be watched.
	 * 
	 * @param state Unused.
	 */
	public void setWatched(boolean state) {

	} // end of setWatched method

	/**
	 * Display the current value of this element.
	 * Only works for watchable elements, and is overridden there.
	 * 
	 * @param where The point on the screen to display at.
	 */
	public void showCurrentValue(Point where) {

	} // end of showCurrent value method

	/**
	 * Get the name of this element.
	 * Overridden by elements that actually have names.
	 * 
	 * @return null;
	 */
	public String getName() {

		return null;
	} // end of getName method

	/**
	 * See if this element is uneditable.
	 * 
	 * @return true if it is uneditable, false if not.
	 */
	public boolean isUneditable() {

		return uneditable;
	} // end of isUneditable method

	/**
	 * Make this element uneditable.
	 * This cannot be undone without editting the saved file.
	 */
	public void makeUneditable() {

		uneditable = true;
	} // end of makeUneditable method

} // end of Element class
