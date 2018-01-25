package org.teamtators.common.scheduler;

/**
 * The different states that the robot can be in
 *
 * @author Alex Mikhalev
 */
public enum RobotState {
    /**
     * The initial state, where nothing can be moving.
     */
    DISABLED,
    /**
     * Robot is executing autonomous actions.
     */
    AUTONOMOUS,
    /**
     * Robot is being controlled by the drivers.
     */
    TELEOP,
    /**
     * Robot is being tested (not used during matches)
     */
    TEST
}
