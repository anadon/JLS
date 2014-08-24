package jls;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.Vector;
import java.util.zip.ZipInputStream;

import javax.help.CSH;
import javax.help.HelpSet;
import javax.print.PrintService;
import javax.swing.Box;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jls.edit.Editor;
import jls.elem.Element;
import jls.elem.LogicElement;
import jls.elem.Memory;
import jls.elem.OutputPin;
import jls.elem.Register;
import jls.elem.SubCircuit;
import jls.sim.BatchSimulator;
import jls.sim.InterractiveSimulator;

import org.tukaani.xz.SeekableFileInputStream;
import org.tukaani.xz.SeekableXZInputStream;

@SuppressWarnings("serial")
public class JLSStart extends JFrame implements ChangeListener {

	// properties
	private static long timeLimit = JLSInfo.defaultTimeLimit;
	private static String paramFile = null;
	private static String testFile = null;
	private static String startFile = null;
	private static boolean printCircuit = false;
	private static boolean printCircuitTop;
	private static String printer = null;
	private InterractiveSimulator interSim;
	private static DefaultExceptionHandler exHandler = null;

	private Container window;
	private JSplitPane both;
	private JTabbedPane edits;
	private Circuit clipboard = new Circuit("clipboard");		// for cut and paste

	/**
	 * Parse command line arguments and start up JLS.
	 * 
	 * @param args Command line arguments.
	 */
	public static void start(String[] args, DefaultExceptionHandler exh) {

		// save exception handler reference
		exHandler = exh;

		// parse command line
		//parseCommandLine(args);

		// if print of circuit wanted, and there is a circuit specified, then print it
		if (printCircuit && startFile != null) {
			printCirc(printCircuitTop);
			return;
		}

		// start up JLS

		// if batch mode, load circuit and run simulator
		if (JLSInfo.batch) {

			// from Zack, for MAC's?
			System.setProperty("java.awt.headless", "true");

			if (startFile == null) {
				System.err.println("batch mode requires circuit file");
				System.exit(1);
			}

			// open file and create scanner
			String name = "";
			if (startFile.endsWith(".jls~")) {
				name = startFile.replaceAll("\\.jls~$","");
			}
			else {
				name = startFile.replaceAll("\\.jls$","");
			}
			String cname = Util.isValidFileName(name);
			if (cname == null) {
				System.out.println(startFile + " is not a valid circuit file name");
				System.exit(1);
			}
			
			Scanner input = staticGetScannerForFile(startFile);

			// create new circuit
			Circuit circ = new Circuit(cname);

			// read circuit from file
			boolean loadOK = circ.load(input);
			if (input.hasNext())
				loadOK = false;		// file shouldn't have anything after ENDCIRCUIT
			input.close();
			if (!loadOK) {
				System.out.println(startFile + " is not a valid circuit file");
				System.out.println("    reason: " + JLSInfo.loadError);
				JOptionPane.showMessageDialog(null, "invalid circuit file: " + JLSInfo.loadError );
				System.exit(1);
			}
			input.close();

			// finish up load
			try {
				if (!circ.finishLoad(null)) {
					System.out.println(startFile + " is not a valid circuit file");
					System.exit(1);
				}
			} catch (Exception e) {
				TellUser.warn((startFile + " is not a valid circuit file"), true);
				e.printStackTrace();
				System.out.println("    reason: " + JLSInfo.loadError);
				JOptionPane.showMessageDialog(null, "invalid circuit file: " + JLSInfo.loadError );
				System.exit(1);
			}

			// save circuit for trace (hopefully not needed!)
			exHandler.setCircuit(circ);

			// process parameter file
			if (paramFile != null)
				processParamFile(paramFile,circ);

			// set up simulator
			BatchSimulator batchSim = new BatchSimulator();
			JLSInfo.sim = batchSim;
			batchSim.setCircuit(circ);
			batchSim.setTimeLimit(timeLimit);
			batchSim.setTestFile(testFile);
			batchSim.addTestGen();

			// run simulator
			batchSim.runSim();

			// display results
			batchSim.displayOutcome();
			displayResults(circ,"");

			// print trace if requested
			if (JLSInfo.printTrace) {
				batchSim.printTrace(printer);
			}
		}
		
		else if (JLSInfo.imgexport) {
			// from Zack, for MAC's?
			System.setProperty("java.awt.headless", "true");

			if (startFile == null) {
				System.err.println("image export requires circuit file");
				System.exit(1);
			}

			// open file and create scanner
			String name = "";
			if (startFile.endsWith(".jls~")) {
				name = startFile.replaceAll("\\.jls~$","");
			}
			else {
				name = startFile.replaceAll("\\.jls$","");
			}
			String cname = Util.isValidFileName(name);
			if (cname == null) {
				System.out.println(startFile + " is not a valid circuit file name");
				System.exit(1);
			}
			InputStream in = null;
			try {

				// see if the .jls file is in zip format
				FileInputStream temp = new FileInputStream(startFile);
				ZipInputStream inz = new ZipInputStream(temp);
				if (inz.getNextEntry() == null) {

					// if not, open as an ordinary file
					temp.close();
					in = new FileInputStream(startFile);
				}
				else {
					in = inz;
				}
			}
			catch (IOException ex) {
				System.out.println("Can't read from " + startFile);
				System.exit(1);
			}
			Scanner input = new Scanner(in);

			// create new circuit
			Circuit circ = new Circuit(cname);

			// read circuit from file
			boolean loadOK = circ.load(input);
			if (input.hasNext())
				loadOK = false;		// file shouldn't have anything after ENDCIRCUIT
			input.close();
			if (!loadOK) {
				System.out.println(startFile + " is not a valid circuit file");
				System.out.println("    reason: " + JLSInfo.loadError);
				JOptionPane.showMessageDialog(null, "invalid circuit file: " + JLSInfo.loadError );
				System.exit(1);
			}
			input.close();

			// finish up load
			try {
				if (!circ.finishLoad(null)) {
					System.out.println(startFile + " is not a valid circuit file");
					System.out.println("    reason: " + JLSInfo.loadError);
					JOptionPane.showMessageDialog(null, "invalid circuit file: " + JLSInfo.loadError );
					System.exit(1);
				}
			} catch (Exception e) {
				TellUser.err(startFile + " is not a valid circuit file", true);
				TellUser.err("    reason: " + JLSInfo.loadError, false);
				e.printStackTrace();
				System.exit(1);
			}
			try {
				circ.exportImage(name + ".jpg");
			} catch (Exception e) {
				TellUser.err("Failed to export image for an undetermined reason", true);
				e.printStackTrace();
			}
		}

		else {

			// start up GUI
			Runnable mainwindow = new Runnable() {

				public void run() {
					try {new JLSStart();}
					catch(HeadlessException e) {
						System.out.println("No usable display device found!");
						System.out.println("Are you using a headless remote session?");
						System.exit(1);
					}
				} // end of run method

			};
			EventQueue.invokeLater(mainwindow);
		}

	} // end of start method

	/**
	 * Display values of watched elements to stdout.
	 * Descends into subcircuits recursively.
	 * 
	 * @param circ The circuit to find watched elements in.
	 * @param qual Qualified name of subcircuit.
	 */
	public static void displayResults(Circuit circ, String qual) {

		for (Element el : circ.getElements()) {
			if (el.isWatched()) {
				if (el instanceof Register) {
					Register reg = (Register)el;
					reg.printValue(qual);
				}
				else if (el instanceof Memory) {
					Memory mem = (Memory)el;
					mem.printChangedValues(qual);
				}
				else if (el instanceof OutputPin) {
					OutputPin pin = (OutputPin)el;
					pin.printValue(qual);
				}
			}
			else if (el instanceof SubCircuit) {
				SubCircuit sel = (SubCircuit)el;
				Circuit subCirc = sel.getSubCircuit();
				String subQual = "";
				if (qual.equals("")) {
					subQual = subCirc.getName();
				}
				else {
					subQual = qual + "." + subCirc.getName();
				}
				displayResults(subCirc,subQual);

			}
		}
	} // end of displayResults method

	/**
	 * Parse command line.
	 * 
	 * @param args The command line arguments.
	 */
	public static void parseCommandLine(String [] args) {

		int pos = 0;
		while (pos < args.length) {
			String arg = args[pos];
			if (arg.charAt(0) == '-') {
				char flag = arg.charAt(1);
				if (flag == 'h') {
					usage();
					System.exit(0);
				}
				else if (flag == 'b') {
					if (arg.length() > 2) {
						usage();
						System.exit(1);
					}
					else {
						JLSInfo.batch = true;
					}
				}
				
				else if (flag == 'i') {
					JLSInfo.imgexport = true;
				}				

				else if (flag == 'r') {
					if (arg.length() > 2) {
						printer = arg.substring(2);
						JLSInfo.printTrace = true;
					}
					else {
						if (args[pos+1].charAt(0) != '-') {
							pos += 1;
							printer = args[pos];
							JLSInfo.printTrace = true;
						}
						else {
							System.out.println("invalid or missing printer name");
							System.exit(1);
						}
					}
				}
				else if (flag == 'p') {
					if (arg.length() > 2) {
						printer = arg.substring(2);
						printCircuit = true;
						printCircuitTop = false;
					}
					else {
						if (args[pos+1].charAt(0) != '-') {
							pos += 1;
							printer = args[pos];
							printCircuit = true;
							printCircuitTop = false;
						}
						else {
							System.out.println("invalid or missing printer name");
							System.exit(1);
						}
					}
				}
				else if (flag == 'v') {
					if (arg.length() > 2) {
						printer = arg.substring(2);
						printCircuit = true;
						printCircuitTop = true;
					}
					else {
						if (args[pos+1].charAt(0) != '-') {
							pos += 1;
							printer = args[pos];
							printCircuit = true;
							printCircuitTop = true;
						}
						else {
							System.out.println("invalid or missing printer name");
							System.exit(1);
						}
					}
				}
				else if (flag == 's') {
					if (arg.length() > 2) {
						paramFile = arg.substring(2);
					}
					else {
						if (args[pos+1].charAt(0) != '-') {
							pos += 1;
							paramFile = args[pos];
						}
						else {
							System.out.println("missing or invalid startup file");
							System.exit(1);
						}
					}
				}
				else if (flag == 'd') {
					String tmp = null;
					if (arg.length() > 2) {
						tmp = arg.substring(2);
					}
					else {
						if (args[pos+1].charAt(0) != '-') {
							pos += 1;
							tmp = args[pos];
						}
						else {
							System.out.println("missing or invalid time limit");
							System.exit(1);
						}
					}
					try {
						timeLimit = Long.parseLong(tmp);
					}
					catch (NumberFormatException ex) {
						System.out.println("time limit not an integer");
						System.exit(1);
					}
				}
				else if (flag == 't') {
					if (arg.length() > 2) {
						testFile = arg.substring(2);
					}
					else {
						if (args[pos+1].charAt(0) != '-') {
							pos += 1;
							testFile = args[pos];
						}
						else {
							System.out.println("missing or invalid test file");
							System.exit(1);
						}
					}
				}
				else {
					System.out.println("invalid flag");
					usage();
					System.exit(1);
				}
			}
			else {
				if (startFile == null) {
					startFile = arg;
				}
				else {
					System.out.println("arguments after circuit file not allowed");
					usage();
					System.exit(1);
				}
			}
			pos += 1;
		}
	} // end of parseCommandLine method

	/**
	 * Print usage information to stderr.
	 */
	private static void usage() {

		System.err.println("usage: jls [ flags ] [ circuit_name ]");
		System.err.println("  -h : print this message");
		System.err.println("  -b : run in batch mode");
		System.err.println("  -sname : startup parameter file");
		System.err.println("  -tname : test input file");
		System.err.println("  -dtime : set simulation time limit (a positive integer)");
		System.err.println("  -pprinter : print circuit to named printer (all other flags ignored");
		System.err.println("  -rprinter : print signal trace to named printer (all other flags ignored)");
		System.err.println("example: jls -b -sstartup -d10000 counter");
	} // end of usage method

	/**
	 * Set up main window.
	 */
	public JLSStart() {

		// make it look the same everywhere (especially MAC's).
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		}
		catch (Exception ex) {

			JOptionPane.showMessageDialog(this, "Can't set cross platform look and feel");
			System.exit(1);
		}

		// save reference for exceptions
		exHandler.setJLS(this);

		// save reference for dialogs
		JLSInfo.frame = this;

		// handle window closings
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener (
				new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						shutdown();
					}
				}
		);

		// set up menu bar
		JMenuBar bar = new JMenuBar();
		bar.add(fileMenu());
		bar.add(simMenu());
		bar.add(globalMenu());
		bar.add(Box.createHorizontalGlue());
		bar.add(helpMenu());
		setJMenuBar(bar);

		// set up main display
		window = getContentPane();
		window.setLayout(new BorderLayout());
		both = new JSplitPane(JSplitPane.VERTICAL_SPLIT,true);
		//both.setDividerLocation(1.0);
		edits = new JTabbedPane(JTabbedPane.TOP,JTabbedPane.SCROLL_TAB_LAYOUT);
		edits.setMinimumSize(new Dimension(1,1));
		both.setTopComponent(edits);
		window.add(both,BorderLayout.CENTER);
		edits.addChangeListener(this);

		// finish up GUI settings
		setTitle(JLSInfo.version);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = (int)(1.618*JLSInfo.windowsize);	// golden ratio
		int height = JLSInfo.windowsize;
		setSize(width,height);
		int left = (screenSize.width - width)/2;
		int top = (screenSize.height - height)/2;
		setLocation(left,top);	// centered in screen
		setVisible(true);

		// set up simulator
		interSim = new InterractiveSimulator();
		JLSInfo.sim = interSim;
		interSim.setTestFile(testFile);

		// load circuit if name given on command line
		if (startFile != null) {
			open(startFile);
		}

	} // end of constructor

	/**
	 * Make simulator point at the correct circuit.
	 * This is the circuit currently being edited if the circuit is not a
	 * subcircuit.
	 * If it is a subcircuit, then the simulator will be pointed at the
	 * root of the subcircuit containment.
	 * If no tab is selected (i.e., all editors have been closed),
	 * then the simulator's circuit is set to null.
	 * 
	 * @param event Unused.
	 */
	public void stateChanged(ChangeEvent event) {

		Editor ed = (Editor)edits.getSelectedComponent();
		if (ed == null) {
			interSim.setCircuit(null);
			both.remove(interSim.getWindow());
			return;
		}
		Circuit circ = ed.getCircuit();
		while (circ.isImported()) {
			SubCircuit sub = circ.getSubElement();
			circ = sub.getCircuit();
		}
		interSim.setCircuit(circ);
	} // end of stateChanged method

	/**
	 * Terminate JLS, but first make sure modified circuits get saved if the
	 * user wants them to be.
	 * Called when JLS is asked to exit.
	 */
	private void shutdown() {

		boolean cancel = false;
		for (Component comp : edits.getComponents()) {
			if (comp instanceof Editor) {
				Editor editor = (Editor)(comp);
				if (!editor.shutdown()) {
					cancel = true;
					break;
				}
				edits.remove(editor);
			}
		}
		if (!cancel)
			System.exit(0);
	} // end of shutdown method

	/**
	 * Get currently visible editor.
	 * 
	 * @return the currently visible editor, or null if no editor is visible.
	 */
	public Editor getVisibleEditor() {

		return (Editor)edits.getSelectedComponent();
	} // end of getVisibleEditor

	/**
	 * Set up file menu.
	 * 
	 * @return the file menu created.
	 */
	public JMenu fileMenu() {

		JMenu menu = new JMenu("File");

		// new
		JMenuItem newc = new JMenuItem("New");
		newc.setToolTipText("create a new circuit");
		menu.add(newc);
		newc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				newCircuit();
			}
		});

		// open
		JMenuItem open = new JMenuItem("Open");
		open.setToolTipText("open an existing circuit file");
		menu.add(open);
		open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				open(null);
			}
		});

		// save
		JMenuItem saveItem = new JMenuItem("Save");
		saveItem.setToolTipText("save the currently visible circuit");
		menu.add(saveItem);
		saveItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Editor ed = (Editor)(edits.getSelectedComponent());
				if (ed != null)
					ed.save();
			}
		});

		// save as
		JMenuItem saveAs = new JMenuItem("Save As");
		saveAs.setToolTipText("save the currently visible circuit under a new name");
		menu.add(saveAs);
		saveAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Editor ed = (Editor)(edits.getSelectedComponent());
				if (ed != null)
					ed.saveAs();
			}
		});

		// print menu item
		JMenu print = new JMenu("Print...");
		JMenuItem printAll = new JMenuItem("Entire circuit");
		printAll.setToolTipText("print the circuit and all subcircuits");
		JMenuItem justThis = new JMenuItem("Just visible window");
		justThis.setToolTipText("print only the currently visible window");
		menu.add(print);
		print.add(printAll);
		print.add(justThis);
		printAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				print(true);
			}
		});
		justThis.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				print(false);
			}
		});

		// import from file menu item
		JMenuItem importItem = new JMenuItem("Import");
		importItem.setToolTipText("create a subcircuit from a circuit file");
		menu.add(importItem);
		importItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				try {
					fileImport();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		// export circuit image menu item
		JMenuItem exportItem = new JMenuItem("Export Image");
		exportItem.setToolTipText("create a JPEG image file of the circuit");
		menu.add(exportItem);
		exportItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				try {
					exportImage();
				} catch (Exception e) {
					System.out.println("ERROR: failed to export image, invalid "
							+ "class reference; could not initialize graphics");
					e.printStackTrace();
				}
			}
		});

		// close menu item
		JMenuItem close = new JMenuItem("Close");
		close.setToolTipText("close the currently visible circuit");
		menu.add(close);
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				closeVisibleEditor();
			}
		});

		// exit menu item
		JMenuItem exit = new JMenuItem("Exit");
		exit.setToolTipText("terminate JLS");
		menu.add(exit);
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				shutdown();
			}
		});

		return menu;
	} // end of fileMenu method

	/**
	 * Set up simulator menu.
	 * 
	 * @return the menu.
	 */
	public JMenu simMenu() {

		JMenu menu = new JMenu("Simulator");
		JMenuItem show = new JMenuItem("Show Simulator Window");
		show.setToolTipText("make simulator window appear");
		menu.add(show);
		JMenuItem hide = new JMenuItem("Hide Simulator Window");
		hide.setToolTipText("make simulator window disappear");
		menu.add(hide);
		JMenuItem run = new JMenuItem("Run (in background)");
		run.setToolTipText("run simulator, don't show window");
		run.setAccelerator(KeyStroke.getKeyStroke("F5"));
		menu.add(run);
		JMenuItem stop = new JMenuItem("Stop (background simulator)");
		stop.setToolTipText("stop runaway simulator");
		stop.setAccelerator(KeyStroke.getKeyStroke("F7"));
		menu.add(stop);

		run.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				both.setBottomComponent(interSim.getStatusBar());
				both.setDividerLocation(0.9);
				JLSInfo.batch = true;
				interSim.setMaxTime();
				interSim.runSim();
				JLSInfo.batch = false;
			}
		});

		stop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				interSim.stop();
			}
		});

		show.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				both.setDividerLocation(0.7);
				both.setBottomComponent(interSim.getWindow());
			}
		});

		hide.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				both.remove(interSim.getWindow());
				both.setDividerLocation(1.0);
			}
		});

		return menu;
	} // end of simMenu method

	/**
	 * Set up global change menu.
	 * 
	 * @return the menu.
	 */
	public JMenu globalMenu() {

		JMenu menu = new JMenu("Global");

		JMenuItem resetDelays = new JMenuItem("Reset all propagation delays");
		menu.add(resetDelays);
		resetDelays.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Editor ed = (Editor)edits.getSelectedComponent();
				if (ed != null) {
					ed.getCircuit().resetAllDelays();
					ed.repaint();
				}
			}
		});

		JMenuItem removeProbes = new JMenuItem("Remove all probes");
		menu.add(removeProbes);
		removeProbes.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Editor ed = (Editor)edits.getSelectedComponent();
				if (ed != null) {
					ed.getCircuit().removeAllProbes();
					ed.repaint();
				}
			}
		});

		JMenuItem clearWatches = new JMenuItem("Unwatch all elements");
		menu.add(clearWatches);
		clearWatches.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Editor ed = (Editor)edits.getSelectedComponent();
				if (ed != null) {
					ed.getCircuit().clearAllWatches();
					ed.repaint();
				}
			}
		});

		JMenuItem expand = new JMenuItem("Expand circuit drawing area by 10%");
		menu.add(expand);
		expand.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Editor ed = (Editor)edits.getSelectedComponent();
				if (ed != null) {
					ed.increaseSize();
				}
			}
		});

		JMenuItem gridCol = new JMenuItem("Change editor window grid color");
		menu.add(gridCol);
		gridCol.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Color newColor = JColorChooser.showDialog(null, "Select Grid Color", JLSInfo.gridColor);
				if (newColor != null)
					JLSInfo.gridColor = newColor;
				Editor ed = (Editor)edits.getSelectedComponent();
				if (ed != null) {
					ed.repaint();
				}
			}
		});

		JMenuItem editBkg = new JMenuItem("Change editor window background color");
		menu.add(editBkg);
		editBkg.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Color newColor = JColorChooser.showDialog(null, "Select Background Color", JLSInfo.backgroundColor);
				if (newColor != null)
					JLSInfo.backgroundColor = newColor;
				Editor ed = (Editor)edits.getSelectedComponent();
				if (ed != null) {
					ed.changeBackgroundColor();
					ed.repaint();
				}
			}
		});

		return menu;
	} // end of globalMenu method

	/**
	 * Create help menu.
	 * 
	 * @return the menu.
	 */
	public JMenu helpMenu() {

		JMenu help = new JMenu("Help");

		// set up about
		JMenuItem about = new JMenuItem("About");
		help.add(about);
		about.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				new About();
			}
		});

		// set up tutorial
		JMenu tutorial = new JMenu("Tutorial");
		help.add(tutorial);
		JMenuItem tutorial1 = new JMenuItem("Introduction");
		String tip1 = "<html>This tutorial demonstrates the " +
		"basic drawing capabilities<br>" + 
		"using simple gates and wires, " +
		"and how to use the simulator&nbsp;&nbsp;<br>" +
		"to watch the circuit in action.</html>";
		tutorial1.setToolTipText(tip1);
		tutorial.add(tutorial1);
		tutorial1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				new Tutorial(JLSInfo.frame,"tutorial1.html",false);
			}
		});
		JMenuItem tutorial2 = new JMenuItem("4-Bit Counter");
		String tip2 = "<html>Demonstrates the use of more complex&nbsp;&nbsp;<br>" + 
		"elements and multi-wire connections.</html>";
		tutorial2.setToolTipText(tip2);
		tutorial.add(tutorial2);
		tutorial2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				new Tutorial(JLSInfo.frame,"tutorial2.html",false);
			}
		});
		JMenuItem tutorial3 = new JMenuItem("Full Adder");
		String tip3 = "<html>Demonstrates how to define and use subcircuits.";
		tutorial3.setToolTipText(tip3);
		tutorial.add(tutorial3);
		tutorial3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				new Tutorial(JLSInfo.frame,"tutorial3.html",false);
			}
		});
		JMenuItem tutorial4 = new JMenuItem("Sign Extension");
		String tip4 = "<html>Demonstrates how to bundle/unbundle&nbsp;&nbsp;<br>" +
		"and to use the signal copy element.";
		tutorial4.setToolTipText(tip4);
		tutorial.add(tutorial4);
		tutorial4.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				new Tutorial(JLSInfo.frame,"tutorial4.html",false);
			}
		});

		// set up javahelp
		JMenuItem contents = new JMenuItem("Contents");
		help.add(contents);
		String helpHS = "help/JLSHelp.hs";
		ClassLoader cl = JLS.class.getClassLoader();
		HelpSet mainHS = null;
		try {
			URL url = HelpSet.findHelpSet(cl, helpHS);
			mainHS = new HelpSet(cl, url);
		}
		catch (Exception ex) {
			contents.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					JOptionPane.showMessageDialog(null,
					"No help - something bad happened during initialization.");
				}
			});
			return help;
		}
		catch (NoClassDefFoundError er) {
			contents.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					JOptionPane.showMessageDialog(null,
					"No help - jh.jar appears to be missing");
				}
			});
			return help;
		}

		JLSInfo.hb = mainHS.createHelpBroker();
		contents.addActionListener(new CSH.DisplayHelpFromSource(JLSInfo.hb));
		return help;
	} // end of helpMenu method

	/**
	 * Create a new circuit.
	 */
	private void newCircuit() {

		String name = JOptionPane.showInputDialog("Enter circuit name (without .jls)");
		if (name == null || name.equals(""))
			return;
		if (!Util.isValidName(name)) {
			JOptionPane.showMessageDialog(JLSInfo.frame,"Invalid circuit name - must have only letters, digits & _");
			return;
		}

		// strip .jls if there
		name = name.replaceAll("\\.jls$","");

		// don't allow duplicate names
		if (duplicateName(name))
			return;

		// create circuit and set up editor
		Circuit circ = new Circuit(name);
		circ.setDirectory(System.getProperty("user.dir"));
		exHandler.setCircuit(circ);
		setupEditor(circ,name);
	} // end of newCircuit method

	private String prevOpenDir = "";  // the directory of the previous file opened

	/**
	 * Open an existing circuit.
	 * 
	 * @param name The name of the circuit.  If null, then prompt user for
	 * the name.
	 */
	private void open(String fileName) {

		File file = null;
		String dir = "";

		// get circuit name from user if parameter is null
		if (fileName == null) {
			JFileChooser chooser = null;
			if (prevOpenDir.equals(""))
				chooser = new JFileChooser(System.getProperty("user.dir"));
			else
				chooser = new JFileChooser(prevOpenDir);
			javax.swing.filechooser.FileFilter filter =
				new javax.swing.filechooser.FileFilter() {
				public boolean accept(File f) {
					return f.getName().endsWith(".jls") || f.getName().endsWith(".jls~")
					|| f.isDirectory();
				}
				public String getDescription() {
					return "JLS Circuit Files";
				}
			};
			chooser.setFileFilter(filter);
			if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) 
				return;
			file = chooser.getSelectedFile();
			fileName = file.getName().trim();
			if (fileName == null || fileName.equals(""))
				return;
			dir = chooser.getCurrentDirectory().toString();
		}
		else {
			file = new File(fileName);
			dir = file.getParent();
			if (dir == null)
				dir = System.getProperty("user.dir");
		}
		
		Scanner input = getScannerForFile(fileName);

		String cname;
		if (fileName.endsWith(".jls~")) {
			cname = fileName.replaceAll("\\.jls~$", "");
		} else {
			cname = fileName.replaceAll("\\.jls$", "");
		}

		// create new circuit
		Circuit circ = new Circuit(cname);
		circ.setDirectory(dir);

		// save directory name for next open
		prevOpenDir = new String(dir);

		// read circuit from file
		boolean loadOK = circ.load(input);
		if (input.hasNext())
			loadOK = false;		// file shouldn't have anything after ENDCIRCUIT
		input.close();
		if (!loadOK) {
			JOptionPane.showMessageDialog(this,
					fileName + " is not a valid circuit file (line " + circ.getLineNumber() + "): " + JLSInfo.loadError, "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		// delete checkpoint file if there is one
		new File(cname + ".jls~").delete();

		// create editor
		exHandler.setCircuit(circ);
		setupEditor(circ,cname);
	} // end of open method

	/**
	 * Set up editor window
	 * 
	 * @param circ The circuit the editor will edit.
	 * @param name The name of the circuit.
	 */
	public void setupEditor(Circuit circ, String name) {

		// create editor
		Editor ed = new Editor(edits,circ,name,clipboard);
		circ.setEditor(ed);

		// update all import menus
		for (Component edit : edits.getComponents()) {
			if (!(edit instanceof Editor))
				continue;
			Editor otherEditor = (Editor)edit;

			// add this circuit to another circuit's import menu
			otherEditor.addToImportMenu(circ);

			// add other non-imported circuits to this circuit's import menu
			if (!otherEditor.getCircuit().isImported())
				ed.addToImportMenu(otherEditor.getCircuit());
		}

		// show on screen
		edits.add(ed);
		edits.setSelectedComponent(ed);

	} // end of setupEditor method

	/**
	 * Close the currently visible editor.
	 * If the currently visible editor is editing a circuit (not a subcircuit) with
	 * subcircuits, close all editted subcircuits too.
	 * If the currently visible editor is editing a circuit that is not a subcircuit,
	 * remove the circuit's name from the import lists of all other editors of
	 * non-imported subcircuits.
	 */
	private void closeVisibleEditor() {

		// get the currently visible editor, and exit if there isn't one
		Editor ed = (Editor)(edits.getSelectedComponent());
		if (ed == null)
			return;
		ed.close();

		// get newly visible editor (if one) and tell exception handler
		ed = (Editor)(edits.getSelectedComponent());
		if (ed == null)
			exHandler.setCircuit(null);
		else {
			Circuit c = ed.getCircuit();
			while (c.isImported())
				c = c.getSubElement().getCircuit();
			exHandler.setCircuit(c);
		}
	} // end of close method

	/**
	 * Print circuit currently being edited, plus any state machines.
	 * 
	 * @param all True to print the entire circuit, false to print just what's visible.
	 */
	public void print(boolean all) {

		// get the currently selected editor, return if none
		Editor ed = (Editor)(edits.getSelectedComponent());
		if (ed == null)
			return;

		// set up printer job
		PrinterJob job = PrinterJob.getPrinterJob();
		PageFormat format = job.defaultPage();
		format.setOrientation(PageFormat.LANDSCAPE);
		Book book = new Book();

		// find all unique sub-circuits (and their state machines)
		if (all) {
			ed.getCircuit().addToBook(book,format);
		}
		else {
			book.append(ed.getCircuit(),format);
		}

		// finish up book
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
	} // end of print method

	/**
	 * Check for duplicate of circuit already being edited.
	 * 
	 * @param name The new name.
	 * 
	 * @return true if a duplicate, false if not.
	 */
	public boolean duplicateName(String name) {

		for (int e=0; e<edits.getTabCount(); e+=1) {
			if (name.equals(edits.getTitleAt(e))) {
				JOptionPane.showMessageDialog(this,
						name + " is already being editted", "Error",
						JOptionPane.ERROR_MESSAGE);
				return true;
			}
		}
		return false;
	} // end of duplicateName method

	/**
	 * 
	 * @param filePath
	 * @return
	 */
	private Scanner getScannerForFile(String filePath){
		
		File file = null;
		String name;
		
		if (filePath.endsWith(".jls~")) {
			name = filePath.replaceAll("\\.jls~$", "");
		} else {
			name = filePath.replaceAll("\\.jls$", "");
		}
		String cname = Util.isValidFileName(name);
		if (cname == null) {
			JOptionPane
					.showMessageDialog(
							this,
							name
									+ " is not a valid circuit file name.\n It must start with a letter and contain letters, digits and underscores.",
							"Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		if (duplicateName(cname))
			return null;

		// open file and create scanner
		InputStream in = null;
		file = new File(filePath);
		

		//See if the .jls file is in xz format
		try{
			in = new SeekableXZInputStream(new SeekableFileInputStream(file));
		}catch(Throwable e){
			//not an xz file
			System.out.println("Not a XZ compressed file, trying to open as zip");
		}
		
		if(in == null){
			try {
				// see if the .jls file is in zip format
				FileInputStream temp2 = new FileInputStream(file);
				in = new ZipInputStream(temp2);
				
				if (((ZipInputStream)in).getNextEntry() == null) {

					// if not, then not a zip file
					in.close();
					in = null;
				}
			} catch (IOException ex) {
				//not a zip file
				System.out.println("Not a zip compressed file, trying to open as "
						+ "plain text");
			}
		}
		
		if(in == null){
			try{
				//final try -- plain text
				in = new FileInputStream(file);
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, "Can't read from " + filePath,
						"Error", JOptionPane.ERROR_MESSAGE);
				return null;
			}
		}
		
		return new Scanner(in);
	}
	
	private static Scanner staticGetScannerForFile(String filePath){
		
		File file = null;
		String name;
		
		if (filePath.endsWith(".jls~")) {
			name = filePath.replaceAll("\\.jls~$", "");
		} else {
			name = filePath.replaceAll("\\.jls$", "");
		}
		String cname = Util.isValidFileName(name);
		if (cname == null) {
			TellUser.err(name + " is not a valid circuit file name.\n"
					+ "It must start with a letter and contain letters, "
					+"digits and underscores.", true);
			return null;
		}

		// open file and create scanner
		InputStream in = null;
		file = new File(filePath);
		

		//See if the .jls file is in xz format
		try{
			in = new SeekableXZInputStream(new SeekableFileInputStream(file));
		}catch(Throwable e){
			//not an xz file
			System.out.println("Not a XZ compressed file, trying to open as zip");
		}
		
		if(in == null){
			try {
				// see if the .jls file is in zip format
				FileInputStream temp2 = new FileInputStream(file);
				in = new ZipInputStream(temp2);
				
				if (((ZipInputStream)in).getNextEntry() == null) {

					// if not, then not a zip file
					in.close();
					in = null;
				}
			} catch (IOException ex) {
				//not a zip file
				System.out.println("Not a zip compressed file, trying to open as "
						+ "plain text");
			}
		}
		
		if(in == null){
			try{
				//final try -- plain text
				in = new FileInputStream(file);
			} catch (IOException ex) {
				TellUser.err("Can't read from " + filePath, true);
				return null;
			}
		}
		
		return new Scanner(in);
	}
	
	
	/**
	 * Import a circuit from a file into this circuit.
	 * @throws Exception 
	 */
	public void fileImport() throws Exception {

		Editor ed = (Editor)edits.getSelectedComponent();
		if (ed == null) {
			JOptionPane.showMessageDialog(this,
					"no circuit to import into", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
		javax.swing.filechooser.FileFilter filter =
			new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				return f.getName().endsWith(".jls") || f.isDirectory();
			}
			public String getDescription() {
				return "JLS Circuit Files";
			}
		};
		chooser.setFileFilter(filter);
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) 
			return;
		String fileName = chooser.getSelectedFile().getName().trim();
		if (fileName == null || fileName.equals(""))
			return;
		if (!fileName.endsWith(".jls"))
			fileName = fileName + ".jls";
		String name = fileName.replaceAll("\\.jls$","");
		fileName = chooser.getCurrentDirectory().toString() + "/"+ fileName;

		Scanner input = getScannerForFile(fileName);

		// create new circuit
		Circuit circ = new Circuit(name);

		// read circuit from file
		boolean loadOK = circ.load(input);
		if (input.hasNext())
			loadOK = false;		// file shouldn't have anything after ENDCIRCUIT
		input.close();
		if (!loadOK) {
			JOptionPane.showMessageDialog(this,
					fileName + " is not a valid circuit file", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		circ.finishLoad(null);

		ed.finishImport(circ);
	} // end of fileImport method

	/**
	 * Check for parameter file in the current directory,
	 * 
	 * @param paramFile The name of the file containing JLS parameters.
	 * @param circuit The circuit to apply the parameters too.
	 */
	public static void processParamFile(String paramFile, Circuit circuit) {

		try {

			// open file, set up miscellaneous stuff
			FileInputStream input = new FileInputStream(paramFile);
			Scanner scan = new Scanner(input);

			// read info
			while (scan.hasNext()) {
				String key = scan.next();

				// process TYPE command
				if (key.equals("TYPE")) {

					Class<?> cl = null;

					// get element type
					if (!scan.hasNext()) {
						System.out.print(paramFile + ": expected element type,");
						System.out.println(" got end of file");
						System.exit(1);
					}
					String type = scan.next();
					try {
						cl = Class.forName("jls.elem." + type);
					}
					catch (ClassNotFoundException ex) {
						System.out.print(paramFile + ": expected element type,");
						System.out.println(" got \"" + type + "\"");
						System.exit(1);
					}

					// get PROPDELAY
					if (!scan.hasNext()) {
						System.out.print(paramFile + ": expected PROPDELAY,");
						System.out.println(" got end of file");
						System.exit(1);
					}
					String word = scan.next();
					if (!word.equals("PROPDELAY")) {
						System.out.print(paramFile + ": expected PROPDELAY,");
						System.out.println(" got \"" + word + "\"");
						System.exit(1);
					}

					// get propagation delay value
					if (!scan.hasNext()) {
						System.out.print(paramFile + ": expected propagation delay,");
						System.out.println(" got end of file");
						System.exit(1);
					}
					if (!scan.hasNextInt()) {
						System.out.print(paramFile + ": expected propagation delay,");
						System.out.println(" got \"" + scan.next() + "\"");
						System.exit(1);
					}

					// get delay value for this type
					int delay = scan.nextInt();
					if (delay < 1) {
						System.out.print(paramFile + ": expected propagation delay > 0,");
						System.out.println(" got \"" + delay + "\"");
						System.exit(1);
					}

					// send to all elements of this type
					setPropDelays(circuit, cl, delay);

				}

				// process ELEMENT command
				else if (key.equals("ELEMENT")) {

					// get name
					if (!scan.hasNext()) {
						System.out.print(paramFile + ": expected element name,");
						System.out.println(" got end of file");
						System.exit(1);
					}
					String name = scan.next();
					Vector<String> qualifiedName = parseName(name);
					if (qualifiedName == null) {
						System.out.println(paramFile + ": invalid element name " + name);
						System.exit(1);
					}

					// run down into subcircuits
					String elementName = qualifiedName.remove(qualifiedName.size()-1);
					Circuit circ = circuit;
					if (qualifiedName.size() > 0) {
						for (String sub : qualifiedName) {
							Circuit next = null;
							for (Element el : circ.getElements()) {
								if (el instanceof SubCircuit) {
									SubCircuit lel = (SubCircuit)el;
									if (sub.equals(lel.getName())) {
										next = lel.getSubCircuit();
										break;
									}
								}
							}
							if (next == null) {
								System.out.println(paramFile + ": no such element name " + name);
								System.exit(1);
							}
							circ = next;
						}
					}

					// look for element in the appropriate circuit or subcircuit
					LogicElement element = null;
					for (Element el : circ.getElements()) {
						if (el instanceof LogicElement) {
							LogicElement lel = (LogicElement)el;
							if (elementName.equals(lel.getName())) {
								element = lel;
								break;
							}
						}
					}
					if (element == null) {
						System.out.print(paramFile + ": no such element named");
						System.out.println(" \"" + name + "\"");
						System.exit(1);
					}
					if (!scan.hasNext()) {
						System.out.print(paramFile + ": expected element property,");
						System.out.println(" got end of file");
						System.exit(1);
					}

					// get element property to change
					String prop = scan.next();
					if (prop.equals("WATCHED")) {

						// get watched info
						if (!scan.hasNext()) {
							System.out.print(paramFile + ": expected true or false,");
							System.out.println(" got end of file");
							System.exit(1);
						}
						String tf = scan.next();
						if (tf.equals("true")) {
							element.setWatched(true);
						}
						else if (tf.equals("false")) {
							element.setWatched(false);
						}
						else {
							System.out.print(paramFile + ": expected true or false,");
							System.out.println(" got \"" + tf + "\"");
							System.exit(1);
						}
					}

					// propdelay info
					else if (prop.equals("PROPDELAY")) {

						if (!scan.hasNext()) {
							System.out.print(paramFile + ": expected prop delay value,");
							System.out.println(" got end of file");
							System.exit(1);
						}
						if (!scan.hasNextInt()) {
							System.out.print(paramFile + ": expected prop delay value,");
							System.out.println(" got \"" + scan.next() + "\"");
							System.exit(1);
						}
						int delay = scan.nextInt();
						if (delay < 1) {
							System.out.print(paramFile + ": expected prop delay > 0,");
							System.out.println(" got \"" + delay + "\"");
							System.exit(1);
						}
						element.setDelay(delay);
					}

					// register info
					else if (prop.equals("INITIALLY")) {
						if (!(element instanceof Register)) {
							System.out.println(name + " is not a Register element");
							System.exit(1);
						}
						if (!scan.hasNextBigInteger()) {
							System.out.print(paramFile + ": expected initial value,");
							System.out.println(" got end of file");
							System.exit(1);
						}
						BigInteger init = scan.nextBigInteger();
						Register reg = (Register)element;
						reg.setInitialValue(init);
					}

					// memory file info
					else if (prop.equals("FILENAME")) {
						if (!(element instanceof Memory)) {
							System.out.println(name + " is not a Memory element");
							System.exit(1);
						}
						if (!scan.hasNext()) {
							System.out.print(paramFile + ": expected memory file name,");
							System.out.println(" got end of file");
							System.exit(1);
						}
						String file = scan.next();
						Memory mem = (Memory)element;
						mem.setMemFile(file);
					}

					// error
					else {
						System.out.print(paramFile + ": expected element property,");
						System.out.println(" got \"" + prop + "\"");
						System.exit(1);
					}
				}

				// process CLEAR command
				else if (key.equals("CLEAR")) {
					if (!scan.hasNext()) {
						System.out.print(paramFile + ": expected WATCHES or PROBES,");
						System.out.println(" got end of file");
						System.exit(1);
					}
					String word = scan.next();
					if (!word.equals("WATCHES") && !word.equals("PROBES")) {
						System.out.print(paramFile + ": expected WATCHES or PROBES,");
						System.out.println(" got \"" + word + "\"");
						System.exit(1);
					}

					if (word.equals("WATCHES")) {

						// clear all watched flags in the circuit
						circuit.clearAllWatches();
					}
					else {

						// clear all probes in the circuit
						circuit.removeAllProbes();
					}
				}


				// process RESET command
				else if (key.equals("RESET")) {
					if (!scan.hasNext()) {
						System.out.print(paramFile + ": expected PROPDELAYS,");
						System.out.println(" got end of file");
						System.exit(1);
					}
					String word = scan.next();
					if (word.equals("PROPDELAYS")) {

						// reset all propagation delays in the circuit
						circuit.resetAllDelays();
					}
					else {
						System.out.print(paramFile + ": expected PROPDELAYS,");
						System.out.println(" got " + word);
						System.exit(1);
					}
				}

				// else an invalid command
				else {
					System.out.println(paramFile + ": invalid command [" + key + "]");
					System.exit(1);
				}

			}
		}
		catch (FileNotFoundException ex) {
			System.out.println("warning: can't open parameter file " + paramFile + ", file ignored");
			// do nothing if the file does not exist or can't be read
		}


		catch (InputMismatchException ex) {
			// should not happen, ignore if it does
		}

	} // end of processParamFile method

	/**
	 * Set the propagation delay of all elements of a certain type
	 * in a circuit and its subcircuits.
	 * 
	 * @param circ The circuit.
	 * @param cl The type (class) of element to change.
	 * @param delay The new propagation delay.
	 */
	public static void setPropDelays(Circuit circ, Class<?> cl, int delay) {

		for (Element el : circ.getElements()) {
			if (el instanceof SubCircuit) {
				SubCircuit sub = (SubCircuit)el;
				Circuit c = sub.getSubCircuit();
				setPropDelays(c,cl,delay);
			}
			else if (el.getClass() == cl) {
				LogicElement lel = (LogicElement)el;
				lel.setDelay(delay);
			}
		}
	} // end of setPropDelays method

	/**
	 * Print the circuit specified in the start file.
	 * 
	 * @param justTop True if just the top level of the circuit is to be printed, false if the whole thing.
	 */
	private static void printCirc(boolean justTop) {

		String name = startFile.replaceAll("\\.jls$","");

		// create new circuit
		Circuit circ = new Circuit(name);
		
		try {
			circ.finishLoad(null);
		} catch (Exception e) {
			System.out.println(startFile + " is not a valid circuit file, bad "
					+ "class reference");
			e.printStackTrace();
			return;
		}

		// set up printer job
		PrinterJob job = PrinterJob.getPrinterJob();
		PrintService [] services = job.lookupPrintServices();
		PrintService want = null;
		for (PrintService s : services) {
			if (s.getName().equals(printer)) {
				want = s;
			}
		}
		if (want ==  null) {
			System.out.println(printer + " is an invalid printer");
			System.exit(1);
		}
		try {
			job.setPrintService(want);
		}
		catch (PrinterException ex) {
			System.out.println(printer + " is an invalid printer");
		}
		PageFormat format = job.defaultPage();
		format.setOrientation(PageFormat.LANDSCAPE);
		Book book = new Book();

		// print either the top level or the entire circuit
		if (justTop) {
			book.append(circ,format);
		}
		else {
			circ.addToBook(book,format);
		}

		// finish up book
		job.setPageable(book);

		// print the circuit
		try {
			job.print();
		}
		catch (PrinterException ex) {
			System.out.println("printing error: " + ex.getMessage());
		}
	} // end of printCirc method

	/**
	 * Write an image of the circuit to a file.
	 * @throws Exception 
	 */
	public void exportImage() throws Exception {

		Editor ed = (Editor)edits.getSelectedComponent();
		if (ed == null)
			return;

		// get name from user
		JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
		javax.swing.filechooser.FileFilter filter =
			new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				return f.getName().endsWith(".jpg") || f.isDirectory();
			}
			public String getDescription() {
				return "JLS Circuit Images";
			}
		};
		chooser.setFileFilter(filter);
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) 
			return;
		String fileName = chooser.getSelectedFile().getName().trim();
		if (fileName == null || fileName.equals(""))
			return;
		String tempName = fileName.replaceAll("\\.jpg$","");
		if (!Util.isValidName(tempName)) {
			JOptionPane.showMessageDialog(JLSInfo.frame,"Invalid file name - must contain only letters, digits & _");
			return;
		}
		if (!fileName.endsWith(".jpg")) {
			fileName = fileName + ".jpg";
		}
		fileName = chooser.getCurrentDirectory() + "/" + fileName;

		// export the image
		Circuit circ = ed.getCircuit();
		circ.exportImage(fileName);
	} // end of exportImage method

	/**
	 * Break compound name (a.b.c) into components.
	 * 
	 * @param name The compound name.
	 * 
	 * @return the components of the name, or null if the name is not valid.
	 */
	public static Vector<String> parseName(String name) {

		Vector<String> comp = new Vector<String>();
		int first = 0;
		for (int p=0; p<name.length(); p+=1) {
			if (name.charAt(p) == '.') {
				String component = name.substring(first,p);
				if (!Util.isValidName(component))
					return null;
				if (component.equals(""))
					return null;
				comp.add(component);
				first = p+1;
			}
		}
		if (name.charAt(name.length()-1) == '.')
			return null;
		String component = name.substring(first,name.length());
		if (!Util.isValidName(component))
			return null;
		comp.add(component);
		return comp;
	} // end of parseName method

} // end of JLSStart class
