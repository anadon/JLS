package jls.sim;

import jls.*;
import jls.elem.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/**
 * Draw the trace of a signal over time.
 * 
 * @author David A. Poplawski
 */
public class Trace extends JPanel implements MouseListener, MouseMotionListener {
	
	// named constants
	protected final int HEIGHT = 40;
	
	// structure to contain a change
	private class Change {
		public BitSet value;
		public long when;
	};
	
	// properties
	private String name;
	private Element element;
	protected InterractiveSimulator.Traces parent;
	private LinkedList<Change> pendingChanges = new LinkedList<Change>();
	private LinkedList<Change> changes = new LinkedList<Change>();
	protected long now = 0;
	private int scaleFactor = 1;
	protected int width;
	private int bits;
	private BitSet previousValue = null;
	private Trace me;
	protected int sliderPos = -1;
	private int base = 10;
	private BitSet off;
	private BitSet begin;
	
	/**
	 * Construct a new Trace object.
	 *
	 * @param name The name of the element being traced (watched).
	 * @param el The element this trace is of.
	 * @param bits The number of bits in the element being traced.
	 * @param width The current width of the trace area.
	 * @param parent The parent object of this one.
	 */
	public Trace(String name, Element el, int bits, int width,
			InterractiveSimulator.Traces parent) {
		
		this.name = name;
		element = el;
		this.bits = bits;
		this.parent = parent;
		setMaximumSize(new Dimension(width,HEIGHT));
		setPreferredSize(new Dimension(width,HEIGHT));
		setMinimumSize(new Dimension(width,HEIGHT));
		setBackground(Color.white);
		addMouseListener(this);
		addMouseMotionListener(this);
		me = this;
		off = new BitSet(bits+1);
		off.set(bits);
		begin = new BitSet(bits+1);
		begin.set(0,bits+1);
	} // end of constructor
	
	/**
	 * Get the name of the element traced.
	 *
	 * @return The name.
	 */
	public String getName() {
		
		return name;
	} // end of getName method
	
	/**
	 * Get the  element traced.
	 *
	 * @return The name.
	 */
	public Element getElement() {
		
		return element;
	} // end of getName method
	
	/**
	 * Set the time scale factor.  The trace is compressed by this amount.
	 *
	 * @param factor The scale factor.
	 */
	public void setScaleFactor(int factor) {
		
		scaleFactor = factor;
	} // end of setScaleFactor method
	
	/**
	 * Add a value/time to the front of the pending changes list.
	 *
	 * @param value The value to add to the list.
	 * @param when The time at which the value occurred.
	 */
	public void addValue(BitSet value, long when) {
		
		// don't add if no change
		if (value == null) {
			value = off;
		}
		if (value.equals(previousValue))
			return;
			
		Change ch = new Change();
		if (value == null) {
			ch.value = off;
			previousValue = off;
		}
		else {
			ch.value = (BitSet)value.clone();
			previousValue = (BitSet)value.clone();
		}
		ch.when = when;
		pendingChanges.add(0,ch);
		
		// delete undisplayable values
		// (test for when == 0 due to no width yet)
		if (when != 0 && pendingChanges.size() > getWidth())
			pendingChanges.removeLast();
	} // end of addValue method
	
	/**
	 * Commit values to be displayed and set the current simulation time.
	 *
	 * @param time The current time.
	 */
	public void commit(long time) {
		
		now = time;
		changes = new LinkedList<Change>(pendingChanges);
	} // end of commit method
	
	/**
	 * Draw the trace.
	 * 
	 * @param g The Graphics object to draw with.
	 */
	public void paintComponent(Graphics g) {
		
		super.paintComponent(g);
		
		// get window size
		width = getWidth()-parent.getNameSpace()-10;
		
		// draw element name
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int descent = fm.getDescent();
		int height = ascent + descent;
		int baseline = HEIGHT/2 - height/2 + ascent;
		g.setColor(Color.black);
		g.drawString(name,width+5,baseline);
		
		// set up for loop
		int top = HEIGHT/2-10;
		int bottom = HEIGHT/2+10;
		int middle = (top+bottom)/2;
		double pos = width;
		long when = now;
		int ch = 0;
		
		// draw a terminator line
		g.setColor(Color.pink);
		g.drawLine(width,0,width,HEIGHT);
		
		// draw tic marks,
		// first compute tic increment
		int t = 0;
		int m = 1;
		int inc = 0;
		while (t < 50) {
			inc = m*50;
			t = inc/scaleFactor;
			if (t < 50) {
				inc = m*100;
				t = inc/scaleFactor;
			}
			if (t < 50) {
				inc = m*200;
				t = inc/scaleFactor;
			}
			if (t < 50) {
				inc = m*250;
				t = inc/scaleFactor;
			}
			m *= 10;
		}
		
		// then draw tics
		double tpos = width-now/scaleFactor;
		int value = 0;
		while (tpos < width) {
			g.setColor(Color.lightGray);
			g.drawLine((int)Math.rint(tpos), 0, (int)Math.rint(tpos), HEIGHT);
			
			// draw value if no changes (i.e., the header)
			if (changes.isEmpty()) {
				g.drawString(value+"", (int)Math.rint(tpos)+2, height);
			}
			tpos += 1.0*inc/scaleFactor;
			value += inc;
		}
		
		// draw until no longer visible
		g.setColor(Color.black);
		BitSet previousVal = null;
		while (pos > 0) {
			
			// get the next change
			if (ch >= changes.size())
				break;
			Change change = changes.get(ch);
			double len = ((double)(when-change.when)/scaleFactor);
			int rpos = (int)Math.round(pos);
			int rlen = (int)Math.round(len);
			
			// draw line(s) from the last point to this change
			if (bits > 1) {
				
				// handle multi-bit value
				if (change.value.equals(off)) {
					g.drawLine(rpos,middle,rpos-rlen,middle);
				}
				else {
					g.drawLine(rpos,top,rpos-rlen,top);
					g.drawLine(rpos,bottom,rpos-rlen,bottom);
				}
				if (!change.value.equals(previousVal)) {
					String val = "";
					if (!change.value.equals(off))
							val = BitSetUtils.ToString(change.value,base);
					int strlen = fm.stringWidth(val) + 2;
					if (strlen <= rlen) {
						g.drawString(val,rpos-rlen+(rlen-strlen)/2+1,baseline);
					}
				}
			}
			
			else {
				
				// handle single-bit value
				if (change.value.equals(off)) {
					g.drawLine(rpos,middle,rpos-rlen,middle);
				}
				else if (change.value.get(0)) {
					g.drawLine(rpos,top,rpos-rlen,top);
				}
				else {
					g.drawLine(rpos,bottom,rpos-rlen,bottom);
				}
			}
			
			// if value changed, draw vertical line
			if (!change.value.equals(previousVal) && previousVal != null) {
				if (bits > 1) {
					g.drawLine(rpos,top,rpos,bottom);
				}
				else if (previousVal.equals(off)) {
					if (change.value.length() == 0) {
						g.drawLine(rpos,middle,rpos,bottom);
					}
					else {
						g.drawLine(rpos,middle,rpos,top);
					}
				}
				else if (change.value.equals(off)) {
					if (previousVal.length() == 0) {
						g.drawLine(rpos,bottom,rpos,middle);
					}
					else {
						g.drawLine(rpos,top,rpos,middle);
					}
				}
				else {
					g.drawLine(rpos,top,rpos,bottom);
				}
			}
			previousVal = change.value;
			pos = pos-len;
			when = change.when;
			ch += 1;
		}
		
		// draw slider
		g.setColor(Color.gray);
		g.drawLine(sliderPos,0,sliderPos,HEIGHT);
		
		// draw value at slider position
		long stime = now-(width-sliderPos)*scaleFactor;
		if (stime >= 0) {
			Change lastChange = null;
			for (Change chg : changes) {
				lastChange = chg;
				if (stime >= chg.when)
					break;
			}
			
			// don't draw value if no changes (e.g., the header)
			if (lastChange != null) {
				String val = "HiZ";
				if (!lastChange.value.equals(off)) {
					val = BitSetUtils.ToString(lastChange.value,base);
				}
				int w = fm.stringWidth(val);
				g.setColor(Color.white);
				g.fillRect(sliderPos-w-4,baseline-ascent,w+3,height);
				g.setColor(Color.magenta);
				g.drawString(val,sliderPos-w-1,baseline);
			}
		}
		
	} // end of paintComponent method
	
	/**
	 * Called to tell this object that the trace window has been resized.
	 *
	 * @param width The new width of the trace window.
	 */
	public void resize(int width) {
		
		setMaximumSize(new Dimension(width,HEIGHT));
		setPreferredSize(new Dimension(width,HEIGHT));
		setMinimumSize(new Dimension(width,HEIGHT));
		repaint();
	} // end of resize method
	
	/**
	 * Draw the slider.
	 *
	 * @param x The x-coordinate of the slider.
	 */
	public void mouseMoved(int x) {
		
		long time = now-(width-x)*scaleFactor;
		
		// set slider pos
		if (time > now)
			sliderPos = width;
		else if (time < 0)
			sliderPos = width - (int)((now)/scaleFactor);
		else
			sliderPos = x;
		repaint();
	} // endof mouseMoved method
	
	/**
	 * If press in the name area, display popup memu to reposition trace.
	 *
	 * @param event The event object containing the position of the mouse.
	 */
	public void mousePressed(MouseEvent event) {
		
		// get the coordinates of the mouse
		int x = event.getX();
		int y = event.getY();
		
		// ignore if not in name area
		if (x <= width)
			return;
		
		// create popup menu
		JPopupMenu ask = new JPopupMenu();
		final JMenuItem top = new JMenuItem("Move Trace To Top");
		ask.add(top);
		final JMenuItem up = new JMenuItem("Move Trace Up");
		ask.add(up);
		final JMenuItem down = new JMenuItem("Move Trace Down");
		ask.add(down);
		final JMenuItem bottom = new JMenuItem("Move Trace To Bottom");
		ask.add(bottom);
		ask.pack();
		ask.show(this,x,y);
		
		// add listeners
		ActionListener list = new ActionListener() {
			
			public void actionPerformed(ActionEvent event) {
				
				// tell parent object to move this JPanel
				if (event.getSource() == top)
					parent.move(me,-2);
				else if (event.getSource() == up)
					parent.move(me,-1);
				else if (event.getSource() == down)
					parent.move(me,1);
				else if (event.getSource() == bottom)
					parent.move(me,2);
			} // end of actionPerformed method
			
		}; // end of ActionListener class
		
		top.addActionListener(list);
		up.addActionListener(list);
		down.addActionListener(list);
		bottom.addActionListener(list);
	} // end of mousePressed method
	
	/**
	 * Tell parent that the mouse moved within this component.
	 *
	 * @param event The MouseEvent for the movement.
	 */
	public void mouseMoved(MouseEvent event) {
		
		parent.mouseMoved(event);
	} // end of mouseMoved method
	
	// unused
	public void mouseEntered(MouseEvent event) {}
	public void mouseExited(MouseEvent event) {}
	public void mouseReleased(MouseEvent event) {}
	public void mouseClicked(MouseEvent event) {}
	public void mouseDragged(MouseEvent event) {}
	
	/**
	 * Set the base to display numbers with.
	 * 
	 * @param base The new base.
	 */
	public void setBase(int base) {
		
		this.base = base;
		repaint();
	} // end of setBase method
	
} // end of Trace class
