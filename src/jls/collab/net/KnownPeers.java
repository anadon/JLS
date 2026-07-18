package jls.collab.net;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * The persisted trust decisions of this install (issue #168, research
 * doc section 5.2 step 4): which peer keys the user has SAS-verified,
 * and the display name each was verified under. Reconnecting to a
 * fingerprint recorded here skips the verify dialog; an unknown
 * fingerprint arriving under a display name that is already bound to a
 * <em>different</em> fingerprint is the key-change case, which the UI
 * must surface as a loud warning, never silently.
 *
 * The store is a small text file ({@code jls/known-peers} under the
 * XDG config base). Fingerprints are identity; names are labels. A
 * malformed store is an error, not a repair - trust data must never be
 * guessed at.
 */
public final class KnownPeers {

	/** The header line naming the format. */
	private static final String HEADER = "JLS known peers v1";

	/** Hostile-input cap: the most peers one store may list. */
	private static final int MAX_PEERS = 10_000;

	/** What this install knows about a peer key and name pair. */
	public enum Trust {
		/** The fingerprint was SAS-verified earlier; skip the dialog. */
		VERIFIED,
		/** Never seen; run the SAS verify flow. */
		UNKNOWN,
		/**
		 * The name is bound to a different key - a possible
		 * man-in-the-middle or a reinstalled peer. Loud warning.
		 */
		KEY_CHANGED
	} // end of Trust enum

	/** One verified peer: its name and when it was verified. */
	private static final class Record {

		/** The display name the peer was verified under. */
		private final String name;

		/** When the verification happened, epoch milliseconds. */
		private final long verifiedAt;

		/**
		 * Bind a verified peer's facts.
		 *
		 * @param name The display name.
		 * @param verifiedAt The verification time, epoch millis.
		 */
		Record(String name, long verifiedAt) {

			this.name = name;
			this.verifiedAt = verifiedAt;
		} // end of constructor

	} // end of Record class

	/** The store file this instance reads and writes. */
	private final Path file;

	/** Verified peers by fingerprint, in stable order. */
	private final TreeMap<String, Record> peers =
			new TreeMap<String, Record>();

	/**
	 * Bind a store to its file. Private: callers arrive through
	 * {@link #load(Path)}.
	 *
	 * @param file The store file.
	 */
	private KnownPeers(Path file) {

		this.file = file;
	} // end of constructor

	/**
	 * Where this install persists its verified peers: {@code
	 * jls/known-peers} under the XDG config base, next to the identity
	 * key.
	 *
	 * @return the per-install known-peers file path.
	 */
	public static Path defaultFile() {

		String xdg = System.getenv("XDG_CONFIG_HOME");
		Path base = xdg == null || xdg.isEmpty()
				? Path.of(System.getProperty("user.home"), ".config")
				: Path.of(xdg);
		return base.resolve("jls").resolve("known-peers");
	} // end of defaultFile method

	/**
	 * Load the store at the given path; a missing file is an empty
	 * store (nothing verified yet).
	 *
	 * @param file The store file (normally {@link #defaultFile()}).
	 *
	 * @return the store.
	 *
	 * @throws IOException if the file exists but is malformed, over
	 *             its caps, or unreadable.
	 */
	public static KnownPeers load(Path file) throws IOException {

		KnownPeers store = new KnownPeers(file);
		if (!Files.exists(file)) {
			return store;
		}
		var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
		if (lines.isEmpty() || !lines.get(0).equals(HEADER)) {
			throw new IOException("known-peers file " + file
					+ " does not begin with '" + HEADER + "'");
		}
		for (int i = 1; i < lines.size(); i += 1) {
			String line = lines.get(i);
			if (line.isBlank()) {
				continue;
			}
			if (store.peers.size() >= MAX_PEERS) {
				throw new IOException("known-peers file " + file
						+ " lists more than " + MAX_PEERS + " peers");
			}
			store.addLine(line, file, i + 1);
		}
		return store;
	} // end of load method

	/**
	 * Classify a peer presenting a fingerprint and display name.
	 *
	 * @param fingerprint The peer's key fingerprint.
	 * @param displayName The display name the peer claims.
	 *
	 * @return the trust decision for the UI layer.
	 */
	public Trust check(String fingerprint, String displayName) {

		if (peers.containsKey(fingerprint)) {
			return Trust.VERIFIED;
		}
		for (Map.Entry<String, Record> entry : peers.entrySet()) {
			if (entry.getValue().name.equals(displayName)) {
				return Trust.KEY_CHANGED;
			}
		}
		return Trust.UNKNOWN;
	} // end of check method

	/**
	 * Record a peer the user just SAS-verified, replacing any earlier
	 * record for the same fingerprint, and persist the store.
	 *
	 * @param fingerprint The verified key fingerprint (64 lowercase
	 *            hex characters).
	 * @param displayName The display name verified with it.
	 * @param verifiedAt When the verification happened, epoch millis.
	 *
	 * @throws IOException if persisting fails.
	 * @throws IllegalArgumentException if the fingerprint or name is
	 *             malformed, or the store is full.
	 */
	public void recordVerified(String fingerprint, String displayName,
			long verifiedAt) throws IOException {

		if (fingerprint == null
				|| !fingerprint.matches("[0-9a-f]{64}")) {
			throw new IllegalArgumentException("a fingerprint is 64"
					+ " lowercase hex characters");
		}
		IdentityKey.validateDisplayName(displayName);
		if (peers.size() >= MAX_PEERS
				&& !peers.containsKey(fingerprint)) {
			throw new IllegalArgumentException("the known-peers store"
					+ " is full (" + MAX_PEERS + " peers)");
		}
		peers.put(fingerprint, new Record(displayName, verifiedAt));
		store();
	} // end of recordVerified method

	/**
	 * The display name a verified fingerprint was recorded under.
	 *
	 * @param fingerprint The key fingerprint.
	 *
	 * @return the name, or null if the fingerprint is not verified.
	 */
	public String nameOf(String fingerprint) {

		Record record = peers.get(fingerprint);
		return record == null ? null : record.name;
	} // end of nameOf method

	/**
	 * The verified fingerprints, in stable sorted order.
	 *
	 * @return an unmodifiable view of the fingerprints.
	 */
	public java.util.Set<String> fingerprints() {

		return Collections.unmodifiableSet(peers.keySet());
	} // end of fingerprints method

	/**
	 * Persist the store to its file.
	 *
	 * @throws IOException if writing fails.
	 */
	private void store() throws IOException {

		StringBuilder text = new StringBuilder();
		text.append(HEADER).append('\n');
		for (Map.Entry<String, Record> entry : peers.entrySet()) {
			Record record = entry.getValue();
			text.append(entry.getKey()).append(' ')
					.append(record.verifiedAt).append(" \"")
					.append(escape(record.name)).append("\"\n");
		}
		if (file.getParent() != null) {
			Files.createDirectories(file.getParent());
		}
		Files.writeString(file, text.toString(),
				StandardCharsets.UTF_8);
	} // end of store method

	/**
	 * Parse one store line: fingerprint, verification time, quoted
	 * escaped name.
	 *
	 * @param line The line text.
	 * @param source The file, for the error.
	 * @param lineNumber The 1-based line number, for the error.
	 *
	 * @throws IOException if the line is malformed.
	 */
	private void addLine(String line, Path source, int lineNumber)
			throws IOException {

		String[] head = line.split(" ", 3);
		if (head.length != 3 || !head[0].matches("[0-9a-f]{64}")) {
			throw new IOException("known-peers file " + source
					+ " line " + lineNumber
					+ " is not 'fingerprint time \"name\"'");
		}
		long verifiedAt;
		try {
			verifiedAt = Long.parseLong(head[1]);
		} catch (NumberFormatException bad) {
			throw new IOException("known-peers file " + source
					+ " line " + lineNumber + " has a non-numeric"
					+ " verification time");
		}
		String quoted = head[2];
		if (quoted.length() < 2 || quoted.charAt(0) != '"'
				|| quoted.charAt(quoted.length() - 1) != '"') {
			throw new IOException("known-peers file " + source
					+ " line " + lineNumber
					+ " does not end with a quoted name");
		}
		String name = unescape(
				quoted.substring(1, quoted.length() - 1));
		try {
			IdentityKey.validateDisplayName(name);
		} catch (IllegalArgumentException bad) {
			throw new IOException("known-peers file " + source
					+ " line " + lineNumber + " holds an invalid"
					+ " name: " + bad.getMessage());
		}
		if (peers.containsKey(head[0])) {
			throw new IOException("known-peers file " + source
					+ " line " + lineNumber + " repeats a fingerprint");
		}
		peers.put(head[0], new Record(name, verifiedAt));
	} // end of addLine method

	/**
	 * Escape a display name for the quoted store form (names cannot
	 * contain control characters, so backslash and quote are the only
	 * escapes needed).
	 *
	 * @param name The validated name.
	 *
	 * @return the escaped form.
	 */
	private static String escape(String name) {

		return name.replace("\\", "\\\\").replace("\"", "\\\"");
	} // end of escape method

	/**
	 * Invert {@link #escape(String)} with a single left-to-right scan
	 * (the #53 idiom).
	 *
	 * @param escaped The escaped form.
	 *
	 * @return the name.
	 */
	private static String unescape(String escaped) {

		StringBuilder name = new StringBuilder(escaped.length());
		for (int i = 0; i < escaped.length(); i += 1) {
			char ch = escaped.charAt(i);
			if (ch == '\\' && i + 1 < escaped.length()) {
				name.append(escaped.charAt(i + 1));
				i += 1;
			} else {
				name.append(ch);
			}
		}
		return name.toString();
	} // end of unescape method

} // end of KnownPeers class
