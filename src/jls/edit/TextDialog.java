package jls.edit;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jls.TellUser;
import jls.elem.Element;
import jls.elem.Text;
import jls.util.Placement;

/**
 * GUI-side creation/edit dialog for {@link Text} (issue #77). Moved from
 * the former {@code Text.TextEdit} inner dialog + {@code Text.setup}/
 * {@code Text.change}, so the model stays headless. One instance is
 * registered for creation and one for change; the change instance's
 * {@code setup} return value is the {@code mustReplace} flag the editor
 * uses to force repositioning.
 */
public final class TextDialog implements ElementDialog {

	/** Width and height of the text edit dialog, in pixels. */
	private static final int SIZE = 500;

	/** True for the create dialog, false for the edit dialog. */
	private final boolean creating;

	/**
	 * Creates a {@code TextDialog}.
	 *
	 * @param creating True for the create dialog, false for the edit
	 *        dialog.
	 */
	public TextDialog(boolean creating) {
		this.creating = creating;
	} // end of constructor

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		Text t = (Text) el;

		if (creating) {
			// show creation dialog
			Form form = new Form(t, true);
			t.setText(form.getText());

			// if cancelled, return
			if (form.cancelled)
				return false;

			// if no text, return
			if (t.getText().length() == 0)
				return false;

			// complete initialization
			t.init(SwingTextMetrics.forGraphics(g));

			// set position
			Point p = Placement.dropPoint(editWindow, x, y,
					t.getWidth(), t.getHeight());
			t.setXY(p.x, p.y);

			return true;
		}

		// show change dialog
		Form ed = new Form(t, false);

		// if cancelled, return
		if (ed.cancelled)
			return false;

		// if no changes, just return
		if (t.getText().equals(ed.getText()) && !ed.changed)
			return false;

		// otherwise force replace
		t.setText(ed.getText());
		t.setWidth(0);
		t.setHeight(0);
		t.init(SwingTextMetrics.forGraphics(g));
		return true;
	} // end of setup method

	/**
	 * Dialog to get text information from user.
	 */
	@SuppressWarnings("serial")
	private static final class Form extends ElementFormDialog
			implements ActionListener {

		/** The text element being created or edited. */
		private final Text elem;
		/** True if the user cancelled the dialog. */
		private boolean cancelled = false;
		/** True if the user changed font, size, style or color. */
		private boolean changed = false;

		// GUI elements
		/** Font family selector. */
		private JComboBox<String> fonts;
		/** Choices offered by the font size selector. */
		private String [] fontSizes = {"8","9","10","11","12","13","14","15","16","17","18","19","20","24","28","32","36","40","48","56","64","72"};
		/** Font size selector. */
		private JComboBox<String> fontSz = new JComboBox<String>(fontSizes);
		/** Button selecting bold text. */
		private JRadioButton bold = new JRadioButton("Bold");
		/** Button selecting italic text. */
		private JRadioButton italic = new JRadioButton("Italic");
		/** Button that brings up the color chooser. */
		private JButton colorButton = new JButton("Color");
		/** Area the user types the text into. */
		private JTextArea textArea = new JTextArea();

		// properties
		/** The text as accepted by the dialog. */
		private String result = "";
		/** The currently selected font family name. */
		private String fn = "";
		/** The currently selected font size. */
		private int fs = 0;
		/** True if bold is currently selected. */
		private boolean isB = false;
		/** True if italic is currently selected. */
		private boolean isI = false;
		/** The currently chosen text color. */
		private Color col = Color.black;

		/**
		 * Initialize the dialog at a given position.
		 *
		 * @param elem The text element being created or edited.
		 * @param creating True if creating, false if changing.
		 */
		private Form(Text elem, boolean creating) {

			super("Create/Modify Text Element","text");

			this.elem = elem;

			// set up GUI
			Container window = getContentPane();

			// set up font inputs
			JPanel details = new JPanel(new FlowLayout());

			// set up font name
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			String [] names = ge.getAvailableFontFamilyNames();
			if (elem.getFontName().isEmpty()) {
				Font f = textArea.getFont();
				fn = f.getFamily();
			}
			else {
				fn = elem.getFontName();
			}
			fonts = new JComboBox<String>(names);
			fonts.setSelectedItem(fn);
			details.add(new JLabel("Font:"));
			details.add(fonts);

			if (elem.getFontSize() == 0) {
				fs = textArea.getFont().getSize();
			}
			else {
				fs = elem.getFontSize();
			}
			fontSz.setSelectedItem(fs+"");
			fontSz.setEditable(true);
			details.add(new JLabel("Size:"));
			details.add(fontSz);

			if (elem.isBold()) {
				bold.setSelected(true);
				isB = true;
			}
			details.add(bold);

			if (elem.isItalic()) {
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
				textArea.setText(elem.getText());
				int bi = 0;
				if (elem.isBold()) bi |= Font.BOLD;
				if (elem.isItalic()) bi |= Font.ITALIC;
				textArea.setFont(new Font(elem.getFontName(),bi,elem.getFontSize()));
				textArea.setForeground(new Color(elem.getColorRGB()));
			}
			JScrollPane pane = new JScrollPane(textArea);
			pane.setPreferredSize(new Dimension(SIZE,SIZE));
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
				elem.setFontName(fn);
				elem.setFontSize(fs);
				elem.setBold(isB);
				elem.setItalic(isI);
				elem.setColorRGB(col.getRGB());
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
				final JColorChooser ch = new JColorChooser(new Color(elem.getColorRGB()));
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
		private String getText() {

			return result;
		} // end of getText method

	} // end of Form class

} // end of TextDialog class
