package jls.edit;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

import org.jspecify.annotations.Nullable;

import jls.Circuit;
import jls.elem.TruthTable;

/**
 * The printable page for a {@link TruthTable} (issue #77). Moved verbatim
 * from the former {@code TruthTable.print}; the element no longer
 * implements {@link Printable} - {@code CircuitRenderer.addToBook} books
 * this wrapper instead - so the model stays headless.
 */
public final class TruthTablePrintable implements Printable {

	/** The truth table this page prints. */
	private final TruthTable ttelem;
	/** The display panel, created lazily the first time this page prints. */
	private @Nullable DisplayBool disp;

	/**
	 * Wrap a truth table as a printable page.
	 *
	 * @param ttelem The truth table to print.
	 */
	public TruthTablePrintable(TruthTable ttelem) {

		this.ttelem = ttelem;
	} // end of constructor

	/**
	 * Get the wrapped truth table.
	 *
	 * @return the truth table this page prints.
	 */
	public TruthTable getTruthTable() {

		return ttelem;
	} // end of getTruthTable method

	/**
	 * Print the truth table.
	 *
	 * @param g The graphics object to use.
	 * @param format Page format info.
	 * @param pagenum Ignored.
	 *
	 * @return Printable.PAGE_EXISTS.
	 */
	@Override
	public int print(Graphics g, PageFormat format, int pagenum) {

		// use better graphics
		Graphics2D gg = (Graphics2D)g;

		// the display panel is otherwise created only when the edit
		// dialog opens, so printing a freshly loaded circuit crashed
		// here with an NPE (found by PrintPathSmokeTest, issue #91)
		DisplayBool d = disp;
		if (d == null) {
			d = new DisplayBool(ttelem);
			disp = d;
		}

		// construct name
		Circuit c = ttelem.getCircuit();
		String nm = ttelem.getName() + " in " + ttelem.getCircuit().getName();
		while (c.isImported()) {
			jls.elem.SubCircuit sub = c.getSubElement();
			if (sub == null) {
				throw new IllegalStateException(
						"imported circuit has no sub-element");
			}
			c = sub.getCircuit();
			nm += " in " + c.getName();
		}

		// set up
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int descent = fm.getDescent();
		int fontHeight = ascent + descent;

		// get bounds
		d.doLayout(ttelem.getInputNames(),ttelem.getOutputNames(),
				ttelem.getTable(),gg);
		Dimension bounds = d.getPreferredSize();

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

		// print
		d.print(gg);

		return Printable.PAGE_EXISTS;
	} // end of print method

} // end of TruthTablePrintable class
