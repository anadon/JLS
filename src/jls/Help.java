package jls;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

import org.jspecify.annotations.Nullable;

/**
 * The in-app help system (issue #11): a plain Swing viewer over the
 * bundled HTML help content, replacing the abandoned JavaHelp library.
 *
 * The existing help metadata is reused as-is: help/Map.jhm maps topic
 * ids to HTML files, and help/JLSHelpTOC.xml provides the table of
 * contents shown as a tree. Both are simple enough to parse directly.
 */
public final class Help {

	/** Topic id to help page name, parsed from help/Map.jhm on first use. */
	private static @Nullable Map<String,String> topicToUrl = null;
	/** The single help window, built lazily on first use. */
	private static @Nullable JFrame window = null;
	/** The HTML viewer pane showing the current help page. */
	private static @Nullable JEditorPane page = null;
	/** The table-of-contents tree shown beside the page viewer. */
	private static @Nullable JTree toc = null;
	/** Topic id to its path in the contents tree, for selection sync. */
	private static @Nullable Map<String,TreePath> topicToTocPath = null;

	/** No instances: this is a static utility. */
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

		Map<String,String> map = Objects.requireNonNull(topicToUrl);
		String url = map.get(topic);
		if (url == null)
			url = map.getOrDefault("top", "overview.html");
		showPage(url);

		JTree tocTree = Objects.requireNonNull(toc);
		TreePath path = Objects.requireNonNull(topicToTocPath).get(topic);
		if (path != null) {
			tocTree.setSelectionPath(path);
			tocTree.scrollPathToVisible(path);
		}

		JFrame win = Objects.requireNonNull(window);
		win.setVisible(true);
		win.toFront();
	} // end of showTopic method

	/**
	 * Display a help page in the viewer, or an inline "not found" notice
	 * if the resource is missing from this build.
	 *
	 * @param url The help-relative page name (a Map.jhm url value).
	 */
	private static void showPage(String url) {

		JEditorPane viewer = Objects.requireNonNull(page);
		URL resource = Help.class.getResource("/help/" + url);
		if (resource == null) {
			System.err.println("JLS help: page missing from this build: help/" + url);
			viewer.setContentType("text/html");
			viewer.setText("<html><body><h1>Help page not found</h1><p>The page <tt>help/"
					+ url + "</tt> is missing from this copy of JLS."
					+ " Please report this as a bug.</p></body></html>");
			return;
		}
		try {
			viewer.setPage(resource);
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
		Map<String,String> map = new HashMap<String,String>();
		topicToUrl = map;
		String text = readResource("/help/Map.jhm");
		Matcher m = Pattern
				.compile("<mapID\\s+target=\"([^\"]+)\"\\s+url=\"([^\"]+)\"")
				.matcher(text);
		while (m.find()) {
			map.put(m.group(1), m.group(2));
		}
	} // end of loadTopicMap method

	/**
	 * Parse help/JLSHelpTOC.xml into a tree. The format is nested
	 * {@code <tocitem text="..." [target="..."]>} elements.
	 *
	 * @return The root node of the contents tree.
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

	/**
	 * Read a bundled classpath resource as ISO-8859-1 text.
	 *
	 * @param path The absolute classpath resource path.
	 * @return The resource contents, or "" if absent or unreadable.
	 */
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

	/**
	 * A TOC tree node: display text plus optional topic id.
	 *
	 * @param text  The label shown in the contents tree.
	 * @param topic The topic id to open when selected, or null.
	 */
	private record TocEntry(String text, @Nullable String topic) {

		/** The display text, so the tree renders the label directly. */
		@Override
		public String toString() {
			return text;
		}
	}

	/** Build the help window: the contents tree, the page viewer, and the
	 *  topic-to-tree-path index, all lazily on first use. */
	private static void buildWindow() {

		final JEditorPane viewer = new JEditorPane();
		page = viewer;
		viewer.setEditable(false);
		viewer.addHyperlinkListener(new HyperlinkListener() {
			/** Follow an activated hyperlink to its target page. */
			@Override
			public void hyperlinkUpdate(HyperlinkEvent event) {
				if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED
						&& event.getURL() != null) {
					try {
						viewer.setPage(event.getURL());
					}
					catch (IOException ex) {
						System.err.println("JLS help: dead link: "
								+ event.getURL() + ": " + ex);
					}
				}
			}
		});

		DefaultMutableTreeNode root = loadToc();
		final JTree tree = new JTree(new DefaultTreeModel(root));
		toc = tree;
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			/** Show the page for the newly selected contents-tree node. */
			@Override
			public void valueChanged(TreeSelectionEvent event) {
				TreePath path = tree.getSelectionPath();
				if (path == null)
					return;
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
				TocEntry entry = (TocEntry)node.getUserObject();
				String topic = entry.topic();
				Map<String,String> map = topicToUrl;
				if (topic != null && map != null) {
					String url = map.get(topic);
					if (url != null)
						showPage(url);
				}
			}
		});
		for (int row = 0; row < tree.getRowCount(); row += 1) {
			tree.expandRow(row);
		}

		// remember where each topic lives in the tree so showTopic can select it
		Map<String,TreePath> tocPaths = new HashMap<String,TreePath>();
		topicToTocPath = tocPaths;
		java.util.Enumeration<?> all = root.depthFirstEnumeration();
		while (all.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)all.nextElement();
			TocEntry entry = (TocEntry)node.getUserObject();
			String topic = entry.topic();
			if (topic != null && !tocPaths.containsKey(topic)) {
				tocPaths.put(topic, new TreePath(node.getPath()));
			}
		}

		JScrollPane tocScroll = new JScrollPane(tree);
		tocScroll.setMinimumSize(new Dimension(150, 100));
		JScrollPane pageScroll = new JScrollPane(viewer);
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tocScroll, pageScroll);
		split.setDividerLocation(220);

		final JFrame win = new JFrame("JLS Help");
		window = win;
		win.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		win.getContentPane().setLayout(new BorderLayout());
		win.getContentPane().add(split, BorderLayout.CENTER);
		win.setSize(900, 650);
		win.setLocationRelativeTo(JLSInfo.frame);
	} // end of buildWindow method

} // end of Help class
