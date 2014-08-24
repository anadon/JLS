package jls;

import jls.edit.*;
import jls.elem.*;
import jls.sim.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.zip.ZipInputStream;

import javax.swing.*;
import javax.swing.event.*;
import javax.help.*;

/**
 * Applet demonstration version.
 * 
 * @author David A. Poplawski
 */
public final class JLSApplet extends JApplet implements ActionListener, ChangeListener {

	// properties
	private JMenuItem newc = new JMenuItem("New");
	private JMenuItem open = new JMenuItem("Open");
	private JMenuItem save = new JMenuItem("Save");
	private JMenuItem saveAs = new JMenuItem("Save As");
	private JMenuItem print = new JMenuItem("Print");
	private JMenuItem imp = new JMenuItem("Import");
	private JMenuItem exp = new JMenuItem("Export Image");
	private JMenuItem close = new JMenuItem("Close");
	private JMenuItem exit = new JMenuItem("Exit");	

	private JSplitPane both;
	private JTabbedPane edits;
	private Circuit clipboard = new Circuit("clipboard");			// for cut and paste
	private InterractiveSimulator sim;

	/**
	 * Set up GUI.
	 */
	public void init() {
		
		JLSInfo.isApplet = true;

		// set up main menu
		JMenuBar bar = new JMenuBar();
		bar.add(fileMenu());
		bar.add(simMenu());
		bar.add(globalMenu());
		bar.add(Box.createHorizontalGlue());
		bar.add(helpMenu());
		setJMenuBar(bar);

		// set up GUI
		Container window = getContentPane();
		window.setLayout(new BorderLayout());

		both = new JSplitPane(JSplitPane.VERTICAL_SPLIT,true);
		both.setResizeWeight(1.0);	// give extra space to top
		edits = new JTabbedPane(JTabbedPane.TOP,JTabbedPane.SCROLL_TAB_LAYOUT);
		edits.setMinimumSize(new Dimension(100,100));
		both.setTopComponent(edits);
		window.add(both,BorderLayout.CENTER);
		edits.addChangeListener(this);

		// set up simulator
		sim = new InterractiveSimulator();
		JLSInfo.sim = sim;

	} // end of init method

	/**
	 * 
	 */
	public void actionPerformed(ActionEvent event) {

		if (event.getSource() == newc) {

			// get circuit name
			String name = JOptionPane.showInputDialog(this,"Circuit name?");
			if (name == null || name.equals(""))
				return;
			for (int e=0; e<edits.getTabCount(); e+=1) {
				if (name.equals(edits.getTitleAt(e))) {
					JOptionPane.showMessageDialog(this,
							name + " is already being editted", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
			}

			// create circuit and editor
			Circuit circ = new Circuit(name);
			setupEditor(circ,name);
		}
		else if (event.getSource() == open) {
			String name = JOptionPane.showInputDialog(this,"Circuit URL?");
			if (name == null || name.equals(""))
				return;
		}
		else if (event.getSource() == close) {
			edits.remove(edits.getSelectedComponent());
		}
		else {
			JOptionPane.showMessageDialog(this,"Not implemented in applet version.",
					"Error",JOptionPane.ERROR_MESSAGE);
		}

	} // end of actionPerformed method

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

		// add to all other editor's import menu
		for (Component edit : edits.getComponents()) {
			if (!(edit instanceof Editor))
				continue;
			Editor otherEditor = (Editor)edit;
			if (otherEditor.getCircuit().isImported())
				continue;
			otherEditor.addToImportMenu(circ);
			ed.addToImportMenu(otherEditor.getCircuit());
		}

		// show on screen
		edits.add(ed);
		edits.setSelectedComponent(ed);

	} // end of setupEditor method

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
			sim.setCircuit(null);
			both.remove(sim.getWindow());
			return;
		}
		Circuit circ = ed.getCircuit();
		while (circ.isImported()) {
			SubCircuit sub = circ.getSubElement();
			circ = sub.getCircuit();
		}
		sim.setCircuit(circ);
	} // end of stateChanged method

	/**
	 * Set up file menu.
	 * 
	 * @return the menu.
	 * 
	 * TODO implement tool tips
	 */
	public JMenu fileMenu() {

		// set up main part of menu
		JMenu file = new JMenu("File");
		file.add(newc);
		newc.setToolTipText("create new circuit");
		newc.addActionListener(this);
		
		file.add(open);
		open.setToolTipText("not implemented");
		open.setForeground(Color.lightGray);
		open.addActionListener(this);
		
		file.add(save);
		save.setToolTipText("not implemented");
		save.setForeground(Color.lightGray);
		save.addActionListener(this);
		
		file.add(saveAs);
		saveAs.setToolTipText("not implemented");
		saveAs.setForeground(Color.lightGray);
		saveAs.addActionListener(this);
		
		file.add(print);
		print.setToolTipText("not implemented");
		print.setForeground(Color.lightGray);
		print.addActionListener(this);
		
		file.add(imp);
		imp.setToolTipText("not implemented");
		imp.setForeground(Color.lightGray);
		imp.addActionListener(this);
		
		file.add(exp);
		exp.setToolTipText("not implemented");
		exp.setForeground(Color.lightGray);
		exp.addActionListener(this);
		
		file.add(close);
		close.setToolTipText("close visible circuit");
		close.addActionListener(this);
		
		file.add(exit);
		exit.setToolTipText("not implemented");
		exit.setForeground(Color.lightGray);
		exit.addActionListener(this);

		return file;
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
		run.setToolTipText("run simulator");
		run.setAccelerator(KeyStroke.getKeyStroke("F5"));
		menu.add(run);
		JMenuItem stop = new JMenuItem("Stop (background simulator)");
		run.setToolTipText("stop simulator");
		stop.setAccelerator(KeyStroke.getKeyStroke("F7"));
		menu.add(stop);

		run.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				both.setBottomComponent(sim.getStatusBar());
				JLSInfo.batch = true;
				sim.runSim();
				JLSInfo.batch = false;
			}
		});

		stop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				sim.stop();
			}
		});

		show.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				both.setBottomComponent(sim.getWindow());
			}
		});

		hide.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				both.remove(sim.getWindow());
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

		JMenuItem expand = new JMenuItem("Expand circuit drawing area");
		menu.add(expand);
		expand.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Editor ed = (Editor)edits.getSelectedComponent();
				if (ed != null) {
					ed.increaseSize();
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
		String tip2 = "<html>This demonstrates the use of more complex&nbsp;&nbsp;<br>" + 
		"elements and multi-wire connections.</html>";
		tutorial2.setToolTipText(tip2);
		tutorial.add(tutorial2);
		tutorial2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				new Tutorial(JLSInfo.frame,"tutorial2.html",false);
			}
		});
		JMenuItem tutorial3 = new JMenuItem("Full Adder");
		String tip3 = "<html>This demonstrates how to define and use subcircuits.";
		tutorial3.setToolTipText(tip3);
		tutorial.add(tutorial3);
		tutorial3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				new Tutorial(JLSInfo.frame,"tutorial3.html",false);
			}
		});
		JMenuItem tutorial4 = new JMenuItem("Sign Extension");
		String tip4 = "<html>This demonstrates how to bundle/unbundle&nbsp;&nbsp;<br>" +
		"and to use the signal copy element.";
		tutorial4.setToolTipText(tip4);
		tutorial.add(tutorial4);
		tutorial4.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				new Tutorial(JLSInfo.frame,"tutorial4.html",false);
			}
		});

		// set up javahelp
		String helpHS = "help/JLSHelp.hs";
		ClassLoader cl = JLSApplet.class.getClassLoader();
		HelpSet mainHS = null;
		try {
			URL url = HelpSet.findHelpSet(cl, helpHS);
			mainHS = new HelpSet(cl, url);
		}
		catch (Exception ex) {
			System.out.println("Help problem: " + ex.getMessage());
			return help;
		}
		JLSInfo.hb = mainHS.createHelpBroker();
		JMenuItem contents = new JMenuItem("Contents");
		help.add(contents);
		contents.addActionListener(new CSH.DisplayHelpFromSource(JLSInfo.hb));

		return help;
	} // end of helpMenu method

} // end of JLSApplet class
