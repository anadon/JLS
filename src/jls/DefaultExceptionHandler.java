package jls;

import java.nio.charset.StandardCharsets;

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
				System.out.println("Please attach it to a bug report at https://github.com/anadon/JLS/issues so it can be fixed.");
				System.exit(1);
			}
			
			// otherwise show message and quit
			else {
				saveTrace(th);
				String msg = "<html>" + 
					"UNEXPECTED INTERNAL ERROR! Try to save circuit(s)." + 
					"<p>" + 
					"JLS will create a file called JLSerror in the current folder/directory." +
					"<br>Please attach it to a bug report at https://github.com/anadon/JLS/issues so it can be fixed." +
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
			PrintWriter out = new PrintWriter("JLSerror", StandardCharsets.UTF_8);
			out.println("Please attach this file to a bug report at");
			out.println("https://github.com/anadon/JLS/issues along with a");
			out.println("short description of what you were doing when the");
			out.println("error occurred. Thanks.");
			out.println("JLS " + JLSInfo.versionString);
			// only environment facts that help diagnose a crash: the full
			// System.getProperties() dump leaked user/host details into a
			// file users are told to share (issue #51)
			for (String key : new String[] { "java.version", "java.vendor",
					"java.vm.name", "os.name", "os.version", "os.arch" }) {
				out.println(key + "=" + System.getProperty(key));
			}
			th.printStackTrace(out);
			if (circuit != null)
				circuit.save(out);
			out.close();
		}
		catch (IOException ex) {
			System.out.println("Can't create JLSerror file");
		}
	} // end of saveTrace method
	
} // end of DefaultExceptionHander class
