package jls;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * The in-app help system (issue #11): a plain Swing viewer over the
 * bundled HTML help content, replacing the abandoned JavaHelp library.
 *
 * The existing help metadata is reused as-is: help/Map.jhm maps topic
 * ids to HTML files, and help/JLSHelpTOC.xml provides the table of
 * contents shown as a tree. Both are simple enough to parse directly.
 */
public final class Help {

	private static Map<String,String> topicToUrl = null;
	private static JFrame window = null;
	private static JEditorPane page = null;
	private static JTree toc = null;
	private static Map<String,TreePath> topicToTocPath = null;

	private Help() {
	}

	/**
	 * Make a button open the help window at a topic when pressed.
	 *
	 * @param button The help button.
	 * @param topic  The topic id (a Map.jhm target).
	 */
	public static void enableHelpOnButton(JButton button, final String topic) {

		button.addActionListener(e -> showTopic(topic));
	} // end of enableHelpOnButton method

	/**
	 * Open (or raise) the help window and show a topic.
	 *
	 * @param topic The topic id (a Map.jhm target); unknown ids show the
	 *              overview page.
	 */
	public static void showTopic(String topic) {

		loadTopicMap();
		if (window == null)
			buildWindow();

		String url = topicToUrl.get(topic);
		if (url == null)
			url = topicToUrl.getOrDefault("top", "overview.html");
		showPage(url);

		TreePath path = topicToTocPath.get(topic);
		if (path != null) {
			toc.setSelectionPath(path);
			toc.scrollPathToVisible(path);
		}

		window.setVisible(true);
		window.toFront();
	} // end of showTopic method

	private static void showPage(String url) {

		URL resource = Help.class.getResource("/help/" + url);
		if (resource == null) {
			System.err.println("JLS help: page missing from this build: help/" + url);
			page.setContentType("text/html");
			page.setText("<html><body><h1>Help page not found</h1><p>The page <tt>help/"
					+ url + "</tt> is missing from this copy of JLS."
					+ " Please report this as a bug.</p></body></html>");
			return;
		}
		try {
			page.setPage(resource);
		}
		catch (IOException ex) {
			System.err.println("JLS help: cannot display help/" + url + ": " + ex);
		}
	} // end of showPage method

	/**
	 * Parse help/Map.jhm: lines of
	 * {@code <mapID target="..." url="..." />}.
	 */
	private static synchronized void loadTopicMap() {

		if (topicToUrl != null)
			return;
		topicToUrl = new HashMap<String,String>();
		String text = readResource("/help/Map.jhm");
		Matcher m = Pattern
				.compile("<mapID\\s+target=\"([^\"]+)\"\\s+url=\"([^\"]+)\"")
				.matcher(text);
		while (m.find()) {
			topicToUrl.put(m.group(1), m.group(2));
		}
	} // end of loadTopicMap method

	/**
	 * Parse help/JLSHelpTOC.xml into a tree. The format is nested
	 * {@code <tocitem text="..." [target="..."]>} elements.
	 */
	private static DefaultMutableTreeNode loadToc() {

		String text = readResource("/help/JLSHelpTOC.xml");
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(new TocEntry("JLS Help", null));
		DefaultMutableTreeNode current = root;
		Matcher m = Pattern.compile(
				"<tocitem\\s+text=\"([^\"]+)\"(?:\\s+target=\"([^\"]+)\")?\\s*(/?)>"
						+ "|</tocitem>").matcher(text);
		while (m.find()) {
			if (m.group().equals("</tocitem>")) {
				if (current != root)
					current = (DefaultMutableTreeNode)current.getParent();
				continue;
			}
			DefaultMutableTreeNode node =
					new DefaultMutableTreeNode(new TocEntry(m.group(1), m.group(2)));
			current.add(node);
			if (m.group(3).isEmpty()) {
				current = node; // open element: children follow
			}
		}
		return root;
	} // end of loadToc method

	private static String readResource(String path) {

		try (InputStream in = Help.class.getResourceAsStream(path)) {
			if (in == null)
				return "";
			return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
		}
		catch (IOException ex) {
			return "";
		}
	} // end of readResource method

	/** A TOC tree node: display text plus optional topic id. */
	private static final class TocEntry {

		final String text;
		final String topic;

		TocEntry(String text, String topic) {
			this.text = text;
			this.topic = topic;
		}

		public String toString() {
			return text;
		}
	}

	private static void buildWindow() {

		page = new JEditorPane();
		page.setEditable(false);
		page.addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent event) {
				if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED
						&& event.getURL() != null) {
					try {
						page.setPage(event.getURL());
					}
					catch (IOException ex) {
						System.err.println("JLS help: dead link: "
								+ event.getURL() + ": " + ex);
					}
				}
			}
		});

		DefaultMutableTreeNode root = loadToc();
		toc = new JTree(new DefaultTreeModel(root));
		toc.setRootVisible(false);
		toc.setShowsRootHandles(true);
		toc.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		toc.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent event) {
				TreePath path = toc.getSelectionPath();
				if (path == null)
					return;
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
				TocEntry entry = (TocEntry)node.getUserObject();
				if (entry.topic != null) {
					String url = topicToUrl.get(entry.topic);
					if (url != null)
						showPage(url);
				}
			}
		});
		for (int row = 0; row < toc.getRowCount(); row += 1) {
			toc.expandRow(row);
		}

		// remember where each topic lives in the tree so showTopic can select it
		topicToTocPath = new HashMap<String,TreePath>();
		java.util.Enumeration<?> all = root.depthFirstEnumeration();
		while (all.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)all.nextElement();
			TocEntry entry = (TocEntry)node.getUserObject();
			if (entry.topic != null && !topicToTocPath.containsKey(entry.topic)) {
				topicToTocPath.put(entry.topic, new TreePath(node.getPath()));
			}
		}

		JScrollPane tocScroll = new JScrollPane(toc);
		tocScroll.setMinimumSize(new Dimension(150, 100));
		JScrollPane pageScroll = new JScrollPane(page);
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tocScroll, pageScroll);
		split.setDividerLocation(220);

		window = new JFrame("JLS Help");
		window.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		window.getContentPane().setLayout(new BorderLayout());
		window.getContentPane().add(split, BorderLayout.CENTER);
		window.setSize(900, 650);
		window.setLocationRelativeTo(JLSInfo.frame);
	} // end of buildWindow method

} // end of Help class
