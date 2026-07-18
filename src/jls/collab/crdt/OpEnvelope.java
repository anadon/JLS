package jls.collab.crdt;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import jls.collab.op.CircuitOp;
import jls.collab.op.CircuitOpReader;
import jls.collab.op.OpRejected;

/**
 * One operation as it travels between peers (issue #171, collab
 * Stage 2, from the #163 sketch): the op itself plus the replication
 * metadata causal delivery needs — the originating peer, that peer's
 * sequence number, and the origin's vector clock at emission. The
 * signature slot from the sketch is deliberately absent for now: it
 * belongs to the payload-hardening and transport-identity work
 * (issues #170/#168) and will wrap this frame rather than live in it.
 *
 * Grammar (save-format idiom; {@link #save} and {@link #read} are exact
 * inverses, and the same envelope always writes the same bytes):
 *
 * <pre>
 * ENVELOPE
 *  String origin "&lt;peer&gt;"
 *  long seq &lt;n&gt;
 *  long peers &lt;count&gt;
 *  clock &lt;peer&gt; &lt;n&gt;     (exactly &lt;count&gt; lines, strictly ascending by peer)
 * OP &lt;kind&gt;
 *  ...
 * END
 * END ENVELOPE
 * </pre>
 *
 * The reader is strict by design — this is the network surface (issues
 * #163/#170), so malformed input is a rejection, never a best-effort
 * repair. Envelope invariant: the clock's counter for the origin equals
 * the sequence number (an op is the origin's {@code seq}-th event).
 *
 * @param id The op's permanent identity (origin peer and sequence).
 * @param clock The origin's vector clock at emission, including this
 *            op itself.
 * @param op The operation being replicated.
 */
public record OpEnvelope(OpId id, VectorClock clock, CircuitOp op) {

	/** Hostile-input cap on clock entries (issue #38 discipline). */
	private static final int MAX_PEERS = 256;

	/**
	 * Validate the envelope invariant.
	 *
	 * @throws IllegalArgumentException if the op is null or the clock's
	 *             counter for the origin differs from the sequence
	 *             number.
	 */
	public OpEnvelope {

		if (op == null) {
			throw new IllegalArgumentException(
					"an envelope must carry an op");
		}
		if (clock.counter(id.origin()) != id.seq()) {
			throw new IllegalArgumentException("the clock counts "
					+ clock.counter(id.origin()) + " ops from origin '"
					+ id.origin() + "' but the envelope is op number "
					+ id.seq());
		}
	} // end of constructor

	/**
	 * The originating peer id.
	 *
	 * @return the origin.
	 */
	public String origin() {

		return id.origin();
	} // end of origin method

	/**
	 * The origin's sequence number for this op.
	 *
	 * @return the sequence number.
	 */
	public long seq() {

		return id.seq();
	} // end of seq method

	/**
	 * Serialize this envelope in the save-format idiom. Deterministic:
	 * the same envelope always writes the same bytes.
	 *
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {

		output.println("ENVELOPE");
		output.println(" String origin \"" + id.origin() + "\"");
		output.println(" long seq " + id.seq());
		output.println(" long peers " + clock.entries().size());
		for (Map.Entry<String, Long> entry : clock.entries().entrySet()) {
			output.println(" clock " + entry.getKey() + " "
					+ entry.getValue());
		}
		op.save(output);
		output.println("END ENVELOPE");
	} // end of save method

	/**
	 * Read the next envelope from the scanner: the exact inverse of
	 * {@link #save}.
	 *
	 * @param input The input, positioned at an {@code ENVELOPE} line.
	 *
	 * @return the parsed envelope.
	 *
	 * @throws OpRejected if the input is not a well-formed envelope.
	 */
	public static OpEnvelope read(Scanner input) throws OpRejected {

		requireLine(input, "ENVELOPE");
		String originLine = nextLine(input);
		if (!originLine.startsWith(" String origin \"")
				|| !originLine.endsWith("\"")
				|| originLine.length() < 17) {
			throw new OpRejected("expected ' String origin \"...\"',"
					+ " found '" + clip(originLine) + "'");
		}
		String origin = originLine.substring(16, originLine.length() - 1);
		checkPeer(origin);
		long seq = parseCount(nextLine(input), " long seq ", "seq");
		long peers = parseCount(nextLine(input), " long peers ", "peers");
		if (peers > MAX_PEERS) {
			throw new OpRejected("the envelope's clock lists " + peers
					+ " peers; the limit is " + MAX_PEERS);
		}
		TreeMap<String, Long> counters = new TreeMap<String, Long>();
		String previous = null;
		for (long i = 0; i < peers; i += 1) {
			String line = nextLine(input);
			if (!line.startsWith(" clock ")) {
				throw new OpRejected("expected a ' clock' line, found '"
						+ clip(line) + "'");
			}
			int sep = line.indexOf(' ', 7);
			if (sep < 0) {
				throw new OpRejected("the clock line '" + clip(line)
						+ "' is missing its counter");
			}
			String peer = line.substring(7, sep);
			checkPeer(peer);
			if (previous != null && peer.compareTo(previous) <= 0) {
				throw new OpRejected("clock peers are not strictly"
						+ " ascending: '" + peer + "' follows '"
						+ previous + "'");
			}
			previous = peer;
			long count = parseCanonical(line.substring(sep + 1),
					"the counter for peer '" + peer + "'");
			if (count < 1) {
				throw new OpRejected("the counter for peer '" + peer
						+ "' is " + count
						+ "; counters are strictly positive");
			}
			counters.put(peer, count);
		}
		Long originCount = counters.get(origin);
		if (originCount == null || originCount != seq) {
			throw new OpRejected("the clock counts "
					+ (originCount == null ? 0 : originCount)
					+ " ops from origin '" + origin
					+ "' but the envelope is op number " + seq);
		}
		CircuitOp op = CircuitOpReader.read(input);
		requireLine(input, "END ENVELOPE");
		return new OpEnvelope(new OpId(origin, seq),
				VectorClock.of(counters), op);
	} // end of read method

	/**
	 * Consume one line and require it to be exactly the expected text.
	 *
	 * @param input The input.
	 * @param expected The required line.
	 *
	 * @throws OpRejected if the line is absent or different.
	 */
	private static void requireLine(Scanner input, String expected)
			throws OpRejected {

		String line = nextLine(input);
		if (!line.equals(expected)) {
			throw new OpRejected("expected '" + expected + "', found '"
					+ clip(line) + "'");
		}
	} // end of requireLine method

	/**
	 * Consume one line, rejecting end of input.
	 *
	 * @param input The input.
	 *
	 * @return the line.
	 *
	 * @throws OpRejected if the input is exhausted.
	 */
	private static String nextLine(Scanner input) throws OpRejected {

		if (!input.hasNextLine()) {
			throw new OpRejected(
					"the envelope ends before its END ENVELOPE line");
		}
		return input.nextLine();
	} // end of nextLine method

	/**
	 * Parse a strictly positive long from a typed line.
	 *
	 * @param line The full line.
	 * @param prefix The required line prefix.
	 * @param name The field name, for rejection messages.
	 *
	 * @return the value.
	 *
	 * @throws OpRejected if the line has the wrong prefix or the value
	 *             is not a canonical strictly positive decimal.
	 */
	private static long parseCount(String line, String prefix,
			String name) throws OpRejected {

		if (!line.startsWith(prefix)) {
			throw new OpRejected("expected '" + prefix.trim()
					+ "', found '" + clip(line) + "'");
		}
		return parseCanonical(line.substring(prefix.length()),
				"the '" + name + "' field");
	} // end of parseCount method

	/**
	 * Parse a canonical decimal: the digits must round-trip exactly
	 * (no signs, leading zeros, or whitespace — determinism on the
	 * wire) and the value must be non-negative.
	 *
	 * @param token The digit token.
	 * @param what What is being parsed, for rejection messages.
	 *
	 * @return the value.
	 *
	 * @throws OpRejected if the token is not a canonical non-negative
	 *             decimal.
	 */
	private static long parseCanonical(String token, String what)
			throws OpRejected {

		long value;
		try {
			value = Long.parseLong(token);
		} catch (NumberFormatException e) {
			throw new OpRejected(what + " is not a number: '"
					+ clip(token) + "'");
		}
		if (value < 0 || !String.valueOf(value).equals(token)) {
			throw new OpRejected(what + " is not a canonical"
					+ " non-negative decimal: '" + clip(token) + "'");
		}
		return value;
	} // end of parseCanonical method

	/**
	 * Reject a malformed peer id with an {@link OpRejected} (the
	 * reader's failure type) rather than the programmer-error
	 * {@link IllegalArgumentException}.
	 *
	 * @param peer The candidate peer id.
	 *
	 * @throws OpRejected if the id is malformed.
	 */
	private static void checkPeer(String peer) throws OpRejected {

		try {
			VectorClock.checkPeer(peer);
		} catch (IllegalArgumentException e) {
			throw new OpRejected(e.getMessage());
		}
	} // end of checkPeer method

	/**
	 * Truncate hostile input for a rejection message.
	 *
	 * @param text The raw text.
	 *
	 * @return at most 60 characters of it.
	 */
	private static String clip(String text) {

		return text.length() <= 60 ? text : text.substring(0, 60) + "...";
	} // end of clip method

} // end of OpEnvelope record
