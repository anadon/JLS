/**
 * The event-driven simulation engine that executes JLS circuits over
 * discrete time. The abstract {@link jls.sim.Simulator} maintains a
 * time-ordered event queue of {@link jls.sim.SimEvent} signal changes and
 * dispatches them to circuit elements through the {@link jls.sim.Reacts}
 * callback interface; the {@link jls.sim.BatchSimulator} runs headless for
 * command-line and test-vector use (including VCD export), while the
 * {@link jls.sim.InteractiveSimulator} adds a Swing GUI with start, step,
 * animate, pause, and stop controls. Supporting classes render signal
 * waveforms over time ({@link jls.sim.Trace}) and display memory activity
 * ({@link jls.sim.MemTrace}).
 *
 * <p>Null-marked (issue #93): every reference in this package is
 * non-null unless annotated {@code @Nullable}, and NullAway enforces
 * the contract at compile time on the default build.</p>
 */
@NullMarked
package jls.sim;

import org.jspecify.annotations.NullMarked;
