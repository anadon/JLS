package jls.elem;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
/**
 * Draw an output signal name, display menu when one is clicked on, and react
 * to menu choices.
 * 
 * @author David A. Poplawski
 */
public final class OutputSig extends Entry implements ActionListener {

	// properties
	private String signal;
	
	// menu items
	private JMenuItem remove = new JMenuItem("delete");
	private JMenuItem rename = new JMenuItem("rename");
	private JMenuItem moveLeft = new JMenuItem("move left");
	private JMenuItem moveRight = new JMenuItem("move right");
	
	/**
	 * Create a new entry.
	 * 
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 * @param signal The name of this signal.
	 * @param g A Graphics object to size the name with.
	 */
	public OutputSig(TruthTable ttelem, String signal, Graphics g) {
		
		super(ttelem);
		this.signal = signal;
		FontMetrics fm = g.getFontMetrics();
		minHeight = fm.getAscent()+fm.getDescent();
		minWidth = fm.stringWidth(" " + signal + " ");
	} // end of constructor

	/**
	 * Draw this entry.
	 * 
	 * @param g The Graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int height = ascent + fm.getDescent();
		int width = fm.stringWidth(signal);
		g.setColor(Color.BLACK);
		g.drawString(signal,x+(this.width-width)/2,y+(this.height-height)/2+ascent);
	} // end of draw method

	/**
	 * Display menu when selected.
	 * 
	 * @param row Unused.
	 * @param col Unused.
	 */
	public void selected(int row, int col) {
		
		JPopupMenu menu = new JPopupMenu();
		menu.add(remove);
		menu.add(rename);
		menu.add(moveLeft);
		menu.add(moveRight);
		remove.addActionListener(this);
		rename.addActionListener(this);
		moveLeft.addActionListener(this);
		moveRight.addActionListener(this);
		menu.show(ttelem.getDisplay(),x,y);
	} // end of selected method
	
	/**
	 * React to menu selections.
	 * 
	 * @param event The event object.
	 */
	public void actionPerformed(ActionEvent event) {
		
		if (event.getSource() == remove) {
			ttelem.removeOutput(signal);
		}
		else if (event.getSource() == rename) {
			ttelem.renameOutput(signal);
		}
		else if (event.getSource() == moveLeft) {
			ttelem.moveOutputLeft(signal);
		}
		else if (event.getSource() == moveRight) {
			ttelem.moveOutputRight(signal);
		}
	} // end of actionPerformed method

} // end of OutputSig method
