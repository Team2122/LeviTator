package org.teamtators.common.scheduler;

/**
 * Interface for things that listen for robot state changes.
 *
 * @author Alex Mikhalev
 */
public interface RobotStateListener {
    /**
     * Called when the robot enters state
     *
     * @param state The state the robot entered
     */
    void onEnterRobotState(RobotState state);
}
