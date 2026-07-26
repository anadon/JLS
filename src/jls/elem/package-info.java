/**
 * Defines the circuit element model for JLS: the building blocks a user places
 * on a schematic and that the simulator evaluates. Every component derives from
 * {@link jls.elem.Element}, with {@link jls.elem.LogicElement} as the base for
 * active, signal-processing parts; concrete types include logic gates
 * (AND, OR, NOT, XOR, ...), wiring ({@link jls.elem.Wire},
 * {@link jls.elem.WireEnd}, {@link jls.elem.WireNet}), input/output pins,
 * arithmetic and storage units (adders, registers, memory, shift registers),
 * multiplexers, decoders, splitters, tri-state buffers, clocks, displays,
 * state machines, and nested {@link jls.elem.SubCircuit}s. Each element knows
 * how to draw itself, edit its properties, persist to and load from file, and
 * react to signal changes during simulation.
 *
 * <p>Null-marked (issue #93): every reference in this package is
 * non-null unless annotated { }, and NullAway enforces
 * the contract at compile time on the default build.</p>
 */
@NullMarked
package jls.elem;

import org.jspecify.annotations.NullMarked;
