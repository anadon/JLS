package jls;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * The JavaHelp replacement (issue #11) is driven by two pieces of
 * metadata reused from the old system: help/Map.jhm (topic id -> HTML
 * file) and help/JLSHelpTOC.xml (the contents tree). These tests verify
 * the parsers and that every topic id referenced from code resolves to
 * an HTML page actually bundled on the classpath.
 *
 * Issue #70 extended this into a full link checker: every Map.jhm url
 * must exist as a bundled resource, every inline href/img src inside the
 * help HTML must resolve (case-sensitively, the way a jar resolves), and
 * every bundled help file must be reachable from some Map.jhm topic.
 * Comparison is against the exact resource names copied into the jar, so
 * a case mismatch (down.gif vs down.GIF) or a stale pre-fork path
 * (edu/mtu/cs/jls/images/...) fails here instead of failing silently for
 * users.
 */
class HelpTopicsTest {

	/** Matches href/src attributes, quoted or not, tolerating spaces
	 *  around '=' the way the HTML renderer does. */
	private static final Pattern LINK = Pattern.compile(
			"(?i)\\b(?:href|src)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>\"'][^\\s>]*))");

	/** The help tree root as bundled on the classpath (works whether the
	 *  classes are loose files or packed in a jar). */
	private static Path helpRoot() throws Exception {
		URL url = Help.class.getResource("/help/Map.jhm");
		assertNotNull(url, "help/Map.jhm missing from classpath");
		URI uri = url.toURI();
		if ("jar".equals(uri.getScheme())) {
			FileSystem fs;
			try {
				fs = FileSystems.getFileSystem(uri);
			}
			catch (FileSystemNotFoundException e) {
				fs = FileSystems.newFileSystem(uri, Map.of());
			}
			return fs.getPath("/help");
		}
		return Path.of(uri).getParent();
	}

	/** Every file under help/, as a relative path with '/' separators,
	 *  in the exact case the jar will contain. */
	private static Set<String> helpResources() throws Exception {
		Path root = helpRoot();
		Set<String> all = new TreeSet<String>();
		try (Stream<Path> walk = Files.walk(root)) {
			walk.filter(Files::isRegularFile)
					.forEach(p -> all.add(
							root.relativize(p).toString().replace('\\', '/')));
		}
		return all;
	}

	/** All href/src targets in a help page, raw (not yet resolved). */
	private static List<String> extractLinks(Path htmlFile) throws Exception {
		String text = new String(Files.readAllBytes(htmlFile),
				java.nio.charset.StandardCharsets.ISO_8859_1);
		List<String> links = new ArrayList<String>();
		Matcher m = LINK.matcher(text);
		while (m.find()) {
			String value = m.group(1) != null ? m.group(1)
					: m.group(2) != null ? m.group(2) : m.group(3);
			links.add(value);
		}
		return links;
	}

	/**
	 * Resolve a link the way JEditorPane does: relative to the page's
	 * directory. Returns the normalized help-relative path, or null if
	 * the link escapes the help tree (which is always a bug for bundled
	 * content). Fragment-only and absolute (scheme:) links return null
	 * via shouldCheck instead.
	 */
	private static String resolve(String pagePath, String link) {
		String target = link;
		int hash = target.indexOf('#');
		if (hash >= 0)
			target = target.substring(0, hash);
		String base = pagePath.contains("/")
				? pagePath.substring(0, pagePath.lastIndexOf('/')) : "";
		Deque<String> parts = new ArrayDeque<String>();
		if (!base.isEmpty())
			for (String p : base.split("/"))
				parts.addLast(p);
		for (String p : target.split("/")) {
			if (p.isEmpty() || p.equals("."))
				continue;
			if (p.equals("..")) {
				if (parts.isEmpty())
					return null; // escapes help/ (e.g. a pre-fork image path)
				parts.removeLast();
			}
			else {
				parts.addLast(p);
			}
		}
		return String.join("/", parts);
	}

	/** Fragment-only links and scheme-absolute links are out of scope. */
	private static boolean shouldCheck(String link) {
		if (link.isEmpty() || link.startsWith("#"))
			return false;
		return !link.matches("[a-zA-Z][a-zA-Z0-9+.-]*:.*");
	}

	/**
	 * Every element type a user can create from the editor palette
	 * (SimpleEditor.makeElements builds the toolbar and the "elements"
	 * menu from exactly these, plus the Import button for SubCircuit),
	 * mapped to the Map.jhm topic that documents it. This is the
	 * completeness contract of issue #85: every palette element has a
	 * help topic that resolves to a bundled page. A static list because
	 * the element registry (#78) does not exist yet; when adding a
	 * palette element, add its row here (the test fails until the topic
	 * and page exist).
	 *
	 * Inventory note (issue #85): at the time this test was added the
	 * audit's "21 element classes without help pages" was re-counted
	 * against the real palette; every user-creatable element already
	 * had a mapped page (the uncovered classes are internal:
	 * puts/wires/dialog plumbing/truth-table display internals), so
	 * this test pins completeness rather than repairing it.
	 */
	private static final Map<String,String> PALETTE_ELEMENT_TOPICS = Map.ofEntries(
			Map.entry("AndGate", "AND"),
			Map.entry("OrGate", "OR"),
			Map.entry("NotGate", "NOT"),
			Map.entry("XorGate", "XOR"),
			Map.entry("NandGate", "NAND"),
			Map.entry("NorGate", "NOR"),
			Map.entry("DelayGate", "DELAY"),
			Map.entry("TriState", "TRISTATE"),
			Map.entry("JumpStart", "start"),
			Map.entry("JumpEnd", "end"),
			Map.entry("InputPin", "Input"),
			Map.entry("OutputPin", "Output"),
			Map.entry("Splitter", "unbundle"),
			Map.entry("Binder", "bundle"),
			Map.entry("Constant", "const"),
			Map.entry("Extend", "extend"),
			Map.entry("Register", "register"),
			Map.entry("Memory", "memory"),
			Map.entry("Mux", "mux"),
			Map.entry("ShiftRegister", "shiftregister"),
			Map.entry("Decoder", "decoder"),
			Map.entry("Adder", "adder"),
			Map.entry("Clock", "clock"),
			Map.entry("Pause", "pause"),
			Map.entry("Stop", "stop"),
			Map.entry("SigGen", "siggen"),
			Map.entry("Display", "display"),
			Map.entry("StateMachine", "stmach"),
			Map.entry("TruthTable", "truth"),
			Map.entry("Text", "text"),
			Map.entry("SubCircuit", "import"));

	/** Every palette element type must have a help topic that resolves
	 *  to a bundled page (issue #85). Also sanity-checks the static
	 *  list itself: each type must be a real jls.elem class, so a
	 *  renamed element cannot leave a stale row behind. */
	@Test
	void everyPaletteElementTypeHasAMappedHelpTopic() throws Exception {
		Map<String,String> map = topicMap();
		Set<String> resources = helpResources();
		List<String> broken = new ArrayList<String>();
		for (String type : new TreeSet<String>(PALETTE_ELEMENT_TOPICS.keySet())) {
			String topic = PALETTE_ELEMENT_TOPICS.get(type);
			try {
				Class.forName("jls.elem." + type);
			}
			catch (ClassNotFoundException ex) {
				broken.add(type + ": no such element class jls.elem." + type);
				continue;
			}
			String url = map.get(topic);
			if (url == null) {
				broken.add(type + ": Map.jhm has no entry for topic '" + topic + "'");
			}
			else if (!resources.contains(url)) {
				broken.add(type + ": topic '" + topic
						+ "' points at a missing page help/" + url);
			}
		}
		assertTrue(broken.isEmpty(),
				"palette elements without a working help topic:\n  "
						+ String.join("\n  ", broken));
	}

	/** Every topic id passed to Help.enableHelpOnButton/showTopic in the source. */
	private static final List<String> TOPICS_USED_BY_CODE = List.of(
			"top", "adder", "bundle", "unbundle", "clock", "const",
			"decoder", "display", "end", "import", "inter.sim", "memory",
			"mux", "register", "shiftregister", "siggen", "start",
			"stmach", "text", "truth", "DELAY", "TRISTATE",
			// dynamic: Gate dialogs pass their type string
			"AND", "OR", "NAND", "NOR", "NOT", "XOR",
			// dynamic: Pin dialogs pass "Input"/"Output"
			"Input", "Output");

	@SuppressWarnings("unchecked")
	private static Map<String,String> topicMap() throws Exception {
		Method load = Help.class.getDeclaredMethod("loadTopicMap");
		load.setAccessible(true);
		load.invoke(null);
		java.lang.reflect.Field field = Help.class.getDeclaredField("topicToUrl");
		field.setAccessible(true);
		return (Map<String,String>) field.get(null);
	}

	@Test
	void everyCodeReferencedTopicResolvesToABundledPage() throws Exception {
		Map<String,String> map = topicMap();
		for (String topic : TOPICS_USED_BY_CODE) {
			String url = map.get(topic);
			assertNotNull(url, "Map.jhm has no entry for topic '" + topic + "'");
			assertNotNull(Help.class.getResource("/help/" + url),
					"help page missing from classpath: " + url + " (topic " + topic + ")");
		}
	}

	@Test
	void tocParsesAndItsTopicsResolve() throws Exception {
		Map<String,String> map = topicMap();
		Method load = Help.class.getDeclaredMethod("loadToc");
		load.setAccessible(true);
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) load.invoke(null);
		assertTrue(root.getChildCount() > 0, "TOC parsed to an empty tree");

		int entriesWithTopic = 0;
		java.util.Enumeration<?> all = root.depthFirstEnumeration();
		while (all.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) all.nextElement();
			Object entry = node.getUserObject();
			java.lang.reflect.Field topicField = entry.getClass().getDeclaredField("topic");
			topicField.setAccessible(true);
			String topic = (String) topicField.get(entry);
			if (topic == null)
				continue;
			entriesWithTopic++;
			assertNotNull(map.get(topic),
					"TOC references topic '" + topic + "' missing from Map.jhm");
		}
		assertTrue(entriesWithTopic > 20,
				"expected a substantial TOC, got " + entriesWithTopic + " linked entries");
	}

	@Test
	void unknownTopicFallsBackToOverview() throws Exception {
		Map<String,String> map = topicMap();
		assertFalse(map.containsKey("no-such-topic"));
		assertNotNull(map.get("top"), "the overview topic must exist for the fallback");
	}

	/** Every Map.jhm url must name a bundled file, exact case (#70:
	 *  three topics used to point at files that were never shipped). */
	@Test
	void everyMapTargetResolvesToABundledFile() throws Exception {
		Map<String,String> map = topicMap();
		Set<String> resources = helpResources();
		List<String> broken = new ArrayList<String>();
		for (Map.Entry<String,String> e : map.entrySet()) {
			if (!resources.contains(e.getValue()))
				broken.add("topic '" + e.getKey() + "' -> help/" + e.getValue());
		}
		assertTrue(broken.isEmpty(),
				"Map.jhm targets pointing at nonexistent files:\n  "
						+ String.join("\n  ", broken));
	}

	/** Every inline href and img src in every help page must resolve to
	 *  a bundled resource, case-sensitively, resolved against the page's
	 *  own path the way JEditorPane resolves it (#70: pre-fork image
	 *  paths, .gif/.GIF case mismatches, typo'd and missing-suffix
	 *  links all shipped broken for years). */
	@Test
	void everyInlineLinkAndImageResolves() throws Exception {
		Path root = helpRoot();
		Set<String> resources = helpResources();
		List<String> broken = new ArrayList<String>();
		for (String page : resources) {
			if (!page.endsWith(".html"))
				continue;
			for (String link : extractLinks(root.resolve(page))) {
				if (!shouldCheck(link))
					continue;
				String resolved = resolve(page, link);
				if (resolved == null || !resources.contains(resolved))
					broken.add("help/" + page + " -> " + link);
			}
		}
		assertTrue(broken.isEmpty(),
				"broken links/images inside help pages:\n  "
						+ String.join("\n  ", broken));
	}

	/** Every bundled help file must be reachable from some Map.jhm topic
	 *  by following inline links (#70: orphan pages shipped in the jar
	 *  that no topic and no page ever led to). */
	@Test
	void everyHelpFileIsReachableFromTheTopicMap() throws Exception {
		Path root = helpRoot();
		Set<String> resources = helpResources();

		Set<String> visited = new HashSet<String>();
		Deque<String> queue = new ArrayDeque<String>(topicMap().values());
		while (!queue.isEmpty()) {
			String page = queue.removeFirst();
			if (!resources.contains(page) || !visited.add(page))
				continue;
			if (!page.endsWith(".html"))
				continue;
			for (String link : extractLinks(root.resolve(page))) {
				if (!shouldCheck(link))
					continue;
				String resolved = resolve(page, link);
				if (resolved != null)
					queue.addLast(resolved);
			}
		}

		Set<String> orphans = new TreeSet<String>(resources);
		orphans.removeAll(visited);
		orphans.remove("Map.jhm");        // the metadata itself
		orphans.remove("JLSHelpTOC.xml"); // is not help content
		assertTrue(orphans.isEmpty(),
				"help files shipped in the jar but unreachable from any topic:\n  "
						+ String.join("\n  ", orphans));
	}
}
