package jls;

import jls.elem.LogicElement;

import org.jspecify.annotations.Nullable;
import jls.sim.TraceSample;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.print.PrintService;

/**
 * Print the waveform traces recorded by a batch simulation run (the
 * {@code -r} command-line flag). This is the GUI/print side of the
 * batch trace pipeline: the headless core accumulates
 * {@link jls.sim.TraceSample} lists
 * ({@link jls.sim.BatchSimulator#getTraceSamples}), and this class owns
 * every AWT printing concern that used to live inside the engine
 * (issue #77).
 *
 * @author David A. Poplawski
 */
public final class BatchTracePrinter {

	/**
	 * Not instantiable: all members are static.
	 */
	private BatchTracePrinter() {
	} // end of constructor

	/**
	 * Print the recorded traces of all watched elements.
	 *
	 * @param eventTrace The recorded traces, oldest sample first (from
	 *        {@link jls.sim.BatchSimulator#getTraceSamples}).
	 * @param printer The name of the printer to print to.
	 */
	public static void printTrace(
			Map<LogicElement,List<TraceSample>> eventTrace, @Nullable String printer) {

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
		Printable pr = tracePrintable(eventTrace);

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
	 * The one-page Printable that renders the recorded traces. Package
	 * visible so the print path can be exercised headless (rendered
	 * into an image) without a real printer, like PrintPathSmokeTest
	 * does for circuits.
	 *
	 * @param eventTrace The recorded traces, oldest sample first.
	 *
	 * @return a Printable rendering all traces onto a single page.
	 *
	 * @jls.testedby jls.BatchTracePrinterTest#tracePrintableRendersTheRecordedSamples()
	 */
	static Printable tracePrintable(
			final Map<LogicElement,List<TraceSample>> eventTrace) {

		return new Printable() {

			/**
			 * Render the accumulated waveform traces onto the print page.
			 *
			 * @param g The graphics context to draw into.
			 * @param format The page geometry.
			 * @param pagenum The zero-based page number to render.
			 *
			 * @return PAGE_EXISTS if drawn, NO_SUCH_PAGE past the last page.
			 */
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
					maxTime = Math.max(maxTime,
							eventTrace.get(el).getLast().time());
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
					if (el == null) {
						continue;
					}
					List<TraceSample> events = eventTrace.get(el);
					if (events == null) {
						continue;
					}

					if (el.getBits() == 1) {

						// create bitset for HiZ
						BitSet off = new BitSet(el.getBits()+1);
						off.set(el.getBits());

						// single bit signal
						long prevValue = BitSetUtils.ToLong(events.getFirst().value());
						if (events.getFirst().value().equals(off))
							prevValue = -1;
						int prevXpos = 0;
						for (TraceSample event : events) {
							int xpos = (int)(event.time()*timeScaleFactor + 0.5);

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
							prevValue = BitSetUtils.ToLong(event.value());
							if (event.value().equals(off))
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
						BigInteger prevValue = BitSetUtils.ToBigInteger(events.getFirst().value());
						if (events.getFirst().value().equals(off))
							prevValue = null;
						int prevXpos = 0;
						for (TraceSample event : events) {
							int xpos = (int)(event.time()*timeScaleFactor + 0.5);

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
							prevValue = BitSetUtils.ToBigInteger(event.value());
							if (event.value().equals(off))
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
	} // end of tracePrintable method

} // end of BatchTracePrinter class
