package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * The network-surface confinement ratchet for issue #168 (prediction
 * P3, in the {@code NotificationRatchetTest} idiom): socket and
 * channel construction may only ever appear under {@code
 * jls.collab.net}, so the network-facing surface cannot quietly
 * spread through the codebase - the property SECURITY.md's collab
 * threat model rests on. The Stage 1a crypto core is
 * transport-agnostic; the socket slice ({@code SessionListener},
 * {@code SocketSession}) now lives under that one allowed directory
 * and nowhere else, so the network surface stays confined to the
 * package the architecture reserves for it.
 *
 * A second ratchet pins research doc section 6.1's hardest rule:
 * never Java object serialization anywhere under {@code jls.collab}
 * - frames and stores are fixed schemas parsed with hostile-input
 * discipline, and an {@code ObjectInputStream} reachable from network
 * bytes would be a remote-code-execution primitive.
 */
class SocketConfinementRatchetTest {

	/**
	 * Source substrings that construct or open a network endpoint.
	 * Mentioning a type is fine (comments, docs); constructing one
	 * is what this ratchet pins.
	 */
	private static final List<String> SOCKET_CONSTRUCTION = List.of(
			"new Socket(", "new ServerSocket(",
			"new DatagramSocket(", "new MulticastSocket(",
			"SocketChannel.open", "ServerSocketChannel.open",
			"DatagramChannel.open",
			"AsynchronousSocketChannel.open",
			"AsynchronousServerSocketChannel.open");

	/**
	 * The only directory whose files may ever construct sockets. The
	 * socket transport lives here and only here; everywhere else this
	 * ratchet proves the repository-wide absence that batch mode and
	 * default GUI start rely on - no code path outside this package can
	 * open a listener, because no socket code exists outside it.
	 */
	private static final String ALLOWED_PREFIX = "src/jls/collab/net/";

	/** Java-serialization source substrings forbidden under collab. */
	private static final List<String> OBJECT_SERIALIZATION = List.of(
			"ObjectInputStream", "ObjectOutputStream",
			"implements Serializable",
			"implements java.io.Serializable");

	@Test
	void socketConstructionStaysInsideCollabNet() throws IOException {

		Set<String> offenders = new TreeSet<String>();
		forEachSource(Path.of("src"), (file, text) -> {
			if (file.startsWith(ALLOWED_PREFIX)) {
				return;
			}
			for (String pattern : SOCKET_CONSTRUCTION) {
				if (text.contains(pattern)) {
					offenders.add(file + " (" + pattern + ")");
				}
			}
		});
		assertEquals(Set.of(), offenders,
				"socket construction outside jls.collab.net; the"
						+ " network surface must stay confined"
						+ " (issue #168)");
	}

	@Test
	void collabNeverTouchesJavaObjectSerialization()
			throws IOException {

		Set<String> offenders = new TreeSet<String>();
		forEachSource(Path.of("src", "jls", "collab"),
				(file, text) -> {
					for (String pattern : OBJECT_SERIALIZATION) {
						if (text.contains(pattern)) {
							offenders.add(file + " (" + pattern + ")");
						}
					}
				});
		assertEquals(Set.of(), offenders,
				"Java object serialization has no place in the collab"
						+ " protocol (research doc section 6.1)");
	}

	/** A consumer of one source file's repo-relative path and text. */
	private interface SourceCheck {

		/**
		 * Inspect one source file.
		 *
		 * @param file The repo-relative path, forward slashes.
		 * @param text The file's full text.
		 */
		void inspect(String file, String text);

	} // end of SourceCheck interface

	/**
	 * Walk a source tree, feeding every Java file to a check.
	 *
	 * @param root The tree to walk.
	 * @param check The check to run on each file.
	 *
	 * @throws IOException if the tree cannot be walked.
	 */
	private static void forEachSource(Path root, SourceCheck check)
			throws IOException {

		assertTrue(Files.isDirectory(root),
				"source tree not found at " + root
						+ " (run tests from the repository root)");
		Path base = Path.of(System.getProperty("user.dir"));
		try (Stream<Path> files = Files.walk(root)) {
			files.filter(p -> p.toString().endsWith(".java"))
					.forEach(p -> {
						String text;
						try {
							text = new String(Files.readAllBytes(p),
									StandardCharsets.UTF_8);
						} catch (IOException unreadable) {
							throw new java.io.UncheckedIOException(
									unreadable);
						}
						check.inspect(base.relativize(
								p.toAbsolutePath().normalize())
								.toString().replace('\\', '/'), text);
					});
		}
	} // end of forEachSource method

} // end of SocketConfinementRatchetTest class
