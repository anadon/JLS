package jls.elem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jls.Circuit;

/**
 * The loader instantiates elements via an explicit (Circuit) constructor
 * lookup (issue #55, audit finding H4) - getConstructors()[0] was
 * order-unspecified and any element with a second public constructor
 * made loading depend on the JVM build. This sweep enforces the contract
 * the explicit lookup relies on: every concrete element class exposes a
 * public (Circuit) constructor.
 */
class ElementConstructorContractTest {

	@Test
	void everyConcreteElementClassHasAPublicCircuitConstructor()
			throws Exception {
		File dir = new File(Element.class.getProtectionDomain()
				.getCodeSource().getLocation().toURI());
		File pkg = new File(dir, "jls/elem");
		assertTrue(pkg.isDirectory(), "compiled classes not found at " + pkg);

		List<String> violations = new ArrayList<>();
		int checked = 0;
		for (File f : pkg.listFiles()) {
			String name = f.getName();
			if (!name.endsWith(".class") || name.contains("$"))
				continue;
			Class<?> c = Class.forName(
					"jls.elem." + name.replace(".class", ""));
			if (!Element.class.isAssignableFrom(c)
					|| Modifier.isAbstract(c.getModifiers()))
				continue;
			if (c == Wire.class)
				continue; // never saved as ELEMENT; rebuilt from WireEnd refs
			checked++;
			try {
				// existence probe: throws if the (Circuit) constructor
				// is missing; the assert uses the value (Error Prone
				// ReturnValueIgnored, via the #93 NullAway wiring)
				assertNotNull(c.getConstructor(Circuit.class));
			} catch (NoSuchMethodException ex) {
				violations.add(c.getName());
			}
		}
		assertTrue(checked > 20, "sweep found too few element classes ("
				+ checked + ") - wrong directory?");
		assertTrue(violations.isEmpty(),
				"element classes without a public (Circuit) constructor: "
						+ violations);
	}

	@Test
	void jumpEndHasExactlyOnePublicConstructor() {
		// pins the resolution of the commented-out JumpEnd(Circuit,String):
		// it was deleted, not restored (issue #55)
		assertFalse(JumpEnd.class.getConstructors().length != 1,
				"JumpEnd grew a second public constructor; make sure the "
				+ "loader contract sweep still passes and this is deliberate");
	}
}
