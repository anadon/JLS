package jls.sim;

import jls.*;
import jls.elem.*;
import jls.edit.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.util.concurrent.*;
import java.util.*;

/**
 * Event driven circuit simulator.
 *
 * @author David A. Poplawski
 */
public final class InterractiveSimulator extends Simulator {

	// minimum size for trace window
	private final int SWIDTH = 1000;
	private final int SHEIGHT = 70;

	// properties
	private int stepAmount = 1;
	private long stepEnd = -1;
	private boolean paused = false;
	private Semaphore pauseSem = new Semaphore(0);
	private Thread sim = null;

	// GUI stuff
	private JPanel action = new JPanel(new FlowLayout(FlowLayout.LEFT));
	private JButton start = new JButton("  Start   ");
	private JButton step = new JButton("Step");
	private JButton animate = new JButton("Animate");
	private JButton end = new JButton("End");
	private JButton pause = new JButton("Pause");
	private JButton resume = new JButton("Resume");
	private JButton stop = new JButton("Stop");
	private JButton print = new JButton("Print");
	private JButton help = new JButton("Help");
	private JPanel window = new JPanel();
	private JLabel msg = new JLabel("");
	private JLabel showClock = new JLabel("Time: 0");
	private JTextField tlimit = new JTextField(maxTime+"",7);
	private JPanel statusBar = new JPanel(new BorderLayout());
	private JLabel statusMsg = new JLabel(" ");
	private JLabel statusClock = new JLabel();

	// for animation
	java.util.Timer timer;

	// for showing traces in interactive mode
	private Traces traces = new Traces();
	private Map<Element,Trace> traceMap = new HashMap<Element,Trace>();
	private Map<Wire,Trace> wireMap = new HashMap<Wire,Trace>();
	private int scaleFactor = 1;
	private Set<MemTrace> memTraces = new HashSet<MemTrace>();
	private Point windowLoc;
	private int displayBase = 10;

	/**
	 * Create a new Simulator object.
	 */
	public InterractiveSimulator() {

		// save a reference to itself
		me = this;

		// if JLSInfo.batch, no GUI
		if (JLSInfo.batch)
			return;

		// set up GUI
		window.setLayout(new BorderLayout());

		// toolbar and info
		JPanel barInfo = new JPanel(new GridLayout(2,1));

		// toolbar
		JPanel toolBar = new JPanel(new BorderLayout());

		start.setBackground(Color.green);
		start.setToolTipText("simulate until done, paused or stopped");
		action.add(start);

		step.setBackground(Color.yellow);
		step.setToolTipText("run simulator for step amount of time");
		action.add(step);

		animate.setBackground(Color.cyan);
		animate.setToolTipText("repeat step every second");
		action.add(animate);

		end.setBackground(Color.pink);
		end.setToolTipText("terminate animation");

		pause.setBackground(Color.yellow);
		pause.setToolTipText("pause running simulation");

		resume.setBackground(Color.green);
		resume.setToolTipText("resume paused simulation");

		stop.setBackground(Color.red);
		stop.setToolTipText("stop running simulation");

		print.setBackground(Color.white);
		print.setToolTipText("print trace window");

		if (!JLSInfo.isApplet) {
			action.add(print);
		}

		if (JLSInfo.hb != null) {
			JLSInfo.hb.enableHelpOnButton(help,"inter.sim",null);
		}
		else {
			Util.noHelp(help);
		}
		action.add(help);
		toolBar.add(action,BorderLayout.WEST);

		// simulation parameters
		JPanel simParams = new JPanel(new FlowLayout());

		// step amount
		JLabel stepLabel = new JLabel("Step: ");
		simParams.add(stepLabel);
		final JTextField stepField = new JTextField("1",6);
		AbstractDocument d = (AbstractDocument)(stepField.getDocument());
		d.setDocumentFilter(new TextFilter(stepField));
		simParams.add(stepField);

		// time limit
		JLabel timeLimitLabel = new JLabel("Time Limit: ");
		simParams.add(timeLimitLabel);
		d = (AbstractDocument)(tlimit.getDocument());
		TextFilter tlFilter = new TextFilter(tlimit);
		tlFilter.setMax(Integer.MAX_VALUE);
		d.setDocumentFilter(tlFilter);
		simParams.add(tlimit);

		toolBar.add(simParams,BorderLayout.EAST);

		// show trace buttton
		JLabel showTrace = new JLabel("<html>Drag divider up to see signal traces</html>",
				SwingConstants.CENTER);
		toolBar.add(showTrace,BorderLayout.CENTER);

		barInfo.add(toolBar);

		// message/clock area
		JPanel bottom = new JPanel(new BorderLayout());
		bottom.setBackground(Color.yellow);
		bottom.add(msg,BorderLayout.WEST);
		bottom.add(showClock,BorderLayout.EAST);
		barInfo.add(bottom);

		window.add(barInfo,BorderLayout.NORTH);

		// trace parameters
		final JPanel params = new JPanel();
		params.setLayout(new BoxLayout(params,BoxLayout.Y_AXIS));

		// scale factor
		JLabel sf = new JLabel("Scale Factor");
		sf.setAlignmentX(Component.CENTER_ALIGNMENT);
		params.add(sf);
		final JTextField scaleField = new JTextField("1",10);
		d = (AbstractDocument)(scaleField.getDocument());
		d.setDocumentFilter(new TextFilter(scaleField));
		scaleField.setMaximumSize(scaleField.getPreferredSize());
		params.add(scaleField);
		JButton getScale = new JButton("Apply");
		getScale.setAlignmentX(Component.CENTER_ALIGNMENT);
		params.add(getScale);
		getScale.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						if (scaleField.getText().length() == 0)
							scaleFactor = 1;
						else
							scaleFactor = Math.max(1,Integer.parseInt(scaleField.getText()));
						scaleField.setText(scaleFactor+"");
						traces.setScaleFactor();
						if (now != 0)
							traces.draw();
					} // end of actionPerformed method
				} // end of ActionListener class
		);
		scaleField.addActionListener(getScale.getActionListeners()[0]);

		// base
		params.add(new JLabel(" "));
		JLabel baseLabel = new JLabel("Display Base");
		baseLabel.setAlignmentX((float)0.5);
		params.add(baseLabel);
		JPanel baseButtons = new JPanel(new GridLayout(1,3));
		final JButton b2 = new JButton("2");
		final JButton b10 = new JButton("10");
		final JButton b16 = new JButton("16");
		baseButtons.add(b2);
		baseButtons.add(b10);
		baseButtons.add(b16);
		b2.setBackground(Color.lightGray);
		b10.setBackground(Color.gray);
		b16.setBackground(Color.lightGray);
		ActionListener blist = new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				int newBase = 0;
				b2.setBackground(Color.lightGray);
				b10.setBackground(Color.lightGray);
				b16.setBackground(Color.lightGray);
				if (event.getSource() == b2) {
					newBase = 2;
					b2.setBackground(Color.GRAY);
				}
				else if (event.getSource() == b10) {
					newBase = 10;
					b10.setBackground(Color.GRAY);
				}
				else if (event.getSource() == b16) {
					newBase = 16;
					b16.setBackground(Color.GRAY);
				}
				displayBase = newBase;
				traces.setBase(newBase);
			}
		};
		b2.addActionListener(blist);
		b10.addActionListener(blist);
		b16.addActionListener(blist);
		baseButtons.setMaximumSize(new Dimension(1000,25));
		params.add(baseButtons);

		window.add(params,BorderLayout.EAST);

		// trace area
		final JScrollPane spane = new JScrollPane(traces);
		window.add(spane,BorderLayout.CENTER);

		// handle window events
		window.addComponentListener(
				new ComponentAdapter() {
					public void componentResized(ComponentEvent event) {
						window.doLayout();
						traces.resize(traces.getWidth());
						windowLoc = window.getLocationOnScreen();
						windowLoc.translate(0,window.getHeight());
					}
					public void componentMoved(ComponentEvent event) {
						windowLoc = window.getLocationOnScreen();
						windowLoc.translate(0,window.getHeight());
					}
				}
		);

		// handle start button push
		start.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						action.removeAll();
						action.add(pause);
						action.add(stop);
						action.validate();
						if (scaleField.getText().length() == 0)
							scaleFactor = 1;
						else
							scaleFactor = Math.max(1,Integer.parseInt(scaleField.getText()));
						scaleField.setText(scaleFactor+"");
						traces.setScaleFactor();
						setMaxTime();
						runSim();
					}
				}
		);

		// handle step button push
		step.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						action.removeAll();
						action.add(resume);
						action.add(step);
						action.add(animate);
						action.add(stop);
						action.add(print);
						action.add(help);
						action.validate();
						if (scaleField.getText().length() == 0)
							scaleFactor = 1;
						else
							scaleFactor = Math.max(1,Integer.parseInt(scaleField.getText()));
						scaleField.setText(scaleFactor+"");
						traces.setScaleFactor();
						if (stepField.getText().length() == 0)
							stepAmount = 1;
						else
							stepAmount = Math.max(1,Integer.parseInt(stepField.getText()));
						stepField.setText(stepAmount+"");
						setMaxTime();
						if (sim == null) {
							stepEnd = stepAmount;
							runSim();
						}
						else {
							if (!paused) {
								return;
							}
							paused = false;
							pauseSem.release();
							if (stepEnd < 0)
								stepEnd = stepAmount;
							else
								stepEnd += stepAmount;
						}
					}
				}
		);

		// handle animate button push
		animate.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent event) {

						// set up buttons
						action.removeAll();
						action.add(end);
						action.validate();

						// set up step info
						if (scaleField.getText().length() == 0)
							scaleFactor = 1;
						else
							scaleFactor = Math.max(1,Integer.parseInt(scaleField.getText()));
						scaleField.setText(scaleFactor+"");
						traces.setScaleFactor();
						if (stepField.getText().length() == 0)
							stepAmount = 1;
						else
							stepAmount = Math.max(1,Integer.parseInt(stepField.getText()));
						stepField.setText(stepAmount+"");
						setMaxTime();

						// create step object (TimerTask)
						TimerTask tc = new TimerTask() {

							public void run() {
								
								if (now >= maxTime) {
									timer.cancel();
								}

								// do a step
								if (sim == null) {
									stepEnd = stepAmount;
									runSim();
								}
								else {
									if (!paused) {
										return;
									}
									paused = false;
									pauseSem.release();
									if (stepEnd < 0)
										stepEnd = stepAmount;
									else
										stepEnd += stepAmount;
								}
							}
						}; // end of TimerTask class

						// run timer task every second
						timer = new java.util.Timer();
						timer.scheduleAtFixedRate(tc, 0, 1000);
					}
				}
		);

		end.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						action.removeAll();
						action.add(animate);
						action.add(resume);
						action.add(stop);
						action.add(print);
						action.add(help);
						action.validate();
						if (timer != null) {
							timer.cancel();
							timer = null;
						}
						step.doClick();
					}
				}
		);

		pause.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						action.removeAll();
						action.add(resume);
						action.add(animate);
						action.add(stop);
						action.add(print);
						action.add(help);
						action.validate();
						paused = true;
						stepEnd = -1; // kill any stepping
					}
				}
		);

		resume.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						action.removeAll();
						action.add(pause);
						action.add(stop);
						action.validate();
						if (scaleField.getText().length() == 0)
							scaleFactor = 1;
						else
							scaleFactor = Math.max(1,Integer.parseInt(scaleField.getText()));
						scaleField.setText(scaleFactor+"");
						traces.setScaleFactor();
						paused = false;
						stepEnd = -1;  // kill any stepping
						pauseSem.release();
					}
				}
		);

		stop.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						stop();
					}
				}
		);

		print.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						printTraces();
					}
				}
		);

		// set up panel
		window.setMinimumSize(new Dimension(SWIDTH,SHEIGHT));

	} // end of constructor

	/**
	 * Get the simulator JPanel.
	 * 
	 * @return the JPanel that displays the simulator
	 */
	public JPanel getWindow() {

		return window;
	} // end of getWindow method

	/**
	 * Set the time limit from the tlimit text field.
	 */
	public void setMaxTime() {

		if (tlimit.getText().length() == 0)
			maxTime = 1;
		else
			maxTime = Math.max(1,Integer.parseInt(tlimit.getText()));
		tlimit.setText(maxTime+"");
	} // end of setMaxTime method

	/**
	 * Enqueue an event.
	 *
	 * @param event The event to enqueue.
	 */
	public void post(SimEvent event) {

		/*
		 System.out.print(now + ": post called for element " +
		 ((LogicElement)(event.getCallBack())).getName());
		 */
		if (dupCheck.add(event)) {
			// System.out.print(" - added to queue");
			eventQueue.add(event);
		}
		// System.out.println();
	} // end of post method

	/**
	 * Run the simulator in interactive mode.
	 */
	public void runSim() {

		// do nothing if no circuit
		if (circuit == null)
			return;

		// if simulator is already running, ignore
		if (sim != null) {
			return;
		}

		// reset clock and empty eventQueue
		now = 0;
		stopping = false;
		paused = false;
		eventQueue.clear();
		dupCheck.clear();

		// create test signal element
		TestGen gen = null;
		if (testFileName != null) {
			gen = new TestGen(circuit);
			gen.setFile(testFileName);
		}

		// initialize all input points
		initInputs(circuit);

		// initialize all elements
		for (Element el : circuit.getElements()) {
			if (el instanceof LogicElement) {
				LogicElement lel = (LogicElement)el;
				lel.initSim(me);
			}
		}

		// initialize test generator, if there is one
		if (gen != null) {
			gen.initSim(me);
		}

		// find all probes and watched elements (if not JLSInfo.batch mode)
		// and set up trace window
		if (!JLSInfo.batch) {
			traces.clear();
			traceMap.clear();
			wireMap.clear();
			for (MemTrace mtr : memTraces) {
				mtr.dispose();
			}
			memTraces.clear();
			findTraces(circuit);
			traces.setup();
			traces.draw();
		}

		// create new thread for the simulator
		sim = new Thread("Runner") {

			public void run() {

				// disable circuit editor (if one)
				Editor ed = circuit.getEditor();
				if (ed != null)
					ed.enableEditor(false); // turn off listeners

				// event loop
				while (!stopping && !eventQueue.isEmpty() && now <= maxTime) {

					// check for being paused
					if (paused) {
						stepEnd = now;
						traces.draw();
						for (MemTrace mtr : memTraces) {
							mtr.update();
						}
						msg.setText("Simulation Paused");
						if (ed != null)
							ed.enableEditor(true);
						try {
							pauseSem.acquire();
						}
						catch (InterruptedException ex) {}
						if (ed != null)
							ed.enableEditor(false);
						continue;
					}

					// check for stepping
					// peek at next event
					SimEvent event = eventQueue.peek();
					long when = event.getTime();

					// if after step end time ... (can't happen in JLSInfo.batch mode)
					if (stepEnd != -1 && when > stepEnd) {

						// update current time
						now = stepEnd;
						showClock.setText("Time: "+now);

						// update traces
						traces.draw();
						for (MemTrace mtr : memTraces) {
							mtr.update();
						}

						// redraw circuit
						if (ed != null)
							ed.repaint();

						paused = true;
						continue;
					}

					if (!JLSInfo.batch)
						msg.setText("Simulation Running");

					// get the next event
					event = eventQueue.poll();
					dupCheck.remove(event);

					// update clock
					now = event.getTime();

					// quit if after time limit
					if (now > maxTime) {
						now = maxTime;
						break;
					}

					// update clock display
					if (!JLSInfo.batch) {
						showClock.setText("Time: "+now);
						window.validate();
					}

					// make the event happen
					event.getCallBack().react(now,me,event.getTodo());

					// trace it (interactive)
					if (!JLSInfo.batch) {

						// handle watched elements
						LogicElement el = (LogicElement)event.getCallBack();
						Trace tr = traceMap.get(el);
						if (tr != null) {
							tr.addValue(el.getCurrentValue(),now);
						}

						// handle probes
						for (Wire wire : wireMap.keySet()) {
							tr = wireMap.get(wire);
							tr.addValue(wire.getValue(),now);
						}
					}

					updateStatusBar();
				} // end of event loop
				
				// kill timer if there is one
				if (timer != null) {
					timer.cancel();
				}

				// update clock display
				if (!JLSInfo.batch) {
					showClock.setText("Time: "+now);
					window.validate();
				}

				// leave a little extra room at the end
				now += 10*scaleFactor;

				// turn on listeners
				if (ed != null)
					ed.enableEditor(true);

				// draw the traces
				if (!JLSInfo.batch) {
					traces.draw();
					for (MemTrace mtr : memTraces) {
						mtr.update();
					}
				}

				// redraw the circuit
				if (ed != null)
					ed.repaint();

				// display reason for stopping
				String reason;
				if (stopping)
					reason = "Simulation Stopped";
				else if (now >= maxTime)
					reason = "Simulation Time Limit";
				else if (eventQueue.size() == 0)
					reason = "Simulation: No More Activity";
				else
					reason = "Simulation Complete";
				stopping = true;

				if (JLSInfo.batch && JLSInfo.frame == null && !JLSInfo.isApplet)
					System.out.println(reason + " at " + now);
				else
					msg.setText(reason);

				updateStatusBar();
				action.removeAll();
				action.add(start);
				action.add(step);
				action.add(animate);
				action.add(print);
				action.add(help);
				action.validate();

				// clear up for next simulation
				sim = null;
			} // end of run method

		}; // end of sim Thread class

		// start up the simulator
		if (!JLSInfo.batch || JLSInfo.frame != null || JLSInfo.isApplet)
			msg.setText("Simulation Running");
		sim.start();
		if (JLSInfo.batch && JLSInfo.frame == null && !JLSInfo.isApplet) {
			try {
				sim.join();
			}
			catch(InterruptedException ex) {}
		}

	} // end of runSim method

	/**
	 * Pause or resume the simulation, if there is one.
	 * Called by the pause element.
	 *
	 * @param which True to pause the simulation, false to resume it.
	 */
	public void pause(boolean which) {

		if (sim != null) {
			action.removeAll();
			action.add(resume);
			action.add(step);
			action.add(animate);
			action.add(stop);
			action.add(print);
			action.add(help);
			action.validate();
			if (timer != null)
				timer.cancel();
			paused = which;
			stepEnd = -1; // kill any stepping
		}
	} // end of pause method

	/**
	 * Stop (end) the simulation, if there is one.
	 */
	public void stop() {

		if (sim != null) {
			/*
			action.removeAll();
			action.add(start);
			action.add(step);
			action.add(animate);
			action.add(print);
			action.add(help);
			action.validate();
			*/
			stopping = true;
			stepEnd = -1; // kill any stepping
			pauseSem.release();
		}
	} // end of stop method

	/**
	 * Find all probes and watched elements, add them to Traces window.
	 * If an element is not watched, reset its trace position.
	 * Descends into subcircuits recursively.
	 * 
	 * @param circ The circuit to find the traces in.
	 */
	private void findTraces(Circuit circ) {

		Point wLoc = new Point(windowLoc);
		for (Element element : circ.getElements()) {

			// if a wire, check for a probe
			if (element instanceof Wire) {
				Wire wire = (Wire)element;
				if (wire.hasProbe()) {
					String name = wire.getEnd().getFullName() + wire.getProbe();
					Trace tr = new Trace(name,element,wire.getBits(),traces.getWidth(),traces);
					tr.setBase(displayBase);
					traces.addTrace(tr);
					tr.addValue(wire.getValue(),now);
					wireMap.put(wire,tr);
				}
			}

			// if a subcircuit, recursively find all traces in it
			else if (element instanceof SubCircuit) {
				SubCircuit sub = (SubCircuit)element;
				Circuit c = sub.getSubCircuit();
				findTraces(c);
			}

			// if a watched element
			else if (element instanceof LogicElement) {
				LogicElement el = (LogicElement)element;
				if (el.isWatched()) {

					// set up trace window
					Trace tr = new Trace(el.getFullName(),el,el.getBits(),
							traces.getWidth(), traces);
					tr.setBase(displayBase);
					traces.addTrace(tr);
					tr.addValue(el.getCurrentValue(),now);
					traceMap.put(el,tr);

					// set up memory trace if it is a memory element
					if (el instanceof Memory) {
						Memory mem = (Memory)el;
						MemTrace mtr = new MemTrace(mem);
						Point loc = new Point(wLoc);
						loc.translate(0,-mtr.getHeight());
						mtr.showit(loc);
						memTraces.add(mtr);
						wLoc.translate(mtr.getWidth(),0);
					}
				}
				else {
					el.setTracePosition(-1);
				}
			}

			// if a subcircuit, recursively find all traces in it
			else if (element instanceof SubCircuit) {
				SubCircuit sub = (SubCircuit)element;
				Circuit c = sub.getSubCircuit();
				findTraces(c);
			}
		}
	} // end of findTraces method

	/**
	 * Get the status bar (containing message and clock).
	 * 
	 * @return the status bar.
	 */
	public JPanel getStatusBar() {

		statusBar.add(statusMsg,BorderLayout.WEST);
		statusBar.add(statusClock,BorderLayout.EAST);
		statusBar.setBackground(Color.yellow);
		return statusBar;
	} // end of getStatusBar method

	/**
	 * Update the status bar.
	 * Grabs current values of msg and showClock and puts into status bar labels.
	 */
	private void updateStatusBar() {

		if (JLSInfo.batch && JLSInfo.frame == null && !JLSInfo.isApplet)
			return;
		statusMsg.setText(msg.getText());
		statusClock.setText(showClock.getText());
	} // end of updateStatusBar method

	/**
	 * Set up to print traces from interactive mode.
	 */
	private void printTraces() {

		// set up printer job
		PrinterJob job = PrinterJob.getPrinterJob();
		PageFormat format = job.defaultPage();
		format.setOrientation(PageFormat.LANDSCAPE);
		Book book = new Book();

		Printable pr = new Printable() {

			public int print(Graphics g, PageFormat format, int pagenum) {

				if (pagenum > 0) return Printable.NO_SUCH_PAGE;
				Graphics2D gg = (Graphics2D)g;
				gg.translate(format.getImageableX(),format.getImageableY());
				double width = format.getImageableWidth();
				double height = format.getImageableHeight();
				Dimension d = traces.getSize();
				double scale = 1.0;
				if (d.width > width) {
					scale = 1.0*width/d.width;
				}
				if (d.height > height) {
					scale = Math.min(scale,1.0*height/d.height);
				}
				gg.scale(scale,scale);
				traces.paint(gg);
				return Printable.PAGE_EXISTS;
			} // end of print method
		};

		book.append(pr,format);
		job.setPageable(book);

		// show dialog, and if ok, print
		if (job.printDialog()) {
			try {
				job.print();
			}
			catch (PrinterException ex) {
				System.out.println("printing error: " + ex.getMessage());
			}
		}
	} // end of printTraces method

	/**
	 * Trace window.
	 */
	public class Traces extends JPanel implements MouseMotionListener {

		// properties
		private java.util.List<Trace> traceList = new LinkedList<Trace>();
		private java.util.List<Trace> newList = new LinkedList<Trace>();
		private int nameSpace;

		/**
		 * Set up the window.
		 */
		public Traces() {

			setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
			addMouseMotionListener(this);
		} // end of constructor

		/**
		 * Clear the traces.
		 */
		public void clear() {

			// clear display
			removeAll();
			nameSpace = 0;
			newList.clear();

			// add header (with a dummy Text element with 0 position)
			Text t = new Text(null);
			t.setTracePosition(0);
			Header head = new Header(getWidth(),t,this);
			addTrace(head);
		} // end of clear method

		/**
		 * Add a trace to the window.
		 *
		 * @param tr The trace to add.
		 */
		public void addTrace(Trace tr) {

			newList.add(tr);
		} // end of addTrace method

		/**
		 * No more traces expected, so lay out the window.
		 */
		public void setup() {

			// build list using predefined order of elements
			SortedMap<Integer,Trace> map = new TreeMap<Integer,Trace>();
			java.util.List<Trace> posList = new LinkedList<Trace>();

			// first add elements that had a position to sorted map
			for (Trace tr : newList) {
				if (tr.getElement().getTracePosition() != -1)
					map.put(tr.getElement().getTracePosition(),tr);
			}

			// now add those elements to the display list
			for (int t : map.keySet()) {
				Trace tr = map.get(t);
				posList.add(tr);
			}

			// then add any elements that didn't have a position on the end
			// of the display list
			for (Trace tr : newList) {
				if (tr.getElement().getTracePosition() == -1) {
					posList.add(tr);
				}
			}

			// add traces to display, figure out space for longest name, and update
			// trace positions in elements
			int pos = 0;
			traceList.clear();
			removeAll();
			for (Trace tr : posList) {

				// add to list and window
				traceList.add(tr);
				add(tr);

				// check for longer name
				FontMetrics fm = tr.getGraphics().getFontMetrics();
				int nameLength = fm.stringWidth(tr.getName());
				if (nameLength > nameSpace)
					nameSpace = nameLength;
				tr.setScaleFactor(scaleFactor);

				// set new position
				tr.getElement().setTracePosition(pos);

				pos += 1;

			}

			// make window look right
			validate();
		} // end of setup method

		/**
		 * Get the maximum space needed for element names.
		 *
		 * @return The space (pixels).
		 */
		public int getNameSpace() {

			return nameSpace;
		} // end of getNameSpace method

		/**
		 * Repaint all the trace objects.
		 */
		private void draw() {

			repaint();
			for (Trace tr : traceList) {
				tr.commit(now);
				tr.repaint();
			}
		} // end of draw method

		/**
		 * Set trace scale factor in all the trace objects.
		 */
		private void setScaleFactor() {

			for (Trace tr : traceList) {
				tr.setScaleFactor(scaleFactor);
			}
		} // end of setScaleFactor method

		/**
		 * Set base in all trace object.
		 * 
		 * @param base The new base.
		 */
		private void setBase(int base) {

			for (Trace tr : traceList) {
				tr.setBase(base);
			}
		} // end of setBase method

		/**
		 * Tell all the elements what the new window width is.
		 *
		 * @param width The new width.
		 */
		private void resize(int width) {

			for (Trace tr : traceList) {
				tr.resize(width);
			}
		} // end of resize method

		/**
		 * Move a trace up or down.
		 * Also rebuild trace position map.
		 *
		 * @param tr The trace to move
		 * @param dir The direction (-2=top, -1=up, 1=down, 2=bottom).
		 */
		public void move(Trace trace, int dir) {

			// get current position
			int pos = traceList.indexOf(trace);

			// reposition in list
			if (dir == -2) {
				traceList.remove(pos);
				traceList.add(1,trace); // move to top
			}
			else if (dir == 2) {
				traceList.remove(pos);
				traceList.add(trace); // move to bottom
			}
			else {
				if (pos+dir < 1 || pos+dir >= traceList.size())
					return;
				traceList.remove(pos);
				traceList.add(pos+dir,trace);
			}

			// re-layout and set new trace positions in elements
			removeAll();
			pos = 0;
			for (Trace tr : traceList) {
				add(tr);
				tr.getElement().setTracePosition(pos);
				pos += 1;
			}
			validate();
		} // end of move method

		/**
		 * Tell all the traces that the mouse has moved in the trace window,
		 * but only if the simulator is paused or stopped.
		 *
		 * @param event The event containing the new position of the mouse.
		 */
		public void mouseMoved(MouseEvent event) {

			if (!paused && !stopping)
				return;
			int x = event.getX();
			for (Trace tr : traceList) {
				tr.mouseMoved(x);
			}
		} // end of mouseMoved method

		// unused
		public void mouseDragged(MouseEvent event) {}

	} // end of Traces class

	/**
	 * A header for the traces, will display the slider and the time the
	 * slider indicates.
	 */
	private class Header extends Trace {

		/**
		 * Call superclass constructor, supply null name.
		 */
		public Header(int width, Element el, InterractiveSimulator.Traces parent) {

			super("",el,0,width,parent);
		} // end of constructor

		/**
		 * Draw the slider and the time the slider indicates.
		 */
		public void paintComponent(Graphics g) {

			super.paintComponent(g);
			int width = getWidth()-parent.getNameSpace()-10;
			long time = now-(width-sliderPos)*scaleFactor;
			if (time >= 0 && time <= now) {
				g.setColor(Color.gray);
				g.drawString(time+"",sliderPos,2*HEIGHT/3);
			}
		} // end of paintComponent method

		/**
		 * Override superclass method to ignore events.
		 */
		public void mousePressed(MouseEvent event) {}

	} // end of Header class

} // end of InterractiveSimulator class

