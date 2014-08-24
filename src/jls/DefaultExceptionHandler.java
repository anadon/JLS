package jls;

import javax.swing.JOptionPane;
import java.io.*;
import java.util.*;

/**
 * Handle unexpected errors and exceptions.
 * 
 * @author David A. Poplawski
 */
public final class DefaultExceptionHandler implements Thread.UncaughtExceptionHandler {
	
	// properties
	private boolean recovering = false;
	private JLSStart jls = null;
	private Circuit circuit = null;
	private int [] extraSpace = new int[10000];
	
	/**
	 * Save reference to JLS in case circuit(s) can be saved.
	 * 
	 * @param jls A reference to the main class.
	 */
	public void setJLS(JLSStart jls) {
		
		this.jls = jls;
	} // end of setJLS method
	
	/**
	 * Save reference to circuit for trace.
	 * 
	 * @param circ The circuit.
	 */
	public void setCircuit(Circuit circ) {
		
		circuit = circ;
	} // end of setCircuit method

	/**
	 * Show a dialog and print a stack trace to a file.
	 * While debugging print a stack trace to the console.
	 * 
	 * @param t Unused.
	 * @param th A throwable containing the exception/error information.
	 */
	public void uncaughtException(Thread t, Throwable th) {
		
		// if already trying to recover, forget it
		if (recovering) {
			System.exit(1);
		}
		recovering = true;
		
		// if out of memory
		if (th instanceof OutOfMemoryError) {
			
			// free up some memory and garbage collect
			extraSpace = null;
			System.gc();
			
			// if batch, print message and quit
			if (jls == null) {
				System.out.println("Not enough memory to simulate circuit");
				System.out.println("Run JLS again and give JVM more memory");
			}
			
			// otherwise display message and quit
			else {
				
				JOptionPane.showMessageDialog(null,
						"Not enough memory", "Error", JOptionPane.ERROR_MESSAGE);
				JOptionPane.showMessageDialog(null,
						"Run JLS again and give JVM more memory", "Warning",
						JOptionPane.WARNING_MESSAGE);
			}
			System.exit(1);
		}
		
		// all other errors/exceptions...
		else {
			
			// if batch, print message and quit
			if (jls == null) {
				saveTrace(th);
				System.out.println("UNEXPECTED INTERNAL ERROR!");
				System.out.println("JLS will create a file called JLSerror in the current folder/directory.");
				System.out.println("Please email it to pop@mtu.edu to report the error so it can be fixed.");
				System.exit(1);
			}
			
			// otherwise show message and quit
			else {
				saveTrace(th);
				String msg = "<html>" + 
					"UNEXPECTED INTERNAL ERROR! Try to save circuit(s)." + 
					"<p>" + 
					"JLS will create a file called JLSerror in the current folder/directory." +
					"<br>Please email it to pop@mtu.edu to report the error so it can be fixed." +
					"<br><br>Try restarting JLS using checkpoints of open circuits" +
					"<br>(i.e., <i>file</i>.jls~, where <i>file</i> is the name of the open circuit)" +
					"</html>";
				JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
		}
	} // end of uncaughtException method
	
	/**
	 * Save stack trace in a file.
	 * 
	 * @param th The throwable (exception/error).
	 */
	protected void saveTrace(Throwable th) {
		
		//th.printStackTrace(); // remove this when not debugging
		try {
			PrintWriter out = new PrintWriter("JLSerror");
			out.println("Please email this file to pop@mtu.edu");
			out.println("along with a short description of what");
			out.println("you were doing when the error occured.");
			out.println("Thanks.");
			out.printf("JLS %d.%d.%d %s\n", JLSInfo.vers, JLSInfo.release, JLSInfo.buildNum,
					JLSInfo.build);
			Properties prop = System.getProperties();
			prop.list(out);
			th.printStackTrace(out);
			if (circuit != null)
				circuit.save(out);
			out.close();
		}
		catch (FileNotFoundException ex) {
			System.out.println("Can't create JLSerror file");
		}
	} // end of saveTrace method
	
} // end of DefaultExceptionHander class
