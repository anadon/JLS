/**
 * Tests for the schematic auto-layout seam (issue #62): graph and
 * result construction rules, the drawing-invariant checks that gate
 * realization as elements and WireEnds, and the quantified readability
 * rubric fixed in the issue's addendum. No layout engine exists here
 * yet - these pin the contract the out-of-process ELK runner (and the
 * Stage 2 importer, issue #61) will be built against.
 */
package jls.hdl.layout;
