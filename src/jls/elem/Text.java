package jls.elem;

import jls.*;

import java.awt.*;
import java.util.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Put text into the circuit.
 * Has nothing to do with simulation, used simply to annotate the circuit.
 * 
 * @author David A. Poplawski
 */
public class Text extends DisplayElement {

	// named constants
	private final int size = 500;	// width and height of dialog

	// saved properties
	private String text = "";
	private String fontName = "";
	private int fontSize = 0;
	private boolean isBold = false;
	private boolean isItalic = false;
	private Color color = Color.black;

	// running properties
	private Vector<String> lines = new Vector<String>();
	private boolean cancelled = false;
	private boolean changed;

	/**
	 * Create a new Text element.
	 * 
	 * @param circuit The circuit this element will be part of.
	 */
	public Text(Circuit circuit) {

		super(circuit);
	} // end of constructor

	/**
	 * Display dialog to get text info.
	 * 
	 * @param g The Graphics object to use.
	 * @param editWindow The window this element will be displayed in.
	 * @param x The x-coordinate of the last known mouse position.
	 * @param y The y-coordinate of the last known mouse position.
	 * 
	 * @return true if there is text, false if not.
	 */
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {

		// show creation dialog
		cancelled = false;
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		if (pos == null) {
			text = new TextEdit(x+win.x,y+win.y,true).getText();
		}
		else {
			text = new TextEdit(pos.x+win.x,pos.y+win.y,true).getText();
		}

		// if cancelled, return
		if (cancelled)
			return false;

		// if no text, return
		if (text.length() == 0)
			return false;

		// complete initialization
		init(g);

		// set position
		Point p = MouseInfo.getPointerInfo().getLocation();
		p.x -= win.x;
		p.y -= win.y;
		if (p != null)
			super.setXY(p.x-width/2,p.y-height/2);

		return true;

	} // end of init method

	/**
	 * Initialize internal info for this element.
	 * Figures out height and width using font info from graphics object.
	 * 
	 * @param g The Graphics object to use.
	 */
	public void init(Graphics g) {

		// first split lines
		lines.clear();
		String str = "";
		for (int p=0; p<text.length(); p+=1) {
			char c = text.charAt(p);
			if (c == '\n') {
				lines.add(str);
				str = "";
			}
			else {
				str += c;
			}
		}
		if (!str.equals("")) {
			lines.add(str);
		}

		// if no graphics object, done
		if (g == null) {
			return;
		}

		// initialize font info defaults if not set from file
		if (fontName.equals("")) {
			fontName = g.getFont().getFamily();
		}
		if (fontSize == 0) {
			fontSize = g.getFont().getSize();
		}
		int bi = 0;
		if (isBold) bi |= Font.BOLD;
		if (isItalic) bi |= Font.ITALIC;
		Font f = new Font(fontName,bi,fontSize);
		Graphics gg = g.create();
		gg.setFont(f);

		// get info for bounding rectangle
		FontMetrics fm = gg.getFontMetrics();
		int textHeight = fm.getAscent() + fm.getDescent();
		width = 0;
		height = 0;
		for (String line : lines) {
			if (fm.stringWidth(line) > width)
				width = fm.stringWidth(line);
			height += textHeight;
		}

	} // end of init method

	/**
	 * Set a string instance variable value (during a load).
	 * 
	 * @param name The name of the instance variable.
	 * @param value The value to set it to.
	 */
	public void setValue(String name, String value) {

		if (name.equals("text")) {
			text = value;
		}
		else if (name.equals("fn")) {
			fontName = value;
		}
		super.setValue(name,value);
	} // end of setValue method

	/**
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The name of the instance variable.
	 * @param value The value to set it to.
	 */
	public void setValue(String name, int value) {

		if (name.equals("fs")) {
			fontSize = value;
		}
		else if (name.equals("bold")) {
			isBold = value == 1;
		}
		else if (name.equals("ital")) {
			isItalic = value == 1;
		}
		else if (name.equals("color")) {
			color = new Color(value);
		}
		super.setValue(name,value);
	} // end of setValue method

	/**
	 * Make a copy of this element.
	 * 
	 * @return an exact copy of this element.
	 */
	public Text copy() {

		Text it = new Text(circuit);
		super.copy(it);
		it.text = text;
		it.fontName = fontName;
		it.fontSize = fontSize;
		it.isBold = isBold;
		it.isItalic = isItalic;
		it.color = color;
		for (String line : lines) {
			it.lines.add(line);
		}
		return it;
	} // end of copy method

	/**
	 * Save this element in a file.
	 * 
	 * @param output A print writer to write to.
	 */
	public void save(PrintWriter output) {

		output.println("ELEMENT Text");
		super.save(output);
		String str = text.replace("\\","\\\\");
		str = str.replace("\"","\\\"");
		str = str.replace("\n","\\n");
		output.println(" String text \"" + str + "\"");
		output.println(" String fn \"" + fontName + "\"");
		output.println(" int fs " + fontSize);
		output.println(" int bold " + (isBold ? 1 : 0));
		output.println(" int ital " + (isItalic ? 1 : 0));
		output.println(" int color " + color.getRGB());
		output.println("END");
	} // end of save method

	/**
	 * Draw this element.
	 * 
	 * @param g The Graphics element to draw with.
	 */
	public void draw(Graphics g) {

		// save current graphics object
		Graphics myg = g.create();

		// draw the text
		int bi = 0;
		if (isBold) bi |= Font.BOLD;
		if (isItalic) bi |= Font.ITALIC;
		myg.setFont(new Font(fontName,bi,fontSize));
		FontMetrics fm = myg.getFontMetrics();
		int ascent = fm.getAscent();
		int descent = fm.getDescent();
		int height = ascent + descent;
		this.height = 0;
		this.width = 0;
		for (String str : lines) {
			this.height += height;
			this.width = Math.max(this.width,fm.stringWidth(str));
		}

		super.draw(g);
		int y = this.y + ascent;
		myg.setColor(color);
		for (String str : lines) {
			myg.drawString(str,x,y);
			y += height;
		}

	} // end of draw method

	/**
	 * Text areas can be changed.
	 * 
	 * @return true.
	 */
	public boolean canChange() {

		return true;
	} // end of canChange method

	/**
	 * Show edit dialog.
	 * Make user re-place element if any changes are made.
	 * 
	 * @param g The Graphics object to use to determine size.
	 * @param editWindow The editor window this element is in.
	 * @param x The current x-coordinate of the mouse.
	 * @param y The current y-coordinate of the mouse.
	 * 
	 * @return true if element must be re-placed in the circuit, false if not.
	 */
	public boolean change(Graphics g, JPanel editWindow, int x, int y) {

		// show dialog
		cancelled = false;
		changed = false;
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		TextEdit ed = null;
		if (pos == null) {
			ed = new TextEdit(x+win.x,y+win.y,false);
		}
		else {
			ed = new TextEdit(pos.x+win.x,pos.y+win.y,false);
		}

		// if cancelled, return
		if (cancelled)
			return false;

		// if no changes, just return
		if (text.equals(ed.getText()) && !changed)
			return false;

		// otherwise force replace
		text = ed.getText();
		width = 0;
		height = 0;
		init(g);
		return true;
	} // end of change method

	/**
	 * Dialog to get text information from user.
	 */
	private class TextEdit extends JDialog implements ActionListener {

		// GUI elements
		private JComboBox fonts;
		private String [] fontSizes = {"8","9","10","11","12","13","14","15","16","17","18","19","20","24","28","32","36","40","48","56","64","72"};
		private JComboBox fontSz = new JComboBox(fontSizes);
		private JRadioButton bold = new JRadioButton("Bold");
		private JRadioButton italic = new JRadioButton("Italic");
		private JButton colorButton = new JButton("Color");
		private JTextArea textArea = new JTextArea();
		private JButton ok = new JButton("OK");
		private JButton cancel = new JButton("Cancel");

		// properties
		private String result = "";
		private String fn = "";
		private int fs = 0;
		private boolean isB = false;
		private boolean isI = false;
		private Color col = Color.black;

		/**
		 * Initialize the dialog at a given position.
		 * 
		 * @param x The x-coordinate of the upper left of the dialog box.
		 * @param y The y-coordinate of the upper left of the dialog box.
		 * @param creating True if creating, false if changing.
		 */
		public TextEdit(int x, int y, boolean creating) {

			super(JLSInfo.frame,"Create/Modify Text Element",true);

			// set up GUI
			Container window = getContentPane();
			window.setLayout(new BorderLayout());

			// set up font inputs
			JPanel details = new JPanel(new FlowLayout());

			// set up font name
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			String [] names = ge.getAvailableFontFamilyNames();
			if (fontName.equals("")) {
				Font f = textArea.getFont();
				fn = f.getFamily();
			}
			else {
				fn = new String(fontName);
			}
			fonts = new JComboBox(names);
			fonts.setSelectedItem(fn);
			details.add(new JLabel("Font:"));
			details.add(fonts);

			if (fontSize == 0) {
				fs = textArea.getFont().getSize();
			}
			else {
				fs = fontSize;
			}
			fontSz.setSelectedItem(fs+"");
			fontSz.setEditable(true);
			details.add(new JLabel("Size:"));
			details.add(fontSz);

			if (isBold) {
				bold.setSelected(true);
				isB = true;
			}
			details.add(bold);

			if (isItalic) {
				italic.setSelected(true);
				isI = true;
			}
			details.add(italic);

			details.add(colorButton);

			fonts.addActionListener(this);
			fontSz.addActionListener(this);
			bold.addActionListener(this);
			italic.addActionListener(this);
			colorButton.addActionListener(this);
			window.add(details,BorderLayout.NORTH);
			if (!creating) {
				textArea.setText(text);
				int bi = 0;
				if (isBold) bi |= Font.BOLD;
				if (isItalic) bi |= Font.ITALIC;
				textArea.setFont(new Font(fontName,bi,fontSize));
				textArea.setForeground(color);
			}
			JScrollPane pane = new JScrollPane(textArea);
			pane.setPreferredSize(new Dimension(size,size));
			window.add(pane, BorderLayout.CENTER);
			JPanel buttons = new JPanel();
			buttons.setLayout(new GridLayout(1,3));
			buttons.add(ok);
			buttons.add(cancel);
			ok.setBackground(Color.green);
			cancel.setBackground(Color.pink);
			JButton help = new JButton("Help");
			if (JLSInfo.hb == null)
				Util.noHelp(help);
			else
				JLSInfo.hb.enableHelpOnButton(help,"text",null);
			buttons.add(help);
			window.add(buttons, BorderLayout.SOUTH);

			// add listeners
			ok.addActionListener(this);
			cancel.addActionListener(this);
			
			// make the text area get the focus
			this.addWindowFocusListener(new WindowAdapter() {
			    public void windowGainedFocus(WindowEvent e) {
			        textArea.requestFocusInWindow();
			    }
			});

			// make it visible
			pack();
			setLocation(x-size/2,y-size/2);
			setVisible(true);
		} // end of constructor

		/**
		 * React to buttons.
		 * 
		 * @param event The event object for this event.
		 */
		public void actionPerformed(ActionEvent event) {

			if (event.getSource() == fonts) {
				fn = (String)fonts.getSelectedItem();
				int bi = 0;
				if (isB) bi |= Font.BOLD;
				if (isI) bi |= Font.ITALIC;
				textArea.setFont(new Font(fn,bi,fs));
				changed = true;
				return;
			}
			else if (event.getSource() == fontSz) {
				try {
					fs = Integer.parseInt((String)fontSz.getSelectedItem());
				}
				catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(this, "Invalid Font Size");
					return;
				}
				if (fs < 1) {
					JOptionPane.showMessageDialog(this, "Invalid Font Size");
					return;
				}
				int bi = 0;
				if (isB) bi |= Font.BOLD;
				if (isI) bi |= Font.ITALIC;
				textArea.setFont(new Font(fn,bi,fs));
				changed = true;
				return;
			}
			else if (event.getSource() == bold) {
				isB = bold.isSelected();
				int bi = 0;
				if (isB) bi |= Font.BOLD;
				if (isI) bi |= Font.ITALIC;
				textArea.setFont(new Font(fn,bi,fs));
				changed = true;
				return;
			}
			else if (event.getSource() == italic) {
				isI = italic.isSelected();
				int bi = 0;
				if (isB) bi |= Font.BOLD;
				if (isI) bi |= Font.ITALIC;
				textArea.setFont(new Font(fn,bi,fs));
				changed = true;
				return;
			}
			else if (event.getSource() == colorButton) {
				final JColorChooser ch = new JColorChooser(color);
				ch.setPreviewPanel(new JPanel());
				ActionListener ok = new ActionListener(){public void actionPerformed(ActionEvent event) {
					col = ch.getColor();
					textArea.setForeground(col);
					changed = true;
				}};
				JDialog cl = JColorChooser.createDialog(this, "pick", true, ch, ok, null);
				cl.setVisible(true);
				cl.dispose();
				return;
			}
			else if (event.getSource() == ok) {
				result = textArea.getText();
				if (changed) {
					fontName = new String(fn);
					fontSize = fs;
					isBold = isB;
					isItalic = isI;
					color = col;
				}
			}
			else if (event.getSource() == cancel) {
				cancelled = true;
			}
			dispose();
		} // end of actionPerformed method

		/**
		 * Get text input.
		 * 
		 * @return The text entered into the text area.
		 */
		public String getText() {

			return result;
		} // end of getText method

	} // end of TextEdit class

} // end of Text method
