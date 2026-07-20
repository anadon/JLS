package jls;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import org.jspecify.annotations.Nullable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

/**
 * Displays the built-in tutorial in a non-modal, resizable dialog.
 * The tutorial is a fixed sequence of bundled HTML pages; the dialog
 * can be opened at any page (each Help&rarr;Tutorial menu item jumps
 * straight to one) and offers Previous/Next buttons so a student can
 * read the whole sequence without going back to the menu (issue #73).
 *
 * @author David A. Poplawski
 */
public final class Tutorial extends JDialog {

	/**
	 * The bundled tutorial pages, in reading order, as resource names
	 * relative to the {@code jls/tutorial/} classpath directory.
	 */
	static final String[] PAGES = {
			"tutorial1.html", "tutorial2.html",
			"tutorial3.html", "tutorial4.html"};

	/**
	 * Human-readable page titles, parallel to {@link #PAGES}; these
	 * match the Help&rarr;Tutorial menu item labels.
	 */
	static final String[] TITLES = {
			"Introduction", "4-Bit Counter",
			"Full Adder", "Sign Extension"};

	/** Initial width of the tutorial text area, in pixels. */
	private static final int WIDTH = 760;

	/** Initial height of the tutorial text area, in pixels. */
	private static final int HEIGHT = 640;

	/** Read-only pane the current tutorial page is rendered in. */
	private final JEditorPane editorPane = new JEditorPane();

	/** Scroll pane wrapping the tutorial text. */
	private final JScrollPane editorScrollPane =
			new JScrollPane(editorPane);

	/** Navigates to the previous page; disabled on the first page. */
	private final JButton previous = new JButton("< Previous");

	/** Navigates to the next page; disabled on the last page. */
	private final JButton next = new JButton("Next >");

	/** Shows "Page x of y: title" between the navigation buttons. */
	private final JLabel position =
			new JLabel("", SwingConstants.CENTER);

	/** Index into {@link #PAGES} of the page being displayed. */
	private int page;

	/**
	 * Create and display the dialog, opened at the given page.
	 *
	 * @param frame The parent frame for the dialog.
	 * @param page Index of the page to open at (0 to
	 *		{@code pageCount()-1}); out-of-range values are clamped.
	 */
	public Tutorial(@Nullable Frame frame, int page) {

		super(frame, "JLS Tutorial", false);

		// tutorial text
		editorPane.setEditable(false);
		editorScrollPane.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		add(editorScrollPane, BorderLayout.CENTER);

		// previous/next navigation
		JPanel nav = new JPanel(new BorderLayout());
		nav.add(previous, BorderLayout.WEST);
		nav.add(position, BorderLayout.CENTER);
		nav.add(next, BorderLayout.EAST);
		add(nav, BorderLayout.SOUTH);
		previous.addActionListener(new ActionListener() {
			/**
			 * Show the previous tutorial page when the button is pushed.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				setPage(Tutorial.this.page - 1);
			}
		});
		next.addActionListener(new ActionListener() {
			/**
			 * Show the next tutorial page when the button is pushed.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				setPage(Tutorial.this.page + 1);
			}
		});

		// finish up
		setPage(page);
		pack();
		setLocationRelativeTo(frame);
		setVisible(true);
	} // end of constructor

	/**
	 * Display the given tutorial page and update the window title, the
	 * position label and the navigation buttons to match.
	 *
	 * @param newPage Index of the page to display; clamped to the
	 *		valid range.
	 */
	private void setPage(int newPage) {

		page = Math.max(0, Math.min(newPage, pageCount() - 1));
		java.net.URL pageURL =
				Tutorial.class.getResource("tutorial/" + pageFile(page));
		if (pageURL == null) {
			System.err.println("Couldn't find file: " + pageFile(page));
		} else {
			try {
				editorPane.setPage(pageURL);
			} catch (IOException e) {
				System.err.println(
						"Attempted to read a bad URL: " + pageURL);
			}
		}
		setTitle("JLS Tutorial - " + pageTitle(page));
		position.setText("Page " + (page + 1) + " of " + pageCount()
				+ ": " + pageTitle(page));
		previous.setEnabled(page > 0);
		next.setEnabled(page < pageCount() - 1);
	} // end of setPage method

	/**
	 * Get the number of tutorial pages.
	 *
	 * @return the number of pages in the tutorial sequence.
	 */
	static int pageCount() {

		return PAGES.length;
	} // end of pageCount method

	/**
	 * Get the bundled resource name of a tutorial page.
	 *
	 * @param index The page index (0 to {@code pageCount()-1}).
	 * @return the page's file name within {@code jls/tutorial/}.
	 */
	static String pageFile(int index) {

		return PAGES[index];
	} // end of pageFile method

	/**
	 * Get the human-readable title of a tutorial page.
	 *
	 * @param index The page index (0 to {@code pageCount()-1}).
	 * @return the page's title, as shown in the dialog and the menu.
	 */
	static String pageTitle(int index) {

		return TITLES[index];
	} // end of pageTitle method

} // end of Tutorial class
