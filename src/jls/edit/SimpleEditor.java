package jls.edit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import jls.Circuit;
import jls.JLSInfo;
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
	protected boolean enabled = true;	// disabled when editting a subcircuit
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
	private Stack<Circuit> undos = new Stack<Circuit>();
	private Stack<Circuit> redos = new Stack<Circuit>();
	private int check = JLSInfo.checkPointFreq+1;				// number of changes, used for checkpointing
	private Map<String,JMenuItem> menuMap = new HashMap<String,JMenuItem>();
	private Map<String,Circuit> circMap = new HashMap<String,Circuit>();

	/**
	 * Create new editor.
	 * 
	 * @param pane The tabbed pane this editor is in.
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
	 * Change the name of a circuit in the import menu.
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
	private enum State {idle, chosen, placing, moving, selecting, selected, option,
		startwire, drawire};

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
			private boolean firstDraw = true;	// used to save ref to Graphics object
			private Graphics graphics;
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
				probe.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,InputEvent.CTRL_MASK));
				watch.setToolTipText("watch activity on this element during simulation");
				watch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,InputEvent.CTRL_MASK));
				modify.setToolTipText("view/modify element details");
				modify.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M,InputEvent.CTRL_MASK));
				timing.setToolTipText("change propagation delay or access time");
				timing.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T,InputEvent.CTRL_MASK));
				view.setToolTipText("view current simulated value");
				view.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,InputEvent.CTRL_MASK));
				cut.setToolTipText("cut all selected elements to clipboard");
				cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,InputEvent.CTRL_MASK));
				copy.setToolTipText("copy all selected elements to clipboard");
				copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,InputEvent.CTRL_MASK));
				delete.setToolTipText("delete all selected elements");
				delete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0));
				lock.setToolTipText("make selected elements uneditable (cannot be undone)");
				lock.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,InputEvent.CTRL_MASK));

				// TODO: Make an action for this.
				//matchJump.setToolTipText("create the wire end to match this wire start");
				//matchJump.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,InputEvent.CTRL_MASK));

				connect.setToolTipText("create a new wire");
				connect.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,InputEvent.CTRL_MASK));
				newMenu.add(connect);

				paste.setToolTipText("paste contents of clipboard");
				paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,InputEvent.CTRL_MASK));
				newMenu.add(paste);

				selAll.setToolTipText("select all elements");
				selAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,InputEvent.CTRL_MASK));
				newMenu.add(selAll);

				close.setToolTipText("close this circuit");
				close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,InputEvent.CTRL_MASK));
				newMenu.add(close);

				undo.setToolTipText("undo last modification");
				undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,InputEvent.CTRL_MASK));
				newMenu.add(undo);

				redo.setToolTipText("redo last undo");
				redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y,InputEvent.CTRL_MASK));
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
					public void actionPerformed(ActionEvent event) {

						// do nothing if editor is disabled
						if (!enabled)
							return;

						// if nothing selected and current state is idle
						if (selected.size() == 0 && currentState == State.idle) {

							// start a wire
							Point p = getMousePosition();
							if (p == null)
								return; // not in drawing window
							setState(State.startwire);
							wireEnd = new WireEnd(circuit);
							x = p.x;
							y = p.y;
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
						else if (selected.size() == 1){

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
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_W,InputEvent.CTRL_MASK),"ctrlw");
				getActionMap().put("ctrlw", ctrlw);

				// set up end wire key binding
				Action endWire = new AbstractAction() {
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
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_S,InputEvent.CTRL_MASK),"view");
				getActionMap().put("view", see);

				// set up modify key binding
				Action modify = new AbstractAction() {
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
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_M,InputEvent.CTRL_MASK),"modify");
				getActionMap().put("modify", modify);

				// set up probe key binding
				Action probe = new AbstractAction() {
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
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_P,InputEvent.CTRL_MASK),"probe");
				getActionMap().put("probe", probe);

				// set up timing key binding
				Action timing = new AbstractAction() {
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
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_T,InputEvent.CTRL_MASK),"timing");
				getActionMap().put("timing",timing);

				// set up selectAll key binding
				Action selectAll = new AbstractAction() {
					public void actionPerformed(ActionEvent event) {

						// do nothing if editor is disabled
						if (!enabled)
							return;

						doSelectAll();
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A,InputEvent.CTRL_MASK),"select all");
				getActionMap().put("select all", selectAll);

				// set up close window key binding
				Action closeWin = new AbstractAction() {
					public void actionPerformed(ActionEvent event) {
						close();
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_E,InputEvent.CTRL_MASK),"close window");
				getActionMap().put("close window", closeWin);

				// set up delete key binding
				Action deleteKey = new AbstractAction() {
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
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0),"do delete");
				getActionMap().put("do delete", deleteKey);

				// set up cut key binding
				Action cutKey = new AbstractAction() {
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
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X,InputEvent.CTRL_MASK),"do cut");
				getActionMap().put("do cut", cutKey);

				// set up copy key binding
				Action copyKey = new AbstractAction() {
					public void actionPerformed(ActionEvent event) {
						if (enabled) {
							copy();
							clearSelected();
							setState(State.idle);
							repaint();
						}
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C,InputEvent.CTRL_MASK),"do copy");
				getActionMap().put("do copy", copyKey);

				// set up paste key binding
				Action pasteKey = new AbstractAction() {
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
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V,InputEvent.CTRL_MASK),"do paste");
				getActionMap().put("do paste", pasteKey);

				// set up undo key binding
				Action undoKey = new AbstractAction() {
					public void actionPerformed(ActionEvent event) {
						if (enabled) {
							undo();
							repaint();
						}
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,InputEvent.CTRL_MASK),"do undo");
				getActionMap().put("do undo", undoKey);

				// set up redo key binding
				Action redoKey = new AbstractAction() {
					public void actionPerformed(ActionEvent event) {
						if (enabled) {
							redo();
							repaint();
						}
					}
				};
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y,InputEvent.CTRL_MASK),"do redo");
				getActionMap().put("do redo", redoKey);

				// set up lock key binding
				Action lockKey = new AbstractAction() {
					public void actionPerformed(ActionEvent event) {
						if (enabled) {

							// warn user first
							int opt = JOptionPane.showConfirmDialog(JLSInfo.frame,
									"Making elements uneditable cannot be undone.  Are you sure you want to do this?",
									"WARNING", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

							// if user still ok with it ...
							if (opt == JOptionPane.OK_OPTION) {

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
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_L,InputEvent.CTRL_MASK),"do lock");
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
					public void actionPerformed(ActionEvent event) {
						setup(new AndGate(circuit),event.getSource() instanceof JButton);
					}
				};
				gates.add(makeElement(act,"AND gate"));

				image = getImage("or");
				text = image == null ? "OR" : "";
				act = new AbstractAction(text,image) {
					public void actionPerformed(ActionEvent event) {
						setup(new OrGate(circuit),event.getSource() instanceof JButton);
					}
				};
				gates.add(makeElement(act,"OR gate"));

				image = getImage("not");
				text = image == null ? "NOT" : "";
				act = new AbstractAction(text,image) {
					public void actionPerformed(ActionEvent event) {
						setup(new NotGate(circuit),event.getSource() instanceof JButton);
					}
				};
				gates.add(makeElement(act,"NOT gate"));

				image = getImage("xor");
				text = image == null ? "XOR" : "";
				act = new AbstractAction(text,image) {
					public void actionPerformed(ActionEvent event) {
						setup(new XorGate(circuit),event.getSource() instanceof JButton);
					}
				};
				gates.add(makeElement(act,"exclusive OR gate"));

				image = getImage("nand");
				text = image == null ? "NAND" : "";
				act = new AbstractAction(text,image) {
					public void actionPerformed(ActionEvent event) {
						setup(new NandGate(circuit),event.getSource() instanceof JButton);
					}
				};
				gates.add(makeElement(act,"NAND gate"));

				image = getImage("nor");
				text = image == null ? "NOR" : "";
				act = new AbstractAction(text,image) {
					public void actionPerformed(ActionEvent event) {
						setup(new NorGate(circuit),event.getSource() instanceof JButton);
					}
				};
				gates.add(makeElement(act,"NOR gate"));

				image = getImage("delay");
				text = image == null ? "DELAY" : "";
				act = new AbstractAction(text,image) {
					public void actionPerformed(ActionEvent event) {
						setup(new DelayGate(circuit),event.getSource() instanceof JButton);
					}
				};
				gates.add(makeElement(act,"user defined signal delay"));

				image = getImage("tristate");
				text = image == null ? "TriState" : "";
				act = new AbstractAction(text,image) {
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
					public void actionPerformed(ActionEvent event) {
						setup(new JumpStart(circuit),event.getSource() instanceof JButton);
					}
				};
				wireWorks.add(makeElement(act,"name a wire"));

				image = getImage("jumpend");
				text = image == null ? "END" : "";
				act = new AbstractAction(text,image) {
					public void actionPerformed(ActionEvent event) {
						setup(new JumpEnd(circuit),event.getSource() instanceof JButton);
					}
				};
				wireWorks.add(makeElement(act,"connect to a named wire"));

				image = getImage("ipin");
				text = image == null ? "I-PIN" : "";
				act = new AbstractAction(text,image) {
					public void actionPerformed(ActionEvent event) {
						setup(new InputPin(circuit),event.getSource() instanceof JButton);
					}
				};
				wireWorks.add(makeElement(act,"input pin"));

				image = getImage("opin");
				text = image == null ? "O-PIN" : "";
				act = new AbstractAction(text,image) {
					public void actionPerformed(ActionEvent event) {
						setup(new OutputPin(circuit),event.getSource() instanceof JButton);
					}
				};
				wireWorks.add(makeElement(act,"output pin"));

				image = getImage("split");
				text = image == null ? "SPLIT" : "";
				act = new AbstractAction(text,image) {
					public void actionPerformed(ActionEvent event) {
						setup(new Splitter(circuit),event.getSource() instanceof JButton);
					}
				};
				wireWorks.add(makeElement(act,"unbundle wires"));

				image = getImage("bind");
				text = image == null ? "BIND" : "";
				act = new AbstractAction(text,image) {
					public void actionPerformed(ActionEvent event) {
						setup(new Binder(circuit),event.getSource() instanceof JButton);
					}
				};
				wireWorks.add(makeElement(act,"bundle wires"));

				image = getImage("const");
				text = image == null ? "CONST" : "";
				act = new AbstractAction(text,image) {
					public void actionPerformed(ActionEvent event) {
						setup(new Constant(circuit),event.getSource() instanceof JButton);
					}
				};
				wireWorks.add(makeElement(act,"constant value"));

				image = getImage("extend");
				text = image == null ? "1-to-N" : "";
				act = new AbstractAction(text,image) {
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
					public void actionPerformed(ActionEvent event) {
						setup(new Register(circuit),event.getSource() instanceof JButton);
					}
				};
				mem.add(makeElement(act,"register (various triggering)"));

				image = getImage("memory");
				text = image == null ? "MEMORY" : "";
				act = new AbstractAction(text,image) {
					public void actionPerformed(ActionEvent event) {
						setup(new Memory(circuit),event.getSource() instanceof JButton);
					}
				};
				mem.add(makeElement(act,"memory, various types"));

				toolbar.add(mem);

				toolbar.add(Box.createRigidArea(new Dimension(5,0)));

				JPanel comb = new JPanel(new GridLayout(2,2));

				image = getImage("mux");
				text = image == null ? "MUX" : "";
				act = new AbstractAction(text,image) {
					public void actionPerformed(ActionEvent event) {
						setup(new Mux(circuit),event.getSource() instanceof JButton);
					}
				};
				comb.add(makeElement(act,"multiplexor"));

				image = getImage("decoder");
				text = image == null ? "DEC" : "";
				act = new AbstractAction(text,image) {
					public void actionPerformed(ActionEvent event) {
						setup(new Decoder(circuit),event.getSource() instanceof JButton);
					}
				};
				comb.add(makeElement(act,"decoder"));

				image = getImage("adder");
				text = image == null ? "ADDER" : "";
				act = new AbstractAction(text,image) {
					public void actionPerformed(ActionEvent event) {
						setup(new Adder(circuit),event.getSource() instanceof JButton);
					}
				};
				comb.add(makeElement(act,"adder"));

				image = getImage("clock");
				text = image == null ? "CLOCK" : "";
				act = new AbstractAction(text,image) {
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
					public void actionPerformed(ActionEvent event) {
						setup(new Pause(circuit),event.getSource() instanceof JButton);
					}
				};
				time.add(makeElement(act,"pause simulator when asserted"));

				image = getImage("stop");
				text = image == null ? "STOP" : "";
				act = new AbstractAction(text,image) {
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
					public void actionPerformed(ActionEvent event) {
						setup(new SigGen(circuit),event.getSource() instanceof JButton);
					}
				};
				test.add(makeElement(act,"generate test signals"));

				image = getImage("display");
				text = image == null ? "DISPLAY" : "";
				act = new AbstractAction(text,image) {
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
					public void actionPerformed(ActionEvent event) {
						setup(new StateMachine(circuit),event.getSource() instanceof JButton);
					}
				};
				complex.add(makeElement(act,"state machine"));

				image = getImage("truth");
				text = image == null ? "Truth Table" : "";
				act = new AbstractAction(text,image) {
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
				return new ImageIcon(image);
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

				// save graphics object for setting up elements and
				// save circuit for undo (since finishLoad must have been
				// done by now)
				if (firstDraw) {
					this.pushCopy();
					firstDraw = false;
					graphics = g;
				}

			} // end of paintComponent method

			/**
			 * React to menu item selections.
			 * 
			 * @param event The event object for actions.
			 */
			public void actionPerformed(ActionEvent event) {

				info.setForeground(Color.BLACK);
				info.setText("");

				// if probe option selected
				if (event.getSource() == probe) {

					// do nothing if editor is disabled
					if (!enabled)
						return;

					//			// draw all elements, selected ones last
					//			try {
					//				circuit.draw(g, selected, me);
					//			} catch (Exception e) {
					//				e.printStackTrace();
					//			}

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
						int opt = JOptionPane.showConfirmDialog(JLSInfo.frame,
								"Making elements uneditable cannot be undone.  Are you sure you want to do this?",
								"WARNING", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

						// if user still ok with it ...
						if (opt == JOptionPane.OK_OPTION) {

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
						JumpEnd nel = new JumpEnd(circuit, el.getName());
						Point p = getMousePosition();
						if(p == null) { // If the context menu wasn't within JLS
							p = MouseInfo.getPointerInfo().getLocation();
							p.x -= getLocationOnScreen().x;
							p.y -= getLocationOnScreen().y;
						}
						x = p.x;
						y = p.y;
						nel.setup(graphics, this, p.x, p.y);

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
				}


			} // end of actionPerformed method

			/**
			 * React to mousePressed events
			 * 
			 * @param event The event object for presses.
			 */
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

						// see if cursor is on an element
						for (Element el : circuit.getElements()) {
							if (el.contains(x,y)) {

								// do nothing if editor is disabled
								if (!enabled)
									return;

								// drag wire means drag both ends
								if (el instanceof Wire) {
									Wire wire = (Wire)el;
									WireEnd end1 = wire.getEnd();
									if (end1.isAttached())
										continue;
									WireEnd end2 = wire.getOtherEnd(end1);
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

						// cursor is not on an element, so start selecting
						selRect = new Rectangle(x,y,0,0);
						clearSelected();
						setState(State.selecting);
					}

					// if right button show menu
					else if (rightButton) {

						// see if cursor is on an element
						for (Element el : circuit.getElements()) {
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
			public void mouseReleased(MouseEvent event) {

				// do nothing if editor is disabled
				if (!enabled)
					return;

				boolean rightButton = (event.getButton() == MouseEvent.BUTTON3) || (event.isPopupTrigger());

				if (currentState == State.idle && rightButton) {

					// see if cursor is on an element
					for (Element el : circuit.getElements()) {
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

					// move them
					for (Element el : selected) {
						el.move(nx-x,ny-y);
					}
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
					repaint();
					return;
				}

				// if selecting elements...
				if (currentState == State.selecting) {

					// create bounding rectangle
					int xc = Math.min(x,nx);
					int yc = Math.min(y,ny);
					int width = Math.abs(nx-x);
					int height = Math.abs(ny-y);
					selRect = new Rectangle(xc,yc,width,height);

					// select elements completely inside bounding rectangle
					for (Element el : circuit.getElements()) {
						if (el.isInside(selRect)) {

							// can't select an attached wire end
							if (el instanceof WireEnd && ((WireEnd)el).isAttached())
								continue;
							el.setHighlight(true);
							selected.add(el);
						}
						else {

							// and unselect those not inside
							el.setHighlight(false);
							selected.remove(el);
						}
					}
					repaint();
					return;
				}

			} // end of mouseDragged method

			/**
			 * React to mouse movements.
			 * 
			 * @param event The event object for moves.
			 */
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

					// select elements when cursor moves over them
					selected.clear();
					for (Element el : circuit.getElements()) {
						if (el.contains(nx,ny)) {

							// can't highlight an attached wire end
							if (el instanceof WireEnd && ((WireEnd)el).isAttached())
								continue;

							// highlight and display info
							selected.add(el);
							el.setHighlight(true);
							el.showInfo(info);
						}
						else {
							el.setHighlight(false);
						}
					}
					repaint();
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

					// place it
					setState(State.placing);
					repaint();
					return;
				}

				// move element while initially placing it
				if (currentState == State.placing || currentState == State.startwire ||
						currentState == State.drawire) {
					for (Element el : selected) {
						el.move(nx-x,ny-y);
					}
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
					repaint();
					return;
				}
			} // end of mouseMoved method

			/**
			 * Unused.
			 * 
			 * @param event Unused.
			 */
			public void mouseClicked(MouseEvent event) {}

			/**
			 * Get focus for keyboard events.
			 * 
			 * @param event Unused.
			 */
			public void mouseEntered(MouseEvent event) {

				requestFocusInWindow(true);
			} // end of mouseEntered method

			/**
			 * If in the idle state, unhighlight everything.
			 * 
			 * @param event Unused.
			 */
			public void mouseExited(MouseEvent event) {

				if (currentState == State.idle) {
					for (Element el : circuit.getElements()) {
						el.setHighlight(false);
						repaint();
					}
				}
			} // end of mouseExited method

			/**
			 * Update current state and show corresponding message.
			 * 
			 * @param newState The new state.
			 */
			private void setState(State newState) {

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
			 * @param end The wire end.
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
				if(put.getElement() instanceof Group) {
					boolean tri = false;
					boolean norm = false;
					for(Put p : put.getElement().getAllPuts()) {
						if(p.isAttached()) {
							if(p.getWireEnd().isTriState())
								tri = true;
							else
								norm = true;
						}
					}
					if(end.isTriState()) tri = true;
					else norm = true;
					if(tri && norm) {
						overlapMessage = "Cannot connect both tri-state and normal wires to a bundle";
						return false;
					}
				}

				// can't attach if multiple wire ends in the same wire net
				// can attach to outputs, unless they are all tristates
				int regCount = 0;
				int triCount = 0;
				for (WireEnd otherEnd : end.getNet().getAllEnds()) {
					if (end == otherEnd)
						continue;
					for (Element el : circuit.getElements()) {
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
			 * @param put1 The put.
			 * @param put2 The other put.
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
				end1.init(circuit);
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
				 * TODO major point of optimization
				 * 
				 * @return true if there is overlap, false if not.
				 */
				private boolean overlap() {

					overlapMessage = "";

					// check every element in the selected set
					//TODO radix sort when done, then check for collisions using bin search
					/*ArrayDeque<Element> selE = new ArrayDeque<Element>();
			ArrayDeque<Element> other = new ArrayDeque<Element>();

			for(Element el : circuit.getElements()){
				if(selected.contains(el))
					selE.add(el);
				else
					other.add(el);
			}

			for(Element test1 : selE){
				for(Element test2 : other){
					if(!test1.intersects(test2)) continue;
					if(!(test1 instanceof Wire || test1 instanceof WireEnd) &&
						!(test2 instanceof Wire || test2 instanceof WireEnd)){
						return true;
					}

					//TODO fringe cases

				}
			}*/


						// check every element in the selected set
						for (Element sel : selected) {

							// check against every element in the circuit
							for (Element el : circuit.getElements()) {

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
											end.setTouching(true);
											otherEnd.setTouching(true);
											ok = true;
										}
										else if (el instanceof Wire) {

											// wire end to wire
											Wire wire = (Wire)el;
											if (!canConnect(end,wire)) {
												untouchAll();
												return true;
											}
											end.setTouching(true);
											wire.setTouching(true);
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
											end.setTouching(true);
											put.setTouching(true);
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

											end.setTouching(true);
											put.setTouching(true);
											ok = true;
										}
									}
									if (!ok) {
										overlapMessage = "overlap";
										untouchAll();
										return true;
									}
								}

								// no intersection, but wires may be overlapping wire ends
								// or puts might line up
								else {

									// see if wires connected to a wire end dragged onto wire ends
									if (sel instanceof WireEnd) {
										WireEnd end = (WireEnd)sel;
										for (Wire wire : end.getWires()) {
											for (Element elm : circuit.getElements()) {
												if (sel == elm)
													continue;
												if (!(elm instanceof WireEnd)) {
													continue;
												}
												WireEnd otherEnd = (WireEnd)elm;
												if (wire.touches(otherEnd)) {
													overlapMessage = "overlap";
													untouchAll();
													return true;
												}
											}
										}
									}

									// see if wires connected to puts dragged onto wire ends
									for (Put p : sel.getAllPuts()) {
										if (p.isAttached()) {
											Wire wire = p.getWireEnd().getOnlyWire();
											for (Element elm : circuit.getElements()) {
												if (sel == elm)
													continue;
												if (!(elm instanceof WireEnd)) {
													continue;
												}
												WireEnd otherEnd = (WireEnd)elm;
												if (wire.touches(otherEnd)) {
													overlapMessage = "overlap";
													untouchAll();
													return true;
												}
											}
										}
									}

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
												p1.setTouching(true);
												p2.setTouching(true);
											}
										}
									}
								}
							}
						}
					repaint();
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

							// check against every element in the circuit
							for (Element el : circuit.getElements()) {

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
										}
									}
								}
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
					 * Untouch all wire ends and puts.
					 */
					private void untouchAll() {

						for (Element el : circuit.getElements()) {
							el.setTouching(false);
						}
					} // end of untouchAll method

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
								JOptionPane.showMessageDialog(JLSInfo.frame,
										"can't delete uneditable element");
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
							if (name != null && !name.equals("")) {
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
							if (name != null && !name.equals(""))
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

						// now copy all wire ends, checking for those attached to puts,
						// create set of all new wire ends for net partitioning later
						LinkedList<WireEnd>ends = new LinkedList<WireEnd>();
						for (Element el : from.getElements()) {
							if (!(el instanceof WireEnd))
								continue;
							WireEnd oldEnd = (WireEnd)el;
							WireEnd newEnd = (WireEnd)(el.copy());
							newEnd.fixPosition();
							newEnd.move(x,y);
							circuit.addElement(newEnd);
							ends.add(newEnd);
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
									JOptionPane.showMessageDialog(getTopLevelAncestor(),
											tabName + " is already being editted", "Error",
											JOptionPane.ERROR_MESSAGE);
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
						el.changeTiming(this, x, y);

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
								JOptionPane.showMessageDialog(JLSInfo.frame,
										"Can't add an input pin to a subcircuit");
								return;
							}
							else if (item instanceof OutputPin) {
								JOptionPane.showMessageDialog(JLSInfo.frame,
										"Can't add an output pin to a subcircuit");
								return;
							}
						}

						// clear selected if there is any, and turn off highlights
						for (Element el : selected) {
							el.setHighlight(false);
						}
						selected.clear();

						// decide position for create dialog
						int dx = x;
						int dy = y;
						if (fromToolBar) {
							dx = tabbedParent.getSize().width/2;
							dy = tabbedParent.getSize().height/4;
						}

						// if not cancelled...
						Point view = pane.getViewport().getViewPosition();
						if (item.setup(graphics,this,dx+view.x,dy+view.y)) {

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

							// create checkpoint file

							String fileName = circ.getDirectory() + "/" + circ.getName() + ".jls~";
							ZipOutputStream out = null;
							try {
								out = new ZipOutputStream(new FileOutputStream(fileName));
								out.putNextEntry(new ZipEntry("JLSCheckpoint"));
							}
							catch (IOException ex) {
								return;
							}
							PrintWriter output = new PrintWriter(out);

							// save the circuit
							boolean changed = circuit.hasChanged();
							circuit.save(output);
							output.close();
							if (changed)
								circuit.markChanged();
						}

					} // end of markChanged method

					/**
					 * Push a copy of the circuit being edited on the undo stack.
					 */
					public void pushCopy() {

						// see if undo stack is full
						if (undos.size() > JLSInfo.undoStackDepth) {

							// delete bottom of stack
							undos.remove(0);
						}

						// make a set of all elements except attached wire ends
						Set<Element> elements = new HashSet<Element>();
						for (Element el : circuit.getElements()) {
							if (el instanceof WireEnd) {
								WireEnd end = (WireEnd)el;
								if (end.isAttached())
									continue;
							}
							elements.add(el);
						}

						// copy elements to new circuit
						Circuit newCopy = new Circuit(circuit.getName());
						Util.copy(elements,newCopy);
						newCopy.setDirectory(circuit.getDirectory());

						// save for undo
						undos.push(newCopy);
					} // end of pushCopy method

					/**
					 * Do undo.
					 */
					public void undo() {

						// no undo left if stack only has a copy of the original circuit
						if (undos.size() == 1) {
							return;
						}

						// pop copy of current circuit and put on redo stack
						Circuit temp = undos.pop();
						redos.push(temp);

						// if nothing left, quit
						if (undos.isEmpty()) {
							return;
						}

						// make a copy of the pushed circuit
						Editor ed = circuit.getEditor();
						temp = undos.peek();
						finishDo(temp);
						circuit.setEditor(ed);
					} // end of undo method

					/**
					 * Do redo.
					 */
					public void redo() {

						// if nothing on the redo stack, then there is nothing to do
						if (redos.isEmpty()) {
							return;
						}

						// pop circuit from redo stack and push on undo stack
						Circuit temp = redos.pop();
						undos.push(temp);

						// make a copy of the circuit
						Editor ed = circuit.getEditor();
						finishDo(temp);
						circuit.setEditor(ed);
					} // end of redo method

					/**
					 * Finish up undo or redo.
					 * Used by undo and redo to make a copy of the circuit.
					 * 
					 * @param temp The circuit being copied.
					 */
					private void finishDo(Circuit temp) {

						// start a new copy of the circuit
						Circuit newCopy = new Circuit(circuit.getName());

						// create set of all elements except attached wire ends
						Set<Element> elements = new HashSet<Element>();
						for (Element el : temp.getElements()) {
							if (el instanceof WireEnd) {
								WireEnd end = (WireEnd)el;
								if (end.isAttached())
									continue;
							}
							elements.add(el);
						}

						// copy them to the new circuit
						Util.copy(elements,newCopy);

						// partition wires and wire ends into wire nets
						Util.partition(newCopy);

						// set their circuit to the new circuit
						for (Element el : newCopy.getElements()) {
							el.setCircuit(newCopy);
						}

						// link into subcircuit if it is imported
						SubCircuit sub = circuit.getSubElement();
						newCopy.setImported(sub);
						if (sub != null) {
							sub.setSubCircuit(newCopy);
							sub.remapPins(newCopy);
						}

						// make it be the current circuit
						circuit = newCopy;
						circuit.setDirectory(temp.getDirectory());
						circuit.markChanged();

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
					 * @param The circuit to process.
					 */
					private void updateNamesUsed(Circuit circ) {

						for (Element el : circ.getElements()) {
							if (el.getName() != null) {
								String name = el.getName();
								circ.addName(name);
							}
							else if (el instanceof SubCircuit) {
								SubCircuit sc = (SubCircuit)el;
								updateNamesUsed(sc.getSubCircuit());
							}
						}
					} // end of updateJumpStarts method

				} // end of EditWindow class

			} // end of SimpleEditor class
