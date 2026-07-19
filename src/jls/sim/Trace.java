package jls.sim;

import jls.*;
import jls.elem.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import org.jspecify.annotations.Nullable;

/**
 * Draw the trace of a signal over time.
 * 
 * @author David A. Poplawski
 */
public class Trace extends JPanel implements MouseListener, MouseMotionListener {
	
	// named constants
	/** The pixel height of one trace row. */
	protected final int HEIGHT = 40;

	/**
	 * How many changes each trace retains for scrollback (issue #121).
	 * This is the display-side bound (distinct from #20's simulation
	 * state bounds): at ~136 bytes per change (Change object + cloned
	 * BitSet + linked-list node) it caps a trace at roughly 14 MB.
	 */
	static final int MAX_RETAINED_CHANGES = 100_000;

	// structure to contain a change
	/**
	 * A single recorded signal value together with the simulation time at
	 * which it took effect.  Traces keep these in a list ordered newest
	 * first to draw and to look up the value in effect at any time.
	 *
	 * @param value The recorded value (a private snapshot, cloned at
	 *              creation so later signal changes cannot alias into it).
	 * @param when The simulation time at which the value took effect.
	 */
	/**
	 * A single recorded signal value with the simulation time it took
	 * effect (issue #94: a record - Traces keep these newest-first).
	 *
	 * @param value The value taking effect (HiZ as the off marker).
	 * @param when The simulation time it took effect.
	 */
	private record Change(BitSet value, long when) {}

	// properties
	/** The traced signal's display name. */
	private String name;
	/** The element whose output this trace records. */
	private Element element;
	/** The trace-window container this trace row belongs to. */
	protected InteractiveSimulator.Traces parent;
	/**
	 * Changes recorded but not yet committed to the display.  Written
	 * by the sim thread and copied by the EDT at commit; both methods
	 * are synchronized and the committed list is replaced, never
	 * mutated, so drawing iterates a stable snapshot (issue #49,
	 * finding M9).
	 */
	private LinkedList<Change> pendingChanges = new LinkedList<Change>();
	/**
	 * The committed changes drawing reads, newest first.  ArrayList:
	 * paintComponent indexes into this list per change, which was
	 * O(n^2) on a LinkedList (issue #43).
	 */
	private volatile java.util.ArrayList<Change> changes =
			new java.util.ArrayList<Change>();
	/** The latest committed simulation time. */
	protected long now = 0;
	/** Horizontal scale: simulation time units per pixel. */
	private int scaleFactor = 1;
	/** The drawable trace width in pixels. */
	protected int width;
	/** The traced signal's bit width. */
	private int bits;
	/** The last committed value; null until the first arrives (#93). */
	private @Nullable BitSet previousValue = null;
	/** This trace, for inner-class callbacks. */
	private Trace me;
	/** The time-cursor slider position in pixels, or -1 if none. */
	protected int sliderPos = -1;
	/** The numeric base values are labeled in (2, 10, or 16). */
	private int base = 10;
	/** The HiZ sentinel: only the extra top bit (index bits) set. */
	private BitSet off;
	/** An all-ones sentinel; initialized but currently unread. */
	private BitSet begin;
	
	/**
	 * Construct a new Trace object.
	 *
	 * @param name The name of the element being traced (watched).
	 * @param el The element this trace is of.
	 * @param bits The number of bits in the element being traced.
	 * @param width The current width of the trace area.
	 * @param parent The parent object of this one.
	 *
	 * @jls.testedby jls.sim.TraceRetentionTest#historyBeyondThePanelWidthSurvivesForScrollback()
	 * @jls.testedby jls.sim.TraceRetentionTest#retentionIsBoundedAtTheDocumentedCap()
	 * @jls.testedby jls.sim.TraceRetentionTest#unchangedValuesAreStillDeduplicated()
	 * @jls.testedby jls.sim.TraceWindowingTest#firstChangeAtOrBeforeMatchesTheLinearScan()
	 * @jls.testedby jls.sim.TraceWindowingTest#windowedRepaintMatchesFullRepaintForAMultiBitTrace()
	 * @jls.testedby jls.sim.TraceWindowingTest#windowedRepaintMatchesFullRepaintForASingleBitTrace()
	 * @jls.testedby jls.sim.TraceWindowingTest#windowedRepaintMatchesFullRepaintForTheHeaderLabels()
	 */
	public Trace(String name, Element el, int bits, int width,
			InteractiveSimulator.Traces parent) {
		
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
	@Override
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
	 *
	 * @jls.testedby jls.sim.TraceWindowingTest#windowedRepaintMatchesFullRepaintForAMultiBitTrace()
	 * @jls.testedby jls.sim.TraceWindowingTest#windowedRepaintMatchesFullRepaintForTheHeaderLabels()
	 */
	public void setScaleFactor(int factor) {
		
		scaleFactor = factor;
	} // end of setScaleFactor method
	
	/**
	 * Add a value/time to the front of the pending changes list.
	 *
	 * @param value The value to add to the list, or null for HiZ.
	 * @param when The time at which the value occurred.
	 *
	 * @jls.testedby jls.sim.TraceRetentionTest#historyBeyondThePanelWidthSurvivesForScrollback()
	 * @jls.testedby jls.sim.TraceRetentionTest#retentionIsBoundedAtTheDocumentedCap()
	 * @jls.testedby jls.sim.TraceRetentionTest#unchangedValuesAreStillDeduplicated()
	 * @jls.testedby jls.sim.TraceWindowingTest#firstChangeAtOrBeforeMatchesTheLinearScan()
	 * @jls.testedby jls.sim.TraceWindowingTest#windowedRepaintMatchesFullRepaintForAMultiBitTrace()
	 * @jls.testedby jls.sim.TraceWindowingTest#windowedRepaintMatchesFullRepaintForASingleBitTrace()
	 */
	public synchronized void addValue(@Nullable BitSet value, long when) {

		// don't add if no change
		if (value == null) {
			value = off;
		}
		if (value.equals(previousValue))
			return;
			
		Change ch = new Change((BitSet)value.clone(),when);
		previousValue = (BitSet)value.clone();
		pendingChanges.add(0,ch);
		
		// retain a bounded scrollback history (issue #121): the cap
		// used to be the panel width, which discarded everything a
		// finished run could no longer display; the panel now grows
		// with the run, so retention is a documented count instead
		if (pendingChanges.size() > MAX_RETAINED_CHANGES)
			pendingChanges.removeLast();
	} // end of addValue method
	
	/**
	 * Commit values to be displayed and set the current simulation time.
	 *
	 * @param time The current time.
	 *
	 * @jls.testedby jls.sim.TraceRetentionTest#historyBeyondThePanelWidthSurvivesForScrollback()
	 * @jls.testedby jls.sim.TraceRetentionTest#retentionIsBoundedAtTheDocumentedCap()
	 * @jls.testedby jls.sim.TraceRetentionTest#unchangedValuesAreStillDeduplicated()
	 * @jls.testedby jls.sim.TraceWindowingTest#firstChangeAtOrBeforeMatchesTheLinearScan()
	 * @jls.testedby jls.sim.TraceWindowingTest#windowedRepaintMatchesFullRepaintForAMultiBitTrace()
	 * @jls.testedby jls.sim.TraceWindowingTest#windowedRepaintMatchesFullRepaintForASingleBitTrace()
	 * @jls.testedby jls.sim.TraceWindowingTest#windowedRepaintMatchesFullRepaintForTheHeaderLabels()
	 */
	public synchronized void commit(long time) {

		now = time;
		changes = new java.util.ArrayList<Change>(pendingChanges);
	} // end of commit method
	
	/**
	 * Draw the trace.
	 * 
	 * @param g The Graphics object to draw with.
	 *
	 * @jls.testedby jls.sim.TraceRetentionTest#paint()
	 * @jls.testedby jls.sim.TraceWindowingTest#paint()
	 */
	@Override
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
		
		// take one snapshot of the committed changes so every phase
		// below sees the same list even if a commit lands mid-paint
		java.util.ArrayList<Change> snapshot = changes;

		// set up for loop
		int top = HEIGHT/2-10;
		int bottom = HEIGHT/2+10;
		int middle = (top+bottom)/2;
		long when = now;
		int ch = 0;

		// draw a terminator line
		g.setColor(Color.pink);
		g.drawLine(width,0,width,HEIGHT);

		// draw tic marks,
		// first compute tic increment (extracted for unit testing,
		// issue #121)
		int inc = TraceGeometry.ticIncrement(scaleFactor);

		// the panel spans the whole retained run now (issue #121), so
		// drawing is windowed to the clip: sweeping every tic and every
		// change from time zero would be O(run length) per repaint
		Rectangle clip = g.getClipBounds();
		int clipLo = clip == null ? 0 : clip.x;
		int clipHi = clip == null ? width : clip.x+clip.width;

		// label only every stride-th tic so adjacent labels can never
		// overlap (issue #121): bound every label's width by the digit
		// count of the largest time value, measured in this font's
		// metrics (metrics, never hard-coded pixels - issue #111)
		double pitch = 1.0*inc/scaleFactor;
		double tzero = width-now/scaleFactor;
		long lastTic = Math.max(0,(long)Math.ceil((width-tzero)/pitch));
		int digitWidth = fm.charWidth(' ');
		for (char c = '0'; c <= '9'; c += 1)
			digitWidth = Math.max(digitWidth,fm.charWidth(c));
		int labelWidth = (String.valueOf(lastTic*inc).length()+1)*digitWidth;
		int stride = TraceGeometry.labelStride(labelWidth,pitch);

		// then draw tics, starting one label stride left of the clip
		// so a label that begins off-clip still paints its tail
		long firstTic = Math.max(0,
				(long)Math.floor((clipLo-tzero)/pitch)-stride);
		for (long tic = firstTic; ; tic += 1) {
			double tpos = tzero+tic*pitch;
			if (tpos >= width || tpos > clipHi)
				break;
			g.setColor(Color.lightGray);
			g.drawLine((int)Math.rint(tpos), 0, (int)Math.rint(tpos), HEIGHT);

			// draw value if no changes (i.e., the header)
			if (snapshot.isEmpty() && tic % stride == 0) {
				g.drawString((tic*inc)+"", (int)Math.rint(tpos)+2, height);
			}
		}

		// skip changes that lie entirely right of the clip: the first
		// change at or before the time just right of the clip edge
		// (2px slack for rounding) opens the visible window
		if (!snapshot.isEmpty() && clipHi < width) {
			long tClip = now-(long)(width-clipHi-2)*scaleFactor;
			ch = firstChangeAtOrBefore(snapshot,tClip);
			if (ch > 0)
				when = snapshot.get(ch-1).when();
		}
		double pos = width-(double)(now-when)/scaleFactor;
		BitSet previousVal = ch > 0 ? snapshot.get(ch-1).value() : null;
		double leftEdge = Math.max(0,clipLo-2);

		// draw until no longer visible
		g.setColor(Color.black);
		while (pos > leftEdge) {

			// get the next change
			if (ch >= snapshot.size())
				break;
			Change change = snapshot.get(ch);
			double len = ((double)(when-change.when())/scaleFactor);
			int rpos = (int)Math.round(pos);
			int rlen = (int)Math.round(len);
			
			// draw line(s) from the last point to this change
			if (bits > 1) {
				
				// handle multi-bit value
				if (change.value().equals(off)) {
					g.drawLine(rpos,middle,rpos-rlen,middle);
				}
				else {
					g.drawLine(rpos,top,rpos-rlen,top);
					g.drawLine(rpos,bottom,rpos-rlen,bottom);
				}
				if (!change.value().equals(previousVal)) {
					String val = "";
					if (!change.value().equals(off))
							val = BitSetUtils.ToString(change.value(),base);
					int strlen = fm.stringWidth(val) + 2;
					if (strlen <= rlen) {
						g.drawString(val,rpos-rlen+(rlen-strlen)/2+1,baseline);
					}
				}
			}
			
			else {
				
				// handle single-bit value
				if (change.value().equals(off)) {
					g.drawLine(rpos,middle,rpos-rlen,middle);
				}
				else if (change.value().get(0)) {
					g.drawLine(rpos,top,rpos-rlen,top);
				}
				else {
					g.drawLine(rpos,bottom,rpos-rlen,bottom);
				}
			}
			
			// if value changed, draw vertical line
			if (!change.value().equals(previousVal) && previousVal != null) {
				if (bits > 1) {
					g.drawLine(rpos,top,rpos,bottom);
				}
				else if (previousVal.equals(off)) {
					if (change.value().length() == 0) {
						g.drawLine(rpos,middle,rpos,bottom);
					}
					else {
						g.drawLine(rpos,middle,rpos,top);
					}
				}
				else if (change.value().equals(off)) {
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
			previousVal = change.value();
			// recompute the position from the change time rather than
			// accumulate, so a windowed repaint puts every segment
			// exactly where a full repaint would (issue #121)
			pos = width-(double)(now-change.when())/scaleFactor;
			when = change.when();
			ch += 1;
		}

		// draw slider
		g.setColor(Color.gray);
		g.drawLine(sliderPos,0,sliderPos,HEIGHT);

		// draw value at slider position
		// (don't draw value if no changes (e.g., the header))
		long stime = now-(long)(width-sliderPos)*scaleFactor;
		if (stime >= 0 && !snapshot.isEmpty()) {

			// binary search: the retained history is no longer bounded
			// by the panel width (issue #121)
			int at = Math.min(firstChangeAtOrBefore(snapshot,stime),
					snapshot.size()-1);
			Change lastChange = snapshot.get(at);
			String val = "HiZ";
			if (!lastChange.value().equals(off)) {
				val = BitSetUtils.ToString(lastChange.value(),base);
			}
			int w = fm.stringWidth(val);
			g.setColor(Color.white);
			g.fillRect(sliderPos-w-4,baseline-ascent,w+3,height);
			g.setColor(Color.magenta);
			g.drawString(val,sliderPos-w-1,baseline);

			// the name at the right edge may be scrolled out of view,
			// so label the cursor with the signal name too (issue
			// #121; bsiever prior art)
			if (!name.isEmpty()) {
				int nw = fm.stringWidth(name);
				g.setColor(Color.black);
				g.drawString(name,sliderPos-nw-1,baseline+ascent+3);
			}
		}

	} // end of paintComponent method

	/**
	 * Find the first (newest) committed change at or before a given
	 * time - the change whose value is in effect at that time.  The
	 * list is ordered newest first; binary search keeps repaints from
	 * scanning the whole retained history (issue #121).
	 *
	 * @param list The committed changes, newest first.
	 * @param time The simulation time to look up.
	 *
	 * @return The smallest index whose change time is at or before
	 *         time, or list.size() if every change is later.
	 */
	private static int firstChangeAtOrBefore(
			java.util.ArrayList<Change> list, long time) {

		int lo = 0;
		int hi = list.size();
		while (lo < hi) {
			int mid = (lo+hi) >>> 1;
			if (list.get(mid).when() <= time)
				hi = mid;
			else
				lo = mid+1;
		}
		return lo;
	} // end of firstChangeAtOrBefore method

	/**
	 * Test seam for the search over the committed snapshot (issue
	 * #121): package-private, used by TraceWindowingTest.
	 *
	 * @param time The simulation time to look up.
	 *
	 * @return See firstChangeAtOrBefore(list,time).
	 *
	 * @jls.testedby jls.sim.TraceWindowingTest#firstChangeAtOrBeforeMatchesTheLinearScan()
	 */
	int firstChangeAtOrBefore(long time) {

		return firstChangeAtOrBefore(changes,time);
	} // end of firstChangeAtOrBefore method
	
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

		// long math: the panel can now span the whole run (issue #121)
		long time = now-(long)(width-x)*scaleFactor;
		
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
	@Override
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
			
			/**
			 * Move this trace according to which popup menu item fired.
			 *
			 * @param event The event whose source is the chosen menu item.
			 */
			@Override
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
	@Override
	public void mouseMoved(MouseEvent event) {
		
		parent.mouseMoved(event);
	} // end of mouseMoved method
	
	// unused
	/**
	 * Unused mouse listener method.
	 *
	 * @param event The mouse event.
	 */
	@Override
	public void mouseEntered(MouseEvent event) {}
	/**
	 * Unused mouse listener method.
	 *
	 * @param event The mouse event.
	 */
	@Override
	public void mouseExited(MouseEvent event) {}
	/**
	 * Unused mouse listener method.
	 *
	 * @param event The mouse event.
	 */
	@Override
	public void mouseReleased(MouseEvent event) {}
	/**
	 * Unused mouse listener method.
	 *
	 * @param event The mouse event.
	 */
	@Override
	public void mouseClicked(MouseEvent event) {}
	/**
	 * Unused mouse motion listener method.
	 *
	 * @param event The mouse event.
	 */
	@Override
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
