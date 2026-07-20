package jls.elem;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The permanent identity of an element (issue #165, collab Stage 0a):
 * a replica id plus a creation counter. Unlike the save-time {@code int
 * id} - a file-local reference index reassigned on every save - a stable
 * id is minted once, when the element comes into existence, and then
 * means the same element across save/load, undo restore, and checkpoint
 * recovery. Replication and per-op addressing (issue #163) build on it.
 *
 * Two minting paths exist:
 *
 * <ul>
 * <li>{@link #mintFresh()} - for elements created in this process
 * (editor placement, paste, programmatic construction): this install's
 * replica id plus a process-wide counter. The replica id persists in
 * the per-install config file and can be pinned with the
 * {@code jls.replicaId} system property or {@code JLS_REPLICA_ID}
 * environment variable (issue #183), so from-scratch saves are
 * reproducible per install and byte-pinnable in CI. When per-install
 * identity keys land (issue #168), the replica id becomes the install's
 * identity.</li>
 * <li>{@link #legacy(long)} - for elements read from files that predate
 * stable ids: the reserved replica id {@code legacy} plus the element's
 * position in file order. File order is deterministic (issue #98), so
 * two loads of the same legacy file mint identical ids - the property
 * the deterministic-serialization work (issue #166) rests on.</li>
 * </ul>
 *
 * The string form is {@code replica:counter}; the replica id is 1-64
 * characters of {@code [0-9a-z]}, so the whole id survives the save
 * format's string escaping untouched.
 */
public final class ElementId implements Comparable<ElementId> {

	/** The reserved replica id for ids minted from pre-#165 files. */
	private static final String LEGACY_REPLICA = "legacy";

	/**
	 * This install's replica id (issue #183). Resolved once, at class
	 * load, by {@link #resolveReplica(String, String, java.nio.file.Path)}:
	 * an explicit override (the {@code jls.replicaId} system property or
	 * {@code JLS_REPLICA_ID} environment variable) wins, then a value
	 * persisted in the per-install config file, then - only when neither
	 * exists - a fresh random draw (32 hex digits from a UUID, which is
	 * SecureRandom-backed; hex cannot collide with the reserved "legacy"
	 * replica, which has letters past 'f'). Persisting the draw makes
	 * from-scratch saves reproducible run-to-run on one install; the
	 * override makes them byte-pinnable in CI and reproducible exports.
	 * Mutable only through {@link #pinForTesting(String, long)}.
	 */
	private static volatile String processReplica = resolveReplica(
			System.getProperty("jls.replicaId"),
			System.getenv("JLS_REPLICA_ID"),
			defaultReplicaFile());

	/** The process-wide creation counter for {@link #mintFresh()}. */
	private static final AtomicLong NEXT_COUNTER = new AtomicLong();

	/**
	 * Where this install persists its replica id: {@code
	 * jls/replica-id} under the XDG config base ({@code $XDG_CONFIG_HOME},
	 * or {@code ~/.config} when unset - the same convention every
	 * XDG-aware Linux tool follows, and a harmless dot-directory
	 * elsewhere).
	 *
	 * @return the per-install replica id file path.
	 */
	private static java.nio.file.Path defaultReplicaFile() {

		String xdg = System.getenv("XDG_CONFIG_HOME");
		java.nio.file.Path base = xdg == null || xdg.isEmpty()
				? java.nio.file.Path.of(
						System.getProperty("user.home"), ".config")
				: java.nio.file.Path.of(xdg);
		return base.resolve("jls").resolve("replica-id");
	} // end of defaultReplicaFile method

	/**
	 * Resolve the replica id (issue #183): explicit override first
	 * (system property, then environment), then the persisted
	 * per-install value, then a fresh draw - which is persisted for the
	 * next start, so one install keeps one replica across runs. Invalid
	 * candidates (not 1-64 characters of {@code [0-9a-z]}, or equal to
	 * the reserved {@code legacy} replica) are skipped rather than
	 * fatal: identity minting must never prevent startup. Unreadable or
	 * unwritable config likewise degrades to a per-process draw - the
	 * pre-#183 behavior.
	 *
	 * @param override The {@code jls.replicaId} system property, or null.
	 * @param env The {@code JLS_REPLICA_ID} environment variable, or null.
	 * @param persisted The per-install replica id file.
	 *
	 * @return the resolved replica id.
	 *
	 * @jls.testedby jls.elem.ElementIdReplicaTest#overrideWinsOverEverything()
	 * @jls.testedby jls.elem.ElementIdReplicaTest#environmentBeatsPersistedConfig()
	 * @jls.testedby jls.elem.ElementIdReplicaTest#persistedReplicaIsReadBack()
	 * @jls.testedby jls.elem.ElementIdReplicaTest#freshDrawIsPersistedAndReused()
	 * @jls.testedby jls.elem.ElementIdReplicaTest#invalidCandidatesAreSkipped()
	 */
	static String resolveReplica(String override, String env,
			java.nio.file.Path persisted) {

		if (isValidReplica(override)) {
			return override;
		}
		if (isValidReplica(env)) {
			return env;
		}
		try {
			if (java.nio.file.Files.isRegularFile(persisted)) {
				String stored = java.nio.file.Files.readString(persisted,
						java.nio.charset.StandardCharsets.UTF_8).trim();
				if (isValidReplica(stored)) {
					return stored;
				}
			}
		} catch (java.io.IOException unreadable) {
			// fall through to a fresh draw
		}
		String fresh =
				java.util.UUID.randomUUID().toString().replace("-", "");
		try {
			java.nio.file.Files.createDirectories(persisted.getParent());
			java.nio.file.Files.writeString(persisted, fresh + "\n",
					java.nio.charset.StandardCharsets.UTF_8);
		} catch (java.io.IOException unwritable) {
			// the draw still serves this process; the next start redraws
		}
		return fresh;
	} // end of resolveReplica method

	/**
	 * Whether a candidate replica id is usable: 1-64 characters of
	 * {@code [0-9a-z]} (the {@link #parse(String)} grammar) and not the
	 * reserved {@code legacy} replica, which would collide fresh mints
	 * with ids minted deterministically from pre-#165 files.
	 *
	 * @param candidate The candidate replica id, or null.
	 *
	 * @return true if the candidate can serve as the replica id.
	 */
	private static boolean isValidReplica(String candidate) {

		return candidate != null && !candidate.equals(LEGACY_REPLICA)
				&& candidate.matches("[0-9a-z]{1,64}");
	} // end of isValidReplica method

	/**
	 * Pin the replica id and creation counter to fixed values for a
	 * test, restoring them on close. This reproduces, inside one JVM,
	 * what two processes started with the same {@code jls.replicaId}
	 * override observe: each starts its counter at the same point, so
	 * identical construction sequences mint identical ids and
	 * from-scratch saves become byte-identical (issue #183). On close
	 * the counter never moves backwards past its pre-pin value, so ids
	 * minted after the pin cannot collide with ids minted before it.
	 *
	 * @param replica The replica id to pin.
	 * @param counter The next counter value to mint from.
	 *
	 * @return a handle that restores the previous replica and counter.
	 *
	 * @jls.testedby jls.elem.ElementIdReplicaTest#pinnedConstructionMintsReproducibleIds()
	 * @jls.testedby jls.elem.ElementIdReplicaTest#pinnedFromScratchSavesAreByteIdentical()
	 */
	static AutoCloseable pinForTesting(String replica, long counter) {

		final String previousReplica = processReplica;
		final long previousCounter = NEXT_COUNTER.get();
		processReplica = replica;
		NEXT_COUNTER.set(counter);
		return () -> {
			processReplica = previousReplica;
			NEXT_COUNTER.getAndUpdate(
					current -> Math.max(current, previousCounter));
		};
	} // end of pinForTesting method

	/** The replica id that minted this identity. */
	private final String replica;
	/** The creation counter within the minting replica. */
	private final long counter;

	/**
	 * Bind a replica id and creation counter into an identity. Private:
	 * callers mint through {@link #mintFresh()}, {@link #legacy(long)}, or
	 * {@link #parse(String)}.
	 *
	 * @param replica The replica id (this process, "legacy", or parsed).
	 * @param counter The creation counter within that replica.
	 */
	private ElementId(String replica, long counter) {

		this.replica = replica;
		this.counter = counter;
	} // end of constructor

	/**
	 * Mint the identity for an element being created right now, in this
	 * process. Every call returns a distinct id.
	 *
	 * @return the new identity.
	 *
	 * @jls.testedby jls.StableElementIdTest#freshMintsAreDistinctAndOrdered()
	 */
	public static ElementId mintFresh() {

		return new ElementId(processReplica, NEXT_COUNTER.getAndIncrement());
	} // end of mintFresh method

	/**
	 * The identity for an element at the given file-order position of a
	 * file that predates stable ids. Deterministic: the same file always
	 * mints the same ids.
	 *
	 * @param counter The element's position in file order.
	 *
	 * @return the legacy identity.
	 */
	public static ElementId legacy(long counter) {

		return new ElementId(LEGACY_REPLICA, counter);
	} // end of legacy method

	/**
	 * Parse the {@code replica:counter} string form, as written by
	 * {@link #toString()} into saved files.
	 *
	 * @param text The string form.
	 *
	 * @return the identity.
	 *
	 * @throws IllegalArgumentException if the text is not a well-formed
	 *             stable id (the loader reports this as an element error,
	 *             issue #52 taxonomy).
	 *
	 * @jls.testedby jls.DeterministicSaveTest#savedBlocksAreSortedByStableId()
	 * @jls.testedby jls.StableElementIdTest#freshMintsAreDistinctAndOrdered()
	 * @jls.testedby jls.StableElementIdTest#parseIsTheInverseOfToString()
	 */
	public static ElementId parse(String text) {

		int sep = text == null ? -1 : text.lastIndexOf(':');
		if (sep < 1 || sep == text.length() - 1) {
			throw new IllegalArgumentException("the stable id '" + text
					+ "' is not in replica:counter form");
		}
		String replica = text.substring(0, sep);
		if (!replica.matches("[0-9a-z]{1,64}")) {
			throw new IllegalArgumentException("the stable id replica '"
					+ replica + "' is not 1-64 characters of [0-9a-z]");
		}
		long counter;
		try {
			counter = Long.parseLong(text.substring(sep + 1));
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException("the stable id counter '"
					+ text.substring(sep + 1) + "' is not a number");
		}
		if (counter < 0) {
			throw new IllegalArgumentException("the stable id counter "
					+ counter + " is negative");
		}
		return new ElementId(replica, counter);
	} // end of parse method

	/**
	 * The canonical order (issue #166): by replica id, then by counter.
	 *
	 * @jls.testedby jls.DeterministicSaveTest#savedBlocksAreSortedByStableId()
	 * @jls.testedby jls.StableElementIdTest#freshMintsAreDistinctAndOrdered()
	 */
	@Override
	public int compareTo(ElementId other) {

		int byReplica = replica.compareTo(other.replica);
		if (byReplica != 0) {
			return byReplica;
		}
		return Long.compare(counter, other.counter);
	} // end of compareTo method

	/**
	 * Two ids are equal when their replica id and counter both match.
	 *
	 * @param other The object to compare against.
	 *
	 * @return true if other is an ElementId with the same replica and
	 *         counter.
	 */
	@Override
	public boolean equals(Object other) {

		if (!(other instanceof ElementId id)) {
			return false;
		}
		return counter == id.counter && replica.equals(id.replica);
	} // end of equals method

	/**
	 * A hash derived from the replica id and counter, consistent with
	 * {@link #equals(Object)}.
	 *
	 * @return the hash code.
	 */
	@Override
	public int hashCode() {

		return replica.hashCode() * 31 + Long.hashCode(counter);
	} // end of hashCode method

	/**
	 * The string form persisted in saved files: {@code replica:counter}.
	 *
	 * @jls.testedby jls.StableElementIdTest#freshMintsAreDistinctAndOrdered()
	 * @jls.testedby jls.StableElementIdTest#parseIsTheInverseOfToString()
	 * @jls.testedby jls.StableElementIdTest#sidsByLogicalElement()
	 */
	@Override
	public String toString() {

		return replica + ":" + counter;
	} // end of toString method

} // end of ElementId class
