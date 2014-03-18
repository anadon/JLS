package jls.elem;

import jls.*;
import jls.sim.*;

import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.math.BigInteger;
import java.util.*;

/**
 * Generate test inputs from a file.
 * This is used only in batch mode as a replacement for a signal generator.
 * It cannot be created by the editor.
 * 
 * @author David A. Poplawski
 */
public class TestGen extends SigSim {
	
	// properties
	private String file = null;
	
	/**
	 * Create new element.
	 * 
	 * @param circuit The circuit this element is in.
	 */
	public TestGen(Circuit circuit) {
		
		super(circuit);
	} // end of constructor
	
	/**
	 * Initialize this element.
	 * 
	 * @param g Unused.
	 */
	public void init(Graphics g) {
		
	} // end of init method
	
	/**
	 * Set test input file name.
	 * 
	 * @param name Test input file name.
	 */
	public void setFile(String name) {
		
		file = name;
	} // end of setFile method
	
//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	/**
	 * Read and parse signal file, post all events.
	 * 
	 * @param sim The simulator.
	 */
	public void initSim(Simulator sim) {
		
		// open test file
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
		}
		catch (FileNotFoundException ex) {
			if (JLSInfo.batch) {
				System.out.println("Can't open test file: " + file);
				System.exit(1);
			}
			else {
				JOptionPane.showMessageDialog(JLSInfo.frame,"Can't open test file: " + file);
				return;
			}
		}
		Scanner input = new Scanner(in);
		super.initSim(sim,input);
	} // end of initSim method
	
	/**
	 * Print or display an error about the test file contents.
	 * 
	 * @param msg An error message.
	 */
	protected void specError(String msg) {
		
		if (JLSInfo.batch && JLSInfo.frame == null && !JLSInfo.isApplet) {
			System.out.println("error in test file");
			System.out.println(msg);
			System.exit(1);
		}
		else {
			JOptionPane.showMessageDialog(JLSInfo.frame,"error in test file: " + msg);
			return;
		}
	} // end of specError method
	
} // end of TestGen class
