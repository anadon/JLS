package jls.edit;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import jls.Circuit;
import jls.core.Geometry;
import jls.elem.Element;
import jls.elem.Input;
import jls.elem.Output;
import jls.elem.State;
import jls.elem.StateMachine;

/**
 * GUI-side drawing and printing for {@link StateMachine} (issue #77):
 * the rounded box, the divider lines, the machine name and the
 * per-signal input/output labels, moved verbatim from the former
 * {@code StateMachine.draw}. The two printed pages - the state diagram
 * and the per-state output summary - moved here from the former
 * {@code StateMachine.print} (its {@link Printable}) and
 * {@code StateMachine.makeOutSum}; {@link CircuitRenderer#addToBook}
 * books them.
 */
public final class StateMachineRenderer implements ElementRenderer {

	@Override
	public void draw(Graphics g, Element el) {

		StateMachine sm = (StateMachine) el;
		int x = sm.getX();
		int y = sm.getY();
		int width = sm.getWidth();
		int height = sm.getHeight();

		// set up
		int s = Geometry.SPACING;
		int d2 = Geometry.POINT_DIAMETER/2;
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int descent = fm.getDescent();
		int fontHeight = ascent + descent;

		// draw context
		ElementRenderSupport.drawHighlight(g, sm);

		// draw box
		g.setColor(Color.BLACK);
		g.drawRoundRect(x,y,width,height,6,6);
		g.drawLine(x,y+height-4*s,x+width,y+height-4*s);
		g.drawLine(x,y+height-2*s,x+width,y+height-2*s);

		// draw name
		String dname = sm.getName();
		if (dname.isEmpty()) {
			dname = "State Machine";
		}
		int w = fm.stringWidth(dname);
		g.drawString(dname,x+(width-w)/2,y+height-3*s-fontHeight/2+ascent);

		// draw inputs and outputs and their names
		for (Input input : sm.getInputs()) {
			int dy = input.getY();
			g.setColor(Color.black);
			g.drawString(input.getName(),x+d2,dy-fontHeight/2+ascent);
			input.draw(g);
		}
		for (Output output : sm.getOutputs()) {
			int dy = output.getY();
			int ow = fm.stringWidth(output.getName());
			g.setColor(Color.black);
			g.drawString(output.getName(),x+width-ow-d2,dy-fontHeight/2+ascent);
			output.draw(g);
		}

	} // end of draw method

	/**
	 * The printable state-diagram page for a state machine. Moved from
	 * the former {@code StateMachine.print}.
	 *
	 * @param sm The state machine.
	 *
	 * @return a Printable that draws the scaled state diagram.
	 */
	public static Printable diagramPage(StateMachine sm) {

		return new DiagramPage(sm);
	} // end of diagramPage method

	/**
	 * The printable state-diagram page for one state machine. A named type
	 * (rather than an anonymous {@link Printable}) so the canonical
	 * page-order check ({@code PrintPageOrderTest}) can correlate a booked
	 * page back to its machine.
	 */
	public static final class DiagramPage implements Printable {

		/** The state machine this page prints. */
		private final StateMachine machine;

		/**
		 * @param machine The state machine to print.
		 */
		DiagramPage(StateMachine machine) {

			this.machine = machine;
		} // end of constructor

		/**
		 * The state machine this page prints.
		 *
		 * @return the machine.
		 */
		public StateMachine machine() {

			return machine;
		} // end of machine method

		/**
		 * Print the state machine's state diagram.
		 */
		@Override
		public int print(Graphics g, PageFormat format, int pagenum) {

			// use better graphics
			Graphics2D gg = (Graphics2D)g;

			// construct name
			Circuit c = machine.getCircuit();
			String nm = machine.getName() + " in " + machine.getCircuit().getName();
			while (c.isImported()) {
				c = c.getSubElement().getCircuit();
				nm += " in " + c.getName();
			}

			// set up
			FontMetrics fm = g.getFontMetrics();
			int ascent = fm.getAscent();
			int descent = fm.getDescent();
			int fontHeight = ascent + descent;

			// get bounds
			Rectangle bounds = null;
			for (State state : machine.getStates()) {
				if (bounds == null)
					bounds = StateRenderer.getBounds(g, state);
				else
					bounds.add(StateRenderer.getBounds(g, state));
			}
			if (bounds == null) {
				bounds = new Rectangle(0,0,1,1);
			}

			// scale and adjust as needed
			double width = format.getImageableWidth();
			double height = format.getImageableHeight();
			double scale = 1.0;
			if (bounds.width > width) {
				scale = 1.0*width/bounds.width;
			}
			if (bounds.height > height) {
				scale = Math.min(scale,1.0*height/bounds.height);
			}
			gg.translate(format.getImageableX(),format.getImageableY());
			gg.drawString(nm,0,ascent);
			gg.translate(0,2*fontHeight);
			gg.scale(scale,scale);
			gg.translate(-bounds.x,-bounds.y);

			// print
			for (State state : machine.getStates()) {
				StateRenderer.draw(gg, state);
			}
			return Printable.PAGE_EXISTS;
		} // end of print method

	} // end of DiagramPage class

	/**
	 * The printable per-state output summary page for a state machine, or
	 * null if no state has any outputs. Moved from the former
	 * {@code StateMachine.makeOutSum}.
	 *
	 * @param sm The state machine.
	 *
	 * @return a Printable summarizing every state's outputs, or null.
	 */
	public static Printable outSummaryPage(StateMachine sm) {

		// return null if no outputs
		int outs = 0;
		for (State state : sm.getStates()) {
			outs += state.numOuts();
		}
		if (outs == 0)
			return null;

		return new Printable() {

			/**
			 * Print a grid summarizing every state's output values.
			 */
			@Override
			public int print(Graphics g, PageFormat format, int pagenum) {

				// set up
				int pageWidth = (int)format.getImageableWidth();
				g.translate((int)(format.getImageableX()),(int)(format.getImageableY()));
				int x = 0;
				int y = 0;

				// sort states by name
				SortedMap<String,State> sorted = new TreeMap<String,State>();
				for (State state : sm.getStates()) {
					sorted.put(state.getName(),state);
				}

				// print outputs for each state
				int maxHeight = 0;
				SortedSet<Integer> borderX = new TreeSet<Integer>();
				for (String sname : sorted.keySet()) {
					State state = sorted.get(sname);

					// determine maximum width and height
					int stateWidth = StateRenderer.maxWidth(g, state);
					maxHeight = Math.max(maxHeight,StateRenderer.maxHeight(g, state));

					// print next state to right unless no more room
					if (x+stateWidth > pageWidth) {
						g.drawLine(0,y+maxHeight+5,x-10,y+maxHeight+5);
						int lines = borderX.size()-1;
						for (int i=0; i<lines; i+=1) {
							int bx = borderX.first();
							g.drawLine(bx,y,bx,y+maxHeight);
							borderX.remove(bx);
						}
						x = 0;
						y += maxHeight+10;
						maxHeight = 0;
						borderX.clear();
					}

					// print state name and outputs
					borderX.add(x+stateWidth+5);
					StateRenderer.print(g, state, x, y);
					x += stateWidth + 10;

				}

				// print vertical borders on last line
				if (borderX.size() > 1) {
					int lines = borderX.size()-1;
					for (int i=0; i<lines; i+=1) {
						int bx = borderX.first();
						g.drawLine(bx,y,bx,y+maxHeight);
						borderX.remove(bx);
					}
				}

				return Printable.PAGE_EXISTS;
			}
		};

	} // end of outSummaryPage method

} // end of StateMachineRenderer class
