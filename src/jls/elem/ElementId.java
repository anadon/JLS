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
 * (editor placement, paste, programmatic construction): a per-process
 * random replica id plus a process-wide counter. When per-install
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
	 * This process's replica id: 32 hex digits, drawn once per process
	 * (a random UUID, which is itself SecureRandom-backed). Hex cannot
	 * collide with the reserved "legacy" replica (it has no letter past
	 * 'f').
	 */
	private static final String PROCESS_REPLICA =
			java.util.UUID.randomUUID().toString().replace("-", "");

	/** The process-wide creation counter for {@link #mintFresh()}. */
	private static final AtomicLong NEXT_COUNTER = new AtomicLong();

	private final String replica;
	private final long counter;

	private ElementId(String replica, long counter) {

		this.replica = replica;
		this.counter = counter;
	} // end of constructor

	/**
	 * Mint the identity for an element being created right now, in this
	 * process. Every call returns a distinct id.
	 *
	 * @return the new identity.
	 */
	public static ElementId mintFresh() {

		return new ElementId(PROCESS_REPLICA, NEXT_COUNTER.getAndIncrement());
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
	 */
	@Override
	public int compareTo(ElementId other) {

		int byReplica = replica.compareTo(other.replica);
		if (byReplica != 0) {
			return byReplica;
		}
		return Long.compare(counter, other.counter);
	} // end of compareTo method

	@Override
	public boolean equals(Object other) {

		if (!(other instanceof ElementId)) {
			return false;
		}
		ElementId id = (ElementId) other;
		return counter == id.counter && replica.equals(id.replica);
	} // end of equals method

	@Override
	public int hashCode() {

		return replica.hashCode() * 31 + Long.hashCode(counter);
	} // end of hashCode method

	/**
	 * The string form persisted in saved files: {@code replica:counter}.
	 */
	@Override
	public String toString() {

		return replica + ":" + counter;
	} // end of toString method

} // end of ElementId class
