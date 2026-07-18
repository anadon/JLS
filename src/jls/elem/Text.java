package jls.elem;

import jls.*;
import jls.util.Placement;

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
	@Override
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {

		// show creation dialog
		cancelled = false;
		text = new TextEdit(true).getText();

		// if cancelled, return
		if (cancelled)
			return false;

		// if no text, return
		if (text.length() == 0)
			return false;

		// complete initialization
		init(g);

		// set position
		Point p = Placement.dropPoint(editWindow,x,y,width,height);
		super.setXY(p.x,p.y);

		return true;

	} // end of init method

	/**
	 * Initialize internal info for this element.
	 * Figures out height and width using font info from graphics object.
	 * 
	 * @param g The Graphics object to use.
	 */
	@Override
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
		if (!str.isEmpty()) {
			lines.add(str);
		}

		// if no graphics object, done
		if (g == null) {
			return;
		}

		// initialize font info defaults if not set from file
		if (fontName.isEmpty()) {
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

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.StringAttribute("text") {
			/**
			 * Get the text string from the given element.
			 *
			 * @param el The element to read from.
			 * @return The element's text.
			 */
			@Override
			protected String get(Element el) { return ((Text)el).text; }
			/**
			 * Set the text string on the given element.
			 *
			 * @param el The element to write to.
			 * @param v The new text value.
			 */
			@Override
			protected void set(Element el, String v) { ((Text)el).text = v; }
		},
		new Attribute.StringAttribute("fn") {
			/**
			 * Get the font name from the given element.
			 *
			 * @param el The element to read from.
			 * @return The element's font name.
			 */
			@Override
			protected String get(Element el) { return ((Text)el).fontName; }
			/**
			 * Set the font name on the given element.
			 *
			 * @param el The element to write to.
			 * @param v The new font name.
			 */
			@Override
			protected void set(Element el, String v) { ((Text)el).fontName = v; }
		},
		new Attribute.IntAttribute("fs") {
			/**
			 * Get the font size from the given element.
			 *
			 * @param el The element to read from.
			 * @return The element's font size.
			 */
			@Override
			protected int get(Element el) { return ((Text)el).fontSize; }
			/**
			 * Set the font size on the given element.
			 *
			 * @param el The element to write to.
			 * @param v The new font size.
			 */
			@Override
			protected void set(Element el, int v) { ((Text)el).fontSize = v; }
		},
		new Attribute.IntAttribute("bold") {
			/**
			 * Get the bold flag from the given element as 1 or 0.
			 *
			 * @param el The element to read from.
			 * @return 1 if bold, 0 if not.
			 */
			@Override
			protected int get(Element el) { return ((Text)el).isBold ? 1 : 0; }
			/**
			 * Set the bold flag on the given element from 1 or 0.
			 *
			 * @param el The element to write to.
			 * @param v 1 to make bold, anything else to clear it.
			 */
			@Override
			protected void set(Element el, int v) { ((Text)el).isBold = v == 1; }
		},
		new Attribute.IntAttribute("ital") {
			/**
			 * Get the italic flag from the given element as 1 or 0.
			 *
			 * @param el The element to read from.
			 * @return 1 if italic, 0 if not.
			 */
			@Override
			protected int get(Element el) { return ((Text)el).isItalic ? 1 : 0; }
			/**
			 * Set the italic flag on the given element from 1 or 0.
			 *
			 * @param el The element to write to.
			 * @param v 1 to make italic, anything else to clear it.
			 */
			@Override
			protected void set(Element el, int v) { ((Text)el).isItalic = v == 1; }
		},
		new Attribute.IntAttribute("color") {
			/**
			 * Get the color from the given element as a packed RGB int.
			 *
			 * @param el The element to read from.
			 * @return The element's color as an RGB integer.
			 */
			@Override
			protected int get(Element el) { return ((Text)el).color.getRGB(); }
			/**
			 * Set the color on the given element from a packed RGB int.
			 *
			 * @param el The element to write to.
			 * @param v The new color as an RGB integer.
			 */
			@Override
			protected void set(Element el, int v) { ((Text)el).color = new Color(v); }
		}
	);

	private static final java.util.List<Attribute> ALL_ATTRIBUTES =
			concatAttributes(OWN_ATTRIBUTES);

	/**
	 * Base attributes plus this element's own, in save order (#23).
	 */
	@Override
	protected java.util.List<Attribute> savedAttributes() {

		return ALL_ATTRIBUTES;
	} // end of savedAttributes method

	/**
	 * Make a copy of this element.
	 *
	 * @return an exact copy of this element.
	 */
	@Override
	public Text copy() {

		Text it = new Text(circuit);
		super.copy(it);
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
	@Override
	public void save(PrintWriter output) {

		output.println("ELEMENT Text");
		super.save(output);
		output.println("END");
	} // end of save method

	/**
	 * Draw this element.
	 * 
	 * @param g The Graphics element to draw with.
	 */
	@Override
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
	@Override
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
	@Override
	public boolean change(Graphics g, JPanel editWindow, int x, int y) {

		// show dialog
		cancelled = false;
		changed = false;
		TextEdit ed = new TextEdit(false);

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
	@SuppressWarnings("serial")
	private class TextEdit extends ElementDialog implements ActionListener {

		// GUI elements
		private JComboBox<String> fonts;
		private String [] fontSizes = {"8","9","10","11","12","13","14","15","16","17","18","19","20","24","28","32","36","40","48","56","64","72"};
		private JComboBox<String> fontSz = new JComboBox<String>(fontSizes);
		private JRadioButton bold = new JRadioButton("Bold");
		private JRadioButton italic = new JRadioButton("Italic");
		private JButton colorButton = new JButton("Color");
		private JTextArea textArea = new JTextArea();

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
		 * @param creating True if creating, false if changing.
		 */
		public TextEdit(boolean creating) {

			super("Create/Modify Text Element","text");

			// set up GUI
			Container window = getContentPane();

			// set up font inputs
			JPanel details = new JPanel(new FlowLayout());

			// set up font name
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			String [] names = ge.getAvailableFontFamilyNames();
			if (fontName.isEmpty()) {
				Font f = textArea.getFont();
				fn = f.getFamily();
			}
			else {
				fn = fontName;
			}
			fonts = new JComboBox<String>(names);
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
			window.add(details);
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
			window.add(pane);

			// make the text area get the focus
			this.addWindowFocusListener(new WindowAdapter() {
			    /**
			     * Move focus to the text area when the dialog gains focus.
			     *
			     * @param e The window focus event.
			     */
			    @Override
			    public void windowGainedFocus(WindowEvent e) {
			        textArea.requestFocusInWindow();
			    }
			});

			finishDialog();
		} // end of constructor

		/**
		 * Accept the entered text.
		 */
		@Override
		protected void validateAndAccept() {

			result = textArea.getText();
			if (changed) {
				fontName = fn;
				fontSize = fs;
				isBold = isB;
				isItalic = isI;
				color = col;
			}
			dispose();
		} // end of validateAndAccept method

		/**
		 * Cancel this text element.
		 */
		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

		/**
		 * React to buttons.
		 * 
		 * @param event The event object for this event.
		 */
		@Override
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
					TellUser.error(this, "Invalid Font Size", "Error");
					return;
				}
				if (fs < 1) {
					TellUser.error(this, "Invalid Font Size", "Error");
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
				ActionListener ok = new ActionListener(){
				/**
				 * Apply the chosen color to the text and mark it changed.
				 *
				 * @param event The event from the color chooser's OK button.
				 */
				@Override public void actionPerformed(ActionEvent event) {
					col = ch.getColor();
					textArea.setForeground(col);
					changed = true;
				}};
				JDialog cl = JColorChooser.createDialog(this, "pick", true, ch, ok, null);
				cl.setVisible(true);
				cl.dispose();
			}
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
