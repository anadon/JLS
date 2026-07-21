package jls.edit;



import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
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
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
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
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import jls.Circuit;
import jls.FileAbstractor;
import jls.JLSInfo;
import jls.MenuAcceleratorPolicy;
import jls.TellUser;
import jls.Util;
import jls.collab.op.AttachProbe;
import jls.collab.op.CircuitOp;
import jls.collab.op.FlipElement;
import jls.collab.op.MoveElements;
import jls.collab.op.OpRejected;
import jls.collab.op.OpSink;
import jls.collab.op.RemoveProbe;
import jls.collab.op.RotateElement;
import jls.collab.op.ToggleWatched;
import jls.elem.Adder;
import jls.elem.AndGate;
import jls.elem.Binder;
import jls.elem.Clock;
import jls.elem.Constant;
import jls.elem.Decoder;
import jls.elem.DelayGate;
import jls.elem.Element;
import jls.elem.ElementId;
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
	/** The circuit being edited. */
	protected Circuit circuit;
	/** The editor window. */
	protected EditWindow ew;
	/**
	 * False while a subcircuit is being edited. Volatile: written from
	 * the sim thread (enableEditor around a run), read by EDT mouse/key
	 * handlers (issue #49, finding H7).
	 */
	protected volatile boolean enabled = true;
	/** The tabbed pane the editor is in. */
	protected JTabbedPane tabbedParent;
	/** The scroll pane the editor is in. */
	private JScrollPane pane;
	/** Top panel; here so Editor class can display file menu. */
	protected JPanel top;
	/**
	 * Prominent full-width banner explaining why editing is blocked
	 * while a subcircuit tab is open (issue #86 H2). Hidden whenever
	 * this editor is enabled; it replaces the old one-line top-bar
	 * label, which was too easy to miss.
	 */
	private JLabel disabledBanner =
			new JLabel(" ",SwingConstants.CENTER);
	/** Editing status message display. */
	private JLabel message =
			new JLabel(" ");
	/** Element information display. */
	private JLabel info =
			new JLabel(" ",SwingConstants.CENTER);
	/** Holds cut/copied elements for cut and paste. */
	private Circuit clipboard;
	/** Displays importable circuits. */
	private JPopupMenu importMenu =
			new JPopupMenu();
	/** This editor, for use inside listeners and dialogs. */
	private SimpleEditor me;
	/** Undo/redo stack policy, extracted from the inline stacks (#84). */
	private final UndoManager undoManager =
			new UndoManager();
	/** Number of changes since the last checkpoint. */
	private int check = JLSInfo.checkPointFreq+1;
	/** Import-menu item for each importable circuit, by name. */
	private Map<String,JMenuItem> menuMap = new HashMap<String,JMenuItem>();
	/** Importable circuit for each import-menu item, by name. */
	private Map<String,Circuit> circMap = new HashMap<String,Circuit>();

	/**
	 * Checkpoint writing happens off the event thread (#19): the latest
	 * serialized circuit per checkpoint file waits here, and a single
	 * writer thread drains it. If edits outrun the disk, intermediate
	 * checkpoints are superseded before being written (coalescing).
	 */
	private static final ConcurrentHashMap<String,String> pendingCheckpoints =
			new ConcurrentHashMap<String,String>();
	/** The single background thread that drains pendingCheckpoints. */
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
	 * @jls.testedby jls.edit.CheckpointWriterTest#cancelSupersedesQueuedCheckpointAndDeletesFile()
	 * @jls.testedby jls.edit.CheckpointWriterTest#checkpointAfterCancelIsStillWritten()
	 * @jls.testedby jls.edit.CheckpointWriterTest#checkpointIsWrittenAndLoadable()
	 * @jls.testedby jls.edit.CheckpointWriterTest#newerCheckpointSupersedesQueuedOne()
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
	 * @jls.testedby jls.edit.CheckpointWriterTest#cancelSupersedesQueuedCheckpointAndDeletesFile()
	 * @jls.testedby jls.edit.CheckpointWriterTest#checkpointAfterCancelIsStillWritten()
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
	 * @jls.testedby jls.edit.TriStateBundleConnectTest#freshWireMayAttachToNormalBundle()
	 * @jls.testedby jls.edit.TriStateBundleConnectTest#freshWireMayAttachToTriStateBundle()
	 * @jls.testedby jls.edit.TriStateBundleConnectTest#nonGroupPutsNeverMix()
	 * @jls.testedby jls.edit.TriStateBundleConnectTest#normalDrivenWireIsStillRefusedOnTriStateBundle()
	 * @jls.testedby jls.edit.TriStateBundleConnectTest#normalWireMayAttachToNormalBundle()
	 * @jls.testedby jls.edit.TriStateBundleConnectTest#triStateWireIsStillRefusedOnNormalBundle()
	 * @jls.testedby jls.edit.TriStateBundleConnectTest#triStateWireMayAttachToTriStateBundle()
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
	 * @jls.testedby jls.edit.WireSweepSymmetryTest#clearWireCollidesInNeitherDirection()
	 * @jls.testedby jls.edit.WireSweepSymmetryTest#elementsMovingWithTheSelectionAreSkipped()
	 * @jls.testedby jls.edit.WireSweepSymmetryTest#landingOnAStationaryWireEndStillCollides()
	 * @jls.testedby jls.edit.WireSweepSymmetryTest#wireCrossingWireStaysLegal()
	 * @jls.testedby jls.edit.WireSweepSymmetryTest#wireHangingOffAnElementDoesNotCollideWithIt()
	 * @jls.testedby jls.edit.WireSweepSymmetryTest#wireSweepingAcrossElementCollidesLikeTheReverseDrag()
	 */
	static boolean wireCollidesAlongSpan(Circuit circuit,
			Set<Element> selected, Element sel, Wire wire) {

		Rectangle span = wire.getIndexBounds();
		span.grow(JLSInfo.pointDiameter,JLSInfo.pointDiameter);
		for (Element elm : circuit.elementsNear(span)) {
			if (sel == elm)
				continue;
			if (elm instanceof WireEnd wend) {
				if (wire.touches(wend)) {
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
		top.add(message,BorderLayout.EAST);
		message.setOpaque(true);
		message.setBackground(Color.cyan);
		top.add(info,BorderLayout.CENTER);

		// full-width warning banner above the status bar, shown only
		// while an open subcircuit tab has this editor disabled
		// (issue #86 H2)
		disabledBanner.setName("subcircuitDisabledBanner");
		disabledBanner.setOpaque(true);
		disabledBanner.setBackground(new Color(255,204,0));
		disabledBanner.setForeground(Color.BLACK);
		disabledBanner.setFont(disabledBanner.getFont().deriveFont(Font.BOLD));
		disabledBanner.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
		disabledBanner.setVisible(false);
		JPanel north = new JPanel(new BorderLayout());
		north.add(disabledBanner,BorderLayout.NORTH);
		north.add(top,BorderLayout.CENTER);
		all.add(north,BorderLayout.NORTH);
		ew = new EditWindow();
		// preferred size follows the model canvas size scaled by the current
		// zoom (issue #74); the EditWindow computes it from its own model
		// bounds and Viewport
		ew.applyPreferredSize();
		pane = new JScrollPane(ew);
		pane.getHorizontalScrollBar().setUnitIncrement(10);
		pane.getVerticalScrollBar().setUnitIncrement(10);
		all.add(pane,BorderLayout.CENTER);
		JPanel elements = ew.getToolBar();
		add(elements,BorderLayout.NORTH);
		add(all);

		// save reference to myself
		me = this;

		// The old "increase drawing area by 10%" corner button and its Global
		// menu twin are retired (issue #74): the canvas now auto-grows as
		// elements approach its edge, and zoom (View menu, Ctrl +/-/0, and
		// Ctrl/Cmd+wheel) replaces the single fixed magnification.

	} // end of constructor

	/**
	 * Get reference to top so applet can put menu in.
	 *
	 * @return the top panel.
	 */
	public JPanel getTop() {

		return top;
	} // end of getTop method

	/**
	 * Get the circuit being editted by this editor.
	 * 
	 * @return the circuit.
	 * @jls.testedby jls.ui.EditorGestureSupport#currentCircuit()
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

		ew.setModelSize(size);
	} // end of setCircuitSize method

	/**
	 * Change editor window background color.
	 */
	public void changeBackgroundColor() {

		ew.setBackground(JLSInfo.backgroundColor);
	} // end of changeBackgroundColor method

	/**
	 * Give keyboard focus to the editing canvas. Called when this
	 * editor's tab is selected so canvas shortcuts work immediately;
	 * together with the click-to-focus behavior in the canvas itself
	 * this replaces the old focus-follows-mouse model (issue #75).
	 */
	public void focusOnCanvas() {

		ew.requestFocusInWindow();
	} // end of focusOnCanvas method

	/**
	 * The single shared {@link Action} this editor uses for the given
	 * editing operation (issue #75). The popup menus, the canvas key
	 * bindings, and the main window's Edit and Element menus all dispatch
	 * through this one object, so the three surfaces are not separate
	 * copies of the operation. The action is always enabled and guards
	 * its own preconditions (selection size, editability), so invoking it
	 * with nothing selected is a harmless no-op. The main menu bar
	 * retargets its items to the currently selected editor's actions on
	 * every tab change.
	 *
	 * @param op the editing operation.
	 * @return the shared action for that operation.
	 */
	public Action editAction(EditOp op) {

		return ew.editAction(op);
	} // end of editAction method

	/**
	 * The current keyboard construction caret position (issue #75), in
	 * model coordinates, or null if the keyboard has not yet been used to
	 * point. The caret is the keyboard counterpart of the mouse pointer: a
	 * grid-snapped marker arrow keys move and W starts a wire at. Exposed
	 * so a keyboard-driven construction test can read where the next
	 * placement or wire endpoint will land between key presses.
	 *
	 * @return a copy of the caret point, or null when the caret is unset.
	 */
	public Point keyboardCaret() {

		return ew.keyboardCaret();
	} // end of keyboardCaret method

	/**
	 * Zoom in one keyboard ladder stop, centered on the visible canvas
	 * (issue #74). Bound to the View menu's Zoom In item and Ctrl/Cmd+=.
	 */
	public void zoomIn() {

		ew.zoomInCentered();
	} // end of zoomIn method

	/**
	 * Zoom out one keyboard ladder stop, centered on the visible canvas
	 * (issue #74). Bound to the View menu's Zoom Out item and Ctrl/Cmd+-.
	 */
	public void zoomOut() {

		ew.zoomOutCentered();
	} // end of zoomOut method

	/**
	 * Reset the zoom to exactly 100%, keeping the visible-canvas center
	 * fixed (issue #74). Bound to the View menu's Actual Size item and
	 * Ctrl/Cmd+0.
	 */
	public void zoomReset() {

		ew.zoomResetCentered();
	} // end of zoomReset method

	/**
	 * Fit the whole circuit into the visible canvas and center it (issue
	 * #74) - the adjudicated "reset the view" affordance. Bound to the
	 * View menu's Fit item and Ctrl/Cmd+9.
	 */
	public void zoomToFit() {

		ew.zoomFit();
	} // end of zoomToFit method

	/**
	 * The current zoom scale of the editing canvas (1.0 = 100%), for
	 * tests and status display (issue #74).
	 *
	 * @return the zoom scale.
	 */
	public double getZoomScale() {

		return ew.getViewportScale();
	} // end of getZoomScale method

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
	 * Disable this editor because the named subcircuit is being edited
	 * in its own tab, and show a prominent banner saying so (issue #86
	 * H2 - the old one-line status label was too easy to miss).
	 * {@link jls.edit.Editor#enableEdits()} re-enables editing and
	 * hides the banner when the subcircuit tab closes.
	 *
	 * @param subcircuitName The name of the subcircuit whose open tab
	 *        blocks editing here.
	 */
	public void disableForSubcircuit(String subcircuitName) {

		enabled = false;
		disabledBanner.setText("Editing of \"" + circuit.getName()
				+ "\" is disabled while subcircuit \"" + subcircuitName
				+ "\" is open in its own tab - close that tab to resume editing here.");
		disabledBanner.setVisible(true);
		revalidate();
		repaint();
	} // end of disableForSubcircuit method

	/**
	 * Hide the banner shown by {@link #disableForSubcircuit(String)}.
	 * Called when the subcircuit tab closes and this editor is
	 * re-enabled.
	 */
	protected void hideDisabledBanner() {

		disabledBanner.setText(" ");
		disabledBanner.setVisible(false);
		revalidate();
		repaint();
	} // end of hideDisabledBanner method

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
	enum State {
		/** No gesture in progress. */
		idle,
		/** A new element has been chosen from the toolbar. */
		chosen,
		/** A new element is following the cursor to its place. */
		placing,
		/** Selected elements are being dragged. */
		moving,
		/** A selection rectangle is being dragged out. */
		selecting,
		/** A selection exists and awaits a command. */
		selected,
		/** The option popup menu is showing. */
		option,
		/** Wire drawing is armed, waiting for the first end. */
		startwire,
		/** A wire is being drawn from its initial end. */
		drawire
	};

	/**
	 * Result of starting the wire-drawing gesture: the initial wire end,
	 * and whether a selection had to be cleared to start it. The flag is
	 * captured before the clear — keyed off the selection afterwards it
	 * would always be true, because the new wire end becomes the
	 * selection (the pitfall AmityWilder/JLS@b1f1573 fixed).
	 *
	 * @param end The new initial wire end.
	 * @param hadSelection Whether a selection existed before it was cleared.
	 */
	record WireStart(WireEnd end, boolean hadSelection) {} // end of WireStart record

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
	 * @jls.testedby jls.edit.CtrlWGestureTest#overlapFeedbackKeyedOffPreClearSelection()
	 * @jls.testedby jls.edit.CtrlWGestureTest#startWireClearsSelectionAndSelectsNewEnd()
	 * @jls.testedby jls.edit.CtrlWGestureTest#startWireFromEmptySelectionMatchesOldBehavior()
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
		private class EditWindow extends JPanel implements ActionListener,MouseListener,MouseMotionListener,MouseWheelListener {

			/**
			 * The view transform (issue #74): owns the zoom scale between
			 * model coordinates (grid-snapped integers, saved to files, used
			 * by hit-testing) and this component's coordinates. Panning is
			 * handled by the enclosing {@link JScrollPane} (plain wheel and
			 * space/middle drag move the scroll position), so this viewport's
			 * translation stays zero and its scale is the single view factor:
			 * component = model * scale, and model = component / scale, which
			 * is the one inversion every mouse-event entry point performs.
			 */
			private final Viewport viewport = new Viewport();

			/**
			 * The logical size of the drawing area in model units. The
			 * component's preferred size is this scaled by the current zoom.
			 * It auto-grows (never shrinks) as elements approach an edge,
			 * replacing the retired manual 10% grow button (issue #74).
			 */
			private Dimension modelSize =
					new Dimension(JLSInfo.circuitsize,JLSInfo.circuitsize);

			/** True while a pan gesture (space-drag or middle-drag) is active. */
			private boolean panning = false;
			/** True while the space bar is held, arming space-drag panning. */
			private boolean spaceDown = false;
			/** Absolute screen x where the current pan started. */
			private int panAnchorX;
			/** Absolute screen y where the current pan started. */
			private int panAnchorY;
			/** Scroll view position when the current pan started. */
			private Point panStartView;

			// popup menus
			/** Right-click menu over an existing element or wire. */
			private JPopupMenu optionMenu = new JPopupMenu();
			/** Probe menu item; gets its title when added to the menu. */
			private JMenuItem probe = new JMenuItem("");
			/** Watch menu item; gets its title when added to the menu. */
			private JMenuItem watch = new JMenuItem("");
			/** Menu item to view/modify element details. */
			private JMenuItem modify = new JMenuItem("Modify");
			/** Menu item to change propagation delay or access time. */
			private JMenuItem timing = new JMenuItem("Change Timing");
			/** Menu item to view the current simulated value. */
			private JMenuItem view = new JMenuItem("View Contents");
			/** Menu item to undo the latest change. */
			private JMenuItem undo = new JMenuItem("Undo");
			/** Menu item to redo the latest undone change. */
			private JMenuItem redo = new JMenuItem("Redo");
			/** Menu item to cut the selection to the clipboard. */
			private JMenuItem cut = new JMenuItem("Cut");
			/** Menu item to copy the selection to the clipboard. */
			private JMenuItem copy = new JMenuItem("Copy");
			/** Menu item to delete the selection. */
			private JMenuItem delete = new JMenuItem("Delete");
			/** Menu item to make the selection uneditable. */
			private JMenuItem lock = new JMenuItem("Lock");

			/** Right-click menu over empty space. */
			private JPopupMenu newMenu = new JPopupMenu();
			/** Menu item to create a new wire. */
			private JMenuItem connect = new JMenuItem("Wire(s)");
			/** Menu item to paste the clipboard contents. */
			private JMenuItem paste = new JMenuItem("Paste");
			/** Menu item to select all elements. */
			private JMenuItem selAll = new JMenuItem("Select All");
			/** Menu item to close the popup menu. */
			private JMenuItem close = new JMenuItem("Close");

			/** Menu item to rotate the selection clockwise. */
			private JMenuItem Crotate = new JMenuItem("Rotate Clockwise");
			/** Menu item to rotate the selection counter-clockwise. */
			private JMenuItem CCrotate = new JMenuItem("Rotate Counter-Clockwise");
			/** Menu item to flip the selection. */
			private JMenuItem flip = new JMenuItem("Flip");
			/** Menu item to create the wire end matching a jump start. */
			private JMenuItem matchJump = new JMenuItem("Create Matching End");

			/**
			 * The one shared {@link Action} per editing operation
			 * (issue #75). Built once in the constructor and reused by the
			 * popup menu items, the canvas key bindings, and the main
			 * window's Edit and Element menus, so those surfaces dispatch
			 * through a single object instead of three separate copies.
			 */
			private final EnumMap<EditOp,Action> editActions =
					new EnumMap<EditOp,Action>(EditOp.class);

			/** The element toolbar shown above the edit window. */
			private JPanel toolbar;
			/** The element menu backing the toolbar. */
			private JMenu elements;

			// properties
			/** The editor's interaction mode. */
			private State currentState = State.idle;
			/** True until the first paint, which pushes the undo base copy. */
			private boolean firstDraw = true;
			/** Latest cursor x coordinate, in model units (issue #74). */
			private int x;
			/** Latest cursor y coordinate, in model units (issue #74). */
			private int y;
			/**
			 * Latest cursor position in this component's own coordinates
			 * (issue #74). Popup menus and value dialogs are positioned in
			 * component space, so they use these rather than the model-space
			 * {@link #x}/{@link #y} the hit-testing uses.
			 */
			private int sx;
			/** Latest cursor y coordinate, in component units (issue #74). */
			private int sy;
			/** Selection rectangle, or null when none is being dragged. */
			private Rectangle selRect = null;
			/** Currently selected elements. */
			private Set<Element>selected =
					new HashSet<Element>();
			/** The end of the wire being drawn. */
			private WireEnd wireEnd;
			/** The wire being drawn. */
			private Wire wire;
			/** The net the wire being drawn belongs to. */
			private WireNet net;
			/** The previous wire end while drawing a multi-segment wire. */
			private WireEnd prev = null;
			/** Elements to add during a connect. */
			private Set<Element>adds =
					new HashSet<Element>();
			/** Elements to remove during a connect. */
			private Set<Element>subs =
					new HashSet<Element>();
			/** Why the current placement overlaps, for the status line. */
			private String overlapMessage = "";
			/** Elements marked touching by overlap/connect. */
			private Set<Element>touchedElements =
					new HashSet<Element>();
			/** Puts marked touching by overlap/connect. */
			private Set<Put>touchedPuts =
					new HashSet<Put>();
			/**
			 * The keyboard construction caret (issue #75), in model
			 * coordinates and always grid-snapped, or null when the
			 * keyboard has not been used to point yet. Arrow keys move it
			 * while idle; W starts a wire at it; it is the keyboard
			 * counterpart of the mouse pointer, so a user with no pointing
			 * device can position a drop point or a wire endpoint. It
			 * tracks the following element or wire end while placing or
			 * drawing so it always shows where the next commit lands.
			 */
			private Point caret = null;

			/**
			 * Create a new edit window.
			 */
			public EditWindow() {

				// set up GUI
				setBackground(JLSInfo.backgroundColor);

				// the canvas holds keyboard focus like any other component:
				// it is focusable, takes focus on click (mousePressed) and
				// on tab selection (focusOnCanvas), and never grabs focus
				// on hover (issue #75 removed the mouseEntered grab that
				// made shortcuts die when the pointer left the canvas)
				setFocusable(true);

				// add listeners for mouse activities
				addMouseListener(this);
				addMouseMotionListener(this);
				// wheel = scroll, Ctrl/Cmd+wheel = zoom-at-cursor (issue #74)
				addMouseWheelListener(this);

				// build the one shared Action per editing operation (#75)
				// before wiring any surface to them
				initEditActions();

				// set up popup menus: every item dispatches through the
				// shared Action, which carries its label, accelerator,
				// tooltip, and the single implementation - so the popups,
				// the canvas key bindings, and the menu bar Edit/Element
				// menus are no longer three separate copies of each op
				probe.setAction(editAction(EditOp.PROBE));
				watch.setAction(editAction(EditOp.WATCH));
				modify.setAction(editAction(EditOp.MODIFY));
				timing.setAction(editAction(EditOp.TIMING));
				view.setAction(editAction(EditOp.VIEW_VALUE));
				cut.setAction(editAction(EditOp.CUT));
				copy.setAction(editAction(EditOp.COPY));
				delete.setAction(editAction(EditOp.DELETE));
				lock.setAction(editAction(EditOp.LOCK));

				// rotate/flip show the plain-key canvas bindings (#75)
				Crotate.setAction(editAction(EditOp.ROTATE_CW));
				CCrotate.setAction(editAction(EditOp.ROTATE_CCW));
				flip.setAction(editAction(EditOp.FLIP));

				// matchJump is contextual (JumpStart only) and popup-only,
				// so it stays on the ActionListener rather than becoming a
				// shared Action
				matchJump.addActionListener(this);

				// the connect item now shows plain W: wire-start moved off
				// the Ctrl/Cmd+W it used to share with Watch, resolving the
				// overload that put the same stroke on two popup items (#75)
				connect.setAction(editAction(EditOp.WIRE_START));
				newMenu.add(connect);

				paste.setAction(editAction(EditOp.PASTE));
				newMenu.add(paste);

				selAll.setAction(editAction(EditOp.SELECT_ALL));
				newMenu.add(selAll);

				close.setAction(editAction(EditOp.CLOSE));
				newMenu.add(close);

				undo.setAction(editAction(EditOp.UNDO));
				newMenu.add(undo);

				redo.setAction(editAction(EditOp.REDO));
				newMenu.add(redo);

				makeElements();
				newMenu.add(elements);

				// canvas key bindings: menu-mask+W now toggles Watch only
				// and wire-start is the dedicated plain-W binding, so the
				// two functions no longer share one stroke (#75). Both
				// point at the same shared Actions the popups and menu bar
				// use.
				int menuMask =
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_W,menuMask),"watch");
				getActionMap().put("watch", editAction(EditOp.WATCH));
				getInputMap().put(MenuAcceleratorPolicy.wireStartStroke(),"wire start");
				getActionMap().put("wire start", editAction(EditOp.WIRE_START));

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

				// the remaining edit-op key bindings all dispatch through the
				// shared Actions (#75): the InputMap names them and the
				// ActionMap points each at editAction(op), the same object the
				// popup items and the menu-bar Edit/Element menus use
				getInputMap().put(MenuAcceleratorPolicy.viewValueStroke(),"view");
				getActionMap().put("view", editAction(EditOp.VIEW_VALUE));
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_M,menuMask),"modify");
				getActionMap().put("modify", editAction(EditOp.MODIFY));
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_P,menuMask),"probe");
				getActionMap().put("probe", editAction(EditOp.PROBE));
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_T,menuMask),"timing");
				getActionMap().put("timing", editAction(EditOp.TIMING));
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A,menuMask),"select all");
				getActionMap().put("select all", editAction(EditOp.SELECT_ALL));
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_E,menuMask),"close window");
				getActionMap().put("close window", editAction(EditOp.CLOSE));
				for (KeyStroke stroke : DeleteKeyPolicy.canvasBindings())
					getInputMap().put(stroke,"do delete");
				getActionMap().put("do delete", editAction(EditOp.DELETE));
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X,menuMask),"do cut");
				getActionMap().put("do cut", editAction(EditOp.CUT));
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C,menuMask),"do copy");
				getActionMap().put("do copy", editAction(EditOp.COPY));
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V,menuMask),"do paste");
				getActionMap().put("do paste", editAction(EditOp.PASTE));
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,menuMask),"do undo");
				getActionMap().put("do undo", editAction(EditOp.UNDO));
				// every redo stroke the policy defines: the platform accelerator
				// plus, on macOS, the historical Cmd+Y alias (#75)
				for (KeyStroke stroke : MenuAcceleratorPolicy.redoBindings(System.getProperty("os.name")))
					getInputMap().put(stroke,"do redo");
				getActionMap().put("do redo", editAction(EditOp.REDO));
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_L,menuMask),"do lock");
				getActionMap().put("do lock", editAction(EditOp.LOCK));
				getInputMap().put(MenuAcceleratorPolicy.rotateCwStroke(),"rotate cw");
				getActionMap().put("rotate cw", editAction(EditOp.ROTATE_CW));
				getInputMap().put(MenuAcceleratorPolicy.rotateCcwStroke(),"rotate ccw");
				getActionMap().put("rotate ccw", editAction(EditOp.ROTATE_CCW));
				getInputMap().put(MenuAcceleratorPolicy.flipStroke(),"do flip");
				getActionMap().put("do flip", editAction(EditOp.FLIP));

				// keyboard-only construction bindings (issue #75, H2):
				// arrow keys move the grid caret, the following element, the
				// wire end, or the selection depending on state; Enter
				// commits (places, wires, or selects). All WHEN_FOCUSED like
				// the other canvas bindings, so a keyboard user builds a
				// circuit with no pointing device
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,0),"kbd up");
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,0),"kbd down");
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,0),"kbd left");
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,0),"kbd right");
				getActionMap().put("kbd up", nudgeAction(KeyboardConstructionPolicy.Nudge.UP));
				getActionMap().put("kbd down", nudgeAction(KeyboardConstructionPolicy.Nudge.DOWN));
				getActionMap().put("kbd left", nudgeAction(KeyboardConstructionPolicy.Nudge.LEFT));
				getActionMap().put("kbd right", nudgeAction(KeyboardConstructionPolicy.Nudge.RIGHT));
				getInputMap().put(KeyboardConstructionPolicy.commitStroke(),"kbd commit");
				Action kbdCommit = new AbstractAction() {
					/**
					 * Handle the keyboard commit (Enter) key.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						keyboardCommit();
					}
				};
				getActionMap().put("kbd commit", kbdCommit);

				// set up zoom key bindings (issue #74): Ctrl/Cmd += in,
				// Ctrl/Cmd +- out, Ctrl/Cmd +0 actual size, Ctrl/Cmd +9 fit.
				// All zoom about the center of the visible canvas and share
				// the code path with the View menu items.
				int zoomMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
				Action zoomInKey = new AbstractAction() {
					/**
					 * Handle the zoom-in shortcut.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						zoomInCentered();
					}
				};
				// accept both the main-row '=' and the numpad '+' for zoom in
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,zoomMask),"zoom in");
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS,zoomMask),"zoom in");
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD,zoomMask),"zoom in");
				getActionMap().put("zoom in", zoomInKey);
				Action zoomOutKey = new AbstractAction() {
					/**
					 * Handle the zoom-out shortcut.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						zoomOutCentered();
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,zoomMask),"zoom out");
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT,zoomMask),"zoom out");
				getActionMap().put("zoom out", zoomOutKey);
				Action zoomResetKey = new AbstractAction() {
					/**
					 * Handle the actual-size shortcut.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						zoomResetCentered();
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_0,zoomMask),"zoom reset");
				getActionMap().put("zoom reset", zoomResetKey);
				Action zoomFitKey = new AbstractAction() {
					/**
					 * Handle the fit-to-circuit shortcut.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						zoomFit();
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_9,zoomMask),"zoom fit");
				getActionMap().put("zoom fit", zoomFitKey);

				// space bar arms space-drag panning while held (issue #74)
				Action spacePressed = new AbstractAction() {
					/**
					 * Arm space-drag panning on space press.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						spaceDown = true;
					}
				};
				Action spaceReleased = new AbstractAction() {
					/**
					 * Disarm space-drag panning on space release.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						spaceDown = false;
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,0,false),"pan on");
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,0,true),"pan off");
				getActionMap().put("pan on", spacePressed);
				getActionMap().put("pan off", spaceReleased);

			} // end of constructor

			/**
			 * The shared {@link Action} for one editing operation (#75).
			 * The popup items, the key bindings, and the menu-bar Edit and
			 * Element menus all dispatch through the object this returns.
			 *
			 * @param op the editing operation.
			 * @return the one shared action for that operation.
			 */
			Action editAction(EditOp op) {

				return editActions.get(op);
			} // end of editAction method

			/**
			 * The current keyboard construction caret position (issue #75),
			 * copied so callers cannot mutate it, or null when unset.
			 *
			 * @return a copy of the caret point, or null.
			 */
			Point keyboardCaret() {

				return caret == null ? null : new Point(caret);
			} // end of keyboardCaret method

			/**
			 * Store a shared editing Action under its operation, tagging it
			 * with the label, accelerator, and tooltip its menu items and
			 * bindings display (#75). The action is always enabled and
			 * guards its own preconditions, so any surface may invoke it
			 * safely.
			 *
			 * @param op the operation the action performs.
			 * @param accel the accelerator keystroke, or null for none.
			 * @param tip the tooltip/short description.
			 * @param action the action implementation.
			 */
			private void register(EditOp op, KeyStroke accel, String tip,
				Action action) {

				action.putValue(Action.NAME, op.label());
				if (accel != null)
					action.putValue(Action.ACCELERATOR_KEY, accel);
				action.putValue(Action.SHORT_DESCRIPTION, tip);
				editActions.put(op, action);
			} // end of register method

			/**
			 * Build the one shared Action per editing operation (#75). Each
			 * body is the single implementation for its operation - lifted
			 * from what used to be a popup switch arm and a duplicate
			 * key-binding action - and carries every guard (editor enabled,
			 * selection size, element type) so it is a harmless no-op when
			 * invoked from the menu bar with nothing appropriate selected.
			 */
			private void initEditActions() {

				int menuMask =
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
				String osName = System.getProperty("os.name");

				register(EditOp.PROBE, KeyStroke.getKeyStroke(KeyEvent.VK_P,menuMask),
					"watch activity on this wire during simulation",
					new AbstractAction() {
						/**
						 * Perform the PROBE operation.
						 *
						 * @param event The triggering action event.
						 */
						@Override
						public void actionPerformed(ActionEvent event) {
							if (!enabled)
								return;
							if (selected.size() != 1)
								return;
							Element el = (Element)selected.toArray()[0];
							if (!(el instanceof Wire))
								return;
							doProbe();
						}
					});

				register(EditOp.WATCH, KeyStroke.getKeyStroke(KeyEvent.VK_W,menuMask),
					"watch activity on this element during simulation",
					new AbstractAction() {
						/**
						 * Perform the WATCH operation.
						 *
						 * @param event The triggering action event.
						 */
						@Override
						public void actionPerformed(ActionEvent event) {
							if (!enabled)
								return;
							if (selected.size() != 1)
								return;
							Element el = (Element)selected.toArray()[0];
							if (el.isUneditable())
								return;
							if (!el.canWatch())
								return;
							submitOp(new ToggleWatched(el.getStableId()));
							clearSelected();
							setState(State.idle);
							repaint();
						}
					});

				register(EditOp.MODIFY, KeyStroke.getKeyStroke(KeyEvent.VK_M,menuMask),
					"view/modify element details",
					new AbstractAction() {
						/**
						 * Perform the MODIFY operation.
						 *
						 * @param event The triggering action event.
						 */
						@Override
						public void actionPerformed(ActionEvent event) {
							if (!enabled)
								return;
							if (selected.size() != 1)
								return;
							doModify();
						}
					});

				register(EditOp.TIMING, KeyStroke.getKeyStroke(KeyEvent.VK_T,menuMask),
					"change propagation delay or access time",
					new AbstractAction() {
						/**
						 * Perform the TIMING operation.
						 *
						 * @param event The triggering action event.
						 */
						@Override
						public void actionPerformed(ActionEvent event) {
							if (!enabled)
								return;
							if (selected.size() != 1)
								return;
							Element el = (Element)selected.toArray()[0];
							if (!el.hasTiming())
								return;
							doTiming();
						}
					});

				register(EditOp.VIEW_VALUE, MenuAcceleratorPolicy.viewValueStroke(),
					"view current simulated value",
					new AbstractAction() {
						/**
						 * Perform the VIEW_VALUE operation.
						 *
						 * @param event The triggering action event.
						 */
						@Override
						public void actionPerformed(ActionEvent event) {
							if (!enabled)
								return;
							if (selected.size() != 1)
								return;
							Element el = (Element)selected.toArray()[0];
							el.showCurrentValue(new Point(x,y));
							clearSelected();
							setState(State.idle);
							repaint();
						}
					});

				register(EditOp.CUT, KeyStroke.getKeyStroke(KeyEvent.VK_X,menuMask),
					"cut all selected elements to clipboard",
					new AbstractAction() {
						/**
						 * Perform the CUT operation.
						 *
						 * @param event The triggering action event.
						 */
						@Override
						public void actionPerformed(ActionEvent event) {
							if (!enabled)
								return;
							copy();
							remove();
							removeCoLinear();
							clearSelected();
							setState(State.idle);
							repaint();
						}
					});

				register(EditOp.COPY, KeyStroke.getKeyStroke(KeyEvent.VK_C,menuMask),
					"copy all selected elements to clipboard",
					new AbstractAction() {
						/**
						 * Perform the COPY operation.
						 *
						 * @param event The triggering action event.
						 */
						@Override
						public void actionPerformed(ActionEvent event) {
							if (!enabled)
								return;
							copy();
							clearSelected();
							setState(State.idle);
							repaint();
						}
					});

				register(EditOp.PASTE, KeyStroke.getKeyStroke(KeyEvent.VK_V,menuMask),
					"paste contents of clipboard",
					new AbstractAction() {
						/**
						 * Perform the PASTE operation.
						 *
						 * @param event The triggering action event.
						 */
						@Override
						public void actionPerformed(ActionEvent event) {
							if (!enabled)
								return;
							if (clipboard.getElements().size() == 0)
								return;
							if (paste(clipboard))
								setState(State.placing);
							repaint();
						}
					});

				register(EditOp.DELETE, DeleteKeyPolicy.menuAccelerator(osName),
					"delete all selected elements",
					new AbstractAction() {
						/**
						 * Perform the DELETE operation.
						 *
						 * @param event The triggering action event.
						 */
						@Override
						public void actionPerformed(ActionEvent event) {
							if (!enabled)
								return;
							remove();
							removeCoLinear();
							clearSelected();
							setState(State.idle);
							repaint();
						}
					});

				register(EditOp.LOCK, KeyStroke.getKeyStroke(KeyEvent.VK_L,menuMask),
					"make selected elements uneditable (cannot be undone)",
					new AbstractAction() {
						/**
						 * Perform the LOCK operation.
						 *
						 * @param event The triggering action event.
						 */
						@Override
						public void actionPerformed(ActionEvent event) {
							if (!enabled)
								return;
							boolean opt = TellUser.confirm(null,
									"Making elements uneditable cannot be undone.  Are you sure you want to do this?",
									"WARNING");
							if (opt) {
								for (Element el : selected) {
									el.makeUneditable();
								}
							}
							clearSelected();
							setState(State.idle);
							repaint();
						}
					});

				register(EditOp.SELECT_ALL, KeyStroke.getKeyStroke(KeyEvent.VK_A,menuMask),
					"select all elements",
					new AbstractAction() {
						/**
						 * Perform the SELECT_ALL operation.
						 *
						 * @param event The triggering action event.
						 */
						@Override
						public void actionPerformed(ActionEvent event) {
							if (!enabled)
								return;
							doSelectAll();
						}
					});

				register(EditOp.UNDO, KeyStroke.getKeyStroke(KeyEvent.VK_Z,menuMask),
					"undo last modification",
					new AbstractAction() {
						/**
						 * Perform the UNDO operation.
						 *
						 * @param event The triggering action event.
						 */
						@Override
						public void actionPerformed(ActionEvent event) {
							if (!enabled)
								return;
							undo();
							repaint();
						}
					});

				register(EditOp.REDO, MenuAcceleratorPolicy.redoDisplayed(osName),
					"redo last undo",
					new AbstractAction() {
						/**
						 * Perform the REDO operation.
						 *
						 * @param event The triggering action event.
						 */
						@Override
						public void actionPerformed(ActionEvent event) {
							if (!enabled)
								return;
							redo();
							repaint();
						}
					});

				register(EditOp.ROTATE_CW, MenuAcceleratorPolicy.rotateCwStroke(),
					"rotate the selected element clockwise",
					new AbstractAction() {
						/**
						 * Perform the ROTATE_CW operation.
						 *
						 * @param event The triggering action event.
						 */
						@Override
						public void actionPerformed(ActionEvent event) {
							rotateSelected(true);
						}
					});

				register(EditOp.ROTATE_CCW, MenuAcceleratorPolicy.rotateCcwStroke(),
					"rotate the selected element counter-clockwise",
					new AbstractAction() {
						/**
						 * Perform the ROTATE_CCW operation.
						 *
						 * @param event The triggering action event.
						 */
						@Override
						public void actionPerformed(ActionEvent event) {
							rotateSelected(false);
						}
					});

				register(EditOp.FLIP, MenuAcceleratorPolicy.flipStroke(),
					"flip the selected element",
					new AbstractAction() {
						/**
						 * Perform the FLIP operation.
						 *
						 * @param event The triggering action event.
						 */
						@Override
						public void actionPerformed(ActionEvent event) {
							flipSelected();
						}
					});

				register(EditOp.WIRE_START, MenuAcceleratorPolicy.wireStartStroke(),
					"create a new wire",
					new AbstractAction() {
						/**
						 * Perform the WIRE_START operation.
						 *
						 * @param event The triggering action event.
						 */
						@Override
						public void actionPerformed(ActionEvent event) {
							if (!enabled)
								return;
							// start the wire at the pointer if it is over the canvas,
							// otherwise at the last tracked model position (e.g. the
							// spot a right-click "Wire(s)" was invoked from)
							// keyboard-first: start at the construction caret
							// when the keyboard has positioned it (issue #75).
							// Otherwise start at the pointer if it is over the
							// canvas, else at the last tracked model position
							if (caret != null) {
								x = caret.x;
								y = caret.y;
							}
							else {
								Point p = getMousePosition();
								if (p != null) {
									p = viewport.toModel(p);
									x = p.x;
									y = p.y;
								}
							}
							autoGrow(x,y);
							setState(State.startwire);
							WireStart start = startWireGesture(circuit,selected,x,y);
							wireEnd = start.end();
							wire = null;
							net = wireEnd.getNet();
							// keep the caret on the wire end so arrows extend it
							caret = new Point(x,y);
							// a cleared selection was under the cursor, so report the
							// (likely) overlap the same way wire dragging does
							if (start.hadSelection()) {
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
					});

				register(EditOp.CLOSE, KeyStroke.getKeyStroke(KeyEvent.VK_E,menuMask),
					"close this circuit",
					new AbstractAction() {
						/**
						 * Perform the CLOSE operation.
						 *
						 * @param event The triggering action event.
						 */
						@Override
						public void actionPerformed(ActionEvent event) {
							close();
						}
					});

			} // end of initEditActions method

			/**
			 * The current zoom scale (1.0 = 100%).
			 *
			 * @return the scale.
			 */
			double getViewportScale() {

				return viewport.getScale();
			} // end of getViewportScale method

			/**
			 * Recompute the component's preferred size from the model canvas
			 * size and the current zoom, then revalidate so the scroll pane
			 * updates its scrollbars (issue #74).
			 */
			void applyPreferredSize() {

				int w = (int)Math.ceil(modelSize.width * viewport.getScale());
				int h = (int)Math.ceil(modelSize.height * viewport.getScale());
				setPreferredSize(new Dimension(w,h));
				revalidate();
			} // end of applyPreferredSize method

			/**
			 * Set the logical drawing-area size (issue #74). The area never
			 * shrinks below the default circuit size; the preferred size is
			 * updated for the current zoom.
			 *
			 * @param size The requested model-space size.
			 */
			void setModelSize(Dimension size) {

				modelSize = new Dimension(
						Math.max(size.width,JLSInfo.circuitsize),
						Math.max(size.height,JLSInfo.circuitsize));
				applyPreferredSize();
			} // end of setModelSize method

			/**
			 * Grow the model drawing area, if needed, so it contains the
			 * given model point plus a generous margin and the whole current
			 * circuit (issue #74). Auto-grow replaces the retired manual 10%
			 * grow button; the area never shrinks, so saved coordinates stay
			 * absolute.
			 *
			 * @param mx The model x that should be inside the area.
			 * @param my The model y that should be inside the area.
			 */
			private void autoGrow(int mx, int my) {

				int margin = 10*JLSInfo.spacing;
				int needW = Math.max(mx + margin, modelSize.width);
				int needH = Math.max(my + margin, modelSize.height);
				Rectangle b = circuit.getBounds();
				if (b != null) {
					needW = Math.max(needW, b.x + b.width + margin);
					needH = Math.max(needH, b.y + b.height + margin);
				}
				if (needW > modelSize.width || needH > modelSize.height) {
					modelSize = new Dimension(needW,needH);
					applyPreferredSize();
				}
			} // end of autoGrow method

			/**
			 * Repaint a dirty region expressed in model coordinates by
			 * mapping it through the view transform to this component's
			 * pixels (issue #74); a null region repaints everything. The
			 * dirty-region logic of issues #35/#43 computes in model space,
			 * so this single mapping keeps those optimizations correct at
			 * any zoom.
			 *
			 * @param modelRect The changed region in model units, or null.
			 */
			private void repaintModel(Rectangle modelRect) {

				if (modelRect == null) {
					repaint();
					return;
				}
				repaint(viewport.toScreen(modelRect));
			} // end of repaintModel method

			/**
			 * The center of the currently visible canvas, in this
			 * component's coordinates - the anchor keyboard zoom holds fixed
			 * (issue #74).
			 *
			 * @return the visible-canvas center point.
			 */
			private Point visibleCenter() {

				JViewport vp = pane.getViewport();
				Point pos = vp.getViewPosition();
				Dimension ext = vp.getExtentSize();
				return new Point(pos.x + ext.width/2, pos.y + ext.height/2);
			} // end of visibleCenter method

			/**
			 * Apply a new zoom scale (clamped), keeping the model point under
			 * the given component-space anchor fixed on screen (issue #74).
			 * The scale lives in the Viewport; the fixed-point math is done
			 * against the scroll position, since panning is the scroll
			 * pane's job.
			 *
			 * @param newScale The requested scale; clamped to the permitted
			 *                 range.
			 * @param anchorX  The component-space x to hold fixed.
			 * @param anchorY  The component-space y to hold fixed.
			 */
			private void applyZoom(double newScale, int anchorX, int anchorY) {

				double oldScale = viewport.getScale();
				double clamped = Viewport.clampScale(newScale);
				if (clamped == oldScale)
					return;

				// the model point currently under the anchor
				double modelX = anchorX / oldScale;
				double modelY = anchorY / oldScale;

				// visible offset of the anchor within the scroll viewport,
				// which must be preserved so the point stays under the cursor
				JViewport vp = pane.getViewport();
				Point viewPos = vp.getViewPosition();
				int visOffX = anchorX - viewPos.x;
				int visOffY = anchorY - viewPos.y;

				// set the new scale (translation stays zero) and resize
				viewport.zoomTo(clamped,0,0);
				applyPreferredSize();
				pane.validate();

				// the anchor model point's new component coordinate, placed
				// back at the same visible offset
				int newX = (int)Math.round(modelX*clamped) - visOffX;
				int newY = (int)Math.round(modelY*clamped) - visOffY;
				Dimension ext = vp.getExtentSize();
				Dimension viewSz = vp.getViewSize();
				int maxX = Math.max(0, viewSz.width - ext.width);
				int maxY = Math.max(0, viewSz.height - ext.height);
				vp.setViewPosition(new Point(
						Math.min(Math.max(0,newX),maxX),
						Math.min(Math.max(0,newY),maxY)));
				repaint();
			} // end of applyZoom method

			/**
			 * Zoom in one keyboard ladder stop, centered on the visible
			 * canvas (issue #74).
			 */
			void zoomInCentered() {

				Point c = visibleCenter();
				applyZoom(Viewport.ladderUp(viewport.getScale()),c.x,c.y);
			} // end of zoomInCentered method

			/**
			 * Zoom out one keyboard ladder stop, centered on the visible
			 * canvas (issue #74).
			 */
			void zoomOutCentered() {

				Point c = visibleCenter();
				applyZoom(Viewport.ladderDown(viewport.getScale()),c.x,c.y);
			} // end of zoomOutCentered method

			/**
			 * Reset the zoom to exactly 100%, keeping the visible-canvas
			 * center fixed (issue #74).
			 */
			void zoomResetCentered() {

				Point c = visibleCenter();
				applyZoom(1.0,c.x,c.y);
			} // end of zoomResetCentered method

			/**
			 * Fit the whole circuit into the visible canvas and center it
			 * (issue #74) - the adjudicated "reset the view" affordance. An
			 * empty circuit resets to 100%.
			 */
			void zoomFit() {

				Rectangle b = circuit.getBounds();
				JViewport vp = pane.getViewport();
				Dimension ext = vp.getExtentSize();
				if (b == null || b.width <= 0 || b.height <= 0
						|| ext.width <= 0 || ext.height <= 0) {
					zoomResetCentered();
					return;
				}

				// generous margin so the circuit is not flush to the edges
				int margin = 2*JLSInfo.spacing;
				double s = Viewport.clampScale(Math.min(
						ext.width / (double)(b.width + 2*margin),
						ext.height / (double)(b.height + 2*margin)));
				viewport.zoomTo(s,0,0);

				// make sure the model area still contains the whole circuit
				setModelSize(new Dimension(b.x + b.width + margin,
						b.y + b.height + margin));
				applyPreferredSize();
				pane.validate();

				// center the circuit's model center in the viewport
				int cx = (int)Math.round((b.x + b.width/2.0)*s) - ext.width/2;
				int cy = (int)Math.round((b.y + b.height/2.0)*s) - ext.height/2;
				Dimension viewSz = vp.getViewSize();
				int maxX = Math.max(0, viewSz.width - ext.width);
				int maxY = Math.max(0, viewSz.height - ext.height);
				vp.setViewPosition(new Point(
						Math.min(Math.max(0,cx),maxX),
						Math.min(Math.max(0,cy),maxY)));
				repaint();
			} // end of zoomFit method

			/**
			 * Rotate the single selected element, if there is exactly one
			 * and it can rotate. Shared by the popup menu items and the
			 * R/Shift+R key bindings (#75).
			 *
			 * @param clockwise true to rotate clockwise, false for
			 * counter-clockwise.
			 */
			private void rotateSelected(boolean clockwise) {

				if (!enabled)
					return;
				if (selected.size() != 1)
					return;
				Element el = (Element)(selected.toArray()[0]);
				if (!el.canRotate() || el.isUneditable())
					return;

				// #167: through the op entry point
				submitOp(new RotateElement(el.getStableId(), clockwise));
				clearSelected();
				setState(State.idle);
				repaint();
			} // end of rotateSelected method

			/**
			 * Flip the single selected element, if there is exactly one and
			 * it can flip. Shared by the popup menu item and the F key
			 * binding (#75).
			 */
			private void flipSelected() {

				if (!enabled)
					return;
				if (selected.size() != 1)
					return;
				Element el = (Element)(selected.toArray()[0]);
				if (!el.canFlip() || el.isUneditable())
					return;

				// #167: through the op entry point
				submitOp(new FlipElement(el.getStableId()));
				clearSelected();
				setState(State.idle);
				repaint();
			} // end of flipSelected method

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
			 * @return the icon, or null if the image resource is missing.
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
			 * @return the toolbar button for the element.
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

				// apply the view transform (issue #74): from here on the
				// Graphics is in model coordinates, so the grid, the circuit,
				// and the selection rectangles all draw with model-space
				// numbers and are scaled once here. Panning is the scroll
				// pane's job, so the transform is scale-only (translate 0).
				gg.transform(viewport.createTransform());

				// draw background grid, but only across the visible (clipped)
				// model region so a large zoomed-out canvas does not draw
				// thousands of off-screen lines
				gg.setColor(JLSInfo.gridColor);
				Rectangle clip = gg.getClipBounds();
				int mx0 = clip == null ? 0 : Math.max(0,clip.x);
				int my0 = clip == null ? 0 : Math.max(0,clip.y);
				int mx1 = clip == null ? modelSize.width
						: Math.min(modelSize.width,clip.x + clip.width);
				int my1 = clip == null ? modelSize.height
						: Math.min(modelSize.height,clip.y + clip.height);
				int firstR = (my0/JLSInfo.spacing)*JLSInfo.spacing;
				for (int r=firstR; r<=my1; r+=JLSInfo.spacing) {
					gg.drawLine(mx0,r,mx1,r);
				}
				int firstC = (mx0/JLSInfo.spacing)*JLSInfo.spacing;
				for (int c=firstC; c<=mx1; c+=JLSInfo.spacing) {
					gg.drawLine(c,my0,c,my1);
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

				// draw the keyboard construction caret (issue #75): a small
				// crosshair marking where the next keyboard placement or
				// wire endpoint lands. Shown only when no mouse gesture owns
				// the screen (idle or a held selection), since while placing
				// or drawing the element or wire end is itself the cursor
				if (caret != null
						&& (currentState == State.idle
							|| currentState == State.selected)) {
					gg.setColor(JLSInfo.selectionColor);
					int r = JLSInfo.spacing/2;
					gg.drawLine(caret.x-r,caret.y,caret.x+r,caret.y);
					gg.drawLine(caret.x,caret.y-r,caret.x,caret.y+r);
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
			 * React to the one popup item still wired through the
			 * ActionListener: matchJump. Every other editing operation now
			 * dispatches through its shared {@link Action} (#75), so only the
			 * contextual "Create Matching End" item (JumpStart only) is
			 * handled here.
			 *
			 * @param event The event object for actions.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {

				info.setForeground(Color.BLACK);
				info.setText("");

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

			} // end of actionPerformed method

			/**
			 * React to mousePressed events
			 * 
			 * @param event The event object for presses.
			 */
			@Override
			public void mousePressed(MouseEvent event) {

				// a click gives the canvas keyboard focus, the standard
				// desktop focus model (issue #75); this replaces the old
				// focus-follows-mouse grab in mouseEntered
				requestFocusInWindow();

				// space-drag or middle-drag starts a pan (issue #74); pan is
				// a view-only gesture, allowed even while editing is disabled
				if (spaceDown || event.getButton() == MouseEvent.BUTTON2) {
					panning = true;
					panAnchorX = event.getXOnScreen();
					panAnchorY = event.getYOnScreen();
					panStartView = pane.getViewport().getViewPosition();
					return;
				}

				// do nothing if editor is disabled
				if (!enabled)
					return;

				info.setForeground(Color.BLACK);
				info.setText("");

				// get event information; the mouse point is inverted through
				// the view transform once here so all downstream hit-testing
				// and placement stays in model coordinates (issue #74)
				sx = event.getX();
				sy = event.getY();
				Point m = viewport.toModel(event.getPoint());
				x = m.x;
				y = m.y;
				autoGrow(x,y);
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
								if (el instanceof Wire dragWire) {
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
								if (el instanceof WireEnd wend && wend.isAttached())
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
								if (el instanceof WireEnd wend && wend.isAttached())
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
									optionMenu.show(this,sx,sy);
								}
								return;
							}
						}
						newMenu.show(this,sx,sy);
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
								optionMenu.show(this,sx,sy);
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

					// commit the placement (shared with the keyboard Enter
					// path, issue #75)
					commitPlacing();
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

					// left button -> commit this wire vertex (shared with the
					// keyboard Enter path, issue #75)
					else {
						commitWireVertex();
						return;
					}

				}
			} // end of mousePressed method

			/**
			 * Commit the element(s) being placed at their current position
			 * (issue #75). Extracted from the left-button branch of
			 * {@link #mousePressed} so the mouse click and the keyboard
			 * Enter key share one implementation. Does nothing and returns
			 * false when the placement overlaps something, matching the
			 * mouse behavior that ignores a drop onto an overlap.
			 *
			 * @return true if the placement was committed, false if an
			 *         overlap blocked it.
			 */
			private boolean commitPlacing() {

				// don't do anything if there is overlap
				if (overlap()) {
					return false;
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
				return true;
			} // end of commitPlacing method

			/**
			 * Commit the current wire vertex and continue or finish the
			 * wire (issue #75). Extracted verbatim from the left-button
			 * branch of {@link #mousePressed} so the mouse click and the
			 * keyboard Enter key share one implementation: it fixes the
			 * current wire end, connects it to any coincident port or wire,
			 * finishes the wire if that end became attached, and otherwise
			 * spawns the next segment. An overlap blocks the commit exactly
			 * as it does for the mouse.
			 */
			private void commitWireVertex() {

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

				// keep the caret on the moving end so arrows extend it
				caret = new Point(x,y);

				// finish up
				setState(State.drawire);
				repaint();
			} // end of commitWireVertex method

			/**
			 * The default keyboard caret position (issue #75): the center
			 * of the visible canvas, in model coordinates, snapped to the
			 * grid. Used the first time an arrow key is pressed while idle,
			 * so the caret appears where the user is looking rather than at
			 * the origin.
			 *
			 * @return the snapped model-space center of the viewport.
			 */
			private Point defaultCaret() {

				JViewport vp = pane.getViewport();
				Point view = vp.getViewPosition();
				Dimension ext = vp.getExtentSize();
				Point c = viewport.toModel(new Point(
						view.x + ext.width/2, view.y + ext.height/2));
				int step = JLSInfo.spacing;
				return new Point(
						KeyboardConstructionPolicy.snap(c.x,step),
						KeyboardConstructionPolicy.snap(c.y,step));
			} // end of defaultCaret method

			/**
			 * An action that nudges in one grid direction (issue #75). All
			 * four arrow keys map to one of these; the action just forwards
			 * to {@link #keyboardNudge} so the per-state dispatch lives in
			 * one place.
			 *
			 * @param n the direction this arrow key moves.
			 * @return the action bound to that arrow key.
			 */
			private AbstractAction nudgeAction(final KeyboardConstructionPolicy.Nudge n) {

				return new AbstractAction() {
					/**
					 * Nudge one grid step in this action's direction.
					 *
					 * @param event The triggering action event.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						keyboardNudge(n);
					}
				};
			} // end of nudgeAction method

			/**
			 * Handle an arrow-key nudge, dispatched by the current editor
			 * state (issue #75): move the following element while placing,
			 * extend the wire end while drawing, nudge the selection through
			 * an undoable move op while a selection is held, and otherwise
			 * move the free grid caret. States with an in-flight mouse
			 * gesture (moving, selecting, option) ignore the keys.
			 *
			 * @param n the direction to nudge one grid step.
			 */
			private void keyboardNudge(KeyboardConstructionPolicy.Nudge n) {

				if (!enabled)
					return;
				int step = JLSInfo.spacing;
				int dx = n.dx(step);
				int dy = n.dy(step);
				switch (currentState) {
					case chosen: {

						// begin following from the element's own position so
						// the caret and the element stay aligned
						Element item = (Element)(selected.toArray()[0]);
						x = item.getX();
						y = item.getY();
						setState(State.placing);
						nudgeFollowing(dx,dy);
						break;
					}
					case placing:
					case startwire:
					case drawire:
						nudgeFollowing(dx,dy);
						break;
					case selected:
						nudgeSelection(dx,dy);
						break;
					case idle:
						moveCaret(dx,dy);
						break;
					default:
						break;
				}
			} // end of keyboardNudge method

			/**
			 * The element the keyboard caret should track while following
			 * (issue #75): the wire end while drawing a wire, otherwise the
			 * first non-wire element of the selection. Its position is
			 * grid-aligned, so anchoring the caret to it keeps the caret on
			 * the grid.
			 *
			 * @return the representative element, or null if none.
			 */
			private Element followRepresentative() {

				if (wireEnd != null && selected.contains(wireEnd)) {
					return wireEnd;
				}
				for (Element el : selected) {
					if (!(el instanceof Wire)) {
						return el;
					}
				}
				return null;
			} // end of followRepresentative method

			/**
			 * Move the element(s) or wire end currently following the
			 * keyboard by one grid step (issue #75), mirroring the placing
			 * branch of {@link #mouseMoved}: shift the selection, re-index,
			 * track the reference point and caret, and give the same overlap
			 * feedback and dirty-region repaint.
			 *
			 * @param dx the x grid delta.
			 * @param dy the y grid delta.
			 */
			private void nudgeFollowing(int dx, int dy) {

				Rectangle dirty = union(selectionBounds(), touchedBounds());
				for (Element el : selected) {
					el.move(dx,dy);
				}
				circuit.reindexAfterMove(selected);

				// anchor the logical cursor and the caret to a representative
				// element's own grid-aligned position rather than accumulating
				// deltas onto the (possibly off-grid) mouse coordinate, so the
				// keyboard caret always lands on the grid and coincides with
				// ports for wiring (issue #75)
				Element rep = followRepresentative();
				if (rep != null) {
					x = rep.getX();
					y = rep.getY();
				}
				else {
					x += dx;
					y += dy;
				}
				autoGrow(x,y);
				caret = new Point(x,y);

				// overlap feedback, as the mouse placing path shows
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
			} // end of nudgeFollowing method

			/**
			 * Nudge the current selection by one grid step through the
			 * undoable move op (issue #75), the keyboard counterpart of
			 * dragging a selection. The op path (not a raw move) keeps the
			 * nudge collaboration- and undo-correct. A move that would push
			 * an element off the canvas is rejected by the op and leaves the
			 * selection where it was.
			 *
			 * @param dx the x grid delta.
			 * @param dy the y grid delta.
			 */
			private void nudgeSelection(int dx, int dy) {

				if (selected.isEmpty())
					return;
				List<ElementId> ids = new ArrayList<ElementId>();
				for (Element el : selected) {
					ids.add(el.getStableId());
				}
				if (!submitOp(new MoveElements(ids,dx,dy)))
					return;
				if (selRect != null)
					selRect.translate(dx,dy);
				Element any = (Element)(selected.toArray()[0]);
				caret = new Point(any.getX(),any.getY());
				repaint();
			} // end of nudgeSelection method

			/**
			 * Move the free grid caret by one grid step while idle
			 * (issue #75), creating it at the viewport center the first
			 * time. The caret never leaves the canvas (clamped at zero) and
			 * seeds the drop point for the next placement and the endpoint
			 * for keyboard wiring.
			 *
			 * @param dx the x grid delta.
			 * @param dy the y grid delta.
			 */
			private void moveCaret(int dx, int dy) {

				if (caret == null)
					caret = defaultCaret();
				int nx = Math.max(0, caret.x + dx);
				int ny = Math.max(0, caret.y + dy);
				caret = new Point(nx,ny);
				x = nx;
				y = ny;
				autoGrow(nx,ny);
				repaint();
			} // end of moveCaret method

			/**
			 * Handle the keyboard commit (Enter) key, dispatched by the
			 * current editor state (issue #75): place a following element,
			 * commit a wire vertex, select the element under the caret, or
			 * drop a held selection back to idle.
			 */
			private void keyboardCommit() {

				if (!enabled)
					return;
				switch (currentState) {
					case chosen:
					case placing:
						commitPlacing();
						break;
					case startwire:
					case drawire:
						commitWireVertex();
						break;
					case idle:
						selectElementAtCaret();
						break;
					case selected:

						// finish a keyboard nudge: drop back to idle
						for (Element el : selected) {
							el.setHighlight(false);
						}
						clearSelected();
						selRect = null;
						setState(State.idle);
						repaint();
						break;
					default:
						break;
				}
			} // end of keyboardCommit method

			/**
			 * Select the element under the keyboard caret, if any, and enter
			 * the selected state so arrow keys nudge it (issue #75). The
			 * keyboard counterpart of clicking an element. Attached wire ends
			 * are skipped, exactly as the mouse selection path skips them.
			 */
			private void selectElementAtCaret() {

				if (caret == null)
					return;
				for (Element el : circuit.elementsAt(caret.x,caret.y)) {
					if (el.contains(caret.x,caret.y)) {
						if (el instanceof WireEnd wend && wend.isAttached())
							continue;
						clearSelected();
						selected.add(el);
						el.setHighlight(true);
						el.savePosition();
						selRect = new Rectangle(el.getRect());
						setState(State.selected);
						repaint();
						return;
					}
				}
			} // end of selectElementAtCaret method


			/**
			 * Set up the options popup menu.
			 * 
			 * @param el The element the menu is being shown for.
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
				if (el instanceof Wire wire) {
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

				// finish a pan gesture (issue #74) before the enabled gate,
				// so panning works during simulation too
				if (panning) {
					panning = false;
					return;
				}

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
							if (el instanceof WireEnd wend && wend.isAttached())
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
								optionMenu.show(this,sx,sy);
							}
							return;
						}
					}
					newMenu.show(this,sx,sy);
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
							if (el instanceof WireEnd end) {
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

				// a pan drag moves the scroll position (issue #74); it is a
				// view-only gesture, so it runs before the enabled gate
				if (panning) {
					int dx = event.getXOnScreen() - panAnchorX;
					int dy = event.getYOnScreen() - panAnchorY;
					JViewport vp = pane.getViewport();
					Dimension ext = vp.getExtentSize();
					Dimension viewSz = vp.getViewSize();
					int maxX = Math.max(0, viewSz.width - ext.width);
					int maxY = Math.max(0, viewSz.height - ext.height);
					int nvx = Math.min(Math.max(0,panStartView.x - dx),maxX);
					int nvy = Math.min(Math.max(0,panStartView.y - dy),maxY);
					vp.setViewPosition(new Point(nvx,nvy));
					return;
				}

				// do nothing if editor is disabled
				if (!enabled)
					return;

				info.setForeground(Color.BLACK);
				info.setText("");

				// get cursor position in model coordinates (issue #74)
				sx = event.getX();
				sy = event.getY();
				Point mp = viewport.toModel(event.getPoint());
				int nx = mp.x;
				int ny = mp.y;
				autoGrow(nx,ny);

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
							if (el instanceof WireEnd wend && wend.isAttached()) {
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
					repaintModel(dirty);
					return;
				}

			} // end of mouseDragged method

			/**
			 * Accumulate a dirty-region rectangle (#17): the union of the
			 * given rectangles, either of which may be null.
			 * 
			 * @param acc The accumulator rectangle, grown in place; may be null.
			 * @param add The rectangle to add; may be null.
			 * @return the union, or null when both inputs are null.
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
					if (el instanceof WireEnd wend) {
						for (Wire w : wend.getWires()) {
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
				repaintModel(dirty);
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

				// get mouse coordinates in model space (issue #74)
				sx = event.getX();
				sy = event.getY();
				Point mp = viewport.toModel(event.getPoint());
				int nx = mp.x;
				int ny = mp.y;
				autoGrow(nx,ny);

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
							if (el instanceof WireEnd wend && wend.isAttached()) {
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
						repaintModel(dirty);
					}
					return;
				}

				// place a just chosen element
				if (currentState == State.chosen) {
					Element item = (Element)(selected.toArray()[0]);
					Point p = getMousePosition();
					if (p == null)
						return;
					// invert the raw component position into model space (#74)
					p = viewport.toModel(p);
					x = p.x;
					y = p.y;
					autoGrow(x,y);
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
			 * Unused. Hover no longer grabs keyboard focus: the canvas
			 * takes focus on click and on tab selection instead, so
			 * shortcuts keep working when the pointer leaves the canvas
			 * (issue #75).
			 *
			 * @param event Unused.
			 */
			@Override
			public void mouseEntered(MouseEvent event) {}

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
			 * React to the mouse wheel (issue #74): a plain wheel scrolls the
			 * canvas (vertically, or horizontally with Shift), while
			 * Ctrl/Cmd+wheel zooms continuously about the cursor - the model
			 * point under the cursor stays fixed. Zooming consumes the event;
			 * scrolling is handled here rather than bubbled so the canvas'
			 * own wheel listener never swallows it.
			 *
			 * @param event The wheel event.
			 */
			@Override
			public void mouseWheelMoved(MouseWheelEvent event) {

				int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
				boolean zoomMod = (event.getModifiersEx() & menuMask) != 0;
				if (zoomMod) {
					double factor = event.getPreciseWheelRotation() < 0
							? Viewport.WHEEL_STEP
							: 1.0 / Viewport.WHEEL_STEP;
					applyZoom(viewport.getScale()*factor,
							event.getX(),event.getY());
					event.consume();
					return;
				}

				// plain wheel: scroll the appropriate bar
				JScrollBar bar = event.isShiftDown()
						? pane.getHorizontalScrollBar()
						: pane.getVerticalScrollBar();
				bar.setValue(bar.getValue()
						+ event.getUnitsToScroll()*bar.getUnitIncrement());
			} // end of mouseWheelMoved method

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
			 * @return true if can connect, false if not.
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
					if (el instanceof WireEnd end) {
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
				if (put instanceof Output out && end.getNet().hasInput()) {
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
						if (p != null && p instanceof Output out) {
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
				if (put instanceof Output out) {
					net.setInput();
					if (out.isTriState()) {
						net.setTriState(true);
					}
				}
				else if (put instanceof Input && net.isTriState()) {
					if (put.getElement() instanceof TriProp pin) {
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
				if (p1 instanceof Output out1 && p2 instanceof Output out2) {
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
				if (p1 instanceof Output o1 && o1.isTriState()) {
					net.setTriState(true);
				}
				else if (p2 instanceof Output o2 && o2.isTriState()) {
					net.setTriState(true);
				}

				// make output pin tristate if appropriate
				if (net.isTriState() && p1.getElement() instanceof TriProp pin) {
					pin.setTriState(true);
				}
				else if (net.isTriState() && p2.getElement() instanceof TriProp pin) {
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
							if (sel instanceof WireEnd end) {
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
									if (sel instanceof WireEnd end) {


										if (el instanceof WireEnd otherEnd) {

											// wire end to wire end
											if (!canConnect(end,otherEnd)) {
												untouchAll();
												return true;
											}
											touch(end);
											touch(otherEnd);
											ok = true;
										}
										else if (el instanceof Wire wire) {

											// wire end to wire
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
											if (!(el instanceof WireEnd end))
												continue;

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
									if (sel instanceof WireEnd end) {


										if (el instanceof WireEnd otherEnd) {

											// wire end to wire end
											WireEnd endLeft = connect(end,otherEnd);
											connected = true;
											if (currentState == State.startwire) {
												wireEnd = endLeft;
												net = endLeft.getNet();
											}
											ok = true;
										}
										else if (el instanceof Wire wire) {

											// wire end to wire
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
											if (!(el instanceof WireEnd end))
												continue;

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
							if (el instanceof WireEnd end) {
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
						// invert the component position into model space (#74)
						pos = viewport.toModel(pos);
						x = pos.x;
						y = pos.y;
						autoGrow(x,y);

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
							if (cel instanceof JumpStart j) {
								circuit.addJumpStart(j.getName(),j);
							}
						}

						// now copy all wire ends, checking for those attached to puts
						for (Element el : from.getElements()) {
							if (!(el instanceof WireEnd oldEnd))
								continue;
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
							if (!(el instanceof Wire wire))
								continue;
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
							if (el instanceof WireEnd end) {
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
						if (el instanceof SubCircuit sub) {

							// get circuit and set up editor for it
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
								if (!(edit instanceof Editor otherEditor))
									continue;
								if (!otherEditor.getCircuit().isImported())
									ed.addToImportMenu(otherEditor.getCircuit());
							}

							// add to tabbed pane
							tabbedParent.add(tabName,ed);
							tabbedParent.setSelectedComponent(ed);

							// disable this editor while the subcircuit is
							// being editted, with a visible explanation
							// (issue #86 H2)
							disableForSubcircuit(sub.getName());
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

						// remove if wire has a probe, add if not (#167:
						// prompting stays here in the gesture - an op is
						// pure data - and the mutation goes through the
						// op entry point)
						if (wire.hasProbe()) {
							submitOp(new RemoveProbe(wire.getStableId()));
						}
						else {
							String name = TellUser.prompt(null, "Name?");
							while (name != null && name.isEmpty()) {
								name = TellUser.prompt(null,
										"Invalid name, try again");
							}
							if (name == null) {
								// parity with the pre-op path: a
								// cancelled prompt still marked the
								// circuit changed
								markChanged();
							}
							else {
								submitOp(new AttachProbe(
										wire.getStableId(), name));
							}
						}

						// clean up
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

						// decide where the element will drop, in model coords
						// (issue #74): the last tracked mouse position (already
						// model space), or the center of the visible canvas when
						// invoked from the tool bar (event-local; #103). The
						// tool-bar drop point is computed in component space and
						// inverted through the view transform once.
						int dx = x;
						int dy = y;
						if (fromToolBar) {
							JViewport vp = pane.getViewport();
							Point view = vp.getViewPosition();
							Dimension ext = vp.getExtentSize();
							Point drop = viewport.toModel(new Point(
									view.x + ext.width/2,
									view.y + ext.height/4));
							dx = drop.x;
							dy = drop.y;
						}
						autoGrow(dx,dy);

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
								// invert the component position to model (#74)
								pos = viewport.toModel(pos);
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

							// keyboard operability (#75 H2): choosing an element
							// from the tool bar/palette leaves focus on that
							// button, so the canvas arrow/Enter placement keys
							// (WHEN_FOCUSED on this EditWindow) would not reach it.
							// Hand focus to the canvas now the element is chosen,
							// so a keyboard-only user can nudge and Enter-place it
							// without first tabbing off the palette.
							requestFocusInWindow();
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
							if (el instanceof WireEnd end) {
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
						undoManager.clearRedos();

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
					 * The single mutation entry point (issue #167, collab
					 * Stage 0b): validate the op, apply it to the circuit,
					 * then do the existing change bookkeeping (undo
					 * snapshot, checkpoint, changed flag). The
					 * collaboration layer (issue #163) will observe this
					 * same entry point; gestures migrate to it one at a
					 * time under the snapshot-undo safety net.
					 */
					private final OpSink opSink = new OpSink() {
						@Override
						public void submit(CircuitOp op) throws OpRejected {

							op.apply(circuit, getGraphics());
							markChanged();
						}
					};

					/**
					 * Submit a gesture's committed op, reporting a
					 * rejection to the user. Rejections cannot happen from
					 * correctly guarded gestures - the guards run before
					 * the op is built - so a dialog here means a gesture
					 * bug, not a user error.
					 *
					 * @param op The operation to perform.
					 *
					 * @return true if the op applied, false if rejected.
					 */
					private boolean submitOp(CircuitOp op) {

						try {
							opSink.submit(op);
							return true;
						} catch (OpRejected ex) {
							TellUser.error(JLSInfo.frame, ex.getMessage(),
									"Error");
							return false;
						}
					} // end of submitOp method

					/**
					 * Push a copy of the circuit being edited on the undo stack.
					 * The snapshot is captured in the save format (#18); the
					 * {@code UndoManager} drops it if it is identical to the
					 * top of the stack (an aborted or no-op change) and
					 * enforces the depth bound (#84).
					 */
					public void pushCopy() {

						undoManager.push(CircuitSnapshot.capture(circuit));
					} // end of pushCopy method

					/**
					 * Do undo. An in-flight gesture is cancelled first,
					 * exactly as Esc would, so the restore always runs
					 * against an idle editor (issue #39: cancel-then-apply).
					 * The stack transitions live in {@code UndoManager}
					 * (#84); restoring stays here in {@code finishDo}.
					 */
					public void undo() {

						cancelGesture();
						undoManager.undo(this::finishDo);
					} // end of undo method

					/**
					 * Do redo. Cancels any in-flight gesture first, like
					 * undo (issue #39). Stack transitions live in
					 * {@code UndoManager} (#84).
					 */
					public void redo() {

						cancelGesture();
						undoManager.redo(this::finishDo);
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
								if (tab instanceof SimpleEditor se
										&& tab != SimpleEditor.this) {
									se
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
								if (!(el instanceof OutputPin pin))
									continue;
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
							if (el instanceof JumpStart j) {
								circ.addJumpStart(j.getName(),j);
							}
							else if (el instanceof SubCircuit sc) {
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
							if (el instanceof SubCircuit sc) {
								updateNamesUsed(sc.getSubCircuit());
							}
						}
					} // end of updateNamesUsed method

				} // end of EditWindow class

			} // end of SimpleEditor class
