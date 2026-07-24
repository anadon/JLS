package jls.elem;

/**
 * Superclass of all truth table display entries. Pure data (issue #77):
 * an entry holds its position, size and minimum size; the GUI draws it
 * through the type switch in {@code jls.edit.DisplayBool} rather than the
 * entry drawing itself, so this model half carries no AWT.
 *
 * @author David A. Poplawski
 */
public abstract class Entry {

	// named constants
	/** The default (and minimum) width of an entry. */
	private final int defaultWidth = 10;
	/** The default (and minimum) height of an entry. */
	private final int defaultHeight = 10;

	// properties
	/** The TruthTable object this entry is a part of. */
	protected TruthTable ttelem;
	/** The x-coordinate of this entry. */
	protected int x;
	/** The y-coordinate of this entry. */
	protected int y;
	/** The minimum width of this entry. */
	protected int minWidth = defaultWidth;
	/** The minimum height of this entry. */
	protected int minHeight = defaultHeight;
	/** The width of this entry. */
	protected int width = defaultWidth;
	/** The height of this entry. */
	protected int height = defaultHeight;

	/**
	 * Construct a new Entry.
	 *
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 */
	public Entry(TruthTable ttelem) {

		this.ttelem = ttelem;
	} // end of constructor

	/**
	 * Get the truth table this entry belongs to.
	 *
	 * @return the truth table element.
	 */
	public TruthTable getTruthTable() {

		return ttelem;
	} // end of getTruthTable method

	/**
	 * Set the x,y position of this entry.
	 *
	 * @param x The x-coordinate.
	 * @param y The y-coordinate.
	 */
	public void setPosition(int x, int y) {

		this.x = x;
		this.y = y;
	} // end of setPosition method

	/**
	 * Set the size of this entry.
	 *
	 * @param width The new width.
	 * @param height The new height.
	 */
	public void setSize(int width, int height) {

		this.width = width;
		this.height = height;
	} // end of setSize method

	/**
	 * Set the minimum size of this entry (computed GUI-side from the font
	 * metrics for sized cells; unsized cells keep the default).
	 *
	 * @param minWidth The new minimum width.
	 * @param minHeight The new minimum height.
	 */
	public void setMinSize(int minWidth, int minHeight) {

		this.minWidth = minWidth;
		this.minHeight = minHeight;
	} // end of setMinSize method

	/**
	 * Get the minimum width of this element.
	 *
	 * @return the minimum width.
	 */
	public int getMinWidth() {

		return minWidth;
	} // end of getMinWidth method

	/**
	 * Get the minimum height of this element.
	 *
	 * @return the minmum height.
	 */
	public int getMinHeight() {

		return minHeight;
	} // end of getMinHeight method

	/**
	 * Get the x-coordinate of this entry.
	 *
	 * @return the x-coordinate.
	 */
	public int getX() {

		return x;
	} // end of getX method

	/**
	 * Get the y-coordinate of this entry.
	 *
	 * @return the y-coordinate.
	 */
	public int getY() {

		return y;
	} // end of getY method

	/**
	 * Get the width of this entry.
	 *
	 * @return the width.
	 */
	public int getWidth() {

		return width;
	} // end of getWidth method

	/**
	 * Get the height of this entry.
	 *
	 * @return the height.
	 */
	public int getHeight() {

		return height;
	} // end of getHeight method

	/**
	 * See if a given point is inside this entry.
	 *
	 * @param x The x-coordinate of the point.
	 * @param y The y-coordinate of the point.
	 *
	 * @return true if the point is inside the entry, false if it is not.
	 */
	public boolean contains(int x, int y) {

		if (x <= this.x || y <= this.y || x >= this.x+width || y >= this.y+height)
			return false;
		return true;
	} // end of contains method

	/**
	 * Do something when the mouse is clicked on this entry.
	 *
	 * @param row The row this entry is in.
	 * @param col The column this entry is in.
	 */
	public void selected(int row, int col) {

		// the default is to do nothing
	} // end of selected method

} // end of Entry class
