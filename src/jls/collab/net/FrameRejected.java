package jls.collab.net;

/**
 * A session frame that could not be sealed or opened (issue #168): an
 * over-cap length, a truncated wire read, a failed authentication tag
 * (which also covers replayed and reordered frames, because the nonce
 * is a strict per-direction counter), or any use of a link that has
 * already failed. In the #38 hostile-input style this is a rejection,
 * never a repair - and because an authenticated channel cannot recover
 * trust after a bad tag, the link that threw it is poisoned for good.
 */
public class FrameRejected extends Exception {

	/** Serialization version (this exception is never sent anywhere). */
	private static final long serialVersionUID = 1L;

	/**
	 * Create a rejection.
	 *
	 * @param reason What was wrong with the frame.
	 */
	public FrameRejected(String reason) {

		super(reason);
	} // end of constructor

} // end of FrameRejected class
