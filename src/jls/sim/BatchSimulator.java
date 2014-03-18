package jls.sim;

import jls.*;
import jls.elem.*;
import jls.edit.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.math.*;
import javax.print.PrintService;
import javax.swing.*;
import javax.swing.text.AbstractDocument;

import java.util.*;

/**
 * Event driven circuit simulator.
 *
 * @author David A. Poplawski
 */
public class BatchSimulator extends Simulator {

	// for printing traces
	private class TrEvent {
		public long time;
		public BitSet value;
	}
	private Map<LogicElement,LinkedList<TrEvent>> eventTrace = 
		new HashMap<LogicElement,LinkedList<TrEvent>>();

	/**
	 * Create a new Simulator object.
	 */
	public BatchSimulator() {

		// save a reference to itself
		me = this;
	} // end of constructor

	/**
	 * Stop (end) the simulation, if there is one.
	 */
	public void stop() {

		stopping = true;
	} // end of stop method

	/**
	 * Stop simulation.
	 * It doesn't make sense to pause it in batch mode.
	 *
	 * @param which Ignored.
	 */
	public void pause(boolean which) {

		stopping = true;
	} // end of pause method

	/**
	 * Run the simulator.
	 */
	public void runSim() {

		// reset clock and empty eventQueue
		stopping = false;
		now = 0;
		eventQueue.clear();
		dupCheck.clear();

		// initialize all input points
		initInputs(circuit);

		// initialize all elements
		for (Element el : circuit.getElements()) {
			if (el instanceof LogicElement) {
				LogicElement lel = (LogicElement)el;
				lel.initSim(me);
			}
		}

		// find watched elements and set up trace map
		findWatched(circuit);

		// event loop
		while (!stopping && !eventQueue.isEmpty() && now <= maxTime) {

			// get the next event
			SimEvent event = eventQueue.poll();
			dupCheck.remove(event);

			// update clock
			now = event.getTime();
			
			// quit if after time limit
			if (now > maxTime) {
				now = maxTime;
				break;
			}

			// make the event happen
			event.getCallBack().react(now,me,event.getTodo());

			// trace it
			if (JLSInfo.printTrace) {

				// see if changing element is watched
				LogicElement el = (LogicElement)event.getCallBack();
				if (el.isWatched()) {

					// get the event trace for this element, 
					// or create one if none yet
					LinkedList<TrEvent> events = eventTrace.get(el);

					// create bitset for HiZ
					BitSet off = new BitSet(el.getBits()+1);
					off.set(el.getBits());

					// add an event to the end of the event list
					// (but not if the same value)
					TrEvent prev = events.getLast();
					if (!prev.value.equals(el.getCurrentValue())) {

						// add only if different
						TrEvent p = new TrEvent();
						p.time = event.getTime();
						p.value = el.getCurrentValue();
						if (p.value == null)
							p.value = off;
						events.add(p);
					}
				}
			}

		} // end of event loop

	} // end of runSim
	
	/**
	 * Create and add TestGen element to circuit.
	 * Remove any SigGen's in the circuit.
	 */
	public void addTestGen() {

		// create test signal element if necessary
		if (testFileName != null) {
			TestGen gen = new TestGen(circuit);
			circuit.addElement(gen);
			gen.setFile(testFileName);
			
			// remove any signal generators from this circuit
			// (signal generators in subcircuits will disable themselves)
			Set<Element> gens = new HashSet<Element>();
			for (Element el : circuit.getElements()) {
				if (el instanceof SigGen) {
					gens.add(el);
				}
			}
			for (Element el : gens) {
				circuit.remove(el);
			}
		}
	} // end of addTestGen method

	/**
	 * Find all watched elements and add entries to batch trace map.
	 * Recursively checks all subcircuits.
	 * 
	 * @param circuit The circuit (or subcircuit) to look in.
	 */
	private void findWatched(Circuit circ) {

		for (Element el : circ.getElements()) {
			if (el instanceof SubCircuit) {
				SubCircuit sub = (SubCircuit)el;
				findWatched(sub.getSubCircuit());
			}
			else if (el.isWatched()) {
				LogicElement lel = (LogicElement)el;
				LinkedList<TrEvent> events = new LinkedList<TrEvent>();
				TrEvent event = new TrEvent();
				event.time = 0;
				event.value = lel.getCurrentValue();
				if (event.value == null) {
					event.value = new BitSet(lel.getBits()+1);
					event.value.set(lel.getBits());
				}
				events.add(event);
				eventTrace.put(lel,events);
			}
		}
	} // end of findWatched method

	/**
	 * Print trace.
	 * 
	 * @param printer The name of the printer to print to.
	 */
	public void printTrace(String printer) {

		// set up printer job
		PrinterJob job = PrinterJob.getPrinterJob();
		PrintService [] services = job.lookupPrintServices();
		PrintService want = null;
		if (printer == null) {
			System.out.println("no printer specified, use -p");
			return;
		}
		for (PrintService s : services) {
			if (s.getName().equals(printer)) {
				want = s;
			}
		}
		if (want == null) {
			System.out.println(printer + " is an invalid printer");
			System.exit(1);
		}
		try {
			job.setPrintService(want);
		}
		catch (PrinterException ex) {
			System.out.println(printer + " is an invalid printer");
		}
		PageFormat format = job.defaultPage();
		format.setOrientation(PageFormat.LANDSCAPE);

		// printable object to do the work
		Printable pr = new Printable() {

			public int print(Graphics g, PageFormat format, int pagenum) {

				int HEIGHT = 40;

				if (pagenum > 0) return Printable.NO_SUCH_PAGE;
				Graphics2D gg = (Graphics2D)g;
				gg.translate(format.getImageableX(),format.getImageableY());
				double pageWidth = format.getImageableWidth();
				double pageHeight = format.getImageableHeight();

				FontMetrics fm = gg.getFontMetrics();
				int ascent = fm.getAscent();
				int descent = fm.getDescent();
				int width = 0;
				int height = 0;
				long maxTime = 0;
				Map<String,LogicElement> map = new TreeMap<String,LogicElement>();
				for (LogicElement el : eventTrace.keySet()) {
					String name = " " + el.getFullName();
					width = Math.max(width,fm.stringWidth(name));
					height += HEIGHT;
					map.put(name, el);
					maxTime = Math.max(maxTime,eventTrace.get(el).getLast().time);
				}
				width += 1000;
				height += HEIGHT;
				double timeScaleFactor = 1000.0/(maxTime+10);

				double scale = 1.0;
				if (width > pageWidth) {
					scale = 1.0*pageWidth/width;
				}
				if (height > pageHeight) {
					scale = Math.min(scale,1.0*pageHeight/height);
				}
				gg.scale(scale,scale);

				// draw time scale
				int inc = (int)(maxTime/10);
				long time = 0;
				gg.setColor(Color.gray);
				for (int i=0; i<=10; i+=1) {
					int xpos = (int)(time*timeScaleFactor);
					g.drawLine(xpos, 0, xpos, height-HEIGHT/2);
					g.drawString(time+"", xpos, height-descent);
					time += inc;
				}

				// draw all traces
				int top = 0;
				int offset = (HEIGHT - (ascent+descent))/2 + ascent;
				gg.setColor(Color.black);
				for (String sig : map.keySet()) {

					// draw signal history
					LogicElement el = map.get(sig);
					LinkedList<TrEvent> events = eventTrace.get(el);

					if (el.getBits() == 1) {

						// create bitset for HiZ
						BitSet off = new BitSet(el.getBits()+1);
						off.set(el.getBits());

						// single bit signal
						long prevValue = BitSetUtils.ToLong(events.getFirst().value);
						if (events.getFirst().value.equals(off))
							prevValue = -1;
						int prevXpos = 0;
						for (TrEvent event : events) {
							int xpos = (int)(event.time*timeScaleFactor + 0.5);

							// draw horizontal line
							int ypos = 0;
							if (prevValue == 0) {
								gg.drawLine(prevXpos,top+30,xpos,top+30);
								ypos = top+30;
							}
							else if (prevValue == 1){
								gg.drawLine(prevXpos,top+10,xpos,top+10);
								ypos = top+10;
							}
							else {
								gg.drawLine(prevXpos,top+20,xpos,top+20);
								ypos = top+20;
							}

							// update
							prevValue = BitSetUtils.ToLong(event.value);
							if (event.value.equals(off))
								prevValue = -1;
							prevXpos = xpos;

							// draw vertical line
							if (prevValue == 0) {
								gg.drawLine(xpos,ypos,xpos,top+30);
							}
							else if (prevValue == 1) {
								gg.drawLine(xpos,ypos,xpos,top+10);
							}
							else {
								gg.drawLine(xpos,ypos,xpos,top+20);
							}

						}
						if (prevValue == 0) {
							gg.drawLine(prevXpos,top+30,1000,top+30);
						}
						else if (prevValue == 1) {
							gg.drawLine(prevXpos,top+10,1000,top+10);
						}
						else {
							gg.drawLine(prevXpos,top+20,1000,top+20);
						}
					}
					else {

						// create bitset for HiZ
						BitSet off = new BitSet(el.getBits()+1);
						off.set(el.getBits());

						// multiple bit signal
						BigInteger prevValue = BitSetUtils.ToBigInteger(events.getFirst().value);
						if (events.getFirst().value.equals(off))
							prevValue = null;
						int prevXpos = 0;
						for (TrEvent event : eventTrace.get(map.get(sig))) {
							int xpos = (int)(event.time*timeScaleFactor + 0.5);

							// draw horizontal line
							if (prevValue != null) {
								gg.drawLine(prevXpos,top+30,xpos,top+30);
								gg.drawLine(prevXpos,top+10,xpos,top+10);
							}
							else {
								gg.drawLine(prevXpos,top+20,xpos,top+20);
							}

							// draw vertical line
							gg.drawLine(xpos,top+10,xpos,top+30);
							
							// draw signal value
							if (prevValue != null) {
								String val = String.format(" %s ", prevValue.toString(16));
								int valWidth = fm.stringWidth(val);
								if (valWidth <= xpos-prevXpos) {
									gg.drawString(val, prevXpos,
											top+(HEIGHT-ascent-descent)/2+ascent);
								}
							}

							// update
							prevValue = BitSetUtils.ToBigInteger(event.value);
							if (event.value.equals(off))
								prevValue = null;
							prevXpos = xpos;
						}
						
						// draw extra signal at end
						if (prevValue != null) {
							gg.drawLine(prevXpos,top+30,1000,top+30);
							gg.drawLine(prevXpos,top+10,1000,top+10);
						}
						else {
							gg.drawLine(prevXpos,top+20,1000,top+20);
						}
						if (prevValue != null) {
							String val = String.format(" %s ", prevValue.toString(16));
							int valWidth = fm.stringWidth(val);
							if (valWidth <= 1000-prevXpos) {
								gg.drawString(val, prevXpos,
										top+(HEIGHT-ascent-descent)/2+ascent);
							}
						}
					}

					// draw signal name
					gg.drawString(sig, 1000, top+offset);

					top += HEIGHT;
				}

				return Printable.PAGE_EXISTS;
			} // end of print method
		};

		// set up book
		Book book = new Book();
		book.append(pr,format);
		job.setPageable(book);

		// print the trace
		try {
			job.print();
		}
		catch (PrinterException ex) {
			System.out.println("printing error: " + ex.getMessage());
		}
	} // end of printTrace method
	
	/**
	 * Display reason for stopping and the time at which it stopped.
	 */
	public void displayOutcome() {

		String reason = "Simulation Complete";
		if (stopping)
			reason = "Simulation Stopped";
		else if (now >= maxTime)
			reason = "Simulation Time Limit";
		else if (eventQueue.size() == 0)
			reason = "Simulation: No More Activity";
		System.out.println(reason + " at " + now);
	} // end of displayOutcome method

} // end of BatchSimulator class

