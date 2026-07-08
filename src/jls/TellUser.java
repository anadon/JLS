/**
 * 
 */
package jls;

import java.awt.GraphicsEnvironment;

import javax.swing.JOptionPane;

/**
 * @author Josh Marshall
 *
 * Just a more light-weight way to notify a user of some events
 * while also keeping code smaller and having more regular
 * output.
 * 
 * Intended for anticipated issues, not unexpected events intended for
 * the DefaultExceptionHandler class
 * 
 */
public class TellUser {
	
	/**
	 * 
	 * @param message
	 * 			The message to tell the user.
	 * 
	 * @param popup
	 * 			The reference needed to allow for a pop-up;
	 * 			if false, this will only print to the console
	 */
	/**
	 * Whether a pop-up can actually be shown. In batch/headless mode a
	 * dialog attempt throws HeadlessException, bypassing the System.exit
	 * that follows and turning a user error into a crash report (#42).
	 */
	static private boolean canPopup() {
		return !GraphicsEnvironment.isHeadless();
	}

	static public void note(String message, boolean popup ){
		System.out.println("NOTE: " + message);
		if(popup && canPopup())
			JOptionPane.showMessageDialog(null, message, "NOTE",
				JOptionPane.PLAIN_MESSAGE);
	}

	static public void warn(String message, boolean popup){
		System.out.println("WARN: " + message);
		if(popup && canPopup())
			JOptionPane.showMessageDialog(null, message, "WARNING",
				JOptionPane.WARNING_MESSAGE);
	}

	static public void err(String message, boolean popup){
		System.err.println("ERROR: " + message);
		if(popup && canPopup())
			JOptionPane.showMessageDialog(null, message, "ERROR",
				JOptionPane.ERROR_MESSAGE);
	}
}
