package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import jls.collab.op.OpExtensionPoints;
import jls.edit.GuiExtensionPoints;
import jls.elem.ElementExtensionPoints;
import jls.hdl.HdlEmitter;
import jls.hdl.HdlExtensionPoints;
import jls.hdl.VerilogEmitter;
import jls.hdl.VhdlEmitter;
import jls.module.ExtensionPoint;
import jls.module.ExtensionRegistry;

/**
 * The completeness test for the extension-point catalog (issue #223):
 * every typed-now seam is one {@code public static final}
 * {@link ExtensionPoint} constant in its home-package holder class,
 * ids are unique and follow the kebab-case dot-prefixed convention,
 * contracts are closed types (interface, sealed, or final — never an
 * open concrete class), and the normative table in
 * {@code docs/extension-points.md} agrees with the constants in both
 * directions, so the doc can never drift from the code.
 */
class ExtensionPointCatalogTest {

	/** The catalog holder classes, one per home package. */
	private static final List<Class<?>> HOLDERS = List.of(
			ElementExtensionPoints.class,
			GuiExtensionPoints.class,
			HdlExtensionPoints.class,
			OpExtensionPoints.class);

	/** Point ids: home-area prefix, dot, kebab-case segments. */
	private static final Pattern ID_SHAPE = Pattern.compile(
			"[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*(-[a-z0-9]+)*)+");

	/** A backticked point id inside a markdown table cell. */
	private static final Pattern DOC_ID = Pattern.compile(
			"`([a-z][a-z0-9.-]*)`");

	/** Every catalog constant, keyed by point id, holder order. */
	private static Map<String, ExtensionPoint<?>> constants() {

		Map<String, ExtensionPoint<?>> byId =
				new LinkedHashMap<String, ExtensionPoint<?>>();
		for (Class<?> holder : HOLDERS) {
			for (Field field : holder.getDeclaredFields()) {
				if (!ExtensionPoint.class
						.isAssignableFrom(field.getType())) {
					continue;
				}
				int mods = field.getModifiers();
				assertTrue(Modifier.isPublic(mods)
						&& Modifier.isStatic(mods)
						&& Modifier.isFinal(mods),
						holder.getSimpleName() + "." + field.getName()
								+ " must be public static final");
				ExtensionPoint<?> point;
				try {
					point = (ExtensionPoint<?>) field.get(null);
				} catch (IllegalAccessException impossible) {
					throw new AssertionError(impossible);
				}
				ExtensionPoint<?> previous =
						byId.put(point.id(), point);
				assertEquals(null, previous,
						"duplicate catalog id '" + point.id() + "'");
			}
		}
		return byId;
	}

	/** The typed-now ids the doc table declares, in table order. */
	private static List<String> typedNowDocIds() throws IOException {

		Path doc = Path.of(System.getProperty("user.dir"), "docs",
				"extension-points.md");
		assertTrue(Files.isRegularFile(doc),
				"catalog document not found at " + doc);
		List<String> ids = new ArrayList<String>();
		for (String line : Files.readAllLines(doc)) {
			if (!line.startsWith("|") || line.startsWith("| ---")) {
				continue;
			}
			String[] cells = line.split("\\|");
			if (cells.length < 8) {
				continue;
			}
			String status = cells[7].strip();
			if (!status.startsWith("typed now")) {
				continue;
			}
			Matcher id = DOC_ID.matcher(cells[2]);
			assertTrue(id.find(),
					"typed-now row without a backticked point id: "
							+ line);
			ids.add(id.group(1));
		}
		return ids;
	}

	@Test
	void everyHolderIsAnUninstantiableConstantTable() {

		for (Class<?> holder : HOLDERS) {
			assertTrue(Modifier.isFinal(holder.getModifiers()),
					holder.getName() + " must be final");
			for (var constructor : holder.getDeclaredConstructors()) {
				assertTrue(Modifier.isPrivate(
						constructor.getModifiers()),
						holder.getName()
								+ " must not be instantiable");
			}
		}
		assertTrue(!constants().isEmpty(),
				"catalog must not be empty");
	}

	@Test
	void idsAreUniqueKebabCaseAndPrefixed() {

		Map<String, ExtensionPoint<?>> byId = constants();
		assertTrue(byId.size() >= 4,
				"the four shipped seams must all be catalogued, found "
						+ byId.keySet());
		for (String id : byId.keySet()) {
			assertTrue(ID_SHAPE.matcher(id).matches(),
					"point id '" + id + "' is not kebab-case with a "
							+ "dot home-area prefix");
		}
	}

	@Test
	void contractsAreClosedTypes() {

		for (ExtensionPoint<?> point : constants().values()) {
			Class<?> contract = point.contract();
			assertTrue(contract.isInterface() || contract.isSealed()
					|| Modifier.isFinal(contract.getModifiers()),
					"contract " + contract.getName() + " of point '"
							+ point.id() + "' must be an interface, "
							+ "sealed, or final - never an open "
							+ "concrete class");
		}
	}

	@Test
	void docTableAndConstantsAgreeBothWays() throws IOException {

		List<String> docIds = typedNowDocIds();
		Map<String, ExtensionPoint<?>> byId = constants();

		TreeSet<String> undocumented =
				new TreeSet<String>(byId.keySet());
		docIds.forEach(undocumented::remove);
		assertEquals(new TreeSet<String>(), undocumented,
				"catalog constants missing a typed-now row in "
						+ "docs/extension-points.md");

		TreeSet<String> phantom = new TreeSet<String>(docIds);
		phantom.removeAll(byId.keySet());
		assertEquals(new TreeSet<String>(), phantom,
				"typed-now rows in docs/extension-points.md without a "
						+ "matching catalog constant");

		assertEquals(docIds.size(),
				new TreeSet<String>(docIds).size(),
				"duplicate typed-now ids in the doc table: " + docIds);
	}

	@Test
	void bothHdlEmittersAreAcceptableExporterContributions() {

		ExtensionRegistry registry = new ExtensionRegistry(
				List.of(HdlExtensionPoints.EXPORTER));
		registry.contribute(HdlExtensionPoints.EXPORTER, "hdl.export",
				new VerilogEmitter());
		registry.contribute(HdlExtensionPoints.EXPORTER, "hdl.export",
				new VhdlEmitter());
		List<HdlEmitter> emitters =
				registry.contributions(HdlExtensionPoints.EXPORTER);
		assertEquals(2, emitters.size());
		assertEquals("v", emitters.get(0).fileExtension());
		assertEquals("vhdl", emitters.get(1).fileExtension());
	}

} // end of ExtensionPointCatalogTest class
