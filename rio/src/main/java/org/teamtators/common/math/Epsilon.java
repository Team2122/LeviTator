package org.teamtators.common.math;

/**
 * For comparing doubles within a certain error value
 * @author Alex Mikhalev
 */
public class Epsilon {
    /**
     * The default acceptable error for double comparisons
     */
    public static final double EPSILON = 1E-9;

    public static boolean isEpsilonEqual(double a, double b, double epsilon) {
        return Math.abs(a - b) <= epsilon;
    }

    public static boolean isEpsilonEqual(double a, double b) {
        return isEpsilonEqual(a, b, EPSILON);
    }

    public static boolean isEpsilonZero(double a, double epsilon) {
        return Math.abs(a) <= epsilon;
    }

    public static boolean isEpsilonZero(double a) {
        return isEpsilonZero(a, EPSILON);
    }

    public static boolean isEpsilonGreaterThan(double a, double b, double epsilon) {
        return a - b > epsilon;
    }

    public static boolean isEpsilonGreaterThan(double a, double b) {
        return isEpsilonGreaterThan(a, b, EPSILON);
    }

    public static boolean isEpsilonLessThan(double a, double b, double epsilon) {
        return b - a > epsilon;
    }

    public static boolean isEpsilonLessThan(double a, double b) {
        return isEpsilonLessThan(a, b, EPSILON);
    }

    public static boolean isEpsilonPositive(double a, double epsilon) {
        return a >= -epsilon;
    }

    public static boolean isEpsilonPositive(double a) {
        return isEpsilonPositive(a, EPSILON);
    }

    public static boolean isEpsilonNegative(double a, double epsilon) {
        return a <= epsilon;
    }

    public static boolean isEpsilonNegative(double a) {
        return isEpsilonNegative(a, EPSILON);
    }
}
