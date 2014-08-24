package jls;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

/**
 * Display dialog showing version and copywrite information about JLS.
 * 
 * @author David A. Poplawski
 */
public final class About extends JDialog implements ActionListener {

	/**
	 * Show information about JLS.
	 */
	public About() {
		
		super((Frame)null,"About JLS");
		
		// set up labels
		Container window = getContentPane();
		window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));
		String line1 = "<h1>&nbsp;JLS Version " + JLSInfo.vers + "." + 
		JLSInfo.release + "." + JLSInfo.buildNum + "&nbsp;</h1>";
		String line2 = "<p>Copyright " + JLSInfo.year;
		String line3 = "<p>David A. Poplawski & Michigan Technological University";
		String line4 = "<p>All Rights Reserved";
		String html = "<html><center>" + line1 + line2 + line3 + line4 +
			JLSInfo.build + "</center></html>";
		JLabel info = new JLabel(html);
		info.setAlignmentX(Component.CENTER_ALIGNMENT);
		window.add(info);
		
		// set up close button
		JLabel s1 = new JLabel(" ");
		s1.setAlignmentX(Component.CENTER_ALIGNMENT);
		window.add(s1);
		JButton close = new JButton("OK");
		close.setAlignmentX(Component.CENTER_ALIGNMENT);
		window.add(close);
		close.addActionListener(this);
		JLabel s2 = new JLabel(" ");
		s2.setAlignmentX(Component.CENTER_ALIGNMENT);
		window.add(s2);
		
		// make visible
		pack();
		Dimension winSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension aboutSize = getSize();
		setLocation((winSize.width-aboutSize.width)/2,(winSize.height-aboutSize.height)/2);
		setVisible(true);
		
	} // end of constructor
	
	/**
	 * Close dialog.
	 * 
	 * @param event Unused.
	 */
	public void actionPerformed(ActionEvent event) {
		
		dispose();
	} // end of actionPerformed method
	
} // end of About class
 