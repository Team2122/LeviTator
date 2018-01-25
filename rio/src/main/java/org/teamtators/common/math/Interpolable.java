package org.teamtators.common.math;

/**
 * @author Alex Mikhalev
 */
public interface Interpolable<T extends Interpolable<T>> {
    T interpolate(T other, double x);
}
