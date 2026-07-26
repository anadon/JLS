package jls.edit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import jls.Help;
import jls.KeyPad;
import jls.TellUser;
import jls.core.Geometry;
import jls.core.GridPoint;
import jls.elem.Element;
import jls.elem.Input;
import jls.elem.Output;
import jls.elem.SMUtil;
import jls.elem.State;
import jls.elem.StateMachine;
import jls.util.Placement;

/**
 * GUI-side creation/edit dialog for {@link StateMachine} (issue #77).
 * Moved from the former {@code StateMachine.StateEditor}/{@code CreateState}
 * inner dialogs and the {@code State.CreateTrans}/{@code EditOutputs}
 * dialogs, plus {@code StateMachine.setup}/{@code change}, so the model
 * stays headless. One instance is registered for creation and one for
 * change; the {@code creating} flag selects which flow {@link #setup}
 * runs (both are "present the editor and apply the result").
 */
public final class StateMachineDialog implements ElementDialog {

	/** True for the create dialog, false for the edit (change) dialog. */
	private final boolean creating;

	/**
	 * Creates a {@code StateMachineDialog}.
	 *
	 * @param creating True for the create dialog, false for the edit
	 *        dialog.
	 */
	public StateMachineDialog(boolean creating) {
		this.creating = creating;
	} // end of constructor

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		StateMachine machine = (StateMachine) el;

		if (creating) {

			// show creation dialog
			StateEditor ed = new StateEditor(machine, true);

			// don't do anything if user canceled
			if (ed.wasCanceled()) {
				return false;
			}

			// complete initialization
			machine.init(SwingTextMetrics.forGraphics(g));

			// save position
			Point p = Placement.dropPoint(editWindow, x, y,
					machine.getWidth(), machine.getHeight());
			machine.setXY(p.x, p.y);
			return true;
		}

		// change: save current state machine
		StateMachine original = (StateMachine) machine.copy();

		// display dialog
		StateEditor ed = new StateEditor(machine, false);

		// if canceled, restore original state machine
		if (ed.wasCanceled()) {
			machine.setStates(original.getStates());
		}

		// if name has changed, detach
		if (ed.nameChanged()) {
			machine.reinit(SwingTextMetrics.of(g));
			return true;
		}

		// make a set of all old input names
		Set<String> oldNames = new HashSet<String>();
		for (Input input : machine.getInputs()) {
			oldNames.add(input.getName());
		}

		// make a set of all new input names
		Set<String> newNames = new HashSet<String>();
		newNames.add("clock");
		for (State state : machine.getStates()) {
			newNames.addAll(state.getInputs());
		}

		// if not the same, detach
		if (!oldNames.equals(newNames)) {
			machine.reinit(SwingTextMetrics.of(g));
			return true;
		}

		// detach if any input signal's #bits changed
		for (Input input : machine.getInputs()) {
			int bits = input.getBits();
			for (State state : machine.getStates()) {
				for (String inp : state.getInputs()) {
					if (java.util.Objects.equals(input.getName(), inp)
							&& state.inputBits(inp) != bits) {
						machine.reinit(SwingTextMetrics.of(g));
						return true;
					}
				}
			}
		}

		// make a set of all old output names
		oldNames.clear();
		for (Output output : machine.getOutputs()) {
			oldNames.add(output.getName());
		}

		// make a set of all new output names
		newNames.clear();
		for (State state : machine.getStates()) {
			newNames.addAll(state.getOutputs());
		}

		// if not the same, detach
		if (!oldNames.equals(newNames)) {
			machine.reinit(SwingTextMetrics.of(g));
			return true;
		}

		// detach if any out signal's #bits changed
		for (Output output : machine.getOutputs()) {
			int bits = output.getBits();
			for (State state : machine.getStates()) {
				for (String out : state.getOutputs()) {
					if (java.util.Objects.equals(output.getName(), out)
							&& state.outputBits(out) != bits) {
						machine.reinit(SwingTextMetrics.of(g));
						return true;
					}
				}
			}
		}

		return false;
	} // end of setup method

	// drawing states
	/**
	 * Interaction mode of the editor's drawing area, tracking what the user
	 * is currently doing (idle, placing or moving states, drawing transitions,
	 * selecting, etc.) so mouse events are handled accordingly.
	 */
	private enum DrawState {
		/** No gesture in progress. */
		idle,
		/** A new state has just been created. */
		created,
		/** A new state is following the cursor to its place. */
		placing,
		/** A selection rectangle is being dragged out. */
		selecting,
		/** A selection exists and awaits a command. */
		selected,
		/** Selected states are being dragged. */
		moving,
		/** A new transition is being drawn. */
		newTrans,
		/** A transition corner point is being dragged. */
		pointMoving
	};

	/**
	 * The state machine editor dialog.
	 */
	@SuppressWarnings("serial")
	private static final class StateEditor extends ElementFormDialog
		implements ActionListener, MouseListener, MouseMotionListener {

		/** The state machine being edited. */
		private final StateMachine machine;
		/** True if the user cancelled this dialog. */
		private boolean canceled;
		/** The drawing area the states are edited in. */
		private JPanel editArea;
		/** Message display at the top of the dialog. */
		private JLabel msgLabel = new JLabel("");
		/** Shows the current interaction mode at the top right. */
		private JLabel stateLabel = new JLabel(" ");
		/** The current interaction mode of the drawing area. */
		private DrawState drawState = DrawState.idle;
		/** The latest mouse x-coordinate. */
		private int x;
		/** The latest mouse y-coordinate. */
		private int y;
		/** The selection rectangle being dragged out, or null if none. */
		private @org.jspecify.annotations.Nullable Rectangle selrect = null;
		/** The currently selected states. */
		private Set<State> selected = new HashSet<State>();
		/** The state under the most recent mouse press, or null if none. */
		private @org.jspecify.annotations.Nullable State on;
		/** Selects triggering on the rising clock edge. */
		private JRadioButton rising = new JRadioButton("rising edge");
		/** Selects triggering on the falling clock edge. */
		private JRadioButton falling = new JRadioButton("falling edge");
		/** Expands the drawing area. */
		private JButton enlarge = new JButton("+");
		/** Input field for the state machine's name. */
		private JTextField nameField = new JTextField(30);
		/** Popup menu item to rename a state. */
		private JMenuItem changeName = new JMenuItem("change name");
		/** Popup menu item to make a state the initial state. */
		private JMenuItem makeInit = new JMenuItem("make initial state");
		/** Popup menu item to start a new transition from a state. */
		private JMenuItem addTrans = new JMenuItem("add transition");
		/** Popup menu item to delete all transitions from a state. */
		private JMenuItem deleteAllTrans = new JMenuItem("delete all transitions");
		/** Popup menu item to edit a state's outputs. */
		private JMenuItem editOutputs = new JMenuItem("edit outputs");
		/** Popup menu item to view a state's outputs. */
		private JMenuItem showOutputs = new JMenuItem("view outputs");
		/** Popup menu item to delete the selected state(s). */
		private JMenuItem delete = new JMenuItem("delete state(s)");
		/** Popup menu item to align the selected states horizontally. */
		private JMenuItem alignHor = new JMenuItem("align states horizontally");
		/** Popup menu item to align the selected states vertically. */
		private JMenuItem alignVer = new JMenuItem("align states vertically");
		/** Midpoints of the transition being drawn; the last entry is the current end point. */
		private Vector<GridPoint> points = new Vector<GridPoint>();
		/** The transition whose highlighted corner point is being dragged. */
		private State.@org.jspecify.annotations.Nullable Transition movingTrans;
		/** True if the machine's name was changed when the dialog was accepted. */
		private boolean nameChange;

		/**
		 * Set up create dialog window.
		 *
		 * @param machine The state machine being edited.
		 * @param creating True if creating a new element, false if editing an existing one.
		 */
		private StateEditor(StateMachine machine, boolean creating) {

			// set up window title
			super("Edit State Machine",null);

			// save reference to me
			this.machine = machine;

			// set not canceled
			canceled = false;

			// set up window
			Container window = getContentPane();
			window.setLayout(new BorderLayout());

			// set up message area
			JPanel messages = new JPanel(new BorderLayout());
			messages.setBackground(Color.cyan);
			messages.add(msgLabel,BorderLayout.CENTER);
			messages.add(stateLabel,BorderLayout.EAST);
			window.add(messages,BorderLayout.NORTH);

			// set up editing area
			editArea = new JPanel() {
				/**
				 * Paint the states, any selection rectangle and an
				 * in-progress transition in the editing area.
				 */
				@Override
				public void paintComponent(Graphics g) {
					super.paintComponent(g);
					if (selrect != null) {
						if (drawState == DrawState.selecting) {
							g.setColor(Color.GRAY);
							g.drawRect(selrect.x,selrect.y,selrect.width,selrect.height);
						}
						else {
							g.setColor(Color.lightGray);
							g.fillRect(selrect.x,selrect.y,selrect.width,selrect.height);
						}
					}
					State from = on;
					if (drawState == DrawState.newTrans && from != null) {
						drawTrans(from,points,g);
					}
					for (State state : machine.getStates()) {
						StateRenderer.draw(g, state);
					}
				}
			};
			if (creating) {
				Rectangle screen = getGraphicsConfiguration().getBounds();
				editArea.setPreferredSize(new Dimension(screen.width-100,screen.height-100));
			}
			else {
				Rectangle es = null;
				for (State state : machine.getStates()) {
					if (es == null)
						es = StateRenderer.getBounds(null, state);
					else
						es.add(StateRenderer.getBounds(null, state));
				}
				if (es != null) {
					int w = (int)((es.x+es.width)*1.1);
					int h = (int)((es.y+es.height)*1.1);
					editArea.setPreferredSize(new Dimension(w,h));
				}
				else {
					Rectangle screen = getGraphicsConfiguration().getBounds();
					editArea.setPreferredSize(new Dimension(screen.width-100,screen.height-100));
				}
			}
			editArea.setBackground(Color.white);
			editArea.addMouseListener(this);
			editArea.addMouseMotionListener(this);
			JScrollPane pane = new JScrollPane(editArea);
			window.add(pane,BorderLayout.CENTER);

			// set up bottom stuff
			JPanel bottom = new JPanel(new GridLayout(4,1));

			JPanel stuff = new JPanel(new BorderLayout());

			JPanel trig = new JPanel(new FlowLayout(FlowLayout.CENTER));
			trig.add(new JLabel("Trigger: "));
			trig.add(rising);
			trig.add(falling);
			ButtonGroup group = new ButtonGroup();
			group.add(rising);
			group.add(falling);
			if (machine.getTrigger() == 1)
				rising.setSelected(true);
			else
				falling.setSelected(true);
			stuff.add(trig,BorderLayout.CENTER);
			stuff.add(enlarge,BorderLayout.EAST);
			bottom.add(stuff);
			enlarge.setToolTipText("expand drawing area");

			JPanel namePanel = new JPanel(new FlowLayout());
			namePanel.add(new JLabel("Name: "));
			namePanel.add(nameField);
			nameField.setText(machine.getName());
			bottom.add(namePanel);

			bottom.add(getErrorLabel());

			JPanel okCancel = new JPanel(new GridLayout(1,3));
			okCancel.add(ok);
			okCancel.add(cancel);
			JButton help = new JButton("Help");
			Help.enableHelpOnButton(help, "stmach");
			okCancel.add(help);
			bottom.add(okCancel);

			window.add(bottom,BorderLayout.SOUTH);

			// set up listeners (OK, Cancel, Escape and the close box are
			// wired by the shared dialog base, issue #26)
			installDialogBehavior();
			enlarge.addActionListener(this);
			changeName.addActionListener(this);
			makeInit.addActionListener(this);
			addTrans.addActionListener(this);
			deleteAllTrans.addActionListener(this);
			editOutputs.addActionListener(this);
			showOutputs.addActionListener(this);
			delete.addActionListener(this);
			alignHor.addActionListener(this);
			alignVer.addActionListener(this);

			// set up new state key binding
			Action newState = new AbstractAction() {
				/**
				 * Handle the new-state key binding by creating a state.
				 */
				@Override
				public void actionPerformed(ActionEvent event) {

					createNewState();
				}
			};
			editArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_N,Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),"new state");
			editArea.getActionMap().put("new state", newState);

			// finish up GUI: size to the monitor this dialog will appear
			// on and place relative to the owner window (#104)
			Rectangle screen = getGraphicsConfiguration().getBounds();
			Dimension dialog = getPreferredSize();
			int w = Math.min(dialog.width,screen.width-100);
			int h = Math.min(dialog.height+100,screen.height-100);
			setSize(w,h);
			setLocationRelativeTo(getOwner());
			setVisible(true);

		} // end of constructor

		/**
		 * See if the editor was cancelled.
		 *
		 * @return true if cancelled, false if not.
		 */
		public boolean wasCanceled() {

			return canceled;
		} // end of wasCanceled method

		/**
		 * Set and display current drawing state.
		 *
		 * @param state The new drawing state.
		 */
		public void setDrawState(DrawState state) {

			drawState = state;
			switch(state) {
			case idle:
				stateLabel.setText(" ");
				break;
			case created:
				stateLabel.setText("created new state");
				break;
			case placing:
				stateLabel.setText("left click to place, right click to cancel");
				break;
			case selecting:
				stateLabel.setText("selecting states");
				break;
			case selected:
				stateLabel.setText("selected states");
				break;
			case moving:
				stateLabel.setText("moving state(s)");
				break;
			case newTrans:
				stateLabel.setText("creating new transition");
				break;
			case pointMoving:
				stateLabel.setText("moving a transition corner");
			}
		} // end of setDrawState method

		/**
		 * Draw a partial transition.
		 * Does not draw arrow, used during transition creation.
		 *
		 * @param from Starting state.
		 * @param points Midpoints, last entry is current end point.
		 * @param g The Graphics object to use.
		 */
		public void drawTrans(State from, Vector<GridPoint> points, Graphics g) {

			GridPoint last = from.getLocation();
			GridPoint prev = null;
			for (GridPoint p : points) {
				g.setColor(Color.black);
				g.drawLine(last.x(),last.y(),p.x(),p.y());
				prev = last;
				last = p;
			}
			if (prev != null) {
				StateRenderer.drawArrow(last.x(),last.y(),SMUtil.getAngle(last.x()-prev.x(),prev.y()-last.y()),g);
			}
		} // end of drawTrans method

		/**
		 * React to events.
		 *
		 * @param event The event.
		 */
		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource() == changeName) {
				if (on != null) {
					CreateState cs = new CreateState(machine,"Change",on.getName());
					if (!cs.wasCancelled()) {
						Graphics eg = editArea.getGraphics();
						if (eg != null) {
							boolean ok = on.changeName(cs.getName(),SwingTextMetrics.of(eg));
							if (!ok) {
								TellUser.error(this,
										"Name won't fit", "Error");
								return;
							}
							editArea.repaint();
						}
					}
				}
			}
			else if (event.getSource() == makeInit) {

				if (on != null) {

					// turn off initial state anywhere else
					for (State state : machine.getStates()) {
						state.setInitial(false);
					}

					// make current state initial
					on.setInitial(true);
					setDrawState(DrawState.idle);
					editArea.repaint();
				}
			}
			else if (event.getSource() == addTrans) {
				points.clear();
				points.add(new GridPoint(x,y));
				setDrawState(DrawState.newTrans);
			}
			else if (event.getSource() == deleteAllTrans) {
				if (on != null) {
					on.deleteAllTrans();
					setDrawState(DrawState.idle);
					editArea.repaint();
				}
			}
			else if (event.getSource() == editOutputs) {
				if (on != null) {
					new EditOutputs(on);
				}
			}
			else if (event.getSource() == showOutputs) {
				if (on != null) {
					showOuts(on, this);
				}
			}
			else if (event.getSource() == delete) {
				if (on != null) {

					// remove all transitions to this state
					for (State state : machine.getStates()) {
						state.removeTrans(on);
					}

					// remove this state and its transitions
					machine.getStates().remove(on);

					// finish up
					selrect = null;
					setDrawState(DrawState.idle);
					editArea.repaint();
				}
				else {

					// remove every selected state
					for (State state : selected) {

						// remove transitions to each state first
						for (State st : machine.getStates()) {
							st.removeTrans(state);
						}

						// then remove the each state
						machine.getStates().remove(state);
					}

					// finish up
					selrect = null;
					setDrawState(DrawState.idle);
					editArea.repaint();
				}
			}
			else if (event.getSource() == alignHor) {

				// find average x-value
				int sum = 0;
				for (State state : selected) {
					sum += state.getX();
				}
				int newx = sum/selected.size();

				// move states
				for (State state : selected) {
					state.savePosition();
					state.setX(newx);
				}

				// check for overlaps
				if (stateOverlap()) {
					TellUser.error(this,"Can't do - states will overlap", "Error");
					for (State state : selected) {
						state.restorePosition();
					}
				}

				// finish up
				selected.clear();
				selrect = null;
				setDrawState(DrawState.idle);
				editArea.repaint();
			}
			else if (event.getSource() == alignVer) {

				// find average y-value
				int sum = 0;
				for (State state : selected) {
					sum += state.getY();
				}
				int newy = sum/selected.size();

				// move states
				for (State state : selected) {
					state.savePosition();
					state.setY(newy);
				}

				// check for overlaps
				if (stateOverlap()) {
					TellUser.error(this,"Can't do - states overlap", "Error");
					for (State state : selected) {
						state.restorePosition();
					}
				}

				// finish up
				selected.clear();
				selrect = null;
				setDrawState(DrawState.idle);
				editArea.repaint();
			}
			else if (event.getSource() == enlarge) {
				Dimension cs = editArea.getPreferredSize();
				int width = (int)(cs.width*1.1);
				int height = (int)(cs.height*1.1);
				editArea.setPreferredSize(new Dimension(width,height));
				editArea.revalidate();
			}

		} // end of actionPerformed method

		/**
		 * See if any states in the selected set overlap any state.
		 *
		 * @return true if there is overlap, false if not
		 */
		private boolean stateOverlap() {

			for (State sel : selected) {
				for (State state : machine.getStates()) {
					if (sel == state)
						continue;
					if (sel.overlaps(state)) {
						return true;
					}
				}
			}
			return false;
		} // end of stateOverlap method

		/**
		 * A machine must have an initial state before the editor closes
		 * (issue #52, M13); the same rule simulation start repairs for
		 * hand-edited files. The remedy is in the editor itself: right
		 * click a state and choose "make initial state".
		 */
		@Override
		protected java.util.List<Violation> validateInputs() {

			String violated = machine.checkInitialState();
			if (violated != null) {
				return java.util.List.of(new Violation(violated, null));
			}
			return java.util.List.of();
		} // end of validateInputs method

		/**
		 * Apply the validated form to the state machine.
		 */
		@Override
		protected void validateAndAccept() {

			if (rising.isSelected()) {
				machine.setTrigger(1);
			}
			else {
				machine.setTrigger(-1);
			}
			String oldName = machine.getName();
			String newName = nameField.getText().trim();
			machine.setName(newName);
			if (oldName.equals(newName)) {
				nameChange = false;
			}
			else {
				nameChange = true;
			}
			dispose();
		} // end of validateAndAccept method

		/**
		 * Cancel this dialog.
		 */
		@Override
		protected void cancelDialog() {

			canceled = true;
			dispose();
		} // end of cancelDialog method

		/**
		 * See if the name of this element has changed.
		 *
		 * @return true if the name has changed, false if it hasn't.
		 */
		public boolean nameChanged() {

			return nameChange;
		} // end of nameChanged method

		/**
		 * React to mouse pressed events.
		 *
		 * @param event The mouse event.
		 */
		@Override
		public void mousePressed(MouseEvent event) {

			// get info
			x = event.getX();
			y = event.getY();
			boolean leftButton = event.getButton() == MouseEvent.BUTTON1;
			boolean rightButton = event.getButton() == MouseEvent.BUTTON3;

			if (drawState == DrawState.idle) {

				// if left button
				if (leftButton) {

					// see if a transition point is at x,y
					for (State state : machine.getStates()) {
						State.Transition tr = state.highlightTransPoints(x,y);
						if (tr != null) {
							movingTrans = tr;
							setDrawState(DrawState.pointMoving);
							return;
						}
					}

					// see if a state is at x,y
					selected.clear();
					for (State state : machine.getStates()) {
						if (state.contains(x,y)) {
							state.setHighlight(true);
							selected.add(state);
							state.savePosition();
						}
					}

					// if no state there, then start selection rectangle
					if (selected.isEmpty()) {
						selrect = new Rectangle(x,y,0,0);
						setDrawState(DrawState.selecting);
						editArea.repaint();
						return;
					}

					// otherwise start moving stuff
					else {
						setDrawState(DrawState.moving);
						editArea.repaint();
						return;
					}
				}

				// if right button...
				else if (rightButton){

					// see if over a transition
					for (State state : machine.getStates()) {
						if (state.highlightTrans(x,y)) {
							boolean result = TellUser.confirm(this,
						            "delete transition?", "option");
							if (result) {
								state.deleteLastHighlighted();
								editArea.repaint();
							}
							return;
						}
					}

					// see if there is a single state at this position
					on = null;
					for (State state : machine.getStates()) {
						if (state.contains(x,y)) {
							on = state;
							break;
						}
					}

					// if so, construct and show state option menu
					if (on != null) {
						JPopupMenu menu = new JPopupMenu();
						menu.add(addTrans);
						menu.add(showOutputs);
						menu.add(editOutputs);
						menu.add(changeName);
						menu.add(makeInit);
						menu.add(deleteAllTrans);
						menu.add(delete);
						menu.show(editArea,x,y);
					}

					// otherwise create a new state
					else {
						createNewState();
						return;
					}
				}
			}

			// if just created
			if (drawState == DrawState.created) {
				Point p = editArea.getMousePosition();
				if (p != null) {
					for (State state : selected) {
						state.moveTo(p.x,p.y);
					}
				}
				repaint();
				return;
			}

			// if placing...
			if (drawState == DrawState.placing) {

				// right click cancels
				if (rightButton) {
					setDrawState(DrawState.idle);
					for (State state : selected) {
						machine.getStates().remove(state);
					}
					selected.clear();
					editArea.repaint();
					return;
				}

				// check for overlap
				boolean overlaps = false;
				for (State sel : selected) {
					for (State st : machine.getStates()) {
						if (sel != st && sel.overlaps(st)) {
							overlaps = true;
						}
					}
				}
				if (overlaps) {
					return;
				}

				// finish up
				setDrawState(DrawState.idle);
				for (State state : selected) {
					state.setHighlight(false);
				}
				selected.clear();
				editArea.repaint();
				mouseMoved(event);
				return;
			}

			// if selected...
			if (drawState == DrawState.selected) {

				if (leftButton) {
					if (selrect != null && selrect.contains(x,y)) {
						setDrawState(DrawState.moving);
						selrect = null;
						repaint();
					}
					else {
						setDrawState(DrawState.idle);
						selrect = null;
						mousePressed(event);
					}
				}
				else { // right button

					// see if it is in the selected area
					if (selrect != null && selrect.contains(x,y)) {
						JPopupMenu menu = new JPopupMenu();
						menu.add(delete);
						if (selected.size() > 1) {
							menu.add(alignHor);
							menu.add(alignVer);
						}
						menu.show(editArea,x,y);
					}
				}
			}

			// if adding a new transition
			if (drawState == DrawState.newTrans) {

				// right button cancels
				if (rightButton) {
					points.clear();
					for (State state : machine.getStates()) {
						state.setHighlight(false);
					}
					setDrawState(DrawState.idle);
					editArea.repaint();
					return;
				}

				// if not on a state, add point
				State target = null;
				for (State state : machine.getStates()) {
					if (state.contains(x,y)) {
						target = state;
						break;
					}
				}
				if (target == null) {
					points.add(new GridPoint(x,y));
					editArea.repaint();
					return;
				}

				// otherwise create new transition
				State fromState = on;
				if (fromState != null) {
					makeTransition(fromState,target,points);
					fromState.setHighlight(false);
				}
				points.clear();
				setDrawState(DrawState.idle);
				editArea.repaint();
				return;
			}

		} // end of mousePressed method

		/**
		 * Build, fill in and add a transition from one state to another.
		 * The headless model builds and validates the transition
		 * ({@link State#buildTransition}/{@link State#addTransition}); this
		 * runs the condition dialog in between.
		 *
		 * @param from The state the transition is from.
		 * @param to The state the transition is to.
		 * @param points The midpoints (the last one is not used).
		 */
		private void makeTransition(State from, State to, Vector<GridPoint> points) {

			State.Transition newTrans = from.buildTransition(to, points);
			if (newTrans == null) {
				return;
			}

			// get condition info
			CreateTrans ct = new CreateTrans(newTrans,from);

			// if it wasn't canceled, validate and add it
			if (!ct.wasCancelled()) {
				from.addTransition(newTrans);
			}
		} // end of makeTransition method

		/**
		 * Create a new state.
		 */
		private void createNewState() {

			CreateState cs = new CreateState(machine,"Create","");
			if (cs.wasCancelled()) {
				return;
			}
			State state = new State(machine,cs.getName(),SwingTextMetrics.forGraphics(editArea.getGraphics()));

			// display it at the current mouse position (if possible)
			Point p = editArea.getMousePosition();
			if (p == null) {
				int d = Geometry.STATE_DIAMETER;
				state.moveTo(-d,-d);	// anywhere off screen
			}
			else {
				state.moveTo(p.x,p.y);
			}
			state.setHighlight(true);
			if (machine.getStates().isEmpty()) {
				state.setInitial(true);
			}
			machine.getStates().add(state);
			selected.clear();
			selected.add(state);
			setDrawState(DrawState.created);
			editArea.repaint();
			return;
		} // end of createNewState method

		/**
		 * React to mouse released events.
		 *
		 * @param event The mouse event.
		 */
		@Override
		public void mouseReleased(MouseEvent event) {

			if (drawState == DrawState.moving || drawState == DrawState.placing) {

				// check for overlap
				boolean overlaps = false;
				for (State sel : selected) {
					for (State st : machine.getStates()) {
						if (sel != st && sel.overlaps(st)) {
							overlaps = true;
						}
					}
				}
				if (overlaps) {
					if (drawState == DrawState.placing)
						return;
					for (State sel : selected) {
						sel.restorePosition();
					}
				}

				// finish up
				selected.clear();
				setDrawState(DrawState.idle);
				editArea.repaint();
				return;
			}

			// if selecting...
			if (drawState == DrawState.selecting) {

				// if nothing selected, return to idle
				if (selected.isEmpty()) {
					selrect = null;
					setDrawState(DrawState.idle);
					editArea.repaint();
					return;
				}

				// shrink selected rectangle
				selrect = null;
				for (State state : selected) {
					if (selrect == null) {
						selrect = AwtGeom.awt(state.getRect());
					}
					else {
						selrect.add(AwtGeom.awt(state.getRect()));
					}
				}
				setDrawState(DrawState.selected);
				repaint();
				return;
			}

			// if moving a point
			if (drawState == DrawState.pointMoving) {
				setDrawState(DrawState.idle);
			}
		} // end of mouseReleased method

		/**
		 * React to mouse moved events.
		 *
		 * @param event The mouse event.
		 */
		@Override
		public void mouseMoved(MouseEvent event) {

			// get info
			int oldx = x;
			int oldy = y;
			x = event.getX();
			y = event.getY();

			// if just moving cursor around...
			if (drawState == DrawState.idle) {

				// highlight state cursor is over, unhighlight the rest
				for (State state : machine.getStates()) {

					if (state.contains(x,y)) {
						state.setHighlight(true);
					}
					else {
						state.setHighlight(false);
					}

					// highlight any transition points from this state too
					state.highlightTransPoints(x,y);

					// highlight any transitions from this state too
					state.highlightTrans(x,y);
				}
				editArea.repaint();
				return;
			}

			// if just created
			if (drawState == DrawState.created ) {
				Point p = editArea.getMousePosition();
				if (p == null) {
					p = new Point(x,y);
				}
				oldx = p.x;
				oldy = p.y;
				for (State state : selected) {
					state.moveTo(p.x,p.y);
				}
				setDrawState(DrawState.placing);
			}

			// if placing or moving
			if (drawState == DrawState.placing || drawState == DrawState.moving) {
				for (State state : selected) {
					state.move(x-oldx,y-oldy,selected);
				}
				editArea.repaint();
			}

			// if making a transition
			if (drawState == DrawState.newTrans) {

				// update position of last point in points list
				points.set(points.size()-1, new GridPoint(x,y));

				// highlight state if under cursor
				for (State state : machine.getStates()) {
					if (state == on)
						continue;
					state.setHighlight(false);
					if (state.contains(x,y)) {
						state.setHighlight(true);
						break;
					}
				}
				repaint();
			}
		} // end of mouseMoved method

		/**
		 * React to mouse dragged events.
		 *
		 * @param event The mouse event.
		 */
		@Override
		public void mouseDragged(MouseEvent event) {

			// if moving
			if (drawState == DrawState.moving) {

				// get info
				int oldx = x;
				int oldy = y;
				x = event.getX();
				y = event.getY();

				// move all states that are selected
				for (State state : selected) {
					state.move(x-oldx,y-oldy,selected);
				}
				editArea.repaint();
				return;
			}

			// if selecting
			if (drawState == DrawState.selecting) {

				// get info
				int nx = event.getX();
				int ny = event.getY();

				// modify selection rectangle
				int rx = Math.min(nx,x);
				int ry = Math.min(ny,y);
				int rw = Math.abs(nx-x);
				int rh = Math.abs(ny-y);
				Rectangle rect = selrect;
				if (rect != null) {
					rect.setBounds(rx,ry,rw,rh);

					// highlight selected elements
					selected.clear();
					for (State state : machine.getStates()) {
						state.setHighlight(false);
						if (state.isInside(AwtGeom.bounds(rect))) {
							selected.add(state);
							state.setHighlight(true);
							state.savePosition();
						}
					}
				}

				// finish up
				editArea.repaint();
				return;
			}

			// if moving a transition point
			if (drawState == DrawState.pointMoving) {

				// get info
				x = event.getX();
				y = event.getY();

				// if there is a state at the point, don't move there
				for (State state : machine.getStates()) {
					if (state.contains(x,y))
						return;
				}

				// move point
				State.Transition mt = movingTrans;
				if (mt != null) {
					mt.moveHighlight(x, y);
				}
				editArea.repaint();
				return;
			}
		} // end of mouseDragged method

		// unused
		/**
		 * Unused mouse listener method.
		 */
		@Override
		public void mouseClicked(MouseEvent event) {}
		/**
		 * Unused mouse listener method.
		 */
		@Override
		public void mouseEntered(MouseEvent event) {}
		/**
		 * Unused mouse listener method.
		 */
		@Override
		public void mouseExited(MouseEvent event) {}

	} // end of StateEditor class

	/**
	 * Create a new state with a name.
	 */
	@SuppressWarnings("serial")
	private static final class CreateState extends ElementFormDialog {

		/** The state machine the new state will belong to. */
		private final StateMachine machine;
		/** The accepted state name. */
		private String name = "";
		/** Input field for the state name. */
		private JTextField nameField = new JTextField(10);
		/** True if the user cancelled this dialog. */
		private boolean stateCancelled;

		/**
		 * Create a new state.
		 *
		 * @param machine The state machine whose states are checked for name clashes.
		 * @param title The partial title of this dialog (e.g., "Create" or "Change");
		 * @param currentName The name the name field starts with.
		 */
		public CreateState(StateMachine machine, String title, String currentName) {

			// set up window title
			super(title + " State",null);

			this.machine = machine;

			// set not cancelled
			stateCancelled = false;

			// set up window
			Container window = getContentPane();

			// set up name field
			JPanel name = new JPanel(new BorderLayout());
			name.add(new JLabel("Name: ",SwingConstants.RIGHT),BorderLayout.WEST);
			name.add(nameField,BorderLayout.CENTER);
			nameField.setText(currentName);
			window.add(name);

			confirmOnEnter(nameField);
			finishDialog();
		} // end of constructor

		/**
		 * Check the state name: present and unique in this machine.
		 */
		@Override
		protected java.util.List<Violation> validateInputs() {

			String newName = nameField.getText().trim();
			if (newName.isEmpty()) {
				return java.util.List.of(new Violation("Missing name",
						nameField));
			}
			for (State state : machine.getStates()) {
				if (state.getName().equals(newName)) {
					return java.util.List.of(new Violation("Duplicate name",
							nameField));
				}
			}
			return java.util.List.of();
		} // end of validateInputs method

		/**
		 * Accept the validated name.
		 */
		@Override
		protected void validateAndAccept() {

			name = nameField.getText().trim();
			dispose();
		} // end of validateAndAccept method

		/**
		 * Cancel state creation.
		 */
		@Override
		protected void cancelDialog() {

			stateCancelled = true;
			dispose();
		} // end of cancelDialog method

		/**
		 * Get the name given to the state.
		 *
		 * @return the name.
		 */
		@Override
		public String getName() {

			return name;
		} // end of getName method

		/**
		 * See if new state dialog was cancelled.
		 *
		 * @return true if it was cancelled, false if not.
		 */
		public boolean wasCancelled() {

			return stateCancelled;
		} // end of wasCancelled method

	} // end of CreateState class

	/**
	 * Dialog to create info about a transition.
	 */
	@SuppressWarnings("serial")
	private static final class CreateTrans extends ElementFormDialog
			implements ActionListener {

		// properties
		/** the transition being created or edited. */
		private State.Transition trans;
		/** the state the transition is from. */
		private State myState;
		/** input field for the condition signal name. */
		private JTextField signalField = new JTextField(10);
		/** button to toggle the condition between = and !=. */
		private JButton equalOrNot = new JButton("=");
		/** input field for the condition value. */
		private JTextField valueField = new JTextField(10);
		/** keypad for entering the condition value. */
		private KeyPad valuePad = new KeyPad(valueField,10,0,this);
		/** input field for the number of bits in the condition signal. */
		private JTextField bitsField = new JTextField(10);
		/** keypad for entering the number of bits. */
		private KeyPad bitsPad = new KeyPad(bitsField,10,1,this);
		/** selects a conditional transition. */
		private JRadioButton conditional = new JRadioButton("conditional");
		/** selects an unconditional transition. */
		private JRadioButton unconditional = new JRadioButton("unconditional");
		/** selects a transition taken when no other condition holds. */
		private JRadioButton otherwise = new JRadioButton("if no other condition");
		/** true if the dialog was cancelled. */
		private boolean cancelled;

		/**
		 * Initialize dialog.
		 *
		 * @param tr The transition to edit.
		 * @param st The state the transition is from.
		 */
		public CreateTrans(State.Transition tr, State st) {

			// set up
			super("Create Transition",null);
			cancelled = false;

			// save working transition and its state
			trans = tr;
			myState = st;

			// get window
			Container window = getContentPane();

			// set up signal/value
			JPanel sigval = new JPanel(new BorderLayout());
			JPanel sig = new JPanel(new GridLayout(3,1));
			sig.add(new JLabel("signal",SwingConstants.CENTER));
			sig.add(signalField);
			sig.add(new JLabel(" "));
			sigval.add(sig,BorderLayout.WEST);
			JPanel other = new JPanel(new BorderLayout());
			JPanel misc = new JPanel(new GridLayout(3,1));
			misc.add(new JLabel(" "));
			misc.add(equalOrNot);
			misc.add(new JLabel("bits: ", SwingConstants.RIGHT));
			other.add(misc,BorderLayout.WEST);
			JPanel values = new JPanel(new GridLayout(3,1));
			values.add(new JLabel("value",SwingConstants.CENTER));
			JPanel val = new JPanel(new BorderLayout());
			val.add(valueField,BorderLayout.CENTER);
			val.add(valuePad,BorderLayout.EAST);
			values.add(val);
			JPanel bits = new JPanel(new BorderLayout());
			bits.add(bitsField,BorderLayout.CENTER);
			bits.add(bitsPad,BorderLayout.EAST);
			values.add(bits);
			other.add(values,BorderLayout.CENTER);
			sigval.add(other,BorderLayout.CENTER);
			window.add(sigval);

			// add radio buttons
			window.add(new JLabel(" "));
			window.add(conditional);
			if (trans.signal.isEmpty()) {
				window.add(unconditional);
			}
			window.add(otherwise);
			ButtonGroup g = new ButtonGroup();
			g.add(conditional);
			g.add(unconditional);
			g.add(otherwise);

			// give initial values
			signalField.setText(tr.signal);
			if (tr.equal) {
				equalOrNot.setText("=");
			}
			else {
				equalOrNot.setText("!=");
			}
			valueField.setText(tr.value+"");
			if (tr.bits == -1)
				bitsField.setText("1");
			else
				bitsField.setText(tr.bits+"");
			if (tr.unconditional) {
				unconditional.setSelected(true);
				signalField.setEditable(false);
				valueField.setEditable(false);
				bitsField.setEditable(false);
			}
			else if (tr.other) {
				otherwise.setSelected(true);
				signalField.setEditable(false);
				valueField.setEditable(false);
				bitsField.setEditable(false);
			}
			else if (!tr.signal.isEmpty()) {
				signalField.setEditable(false);
				bitsField.setEditable(false);
			}

			// add listeners
			equalOrNot.addActionListener(this);
			conditional.addActionListener(this);
			unconditional.addActionListener(this);
			otherwise.addActionListener(this);

			finishDialog();
		} // end of constructor

		/**
		 * Validate the form and set up the transition.
		 */
		@Override
		protected void validateAndAccept() {

			// handle unconditional transition
			if (unconditional.isSelected()) {
				trans.unconditional = true;
				trans.other = false;
				dispose();
				return;
			}

			// handle else transition
			if (otherwise.isSelected()) {
				trans.unconditional = false;
				trans.other = true;
				dispose();
				return;
			}

			// check for missing signal name
			if (signalField.getText().isEmpty()) {
				reject("Missing signal name");
				return;
			}

			// check for invalid signal names
			if (signalField.getText().equals("else")) {
				reject("Invalid signal name");
				return;
			}
			if (signalField.getText().equals("clock")) {
				reject("Invalid signal name");
				return;
			}

			// if input signal already exists, get bit count for it
			int hasBits = -1;
			for (State st : myState.getMachineStates()) {
				for (State.Transition tr : st.getTransitions()) {
					if (tr.signal.equals(signalField.getText())) {
						hasBits = tr.bits;
					}
				}
			}

			// get value and bits from dialog
			int tempValue = 0;
			int tempBits = 0;
			try {
				tempValue = Integer.parseInt(valueField.getText());
				tempBits = Integer.parseInt(bitsField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Invalid numeric value");
				return;
			}

			// make sure bits match with existing input
			if (hasBits > 0 && tempBits != hasBits) {
				reject("Bits don't match with previous signal specification of " +
						hasBits + " bits");
				return;
			}
			int newbits = tempBits;
			if (trans.bits != -1 && trans.bits != newbits) {
				reject("Bits don't match with previous signal specification of " +
						trans.bits + " bits");
				return;
			}

			// make sure value will fit
			if (Math.log(tempValue+1)/Math.log(2) > tempBits) {
				reject("Value too large for number of bits");
				return;
			}

			// finish conditional transition
			trans.unconditional = false;
			trans.other = false;
			trans.signal = signalField.getText();
			if (equalOrNot.getText().equals("=")) {
				trans.equal = true;
			}
			else {
				trans.equal = false;
			}
			trans.value = tempValue;
			trans.bits = newbits;
			dispose();
		} // end of validateAndAccept method

		/**
		 * Cancel the new transition.
		 */
		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

		/**
		 * React to events.
		 *
		 * @param event The event object.
		 */
		@Override
		public void actionPerformed(ActionEvent event) {

			// save equality type check
			if (event.getSource() == equalOrNot) {
				if (equalOrNot.getText().equals("=")) {
					equalOrNot.setText("!=");
				}
				else {
					equalOrNot.setText("=");
				}
			}

			// handle new conditional transition
			else if (event.getSource() == conditional) {
				if (trans.signal.isEmpty()) {
					signalField.setEditable(true);
					valueField.setEditable(true);
					bitsField.setEditable(true);
				}
				else {
					signalField.setEditable(false);
					valueField.setEditable(true);
					bitsField.setEditable(false);

					// find unused value and put in value field
					BitSet used = new BitSet();
					int bits = 0;
					for (State.Transition tran : myState.getTransitions()) {
						bits = trans.bits;
						used.set(tran.value);
					}
					int val = used.nextClearBit(0);
					if (val < 1<<bits) {
						valueField.setText(val+"");
					}
				}
			}

			// handle new unconditional or else transition
			else if (event.getSource() == unconditional ||
					event.getSource() == otherwise) {
				signalField.setEditable(false);
				valueField.setEditable(false);
				bitsField.setEditable(false);
			}

		} // end of actionPerformed method

		/**
		 * See if this dialog was canceled.
		 *
		 * @return true if canceled, false if not.
		 */
		public boolean wasCancelled() {

			return cancelled;
		} // end of wasCancelled method

	} // end of CreateTrans class

	/**
	 * Show outputs, no editing possible.
	 *
	 * @param state The state whose outputs are shown.
	 * @param theDialog The state machine edit dialog this hangs off of.
	 */
	private static void showOuts(State state, JDialog theDialog) {

		// set up dialog
		final JDialog show = new JDialog(theDialog,false);
		Container window = show.getContentPane();
		addOuts(state, window);
		JButton close = new JButton("close window");
		close.setBackground(Color.green);
		window.add(close);
		close.addActionListener(new ActionListener() {
			/**
			 * Dispose the outputs window when the close button is pressed.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				show.dispose();
			}
		});

		// finish up
		show.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		show.pack();
		show.setLocationRelativeTo(theDialog);
		show.setVisible(true);
	} // end of showOuts method

	/**
	 * Add labels for each output signal to a container.
	 * Give the container a grid layout of the appropriate size.
	 *
	 * @param state The state whose outputs are listed.
	 * @param window The container to add to.
	 */
	private static void addOuts(State state, Container window) {

		// set up container
		window.setBackground(Color.WHITE);
		window.setLayout(new GridLayout(state.getOuts().size()+2,1));

		// add title
		JLabel title = new JLabel("Outputs for state: " + state.getName());
		title.setOpaque(true);
		title.setBackground(Color.LIGHT_GRAY);
		window.add(title);

		// add labels, one per output
		for (State.Out out : state.getOuts()) {
			window.add(new JLabel(out.toString(),SwingConstants.CENTER));
		}

	} // end of addOuts method

	/**
	 * Dialog to create outputs for a state.
	 */
	@SuppressWarnings("serial")
	private static final class EditOutputs extends ElementFormDialog
			implements ActionListener {

		// properties
		/** the state whose outputs are edited. */
		private State state;
		/** displays the outputs of this state. */
		private JList<State.Out> outList;
		/** the list model backing the output list. */
		DefaultListModel<State.Out> model;
		/** button to add a new output. */
		private JButton add = new JButton("add new output");
		/** button to delete the selected output. */
		private JButton delete = new JButton("delete selected output");
		/** input field for the output signal name. */
		private JTextField signalField = new JTextField(10);
		/** input field for the output value. */
		private JTextField valueField = new JTextField("1");
		/** keypad for entering the output value. */
		private KeyPad valuePad = new KeyPad(valueField,10,0,this);
		/** input field for the number of bits in the output signal. */
		private JTextField bitsField = new JTextField("1");
		/** keypad for entering the number of bits. */
		private KeyPad bitsPad = new KeyPad(bitsField,10,1,this);
		/** true if the dialog was cancelled. */
		private boolean cancelled;

		/**
		 * Initialize dialog.
		 *
		 * @param state The state whose outputs are edited.
		 */
		public EditOutputs(State state) {

			// set up
			super("Edit Outputs",null);
			cancelled = false;
			this.state = state;

			// get window
			Container window = getContentPane();

			// set up output list
			model = new DefaultListModel<State.Out>();
			for (State.Out output : state.getOuts()) {
				model.addElement(output);
			}
			outList = new JList<State.Out>(model);
			outList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			JScrollPane pane =  new JScrollPane(outList);
			pane.setSize(400,400);
			window.add(pane);

			// set up add/delete buttons
			JPanel addDel = new JPanel(new FlowLayout());
			addDel.add(add);
			addDel.add(delete);
			window.add(addDel);

			// set up signal/value
			JPanel sigval = new JPanel(new BorderLayout());
			JPanel sig = new JPanel(new GridLayout(3,1));
			sig.add(new JLabel("signal",SwingConstants.CENTER));
			sig.add(signalField);
			sig.add(new JLabel(" "));
			sigval.add(sig,BorderLayout.WEST);
			JPanel other = new JPanel(new BorderLayout());
			JPanel misc = new JPanel(new GridLayout(3,1));
			misc.add(new JLabel(" "));
			misc.add(new JLabel("="));
			misc.add(new JLabel("bits: ", SwingConstants.RIGHT));
			other.add(misc,BorderLayout.WEST);
			JPanel values = new JPanel(new GridLayout(3,1));
			values.add(new JLabel("value",SwingConstants.CENTER));
			JPanel val = new JPanel(new BorderLayout());
			val.add(valueField,BorderLayout.CENTER);
			val.add(valuePad,BorderLayout.EAST);
			values.add(val);
			JPanel bits = new JPanel(new BorderLayout());
			bits.add(bitsField,BorderLayout.CENTER);
			bits.add(bitsPad,BorderLayout.EAST);
			values.add(bits);
			other.add(values,BorderLayout.CENTER);
			sigval.add(other,BorderLayout.CENTER);
			window.add(sigval);

			// add listeners
			add.addActionListener(this);
			delete.addActionListener(this);

			finishDialog();
		} // end of constructor

		/**
		 * Close the dialog (outputs are edited in place).
		 */
		@Override
		protected void validateAndAccept() {

			dispose();
		} // end of validateAndAccept method

		/**
		 * React to events.
		 *
		 * @param event The event object.
		 */
		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource() == add) {
				String signal = signalField.getText().trim();
				if (signal.isEmpty()) {
					TellUser.error(this,
							"Missing signal name", "Error");
					return;
				}
				long value;
				int bits;
				try {
					value = Integer.parseInt(valueField.getText());
					bits = Integer.parseInt(bitsField.getText());
				}
				catch (NumberFormatException ex) {
					TellUser.error(this,
							"Value or bits not valid", "Error");
					return;
				}
				State.Out newOut = state.addOutput(signal, value, bits);
				if (newOut != null) {
					model.addElement(newOut);
				}
			}
			else if (event.getSource() == delete) {
				int pos = outList.getSelectedIndex();
				if (pos >= 0) {
					state.deleteOutput(pos);
					model.remove(pos);
				}
			}

		} // end of actionPerformed method

		/**
		 * See if this dialog was cancelled.
		 *
		 * @return true if cancelled, false if not.
		 */
		@SuppressWarnings("unused")
		public boolean wasCancelled() {

			return cancelled;
		} // end of wasCancelled method

	} // end of EditOutputs class

} // end of StateMachineDialog class
