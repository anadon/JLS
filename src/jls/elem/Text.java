package jls.elem;

import jls.*;

import java.util.*;
import java.io.*;

/**
 * Put text into the circuit.
 * Has nothing to do with simulation, used simply to annotate the circuit.
 * 
 * @author David A. Poplawski
 */
public final class Text extends DisplayElement {

	// saved properties
	/** The text to display, lines separated by newlines. */
	private String text = "";
	/** The font family name; empty until set from a file or from defaults. */
	private String fontName = "";
	/** The font point size; 0 until set from a file or from defaults. */
	private int fontSize = 0;
	/** True if the text is drawn in bold. */
	private boolean isBold = false;
	/** True if the text is drawn in italics. */
	private boolean isItalic = false;
	/** The color the text is drawn in, as a packed ARGB int (opaque black
	 *  by default; #77 keeps the model free of {@code Color}). */
	private int colorRGB = 0xFF000000;

	// running properties
	/** The text split into individual display lines. */
	private Vector<String> lines = new Vector<String>();

	/**
	 * Create a new Text element.
	 * 
	 * @param circuit The circuit this element will be part of, or null
	 *            for a detached header/sentinel element.
	 */
	public Text(@org.jspecify.annotations.Nullable Circuit circuit) {

		super(circuit);
	} // end of constructor

	/**
	 * Initialize internal info for this element.
	 * Figures out height and width using font info from graphics object.
	 *
	 * <p>Issue #77: sizing is done through a {@link jls.core.TextMetrics}
	 * that also supplies named-font metrics (its {@link
	 * jls.core.TextMetrics.Provider} facet), so the model needs no
	 * AWT font types; the computation is byte-identical to
	 * the original.
	 *
	 * @param g The text metrics to size with (also a font Provider).
	 */
	@Override
	public void init(jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

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

		// if no metrics, done
		if (g == null) {
			return;
		}

		// the Text element chooses its own font family/style/size, so it
		// measures through the Provider facet of the supplied metrics
		jls.core.TextMetrics.Provider prov = (jls.core.TextMetrics.Provider) g;

		// initialize font info defaults if not set from file
		if (fontName.isEmpty()) {
			fontName = prov.defaultFontName();
		}
		if (fontSize == 0) {
			fontSize = prov.defaultFontSize();
		}
		jls.core.TextMetrics fm = prov.forFont(fontName, isBold, isItalic,
				fontSize);

		// get info for bounding rectangle
		int textHeight = fm.ascent() + fm.descent();
		width = 0;
		height = 0;
		for (String line : lines) {
			if (fm.stringWidth(line) > width)
				width = fm.stringWidth(line);
			height += textHeight;
		}

	} // end of init method

	/**
	 * The text string (issue #77: read/written by the GUI-side dialog).
	 *
	 * @return the text to display.
	 */
	public String getText() {

		return text;
	} // end of getText method

	/**
	 * Set the text string (issue #77: applied by the GUI-side dialog).
	 *
	 * @param text The new text to display.
	 */
	public void setText(String text) {

		this.text = text;
	} // end of setText method

	/**
	 * The font family name (issue #77: read by the GUI-side renderer and
	 * dialog).
	 *
	 * @return the font family name.
	 */
	public String getFontName() {

		return fontName;
	} // end of getFontName method

	/**
	 * Set the font family name (issue #77: applied by the GUI-side dialog).
	 *
	 * @param fontName The new font family name.
	 */
	public void setFontName(String fontName) {

		this.fontName = fontName;
	} // end of setFontName method

	/**
	 * The font point size (issue #77: read by the GUI-side renderer and
	 * dialog).
	 *
	 * @return the font point size.
	 */
	public int getFontSize() {

		return fontSize;
	} // end of getFontSize method

	/**
	 * Set the font point size (issue #77: applied by the GUI-side dialog).
	 *
	 * @param fontSize The new font point size.
	 */
	public void setFontSize(int fontSize) {

		this.fontSize = fontSize;
	} // end of setFontSize method

	/**
	 * Whether the text is drawn in bold (issue #77: read by the GUI-side
	 * renderer and dialog).
	 *
	 * @return true if bold.
	 */
	public boolean isBold() {

		return isBold;
	} // end of isBold method

	/**
	 * Set whether the text is drawn in bold (issue #77: applied by the
	 * GUI-side dialog).
	 *
	 * @param isBold true to draw in bold.
	 */
	public void setBold(boolean isBold) {

		this.isBold = isBold;
	} // end of setBold method

	/**
	 * Whether the text is drawn in italics (issue #77: read by the
	 * GUI-side renderer and dialog).
	 *
	 * @return true if italic.
	 */
	public boolean isItalic() {

		return isItalic;
	} // end of isItalic method

	/**
	 * Set whether the text is drawn in italics (issue #77: applied by the
	 * GUI-side dialog).
	 *
	 * @param isItalic true to draw in italics.
	 */
	public void setItalic(boolean isItalic) {

		this.isItalic = isItalic;
	} // end of setItalic method

	/**
	 * The color the text is drawn in, as a packed ARGB int (issue #77:
	 * read by the GUI-side renderer and dialog, which wrap it in a
	 * {@code Color}). Kept as an int so the model imports no AWT.
	 *
	 * @return the text color as a packed ARGB int.
	 */
	public int getColorRGB() {

		return colorRGB;
	} // end of getColorRGB method

	/**
	 * Set the color the text is drawn in, as a packed ARGB int (issue #77:
	 * applied by the GUI-side dialog from a {@code Color}).
	 *
	 * @param colorRGB The new text color as a packed ARGB int.
	 */
	public void setColorRGB(int colorRGB) {

		this.colorRGB = colorRGB;
	} // end of setColorRGB method

	/**
	 * The individual display lines (issue #77: iterated by the GUI-side
	 * renderer).
	 *
	 * @return the text split into display lines.
	 */
	public java.util.List<String> getLines() {

		return lines;
	} // end of getLines method

	/**
	 * Set this element's width (issue #77: the GUI-side renderer recomputes
	 * the bounding box from live font metrics at draw time).
	 *
	 * @param width The new width.
	 */
	public void setWidth(int width) {

		this.width = width;
	} // end of setWidth method

	/**
	 * Set this element's height (issue #77: the GUI-side renderer recomputes
	 * the bounding box from live font metrics at draw time).
	 *
	 * @param height The new height.
	 */
	public void setHeight(int height) {

		this.height = height;
	} // end of setHeight method

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	/** This element's own saved attributes, in save order (#23). */
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
			protected int get(Element el) { return ((Text)el).colorRGB; } // packed ARGB (#77)
			/**
			 * Set the color on the given element from a packed RGB int.
			 *
			 * @param el The element to write to.
			 * @param v The new color as an RGB integer.
			 */
			@Override
			protected void set(Element el, int v) { ((Text)el).colorRGB = 0xFF000000 | (v & 0xFFFFFF); } // force opaque, as new Color(v) did (#77)
		}
	);

	/** Base attributes followed by this element's own, in save order (#23). */
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

		Text it = new Text(getCircuit());
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
	 * Text areas can be changed.
	 * 
	 * @return true.
	 */
	@Override
	public boolean canChange() {

		return true;
	} // end of canChange method

} // end of Text method
