package jls.edit;



import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import jls.Circuit;
import jls.FileAbstractor;
import jls.JLSInfo;
import jls.TellUser;
import jls.Util;
import jls.elem.Adder;
import jls.elem.AndGate;
import jls.elem.Binder;
import jls.elem.Clock;
import jls.elem.Constant;
import jls.elem.Decoder;
import jls.elem.DelayGate;
import jls.elem.Element;
import jls.elem.Extend;
import jls.elem.Group;
import jls.elem.Input;
import jls.elem.InputPin;
import jls.elem.JumpEnd;
import jls.elem.JumpStart;
import jls.elem.Memory;
import jls.elem.Mux;
import jls.elem.NandGate;
import jls.elem.NorGate;
import jls.elem.NotGate;
import jls.elem.OrGate;
import jls.elem.Output;
import jls.elem.OutputPin;
import jls.elem.Pause;
import jls.elem.Put;
import jls.elem.Register;
import jls.elem.ShiftRegister;
import jls.elem.SigGen;
import jls.elem.Splitter;
import jls.elem.StateMachine;
import jls.elem.Stop;
import jls.elem.SubCircuit;
import jls.elem.Text;
import jls.elem.TriProp;
import jls.elem.TriState;
import jls.elem.TruthTable;
import jls.elem.Wire;
import jls.elem.WireEnd;
import jls.elem.WireNet;
import jls.elem.XorGate;

/**
 * Main circuit editing class.
 * Sets up and manages GUI.
 * 
 * @author David A. Poplawski
 */
@SuppressWarnings("serial")
public abstract class SimpleEditor extends JPanel {

	// properties
	protected Circuit circuit;			// the circuit being edited
	protected EditWindow ew;			// the editor window
	// volatile: written from the sim thread (enableEditor around a run),
	// read by EDT mouse/key handlers (issue #49, finding H7)
	protected volatile boolean enabled = true;	// disabled when editting a subcircuit
	protected JTabbedPane tabbedParent;	// the tabbed pane it is in
	private JScrollPane pane;			// the scroll page it is in
	protected JPanel top;				// here so Editor class can display file menu
	protected JLabel editable =
			new JLabel(" ");				// to show if circuit editing is enabled
	private JLabel message = 
			new JLabel(" ");				// editing status message display
	private JLabel info =
			new JLabel(" ",SwingConstants.CENTER);	// element information display
	private Circuit clipboard;			// for cut and paste
	private JPopupMenu importMenu = 
			new JPopupMenu();				// to display importable circuits
	private SimpleEditor me;
	private Stack<CircuitSnapshot> undos = new Stack<CircuitSnapshot>();
	private Stack<CircuitSnapshot> redos = new Stack<CircuitSnapshot>();
	private int check = JLSInfo.checkPointFreq+1;				// number of changes, used for checkpointing
	private Map<String,JMenuItem> menuMap = new HashMap<String,JMenuItem>();
	private Map<String,Circuit> circMap = new HashMap<String,Circuit>();

	// Checkpoint writing happens off the event thread (#19): the latest
	// serialized circuit per checkpoint file waits here, and a single
	// writer thread drains it. If edits outrun the disk, intermediate
	// checkpoints are superseded before being written (coalescing).
	private static final ConcurrentHashMap<String,String> pendingCheckpoints =
			new ConcurrentHashMap<String,String>();
	private static final ExecutorService checkpointWriter =
			Executors.newSingleThreadExecutor(new ThreadFactory() {
				/**
				 * Create the daemon thread that drains queued checkpoint writes.
				 *
				 * @param r The task the writer thread runs.
				 * @return the new daemon thread.
				 */
				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "JLS-checkpoint-writer");
					t.setDaemon(true);
					return t;
				}
			});

	/**
	 * Queue a checkpoint for background writing. The newest text for a
	 * given file always wins; the write itself is atomic (temp file +
	 * rename via FileAbstractor), so a crash at any moment leaves the
	 * previous complete checkpoint in place.
	 *
	 * @param fileName Absolute path of the .jls~ checkpoint file.
	 * @param circuitText The serialized circuit.
	 * @see jls.edit.CheckpointWriterTest#cancelSupersedesQueuedCheckpointAndDeletesFile()
	 * @see jls.edit.CheckpointWriterTest#checkpointAfterCancelIsStillWritten()
	 * @see jls.edit.CheckpointWriterTest#checkpointIsWrittenAndLoadable()
	 * @see jls.edit.CheckpointWriterTest#newerCheckpointSupersedesQueuedOne()
	 */
	static void writeCheckpointInBackground(final String fileName, String circuitText) {

		if (pendingCheckpoints.put(fileName, circuitText) != null)
			return;	// a queued task will pick up this newer text
		checkpointWriter.execute(new Runnable() {
			/**
			 * Write the newest pending checkpoint text for the queued file.
			 */
			@Override
			public void run() {
				String content = pendingCheckpoints.remove(fileName);
				if (content == null)
					return;
				try {
					FileAbstractor.writeCircuit(new File(fileName), content);
				}
				catch (IOException ex) {
					// checkpoints are best-effort; the previous one survives
				}
			}
		});
	} // end of writeCheckpointInBackground method

	/**
	 * Supersede any pending checkpoint for a file and delete the checkpoint
	 * file itself. The delete runs on the writer thread, so it is ordered
	 * after any write already in flight — a checkpoint queued before a save
	 * can never resurrect the file afterwards. Waits briefly for the delete
	 * so quitting right after a save cannot leave a stale checkpoint behind.
	 *
	 * @param fileName Absolute path of the .jls~ checkpoint file.
	 * @see jls.edit.CheckpointWriterTest#cancelSupersedesQueuedCheckpointAndDeletesFile()
	 * @see jls.edit.CheckpointWriterTest#checkpointAfterCancelIsStillWritten()
	 */
	static void cancelCheckpoint(final String fileName) {

		pendingCheckpoints.remove(fileName);
		Future<?> deleted = checkpointWriter.submit(new Runnable() {
			/**
			 * Delete the checkpoint file, ordered after any in-flight write.
			 */
			@Override
			public void run() {
				new File(fileName).delete();
			}
		});
		try {
			deleted.get(5, TimeUnit.SECONDS);
		}
		catch (Exception ex) {
			// best-effort: the pending entry is already superseded
		}
	} // end of cancelCheckpoint method

	/**
	 * See if attaching a wire end to a put of a bundle (Group) would mix
	 * tri-state and normal connections, which bundles cannot have.
	 *
	 * A wire net's tri-state flag reflects only the puts the net is
	 * already attached to (see WireNet.recheck), so the incoming end is
	 * classified only when its net has at least one attached end. A
	 * freshly drawn wire's net has none and used to be miscounted as a
	 * normal connection, spuriously refusing legitimate tri-state bundle
	 * wiring (issue #118). The guard is on the whole net, not on
	 * end.isAttached(): the end handed to canConnect passed the
	 * isDangling gate, so it is itself never attached, but its net can
	 * already be tri-state or normal through its other, attached ends,
	 * and those connections must still be counted.
	 *
	 * @param end The (dangling) wire end being attached.
	 * @param put The put it would be attached to.
	 *
	 * @return true if put belongs to a bundle and the attach would mix
	 *         tri-state and normal wires, false otherwise.
	 * @see jls.edit.TriStateBundleConnectTest#freshWireMayAttachToNormalBundle()
	 * @see jls.edit.TriStateBundleConnectTest#freshWireMayAttachToTriStateBundle()
	 * @see jls.edit.TriStateBundleConnectTest#nonGroupPutsNeverMix()
	 * @see jls.edit.TriStateBundleConnectTest#normalDrivenWireIsStillRefusedOnTriStateBundle()
	 * @see jls.edit.TriStateBundleConnectTest#normalWireMayAttachToNormalBundle()
	 * @see jls.edit.TriStateBundleConnectTest#triStateWireIsStillRefusedOnNormalBundle()
	 * @see jls.edit.TriStateBundleConnectTest#triStateWireMayAttachToTriStateBundle()
	 */
	static boolean mixesTriStateAndNormal(WireEnd end, Put put) {

		if (!(put.getElement() instanceof Group))
			return false;

		// classify the bundle's already-attached wires
		boolean tri = false;
		boolean norm = false;
		for (Put p : put.getElement().getAllPuts()) {
			if (p.isAttached()) {
				if (p.getWireEnd().isTriState())
					tri = true;
				else
					norm = true;
			}
		}

		// classify the incoming wire, but only if its net is attached to
		// something - an unattached net's tri-state flag is meaningless
		boolean netAttached = false;
		for (WireEnd e : end.getNet().getAllEnds()) {
			if (e.isAttached()) {
				netAttached = true;
				break;
			}
		}
		if (netAttached) {
			if (end.isTriState())
				tri = true;
			else
				norm = true;
		}

		return tri && norm;
	} // end of mixesTriStateAndNormal method

	/**
	 * See if a wire hanging off the moving selection collides with
	 * anything along its span: a stationary wire end it lands on, or a
	 * stationary non-wire element its body sweeps across.
	 *
	 * The element check closes a drag-direction asymmetry: dragging an
	 * element into a wire has always been an overlap (the main overlap()
	 * loop calls sel.intersects(wire)), but dragging the wire so its
	 * body crossed a stationary element was never checked, so the same
	 * forbidden geometry could be created from one direction and not the
	 * other. The element predicate here is elm.intersects(wire) - the
	 * exact call the reverse direction makes - so the two directions
	 * cannot disagree. Consequences preserved from both directions:
	 * wire-over-wire crossings stay legal (schematic wires cross
	 * freely), and a wire never collides with an element an end of it
	 * sits on (Wire.intersects excludes wires whose endpoint is inside
	 * the rectangle), so attached wires do not collide with their own
	 * element. Elements in the selected set move rigidly with the wire
	 * and are skipped, as in the main overlap() loop.
	 *
	 * Queries the spatial index around the wire's own bounds (#3, #17);
	 * one query serves both the wire-end and the element checks.
	 *
	 * @param circuit The circuit being edited.
	 * @param selected The moving selection.
	 * @param sel The selected element the wire hangs off.
	 * @param wire The wire to check.
	 *
	 * @return true if the wire collides along its span.
	 * @see jls.edit.WireSweepSymmetryTest#clearWireCollidesInNeitherDirection()
	 * @see jls.edit.WireSweepSymmetryTest#elementsMovingWithTheSelectionAreSkipped()
	 * @see jls.edit.WireSweepSymmetryTest#landingOnAStationaryWireEndStillCollides()
	 * @see jls.edit.WireSweepSymmetryTest#wireCrossingWireStaysLegal()
	 * @see jls.edit.WireSweepSymmetryTest#wireHangingOffAnElementDoesNotCollideWithIt()
	 * @see jls.edit.WireSweepSymmetryTest#wireSweepingAcrossElementCollidesLikeTheReverseDrag()
	 */
	static boolean wireCollidesAlongSpan(Circuit circuit,
			Set<Element> selected, Element sel, Wire wire) {

		Rectangle span = wire.getIndexBounds();
		span.grow(JLSInfo.pointDiameter,JLSInfo.pointDiameter);
		for (Element elm : circuit.elementsNear(span)) {
			if (sel == elm)
				continue;
			if (elm instanceof WireEnd) {
				if (wire.touches((WireEnd)elm)) {
					return true;
				}
				continue;
			}
			if (elm instanceof Wire)
				continue;
			if (selected.contains(elm))
				continue;
			if (elm.intersects(wire)) {
				return true;
			}
		}
		return false;
	} // end of wireCollidesAlongSpan method

	/**
	 * Create new editor.
	 * 
	 * @param parent The tabbed pane this editor is in.
	 * @param circuit The circuit it will edit.
	 * @param name The name of the circuit.
	 * @param clipboard For cut and paste.
	 */
	public SimpleEditor(JTabbedPane parent, Circuit circuit, String name, Circuit clipboard) {

		// save parameters
		this.circuit = circuit;
		this.clipboard = clipboard;
		tabbedParent = parent;

		// give it a name
		setName(name);

		// set up GUI
		setLayout(new BorderLayout());
		JPanel all = new JPanel(new BorderLayout());

		top = new JPanel();
		top.setLayout(new BorderLayout());
		top.setBackground(Color.CYAN);
		top.add(editable,BorderLayout.WEST);
		top.add(message,BorderLayout.EAST);
		message.setOpaque(true);
		message.setBackground(Color.cyan);
		top.add(info,BorderLayout.CENTER);
		all.add(top,BorderLayout.NORTH);
		ew = new EditWindow();
		ew.setPreferredSize(new Dimension(JLSInfo.circuitsize,JLSInfo.circuitsize));
		pane = new JScrollPane(ew);
		pane.getHorizontalScrollBar().setUnitIncrement(10);
		pane.getVerticalScrollBar().setUnitIncrement(10);
		all.add(pane,BorderLayout.CENTER);
		JPanel elements = ew.getToolBar();
		add(elements,BorderLayout.NORTH);
		add(all);

		// save reference to myself
		me = this;

		// set up increase circuit size button in lower right corner of scroll pane
		JButton corner = new JButton();
		corner.setToolTipText("expand circuit drawing area by 10%");
		pane.setCorner(JScrollPane.LOWER_RIGHT_CORNER, corner);
		corner.addActionListener(new ActionListener() {
			/**
			 * Expand the circuit drawing area when the corner button is pressed.
			 *
			 * @param event The triggering action event.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				me.increaseSize();
			}
		});

	} // end of constructor

	/**
	 * Get reference to top so applet can put menu in.
	 */
	public JPanel getTop() {

		return top;
	} // end of getTop method

	/**
	 * Get the circuit being editted by this editor.
	 * 
	 * @return the circuit.
	 * @see jls.ui.EditorGestureSupport#currentCircuit()
	 */
	public Circuit getCircuit() {

		return circuit;
	} // end of getCircuit method

	/**
	 * Add a circuit to the import menu.
	 * 
	 * @param subCirc The circuit to add.
	 */
	public void addToImportMenu(Circuit subCirc) {

		Action act = new AbstractAction(subCirc.getName()) {
			/**
			 * Import the subcircuit named by this menu item.
			 *
			 * @param event The triggering action event.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				ew.doImport((String)(this.getValue(Action.NAME)));
			}
		};
		JMenuItem imp = new JMenuItem(act);
		importMenu.add(imp);
		menuMap.put(subCirc.getName(),imp);
		circMap.put(subCirc.getName(),subCirc);
	} // end of addToImportMenu method

	/**
	 * Remove a circuit from the import menu.
	 *
	 * @param subCirc The circuit to remove.
	 */
	public void removeFromImportMenu(Circuit subCirc) {

		String name = subCirc.getName();
		if (name == null)
			return;
		JMenuItem item = menuMap.get(name);
		if (item != null)
			importMenu.remove(item);
		menuMap.remove(name);
		circMap.remove(name);
	} // end of removeFromImportMenu method

	/**
	 * Point this editor's import map at a replacement circuit instance
	 * with the same name. Undo/redo installs a freshly loaded Circuit;
	 * without this refresh a sibling editor's Import silently copies the
	 * discarded instance's content (issue #39, audit finding M8).
	 *
	 * @param circ The replacement circuit.
	 */
	public void refreshInImportMenu(Circuit circ) {

		if (circMap.containsKey(circ.getName())) {
			circMap.put(circ.getName(), circ);
		}
	} // end of refreshInImportMenu method

	/**
	 * Rename a circuit in the import menu, updating the menu item
	 * label and the name maps.
	 *
	 * @param oldname The current name.
	 * @param newname The new name.
	 */
	public void changeInImportMenu(String oldname, String newname) {

		JMenuItem item = menuMap.get(oldname);
		item.setText(newname);
		menuMap.remove(oldname);
		menuMap.put(newname,item);

		Action act = new AbstractAction(newname) {
			/**
			 * Import the subcircuit named by this renamed menu item.
			 *
			 * @param event The triggering action event.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				ew.doImport((String)(this.getValue(Action.NAME)));
			}
		};
		item.setAction(act);

		Circuit newCircuit = circMap.get(oldname);
		circMap.remove(oldname);
		circMap.put(newname,newCircuit);
	} // end of changeInImportMenu method

	/**
	 * Set the size of the circuit drawing area.
	 * 
	 * @param size The size.
	 */
	public void setCircuitSize(Dimension size) {

		ew.setPreferredSize(size);
		ew.revalidate();
	} // end of setCircuitSize method

	/**
	 * Change editor window background color.
	 */
	public void changeBackgroundColor() {

		ew.setBackground(JLSInfo.backgroundColor);
	} // end of changeBackgroundColor method

	/**
	 * Increase circuit drawing area size by 10%.
	 */
	public void increaseSize() {

		Dimension size = ew.getSize();
		Dimension newSize = new Dimension((int)(size.width*1.1),(int)(size.height*1.1));
		setCircuitSize(newSize);
	} // end of increaseSize method

	/**
	 * Finish up import of a circuit.
	 * 
	 * @param impCirc The imported circuit.
	 */
	public void finishImport(Circuit impCirc) {

		// create subcircuit element
		SubCircuit sub = new SubCircuit(circuit);
		sub.setSubCircuit(impCirc);
		impCirc.setImported(sub);
		ew.setup(sub,true);

		// remove any signal generators from imported circuit
		Set<Element> sigGens = new HashSet<Element>();
		for (Element el : impCirc.getElements()) {
			if (el instanceof SigGen) {
				sigGens.add(el);
			}
		}
		for (Element el : sigGens) {
			impCirc.remove(el);
		}
	} // end of finishImport method

	/**
	 * Make the editor able to make changes.
	 * Usually turned off during simulation.
	 * 
	 * @param which True to enable editing, false to disable it.
	 */
	public void enableEditor(boolean which) {

		enabled = which;
	} // end of enableEditor method

	/**
	 * Reset editor after finishing a quick edit.
	 */
	public void quickReset() {

		ew.markChanged();
		ew.clearSelected();
		ew.setState(State.idle);
	} // end of quickReset method

	/**
	 * This should be overridden in the Editor subclass.
	 */
	public void close() {

		throw new UnsupportedOperationException();
	} // end of close method

	// can't be in EditWindow, but should be
	// (package-private so the gesture decision below is testable: #126)
	/**
	 * The editor's interaction mode, driving how mouse and keyboard input
	 * are interpreted: the constants enumerate the phases of editing an
	 * element or wire (idle, chosen, placing, moving, selecting, selected,
	 * option, startwire, drawire).
	 */
	enum State {idle, chosen, placing, moving, selecting, selected, option,
		startwire, drawire};

	/**
	 * What the ctrl-W shortcut does, as a pure function of editor state
	 * and selection size, so the dispatch is unit-testable headless
	 * (issue #126; the injected-facts pattern of ToolkitPolicy).
	 */
	enum CtrlW {START_WIRE, TOGGLE_WATCH, NONE};

	/**
	 * Decide the ctrl-W gesture. From idle the shortcut always starts a
	 * wire — before #126 it also required an empty selection, so the
	 * hover selection that idle mouse motion maintains made the shortcut
	 * toggle watch, or do nothing, until the cursor left every element.
	 * Watch toggling remains reachable from every non-idle state with a
	 * single selected element (e.g. after a select-rectangle).
	 *
	 * @param state The current editor state.
	 * @param selectionSize The number of currently selected elements.
	 * @return the gesture to perform.
	 * @see jls.edit.CtrlWGestureTest#idleStartsWireDespiteMultiSelection()
	 * @see jls.edit.CtrlWGestureTest#idleStartsWireDespiteSingleSelection()
	 * @see jls.edit.CtrlWGestureTest#idleStartsWireWithEmptySelection()
	 * @see jls.edit.CtrlWGestureTest#otherStatesDoNothing()
	 * @see jls.edit.CtrlWGestureTest#watchToggleStillReachableFromSelectedState()
	 */
	static CtrlW ctrlWGesture(State state, int selectionSize) {

		if (state == State.idle)
			return CtrlW.START_WIRE;
		if (selectionSize == 1)
			return CtrlW.TOGGLE_WATCH;
		return CtrlW.NONE;
	} // end of ctrlWGesture method

	/**
	 * Result of starting the wire-drawing gesture: the initial wire end,
	 * and whether a selection had to be cleared to start it. The flag is
	 * captured before the clear — keyed off the selection afterwards it
	 * would always be true, because the new wire end becomes the
	 * selection (the pitfall AmityWilder/JLS@b1f1573 fixed).
	 */
	static final class WireStart {
		final WireEnd end;
		final boolean hadSelection;
		/**
		 * Record the result of a wire-start gesture.
		 *
		 * @param end The new initial wire end.
		 * @param hadSelection Whether a selection existed before it was cleared.
		 */
		WireStart(WireEnd end, boolean hadSelection) {
			this.end = end;
			this.hadSelection = hadSelection;
		}
	} // end of WireStart class

	/**
	 * The model half of the idle-state wire-start gesture (#126): clear
	 * the (hover) selection, create the initial wire end at (x,y), add
	 * it to the circuit and to the selection, and give it a fresh wire
	 * net. Swing-free so it is unit-testable headless; the caller owns
	 * the GUI half (state transition, overlap feedback, repaint).
	 *
	 * @param circuit The circuit being edited.
	 * @param selected The current selection; cleared, then left holding
	 *        only the new wire end.
	 * @param x The x-coordinate for the wire end.
	 * @param y The y-coordinate for the wire end.
	 * @return the new wire end and the pre-clear selection state.
	 * @see jls.edit.CtrlWGestureTest#overlapFeedbackKeyedOffPreClearSelection()
	 * @see jls.edit.CtrlWGestureTest#startWireClearsSelectionAndSelectsNewEnd()
	 * @see jls.edit.CtrlWGestureTest#startWireFromEmptySelectionMatchesOldBehavior()
	 */
	static WireStart startWireGesture(Circuit circuit, Set<Element> selected,
			int x, int y) {

		boolean hadSelection = !selected.isEmpty();
		for (Element el : selected) {
			el.setHighlight(false);
		}
		selected.clear();
		WireEnd end = new WireEnd(circuit);
		end.setXY(x,y);
		end.init(circuit);
		circuit.addElement(end);
		selected.add(end);
		WireNet net = new WireNet();
		net.add(end);
		end.setNet(net);
		return new WireStart(end,hadSelection);
	} // end of startWireGesture method

		/**
		 * The window the circuit is displayed and edited in.
		 */
		private class EditWindow extends JPanel implements ActionListener,MouseListener,MouseMotionListener {

			// popup menus
			private JPopupMenu optionMenu = new JPopupMenu();
			private JMenuItem probe = new JMenuItem(""); // will get title when added to menu
			private JMenuItem watch = new JMenuItem(""); // will get title when added to menu
			private JMenuItem modify = new JMenuItem("Modify");
			private JMenuItem timing = new JMenuItem("Change Timing");
			private JMenuItem view = new JMenuItem("View Contents");
			private JMenuItem undo = new JMenuItem("Undo");
			private JMenuItem redo = new JMenuItem("Redo");
			private JMenuItem cut = new JMenuItem("Cut");
			private JMenuItem copy = new JMenuItem("Copy");
			private JMenuItem delete = new JMenuItem("Delete");
			private JMenuItem lock = new JMenuItem("Lock");

			private JPopupMenu newMenu = new JPopupMenu();
			private JMenuItem connect = new JMenuItem("Wire(s)");
			private JMenuItem paste = new JMenuItem("Paste");
			private JMenuItem selAll = new JMenuItem("Select All");
			private JMenuItem close = new JMenuItem("Close");

			private JMenuItem Crotate = new JMenuItem("Rotate Clockwise");
			private JMenuItem CCrotate = new JMenuItem("Rotate Counter-Clockwise");
			private JMenuItem flip = new JMenuItem("Flip");
			private JMenuItem matchJump = new JMenuItem("Create Matching End");

			private JPanel toolbar;
			private JMenu elements;

			// properties
			private State currentState = State.idle;
			private boolean firstDraw = true;	// first paint pushes the undo base copy
			private int x, y;					// latest actual cursor coordinates
			private Rectangle selRect = null;	// selection rectangle
			private Set<Element>selected =
					new HashSet<Element>();			// currently selected elements
			private WireEnd wireEnd;			// used for drawing wires
			private Wire wire;
			private WireNet net;
			private WireEnd prev = null;
			private Set<Element>adds =
					new HashSet<Element>();			// for adding elements during a connect
			private Set<Element>subs =
					new HashSet<Element>();			// for removing elements during a connect
			private String overlapMessage = "";
			private Set<Element>touchedElements =
					new HashSet<Element>();			// elements marked touching by overlap/connect
			private Set<Put>touchedPuts =
					new HashSet<Put>();				// puts marked touching by overlap/connect

			/**
			 * Create a new edit window.
			 */
			public EditWindow() {

				// set up GUI
				setBackground(JLSInfo.backgroundColor);

				// add listeners for mouse activities
				addMouseListener(this);
				addMouseMotionListener(this);

				// set up popup menus
				probe.setToolTipText("watch activity on this wire during simulation");
				probe.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
				watch.setToolTipText("watch activity on this element during simulation");
				watch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
				modify.setToolTipText("view/modify element details");
				modify.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
				timing.setToolTipText("change propagation delay or access time");
				timing.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
				view.setToolTipText("view current simulated value");
				view.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
				cut.setToolTipText("cut all selected elements to clipboard");
				cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
				copy.setToolTipText("copy all selected elements to clipboard");
				copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
				delete.setToolTipText("delete all selected elements");
				delete.setAccelerator(DeleteKeyPolicy.menuAccelerator(System.getProperty("os.name")));
				lock.setToolTipText("make selected elements uneditable (cannot be undone)");
				lock.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));

				// TODO: Make an action for this.
				//matchJump.setToolTipText("create the wire end to match this wire start");
				//matchJump.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));

				connect.setToolTipText("create a new wire");
				connect.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
				newMenu.add(connect);

				paste.setToolTipText("paste contents of clipboard");
				paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
				newMenu.add(paste);

				selAll.setToolTipText("select all elements");
				selAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
				newMenu.add(selAll);

				close.setToolTipText("close this circuit");
				close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
				newMenu.add(close);

				undo.setToolTipText("undo last modification");
				undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
				newMenu.add(undo);

				redo.setToolTipText("redo last undo");
				redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
				newMenu.add(redo);

				makeElements();
				newMenu.add(elements);

				// add listeners for menu items
				probe.addActionListener(this);
				watch.addActionListener(this);
				modify.addActionListener(this);
				timing.addActionListener(this);
				view.addActionListener(this);
				cut.addActionListener(this);
				copy.addActionListener(this);
				delete.addActionListener(this);
				lock.addActionListener(this);
				undo.addActionListener(this);
				redo.addActionListener(this);
				paste.addActionListener(this);
				selAll.addActionListener(this);
				close.addActionListener(this);
				connect.addActionListener(this);
				Crotate.addActionListener(this);
				CCrotate.addActionListener(this);
				flip.addActionListener(this);
				matchJump.addActionListener(this);

				// set up ctrl-w (new wire / watch) key binding
				Action ctrlw = new AbstractAction() {
					/**
					 * Handle ctrl-W: start a wire from idle, otherwise toggle the watch
					 * on a single selected element.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {

						// do nothing if editor is disabled
						if (!enabled)
							return;

						// decide what the shortcut does here (#126)
						CtrlW gesture = ctrlWGesture(currentState,selected.size());

						// if current state is idle, start a wire, superseding
						// any hover selection (#126)
						if (gesture == CtrlW.START_WIRE) {

							// start a wire
							Point p = getMousePosition();
							if (p == null)
								return; // not in drawing window
							setState(State.startwire);
							x = p.x;
							y = p.y;
							WireStart start =
									startWireGesture(circuit,selected,x,y);
							wireEnd = start.end;
							wire = null;
							net = wireEnd.getNet();

							// a cleared selection was under the cursor, so
							// report the (likely) overlap the same way wire
							// dragging does
							if (start.hadSelection) {
								if (overlap()) {
									info.setText(overlapMessage);
									info.setForeground(Color.red);
								}
								else {
									info.setText("");
									info.setForeground(Color.black);
								}
							}
							repaint();
						}
						else if (gesture == CtrlW.TOGGLE_WATCH) {

							// watch/unwatch
							Element el = (Element)selected.toArray()[0];

							// do nothing if locked
							if (el.isUneditable())
								return;

							// do nothing if not watchable
							if (!el.canWatch())
								return;

							// remove if watched, add if not
							if (el.isWatched()) {
								el.setWatched(false);
							}
							else {
								el.setWatched(true);
							}
							markChanged();

							// clean up
							clearSelected();
							setState(State.idle);
							repaint();
						}
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_W,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),"ctrlw");
				getActionMap().put("ctrlw", ctrlw);

				// set up end wire key binding
				Action endWire = new AbstractAction() {
					/**
					 * Handle Escape: abandon a wire being drawn and return to idle.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {

						// do nothing if editor is disabled
						if (!enabled)
							return;

						// if drawing a wire...
						if (currentState == State.startwire || currentState == State.drawire) {

							// remove wire end and wire from circuit and wire net
							circuit.remove(wireEnd);
							if (wire != null) {
								circuit.remove(wire);
								wire.getOtherEnd(wireEnd).remove(wire,circuit);
								markChanged();
							}
							net.remove(wireEnd);
							net.remove(wire);

							// remove any colinear wire ends
							removeCoLinear();

							// reset for next time
							wireEnd = null;
							wire = null;
							prev = null;
							clearSelected();
							untouchAll();
							setState(State.idle);
							repaint();
							return;
						}
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),"Escape");
				getActionMap().put("Escape", endWire);

				// set up view key binding
				Action see = new AbstractAction() {
					/**
					 * Handle the view shortcut: show the single selected element's
					 * current value.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {

						// do nothing if editor is disabled
						if (!enabled)
							return;

						// do nothing if not just one element selected
						if (selected.size() != 1)
							return;

						// get the single item in the selected set
						Element el = (Element)(selected.toArray()[0]);

						// ask element to display its current value
						el.showCurrentValue(new Point(x,y));

						// clean up
						clearSelected();
						setState(State.idle);
						repaint();

					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_S,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),"view");
				getActionMap().put("view", see);

				// set up modify key binding
				Action modify = new AbstractAction() {
					/**
					 * Handle the modify shortcut: modify the single selected element.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {

						// do nothing if editor is disabled
						if (!enabled)
							return;

						// do nothing if not just one element selected
						if (selected.size() != 1)
							return;

						doModify();
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_M,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),"modify");
				getActionMap().put("modify", modify);

				// set up probe key binding
				Action probe = new AbstractAction() {
					/**
					 * Handle the probe shortcut: toggle a probe on the single selected
					 * wire.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {

						// do nothing if editor is disabled
						if (!enabled)
							return;

						// do nothing if not just one element selected
						if (selected.size() != 1)
							return;

						// do nothing if the selected element is not a wire
						Element el = (Element)selected.toArray()[0];
						if (!(el instanceof Wire))
							return;

						doProbe();
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_P,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),"probe");
				getActionMap().put("probe", probe);

				// set up timing key binding
				Action timing = new AbstractAction() {
					/**
					 * Handle the timing shortcut: change timing on the single selected
					 * element that has it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {

						// do nothing if editor is disabled
						if (!enabled)
							return;

						// do nothing if not just one element selected
						if (selected.size() != 1)
							return;

						// do nothing if the selected element does not having timing
						Element el = (Element)selected.toArray()[0];
						if (!el.hasTiming())
							return;

						doTiming();
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_T,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),"timing");
				getActionMap().put("timing",timing);

				// set up selectAll key binding
				Action selectAll = new AbstractAction() {
					/**
					 * Handle the select-all shortcut.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {

						// do nothing if editor is disabled
						if (!enabled)
							return;

						doSelectAll();
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),"select all");
				getActionMap().put("select all", selectAll);

				// set up close window key binding
				Action closeWin = new AbstractAction() {
					/**
					 * Handle the close-window shortcut.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						close();
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_E,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),"close window");
				getActionMap().put("close window", closeWin);

				// set up delete key binding
				Action deleteKey = new AbstractAction() {
					/**
					 * Handle the delete shortcut: remove the selected elements.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						if (enabled) {
							remove();
							removeCoLinear();
							clearSelected();
							setState(State.idle);
							repaint();
						}
					}
				};
				for (KeyStroke stroke : DeleteKeyPolicy.canvasBindings())
					getInputMap().put(stroke,"do delete");
				getActionMap().put("do delete", deleteKey);

				// set up cut key binding
				Action cutKey = new AbstractAction() {
					/**
					 * Handle the cut shortcut: copy then remove the selected elements.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						if (enabled) {
							copy();
							remove();
							removeCoLinear();
							clearSelected();
							setState(State.idle);
							repaint();
						}
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),"do cut");
				getActionMap().put("do cut", cutKey);

				// set up copy key binding
				Action copyKey = new AbstractAction() {
					/**
					 * Handle the copy shortcut: copy the selected elements to the
					 * clipboard.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						if (enabled) {
							copy();
							clearSelected();
							setState(State.idle);
							repaint();
						}
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),"do copy");
				getActionMap().put("do copy", copyKey);

				// set up paste key binding
				Action pasteKey = new AbstractAction() {
					/**
					 * Handle the paste shortcut: paste the clipboard contents.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						if (enabled) {
							if (clipboard.getElements().size() == 0)
								return;
							if (paste(clipboard)) {
								setState(State.placing);
							}
							repaint();
						}
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),"do paste");
				getActionMap().put("do paste", pasteKey);

				// set up undo key binding
				Action undoKey = new AbstractAction() {
					/**
					 * Handle the undo shortcut.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						if (enabled) {
							undo();
							repaint();
						}
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),"do undo");
				getActionMap().put("do undo", undoKey);

				// set up redo key binding
				Action redoKey = new AbstractAction() {
					/**
					 * Handle the redo shortcut.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						if (enabled) {
							redo();
							repaint();
						}
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),"do redo");
				getActionMap().put("do redo", redoKey);

				// set up lock key binding
				Action lockKey = new AbstractAction() {
					/**
					 * Handle the lock shortcut: make the selected elements uneditable.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						if (enabled) {

							// warn user first
							boolean opt = TellUser.confirm(null,
									"Making elements uneditable cannot be undone.  Are you sure you want to do this?",
									"WARNING");

							// if user still ok with it ...
							if (opt) {

								// make selected elements uneditable
								for (Element el : selected) {
									el.makeUneditable();
								}
							}

							// finish up
							clearSelected();
							setState(State.idle);
							repaint();
							return;
						}
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_L,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),"do lock");
				getActionMap().put("do lock", lockKey);

			} // end of constructor

			/**
			 * Create element tool bar and menu.
			 */
			public void makeElements() {

				toolbar = new JPanel();
				toolbar.setLayout(new BoxLayout(toolbar,BoxLayout.X_AXIS));
				elements = new JMenu("elements");

				JPanel gates = new JPanel(new GridLayout(2,4));

				ImageIcon image = getImage("and");
				String text = image == null ? "AND" : "";
				Action act = new AbstractAction(text,image) {
					/**
					 * Create a new AND gate and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new AndGate(circuit),event.getSource() instanceof JButton);
					}
				};
				gates.add(makeElement(act,"AND gate"));

				image = getImage("or");
				text = image == null ? "OR" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new OR gate and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new OrGate(circuit),event.getSource() instanceof JButton);
					}
				};
				gates.add(makeElement(act,"OR gate"));

				image = getImage("not");
				text = image == null ? "NOT" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new NOT gate and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new NotGate(circuit),event.getSource() instanceof JButton);
					}
				};
				gates.add(makeElement(act,"NOT gate"));

				image = getImage("xor");
				text = image == null ? "XOR" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new exclusive-OR gate and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new XorGate(circuit),event.getSource() instanceof JButton);
					}
				};
				gates.add(makeElement(act,"exclusive OR gate"));

				image = getImage("nand");
				text = image == null ? "NAND" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new NAND gate and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new NandGate(circuit),event.getSource() instanceof JButton);
					}
				};
				gates.add(makeElement(act,"NAND gate"));

				image = getImage("nor");
				text = image == null ? "NOR" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new NOR gate and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new NorGate(circuit),event.getSource() instanceof JButton);
					}
				};
				gates.add(makeElement(act,"NOR gate"));

				image = getImage("delay");
				text = image == null ? "DELAY" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new delay gate and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new DelayGate(circuit),event.getSource() instanceof JButton);
					}
				};
				gates.add(makeElement(act,"user defined signal delay"));

				image = getImage("tristate");
				text = image == null ? "TriState" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new tri-state gate and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new TriState(circuit),event.getSource() instanceof JButton);
					}
				};
				gates.add(makeElement(act,"tri-state gate"));

				toolbar.add(gates);

				toolbar.add(Box.createRigidArea(new Dimension(5,0)));

				JPanel wireWorks = new JPanel(new GridLayout(2,4));

				image = getImage("jumpstart");
				text = image == null ? "START" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new named-wire (jump start) and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new JumpStart(circuit),event.getSource() instanceof JButton);
					}
				};
				wireWorks.add(makeElement(act,"name a wire"));

				image = getImage("jumpend");
				text = image == null ? "END" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new jump end and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new JumpEnd(circuit),event.getSource() instanceof JButton);
					}
				};
				wireWorks.add(makeElement(act,"connect to a named wire"));

				image = getImage("ipin");
				text = image == null ? "I-PIN" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new input pin and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new InputPin(circuit),event.getSource() instanceof JButton);
					}
				};
				wireWorks.add(makeElement(act,"input pin"));

				image = getImage("opin");
				text = image == null ? "O-PIN" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new output pin and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new OutputPin(circuit),event.getSource() instanceof JButton);
					}
				};
				wireWorks.add(makeElement(act,"output pin"));

				image = getImage("split");
				text = image == null ? "SPLIT" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new splitter and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new Splitter(circuit),event.getSource() instanceof JButton);
					}
				};
				wireWorks.add(makeElement(act,"unbundle wires"));

				image = getImage("bind");
				text = image == null ? "BIND" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new binder and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new Binder(circuit),event.getSource() instanceof JButton);
					}
				};
				wireWorks.add(makeElement(act,"bundle wires"));

				image = getImage("const");
				text = image == null ? "CONST" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new constant and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new Constant(circuit),event.getSource() instanceof JButton);
					}
				};
				wireWorks.add(makeElement(act,"constant value"));

				image = getImage("extend");
				text = image == null ? "1-to-N" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new extend element and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new Extend(circuit),event.getSource() instanceof JButton);
					}
				};
				wireWorks.add(makeElement(act,"make N copies of the input"));

				toolbar.add(wireWorks);

				toolbar.add(Box.createRigidArea(new Dimension(5,0)));

				JPanel mem = new JPanel(new GridLayout(2,1));

				image = getImage("register");
				text = image == null ? "REG" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new register and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new Register(circuit),event.getSource() instanceof JButton);
					}
				};
				mem.add(makeElement(act,"register (various triggering)"));

				image = getImage("memory");
				text = image == null ? "MEMORY" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new memory and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new Memory(circuit),event.getSource() instanceof JButton);
					}
				};
				mem.add(makeElement(act,"memory, various types"));

				toolbar.add(mem);

				toolbar.add(Box.createRigidArea(new Dimension(5,0)));

				JPanel comb = new JPanel(new GridLayout(2,3));

				image = getImage("mux");
				text = image == null ? "MUX" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new multiplexor and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new Mux(circuit),event.getSource() instanceof JButton);
					}
				};
				comb.add(makeElement(act,"multiplexor"));

				image = getImage("decoder");
				text = image == null ? "DEC" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new decoder and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new Decoder(circuit),event.getSource() instanceof JButton);
					}
				};
				comb.add(makeElement(act,"decoder"));

				image = getImage("shiftregister");
				text = image == null ? "SHIFT" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new shift register and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new ShiftRegister(circuit),event.getSource() instanceof JButton);
					}
				};
				comb.add(makeElement(act,"shift register (combinational shifter)"));

				image = getImage("adder");
				text = image == null ? "ADDER" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new adder and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new Adder(circuit),event.getSource() instanceof JButton);
					}
				};
				comb.add(makeElement(act,"adder"));

				image = getImage("clock");
				text = image == null ? "CLOCK" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new clock and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new Clock(circuit),event.getSource() instanceof JButton);
					}
				};
				comb.add(makeElement(act,"clock"));

				toolbar.add(comb);

				toolbar.add(Box.createRigidArea(new Dimension(5,0)));
				JPanel time = new JPanel(new GridLayout(2,1));

				image = getImage("pause");
				text = image == null ? "PAUSE" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new pause and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new Pause(circuit),event.getSource() instanceof JButton);
					}
				};
				time.add(makeElement(act,"pause simulator when asserted"));

				image = getImage("stop");
				text = image == null ? "STOP" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new stop and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new Stop(circuit),event.getSource() instanceof JButton);
					}
				};
				time.add(makeElement(act,"stop simulator when asserted"));

				toolbar.add(time);

				toolbar.add(Box.createRigidArea(new Dimension(5,0)));

				JPanel test = new JPanel(new GridLayout(2,1));

				image = getImage("siggen");
				text = image == null ? "SIGGEN" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new signal generator and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new SigGen(circuit),event.getSource() instanceof JButton);
					}
				};
				test.add(makeElement(act,"generate test signals"));

				image = getImage("display");
				text = image == null ? "DISPLAY" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new display and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new jls.elem.Display(circuit),
								event.getSource() instanceof JButton);
					}
				};
				test.add(makeElement(act,"display circuit value"));

				toolbar.add(test);

				toolbar.add(Box.createRigidArea(new Dimension(5,0)));

				JPanel complex = new JPanel(new GridLayout(2,1));

				image = getImage("statemachine");
				text = image == null ? "ST. MAC." : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new state machine and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new StateMachine(circuit),event.getSource() instanceof JButton);
					}
				};
				complex.add(makeElement(act,"state machine"));

				image = getImage("truth");
				text = image == null ? "Truth Table" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new truth table and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new TruthTable(circuit),event.getSource() instanceof JButton);
					}
				};
				complex.add(makeElement(act,"truth table"));

				toolbar.add(complex);

				toolbar.add(Box.createRigidArea(new Dimension(5,0)));

				image = getImage("text");
				text = image == null ? "TEXT" : "";
				act = new AbstractAction(text,image) {
					/**
					 * Create a new text element and begin placing it.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						setup(new Text(circuit),event.getSource() instanceof JButton);
					}
				};
				toolbar.add(makeElement(act,"text (for annotations)"));

				toolbar.add(Box.createRigidArea(new Dimension(5,0)));

				// import menu
				final JButton imp = new JButton("Import",getImage("down"));
				imp.setToolTipText("import an open subcircuit");
				imp.setBackground(Color.WHITE);
				imp.setHorizontalTextPosition(AbstractButton.LEADING);
				toolbar.add(imp);

				imp.addActionListener(new ActionListener() {
					/**
					 * Show the import menu below the Import button when it has entries.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						if (importMenu.getComponentCount() > 0) {
							Dimension d = imp.getSize();
							importMenu.show(imp,0,d.height);
							importMenu.setVisible(true);
						}
					}
				});

			} // end of makeToolBar method

			/**
			 * Get image.
			 * 
			 * @param name Base name of image.
			 */
			public ImageIcon getImage(String name) {
				URL image = getClass().getResource("images/" + name + ".gif");
				// a missing icon falls back to the button's text label
				// instead of an NPE at startup (issue #78 observation)
				return image == null ? null : new ImageIcon(image);
			} // end of geImage method

			/**
			 * Make a single element for the toolbar and menu.
			 * 
			 * @param action The action for this element.
			 * @param tip The tool tip for this element.
			 */
			public JButton makeElement(Action action, String tip) {

				JButton button = new JButton(action);
				button.setMinimumSize(new Dimension(32,232));
				button.setPreferredSize(new Dimension(32,32));
				JMenuItem item = new JMenuItem(action);
				button.setToolTipText(tip);
				button.setBackground(Color.WHITE);
				item.setToolTipText(tip);
				elements.add(item);
				return button;
			} // end of makeElement method

			/**
			 * Get the tool bar.
			 * 
			 * @return the tool bar.
			 */
			public JPanel getToolBar() {

				return toolbar;
			} // end of getToolBar method

			/**
			 * Draw the window.
			 * 
			 * @param g The Graphics object to draw with.
			 */
			@Override
			public void paintComponent(Graphics g) {

				super.paintComponent(g);
				Graphics2D gg = (Graphics2D)g;

				// draw background
				gg.setColor(JLSInfo.gridColor);
				Dimension size = getSize();
				for (int r=0; r<size.height; r+=JLSInfo.spacing) {
					gg.drawLine(0,r,size.width,r);
				}
				for (int c=0; c<size.width; c+=JLSInfo.spacing) {
					gg.drawLine(c,0,c,size.height);
				}

				// draw selection rectangle if elements selected
				if (currentState == State.selected) {
					gg.setColor(JLSInfo.selectionColor);
					gg.fill(selRect);
				}

				// draw all elements, selected ones last
				try {
					circuit.draw(g,selected,me);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// draw selecting rectangle if necessary
				if (currentState == State.selecting) {
					gg.setColor(Color.lightGray);
					if (selRect != null)
						gg.draw(selRect);
				}

				// save circuit for undo on first draw (finishLoad must
				// have been done by now); the paint-pass Graphics is NOT
				// cached - Swing may dispose it after this call, so later
				// element setup asks the component for a fresh one (#51)
				if (firstDraw) {
					this.pushCopy();
					firstDraw = false;
				}

			} // end of paintComponent method

			/**
			 * React to menu item selections.
			 * 
			 * @param event The event object for actions.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {

				info.setForeground(Color.BLACK);
				info.setText("");

				// if probe option selected: attach or remove a probe.
				// This branch used to swallow every handler below it - a
				// merge-conflict brace error left them nested inside and
				// unreachable, and dropped the doProbe() call (issue #37)
				if (event.getSource() == probe) {

					// do nothing if editor is disabled
					if (!enabled)
						return;

					doProbe();
					return;
				}

				// if watch option selected, ...
				if (event.getSource() == watch) {

					// do nothing if editor is disabled
					if (!enabled)
						return;

					// get the single item in the selected set
					Element el = (Element)(selected.toArray()[0]);

					// if its locked, don't change it
					if (el.isUneditable())
						return;

					// remove if watched, add if not
					if (el.isWatched()) {
						el.setWatched(false);
					}
					else {
						el.setWatched(true);
					}
					markChanged();

					// clean up
					clearSelected();
					setState(State.idle);
					repaint();
					return;
				}

				// if view option selected, ...
				if (event.getSource() == modify) {

					// do nothing if editor is disabled
					if (!enabled)
						return;
					doModify();
				}

				// if change timing, then it must have timing info
				if (event.getSource() == timing) {

					// do nothing if editor is disabled
					if (!enabled)
						return;

					doTiming();
				}

				// if view, then it must be a watchable element
				if (event.getSource() == view) {

					// do nothing if editor is disabled
					if (!enabled)
						return;

					// get the single item in the selected set
					Element el = (Element)(selected.toArray()[0]);

					// ask element to display its current value
					el.showCurrentValue(new Point(x,y));

					// clean up
					clearSelected();
					setState(State.idle);
					repaint();
					return;
				}

				// if cut option selected, copy selected elements to clipboard,
				// then delete those elements
				if (event.getSource() == cut) {

					// do nothing if editor is disabled
					if (!enabled)
						return;

					// copy, then remove
					copy();
					remove();

					// clean up
					removeCoLinear();
					clearSelected();
					setState(State.idle);
					repaint();
					return;
				}

				// if copy option, copy selected elements to clipboard
				if (event.getSource() == copy) {

					// do nothing if editor is disabled
					if (!enabled)
						return;

					copy();
					clearSelected();
					setState(State.idle);
					repaint();
					return;
				}

				// if delete option, delete selected elements
				if (event.getSource() == delete) {

					// do nothing if editor is disabled
					if (!enabled)
						return;

					// remove
					remove();
					removeCoLinear();
					clearSelected();
					setState(State.idle);
					repaint();
					return;
				}

				// if lock, set all selected elements to uneditable
				if (event.getSource() == lock) {

					// do nothing if editor is disabled
					if (!enabled)
						return;

					// warn user first
					boolean opt = TellUser.confirm(null,
							"Making elements uneditable cannot be undone.  Are you sure you want to do this?",
							"WARNING");

					// if user still ok with it ...
					if (opt) {

						// make selected elements uneditable
						for (Element el : selected) {
							el.makeUneditable();
						}
					}

					// finish up
					clearSelected();
					setState(State.idle);
					repaint();
					return;
				}

				// paste contents of clipboard
				if (event.getSource() == paste) {

					// do nothing if editor is disabled
					if (!enabled)
						return;

					// paste
					if (clipboard.getElements().size() == 0)
						return;
					if (paste(clipboard)) {
						setState(State.placing);
					}
					repaint();
					return;
				}

				// if select all option, select everything
				if (event.getSource() == selAll) {

					// do nothing if editor is disabled
					if (!enabled)
						return;

					doSelectAll();
				}

				// if close, close this circuit (or subcircuit)
				if (event.getSource() == close) {
					close();
				}

				// if undo, restore prevous copy of circuit
				if (event.getSource() == undo) {

					// do nothing if editor is disabled
					if (!enabled)
						return;

					undo();
					repaint();
				}

				// if redo, restore future copy of circuit
				if (event.getSource() == redo) {

					// do nothing if editor is disabled
					if (!enabled)
						return;

					redo();
				}

				if(event.getSource() == Crotate)
				{
					if(!enabled)
						return;
					Element el = (Element)(selected.toArray()[0]);
					el.rotate(JLSInfo.Orientation.RIGHT, this.getGraphics());
					markChanged();
					clearSelected();
					setState(State.idle);
					repaint();


				}

				if(event.getSource() == CCrotate)
				{
					if(!enabled)
						return;
					Element el = (Element)(selected.toArray()[0]);
					el.rotate(JLSInfo.Orientation.LEFT, this.getGraphics());
					markChanged();
					clearSelected();
					setState(State.idle);
					repaint();
				}

				if(event.getSource() == matchJump) {
					if(!enabled)
						return;

					JumpStart el = (JumpStart)(selected.toArray()[0]);
					JumpEnd nel = new JumpEnd(circuit);
					nel.setName(el.getName());
					// place at the last tracked mouse position (event-local; #103)
					nel.setup(this.getGraphics(), this, x, y);

					clearSelected();
					circuit.addElement(nel);
					nel.setHighlight(true);
					selected.add(nel);

					setState(State.chosen);
					markChanged();
					repaint();
				}

				if(event.getSource() == flip)
				{
					if(!enabled)
						return;
					Element el = (Element)(selected.toArray()[0]);
					el.flip(this.getGraphics());
					markChanged();
					clearSelected();
					setState(State.idle);
					repaint();
				}
				// if connection, start drawing wires
				else if (event.getSource() == connect) {

					// do nothing if editor is disabled
					if (!enabled)
						return;

					setState(State.startwire);
					wireEnd = new WireEnd(circuit);
					wireEnd.setXY(x,y);
					wireEnd.init(circuit);
					circuit.addElement(wireEnd);
					selected.add(wireEnd);
					wire = null;
					net = new WireNet();
					net.add(wireEnd);
					wireEnd.setNet(net);
					repaint();
				}

			} // end of actionPerformed method

			/**
			 * React to mousePressed events
			 * 
			 * @param event The event object for presses.
			 */
			@Override
			public void mousePressed(MouseEvent event) {

				// do nothing if editor is disabled
				if (!enabled)
					return;

				info.setForeground(Color.BLACK);
				info.setText("");

				// get event information
				x = event.getX();
				y = event.getY();
				boolean leftButton = event.getButton() == MouseEvent.BUTTON1;
				boolean rightButton = (event.getButton() == MouseEvent.BUTTON3) || (event.isPopupTrigger());

				// if cursor is just wandering around...
				if (currentState == State.idle) {

					// if left button...
					if (leftButton) {

						// see if cursor is on an element, asking the spatial
						// index for the few elements near the cursor instead
						// of scanning the whole circuit (#3, #17)
						for (Element el : circuit.elementsAt(x,y)) {
							if (el.contains(x,y)) {

								// do nothing if editor is disabled
								if (!enabled)
									return;

								// drag wire means drag both ends
								if (el instanceof Wire) {
									Wire dragWire = (Wire)el;
									WireEnd end1 = dragWire.getEnd();
									if (end1.isAttached())
										continue;
									WireEnd end2 = dragWire.getOtherEnd(end1);
									if (end2.isAttached())
										continue;
									selected.add(end1);
									selected.add(end2);
									end1.setHighlight(true);
									end2.setHighlight(true);
									end1.savePosition();
									end2.savePosition();
									setState(State.moving);
									return;
								}

								// can't select an attached wire end
								if (el instanceof WireEnd && ((WireEnd)el).isAttached())
									continue;

								// select it and begin moving
								selected.add(el);
								el.setHighlight(true);
								el.savePosition();
								setState(State.moving);
								return;
							}
						}

						// cursor is not on an element, so start selecting;
						// repaint so cleared hover highlights disappear (#35)
						selRect = new Rectangle(x,y,0,0);
						clearSelected();
						setState(State.selecting);
						repaint();
					}

					// if right button show menu
					else if (rightButton) {

						// see if cursor is on an element, via the spatial
						// index as in the left-button path (#3, #17)
						for (Element el : circuit.elementsAt(x,y)) {
							if (el.contains(x,y)) {

								// can't display menu on attached wire end
								if (el instanceof WireEnd && ((WireEnd)el).isAttached())
									continue;

								makeOptionMenu(el);
								if (!el.isUneditable()) {
									optionMenu.add(cut);
									optionMenu.add(copy);
									optionMenu.add(delete);
									optionMenu.add(lock);
								}
								if (optionMenu.getComponentCount() > 0) {
									setState(State.option);
									selected.add(el);
									el.setHighlight(true);
									optionMenu.show(this,x,y);
								}
								return;
							}
						}
						newMenu.show(this,x,y);
					}
					return;
				}

				// if option menu is visible
				if (currentState == State.option) {
					setState(State.idle);
					for (Element el : selected) {
						el.setHighlight(false);
					}
					clearSelected();
					repaint();
					mousePressed(event);
					return;
				}

				// if elements are selected...
				if (currentState == State.selected) {

					// if cursor is inside selection rectangle
					if (selRect.contains(x,y)) {

						// if left button, start moving
						if (leftButton) {

							// do nothing if editor is disabled
							if (!enabled)
								return;

							for (Element sel : selected) {
								sel.savePosition();
							}
							setState(State.moving);
						}
						else if (rightButton) {

							// show options menu
							optionMenu.removeAll();
							if (selected.size() == 1) {
								Element el = (Element)(selected.toArray()[0]);
								makeOptionMenu(el);
							}
							boolean canEdit = true;
							for (Element el : selected) {
								if (el.isUneditable())
									canEdit = false;
							}
							if (canEdit) {
								optionMenu.add(cut);
								optionMenu.add(copy);
								optionMenu.add(delete);
								optionMenu.add(lock);
							}
							if (optionMenu.getComponentCount() > 0) {
								setState(State.option);
								selRect = null;
								repaint();
								optionMenu.show(this,x,y);
							}
						}
						return;
					}

					// otherwise deselect everything
					else {
						setState(State.idle);
						for (Element el : selected) {
							el.setHighlight(false);
						}
						clearSelected();
						selRect = null;
						repaint();
						return;
					}
				}

				// if placing a new element or pasted elements
				if (currentState == State.placing) {

					// cancel if right button
					if (rightButton) {

						for (Element el : selected) {
							el.remove(circuit);
						}
						setState(State.idle);
						clearSelected();
						untouchAll();
						repaint();
						return;
					}

					// don't do anything if there is overlap
					if (overlap()) {
						return;
					}

					// fix all positions
					for (Element el : selected) {
						el.fixPosition();
					}

					// connect everything connectable
					connect();
					markChanged();

					// clear up
					setState(State.idle);
					clearSelected();
					repaint();
					return;
				}

				// if drawing a wire...
				if (currentState == State.startwire || currentState == State.drawire) {

					// right button -> cancel
					if (rightButton) {

						// remove wire end and wire from circuit and wire net
						circuit.remove(wireEnd);
						if (wire != null) {
							circuit.remove(wire);
							wire.getOtherEnd(wireEnd).remove(wire,circuit);
							markChanged();
						}
						net.remove(wireEnd);
						net.remove(wire);

						// remove any colinear wire ends
						removeCoLinear();

						// reset for next time
						wireEnd = null;
						wire = null;
						prev = null;
						clearSelected();
						untouchAll();
						setState(State.idle);
						repaint();
						return;
					}

					// left button -> put in place and start next wire end
					else {

						// make sure no overlaps
						if (overlap()) {
							return;
						}

						// set end of wire
						wireEnd.fixPosition();

						// connect if possible
						connect();

						// finish up if wire becomes attached to another wire or put
						if (currentState == State.drawire && wireEnd.isAttached()) {

							// if attached to imaginary put, unattach it
							if (wireEnd.getPut().getElement() == null)
								wireEnd.setPut(null);
							wireEnd = null;
							wire = null;
							prev = null;
							clearSelected();
							setState(State.idle);
							markChanged();
							repaint();
							return;
						}

						// if attached to imaginary put, unattach it
						if (currentState == State.startwire && wireEnd.isAttached()) {
							if (wireEnd.getPut().getElement() == null)
								wireEnd.setPut(null);
						}

						// save current wire end
						prev = wireEnd;
						WireEnd save = wireEnd;

						// create new wire end
						wireEnd = new WireEnd(circuit);
						wireEnd.setXY(x,y);
						wireEnd.init(circuit);
						circuit.addElement(wireEnd);

						// create new wire
						wire = new Wire(save,wireEnd);
						save.addWire(wire);
						wireEnd.addWire(wire);
						circuit.addElement(wire);

						// add wire end and wire to net
						net.add(wireEnd);
						wireEnd.setNet(net);
						net.add(wire);
						wire.setNet(net);

						// set up new selected set
						clearSelected();
						selected.add(wireEnd);
						selected.add(wire);

						// finish up
						setState(State.drawire);
						repaint();
						return;
					}

				}
			} // end of mousePressed method

			/**
			 * Set up the options popup menu.
			 */
			private void makeOptionMenu(Element el) {

				optionMenu.removeAll();
				if (el.quickChange() && !el.isUneditable()) {
					optionMenu.add(el.setupQuickMenu(me));
				}
				if (el.canChange() && !el.isUneditable()) {
					optionMenu.add(modify);
				}
				if (el.hasTiming() && !el.isUneditable()) {
					optionMenu.add(timing);
				}
				if (el.canWatch()) {
					if (!el.isUneditable()) {
						if (el.isWatched()) {
							watch.setText("Un-watch Element");
						}
						else {
							watch.setText("Watch Element");
						}
						optionMenu.add(watch);
					}
					optionMenu.add(view);
				}
				if(el.canRotate())
				{
					if(!el.isUneditable())
					{
						optionMenu.add(Crotate);
						optionMenu.add(CCrotate);
					}
				}
				if(el.canFlip())
				{
					if(!el.isUneditable())
					{
						optionMenu.add(flip);
					}
				}
				if (el instanceof JumpStart) {
					if(!el.isUneditable())
					{
						optionMenu.add(matchJump);
					}
				}
				if (el instanceof Wire) {
					Wire wire = (Wire)el;
					if (wire.hasProbe()) {
						probe.setText("Remove Probe");
					}
					else {
						probe.setText("Attach Probe");
					}
					optionMenu.add(probe);
				}
			} // end of makeOptions method

			/**
			 * React to mouse release events.
			 * 
			 * @param event The event object for releases.
			 */
			@Override
			public void mouseReleased(MouseEvent event) {

				// do nothing if editor is disabled
				if (!enabled)
					return;

				boolean rightButton = (event.getButton() == MouseEvent.BUTTON3) || (event.isPopupTrigger());

				if (currentState == State.idle && rightButton) {

					// see if cursor is on an element, via the spatial index
					// as in mousePressed (#3, #17)
					for (Element el : circuit.elementsAt(x,y)) {
						if (el.contains(x,y)) {

							// can't display menu on attached wire end
							if (el instanceof WireEnd && ((WireEnd)el).isAttached())
								continue;

							makeOptionMenu(el);
							if (!el.isUneditable()) {
								optionMenu.add(cut);
								optionMenu.add(copy);
								optionMenu.add(delete);
								optionMenu.add(lock);
							}
							if (optionMenu.getComponentCount() > 0) {
								setState(State.option);
								selected.add(el);
								el.setHighlight(true);
								optionMenu.show(this,x,y);
							}
							return;
						}
					}
					newMenu.show(this,x,y);
					return;
				}

				info.setForeground(Color.BLACK);
				info.setText("");

				// if moving elements...
				if (currentState == State.moving) {

					// if overlaps
					if (overlap()) {

						// reset selected elements to original positions
						for (Element sel : selected) {
							sel.restorePosition();
						}
					}

					// otherwise 
					else {

						// fix elements at their new positions
						for (Element el : selected) {
							el.fixPosition();
						}

						// connect everything possible
						connect();

						// fix up wire end attached to imaginary puts
						for (Element el : selected) {
							if (el instanceof WireEnd) {
								WireEnd end = (WireEnd)el;
								if (end.isAttached() && end.getPut().getElement() == null) {
									end.setPut(null);
								}
							}
						}

						// record that changes have been made
						markChanged();
					}

					// clean up
					for (Element el : selected) {
						el.setHighlight(false);
					}
					removeCoLinear();
					clearSelected();
					setState(State.idle);
					repaint();
					return;
				}

				// if selecting elements...
				if (currentState == State.selecting) {

					// if something selected...
					if (selected.size() > 0) {

						// minimize selection rectangle
						selRect = null;
						for (Element el : selected) {

							// ignore degenerate rectangles (from wires)
							if (el.getRect().equals(new Rectangle())) {
								continue;
							}

							// add to building rectangle (create if the first)
							if (selRect == null) {
								selRect = new Rectangle(el.getRect());
							}
							else {
								selRect.add(el.getRect());
							}
						}

						// next click will decide next action
						if (selRect != null)
							setState(State.selected);
						else
							setState(State.idle);
					}
					else {

						// otherwise back to idle
						clearSelected();
						setState(State.idle);
						selRect = null;
					}
					repaint();
					return;
				}

			} // end of mouseReleased method

			/**
			 * React to mouse dragged events.
			 * 
			 * @param event The event object for drags.
			 */
			@Override
			public void mouseDragged(MouseEvent event) {

				// do nothing if editor is disabled
				if (!enabled)
					return;

				info.setForeground(Color.BLACK);
				info.setText("");

				// get cursor position
				int nx = event.getX();
				int ny = event.getY();

				// if moving elements...
				if (currentState == State.moving) {

					// everything that changes visually lies within the union
					// of the selection's bounds before and after the move,
					// plus whatever overlap() touch-marks or un-marks, so
					// only that region needs repainting (#43)
					Rectangle dirty = union(selectionBounds(), touchedBounds());

					// move them
					for (Element el : selected) {
						el.move(nx-x,ny-y);
					}
					circuit.reindexAfterMove(selected);
					x = nx;
					y = ny;

					// check for overlaps
					if (overlap()) {
						info.setText(overlapMessage);
						info.setForeground(Color.red);
					}
					else {
						info.setText("");
						info.setForeground(Color.black);
					}
					dirty = union(dirty, selectionBounds());
					dirty = union(dirty, touchedBounds());
					repaintDirty(dirty);
					return;
				}

				// if selecting elements...
				if (currentState == State.selecting) {

					// create bounding rectangle; anything that changes
					// visually (the outline, every highlight change) lies
					// within the union of the old and new rectangles, so
					// only that region needs repainting (#17)
					Rectangle dirty = selRect == null ? null : new Rectangle(selRect);
					int xc = Math.min(x,nx);
					int yc = Math.min(y,ny);
					int width = Math.abs(nx-x);
					int height = Math.abs(ny-y);
					selRect = new Rectangle(xc,yc,width,height);
					dirty = union(dirty, selRect);

					// select elements completely inside bounding rectangle,
					// querying the spatial index instead of scanning the
					// whole circuit (#17); attached wire ends inside the
					// rectangle are left untouched, exactly as before
					Set<Element> inside = new HashSet<Element>();
					Set<Element> attachedInside = new HashSet<Element>();
					for (Element el : circuit.elementsNear(selRect)) {
						if (el.isInside(selRect)) {
							if (el instanceof WireEnd && ((WireEnd)el).isAttached()) {
								attachedInside.add(el);
								continue;
							}
							inside.add(el);
						}
					}

					// unselect anything highlighted or selected that fell
					// outside the rectangle; highlight changes on elements
					// larger than the rectangle union (e.g. a hover
					// highlight from before the drag) widen the dirty
					// region by their own bounds
					Set<Element> wasHighlighted = circuit.getHighlighted();
					Set<Element> toClear = new HashSet<Element>(wasHighlighted);
					toClear.addAll(selected);
					for (Element el : toClear) {
						if (!inside.contains(el) && !attachedInside.contains(el)) {
							el.setHighlight(false);
							selected.remove(el);
							dirty = union(dirty, el.getRect());
						}
					}
					for (Element el : inside) {
						if (!wasHighlighted.contains(el)) {
							dirty = union(dirty, el.getRect());
						}
						el.setHighlight(true);
						selected.add(el);
					}
					dirty.grow(JLSInfo.spacing, JLSInfo.spacing);
					repaint(dirty);
					return;
				}

			} // end of mouseDragged method

			/**
			 * Accumulate a dirty-region rectangle (#17): the union of the
			 * given rectangles, either of which may be null.
			 */
			private Rectangle union(Rectangle acc, Rectangle add) {

				if (acc == null) {
					return add == null ? null : new Rectangle(add);
				}
				if (add != null) {
					acc.add(add);
				}
				return acc;
			} // end of union method

			/**
			 * The screen area the current selection can draw in: every
			 * selected element's bounds, plus the full span of any wire
			 * hanging off a selected wire end or off a wire end attached to
			 * a selected element's puts (moving an element stretches those
			 * wires, so their whole old/new span redraws). Used to build
			 * move/place dirty regions (#43).
			 *
			 * @return the union of those bounds, or null if nothing is
			 * selected.
			 */
			private Rectangle selectionBounds() {

				Rectangle acc = null;
				for (Element el : selected) {
					acc = union(acc, el.getRect());
					if (el instanceof WireEnd) {
						for (Wire w : ((WireEnd)el).getWires()) {
							acc = union(acc, w.getRect());
						}
					}
					for (Put p : el.getAllPuts()) {
						WireEnd end = p.getWireEnd();
						if (end != null) {
							acc = union(acc, end.getRect());
							for (Wire w : end.getWires()) {
								acc = union(acc, w.getRect());
							}
						}
					}
				}
				return acc;
			} // end of selectionBounds method

			/**
			 * The screen area covered by everything currently touch-marked
			 * by overlap()/connect(): touched elements (wires redraw along
			 * their whole span when their color changes) and touched puts.
			 * Called before and after overlap() so both the marks it clears
			 * and the marks it sets fall inside the dirty region (#43).
			 *
			 * @return the union of those bounds, or null if nothing is
			 * touched.
			 */
			private Rectangle touchedBounds() {

				Rectangle acc = null;
				for (Element el : touchedElements) {
					acc = union(acc, el.getRect());
				}
				int d = JLSInfo.pointDiameter;
				for (Put put : touchedPuts) {
					acc = union(acc, new Rectangle(put.getX()-d, put.getY()-d,
							2*d, 2*d));
				}
				return acc;
			} // end of touchedBounds method

			/**
			 * Repaint a dirty region built by the move/place paths, padded
			 * so highlight rings, connect markers, and labels drawn near
			 * (but outside) element bounds are covered -- the same margin
			 * Circuit.mayBeVisible assumes when clipping the redraw (#43).
			 * A null region (nothing known to have changed) conservatively
			 * repaints everything.
			 *
			 * @param dirty The region that changed, or null.
			 */
			private void repaintDirty(Rectangle dirty) {

				if (dirty == null) {
					repaint();
					return;
				}
				dirty.grow(8*JLSInfo.spacing, 8*JLSInfo.spacing);
				repaint(dirty);
			} // end of repaintDirty method

			/**
			 * React to mouse movements.
			 *
			 * @param event The event object for moves.
			 */
			@Override
			public void mouseMoved(MouseEvent event) {

				// do nothing if editor is disabled
				if (!enabled)
					return;

				info.setForeground(Color.BLACK);
				info.setText("");

				// get mouse coordinates
				int nx = event.getX();
				int ny = event.getY();

				info.setText(" ");
				if (currentState == State.idle) {

					// select elements when cursor moves over them, asking
					// the spatial index for the few elements near the
					// cursor instead of scanning the whole circuit (#17);
					// attached wire ends under the cursor are left
					// untouched, exactly as before
					selected.clear();
					Set<Element> under = new HashSet<Element>();
					Set<Element> attachedUnder = new HashSet<Element>();
					for (Element el : circuit.elementsAt(nx,ny)) {
						if (el.contains(nx,ny)) {
							if (el instanceof WireEnd && ((WireEnd)el).isAttached()) {
								attachedUnder.add(el);
								continue;
							}
							under.add(el);
						}
					}

					// repaint only where highlights change (#17): idle
					// motion over unchanged content costs no drawing
					Set<Element> wasHighlighted = circuit.getHighlighted();
					Rectangle dirty = null;

					// unhighlight whatever the cursor left
					for (Element el : wasHighlighted) {
						if (!under.contains(el) && !attachedUnder.contains(el)) {
							el.setHighlight(false);
							dirty = union(dirty, el.getRect());
						}
					}

					// highlight and display info
					for (Element el : under) {
						selected.add(el);
						if (!wasHighlighted.contains(el)) {
							dirty = union(dirty, el.getRect());
						}
						el.setHighlight(true);
						el.showInfo(info);
					}
					if (dirty != null) {
						dirty.grow(JLSInfo.spacing, JLSInfo.spacing);
						repaint(dirty);
					}
					return;
				}

				// place a just chosen element
				if (currentState == State.chosen) {
					Element item = (Element)(selected.toArray()[0]);
					Point p = getMousePosition();
					if (p == null)
						return;
					x = p.x;
					y = p.y;
					item.setXY(p.x,p.y);
					item.savePosition();

					// place it (a state transition, once per placement
					// gesture, so a full repaint is fine here)
					setState(State.placing);
					repaint();
					return;
				}

				// move element while initially placing it
				if (currentState == State.placing || currentState == State.startwire ||
						currentState == State.drawire) {

					// as in the moving drag path, repaint only the union of
					// the selection's old and new bounds plus the
					// touch-marked elements (#43)
					Rectangle dirty = union(selectionBounds(), touchedBounds());
					for (Element el : selected) {
						el.move(nx-x,ny-y);
					}
					circuit.reindexAfterMove(selected);
					x = nx;
					y = ny;

					// check for overlaps
					if (overlap()) {

						// don't show overlap while new wire end is still
						// close to the previously placed wire end
						if (!(currentState == State.drawire &&
								prev != null && prev.getX() == wireEnd.getX() &&
								prev.getY() == wireEnd.getY())) {
							info.setText(overlapMessage);
							info.setForeground(Color.red);
						}
					}
					else {
						prev = null;
						info.setText("");
						info.setForeground(Color.black);
					}
					dirty = union(dirty, selectionBounds());
					dirty = union(dirty, touchedBounds());
					repaintDirty(dirty);
					return;
				}
			} // end of mouseMoved method

			/**
			 * Unused.
			 * 
			 * @param event Unused.
			 */
			@Override
			public void mouseClicked(MouseEvent event) {}

			/**
			 * Get focus for keyboard events.
			 * 
			 * @param event Unused.
			 */
			@Override
			public void mouseEntered(MouseEvent event) {

				requestFocusInWindow(true);
			} // end of mouseEntered method

			/**
			 * If in the idle state, unhighlight everything.
			 * 
			 * @param event Unused.
			 */
			@Override
			public void mouseExited(MouseEvent event) {

				if (currentState == State.idle) {
					for (Element el : circuit.getHighlighted()) {
						el.setHighlight(false);
					}
					repaint();
				}
			} // end of mouseExited method

			/**
			 * Update current state and show corresponding message.
			 * 
			 * @param newState The new state.
			 */
			private void setState(State newState) {

				// gesture transitions are where uncovered geometry changes
				// (snap, aborted move, rotate) land; one lazy index rebuild
				// per transition keeps the spatial index honest (#17)
				circuit.invalidateIndex();
				currentState = newState;
				switch (currentState) {
				case idle:
					message.setText(" ");
					message.setBackground(Color.CYAN);
					break;
				case chosen:
				case placing:
					message.setText("left click to place, right click to cancel");
					message.setBackground(Color.yellow);
					break;
				case moving:
					message.setText("moving element(s)");
					message.setBackground(Color.yellow);
					break;
				case selecting:
					message.setText("selecting element(s)");
					message.setBackground(Color.yellow);
					break;
				case selected:
					message.setText("element(s) selected");
					message.setBackground(Color.yellow);
					break;
				case option:
					message.setText("pick option for selected element(s)");
					message.setBackground(Color.yellow);
					break;
				case startwire:
					message.setText("pick start end of wire");
					message.setBackground(Color.yellow);
					break;
				case drawire:
					message.setText("pick next end of wire, right click to end");
					message.setBackground(Color.yellow);
					break;
				}
			} // end of updateMessage method

			/**
			 * See if wire ends can connect.
			 * Not possible if one end is not dangling.
			 * Not possible if part of the same wire net.
			 * Not possible if bits don't match.
			 * Not possible if both have inputs already.
			 * 
			 * @param end1 The wire being connected.
			 * @param end2 The other wire being connected.
			 * 
			 * @return false if bits don' match or both have inputs, true otherwise.
			 */
			private boolean canConnect(WireEnd end1, WireEnd end2) {

				if (!end1.isDangling() && !end2.isDangling()) {
					overlapMessage = "One end must dangle";
					return false;
				}

				// make sure not the same net
				if (end1.getNet() == end2.getNet()) {
					overlapMessage = "Can't make a wire loop";
					return false;
				}

				// make sure not merging two nets that will make a loop
				if (end1.getNet().netOverlap(end2.getNet())) {
					overlapMessage = "Can't make a wire loop";
					return false;
				}

				// make sure wire sizes match
				int bits1 = end1.getBits();
				int bits2 = end2.getBits();
				if (bits1 > 0 && bits2 > 0 && bits1 != bits2) {
					overlapMessage = "Bits don't match";
					return false;
				}

				// can't attach to more than one output unless both are tristate
				if (end1.getNet().hasInput() && end2.getNet().hasInput()) {
					if (end1.isTriState() && end2.isTriState())
						return true;
					overlapMessage = "Both wires have inputs";
					return false;
				}

				return true;
			} // end of canConnect method

			/**
			 * Connect exiting wire ends.
			 * Assumes ends can be connected (overlap, one end dangling, one input, bits match,
			 * not same net).
			 * One wire end will be deleted, and the remaining one is returned.
			 * 
			 * @param end1 The wire end being connected.
			 * @param end2 The other wire end being connected.
			 * 
			 * @return the remaining wire end.
			 */
			private WireEnd connect(WireEnd end1, WireEnd end2) {

				// get wire nets
				WireNet net1 = end1.getNet();
				WireNet net2 = end2.getNet();

				// get bits right
				int bits = Math.max(end1.getBits(),end2.getBits());
				net1.setBits(bits);
				net2.setBits(bits);

				// get hasInput right
				if (net1.hasInput() || net2.hasInput()) {
					net1.setInput();
					net2.setInput();
				}

				// get tristate right
				if (net1.isTriState() || net2.isTriState()) {
					net1.setTriState(true);
					net2.setTriState(true);
				}

				// make end2 be the dangling end
				if (end1.isDangling()) {
					WireEnd tempe = end1;
					end1 = end2;
					end2 = tempe;
					WireNet tempn = net1;
					net1 = net2;
					net2 = tempn;
				}

				// get rid of dangling wire end
				subs.add(end2);
				net2.remove(end2);

				// get next wire end from dangling wire end
				Wire wire = end2.getOnlyWire();

				// if none, do nothing
				if (wire == null) {
					return end1;
				}
				WireEnd otherEnd = wire.getOtherEnd(end2);

				// connect wire ends
				wire.setEnds(end1,otherEnd);
				end1.addWire(wire);

				// merge new net into old one
				net1.absorb(net2);

				// indicate removed wire is attached to an invisible input
				end2.setPut(new Input(null,null,0,0,0));

				return end1;

			} // end of connect method (wire end to wire end)

			/**
			 * See if a wire end can attach to a wire.
			 * 
			 * @param end1 The wire end.
			 * @param wire The wire.
			 * 
			 * @returns true if can connect, false if not.
			 */
			public boolean canConnect(WireEnd end1, Wire wire) {

				WireEnd end2 = wire.getEnd();

				if (!end1.isDangling()) {
					overlapMessage = "Wire end must dangle";
					return false;
				}

				// make sure no wire loop possible
				if (end2.getNet().netOverlap(end1.getNet())) {
					overlapMessage = "Can't make a wire loop";
					return false;
				}

				// make sure not multiple ends connecting to this wire
				int count = 0;
				for (Element el : selected) {
					if (el instanceof WireEnd) {
						WireEnd end = (WireEnd)el;
						if (wire.contains(end.getX(),end.getY())) {
							count += 1;
						}
					}
				}
				if (count > 1) {
					overlapMessage = "Multiple connects to same wire not implemented";
					return false;
				}

				// make sure wire sizes match
				int bits1 = end1.getBits();
				int bits2 = end2.getBits();
				if (bits1 > 0 && bits2 > 0 && bits1 != bits2) {
					overlapMessage = "Bits don't match";
					return false;
				}

				// can't attach to more than one output unless both are tristate
				if (end1.getNet().hasInput() && end2.getNet().hasInput()) {
					if (end1.isTriState() && end2.isTriState())
						return true;
					overlapMessage = "Both wires have inputs";
					return false;
				}

				return true;
			} // end of canConnect method (wire end to wire)

			/**
			 * Connect a wire end to a wire.
			 * Return the end that was connected.
			 * 
			 * @param end The wire end.
			 * @param wire The wire.
			 * 
			 * @return the end that was connected.
			 */
			public WireEnd connect(WireEnd end, Wire wire) {

				WireEnd end1 = wire.getEnd();
				WireEnd end2 = wire.getOtherEnd(end1);

				// get wire nets
				WireNet net1 = end.getNet();
				WireNet net2 = end1.getNet();

				// get bits right
				int bits = Math.max(end.getBits(),end1.getBits());
				net1.setBits(bits);

				// get hasInput right
				if (net1.hasInput() || net2.hasInput()) {
					net1.setInput();
				}

				// get tristate right
				if (net1.isTriState() || net2.isTriState()) {
					net1.setTriState(true);
				}

				// splice in new wire end
				Wire wire1 = new Wire(end1,end);
				end1.addWire(wire1);
				end.addWire(wire1);
				wire1.setNet(net1);
				net1.add(wire1);
				adds.add(wire1);

				Wire wire2 = new Wire(end2,end);
				end2.addWire(wire2);
				end.addWire(wire2);
				wire2.setNet(net1);
				net1.add(wire2);
				adds.add(wire2);

				end1.remove(wire);
				end2.remove(wire);
				net2.remove(wire);
				subs.add(wire);

				// merge new net into old one
				net1.absorb(net2);

				// indicate connected wire end is attached
				end.setPut(new Input(null,null,0,0,0));

				return end;

			} // end of connect method (wire end to wire)

			/**
			 * See if a wire end can attach to a put.
			 * Not possible if put is already attached, or bits don't match, or
			 * put is an output and wire end is already connected to an input.
			 * 
			 * @param end The wire end.
			 * @param put The put.
			 * 
			 * @return false if put is already attached or bits do not match, true otherwise.
			 */
			private boolean canConnect(WireEnd end, Put put) {

				// can't attach a non-dangling wire end
				if (!end.isDangling()) {
					overlapMessage = "Must be a wire end";
					return false;
				}

				// can't attach to an already attached put
				if (put.isAttached()) {
					overlapMessage = "Input/output already attached";
					return false;
				}

				// can't attach if bits don't match
				int wireBits = end.getBits();
				int putBits = put.getBits();
				if (wireBits > 0 && putBits > 0 && wireBits != putBits) {
					overlapMessage = "Bits don't match";
					return false;
				}

				// can't attach if put is an output and wire end has an input
				// unless both are tristate
				if (put instanceof Output && end.getNet().hasInput()) {
					Output out = (Output)put;
					if (!(out.isTriState() && end.isTriState())) {
						overlapMessage = "Wire already has an input";
						return false;
					}
				}

				// groups cannot have both tri-state and normal connections
				if (mixesTriStateAndNormal(end,put)) {
					overlapMessage = "Cannot connect both tri-state and normal wires to a bundle";
					return false;
				}

				// can't attach if multiple wire ends in the same wire net
				// can attach to outputs, unless they are all tristates.
				// Only elements near each net end can have a put within
				// getPut's half-spacing tolerance of it, so ask the spatial
				// index for those few candidates instead of scanning the
				// whole circuit for every net end on every drag event (#43);
				// the getPut/instanceof predicates are unchanged, so the
				// same puts are counted as before
				int regCount = 0;
				int triCount = 0;
				for (WireEnd otherEnd : end.getNet().getAllEnds()) {
					if (end == otherEnd)
						continue;
					for (Element el : circuit.elementsAt(otherEnd.getX(),otherEnd.getY())) {
						Put p = el.getPut(otherEnd.getX(),otherEnd.getY());
						if (p != null && p instanceof Output) {
							Output out = (Output)p;
							if (out.isTriState()) {
								triCount += 1;
							}
							else {
								regCount += 1;
							}
						}
					}
				}
				if (!(regCount == 0 || regCount == 1 && triCount == 0)) {
					overlapMessage = "Wire will have multiple inputs";
					return false;
				}

				return true;
			} // end of canConnect method (wire end to put)

			/**
			 * Attach a wire end to a put.
			 * The wire end must be dangling.
			 * The put must not be attached.
			 * Bits must match, and the wire net must not already have an input if this
			 * put is an output.
			 * Bits will be set in the net.
			 * Has-input will be set in the net if necessary.
			 * Tri-state will be set in the net if necessary.
			 * 
			 * @param end The wire end.
			 * @param put The put.
			 */
			private void connect(WireEnd end, Put put) {

				put.setAttached(end);
				end.setPut(put);
				WireNet net = end.getNet();
				if (net.getBits() == 0) {
					net.setBits(put.getBits());
				}
				if (put instanceof Output) {
					net.setInput();
					Output out = (Output)put;
					if (out.isTriState()) {
						net.setTriState(true);
					}
				}
				else if (put instanceof Input && net.isTriState()) {
					if (put.getElement() instanceof TriProp) {
						TriProp pin = (TriProp)put.getElement();
						pin.setTriState(true);
					}
				}
			} // end of connect method (wire end to put)

			/**
			 * See if a put can attach to another put.
			 * Not possible of they don't overlap.
			 * Not possible if either put is already attached.
			 * Not possible if bits don't match.
			 * Not possible if both are outputs or both are inputs,
			 * unless both are tristate outputs.
			 * 
			 * @param p1 The put.
			 * @param p2 The other put.
			 * 
			 * @return true if puts can be attached, false if not.
			 */
			private boolean canConnect(Put p1, Put p2) {

				// make sure puts are not attached
				if (p1.isAttached() || p2.isAttached()) {
					overlapMessage = "Input/output already attached";
					return false;
				}

				// can't attach if bits don't match
				int b1 = p1.getBits();
				int b2 = p2.getBits();
				if (b1 > 0 && b2 > 0 && b1 != b2) {
					overlapMessage = "Bits don't match";
					return false;
				}

				// can't connect output to output unless both are tristate
				if (p1 instanceof Output && p2 instanceof Output) {
					Output out1 = (Output)p1;
					Output out2 = (Output)p2;
					if (!(out1.isTriState() && out2.isTriState())) {
						overlapMessage = "Can't connect output to output";
						return false;
					}
				}

				return true;
			} // end of canConnect method (put to put)

			/**
			 * Attach a put to a put by creating a wire net with two wire ends and a wire.
			 * The puts must overlap.
			 * The puts must not be attached.
			 * Bits must match
			 * 
			 * @param p1 The put.
			 * @param p2 The other put.
			 */
			private void connect(Put p1, Put p2) {

				// create wire net, wire ends and wire
				int x = p1.getX();
				int y = p1.getY();

				// create and set up wire net
				WireNet net = new WireNet();
				net.setBits(Math.max(p1.getBits(),p2.getBits()));
				net.setInput();

				// create one end of wire
				WireEnd end1 = new WireEnd(circuit);
				end1.setXY(x,y);
				end1.init(circuit);
				end1.setPut(p1);
				end1.setNet(net);
				net.add(end1);

				// create other end of wire
				WireEnd end2 = new WireEnd(circuit);
				end2.setXY(x,y);
				end2.init(circuit);
				end2.setPut(p2);
				end2.setNet(net);
				net.add(end2);

				// create wire
				Wire wire = new Wire(end1,end2);
				end1.addWire(wire);
				end2.addWire(wire);
				net.add(wire);
				wire.setNet(net);

				// put wire and wire ends in adds set for now
				adds.add(wire);
				adds.add(end1);
				adds.add(end2);

				// attach wire ends to puts
				p1.setAttached(end1);
				p2.setAttached(end2);

				// fix positions of wire ends through their elements
				p1.getElement().fixPosition();
				p2.getElement().fixPosition();

				// make net tristate if either put is a tristate output
				if (p1 instanceof Output && ((Output)p1).isTriState()) {
					net.setTriState(true);
				}
				else if (p2 instanceof Output && ((Output)p2).isTriState()) {
					net.setTriState(true);
				}

				// make output pin tristate if appropriate
				if (net.isTriState() && p1.getElement() instanceof TriProp) {
					TriProp pin = (TriProp)p1.getElement();
					pin.setTriState(true);
				}
				else if (net.isTriState() && p2.getElement() instanceof TriProp) {
					TriProp pin = (TriProp)p2.getElement();
					pin.setTriState(true);
				}
			}

				/**
				 * See if the selected elements overlap non-selected elements.
				 * Highlights possible connections when there is no overlap.
				 *
				 * Candidates come from the circuit's spatial index instead
				 * of a scan of every element, so per-drag-event cost follows
				 * the selection size, not the circuit size (#3, #17). The
				 * accept/reject predicates themselves are unchanged.
				 *
				 * @return true if there is overlap, false if not.
				 */
				private boolean overlap() {

					overlapMessage = "";

						// check every element in the selected set
						for (Element sel : selected) {

							// wires hanging off a moved wire end, or off a
							// wire end attached to one of sel's puts, must
							// not land on other wire ends or sweep across
							// stationary elements anywhere along their span
							// (wireCollidesAlongSpan; the element predicate
							// is the same one the reverse drag direction
							// uses); these checks depend only on sel
							if (sel instanceof WireEnd) {
								WireEnd end = (WireEnd)sel;
								for (Wire wire : end.getWires()) {
									if (wireCollidesAlongSpan(circuit,selected,sel,wire)) {
										overlapMessage = "overlap";
										untouchAll();
										return true;
									}
								}
							}
							for (Put p : sel.getAllPuts()) {
								if (p.isAttached()) {
									Wire wire = p.getWireEnd().getOnlyWire();
									if (wireCollidesAlongSpan(circuit,selected,sel,wire)) {
										overlapMessage = "overlap";
										untouchAll();
										return true;
									}
								}
							}

							// check against every element near the selection
							// (grown so edge-touching put alignments are
							// included)
							Rectangle near = sel.getIndexBounds();
							near.grow(JLSInfo.spacing,JLSInfo.spacing);
							for (Element el : circuit.elementsNear(near)) {

								// ignore elements in the selected set
								if (selected.contains(el))
									continue;

								// check simple overlap of areas
								if (sel.intersects(el)) {

									// no overlap if possible connection,
									boolean ok = false;

									// if selected is a wire end ...
									if (sel instanceof WireEnd) {

										WireEnd end = (WireEnd)sel;

										if (el instanceof WireEnd) {

											// wire end to wire end
											WireEnd otherEnd = (WireEnd)el;
											if (!canConnect(end,otherEnd)) {
												untouchAll();
												return true;
											}
											touch(end);
											touch(otherEnd);
											ok = true;
										}
										else if (el instanceof Wire) {

											// wire end to wire
											Wire wire = (Wire)el;
											if (!canConnect(end,wire)) {
												untouchAll();
												return true;
											}
											touch(end);
											touch(wire);
											ok = true;
										}
										else {

											// wire end to put

											// can't attach if there is no put
											Put put = el.getPut(end.getX(),end.getY());
											if (put == null) {
												overlapMessage = "overlap";
												untouchAll();
												return true;
											}

											// if already attached, ignore
											WireEnd putEnd = put.getWireEnd();
											if (end == putEnd) {
												ok = true;
												continue;
											}

											// if attached through a single wire, ignore
											if (putEnd != null) {
												Wire onlyWire = putEnd.getOnlyWire();
												if (onlyWire.getOtherEnd(putEnd) == end) {
													ok = true;
													continue;
												}
											}

											// make sure connection can be made to this put
											if (!canConnect(end,put)) {
												untouchAll();
												return true;
											}

											// no overlap if we get this far
											touch(end);
											touch(put);
											ok = true;
										}
									}

									// selected is not a wire end
									else {

										// put to wire end
										for (Put put : sel.getAllPuts()) {

											// if not a wire end, ignore
											if (!(el instanceof WireEnd))
												continue;
											WireEnd end = (WireEnd)el;

											// if don't line up, ignore
											if (put.getX() != end.getX() || put.getY() != end.getY()) {
												continue;
											}

											// if already attached to this wire end, ignore
											if (end == put.getWireEnd()) {
												ok = true;
												continue;
											}

											// if attached through a single wire, ignore
											WireEnd putEnd = put.getWireEnd();
											if (putEnd != null &&
													putEnd.getOnlyWire().getOtherEnd(putEnd) == end) {
												ok = true;
												continue;
											}

											// if cannot connect, return
											if (!canConnect(end,put)) {
												untouchAll();
												return true;
											}

											touch(end);
											touch(put);
											ok = true;
										}
									}
									if (!ok) {
										overlapMessage = "overlap";
										untouchAll();
										return true;
									}
								}

								// no intersection, but puts might line up
								else {

									// check all put combinations
									for (Put p1 : sel.getAllPuts()) {
										for (Put p2 : el.getAllPuts()) {

											// if don't line up, ignore
											if (p1.getX() != p2.getX() || p1.getY() != p2.getY()) {
												continue;
											}

											// ignore overlaps on already connected puts
											WireEnd end1 = p1.getWireEnd();
											WireEnd end2 = p2.getWireEnd();
											if (end1 != null && end2 != null && 
													end1.getOnlyWire().getOtherEnd(end1) == end2) {
												continue;
											}

											// make sure can connect
											if (!canConnect(p1,p2)) {
												untouchAll();
												return true;
											}
											else {
												touch(p1);
												touch(p2);
											}
										}
									}
								}
							}
						}
					// no repaint here: every caller repaints after overlap()
					// returns false, either the full canvas (gesture ends in
					// mousePressed/mouseReleased) or the dirty region around
					// the moved selection (drag/place motion, #43)
					return false;
					} // end of overlap method

					/**
					 * Connect everything possible.
					 * Assumes there is no overlap.
					 */
					public void connect() {

						// untouch everything
						untouchAll();

						// clear adds
						adds.clear();
						subs.clear();

						// check every element in the selected set
						for (Element sel : selected) {

							// whether this element actually made a connection
							// (only then can the index have gone stale)
							boolean connected = false;

							// check against every element near the selection
							// (grown so edge-touching put alignments are
							// included), as in overlap() (#3, #17)
							Rectangle near = sel.getIndexBounds();
							near.grow(JLSInfo.spacing,JLSInfo.spacing);
							for (Element el : circuit.elementsNear(near)) {

								// ignore elements in the selected set
								if (selected.contains(el))
									continue;

								// check simple overlap of areas
								if (sel.intersects(el)) {

									// no overlap if possible connection,
									boolean ok = false;

									// if a wire end ...
									if (sel instanceof WireEnd) {

										WireEnd end = (WireEnd)sel;

										if (el instanceof WireEnd) {

											// wire end to wire end
											WireEnd otherEnd = (WireEnd)el;
											WireEnd endLeft = connect(end,otherEnd);
											connected = true;
											if (currentState == State.startwire) {
												wireEnd = endLeft;
												net = endLeft.getNet();
											}
											ok = true;
										}
										else if (el instanceof Wire) {

											// wire end to wire
											Wire wire = (Wire)el;
											WireEnd it = connect(end,wire);
											connected = true;
											if (currentState == State.startwire) {
												wireEnd = it;
												net = it.getNet();
											}
											ok = true;
										}
										else {

											// wire end to put
											Put put = el.getPut(end.getX(),end.getY());

											// if already attached, ignore
											if (end == put.getWireEnd()) {
												ok = true;
												continue;
											}

											// if attached through a single wire, ignore
											WireEnd putEnd = put.getWireEnd();
											if (putEnd != null &&
													putEnd.getOnlyWire().getOtherEnd(putEnd) == end) {
												ok = true;
												continue;
											}
											connect(end,put);
											connected = true;
											ok = true;
										}
									}

									// selected is not a wire end
									else {

										// put to wire end
										for (Put put : sel.getAllPuts()) {

											// if not a wire end, ignore
											if (!(el instanceof WireEnd))
												continue;
											WireEnd end = (WireEnd)el;

											// if don't line up, ignore
											if (put.getX() != end.getX() || put.getY() != end.getY()) {
												continue;
											}

											// if already attached to this wire end, ignore
											if (end == put.getWireEnd()) {
												ok = true;
												continue;
											}

											// if attached through a single wire, ignore
											WireEnd putEnd = put.getWireEnd();
											if (putEnd != null &&
													putEnd.getOnlyWire().getOtherEnd(putEnd) == end) {
												ok = true;
												continue;
											}

											connect(end,put);
											connected = true;
											ok = true;
										}
									}
									if (!ok) {
										return;
									}
								}

								// no intersection, but puts might line up
								else {

									// check all put combinations
									for (Put p1 : sel.getAllPuts()) {
										for (Put p2 : el.getAllPuts()) {

											// if don't line up, ignore
											if (p1.getX() != p2.getX() || p1.getY() != p2.getY()) {
												continue;
											}

											// ignore overlaps on already connected puts
											WireEnd end1 = p1.getWireEnd();
											WireEnd end2 = p2.getWireEnd();
											if (end1 != null && end2 != null && 
													end1.getOnlyWire().getOtherEnd(end1) == end2) {
												continue;
											}

											// put to put
											connect(p1,p2);
											connected = true;
										}
									}
								}
							}

							// connections made for this element may have
							// moved wires and merged ends; rebuild before
							// the next element queries the index. When
							// nothing connected, nothing moved, so the
							// common no-connection drop stays O(selected)
							// instead of paying a rebuild per element
							if (connected) {
								circuit.invalidateIndex();
							}
						}

						// add any new wires created by connecting puts to puts
						for (Element el : adds) {
							circuit.addElement(el);
						}

						// remove any wire ends when merging nets
						// or wires when splicing
						for (Element el : subs) {
							circuit.remove(el);
						}
					} // end of connect method

					/**
					 * Untouch all wire ends and puts. Only elements and puts
					 * recorded by touch() can be touching, so this is
					 * O(touched) instead of a full-circuit scan (#17).
					 */
					private void untouchAll() {

						for (Element el : touchedElements) {
							el.setTouching(false);
						}
						touchedElements.clear();
						for (Put put : touchedPuts) {
							put.setTouching(false);
						}
						touchedPuts.clear();
					} // end of untouchAll method

					/**
					 * Mark an element as touching and remember it so
					 * untouchAll can clear exactly what was set.
					 *
					 * @param el The element now touching.
					 */
					private void touch(Element el) {

						el.setTouching(true);
						touchedElements.add(el);
					} // end of touch method

					/**
					 * Mark a put as touching and remember it so untouchAll
					 * can clear exactly what was set.
					 *
					 * @param put The put now touching.
					 */
					private void touch(Put put) {

						put.setTouching(true);
						touchedPuts.add(put);
					} // end of touch method

					/**
					 * Copy all selected elements to the clipboard.
					 */
					private void copy() {

						// clear clipboard
						clipboard.clear();

						// do copy
						Point min = Util.copy(selected,clipboard);

						// adjust positions of all elements and set circuit they are in
						for (Element el : clipboard.getElements()) {

							el.setCircuit(clipboard);

							// don't adjust wires
							if (el instanceof Wire)
								continue;

							// or attached wire ends
							if (el instanceof WireEnd) {
								WireEnd end = (WireEnd)el;
								if (end.isAttached())
									continue;
							}

							// adjust
							el.move(-min.x,-min.y);
						}
					} // end of copy method

					/**
					 * Remove all elements in the selected set.
					 * Abort removal if any elements in the set are uneditable.
					 */
					private void remove() {

						// make sure no element is uneditable
						for (Element el : selected) {
							if (el.isUneditable()) {
								TellUser.error(JLSInfo.frame,
										"can't delete uneditable element", "Error");
								return;
							}
						}

						// remove each element
						for (Element el : selected) {
							el.remove(circuit); // elements remove themselves
						}

						// mark the circuit as changed
						markChanged();
					} // end of remove method

					/**
					 * Paste all elements from a given circuit into the current circuit.
					 * Can't be done if there are elements in the "from" circuit that
					 * have the same names as elements in the current circuit.
					 * 
					 * @param from The circuit to copy from.
					 * 
					 * @return false if can't be done, true if done.
					 */
					private boolean paste(Circuit from) {

						// check for naming conflicts
						for (Element el : from.getElements()) {
							if (el instanceof JumpEnd)
								continue;
							String name = el.getName();
							if (name != null && !name.isEmpty()) {
								if (circuit.hasName(name)) {
									info.setForeground(Color.red);
									info.setText("Paste will result in elements with duplicate names");
									return false;
								}
							}
						}

						// check for duplicate jump start names
						// also save all jump start names in pasted circuit
						Set<String> jsnames = new HashSet<String>();
						for (Element el : from.getElements()) {
							if (el instanceof JumpStart) {
								String name = el.getName();
								jsnames.add(name);
								if (circuit.getJumpStart(name) != null) {
									info.setForeground(Color.red);
									info.setText("Paste will result in duplicate named wires");
									return false;
								}
							}
						}

						// make sure there are no jump ends without a matching jump start
						for (Element el : from.getElements()) {
							if (el instanceof JumpEnd) {
								String name = el.getName();
								if (!jsnames.contains(name) && circuit.getJumpStart(name) == null) {
									info.setForeground(Color.red);
									info.setText("Paste will result in wire end(s) with no source");
									return false;
								}
							}
						}

						// add names to circuit name list
						for (Element el : from.getElements()) {
							String name = el.getName();
							if (name != null && !name.isEmpty())
								circuit.addName(name);
						}

						// initialize
						clearSelected();
						Point pos = getMousePosition();
						if (pos == null)
							return false;
						x = pos.x;
						y = pos.y;

						// first copy all but wires and wire ends
						for (Element el : from.getElements()) {
							if (el instanceof Wire || el instanceof WireEnd) 
								continue;
							Element cel = el.copy();
							cel.fixPosition();
							cel.move(x,y);
							circuit.addElement(cel);
							selected.add(cel);
							cel.setHighlight(true);

							// if a jump start, add name to start list
							if (cel instanceof JumpStart) {
								JumpStart j = (JumpStart)cel;
								circuit.addJumpStart(j.getName(),j);
							}
						}

						// now copy all wire ends, checking for those attached to puts
						for (Element el : from.getElements()) {
							if (!(el instanceof WireEnd))
								continue;
							WireEnd oldEnd = (WireEnd)el;
							WireEnd newEnd = (WireEnd)(el.copy());
							newEnd.fixPosition();
							newEnd.move(x,y);
							circuit.addElement(newEnd);
							if (oldEnd.isAttached()) {
								Put newPut = oldEnd.getPut().getCopy();
								newEnd.setPut(newPut);
								newPut.setAttached(newEnd);
							}
							else {
								selected.add(newEnd);
								newEnd.setHighlight(true);
							}
						}

						// add wires
						for (Element el : from.getElements()) {
							if (!(el instanceof Wire))
								continue;
							Wire wire = (Wire)el;
							WireEnd end1 = wire.getEnd();
							WireEnd end2 = wire.getOtherEnd(end1);

							// create new wire and add to new ends
							Wire newWire = new Wire(end1.getCopy(),end2.getCopy());
							end1.getCopy().addWire(newWire);
							end2.getCopy().addWire(newWire);
							circuit.addElement(newWire);
							selected.add(newWire);
						}

						// partition ends into wire nets
						Util.partition(circuit);


						// set circuit elements are now in
						for (Element el : circuit.getElements()) {
							el.setCircuit(circuit);
						}

						// indicate that circuit has changed
						markChanged();
						return true;
					} // end of paste method

					/**
					 * Reset highlights of elements in selected set, then clear selected set.
					 */
					private void clearSelected() {

						for (Element el : selected) {
							el.setHighlight(false);
						}
						selected.clear();
					} // end of clearSelected method

					/**
					 * Select all elements currently in the circuit, except attached wire ends.
					 */
					private void doSelectAll() {

						if (circuit.getElements().size() == 0)
							return;
						clearSelected();
						selRect = null;
						for (Element el : circuit.getElements()) {
							if (el instanceof WireEnd) {
								WireEnd end = (WireEnd)el;
								if (end.isAttached())
									continue;
							}
							selected.add(el);
							el.setHighlight(true);
							if (el instanceof Wire)
								continue;
							if (selRect == null) {
								selRect = new Rectangle(el.getRect());
							}
							else {
								selRect.add(el.getRect());
							}
						}
						if (selRect != null)
							setState(State.selected);
						repaint();
					} // end of doSelectAll method

					/**
					 * Change an element (if it can change).
					 */
					private void doModify() {

						// get the single item in the selected set
						Element el = (Element)(selected.toArray()[0]);

						// if it is a subcircuit...
						if (el instanceof SubCircuit) {

							// get circuit and set up editor for it
							SubCircuit sub = (SubCircuit)el;
							Circuit subcirc = sub.getSubCircuit();
							String tabName = sub.getName() + " in " + circuit.getName();
							for (int e=0; e<tabbedParent.getTabCount(); e+=1) {
								if (tabName.equals(tabbedParent.getTitleAt(e))) {
									TellUser.error(getTopLevelAncestor(),
											tabName + " is already being editted", "Error");
									clearSelected();
									setState(State.idle);
									repaint();
									return;
								}
							}
							subcirc.setImported(sub);
							Editor ed = new Editor(tabbedParent,subcirc,sub.getName(),clipboard);
							Dimension all = subcirc.getBounds().getSize();
							ed.setCircuitSize(all);
							subcirc.setEditor(ed);

							// set up import menu
							for (Component edit : tabbedParent.getComponents()) {
								if (!(edit instanceof Editor))
									continue;
								Editor otherEditor = (Editor)edit;
								if (!otherEditor.getCircuit().isImported())
									ed.addToImportMenu(otherEditor.getCircuit());
							}

							// add to tabbed pane
							tabbedParent.add(tabName,ed);
							tabbedParent.setSelectedComponent(ed);

							// disable this editor while subcircuit it being editted
							enabled = false;
							editable.setText("editting is disabled while a subcircuit is being modified");
						}

						// otherwise element will change itself
						else {
							boolean mustReplace = el.change(this.getGraphics(), this, x, y);

							// if size has changed, force user to reposition
							if (mustReplace) {
								clearSelected();
								selected.add(el);
								el.setHighlight(true);
								setState(State.placing);
								repaint();
								return;
							}
						}

						// finish up
						clearSelected();
						setState(State.idle);
						repaint();
					} // end of doModify method

					/**
					 * Put a probe on a wire.
					 */
					private void doProbe() {

						// get the single item in the selected set
						Wire wire = (Wire)(selected.toArray()[0]);

						// remove if wire has a probe, add if not
						if (wire.hasProbe()) {
							wire.removeProbe();
						}
						else {
							wire.attachProbe(null);
						}

						// clean up
						markChanged();
						clearSelected();
						setState(State.idle);
						repaint();
					} // end of doProbe method

					/**
					 * Change the propagation delay or access time of an element.
					 */
					private void doTiming() {

						// get the single item in the selected set
						Element el = (Element)(selected.toArray()[0]);

						// change its timing info
						el.changeTiming();

						// clean up
						clearSelected();
						setState(State.idle);
						repaint();
					} // end of doTiming method

					/**
					 * Remove any wire end that has degree 2 and is co-linear
					 * (horizontally or vertically) with opposite ends, or that
					 * has degree 1, not attached and is in the same place as its other end.
					 */
					private void removeCoLinear() {

						// get all editable wire ends
						Set<WireEnd> ends = new HashSet<WireEnd>();
						for (Element el : circuit.getElements()) {
							if (!(el instanceof WireEnd))
								continue;
							if (el.isUneditable())
								continue;
							ends.add((WireEnd)el);
						}

						// check each wire end
						for (WireEnd end : ends) {

							// check degree 1
							if (end.degree() == 1) {
								Object [] wires = end.getWires().toArray();
								Wire wire = (Wire)wires[0];
								WireEnd otherEnd = wire.getOtherEnd(end);
								if (end.isAttached() || !otherEnd.isAttached() || !end.intersects(otherEnd))
									continue;
								wire.remove(circuit);
							}

							// check degree 2
							else if (end.degree() == 2) {

								// must be co-linear
								boolean colinear = false;
								Object [] wires = end.getWires().toArray();
								Wire wire0 = (Wire)wires[0];
								Wire wire1 = (Wire)wires[1];
								int x0 = wire0.getOtherEnd(end).getX();
								int x1 = end.getX();
								int x2 = wire1.getOtherEnd(end).getX();
								if (x0 == x1 && x1 == x2) {
									colinear = true;
								}
								int y0 = wire0.getOtherEnd(end).getY();
								int y1 = end.getY();
								int y2 = wire1.getOtherEnd(end).getY();
								if (y0 == y1 && y1 == y2) {
									colinear = true;
								}
								if (!colinear)
									continue;

								// create new wire
								WireNet net = end.getNet();
								WireEnd end0 = wire0.getOtherEnd(end);
								WireEnd end1 = wire1.getOtherEnd(end);
								Wire newWire = new Wire(end0,end1);
								end0.addWire(newWire);
								end1.addWire(newWire);
								net.add(newWire);
								newWire.setNet(net);
								circuit.addElement(newWire);

								// delete wire end and old wires
								net.remove(end);
								net.remove(wire0);
								net.remove(wire1);
								end.remove(circuit);
							}
						}

					} // end of removeCoLinear method

					/**
					 * Set up new element if editor is enabled.
					 * Usually pops up dialog to enter characteristics.
					 * If not cancelled, then adds it to the circuit and gets ready
					 * to place it.
					 * 
					 * @param item The element to set up.
					 * @param fromToolBar True if toolbar button selected, false if from menu.
					 */
					private void setup(Element item, boolean fromToolBar) {

						// if disabled, do nothing
						if (!enabled)
							return;

						// if in the middle of an edit, do nothing
						if (currentState != State.idle) {
							return;
						}

						// can't put an input or output pin in an existing subcircuit
						if (circuit.isImported()) {
							if (item instanceof InputPin) {
								TellUser.error(JLSInfo.frame,
										"Can't add an input pin to a subcircuit", "Error");
								return;
							}
							else if (item instanceof OutputPin) {
								TellUser.error(JLSInfo.frame,
										"Can't add an output pin to a subcircuit", "Error");
								return;
							}
						}

						// clear selected if there is any, and turn off highlights
						for (Element el : selected) {
							el.setHighlight(false);
						}
						selected.clear();

						// decide where the element will drop: the last tracked
						// mouse position, or the center of the visible canvas
						// when invoked from the tool bar (event-local; #103)
						int dx = x;
						int dy = y;
						if (fromToolBar) {
							Point view = pane.getViewport().getViewPosition();
							dx = view.x + tabbedParent.getSize().width/2;
							dy = view.y + tabbedParent.getSize().height/4;
						}

						// if not cancelled...
						if (item.setup(this.getGraphics(),this,dx,dy)) {

							// put into circuit
							Point pos = getMousePosition();
							if (pos == null) {

								// add to circuit and selected set
								circuit.addElement(item);
								selected.add(item);
								item.setHighlight(true);

								// get ready to place it
								setState(State.chosen);
							}
							else {
								x = pos.x;
								y = pos.y;
								item.savePosition();

								// add to circuit and selected set
								circuit.addElement(item);
								selected.add(item);
								item.setHighlight(true);

								// place it
								setState(State.placing);
								repaint();
							}
						}
					} // end of setup method

					/**
					 * Import a copy of a subcircuit.
					 * 
					 * @param name The name of the subcircuit.
					 */
					public void doImport(String name) {

						// make a set of all elements except attached wire ends from subcircuit
						Circuit source = circMap.get(name);
						Set<Element> elements = new HashSet<Element>();
						for (Element el : source.getElements()) {
							if (el instanceof WireEnd) {
								WireEnd end = (WireEnd)el;
								if (end.isAttached())
									continue;
							}
							elements.add(el);
						}

						// copy elements to new circuit
						Circuit newCopy = new Circuit(name);
						Util.copy(elements,newCopy);
						Util.partition(newCopy);
						for (Element el : newCopy.getElements()) {
							el.setCircuit(newCopy);
						}

						// add jumpstarts to list
						updateJumpStarts(newCopy);

						finishImport(newCopy);

					} // end of doImport method

					/**
					 * Mark the editted circuit as changed, save a copy of
					 * the circuit for undo, and checkpoint file if it is time.
					 */
					public void markChanged() {

						// mark the circuit
						circuit.markChanged();

						// push a copy for undo
						pushCopy();

						// clear redos
						redos.clear();

						// save checkpoint file (if it is time)
						check += 1;
						if (check > JLSInfo.checkPointFreq) {
							check = 1;

							// get top level circuit
							Circuit circ = circuit;
							while (circ.isImported()) {
								circ = circ.getSubElement().getCircuit();
							}

							// Serialize in memory here (cheap relative to disk
							// I/O), then hand the text to the background checkpoint
							// writer so the write never stalls the event thread (#19).
							// checkpoint the top-level circuit, matching the file
							// it is written to: saving a subcircuit under the
							// top-level name would leave an unrecoverable checkpoint
							String fileName = circ.getDirectory() + "/" + circ.getName() + ".jls~";
							StringWriter text = new StringWriter();
							try (PrintWriter output = new PrintWriter(text)) {
								circ.save(output);
							}
							writeCheckpointInBackground(fileName, text.toString());
						}

					} // end of markChanged method

					/**
					 * Push a copy of the circuit being edited on the undo stack.
					 */
					public void pushCopy() {

						// snapshot the circuit in the save format (#18);
						// an aborted or no-op change serializes identically
						// to the top of the stack and is not pushed again
						CircuitSnapshot snap = CircuitSnapshot.capture(circuit);
						if (!undos.isEmpty() && undos.peek().sameAs(snap)) {
							return;
						}

						// see if undo stack is full
						if (undos.size() > JLSInfo.undoStackDepth) {

							// delete bottom of stack
							undos.remove(0);
						}

						// save for undo
						undos.push(snap);
					} // end of pushCopy method

					/**
					 * Do undo. An in-flight gesture is cancelled first,
					 * exactly as Esc would, so the restore always runs
					 * against an idle editor (issue #39: cancel-then-apply).
					 */
					public void undo() {

						cancelGesture();

						// no undo left if stack only has a copy of the original circuit
						if (undos.size() <= 1) {
							return;
						}

						// restore the previous snapshot first; only a
						// successful restore may touch the stacks
						CircuitSnapshot current = undos.get(undos.size() - 1);
						CircuitSnapshot previous = undos.get(undos.size() - 2);
						if (!finishDo(previous)) {
							return;
						}
						undos.pop();
						redos.push(current);
					} // end of undo method

					/**
					 * Do redo. Cancels any in-flight gesture first, like
					 * undo (issue #39).
					 */
					public void redo() {

						cancelGesture();

						// if nothing on the redo stack, then there is nothing to do
						if (redos.isEmpty()) {
							return;
						}

						// restore the snapshot first; only a successful
						// restore may touch the stacks
						CircuitSnapshot next = redos.peek();
						if (!finishDo(next)) {
							return;
						}
						redos.pop();
						undos.push(next);
					} // end of redo method

					/**
					 * Cancel any in-flight gesture, exactly as Esc or a
					 * right-click cancel would, without touching the
					 * undo/redo stacks (issue #39, audit finding H6). A
					 * gesture left in flight across an undo made the next
					 * mouse event operate on elements of the discarded
					 * circuit instance; a half-drawn wire could bridge an
					 * orphaned WireNet into the restored circuit.
					 */
					private void cancelGesture() {

						if (currentState == State.idle) {
							return;
						}

						// discard a partially drawn wire
						if (currentState == State.startwire
								|| currentState == State.drawire) {
							circuit.remove(wireEnd);
							if (wire != null) {
								circuit.remove(wire);
								wire.getOtherEnd(wireEnd).remove(wire,circuit);
							}
							net.remove(wireEnd);
							net.remove(wire);
							removeCoLinear();
							wireEnd = null;
							wire = null;
							prev = null;
						}

						// a drag returns to its origin
						else if (currentState == State.moving) {
							for (Element sel : selected) {
								sel.restorePosition();
							}
						}

						// elements not yet placed are removed
						else if (currentState == State.placing) {
							for (Element el : selected) {
								el.remove(circuit);
							}
						}

						// common cleanup back to the idle state
						selRect = null;
						clearSelected();
						untouchAll();
						setState(State.idle);
						repaint();
					} // end of cancelGesture method

					/**
					 * Finish up undo or redo: restore a snapshot and install
					 * it as the edited circuit (#18).
					 *
					 * @param snap The snapshot to restore.
					 *
					 * @return true if the snapshot was restored and
					 *         installed, false if it failed to load (the
					 *         current circuit is untouched).
					 */
					private boolean finishDo(CircuitSnapshot snap) {

						// rebuild the circuit through the ordinary load path
						Circuit newCopy = snap.restore(circuit.getName(), this.getGraphics());
						if (newCopy == null) {
							return false;
						}
						Editor ed = circuit.getEditor();
						newCopy.setDirectory(circuit.getDirectory());

						// link into subcircuit if it is imported
						SubCircuit sub = circuit.getSubElement();
						newCopy.setImported(sub);
						if (sub != null) {
							sub.setSubCircuit(newCopy);
							sub.remapPins(newCopy);
						}

						// make it be the current circuit
						circuit = newCopy;
						circuit.setEditor(ed);
						circuit.markChanged();

						// point sibling editors' import maps at the new
						// instance so Import copies restored content, not
						// the discarded circuit (issue #39, finding M8)
						if (tabbedParent != null) {
							for (Component tab : tabbedParent.getComponents()) {
								if (tab instanceof SimpleEditor
										&& tab != SimpleEditor.this) {
									((SimpleEditor)tab)
											.refreshInImportMenu(circuit);
								}
							}
						}

						// update jump start list
						updateJumpStarts(circuit);

						// update names used list
						updateNamesUsed(circuit);

						// if not imported, point simulator at it
						if (!circuit.isImported()) {
							JLSInfo.sim.setCircuit(circuit);
						}

						else {

							// propagate tri-state to outputs
							for (Element el : circuit.getElements()) {
								if (!(el instanceof OutputPin))
									continue;
								OutputPin pin = (OutputPin)el;
								SubCircuit subc = pin.getCircuit().getSubElement();
								Output put = (Output)subc.getPut(pin.getName());
								Input input = pin.getInput("input");
								if (!input.isAttached()) {
									put.setTriState(false);
								}
								else {
									put.setTriState(input.getWireEnd().isTriState());
								}
							}
						}
						return true;
					} // end of finishDo method

					/**
					 * Find all jump start elements and add names to the jumpstart list in this circuit.
					 * Do the same for all subcircuits.
					 * 
					 * @param circ The circuit to process.
					 */
					private void updateJumpStarts(Circuit circ) {

						for (Element el : circ.getElements()) {
							if (el instanceof JumpStart) {
								JumpStart j = (JumpStart)el;
								circ.addJumpStart(j.getName(),j);
							}
							else if (el instanceof SubCircuit) {
								SubCircuit sc = (SubCircuit)el;
								updateJumpStarts(sc.getSubCircuit());
							}
						}
					} // end of updateJumpStarts method

					/**
					 * Find all named elements and add names to the namesUsed list in this circuit.
					 * Do the same for all subcircuits.
					 * 
					 * @param circ The circuit to process.
					 */
					private void updateNamesUsed(Circuit circ) {

						for (Element el : circ.getElements()) {
							if (el.getName() != null) {
								String name = el.getName();
								circ.addName(name);
							}
							// not an else: subcircuits are named, so the
							// recursion never ran as an else-if (#51)
							if (el instanceof SubCircuit) {
								SubCircuit sc = (SubCircuit)el;
								updateNamesUsed(sc.getSubCircuit());
							}
						}
					} // end of updateNamesUsed method

				} // end of EditWindow class

			} // end of SimpleEditor class
