/**
 * Tests for the Stage 2 Yosys-netlist importer (issue #61): the
 * cell-to-element mapper and plain-text save-format emitter in
 * {@link jls.hdl.imp}. The tests are fixture-driven and tool-free -
 * committed JSON netlists are imported and the emitted text is loaded
 * through JLS's real loader - so they run in any environment; the
 * external-Yosys leg is exercised separately by the CI import job.
 */
package jls.hdl.imp;
