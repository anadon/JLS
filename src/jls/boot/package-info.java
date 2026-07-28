/**
 * The compiled-in module assembly (issue #220, grand-architecture §4):
 * the wiring layer that turns JLS's built-in subsystems into
 * {@link jls.module.JlsModule} manifests and boots them through
 * {@link jls.module.ModuleRuntime}'s resolve → register → start path.
 * {@link jls.boot.JlsModules} declares the host's extension points,
 * lists the four shipped modules — {@link jls.boot.CoreModule},
 * {@link jls.boot.GuiModule}, {@link jls.boot.HdlModule},
 * {@link jls.boot.CollabModule} — and exposes the single {@code boot()}
 * the application entry point calls.
 *
 * <p>This is the assembly tier, above both the headless core and the
 * GUI: it deliberately imports from every layer at once (core element
 * registry, GUI palette, HDL emitters, collab op stream) to bind them
 * to the host's declared seams. It is therefore <em>not</em> enrolled
 * in {@code HeadlessCoreRatchetTest} — it sits above that boundary by
 * design, while every module it wires stays on the correct side of it.
 * The module contributions used at boot are AWT-free, so booting is
 * safe in headless batch and HDL modes.</p>
 *
 * <p>Null-marked (issue #93): every reference in this package is
 * non-null unless annotated {@code @Nullable}, and NullAway enforces
 * the contract at compile time on the default build.</p>
 */
@NullMarked
package jls.boot;

import org.jspecify.annotations.NullMarked;
