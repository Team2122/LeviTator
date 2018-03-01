package org.teamtators.common.math;

public class LinearInterpolationFunction implements DoubleFunction {
    public double minX;
    public double maxX;
    public double minY;
    public double maxY;

    @Override
    public double calculate(double x) {
        if (x < minX) {
            return minY;
        } else if (x > maxX) {
            return maxY;
        }
        double slope = (maxY - minY) / (maxX - minX);
        return slope * (x - minX) + minY;
    }
}
