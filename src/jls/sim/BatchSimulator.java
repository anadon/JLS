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

	// VCD export (issue #72): the file to write, or null for no export.
	// A non-null value enables trace accumulation in afterEvent even
	// when the -r printer flag (JLSInfo.printTrace) is off.
	private String vcdFileName = null;

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
	@Override
	public void stop() {

		stopping = true;
	} // end of stop method

	/**
	 * Stop simulation.
	 * It doesn't make sense to pause it in batch mode.
	 *
	 * @param which Ignored.
	 */
	@Override
	public void pause(boolean which) {

		stopping = true;
	} // end of pause method

	/**
	 * Run the simulator.
	 */
	public void runSim() {

		// reset clock/queues and initialize all elements
		initSimulation();

		// find watched elements and set up trace map
		findWatched(circuit);

		// run the shared event loop (tracing happens in afterEvent)
		runEventLoop();

	} // end of runSim

	/**
	 * Record a trace entry for the element that just reacted, if traces
	 * were requested and the element is watched.
	 *
	 * @param event The event that just reacted.
	 */
	@Override
	protected void afterEvent(SimEvent event) {

		// accumulate when any trace consumer is active: the -r printer
		// or the -vcd exporter (issue #72)
		if (!JLSInfo.printTrace && vcdFileName == null)
			return;

		// see if changing element is watched
		LogicElement el = (LogicElement)event.getCallBack();
		if (el.isWatched()) {

			// get the event trace for this element,
			// or create one if none yet
			LinkedList<TrEvent> events = eventTrace.get(el);

			// create bitset for HiZ
			BitSet off = new BitSet(el.getBits()+1);
			off.set(el.getBits());

			// normalize a HiZ (null) value to the marker BitSet before
			// comparing, so a value that stays HiZ is not recorded as a
			// change on every react (issue #72)
			BitSet current = el.getCurrentValue();
			if (current == null)
				current = off;

			// add an event to the end of the event list
			// (but not if the same value)
			TrEvent prev = events.getLast();
			if (!prev.value.equals(current)) {

				// add only if different
				TrEvent p = new TrEvent();
				p.time = event.getTime();
				p.value = current;
				events.add(p);
			}
		}
	} // end of afterEvent method
	
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
	 * @param circ The circuit (or subcircuit) to look in.
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

			@Override
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
	 * Set the VCD output file name, or null for no VCD export.
	 * Must be called before runSim so that afterEvent accumulates the
	 * trace (issue #72).
	 *
	 * @param fileName The VCD file to write, or null.
	 */
	public void setVcdFile(String fileName) {

		vcdFileName = fileName;
	} // end of setVcdFile method

	/**
	 * Write the accumulated trace of all watched elements to the file
	 * given to setVcdFile, as IEEE 1364-2001 (section 18) VCD.
	 *
	 * @throws java.io.IOException if the file cannot be written.
	 */
	public void writeVcd() throws java.io.IOException {

		java.nio.file.Files.write(
				java.nio.file.Paths.get(vcdFileName),
				toVcd().getBytes(java.nio.charset.StandardCharsets.UTF_8));
	} // end of writeVcd method

	/**
	 * Render the accumulated trace of all watched elements as an IEEE
	 * 1364-2001 (section 18) Value Change Dump. The exact format is a
	 * compatibility contract documented in docs/batch-interface.md
	 * (issue #72). Deterministic by construction: signals are declared
	 * and dumped in full-name order, no $date/$version headers, one
	 * JLS simulation time unit per VCD time unit (timescale 1 ns).
	 *
	 * @return the complete VCD text.
	 */
	public String toVcd() {

		StringBuilder out = new StringBuilder();

		// signals in full-name order: deterministic header, identifier
		// assignment, and per-timestamp change order
		TreeMap<String,LogicElement> signals =
			new TreeMap<String,LogicElement>();
		for (LogicElement el : eventTrace.keySet()) {
			signals.put(el.getFullName(), el);
		}
		Map<String,String> codes = new HashMap<String,String>();
		int next = 0;
		for (String name : signals.keySet()) {
			codes.put(name, vcdId(next));
			next += 1;
		}

		// header: no $date/$version sections (both optional in the
		// standard) so the same run always produces the same bytes
		out.append("$comment JLS batch simulation trace $end\n");
		out.append("$timescale 1 ns $end\n");
		out.append("$scope module ").append(circuit.getName())
			.append(" $end\n");
		for (Map.Entry<String,LogicElement> e : signals.entrySet()) {
			int bits = e.getValue().getBits();
			out.append("$var wire ").append(bits).append(' ')
				.append(codes.get(e.getKey())).append(' ')
				.append(e.getKey());
			if (bits > 1) {
				out.append(" [").append(bits - 1).append(":0]");
			}
			out.append(" $end\n");
		}
		out.append("$upscope $end\n");
		out.append("$enddefinitions $end\n");

		// fold each signal's event list into time -> value (the last
		// event recorded at a given time wins) and collect the set of
		// change times
		Map<String,TreeMap<Long,BitSet>> folded =
			new HashMap<String,TreeMap<Long,BitSet>>();
		TreeSet<Long> times = new TreeSet<Long>();
		for (Map.Entry<String,LogicElement> e : signals.entrySet()) {
			TreeMap<Long,BitSet> byTime = new TreeMap<Long,BitSet>();
			for (TrEvent ev : eventTrace.get(e.getValue())) {
				byTime.put(ev.time, ev.value);
				times.add(ev.time);
			}
			folded.put(e.getKey(), byTime);
		}

		// initial values (findWatched guarantees a time-0 entry for
		// every watched element)
		out.append("#0\n");
		out.append("$dumpvars\n");
		for (Map.Entry<String,LogicElement> e : signals.entrySet()) {
			BitSet value = folded.get(e.getKey()).get(0L);
			out.append(vcdValue(e.getValue(), value, codes.get(e.getKey())))
				.append('\n');
		}
		out.append("$end\n");

		// subsequent changes, grouped by ascending time, signals in
		// name order within each timestamp
		long last = 0;
		for (long t : times) {
			if (t == 0) {
				continue;
			}
			out.append('#').append(t).append('\n');
			for (Map.Entry<String,LogicElement> e : signals.entrySet()) {
				BitSet value = folded.get(e.getKey()).get(t);
				if (value != null) {
					out.append(vcdValue(e.getValue(), value,
							codes.get(e.getKey()))).append('\n');
				}
			}
			last = t;
		}

		// a final timestamp so viewers show the full simulated duration
		if (now > last) {
			out.append('#').append(now).append('\n');
		}
		return out.toString();
	} // end of toVcd method

	/**
	 * The VCD identifier code for the n'th signal: the printable ASCII
	 * characters '!' (33) through '~' (126), extended to multiple
	 * characters after 94 signals, assigned in signal-name order.
	 *
	 * @param index The zero-based signal number.
	 *
	 * @return the identifier code.
	 */
	private static String vcdId(int index) {

		StringBuilder code = new StringBuilder();
		int n = index + 1;
		while (n > 0) {
			n -= 1;
			code.insert(0, (char)('!' + n % 94));
			n /= 94;
		}
		return code.toString();
	} // end of vcdId method

	/**
	 * One VCD value-change entry. JLS values are two-state plus HiZ,
	 * so only 0, 1 and z ever appear ('x' never does): a single-bit
	 * signal becomes 0<code>, 1<code> or z<code>; a multi-bit signal
	 * becomes a binary vector b<value> <code> with leading zeros
	 * omitted, or bz <code> when the whole signal is HiZ.
	 *
	 * @param el The signal's element (for its bit width).
	 * @param value The recorded value, with HiZ encoded as the trace's
	 *        marker BitSet (only bit el.getBits() set).
	 * @param code The signal's identifier code.
	 *
	 * @return the value-change line, without the newline.
	 */
	private static String vcdValue(LogicElement el, BitSet value,
			String code) {

		int bits = el.getBits();
		BitSet off = new BitSet(bits + 1);
		off.set(bits);
		boolean hiZ = value.equals(off);
		if (bits == 1) {
			if (hiZ) {
				return "z" + code;
			}
			return (BitSetUtils.ToLong(value) == 0 ? "0" : "1") + code;
		}
		if (hiZ) {
			return "bz " + code;
		}
		return "b" + BitSetUtils.ToBigInteger(value).toString(2)
				+ " " + code;
	} // end of vcdValue method

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

