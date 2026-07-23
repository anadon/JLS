/**
 * The headless model core (issue #77): types shared by the circuit
 * model, the simulation engine, and persistence that carry no GUI
 * dependency and must never import AWT, Swing, or {@code jls.edit}.
 * {@link jls.core.Orientation} — the four cardinal directions an
 * element faces — is the first tenant, relocated here from the former
 * {@code jls.JLSInfo} app hub because it is pure model geometry. The
 * package is policed as core-clean from birth by
 * {@code HeadlessCoreRatchetTest} (it never gets a baseline entry).
 *
 * <p>Null-marked (issue #93): every reference in this package is
 * non-null unless annotated {@code @Nullable}, and NullAway enforces
 * the contract at compile time on the default build.</p>
 */
@NullMarked
package jls.core;

import org.jspecify.annotations.NullMarked;
