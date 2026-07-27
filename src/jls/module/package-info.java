/**
 * The module runtime foundation (issue #220, grand-architecture §4):
 * declarative {@link jls.module.ModuleManifest} records that describe
 * what a module provides, requires, and how it orders — readable
 * without running any module code — plus the
 * {@link jls.module.ModuleResolver} that turns a set of manifests into
 * one deterministic startup order (Kahn's topological sort, loud cycle
 * diagnostics). This package is pure data and algorithm: the sealed
 * lifecycle interface and the two-phase register/start runner arrive in
 * a later slice, once the first tenant modules exist.
 *
 * <p>Core-clean from birth: nothing here may import AWT, Swing, or
 * {@code jls.edit}, enforced by {@code HeadlessCoreRatchetTest} with no
 * baseline entry ever.</p>
 *
 * <p>Null-marked (issue #93): every reference in this package is
 * non-null unless annotated {@code @Nullable}, and NullAway enforces
 * the contract at compile time on the default build.</p>
 */
@NullMarked
package jls.module;

import org.jspecify.annotations.NullMarked;
