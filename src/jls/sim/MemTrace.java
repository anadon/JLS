package jls.sim;

import jls.elem.*;
import java.awt.*;
import javax.swing.*;

/**
 * Display memory read/write info in a popup window.
 * 
 * @author David A. Poplawski
 */
public final class MemTrace extends JFrame {
	
	// named constants
	private int WIDTH = 300;
	private int HEIGHT = 200;
	
	// properties
	private Memory mem;
	private JTextArea info = new JTextArea();
	
	/**
	 * Set up memory trace window.
	 *
	 * @param mem A reference to the memory element this will display.
	 */
	public MemTrace(Memory mem) {
		
		super(mem.getName() + " write trace");
		this.mem = mem;
		setSize(WIDTH,HEIGHT);
		setLayout(new BorderLayout());
		add(new JScrollPane(info),BorderLayout.CENTER);
	} // end of constructor
	
	/**
	 * Make the window visible.
	 *
	 * @param loc Where to put the window.
	 */
	public void showit(Point loc) {
		
		setLocation(loc);
		setVisible(true);
	} // end of showit method
	
	/**
	 *
	 */
	public void update() {
		
		info.setText(mem.getActivityTrace());
	} // end of update method
	
} // end of MemTrace class
