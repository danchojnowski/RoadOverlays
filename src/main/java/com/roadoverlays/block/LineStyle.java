package com.roadoverlays.block;

/** Stripe sizes match the source straight-line paint textures. */
public enum LineStyle {
    SINGLE(1.0 / 16, 0),
    DOUBLE(1.0 / 16, 2.0 / 16),
    THICK(2.0 / 16, 0),
    DOUBLE_THICK(2.0 / 16, 3.0 / 16);

    private final double halfWidth;
    private final double halfSpacing;

    LineStyle(double halfWidth, double halfSpacing) {
        this.halfWidth = halfWidth;
        this.halfSpacing = halfSpacing;
    }

    public double halfWidth() {
        return halfWidth;
    }

    public double halfSpacing() {
        return halfSpacing;
    }
}
