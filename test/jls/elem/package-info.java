/**
 * Test suite for the {@code jls.elem} production package, which defines the
 * circuit elements JLS simulates (gates, memories, multiplexers, clocks,
 * tri-state buffers, constants, and the shared {@link jls.elem.Element}
 * base). These tests pin the behaviors that make elements safe to save,
 * load, copy, and simulate: byte-exact attribute persistence and loader
 * round-tripping (including the run-length memory-init encoding), rejection
 * of invalid element parameters at load time, orientation-aware bounding
 * box and connector geometry across every orientation, and the constructor
 * contract the circuit loader relies on to instantiate elements by
 * reflection. Many cases are regression baselines tied to specific issues
 * and audit findings, guarding against silent drift when the corresponding
 * production element code is refactored.
 */
package jls.elem;
