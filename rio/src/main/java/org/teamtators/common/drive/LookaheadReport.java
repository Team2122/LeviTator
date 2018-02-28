package org.teamtators.common.drive;

import org.teamtators.common.control.TrapezoidalProfile;
import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Rotation;

/**
 * @author Alex Mikhalev
 */
public class LookaheadReport {
    public Pose2d nearestPoint;
    public Pose2d lookaheadPoint;
    public double traveledDistance;
    public double remainingDistance;
    public double lookaheadTraveledDistance;
    public double lookaheadRemainingDistance;
    public double trackError;
    public Rotation yawError;

    @Override
    public String toString() {
        return "LookaheadReport{" +
                "nearestPoint=" + nearestPoint +
                ", lookaheadPoint=" + lookaheadPoint +
                ", traveledDistance=" + traveledDistance +
                ", remainingDistance=" + remainingDistance +
                ", trackError=" + trackError +
                ", yawError=" + yawError +
                '}';
    }
}
