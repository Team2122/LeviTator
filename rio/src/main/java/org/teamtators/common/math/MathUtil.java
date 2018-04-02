package org.teamtators.common.math;

/**
 * @author Alex Mikhalev
 */
public class MathUtil {
    public static double applyLimits(double value, double min, double max) {
        if (value > max) return max;
        else if (value < min) return min;
        else return value;
    }
}
