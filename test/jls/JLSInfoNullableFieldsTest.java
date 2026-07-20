package jls;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Nullability annotation coverage (issue #93): the genuinely nullable
 * static reference fields on {@link JLSInfo} - {@code frame},
 * {@code sim}, and {@code lastLoadError} - must carry
 * {@code @Nullable} so NullAway can enforce their contracts once the
 * package is marked.
 */
class JLSInfoNullableFieldsTest {

	/** Field names expected to carry {@code @Nullable}. */
	private static final List<String> EXPECTED_NULLABLE = List.of(
			"frame", "sim", "lastLoadError");

	/**
	 * Every expected field is annotated {@code @Nullable}.
	 *
	 * @throws NoSuchFieldException if an expected field does not exist.
	 */
	@Test
	void expectedFieldsAreNullable() throws NoSuchFieldException {
		List<String> missing = new ArrayList<String>();
		for (String name : EXPECTED_NULLABLE) {
			Field field = JLSInfo.class.getDeclaredField(name);
			if (!field.getAnnotatedType().isAnnotationPresent(Nullable.class)) {
				missing.add(name);
			}
		}
		assertTrue(missing.isEmpty(),
				"JLSInfo fields missing @Nullable (issue #93): " + missing);
	}

} // end of JLSInfoNullableFieldsTest class
