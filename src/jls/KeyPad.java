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
	/** The digit buttons, 0-9 (and a-f when the base is 16). */
	private JButton [] digits = new JButton[16];
	/** The button that resets the text field to the default value. */
	private JButton reset = new JButton("C");
	/** The button that hides the keypad window. */
	private JButton hide = new JButton(); 
	/** The text field the keypad fills. */
	private JTextField target;
	/** The number base (10 or 16), hence how many digit buttons work. */
	private int base;
	/** The value the reset button puts back into the text field. */
	private long defaultValue;
	/** True if the next digit replaces the text instead of appending. */
	private boolean firstChange = true;
	/** The undecorated window containing the keypad buttons. */
	private JDialog win;
	/** True when the keypad window is showing. */
	private boolean winVisible = false;
	/** When the keypad was last auto-hidden by focus loss, in milliseconds. */
	private long autoHidden = 0;
	/** The document filter restricting the text field to digits in the base. */
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
		URL down = KeyPad.class.getResource("images/down.gif");
		setIcon(new ImageIcon(down));
		setBackground(Color.WHITE);
		URL up = KeyPad.class.getResource("images/up.gif");
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
		
		// set up GUI: an owned window, so it stays attached to the
		// dialog it belongs to on every platform (#104)
		win = new JDialog(f);
		win.setUndecorated(true);
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

		// standard dismissal (#86): Escape closes the keypad ...
		ActionListener hideAction = new ActionListener() {
			/**
			 * Dismiss the keypad when Escape is pressed.
			 *
			 * @param event The event object for actions.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				hideKeyPad();
			}
		};
		win.getRootPane().registerKeyboardAction(hideAction,
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		// ... and so does clicking anywhere outside it (focus loss)
		win.addWindowFocusListener(new WindowAdapter() {
			/**
			 * Hide the keypad when focus moves outside it, recording when so
			 * a click on the owning button does not immediately reopen it.
			 *
			 * @param event The window event describing the focus loss.
			 */
			@Override
			public void windowLostFocus(WindowEvent event) {
				if (winVisible) {
					hideKeyPad();
					autoHidden = System.currentTimeMillis();
				}
			}
		});

	} // end of constructor

	/**
	 * Hide the keypad window.
	 */
	private void hideKeyPad() {

		win.setVisible(false);
		winVisible = false;
	} // end of hideKeyPad method
	
	/**
	 * React to button pushed.
	 * 
	 * @param event The event object for actions.
	 */
	@Override
	public void actionPerformed(ActionEvent event) {
		
		if (event.getSource() == this) {
			if (winVisible) {
				hideKeyPad();
			}
			else if (event.getWhen()-autoHidden > 250) {
				// place just below this button (owner-relative; #104);
				// the time check keeps a click on this button from
				// reopening the keypad it just dismissed by focus loss
				win.setLocationRelativeTo(this);
				Point p = win.getLocation();
				p.translate(0,(getHeight()+win.getHeight())/2);
				win.setLocation(p);
				winVisible = true;
				win.setVisible(true);
			}
			return;
		}

		if (event.getSource() == hide) {
			hideKeyPad();
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
