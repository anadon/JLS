package jls.sim;

import java.util.BitSet;

/**
 * One recorded sample in a watched element's batch trace: the value the
 * element held starting at the given simulation time (issue #77). The
 * BitSet width is the element's bit count plus one, with the extra top
 * bit set to mark a HiZ (undriven) value.
 *
 * This is a headless-core type: consumers such as the VCD exporter
 * (issue #72) and the GUI-side {@link jls.BatchTracePrinter} read
 * samples from {@link BatchSimulator#getTraceSamples}.
 *
 * @param time The simulation time at which the value took effect.
 * @param value The recorded value, with HiZ encoded as the marker
 *        BitSet (only the top bit set).
 */
public record TraceSample(long time, BitSet value) {
} // end of TraceSample record
