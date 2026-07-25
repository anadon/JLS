package jls.core;

/**
 * An exact integer point on the snap grid (issue #77): a headless value
 * type replacing {@code Point} in the grid-geometry transforms
 * ({@link Geometry}), so element geometry can be computed without
 * an AWT dependency. Component names mirror {@code Point}'s fields, so
 * {@code .x}/{@code .y} reads become {@code .x()}/{@code .y()}.
 *
 * @param x the x offset, in pixels.
 * @param y the y offset, in pixels.
 */
public record GridPoint(int x, int y) {
} // end of GridPoint record
