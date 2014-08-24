package jls.elem;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/**
 * Display the truth table.
 * Uses an array of Entry sub-objects.
 * 
 * @author David A. Poplawski
 */
@SuppressWarnings("serial")
public final class DisplayBool extends JPanel implements MouseListener {

	// properties
	private TruthTable ttelem;
	private Entry [][] dtable;
	private int rows;
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
	 * Draw the truth table
	 * 
	 * @param g The Graphics object to draw with.
	 */
	public void paintComponent(Graphics g) {
		
		super.paintComponent(g);
		
		// don't draw anything until there is something to draw
		if (dtable == null)
			return;
		
		// draw the truth table
		for (int r = 0; r<rows; r+=1) {
			for (int c = 0; c<cols; c+=1) {
					dtable[r][c].draw(g);
			}
		}
	} // end of paintComponent method

	/**
	 * Print the truth table
	 * 
	 * @param g The Graphics object to print with.
	 */
	public void print(Graphics g) {
		
		// don't draw anything until there is something to draw
		if (dtable == null)
			return;
		
		// draw the truth table
		for (int r = 0; r<rows; r+=1) {
			for (int c = 0; c<cols; c+=1) {
					dtable[r][c].draw(g);
			}
		}
	} // end of print method
	
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
			dtable[0][c] = new InputSig(ttelem,ins.get(c),g);
			dtable[1][c] = new HLine(ttelem);
			for (int r=2; r<rows; r+=1) {
				dtable[r][c] = new InputVal(ttelem,table[r-2][c],g);
			}
		}
		
		// set up vertical separator
		dtable[0][in] = new VLine(ttelem);
		dtable[1][in] = new Cross(ttelem);
		for (int r=2; r<rows; r+=1)
			dtable[r][in] = new VLine(ttelem);
		
		// set up outputs
		for (int c=in+1; c<cols; c+=1) {
			dtable[0][c] = new OutputSig(ttelem,outs.get(c-in-1),g);
			dtable[1][c] = new HLine(ttelem);
			for (int r=2; r<rows; r+=1) {
				dtable[r][c] = new OutputVal(ttelem,table[r-2][c-1],g);
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
	 * React to mouse pressed events.
	 * Find entry under mouse and do the appropriate action depending
	 * on the type of the entry.
	 * 
	 * @param event The event object.
	 */
	public void mousePressed(MouseEvent event) {
		
		int x = event.getX();
		int y = event.getY();
		for (int r=0; r<rows; r+=1) {
			for (int c=0; c<cols; c+=1) {
				if (dtable[r][c].contains(x,y)) {
					dtable[r][c].selected(r,c);
					return;
				}
			}
		}
	} // end of mousePressed method

	// remaining methods not used
	public void mouseReleased(MouseEvent event) {}
	public void mouseClicked(MouseEvent event) {}
	public void mouseEntered(MouseEvent event) {}
	public void mouseExited(MouseEvent event) {}

} // end of Display class
