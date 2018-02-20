package org.teamtators.common.math;

import java.util.Map;

/**
 * @author Alex Mikhalev
 */
public class PiecewiseLinear implements DoubleFunction {
    private InterpolableTreeMap<InterpolableDouble, InterpolableDouble> points = new InterpolableTreeMap<>();

    public double calculate(double x) {
        return points.getInterpolated(InterpolableDouble.of(x))
                .map(InterpolableDouble::get)
                .orElse(0.0);
    }

    public void configure(Map<Double, Double> points) {
        this.points.clear();
        for (Map.Entry<Double, Double> entry : points.entrySet()) {
            this.points.put(InterpolableDouble.of(entry.getKey()), InterpolableDouble.of(entry.getValue()));
        }
    }
}
