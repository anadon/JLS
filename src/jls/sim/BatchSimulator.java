package jls.sim;

import jls.*;
import jls.elem.*;

import java.util.*;

import org.jspecify.annotations.Nullable;

/**
 * Event driven circuit simulator.
 *
 * Headless by construction (issue #77): this class must not import
 * AWT, Swing, or {@code jls.edit} - the HeadlessCoreRatchetTest
 * enforces it. Trace printing, the one AWT concern it used to own,
 * lives GUI-side in {@link jls.BatchTracePrinter} consuming
 * {@link #getTraceSamples}.
 *
 * @author David A. Poplawski
 */
public class BatchSimulator extends Simulator {

	/** Per watched element, its recorded samples in time order. */
	private Map<LogicElement,List<TraceSample>> eventTrace =
		new HashMap<LogicElement,List<TraceSample>>();

	/**
	 * Per probed net (keyed by probe name), its recorded samples in time
	 * order (issue #200). Probes name a wire net rather than an element,
	 * so they trace through a parallel map fed by
	 * {@link #probeSample} and merged into the VCD by {@link #toVcd}.
	 */
	private final Map<String,List<TraceSample>> probeTrace =
		new HashMap<String,List<TraceSample>>();
	/** Per probed net, its bit width (issue #200). */
	private final Map<String,Integer> probeBits =
		new HashMap<String,Integer>();

	/**
	 * VCD export (issue #72): the file to write, or null for no export.
	 * A non-null value enables trace accumulation in afterEvent even
	 * when the -r printer flag (JLSInfo.printTrace) is off.
	 */
	private @Nullable String vcdFileName = null;

	/**
	 * Create a new Simulator object.
	 *
	 * @jls.testedby jls.BatchSimulationGoldenTest#ramWriteStoresTheWord()
	 * @jls.testedby jls.BatchSimulationGoldenTest#simulate()
	 * @jls.testedby jls.BatchSimulationGoldenTest#watchedElementsPrintInNameOrder()
	 * @jls.testedby jls.ElementSimulationGoldenTest#simulate()
	 * @jls.testedby jls.ElementSimulationGoldenTest#stopHaltsTheSimulationEarly()
	 * @jls.testedby jls.SequentialGoldenTest#simulate()
	 * @jls.testedby jls.SequentialGoldenTest#simulateWithVectors()
	 * @jls.testedby jls.ShiftRegisterTest#simulate()
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#agreeingTriStateDriversDoNotWarn()
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#constantValueIsMaskedToTheNetWidth()
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#initInputsReachesInsideSubcircuits()
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#multiDriverConflictResolvesDeterministicallyAndWarnsOnce()
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#registerInitSimResetsTheWatchedCurrentValue()
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#stateMachineWithNoMatchingTransitionStaysAliveAndWarnsOnce()
	 * @jls.testedby jls.VcdExportGoldenTest#clockedRegisterVcdMatchesGoldenByteForByte()
	 * @jls.testedby jls.VcdExportGoldenTest#testVectorStimulusVcdMatchesGoldenAndCoversHiZ()
	 * @jls.testedby jls.elem.MemoryInitEncodingTest#rleMemorySimulatesLikeRawMemory()
	 */
	public BatchSimulator() {

	} // end of constructor

	/**
	 * Stop (end) the simulation, if there is one.
	 */
	@Override
	public void stop() {

		stopping = true;
	} // end of stop method

	/**
	 * Stop simulation.
	 * It doesn't make sense to pause it in batch mode.
	 *
	 * @param which Ignored.
	 */
	@Override
	public void pause(boolean which) {

		stopping = true;
	} // end of pause method

	/**
	 * Run the simulator.
	 *
	 * @jls.testedby jls.BatchSimulationGoldenTest#ramWriteStoresTheWord()
	 * @jls.testedby jls.BatchSimulationGoldenTest#simulate()
	 * @jls.testedby jls.BatchSimulationGoldenTest#watchedElementsPrintInNameOrder()
	 * @jls.testedby jls.ElementSimulationGoldenTest#simulate()
	 * @jls.testedby jls.ElementSimulationGoldenTest#stopHaltsTheSimulationEarly()
	 * @jls.testedby jls.SequentialGoldenTest#simulate()
	 * @jls.testedby jls.SequentialGoldenTest#simulateWithVectors()
	 * @jls.testedby jls.ShiftRegisterTest#simulate()
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#agreeingTriStateDriversDoNotWarn()
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#constantValueIsMaskedToTheNetWidth()
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#multiDriverConflictResolvesDeterministicallyAndWarnsOnce()
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#registerInitSimResetsTheWatchedCurrentValue()
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#stateMachineWithNoMatchingTransitionStaysAliveAndWarnsOnce()
	 * @jls.testedby jls.VcdExportGoldenTest#clockedRegisterVcdMatchesGoldenByteForByte()
	 * @jls.testedby jls.VcdExportGoldenTest#testVectorStimulusVcdMatchesGoldenAndCoversHiZ()
	 * @jls.testedby jls.elem.MemoryInitEncodingTest#rleMemorySimulatesLikeRawMemory()
	 */
	public void runSim() {

		// reset clock/queues and initialize all elements
		initSimulation();

		// find watched elements and set up trace map
		findWatched(circuit());

		// register probed nets so they trace into the VCD alongside
		// watched elements (issue #200); only when a trace consumer is
		// active, matching afterEvent's gate
		if (JLSInfo.printTrace || vcdFileName != null) {
			findProbes(circuit());
		}

		// run the shared event loop (tracing happens in afterEvent and,
		// for probed nets, in probeSample via WireNet.propagate)
		runEventLoop();

	} // end of runSim

	/**
	 * Record a trace entry for the element that just reacted, if traces
	 * were requested and the element is watched.
	 *
	 * @param event The event that just reacted.
	 */
	@Override
	protected void afterEvent(SimEvent event) {

		// accumulate when any trace consumer is active: the -r printer
		// or the -vcd exporter (issue #72)
		if (!JLSInfo.printTrace && vcdFileName == null)
			return;

		// see if changing element is watched
		LogicElement el = (LogicElement)event.getCallBack();
		if (el.isWatched()) {

			// get the event trace for this element,
			// or create one if none yet
			// findWatched registered every watched element with a time-0
			// entry before the event loop started, so an element without
			// one has no trace to extend (issue #93)
			List<TraceSample> events = eventTrace.get(el);
			if (events == null)
				return;

			// create bitset for HiZ
			BitSet off = new BitSet(el.getBits()+1);
			off.set(el.getBits());

			// normalize a HiZ (null) value to the marker BitSet before
			// comparing, so a value that stays HiZ is not recorded as a
			// change on every react (issue #72)
			BitSet current = el.getCurrentValue();
			if (current == null)
				current = off;

			// add an event to the end of the event list
			// (but not if the same value)
			TraceSample prev = events.getLast();
			if (!prev.value().equals(current)) {

				// add only if different
				events.add(new TraceSample(event.getTime(),current));
			}
		}
	} // end of afterEvent method
	
	/**
	 * Create and add TestGen element to circuit.
	 * Remove any SigGen's in the circuit.
	 *
	 * @jls.testedby jls.SequentialGoldenTest#simulateWithVectors()
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#stateMachineWithNoMatchingTransitionStaysAliveAndWarnsOnce()
	 * @jls.testedby jls.VcdExportGoldenTest#testVectorStimulusVcdMatchesGoldenAndCoversHiZ()
	 */
	public void addTestGen() {

		// create test signal element if necessary
		String testFile = testFileName;
		if (testFile != null) {
			Circuit circ = circuit();
			TestGen gen = new TestGen(circ);
			circ.addElement(gen);
			gen.setFile(testFile);

			// remove any signal generators from this circuit
			// (signal generators in subcircuits will disable themselves)
			Set<Element> gens = new HashSet<Element>();
			for (Element el : circ.getElements()) {
				if (el instanceof SigGen) {
					gens.add(el);
				}
			}
			for (Element el : gens) {
				circ.remove(el);
			}
		}
	} // end of addTestGen method

	/**
	 * Find all watched elements and add entries to batch trace map.
	 * Recursively checks all subcircuits.
	 * 
	 * @param circ The circuit (or subcircuit) to look in.
	 */
	private void findWatched(Circuit circ) {

		for (Element el : circ.getElements()) {
			if (el instanceof SubCircuit sub) {
				findWatched(sub.getSubCircuit());
			}
			else if (el.isWatched()) {
				LogicElement lel = (LogicElement)el;
				List<TraceSample> events = new LinkedList<TraceSample>();
				BitSet value = lel.getCurrentValue();
				if (value == null) {
					value = new BitSet(lel.getBits()+1);
					value.set(lel.getBits());
				}
				events.add(new TraceSample(0,value));
				eventTrace.put(lel,events);
			}
		}
	} // end of findWatched method

	/**
	 * Find all probed nets and seed each with a time-0 sample, so every
	 * probe has a VCD $dumpvars baseline and appears even if its value
	 * never changes (issue #200). Recurses into subcircuits. Wires are
	 * elements of the circuit (Circuit.getElements includes them), and a
	 * probe is attached to a wire; several wire segments of one net may
	 * carry the same probe name, so the first registration wins.
	 *
	 * @param circ The circuit (or subcircuit) to look in.
	 */
	private void findProbes(Circuit circ) {

		for (Element el : circ.getElements()) {
			if (el instanceof SubCircuit sub) {
				findProbes(sub.getSubCircuit());
			}
			else if (el instanceof Wire wire && wire.hasProbe()) {
				String name = wire.getProbe();
				if (probeTrace.containsKey(name)) {
					continue;
				}
				int bits = wire.getBits();
				BitSet value = wire.getValue();
				BitSet sample;
				if (value == null) {
					sample = new BitSet(bits + 1);
					sample.set(bits);
				}
				else {
					sample = (BitSet) value.clone();
				}
				List<TraceSample> events = new LinkedList<TraceSample>();
				events.add(new TraceSample(0, sample));
				probeTrace.put(name, events);
				probeBits.put(name, bits);
			}
		}
	} // end of findProbes method

	/**
	 * Record a probed net's value change (issue #200), mirroring
	 * {@link #afterEvent}'s dedup: extend the probe's sample list only
	 * when the value differs from the previous one, and normalize HiZ to
	 * the marker BitSet so a net that stays undriven is not re-recorded
	 * on every propagate. A probe not registered by {@link #findProbes}
	 * (tracing off) is ignored.
	 *
	 * @param name  The probe name.
	 * @param bits  The net's bit width.
	 * @param time  The simulation time of the change.
	 * @param value The net's new value, or null for HiZ.
	 *
	 * @jls.testedby jls.VcdProbeExportTest#probedNetAppearsInVcd()
	 */
	@Override
	public void probeSample(String name, int bits, long time,
			@Nullable BitSet value) {

		if (!JLSInfo.printTrace && vcdFileName == null) {
			return;
		}
		List<TraceSample> events = probeTrace.get(name);
		if (events == null) {
			return;
		}
		BitSet off = new BitSet(bits + 1);
		off.set(bits);
		BitSet current = (value == null) ? off : (BitSet) value.clone();
		TraceSample prev = events.getLast();
		if (!prev.value().equals(current)) {
			events.add(new TraceSample(time, current));
		}
	} // end of probeSample method

	/**
	 * The recorded traces of every watched element, for consumers such
	 * as the GUI-side trace printer ({@link jls.BatchTracePrinter}).
	 * Each element's samples are ordered oldest first, starting with a
	 * time-0 sample (findWatched guarantees it), with HiZ values
	 * encoded as the marker BitSet described in {@link TraceSample}.
	 *
	 * Traces accumulate only when a consumer was enabled before runSim
	 * (the -r printer flag or a VCD file).
	 *
	 * @return a read-only view of the trace map.
	 *
	 * @jls.testedby jls.BatchTracePrinterTest#traceSamplesRecordTheWatchedRun()
	 * @jls.testedby jls.BatchTracePrinterTest#tracePrintableRendersTheRecordedSamples()
	 */
	public Map<LogicElement,List<TraceSample>> getTraceSamples() {

		return Collections.unmodifiableMap(eventTrace);
	} // end of getTraceSamples method

	/**
	 * Set the VCD output file name, or null for no VCD export.
	 * Must be called before runSim so that afterEvent accumulates the
	 * trace (issue #72).
	 *
	 * @param fileName The VCD file to write, or null.
	 *
	 * @jls.testedby jls.VcdExportGoldenTest#clockedRegisterVcdMatchesGoldenByteForByte()
	 * @jls.testedby jls.VcdExportGoldenTest#testVectorStimulusVcdMatchesGoldenAndCoversHiZ()
	 */
	public void setVcdFile(@Nullable String fileName) {

		vcdFileName = fileName;
	} // end of setVcdFile method

	/**
	 * Write the accumulated trace of all watched elements to the file
	 * given to setVcdFile, as IEEE 1364-2001 (section 18) VCD.
	 *
	 * @throws java.io.IOException if the file cannot be written.
	 * @throws IllegalStateException if setVcdFile has not been called
	 *         with a non-null file name.
	 *
	 * @jls.testedby jls.VcdExportGoldenTest#clockedRegisterVcdMatchesGoldenByteForByte()
	 */
	public void writeVcd() throws java.io.IOException {

		String fileName = vcdFileName;
		if (fileName == null) {
			throw new IllegalStateException(
					"setVcdFile was not called before writeVcd");
		}
		java.nio.file.Files.write(
				java.nio.file.Paths.get(fileName),
				toVcd().getBytes(java.nio.charset.StandardCharsets.UTF_8));
	} // end of writeVcd method

	/**
	 * Render the accumulated trace of all watched elements as an IEEE
	 * 1364-2001 (section 18) Value Change Dump. The exact format is a
	 * compatibility contract documented in docs/batch-interface.md
	 * (issue #72). Deterministic by construction: signals are declared
	 * and dumped in full-name order, no $date/$version headers, one
	 * JLS simulation time unit per VCD time unit (timescale 1 ns).
	 *
	 * @return the complete VCD text.
	 *
	 * @jls.testedby jls.VcdExportGoldenTest#clockedRegisterVcdMatchesGoldenByteForByte()
	 * @jls.testedby jls.VcdExportGoldenTest#testVectorStimulusVcdMatchesGoldenAndCoversHiZ()
	 */
	public String toVcd() {

		StringBuilder out = new StringBuilder();

		// unify watched elements and probed nets (issue #200) into one
		// signal set, keyed by full name for a deterministic header,
		// identifier assignment, and per-timestamp change order. Each
		// signal carries its bit width and its folded time -> value
		// history; the change times accumulate into a shared set.
		record Sig(int bits, TreeMap<Long,BitSet> byTime) { }
		TreeMap<String,Sig> signals = new TreeMap<String,Sig>();
		TreeSet<Long> times = new TreeSet<Long>();
		for (Map.Entry<LogicElement,List<TraceSample>> e
				: eventTrace.entrySet()) {
			signals.put(e.getKey().getFullName(),
					new Sig(e.getKey().getBits(), fold(e.getValue(), times)));
		}
		for (Map.Entry<String,List<TraceSample>> e : probeTrace.entrySet()) {
			// a probe name that collides with an element's full name is
			// disambiguated so neither signal is silently dropped
			String key = e.getKey();
			while (signals.containsKey(key)) {
				key = key + "_probe";
			}
			signals.put(key, new Sig(
					Objects.requireNonNull(probeBits.get(e.getKey())),
					fold(e.getValue(), times)));
		}

		Map<String,String> codes = new HashMap<String,String>();
		int next = 0;
		for (String name : signals.keySet()) {
			codes.put(name, vcdId(next));
			next += 1;
		}

		// header: no $date/$version sections (both optional in the
		// standard) so the same run always produces the same bytes
		out.append("$comment JLS batch simulation trace $end\n");
		out.append("$timescale 1 ns $end\n");
		out.append("$scope module ").append(circuit().getName())
			.append(" $end\n");
		for (Map.Entry<String,Sig> e : signals.entrySet()) {
			int bits = e.getValue().bits();
			out.append("$var wire ").append(bits).append(' ')
				.append(codes.get(e.getKey())).append(' ')
				.append(e.getKey());
			if (bits > 1) {
				out.append(" [").append(bits - 1).append(":0]");
			}
			out.append(" $end\n");
		}
		out.append("$upscope $end\n");
		out.append("$enddefinitions $end\n");

		// initial values (findWatched/findProbes guarantee a time-0 entry
		// for every watched element and probed net)
		out.append("#0\n");
		out.append("$dumpvars\n");
		for (Map.Entry<String,Sig> e : signals.entrySet()) {
			BitSet value = Objects.requireNonNull(
					e.getValue().byTime().get(0L));
			out.append(vcdValue(e.getValue().bits(), value,
					Objects.requireNonNull(codes.get(e.getKey()))))
				.append('\n');
		}
		out.append("$end\n");

		// subsequent changes, grouped by ascending time, signals in
		// name order within each timestamp
		long last = 0;
		for (long t : times) {
			if (t == 0) {
				continue;
			}
			out.append('#').append(t).append('\n');
			for (Map.Entry<String,Sig> e : signals.entrySet()) {
				BitSet value = e.getValue().byTime().get(t);
				if (value != null) {
					out.append(vcdValue(e.getValue().bits(), value,
							Objects.requireNonNull(codes.get(e.getKey()))))
						.append('\n');
				}
			}
			last = t;
		}

		// a final timestamp so viewers show the full simulated duration
		if (now > last) {
			out.append('#').append(now).append('\n');
		}
		return out.toString();
	} // end of toVcd method

	/**
	 * Fold a signal's ordered samples into a time -> value map (the last
	 * sample recorded at a given time wins) and add its change times to
	 * the shared set, so watched elements and probed nets share one
	 * timeline (issue #200).
	 *
	 * @param samples The signal's samples, oldest first.
	 * @param times   The shared change-time set to extend.
	 *
	 * @return the time -> value map for this signal.
	 */
	private static TreeMap<Long,BitSet> fold(List<TraceSample> samples,
			TreeSet<Long> times) {

		TreeMap<Long,BitSet> byTime = new TreeMap<Long,BitSet>();
		for (TraceSample ev : samples) {
			byTime.put(ev.time(), ev.value());
			times.add(ev.time());
		}
		return byTime;
	} // end of fold method

	/**
	 * The VCD identifier code for the n'th signal: the printable ASCII
	 * characters '!' (33) through '~' (126), extended to multiple
	 * characters after 94 signals, assigned in signal-name order.
	 *
	 * @param index The zero-based signal number.
	 *
	 * @return the identifier code.
	 */
	private static String vcdId(int index) {

		StringBuilder code = new StringBuilder();
		int n = index + 1;
		while (n > 0) {
			n -= 1;
			code.insert(0, (char)('!' + n % 94));
			n /= 94;
		}
		return code.toString();
	} // end of vcdId method

	/**
	 * One VCD value-change entry. JLS values are two-state plus HiZ,
	 * so only 0, 1 and z ever appear ('x' never does): a single-bit
	 * signal becomes its value directly followed by the identifier
	 * code ({@code 0c}, {@code 1c} or {@code zc}); a multi-bit signal
	 * becomes a binary vector {@code b<value> <code>} with leading
	 * zeros omitted, or {@code bz <code>} when the whole signal is
	 * HiZ.
	 *
	 * @param bits The signal's bit width (a watched element's or a
	 *        probed net's, issue #200).
	 * @param value The recorded value, with HiZ encoded as the trace's
	 *        marker BitSet (only bit {@code bits} set).
	 * @param code The signal's identifier code.
	 *
	 * @return the value-change line, without the newline.
	 */
	private static String vcdValue(int bits, BitSet value,
			String code) {

		BitSet off = new BitSet(bits + 1);
		off.set(bits);
		boolean hiZ = value.equals(off);
		if (bits == 1) {
			if (hiZ) {
				return "z" + code;
			}
			return (BitSetUtils.ToLong(value) == 0 ? "0" : "1") + code;
		}
		if (hiZ) {
			return "bz " + code;
		}
		return "b" + BitSetUtils.ToBigInteger(value).toString(2)
				+ " " + code;
	} // end of vcdValue method

	/**
	 * Display reason for stopping and the time at which it stopped.
	 *
	 * @jls.testedby jls.ElementSimulationGoldenTest#stopHaltsTheSimulationEarly()
	 */
	public void displayOutcome() {

		String reason = "Simulation Complete";
		if (stopping)
			reason = "Simulation Stopped";
		else if (now >= maxTime)
			reason = "Simulation Time Limit";
		else if (eventQueue.size() == 0)
			reason = "Simulation: No More Activity";
		System.out.println(reason + " at " + now);
	} // end of displayOutcome method

} // end of BatchSimulator class

