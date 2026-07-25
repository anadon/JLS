package jls.edit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.BitSet;
import java.util.SortedSet;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import jls.BitSetUtils;
import jls.JLSInfo;
import jls.elem.Memory;

/**
 * GUI-side "show memory contents" window for {@link Memory} (issue #77).
 * Moved verbatim from the former {@code Memory.showCurrentValue} Swing
 * body, so the model stays headless; it reads the stored words through the
 * memory's public accessors and renders them exactly as before.
 */
public final class MemoryContentsDialog {

	/**
	 * Prevents instantiation; this class only holds a static entry point.
	 */
	private MemoryContentsDialog() {
	} // no instances

	/**
	 * Show the non-zero words of a memory in a modal window.
	 *
	 * @param mem The memory whose contents to display.
	 */
	public static void show(Memory mem) {

		// set up
		final JDialog contents = new JDialog(JLSInfo.frame, true);
		Container window = contents.getContentPane();
		window.setLayout(new BorderLayout());

		// create addr/data display
		SortedSet<Integer> addrs = mem.storedAddresses();
		int bits = mem.getBits();
		JPanel display = new JPanel(new GridLayout(addrs.size(), 2, 10, 3));
		for (int addr : addrs) {

			// address info
			String hexAddr = Integer.toHexString(addr);
			String allAddr = "0x" + hexAddr + " (" + addr + "):";
			JLabel addrLabel = new JLabel(allAddr, SwingConstants.RIGHT);
			addrLabel.setOpaque(true);
			addrLabel.setBackground(Color.WHITE);
			display.add(addrLabel);

			// data info
			BitSet temp = new BitSet(1);
			if (mem.getCurrentValue(addr) != null) {
				temp = mem.getCurrentValue(addr);
			}
			String hex = BitSetUtils.ToString(temp, 16);
			String unsigned = BitSetUtils.ToString(temp, 10);
			String signed = BitSetUtils.ToStringSigned(temp, bits);
			String value = "0x" + hex + " (" + unsigned + " unsigned, "
					+ signed + " signed)";
			JLabel dataLabel = new JLabel(value);
			dataLabel.setOpaque(true);
			dataLabel.setBackground(Color.WHITE);
			display.add(dataLabel);
		}
		JScrollPane pane = new JScrollPane(display);
		int width = (int) (display.getPreferredSize().getWidth());
		int height = (int) (display.getPreferredSize().getHeight());
		pane.setPreferredSize(new Dimension(width + 50, Math.min(height, 400)));
		window.add(pane, BorderLayout.CENTER);
		JButton ok = new JButton("ok");
		ok.addActionListener(event -> contents.dispose());
		window.add(ok, BorderLayout.SOUTH);
		window.add(new JLabel("Non-Zero Words:", SwingConstants.CENTER),
				BorderLayout.NORTH);

		contents.pack();
		contents.setLocation(10, 10);
		contents.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		contents.setVisible(true);
	} // end of show method

} // end of MemoryContentsDialog class
