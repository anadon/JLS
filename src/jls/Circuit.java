package jls;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import jls.edit.Editor;
import jls.edit.SimpleEditor;
import jls.elem.Element;
import jls.elem.JumpStart;
import jls.elem.LogicElement;
import jls.elem.Output;
import jls.elem.StateMachine;
import jls.elem.SubCircuit;
import jls.elem.TruthTable;
import jls.elem.Wire;
import jls.elem.WireEnd;
import jls.elem.WireNet;

/**
 * The main (container) class for each circuit.
 * 
 * @author David A. Poplawski
 */
public class Circuit implements Printable {

	// properties
	private String name = null; // will be the file name after appending .jls
	private String dir = ""; // the directory the file is in
	private Set<Element> elements = new HashSet<Element>();
	private SubCircuit subElement = null; // the element referring to this
											// circuit
	private Editor editor = null; // this circuit's editor (null if none)
	private Set<String> namesUsed = new HashSet<String>(); // so element names
															// can be unique
	private SortedMap<String, JumpStart> starts = new TreeMap<String, JumpStart>(); // jumpstarts
	private boolean changed = false;

	private final SpatialIndex index = new SpatialIndex(); // grid index over
															// element bounds
															// (#3, #17)
	private final Set<Element> highlighted = new HashSet<Element>(); // elements
																		// currently
																		// drawn
																		// highlighted

	private Set<Element> loadedElements = new HashSet<Element>(); // for loading
	private boolean deferredFinishReported = false; // draw-time finishLoad failure (#58)
																	// from file
	private Map<Integer, Element> elementMap = new HashMap<Integer, Element>(); // for
																				// loading
																				// from
																				// file
	private static int lineNumber; // to report errors when reading circuit file

	/**
	 * Create a new, empty circuit.
	 * 
	 * @param name
	 *            The name of this circuit.
	 */
	public Circuit(String name) {

		this.name = name;
	} // end of constructor

	/**
	 * Get the directory the circuit file is stored in.
	 * 
	 * @return the full path name of the directory.
	 */
	public String getDirectory() {

		return dir;
	} // end of getDirectory method

	/**
	 * Set the directory the circuit file is store in.
	 * 
	 * @param dir
	 *            The full path name of the directory.
	 */
	public void setDirectory(String dir) {

		this.dir = dir;
	} // end of setDirectory method

	/**
	 * Get the name of this circuit.
	 * 
	 * @return the name of this circuit.
	 */
	public String getName() {

		return name;
	} // end of getFileName method

	/**
	 * Change name of this circuit.
	 * 
	 * @param name
	 *            New name.
	 */
	public void setName(String name) {

		this.name = name;
	} // end of setName method

	/**
	 * Check if circuit has changed.
	 * 
	 * @return true if the circuit has changed, false if not.
	 */
	public boolean hasChanged() {

		return changed;
	} // end of hasChanged method

	/**
	 * Mark this circuit as changed. If it is a subcircuit, mark the circuit it
	 * is in as changed too. If a simulator is running, stop it.
	 */
	public void markChanged() {

		changed = true;
		index.invalidate();
		if (subElement != null) {
			subElement.getCircuit().markChanged();
		}
		if (JLSInfo.sim != null) {
			JLSInfo.sim.stop();
		}
	} // end of markChanged method

	/**
	 * Mark this circuit as saved. Called only after its serialized form has
	 * actually been written to disk, so a failed write keeps the
	 * unsaved-changes protection alive.
	 */
	public void clearChanged() {

		changed = false;
	} // end of clearChanged method

	/**
	 * Remove all elements from this circuit.
	 */
	public void clear() {

		elements.clear();
		namesUsed.clear();
		starts.clear();
		highlighted.clear();
		index.invalidate();
	} // end of clear method

	/**
	 * Add an element to this circuit.
	 * 
	 * @param el
	 *            The element to add.
	 */
	public void addElement(Element el) {

		elements.add(el);
		el.setCircuit(this);
		index.invalidate();
	} // end of addElement method

	/**
	 * Delete element from circuit. Do nothing if the element is not in the
	 * circuit.
	 * 
	 * @param el
	 *            The element to remove.
	 */
	public void remove(Element el) {

		elements.remove(el);
		highlighted.remove(el);
		index.invalidate();
	} // end of remove method

	/**
	 * Get the element set, as an unmodifiable view (#27): every caller
	 * iterates; mutation goes through addElement/remove so the circuit
	 * controls its own membership (and future indexes stay coherent).
	 */
	public Set<Element> getElements() {

		return Collections.unmodifiableSet(elements);
	} // end of getElements method

	/**
	 * Mark the spatial index stale after a geometry change the incremental
	 * paths don't cover (rotate, flip, size change, aborted move). The next
	 * spatial query rebuilds it in one pass.
	 */
	public void invalidateIndex() {

		index.invalidate();
	} // end of invalidateIndex method

	/**
	 * Keep the spatial index current for elements just moved by a drag,
	 * including wires whose bounds follow a moved wire end. No-op when a
	 * rebuild is already pending. O(moved), which is what makes drag events
	 * independent of circuit size (#17).
	 *
	 * @param moved The elements the editor just moved.
	 */
	public void reindexAfterMove(Set<Element> moved) {

		if (index.isDirty()) {
			return;
		}
		for (Element el : moved) {
			reindexMoved(el);

			// moving a logic element drags its attached wire ends along
			// (LogicElement.move), so their bounds changed too
			for (jls.elem.Put put : el.getAllPuts()) {
				WireEnd end = put.getWireEnd();
				if (end != null) {
					reindexMoved(end);
				}
			}
		}
	} // end of reindexAfterMove method

	/**
	 * Refresh one moved element in the index, along with the wires whose
	 * bounds follow it if it is a wire end.
	 */
	private void reindexMoved(Element el) {

		if (elements.contains(el)) {
			index.update(el);
		}
		if (el instanceof WireEnd) {
			for (Wire wire : ((WireEnd) el).getWires()) {
				if (elements.contains(wire)) {
					index.update(wire);
				}
			}
		}
	} // end of reindexMoved method

	/**
	 * All elements whose bounds may intersect or touch the given rectangle
	 * (a superset: callers apply their exact predicates). Replaces
	 * full-circuit scans in per-mouse-event paths (#3, #17).
	 *
	 * @param rect The query rectangle.
	 *
	 * @return the candidate elements.
	 */
	public Set<Element> elementsNear(Rectangle rect) {

		if (index.isDirty()) {
			index.rebuild(elements);
		}
		return index.query(rect);
	} // end of elementsNear method

	/**
	 * All elements whose bounds come within the snap spacing of a point —
	 * a superset of every element whose contains(x,y) can be true,
	 * including wires' half-spacing tolerance around their segment.
	 *
	 * @param x The x-coordinate of the point.
	 * @param y The y-coordinate of the point.
	 *
	 * @return the candidate elements.
	 */
	public Set<Element> elementsAt(int x, int y) {

		int pad = JLSInfo.spacing;
		return elementsNear(new Rectangle(x - pad, y - pad, 2 * pad, 2 * pad));
	} // end of elementsAt method

	/**
	 * Track highlight state so "unhighlight everything" is O(highlighted),
	 * not O(circuit). Called by Element.setHighlight.
	 *
	 * @param el The element whose highlight changed.
	 * @param light The new highlight state.
	 */
	public void noteHighlight(Element el, boolean light) {

		if (light) {
			highlighted.add(el);
		} else {
			highlighted.remove(el);
		}
	} // end of noteHighlight method

	/**
	 * A snapshot of the currently highlighted elements (safe to unhighlight
	 * while iterating).
	 *
	 * @return the highlighted elements at this moment.
	 */
	public Set<Element> getHighlighted() {

		return new HashSet<Element>(highlighted);
	} // end of getHighlighted method

	/**
	 * Load circuit from file.
	 * 
	 * @param scanner
	 *            A scanner to read with.
	 * 
	 * @return false if there were problems, true if load was successful.
	 */
	public boolean load(Scanner input) {

		// a fresh load must not report a previous load's failure (#58)
		JLSInfo.loadError = "";
		lineNumber = 1;
		boolean ok = load(input, 0);
		if (!ok) {
			// Scanner swallows IOException and presents it as end of
			// input; distinguish a truncated/corrupted stream from a
			// short-but-well-formed file (#58)
			IOException ioex = input.ioException();
			if (ioex != null) {
				JLSInfo.loadError = "I/O error while reading: "
						+ ioex.getMessage();
			}
		}
		return ok;
	} // end of load method

	/**
	 * Load circuit from file.
	 * 
	 * @param scanner
	 *            A scanner to read with.
	 * @param ln
	 *            Unused, except to give a different signature for the recursive
	 *            call that doesn't reset the line number counter.
	 * 
	 * @return false if there were problems, true if load was successful.
	 */
	private boolean load(Scanner input, int ln) {

		// catch all exceptions, assume there is a problem with the circuit file
		try {

			// read header
			if (!input.hasNext()) {
				JLSInfo.loadError = "no header info";
				return false;
			}
			String str = input.next();
			if (!str.equals("CIRCUIT")) {
				JLSInfo.loadError = "file does not start with CIRCUIT";
				return false;
			}
			if (!input.hasNext()) {
				JLSInfo.loadError = "no name for CIRCUIT";
				return false;
			}

			// ignore name if not a subcircuit
			if (name.equals(""))
				name = input.next();
			else
				input.next();

			// read circuit and get basic info for each element
			lineNumber += 1;
			while (input.hasNext()) {
				str = input.next();

				// if end of top level circuit
				if (str.equals("ENDCIRCUIT")) {
					lineNumber += 1;
					return true;
				}

				// should be the beginning of an(other) element
				if (!str.equals("ELEMENT")) {
					JLSInfo.loadError = "expecting ELEMENT";
					return false;
				}

				// the next input should exist (element type)
				if (!input.hasNext()) {
					JLSInfo.loadError = "expecting ELEMENT type";
					return false;
				}

				// get element type and create element; select the
				// (Circuit) constructor explicitly - getConstructors()
				// returns constructors in no specified order (issue #55)
				String elementType = input.next();
				Element newElement = null;
				try {
					Class<? extends Element> c =
							Class.forName("jls.elem." + elementType)
									.asSubclass(Element.class);
					newElement = c.getConstructor(Circuit.class)
							.newInstance(this);
				} catch (ClassNotFoundException ex) {
					JLSInfo.loadError = "unknown element type " + elementType;
					return false;
				} catch (ClassCastException ex) {
					JLSInfo.loadError = "non-Element subclass";
					return false;
				} catch (NoSuchMethodException ex) {
					JLSInfo.loadError = elementType
							+ " has no (Circuit) constructor";
					return false;
				} catch (InstantiationException ex) {
					JLSInfo.loadError = "cannot instantiate element type "
							+ elementType;
					return false;
				} catch (IllegalAccessException ex) {
					JLSInfo.loadError = "illegal access exception";
					return false;
				} catch (InvocationTargetException ex) {
					JLSInfo.loadError = "invocation target exception";
					return false;
				}
				lineNumber += 1;
				boolean loadOK = loadElement(newElement, input);
				if (loadOK) {
					loadedElements.add(newElement);
				} else {
					JLSInfo.loadError += "; false from loadElement";
					return false;
				}
			}
			JLSInfo.loadError = "no trailer";
			return false; // no trailer
		} catch (Exception ex) {
			JLSInfo.loadError = "load exception: " + ex.getMessage();
			for (int i = 0; i < ex.getStackTrace().length; i += 1) {
				JLSInfo.loadError += "\n" + ex.getStackTrace()[i].getFileName()
						+ ex.getStackTrace()[i].getLineNumber();
			}
			return false;
		} catch (Error er) {
			JLSInfo.loadError = "load error: " + er.getMessage();
			return false;
		}
	} // end of load method

	/**
	 * Load an element by reading all of its instance variable values.
	 * 
	 * @param instance
	 *            An empty object to load.
	 * 
	 * @return false if the file is not in the right format, true if it is.
	 */
	public boolean loadElement(Element el, Scanner input) {
		while (input.hasNext()) {
			if (input.hasNext("CIRCUIT")) {
				if (!(el instanceof SubCircuit)) {
					JLSInfo.loadError = "expected SubCircuit";
					return false;
				}
				Circuit subCirc = new Circuit("");
				if (!subCirc.load(input, 0)) {
					JLSInfo.loadError = "false from subCirc.load(input,0)";
					return false;
				}
				SubCircuit sub = (SubCircuit) el;
				subCirc.setImported(sub);
				sub.setSubCircuit(subCirc);
				sub.setName(subCirc.getName());
				addName(subCirc.getName());
				try {
					if (!subCirc.finishLoad(null)) {
						JLSInfo.loadError = "false from subCir.finishLoad(null)";
						return false;
					}
				} catch (Exception e) {
					// a broken subcircuit must fail the parent load, not
					// report success with a stack trace on stderr (#58)
					e.printStackTrace();
					JLSInfo.loadError = "subcircuit " + subCirc.getName()
							+ " failed to finish loading: " + e.getMessage();
					return false;
				}
				continue;
			}
			String type = input.next();
			if (type.equals("END")) {
				lineNumber += 1;
				return true;
			}
			if (type.equals("int")) {
				if (!input.hasNext()) {
					JLSInfo.loadError = "unexpected end";
					return false;
				}
				String name = input.next();
				if (!input.hasNextInt()) {
					JLSInfo.loadError = "expecting int value";
					return false;
				}
				int value = input.nextInt();
				if (name.equals("id")) {
					elementMap.put(value, el);
				}
				el.setValue(name, value);
				lineNumber += 1;
			} else if (type.equals("long")) {
				if (!input.hasNext()) {
					JLSInfo.loadError = "unexpected end";
					return false;
				}
				String name = input.next();
				if (!input.hasNextLong()) {
					JLSInfo.loadError = "expecting long value";
					return false;
				}
				long value = input.nextLong();
				el.setValue(name, value);
				lineNumber += 1;
			} else if (type.equals("Int")) { // BigInteger
				if (!input.hasNext()) {
					JLSInfo.loadError = "unexpected end";
					return false;
				}
				String name = input.next();
				if (!input.hasNextBigInteger()) {
					JLSInfo.loadError = "expecting BigInteger value";
					return false;
				}
				BigInteger value = input.nextBigInteger();
				el.setValue(name, value);
				lineNumber += 1;
			} else if (type.equals("String")) {
				if (!input.hasNext()) {
					JLSInfo.loadError = "unexpected end";
					return false;
				}
				String name = input.next();
				String pattern = ".*";
				String raw = input.findInLine(pattern);
				if (raw == null) {
					JLSInfo.loadError = "null findInLine";
					return false;
				}
				String value = unquoteAndUnescape(raw);
				if (value == null) {
					JLSInfo.loadError = "expecting quoted string value";
					return false;
				}
				el.setValue(name, value);
				lineNumber += 1;
			} else if (type.equals("ref")) {
				if (!input.hasNext()) {
					JLSInfo.loadError = "unexpected end";
					return false;
				}
				String name = input.next();
				if (!input.hasNextInt()) {
					JLSInfo.loadError = "expecting int value";
					return false;
				}
				int value = input.nextInt();
				el.setValue(name, value);
				lineNumber += 1;
			} else if (type.equals("pair")) {
				if (!input.hasNextInt()) {
					JLSInfo.loadError = "expecting int value";
					return false;
				}
				int v1 = input.nextInt();
				if (!input.hasNextInt()) {
					JLSInfo.loadError = "expecting int value";
					return false;
				}
				int v2 = input.nextInt();
				el.setPair(v1, v2);
				lineNumber += 1;
			} else if (type.equals("probe")) {
				if (!input.hasNextInt()) {
					JLSInfo.loadError = "expecting int value";
					return false;
				}
				int id = input.nextInt();
				String pattern = ".*";
				String raw = input.findInLine(pattern);
				if (raw == null) {
					JLSInfo.loadError = "null findInLine";
					return false;
				}
				String value = unquoteAndUnescape(raw);
				if (value == null) {
					JLSInfo.loadError = "expecting quoted string value";
					return false;
				}
				if (!(el instanceof WireEnd)) {
					JLSInfo.loadError = "expecting WireEnd";
					return false;
				}
				WireEnd end = (WireEnd) el;
				end.setProbe(id, value);
				lineNumber += 1;
			} else {
				JLSInfo.loadError = "expecting item type";
				return false;
			}
		}
		JLSInfo.loadError = "abnormal loadElement termination";
		return false;
	} // end of loadElement method

	/**
	 * Extract and decode a quoted string value from the rest of a saved
	 * line. The writer (Attribute.StringAttribute.save) escapes backslash,
	 * quote and newline as two-character sequences; a single left-to-right
	 * scan is its exact inverse. Sequential replace() passes were not (issue
	 * #53): they collapsed a literal backslash-n into a real newline and
	 * corrupted trailing backslashes.
	 *
	 * @param raw The rest of the line, including the surrounding quotes.
	 *
	 * @return the decoded string, or null if no quoted value is present.
	 */
	private static String unquoteAndUnescape(String raw) {

		int start = raw.indexOf('"');
		int end = raw.lastIndexOf('"');
		if (start < 0 || end <= start)
			return null;
		String escaped = raw.substring(start + 1, end);
		StringBuilder value = new StringBuilder(escaped.length());
		for (int i = 0; i < escaped.length(); i += 1) {
			char ch = escaped.charAt(i);
			if (ch == '\\' && i + 1 < escaped.length()) {
				char next = escaped.charAt(i + 1);
				if (next == 'n') {
					value.append('\n');
					i += 1;
				} else if (next == '\\' || next == '"') {
					value.append(next);
					i += 1;
				} else {
					// not a writer-produced escape; keep it verbatim
					value.append(ch);
				}
			} else {
				value.append(ch);
			}
		}
		return value.toString();
	} // end of unquoteAndUnescape method

	/**
	 * Finish load of circuit.
	 * 
	 * @param g
	 *            The Graphics object to use.
	 * 
	 * @return false if any exceptions occur
	 * @throws Exception
	 */
	public boolean finishLoad(Graphics g) throws Exception {

		// if any exceptions, assume load problem
		try {

			// finish up non-wire ends first
			for (Element el : loadedElements) {
				if (el instanceof WireEnd)
					continue;
				el.init(g);
				elements.add(el);
			}

			// finish up wire ends
			LinkedList<WireEnd> ends = new LinkedList<WireEnd>();
			for (Element el : loadedElements) {
				if (!(el instanceof WireEnd))
					continue;
				WireEnd end = (WireEnd) el;
				end.init(this);
				elements.add(end);
				ends.add(end);
			}

			// partition ends into wire nets
			while (!ends.isEmpty()) {

				// start visit list and new wire net
				LinkedList<WireEnd> visit = new LinkedList<WireEnd>();
				WireEnd end = ends.remove();
				visit.add(end);
				WireNet net = new WireNet();

				// visit ends in visit list until empty
				Set<WireEnd> visited = new HashSet<WireEnd>();
				while (!visit.isEmpty()) {

					// get wire end, add to wire net
					WireEnd vend = visit.remove();
					ends.remove(vend);
					visited.add(vend);
					net.add(vend);
					vend.setNet(net);
					if (vend.isLoadTriState()) {
						net.loadTriState();
					}
					if (vend.isAttached()) {
						net.setBits(vend.getPut().getBits());
						if (vend.getPut() instanceof Output) {
							net.setInput();
						}
					}

					// add wires to wire net and add opposite wire ends to visit
					// list
					for (Wire wire : vend.getWires()) {
						WireEnd otherEnd = wire.getOtherEnd(vend);
						if (!visited.contains(otherEnd)) {
							visit.add(otherEnd);
							net.add(wire);
							wire.setNet(net);
						}
					}
				}
			}

			loadedElements.clear();
			// the id map is only needed while wire ends resolve their
			// refs above; keeping it pinned every loaded element (#51)
			elementMap.clear();
		} catch (Exception ex) {
			JLSInfo.loadError = "finishLoad Exception " + ex.getMessage();
			return false;
		} catch (Error er) {
			JLSInfo.loadError = "finishLoad Error " + er.getMessage();
			return false;
		}
		return true;

	} // end of finishLoad method

	/**
	 * Get the smallest rectangle containing all the elements in the circuit.
	 * 
	 * @return the smallest rectangle.
	 */
	public Rectangle getBounds() {

		Rectangle rect = new Rectangle();
		boolean firstTime = true;
		for (Element el : elements) {
			if (el instanceof Wire)
				continue;
			if (firstTime) {
				rect = el.getRect();
				firstTime = false;
			} else {
				rect.add(el.getRect());
			}
		}
		return rect;
	} // end of getBounds method

	/**
	 * Save circuit in file.
	 * 
	 * @param output
	 *            The file to write to.
	 */
	public void save(PrintWriter output) {

		// write header
		if (isImported()) {
			output.println("CIRCUIT " + subElement.getName());
		} else {
			output.println("CIRCUIT " + name);
		}

		// give each element a unique id
		int id = 0;
		for (Element el : elements) {
			el.setID(id);
			id += 1;
		}

		// save elements
		for (Element el : elements) {
			el.save(output);
		}

		// write trailer
		output.println("ENDCIRCUIT");
	} // end of save method

	/**
	 * Draw the circuit by drawing every element. First the set of elements not
	 * in the second set are drawn, then the ones in the second set are drawn.
	 * Wires are drawn first in each set.
	 *
	 * @param g
	 *            The graphics object to draw with.
	 * @param second
	 *            The second set of elements to draw.
	 * @param ed
	 *            The editor window doing the drawing.
	 * @throws Exception
	 */
	public void draw(Graphics g, Set<Element> second, SimpleEditor ed)
			throws Exception {

		// finish up loading process if necessary
		if (loadedElements.size() > 0) {
			if (!finishLoad(g)) {
				// report once instead of silently re-failing on every
				// repaint (#58)
				if (!deferredFinishReported) {
					deferredFinishReported = true;
					System.err.println("deferred finishLoad of circuit "
							+ name + " failed: " + JLSInfo.loadError);
				}
			}

			// set circuit size to the largest of the default area or the needed
			// area
			Rectangle rect = new Rectangle(0, 0, JLSInfo.circuitsize,
					JLSInfo.circuitsize);
			rect.add(getBounds());
			ed.setCircuitSize(rect.getSize());
		}

		// partition into draw layers in one pass instead of four full
		// scans (#27 S3): wires under non-wires, the second (selected)
		// set on top of both. Elements far outside the clip cannot be
		// visible and are skipped, so a scrolled view pays for what it
		// shows, not for the whole circuit (#17).
		Rectangle clip = g.getClipBounds();
		java.util.List<Element> wires = new java.util.ArrayList<Element>();
		java.util.List<Element> parts = new java.util.ArrayList<Element>();
		java.util.List<Element> secondWires = new java.util.ArrayList<Element>();
		java.util.List<Element> secondParts = new java.util.ArrayList<Element>();
		for (Element el : elements) {
			if (clip != null && !mayBeVisible(el, clip)) {
				continue;
			}
			if (el instanceof Wire) {
				(second.contains(el) ? secondWires : wires).add(el);
			} else {
				(second.contains(el) ? secondParts : parts).add(el);
			}
		}
		for (Element el : wires) {
			el.draw(g);
		}
		for (Element el : parts) {
			el.draw(g);
		}
		for (Element el : secondWires) {
			el.draw(g);
		}
		for (Element el : secondParts) {
			el.draw(g);
		}
	} // end of draw method

	/**
	 * Whether an element could draw inside the clip. The margin generously
	 * covers labels drawn near (but outside) an element's bounds.
	 */
	private static boolean mayBeVisible(Element el, Rectangle clip) {

		Rectangle b = el.getIndexBounds();
		b.grow(8 * JLSInfo.spacing, 8 * JLSInfo.spacing);
		return b.intersects(clip);
	} // end of mayBeVisible method

	/**
	 * Print the circuit.
	 * 
	 * @param g
	 *            The graphics object to use
	 * @param format
	 *            Page format info.
	 * @param pagenum
	 *            Ignored.
	 * 
	 * @return Printable.PAGE_EXISTS.
	 */
	public int print(Graphics g, PageFormat format, int pagenum) {

		// use better graphics
		Graphics2D gg = (Graphics2D) g;

		// construct name
		Circuit c = this;
		String nm = name;
		while (c.isImported()) {
			c = c.getSubElement().getCircuit();
			nm += " in " + c.getName();
		}

		// set up
		FontMetrics fm = gg.getFontMetrics();
		int ascent = fm.getAscent();
		int descent = fm.getDescent();
		int fontHeight = ascent + descent;

		// get bounds of actual circuit
		Rectangle rect = getBounds();

		// translate to page area
		double width = format.getImageableWidth();
		double height = format.getImageableHeight();
		gg.translate(format.getImageableX(), format.getImageableY());

		// draw title
		gg.drawString(nm, 0, ascent);

		// translate and scale to fit circuit to remaining page area
		gg.translate(0, fontHeight * 2);
		height -= fontHeight * 2;
		double scale = 1.0;
		if (rect.width > width) {
			scale = width / rect.width;
		}
		if (rect.height + JLSInfo.pointDiameter > height) {
			scale = Math.min(scale, height
					/ (rect.height + JLSInfo.pointDiameter));
		}
		gg.scale(scale, scale);
		gg.translate(-rect.x + JLSInfo.pointDiameter / 2, -rect.y
				+ JLSInfo.pointDiameter / 2);

		// print
		try {
			draw(gg, new HashSet<Element>(), null);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Printable.PAGE_EXISTS;
	} // end of print method

	/**
	 * Add this circuit to the print book, add any of its state machines, truth
	 * tables and all subcircuits.
	 * 
	 * @param book
	 *            The book to add to.
	 * @param format
	 *            The page format to use.
	 */
	public void addToBook(Book book, PageFormat format) {

		// add this circuit
		book.append(this, format);

		// add state machines
		for (Element el : elements) {
			if (el instanceof StateMachine) {
				StateMachine sm = (StateMachine) el;
				book.append(sm, format);
				Printable p = sm.makeOutSum();
				if (p != null)
					book.append(p, format);
			}
		}

		// add truth tables
		for (Element el : elements) {
			if (el instanceof TruthTable) {
				TruthTable tt = (TruthTable) el;
				book.append(tt, format);
			}
		}

		// add subcircuits
		for (Element el : elements) {
			if (el instanceof SubCircuit) {
				((SubCircuit) el).getSubCircuit().addToBook(book, format);
			}
		}
	} // end of addToBook method

	/**
	 * Export an image of the circuit.
	 *
	 * @param file
	 *            The name of the file to write to.
	 * @throws Exception
	 */
	public void exportImage(String file) throws Exception {

		// get bounds of actual circuit
		Rectangle rect = getBounds();

		// add 10 pixels on all edges
		int border = 10;
		rect = new Rectangle(rect.x - border, rect.y - border, rect.width + 2
				* border, rect.height + 2 * border);

		// set up image
		BufferedImage image = new BufferedImage(rect.width, rect.height,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		AffineTransform translate = new AffineTransform();
		translate.translate(-rect.x, -rect.y);
		g.setTransform(translate);

		// draw the image
		g.setColor(Color.white);
		g.fill(rect);
		draw(g, new HashSet<Element>(), null);

		// write the image
		try {
			ImageIO.write(image, "JPEG", new File(file));
		} catch (Exception ex) {
			System.out.println("image write exception");
		}

		// clean up
		g.dispose();
		image.flush();
		image = null;

	} // end of print method

	/**
	 * Untouch inputs and outputs of all logic elements.
	 */
	public void untouchPuts() {

		for (Element el : elements) {
			el.untouchPuts();
		}
	} // end of untouchPuts method

	/**
	 * Get an element from the load map.
	 * 
	 * @param id
	 *            The id of the element.
	 * 
	 * @return the element with the given id, or null if not in the map.
	 */
	public Element getElementByID(int id) {

		return elementMap.get(id);
	} // end of getElementByID method

	/**
	 * Add a name to the list of names used. If already used in the list, don't
	 * add it.
	 * 
	 * @param name
	 *            The new name.
	 * 
	 * @return false if the name is already in the list, true if not.
	 */
	public boolean addName(String name) {

		if (namesUsed.contains(name))
			return false;
		namesUsed.add(name);
		return true;
	} // end of addName method

	/**
	 * See if this circuit already has an element with a given name.
	 * 
	 * @param name
	 *            The name to check for.
	 * 
	 * @return true if the name is already used, false if not.
	 */
	public boolean hasName(String name) {

		return namesUsed.contains(name);
	} // end of hasName method

	/**
	 * Remove a name from the list of names used. Do nothing if not there to
	 * start with.
	 * 
	 * @param name
	 *            The name to remove.
	 */
	public void removeName(String name) {

		namesUsed.remove(name);
	} // end of removeName method

	/**
	 * Set that this circuit is an imported circuit. This means it cannot be
	 * saved in a file and that pins cannot be added or removed.
	 * 
	 * @param sub
	 *            The SubCircuit element in the main circuit that refers to this
	 *            subcircuit.
	 */
	public void setImported(SubCircuit sub) {

		subElement = sub;
	} // end of setImported method

	/**
	 * See if this is an imported circuit.
	 * 
	 * @return true if it is imported, false otherwise.
	 */
	public boolean isImported() {

		return subElement != null;
	} // end of isImported method

	/**
	 * Get the SubCircuit element referring to this circuit.
	 * 
	 * @return the element.
	 */
	public SubCircuit getSubElement() {

		return subElement;
	} // end of getSubElement method

	/**
	 * Remove all probes from this circuit and all subcircuits.
	 */
	public void removeAllProbes() {

		for (Element el : elements) {
			if (el instanceof Wire) {
				Wire wire = (Wire) el;
				wire.removeProbe();
			} else if (el instanceof SubCircuit) {
				SubCircuit sub = (SubCircuit) el;
				sub.getSubCircuit().removeAllProbes();
			}
		}
	} // end of removeAllProbes

	/**
	 * Turn off watches in all elements in this circuit and all subcircuits.
	 */
	public void clearAllWatches() {

		for (Element el : elements) {
			if (el instanceof SubCircuit) {
				SubCircuit sub = (SubCircuit) el;
				sub.getSubCircuit().clearAllWatches();
			} else {
				if (!el.isUneditable())
					el.setWatched(false);
			}
		}
	} // end of clearAllWatches method

	/**
	 * Reset all propagation delays to their defaults.
	 */
	public void resetAllDelays() {

		for (Element el : elements) {
			if (!(el instanceof LogicElement))
				continue;
			if (el.isUneditable())
				continue;
			LogicElement logic = (LogicElement) el;
			logic.resetPropDelay();
		}
	} // end of resetAllDelays method

	/**
	 * Add a jumpstart to the list of jumpstarts in this circuit. If there is
	 * already one with the given name, do not add it.
	 * 
	 * @param name
	 *            The name of the jumpstart.
	 * @param start
	 *            The jumpstart object.
	 * 
	 * @return false if there already is a jumpstart with the given name, true
	 *         otherwise.
	 */
	public boolean addJumpStart(String name, JumpStart start) {

		if (starts.containsKey(name))
			return false;
		starts.put(name, start);
		return true;
	} // end of addJumpStart method

	/**
	 * Get the jumpstart with the given name.
	 * 
	 * @param name
	 *            The name of the desired jumpstart.
	 * 
	 * @return the jumpstart, or null if it no jumpstart with the given name
	 *         exists.
	 */
	public JumpStart getJumpStart(String name) {

		return starts.get(name);
	} // end of getJumpStart method

	/**
	 * Get all jump start names in alphabetical order.
	 * 
	 * @return the starts.
	 */
	public Set<String> getJumpStartNames() {

		// a copy: handing out the live keySet let callers (or their
		// iteration) corrupt the jump-start map (issue #51)
		return new TreeSet<String>(starts.keySet());
	} // end of getJumpStartNames method

	/**
	 * Remove a jump start from the list.
	 * 
	 * @param name
	 *            The name of this jump start.
	 */
	public void removeJumpStart(String name) {

		starts.remove(name);
	} // end of removeJumpStart

	/**
	 * Set the editor of this circuit.
	 * 
	 * @param ed
	 *            The current editor, or null to indicate not being edited.
	 */
	public void setEditor(Editor ed) {

		editor = ed;
	} // end of setEditor method

	/**
	 * Get the editor of this circuit.
	 * 
	 * @return The current editor, or null if not being edited.
	 */
	public Editor getEditor() {

		return editor;
	} // end of getEditor method

	/**
	 * Get the current line number of the loaded circuit file. Used when an
	 * error occurs so a meaningful error message can be printed.
	 */
	public int getLineNumber() {

		return lineNumber;
	} // end of getLineNumber method

	/**
	 * For debugging, return name and super.toString
	 * 
	 * @return string version of this circuit.
	 */
	public String toString() {

		return name + "(" + super.toString() + ")";
	} // end of toString method


} // end of Circuit class
