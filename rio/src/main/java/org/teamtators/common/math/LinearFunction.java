package org.teamtators.common.math;

public class LinearFunction implements DoubleFunction {
    public double m;
    public double b;

    @Override
    public double calculate(double x) {
        return m * x + b;
    }
}
