package org.teamtators.common.math;

/**
 * A function in the form a*x^b
 */
public class PowerFunction implements DoubleFunction {
    public double a = 0.0;
    public double b = 0.0;

    public double calculate(double x) {
        return a * Math.pow(x, b);
    }
}
