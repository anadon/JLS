/**
 * Schematic auto-layout for imported netlists (issue #62): the fixed
 * interface Stage 2 HDL import (issue #61) codes against, plus the
 * JLS-side geometry contract and quality harness that any layout
 * engine must satisfy. {@link jls.hdl.layout.LayoutGraph} carries the
 * placement problem (elements with per-port pixel offsets, point-to-
 * point connections grouped into nets, register feedback marked as
 * back-edges); a {@link jls.hdl.layout.SchematicLayouter} produces a
 * {@link jls.hdl.layout.LayoutResult} of grid positions and orthogonal
 * wire waypoints; {@link jls.hdl.layout.LayoutInvariants} checks the
 * hard drawing invariants (12-px grid, routes anchored at exact port
 * offsets, orthogonal segments, non-overlapping element bodies); and
 * {@link jls.hdl.layout.LayoutMetrics} computes the quantified
 * readability rubric fixed in the issue-62 addendum (zero overlaps,
 * crossings per net, Manhattan wire-length ratio, left-to-right flow).
 *
 * <p>Engine decision, adjudicated on the issue 2026-07-17: the layout
 * engine is an out-of-process ELK Layered runner (a separate process
 * exchanging layout JSON is license-clean mere aggregation even while
 * ELK is EPL-2.0-only; in-process linking becomes possible if ELK
 * 0.12.0 ships its merged GPLv3 secondary license). A hand-rolled
 * Sugiyama layouter is explicitly out of scope. Everything in this
 * package is engine-neutral so that runner - or any future engine -
 * plugs in behind {@link jls.hdl.layout.SchematicLayouter} without
 * importer changes.</p>
 */
package jls.hdl.layout;
