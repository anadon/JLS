package jls.collab.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * The short authentication string's contract (issue #168, research
 * doc section 5.2 step 3): seven glyphs of six bits from a shared
 * secret, drawn from a fixed vocabulary of 64 distinct, speakable
 * names whose order is part of the protocol.
 */
class SasTest {

	@Test
	void theVocabularyHas64DistinctSpeakableNames() {
		assertEquals(64, Sas.vocabularySize());
		Set<String> names = new HashSet<String>();
		for (int i = 0; i < 64; i += 1) {
			String word = Sas.word(i);
			assertTrue(word.matches("[a-z]{3,10}"),
					"glyph names are plain lowercase words: " + word);
			names.add(word);
		}
		assertEquals(64, names.size(),
				"every glyph name must be distinct");
		assertThrows(IllegalArgumentException.class,
				() -> Sas.word(64));
		assertThrows(IllegalArgumentException.class,
				() -> Sas.word(-1));
	}

	@Test
	void derivationIsDeterministicAndBitExact() {
		byte[] zeros = new byte[8];
		Sas allFirst = Sas.fromSecret(zeros);
		assertEquals(List.of(Sas.word(0), Sas.word(0), Sas.word(0),
				Sas.word(0), Sas.word(0), Sas.word(0), Sas.word(0)),
				allFirst.words(),
				"an all-zero secret selects glyph 0 seven times");

		byte[] ones = new byte[8];
		java.util.Arrays.fill(ones, (byte) 0xff);
		assertEquals(List.of(Sas.word(63), Sas.word(63), Sas.word(63),
				Sas.word(63), Sas.word(63), Sas.word(63), Sas.word(63)),
				Sas.fromSecret(ones).words(),
				"an all-one secret selects glyph 63 seven times");

		// 0b000001_000010 ... : the first byte 0000 0100 and second
		// byte 0010 0000 make glyph 0 = 000001 = 1, glyph 1 = 000010
		byte[] counted = { 0x04, 0x20, 0, 0, 0, 0, 0, 0 };
		Sas mixed = Sas.fromSecret(counted);
		assertEquals(Sas.word(1), mixed.words().get(0));
		assertEquals(Sas.word(2), mixed.words().get(1));
		assertEquals(Sas.word(0), mixed.words().get(2));

		assertEquals(Sas.fromSecret(counted), Sas.fromSecret(counted));
		assertEquals(Sas.fromSecret(counted).hashCode(),
				Sas.fromSecret(counted).hashCode());
		assertNotEquals(Sas.fromSecret(counted), allFirst);
	}

	@Test
	void theSpokenFormJoinsTheSevenWords() {
		Sas sas = Sas.fromSecret(new byte[8]);
		assertEquals(String.join(" ", sas.words()), sas.toString());
		assertEquals(7, sas.glyphIndices().length);
	}

	@Test
	void shortSecretsAreErrors() {
		assertThrows(IllegalArgumentException.class,
				() -> Sas.fromSecret(new byte[5]));
		assertThrows(IllegalArgumentException.class,
				() -> Sas.fromSecret(null));
	}

} // end of SasTest class
