package jls.collab.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A short authentication string (issue #168, the {@code
 * ShortAuthString} of the #163 sketch): seven glyphs of six bits each,
 * 42 bits total, taken from a secret both handshake sides derive from
 * the full handshake transcript. Both humans read their seven glyphs
 * to each other out of band; a man-in-the-middle necessarily changes
 * the transcript, so the glyphs differ on at least one side - the
 * Signal safety-number / Telegram call-emoji construction.
 *
 * Each glyph has an English name so the comparison works spoken aloud,
 * and the name doubles as the file name of the bundled glyph image the
 * verify dialog will render (research doc section 5.2: Swing emoji
 * rendering is not dependable, so glyphs ship as images with word
 * labels; the images are a follow-up asset task, the names are the
 * contract).
 */
public final class Sas {

	/** How many glyphs a short authentication string shows. */
	public static final int GLYPH_COUNT = 7;

	/** How many bits select one glyph (the table has 2^6 entries). */
	private static final int BITS_PER_GLYPH = 6;

	/**
	 * The glyph vocabulary: 64 concrete, drawable, phonetically
	 * distinct English nouns (the PGP word-list precedent). Order is
	 * part of the protocol - index n here must mean the same glyph on
	 * every JLS install forever, so entries may be appended in a later
	 * protocol version but never reordered or renamed.
	 */
	private static final String[] WORDS = {
			"acorn", "anchor", "apple", "arrow",
			"balloon", "banana", "basket", "bell",
			"bridge", "brush", "button", "cactus",
			"camera", "candle", "canoe", "castle",
			"cherry", "circle", "cloud", "clover",
			"crown", "diamond", "dolphin", "dragon",
			"drum", "eagle", "feather", "fish",
			"flag", "flower", "forest", "fox",
			"glacier", "grape", "guitar", "hammer",
			"harbor", "heart", "helmet", "island",
			"kite", "ladder", "lantern", "leaf",
			"lemon", "lion", "magnet", "maple",
			"mirror", "moon", "mountain", "mushroom",
			"ocean", "owl", "pearl", "penguin",
			"piano", "rabbit", "rainbow", "river",
			"rocket", "rose", "turtle", "whale" };

	/** The seven glyph indices, each in 0..63. */
	private final int[] glyphs;

	/**
	 * Bind the glyph indices. Private: callers derive through
	 * {@link #fromSecret(byte[])}.
	 *
	 * @param glyphs The seven indices, each already range-checked.
	 */
	private Sas(int[] glyphs) {

		this.glyphs = glyphs;
	} // end of constructor

	/**
	 * Derive the short authentication string from the SAS secret both
	 * handshake sides export: the first 42 bits, big-endian, read as
	 * seven 6-bit glyph indices.
	 *
	 * @param secret The SAS secret; at least 6 bytes are consumed.
	 *
	 * @return the short authentication string.
	 *
	 * @throws IllegalArgumentException if the secret is too short to
	 *             hold 42 bits.
	 */
	public static Sas fromSecret(byte[] secret) {

		int needed = (GLYPH_COUNT * BITS_PER_GLYPH + 7) / 8;
		if (secret == null || secret.length < needed) {
			throw new IllegalArgumentException("a SAS secret must have at"
					+ " least " + needed + " bytes");
		}
		int[] glyphs = new int[GLYPH_COUNT];
		for (int g = 0; g < GLYPH_COUNT; g += 1) {
			int index = 0;
			for (int b = 0; b < BITS_PER_GLYPH; b += 1) {
				int bit = g * BITS_PER_GLYPH + b;
				int byteValue = secret[bit / 8] & 0xff;
				index = (index << 1) | ((byteValue >> (7 - bit % 8)) & 1);
			}
			glyphs[g] = index;
		}
		return new Sas(glyphs);
	} // end of fromSecret method

	/**
	 * How many glyphs the vocabulary holds.
	 *
	 * @return the table size (64 in protocol version 1).
	 */
	public static int vocabularySize() {

		return WORDS.length;
	} // end of vocabularySize method

	/**
	 * The English name of one glyph in the vocabulary.
	 *
	 * @param index The glyph index.
	 *
	 * @return the glyph's name.
	 *
	 * @throws IllegalArgumentException if the index is out of range.
	 */
	public static String word(int index) {

		if (index < 0 || index >= WORDS.length) {
			throw new IllegalArgumentException("glyph index " + index
					+ " is not in 0.." + (WORDS.length - 1));
		}
		return WORDS[index];
	} // end of word method

	/**
	 * The seven glyph names, in display order.
	 *
	 * @return an unmodifiable list of the seven words.
	 */
	public List<String> words() {

		List<String> names = new ArrayList<String>(glyphs.length);
		for (int glyph : glyphs) {
			names.add(WORDS[glyph]);
		}
		return Collections.unmodifiableList(names);
	} // end of words method

	/**
	 * The seven glyph indices, in display order (the verify dialog maps
	 * these to bundled images).
	 *
	 * @return a fresh copy of the indices.
	 */
	public int[] glyphIndices() {

		return glyphs.clone();
	} // end of glyphIndices method

	/**
	 * Two short authentication strings are equal when all seven glyphs
	 * match - the comparison the two humans perform.
	 *
	 * @param other The object to compare against.
	 *
	 * @return true if other shows the same seven glyphs.
	 */
	@Override
	public boolean equals(Object other) {

		if (!(other instanceof Sas sas)) {
			return false;
		}
		return java.util.Arrays.equals(glyphs, sas.glyphs);
	} // end of equals method

	/**
	 * A hash consistent with {@link #equals(Object)}.
	 *
	 * @return the hash code.
	 */
	@Override
	public int hashCode() {

		return java.util.Arrays.hashCode(glyphs);
	} // end of hashCode method

	/**
	 * The spoken form: the seven words joined with spaces.
	 *
	 * @return the words, e.g. {@code "turtle moon anchor ..."}.
	 */
	@Override
	public String toString() {

		return String.join(" ", words());
	} // end of toString method

} // end of Sas class
