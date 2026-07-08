package jls;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * The JavaHelp replacement (issue #11) is driven by two pieces of
 * metadata reused from the old system: help/Map.jhm (topic id -> HTML
 * file) and help/JLSHelpTOC.xml (the contents tree). These tests verify
 * the parsers and that every topic id referenced from code resolves to
 * an HTML page actually bundled on the classpath.
 */
class HelpTopicsTest {

	/** Every topic id passed to Help.enableHelpOnButton/showTopic in the source. */
	private static final List<String> TOPICS_USED_BY_CODE = List.of(
			"top", "adder", "bundle", "unbundle", "clock", "const",
			"decoder", "display", "end", "import", "inter.sim", "memory",
			"mux", "register", "siggen", "start", "stmach", "text",
			"truth", "DELAY", "TRISTATE",
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
}
