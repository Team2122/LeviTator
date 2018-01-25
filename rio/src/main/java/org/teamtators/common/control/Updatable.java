package org.teamtators.common.control;

/**
 * Represents things that can be updated at a certain period
 *
 * @author Alex Mikhalev
 */
public interface Updatable {
    /**
     * Updates this item
     *
     * @param delta The time, in seconds, since the last update or since updating was started. May be a very small
     *              number.
     */
    void update(double delta);

    default String getName() {
        return "Updatable";
    }
}
