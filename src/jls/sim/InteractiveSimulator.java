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

import org.jspecify.annotations.Nullable;

/**
 * Event driven circuit simulator.
 *
 * @author David A. Poplawski
 */
public final class InteractiveSimulator extends Simulator {

	// minimum size for trace window
	/** The trace window's minimum width in pixels. */
	private final int SWIDTH = 1000;
	/** The trace window's minimum height in pixels. */
	private final int SHEIGHT = 70;

	// control state shared between the EDT, the animation timer thread,
	// and the sim thread: volatile so Stop/Pause/Step cannot be missed
	// under JIT hoisting (issue #49, finding H7)
	/** How many time units one Step press advances. */
	private volatile int stepAmount = 1;
	/** The simulation time the current step run pauses at, or -1. */
	private volatile long stepEnd = -1;
	/** Whether the simulation is paused. */
	private volatile boolean paused = false;
	/** The sim thread blocks on this while paused. */
	private Semaphore pauseSem = new Semaphore(0);
	/** The running simulation thread; null between runs (issue #93). */
	private volatile @Nullable Thread sim = null;
	/**
	 * Suppress UI updates for this run only ("run in background");
	 * replaces the racy JLSInfo.batch toggle (issue #49, finding M15).
	 */
	private volatile boolean quiet = false;

	// GUI stuff
	/** The button row at the top of the simulator window. */
	private JPanel action = new JPanel(new FlowLayout(FlowLayout.LEFT));
	/** Starts a simulation run. */
	private JButton start = new JButton("  Start   ");
	/** Advances the simulation by the step amount. */
	private JButton step = new JButton("Step");
	/** Runs the simulation with periodic display updates. */
	private JButton animate = new JButton("Animate");
	/** Runs the simulation to the time limit without animation. */
	private JButton end = new JButton("End");
	/** Pauses the running simulation. */
	private JButton pause = new JButton("Pause");
	/** Resumes a paused simulation. */
	private JButton resume = new JButton("Resume");
	/** Stops the running simulation. */
	private JButton stop = new JButton("Stop");
	/** Prints the traces. */
	private JButton print = new JButton("Print");
	/** Opens the simulator help topic. */
	private JButton help = new JButton("Help");
	/** The content pane the trace rows live in. */
	private JPanel window = new JPanel();
	/** The message label in the button row. */
	private JLabel msg = new JLabel("");
	/** The current-time display in the button row. */
	private JLabel showClock = new JLabel("Time: 0");
	/** The editable simulation time limit. */
	private JTextField tlimit = new JTextField(maxTime+"",7);
	/** The status bar at the bottom of the simulator window. */
	private JPanel statusBar = new JPanel(new BorderLayout());
	/** The status bar's message area. */
	private JLabel statusMsg = new JLabel(" ");
	/** The status bar's simulation-time display. */
	private JLabel statusClock = new JLabel();

	// for animation
	/** The animation timer; null unless animating (issue #93). */
	volatile java.util.@Nullable Timer timer;

	// for showing traces in interactive mode
	/** The container of all trace rows. */
	private Traces traces = new Traces();
	/** Per watched element, its trace row. */
	private Map<Element,Trace> traceMap = new HashMap<Element,Trace>();
	/** Per watched wire, its trace row. */
	private Map<Wire,Trace> wireMap = new HashMap<Wire,Trace>();
	/** Horizontal scale: simulation time units per pixel. */
	private int scaleFactor = 1;
	/** The open memory-trace popup windows. */
	private Set<MemTrace> memTraces = new HashSet<MemTrace>();
	/** The numeric base trace values are labeled in (2, 10, or 16). */
	private int displayBase = 10;

	/**
	 * Create a new Simulator object.
	 *
	 * @jls.testedby jls.sim.InteractiveSimulatorFieldTest#buildSimulatorPanel()
	 * @jls.testedby jls.sim.TraceRetentionTest#parent()
	 * @jls.testedby jls.sim.TraceWindowingTest#parent()
	 * @jls.testedby jls.ui.EditorGestureSupport#EditorGestureSupport()
	 */
	public InteractiveSimulator() {

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

		action.add(print);

		Help.enableHelpOnButton(help, "inter.sim");
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
					/**
					 * Apply the typed scale factor to the traces, redrawing
					 * an in-progress run.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						scaleFactor = NumericField.parse(window,scaleField,
								1,scaleFactor,"Scale factor");
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
			/**
			 * Select the trace display base (2, 10 or 16) and highlight the
			 * chosen button.
			 */
			@Override
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
					/**
					 * Re-layout the window and resize the traces when the
					 * panel changes size.
					 */
					@Override
					public void componentResized(ComponentEvent event) {
						window.doLayout();
						traces.resize(traces.getWidth());
					}
				}
		);

		// handle start button push
		start.addActionListener(
				new ActionListener() {
					/**
					 * Start a fresh simulation run when the Start button is
					 * pushed.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						action.removeAll();
						action.add(pause);
						action.add(stop);
						action.validate();
						scaleFactor = NumericField.parse(window,scaleField,
								1,scaleFactor,"Scale factor");
						traces.setScaleFactor();
						setMaxTime();
						runSim();
					}
				}
		);

		// handle step button push
		step.addActionListener(
				new ActionListener() {
					/**
					 * Advance the simulation by the step amount when the Step
					 * button is pushed.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						action.removeAll();
						action.add(resume);
						action.add(step);
						action.add(animate);
						action.add(stop);
						action.add(print);
						action.add(help);
						action.validate();
						scaleFactor = NumericField.parse(window,scaleField,
								1,scaleFactor,"Scale factor");
						traces.setScaleFactor();
						stepAmount = NumericField.parse(window,stepField,
								1,stepAmount,"Step amount");
						setMaxTime();
						if (sim == null) {
							stepEnd = stepAmount;
							runSim();
						}
						else {
							if (!paused) {
								return;
							}
							// set the step target BEFORE waking the sim
							// thread, or a Step can advance zero time
							// (issue #49, finding M9)
							if (stepEnd < 0)
								stepEnd = stepAmount;
							else
								stepEnd += stepAmount;
							paused = false;
							pauseSem.release();
						}
					}
				}
		);

		// handle animate button push
		animate.addActionListener(
				new ActionListener() {
					/**
					 * Begin repeating a step every second when the Animate
					 * button is pushed.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {

						// set up buttons
						action.removeAll();
						action.add(end);
						action.validate();

						// set up step info
						scaleFactor = NumericField.parse(window,scaleField,
								1,scaleFactor,"Scale factor");
						traces.setScaleFactor();
						stepAmount = NumericField.parse(window,stepField,
								1,stepAmount,"Step amount");
						setMaxTime();

						// create step object (TimerTask)
						TimerTask tc = new TimerTask() {

							/**
							 * Perform one animation step, cancelling the timer
							 * at the time limit.
							 */
							@Override
							public void run() {

								if (now >= maxTime) {
									java.util.Timer t = timer;
									if (t != null)
										t.cancel();
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
									// step target before the wakeup (#49)
									if (stepEnd < 0)
										stepEnd = stepAmount;
									else
										stepEnd += stepAmount;
									paused = false;
									pauseSem.release();
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
					/**
					 * Stop animation and hand control back to the step
					 * controls.
					 */
					@Override
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
					/**
					 * Pause the running simulation when the Pause button is
					 * pushed.
					 */
					@Override
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
					/**
					 * Resume the paused simulation when the Resume button is
					 * pushed.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						action.removeAll();
						action.add(pause);
						action.add(stop);
						action.validate();
						scaleFactor = NumericField.parse(window,scaleField,
								1,scaleFactor,"Scale factor");
						traces.setScaleFactor();
						paused = false;
						stepEnd = -1;  // kill any stepping
						pauseSem.release();
					}
				}
		);

		stop.addActionListener(
				new ActionListener() {
					/**
					 * Stop the running simulation when the Stop button is
					 * pushed.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						stop();
					}
				}
		);

		print.addActionListener(
				new ActionListener() {
					/**
					 * Print the trace window when the Print button is pushed.
					 */
					@Override
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
	 *
	 * @jls.testedby jls.sim.InteractiveSimulatorFieldTest#button()
	 * @jls.testedby jls.sim.InteractiveSimulatorFieldTest#fieldWithColumns()
	 */
	public JPanel getWindow() {

		return window;
	} // end of getWindow method

	/**
	 * Set the time limit from the tlimit text field.
	 *
	 * @jls.testedby jls.sim.InteractiveSimulatorFieldTest#setMaxTimeRejectsHugeNegativeAndKeepsPreviousLimit()
	 * @jls.testedby jls.sim.InteractiveSimulatorFieldTest#setMaxTimeStillAcceptsValidLimits()
	 */
	public void setMaxTime() {

		// maxTime is a long but only ever holds int-ranged values on the
		// interactive path (the field's TextFilter caps typing at
		// Integer.MAX_VALUE); the clamp keeps the cast safe regardless
		int previous = (int)Math.min(Math.max(1,maxTime),Integer.MAX_VALUE);
		maxTime = NumericField.parse(window,tlimit,1,previous,"Time limit");
	} // end of setMaxTime method

	/**
	 * Run the simulator in interactive mode.
	 */
	public void runSim() {

		runSim(false);
	} // end of runSim method

	/**
	 * Run the simulator, optionally suppressing UI updates for this run
	 * only ("run in background"). Replaces the JLSInfo.batch toggle the
	 * menu used, which the sim thread almost never observed (issue #49,
	 * finding M15).
	 *
	 * @param background True to suppress trace/clock UI updates.
	 */
	public void runSim(boolean background) {

		// do nothing if no circuit
		final Circuit circ = circuit;
		if (circ == null)
			return;

		quiet = background;

		// if simulator is already running, ignore
		if (sim != null) {
			return;
		}

		// create test signal element
		TestGen gen = null;
		String testFile = testFileName;
		if (testFile != null) {
			gen = new TestGen(circ);
			gen.setFile(testFile);
		}

		// reset clock/queues and initialize all elements; stale permits
		// from a previous run's stop() made the first Pause fall
		// through once (issue #49, finding M9)
		paused = false;
		runningMsgShown = false;
		pauseSem.drainPermits();
		initSimulation();

		// initialize test generator, if there is one
		if (gen != null) {
			gen.initSim(this);
		}

		// find all probes and watched elements (if not batch/background)
		// and set up trace window
		if (!isQuiet()) {
			traces.clear();
			traceMap.clear();
			wireMap.clear();
			for (MemTrace mtr : memTraces) {
				mtr.dispose();
			}
			memTraces.clear();
			findTraces(circ);
			traces.setup();
			traces.draw();
		}

		// create new thread for the simulator
		sim = new Thread("Runner") {

			/**
			 * Run the event loop on the simulator thread, then post the
			 * run's UI epilogue to the EDT.
			 */
			@Override
			public void run() {

				// disable circuit editor (if one)
				Editor ed = circ.getEditor();
				if (ed != null)
					ed.enableEditor(false); // turn off listeners

				// run the shared event loop; pausing, stepping, and
				// tracing happen in the hooks below (#25)
				runEventLoop();

				// kill timer if there is one
				java.util.Timer t = timer;
				if (t != null) {
					t.cancel();
				}

				// determine reason for stopping BEFORE padding the clock,
				// or a completed run can misreport as a time-limit stop
				// (issue #51 low item)
				String reason;
				final long stopTime = now;
				if (stopping)
					reason = "Simulation Stopped";
				else if (now >= maxTime)
					reason = "Simulation Time Limit";
				else if (eventQueue.size() == 0)
					reason = "Simulation: No More Activity";
				else
					reason = "Simulation Complete";
				stopping = true;

				// leave a little extra room at the end
				now += 10L * scaleFactor;

				// turn on listeners
				if (ed != null)
					ed.enableEditor(true);

				if (JLSInfo.batch && JLSInfo.frame == null) {
					System.out.println(reason + " at " + stopTime);
				}

				// UI epilogue on the EDT, not the sim thread (issue #49,
				// finding H8)
				final String reasonText = reason;
				final Editor edRef = ed;
				SwingUtilities.invokeLater(new Runnable() {
					/**
					 * Update traces, clock and buttons on the EDT after the
					 * run ends.
					 */
					@Override
					public void run() {
						if (!isQuiet()) {
							showClock.setText("Time: "+stopTime);
							window.validate();
							traces.draw();
							for (MemTrace mtr : memTraces) {
								mtr.update();
							}
						}
						if (edRef != null)
							edRef.repaint();
						if (!(JLSInfo.batch && JLSInfo.frame == null))
							msg.setText(reasonText);
						updateStatusBar();
						action.removeAll();
						action.add(start);
						action.add(step);
						action.add(animate);
						action.add(print);
						action.add(help);
						action.validate();
					}
				});

				// clear up for next simulation
				sim = null;
			} // end of run method

		}; // end of sim Thread class

		// start up the simulator
		if (!JLSInfo.batch || JLSInfo.frame != null)
			msg.setText("Simulation Running");
		sim.start();
		if (JLSInfo.batch && JLSInfo.frame == null) {
			try {
				sim.join();
			}
			catch(InterruptedException ex) {}
		}

	} // end of runSim method

	/**
	 * Handle pausing and stepping before the next event is dequeued.
	 *
	 * @return true to proceed with the next event, false to re-check the
	 *         loop conditions.
	 */
	@Override
	protected boolean beforeEvent() {

		Editor ed = circuit().getEditor();

		// check for being paused
		if (paused) {
			stepEnd = now;
			final long pausedAt = now;
			// UI updates on the EDT, not the sim thread (#49, H8)
			SwingUtilities.invokeLater(new Runnable() {
				/**
				 * Refresh the trace display and message on the EDT while
				 * paused.
				 */
				@Override
				public void run() {
					if (!isQuiet()) {
						showClock.setText("Time: "+pausedAt);
						traces.draw();
						for (MemTrace mtr : memTraces) {
							mtr.update();
						}
						msg.setText("Simulation Paused");
						updateStatusBar();
					}
				}
			});
			if (ed != null)
				ed.enableEditor(true);
			try {
				pauseSem.acquire();
			}
			catch (InterruptedException ex) {}
			if (ed != null)
				ed.enableEditor(false);
			return false;
		}

		// check for stepping
		// peek at next event
		SimEvent event = eventQueue.peek();
		long when = event.getTime();

		// if after step end time ... (can't happen in quiet mode)
		if (stepEnd != -1 && when > stepEnd) {

			// update current time
			now = stepEnd;
			final long steppedTo = now;
			final Editor edRef = ed;
			SwingUtilities.invokeLater(new Runnable() {
				/**
				 * Advance the clock and traces on the EDT at the end of a
				 * step.
				 */
				@Override
				public void run() {
					showClock.setText("Time: "+steppedTo);
					traces.draw();
					for (MemTrace mtr : memTraces) {
						mtr.update();
					}
					if (edRef != null)
						edRef.repaint();
				}
			});

			paused = true;
			return false;
		}

		if (!isQuiet())
			setMsgOnceRunning();
		return true;
	} // end of beforeEvent method

	/**
	 * Whether the running message is already up: "Simulation Running"
	 * only needs to be set once per run, not per event from the sim
	 * thread (#49, H8).
	 */
	private volatile boolean runningMsgShown = false;

	/**
	 * Post the "Simulation Running" message once per run, on the EDT
	 * (issue #49, finding H8).
	 */
	private void setMsgOnceRunning() {

		if (runningMsgShown)
			return;
		runningMsgShown = true;
		SwingUtilities.invokeLater(new Runnable() {
			/**
			 * Set the running message on the EDT.
			 */
			@Override
			public void run() {
				msg.setText("Simulation Running");
			}
		});
	} // end of setMsgOnceRunning method

	/**
	 * Update the clock display before the event reacts, at a bounded
	 * rate: the per-event setText+validate was both an EDT violation and
	 * a real slowdown (issue #49, finding H8).
	 */
	private volatile long lastClockUpdate = 0;

	/**
	 * Update the clock display at a bounded rate before an event reacts,
	 * dispatching the label change to the EDT (issue #49, finding H8).
	 */
	@Override
	protected void beforeReact() {

		if (JLSInfo.batch && JLSInfo.frame == null)
			return;
		long nowMillis = System.currentTimeMillis();
		if (nowMillis - lastClockUpdate < 50)
			return;
		lastClockUpdate = nowMillis;
		final long simTime = now;
		SwingUtilities.invokeLater(new Runnable() {
			/**
			 * Push the current time and status bar to the EDT.
			 */
			@Override
			public void run() {
				showClock.setText("Time: "+simTime);
				// keeps the status bar live for background runs too
				updateStatusBar();
			}
		});
	} // end of beforeReact method

	/**
	 * Record traces and probes after the event has reacted.
	 *
	 * @param event The event that just reacted.
	 */
	@Override
	protected void afterEvent(SimEvent event) {

		if (!isQuiet()) {

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
	} // end of afterEvent method

	/**
	 * Whether UI updates are suppressed: global batch mode or a
	 * background run (issue #49, finding M15).
	 *
	 * @return true if no UI should be touched.
	 */
	private boolean isQuiet() {

		return JLSInfo.batch || quiet;
	} // end of isQuiet method

	/**
	 * Pause or resume the simulation, if there is one.
	 * Called by the pause element.
	 *
	 * @param which True to pause the simulation, false to resume it.
	 */
	@Override
	public void pause(boolean which) {

		if (sim != null) {
			// called from inside react() on the sim thread (the Pause
			// element); route the button swap to the EDT (#49, H8)
			SwingUtilities.invokeLater(new Runnable() {
				/**
				 * Swap the toolbar to the paused button set on the EDT.
				 */
				@Override
				public void run() {
					action.removeAll();
					action.add(resume);
					action.add(step);
					action.add(animate);
					action.add(stop);
					action.add(print);
					action.add(help);
					action.validate();
				}
			});
			if (timer != null)
				timer.cancel();
			paused = which;
			stepEnd = -1; // kill any stepping
		}
	} // end of pause method

	/**
	 * Stop (end) the simulation, if there is one.
	 */
	@Override
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

		for (Element element : circ.getElements()) {

			// if a wire, check for a probe
			if (element instanceof Wire wire) {
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
			else if (element instanceof SubCircuit sub) {
				Circuit c = sub.getSubCircuit();
				findTraces(c);
			}

			// if a watched element
			else if (element instanceof LogicElement el) {
				if (el.isWatched()) {

					// set up trace window
					Trace tr = new Trace(el.getFullName(),el,el.getBits(),
							traces.getWidth(), traces);
					tr.setBase(displayBase);
					traces.addTrace(tr);
					tr.addValue(el.getCurrentValue(),now);
					traceMap.put(el,tr);

					// set up memory trace if it is a memory element
					if (el instanceof Memory mem) {
						MemTrace mtr = new MemTrace(mem);
						mtr.showit(window);
						memTraces.add(mtr);
					}
				}
				else {
					el.setTracePosition(-1);
				}
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

		if (JLSInfo.batch && JLSInfo.frame == null)
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

			/**
			 * Render the trace window scaled to fit a single printed page.
			 */
			@Override
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
	@SuppressWarnings("serial")
	public class Traces extends JPanel implements MouseMotionListener {

		// properties
		/** The displayed trace rows, top to bottom. */
		private java.util.List<Trace> traceList = new LinkedList<Trace>();
		/** Trace rows added since the last relayout. */
		private java.util.List<Trace> newList = new LinkedList<Trace>();
		/** The pixel width reserved for the signal-name column. */
		private int nameSpace;

		/**
		 * Hard ceiling on how wide the trace panels may grow (issue
		 * #121): keeps a very long run at a tiny scale factor from
		 * demanding an absurd component width; the retained history
		 * is bounded by Trace.MAX_RETAINED_CHANGES anyway.
		 */
		private static final int MAX_PANEL_WIDTH = 4_000_000;

		/**
		 * Set up the window.
		 *
		 * @jls.testedby jls.sim.TraceRetentionTest#parent()
		 * @jls.testedby jls.sim.TraceWindowingTest#parent()
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
		 * Repaint all the trace objects, first growing them to span the
		 * whole retained run so the enclosing scroll pane can reach all
		 * of it (issue #121).  Every call site is on the EDT except the
		 * animate path, which reaches here from a java.util.Timer
		 * thread (runSim() at animate start; a pre-existing off-EDT
		 * path in the #49 series), so re-dispatch before touching the
		 * component tree or the viewport.
		 */
		private void draw() {

			if (!SwingUtilities.isEventDispatchThread()) {
				SwingUtilities.invokeLater(new Runnable() {
					/**
					 * Re-dispatch the draw onto the EDT.
					 */
					@Override
					public void run() {
						draw();
					}
				});
				return;
			}

			// width the run needs at the current scale factor, plus the
			// name area; never smaller than the viewport so short runs
			// still fill the window
			JViewport port = (JViewport)SwingUtilities.getAncestorOfClass(
					JViewport.class,this);
			int viewWidth = port == null ? getWidth()
					: port.getExtentSize().width;
			long runWidth = now/scaleFactor+nameSpace+10;
			final int target = (int)Math.min(
					Math.max(runWidth,viewWidth),MAX_PANEL_WIDTH);

			// keep following the newest activity unless the user has
			// scrolled back into the history
			boolean atEnd = port == null || port.getViewPosition().x
					+port.getExtentSize().width >= getWidth()-5;
			if (target != getWidth()) {
				resize(target);
				revalidate();
			}

			repaint();
			for (Trace tr : traceList) {
				tr.commit(now);
				tr.repaint();
			}

			// after the revalidate has laid the new width out
			if (atEnd && port != null) {
				final JViewport p = port;
				SwingUtilities.invokeLater(new Runnable() {
					/**
					 * Scroll the viewport to follow the newest activity after
					 * re-layout.
					 */
					@Override
					public void run() {
						p.setViewPosition(new Point(
								Math.max(0,target-p.getExtentSize().width),
								p.getViewPosition().y));
					}
				});
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
		 * @param trace The trace to move
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
		@Override
		public void mouseMoved(MouseEvent event) {

			if (!paused && !stopping)
				return;
			int x = event.getX();
			for (Trace tr : traceList) {
				tr.mouseMoved(x);
			}
		} // end of mouseMoved method

		// unused
		/**
		 * Ignore mouse drags in the trace window.
		 */
		@Override
		public void mouseDragged(MouseEvent event) {}

	} // end of Traces class

	/**
	 * A header for the traces, will display the slider and the time the
	 * slider indicates.
	 */
	@SuppressWarnings("serial")
	private class Header extends Trace {

		/**
		 * Call superclass constructor, supply null name.
		 *
		 * @param width The drawable trace width in pixels.
		 * @param el The element the header row nominally traces.
		 * @param parent The trace-window container this header belongs to.
		 */
		public Header(int width, Element el, InteractiveSimulator.Traces parent) {

			super("",el,0,width,parent);
		} // end of constructor

		/**
		 * Draw the slider and the time the slider indicates.
		 */
		@Override
		public void paintComponent(Graphics g) {

			super.paintComponent(g);
			int width = getWidth()-parent.getNameSpace()-10;
			// long math: the panel can span the whole run (issue #121)
			long time = now-(long)(width-sliderPos)*scaleFactor;
			if (time >= 0 && time <= now) {
				g.setColor(Color.gray);
				g.drawString(time+"",sliderPos,2*HEIGHT/3);
			}
		} // end of paintComponent method

		/**
		 * Override superclass method to ignore events.
		 */
		@Override
		public void mousePressed(MouseEvent event) {}

	} // end of Header class

} // end of InteractiveSimulator class

