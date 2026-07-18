package jls;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import jls.edit.Editor;
import jls.edit.SimpleEditor;
import jls.elem.Element;
import jls.elem.ElementRegistry;
import jls.elem.ElementType;
import jls.elem.JumpStart;
import jls.elem.LogicElement;
import jls.elem.Output;
import jls.elem.SaveTags;
import jls.elem.StateMachine;
import jls.elem.SubCircuit;
import jls.elem.TruthTable;
import jls.elem.Wire;
import jls.elem.WireEnd;
import jls.elem.WireNet;

/**
 * The main (container) class for each circuit.
 * 
 * @author David A. Poplawski
 */
public class Circuit implements Printable {

	// properties
	private String name = null; // will be the file name after appending .jls
	private String dir = ""; // the directory the file is in
	private Set<Element> elements = new HashSet<Element>();
	private SubCircuit subElement = null; // the element referring to this
											// circuit
	private Editor editor = null; // this circuit's editor (null if none)
	private Set<String> namesUsed = new HashSet<String>(); // so element names
															// can be unique
	private SortedMap<String, JumpStart> starts = new TreeMap<String, JumpStart>(); // jumpstarts
	private boolean changed = false;
	private FileAbstractor.Container saveContainer =
			FileAbstractor.Container.XZ; // on-disk container saves use (#129)

	private final SpatialIndex index = new SpatialIndex(); // grid index over
															// element bounds
															// (#3, #17)
	private final Set<Element> highlighted = new HashSet<Element>(); // elements
																		// currently
																		// drawn
																		// highlighted

	// insertion (file) order, so wire-net construction in finishLoad -
	// and with it multi-driver resolution - is deterministic (#98, S1)
	private Set<Element> loadedElements = new LinkedHashSet<Element>(); // for loading
	private boolean deferredFinishReported = false; // draw-time finishLoad failure (#58)
																	// from file
	private Map<Integer, Element> elementMap = new HashMap<Integer, Element>(); // for
																				// loading
																				// from
																				// file
	private static int lineNumber; // to report errors when reading circuit file

	/**
	 * The newest save-format version this JLS can read (issue #79).
	 * Headerless legacy files are implicitly version 0; headered files
	 * begin with a "FORMAT n" line ahead of the top-level CIRCUIT line.
	 * Nested subcircuit blocks never carry a header - a file states its
	 * format version exactly once, at the top. A writer emits the
	 * highest version whose features the file actually uses (see
	 * {@link #formatVersionNeeded()}), so files that avoid newer
	 * features stay readable by older JLS versions. Version 2 adds
	 * vertical (UP/DOWN) orientation for Binder/Splitter (issue #124).
	 */
	public static final int FORMAT_VERSION = 2;

	/**
	 * Create a new, empty circuit.
	 * 
	 * @param name
	 *            The name of this circuit.
	 *
	 * @see jls.AllElementsRoundTripTest#load()
	 * @see jls.BatchSimulationGoldenTest#ramWriteStoresTheWord()
	 * @see jls.BatchSimulationGoldenTest#simulate()
	 * @see jls.BatchSimulationGoldenTest#watchedElementsPrintInNameOrder()
	 * @see jls.CircuitChangedFlagTest#clearChangedClearsTheFlag()
	 * @see jls.CircuitChangedFlagTest#newCircuitStartsUnchanged()
	 * @see jls.CircuitChangedFlagTest#serializationDoesNotClearChangedFlag()
	 * @see jls.CircuitLoadErrorTest#rejectAndGetError()
	 * @see jls.CircuitLoadErrorTest#unknownElementTypeIsRejected()
	 * @see jls.CircuitRoundTripTest#load()
	 * @see jls.CliTextSaveTest#theConvertedFileHoldsTheSameCircuit()
	 * @see jls.ContainerMutationFuzzTest#loadMutant()
	 * @see jls.ContainerMutationFuzzTest#theUnmutatedBaselinesActuallyLoad()
	 * @see jls.DeterministicSaveTest#load()
	 * @see jls.DrawCullingParityTest#tiledClippedDrawsMatchFullDraw()
	 * @see jls.ElementDrawSmokeTest#load()
	 * @see jls.ElementSimulationGoldenTest#simulate()
	 * @see jls.ElementSimulationGoldenTest#stopHaltsTheSimulationEarly()
	 * @see jls.FileAbstractorTest#aFreshCircuitDefaultsToTheXZContainer()
	 * @see jls.FileFormatSpecTest#load()
	 * @see jls.FileHandleReleaseTest#assertOpenReleasesTheFile()
	 * @see jls.FormatHeaderTest#failToLoad()
	 * @see jls.FormatHeaderTest#load()
	 * @see jls.GenerativeRoundTripFuzzTest#loadClassified()
	 * @see jls.LoadErrorReportingTest#loadErrorIsResetBetweenLoads()
	 * @see jls.LoadErrorReportingTest#midStreamIOExceptionIsReportedAsIOError()
	 * @see jls.LoadErrorReportingTest#reject()
	 * @see jls.LoadErrorReportingTest#unknownElementTypeReportsItsOwnError()
	 * @see jls.PrintPathSmokeTest#load()
	 * @see jls.SequentialGoldenTest#simulate()
	 * @see jls.SequentialGoldenTest#simulateWithVectors()
	 * @see jls.ShiftRegisterTest#load()
	 * @see jls.SimulationSemanticsRegressionTest#load()
	 * @see jls.SimulationSemanticsRegressionTest#triStateDoesNotRepostUnchangedOutputEvents()
	 * @see jls.SizeMeasurement#measure()
	 * @see jls.SpatialIndexTest#loadWired()
	 * @see jls.SpatialIndexTest#reportsIndexVsScanTiming()
	 * @see jls.StableElementIdTest#duplicateFileIdsAreRejected()
	 * @see jls.StableElementIdTest#load()
	 * @see jls.StableElementIdTest#malformedIdsAreClassifiedNotThrown()
	 * @see jls.StringEscapeRoundTripTest#roundTrip()
	 * @see jls.SvgExportTest#load()
	 * @see jls.UntrustedFileHardeningTest#rleIsBoundedByDeclaredCapacity()
	 * @see jls.UtilFunctionsTest#copyOfAPartialSelectionDropsDanglingWires()
	 * @see jls.UtilFunctionsTest#copyReproducesElementsWiresAndAttachment()
	 * @see jls.UtilFunctionsTest#partitionRebuildsWireNets()
	 * @see jls.UtilFunctionsTest#source()
	 * @see jls.VcdExportGoldenTest#load()
	 * @see jls.edit.CircuitSnapshotTest#load()
	 * @see jls.edit.CircuitSnapshotTest#snapshotIsCompact()
	 * @see jls.edit.CtrlWGestureTest#oneConstant()
	 * @see jls.edit.DragCandidateBoundTest#grid()
	 * @see jls.edit.SaveAsNameCheckTest#distinctSiblingWithSameNameIsStillACollision()
	 * @see jls.edit.SaveAsNameCheckTest#importedCircuitsAreSkipped()
	 * @see jls.edit.SaveAsNameCheckTest#savingUnderAnotherEditorsNameIsACollision()
	 * @see jls.edit.SaveAsNameCheckTest#savingUnderOwnCurrentNameIsNotACollision()
	 * @see jls.edit.SaveAsNameCheckTest#unusedNameIsNotACollision()
	 * @see jls.edit.TriStateBundleConnectTest#load()
	 * @see jls.edit.WireSweepSymmetryTest#landingOnAStationaryWireEndStillCollides()
	 * @see jls.edit.WireSweepSymmetryTest#wireCrossingWireStaysLegal()
	 * @see jls.edit.WireSweepSymmetryTest#withConstant()
	 * @see jls.elem.AttributePersistenceTest#load()
	 * @see jls.elem.DialogValidationTest#loadExpectingFailure()
	 * @see jls.elem.DialogValidationTest#stateMachineInitialStateRuleIsSharedWithSimStart()
	 * @see jls.elem.DisplayLegacyOrientTest#loadAndSaveElement()
	 * @see jls.elem.GroupOrientationTest#load()
	 * @see jls.elem.HollowVsFilledCollisionTest#build()
	 * @see jls.elem.JumpEndNoNamedWiresTest#circuitWithNamedWire()
	 * @see jls.elem.JumpEndNoNamedWiresTest#endGestureFailsFastWhenNoNamedWiresExist()
	 * @see jls.elem.MemoryInitEncodingTest#load()
	 * @see jls.elem.MuxSymbolTest#render()
	 * @see jls.elem.OrientationGeometryTest#describe()
	 * @see jls.elem.ParameterValidationTest#loadExpectingFailure()
	 * @see jls.elem.ParameterValidationTest#stateMachineWithoutInitialStateSurvivesSimInit()
	 * @see jls.elem.ParameterValidationTest#validValuesStillLoad()
	 * @see jls.hdl.HdlCircuitBuilder#load()
	 * @see jls.ui.DialogConstructionSmokeTest#constructAndCancel()
	 * @see jls.ui.EditorGestureSupport#EditorGestureSupport()
	 * @see jls.ui.EditorGestureTest#movingOneOfTwoElementsLeavesTheOtherPut()
	 * @see jls.ui.EditorGestureTest#oneGate()
	 * @see jls.ui.UiHarnessPilotTest#load()
	 */
	public Circuit(String name) {

		this.name = name;
	} // end of constructor

	/**
	 * Get the directory the circuit file is stored in.
	 * 
	 * @return the full path name of the directory.
	 */
	public String getDirectory() {

		return dir;
	} // end of getDirectory method

	/**
	 * Set the directory the circuit file is store in.
	 * 
	 * @param dir
	 *            The full path name of the directory.
	 */
	public void setDirectory(String dir) {

		this.dir = dir;
	} // end of setDirectory method

	/**
	 * Get the name of this circuit.
	 * 
	 * @return the name of this circuit.
	 *
	 * @see jls.FormatHeaderTest#headerlessLegacyTextStillLoads()
	 * @see jls.ui.CircuitAssert#assertElementPresent()
	 * @see jls.ui.EditorGestureSupport#EditorGestureSupport()
	 */
	public String getName() {

		return name;
	} // end of getFileName method

	/**
	 * Change name of this circuit.
	 * 
	 * @param name
	 *            New name.
	 */
	public void setName(String name) {

		this.name = name;
	} // end of setName method

	/**
	 * Get the on-disk container this circuit's saves are written in.
	 * XZ unless the user opted into plain text (issue #129).
	 *
	 * @return the save container.
	 *
	 * @see jls.FileAbstractorTest#aFreshCircuitDefaultsToTheXZContainer()
	 */
	public FileAbstractor.Container getSaveContainer() {

		return saveContainer;
	} // end of getSaveContainer method

	/**
	 * Change the on-disk container this circuit's saves are written in.
	 * Set from the Save As file-type choice (issue #129); the choice
	 * sticks for later plain Saves of the same circuit, so re-saving
	 * cannot silently re-wrap a version-controlled plain-text file.
	 *
	 * @param container
	 *            The container future saves will use.
	 */
	public void setSaveContainer(FileAbstractor.Container container) {

		this.saveContainer = container;
	} // end of setSaveContainer method

	/**
	 * Check if circuit has changed.
	 * 
	 * @return true if the circuit has changed, false if not.
	 *
	 * @see jls.CircuitChangedFlagTest#clearChangedClearsTheFlag()
	 * @see jls.CircuitChangedFlagTest#newCircuitStartsUnchanged()
	 * @see jls.CircuitChangedFlagTest#serializationDoesNotClearChangedFlag()
	 * @see jls.edit.CircuitSnapshotTest#capturePreservesTheChangedFlag()
	 */
	public boolean hasChanged() {

		return changed;
	} // end of hasChanged method

	/**
	 * Mark this circuit as changed. If it is a subcircuit, mark the circuit it
	 * is in as changed too. If a simulator is running, stop it.
	 *
	 * @see jls.CircuitChangedFlagTest#clearChangedClearsTheFlag()
	 * @see jls.CircuitChangedFlagTest#serializationDoesNotClearChangedFlag()
	 * @see jls.edit.CircuitSnapshotTest#capturePreservesTheChangedFlag()
	 */
	public void markChanged() {

		changed = true;
		index.invalidate();
		if (subElement != null) {
			subElement.getCircuit().markChanged();
		}
		if (JLSInfo.sim != null) {
			JLSInfo.sim.stop();
		}
	} // end of markChanged method

	/**
	 * Mark this circuit as saved. Called only after its serialized form has
	 * actually been written to disk, so a failed write keeps the
	 * unsaved-changes protection alive.
	 *
	 * @see jls.CircuitChangedFlagTest#clearChangedClearsTheFlag()
	 */
	public void clearChanged() {

		changed = false;
	} // end of clearChanged method

	/**
	 * Remove all elements from this circuit.
	 */
	public void clear() {

		elements.clear();
		namesUsed.clear();
		starts.clear();
		highlighted.clear();
		index.invalidate();
	} // end of clear method

	/**
	 * Add an element to this circuit.
	 * 
	 * @param el
	 *            The element to add.
	 *
	 * @see jls.edit.TriStateBundleConnectTest#freshEnd()
	 * @see jls.edit.WireSweepSymmetryTest#landingOnAStationaryWireEndStillCollides()
	 * @see jls.edit.WireSweepSymmetryTest#wire()
	 * @see jls.elem.HollowVsFilledCollisionTest#build()
	 * @see jls.elem.HollowVsFilledCollisionTest#corner()
	 * @see jls.elem.HollowVsFilledCollisionTest#edge()
	 */
	public void addElement(Element el) {

		elements.add(el);
		el.setCircuit(this);
		index.invalidate();
	} // end of addElement method

	/**
	 * Delete element from circuit. Do nothing if the element is not in the
	 * circuit.
	 * 
	 * @param el
	 *            The element to remove.
	 */
	public void remove(Element el) {

		elements.remove(el);
		highlighted.remove(el);
		index.invalidate();
	} // end of remove method

	/**
	 * Get the element set, as an unmodifiable view (#27): every caller
	 * iterates; mutation goes through addElement/remove so the circuit
	 * controls its own membership (and future indexes stay coherent).
	 *
	 * @see jls.AllElementsRoundTripTest#copyPreservesEverySavedAttribute()
	 * @see jls.BatchSimulationGoldenTest#ramWriteStoresTheWord()
	 * @see jls.BatchSimulationGoldenTest#simulate()
	 * @see jls.CircuitRoundTripTest#assertLoadsViaSniffer()
	 * @see jls.CircuitRoundTripTest#saveLoadIsAFixedPoint()
	 * @see jls.CliTextSaveTest#theConvertedFileHoldsTheSameCircuit()
	 * @see jls.DeterministicSaveTest#stateHashIsContentDetermined()
	 * @see jls.DrawCullingParityTest#culledCandidatesMatchFullScan()
	 * @see jls.ElementDrawSmokeTest#theFixtureCoversEveryDrawableElementType()
	 * @see jls.ElementSimulationGoldenTest#pinValue()
	 * @see jls.FormatHeaderTest#headeredTextLoads()
	 * @see jls.FormatHeaderTest#headerlessLegacyTextStillLoads()
	 * @see jls.ProofBridgeTest#a1IndexIntervalsAreNonEmpty()
	 * @see jls.ProofBridgeTest#a5DrawMarginAndMayBeVisibleMatchModel()
	 * @see jls.SequentialGoldenTest#simulate()
	 * @see jls.SequentialGoldenTest#simulateWithVectors()
	 * @see jls.ShiftRegisterTest#forkAuthoredCircuitLoadsWiresAndSimulatesEquivalently()
	 * @see jls.ShiftRegisterTest#pinValue()
	 * @see jls.SimulationSemanticsRegressionTest#find()
	 * @see jls.SimulationSemanticsRegressionTest#initInputsReachesInsideSubcircuits()
	 * @see jls.SimulationSemanticsRegressionTest#pinValue()
	 * @see jls.SpatialIndexTest#bruteForceNear()
	 * @see jls.SpatialIndexTest#everyContainingElementIsACandidate()
	 * @see jls.SpatialIndexTest#everyInsideElementIsACandidate()
	 * @see jls.SpatialIndexTest#queriesMatchBruteForceOnWiredCircuit()
	 * @see jls.SpatialIndexTest#reportsIndexVsScanTiming()
	 * @see jls.SpatialIndexTest#staysExactAfterMovesAndInvalidation()
	 * @see jls.StableElementIdTest#copyMintsAFreshId()
	 * @see jls.StableElementIdTest#idsAreUniqueWithinACircuit()
	 * @see jls.StableElementIdTest#mintedIdsSkipIdsTheFileAlreadyUses()
	 * @see jls.StableElementIdTest#sidsByLogicalElement()
	 * @see jls.StringEscapeRoundTripTest#roundTrip()
	 * @see jls.UtilFunctionsTest#copyOfAPartialSelectionDropsDanglingWires()
	 * @see jls.UtilFunctionsTest#copyReproducesElementsWiresAndAttachment()
	 * @see jls.UtilFunctionsTest#partitionRebuildsWireNets()
	 * @see jls.edit.CircuitSnapshotTest#captureRestoreReproducesTheCircuit()
	 * @see jls.edit.CircuitSnapshotTest#changedCircuitSnapshotsDifferently()
	 * @see jls.edit.CircuitSnapshotTest#snapshotIsCompact()
	 * @see jls.edit.CtrlWGestureTest#hoverSelect()
	 * @see jls.edit.CtrlWGestureTest#startWireClearsSelectionAndSelectsNewEnd()
	 * @see jls.edit.CtrlWGestureTest#startWireFromEmptySelectionMatchesOldBehavior()
	 * @see jls.edit.DragCandidateBoundTest#candidateSetPerNetEndIsBoundedIndependentOfCircuitSize()
	 * @see jls.edit.DragCandidateBoundTest#indexCandidatesFindExactlyTheSamePutsAsAFullScan()
	 * @see jls.edit.DragCandidateBoundTest#putLocations()
	 * @see jls.edit.TriStateBundleConnectTest#elementAt()
	 * @see jls.edit.WireSweepSymmetryTest#constantIn()
	 * @see jls.elem.AttributePersistenceTest#copyIsFieldEquivalent()
	 * @see jls.elem.AttributePersistenceTest#defaultsAreOmittedExactlyAsBefore()
	 * @see jls.elem.AttributePersistenceTest#legacyLongValueStillLoads()
	 * @see jls.elem.AttributePersistenceTest#savedBytesMatchTheHandwrittenFormat()
	 * @see jls.elem.DisplayLegacyOrientTest#loadAndSaveElement()
	 * @see jls.elem.GroupOrientationTest#group()
	 * @see jls.elem.HollowVsFilledCollisionTest#hollowInteriorDoesNotCollide()
	 * @see jls.elem.JumpEndNoNamedWiresTest#endGestureFailsFastWhenNoNamedWiresExist()
	 * @see jls.elem.MemoryInitEncodingTest#rleMemorySimulatesLikeRawMemory()
	 * @see jls.elem.MuxSymbolTest#render()
	 * @see jls.elem.OrientationGeometryTest#describe()
	 * @see jls.ui.CircuitAssert#assertElementPresent()
	 * @see jls.ui.CircuitAssert#reaches()
	 * @see jls.ui.EditorGestureTest#rightClickDeleteRemovesTheElement()
	 * @see jls.ui.EditorGestureTest#undoRestoresADeletedElementAndRedoRemovesItAgain()
	 */
	public Set<Element> getElements() {

		return Collections.unmodifiableSet(elements);
	} // end of getElements method

	/**
	 * Get the elements in the circuit's canonical order: sorted by
	 * stable id (#165/#166). {@link #getElements()} iterates in hash
	 * order, which depends on identity hashes and so varies between
	 * runs and machines; any consumer whose iteration order reaches
	 * observable output - the simulation event seed (#181), the
	 * printed page sequence (#182) - must iterate this list instead,
	 * so the output is a pure function of circuit content.
	 *
	 * @return a fresh list of every element, sorted by stable id.
	 *
	 * @see jls.PrintPageOrderTest#bookedPagesFollowStableIdOrder()
	 * @see jls.SimulationSeedOrderTest#stableOrderIsSortedByStableId()
	 * @see jls.SimulationSeedOrderTest#initSimIsSeededInStableIdOrder()
	 */
	public java.util.List<Element> getElementsInStableOrder() {

		java.util.List<Element> ordered =
				new java.util.ArrayList<Element>(elements);
		ordered.sort(java.util.Comparator.comparing(Element::getStableId));
		return ordered;
	} // end of getElementsInStableOrder method

	/**
	 * Mark the spatial index stale after a geometry change the incremental
	 * paths don't cover (rotate, flip, size change, aborted move). The next
	 * spatial query rebuilds it in one pass.
	 *
	 * @see jls.SpatialIndexTest#staysExactAfterMovesAndInvalidation()
	 */
	public void invalidateIndex() {

		index.invalidate();
	} // end of invalidateIndex method

	/**
	 * Keep the spatial index current for elements just moved by a drag,
	 * including wires whose bounds follow a moved wire end. No-op when a
	 * rebuild is already pending. O(moved), which is what makes drag events
	 * independent of circuit size (#17).
	 *
	 * @param moved The elements the editor just moved.
	 *
	 * @see jls.DrawCullingParityTest#culledCandidatesMatchFullScan()
	 * @see jls.SpatialIndexTest#staysExactAfterMovesAndInvalidation()
	 */
	public void reindexAfterMove(Set<Element> moved) {

		if (index.isDirty()) {
			return;
		}
		for (Element el : moved) {
			reindexMoved(el);

			// moving a logic element drags its attached wire ends along
			// (LogicElement.move), so their bounds changed too
			for (jls.elem.Put put : el.getAllPuts()) {
				WireEnd end = put.getWireEnd();
				if (end != null) {
					reindexMoved(end);
				}
			}
		}
	} // end of reindexAfterMove method

	/**
	 * Refresh one moved element in the index, along with the wires whose
	 * bounds follow it if it is a wire end.
	 */
	private void reindexMoved(Element el) {

		if (elements.contains(el)) {
			index.update(el);
		}
		if (el instanceof WireEnd) {
			for (Wire wire : ((WireEnd) el).getWires()) {
				if (elements.contains(wire)) {
					index.update(wire);
				}
			}
		}
	} // end of reindexMoved method

	/**
	 * All elements whose bounds may intersect or touch the given rectangle
	 * (a superset: callers apply their exact predicates). Replaces
	 * full-circuit scans in per-mouse-event paths (#3, #17).
	 *
	 * @param rect The query rectangle.
	 *
	 * @return the candidate elements.
	 *
	 * @see jls.DrawCullingParityTest#culledCandidatesMatchFullScan()
	 * @see jls.SpatialIndexTest#assertQueryParity()
	 * @see jls.SpatialIndexTest#everyInsideElementIsACandidate()
	 * @see jls.SpatialIndexTest#staysExactAfterMovesAndInvalidation()
	 * @see jls.elem.HollowVsFilledCollisionTest#diagonalWireIsCandidateButNotCollisionOffTheDiagonal()
	 * @see jls.elem.HollowVsFilledCollisionTest#indexShortlistsMatchOutlineGeometry()
	 */
	public Set<Element> elementsNear(Rectangle rect) {

		if (index.isDirty()) {
			index.rebuild(elements);
		}
		return index.query(rect);
	} // end of elementsNear method

	/**
	 * All elements whose bounds come within the snap spacing of a point —
	 * a superset of every element whose contains(x,y) can be true,
	 * including wires' half-spacing tolerance around their segment.
	 *
	 * @param x The x-coordinate of the point.
	 * @param y The y-coordinate of the point.
	 *
	 * @return the candidate elements.
	 *
	 * @see jls.SpatialIndexTest#everyContainingElementIsACandidate()
	 * @see jls.SpatialIndexTest#reportsIndexVsScanTiming()
	 * @see jls.edit.DragCandidateBoundTest#indexCandidatesFindExactlyTheSamePutsAsAFullScan()
	 * @see jls.edit.DragCandidateBoundTest#worstCandidateCount()
	 */
	public Set<Element> elementsAt(int x, int y) {

		int pad = JLSInfo.spacing;
		return elementsNear(new Rectangle(x - pad, y - pad, 2 * pad, 2 * pad));
	} // end of elementsAt method

	/**
	 * Track highlight state so "unhighlight everything" is O(highlighted),
	 * not O(circuit). Called by Element.setHighlight.
	 *
	 * @param el The element whose highlight changed.
	 * @param light The new highlight state.
	 */
	public void noteHighlight(Element el, boolean light) {

		if (light) {
			highlighted.add(el);
		} else {
			highlighted.remove(el);
		}
	} // end of noteHighlight method

	/**
	 * A snapshot of the currently highlighted elements (safe to unhighlight
	 * while iterating).
	 *
	 * @return the highlighted elements at this moment.
	 *
	 * @see jls.edit.CtrlWGestureTest#startWireClearsSelectionAndSelectsNewEnd()
	 */
	public Set<Element> getHighlighted() {

		return new HashSet<Element>(highlighted);
	} // end of getHighlighted method

	/**
	 * Load circuit from file.
	 * 
	 * @param input
	 *            A scanner to read with.
	 * 
	 * @return false if there were problems, true if load was successful.
	 *
	 * @see jls.AllElementsRoundTripTest#load()
	 * @see jls.BatchSimulationGoldenTest#ramWriteStoresTheWord()
	 * @see jls.BatchSimulationGoldenTest#simulate()
	 * @see jls.BatchSimulationGoldenTest#watchedElementsPrintInNameOrder()
	 * @see jls.CircuitLoadErrorTest#rejectAndGetError()
	 * @see jls.CircuitLoadErrorTest#unknownElementTypeIsRejected()
	 * @see jls.CircuitRoundTripTest#load()
	 * @see jls.CliTextSaveTest#theConvertedFileHoldsTheSameCircuit()
	 * @see jls.ContainerMutationFuzzTest#loadMutant()
	 * @see jls.ContainerMutationFuzzTest#theUnmutatedBaselinesActuallyLoad()
	 * @see jls.DeterministicSaveTest#load()
	 * @see jls.DrawCullingParityTest#tiledClippedDrawsMatchFullDraw()
	 * @see jls.ElementDrawSmokeTest#load()
	 * @see jls.ElementSimulationGoldenTest#simulate()
	 * @see jls.ElementSimulationGoldenTest#stopHaltsTheSimulationEarly()
	 * @see jls.FileFormatSpecTest#load()
	 * @see jls.FileHandleReleaseTest#assertOpenReleasesTheFile()
	 * @see jls.FormatHeaderTest#failToLoad()
	 * @see jls.FormatHeaderTest#load()
	 * @see jls.GenerativeRoundTripFuzzTest#loadClassified()
	 * @see jls.LoadErrorReportingTest#loadErrorIsResetBetweenLoads()
	 * @see jls.LoadErrorReportingTest#midStreamIOExceptionIsReportedAsIOError()
	 * @see jls.LoadErrorReportingTest#reject()
	 * @see jls.LoadErrorReportingTest#unknownElementTypeReportsItsOwnError()
	 * @see jls.PrintPathSmokeTest#load()
	 * @see jls.SequentialGoldenTest#simulate()
	 * @see jls.SequentialGoldenTest#simulateWithVectors()
	 * @see jls.ShiftRegisterTest#load()
	 * @see jls.SimulationSemanticsRegressionTest#load()
	 * @see jls.SizeMeasurement#measure()
	 * @see jls.SpatialIndexTest#loadWired()
	 * @see jls.SpatialIndexTest#reportsIndexVsScanTiming()
	 * @see jls.StableElementIdTest#duplicateFileIdsAreRejected()
	 * @see jls.StableElementIdTest#load()
	 * @see jls.StableElementIdTest#malformedIdsAreClassifiedNotThrown()
	 * @see jls.StringEscapeRoundTripTest#roundTrip()
	 * @see jls.SvgExportTest#load()
	 * @see jls.UntrustedFileHardeningTest#rleIsBoundedByDeclaredCapacity()
	 * @see jls.UtilFunctionsTest#source()
	 * @see jls.VcdExportGoldenTest#load()
	 * @see jls.edit.CircuitSnapshotTest#load()
	 * @see jls.edit.CircuitSnapshotTest#snapshotIsCompact()
	 * @see jls.edit.CtrlWGestureTest#oneConstant()
	 * @see jls.edit.DragCandidateBoundTest#grid()
	 * @see jls.edit.TriStateBundleConnectTest#load()
	 * @see jls.edit.WireSweepSymmetryTest#withConstant()
	 * @see jls.elem.AttributePersistenceTest#load()
	 * @see jls.elem.DialogValidationTest#loadExpectingFailure()
	 * @see jls.elem.DisplayLegacyOrientTest#loadAndSaveElement()
	 * @see jls.elem.GroupOrientationTest#load()
	 * @see jls.elem.JumpEndNoNamedWiresTest#circuitWithNamedWire()
	 * @see jls.elem.MemoryInitEncodingTest#load()
	 * @see jls.elem.MuxSymbolTest#render()
	 * @see jls.elem.OrientationGeometryTest#describe()
	 * @see jls.elem.ParameterValidationTest#loadExpectingFailure()
	 * @see jls.elem.ParameterValidationTest#validValuesStillLoad()
	 * @see jls.hdl.HdlCircuitBuilder#load()
	 * @see jls.ui.EditorGestureTest#movingOneOfTwoElementsLeavesTheOtherPut()
	 * @see jls.ui.EditorGestureTest#oneGate()
	 * @see jls.ui.UiHarnessPilotTest#load()
	 */
	public boolean load(Scanner input) {

		// a fresh load must not report a previous load's failure (#58)
		JLSInfo.setLoadError(null);
		lineNumber = 1;
		boolean ok = readFormatHeader(input) && loadCircuit(input);
		if (!ok) {
			// Scanner swallows IOException and presents it as end of
			// input; distinguish a truncated/corrupted stream from a
			// short-but-well-formed file (#58)
			IOException ioex = input.ioException();
			if (ioex != null) {
				failLoad(LoadError.Category.IO_ERROR,
						"reading was interrupted by an I/O error ("
								+ ioex.getMessage() + ")",
						"The file or disk may be damaged - try copying "
								+ "the file again, or recover from the "
								+ ".jls~ checkpoint or a backup.");
			}
		}
		return ok;
	} // end of load method

	/**
	 * Consume the optional FORMAT version header at the top of a file
	 * (issue #79). Legacy files have no header and are implicitly format
	 * version 0; current saves begin with a FORMAT line declaring the
	 * version their features need (see
	 * {@link #formatVersionNeeded()}). A version newer
	 * than {@link #FORMAT_VERSION} is refused with an explicit
	 * needs-a-newer-JLS error (#58 taxonomy) rather than a misparse.
	 * Only the true top of a file may carry the header - nested
	 * subcircuit CIRCUIT blocks are read elsewhere and never see one.
	 *
	 * @param input The scanner to read with.
	 *
	 * @return true if there is no header or a usable one was consumed,
	 *         false (with the load error set) if the header is present
	 *         but truncated, malformed, or declares a newer version.
	 */
	private static boolean readFormatHeader(Scanner input) {

		if (!input.hasNext("FORMAT")) {
			return true; // headerless legacy file (implicit version 0)
		}
		input.next();
		if (!input.hasNext()) {
			return failLoad(LoadError.Category.MALFORMED,
					"the FORMAT header has no version number after it",
					TRUNCATED_HINT);
		}
		if (!input.hasNextInt()) {
			if (input.hasNextBigInteger()) {
				// a numeric version too large for an int is certainly
				// newer than anything this JLS knows
				return failLoad(LoadError.Category.NEWER_FORMAT,
						"this file is save-format version " + input.next()
								+ ", but this version of JLS only reads up "
								+ "to version " + FORMAT_VERSION,
						NEWER_FORMAT_HINT);
			}
			return failLoad(LoadError.Category.MALFORMED,
					"the FORMAT header version '" + input.next()
							+ "' is not a number",
					NOT_JLS_HINT);
		}
		int version = input.nextInt();
		if (version < 0) {
			return failLoad(LoadError.Category.MALFORMED,
					"the FORMAT header version " + version
							+ " is not a valid format version",
					NOT_JLS_HINT);
		}
		if (version > FORMAT_VERSION) {
			return failLoad(LoadError.Category.NEWER_FORMAT,
					"this file is save-format version " + version
							+ ", but this version of JLS only reads up to "
							+ "version " + FORMAT_VERSION,
					NEWER_FORMAT_HINT);
		}
		lineNumber += 1;
		return true;
	} // end of readFormatHeader method

	/** Next-step hint when the file declares a newer format version. */
	private static final String NEWER_FORMAT_HINT =
			"The circuit was saved by a newer version of JLS - upgrade "
					+ "JLS to open this file.";

	/** Next-step hint for failures that smell like a cut-off file. */
	private static final String TRUNCATED_HINT =
			"The file may be truncated - recover from the .jls~ "
					+ "checkpoint or a backup, or re-save the circuit "
					+ "from a working copy.";

	/** Next-step hint for content that was never a JLS save. */
	private static final String NOT_JLS_HINT =
			"Make sure you opened a .jls circuit file saved by JLS, "
					+ "not some other kind of file.";

	/**
	 * Report a load failure at the current line, with no element context
	 * (issue #58 addendum: category + location + detail + next step).
	 *
	 * @param category Which kind of failure this is.
	 * @param detail   What went wrong, in words.
	 * @param hint     One actionable next-step sentence, or null.
	 *
	 * @return false, so callers can 'return failLoad(...)'.
	 */
	private static boolean failLoad(LoadError.Category category,
			String detail, String hint) {

		JLSInfo.setLoadError(
				new LoadError(category, detail, lineNumber, null, hint));
		return false;
	} // end of failLoad method

	/**
	 * Report a load failure at the current line inside a specific element.
	 *
	 * @param category Which kind of failure this is.
	 * @param el       The element being read.
	 * @param detail   What went wrong, in words.
	 * @param hint     One actionable next-step sentence, or null.
	 *
	 * @return false, so callers can 'return failLoad(...)'.
	 */
	private static boolean failLoad(LoadError.Category category, Element el,
			String detail, String hint) {

		JLSInfo.setLoadError(new LoadError(category, detail, lineNumber,
				describe(el), hint));
		return false;
	} // end of failLoad method

	/**
	 * Describe an element for an error message: "'name' (Type)" when the
	 * element is named, otherwise just its type.
	 *
	 * @param el The element.
	 *
	 * @return the description.
	 */
	private static String describe(Element el) {

		String type = el.getClass().getSimpleName();
		String elName = el.getName();
		if (elName == null || elName.isEmpty())
			return type;
		return "'" + elName + "' (" + type + ")";
	} // end of describe method

	/**
	 * Load circuit from file without resetting the line number counter,
	 * for the recursive call.
	 *
	 * @param input
	 *            A scanner to read with.
	 *
	 * @return false if there were problems, true if load was successful.
	 */
	private boolean loadCircuit(Scanner input) {

		// catch all exceptions, assume there is a problem with the circuit file
		try {

			// read header
			if (!input.hasNext()) {
				return failLoad(LoadError.Category.NOT_A_CIRCUIT,
						"the file is empty - there is no header to read",
						NOT_JLS_HINT);
			}
			String str = input.next();
			if (!str.equals("CIRCUIT")) {
				return failLoad(LoadError.Category.NOT_A_CIRCUIT,
						"the file does not start with CIRCUIT, so it is "
								+ "not a JLS circuit save",
						NOT_JLS_HINT);
			}
			if (!input.hasNext()) {
				return failLoad(LoadError.Category.MALFORMED,
						"the CIRCUIT header has no circuit name after it",
						TRUNCATED_HINT);
			}

			// ignore name if not a subcircuit
			if (name.isEmpty())
				name = input.next();
			else
				input.next();

			// read circuit and get basic info for each element
			lineNumber += 1;
			while (input.hasNext()) {
				str = input.next();

				// if end of top level circuit
				if (str.equals("ENDCIRCUIT")) {
					lineNumber += 1;
					return true;
				}

				// should be the beginning of an(other) element
				if (!str.equals("ELEMENT")) {
					return failLoad(LoadError.Category.MALFORMED,
							"expected ELEMENT or ENDCIRCUIT here, but "
									+ "found '" + str + "'",
							TRUNCATED_HINT);
				}

				// the next input should exist (element type)
				if (!input.hasNext()) {
					return failLoad(LoadError.Category.MALFORMED,
							"the file ends where an element type name "
									+ "should be",
							TRUNCATED_HINT);
				}

				// get element type and create the element through the
				// registry (issue #78): the descriptor's factory replaces
				// the historical Class.forName reflection, so a tag no
				// longer has to name a class (aliases can preserve
				// renamed tags, #79) and every non-element class in
				// jls.elem is simply not a tag
				String elementType = input.next();
				ElementType descriptor = ElementRegistry.forTag(elementType);
				if (descriptor == null) {
					return failLoad(LoadError.Category.UNKNOWN_ELEMENT,
							"this version of JLS has no element type "
									+ "named '" + elementType + "'",
							"The circuit may have been saved by a newer "
									+ "version of JLS - upgrade JLS, or "
									+ "remove the unrecognized element "
									+ "from the file.");
				}
				Element newElement = null;
				try {
					newElement = descriptor.create(this);
				} catch (RuntimeException ex) {
					// the element's own constructor threw; its message
					// (never its class name or stack, #58 P6) is the why
					return failLoad(LoadError.Category.ELEMENT_ERROR,
							"creating an element of type '" + elementType
									+ "' failed"
									+ (ex.getMessage() == null ? ""
											: ": " + ex.getMessage()),
							"Fix that element's values in the file, or "
									+ "re-save the circuit from JLS.");
				}
				lineNumber += 1;
				boolean loadOK = loadElement(newElement, input);
				if (loadOK) {
					loadedElements.add(newElement);
				} else {
					// loadElement already reported the specific failure
					return false;
				}
			}
			return failLoad(LoadError.Category.MALFORMED,
					"the file ends before the ENDCIRCUIT trailer",
					TRUNCATED_HINT);
		} catch (Exception ex) {
			// the stack trace goes to stderr for debugging; the user
			// message must never show one (#58 P6)
			ex.printStackTrace();
			return failLoad(LoadError.Category.MALFORMED,
					"an unexpected problem stopped the load"
							+ (ex.getMessage() == null ? ""
									: ": " + ex.getMessage()),
					TRUNCATED_HINT);
		} catch (Error er) {
			return failLoad(LoadError.Category.MALFORMED,
					"an unexpected problem stopped the load"
							+ (er.getMessage() == null ? ""
									: ": " + er.getMessage()),
					TRUNCATED_HINT);
		}
	} // end of loadCircuit method

	/**
	 * Load an element by reading all of its instance variable values.
	 * 
	 * @param el
	 *            An empty object to load.
	 * 
	 * @return false if the file is not in the right format, true if it is.
	 */
	public boolean loadElement(Element el, Scanner input) {
		try {
			return loadElementItems(el, input);
		} catch (IllegalArgumentException ex) {
			// a parameter failed the element's own validation (issue
			// #52); the constraint text is the why, and the element and
			// line say where (#58)
			return failLoad(LoadError.Category.ELEMENT_ERROR, el,
					ex.getMessage() == null
							? "an attribute value was rejected"
							: ex.getMessage(),
					"Fix that value in the file, or re-create the "
							+ "element in JLS and save again.");
		}
	} // end of loadElement method

	/**
	 * Read the attribute items of one element until its END line.
	 *
	 * @param el    The element being loaded.
	 * @param input The scanner to read with.
	 *
	 * @return false if the file is not in the right format, true if it is.
	 */
	private boolean loadElementItems(Element el, Scanner input) {
		while (input.hasNext()) {
			if (input.hasNext("CIRCUIT")) {
				if (!(el instanceof SubCircuit)) {
					return failLoad(LoadError.Category.MALFORMED, el,
							"a nested CIRCUIT appears here, but only a "
									+ "SubCircuit element may contain one",
							NOT_JLS_HINT);
				}
				Circuit subCirc = new Circuit("");
				if (!subCirc.loadCircuit(input)) {
					// the subcircuit's load already reported the
					// specific failure - keep it (#58)
					return false;
				}
				SubCircuit sub = (SubCircuit) el;
				subCirc.setImported(sub);
				sub.setSubCircuit(subCirc);
				sub.setName(subCirc.getName());
				addName(subCirc.getName());
				try {
					if (!subCirc.finishLoad(null)) {
						// finishLoad reported the failure; add which
						// subcircuit it was in if it did not say
						if (JLSInfo.lastLoadError == null) {
							failLoad(LoadError.Category.ELEMENT_ERROR, el,
									"subcircuit " + subCirc.getName()
											+ " failed to finish loading",
									TRUNCATED_HINT);
						}
						return false;
					}
				} catch (Exception e) {
					// a broken subcircuit must fail the parent load, not
					// report success with a stack trace on stderr (#58)
					e.printStackTrace();
					return failLoad(LoadError.Category.ELEMENT_ERROR, el,
							"subcircuit " + subCirc.getName()
									+ " failed to finish loading"
									+ (e.getMessage() == null ? ""
											: ": " + e.getMessage()),
							TRUNCATED_HINT);
				}
				continue;
			}
			String type = input.next();
			if (type.equals("END")) {
				lineNumber += 1;
				return true;
			}
			if (type.equals("int")) {
				if (!input.hasNext()) {
					return truncatedInElement(el, type);
				}
				String name = input.next();
				if (!input.hasNextInt()) {
					return badValueInElement(el, name, "an integer");
				}
				int value = input.nextInt();
				if (name.equals("id")) {
					elementMap.put(value, el);
				}
				el.setValue(name, value);
				lineNumber += 1;
			} else if (type.equals("long")) {
				if (!input.hasNext()) {
					return truncatedInElement(el, type);
				}
				String name = input.next();
				if (!input.hasNextLong()) {
					return badValueInElement(el, name, "an integer");
				}
				long value = input.nextLong();
				el.setValue(name, value);
				lineNumber += 1;
			} else if (type.equals("Int")) { // BigInteger
				if (!input.hasNext()) {
					return truncatedInElement(el, type);
				}
				String name = input.next();
				if (!input.hasNextBigInteger()) {
					return badValueInElement(el, name, "an integer");
				}
				BigInteger value = input.nextBigInteger();
				el.setValue(name, value);
				lineNumber += 1;
			} else if (type.equals("String")) {
				if (!input.hasNext()) {
					return truncatedInElement(el, type);
				}
				String name = input.next();
				String pattern = ".*";
				String raw = input.findInLine(pattern);
				if (raw == null) {
					return truncatedInElement(el, type);
				}
				String value = unquoteAndUnescape(raw);
				if (value == null) {
					return badValueInElement(el, name, "a quoted string");
				}
				el.setValue(name, value);
				lineNumber += 1;
			} else if (type.equals("ref")) {
				if (!input.hasNext()) {
					return truncatedInElement(el, type);
				}
				String name = input.next();
				if (!input.hasNextInt()) {
					return badValueInElement(el, name, "an integer");
				}
				int value = input.nextInt();
				el.setValue(name, value);
				lineNumber += 1;
			} else if (type.equals("pair")) {
				if (!input.hasNextInt()) {
					return badValueInElement(el, type, "two integers");
				}
				int v1 = input.nextInt();
				if (!input.hasNextInt()) {
					return badValueInElement(el, type, "two integers");
				}
				int v2 = input.nextInt();
				el.setPair(v1, v2);
				lineNumber += 1;
			} else if (type.equals("probe")) {
				if (!input.hasNextInt()) {
					return badValueInElement(el, type, "an integer");
				}
				int id = input.nextInt();
				String pattern = ".*";
				String raw = input.findInLine(pattern);
				if (raw == null) {
					return truncatedInElement(el, type);
				}
				String value = unquoteAndUnescape(raw);
				if (value == null) {
					return badValueInElement(el, type, "a quoted string");
				}
				if (!(el instanceof WireEnd)) {
					return failLoad(LoadError.Category.MALFORMED, el,
							"a probe entry appears here, but probes can "
									+ "only be attached to wire ends",
							NOT_JLS_HINT);
				}
				WireEnd end = (WireEnd) el;
				end.setProbe(id, value);
				lineNumber += 1;
			} else {
				return failLoad(LoadError.Category.MALFORMED, el,
						"'" + type + "' is not a kind of attribute JLS "
								+ "knows how to read",
						TRUNCATED_HINT);
			}
		}
		return failLoad(LoadError.Category.MALFORMED, el,
				"the file ends in the middle of this element (its END "
						+ "line is missing)",
				TRUNCATED_HINT);
	} // end of loadElementItems method

	/**
	 * Report a file that stops in the middle of an element attribute.
	 *
	 * @param el   The element being read.
	 * @param item The attribute kind being read when input ran out.
	 *
	 * @return false, so callers can 'return truncatedInElement(...)'.
	 */
	private static boolean truncatedInElement(Element el, String item) {

		return failLoad(LoadError.Category.MALFORMED, el,
				"the file ends in the middle of "
						+ (item == null || item.isEmpty() ? "an attribute"
								: "a '" + item + "' attribute"),
				TRUNCATED_HINT);
	} // end of truncatedInElement method

	/**
	 * Report an attribute whose value is not of the required kind.
	 *
	 * @param el       The element being read.
	 * @param name     The attribute (or item kind) with the bad value.
	 * @param expected What the value should have been, e.g. "an integer".
	 *
	 * @return false, so callers can 'return badValueInElement(...)'.
	 */
	private static boolean badValueInElement(Element el, String name,
			String expected) {

		return failLoad(LoadError.Category.MALFORMED, el,
				"the value of '" + name + "' should be " + expected
						+ ", but what follows is not",
				TRUNCATED_HINT);
	} // end of badValueInElement method

	/**
	 * Extract and decode a quoted string value from the rest of a saved
	 * line. The writer (Attribute.StringAttribute.save) escapes backslash,
	 * quote and newline as two-character sequences; a single left-to-right
	 * scan is its exact inverse. Sequential replace() passes were not (issue
	 * #53): they collapsed a literal backslash-n into a real newline and
	 * corrupted trailing backslashes.
	 *
	 * @param raw The rest of the line, including the surrounding quotes.
	 *
	 * @return the decoded string, or null if no quoted value is present.
	 */
	private static String unquoteAndUnescape(String raw) {

		int start = raw.indexOf('"');
		int end = raw.lastIndexOf('"');
		if (start < 0 || end <= start)
			return null;
		String escaped = raw.substring(start + 1, end);
		StringBuilder value = new StringBuilder(escaped.length());
		for (int i = 0; i < escaped.length(); i += 1) {
			char ch = escaped.charAt(i);
			if (ch == '\\' && i + 1 < escaped.length()) {
				char next = escaped.charAt(i + 1);
				if (next == 'n') {
					value.append('\n');
					i += 1;
				} else if (next == '\\' || next == '"') {
					value.append(next);
					i += 1;
				} else {
					// not a writer-produced escape; keep it verbatim
					value.append(ch);
				}
			} else {
				value.append(ch);
			}
		}
		return value.toString();
	} // end of unquoteAndUnescape method

	/**
	 * Finish load of circuit.
	 * 
	 * @param g
	 *            The Graphics object to use.
	 * 
	 * @return false if any exceptions occur
	 * @throws Exception
	 *
	 * @see jls.AllElementsRoundTripTest#load()
	 * @see jls.BatchSimulationGoldenTest#ramWriteStoresTheWord()
	 * @see jls.BatchSimulationGoldenTest#simulate()
	 * @see jls.BatchSimulationGoldenTest#watchedElementsPrintInNameOrder()
	 * @see jls.CircuitRoundTripTest#load()
	 * @see jls.CliTextSaveTest#theConvertedFileHoldsTheSameCircuit()
	 * @see jls.ContainerMutationFuzzTest#loadMutant()
	 * @see jls.ContainerMutationFuzzTest#theUnmutatedBaselinesActuallyLoad()
	 * @see jls.DeterministicSaveTest#load()
	 * @see jls.DrawCullingParityTest#tiledClippedDrawsMatchFullDraw()
	 * @see jls.ElementDrawSmokeTest#load()
	 * @see jls.ElementSimulationGoldenTest#simulate()
	 * @see jls.ElementSimulationGoldenTest#stopHaltsTheSimulationEarly()
	 * @see jls.FileFormatSpecTest#load()
	 * @see jls.FileHandleReleaseTest#assertOpenReleasesTheFile()
	 * @see jls.FormatHeaderTest#load()
	 * @see jls.GenerativeRoundTripFuzzTest#loadClassified()
	 * @see jls.PrintPathSmokeTest#load()
	 * @see jls.SequentialGoldenTest#simulate()
	 * @see jls.SequentialGoldenTest#simulateWithVectors()
	 * @see jls.ShiftRegisterTest#load()
	 * @see jls.SimulationSemanticsRegressionTest#load()
	 * @see jls.SizeMeasurement#measure()
	 * @see jls.SpatialIndexTest#loadWired()
	 * @see jls.SpatialIndexTest#reportsIndexVsScanTiming()
	 * @see jls.StableElementIdTest#duplicateFileIdsAreRejected()
	 * @see jls.StableElementIdTest#load()
	 * @see jls.StringEscapeRoundTripTest#roundTrip()
	 * @see jls.SvgExportTest#load()
	 * @see jls.UtilFunctionsTest#source()
	 * @see jls.VcdExportGoldenTest#load()
	 * @see jls.edit.CircuitSnapshotTest#load()
	 * @see jls.edit.CircuitSnapshotTest#snapshotIsCompact()
	 * @see jls.edit.CtrlWGestureTest#oneConstant()
	 * @see jls.edit.DragCandidateBoundTest#grid()
	 * @see jls.edit.TriStateBundleConnectTest#load()
	 * @see jls.edit.WireSweepSymmetryTest#withConstant()
	 * @see jls.elem.AttributePersistenceTest#load()
	 * @see jls.elem.DisplayLegacyOrientTest#loadAndSaveElement()
	 * @see jls.elem.GroupOrientationTest#load()
	 * @see jls.elem.JumpEndNoNamedWiresTest#circuitWithNamedWire()
	 * @see jls.elem.MemoryInitEncodingTest#load()
	 * @see jls.elem.MuxSymbolTest#render()
	 * @see jls.elem.OrientationGeometryTest#describe()
	 * @see jls.hdl.HdlCircuitBuilder#load()
	 * @see jls.ui.EditorGestureTest#movingOneOfTwoElementsLeavesTheOtherPut()
	 * @see jls.ui.EditorGestureTest#oneGate()
	 * @see jls.ui.UiHarnessPilotTest#load()
	 */
	public boolean finishLoad(Graphics g) throws Exception {

		// if any exceptions, assume load problem
		try {

			// stable identity (#165): file-declared stable ids must be
			// unique within the circuit, and elements from files that
			// predate stable ids get one minted deterministically - in
			// file order, which is itself deterministic (#98), so two
			// loads of the same file always agree
			Set<jls.elem.ElementId> usedIds = new HashSet<jls.elem.ElementId>();
			for (Element el : loadedElements) {
				if (el.hasFileStableId() && !usedIds.add(el.getStableId())) {
					JLSInfo.setLoadError(LoadError.of(
							LoadError.Category.ELEMENT_ERROR,
							"two elements declare the same stable id '"
									+ el.getStableId() + "'",
							"Remove the duplicated sid line from the file, "
									+ "or re-save the circuit from JLS."));
					return false;
				}
			}
			long nextLegacy = 0;
			for (Element el : loadedElements) {
				if (el.hasFileStableId()) {
					continue;
				}
				jls.elem.ElementId minted;
				do {
					minted = jls.elem.ElementId.legacy(nextLegacy);
					nextLegacy += 1;
				} while (usedIds.contains(minted));
				usedIds.add(minted);
				el.assignLegacyStableId(minted);
			}

			// finish up non-wire ends first
			for (Element el : loadedElements) {
				if (el instanceof WireEnd)
					continue;
				el.init(g);
				elements.add(el);
			}

			// finish up wire ends
			LinkedList<WireEnd> ends = new LinkedList<WireEnd>();
			for (Element el : loadedElements) {
				if (!(el instanceof WireEnd))
					continue;
				WireEnd end = (WireEnd) el;
				end.init(this);
				elements.add(end);
				ends.add(end);
			}

			// partition ends into wire nets
			while (!ends.isEmpty()) {

				// start visit list and new wire net
				LinkedList<WireEnd> visit = new LinkedList<WireEnd>();
				WireEnd end = ends.remove();
				visit.add(end);
				WireNet net = new WireNet();

				// visit ends in visit list until empty
				Set<WireEnd> visited = new HashSet<WireEnd>();
				while (!visit.isEmpty()) {

					// get wire end, add to wire net
					WireEnd vend = visit.remove();
					ends.remove(vend);
					visited.add(vend);
					net.add(vend);
					vend.setNet(net);
					if (vend.isLoadTriState()) {
						net.loadTriState();
					}
					if (vend.isAttached()) {
						net.setBits(vend.getPut().getBits());
						if (vend.getPut() instanceof Output) {
							net.setInput();
						}
					}

					// add wires to wire net and add opposite wire ends to visit
					// list
					for (Wire wire : vend.getWires()) {
						WireEnd otherEnd = wire.getOtherEnd(vend);
						if (!visited.contains(otherEnd)) {
							visit.add(otherEnd);
							net.add(wire);
							wire.setNet(net);
						}
					}
				}
			}

			loadedElements.clear();
			// the id map is only needed while wire ends resolve their
			// refs above; keeping it pinned every loaded element (#51)
			elementMap.clear();
		} catch (Exception ex) {
			// stack trace to stderr only; never in the user message (#58 P6)
			ex.printStackTrace();
			JLSInfo.setLoadError(LoadError.of(
					LoadError.Category.ELEMENT_ERROR,
					"the circuit could not be assembled after reading"
							+ (ex.getMessage() == null ? ""
									: ": " + ex.getMessage()),
					TRUNCATED_HINT));
			return false;
		} catch (Error er) {
			JLSInfo.setLoadError(LoadError.of(
					LoadError.Category.ELEMENT_ERROR,
					"the circuit could not be assembled after reading"
							+ (er.getMessage() == null ? ""
									: ": " + er.getMessage()),
					TRUNCATED_HINT));
			return false;
		}
		return true;

	} // end of finishLoad method

	/**
	 * Get the smallest rectangle containing all the elements in the circuit.
	 * 
	 * @return the smallest rectangle.
	 */
	public Rectangle getBounds() {

		Rectangle rect = new Rectangle();
		boolean firstTime = true;
		for (Element el : elements) {
			if (el instanceof Wire)
				continue;
			if (firstTime) {
				rect = el.getRect();
				firstTime = false;
			} else {
				rect.add(el.getRect());
			}
		}
		return rect;
	} // end of getBounds method

	/**
	 * Save circuit in file.
	 * 
	 * @param output
	 *            The file to write to.
	 *
	 * @see jls.AllElementsRoundTripTest#save()
	 * @see jls.CircuitChangedFlagTest#serialize()
	 * @see jls.CircuitRoundTripTest#save()
	 * @see jls.DeterministicSaveTest#canonicalBytesAreIdenticalWhateverThePlatformNewline()
	 * @see jls.DeterministicSaveTest#save()
	 * @see jls.FileFormatSpecTest#save()
	 * @see jls.FormatHeaderTest#save()
	 * @see jls.GenerativeRoundTripFuzzTest#save()
	 * @see jls.SizeMeasurement#measure()
	 * @see jls.StableElementIdTest#save()
	 * @see jls.edit.CircuitSnapshotTest#save()
	 * @see jls.elem.GroupOrientationTest#save()
	 * @see jls.elem.MemoryInitEncodingTest#save()
	 */
	public void save(PrintWriter output) {

		// canonical newlines (#111, #166): println follows the platform
		// line separator, but canonical bytes must be identical on every
		// OS - a circuit saved on Windows must byte-match the same
		// circuit saved on Linux, or determinism (and stateHash) would
		// be platform-dependent. Wrap the writer so every println, here
		// and in every element save method, terminates lines with '\n'.
		PrintWriter out = canonicalNewlines(output);

		// write header; a file-level save states the format version once
		// at the top (issue #79) - nested subcircuit blocks, which are
		// always saved through their imported circuit, never repeat it
		if (isImported()) {
			out.println("CIRCUIT " + subElement.getName());
		} else {
			out.println("FORMAT " + formatVersionNeeded());
			out.println("CIRCUIT " + name);
		}

		// canonical save order (#166): elements sorted by stable id,
		// wires - which are reconstructed from refs and save nothing -
		// after every saved block. The sequential file-local ids are
		// assigned in that same order, so id and ref lines depend only
		// on circuit content: two circuits with identical content save
		// byte-identically, whatever their load/edit history.
		java.util.List<Element> ordered =
				new java.util.ArrayList<Element>(elements);
		ordered.sort(java.util.Comparator
				.comparingInt((Element el) -> el instanceof Wire ? 1 : 0)
				.thenComparing(Element::getStableId));

		// give each element a unique id
		int id = 0;
		for (Element el : ordered) {
			el.setID(id);
			id += 1;
		}

		// save elements
		for (Element el : ordered) {
			el.save(out);
		}

		// write trailer
		out.println("ENDCIRCUIT");
		out.flush();
	} // end of save method

	/**
	 * Wrap a writer so println terminates lines with '\n' whatever the
	 * platform line separator is. PrintWriter(Writer) adds no buffering,
	 * so writes pass straight through to the wrapped writer.
	 */
	private static PrintWriter canonicalNewlines(PrintWriter output) {

		return new PrintWriter(output) {
			/**
			 * Terminate each println with a canonical '\n' instead of the
			 * platform line separator.
			 */
			@Override
			public void println() {
				write('\n');
			}
		};
	} // end of canonicalNewlines method

	/**
	 * A hash of this circuit's canonical serialized form (#166): equal
	 * for any two circuits with identical content, whatever their
	 * load/edit history. The convergence oracle and sync indicator for
	 * collaborative editing (#163).
	 *
	 * @return the SHA-256 of the canonical save text, in lowercase hex.
	 *
	 * @see jls.DeterministicSaveTest#stateHashIsContentDetermined()
	 */
	public String stateHash() {

		java.io.StringWriter text = new java.io.StringWriter();
		try (PrintWriter out = new PrintWriter(text)) {
			save(out);
		}
		java.security.MessageDigest sha;
		try {
			sha = java.security.MessageDigest.getInstance("SHA-256");
		} catch (java.security.NoSuchAlgorithmException ex) {
			// every JRE ships SHA-256 (it is required by the platform spec)
			throw new AssertionError(ex);
		}
		byte[] digest = sha.digest(text.toString()
				.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		StringBuilder hex = new StringBuilder(digest.length * 2);
		for (byte b : digest) {
			hex.append(Character.forDigit((b >> 4) & 0xf, 16));
			hex.append(Character.forDigit(b & 0xf, 16));
		}
		return hex.toString();
	} // end of stateHash method

	/**
	 * The save-format version a save of this circuit must declare: the
	 * highest version any of its elements (or, through SubCircuit,
	 * nested circuits) requires - see the docs/file-format.md section 9
	 * evolution policy. Files that use no post-version-1 feature keep
	 * declaring "FORMAT 1" so older JLS versions can still read them.
	 *
	 * @return the format version a save of this circuit declares.
	 */
	public int formatVersionNeeded() {

		int version = 1;
		for (Element el : elements) {
			version = Math.max(version, el.saveFormatVersion());
		}
		return version;
	} // end of formatVersionNeeded method

	/**
	 * Draw the circuit by drawing every element. First the set of elements not
	 * in the second set are drawn, then the ones in the second set are drawn.
	 * Wires are drawn first in each set.
	 *
	 * @param g
	 *            The graphics object to draw with.
	 * @param second
	 *            The second set of elements to draw.
	 * @param ed
	 *            The editor window doing the drawing.
	 * @throws Exception
	 *
	 * @see jls.DrawCullingParityTest#tiledClippedDrawsMatchFullDraw()
	 */
	public void draw(Graphics g, Set<Element> second, SimpleEditor ed)
			throws Exception {

		// finish up loading process if necessary
		if (loadedElements.size() > 0) {
			if (!finishLoad(g)) {
				// report once instead of silently re-failing on every
				// repaint (#58)
				if (!deferredFinishReported) {
					deferredFinishReported = true;
					System.err.println("deferred finishLoad of circuit "
							+ name + " failed: " + JLSInfo.loadError);
				}
			}

			// set circuit size to the largest of the default area or the needed
			// area
			Rectangle rect = new Rectangle(0, 0, JLSInfo.circuitsize,
					JLSInfo.circuitsize);
			rect.add(getBounds());
			if (ed != null) {
				ed.setCircuitSize(rect.getSize());
			}
		}

		// partition into draw layers in one pass instead of four full
		// scans (#27 S3): wires under non-wires, the second (selected)
		// set on top of both. Elements far outside the clip cannot be
		// visible and are skipped, so a scrolled view pays for what it
		// shows, not for the whole circuit (#17). The candidates come
		// from the spatial index, not a full scan, so a dirty-region
		// repaint during a drag costs O(visible), not O(circuit); the
		// query pads the clip by the same margin mayBeVisible allows
		// for labels, so its exact check below accepts the same
		// elements a full scan would. That parity is machine-checked:
		// THEOREM 2 (culling-parity) in
		// proofs/SpatialIndexCorrectness.agda, with the margin/grow/
		// intersects assumptions pinned by jls.ProofBridgeTest.
		Rectangle clip = g.getClipBounds();
		java.util.Collection<Element> candidates;
		if (clip == null) {
			candidates = elements;
		} else {
			Rectangle query = new Rectangle(clip);
			query.grow(DRAW_MARGIN, DRAW_MARGIN);
			candidates = elementsNear(query);
		}
		java.util.List<Element> wires = new java.util.ArrayList<Element>();
		java.util.List<Element> parts = new java.util.ArrayList<Element>();
		java.util.List<Element> secondWires = new java.util.ArrayList<Element>();
		java.util.List<Element> secondParts = new java.util.ArrayList<Element>();
		for (Element el : candidates) {
			if (clip != null && !mayBeVisible(el, clip)) {
				continue;
			}
			if (el instanceof Wire) {
				(second.contains(el) ? secondWires : wires).add(el);
			} else {
				(second.contains(el) ? secondParts : parts).add(el);
			}
		}
		for (Element el : wires) {
			el.draw(g);
		}
		for (Element el : parts) {
			el.draw(g);
		}
		for (Element el : secondWires) {
			el.draw(g);
		}
		for (Element el : secondParts) {
			el.draw(g);
		}
	} // end of draw method

	/**
	 * How far outside its index bounds an element may draw (labels and
	 * similar decorations). Draw culling pads by this margin on both the
	 * index query and the exact visibility check.
	 */
	private static final int DRAW_MARGIN = 8 * JLSInfo.spacing;

	/**
	 * Whether an element could draw inside the clip. The margin generously
	 * covers labels drawn near (but outside) an element's bounds.
	 */
	private static boolean mayBeVisible(Element el, Rectangle clip) {

		Rectangle b = el.getIndexBounds();
		b.grow(DRAW_MARGIN, DRAW_MARGIN);
		return b.intersects(clip);
	} // end of mayBeVisible method

	/**
	 * Print the circuit.
	 * 
	 * @param g
	 *            The graphics object to use
	 * @param format
	 *            Page format info.
	 * @param pagenum
	 *            Ignored.
	 * 
	 * @return Printable.PAGE_EXISTS.
	 *
	 * @see jls.PrintPathSmokeTest#printingTheCircuitDirectlyRenders()
	 */
	@Override
	public int print(Graphics g, PageFormat format, int pagenum) {

		// use better graphics
		Graphics2D gg = (Graphics2D) g;

		// construct name
		Circuit c = this;
		String nm = name;
		while (c.isImported()) {
			c = c.getSubElement().getCircuit();
			nm += " in " + c.getName();
		}

		// set up
		FontMetrics fm = gg.getFontMetrics();
		int ascent = fm.getAscent();
		int descent = fm.getDescent();
		int fontHeight = ascent + descent;

		// get bounds of actual circuit
		Rectangle rect = getBounds();

		// translate to page area
		double width = format.getImageableWidth();
		double height = format.getImageableHeight();
		gg.translate(format.getImageableX(), format.getImageableY());

		// draw title
		gg.drawString(nm, 0, ascent);

		// translate and scale to fit circuit to remaining page area
		gg.translate(0, fontHeight * 2);
		height -= fontHeight * 2;
		double scale = 1.0;
		if (rect.width > width) {
			scale = width / rect.width;
		}
		if (rect.height + JLSInfo.pointDiameter > height) {
			scale = Math.min(scale, height
					/ (rect.height + JLSInfo.pointDiameter));
		}
		gg.scale(scale, scale);
		gg.translate(-rect.x + JLSInfo.pointDiameter / 2, -rect.y
				+ JLSInfo.pointDiameter / 2);

		// print
		try {
			draw(gg, new HashSet<Element>(), null);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Printable.PAGE_EXISTS;
	} // end of print method

	/**
	 * Add this circuit to the print book, add any of its state machines, truth
	 * tables and all subcircuits.
	 * 
	 * @param book
	 *            The book to add to.
	 * @param format
	 *            The page format to use.
	 *
	 * @see jls.PrintPathSmokeTest#everyBookedPagePrintsIntoAGraphics()
	 */
	public void addToBook(Book book, PageFormat format) {

		// add this circuit
		book.append(this, format);

		// canonical page order (#182): iterating the element HashSet
		// would order the pages by identity hash, which varies between
		// runs - two prints of one circuit could page differently
		List<Element> ordered = getElementsInStableOrder();

		// add state machines
		for (Element el : ordered) {
			if (el instanceof StateMachine) {
				StateMachine sm = (StateMachine) el;
				book.append(sm, format);
				Printable p = sm.makeOutSum();
				if (p != null)
					book.append(p, format);
			}
		}

		// add truth tables
		for (Element el : ordered) {
			if (el instanceof TruthTable) {
				TruthTable tt = (TruthTable) el;
				book.append(tt, format);
			}
		}

		// add subcircuits
		for (Element el : ordered) {
			if (el instanceof SubCircuit) {
				((SubCircuit) el).getSubCircuit().addToBook(book, format);
			}
		}
	} // end of addToBook method

	/**
	 * Export an image of the circuit.
	 *
	 * @param file
	 *            The name of the file to write to.
	 * @throws Exception
	 *
	 * @see jls.ElementDrawSmokeTest#everyElementDrawsOnTheRasterExportPath()
	 * @see jls.ElementDrawSmokeTest#everyElementDrawsOnTheSvgExportPath()
	 * @see jls.SvgExportTest#exportingTwiceIsByteIdentical()
	 * @see jls.SvgExportTest#theDocumentIsAnSvgImageWithDrawnContent()
	 */
	public void exportImage(String file) throws Exception {

		// get bounds of actual circuit
		Rectangle rect = getBounds();

		// add 10 pixels on all edges
		int border = 10;
		rect = new Rectangle(rect.x - border, rect.y - border, rect.width + 2
				* border, rect.height + 2 * border);

		// vector export (issue #154): the same element paint path that
		// fills a bitmap below draws into JFreeSVG's Graphics2D instead,
		// so .svg output needs no per-element work
		if (file.toLowerCase(java.util.Locale.ROOT).endsWith(".svg")) {
			org.jfree.svg.SVGGraphics2D svg =
					new org.jfree.svg.SVGGraphics2D(rect.width, rect.height);
			// a fixed defs prefix keeps two exports of the same circuit
			// byte-identical (the default prefix is instance-derived)
			svg.setDefsKeyPrefix("jls");
			AffineTransform svgTranslate = new AffineTransform();
			svgTranslate.translate(-rect.x, -rect.y);
			svg.setTransform(svgTranslate);
			svg.setColor(Color.white);
			svg.fill(rect);
			// draw in a deterministic order: elements live in a
			// HashSet, and while raster export doesn't care (same
			// pixels either way, overlaps aside), SVG serializes the
			// draw order into the file - an unstable order would break
			// byte-identical goldens across load instances. Wires
			// under non-wires, like the interactive draw path.
			java.util.List<Element> wireLayer = new java.util.ArrayList<Element>();
			java.util.List<Element> partLayer = new java.util.ArrayList<Element>();
			for (Element el : elements) {
				(el instanceof jls.elem.Wire ? wireLayer : partLayer).add(el);
			}
			// order on index bounds, not x/y: wires keep x/y at their
			// defaults, but their bounds are derived from their ends
			java.util.Comparator<Element> drawOrder = java.util.Comparator
					.comparingInt((Element el) -> el.getIndexBounds().x)
					.thenComparingInt(el -> el.getIndexBounds().y)
					.thenComparingInt(el -> el.getIndexBounds().width)
					.thenComparingInt(el -> el.getIndexBounds().height)
					.thenComparing(el -> el.getClass().getName())
					.thenComparingInt(Element::getID);
			wireLayer.sort(drawOrder);
			partLayer.sort(drawOrder);
			for (Element el : wireLayer) {
				el.draw(svg);
			}
			for (Element el : partLayer) {
				el.draw(svg);
			}
			try {
				java.nio.file.Files.writeString(java.nio.file.Path.of(file),
						svg.getSVGDocument(),
						java.nio.charset.StandardCharsets.UTF_8);
			} finally {
				svg.dispose();
			}
			return;
		}

		// set up image
		BufferedImage image = new BufferedImage(rect.width, rect.height,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		AffineTransform translate = new AffineTransform();
		translate.translate(-rect.x, -rect.y);
		g.setTransform(translate);

		// draw the image
		g.setColor(Color.white);
		g.fill(rect);
		draw(g, new HashSet<Element>(), null);

		// write the image, the format following the file extension
		// (issue #71): .png produces PNG, anything else the legacy JPEG
		try {
			String format = file.toLowerCase(java.util.Locale.ROOT)
					.endsWith(".png") ? "png" : "jpg";
			if (!ImageIO.write(image, format, new File(file))) {
				throw new IOException("no " + format + " image writer available");
			}
		} finally {
			// clean up
			g.dispose();
			image.flush();
		}

	} // end of print method

	/**
	 * Untouch inputs and outputs of all logic elements.
	 */
	public void untouchPuts() {

		for (Element el : elements) {
			el.untouchPuts();
		}
	} // end of untouchPuts method

	/**
	 * Get an element from the load map.
	 * 
	 * @param id
	 *            The id of the element.
	 * 
	 * @return the element with the given id, or null if not in the map.
	 */
	public Element getElementByID(int id) {

		return elementMap.get(id);
	} // end of getElementByID method

	/**
	 * Add a name to the list of names used. If already used in the list, don't
	 * add it.
	 * 
	 * @param name
	 *            The new name.
	 * 
	 * @return false if the name is already in the list, true if not.
	 */
	public boolean addName(String name) {

		if (namesUsed.contains(name))
			return false;
		namesUsed.add(name);
		return true;
	} // end of addName method

	/**
	 * See if this circuit already has an element with a given name.
	 * 
	 * @param name
	 *            The name to check for.
	 * 
	 * @return true if the name is already used, false if not.
	 */
	public boolean hasName(String name) {

		return namesUsed.contains(name);
	} // end of hasName method

	/**
	 * Remove a name from the list of names used. Do nothing if not there to
	 * start with.
	 * 
	 * @param name
	 *            The name to remove.
	 */
	public void removeName(String name) {

		namesUsed.remove(name);
	} // end of removeName method

	/**
	 * Set that this circuit is an imported circuit. This means it cannot be
	 * saved in a file and that pins cannot be added or removed.
	 * 
	 * @param sub
	 *            The SubCircuit element in the main circuit that refers to this
	 *            subcircuit.
	 *
	 * @see jls.edit.SaveAsNameCheckTest#importedCircuitsAreSkipped()
	 */
	public void setImported(SubCircuit sub) {

		subElement = sub;
	} // end of setImported method

	/**
	 * See if this is an imported circuit.
	 * 
	 * @return true if it is imported, false otherwise.
	 */
	public boolean isImported() {

		return subElement != null;
	} // end of isImported method

	/**
	 * Get the SubCircuit element referring to this circuit.
	 * 
	 * @return the element.
	 */
	public SubCircuit getSubElement() {

		return subElement;
	} // end of getSubElement method

	/**
	 * Remove all probes from this circuit and all subcircuits.
	 */
	public void removeAllProbes() {

		for (Element el : elements) {
			if (el instanceof Wire) {
				Wire wire = (Wire) el;
				wire.removeProbe();
			} else if (el instanceof SubCircuit) {
				SubCircuit sub = (SubCircuit) el;
				sub.getSubCircuit().removeAllProbes();
			}
		}
	} // end of removeAllProbes

	/**
	 * Turn off watches in all elements in this circuit and all subcircuits.
	 */
	public void clearAllWatches() {

		for (Element el : elements) {
			if (el instanceof SubCircuit) {
				SubCircuit sub = (SubCircuit) el;
				sub.getSubCircuit().clearAllWatches();
			} else {
				if (!el.isUneditable())
					el.setWatched(false);
			}
		}
	} // end of clearAllWatches method

	/**
	 * Reset all propagation delays to their defaults.
	 */
	public void resetAllDelays() {

		for (Element el : elements) {
			if (!(el instanceof LogicElement))
				continue;
			if (el.isUneditable())
				continue;
			LogicElement logic = (LogicElement) el;
			logic.resetPropDelay();
		}
	} // end of resetAllDelays method

	/**
	 * Add a jumpstart to the list of jumpstarts in this circuit. If there is
	 * already one with the given name, do not add it.
	 * 
	 * @param name
	 *            The name of the jumpstart.
	 * @param start
	 *            The jumpstart object.
	 * 
	 * @return false if there already is a jumpstart with the given name, true
	 *         otherwise.
	 */
	public boolean addJumpStart(String name, JumpStart start) {

		if (starts.containsKey(name))
			return false;
		starts.put(name, start);
		return true;
	} // end of addJumpStart method

	/**
	 * Get the jumpstart with the given name.
	 * 
	 * @param name
	 *            The name of the desired jumpstart.
	 * 
	 * @return the jumpstart, or null if it no jumpstart with the given name
	 *         exists.
	 */
	public JumpStart getJumpStart(String name) {

		return starts.get(name);
	} // end of getJumpStart method

	/**
	 * Get all jump start names in alphabetical order.
	 * 
	 * @return the starts.
	 */
	public Set<String> getJumpStartNames() {

		// a copy: handing out the live keySet let callers (or their
		// iteration) corrupt the jump-start map (issue #51)
		return new TreeSet<String>(starts.keySet());
	} // end of getJumpStartNames method

	/**
	 * Remove a jump start from the list.
	 * 
	 * @param name
	 *            The name of this jump start.
	 */
	public void removeJumpStart(String name) {

		starts.remove(name);
	} // end of removeJumpStart

	/**
	 * Set the editor of this circuit.
	 * 
	 * @param ed
	 *            The current editor, or null to indicate not being edited.
	 */
	public void setEditor(Editor ed) {

		editor = ed;
	} // end of setEditor method

	/**
	 * Get the editor of this circuit.
	 * 
	 * @return The current editor, or null if not being edited.
	 */
	public Editor getEditor() {

		return editor;
	} // end of getEditor method

	/**
	 * Get the current line number of the loaded circuit file. Used when an
	 * error occurs so a meaningful error message can be printed.
	 */
	public int getLineNumber() {

		return lineNumber;
	} // end of getLineNumber method

	/**
	 * For debugging, return name and super.toString
	 * 
	 * @return string version of this circuit.
	 */
	@Override
	public String toString() {

		return name + "(" + super.toString() + ")";
	} // end of toString method


} // end of Circuit class
