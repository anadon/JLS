package jls.core;

/**
 * An exact integer box size on the snap grid (issue #77): a headless
 * value type replacing {@link java.awt.Dimension} in the grid-geometry
 * transforms ({@link GridTransform}), so element geometry can be
 * computed without an AWT dependency. Component names mirror
 * {@code Dimension}'s fields, so {@code .width}/{@code .height} reads
 * become {@code .width()}/{@code .height()}.
 *
 * @param width the box width, in pixels.
 * @param height the box height, in pixels.
 */
public record GridSize(int width, int height) {
} // end of GridSize record
