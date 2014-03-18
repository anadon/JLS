package jls;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.*;
import java.net.*;

/**
 * Implements keypad for inputing numbers.
 * 
 * @author David A. Poplawski
 */
public final class KeyPad extends JButton implements ActionListener {
	
	// properties
	private JButton [] digits = new JButton[16];
	private JButton reset = new JButton("C");
	private JButton hide = new JButton(); 
	private JTextField target;
	private int base;
	private long defaultValue;
	private boolean firstChange = true;
	private JWindow win;
	private boolean winVisible = false;
	TextFilter filter;
	
	/**
	 * Create a new KeyPad object.
	 * 
	 * @param target The text field the keypad will fill.
	 * @param digitKeys The number of digits in the keypad (10 or 16 only);
	 * @param defaultValue The default value for this field.
	 * @param f The window this is in.
	 */
	public KeyPad(JTextField target, int digitKeys, long defaultValue, final Window f) {
		
		// initialize
		URL down = getClass().getResource("images/down.gif");
		setIcon(new ImageIcon(down));
		setBackground(Color.WHITE);
		URL up = getClass().getResource("images/up.gif");
		hide.setIcon(new ImageIcon(up));
		
		
		// save parameters
		this.target = target;
		this.base = digitKeys;
		this.defaultValue = defaultValue;
		
		// create buttons
		for (int i=0; i<=9; i+=1) {
			digits[i] = new JButton(i+"");
		}
		digits[10] = new JButton("a");
		digits[11] = new JButton("b");
		digits[12] = new JButton("c");
		digits[13] = new JButton("d");
		digits[14] = new JButton("e");
		digits[15] = new JButton("f");
		
		// set up GUI
		win = new JWindow(f);
		final Container window = win.getContentPane();
		if (base == 16) {
			window.setLayout(new GridLayout(6,3));
		}
		else if (base == 10) {
			window.setLayout(new GridLayout(4,3));
		}
		if (base == 16) {
			window.add(digits[13]);
			window.add(digits[14]);
			window.add(digits[15]);
			window.add(digits[10]);
			window.add(digits[11]);
			window.add(digits[12]);
		}
		window.add(digits[7]);
		window.add(digits[8]);
		window.add(digits[9]);
		window.add(digits[4]);
		window.add(digits[5]);
		window.add(digits[6]);
		window.add(digits[1]);
		window.add(digits[2]);
		window.add(digits[3]);
		window.add(reset);
		window.add(digits[0]);
		window.add(hide);
		
		// set up listeners
		for (int i=0; i<base; i+=1) {
			digits[i].addActionListener(this);
		}
		reset.addActionListener(this);
		hide.addActionListener(this);
		
		// set up filter
		AbstractDocument doc = (AbstractDocument)(target.getDocument());
		filter = new TextFilter(target);
		doc.setDocumentFilter(filter);
		
		// finish keypad window
		win.pack();
		addActionListener(this);
		
		// set up to make keypad follow dialog when the dialog is moved
		ComponentAdapter list = new ComponentAdapter() {
			private Point then = null;
			public void componentMoved(ComponentEvent event) {
				if (then == null) {
					then = f.getLocation();
				}
				else {
					Point now = f.getLocation();
					int dx = now.x - then.x;
					int dy = now.y - then.y;
					Point winloc = win.getLocation();
					winloc.translate(dx,dy);
					win.setLocation(winloc);
					then = now;
				}
			}
		};
		f.addComponentListener(list);
		
		// make right click anywhere close window
		MouseAdapter hide = new MouseAdapter() {
			public void mousePressed(MouseEvent event) {
				if (event.getButton() == MouseEvent.BUTTON3) {
					win.setVisible(false);
					winVisible = false;
				}
			}
		};
		for (int i=0; i<base; i+=1) {
			digits[i].addMouseListener(hide);
		}
		reset.addMouseListener(hide);
		
	} // end of constructor
	
	/**
	 * React to button pushed.
	 * 
	 * @param event The event object for actions.
	 */
	public void actionPerformed(ActionEvent event) {
		
		if (event.getSource() == this) {
			Point p = this.getLocationOnScreen();
			Dimension d = this.getSize();
			win.setLocation(p.x,p.y+d.height);
			winVisible = !winVisible;
			win.setVisible(winVisible);
			return;
		}
		
		if (event.getSource() == hide) {
			win.setVisible(false);
			winVisible = false;
		}
		
		// handle reset
		if (event.getSource() == reset) {
			target.setText(Long.toString(defaultValue,base));
			firstChange = true;
			return;
		}
		
		// find out which button caused the event
		for (int i=0; i<base; i+=1) {
			if (event.getSource() == digits[i]) {
				
				// append digit to string
				if (firstChange) {
					target.setText(Integer.toString(i,base));
					firstChange = false;
				}
				else {
					target.setText(target.getText() + Integer.toString(i,base));
				}
				return;
			}
		}
		
	} // end of actionPerformed method
	
	/**
	 * Change the base.
	 * Will not change the appearance of the keypad, but will change what
	 * digits are allowed.
	 * 
	 * @param base The new base.
	 */
	public void setBase(int base) {
		
		this.base = base;
		filter.setBase(base);
	} // end of setBase method
	
	/**
	 * Value has been reset, so remember that any change will be the first change.
	 */
	public void reset() {
		
		firstChange = true;
	} // end of reset method
	
	/**
	 * Get rid of this keypad.
	 */
	public void close() {
		
		win.setVisible(false);
		win.dispose();
	} // end of close method
	
} // end of KeyPad class
