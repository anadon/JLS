// Minimal Swing control program for the Wayland first-light rig
// (issue #101, section 9 "Falsification Criteria").
//
// The rig launches this before JLS on the same runtime, the same
// toolkit (-Dawt.toolkit.name=WLToolkit), and the same headless sway
// compositor, so every run separates the two possible failure modes
// without human interpretation:
//
//   control succeeds, JLS fails  ->  the defect is in JLS's startup path
//                                    (census/fix issues)
//   control fails too            ->  the blocker is upstream
//                                    (JBR/Wakefield or the compositor)
//
// It is intentionally the smallest thing that exercises the toolkit's
// window-mapping and text-rendering paths: one JFrame, one JLabel.
// The frame title is matched by name in `swaymsg -t get_tree`, so keep
// it in sync with scripts/wayland-rig.sh if it ever changes.
//
// Run standalone (single-file source launch, no build step needed):
//
//     java -Dawt.toolkit.name=WLToolkit scripts/HelloSwingControl.java
//
// The process stays up until killed; the rig kills it once its window
// has (or has not) appeared.

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public final class HelloSwingControl {

	/** Window title the rig greps for in the compositor tree. */
	private static final String TITLE = "HelloSwingControl";

	private HelloSwingControl() {
		// static entry point only
	} // end of HelloSwingControl constructor

	/**
	 * Maps a minimal frame and parks the main thread; the AWT event
	 * dispatch thread keeps the JVM alive until the rig kills it.
	 *
	 * @param args ignored
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame(TITLE);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.add(new JLabel("first light", SwingConstants.CENTER));
			frame.setSize(320, 200);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	} // end of main method

} // end of HelloSwingControl class
