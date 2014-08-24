package jls;

import java.awt.*;
import java.io.IOException;

import javax.swing.*;

/**
 * Display tutorial.
 * Also used to display the text that accompanies a demo in the Applet version.
 * 
 * @author David A. Poplawski
 */
public final class Tutorial extends JDialog {
	
	private final int width = 400;
	private final int height = 500;

	/**
	 * Create and display the dialog frame.
	 * 
	 * @param frame The parent frame for the dialog.
	 * @param name The name of the html file for the demo.
	 * @param isDemo False if tutorial, true if an Applet verson demo.
	 */
	public Tutorial(Frame frame, String name, boolean isDemo) {
		
		super(frame,isDemo?"JLS Demo":"JLS Tutorial",false);
		
		Container window = getContentPane();
		JEditorPane editorPane = new JEditorPane();
		editorPane.setEditable(false);
		
		java.net.URL helpURL = getClass().getResource("tutorial/" + name);
		if (helpURL != null) {
		    try {
		        editorPane.setPage(helpURL);
		    } catch (IOException e) {
		        System.err.println("Attempted to read a bad URL: " + helpURL);
		    }
		} else {
		    System.err.println("Couldn't find file: " + name);
		}

		JScrollPane editorScrollPane = new JScrollPane(editorPane);
		editorScrollPane.setPreferredSize(new Dimension(width, height));
		editorScrollPane.setMinimumSize(new Dimension(width, height));
		window.add(editorScrollPane);
		
		// finish up
		pack();
		setVisible(true);
	} // end of constructor
	
} // end of Tutorial class
