package jls.elem;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * A truth-table signal-name header cell (#27 S4): draws the signal name,
 * shows the delete/rename/move menu when clicked, and dispatches the
 * chosen action. InputSig and OutputSig were byte-identical except for
 * which TruthTable methods the menu actions call; those four calls are
 * the template hooks.
 *
 * @author David A. Poplawski
 */
public abstract class SigEntry extends Entry implements ActionListener {

	// properties
	/** The name of this signal. */
	protected String signal;

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
	public SigEntry(TruthTable ttelem, String signal, Graphics g) {

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
	@Override
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
	@Override
	public void selected(int row, int col) {

		JPopupMenu menu = new JPopupMenu();
		menu.add(remove);
		menu.add(rename);
		menu.add(moveLeft);
		menu.add(moveRight);
		// re-adding listeners on every popup made each menu action fire
		// once per prior popup (issue #51)
		if (remove.getActionListeners().length == 0) {
			remove.addActionListener(this);
			rename.addActionListener(this);
			moveLeft.addActionListener(this);
			moveRight.addActionListener(this);
		}
		menu.show(ttelem.getDisplay(),x,y);
	} // end of selected method

	/**
	 * React to menu selections.
	 *
	 * @param event The event object.
	 */
	@Override
	public void actionPerformed(ActionEvent event) {

		if (event.getSource() == remove) {
			doRemove();
		}
		else if (event.getSource() == rename) {
			doRename();
		}
		else if (event.getSource() == moveLeft) {
			doMoveLeft();
		}
		else if (event.getSource() == moveRight) {
			doMoveRight();
		}
	} // end of actionPerformed method

	/** Delete this signal's column. */
	protected abstract void doRemove();

	/** Rename this signal. */
	protected abstract void doRename();

	/** Move this signal's column left. */
	protected abstract void doMoveLeft();

	/** Move this signal's column right. */
	protected abstract void doMoveRight();

} // end of SigEntry class
