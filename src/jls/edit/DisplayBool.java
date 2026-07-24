package jls.edit;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import jls.elem.Cross;
import jls.elem.Entry;
import jls.elem.HLine;
import jls.elem.InputSig;
import jls.elem.InputVal;
import jls.elem.OutputSig;
import jls.elem.OutputVal;
import jls.elem.SigEntry;
import jls.elem.TruthTable;
import jls.elem.VLine;
import jls.elem.ValEntry;

/**
 * Displays the truth table, GUI-side (issue #77). Moved from the former
 * {@code jls.elem.DisplayBool}: it lays out an array of {@link Entry}
 * cells (now pure model data) and draws each one through a type switch,
 * absorbing the drawing that used to live on the cell classes. Signal
 * header cells show the delete/rename/move popup menu on click.
 *
 * @author David A. Poplawski
 */
@SuppressWarnings("serial")
public final class DisplayBool extends JPanel implements MouseListener {

	// properties
	/** The truth table element this display is a part of. */
	private TruthTable ttelem;
	/** The displayed entries, indexed by row and column. */
	private Entry [][] dtable;
	/** Number of rows in the displayed table. */
	private int rows;
	/** Number of columns in the displayed table. */
	private int cols;

	/**
	 * Create display area.
	 *
	 * @param ttelem The truth table element this is a part of.
	 */
	public DisplayBool(TruthTable ttelem) {

		this.ttelem = ttelem;
		addMouseListener(this);
	} // end of constructor

	/**
	 * Re-lay out and repaint the table from the element's current model
	 * state; wired as the element's display refresher after a model change.
	 */
	public void refresh() {

		doLayout(ttelem.getInputNames(),ttelem.getOutputNames(),
				ttelem.getTable(),null);
		repaint();
	} // end of refresh method

	/**
	 * Draw the truth table
	 *
	 * @param g The Graphics object to draw with.
	 */
	@Override
	public void paintComponent(Graphics g) {

		super.paintComponent(g);

		// don't draw anything until there is something to draw
		if (dtable == null)
			return;

		// draw the truth table
		for (int r = 0; r<rows; r+=1) {
			for (int c = 0; c<cols; c+=1) {
					drawEntry(g,dtable[r][c]);
			}
		}
	} // end of paintComponent method

	/**
	 * Print the truth table
	 *
	 * @param g The Graphics object to print with.
	 */
	@Override
	public void print(Graphics g) {

		// don't draw anything until there is something to draw
		if (dtable == null)
			return;

		// draw the truth table
		for (int r = 0; r<rows; r+=1) {
			for (int c = 0; c<cols; c+=1) {
					drawEntry(g,dtable[r][c]);
			}
		}
	} // end of print method

	/**
	 * Draw a single entry, dispatching on its type (issue #77). The cell
	 * classes are pure data; each draw body moved here verbatim from the
	 * former {@code Entry} subclass draw methods.
	 *
	 * @param g The Graphics object to draw with.
	 * @param e The entry to draw.
	 */
	private void drawEntry(Graphics g, Entry e) {

		int x = e.getX();
		int y = e.getY();
		int width = e.getWidth();
		int height = e.getHeight();

		if (e instanceof HLine) {
			g.setColor(Color.black);
			g.drawLine(x,y+height/2,x+width,y+height/2);
		}
		else if (e instanceof Cross) {
			g.setColor(Color.black);
			g.drawLine(x,y+height/2,x+width,y+height/2);
			g.drawLine(x+width/2,y,x+width/2,y+height);
		}
		else if (e instanceof VLine) {
			g.setColor(Color.black);
			g.drawLine(x+width/2,y,x+width/2,y+height);
		}
		else if (e instanceof SigEntry) {
			SigEntry se = (SigEntry) e;
			FontMetrics fm = g.getFontMetrics();
			int ascent = fm.getAscent();
			int h = ascent + fm.getDescent();
			int w = fm.stringWidth(se.getSignal());
			g.setColor(Color.BLACK);
			g.drawString(se.getSignal(),x+(width-w)/2,y+(height-h)/2+ascent);
		}
		else if (e instanceof ValEntry) {
			ValEntry ve = (ValEntry) e;
			FontMetrics fm = g.getFontMetrics();
			int ascent = fm.getAscent();
			int h = ascent + fm.getDescent();
			String str = "" + ve.getValue();
			if (ve.getValue() == 2)
				str = "x";
			int w = fm.stringWidth(str);
			g.setColor(Color.BLACK);
			g.drawString(str,x+(width-w)/2,y+(height-h)/2+ascent);
		}
	} // end of drawEntry method

	/**
	 * Set the positions of all entries.
	 * Uses minimum sizes of all entries to make all rows and columns
	 * line up.
	 *
	 * @param ins The input signal names.
	 * @param outs The output signal names.
	 * @param table The input and output values.
	 * @param g A Graphics object to use.  If null, get this object's Graphics object.
	 */
	public void doLayout(Vector<String>ins, Vector<String>outs, int[][] table, Graphics g) {

		// set up to create displayable table
		int in = ins.size();
		int out = outs.size();
		cols = in+out+1;
		rows = table.length+2;
		dtable = new Entry[rows][cols];
		if (g == null) {
			g = getGraphics();
		}

		// set up inputs
		for (int c=0; c<in; c+=1) {
			InputSig sig = new InputSig(ttelem,ins.get(c));
			sizeSignal(sig,ins.get(c),g);
			dtable[0][c] = sig;
			dtable[1][c] = new HLine(ttelem);
			for (int r=2; r<rows; r+=1) {
				InputVal val = new InputVal(ttelem,table[r-2][c]);
				sizeValue(val,table[r-2][c],g);
				dtable[r][c] = val;
			}
		}

		// set up vertical separator
		dtable[0][in] = new VLine(ttelem);
		dtable[1][in] = new Cross(ttelem);
		for (int r=2; r<rows; r+=1)
			dtable[r][in] = new VLine(ttelem);

		// set up outputs
		for (int c=in+1; c<cols; c+=1) {
			OutputSig sig = new OutputSig(ttelem,outs.get(c-in-1));
			sizeSignal(sig,outs.get(c-in-1),g);
			dtable[0][c] = sig;
			dtable[1][c] = new HLine(ttelem);
			for (int r=2; r<rows; r+=1) {
				OutputVal val = new OutputVal(ttelem,table[r-2][c-1]);
				sizeValue(val,table[r-2][c-1],g);
				dtable[r][c] = val;
			}
		}

		// get row heights
		int [] heights = new int[rows];
		int totalHeight = 0;
		for (int r = 0; r<rows; r+=1) {
			int maxHeight = 0;
			for (int c = 0; c<cols; c+=1) {
				maxHeight = Math.max(maxHeight,dtable[r][c].getMinHeight());
			}
			heights[r] = maxHeight;
			totalHeight += maxHeight;
		}

		// get column widths
		int [] widths = new int[cols];
		int totalWidth = 0;
		for (int c = 0; c<cols; c+=1) {
			int maxWidth = 0;
			for (int r = 0; r<rows; r+=1) {
				maxWidth = Math.max(maxWidth,dtable[r][c].getMinWidth());
			}
			widths[c] = maxWidth;
			totalWidth += maxWidth;
		}

		// position entries
		int y = 0;
		for (int r = 0; r<rows; r+=1) {
			int x = 0;
			for (int c = 0; c<cols; c+=1) {
				dtable[r][c].setPosition(x,y);
				dtable[r][c].setSize(widths[c],heights[r]);
				x += widths[c];
			}
			y += heights[r];
		}

		// set preferred size of this component
		setPreferredSize(new Dimension(totalWidth,totalHeight));
		revalidate();

	} // end of doLayout method

	/**
	 * Size a signal header cell from the font metrics, exactly as the
	 * former SigEntry constructor did.
	 *
	 * @param sig The signal cell to size.
	 * @param signal The signal name.
	 * @param g The Graphics object to measure with.
	 */
	private void sizeSignal(SigEntry sig, String signal, Graphics g) {

		FontMetrics fm = g.getFontMetrics();
		sig.setMinSize(fm.stringWidth(" " + signal + " "),
				fm.getAscent()+fm.getDescent());
	} // end of sizeSignal method

	/**
	 * Size a value cell from the font metrics, exactly as the former
	 * ValEntry constructor did.
	 *
	 * @param val The value cell to size.
	 * @param value The cell value.
	 * @param g The Graphics object to measure with.
	 */
	private void sizeValue(ValEntry val, int value, Graphics g) {

		FontMetrics fm = g.getFontMetrics();
		val.setMinSize(fm.stringWidth(" " + value + " "),
				fm.getAscent()+fm.getDescent());
	} // end of sizeValue method

	/**
	 * React to mouse pressed events.
	 * Find entry under mouse and do the appropriate action depending
	 * on the type of the entry.
	 *
	 * @param event The event object.
	 */
	@Override
	public void mousePressed(MouseEvent event) {

		int x = event.getX();
		int y = event.getY();
		for (int r=0; r<rows; r+=1) {
			for (int c=0; c<cols; c+=1) {
				if (dtable[r][c].contains(x,y)) {
					Entry e = dtable[r][c];
					if (e instanceof SigEntry) {
						showSignalMenu((SigEntry) e);
					}
					else {
						e.selected(r,c);
					}
					return;
				}
			}
		}
	} // end of mousePressed method

	/**
	 * Show the delete/rename/move menu for a signal header cell (issue
	 * #77: the popup that used to live on SigEntry). Fresh menu items each
	 * time, so a listener can never fire more than once (the former #51
	 * double-fire guard is now structural).
	 *
	 * @param se The signal header cell clicked on.
	 */
	private void showSignalMenu(SigEntry se) {

		JPopupMenu menu = new JPopupMenu();
		JMenuItem remove = new JMenuItem("delete");
		JMenuItem rename = new JMenuItem("rename");
		JMenuItem moveLeft = new JMenuItem("move left");
		JMenuItem moveRight = new JMenuItem("move right");
		menu.add(remove);
		menu.add(rename);
		menu.add(moveLeft);
		menu.add(moveRight);
		remove.addActionListener(event -> se.doRemove());
		rename.addActionListener(event -> se.doRename());
		moveLeft.addActionListener(event -> se.doMoveLeft());
		moveRight.addActionListener(event -> se.doMoveRight());
		menu.show(this,se.getX(),se.getY());
	} // end of showSignalMenu method

	// remaining methods not used
	/**
	 * Unused MouseListener callback; no action on mouse released.
	 *
	 * @param event The event object.
	 */
	@Override
	public void mouseReleased(MouseEvent event) {}
	/**
	 * Unused MouseListener callback; no action on mouse clicked.
	 *
	 * @param event The event object.
	 */
	@Override
	public void mouseClicked(MouseEvent event) {}
	/**
	 * Unused MouseListener callback; no action on mouse entered.
	 *
	 * @param event The event object.
	 */
	@Override
	public void mouseEntered(MouseEvent event) {}
	/**
	 * Unused MouseListener callback; no action on mouse exited.
	 *
	 * @param event The event object.
	 */
	@Override
	public void mouseExited(MouseEvent event) {}

} // end of DisplayBool class
