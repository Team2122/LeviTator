package org.teamtators.common.math;

/**
 * @author Alex Mikhalev
 */
public class InterpolableDouble implements Interpolable<InterpolableDouble>, InverseInterpolable<InterpolableDouble>,
        Comparable<InterpolableDouble> {
    private double value;

    private InterpolableDouble(double value) {
        this.value = value;
    }

    public static InterpolableDouble of(double value) {
        return new InterpolableDouble(value);
    }

    public double get() {
        return value;
    }

    public double set(double value) {
        return value;
    }

    @Override
    public InterpolableDouble interpolate(InterpolableDouble other, double x) {
        double m = other.value - value;
        double y = m * x + value;
        return new InterpolableDouble(y);
    }

    @Override
    public double inverseInterpolate(InterpolableDouble upper, InterpolableDouble query) {
        double total_dx = upper.value - value;
        double query_dx = query.value - value;
        return query_dx / total_dx;
    }

    @Override
    public int compareTo(InterpolableDouble o) {
        return Double.compare(value, o.value);
    }
}
