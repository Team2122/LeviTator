package org.teamtators.common.scheduler;

import org.teamtators.common.util.FMSData;

/**
 * Interface for things that listen for FMS data
 *
 * @author Avery Bainbridge
 */
public interface FMSDataListener {
    /**
     * Called when the robot receives FMS data
     *
     * @param data New FMS data
     */
    void onFMSData(FMSData data);
}
