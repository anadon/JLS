/**
 * Small, self-contained utility helpers shared across the JLS editor.
 * Classes here hold stateless static helpers that keep tricky logic out
 * of the UI and simulation code and make it independently testable; for
 * example, {@link jls.util.Placement} computes where a newly created
 * circuit element should be dropped on the canvas using only event-local
 * mouse coordinates, avoiding brittle global-pointer queries that fail on
 * modern display servers such as Wayland. Add general-purpose, dependency-light
 * utilities here rather than embedding them in the packages that use them.
 *
 * <p>Null-marked (issue #93): every reference in this package is
 * non-null unless annotated {@code @Nullable}, and NullAway enforces
 * the contract at compile time on the default build.</p>
 */
@NullMarked
package jls.util;

import org.jspecify.annotations.NullMarked;
