/**
 * Test suite for the top-level {@code jls} production package - the
 * application's core model, entry points, and persistence layer. These
 * tests exercise the {@code Circuit} model and its save/load pipeline
 * ({@code FileAbstractor}, {@code LoadError}, the text/zip/xz container
 * formats), asserting that save-load-save round-trips are byte-stable
 * fixed points across every element type. They cover the command-line
 * interface contract (usage diagnostics, exit codes, headless image and
 * text export) and pin simulation, VCD, and sequential-circuit behavior
 * against golden references. The package also hosts executable
 * architecture ratchets that guard the emerging headless core - keeping
 * the circuit model, simulation, and persistence free of AWT, Swing, and
 * editor dependencies - plus generative and fuzz round-trip harnesses
 * that stress the format against randomized circuits.
 */
package jls;
