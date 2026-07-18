/**
 * Test suite for the {@link jls.sim} simulation engine, focused on the
 * interactive simulator's Swing controls and the signal-trace display.
 * {@code InteractiveSimulatorFieldTest} exercises input validation of the
 * time-scale, step-amount, and time-limit control fields, guarding against
 * the numeric-overflow and bad-input parsing defects of issue #119. The
 * {@code TraceGeometry}, {@code TraceRetention}, and {@code TraceWindowing}
 * tests cover the waveform trace model from issue #121: tic-increment and
 * label-stride geometry, history retention independent of panel width, and
 * clip-windowed repainting that must render pixel-identically to a full
 * repaint. All tests run headless, driving the real components rather than
 * mocks so they double as regression evidence against the pre-fix commits.
 */
package jls.sim;
