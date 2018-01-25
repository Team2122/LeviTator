package org.teamtators.common.math;

/**
 * @author Alex Mikhalev
 */
public interface InverseInterpolable<T extends InverseInterpolable<T>> {
    double inverseInterpolate(T upper, T query);
}
